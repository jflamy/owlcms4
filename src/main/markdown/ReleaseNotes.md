**Version 49.1 alpha**

> [!CAUTION]
>
> - This is an **alpha release**, used for validating new features.  *Some features are likely to be incomplete or non-functional*.  
> - **Alpha releases are not normally used in actual competitions.**

- New: The speaker gets a notification for a change of athlete
  - The notification is synchronized with the appearance of the athlete's name on the attempt board to respect TCRR rules.

- New default values:  These features will eventually be selectable using the interface.
  - Declarations are *not* shown to the speaker unless the `showDeclarationsToAnnouncer` toggle is used.
  - The marshal screen shows athletes in start order order unless `marshalLiftingOrder` is present.  This avoids clicking on the wrong athlete because the underlying order has just changed.

- New selectable behaviors: these features will eventually be selectable using the interface.
  - When using the `centerAnnouncerNotifications` feature switch, Announcer notifications are centered and larger
  - When using the `noLiveLights` toggle, the decision lights are not shown to the announcer or marshal to prevent premature announcements of decisions.
- Improvements:
  - The information update in the announcer and marshal grids now allows announcing the total as soon as the decision is made.
- USAW note: the list of feature toggles to use until the options are configurable using the interface is
  `USAW,athleteCardEntryTotal,explicitTeams,noLiveLights,centerAnnouncerNotifications`

