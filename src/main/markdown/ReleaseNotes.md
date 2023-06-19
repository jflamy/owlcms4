41.0 Styling Improvements.

*Alpha release. For experimentation and feedback only. Features may change without notice.*

- 41.0.0-alpha02:
  - Medal colors are highlighted on the Medal displays.
  - Show flags on scoreboard
  - (experimental) Automatic team name sizing on scoreboard, uses "auto" or "minmax" CSS grid column sizes.
  
- 41.0.0-alpha01: 
  - In a 3-medal competition, the classification-order scoreboard now shows the clean & Jerk ranking when snatch is over. It switches to the total when the group is done.  For a one-medal competition, the order switches to total as soon as C&J starts.
  - Fixed the layout of the video streaming header for the classification and medals page to be on two lines
- Small addition to the scoreboard HTML page structure to get more CSS formatting flexibility.
- The CSS styling for the "Current Athlete" bottom banner no longer restricts the length of the translated words.
- The variable OWLCMS_STYLESDIR indicates what directory contains the style sheets.  The default value is "styles". This makes it easier to create and test alternate visual styles.
- Reorganized the buttons on the Display Selection and Video Streaming pages to be more logical

##### Highlights from recent stable releases

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
