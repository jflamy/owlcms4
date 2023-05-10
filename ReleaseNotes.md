38.5 Minor enhancements

- 38.5 Changes
  - 38.5.2: By default, the "Medals" display now updates along with the current group of the current platform
  - 38.5.2: Non standard bodyweight categories now accepted as IWF records, for convenience (typically used to add YTH records).  Also, the upper bound of a category can now be written using either `>` or `+` as the first character (for example `>87`  or `+87`)
  - 38.5.1: On the start list, if weigh-in times are not provided, the natural order of group names will be used to order the groups (Group 8 will come before Group 15 even though the alphabetical order says otherwise).
  - 38.5.1: When using the "Competition Results" section to get the results for a specific category  the new records for that age group/weight class are shown in the report.
    - Note: this only applies to competitions run using 38.5.1 or later; this feature required additional information to be stored about the record attempts taking place during a meet.
  - 38.5: When using the "Competition Results" section to get the results for a specific category  the new records for that session are shown.  
  - 38.5: Also added a record sheet on the final package competition book.
  - Jury 1 is now translated as Jury President on the Groups page and the various schedules/reports
  - The down sound can now to be turned off separately from the clock warnings.
    - If using a down signal light equipped with a buzzer, the down signal sound from the countdown displays is redundant and potentially confusing.  
    - Added a MQTT message `owlcms/fop/timeRemaining` that is emitted at 90, 30, 0 seconds remaining for the various warnings.
  - Athlete Challenge
    - Added a Challenge button to the Jury dialog, and a matching keyboard shortcut (the `c` key on the keyboard). 
  
    - This displays "Challenge" on the attempt board as required by TCRR and also changes the video status monitor to BREAK.CHALLENGE.
  
    - Added a MQTT message `owlcms/jurybox/challenge` which is the same as the deliberation message, but triggers a BREAK.CHALLENGE instead
  - Fix: Weigh-in screen could fail to load if an athlete had been registered without a category or age group.
  - Fix: USAW Results template fixed to correctly match Open M and Open F names
  - Fix: Platform selection would be reset to first available after editing a group 


##### Highlights from recent stable releases

- We now recommend using [fly.io](https://owlcms.github.io/owlcms4-prerelease/#/Fly) as the cloud installation is straightforward and owlcms can be run for free. Heroku is now deprecated as they have broken the easy install method and are no longer free.
- The editing page used for athlete registration and weigh-in has been redone to be more readable and better organized.
- The jury members can now vote again during a deliberation break. The decision lights are reset when deliberation starts so the post-video vote is a secret vote. 
- A new site section has been added to start the displays used for video streaming (see the [streaming documentation](https://owlcms.github.io/owlcms4-prerelease/#/OBS?id=_2-setup-owlcms-with-some-data)). The video-oriented scoreboards have a different header with the event name and group description, and show different columns (by default, the same as used by IWF).


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_38.5.2-rc01.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_38.5.2-rc01.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the **[Fly.io](https://owlcms.github.io/owlcms4-prerelease/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)
