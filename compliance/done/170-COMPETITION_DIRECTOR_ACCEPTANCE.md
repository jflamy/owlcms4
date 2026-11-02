# Competition Director and Passive Speaker — Acceptance

Status: done. Implemented, and every browser acceptance check passed on 2026-11-02 (see
[Results](#results--accounts-mode-run-2026-11-02)). The implementation plan is archived in
[COMPETITION_DIRECTOR_PAGE_IMPLEMENTATION_PLAN.md](COMPETITION_DIRECTOR_PAGE_IMPLEMENTATION_PLAN.md)
(analysis in
[COMPETITION_DIRECTOR_PASSIVE_ANNOUNCER_ANALYSIS.md](COMPETITION_DIRECTOR_PASSIVE_ANNOUNCER_ANALYSIS.md)).

Possible follow-up, not required: an automated version of these checks in the `playwright`
module (Java Playwright, like `UpdateCheck`), launched with `OWLCMS_LOADJSON` pointing at
the `competition-director-acceptance.json` V2 fixture
([DIRECT_JSON_BOOT.md](160-DIRECT_JSON_BOOT.md)).

Configuration access (decided 2026-11-02): the Competition Supervisor needs every tab of
Language and System Settings (Language and Time Zone, Connections, Features,
Customization), so `ConfigContent` accepts `COMPETITION_SUPERVISOR` like the
Administration page. The partial, per-tab permissions proposed in the analysis are not
needed. The supervisor also gets the Security and Audit tab of the Administration page
(`Capability.SECURITY_SETTINGS`: access mode switch with the control panel token, IWF
compliance check, shared passwords). Admin only: audit key initialization and audit
integrity verification (`Capability.AUDIT_KEYS`, `AuditIntegrityCheckView`), account
management (`AccountsContent`, which could grant ADMIN) and session import
(`SessionImportContent`).

## Browser acceptance

Pages under test: **PIN speaker** (`lifting/announcer`, PIN mode — active),
**passive speaker** (`lifting/announcer/passive`, accounts-mode default for an
announcer account) and **Competition Director** (`lifting/competition-director`,
accounts mode or PIN with the feature on). Everything that works today on the
PIN speaker must keep working there; the checks below say which page each
applies to.

The PIN speaker checks (#1 PIN-mode part, #3, #7 and #9a on the PIN speaker)
are covered by the maintainer's ongoing use of the speaker page in PIN mode on
dev69 (2026-10-27). The remaining checks concern accounts mode and the passive
and Competition Director pages.

Only #1 and #2 depend on the mode: the mode chooses the default speaker page and
whether the Competition Director page is offered. The other checks test page
behavior, which is the same in both modes; they can be run in PIN mode with the
Competition Director feature switch on, opening the passive speaker from the
cogwheel or at `lifting/announcer/passive`.

Passive is a default, not a restriction: both speaker pages require the same
`ANNOUNCER` permission, so an announcer account can switch to the active
speaker from the cogwheel. This is intended. Some speakers are experienced
adults who run the platform themselves, and the competition supervisor may act
as speaker for some sessions.

### Navigation and defaults

| # | Check | Page |
| --- | --- | --- |
| 1 | Run Session and the announcer landing page open the **active** speaker in PIN mode and the **passive** speaker in accounts mode | PIN speaker, passive speaker |
| 2 | Competition Director is off by default in PIN mode, appears when the Features checkbox is ticked (or the startup override is set); in accounts mode it is available to Competition Director and Admin accounts regardless of the toggle | Competition Director |

### Same presentation on all three

| # | Check | Page |
| --- | --- | --- |
| 3 | Athlete header, timer, decision lights, lifting-order grid and notifications are identical on the three pages | all three |
| 7 | Every notification is dismissible and the jury dialogs keep their actions (jury interaction is allowed even on the passive page) | all three |

### Passive speaker is read-only

| # | Check | Page |
| --- | --- | --- |
| 4 | Clock, decision, session selection, introduction, reload, athlete editing and attempt reversal stay hidden or disabled after start, stop, break, session change and lifting-order updates | passive speaker |
| 5 | Red pause indicator is visible but cannot be clicked | passive speaker |
| 6 | Row clicks, attempt clicks and the active-page keyboard shortcuts cannot operate the competition | passive speaker |
| 8 | Live-light cogwheel setting changes only the passive presentation; it does not write the single-referee setting to the platform | passive speaker |

### Active pages keep every control

| # | Check | Page |
| --- | --- | --- |
| 9a | All speaker controls, shortcuts and the pause button work as before | PIN speaker |
| 9b | Cogwheel **switch to passive / switch to active** takes effect at once (full-page navigation, no manual refresh) and preserves platform, session, sound, single-referee, live-light, declaration and notification-position parameters | PIN speaker (both directions), passive speaker |
| 10 | All speaker controls work; the cogwheel has **no** passive-mode command | Competition Director |

## Results — accounts-mode run, 2026-11-02

Fixture `owlcms/src/test/resources/startup/competition-director-acceptance.json`
(accounts mode, jury size 0), three 1366×768 windows: announcer account on the
passive speaker, competition_supervisor account on the Competition Director
page, admin account on the active speaker. Clock and referee decisions sent over
MQTT (`owlcms/clock/A`, `owlcms/refbox/decision/A`); FOP events checked in the
server log.

| # | Result |
| --- | --- |
| 1 | Accounts half passed: the announcer login lands on `lifting/announcer/passive`, and the Announcer button on `/lifting` opens the passive speaker for the announcer and supervisor accounts. PIN half covered by the maintainer's use (see above). |
| 2 | Passed. Accounts mode: the Competition Supervisor button is shown to the supervisor and admin accounts, not to the announcer account; the supervisor login lands on the Competition Director page. PIN mode (switched live from Administration → Security, "Switch to Simple Access", with the control panel token): the button is absent while the feature switch is off, appears once Features → `competitionDirectorPage` is ticked, and opens the Competition Director page; the Announcer button still opens the active speaker. |
| 3 | Passed for idle, clock running, decision and notifications: same header, timer, lights and grid on the three pages. The down signal is not shown on the speaker pages (none of the three), which is expected. |
| 4 | Passed after start, stop, decision, lifting-order update, a marshal break and a session change (M1 → M2): the passive page keeps only the disabled session menu, the cogwheel and the disabled pause indicator, and follows the session chosen elsewhere. During a technical or marshal break the stoppage notification offers **Resume Competition** on the passive page too; this is accepted. |
| 5 | Passed: the red pause / break indicator is visible and disabled. |
| 6 | **Failed, then fixed.** Row clicks and the shortcuts (`,` `/` `.` `*` `=` `;` `Shift+=` F2 F4) posted nothing, but a click on an attempt cell opened the full, editable athlete card. Fixed in `AthleteGridContent.handleAttemptClick` (same rule as row clicks: nothing on the passive speaker). Rechecked after a clean restart: clicks on the current athlete's attempt, on a failed attempt (the lift-reversal target on active pages) and on a row open nothing on the passive page, while the active speaker still opens the card. |
| 7 | Passed. Notifications are dismissible on all three pages. With a jury size of 3 set in the competition rules, a jury deliberation (MQTT `owlcms/jurybox/break/A deliberation`) opened the same dialog (Good Lift, No Lift, Resume Competition) on the three pages; **No Lift** pressed on the passive page was applied ("entered by the Speaker"), the dialogs closed on the other two pages and lifting resumed. |
| 8 | Passed: unticking live lights on the passive page only changed its own URL (`liveLights=false`); nothing was written on the server and the active speaker's single-referee and live-light settings were unchanged. Single Referee is disabled in the passive cogwheel. |
| 9a | Passed on the active speaker (admin): start lifting, clock start and stop, 2:00, good lift, and the clock-start shortcut. |
| 9b | Passed both ways (passive → active → passive): immediate full-page navigation, `fop` and `liveLights=false` preserved. The other parameters were at their defaults. |
| 10 | Passed (competition_supervisor account): start lifting, clock start and stop, 1:00, 2:00, the `,` and `.` shortcuts and No Lift all posted their events; the cogwheel has no Speaker Controls (passive) entry. |

The run was interrupted once by a JVM crash (SIGBUS in an event-bus subscriber call)
after the hot reload of the `AthleteGridContent` fix redefined 12 classes; this
is a DCEVM/JBR hot-reload hazard, not an application fault. Checks 6 (recheck), 7
and 10 were run after a clean restart. The PIN-mode half of #2 was run on the same
server after switching to Simple Access from the Administration page.
