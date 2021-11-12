##### **Changes for release ${revision}**  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))

- [x] Enhancement: It is now possible to edit an athlete's results from the Category Results page, and to recompute the rankings.
- [x] Enhancement: show SMF ranking in Team Results (for Masters competitions)
- [x] Enhancement: select all button on Team Selection page to include all athletes in teams and then deselect non-members.
- [x] Fix: Team results calculation was ignoring the team membership status
- [x] Fix: Sinclair scoreboard was not updating as lifts were made or athlete cards modified.
- [x] Enhancement: Two custom fields are available on the registration/weigh-in forms and in the registration Excel file.  Typical use is for athlete status or other local usage (ex: a patronym). Usable as ${l.custom1} and ${l.custom2} in the Excel templates. The headers used are defined in the translation file under "Custom1.Title" and "Custom2.Title".  

###### New in this release

- [x] It is now possible to export, manipulate and reload the registration data (athletes, groups, referees) in Excel format.  

  - Exporting the previously loaded data, rearranging the groups, adding/deleting athletes, changing expected category and entry totals is now possible. Until final verification of entries, the Excel sheet can be used as authoritative list of participating athletes: reloading it erases and recreates the athletes and groups.
  - Only registration data is exported.  This does not export the lifts and requested changes.  The file should not be loaded after the competition has started as it recreates the athletes from scratch.
  - Note that the format has changed to reflect the fact that category allocation is now automatic - only the gender and expected weight of the athlete are used. You need to download a new empty template, or export existing data. 
  - The new format makes it easier to cut and paste athletes (no hidden columns). Both the empty sheet and the exported sheet are now translated in the current language.
  
- [x] The jury console now allows direct reversal/confirmation of lifts (#435, #427)  
  - The jury chief can confirm and reverse lifts directly and can ask the announcer to call the technical controller.  
  - Jury actions are shown to the other technical officials consoles to keep them informed.
  - Shortcuts are defined to support a jury keypad. See [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Refereeing#jury-console-keypad) for details
  
- [x] Enhancement: the Available Plates page can now be reached from the Field of Play/Platforms preparation page.  The button to reach this page has also been renamed (used to be "Technical Controller")

- [x] Enhancement: added a "Coach" field to the athlete registration and weigh-in forms.  Usable as ${l.coach} in the Excel templates.

- [x] Fix: if you had imported the registration Excel, there was the possibility that duplicate platforms had been created.  The program now keeps the oldest platform when there are duplicates.

- [x] It is now possible to Export and Import the database content (#449).  This allows taking a snapshot of the database in the middle of a competition. It also allows bringing back a Heroku database for local use, and conversely, setting up a competition locally prior to loading on Heroku.

  > For now you should still take backups of the real database (in the database directory on a laptop, or using the PostgreSQL backup on Heroku).  Until it is fully mature, it is considered a technical feature and is located at on the "Language and System Settings Page".

- [x] PIN/password is now handled as a salted SHA-256 hash, so it does not appear in clear in the export file.

- [x] The procedure to import a PostgreSQL database from Heroku and use it a local machine is now documented. See [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/PostgreSQL) for details.  This is meant as a backup for investigating PostgreSQL specific issues, or something that would prevent a cloud instanceser from starting.

###### **Key Highlights from recent stable releases**

- [x] Explicit support for participation to multiple age groups (#433)
  - An athlete will, by default, be eligible and ranked separately in all the active categories in which the age and qualifying total are met.   
  - Participation to an eligible category can be removed or added back on the registration page for the athlete.
  - "Multiple rank" scoreboard shows ranks for all age groups present in the currently lifting group.
  - Final results packages can be produced for each age group, or combined for all age groups (when all age groups contribute to team points)
  - Qualifying totals can be entered in the AgeGroup Excel file for each age group category, and also from the Age Group web page.
  - Robi scores are now computed based on age group. If an athlete is eligible for JR and SR, the JR Robi will appear in the JR final package, and the SR Robi in the Senior final package.
- [x] Teams are now separate by age group or age division

  - Teams can be defined by Age Group (e.g. JR, SR, U15, U17) or by Age Division together (e.g. Masters, or all age Uxx age groups combined)
  - Team-oriented scoreboards allow selecting which age group is shown (e.g. JR Team is different from the SR team)
- [x] Results improvements and fixes:
  - The Protocol sheet and the Final Package result pages shows header for each category.  The old flat protocol sheet is still available as "6Attempts" template for copy-paste needs.
  - From the Category Results page, ability to produce a result sheet for the selected category, age group, or age division.
  - Cleaned-up the Final Package Excel templates: fixed formatting, added missing translations. Custom score final package option now present.
- [x] All resources in the local directory take precedence over the built-in ones (visual styles, templates, age group definitions, sounds, etc.)
  - You can also add a file with an extension for your locale (e.g. ex: _hy, _ru, _fr) and it will be used for things like marshal cards, starting list, etc.
  - You can zip the local directory on your laptop and upload the to a cloud-based setup (see the System configuration page from the Preparation section) (#366)
- [x] Moved the Language and Time Zone settings together with the technical settings to a renamed "Language and System Settings" page reachable from the "Preparation" section.
  - [x] For troubleshooting, entering the "Language and System Settings" page prints the networking interfaces and addresses in the log.
- [x] Enhancement: the program now forces start numbers when lifting starts if they have not been attributed at weigh-in (useful for small competitions not using cards with novice users)
- [x] Marshall screen now shows decisions (#411). This is for setups where athlete cards are used and where it is difficult to see the scoreboard or inconvenient to switch tabs.
- [x] Clearer error message when athlete A cannot move down because B has attempted same weight on a bigger attempt number (if so, A should have lifted before B, cannot move down.)

- [x] When a display is first started, a dialog offers to enable warning sounds or not.  Warnings are silenced by default; they should normally be enabled on only one display per room, to avoid warnings coming from several directions. See the [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Displays#display-settings) for details (#407)
- [x] Implemented the <u>rules to prevent athletes from moving down their requested weight illegally</u>.  Moving down is denied if the athlete should already have attempted that weight according to the official lifting order.  The exact checks resulting from applying the TCRR to that situation are spelled out in the [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Announcing#rules-for-moving-down). (#418)
- [x] Violations of <u>rules for timing of declarations</u> (before initial 30 seconds), and for changes (before final warning) are now signaled as errors (#425, #426). Overriding is possible for officiating mistakes.
- [x] iPads now supported as refereeing device with Bluetooth buttons (running either the athlete-facing time+decision display or the attempt board display.)   iPads require that sound be enabled by touching a screen button once when the board is started. (#408). 

**Installation Instructions :**

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean) or `k3d_setup.yaml` for [home hosting](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/k3d).  For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
