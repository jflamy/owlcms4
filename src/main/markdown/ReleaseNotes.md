* Changes for release ${revision}  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))

  - [x] Added introductory videos to the documentation, accessed from the home page.  Updated the Refereeing section to cover Bluetooth keypads.
  - [x] Fixed regression that was causing server-side sounds not to work anymore (#410)
  - [x] Cleaned-up break management code to correct when a second person opens dialog while a break is under way.  Fixed break type calculation according to stage of competition group.
  - [x] Reorganized documentation pages for setting up Virtual Competitions.  Documented use of Zoom and OBS for videoconferencing and live broadcast streaming.
  - [x] iPads now supported as refereeing device (running either the athlete-facing time+decision display or the attempt board display.)   Sound is enabled by touching a screen button once when the board is started. (#408)  Special code to compensate for the fact that these devices are sometimes slow to react when clock is started.
  - [x] Improved accuracy of signal timing, compensating for the variable time it takes for timers to start on various devices (#406)
  - [x] When loading the registration file, leaving the Platform column empty would create platforms with no name and lead to faulty URLs.  Fixed.
  
  Key Highlights from recent stable releases
  
  - [x] Support for large competitions on Heroku. Added documentation for [economical use of Heroku professional tiers](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/HerokuLarge). Heroku now provides the memory defaults for all configurations.
    If you are limited to using the free setup and need to stretch it to its maximum, set the `_JAVA_OPTIONS` configuration variable to something like `-Xmx384m -XX:MaxMetaspaceSize=80m`
  - [x] Improvement: Better handling of double-clicking/double-tapping on the start timer buttons. (#405) In odd circumstances this could result in rogue beeps/buzzers.
  - [x] Fix: added missing button for the Current Athlete display. This display is normally used in video broadcasts (the display is then cropped and shown at the bottom of the screen to show current attempt information and the previous lifts and rank of the current athlete.)
  - [x] New: added a new item for video broadcasts in the technical configuration section. Video capture using OBS or similar streaming software is awkward when a PIN or password is set.  If it is known that the video operator is working from a safe setting (such as a home network) , a "backdoor" setting (OWLCMS_BACKDOOR if using an environment variable) can be used to allow password-less login from a comma-separated list of addresses.  Use with care.
  - [x] Fix: The publicresults application was switching to an empty scoreboard page on owlcms startup (#404).  Now properly shows the "Waiting for next group" banner.
  - [x] Fix: The Officials tab on the Start List spreadsheet now includes the weigh-in officials for the group.
  
  * [x] Improvement: New scoreboard with multiple IWF age group rankings (Youth, Junior, Senior).  Final package also includes the three rankings. (#372)
  

Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the ([Heroku Cloud Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/HerokuLarge).

  - For **Kubernetes** deployments, you can use `kubectl apply` on the `k3s_setup.yaml` file for k3s  (see [instructions](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean) ) or `k3d_setup.yaml` for home use using k3d with Docker Desktop (see [instructions](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/k3d) ).  For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
