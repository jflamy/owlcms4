#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: scripts/create-worktree-branch.sh <branch> [worktree-path] [start-point]

Creates a git worktree for <branch>, then copies local uncommitted workspace files
that are normally ignored in this repository:
  - files under .vscode/ that are missing in the new worktree, such as .env.mac
  - jfl.code-workspace, when present

If <branch> exists locally or as origin/<branch>, the worktree uses that branch.
If it does not exist, a new branch is created from [start-point] or HEAD.

Examples:
  scripts/create-worktree-branch.sh dev68
  scripts/create-worktree-branch.sh test-fix ../owlcms-test-fix dev68
USAGE
}

if [[ $# -lt 1 || $# -gt 3 || "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 1
fi

BRANCH="$1"
WORKTREE_PATH="${2:-../${BRANCH}}"
START_POINT="${3:-HEAD}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "${SCRIPT_DIR}/.." rev-parse --show-toplevel)"
WORKTREE_PATH_ABS="$(cd "${REPO_ROOT}" && mkdir -p "$(dirname "${WORKTREE_PATH}")" && cd "$(dirname "${WORKTREE_PATH}")" && pwd)/$(basename "${WORKTREE_PATH}")"

if [[ -e "${WORKTREE_PATH_ABS}" ]]; then
  echo "ERROR: worktree path already exists: ${WORKTREE_PATH_ABS}" >&2
  exit 1
fi

echo "Repository: ${REPO_ROOT}"
echo "Branch:     ${BRANCH}"
echo "Worktree:   ${WORKTREE_PATH_ABS}"
echo ""

if git -C "${REPO_ROOT}" show-ref --verify --quiet "refs/heads/${BRANCH}"; then
  echo "Creating worktree from local branch ${BRANCH}..."
  git -C "${REPO_ROOT}" worktree add "${WORKTREE_PATH_ABS}" "${BRANCH}"
elif git -C "${REPO_ROOT}" show-ref --verify --quiet "refs/remotes/origin/${BRANCH}"; then
  echo "Creating worktree from origin/${BRANCH}..."
  git -C "${REPO_ROOT}" worktree add --track -b "${BRANCH}" "${WORKTREE_PATH_ABS}" "origin/${BRANCH}"
else
  echo "Creating new branch ${BRANCH} from ${START_POINT}..."
  git -C "${REPO_ROOT}" worktree add -b "${BRANCH}" "${WORKTREE_PATH_ABS}" "${START_POINT}"
fi

copy_if_missing() {
  local source_path="$1"
  local relative_path="${source_path#"${REPO_ROOT}/"}"
  local target_path="${WORKTREE_PATH_ABS}/${relative_path}"

  if [[ ! -e "${source_path}" ]]; then
    return
  fi
  if [[ -e "${target_path}" ]]; then
    return
  fi

  mkdir -p "$(dirname "${target_path}")"
  cp -p "${source_path}" "${target_path}"
  echo "Copied local file: ${relative_path}"
}

echo ""
echo "Copying missing local workspace files..."

if [[ -d "${REPO_ROOT}/.vscode" ]]; then
  while IFS= read -r -d '' vscode_file; do
    copy_if_missing "${vscode_file}"
  done < <(find "${REPO_ROOT}/.vscode" -type f -print0)
fi

copy_if_missing "${REPO_ROOT}/jfl.code-workspace"

echo ""
echo "Done. New worktree: ${WORKTREE_PATH_ABS}"
