* Changes for release ${revision}  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))

  - [x] Skipping tests is now controlled explicit parameter visible in the Azure DevOps interface, and is off by default (tests are run again during both prerelease and release builds)
  
* Changes for previous 4.19 pre-releases

  - [x] Added additional tests to "who lifted first on previous attempt" when requesting same weight, same attempt
  - [x] Portuguese translation was missing.

  * [x] Implemented the rules to prevent athletes from moving down their requested weight illegally.  Moving down is only allowed if the requested weight does not cause the athlete to lift out of order (i.e. moving is denied if the athlete should have lifted the weight earlier according the the rules, and is therefore gaining unfair recovery time)
    - An athlete cannot move to a weight smaller than an already started clock, or an already lifted weight (bar weight does not go down except to correct marshalling or announcing errors)
    - If moving to a value for an already started clock or an already attempted weight
      - If an athlete previously lifted the requested weight and did so on an earlier attempt, then the moving athlete cannot move (at a given weight, cannot take attempt 2 before attempt 1)
      - If an athlete previously lifted the requested weight and did so on the same attempt, then the moving athlete must have lifted later (taken a larger bar on the previous attempt - smaller progression)
      - If an athlete previously lifted the requested weight and did so on the same attempt with the same previous weight, then the moving athlete must have  larger start number (for example, on a first attempt start 1 cannot lift after start 2 by moving up, then moving down after start 2 has lifted)
    - Because sometimes there is confusion when entering weights -- similar names, fatigue, etc., the rules can be waived.
    - Note: The rules are not applied for mixed children groups with "all girls before all boys" (since the first boy will likely request less than the last girl)
  * [x] For cloud-based competitions, setting the time zone can now be done from within the application instead of requiring an environment variable. (#422)
  * [x] The Kubernetes setup files were missing an annotation that was preventing generation of the https certificate. Also updated cert-manager to the current version.

* Key Highlights from recent stable releases

  - [x] A request for a weight below what that was loaded on last clock start will be blocked (#234).  This prevents accidental typing from messing up the lifting order (ex: typing 87 instead of 97). The other rules for moving down will be enforced automatically will be added later in upcoming releases.
    - [x] a problem with entering weights at weigh-in was fixed in version 4.18
  - [x] Marshall can no longer edit or overwrite lift results by mistake. An explicit checkbox is required to enable edit (#286)
  - [x] Solo mode where a single technical official uses the good lift/bad lift buttons on the announcer screen now correctly supports decision reversal within 3 seconds, and correctly ignores multiple clicks. (#281)
  - [x] iPads now supported as refereeing device with Bluetooth buttons (running either the athlete-facing time+decision display or the attempt board display.)   Sound is enabled by touching a screen button once when the board is started. (#408). Note that iPads may lag by a second or two compared to other devices (will be worked around in a future release)
  - [x] Reorganized documentation pages for setting up Virtual Competitions.  Documented use of Zoom and OBS for videoconferencing and live broadcast streaming. Added introductory videos to the documentation, accessed from the home page.  Updated the Refereeing section to cover Bluetooth keypads.  Added instructions for using a cheap Fire TV Stick as a display device.
  - [x] Support for large competitions on Heroku. Added documentation for [economical use of Heroku professional tiers](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/HerokuLarge). Heroku now provides the memory defaults for all configurations.
    If you are limited to using the free setup and need to stretch it to its maximum, set the `_JAVA_OPTIONS` configuration variable to something like `-Xmx384m -XX:MaxMetaspaceSize=80m`
  - [x] New: added a new item for video broadcasts in the technical configuration section. Video capture using OBS or similar streaming software is awkward when a PIN or password is set.  If it is known that the video operator is working from a safe setting (such as a home network) , a "backdoor" setting (OWLCMS_BACKDOOR if using an environment variable) can be used to allow password-less login from a comma-separated list of addresses.  Use with care.
  - [x] Fix: The Officials tab on the Start List spreadsheet now includes the weigh-in officials for the group.
  
  * [x] Improvement: New scoreboard with multiple IWF age group rankings (Youth, Junior, Senior).  Final package also includes the three rankings. (#372)


Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the ([Heroku Cloud Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean) or `k3d_setup.yaml` for [home hosting](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/k3d).  For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
