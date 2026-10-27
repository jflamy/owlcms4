# Protecting Connection Credentials in Exports

Status: implemented 2026-10-27 with a different key and scope than proposed
below; the sections that follow are the original proposal.

- PIN mode: forwarding update keys are stored in plain text in the database
  and in exports, so a competition prepared on one computer runs on another.
- Accounts mode: they are encrypted (same `enc:v1:` layout, AES-256-GCM) with
  a key derived by HKDF-SHA256 from the audit signing key
  (`AuditSigningKey.deriveSymmetricKey`, see `ForwardingKeyCipher`): the
  configured PEM key, or else the built-in key, whose private part is public,
  so that only hides the values (demos).
- Values are read whatever the mode. A value that cannot be decrypted with the
  current or built-in key is treated as unset, written as null on the next save
  or export, and must be entered again. Until the audit key is loaded at
  startup, stored values are kept unchanged.
- The installation key (`~/.owlcms/key`) is not used for forwarding keys; it
  only decrypts development API keys (Google Sheets).
This document concerns **what an export contains**, not who may download it.
URL authorization is in [020](020-TOKEN_PROTECTED_URLS.md); checkpoint collection
is in [050](050-SCHEDULED_CHECKPOINTS.md).

## Required behavior

Recovery exports must retain connection settings faithfully without exposing
forwarding shared secrets in cleartext.

- Protect `forwardingDestinations[].updateKey` and legacy public-results/video
  update keys in JSON V1, V2 and `competition.json` inside `database_zip`.
- Keep URLs, user names, activation/control-panel flags, order and null/empty
  distinctions. Existing account/PIN/MQTT password-verification hashes stay
  unchanged. This is not anonymization or protection of the complete export.
- Encrypt new or explicitly edited keys on save, including new/changed managed
  environment keys. Accept legacy plaintext without migrating it merely on
  load, unrelated saves or legacy-schema conversion.
- Every export encrypts plaintext in a detached view and preserves valid
  ciphertext byte-for-byte. Never modify Config/database values during export.
- Runtime resolution decrypts only into an in-memory connection view.
  Tracker still authenticates with the original shared key; neither its
  `OWLCMS_UPDATEKEY` comparison nor the WebSocket wire protocol changes.

## Encoding and key

Reuse the existing InstallationSecret/Google API-key encoding exactly:

```text
enc:v1:<key-fingerprint>:<base64-nonce-ciphertext-and-tag>
```

- AES-256-GCM; fresh 96-bit random nonce; 128-bit authentication tag; UTF-8.
- Fingerprint: first 8 hexadecimal characters of SHA-256 of the 32-byte key.
- Base64 contains nonce, ciphertext and tag in that order. No added derivation
  or additional authenticated data under the existing `enc:v1` marker.
- Reuse the installation key, normally `~/.owlcms/key`, retaining legacy
  `~/.owlcms` file support. Keep it outside the database and exports, backed up
  independently. Never derive it from the forwarding key.
- Null/absent, plaintext and supported ciphertext are distinct cases.
  Foreign fingerprints, malformed reserved `enc:` values, unsupported versions
  or authentication failures are errors, never plaintext fallbacks.
- Never log plaintext, ciphertext or key bytes; safe diagnostics may identify
  the encoding version and fingerprint.

Hashing cannot replace encryption here: OWLCMS must recover the original
forwarding secret. Personal account tokens use a verifier hash for a different
purpose and are not these connection keys.

## Import and recovery

1. Parse and validate all connection records and protected values, locate the
   matching key and authenticate/decrypt before destructive database changes.
2. If any required key/value is invalid, fail visibly without replacing data.
3. Accept unmarked plaintext from historical/hand-edited JSON and encrypt it
   on explicit import. Preserve valid imported ciphertext.
4. Do not silently clear credentials, keep the destination machine's current
   connections or perform a partial restore.

Older OWLCMS versions may mistake ciphertext for a literal credential.
The protected-value marker, not solely a format-version change, identifies it.
JSON layouts and field names remain compatible for consumers not using keys.

### Lost recovery key

An operator may deliberately sanitize a **copy**, preserving the original:

- Replace every unavailable encrypted connection key with a known plaintext
  replacement, null, or remove its connection. No undecryptable value may remain.
- Keep URL/activation state if keyless forwarding is intended. It works only
  when effective OWLCMS runtime overrides and Tracker's expected update key
  also permit keyless operation; this does not bypass authentication.
- Do not reset independent account/PIN/MQTT hashes unless explicitly intended.
- Record original and derivative filenames and exact JSON SHA-256 values,
  reason, changed field classes (not values), operator and UTC time.
  Never present the derivative as the original checkpoint.
- Repackaging unchanged JSON leaves its JSON digest unchanged; editing it does
  not. Loading does not require an embedded checksum. An offline sanitizer is
  optional, not a prerequisite.

## Integration and acceptance

Use one protected-value parser and detached export adapter across V1, V2,
manual downloads and forwarded ZIPs. Tracker treats encrypted Config values
as opaque and does not need the recovery key.

Verify:

- Faithful round trips with the matching key, including inactive destinations,
  legacy fields and null/empty keys.
- Fresh ciphertext for plaintext on each export; valid ciphertext is not
  double-encrypted; neither export nor unrelated save mutates legacy data.
- All export paths protect keys; runtime forwarding sends the original secret.
- Foreign/missing keys, bad Base64/tags/versions and invalid connection records
  fail before data replacement.
- Existing decoder compatibility and plaintext/keyless recovery derivatives.
- No credentials in logs, recovery keys in exports, or plaintext write-back.

The export remains sensitive competition/account data. Encryption does not
protect a compromised process/host or historical plaintext files.
