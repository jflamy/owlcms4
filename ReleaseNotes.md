> **Version 45.0**	
>
> - Version 45 is a technical release that updates the way the scoreboard styling is done.  There is now support for 4K TV screens, and better support for phones and tablets.  Vertical orientation is also supported.  *If you have customized your own CSS style files, see the section at the bottom of these notes.*
> - Reminder: you should always test any new release several days in advance, with your own data.

##### 45.0 Changes

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
- Templates and documents

  - The template name is used as the basis for the output file name.
  - Challenge cards can now be printed (same menu as Marshal cards)
- MQTT: a new message "refereesDecision" is available to make it easier to create devices that control ambiance lighting based on good/bad lift.


**Version 45 CUSTOM CSS ADJUSTMENTS**

This only concerns advanced users who have edited the css files

- *Important changes were made to the css files* for the scoreboards.  
  - In particular, the differences between dark and light styles are defined by variables in the  `colors.css` . 
  - The other files, such as `results.css` use these variables. Only a very small number of CSS rules still need to be different between dark and light.

- The major change is using an absolute `rem` font size unit in the `wrapper`.  All the font sizes elsewhere should be `em` units.   However, for now, the dimensions of the timers and decision lights remain vh/vw units.
- Another noticeable change is that the record grids at the bottom of scoreboards have been redone using grids and as a result, the border colors and width must now be defined using the `outline` directive.

