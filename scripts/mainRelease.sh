#!/usr/bin/env bash
set -euo pipefail

# This script assumes you're on a devXX branch where XX is a number.
# It:
# 1. Pulls the current dev branch
# 2. Switches to mainXX
# 3. Fast-forward merges from devXX
# 4. Launches release.sh with any arguments passed
# 5. When release.sh finishes, switches back to devXX
# 6. Fast-forward merges from mainXX

CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"

# Extract the number from devXX
if [[ ! "${CURRENT_BRANCH}" =~ ^dev([0-9]+)$ ]]; then
  echo "ERROR: Current branch must be in format 'devXX' where XX is a number" >&2
  echo "       Current branch: ${CURRENT_BRANCH}" >&2
  exit 1
fi

VERSION_NUM="${BASH_REMATCH[1]}"
MAIN_BRANCH="main${VERSION_NUM}"
DEV_BRANCH="dev${VERSION_NUM}"
ORIGINAL_BRANCH="${CURRENT_BRANCH}"
COMPLETED="false"

restore_original_branch_on_exit() {
  local exit_code=$?

  if [[ "${COMPLETED}" == "true" ]]; then
    return "${exit_code}"
  fi

  local active_branch
  active_branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || true)"

  if [[ -n "${active_branch}" && "${active_branch}" != "${ORIGINAL_BRANCH}" ]]; then
    echo ""
    echo "mainRelease.sh did not complete successfully." >&2
    echo "Restoring original branch ${ORIGINAL_BRANCH} from ${active_branch}..." >&2
    if ! git checkout "${ORIGINAL_BRANCH}"; then
      echo "WARNING: Failed to restore branch ${ORIGINAL_BRANCH}." >&2
      echo "         Please switch back manually after reviewing the working tree state." >&2
    fi
  fi

  return "${exit_code}"
}

trap restore_original_branch_on_exit EXIT

echo "Dev branch:  ${DEV_BRANCH}"
echo "Main branch: ${MAIN_BRANCH}"
echo ""

# Step 1: Pull current dev branch
echo "Pulling ${DEV_BRANCH}..."
git pull --ff-only origin "${DEV_BRANCH}"

# Step 2: Switch to mainXX (create from devXX if it doesn't exist)
if git show-ref --verify --quiet "refs/heads/${MAIN_BRANCH}"; then
  echo "Switching to existing ${MAIN_BRANCH}..."
  git checkout "${MAIN_BRANCH}"
  # Step 3: Fast-forward merge from devXX
  echo "Merging ${DEV_BRANCH} into ${MAIN_BRANCH} (fast-forward only)..."
  git merge --ff-only "${DEV_BRANCH}"
elif git show-ref --verify --quiet "refs/remotes/origin/${MAIN_BRANCH}"; then
  echo "Switching to existing origin/${MAIN_BRANCH}..."
  git checkout --track "origin/${MAIN_BRANCH}"
  # Step 3: Fast-forward merge from devXX
  echo "Merging ${DEV_BRANCH} into ${MAIN_BRANCH} (fast-forward only)..."
  git merge --ff-only "${DEV_BRANCH}"
else
  echo "Creating ${MAIN_BRANCH} from ${DEV_BRANCH}..."
  git checkout -b "${MAIN_BRANCH}" "${DEV_BRANCH}"
fi

# Step 4: Push the merged mainXX branch
echo "Pushing ${MAIN_BRANCH}..."
git push origin "${MAIN_BRANCH}"

# Step 5: Extract default REVISION from release.sh and remove suffix
# release.sh uses format: REVISION="${1:-64.0.4-rc02}"
# We need to extract the default value (after :-)
REVISION_LINE=$(grep '^REVISION=' ./release.sh | head -n 1)
DEFAULT_REVISION=$(echo "${REVISION_LINE}" | sed 's/.*:-\([^}]*\)}.*/\1/')
BASE_REVISION=$(echo "${DEFAULT_REVISION}" | sed 's/-.*$//')

echo "Default revision from release.sh: ${DEFAULT_REVISION}"
echo "Base revision (suffix removed): ${BASE_REVISION}"

# Step 6: Launch release.sh with base revision (or use passed arguments if provided)
if [[ $# -eq 0 ]]; then
  echo "Launching release.sh ${BASE_REVISION}..."
  DO_GIT_PULL=false ./release.sh "${BASE_REVISION}"
else
  echo "Launching release.sh $@..."
  DO_GIT_PULL=false ./release.sh "$@"
fi

# Step 7: Refresh local mainXX from origin after release.sh.
# release.sh triggers CI-side release commits and may or may not leave the local
# branch advanced to the remote tip, depending on local branch tracking/state.
# Pull explicitly here so the merge-back to devXX always uses the released main tip.
echo "Refreshing ${MAIN_BRANCH} from origin..."
git pull --ff-only origin "${MAIN_BRANCH}"

# Step 8: Switch back to devXX
echo "Switching back to ${DEV_BRANCH}..."
git checkout "${DEV_BRANCH}"

# Step 9: Fast-forward merge from mainXX
echo "Merging ${MAIN_BRANCH} into ${DEV_BRANCH} (fast-forward only)..."
git merge --ff-only "${MAIN_BRANCH}"

# Step 10: Push the merged devXX branch
echo "Pushing ${DEV_BRANCH}..."
git push origin "${DEV_BRANCH}"

COMPLETED="true"

echo ""
echo "Done! Both ${DEV_BRANCH} and ${MAIN_BRANCH} are now in sync and pushed."
