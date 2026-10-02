"""Compile v3 fixtures; preserve mixed faces unless a verified asset profile applies."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import zlib

MAGIC = b"BEHWMESH"
HEADER = struct.Struct("<8s19I")
ATTRIBUTE = struct.Struct("<4i")
DRAW = struct.Struct("<3I")
ATTRIBUTES = [(0, 0, 3, 0), (1, 0, 1, 0), (4, 0, 2, 1), (5, 0, 2, 1), (12, 4, 4, 2), (13, 6, 4, 2)]
STRIDES = [16, 16, 12]
LAYOUTS = [(ATTRIBUTES, STRIDES), ([a for a in ATTRIBUTES if a[0] != 5], [16, 8, 12])]
LAYOUTS += [([a for a in attrs if a[0] != 12], [strides[0], strides[1], 4]) for attrs, strides in list(LAYOUTS)]
MAX_FILE_BYTES = 16 * 1024 * 1024
PROFILE_PATH = Path(__file__).with_name("android_headwear_asset_profiles.json")


class ProfileRejected(ValueError):
    """A known accepted asset drifted; never silently omit its fixture."""


def asset_profiles():
    if PROFILE_PATH.stat().st_size > 64 * 1024:
        raise ValueError("asset profile registry exceeds 64 KiB")
    registry = bounded_json(PROFILE_PATH)
    if registry.get("schema") != 1 or not isinstance(registry.get("profiles"), list) or len(registry["profiles"]) > 64:
        raise ValueError("invalid asset profile registry")
    names = set()
    for profile in registry["profiles"]:
        mesh = profile.get("mesh")
        if not isinstance(mesh, str) or not re.fullmatch(r"[A-Za-z0-9_]+", mesh) or mesh in names:
            raise ValueError("invalid or duplicate asset profile identity")
        names.add(mesh)
        if profile.get("policy") != "head_boundary_all_vertices":
            raise ValueError("unsupported asset classification policy")
    return registry["profiles"]


def asset_profile(name):
    return next((p for p in asset_profiles() if p["mesh"] == name), None)


def verify_profile_source(profile, mesh_id, original, streams):
    if profile and (profile.get("mesh_id") != mesh_id or
                    profile.get("source_index_sha256") != hashlib.sha256(original).hexdigest() or
                    profile.get("source_stream_sha256") != [hashlib.sha256(s).hexdigest() for s in streams]):
        raise ProfileRejected("profile source fingerprint differs")


class ClassificationRejected(ValueError):
    def __init__(self, message, diagnostics):
        super().__init__(message)
        self.diagnostics = diagnostics


def bounded_json(path):
    if path.stat().st_size > 64 * 1024 * 1024:
        raise ValueError("metadata exceeds 64 MiB")
    return json.loads(path.read_text(encoding="utf-8-sig"))


def head_palette(renderer, bones):
    bone_rows = renderer["bones"]
    if len(bone_rows) != bones or {b["index"] for b in bone_rows} != set(range(bones)):
        raise ValueError("renderer bone palette differs")
    anchors = set()
    for bone in bone_rows:
        segments = (bone.get("path") or "").split("/")
        if "Bip001_Head" in segments:
            if segments.count("Bip001_Head") != 1:
                raise ValueError("head bone missing or ambiguous")
            anchors.add("/".join(segments[:segments.index("Bip001_Head") + 1]))
    if len(anchors) != 1:
        raise ValueError("head bone missing or ambiguous")
    anchor = anchors.pop()
    return {b["index"] for b in bone_rows if b.get("path") == anchor or (b.get("path") or "").startswith(anchor + "/")}


def compile_fixture(raw, graph):
    profile = asset_profile(bounded_json(raw / "layout.json")["mesh"])
    try:
        return _compile_fixture(raw, graph)
    except (ValueError, KeyError, OSError, struct.error) as error:
        if profile and not isinstance(error, ProfileRejected):
            raise ProfileRejected(f"profile source contract rejected: {error}") from error
        raise


def _compile_fixture(raw, graph):
    layout = bounded_json(raw / "layout.json")
    name = layout["mesh"]
    if not isinstance(name, str) or not re.fullmatch(r"[A-Za-z0-9_]+", name):
        raise ValueError("unsafe mesh name")
    encoded_name = name.encode("utf-8")
    if not 1 <= len(encoded_name) <= 160:
        raise ValueError("mesh name exceeds protocol bound")
    candidates = [r for r in graph["renderers"] if str(r.get("resource_root", "")).endswith("_postmodel") and str(r.get("path", "")).startswith(str(r.get("resource_root", "")) + "/Mesh_all/") and (r.get("name") == name or re.sub(r"_[0-9]+$", "", name) == r.get("name")) and r.get("mesh_name") == name and r.get("mesh_id") == layout["mesh_id"]]
    if not candidates:
        raise ValueError("world renderer identity missing or ambiguous")
    renderer = candidates[0]
    source_mesh = graph["meshes"][renderer["mesh_id"]]
    submeshes = source_mesh.get("submeshes", [])
    vertices, count, bones = layout["vertices"], layout["indices"], layout["bindposes"]
    element_size = layout["indexElementSize"]
    if not (isinstance(vertices, int) and 0 < vertices <= 262144 and isinstance(count, int) and 6 <= count <= 1200000 and count % 3 == 0 and isinstance(bones, int) and 0 < bones <= 256 and element_size in (2,4)):
        raise ValueError("unsupported mesh counts/index encoding")
    if element_size == 2 and vertices > 65535:
        raise ValueError("UInt16 vertex count exceeds bound")
    if layout["blendshapes"] != 0:
        raise ValueError("blendshapes require another contract")
    if not 1 <= len(submeshes) <= 8 or layout["submeshes"] != len(submeshes):
        raise ValueError("submesh count differs or exceeds bound")
    draws, cursor = [], 0
    for sub in submeshes:
        if sub.get("topology") != "Triangles" or sub.get("base_vertex") != 0 or sub.get("first_byte") != cursor * element_size:
            raise ValueError("unsupported submesh topology or offsets")
        size = sub.get("index_count")
        if not isinstance(size,int) or size <= 0 or size % 3 or cursor + size > count:
            raise ValueError("invalid submesh index range")
        draws.append((cursor,size,0))
        cursor += size
    if cursor != count:
        raise ValueError("submesh ranges do not cover indices")
    head = head_palette(renderer, bones)
    # Instances sharing one Mesh must classify the same source bone indices.
    if any(head_palette(candidate, bones) != head for candidate in candidates[1:]):
        raise ValueError("world renderer head classification ambiguous")
    streams = layout["streams"]
    strides = [s["stride"] for s in streams]
    attributes = [(a["attribute"], a["format"], a["dimension"], a["stream"]) for a in layout["attributes"]]
    if (attributes, strides) not in LAYOUTS:
        raise ValueError("source vertex attributes differ")
    data = []
    for i, stream in enumerate(streams):
        expected = vertices * strides[i]
        path = raw / f"stream{i}.bin"
        if stream["length"] != expected or path.stat().st_size != expected:
            raise ValueError("source stream size differs")
        data.append(path.read_bytes())
    index_path = raw / "indices-original.bin"
    if index_path.stat().st_size != count * element_size:
        raise ValueError("source index size differs")
    original = index_path.read_bytes()
    profile = asset_profile(name)
    verify_profile_source(profile, renderer["mesh_id"], original, data)
    original_crc = zlib.crc32(original)
    if original_crc == 0:
        raise ValueError("source index CRC must be nonzero")
    index_code = "H" if element_size == 2 else "I"
    indices = list(struct.unpack(f"<{count}{index_code}", original))
    if max(indices) >= vertices:
        raise ValueError("source index exceeds vertex count")
    head_weight = []
    for vertex in range(vertices):
        rigid = strides[2] == 4
        palette = struct.unpack_from("<4B", data[2], vertex * strides[2] + (0 if rigid else 8))
        if any(index >= bones for index in palette):
            raise ValueError("source bone index exceeds bindpose count")
        if rigid:
            if any(palette[1:]):
                raise ValueError("rigid skin contains additional bone indices")
            head_weight.append(65535 if palette[0] in head else 0)
        else:
            weights = struct.unpack_from("<4H", data[2], vertex * 12)
            head_weight.append(sum(w for w, i in zip(weights, palette) if i in head))
    hidden = mixed = pure_head = profile_hidden = 0
    for index in range(0, count, 3):
        weights = [head_weight[indices[index + j]] for j in range(3)]
        matches = sum(weight > 32767 for weight in weights)
        pure_head += matches == 3
        mixed += 0 < matches < 3
        # An asset profile owns the accepted boundary semantics; the default
        # conservative rule remains identical for every unprofiled source.
        selected = matches == 3 or (profile is not None and
                                    (matches > 0 or all(weight > 0 for weight in weights)))
        if selected:
            indices[index + 1] = indices[index]
            indices[index + 2] = indices[index]
            hidden += 1
            profile_hidden += matches != 3
    if hidden == 0:
        raise ClassificationRejected("no pure head triangles", dict(pure_head_triangles=hidden, mixed_triangles=mixed, pure_body_triangles=count // 3 - hidden - mixed))
    clipped = struct.pack(f"<{count}{index_code}", *indices)
    payload = b"".join(data) + clipped
    header_size = HEADER.size + len(attributes) * ATTRIBUTE.size + len(draws) * DRAW.size + len(encoded_name)
    header = HEADER.pack(MAGIC, 3, header_size, vertices, count, bones, hidden, len(attributes), len(data), *(len(d) for d in data), *strides, zlib.crc32(payload), original_crc, len(encoded_name),element_size,len(draws))
    result = header + b"".join(ATTRIBUTE.pack(*a) for a in attributes) + b"".join(DRAW.pack(*d) for d in draws) + encoded_name + payload
    if len(result) > MAX_FILE_BYTES:
        raise ValueError("fixture exceeds 16 MiB")
    if profile and (hidden != profile.get("accepted_hidden_triangles") or
                    hashlib.sha256(result).hexdigest() != profile.get("accepted_fixture_sha256")):
        raise ProfileRejected("profile output differs from accepted fixture")
    report = dict(mesh=name, mesh_id=renderer["mesh_id"], resource_root=renderer["resource_root"], renderer_path=renderer["path"], layout=dict(attributes=attributes,strides=strides), submeshes=layout["submeshes"], blendshapes=layout["blendshapes"], vertices=vertices, indices=count, bones=bones, head_bones=len(head), pure_head_triangles=pure_head, mixed_triangles=mixed, pure_body_triangles=count // 3 - pure_head - mixed, hidden_triangles=hidden, profile_hidden_triangles=profile_hidden, retained_triangles=count // 3 - hidden, classification_profile=profile["policy"] if profile else None, original_index_crc32=original_crc, payload_crc32=zlib.crc32(payload), fixture_bytes=len(result), fixture_sha256=hashlib.sha256(result).hexdigest(), runtime_verified=False)
    return result, report


def build(raw, database, output):
    blob, report = compile_fixture(raw, bounded_json(database))
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(blob)
    return report["hidden_triangles"], len(blob)


def build_catalog(raw_root, database, output):
    graph = bounded_json(database)
    layouts = sorted(raw_root.glob("*/layout.json"))
    # Accepted profiles are mandatory for a catalog. Preflight all of them
    # before writing or removing any previous output, including early metadata
    # rejection and missing assets which cannot reach fingerprint validation.
    required = {profile["mesh"]: [] for profile in asset_profiles()}
    for path in layouts:
        try:
            name = bounded_json(path).get("mesh")
            if isinstance(name, str) and name in required:
                required[name].append(path.parent)
        except (ValueError, OSError):
            continue  # Missing required identities fail the preflight below.
    for name, sources in required.items():
        if len(sources) != 1:
            raise ProfileRejected(f"required profile source missing or ambiguous: {name}")
        compile_fixture(sources[0], graph)
    output.mkdir(parents=True, exist_ok=True)
    previous_owned = set()
    coverage_path = output / "coverage.json"
    if coverage_path.exists():
        previous = bounded_json(coverage_path)
        if not isinstance(previous, dict) or not isinstance(previous.get("included"), list):
            raise ValueError("previous coverage has invalid included list")
        for entry in previous["included"]:
            name = entry.get("mesh") if isinstance(entry, dict) else None
            if isinstance(name, str) and 1 <= len(name.encode("utf-8")) <= 160 and re.fullmatch(r"[A-Za-z0-9_]+", name):
                previous_owned.add(name)
    report = dict(schema=3, platform="Android", runtime_verified=False, database=str(database), included=[], skipped=[])
    seen = set()
    for layout_path in layouts:
        try:
            blob, entry = compile_fixture(layout_path.parent, graph)
            path = output / (entry["mesh"] + ".behw")
            if entry["mesh"] in seen:
                raise ValueError("duplicate fixture identity")
            path.write_bytes(blob)
            seen.add(entry["mesh"])
            report["included"].append(entry)
        except (ValueError, KeyError, OSError, struct.error) as error:
            if isinstance(error, ProfileRejected):
                raise
            entry = dict(mesh=layout_path.parent.name, reason=str(error))
            if isinstance(error, ClassificationRejected):
                entry.update(error.diagnostics)
            try:
                layout = bounded_json(layout_path)
                entry["mesh_id"] = layout.get("mesh_id")
                entry["layout"] = dict(attributes=layout.get("attributes"), streams=layout.get("streams"))
                entry["world_renderers"] = [dict(resource_root=r.get("resource_root"), path=r.get("path")) for r in graph["renderers"] if r.get("mesh_id") == layout.get("mesh_id") and str(r.get("resource_root", "")).endswith("_postmodel")]
            except (ValueError, KeyError, OSError):
                pass  # The original rejection is retained even if metadata cannot be read.
            report["skipped"].append(entry)
    # Previous coverage owns only its safe included names. Unknown user files
    # remain untouched, including when this output directory has no coverage.
    for name in previous_owned - seen:
        path = output / (name + ".behw")
        if path.exists():
            path.unlink()
    coverage_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--raw", type=Path, required=True)
    parser.add_argument("--database", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--catalog", action="store_true")
    args = parser.parse_args()
    if args.catalog:
        report = build_catalog(args.raw, args.database, args.output)
        print(f"included={len(report['included'])} skipped={len(report['skipped'])}")
    else:
        hidden, size = build(args.raw, args.database, args.output)
        print(f"hidden={hidden} file_bytes={size} file={args.output}")


if __name__ == "__main__":
    main()
