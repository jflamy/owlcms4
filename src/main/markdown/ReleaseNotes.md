> **Version 44 beta release**
>
> - Beta releases are meant for translation and for testing by people other than the developers.
> - *This release should be tested very thoroughly before being used.*
>
> Version 44 is a technical migration release.  It updates the code to the current version of the user interface framework ([Vaadin 24](http://vaadin.com)). A significant clean-up of the code was performed at the same time, and several annoyances were fixed as a result.
>

**44.0.0-beta15**

- Fix: the attempt board would not clear the record indicators when switching to another athlete.  The color of the new record was also wrong.
- Fix: the athlete card validation was over-zealous, and signaled errors on cells that should not have been validated.
- Flags: The scoreboards and medal boards now handle both short and long team names.  The flag is positioned at the left of the cell.  For 3-letter acronyms, making the team column narrow works well.
- Fix: the public scoreboard now correctly switches to medals when the announcer starts the medal ceremony.  The medal board inherits the team width and other styling parameters from the public scoreboard.
- Known issues: see this [list of known defects](https://github.com/jflamy/owlcms4/issues/734)


**44.0 Changes**

- The display selection page has been changed.  The public scoreboards are now separate from the warmup scoreboards - many people were unaware of the public scoreboard feature that switches the display during medal ceremonies.
- It is no longer necessary to start different browsers to run owlcms, publicresults and owlcms-firmata on the same machine  They no longer interfere with one another.
  - However different browsers are still needed when testing several platforms on the same computer
- The visual styling has been changed for a more modern look. 
  - The new default is `nogrid`
  - The old "styles" has been renamed to `grid`. 
  - An alternate styling directory can specified in the Preparation - Settings - Customization page.  For example, to get the old look back, you would specify `grid` instead of `nogrid`.  You can also specify a local styling directory.
  - Local variations to styling MUST be copied to a subdirectory of `local/css`.  If you have customized the `styles` folder move it to `local/css/mystyles` (or whatever name you want), and update the location your Preparation - Settings - Customization page. 
  - If the styles directory named in the database is not found, the default  `nogrid`  is forced. A customized style directory must be moved to `local/css` before it can be used.
  - Style sheet changes :  If you have customized the scoreboards,  you need to edit the `results.css` files. All instances of `:host(.dark)` must be changed to `.host .dark`  and all instances of `:host(.dark)` must be changed to `.host .dark`  
- A new report is available to accelerate the verification of final entries (VFE).  See the "Pre-Competition Documents" under "Teams". The document allows team leaders to sign off the changes to the athlete's category, entry total, as well as team memberships.
- The break management dialog for pauses and countdowns has been redesigned.
- The download dialog for documents has been redone for robustness.
- This version updates the user interface to use the most current [Vaadin 24 framework](https://vaadin.com/), which has several major changes. The most important is a switch to the modern [LitElement](https://lit.dev/) template framework (used for the scoreboards, timers, and decisions).
