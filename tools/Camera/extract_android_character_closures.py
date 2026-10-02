"""Bounded, read-only ADB extraction of a manifest-selected world prefab closure."""
import argparse, importlib.util, json, re, subprocess, sys, types, zlib
from pathlib import Path

def load(name, path):
    spec=importlib.util.spec_from_file_location(name,path)
    module=importlib.util.module_from_spec(spec);sys.modules[name]=module
    spec.loader.exec_module(module);return module

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for flag in ('roster','metadata','unpacker','helper','output'):p.add_argument('--'+flag,type=Path,required=True)
    p.add_argument('--serial',required=True);p.add_argument('--extract',action='store_true')
    p.add_argument('--budget',type=int,default=1024**3)
    a=p.parse_args();roster=json.loads(a.roster.read_text(encoding='utf-8'))
    stub=types.ModuleType('config');stub.get_game_dir=lambda:str(a.metadata);sys.modules['config']=stub
    vfs=load('android_all_vfs',a.unpacker/'decrypt_vfs.py');vfs.crc32=lambda d:(zlib.crc32(d)+2**31)%2**32-2**31
    helper=load('android_all_helper',a.helper)
    wanted={r['name'].casefold() for r in roster['bundles']};found={}
    for blc in sorted(a.metadata.glob('*/*.blc')):
        for record in helper.parse_blc_records(vfs,blc,'Android',re.compile('.*')):
            if record.relative_path.casefold() in wanted:found[record.relative_path.casefold()]=(record,blc.parent.name)
    rows=[];new_bytes=0
    for key in sorted(wanted):
        if key not in found:rows.append(dict(name=key,status='missing-vfs-record'));continue
        rec,directory=found[key];cached=a.metadata/'extracted'/rec.relative_path
        cache_hit=cached.exists() and cached.stat().st_size==rec.size
        if not cache_hit:new_bytes+=rec.size
        rows.append(dict(name=rec.relative_path,size=rec.size,cached=cache_hit,directory=directory,chunk=rec.chunk,status='available'))
    sizes={r['name'].casefold():r for r in rows}
    for char in roster['characters']:
        char['closure_bytes']=sum(sizes[b['name'].casefold()].get('size',0) for b in char['bundles'])
        char['missing']=[b['name'] for b in char['bundles'] if sizes[b['name'].casefold()]['status']!='available']
    report=dict(roster,total_bytes=sum(r.get('size',0) for r in rows),new_bytes=new_bytes,budget=a.budget,storage=rows)
    a.output.mkdir(parents=True,exist_ok=True);(a.output/'extraction-plan.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(dict(characters=len(roster['characters']),bundles=len(rows),total_bytes=report['total_bytes'],new_bytes=new_bytes,missing=sum(r['status']!='available' for r in rows))),flush=True)
    if not a.extract:return
    if new_bytes>a.budget:raise ValueError('new extraction bytes exceed approved budget')
    if any(r['status']!='available' for r in rows):raise ValueError('incomplete manifest closure')
    for i,row in enumerate(rows):
        rec,directory=found[row['name'].casefold()];dest=a.metadata/'extracted'/rec.relative_path
        if row['cached']:continue
        first,delta=divmod(rec.offset,4096);count=(delta+rec.size+4095)//4096
        remote=f'/sdcard/Android/data/com.hypergryph.endfield/files/VFS/{directory}/{rec.chunk}.chk'
        raw=subprocess.check_output(['adb','-s',a.serial,'exec-out','dd',f'if={remote}','bs=4096',f'skip={first}',f'count={count}'],stderr=subprocess.DEVNULL,timeout=60)
        data=raw[delta:delta+rec.size]
        if len(data)!=rec.size:raise ValueError('short read: '+rec.relative_path)
        if rec.encrypted:data=vfs.per_file_decrypt(data,rec.iv_seed)
        dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data)
        print(f'{i+1}/{len(rows)} {rec.relative_path} {rec.size}',flush=True)

if __name__=='__main__':main()
