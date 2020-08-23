* Changes for release 4.10.0-alpha06  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))
  - [x] Translation: Excel results files (starting weights, group results, final package) are now generated in the current language (#273)
  - [x] Enhancement: Competition group schedule and officials now produced as part of the final results package (#382)
  - [x] Technical: tweaking the Kubernetes manifests (k8s.zip in the release)
  
* Key Highlights from recent stable releases

   - [x] Enhancement: groups can be defined using the registration spreadsheet.  Athletes, Platforms and Groups are now updated according to the spreadsheet, replacing prior content as needed. (#374)  Also, the athlete registration spreadsheet upload accepts birth dates in the current language and country format in addition to ISO 8601 yyyy-MM-dd format. (#380).  Error messages are now localized.
   - [x] Technical: sample Kubernetes deployment files in the owlcms-docker subproject now deploy both owlcms and publicresults working together (see owlcms-docker/src/main/assembly/k8s to use as starting point)

   * [x] Important Fix: Prior to release 4.9, some changes to requested weights would be shown immediately on the top part of the scoreboard, but delayed on the bottom part (#376)   Also other fixes to correctly show decision reversals (#378) and to show the down arrow when connecting refereeing devices to a scoreboard display (#375)
   * [x] Fix:  Pausing and resuming a break is again working; also, the "before introduction" timer automatically switches to the the "introduction" pause.
   * [x] Enhancement: Round-robin lifting order now selectable on Competition Information page. When selected, all lifters do their first attempt according to requested weight,  then all lifters do the second, and so on (#367)
   * [x] Enhancement: Timer, down and decisions are now shown on the `publicresults` site.  Documentation for running virtual competitions and for using remote scoreboards using `publicresults` has been updated.  See the `Advanced Topics` section of the documentation. (#352) (#362)
   * [x] Enhancement: CSS stylesheets for scoreboards now editable in local/styles (#365)
   * [x] Enhancement: updated the bundled Java version to current long-term support version (JDK11).
   * [x] Enhancement: Technical configuration parameters for cloud-based usage no longer requires using environment variables.  A new `Technical Configuration` button is available on the `Prepare Competition`page (#361)
   * [x] Fix: Warning messages for out-of-sequence actions (stopping time when not started, starting time when decisions still visible, etc.) now more visible and have been translated to natural language (#359)
   * [x] Enhancement: Timekeeper screen now with large buttons, can be used from iPad/iPhone etc.  Also supports "," as keyboard shortcut to start clock and "." to stop clock for use with programmable keypads. (#340) 
   * [x] Enhancement: When using refereeing devices, announcer sees decisions as they come in. Useful when there is no jury to remind referee or to detect device/network faults. (#328)  Can be turned off on the Competition Information page.
   * [x] Enhancement: Team Results page accessible from the "Result Documents" navigation page. 
     * [x] Shows the team points scored by each team (using the IWF scoring 28-25-24 as given at end of group) (#336)   
     * [x] Also includes the Sinclair total for the team, reflecting lifts done (#337)
   * [x] Enhancement: Team Sinclair Scoreboard for top 5 teams.  Updated on every lift. (#337)
   * [x] Enhancement: Simple team competition scoreboard.  Top 5 men and top 5 women teams are displayed. Shows how many athletes have been tallied and full team size. (#327)
   * [x] Enhancement: The final package page contains all the scores (SMM, Sinclair, Robi) same as the group results page.
   * [x] Enhancement: In order to facilitate video streaming overlays (e.g. with OBS Studio), the URL http://my.address:8080/displays/currentathlete gives a special screen with only the current athlete shown on the scoreboard.  Edit the file local/styles/currentathlete.css to customize the look and feel. (#312)

* Workarounds/Known Issues
  
  - [ ] Workaround: (#304) When the browser screens have already been started and owlcms is restarted,  occasionnaly a refresh of the page may be required.

Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the ([Heroku Cloud Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/Cloud) to deploy your own copy.

  - For **Kubernetes** deployments, download the `k8s.zip` file.   You will need to unzip the file and adapt it for your specific cluster and host names. The zip contains a README file with guidance on how to proceed. 
