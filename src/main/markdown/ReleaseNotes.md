> New numbering scheme.  First level = significant features that can affect competition flow or results interpretation.  Second level = minor user interface or perceptible technical improvements.  Third level = bug fixes.

- 34.0.0-beta02: Setting a password no longer shows the confusing encrypted password, but rather a string of 10 black circles.  The length of the actual password is not revealed.  Clearing the string clears the password.
- 34.0.0-beta01: Ready for translations.  Fixed logging for public demonstration mode.
- **34.0.0:** **Added new Sinclair coefficients for the 2024 Olympiad**.  An option on the Competition rules page allows selecting the previous (2020 Olympiad) values.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.
- 34.0.0: Additional environment variable OWLCMS_PUBLICDEMO for restarting periodically the public demonstration site.

##### Highlights from recent stable releases

- Cloud Support Changes
  - Added [instructions](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Fly) for using [fly.io](https://fly.io) as a cloud provider (cheaper alternative to Heroku)
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

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For cloud installs, no download is necessary. Follow the **[Heroku](Heroku) **or **[Fly.io](Fly)** installation instructions.

- For **Docker**, you may use the `owlcms/owlcms` and `owlcms/publicresults` images on hub.docker.com.  `latest` is the tag for the latest stable image, `prerelease` is used for the latest prerelease.  
  In the environment variables for owlcms, provide a standard DATABASE_URL to a running postgres instance or container. `postgres://{user}:{password}@{hostname}:{port}/{database-name}` (all parameters are required).
  The database is initially empty. owlcms will create/alter the required tables so the account used requires the privileges to do so. See [Postgres database creation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/PostgreSQL?id=initial-configuration-of-postgresql) for additional info.

- For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 