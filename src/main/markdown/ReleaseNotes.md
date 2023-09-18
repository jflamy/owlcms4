> **Version 44 beta release**
>
> - Beta releases are meant for translation and for testing by people other than the developers.
> - Beta07 should be tested very thoroughly before being used.
>
> Version 44 is a technical migration and clean-up release.  It updates the code to the current version of the user interface framework ([Vaadin 24](http://vaadin.com)). This update requires changes in the code that provide an opportunity to clean and simplify the programming.
>

**44.0.0-beta07**

- Internal clean-up: continuation of the cleanup done in beta06
- Known issues:
  - The Public scoreboard mechanism and the Medals display don't work yet.
  - See also this [list of known small issues](https://github.com/jflamy/owlcms4/issues/734)

**44.0 Changes**

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
