# Cloud App: Control Panel Token

This item is for the cloud deployment app (`fly-owlcms`, repository `cloud`), not for
OWLCMS itself. It completes the locality rules of
[130](130-ACCESS_CONTROL_REMAINING.md#locality-the-control-panel-token): in the cloud
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

## To decide when implementing

- Whether the token is generated at app creation only, or also offered as a
  "regenerate" action.
- Where the copy icon sits in the apps view.
