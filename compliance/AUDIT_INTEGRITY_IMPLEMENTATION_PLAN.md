# Audit Integrity Implementation Plan

This plan implements:

- [AUDIT_TRAIL_SPECIFICATION.md](AUDIT_TRAIL_SPECIFICATION.md)
- [AUDIT_INTEGRITY_SPECIFICATION.md](AUDIT_INTEGRITY_SPECIFICATION.md)

Each checkpoint ends with reviewable behavior and is a candidate commit boundary. Do not combine checkpoints into one large change. Obtain authorization before commits or broad Maven runs.

## Checkpoint Progress

- **Checkpoint 0:** specification references checked; whitespace validation passed.
- **Checkpoints 1–2:** switches, startup-frozen key identity, key precedence, path resolution and pending promotion implemented. Focused key/configuration tests pass.
- **Checkpoint 3:** preparation page, checking-page placeholder, About identity/status and forced-switch display implemented. Browser validation is pending: no local OWLCMS instance was listening on port 8080. New translation rows require the external translation import workflow.
- **Numeric limits:** agreed: 10 MiB files, 1 MiB / 500 ordinary records / five-minute maximum age per block, and 8 MiB total frozen data awaiting signing. Five-minute idle sealing remains an independent trigger.
- **Checkpoint 4:** implemented and tested: typed marker schemas, canonical UTF-8 signed encoding, public-key verification, block-hash chaining and strict parsing. OpenSSL independently verifies a Java-signed fixture and rejects an altered fixture.
- **Checkpoint 5 core:** implemented and tested: exact-byte stream capture, ordered asynchronous signing, the 8 MiB shared queue, explicit block boundaries, idle/age triggers, byte/count limits, and failure propagation. Production event hooks are deferred to Checkpoint 6 so they are connected together with the audit-owned file lifecycle rather than attached to Logback's old rollover.
- **Checkpoint 6:** implemented and tested: audit-owned `_full` files with timestamped names, 10 MiB rollover with CONTINUE, restart append with a new FIRST, read-back verification, idle timer, shutdown FINAL, and degraded failure reporting. Production boundaries are wired: weigh-in/registration batch seals, a seal after each jury decision, and a seal once a lift result is saved (deferred until the enclosing event's audit line is written). About shows active or failed sealing. The lift end-to-end test reuses `TwoMinutesRuleTest` sequence 3 and checks that every `referee.decision` line is followed immediately by a SEAL.
- **Checkpoint 6 review:** production rollover, restart, orderly shutdown, abrupt-tail preservation and save boundaries agree with the specification. The step 7 fixtures now also cover restart after rollover and restart after a partially published marker.
- **Checkpoint 7:** implemented and tested as a read-only, non-UI checker. It returns structured integrity, coverage and key-protection dimensions; validates exact bytes, marker signatures, block/range/predecessor links and cross-file continuations; reports validated and unvalidated ranges plus fingerprint coverage; supports inventory expectations and independent-file warnings. It checks only what the user selects; it does not copy or compare logs.
- **Checkpoints 8 and 10:** not started. Checkpoint 9 (H2 TCP) is tracked outside this plan.
- **Audit Log Snapshot:** implemented (see the section at the end). Forced rollover is tested at the file-store level, including that the resulting zip contents check `COMPLETE`.
- **Validation:** audit suite (76 tests, including 15 checker tests), configuration, route access, JSON export/import and the FieldOfPlay regressions (`TwoMinutesRuleTest`, `ClockStartRestartTest`, `RecordsTest`) pass. Java Problems diagnostics are clean. Browser verification still requires a running instance.
- No implementation changes have been committed.

## Checkpoint 0 — Freeze the Specifications

### Work

- Verify the renamed specifications contain no proposal/history language.
- Verify no references to `AUDIT_TRAIL_DESIGN.md` or `AUDIT_INTEGRITY_PROPOSAL.md` remain.
- Confirm:
  - `tcrrCompliance` and `iwfCompliance` are independent;
  - real-key presence forces effective `iwfCompliance` on;
  - the built-in key does not itself force integrity mode on;
  - H2 TCP removal is global.

### Validation

- Markdown links resolve.
- `git diff --check` passes.

### Exit criterion

Specifications are stable enough to serve as acceptance criteria.

## Checkpoint 1 — Compliance Switches and Effective Integrity Mode

### Work

Add:

```java
TCRR_COMPLIANCE("tcrrCompliance", SPECIALTY_FEATURES)
IWF_COMPLIANCE("iwfCompliance", SPECIALTY_FEATURES)
```

Create a non-UI startup service that determines:

```text
effectiveIwfCompliance =
    configured iwfCompliance
    OR real-key configuration present
```

Real-key configuration includes:

- `OWLCMS_AUDIT_SIGNING_PEM_BASE64`;
- configured `OWLCMS_AUDIT_KEY_FILE`;
- `auditkey.pem.new`;
- `auditkey.pem`.

Rules:

- A real key forces integrity mode on.
- Integrity cannot be turned off until key configuration is removed and OWLCMS restarts.
- Explicit `iwfCompliance=true` with no real key uses the built-in key.
- `iwfCompliance=false` with no key produces ordinary unsealed audit logs.
- `tcrrCompliance` remains independent.
- Effective mode is fixed for the process lifetime.

### Tests

| Configured toggle | Key source | Expected mode |
|---|---|---|
| off | none | unsealed |
| on | none | sealed, built-in |
| off | PEM | sealed, configured |
| off | PEM.new | sealed, configured after promotion |
| off | flattened PEM | sealed, configured |
| either | invalid key | startup error |

Do not create Vaadin objects in tests.

### Checkpoint result

Demonstrate that key presence forces effective integrity mode and TCRR remains independent.

## Checkpoint 2 — Key Loading, Promotion and Fingerprinting

### Work

Implement a non-UI key service responsible for:

1. Reading `OWLCMS_AUDIT_SIGNING_PEM_BASE64`.
2. Resolving `OWLCMS_AUDIT_KEY_FILE`, including:
   - `~`;
   - `~/...`;
   - `~\...`;
   - normalized absolute paths;
   - rejection of home traversal.
3. Defaulting to:
   ```text
   ~/.owlcms/auditkey.pem
   ```
4. Detecting:
   ```text
   auditkey.pem.new
   auditkey.pem
   ```
5. Applying startup precedence:
   - flattened PEM;
   - `.pem.new`;
   - `.pem`;
   - built-in key if integrity was explicitly enabled.
6. Validating both PEM blocks and proving they form a pair by signing and verifying test bytes.
7. Computing SHA-256 of the public SubjectPublicKeyInfo DER.
8. Formatting the fingerprint in groups of four.

### Pending-key promotion

Before audit logging:

1. Validate `.pem.new`.
2. Delete the old `.pem`.
3. Rename `.pem.new` to `.pem`.
4. Reread and validate `.pem`.
5. Initialize audit logging.

If the process stops after deletion and before rename, the next startup finds and promotes the validated `.new` file.

### Tests

- Valid configured and built-in PEM.
- Flattened PEM round trip.
- LF and CRLF PEM.
- Missing, malformed, incomplete and mismatched keys.
- Home expansion and Windows-style paths.
- Home traversal rejection.
- `.new` promotion and interrupted promotion recovery.
- Environment key leaves files untouched.
- Fingerprint matches OpenSSL.

### Checkpoint result

Show Java and OpenSSL producing the same fingerprint and demonstrate recoverable pending-key promotion.

## Checkpoint 3 — Admin Preparation and About-Page Status

### Work

Add actions to `AdminView`:

- **Prepare Audit Integrity**
- **Check Audit Integrity**

The preparation page:

- runs generation and file I/O in a background worker;
- writes and validates `auditkey.pem.new`;
- displays the fingerprint;
- triggers the standard restart;
- refuses file preparation while the flattened environment key is active.

Update `InfoNavigationContent` to display:

```text
audit integrity — off
```

or the loaded fingerprint, with:

```text
builtin key — no protection
```

when applicable.

When key presence forces configured `iwfCompliance=false` to effective true, display that fact.

### Concurrency

- Capture `UI` on the UI thread.
- Generate/write in a background worker.
- Use short `ui.access(...)` calls only to show results and initiate restart.
- Do not nest `ui.access(...)`.

### Validation

- Java Problems diagnostics clean.
- Browser review of Admin and About pages.
- No Vaadin objects in unit tests.
- Translation keys supplied through an approved TSV file.

### Checkpoint result

Prepare a development key, restart, and verify that the About-page fingerprint matches.

## Decision Checkpoint — Numeric Limits

Before sealing implementation, decide:

- target `_full.log` file size: 10 MiB (10,485,760 bytes), agreed;
- maximum block byte count: 1 MiB (1,048,576 bytes), agreed;
- maximum block record count: 500, agreed;
- maximum block age during continuous activity: five minutes, agreed;
- maximum pending signing-queue bytes: 8 MiB (8,388,608 bytes), agreed.

The five-minute idle boundary is already specified. Keep limits in one place and make them overridable in tests.

## Checkpoint 4 — Marker Schemas and Cryptographic Primitives

### Work

Implement immutable marker types:

- `FIRST`
- `CONTINUE`
- `SEAL`
- `FINAL`

Implement:

- exact byte counting;
- SHA-256 including record line terminators;
- RFC 8785-compatible canonical marker payloads;
- versioned audit-seal purpose prefix;
- Ed25519 signing and verification;
- block-hash chaining;
- Base64 signature encoding.

Rules:

- `FIRST` has no predecessor.
- `CONTINUE` links previous filename plus last block SHA-256.
- `SEAL` links the prior block SHA-256.
- `FINAL` repeats the last block SHA-256 and segment record count.
- Marker lines do not consume audit sequence numbers.

### Tests

- Built-in-key vectors.
- Signature verification.
- One-byte record alteration.
- Changed range/hash/filename/run ID.
- Deleted, inserted, duplicated and reordered blocks.
- Wrong public key.
- CRLF/LF sensitivity.
- Canonical serialization determinism.

### Checkpoint result

Generate and verify a small sealed fixture.

## Checkpoint 5 — Sealed Audit Stream Writer

### Work

Separate:

- readable log: current human-readable behavior;
- authoritative `_full.log`: integrity writer when effective `iwfCompliance` is on.

The integrity writer owns:

- exact bytes;
- sequence reset at every `audit.open`;
- `FIRST`, `CONTINUE`, `SEAL`, `FINAL`;
- per-stream active blocks;
- ordered asynchronous signing;
- queue bounds and explicit degraded-state reporting.

When integrity mode is off, preserve ordinary unsealed audit logging without markers or key loading.

### Natural boundaries

Seal after:

- `referee.decision`;
- `jury.decision`;
- the final record of one athlete save from `WEIGHIN` or `REGISTRATION`;
- five minutes with unsealed records.

No-op when no records are unsealed. Add an explicit `AuditLog.closeBlock(stream, reason)`-style API rather than inferring boundaries from rendered text.

### Tests

- One lift per block.
- Jury decision after referee decision.
- Multi-field weigh-in/registration save produces one block.
- Idle timer seals pending records.
- Idle timer does nothing after a seal.
- Platform streams remain independent.
- Backpressure never silently discards data.

### Checkpoint result

Run a small competition flow and inspect readable/full logs block by block.

## Checkpoint 6 — Size Rollover, Restart, Shutdown and Crash Handling

### Work

Replace Logback time rollover for authoritative `_full` logs with audit-owned size rollover.

At rollover:

1. Seal the current block.
2. Write `FINAL`.
3. Flush and close.
4. Open an ISO-datetime filename with create-new semantics.
5. Reset sequence to 1.
6. Write `audit.open`.
7. Write `CONTINUE`.
8. Resume records.

At restart:

- append `audit.open`/`FIRST` to the latest under-size file;
- use a new run ID;
- reset sequence to 1;
- do not chain across process runs.

At orderly shutdown:

- audit component's own hook seals pending records and writes `FINAL`.

After abrupt termination:

- preserve the file unchanged;
- append a new run on restart;
- report the unsealed tail as not validated.

### Tests

Use small test limits:

- multiple size rollovers;
- ISO filename ordering;
- restart in the same file;
- restart after rollover;
- crash after ordinary record;
- crash during marker publication;
- missing `FINAL`;
- independent single-file verification;
- complete cross-file verification.

### Checkpoint result

Produce rolled files, restart, and verify independent-file and complete-chain behavior.

## Checkpoint 7 — Integrity Checker Core

### Work

Implement a read-only, non-UI checker reporting:

```text
Integrity: INTACT | FAILED | UNSEALED
Coverage: COMPLETE | PARTIAL | NOT_ASSESSED
Key protection: CONFIGURED | BUILTIN | MIXED | NONE
```

It must:

- split segments at `audit.open`;
- verify marker signatures;
- recompute every block hash;
- verify predecessor links and ranges;
- verify `CONTINUE` against the named previous file;
- support independent single-file checking with a predecessor-not-checked warning.

The checker examines only the evidence the user selects; it never copies or compares logs.

### Tests

- Byte edits.
- Line insertion/deletion.
- Block deletion/reordering.
- Marker replacement.
- Key replacement.
- Missing stream/file.
- Non-finalized run.
- Built-in/configured/mixed keys.
- Ordinary unsealed logs.

### Checkpoint result

Expected fixtures:

```text
INTACT / COMPLETE / BUILTIN
INTACT / PARTIAL / BUILTIN
FAILED / PARTIAL / ...
UNSEALED / NOT_ASSESSED / NONE
```

## Checkpoint 8 — Admin Checking Page

### Work

Implement the `/admin` checking page over the non-UI checker.

- Select the logs or directory to check; default to the installation's own audit log directory, where the retained copies are placed by hand.
- Run checking in a bounded background task.
- Display the three result dimensions.
- Display failed/unvalidated ranges.
- Display full grouped fingerprints.
- Never modify or repair evidence.
- Document the normal procedure (copies placed in a clean installation's audit directory) and that running on a production machine detects content changes but says nothing about key substitution.

JSON upload/checksum calculation remains a future enhancement.

### Validation

- Workspace Java diagnostics.
- Browser review.
- Background operation does not block the UI.
- No private key or fingerprint input.

### Checkpoint result

Check retained fixtures from a clean installation and perform visual fingerprint comparison.

## Checkpoint 9 — Remove H2 TCP Service (out of scope for this plan)

Independent global change, tracked separately; it does not depend on audit integrity and is not part of this delivery:

- remove H2 TCP startup;
- remove `H2ServerPort` / `OWLCMS_H2SERVERPORT`;
- reject TCP/SSL H2 URLs and unsafe options;
- preserve embedded H2 and Hikari;
- update configuration documentation;
- add focused URL-validation tests.

### Checkpoint result

Verify embedded H2 startup and rejection of external H2 configuration.

## Checkpoint 10 — Final Conformance Pass

- Run focused audit, export, key, sealing, checker, restart and H2 tests.
- Verify Java Problems diagnostics are clean.
- Run broader suites only after agreement.
- Review Admin and About pages.
- Verify translation TSV completeness.
- Verify OpenSSL and Java export hashes match.
- Verify documentation links and examples.
- Remove temporary artifacts.
- Review diffs for unrelated changes.
- Commit in checkpoint-sized commits only after authorization.

## Audit Log Snapshot (former backup TODO)

Implemented as `/admin/audit/snapshot` over the non-UI `AuditSnapshot`: optional database export (sealed as `export.json`, `channel=audit`), an `audit.snapshot` line, forced rollover of every sealed stream (`AuditLog.rollover()`), and a zip of the whole `logs` tree (application logs, readable and sealed audit logs), optionally with the `local` folder. The snapshot is a diagnostic backup as well as evidence, so nothing is filtered out except the sealed files just opened by the roll, which hold nothing yet. In plain (unsealed) mode nothing is rolled. Browser review is pending.
