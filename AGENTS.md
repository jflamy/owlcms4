# Development Guidance

## Vaadin Concurrency

- Never perform blocking work on the Vaadin UI thread or inside `ui.access(...)`. Run network requests, command execution, and other I/O in a background worker.
- Capture the `UI` on the UI thread and pass it explicitly to background work. Do not call `UI.getCurrent()` from a worker thread.
- Use `ui.access(...)` only to apply the completed result to components, and keep that block as short as possible.
- Treat `ui.access(...)` as a sequencing mechanism, not a reentrant lock. Calling `ui.access(...)` from code already executing in `ui.access(...)` queues the inner block for later execution; it does not mean that the inner block already owns the UI lock. Avoid nested calls.