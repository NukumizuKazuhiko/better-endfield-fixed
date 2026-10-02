"""Fetch the pinned public headwear catalog used by Android CI builds."""

import argparse
import hashlib
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile


REPOSITORY = "NukumizuKazuhiko/better-endfield-fixed"
RELEASE_TAG = "v3.3.22-alpha.21"
ASSET_NAME = "headwear-catalog-v3-834.zip"
ARCHIVE_SHA256 = "beaf2135063c962d382129098b65a3779d18adf515ebdac1fbd292e7b4644a78"
EXPECTED_FILES = 835  # 834 fixtures and coverage.json
MAX_UNCOMPRESSED_BYTES = 100 * 1024 * 1024


def fetch(output: Path) -> Path:
    output = output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    if output.exists() and any(output.iterdir()):
        raise ValueError(f"headwear output must be empty: {output}")

    with tempfile.TemporaryDirectory(prefix="headwear-download-", dir=output.parent) as temp:
        archive = Path(temp) / ASSET_NAME
        subprocess.run(
            ["gh", "release", "download", RELEASE_TAG, "--repo", REPOSITORY,
             "--pattern", ASSET_NAME, "--dir", temp],
            check=True,
        )
        with archive.open("rb") as stream:
            digest = hashlib.file_digest(stream, "sha256").hexdigest()
        if digest != ARCHIVE_SHA256:
            raise ValueError(f"headwear archive SHA-256 mismatch: {digest}")

        with zipfile.ZipFile(archive) as package:
            files = [entry for entry in package.infolist() if not entry.is_dir()]
            names = [entry.filename for entry in files]
            if (len(files) != EXPECTED_FILES or len(set(names)) != EXPECTED_FILES
                    or "catalog-bundled/coverage.json" not in names
                    or sum(entry.file_size for entry in files) > MAX_UNCOMPRESSED_BYTES):
                raise ValueError("headwear archive has an invalid catalog inventory")
            for entry in files:
                name = entry.filename
                leaf = name.removeprefix("catalog-bundled/")
                if (not name.startswith("catalog-bundled/")
                        or not (leaf == "coverage.json" or re.fullmatch(r"[A-Za-z0-9_]{1,160}\.behw", leaf))
                        or entry.file_size > 16 * 1024 * 1024
                        or (entry.external_attr >> 16) & 0o170000 == 0o120000):
                    raise ValueError(f"unsafe headwear archive entry: {name}")

            catalog = output / "catalog-bundled"
            catalog.mkdir(parents=True, exist_ok=True)
            for entry in files:
                target = catalog / Path(entry.filename).name
                with package.open(entry) as source, target.open("xb") as sink:
                    shutil.copyfileobj(source, sink)
    print(f"headwear catalog: {catalog} ({EXPECTED_FILES} files, sha256:{ARCHIVE_SHA256})")
    return catalog


if __name__ == "__main__":
    parser = argparse.ArgumentParser(__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    fetch(args.output)
