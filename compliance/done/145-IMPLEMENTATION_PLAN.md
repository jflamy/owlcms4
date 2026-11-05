# 145 Implementation Plan: Signed V2 exports

Status: implemented and accepted 2026-11-04. Done: phase 1 (`ExportSignature`,
`ExportSignatureTest`), phase 2 (signed V2 export), phase 3 (import outcome in
the audit trail), phase 4 (export check section on `admin/audit/check`,
`ExportCheckText`, `export_check_translations.tsv`), phase 5 (tracker-core
`tests/v2-signature-test.js`, release notes), final acceptance (manual check done by the maintainer). Decided with the
maintainer on 2026-11-04 (already reflected below):

- Always sign, with the audit signing key, built-in included (invariant 4).
  `ExportSignature.trailer` accepts any key; there is no `allowBuiltin` overload.
- V1 stays unsigned. The `noV1Export` feature switch (IWF Requirements section)
  withdraws V1 exports through `Capability.EXPORT_V1` and implies `v2Export`.
  The IWF compliance check only reports it ("Exports are signed").
- Verification UI is a section of `AuditIntegrityCheckView`, not a separate
  page; the upload is checked in the upload callback (rare, in-memory hashing).
- Checking an export is independent of the audit trail. The integrity check
  does not show import outcomes (former 4.3, dropped).

Implements [145-SIGNED_EXPORTS.md](145-SIGNED_EXPORTS.md). Read the spec first;
this plan says how, the spec says what. Where they disagree, stop and ask the
maintainer.

Already in place (do not redo): `ChecksummedJsonExport` (streamed export with a
running SHA-256), `ExportAudit` (`export.json` events), `ApplicationAudit`
(`application.import`, `application.startup-import`), `AuditSigningKey` /
`AuditPublicKey` (Ed25519), `AuditMarkerCodec.canonicalJson`, `AuditIntegrity`
(startup-frozen signing key), `AuditIntegrityCheckView`, forwarding-key
encryption (140).

Out of scope: V1 exports (never signed), support procedures (operator
practice, no code), an `auditHead` reference, a JSON-level canonical form,
changes to tracker or tracker-core beyond a fixture test.

## Rules for this work

- Read `AGENTS.md` and `.github/copilot-instructions.md`. In particular:
  - Never run Maven while the dev server is up. Check with
    `curl -s -o /dev/null -w '%{http_code}' --max-time 3 http://localhost:8080/`
    (000 means down). If it is up, ask the maintainer to stop it and wait.
  - Focused Maven test runs are pre-authorized (skill `run-maven-test`); read the
    Surefire reports. Never `mvn package` or `install`.
  - Never commit, push or stage without the maintainer's explicit go. Never add a
    Copilot `Co-authored-by` trailer.
  - Never edit `shared/src/main/resources/i18n/translation4.csv`. New keys go
    into a TSV (skill `update-translations`).
  - No hard-coded UI strings; no fully qualified class names in Java sources.
  - Avoid `OwlcmsSession` and `UI.getCurrent()` in new code: capture values at
    the edge (listener, caller thread) and pass them as parameters, including to
    the export writer thread.
  - Logging: skill `owlcms-logging-format`. Never log key material.
- Do not create Vaadin components in unit tests (skill `add-test-case`).
- After editing Java, check the Problems panel for the touched files (skill
  `verify-java-fix`).
- Stop at the end of each phase, report, and wait for the maintainer.

## Invariants (check every phase against these)

1. **One pass when generating.** The export body is serialized once and
   streamed. Never parse, buffer whole, re-read or re-serialize the export to
   sign it. Extra memory is bounded (one held-back byte, one fixed-size trailer).
2. **No JSON parsing to verify.** Verification hashes raw bytes once and checks
   a fixed-length byte trailer. Import still parses as it does today; the
   verifier adds no parse.
3. **One checksum.** `sha256` = SHA-256 of the JSON body as the serializer
   wrote it, including its closing `}`, without the signature. For an unsigned
   export this is the SHA-256 of the whole file, so unsigned behavior is
   unchanged. Audit records always carry the locally computed value, never the
   value claimed by the file.
4. **Always sign, as the audit trail does.** Use the audit signing key, even
   the built-in one. Its private part is public, so never report a
   built-in-key signature as authenticated (KeyMatch BUILTIN).
5. **Never refuse an import because of the signature.** Record the outcome.
6. **A signing failure fails the export.** Never fall back silently to an
   unsigned file when a signing key is present.
7. **V2 only.** V1 export output is byte-for-byte unchanged.

## Wire format (exact)

The exporter writes the body with its final `}` held back, then this trailer,
ASCII, no whitespace, fields in this order:

```text
,"signature":{"alg":"Ed25519","publicKey":"<P>","keyFingerprint":"<F>","sha256":"<H>","value":"<V>"}}
```

- `<P>`: Base64 (standard alphabet, padded) of the X.509 encoding of the Ed25519
  public key, `AuditSigningKey.publicKeyBase64()`. Always 44 bytes, so always
  60 characters ending in one `=`.
- `<F>`: `AuditSigningKey.fingerprint()`, lowercase hex SHA-256 of the X.509
  bytes. 64 characters.
- `<H>`: lowercase hex body checksum (invariant 3). 64 characters.
- `<V>`: Base64 of the 64-byte Ed25519 signature, 88 characters ending in `==`.

The trailer is therefore always **365 bytes**. Compute the length from the
format in code and assert it equals 365 in a test; do not scatter the literal.

Signed message for `<V>`:

```text
"OWLCMS-EXPORT-SIG:1\n" + canonicalJson({alg, keyFingerprint, sha256})
```

UTF-8, with `canonicalJson` the existing `AuditMarkerCodec.canonicalJson`
(sorted keys, no whitespace). `publicKey` is not in the message: the fingerprint
binds it, and the verifier checks `SHA-256(P) == F`.

Recognition pattern for the last 365 bytes (anchored at both ends):

```text
,"signature":\{"alg":"Ed25519","publicKey":"([A-Za-z0-9+/]{59}=)","keyFingerprint":"([0-9a-f]{64})","sha256":"([0-9a-f]{64})","value":"([A-Za-z0-9+/]{86}==)"\}\}
```

Also require canonical Base64 (decode then re-encode yields the same text).

Verification on a file of `n` bytes:

1. If `n < 365` or the last 365 bytes do not match the pattern: **ABSENT**
   (absent or unrecognized). Checksum = SHA-256 of the whole file.
2. Else body = bytes `[0, n-365)` followed by one `}`. Hash it (one pass:
   update with the prefix, then with `}`). Mismatch with `<H>`: **INVALID**
   (reason CHECKSUM).
3. Decode `<P>`; failure, or `SHA-256(P) != <F>`: **INVALID** (reason KEY).
4. Verify `<V>` over the signed message with `<P>`; failure: **INVALID**
   (reason SIGNATURE).
5. Else **VALID**, with fingerprint `<F>`.

Key match is a separate step, done by the caller once the server key is known:
OWN (fingerprint equals the server's configured, non-built-in key), BUILTIN
(equals `AuditSigningKey.BUILTIN_FINGERPRINT`), else FOREIGN.

## Phase 1: signature codec and verifier

### 1.1 `owlcms/src/main/java/app/owlcms/audit/ExportSignature.java` (new)

Put it in `app.owlcms.audit` so it can use the package-private
`AuditMarkerCodec.canonicalJson` without widening it.

```java
public final class ExportSignature {
	public static final int TRAILER_LENGTH; // 365, computed from the format

	public enum Status { ABSENT, INVALID, VALID }
	public enum Reason { NONE, CHECKSUM, KEY, SIGNATURE }
	public enum KeyMatch { NONE, OWN, FOREIGN, BUILTIN }

	/** sha256: locally computed (body if a trailer was recognized, else whole file). */
	public record Result(Status status, Reason reason, String sha256, long bytes, String fingerprint) {}

	/** Trailer bytes for a body checksum, with any key, built-in included. */
	public static byte[] trailer(String bodySha256, AuditSigningKey key) throws GeneralSecurityException;

	public static Result verify(byte[] file);

	/** serverKey: AuditIntegrity's signing key, or null if not initialized. */
	public static KeyMatch match(Result result, AuditSigningKey serverKey);
}
```

- `trailer` builds the exact format above, with any key (invariant 4).
- `verify` follows the five steps. `fingerprint` is the claimed `<F>` for
  INVALID, the verified one for VALID, null for ABSENT. It must not throw for
  any input; malformed input is ABSENT or INVALID.
- `verify(byte[])` is enough: every caller already holds the JSON bytes
  (`FormatDetector.readJsonBytes`). Do not add a streaming variant unless a
  caller needs it.
- `match` returns NONE unless status is VALID.

### 1.2 Tests: `owlcms/src/test/java/app/owlcms/audit/ExportSignatureTest.java` (new)

Generate a key with `AuditSigningKey.generate()`; build files as
`body[0..n-1] + trailer(sha256(body), key)`.

- Trailer length is 365; `TRAILER_LENGTH == 365`.
- Round trip: VALID, `sha256` equals SHA-256 of the original body.
- Unsigned body: ABSENT, `sha256` equals SHA-256 of the whole file.
- Edit one body byte: INVALID/CHECKSUM.
- Edit body and replace `<H>` with the new checksum: INVALID/SIGNATURE.
- Re-sign an edited body with another generated key: VALID, `match` FOREIGN.
- `match` with the signing key: OWN. BUILTIN: anyone can sign with the
  built-in key (its private part is public), so a file signed that way must
  verify VALID and match BUILTIN.
- `<P>` swapped for another valid key: INVALID/KEY.
- Non-canonical Base64 in `<P>` or `<V>`: not VALID.
- Bytes appended after the trailer, a field added, whitespace inserted, a
  truncated trailer: never VALID (ABSENT is acceptable).
- A body string containing `,"signature":` and a full fake trailer inside a
  string value: still verifies on the real trailer only.
- Garbage, empty array, 364 bytes: ABSENT, no exception.

Stop. Report.

## Phase 2: single-pass signed export

### 2.1 `data/export/ChecksummedJsonExport.java`

Add an optional `AuditSigningKey signingKey` (null = unsigned: V1, and V2 only
when `AuditIntegrity` is not initialized, e.g. in unit tests).

Stream chain when signing:

```text
body -> NonClosing -> DigestOutputStream(digest) -> HoldLastByte -> Counting -> pipe
```

- `HoldLastByteOutputStream` (private nested class): forwards every byte except
  the most recent one, which it holds. Single-byte buffer; for array writes,
  forward `len-1` bytes plus the previously held byte, then hold the last.
- After `body.writeTo` returns: digest is final (it saw every body byte,
  including the held `}`). Require the held byte to be `}`; otherwise fail the
  export (`IOException("export body does not end with }")`).
- `bodySha256 = hex(digest.digest())`; write `ExportSignature.trailer(bodySha256,
  key)` straight to the counting stream (bypassing digest and hold-back).
- Only then build `Checksum(counter.count, bodySha256)`, call
  `listener.succeeded`, complete the future, close. Any exception in signing or
  writing the trailer goes through the existing failure path (invariant 6):
  `writerException` set before close, `listener.failed`, no success event.
- Unsigned path: unchanged, no hold-back stream in the chain.
- Extend `Checksum` with `String signatureFingerprint` (null when unsigned) so
  the audit listener can record it. Update existing constructors/tests.
- `audited(int format, String channel, AuditActor actor, AuditSigningKey
  signingKey, Body body)`; keep the old signature delegating with `null` for V1.

### 2.2 `data/export/v2/CompetitionDataV2.java`

In `export(...)`, on the caller's thread, before starting the writer:

```java
AuditSigningKey key = AuditIntegrity.isInitialized()
        ? AuditIntegrity.current().signingKey() : null;
```

Pass it to `ChecksummedJsonExport.audited(2, ...)`. Applies to every V2 entry
point (download, HTTP, WebSocket, checkpoint ZIP in `AuditSnapshot`, the
no-arg `exportData()`). Check that no getter or field named `signature` exists
on `CompetitionDataV2` (the name is reserved for the trailer).

`CompetitionData` (V1): no change.

### 2.3 `audit/ExportAudit.java`

`successDetail` gains `signature=<fingerprint>` or `signature=none`, after
`sha256`. Update `ExportAuditTest` for both cases. `sha256` keeps its name; its
meaning is the body checksum (invariant 3), equal to the file checksum when
unsigned.

### 2.4 `monitors/websocket/TrackerDatabaseChecksum.java`

Remove top-level `signature` along with `exportDate`. Add a case to
`TrackerDatabaseChecksumTest`: two exports of the same data with different
signatures give the same tracker checksum.

### 2.5 Tests

`ChecksummedJsonExportTest`, new cases:

- Signed: output = body without last byte + trailer; `ExportSignature.verify`
  on the output is VALID; `checksum().sha256()` equals SHA-256 of the body;
  `checksum().bytes()` equals output length.
- Body larger than the pipe buffer, written in odd-sized chunks and one byte at
  a time: same result (hold-back correctness across chunk boundaries).
- Body not ending in `}`: export fails, reader sees the error, no success event.
- Signing throws (e.g. a key whose `sign` fails, or inject via a test seam):
  reader sees the error, no success event, no unsigned output delivered as
  complete.
- Unsigned path: output and checksum identical to today.

`AuditSnapshotScopeTest` or a new test: the checkpoint ZIP's JSON entry
verifies VALID when a key is given (if the existing test infrastructure allows
a key without `AuditIntegrity` initialization; otherwise cover through
`ChecksummedJsonExportTest` and report).

Run `V2FormatCoverageTest`, `JSONExportImportTest`, `FormatDetectorZipTest`:
an import of a signed export must succeed (unknown top-level field ignored).

Stop. Report.

## Phase 3: import outcome in the audit trail

### 3.1 `audit/ApplicationAudit.java`

Change `imported(...)` and `startupImport(...)` to take the
`ExportSignature.Result` and `KeyMatch`, and write:

```text
source=..,bytes=<file length>,sha256=<result.sha256>,signature=<absent|invalid|valid>,reason=<checksum|key|signature|->,key=<own|foreign|builtin|->,fingerprint=<hex|->
```

`startupImport` gains `bytes`. Keep field order stable; tests assert it.

### 3.2 `data/export/FormatDetector.java`

In `importData(byte[] uploaded, ...)`: after `readJsonBytes`, call
`ExportSignature.verify(json)` once, replacing the separate `sha256(json)`
(the verifier already hashed the bytes; do not hash twice). After the import,
compute `match(result, AuditIntegrity.current().signingKey())` and pass both to
`ApplicationAudit.imported`. Remove the now-unused private `sha256` if nothing
else uses it.

### 3.3 `Main.java` (`initFromImport`)

The startup import runs **before** `completeConfig()` initializes
`AuditIntegrity`. So:

- `ExportSignature.Result result = ExportSignature.verify(json);` before the
  import, replacing `sha256(json)`; log it with the existing info line.
- After `completeConfig()`, compute `match(result,
  AuditIntegrity.current().signingKey())` and call
  `ApplicationAudit.startupImport(name, json.length, result, match)`.

### 3.4 Tests

- Unit-test the detail strings (same style as `ExportAuditTest`) for each row of
  the spec's import table: ABSENT, INVALID (each reason), VALID OWN, VALID
  FOREIGN, VALID BUILTIN.
- Extend an existing import test to import a signed and a tampered V2 export
  and assert the import succeeds in both cases (invariant 5).

Stop. Report.

## Phase 4: export check (done)

### 4.1 Export check section on `admin/audit/check` (done)

`AuditIntegrityCheckView` has an "Export Signature Check" section: the same
upload as the database import (`UploadHandler.inMemory`, `application/json`,
ZIP read with `FormatDetector.readJsonBytes`), but the file is only verified
with `ExportSignature.verify`/`match`, never imported. The check is rare and
only hashes bytes already in memory, so it runs in the upload callback (UI
thread), with no background thread. No control panel token: only the uploaded
bytes are read. Text in `audit/ExportCheckText` (tested by
`ExportCheckTextTest`): verdict (unsigned, invalid, signed with this server's
key / another key / the built-in key), file, size, SHA-256, fingerprint, and
the fingerprint comparison paragraph for own and foreign keys.

### 4.2 Translations

All new labels in one TSV under `shared/src/main/resources/i18n/` (skill
`update-translations`): `ExportCheck.` for the check section (done,
`export_check_translations.tsv`).

Stop. Report.

## Phase 5: tracker-core fixture and documentation

### 5.1 tracker-core

`/Users/jflamy/git/tracker-core/src/protocol/parser-v2.js` reads known fields by
name and spreads only `db.competition`; a top-level `signature` is ignored. Add
a test under `tracker-core/tests/` that parses a minimal V2 database with a
`signature` object and asserts the normalized result has no `signature` key and
is otherwise identical. Do not change parser code. Ask the maintainer before
running tracker-core tests if unsure how.

### 5.2 Documentation

- [145-SIGNED_EXPORTS.md](145-SIGNED_EXPORTS.md): set status to implemented,
  note any deviation decided with the maintainer. Move both files to `done/`
  only when the maintainer says so, and fix the links in
  [000-INDEX.md](../000-INDEX.md).
- Release notes: skill `update-release-notes`. One entry: V2 exports are
  signed (with the audit key; the built-in key only shows the file is intact);
  the audit check page can check an export's signature; imports record the
  signature outcome; the IWF Requirements feature switches (`noV1Export`
  withdraws the unsigned V1 export and implies `v2Export`).

Stop. Report.

## Final acceptance (manual, with the maintainer)

Automated: the unit tests cover the codec, the signed stream, the audit
details, the check texts and the import of signed and edited exports.
`AuditLogPersistenceTest.realV2ExportIsSignedAndAnEditedImportIsRecordedInvalid`
(slow category, separate JVM) runs the real V2 export with a configured key,
checks it is signed with that key, imports an edited copy, and checks both
audit records.

Manual, on the dev server: download a V2 export and check it in the Export
Signature Check section of the audit check page. This covers the page and the
download button.
