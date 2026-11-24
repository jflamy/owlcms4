# Copilot workflow insights

Date: 2026-11-03

## Observed usage patterns

- Specification work can become very long-running. The jury-design session exceeded 14,000
  turns and involved extensive inspection, shell work, edits, and clarification.
- Browser validation is intensive. One recent workflow used 78 Playwright runs, 54 shell calls,
  and 42 edits.
- Branch and worktree decisions recur around dirty files, backporting, and keeping development
  branches separate.
- Translation work involves repeated key searches, edits, problem checks, and later commits.
- The repository has many specialized skills, but no repository-specific custom agents.

## Recommendations

### 1. Add a read-only specification agent

Create a `spec-reviewer.agent.md` for compliance and design work with this contract:

> Review the document only. Do not edit code. For each numbered item, report evidence,
> contradiction, decision, and required document change. Wait for explicit approval before
> applying edits.

This fits long jury-design and compliance-review sessions where requirements are refined over
many short decisions.

### 2. Create a visual-scenario agent

Build a custom agent around the existing scoreboard and browser skills. It should accept a
scenario matrix and return:

- screenshots;
- an event timeline;
- visible controls by state;
- pass/fail differences.

This would reduce repeated individual Playwright reruns.

### 3. Separate read-only and implementation sessions

For compliance, protocol, and cross-repository investigations, use a session with edit, Git,
Maven, and build tools denied. Start a separate implementation session after the decisions are
accepted.

This matches the repository's restrictions on automatic builds and Git operations while
preventing accidental edits during analysis.

### 4. State worktree policy in the first prompt

For branch-heavy tasks, begin with a fixed contract:

> Current branch: `dev69`. Preserve all dirty files. Do not checkout or create a worktree
> without asking. First report the safest branch/worktree plan.

This avoids repeated uncertainty when preparing backports or moving uncommitted files.

## Prompt improvement

Short follow-ups are effective once context is established, but messages such as “item 3” can
be ambiguous. Prefer stating the decision, requested action, next scope, and mutation boundary
in one message:

> Decision: accept item 2 as coarse stale-save protection. Update the document, then explain
> item 3. Do not implement code.
