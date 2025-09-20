#!/usr/bin/env python3
"""
crop.py

Crop images to a target aspect ratio (center-crop) and save to an output
directory. This reproduces the logic from `crop.sh` without requiring `bc`.

Usage:
  python crop.py            # runs in current dir, target 5:7, saves to cropped_images/
  python crop.py --target 4 3 --input-dir input --output-dir out

Requires: Pillow (`pip install pillow`)
"""

from __future__ import annotations
import argparse
import os
import sys
from PIL import Image, ImageOps


def is_image_file(name: str, exts: tuple) -> bool:
    return name.lower().endswith(exts)


def center_crop_to_ratio(img: Image.Image, tw: int, th: int) -> Image.Image:
    width, height = img.size
    if width * th > height * tw:
        # too wide -> reduce width
        new_width = (height * tw) // th
        left = (width - new_width) // 2
        box = (left, 0, left + new_width, height)
    else:
        # too tall -> reduce height
        new_height = (width * th) // tw
        top = (height - new_height) // 2
        box = (0, top, width, top + new_height)
    return img.crop(box)


def main(argv=None):
    parser = argparse.ArgumentParser(description="Center-crop images to a target aspect ratio")
    parser.add_argument('--target', '-t', nargs=2, type=int, metavar=('W', 'H'), default=[5, 7],
                        help='Target aspect ratio expressed as two integers (width height).')
    parser.add_argument('--input-dir', '-i', default='.', help='Directory containing images (default: current)')
    parser.add_argument('--output-dir', '-o', default='cropped_images', help='Where to save cropped images')
    parser.add_argument('--exts', nargs='*', default=['.jpg', '.jpeg', '.png'], help='File extensions to process')
    parser.add_argument('--overwrite', action='store_true', help='Overwrite existing files in output directory')
    parser.add_argument('--quality', type=int, default=95, help='JPEG quality when saving (default 95)')

    args = parser.parse_args(argv)

    tw, th = args.target
    input_dir = os.path.abspath(args.input_dir)
    output_dir = os.path.abspath(args.output_dir)
    exts = tuple(e.lower() for e in args.exts)

    if not os.path.isdir(input_dir):
        print(f"Input directory does not exist: {input_dir}")
        sys.exit(2)

    os.makedirs(output_dir, exist_ok=True)

    files = sorted(os.listdir(input_dir))
    processed = 0

    for name in files:
        if not is_image_file(name, exts):
            continue
        src = os.path.join(input_dir, name)
        dst = os.path.join(output_dir, name)
        if os.path.exists(dst) and not args.overwrite:
            print(f"Skipping (exists): {name}")
            continue

        try:
            with Image.open(src) as im_raw:
                # Apply EXIF-based orientation so images are not rotated unexpectedly
                im = ImageOps.exif_transpose(im_raw)
                width, height = im.size
                if not (isinstance(width, int) and isinstance(height, int) and width > 0 and height > 0):
                    print(f"⚠️  Skipping {name} (invalid dimensions: {width}x{height})")
                    continue

                print(f"Processing {name} — {width}x{height}")
                cropped = center_crop_to_ratio(im, tw, th)

                save_kwargs = {}
                fmt = im.format if im.format else 'JPEG'
                if fmt.upper() in ('JPEG', 'JPG'):
                    save_kwargs['quality'] = args.quality
                    save_kwargs['subsampling'] = 0

                # Try to preserve EXIF metadata for JPEGs while removing the
                # Orientation tag (so viewers don't rotate again). This requires
                # the optional `piexif` package. If it's not available we fall
                # back to saving without EXIF.
                exif_bytes = None
                try:
                    import piexif
                    if fmt.upper() in ('JPEG', 'JPG') and 'exif' in im_raw.info:
                        exif_dict = piexif.load(im_raw.info['exif'])
                        # Remove Orientation tag if present
                        try:
                            if piexif.ImageIFD.Orientation in exif_dict.get('0th', {}):
                                del exif_dict['0th'][piexif.ImageIFD.Orientation]
                        except Exception:
                            pass
                        exif_bytes = piexif.dump(exif_dict)
                except Exception:
                    exif_bytes = None

                if exif_bytes:
                    cropped.save(dst, format=fmt, exif=exif_bytes, **save_kwargs)
                else:
                    cropped.save(dst, format=fmt, **save_kwargs)
                processed += 1
        except Exception as e:
            print(f"Error processing {name}: {e}")

    print(f"✅ Processed {processed} image(s). Saved to {output_dir}")


if __name__ == '__main__':
    main()
