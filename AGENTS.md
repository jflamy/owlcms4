# Development Guidance

## Git Commits

- Never add a Copilot `Co-authored-by` trailer to a commit message.

## Architecture Principles

- Avoid the session globals (`OwlcmsSession`, `UI.getCurrent()`) as much as possible. Capture the values at the edge, on the UI thread where the action starts, and pass them down as parameters.
- Avoid disguised globals (`ThreadLocal`, including `AuditContext`) as much as possible. Capture the values and pass them down as parameters, including to background work.
- Log and audit where the act takes place, in the back end, with the cause passed in by the caller. For audit entries, the caller captures an `AuditOrigin` (who, and where the action was triggered from) and passes it down to the function that makes the change.

## Vaadin Concurrency

- Never perform blocking work on the Vaadin UI thread or inside `ui.access(...)`. Run network requests, command execution, and other I/O in a background worker.
- Capture the `UI` on the UI thread and pass it explicitly to background work. Do not call `UI.getCurrent()` from a worker thread.
- Use `ui.access(...)` only to apply the completed result to components, and keep that block as short as possible.
- Treat `ui.access(...)` as a sequencing mechanism, not a reentrant lock. Calling `ui.access(...)` from code already executing in `ui.access(...)` queues the inner block for later execution; it does not mean that the inner block already owns the UI lock. Avoid nested calls.