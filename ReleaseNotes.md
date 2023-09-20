43.0

> *Release Candidates* are the final testing releases
> *Do test thouroughly before using in a a competition.*

- 43.0.6:
  - Fix: Starting the snatch countdown after the end of introduction no longer requires ending the break.
  - Fix: Exporting a database that included records with no record date and no athlete age could fail.
  
- Referee decision updates are ignored once the decision has been shown. 
  - Referees must use flags or cards after 3 seconds. In this way, what the jury sees matches what the public saw.

- Prevention of accidental countdown interruptions.  
  - When a countdown is running it is now necessary to use the "Pause" dialog in order to switch to a different kind of break.
  - Therefore, accidentally pressing the jury control button for a technical break (or other) will be ignored during the countdowns to the introductions, the first snatch, or the first clean & jerk.
  - Ceremonies (Introduction, Medals) work like before since they do not interrupt the countdown.

- Records
  - Error messages are now visible directly in the user interface, and have also been improved to catch more types of errors.

  - It is now possible to export all records as a single Excel to check what has been loaded or to reload in a later competition.
- CSS Styling:  
  - An alternate directory to use for styling files can now be given on the "System Settings - Personalization" page.  The directory name given is looked up in the "local" subdirectory of the installation (the default is "styles").  BEWARE: if given, the `OWLCMS_STYLESDIR` variable takes precedence over the database setting.
  - It is now easier to hide the body weight category column to promote inclusivity in local competitions: In `local/styles/resultsCustomization.css`, set `--categoryVisibility=hidden` and `--categoryWidth=0` to hide the body weight category column on the scoreboard.
- Public Results 
  - It is now possible to choose the lifting order instead of the start number order on the remote scoreboard (click on the scoreboard to see the options)
  - Flags are shown on the remote scoreboard if present in the main owlcms `local/flags`
  - All the styles sheets under `local` are sent to the remote server.  The styles directory specified in the owlcms configuration is used by publicresults also, so the "look and feel" is the same on both ends. 
  - However, on publicresults, the `publicresultsCustomization.css` file is used instead of `resultsCustomization.css`.  By default, these two files are the same, but editing `publicresultsCustomization.css` allows for different column visibility on the remote scoreboard.
- The Session editing form now uses tabs for better visual organization.
- An MQTT `fop/config` message is published on startup and when platforms are edited or deleted.  Device management applications can listen to this message to display the available platforms.

#####  Highlights from recent stable releases

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
- We now recommend using [fly.io](https://owlcms.github.io/owlcms4/#/Fly) as the cloud installation is straightforward and owlcms can be run for free. Heroku is now deprecated as they have broken the easy install method and are no longer free.
- The editing page used for athlete registration and weigh-in has been redone to be more readable and better organized.
- The jury members can now vote again during a deliberation break. The decision lights are reset when deliberation starts so the post-video vote is a secret vote. 
- A new site section has been added to start the displays used for video streaming (see the [streaming documentation](https://owlcms.github.io/owlcms4/#/OBS?id=_2-setup-owlcms-with-some-data)). The video-oriented scoreboards have a different header with the event name and group description, and show different columns (by default, the same as used by IWF).


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_43.0.6.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_43.0.6.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the **[Fly.io](https://owlcms.github.io/owlcms4/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4/#/LocalWindowsSetup)
