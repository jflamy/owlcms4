# Replays Access Token (video module)

This item is for `owlcms/video` (Go module `github.com/owlcms/video`), not for
OWLCMS itself. It is listed here because it completes the token-protected URLs of
[120](120-TOKEN_PROTECTED_URLS.md).

## Context

The Replays HTTP server of the video module serves clips and the replay browsing
page to streamers without any check. Its routes (`internal/httpServer/server.go`):

| Route | Use |
| --- | --- |
| `/` | Replay browsing page |
| `/static/...` | Page scripts and styles |
| `/api/sessions`, `/api/sessions/{session}/lifts`, `/api/replay-state` | Session and lift lists, current state |
| `/ws` | Live updates to the page |
| `/replay/{camera}`, `/replay/{camera}.mp4` | The clip a streamer plays |
| `/videos/...` | Direct access to the video folder |
| `/api/cameras/config` | Camera configuration, not a streamer route |

Replays talks to OWLCMS only as an MQTT client and has no OWLCMS account. Checking
OWLCMS account tokens from Replays would need a new channel for little benefit.

## Decision

Replays gets its own token, managed in the video application, the same way OWLCMS
accounts manage theirs:

- A **Generate** button in the video application creates a random token (same
  strength as OWLCMS account tokens). The operator copies it into the streamers'
  replay URLs.
- The token stays valid until the operator generates a new one, which replaces it.
  Regenerating is the way to cut off access.
- Streamer URLs carry it as `?token=<value>`, like OWLCMS display URLs.
- It is independent of OWLCMS accounts and roles: anyone holding the Replays token
  can watch and download replays, and nothing else.
- The token is never logged, and is redacted from request logs (the replay request
  log currently prints the full URI).

## To decide when implementing

- Which routes require the token. Proposed: every replay route including `/videos/`;
  `/static/` stays open. `/api/cameras/config` is not a streamer route and is handled
  separately.
- How the browsing page passes the token to its follow-up requests (`/api/...`, `/ws`,
  video elements): a cookie set after the first valid `?token=`, or the page appending
  `?token=` to each request.
- Storage: the token in clear in the Replays configuration, so the operator can copy
  it again, or only its hash, with the value shown once at generation.
- Response to a missing or wrong token (proposed: plain 401, throttled per address).
- Behavior on first start with no token generated yet.
