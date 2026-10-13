#!/usr/bin/env bash
# Regenerate the setup diagrams in img/setup from setup.drawio.
# Usage: ./export-images.sh [svg|png|all]   (default: svg)
# Images are cropped to the visible content and rendered in light mode.
# Pages and layers are looked up by name, so reordering layers in draw.io is safe.
set -euo pipefail

cd "$(dirname "$0")"
DRAWIO="${DRAWIO:-/Applications/draw.io.app/Contents/MacOS/draw.io}"
SRC="setup.drawio"
OUT="img/setup"
PNG_SCALE="${PNG_SCALE:-3}"

case "${1:-svg}" in
  svg) FORMATS=(svg) ;;
  png) FORMATS=(png) ;;
  all) FORMATS=(svg png) ;;
  *) echo "usage: $0 [svg|png|all]" >&2; exit 1 ;;
esac

mkdir -p "$OUT"

# output name | page name | layer names (phantom is always added)
DIAGRAMS=(
  "main_attempts|Main|computers,screens,Attempt HDMI"
  "main_scoreboard|Main|computers,screens,Scoreboard HDMI"
  "main_video|Main|computers,screens,Attempt HDMI,Scoreboard HDMI"
  "main_networking|Main|computers,networking"
  "main_layout|Main|computers,screens,platform,tables,distances"
  "warmup_attempts|Warmup|computers,screens,Attempt HDMI"
  "warmup_scoreboards|Warmup|computers,screens,Scoreboard HDMI"
  "warmup_video|Warmup|computers,screens,Attempt HDMI,Scoreboard HDMI"
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
    [[ "$fmt" == png ]] && extra+=(--scale "$PNG_SCALE")
    "$DRAWIO" --export --format "$fmt" "${extra[@]}" --page-index "$page_index" \
      --layers "$layer_indexes" --output "$OUT/$name.$fmt" "$SRC" >/dev/null 2>&1
    # draw.io appends a "Text is not SVG - cannot display" fallback that Word shows even though it renders the labels
    [[ "$fmt" == svg ]] && perl -0pi -e 's{<switch><g requiredFeatures="[^"]*"/><a[^>]*><text[^>]*>Text is not SVG - cannot display</text></a></switch>}{}g' "$OUT/$name.$fmt"
    echo "$OUT/$name.$fmt  (page $page_index, layers $layer_indexes)"
  done
done
