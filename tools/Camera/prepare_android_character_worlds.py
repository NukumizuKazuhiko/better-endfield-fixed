"""Link an extracted manifest closure, read its graph and export world raw meshes."""
import argparse,importlib.util,json,os,subprocess,sys
from pathlib import Path

def main():
    p=argparse.ArgumentParser(description=__doc__)
    for f in ('plan','cache','output','dotnet','reader','parser','exporter'):p.add_argument('--'+f,type=Path,required=True)
    a=p.parse_args();plan=json.loads(a.plan.read_text(encoding='utf-8'));inputs=a.output/'inputs';inputs.mkdir(parents=True,exist_ok=True)
    characters=[c for c in plan['characters'] if '/postmodels/characters/' in c['asset_path']]
    selected={b['name'].casefold() for c in characters for b in c['bundles']}
    for row in plan['storage']:
        if row['name'].casefold() not in selected:continue
        src=a.cache/row['name'];dest=inputs/row['name']
        if not src.exists() or src.stat().st_size!=row['size']:raise ValueError('incomplete closure '+row['name'])
        dest.parent.mkdir(parents=True,exist_ok=True)
        if not dest.exists():os.link(src,dest)
    snapshot=dict(schema=1,manifest_version=plan['manifest_version'],perforce_cl=plan['perforce_cl'],assets=[dict(path=c['asset_path']) for c in characters],missing=[])
    (inputs/'extraction.json').write_text(json.dumps(snapshot),encoding='utf-8')
    raw=a.output/'raw.json';database=a.output/'database.json'
    with (a.output/'reader.log').open('w',encoding='utf-8') as log:
        subprocess.run([str(a.dotnet),str(a.reader),str(inputs),str(raw)],check=True,stdout=log,stderr=subprocess.STDOUT)
    spec=importlib.util.spec_from_file_location('world_parse',a.parser);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
    graph=module.parse(json.loads(raw.read_text(encoding='utf-8')))
    # Full evidence is retained; compact JSON stays within the fixture reader's
    # metadata budget without removing bones or source identity fields.
    database.write_text(json.dumps(graph,separators=(',',':'),ensure_ascii=False),encoding='utf-8')
    with (a.output/'raw-export.log').open('w',encoding='utf-8') as log:
        subprocess.run([str(a.dotnet),str(a.exporter),str(inputs),str(database),str(a.output/'world-raw')],check=True,stdout=log,stderr=subprocess.STDOUT)
    print(json.dumps(graph['summary']),flush=True)

if __name__=='__main__':main()
