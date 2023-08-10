44.0.0-alpha

> RELEASE 44 is a technical transition release to a new version of the user interface software.
>
> **DO NOT USE release 44**

#####  Highlights from recent stable releases

- Referee decision updates are ignored once the decision has been shown.  Referees must use flags or cards after 3 seconds. In this way, what the jury sees matches what the public saw.
- Prevention of accidental countdown interruptions.  When a countdown is running it is now necessary to use the "Pause" dialog in order to switch to a different kind of break.
- Records
  - Error messages are now visible directly in the user interface, and have also been improved to catch more types of errors.

  - It is now possible to export all records as a single Excel to check what has been loaded or to reload in a later competition.
- CSS Styling:  
  - An alternate directory to use for styling files can now be given on the "System Settings - Personalization" page.  The directory name given is looked up in the "local" subdirectory of the installation (the default is "styles").  If given, the `OWLCMS_STYLESDIR` variable takes precedence over the database setting.
- Public Results 
  - It is now possible to choose the lifting order instead of the start number order on the remote scoreboard (click on the scoreboard to see the options)
  - Flags are shown on the remote scoreboard if present in the main owlcms `local/flags`
  - All the styles sheets under `local` are sent to the remote server.  The styles directory specified in the owlcms configuration is used by publicresults also, so the "look and feel" is the same on both ends. However, on publicresults, the `publicresultsCustomization.css` file is used instead of `resultsCustomization.css`. 
- Interactive scoreboard layout changes
  - On the result scoreboards, you can change the width of the Team column using ⇦ and ⇨ (giving more space for the name) and you can change the font size using ⇧ and ⇩ .
  - If names are very long, you can get an abbreviated first name by adding abb=true to the URL parameters
- The Clean & Jerk break now automatically starts at the end of the snatch.  
  - The duration can now be parameterized in the competition rules (longer breaks when very few athletes, or a shorter or no break for large sessions).
- New pre-competition documents
  - Entry list by bodyweight category, showing the age groups in which the athlete is eligible
  - Team entry list, showing the age group teams in which the athlete is registered
  - Categories entry list, showing the competitor in each age group+body weight category
- Improved styling
  - Medal colors are highlighted on the Medal displays.
  - Team flags are shown on the scoreboard
  - Flags are shown on the medals display when a single category is shown
- For video streaming, added a Video Overlay feature to display competition events such as Jury decisions, Challenges, Records, Technical Breaks, etc. on top of the video feed.
- New session scoreboard for current rankings
- A new Records Management page has been added, reachable from the preparation page.
  - Record definition files can be uploaded interactively 
  - The ordering of the records on the scoreboard is no longer dependent on the file names, and is edited interactively.
- The "Athlete Challenge" situation is now displayed and supported by the MQTT messages and the jury device.
- We now recommend using [fly.io](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Fly) as the cloud installation is straightforward and owlcms can be run for free. Heroku is now deprecated as they have broken the easy install method and are no longer free.
- The editing page used for athlete registration and weigh-in has been redone to be more readable and better organized.
- The jury members can now vote again during a deliberation break. The decision lights are reset when deliberation starts so the post-video vote is a secret vote. 
- A new site section has been added to start the displays used for video streaming (see the [streaming documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/OBS?id=_2-setup-owlcms-with-some-data)). The video-oriented scoreboards have a different header with the event name and group description, and show different columns (by default, the same as used by IWF).


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_${revision}.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_${revision}.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the **[Fly.io](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)
