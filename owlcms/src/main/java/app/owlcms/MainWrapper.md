# MainWrapper Child Lifecycle

`MainWrapper` starts `app.owlcms.Main` as a separate JVM.  It keeps the wrapper
alive while a JSON import asks OWLCMS to exit with a restartable status, then
starts the replacement JVM.

## Wrapper Exit

During development, VS Code can stop `MainWrapper` with `SIGKILL`.  A killed
wrapper cannot run its shutdown hook, so its child JVM would otherwise continue
to serve OWLCMS as an orphaned process.

On macOS and Linux, `MainWrapper` passes its process ID to each non-daemon child
through `OWLCMS_WRAPPER_PID`.  Early in startup, `Main` resolves that PID with
`ProcessHandle.of(pid)` and registers an `onExit()` callback.  If the wrapper
exits, the callback calls `System.exit(0)` in the child.  This runs OWLCMS
shutdown hooks and leaves no server behind after the IDE stops the wrapper.

The child is not watching a socket or polling.  The JDK completes the
`ProcessHandle.onExit()` future when the wrapper exits.

## Scope and Restart Behavior

The feature is ignored on Windows. Direct `app.owlcms.Main` launches have no
wrapper PID and are unaffected. `MainWrapper` also omits the wrapper PID when
`CONTROLPANEL_RUN_AS_DAEMON` is enabled. It recognizes the same values as the
control panel: `1`, `true`, `yes`, and `on`.

An OWLCMS-initiated restart is unchanged: `Main` exits with its restart code,
the still-running `MainWrapper` observes it, and starts a new child JVM with the
same wrapper PID.  Only wrapper termination causes the child to exit normally.
