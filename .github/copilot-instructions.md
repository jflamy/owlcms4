(The file `c:\Dev\git\owlcms_v23\.github\copilot-instructions.md` exists, but is empty)
# Copilot / Assistant Instructions for this repository

Purpose: provide concise, machine- and human-readable guidance for automated
assistants (CoPilot-style agents) working in this repository. This file is the
primary location assistants should read before taking actions.

### MACHINE_DIRECTIVES_START
{
	"version": 1,
	"directives": {
		"no_auto_build": true,
		"no_mvn": true,
		"require_human_consent_for_build": true,
		"do_not_exfiltrate_secrets": true,
		"preferred_shell": "bash",
		"enforce_preferred_shell": true,
		"allowed_shells": ["bash"],
		"shell_commands_should_be_in": "bash",
		"use_warn_for_temp_debug": true,
		"use_warn_with_stacktrace": false,
		"use_warn_with_wherefrom": true,
		"where_from_helper": "LoggerUtils.whereFrom",
		"stack_trace_helper": "LoggerUtils.stackTrace",
		"ci_allows_mvn": true
	}
}
### MACHINE_DIRECTIVES_END

**Quick human summary**
- Do not run `mvn` or perform automatic builds without explicit human
	consent. CI jobs are allowed to run Maven; local/editor automation must not.
- Never exfiltrate secrets — use environment variables or the project's
	secrets manager.
- Always format shell commands for `bash`. On Windows, execute via or Git Bash.

**Why these rules exist (big picture)**
- This repository contains a JVM backend (`pom.xml`, `src/`) and a separate
	frontend and firmware area (`frontend/`, `owlcms-firmata/firmware`). Unchecked
	builds or device flashing can cause side effects; get human approval first.

**Practical examples & patterns**
- Temporary debug logs: use `logger.warn(...)` so debug lines are easy to find.
	Prefer adding the origin helper for clarity:

```bash
logger.warn(LoggerUtils.whereFrom())
```

- Use `LoggerUtils.stackTrace()` only when a full dump is requested by the
	human reviewer.

**Key files and directories to inspect**
- `pom.xml` — project build configuration (Maven). Do not run automatically.
- `src/` — server-side Java sources.
- `frontend/` — web UI sources (Vite/TypeScript).

**Shell examples (always present as bash blocks)**
Run a read-only Maven query (safe) — note: still ask before running builds:

```bash
bash -lc "mvn -q -DskipTests help:effective-pom"
```

If asked to provide commands for Windows users, present them as bash via WSL/Git-Bash, e.g.:

```bash
bash -lc "./mvnw -DskipTests package"
```

**Agent checklist before any action**
1. Read this file in repo root.
2. If action runs `mvn`, builds, deploys, or touches devices, request explicit human consent.
3. Never print or store secrets in logs or PRs.
4. Prefer non-destructive, read-only analyses (grep, static analysis) unless approved.

If something here is ambiguous or you need broader context, ask a human reviewer before proceeding.

**Special-case: JBR / bundled JDK in workspace**
- Detection: check `java.configuration.runtimes` in the workspace (see `jfl.code-workspace`). If the configured `path` contains `jbr` (for example `jbr_jcef...`) the developer is using a JetBrains Runtime/IDE-bundled JDK.
- Behavior: do NOT prompt the user to run a full `mvn` compile when the workspace uses a JBR runtime. Instead, ask the user to reload the IDE window or refresh the project classpath so the language server picks up the bundled JDK.
- Example prompt to show the user:

```text
I detected a bundled JBR runtime in your workspace settings (e.g. `jbr_jcef...`). There's no need to run `mvn` now — please reload the VS Code window (Command Palette -> "Developer: Reload Window") or re-open the workspace to refresh the Java language server.
```

This avoids unnecessary builds while the IDE-provided JDK is in use.

---
If you'd like, I can also add short examples for common developer workflows (build/test/debug) based on this repo's `pom.xml` and `frontend/` tooling — tell me which area to document next.
