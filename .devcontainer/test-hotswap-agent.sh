#!/usr/bin/env bash
# -------------------------------------------------------------
# test-hotswap-agent.sh
# Standalone verifier for downloading the correct Hotswap Agent JAR.
# Intended for local use in Git Bash (Windows) or Linux before
# committing changes to the devcontainer setup script.
#
# Features:
#   - Supports explicit version (e.g. 2.0.1) or 'latest' (default)
#   - Filters out sources / javadoc JARs
#   - Works with curl (preferred) or wget fallback
#   - Resolves ONLY from GitHub Releases (no Maven Central dependency)
#   - Verifies JAR basic signature (zip + manifest presence)
#   - Computes SHA256 of the downloaded file
#   - Idempotent: reuses existing output unless --force provided
#
# Usage:
#   ./test-hotswap-agent.sh                # download latest
#   ./test-hotswap-agent.sh 2.0.1          # specific version
#   HOTSWAP_AGENT_VERSION=2.0.1 ./test-hotswap-agent.sh
#   ./test-hotswap-agent.sh --force latest # re-download latest
#
# Exit codes:
#   0 success
#   1 usage / parameter error
#   2 download / resolution failure
#   3 integrity / validation failure
# -------------------------------------------------------------
set -euo pipefail

COLOR_RED='\033[0;31m'
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[0;33m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

log() { printf "%b[INFO ]%b %s\n" "${COLOR_BLUE}" "${COLOR_RESET}" "$*"; }
warn() { printf "%b[WARN ]%b %s\n" "${COLOR_YELLOW}" "${COLOR_RESET}" "$*"; }
err()  { printf "%b[ERROR]%b %s\n" "${COLOR_RED}" "${COLOR_RESET}" "$*" >&2; }
ok()   { printf "%b[ OK  ]%b %s\n" "${COLOR_GREEN}" "${COLOR_RESET}" "$*"; }

# ------------------------ Arg / env handling ------------------
FORCE=0
REQ_VERSION="${HOTSWAP_AGENT_VERSION:-}"  # env can override

for arg in "$@"; do
  case "$arg" in
    --force) FORCE=1 ; shift ;;
    latest)  REQ_VERSION="latest" ; shift ;;
    -h|--help)
      sed -n '1,80p' "$0" | sed 's/^# \{0,1\}//' | grep -v '^!/'; exit 0 ;;
    *)
      if [[ -z "$REQ_VERSION" ]]; then
        REQ_VERSION="$arg"; shift;
      else
        err "Unexpected extra argument: $arg"; exit 1;
      fi
      ;;
  esac
done

REQ_VERSION="${REQ_VERSION:-latest}"

# ------------------------ Resolution logic --------------------
DEFAULT_FALLBACK_VERSION="2.0.1"
OUTPUT_DIR="hotswap-test"
OUTPUT_JAR="${OUTPUT_DIR}/hotswap-agent.jar"
META_FILE="${OUTPUT_DIR}/download.meta"
mkdir -p "$OUTPUT_DIR"

have_cmd() { command -v "$1" >/dev/null 2>&1; }

fetch() {
  local url="$1" out="$2"
  if have_cmd curl; then
    curl -L --fail --silent --show-error -o "$out" "$url"
  elif have_cmd wget; then
    wget -q -O "$out" "$url"
  else
    err "Neither curl nor wget available"; return 2
  fi
}

resolve_url() {
  local version="$1"; local resolved=""; local api_json
  {
    if [[ "$version" == "latest" ]]; then
      if have_cmd curl; then
        log "Querying GitHub Releases API for latest metadata..."
        if ! api_json=$(curl -sL https://api.github.com/repos/HotswapProjects/HotswapAgent/releases); then
          warn "GitHub API call failed; falling back to ${DEFAULT_FALLBACK_VERSION}"
          version="$DEFAULT_FALLBACK_VERSION"
        else
          if echo "$api_json" | grep -qi "API rate limit exceeded"; then
            warn "GitHub API rate limit exceeded; falling back to ${DEFAULT_FALLBACK_VERSION}"
            version="$DEFAULT_FALLBACK_VERSION"
          else
            # Find first non-prerelease hotswap agent jar
            resolved=$(printf '%s' "$api_json" | jq -r '.[] | select(.prerelease == false) | .assets[] | select(.name | test("hotswap-agent-.*\\.jar$") and (test("sources|javadoc") | not)) | .browser_download_url' 2>/dev/null | head -n1 || printf '%s' "$api_json" | grep -E 'browser_download_url' | grep -E 'hotswap-agent-[0-9].*\.jar"' | grep -v -E '(sources|javadoc)' | head -n1 | cut -d '"' -f 4 || true)
            if [[ -z "$resolved" ]]; then
              warn "Could not parse latest release asset; falling back to ${DEFAULT_FALLBACK_VERSION}"
              version="$DEFAULT_FALLBACK_VERSION"
            fi
          fi
        fi
      else
        warn "curl unavailable; using pinned ${DEFAULT_FALLBACK_VERSION}"
        version="$DEFAULT_FALLBACK_VERSION"
      fi
    fi
    if [[ -z "$resolved" ]]; then
      [[ "$version" == "latest" ]] && version="$DEFAULT_FALLBACK_VERSION"
      resolved="https://github.com/HotswapProjects/HotswapAgent/releases/download/${version}/hotswap-agent-${version}.jar"
    fi
    log "Resolved URL (internal): $resolved"
  } >&2
  printf '%s' "$resolved"
}

# ------------------------ Main flow ---------------------------
log "Requested version: $REQ_VERSION (force=$FORCE)"
if [[ -f "$OUTPUT_JAR" && $FORCE -eq 0 ]]; then
  ok "Jar already exists at $OUTPUT_JAR (use --force to re-download)."
  exit 0
fi

URL=$(resolve_url "$REQ_VERSION") || { err "Failed to resolve URL"; exit 2; }
log "Resolved download URL: $URL"
TMP_JAR="${OUTPUT_JAR}.tmp"

if ! fetch "$URL" "$TMP_JAR"; then
  err "Download failed: $URL"; rm -f "$TMP_JAR"; exit 2;
fi

# Basic validation: ensure it's a zip (PK header) & contains a manifest
if ! head -c 4 "$TMP_JAR" | grep -q 'PK'; then
  err "Downloaded file does not appear to be a JAR (missing PK header)."; rm -f "$TMP_JAR"; exit 3;
fi

# Create listing (normalized) and test several strategies to avoid false negatives
unzip -l "$TMP_JAR" >"${OUTPUT_DIR}/unzip_listing.tmp" 2>/dev/null || true
FOUND_MANIFEST=0

# Strategy 1: unzip -Z1 (if available)
if command -v unzip >/dev/null 2>&1 && unzip -Z1 "$TMP_JAR" 2>/dev/null | tr -d '\r' | grep -q '^META-INF/MANIFEST.MF$'; then
  FOUND_MANIFEST=1
fi

# Strategy 2: previously captured listing
if [ $FOUND_MANIFEST -eq 0 ] && grep -F 'META-INF/MANIFEST.MF' "${OUTPUT_DIR}/unzip_listing.tmp" >/dev/null 2>&1; then
  FOUND_MANIFEST=1
fi

# Strategy 3: jar tf fallback
if [ $FOUND_MANIFEST -eq 0 ] && command -v jar >/dev/null 2>&1 && jar tf "$TMP_JAR" 2>/dev/null | tr -d '\r' | grep -q '^META-INF/MANIFEST.MF$'; then
  FOUND_MANIFEST=1
fi

if [ $FOUND_MANIFEST -eq 0 ]; then
  warn "Manifest not detected – gathering diagnostics before failing..."
  echo "---- DIAGNOSTICS (file info) ----" >&2
  if command -v file >/dev/null 2>&1; then file "$TMP_JAR" >&2; fi
  echo "---- First 32 bytes (hex) ----" >&2
  (hexdump -C "$TMP_JAR" 2>/dev/null | head -n2) >&2 || true
  echo "---- unzip -l (first 40 lines) ----" >&2
  (head -n 40 "${OUTPUT_DIR}/unzip_listing.tmp") >&2 || true
  echo "---- jar tf (first 40 lines) ----" >&2
  if command -v jar >/dev/null 2>&1; then (jar tf "$TMP_JAR" | head -n40) >&2 || true; fi
  echo "---- End diagnostics ----" >&2
  mv "$TMP_JAR" "${OUTPUT_DIR}/hotswap-agent-download-failed.jar" 2>/dev/null || true
  err "JAR missing MANIFEST.MF (saved as hotswap-agent-download-failed.jar)"; exit 3;
else
  log "Detected MANIFEST.MF in JAR"
fi

# Detect accidental sources/javadoc
if unzip -l "$TMP_JAR" | grep -qi 'source'; then
  warn "The JAR contains 'source' entries; verify it's not a sources classifier."
fi

mv "$TMP_JAR" "$OUTPUT_JAR"
SHA256=$(sha256sum "$OUTPUT_JAR" 2>/dev/null | awk '{print $1}' || shasum -a 256 "$OUTPUT_JAR" | awk '{print $1}')
SIZE=$(wc -c < "$OUTPUT_JAR")

cat > "$META_FILE" <<EOF
URL=$URL
VERSION_REQUESTED=$REQ_VERSION
SHA256=$SHA256
SIZE=$SIZE
TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

ok "Downloaded Hotswap Agent JAR"
log "Path: $OUTPUT_JAR"
log "Size: $SIZE bytes"
log "SHA256: $SHA256"
log "Meta: $META_FILE"

exit 0