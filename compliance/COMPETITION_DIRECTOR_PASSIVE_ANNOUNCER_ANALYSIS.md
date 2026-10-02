# Competition Director and Passive Announcer — Compliance Analysis

Status: historical analysis. The confirmed implementation decisions below supersede the original passive-page recommendations.

## Implementation Clarification (2026-10-02)

The page change uses thin wrappers around `AnnouncerContent`, not a separately reimplemented passive display. Competition Director keeps all active-speaker controls and settings and does not offer a switch to passive mode.

Passive speaker keeps the exact announcer layout, athlete information, grid, timers, lights and notifications. Non-jury operating controls are hidden or disabled, and their keyboard shortcuts are not registered. The red pause indicator remains visible but disabled. Row editing, attempt reversal, session loading and changes to Field of Play single-referee state are blocked.

Jury handling is an explicit exception to passive operation: the existing jury dialogs and actions remain fully interactive, including their competition events. Notification dismissal and the cogwheel live-light setting remain available. The cogwheel can switch back to active speaker, using full-page navigation so no manual refresh is required.

PIN defaults to active speaker; Accounts defaults to passive speaker. Competition Director availability defaults off in PIN and on in Accounts, with explicit feature overrides allowing it in PIN mode.

The original recommendations below concerning independent passive implementation, prohibition of all competition events and display-only jury dialogs are superseded. The grantable Competition Director role is now implemented and exclusively protects its operational page alongside Admin. The feature toggle applies only in PIN mode. Partial configuration-tab permissions remain separate work.

Related: [ACCESS_CONTROL_DESIGN.md](ACCESS_CONTROL_DESIGN.md) and
[COMPETITION_DIRECTOR_PAGE_IMPLEMENTATION_PLAN.md](COMPETITION_DIRECTOR_PAGE_IMPLEMENTATION_PLAN.md).

## 1. Scope

This analysis covers two related changes:

1. Add a grantable `COMPETITION_DIRECTOR` role with almost all administrative and competition-operating access, but without connection configuration or access/account administration. Personalization remains available.
2. Add a separate `PassiveAnnouncerContent` page. It presents the information needed by the speaker and receives jury notifications, but has no competition-operating controls. The speaker cogwheel switches between active and passive announcer pages. Accounts mode opens the passive page by default.

The passive page is recommended instead of adding a read-only state to `AnnouncerContent`. The active page inherits many mutation paths from `AthleteGridContent`; disabling its visible top-bar buttons would not make it read-only.

## 2. Current State

### 2.1 Roles and administration

- `Role.ADMIN` is a grantable composite that expands to every base role, including `ADMIN_PAGES`.
- `AccessPolicy.isAdmin` recognizes only an explicit `ADMIN` grant. Admin accounts are not platform-locked and do not choose a platform at login.
- `ConfigContent` is one `ADMIN_PAGES` route containing five tabs:
  1. Language
  2. Connections
  3. Access Control
  4. Features
  5. Customization
- `AccountsContent` is a separate `ADMIN_PAGES` route. It edits accounts, roles and Accounts/PIN mode.
- Other `ADMIN_PAGES` routes include the administrative diagnostics page, federation-record comparison, session import and testing.

Relevant implementation surfaces:

- `owlcms/src/main/java/app/owlcms/access/Role.java`
- `owlcms/src/main/java/app/owlcms/access/AccessPolicy.java`
- `owlcms/src/main/java/app/owlcms/nui/preparation/ConfigContent.java`
- `owlcms/src/main/java/app/owlcms/nui/preparation/ConfigEditingFormFactory.java`
- `owlcms/src/main/java/app/owlcms/nui/preparation/AccountsContent.java`

### 2.2 Active announcer page

`AnnouncerContent` is authorized by the platform-bound `ANNOUNCER` role. It is not a passive display. Its mutation paths include:

- selecting or switching the session;
- starting lifting and introduction countdowns;
- starting, stopping and setting the clock;
- starting and ending breaks;
- posting explicit good/no-lift decisions;
- opening athlete forms by clicking rows;
- clearing or reversing recorded attempts;
- reloading the group from the database;
- opening medal and ceremony functions; and
- keyboard shortcuts for clock and decision actions.

Its cogwheel currently controls local presentation options such as sounds, single-referee mode, live lights, centered notifications and declaration notifications.

Jury notifications are not inherently read-only in the active page. Current notification dialogs can post `StartLifting` and `JuryDecision` events. Therefore, the active `JuryDecisionDialog` and stoppage dialog cannot be reused unchanged by a passive page.

Relevant implementation surfaces:

- `owlcms/src/main/java/app/owlcms/nui/lifting/AnnouncerContent.java`
- `owlcms/src/main/java/app/owlcms/nui/lifting/JuryDecisionDialog.java`
- `owlcms/src/main/java/app/owlcms/nui/shared/AthleteGridContent.java`
- `owlcms/src/main/java/app/owlcms/components/elements/PassiveTimerElement.java`
- `owlcms/src/main/java/app/owlcms/components/elements/PassiveDecisionElement.java`

## 3. Competition Director Role

### 3.1 Proposed role

Add `COMPETITION_DIRECTOR` as a grantable, non-platform-scopable composite role.

It should expand to the ordinary competition roles on all platforms:

- `PREPARATION`
- `REGISTRATION`
- `WEIGHIN`
- `RESULTS`
- `ANNOUNCER`
- `MARSHAL`
- `TIMEKEEPER`
- `TC`
- `JURY`
- `REFEREE`
- `DISPLAYS`

It must not expand to `ADMIN_PAGES`. A distinct configuration permission or capability is required for the permitted configuration tabs; reusing `ADMIN_PAGES` would also expose accounts and technical administration.

### 3.2 Platform behavior

The Competition Director is an all-platform competition role and should behave like Admin for platform selection:

- no platform-selection step at login;
- no platform lock;
- live platform selectors remain available; and
- access applies to all platforms.

The current `AccessPolicy.isAdmin` special case is too narrow for this behavior. The implementation should express the concept explicitly, for example as an `unlockedAllPlatforms` role property or policy method. It should not rename the Competition Director to an administrator internally.

### 3.3 Configuration access

| Surface | Admin | Competition Director |
| --- | --- | --- |
| Language | Allowed | Allowed |
| Connections | Allowed | **Denied** |
| Access Control | Allowed | **Denied** |
| Features | Allowed | Allowed |
| Customization / personalization | Allowed | Allowed |
| Account management | Allowed | **Denied** |
| Switch PIN/Accounts mode | Allowed | **Denied** |

Connections includes forwarding destinations, MQTT and other external-system credentials or endpoints. Access Control includes PINs, allowlists, backdoor-related configuration, authentication mode and account entry points.

Hiding tabs is insufficient. The server must not bind, accept or save restricted connection/access properties for a Competition Director. Direct navigation to restricted tab indices must select an allowed tab or return access denied without constructing the restricted form.

The safest design is to build `ConfigContent` from independently authorized tab components or field groups. A single form bound to the complete mutable `Config` object risks accepting restricted values even when their controls are hidden.

### 3.4 Other Admin-only routes

The Competition Director must not receive `ADMIN_PAGES`, so these remain Admin-only unless separately authorized in a later decision:

- account management;
- low-level administrative diagnostics;
- testing tools;
- record-federation comparison administration; and
- session import.

This is the least-privilege default. Session import may be operationally useful to a Competition Director, but it should be granted through a separate capability rather than through `ADMIN_PAGES`.

## 4. PassiveAnnouncerContent

### 4.1 Route and authorization

Add a separate platform-bound route owned by `PassiveAnnouncerContent`, for example:

```text
lifting/announcer/passive
```

It uses the `ANNOUNCER` role so existing platform grants remain applicable. The route must perform no mutation merely by loading URL parameters. In particular, a `group` parameter may select what is displayed but must not load or switch the Field of Play session.

### 4.2 Presentation

The passive page should display the same operational facts the speaker needs:

- platform and current session;
- current athlete, start number, attempt and requested weight;
- authoritative countdown;
- referee decision lights according to the selected presentation settings;
- lifting order and completed attempts; and
- jury notifications.

Use passive display components such as `PassiveTimerElement` and `PassiveDecisionElement`. Do not subclass `AthleteGridContent`: that class owns CRUD, attempt-reversal, break and top-bar mutation behavior. Extract a read-only presentation component or build a dedicated read-only grid from the Field of Play state.

### 4.3 No competition controls

`PassiveAnnouncerContent` must not create or register:

- clock start, stop, one-minute or two-minute controls;
- clock or decision keyboard shortcuts;
- good-lift or no-lift controls;
- break, resume or start-lifting controls;
- introduction, session-switch or reload controls;
- athlete editing, attempt clearing or attempt reversal;
- medal-download or ceremony controls; or
- any handler that posts an `FOPEvent` or saves an athlete.

The passive page may provide non-competition display preferences and the cogwheel command that opens the active announcer route.

### 4.4 Jury notifications

The passive page receives and displays all jury notification states needed by the speaker, including summons, deliberation/challenge, technical pause, loading error, verdict, reason and end notifications.

Notifications are display-only:

- no good-lift or no-lift response;
- no `JuryDecision` event;
- no Resume or `StartLifting` event;
- no acknowledgement sent to the Field of Play; and
- no Enter-key shortcut that activates a dialog action.

A local Dismiss control is permissible because it changes only that browser's presentation. Automatic closure on the corresponding end event is preferable. The existing `JuryDecisionDialog` must not be instantiated by the passive page because all of its principal buttons mutate competition state.

### 4.5 Cogwheel switching

Both announcer pages expose a cogwheel item that switches layouts by navigating to the other route:

- active page: **Switch to passive speaker view**;
- passive page: **Switch to active speaker controls**.

Navigation preserves applicable query parameters, especially `fop`, `group`, sound, decision-light, declaration and notification-position preferences. The target page re-resolves access for the target platform; it must not rely only on the source page's authorization.

The switch is a route/layout change, not a CSS hide/show operation. Detaching the active page removes its keyboard shortcuts and mutation handlers.

### 4.6 Accounts-mode default

In Accounts mode, navigation to the speaker function defaults to `PassiveAnnouncerContent`. In PIN mode, the current active announcer page remains the default.

This is a default, not an authorization boundary. If every `ANNOUNCER` account can use the cogwheel to open the active page, every such account can still operate the competition. If enforced passive-only accounts are later required, add a separate active-announcer capability or role; do not infer authorization from the default route.

Direct bookmarks behave consistently:

- passive URL opens passive in either mode;
- active URL opens active when the principal has active-announcer access; and
- switching modes does not silently rewrite an already open tab.

## 5. Proposed Requirements

### Competition Director

- **CD-01** The system shall provide a grantable `COMPETITION_DIRECTOR` role.
- **CD-02** The role shall provide all-platform competition operation without a login-platform lock.
- **CD-03** The role shall not grant `ADMIN_PAGES`.
- **CD-04** The role shall permit Language, Features and Customization configuration.
- **CD-05** The role shall deny Connections configuration.
- **CD-06** The role shall deny Access Control configuration and Accounts/PIN mode switching.
- **CD-07** The role shall deny account creation, editing, password reset, role assignment, enabling and deletion.
- **CD-08** Restricted configuration shall be enforced during navigation, form construction and persistence, not only by hiding controls.
- **CD-09** Personalization shall remain available to the Competition Director in this iteration.

### Passive announcer

- **PA-01** The system shall provide a separate `PassiveAnnouncerContent` route.
- **PA-02** The passive page shall require `ANNOUNCER` access for the selected platform.
- **PA-03** The passive page shall show current speaker information without exposing competition controls.
- **PA-04** The passive page shall not post an `FOPEvent`, save an athlete or mutate Field of Play state.
- **PA-05** The passive page shall not inherit interactive athlete-grid behavior.
- **PA-06** The passive page shall display jury notifications without state-changing notification buttons or shortcuts.
- **PA-07** The cogwheel shall switch between active and passive routes while preserving applicable parameters.
- **PA-08** Accounts mode shall use the passive announcer route as the default speaker destination.
- **PA-09** PIN mode shall retain the active announcer route as the default speaker destination.
- **PA-10** Direct route access shall still be checked independently of the default destination.

## 6. Compliance Matrix — Current Implementation

| Requirement | Current compliance | Evidence / gap |
| --- | --- | --- |
| CD-01 | Not compliant | No `COMPETITION_DIRECTOR` role exists. |
| CD-02 | Not compliant | Only explicit `ADMIN` grants bypass platform selection and locking. |
| CD-03 | Not applicable yet | The proposed role does not exist. |
| CD-04 | Not compliant | The entire configuration route requires `ADMIN_PAGES`. |
| CD-05 | Not compliant | Configuration authorization is route-wide, not tab-specific. |
| CD-06 | Not compliant | Configuration authorization is route-wide; Accounts is separately `ADMIN_PAGES`. |
| CD-07 | Compliant for non-admin roles | `AccountsContent` currently requires `ADMIN_PAGES`; the new role must not inherit it. |
| CD-08 | Not compliant | No partial configuration permission or restricted persistence path exists. |
| CD-09 | Not compliant | Customization is currently available only through the Admin-only configuration route. |
| PA-01 | Not compliant | Only `AnnouncerContent` exists. |
| PA-02 | Partially reusable | Active announcer access already uses platform-bound `ANNOUNCER`. |
| PA-03 | Not compliant | The existing announcer page is operational and mutable. |
| PA-04 | Not compliant | Existing announcer controls post events and save competition data. |
| PA-05 | Not compliant | `AnnouncerContent` inherits `AthleteGridContent`. |
| PA-06 | Not compliant | Current jury dialogs can post `JuryDecision` and `StartLifting`. |
| PA-07 | Not compliant | The cogwheel has presentation settings but no route switch. |
| PA-08 | Not compliant | Accounts mode does not select a passive announcer route. |
| PA-09 | Compliant baseline | PIN-mode announcer navigation currently opens the active page. |
| PA-10 | Partially reusable | Global route authorization exists; a new route must declare its rule. |

## 7. Acceptance Analysis

### 7.1 Role tests

- A Competition Director can open ordinary preparation, registration, weigh-in, results, lifting and display pages for every platform.
- A Competition Director is not prompted to choose a login platform and can change platform selectors.
- A Competition Director can open Language, Features and Customization directly.
- Connections and Access Control are absent and direct tab URLs do not expose their components.
- Crafted requests cannot alter restricted `Config` properties.
- Accounts, admin diagnostics and testing routes are denied.
- Admin behavior remains unchanged.

### 7.2 Passive announcer tests

- Accounts-mode speaker navigation opens the passive route; PIN mode opens the active route.
- Current athlete, attempt, weight, timer, lights and lifting order track the same Field of Play as the active page.
- Clicking rows and attempts does nothing and opens no edit dialog.
- Clock, decision, break, reload, session and medal controls are absent.
- Active-page keyboard shortcuts have no effect after switching to passive.
- Every jury-notification type is displayed without generating an `FOPEvent`.
- The cogwheel switch preserves platform, group and presentation parameters.
- Switching back to active creates a fresh active page and restores its controls only after route authorization.
- Two simultaneous browsers, one active and one passive, remain synchronized without the passive browser mutating state.

## 8. Decisions and Risks

Decided:

- Use a separate `PassiveAnnouncerContent`, not a read-only flag on `AnnouncerContent`.
- The cogwheel switches routes/layouts outright.
- Passive jury notifications are display-only.
- Accounts mode defaults to passive; PIN mode defaults to active.
- Personalization remains available to Competition Directors.

Risks and follow-ups:

- Defaulting to passive does not prevent an authorized announcer from switching to active. Add a distinct permission only if passive-only enforcement is required.
- Configuration tab hiding without persistence enforcement would be a privilege-escalation defect.
- Sharing active announcer dialog or grid classes can accidentally reintroduce mutation handlers; share immutable presentation models/components instead.
- Existing query parameters can mutate Field of Play state on the active announcer page. The passive route must explicitly reject that behavior.
- New visible role, tab and cogwheel labels require translation keys through the repository's approved translation process.