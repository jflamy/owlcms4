42.0.0-rc02

> *Feature freeze for release 42 - normally only fixes from this point onward*
> *Candidate for official release.  Like any version, you should still test with your own data before using in a meet.*

42.0.0

- Additional fix for 2 minute clock when announcer enters decisions
  - When using flags, unwarranted two minute clocks could be awarded due to the previous athlete being erroneously set. 

- Fix for the registration form behavior
  - If there was no body weight, and the selected registration category was unselected from the dropdown, the checkboxes showing the possible eligibility categories based on age and qualifying total were not shown.
- Interactive scoreboard layout changes
  - On the result scoreboards, you can change the width of the Team column using ⇦ and ⇨ (giving more space for the name) and you can change the font size using ⇧ and ⇩ .
  - If names are very long, you can get an abbreviated first name by adding abb=true to the URL parameters
  - (beta01) the team width and the name abbreviation settings can be set from the options dialog.
- Reorganized the menus for the weigh-in page, the registration page, and the pre-competition documents page.  The registration reports are now available in both the registration and pre-competition locations. It is now possible to edit athletes on all 3 pages.
- New pre-competition documents
  - Entry list by bodyweight category, showing the age groups in which the athlete is eligible
  - Team entry list, showing the age group teams in which the athlete is registered
  - Categories entry list, showing the competitor in each age group+body weight category
- The athlete editing form now shows, next to one another, the eligible categories and age group teams in which the athlete is entered.
- The final results package for total and snatch/clean&jerk/total competitions now includes a summary of medals on the team points page.
- The team membership page has been redone; it now shows warnings if there are more athletes than permitted in a team and if there are too many athletes in a single category.  The limits are set on the competition rules (default are IWF rules - 10 per team, 2 max per category). If the IWF rules don't apply, you can ignore the warnings or change the limits.
- The Clean & Jerk break now automatically starts at the end of the snatch.  
  - The duration can now be parameterized in the competition rules (longer breaks when very few athletes, or a shorter or no break for large sessions).
  - This can be disabled if some special event takes place during the break.
- Medal Ceremonies
  - The order of medal ceremonies in the drop-down menu for the announcer is now more recent session first
  - The video streaming display for medals now correctly follows when the announcer switches to another medal category.
- Announcer Button Label: added a textual label to the "Pause" button to facilitate understanding, and moved it to the right.
- Accept and display line breaks in the coach, custom1, and custom2 fields.  Currently, the only way to create such line breaks is to use Alt-Enter in Excel.

##### Highlights from recent stable releases

- Start lists
  - Added a Start List template that lists the eligible age groups, for use in multiple age group competitions.
  - Added a Start List template that lists the athletes according to their bodyweight class.  This is used in multi-age competitions where athletes of the same weight class compete together and are not separated by age group.
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
- We now recommend using [fly.io](https://owlcms.github.io/owlcms4-prerelease/#/Fly) as the cloud installation is straightforward and owlcms can be run for free. Heroku is now deprecated as they have broken the easy install method and are no longer free.
- The editing page used for athlete registration and weigh-in has been redone to be more readable and better organized.
- The jury members can now vote again during a deliberation break. The decision lights are reset when deliberation starts so the post-video vote is a secret vote. 
- A new site section has been added to start the displays used for video streaming (see the [streaming documentation](https://owlcms.github.io/owlcms4-prerelease/#/OBS?id=_2-setup-owlcms-with-some-data)). The video-oriented scoreboards have a different header with the event name and group description, and show different columns (by default, the same as used by IWF).


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_42.0.0-rc02.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_42.0.0-rc02.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the **[Fly.io](https://owlcms.github.io/owlcms4-prerelease/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)
