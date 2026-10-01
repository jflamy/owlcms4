# Audit Log and JSON V2 Export Integrity

Status: proposal only, 2026-10-01. No implementation changes are authorized by this document.

Related: [AUDIT_TRAIL_DESIGN.md](AUDIT_TRAIL_DESIGN.md).

## 1. Recommendation

Use **logger-owned batches of original bytes with asynchronous hashing and signatures**, not a hash chain calculated on every log entry. Rollover-only sealing is insufficient when an active file might be edited.

- Keep the existing full audit logs readable and preserve their normal logging path.
- Retain the exact encoded bytes emitted by the full-log appender in a bounded batch of up to N records, with additional byte-size and age limits.
- At a batch boundary, freeze the original bytes and hand ownership of that batch and its offset metadata to a background worker; continue writing into a new batch.
- Compute SHA-256 from the frozen logger buffer, not by rereading the writable file. A running write-time digest remains a lower-memory alternative to benchmark.
- Sign each buffered range X through Y asynchronously with a dedicated Ed25519 key, then append a `SEAL` line to the same full log. It need not appear immediately after Y; no separate batch-manifest file is required.
- At rollover or finalization, verify the stored bytes against the original commitments, rather than sign whatever bytes happen to be present then.
- Identify the signer by a public-key fingerprint, registered independently before the competition.
- For JSON V2 exports, compute SHA-256 over the exact emitted bytes and include it in the audited export event. The audit seal authenticates that reference; no separate JSON signature is required.
- For stronger protection, send signed checkpoints to an independently controlled archive or witness service.

This avoids per-entry hashing, signatures, hash-chain records and synchronous network calls on the logging path. It does require capturing each emitted byte into the batch: avoiding all write-time capture would leave the pre-seal tampering gap open. The cost is a buffer append or safe ownership transfer, bounded memory and queue management. The worker hashes each completed batch once. Actual overhead must be measured rather than assumed negligible.

The correct claim is **tamper-evident**, not absolutely tamper-proof. A compromised machine that controls both the application and its usable signing key can manufacture new signed evidence. Independent custody or remote key control strengthens the guarantee.

## 2. Existing Implementation

- [AuditLog.java](../owlcms/src/main/java/app/owlcms/audit/AuditLog.java) uses **SLF4J with Logback**, including a sifting appender and daily rolling file appenders. It does not currently use Log4j.
- Each platform has a readable log and a `_full` log. Competition-wide events have their own stream. The full logs are the authoritative files proposed for sealing.
- Sequence numbers are per platform and per process; a random run ID is recorded by `audit.open`. Appends can span several runs in one daily file.
- Writes are synchronized, but sequence state is in memory. There is no persistent integrity checkpoint or signature.
- [CompetitionDataV2.java](../owlcms/src/main/java/app/owlcms/data/export/v2/CompetitionDataV2.java) creates JSON through a background writer and a pipe. Its database collection is not shown as one consistent transaction across all repositories.
- [InstallationSecret.java](../shared/src/main/java/app/owlcms/utils/InstallationSecret.java) decrypts the control panel's `enc:v1:` values using AES-256-GCM and an installation key stored outside the installation folder.

No secret values from the local environment files were inspected for this proposal.

## 3. Protection Levels

| Approach | What it establishes | Main limitation |
| --- | --- | --- |
| SHA-256 checksum file | Bytes match a known digest | An attacker can replace both the file and digest unless the digest is held independently |
| HMAC over blocks | Integrity for parties holding a shared secret | Anyone who can verify with that secret can also forge; unsuitable for public verification |
| Digital signature over block/file manifests | Integrity and attribution to a particular signing key | Requires a trusted public key; local key compromise permits forgery |
| Signed manifests plus independent checkpoints | Earlier committed content cannot be silently replaced or rolled back past a retained checkpoint | Uncommitted intervals remain exposed; witness policy and availability matter |
| Independent archive with retention lock | Preserves accepted bytes and manifests against later local deletion | Does not establish that the original application recorded every action truthfully |

Recommended first stage: capture original logger bytes in bounded batches, hash/sign those batches asynchronously, and record JSON export checksums in that sealed audit trail. File-only sealing remains a lower-assurance archival option, not protection against edits made before sealing. Independent checkpoint retention is needed to detect a complete rollback reliably.

## 4. Performance-Conscious Sealing Options

### Option A: Closed File Sealing

After rollover closes a full audit file, enqueue its identity for a background sealer. The sealer reads it once, records its byte length and SHA-256 digest, and signs a manifest. Also close/seal segments at session or competition finalization so protection does not depend on midnight rollover.

Advantages: simplest implementation, no per-line changes, no cryptographic cost on the logging thread. Limitation: the active file is unprotected until it is sealed; edits made before sealing become part of the accepted content.

This does not satisfy protection against pre-rollover tampering. It establishes only that the file has not changed since sealing, not that it contains the original logger output. Even a closed file can be edited before the background worker reads it.

### Option B: Periodic Byte-Range Sealing

At a configurable time or size threshold, determine a stable, flushed prefix of each full audit file, ending at a complete record boundary. Capture the file generation and end offset, then let a background worker hash only the newly covered byte range. The logging thread continues appending beyond that offset.

Each signed block manifest includes the previous manifest's digest, stream identity and start/end offsets. Thus the chain is **per block**, not per line. A single signature covers many entries, and each byte normally needs hashing only once.

A file can therefore have several sealed blocks and an unsealed tail. The verifier must report the tail explicitly. A final manifest closes the tail and declares the expected final length.

Advantages: shorter exposure intervals than daily sealing and low amortized cost. Limitation: edits before the worker reads a range can still be accepted as genuine. Coordinating flush, rollover and file identity is also more complex than sealing closed files. The worker must not treat a pathname as proof that it still refers to the same file generation. This is not sufficient for the stated pre-seal tampering threat.

### Option C: Incremental Digest in an Appender

A custom appender or output-stream wrapper can feed encoded bytes to a digest as they are written, then sign only at block boundaries. There is no per-line signature, but every write still performs digest work on the logging path.

This is a lower-memory alternative for detecting edits before rollover. The digest represents the original emitted bytes, not a later reading of a writable file. An edit made during the active block will therefore disagree with its eventual signed commitment, provided the attacker cannot alter the logger's in-memory digest or signing worker.

Perform capture in the appender/output path, serialized with the actual file writes, not in a later file watcher. If the framework uses an asynchronous appender, capture belongs in its ordered consumer that encodes/writes the full log. This may move the work away from caller threads, but queue saturation and failure semantics must still be measured and specified.

At a complete-record boundary, finalize the current block digest and atomically switch to a new digest. Enqueue only the completed digest, byte offsets and identity metadata for signing; never hand a mutable live digest to another thread. Avoid an extra copy of every record. A block checkpoint is the commitment to the concatenated original bytes, not a chain operation per line.

Track write and flush failures explicitly: digesting intended output is not proof that the file stored it successfully. Before declaring a segment complete, read it back and compare its ranges with the original signed digests. Any mismatch or known write failure is an integrity failure, not a reason to regenerate signatures from the altered file.

An unfinished block held only in memory remains vulnerable to process termination or machine compromise. Shorter blocks reduce this window; durable signed checkpoints and independent receipts protect completed history. Recover an unsigned crash tail as unverified evidence, never by silently hashing and certifying it as original logger output.

### Option D: Logger-Owned N-Record Buffers

This is the preferred candidate when hashing should not run on the logging path. The logger continues writing normally but retains the exact same encoded bytes in a bounded batch. Once the batch is closed, a background worker hashes and signs the retained bytes. Editing the file before that worker runs does not change the retained original content.

The sequence is:

1. Encode each full audit record once, including its actual line separator. Under the appender's write ordering, append those bytes to the file and capture them in the active batch. Track write failures explicitly.
2. Close the batch on N records, the byte-size limit, the age limit, rollover, shutdown or finalization, whichever occurs first. Attach stream/generation identity, block number and inclusive audit-line ordinals X through Y. Track the required write/flush completion separately from capture.
3. Freeze the batch and transfer ownership to the worker. Switch the logger to a fresh buffer; never overwrite or reuse a queued buffer while a worker still owns it.
4. The worker hashes the original batch bytes and signs a seal linked to the preceding seal. Return it to the owning appender to append as a `SEAL` line, even if newer audit records have already been written. Complete the required durable publication before releasing or recycling the batch buffer.
5. Verify the covered audit-line ranges against those commitments. A mismatch is an integrity failure, not an invitation to sign the current disk content instead.

Use non-overlapping batches rather than repeatedly hashing a sliding window of the last N records. Each original byte is hashed once for its block. Final readback verification is a separate check.

For benchmarking, try a policy such as 1,000 records, 1 MiB or 5 seconds, whichever comes first. These values are candidates only. A byte limit is essential because line lengths vary; an oversized individual record needs an explicitly bounded handling policy, not truncation or an unlimited allocation. A timer must close partial batches even when no new records arrive, synchronized with the appender's write and rollover ordering.

Memory comprises active buffers for all live streams plus all queued/in-flight batches, not just N lines in one logger. Bound total queued bytes as well as batch count. Under normal load, buffer swapping lets logging continue while hashing/signing happen elsewhere. If the worker falls behind, use defined backpressure or report an integrity-degraded state. Never discard or overwrite unsealed batches silently. No bounded-memory design can guarantee both uninterrupted logging and lossless sealing under an indefinitely stalled worker.

This buffer is evidence of original output only while the process and its memory remain trusted. An abrupt crash may lose active or queued unsigned batches; a durable unsigned spool alone does not establish authenticity after restart. Report the corresponding tail as unverified unless an authenticated commitment survived. Independent retention of completed signed checkpoints remains necessary for rollback protection.

### Proposed Scheduling

- Always checkpoint the final partial block at segment close and verify closed segments against their write-time commitments. Seal final artifacts only after these checks succeed.
- For active logs, benchmark record-count, byte-size and age thresholds together, including the candidate policy in Option D. Select them from measured overhead and the acceptable unsealed interval.
- Do not periodically rehash the complete growing file; that would repeatedly scan old data.
- For Option D, queue bounded immutable byte batches; for Option C, queue completed digest descriptors. Preserve pending commitments durably where possible; unsigned local data alone is not trusted evidence after a compromise.
- Do not drop original batches or completed digests and replace them with a later file read under queue pressure. Define backpressure or an explicit integrity-degraded state, and never claim that discarded or unverified data is protected.
- Serialize manifest publication per stream. Hashing/signing can happen in background workers, but original byte capture, offsets and predecessor references must remain ordered.
- Keep signing and all remote communication outside the logging call and outside Vaadin UI access blocks.
- Ordinary batch boundaries do not wait for signing. Before a file generation closes or rolls, drain its pending seals into that generation; define rollover backpressure and crash recovery explicitly.

## 5. Logging Framework and Windows Compatibility

Keep the manifest signer and verifier independent of Logback or Log4j. For the protected mode, the worker's input is a frozen batch of original logger bytes (Option D) or a write-time block digest (Option C), plus stream metadata, not a later reading of the file. A small framework-specific appender/output adapter supplies that input.

For the present Logback implementation, integrate byte capture with the full-log appender's encoding/output path and segment lifecycle. For a Log4j 2 deployment, provide the corresponding adapter. Exact extension points must be checked against the selected framework version before implementation. A directory scanner may discover files needing verification, but cannot reconstruct missing original write-time commitments or establish authenticity from a name or unchanged timestamp.

Do not replace Logback solely to obtain integrity protection. If "Log4j" means the logging layer generally, the current logging layer can remain unchanged. If a specific Log4j deployment is required, confirm whether that means Log4j 2; this proposal does not recommend obsolete Log4j 1.x.

Use portable Java cryptography and file APIs, without shell scripts or external checksum commands:

- Hash exact bytes, not strings decoded and re-encoded by a verifier.
- Store relative artifact names in manifests, not absolute machine paths. Reject unsafe paths during verification.
- Existing line endings may differ between Windows and macOS/Linux. An exact-byte signature intentionally detects newline conversion; copying a file unchanged remains verifiable everywhere.
- Prefer explicitly configured UTF-8 for new sealed artifacts. Do not retrofit newline or encoding changes onto existing files as part of verification.
- On Windows, close writers and readers as required before rename, replacement or deletion. Do not assume Unix open-file rename semantics.
- Append log seals through the same serialized writer as ordinary records. A crash can leave an incomplete seal line, which must be reported rather than accepted.
- Publish detached export manifests through a temporary file and an atomic move where supported. Define recovery for incomplete publication where atomic moves are unavailable.
- Retain pending source segments until sealing succeeds; test rotation, restart and recovery on Windows as well as macOS/Linux.

## 6. Signed Manifest and Fingerprint

### Inline Seals with Asynchronous Placement

Each buffered batch produces one signed `SEAL` line in the full audit log. Its explicit range identifies the covered records, independently of where the seal is appended. For example, a worker can finish sealing records 1 through 1,000 after the logger has already written record 1,042:

```text
<audit line 1>
...
<audit line 1000>
<audit line 1001>
...
<audit line 1042>
SEAL {"version":1,"firstLine":1,"lastLine":1000,"sha256":"...","previousSeal":"GENESIS","keyFingerprint":"...","signature":"..."}
<audit line 1043>
...
```

This is an illustrative layout with abbreviated cryptographic values, not a complete valid seal. The signature also covers stream/generation identity and the other metadata listed below.

X and Y are **audit-line ordinals within the file generation**, excluding `SEAL` control lines. They are neither physical editor line numbers after seals have been inserted nor existing per-process event sequence numbers, which reset on restart. Ordinary audit records must occupy one physical line, with embedded newlines escaped. The next batch covers Y+1 onward, regardless of interleaved seal lines.

Hash the exact concatenation of the buffered audit lines, including their original line terminators. Exclude seal lines from both that hash and the N-record buffer. A seal authenticates its own metadata through its signature, not by hashing itself; the next seal links to a digest of the previous canonical signed envelope. This is a chain of batches, not a chain operation per audit line.

Append a completed seal through the owning appender's serialized writer, bypassing ordinary audit-record capture. Do not log it through the usual audit path, where it could enter the buffer, duplicate into other streams or trigger recursive sealing. Reserve an unambiguous control-line prefix and reject ordinary records that could impersonate it.

**No synchronization with line Y is required.** Freeze the buffer at Y, continue normal logging, and append the seal when ready. Only the brief buffer swap and actual append require normal writer coordination. Emit seals in batch order to maintain predecessor links; never seek backward or rewrite existing records to place a seal next to Y.

The verifier reads the entire file and matches each seal to its declared audit-line range, rather than treating the immediately preceding N physical lines as its input. A changed record fails verification even if it was edited before the seal appeared, because the worker hashed the retained original bytes.

At final close, seal the partial batch and drain pending seals, then append a signed final control record declaring the total audit-line count and last seal fingerprint. An unfinished batch or partial seal after a crash remains unverified. Removing a complete final batch and its seal still requires an independently held checkpoint or final inventory to detect reliably.

### Signed Fields and Key Identity

A versioned seal payload should contain:

- Manifest schema version, SHA-256 digest algorithm and Ed25519 signature algorithm.
- Persistent competition audit ID, installation/signer ID, logical stream ID and unique file-generation ID.
- Relative artifact name, inclusive first/last audit-line ordinals, covered byte count and digest of their exact concatenated bytes. Offsets may be supplementary; audit-line ranges are the primary identifier for inline seals.
- Block number and digest of the preceding signed manifest, or an explicit genesis marker.
- Run IDs and sequence boundaries where available; these are supporting metadata, not substitutes for byte coverage.
- UTC sealing time, application version and public-key fingerprint.
- Whether this is an intermediate checkpoint or a final seal declaring the complete file length.

Sign the manifest payload with an explicit purpose/version prefix, so an export signature cannot be mistaken for an audit-block signature. Use a defined serialization such as RFC 8785 canonical JSON for manifest payloads, implemented by a tested library. Keep the signature outside the signed payload and specify whether predecessor digests cover the canonical signed envelope. Do not leave field order, numeric encoding or signature coverage implicit.

The **signing fingerprint** is the full SHA-256 digest of the Ed25519 public key's DER SubjectPublicKeyInfo encoding. A shortened display value may be convenient, but verification and key registration use the full fingerprint.

This is separate from the existing `enc:v1:` installation-key fingerprint, which is only four digest bytes used to identify the local decryption key. That short fingerprint must not become the public trust identifier.

Ship the public key with the evidence for convenience, but trust it only after comparison with an independently registered fingerprint or an approved certificate/registry. A key supplied alongside a signature is not, by itself, proof of the signer's identity.

## 7. Key Storage and Custody

Generate a dedicated audit signing key. The JSON checksum needs no separate key. Do not reuse the installation AES key, API credentials or password hashes as signing material.

The existing control-panel encryption format can protect an encoded signing private key in configuration, including the same mechanism used for encrypted values in `.env.mac`. The application would decrypt it through the existing installation-secret facility. Require encrypted storage in the chosen deployment policy rather than silently accepting an unprotected private key.

Important limitations:

- AES-GCM encryption protects the saved value, not the running application from a local administrator who can read its decryption key or memory.
- Copying configuration to another computer does not automatically make the encrypted key usable there. Migration needs an explicit re-encryption or key-replacement procedure.
- Keep public verification keys indefinitely. Lost private keys prevent new signatures but need not prevent verification of existing evidence.
- Record key changes and link the old and new identities through authorized registration and, when available, an old-key-signed transition. Never silently start a new trusted identity after restart.
- Record revocation and compromise information. Without independent timestamp/checkpoint evidence, a signature's claimed creation time cannot establish that it predates compromise.

For higher assurance, keep the signing key in a hardware-backed store or remote signing service. Remote signing of every event is unnecessary: signing a batch commitment is sufficient. These remain optional deployment choices, not prerequisites for an offline first stage.

## 8. Final JSON V2 Export

### SHA-256 Anchored in the Export Audit Event

Emitting a JSON V2 export is an audited action. Include its SHA-256 digest in that event, so the existing log seal protects the reference checksum. Keep the V2 JSON schema unchanged; no separate export signature or checksum sidecar is required for this baseline.

The competition-wide full log's `export.jsonv2` event should include:

- Requesting actor/station and event timestamp, using normal audit attribution.
- Export ID, competition audit ID, artifact name and format version.
- Exact serialized byte length and full SHA-256 digest (the export's content fingerprint).
- Generation outcome and, for a final export, its declared snapshot/audit boundary.

Generate JSON once, hashing the same bytes written to the export artifact. After successful serialization, record the completed digest in the export event and provide that exact artifact for download. Do not regenerate JSON separately for checksum calculation: timestamps, iteration order or concurrent changes could produce different bytes. Capture the requesting actor before entering the background exporter.

The existing piped export is an integration point, but a stable completed artifact makes successful generation and its checksum unambiguous. Audit failed generation explicitly rather than emitting a success digest for a partial stream. A completed export made available by the server does not prove the browser received it; recipient checksum verification detects truncation or alteration.

The export event can be sealed asynchronously like any other event. Until its covering seal is published, its digest is recorded but not yet authenticated. Final evidence handover must include the seal covering that event. Verify that seal and compare the supplied JSON's SHA-256 with the recorded digest. Changing both a downloaded JSON and a loose checksum cannot evade a comparison with this authenticated audit reference.

The content fingerprint is distinct from the audit signer's public-key fingerprint. SHA-256 alone is sufficient for comparing bytes with this trusted reference; without the sealed audit event or another independently trusted digest, it cannot prove provenance. Retaining the relevant seal independently also protects against replacing or rolling back the entire evidence set.

### H2 Configuration Assumption

Source inspection of [JPAService.java](../owlcms/src/main/java/app/owlcms/data/jpa/JPAService.java) supports embedded file-mode H2 by default, but app-only access must be checked in the deployed configuration:

- The default file URL adds `DB_CLOSE_DELAY=-1` and `TRACE_LEVEL_FILE=4`, not `AUTO_SERVER`. Normal H2 embedded file locking prevents another process from independently opening the same active database; this is not a security boundary against a privileged administrator.
- The existing `H2ServerPort` / `OWLCMS_H2SERVERPORT` option can enable an H2 TCP server with `-tcpAllowOthers`. The agreed proposal is to remove this option and its server-start code entirely, not retain a development exception.
- A configured `JDBC_DATABASE_URL` can select server-mode H2, PostgreSQL or custom options such as `AUTO_SERVER=TRUE` or altered locking. Check the resolved mode/options without exposing credentials.
- The file-mode defaults are user `sa` and an empty password unless overridden. These do not provide adequate protection if external access is enabled.
- Confirm database-file permissions and absence of unaudited console/maintenance access. Offline modification, application compromise and privileged machine access remain outside the embedded-lock guarantee.

The running instance's effective settings have not been verified. A controlled non-production check should confirm that a second process cannot connect to the active database under the selected settings, on Windows and macOS/Linux.

### Enforce No External H2 Queries, Preserve the Pool

The requirement is **process-local embedded H2 with no server access**, not a single exclusive SQL session. Keep Hibernate/Hikari's pool unchanged. Current settings specify a minimum of 5 idle connections and a maximum of 15; these connections are all within the application process. Do not introduce `SET EXCLUSIVE` or limit the pool to one connection to address external access.

The executable packaging is documented in [BUILDING.md](../BUILDING.md): `owlcms.jar` is an uberjar containing the application and its dependencies. H2 runs embedded within that application's JVM; bundling the H2 dependency does not require launching an H2 database server. Removing listener startup does not remove the embedded JDBC driver or change the connection pool. JSON export and application search remain the supported inspection workflow.

The repository uses H2 2.1.214. In [JPAService.java](../owlcms/src/main/java/app/owlcms/data/jpa/JPAService.java), `processSettings()` can call `startH2EmbeddedServer()` before the entity-manager factory and connection pool are created. The proposed enforcement point is therefore configuration resolution, **before any listener or pool is started**:

1. Resolve the selected database mode and effective settings without opening connections. For an H2 deployment covered by this policy, allow embedded `jdbc:h2:file:` or named `jdbc:h2:mem:` URLs only. Reject remote H2 URLs, including TCP and SSL server modes, rather than connect and try to establish exclusivity afterward.
2. Remove `startH2EmbeddedServer()`, all calls to it, the now-unused `embeddedH2Server` startup flag and the `org.h2.tools.Server` import. Remove the option's advertised system-property/environment support and its row in [docs/100-AdvancedTopics/140-Configuration.md](../docs/100-AdvancedTopics/140-Configuration.md). Do not keep an alternate debug switch that restores server access. If stale settings are detected for migration diagnostics, they must never start a listener.
3. Reject `AUTO_SERVER=TRUE`, which enables automatic mixed embedded/server operation. Explicit `AUTO_SERVER=FALSE` or its absence is compatible with the policy. Reject contradictory/duplicate options and unsupported values rather than rely on URL option ordering.
4. Inspect custom URL initialization options such as `INIT`, which can execute SQL during connection creation. Reject these unless explicitly reviewed; the no-server claim must not be undermined by configuration-driven SQL or Java aliases. Prefer a small documented allowlist of supported connection options, implemented with validated parsing rather than substring checks.
5. Once validated, create the normal embedded pool. There is no application-managed H2 server-start path or development exception. Preserve the embedded H2 dependency and existing pooled-connection settings.

Also check that the application has no alternate H2 TCP, PostgreSQL-protocol or web-console startup path, and that no management endpoint exposes arbitrary SQL. The inspected database startup path currently contains the optional TCP server targeted for removal; this is not yet a complete runtime listener inventory. A loopback-only listener still permits queries from other local programs, so binding to localhost is not an adequate substitute for removing the server-start capability.

With embedded file mode, automatic mixed mode disabled and normal file locking retained, a second process cannot independently open the same active database through supported H2 access. Reject disabled locking options such as `FILE_LOCK=NO` if custom URLs are permitted. Do not manually remove lock files or implement a separate lock-file existence check; let H2 acquire and enforce its own file lock. If another process already owns the database, startup must fail visibly rather than fall back to a different database or mode.

The guarantee is limited to external database access. Code running in the same JVM can still create additional embedded connections if it has access to the database; privileged host access and application compromise remain outside this boundary. Preventing those requires application/OS controls, not a change to the connection pool.

Proposed focused acceptance checks, without changing the pool:

- Default embedded settings initialize and concurrent pooled transactions work normally.
- The H2 server-start method, call sites, import and advertised option are gone; an old server-port argument/environment value cannot enable a listener.
- A remote H2 URL, `AUTO_SERVER=TRUE`, unsafe initialization option or disabled-locking option fails validation before a listener or connection is created.
- JSON V2 export and application search still work with the embedded pool.
- While the app owns an embedded file database, a second JVM's ordinary embedded connection fails; the app's own pooled connections continue working.
- No H2 listener is opened by the application, and external connection attempts cannot query the active database. A port occupied by an unrelated process is not evidence of an OWLCMS listener; verify listener ownership.
- If another JVM owns the database first, OWLCMS startup fails without a silent fallback. Run the file-ownership checks on Windows and macOS/Linux.

These are proposed checks, not tests run against the current instance. PostgreSQL deployments need a separate network/authentication policy; do not label them embedded H2 or alter them incidentally in this work.

### Live Versus Final Snapshot

App-only database ownership does not stop concurrent writes by application threads. The current export collects separate repository reads, not a demonstrated single consistent snapshot. For a final export, pause relevant edits or use a proven consistent snapshot, and declare the corresponding audit boundary before recording the completed export event. A checksum establishes the identity of the emitted bytes, not snapshot consistency or result correctness.

Reformatting, newline conversion or editing intentionally changes the exact-byte checksum. Official approval and completeness of all competition actions are separate assertions. A separately signed export manifest remains an optional stronger profile only if verification without the audit trail becomes a requirement.

## 9. Completeness, Restart and Independent Evidence

A valid signature over a surviving prefix does not detect removal of later blocks. A valid signed platform file does not prove another platform file was not omitted.

- Maintain a signed competition inventory listing all expected logical streams and generations, and bind it into final exports.
- Link new file generations and process runs to their preceding checkpoints. Keep competition identity separate from transient run IDs and sanitized filenames.
- On restart, verify the existing sealed history, recover pending work and declare any unsealed tail. Do not silently bless a rewritten file by generating a replacement seal.
- A crash may leave a partial record or unsigned tail. Preserve it as evidence and report it, rather than silently modifying previously sealed bytes.
- Independently retain checkpoints and the final inventory. Local persistence alone cannot detect rollback of the whole directory to an earlier valid signed version.

A witness service should record receipt time and reject incompatible successors for a registered stream/checkpoint. A timestamp authority can establish that a particular digest existed by a time, but does not by itself prevent the signer from creating competing histories. Retained receipts must be available to verifiers.

For offline events, copy periodic signed checkpoints to an official's independently held device or archive. During a connectivity outage, continue local sealing and mark checkpoints as locally sealed but not witnessed. The exposed window lasts until independent receipt; the local clock is not trusted timestamp evidence.

## 10. Verification and Acceptance Criteria

Provide an offline verifier requiring no private key and no running OWLCMS instance. It must:

1. Establish trust in the public key from a separately approved fingerprint or registry.
2. Verify manifest signatures, purpose/version and predecessor links.
3. Parse inline seals and check every covered audit-line range, exact bytes, final audit-line count, expected file length and competition inventory.
4. Identify missing, overlapping, duplicated, reordered or altered blocks, and missing whole streams.
5. Distinguish a valid covered prefix from a complete finalized package; report unsealed tails and gaps.
6. Compare independent checkpoints/receipts when available and identify rollback or conflicting histories.
7. Report signature validity, signer trust and independent witnessing separately. A mathematically valid signature from an unknown key is not a trusted result.

For JSON V2, verify the seal covering its export event and compare the file's SHA-256 and byte length with that event. Test partial generation/download, one changed byte, replacement with another export, and an export event whose seal is still pending. A matching checksum without an authenticated reference must not be reported as trusted provenance.

Before implementation is accepted, benchmark the existing logger against sealing enabled using representative concurrent platform traffic. Measure logging-call latency, throughput, CPU, disk traffic, sealing lag and queue growth. Include rollover and final export under load. Agree numeric overhead limits from the baseline before selecting thresholds.

Exercise alteration of one byte, record deletion/reordering, file truncation, removal of the final block, omission of a complete platform, replacement of the public key, restart, crash during publication, disk full, key loss and witness outage. Run file-lifecycle and unchanged-copy verification tests on Windows and macOS/Linux.

Logging frameworks can report I/O failure without throwing it back to the caller. Sealing cannot compensate for entries that never reached the file. Integrate logger/appender health reporting and refuse a clean final integrity status when known audit-write or sealing failures remain unresolved. Whether competition operation itself must stop on such failures is a separate policy decision.

## 11. Decisions Before Implementation

1. Is post-event protection sufficient initially, or are periodic in-session checkpoints required?
2. Is the threat accidental alteration, deliberate file editing, or a compromised competition machine?
3. Who registers the signer fingerprint and retains the final inventory independently?
4. Should the signer represent an installation, organizer or competition, and who authorizes key rotation?
5. Is an offline evidence package sufficient, or is independent remote witnessing required?
6. Does the logging requirement mean the current Logback setup, or a specific Log4j 2 deployment?
7. What latency/throughput overhead and maximum unsealed interval are acceptable?
8. What finalization/snapshot procedure binds the final JSON export to the audit boundary?

Proposed sequence: benchmark logger-owned byte batches against incremental appender digests; implement original-output capture with asynchronous seals; record JSON export SHA-256 digests in audited export events; independently retain checkpoints for stronger guarantees. Rollover-only or periodic reread sealing is insufficient for pre-seal tampering. No per-entry hash chain is required at any stage.