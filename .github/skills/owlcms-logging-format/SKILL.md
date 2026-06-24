---
name: owlcms-logging-format
description: "Use when: adding, reviewing, or changing owlcms Java logging; choosing logger.warn vs logger./**/warn; adding FOP/platform prefixes; making logs filterable by platform; adding temporary visible traces. Keywords: logging, warn, logger.warn, logger./**/warn, FieldOfPlay.getLoggingName, FOP logs, platform logs."
---

# OWLCMS Logging Format

Use this skill when editing Java logging in owlcms, especially logs that should be visible at WARN or filterable by platform/FOP.

## Permanent vs Temporary WARN

- Permanent visible warnings use the comment-armored WARN form:

```java
logger./**/warn("{}message key={} state={}", FieldOfPlay.getLoggingName(fop), key, state);
getLogger()./**/warn("{}message key={} state={}", FieldOfPlay.getLoggingName(fop), key, state);
```

- Temporary visible diagnostic traces use normal `warn` so they are easy to find and remove or demote later:

```java
logger.warn("{}temporary trace value={} {}", FieldOfPlay.getLoggingName(fop), value, LoggerUtils.whereFrom());
getLogger().warn("{}temporary trace value={} {}", FieldOfPlay.getLoggingName(fop), value, LoggerUtils.whereFrom());
```

- If a temporary trace needs a full stack, use the project helper:

```java
logger.warn("{}temporary trace value={}\n{}", FieldOfPlay.getLoggingName(fop), value, LoggerUtils.stackTrace());
```

## FOP / Platform Prefix

- For platform-filterable logs, put the FOP prefix first in the message and pass it as the first argument:

```java
logger./**/warn("{}decision refused state={} athlete={}", FieldOfPlay.getLoggingName(fop), state, athlete);
```

- `FieldOfPlay.getLoggingName(fop)` is the correct null-safe helper. It returns the standard fixed-width prefix, and with `null` it renders `FOP -    `.
- Do not hand-build prefixes such as `"FOP " + fop.getName()`; that is not null-safe and does not preserve the standard spacing.
- When the current object owns a FOP, use `FieldOfPlay.getLoggingName(this)`, `FieldOfPlay.getLoggingName(getFop())`, or `FieldOfPlay.getLoggingName(fop)` as appropriate.
- When entering shared/global code from a FOP context, pass the `FieldOfPlay fop` explicitly into helpers so downstream logs can keep the correct prefix.
- Do not retrieve FOP or locale from `OwlcmsSession` inside background callbacks just for logging; capture/pass the FOP from the event/UI context.

## Levels

- Use `INFO`, `DEBUG`, or `TRACE` for normal progress/timing logs unless the condition deserves operator attention.
- Use WARN for temporary diagnostics only when the user asked for visible traces or the issue must stand out in normal logs.
- Demote or remove temporary `logger.warn` traces after the investigation unless the user explicitly wants to keep them.