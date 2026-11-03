# Tracker Reverse Proxy

Status: implemented (2026-11-02). Tracker repository.
Related: [130](130-ACCESS_CONTROL_REMAINING.md#non-route-review) (OWLCMS no longer
supports being framed by another site; the tracker proxy is the supported way).

## Context

The tracker's `/proxy` route
(`src/lib/server/owlcms-proxy.js`, `http-proxy-middleware`) forwards requests to
OWLCMS, including the Vaadin push WebSocket. The celebrations plugin shows the OWLCMS
scoreboard in an iframe through it: the frame is then same-origin with the tracker
page, so Vaadin's `X-Frame-Options: SAMEORIGIN` is met and the session cookie is
first-party.

Celebrations is the only user. The jury replays plugin does not use the proxy: it
calls the replays server directly (`replaysBaseUrl`, `fetch` and a status WebSocket
in `src/plugins/jury/replays/helpers.client.js`). `_target` was only used by the
display-control plugin, removed in commit `827c9be`.

## Problems

- **Open proxy.** The `_target` query parameter chooses the proxied address, with no
  check. Anyone who can reach the tracker can make it fetch any address it can reach:
  machines on the local network, or the cloud provider's metadata endpoint on Fly.
  Verified with a stub server (2026-11-02). No tracker code or plugin uses `_target`.
- **Second entry point to OWLCMS.** Any browser can open `/proxy/...` directly and use
  every OWLCMS page through the tracker, officials' pages included.
- **Dead header-stripping code.** `onProxyRes` is meant to delete `X-Frame-Options` and
  the content security policy, but http-proxy-middleware 3 ignores the `onProxyRes`
  option (it reads `on: { proxyRes }`), so it never runs. OWLCMS's headers already
  reach the browser unchanged (verified with a stub server). The code and its comment
  are misleading.

## Proposal

Framed pages behave exactly as if opened directly on OWLCMS. When a page needs a
token, the token is part of the framed address and OWLCMS checks it as usual.

- The proxied address comes from `OWLCMS_URL` when configured; otherwise it is the
  authenticated OWLCMS WebSocket connection's remote address plus the `scheme` and
  `port` from its `server_info` protocol message. Older OWLCMS versions use port
  8080. It never comes from the browser request. Remove `_target`.
- Only the tracker's own pages may use the proxy, for example the celebrations frame.
  Browsers label every request with `Sec-Fetch-Site` and `Sec-Fetch-Dest`:
  - Accept an HTTP request only if `Sec-Fetch-Site` is `same-origin` and
    `Sec-Fetch-Dest` is not `document`. A frame inside a tracker page and everything
    the framed page loads (scripts, styles, UIDL, long-polling push) pass. Opening
    `/proxy/...` in the address bar (`none`), a link from another site
    (`cross-site`) and framing by another site are refused with 403.
  - Accept a WebSocket upgrade (Vaadin push) only if `Sec-Fetch-Site` is
    `same-origin`, or, when absent, if `Origin` matches the request's host.
  - Refuse requests without these headers (curl, scripts). Every current browser
    and the OBS browser source (Chromium) send them.
- Remove the dead header-stripping code. OWLCMS's `X-Frame-Options: SAMEORIGIN` is
  met because the celebrations frame is same-origin with the tracker page.
- Celebrations: accept a path such as `/displays/scoreboard?fop=A&token=...` and frame
  it as `/proxy/displays/...`, so the frame is always same-origin. Keep accepting a
  full URL.

Limit: the request headers are a browser check. A script can forge them, but then
it only reaches the configured OWLCMS, with OWLCMS's own access control. Because
OWLCMS sees requests as coming from the tracker's address, a Simple Access address
list should not trust the tracker's machine; `xfwd` is not used.

## Acceptance

- `/proxy/...?_target=http://example.org` does not reach `example.org`.
- Opening `/proxy/lifting/announcer` in the address bar is refused.
- A page on another site cannot frame `/proxy/...`.
- The celebrations plugin still shows the scoreboard with live updates (push),
  including in an OBS browser source.
- Unit tests cover `_target`, the accepted and refused `Sec-Fetch-*` combinations,
  and the WebSocket origin check.
