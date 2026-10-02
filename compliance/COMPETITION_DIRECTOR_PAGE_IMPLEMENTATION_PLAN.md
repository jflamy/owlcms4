# Competition Director and Passive Speaker Implementation

Status: implemented; focused Maven verification passes. Authenticated browser acceptance is pending.

Related: [COMPETITION_DIRECTOR_PASSIVE_ANNOUNCER_ANALYSIS.md](COMPETITION_DIRECTOR_PASSIVE_ANNOUNCER_ANALYSIS.md) and [ACCESS_CONTROL_DESIGN.md](ACCESS_CONTROL_DESIGN.md).

## 1. Confirmed Behavior

Competition Director performs the same operations as an active speaker when the speaker is passive. It must not inherit a switch to passive mode.

The passive speaker is the existing announcer page with operational controls hidden or disabled, not a separately reimplemented page. All athlete information, timers, lights, grid layout and notifications remain inherited.

Jury interactions are an explicit exception: the passive speaker retains the existing interactive jury dialogs and notification dismissal. Passive does not mean that the page can never post a competition event; jury interactions may do so.

| Surface | Active Speaker | Passive Speaker | Competition Director |
| --- | --- | --- | --- |
| Athlete information and lifting-order grid | Existing layout | Same layout | Same layout |
| Timer and decision display | Visible | Visible | Visible |
| Clock and direct decision controls | Active | Hidden and disabled; no shortcuts | Active |
| Session selection, introduction and reload | Active | Disabled or hidden | Active |
| Athlete editing and attempt reversal | Active | Disabled | Active |
| Pause indicator | Interactive | Red, visible, disabled | Interactive |
| Jury dialogs and jury actions | Interactive | Interactive | Interactive |
| All speaker notifications and local dismissal | Available | Available | Available |
| Live-light cogwheel setting | Available | Available, display-only | Available |
| Speaker mode switch | To passive | To active | Absent |

## 2. Mode Defaults

| Access mode | Speaker destination | Competition Director availability when unset |
| --- | --- | --- |
| PIN | Active | Off |
| Accounts | Passive | On |

Competition Director can be enabled in PIN mode through the existing Features checkbox. The stored toggle and startup feature overrides control PIN-mode availability only. Accounts mode ignores the toggle and permits access only to Competition Director and Admin accounts.

Default speaker selection applies to Run Session navigation and the account announcer landing page. Direct active and passive URLs remain usable in either mode, subject to platform role authorization.

## 3. Implementation

- `CompetitionDirectorContent` extends `AnnouncerContent`, overrides the title and suppresses only the speaker-mode menu item.
- `PassiveAnnouncerContent` extends `AnnouncerContent`, overrides the title and returns true from `isPassiveSpeaker()`.
- `AnnouncerContent` retains its original header, grid and notification implementations. Passive checks hide or disable only the operational controls.
- `AthleteGridContent` prevents passive row editing and clock actions. The existing attempt-reversal hook is disabled for passive speakers.
- `SoundParametersReader` does not write the Field of Play single-referee setting when reading passive-page parameters.
- `JuryDecisionDialog` remains unchanged and fully interactive.
- Active clock and decision shortcuts are bound to the page lifecycle and are not registered for the passive wrapper.

Routes:

```text
lifting/announcer
lifting/announcer/passive
lifting/competition-director
```

The active and passive speaker routes use platform-bound `ANNOUNCER` access. The director route requires `COMPETITION_DIRECTOR`; Admin includes that permission. The grantable director role expands to competition-operating permissions without `ADMIN_PAGES` and is not platform-locked. The existing global access listener also checks the PIN-mode feature gate and platform authorization.

## 4. View Switching

The speaker cogwheel switches between the active and passive routes using full-page navigation. This creates a fresh UI and avoids requiring a manual refresh after the switch.

The destination receives platform, group and applicable sound, single-referee, live-light, declaration and notification-position parameters. Route authorization runs again at the destination. No mode switch is offered on the Competition Director page.

## 5. Translation Delivery

Six new keys are proposed in `shared/src/main/resources/i18n/competition_director_translations.tsv`:

- `CompetitionDirector`
- `Access.Role.COMPETITION_DIRECTOR`
- `Announcer.PassiveTitle`
- `Announcer.SwitchToPassive`
- `Announcer.SwitchToActive`
- `FeatureSwitch.competitionDirectorPage`

The managed `translation4.csv` contains five maintainer-imported feature labels. The TSV includes the new role label and the revised PIN-only feature description for the next translation import. Missing-key markers remain possible for keys not yet imported.

## 6. Verification

Focused Maven command:

```bash
mvn -pl owlcms -am -Dtest=AccessPolicyTest,ConfigTest,RouteAccessCoverageTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test
```

Coverage includes PIN-only feature gating, startup override precedence, wrapper inheritance, director/admin access, exclusion of ordinary announcer and platform accounts, and unrestricted director platform access without administrative permissions. Tests do not instantiate Vaadin UI objects.

Browser acceptance still needs an authenticated session:

1. Verify PIN opens active speaker and Accounts opens passive speaker.
2. Verify the director feature default and explicit PIN enablement.
3. Compare active, passive and director athlete headers and grids.
4. Verify passive controls remain hidden or disabled after start, stop, break, session and lifting-order updates.
5. Verify the red pause indicator is visible but cannot be clicked.
6. Verify passive row clicks, attempt clicks and active-page shortcuts cannot operate the competition.
7. Verify every notification remains dismissible and jury dialogs retain their actions.
8. Verify live-light settings affect only the passive presentation.
9. Verify both cogwheel switches take effect immediately without manual refresh and preserve parameters.
10. Verify director controls stay active and its cogwheel has no passive-mode command.

The grantable Competition Director role is implemented. Partial configuration-tab permissions remain separate work.