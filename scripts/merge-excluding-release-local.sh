#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: scripts/merge-excluding-release-local.sh <source-branch>

Merges <source-branch> into the current branch with --no-commit, then restores
these local/current-branch files so they are excluded from the merge result:
  - ReleaseNotes.md
  - .vscode/launch.json

The script does not commit. Review the result, resolve any remaining conflicts,
then commit manually.
USAGE
}

if [[ $# -ne 1 || "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 1
fi

SOURCE_BRANCH="$1"
EXCLUDED_PATHS=(
  "ReleaseNotes.md"
  ".vscode/launch.json"
)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "${SCRIPT_DIR}/.." rev-parse --show-toplevel)"

cd "${REPO_ROOT}"

if [[ -f "$(git rev-parse --git-path MERGE_HEAD)" ]]; then
  echo "ERROR: a merge is already in progress. Resolve or abort it before running this script." >&2
  exit 1
fi

if ! git rev-parse --verify --quiet "${SOURCE_BRANCH}^{commit}" >/dev/null; then
  echo "ERROR: source branch or commit not found: ${SOURCE_BRANCH}" >&2
  exit 1
fi

CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"

echo "Repository:    ${REPO_ROOT}"
echo "Current:       ${CURRENT_BRANCH}"
echo "Merge source:  ${SOURCE_BRANCH}"
echo "Excluded:      ${EXCLUDED_PATHS[*]}"
echo ""

set +e
git merge --no-commit --no-ff "${SOURCE_BRANCH}"
MERGE_STATUS=$?
set -e

echo ""
echo "Restoring excluded paths from ${CURRENT_BRANCH}..."
for excluded_path in "${EXCLUDED_PATHS[@]}"; do
  if git cat-file -e "HEAD:${excluded_path}" 2>/dev/null; then
    git restore --source=HEAD --staged --worktree -- "${excluded_path}"
    echo "Restored: ${excluded_path}"
  else
    git rm -f --ignore-unmatch -- "${excluded_path}" >/dev/null
    echo "Removed merge-added path absent from HEAD: ${excluded_path}"
  fi
done

echo ""
if [[ ${MERGE_STATUS} -eq 0 ]]; then
  echo "Merge applied without conflicts. Excluded paths were restored from ${CURRENT_BRANCH}."
  echo "Review with: git status && git diff --cached"
  echo "Commit when ready."
else
  echo "Merge reported conflicts. Excluded paths were restored from ${CURRENT_BRANCH}." >&2
  echo "Resolve remaining conflicts, then commit when ready." >&2
  exit "${MERGE_STATUS}"
fi
