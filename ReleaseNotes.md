<<<<<<< HEAD
> **Version 45.0-alpha**	
=======
> **Version 44.5 release candidate**
>>>>>>> refs/remotes/origin/v24
>
> - Alpha releases are snapshots of active development.  
> - They are meant to gather feedback and for initial tests.
> - Alpha releases are not meant for use in competitions. Please look for a [stable release](https://github.com/owlcms/owlcms4/releases) or at least a release candidate.

<<<<<<< HEAD
**45.0-alpha**
=======
##### 44.5
>>>>>>> refs/remotes/origin/v24

<<<<<<< HEAD
- alpha03: Templates
  - Workaround for non-working page breaks in Apache POI Excel library
  - Changed Challenge Card templates so they can be printed in advance (no start number)
=======
- (rc01) Fix: Refreshing publicresults would alternate between normal starting order and lifting order. Now correctly interprets the URL parameters.
- Fix: During a 2:00 athlete clock, a late declaration would be falsely signaled when the first change took place after 1:30.
- Fix: Registration form upload could occasionally fail due to an error in category matching
- Fix: Reloading athlete info during the CJ break would restart the break needlessly.
- Change: the snatch and cj break countdown timers are no longer hidden during the presentation of officials and medal ceremonies.
>>>>>>> refs/remotes/origin/v24

##### 45.0

- Added Challenge cards (same menu as Marshal cards)
- Changes to the `nogrid` and `grid`  styles
  - Support for 4K TV screens (without having to change the display resolution)
  - CSS variants for phones, tablets, laptops, 2K (1920x1080) and 4K devices.
  - Support for vertical orientation devices. 
    - The initial look on phones and tablets is now correct. 
    - Special competitions that use very large groups with multiple platforms can use vertical TV screens
- On-site styles and video styles are now separate configuration settings.
  - They both default to `nogrid`. 
  - You can create alternate looks by copying a style to a new name under `local/css` and editing the colors and other css.
  - However, a `transparent` style is also available for video. It uses a pure green background that can be made transparent by video streaming software such as OBS.  `transparent` is designed for a 1920x1080 canvas.
- Updated to latest Vaadin 24.2.0


**Version 45 CUSTOM CSS ADJUSTMENTS**

This only concerns advanced users who have edited the css files

- *Important changes were made to the css files* for the scoreboards. 
- The `colors.css` file has only minor changes.  You should be able to put your colors back.
- The major change is using an absolute `rem` font size unit in the `wrapper`.  All the font sizes elsewhere should be `em` units.   However, for now, the dimensions of the timers and decision lights remain vh/vw units.
- Another noticeable change is that the record grids at the bottom of scoreboards have been redone using grids and as a result, the border colors and width must now be defined using the `outline` directive.

