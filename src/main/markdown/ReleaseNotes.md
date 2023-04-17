38.4 Minor enhancements

- 38.4.2 Fix
  - Fixed an obscure tie-break issue that would only arise if the competition secretary had forgotten to draw the lot numbers. ([#657](https://github.com/jflamy/owlcms4/issues/657))
  - Fixed a typo in the Fly installation command, added notes for adding the database if the initial install omitted to do so (Kenny Ng, [#658](https://github.com/jflamy/owlcms4/pull/658))
  
- 38.4 Fixes
  - Correct IP addresses now shown on home page for Linux and macOS.
  - Demo site moved to owlcms.fly.dev
  
- 38.3 Fixes
  - The new athlete editing dialog could mistakenly create a 0 declaration and a failed 0 kg lift for the first snatch and first clean & jerk.  Now fixed.
  - Download dialogs for pre-competition and results documents could fail randomly.  Reverted to the previous version of the UI dialog component due to a bug introduced in the user interface library.
  - If some athletes had not been assigned to a group, errors could occur on the preparation document page. Also, the start list and the athlete cards would fail when printing them for all athletes.
  - A team name with characters that cannot be used in file names (like "/" or "?") would cause errors on the attempt board if flag images were used.
  - [Fly.io](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Fly) cloud setup documentation updated to match their new updated v2 machines
- 38.2 Heroku deprecated
  - Heroku cloud installation is no longer recommended.
    - We now recommend using [fly.io](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Fly) as the cloud installation is straightforward and owlcms can be run for free (it is below their monthly minimum for processing a bill.)
    -  Heroku has broken the easy one-click installation process as it was used by owlcms.  The Heroku documentation has been updated to show the "official" command-line installation process.
- 38.1 Enhancements
  - The editing page used for athlete registration and weigh-in has been redone to be more readable and better organized.
  - It is now possible to add personal bests for athletes, which are displayed along with records. Personal bests are for information and not highlighted like official records.  Personal bests are read from and exported on the registration spreadsheets.
  - The attempt board now shows the athlete category and the lift type.  If you only want to see the attempt number, leave the `AttemptBoard_lift_attempt_number` translation empty.
  - The gender letters used for displaying *age groups* and *categories* are no longer fixed to `M` and `F` and can now be translated (for example, Germany could choose to use U17 D instead of U17 F)
  - The jury members can now vote again during a deliberation break. The decision lights are reset when deliberation starts so the post-video vote is a secret vote.  A new MQTT message (`fop/juryDeliberation`) has been added so devices know when to reset the jury lights.
  - The announcer "refresh list" button has been renamed to "Reload Group". It reloads the group completely (same as exiting and re-entering the group). This also sends a refresh signal to all displays. This is useful if for some reason it is necessary to edit athlete registration information.
- 38.1 Fixes
  - The total of the last snatch athlete was being shown during the clean & jerk break on the "current athlete" video streaming footer.

##### Highlights from recent stable releases

- A new site section has been added to start the displays used for video streaming (see the [streaming documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/OBS?id=_2-setup-owlcms-with-some-data)). The video-oriented scoreboards have a different header with the event name and group description, and show different columns (by default, the same as used by IWF).
- The announcer and marshal screens show the 6 attempts and total for each athlete. ([#525](https://github.com/jflamy/owlcms4/issues/525))
- Capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://owlcms.github.io/owlcms4-prerelease/#/FlagsPicture) documentation.
- There is now a separate page for pre-competition documents. There are now separate sections for each purpose instead of multiple tabs. See [Pre-Competition Documents Documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/2400PreCompetitionDocuments).
- Customization of team points. Templates for the final results package now have an extra tab that contains the points awarded for each rank. Copy and rename the template if you need to change the point system for a given competition.
- Improvements to Records eligibility. See [Records Eligibility](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Records) documentation. 
- New Weigh-in template to create an empty Weigh-in Summary (used to facilitate data entry)
- New Sinclair coefficients for the 2024 Olympiad.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_${revision}.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_${revision}.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the [Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#Heroku) or (recommended) **[Fly.io](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean)
