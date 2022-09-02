- 4.33.12: Fix issue introduced in 4.33.11 that will cause the http server that dispatches work to restart after 3 hours or so.  The pages would have reloaded correctly in any case.
- 4.33.11: Database location parsing was broken for Heroku in releases 4.33.9 and 4.33.10. We now  give precedence to JDBC_DATABASE_URL over DATABASE_URL if both are provided (as is the case on Heroku). Parsing of DATABASE_URL was also fixed for completeness.
- 4.33.10: Cloud documentation reorganized to avoid redundancies and increase visibility of publicresults.
- 4.33.9: Cloud Support Changes
  - Added [instructions](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Fly) for using [fly.io](https://fly.io) as a cloud provider. owlcms now automatically detects and uses the postgres database provided by fly.io .
  - Adjusted [instructions](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Heroku) for using Heroku now that there is no longer a free tier.
- 4.33.8: A new "local templates only" checkbox is added on the Languages and Settings page. If selected, the built-in Excel templates will not be listed in the dropdown lists. Only what is in the `local/templates` folder (or has been uploaded as a zip) with be shown. You can therefore remove files you don't use from local/templates and rename the templates to your local language if you wish. 
- 4.33.7: Examples for Masters and for different file formats to [Record Definition Examples](https://www.dropbox.com/sh/409fqybabjv6byt/AADZIcMxn2Q8epqiZQX3EQk4a?dl=0)
- 4.33.6: fixes for jury summoning referees and for decision reminders
- **Recommended update if current version older than 4.33.4:** Fix for possible birth date errors (one day early) on laptop installations
  - Due to a bug in the way the H2 database driver stores dates that have no time zone (#513),  the birth date of the athletes would, in some time zones, be converted to the day before.
- A public-facing view decisions display has been added to the streaming-oriented displays, for convenience.  Currently this is the same as cropping the top-right corner of the scoreboard. In the future there might be options to just have the timer and the decisions.

##### Highlights from recent stable releases

- Records
  - Records are shown if record definition Excel spreadsheets are present in the local/records directory.  See the following folder for examples: [Sample Record Files](https://www.dropbox.com/sh/sbr804kqfwkgs6g/AAAEcT2sih9MmnrpYzkh6Erma?dl=0) . 
  - Records definitions are read when the program starts.  Records set during the competition are updated on the scoreboard, but the Excel files need to be updated manually to reflect the official federation records.
  - Records are shown according to the sorting order of the file names. Use a numerical prefix to control the order (for example 10Canada.xlsx, 20Commonwealth.xlsx, 30PanAm.xlsx).
  - All records potentially applicable to the current athlete are shown on the scoreboard.  Records that would be improved by the next lift are highlighted.  If there are too many athletes in a group the records can be hidden using the display-specific settings, or by adding `records=false` to the URL
  - Notifications of record attempts and new records are shown on the scoreboard and attempt board. See [this reference](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Styles#hiding-notifications) if you need to disable.
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

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)

    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

    For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/HerokuLarge).  You may also use the Docker container if you prefer.

- For **Docker**, you may use the `owlcms/owlcms` and `owlcms/publicresults` images on hub.docker.com.  `latest` is the tag for the latest stable image, `prerelease` is used for the latest prerelease.  You will need to provide the `JDBC_DATABASE_URL` `JDBC_DATABASE_USERNAME` and `JDBC_DATABASE_PASSWORD` environment and point them to a Postgres database instance (for example, in another container). owlcms will create/alter the required tables and the account requires the privileges to do so. See [Postgres database creation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/PostgreSQL?id=initial-configuration-of-postgresql) for additional info.

- For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 