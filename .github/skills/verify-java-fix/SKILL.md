---
name: verify-java-fix
description: "Use when: editing Java files, fixing Java compile errors, fixing Java syntax errors, reporting a Java fix as complete, or validating that a Java change actually compiles in the workspace. Keywords: Java fix, compile error, syntax error, Problems panel, get_errors, build clean, Java validation, done fixing Java."
---

# Verify Java Fix

This skill prevents false completion claims for Java work in `owlcms4`.

## Scope

Use this skill whenever Java source files were edited, Java compile or syntax errors were investigated, or a response would otherwise say that a Java fix is done.

## Goal

Do not claim success on a Java fix unless the relevant Java files are free of workspace-reported syntax and compile errors.

## Core Rule

If Java was changed, check the workspace errors before reporting the fix as complete.

A Java fix is not complete when:

- the edited Java file still has syntax errors
- the edited Java file still has compile errors
- the workspace still shows cascading parser errors caused by the recent edit
- validation was not run and there is no explicit statement that verification is still pending

## Required Workflow

1. Identify every Java file you edited for the task.
2. Run `get_errors` on those files after the edit.
3. If errors remain, treat the task as still in progress and fix the code or clearly report the blocker.
4. Only after `get_errors` is clean should you describe the Java fix as completed.
5. If builds or tests are restricted by repository policy, say that explicitly, but do not confuse that with syntax validation. `get_errors` is still required.

## Reporting Rules

- Do not say "fixed" or "done" for Java changes if `get_errors` still reports syntax or compile problems.
- If the code is partially repaired but not clean, say exactly that.
- Distinguish between:
  - workspace syntax or compile validation
  - unit or integration test execution
  - full Maven or project builds that require human approval

## Repository-Specific Notes

- In `owlcms4`, local Maven and full builds require human consent.
- That restriction does not remove the obligation to check workspace Java errors after edits.
- Prefer `get_errors` for immediate validation of edited Java files.

## Expected Output

When using this skill:

- validate the edited Java files with `get_errors`
- fix remaining syntax issues before closing the task when feasible
- report clearly whether Java validation is clean
- separately mention when Maven or tests were not run

## Do Not

- Do not claim a Java fix is complete while the edited file still has Problems panel errors.
- Do not hide behind "I did not run Maven" when the file still has direct syntax errors.
- Do not assume a small patch is safe without checking the workspace error state.
- Do not stage, commit, or push unless explicitly requested.