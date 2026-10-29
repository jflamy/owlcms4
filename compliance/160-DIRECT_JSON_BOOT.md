# Direct JSON Boot - Remaining Work

Checked against the implementation on 2026-10-27. Completed startup-import
work is omitted. This is a backlog, not a description of available features.

Available: `OWLCMS_LOADJSON` names a `.json`/`.zip` export or a folder, in
which case the most recently modified `.json`/`.zip` directly inside it is
loaded (`StartupImportSource`). The VS Code "load JSON" launch configurations
prompt for a file or folder. Access-mode selection at JSON startup is in
[100](done/100-PEM_ACCOUNT_MODE.md).

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
  its exit code, except for the restart code below. Normal launches and
  UI-import restarts remain unchanged.
  Done for GUI and command-line/systemd launches (`owlcms/startupimport.go`).
- **Import after a startup load:** done. A process cannot change its parent's
  environment, so OWLCMS tells its launcher: after a successful startup load, a
  restart request (UI import, Admin restart, new audit key) exits with code 75
  (`Main.EXIT_RESTART_WITHOUT_STARTUP_IMPORT`) instead of 1. The control panel
  and `MainWrapper` then restart without `OWLCMS_LOADJSON` (and `-DloadJson`),
  for that restart and later ones, so the imported database is kept. Without a
  startup load, restarts still exit with 1. In the cloud, Fly restarts with the
  same secrets, so startup loading should not be used there.

## 2. Remaining runtime acceptance

- Start from a V2 export using Postgres.
