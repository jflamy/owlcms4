**Version 50.0 beta**

> [!WARNING]
>
> - This is a **beta release**, used for testing and translation. ***Some features could be non-functional***.
> - Beta releases are **not** normally used in actual competitions, except if a new feature is required. Use extreme care in testing if you intend to do so.

- (beta05) Code review to propagate the platform explicitly instead of relying on session information.
- (beta05) The list of parameters considered when cleaning up the URLs has been updated, so the URLs are again short.
- (beta04) Group menus or group pages could fail with old databases containing empty group names, due to new sorting now used.
- (beta03) Undid accidental rename of a SQL column name that would prevent opening old databases
- (beta02) Fix for plates loading chart computation when using Open Categories
- (beta02) Improvement: accept common image formats for flags (`.svg`, `.png`, `.jpg`, `.jpeg`, or `.webp`)
- New: The speaker gets a notification for a change of athlete
  - The notification is synchronized with the appearance of the athlete's name on the attempt board to respect TCRR rules.
- New default values: 
  - Declarations are *no longer* shown to the speaker by default. The speaker can show/hide the declaration notifications using the ⚙ menu.
  - The marshal screen shows athletes in start order by default. The marshal can unselect this behavior and get lifting order using the ⚙ menu.
  - When the `noLiveLights` [feature toggle](https://owlcms.github.io/owlcms4-prerelease/#/FeatureToggles) is applied, the decision lights are not shown to the announcer or marshal to prevent premature announcements of decisions. The speaker can override the live lights setting using the ⚙ menu.
  - When using the `centerAnnouncerNotifications` [feature toggle](https://owlcms.github.io/owlcms4-prerelease/#/FeatureToggles), Announcer notifications are centered and larger.  The speaker can override this setting the notifications of not using the ⚙ menu.  This is not the default because, in smaller meets, the speaker often does double duty as marshal.
- New: Children's bars
  - The bars available (5 / 10 / 15 / 20 / non-standard) can be selected on the Plates and Barbell page.
  - The computation of the bar used is made to use the large 2,5 or 5kg bumper plates if available.  Collars are not used on light bars. The sturdiest bar is used (for 20kg a 15kg bar with 2.5kg plates will be used instead of a 10kg bar with 5kg plates).
  - Non-standard bars can be selected on the Plates and Barbell page. Typically used with old North-American 15lb (~7kg) bars.
  - For age groups with children under 12, the 20kg bar is not used.  For older age groups where this is desired the 20kg bar can be unselected manually on the Plates and Barbell page.
  - If lighter children's bars are used, they will be shown in white/green/yellow on the attempt board according to bar weight.  Brown is used for non-standard bars
  - The same rules are applied for light Masters loads if the light bars are selected.
  - The [feature toggle](https://owlcms.github.io/owlcms4-prerelease/#/FeatureToggles) `childrenEquipment` automatically indicates that 5/10 bars and 2,5/5 large plates are available.
- Fix: IWF behavior for start numbers ([gh-1025](https://github.com/jflamy/owlcms4/pull/1026))
- Improvements:
  - The information update in the announcer and marshal grids now allows announcing the total as soon as the decision is made.
- USAW note: the list of feature toggles to use for National championships is
  `USAW,athleteCardEntryTotal,explicitTeams,noLiveLights,centerAnnouncerNotifications,childrenEquipment`

