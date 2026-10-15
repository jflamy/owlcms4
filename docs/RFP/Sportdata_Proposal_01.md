# Compliance table summaries

Extracted from `Sportdata_Proposal_01.docx`. Fill in the `R:` (requirement) and `J:`
(justification) lines; keep each on one line. Do not edit the ## headings.
Quoted text is for reading only; fix the full text in Word.
Empty summaries keep the original text when reinjected.
`FIX:` lines flag problems to correct in the proposal itself; delete a `FIX:` line
once the proposal is corrected.

# RFP Section 2

## 2.1 | B.2.1

**Requirement** (RFP §2.1)

> Complete sport logic, mandatory FOP controls, screens and official documents.

R:

**Justification**

> OWLCMS version 69 and Blue Owl version 3 are fully compliant with the TCRR as of October 2026.

J: OWLCMS 69 and Blue Owl 3 are fully compliant with the TCRR as of October 2026.

## 2.1-R02 | B.2.2

**Requirement** (RFP §2.1)

> M0 plus baseline control, audit, interfaces, offline operation, recovery, procedures, training, support and spares.

R:

**Justification**

> The system as delivered will meet all the stated requirements. OWLCMS includes a module specifically meant for the creation of external data interfaces. The hardware devices use documented protocols. Baseline control will be according to section K.1.2.

J: The delivered system meets all stated requirements, with a dedicated module for external data interfaces and documented device protocols. Baseline control follows section K.1.2.

## 2.1-R03 | B.2.3

**Requirement** (RFP §2.1)

> M1 plus applicable VPT, public/live services, broadcast outputs, enhanced monitoring, high availability and full rehearsal.

R:

**Justification**

> Video playback uses video-capable equipment that is included in the proposal (laptops with video capture/gaming cards, cameras). OWLCMS has its own standalone VPT capabilities and provides a core set of TV Graphics for broadcasts. OWLCMS replay streams for each lift can be fetched from the replay server once networking access configurations are done. Furthermore, Supplier can create additional TV graphics for specific event requirements; the actual creation and testing of such items would have to be negotiated with the supplier

J: Video-capable equipment, standalone VPT, per-lift replay streams and a core set of TV graphics are included. Additional event-specific TV graphics are negotiable with the Supplier.

## 2.1-R04 | B.2.4

**Requirement** (RFP §2.1)

> M2 plus approved Games/ORIS and programme-specific integrations.

R:

**Justification**

> OWLCMS includes a fully documented external interface data format and sample applications that leverage it. All competition data, and every single field-of-play event (weight changes, decisions, etc.) are available for propagation on the Games feeds.
> OWLCMS has previously been integrated with Olympic Data Format feeds using that approach.
> The interfacing capability is an integral part of the proposal; the actual creation and testing of specific interfaces for specific external formats or events would have to be negotiated with the Supplier.
> Figure 1: Solution Overview
> Figure 2: Detail: VPT and Interfaces

J: A fully documented external data stream exposes all competition data and field-of-play events, and has previously been integrated with ODF feeds. Creating and testing event-specific interfaces is negotiable with the Supplier.

## 2.1-R05 | B.2.5

**Requirement** (RFP §2.1)

> Simultaneous platform operation with separation and capacity evidence.

R:

**Justification**

> OWLCMS has been used in actual competitions with up to 6 platforms simultaneously and 1600 athletes competing, with extremely low computer and network usage.
> When running multiple platforms, a single instance of OWLCMS runs them all together, and no integration step for results is required. All results and rankings are available live for all platforms. The normal constraints expressed in TCRR (group B should finish before group A for a given category) should nevertheless be respected so coaches get meaningful information.
> The program is verified with a stress test that simulates competition updates as fast as possible in real life, using the entry totals to generate credible lift values. Any competition can be simulated as soon as preliminary registration data is available to confirm expected performance and validate reports

J: In multiple competitions, OWLCMS has run 3 to 6 simultaneous platforms and 1600 athletes, with live results and very low resource usage. Capacity can verified beforehand by a stress test simulation using preliminary registration data.

## 2.2 | B.3.1

**Requirement** (RFP §2.2)

> The licence certificate shall state the supplier and product.

R:

**Justification**

> The supplier of the product shall be [Supplier], who will act as integrator of the OWLCMS and Blue Owl solutions and any additional components required to meet the claimed profiles as per requirements.

J: Sportdata is the supplier and acts as integrator of the OWLCMS and Blue Owl solutions and any additional components required for the claimed profiles.

## 2.2-R02 | B.3.2

**Requirement** (RFP §2.2)

> The licence certificate shall state the exact baseline.

R:

**Justification**

> Any stable version of OWLCMS after version 69 is claimed to meet the current requirements and is fully compliant with TCRR as published at the time of this tender. Any version of the Blue Owl software after version 3.0 meets the requirements. Baseline will be established at acceptance and evolve according to section K.1.2.

J: Any stable version of OWLCMS after 69 and Blue Owl after 3.0 meets the requirements and the TCRR. The baseline is established at acceptance and evolves according to section K.1.2.

## 2.2-R03 | B.3.3

**Requirement** (RFP §2.2)

> The licence certificate shall state approved profiles and capacities.

R:

**Justification**

> M3 and MP profiles: The system complies with the M3 and MP profiles, up to 6 platforms. For the purpose of this license, the number of participants shall be 1800 or less. There is no intrinsic limitation on the number of participants; this number is given as a safe, previously observed figure with a margin of safety.
> Note that actual integration and testing with external systems such as Games feeds is event-specific as the supplier of these services varies from Games to Games.

J: The system complies with the M2 and MP profiles for up to 6 platforms and 1800 participants, a safe figure with no intrinsic limit. For M3, integration capability is included but actual external integration and testing are not included as they vary depending from event to event.

## 2.2-R04 | B.3.4

**Requirement** (RFP §2.2)

> The licence certificate shall state interfaces and critical dependencies.

R:

**Justification**

> The system has no critical dependencies. All communications use the standard HTTP and MQTT protocols.
> The specific MQTT messages used for the devices are fully documented. The physical interfaces for devices are USB and RJ-45 connectors. Only commercially available cabling (CAT-6, USB 2.0, HDMI) is used.
> The protocol used to create additional interfaces for Data Exports, TV Graphics, etc. is public and has a published reference implementation.

J: There are no critical dependencies; only standard HTTP and MQTT, documented MQTT device messages, USB and RJ-45 interfaces, and commercial cabling are used. The protocol for additional interfaces is public with a published reference implementation.

## 2.2-R05 | B.3.5

**Requirement** (RFP §2.2)

> The licence certificate shall state limitations and deviations.

R:

**Justification**

> See section B.3.3 for context on profile M3. No limitations on profiles M0, M1, M2 and MP.

J: Profiles M0, M1, M2 and MP have no limitations; M3 has been fully demonstrated but is event-dependent as described in section B.3.3.

## 2.2-R06 | B.3.6

**Requirement** (RFP §2.2)

> The licence certificate shall state validity and surveillance conditions.

R:

**Justification**

> The certificate will be valid for 4 years after date of issuance.

J: The certificate is valid for 4 years after issuance.

# RFP Section 3

## 3-R01 | E.1.1

**Requirement** (RFP §3)

> Import and validate athlete, federation/NOC, official, event, venue and record data; stable IDs and error reporting.

R:

**Justification**

> Registration data is converted to an Excel-style sheet with the athlete identifier, name, and lifting data. The input format handles multiple age groups and supports team allocation. The intake format can be exported, edited offline, and re-ingested without loss of information.
> The same applies to schedule changes and referee assignments that can be modified in spreadsheet form until the drafts are considered stable.
> All information is stored in a relational database. The program performs validation when loading to ensure integrity.

J: Registration data, schedules and referee assignments are handled as spreadsheets that can be exported, edited offline and re-ingested without loss, supporting multiple age groups and teams. All data is stored in a relational database and validated on loading.

## 3-R02 | E.1.2

**Requirement** (RFP §3)

> Corrections, approvals, random lots, grouping, timetable and Technical Official assignments.

R:

**Justification**

> The program prints VFE verification sheets that show exactly which athlete is in which category or categories (see section E.2.4.2). This allows the competition supervisor to verify that eligibility rules have been respected, and to know in advance what information will be required at VFE (e.g., which teams need to remove substitute athletes from their roster). The program also prints team membership sheets to verify that all team allocations are correct. Team representatives can sign these sheets to support the VFE process and reduce errors. The TIS team uses these sheets for data capture.
> The database includes a list of technical officials and their team assignments. The uploaded referee team timetable can be edited interactively. In addition, individual assignment changes can then be made as required (e.g., substituting one referee for another for a given session). See Section E.2.3.2.3.

J: VFE verification and team membership sheets let the supervisor check category eligibility and team allocations, and support signatures and data capture. Technical official team assignments are stored in the database and can be edited interactively, including individual substitutions per session.

## 3-R03 | E.1.3

**Requirement** (RFP §3)

> Approved Start List and controlled distribution/versioning.

R:

**Justification**

> Fully formatted start books are produced. See section E.2.4.2.2 for examples. A preliminary version can list participants in each category, with subsequent revisions adding the schedule and officials’ assignments. There are also spreadsheet formats for the actual schedules and start lists for convenient publishing.
> Records are loaded in OWLCMS using a spreadsheet (they are converted from the data made available by the federations).
> For convenience, individual documents are available for the competition schedule, for referee assignments, for start lists, etc.

J: Fully formatted start books are produced, along with spreadsheet schedules, start lists and other verification documents. Records are loaded from spreadsheets converted from federation data.

## 3-R04 | E.1.4

**Requirement** (RFP §3)

> Correct timing, bodyweight, first attempts, 20 kg checks, start numbers, signatures and closed snapshot.

R:

**Justification**

> OWLCMS produces field-tested weigh-in forms to capture both the weight-on-scale and deducted weight. The form captures all the required signatures. See section E.2.3.4 for screens and E.2.4.3 for documents. Compliant athlete cards are printed (see E.2.4.4)
> During data entry, all applicable rules are checked by the pro, including 20 kg and qualifying totals where applicable. A special data entry mode is used for quick capture of weigh-in data.
> Start numbers are assigned by the program at the end of weigh-in so they can be checked against distributed bibs. The program produces a speaker introduction sheet for athletes and officials, as well as empty protocols and jury member sheets.
> The competition supervisor takes a snapshot backup copy of the database at this point.

J: Weigh-in forms capture scale and deducted weights and all signatures. Data entry checks the 20 kg and qualifying total rules. Start numbers are assigned at the end of weigh-in, empty protocols and jury sheets are printed, and a database snapshot backup is taken.

## 3-R05 | E.1.5

**Requirement** (RFP §3)

> Calling order, declarations/changes, 60/120-second timing, referee decisions, records and live outputs.

R:

**Justification**

> The system fully complies with the competition flow.
> All marshal data entry is immediately handled by the system and all lifting order or requested weights are instantly updated on all displays and scoreboards. Referee decisions and jury reversals are automatically handled, and all scoreboards and attempt boards are immediately updated.
> All the normal flow timing decisions are automatic (the clock is automatically set to the right allocated time for each athlete, including on fast sequences of changes). Only explicit loading incidents require the use of the 1:00 or 2:00 reset buttons.
> The 20 kg rule is systematically checked and any deviation is explicitly visible to the marshal (including the necessary amounts for corrections). See section E.2.3.5.
> Records attempted are shown as such (see section E.2.3.15), and any record improved is registered in the competition database.

J: Marshal entries, referee decisions and jury reversals instantly update all displays, and clock timing is set automatically. The 20 kg rule is systematically checked, attempted records are shown, and improved records are registered.

## 3-R06 | E.1.6

**Requirement** (RFP §3)

> Controlled Snatch-to-Clean & Jerk transition and synchronized displays.

R:

**Justification**

> Transition from the snatch to the clean & jerk break at the end of the snatch is automatic. Jury deliberation or challenge on the very last snatch lift is correctly handled (the snatch is not done until resolution).
> Default rules for longer breaks (few athletes) and shorter breaks (large sessions) may be given as part of the event-specific rules. Break durations can also be changed during the meet, at any time before the break start. Break duration can also be set at break time in case of a last-minute change.

J: The transition to the Clean & Jerk break is automatic and correctly waits for any jury resolution on the last snatch. Break durations follow event-specific defaults and can be changed at any time by the supervisor.

## 3-R07 | E.1.7

**Requirement** (RFP §3)

> Calling order, weight changes, 20 kg enforcement, totals, ranks and records.

R:

**Justification**

> See section E.1.5 Snatch; the Clean & Jerk flow is completely automated in the same way.

J: The Clean & Jerk flow is fully automated in the same way as the snatch, as described in section E.1.5.

## 3-R08 | E.1.8

**Requirement** (RFP §3)

> STOP, Jury, challenge/VPT where applicable, additional attempts, correction, withdrawal and restart.

R:

**Justification**

> The jury can start a deliberation or accept a challenge using a physical jury keypad button or using on the jury control display. A flashing STOP sign is shown to the athlete on the platform, and all attempt boards and displays indicate the interruption. Transmission of the jury reversal code to the speaker can be done the traditional way (by the technical controller), or automatically when the jury president chooses the reversal code. In either case, the jury or challenge decision is shown as the speaker announces it. See section E.2.3.10.
> The system can optionally display jury decisions on the scoreboards and the attempt boards (like IWF has been doing experimentally in its own championships). This is optional as it is not part of TCRR.

J: The jury can start a deliberation or accept a challenge from a keypad or display. All attempt boards show the reason for interruption., Reversal codes reach the speaker automatically if entered on the Jury Control display. Jury decisions can optionally be shown to the public on scoreboards and attempt boards.

## 3-R09 | E.1.9

**Requirement** (RFP §3)

> Protocol reconciliation, signatures/status, results, classifications and records.

R:

**Justification**

> The session protocol sheet is available immediately after the session for signature by the jury president. It contains all results, ranking information and each occurrence of improved records.
> Should any correction be required, the athlete card can be adjusted from the results page, with full traceability in the audit log. Errors in registration data (for example, wrong eligibility for a record) can be fixed in the same way, and records re-computed prior to emitting an amended protocol for signature.

J: The session protocol with results, rankings and improved records is available for signature immediately after the session. Corrections to athlete cards or registration data are traceable in the audit log, and records can be recomputed before an amended protocol is issued.

## 3-R10 | E.1.10

**Requirement** (RFP §3)

> Medallists, public/broadcast outputs, communications and revised outputs.

R:

**Justification**

> At the end of a session, a medal sheet is available for the speaker. The medal sheet correctly reports the categories that are actually finished (important when A and B groups from different bodyweight categories are together in the same sessions). Combined category results are also available (showing the ranking for all combined A/B/C… groups)
> The system has special purpose scoreboards for ceremonies.
> Public scoreboards are styled to match the event colours. They automatically switch to a start list during introductions. During medal ceremonies, the technical table selects which category is receiving medals so the display switches accordingly. See section E.2.3.16.
> Broadcast-oriented scoreboards are typically visually similar to public scoreboards but styled differently (typically a transparent format is used). If the video producer selects the medal display, it will track what the technical table selects as the current medal ceremony.

J: Medal sheets correctly report only finished categories, combined category results are available, and dedicated ceremony scoreboards are present. Public and broadcast medal scoreboards are styled for the event; the competition supervisor can track the ceremonies them when several age groups are present .

## 3-R11 | E.1.11

**Requirement** (RFP §3)

> Final dataset, official outputs, logs, revisions, evidence and machine-readable export.

R:

**Justification**

> The full database content is exported in JSON format. This format is suitable for archival – OWLCMS can read archives produced several years ago.
> The archived documents should also include
> A “final result package” in Excel format (all categories, best athlete rankings, team points, medals)
> A spreadsheet document with the protocol content for all individual sessions
> The full “publication format” formatted PDF results book
> The audit logs

J: The full database is exported in JSON, which OWLCMS can read years later. Archival material also includes the audit logs, the Excel final results package, a spreadsheet with all session protocols, and the PDF results books.

## 3.1-R01 | E.1.12.1

**Requirement** (RFP §3.1)

> Snatch followed by Clean & Jerk, maximum three attempts per lift, correct Total calculation.

R:

**Justification**

> The database explicitly stores 3 attempts per lift and the associated information. The state automaton enforces the sequence. The total calculations comply with TCRR rules are subject to automated tests performed before each release.

J: The database stores 3 attempts per lift, the state automaton enforces the sequence, and TCRR total calculations are verified by automated tests prior to each release.

## 3.1-R02 | E.1.12.2

**Requirement** (RFP §3.1)

> Whole-kilogram weights and applicable minimum starting weights.

R:

**Justification**

> The database explicitly stores 3 integer values for the weights. The 20kg rule is systematically verified on any declaration or weight change. The 20kg rules have their own suite of automated tests performed before each release.

J: Weights are stored as 3 integer values per lift, and the 20 kg rule is verified on every declaration or change with its own automated test suite performed before each release.

## 3.1-R03 | E.1.12.3

**Requirement** (RFP §3.1)

> Correct calling order, including weight, attempt number, previous-attempt sequence and start number.

R:

**Justification**

> The calling order is explicitly programmed to determine the next athlete based on the 4 TCRR criteria. These rules have their own automated tests.

J: The calling order is programmed to apply the 4 TCRR criteria and has its own automated tests.

## 3.1-R04 | E.1.12.4

**Requirement** (RFP §3.1)

> Permitted weight changes, final-call and consecutive-attempt rules.

R:

**Justification**

> The state automaton remembers the athlete for which the clock was last started. It correctly resumes the clock at the correct remaining time value when several intervening changes bring back the lifting order to that athlete. The state automaton also determines correctly two-minute situations. The correct “1 minute only” rule is applied when the same athlete lifts twice in succession, but the clock has started on another athlete
> Violations of the late-change delays are signalled to the marshal. The marshal can accept a change that, in their opinion, was signed in time but could not be typed in fast enough.
> The system will also systematically detect and report violations when an athlete is moving down but should already have lifted. In order to do so, the system recomputes the full prior lifting order as it should have taken place, on every weight change.

J: The state automaton automatically sets clock countdowns, handles two-minute and one-minute situations, and signals late-change violations that the marshal can override. Recomputing the prior lifting order on every weight change detects athletes moving down who should already have lifted.

## 3.1-R05 | E.1.12.5

**Requirement** (RFP §3.1)

> Withdrawals, no-lift/no-Total conditions, disqualification and additional attempts.

R:

**Justification**

> The marshal screen has explicit buttons to declare withdrawals of either the snatch or both events. The computation rules correctly register failed lifts and compute no totals for bomb-outs. If an athlete is disqualified, the reason can be captured.
> If an attempt is given again, and this results in the same athlete card field being rewritten, the audit trail will show the edition of athlete data. This can then be compared with the manual edits done on the athlete card.

J: The marshal screen has explicit withdrawal buttons. Failed lifts and bomb-outs are processed correctly, and disqualification reasons can be captured. Rewritten athlete card fields resulting from additional attempts being granted appear in the audit trail for comparison with the athlete cards.

## 3.1-R06 | E.1.12.6

**Requirement** (RFP §3.1)

> Material corrections recalculate affected rankings, records and outputs.

R:

**Justification**

> Any change to the athlete card data for any athlete causes a recalculation of all affected ranks. Recalculation of records can be triggered on the records management page if there is a change posterior to the actual lift. Reports and result books can be regenerated on demand.

J: Any athlete card change recalculates all affected ranks. Records can be recalculated on demand, as can reports and result books.

# RFP Section 4

## 4.1-R01 | E.2.1.1

**Requirement** (RFP §4.1)

> Authoritative electronic countdown in one-second increments, configurable up to at least 15 minutes.

R:

**Justification**

> Athlete countdown information is updated at one-second increments on the athlete-facing clock, on all attempt boards, and on scoreboards. Breaks can be set to any duration.

J: Athlete countdowns update in one-second increments on the athlete-facing clock, on the attempt boards and on scoreboards. Break countdowns can be set to any duration.

## 4.1-R02 | E.2.1.2

**Requirement** (RFP §4.1)

> Automatic 60- and 120-second attempt clocks and applicable introduction/break countdowns.

R:

**Justification**

> Clock ownership is automatic; no intervention is required to set the clock to 1:00, 2:00 or to the time remaining for the athlete.
> The system has break countdowns prior to introduction, prior to first snatch and prior to first clean & jerk. A break management dialog in the user interface does the automatic transition between these countdowns, and also allows overriding the durations are required.

J: The clock is set automatically to 1:00, 2:00 or the athlete's remaining time. The break management dialog steps automatically through the introduction, and first snatch countdowns. Clean&Jerk break is automatically handled. Durations of breaks can be overridden.

## 4.1-R03 | E.2.1.3

**Requirement** (RFP §4.1)

> Correct start, stop, restart and STOP behaviour according to the rules.

R:

**Justification**

> The timekeeper start, stop and restart behavior is according to TCRR.

J: The timekeeper start, stop and restart behaviour follows the TCRR.

## 4.1-R04 | E.2.1.4

**Requirement** (RFP §4.1)

> All connected displays use the same authoritative time state within the approved tolerance.

R:

**Justification**

> Time is kept on the OWLCMS server. All devices are time-synchronized to universal time. When a timer countdown is sent to remote browsers for display, the actual universal time on the server is included so the remote machine can determine how much propagation drift took place and adjust the countdown as needed.

J: Time is kept on the OWLCMS server, and all display stations are synchronized to universal time. Each countdown sent to a display includes the universal time so the display can correct for propagation drift.

## 4.1-R05 | E.2.1.5

**Requirement** (RFP §4.1)

> Time corrections are authorised, recorded and auditable.

R:

**Justification**

> All machines are configured NTP-synchronized at the operating system level, and an NTP source is configured on the network as part of the system setup. Time drift corrections are logged in the operating system journals. A manual resynchronization should be treated the same as an equipment replacement and logged manually by the competition supervisor, as a procedural requirement beyond the scope of the competition automated logs.

J: All machines use NTP from a source on the event network, and journalization is done by the operating system, outside the scope of the competition management software. The competition supervisor should report manual synchronizations as infrastructure incidents, like an equipment replacement.

## 4.2-R01 | E.2.2.1

**Requirement** (RFP §4.2)

> Three independent referee control boxes with white/red decisions and required indicators.

R:

**Justification**

> Blue Owl referee buttons have visual indicator, sound, and vibration.

J: Blue Owl referee buttons give visual, sound and vibration indications.

## 4.2-R02 | E.2.2.2

**Requirement** (RFP §4.2)

> Correct majority/Down signal, reminder and reversal-window behaviour.

R:

**Justification**

> Implemented by the state automaton. 3-second reversal window is enforced as per TCRR. Should a late reversal be done using flags, the competition supervisor can easily perform the reversal. See section E.2.3.8 for an example of reversal.

J: The state automaton implements this behaviour and enforces the 3-second reversal window required by the TCRR. The competition supervisor can update the data when a late flag reversal occurs (see E.2.3.8).

## 4.2-R03 | E.2.2.3

**Requirement** (RFP §4.2)

> Jury visibility of individual referee decisions before public reveal.

R:

**Justification**

> Live referee decisions and reversals are shown to the jury both on the keypad and on the jury control screen. See section E.2.3.10 for exame

J: The jury sees live referee decisions and reversals on its physical keypad and on the jury control screen (see E.2.3.10).

## 4.2-R04 | E.2.2.4

**Requirement** (RFP §4.2)

> Support for approved three- and five-member Jury configurations.

R:

**Justification**

> Jury size is selectable (3, 5 jury members or no jury)

J: The jury size can be set to 3 or 5 members, (or to no jury).

## 4.2-R05 | E.2.2.5

**Requirement** (RFP §4.2)

> Individual Jury decision controls and protected independence between members.

R:

**Justification**

> Individual jury buttons are provided as per TCRR. Decision protection housings are not currently a TCRR requirement; [Supplier] would provide some acceptable housing after clarification of IWF expectations.

J: Individual jury buttons are provided as required by the TCRR.

## 4.2-R06 | E.2.2.6

**Requirement** (RFP §4.2)

> Material decisions and actions are attributable and time-stamped.

R:

**Justification**

> There is a full audit log of every single decision, timestamped to the millisecond, with the originating station or device.

J: A full audit log records every decision with a millisecond timestamp and the originating station or device.

## 4.3-R01 | E.2.3.1

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide System health.

R:

**Justification**

> A technical log of the system is constantly updated and can be monitored. This includes technical indicators on the environment, notably CPU and memory usage. The normal operating system gauges can also be used. Note that OWLCMS is typically well under 10% of CPU usage, and under 2GB of memory even for very large events, so the operating system gauges are usually entirely sufficient.

J: A continuously updated technical log reports CPU and memory usage, and standard operating system gauges are used for I/O and networking.

## 4.3-R02 | E.2.3.2

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Event setup.

R:

**Justification**

> Competition setup is done using interactive screens. Initial data capture can be prepared using spreadsheets to accelerate the process. Green is used in the software to indicate the more common actions.

J: Competition setup is done on interactive screens; spreadsheets are used for bulk input.

## 4.3-R04 | E.2.3.2.2

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide lots/groups/timetable.

R:

**Justification**

> For an IWF event, it is expected that a spreadsheet or CSV format of registration data will be available. This format is then converted to the OWLCMS format that contains the expected information, including athlete identifiers.
> The OWLCMS registration format also includes a sheet to define the schedule. In this way, the initial schedule and session assignment can be done in a spreadsheet using whatever means the organisers are comfortable with.
> [Supplier] has its own software for creating such initial schedules and session/group assignments
> OWLCMS then loads the registration data and validates it. OWLCMS can re-export registration data and adjustments can be made on the export, and re-loaded. Such round-trips can be made until satifsfactory. Once stable the registration data is editable directly in the application.
> The registration screen allows modification of the allocated session, moving the athlete to another group, adjusting eligibility to multiple age groups as appropriate.
> The other tabs on the registration form cover things like record eligibility (in joint jurisdiction championships, not all athletes are eligible to improve all records)
> The session timetable is editable.

J: Registration and schedule data are converted from spreadsheet or CSV format and loaded with validation. Export/reload round trips are repeated until the data is satisfactory. Once stable, sessions, groups, age groups, record eligibility, and the refereeing timetable can be edited directly in the application.

## 4.3-R07 | E.2.3.4

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Weigh-in.

R:

**Justification**

> The weigh-in data entry form is intentionally the same format as the registration form. All relevant checks for the 20 kg rule are applied at data entry in case the weigh-in TOs missed them.
> After weigh-in, the page allows creation of all relevant documents for the speaker (an introduction sheet) and the jury (jury protocol sheets)

J: The weigh-in form uses the same format as the registration form and applies the 20 kg rule checks during data entry. It also produces the speaker's introduction sheet and the jury protocol sheets.

## 4.3-R08 | E.2.3.5

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Chief Marshal.

R:

**Justification**

> The marshal screen shows all athletes in start order by default (this can be changed to lifting order if the operator of the station prefers). Clicking an athlete brings up the athlete card. The layout is identical to the paper card for easy comparisons. Focus is automatically brought to the right cell, so typing the desired value and hitting Enter works immediately for fast data entry.
> Violations such as 20kg rule violations or late changes are immediately made visible and prevent the card from closing. When the card was signed on time and the delay came from data entry, the operator can accept the change.

J: The marshal screen opens an athlete card laid out like the paper card. The cursor is positioned on the correct cell. Violations such as 20 kg rule breaches or late changes are flagged and keep the card open, with the chief marshal able to accept corrections.

## 4.3-R09 | E.2.3.6

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Timekeeper.

R:

**Justification**

> During the competition a tablet-friendly display is used, that can be used together with the Blue Owl buttons, as preferred.
> The timekeeper can set the break durations to arbitrary values as required by TCRR. In actual practice, this is typically handled by the competition supervisor (see section E.2.3.8)

J: In addition to timekeeper physical buttons, a tablet-friendly timekeeper display can be used. Break durations can be set to any value required by the TCRR, though the competition supervisor usually handles this.

## 4.3-R11 | E.2.3.8

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Competition supervisor.

R:

**Justification**

> The competition supervisor screen is the as the speaker, with the addition of the decision lights that can be seen live as an option. This is because the switches between sessions, and breaks, and lift overrides after a late referee flag call can all be done from that screen. Depending on competitions, the speaker screen will be made “passive” if the technical table assumes those tasks instead of an ITO1 comfortable with technology.
> The competition supervisor can watch the competition-level audit log as it is written.
> There are two additional technical logs that can be watched. There is a specific log for application health indicators (memory and CPU usage etc.). There is also a full technical logs that traces the internal application events and all the transitions of the competition state automaton, for all platforms, in real time. Program exceptions, if any, are visible in this technical log.

J: The supervisor screen has all the information of the speaker and live decision lights. It handles session changes, breaks and decision changes after late flag calls. The supervisor can also watch the audit log, an application health log, and a real-time technical log of state transitions and exceptions.

## 4.3-R12 | E.2.3.9

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Referee monitoring.

R:

**Justification**

> The competition supervisor screen can see the live referee lights, as does the jury. The audit log shown in section E.2.3.9traces each individual decision and each weight change from the marshals.

J: The competition supervisor and the jury can see live referee lights. The audit log records each decision and each marshal weight change.

## 4.3-R13 | E.2.3.10

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Jury control.

R:

**Justification**

> The jury control can be done entirely using a Blue Owl jury control keypad
> In the context of an IWF meet, it becomes desirable to accelerate the transfer of decisions from the jury to the speaker. To this effect, a tablet-style jury display is available that has the same features as the keypad
> When this is used, the Speaker and Competition Supervisor screens get the reason immediately, and can resume the competition after announcing it.

J: The jury can be operated entirely from the Blue Owl keypad or from an equivalent Jury Control display. The jury control display is used to send the decision reason codes immediately to the speaker and supervisor screens.

## 4.3-R14 | E.2.3.11

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Attempt boards.

R:

**Justification**

> Attempt boards show all the information required by TCRR. They can also display the flag of the country. If one or more record attempts take place the attempt boards show it as a rolling banner.

J: Attempt boards show all information required by the TCRR, optionally with the country flag and/or athlete picture, and show record attempts in a rolling banner.

## 4.3-R15 | E.2.3.12

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Scoreboards.

R:

**Justification**

> Scoreboards contain all the requisite information. This is the warm-up room layout, which does not change during ceremonies so coaches can count attempts. The top line contains all the current attempt information. The bottom line contains additional information, such as the various age groups an athlete is competing in, rank if successful, jury decisions, etc.
> In the main room, styling will be applied as defined by the event, and the top row is used as a banner.
> Jury Decisions are shown at the bottom if this optional feature is enabled (as it is not currently officially in TCRR) Full-fledged video production companies can replace this scoreboard and the TV Graphics by using the extension capabilities provided by OWLCMS. The extension protocol is documented with a reference implementation at https://github.com/owlcms/tracker-core .

J: Scoreboards show current-attempt information and extra details such as age groups, "rank if successful" and optionally, jury decisions. Even styling and colours are used for the main room.

## 4.3-R16 | E.2.3.13

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide RTDS/warm-up.

R:

**Justification**

> In OWLCMS all the screens provide RTDS capabilities (Real-time Display System)
> When decisions are given, they are shown both on the attempt board and on the scoreboards (as shown in section E.2.3.12.
> Jury decisions (3 or 5) can also be shown on warm-up and/or public attempt boards if so desired. This allows coaches to see the jury vote as they may not be in a position to view the main scoreboard.

J: Every OWLCMS screen has real-time display capabilities. Decisions appear on both attempt boards and scoreboards. Jury votes (3 or 5) can optionally be shown on warm-up and public attempt boards.

## 4.3-R17 | E.2.3.14

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide STOP.

R:

**Justification**

> The jury (section E.2.3.10) and the competition supervisor (section E.2.3.8) can stop the competition at any time. In addition, the marshal can also trigger a competition stop to sort out lift order issues using a button at the top right of their station screen (section E.2.3.5).
> During deliberation, challenge, technical issues, or other stoppages, the attempt board shows the reason for the break, and the athlete-facing clock display shows a flashing stop.

J: The jury, the competition supervisor and the marshal can stop the competition. During a stoppage, the attempt board shows the reason and the athlete-facing clock flashes STOP.

## 4.3-R18 | E.2.3.15

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Records.

R:

**Justification**

> If there is a record attempt, the speaker and supervisor screens show the details of what is being attempted and what records are being improved.
> The records attempted are visible on the scoreboards
> The attempt boards show a scrolling ticker of all the records being attempted. Successful records are highlighted.

J: Speaker and supervisor screens show the details of record attempts so they can be announced, and the attempted records appear on the scoreboards. Attempt boards show a scrolling ticker of attempted records, and successful records are highlighted.

## 4.3-R19 | E.2.3.16

**Requirement** (RFP §4.3)

> At minimum, the TIS shall provide Results.

R:

**Justification**

> Official results are of course available in a variety of formats, refer to sections E.2.4.7 and E.2.4.8
> Scoreboards with medals or rankings are available. When there are mixed A and B groups in the same sessions, the system correctly determines which categories are actually done. The competition director can select to show all categories in the championships, or show one category at a time.
> An end-of-session medal report is available. The medal report shows the medals in the order expected by the speaker.

J: Official results are available in several formats, and medal or ranking scoreboards correctly detect finished categories in mixed A/B sessions. An end-of-session medal report lists medals in the order the speaker needs.

## 4.3-R20 | E.2.3.17

**Requirement** (RFP §4.3)

> Other approved states.

R:

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]

## 4.4-R01 | E.2.4.1

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Start Lists.

R:

**Justification**

> A Start List is provided. Several formats are available, depending on whether the competition is on one or more platforms.

J: A Start List is provided in several formats, suited to one or more platforms.

## 4.4-R02 | E.2.4.2

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Start List Package.

R:

**Justification**

> All the documents listed in section 3.4.4 of the TCRR are available. A fully formatted start book is available.

J: All documents listed in TCRR section 3.4.4 are available as a start package. A fully formatted start book is available.

## 4.4-R03 | E.2.4.3

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Weigh-in List.

R:

**Justification**

> The weigh-in list allows capturing both the weight-on-scale and the adjusted weights. Signature areas for the attending officials are provided. Some federations require coach signatures; this is also provided.

J: The weigh-in list records both the scale weight and the adjusted weight, and it has signature areas for the attending officials. Some federations require coach signatures, this is also provided.

## 4.4-R04 | E.2.4.4

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Athlete's Cards.

R:

**Justification**

> Athlete Cards with all the required signature fields are provided.

J: Athlete Cards are provided with all required signature fields.

## 4.4-R06 | E.2.4.5

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Manual Protocol.

R:

**Justification**

> An empty protocol sheet suitable for manual entry is available.

J: A blank protocol sheet is available for manual entry.

## 4.4-R07 | E.2.4.6

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Empty Protocol.

R:

**Justification**

> In addition to the manual protocol from section E.2.4.5 the system provides an empty jury protocol sheet. Bigger text boxes allow capturing the decisions according to the TCRR rules for examinations, and the calculation section for exams can optionally be included. The jury may get both empty protocol formats so they can see the records and referees.

J: An empty jury protocol sheet is also provided, with larger boxes for recording decisions under the TCRR examination rules. It can optionally include the examination calculation section. The jury may get both formats in order to see records and referees.

## 4.4 | E.2.4.7

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Protocol.

R:

**Justification**

> A session-end protocol sheet is provided with the results filled in, including record improvements

J: A session-end protocol sheet is provided with the results filled in, including record improvements.

## 4.4-R08 | E.2.4.8

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Results.

R:

**Justification**

> Full competition results, per category, are available in several formats. For example, a category-per-category output in Excel format.
> Flat-file formats, such as the one expected by IWF are also supported for easier importing.

J: Full results by category are available in several formats, including Excel, and flat-file formats such as the IWF format are supported for import.

## 4.4-R09 | E.2.4.9

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Team Classification/Placing.

R:

**Justification**

> The Excel-format final package described in section E.2.4.11 includes a sheet with all the team classification details for IWF points, as well as medal standings. The following example is for a Youth championship. A fully formatted version of the team classifications and medal standings is present in Results Book (see section E.2.4.12)
> For verification purposes, all the individual athlete statistics are also included

J: The Excel final package includes team classifications for IWF points, medal standings, and individual athlete statistics for verification. A fully formatted version appears in the Results Book.

## 4.4-R10 | E.2.4.10

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Records.

R:

**Justification**

> Records improved during a competition can be visualized directly, for a given age group.
> An arbitrary number of record files can be loaded; the system can export records in the same format it reads. Records that are loaded are shown in the Start Book (see Section E.2.4.2.2.6)
> The registration data for each athlete determines who is eligible for what records (for example, a Mexican athlete is eligible for PanAmerican and World Records, but not for the South American ones; OWLCMS handles this automatically).
> If a missing record file was not loaded at the start of the competition (or if an error is found), it is possible to reload the record files and recompute the records set during the competition. New protocol sheets can then be emitted and signed as authentic by the jury presidents concerned.
> At the end of the competition, records can be exported as required. There are both display- and import-oriented formats.

J: Any number of record files can be loaded, re-exported and shown in the Start Book, and registration data determines record eligibility automatically. Records can be exported at the end in display or import formats. Record definitions can be updated, and records can be recalculated on demand.

## 4.4-R11 | E.2.4.11

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Final Results Package.

R:

**Justification**

> OWLCMS produces a spreadsheet with all relevant results, including categories, best athlete scores and team scores

J: OWLCMS produces a spreadsheet with all relevant results, including categories, best athlete scores and team scores.

## 4.4-R12 | E.2.4.12

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Results Book where applicable.

R:

**Justification**

> A fully formatted results book with a table of contents is produced directly with no human intervention. The following sections illustrate a subset; the actual results book is complete with all sections required by the TCRR. Participation and technical official sections like those in the start book are included, for instance (see E.2.4.2.2).

J: A fully formatted results book with all TCRR-mandated content, is produced automatically.

## 4.4-R13 | E.2.4.13

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Machine-readable export.

R:

**Justification**

> The full database content is available on demand in normalized JSON format. The time required to produce is extremely short and the data is consistent; such a snapshot export can be triggered at any idle time during the competition.
> The competition supervisor typically triggers such exports at meaningful times (after a weigh-in, at Clean&Jerk break, or at the end of a session). The backups can also include a sealed version of the audit log, ready to be saved off-site.

J: The full database can be exported quickly and consistently on demand as a normalized JSON. The supervisor typically does this at meaningful moments (after weigh-in, end of session). Sealed audit logs are exported in sync.

## 4.4-R14 | E.2.4.14

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Distribution records.

R:

**Justification**

> Distribution of results is entirely under the control of the competition staff. Competition staff is expected to upload official documents to IWF’s document management systems that will then control distribution. Production of authoritative exports is logged in the audit logs.

J: Competition staff control the distribution of results by uploading official documents to IWF's document management systems. The audit logs record the production of authoritative exports for later integrity validations.

## 4.4-R15 | E.2.4.15

**Requirement** (RFP §4.4)

> At minimum, the TIS shall provide Final evidence/archive.

R:

**Justification**

> The machine-readable export described in section E.2.4.13 and the audit logs described in F.1.1.5 constitute the source of truth. A SHA-256 checksum of the export is logged in the audit log so the time of production in the export can be cross-checked with that written in the log.

J: The machine-readable export and the audit logs are the source of truth. A SHA-256 checksum of each export is logged so the production time and validity of the export can be cross-checked.

# RFP Section 5

## 5-R01 | D.1.1

**Requirement** (RFP §5)

> Controlled capacity for authoritative state and approved services, with spare/recovery path.

R:

**Justification**

> OWLCMS is extremely efficient in memory, input-output and networking. As a reference, the standard OWLCMS setup has been tested to run 1600 athletes on 6 simultaneous platforms on a 8GB Raspberry Pi5 mini-PC.
> Two machines in this proposal qualify as servers, as shown in Figure 1.
> The main station OWLCMS server. This proposal uses a standard 16GB Windows laptop with an SSD disk for running OWLCMS. That station is used by the competition supervisor, and includes Microsoft Office.
> The VPT and Interfaces server. This is a high-end gaming laptop configuration with the capability to run full live video processing for many feeds. The laptop meets the standard for the commercial vMix video software, as a provision for eventual broadcast-level processing.

J: Two server configurations are proposed: a 16GB Windows laptop with SSD and Microsoft Office for OWLCMS, and a high-end gaming laptop for VPT and interfaces.

## 5-R02 | D.1.2

**Requirement** (RFP §5)

> Dedicated, role-controlled competition administration and operation positions.

R:

**Justification**

> The competition supervisor uses the main station as his operator station. In a multi-platform setup, only one director station will run the OWLCMS software, and the others act as server redundancy.
> The other Operator stations are only required to run two browsers as a maximum. The proposed configuration is from the field-tested OTIS kit. Stations are 8GB Raspberry Pi 500s that are cost-effective, easily replaceable, guaranteed to be in production until at least 2036, and designed for use in difficult environments such as classrooms. Operator stations use portable screens that can be replaced if broken.
> The USB ports on the operator stations are used to connect the Blue Owl refereeing devices, as shown in Figure 1 above.
> Keyboards with numeric pads will be provided for the Marshal and Data entry stations.

J: The competition supervisor operates from the main station, and in multi-platform setups only one director station runs OWLCMS while the others provide redundancy. The other operator stations are replaceable 8GB Raspberry Pi 500s with portable screens and USB ports for Blue Owl devices, and the marshal and data entry stations get numeric keypads.

## 5-R03 | D.1.3

**Requirement** (RFP §5)

> Protected authoritative storage, backup and tested recovery.

R:

**Justification**

> A fully relational database called H2 is used. H2 has been used without a single data integrity failure since the initial versions of OWLCMS in 2009, and is extremely quick. The database sizes of a full competition are very small (typically well below 2MB). Furthermore, since OWLCMS preserves the ordering of all events, and the number of data entry operations is very low, there is very little concurrency. The only significant queries are for results production but most of the data fits in main memory for caching.
> The actual H2 binary file can be kept and is machine-independent, in addition to the structured JSON exports. The exports is the primary means of backup and snapshot. A readable consistent snapshot can be obtained at any time from the competition supervisor station. The laptop that implements the interfaces can also pull a snapshot on demand, as a quick recovery capability.
> In a single platform scenario, the VPT+interface laptop acts the spare for the OWLCMS server.

J: A fully transactional relational database is used. Structured JSON exports are the primary backup and can be taken on demand. The VPT and Interface servers are used as spares (exports are copied at frequent intervals and key moments)

## 5-R04 | D.1.4

**Requirement** (RFP §5)

> Three independent red/white referee control devices.

R:

**Justification**

> The Blue Owl referee controls include the two buttons, a visible LED, a sound, and a vibration for warning the referees. Referee controllers are cabled in order to avoid any concern with batteries.

J: Blue Owl referee controls have two buttons plus LED, sound and vibration warnings.

## 5-R05 | D.1.5

**Requirement** (RFP §5)

> Individual devices for applicable Jury members plus President controls.

R:

**Justification**

> Jury controls for jury members are the same as the referee controls, but without the warning features. Referee controllers can be used as backups for jury controllers (the warnings would simply not be emitted).
> President controls are of two kinds. The solution includes a physical keypad with all the required real-time referee decision display and the required jury member behaviour (green until all decisions visible)
> The jury station will also have a tablet-style display that allows the jury president to indicate the reason for lift reversal.

J: Jury member controls are the form-factor as referee controls but without the warning indicators. The jury president gets a physical keypad with real-time decision lights. A jury control screen is used for indicating the reason for a lift reversal.

## 5-R06 | D.1.6

**Requirement** (RFP §5)

> Authoritative timer/controller with approved displays and audio.

R:

**Justification**

> Timing is done on the main server using the operating system timers. Operating system timing resolution is 1 ms as used. Propagation times of MQTT messages is normally well under 10 ms.
> Time is displayed on an athlete-facing display, on the attempt boards, and on scoreboards. Each machine with displays attached is synchronized with the OWLCMS computer and receives the millisecond timestamps at which OWLCMS emitted the command. In this way, the propagation delay (even though it is very small) can be compensated.
> Timer warning and time-over signals are emitted on the down-signal device and logged in the audit log (for example, to determine if the timekeeper stopped the clock before or after the time-over buzzer).

J: The main server uses 1 ms operating system timers, MQTT propagation is normally well under 10 ms Timer warning and time-over signals are emitted on the MQTT down-signal device and recorded in the audit log. For the browser-based displays, the timing messages are timestamped with NTP time so propagation can be compensated.

## 5-R07 | D.1.7

**Requirement** (RFP §5)

> Competition and warm-up presentation with mandatory athlete/attempt/weight/clock/decision data.

R:

**Justification**

> TCRR-compliant attempt boards are provided with all the required information.
> Attempt boards are standard FullHD or 4K TVs with HDMI inputs. The actual TV screens are to be provided locally and are not part of the certified kit.
> As shown in Figure 1, there are two computers running standard web browsers that provide the attempt boards as HDMI outputs. The Vaadin library sends live field of play updates to these browsers over a WebSocket, there is no polling
> The first attempt board output is for the front room and is styled to match the event colour scheme (this is a simple documented action during event setup). A splitter is used to feed one attempt board on each side of the platform.
> The second attempt board output is the normal high-contrast OWLCMS board and is used in all back-room areas (waiting, marshal, warmup). The kit includes splitters and extenders to reach the typical waiting, marshal and warm-up areas located immediately behind the main platform. Further splitting or extension of the signal to reach multiple attempt boards will use HDMI or SDI equipment obtained locally.
> Scoreboard and attempt board connections for the main room would be as illustrated in Figure 3. Figure 4 illustrates the back-room connections for scoreboards and attempt boards.
> Figure 3: Main Room Scoreboard/RTDS Connections
> Figure 4: Warmup Room Scoreboards/RTDS Connections

J: TCRR-compliant attempt boards are provided with all the required information. Two feeds are provided for each. One for the front room, with visual styling matching that of the event, and one for the back room with plain high-contrast styling for coaches.

## 5-R08 | D.1.8

**Requirement** (RFP §5)

> Real-time competition and warm-up displays with correct ranking and state.

R:

**Justification**

> TCRR-compliant scoreboard displays are provided with all the required information.
> The situation for the scoreboards is the same as for attempt boards. There are two HDMI sources, one for the stage area, one for the back. For the stage area, the video signal is assumed to go to a single large LED scoreboard.
> Refer to Figure 3 and Figure 4 for details.

J: TCRR-compliant scoreboard displays are provided with all the required information. Two feeds are provided for each. One for the front room, with visual styling matching that of the event, and one for the back room with plain high-contrast styling for coaches.

## 5-R09 | D.1.9

**Requirement** (RFP §5)

> Approved cameras, recording/replay and Jury output for events requiring VPT.

R:

**Justification**

> The VPT system is under the full control of the competition staff, and the TCRR-mandated angles can be displayed in self-service mode by the jury if so required (or provided by the competition supervisor).
> As indicated in TCRR, the replay clips are accessible by the venue video team and broadcast teams. Such access will be controlled by networking equipment configurations.
> The VPT system relies on four PTZ cameras. The proposal is based on Tenveo PTZ 60fps cameras. These cameras provide Power-over-Ethernet RTSP video feeds, HDMI, and compressed H.264 USB feeds. The wiring shown below assumes RTSP; it is also feasible to use USB wiring directly to the VPT station, or HDMI with capture cards.
> The center camera’s HDMI feed would be used to feed the live screens in the warmup room using HDMI splitters.
> OWLCMS has a VPT module that automatically clips and attributes the replays to the athlete lifting. This module keeps a full indexed archive of all the lifts, at all angles. The VPT will execute on a high-performance laptop with a GPU.
> Figure : VPT Networking and Video Connections
> TODO: add the live video uplink to the back room.

J: The OWLCMS VPT module uses four PoE Tenveo 60fps PTZ cameras. The VPT modules clips replays, attributes them to the lifting athlete and keep an indexed archive of all replay angles. The jury can select the replays to be shown, or this can be done from the technical table by the competition supervisor.

## 5-R10 | D.1.10

**Requirement** (RFP §5)

> Documented, segmented, monitored event network with common time.

R:

**Justification**

> The networking configuration is based on switching. The only specificity is that a minimum of 4 POE (Power-over-Ethernet) ports is necessary for VPT Cameras. Connectivity for VPT cameras is presented separately in section D.1.9.
> Figure 6: Main Room Networking
> Figure 7: Back-room Networking
> All the machines on a given platform are connected to a switch on that platform. OWLCMS networking traffic is minimal. The application is routinely used in a cloud configuration with very remote servers and no perceptible delay.
> In secured installations, only the Interfaces computer is connected to the outside world, and the competition computers are not on WiFi.

J: The network is switched, with at least 4 PoE ports for VPT cameras and one switch per platform (Figures 5 and 6). OWLCMS traffic is minimal. In secured installations, only the Interface computer connects externally. Competition computers do not use WiFi.

## 5-R11 | D.1.11

**Requirement** (RFP §5)

> Controlled read-only path from accepted state to approved consumers.

R:

**Justification**

> There is a specific role for external accesses. The role has access to the scoreboards, attempt boards and lower third overlays provided by OWLCMS.
> OWLCMS includes the capability to also push out the data and events externally such that no read-only path is needed for access by consumers. See also section G.x on integration of TV Graphics.

J: There is a specific role for external accesses. The role has access to the scoreboards, attempt boards and lower third overlays provided by OWLCMS.

## 5-R12 | D.1.12

**Requirement** (RFP §5)

> Reliable official documents with fallback.

R:

**Justification**

> A Windows laptop with Microsoft Office is used for data entry and report generation to maximize compatibility with whatever printer the event organizing committee provides. OWLCMS produces documents in Excel format. For archival, export to PDF in read-only mode can be performed by the competition staff.
> Start books, Result books, Refereeing schedules and similar layouts are produced directly as PDF. For M3 events, the production of these documents is typically customized specifically for the event.

J: A Windows laptop with Microsoft Office produces Excel documents for maximum printer compatibility, with read-only PDF export for archival. Start books, result books and similar layouts are produced directly as PDF and are typically customized for M3 events.

## 5-R13 | D.1.13

**Requirement** (RFP §5)

> Protection and approved autonomy for critical equipment.

R:

**Justification**

> UPS devices will protect the networking switches in the front and back room so the marshal and competition supervisor can still capture changes and perform timing during a short power outage, relying on their laptop batteries.
> When local conditions include frequent brownouts, it is expected that event organisers will provide full site protection (e.g., generators) and this is out of supplier’s scope.

J: UPS devices protect the front and back-room network switches so that laptops on battery can continue to communicate during short outages.

## 5-R14 | D.1.14

**Requirement** (RFP §5)

> Health, service, device, storage, time and stale-state monitoring.

R:

**Justification**

> The OWLCMS software has very low memory and disk I/O requirements. A dedicated log captures the memory and CPU usage from the program runtime every 30s. I/O and networking are tracked by the normal operating system utilities.

J: OWLCMS logs its memory and CPU usage every 30 seconds, and standard operating system utilities track I/O and networking.

## 5-R15 | D.1.15

**Requirement** (RFP §5)

> Rapidly deployable replacements for critical equipment families.

R:

**Justification**

> In the proposed configuration, all operator stations are standard Windows laptops with a minimal standardized configuration that can be substituted at any time.
> The special configuration machines are the Competition (better CPU, Microsoft Office) and the machines doing video processing (VPT or TV Graphics) with a sufficiently powerful GPU. A VPT-class laptop can normally act as spare for all three.
> The supplier has spares for the Blue Owl devices.

J: Operator stations are standardized Windows laptops that can be substituted at any time. Only the competition machine and the GPU-equipped video-processing machines have special configurations with specific spares. Supplier has spare Blue Owl devices.

## 5-R16 | D.1.16

**Requirement** (RFP §5)

> Safe, labelled cabling and equipment placement without obstruction or sightline conflicts.

R:

**Justification**

> Networking cables coming into the 3 switches (main table, refereeing switch and marshal switch) will be pre-labelled with their origin. Cables that cannot be pre-labelled will be labeled on-site (e.g., the cable from the main room to the marshal area). As illustrated in Figure 3 to Figure 6, cabling is routed along or under the stage whenever possible.

J: Cables to the 3 switches are pre-labelled or labelled on site and are routed along or under the stage whenever possible (Figures 3 to 6).

## 5.1 | D.1.17

**Requirement** (RFP §5.1)

> For each licensed event, the supplier shall maintain make/model, quantity, serial or asset ID, firmware, location, connections, power source, owner, health status, spare path and approved deviations.

R:

**Justification**

> Refer to Appendix 1 for the list of components.
> .

J: The list of components is in Appendix 1.

# RFP Section 6

## 6.1-R01 | C.1.1.1

**Requirement** (RFP §6.1)

> Implement the effective IWF rule pack and support controlled updates.

R:

**Justification**

> The solution is tested against the TCRR rules using automated tests. OWLCMS has over 350 automated tests covering rules, reports and program behaviour. Any new release is tested against regressions. Should TCRR be updated in a way that affects the program, a controlled release would be made available according to section K.1.2.4

J: Over 350 automated tests verify compliance with the TCRR rules, reports and program behaviour, and every release is checked for regressions. TCRR changes would result in a new controlled release.

## 6.1-R02 | C.1.1.2

**Requirement** (RFP §6.1)

> Competition-critical functions shall not depend on unavailable internet, licence-server or payment services.

R:

**Justification**

> Once downloaded, the solution is completely autonomous and works with no internet connection whatsoever

J: Once downloaded, the solution is completely autonomous and works without any internet connection.

## 6.1-R03 | C.1.1.3

**Requirement** (RFP §6.1)

> Exact production version, configuration, database schema, rule pack and relevant assets shall be identifiable and reproducible.

R:

**Justification**

> Releases are created by a fully repeatable automated build process, and under strict version control. This applies both to OWLCMS and the Blue Owl components. All past versions and the source code used to build them are available.

J: OWLCMS and Blue Owl releases come from a repeatable automated build under strict version control, and all past versions and their source code are available.

## 6.1-R04 | C.1.1.4

**Requirement** (RFP §6.1)

> Installation and rollback shall be documented and repeatable.

R:

**Justification**

> OWLCMS is launched by a control panel that allows switching between versions. Within a major version, the database can be rolled back if a fix release is unsatisfactory. Fix releases can always read exported prior data so they can be tested independently. Emergency installation of a fix is a matter of seconds once the fix release is downloaded. The OWLCMS program runs on any available Windows, macOS or Linux computer, and the database exports are completely portable, so any available computer can be used for testing.

J: A control panel allows switching between OWLCMS versions, databases can be rolled back within a major version, and fix releases can read prior exports. Emergency fix installation is very quick. Portable exports allow testing on any other computer prior to deployment.

## 6.1-R05 | C.1.1.5

**Requirement** (RFP §6.1)

> Automatic updates shall not occur during an event freeze unless approved.

R:

**Justification**

> The system does not perform any type of automatic update. Operating system updates shall be managed by duly authorized personnel according to supplier procedures; they are not under the control of the solution. Supplier will turn off automated Windows updates for the duration of competition as part of its standard operating procedures.

J: The system never updates automatically, and authorized personnel manage operating system updates according to supplier procedures. Automated Windows updates will be turned off for the duration of a competition.

## 6.2-R01 | C.1.2.1

**Requirement** (RFP §6.2)

> Every sporting fact shall have one declared authoritative owner.

R:

**Justification**

> Any change to the competition state is uniquely attributed to a human actor, to a device input, or to an automated calculation. The audit log reflects this attribution.

J: Every change to the competition state is attributed to a human-operated screen, a device input or an automated calculation, and the audit log records this attribution.

## 6.2-R02 | C.1.2.2

**Requirement** (RFP §6.2)

> Derived screens, feeds, documents and caches shall be traceable to the accepted state.

R:

**Justification**

> All computed results are directly attributable to programmed rules based on the authoritative data, and can be recomputed from said data.

J: All computed results come from programmed rules applied to the authoritative data, and can be recomputed from the data.

## 6.2-R03 | C.1.2.3

**Requirement** (RFP §6.2)

> Corrections shall be authorised, atomic, attributable and propagated.

R:

**Justification**

> Once the competition has started, all changes are meant to be made using the interactive interfaces or the explicit import features of the programs such that audit traces are kept. Database state that does not correspond to audited actions is evidence of tampering. Should intentional exceptional corrective actions by the supplier requiring database queries or similar be necessary, the corresponding authorizations are expected to be managed separately as part of incident response.

J: All changes during competition are made through the program interfaces and audited. Differences from the sealed logs or checksum mismatches on exported data indicate tampering. Any exceptional supplier corrections are authorized separately through incident response.

## 6.2-R04 | C.1.2.4

**Requirement** (RFP §6.2)

> Duplicate, lost, conflicting or out-of-order transactions shall be prevented or detected.

R:

**Justification**

> The system applies strict sequencing for all events, whether device-generated or human-generated. Duplicate events are eliminated.
> Conflicting updates from technical officials are detected and reported (e.g., competition supervisor and marshal both attempting to correct an error). The marshals cannot accidentally interfere with the data updates resulting from a refereeing decision (the athlete card is closed when a decision is received). Athlete registration information cannot be edited while a session is in progress. Likewise, the features that enable post-session athlete card corrections are blocked until the session is over – during a session only the marshal and director pages can edit the athlete cards.

J: All device and human events are strictly sequenced as received for processing, and duplicates are eliminated. Conflicting updates from officials are detected. Technical Official inputs are prevented during decision processing. Changes to an athlete's informational data is blocked when their session has started.

## 6.2-R05 | C.1.2.5

**Requirement** (RFP §6.2)

> A complete machine-readable event export shall be available to IWF.

R:

**Justification**

> The database export contains all the event data in structured JSON format. The audit logs are also machine-readable.

J: The database export contains all event data in structured JSON, and the audit logs are machine-readable.

## 6.3 | G.1

**Requirement** (RFP §6.3)

> Material interfaces shall document producer/consumer, purpose, version, schema/field mapping, identifiers, status/null rules, sequencing/revision, time source, latency, authentication, acknowledgement, retry/reconnect, monitoring and fallback.

R:

**Justification**

> Material interfaces shall document producer/consumer, purpose, version, schema/field mapping, identifiers, status/null rules, sequencing/revision, time source, latency, authentication, acknowledgement, retry/reconnect, monitoring and fallback.

J: Material interfaces shall document their producers and consumers, purpose, version, schema, identifiers, sequencing, timing, authentication, acknowledgement, retry, monitoring and fallback.

## 6.4 | C.2

**Requirement** (RFP §6.4)

> All third-party software, libraries, services, licences and hardware necessary for operation shall be declared, including version, ownership, support status and cloud/public-internet dependency.

R:

**Justification**

> All third-party software, libraries, services, licences and hardware necessary for operation shall be declared, including version, ownership, support status and cloud/public-internet dependency.

J: All dependencies are explicitly stated in the public repositories for the software.

# RFP Section 7

## 7.1-R01 | F.1.1.1

**Requirement** (RFP §7.1)

> Least-privilege access by role and device identity.

R:

**Justification**

> OWLCMS supports separate logins for every role, and where relevant, separate logins for each role and platform. Devices are identified per platform and authenticated using the MQTT identity of the MQTT interface.

J: OWLCMS provides separate logins per role, and per platform where relevant, and devices are identified per platform and authenticated by their MQTT identity.

## 7.1-R02 | F.1.1.2

**Requirement** (RFP §7.1)

> Separation of competition, device, public/display, database and management functions where applicable.

R:

**Justification**

> The role structure separates the technical official roles, preparation roles, the competition supervisor, and admin functions.

J: The role structure does separate the various technical officials, preparation, the competition supervisor, administration and external accesses.

## 7.1-R03 | F.1.1.3

**Requirement** (RFP §7.1)

> Protection of credentials, secrets and data in transit/at rest where required.

R:

**Justification**

> All credentials or secrets are stored encrypted and transported in encrypted form. They are never written to log files in clear. Account passwords and interface connection passwords use PBKDF2 encryption with HMAC-SHA256.

J: Credentials are stored and transported encrypted and never logged in clear. Passwords use PBKDF2 with HMAC-SHA256.

## 7.1-R04 | F.1.1.4

**Requirement** (RFP §7.1)

> Controlled remote support, removable media and patch/deployment processes.

R:

**Justification**

> Supplier will install a remote support program on the competition supervisor laptop and on the interface laptop (AnyDesk). Access to the competition system will be granted explicitly by the competition supervisor.
> IWF will provide removable media to the competition supervisor, and IWF will keep the files copied on that device.
> OWLCMS and Blue Owl are distributed as installation packages created using automated, repeatable processes. Both packages follow strict version control rules.
> So as a rule, when a fix is required, an installation package is made available for the fixed version. The fixed package is installed alongside the old version, and data is imported (the OWLCMS launch process supports this). If the test is conclusive, the new version continues to be used; otherwise, the prior version is used again.
> There are situations where a fix is extremely simple (adjusting a configuration file or a template). In such cases, the corrective fix may be simply applied in the correct location. But the supplier will generally avoid such processes.

J: AnyDesk remote support on the supervisor and interface laptops requires the competition supervisor's explicit approval. IWF retains the removable media. Fixes are delivered as versioned packages installed alongside the old version. Data is imported and rollback are performed if needed.

## 7.1-R05 | F.1.1.5

**Requirement** (RFP §7.1)

> Tamper-resistant audit sufficient to reconstruct material actions.

R:

**Justification**

> The audit logs include every single modification to the athlete and lifting data, and all events coming from devices (decisions, etc.). All export actions are logged with the checksum of the exported file, for later integrity checks of exported files.
> The audit log is a chain of cryptographically signed blocks of log lines. Each block is signed by a private key. Each block's signature also includes the previous block's signature, creating a chain.
> Prior to each competition, a cryptographic public-private key pair is generated, along with a public-key fingerprint. The technical delegate or other trusted person takes a picture of the fingerprint. Logs are copied away from the competition computer to a safe location periodically during the meet.
> In case of a dispute, the safe logs are copied to a clean machine, and a clean OWLCMS is installed. Each log contains the public key that matches the (undisclosed) private key used to do the signatures. The checker can confirm that each block was signed by the matching private key and that each block was not modified. The technical delegate confirms the public key’s authenticity by comparing its fingerprint with what they photographed.

J: The audit log records all athlete and lifting changes, all device events and checksums of exports. Blocks of lines are signed, and chained to capture the sequence. A per-competition key fingerprint, photographed by the technical delegate, and periodic off-machine copies allow independent verification on a clean installation.

## 7.1-R06 | F.1.1.6

**Requirement** (RFP §7.1)

> No public or anonymous path may write authoritative competition state.

R:

**Justification**

> The only writing to the database is done by OWLCMS itself, which owns an exclusive file lock. The database is therefore the source of truth. Exports are cryptographically checksummed, and the checksum is logged in the audits to allow later verification of integrity.

J: Only OWLCMS writes to the database, through an exclusive file lock, and all updates are audited.

## 7.2 | F.1.2

**Requirement** (RFP §7.2)

> Core competition, officiating, timing, local displays and local official documents shall remain usable during loss of public internet or cloud connectivity.

R:

**Justification**

> The normal mode of operation of OWLCMS in a major competition is completely offline, on a private local area network as shown in Figure 1. Operator stations are normally wired using Ethernet cabling to avoid WiFi interference or foul play.

J: OWLCMS runs fully offline on a private switched LAN, with operator stations wired over Ethernet.

## 7.3-R01 | F.1.3.1

**Requirement** (RFP §7.3)

> Protected backup copies of data, configuration, rules, assets and audit.

R:

**Justification**

> All files that constitute the OWLCMS configuration and rules can be backed up as a single zipped folder. This zip is copied remotely by the competition supervisor. This
> The cryptographic key is preserved separately, as a matter of principle.
> While OWLCMS is running, it uses the JSON export format for snapshots, which can be produced on demand. Copying the actual binary database is possible when OWLCMS is stopped.

J: The configuration and rules are backed up as a single zipped folder. The archive is copied to a separate location by the competition supervisor, and the cryptographic key is kept separately. On-demand JSON exports provide consistent snapshots while OWLCMS is running, and the binary database can be copied when it is stopped.

## 7.3-R02 | F.1.3.2

**Requirement** (RFP §7.3)

> Timed restoration demonstrating the approved recovery objective.

R:

**Justification**

> Starting OWLCMS from scratch and loading an export takes less than a minute.

J: Restoring a saved state from scratch normally takes less than a minute.

## 7.3-R03 | F.1.3.3

**Requirement** (RFP §7.3)

> Defined promotion, split-brain prevention, failback and reconciliation where redundancy is claimed.

R:

**Justification**

> The system does not operate in active-active mode. Checkpoint copies are used.
> Checkpoint copies can be automated to a defined Recovery Point Objective (RPO). This would be used in the event of a complete loss of the main computer. In such a case, the competition would proceed manually using flags and cards. Assuming checkpoints are taken every 5 minutes, restarting a waiting spare from the checkpoint takes less than a minute. Given that lifts take roughly 1:20 seconds on average, recovery would require re-entering something like 4 or 5 lifts manually before competition could resume in automated mode; in normal circumstances the displays would automatically refresh to the new master.

J: The system does not run in tandem and uses checkpoint copies instead. A waiting backup laptop can reload a checkpoint copy very quicky and manual re-entry of a few lifts can bring the system back in a matter of minutes.

## 7.3-R04 | F.1.3.4

**Requirement** (RFP §7.3)

> Cold-start and rollback procedures using controlled recovery media.

R:

**Justification**

> Checkpoint copies of the export format can be obtained on demand and constitute full rollback points. The system can restart from a checkpoint.

J: On-demand export checkpoints are full rollback points. The system can restart from a checkpoint.

## 7.4 | F.1.4

**Requirement** (RFP §7.4)

> A manual Protocol and rehearsed manual-continuity procedure shall remain available. Staff shall know when to continue, when to STOP and how to reconcile manual records with the electronic state.

R:

**Justification**

> A manual Protocol and rehearsed manual-continuity procedure shall remain available. Staff shall know when to continue, when to STOP and how to reconcile manual records with the electronic state.
> Manual mode is a standard operational procedure that all ITOs understand. A power outage, a networking mishap, or a physical hazard like water damage from a leaky roof can prevent use of the system and cause a revert to a “manual cards and flags” mode.
> OWLCMS has been tested in such situations before. As soon as the system is restored, someone can simply enter the information as it stands on the cards, even while lifting is going on that platform. As soon as the information matches the paper trail, the screens can be turned on for that platform. OWLCMS always derives lifting order from the athlete card information.
> The supplier will have explicit operational procedures and ITOs or very senior NTOs on staff.

J: Falling back to manual cards and flags is standard procedure and has been tested with OWLCMS. Card data is re-entered after restoration even while lifting continues, the screens resume once data matches the paper trail.

## 7.5 | F.1.5

**Requirement** (RFP §7.5)

> Actionable monitoring shall cover critical services, devices, timing, storage, network and stale-state conditions, with clear escalation responsibilities.

R:

**Justification**

> Actionable monitoring shall cover critical services, devices, timing, storage, network and stale-state conditions, with clear escalation responsibilities.
> The main server is under direct observation by the competition supervisor and the health log plus the normal OS indicators can be used for monitoring. In Games settings where critical interfaces are used, there will likewise be supplier staff directly monitoring the interfaces and the Interface laptop. Given that all operator stations are within sight, that they only use browsers and that they have been observed to run the OWLCMS screens for weeks without restarting, they are not critical devices in a sense that warrants dedicated monitoring sensors.
> The refereeing devices and jury devices can be observed by the competition supervisor. In actual practice, the only failure mode is a disconnected USB connector or RJ-45 button. A failure of the Blue Owl relay itself is reported in the technical log and in the health log – it will reconnect immediately if connectivity is restored.
> In actual practice over hundreds of competitions the most common failure modes are TV connectors being loose, TVs going to sleep, referee buttons being disconnected. The only actionable monitoring for those is human, and that is part of what Technical Controllers do.

J: The critical devices are directly used by the competition supervisor and the operators of the critical interfaces. The competition supervisor can directly monitor the refereeing devices. For other devices the failure modes are not preventable by monitoring (disconnected cables) and directly observable.

# RFP Section 8

## 8.1-R01 | A.1.1.1

**Requirement** (RFP §8.1)

> Legally established entity able to contract with IWF.

R:

**Justification**

> This proposal is put forward by [Supplier], a duly incorporated corporation under [Country Law]

J: The proposal is submitted by [Supplier], a corporation duly incorporated under [Country Law].

## 8.1-R02 | A.1.1.2

**Requirement** (RFP §8.1)

> Relevant experience delivering competition scoring/results or comparable mission-critical systems.

R:

**Justification**

> [Supplier] has been providing similar services to world-class events such as the Asian Games, [To be completed]
> [Supplier} provided level M3 (Full on-venue results and full integration with Games central and broadcast-quality TV graphics) for Islamic Solidarity Games in 2025.
> [Supplier] has explicit support from the original developer of the OWLCMS software and can leverage a network of extremely experienced competition supervisors with direct experience using the software. These people have managed competitions across 3 to 6 simultaneous platforms, involving up to 1600 athletes and lasting 3 to 9 days.

J: [Supplier] has served events such as the Asian Games and provided level M3 for the 2025 Islamic Solidarity Games. [Supplier] explicit support from the original OWLCMS developer and access to experienced competition supervisors who have managed competitions of 3 to 6 platforms, up to 1600 athletes and 3 to 9 days.

## 8.1-R03 | A.1.1.3

**Requirement** (RFP §8.1)

> Ability to accept end-to-end responsibility for the complete system and declared third-party components.

R:

**Justification**

> [Supplier] has been accepting such responsibility for major events. [Supplier] can fully maintain and fix the declared components.

J: [Supplier] has accepted such responsibility for major events and can fully maintain and fix the declared components.

## 8.1-R04 | A.1.1.4

**Requirement** (RFP §8.1)

> Qualified technical, competition-operations and support personnel.

R:

**Justification**

> [Supplier] has retainers with technology specialists to install and operate the actual TIS for its very large events, and a large network of people experienced with the system, the devices, and the processes it can draw upon.

J: [Supplier] retains technology specialists to install and operate the TIS at very large events and can draw on a large network of people experienced with the system, devices and processes.

## 8.1-R05 | A.1.1.5

**Requirement** (RFP §8.1)

> International training, event support and incident-response capability.

R:

**Justification**

> [Supplier] is able to provide trainers that would do international training. [Supplier] already monitors its own events at distance with remote intervention capabilities. [Supplier] can create teams of people to be dispatched internationally, as it currently does with technical officials.

J: [Supplier] can provide international trainers, already monitors its events remotely with intervention capabilities, and can dispatch teams internationally as it does with technical officials.

## 8.1-R06 | A.1.1.6

**Requirement** (RFP §8.1)

> Ability to maintain the approved baseline for the license period.

R:

**Justification**

> [Supplier] already maintains similar baselines for its established activities.

J: [Supplier] already maintains similar baselines for its established activities.

## 8.2-R01 | H.1.1

**Requirement** (RFP §8.2)

> Provide accurate product and hardware baseline.

R:

**Justification**

> [To be completed]

J: [To be completed]

## 8.2-R02 | H.1.2

**Requirement** (RFP §8.2)

> Provide complete technical submission and evidence package.

R:

**Justification**

> A test environment can be provided to reproduce any claimed feature.

J: A test environment can be provided to reproduce any claimed feature.

## 8.2-R03 | H.1.3

**Requirement** (RFP §8.2)

> Make the declared system available for IWF testing and demonstrations.

R:

**Justification**

> A test environment can be provided if supplier is selected.

J: A test environment can be provided if the supplier is selected.

## 8.2-R04 | H.1.4

**Requirement** (RFP §8.2)

> Correct deficiencies and complete regression testing when required.

R:

**Justification**

> Agreed if supplier is selected.

J: Agreed if the supplier is selected.

## 8.2-R05 | H.1.5

**Requirement** (RFP §8.2)

> Notify IWF of material product, security, hardware, interface or rule-logic changes.

R:

**Justification**

> Agreed.

J: Agreed.

## 8.2-R06 | H.1.6

**Requirement** (RFP §8.2)

> Maintain qualified support personnel and appropriate spares.

R:

**Justification**

> Agreed.

J: Agreed.

## 8.2-R07 | H.1.7

**Requirement** (RFP §8.2)

> Protect IWF, athlete and event information and comply with applicable data-protection requirements.

R:

**Justification**

> The only information that would normally be considered Personal Data under GDPR and similar frameworks is the athlete’s body weight and birth date. The body weight being necessary to the application of rules, it can be construed to fall under the “legitimate interests” clause (and disclosure should be mentioned as part of any consent and risk acceptance form signed by athletes).
> Concerning birth date, IWF’s own rules do not require the full birth date to be published, only the birth year. OWLCMS has a setting to only use and show the birth year; we would recommend that this be used in IWF competitions.
> The rest of the athlete information and event information is meant to be made public as it is part of public event invitations and included in preliminary participation lists, start books and result books.
> In an IWF event, it is expected that an IWF identifier of the athlete will be included in the registration data. OWLCMS treats that identifier as an opaque anonymized foreign identifier.

J: Only body weight and birth date are personal data; body weight falls under legitimate interests, and OWLCMS can show only the birth year as IWF rules require. Other athlete and event data is public, and the IWF athlete identifier is treated as an opaque anonymized foreign identifier.

# RFP Section 9

## 9.1-R01 | K.3.1

**Requirement** (RFP §9.1)

> Acceptance of the IWF TCRR as the controlling sporting rules.

R:

**Justification**

> Agreed.

J: Agreed.

## 9.1-R02 | K.3.2

**Requirement** (RFP §9.1)

> Identification of all material limitations and dependencies.

R:

**Justification**

> None identified. To be confirmed during acceptance testing.

J: No material limitations or dependencies have been identified, subject to confirmation during acceptance testing.

## 9.1-R03 | K.3.3

**Requirement** (RFP §9.1)

> Ability to reproduce the tested system from controlled installation material.

R:

**Justification**

> Installation from a compliant kit shall reproduce the tested behaviour.

J: Installation from a compliant kit can be used to reproduce all claimed behaviour.

## 9.1-R04 | K.3.4

**Requirement** (RFP §9.1)

> Offline operation and tested recovery for the claimed profile.

R:

**Justification**

> The system is confirmed to operate without on-line access, and to be recoverable from the described exports.

J: The system operates without online access and can be recovered from the described exports.

## 9.1-R05 | K.3.5

**Requirement** (RFP §9.1)

> No automatic licensing mechanism that can interrupt an active competition.

R:

**Justification**

> There are no such automatic licensing mechanisms.

J: There are no automatic licensing mechanisms.

# RFP Section 10

## 10-S01 | I.1.1

**Requirement** (RFP §10)

> Completeness, eligibility and declarations.

R:

**Justification**

> Supplier declares the current information to be correct at the date of submission.

J: The supplier declares the information to be exact at the date of submission.

## 10-S02 | I.1.2

**Requirement** (RFP §10)

> Architecture, functionality, hardware, interfaces, security and resilience.

R:

**Justification**

> To be performed by IWF selection committee.

J: [Supplier] states that the architecture as described meeds the claims made for functionaity, security and resilience.

## 10-S03 | I.1.3

**Requirement** (RFP §10)

> Freeze baseline, profile, capacity, dependencies and test plan.

R:

**Justification**

> All claimed capabilities are present in the proposed baseline, in compliance with TCRR as available October 1, 2026.
> Baseline will managed according to section K.1.2. Note that the baseline may need to evolve to reflect software modifications due to changes in IWF rules or formal adoption of experimental practices (e.g., display of jury decisions on scoreboards). The baseline may also evolve due to incident resolution or significant supplier upgrades, such as CVE security incident resolutions.

J: The baseline will be managed in compliance with K.1.2, including the dependencies and associated tests.

## 10-S04 | I.1.4

**Requirement** (RFP §10)

> Controlled normal, edge, security, capacity and failure testing.

R:

**Justification**

> Initial acceptance tests to be formalized if supplier is selected. All capabilities claimed can be demonstrated.

J: Acceptance tests will be formalized if the supplier is selected. claimed capabilities can be demonstrated.

## 10-S05 | I.1.5

**Requirement** (RFP §10)

> Resolve deficiencies and provide regression evidence.

R:

**Justification**

> Any issue declared will be investigated, and updated official builds will be provided. OWLCMS includes over 350 automated tests to prevent regressions; such tests are run before any release.

J: Declared issues will be investigated and fixed in updated official builds. Over 350 automated OWLCMS tests are run before every release to prevent regressions.

## 10-S06 | I.1.6

**Requirement** (RFP §10)

> Representative event tempo with operators, officials, displays, feeds and recovery.

R:

**Justification**

> OWLCMS includes an automated stress simulation test that allows all required displays to be observed on all platforms as desired. All device interfaces are exercised, and all external feeds are fed.
> The stress test proceeds as fast as could possibly be done in real life: a full lift cycle takes about 12 seconds from one decision to the next, including a real clock start and the decision reversal delays. Simulated attempted weights are randomly modified using the audited marshal events (simulating a plausible rate of such changes). Referee decisions are generated using the actual audited referee events as they would be generated by devices, to create a plausible rate of successive attempts.
> All the displays, devices, and operator stations, for all platforms, can be opened at once and observed.
> It is of course possible to perform the same test manually with human operators.

J: An automated OWLCMS stress test runs accelerated 12-second lift cycles on all platforms and uses audited marshal and referee events to exercise every device, display and external feed.

## 10-S07 | I.1.7

**Requirement** (RFP §10)

> Review requirements, hard gates, evidence and findings.

R:

**Justification**

> To be made by IWF selection committee.

J: This is to be performed by the IWF selection committee.

## 10-S08 | I.1.8

**Requirement** (RFP §10)

> Complete legal and operational conditions.

R:

**Justification**

> To be defined once supplier is selected. [Supplier] has entered into such agreements with several international federations.

J: These conditions will be defined after initial selection.

## 10-S09 | I.1.9

**Requirement** (RFP §10)

> Check actual installation against approved baseline.

R:

**Justification**

> Site testing to be defined by IWF acceptance committee.

J: This is to be performed by the IWF acceptance committee.

## 10-S10 | I.1.10

**Requirement** (RFP §10)

> Review incidents, changes, support and expiry.

R:

**Justification**

> IWF oversight mechanisms are to be defined. The supplier has established processes for incident management.

J: IWF oversight mechanisms are to be defined. The supplier has established processes for incident management.

## 10.1 | I.1.11

**Requirement** (RFP §10.1)

> Evidence shall be attributable to a specific baseline and test. Screenshots alone are insufficient where logs, physical inspection or live behaviour are necessary.

R:

**Justification**

> Supplier will capture exports and logs as required during testing.

J: The supplier will capture exports and logs as required during testing.

# RFP Section 11

## HG-01 | I.2.1

**Requirement** (RFP §11)

> Incorrect sporting fact, rank, medal, record or rule-critical transition.

R:

**Justification**

> No rule-critical deviation from TCRR rules has been reported in production.

J: No such error is currently reported in actual production use, over hundreds of events.

## HG-02 | I.2.2

**Requirement** (RFP §11)

> Split, duplicated, lost or silently overwritten accepted state.

R:

**Justification**

> Silent overwrites are prevented; all state transitions are processed and update the database as transactions.

J: Silent overwrites are prevented; all state transitions are processed and update the database as transactions.

## HG-03 | I.2.3

**Requirement** (RFP §11)

> Unsafe or ambiguous timing, decision, Down signal or restart.

R:

**Justification**

> Timing and decision events are processed as mandated by TCRR. For athlete safety, the system will issue a down signal and record a decision even if the timekeeper fails to restart the clock; the jury will decide whether to accept the lift.

J: Timing and decision events are processed as mandated by TCRR.

## HG-04 | I.2.4

**Requirement** (RFP §11)

> Unauthorised, partial, untraceable or unreconciled correction.

R:

**Justification**

> All data modifications are transactional. The system includes an audit trail of all data modifications, and all access is role-based.

J: All data modifications are transactional, recorded in an audit trail, and all access is role-based.

## HG-05 | I.2.5

**Requirement** (RFP §11)

> Uncontrolled loss of competition or unverified recovery.

R:

**Justification**

> Actual production occurrences of loss of power or loss of networking do occur regularly in some countries, and OWLCMS has been used in competitions where they occurred. The system has never been observed to lose data. In IWF-scale events, it is always run on battery-protected laptops and/or on a UPS to prevent power spikes. Even if a laptop running on battery suddenly runs out of power, the database's transactional nature prevents data loss, in addition to itelf running on a journaling file system.
> Recovery is achieved as soon as the system has been updated to catch up with the handwritten athlete cards (or even the manual protocol). A platform can be on outage (local power loss to its networking switch) while others are working, catching up for that platform will allow it to recover, the other platforms are not affected.
> As an additional precaution against human error, the competition supervisor would normally trigger a database export after weigh-ins and at each break on each platform. This can also be automated to take place at fixed intervals.

J: OWLCMS has never been observed to lose data as it uses a transactional database and is normally run on laptops that are power-protected and use journaling file systems (such as NTFS) . A platform recovers by catching up from the handwritten athlete cards without affecting other platforms, and database exports can be taken manually or automatically at intervals.

## HG-06 | I.2.6

**Requirement** (RFP §11)

> Uncontrolled privilege, public write path or material compromise.

R:

**Justification**

> The system as proposed to IWF is fully role- and least-privilege-based. The database is in exclusive write mode from the OWLCMS system, and all competition modifications done by the application are logged.

J: The proposed system is fully role-based and least-privilege, only OWLCMS can write to the database, and all competition modifications are logged.

## HG-07 | I.2.7

**Requirement** (RFP §11)

> Contradictory outputs or untraceable final publication.

R:

**Justification**

> No such incident reported in production

J: No such incident has been reported in production.

## HG-08 | I.2.8

**Requirement** (RFP §11)

> Unknown tested version, missing evidence or irreproducible installation.

R:

**Justification**

> All builds are versioned and created from a repeatable procedure that is public. The actual binary files contain version information.

J: All builds are versioned and produced by a public, repeatable procedure, and the binaries contain version information.

## HG-09 | I.2.9

**Requirement** (RFP §11)

> Missing critical equipment, unsafe installation or unresolved obstruction.

R:

**Justification**

> This clause is event-specific, to be completed by the technical delegate of the event.

J: This clause is event-specific and is to be completed by the event's technical delegate.

## 11.1-R01 | I.2.10.1

**Requirement** (RFP §11.1)

> All applicable mandatory requirements and tests must be disposed of.

R:

**Justification**

> To be completed by IWF evaluation committee.

J: To be completed by IWF evaluation committee.

## 11.1-R02 | I.2.10.2

**Requirement** (RFP §11.1)

> Applicable TCRR requirements and hard gates must pass.

R:

**Justification**

> To be completed by IWF evaluation committee.

J: To be completed by IWF evaluation committee

## 11.1-R03 | I.2.10.3

**Requirement** (RFP §11.1)

> No open Critical finding may remain.

R:

**Justification**

> To be completed by IWF evaluation committee.

J: To be completed by IWF evaluation committee.

## 11.1-R04 | I.2.10.4

**Requirement** (RFP §11.1)

> Traceability from requirement to evidence and test must be complete.

R:

**Justification**

> To be completed by IWF evaluation committee.

J: To be completed by IWF evaluation committee.

## 11.1-R05 | I.2.10.5

**Requirement** (RFP §11.1)

> Successful FAT, recovery and operational validation are required for the claimed profile.

R:

**Justification**

> To be completed by IWF evaluation committee.

J: To be completed by IWF evaluation committee.

## 11.1-R06 | I.2.10.6

**Requirement** (RFP §11.1)

> Partial is not Pass.

R:

**Justification**

> To be completed by IWF evaluation committee.

J: To be completed by IWF evaluation committee.

## 11.1-R07 | I.2.10.7

**Requirement** (RFP §11.1)

> A profile limitation cannot change an IWF sport rule or bypass a hard gate.

R:

**Justification**

> To be completed by IWF evaluation committee.

J: To be completed by IWF evaluation committee.

# RFP Section 12

## 12.1 | K.1.1

**Requirement** (RFP §12.1)

> The IWF licence shall be valid for a four-year period. The certificate shall state supplier, product/baseline, profiles, capacities, limitations, deviations, validity and surveillance conditions.

R:

**Justification**

> The IWF licence shall be valid for a four-year period. The final contract shall state the exact licence duration and renewal conditions.

J: The IWF licence is valid for four years, and the final contract will state the exact duration and renewal conditions.

## 12.2-R01 | K.1.2.1

**Requirement** (RFP §12.2)

> Controlled update and review.

R:

**Justification**

> Will be treated the same as minor cosmetic

J: These changes will be treated the same as minor cosmetic changes.

## 12.2-R02 | K.1.2.2

**Requirement** (RFP §12.2)

> Supplier assessment and IWF notification where applicable.

R:

**Justification**

> These are typically typos or simple modification to the layout (spacing, etc.). To be considered minor, operational procedures would not need to be modified.

J: These are typos or simple layout changes that do not require any change to operational procedures.

## 12.2-R03 | K.1.2.3

**Requirement** (RFP §12.2)

> Impact assessment and approved regression/delta test.

R:

**Justification**

> Changes in this category will be treated in one of several categories.
> Technical changes (OS/firmware/security/hardware). These are often unavoidable and required by suppliers. A new release will be made available and should be considered as required.
> Data Interface changes – User interface changes, workflow changes, changes or additions to data sent over interfaces. Because the dependency is to an external entity (for example, Games central services) the update will likely need to be considered as required.
> Functional changes – User interface changes, workflow changes other than TCRR requirements. IWF may elect to remain on a prior release.

J: Technical changes (OS, firmware, security, hardware), data interface changes are normally mandatory releases. Functional changes that are not TCRR-related may be considered optional.

## 12.2-R04 | K.1.2.4

**Requirement** (RFP §12.2)

> Re-homologation before event use.

R:

**Justification**

> Evolution of TCRR rules or other IWF requests; corrections to such. These will be considered mandatory upgrades

J: Full assessment of compliance with the rule changes.

## 12.2-R05 | K.1.2.5

**Requirement** (RFP §12.2)

> Named approval, backup, rollback, targeted test and retrospective evidence.

R:

**Justification**

> It is expected that competition staff will be able to obtain approval as part of the IWF approval chain (e.g. by an on-site technical delegate). As per industry best practices, any emergency remediation will be considered as temporary, and a proper corrective fix will be made available after root cause analysis.

J: Competition staff are expected to obtain approval through the IWF chain, such as an on-site technical delegate. Every emergency remediation will be investigated and followed by a proper corrective fix subject to present section K.

## 12.3-R01 | H.2.1

**Requirement** (RFP §12.3)

> Only the approved baseline and event configuration may be used.

R:

**Justification**

> Agreed.

J: Agreed.

## 12.3-R02 | H.2.2

**Requirement** (RFP §12.3)

> Actual hardware, software, network, power, displays, spares and staffing must match the approved event design.

R:

**Justification**

> Agreed.

J: Agreed.

## 12.3-R03 | H.2.3

**Requirement** (RFP §12.3)

> Dress rehearsal and failure/recovery test shall be completed before readiness sign-off.

R:

**Justification**

> Agreed.

J: Agreed.

## 12.3-R04 | H.2.4

**Requirement** (RFP §12.3)

> Substitutions require approved deviation or delta testing before use.

R:

**Justification**

> Agreed. Note that operator stations used by technical officials are only actually dependent on the capability to run a current mainstream browser and on the presence of HDMI ports and USB ports. Any Windows laptop with these features can be substituted in an emergency.

J: Agreed. In an emergency, any Windows laptop with a current mainstream browser and HDMI and USB ports can replace a technical officials' operator station.

## 12.4-R01 | H.3.1

**Requirement** (RFP §12.4)

> Defined escalation contacts and event support.

R:

**Justification**

> Competition staff will be provided with that information. Escalation staff will either be directly on site or available remotely.

J: Competition staff will have access to the escalation contacts.

## 12.4-R02 | H.3.2

**Requirement** (RFP §12.4)

> Critical incidents documented, contained, investigated and reported.

R:

**Justification**

> Supplier will provide an issue reporting environment and ensure follow-up and resolution of incidents.

J: The supplier will provide an issue reporting environment and ensure that incidents are followed up and resolved.

## 12.4-R03 | H.3.3

**Requirement** (RFP §12.4)

> Serious incidents, unauthorised changes, false evidence, unsupported baselines or material security compromise may lead to restriction, suspension or withdrawal.

R:

**Justification**

> Agreed.

J: Agreed.

## 12.5 | K.2

**Requirement** (RFP §12.5)

> IWF shall receive the rights, documentation, exports, interfaces and operational access necessary to use, verify, archive and maintain the approved system and competition records.

R:

**Justification**

> The supplier retains its intellectual property except where otherwise agreed. IWF shall receive the rights, documentation, exports, interfaces and operational access necessary to use, verify, archive the approved system and competition records.
> The responsibility and possibility to maintain the software, computers and devices lies with the supplier.

J: The supplier keeps its intellectual property unless otherwise agreed. IWF receives the required rights to use the system and keep its records Maintenance of the software and equipment is the supplier's responsibility.s

# RFP Section 13

## 13-R01 | J.1.1

**Requirement** (RFP §13)

> The Licensing Fee for the four-year period is US$20,000, payable on an annual basis of US$5,000 per year.

R:

**Justification**

> US$5,000 per year / US$20,000 total

J: The fee is US$5,000 per year, for a total of US$20,000.

## 13-R02 | J.1.2

**Requirement** (RFP §13)

> Licence

R:

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]

## 13-R03 | J.1.3

**Requirement** (RFP §13)

> Implementation

R:

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]

## 13-R04 | J.1.4

**Requirement** (RFP §13)

> Equipment

R:

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]

## 13-R05 | J.1.5

**Requirement** (RFP §13)

> Support / maintenance

R:

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]

## 13-R06 | J.1.6

**Requirement** (RFP §13)

> Optional services

R:

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]

## 13-R07 | J.1.7

**Requirement** (RFP §13)

> Recurring charges

R:

**Justification**

> [TO BE COMPLETED]

J: [TO BE COMPLETED]
