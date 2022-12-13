> New numbering scheme.  First level = significant features that can affect how a competition is run.  Second level = smaller features such as user interface improvements or technical changes.  Third level = bug fixes.

- 35.1: Improvement of registration sheet behavior ([#594](https://github.com/jflamy/owlcms4/issues/594), [#595](https://github.com/jflamy/owlcms4/issues/595))
  - 35.1 Enhancements / functional changes
    - [Registration Spreadsheet Documentation](https://owlcms.github.io/owlcms4-prerelease/#/Registration): the recommended way to enter categories when initially loading the athletes has been clarified and documented. Documentation of the export conventions for category eligibility and team membership.
    - 35.1.0-rc02: Added capability to use a VLOOKUP formula in the competition book team reasults template. See [example](https://github.com/jflamy/owlcms4/raw/develop/owlcms/src/test/resources/templates/competitionBook/EC-SnatchCJTotal-A4.xls). The formulas for points are in cell K12 of the MCT, WCT and MWCT tabs. The points are in the tab named "Points"
  - 35.1 Fixes:
    - Fix: Using the registration spreadsheet to enter the athletes did not make them team members by default.  The import/export process did not preserve team membership changes ([#594](https://github.com/jflamy/owlcms4/issues/594))
    - Fix: Entering the expected bodyweight of an athlete on the registration sheet was no longer inferring the eligibility categories ([#595](https://github.com/jflamy/owlcms4/issues/595))
    - Fix: Categories entered on the registration sheet are now always checked relative to the qualification criterion.

##### Highlights from recent stable releases

- **New Sinclair coefficients for the 2024 Olympiad**.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.
- Experimental capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://owlcms.github.io/owlcms4-prerelease/#/FlagsPicture) documentation.
- New Records processing, including Eligibility Criteria and Federations (so invited athletes do not show up as breaking local records, etc.). See [Records Eligibility](https://owlcms.github.io/owlcms4-prerelease/#/Records) documentation. 
- New Weigh-in template to create a Weigh-in Summary. Meant to facilitate data entry. As used in Pan-American federation, the body weight and declarations are copied one per line and countersigned by the coach/athlete. Select the template from the "Starting Weights" button on the weigh-in page.
- [Customization](https://owlcms.github.io/owlcms4-prerelease/#/UploadingLocalSettings) of colors and styling of scoreboards and attempt board. 
- Improved management of ceremonies : see [Breaks and Ceremonies](https://owlcms.github.io/owlcms4-prerelease/#/Breaks) procedures, and [Result Documents](https://owlcms.github.io/owlcms4-prerelease/#/Documents) for the medals spreadsheet.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_35.1.0-rc02.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_35.1.0-rc02.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the **[Heroku](Heroku) **or **[Fly.io](Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes]()

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://owlcms.github.io/owlcms4-prerelease/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
