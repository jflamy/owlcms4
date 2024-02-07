> **Version 45**	
>
> - Version 45 is a technical release that updates the way the scoreboard styling is done.  There is now support for 4K TV screens, and better support for phones and tablets.  Vertical orientation is also supported.  *If you have customized your own CSS style files, see the section at the bottom of these notes.*
> - Reminder: you should always test any new release several days in advance, with your own data.

##### 45.2

- When using records from multiple federations, 
  - if there were identical records (e.g. Canadian Record = PanAm record) the record loaded last would be falsely ignored as a duplicate.
  - The setting for "show records from all federations" did not cover all cases.
  - The list of eligible federations did not work if there were spaces around the "," or ";" separator.

**45.1**

- Wrap-up release. Work has started on release 46.
- Only the addresses reachable from other computers are shown on the home page. Loopback (127.*) and APIPA (168.154.*) addresses are no longer shown.
- Green highlights for frequently used buttons on various pages
- We no longer suggest updating to an alpha version.

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

