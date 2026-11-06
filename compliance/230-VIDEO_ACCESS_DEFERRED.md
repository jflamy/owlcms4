# 230 - Video Access: Deferred Work

Status: deferred, not planned. Recorded so the requirements are not lost.
Phase 1, the Replays viewer tokens for one laptop per platform (scenario 1),
is [190](190-VIDEO_REPLAYS_TOKEN.md); section and file references below
(`internal/access`, `video-access.json`, `SecurityDir`) are defined there.

## 1. Cameras configuration token

Not needed in phase 1. With Cameras and Replays in the same process (scenario
1), Replays reads the Cameras configuration directly, Caddy refuses
`/api/cameras/config` from outside, and the LAN is trusted. Needed when Replays
imports from Cameras on another machine over a LAN that is not trusted. Closes
the security-review finding that `/api/cameras/config` returns `cameras.toml`
(RTSP URLs can embed camera passwords) to anyone, with wildcard CORS.

- `internal/access`: a `cameras` field in `video-access.json` with a single
  record (`hash`, `createdAt`, `expiresAt`) and `CamerasState`,
  `GenerateCameras`, `ClearCameras`, `VerifyCameras`: one token, 21 days,
  Generate replaces it, Clear returns to Open. One token suffices: its only
  holder is a Replays application on another machine. No browser session.
- Guard `/api/cameras/config` with `VerifyCameras`: Bearer header or `?token=`.
  A viewer token or viewer cookie is refused. Remove its wildcard CORS:
  only the Replays application (not a browser) calls it.
- Cameras menu: **Access Token...**: status, **Generate** (or
  **Regenerate**) and **Clear**, with the show-once dialog of 190 §1.3.
- Same process: Replays keeps reading the Cameras configuration directly
  (`loadLocalCamerasImportPreview`, `syncLocalCamerasConfig`); no token involved,
  including after restart.
- Separate machine: `showRemoteCamerasImportDialog` gains a masked
  "Cameras token" entry. Replays stores it with the server address in
  `<SecurityDir>/cameras-client.json` (0600, same write procedure; plaintext,
  since Replays must send it). `loadRemoteCamerasImportPreview` sends it as
  `Authorization: Bearer`, sets `CheckRedirect` to refuse redirects, and only
  sends it to the address it was stored with. On 401, ask for a new token.
- Tests: guard separation in both directions, remote import with
  valid, expired and missing token against an `httptest` server, redirect
  refused, local import unaffected.

Out of scope here: trimming the configuration response to what Replays needs.
Review it separately; it changes the import format.

## 2. Several Video instances on one machine (scenario 2)

Feasible but more complex; not planned. Recorded so the requirements are not
lost. Example: one replays machine running two Replays-only Video instances,
platform A on HTTP 8091 and platform B on 8092, each importing the streams of
its platform's capture laptop.

Required:

1. **Control Panel installs Video per instance.** Today a Control Panel
   instance (`--instance <name>`) separates OWLCMS and Tracker
   (`OWLCMS_INSTALLDIR`, `TRACKER_INSTALLDIR`) but Video always uses the fixed
   `owlcms-video` folder (`controlpanel/video/config.go`). Two instances would
   share Video versions, the version folder used as `--configDir` (same
   `replays.toml`: port, platform, videos folder), and `video.pid`; launching
   the second would stop the first's Video. Add `VIDEO_INSTALLDIR`, set by
   `applyCLIInstanceOptions` to `owlcms-video` for the main instance and
   `<name>-video` for the others, read by the Video tab's `getInstallDir` and
   `videoPIDFile`. Each instance then also gets its own token file under rule 2
   of 190 §1.1, with no Video change.
2. **Unique MQTT client ID.** Replays connects to OWLCMS as
   `replays-monitor-<machine IP>` (`internal/monitor/monitor.go`); two instances
   on one machine would keep disconnecting each other. Add the HTTP port.
3. **Separate stream ports per capture laptop** (configuration): for example
   laptop B starting at 9005 or 9101, unicast destination the replays machine's
   LAN address. Keep separate ports in multicast mode too.

Then, as in scenario 1 (190): each instance has its own token list, cookies
kept apart by the instance ID, Caddy names mapped to each instance's port,
each listing Caddy as trusted proxy.

Only if Cameras also runs on that machine as a third instance: the module
selection preference (`app.owlcms.video`) is shared by all instances and must
be keyed by the instance's Video folder; and unicast within one machine needs
Replays to accept loopback destinations from a local Cameras server, let the
operator choose among them, and listen on the chosen address rather than
`0.0.0.0` (Cameras sends every stream to `127.0.0.1` and to every destination).

## 3. External broadcaster acceptance through Caddy

Code and documentation for `trustedProxies` and `publicUrl` are in [190](190-VIDEO_REPLAYS_TOKEN.md);
live acceptance testing with Caddy, DNS and router port forwarding is deferred
here:

1. Two platforms through Caddy with one DNS name each (`a.…`, `b.…`), router
   forwarding 80 and 443 only. From outside, as a broadcaster:
   - open `https://a.replays.example.org/`, get the sign-in form, type the "RAI"
     token, get the replays page with **Sign out** at the top right, whose
     tooltip reads "Signed in as RAI" and the expiry;
     let the password manager save the token; close and reopen the browser:
     still signed in; Sign out: back to the form;
   - open `https://a.replays.example.org/?token=<token>`: replays page, token
     gone from the address bar;
   - list `/api/sessions` with a Bearer header, download a lift's MP4 with
     `?token=`, get live updates over `wss://…/ws?token=`;
   - platform A's token is refused by platform B.
2. Video's log shows "RAI signed in" with the broadcaster's external address,
   the cookie is `Secure`, one broadcaster's bad tokens do not block another,
   `/api/cameras/config` is refused by Caddy, and port 8091 is not reachable
   from outside.
