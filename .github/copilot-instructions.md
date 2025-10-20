# Copilot / Assistant Instructions for this repository

Purpose: provide concise, machine- and human-readable guidance for automated
assistants (CoPilot-style agents) working in this repository. This file is the
primary location assistants should read before taking actions.

<!-- markdownlint-disable -->
### MACHINE_DIRECTIVES_START
{
  "version": 1,
  "directives": {
    "no_auto_build": true,
    "no_mvn": true,
    "avoid_fully_qualified_class_names": true,
    "require_human_consent_for_build": true,
    "do_not_exfiltrate_secrets": true,
    "preferred_shell": "bash",
    "enforce_preferred_shell": true,
    "allowed_shells": ["bash"],
    "shell_commands_should_be_in": "bash",
  "do_not_wrap_shell_commands_in_bash_lc": true,
    "use_warn_for_temp_debug": true,
    "use_warn_with_stacktrace": true,
    "use_warn_with_wherefrom": true,
    "where_from_helper": "LoggerUtils.whereFrom",
    "stack_trace_helper": "LoggerUtils.stackTrace",
    "ci_allows_mvn": true
  }
}
### MACHINE_DIRECTIVES_END

## Quick human summary

- Do not run `mvn` or perform automatic builds without explicit human consent. CI jobs are allowed to run Maven; local/editor automation must not.
- Never exfiltrate secrets — use environment variables or the project's secrets manager.
- Always format shell commands for `bash`. On Windows, execute via or Git Bash.
- Never use fully-qualified class names in new or edited Java source files. Prefer adding an import and using the short type name. Only use a fully-qualified name when a collision or other exceptional situation requires it.

Important: The file `shared/src/main/resources/i18n/translation4.csv` is managed by an external i18n tool and must never be edited by automated assistants or by hand in source control. Do not modify `translation4.csv` in this repository; use the external translation management system and the approved process instead. If an update to translations is required, open an issue and request that the translation team apply the change through the canonical workflow.

## Why these rules exist (big picture)

- This repository uses DCEVM (Dynamic Code Evolution Virtual Machine) and automatic code building during local development. Saving a modified Java source file can cause the running application to reload classes. Running full builds or performing device deployments without human consent may disrupt developers' workflows, so request permission before running such operations.

## Practical examples & patterns

- Temporary debug logs: use `logger.warn(...)` so debug lines are easy to find. Prefer adding the origin helper for clarity:

```java
logger.warn(LoggerUtils.whereFrom());
```

- Use `LoggerUtils.stackTrace()` only when a full dump is requested by the human reviewer.

## Key files and directories to inspect

- `pom.xml` — project build configuration (Maven). Do not run automatically.
- `src/` — server-side Java sources.
- `frontend/` — web UI sources (Vite/TypeScript).

## Shell examples (always present as bash blocks)

Run a read-only Maven query (safe) — note: still ask before running builds:

```bash
mvn -q -DskipTests help:effective-pom
```

If asked to provide commands for Windows users, present them as bash via WSL/Git-Bash, e.g.:

```bash
./mvnw -DskipTests package
```

When running python scripts, do not use venvs or activate scripts; just run directly:

```bash
python script.py
```

Common local developer commands (preferred forms):

```bash
# Compile only (fast check; do not run package locally)
mvn -DskipTests compile

# Run the full test suite (slower)
mvn test

# Run a single test class or method (replace MyTestClass and myTestMethod)
mvn -Dtest=MyTestClass#myTestMethod test

# If you must produce a distributable artifact in CI only, use package in CI
# Do NOT run `mvn package` in local/editor automation; prefer `mvn -DskipTests compile`.
```

## Agent checklist before any action

1. If action runs `mvn`, builds, deploys, or touches devices, request explicit human consent.
2. Never print or store secrets in logs or PRs.
3. Prefer non-destructive, read-only analyses (grep, static analysis) unless approved.

4. Do NOT create, stage or commit changes to the repository without explicit human approval.
  - Automated assistants must never perform commits as part of routine actions.
  - If a commit is necessary, obtain a one-line explicit approval from a human and then run the commit manually or set an explicit environment flag (for example: `ALLOW_AUTOMATED_COMMIT=1`) and document the reason in the commit message.
  - This repository enforces human-in-the-loop commits to avoid accidental changes; follow organizational review practices before pushing changes.

## Special-case: JBR / bundled JDK in workspace

- Detection: check `java.configuration.runtimes` in the workspace (see `jfl.code-workspace`). If the configured `path` contains `jbr` (for example `jbr_jcef...`) the developer is using a JetBrains Runtime/IDE-bundled JDK.
- Behavior: do NOT prompt the user to run a full `mvn` compile when the workspace uses a JBR runtime. Instead, ask the user to reload the IDE window or refresh the project classpath so the language server picks up the bundled JDK.

Example prompt to show the user:

I detected a bundled JBR runtime in your workspace settings (e.g. `jbr_jcef...`). There's no need to run `mvn` now — please reload the VS Code window (Command Palette -> "Developer: Reload Window") or re-open the workspace to refresh the Java language server.

This avoids unnecessary builds while the IDE-provided JDK is in use.

---
If you'd like, I can also add short examples for common developer workflows (build/test/debug) based on this repo's `pom.xml` and `frontend/` tooling — tell me which area to document next.
