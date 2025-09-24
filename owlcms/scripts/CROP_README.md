# CROP_README.md

Overview
--------
`crop.py` center-crops images to a target aspect ratio and saves results to an output directory.
It aims to match behavior from `crop.sh` but without requiring `bc`.

Quick facts
- Default target ratio: 5:7
- Default input dir: current working directory
- Default output dir: `cropped_images/`
- Requires Pillow; optional: NumPy + OpenCV (face detection), piexif (preserve EXIF)

Install dependencies (user-level, no virtualenv)
-----------------------------------------------

Pillow (required)
```bash
python -m pip install --user pillow
```

Optional (face detection)
```bash
python -m pip install --user numpy opencv-python-headless
```

Optional (preserve EXIF in JPEGs)
```bash
python -m pip install --user piexif
```

Verify installations
```bash
python -c "import PIL; print('Pillow OK')"
python -c "import cv2; print('OpenCV OK')"          # optional
python -c "import piexif; print('piexif OK')"       # optional
```

Usage
-----
Run from the command line:

Basic
```bash
python crop.py
```

Custom input/output and ratio
```bash
python crop.py --target 4 3 --input-dir photos --output-dir out
```

Show help
```bash
python crop.py --help
```

Options (matching crop.py)
--------------------------
- `--target -t W H`  
  Target aspect ratio as two integers, width then height. Default: `5 7`.

- `--input-dir -i DIR`  
  Directory containing images. Default: `.`

- `--output-dir -o DIR`  
  Directory to save cropped images. Default: `cropped_images`

- `--exts EXT [EXT ...]`  
  File extensions to process (e.g. `.jpg .png`). Default: `.jpg .jpeg .png`

- `--overwrite`  
  Overwrite existing files in output directory.

- `--force -f`  
  Alias for `--overwrite`.

- `--quality N`  
  JPEG quality when saving (0-100). Default: `95`.

- `--top-bias`  
  Vertical bias for portrait crops. Float in [0.0,1.0]. Default: `0.25` (slightly top biased).

- `--face-detect`  
  If available, prefer cropping around detected faces (requires OpenCV).

Behavior notes
--------------
- The script applies EXIF orientation automatically (via Pillow) so images are not rotated unexpectedly.
- For portrait images, the crop is biased vertically (`--top-bias`) to favor faces near the top; enabling `--face-detect` will attempt to center on the largest detected face.
- If `piexif` is installed and image is JPEG with EXIF, the Orientation tag is removed from the saved image to avoid double-rotation.
- Non-image files and invalid images are skipped.
- The output directory will be created if missing.

Examples
--------
Crop current directory to default 5:7:
```bash
python crop.py
```

Crop photos to 4:3 and save to `out/`:
```bash
python crop.py --target 4 3 -i photos -o out
```

Use face detection and overwrite:
```bash
python crop.py --face-detect --overwrite
```

Troubleshooting
---------------
- If face detection prints an OpenCV/import error, install `numpy` and `opencv-python-headless` with the command above.
- If images appear rotated, ensure EXIF orientation is present and Pillow version is recent (Pillow handles exif_transpose()).

See the script help for complete and up-to-date option descriptions:
```bash
python crop.py --help
```
```// filepath: c:\Dev\git\owlcms_v23\owlcms\scripts\CROP_README.md
# CROP_README.md

Overview
--------
`crop.py` center-crops images to a target aspect ratio and saves results to an output directory.
It aims to match behavior from `crop.sh` but without requiring `bc`.

Quick facts
- Default target ratio: 5:7
- Default input dir: current working directory
- Default output dir: `cropped_images/`
- Requires Pillow; optional: NumPy + OpenCV (face detection), piexif (preserve EXIF)

Install dependencies (user-level, no virtualenv)
-----------------------------------------------
Pillow (required)
```bash
python -m pip install --user pillow
```

Optional (face detection)
```bash
python -m pip install --user numpy opencv-python-headless
```

Optional (preserve EXIF in JPEGs)
```bash
python -m pip install --user piexif
```

Verify installations
```bash
python -c "import PIL; print('Pillow OK')"
python -c "import cv2; print('OpenCV OK')"          # optional
python -c "import piexif; print('piexif OK')"       # optional
```

Usage
-----
Run from the command line:

Basic
```bash
python crop.py
```

Custom input/output and ratio
```bash
python crop.py --target 4 3 --input-dir photos --output-dir out
```

Show help
```bash
python crop.py --help
```

Options (matching crop.py)
--------------------------
- `--target -t W H`  
  Target aspect ratio as two integers, width then height. Default: `5 7`.

- `--input-dir -i DIR`  
  Directory containing images. Default: `.`

- `--output-dir -o DIR`  
  Directory to save cropped images. Default: `cropped_images`

- `--exts EXT [EXT ...]`  
  File extensions to process (e.g. `.jpg .png`). Default: `.jpg .jpeg .png`

- `--overwrite`  
  Overwrite existing files in output directory.

- `--force -f`  
  Alias for `--overwrite`.

- `--quality N`  
  JPEG quality when saving (0-100). Default: `95`.

- `--top-bias`  
  Vertical bias for portrait crops. Float in [0.0,1.0]. Default: `0.25` (slightly top biased).

- `--face-detect`  
  If available, prefer cropping around detected faces (requires OpenCV).

Behavior notes
--------------
- The script applies EXIF orientation automatically (via Pillow) so images are not rotated unexpectedly.
- For portrait images, the crop is biased vertically (`--top-bias`) to favor faces near the top; enabling `--face-detect` will attempt to center on the largest detected face.
- If `piexif` is installed and image is JPEG with EXIF, the Orientation tag is removed from the saved image to avoid double-rotation.
- Non-image files and invalid images are skipped.
- The output directory will be created if missing.

Examples
--------
Crop current directory to default 5:7:
```bash
python crop.py
```

Crop photos to 4:3 and save to `out/`:
```bash
python crop.py --target 4 3 -i photos -o out
```

Use face detection and overwrite:
```bash
python crop.py --face-detect --overwrite
```

Troubleshooting
---------------
- If face detection prints an OpenCV/import error, install `numpy` and `opencv-python-headless` with the command above.
- If images appear rotated, ensure EXIF orientation is present and Pillow version is recent (Pillow handles exif_transpose()).

See the script help for complete and up-to-date option descriptions:
```bash
python crop.py --help
```