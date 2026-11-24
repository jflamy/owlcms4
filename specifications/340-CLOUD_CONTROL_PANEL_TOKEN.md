# Cloud App: Control Panel Token

This item is for the cloud deployment app (`fly-owlcms`, repository `cloud`), not for
OWLCMS itself. It completes the locality rules of
[130](done/130-ACCESS_CONTROL_REMAINING.md#locality-the-control-panel-token): in the cloud
there is no OWLCMS machine at hand and no local control panel, so the cloud app plays
the control panel's role.

## Context

OWLCMS reads `OWLCMS_CONTROLPANELTOKEN` at startup. A request presenting that token
counts as coming from the OWLCMS machine. In the cloud every request arrives through
Fly's proxy with forwarding headers, so the direct-local-connection fallback never
applies: without a token, the actions that require locality are unreachable (audit
preparation and checking, access-mode changes), and so is the admin account until it
has a password. The IWF compliance check reports a missing token as a requirement not
met.

Until this item is done, cloud apps stay in PIN mode, the default: switching to user
accounts on the Administration page needs the token. An organizer who wants accounts in
the cloud sets it by hand (`fly secrets set OWLCMS_CONTROLPANELTOKEN=<value>`) and
pastes the same value when asked.

Fly secrets are write-only: `fly secrets list` returns names, never values. The cloud
app has no storage of its own between sessions.

## Decision

- When the cloud app creates or updates an OWLCMS app, it generates a token and sets
  two secrets in one staged deploy:
  - `OWLCMS_CONTROLPANELTOKEN=<token>`: the value OWLCMS reads;
  - `CPTOKEN_<token>=1`: a marker whose **name** carries the token, so the cloud app
    can recover it by listing secret names. The marker does not start with `OWLCMS_`,
    so OWLCMS never treats it as one of its settings.
- This is the pattern already used for tracker connections
  (`OWLCMS_CONNECTION_<APP>=connected`). Listing the names requires the Fly account
  login, which is acceptable: whoever can list the secrets owns the app.
- The cloud app shows a copy icon for the token next to the OWLCMS app. The value
  itself is not displayed.
- If either secret is missing, or the two disagree, the cloud app generates a fresh
  pair and replaces both; OWLCMS uses the new token after its next restart.
- Token format: secret names are environment-variable names (the tracker code already
  uppercases them, and `-` is not allowed), so the token uses uppercase letters and
  digits without look-alikes (the OWLCMS token-ID alphabet), 15 characters (75 bits).
  OWLCMS only compares the value it was given, so the format may differ from the local
  control panel's.

## Decided when implementing (2026-11-23)

- Only OWLCMS 69.0.0-alpha00 and later get a token. The same version rule lets the image
  set `OWLCMS_CONTROLPANEL`: the cloud app no longer sets that secret for 69+ (it unsets
  it on update), and keeps `OWLCMS_CONTROLPANEL=3.1.0` for older versions.
- The token is generated at creation, and at update when the pair is missing or there is
  not exactly one marker. A Generate/Regenerate button on each OWLCMS app replaces it:
  a running OWLCMS restarts to use it; a stopped one gets it at its next start (staged,
  so that OWLCMS is not started while its database is stopped).
- The token row (copy icon and Generate/Regenerate) sits under the
  Update/Start/Stop/Delete buttons, where the tracker key controls are.
- The token is passed to `fly secrets import` on stdin, never on a logged command line,
  and marker names are hidden in the operation log.
