**Version 47.0 ALPHA**	

> [!WARNING]
>
> - This is an early release **NOT READY FOR USE IN A COMPETITION**.  Alpha releases are used to gather feedback on possible features.  **Features are likely incomplete or broken.**
> - If you are looking for a release 46 release candidate, please look at the [full pre-release list](https://github.com/owlcms/owlcms4-prerelease/tags).
> - If you have made local customizations, see the Customization Notes at the bottom of this page.

##### 47.0 

- Ability to customize the result and attempt board styling based on the current platform.  This is typically used to change some colors for identification when streaming, or the attempt board when on site.
  - An identifier for the current platform is added to the top-level `wrapper` element in the page.  The name is modified to only keep legal CSS characters (Latin letters, digits, hyphens and underscores). If you wish to use this feature, the platform will have to have a Latin name, or only digits.  A `_` will be added in front of a digits-only name to make it legal.


##### Customization Notes

> - Some minor changes have been made to the colors.css, results.css and attemptboard.css files.

