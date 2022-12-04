> New numbering scheme.  First level = significant features that can affect how a competition is run.  Second level = smaller features such as user interface improvements or technical changes.  Third level = bug fixes.

- 35.0.0-rc:

  *Release Candidate.  Has been used in meets.*

  - Enhancements / functional changes

    * Initial capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/FlagsPicture) documentation.
    * New Records Eligibility Criteria based on Age Groups and Federations (so invited athletes do not show up as breaking local records, etc.). See [Records Eligibility](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Records#eligibility-criteria) documentation. 
    * Timekeeper can now switch groups and start countdowns. Useful when a veteran announcer is using the scoreboard only.
    * Notification given to TOs if current athlete does not meet starting weights rule when called for first clean and jerk (TCRR regulation to rule 6.6.5 clause 6) (#556)
    * Clicking on the break button no longer starts a technical or marshal break immediately. It is possible to cancel, or to change the kind of break desired. The ▶ button must be used to start the break. Also, the Marshall break button is labeled "Marshal Issue" to make it's purpose clearer. (#569)
    * Feature toggles to enable new features that may still evolve.  See [Feature Toggles](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/FeatureToggles) for documentation.
      * Added a feature toggle `forceAllGroupRecords`.  If true, the scoreboards will show all the records that can be broken in the current group (as opposed to just those for the current athlete, which is the default.) This works if there is less than about 6 age groups in the group (zooming and adjusting the font size may be required)
      * Added a feature toggle `preCompDocs`.  If defined, the download buttons for pre-competition documents (start list and athlete card) are moved on a new separate page.  This allows more appropriate filtering (for example, producing different start lists for different competitions if there are joint meets, or a schedule for each platform)
    
    * Improved ordering of the registration export page to make it easier to do the initial allocation to groups when there are multiple age groups and there is a need to create A and B groups.
    * In multi-lingual settings, a drop down at the top of navigation pages allows changing the language for the current browser. Pages and displays opened from that browser will be in the new language; the overall default is not changed.  (#553)
    * Full support of MQTT messaging for devices. Jury/referee/timekeeper devices can now issue MQTT commands instead of key presses, and subscribe to messages issued by the main program (#469)  See  [MQTT Messages documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/MQTTMessages).
    
    - Improved colors on the Current Athlete view used when streaming (current requested weight are more visible, empty cells are less visible) (#562)
    
  - Fixes:
  
    - 35.5.0-rc03: Fix: Heroku Deploy button should work again - adjusted the specifications after removal of free plans (#588)
    - 35.5.0-rc03: Fix: No blinking, leaders or records shown during introduction of athletes (#587)
    - 35.5.0-rc02: Fix: Pre-competition documents now work in the cloud (Postgres is stricter than H2) (#582)
    - 35.5.0-rc02: Fix: Platforms are now reloaded correctly and also registered to MQTT after importing a JSON database file (#579)
    - Fixed the Weigh-In Summary document available from the Starting Weights button on the weigh-in page.  The document was not being produced (#578)
    - Fixed a race condition that could cause 404 errors when trying to download a file, or silently required a second click to work. (#574)
    - 35.0.0-beta11: Fixed spurious error message if clock started when already running (#575)
    - Fixed a vulnerability in processing the passwords. An error message is given at startup if the faulty encoding is detected, asking users to change the password (#574)
    - Fixed premature notifications to technical officials for events that were in fact forbidden (#570)
    - Final Package was including SMF and Sinclair results for all athletes, ignoring the filtering requested. (#561)
    - On mobile refereeing screen, reversal was not restoring button colors (#560)
    - Record attempt notification remained on attempt board at end of group (#557)

##### Highlights from recent stable releases

- Experimental capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/FlagsPicture) documentation.
- New Records processing, including Eligibility Criteria based on Age Groups and Federations (so invited athletes do not show up as breaking local records, etc.). See [Records Eligibility](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Records) documentation. 
- New Weigh-in template to create a Weigh-in Summary. Meant to facilitate data entry. As used in Pan-American federation, the body weight and declarations are copied one per line and countersigned by the coach/athlete. Select the template from the "Starting Weights" button on the weigh-in page.
- New Sinclair coefficients for the 2024 Olympiad.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.
- [Customization](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/UploadingLocalSettings) of colors and styling of scoreboards and attempt board. 
- Improved management of ceremonies : see [Breaks and Ceremonies](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Breaks) procedures, and [Result Documents](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Documents) for the medals spreadsheet.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_${revision}.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the **[Heroku](Heroku) **or **[Fly.io](Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes]()

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
