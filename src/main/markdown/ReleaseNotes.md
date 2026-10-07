<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**New in Release 69.0**

- 69.0.0: Named accounts with role-based access control.
  - Assign officials access to specific platforms and duties; existing PIN-based access remains available.
  - Login opens the page appropriate to the assigned role; accounts with multiple roles start on Home.
  - Enabling accounts creates missing role accounts, with platform-specific accounts for platform duties. The account list identifies accounts that still need a password.
  - Account names preserve the written case of platform names; login names are case-insensitive.
  - Platform-role accounts omit the platform suffix when there is only one platform. "Reset Platform Accounts" recreates conventional role accounts for the current platforms, clearing their passwords while preserving custom and global accounts.

- 69.0.0: Per-platform tamper-resistant audit trails record competition actions and athlete changes.
  - Includes accepted and refused commands, record changes, session activity, and application start/stop events, with operator and station identification.

- 69.0.0: Optional resource monitoring logs CPU, process memory, heap and garbage-collection usage every 30 seconds.
  - Enable `resourceTraces` in System Settings. Samples appear in the normal log and a separate timestamped resource table; normal-log samples can be disabled independently.

- 69.0.0: Competition Director page for managing lifting
  - Enabled by named accounts; in PIN mode, enabled by `competitionDirectorPage` feature switch.
  - Passive speaker view follows live lifting without allowing timer control, athlete edits, or decision reversals.

- 69.0.0: Event forwarding keys are securely encrypted in JSON database exports.

- 69.0.0: VFE team reports flag category and team-size violations.

- 69.0.0: Session selectors distinguish completed and upcoming sessions, with ordering appropriate to registration, weigh-in, and results.

-For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
