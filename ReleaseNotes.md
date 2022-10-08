> New numbering scheme.  First level = significant features that can affect competition flow or results interpretation.  Second level = smaller features such as user interface improvements or technical changes.  Third level = bug fixes.

- 34.2.0-rc01: **Important update**: Fixed a *very rare issue* *that could stop the competition from proceeding*. Sorting the ranks would crash, for example, if an "in-progress" Sinclair value based on the snatch exceeded the "official" Sinclair of another earlier competitor. Because ranks are updated after every lift, this could block the updating of the lifting order.
- 34.2.0-rc01: Fixed the competition simulator to correctly handle the end-of-group events that were cleaned-up to support automatic scene switching during streaming.
- 34.2.0-rc01: Fixed group selection menus to use natural ordering (1A comes before 10A).
- 34.2.0-beta03: Ability to run correctly under gitpod.io to facilitate testing of patches and pull requests and enable remote sharing of workspaces. Includes minor code clean-up to remove compiler warnings shown in that environment.
- 34.2.0-beta03: Produce correct error messages and logs when timekeeping MQTT messages were received in incorrect field of play state; fixed intermittent incorrect initialization of MQTT topics.
- 34.2.0: MQTT timekeeping device support: An MQTT timekeeper device can send one of the 4 following commands (*platform* is the code for the targeted platform): `/clock/platform/start` `/clock/platform/stop` `/clock/platform/60` `/clock/platform/120`
  Note that only 60 and 120 are the only legal numerical values to reset the clock to the corresponding number of seconds.
- 34.2.0: The lifting order display is now a full scoreboard (shows the 6 attempts)
- 34.2.0: Decisions entered by the announcer are now shown immediately by default, unless the option to show the down signal is enabled.  This is useful when flags or standalone systems are used, so that the decision is shown on scoreboards without a superfluous 3-second delay.
- 34.2.0: On the protocol sheets, when athletes are eligible for multiple age groups they will now be shown in each eligible grouping, with the corresponding ranking and Robi.  To get the old behavior back (each athlete shown only once in their "natural" age group) you can use the "oldProtocol" feature toggle.
- 34.1.1: Values in cells A1..A4 of the registration spreadsheet contain federation data. The cells were included in the exported file but not read back on import.
- 34.1.0:  User interface improvement: Added athlete card button to handle athlete withdrawing from snatch but continuing with clean & jerk.
- 34.0.1: When creating the Excel reports for a group, the group definition is now read again from the database to ensure its correctness.
- **34.0.0:** **New Sinclair coefficients for the 2024 Olympiad**.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.
- 34.0.0: Setting a password no longer shows the confusing encrypted password, but rather a string of 10 black circles, so that neither the password nor its length is revealed.  Clearing the string clears the password.
- 34.0.0: Additional environment variable OWLCMS_PUBLICDEMO for restarting periodically the public demonstration site.

##### Highlights from recent stable releases

- Cloud Support Changes
  - Added [instructions](https://owlcms.github.io/owlcms4-prerelease/#/Fly) for using [fly.io](https://fly.io) as a cloud provider (cheaper alternative to Heroku that is no longer free)
  - Adjusted [instructions](https://owlcms.github.io/owlcms4-prerelease/#/Heroku) for using Heroku now that there is no longer a free tier.
- Ability to hide unneeded templates and rename templates to local language
  - A new "local templates only" checkbox is added on the Languages and Settings page. If selected, the built-in Excel templates will not be listed in the dropdown lists. Only what is in the `local/templates` folder (or has been uploaded as a zip) with be shown. You can therefore remove files you don't use from local/templates and rename the templates to your local language if you wish (non-Latin languages are supported).

- Records
  - Records are shown if record definition Excel spreadsheets are present in the local/records directory.  See the following folder for examples: [Sample Record Files](https://www.dropbox.com/sh/sbr804kqfwkgs6g/AAAEcT2sih9MmnrpYzkh6Erma?dl=0) (includes examples for Masters)
  - Records definitions are read when the program starts.  Records set during the competition are updated on the scoreboard, but the Excel files need to be updated manually once the federations makes the record official.
  - Records are shown according to the sorting order of the file names. Use a numerical prefix to control the order (for example 10Canada.xlsx, 20Commonwealth.xlsx, 30PanAm.xlsx).
  - If there are too many athletes in a group the records can be hidden using the display-specific settings, or by adding `records=false` to the URL
  - Notifications of record attempts and new records are shown on the scoreboard and attempt board. See [this reference](https://owlcms.github.io/owlcms4-prerelease/#/Styles#hiding-notifications) if you need to disable the notifications.
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
- Documentation now includes a tutorial on how to change the scoreboard colors: [Scoreboard Colors](https://owlcms.github.io/owlcms4-prerelease/#/Styles) 
- On weigh-in or registration forms, if a change in category results, a confirmation is required (#499)
- [Customization](https://owlcms.github.io/owlcms4-prerelease/#/UploadingLocalSettings) of colors and styling of scoreboards and attempt board. 
- Improved management of ceremonies : see [Breaks and Ceremonies](https://owlcms.github.io/owlcms4-prerelease/#/Breaks) procedures, and [Result Documents](https://owlcms.github.io/owlcms4-prerelease/#/Documents) for the medals spreadsheet.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For cloud installs, no download is necessary. Follow the **[Heroku](Heroku) **or **[Fly.io](Fly)** installation instructions.

- For **Docker**, you may use the `owlcms/owlcms` and `owlcms/publicresults` images on hub.docker.com.  `latest` is the tag for the latest stable image, `prerelease` is used for the latest prerelease.  
  In the environment variables for owlcms, provide a standard DATABASE_URL to a running postgres instance or container. `postgres://{user}:{password}@{hostname}:{port}/{database-name}` (all parameters are required).
  The database is initially empty. owlcms will create/alter the required tables so the account used requires the privileges to do so. See [Postgres database creation](https://owlcms.github.io/owlcms4-prerelease/#/PostgreSQL?id=initial-configuration-of-postgresql) for additional info.

- For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://owlcms.github.io/owlcms4-prerelease/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 

[//]: # "- 34.0.0: Release candidate ([definition](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate)), usable in competitions."