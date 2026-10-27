# Direct JSON Boot - Remaining Work

Checked against the implementation on 2026-10-27. Completed startup-import
work is omitted. This is a backlog, not a description of available features.

Available: `OWLCMS_LOADJSON` names a `.json`/`.zip` export or a folder, in
which case the most recently modified `.json`/`.zip` directly inside it is
loaded (`StartupImportSource`). The VS Code "load JSON" launch configurations
prompt for a file or folder. Access-mode selection at JSON startup is in
[100](100-PEM_ACCOUNT_MODE.md).

Decision 2026-10-27: **standby mode is dropped.** Loading from JSON is fast
enough that waiting for an operator to pick a checkpoint inside OWLCMS is not
needed; the operator picks the checkpoint folder in the control panel and
OWLCMS loads the latest checkpoint at startup.

## 1. Control panel

In the controlpanel repository:

- **Launch from checkpoint folder:** let the operator choose a folder (or a
  single export) and launch OWLCMS with `OWLCMS_LOADJSON` set to it. Remember
  the last folder; carry the setting across installed-version upgrades.
- **Restart rule:** never restart a child launched with `OWLCMS_LOADJSON` set,
  whether import fails or the application crashes later. Reloading would
  replace competition work. Decide from the child's launch environment, not
  its exit code. Normal launches and UI-import restarts remain unchanged.

## 2. Remaining runtime acceptance

- Start from a V2 export using Postgres.
