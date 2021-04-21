* Changes for release 4.18.0  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))

  * [x] Fixed sizing of referee decision lights on jury console (#418)
  * [x] A request for a weight below what that was loaded on last clock start will be blocked (#234).  This prevents accidental typing from messing up the lifting order (ex: typing 87 instead of 97). The other rules for moving down will be enforced automatically will be added later in upcoming releases.
  * [x] Marshall can no longer edit or overwrite lift results by mistake. An explicit checkbox is required to enable edit (#286)
  * [x] Solo mode where a single technical official uses the good lift/bad lift buttons on the announcer screen now correctly supports decision reversal within 3 seconds, and correctly ignores multiple clicks. (#281)
  * [x] Announcing using iPad is now easier.  The font size on the top row and the width of decision lights area were made smaller to accommodate regular iPads. (#412)
  * [x] Referee lights are now correctly shown to the announcer when the timekeeper handles the clock (#411)
  * [x] Removed code to compensate for slow timer start on iPad (#414).  Intermittent issues on unrelated setups were observed.

* Key Highlights from recent stable releases

  - [x] iPads now supported as refereeing device with Bluetooth buttons (running either the athlete-facing time+decision display or the attempt board display.)   Sound is enabled by touching a screen button once when the board is started. (#408)
  - [x] Reorganized documentation pages for setting up Virtual Competitions.  Documented use of Zoom and OBS for videoconferencing and live broadcast streaming. Added introductory videos to the documentation, accessed from the home page.  Updated the Refereeing section to cover Bluetooth keypads.  Added instructions for using a cheap Fire TV Stick as a display device.
  - [x] Support for large competitions on Heroku. Added documentation for [economical use of Heroku professional tiers](https://owlcms.github.io/owlcms4/#/HerokuLarge). Heroku now provides the memory defaults for all configurations.
    If you are limited to using the free setup and need to stretch it to its maximum, set the `_JAVA_OPTIONS` configuration variable to something like `-Xmx384m -XX:MaxMetaspaceSize=80m`
  - [x] New: added a new item for video broadcasts in the technical configuration section. Video capture using OBS or similar streaming software is awkward when a PIN or password is set.  If it is known that the video operator is working from a safe setting (such as a home network) , a "backdoor" setting (OWLCMS_BACKDOOR if using an environment variable) can be used to allow password-less login from a comma-separated list of addresses.  Use with care.
  - [x] Fix: The Officials tab on the Start List spreadsheet now includes the weigh-in officials for the group.
  
  * [x] Improvement: New scoreboard with multiple IWF age group rankings (Youth, Junior, Senior).  Final package also includes the three rankings. (#372)


Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://owlcms.github.io/owlcms4/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the ([Heroku Cloud Installation](https://owlcms.github.io/owlcms4/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://owlcms.github.io/owlcms4/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://owlcms.github.io/owlcms4/#/DigitalOcean) or `k3d_setup.yaml` for [home hosting](https://owlcms.github.io/owlcms4/#/k3d).  For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
