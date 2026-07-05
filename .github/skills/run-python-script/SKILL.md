---
name: run-python-script
description: "Use when: running an ad hoc Python script, using python -c for inspection or file transforms, editing ZIP/XLSX/XML files with Python, or choosing between python and python3 on macOS/Linux/Windows. Keywords: python script, python3, python -c, zipfile, one-off Python command, interpreter check."
---

# Run Python Script

This skill standardizes how to run short Python helpers safely in this workspace.

## Scope

Use this skill when the task needs a one-off Python script for inspection, transformation, extraction, archive editing, or verification.

## Goal

Run Python helper logic with the right interpreter, without tripping over shell quoting or assuming `python` exists.

## Interpreter Selection

1. **On this machine, `python` does not exist. Always use `python3` directly — do not probe with `which python` or `command -v python` first.**
2. Do not activate virtual environments for this repo unless the user explicitly asks.
3. Use `python3` consistently for the whole task.

## Default Approach

1. For short read-only inspection, `python3 -c '...'` is acceptable if quoting is simple.
2. For multi-line, regex-heavy, XML/ZIP-heavy, or mutating work, prefer a temporary `.py` script file over a long `python3 -c` command.
3. Keep scripts read-only unless the task explicitly requires writing output.
4. For file transforms, write to a temporary output first and replace the original only after the expected change is verified.
5. Print a concise summary such as affected paths or replacement counts.

## Shell Rules

- Use bash-compatible syntax for commands.
- `python` does not exist here; always use `python3`.
- Avoid inline heredocs for Python snippets in Git Bash or mixed shell environments.
- Avoid long `python3 -c` commands when nested quotes, XML, JSON, or regular expressions make shell parsing fragile.

## File Safety Rules

- For ZIP-based files such as `.xlsx`, preserve archive entries and rewrite only the intended member files.
- Validate expected match counts before replacing content.
- Fail loudly with a clear error if the target pattern is absent or appears more times than expected.
- Do not stage, commit, or push unless explicitly requested.

## Preferred Patterns

Short one-liner:

```bash
python3 -c 'print("ok")'
```

Temporary script:

```bash
python3 path/to/temp_script.py
```

## Expected Output

When using this skill:

- Use `python3` directly; no interpreter probing.
- Keep side effects explicit and narrowly scoped.
- Report what was changed or measured.

## Do Not

- Do not call `python` — it does not exist on this machine; use `python3`.
- Do not probe for the interpreter (`which python`, `command -v python`) before running.
- Do not use Python heredocs for inline scripts in Git Bash or cross-platform sessions.
- Do not overwrite source files before checking expected match counts.
- Do not leave temporary helper scripts behind unless they are intentionally useful.
