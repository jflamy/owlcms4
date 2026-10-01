# Audit Log and JSON V2 Export Integrity

Status: proposal only, 2026-10-01. No implementation changes are authorized by this document.

Related: [AUDIT_TRAIL_DESIGN.md](AUDIT_TRAIL_DESIGN.md).

## 1. Final Recommendation

- **Audit log:** retain bounded batches of the exact bytes written by the logger. Hash and sign each completed batch asynchronously, then append a `SEAL` line covering audit records X through Y.
- **JSON V2:** hash the exact exported bytes once and record the SHA-256 and byte length in the audited `export.jsonv2` event. The audit seal covering that event authenticates the checksum.
- **Key pair:** use a dedicated Ed25519 pair. Low-risk installations may load it from `~/.owlcms/auditKey.pem`. Higher-risk installations retrieve it from a password manager and inject it without local persistence. Cloud deployments inject the same values through cloud configuration and secrets.
- **H2:** remove the H2 TCP server option and permit only process-local embedded H2 URLs. Keep the existing Hikari connection pool.
- **Retention:** keep completed seals or the final signed inventory independently from the competition machine to detect replacement or rollback of the complete local evidence set.

Changing, deleting or reordering sealed records invalidates a signature, and changing a JSON export makes its digest differ from the sealed `export.jsonv2` event. Replacing the public key fails comparison with the independently registered fingerprint. Immediate transfer to another system preserves the later seals and final inventory needed to detect truncation or rollback.

The password-manager profile assumes no administrative or interactive access to the competition machine while OWLCMS runs. On restart, the operator injects a key pair again. The same pair is expected but not required: a different pair starts a new signer section in the audit log, and that section must later be verified with its own public key. Compromise of the password manager, key-entry path or running process remains outside the guarantee.

Under these constraints, sealed logs and completed JSON exports are moved immediately to independent safekeeping together with the final inventory. Given the trusted public key or keys, the Check Integrity program confirms that every retained sealed log section is unchanged and that every retained JSON export matches its sealed audit event. A successful complete check therefore confirms the absence of tampering in the retained evidence.

The security, access control, availability and retention policy of the independent safekeeping system are outside the scope of this proposal.

## 2. Current Implementation

- [AuditLog.java](../owlcms/src/main/java/app/owlcms/audit/AuditLog.java) uses SLF4J and Logback with daily rolling files under `logs/audit/`. Each platform has a readable `.log` and a `_full.log`; the `_full` logs are the authoritative files to seal.
- [CompetitionDataV2.java](../owlcms/src/main/java/app/owlcms/data/export/v2/CompetitionDataV2.java) writes JSON V2 through a background writer and pipe. It does not currently compute a digest.
- [InstallationSecret.java](../shared/src/main/java/app/owlcms/utils/InstallationSecret.java) and `controlpanel/shared/secret.go` share the existing Base64-encoded 32-byte AES installation key at `~/.owlcms/key`.
- [JPAService.java](../owlcms/src/main/java/app/owlcms/data/jpa/JPAService.java) uses embedded H2 by default and a Hikari pool with minimum 5 and maximum 15 connections. It can currently start an H2 TCP listener when `H2ServerPort` or `OWLCMS_H2SERVERPORT` is set.

## 3. Audit Log Sealing

### 3.1 Capture and Batch

For each `_full.log` stream:

1. Encode each ordinary audit record once, including its actual line terminator.
2. Write those bytes through the normal appender and retain the same bytes in the active bounded batch.
3. Close the batch on its record-count, byte-size or age limit, or at rollover, shutdown or finalization.
4. Freeze the batch and transfer it to a background worker while logging continues into a new batch.
5. Hash the frozen bytes with SHA-256, sign the seal payload with Ed25519, and return the completed seal to the owning appender.
6. Append the seal through the same serialized writer, bypassing ordinary audit capture so seals cannot recursively enter a batch.

Batches are non-overlapping. Each ordinary record belongs to exactly one batch. Bound both individual batch size and total queued bytes. If the worker cannot keep up, apply defined backpressure or report an integrity-degraded state; never discard an unsealed batch silently.

Use 1,000 records, 1 MiB or 5 seconds, whichever comes first, as the initial benchmark configuration. Confirm the final thresholds from measured logging latency, throughput, CPU, disk traffic, sealing lag and queue growth.

### 3.2 Inline Seal

A seal can appear after records written later than the range it covers:

```text
<audit records 1 through 1042>
SEAL {"version":1,"firstLine":1,"lastLine":1000,"sha256":"...","previousSeal":"GENESIS","keyFingerprint":"...","signature":"..."}
<audit record 1043>
```

`firstLine` and `lastLine` are audit-record ordinals within the file generation. They exclude `SEAL` control lines and are independent of physical file-line positions and process-local sequence numbers.

Hash the exact concatenated bytes of the declared ordinary records, including line terminators. Emit seals in batch order. Each seal links to the digest of the preceding signed envelope, or to an explicit genesis marker.

The signed payload contains:

- schema, hash and signature algorithm versions;
- competition audit ID, signer ID, stream ID and file-generation ID;
- relative artifact name, batch number, first and last audit-record ordinals, byte count and SHA-256;
- preceding seal digest;
- UTC sealing time, application version and public-key fingerprint; and
- an intermediate or final marker.

Use RFC 8785 canonical JSON for the signed payload. Prefix the signed bytes with an explicit audit-seal purpose and version.

### 3.3 Public Key and Fingerprint

The public-key fingerprint is the full SHA-256 of the Ed25519 public key's DER SubjectPublicKeyInfo encoding.

Every process run starts a signer section with an `audit.open` record containing the run ID, Base64 SubjectPublicKeyInfo public key and full fingerprint. The first seal in that section covers `audit.open`; later seals contain the run ID and fingerprint. A restart may append a section signed by the same key or a different key. Seal chains do not cross signer-section boundaries, and verification selects the applicable key separately for each section.

The public key is not secret. A verifier trusts it only when its fingerprint matches one registered independently before the competition.

### 3.4 Rollover, Shutdown and Failure

At rollover or orderly shutdown, close the partial batch, drain pending seals into that file generation, and append a signed final control record containing the total audit-record count and final seal digest.

Before declaring a generation complete, read it back and compare every covered range with its signed commitment. Do not sign bytes reread from disk as a replacement for lost original batches.

After a crash, preserve partial records, incomplete seals and unsigned tails as evidence and report them as unverified. Known logger write, disk or sealing failures prevent a clean final integrity status.

The implementation uses portable Java cryptography and file APIs. Exact-byte verification intentionally detects newline conversion. Test rollover, restart and recovery on Windows, macOS and Linux; do not assume Unix open-file rename behavior on Windows.

## 4. Signing Key Provisioning

Use a dedicated Ed25519 pair. Do not reuse the installation AES key, API credentials or password hashes.

Keep `~/.owlcms/key` in its current format. It remains the raw Base64-encoded 32-byte AES key read by Java and Go; it is not converted to PEM.

### 4.1 Low-Risk Local File

Low-risk control-panel installations may store the signing pair in `~/.owlcms/auditKey.pem`, mode `0600`, under the existing mode-`0700` directory:

```text
-----BEGIN PRIVATE KEY-----
<Base64 PKCS#8 Ed25519 private key>
-----END PRIVATE KEY-----
-----BEGIN PUBLIC KEY-----
<Base64 SubjectPublicKeyInfo Ed25519 public key>
-----END PUBLIC KEY-----
```

The control panel parses the PEM, validates that the keys match, injects their Base64 DER values as `OWLCMS_AUDIT_SIGNING_PRIVATE_KEY` and `OWLCMS_AUDIT_SIGNING_PUBLIC_KEY`, and never logs the private value.

### 4.2 Password-Manager Injection

For higher-risk competitions, the private key is retained in the operator's password manager and is not saved in `~/.owlcms`, `env.properties` or the OWLCMS installation.

At launch:

1. The control panel requests the Base64 PKCS#8 private key through a masked secret-entry control. The operator retrieves and pastes it from the password manager.
2. The control panel accepts the corresponding Base64 SubjectPublicKeyInfo public key, validates that the pair matches and displays its full fingerprint for confirmation.
3. The control panel places the values only in the OWLCMS child process environment as `OWLCMS_AUDIT_SIGNING_PRIVATE_KEY` and `OWLCMS_AUDIT_SIGNING_PUBLIC_KEY`.
4. The control panel does not write either value to configuration, files, command-line arguments, logs or shell history. It releases its transient copies after the child starts.
5. OWLCMS validates the pair at startup and keeps the private key only for the lifetime of the process.

The competition machine has no operator or administrative access while OWLCMS runs. When the process ends, no local persistent copy of the private key remains. The password manager remains the authoritative private-key store. A restart repeats the injection procedure. Reusing the same pair simplifies verification; injecting a different valid pair is permitted and creates a new signer section with a different fingerprint.

Cloud deployments inject the private value as a secret and the public value as configuration under the same environment names; no PEM file is required.

OWLCMS validates the pair at startup. A missing, malformed or mismatched pair is a visible configuration error and must not cause silent generation of a new identity. Key replacement requires explicit, recorded rotation. Retain old public keys so existing evidence remains verifiable.

The local PEM profile relies on OS account access and file permissions. The password-manager profile removes that at-rest copy but does not protect against compromise during entry or while OWLCMS holds the key in its environment and memory.

## 5. JSON V2 Export

Generate the JSON once and compute SHA-256 over the exact bytes delivered as the export. Do not regenerate the content for checksum calculation.

After successful serialization, emit a competition-wide `export.jsonv2` audit event containing:

- requesting actor and station;
- event timestamp, export ID and competition audit ID;
- artifact name and format version;
- exact byte length and full SHA-256;
- generation outcome; and
- for a final export, its declared snapshot and audit boundary.

Audit failed generation explicitly. A successful digest must never describe a partial artifact.

The export event is authenticated when its containing audit batch is sealed. Final evidence handover includes that seal. Verification first checks the seal and then compares the supplied JSON's byte length and SHA-256 with the event.

The JSON schema remains unchanged and no separate JSON signature or checksum sidecar is required. The checksum identifies exact bytes; it does not establish snapshot consistency. For a final export, pause relevant edits or use a proven consistent snapshot and declare its audit boundary.

## 6. Embedded H2 Only

Remove the external H2 server capability while preserving the application's connection pool:

1. Remove `startH2EmbeddedServer()`, its call sites, the `embeddedH2Server` flag and the `org.h2.tools.Server` import.
2. Remove `H2ServerPort` / `OWLCMS_H2SERVERPORT` support and its row in [docs/100-AdvancedTopics/140-Configuration.md](../docs/100-AdvancedTopics/140-Configuration.md).
3. Before creating the pool, allow only embedded `jdbc:h2:file:` and named `jdbc:h2:mem:` URLs for H2 deployments.
4. Reject TCP and SSL H2 URLs, `AUTO_SERVER=TRUE`, `FILE_LOCK=NO`, `INIT`, duplicate options and unsupported options.
5. Let H2 enforce its normal file lock. If another process owns the database, startup fails visibly without fallback.
6. Preserve the embedded H2 dependency and Hikari's minimum 5 and maximum 15 process-local connections.

A loopback listener is not an acceptable substitute for removing server startup. PostgreSQL deployments require their own network and authentication policy and are outside this recommendation.

## 7. Completeness and Independent Retention

Maintain a signed competition inventory listing every expected stream, file generation and signer section, including each run ID and public-key fingerprint. Link generations and process runs to preceding checkpoints.

Move sealed logs and completed JSON exports immediately to an independently controlled system. That system verifies incoming seals against the registered public-key fingerprint and retains the latest accepted seal and final inventory. Local signatures alone cannot reveal that the whole directory was rolled back to an earlier valid state or that a complete platform stream was omitted.

This proposal defines what evidence is transferred and how its integrity is checked. It does not define or evaluate the security controls of the receiving system.

During an outage, continue local sealing and mark checkpoints as not independently retained. The local clock alone is not trusted timestamp evidence.

## 8. Integrity Checker and Acceptance

Provide a standalone offline **Check Integrity** program requiring neither a running OWLCMS instance nor a private key. It accepts:

- one audit log, or a directory containing all audit logs for the competition;
- one or more JSON V2 exports, or a directory containing them; and
- one or more trusted public keys, supplied as PEM files or Base64 DER SubjectPublicKeyInfo values.

Key inputs are repeatable because restarted signer sections may use different pairs. A PEM containing both `PRIVATE KEY` and `PUBLIC KEY` blocks is accepted for operator convenience, but the checker reads only the public block and never needs the private key.

The checker must:

1. Split each log into signer sections beginning at `audit.open`.
2. Read the embedded public key and fingerprint from each section and match them to a supplied trusted public key. If one key was reused, one supplied key verifies every section; otherwise each distinct fingerprint requires its corresponding key.
3. Verify every seal signature, predecessor link and declared audit-record range within its signer section.
4. Detect changed, missing, duplicated, overlapping or reordered records and batches.
5. Report unsigned tails, incomplete seals, untrusted signer sections, missing streams and non-finalized packages.
6. Verify the signed competition inventory and independently retained checkpoints, including the expected run IDs and signer fingerprints.
7. Locate each sealed `export.jsonv2` event and compare the named JSON export's byte length and SHA-256 with that event.
8. Report results by file and signer section, showing run ID, fingerprint, covered record range, signature status, completeness and JSON status.

The program exits unsuccessfully for any altered data, invalid signature, missing required key, unsealed gap, inventory mismatch or JSON mismatch. It distinguishes those failures from an otherwise valid but deliberately non-finalized package.

Acceptance tests cover byte alteration, deletion, reordering, truncation, removal of a complete final batch, omission of a stream, public-key replacement, restart, crash during publication, disk full, key loss and unavailable independent storage.

Benchmark representative concurrent platform traffic before selecting final batch limits. Exercise unchanged-copy verification, rollover and crash recovery on Windows, macOS and Linux.
