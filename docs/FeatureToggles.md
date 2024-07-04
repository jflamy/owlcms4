Feature Switches (also known as Feature Toggles) are parameters that can be turned on or off to change the application behavior.  They are typically used when a feature is added, before it becomes clear how to control through the regular interface. To access the list, use to the Languages and System Settings button from the "Preparation" page and scroll down.

![040_FeatureToggles](img/SystemSettings/040_FeatureToggles.png)

Feature switches are not case sensitive.  Enter them separated by a comma if you need more than one.

### Common Features

These switches may eventually be promoted to the user interface.

| Feature Switch               | Description                                                  | Normal Way to Activate                    |
| ---------------------------- | ------------------------------------------------------------ | ----------------------------------------- |
| athleteCardEntryTotal        | Show the entry total on the interactive athlete card used by the marshal | Only available as a feature switch.       |
| explicitTeams                | When loading the registration file or the SBDE file, do not add the athlete to the teams according to their eligibility categories. Teams must be assigned manually. | Only available as a feature switch.       |
| bestMatchCategories          | If present, then at weigh-in the youngest most specific age group will be selected as the registration category.  Use this when the age alone is sufficient to determine the competition group. | Only available as a feature switch.       |
| bwClassThenAgeGroup          | The normal start group allocation is "ascending body weight category, lot number within body weight category".  This changes the behavior to "ascending bodyweight category, ascending age group, lot number" -- the resulting scoreboard is easier to read in the context of multiple age groups competing simultaneously. | Only available as a feature switch.       |
| gamx                         | activate the GAMX scoring system from Marianne Huebner       | Only available as a feature switch.       |
| forceAllGroupRecords         | On scoreboards show records from all categories, not just that of the current athlete | Only available as a feature switch.       |
| forceAllFederationRecords    | On scoreboards show records from all federations, not just that of the current athlete.  E.g. South American records would be shown for a North American athlete during a Pan American championship. | Only available as a feature switch.       |
| childrenEquipment            | If present, it is assumed that all platforms have 2,5kg and 5kg large discs, and have 5kg and 10kg bars | Only available as a feature switch.       |
| centerAnnouncerNotifications | if present, the notifications to the announcer are centered by default. | Can be changed from the speaker settings. |
| noLiveLights                 | If present, the speaker does not see the live decisions.     | Can be changed from the speaker settings. |

### Specialty Features

These features are not commonly used, and will not be promoted to the interface

| Feature Switch                | Description                                                  | Normal Way to Activate              |
| ----------------------------- | ------------------------------------------------------------ | ----------------------------------- |
| disableRecordHighlight        | If present, the attempt board will not change during record attempts.  This presumes that other means are used to inform the audience (for example, OBS changing the main screen) | Only available as a feature switch. |
| useCustom2AsSubCategory       | (Obsolete) There is now a specific field on the Athlete<br />If present, the value of the Custom2 field of the athlete is assumed to be A, B, C, D and so on to indicate the sub-category.  If the Custom2 field is empty, the A group is assumed, so only the B... groups need to be filled in. | Only available as a feature switch. |
| blackStopButton               | if present, the stop button on the Announcer and Timekeeper page will be black instead of red.  This is to accommodate announcer keypads where red/white buttons are used to enter good/bad lifts signaled with flags, and green/black buttons are used to start/stop the clock. | Only available as a feature switch. |
| noForwarderKeepAlive          | publicresults and video information is sent every 15 seconds.  Set this option to prevent this during troubleshooting. | Only available as a feature switch. |
| enableTimeKeeperSessionSwitch | The time keeper can switch sessions                          | Only available as a feature switch. |

### Current Features

These switches will likely be removed, since they can be activated from the application and the setting is stored in the database

| Feature Switch       | Description                                                  | Normal Way to Activate                                       |
| -------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| localTemplatesOnly   | If present, the default templates distributed inside the owlcms binary will not be shown.  Only the templates found in the local folder will be used.  If a .zip file is used to package the local folder and upload it to the program, then only these templates will be shown.<br />This is normally used to create a zip with only the files used in a given federation, potentially renamed in the local language. | This feature can be activated on the Languages and Settings page. |
| shortScoreboardNames | if present, the normal scoreboards will use the abbreviated first names | On demand, on each scoreboard                                |