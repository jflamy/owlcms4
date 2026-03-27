<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 66**

66.0.0: Mixed Team Championships
  - Allow explicit nomination of a mixed team, or implicit men and women combined
  - A gender-equitable scoring system (e.g. GAMX) can be applied, independently of the gendered teams scoring
  
66.0.0: Enhanced definition of Championships
  - Championships now define all the awards
    - best athlete scoring systems
    - medaling rules (total or per event), medaling on score or lifted weight
    - points awarded
    - scoring systems for gendered teams and for mixed teams

66.0.0: Mobile device versions for jury member devices and jury president keypad
  - Can act as jury member using a phone or tablet, and act as jury president using a tablet

66.0.0: Fix: Results records are extracted in competition results for a specific given category is selected
  - The standard "protocol" look templates show the templates (Total, SnCjTot)

66.0.0: Clean-up of the solo referee/announcer/3-referee behavior wrt reversal delay and initial decision
  - Solo referee decisions have a reversal delay, same as 3 referees.  Only the visual rendering changes (single referee light instead of 3)
    - in solo referee mode, the first decision received from any referee is automatically the majority
    - an indicator is propagated to modulate display
  - Announcer input of a decision has no reversal delay because it usually follows flags or some incident
    - no INITIAL_DECISION event is therefore sent
    - unless the announcerTriggersInitialDecision feature toggle is sent (for example, to always have good/bad lift videos on a listener)

66.0.0: showDecisionsImmediately feature toggle (off by default, TCRR still indicates a 3-second delay)
  - Show the decision as soon as the 3 referees have given it. Note: this always sends INITIAL_DECISION


For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
