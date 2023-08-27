> **Version 44 is a Technical migration release: DO NOT USE release 44 until an explicit note that it has been stabilized.**
>
> Version 44 uses the most current Vaadin 24, which has several major changes.  Vaadin has switched to a newer and much cleaner JavaScript templating toolkit (LitElement) so the major difference is that all the scoreboards, timers and decision displays have to be migrated.
>
> MISSING
>
> - scoreboards have not been migrated yet
> - publicresults does not work (requires scoreboard migration)

44.0.0-alpha05: Snapshot release.

- Redid the download button mechanism for documents for better robustness.
- Standard scoreboard upgrades on lifts, doesn't reflect breaks correctly yet

44.0.0

- Migration of scoreboards, timers and decision displays to a different JavaScript templating toolkit
- Migration of the application to Vaadin v24
- The break management dialog for pauses and countdowns has been redesigned.
- The download button for documents has been redone for robustness.
