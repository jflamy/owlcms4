# Tracker Reverse Proxy

Status: note, not started (2026-10-31). Tracker repository.
Related: [130](done/130-ACCESS_CONTROL_REMAINING.md#non-route-review) (OWLCMS no longer
supports being framed by another site; the tracker proxy is the supported way).

## Context

The tracker's `/proxy` route
(`src/lib/server/owlcms-proxy.js`, `http-proxy-middleware`) forwards requests to
OWLCMS, including the Vaadin push WebSocket. The celebrations plugin shows the OWLCMS
scoreboard in an iframe through it: the frame is then same-origin with the tracker
page, so Vaadin's `X-Frame-Options: SAMEORIGIN` is met and the session cookie is
first-party.

## Problems

- **Open proxy.** The `_target` query parameter chooses the proxied address, with no
  check. Anyone who can reach the tracker can make it fetch any address it can reach:
  machines on the local network, or the cloud provider's metadata endpoint on Fly.
- **Framing protection removed.** `onProxyRes` deletes `X-Frame-Options`,
  `Content-Security-Policy` and `Content-Security-Policy-Report-Only`, then sends
  `X-Frame-Options: ALLOWALL`, which is not a valid value: browsers ignore it and allow
  framing by any site. Every OWLCMS page reached through `/proxy/...`, officials' pages
  included, can then be framed by another site (clickjacking).

## Proposal

- The proxied address comes only from the tracker's configuration (the OWLCMS it is
  connected to, `setOwlcmsTarget`), never from the request. If plugins need another
  OWLCMS, they name a configured entry, not a URL.
- Keep OWLCMS's own `X-Frame-Options` and content security policy. The celebrations
  frame is same-origin with the tracker page and passes `SAMEORIGIN` unchanged.
- Optionally, restrict the proxied paths to what the celebrations plugin needs (the
  display pages, `/VAADIN/`, `/local/`, push), so officials' pages are not reachable
  through the tracker.

## Acceptance

- `/proxy/...?_target=http://example.org` does not reach `example.org`.
- The celebrations plugin still shows the scoreboard with live updates (push).
- A page on another site cannot frame `/proxy/lifting/announcer`.
