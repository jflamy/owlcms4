> **Version 45.0-alpha**	
>
> - Alpha releases are snapshots of active development.  
> - They are meant to gather feedback and for initial tests.
> - Alpha releases are not meant for use in competitions. Look for a stable release or at least a release candidate

**45.0**

- alpha00: changes to the "nogrid" style ("grid" will be done afterwards)
  - Support for 4K TV screens without having to change display resolution
  - CSS variations for phones, tablets, laptops, 2K (1920x1080) and 4K devices.
  - Support for vertical orientation devices
- alpha00: on-site styles and video styles and are now two different settings.  They both default to `nogrid`.  If you use green color filtering for transparency effects on video, you can select `transparent` as the video style (the `transparent` style is designed for a 1920x1080 canvas) 

**Version 45 CUSTOM CSS ADJUSTMENTS**

This only concerns advanced users who have edited the css files

- *Important changes were made to the css files* for the scoreboards. 
- The `colors.css` file has only minor changes.  You should be able to put your colors back.
- The major change is the use of an absolute `rem` font size unit in the `wrapper`.  All the font sizes elsewhere should be `em` units.  One exception is the decision lights and timer areas, which are in viewport units (vw/vh).
- Another change is that the record grids at the bottom of scoreboards have been redone using grids. As a result, the borders are now done using the `outline` directive.

