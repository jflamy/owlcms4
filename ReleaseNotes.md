* Changes for release 4.22.0-alpha05  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))

  - [x] Customizations done on a local laptop can be zipped and uploaded to a cloud-based setup.  (#366).
  
    To use, run on a laptop, edit the files in the local directory, zip and upload using the  Technical Configuration page accessible from the Preparation main menu. The uploaded zip is kept in the remote database and unzipped when the application starts. 
  
    This allows changing colors, translations, sounds and templates even when running in the cloud with no access to the local files.  Note that when editing the files in /local on your laptop, you should use the developer mode in your browser and disable your cache under the "Network" tab of your developer window.
  
* Key Highlights from recent stable releases

  - [x] Top of scoreboards was not updating to first athlete on break end (#431)
  - [x] Final results package had erroneous rankings for youth and juniors (#432)
  - [x] Timing stats spreadsheet correctly produced again, was empty (#430)When a display is first started, a dialog offers to enable warning sounds or not.  Warnings are silenced by default; they should normally be enabled on only one display per room, to avoid warnings coming from several directions. See the [documentation](https://jflamy-dev.github.io/owlcms4-prerelease/#/Displays#display-settings) for details (#407)
  - [x] Fix for registration file upload: empty group start and weigh-in times were causing a generic error message when uploading an .xlsx file instead of being ignored.
  - [x] Implemented the <u>rules to prevent athletes from moving down their requested weight illegally</u>.  Moving down is denied if the athlete should already have attempted that weight according to the official lifting order.  The exact checks resulting from applying the TCRR to that situation are spelled out in the [documentation](https://jflamy-dev.github.io/owlcms4-prerelease/#/Announcing#rules-for-moving-down). (#418)
  - [x] Violations of <u>rules for timing of declarations</u> (before initial 30 seconds), and for changes (before final warning) are now signaled as errors (#425, #426). Overriding is possible for officiating mistakes.
  - [x] CSS style sheets for attempt board and decision board are now editable in local/styles (#424)
  - [x] For cloud-based competitions, setting the time zone can now be done directly from the Competition Information page. (#422).
  - [x] Marshall can no longer edit or overwrite lift results by mistake. An explicit checkbox is required to enable edit (#286)
  - [x] Solo mode where a single technical official uses the good lift/bad lift buttons on the announcer screen now correctly supports decision reversal within 3 seconds, and correctly ignores multiple clicks. (#281)
  - [x] iPads now supported as refereeing device with Bluetooth buttons (running either the athlete-facing time+decision display or the attempt board display.)   iPads require that sound be enabled by touching a screen button once when the board is started. (#408). 
  - [x] Workaround applied for iPad unpredictable response time (from 0.1 to 3 sec. lag) when used as display. The iPad will "skip ahead" to the correct remaining time as soon as the start command is received, and "skip back" on a stop. Only applies to iPads, ignored by all other platforms. (#419) 
  - [x] Support for large competitions on Heroku. Added documentation for [economical use of Heroku professional tiers](https://jflamy-dev.github.io/owlcms4-prerelease/#/HerokuLarge). Heroku now provides the memory defaults for all configurations.
    If you are limited to using the free setup and need to stretch it to its maximum, set the `_JAVA_OPTIONS` configuration variable to something like `-Xmx384m -XX:MaxMetaspaceSize=80m`
  - [x] New: added a new item for video broadcasts in the technical configuration section. Video capture using OBS or similar streaming software is awkward when a PIN or password is set.  If it is known that the video operator is working from a safe setting (such as a home network) , a "backdoor" setting (OWLCMS_BACKDOOR if using an environment variable) can be used to allow password-less login from a comma-separated list of addresses.  Use with care.
  
  * [x] Improvement: New scoreboard with multiple IWF age group rankings (Youth, Junior, Senior).  Final package also includes the three rankings. (#372)


Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://jflamy-dev.github.io/owlcms4-prerelease/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://jflamy-dev.github.io/owlcms4-prerelease/#/DigitalOcean) or `k3d_setup.yaml` for [home hosting](https://jflamy-dev.github.io/owlcms4-prerelease/#/k3d).  For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
