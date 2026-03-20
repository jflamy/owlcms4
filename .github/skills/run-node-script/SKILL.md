---
name: run-node-script
description: "Use when: running an ad hoc Node.js script, using node -e for inspection or file transforms, creating a temporary .cjs helper, or avoiding Git Bash quoting and history-expansion issues on Windows. Keywords: node script, node -e, temporary script, .cjs, Git Bash, history expansion, JSON transform, one-off Node command."
---

# Run Node Script

This skill standardizes how to run short Node.js scripts safely in this workspace, especially on Windows with Git Bash.

## Scope

Use this skill when the task needs a one-off Node.js script for inspection, transformation, extraction, or verification.

## Goal

Run Node-based helper logic without corrupting files, tripping over Git Bash parsing, or leaving unclear side effects.

## Default Approach

1. Prefer a temporary script file over a long `node -e` command.
2. Use a `.cjs` file for ad hoc scripts unless the repo already clearly expects ESM for the helper.
3. Keep the script read-only unless the task explicitly requires writing output.
4. If writing output, write to a new file first when practical.
5. Print a small, useful summary at the end such as counts, ids, or affected object names.

## Workflow

1. Decide whether the script is read-only or mutating.
2. For trivial read-only inspection, `node -e` is acceptable if the command is short and quoting is simple.
3. For anything multi-line, regex-heavy, JSON-heavy, or containing characters that Bash may interpret, create a temporary `.cjs` file instead.
4. Run the script with `node path/to/script.cjs`.
5. Review the output before making follow-up edits.
6. Remove the temporary helper when it is no longer needed, unless the script has continuing value for the repo.

## Git Bash Rules

- Do not use inline heredocs for Node scripts.
- Be cautious with `!` in `node -e` commands because Git Bash history expansion can break the command.
- If a one-liner must contain `!`, disable history expansion first with `set +H` in that shell session.
- Prefer a temporary script file instead of fighting shell escaping.
- Use bash syntax for commands.

## File Safety Rules

- For transforms, prefer reading one file and writing a separate output file first.
- Do not overwrite a source file until the script result is understood.
- Preserve formatting and field order when modifying structured data unless the task explicitly allows normalization.
- For JSON fixtures, avoid changing unrelated top-level objects unless pruning is part of the task.

## Script Style

- Use `const fs = require('fs');` for ad hoc CommonJS helpers.
- Keep the script small and focused on the current task.
- Validate the expected input shape before traversing nested properties.
- Fail loudly with a clear error message when required data is missing.
- Print deterministic summaries such as `athletes=40` rather than verbose dumps.

## Preferred Patterns

Short inspection one-liner:

```bash
set +H
node -e 'const fs=require("fs"); const data=JSON.parse(fs.readFileSync("path/to/file.json","utf8")); console.log(data.athletes.length);'
```

Temporary script for anything non-trivial:

```bash
node temp-script.cjs
```

Minimal helper structure:

```javascript
const fs = require('fs');

const inputPath = 'input.json';
const outputPath = 'output.json';
const data = JSON.parse(fs.readFileSync(inputPath, 'utf8'));

if (!Array.isArray(data.athletes)) {
  throw new Error('Expected data.athletes to be an array');
}

fs.writeFileSync(outputPath, JSON.stringify(data, null, 2) + '\n');
console.log(`athletes=${data.athletes.length}`);
```

## Expected Output

When using this skill:

- Choose the simplest safe execution form.
- Avoid Git Bash parsing pitfalls.
- Keep side effects explicit.
- Report what changed or what was measured.

## Do Not

- Do not use heredocs to embed Node code directly in Git Bash.
- Do not rely on fragile quoting for long `node -e` commands.
- Do not overwrite important inputs without an explicit reason.
- Do not leave behind temporary helper files unless they are intentionally useful.
- Do not stage, commit, or push unless explicitly requested.