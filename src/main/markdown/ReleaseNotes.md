> **Version 44.3-beta**	
>
> - Reminder: *You should test any release with your own data before using it in a competition.*

**44.3**

- 44.3.0-beta01 New: added support for [Qpoints](https://osf.io/8x3nb/) (a cleaner alternative to Sinclair).  A new protocol results sheet (QProtocol) shows QPoints instead of Sinclair. The final results packages have an additional sheet for Qpoints.  The Qpoints are also visible on the Results pages.

**Version 44 changes **

*See the bottom of this note for required changes if you are streaming video or have customized the styles.*

- Visual styling changes
  - The visual styling has been changed for a more modern look. 
    - The new default is called `nogrid`
    - The old style has been renamed to `grid`. 
    - The style can specified in the *Preparation - Settings - Customization* page.  For example, to get the old look back, you would specify `grid` instead of `nogrid`.  You can also specify the name of your own local styling directory.
    - If the style directory named in the database is not found, the default  `nogrid`  is forced. So if you had changed the old `styles` directory, you must move it `local/css` before it can be used. You can rename it to whatever name you want.

- Video Streaming: Video styling is now requested using `video=true`  as a query parameter after the `?` (like all the other parameters). See the *REQUIRED CHANGES FOR VIDEO STREAMING.* section at the bottom of this note.

- The display selection page has been changed.  The public scoreboards are now separate from the warmup scoreboards. The public scoreboards switch to the medal display during medal ceremonies.


- It is no longer necessary to start different browsers to run owlcms, publicresults and owlcms-firmata on the same laptop. They no longer interfere with one another.
  - However different browsers are still needed when testing several platforms on the same computer


- A new report is available to accelerate the verification of final entries (VFE).  See the "Pre-Competition Documents" under "Teams". The document allows team leaders to sign off the changes to the athlete's category, entry total, as well as team memberships.

- The break management dialog for pauses and countdowns has been redesigned.

- The download dialog for documents has been redone for robustness.

- This version updates the user interface to use the most current [Vaadin 24 framework](https://vaadin.com/), which has several major changes. The most important is a switch to the modern [LitElement](https://lit.dev/) template framework (used for the scoreboards, timers, and decisions).

**Version 44 REQUIRED ADJUSTMENTS**

- **REQUIRED CHANGES FOR CSS CUSTOMIZATION**.
  This only concerns advanced users who have edited the css files
  - Compare with the official style sheets and define a variable called 
    `--defaultLeaderFillerHeight: 1fr` and edit the definitions of the `.filler` style to remove the `min-height` settings.
  - If you display the custom1 or custom2 fields on the scoreboard, you need to add the lines from the bottom of `resultsCustomization.css`.  If several lines are needed, you must also uncomment the directive so that `pre-wrap` is enabled.
  - *Local variations to styling MUST be copied to a subdirectory of* `local/css`.  If you have customized the `styles` folder move it to `local/css/mystyles` (or whatever name you want), and update the location your Preparation - Settings - Customization page. 
  - Style sheet changes :  If you have customized the scoreboards,  you need to edit the `results.css` files. All instances of `:host(.dark)` must be changed to `.host .dark`  and all instances of `:host(.dark)` must be changed to `.host .dark`  
  - Style sheet bug fixes: Several small changes have been made to grid and nogrid to fix small problems. You should compare your style sheets to the official ones.  Or for more safety, start from the official ones and redo your adjustments.

- **REQUIRED CHANGES FOR VIDEO STREAMING**.
  - For video streaming, *URLs that use*  `/video` *in* *the URL path should be changed* to use `?video=true` as a query parameter instead (video is now a parameter like all the others.)
