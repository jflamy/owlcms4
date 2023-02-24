38.0.0-beta01  Support for video displays with specific styling

- 38.0 Enhancements
  - A new site section has been added to start the displays used for video streaming (see the [streaming documentation](https://owlcms.github.io/owlcms4-prerelease/#/OBS?id=_2-setup-owlcms-with-some-data)). The video-oriented scoreboards have a different header with the event name and group description, and show different columns (by default, the same as used by IWF).
    - The video pages can be changed to have a different look than the on-site displays. They have their own style sheets in `local/styles/video`. The [Style Customization](https://owlcms.github.io/owlcms4-prerelease/#/Styles) instructions can be used to change the colors, what columns are visible, to include logos, etc.
  - The medals and rankings video displays can be controlled from the video streaming page. When on-site scoreboards show a group that has been presented and is warming up, the video displays can be switched independently to show a previous group or category for medal presentations.
    - The video display tracks  
  - New MQTT messages.  A device driver can query what platforms are configured in owlcms using `owlcms/config` message.  Reply comes in a `owlcms/fop/config` message as a JSON object. Other items such as the owlcms version may be included.
- 38.0.0-beta01 Fixes:
  - Video medals and rankings displays correctly switch when a group/category is selected on video page 
  - Medals and rankings pages now refresh after lifts or changes; for video purposes it is sufficient to open with fop= without group or category.
  - Show leaderboard for snatch until first total is available, then switch to leaderboard for total.
- 38.0 Fixes
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

  - For **Windows**, download `owlcms_setup_38.0.0-beta01.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_38.0.0-beta01.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the [Heroku](https://owlcms.github.io/owlcms4-prerelease/#Heroku) or (recommended) **[Fly.io](https://owlcms.github.io/owlcms4-prerelease/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes](https://owlcms.github.io/owlcms4-prerelease/#/DigitalOcean)
