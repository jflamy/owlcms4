38.1.0-alpha00  Additional attempt board information

- 38.1 Enhancements
  - The editing page for athlete registration and weigh-in has been redone to be more readable and better organized.
  - It is now possible to add personal bests for athletes, which are displayed along with records. Personal bests are for information and not highlighted like official records.
  - The attempt board now shows the athlete category and the lift type.  If you only want to see the attempt number, leave the `AttemptBoard_lift_attempt_number` translation empty.
  - The announcer "refresh list" button has been renamed to "Reload Group". It reloads the group completely (same as exiting and re-entering the group). This also sends a refresh signal to all displays. This is useful if for some reason it is necessary to edit athlete registration information.
  - The gender letters used for displaying *age groups* and *categories* are no longer fixed to `M` and `F` and can now be translated (for example, Germany could choose to use U17 D instead of U17 F)
- 38.1 Fixes
  - 38.1.0-alpha04: Fixed edge cases when all records were excluded for an invited athlete
  - 38.1.0-alpha03: Translated gender letters for age groups and categories were not being used consistently on all displays.
  - 38.1.0-alpha02: A specific error message is given if the athlete's body weight is changed and the athlete does not meet the minimum entry total for the new category and is therefore not eligible.
  - 38.1.0-alpha01: the current attempt number was not being hidden correctly on the down signal.
  - The total of the last snatch athlete was being shown during the clean & jerk break on the "current athlete" video streaming footer.
- 38.0 Enhancements
  - A new site section has been added to start the displays used for video streaming (see the [streaming documentation](https://owlcms.github.io/owlcms4-prerelease/#/OBS?id=_2-setup-owlcms-with-some-data)). The video-oriented scoreboards have a different header with the event name and group description, and show different columns (by default, the same as used by IWF).
    - The video pages can be changed to have a different look than the on-site displays. They have their own style sheets in `local/styles/video`. The [Style Customization](https://owlcms.github.io/owlcms4-prerelease/#/Styles) instructions can be used to change the colors, what columns are visible, to include logos, etc.
  - The medals and rankings video displays can be controlled from the video streaming page. When on-site scoreboards show a group that has been presented and is warming up, the video displays can be switched independently to show a previous group or category for medal presentations.
    - The medals and rankings displays track the group chosen on the video page; the other displays track the group selected by the announcer or timekeeper.
  - New MQTT messages.  A device driver can query what platforms are configured in owlcms using `owlcms/config` message.  Reply comes in a `owlcms/fop/config` message as a JSON object. Other items such as the owlcms version may be included.
- 38.0 Fixes
  - Only athletes listed as team members are now shown in the final package team scores. This makes it obvious when team memberships have not been assigned.
  - Team memberships were cleared when reassigning categories after changing age group definitions.  When reassigning, athletes are included in the teams by default, it is less work to remove the extras.
  - In the final package, Tab names were not being translated for some derived languages (ex: fr_CA)
  - Re-enabled the publicresults remote scoreboard capability.
  - Leaders from previous B/C/D group were not shown on first athlete of next group  ([#633](https://github.com/jflamy/owlcms4/issues/633))
  - When using the `forceAllGroupRecords` feature switch to show records applicable to all participants in a group, the records were not being fetched correctly ([#634](https://github.com/jflamy/owlcms4/issues/634))



##### Highlights from recent stable releases

- The announcer and marshal screens show the 6 attempts and total for each athlete. ([#525](https://github.com/jflamy/owlcms4/issues/525))
- Capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://owlcms.github.io/owlcms4-prerelease/#/FlagsPicture) documentation.
- There is now a separate page for pre-competition documents. There are now separate sections for each purpose instead of multiple tabs. See [Pre-Competition Documents Documentation](https://owlcms.github.io/owlcms4-prerelease/#/2400PreCompetitionDocuments).
- Customization of team points. Templates for the final results package now have an extra tab that contains the points awarded for each rank. Copy and rename the template if you need to change the point system for a given competition.
- Improvements to Records eligibility. See [Records Eligibility](https://owlcms.github.io/owlcms4-prerelease/#/Records) documentation. 
- New Weigh-in template to create an empty Weigh-in Summary (used to facilitate data entry)
- New Sinclair coefficients for the 2024 Olympiad.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_38.1.0-alpha04.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_38.1.0-alpha04.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the [Heroku](https://owlcms.github.io/owlcms4-prerelease/#Heroku) or (recommended) **[Fly.io](https://owlcms.github.io/owlcms4-prerelease/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes](https://owlcms.github.io/owlcms4-prerelease/#/DigitalOcean)
