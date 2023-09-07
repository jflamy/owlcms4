> **Version 44 is a Technical migration release: DO NOT USE release 44 until an explicit note that it has been stabilized.**
>
> Version 44 updates to the most current Vaadin 24, which has several major changes. The most important is a switch to a more modern template framework (used for the scoreboards, timers, and decisions).
>
> STATUS
>
> - owlcms:  Migration complete. All features required for a competition are present. No systematic tests yet.
> - publicresults: Not converted yet.

44.0.0-alpha11:

- Migrated athlete- and public-facing decision displays to LitElement.
- Simplified BeepElement for mobile device refereeing interface
- Athlete card now uses automatic progression calculations from the domain object.

44.0.0

- Scoreboards, timers and decision displays migrated to a new template mechanism.
- The break management dialog for pauses and countdowns has been redesigned.
- The download button for documents has been redone for robustness.
- The application has been migrated to the most current Vaadin version 24.
