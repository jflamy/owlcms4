#### Version 47.1.1

> [!IMPORTANT]
> 
> - You should test all releases with your own data, several days before a competition.

##### 47.1.1

- Fix: the countdown for the time before snatch is now starting correctly at the end of the athlete introduction.

##### 47.1

- Announcer-controlled display of jury decisions : To better apply the IWF rule that the reason for jury reversals must be announced, by default the jury decision buttons now triggers a two-step process:

  1. show a prompt to the announcer.  The announcer  announces the decision and the reason
  2. the announcer presses a button that updates the system and the scoreboards. 

  The prior behaviour (instant update when the jury presses) is still available by using a checkbox in the Competition Rules section

- Excel Registration File Fixes

  - Both the Simple registration format and the full Start Book Data Entry (SBDE) now accept start times in Excel numerical date format, and in Text format (yyyy-MM-dd hh:ss).  
  - The times are now correctly rounded to the closest minute
  - Both formats again correctly update pre-existing groups.

- Age Group Definitions

  - It is now possible to download an age group definition Excel from the system, edit it, and upload the edited file.  This makes it easier to change things like qualification totals. 
  - The list of predefined Age Group definition files is again filtered according to locale, so that fewer files are shown: if the file name ends with _es_ES, it will only be shown to Spanish users in Spain.

- User Interface Fixes: 

  - Sessions: Setting the session competition time sets the weigh-in time if it is empty, and vice-versa.  Once set the two fields need to be changed individually.  Clear the other field first if you want the automatic computation.
  - Registration and Weigh-in lists: Athletes eligible in multiple categories could appear multiple times.

- Excel Templates:

  - New template: Check-in sheet. Used to hand out promotional items to athletes, give access passes, etc. Template serves as example of jxls3 (see below)
  - Excel templates for documents can now be in [jxls3](https://jxls.sourceforge.net/) format.  A jxls3 template is detected from the presence of a `jx:area` directive in a note in cell A1 of the first sheet.

- Technical:

  - Updated to Vaadin 24.3.7, to update the Atmosphere push library.
  - Updated calls to MQTT to respect the 23-character limit on client identifiers

##### 47.0.1

- Fix for Record display on scoreboards; a missing fix from version 46.0.4 was merged.
  Records from national, continental, and world federations again appear in separate rows according to the name given, with one record box per age group and body weight.  A previous attempt to automatically accommodate conflicting names when loading from several federations has been undone. Conflicts in record names should be fixed in the source files.

##### 47.0.0

- Start numbers are now assigned by bodyweight category then by age group. A checkbox is available when it it desired to keep categories from the same age group together (i.e. kid categories first).  A separate option selects Masters order (older categories first).
- There is now the ability to have different styles for the result and attempt board styling depending on the platform (both on-site and on streaming)
  - An identifier for the current platform is added to the top-level `wrapper` element in the page.  The name is modified to only keep legal CSS characters (Latin letters, digits, hyphens and underscores). If you wish to use this feature, the platform will have to have a Latin name, or only digits.  A `_` will be added in front of a digits-only name to make it legal.
- When running on a laptop, it is now possible to create a zip backup of the  `local` directory (from the Settings page/Customization tab).   This makes it easier to upload flags, style sheets, and templates to cloud instances, or to create a standard kit within a federation.
  - When running in the cloud, the zip is loaded to the database, and unpacked when the application starts to provide the customization.
  - When running locally, there are two options
    1. Same as the cloud -- load the .zip inside the database, it is unpacked at startup, but the contents of the local directory in the installation directory is untouched.
    2. Delete the files inside the local directory inside the installation and replace them with the zip.  This is more risky but makes changing the .zip easier -- edit and re-package.
  - NOTE: if you have accented or non-Latin characters in your file names, the resulting zip is in "international" format and must be read using the 7z program that is available on Windows, Linux and MacOS, or using the `jar` command that comes with Java.
- Fix: Public Results: the Down icon was missing since version 45, is now back.
- Fix: If the advanced start book data entry (SBDE) spreadsheet was loaded, but owlcms was not restarted before the competition, multiple display update processes were present and could issue mutually contradictory display update instructions.
- Flic2 buttons: Added `;` and the numeric pad `=` as keyboard shortcuts for setting the clock to 2:00

##### Customization Notes

> - Some minor changes have been made to the colors.css, results.css and attemptboard.css files.

