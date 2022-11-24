> New numbering scheme.  First level = significant features that can affect how a competition is run.  Second level = smaller features such as user interface improvements or technical changes.  Third level = bug fixes.

- 35.0.0-beta:

  *Beta release: Ready for translation and exploratory use. Test thoroughly with your own data if you intend to use in a meet.*

  Release 35 aims at fully supporting jury, referee and timekeeper devices that can send commands using MQTT without having to act as a keyboard.  Release 35 also improves processing of records.

  - Enhancements / functional changes

    * 35.0.0-beta08: Export of registration data now lists the groups per platform to make time allocation easier.
    * 35.0.0-beta06: Improved ordering of the registration export page to make it easier to do the initial allocation to groups when there are multiple age groups and there is a need to create A and B groups.
    * 35.0.0-beta05: Experimental capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/FlagsPicture) documentation.
    * 35.0.0-beta05: Clicking on the pink break buttons no longer starts a technical or marshal break immediately. It is possible to cancel, or to change the kind of break desired. The ▶ button must be used to start the break. Also, the Marshall break button is labeled "Marshal Issue" to make it's purpose clearer. (#569)
    * 35.0.0-beta03: Added a feature toggle `forceAllGroupRecords`.  If true, the scoreboards will show all the records that can be broken in the current group (as opposed to just those for the current athlete, which is the default.) This works if there is less than about 6 age groups in the group (zooming and adjusting the font size may be required)
    * 35.0.0-beta01: Removed obsolete feature on break dialog to toggle display of next weight (now always shown)
    * 35.0.0-beta00: new feature toggle `mqttDecisions` for testing purposes.  This tells the decision displays to send MQTT messages just as if they were independent referee boxes.
    * 35.0.0-alpha12:  New Records Eligibility Criteria based on Federations (so invited athletes do not show up as breaking local records, etc.). See [Records Eligibility](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Records#eligibility-criteria) documentation.
    * 35.0.0-alpha12: Timekeeper can now switch groups and start countdowns. Useful when veteran announcer is only using the scoreboard.
    * 35.0.0-alpha11: Field of play state is now full source of truth for jury decisions. (#469)
      * Refreshing the jury page (or starting a new one) fetches the referee and jury decisions as stored in the state.
      * Jury and Referee decisions are now cleared as late as possible on clock restart following change of clock ownership or when a new clock is granted, or on group change.
    * 35.0.0-alpha09: MQTT events from referee box and jury box shown on jury page (#469)
    * 35.0.0-alpha08:  Record eligibility check for Age Groups.  (#555).  See [Records Eligibility](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Records#eligibility-criteria) documentation.
    * 35.0.0-alpha07:  Add version number on installer and zip files (#552)
    * 35.0.0-alpha01:  Record information is now included in the json database export (#563)
    * 35.0.0-alpha01:  In multi-lingual settings, a drop down at the top of navigation pages allows changing the language for the current browser. Pages and displays opened from that browser will be in the new language; the overall default is not changed.  (#553)
    * 35.0.0-alpha01:  Notification given to TOs if current athlete does not meet starting weights rule when called for first clean and jerk (TCRR regulation to rule 6.6.5 clause 6) (#556)
    * 35.0.0-alpha00: **Full support of MQTT messaging for devices**. Jury/referee/timekeeper devices can now issue MQTT commands instead of key presses, and subscribe to messages issued by the main program (#469)  See  [MQTT Messages documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/MQTTMessages).
    
    - 35.0.0-alpha00: improved colors on Current Athlete view (current requested weight more visible, fixed empty cell colors) (#562)
    
  - Fixes:
  
    - 35.0.0-beta08: Fixed 2 missing values in the default Latin American Spanish translations
  
    - 35.0.0-beta07: Fixed regression that disabled MQTT referee reminders (#571)
  
    - 35.0.0-beta07: Fixed premature notifications to technical officials for events that were in fact forbidden (#570)  All TO notifications were refactored to be issued from the FOP state machine.
  
    - 35.0.0-beta04: Updated documentation for [Records](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Records) to describe  the fields and to include links to examples.  
    
    - 35.0.0-beta04: The .zip distributions for owlcms were missing some of the directories under `local` (e.g. records).
    
    - 35.0.0-beta01: Ignore superseded out-of-order events on the asynchronous UI Update bus (#567). The events are innocuous but make logs confusing to read.
    
    - 35.0.0-beta01: Code review to improve concurrency-resilience and privacy of the field of play state (#566)
    
    - 35.0.0-alpha13: Fixed import of registration data for federation codes used for record eligibility.
    
    - 35.0.0-alpha00: Bottom part of Current Athlete view would switch to next athlete's attempts before top part updated (#558)
    
    - 35.0.0-alpha00: Final Package was including SMF and Sinclair results for all athletes, ignoring the filtering requested. (#561)
    
    - 35.0.0-alpha00: On mobile refereeing screen, reversal was not restoring button colors (#560)
    
    - 35.0.0-alpha00: Record attempt notification remained on attempt board at end of group (#557)
  
- 34.4.0

  - Improvements

    - New Weigh-in Starting Weights template to create a Weigh-in Summary. Meant to facilitate data entry. As used in Pan-American federation, the body weight and declarations are copied one per line and countersigned by the coach/athlete.
    - New Jury template with bigger cells and with additional info (team, entry total)
    - There are now separate passwords for officials (OWLCMS_PIN) and for displays (OWLCMS_DISPLAYPIN).  Normally only the officials password is set. This protects the competition input screens but keeps password-less access to the displays. If needed, scoreboards can also be protected by a password and by a separate list of authorized access lists.
    - The "Recompute Ranks" button on the Results and End of Competition pages now performs a full recalculation of all the ranks taking into account all the eligibilities.  This will now reflect eligibility changes made in the course of the competition, if any.
    - Added USAW Age group file and updated the results sheet to be in the USA BARS format.  To produce results for BARS you need to have loaded the USA Age Groups file and assigned the athletes using the USA Age Groups.

  - Fixes:

    - Added hard page breaks to the Athlete cards; this works around a problem whereby Excel showed a correct print preview but the printer driver miscalculated margins.
    - When using flags, the announcer-entered decision is now correctly redisplaying the new clock value when the same athlete follows in sequence.
    - Fix: Jury display. Reds given if bar had gone past the knee and then put down are no longer erased from the referee decision section when the clock is restarted.  
    - Adjustments to the statuses provided for automatic video scene switching
    - Bookmarking the main screen with "public=true" now works.

- 34.3.0: New end of competition report: records set during the meet are exported in the same format as the record definition files, to facilitate post-competition updates. Newly set records are kept in the database and preserved on application restart; a new "Clear New Records" is available to remove them when doing pre-competition tests.

- 34.3.0: Safe export/import of all registration data, including eligibility categories. Until competition start, it is now possible to use Excel to safely reassign athletes to groups, change athlete categories (including the additional categories where the athlete is eligible), to define new groups, to change referee assignments, etc.  This recreates clean athletes and groups, after weigh-ins have started changes must be made using the program screen.

- 34.2.1: Fix for refereeing devices: When using USB or Bluetooth devices attached to the athlete-facing display, decisions were not shown on that display, but were shown on all others ((problem introduced in 34.2.0)

- 34.2.1: **Important update**: Fixed an *extremely rare issue* *that could nevertheless stop the competition from proceeding*.  Ranks are updated after every lift, before updating the lifting order, so a fatal error in updating the ranks would prevent the lifting order update.

- 34.2.0: Improvement: The lifting order display is now a full scoreboard (shows the 6 attempts)

- 34.2.0: Decisions entered by the announcer are now shown immediately by default, unless there is a single referee using the screen and the "emit a down signal" setting is enabled.  This is useful when flags or standalone systems are used, or when there is a refereeing device disconnected.

- 34.2.0: On the protocol sheets, when athletes are eligible for multiple age groups they will now be shown in each eligible grouping, with the corresponding ranking and Robi.  To get the old behavior back (each athlete shown only once in their "natural" age group) you can use the "6Attempts" template.

- 34.2.0:  Group Results page no longer shows all athletes by default, as this is inconvenient for large competitions with hundreds of competitors.  The "All Groups" option can be used at the end of the meet to create a full results sheet for the federation.

- 34.2.0: MQTT timekeeping device support: An MQTT timekeeper device can send one of the 4 following commands (*platform* is the code for the targeted platform): `/clock/platform/start` `/clock/platform/stop` `/clock/platform/60` `/clock/platform/120`
  Note that only 60 and 120 are the only legal numerical values to reset the clock to the corresponding number of seconds.

- 34.2.0: Fixed the competition simulator to correctly handle the end-of-group events.

- 34.1.0:  User interface improvement: Added athlete card button to handle athlete withdrawing from snatch but continuing with clean & jerk.

- 34.0.1: When creating the Excel reports for a group, the group definition is now read again from the database to ensure its correctness.

- **34.0.0:** **New Sinclair coefficients for the 2024 Olympiad**.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.

- 34.0.0: Setting a password no longer shows the confusing encrypted password, but rather a string of 10 black circles, so that neither the password nor its length is revealed.  Clearing the string clears the password.

- 34.0.0: Additional environment variable OWLCMS_PUBLICDEMO for restarting periodically the public demonstration site.

##### Highlights from recent stable releases

- Cloud Support Changes
  - Added [instructions](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Fly) for using [fly.io](https://fly.io) as a cloud provider (cheaper alternative to Heroku that is no longer free)
  - Adjusted [instructions](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Heroku) for using Heroku now that there is no longer a free tier.
- Ability to hide unneeded templates and rename templates to local language
  - A new "local templates only" checkbox is added on the Languages and Settings page. If selected, the built-in Excel templates will not be listed in the dropdown lists. Only what is in the `local/templates` folder (or has been uploaded as a zip) with be shown. You can therefore remove files you don't use from local/templates and rename the templates to your local language if you wish (non-Latin languages are supported).

- Records
  - Records are shown if record definition Excel spreadsheets are present in the local/records directory.  See the following folder for examples: [Sample Record Files](https://www.dropbox.com/sh/sbr804kqfwkgs6g/AAAEcT2sih9MmnrpYzkh6Erma?dl=0) (includes examples for Masters)
  - Records definitions are read when the program starts.  Records set during the competition are updated on the scoreboard, but the Excel files need to be updated manually once the federations makes the record official.
  - Records are shown according to the sorting order of the file names. Use a numerical prefix to control the order (for example 10Canada.xlsx, 20Commonwealth.xlsx, 30PanAm.xlsx).
  - If there are too many athletes in a group the records can be hidden using the display-specific settings, or by adding `records=false` to the URL
  - Notifications of record attempts and new records are shown on the scoreboard and attempt board. See [this reference](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Styles#hiding-notifications) if you need to disable the notifications.
- Additional fields on the scoreboards
  - Added the custom1 and custom2 fields to the scoreboards (after the year of birth).  They are hidden by default; change the width to non-zero and visibility to `visible` in results.css in order to show one or the other or both.
- Shared visual styling between owlcms and publicresults.
  - publicresults scoreboard now uses the same colors.css and results.css stylesheets as owlcms.  owlcms sends the exact files it is using for itself to publicresults. The priority used by owlcms to find the style sheets is as follows:
    1. css loaded in a zip using the Language and Settings page, found in the local/styles folder of the zip.  
    2. css in the local/styles folder where owlcms is installed
    3. css found in the binary files of the owlcms distribution.
  - The Records and Leader sections can now be shown/hidden from the pop-up dialog on the scoreboard screens for both owlcms and publicresults
- Masters rulebook
  - Updated the default AgeGroups.xlsx definition file for the W80, W85, M85 and M85 age categories.
  - Updated the age-adjusted Sinclair calculation for women to use the SMHF coefficients.
- New: Announcer can act as solo athlete-facing referee. A setting on the announcer screen (⚙) enables emitting down signal on decision so it is heard and shown on displays.
- New: Round-robin "fixed order" option for team competitions.  If this option is selected in the Competition Non-Standard Rules, athletes lift according to their lot number on each round. The lot number can be preset at registration or drawn at random depending on competition rules.
- Sinclair meets: New competition option to use Sinclair for ranking - one ranking per gender. 
- Documentation now includes a tutorial on how to change the scoreboard colors: [Scoreboard Colors](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Styles) 
- On weigh-in or registration forms, if a change in category results, a confirmation is required (#499)
- [Customization](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/UploadingLocalSettings) of colors and styling of scoreboards and attempt board. 
- Improved management of ceremonies : see [Breaks and Ceremonies](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Breaks) procedures, and [Result Documents](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Documents) for the medals spreadsheet.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_${revision}.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For cloud installs, no download is necessary. Follow the **[Heroku](Heroku) **or **[Fly.io](Fly)** installation instructions.

- For **Docker**, you may use the `owlcms/owlcms` and `owlcms/publicresults` images on hub.docker.com.  `latest` is the tag for the latest stable image, `prerelease` is used for the latest prerelease.  
  In the environment variables for owlcms, provide a standard DATABASE_URL to a running postgres instance or container. `postgres://{user}:{password}@{hostname}:{port}/{database-name}` (all parameters are required).
  The database is initially empty. owlcms will create/alter the required tables so the account used requires the privileges to do so. See [Postgres database creation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/PostgreSQL?id=initial-configuration-of-postgresql) for additional info.

- For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 

[//]: # "- 34.0.0: Release candidate ([definition](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate)), usable in competitions."