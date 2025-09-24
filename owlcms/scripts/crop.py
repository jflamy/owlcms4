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

# When printing install instructions for OpenCV, only show once per run
_cv2_install_message_printed = False


def is_image_file(name: str, exts: tuple) -> bool:
    return name.lower().endswith(exts)


def crop_to_ratio(img: Image.Image, tw: int, th: int, top_bias: float = 0.25, face_detect: bool = False, image_name: str | None = None) -> Image.Image:
    """Crop `img` to target ratio `tw:th`.

    When the image is taller than the target ratio (portrait images), this
    function biases the vertical crop toward the top of the image using
    `top_bias` (0.0 = top, 0.5 = center, 1.0 = bottom). By default we use
    a small top bias because faces in portrait photos commonly sit in the
    upper part of the frame.

    If `face_detect` is True and OpenCV is available, try to detect faces
    and center the crop vertically around the largest detected face.
    """
    import math
    width, height = img.size

    # Determine whether we need to reduce width (image too wide) or height (too tall)
    reduce_width = width * th > height * tw
    new_width = (height * tw) // th
    new_height = (width * th) // tw

    # Clamp top_bias to [0,1]
    top_bias = max(0.0, min(1.0, float(top_bias)))

    # Default crop: centered horizontally for wide images, top-biased vertically for tall images
    if reduce_width:
        left = (width - new_width) // 2
        top = 0
        box = (left, top, left + new_width, height)
    else:
        top = int((height - new_height) * top_bias)
        top = max(0, min(top, height - new_height))
        left = 0
        box = (left, top, width, top + new_height)

    # Attempt face detection with OpenCV if requested (apply to all images)
    if face_detect:
        name = image_name or '<image>'
        try:
            import cv2
            import numpy as np

            gray = np.array(img.convert('L'))
            cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
            clf = cv2.CascadeClassifier(cascade_path)
            faces = clf.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))
            if len(faces) > 0:
                # Pick the largest face
                faces = sorted(faces, key=lambda r: r[2] * r[3], reverse=True)
                x, y, w, h = faces[0]
                if reduce_width:
                    # center the crop horizontally on the face center
                    face_cx = x + w // 2
                    desired_left = int(face_cx - new_width // 2)
                    left = max(0, min(desired_left, width - new_width))
                    box = (left, 0, left + new_width, height)
                    print(f"Face-detect: {name} -> face at x={x},y={y},w={w},h={h}; using left={left}")
                else:
                    # vertically center the face inside the crop
                    desired_top = int(y - (new_height - h) // 2)
                    top = max(0, min(desired_top, height - new_height))
                    box = (0, top, width, top + new_height)
                    print(f"Face-detect: {name} -> face at x={x},y={y},w={w},h={h}; using top={top}")
                return img.crop(box)
            else:
                print(f"Face-detect: {name} -> no faces found; falling back to bias")
        except Exception as exc:
            # If OpenCV isn't available or detection fails, fall back to bias
            global _cv2_install_message_printed
            msg = str(exc) or ''
            if (isinstance(exc, (ImportError, ModuleNotFoundError)) or 'cv2' in msg) and not _cv2_install_message_printed:
                print("Face-detect: OpenCV not available; falling back to bias")
                print("To enable headless face detection install OpenCV and numpy:")
                print("  python -m pip install --user numpy opencv-python-headless")
                _cv2_install_message_printed = True
            else:
                print(f"Face-detect: {name} -> detection error ({exc}); falling back to bias")

    return img.crop(box)


def main(argv=None):
    parser = argparse.ArgumentParser(description="Center-crop images to a target aspect ratio")
    parser.add_argument('--target', '-t', nargs=2, type=int, metavar=('W', 'H'), default=[5, 7],
                        help='Target aspect ratio expressed as two integers (width height).')
    parser.add_argument('--input-dir', '-i', default='.', help='Directory containing images (default: current)')
    parser.add_argument('--output-dir', '-o', default='cropped_images', help='Where to save cropped images')
    parser.add_argument('--exts', nargs='*', default=['.jpg', '.jpeg', '.png'], help='File extensions to process')
    parser.add_argument('--overwrite', action='store_true', help='Overwrite existing files in output directory')
    parser.add_argument('--force', '-f', action='store_true', help='Alias for --overwrite (overwrite existing files)')
    parser.add_argument('--quality', type=int, default=95, help='JPEG quality when saving (default 95)')
    parser.add_argument('--top-bias', type=float, default=0.25, help='Vertical bias for portrait crops (0.0=top, 0.5=center)')
    parser.add_argument('--face-detect', action='store_true', help='If available, prefer cropping around detected faces (requires OpenCV)')

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
        overwrite = bool(args.overwrite or args.force)
        if os.path.exists(dst) and not overwrite:
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
                cropped = crop_to_ratio(im, tw, th, top_bias=args.top_bias, face_detect=args.face_detect, image_name=name)

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
