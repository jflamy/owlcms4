40.0 Scoreboards

*Alpha version. Initial testing of new features that can change without warning.*

- 40.0 Changes

  - 40.0.0-beta01: Flags are shown next to the team name when a single category is selected during the medal ceremony.
  - 40.0.0-alpha03: Category switches by the announcer during the medal ceremony are correctly reflected on the main and medal displays.
  - 40.0.0-alpha02: Fix: The attempt board would sometimes display the next athlete's team when the previous decision was still visible (would happen more often when the announcer entered decisions, or when forcing decisions during a simulation)
  - 40.0.0-alpha02: Various clean-up in scoreboard stylesheets to make it easier to create video-oriented scoreboards
  - 40.0.0-alpha01 Fix: Missing Marshal #2 on Officials Schedule pre-competition document, fixed the name of exported file.
  - 40.0.0-alpha00 Added scoreboard to show athletes in their live registration category ranking order (instead of start number). This is meant to be used for public display or video streaming.
  - CSS Styling
    - The use of @import has been removed from the CSS files. An "autoversioning" suffix is used to force the browsers to refresh the colors and other shared files after a program restart.
    - Added an option to disable the "autoversioning" suffix when using browser development tools to edit the CSS files.  See the [Styling](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Styling) documentation for details.
    - The scoreboards will no longer show a scrollbar. This allows a bit more flexibility when zooming in or enlarging the fonts.
  - Fix: Before the start of Clean and Jerk, the order of snatch leaders shown at the bottom of the scoreboard was random.
  - Fix: Added 2nd marshal and 2nd technical controller on protocol sheets.
  

##### Highlights from recent stable releases

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
