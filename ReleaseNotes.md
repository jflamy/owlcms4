* Changes for release 4.19.1-rc04  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))

  * [x] 4.19.1-rc01 Error message for 20kg rule was broken in release 4.19.0. Fixed.
  * [x] 4.19.1-rc02 Fixed an exception when writing to the log file if no start number was assigned to an athlete.
  * [x] 4.19.1-rc04 Added a local/logback.xml file to the default install so it is easier to change the logging levels when troubleshooting.
  
* Key Highlights from recent stable releases

  - [x] Implemented the rules to prevent athletes from moving down their requested weight illegally.  Moving down is denied if the athlete should already have attempted that weight according to the official lifting order.  The exact checks resulting from applying the TCRR to that situation are spelled out in the [documentation](https://jflamy-dev.github.io/owlcms4-prerelease/#/Announcing#rules-for-moving-down). (#418)
  - [x] For cloud-based competitions, setting the time zone can now be done directly from the Competition Information page. (#422).
  - [x] Marshall can no longer edit or overwrite lift results by mistake. An explicit checkbox is required to enable edit (#286)
  - [x] Solo mode where a single technical official uses the good lift/bad lift buttons on the announcer screen now correctly supports decision reversal within 3 seconds, and correctly ignores multiple clicks. (#281)
  - [x] iPads now supported as refereeing device with Bluetooth buttons (running either the athlete-facing time+decision display or the attempt board display.)   Sound is enabled by touching a screen button once when the board is started. (#408). Note that iPads may lag by a second or two compared to other devices (will be worked around in a future release)
  - [x] Reorganized documentation pages for setting up Virtual Competitions.  Documented use of Zoom and OBS for videoconferencing and live broadcast streaming. Added introductory videos to the documentation, accessed from the home page.  Updated the Refereeing section to cover Bluetooth keypads.  Added instructions for using a cheap Fire TV Stick as a display device.
  - [x] Support for large competitions on Heroku. Added documentation for [economical use of Heroku professional tiers](https://jflamy-dev.github.io/owlcms4-prerelease/#/HerokuLarge). Heroku now provides the memory defaults for all configurations.
  If you are limited to using the free setup and need to stretch it to its maximum, set the `_JAVA_OPTIONS` configuration variable to something like `-Xmx384m -XX:MaxMetaspaceSize=80m`
  - [x] New: added a new item for video broadcasts in the technical configuration section. Video capture using OBS or similar streaming software is awkward when a PIN or password is set.  If it is known that the video operator is working from a safe setting (such as a home network) , a "backdoor" setting (OWLCMS_BACKDOOR if using an environment variable) can be used to allow password-less login from a comma-separated list of addresses.  Use with care.
  
  * [x] Improvement: New scoreboard with multiple IWF age group rankings (Youth, Junior, Senior).  Final package also includes the three rankings. (#372)


Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the ([Heroku Cloud Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://jflamy-dev.github.io/owlcms4-prerelease/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://jflamy-dev.github.io/owlcms4-prerelease/#/DigitalOcean) or `k3d_setup.yaml` for [home hosting](https://jflamy-dev.github.io/owlcms4-prerelease/#/k3d).  For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
