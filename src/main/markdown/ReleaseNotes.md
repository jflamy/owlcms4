> New numbering scheme.  First level = significant features that can affect how a competition is run.  Second level = smaller features such as small user interface improvements or technical changes.  Third level = bug fixes.

- 37.0.0-beta: **New version after migration to Vaadin 23.**

  All features from version 35 (which used Vaadin 14) appear to work.

  - 37.0 Enhancements
    - Migration to new version of the user interface framework
    - On the preparation page, moved the pre-competition documents (Start List, Cards) to a separate page with more appropriate filtering.
    - beta09: Edit Athlete Entries: updated the category selection logic to make initial entry of athletes (+) more intuitive
    - beta10: main screen shows the group description at the top if one is available
    - beta10: attempt board and decision board now show a "waiting for next group" instead of blank.
  - 37.0-beta Fixes
    - beta10: The decision display for TV (public-facing) was showing decision lights in the reverse order.  The public-facing attempt board was correct.
    - beta08: enhanced documentation for physical setup and networking options
    - beta08: changed the multichrome.bat script to use Edge instead of Chrome (uses much less memory)
    - beta07: the public results scoreboard was not updating
    - beta06: updated build to create Heroku version
    - beta04: jury could give a premature decision while lights were still on; jury break is now required.
    - beta03: made the referee summoning keyboard shortcuts work again on the jury page ([#603](https://github.com/jflamy/owlcms4/issues/603)); also fixed phone/tablet refereeing so they obey "summon all" correctly
    - beta03: the web page names are now the same as in version 35 (the leading "n" has been removed)
    - beta03: checked that all page sizes in the final package and pre-competition documents are correct ([#605](https://github.com/jflamy/owlcms4/issues/605))
    - beta03: the current language is now indicated explicitly in the html response ([#604](https://github.com/jflamy/owlcms4/issues/604))
    - beta03: fixed wrong column heading translation string in Weigh-In summary template.

##### Highlights from recent stable releases

- Customization of team points. Templates for the final results package now have an extra tab that contains the points awarded for each rank. Copy and rename the template if you need to change the point system for a given competition.
-  Improvement of registration spreadsheet.  See [Registration Spreadsheet Documentation](https://owlcms.github.io/owlcms4-prerelease/#/Registration)
- **New Sinclair coefficients for the 2024 Olympiad**.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.
- Experimental capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://owlcms.github.io/owlcms4-prerelease/#/FlagsPicture) documentation.
- New Records processing, including Eligibility Criteria and Federations (so invited athletes do not show up as breaking local records, etc.). See [Records Eligibility](https://owlcms.github.io/owlcms4-prerelease/#/Records) documentation. 
- New Weigh-in template to create a Weigh-in Summary. Meant to facilitate data entry. As used in Pan-American federation, the body weight and declarations are copied one per line and countersigned by the coach/athlete. Select the template from the "Starting Weights" button on the weigh-in page.
- [Customization](https://owlcms.github.io/owlcms4-prerelease/#/UploadingLocalSettings) of colors and styling of scoreboards and attempt board. 
- Improved management of ceremonies : see [Breaks and Ceremonies](https://owlcms.github.io/owlcms4-prerelease/#/Breaks) procedures, and [Result Documents](https://owlcms.github.io/owlcms4-prerelease/#/Documents) for the medals spreadsheet.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_37.0.0-alpha00.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_37.0.0-alpha00.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the **[Heroku](Heroku) **or **[Fly.io](Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes]()

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://owlcms.github.io/owlcms4-prerelease/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
