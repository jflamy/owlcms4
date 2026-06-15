# Destination Forwarder Refactor Plan

## Goal

Forwarding is destination-based, not role-based.

A destination is identified by its normalized base URL and owns exactly one update key. Each forwarder instance owns exactly one destination. If there are two HTTP destinations, there are two `EventForwarder` instances. If there are two WebSocket destinations, there are two `WebSocketEventForwarder` instances. If the configuration is mixed, there is one of each.

Runtime event handlers must not branch over publicResults/videoData destinations. They send to the instance destination only.

## Invariants

- Config resolution creates 0..2 destination specs from the configured base URLs and their matching keys.
- A forwarder instance has instance fields for `baseUrl` and `updateKey`.
- HTTP endpoints are suffixes of `baseUrl`: `/update`, `/timer`, `/decision`, `/config`.
- WebSocket sends use `baseUrl` directly; message type is carried in the frame/wrapper.
- `WebSocketEventSender` receives the explicit destination key and never resolves keys from config.
- No sender or forwarder falls back from one configured key to another.
- The only compatibility behavior remains on the tracker receiver: unkeyed binary frames are accepted only when authentication is not configured.

## Implementation Steps

1. Add `ForwardingDestination` value object.
   - Stores normalized `baseUrl` and `updateKey`.
   - Exposes `isHttp()`, `isWebSocket()`, `updateUrl()`, `timerUrl()`, `decisionUrl()`, and `configUrl()`.
   - Provides `fromConfig(Config)` to build the destination list from publicResults and videoData config entries.
   - Deduplicates by URL; same URL with different keys is logged as an error and skipped rather than silently choosing a key.

2. Convert `EventForwarder` to one HTTP destination per instance.
   - Add `destination`, `baseUrl`, and `updateKey` fields.
   - Constructor receives a `ForwardingDestination`.
   - Static map becomes `Map<String, Map<String, EventForwarder>>`, keyed by FOP name then base URL.
   - Reconcile configured HTTP destinations per FOP: keep unchanged forwarders, unregister removed/changed ones, create new ones.
   - Event handlers call instance endpoints only: `destination.updateUrl()`, `destination.timerUrl()`, `destination.decisionUrl()`.
   - `sendPost` stamps the instance `updateKey` into a defensive copy of payload parameters.
   - `sendConfig` uses the instance `updateKey`.

3. Convert `WebSocketEventForwarder` to one WebSocket destination per instance.
   - Add `destination`, `baseUrl`, and `updateKey` fields.
   - Constructor receives a `ForwardingDestination`.
   - Static map becomes `Map<String, Map<String, WebSocketEventForwarder>>`, keyed by FOP name then base URL.
   - Reconcile configured WebSocket destinations per FOP: keep unchanged forwarders, close removed/changed WebSocket sender, unregister removed/changed forwarders, create new ones.
   - Event handlers send to instance `baseUrl` only.
   - Startup, database, translations, flags, `onOpen`, and 428 callbacks use instance `baseUrl` and `updateKey`.

4. Keep `WebSocketEventSender` as a low-level destination-keyed connection.
   - It already requires explicit `updateKey` in `getOrCreate`.
   - It must not import or call `Config`.

5. Validation.
   - Run `get_errors` on edited Java files after each slice.
   - Search for old dual-destination branching in `EventForwarder` and `WebSocketEventForwarder`.
   - Search for unkeyed `WebSocketEventSender.getOrCreate` calls.
   - Do not run Maven/builds without explicit approval.
