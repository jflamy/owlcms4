# Competition Director Page — Implementation Plan

Status: implementation plan only. No implementation is authorized by this document.

Related: [COMPETITION_DIRECTOR_PASSIVE_ANNOUNCER_ANALYSIS.md](COMPETITION_DIRECTOR_PASSIVE_ANNOUNCER_ANALYSIS.md) and [ACCESS_CONTROL_DESIGN.md](ACCESS_CONTROL_DESIGN.md).

## 1. Outcome

Add a read-only `CompetitionDirectorContent` page for following a running session without exposing competition-operating controls. Add a **Competition Director** button to the **Run Session** page.

The page availability setting has a mode-sensitive default:

| Access mode | Default when no override is stored |
| --- | --- |
| PIN | Off |
| Accounts | On |

An administrator can explicitly enable or disable the page. An explicit value overrides the mode-sensitive default and remains in effect when the access mode changes.

Page availability and page authorization are separate checks:

- availability controls whether the Run Session button and route are enabled for the installation;
- authorization controls whether the current principal may open the route for the selected platform; and
- hiding the button is not an authorization boundary.

## 2. Route and Entry Point

### 2.1 New route

Create `CompetitionDirectorContent` in the lifting package with a stable route such as:

```text
lifting/competition-director
```

The route is platform-bound. It accepts the normal `fop` display-selection parameter, but loading the route must not select, load or change the session assigned to the Field of Play.

Use the same platform access that protects the announcer information in the first iteration. The route annotation should name a base role accepted by the existing access framework; do not use button visibility or a composite-role check as route protection. If the Competition Director role is implemented in the same delivery, add a dedicated base page capability to its expansion only if access must be narrower than `ANNOUNCER`.

### 2.2 Run Session button

Update `LiftingNavigationContent.onAttach()` to add a **Competition Director** button to the operational grid that currently contains Announcer, Marshal, Timekeeper and Technical Controller.

The button:

- opens `CompetitionDirectorContent` in a new tab using the selected `fop`;
- uses a distinct, familiar icon, such as `VaadinIcon.EYE`;
- is created only when the effective page-availability setting is enabled; and
- remains subject to the existing role-aware navigation and route access checks.

Do not replace or redirect the existing Announcer button. The active Announcer page remains available at its existing URL.

## 3. Mode-Sensitive Availability

### 3.1 Configuration model

Add a feature identifier such as `competitionDirectorPage` under the user-interface feature section. The current `FeatureSwitch` model has a fixed boolean default, so it cannot directly represent an unset value whose default depends on `Config.isAccountsMode()`.

Extend the feature API narrowly:

1. Preserve the stored feature map as the source of explicit overrides.
2. Add a way to distinguish “configured false” from “not configured”.
3. Resolve the effective Competition Director page value as:

```text
explicit override, when present
otherwise Config.isAccountsMode()
```

4. Keep existing feature switches and their fixed defaults unchanged.

The environment/startup feature-switch override remains the highest-priority value, consistent with the existing effective-feature map.

### 3.2 Features UI

Show the setting in the Features tab with three meaningful states:

- **Use access-mode default**;
- **Enabled**; and
- **Disabled**.

A plain checkbox is insufficient because clearing an explicit override must be possible. Implement a small select or radio group for this switch rather than changing every existing feature row.

Display the currently resolved default in the description so administrators understand that PIN defaults to off and Accounts defaults to on. Changing Accounts/PIN mode must not rewrite an explicit override.

### 3.3 Disabled behavior

When effectively disabled:

- omit the Competition Director button from Run Session; and
- reject direct navigation to the route without constructing or attaching the page.

Prefer a reusable availability guard integrated with navigation authorization. If the first implementation uses a route lifecycle check, forward to the access-denied page before registering event listeners or building page controls.

## 4. Read-Only Page Construction

Implement the page as a dedicated read-only view. Do not subclass `AthleteGridContent` or instantiate `AnnouncerContent`, because both expose mutation paths.

The page displays:

- selected platform and current session;
- current athlete, start number, attempt and requested weight;
- authoritative countdown using `PassiveTimerElement`;
- referee lights using passive decision components;
- lifting order and completed attempts through a dedicated read-only grid; and
- jury notifications through display-only components.

The page must not create or register:

- clock, decision, break, resume or start-lifting controls;
- session-switch, group-load or reload actions;
- athlete edit, attempt clear or attempt reversal handlers;
- medal or ceremony actions;
- active-announcer keyboard shortcuts; or
- any handler that posts an `FOPEvent`, saves an athlete or mutates `FieldOfPlay` state.

Extract immutable presentation models or narrowly reusable passive components where the active Announcer page already computes suitable display data. Do not share active dialogs or handlers merely to avoid duplication.

## 5. Jury Notifications

Create a passive jury-notification presenter that handles summons, deliberation or challenge, technical pause, loading error, verdict, reason and end events.

It may close automatically on the corresponding end event and may offer a browser-local Dismiss action. It must not post `JuryDecision`, `StartLifting` or acknowledgement events, and it must not bind Enter or another shortcut to a competition action.

Keep active and passive notification behavior in separate controller code. Share only immutable event-to-display-text mapping where useful.

## 6. Access-Control Integration

If delivered together with the proposed Competition Director role:

1. Add the grantable composite role and its all-platform competition-role expansion.
2. Add an explicit policy concept for principals that are not platform-locked, rather than treating the role as Admin.
3. Keep `ADMIN_PAGES` out of the role expansion.
4. Test the page capability independently from configuration and account-management permissions.

PIN-mode officials retain their existing access behavior. Enabling the page in PIN mode makes the new button and route available under the selected base-role rule; it does not create a new PIN or alter PIN authentication.

## 7. Navigation Between Active and Read-Only Views

Add a cogwheel command on the active Announcer page to open the Competition Director page, and a reciprocal command on the Competition Director page to open active Announcer controls.

Preserve applicable query parameters, including `fop`, sound, light and notification-presentation preferences. Re-run route authorization and availability checks at the destination. Navigating away from active Announcer must detach its shortcuts and mutation handlers.

Do not automatically rewrite an already open browser tab when Accounts/PIN mode or the availability override changes.

## 8. Translation Work

Add translation keys for:

- Competition Director page and button title;
- feature setting title and description;
- Use access-mode default, Enabled and Disabled options; and
- active/read-only view-switch commands.

Follow the repository translation workflow: read the language header from `translation4.csv`, create a separate tab-delimited import file under `shared/src/main/resources/i18n/`, and do not edit `translation4.csv` directly.

## 9. Implementation Sequence

1. Add focused tests for mode-sensitive availability and explicit override precedence.
2. Add the feature identifier, tri-state persistence API and Features-tab control.
3. Add route-level availability enforcement and its access tests.
4. Build the passive current-lift header, timer and decision-light presentation.
5. Add the read-only lifting-order grid and prove that row interaction cannot edit data.
6. Add passive jury-notification handling and event-side-effect tests.
7. Add `CompetitionDirectorContent` and the conditional Run Session button.
8. Add reciprocal cogwheel navigation with query-parameter preservation.
9. Add translations through the approved TSV workflow.
10. Perform focused Java validation, then browser acceptance checks in both access modes.

Each page slice should be validated before adding the next. This limits the risk of accidentally importing mutation behavior from the active Announcer implementation.

## 10. Focused Tests

### Configuration tests

- Unset setting resolves false in PIN mode.
- Unset setting resolves true in Accounts mode.
- Explicit true resolves true in both modes.
- Explicit false resolves false in both modes.
- Clearing the override restores the current mode-derived default.
- Environment/startup override has the documented precedence.
- Saving unrelated feature switches preserves the tri-state value.

### Access and navigation tests

- Disabled page is absent from Run Session and direct navigation is rejected.
- Enabled page button carries the selected `fop` and opens the new route.
- A principal lacking the route role cannot open the route even when the feature is enabled.
- A platform-scoped principal cannot use a crafted `fop` parameter to cross platforms.
- PIN-mode official behavior remains unchanged when the feature is disabled by default.
- Accounts mode shows the button by default without changing existing Announcer navigation.

### Read-only behavior tests

- Timer, lights, current athlete and lifting order follow Field of Play updates.
- Clicking rows or attempts does not open an editor or mutate an athlete.
- No clock, decision, break, reload, group-switch or ceremony controls exist.
- Active-page keyboard shortcuts have no effect on the Competition Director page.
- Every jury notification renders without posting an `FOPEvent`.
- One active Announcer browser and one Competition Director browser stay synchronized while only the active browser can mutate state.

### Regression tests

- Existing Announcer route, Run Session button and controls remain unchanged.
- Existing fixed-default feature switches retain their current persistence behavior.
- Switching Accounts/PIN mode does not overwrite an explicit page-availability override.
- Admin, PIN officials and existing account roles retain their current route access except for the intentionally added page.

## 11. Browser Acceptance Matrix

| Mode | Stored override | Run Session button | Direct route |
| --- | --- | --- | --- |
| PIN | Unset | Hidden | Rejected |
| Accounts | Unset | Shown to authorized principal | Allowed to authorized principal |
| PIN | Enabled | Shown to authorized principal | Allowed to authorized principal |
| Accounts | Disabled | Hidden | Rejected |

For every allowed case, verify at least two platforms and a crafted unauthorized `fop` value. Verify desktop layout at 1920x1080 and a narrow viewport, with no overlap in the current-lift header, timer, decision lights or lifting-order grid.

## 12. Completion Criteria

The change is complete when:

- the availability matrix above is enforced by both navigation and direct routing;
- the Run Session page contains the conditional Competition Director button;
- the new page presents all required live session information;
- static review and tests find no state-changing handler reachable from the new page;
- active Announcer behavior has no regression;
- focused Java checks pass; and
- browser checks confirm synchronized, read-only behavior in PIN and Accounts modes.