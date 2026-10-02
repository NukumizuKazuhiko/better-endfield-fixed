using AnimeStudio;
using System.Buffers.Binary;
using System.Collections;
using System.Reflection;
using System.Text.Json;
static T Field<T>(object obj,string name)=>(T)(obj.GetType().GetField(name,BindingFlags.Instance|BindingFlags.Public|BindingFlags.NonPublic)?.GetValue(obj)??throw new InvalidDataException("missing "+name));
if(args.Length != 3) throw new ArgumentException("<Android-bundle-directory> <database.json> <output-directory>"); var outputRoot=Path.GetFullPath(args[2]); var output=outputRoot;
Directory.CreateDirectory(output);
var manager=new AssetsManager { Game=GameManager.GetGameByType(GameType.ArknightsEndfield)! };
manager.LoadFiles(Directory.GetFiles(Path.GetFullPath(args[0]),"*.ab",SearchOption.AllDirectories));
using var graph=JsonDocument.Parse(File.ReadAllText(Path.GetFullPath(args[1]))); var ids=graph.RootElement.GetProperty("renderers").EnumerateArray().Where(r=>r.GetProperty("resource_root").GetString()!.EndsWith("_postmodel")).Select(r=>r.GetProperty("mesh_id").GetString()).ToHashSet(); foreach(var mesh in manager.assetsFileList.SelectMany(f=>f.Objects).OfType<Mesh>().Where(m=>ids.Contains(m.assetsFile.fileName.ToLowerInvariant()+":"+m.m_PathID))) { output=Path.Combine(outputRoot,mesh.m_Name+"__"+mesh.assetsFile.fileName.ToLowerInvariant()+"_"+mesh.m_PathID); Directory.CreateDirectory(output);
var vertex=Field<object>(mesh,"m_VertexData");var streamList=Field<IList>(vertex,"m_Streams");var channelList=Field<IList>(vertex,"m_Channels");var sourceIndices=Field<uint[]>(mesh,"m_IndexBuffer");var shortIndices=Field<bool>(mesh,"m_Use16BitIndices");
var indexSize=shortIndices?2:4;
var data=Field<byte[]>(vertex,"m_DataSize");
var streams=new List<object>();
for(int i=0;i<streamList.Count;++i){var stream=streamList[i]!;var start=checked((int)Field<uint>(stream,"offset"));var stride=Field<uint>(stream,"stride");var length=checked((int)(stride*(uint)mesh.m_VertexCount));if(start<0||length<=0||start+length>data.Length)throw new InvalidDataException("invalid stream range");File.WriteAllBytes(Path.Combine(output,$"stream{i}.bin"),data.AsSpan(start,length).ToArray());streams.Add(new{stream=i,offset=start,stride,length,channelMask=Field<uint>(stream,"channelMask")});}
var indices=new byte[sourceIndices.Length*indexSize];
for(int i=0;i<sourceIndices.Length;++i) { if(shortIndices) BinaryPrimitives.WriteUInt16LittleEndian(indices.AsSpan(i*2),checked((ushort)sourceIndices[i])); else BinaryPrimitives.WriteUInt32LittleEndian(indices.AsSpan(i*4),sourceIndices[i]); }
File.WriteAllBytes(Path.Combine(output,"indices-original.bin"),indices);
var attrs=new List<object>();
for(int i=0;i<channelList.Count;++i){var ch=channelList[i]!;var dimension=Field<byte>(ch,"dimension");if(dimension==0)continue;attrs.Add(new{attribute=i,format=(int)Field<byte>(ch,"format"),dimension=(int)dimension,stream=(int)Field<byte>(ch,"stream"),offset=(int)Field<byte>(ch,"offset")});}
var bindposes=Field<Array>(mesh,"m_BindPose").Length;
File.WriteAllText(Path.Combine(output,"layout.json"),JsonSerializer.Serialize(new{mesh=mesh.m_Name,mesh_id=mesh.assetsFile.fileName.ToLowerInvariant()+":"+mesh.m_PathID,vertices=mesh.m_VertexCount,indices=sourceIndices.Length,indexElementSize=indexSize,bindposes,submeshes=mesh.m_SubMeshes.Count,blendshapes=mesh.m_Shapes.shapes.Count,streams,attributes=attrs},new JsonSerializerOptions{WriteIndented=true}));
Console.WriteLine($"raw streams={streams.Count} bytes={data.Length} indices={sourceIndices.Length} attributes={attrs.Count}");
} manager.Clear();
