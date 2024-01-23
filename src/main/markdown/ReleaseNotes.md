> **Version 45.0**	
>
> - Version 45 is a technical release that updates the way the scoreboard styling is done.  There is now support for 4K TV screens, and better support for phones and tablets.  Vertical orientation is also supported.  *If you have customized your own CSS style files, see the section at the bottom of these notes.*
> - Reminder: you should always test any new release several days in advance, with your own data.

45.0.x

- (45.0.7) Clean & Jerk break could be displayed falsely as Introduction of Athletes.
- (45.0.6) Robi points: the default AgeGroups.xls file first tab has been updated with current world records. 
- (45.0.6) Very long family names on the attempt board are split on two lines if over 18 characters. The split is made on spaces or hyphens, and the font is made a bit smaller.
- (45.0.6) Usage logging has been made more reliable.
- (45.0.5) New feature toggle `disableRecordHighlight`  This is useful to leave the attempt board untouched (no "Record Attempt" banner) when record attempts or new records are signaled to the audience on the main screen using video production equipment. See the [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/FeatureToggles).
- (45.0.5) New BENCHMARK value for OWLCMS_INITIALDATA. This creates a very large competition with 4 concurrent platforms and ~1150 athletes of all IWF and Masters age groups. See he [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Configuration).
- (45.0.5) The clean and jerk break is shortened to 10 seconds during simulations.
- (45.0.5) Fixed usage reporting to report whether the application is running locally or in the cloud, and to report only once
- (45.0.4) Updated translations to 2024-01-12.
- (45.0.4) Updated cloud installation instructions to use the interactive [owlcms-cloud.fly.dev](https://owlcms-cloud.fly.dev) application
- (45.0.3) Fixed the vertical layouts for phones/tablets/tvs (including publicresults) to use the full width.
- (45.0.2) Fixed the Jury sheet template layout (available from the Weigh-In and Preparation pages)
- (45.0.1) Increased the flag sizes and fixed the borders on the results scoreboards.
- (45.0.1) Local installation of publicresults fixed to correctly request the configuration files.

##### 45.0
- Templates and documents
  - It is now possible to print challenge cards. Select the template under "Athlete Cards" in the pre-competition or weigh-in pages.
  - Verification of Final Entries: A form is now available to allow team leaders to sign off (in writing) their team membership and registered categories (Select the template under "Pre-competition Documents > Entries, >Teams")
  - Challenge cards can now be printed (same menu as Marshal cards)
  - The template name is used as the basis for the output file name.
- Changes to the `nogrid` and `grid`  styles
  - Support for 4K TV screens (at 100% scaling)
  - CSS variants for phones, tablets, laptops, 2K (1920x1080) and 4K devices.
  - Support for vertical orientation devices. 
    - The initial look on phones and tablets is now correct. 
    - Special competitions that use very large groups with multiple platforms can use vertical TV screens
- On-site styles and video styles are now separate configuration settings.
  - By default, both the on-site and video styles are set to `nogrid`
  - A `transparent` style can be used for video. It uses a pure green background that can be made transparent by video streaming software such as OBS.  `transparent` is designed for a 1920x1080 canvas.
  - You can create you own alternate looks by copying a style to a new name under `local/css` and editing the `colors.css` files and, if needed, the other .css files.
- MQTT: a new message "refereesDecision" is available to make it easier to create devices that control ambiance lighting based on good/bad lift.


**Version 45 CUSTOM CSS ADJUSTMENTS**

This only concerns advanced users who have edited the css files

- *Important changes were made to the css files* for the scoreboards.  
  - In particular, the differences between dark and light styles are defined by variables in the  `colors.css` . 
  - The other files, such as `results.css` use these variables. Only a very small number of CSS rules still need to be different between dark and light.

- The major change is using an absolute `rem` font size unit in the `wrapper`.  All the font sizes elsewhere should be `em` units.   However, for now, the dimensions of the timers and decision lights remain vh/vw units.
- Another noticeable change is that the record grids at the bottom of scoreboards have been redone using grids and as a result, the border colors and width must now be defined using the `outline` directive.

