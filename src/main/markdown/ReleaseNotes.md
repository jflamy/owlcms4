> **Version 44 is now an early beta release.**
>
> - Beta releases are meant for translations and testing.
> - Please perform exhaustive tests with your own data if you intend to use it in a competition, and report any issues.
>
> Version 44 is mostly a technical migration release. It updates the user interface to use the most current [Vaadin 24 framework](https://vaadin.com/).
>

44.0.0-beta02:

- Tweaked the CSS styles location processing.  If the styles directory named in the database is not found, the default  `nogrid`  is forced. A customized style directory must be moved to `local/css` before it can be used.

44.0 Changes

- The visual styling has been changed for a more modern look. 
  - The new default is `nogrid`
  - The old "styles" has been renamed to `grid`. 
  - An alternate styling directory can specified in the Preparation - Settings - Customization page.  For example, to get the old look back, you would specify `grid` instead of `nogrid`.  You can also specify a local styling directory.
  - Local variations to styling MUST be copied to a subdirectory of `local/css`.  If you have customized the `styles` folder move it to `local/css/mystyles` (or whatever name you want), and update the location your Preparation - Settings - Customization page. 
- A new report is available to accelerate the verification of final entries (VFE).  See the "Pre-Competition Documents" under "Teams". The document allows team leaders to sign off the changes to the athlete's category, entry total, as well as team memberships.
- The break management dialog for pauses and countdowns has been redesigned.
- The download dialog for documents has been redone for robustness.
- This version updates the user interface to use the most current [Vaadin 24 framework](https://vaadin.com/), which has several major changes. The most important is a switch to the modern [LitElement](https://lit.dev/) template framework (used for the scoreboards, timers, and decisions).
