# Audit Integrity Implementation Plan

This plan implements:

- [AUDIT_TRAIL_SPECIFICATION.md](AUDIT_TRAIL_SPECIFICATION.md)
- [AUDIT_INTEGRITY_SPECIFICATION.md](AUDIT_INTEGRITY_SPECIFICATION.md)

Each checkpoint ends with reviewable behavior and is a candidate commit boundary. Do not combine checkpoints into one large change. Obtain authorization before commits or broad Maven runs.

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

- target `_full.log` file size;
- maximum block byte count;
- maximum block record count;
- maximum block age during continuous activity;
- maximum pending signing-queue bytes.

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
- compare local and retained copies;
- report exact validated/unvalidated ranges;
- list fingerprints by covered time/range;
- support independent single-file checking with a predecessor-not-checked warning.

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

- Select/upload retained logs and optional local copies.
- Run checking in a bounded background task.
- Display the three result dimensions.
- Display failed/unvalidated ranges.
- Display full grouped fingerprints.
- Never modify or repair evidence.
- Document that disputes are checked on a separate machine with a clean OWLCMS installation.

JSON upload/checksum calculation remains a future enhancement.

### Validation

- Workspace Java diagnostics.
- Browser review.
- Background operation does not block the UI.
- No private key or fingerprint input.

### Checkpoint result

Check retained fixtures from a clean installation and perform visual fingerprint comparison.

## Checkpoint 9 — Remove H2 TCP Service

Independent global change:

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
