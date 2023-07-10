41.0 Styling and Video streaming improvements.

*Release Candidate. Test thoroughly if you intend to use it in a competition.*

- Team membership: added a count column to the table, to help with checking if the number of athletes complies with competition rule.  Athletes not in team are sorted at the bottom.
- Start lists
  - Added a Start List template that lists the eligible age groups, for use multiple age group competitions.
  - Added a Start List template that lists the athletes according to their bodyweight class.  This is used in multi-age competitions where athletes of a same weight class compete together and are not separated by age group.
- For video streaming, added a Video Overlay feature to display competition events such as Jury decisions, Challenges, Records, Technical Breaks, etc. on top of the video feed.  The Overlay is green background so that the video software can make the page transparent.  It is centered by default but you can move or crop the overlay to position it where you want.
  Colors and styles are controlled by `video/eventmonitor.css`
- In a 3-medal competition, the classification-order scoreboard now shows the clean & Jerk ranking when the snatch is over. It switches to the total when the group is done.  For a one-medal competition, the order switches to total as soon as C&J starts.
- Several changes to CSS Styling
  - Medal colors are highlighted on the Medal displays.
  - Team flags are shown on the scoreboard
  - Automatic team name sizing on the scoreboard allows the use "auto" or "minmax" CSS grid column sizes.
  - When the leaders are hidden, the space can be used for additional athletes on the main scoreboard.
  - Styling adjustments to the attempt board to prevent items from moving slightly when records are attempted/improved.
  - The CSS styling for the "Current Athlete" bottom banner allows long names for the lifts and the rank.
  - When running locally, the variable OWLCMS_STYLESDIR indicates what directory contains the style sheets.  The default value is "styles". This makes it easier to create and test alternate visual styles.
- Records display: The "show records for all categories in session" and "show records from all federations, eligible or not" checkboxes now do what they should.
- Made the Protocol template use the overall Rank for the Total, same as the Snatch-CJ-Total template (instead of the current session ignoring the previous ones).
- Small fix to prevent the countdown/decision display from randomly (and rarely) requiring a refresh.
- Added an extra column on the registration data export to show the athlete's weight class. This allows sorting athletes by weight class and entry total more easily when allocating athletes to groups.
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
