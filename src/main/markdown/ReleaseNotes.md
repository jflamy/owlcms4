> **Version 44.2**
>
> - Reminder: *You should test any release with your own data before using it in a competition.*

**44.2.2**

- 44.2.2-rc02 Fix: A false error message could be emitted when the first CJ was lower than the highest Snatch.
- Fix: Editing athletes during a break could end the break
- Fix: The withdrawal of an athlete would prevent the automatic CJ break
- Fix: A false error message could be emitted related to a late declaration.

**44.2.1**

- Fix: Athlete pictures would not show on attempt board if the application was running on Windows.
- Fix: Hidden "Team" header cell on start list and other Excel templates

**44.2.0**

- Fixes for sound activation.  Browsers require that an interaction such as clicking or touching takes place in order to play sounds.  This was not working on iPad/iPhone, and not fully reliable on other browsers.
- Usability: Disabled the user interface library's default keyboard navigation focus ring on the athlete grids (announcer, marshal, registration, weigh-in, etc.).
- Fix: Flags were not positioned correctly on the attempt board (they were always centered as if athlete pictures were present).
- Fix: Records would not be shown if the records were uploaded but the "save configuration" button was not applied. Uploading record files now saves the configuration.
- Fix: On grids with checkboxes or buttons (such as the age group or team membership pages), clicking on the checkbox or button would open the dialog box as if the line had been selected (the behavior changed from previous versions of the Vaadin user interface library.). Now fixed.
- Fix: The scoreboard line height was too high if data was present in the custom1/custom2 fields, even if these columns were hidden.  See the *REQUIRED CHANGES FOR CSS CUSTOMIZATION* text at the bottom of these notes.
- Fix: The Marshal console now is silent by default.
- Fix: On the scoreboard, there would be a short period where the group description would switch to "Clean & Jerk" instead of staying on "Snatch" after an athlete took their 3rd Snatch. Now fixed.
- Fix for importing databases when the `local` override directory has been deleted.

**Version 44 changes**

*See the bottom of this note for required changes if you are doing video streaming or if you have customized the styles.*

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
