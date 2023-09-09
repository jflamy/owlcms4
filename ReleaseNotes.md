> **Version 44 is a Technical migration release: DO NOT USE release 44 until an explicit note that it has been stabilized.**
>
> Version 44 updates to the most current Vaadin 24, which has several major changes. The most important is a switch to a more modern template framework (used for the scoreboards, timers, and decisions).
>
> STATUS
>
> - owlcms:  Migration complete. No systematic tests yet.
> - publicresults: Migration complete.

44.0.0-alpha16:

- Wrong main class in publicresults jar causing issue in installer.
- Fixed display parameter dialogs not showing up in the production build, replaced events with callbacks.
- Fixed scoreboards that would not show up in the installed version due to differences in LitElement bundling.
- The default styling directory is now `css/nogrid` to give a more modern look. 
  - The old "styles" directory has moved to `css/grid`. 
  - An alternate styling directory can specified in the Preparation - Settings - Customization page.  For example, to get the old look back, the value would be `css/grid`  .
  - Local variations to styling should be copied to a subdirectory of `css`.  If you have customized `styles`, move it to `css/myfederation` (or whatever name you choose), and define it in the Customization page.


44.0.0

- Scoreboards, timers and decision displays migrated to a new template mechanism.
- The break management dialog for pauses and countdowns has been redesigned.
- The download dialog for documents has been redone for robustness.
- A new Team report is available to simplify VFE.  The document allows signing off changes to category, entry total, as well as team memberships.
- The application has been migrated to the most current Vaadin version 24.
