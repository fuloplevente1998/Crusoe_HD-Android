"""Fidelity and corrupt-input checks using only synthetic JARs and pixels."""
import importlib.util
import struct
import tempfile
import unittest
import zipfile
import zlib
from pathlib import Path

spec = importlib.util.spec_from_file_location('audit', Path(__file__).parents[1] / 'scripts/audit_local_assets.py')
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


def png(width, height):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>2I5B', width, height, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress((b'\0' + b'\xff\0\0\xff' * width) * height)) + chunk(b'IEND', b'')


class ResourceAuditTest(unittest.TestCase):
    def fixture(self, root, mutation=None):
        original = root / 'original.jar'
        modern = root / 'modern.jar'
        counts = [0, 0, 0, 20] + [0] * 11
        entries = {'Game.class': b'synthetic-bytecode', 'data/BS3.png': png(348, 36),
                   'data/D0': struct.pack('<15H', *counts) + struct.pack('<6h', 0, 0, 19, 36, 0, 0) * 20}
        for layer in ['FULL', 'Rocks', 'Decor', 'Borders', 'Objects']:
            entries['BIGisland' + layer + '.byt'] = b'\x01' * (180 * 180)
        with zipfile.ZipFile(original, 'w') as jar:
            for name, data in entries.items():
                jar.writestr(name, data)
        entries.update({'crusoe_hd/BS03_%03d.png' % i: png(64, 64) for i in range(20)})
        if mutation:
            mutation(entries)
        with zipfile.ZipFile(modern, 'w') as jar:
            for name, data in entries.items():
                jar.writestr(name, data)
        return original, modern

    def test_unchanged_logic_and_complete_optional_art_pass(self):
        with tempfile.TemporaryDirectory() as root:
            result = audit.audit(*self.fixture(Path(root)))
            self.assertTrue(all(result['checks'].values()))
            self.assertEqual(result['unchanged_original_class_count'], 1)

    def test_modified_game_logic_fails(self):
        with tempfile.TemporaryDirectory() as root:
            result = audit.audit(*self.fixture(Path(root), lambda e: e.update({'Game.class': b'changed'})))
            self.assertFalse(result['checks']['all original game classes are byte-identical'])

    def test_modified_map_fails(self):
        with tempfile.TemporaryDirectory() as root:
            result = audit.audit(*self.fixture(Path(root), lambda e: e.update({'BIGislandFULL.byt': b'changed'})))
            self.assertFalse(result['checks']['all original data and graphics are byte-identical'])

    def test_missing_frame_fails(self):
        with tempfile.TemporaryDirectory() as root:
            result = audit.audit(*self.fixture(Path(root), lambda e: e.pop('crusoe_hd/BS03_019.png')))
            self.assertFalse(all(result['checks'].values()))

    def test_truncated_image_fails(self):
        with self.assertRaises(ValueError):
            audit.dimensions(png(64, 64)[:32])

    def test_invalid_pixel_chunk_checksum_fails(self):
        data = bytearray(png(64, 64))
        data[45] ^= 1
        with self.assertRaises(ValueError):
            audit.dimensions(bytes(data))


if __name__ == '__main__':
    unittest.main()
