#!/bin/bash
# Test script for resolve_hotswap_url function - extract function only

# Extract and define just the function
resolve_hotswap_url() {
  local version="$1"
  local DEFAULT_VERSION="2.0.1"
  local url=""

  if [ "$version" = "latest" ]; then
    if command -v curl >/dev/null 2>&1; then
      echo "Querying GitHub Releases API for latest Hotswap Agent..." >&2
      local api_json candidates chosen ver extracted_min
      api_json=$(curl -s -H "Accept: application/vnd.github.v3+json" https://api.github.com/repos/HotswapProjects/HotswapAgent/releases/latest 2>/dev/null || true)
      # Validate we got JSON response
      if ! echo "$api_json" | grep -q '"browser_download_url"'; then
        echo "Invalid GitHub API response; falling back to pinned ${DEFAULT_VERSION}" >&2
        version="$DEFAULT_VERSION"
      elif echo "$api_json" | grep -qi "API rate limit exceeded"; then
        echo "GitHub API rate limit exceeded; falling back to pinned ${DEFAULT_VERSION}" >&2
        version="$DEFAULT_VERSION"
      else
        candidates=$(echo "$api_json" | grep -E '"browser_download_url"' | grep 'hotswap-agent-' | grep '.jar"' | grep -v -E '(sources|javadoc)' | cut -d '"' -f 4)
        # Pick the first candidate whose version is >= DEFAULT_VERSION (simple numeric compare stripping non-digits/dots)
        for c in $candidates; do
          ver=$(echo "$c" | sed -E 's#.*/hotswap-agent-([^/]+)\.jar#\1#')
          # Normalize version (strip leading 'RELEASE-' or 'v')
          ver=$(echo "$ver" | sed -E 's/^(RELEASE-|v)//')
          # Compare by removing dots (fallback heuristic)
          if [ -z "$chosen" ]; then
            chosen="$c"; extracted_min="$ver"
          fi
          if [ "$(echo "$ver" | tr -d '.')" -ge "$(echo "$DEFAULT_VERSION" | tr -d '.')" ]; then
            chosen="$c"; extracted_min="$ver"; break
          fi
        done
        if [ -n "$chosen" ]; then
          url="$chosen"
          echo "Selected Hotswap Agent asset: $url (version $extracted_min)" >&2
          # Enforce minimum version
          if [ "$(echo "$extracted_min" | tr -d '.')" -lt "$(echo "$DEFAULT_VERSION" | tr -d '.')" ]; then
            echo "Chosen version $extracted_min is below minimum $DEFAULT_VERSION; using pinned." >&2
            url=""
            version="$DEFAULT_VERSION"
          fi
        else
          echo "No suitable binary asset found; falling back to pinned ${DEFAULT_VERSION}" >&2
          version="$DEFAULT_VERSION"
        fi
      fi
    else
      echo "curl not available; using pinned ${DEFAULT_VERSION}" >&2
      version="$DEFAULT_VERSION"
    fi
  fi

  if [ -z "$url" ]; then
    # Either a specific version was requested or latest fallback selected.
    if [ "$version" = "latest" ]; then
      version="$DEFAULT_VERSION"
    fi
    url="https://github.com/HotswapProjects/HotswapAgent/releases/download/${version}/hotswap-agent-${version}.jar"
  fi

  # Emit only the URL on stdout
  echo "$url"
}

# Test function directly
echo "Testing resolve_hotswap_url function..."
echo "=========================="

echo "Test 1: latest version"
url=$(resolve_hotswap_url "latest")
echo "Result: '$url'"
echo

echo "Test 2: specific version"  
url=$(resolve_hotswap_url "2.0.1")
echo "Result: '$url'"
echo

echo "Test 3: Check no debug output in result"
result=$(resolve_hotswap_url "latest")
if echo "$result" | grep -q "Querying GitHub"; then
    echo "ERROR: Debug output leaked into result!"
    echo "Result was: '$result'"
else
    echo "SUCCESS: Clean URL result"
    echo "Clean result: '$result'"
fi
