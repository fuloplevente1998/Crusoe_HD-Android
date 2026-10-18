#!/usr/bin/env python3
"""Read-only local audit of original Crusoe and modern sprite files.

Usage:
  python3 scripts/audit_local_assets.py /path/to/original.jar /path/to/modern.jar

The input JARs are never copied, committed or uploaded.
Only standard-library modules are required.
"""
import argparse
import hashlib
import io
import json
import struct
import zipfile
import zlib
from pathlib import Path


def dimensions(png):
    if png[:8] != b"\x89PNG\r\n\x1a\n" or png[12:16] != b"IHDR":
        raise ValueError("Not a valid PNG header")
    width, height = struct.unpack_from(">II", png, 16)
    if not (0 < width <= 4096 and 0 < height <= 4096):
        raise ValueError("Invalid or unexpectedly large PNG dimensions")
    offset, has_data = 8, False
    while offset + 12 <= len(png):
        length = struct.unpack_from(">I", png, offset)[0]
        end = offset + 12 + length
        if end > len(png):
            raise ValueError("Truncated PNG chunk")
        kind = png[offset + 4:offset + 8]
        checksum = struct.unpack_from(">I", png, end - 4)[0]
        if zlib.crc32(png[offset + 4:end - 4]) != checksum:
            raise ValueError("Invalid PNG chunk checksum")
        has_data |= kind == b"IDAT" and length > 0
        if kind == b"IEND":
            if length or not has_data or end != len(png):
                raise ValueError("Invalid PNG ending")
            return width, height
        offset = end
    raise ValueError("PNG has no IEND chunk")


def audit(game_path, hd_path):
    checks = {}
    with zipfile.ZipFile(game_path) as original, zipfile.ZipFile(hd_path) as modern:
        original_names = {entry.filename for entry in original.infolist() if not entry.is_dir()}
        modern_names = {entry.filename for entry in modern.infolist() if not entry.is_dir()}
        classes = {name for name in original_names if name.endswith(".class")}
        changed = sorted(name for name in original_names
                         if name not in modern_names or original.read(name) != modern.read(name))
        checks["all original game classes are byte-identical"] = not classes.intersection(changed)
        checks["all original data and graphics are byte-identical"] = not any(
            not name.startswith("META-INF/") for name in changed)
        checks["resource package has no additional game classes"] = not any(
            name.endswith(".class") for name in modern_names - original_names)
        checks["resource package contains no duplicate entry names"] = len(modern.namelist()) == len(set(modern.namelist()))
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
        checks["all 20 modern images have valid PNG headers and chunk checksums"] = len(modern_sizes) == 20
    result = {
        "checks": checks,
        "original_sha256": hashlib.sha256(game_path.read_bytes()).hexdigest(),
        "modern_sha256": hashlib.sha256(hd_path.read_bytes()).hexdigest(),
        "changed_or_missing_original_entries": changed,
        "unchanged_original_class_count": len(classes - set(changed)),
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
    try:
        result = audit(args.original_jar, args.modern_resource_jar)
    except (ValueError, KeyError, OSError, struct.error, zipfile.BadZipFile) as error:
        print(json.dumps({"error": str(error)}, ensure_ascii=False, indent=2))
        return 1
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if all(result["checks"].values()) else 1


if __name__ == "__main__":
    raise SystemExit(main())
