using System.Text.Json;
using System.Text.RegularExpressions;
using Beyond.ManifestBinary;

// Compile with the game's ManifestBinary reader. Only manifest facts are exported;
// gameplay availability cannot be established by an asset filename.
if(args.Length!=2) throw new ArgumentException("<manifest.hgmmap> <roster.json>");
var manifest=new ManifestDataBinary().InitBinary(File.ReadAllBytes(args[0]));
var bundles=manifest.Bundles.ToDictionary(b=>b.bundleIndex);
var pattern=new Regex(@"(?:^|/)(chr_[^/]+)_postmodel\.prefab$",RegexOptions.IgnoreCase);
var rows=new List<object>(); var union=new HashSet<int>();
foreach(var asset in manifest.Assets.OrderBy(a=>a.path)) {
 var match=pattern.Match(asset.path??""); if(!match.Success) continue;
 var closure=new HashSet<int>(); var pending=new Stack<int>();pending.Push(asset.bundleIndex);
 while(pending.Count>0) {var id=pending.Pop();if(!closure.Add(id))continue;
  if(closure.Count>2048||!bundles.TryGetValue(id,out var b))throw new InvalidDataException("invalid closure");
  foreach(var dep in b.dependencies??[])pending.Push(dep);
  foreach(var dep in b.directDependencies??[])pending.Push(dep);
 }
 union.UnionWith(closure);
 var category=asset.path.Contains("/postmodels/characters/")?"characters-directory":asset.path.Contains("/postmodels/npc/")?"npc-directory":"other-directory";
 rows.Add(new{character=match.Groups[1].Value,asset_path=asset.path,asset_category=category,prefab_bundle=asset.bundleIndex,
  availability="unknown-manifest-only",category_evidence="postmodel prefab asset; gameplay and skin status unproven",
  bundles=closure.Order().Select(id=>new{index=id,name="Bundles/Android/"+bundles[id].name})});
}
File.WriteAllText(args[1],JsonSerializer.Serialize(new{schema=1,manifest_version=manifest.Version,perforce_cl=manifest.perforceCL,
 characters=rows,bundles=union.Order().Select(id=>new{index=id,name="Bundles/Android/"+bundles[id].name}),runtime_verified=false},new JsonSerializerOptions{WriteIndented=true}));
Console.WriteLine($"postmodels={rows.Count} unique_bundles={union.Count}");
