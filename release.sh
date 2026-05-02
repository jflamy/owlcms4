#!/usr/bin/env bash
REVISION="${1:-66.1.0-rc06}"
set -euo pipefail

# Triggers the GitHub Actions workflow `.github/workflows/release.yaml`
# and watches the run until completion.
#
# Usage:
#   ./release.sh 65.0.0-beta01
#   BUILD_IMAGES=false ./release.sh 65.0.0-beta01
#
# Defaults:
#   - Commits + pushes release note sources (src/main/markdown/*) and release.sh before triggering
#     (so CI builds what you see)
#   - Runs the workflow on the current git branch
#   - After a successful run, does a safe git pull --ff-only

if [[ -z "${REVISION}" ]]; then
  echo "ERROR: REVISION must be defined or passed as the first argument: ./release.sh 65.0.0-beta01" >&2
  exit 1
fi

BUILD_IMAGES="${BUILD_IMAGES:-true}"
DO_COMMIT="${DO_COMMIT:-true}"
DO_PUSH="${DO_PUSH:-true}"
DO_GIT_PULL="${DO_GIT_PULL:-true}"
WORKFLOW_FILE="release.yaml"

if ! command -v gh >/dev/null 2>&1; then
  echo "ERROR: 'gh' (GitHub CLI) not found on PATH." >&2
  exit 1
fi

if ! command -v git >/dev/null 2>&1; then
  echo "ERROR: 'git' not found on PATH." >&2
  exit 1
fi

# Fail fast if not authenticated.
if ! gh auth status >/dev/null 2>&1; then
  echo "ERROR: gh is not authenticated. Run: gh auth login" >&2
  exit 1
fi

REPO="$(gh repo view --json nameWithOwner -q .nameWithOwner)"

CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "${CURRENT_BRANCH}" == "HEAD" ]]; then
  echo "ERROR: Detected detached HEAD; checkout a branch before running release.sh." >&2
  exit 1
fi
GIT_REF="${CURRENT_BRANCH}"

echo "Repo:      ${REPO}"
echo "Workflow:  ${WORKFLOW_FILE}"
echo "Revision:  ${REVISION}"
echo "Images:    ${BUILD_IMAGES}"
echo "Branch:    ${GIT_REF}"
echo "Commit:    ${DO_COMMIT}"
echo "Push:      ${DO_PUSH}"
echo "Git pull:  ${DO_GIT_PULL}"

# Check if the tag already exists locally or remotely.
if git rev-parse "${REVISION}" >/dev/null 2>&1; then
  echo "ERROR: Tag '${REVISION}' already exists in local repository." >&2
  echo "       Use a new version number." >&2
  exit 3
fi

if git ls-remote --tags origin | grep -q "refs/tags/${REVISION}$"; then
  echo "ERROR: Tag '${REVISION}' already exists in remote repository." >&2
  echo "       Use a new version number." >&2
  exit 3
fi

# Check that translation4.csv, refreshed from HEAD, matches the Google Sheets source.
# Local edits to translation4.csv are intentionally discarded for this check.
TRANSLATION_CSV="shared/src/main/resources/i18n/translation4.csv"
GOOGLE_SHEET_URL="https://docs.google.com/spreadsheets/d/1ZRfYHCARnPCnUEVZYo3Y_7qJGS9z7NRVg-Se7z3lHtE/export?format=csv"

echo "Checking translation4.csv against Google Sheets source..."
REMOTE_TMP=$(mktemp)
trap "rm -f ${REMOTE_TMP}" EXIT
curl -sL "${GOOGLE_SHEET_URL}" > "${REMOTE_TMP}"

if ! git restore --source=HEAD --worktree -- "${TRANSLATION_CSV}"; then
  echo "ERROR: Could not refresh ${TRANSLATION_CSV} from HEAD." >&2
  exit 4
fi

if ! diff --strip-trailing-cr -q "${TRANSLATION_CSV}" "${REMOTE_TMP}" >/dev/null 2>&1; then
  echo "WARNING: translation4.csv does not match the Google Sheets source!" >&2
  echo "         The HEAD version on this branch is out of date." >&2
  echo ""
  echo "Running translation consistency check..."
  PROMPT_DOWNLOAD=false ./scripts/check-translation-diff.sh || true
  echo ""
  read -p "Refresh translation4.csv from Google Sheets now and exit? (y/N): " -r REFRESH
  if [[ "${REFRESH}" =~ ^[Yy]$ ]]; then
    echo "Updating ${TRANSLATION_CSV} from Google Sheets..."
    if [[ -f "${TRANSLATION_CSV}" ]]; then
		BACKUP_FILE="${TRANSLATION_CSV}.bak"
      echo "Moving existing file aside: ${BACKUP_FILE}"
      mv "${TRANSLATION_CSV}" "${BACKUP_FILE}"
    fi
    if ! cp "${REMOTE_TMP}" "${TRANSLATION_CSV}"; then
      if [[ -n "${BACKUP_FILE:-}" && -f "${BACKUP_FILE}" ]]; then
        echo "Restoring backup..."
        mv "${BACKUP_FILE}" "${TRANSLATION_CSV}"
      fi
      echo "ERROR: Failed to refresh ${TRANSLATION_CSV}." >&2
      exit 4
    fi
    echo "translation4.csv was refreshed."
    echo "Aborting release after refreshing translation4.csv. Please review/commit the updated file, then rerun release.sh." >&2
    exit 4
  fi

  read -p "Do you want to proceed anyway? (y/N): " -r PROCEED
  if [[ ! "${PROCEED}" =~ ^[Yy]$ ]]; then
    echo "Aborting. Update translation4.csv from Google Sheets before releasing." >&2
    exit 4
  fi
  echo "Proceeding despite translation mismatch..."
else
  echo "translation4.csv matches Google Sheets source."
fi

if [[ "${DO_COMMIT}" == "true" ]]; then
  # Only allow committing the files that must match the build.
  # Notes live in src/main/markdown and are assembled by the workflow.
  # src/main/markdown/ReleaseNotes.md is OK to commit.
  # Root-level ReleaseNotes.md (if any) should NOT be committed.
  ALLOWED_FILES=(
    "release.sh"
    "src/main/markdown/ReleaseNotes.md"
    "src/main/markdown/release.md"
    "src/main/markdown/rc.md"
    "src/main/markdown/beta.md"
    "src/main/markdown/alpha.md"
  )

  DIRTY_FILES=()
  while IFS= read -r line; do
    [[ -z "${line}" ]] && continue
    # Porcelain format: XY <path> (we only care about the path)
    path="${line:3}"
    DIRTY_FILES+=("${path}")
  done < <(git status --porcelain)

  if ((${#DIRTY_FILES[@]} > 0)); then
    for f in "${DIRTY_FILES[@]}"; do
      allowed=false
      for a in "${ALLOWED_FILES[@]}"; do
        if [[ "${f}" == "${a}" ]]; then
          allowed=true
          break
        fi
      done
      if [[ "${allowed}" == "false" ]]; then
        echo "ERROR: Working tree has changes outside allowed files:" >&2
        echo "  Allowed: ${ALLOWED_FILES[*]}" >&2
        echo "  Found:   ${DIRTY_FILES[*]}" >&2
        echo "Commit/stash other changes, or set DO_COMMIT=false." >&2
        exit 2
      fi
    done
  fi

  # Stage + commit if there are changes.
  git add -- "${ALLOWED_FILES[@]}" || true
  if git diff --cached --quiet; then
    echo "No changes to commit in ${ALLOWED_FILES[*]}"
  else
    git commit -m "Release ${REVISION}"
  fi

  if [[ "${DO_PUSH}" == "true" ]]; then
    # Ensure the remote has the commit before triggering workflow_dispatch.
    # Push only the current branch by name (not all matching branches).
    git push origin "${CURRENT_BRANCH}"
  fi
fi

# Capture the most recent run before triggering so we can detect the new run.
PREV_RUN_ID="$(gh run list --repo "${REPO}" --workflow "${WORKFLOW_FILE}" --limit 1 --json databaseId -q '.[0].databaseId' 2>/dev/null || true)"

ARGS=(--repo "${REPO}" -f "revision=${REVISION}" -f "buildImages=${BUILD_IMAGES}")
ARGS+=( --ref "${GIT_REF}" )

echo "Triggering workflow_dispatch…"
gh workflow run "${WORKFLOW_FILE}" "${ARGS[@]}"

echo "Waiting for the run to appear…"
RUN_ID=""
for _ in {1..60}; do
  # Get the most recent run that is queued or in_progress
  RUN_ID="$(gh run list \
    --repo "${REPO}" \
    --workflow "${WORKFLOW_FILE}" \
    --limit 1 \
    --json databaseId,status \
    -q '.[] | select(.status == "queued" or .status == "in_progress") | .databaseId' \
    || true)"

  if [[ -n "${RUN_ID}" && "${RUN_ID}" != "${PREV_RUN_ID}" ]]; then
    break
  fi

  sleep 3
done

if [[ -z "${RUN_ID}" || "${RUN_ID}" == "${PREV_RUN_ID}" ]]; then
  echo "ERROR: Could not find the newly triggered run for ${WORKFLOW_FILE}." >&2
  echo "Tip: Check runs manually: gh run list --repo \"${REPO}\" --workflow \"${WORKFLOW_FILE}\"" >&2
  exit 1
fi

echo "Run ID: ${RUN_ID}"
echo "Run URL: https://github.com/${REPO}/actions/runs/${RUN_ID}"
echo "Watching run (Ctrl+C to detach)…"

RUN_FAILED=false
if ! gh run watch --repo "${REPO}" "${RUN_ID}" --exit-status; then
  RUN_FAILED=true
  echo ""
  echo "╔════════════════════════════════════════════════════════════════════════════╗"
  echo "║ Run FAILED                                                                 ║"
  echo "╚════════════════════════════════════════════════════════════════════════════╝"
  echo "View this run on GitHub: https://github.com/${REPO}/actions/runs/${RUN_ID}"
  echo ""
  echo "Showing failed logs…"
  gh run view --repo "${REPO}" "${RUN_ID}" --log-failed || true
else
  echo "Run finished. Showing summary…"
  gh run view --repo "${REPO}" "${RUN_ID}"
fi

if [[ "${DO_GIT_PULL}" == "true" ]]; then
  echo "Updating local repo via git pull (--ff-only)…"

  if [[ -n "$(git status --porcelain)" ]]; then
    echo "ERROR: Working tree is not clean; refusing to run git pull." >&2
    echo "Commit/stash changes and re-run with DO_GIT_PULL=true." >&2
    exit 2
  fi

  git pull --ff-only
fi

if [[ "${RUN_FAILED}" == "true" ]]; then
  exit 1
fi
