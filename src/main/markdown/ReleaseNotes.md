> **Version 44.0 **
>
> - A release candidate is normally the last release before an official release.  It is used as a last check to make sure that the build and packaging steps are correct.
> - *Any release should be tested thoroughly before being used.*
>
> Version 44 is a technical migration release.  It updates the code to the current version of the user interface framework ([Vaadin 24](http://vaadin.com)). A significant clean-up of the code was performed at the same time, and several annoyances were fixed as a result.
>

**44.1.0**

- iOS devices (iPhones and iPads) were not emitting sounds for down and timer warnings.  General clean-up of the sound activation dialogs.

44.0.1

- Fixes for links in the documentation

**44.0.0**

- *See the bottom of this note for required changes if you are doing video streaming or if you have customized the styles.*
- Visual styling changes
  - The visual styling has been changed for a more modern look. 
    - The new default is called `nogrid`
    - The old style has been renamed to `grid`. 
    - An alternate styling directory can specified in the *Preparation - Settings - Customization* page.  For example, to get the old look back, you would specify `grid` instead of `nogrid`.  You can also specify your own local styling directory.
    - If the styles directory named in the database is not found, the default  `nogrid`  is forced. So if you had changed the old `styles` directory, you must move it `local/css` before it can be used. You may now rename to whatever name you chose.
  - Streaming: Video styling is now requested using `video=true`  as a query parameter after the `?` (like all the other parameters). See the bottom of this note.
- The display selection page has been changed.  The public scoreboards are now separate from the warmup scoreboards - many people were unaware of the public scoreboard feature that switches the display during medal ceremonies.
- It is no longer necessary to start different browsers to run owlcms, publicresults and owlcms-firmata on the same machine  They no longer interfere with one another.
  - However different browsers are still needed when testing several platforms on the same computer
- A new report is available to accelerate the verification of final entries (VFE).  See the "Pre-Competition Documents" under "Teams". The document allows team leaders to sign off the changes to the athlete's category, entry total, as well as team memberships.
- The break management dialog for pauses and countdowns has been redesigned.
- The download dialog for documents has been redone for robustness.
- This version updates the user interface to use the most current [Vaadin 24 framework](https://vaadin.com/), which has several major changes. The most important is a switch to the modern [LitElement](https://lit.dev/) template framework (used for the scoreboards, timers, and decisions).

**44.0 REQUIRED ADJUSTMENTS**

- **REQUIRED CHANGES FOR STYLES CUSTOMIZATION AND VIDEO STREAMING**.
  This only concerns advanced users who have edited the css files or are doing video streaming.
  - *Local variations to styling MUST be copied to a subdirectory of* `local/css`.  If you have customized the `styles` folder move it to `local/css/mystyles` (or whatever name you want), and update the location your Preparation - Settings - Customization page. 
  - Style sheet changes :  If you have customized the scoreboards,  you need to edit the `results.css` files. All instances of `:host(.dark)` must be changed to `.host .dark`  and all instances of `:host(.dark)` must be changed to `.host .dark`  
  - Style sheet bug fixes: Several small changes have been made to grid and nogrid to fix small problems. You should compare your style sheets to the official ones.  Or for more safety, start from the official ones and redo your adjustments.
  - For video streaming, *URLs that use*  `/video` *in* *the URL path should be changed* to use `?video=true` as a query parameter instead (video is now a parameter like all the others.)
- 
