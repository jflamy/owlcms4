# Compliance table summaries

Extracted from `OTIS_Proposal_01.docx`. Fill in the `R:` (requirement) and `J:`
(justification) lines; keep each on one line. Do not edit headings or hash comments.
Empty summaries keep the original text when reinjected.
`FIX:` lines flag problems to correct in the proposal itself; delete a `FIX:` line
once the proposal is corrected.

## Open problems in the proposal

- ⚠️ **3-R04**: Justification: "checked by the pro" looks truncated ("program"?).
- ⚠️ **3.1-R02**: Justification typo: "The 20kg rules has".
- ⚠️ **3.1-R05**: Justification typo: "The marshal screen have".
- ⚠️ **4.1-R02**: Justification typo: "overriding the durations are required" (as required).
- ⚠️ **4.2-R03**: Justification typo: "for exame" (example).
- ⚠️ **4.3-R04**: Justification typo: "satifsfactory".
- ⚠️ **4.3-R07**: Justification typo: "weigh-in Tos" (TOs).
- ⚠️ **4.3-R11**: Justification typo: "is the as the speaker" (missing "same").
- ⚠️ **4.3-R15**: Justification typo: "A row at the bottom carries will carry".
- ⚠️ **4.4-R12**: Justification typo: "contentsis".
- ⚠️ **4.4-R15**: Justification ends without a period ("The export is signed").
- ⚠️ **5-R07**: Justification contains two TODO notes about pictures (Figures 3–4).
- ⚠️ **5-R15**: Justification is truncated: "OWLCMS runs on Windows, macOS,".
- ⚠️ **5-R16**: Justification wording: "routed along shared with video distribution".
- ⚠️ **5.1**: Justification has a stray "." paragraph.
- ⚠️ **6.3**: Justification only repeats the requirement; no actual answer.
- ⚠️ **6.4**: Justification only repeats the requirement; no actual answer.
- ⚠️ **7.4**: Justification starts by repeating the requirement text.
- ⚠️ **7.5**: Justification starts by repeating the requirement text.
- ⚠️ **10-S03**: Justification typo: "Baseline will managed".
- ⚠️ **12.2-R01**: Justification is an incomplete sentence: "Will be treated the same as minor cosmetic".

# RFP Section 2

## 2.1 | B.2.1
<!-- hash: abc6d5677787 -->

**Requirement** (RFP §2.1)

> Complete sport logic, mandatory FOP controls, screens and official documents.

R: M0: sport logic, FOP controls, screens, documents.

**Justification**

> OWLCMS version 69 and Blue Owl version 3 are fully compliant with the TCRR as of October 2026.

J: OWLCMS 69 and Blue Owl 3 are fully compliant with the TCRR.

## 2.1-R02 | B.2.2
<!-- hash: d61eb12c10e5 -->

**Requirement** (RFP §2.1)

> M0 plus baseline control, audit, interfaces, offline operation, recovery, procedures, training, support and spares.

R: M1: control, audit, interfaces, offline, recovery, support.

**Justification**

> The system as delivered will meet all the stated requirements. OWLCMS includes a module specifically meant for the creation of external data interfaces. The hardware devices use documented protocols. Baseline control will be according to section K.1.2.

J: The delivered system meets all requirements, with documented interfaces and baseline control per K.1.2.

## 2.1-R03 | B.2.3
<!-- hash: 4110443078ab -->

**Requirement** (RFP §2.1)

> M1 plus applicable VPT, public/live services, broadcast outputs, enhanced monitoring, high availability and full rehearsal.

R: M2: VPT, live/broadcast outputs, monitoring, HA, rehearsal.

**Justification**

> Video playback uses video-capable equipment that is included in the proposal (laptops with video capture/gaming cards, cameras). OWLCMS has its own standalone VPT capabilities and provides a core set of TV Graphics for broadcasts. OWLCMS replay streams for each lift can be fetched from the replay server once networking access configurations are done. Furthermore, Supplier can create additional TV graphics for specific event requirements; the actual creation and testing of such items would have to be negotiated with the supplier

J: Standalone VPT, a replay server and core TV graphics are included.

## 2.1-R04 | B.2.4
<!-- hash: 568201c1b30f -->

**Requirement** (RFP §2.1)

> M2 plus approved Games/ORIS and programme-specific integrations.

R: M3: Games/ORIS and programme integrations.

**Justification**

> OWLCMS includes a fully documented external interface data format and sample applications that leverage it. All competition data, and every single field-of-play event (weight changes, decisions, etc.) are available for propagation on the Games feeds.
> OWLCMS has previously been integrated with Olympic Data Format feeds using that approach.
> The interfacing capability is an integral part of the proposal; the actual creation and testing of specific interfaces for specific external formats or events would have to be negotiated with the Supplier.
> Figure 1: Solution Overview
> Figure 2: Detail: VPT and Interfaces

J: A documented data feed, already used for ODF, supports integrations; event-specific work is negotiable.

## 2.1-R05 | B.2.5
<!-- hash: 63fba9719902 -->

**Requirement** (RFP §2.1)

> Simultaneous platform operation with separation and capacity evidence.

R: MP: simultaneous platforms with capacity evidence.

**Justification**

> OWLCMS has been used in actual competitions with up to 6 platforms simultaneously and 1600 athletes competing, with extremely low computer and network usage.
> When running multiple platforms, a single instance of OWLCMS runs them all together, and no integration step for results is required. All results and rankings are available live for all platforms. The normal constraints expressed in TCRR (group B should finish before group A for a given category) should nevertheless be respected so coaches get meaningful information.
> The program is verified with a stress test that simulates competition updates as fast as possible in real life, using the entry totals to generate credible lift values. Any competition can be simulated as soon as preliminary registration data is available to confirm expected performance and validate reports

J: One OWLCMS instance has run 6 platforms and 1600 athletes, and capacity is verified by stress test.

## 2.2 | B.3.1
<!-- hash: d75c3ab130bc -->

**Requirement** (RFP §2.2)

> The licence certificate shall state the supplier and product.

R: Certificate states supplier and product.

**Justification**

> The supplier of the product shall be [Supplier], who will act as integrator of the OTIS solution and any additional components required to meet the claimed profiles as per requirements.

J: [Supplier] is the supplier and acts as OTIS integrator.

## 2.2-R02 | B.3.2
<!-- hash: 80edac2bb2ab -->

**Requirement** (RFP §2.2)

> The licence certificate shall state the exact baseline.

R: Certificate states exact baseline.

**Justification**

> Any stable version of OWLCMS after version 69 is claimed to meet the current requirements and is fully compliant with TCRR as published at the time of this tender. Any version of the Blue Owl software after version 3.0 meets the requirements. Baseline will be established at acceptance and evolve according to section K.1.2.

J: The baseline is OWLCMS 69+ and Blue Owl 3.0+, fixed at acceptance.

## 2.2-R03 | B.3.3
<!-- hash: 52e0e2876a7c -->

**Requirement** (RFP §2.2)

> The licence certificate shall state approved profiles and capacities.

R: Certificate states profiles and capacities.

**Justification**

> M2 and MP profiles: The system complies with the M2 and MP profiles, up to 6 platforms. For the purpose of this license, the number of participants shall be 1800 or less. There is no intrinsic limitation on the number of participants; this number is given as a safe, previously observed figure with a margin of safety.
> M3 profile: Integration capabilities with external systems are intrinsic; actual integration and testing with external systems are not included in this proposal. There are no exclusions preventing IWF from providing the live integration feed to a qualified party.

J: M2 and MP are supported up to 6 platforms and 1800 participants; M3 integration is negotiable.

## 2.2-R04 | B.3.4
<!-- hash: 05aef7ee6699 -->

**Requirement** (RFP §2.2)

> The licence certificate shall state interfaces and critical dependencies.

R: Certificate states interfaces and dependencies.

**Justification**

> The system has no critical dependencies. All communications use the standard HTTP and MQTT protocols.
> The specific MQTT messages used for the devices are fully documented. The physical interfaces for devices are USB and RJ-45 connectors. Only commercially available cabling (CAT-6, USB 2.0, HDMI) is used.
> The protocol used to create additional interfaces for Data Exports, TV Graphics, etc. is public and has a published reference implementation.

J: There are no critical dependencies; only standard HTTP/MQTT and documented device protocols are used.

## 2.2-R05 | B.3.5
<!-- hash: fc4c9efa84f2 -->

**Requirement** (RFP §2.2)

> The licence certificate shall state limitations and deviations.

R: Certificate states limitations and deviations.

**Justification**

> See section B.3.3 for context on profile M3. No limitations on profiles M0, M1, M2 and MP.

J: There are no limitations for M0, M1, M2 and MP; M3 is described in B.3.3.

## 2.2-R06 | B.3.6
<!-- hash: 84020ec76e0b -->

**Requirement** (RFP §2.2)

> The licence certificate shall state validity and surveillance conditions.

R: Certificate states validity and surveillance.

**Justification**

> The certificate will be valid for 4 years after date of issuance.

J: The certificate is valid for 4 years from issuance.

# RFP Section 3

## 3-R01 | E.1.1
<!-- hash: 0c976fcbdbf0 -->

**Requirement** (RFP §3)

> Import and validate athlete, federation/NOC, official, event, venue and record data; stable IDs and error reporting.

R: Import and validate entry, official and record data.

**Justification**

> Registration data is converted to an Excel-style sheet with the athlete identifier, name, and lifting data. The input format handles multiple age groups and supports team allocation. The intake format can be exported, edited offline, and re-ingested without loss of information.
> The same applies to schedule changes and referee assignments that can be modified in spreadsheet form until the drafts are considered stable.
> All information is stored in a relational database. The program performs validation when loading to ensure integrity.

J: Spreadsheet imports are validated and can be round-tripped before loading into a relational database.

## 3-R02 | E.1.2
<!-- hash: 6b7e9d1e294c -->

**Requirement** (RFP §3)

> Corrections, approvals, random lots, grouping, timetable and Technical Official assignments.

R: Corrections, lots, groups, timetable, TO assignments.

**Justification**

> The program prints VFE verification sheets that show exactly which athlete is in which category or categories (see section E.2.4.2). This allows the competition director to verify that eligibility rules have been respected, and to know in advance what information will be required at VFE (e.g., which teams need to remove substitute athletes from their roster). The program also prints team membership sheets to verify that all team allocations are correct. Team representatives can sign these sheets to support the VFE process and reduce errors. The TIS team uses these sheets for data capture.
> The database includes a list of technical officials and their team assignments. A referee team timetable is uploaded and can be edited interactively. In addition, individual assignment changes can then be made as required (e.g., substituting one referee for another for a given session).

J: VFE and team verification sheets are printed, and the referee timetable is editable.

## 3-R03 | E.1.3
<!-- hash: d1cece65c8ce -->

**Requirement** (RFP §3)

> Approved Start List and controlled distribution/versioning.

R: Approved, versioned Start List distribution.

**Justification**

> The solution includes the OWLCMS modules for producing a complete start book. A preliminary version can be published to list participants in each category, with subsequent revisions adding the schedule and the officials’ assignments. There are also spreadsheet formats for the actual schedules and start lists for convenient publishing.
> Records are loaded in OWLCMS using a spreadsheet (they are converted from the data made available by the federations).
> For convenience, individual documents are available for the competition schedule, for referee assignments, for start lists, etc.

J: A start book is produced with incremental revisions, plus schedule and start list exports.

## 3-R04 | E.1.4
<!-- hash: 55582776255c -->
FIX: Justification: "checked by the pro" looks truncated ("program"?).

**Requirement** (RFP §3)

> Correct timing, bodyweight, first attempts, 20 kg checks, start numbers, signatures and closed snapshot.

R: Weigh-in: bodyweight, first attempts, 20 kg, signatures.

**Justification**

> OWLCMS produces field-tested weigh-in forms to capture both the weight-on-scale and deducted weight. The form captures all the required signatures. Compliant athlete cards are printed.
> During data entry, all applicable rules are checked by the pro, including 20 kg and qualifying totals where applicable. Compliant athlete cards are likewise printed. A special data entry mode is used for quick capture of weigh-in data.
> Start numbers are cross-checked at the end of the weigh-in and checked against allocated bib numbers. A speaker introduction sheet for athletes and officials is produced, as are the empty protocols and jury member sheets. The competition director takes a snapshot copy of the database at this point.

J: Weigh-in forms and athlete cards are produced, 20 kg rules are checked, and a snapshot is taken.

## 3-R05 | E.1.5
<!-- hash: 34e4411509d5 -->

**Requirement** (RFP §3)

> Calling order, declarations/changes, 60/120-second timing, referee decisions, records and live outputs.

R: Snatch flow: order, changes, timing, decisions, records.

**Justification**

> The system fully complies with the competition flow.
> All marshal data entry is immediately handled by the system and all lifting order or requested weights are instantly updated on all displays and scoreboards. Referee decisions and jury reversals are automatically handled, and all scoreboards and attempt boards are immediately updated.
> All the normal flow timing decisions are automatic (the clock is automatically set to the right allocated time for each athlete, including on fast sequences of changes). Only explicit loading incidents require the use of the 1:00 or 2:00 reset buttons.
> The 20 kg rule is systematically checked and any deviation is explicitly visible to the marshal (including the necessary amounts for corrections).
> Records attempted are shown as such, and any record improved is registered in the competition database.

J: Lifting order, clock, decisions, 20 kg checks and records are handled automatically.

## 3-R06 | E.1.6
<!-- hash: f324c9afb698 -->

**Requirement** (RFP §3)

> Controlled Snatch-to-Clean & Jerk transition and synchronized displays.

R: Controlled snatch to C&J transition.

**Justification**

> Transition from the snatch to the clean & jerk break at the end of the snatch is automatic. Jury deliberation or challenge on the very last snatch lift is correctly handled (the snatch is not done until resolution).
> Default rules for longer breaks (few athletes) and shorter breaks (large sessions) may be given as part of the event-specific rules. Break durations can also be changed during the meet, at any time before the break start. The break duration can also be set at break time in case of a last minute change.

J: The transition is automatic and break durations are configurable at any time.

## 3-R07 | E.1.7
<!-- hash: b70a98a88d0c -->

**Requirement** (RFP §3)

> Calling order, weight changes, 20 kg enforcement, totals, ranks and records.

R: C&J flow: order, changes, totals, ranks, records.

**Justification**

> See section E.1.5 Snatch; the Clean & Jerk flow is completely automated in the same way.

J: The Clean & Jerk is automated exactly like the Snatch (E.1.5).

## 3-R08 | E.1.8
<!-- hash: 0fdacb72bb6a -->

**Requirement** (RFP §3)

> STOP, Jury, challenge/VPT where applicable, additional attempts, correction, withdrawal and restart.

R: STOP, Jury, challenge, corrections, restart.

**Justification**

> The jury can start a deliberation or accept a challenge using a physical jury keypad button or using on the jury control display.
> A flashing STOP sign is shown to the athlete on the platform, and all attempt boards and displays indicate the interruption.
> Transmission of the jury reversal code to the speaker can be done the traditional way (by the technical controller), or automatically when the jury president chooses the reversal code. In either case, the jury or challenge decision is shown as the speaker announces it.
> The system can optionally display jury decisions on the scoreboards and the attempt boards (like IWF has been doing experimentally in its own championships). This is optional as it is not part of TCRR.

J: The jury uses a keypad or screen, STOP flashes on all displays, and reversals reach the speaker.

## 3-R09 | E.1.9
<!-- hash: b07d55600623 -->

**Requirement** (RFP §3)

> Protocol reconciliation, signatures/status, results, classifications and records.

R: Protocol, results, classifications and records.

**Justification**

> The session protocol sheet is available immediately after the session for signature by the jury president. It contains all results, ranking information and each occurrence of improved records.
> Should any correction be required, the athlete card can be adjusted from the results page, with full traceability in the audit log. Errors in registration data (for example, wrong eligibility for a record) can be fixed in the same way, and records re-computed prior to emitting an amended protocol for signature.

J: The protocol is ready at session end, and corrections are audited with records recalculated.

## 3-R10 | E.1.10
<!-- hash: 9420f137de61 -->

**Requirement** (RFP §3)

> Medallists, public/broadcast outputs, communications and revised outputs.

R: Medallists, public/broadcast and revised outputs.

**Justification**

> At the end of a session, a medal sheet is available for the speaker. The medal sheet correctly reports the categories that are actually finished (important when A and B groups from different bodyweight categories are together in the same sessions). Combined category results are also available (showing the ranking for all combined A/B/C… groups)
> The system has special purpose scoreboards for ceremonies.
> Public scoreboards are normally styled to match the event colours, and switch content during ceremonies. OWLCMS will automatically switch during introductions. During medal ceremonies, the technical table selects which category is receiving medals so the display switches
> Broadcast-oriented scoreboards are typically visually similar to public scoreboards but styled differently (typically a transparent format is used). If the video producer selects the medal display, it will track what the technical table selects as the current medal ceremony.

J: Medal sheets, ceremony scoreboards and broadcast-styled displays are provided.

## 3-R11 | E.1.11
<!-- hash: b9e9aafe754d -->

**Requirement** (RFP §3)

> Final dataset, official outputs, logs, revisions, evidence and machine-readable export.

R: Final dataset, logs, evidence, machine-readable export.

**Justification**

> The full database content is exported in JSON format. This format is suitable for archival – OWLCMS can read archives produced several years ago.
> The archived documents also include
> A final result package in Excel format (all categories, best athlete rankings, team points, medals)
> A spreadsheet document with the protocols for all individual sessions
> The full “publication format” results book
> The audit logs

J: The archive includes the JSON export, results package, protocols, results book and audit logs.

## 3.1-R01 | E.1.12.1
<!-- hash: 9d33824fc598 -->

**Requirement** (RFP §3.1)

> Snatch followed by Clean & Jerk, maximum three attempts per lift, correct Total calculation.

R: Snatch then C&J, three attempts, correct Total.

**Justification**

> The database explicitly stores 3 attempts per lift and the associated information. The state automaton enforces the sequence. The total calculation rules are subject to automated tests.

J: The state machine enforces the sequence, and Total calculation is covered by automated tests.

## 3.1-R02 | E.1.12.2
<!-- hash: 89486e8b3358 -->
FIX: Justification typo: "The 20kg rules has".

**Requirement** (RFP §3.1)

> Whole-kilogram weights and applicable minimum starting weights.

R: Whole-kg weights and minimum starting weights.

**Justification**

> The database explicitly stores 3 integer values for the weights. The 20kg rule is systematically verified on any declaration or weight change. The 20kg rules has its own suite of automated tests.

J: Weights are stored as integers, and the 20 kg rule is checked on every change and tested.

## 3.1-R03 | E.1.12.3
<!-- hash: 4db52d5ec987 -->

**Requirement** (RFP §3.1)

> Correct calling order, including weight, attempt number, previous-attempt sequence and start number.

R: Correct calling order.

**Justification**

> The calling order is explicitly programmed to determine the next athlete based on the 4 TCRR criteria.

J: The calling order implements all four TCRR criteria.

## 3.1-R04 | E.1.12.4
<!-- hash: 47c6b88a307c -->

**Requirement** (RFP §3.1)

> Permitted weight changes, final-call and consecutive-attempt rules.

R: Weight change, final-call, consecutive-attempt rules.

**Justification**

> The state automaton remembers the athlete for which the clock was last started. It correctly resumes the clock at the correct value when several intervening changes bring back the lifting order to that athlete. The state automaton also determines correctly two-minute situations.
> Violations of the late-change delays are signalled to the marshal. The marshal can accept a change that was signed in time but could not be typed in fast enough.
> Furthermore, to determine violations when an athlete is moving down but should already have lifted, the system also checks the full correct prior sequence on every lift.

J: The clock resumes correctly, late changes are flagged, and the prior sequence is verified.

## 3.1-R05 | E.1.12.5
<!-- hash: 8c8435844297 -->
FIX: Justification typo: "The marshal screen have".

**Requirement** (RFP §3.1)

> Withdrawals, no-lift/no-Total conditions, disqualification and additional attempts.

R: Withdrawals, no-lift/no-Total, DQ, extra attempts.

**Justification**

> The marshal screen have explicit buttons to mark withdrawals of either the snatch, or of both events. The computation rules correctly register no lifts and compute no totals. The system tracks the reasons for disqualification. If an attempt is given again, the audit trail will show the edition of athlete data to match the changes written on the athlete card.

J: Withdrawals, no-Totals and DQ reasons are recorded, and re-attempts are audited.

## 3.1-R06 | E.1.12.6
<!-- hash: a6abfd254494 -->

**Requirement** (RFP §3.1)

> Material corrections recalculate affected rankings, records and outputs.

R: Corrections recalculate rankings, records, outputs.

**Justification**

> Any change to the athlete card data for any athlete causes a recalculation of all affected items. Recalculation of records can be triggered on the records management page. Reports and result books can be regenerated immediately afterwards.

J: Any card change triggers recalculation, and reports regenerate immediately.

# RFP Section 4

## 4.1-R01 | E.2.1.1
<!-- hash: 23090f84d3b4 -->

**Requirement** (RFP §4.1)

> Authoritative electronic countdown in one-second increments, configurable up to at least 15 minutes.

R: Authoritative 1-second countdown up to 15 minutes.

**Justification**

> Athlete countdown information is updated at one-second increments on the athlete-facing clock, on all attempt boards, and on scoreboards. Breaks can be configured to any duration.

J: All clocks update every second, and breaks can have any duration.

## 4.1-R02 | E.2.1.2
<!-- hash: 1e2536fad82c -->
FIX: Justification typo: "overriding the durations are required" (as required).

**Requirement** (RFP §4.1)

> Automatic 60- and 120-second attempt clocks and applicable introduction/break countdowns.

R: Automatic 60/120-second and break countdowns.

**Justification**

> Clock ownership is automatic; no intervention is required to set the clock to 1:00, 2:00 or to the time remaining for the athlete.
> The system has break countdowns prior to introduction, prior to first snatch and prior to first clean & jerk. A break management dialog in the user interface does the automatic transition between these countdowns, and also allows overriding the durations are required.

J: Attempt clocks are set automatically, and break countdowns are managed with overrides.

## 4.1-R03 | E.2.1.3
<!-- hash: d0040e8b47a8 -->

**Requirement** (RFP §4.1)

> Correct start, stop, restart and STOP behaviour according to the rules.

R: Correct start, stop, restart and STOP behaviour.

**Justification**

> The timekeeper start, stop and restart behavior is according to TCRR.

J: Start, stop and restart follow the TCRR.

## 4.1-R04 | E.2.1.4
<!-- hash: e59e2dd868e8 -->

**Requirement** (RFP §4.1)

> All connected displays use the same authoritative time state within the approved tolerance.

R: All displays share one authoritative time.

**Justification**

> Time is kept on the OWLCMS server. All devices are time-synchronized to universal time. When a timer countdown is sent to remote browsers for display, the actual universal time on the server is included so the remote machine can determine how much propagation drift took place and adjust the countdown as needed.

J: Time is kept on the server, and displays compensate for propagation drift.

## 4.1-R05 | E.2.1.5
<!-- hash: 8a195c8df02d -->

**Requirement** (RFP §4.1)

> Time corrections are authorised, recorded and auditable.

R: Time corrections authorised and auditable.

**Justification**

> All machines are configured NTP-synchronized at the operating system level, and an NTP source is configured on the network as part of the system setup. Time drift corrections are logged in the operating system journals. A manual resynchronization should be treated the same as an equipment replacement and logged manually by the competition director, as a procedural requirement beyond the scope of the competition automated logs.

J: All machines are NTP-synchronized, and corrections are logged by the operating system.

## 4.2-R01 | E.2.2.1
<!-- hash: 037f480a921b -->

**Requirement** (RFP §4.2)

> Three independent referee control boxes with white/red decisions and required indicators.

R: Three referee boxes with decisions and indicators.

**Justification**

> Blue Owl referee buttons have visual indicator, sound, and vibration.

J: Blue Owl buttons provide light, sound and vibration.

## 4.2-R02 | E.2.2.2
<!-- hash: f03be4bc729e -->

**Requirement** (RFP §4.2)

> Correct majority/Down signal, reminder and reversal-window behaviour.

R: Correct Down signal, reminder, reversal window.

**Justification**

> Implemented by the state automaton. 3-second reversal window is enforced as per TCRR. Should a late reversal be done using flags, the competition director can easily perform the reversal. See section E.2.3.8 for an example of reversal.

J: The state machine enforces the TCRR 3-second reversal window.

## 4.2-R03 | E.2.2.3
<!-- hash: fc222f89870f -->
FIX: Justification typo: "for exame" (example).

**Requirement** (RFP §4.2)

> Jury visibility of individual referee decisions before public reveal.

R: Jury sees referee decisions before public reveal.

**Justification**

> Live referee decisions and reversals are shown to the jury both on the keypad and on the jury control screen. See section E.2.3.10 for exame

J: Live decisions are shown on the jury keypad and jury screen.

## 4.2-R04 | E.2.2.4
<!-- hash: 77d51a09f26f -->

**Requirement** (RFP §4.2)

> Support for approved three- and five-member Jury configurations.

R: Three- and five-member Jury support.

**Justification**

> Jury size is selectable (3, 5 jury members or no jury)

J: Jury size is selectable: 3, 5 or none.

## 4.2-R05 | E.2.2.5
<!-- hash: c5c2977e1d9e -->

**Requirement** (RFP §4.2)

> Individual Jury decision controls and protected independence between members.

R: Independent individual Jury controls.

**Justification**

> Individual jury buttons are provided as per TCRR. Decision protection housings are not currently a TCRR requirement; [Supplier] would provide some acceptable housing after clarification of IWF expectations.

J: Individual jury buttons are provided; housings will follow IWF clarification.

## 4.2-R06 | E.2.2.6
<!-- hash: 6dd28fea187d -->

**Requirement** (RFP §4.2)

> Material decisions and actions are attributable and time-stamped.

R: Decisions attributable and time-stamped.

**Justification**

> There is a full audit log of every single decision, timestamped to the millisecond, with the originating station or device.

J: Every decision is logged to the millisecond with its originating device.

## 4.3-R01 | E.2.3.1
<!-- hash: 8d901fdcb7fc -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide System health.

R: System health view.

**Justification**

> A technical log of the system is constantly updated and can be monitored. This includes technical indicators on the environment, notably CPU and memory usage. The normal operating system gauges can also be used. Note that OWLCMS is typically well under 10% of CPU usage, and under 2GB of memory even for very large events, so the operating system gauges are usually entirely sufficient.

J: A live technical log reports CPU and memory use.

## 4.3-R02 | E.2.3.2
<!-- hash: c722406b7edf -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Event setup.

R: Event setup view.

**Justification**

> All competition information setup is done using interactive screens. Green is used in the software to indicate common actions.

J: All setup is done through interactive screens.

## 4.3-R04 | E.2.3.2.2
<!-- hash: 99c84c5e95c5 -->
FIX: Justification typo: "satifsfactory".

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide lots/groups/timetable.

R: Lots, groups and timetable view.

**Justification**

> For an IWF event, it is expected that a spreadsheet or CSV format of registration data will be available. This format is then converted to the OWLCMS format that contains the expected information, including athlete identifiers.
> The OWLCMS registration format also includes a sheet to define the schedule. In this way, the initial schedule and session assignment can be done in a spreadsheet using whatever means the organisers are comfortable with.
> [Supplier] has its own software for creating such initial schedules and session/group assignments
> OWLCMS then loads the registration data and validates it. OWLCMS can re-export registration data and adjustments can be made on the export, and re-loaded. Such round-trips can be made until satifsfactory. Once stable the registration data is editable directly in the application.
> The registration screen allows modification of the allocated session, moving the athlete to another group, adjusting eligibility to multiple age groups as appropriate.
> The other tabs on the registration form cover things like record eligibility (in joint jurisdiction championships, not all athletes are eligible to improve all records)
> The session timetable is editable.

J: Registration and schedule are imported from spreadsheets, round-tripped, then edited in the application.

## 4.3-R07 | E.2.3.4
<!-- hash: 7316679220ec -->
FIX: Justification typo: "weigh-in Tos" (TOs).

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Weigh-in.

R: Weigh-in view.

**Justification**

> The weigh-in data entry form is intentionally the same format as the registration form. All relevant checks for the 20 kg rule are applied at data entry, in case they were missed by the weigh-in Tos.
> After weigh-in, the page allows the creation of all the relevant forms for the speaker and for the jury.

J: Weigh-in entry mirrors registration, applies 20 kg checks, and produces speaker and jury forms.

## 4.3-R08 | E.2.3.5
<!-- hash: 3b2e71b8e6c3 -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Chief Marshal.

R: Chief Marshal view.

**Justification**

> The marshal table has a station that shows all athletes in start order (or lifting order if so preferred). Clicking an athlete brings up a card laid out exactly like the athlete card. Focus is automatically brought to the right cell, so typing the desired value and hitting Enter works immediately for fast data entry.
> 20kg rule violations and late changes are immediately made visible and prevent the card from closing.

J: The marshal screen mirrors the athlete card for fast entry, and violations block closing.

## 4.3-R09 | E.2.3.6
<!-- hash: 9f149c532b4b -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Timekeeper.

R: Timekeeper view.

**Justification**

> During the competition a tablet-friendly display is used, that can be used together with the Blue Owl buttons, as preferred.
> The timekeeper can control the breaks (in addition to the main technical table or the speaker having the same feature depending on the skill and comfort levels). The delay can be set to a clock or duration as relevant for the type of break.

J: The timekeeper uses a tablet display or Blue Owl buttons and can control breaks.

## 4.3-R11 | E.2.3.8
<!-- hash: 7ea4ef4ee0e3 -->
FIX: Justification typo: "is the as the speaker" (missing "same").

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Competition supervisor.

R: Competition supervisor view.

**Justification**

> The competition supervisor screen is the as the speaker, with the addition of the decision lights that can be seen live as an option. This is because the switches between sessions, and breaks, and lift overrides after a late referee flag call can all be done from that screen. Depending on competitions, the speaker screen will be made “passive” if the technical table assumes those tasks instead of an ITO1 comfortable with technology.
> In addition, OWLCMS is launched from a control panel that gives additional options such as viewing the technical log going live. The competition supervisor can watch the competition-level audit log as it is written.

J: The supervisor uses the speaker screen with live lights and can watch the audit log.

## 4.3-R12 | E.2.3.9
<!-- hash: 185d09569ad1 -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Referee monitoring.

R: Referee monitoring view.

**Justification**

> The competition director screen can see the live referee lights, as does the jury. The audit log shows each individual decision, and each weight change from the marshals.

J: Referee lights are visible live, and each decision is in the audit log.

## 4.3-R13 | E.2.3.10
<!-- hash: 38e6f6aabb95 -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Jury control.

R: Jury control view.

**Justification**

> The jury control can be done entirely using a Blue Owl jury control keypad
> In the context of an IWF meet, it becomes desirable to accelerate the transfer of decisions from the jury to the speaker. To this effect, a tablet-style jury display is available that has the same features as the keypad
> When this is used, the Speaker and Competition Supervisor screens get the reason immediately, and can resume the competition after announcing it.

J: The jury uses a Blue Owl keypad or tablet, and the reason is sent to the speaker.

## 4.3-R14 | E.2.3.11
<!-- hash: 372e79fe01e2 -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Attempt boards.

R: Attempt boards.

**Justification**

> Attempt boards show all the information required by TCRR. They can also display the flag of the country. If one or more record attempts take place the attempt boards show it as a rolling banner.

J: Attempt boards show TCRR content, the country flag and a record-attempt banner.

## 4.3-R15 | E.2.3.12
<!-- hash: 244d90169f92 -->
FIX: Justification typo: "A row at the bottom carries will carry".

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Scoreboards.

R: Scoreboards.

**Justification**

> Scoreboards contain all the requisite information. In addition, the top line contains the current attempt. The bottom line contains additional information, such as the various championships, rank if successful, jury decisions etc.). This is the warmup room layout, that does not change during ceremonies so coaches can count attempts. Scoreboard during session with previous leaders
> In the main room, styling will be applied as required by the event, and the top row is used as a banner. A row at the bottom carries will carry the clock and other attempt information. Main Room Scoreboard with previous leaders
> Full-fledged video production companies can replace this scoreboard and the TV Graphics by using the extension capabilities provided by OWLCMS. The extension protocol is documented with a reference implementation at https://github.com/owlcms/tracker-core .

J: Warm-up and main-room layouts are provided and are extensible via tracker-core.

## 4.3-R16 | E.2.3.13
<!-- hash: 51449b69a612 -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide RTDS/warm-up.

R: RTDS and warm-up displays.

**Justification**

> In OWLCMS all the screens are RTDS (Real-time Display System)
> When decisions are given, they are shown both on the attempt board and on the scoreboards.
> If jury decisions are being shown, these appear as well. The bottom line then shows the athlete under review, whereas the top line shows the athlete getting ready (whose clock is not running). Jury Decision for current athlete

J: All screens are real-time, including referee and jury decisions.

## 4.3-R17 | E.2.3.14
<!-- hash: ffa5c8846644 -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide STOP.

R: STOP display.

**Justification**

> During deliberation, challenge, technical issue or other stoppage, the reason is shown
> Flashing Stop

J: A flashing STOP is shown with the reason.

## 4.3-R18 | E.2.3.15
<!-- hash: 4301b9abf4b5 -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Records.

R: Records display.

**Justification**

> Record Attempt Attempt Board
> Record Attempt Scoreboard

J: Record attempts are shown on attempt boards and scoreboards.

## 4.3-R19 | E.2.3.16
<!-- hash: b5bb386a9784 -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Results.

R: Results display.

**Justification**

> End of session rankings with medals

J: End-of-session rankings are shown with medals.

## 4.3-R20 | E.2.3.17
<!-- hash: acb8f8953498 -->

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Other approved states.

R: Other approved states.

**Justification**

> OWLCMS can provide additional scoreboards for team competitions, for example, mixed team championships with GAMX scoring.

J: Team scoreboards, such as GAMX mixed teams, are available.

## 4.4-R01 | E.2.4.1
<!-- hash: 88f7c51c6915 -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Start Lists.

R: Start Lists.

**Justification**

> A Start List is provided. Several formats can be provided, depending on whether the competition is on one or more platforms.

J: Start lists are provided for single- and multi-platform events.

## 4.4-R02 | E.2.4.2
<!-- hash: b40411377b1f -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Start List Package.

R: Start List Package.

**Justification**

> The Start List Package is expanded to include documents used at VFE.

J: The package also includes VFE documents.

## 4.4-R03 | E.2.4.3
<!-- hash: 8459a5b052ab -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Weigh-in List.

R: Weigh-in List.

**Justification**

> The weigh-in list allows capturing both the weight-on-scale and the adjusted weights. Signature areas for the attending officials are provided

J: The list captures scale and adjusted weights, with signature areas.

## 4.4-R04 | E.2.4.4
<!-- hash: 6214dc821ade -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Athlete's Cards.

R: Athlete's Cards.

**Justification**

> Athlete Cards with all the required signature fields are produced.

J: Athlete cards include all signature fields.

## 4.4-R06 | E.2.4.5
<!-- hash: e38eeedf1e5e -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Manual Protocol.

R: Manual Protocol.

**Justification**

> An empty protocol sheet suitable for manual entry is available.

J: An empty manual-entry protocol is provided.

## 4.4-R07 | E.2.4.6
<!-- hash: cd1a2f527f92 -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Empty Protocol.

R: Empty Protocol.

**Justification**

> In addition to the manual protocol from section E.2.4.5 the system provides an empty protocol sheet that is typically used by the jury. Bigger text boxes allow capturing the decisions according to the TCRR rules for examinations.

J: A jury protocol with space for decisions is provided.

## 4.4 | E.2.4.7
<!-- hash: 366d13b53d7a -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Protocol.

R: Protocol.

**Justification**

> A compliant protocol sheet is provided, including records

J: A compliant protocol, including records, is provided.

## 4.4-R08 | E.2.4.8
<!-- hash: e3c794a60fa0 -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Results.

R: Results.

**Justification**

> Full competition results, per category, are available in several formats.
> Flat-file formats, such as the one expected by IWF are also supported for easier importing.

J: Per-category results are available, including the IWF flat-file format.

## 4.4-R09 | E.2.4.9
<!-- hash: f455140d6d19 -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Team Classification/Placing.

R: Team Classification/Placing.

**Justification**

> The final package described in section E.2.4.11 includes a sheet with all the team classification details for IWF points, as well as medal standings. The fully formatted version of these results is present in the Results Book (see section E.2.4.12). In events with multiple concurrent championships, the classification is done per championship. The following example is for a Youth championship.
> For verification purposes, all the individual athlete statistics are also included

J: IWF team points and medal standings are produced per championship.

## 4.4-R10 | E.2.4.10
<!-- hash: 2637ee8f2083 -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Records.

R: Records.

**Justification**

> Records set during a competition can be visualized directly, for a given age group.
> At the end of the competition, records can be exported as required (either only the new records, or an updated list with or without every improvement). Both display-oriented and flat-file formats meant for importing are provided.

J: Records are viewable live and exportable as new or updated lists.

## 4.4-R11 | E.2.4.11
<!-- hash: 856484509c4d -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Final Results Package.

R: Final Results Package.

**Justification**

> OWLCMS produces a spreadsheet with all relevant results (per category, best athlete scores, team scores)

J: A spreadsheet contains category, best-athlete and team results.

## 4.4-R12 | E.2.4.12
<!-- hash: 0cac54fe6801 -->
FIX: Justification typo: "contentsis".

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Results Book where applicable.

R: Results Book.

**Justification**

> A fully formatted results book complete with table of contentsis produced automatically with no human intervention. The following are sample pages.
> Medals
> Team Points
> Best Athlete
> Category Rankings
> Session Protocols

J: A formatted results book with table of contents is generated automatically.

## 4.4-R13 | E.2.4.13
<!-- hash: e0fcd46916fe -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Machine-readable export.

R: Machine-readable export.

**Justification**

> The full database content is available in canonical JSON format, as a snapshot, on demand. The time required to produce is extremely short; this can be triggered at any idle time during the competition.
> It is also possible to get a SQL-format backup of the H2 database.

J: A JSON snapshot is available on demand, plus an SQL backup.

## 4.4-R14 | E.2.4.14
<!-- hash: 11879ed4b6f4 -->

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Distribution records.

R: Distribution records.

**Justification**

> Distribution of results is entirely under the control of the competition staff. Competition staff is expected to upload official documents to IWF’s document management systems that will control distribution. Production of authoritative exports is however logged in the audit logs.

J: Distribution is controlled by staff, and exports are recorded in the audit log.

## 4.4-R15 | E.2.4.15
<!-- hash: eaf0e13d2571 -->
FIX: Justification ends without a period ("The export is signed").

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Final evidence/archive.

R: Final evidence/archive.

**Justification**

> The machine-readable export described in section E.2.4.13 and the audit logs described in F.1.1.5 constitute the source of truth. The export is signed

J: The signed JSON export and audit logs form the archive.

# RFP Section 5

## 5-R01 | D.1.1
<!-- hash: 251695cf5153 -->

**Requirement** (RFP §5)

> Controlled capacity for authoritative state and approved services, with spare/recovery path.

R: Server capacity with spare and recovery path.

**Justification**

> OWLCMS is extremely efficient in memory, input-output and networking. As a reference, the standard OTIS kit has been tested to run 1600 athletes on 6 simultaneous platforms on a 8GB Raspberry Pi5 mini-PC.
> Two machines in this proposal qualify as servers, as shown in Figure 1.
> The main station OWLCMS server. This proposal uses a standard 16GB Windows laptop with a SSD disk for running OWLCMS. That station is used by the competition director, and includes Microsoft Office.
> The VPT and Interfaces server. This is a high-end gaming laptop configuration with the capability to run full live video processing for many feeds. The laptop meets the standard for the commercial vMix video software, as a provision for eventual broadcast-level processing.

J: OWLCMS needs few resources and runs on a server laptop, with the VPT laptop as second server.

## 5-R02 | D.1.2
<!-- hash: 41ac008116a5 -->

**Requirement** (RFP §5)

> Dedicated, role-controlled competition administration and operation positions.

R: Dedicated role-controlled operator positions.

**Justification**

> The competition director uses the main station as his operator station. In a multi-platform setup, only one director station will run the OWLCMS software, and the others act as server redundancy.
> The other Operator stations are only required to run two browsers as a maximum. The proposed configuration is from the field-tested OTIS kit. Stations are 8GB Raspberry Pi 500s that are cost-effective, easily replaceable, guaranteed to be in production until at least 2036, and designed for use in difficult environments such as classrooms. Operator stations use portable screens that can be replaced if broken.
> The USB ports on the operator stations are used to connect the Blue Owl refereeing devices, as shown in Figure 1 above.
> Keyboards with numeric pads will be provided for the Marshal and Data entry stations.

J: The director uses a laptop, operators use Raspberry Pi 500 stations, and keypads are supplied.

## 5-R03 | D.1.3
<!-- hash: f4364eee5c48 -->

**Requirement** (RFP §5)

> Protected authoritative storage, backup and tested recovery.

R: Protected storage, backup and tested recovery.

**Justification**

> A fully relational database called H2 is used. H2 has been used without a single data integrity failure since the initial versions of OWLCMS in 2009, and is extremely quick. The database sizes of a full competition are very small (typically well below 2MB). Furthermore, since OWLCMS preserves the ordering of all events, and the number of data entry operations is very low, there is very little concurrency. The only significant queries are for results production but most of the data fits in main memory for caching.
> The actual H2 binary file can be kept and is machine-independent, in addition to the structured JSON exports. The exports is the primary means of backup and snapshot. A readable consistent snapshot can be obtained at any time from the competition director station. The laptop that implements the interfaces can also pull a snapshot on demand, as a quick recovery capability.
> In a single platform scenario, the VPT+interface laptop acts the spare for the OWLCMS server.

J: The H2 database is backed up by on-demand JSON snapshots, and the VPT laptop is the spare.

## 5-R04 | D.1.4
<!-- hash: eb73df375694 -->

**Requirement** (RFP §5)

> Three independent red/white referee control devices.

R: Three independent referee devices.

**Justification**

> The Blue Owl referee controls include the two buttons, a visible LED, a sound, and a vibration for warning the referees. Referee controllers are cabled in order to avoid any concern with batteries.

J: Wired Blue Owl controls provide LED, sound and vibration.

## 5-R05 | D.1.5
<!-- hash: 663d79f97f4a -->

**Requirement** (RFP §5)

> Individual devices for applicable Jury members plus President controls.

R: Jury member devices and President controls.

**Justification**

> Jury controls for jury members are the same as the referee controls, but without the warning features. Referee controllers can be used as backups for jury controllers (the warnings would simply not be emitted).
> President controls are of two kinds. The solution includes a physical keypad with all the required real-time referee decision display and the required jury member behaviour (green until all decisions visible)
> The jury station will also have a tablet-style display that allows the jury president to indicate the reason for lift reversal.

J: Jury members have buttons, and the President has a keypad and a reversal-reason tablet.

## 5-R06 | D.1.6
<!-- hash: cdbb6934d28b -->

**Requirement** (RFP §5)

> Authoritative timer/controller with approved displays and audio.

R: Authoritative timer with displays and audio.

**Justification**

> Timing is done on the main server using the operating system timers. Operating system timing resolution is 1 ms as used. Propagation times of MQTT messages is typically under 10 ms.
> Time is displayed on an athlete-facing display, on the attempt boards, and on scoreboards. Each machine with displays attached is synchronized with the OWLCMS computer and receives the millisecond timestamps at which OWLCMS emitted the command. In this way, the propagation delay (even though it is very small) can be compensated.
> Timer warning and time-over signals are emitted on the down-signal device and logged in the audit log (for example, to determine if the timekeeper stopped the clock before or after the time-over buzzer).

J: The server times to 1 ms, displays compensate drift, and signals are audited.

## 5-R07 | D.1.7
<!-- hash: 13a511449b41 -->
FIX: Justification contains two TODO notes about pictures (Figures 3–4).

**Requirement** (RFP §5)

> Competition and warm-up presentation with mandatory athlete/attempt/weight/clock/decision data.

R: Competition and warm-up attempt boards.

**Justification**

> Attempt boards are standard FullHD or 4K TVs with HDMI inputs. The actual TV screens are to be provided locally and are not part of the certified kit.
> As shown in Figure 1, there are two computers running standard web browsers that provide the attempt boards as HDMI outputs. The Vaadin library sends live field of play updates to these browsers over a WebSocket, there is no polling
> The first attempt board output is for the front room and is styled to match the event colour scheme (this is a simple documented action during event setup). A splitter is used to feed one attempt board on each side of the platform.
> The second attempt board output is the normal high-contrast OWLCMS board and is used in all back-room areas (waiting, marshal, warmup). The kit includes splitters and extenders to reach the typical waiting, marshal and warm-up areas located immediately behind the main platform. Further splitting or extension of the signal to reach multiple attempt boards will use HDMI or SDI equipment obtained locally.
> Video connections for the main attempt boards would be as illustrated in Figure 3.
> Figure 4 illustrates the back-room connections for attempt boards. TODO: use pictures that reflect OTIS usage of dual-HDMI computers
> TODO: use a more typical backroom layout
> Figure 3: Attempt Board Video Connections
> Figure 4: Warmup Room Attempt Board Video Connections

J: Two browser HDMI outputs feed the boards through included splitters and extenders.

## 5-R08 | D.1.8
<!-- hash: c5d8b722c11e -->

**Requirement** (RFP §5)

> Real-time competition and warm-up displays with correct ranking and state.

R: Real-time competition and warm-up scoreboards.

**Justification**

> The situation for the scoreboards is the same as for attempt boards. There are two HDMI sources, one for the stage area, one for the back. For the stage area, the signal is assumed to go to a single large LED scoreboard.

J: Stage and back-room HDMI sources feed the scoreboards, as for attempt boards.

## 5-R09 | D.1.9
<!-- hash: 41c88215fc66 -->

**Requirement** (RFP §5)

> Approved cameras, recording/replay and Jury output for events requiring VPT.

R: Cameras, replay and Jury output for VPT.

**Justification**

> The VPT system is under the full control of the competition staff, and the TCRR-mandated angles can be displayed in self-service mode by the jury if so required (or provided by the competition director).
> As indicated in TCRR, the replay clips are accessible by the venue video team and broadcast teams. Such access will be controlled by networking equipment configurations.
> The VPT system relies on four PTZ cameras. Based on our experience in Continental competitions, we propose Tenveo PTZ 60fps cameras. These cameras provide Power-over-Ethernet RTSP video feeds, HDMI, and compressed H.264 USB feeds.
> OWLCMS has a VPT module that automatically clips and attributes the replays to the athlete lifting. This module keeps a full indexed archive of all the lifts, at all angles.
> The VPT will execute on a high-performance laptop that also meets the requirements for the vMix software. If a client federation wants to add such additional video-processing capabilities, the proposed equipment will be sufficient for them to use their own license on the video-processing laptop.

J: Four PTZ cameras feed automatic per-lift clips that the jury can replay.

## 5-R10 | D.1.10
<!-- hash: eaea25a5ce73 -->

**Requirement** (RFP §5)

> Documented, segmented, monitored event network with common time.

R: Segmented, monitored network with common time.

**Justification**

> The networking configuration is based on switching. Unmanaged switches are sufficient. A minimum of 4 POE (Power-over-Ethernet) ports is necessary to provide power to the networked PTZ cameras. A 16-port switch per platform is provided.
> Figure 5: Main Room Networking
> Figure 6: Back-room Networking
> All the machines on one platform are connected to a switch on that platform. Use of a switch also ensures that video traffic from the cameras goes directly to the video equipment and does not touch other machines that don’t need it
> OWLCMS networking traffic is minimal. The application is routinely used in a cloud configuration with very remote servers and no perceptible delay.
> An ASUS router is included. This router is used when an external wired internet connection is available for streaming. WiFi is not normally turned on. If federation personnel request access, it is preferred to loan them a network dongle. This eliminates concerns about illicit WiFi access.

J: Each platform has a PoE switch, a router is included, and WiFi is off.

## 5-R11 | D.1.11
<!-- hash: 2b83ce85f9ff -->

**Requirement** (RFP §5)

> Controlled read-only path from accepted state to approved consumers.

R: Read-only path to approved consumers.

**Justification**

> The video laptop will host all the external interfaces. In actual practice, sending information out to a games central is very low usage (and ODF protocols are push only). OWLCMS can also publish to cloud-based scoreboards and other feature, but those are also push only. In the past, outside broadcasters have used the OVR-provided screen layouts directly. This was accommodated by opening a port on the router to a specifically white-listed address. This capability is generic with the equipment provided.

J: The interface laptop pushes feeds out, and inbound access is whitelisted.

## 5-R12 | D.1.12
<!-- hash: a3670922b8a3 -->

**Requirement** (RFP §5)

> Reliable official documents with fallback.

R: Reliable official documents with fallback.

**Justification**

> A Windows laptop with Microsoft Office is used for data entry and report generation to maximize compatibility with whatever printer is made available. OWLCMS produces documents in Excel format. Documents should be exported as PDF in non-editable mode and kept as such for archival.
> Start books, Result books, Refereeing schedules and similar sophisticated layouts are produced as PDF.

J: Documents are produced as Excel and PDF from an Office laptop.

## 5-R13 | D.1.13
<!-- hash: 9951777a9501 -->

**Requirement** (RFP §5)

> Protection and approved autonomy for critical equipment.

R: Power protection for critical equipment.

**Justification**

> UPS will be provided to protect the networking switches in the front and back room, so the marshal and competition director can still capture changes and perform timing during a short power outage, relying on the fact that their stations have batteries.
> Event organizing committees are expected to provide protection for the rest of the equipment, such as TVs and all other networking and video distribution, if warranted by local conditions.

J: Switches are on UPS, and the stations run on battery.

## 5-R14 | D.1.14
<!-- hash: 27a92f560672 -->

**Requirement** (RFP §5)

> Health, service, device, storage, time and stale-state monitoring.

R: Health, device, storage and time monitoring.

**Justification**

> The OWLCMS software has very low memory and disk I/O requirements. Use of the normal operating system indicators is therefore sufficient. Additional technical logging for memory and CPU indicators can be turned on in the technical logs.

J: Operating system indicators suffice, with optional CPU and memory logging.

## 5-R15 | D.1.15
<!-- hash: e66e2193fdaa -->
FIX: Justification is truncated: "OWLCMS runs on Windows, macOS,".

**Requirement** (RFP §5)

> Rapidly deployable replacements for critical equipment families.

R: Rapid replacement of critical equipment.

**Justification**

> All the stations only require the presence of a current browser (Chrome, Edge, Firefox or Safari), and for some of them, the ability to connect a USB device. So essentially any laptop can be used, whether Windows, macOS, Linux. Chromebooks can be used when no USB device is needed.
> OWLCMS runs on Windows, macOS,

J: Any laptop with a current browser can replace a station.

## 5-R16 | D.1.16
<!-- hash: 80174d761889 -->
FIX: Justification wording: "routed along shared with video distribution".

**Requirement** (RFP §5)

> Safe, labelled cabling and equipment placement without obstruction or sightline conflicts.

R: Safe, labelled, unobstructive cabling.

**Justification**

> The OTIS kit cabling is fully labelled. As illustrated in Figure 3 to Figure 6, cabling is routed along shared with video distribution, runs under the stage whenever possible, and never crosses paths used by athletes.

J: Cabling is labelled, runs under the stage, and avoids athlete paths.

## 5.1 | D.1.17
<!-- hash: adb0d67a7c6d -->
FIX: Justification has a stray "." paragraph.

**Requirement** (RFP §5.1)

> For each licensed event, the supplier shall maintain make/model, quantity, serial or asset ID, firmware, location, connections, power source, owner, health status, spare path and approved deviations.

R: Per-event equipment register.

**Justification**

> The OTIS kit already has a full bill of materials (BOM); Refer to Appendix 1 for the additional components.
> .

J: A full bill of materials for the OTIS kit is provided (Appendix 1).

# RFP Section 6

## 6.1-R01 | C.1.1.1
<!-- hash: 353ae8a7af60 -->

**Requirement** (RFP §6.1)

> Implement the effective IWF rule pack and support controlled updates.

R: Implement IWF rule pack with controlled updates.

**Justification**

> The solution is tested against the TCRR rules using automated tests. OWLCMS has over 350 automated tests covering rules, reports and program behaviour. Any new release is tested against regressions.

J: Over 350 automated tests check rules and prevent regressions.

## 6.1-R02 | C.1.1.2
<!-- hash: 6941e73c1974 -->

**Requirement** (RFP §6.1)

> Competition-critical functions shall not depend on unavailable internet, licence-server or payment services.

R: No dependency on internet or licence servers.

**Justification**

> Once downloaded, the solution is completely autonomous and works with no internet connection whatsoever

J: The system runs fully offline.

## 6.1-R03 | C.1.1.3
<!-- hash: d5e66bfa6fed -->

**Requirement** (RFP §6.1)

> Exact production version, configuration, database schema, rule pack and relevant assets shall be identifiable and reproducible.

R: Reproducible version, config, schema and assets.

**Justification**

> Releases are created by a fully repeatable automated build process, and under strict version control. This applies both to OWLCMS and the Blue Owl components. All past versions and the source code used to build them are available.

J: Releases come from repeatable automated builds under version control.

## 6.1-R04 | C.1.1.4
<!-- hash: fedb623c7770 -->

**Requirement** (RFP §6.1)

> Installation and rollback shall be documented and repeatable.

R: Documented, repeatable install and rollback.

**Justification**

> OWLCMS is launched by a control panel that allows switching between versions. Within a major version, the database can be rolled back if a fix release is unsatisfactory. Fix releases can always read exported prior data so they can be tested independently. Emergency installation of a fix is a matter of seconds once the fix release is downloaded. The OWLCMS program runs on any available Windows, macOS or Linux computer, and the database exports are completely portable, so any available computer can be used for testing.

J: The control panel switches versions, and the database can be rolled back in seconds.

## 6.1-R05 | C.1.1.5
<!-- hash: 4232a2ee320e -->

**Requirement** (RFP §6.1)

> Automatic updates shall not occur during an event freeze unless approved.

R: No automatic updates during event freeze.

**Justification**

> The system does not perform any type of automatic update. Operating system updates shall be managed by duly authorized personnel according to supplier procedures; they are not under the control of the solution.

J: The system never updates itself, and OS updates follow procedures.

## 6.2-R01 | C.1.2.1
<!-- hash: 7b8362148ed6 -->

**Requirement** (RFP §6.2)

> Every sporting fact shall have one declared authoritative owner.

R: One authoritative owner per sporting fact.

**Justification**

> Any change to the competition state is uniquely attributed to a human actor, to a device input, or to an automated calculation. The audit log reflects this attribution.

J: Every change is attributed in the audit log.

## 6.2-R02 | C.1.2.2
<!-- hash: 7c6e76091818 -->

**Requirement** (RFP §6.2)

> Derived screens, feeds, documents and caches shall be traceable to the accepted state.

R: Outputs traceable to accepted state.

**Justification**

> All computed results are directly attributable to programmed rules based on the authoritative data.

J: All results are derived by rules from authoritative data.

## 6.2-R03 | C.1.2.3
<!-- hash: 9b96955a5b63 -->

**Requirement** (RFP §6.2)

> Corrections shall be authorised, atomic, attributable and propagated.

R: Authorised, atomic, attributable corrections.

**Justification**

> Once the competition has started, all changes are meant to be made using the interactive interfaces or the explicit import features of the programs such that audit traces are kept. Database state that does not correspond to audited actions is evidence of tampering. Should intentional exceptional corrective actions by the supplier requiring database queries or similar be necessary, the corresponding authorizations are expected to be managed separately as part of incident response.

J: Changes go through audited interfaces, so unaudited state reveals tampering.

## 6.2-R04 | C.1.2.4
<!-- hash: a772a2249b15 -->

**Requirement** (RFP §6.2)

> Duplicate, lost, conflicting or out-of-order transactions shall be prevented or detected.

R: Prevent or detect bad transaction sequencing.

**Justification**

> The system applies strict sequencing for all events, whether device-generated or human-generated. Duplicate events are eliminated.
> Conflicting updates from technical officials are detected and reported (e.g., competition director and marshal both attempting to correct an error). The marshals cannot accidentally interfere with the data updates resulting from a refereeing decision (the athlete card is closed when a decision is received). Athlete registration information cannot be edited while a session is in progress. Likewise, the features that enable post-session athlete card corrections are blocked until the session is over – during a session only the marshal and director pages can edit the athlete cards.

J: Events are strictly sequenced, duplicates removed, and conflicts detected and locked out.

## 6.2-R05 | C.1.2.5
<!-- hash: 76cec6f7a3a8 -->

**Requirement** (RFP §6.2)

> A complete machine-readable event export shall be available to IWF.

R: Complete machine-readable export.

**Justification**

> The database export contains all the event data in structured JSON format. The audit logs are also machine-readable.

J: The JSON export and audit logs are machine-readable.

## 6.3 | G.1
<!-- hash: 644b00e6d994 -->
FIX: Justification only repeats the requirement; no actual answer.

**Requirement** (RFP §6.3)

> Material interfaces shall document producer/consumer, purpose, version, schema/field mapping, identifiers, status/null rules, sequencing/revision, time source, latency, authentication, acknowledgement, retry/reconnect, monitoring and fallback.

R: Documented material interfaces.

**Justification**

> Material interfaces shall document producer/consumer, purpose, version, schema/field mapping, identifiers, status/null rules, sequencing/revision, time source, latency, authentication, acknowledgement, retry/reconnect, monitoring and fallback.

J:

## 6.4 | C.2
<!-- hash: 9b5ea76dfb28 -->
FIX: Justification only repeats the requirement; no actual answer.

**Requirement** (RFP §6.4)

> All third-party software, libraries, services, licences and hardware necessary for operation shall be declared, including version, ownership, support status and cloud/public-internet dependency.

R: Declared third-party dependencies.

**Justification**

> All third-party software, libraries, services, licences and hardware necessary for operation shall be declared, including version, ownership, support status and cloud/public-internet dependency.

J:

# RFP Section 7

## 7.1-R01 | F.1.1.1
<!-- hash: 4d2aa33dcef3 -->

**Requirement** (RFP §7.1)

> Least-privilege access by role and device identity.

R: Least-privilege access by role and device.

**Justification**

> OWLCMS supports separate logins for every role, and where relevant, separate logins for each role and platform. Devices are identified per platform and authenticated using the MQTT identity of the MQTT interface.

J: Logins are per role and platform, and devices are identified by MQTT identity.

## 7.1-R02 | F.1.1.2
<!-- hash: de30b7d4c4c4 -->

**Requirement** (RFP §7.1)

> Separation of competition, device, public/display, database and management functions where applicable.

R: Separation of functions.

**Justification**

> The role structure separates the technical official roles, preparation roles, the competition director, and admin functions.

J: Official, preparation, director and admin roles are separated.

## 7.1-R03 | F.1.1.3
<!-- hash: 15a16b4201b0 -->

**Requirement** (RFP §7.1)

> Protection of credentials, secrets and data in transit/at rest where required.

R: Protected credentials and data.

**Justification**

> All credentials or secrets are stored encrypted and transported in encrypted form. They are never written to log files in clear. Account passwords and interface connection passwords use PBKDF2 encryption with HMAC-SHA256.

J: Secrets are encrypted, and passwords use PBKDF2 with HMAC-SHA256.

## 7.1-R04 | F.1.1.4
<!-- hash: d6300a92fa9b -->

**Requirement** (RFP §7.1)

> Controlled remote support, removable media and patch/deployment processes.

R: Controlled remote support, media and patching.

**Justification**

> Supplier will install a remote support program on the competition director laptop and on the interface laptop (AnyDesk).
> Removable media will be provided by IWF to the competition director, as IWF will keep the files copied to that device.
> OWLCMS is always updated from versions built using its repeatable build process, which creates zip files in the official repository. The updated version can be pulled by the OWLCMS launcher if there is internet access; otherwise, another machine can be used to copy the release on removable media and then used to do the updating. The OWLCMS launcher keeps multiple versions side-by-side and maintains a copy of the data for each so that it is always possible to go back to the previous version. The exported files are also systematically tested so that export from a patch version can always be read by a prior version in the same major release cycle.

J: Support uses AnyDesk, media come from IWF, and versions run side by side for rollback.

## 7.1-R05 | F.1.1.5
<!-- hash: cc4b25849d7c -->

**Requirement** (RFP §7.1)

> Tamper-resistant audit sufficient to reconstruct material actions.

R: Tamper-resistant audit.

**Justification**

> The audit logs include every single modification to the athlete and lifting data, and all events coming from devices (decisions, etc.). All export actions are logged with the checksum of the exported file, for later integrity checks of exported files.
> The audit log is a chain of cryptographically signed blocks of log lines. Each block is signed by a private key. Each block's signature also includes the previous block's signature, creating a chain.
> Prior to each competition, a cryptographic public-private key pair is generated, along with a public-key fingerprint. The technical delegate or other trusted person takes a picture of the fingerprint. Logs are copied away from the competition computer to a safe location periodically during the meet.
> In case of a dispute, the safe logs are copied to a clean machine, and a clean OWLCMS is installed. Each log contains the public key that matches the (undisclosed) private key used to do the signatures. The checker can confirm that each block was signed by the matching private key and that each block was not modified. The technical delegate confirms the public key’s authenticity by comparing its fingerprint with what they photographed.

J: Log blocks are signed and chained, exports are checksummed, and keys are verified by fingerprint.

## 7.1-R06 | F.1.1.6
<!-- hash: 667eeb63d97d -->

**Requirement** (RFP §7.1)

> No public or anonymous path may write authoritative competition state.

R: No public write path to competition state.

**Justification**

> The only writing to the database is done by OWLCMS itself, which owns an exclusive file lock. The database is therefore the source of truth. Exports are cryptographically checksummed, and the checksum is logged in the audits to allow later verifications of integrity.

J: Only OWLCMS writes to the locked database, and exports are checksummed.

## 7.2 | F.1.2
<!-- hash: 6376cd3785b0 -->

**Requirement** (RFP §7.2)

> Core competition, officiating, timing, local displays and local official documents shall remain usable during loss of public internet or cloud connectivity.

R: Operation without internet or cloud.

**Justification**

> The normal mode of operation of OWLCMS in a major competition is completely offline, on a private local area network as shown in Figure 1. Operator stations are normally wired using Ethernet cabling to avoid WiFi interference or foul play.

J: The system normally runs fully offline on a wired private network.

## 7.3-R01 | F.1.3.1
<!-- hash: b57e0b583c49 -->

**Requirement** (RFP §7.3)

> Protected backup copies of data, configuration, rules, assets and audit.

R: Protected backups of data, config and audit.

**Justification**

> All files that constitute the OWLCMS configuration and rules are stored in a single folder. The cryptographic key is kept separate as it should not leak on backups.
> While OWLCMS is running the export format is used to capture snapshots, as it can be produced on demand in a consistent state. The actual binary database file can be backed up when the program is stopped.

J: Configuration is in one folder, the key is kept separate, and snapshots are on demand.

## 7.3-R02 | F.1.3.2
<!-- hash: e506159a7763 -->

**Requirement** (RFP §7.3)

> Timed restoration demonstrating the approved recovery objective.

R: Timed restoration within objective.

**Justification**

> Restoration of a saved state from scratch normally takes less than a minute.

J: A full restore takes under a minute.

## 7.3-R03 | F.1.3.3
<!-- hash: 13936f030cf2 -->

**Requirement** (RFP §7.3)

> Defined promotion, split-brain prevention, failback and reconciliation where redundancy is claimed.

R: Failover controls where redundancy claimed.

**Justification**

> The system would not operate in tandem. Checkpoint copies would be used.

J: Systems do not run in tandem; checkpoint copies are used instead.

## 7.3-R04 | F.1.3.4
<!-- hash: efe0e0920888 -->

**Requirement** (RFP §7.3)

> Cold-start and rollback procedures using controlled recovery media.

R: Cold-start and rollback procedures.

**Justification**

> Checkpoint copies of the export format can be obtained on demand, and constitute full rollback points. Restarting the system without a database file creates a minimal empty one that can be immediately loaded from a checkpoint.

J: Export checkpoints serve as full rollback points.

## 7.4 | F.1.4
<!-- hash: cab8e06fbcc7 -->
FIX: Justification starts by repeating the requirement text.

**Requirement** (RFP §7.4)

> A manual Protocol and rehearsed manual-continuity procedure shall remain available. Staff shall know when to continue, when to STOP and how to reconcile manual records with the electronic state.

R: Rehearsed manual-continuity procedure.

**Justification**

> A manual Protocol and rehearsed manual-continuity procedure shall remain available. Staff shall know when to continue, when to STOP and how to reconcile manual records with the electronic state.
> This is standard operational procedure. A power outage, or a networking mishap can prevent use the system and cause a revert to a “manual cards and flags” mode.
> OWLCMS has been tested in such situations before. As soon as the system is restored, someone can simply copy the information as it stands on the cards, even while lifting is going on that platform. As soon as the information has been updated and matches the paper trail, the screens can be turned on for that platform. All the lifting order and such is derived from the authoritative card data as entered.
> Writing operational procedures as expected by IWF would take place before deployment.

J: Fallback to cards and flags is tested, and the system resyncs from the cards.

## 7.5 | F.1.5
<!-- hash: c881ef819662 -->
FIX: Justification starts by repeating the requirement text.

**Requirement** (RFP §7.5)

> Actionable monitoring shall cover critical services, devices, timing, storage, network and stale-state conditions, with clear escalation responsibilities.

R: Actionable monitoring with escalation.

**Justification**

> Actionable monitoring shall cover critical services, devices, timing, storage, network and stale-state conditions, with clear escalation responsibilities.
> The actual OTIS kit requires a router and, for each platform, a switch. As part of the certified kit, this constitutes the monitoring boundary. The kit devices are attached directly to these and can be monitored (including remote logins).

J: The router and switches form the monitored boundary of the kit.

# RFP Section 8

## 8.1-R01 | A.1.1.1
<!-- hash: aa18e9634b6c -->

**Requirement** (RFP §8.1)

> Legally established entity able to contract with IWF.

R: Legal entity able to contract with IWF.

**Justification**

> This proposal is put forward by [Supplier], a duly incorporated corporation under [Country Law]
> [To be completed]

J: [Supplier] is a duly incorporated corporation under [Country Law]. [To be completed]

## 8.1-R02 | A.1.1.2
<!-- hash: b2f3935098f3 -->

**Requirement** (RFP §8.1)

> Relevant experience delivering competition scoring/results or comparable mission-critical systems.

R: Relevant mission-critical delivery experience.

**Justification**

> [Supplier] has been organizing an annual series of competitions for many years, as well as its own very large scale national championships. Each of these competitions features from 3 to 6 simultaneous platforms, involves up to 1600 athletes, and last between 3 and 9 days. The competitions are streamed and results are available live on the Web. Furthermore, state-level organizations routinely organize multi-platform events with several hundred participants using the exact same technologies and procedures, so there is a large network of already trained persons available.

J: [Supplier] runs annual multi-platform events of up to 1600 athletes, streamed live.

## 8.1-R03 | A.1.1.3
<!-- hash: 6aca76390cce -->

**Requirement** (RFP §8.1)

> Ability to accept end-to-end responsibility for the complete system and declared third-party components.

R: End-to-end system responsibility.

**Justification**

> The specific combination of software (OWLCMS) and devices (Blue Owl) presented in this proposal has been used since 2024 at all national events.

J: The same OWLCMS and Blue Owl combination has been used nationally since 2024.

## 8.1-R04 | A.1.1.4
<!-- hash: c2f801b54290 -->

**Requirement** (RFP §8.1)

> Qualified technical, competition-operations and support personnel.

R: Qualified technical and support personnel.

**Justification**

> [Supplier] has retainers with technology specialists to install and operate the actual TIS for its very large events, and a large network of people experienced with the system, the devices, and the processes it can draw upon.

J: [Supplier] retains specialists and can draw on a large trained network.

## 8.1-R05 | A.1.1.5
<!-- hash: 3ce6efabd911 -->

**Requirement** (RFP §8.1)

> International training, event support and incident-response capability.

R: International training, support and incident response.

**Justification**

> [Supplier] is able to provide trainers that would do international training. [Supplier] already monitors its own events at distance with remote intervention capabilities. [Supplier] can create teams of people to be dispatched internationally, as it currently does with technical officials.

J: [Supplier] provides international trainers, remote monitoring and deployable teams.

## 8.1-R06 | A.1.1.6
<!-- hash: e2f9f700332a -->

**Requirement** (RFP §8.1)

> Ability to maintain the approved baseline for the license period.

R: Maintain baseline for licence period.

**Justification**

> [Supplier] already maintains the exact same baseline for its own very large scale events.

J: [Supplier] already maintains this baseline for its own large events.

## 8.2-R01 | H.1.1
<!-- hash: 450e78cb1142 -->

**Requirement** (RFP §8.2)

> Provide accurate product and hardware baseline.

R: Accurate product and hardware baseline.

**Justification**

> [To be completed]

J: [To be completed]

## 8.2-R02 | H.1.2
<!-- hash: e8c1944f31f8 -->

**Requirement** (RFP §8.2)

> Provide complete technical submission and evidence package.

R: Complete technical submission and evidence.

**Justification**

> A test environment can be provided to reproduce any claimed feature.

J: A test environment can reproduce any claimed feature.

## 8.2-R03 | H.1.3
<!-- hash: 5d6fb4a78560 -->

**Requirement** (RFP §8.2)

> Make the declared system available for IWF testing and demonstrations.

R: System available for IWF testing.

**Justification**

> A test environment can be provided if supplier is selected.

J: A test environment will be provided if selected.

## 8.2-R04 | H.1.4
<!-- hash: db47817efcad -->

**Requirement** (RFP §8.2)

> Correct deficiencies and complete regression testing when required.

R: Correct deficiencies with regression testing.

**Justification**

> Agreed if supplier is selected.

J: Agreed if selected.

## 8.2-R05 | H.1.5
<!-- hash: 5b699941dce6 -->

**Requirement** (RFP §8.2)

> Notify IWF of material product, security, hardware, interface or rule-logic changes.

R: Notify IWF of material changes.

**Justification**

> Agreed.

J: Agreed.

## 8.2-R06 | H.1.6
<!-- hash: a0b937fd8723 -->

**Requirement** (RFP §8.2)

> Maintain qualified support personnel and appropriate spares.

R: Maintain support personnel and spares.

**Justification**

> Agreed.

J: Agreed.

## 8.2-R07 | H.1.7
<!-- hash: f0335263fe00 -->

**Requirement** (RFP §8.2)

> Protect IWF, athlete and event information and comply with applicable data-protection requirements.

R: Protect data and meet data-protection rules.

**Justification**

> The only information that would normally be considered Personal Data under GDPR and similar frameworks is the athlete’s body weight and birth date. The body weight being necessary to the application of rules, it can be construed to fall under the “legitimate interests” clause (and disclosure should be mentioned as part of any consent and risk acceptance form signed by athletes).
> Concerning birth date, IWF’s own rules do not require the full birth date to be published, only the birth year. OWLCMS has a setting to only use and show the birth year; we would recommend that this be used in IWF competitions.
> The rest of the athlete information and event information is meant to be made public as it is part of public event invitations and included in preliminary participation lists, start books and result books.
> In an IWF event, it is expected that an IWF identifier of the athlete will be included in the registration data. OWLCMS treats that identifier as an opaque anonymized foreign identifier.

J: Personal data is minimal, birth year alone can be used, and IWF IDs are treated as opaque.

# RFP Section 9

## 9.1-R01 | K.3.1
<!-- hash: 75e77006aac5 -->

**Requirement** (RFP §9.1)

> Acceptance of the IWF TCRR as the controlling sporting rules.

R: Accept IWF TCRR as controlling rules.

**Justification**

> Agreed.

J: Agreed.

## 9.1-R02 | K.3.2
<!-- hash: b57011e2da2c -->

**Requirement** (RFP §9.1)

> Identification of all material limitations and dependencies.

R: Identify material limitations and dependencies.

**Justification**

> None identified. To be confirmed during acceptance testing.

J: None have been identified; this will be confirmed at acceptance.

## 9.1-R03 | K.3.3
<!-- hash: 91a6690c2e69 -->

**Requirement** (RFP §9.1)

> Ability to reproduce the tested system from controlled installation material.

R: Reproducible installation from controlled material.

**Justification**

> Installation from a compliant kit shall reproduce the tested behaviour.

J: Installing from a compliant kit reproduces the tested behaviour.

## 9.1-R04 | K.3.4
<!-- hash: 3706b088b811 -->

**Requirement** (RFP §9.1)

> Offline operation and tested recovery for the claimed profile.

R: Offline operation and tested recovery.

**Justification**

> The system is confirmed to operate without on-line access, and to be recoverable from the described exports.

J: The system runs offline and recovers from exports.

## 9.1-R05 | K.3.5
<!-- hash: 67b3756fca40 -->

**Requirement** (RFP §9.1)

> No automatic licensing mechanism that can interrupt an active competition.

R: No licensing mechanism that can interrupt competition.

**Justification**

> There are no such automatic licensing mechanisms.

J: There is no such mechanism.

# RFP Section 10

## 10-S01 | I.1.1
<!-- hash: 293af174e2e5 -->

**Requirement** (RFP §10)

> Completeness, eligibility and declarations.

R: Completeness, eligibility and declarations.

**Justification**

> Supplier declares the current information to be correct at the date of submission.

J: The information is declared correct at the submission date.

## 10-S02 | I.1.2
<!-- hash: 4ae66e7c3637 -->

**Requirement** (RFP §10)

> Architecture, functionality, hardware, interfaces, security and resilience.

R: Technical evaluation.

**Justification**

> To be performed by IWF selection committee.

J: This is performed by the IWF selection committee.

## 10-S03 | I.1.3
<!-- hash: 80ecd5df46b5 -->
FIX: Justification typo: "Baseline will managed".

**Requirement** (RFP §10)

> Freeze baseline, profile, capacity, dependencies and test plan.

R: Freeze baseline, profile and test plan.

**Justification**

> All claimed capabilities are present in the proposed baseline, in compliance with TCRR as available October 1, 2026.
> Baseline will managed according to section K.1.2. Note that the baseline may need to evolve to reflect software modifications due to changes in IWF rules or formal adoption of experimental practices (e.g., display of jury decisions on scoreboards). The baseline may also evolve due to incident resolution or significant supplier upgrades, such as CVE security incident resolutions.

J: All capabilities are in the baseline, which is managed per K.1.2.

## 10-S04 | I.1.4
<!-- hash: cd252f70d96b -->

**Requirement** (RFP §10)

> Controlled normal, edge, security, capacity and failure testing.

R: Controlled acceptance testing.

**Justification**

> Initial acceptance tests to be formalized if supplier is selected. All capabilities claimed can be demonstrated.

J: Tests will be formalized if selected, and all claims can be demonstrated.

## 10-S05 | I.1.5
<!-- hash: 554059eb7faf -->

**Requirement** (RFP §10)

> Resolve deficiencies and provide regression evidence.

R: Resolve deficiencies with regression evidence.

**Justification**

> Any issue declared will be investigated, and updated official builds will be provided. OWLCMS includes over 350 automated tests to prevent regressions; such tests are run before any release.

J: Fixed official builds are provided, backed by over 350 regression tests.

## 10-S06 | I.1.6
<!-- hash: 671f7f3824fc -->

**Requirement** (RFP §10)

> Representative event tempo with operators, officials, displays, feeds and recovery.

R: Representative event-tempo test.

**Justification**

> OWLCMS includes an automated stress simulation test that allows all required displays to be observed on all platforms as desired. All device interfaces are exercised, and all external feeds are fed.
> The stress test proceeds as fast as could possibly be done in real life: a full lift cycle takes about 12 seconds from one decision to the next, including a real clock start and the decision reversal delays. Simulated attempted weights are randomly modified using the audited marshal events (simulating a plausible rate of such changes). Referee decisions are generated using the actual audited referee events as they would be generated by devices, to create a plausible rate of successive attempts.
> All the displays, devices, and operator stations, for all platforms, can be opened at once and observed.
> It is of course possible to perform the same test manually with human operators.

J: An automated real-tempo stress test exercises all displays, devices and feeds.

## 10-S07 | I.1.7
<!-- hash: 8a039f3f0409 -->

**Requirement** (RFP §10)

> Review requirements, hard gates, evidence and findings.

R: Review gates, evidence and findings.

**Justification**

> To be performed by IWF selection committee.

J: This is performed by the IWF selection committee.

## 10-S08 | I.1.8
<!-- hash: 31cc4039e430 -->

**Requirement** (RFP §10)

> Complete legal and operational conditions.

R: Legal and operational conditions.

**Justification**

> To be defined once supplier is selected.

J: These will be defined once a supplier is selected.

## 10-S09 | I.1.9
<!-- hash: 80ac42c703d0 -->

**Requirement** (RFP §10)

> Check actual installation against approved baseline.

R: Installation matches approved baseline.

**Justification**

> To be performed by IWF acceptance committee.

J: This is performed by the IWF acceptance committee.

## 10-S10 | I.1.10
<!-- hash: f26c7145511e -->

**Requirement** (RFP §10)

> Review incidents, changes, support and expiry.

R: Review incidents, changes, support, expiry.

**Justification**

> IWF oversight mechanisms are to be defined.

J: IWF oversight mechanisms are to be defined.

## 10.1 | I.1.11
<!-- hash: ab79074bd282 -->

**Requirement** (RFP §10.1)

> Evidence shall be attributable to a specific baseline and test. Screenshots alone are insufficient where logs, physical inspection or live behaviour are necessary.

R: Evidence tied to baseline and test.

**Justification**

> Supplier will capture exports and logs as required during testing.

J: Exports and logs will be captured during testing.

# RFP Section 11

## HG-01 | I.2.1
<!-- hash: 5c4aace51f40 -->

**Requirement** (RFP §11)

> Incorrect sporting fact, rank, medal, record or rule-critical transition.

R: Incorrect sporting fact or rule transition.

**Justification**

> No such incident reported in actual production use.

J: No such incident has been reported in production use.

## HG-02 | I.2.2
<!-- hash: f393be5738c7 -->

**Requirement** (RFP §11)

> Split, duplicated, lost or silently overwritten accepted state.

R: Split, lost or overwritten state.

**Justification**

> No such incident reported in actual production use.

J: No such incident has been reported in production use.

## HG-03 | I.2.3
<!-- hash: c54276d09248 -->

**Requirement** (RFP §11)

> Unsafe or ambiguous timing, decision, Down signal or restart.

R: Unsafe timing, decision or restart.

**Justification**

> No such incident reported in actual production use.

J: No such incident has been reported in production use.

## HG-04 | I.2.4
<!-- hash: 6ff4bb7a3ed8 -->

**Requirement** (RFP §11)

> Unauthorised, partial, untraceable or unreconciled correction.

R: Untraceable or unauthorised correction.

**Justification**

> System includes audit of all modifications.

J: All modifications are audited.

## HG-05 | I.2.5
<!-- hash: e80c1d33083d -->

**Requirement** (RFP §11)

> Uncontrolled loss of competition or unverified recovery.

R: Uncontrolled loss or unverified recovery.

**Justification**

> Actual production occurrences of loss of power or loss of networking do occur regularly in some countries, and OWLCMS has been used in competitions where they occurred. The system has never been observed to lose data. In IWF-scale events, it is always run on battery-protected laptops and/or on a UPS to prevent power spikes. Even if a laptop running on battery suddenly runs out of power, the database's transactional nature prevents data loss.
> Recovery is achieved as soon as the system has been updated to catch up with the handwritten athlete cards (or even the manual protocol). A platform can be on outage (local power loss to its networking switch) while others are working, catching up for that platform will allow it to recover, the other platforms are not affected.
> As an additional precaution against human error, the competition director would normally trigger a database export after weigh-ins and at each break on each platform. This can also be automated to take place at fixed intervals.

J: No data loss has been observed, platforms recover independently, and exports are taken regularly.

## HG-06 | I.2.6
<!-- hash: 67c6207906d2 -->

**Requirement** (RFP §11)

> Uncontrolled privilege, public write path or material compromise.

R: Uncontrolled privilege or public write path.

**Justification**

> The system as proposed to IWF is fully role- and least-privilege-based. The database is in exclusive write mode from the OWLCMS system, and all competition modifications done by the application are logged.

J: Roles are least-privilege, database writes are exclusive, and all changes are logged.

## HG-07 | I.2.7
<!-- hash: 957cc97039b3 -->

**Requirement** (RFP §11)

> Contradictory outputs or untraceable final publication.

R: Contradictory or untraceable outputs.

**Justification**

> No such incident reported in production

J: No such incident has been reported in production use.

## HG-08 | I.2.8
<!-- hash: 07c289e9b4c8 -->

**Requirement** (RFP §11)

> Unknown tested version, missing evidence or irreproducible installation.

R: Unknown version or irreproducible install.

**Justification**

> All builds are versioned and created from a repeatable procedure that is public. The actual binary files contain version information.

J: Binaries are versioned and built by a public, repeatable procedure.

## HG-09 | I.2.9
<!-- hash: 509053c19f08 -->

**Requirement** (RFP §11)

> Missing critical equipment, unsafe installation or unresolved obstruction.

R: Missing equipment or unsafe installation.

**Justification**

> This clause is event-specific, to be completed by the technical delegate of the event.

J: This is event-specific and is completed by the technical delegate.

## 11.1-R01 | I.2.10.1
<!-- hash: 4d8123adec97 -->

**Requirement** (RFP §11.1)

> All applicable mandatory requirements and tests must be disposed of.

R: All mandatory requirements disposed of.

**Justification**

> Understood as “must be met”. To be completed by IWF evaluation committee.

J: This is understood as "must be met" and is evaluated by IWF.

## 11.1-R02 | I.2.10.2
<!-- hash: 1e08cb714074 -->

**Requirement** (RFP §11.1)

> Applicable TCRR requirements and hard gates must pass.

R: TCRR requirements and hard gates pass.

**Justification**

> To be completed by IWF evaluation committee.

J: This is evaluated by the IWF evaluation committee.

## 11.1-R03 | I.2.10.3
<!-- hash: ed553e3d389b -->

**Requirement** (RFP §11.1)

> No open Critical finding may remain.

R: No open Critical finding.

**Justification**

> To be completed by IWF evaluation committee.

J: This is evaluated by the IWF evaluation committee.

## 11.1-R04 | I.2.10.4
<!-- hash: 49b21cfd79fe -->

**Requirement** (RFP §11.1)

> Traceability from requirement to evidence and test must be complete.

R: Complete requirement-to-test traceability.

**Justification**

> To be completed by IWF evaluation committee.

J: This is evaluated by the IWF evaluation committee.

## 11.1-R05 | I.2.10.5
<!-- hash: 66e7afe791ce -->

**Requirement** (RFP §11.1)

> Successful FAT, recovery and operational validation are required for the claimed profile.

R: Successful FAT, recovery and validation.

**Justification**

> To be completed by IWF evaluation committee.

J: This is evaluated by the IWF evaluation committee.

## 11.1-R06 | I.2.10.6
<!-- hash: e175c08aeebe -->

**Requirement** (RFP §11.1)

> Partial is not Pass.

R: Partial is not Pass.

**Justification**

> To be completed by IWF evaluation committee.

J: This is evaluated by the IWF evaluation committee.

## 11.1-R07 | I.2.10.7
<!-- hash: 4c71e7834534 -->

**Requirement** (RFP §11.1)

> A profile limitation cannot change an IWF sport rule or bypass a hard gate.

R: Profiles cannot override rules or gates.

**Justification**

> To be completed by IWF evaluation committee.

J: This is evaluated by the IWF evaluation committee.

# RFP Section 12

## 12.1 | K.1.1
<!-- hash: 5632a7f16e1b -->

**Requirement** (RFP §12.1)

> The IWF licence shall be valid for a four-year period. The certificate shall state supplier, product/baseline, profiles, capacities, limitations, deviations, validity and surveillance conditions.

R: Four-year licence with certificate contents.

**Justification**

> The IWF licence shall be valid for a four-year period. The final contract shall state the exact licence duration and renewal conditions.

J: The licence runs four years, with duration and renewal set in the final contract.

## 12.2-R01 | K.1.2.1
<!-- hash: 688e0cca9598 -->
FIX: Justification is an incomplete sentence: "Will be treated the same as minor cosmetic".

**Requirement** (RFP §12.2)

> Controlled update and review.

R: Editorial changes: controlled update and review.

**Justification**

> Will be treated the same as minor cosmetic

J: Editorial changes are treated as minor cosmetic changes.

## 12.2-R02 | K.1.2.2
<!-- hash: 4c3deeb0cfbb -->

**Requirement** (RFP §12.2)

> Supplier assessment and IWF notification where applicable.

R: Minor cosmetic changes: supplier assessment, IWF notice.

**Justification**

> These are typically typos or simple modification to the layout (spacing, etc.). To be considered minor, operational procedures would not need to be modified.

J: These cover typos and layout only, with no procedure changes.

## 12.2-R03 | K.1.2.3
<!-- hash: 47901d19d6c2 -->

**Requirement** (RFP §12.2)

> Impact assessment and approved regression/delta test.

R: Significant changes: impact assessment and delta test.

**Justification**

> Changes in this category will be treated in one of two categories.
> Technical changes (OS/firmware/security/hardware). These are often unavoidable and required by suppliers.
> Functional changes – User interface changes, workflow changes, changes or additions to data sent over interfaces
> The difference lies in who assesses impacts and approves the changes.

J: Changes are technical or functional, which determines who approves them.

## 12.2-R04 | K.1.2.4
<!-- hash: 71bb4b484a41 -->

**Requirement** (RFP §12.2)

> Re-homologation before event use.

R: Major logic/timing changes: re-homologation.

**Justification**

> Full impact assessment will be done. Such changes are expected if TCRR updates rules.

J: A full impact assessment is done, typically for TCRR rule changes.

## 12.2-R05 | K.1.2.5
<!-- hash: 8e9c62c649b9 -->

**Requirement** (RFP §12.2)

> Named approval, backup, rollback, targeted test and retrospective evidence.

R: Emergency fixes: approval, rollback, retrospective evidence.

**Justification**

> It is expected that competition staff will be able to obtain approval as part of the IWF approval chain (e.g. by an on-site technical delegate). As per industry best practices, any emergency remediation will be investigated and a proper corrective fix will be made available under the other clauses of this section.

J: On-site IWF approval is obtained, and a proper corrective fix follows.

## 12.3-R01 | H.2.1
<!-- hash: 2b1bb5301fc8 -->

**Requirement** (RFP §12.3)

> Only the approved baseline and event configuration may be used.

R: Only approved baseline and configuration.

**Justification**

> Agreed.

J: Agreed.

## 12.3-R02 | H.2.2
<!-- hash: d7056c3a6a88 -->

**Requirement** (RFP §12.3)

> Actual hardware, software, network, power, displays, spares and staffing must match the approved event design.

R: Event setup matches approved design.

**Justification**

> Agreed.

J: Agreed.

## 12.3-R03 | H.2.3
<!-- hash: 8c025b0dd6ee -->

**Requirement** (RFP §12.3)

> Dress rehearsal and failure/recovery test shall be completed before readiness sign-off.

R: Dress rehearsal and recovery test before sign-off.

**Justification**

> Agreed.

J: Agreed.

## 12.3-R04 | H.2.4
<!-- hash: de4350046940 -->

**Requirement** (RFP §12.3)

> Substitutions require approved deviation or delta testing before use.

R: Substitutions need approval or delta test.

**Justification**

> Agreed. Note that operator stations used by technical officials are only actually dependent on the capability to run a current mainstream browser and on the presence of HDMI ports and USB ports. Any Windows laptop with these features can be substituted in an emergency.

J: Agreed; any laptop with a browser, HDMI and USB can substitute.

## 12.4-R01 | H.3.1
<!-- hash: edddb9736aa6 -->

**Requirement** (RFP §12.4)

> Defined escalation contacts and event support.

R: Escalation contacts and event support.

**Justification**

> Competition staff will be provided with that information. Escalation staff will either be directly on site or available remotely.

J: Contacts are provided, with escalation staff on site or remote.

## 12.4-R02 | H.3.2
<!-- hash: 5128d922f749 -->

**Requirement** (RFP §12.4)

> Critical incidents documented, contained, investigated and reported.

R: Critical incidents handled and reported.

**Justification**

> Supplier will provide an issue reporting environment and ensure follow-up and resolution of incidents.

J: Incidents are tracked and followed to resolution.

## 12.4-R03 | H.3.3
<!-- hash: 71e3e4367069 -->

**Requirement** (RFP §12.4)

> Serious incidents, unauthorised changes, false evidence, unsupported baselines or material security compromise may lead to restriction, suspension or withdrawal.

R: Serious breaches may lead to suspension.

**Justification**

> Agreed.

J: Agreed.

## 12.5 | K.2
<!-- hash: 3cb90f3e15da -->

**Requirement** (RFP §12.5)

> IWF shall receive the rights, documentation, exports, interfaces and operational access necessary to use, verify, archive and maintain the approved system and competition records.

R: IWF rights, documentation and access.

**Justification**

> The supplier retains its intellectual property except where otherwise agreed. IWF shall receive the rights, documentation, exports, interfaces and operational access necessary to use, verify, archive and maintain the approved system and competition records. Final contractual wording will be agreed with the selected supplier.

J: These rights are granted; the supplier keeps its IP, with wording per contract.

# RFP Section 13

## 13-R01 | J.1.1
<!-- hash: 5376397759f9 -->

**Requirement** (RFP §13)

> The Licensing Fee for the four-year period is US$20,000, payable on an annual basis of US$5,000 per year.

R: US$20,000 licence fee over four years.

**Justification**

> US$5,000 per year / US$20,000 total

J: The fee is US$5,000 per year.

## 13-R02 | J.1.2
<!-- hash: 45202740c852 -->

**Requirement** (RFP §13)

> Licence

R: Licence.

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]

## 13-R03 | J.1.3
<!-- hash: 0aff1ab3af8b -->

**Requirement** (RFP §13)

> Implementation

R: Implementation.

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]

## 13-R04 | J.1.4
<!-- hash: 8486b8c08808 -->

**Requirement** (RFP §13)

> Equipment

R: Equipment.

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]

## 13-R05 | J.1.5
<!-- hash: 46e8b87568fb -->

**Requirement** (RFP §13)

> Support / maintenance

R: Support / maintenance.

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]

## 13-R06 | J.1.6
<!-- hash: bc3091c6a005 -->

**Requirement** (RFP §13)

> Optional services

R: Optional services.

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]

## 13-R07 | J.1.7
<!-- hash: 32017247f7a1 -->

**Requirement** (RFP §13)

> Recurring charges

R: Recurring charges.

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]
