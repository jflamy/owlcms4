---
name: git-linked-worktree
description: "Use when: running Git commands, checking status or diffs, staging, committing, or diagnosing 'not a git repository' in an OWLCMS linked worktree. Keywords: git, worktree, linked worktree, gitdir, sandbox, status, diff, add, commit, not a git repository."
---

# Git in Linked Worktrees

## Purpose

Avoid repeated Git failures when the workspace checkout is a linked worktree whose metadata is stored outside the workspace sandbox.

## Required Workflow

1. Before the first Git command in a repository, read the repository root `.git` path.
2. If `.git` is a file containing a `gitdir:` path outside the workspace, treat the checkout as a linked worktree.
3. Run Git commands with `run_in_terminal` and request unsandboxed execution immediately. Do not first attempt the same command in the sandbox.
4. Explain that unsandboxed execution is needed only because Git must read and update the external worktree metadata.
5. Use direct command-line Git. Do not use GitLens, GitKraken, or related integrations.
6. Batch related read-only checks, such as `git status --short`, `git diff --check`, and a path-scoped `git diff`, into one unsandboxed command.
7. For a requested commit, stage only the verified task files and commit them in one unsandboxed command.
8. After committing, verify the commit and remaining worktree state with an unsandboxed `git status --short` and `git log -1 --oneline`.

## Execution Template

For inspection:

```bash
cd /path/to/worktree && git status --short && git diff --check && git diff -- path/to/file1 path/to/file2
```

For a user-authorized commit:

```bash
cd /path/to/worktree && git add path/to/file1 path/to/file2 && git commit -m "Commit title"
```

For verification:

```bash
cd /path/to/worktree && git status --short && git log -1 --oneline
```

Set `requestUnsandboxedExecution: true` with a reason such as:

```text
This linked worktree stores required Git metadata outside the current workspace sandbox.
```

## Terminal Clarity

- `run_in_terminal` starts an agent-managed terminal execution. Do not claim it is the user's existing visible terminal.
- An existing terminal can be reused only when the tool provides its terminal ID.
- Do not ask the user to paste commands merely because sandboxed Git failed. Retry correctly with unsandboxed execution.

## Failure Interpretation

When Git reports:

```text
fatal: not a git repository: /external/path/.git/worktrees/name
```

and the worktree `.git` file points there, assume sandbox isolation first. Do not conclude that repository metadata is corrupt until the same read-only command fails with unsandboxed execution.

If the unsandboxed command is cancelled, report that no Git operation occurred. Do not silently retry or claim a Git failure.

## Safety

- Never commit, stage, push, reset, checkout, or otherwise modify Git state without explicit user authorization.
- Never include unrelated modified files in a commit.
- Never use destructive commands such as `git reset --hard` or `git checkout --` unless explicitly requested.
- Do not push unless separately authorized.
