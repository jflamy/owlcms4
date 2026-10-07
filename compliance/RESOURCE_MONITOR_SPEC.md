# Periodic CPU / Memory / GC Log (Resource Monitor) — Spec

Status: draft
Scope: owlcms server process, observability only (no UI, no data model change)
Owner files: new `app.owlcms.monitors.ResourceMonitor`, `FeatureSwitch`, `Main`

## Problem

owlcms used to have memory logging, and it was later removed:

- 2022: `FieldOfPlay.handleFOPEvent` logged free/total/max memory on every FOP
  event when `Config.isTraceMemory()` was set. That was removed in `71a4f71d7`.
  It was event-driven rather than periodic, and it reported no CPU.
- 2024: `publicresults/Main` ran a 60 s loop that logged session count and heap
  / non-heap used/committed (`36032f839`). It went away with publicresults
  (`565d4e153`).

There is currently no way to tell from the logs whether a slow or failing
competition site was CPU-starved, short of memory, or thrashing the garbage
collector. Organizers run on laptops, mini-PCs, Docker and cloud hosts. When
something goes wrong, the log file is usually the only evidence we get back.

## Goal

When the feature switch is on, write one compact log line per minute with:

1. CPU usage of the owlcms process and of the whole machine.
2. Physical memory actually used by the owlcms process (RSS / Windows working
   set), with its peak.
3. Heap used / committed / max, plus non-heap used.
4. Garbage-collection activity since the previous line.

The switch can be turned on and off at runtime without restarting. It costs
nothing when off.

## Non-goals

- No metrics backend (Prometheus, OTLP, …) and no new dependency.
  - Vaadin Observability Kit (25.3) was evaluated and rejected. It is
    commercial, Micrometer/Spring Boot based, and exports to backends rather
    than to the log. It also provides no JVM metrics of its own.
  - Plain Micrometer (`LoggingMeterRegistry` + JVM binders) was rejected too.
    It adds a dependency, has no cross-platform RSS collector, and writes many
    lines per interval.
- No UI display of the figures.
- No alerting or thresholds.

## Runtime constraints

Production runs on a standard **Temurin 25 JRE**, not a full JDK:

- Docker: `eclipse-temurin:25-jre`.
- Releases: `temurin` / `java-version: '25'`.
- Control-panel installs: `jdk-25.0.3+9-jre`.

The image contains `java.management`, `jdk.management`, `jdk.jfr` and
`jdk.management.jfr`. Every API below was confirmed with `javap --system`
against the `25.0.3+9` JRE image. They were also exercised on macOS with that
JRE.

| Not available in 25 | Consequence |
|---|---|
| `MemoryMXBean.getTotalGcCpuTime()` (newer JDKs only) | Report GC **pause time** instead of GC CPU time |

## Metrics and sources

| Field | API | Notes |
|---|---|---|
| CPU own | `com.sun.management.OperatingSystemMXBean.getProcessCpuLoad()` | 0..1; negative if unavailable. The first call after startup returns 0 because there is no baseline yet |
| CPU global | `com.sun.management.OperatingSystemMXBean.getCpuLoad()` | 0..1. Container-aware: in Docker it reflects the container's CPU quota, not the host |
| RSS current / peak | JFR event `jdk.ResidentSetSize`, fields `size` and `peak` | See *Process memory* below |
| Heap used / committed / max | `MemoryMXBean.getHeapMemoryUsage()` | `max` may be `-1` (undefined) |
| Non-heap used | `MemoryMXBean.getNonHeapMemoryUsage().getUsed()` | Metaspace + code cache + compressed class space |
| GC per collector | `GarbageCollectorMXBean.getCollectionCount()` / `getCollectionTime()` | Logged as the change since the previous line |
| Machine RAM total / free | `getTotalMemorySize()` / `getFreeMemorySize()` (optional) | Container-aware |

### Process memory (RSS)

No `MXBean` getter exposes the resident set size. On Windows, macOS and Linux,
`getCommittedVirtualMemorySize()` is *virtual* size and is misleading (it is
very large on macOS), so it must not be used.

The JFR periodic event `jdk.ResidentSetSize` is used instead. In JDK 25 it is
backed by `os::rss()` and a per-OS peak:

| OS | `size` | `peak` |
|---|---|---|
| Windows | `GetProcessMemoryInfo` → `WorkingSetSize` (Task Manager "Working set (memory)") | `PeakWorkingSetSize` |
| macOS | `task_info(MACH_TASK_BASIC_INFO).resident_size` | tracked by the JVM |
| Linux / Docker | `/proc/self/...` accurate RSS, falling back to `VmRSS` | `VmHWM` |

Task Manager's default "Memory" column shows the *private* working set, which
is not exposed. The logged figure will therefore read somewhat higher than that
column on Windows.

Consuming the event requires an in-process `jdk.jfr.consumer.RecordingStream`:

- Enable only `jdk.ResidentSetSize`, with a period equal to the log interval.
  No other JFR events are enabled.
- The handler stores the latest `size` / `peak` in volatile fields, and the
  log tick reads them.
- Start the stream when the switch turns on and `close()` it when the switch
  turns off. When off, JFR is not running at all.
- If the stream cannot start (`IllegalStateException`, `SecurityException`, or
  JFR disabled with `-XX:-FlightRecorder`), log one INFO line and omit the
  `rss` segment. Everything else continues.

## Log format

Sample once per interval and make two separate logging calls from the same
snapshot, with independently formatted messages:

- `app.owlcms.monitors.ResourceMonitor` at **INFO** uses the standard file and
  console appenders and their usual format, including timestamp, level and
  source location. Samples are interleaved with ordinary application events.
  Declare this logger explicitly at INFO in `logback.xml` and
  `logback-console.xml`, rather than relying on the inherited `app.owlcms`
  level, so its standard-log output can be disabled independently.
- `app.owlcms.monitors.ResourceMonitor.table` at **INFO** writes only column
  headers and table-like sample rows to a separate daily rolling file,
  `logs/resources.log`. Configure this logger with `additivity="false"` so
  table rows do not propagate to the standard appenders.

Required logger entries (the `RESOURCE_TABLE` appender is defined separately):

```xml
<logger name="app.owlcms.monitors.ResourceMonitor" level="INFO" />
<logger name="app.owlcms.monitors.ResourceMonitor.table" level="INFO" additivity="false">
    <appender-ref ref="RESOURCE_TABLE" />
</logger>
```

Setting the first entry to `level="OFF"` suppresses resource-monitor output
in the normal log, including lifecycle messages and diagnostics, while leaving
the table logger at INFO. Its explicit level and non-additive routing make it
independent of the parent logger's level. Setting the first entry to WARN
instead keeps warnings and errors but suppresses periodic INFO samples.

Do not gate sampling or the table logging call on the standard logger's
`isInfoEnabled()`. The `resourceTraces` feature switch controls the monitor's
lifecycle; logger levels independently control the two output destinations.

The monitor covers the whole process, so the standard-log message has **no**
FOP prefix. If a prefix is used for consistency, it must be the standard
null-FOP prefix (`FOP -`), never a specific platform. The table has no FOP
prefix or column.

### Standard-log sample

```
cpu own 3.8% sys 13.5% | rss 982MB (peak 982) | heap 398/1108/6144MB | nonheap 112MB | gc young +2 (50ms) conc +2 (3ms) old +0 (0ms)
```

Rules:

- Memory figures are in MB (integer division by 1 MiB). Heap is shown as
  `used/committed/max`; when `max` is `-1`, it is printed as `?`.
- CPU is a percentage with one decimal place. A negative value (unavailable)
  is printed as `n/a`.
- GC segments are listed in `getGarbageCollectorMXBeans()` order. Each name is
  shortened to its distinguishing word in lower case:
  - G1 `Young Generation` → `young`
  - G1 `Concurrent GC` → `conc`
  - G1 `Old Generation` → `old`
  - unknown names → the full name with spaces removed.
- GC deltas are `+count (time ms)` since the previous line. The first line after
  enabling shows deltas since the JVM started, prefixed with `since start`.
- For G1, an increase in `old` means a full collection. Explicit
  `System.gc()` calls are also counted there (measured). This spec assumes the
  `DebugUtils.gc()` calls on navigation pages are removed (or moved to an
  admin-page action), so an `old` increase normally indicates heap pressure.
- An interval with no collections still prints the segment (`+0 (0ms)`), so
  the line has a fixed shape and is easy to `grep`, `cut` or chart.

### Table-log sample

The **timestamp is the first column**, including the date and milliseconds.
Use aligned columns with units in the header. A small custom encoder for the
dedicated appender writes only the formatted message and a newline (equivalent
to `%msg%n`); the timestamp belongs to the row, not to a standard logging prefix.
The encoder supplies the matching column headings through `headerBytes()`.
The built-in `outputPatternAsHeader` is not suitable: it prints the logging
pattern, not the table's column headings.

Column order:

1. Timestamp (`yyyy-MM-dd HH:mm:ss.SSS`).
2. Process CPU and system CPU (%).
3. RSS current and peak (MB).
4. Heap used, committed and max (MB).
5. Non-heap used (MB).
6. GC baseline (`since-start` for the first sample after enabling, `interval`
   thereafter).
7. Count delta and time delta (ms) for each collector, in MXBean order.

Use the same values, units and collector-name rules as the standard-log
sample. Unavailable RSS values remain as `n/a` columns rather than disappearing;
unavailable CPU is `n/a` and undefined heap max is `?`. Keep column order stable
within each table.

Enable/disable messages, JFR availability notices, warnings and exceptions go
only to the standard logger. The table logger contains no levels, source
locations, stack traces or lifecycle messages. A sample associated with a
future full-GC warning still produces an ordinary table row.

Configure the dedicated appender in both `logback.xml` and
`logback-console.xml`, preserving their existing standard-log destinations.
Let Logback write the encoder header when it opens the output file, including
after each daily rollover, so each daily table is readable independently.
The monitor does not detect rollover or emit header logging calls. Reopening
an existing file after restart or logging reconfiguration may repeat the
header; repeated headers are acceptable. The encoder and row formatter must
share the same column schema, including the collector columns.

## Behaviour

### Feature switch

- Add `RESOURCE_TRACES("resourceTraces", FeatureSwitchSection.OBSERVABILITY)`
  next to `ATTEMPT_TRACES`, `CLOCK_TRACES` and `PLAYWRIGHT`. It is off by
  default.
- It can be set from the System Settings UI or through the `featureSwitches`
  environment variable / system property (existing mechanism, which overrides
  the stored JSON).
- On each tick, read it with `Config.getCurrent().featureSwitch(...)`. Turning
  it on or off takes effect at the next tick, with no restart and no listener
  wiring.

### Scheduling

- A single daemon `ScheduledExecutorService` thread named
  `owlcms-resource-monitor`, created once. This is the same pattern as the
  `Results` and `MQTTMonitor` schedulers.
- Start it from `Main.initConfig()` after `Config.initConfig()`, so that
  `Config.getCurrent()` is valid on the first tick.
- Fixed rate of 60 s and an initial delay of 60 s. Not configurable in this
  version (see open questions).
- Each tick:
  1. Read the switch. If off: close the JFR stream if it is open, clear the
     previous-sample state, and return.
  2. If on and the JFR stream is not open: open it and log
     `resource monitor enabled`.
  3. Sample once, compute deltas, write the standard-log sample and the
     table-log row from that snapshot, and store the current GC counters/times
     as the new baseline.
- When the switch goes off, log `resource monitor disabled` once.
- Stop it from `Main.prepareForExit(...)`: shut down the executor and close
  the JFR stream.

### Robustness

- The tick body catches `Throwable` and logs it at WARN (without a stack trace
  unless repeated). Otherwise a single exception would cancel all future runs
  of a scheduled task.
- Never call `UI.getCurrent()`, `OwlcmsSession` or Vaadin APIs from the
  monitor thread.
- Every sampling call is a cheap getter; no blocking I/O is performed.
- `com.sun.management.OperatingSystemMXBean` is obtained with an
  `instanceof` check. On a JVM without it, the CPU and machine-RAM fields print
  `n/a`.

## Files to change

| File | Change |
|---|---|
| `owlcms/.../monitors/ResourceMonitor.java` | **New.** Scheduler, JFR stream lifecycle, sampling, formatting (~100 lines) |
| `owlcms/.../monitors/ResourceTableEncoder.java` | **New.** Table row encoding and `headerBytes()`, using the same column schema as the monitor |
| `owlcms/.../data/config/FeatureSwitch.java` | Add `RESOURCE_TRACES` in the `OBSERVABILITY` section |
| `owlcms/.../Main.java` | Start in `initConfig()`, stop in `prepareForExit(...)` |
| `owlcms/src/main/resources/logback.xml` | Add an explicit standard resource logger, a dedicated daily rolling table appender with the custom encoder, and an explicitly INFO, non-additive table logger |
| `owlcms/src/main/resources/logback-console.xml` | Add the same independent resource logger controls and table output while preserving standard console logging |
| `shared/src/main/resources/i18n/resource_monitor_translations.tsv` | **New.** `FeatureSwitch.resourceTraces` row in all languages, following the TSV process. Do not edit `translation4.csv` |
| `docs/.../ReleaseNotes.md` | Release-note entry (via the release-notes process) |

Imports, not fully qualified names. `com.sun.management.OperatingSystemMXBean`
is imported directly. It is the only type used under that name in the class,
so it does not clash with `java.lang.management.OperatingSystemMXBean`.

Proposed English text for the switch description: *Log CPU, process memory,
heap and garbage-collection usage once per minute.*

## Acceptance criteria

1. With the switch off (default), the `owlcms-resource-monitor` thread wakes
   every 60 s, logs nothing, and does not start a JFR recording (`jcmd <pid>
   JFR.check` shows none).
2. Turning the switch on in System Settings produces `resource monitor enabled`
   followed by one sample per minute in the standard log's usual format and
   one matching row in the separate table log, with no restart. The table's
   first column is the timestamp; table rows do not duplicate into the
   standard log.
3. Turning it off produces `resource monitor disabled`, stops the lines, and
   closes the JFR recording.
4. RSS matches the OS tools within a few MB:
   - Linux: `ps -o rss`
   - macOS: Activity Monitor "Real Memory"
   - Windows: Task Manager "Working set (memory)"
5. With `-XX:-FlightRecorder`, lines still appear without the `rss` segment,
   and a single INFO line explains why.
6. A forced allocation burst shows `young` deltas increasing and heap used
   rising, then falling.
7. Shutdown (SIGTERM, control-panel stop) does not hang on the monitor thread.
8. Works on the production `eclipse-temurin:25-jre` image without any JVM flag.
9. The table contains only headers and sample rows, with an encoder-provided
   header on file opening and daily rollover. Lifecycle messages and errors
   appear only in the standard log when its level permits them. Missing RSS
   leaves `n/a` columns in the table.
10. With `resourceTraces` enabled, setting the explicit standard resource
    logger to OFF stops its normal-log output but table samples continue.
    Restoring INFO resumes interleaved samples without restarting the monitor.
    Verify these controls in both logging configurations.

## Measured overhead

Measured on Apple Silicon (macOS, JBR 25.0.3). A low-end Windows laptop may be
roughly 10× slower.

| Item | Cost |
|---|---|
| MXBean sampling + line formatting | ~3 µs per tick warm (first call ~3 ms) |
| JFR `RecordingStream` start | ~0.3–0.4 s CPU, once per enable |
| JFR stream running | ~2 ms CPU per second (≈0.2 % of one core), **same with a 1 s or 5 s period**: the cost comes from the stream's own flush cycle, not from the event period |
| JFR memory | +~26 MB RSS while the stream is open |
| Log volume | ~150 bytes/line: 0.2 MB/day at 60 s, 1.3 MB/day at 10 s (log rolls daily) |

Conclusion: the interval does not drive the cost. The JFR stream (needed only
for RSS) is the only measurable overhead, and it exists only while the switch
is on.

## Decisions

1. Switch name: `resourceTraces`.
3. No Vaadin session count.
4. No thread count or allocation rate.

## Open questions

2. Interval. 60 s is too coarse: `getProcessCpuLoad()` averages since the
   previous call, so a 60 s window smooths away short CPU spikes. Since the
   interval does not affect the cost, the choice is between log volume and
   resolution. Proposal: default 10 s, configurable through
   `OWLCMS_RESOURCE_LOG_SEC`.
5. Full-GC escalation. Precondition: the `DebugUtils.gc()` calls are removed
   from the navigation pages (or moved to an explicit admin action).

   A genuine full GC means G1 could not reclaim enough memory concurrently.
   It does not by itself mean the heap must be increased:
   - heap used stays near max after the collection → heap too small for the
     live data, or a leak. Raise the heap (`-Xmx` / `-XX:MaxRAMPercentage`)
     or investigate;
   - heap used drops well below max → transient burst (for example, a large
     report). No change is needed.

   Proposal: escalate the line to WARN when an allocation-driven full GC
   occurs, and print the heap used after the collection so the reader can
   tell the two cases apart. Use `GarbageCollectionNotificationInfo`
   (`gcCause`, `getGcInfo().getMemoryUsageAfterGc()`) so that an explicit
   `System.gc()` — such as a future admin "GC now" button — is reported as
   INFO and never triggers WARN.
