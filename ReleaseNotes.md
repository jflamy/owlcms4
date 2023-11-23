> **Version 45.0-alpha**	
>
> - BEWARE: this is an early version of a future release. 
>   - **For the bug fix prereleases of the 44 stable version, look for versions 44.x-beta or 44.x-rc**
> - Alpha releases are snapshots of active development.  
> - They are meant to gather feedback and for initial tests.
> - Alpha releases are not meant for use in competitions. Please look for a [stable release](https://github.com/owlcms/owlcms4/releases) or at least a release candidate.

**45.0-alpha**

- (alpha04) : Merge 44.5 fixes and update to Vaadin 23.2.4
- (alpha04) : Templates and documents
  - Enhancement: Use the template name as the basis for the output file name.
- (alpha03) : Templates
  - Fix: Workaround for non-working page breaks in Apache POI Excel library
  - Fix: Changed the Challenge Card templates so they can be printed in advance (no start number)

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

