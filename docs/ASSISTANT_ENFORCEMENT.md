**Assistant Enforcement**

This document explains the machine-readable directives in `ASSISTANT_DIRECTIVES.txt` and provides safe, manual checks contributors can run locally. No automatic enforcement scripts or git hooks are created by default — this file describes manual steps and optional, opt-in workflows.

- **Location of directives**: `ASSISTANT_DIRECTIVES.txt` contains a human-readable section and a JSON block between the markers `### MACHINE_DIRECTIVES_START` and `### MACHINE_DIRECTIVES_END`.
- **Key directives you should respect**:
  - `no_auto_build`: true — do not run repository builds automatically in checks.
  - `no_mvn`: true — avoid committing changes that introduce or call `mvn` or `mvnw` in scripts.
  - `require_human_consent_for_build`: true — builds or deployments must be explicitly approved.
  - `do_not_exfiltrate_secrets`: true — never commit secrets; use environment variables or a secrets manager.
  - `preferred_shell`: `bash` and `enforce_preferred_shell`: true — repository expects bash-compatible scripts. On Windows use `bash -lc "..."`, WSL, or Git Bash.

Manual checks
---------------
Below are lightweight manual checks you (or CI) can run. These are intentionally low-risk and do not perform any builds.

- List staged files:

```bash
git diff --name-only --cached
```

- Check for accidental `mvn` usage in staged files:

```bash
git diff --name-only --cached | xargs -r grep -nE "\\bmvn\\b|\\bmvnw\\b"
```

- Search staged files for likely secrets (heuristic):

```bash
git diff --name-only --cached | xargs -r grep -nE "(API|SECRET|TOKEN|PASSWORD|PASS|KEY)[\\s:=]{1,10}['\"]?[A-Za-z0-9_\\-]{8,}['\"]?|-----BEGIN PRIVATE KEY-----|AKIA[0-9A-Z]{16}"
```

- Ensure shell scripts use bash shebang (for files with `.sh` extension):

```bash
for f in $(git diff --name-only --cached); do
  case "$f" in
    *.sh|*.bash)
      head -n1 "$f" | grep -q "#!.*bash" || echo "WARNING: $f may not have a bash shebang"
      ;;
  esac
done
```

Guidance for Windows contributors
----------------------------------
- The repository's preferred shell is `bash`. On Windows use one of:
  - Git Bash (installed with Git for Windows)
  - WSL (Windows Subsystem for Linux) and run `bash -lc "..."` when examples show shell commands
  - `bash` available in developer shells

- When running the manual grep checks above in PowerShell, prefix with a bash invocation, for example:

```powershell
bash -lc "git diff --name-only --cached | xargs -r grep -nE 'pattern'"
```

Optional (opt-in) automation ideas
----------------------------------
- If the team wants automated checks, consider one of the following (all opt-in):
  - Add a CI job that runs the manual checks above (non-blocking or blocking as desired).
  - Add a `pre-commit` framework configuration (requires contributors to opt into it).
  - Add a local `scripts/check_directives.py` and a `pre-commit` or `git` hook to call it (only with team agreement).

Notes and safety
----------------
- Per `ASSISTANT_DIRECTIVES.txt`, automated builds (`mvn`, etc.) should not be run without explicit human consent. The manual checks above intentionally avoid running any build tools.
- The secret-detection patterns are heuristics. If you encounter false positives, adjust the pattern or exclude specific files in your local checks.

If you want, I can instead add a CI job configuration (for GitHub Actions, GitLab CI, etc.) that runs the checks same as above. Confirm which CI provider you use and I will prepare a non-blocking job.
