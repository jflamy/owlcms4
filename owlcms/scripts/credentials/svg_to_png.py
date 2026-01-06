#!/usr/bin/env python3
"""
Convert SVG files to PNG files recursively, preserving directory structure using Inkscape CLI.

Requirements:
- Python 3.7+
- Inkscape CLI available on PATH (install Inkscape from https://inkscape.org)

Usage examples:

# Convert ./flags (source) to sibling ./flags_png
python scripts/svg_to_png.py ./flags

# Specify explicit output directory
python scripts/svg_to_png.py ./flags ./flags_png

# Force overwrite existing PNGs, convert with scale 2.0
python scripts/svg_to_png.py ./flags ./flags_png --overwrite --scale 2.0

# Convert with fixed width (pixels)
python scripts/svg_to_png.py ./flags ./flags_png --width 512

"""

from __future__ import annotations
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
import sys
import io
import os
import time

import shutil
import subprocess

# we use Inkscape CLI for rasterization to avoid native cairo dependencies



def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Recursively convert SVG files to PNG preserving structure")
    p.add_argument("source", help="Source directory containing .svg files")
    p.add_argument("dest", nargs="?", help="Destination directory (optional). If omitted, a sibling named <source>_png is created")
    p.add_argument("--overwrite", "-o", action="store_true", help="Overwrite existing PNG files")
    p.add_argument("--scale", type=float, default=1.0, help="Scale multiplier for rasterization (default: 1.0)")
    p.add_argument("--width", type=int, default=None, help="Output width in pixels (overrides scale if provided)")
    p.add_argument("--height", type=int, default=None, help="Output height in pixels (overrides scale if provided)")
    p.add_argument("--max-dimension", type=int, default=None, help="Maximum pixel dimension for the largest side; preserves aspect ratio")
    p.add_argument("--workers", "-w", type=int, default=4, help="Number of worker threads (default: 4)")
    p.add_argument("--quiet", "-q", action="store_true", help="Quiet mode; minimal output")
    p.add_argument("--inkscape-path", help="Path to inkscape executable or folder containing it")
    return p.parse_args()


def find_svgs(source_dir: Path):
    for p in source_dir.rglob("*.svg"):
        if p.is_file():
            yield p


def should_convert(src: Path, dst: Path, overwrite: bool) -> bool:
    if overwrite:
        return True
    if not dst.exists():
        return True
    try:
        return src.stat().st_mtime > dst.stat().st_mtime
    except OSError:
        return True


def _detect_svg_ratio(src: Path) -> tuple[float, float] | None:
    """Return (w, h) from viewBox or width/height attributes if possible, otherwise None."""
    try:
        import xml.etree.ElementTree as ET
        tree = ET.parse(src)
        root = tree.getroot()
        # SVG namespace handling
        tag = root.tag
        # get viewBox
        vb = root.get('viewBox')
        if vb:
            parts = vb.replace(',', ' ').split()
            if len(parts) >= 4:
                vb_w = float(parts[2])
                vb_h = float(parts[3])
                return vb_w, vb_h
        # try width/height attributes
        w = root.get('width')
        h = root.get('height')
        if w and h:
            def parse_val(v: str) -> float:
                # strip units like px, pt, mm
                import re
                m = re.match(r"([0-9.]+)", v)
                return float(m.group(1)) if m else None
            wv = parse_val(w)
            hv = parse_val(h)
            if wv and hv:
                return float(wv), float(hv)
    except Exception:
        return None
    return None


def convert_one(src: Path, dst: Path, width: int | None, height: int | None, scale: float, overwrite: bool, inkscape_path: str, max_dimension: int | None) -> tuple[Path, bool, str]:
    """Convert a single SVG to PNG using Inkscape CLI. Returns (src, success, message)."""
    if not should_convert(src, dst, overwrite):
        return src, True, "skipped (up-to-date)"
    dst.parent.mkdir(parents=True, exist_ok=True)
    # Determine which export dimension to pass to inkscape so aspect ratio is preserved
    use_width = width
    use_height = height
    if max_dimension is not None and width is None and height is None:
        dims = _detect_svg_ratio(src)
        if dims:
            wv, hv = dims
            if wv > hv:
                use_width = max_dimension
            else:
                use_height = max_dimension
        else:
            # fallback: set width to max_dimension
            use_width = max_dimension

    # Build inkscape command
    cmd = [inkscape_path]
    # new CLI (Inkscape >=1.0) uses --export-filename and accepts --export-width/height
    cmd += [str(src)]
    cmd += ["--export-filename", str(dst)]
    if use_width is not None:
        cmd += ["--export-width", str(int(use_width))]
    if use_height is not None:
        cmd += ["--export-height", str(int(use_height))]
    # if width/height not provided and scale != 1.0, calculate scale via DPI? we skip scale

    try:
        proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
        if proc.returncode == 0:
            # set mtime to match source
            os.utime(dst, (src.stat().st_atime, src.stat().st_mtime))
            return src, True, f"converted -> {dst}"
        else:
            return src, False, f"inkscape failed (code {proc.returncode}): {proc.stderr.decode(errors='replace')}"
    except FileNotFoundError as e:
        return src, False, f"inkscape not found: {e}"
    except Exception as e:
        return src, False, f"error: {e}"


def main():
    args = parse_args()
    # find inkscape
    # Determine inkscape executable path: CLI arg > INKSCAPE env > PATH > common install locations
    inkscape_path = None
    if args.inkscape_path:
        inkscape_path = args.inkscape_path
    if not inkscape_path:
        inkscape_path = os.environ.get('INKSCAPE')
    if not inkscape_path:
        inkscape_path = shutil.which('inkscape') or shutil.which('inkscape.exe')
    # if user gave a directory, try to use inkscape.exe inside it
    if inkscape_path:
        p = Path(inkscape_path)
        if p.is_dir():
            candidate = p / 'inkscape.exe'
            if candidate.exists():
                inkscape_path = str(candidate)
    # try common Windows install locations as a final fallback
    if not inkscape_path:
        # hard-code the typical Windows install path
        hardcoded = r"C:\Program Files\Inkscape\bin\inkscape.exe"
        if Path(hardcoded).exists():
            inkscape_path = hardcoded
    if not inkscape_path:
        print("Inkscape CLI not found. Provide --inkscape-path or add Inkscape to PATH.", file=sys.stderr)
        sys.exit(2)

    source = Path(args.source).resolve()
    if not source.exists() or not source.is_dir():
        print(f"Source directory not found or not a directory: {source}", file=sys.stderr)
        sys.exit(2)

    if args.dest:
        dest = Path(args.dest).resolve()
    else:
        # sibling named <source>_png inside same parent
        dest = source.with_name(source.name + "_png")

    start = time.time()
    svgs = list(find_svgs(source))
    if not args.quiet:
        print(f"Found {len(svgs)} .svg files under {source}")
        print(f"Destination directory: {dest}")

    tasks = []
    with ThreadPoolExecutor(max_workers=args.workers) as ex:
        futures = {}
        for svg in svgs:
            rel = svg.relative_to(source)
            out_png = dest.joinpath(rel).with_suffix('.png')
            futures[ex.submit(convert_one, svg, out_png, args.width, args.height, args.scale, args.overwrite, inkscape_path, args.max_dimension)] = svg

        converted = 0
        skipped = 0
        failed = 0
        for fut in as_completed(futures):
            src, ok, msg = fut.result()
            if args.quiet:
                continue
            if ok:
                if msg.startswith('skipped'):
                    skipped += 1
                else:
                    converted += 1
                print(f"{src} -> {msg}")
            else:
                failed += 1
                print(f"{src} -> {msg}")

    elapsed = time.time() - start
    if not args.quiet:
        print(f"Done: converted={converted}, skipped={skipped}, failed={failed} in {elapsed:.1f}s")


if __name__ == '__main__':
    main()
