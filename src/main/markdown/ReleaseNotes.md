**Version 50.0 alpha**

> [!CAUTION]
>
> - This is an **alpha release**, used for validating new features.  *Some features are likely to be incomplete or non-functional*.  
> - **Alpha releases are not normally used in actual competitions.**

- (alpha03) Fix: IWF behavior for start numbers ([gh-1025](https://github.com/jflamy/owlcms4/pull/1026))
  
- (alpha02) New: color for children bar
  - If lighter children bars are used, they will be shown in white/green/yellow on the attempt board according to bar weight.  As a special case, brown is used for old-style North-American 15lb bars (counted as 7kg)
  - Automatic bar/collar switching: if the feature toggle `childrenBars` is used, the changes in bar and collars will be automatic when the age group includes children 12 year old or younger.  It is assumed that 5/10/15kg bars are available and will be used, and that clips can be used (not counted in the weight).   A group with boys 12-13 would automatically use the 15 bar, whereas a 13-15 group would not -- the manual setting from the Plates and Barbell page would be needed.
  
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
  `USAW,athleteCardEntryTotal,explicitTeams,noLiveLights,centerAnnouncerNotifications,childrenBars`

