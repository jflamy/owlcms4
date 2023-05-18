39. 0 Records Management

- 39.0 Changes

  - 39.0.0-beta01: Danish, Portuguese, Hungarian, Romanian, Russian translation updates.
    
  - A new Records Management page has been added, reachable from the preparation page.
    - Record definition files can be uploaded interactively 
      - Loading a file replaces the previous information from the same file.  We suggest you change the file names when you update the records, and that you clear previous versions explicitly using the buttons provided.
      - If record definition files are found in `local/records`, they are  loaded on startup. Beware that each file will replace what was previously loaded or uploaded under that same file name.
    - The ordering of the records on the scoreboard is no longer dependent on the file names, and is edited interactively.

  - Fix: Now able run simulations on fly.io (adjusted the parsing of the forwarding information)
  
  - Fix: The lifting order screen would occasionally show a stretched athlete line instead of blank space

##### Highlights from recent stable releases

- Athlete challenge is now displayed and supported by the MQTT jury device.
- We now recommend using [fly.io](https://owlcms.github.io/owlcms4-prerelease/#/Fly) as the cloud installation is straightforward and owlcms can be run for free. Heroku is now deprecated as they have broken the easy install method and are no longer free.
- The editing page used for athlete registration and weigh-in has been redone to be more readable and better organized.
- The jury members can now vote again during a deliberation break. The decision lights are reset when deliberation starts so the post-video vote is a secret vote. 
- A new site section has been added to start the displays used for video streaming (see the [streaming documentation](https://owlcms.github.io/owlcms4-prerelease/#/OBS?id=_2-setup-owlcms-with-some-data)). The video-oriented scoreboards have a different header with the event name and group description, and show different columns (by default, the same as used by IWF).


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_39.0.0-beta01.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_39.0.0-beta01.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the **[Fly.io](https://owlcms.github.io/owlcms4-prerelease/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)
