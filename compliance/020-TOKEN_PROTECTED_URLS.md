# Token-Protected URLs

Status: agreed 2026-10-25; not implemented.
Prerequisite: [account token generation](010-ACCOUNT_TOKENS.md).

## Common rule

URL-based data extraction requires a valid token and the owning account's
permission for the operation. This includes display pages, replay clips,
competition exports and H2 backup downloads. A token is a login credential,
not a new permission.

- Accept `?token=<value>` (or `&token=<value>`) in persistent OBS and
  broadcaster port-forwarded URLs. No interactive login or token-stripping
  redirect is required. Also support a bearer token in the Authorization header.
- Resolve the owner and current grants, then apply the existing page,
  capability, platform and feature-switch rules. Recheck token validity on
  subsequent protected requests; a session must not bypass revocation.
- No IP whitelist is required for these uses. Loopback/private-network/backdoor
  matches do not replace a token or upgrade its permissions.
- Plain servlets need the same authorization as routes, including GET, HEAD
  and range requests. Protected non-interactive endpoints return 401 for
  missing/invalid tokens and 403 for insufficient permissions.
- Rate-limit failed authentication; audit by account and non-secret token ID.
  Redact tokens from application/proxy logs, diagnostics and referrers; do not
  put them in unrelated links or shared caches.
- Require HTTPS for broadcaster-facing access outside trusted local deployment.
  Copied URLs, OBS scene collections and replay configuration are credentials.

## Replays and EXTERNAL accounts

- The same token grants the EXTERNAL account read-only OWLCMS displays and
  replay browsing, playback and clip downloads. Keep the jury-scoreboard
  exclusion and server-side refusal of referee input.
- For now the administrator copies the token verbatim into replays.
  No separate replay user or broadcaster permission configuration.
- Replays validates tokens with OWLCMS so current grants, account disabling and
  revocation apply. Comparing only with the copied configuration is insufficient.
  Validation failure/unavailability must not grant access.
- EXTERNAL alone cannot export competition data, download H2/audit backups,
  configure cameras, control recording or delete clips.
- Regeneration requires updating every configured URL/client using the token.
  Tracker forwarding update keys are a separate protocol and remain unchanged.

## Scope and remaining decisions

- Decide exact roles/capabilities for JSON/SBDE exports, H2 and full audit backups.
  The token requirement for URL extraction is settled.
- Preserve platform-lock restrictions for staff tokens; define how their
  login platform is selected.
- Define the replay-validation endpoint, cache/revocation behavior and whether
  already-open push-only boards/streams are disconnected immediately.
- Local maintenance operations (`/controlpanel/stop`, simulation, standby) are
  separate from data extraction; their restrictions are not removed here.

## Acceptance

Test allowed and denied account/platform combinations through actual OBS,
replay and export URLs. Test expiry/restart without interactive login,
revocation/role changes, servlet GET/HEAD/range behavior, replay validation
outages, rate limiting and actual credential redaction. IP or forwarded-header
claims must not bypass authentication.
