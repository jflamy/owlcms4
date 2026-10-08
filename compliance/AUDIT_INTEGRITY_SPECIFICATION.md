# Audit Log and JSON Export Integrity

Audit-record content, routing and actor attribution are defined in [AUDIT_TRAIL_SPECIFICATION.md](AUDIT_TRAIL_SPECIFICATION.md).

## How Signing and Checking Work

### Preparing

- The competition director opens `/admin` and selects **Prepare Audit Integrity**, which opens a dedicated preparation page (§8).
- The page generates the key pair, writes the PEM file to its configured location and prints the key's fingerprint.
- A successful preparation triggers an OWLCMS restart. After the restart, the About page shows the fingerprint of the loaded key under the version number.
- The technical delegate photographs the fingerprint and checks that the About page shows the same one.
- The delegate keeps that record away from the competition machine.

### Signing

- OWLCMS holds a private key.
- Audit lines are grouped into blocks as they are written.
- For each block, OWLCMS computes a SHA-256 hash of the block and appends a signed `SEAL` line containing that hash, the line range and the SHA-256 of the previous block.
- This chains the seals, so blocks cannot be deleted or reordered unnoticed.
- The public key is written in the log itself (`audit.open`).
- JSON exports are covered too: each export's SHA-256 is an audit line, so it is protected by the seal over its block.
- The competition director periodically copies the full logs to a removable device or private network destination and keeps the earlier copies.

### Checking

- From `/admin`, **Check Audit Integrity** opens a dedicated checking page (§7). The checker needs no secret: it reads the public key from the log itself.
- It confirms that every block is unchanged, that the seals form an unbroken chain, and that each seal was made with the private key matching that public key.
- JSON-file comparison is a documented manual process (§4.5). OWLCMS does not retain copies of downloaded JSON files, and the checker does not try to infer which audit line belongs to a supplied file.
- It checks whatever audit logs the user selects. Normally the off-machine copies are placed by hand in the audit log directory of a clean installation and checked there. Running it on the competition machine itself is also valid: it detects any change to sealed content, but the matching fingerprint there is not proof that the key was never substituted; only the delegate's photographed fingerprint settles that.
- It prints the fingerprint of the key that sealed the log, for example: `Log intact. Sealed with key fingerprint d8a3…dde6.`
- For the disputed period, the technical delegate visually compares the fingerprint reported by the checker with the fingerprint photographed before that period began. Nothing is typed into the checker.
- The built-in key is recognized and reported as `builtin key, no protection`, never as verified.

## 0. IWF Context and Scope

This work exists to satisfy IWF competition-management requirements. JSON V2 is the IWF export format; JSON V1 remains available for other federations and for backward compatibility.

### 0.1 Compliance Feature Toggles

Add two independent `FeatureSwitch` values in `SPECIALTY_FEATURES`, both off by default:

- `TCRR_COMPLIANCE` (`tcrrCompliance`) enables compliance with IWF Technical and Competition Rules and Regulations.
- `IWF_COMPLIANCE` (`iwfCompliance`) enables IWF integrity and evidence controls, including cryptographic audit sealing.

Neither switch implies the other. An event requiring both rule compliance and integrity protection must enable both.

| Behavior | Control | Rationale |
|---|---|---|
| Base audit records defined by the audit-trail specification | Always on | Ordinary auditability is useful for every competition. |
| SHA-256 and byte length of every JSON export written to the audit log (§4) | Always on | Cheap, harmless, and useful for any federation. |
| IWF Technical and Competition Rules behavior | `tcrrCompliance` | Rule compliance is independent of cryptographic integrity. |
| Audit-log sealing, signing and integrity-controlled rollover (§2) | `iwfCompliance` | Plain audit logs remain available without cryptographic markers. |
| Signing-key loading and pending-key promotion (§3) | `iwfCompliance` | No key is loaded when integrity controls are off. |
| Real signing key | Optional within `iwfCompliance` | The built-in key is the fallback and is reported as unprotected. |
| Remove the H2 TCP service and permit embedded H2 only (§5) | Always/global | External H2 access is removed independently of both switches. |
| Competition inventory and off-machine log-copy procedure (§6) | `iwfCompliance` | IWF evidence-handover requirement. |
| Final-export declaration with snapshot and audit boundary (§4) | `iwfCompliance` | IWF evidence-handover requirement. |
| Integrity checking page (§7) | Always available to administrators | A clean installation must be able to check evidence without enabling sealing locally. |

Both switches use the normal feature-switch configuration and environment override mechanism. `tcrrCompliance` is controlled only by its configured feature-switch value.

The effective `iwfCompliance` value is:

```text
configured iwfCompliance
OR
real-key configuration present
```

Real-key configuration is present when any of the following exists:

- `OWLCMS_AUDIT_SIGNING_PEM_BASE64`;
- an explicitly configured `OWLCMS_AUDIT_KEY_FILE` override;
- `auditkey.pem.new`;
- `auditkey.pem` at the configured/default audit-key location.

Key presence therefore turns integrity mode on. While a real-key configuration is present, `iwfCompliance` cannot be turned off: a configured false value is overridden, and the UI shows that integrity mode is forced by the key. Removing the key configuration and restarting is required before integrity mode can be disabled.

The effective values are fixed at startup; changing either switch or any key source requires a restart.

When effective `iwfCompliance` is off:

- audit records are written without `FIRST`, `CONTINUE`, `SEAL` or `FINAL`;
- no signing key is loaded or promoted;
- ordinary non-integrity audit rollover applies;
- the checker reports such input as unsealed rather than altered.

When effective `iwfCompliance` is on, all requirements in §§2–3 and §6 apply.

### 0.2 Matching an Export to the Log

To verify a JSON file, compute its SHA-256 and look for an `export.json` audit event with the same `sha256` value. A V2 file contains its own `exportDate` (ISO-8601 instant, written as the second field) which narrows the time window to search. V1 files have no embedded production time; they are matched by checksum alone.

## 1. Final Recommendation

- **Audit log:** always write the ordinary audit trail. When `iwfCompliance` is enabled, retain bounded batches of the exact bytes written by the logger, hash and sign each completed batch asynchronously, and append a `SEAL` line covering audit records X through Y.
- **JSON exports (V1 and V2):** hash the exact exported bytes once, on the fly, and record the SHA-256 and byte length in the audited `export.json` event. The audit seal covering that event authenticates the checksum.
- **Key pair:** under `iwfCompliance`, use a dedicated Ed25519 pair. The competition director generates it and places the PEM file at a fixed location on the competition machine; OWLCMS loads it automatically at every start. Before the competition, the director sends the fingerprint to the technical delegate, who keeps it off the machine. When no key is configured, a public built-in default pair is used; it provides no protection.
- **H2:** remove the H2 TCP server option and permit only process-local embedded H2 URLs. Keep the existing Hikari connection pool.
- **Retention:** the competition director periodically copies the full sealed audit logs to a removable device or private network destination. The off-machine copies are the evidence used to resolve a dispute; they also detect a later local rewrite by anyone able to read the key file.

Changing, deleting or reordering sealed records invalidates a signature, and changing a JSON export makes its digest differ from the sealed `export.json` event. A different key is revealed by a fingerprint that does not match the one held by the technical delegate. The off-machine copy reveals truncation, rollback, and logs rewritten and re-signed with the real key.

The competition director is trusted. The key file protects against everyone else with access to the machine only to the extent that they cannot read it; anyone who can read it can re-sign a rewritten local log, which the off-machine copy exposes. Restarts require no operator action. Compromise of the running process remains outside the guarantee.

Under these constraints, the competition director copies sealed logs to independent safekeeping at operationally chosen intervals. Given the fingerprint recorded by the technical delegate, the Check Integrity program confirms that every retained sealed log section is unchanged. JSON-file comparison, when needed, is performed separately using the documented manual process (§4.5). A successful complete check therefore confirms the absence of tampering in the retained evidence.

The security, access control, availability and retention policy of the independent safekeeping system are outside the scope of this specification.

## 2. Audit Log Sealing

### 2.0 Block Chaining Overview

Each sealed block has:

```text
sha256                 SHA-256 of the exact audit-record bytes in this block
previousBlockSha256    SHA-256 of the immediately preceding block
```

The marker containing those values, the covered sequence range and the run and file identity is signed with the private key.

For three blocks whose SHA-256 values are `H1`, `H2` and `H3`, the log contains:

```text
# Ordinary log lines in block H1
    1 | 08:00:00.000 | SYSTEM       | audit.open         | ... runId=...,publicKey=...,fingerprint=...

# Signed control line that seals block H1
FIRST {
  firstLine: 1,
  lastLine: 1,
  sha256: H1,
  signature: ...
}

# Ordinary log lines in block H2
    2 | 08:00:00.010 | SYSTEM       | application.started | ...
    3 | 08:02:14.221 | WEIGHIN      | athlete.change      | ... bodyWeight - -> 81.35
    4 | 08:02:14.221 | WEIGHIN      | athlete.change      | ... declaration - -> 120
      ...
   50 | 08:14:49.571 | REFEREE#3    | referee.vote        | ... decision=GOOD
   51 | 08:14:49.604 | ANNOUNCER    | referee.decision    | ... decision=GOOD

# Signed control line that seals block H2 and links it to H1
SEAL {
  firstLine: 2,
  lastLine: 51,
  sha256: H2,
  previousBlockSha256: H1,
  signature: ...
}

# Ordinary log lines in block H3
   52 | 08:14:50.019 | MARSHAL      | athlete.change      | ... declaration 121 -> 122
   53 | 08:15:02.817 | TIMEKEEPER   | clock.start         | ...
      ...
   95 | 08:16:08.101 | REFEREE#2    | referee.vote        | ... decision=NO_LIFT
   96 | 08:16:08.136 | ANNOUNCER    | referee.decision    | ... decision=NO_LIFT

# Signed control line that seals block H3 and links it to H2
SEAL {
  firstLine: 52,
  lastLine: 96,
  sha256: H3,
  previousBlockSha256: H2,
  signature: ...
}

# Signed control line that closes this run segment
FINAL {
  recordCount: 96,
  lastBlockSha256: H3,
  signature: ...
}
```

The checker processes each block in order:

1. Read the exact audit records covered by the marker, including their line terminators.
2. Compute their SHA-256 and compare it with the marker's `sha256`.
3. Verify the marker's signature using the public key in `audit.open`.
4. Compare `previousBlockSha256` with the preceding block's `sha256`.
5. Confirm that the covered sequence ranges are adjacent and do not overlap.

Tampering is detected as follows:

| Change | Why checking fails |
|---|---|
| Edit a record | The computed block SHA-256 changes. |
| Delete a record | The block SHA-256 changes or the sequence range has a gap. |
| Insert a record | The block SHA-256 changes or ranges overlap. |
| Delete a whole block | The following marker refers to its missing SHA-256. |
| Reorder blocks | `previousBlockSha256` no longer matches. |
| Change a hash, range, filename or run ID in a marker | The marker's signature fails. |
| Replace a marker | Its signature fails unless the replacement was made with the private key; the off-machine copy detects re-signing with the real key. |

The first block of a process run contains only the `audit.open` record:

```text
record 1: audit.open

FIRST {
  firstLine: 1,
  lastLine: 1,
  sha256: SHA-256(exact audit.open record bytes),
  signature: ...
}
```

`FIRST` has no `previousBlockSha256`, because it starts a new process chain.

At size rollover, the old file ends:

```text
SEAL {
  sha256: H47,
  previousBlockSha256: H46,
  ...
}

FINAL {
  recordCount: 1834,
  lastBlockSha256: H47,
  signature: ...
}
```

The new file resets its sequence and starts:

```text
record 1: audit.open {
  runId: same process run,
  previousFile: A_full_2026-10-08T09-16-40.123Z.log,
  previousBlockSha256: H47,
  ...
}

CONTINUE {
  firstLine: 1,
  lastLine: 1,
  sha256: H48,
  previousFile: A_full_2026-10-08T09-16-40.123Z.log,
  previousBlockSha256: H47,
  signature: ...
}

record 2: ...

SEAL {
  sha256: H49,
  previousBlockSha256: H48,
  ...
}
```

`CONTINUE` both seals the new file's `audit.open` block (`H48`) and links the new file to the named old file ending at `H47`.

The block SHA-256 is already the block fingerprint. It is not hashed again. The marker itself is signed, so the checker obtains the required protection by verifying every marker signature, every block SHA-256, every adjacent sequence range and every predecessor link.

A process restart starts a new chain:

```text
record 1: audit.open with a new runId
FIRST: no previousBlockSha256
```

There is deliberately no block-chain link across process runs. The off-machine copies and inventory detect an omitted run, and the fingerprint reported for each run identifies the key that signed it.

### 2.1 Capture and Batch

For each `_full.log` stream:

1. Encode each ordinary audit record once, including its actual line terminator.
2. Write those bytes through the normal appender and retain the same bytes in the active bounded batch.
3. Close the batch right after a natural boundary (a saved lift result, an athlete saved from the weigh-in or registration station, or 5 minutes of idle time with unsealed records), when the batch reaches its fallback age, record count or byte size, or at rollover, shutdown or finalization.
4. Freeze the batch and transfer it to a background worker while logging continues into a new batch.
5. Hash the frozen bytes with SHA-256, sign the seal payload with Ed25519, and return the completed seal to the owning appender.
6. Append the seal through the same serialized writer, bypassing ordinary audit capture so seals cannot recursively enter a batch.

Only the `_full.log` streams are sealed. The readable `.log` is not sealed separately: each of its lines is the leading part of the corresponding `_full.log` line, ending just before ` | cause=`. Sealing the full line therefore covers the readable line, and the checker can regenerate the readable log from the sealed full log to confirm they agree.

Batches are non-overlapping. Each ordinary record belongs to exactly one batch. Bound both individual batch size and total queued bytes. If the worker cannot keep up, apply defined backpressure or report an integrity-degraded state; never discard an unsealed batch silently.

Batches are closed at natural boundaries:

- **A lift.** On a platform stream, the batch is closed as soon as the lift result has been saved to the athlete (together with any new or cancelled records). If the save happens while the triggering event is being processed, the batch is closed after that event's own audit record is written. The batch is closed again after a `jury.decision` record, which is written after the jury's change has been saved. A seal then covers everything logged for that lift: attempt changes, clock events, votes, the decision and the saved result. Decision visibility is not the boundary: with `showDecisionsImmediately` the decision is shown before the reversal window ends and saved afterwards.
- **An athlete saved from the weigh-in or registration station.** One form save writes one `athlete.change` record per changed field (station `WEIGHIN` or `REGISTRATION`). The batch is closed after the last record of that save. Weigh-in records go to the platform stream of the athlete's session, which may not be lifting at the time (before the first session, between sessions, or on a weigh-in day), so lift decisions alone would leave them unsealed.

- **Five minutes of idle time, on every stream.** When no new record has arrived for 5 minutes and there are unsealed records, the batch is closed. If the last line is already a seal, nothing is done. On the competition-wide stream, which records logins (a burst at the start of the day, then occasional ones) and exports (about every hour), this is the usual trigger: the morning login burst is sealed 5 minutes after the last login, and each export 5 minutes after it is written. On platform streams it seals anything left after the last lift or athlete save, for example during a break.

Remaining cases, such as continuous activity that never reaches a natural boundary, are covered by fallback limits:

| Limit | Value |
|---|---|
| Block byte size | 1 MiB (1,048,576 bytes) |
| Ordinary records per block | 500 |
| Block age | 5 minutes from the oldest unsealed record |
| Frozen data awaiting signing | 8 MiB (8,388,608 bytes) |

The first block limit reached closes the block, independently of the five-minute idle trigger. The age timer operates only while records are unsealed, so empty periods produce no seals. The signing-queue bound applies across streams; reaching it applies explicit backpressure and must never discard a block silently. After an abrupt stop, at most the records since the last seal are reported as not validated (§2.4).

### 2.2 Inline Seal

A seal can appear after records written later than the range it covers. Four signed control lines are used: `FIRST` or `CONTINUE` to seal the file's header, `SEAL` for each block, and `FINAL` to close the file.

A file starting a new process run:

```text
<record 1: audit.open header>
FIRST    {"version":1,"firstLine":1,"lastLine":1,"sha256":"H1","keyFingerprint":"...","signature":"..."}
<records 2 through 1042>
SEAL     {"version":1,"firstLine":2,"lastLine":1000,"sha256":"H2","previousBlockSha256":"H1","keyFingerprint":"...","signature":"..."}
<record 1043>
...
FINAL    {"version":1,"recordCount":2417,"lastBlockSha256":"HN","keyFingerprint":"...","signature":"..."}
```

The next file of the same run, created by size-based rollover:

```text
<record 1: audit.open header, naming the previous file>
CONTINUE {"version":1,"firstLine":1,"lastLine":1,"sha256":"H1","previousFile":"A_full_2026-10-08T09-16-40.123Z.log","previousBlockSha256":"<lastBlockSha256 from the previous FINAL>","keyFingerprint":"...","signature":"..."}
<records 2 through 980>
SEAL     {"version":1,"firstLine":2,"lastLine":980,"sha256":"H2","previousBlockSha256":"H1","keyFingerprint":"...","signature":"..."}
...
FINAL    {"version":1,"recordCount":1530,"lastBlockSha256":"HN","keyFingerprint":"...","signature":"..."}
```

`firstLine` and `lastLine` use the segment-local audit sequence. It starts at 1 for every `audit.open`, including after size rollover and after process restart. `FIRST`, `CONTINUE`, `SEAL` and `FINAL` do not consume sequence numbers. The run ID and filename distinguish equal sequence numbers from different segments.

Hash the exact concatenated bytes of the declared ordinary records, including line terminators. Emit seals in batch order.

- `FIRST` seals the `audit.open` header alone, immediately (§2.3). It has no `previousBlockSha256`: the chain of a new process run starts there.
- `CONTINUE` seals the new file's `audit.open` header alone, immediately after size rollover. Its `previousBlockSha256` equals the `lastBlockSha256` in the previous file's signed `FINAL`, and it identifies that file by name. The checker requires the named file and its valid `FINAL` for a complete cross-file check, so a whole file cannot be deleted or replaced unnoticed.
- `SEAL` seals one block of ordinary records. Its `previousBlockSha256` is the `sha256` of the immediately preceding block sealed by `FIRST`, `CONTINUE` or `SEAL`.
- `FINAL` closes the current file generation or run segment at rollover or orderly shutdown (§2.4). It contains the total record count and repeats the `sha256` of the last sealed block as `lastBlockSha256`.

The block SHA-256 is already the fingerprint used for chaining; it is not hashed again. Every marker is signed, so the block hash, predecessor hash, ranges, run ID and filenames cannot be changed without invalidating the signature. The checker independently verifies every signature and requires adjacent, non-overlapping ranges.

The examples above abbreviate the marker payloads. The complete signed schemas are:

**Common fields in every marker**

| Field | Meaning |
|---|---|
| `version` | Seal schema version. Version 1 fixes SHA-256 and Ed25519. |
| `marker` | `FIRST`, `CONTINUE`, `SEAL` or `FINAL`. |
| `stream` | Audit stream, for example `competition` or `A`. |
| `runId` | Process-run identifier from `audit.open`. |
| `artifactName` | Relative `_full.log` filename containing the marker. |
| `sealedAt` | UTC time at which the marker was produced. |
| `applicationVersion` | OWLCMS version. |
| `keyFingerprint` | Full SHA-256 fingerprint of the signing public key. |

**Additional `FIRST` fields**

| Field | Meaning |
|---|---|
| `firstLine` | `1`. |
| `lastLine` | `1`. |
| `byteCount` | Exact byte count of the `audit.open` record, including its line terminator. |
| `sha256` | SHA-256 of those exact bytes. |

`FIRST` has no predecessor field.

**Additional `CONTINUE` fields**

| Field | Meaning |
|---|---|
| `firstLine` | `1`. |
| `lastLine` | `1`. |
| `byteCount` | Exact byte count of the new file's `audit.open` record, including its line terminator. |
| `sha256` | SHA-256 of those exact bytes. |
| `previousFile` | Relative filename of the preceding size-rolled file. |
| `previousBlockSha256` | `lastBlockSha256` from that file's valid signed `FINAL`. |

**Additional `SEAL` fields**

| Field | Meaning |
|---|---|
| `firstLine` | First run-segment sequence number covered by this block. |
| `lastLine` | Last run-segment sequence number covered by this block. |
| `byteCount` | Exact byte count of all covered records, including their line terminators. |
| `sha256` | SHA-256 of those exact concatenated bytes. |
| `previousBlockSha256` | `sha256` from the immediately preceding `FIRST`, `CONTINUE` or `SEAL`. |

**Additional `FINAL` fields**

| Field | Meaning |
|---|---|
| `recordCount` | Number of ordinary audit records in the current segment. |
| `lastBlockSha256` | `sha256` from the last `FIRST`, `CONTINUE` or `SEAL` in the segment. |

`FINAL` does not cover a new block and therefore has no `firstLine`, `lastLine`, `byteCount` or new block `sha256`.

Each rendered control line has the form:

```text
MARKER {all signed fields...,"signature":"<Base64 Ed25519 signature>"}
```

The `marker` value is included inside the signed JSON even though it is also printed before the JSON. Version 1 signs the UTF-8 bytes of `OWLCMS-AUDIT-SEAL:1\n` (with one literal LF byte) followed by RFC 8785 canonical JSON of every field except `signature`. Marker fields contain only strings and nonnegative integers no greater than 9,007,199,254,740,991. Unknown fields, duplicate JSON keys, unsupported versions, invalid ranges and unpaired Unicode surrogates are rejected. Batch numbers are unnecessary because signed sequence ranges and predecessor hashes establish ordering.

### 2.3 Public Key and Fingerprint

The public-key fingerprint is the full SHA-256 of the Ed25519 public key's DER SubjectPublicKeyInfo encoding.

Each segment starts with `audit.open` at sequence 1 and repeats the run ID, Base64 SubjectPublicKeyInfo public key, full fingerprint and `keySource` (`builtin` when the built-in default key of §3.0 is used, `configured` otherwise).

A file may hold several runs. A restart appends a new `audit.open` and `FIRST` to the latest file for that stream when it remains below the rollover size. If the previous run was stopped abruptly, its last lines are unsealed and are reported as not validated (§2.4). The checker splits a file at every `audit.open`, not only at its first line.

At process start or restart, `audit.open` has no predecessor fields and is sealed immediately by `FIRST`. After size rollover in the same process run, the new file's `audit.open` also names the previous file and repeats the previous signed `FINAL` line's `lastBlockSha256`; it is sealed immediately by `CONTINUE`. The filename plus that last-block hash is the link to the previous file. If the process crashes before the next `SEAL`, the segment still has a validated header identifying the run and key. A restart may use the same key or a different key. Block chains do not cross process runs, and verification selects the applicable key separately for each run.

Because every segment starts with a sealed `audit.open` containing the public key, one file can be checked independently when only an event in that file is disputed. That independent check can verify the file's signatures, covered records and internal block chain. If the file starts with `CONTINUE`, checking it without the named previous file cannot establish cross-file continuity or completeness; the report must say that the predecessor link was not checked.

The public key is not secret. A verifier trusts it only when its fingerprint matches the one the technical delegate recorded before the competition (§3.1).

### 2.4 Rollover, Shutdown and Failure

The audit component owns authoritative `_full` file writing and rollover; it does not delegate rollover to Logback's `TimeBasedRollingPolicy`. Rollover is based on file size, not time:

1. Finish and seal the current block.
2. If the file has reached the configured target size, append `FINAL`, flush and close it.
3. Open the next file, reset the sequence to 1, write `audit.open` with the previous filename and last-block hash, and immediately write `CONTINUE` before accepting another ordinary record.

Rollover never splits a block. The file target is 10 MiB (10,485,760 bytes). A file may exceed the target by at most one bounded block plus its control lines. Block limits are specified in §2.1.

Authoritative filenames contain the stream name and the UTC ISO date-time at which the file was opened. Colons are rendered as hyphens for filesystem portability:

```text
A_full_2026-10-08T09-16-40.123Z.log
A_full_2026-10-08T14-05-02.481Z.log
competition_full_2026-10-08T09-16-40.127Z.log
```

The ISO date-time makes sequencing implicit and the file is created with create-new semantics so an existing file is never overwritten. The signed `previousFile` plus `previousBlockSha256` link remains the authoritative order if the system clock is adjusted. Time does not trigger rollover.

At orderly shutdown, close the partial batch, drain pending seals and append the signed `FINAL` line containing the current run's total audit-record count (§2.2). The file may be reopened in append mode by a later run if it remains below the rollover target; the previous `FINAL` then ends that run segment, not necessarily the physical file forever.

The audit logging component owns this: it registers its own JVM shutdown hook to write `FINAL`, and does not depend on Logback stopping its appenders or on any other part of the application. Logback flushes each line as it is written, so records already written survive any exit. The hook runs on a normal exit, Ctrl-C or SIGTERM, but not on a forced kill, a JVM crash or a power loss, so `FINAL` is best-effort and is never relied upon.

Before declaring a generation complete, read it back and compare every covered range with its signed commitment. Do not sign bytes reread from disk as a replacement for lost original batches.

After a crash, the file has no `FINAL` line and the records after the last valid seal are unsealed. Nothing is recovered or re-signed: the checker reports those records, and any incomplete seal, as not validated. The next run starts a new chain with `audit.open` and its `FIRST` line.

The implementation uses portable Java cryptography and file APIs. Exact-byte verification intentionally detects newline conversion. Test size-based rollover, restart and recovery on Windows, macOS and Linux.

## 3. Signing Key Provisioning

Use a dedicated Ed25519 pair. Do not reuse the installation AES key, API credentials or password hashes.

Keep `~/.owlcms/key` in its current format. It remains the raw Base64-encoded 32-byte AES key read by Java and Go; it is not converted to PEM.

### 3.0 Built-in Default Key

When `iwfCompliance` is explicitly enabled and no real-key configuration is present (§3.1), OWLCMS uses a built-in Ed25519 pair compiled into the application. A real key is never mandatory within integrity mode. Development and tests explicitly enable `iwfCompliance` to exercise sealing with this pair.

The built-in private key is public and provides **no protection**: anyone can produce valid seals with it. Its fingerprint must never be registered as trusted. Each run using it records `keySource=builtin` in `audit.open`, and the startup log states that audit logs are sealed with the built-in key.

```text
OWLCMS_AUDIT_SIGNING_PEM_BASE64=LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tCk1DNENBUUF3QlFZREsyVndCQ0lFSUQ4RGozSTBwcmpZQkVUSjJoWm1VRjZ0WWRPbkJXVGU3QUhnNXBLdGhlOVQKLS0tLS1FTkQgUFJJVkFURSBLRVktLS0tLQotLS0tLUJFR0lOIFBVQkxJQyBLRVktLS0tLQpNQ293QlFZREsyVndBeUVBT1d1UUltWEVTZ3JkTGd5Tk53T0s1bDhiek9BNFlKS1F2Q2tUMVJLdnZGRT0KLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0tCg==
fingerprint=d8a318725d89bf63d698a09cb3c10684ac6e76c05bda32318da8b8d71775dde6
```

The same values may be placed in a development environment file and used as a test fixture.

### 3.1 Director-Held Key File

Roles:

- The **competition director** is trusted. The director generates the pair, places the PEM file on the competition machine and protects it.
- The **technical delegate** receives the fingerprint from the director before the competition and keeps it away from the competition machine. In a dispute, verification uses this recorded fingerprint, never one supplied afterwards.

The PEM file contains both blocks:

```text
-----BEGIN PRIVATE KEY-----
<Base64 PKCS#8 Ed25519 private key>
-----END PRIVATE KEY-----
-----BEGIN PUBLIC KEY-----
<Base64 SubjectPublicKeyInfo Ed25519 public key>
-----END PUBLIC KEY-----
```

A pair can be generated and its fingerprint shown with standard tools:

```bash
openssl genpkey -algorithm ed25519 -out auditkey.pem
openssl pkey -in auditkey.pem -pubout >> auditkey.pem
openssl pkey -in auditkey.pem -pubout -outform DER | shasum -a 256
```

The default key path is:

```text
~/.owlcms/auditkey.pem
```

OWL CMS resolves this programmatically from the operating-system user home directory:

```java
Path home = Path.of(System.getProperty("user.home"));
Path keyFile = home.resolve(".owlcms").resolve("auditkey.pem");
```

The default is equivalent to the machine-independent setting:

```text
OWLCMS_AUDIT_KEY_FILE=~/.owlcms/auditkey.pem
```

OWL CMS, not the shell or environment-file parser, expands a leading `~/` or `~\`. On Windows, if `user.home` is `C:\Users\Director`, the resolved path is:

```text
C:\Users\Director\.owlcms\auditkey.pem
```

This is independent of the OWLCMS installation directory and version. Multiple installed versions and an upgraded version use the same key when they run under the same operating-system account. It follows the existing version-independent `~/.owlcms/key` convention used for the installation secret.

`OWLCMS_AUDIT_KEY_FILE` optionally overrides the path. Resolution is:

1. Trim surrounding whitespace.
2. If the value is exactly `~`, use `user.home`.
3. If it begins with `~/` or `~\`, remove that prefix, resolve the remainder against `user.home`, and normalize it.
4. Reject a home-relative result that does not still start with the normalized home path (for example `~/../other/auditkey.pem`).
5. Otherwise parse and normalize the value and require it to be an absolute path.

No `~otheruser`, `$HOME`, `${HOME}` or `%USERPROFILE%` expansion is performed.

The setting may be placed in a local environment file, the process environment or cloud configuration. The same portable value works on Windows, macOS and Linux:

```text
OWLCMS_AUDIT_KEY_FILE=~/.owlcms/auditkey.pem
```

Windows environment files may also use an absolute path with forward slashes:

```text
OWLCMS_AUDIT_KEY_FILE=E:/owlcms/auditkey.pem
```

An override on macOS may point to a removable device:

```text
OWLCMS_AUDIT_KEY_FILE=/Volumes/OWLCMSKEY/auditkey.pem
```

The pending file is the resolved path with `.new` appended, for example `~/.owlcms/auditkey.pem.new`.

OWL CMS reads the resolved path at every start, so restarts need no operator action, and logs the fingerprint and `keySource=configured` in `audit.open`. The override may point to a removable device that the director plugs in before starting OWLCMS and removes afterwards; OWLCMS keeps the key in memory for the lifetime of the process.

Environment-file and cloud deployments may supply the complete PEM through the single-line secret `OWLCMS_AUDIT_SIGNING_PEM_BASE64`. Its value is the Base64 encoding of the complete PEM file, including both blocks and their line terminators:

```bash
openssl base64 -A -in auditkey.pem
```

At startup, OWLCMS Base64-decodes the value to PEM text and applies the same pair validation as for a file. The outer Base64 layer only flattens the PEM for `.env` and cloud-secret settings; it is not encryption and the variable must be treated as a secret.

The `/admin` preparation page is a convenience, not the only provisioning method. An administrator may manually:

- place a complete valid `auditkey.pem` at the configured path and start or restart OWLCMS; or
- place a complete valid `auditkey.pem.new` beside it and restart OWLCMS, causing startup to validate and promote the pending file.

Startup precedence is:

1. a valid `OWLCMS_AUDIT_SIGNING_PEM_BASE64`;
2. otherwise, a valid `auditkey.pem.new`, which is promoted to `auditkey.pem`;
3. otherwise, a valid configured `auditkey.pem`;
4. otherwise, the built-in key when `iwfCompliance` was explicitly enabled without a real-key configuration.

If the environment variable is present but cannot be decoded or does not contain a valid matching pair, startup fails visibly; it does not continue to the files. When the environment variable is present, startup does not promote, delete or otherwise modify `auditkey.pem.new` or `auditkey.pem`. If a configured or pending file is selected but invalid, startup likewise fails instead of continuing to the next choice.

### 3.2 Validation and Exposure

OWL CMS determines effective `iwfCompliance` from the configured switch and key presence before audit logging starts. Any real-key configuration forces integrity mode on and is validated. If an environment key or file is configured but cannot be decoded, is missing, incomplete, malformed or mismatched, startup fails visibly: it must neither fall back to the built-in key nor silently disable integrity mode. Key replacement requires explicit, recorded rotation, and the new fingerprint must reach the technical delegate. Retain old public keys so existing evidence remains verifiable.

Only when `iwfCompliance` is configured off and no real-key configuration is present does OWLCMS skip key loading, pending-file promotion and signing initialization.

The PEM file is not secret from anyone who can read it, and its fingerprint can be computed from it; that is acceptable because trust comes from the fingerprint recorded in advance by the technical delegate, not from secrecy of the fingerprint. A key generated by anyone else has a different fingerprint and its sections are reported as untrusted. Anyone able to read the real key file can rewrite and re-sign the local logs; the off-machine copy of §6 exposes such a rewrite.

## 4. JSON Export Checksums

Applies to both JSON V1 and JSON V2. Always on; not gated by `iwfCompliance` (§0.1).

### 4.1 Streaming Computation

JSON export serialization streams through a `PipedOutputStream` on a writer thread while the consumer reads the `PipedInputStream`. Compute the checksum on the fly by wrapping the pipe's output side in a `java.security.DigestOutputStream` (SHA-256) together with a byte counter:

```text
Jackson writer ──► DigestOutputStream(SHA-256) ──► PipedOutputStream ──► PipedInputStream ──► consumer
                   (and byte count)
```

Every byte that reaches the pipe updates the digest; nothing is buffered and nothing is re-serialized. The digest and count are final when the writer closes the stream. Generate the JSON once; do not regenerate it to compute the checksum.

Implementation: one shared helper replaces the four duplicated pipe/thread blocks in `CompetitionData.exportData(...)` and `CompetitionDataV2.exportData(...)`.

### 4.2 Audit Event

When the writer completes, it writes a competition-wide `export.json` audit event from the writer thread. The requesting actor is captured on the calling thread before the writer thread starts and passed in; `UI.getCurrent()` is not available on the writer thread.

Success detail:

```text
format=2,channel=download,bytes=2948571,sha256=<64 hex characters>
```

Failure detail (no digest is ever reported for a partial artifact):

```text
format=2,channel=download,outcome=failed,reason="..."
```

Fields:

- `format`: `1` or `2`;
- `channel`: `download` (Vaadin button), `http` (`/competition/export...` servlet), `websocket` (tracker forwarding), `internal` (serialization of a prepared instance by tests or tooling);
- `bytes`: exact byte length of the serialized JSON;
- `sha256`: lower-case hex SHA-256 of the exact serialized bytes.

Under `iwfCompliance`, a final export additionally declares its snapshot and audit boundary.

### 4.3 Writer Failure Propagation

The checksummed stream must surface a writer failure to the reader. A truncated export must never appear as normal EOF and must never receive a successful checksum audit event.

### 4.4 WebSocket Path and the Tracker's `databaseChecksum`

The artifact SHA-256 and tracker `databaseChecksum` have different meanings and must remain separate:

- the artifact SHA-256 covers the exact `competition.json` bytes, including `exportDate`, and is written to the audit log;
- `databaseChecksum` is a cache-busting token over the same bytes with `exportDate` and the encrypted `updateKey`
  values removed (the latter are re-encrypted with a fresh nonce on every serialization). It is not written to the
  audit log.

`tracker-core` compares `databaseChecksum` with string equality against the last accepted value to skip a reload, and
plugins use it as a cache-key component. OWLCMS sends it in a `database_metadata` WebSocket API message immediately
before the corresponding `database_zip` binary frame. Tracker-core retains it per connection and attaches it to the
parsed database before cache processing. It is no longer sent in `update` messages, where the tracker ignored it.

The ZIP remains a compression container only. It contains the exact, audited `competition.json` and no protocol
metadata. `DatabaseZipHelper` places the already-serialized bytes in `competition.json` instead of re-serializing the
parsed structure.

### 4.5 Manual JSON Verification

OWLCMS does not retain downloaded JSON files, and the integrity checker does not try to infer which `export.json` audit event belongs to a supplied file. JSON verification is a documented manual process:

1. Compute the SHA-256 of the disputed JSON file over its exact bytes using a common tool:

   ```bash
   openssl dgst -sha256 disputed.json
   ```

   `shasum -a 256 disputed.json` must produce the same hexadecimal value.
2. Note the exact byte length using an operating-system tool.
3. Search the sealed `_full` audit logs for an `export.json` event with the same `sha256` and `bytes` values. The V2 `exportDate` and audit timestamp may help narrow the search, but the operator chooses the audit event; the checker does not infer it.
4. Verify the seal covering that selected audit event.

Tests must confirm that Java's streaming SHA-256, `openssl dgst -sha256` and `shasum -a 256` produce the same hexadecimal digest for identical fixture bytes on supported platforms.

The JSON schema remains unchanged and no separate JSON signature or checksum sidecar is required. The checksum identifies exact bytes; it does not establish snapshot consistency. For a final export, pause relevant edits or use a proven consistent snapshot and declare its audit boundary.

**Future step:** the `/admin` integrity page may accept a JSON upload solely to compute and display its byte length and SHA-256. It must not automatically select or claim a match with an audit event; the operator performs that comparison manually.

## 5. Embedded H2 Only

Remove the external H2 server capability globally while preserving the application's process-local connection pool. This is not gated by `iwfCompliance`:

1. Remove `startH2EmbeddedServer()`, its call sites, the `embeddedH2Server` flag and the `org.h2.tools.Server` import.
2. Remove `H2ServerPort` / `OWLCMS_H2SERVERPORT` support and its row in [docs/100-AdvancedTopics/140-Configuration.md](../docs/100-AdvancedTopics/140-Configuration.md).
3. Before creating the pool, allow only embedded `jdbc:h2:file:` and named `jdbc:h2:mem:` URLs for H2 deployments.
4. Reject TCP and SSL H2 URLs, `AUTO_SERVER=TRUE`, `FILE_LOCK=NO`, `INIT`, duplicate options and unsupported options.
5. Let H2 enforce its normal file lock. If another process owns the database, startup fails visibly without fallback.
6. Preserve the embedded H2 dependency and Hikari's minimum 5 and maximum 15 process-local connections.

A loopback listener is not an acceptable substitute for removing server startup. PostgreSQL deployments require their own network and authentication policy and are outside this recommendation.

## 6. Completeness and Independent Retention

The competition director periodically copies the rolled, finalized audit logs — files no longer being written — to a removable device or private network destination. This is an operational procedure; OWLCMS does not transfer the logs or require a receiving protocol. The file currently being written is not locked, so it can be copied, but the copy is only the prefix that existed at that instant; it has no `FINAL` and the checker reports it as `PARTIAL`. The **Audit Log Snapshot** admin action (`/admin/audit/snapshot`) gives the director a quiet point to copy from: it optionally writes a database export (recorded as `export.json` with `channel=audit`), writes an `audit.snapshot` line, forces every sealed stream into a new file so all previous files are finalized, and downloads a zip of the entire `logs` tree — sealed audit files, readable audit files and application logs alike, since the snapshot doubles as a diagnostic backup — optionally with the `local` folder. Only the sealed files just opened by the roll are omitted, as they hold nothing yet. Checked, the sealed files from this run are `COMPLETE`; a tail left by an earlier crash is reported as `PARTIAL`, as it should be.

Each copy is kept as a timestamped snapshot and does not overwrite earlier snapshots. Full logs are copied, not only seals, because resolving a dispute requires the original records, not merely proof that something changed. Downloaded JSON files are not retained by OWLCMS or by this procedure.

In a dispute, the latest relevant off-machine snapshot is the evidence examined. A rewrite re-signed with the real key file (§3.2) cannot alter a snapshot already taken off the machine. The exposure is limited to records created or changed since the most recent retained copy.

Maintain a competition inventory listing every expected stream, file generation and process run, including each run ID and public-key fingerprint, so that an omitted platform stream or process run is detected.

The copy schedule, custody of the removable device and security of the private network destination are operational responsibilities of the competition director. The local clock alone is not trusted timestamp evidence.

## 7. Integrity Checker and Acceptance

The `/admin` route adds a **Check Audit Integrity** action that opens a dedicated checking page. The page requires no private key and checks whatever the user selects: one audit log, or a directory of audit logs, defaulting to the installation's own audit log directory. OWLCMS does not copy, collect or compare logs; choosing the evidence is the user's decision.

The normal dispute procedure is: copy the retained logs from the removable device into the audit log directory of a clean installation of the official OWLCMS version, then run the checker there. The checker may also be run on a competition machine that is in production. It detects any alteration of sealed content there, but a fingerprint that matches the machine's own key is not evidence that the key was never substituted; that is established only by the delegate's visual comparison against the fingerprint photographed in advance.

No key or fingerprint is entered. The public key is read from each section's `audit.open`. The checker prints the fingerprint used for each covered range. For the disputed period, the technical delegate visually compares the reported fingerprint with the fingerprint photographed before that period began.

Checking runs as a bounded background task so file reading, hashing and signature verification never block the Vaadin UI thread. The page uses `ui.access(...)` only to display progress and the completed report. The selected evidence is read-only: checking never modifies, repairs or re-signs a log.

The checker must:

1. Split files into segments at every `audit.open`, then group segments by process run and follow `CONTINUE` links across size-rolled files.
2. Read the embedded public key from each section and compute its fingerprint. The checker knows the built-in fingerprint (§3.0). Sections sealed with the built-in key receive every check below, exactly as for a real key, but are reported as unprotected.
3. Verify every marker signature, every `previousBlockSha256` link, adjacent non-overlapping record ranges, and each `CONTINUE` against the named previous file's valid `FINAL` and matching `lastBlockSha256`.
4. Detect changed, missing, duplicated, overlapping or reordered records and batches.
5. Report records not covered by a valid seal (for example after a crash) as not validated, together with incomplete seals, missing streams and non-finalized runs. These conditions make coverage partial; they are not by themselves evidence that a sealed record was altered.
6. Verify the competition inventory when one is supplied.
7. Report results by file and run, showing run ID, fingerprint, covered and uncovered record ranges, signature status, coverage and key protection.
8. End with a summary listing each distinct fingerprint found, prominently and in full, with its covered time and record ranges. For the disputed period, the technical delegate compares the reported fingerprint with the fingerprint photographed before that period began. If several fingerprints cover the disputed period, each must be compared.

The report has three independent result dimensions:

| Dimension | Value | Meaning |
|---|---|---|
| Integrity | `INTACT` | Every sealed block, signature and block link passed. |
| Integrity | `FAILED` | A claimed sealed block, signature or link failed. |
| Integrity | `UNSEALED` | The input is an ordinary audit log produced without integrity mode; no cryptographic integrity claim can be made. |
| Coverage | `COMPLETE` | Every expected stream and ordinary record is covered by valid markers, and every run segment expected to be finalized has `FINAL`. |
| Coverage | `PARTIAL` | Some records are not sealed, a run is not finalized, or an expected file or stream is missing. The report lists the exact uncovered ranges and reasons. |
| Coverage | `NOT_ASSESSED` | Coverage cannot be assessed cryptographically because the input is unsealed. |
| Key protection | `CONFIGURED` | Every checked range is signed with a configured key. |
| Key protection | `BUILTIN` | Every checked range is signed with the built-in key and therefore has no key protection. |
| Key protection | `MIXED` | The checked evidence contains both configured-key and built-in-key ranges. |
| Key protection | `NONE` | The input is unsealed and no signing key was used. |

Examples:

```text
Integrity: INTACT
Coverage: PARTIAL
Key protection: CONFIGURED

Validated:     A_full_...log, run abc, records 1–1840
Not validated: A_full_...log, run abc, records 1841–1847
Reason: records follow the last valid SEAL; FINAL is absent
```

```text
Integrity: FAILED
Coverage: PARTIAL
Key protection: CONFIGURED

Failure: block covering records 1792–1840 does not match its signed SHA-256
```

The checker never reports a log as verified by itself. `Integrity: INTACT` means the sealed evidence is internally consistent; for the disputed period, the technical delegate completes verification by visually comparing the reported fingerprint with the fingerprint photographed before that period began. `Key protection: BUILTIN` or `MIXED` is always called out prominently.

The checking page exposes these dimensions as structured result values rather than process exit statuses. Automated tests assert those values directly. Unaltered built-in-key fixtures normally produce `INTACT / COMPLETE / BUILTIN`; an unaltered crash fixture with an unsealed tail produces `INTACT / PARTIAL / BUILTIN`; an ordinary audit log produced with `iwfCompliance` off produces `UNSEALED / NOT_ASSESSED / NONE`.

Acceptance tests cover byte alteration, deletion, reordering, truncation, removal of a complete final batch, omission of a stream, public-key replacement, restart, crash during publication, disk full, key loss and unavailable independent storage.

Benchmark representative concurrent platform traffic before selecting final batch limits. Exercise unchanged-copy verification, rollover and crash recovery on Windows, macOS and Linux.

## 8. Preparing for the Competition

### 8.1 Preparation Page

The `/admin` route adds a **Prepare Audit Integrity** action that opens a dedicated preparation page. It is restricted by the existing `/admin` access rules (`ADMIN_PAGES` role and localhost or backdoor access).

The preparation and checking pages share the same fingerprint computation and display format. Key generation uses only the JDK (Ed25519 is supported since Java 15) and needs no OpenSSL installation.

If `OWLCMS_AUDIT_SIGNING_PEM_BASE64` is present, the preparation page reports that the environment key has precedence and does not create `auditkey.pem.new`; replacing a cloud or `.env` key is done in that environment setting.

Otherwise, the preparation page may create `auditkey.pem.new` even while integrity mode is currently off. The pending key forces effective `iwfCompliance` on at the restart that promotes it.

1. Generates an Ed25519 key pair.
2. Writes `auditkey.pem.new` in the same directory as the configured `auditkey.pem`, containing the `PRIVATE KEY` and `PUBLIC KEY` blocks (§3.1), with owner-only permissions on macOS and Linux.
3. Flushes and rereads `auditkey.pem.new`, parses both keys, verifies that they form a pair, and recomputes the displayed fingerprint from the reread public key.
4. Prints the fingerprint in full and in groups of four hexadecimal digits for visual comparison, for example `d8a3 1872 5d89 bf63 …`.
5. Displays a confirmation that preparation succeeded and that OWLCMS will restart to activate the pending key.
6. Triggers the repository-standard OWLCMS restart. The restart is not triggered after a write or validation failure.

Preparation never modifies the active `auditkey.pem`. A failure leaves the existing key untouched.

At startup, before audit logging is initialized:

1. If `OWLCMS_AUDIT_SIGNING_PEM_BASE64` is present, decode and validate it, load that key and leave both PEM files untouched.
2. Otherwise, if `auditkey.pem.new` is absent, load and validate `auditkey.pem` normally.
3. If `auditkey.pem.new` is present, validate it again.
4. Delete the old `auditkey.pem` if it exists.
5. Rename `auditkey.pem.new` to `auditkey.pem` in the same directory.
6. Reread and validate the resulting `auditkey.pem`.
7. Only then initialize audit logging and write `audit.open` with the newly active fingerprint.

If validation, deletion or rename fails, startup stops with a visible error. It must not use the pending key directly, fall back to the built-in key or silently continue with the old PEM.

No audit record has been written during promotion. If the process stops after deleting the old PEM but before renaming the new one, the next startup finds the complete `auditkey.pem.new`, validates it and repeats the delete-and-rename procedure. The old PEM is deleted only after the pending PEM has been fully written and validated.

Key generation and file I/O run in a background worker, not on the Vaadin UI thread. The page captures the `UI` before starting the worker and uses a short `ui.access(...)` block only to show the result and initiate the restart.

### 8.2 About Page

The About page ([InfoNavigationContent.java](../owlcms/src/main/java/app/owlcms/nui/home/InfoNavigationContent.java)) displays audit-integrity status under the version number. When the configured switch is off but key presence forces integrity on, it says so. With effective `iwfCompliance` off it shows `audit integrity — off`. With integrity mode on it displays the fingerprint of the key OWLCMS actually loaded, in groups of four. When the built-in key is in use, it also shows `builtin key — no protection`. Nothing else about the key is shown.

### 8.3 Procedure

1. The competition director opens `/admin`, selects **Prepare Audit Integrity**, and confirms creation of the pending `auditkey.pem.new`. The technical delegate photographs the fingerprint shown on the preparation page.
2. The page restarts OWLCMS. Startup promotes the pending key before audit logging begins. After reconnection, the director opens the About page and the delegate checks that the fingerprint shown there matches. This confirms that OWLCMS activated the key that was just generated, not a stale file or the built-in key.
3. The delegate keeps the photo or sheet away from the competition machine. Its date establishes that it was recorded before the competition.
4. If the key is ever replaced, the procedure is repeated and the new fingerprint is recorded the same way.
5. In a dispute, an administrator copies the retained logs from the removable device into the audit log directory of a clean OWLCMS installation, opens **Check Audit Integrity** from `/admin`, and runs the check. For the disputed period, the delegate visually compares the fingerprint reported by the checking page with the fingerprint photographed before that period began. No fingerprint is typed at any stage.

### 8.4 Existing Log When a Key Is Prepared

Preparing a key does not rewrite or re-sign existing audit records. The normal restart closes the current run as far as possible, and the restarted process appends a new `audit.open` and `FIRST` using the newly loaded key.

The same daily log may therefore contain consecutive ranges signed by different keys:

- ranges written before `iwfCompliance` was enabled are unsealed and are reported as `UNSEALED / NOT_ASSESSED / NONE`;
- if integrity mode was already enabled but no PEM existed before preparation, the earlier ranges are signed with the built-in key and are reported as `INTACT / COMPLETE / BUILTIN` when otherwise complete;
- if an older PEM existed, the earlier ranges are signed with that key and are reported with its observed fingerprint;
- ranges after the restart are signed with the newly prepared key and are reported with its observed fingerprint;
- records not covered by a valid seal, for example after an abrupt stop, are reported separately as not validated.

The checking page lists each observed fingerprint together with its run ID, file and covered time and record ranges. It does not label a fingerprint as expected. For the disputed period, the technical delegate visually compares the reported fingerprint with the fingerprint photographed before that period began.
