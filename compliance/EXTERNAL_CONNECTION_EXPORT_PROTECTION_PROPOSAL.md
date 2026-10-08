# External Connection Information in Recovery Exports

Status: proposal only, 2026-10-02. No implementation changes are authorized by this document.

Related: [AUDIT_INTEGRITY_SPECIFICATION.md](AUDIT_INTEGRITY_SPECIFICATION.md).

## 1. Scope and Decision

Competition JSON exports are recovery checkpoints. They must remain faithful: restoring a checkpoint must restore the external connection configuration that existed when it was created, including endpoints, activation state, user names and credentials.

The immediate compliance concern is the content of the JSON export at rest. A JSON file, whether stored directly or as `competition.json` inside `database_zip`, must not reveal recoverable connection credentials in cleartext. ZIP compression and the transport used to deliver the ZIP do not change that requirement.

WebSocket framing, live frame authentication and transport security are not being redesigned by this proposal. Tracker is relevant because it may retain the received ZIP as a passive recovery checkpoint, but protection must already exist inside `competition.json` before transmission or storage.

The recommendation is therefore:

1. Keep external connection information in V1 and V2 recovery exports.
2. Encrypt newly entered or explicitly edited forwarding update keys on save, without automatically migrating existing plaintext database keys.
3. Always protect forwarding update keys at export using the existing Google Sheets API-key encoding: encrypt plaintext in a detached export view and preserve valid existing ciphertext.
4. Accept unmarked plaintext keys in JSON and existing databases, including deliberately hand-edited recovery files.
5. Require the matching recovery key for encrypted values and fail before changing the database if it is unavailable.
6. Use the same protected JSON representation whenever V2 is packaged as `database_zip`.

This is lossless protection, not anonymization. A checkpoint plus its recovery key restores the exact configuration. A checkpoint without that key cannot activate the protected connections faithfully, but a deliberate break-glass sanitization may remove those credentials so the remaining competition data can be recovered.

Export protection is mandatory and independent of database migration. The database may contain encrypted or legacy plaintext keys. Every export passes through a protection step that encrypts plaintext in a detached view and preserves valid existing ciphertext without changing Config or the database. Explicit new or edited keys are encrypted on save, but loading existing data, saving unrelated configuration or migrating the legacy connection schema must not automatically rewrite plaintext keys. OWLCMS resolves either representation to plaintext only for runtime use. Tracker continues to compare the received original key against `OWLCMS_UPDATEKEY` from its environment. Neither Tracker authentication nor the WebSocket wire value changes.

## 2. Current State

Both export formats serialize the live `Config` object:

- `CompetitionData` includes `Config` in JSON V1.
- `CompetitionDataV2` includes `Config` in JSON V2.
- `ForwarderPayloadBuilder.exportCompetitionData` builds the same JSON V2 structure for external forwarding.
- `WebSocketEventForwarder.sendDatabase` compresses that structure as `database_zip` and sends it to each WebSocket destination.

The serialized configuration can contain:

- `forwardingDestinations[].url`;
- `forwardingDestinations[].updateKey`;
- each destination's `active` and `controlPanelManaged` state;
- MQTT port, user name and password; and
- legacy public-results and video-data URLs and update keys in older exports.

The forwarding URL and its update key are paired in `ForwardingConnection`. The update key is an authentication credential. A forwarded database currently gives a destination the exported configuration for that destination and potentially every other destination.

WSS provides confidentiality and integrity in transit when certificate validation succeeds. It does not make credentials safe to retain in the resulting export. Plain `ws://` remains supported and provides no transport confidentiality.

### 2.1 Current at-rest assessment

The current JSON export, whether stored directly or packaged inside a ZIP, does not provide sufficient at-rest protection for forwarding credentials:

| Value | Current exported representation | Direct cleartext disclosure |
| --- | --- | --- |
| Account password | Salted PBKDF2-SHA256 hash | No, although the export permits offline password guessing |
| Officials/display PIN | Salted legacy SHA-256 representation | No, although short PINs may be guessed offline |
| Database-backed MQTT password | Salted legacy SHA-256 representation | No, although it is not recoverable as plaintext |
| Forwarding `updateKey` | Literal string in `forwardingDestinations` | **Yes** |
| Legacy public-results/video update key | Literal string when present in historical JSON | **Yes** |
| Forwarding URL and MQTT user name | Literal string | Yes; these reveal connection topology and identity rather than a password |

Salted hashing is appropriate where OWLCMS only needs to verify a candidate password. It cannot satisfy faithful recovery of a forwarding key because OWLCMS must recover that shared secret and send it to Tracker for WebSocket authentication. Therefore, exported forwarding keys require reversible authenticated encryption regardless of their database representation; replacing them with salted hashes would make a restored connection unusable.

The ZIP container adds compression, not encryption. At-rest protection must already be present in the JSON values before `DatabaseZipHelper` creates the ZIP. With the proposed `enc:v1` values and a separately retained recovery key, an untouched Tracker copy of the ZIP does not reveal forwarding cleartext. Without that implementation, the current ZIP does reveal it.

## 3. Recovery Requirement

Protection must not weaken the checkpoint semantics:

- no connection field may be replaced with `null`, a mask or a one-way hash;
- order, URL, key, active state and control-panel ownership must round-trip;
- MQTT connection fields must round-trip;
- null and empty values must remain distinguishable where the current model distinguishes them;
- export protection must operate on a detached view and must not replace keys on the singleton `Config` or in the database; and
- a successful import must either restore all protected connection information or restore none of it.

The recovery key is part of the recovery procedure but must not be embedded in the export. It must be retained independently, with access controls appropriate to a credential capable of recovering all protected connection values.

## 4. Protected Value Encoding

Use the recognizable value format already established by `InstallationSecret` and the control panel:

```text
enc:v1:<key-fingerprint>:<base64-nonce-ciphertext-and-tag>
```

For version 1:

- encryption is AES-256-GCM;
- every newly encrypted value receives a fresh 96-bit random nonce;
- the authentication tag is 128 bits;
- the fingerprint is the first 8 hexadecimal characters of SHA-256 of the 32-byte recovery key, matching the existing convention; and
- UTF-8 is used before encryption and after decryption.

The prefix identifies the encoding, not merely that a value happens to look encrypted. The fingerprint selects the required recovery key and allows a clear diagnostic before attempting decryption. It is not secret and is not an authentication substitute.

Use the existing encoding exactly: the Base64 payload is the nonce followed by ciphertext and authentication tag, encrypted with the installation key without additional authenticated data. Do not add a derived key or field-specific context under the same `enc:v1` marker: the Google API-key decoder would no longer be able to decode those values. A different cryptographic convention would require its own version and is outside this decision.

A value without an encryption marker is a plaintext key. A supported marker requires successful decryption; a malformed marker, unsupported encoding version or failed decryption must never fall back to treating the ciphertext as plaintext. The existing Google API-key helper recognizes `enc:v1:` specifically; rejecting other reserved `enc:` versions must be handled explicitly by the connection-key resolver.

The marker is an encoding discriminator, not a digital signature. AES-GCM authenticates the encrypted value. Every export protects every populated key: plaintext is encrypted with a fresh nonce, while an existing valid encrypted value is preserved byte-for-byte without double encryption. Repeated exports of a legacy plaintext key therefore produce different ciphertext without modifying the database.

Do not replace the forwarding key with a one-way hash in live configuration. OWLCMS must recover and transmit the exact shared secret, so a password-verification hash is unsuitable. Sending the hash itself as the WebSocket credential would merely make that hash a reusable bearer secret, break existing Tracker configuration and still allow anyone reading the export to authenticate with the copied value.

Do not send the randomized `enc:v1` ciphertext as the WebSocket credential. Tracker is configured with the shared plaintext value, and a fresh nonce means repeated encryptions intentionally differ. Authentication continues to use the decrypted runtime value.

## 5. Fields to Protect

Protect forwarding shared secrets. This decision does not conceal connection topology or change existing password-verification hashes:

| Configuration surface | Protected values | Values left structured |
| --- | --- | --- |
| Forwarding destinations | `updateKey` | `url`, `active`, `controlPanelManaged`, list order |
| MQTT | Existing password hash remains unchanged | user name, port, internal/enabled flags |
| Legacy forwarding fields | public-results and video-data update keys, protected during export | URLs, field names and null state |

URLs and user names remain readable. Encrypting those fields would be a separate scope decision. Tracker does not use exported account password hashes for authentication; it uses the separately configured update key.

Account password hashes, officials/display PIN hashes, allowlists and backdoor settings are security-sensitive but are outside this narrowly scoped proposal. They require a separate review; protecting connection fields must not be presented as protection of the complete export.

## 6. Key Management and Faithful Recovery

Reuse the existing 32-byte installation key and `enc:v1` implementation, including its fingerprint convention. The key lives outside the database and exports, normally at `~/.owlcms/key`; the existing legacy `~/.owlcms` file location remains supported. Do not derive the encryption key from the forwarding update key or embed it in a checkpoint.

For a checkpoint to support machine-loss recovery, the operator must retain the installation/recovery key separately from the competition export. Copying only the JSON file is not a complete recovery set. The control panel should provide a documented backup and restore procedure for the key without printing it in logs or placing it in the export directory.

Recovery behavior:

1. Scan the complete export for protected values and collect distinct fingerprints.
2. Resolve the required key before any database mutation.
3. Authenticate and decrypt each marked key for validation without replacing its ciphertext in the stored import model; accept unmarked plaintext keys.
4. Validate connection records and prepare plaintext keys for encrypted persistence using the existing encoding convention.
5. Only then begin replacement of the current competition data.

The current V1 and V2 restore paths call `removeAll()` before import and configuration validation. That ordering is incompatible with encrypted recovery checkpoints. Import parsing, key resolution, decryption and validation must become a preflight phase before `removeAll()` or any other destructive action.

Missing keys for encrypted values, foreign fingerprints, malformed Base64, failed GCM authentication, unsupported encoding versions and invalid connection records are fatal recovery errors. They must not silently clear imported values, retain the machine's current connections, treat ciphertext as a literal key or partially restore the competition.

### 6.1 Lost-key break-glass recovery

If every copy of the matching AES recovery key is lost, an operator can knowingly create a sanitized derivative of the checkpoint:

1. Preserve the original JSON or ZIP unchanged as evidence.
2. For a direct JSON export, edit a copy of that JSON. For `database_zip`, extract `competition.json` and edit a copy.
3. Replace every `enc:v1` connection credential with a known replacement plaintext key, with `null`, or remove the complete affected connection record.
4. No encrypted key may remain unless its matching installation key is available. URLs and user names do not need to be removed under this protection scope.
5. Optionally set `config.pin`, `config.displayPin`, `config.mqttPassword` and each `accounts[].passwordHash` to `null` only when the operator intends to reset those independent authentication mechanisms too. Their existing one-way hashes do not require the AES key for ordinary restoration.
6. Save a direct JSON derivative, or, for `database_zip`, repackage the edited `competition.json` in a new ZIP.
7. Re-enter any cleared credentials through the normal configuration interfaces after recovery. A replacement plaintext key supplied in the JSON is accepted and encrypted on persistence.

This maneuver is structurally possible in the current model:

- `ForwardingConnection.updateKey` is nullable and a destination without a key sends unauthenticated frames;
- `Config` PIN and MQTT password fields accept null;
- `UserAccount.passwordHash` is nullable and an account without a password cannot log in; and
- V1 and V2 import replace accounts only when an `accounts` array is present. Omitting `accounts` preserves the installation's existing account set, while retaining the array with null hashes restores identities and grants without usable passwords.

Intentional key removal is also a supported way to restore keyless forwarding: keep a connection's URL and activation state, remove its `updateKey` property or set it to `null`, then reimport the JSON. Provided no remaining encrypted value requires the lost installation key, the import can proceed without that key and OWLCMS sends frames without an update key. Removing the whole connection record instead removes that destination; it is not the same as keeping a keyless connection.

Tracker must also be configured for keyless operation: `OWLCMS_UPDATEKEY` must be unset or blank. Removing a key from the JSON does not bypass a Tracker that still requires its configured shared secret. Environment or control-panel keys can override imported settings, so the effective OWLCMS connection key must also be absent. This is a deliberate authentication change, not a silent fallback after decryption failure.

The current JSON import path does not enforce an embedded checksum. The archival checksum shall be SHA-256 of the exact JSON bytes, stored in a separate inventory or sidecar, never embedded in the JSON itself. Nothing is excluded from checksum computation. Editing the JSON changes its hash, as intended; repackaging unchanged JSON in a different ZIP does not. A future signed audit record can authenticate that separately retained hash.

The sanitized derivative must receive a new SHA-256 and an explicit manifest entry containing:

- the original checkpoint name and SHA-256;
- the sanitized artifact name and SHA-256;
- the reason (`recovery key lost`);
- the classes of fields removed or replaced, never their credential values; and
- operator and UTC creation time when available.

It must never be presented as the original faithful checkpoint or be given the original checksum. Manual replacement with a known plaintext key remains supported for backward compatibility. A future offline sanitizer could automate credential removal and provenance, but is not required to recognize plaintext keys.

## 7. Backward Compatibility

Backward compatibility is intentionally asymmetric:

- new OWLCMS versions accept unmarked plaintext keys in historical or hand-edited JSON and in existing databases;
- new OWLCMS versions accept `enc:v1` values when the matching key is available;
- null and absent legacy fields retain their existing migration meaning; and
- V1 legacy URL/key pairs migrate into `ForwardingConnection` records without changing the key representation merely because the schema changes.

The runtime resolver returns an unmarked key unchanged, just as the Google API-key helper does. Plaintext found in a database remains usable and is not automatically migrated on load, on unrelated configuration saves or during legacy connection-schema migration. New or explicitly edited keys are encrypted on save. JSON import accepts plaintext and stores it encrypted as part of that explicit import. There is no automatic rewrite of the source JSON file.

Export always encrypts plaintext in its detached view, even when the database continues to hold the legacy plaintext value. Exporting must not persist this conversion back into the database.

Editing a copy of an export to replace an encrypted key with the known cleartext shared key therefore works without the original recovery key, provided no other protected value still requires that key. It is a deliberate replacement of a credential, not decryption of the old one. Any checksum or signature over the original artifact no longer matches the edited derivative.

An older OWLCMS version cannot safely restore a newly protected export because it would treat ciphertext as a literal credential. A deliberately edited derivative with plaintext keys can be used where the older schema is otherwise compatible, but exposes those keys and must not be described as a protected export.

The top-level V1 and V2 object layouts and existing field names remain unchanged. The marker is carried as a string value, so JSON parsers and external receivers that do not use connection settings can continue to process the rest of the export. If the implementation changes the V2 `formatVersion`, it should be a minor format revision and must not be the sole means of recognizing protected values.

## 8. Delivery and Passive Tracker Retention

Any producer of `database_zip` must use the same export-protection step as a manual JSON download, encrypting legacy plaintext keys in a detached view and preserving valid stored ciphertext. It must not serialize a runtime-decrypted connection view. Tracker may then retain the received ZIP without exposing the connection credentials embedded in `competition.json`.

The live WebSocket authentication value is outside this at-rest control. OWLCMS must decrypt a restored connection key into its in-memory runtime configuration before opening a connection, but no change to frame authentication is required here.

No WebSocket frame-format or protocol-version change is required. The only relevant payload change is inside `competition.json`: forwarding update keys use the `enc:v1` representation. Existing `tracker-core` parsing treats `db.config` as data and does not need to decrypt it.

Delivery requirements relevant to the stored JSON are:

- do not decrypt protected export fields at the receiving tracker merely to cache or display competition data;
- never include the recovery key in a WebSocket header, query parameter, JSON message or ZIP member;
- do not log plaintext or ciphertext connection values; and
- treat a captured or retained `database_zip` as a sensitive recovery artifact even though its connection fields are encrypted, because competition and account data remain present.

`tracker-core` should treat protected strings under `db.config` as opaque and must never attempt to use them as connection credentials. Other WebSocket authentication hardening is a separate concern.

### 8.1 Tracker as a passive recovery checkpoint

Tracker may act as an independent passive backup by periodically requesting and retaining the database received from OWLCMS. Such a copy is a recovery checkpoint, not a projection of scoreboard state, and must be faithful to the OWLCMS export.

For `database_zip`, `tracker-core` receives the exact ZIP payload after the WebSocket layer has authenticated the frame and removed the outer key trailer. It must persist those ZIP bytes unchanged before or independently of JSON parsing. The outer trailer is transport authentication metadata and is not part of the recovery export. The `competition.json` inside the retained ZIP is the complete checkpoint, including opaque `enc:v1` connection values.

Tracker must not create a recovery checkpoint by serializing `competitionHub.databaseState`. The V2 parser normalizes entities for scoreboard use and does not preserve every top-level object or every original representation. Re-serialization would therefore produce a display-oriented derivative, not a faithful recovery file.

The passive backup flow should be:

1. Tracker requests a fresh `database_zip` through the existing resource-request mechanism at the configured checkpoint interval.
2. OWLCMS builds one V2 export through the mandatory key-protection step, compresses it and sends it in an authenticated WebSocket frame.
3. `tracker-core` validates the outer update key and protocol framing, removes only the authentication trailer and identifies the payload as `database_zip`.
4. Tracker writes the unchanged ZIP payload to a temporary file, extracts the raw `competition.json` bytes and computes their SHA-256 and byte length before JSON parsing or reserialization.
5. Tracker validates that the ZIP is readable and contains a parseable `competition.json`, then atomically publishes the unchanged ZIP under a timestamped or export-ID name with its separate JSON-checksum inventory entry.
6. The same payload may then be parsed into normalized hub state for scoreboard use. Parsing success must not modify the stored checkpoint bytes.

If OWLCMS supplies an export ID, export timestamp, cache identifier or audit boundary, retain it in a separate checkpoint inventory rather than editing the ZIP. Tracker-generated receipt time, source identity, JSON byte length and archival JSON SHA-256 also belong in that inventory. If the live `databaseChecksum` is retained, label it separately as a cache identifier, never as the archival JSON hash. A failed or partial transfer must never replace the latest completed checkpoint.

The archival digest is of the complete JSON byte sequence, not the ZIP and not a parsed/reformatted object. Do not embed it in `competition.json` or omit any field from hashing. Standard SHA-256 utilities can reproduce it from the direct JSON file or from the raw extracted ZIP member. The current WebSocket `databaseChecksum` hashes JSON before `DatabaseZipHelper` reserializes it, so it must not be assumed to equal the archival JSON hash.

For a legacy text `database` message or raw-JSON binary fallback, retain the exact authenticated payload bytes in their received JSON form. Prefer requesting `database_zip` for periodic checkpoints because it gives one unambiguous artifact and avoids JSON parse/re-serialization changes.

Tracker does not need the recovery key and must not decrypt protected connection fields. Recovery consists of supplying the retained OWLCMS artifact and the independently retained matching recovery key to OWLCMS. Retention count, storage location, access control and deletion policy are deployment concerns that must be configured outside the WebSocket protocol.

Transport protection and export protection are complementary. Neither replaces destination authentication, certificate validation, file access control, retention limits or secure deletion.

### 8.2 Live Cache Identification vs Archival Integrity

These are separate mechanisms, even when both values happen to be computed using SHA-256:

| Aspect | Live `databaseChecksum` | Archival JSON SHA-256 |
| --- | --- | --- |
| Purpose | Cache busting, freshness identification and duplicate-load avoidance | Later verification of the exact retained JSON bytes |
| Input | Currently, OWLCMS JSON serialization before ZIP reserialization | The complete raw JSON file or extracted `competition.json` bytes |
| Handling | Tracker can compare the supplied identifier with its cached identifier; it does not recompute a payload digest to validate it | The archiver computes the hash from the retained JSON bytes; later verification recomputes and compares it |
| Required | Optional for live ingestion; database payloads without it are accepted | Required in the separate inventory or sidecar for an archived checkpoint |
| Security role | Neither source authentication nor an archival integrity check | Detects byte changes against a trusted reference; authenticity requires protecting or signing that reference |

The live feed relies on its configured OWLCMS authentication and transport protections, not on `databaseChecksum`. An intentionally keyless connection is not shared-key authenticated. This proposal does not replace or remove the existing live cache mechanism, and adding archival hashing does not make a checksum mandatory for live ingestion.

Do not substitute the live cache identifier for the archival digest, compare one against the other, or embed the archival digest in the JSON. Keep their names and roles distinct in inventories and diagnostics. An intentionally edited recovery derivative receives a new archival JSON hash; any later live cache identifier remains independently generated by OWLCMS.

## 9. Implementation Boundaries

Separate stored keys, runtime-resolved keys and export-protected keys. Forwarding destination resolution decrypts into a runtime-only view; it must not write plaintext back into Config. The editor encrypts new or explicitly changed plaintext keys without double-encrypting marked values or converting untouched legacy plaintext keys on an unrelated save.

Reuse the Google API-key secret utility and extend it with compatible encryption as needed. Changes belong in explicit connection-key writes, runtime resolution, editor handling, import preflight and a shared detached export adapter. Existing string columns and JSON field names can remain unchanged. New or changed keys copied from environment variables into managed database connections must also be encrypted before persistence; unchanged legacy values are not automatically migrated.

Environment-provided keys remain accepted as plaintext or supported encrypted values at the runtime boundary. `WebSocketEventSender`, the binary authentication trailer and Tracker comparison continue to receive the original plaintext shared key. V1, V2 and forwarded `database_zip` exports must all invoke the shared protection step. It encrypts unmarked plaintext and preserves valid ciphertext without double encryption; exports must never expose a decrypted getter or mutate stored keys.

Centralize protected-string parsing so V1, V2, manual downloads and WSS startup exports use identical logic. The parser must classify a value as one of:

- absent/null;
- legacy plaintext;
- supported protected value;
- protected value for a different key;
- malformed protected value; or
- unsupported protected version.

Only the first three classifications can proceed automatically. Diagnostics may include the encoding version and fingerprint, but never plaintext, ciphertext or key bytes.

## 10. Direct Export Endpoint Access

The direct export servlets currently authorize a request when `AccessUtils.isLocalNetwork(clientIp)` or `AccessUtils.checkBackdoor(clientIp)` is true. They do not require an authenticated account, PIN or role because plain servlet requests do not pass through the Vaadin `AccessControlListener`.

Broad local-network access is no longer acceptable for recovery exports. Private RFC 1918 addresses, link-local addresses and IPv6 unique-local addresses must not be authorized merely because they are local-network ranges.

This rule also applies when OWLCMS is deployed on a physically or logically closed competition network. "Local" means the server's actual loopback address, not every address on the closed LAN. Other devices on that network remain unauthorized unless their exact address is explicitly configured in the backdoor list.

The intended authorized sources are:

1. the OWLCMS server itself, using an actual loopback socket peer for control-panel operations; and
2. the Competition Director laptop, using an exact address configured in the backdoor list.

Apply this rule to:

- `/competition/export`;
- `/competition/export/json/1`;
- `/competition/export/json/2`; and
- `/competition/export/sbde`.

Authorization must use the socket peer address by default. `X-Forwarded-For` or another forwarded-client header may be used only when the immediate socket peer is a configured trusted reverse proxy. A directly connected requester must not be able to become loopback or match a backdoor entry by supplying its own forwarding header.

### 10.1 Known proxy definition

A known proxy is an immediate socket peer covered by deployment-controlled trust configuration and prevented by network policy from being impersonated. It is not inferred merely because OWLCMS is running in a cloud environment or because a forwarding header is present.

Supported trust sources should be:

- loopback addresses when a reverse proxy such as Caddy or Nginx runs on the same VPS;
- exact proxy addresses or narrowly scoped proxy CIDRs configured by the operator when the proxy runs on another host; and
- a documented platform-proxy mode when the cloud platform guarantees that its edge removes or overwrites the selected client-IP header and the OWLCMS service is not directly reachable around that edge.

Do not automatically trust every private-network address, the complete cloud private network, DNS names resolved at request time or arbitrary `X-Forwarded-For` senders. An explicit trusted-proxy CIDR is acceptable only when the deployment operator controls that range and firewall/service configuration prevents untrusted workloads from connecting as peers within it.

For a local VPS proxy, the OWLCMS backend port must be bound to loopback or firewalled from external access. For a cloud edge, direct access to the application listener must be disabled or restricted to the platform proxy. Header trust without that network restriction is insufficient.

#### DigitalOcean deployments

A DigitalOcean Cloud Firewall by itself is a network-based stateful firewall. It does not proxy HTTP and does not add `X-Forwarded-For`; a directly exposed Droplet application sees the client as its socket peer and should ignore forwarding headers.

A DigitalOcean HTTP/HTTPS Load Balancer is an HTTP proxy. DigitalOcean documents that it sets `X-Forwarded-For`, `X-Forwarded-Proto` and `X-Forwarded-Port`. Its backend source addresses are dynamic and DigitalOcean advises against using those dynamic addresses as firewall identifiers, so a static list of observed load-balancer IPs is not a stable trust configuration.

Support this case through an explicit DigitalOcean Load Balancer proxy mode, enabled only when deployment networking guarantees that the OWLCMS backend listener cannot be reached directly or by untrusted VPC workloads. In that mode, use DigitalOcean's overwritten `X-Forwarded-For` value according to its documented convention. The trust assertion is the combination of explicit mode and isolated backend path, not the header alone.

If a DigitalOcean network load balancer preserves the client source address, use the socket peer and do not enable HTTP-header trust. If TCP/SSL passthrough with PROXY protocol is used, Jetty must be explicitly configured to parse that protocol; `X-Forwarded-For` handling is not a substitute.

#### Fly.io Docker/Machine deployments

On Fly.io, the OWLCMS Docker image runs inside a Fly Machine and listens on the `internal_port` configured in `fly.toml`. Public HTTP traffic reaches that port through Fly Proxy. With `[http_service]` or an HTTP handler, Fly Proxy adds `Fly-Client-IP` and `X-Forwarded-For`; the servlet socket peer is Fly's internal proxy path rather than the public client.

Support this case through an explicit Fly proxy mode. When there is no additional reverse proxy in front of Fly.io, use `Fly-Client-IP` as the effective client address. Fly documents it as the address from which Fly Proxy accepted the connection and recommends it over parsing `X-Forwarded-For` in that topology. If another proxy or CDN sits in front of Fly.io, `Fly-Client-IP` identifies that proxy and the full `X-Forwarded-For` chain requires separate trusted-hop configuration.

Fly's public service edge is default-deny except for services exposed in `fly.toml`, but private 6PN/Flycast paths must also be considered. Fly proxy mode is valid only when untrusted applications cannot connect directly to the OWLCMS internal listener and supply a forged `Fly-Client-IP`. The Docker image itself is not the trust signal; explicit Fly mode plus the deployment's network path is.

### 10.2 Client-address resolution

Resolve the authorization address as follows:

1. Read the immediate peer from `request.getRemoteAddr()`.
2. If that peer is not trusted, ignore all forwarding headers and use the peer address.
3. If the peer is trusted, parse only the configured platform/header convention.
4. For `X-Forwarded-For`, walk addresses from right to left, removing trusted proxy hops; the first remaining valid literal IP is the client. Reject malformed, empty or excessively long chains.
5. Record whether the effective client came through a proxy.

Do not authorize on the socket peer's loopback status when a valid trusted-proxy header identifies an external client. Otherwise, a local Caddy/Nginx process would make every Internet request appear to come from localhost.

The resulting gate is:

```text
direct, non-proxied socket peer is loopback
OR
effective client address exactly matches a backdoor entry
```

The first branch uses `request.getRemoteAddr()` and applies only when no trusted proxy supplied an external client. The second branch may use a forwarded client address only after trusted-proxy validation. Backdoor matching remains exact; CIDR or private-network expansion is not implied.

This remains host-based authorization rather than user authentication. Adding account/role authentication to these servlet endpoints is a separate decision.

## 11. Proposed Requirements

- **ECX-01** V1 and V2 recovery exports shall retain all external connection information losslessly.
- **ECX-02** Every export shall protect forwarding update keys, encrypting plaintext in a detached view and preserving valid ciphertext. New or explicitly edited database keys shall be encrypted without automatically migrating existing plaintext keys. URLs, user names and existing password hashes remain unchanged.
- **ECX-03** Protected strings shall identify their encoding version and recovery-key fingerprint.
- **ECX-04** New encryption shall use a fresh nonce and the existing Google API-key `enc:v1` convention without changing its key or authenticated-context rules.
- **ECX-05** The recovery key shall be retained independently and shall never be included in an export or forwarded payload.
- **ECX-06** Import and runtime resolution shall accept unmarked plaintext keys, including hand-edited JSON and plaintext found in existing databases; explicit import or key edits shall encrypt them, while unrelated saves shall preserve untouched legacy plaintext.
- **ECX-07** Legacy V1 forwarding schema migration shall preserve the key representation without automatic plaintext-to-ciphertext migration; export protection shall still encrypt plaintext keys.
- **ECX-08** Import shall preflight and validate every protected value before deleting or replacing current data.
- **ECX-09** A missing or incorrect recovery key shall fail the whole import visibly without changing current data.
- **ECX-10** WSS database forwarding shall carry the protected export representation.
- **ECX-11** Export shall neither mutate stored keys nor expose runtime-decrypted keys; plaintext resolution shall use a runtime-only view.
- **ECX-12** Logs and diagnostics shall not disclose protected values or recovery-key material.
- **ECX-13** A tracker recovery checkpoint shall preserve the exact authenticated `database_zip` payload bytes and shall not be regenerated from normalized hub state.
- **ECX-14** Tracker shall publish a checkpoint atomically only after transfer, ZIP and JSON validation succeed.
- **ECX-15** Tracker shall record the exact JSON byte length and SHA-256 in a separate inventory or sidecar. The hash shall cover the complete JSON bytes, not the ZIP or a reserialized object, and shall never be embedded in the JSON or require excluded fields.
- **ECX-16** Tracker shall be able to request fresh checkpoints periodically through the existing resource-request mechanism without changing WebSocket authentication semantics.
- **ECX-17** A lost-key recovery tool shall be able to create a credential-free JSON derivative, directly or within `database_zip`, without requiring the lost key and while preserving the original artifact unchanged.
- **ECX-18** A sanitized derivative shall receive a new digest and provenance record and shall never reuse or claim the original checksum.
- **ECX-19** Import shall accept explicit null or absent optional credentials without treating them as undecryptable protected values. Retaining a destination with its key removed shall permit keyless forwarding when no runtime key override exists and Tracker has no expected update key; it shall not bypass a Tracker that requires a key.
- **ECX-20** Competition export servlets shall not authorize a request solely because its client address belongs to a private, link-local or unique-local network.
- **ECX-21** Competition export servlets shall authorize an actual loopback socket peer or an exact configured backdoor address.
- **ECX-22** A forwarded client address shall affect export authorization only when the immediate socket peer is a configured trusted proxy.
- **ECX-23** A requester directly connected to OWLCMS shall not be able to alter export authorization with `X-Forwarded-For` or equivalent headers.
- **ECX-24** A request forwarded by a trusted loopback proxy shall not receive localhost authorization when its resolved client address is external.
- **ECX-25** Trusted-proxy configuration shall identify immediate peers by deployment-controlled addresses, CIDRs or an explicit documented platform mode and shall be paired with prevention of direct backend access.
- **ECX-26** Live `databaseChecksum` shall remain an optional cache/freshness identifier, distinct from the archival JSON SHA-256. Any retained live identifier shall be labeled separately and shall not satisfy archival verification or become a prerequisite for live ingestion.

## 12. Acceptance Tests

Cover both JSON V1 and V2, plus the WSS `database_zip` path:

1. A configuration containing multiple active/inactive destinations and MQTT settings round-trips exactly with the matching recovery key.
2. Repeated exports of a database plaintext key produce different ciphertext because nonces differ, while repeated exports of an already encrypted key preserve its ciphertext unchanged; neither changes the stored value.
3. Historical plaintext V1 and V2 fixtures, hand-edited JSON plaintext keys and plaintext found in existing databases remain usable. Explicit import and key edits encrypt plaintext; loading or saving unrelated configuration does not migrate untouched keys.
4. Legacy public-results/video-data fields migrate to the same forwarding destinations without changing plaintext or ciphertext key representations; subsequent exports protect both without mutating the database.
5. A foreign fingerprint fails before `removeAll()` and leaves the existing competition and configuration unchanged.
6. A changed ciphertext byte, malformed payload or unsupported `enc:v2` value fails atomically and is never treated as a plaintext key.
7. Exported JSON and decompressed WSS database ZIP contain no configured plaintext forwarding update key, including when the database still contains legacy plaintext. New or edited database keys are encrypted; untouched legacy keys, URLs, user names and existing password hashes remain unchanged.
8. Exporting does not change `Config.getCurrent()`, persisted configuration or active forwarders.
9. Null, empty and populated values retain their defined distinctions.
10. Logs contain only safe error categories, encoding versions and fingerprints.
11. A tracker-retained `database_zip` is byte-for-byte identical to the binary payload emitted by OWLCMS after removal of the outer authentication trailer.
12. Restoring the retained ZIP with the matching recovery key reproduces every exported connection field and the complete competition data set.
13. Serializing normalized `competitionHub.databaseState` is demonstrated not to be the checkpoint implementation.
14. Interrupted transfer, invalid ZIP or invalid `competition.json` leaves the previous completed checkpoint and inventory entry unchanged.
15. Periodic requests produce independently named checkpoints whose raw JSON byte lengths and SHA-256 values match the separate inventory and standard SHA-256 utility output.
16. With the AES key unavailable, sanitizing all `enc:v1` values in a direct JSON export or in `competition.json` produces an importable derivative whose competition data is preserved and whose external connections have no usable credentials.
17. Leaving one protected value in the derivative still requires its matching key and fails preflight without changing current data.
18. The sanitized derivative has a different SHA-256, retains the original unchanged and records both digests in its provenance manifest.
19. Loopback socket peers can access each competition export path.
20. An exact configured Competition Director backdoor address can access each competition export path.
21. Other clients on `10/8`, `172.16/12`, `192.168/16`, link-local and IPv6 unique-local networks receive HTTP 403.
22. An untrusted direct client sending `X-Forwarded-For: 127.0.0.1` or a configured backdoor address receives HTTP 403.
23. A configured trusted proxy can supply the Competition Director address and receives the same authorization result as a direct request from that address.
24. A local trusted proxy with an external client not in the backdoor list receives HTTP 403 even though its socket peer is loopback.
25. A local trusted proxy with the Competition Director client address receives access, while the same forwarding header from an untrusted peer receives HTTP 403.
26. Malformed, multi-valued and oversized forwarding chains fail closed and cannot produce loopback or backdoor authorization.
27. Ciphertext produced by the existing Google API-key/control-panel convention can be resolved as a connection key with the matching installation key, and newly encrypted connection keys remain compatible with that decoder.
28. Replacing an encrypted key in a JSON copy with a known plaintext shared key works without the original encryption key when no other protected values remain; the imported key is encrypted using the destination installation key.
29. Tracker configured with the original key in `OWLCMS_UPDATEKEY` accepts OWLCMS frames after runtime decryption; ciphertext is never sent as the authentication credential.
30. Removing a destination's `updateKey` property or setting it to `null`, while preserving its URL and activation state, permits import without the original installation key when no other encrypted values remain. With no runtime key override, OWLCMS sends keyless frames that a Tracker with unset or blank `OWLCMS_UPDATEKEY` accepts, while a Tracker with a configured expected key rejects them.
31. No archival checksum is embedded in the JSON. Repackaging identical JSON bytes leaves the archival hash unchanged; editing or removing a connection key changes it, and the derivative receives a new separately recorded JSON hash.
32. Live database ingestion still accepts payloads without `databaseChecksum`. Archival hashing uses the raw retained JSON bytes independently of any supplied cache identifier, and any inventory containing both values distinguishes them explicitly.

## 13. Residual Risks

Anyone holding both the checkpoint and recovery key can recover all protected connections. The key backup therefore requires separate access control and retention policy.

The export still contains personal competition data, account names and password hashes. Encrypting external connection values does not make the overall export suitable for public distribution.

Deliberately configuring Tracker without an expected update key allows any reachable sender to submit data without shared-key authentication. Recovery by key removal does not provide source authentication; network access controls and transport security remain separate concerns.

An attacker controlling the running OWLCMS process can access the installation key and connection plaintext during runtime decryption. This proposal protects exported keys and newly saved encrypted database keys when separated from the installation key; it does not claim to protect a compromised process or host. Untouched legacy database keys and historical or deliberately edited plaintext files remain readable. The mandatory export step protects their keys in new exports without rewriting those sources.