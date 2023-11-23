> **Version 44.5 release candidate**
>
> - Reminder: *You should test any release with your own data before using it in a competition.*

##### 44.5

- Fix: During a 2:00 athlete clock, a late declaration would be falsely signaled when the first change took place after 1:30.
- Fix: Reloading athlete info during the CJ break would restart the break needlessly.
- Fix: Registration form upload could occasionally fail due to an error in category matching
- Change: the snatch and cj break countdown timers are no longer hidden during the presentation of officials and medal ceremonies.
- Fix: Refreshing publicresults would alternate between normal starting order and lifting order. Now correctly interprets the URL parameters.

##### Version 44 changes

*See the bottom of this note for required changes if you are streaming video or have customized the styles.*

- Visual styling changes
  - The visual styling has been changed for a more modern look. 
    - The new default is called `nogrid`
    - The old style has been renamed to `grid`. 
    - The style can specified in the *Preparation - Settings - Customization* page.  For example, to get the old look back, you would specify `grid` instead of `nogrid`.  You can also specify the name of your own local styling directory.
    - If the style directory named in the database is not found, the default  `nogrid`  is forced. So if you had changed the old `styles` directory, you must move it `local/css` before it can be used. You can also then rename it to whatever you want, and use that name in the Customization section.

- Video Streaming: Video styling is now requested using `video=true`  as a query parameter after the `?` (like all the other parameters). See the *REQUIRED CHANGES FOR VIDEO STREAMING.* section at the bottom of this note.

- The display selection page has been changed.  The public scoreboards are now separate from the warmup scoreboards. The public scoreboards switch to the medal display during medal ceremonies.


- It is no longer necessary to start different browsers to run owlcms, publicresults and owlcms-firmata on the same laptop. They no longer interfere with one another.
  - However different browsers are still needed when testing several platforms on the same computer


- A new report is available to accelerate the verification of final entries (VFE).  See the "Pre-Competition Documents" under "Teams". The document allows team leaders to sign off the changes to the athlete's category, entry total, as well as team memberships.

- The break management dialog for pauses and countdowns has been redesigned.

- The download dialog for documents has been redone for robustness.

- Fix: for small weights, if no 5kg bumpers are available, the loading display will now show the 2.5kg bumper and metal plates/collars as required (instead of switching to a 5kg metal plate).

- Added support for [Qpoints](https://osf.io/8x3nb/) (a cleaner alternative to Sinclair).  A new protocol results sheet (QProtocol) shows QPoints instead of Sinclair. The final results packages have an additional sheet for Qpoints.  The Qpoints are also visible on the Results pages.
- This version updates the user interface to use the most current [Vaadin 24 framework](https://vaadin.com/), which has several major changes. The most important is a switch to the modern [LitElement](https://lit.dev/) template framework (used for the scoreboards, timers, and decisions).

**Version 44 REQUIRED ADJUSTMENTS**

- **REQUIRED CHANGES FOR CSS CUSTOMIZATION**.
  This only concerns advanced users who have edited the css files
  - Compare with the official style sheets and make sure you define a variable called 
    `--defaultLeaderFillerHeight: 1fr` and edit the definitions of the `.filler` style to remove the `min-height` settings.
  - If you display the custom1 or custom2 fields on the scoreboard, you need to add the lines from the bottom of `resultsCustomization.css`.  If you wish to have custom1 and custom2 display with line breaks, you must also uncomment the directive so that `white-space: pre-wrap` is defined.
  - *Local variations to styling MUST be copied to a subdirectory of* `local/css`.  If you have customized the `styles` folder move it to `local/css/mystyles` (or whatever name you want), and update the location your Preparation - Settings - Customization page. 
  - Style sheet changes :  If you have customized the scoreboards,  you need to edit the `results.css` files. 
    - All instances of `:host(.dark)` must be changed to `.host .dark`  and all instances of `:host(.light)` must be changed to `.host .light`  
  - Style sheet bug fixes: Several small changes have been made to grid and nogrid to fix small problems. You should compare your style sheets to the official ones.  Or for more safety, start from the official ones and redo your adjustments.
  
- **REQUIRED CHANGES FOR VIDEO STREAMING**.
  - For video streaming, *URLs that use*  `/video` *in* *the URL path should be changed* to use `?video=true` as a query parameter instead (video is now a parameter like all the others.)
