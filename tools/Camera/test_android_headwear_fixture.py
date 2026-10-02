"""Fixture compiler contracts and optional original-Android-byte regression.

Set HEADWEAR_ANDROID_RAW and HEADWEAR_ANDROID_DATABASE to run asset regressions.
"""
import copy
import hashlib
import json
import os
from pathlib import Path
import struct
import tempfile
import unittest
from unittest.mock import patch
import zlib

import build_android_headwear_fixture as fixture


class CompilerContract(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.raw = Path(self.temp.name)
        self.layout = dict(mesh="S_actor_test_cloth_01_lod1", mesh_id="mesh:1", vertices=6,
                           indices=9, bindposes=2, indexElementSize=2, submeshes=1, blendshapes=0,
                           attributes=[dict(zip(("attribute", "format", "dimension", "stream"), a)) for a in fixture.ATTRIBUTES],
                           streams=[dict(stride=s,length=s*6) for s in fixture.STRIDES])
        self.renderer = dict(name=self.layout["mesh"],mesh_name=self.layout["mesh"],mesh_id="mesh:1",
                             resource_root="chr_0001_test_postmodel",path="chr_0001_test_postmodel/Mesh_all/lod1/cloth",
                             bones=[dict(index=0,path="root/Bip001_Head"),dict(index=1,path="root/Body")])
        self.graph = dict(renderers=[self.renderer],meshes={"mesh:1":dict(submeshes=[dict(topology="Triangles",base_vertex=0,first_byte=0,index_count=9)])})
        (self.raw / "stream0.bin").write_bytes(bytes(96))
        (self.raw / "stream1.bin").write_bytes(bytes(96))
        skin = b"".join(struct.pack("<4H4B",65535,0,0,0,i//3,0,0,0) for i in range(6))
        (self.raw / "stream2.bin").write_bytes(skin)
        (self.raw / "indices-original.bin").write_bytes(struct.pack("<9H",0,1,2,3,4,5,0,3,4))
        self.write_layout()

    def write_layout(self):
        (self.raw / "layout.json").write_text(json.dumps(self.layout),encoding="utf-8")

    def test_v3_abi_and_conservative_mixed_triangles(self):
        blob, report = fixture.compile_fixture(self.raw,self.graph)
        header = fixture.HEADER.unpack_from(blob)
        self.assertEqual((header[0],header[1]),(b"BEHWMESH",3))
        self.assertEqual(header[2],84+6*16+12+len(self.layout["mesh"]))
        self.assertEqual(header[17],len(self.layout["mesh"]))
        self.assertEqual(header[15],zlib.crc32(blob[header[2]:]))
        self.assertEqual(header[18:20],(2,1))
        self.assertEqual(struct.unpack("<9H",blob[-18:]),(0,0,0,3,4,5,0,3,4))
        self.assertEqual((report["pure_head_triangles"],report["mixed_triangles"],report["pure_body_triangles"]),(1,1,1))

    def test_five_attribute_real_layout(self):
        self.layout["attributes"] = [a for a in self.layout["attributes"] if a["attribute"] != 5]
        self.layout["streams"][1] = dict(stride=8,length=48)
        (self.raw / "stream1.bin").write_bytes(bytes(48))
        self.write_layout()
        blob, report = fixture.compile_fixture(self.raw,self.graph)
        self.assertEqual(report["layout"]["strides"],[16,8,12])
        self.assertEqual(fixture.HEADER.unpack_from(blob)[7],5)

    def test_shadow_proxy_does_not_make_world_identity_ambiguous(self):
        proxy = copy.deepcopy(self.renderer)
        proxy["name"] = "cloth_shadowProxyMobile"
        self.graph["renderers"].append(proxy)
        fixture.compile_fixture(self.raw,self.graph)
        self.graph["renderers"].append(copy.deepcopy(self.renderer))
        fixture.compile_fixture(self.raw,self.graph)
        self.graph["renderers"][-1]["bones"][0]["path"]="root/Body"
        self.graph["renderers"][-1]["bones"][1]["path"]="root/Bip001_Head"
        with self.assertRaisesRegex(ValueError,"classification ambiguous"):
            fixture.compile_fixture(self.raw,self.graph)

    def test_primary_character_scope_excludes_same_name_proxy_and_summon(self):
        for path in ("Shadow_Proxy/SP_Mobile/cloth", "Root/Soldier_Root/Rush01/P_wpn/Mesh_all/lod1/cloth"):
            duplicate = copy.deepcopy(self.renderer)
            duplicate["path"] = duplicate["resource_root"] + "/" + path
            duplicate["bones"][0]["path"]="other/Body"
            self.graph["renderers"].append(duplicate)
        fixture.compile_fixture(self.raw,self.graph)
        self.graph["renderers"].pop(0)
        with self.assertRaisesRegex(ValueError,"identity missing"):
            fixture.compile_fixture(self.raw,self.graph)

    def test_malformed_counts_and_layouts(self):
        for key, value in [("vertices",262145),("vertices",65536),("vertices",0),("indices",1200003),("indices",8),("bindposes",257),("indexElementSize",3),("blendshapes",1),("submeshes",2),("mesh","../unsafe"),("mesh","a"*161)]:
            with self.subTest(key=key,value=value):
                original = self.layout[key]
                self.layout[key] = value
                self.write_layout()
                with self.assertRaises(ValueError):
                    fixture.compile_fixture(self.raw,self.graph)
                self.layout[key] = original
        self.write_layout()
        self.layout["streams"][0]["stride"] = 12
        self.write_layout()
        with self.assertRaises(ValueError):
            fixture.compile_fixture(self.raw,self.graph)

    def test_invalid_indices_and_truncated_stream(self):
        (self.raw / "indices-original.bin").write_bytes(struct.pack("<9H",0,1,6,3,4,5,0,3,4))
        with self.assertRaisesRegex(ValueError,"index exceeds"):
            fixture.compile_fixture(self.raw,self.graph)
        (self.raw / "indices-original.bin").write_bytes(bytes(17))
        with self.assertRaisesRegex(ValueError,"index size"):
            fixture.compile_fixture(self.raw,self.graph)

    def test_palette_and_exact_head_path(self):
        self.renderer["bones"][0]["path"] = "root/Bip001_HeadAccessory"
        with self.assertRaisesRegex(ValueError,"head bone"):
            fixture.compile_fixture(self.raw,self.graph)

        self.renderer["bones"][0]["path"] = "root/Bip001_Head"
        skin = bytearray((self.raw / "stream2.bin").read_bytes())
        skin[8] = 2
        (self.raw / "stream2.bin").write_bytes(skin)
        with self.assertRaisesRegex(ValueError,"bone index"):
            fixture.compile_fixture(self.raw,self.graph)

    def test_truncated_stream(self):
        (self.raw / "stream1.bin").write_bytes(bytes(95))
        with self.assertRaisesRegex(ValueError,"stream size"):
            fixture.compile_fixture(self.raw,self.graph)

    def test_head_descendant_without_root_in_palette(self):
        self.renderer["bones"][0]["path"] = "root/Bip001_Head/Accessory"
        _, report = fixture.compile_fixture(self.raw,self.graph)
        self.assertEqual(report["pure_head_triangles"],1)
        self.renderer["bones"][1]["path"] = "other/Bip001_Head/Accessory"
        with self.assertRaisesRegex(ValueError,"head bone.*ambiguous"):
            fixture.compile_fixture(self.raw,self.graph)

    def test_all_head_has_shadow_source_and_no_head_rejected(self):
        (self.raw / "stream2.bin").write_bytes(struct.pack("<4H4B",65535,0,0,0,0,0,0,0)*6)
        _, report = fixture.compile_fixture(self.raw,self.graph)
        self.assertEqual(report["pure_head_triangles"],3)
        (self.raw / "stream2.bin").write_bytes(struct.pack("<4H4B",65535,0,0,0,1,0,0,0)*6)
        with self.assertRaisesRegex(ValueError,"no pure head"):
            fixture.compile_fixture(self.raw,self.graph)

    def test_uint32_two_material_slots_preserved(self):
        self.layout.update(indexElementSize=4,submeshes=2)
        self.write_layout()
        self.graph["meshes"]["mesh:1"]["submeshes"] = [dict(topology="Triangles",base_vertex=0,first_byte=0,index_count=3),dict(topology="Triangles",base_vertex=0,first_byte=12,index_count=6)]
        (self.raw / "indices-original.bin").write_bytes(struct.pack("<9I",0,1,2,3,4,5,0,3,4))
        blob, _ = fixture.compile_fixture(self.raw,self.graph)
        header = fixture.HEADER.unpack_from(blob)
        self.assertEqual(header[18:20],(4,2))
        self.assertEqual(fixture.DRAW.unpack_from(blob,84+96),(0,3,0))
        self.assertEqual(fixture.DRAW.unpack_from(blob,84+96+12),(3,6,0))
        self.assertEqual(struct.unpack("<9I",blob[-36:]),(0,0,0,3,4,5,0,3,4))
        self.graph["meshes"]["mesh:1"]["submeshes"][1]["first_byte"]=8
        with self.assertRaisesRegex(ValueError,"offsets"):
            fixture.compile_fixture(self.raw,self.graph)

    def test_rigid_bone_indices_contract(self):
        self.layout["attributes"]=[a for a in self.layout["attributes"] if a["attribute"]!=12]
        self.layout["streams"][2]=dict(stride=4,length=24)
        self.write_layout()
        (self.raw/"stream2.bin").write_bytes(b"".join(bytes((i//3,0,0,0)) for i in range(6)))
        _, report = fixture.compile_fixture(self.raw,self.graph)
        self.assertEqual(report["pure_head_triangles"],1)
        (self.raw/"stream2.bin").write_bytes(bytes((0,1,0,0))*6)
        with self.assertRaisesRegex(ValueError,"additional bone indices"):
            fixture.compile_fixture(self.raw,self.graph)


class CatalogOutputOwnership(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.raw = self.root / "raw"
        self.raw.mkdir()
        self.output = self.root / "output"
        self.output.mkdir()
        self.database = self.root / "database.json"
        self.database.write_text(json.dumps(dict(renderers=[],meshes={})),encoding="utf-8")
        registry = self.root / "profiles.json"
        registry.write_text(json.dumps(dict(schema=1, profiles=[])), encoding="utf-8")
        self.profile_patch = patch.object(fixture, "PROFILE_PATH", registry)
        self.profile_patch.start()
        self.addCleanup(self.profile_patch.stop)

    def test_unknown_user_fixture_survives_without_previous_coverage(self):
        extra = self.output / "user_extra.behw"
        extra.write_bytes(b"user fixture")
        fixture.build_catalog(self.raw,self.database,self.output)
        self.assertEqual(extra.read_bytes(),b"user fixture")

    def test_only_previous_owned_stale_fixture_is_removed(self):
        stale = self.output / "generated_stale.behw"
        stale.write_bytes(b"previous generated fixture")
        extra = self.output / "user_extra.behw"
        extra.write_bytes(b"user fixture")
        outside = self.root / "outside.behw"
        outside.write_bytes(b"outside fixture")
        (self.output / "coverage.json").write_text(json.dumps(dict(included=[dict(mesh="generated_stale"),dict(mesh="../outside"),dict(mesh="user_extra.behw")])),encoding="utf-8")
        fixture.build_catalog(self.raw,self.database,self.output)
        self.assertFalse(stale.exists())
        self.assertEqual(extra.read_bytes(),b"user fixture")
        self.assertEqual(outside.read_bytes(),b"outside fixture")


@unittest.skipUnless(os.environ.get("HEADWEAR_ANDROID_RAW") and os.environ.get("HEADWEAR_ANDROID_DATABASE"),"original Android assets not configured")
class AndroidOriginalBytes(unittest.TestCase):
    def test_profile_metadata_drift_or_missing_asset_aborts_catalog_before_writes(self):
        root = Path(os.environ["HEADWEAR_ANDROID_RAW"])
        graph_path = Path(os.environ["HEADWEAR_ANDROID_DATABASE"])
        by_name = {fixture.bounded_json(p)["mesh"]:p.parent for p in root.glob("*/layout.json")}
        for failure in ("identity", "layout", "stream", "missing"):
            with self.subTest(failure=failure), tempfile.TemporaryDirectory() as directory:
                base = Path(directory)
                raw_root = base / "raw"
                raw_root.mkdir()
                for profile in fixture.asset_profiles():
                    target = raw_root / profile["mesh"]
                    target.mkdir()
                    for source in by_name[profile["mesh"]].iterdir():
                        if source.is_file():
                            (target / source.name).write_bytes(source.read_bytes())
                changed = raw_root / "S_actor_bounda_cloth_01_lod1"
                layout = fixture.bounded_json(changed / "layout.json")
                if failure == "identity":
                    layout["mesh_id"] = "changed-source"
                elif failure == "layout":
                    layout["streams"][0]["stride"] = 20
                elif failure == "stream":
                    (changed / "stream0.bin").unlink()
                else:
                    layout["mesh"] = "S_actor_changed_cloth_01_lod1"
                (changed / "layout.json").write_text(json.dumps(layout), encoding="utf-8")
                output = base / "output"
                output.mkdir()
                existing = output / "S_actor_bounda_cloth_01_lod1.behw"
                existing.write_bytes(b"previous accepted artifact")
                with self.assertRaises(fixture.ProfileRejected):
                    fixture.build_catalog(raw_root, graph_path, output)
                self.assertEqual(existing.read_bytes(), b"previous accepted artifact")
                self.assertEqual(list(output.iterdir()), [existing])

    def test_bounda_reproduces_device_accepted_boundary_bytes(self):
        root = Path(os.environ["HEADWEAR_ANDROID_RAW"])
        graph = fixture.bounded_json(Path(os.environ["HEADWEAR_ANDROID_DATABASE"]))
        by_name = {fixture.bounded_json(p)["mesh"]:p.parent for p in root.glob("*/layout.json")}
        accepted = [
            (2200, "3794b6036fa45eed6ffb407295dc32ca1dee06b682a39fd9f058298f73c3682a"),
            (967, "32cca00dce464ce67b69943ed03954122c9b505301a4fe4815899543c3346e7d"),
            (243, "80faafec5a74d22d6890ff25d82991b8e58800aeaa57ddbbee8d173985404814"),
        ]
        for lod, (hidden, digest) in enumerate(accepted, 1):
            with self.subTest(lod=lod):
                raw = by_name[f"S_actor_bounda_cloth_01_lod{lod}"]
                blob, report = fixture.compile_fixture(raw, graph)
                self.assertEqual(hashlib.sha256(blob).hexdigest(), digest)
                self.assertEqual(fixture.HEADER.unpack_from(blob)[6], hidden)
                self.assertEqual(report["hidden_triangles"], hidden)
                # Even tiny source-weight drift must reject the accepted profile.
                with tempfile.TemporaryDirectory() as directory:
                    changed = Path(directory)
                    for source in raw.iterdir():
                        if source.is_file():
                            (changed / source.name).write_bytes(source.read_bytes())
                    skin = bytearray((changed / "stream2.bin").read_bytes())
                    skin[0] ^= 1
                    (changed / "stream2.bin").write_bytes(skin)
                    with self.assertRaisesRegex(ValueError, "profile source fingerprint differs"):
                        fixture.compile_fixture(changed, graph)

    def test_original_special_layouts_keep_material_ranges_and_body_faces(self):
        root = Path(os.environ["HEADWEAR_ANDROID_RAW"])
        graph = fixture.bounded_json(Path(os.environ["HEADWEAR_ANDROID_DATABASE"]))
        by_name={fixture.bounded_json(p)["mesh"]:p.parent for p in root.glob("*/layout.json")}
        expected={"aurora_fur_01_lod1_8":1392,"karin_fur_01_lod1_8":1760,
                  "purrchena_fur_01_lod1_11":1947,"tangtang_fur_01_lod1_8":1632,
                  "endminm_cloth_04_lod3":206,"bounda_clothshadowless_01_lod1":336,
                  "bounda_clothshadowless_01_lod2":94,"bounda_clothshadowless_01_lod3":32,
                  "purrchena_fur_03_lod1_11":21648}
        for suffix, hidden in expected.items():
            name="S_actor_"+suffix
            if name not in by_name:
                self.skipTest("all-character Android originals not configured")
            with self.subTest(mesh=name):
                raw=by_name[name]
                layout=fixture.bounded_json(raw/"layout.json")
                blob, report=fixture.compile_fixture(raw,graph)
                self.assertEqual(report["pure_head_triangles"],hidden)
                header=fixture.HEADER.unpack_from(blob)
                streams=b"".join((raw/f"stream{i}.bin").read_bytes() for i in range(3))
                self.assertEqual(blob[header[2]:header[2]+len(streams)],streams)
                draw_offset=84+header[7]*16
                for i, draw in enumerate(graph["meshes"][layout["mesh_id"]]["submeshes"]):
                    self.assertEqual(fixture.DRAW.unpack_from(blob,draw_offset+i*12),(draw["first_byte"]//layout["indexElementSize"],draw["index_count"],0))
                before=(raw/"indices-original.bin").read_bytes()
                after=blob[header[2]+len(streams):]
                code="<3H" if layout["indexElementSize"]==2 else "<3I"
                size=struct.calcsize(code)
                changed=0
                for offset in range(0,len(before),size):
                    a,b=struct.unpack_from(code,before,offset),struct.unpack_from(code,after,offset)
                    if a!=b:
                        self.assertEqual(b,(a[0],)*3)
                        changed+=1
                self.assertEqual(changed,hidden)

    def test_three_character_meshes_preserve_streams_and_remaining_faces(self):
        root = Path(os.environ["HEADWEAR_ANDROID_RAW"])
        graph = fixture.bounded_json(Path(os.environ["HEADWEAR_ANDROID_DATABASE"]))
        by_name={fixture.bounded_json(p)["mesh"]:p.parent for p in root.glob("*/layout.json")}
        expected = {"zhuangfy":[3300,1746,955],"aglina":[177,119,105],"pelica":[220,54,29]}
        for character, hidden_counts in expected.items():
            for lod, hidden in enumerate(hidden_counts,1):
                name = f"S_actor_{character}_cloth_01_lod{lod}"
                with self.subTest(mesh=name):
                    raw = by_name[name]
                    blob, report = fixture.compile_fixture(raw,graph)
                    self.assertEqual(report["pure_head_triangles"],hidden)
                    self.assertEqual(report["mixed_triangles"],0)
                    start = fixture.HEADER.unpack_from(blob)[2]
                    original_streams = b"".join((raw / f"stream{i}.bin").read_bytes() for i in range(3))
                    self.assertEqual(blob[start:start+len(original_streams)],original_streams)
                    original_indices = (raw / "indices-original.bin").read_bytes()
                    changed_indices = blob[start+len(original_streams):]
                    changed = 0
                    for offset in range(0,len(original_indices),6):
                        before = struct.unpack_from("<3H",original_indices,offset)
                        after = struct.unpack_from("<3H",changed_indices,offset)
                        if before != after:
                            self.assertEqual(after,(before[0],)*3)
                            changed += 1
                    self.assertEqual(changed,hidden)


if __name__ == "__main__":
    unittest.main()
