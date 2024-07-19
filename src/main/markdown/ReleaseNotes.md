**Version 50.0 release candidate**

> [!WARNING]
>
> - This is a release candidate [(see definition)](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate), used for final public testing and translation. *It is still a preliminary release*
> - You should test all releases, with actual data, *several days* before a competition. This is especially important when considering the use of a release candidate.

- (rc03) The medal sheet and the medal display screens now correctly include all the categories where medals can be awarded. Previously, if an athlete had not weighed in, or had been unassigned to a session a category could be considered to still be in progress.

- (rc03) Fix for categoryDone column needlessly included in the persisted database, causing issues with json export and other features (#1054)

- (rc03) Records are now imported correctly from a database export.  Previously some could be missing, requiring a second import.

- (rc03) The "non-standard bar is in use" indicator is now read correctly from the database.  In prior 50.x releases this was not initialized properly and could prevent marshal changes from working.

- (rc02) Fix: current athlete display did not update or would fail to start.

- (rc02) Fix: After introductions, could not go back to break management dialog to start the Time to snatch timer

- (rc02) The local/robi directory was not being populated by the Windows installer (contains the records used for computing Robi coefficients)

- (rc02) Technical: continuing cleanup of code used to determine the current field of play

- (rc01) Additional updates to translations: Spanish, Portuguese

- (rc01) Publicresults: On session expiry,  reload button label omits the platform name when there is only one platform.

- Publicresults session expiry.  In previous versions, the instantaneous update feature of the scoreboards caused sessions to stay open forever, leading to out-of-memory errors after a few hours when many users had logged on.

  - Sessions are now expired if all scoreboards have been hidden for more than 15 minutes, or if the visible scoreboards have not received an update for 15 minutes.  When expired, an expiry notice is given, and  button is displayed to reload the expired scoreboard.
  - The environment variable `OWLCMS_INACTIVITY_SEC` controls the duration in seconds of the inactivity interval (default = 15 * 60).
  - The environment variable `OWLCMS_CLEANUP_SEC` controls how often the sessions are checked for inactivity (default = 60)

- New: Marshal and announcer
  - The notification is synchronized with the appearance of the athlete's name on the attempt board to respect TCRR rules.
  - The information update in the announcer grid now allows announcing the total as soon as the decision is made.

  - Declarations are *no longer* shown to the speaker by default. The speaker can show/hide the declaration notifications using the ⚙ menu.
  - The marshal screen shows athletes in start order by default. The marshal can unselect this behavior and get lifting order using the ⚙ menu.
  - When the `noLiveLights` [feature toggle](https://owlcms.github.io/owlcms4-prerelease/#/FeatureToggles) is present, the decision lights are not shown to the announcer or marshal to prevent premature announcements of decisions. The speaker can override the live lights setting using the ⚙ menu.
  - When the `centerAnnouncerNotifications` [feature toggle](https://owlcms.github.io/owlcms4-prerelease/#/FeatureToggles) is present, the Announcer notifications are centered and larger.  The speaker can override this setting the notifications of not using the ⚙ menu.  When the speaker does double duty as marshal in a small meet, this interferes with the grid, so it is not the default.

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
  - Accept common image formats for flags (`.svg`, `.png`, `.jpg`, `.jpeg`, or `.webp`)

- USAW note: the list of feature toggles to use for National championships is
  `USAW,athleteCardEntryTotal,explicitTeams,noLiveLights,centerAnnouncerNotifications,childrenEquipment`

