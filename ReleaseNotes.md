> **Version 44 is now an early beta release.**
>
> - Beta releases are meant for translations and testing.
> - Please perform exhaustive tests with your own data if you intend to use it in a competition, and report any issues.
>
> Version 44 is mostly a technical migration release. It updates the user interface to use the most current [Vaadin 24 framework](https://vaadin.com/).
>

44.0.0-beta01:

- Production bundles of both owlcms and publicresults are done.

44.0.0

- The default styling directory is now `css/nogrid` to give a more modern look. 
  - The old "styles" directory has moved to `css/grid`. 
  - An alternate styling directory can specified in the Preparation - Settings - Customization page.  For example, to get the old look back, you would specify `css/grid`  .
  - Local variations to styling should be copied to a subdirectory of `css`.  If you have customized the `styles` folder rename it to `css/mystyles` (or whatever name you choose), and update your Customization page.
- A new Team report is available to simplify VFE.  The document allows signing off changes to category, entry total, as well as team memberships.
- The break management dialog for pauses and countdowns has been redesigned.
- The download dialog for documents has been redone for robustness.
- This version updates the user interface to use the most current [Vaadin 24 framework](https://vaadin.com/), which has several major changes. The most important is a switch to the modern [LitElement](https://lit.dev/) template framework (used for the scoreboards, timers, and decisions).
