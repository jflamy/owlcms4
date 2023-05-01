38.5 Minor enhancements

- 38.5 Changes
  - beta04: starting list and other reports would fail if there were no records
  - beta03: translations
  - beta02: Added Records section on the results sheet for each group, and added a record sheet on the final package competition book.
  - beta02: Jury 1 is translated as Jury President on the Groups page and the various schedules/reports
  - beta01: Fix:  USAW Results template to correctly match Open M and Open F names
  - beta01: Fix: Platform selection would be reset to first available after editing a group 
  - If using a down signal light equipped with a buzzer, the down signal sound from the countdown displays is redundant and potentially confusing.  
    - The down sound can now to be turned off separately from the clock warnings.
    - Added a MQTT message `owlcms/fop/timeRemaining` that is emitted at 90, 30, 0 seconds remaining for the various warnings.
  - Fix: Weigh-in screen could fail to load if an athlete had been registered without a category or age group.
  - Athlete Challenge
    - Added a Challenge button to the Jury dialog, and a keyboard shortcut (C). 
  
    - This displays "Challenge" on the attempt board as required by TCRR and also changes the video status monitor to BREAK.CHALLENGE.
  
    - Added a MQTT message `owlcms/jurybox/challenge` which is the same as the deliberation message, but transitions to a BREAK.CHALLENGE state instead of a BREAK.JURY state.


##### Highlights from recent stable releases

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
