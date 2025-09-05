#!/usr/bin/env bash
# -------------------------------------------------------------
# test-maven-download.sh
# Diagnostic script to explore Maven availability sources.
# Goal:
#   Verify obtaining the FULL Maven binary distribution (not individual jars) from Maven Central
#   and (optionally) install it under /opt similar to setup-jdk.sh logic.
#
# Usage:
#   ./test-maven-download.sh                # uses default version 3.9.11
#   ./test-maven-download.sh 3.9.10         # specify version
#   MAVEN_VERSION=3.9.11 ./test-maven-download.sh
#   ./test-maven-download.sh --json         # machine-readable JSON summary to stdout
#   ./test-maven-download.sh --keep         # keep test directory after completion
#   ./test-maven-download.sh --json --keep  # both options
#
# Exit codes:
#   0 success (script executed; artifacts probed; results reported)
#   2 download / extraction failure
#   3 validation failure
#
# Notes:
#   Maven binary distributions are available on Maven Central and Apache mirrors.
# -------------------------------------------------------------
set -euo pipefail

COLOR_RED='\033[0;31m'
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[0;33m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

log()  { printf "%b[INFO ]%b %s\n"   "$COLOR_BLUE"   "$COLOR_RESET" "$*"; }
warn() { printf "%b[WARN ]%b %s\n"   "$COLOR_YELLOW" "$COLOR_RESET" "$*"; }
err()  { printf "%b[ERROR]%b %s\n"  "$COLOR_RED"   "$COLOR_RESET" "$*" >&2; }
ok()   { printf "%b[ OK  ]%b %s\n"   "$COLOR_GREEN" "$COLOR_RESET" "$*"; }

JSON_MODE=0
KEEP_FILES=0
REQ_VERSION="${MAVEN_VERSION:-}" # env override

for arg in "$@"; do
  case "$arg" in
    --json) JSON_MODE=1 ; shift ;;
    --keep) KEEP_FILES=1 ; shift ;;
    -h|--help)
      sed -n '1,120p' "$0" | sed 's/^# \{0,1\}//' | grep -v '^!/' ; exit 0 ;;
    *)
      if [[ -z "$REQ_VERSION" ]]; then REQ_VERSION="$arg"; shift; else err "Unexpected extra arg: $arg"; exit 1; fi ;;
  esac
done

REQ_VERSION=${REQ_VERSION:-3.9.11}

have() { command -v "$1" >/dev/null 2>&1; }

fetch_head() {
  local url="$1"
  if have curl; then
    curl -s -o /dev/null -w '%{http_code}' -I -L "$url" || echo "000"
  elif have wget; then
    # wget exits non-zero on 404 so capture output quietly
    wget --spider -q "$url" && echo 200 || echo 404
  else
    echo "000"
  fi
}

download() {
  local url="$1" out="$2"
  if have curl; then curl -L --fail --silent --show-error -o "$out" "$url"; elif have wget; then wget -q -O "$out" "$url"; else return 2; fi
}

log "Testing Maven version: $REQ_VERSION"

DIST_TGZ="apache-maven-${REQ_VERSION}-bin.tar.gz"
PRIMARY_URL="https://dlcdn.apache.org/maven/maven-3/${REQ_VERSION}/binaries/${DIST_TGZ}"
FALLBACK_URL="https://repo1.maven.org/maven2/org/apache/maven/apache-maven/${REQ_VERSION}/${DIST_TGZ}"

HTTP_PRIMARY=$(fetch_head "$PRIMARY_URL")
if [[ "$HTTP_PRIMARY" == 200 ]]; then
  SELECTED_URL="$PRIMARY_URL"; ok "Found distribution on Apache CDN";
else
  warn "Primary CDN returned $HTTP_PRIMARY; trying Maven Central..."
  HTTP_FALLBACK=$(fetch_head "$FALLBACK_URL")
  if [[ "$HTTP_FALLBACK" == 200 ]]; then
    SELECTED_URL="$FALLBACK_URL"; ok "Found distribution on Maven Central";
  else
    err "Distribution not found (CDN=$HTTP_PRIMARY central=$HTTP_FALLBACK)"; exit 2;
  fi
fi

WORKDIR="maven-dist-test"
mkdir -p "$WORKDIR"
TARBALL="$WORKDIR/${DIST_TGZ}"

if [[ ! -f "$TARBALL" ]]; then
  log "Downloading $SELECTED_URL"
  if ! download "$SELECTED_URL" "$TARBALL"; then err "Download failed"; exit 2; fi
else
  log "Reusing existing tarball $TARBALL"
fi

if [[ ! -s "$TARBALL" ]]; then err "Tarball is empty"; exit 3; fi
if ! head -c 2 "$TARBALL" | grep -q "\x1f\x8b"; then warn "Tarball missing gzip magic (continuing)"; fi

EXTRACT_DIR="$WORKDIR/extracted"
mkdir -p "$EXTRACT_DIR"
rm -rf "$EXTRACT_DIR"/apache-maven-${REQ_VERSION} 2>/dev/null || true
tar -xzf "$TARBALL" -C "$EXTRACT_DIR"

if [[ ! -x "$EXTRACT_DIR/apache-maven-${REQ_VERSION}/bin/mvn" ]]; then
  err "mvn executable not found after extraction"; exit 3;
fi
ok "Extracted Maven distribution"

MVN_TEST="$EXTRACT_DIR/apache-maven-${REQ_VERSION}/bin/mvn"
log "Running: $MVN_TEST -version"
"$MVN_TEST" -version || { err "mvn -version failed"; exit 3; }

if [[ $JSON_MODE -eq 1 ]]; then
  cat <<JSON
{
  "version": "${REQ_VERSION}",
  "distributionUrl": "${SELECTED_URL}",
  "installedPath": "${EXTRACT_DIR}/apache-maven-${REQ_VERSION}",
  "status": "ok"
}
JSON
fi

# Cleanup unless --keep flag was specified
if [[ $KEEP_FILES -eq 0 ]]; then
  log "Cleaning up test directory: $WORKDIR"
  rm -rf "$WORKDIR" 2>/dev/null || true
else
  log "Keeping test files in: $WORKDIR (--keep specified)"
fi

exit 0
