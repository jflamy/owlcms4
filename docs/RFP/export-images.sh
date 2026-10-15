#!/usr/bin/env bash
# Regenerate the layout diagrams in img/setup from layout.drawio.
# Usage: ./export-images.sh [svg|png|all]   (default: svg)
# Images are cropped to the visible content and rendered in light mode.
# Pages and layers are looked up by name, so reordering layers in draw.io is safe.
set -euo pipefail

cd "$(dirname "$0")"
DRAWIO="${DRAWIO:-/Applications/draw.io.app/Contents/MacOS/draw.io}"
SRC="layout.drawio"
OUT="img/setup"
PNG_WIDTH=2126
PNG_DPI=300

case "${1:-svg}" in
  svg) FORMATS=(svg) ;;
  png) FORMATS=(png) ;;
  all) FORMATS=(svg png) ;;
  *) echo "usage: $0 [svg|png|all]" >&2; exit 1 ;;
esac

mkdir -p "$OUT"

# output name | page name | layer names (phantom is always added)
DIAGRAMS=(
  "main_video|Main|computers,screens,video"
  "main_vpt|Main|vpt"
  "main_networking|Main|computers,networking"
  "main_layout|Main|computers,screens,platform,tables,distances"
  "warmup_video|Warmup|computers,screens,video"
  "warmup_networking|Warmup|computers,networking"
  "warmup_layout|Warmup|computers,screens,platform,tables,distances"
)

# Prints "<1-based page index> <comma-separated 0-based layer indexes>"
resolve() {
  python3 - "$SRC" "$1" "phantom,$2" <<'EOF'
import sys, xml.etree.ElementTree as ET
src, page_name, wanted = sys.argv[1], sys.argv[2], sys.argv[3].split(",")
diagrams = ET.parse(src).getroot().findall("diagram")
for p, d in enumerate(diagrams, 1):
    if d.get("name") != page_name:
        continue
    cells = d.find("mxGraphModel/root").findall("mxCell")
    root_id = next(c.get("id") for c in cells if c.get("parent") is None)
    layers = [c.get("value") or "" for c in cells if c.get("parent") == root_id]
    missing = [w for w in wanted if w not in layers]
    if missing:
        sys.exit(f"page {page_name}: unknown layer(s) {missing}; have {layers}")
    print(p, ",".join(str(layers.index(w)) for w in wanted))
    break
else:
    sys.exit(f"page {page_name} not found")
EOF
}

for entry in "${DIAGRAMS[@]}"; do
  IFS='|' read -r name page layers <<<"$entry"
  read -r page_index layer_indexes <<<"$(resolve "$page" "$layers")"
  for fmt in "${FORMATS[@]}"; do
    extra=(--theme light)
    [[ "$fmt" == png ]] && extra+=(--width "$PNG_WIDTH")
    "$DRAWIO" --export --format "$fmt" "${extra[@]}" --page-index "$page_index" \
      --layers "$layer_indexes" --output "$OUT/$name.$fmt" "$SRC" >/dev/null 2>&1
    if [[ "$fmt" == png ]]; then
      python3 -c '
import sys
from pathlib import Path
from PIL import Image
image_path = Path(sys.argv[1])
dpi = int(sys.argv[2])
temporary_path = image_path.with_suffix(".tmp.png")
with Image.open(image_path) as image:
    image.save(temporary_path, dpi=(dpi, dpi))
temporary_path.replace(image_path)
' "$OUT/$name.$fmt" "$PNG_DPI"
    fi
    # draw.io appends a "Text is not SVG - cannot display" fallback that Word shows even though it renders the labels
    [[ "$fmt" == svg ]] && perl -0pi -e 's{<switch><g requiredFeatures="[^"]*"/><a[^>]*><text[^>]*>Text is not SVG - cannot display</text></a></switch>}{}g' "$OUT/$name.$fmt"
    echo "$OUT/$name.$fmt  (page $page_index, layers $layer_indexes)"
  done
done
