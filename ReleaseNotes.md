> **Version 45.0-beta**	
>
> - BEWARE: this is an early version of a future 45 release, **not a version 44 bug fix release**.  The version 44 prereleases have 44 in their version number.
> - Beta releases are for testing and translation, they are meant to gather feedback and for tests by early adopters.  Normally no new features are introduced at the beta stage, only fixes.
> - Beta releases are seldom used in competitions and need to be extremely well tested before doing so. Please look for a [stable release](https://github.com/owlcms/owlcms4/releases) or at least a release candidate unless you absolutely need a feature or fix.

**45.0-beta**

- (beta04) Fix transparent style sheet for video streaming, adjusted nogrid style sheet.
- (beta03) Fix library dependencies so the application runs in production mode, update to Jetty version 12.
- (beta02) Merge bug fixes from 44.7, update to Vaadin 24.2.6
- (beta01) Feature freeze.

##### 45.0

- Changes to the `nogrid` and `grid`  styles
  - Support for 4K TV screens (at 100% scaling)
  - CSS variants for phones, tablets, laptops, 2K (1920x1080) and 4K devices.
  - Support for vertical orientation devices. 
    - The initial look on phones and tablets is now correct. 
    - Special competitions that use very large groups with multiple platforms can use vertical TV screens
- On-site styles and video styles are now separate configuration settings.
  - They both default to `nogrid`. 
  - You can create alternate looks by copying a style to a new name under `local/css` and editing the colors and other css.
  - However, a `transparent` style is also available for video. It uses a pure green background that can be made transparent by video streaming software such as OBS.  `transparent` is designed for a 1920x1080 canvas.
- Templates and documents

  - Use the template name as the basis for the output file name.
  - Changed the Challenge Card templates so they can be printed in advance (no start number)

  - Added Challenge cards (same menu as Marshal cards)
- MQTT: a new message "refereesDecision" was added to make it easier to create devices that control ambiance lighting based on good/bad lift.


**Version 45 CUSTOM CSS ADJUSTMENTS**

This only concerns advanced users who have edited the css files

- *Important changes were made to the css files* for the scoreboards. 
- The `colors.css` file has only minor changes.  You should be able to put your colors back.
- The major change is using an absolute `rem` font size unit in the `wrapper`.  All the font sizes elsewhere should be `em` units.   However, for now, the dimensions of the timers and decision lights remain vh/vw units.
- Another noticeable change is that the record grids at the bottom of scoreboards have been redone using grids and as a result, the border colors and width must now be defined using the `outline` directive.

