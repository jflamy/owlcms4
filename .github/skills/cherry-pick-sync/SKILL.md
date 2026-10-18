---
name: cherry-pick-sync
description: "Use when: synchronizing an OWLCMS release branch/worktree (dev66, dev67, dev68, dev69) by cherry-picking changes from another one, and creating the paired synchronization tags. Keywords: cherry-pick, cherrypick, sync, synchronization tag, devNN-sync, dev68 to dev69, pull changes from another branch."
---

# Cherry-Pick Synchronization Between Release Branches

Each release branch lives in its own linked worktree (`owlcms_66` = dev66, `owlcms_67` = dev67, `owlcms_68` = dev68, `owlcms_69` = dev69). Changes flow from an older branch to a newer one by selective cherry-picking, and each sync is recorded with a pair of annotated tags. Follow the `git-linked-worktree` skill for Git execution (unsandboxed, direct CLI).

## Tag Convention

For a sync from SOURCE (e.g. dev68) into DESTINATION (e.g. dev69) on date `YYYY-MM-DD`:

| Tag | Points at | Message |
| --- | --- | --- |
| `<DEST>-sync-YYYY-MM-DD` (e.g. `dev69-sync-2026-10-14`), on the source repo | source branch tip at sync time | `<SOURCE> synchronization point for <DEST> through <date>`; body lists applied, already-present, and skipped commits (with short hashes and reasons) |
| `<SOURCE>-sync-YYYY-MM-DD` (e.g. `dev68-sync-2026-10-14`), on the destination | destination tip after the cherry-picks | `<DEST> synced with selected <SOURCE> changes through <date>`; body summarizes what was applied and exclusions, and cross-references the source tag and its hash |

- The tag name carries the branch that the tag is a sync point *for* on the source side, and the branch it was synced *from* on the destination side. Check existing tags before choosing names.
- Several syncs on one day take a letter suffix: `-2026-10-12`, `-2026-10-12a`, `-2026-10-12b`.
- Tags are annotated (`git tag -a`).

## Workflow

1. Find the previous sync: `git tag --list '<SOURCE>-sync-*' --sort=creatordate | tail -3` (run in the destination worktree), and check which source commit it recorded (`git tag -n9 <tag>`).
2. List candidate commits on the source since that point: `git log --oneline --reverse <previous-source-sync-tag>..<SOURCE-branch>` run from the destination worktree (worktrees share the repo, so branches are visible).
3. Classify every candidate:
   - **Apply**: functional fixes and features wanted in the destination.
   - **Already present**: equivalent change exists in the destination (compare with `git cherry` or by inspecting diffs); do not apply again.
   - **Skip**: managed translation commits (`translation4.csv` is maintained externally) and branch-specific release metadata (version bumps, release commits such as `Release 68.0.0`).
4. Present the classification to the user and wait for confirmation if anything is ambiguous.
5. Cherry-pick the "apply" commits in chronological order in the destination worktree. Resolve conflicts, then `git cherry-pick --continue`. Never stage anything outside the cherry-pick itself.
6. Validate in proportion to the change (Java diagnostics, shell syntax, focused tests with Maven consent per `run-maven-test`).
7. Create the two annotated tags with detailed messages (see convention). Use a `-F` file or multiple `-m` options to keep the message readable.
8. Confirm both worktrees are clean (`git status --short`).
9. Push only when the user asks, and push only the two tags: `git push origin <tag1> <tag2>`. Do not push branches. Verify with `git ls-remote --tags origin '<tag>*'`.

## Tag Creation Template

```bash
cd /Users/jflamy/git/owlcms_68 && git tag -a dev69-sync-2026-10-14 <source-tip> -m "dev68 synchronization point for dev69 through 2026-10-14" -m "Selected changes through dev68 <hash> (<subject>). Applied <hashes>. Already present: <hashes>. Skipped managed translation commits <hashes>; skipped branch-specific release commits <hashes>."
cd /Users/jflamy/git/owlcms_69 && git tag -a dev68-sync-2026-10-14 HEAD -m "dev69 synced with selected dev68 changes through 2026-10-14" -m "Applied <summary>. Source: dev69-sync-2026-10-14 at <hash>. Managed translations and branch-specific release metadata intentionally excluded."
```

Because tags are shared repository refs, they can be created from either worktree; the commit argument decides what they point at.

## Rules

- Never `git add`/`git rm` outside what cherry-pick does; never commit or push without the user's explicit consent (tags included).
- Do not use `--no-verify`, force pushes, or reset to work around conflicts.
- Do not modify `translation4.csv` as part of a sync.
