38.0.0  Support for video displays with specific styling

- 38.0 Enhancements
  - Experimental: A separate site section has been added to manage the displays used for video streaming. The video displays now have their own style sheets. 
    - The video-specific style sheets are in the `local/styles/video`. By default, the color scheme is the same as the site pages, with slight differences in the content shown (to match the columns used on IWF broadcasts). A Web Designer (or a motivated person) can use the [Style Customization](https://owlcms.github.io/owlcms4-prerelease/#/Styles) instructions to change the colors, what columns are visible, to include logos, etc.
  - Experimental: A current "video group" and "video category" can be set for medals and ranking pages.
    - A "video group" and a "video category" are linked to the current platform.  If the medals video page is opened without explicit group and category parameters, the "video group" and "video category" selected on the video page are used. In this way, there is no need to manipulate the streaming software.
    - Example: Group H1 has been presented and is warming up.  Medal ceremony for the categories of group F1 starts. To use F1 for the video screens, the "video group" can be set to F1  and the "video category" can be used if there are several categories in a group and it is desired to show them one by one. 
  - New MQTT messages.  A device driver can query what platforms are configured in owlcms using `owlcms/config` message.  Reply comes in a `owlcms/fop/config` message as a JSON object. Other items such as the owlcms version may be included.
- 38.0.0-alpha03 Fixes:
  - Medals and rankings page now refresh after lifts or changes; for video purposes it is sufficient to open with fop= without group or category.
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

  - For **Windows**, download `owlcms_setup_38.0.0-alpha03.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_38.0.0-alpha03.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the [Heroku](https://owlcms.github.io/owlcms4-prerelease/#Heroku) or (recommended) **[Fly.io](https://owlcms.github.io/owlcms4-prerelease/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes](https://owlcms.github.io/owlcms4-prerelease/#/DigitalOcean)
