<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**Maintenance Log**

- 68.0.4: Fixed decisions being ignored after reloading the session while a decision was shown: the platform stayed stuck and refused to start the clock until the session was unloaded.
  - Reloading the session while the clock is running or a decision is in progress now asks for confirmation (Cancel is the default). Confirming stops the clock, clears the decision, and lifting must be restarted.
  - Editing a platform no longer reloads its current session

- 68.0.4: Updated the age groups files included in the distribution to use the 2026 bodyweight categories
  - Official source for RU
  - Computed mapping for DE, FFH and FHQ. For categories lower than U17, check with federation values before using.
  - FHQ: U13 is now U12 (ages 11–12), qualifying totals were removed, and age-only masters groups A30 to A85 (single 999 category, QMasters scoring) were added.

- 68.0.4: Fixed selecting a session immediately after unselecting one, which was sometimes ignored.

- 68.0.3: Fixed scoreboards when the session is unselected during a break or the introduction of athletes: the scoreboard now shows the waiting screen instead of the previous session, or "undefined" when the scoreboard was reloaded.

- 68.0.2: Updated the Spain age group template.

- 68.0.2: Fixed unnecessary delays when starting the next clock after a decision:
  - the extra wait for jury votes now applies only when jury decisions on attempt boards are enabled and the jury size is above zero.
  - New competitions default to no jury (size 0); existing competitions keep their saved jury size.

- 68.0.2: The bottom decision section can now be shown or hidden for each scoreboard. It is shown by default on public and video scoreboards, and hidden on warm-up scoreboards unless enabled by the feature switch.

- 68.0.2: Jury decisions on attempt boards can be enabled by default with the `juryDecisionsOnAttemptBoard` feature switch. The actual option is only offered when the jury size is above zero.

- 68.0.1: Fixed the medal ceremony dialogue: starting a single category ceremony (e.g. "45" under P15 F) from the Medal Ceremony page would show an empty medals page.

**New in Release 68.0**

- 68.0.0: Migration to Vaadin 25 user interface toolkit
  - Change to use new CSS theming mechanism (the selectors have changed slightly)
  - Systematic cleanup of CSS files to use import+overrides instead of copies
  - **If you have customized your CSS files, you will need to review and potentially adjust them**.

- 68.0.0: Championship editing reorganized under the "Championships, Medals and Teams" tab and the Define Championships page.

  - Each championship row has "Edit Medals and Team Rules", "Select Age Groups", "Reset to Template" and "Delete".
  - "Define Age Groups and Categories" and "Define Championship Template" buttons are next to the championship list; championships in use are listed first.
  - New "Select Age Groups" dialog moves age groups into a championship, after a confirmation that names the championship each one leaves.
  - Deleting a championship deletes its age groups, after a confirmation listing them.

  -  Athletes are kept and moved to their most specific remaining category; previously, deleting age groups that had registered athletes failed.

- 68.0.0: The Scoring Systems selection was removed from the competition settings: all scores are always computed, and best athlete scoring system is now championship-per-championship

  - The best athlete scoring selector on the results pages lists the championships' schemes first, then the GAMX, QPoints, Robi and Sinclair families.

- 68.0.0: The Medals board now matches the medals sheet and only shows finished categories. A category split across sessions (e.g. 69A and 69B) is not shown until all its sessions are finished.

  - To see prospective medalists, tick "Include not completed" before opening the Medals board (warmup, public and video displays).

- 68.0.0: Reinstated the `displayBodyWeight` feature toggle as a first class citizen (no longer shares the custom1 column)

- 68.0.0: Finish the transition to championships as source of truth for best athlete.

  - There is one best athlete scheme per championship.
  - For a given championship the mBest and wBest categories are computed.  The old globally computed collections have been dropped.
  - It is still possible to switch the best athlete scheme when producing reports for comparison purposes -- this does not change the ranking scheme officially defined for the championship
  - Templates can get all the individual scores e.g.
  - The current best athlete score and rank for a given athlete are `${a.bestAthleteScore}` and `${bestAthleteRank}`
  - A template can still get any computed score e.g.,`${a.gamx}` independently of the current best athlete score.  But only the currently selected scheme rank.

- 68.0.0: The `alwaysUseLiftingOrderTieBreaks` feature switch lets concurrent A/B/C sessions use category-wide lifting-order tie breaks. For equal totals, session and lift times are ignored; lot number replaces start number as the final tie breaker.

- 68.0.0: IMWA team results exclude athletes who did not weigh in from category athlete counts.

- 68.0.0: Record definition files with more than five distinct record names per federation are now rejected.

  - Typical errror is when an AI agent generates the file from a database and puts a different record name on each row

- 68.0.0: The Jury Control page now allows the president to indicate which IWF rejection code was invoked.  When this is done, the speaker gets the reason immediately.

- 68.0.0: Added "post weigh-in athlete-card" templates that print recorded declarations and changes, with large red start numbers for emergency replacement of lost/misplaced cards.

- 68.0.0: Weigh-in forms 

  - now include a separate weight-on-scale column before body weight so both values can be recorded when calculating the adjustment by hand.

- 68.0.0: IMWA athletes below the category qualifying total keep their rank and team points but do not receive a Total medal.

- 68.0.0: Jury decisions are optionally shown on attempt board, selected on the attempt board dialog.

- 68.0.0: Jury decisions can be shown on the attempt board (off by default).  
  - This relies on the jury size being set in competition options, and on being selected on the attempt board (URL parameter showJuryDecisions=true)
  - If you change the jury size, reload the session (only the reloaded sessions will pick up the new size)
- 68.0.0: Decision reversal from the announcer/marshal screen, accessed by clicking on the cell for the attempt
- 68.8.0: Dedicated page for medal ceremonies on the "run lifting" page; this causes public scoreboards to display the medals
  - The page estimates what categories fit on the page to make it easier to go through the presentations
- 68.8.0: Public scoreboards switch to the "start list" page until the snatch countdown is started, and to medals when ceremonies take place
- 68.8.0: Championships management
  - Championships can be re-ordered
  - Medal presentations and other such documents will follow the championship ordering.
  - Empty championships are hidden from championship dropdown filters by default
  - Medals can be awarded for the 3 events, for the total, or only for the two lifts. 
  - Team points can be awarded for total only, even if the championship awards the 3 medals.
  - Deletion of a championship also deletes the associated age groups (after confirmation)
- 68.0.0: Event forwarding destinations can now be added, removed, and activated individually; local Tracker connections managed by the Control Panel is treated specially.
- 68.0.0: Reorganized display launchers into tabs, with consistent warm-up and public display choices and Video Streaming available under Displays.
- 68.0.0: The default paper size can be selected (a default is picked based on presumed location).  This hides the irrelevant templates.
- 68.0.0: Added mobile navigation pages for phones and tablets
  - When on mobile, the home page redirects to a refereeing page
  - Links for jury and scoreboard pages are added as appropriate for mobile devices
- 68.0.0: Usability: Changed the wording for the CJ break duration settings to be inclusive (6 or fewer, 10 or more)
- 68.0.0: Cleanup and standardization of template headers for protocols, jury protocols, and competition results
- 68.0.0: Technical official spreadsheet imports now report invalid team-role values in the upload dialogue.
- 68.0.0: SBDE imports matching for existing athletes is now case-insensitive and diacritic-insensitive.
- 68.0.0: Normalization of names to Olympic Data Format guidelines, unless `dontFixNames` feature toggle is on.
- 68.0.0: Mixed teams, by default, combine the men's and women's teams (so up to 16 athletes in IWF settings)
- 68.0.0: Updated the "Top X" displays to use the correct championship-configured ranking system.
- 68.0.0: Interactive editing of IWF-style technical official team assignment table
- 68.0.0: owlcms.local published as a local network mDNS host name alias so that http://owlcms.local reaches the OWLCMS server
- 68.0.0: 15kg bar with no plates was erroneously preferred to 5kg or 10kg bar + kid bumpers
  - 5kg bar never gets collars (they are rated to 20kg)
- 68.0.0: Migrate birth dates to directly readable canonical ISO8601 textual format to avoid interpretation, time zone, and conversion issues.
- 68.0.0: Dark mode/Light mode toggle in the main menu side bar.



For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
