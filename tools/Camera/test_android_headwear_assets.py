"""Packaging gate against the complete source-generated Android catalog."""
import os
from pathlib import Path
import tempfile
import shutil
import unittest

import package_android_headwear_assets as assets


@unittest.skipUnless(os.environ.get("HEADWEAR_PACKAGE_CATALOG"), "complete catalog not configured")
class AssetPackaging(unittest.TestCase):
    def test_corrupt_or_missing_fixture_rejected_before_output_changes(self):
        catalog = Path(os.environ["HEADWEAR_PACKAGE_CATALOG"])
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            copied = root / "catalog"
            shutil.copytree(catalog, copied)
            output = root / "output/headwear-v3"
            output.mkdir(parents=True)
            marker = output / "manifest.tsv"
            marker.write_bytes(b"previous valid manifest")
            source = next(copied.glob("*.behw"))
            original = source.read_bytes()
            source.write_bytes(bytes([original[0] ^ 1]) + original[1:])
            with self.assertRaisesRegex(ValueError, "fingerprint differs"):
                assets.stage(copied, output.parent)
            self.assertEqual(marker.read_bytes(), b"previous valid manifest")
            source.unlink()
            with self.assertRaisesRegex(ValueError, "identities differ"):
                assets.stage(copied, output.parent)
            self.assertEqual(list(output.iterdir()), [marker])

    def test_complete_catalog_stages_exact_bytes_and_bounded_manifest(self):
        catalog = Path(os.environ["HEADWEAR_PACKAGE_CATALOG"])
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            bundle = assets.stage(catalog, output)
            manifest = (output / "headwear-v3/manifest.tsv").read_bytes()
            self.assertTrue(manifest.startswith(f"BEHWASSETS\t1\t50\t{bundle}\n".encode()))
            self.assertEqual(len(manifest.splitlines()), 835)
            names = [row.split(b"\t")[0] for row in manifest.splitlines()[1:]]
            self.assertEqual(names, sorted(names))
            for source in catalog.glob("*.behw"):
                self.assertEqual(source.read_bytes(), (output / "headwear-v3" / source.name).read_bytes())


if __name__ == "__main__":
    unittest.main()
