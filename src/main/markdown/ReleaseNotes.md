**Changes for ${revision}**

- Record attempts and new records now advertised on the attempt board.
  - Record notifications can be enabled/disabled on the scoreboards and attempt board using the z-index CSS property. Set the variable `--zIndexRecordNotifications` in `colors.css` to a positive value (ex: 10) to enable the notifications, and a negative value (ex: -10) to always hide them.

##### Changes for 4.33.2

- Documentation only (Heroku setup, pictures on home page)

##### Changes for 4.33.1

- Shared visual styling between owlcms and publicresults.
  - publicresults scoreboard now uses the same colors.css and results.css stylesheets as owlcms.  owlcms sends the exact files it is using for itself to publicresults. The priority used by owlcms to find the style sheets is as follows:
    1. css loaded in a zip using the Language and Settings page, found in the local/styles folder of the zip.  
    2. css in the local/styles folder where owlcms is installed
    3. css found in the binary files of the owlcms distribution.
  - The Records and Leader sections can now be shown/hidden from the pop-up dialog on the scoreboard screens for both owlcms and publicresults
- Sinclair Meet added as a non-standard competition rule.  This is for meets where the medals are for men and for women independently of the bodyweight and age.  The feature switch still works, but there is now a checkbox on the competition page.

##### Changes for 4.33.0

- Records
  - Records are shown if record definition Excel spreadsheets are present in the local/records directory.  See the following folder for examples: [Sample Record Files](https://www.dropbox.com/sh/sbr804kqfwkgs6g/AAAEcT2sih9MmnrpYzkh6Erma?dl=0) . 
  - Records definitions are read when the program starts.  Records set during the competition are updated on the scoreboard, but the Excel files need to be updated manually to reflect the official federation records.
  - Records are shown according to the sorting order of the file names. Use a numerical prefix to control the order (for example 10Canada.xlsx, 20Commonwealth.xlsx, 30PanAm.xlsx).
  - All records potentially applicable to the current athlete are shown on the scoreboard.  Records that would be improved by the next lift are highlighted.  If there are too many athletes in a group the records can be hidden using the display-specific settings, or by adding `records=false` to the URL
  - Added large notifications in the record section for record attempts and new records.  You can hide the scoreboard record notifications by setting the `--showRecordNotifications` variable at the top of `colors.css` to `hidden` if you do not want them.
- Sinclair meets
  - If the feature switch `SinclairMeet` is defined on the Language and Settings page, then the scoreboard hides the lift and total ranks, and shows the Sinclair and Sinclair rank.  Also, the leaders section uses the Sinclair ranking.
- Documentation now includes a tutorial on how to change the scoreboard colors: [Scoreboard Colors](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Styles) 
- Additional fields on the scoreboards
  - Added the custom1 and custom2 fields to the scoreboards (after the year of birth).  They are hidden by default; change the width to non-zero and visibility to `visible` in results.css in order to show one or the other or both.
- Masters rulebook
  - Updated the default AgeGroups.xlsx definition file for the W80, W85, M85 and M85 age categories.
  - Updated the age-adjusted Sinclair calculation for women to use the SMHF coefficients.
- New: Announcer can act as solo athlete-facing referee. A setting on the announcer screen (⚙) enables emitting down signal on decision so it is heard and shown on displays.
- New: Round-robin "fixed order" option for team competitions.  If this option is selected in the Competition Non-Standard Rules, athletes lift according to their lot number on each round. The lot number can be preset at registration or drawn at random depending on competition rules.
- New: 24h time will now be used in the date-time picker when using English outside of the "traditional" English-speaking countries ("AU", "GB", "IN", "NZ", "PH", "US", "ZA").  On a laptop, the country is obtained from the operating system.  If using English in the cloud, we recommend setting the `OWLCMS_LOCALE` environment variable to `en_SE` in order to get English with consistent ISO date and 24h time formatting throughout the program.

### Highlights from recent stable releases

- On weigh-in or registration forms, if a change in category results, a confirmation is required (#499)
- [Customization](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/UploadingLocalSettings) of colors and styling of scoreboards and attempt board. 
- Improved management of ceremonies : see [Breaks and Ceremonies](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Breaks) procedures, and [Result Documents](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Documents) for the medals spreadsheet.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)

    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

    For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/HerokuLarge).  You may also use the Docker container if you prefer.

- For **Docker**, you may use the `owlcms/owlcms` and `owlcms/publicresults` images on hub.docker.com.  `latest` is the tag for the latest stable image, `prerelease` is used for the latest prerelease.  You will need to provide the `JDBC_DATABASE_URL` `JDBC_DATABASE_USERNAME` and `JDBC_DATABASE_PASSWORD` environment and point them to a Postgres database instance (for example, in another container). owlcms will create/alter the required tables and the account requires the privileges to do so. See [Postgres database creation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/PostgreSQL?id=initial-configuration-of-postgresql) for additional info.

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 