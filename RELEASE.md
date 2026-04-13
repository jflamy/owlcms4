# Release Process (owlcms4)

This repo uses a GitHub Actions workflow ([.github/workflows/release.yaml](.github/workflows/release.yaml)) to build OWLCMS and create a GitHub Release.

The workflow also **pushes back** a few generated/“assembled” artifacts to the source branch -- the `release.sh` script does a `git pull` to retrieve them:

- `version.txt` updated to the exact semver tag that was built
- `ReleaseNotes.md` assembled from `src/main/markdown/*`
- an updated default `REVISION` in `release.sh` (so you can see what was last built)
- a git tag matching the release semver

## Prerelease builds (alpha/beta/rc)

Prerelease builds are the usual flow.

### 1) Start from a clean branch

- Checkout your prerelease branch.
- Ensure your working tree is clean.
- Pull the latest changes (including any “assembled” artifacts from the previous run).

### 2) Set the new revision

Edit `release.sh` and update the default revision on line 2 to the new semver prerelease, e.g.:

- `64.0.0-rc08`

### 3) Update release notes sources (if needed)

Edit the maintained notes in:

- `src/main/markdown/ReleaseNotes.md`

Optionally update the prefix pages (used for alpha/beta/rc/final headers):

- `src/main/markdown/alpha.md`
- `src/main/markdown/beta.md`
- `src/main/markdown/rc.md`
- `src/main/markdown/release.md`

Do **not** manually edit the root `ReleaseNotes.md` for a release. That file is assembled by the workflow and pushed back.

### 4) Commit your changes

Commit your edits on the prerelease branch.

### 5) Run the release script

Run from the repo root:

```bash
./release.sh
```

The script will:

- (by default) commit + push the release-note source files and `release.sh` (so the workflow builds exactly what you have)
- trigger the GitHub Actions workflow on the **current branch**
- wait for completion
- on success, `git pull --ff-only` to retrieve the assembled artifacts and the tag

## Main release (stable)

A stable release should be cut from the main/stable branch IMMEDIATELY AFTER doing a last prerelease branch

Suggested flow:

1. Update the release notes - clean up -rcXX from the version numbers, put the routine fixes at the bottom of the list
2. Run the following script that will switch to main (creating it if needed), push everything, and come back to dev.

```bash
./scripts/mainRelease.sh
```


## Requirements

- GitHub CLI (`gh`) installed and authenticated.
- Permission to trigger workflows and push commits/tags to the repo.
