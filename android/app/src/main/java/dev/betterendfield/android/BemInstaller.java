package dev.betterendfield.android;

import android.content.Context;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.Executors;

/** Installation happens in the module app; the injected runtime never encodes textures. */
final class BemInstaller {
    static final String INDEX = "installed_bem_packages";
    static final String KEEP_LOCAL_COPIES = "bem_keep_local_copies";
    static volatile String status = "原样导入 BEM 包；贴图异常时，可在对应模型包下手动转换手机纹理。";
    static volatile boolean busy;
    static volatile boolean removing;
    static volatile int progressPercent=-1;
    static volatile long startedAt;
    private static volatile boolean cancelled;
    private static boolean nativeLoaded;
    private static final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
    static native String convertNative(String input, String output, String rules, boolean astc) throws IOException;
    static native String inspectNative(String input) throws IOException;
    static native void cancelNative();
    // Called on the native conversion worker, consumed by the management UI.
    static void conversionProgress(String texture,int current,int total,int mip,int mips,float percent) {
        if(!cancelled) {
            progressPercent=Math.max(0,Math.min(100,Math.round(percent)));
            status="正在转换第 "+current+" 张纹理 · mip "+mip+" / "+mips+"\n"+texture;
        }
    }
    static synchronized void loadCodec() { if (!nativeLoaded) { System.loadLibrary("betterendfield_installer"); nativeLoaded=true; } }
    static synchronized void cancel() { if(removing || !busy) return; cancelled=true; if(nativeLoaded) cancelNative(); status="正在取消，保留原安装版本…"; }
    static void checkpoint() throws IOException { if(cancelled) throw new IOException("已取消安装"); }
    static JSONArray index(Context context) {
        try { return new JSONArray(FrameworkSettings.open(context).getString(INDEX,"[]")); }
        catch (Exception e) { throw new IllegalStateException("安装索引损坏",e); }
    }
    static synchronized void start(Context context, Uri uri) {
        run(context, uri, null);
    }
    static synchronized void convert(Context context, String generation) {
        run(context, null, generation);
    }
    private static synchronized void run(Context context, Uri uri, String previousGeneration) {
        if(busy) return;
        boolean converting=previousGeneration!=null;
        progressPercent=-1;startedAt=android.os.SystemClock.elapsedRealtime();
        busy=true; cancelled=false; status=converting?"正在准备纹理转换…":"正在读取并校验包（不转换纹理）…";
        Context app=context.getApplicationContext();
        worker.execute(() -> {
            File stage=null;
            try {
                File root=new File(app.getFilesDir(),"bem-installed");
                if(!root.isDirectory() && !root.mkdirs()) throw new IOException("无法创建安装目录");
                // Every transaction gets its own directory. A process death cannot replace the active generation.
                stage=new File(root,"stage-"+UUID.randomUUID());
                if(!stage.mkdir()) throw new IOException("无法创建临时目录");
                File output=new File(stage,"installed.bem");
                File source=converting?new File(stage,"source.bem"):output;
                JSONObject previousEntry=null;
                if(converting) {
                    if(!previousGeneration.matches("[a-f0-9-]{36}")) throw new IOException("无效的模型包版本");
                    previousEntry=findEntry(index(app),previousGeneration);
                    if(previousEntry==null) throw new IOException("模型包已被替换，请刷新后重试");
                }
                File localSource=converting?new File(new File(root,previousGeneration),"installed.bem"):null;
                try(InputStream in=converting
                        ?(localSource.isFile()?new FileInputStream(localSource):FrameworkSettings.openBem("bem-"+previousGeneration+".bem"))
                        :app.getContentResolver().openInputStream(uri); FileOutputStream out=new FileOutputStream(source)) {
                    if(in==null) throw new IOException("无法打开包");
                    copy(in,out,2L*1024*1024*1024);out.getFD().sync();
                }
                loadCodec();checkpoint();
                JSONObject result;
                if(converting) {
                    boolean astc=AstcSupport.available();
                    status=astc?"正在转换 ASTC 纹理；大包可能需要数分钟…":"设备未报告 ASTC，正在生成 RGBA32 资源…";
                    String rules;
                    try(InputStream in=app.getAssets().open("android-normal-rules.json")) {rules=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
                    result=new JSONObject(convertNative(source.getAbsolutePath(),output.getAbsolutePath(),rules,astc));
                    result.put("texture_mode",astc?"astc":"rgba32");
                    source.delete();
                } else {
                    result=new JSONObject(inspectNative(source.getAbsolutePath()));
                    result.put("texture_mode","original");
                }
                checkpoint();
                try(FileOutputStream out=new FileOutputStream(new File(stage,"report.json"))) {out.write(result.toString(2).getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
                String generation=UUID.randomUUID().toString();
                File installed=new File(root,generation);
                if(!stage.renameTo(installed)) throw new IOException("安装结果发布失败");
                stage=installed;
                result.put("generation",generation).put("remote","bem-"+generation+".bem").put("enabled",true);
                progressPercent=-1;status="正在发布给游戏…";
                if(!FrameworkSettings.publishBem(new File(installed,"installed.bem"),result.getString("remote")))
                    throw new IOException("框架服务未连接；请启用 modern 模块后重试");
                checkpoint();
                synchronized(BemInstaller.class) {
                    JSONArray previous=index(app), next=new JSONArray();
                    String savedParameters=result.optString("default_parameters","");
                    if(converting) {
                        JSONObject latest=findEntry(previous,previousGeneration);
                        if(latest==null) throw new IOException("模型包已被替换，原配置保持不变");
                        savedParameters=latest.optString("remembered_parameters",
                            latest.optString("selected_parameters",savedParameters));
                        result.put("enabled",latest.optBoolean("enabled",true));
                        if(result.optInt("bem_minor",0)>=1) {
                            String saved=latest.optString("selected_options",result.getString("default_options"));
                            try {result.put("selected_options",BemOptions.encode(BemOptions.parse(result,saved)));}
                            catch(Exception removedChoice) {result.put("selected_options",result.getString("default_options"));}
                        } else result.put("selected_appearance",latest.optString("selected_appearance",latest.getString("default_appearance")));
                    }
                    BemParameters.restore(result,savedParameters);
                    for(int i=0;i<previous.length();++i) {
                        JSONObject old=previous.getJSONObject(i);
                        if(!old.getString("character_id").equals(result.getString("character_id"))) next.put(old);
                    }
                    next.put(result);
                    if(!FrameworkSettings.open(app).edit().putString(INDEX,next.toString()).commit()) throw new IOException("安装索引保存失败");
                }
                // The committed generation is live. Cleanup must never roll it back.
                stage=null;
                String cleanupNotice="";
                try {
                    // The game's private copy remains available until its next startup prune.
                    if(converting && !discardGeneration(root,previousGeneration)) cleanupNotice="旧版本残留可稍后清理。";
                    if(!FrameworkSettings.open(app).getBoolean(KEEP_LOCAL_COPIES,true)
                            && FrameworkSettings.bemSize(result.getString("remote"))==result.getLong("bytes"))
                        new File(installed,"installed.bem").delete();
                } catch(Exception cleanupError) {
                    cleanupNotice="旧文件清理未完成，可稍后重试。";
                    android.util.Log.w("BetterEndfield.Install","Post-install cleanup failed",cleanupError);
                }
                status=(converting?"已转换：":"已原样导入：")+result.getString("name")+"。重启游戏后生效。"+cleanupNotice;
            } catch(Throwable error) {
                String reason=error.getMessage();
                if(reason!=null && reason.contains("Normal slot has no verified source encoding"))
                    reason="此包缺少已确认的法线贴图编码信息，暂时无法转换；可继续使用原包。";
                status=(converting?"转换未完成，原包和启用状态已保留：":"导入未完成：")+reason;
                android.util.Log.e("BetterEndfield.Install",status,error);
            }
            finally {if(stage!=null) deleteOwned(stage);busy=false;}
        });
    }
    private static JSONObject findEntry(JSONArray entries,String generation) throws Exception {
        for(int i=0;i<entries.length();++i) {
            JSONObject entry=entries.getJSONObject(i);
            if(generation.equals(entry.getString("generation"))) return entry;
        }
        return null;
    }
    static synchronized void remove(Context context,String generation) throws Exception {
        if(busy) throw new IOException("请等待当前操作完成，或取消后再移除");
        if(!generation.matches("[a-f0-9-]{36}")) throw new IOException("无效的模型包版本");
        Context app=context.getApplicationContext();
        JSONArray previous=index(app),next=new JSONArray();
        JSONObject removed=findEntry(previous,generation);
        if(removed==null) throw new IOException("模型包已被移除或更新，请刷新后重试");
        for(int i=0;i<previous.length();++i) {
            JSONObject entry=previous.getJSONObject(i);
            if(!generation.equals(entry.getString("generation"))) next.put(entry);
        }
        // Stop advertising the package before touching its immutable files.
        if(!FrameworkSettings.open(app).edit().putString(INDEX,next.toString()).commit()) throw new IOException("移除配置保存失败");
        busy=true;removing=true;cancelled=false;progressPercent=-1;startedAt=android.os.SystemClock.elapsedRealtime();
        status="已从列表移除，正在清理安装文件…";
        worker.execute(()->{
            boolean complete=true;
            try {
                File root=new File(app.getFilesDir(),"bem-installed").getCanonicalFile();
                File[] versions=root.listFiles();
                if(versions!=null) for(File version:versions) {
                    if(!version.getName().matches("[a-f0-9-]{36}") || !version.getCanonicalFile().getParentFile().equals(root)) continue;
                    if(findEntry(next,version.getName())!=null) continue;
                    boolean belongs=version.getName().equals(generation);
                    File report=new File(version,"report.json");
                    if(!belongs && report.isFile()) {
                        try {
                            if(report.length()>1024*1024) {complete=false;continue;}
                            JSONObject metadata=new JSONObject(new String(
                                Files.readAllBytes(report.toPath()),StandardCharsets.UTF_8));
                            belongs=removed.getString("character_id").equals(metadata.optString("character_id"));
                        } catch(Exception unreadable) {complete=false;continue;}
                    }
                    if(!belongs) continue;
                    complete &= FrameworkSettings.removeBem("bem-"+version.getName()+".bem");
                    deleteOwned(version);complete &= !version.exists();
                }
                status="已移除："+removed.getString("name")+"。重启游戏后恢复原模型。"
                        +(complete?"":"部分残留文件未能清理，但该包已停用。");
            } catch(Exception error) {status="模型包已移除；部分文件清理失败："+error.getMessage();}
            finally {removing=false;busy=false;}
        });
    }
    private static boolean discardGeneration(File installed,String generation) throws IOException {
        File root=installed.getCanonicalFile(),version=new File(root,generation);
        if(!generation.matches("[a-f0-9-]{36}") || !version.getCanonicalFile().getParentFile().equals(root))
            throw new IOException("无效的安装目录");
        boolean remoteRemoved=FrameworkSettings.removeBem("bem-"+generation+".bem");
        deleteOwned(version);
        return remoteRemoved && !version.exists();
    }
    /** Never clean when the index is malformed: all referenced generations must be known first. */
    static java.util.Set<String> referencedGenerations(JSONArray entries) throws Exception {
        java.util.Set<String> keep=new java.util.HashSet<>();
        for(int i=0;i<entries.length();++i) {
            String generation=entries.getJSONObject(i).getString("generation");
            if(!generation.matches("[a-f0-9-]{36}")) throw new IOException("安装索引包含无效版本");
            keep.add(generation);
        }
        return keep;
    }
    static long cleanLocalUnused(File root,java.util.Set<String> keep) throws IOException {
        return cleanLocalUnused(root,keep,BemInstaller::deleteOwned);
    }
    static long cleanLocalUnused(File root,java.util.Set<String> keep,java.util.function.Consumer<File> remove) throws IOException {
        File canonical=root.getCanonicalFile();
        File[] children=canonical.listFiles();long freed=0;
        if(children==null) {
            if(canonical.exists()) throw new IOException("本地模型目录无法列举");
            return 0;
        }
        for(File child:children) {
            String name=child.getName();
            if(!(name.matches("stage-[a-f0-9-]{36}") || name.matches("[a-f0-9-]{36}") && !keep.contains(name))) continue;
            if(!child.getCanonicalFile().getParentFile().equals(canonical)) continue;
            long bytes=sizeOf(child);remove.accept(child);
            if(child.exists()) throw new IOException("本地安装文件删除失败（此前可能已清理部分数据）："+name);
            freed+=bytes;
        }
        return freed;
    }
    private static long sizeOf(File file) {
        if(Files.isSymbolicLink(file.toPath())) return 0;
        File[] children=file.listFiles();if(children==null) return file.length();
        long total=0;for(File child:children) total+=sizeOf(child);return total;
    }
    private static String megabytes(long bytes) {return String.format(java.util.Locale.ROOT,"%.1f MB",bytes/1048576.0);}
    /** Explicitly removes interrupted stages and generations absent from the committed index. */
    static synchronized boolean cleanUnused(Context context) {
        if(busy) return false;
        Context app=context.getApplicationContext();
        busy=true;removing=true;cancelled=false;progressPercent=-1;startedAt=android.os.SystemClock.elapsedRealtime();
        status="正在清理未使用数据…";
        try {worker.execute(()->{
            try {
                if(!FrameworkSettings.remoteAvailable()) throw new IOException("框架服务未连接");
                java.util.Set<String> keep=referencedGenerations(index(app));
                String[] remoteNames=FrameworkSettings.listBem();
                long freed=cleanLocalUnused(new File(app.getFilesDir(),"bem-installed"),keep);
                for(String remote:remoteNames) {
                    String generation=remote.substring(4,remote.length()-4);
                    if(keep.contains(generation)) continue;
                    long bytes=FrameworkSettings.bemSize(remote);
                    if(!FrameworkSettings.removeBem(remote)) throw new IOException("框架模型文件删除失败（此前可能已清理部分数据）："+remote);
                    freed+=Math.max(0,bytes);
                }
                status="已清理 "+megabytes(freed)+"。游戏目录中未启用的模型会在下次启动时清理。";
            } catch(Exception error) {status="清理未完成："+error.getMessage();}
            finally {removing=false;busy=false;}
        });} catch(RuntimeException rejected) {removing=false;busy=false;status="无法启动清理任务，请重试。";throw rejected;}
        return true;
    }
    /** A local installed.bem is optional only while a matching framework copy exists. */
    static synchronized boolean applyLocalCopies(Context context,boolean keepLocal) {
        if(busy) return false;
        Context app=context.getApplicationContext();
        busy=true;removing=true;cancelled=false;progressPercent=-1;startedAt=android.os.SystemClock.elapsedRealtime();
        status=keepLocal?"正在恢复本地副本…":"正在移除本地副本…";
        try {worker.execute(()->{
            try {
                if(!FrameworkSettings.remoteAvailable()) throw new IOException("框架服务未连接");
                JSONArray entries=index(app);referencedGenerations(entries);
                File root=new File(app.getFilesDir(),"bem-installed").getCanonicalFile();
                long changed=0;StringBuilder missing=new StringBuilder();
                for(int i=0;i<entries.length();++i) {
                    JSONObject entry=entries.getJSONObject(i);
                    String generation=entry.getString("generation"),remote="bem-"+generation+".bem";
                    long bytes=entry.getLong("bytes");
                    if(bytes<=0 || bytes>2L*1024*1024*1024) throw new IOException("安装索引包含无效大小");
                    File folder=new File(root,generation),local=new File(folder,"installed.bem");
                    if(!folder.getCanonicalFile().getParentFile().equals(root)) throw new IOException("无效的安装目录");
                    boolean published=FrameworkSettings.bemSize(remote)==bytes;
                    if(keepLocal) {
                        if(local.isFile() && local.length()==bytes) continue;
                        if(!published) {missing.append(missing.length()>0?"、":"").append(entry.getString("name"));continue;}
                        if(!folder.isDirectory() && !folder.mkdirs()) throw new IOException("无法创建安装目录");
                        File temp=new File(folder,"installed.tmp");
                        try {
                            try(InputStream in=FrameworkSettings.openBem(remote);FileOutputStream out=new FileOutputStream(temp)) {
                                copy(in,out,bytes);out.getFD().sync();
                            }
                            if(temp.length()!=bytes) throw new IOException("本地副本大小不符");
                            android.system.Os.rename(temp.getAbsolutePath(),local.getAbsolutePath());
                        } finally {temp.delete();}
                        changed+=bytes;
                    } else if(local.isFile()) {
                        if(!published) {missing.append(missing.length()>0?"、":"").append(entry.getString("name"));continue;}
                        if(!local.delete()) throw new IOException("本地副本删除失败（此前可能已处理部分文件）："+entry.getString("name"));
                        changed+=bytes;
                    }
                }
                status=(keepLocal?"已恢复本地副本 ":"已移除本地副本 ")+megabytes(changed)+"。"
                        +(missing.length()==0?"":(keepLocal?"框架中缺少文件，需重新导入：":"框架中缺少文件，已保留本地副本：")+missing);
            } catch(Exception error) {status=(keepLocal?"恢复本地副本未完成：":"移除本地副本未完成：")+error.getMessage();}
            finally {removing=false;busy=false;}
        });} catch(RuntimeException rejected) {removing=false;busy=false;status="无法启动任务，请重试。";throw rejected;}
        return true;
    }
    static void copy(InputStream in,OutputStream out,long limit) throws IOException {
        byte[] buffer=new byte[65536];long size=0;int count;
        while((count=in.read(buffer))!=-1) {checkpoint();size+=count;if(size>limit) throw new IOException("文件超过大小限制");out.write(buffer,0,count);}
    }
    private static void deleteOwned(File file) {
        if(!Files.isSymbolicLink(file.toPath())) {
            File[] children=file.listFiles();if(children!=null) for(File child:children) deleteOwned(child);
        }
        file.delete();
    }
    static synchronized void select(Context app,String generation,String appearance,boolean enabled) throws Exception {
        JSONArray entries=index(app);
        for(int i=0;i<entries.length();++i) {JSONObject entry=entries.getJSONObject(i);if(entry.getString("generation").equals(generation)) {entry.put("selected_appearance",appearance);entry.put("enabled",enabled);}}
        if(!FrameworkSettings.open(app).edit().putString(INDEX,entries.toString()).commit()) throw new IOException("保存失败");
    }
    static synchronized void disableAll(Context app) throws Exception {
        if(busy) throw new IOException("请等待当前操作完成后修改");
        JSONArray entries=index(app);
        for(int i=0;i<entries.length();++i) entries.getJSONObject(i).put("enabled",false);
        if(!FrameworkSettings.open(app).edit().putString(INDEX,entries.toString()).commit()) throw new IOException("保存失败");
    }
    static synchronized void saveAll(Context app,JSONArray changes) throws Exception {
        if(busy) throw new IOException("请等待当前操作完成后保存");
        JSONArray entries=index(app);
        for(int i=0;i<changes.length();++i) {
            JSONObject change=changes.getJSONObject(i);
            JSONObject entry=findEntry(entries,change.getString("generation"));
            if(entry==null) throw new IOException("模型包列表已更新，请重新选择后保存");
            if(entry.optInt("bem_minor",0)>=1) {
                String options=BemOptions.encode(BemOptions.parse(entry,change.getString("options")));
                entry.put("selected_options",options);
            } else {
                String appearance=change.getString("appearance");boolean valid=false;
                JSONArray choices=entry.getJSONArray("appearances");
                for(int j=0;j<choices.length();++j) valid |= appearance.equals(choices.getString(j));
                if(!valid) throw new IOException("无效的外观选项");
                entry.put("selected_appearance",appearance);
            }
            if(entry.optInt("bem_minor",0)>=3)
                BemParameters.select(entry,change.getString("parameters"));
            entry.put("enabled",change.getBoolean("enabled"));
        }
        if(!FrameworkSettings.open(app).edit().putString(INDEX,entries.toString()).commit()) throw new IOException("保存失败");
    }
}
