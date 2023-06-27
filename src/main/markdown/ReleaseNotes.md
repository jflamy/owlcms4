41.0 Styling and Video streaming improvements.

*Beta release. For testing and translation. Test thoroughly if you intend to use it in a competition.*

- 41.0.0-beta04:
  - Styling adjustments to the attempt board to prevent items from moving slightly when records are attempted/improved.
  
  - Made the Protocol template use the overall Rank for the Total, same as the Snatch-CJ-Total template (instead of the current session ignoring the previous ones).
  
  - Small fix to prevent the countdown/decision display from randomly (and rarely) requiring a refresh.
  
- 41.0.0-beta03:
  - The video overlay for highlighting events is now larger and centered.
    For video streaming, added a Video Overlay feature to display competition events such as Jury decisions, Challenges, Records, Technical Breaks, etc. on top of the video feed.  The Overlay is green background so that the video software can make the page transparent.  It is centered by default but you can move or crop the overlay to position it where you want.
    Colors and styles are controlled by `video/eventmonitor.css`

- 41.0.0-beta02:
  - Added an extra column on the registration data export to show the athlete's weight class. This allows sorting athletes by weight class and entry total more easily when allocating athletes to groups.
- In a 3-medal competition, the classification-order scoreboard now shows the clean & Jerk ranking when the snatch is over. It switches to the total when the group is done.  For a one-medal competition, the order switches to total as soon as C&J starts.
- Several changes to CSS Styling
  - Medal colors are highlighted on the Medal displays.
  - Team flags are shown on the scoreboard
  - Automatic team name sizing on the scoreboard allows the use "auto" or "minmax" CSS grid column sizes.
  - When the leaders are hidden, the space freed can again be correctly used for additional athletes on the main scoreboard.
  - The CSS styling for the "Current Athlete" bottom banner no longer restricts the length of the translated words.
  - The variable OWLCMS_STYLESDIR indicates what directory contains the style sheets.  The default value is "styles". This makes it easier to create and test alternate visual styles.

- Reorganized the buttons on the Display Selection and Video Streaming pages to be more logical
- Special Characters (such as Alt-Enter) are removed when reading Excel registration data

##### Highlights from recent stable releases

- Flags are shown on the medals display when a single category is shown.
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
