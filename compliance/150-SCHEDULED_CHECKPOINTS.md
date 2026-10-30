# Scheduled Recovery Checkpoints

Status: agreed 2026-10-17; not implemented. This is a Tracker storage/timer
feature using the existing authenticated WebSocket connection, not URL access.

## Decision

- OWLCMS already answers a `428` request for `["database"]` with a V2 export
  zipped as `competition.json` in `database_zip`. No new OWLCMS endpoint,
  account token or WebSocket protocol is needed for this mechanism.
- In tracker-core, retain the received ZIP bytes unchanged after transport
  authentication/trailer removal. Write to a temporary file and atomically
  rename after ZIP/JSON validation; incomplete transfers must not replace the
  latest completed copy. Keep `local/database/` out of reconnect purging.
- Never reconstruct a checkpoint from normalized `competitionHub.databaseState`.
- A singleton timer in the Tracker plugin's `helpers.data.js` requests
  `["database"]` through the hub resource callback every N minutes.
- When `databaseChecksum` differs from the last saved copy, atomically copy
  the retained ZIP to
  `competition_v2_<yyyy-MM-dd_HHhmmss>_<shortChecksum>.zip`.
  Ignore unchanged data. Capture unsolicited database updates the same way.
- Configure checkpoint folder and interval through `config.js` and server-wide
  `config-override.json5`; re-read each tick. Zero disables; default target is
  five minutes. Keep all copies.
- Temporary files must not end in `.json` or `.zip`, so startup source
  selection cannot choose an incomplete checkpoint.

## Integrity and recovery

Checksums are forensic, not a loading prerequisite. `databaseChecksum` is a
live change/cache identifier, not the digest of the archived JSON.
The exact retained `competition.json` bytes can later be SHA-256 hashed and
matched against OWLCMS's `export.json` (`channel=websocket`) audit record.
No embedded digest, separate sidecar or reserialization is required.

Connection-credential handling belongs to
[140](done/140-EXPORT_CREDENTIAL_PROTECTION.md). Tracker stores those values as
received and never decrypts them; only OWLCMS does, when it loads a checkpoint.
Loading belongs to [160](160-DIRECT_JSON_BOOT.md): `OWLCMS_LOADJSON` set to
the checkpoint folder loads the newest complete checkpoint at startup.

Full JSON-plus-log audit snapshots are not five-minute checkpoints: they
roll audit logs and repeatedly archive the full log history. Keep them at
natural session boundaries or on demand. HTTP-polling alternatives are not
part of the chosen mechanism.

## Acceptance

Verify the configured timer/disable setting, unchanged-data suppression,
byte-identical ZIP retention, reconnect persistence, atomic publication under
transfer/validation failure, source-folder compatibility and recovery from
an actual retained checkpoint.
