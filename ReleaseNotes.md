**Version 47.0 ALPHA**	

> [!WARNING]
>
> - This is an early release **NOT READY FOR USE IN A COMPETITION**.  Alpha releases are used to gather feedback on possible features.  **Features are likely incomplete or broken.**
> - If you are looking for a release 46 release candidate, please look at the [full pre-release list](https://github.com/owlcms/owlcms4-prerelease/tags).
> - If you have made local customizations, see the Customization Notes at the bottom of this page.

##### 47.0 

- Ability to customize the result and attempt board styling based on the current platform.  This is typically used to change some colors for identification when streaming, or the attempt board when on site.
  - An identifier for the current platform is added to the top-level `wrapper` element in the page.  The name is modified to only keep legal CSS characters (Latin letters, digits, hyphens and underscores). If you wish to use this feature, the platform will have to have a Latin name, or only digits.  A `_` will be added in front of a digits-only name to make it legal.
- It is now possible to create a zip archive of the local directory overrides from the "Customization" tab on the Settings page.
  - When running in the cloud, the setup can be done on a local laptop. The resulting zip can be uploaded to the cloud server where it is stored inside the database
  - When running locally, a federation can create a standard "kit" on a reference laptop and capture the zip this way.  Member clubs upload the zip, which is stored inside the database (same as the cloud)
  - NOTE: due to the possible presence of accented characters in file names, the resulting zip cannot always be read by the standard zip utility shipped by default.  However, the widely available 7z software (p7zip on Linux) can read the newer internationalized format.  So use 7z if you want to prune the resulting zip.  There is a version of 7z available for Windows, Linux and Mac.  You can also use the `jar` command that comes with Java.
- Flic2: Added `;` and the numeric pad `=` as keyboard shortcuts for setting the clock to 2:00

##### Customization Notes

> - Some minor changes have been made to the colors.css, results.css and attemptboard.css files.

