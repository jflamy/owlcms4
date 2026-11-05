# Signed Exports

Status: proposed 2026-10-28. Implementation plan:
[145-IMPLEMENTATION_PLAN.md](145-IMPLEMENTATION_PLAN.md) (exact wire format).

Make a JSON export show whether it is the unmodified output of a server holding
a given PEM key. This gives evidence of tampering; it does not control who may
load an export or recover admin access.
Forwarding-key encryption is in [140](done/140-EXPORT_CREDENTIAL_PROTECTION.md).

Scope: V2 exports only. V1 exports are unchanged and never signed; a V1 import
is recorded as unsigned. The `noV1Export` feature switch (IWF Requirements
section) withdraws V1 exports: `Capability.EXPORT_V1` is unavailable, so the V1
button is hidden and the V1 download and `/competition/export[/json/1]` URLs
are refused and audited. `noV1Export` also turns on `v2Export`, so with it on,
every export offered is signed. The IWF compliance check reports both switches; it does not
set them.

## Existing behavior

- Every V1/V2 export records the SHA-256 and length of its exact bytes as an
  `export.json` audit event (`ChecksummedJsonExport`, `ExportAudit`). The
  audit chain is sealed with the PEM, so this is already a signed checksum,
  verifiable only against the original server's audit trail.
- Startup imports record the SHA-256 of the bytes loaded (`ApplicationAudit`).

## Signature in the JSON

The existing checksum is signed so that it cannot be recomputed after an edit,
and so that an export can be checked without the original audit trail. There is
one checksum: the SHA-256 of the original JSON body, including its closing
`}`, without the added `signature` field. The signature covers that checksum.
Exports are frequent (checkpoints), so the document is never parsed, buffered
or re-serialized to sign it.

- Always sign, with the key the audit trail uses. As for the audit trail, a
  built-in key signature only shows that the file is intact: its private part
  is public, so verification reports it as the built-in key (not
  authenticated), never as the server's own key.
- `ChecksummedJsonExport` holds back the final `}` from the output, but includes
  it in the running digest. Finalize the digest once; no clone is needed.
- It then writes the trailer `,"signature":{...}}`, making `signature` the last
  top-level field. The object has `alg` (`Ed25519`), `publicKey` (Base64 X.509,
  as in the `audit.open` header), `keyFingerprint` (SHA-256 of `publicKey`),
  `sha256` (the checksum) and `value`.
- The trailer bypasses the finalized digest. It has an exact ASCII byte
  format: fields in the order above, no whitespace, fixed literals and
  canonical Base64/hex values of the required lengths, so it is always 365
  bytes. Its final `}}` ends the file. The exporter reserves the top-level
  `signature` name.
- `value` is the Ed25519 signature of the purpose prefix
  `OWLCMS-EXPORT-SIG:1\n` followed by the canonical JSON of `alg`,
  `keyFingerprint` and `sha256` (the `AuditMarkerCodec` canonical form). The
  prefix keeps export and audit marker signatures from being reused for each
  other.

## Verification

The PEM holds two keys. The private key signs and must stay secret; it is never
included in the export. The public key only verifies and can be
published; the fingerprint is its short form. Checking a signature therefore
needs only the public key, which the export carries. No PEM is involved in
validation.

The file does not vouch for itself. Its signature shows only that the holder of
the embedded key's private part signed this content. Whether that key is the
competition's is decided by comparing the fingerprint with one obtained
independently. Someone who edits the file can re-sign it with their own key; the
result validates, but under a different fingerprint.

1. Trailer and checksum. Match the exact trailer format at the end of the
   file, checking its literals, field order, encodings and lengths directly
   on bytes. A marker search alone is not sufficient. Extra fields, whitespace
   or data after the trailer cannot match. Hash the preceding raw bytes plus
   one `}` to reconstruct the original body checksum; it must equal `sha256`.
   Neither the body nor the trailer is passed through a JSON parser.
2. Key. SHA-256 of the decoded `publicKey` must equal `keyFingerprint`, and
   `value` must verify with `publicKey`. This proves that the holder of that
   key signed this checksum.
3. Correct key. The displayed fingerprint is compared with the competition's
   fingerprint: the one shown on the Info page of the server holding the PEM,
   or the one photographed when the key was generated. OWLCMS marks a match
   with its own key; another key is reported with its fingerprint, never as
   trusted by itself.

Anyone can sign an edited file with their own key, so steps 1 and 2 alone
prove nothing; step 3 is what ties the export to the competition.

Verification requires reading and hashing the body bytes once, not parsing or
re-serializing them. A bounded tail buffer is sufficient to separate the
trailer while streaming. Import still performs its normal JSON parsing, but
verification adds no JSON parse. If no exact trailer is recognized, the
byte-only checker reports "signature absent or unrecognized", not success;
it does not parse the document to distinguish a missing signature from one
that has been reformatted or malformed.

- The `export.json` audit event and the import audit record this same checksum.
  Signed V2 records identify its scope as the original JSON body, rather than
  the complete signed file; the recorded byte length remains the file length.
  For an unsigned export, the rest of the JSON is the whole file, so the
  checksum is unchanged from today.
- An unknown top-level field is ignored by OWLCMS V2 import
  (`FAIL_ON_UNKNOWN_PROPERTIES` disabled) and by tracker-core `parseV2Database`,
  which reads known fields by name. Confirm with tracker-core when implementing.
- `TrackerDatabaseChecksum` already parses the document; it must also remove
  `signature`, which changes on every export.

Not included: a reference to the audit trail position (`auditHead`). The
signature stands on its own; the `export.json` event already links the export
to the trail when the trail is available.

## Validate export

Administration gets a "Validate export" action, next to the audit integrity
check. The user uploads an export; nothing is loaded and the competition is not
touched. The page shows:

- checksum: matches, does not match, or unavailable;
- signature: valid, invalid, or absent/unrecognized;
- fingerprint, in the four-digit groups used elsewhere, and whether it is this
  server's key.

The page and import use the same verifier. Any server can validate, with or
without a PEM, since the public key is in the file. The steps above are also
simple enough to reproduce outside OWLCMS (a few lines of Python or
`openssl`), for a federation that does not run OWLCMS.

## Import

Loading is never refused because of the signature. Record one outcome in the
audit trail, with the import SHA-256:

| Export | Outcome recorded |
| --- | --- |
| No recognized signature trailer | not authenticated, signature absent or unrecognized |
| Checksum or signature does not verify | invalid, claimed fingerprint |
| Valid, key is this server's PEM | valid, own key |
| Valid, another key | valid, foreign fingerprint |

Checking an export does not depend on the audit trail: the file carries its
own signature. The audit integrity check is not involved.

## Usage without a key

Accounts mode is commonly used without a PEM, with empty or trivial role
passwords, only to limit what each station shows; the admin password is kept
for support. Nothing changes for this usage: exports are not signed, and
support can recover admin access with its server's control panel token. This
works even when the imported admin password is unknown; removing its hash is
not required. Token login requires setting a new admin password and restarting.

## Support procedures

Support never works on the competition server. Support may need to edit the
JSON programmatically, not only through OWLCMS.

The distinction is how the PEM is shared. In A, on-site support receives a
local copy on removable media. In B, sending the PEM by email, messaging or
other remote channels is considered undesirable, so support receives only the
export and the receiving party keeps responsibility for vetting and signing.

In both procedures:

- Validate the untouched original against the independently obtained competition
  fingerprint, retain it, and edit a working copy. Keep a description of the
  changes with the edited copy.
- Editing the body invalidates its original signature. If the exact trailer is
  retained, verification reports a checksum mismatch; if the script removes or
  reformats the trailer, it may report signature absent or unrecognized instead.
  Recomputing the checksum alone cannot restore authenticity.
- Loading the edited copy is intentional and allowed. Its import audit records
  the verification outcome, not a valid original signature. Edits performed by
  a script are not individually recorded as OWLCMS audit events.
- Support's OWLCMS instance must not forward to live destinations. Admin
  recovery uses the local server's control panel token, followed by a new
  password and restart.
- Retain an operator-side backup before replacing the competition. A new
  signature authenticates the accepted state, not the history or correctness
  of the repair.

Neither editing nor signature verification requires the private key. The key
is needed to produce a new competition-key signature and to decrypt forwarding
credentials encrypted under it.

### A. On-site support uses a copied key

Support works on a separate computer, never on the competition server. The
operator provides the export and copies the competition PEM onto a removable
disk for support to use on its own OWLCMS instance.

1. Support retains and validates an original export, makes any required
   programmatic edits to a working copy, and checks the proposed repair.
2. Support configures its own OWLCMS instance with the copied PEM and reloads
   the edited JSON there. The edited copy is imported as unauthenticated, as
   described above. If admin recovery is needed, support uses its own instance's
   control panel token and completes the password change and restart.
3. Support checks the loaded state and generates a fresh OWLCMS export, signed
   with the copied competition PEM, and returns it to the operator. Unchanged
   encrypted forwarding credentials remain usable.
4. The operator validates the returned export against the competition
   fingerprint and imports it on the competition server. If support changed
   the admin password, the operator resets it with the competition server's
   own control panel token and completes the restart.
5. Support's edited import, any admin recovery, in-application changes and
   re-export are recorded in support's audit files. The competition server
   records the operator's import of the signed return. Sharing the signing key
   does not merge these trails; support supplies its logs separately if needed.
   The script's individual edits are described separately.

### B. Remote support; the receiving party vets and signs

The PEM is not sent to remote support. Support returns edited JSON, which the
receiving party vets before accepting it for signing with the locally held
competition PEM.

1. The operator sends the export only. Support validates the original using its
   embedded public key and compares the fingerprint supplied independently.
2. Support edits a working copy programmatically and returns that JSON with a
   description of the changes. Support does not import and re-export it through
   OWLCMS. The edited copy has no valid original signature; it is not presented
   as an authenticated competition export.
3. The receiving party vets the changes and approves the edited copy, then
   loads it on the server holding the competition PEM and checks the repair.
   Unchanged encrypted forwarding values are preserved verbatim in the edited
   JSON and can be decrypted there; support does not need to know or replace
   them.
4. The receiving party generates a fresh OWLCMS export, signed with the
   competition PEM. Its audit trail records the unauthenticated import and the
   new export. The new signature authenticates the accepted state, not the
   intervening support edits.

Vetting is a procedural prerequisite, not a new OWLCMS approval gate. It
precedes import on the PEM-equipped server because automatic checkpoints can
sign exports after import. If vetting requires a test import, use a separate
instance without a configured PEM.

## Acceptance

- With a PEM: exports carry a valid signature; editing any value, including
  `exportDate` or an `updateKey`, makes it invalid; removing `signature` makes
  it unsigned and validation reports signature absent or unrecognized.
- A server without a configured PEM produces unsigned exports (no
  `signature`).
- Validation needs only the export, not the signer's PEM: a signed export from
  another server is validated on any server, PEM or not, and shown with its
  fingerprint.
- Signing adds no parse or second pass: one serialization, bounded additional
  memory and one Ed25519 signature. Verification hashes raw bytes once without
  parsing JSON.
- Appended fields, duplicate trailer fields, a truncated trailer, or bytes
  after the trailer never produce successful validation.
- A content edit with a recomputed `sha256` is reported invalid.
- An edited export re-signed with another key and its own `publicKey` is
  reported valid with that other fingerprint, not as this server's key.
- A `publicKey` that does not match `keyFingerprint` is reported invalid.
- A content string containing `,"signature":` does not affect verification.
- Validate export shows each outcome without changing the database.
- Each import outcome in the table is recorded in the audit trail.
- Procedure A: support uses its own computer and the competition PEM copied
  via removable disk to reload edited JSON and re-export. Support's trail
  records the unauthenticated import and the new export; the competition server
  records the operator's import of the signed return.
- Procedure B: support returns edited JSON without re-exporting through OWLCMS.
  The receiving party vets it before loading it onto the signing server and
  re-exports with the competition PEM; its trail records the unauthenticated
  import and the new export. Unchanged encrypted forwarding values remain
  usable on the receiving server.
- Admin recovery works with the local control panel token without removing the
  imported admin hash, and requires a password change and restart.
