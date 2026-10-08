# Concurrent Opening of Athlete and Registration Cards — Compliance Analysis

Status: historical proposal, with implementation updates below. Presence tracking and warning-on-open are not implemented.

## Implementation update (2026-10-06)

- Athlete cards use stale-save confirmation. Registration and weigh-in saves instead refuse stale data with a Close-only message, in addition to checking whether the athlete's session is in progress on a platform (see "Session in progress" below).
- Athlete-card dialogs use strict server-side modality, both in the shared CRUD layout and in the standalone card opened from Registration. Dialogs and notifications opened from the card become its descendants. Closing the card, including on a down signal or decision where the card subscribes to those events, detaches all dependent overlays and removes their client interaction targets.
- This uses Vaadin's dialog ownership rather than a separate registry. Dependent confirmations must not be explicitly attached to the UI outside their parent dialog.
- Card comparison and saving through database commit now run under the same singleton FOP monitor. Save anyway rechecks the snapshot displayed in the confirmation.

## Review decisions and outstanding risk (2026-10-06)

This section is the current decision record and takes precedence over the historical proposal below.

### Item 1: close all dependent dialogs — implemented

**Problem:** a pending stale-save confirmation could remain usable after a down signal or decision closed its athlete card.

**Fix:** use `ModalityMode.STRICT` for both card-hosting paths:

- [OwlcmsGridLayout.showDialog](../owlcms/src/main/java/app/owlcms/nui/crudui/OwlcmsGridLayout.java), the shared CRUD dialog host.
- [RegistrationContent.openAthleteCard](../owlcms/src/main/java/app/owlcms/nui/preparation/RegistrationContent.java), the standalone card host.

Vaadin attaches subsequently opened overlays to the active modal parent. Closing the parent detaches the dependent overlay tree, including stale-save and delete confirmations, and their controls can no longer submit an operation. This applies to every parent-close path, not just lift events. It does not add lift-event subscriptions to hosts that do not already have them.

**Validation:** workspace Java diagnostics were clean; Maven compilation and the eight existing `ConcurrentEditCheckTest` tests passed. Vaadin 25.2.8 ownership behavior was checked against its documentation and implementation. A live-browser close-on-decision scenario has not yet been exercised; the eight tests do not test dialog lifecycle.

### Item 3: refuse a stale registration save, with Close only — implemented

The existing session-status check remains: registration data cannot be saved while the athlete's session is in progress on a platform. Both the form's proposed session and the athlete's stored session are checked.

That check alone does not protect a form opened before lifting and left open until after the session is unselected. Unselection permits editing again, but does not make an old form snapshot safe to save.

**Agreed freshness rule:**

1. Capture the stored athlete data when the registration form opens.
2. Before saving, compare that reference with freshly read stored data. Compare the two database snapshots, not the user's edited form. Unsaved form edits cannot cause this comparison to fail.
3. Any intervening change to data the registration save can overwrite makes the form stale, including hidden lift data and fields the user did not edit. Derived/recomputed values preserved separately by persistence should not cause false refusals.
4. Refuse the stale save and show a message with **Close as the only option**:

   > This athlete's data changed after you opened the form. Your changes cannot be saved. Reopen the athlete to edit the current data.

5. Close dismisses both the message and the registration form without saving. There is no Save anyway, Keep editing, merge, or automatic refresh. Escape/outside-click must not offer a way back to saving the rejected form. Reopening loads fresh data.

This behavior applies to registration, weigh-in, and the registration form reached through team selection; team selection delegates construction and operation buttons to the registration factory. The fields displayed at opening now come from the same freshly loaded athlete as the reference snapshot, rather than a potentially older grid row.

[RegistrationEditCheck](../owlcms/src/main/java/app/owlcms/data/athlete/RegistrationEditCheck.java) compares identity, registration/eligibility data, session and category IDs, participation membership, all declaration/change/result fields, custom score, and hidden referee/jury/challenge/time metadata. Snapshots copy mutable decision lists and participation values; computed progressions and ranking changes do not invalidate them.

UI wording is delivered in [registration_stale_translations.tsv](../shared/src/main/resources/i18n/registration_stale_translations.tsv); the canonical translation CSV is unchanged. Registration check and write are not a global concurrency guarantee against arbitrary other registration writers. Item 2's serialization is scoped to cards and event processing on the same FOP, as agreed.

### Item 2: guarded card saves and recheck on confirmation — implemented

**Agreed scope:** the marshal and competition director use the same singleton FOP. No distributed editing lock, per-athlete lock registry, JPA `@Version`, or pessimistic database locking was added.

- **A:** [ConcurrentEditCheck.trySave](../owlcms/src/main/java/app/owlcms/data/athlete/ConcurrentEditCheck.java) acquires that FOP's existing Java monitor and keeps it across the current database read, comparison, copying of card values, persistence through commit, and the WeightChange event. FOP event handling is already synchronized on the same instance. A second participating save therefore checks only after the first has committed.
- **B:** every Save anyway click repeats the guarded operation using the exact stored snapshot captured for the displayed confirmation. Further conflicting changes produce an updated confirmation, not a write. Changes already matching the proposed values need no confirmation.
- A renewed confirmation shows the complete set of still-pending overwrites, not just fields changed since the last confirmation. For example, it shows both Change 1 (stored 65, proposed 63) and Change 2 (stored 67, proposed blank). Previously accepted conflicts remain visible when another change requires renewed consent.
- Normal Save, the direct card update entry point, and both withdrawals use the same operation. No monitor is held while displaying a dialog or waiting for a user.
- [FormSaveTask](../owlcms/src/main/java/app/owlcms/nui/shared/FormSaveTask.java) executes persistence/monitor acquisition in a virtual-thread worker. It captures the UI and audit actor on the UI thread, prevents duplicate submission, and applies completed UI work separately. Closing/detaching the form cancels pending work before it saves; exceptions are logged and shown through the standard operation-error UI.
- The WeightChange event is constructed on the UI thread so its audit identity is retained. Strict modality continues to remove dependent confirmations when the parent closes.
- The existing off-platform card host is retained for compatibility and uses a common fallback monitor for off-platform cards. It is outside the agreed same-FOP concurrency guarantee; that fallback does not serialize it with a platform.

The original risk assessment motivating these changes is retained below:

There are two different exposure windows:

| Situation | Exposure | Assessment |
| --- | --- | --- |
| Normal Save with no conflict | Another save to the same athlete commits after the snapshot read but before this save writes. Two officials can both pass their checks before either saves. | A short server-execution window, not the entire time the card is open. Likelihood is expected to be low in ordinary manual use, but no timing measurements or incident rate establish a numerical probability. |
| Save anyway after a conflict | Another save occurs while the official reads the confirmation. Save anyway currently writes without rereading. | A human-duration window with no fixed upper bound. More credible during active corrections; the confirmation can omit changes the official is about to overwrite. |

The consequence in either case can be loss of a declaration, change, or result and thus an incorrect requested weight or lifting order. Conflicting edits to the same field are not required: a card writes back its other stale values too. The comparison protects ordinary sequential saves for its covered fields; this is a residual concurrency gap, not a failure of every two-card interaction.

Vaadin serializes requests within one browser session, not between the marshal's and director's separate sessions. Strict modality solves item 1 only. Close-on-decision reduces exposure to lift-driven writes for subscribed cards but does not serialize two officials' saves. Audit records aid investigation but are not prevention or guaranteed automatic recovery.

**Priority interpretation:** the review's P1 label referred to the potential competition-data impact, not evidence that simultaneous saves are frequent. Both windows are now addressed within the agreed single-FOP scope. Writers outside that synchronization and other server processes remain outside this guarantee.

### Item 4: custom score — implemented

Custom score is included in card snapshots/comparison and labeled using the existing `Score` translation key. Registration freshness comparison includes it as hidden data too.

### Focused verification

### Session in progress — implemented

"Beyond the first lift" was too late: a session could be selected and its clock running while registration data was still saveable. The predicate is now `FieldOfPlay.isSessionInProgress()`, and `OwlcmsFactory.getFOPSessionInProgress(group)` finds the platform where it holds:

- **Selection:** when a session is selected, it is in progress if any of its athletes already has a completed attempt (good or failed); otherwise it is not.
- **First clock start:** starting the athlete clock while a session is selected marks it in progress.
- **Stops, resets, breaks, and reloads of the same session:** no change.
- **Unselection or switching to another session:** clears the status; a newly selected session is evaluated as above.

The same predicate guards registration, weigh-in, and team-selection editing, and session results / package editing. It is a status check, not a lock.

Maven: 47 focused tests passed — 19 card-conflict, 11 registration-freshness, 9 session-in-progress (driven through real `SwitchGroup`, `StartLifting`, `TimeStarted`, `TimeStopped`, `ForceTime`, and `BreakStarted` events), and 8 `TwoMinutesRuleTest` clock tests, since the clock-start path was touched.

Maven compilation and 37 focused tests passed: 18 card-conflict/guarded-save tests, 11 registration-freshness tests, and eight selected-session status tests. Coverage includes simultaneous same-FOP saves, database commit visibility, repeated Save anyway with further changes (including a different field), matching values, cancelled saves, both refused and accepted withdrawal callbacks, custom score, hidden results after session unselection, immutable opening values, participation membership versus derived ranks, actual registration write/refusal callbacks, both stored and proposed lifting sessions, the break state, session identity, and selection/unselection/reselection. Tests instantiate no Vaadin UI objects. Dialog lifecycle and the Close-only refusal still require live-browser verification; their UI behavior is not claimed to be covered by these tests.

## Original decisions (2026-10-06; superseded where noted above)

- **Warning when a card is opened**: advisory only, never a lock (§4.3, §5.1).
- **Confirmation on stale save**: adopted (§6.1).
- **Registration and athlete cards warn about each other**: adopted.
- **PIN mode**: show the station (and the platform when there are several).
- **Heartbeat**: see §3.5. Recommendation: keep the default interval.
- **People who already have the card open are told**, on every platform and on Registration and Weigh-in, through a targeted event on the application-wide bus (§4.4).

Related: [ACCESS_CONTROL_DESIGN.md](ACCESS_CONTROL_DESIGN.md) (identity), [AUDIT_TRAIL_SPECIFICATION.md](AUDIT_TRAIL_SPECIFICATION.md) (stations, change records).

## 1. Question

When someone opens an athlete card or a registration card, can we tell them that the card is already open and who opened it? Roles and accounts now exist, so we can identify that person.

**Short answer: yes.** The identity is available today: `Principal` gives the account, and `StationResolver` gives the station. The missing piece is a server-side registry that records which athlete cards are open. Nothing tracks this today.

A warning alone does not prevent lost updates. A confirmation on stale save is therefore part of the design (§6).

## 2. Scope: what counts as a "card"

Both forms edit the same `Athlete` row.

| Form | Factory | Opened from | Station (`StationResolver`) |
| --- | --- | --- | --- |
| Athlete card | `AthleteCardFormFactory` | `AthleteGridContent` row click: Announcer (not passive speaker), Marshal, Timekeeper, TC, Jury, Testing, Session Results, Package | ANNOUNCER, MARSHAL, TIMEKEEPER, TC, JURY_CONSOLE, TESTING, RESULTS |
| Athlete card | `AthleteCardFormFactory` | `RegistrationContent.openAthleteCard` (separate `Dialog`, `getFop()` is `null`) | REGISTRATION |
| Athlete card | `AthleteCardFormFactory` | `TeamItemResultsFormFactory` | RESULTS |
| Registration card | `NAthleteRegistrationFormFactory` | `RegistrationContent`, `WeighinContent`, `TeamItemSelectionFormFactory` | REGISTRATION, WEIGHIN |

Both forms bind the snatch and clean & jerk first declarations (`snatch1Declaration`, `cleanJerk1Declaration`). The two forms therefore conflict with each other as well as with themselves, so each form must warn about the other.

## 3. Current state

### 3.1 No presence tracking and no locking

- No component records which athlete forms are open. A search for `editingAthlete`, `lockedBy`, `openedBy` and similar names finds nothing.
- `Athlete` has no `@Version` field, so the database has no optimistic locking.
- `AthleteCardFormFactory.buildNewForm` loads a fresh copy from the database when the card opens. `doUpdate` then calls `Athlete.conditionalCopy(originalAthlete, editedAthlete, true, true, true)` and `AthleteRepository.save`. This writes back **every field as it was when the card opened**, plus the user's edits.
- `NAthleteRegistrationFormFactory.update` merges the whole bean (`AthleteRepository.save` → `em.merge`).
- **Result: the last save wins on every field.** If the marshal records a change on athlete X while the announcer has X's card open, saving the announcer's card silently undoes the marshal's change. The audit trail records both saves, so the change can be reconstructed afterwards, but nothing prevents it.

### 3.2 Existing partial mitigation

`AthleteCardFormFactory.FormComponent` closes the card on `UIEvent.DownSignal` and `UIEvent.Decision` when the card's athlete is the current athlete. This covers only the lift itself. It does not cover changes made by another station.

### 3.3 Identity available for the message

| Source | Accounts mode | PIN mode |
| --- | --- | --- |
| `Principal.username()` | login name | `"-"` (officials PIN), `"backdoor"` |
| `UserAccount.getShownName()` | display name, or the login name if none | n/a |
| `Principal.loginPlatform()` | platform chosen at login (lockable roles) | `null` |
| `StationResolver.resolve(origin)` | MARSHAL, ANNOUNCER, … | same, inferred from the page |
| `AuditActor.client` | IP + session hash | IP + session hash |

In PIN mode every official is the same anonymous principal. The station and the platform are the only useful identification.

### 3.4 Dialog lifecycle hooks

- `OwlcmsGridLayout.showDialog` already adds an `addOpenedChangeListener`. It is used by every `OwlcmsCrudGrid` form, including the athlete card in `AthleteGridContent` and the registration and weigh-in forms.
- `RegistrationContent.openAthleteCard` builds its own `Dialog` and needs its own listener.
- If the UI goes away while the dialog is open, the dialog-close listener does not fire. When a UI closes, its component tree is detached, so a detach listener on the form component covers every case. The only question is *when* Vaadin closes the UI (§3.5).
- `AppShell` already has a session-destroy listener. `SessionLogout.redirectAllAndInvalidate` handles explicit logout.

### 3.5 Heartbeat and UI cleanup (Vaadin 25.2.8, verified in the Flow jars)

Vaadin has three separate mechanisms that close a UI. Each one detaches the component tree and therefore releases the claim.

| Mechanism | How it works | Delay |
| --- | --- | --- |
| **Unload beacon** | `FlowClient` sends a `navigator.sendBeacon` with an `UNLOAD` payload on `pagehide`. `ServerRpcHandler.handleUnloadBeaconRequest` closes the UI ("UI closed with a beacon request"). It is skipped for `@PreserveOnRefresh` views, and owlcms has none. | about 1 s |
| **Missed heartbeats** | The client sends a heartbeat every `heartbeatInterval` (not configured, so the default of 300 s applies). A UI expires after 3 missed heartbeats, but dead UIs are only removed **when another request arrives on the same Vaadin session**. | 15 min, and only if another tab of the same session is still active |
| **HTTP session expiry** | `closeIdleSessions` is false, so heartbeats keep the session alive. When all of a session's tabs have stopped sending requests, the servlet session times out after Jetty's default `session-timeout` of 30 min. The session-destroy listener then closes all its UIs. | 30 min after the last request |

What this means for open-card claims:

| Situation | Released by | Typical delay |
| --- | --- | --- |
| Save, Cancel, Esc, click outside, closing on DownSignal or Decision | dialog close listener | immediate |
| Navigating to another owlcms page in the same tab | view detach | immediate |
| Closing the tab or the browser, reloading, typing another URL | unload beacon | about 1 s |
| Logout | `releaseSession` | immediate |
| Browser crash, tablet powered off or battery dead, Wi-Fi lost for good, mobile browser killed in the background | heartbeat or session expiry | 15 to 30 min |
| Laptop asleep with the card open | nothing: the card **is** still open, and the claim is correct when the laptop wakes | n/a |

Only the crash and network-loss cases leave a stale entry, and this is why the warning shows "since hh:mm".

**Reducing `heartbeatInterval` (for example to 60 s) is not recommended:**

- It only shortens the *missed heartbeat* case (15 min → 3 min), and only when the same browser session has another active tab. The more common single-tab crash still waits for the 30-minute session expiry. That timeout would also need to be reduced, which affects every page.
- It reduces how long a tablet can stay disconnected. With 300 s, a station can drop off the Wi-Fi for up to 15 minutes and resume where it was. With 60 s, any outage longer than 3 minutes closes the UI. The marshal or announcer then gets a "session expired" reload during the competition and loses any unsaved card. That is a worse failure than a stale "also open" warning.
- The cost of the extra requests is negligible for owlcms. The real objection is that UIs would be closed too eagerly.

Because the warning is advisory and the stale-save confirmation (§6.1) protects the data whatever the claims say, a stale claim only adds one extra line to the warning. If needed later, the holder's age could be shown with emphasis (for example "since 13:12, 40 min ago"), rather than changing the timeouts.

## 4. Proposal

### 4.1 `AthleteEditingRegistry` (pure logic, unit-tested)

An in-memory, application-wide registry. It contains no Vaadin types and nothing is persisted.

```
record EditingClaim(UUID token, long athleteId, FormKind kind /* CARD, REGISTRATION */,
                    String user /* shown name, null in PIN mode */, String station,
                    String platform /* nullable */, String sessionKey, Instant openedAt)

UUID claim(EditingClaim c)           // returns the token
List<EditingClaim> othersFor(long athleteId, UUID exceptToken)
void release(UUID token)             // idempotent
void releaseSession(String sessionKey)
void clear()                         // on database reset/import
void recordSave(long athleteId, String user, String station, String platform, Instant at)
Optional<LastSave> lastSave(long athleteId)   // for the stale-save confirmation (§6.1)
```

- Backed by `ConcurrentHashMap<Long, List<EditingClaim>>` (copy-on-write lists).
- `sessionKey` is an opaque key for the Vaadin session (for example a hash of the session id). It allows a release on logout or session destroy, and lets the message say "you have it open in another window". It is never displayed.

### 4.2 When to claim and release

The claim and the app-bus subscription share one lifecycle: the **attach and detach of the form component**. Every way of opening and closing a card goes through it:

- Both dialogs are opened without being added to a parent (`OwlcmsGridLayout.showDialog`, `RegistrationContent.openAthleteCard`). Vaadin therefore adds the dialog to the UI when it opens and **removes it when it closes**, which detaches its content.
- Each opening builds a **new** form component: `buildNewForm` is called for every card. Weigh-in "Update and next" closes the form (`hideForm`) and opens a new one for the next athlete. A form component therefore always belongs to exactly one athlete.
- Closing the UI (unload beacon, heartbeat, session expiry, §3.5) and navigating away also detach the component.

| Event | Action |
| --- | --- |
| Form component attached: athlete card (`AthleteCardFormFactory.FormComponent`) or registration/weigh-in form (`UPDATE` only, because `ADD` has no id) | On the UI thread: capture `Principal`, station and platform; call `othersFor` and show the warning (§4.3); `claim`; subscribe to the app bus (§4.4). |
| Form component detached: Save, Cancel, Esc, outside click, `closeDialog()` from DownSignal/Decision, withdrawal buttons, Update and next, UI closed, navigation | `release(token)` and unsubscribe from the app bus. |
| Logout and Vaadin session destroy | `releaseSession(sessionKey)`. This is a safety net; detach normally comes first. |
| Competition import or database reset | `clear()`. Athlete ids are recreated, so old claims are meaningless. |

A dialog `openedChanged` listener is not needed: detach covers it, and a single hook also covers `RegistrationContent.openAthleteCard`, which has its own dialog.

### 4.3 What the user sees

Advisory, never blocking (see §5.1). The registration card and the athlete card warn about each other: claims of both kinds are returned for the same athlete. When opening a card for which `othersFor` is not empty, show a notification (warning theme, does not close the dialog), for example:

- Accounts mode: *"Also open by Marie Tremblay (Marshal, Platform A) since 14:02."*
- PIN mode: *"Also open at the Marshal station (Platform A) since 14:02."* The anonymous PIN user name is never shown.
- Other kind of card: *"Registration card also open at the Weigh-in station since 09:15."*
- Same session: *"You already have this card open in another window."*
- Several holders are listed on one line each, up to three, then "+n".

Show the platform only when the competition has more than one platform. Show the time the card was opened, because a claim may be stale (§3.5).

The people who already have the card open are told as well (§4.4).

### 4.4 Telling the people who already have the card open

Example: the marshal has athlete A open on platform A. The Competition Director opens A from platform B, or from Registration. The marshal must be told.

**The per-platform UI bus cannot carry this.** `FieldOfPlay.getUiEventBus()` only reaches the pages of one platform, and Registration, Weigh-in and the Registration-page athlete card have no FOP (`getFop()` returns `null`).

**Use an application-wide bus, with the claim token as the address.**

- `OwlcmsFactory.getAppUIBus()` already exists. It was created for application-wide notices, for example warning visitors to the public cloud demo that the database was about to be reset. That demo code has since been removed, so the bus has no subscribers and no posts today. Reuse it, because it is the application-wide channel this feature needs:
  - Guava dispatches by event type, so a future demo-reset broadcast and the card events cannot see each other's events. Each subscriber receives only the types it `@Subscribe`s to.
  - Make it an `AsyncEventBus` with a small daemon executor, like the FOP UI buses, so that the opener's request thread never runs code for another user's UI (§5.4). A demo-reset broadcast to every UI also needs this.
  - Create it eagerly, or make the getter `synchronized`. The current lazy `if (appEventBus == null)` is not thread-safe: two threads could each create a bus, and events posted on one would be lost on the other.
  - Add a comment on the getter: events on this bus are either broadcasts or addressed (`CardAlsoOpened` by claim token), and subscribers of addressed events must filter.
- New event `UIEvent.CardAlsoOpened(UUID targetToken, long athleteId, FormKind openerKind, String openerUser, String openerStation, String openerPlatform, Instant at)`.
- **Sender:** the registry, in `claim()`. For each existing claim on the same athlete (other than the new one), it posts one `CardAlsoOpened` whose `targetToken` is that claim's token. The addressee is a single open card, not a page or a platform.
- **Receivers: the athlete card component and the registration/weigh-in form.** Both subscribe through one small shared helper, `CardPresence`, so the logic is written once:

  ```
  CardPresence.bind(Component host, long athleteId, FormKind kind, Origin origin)
  ```

  - `host` is `AthleteCardFormFactory.FormComponent` for the athlete card. For the registration form it is the `FlexLayout` returned by `createTabSheets`, which is the root of the form. No new wrapper component is needed.
  - **On host attach**: warn the opener (§4.3), `claim`, then `SafeEventBusRegistration.registerSubscriber(this, appBus)`. (`uiEventBusRegister` needs a FOP and ties unregistration to the *UI* detach, so it cannot be used here.)
  - **On host detach**: `release(token)` and `SafeEventBusRegistration.unregisterSubscriber(this, appBus)`. This must be the **component** detach, not the UI detach. Otherwise each card ever opened on a page would stay subscribed until the page is left, because Guava keeps strong references to subscribers.
  - **`@Subscribe onCardAlsoOpened(e)`**: return unless `e.targetToken()` is this helper's token. Otherwise call `UIEventProcessor.uiAccessIgnoreIfSelfOrigin(host, appBus, e, 1, () -> showNotification(...))`. That method is public, and it already unregisters the subscriber if the host no longer has a UI.
  - The athlete card, the Registration-page athlete card, the registration and weigh-in forms, and the two team-item factories (which delegate to these two forms) are all covered by the two `bind` calls.
- **Live delivery works**: `AppShell` has `@Push`, so a notification shown inside `ui.access` on a bus thread reaches the browser at once, without waiting for the marshal to click anything.
- **Ordering**: the opener posts the events in `claim()`, before its own subscription. The events are addressed to the *other* tokens, so the opener never receives its own events.
- **What the marshal sees**: a warning notification that stays until it is dismissed, for example *"Athlete A: card also opened by Pat Smith (Competition Director, Platform B) at 14:05."* In PIN mode: *"… also opened at the Registration station at 14:05."* The marshal's card stays open, and the stale-save check (§6) still applies when the marshal saves.
- **Same browser session**: no notification. The opener has already been told "you already have this card open in another window".
- **Bonus clean-up**: if the addressee's host no longer has a UI, `uiAccessIgnoreIfSelfOrigin` already unregisters it. The handler also releases that claim. A stale claim is therefore removed the first time someone else opens the same athlete. Combined with the unload beacon, very few stale entries remain.

Cost: one event class, the `CardPresence` helper, one `bind` call in each of the two form factories, and one loop in `claim()`. The bus only carries these events, so the volume is a few events per card opening. The feature can be removed without affecting the open warning or the stale-save confirmation.

**Related fix (done, 2026-10-06):** the athlete card's existing subscription to the FOP bus (for DownSignal/Decision) used `uiEventBusRegister`. That method unregisters only when the *UI* detaches or the page is left, and it adds two listeners to the UI on every call. Each opening of a card therefore left one more subscriber on the FOP bus and two more listeners on the UI until the user left the lifting page. The handler read factory-level fields (`originalAthlete`, `uiEventBus`) that point at the latest card. `AthleteCardFormFactory.FormComponent` now:

- keeps its own athlete and bus reference;
- subscribes with `registerSubscriber` when it is attached and unsubscribes with `unregisterSubscriber` when it is detached (that is, when the dialog closes);
- runs `uiAccess` on itself rather than on the page, so a failed access unregisters the card and not the page.

`CardPresence` uses the same pattern on the app bus.

**Same pattern, not yet fixed:** `BreakManagement` (the content of `BreakDialog`, created for each opening) also uses `uiEventBusRegister`. It unregisters itself only in the "end break" button handler. When the break dialog is closed any other way, it stays subscribed to the FOP bus until the page is left. The same component-detach unregistration would fix it.

## 5. Compliance considerations

### 5.1 Advisory, not a lock

A hard lock is not acceptable. A stale claim (crashed tablet, lost network) would stop the announcer or marshal from correcting an athlete during a session for up to 30 minutes (§3.5). The rules require the announcer and marshal to be able to act immediately. The message informs the user, and the stale-save confirmation (§6.1) protects the data.

### 5.2 Access control

- No new route, role, `Capability` or `PageRule`. `AccessPolicy` is unchanged.
- The holder's identity is shown only to users who are already authorized to open that athlete's card, so they can already see the athlete's data. The disclosure is limited to "who else is working on this athlete now".
- Platform-locked users (§4.4 of the access-control design) see claims made from other platforms only for athletes that they can already open, for example from Registration or Results. This is acceptable: registration staff need to know that a platform station is editing the athlete.
- Passive speaker cannot open cards (`setClickRowToUpdate(!isPassiveSpeaker())`), so it never creates claims. Display principals cannot open these pages.

### 5.3 Personal data and privacy

- Show only the shown name, the station and the platform. **Never** show the client IP or the session hash. `AuditActor.client` stays in the audit trail only.
- The registry is in memory only. It is not included in JSON or SBDE exports, not written to the database and not written to the audit files. The audit trail records *changes*, not views ([AUDIT_TRAIL_SPECIFICATION.md](AUDIT_TRAIL_SPECIFICATION.md) §1). Recording card *opens* in the audit would change its scope and is not recommended.
- Debug logging of claim and release is allowed with the platform prefix (`FieldOfPlay.getLoggingName`). Log the station and the user name, never the IP or the session id.

### 5.4 Thread safety (repository rules)

- `OwlcmsSession.getPrincipal()` depends on the current `VaadinSession`. Capture it, together with the station and the platform, in the host's attach listener, which runs on the UI thread. Do not capture it inside FOP event callbacks, app-bus handlers or `UIEventProcessor.uiAccess` blocks.
- Registry operations are non-blocking map updates and are safe to run on the UI thread.
- The app bus that warns the people who already have the card open (§4.4) is asynchronous. `claim()` returns at once. Each addressee's notification runs on a bus thread through `UIEventProcessor.uiAccess` on **its own** component's UI. There is no `ui.access` nested inside the opener's request, and no use of `UI.getCurrent()` in the handler.

### 5.5 Translations

The feature needs new keys, for example `AthleteCard.AlsoOpenBy`, `AthleteCard.AlsoOpenAtStation`, `AthleteCard.AlsoOpenSelf`, `AthleteCard.AndMore`, `AthleteCard.StaleSaveTitle`, `AthleteCard.StaleSaveReload`, `AthleteCard.StaleSaveOverwrite`, and the station display names if no existing keys fit. Deliver them as a TSV file in `shared/src/main/resources/i18n/` following the repository process. Do not edit `translation4.csv`.

## 6. Save-time check: confirmation on stale save (adopted)

A user who sees the warning can still save a stale card. A stale card can also exist without any warning: for example, the other station saved and closed its card before this one was opened elsewhere and then edited. The save-time check is what actually protects the data.

### 6.1 Stale-save confirmation

- **On opening**, keep a snapshot of the form-editable fields of the athlete as loaded from the database. Use `AthleteDiff.Snapshot`, restricted to the fields the form binds (§6.2).
- **On save**, before the full write (`doUpdate` in `AthleteCardFormFactory`, `update` in `NAthleteRegistrationFormFactory`), reload the athlete and take the same snapshot. Compare it with the opening snapshot.
- **If nothing changed**, save as today.
- **If fields changed**, do not save yet. Show a confirmation that lists each changed field with its value at opening and its current value, for example:

  > Changed since you opened this card (Marshal, 14:03):
  > C&J 2 — change 1: 120 → 123
  >
  > [Reload card] [Overwrite] [Cancel]

  - **Reload card** reopens the card with the current database values. The user's edits are lost and must be retyped. This is the safe default and gets the focus.
  - **Overwrite** saves as today. The audit trail records both saves.
  - **Cancel** returns to the card with the user's edits.
- The "who and when" line comes from the registry: on every successful save, record `lastSave(athleteId) = {user, station, platform, time}`. If none is recorded (for example a FOP-side change), omit the line.
- The comparison is a pure function over two snapshots and the set of bound fields, so it can be unit-tested.

### 6.2 Fields compared

Compare **only the fields that the form writes back**. Otherwise derived fields would trigger false confirmations: ranks recomputed by `BestAthleteRankingService` when someone else saves, totals and scores, lift order.

- Athlete card: declarations, changes and actual lifts for all six attempts, plus the withdrawal and forced-current flags.
- Registration card: the bound fields (identity, team, body weight, category and eligibility, entry total, the two first declarations, group, …).

The two lists overlap on the snatch and clean & jerk first declarations, which is why the two kinds of cards must also check each other.

### 6.3 Interaction with lifting

- When the card's athlete is the current athlete, the card is already closed on `DownSignal` and `Decision` (§3.2).
- Lift results are posted by the FOP, not by a card. If an announcer card is open on an athlete whose result is reversed or corrected elsewhere, the comparison shows the result field and the user decides.

### 6.4 Follow-up, not in this iteration

**Write only the fields the user changed.** Replace the full `conditionalCopy` and `merge` writes with a write of only the fields this form edited, so that untouched fields keep the other station's value. This would remove most confirmations, but it is a larger change in two complex form factories and must be tested against weigh-in and declaration validation.

Adding `@Version` (optimistic locking) is **not** recommended in this iteration. FOP code also saves the athlete (good/no lift, automatic progressions), so a version check would reject legitimate card saves during lifting and would need retry and merge handling everywhere.

## 7. Files affected (implementation sketch)

| File | Change |
| --- | --- |
| new `app/owlcms/data/athlete/AthleteEditingRegistry.java` (or `app/owlcms/access/`) | Registry, `EditingClaim`, `FormKind` |
| new `nui/shared/CardPresence.java` | Attach: warn, claim, subscribe. Detach: release, unsubscribe. `@Subscribe` handler filtered by token (§4.4). |
| `nui/lifting/AthleteCardFormFactory.java` | `CardPresence.bind(mainLayout, …)` in `buildNewForm` |
| `nui/shared/NAthleteRegistrationFormFactory.java` | `CardPresence.bind(form, …)` in `buildNewForm`, `UPDATE` only |
| `access/SessionLogout.java`, `AppShell.java` | `releaseSession` |
| `init/OwlcmsFactory.java` | `getAppUIBus()` becomes an `AsyncEventBus`, created thread-safely. It was used by the removed cloud-demo reset notice and has no users today. |
| `uievents/UIEvent.java` | New `CardAlsoOpened` event, addressed by claim token (§4.4) |
| Competition import or reset path | `clear()` |
| new TSV in `shared/src/main/resources/i18n/` | New keys |
| new `AthleteEditingRegistryTest` | Unit tests (§8) |

## 8. Tests

Pure JUnit tests on the registry, with no Vaadin objects (repository rule):

- Claim, then `othersFor` from another token returns the first claim. The holder's own token is excluded.
- Release is idempotent. `releaseSession` removes all claims of that session and none of the others.
- `CARD` and `REGISTRATION` claims on the same athlete are visible to each other.
- `clear()` empties the registry.
- `claim()` produces one `CardAlsoOpened` per other claim on the athlete, each with that claim's token. It produces none for claims of the same session. Inject the bus or a poster so this can be tested without Vaadin.
- Concurrent claims and releases from several threads leave no orphans.

If the stale-save check (§6.1) is implemented, test the snapshot comparison as a pure function over `AthleteDiff.Snapshot`:

- No change → no confirmation.
- Change to a bound field → listed with the old and new values.
- Change only to derived fields (ranks, totals) → no confirmation.
- A declaration changed by the registration card is detected by the athlete card, and the reverse.

## 9. Remaining decisions

1. Default button of the stale-save confirmation: **Reload card** (recommended) or **Overwrite**?
