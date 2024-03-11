**Version 47.1.0 Alpha**

> [!WARNING]
>
> - Alpha releases are for the initial testing of new features. Features can be broken or incomplete. 
>   **Do not use alpha releases for actual competitions.**
> - You should test all releases with your own data, several days in advance.

##### 47.1

- Fix: If the advanced start book data entry (SBDE) spreadsheet was loaded, but owlcms was not restarted before the competition, multiple display update processes were present and could issue mutually contradictory display update instructions.

##### 47.0

- Start numbers are now assigned by bodyweight category then by age group. A checkbox is available when it it desired to keep categories from the same age group together (i.e. kid categories first).  A separate option selects Masters order (older categories first).
- There is now the ability to have different styles for the result and attempt board styling depending on the platform (both on-site and on streaming)
  - An identifier for the current platform is added to the top-level `wrapper` element in the page.  The name is modified to only keep legal CSS characters (Latin letters, digits, hyphens and underscores). If you wish to use this feature, the platform will have to have a Latin name, or only digits.  A `_` will be added in front of a digits-only name to make it legal.
- When running on a laptop, it is now possible to create a zip backup of the  `local` directory (from the Settings page/Customization tab).   This makes it easier to upload flags, style sheets, and templates to cloud instances, or to create a standard kit within a federation.
  - When running in the cloud, the zip is loaded to the database, and unpacked when the application starts to provide the customization.
  - When running locally, there are two options
    1. Same as the cloud -- load the .zip inside the database, it is unpacked at startup, but the contents of the local directory in the installation directory is untouched.
    2. Delete the files inside the local directory inside the installation and replace them with the zip.  This is more risky but makes changing the .zip easier -- edit and re-package.
  - NOTE: if you have accented or non-Latin characters in your file names, the resulting zip is in "international" format and must be read using the 7z program that is available on Windows, Linux and MacOS, or using the `jar` command that comes with Java.
- Public Results: the Down icon has been missing since version 45. It is now back.
- Flic2 buttons: Added `;` and the numeric pad `=` as keyboard shortcuts for setting the clock to 2:00

