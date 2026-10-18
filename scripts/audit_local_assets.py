#!/usr/bin/env python3
"""Read-only local audit of original Crusoe and modern sprite files.

Usage:
  python3 scripts/audit_local_assets.py /path/to/original.jar /path/to/modern.jar

The input JARs are never copied, committed or uploaded.
Only standard-library modules are required.
"""
import argparse
import io
import json
import struct
import zipfile
from pathlib import Path


def dimensions(png):
    if png[:8] != b"\x89PNG\r\n\x1a\n" or png[12:16] != b"IHDR":
        raise ValueError("Not a valid PNG header")
    return struct.unpack_from(">II", png, 16)


def audit(game_path, hd_path):
    checks = {}
    with zipfile.ZipFile(game_path) as original, zipfile.ZipFile(hd_path) as modern:
        counts = struct.unpack_from("<15H", original.read("data/D0"), 0)
        checks["BS3 has 20 animation frames"] = counts[3] == 20
        checks["original BS3 atlas is 348x36"] = dimensions(original.read("data/BS3.png")) == (348, 36)
        checks["original five map layers exist"] = all(
            name in original.namelist() for name in (
                "BIGislandFULL.byt", "BIGislandRocks.byt", "BIGislandDecor.byt",
                "BIGislandBorders.byt", "BIGislandObjects.byt"))
        meta = original.read("data/D0")
        pointer = 30 + 12 * sum(counts[:3])
        ratios = []
        source_rects = []
        modern_sizes = []
        for frame in range(counts[3]):
            x, y, raw_width, raw_height, anchor_x, anchor_y = struct.unpack_from(
                "<6h", meta, pointer)
            pointer += 12
            width, height = raw_width & 0x7FFF, raw_height & 0x7FFF
            source_rects.append((x, y, width, height))
            name = "crusoe_hd/BS03_{:03d}.png".format(frame)
            if name not in modern.namelist():
                continue
            hd_size = dimensions(modern.read(name))
            modern_sizes.append(hd_size)
            ratios.append((height * hd_size[0] / hd_size[1]) / width)
        checks["original crop rectangles are in atlas bounds"] = all(
            x >= 0 and y >= 0 and w > 0 and h > 0
            and x + w <= 348 and y + h <= 36 for x, y, w, h in source_rects)
        checks["all 20 modern images exist and are PNGs"] = len(modern_sizes) == 20
    result = {
        "checks": checks,
        "legacy_frame_count": counts[3],
        "modern_frame_count": len(modern_sizes),
        "visual_warning": "Modern frames have a wider natural aspect ratio than legacy sprite slots; use a centered, anchored HD draw, not a stretched replacement.",
        "projected_visual_width_factor_range": [
            round(min(ratios), 2), round(max(ratios), 2)] if ratios else [],
    }
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("original_jar", type=Path)
    parser.add_argument("modern_resource_jar", type=Path)
    args = parser.parse_args()
    result = audit(args.original_jar, args.modern_resource_jar)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if all(result["checks"].values()) else 1


if __name__ == "__main__":
    raise SystemExit(main())
