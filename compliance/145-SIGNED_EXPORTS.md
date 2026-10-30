# Signed Exports

Status: proposed 2026-10-28.

Make a JSON export show whether it is the unmodified output of a server holding
a given PEM key. This gives evidence of tampering; it does not control who may
load an export or obtain a temporary admin password ([100](done/100-PEM_ACCOUNT_MODE.md)).
Forwarding-key encryption is in [140](done/140-EXPORT_CREDENTIAL_PROTECTION.md).

## Existing behavior

- Every V1/V2 export records the SHA-256 and length of its exact bytes as an
  `export.json` audit event (`ChecksummedJsonExport`, `ExportAudit`). The
  audit chain is sealed with the PEM, so this is already a signed checksum,
  verifiable only against the original server's audit trail.
- Startup imports record the SHA-256 of the bytes loaded (`ApplicationAudit`).

## Signature in the JSON

- Sign only when a configured PEM is present. The built-in key's private part
  is public, so its signature would prove nothing.
- Add a top-level `signature` object, written last:
  `alg`, `keyFingerprint`, `canonical` (signed-form version, for example
  `owlcms-1`), `sha256`, `auditHead` and `value`.
- Signed form: parse the JSON, remove the top-level `signature`, and write it
  back with a fixed mapper configuration (sorted properties, stable number
  formatting). `sha256` is the digest of that form. Signer and verifier use the
  same code, as `TrackerDatabaseChecksum` already does.
- Sign the hash, not the content: `value` is the Ed25519 signature of a purpose
  prefix (for example `owlcms-export-v1`) followed by the canonical JSON of the
  other `signature` fields (`alg`, `auditHead`, `canonical`, `keyFingerprint`,
  `sha256`), as
  `AuditMarkerCodec` does for audit markers. Ed25519 cannot sign incrementally,
  the metadata is bound to the signature, and the prefix keeps export and audit
  marker signatures from being reused for each other.
- Verify by recomputing `sha256` from the content first, then checking `value`.

## Position in the audit trail

- `auditHead` identifies the last sealed block of the competition audit stream
  when the export is taken: `stream`, `runId`, `artifactName`, `lastLine` and
  `blockSha256`. Line numbers restart in each audit file, so `blockSha256` is
  the identity; the other fields locate the block.
- The export's own `export.json` event is written later in the same stream, so
  the export lies between `auditHead` and that event, verifiable on one chain.
- Platform streams are not referenced. They share the run, key and clock, so
  their signed timestamps order lifts relative to the export, and their own
  chains detect edits and truncation.
- No seal is forced before export; competition lines written after the head
  fall inside the window. Forcing a seal is optional, to narrow it.
- The integrity check locates `auditHead` and the matching `export.json` event
  and reports the window, or that the trail containing them is not available.
- Unlike the tracker checksum, keep `exportDate` and the encrypted `updateKey`
  values in the signed form.
- The audited SHA-256 of the exact bytes is unchanged and also covers the
  signature.
- Exports are streamed through a pipe: buffer the body or make a second pass so
  the signature is computed before it is written.
- An unknown top-level field is ignored by OWLCMS V1/V2 import
  (`FAIL_ON_UNKNOWN_PROPERTIES` disabled) and by tracker-core `parseV2Database`,
  which reads known fields by name. Confirm with tracker-core when implementing.
- `TrackerDatabaseChecksum` must also remove `signature`; otherwise the tracker
  cache identity changes on every export.

## Import

Loading is never refused because of the signature. Record one outcome in the
audit trail, with the import SHA-256:

| Export | Outcome recorded |
| --- | --- |
| No `signature` | unsigned |
| Signature valid, key is this server's PEM | valid, fingerprint |
| Signature valid, another key | valid, foreign fingerprint (verifiable only with that public key) |
| Signature does not match the content | invalid, claimed fingerprint |
| Server without a configured PEM | not authenticated, plus whatever the export claims |

Show the outcome of the import that replaced the competition in the audit
integrity check and report.

## Usage without a key

Accounts mode is commonly used without a PEM, with empty or trivial role
passwords, only to limit what each station shows; the admin password is kept
for support. Nothing changes for this usage: exports are not signed, and
support can remove the admin hash, load the file and use the temporary admin
password.

## Support procedures

A support provider receiving an export from a PEM-protected competition can
always obtain a temporary admin password by removing the admin hash. Without
the key, its server records the import as invalid, cannot decrypt the
forwarding keys (exported as null, see 140) and produces unsigned exports.
Two standard procedures:

### A. Support is given the key

For fixes that support must make directly on the competition.

1. The operator sends the export and the PEM by separate secure channels.
2. Support starts with the PEM, removes the admin hash if needed, loads the
   export and logs in as `admin` with its own control panel token.
3. Support makes the fix and exports. The export is signed with the
   competition key and keeps its forwarding keys.
4. The operator loads the returned export. The audit trail shows the invalid
   import (admin hash removed), the control panel token login and the support
   changes, all signed with the competition key.
5. The operator decides whether to generate a new PEM after the support case.

### B. The operator keeps the key

Default procedure.

1. The operator sends the export only.
2. Support removes the admin hash, loads the export, makes the fix and
   exports. The returned export is unsigned and has no forwarding keys.
3. The operator loads it on the server holding the PEM, checks it, re-enters
   the forwarding keys and resets the admin password if needed (log in as
   `admin` with the control panel token).
4. Exports from that point are signed with the competition key. The audit
   trail records that an unsigned export was accepted.

## Acceptance

- With a PEM: exports carry a valid signature; editing any value, including
  `exportDate` or an `updateKey`, makes it invalid; removing `signature` makes
  it unsigned.
- Without a PEM: exports have no `signature`; imports record not authenticated.
- `auditHead` names a sealed block of the competition stream that precedes the
  export's `export.json` event, across audit file changes and server restarts.
- Fixture test pins the signed form, so a library upgrade that changes it
  fails the build.
- Each import outcome in the table is recorded and shown by the integrity
  check.
- Procedure A: the returned export is signed with the competition key and the
  reset is visible in the trail.
- Procedure B: the operator's trail records the unsigned import, and the next
  export is signed.
- Key-less usage: removing the admin hash and loading still yields a temporary
  admin password.
