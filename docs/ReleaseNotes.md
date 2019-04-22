#Release Notes

##Release 2.20.3.1
* Change: it is now required to specify the athlete's category explicitly either at registration (when uploading an excel or csv file), or by editing the weigh-in value.  The system will no longer guess an unspecified category.
* Fix: the default value for using registration categories was wrong (bug introduced in 2.20.2)
* New Feature: Compute Robi points.  When editing the Excel templates, use ${l.robi} to access the robi points in protocol sheets and competition books. For competition books, you can use ${l.robiRank} for the ranking.
* New Feature: Added the robi points to the default Excel protocol sheets. Sinclair scores are still present.
* New Feature: Added extra sheets for Robi to the default Excel competition books. Sinclair ranking is still present.
* New Feature: Added the world record column to the category definition. Should a world record change, it is now possible to adjust the Robi points without waiting for an owlcms upgrade. 
* Fix: In version 2.20.1, using a database from version 2.19 would fail.  
    * If you are keeping your previous database, it is suggested that you assign age groups to your previously defined categories using the letters s, j, y, k or m as prefixes (see notes for release 2.20.1 for the list of age divisions -- the first letter is the prefix, and using no letter corresponds to the default IWF senior/junior categories)
    * Alternately, you may remove all your categories, the program will recreate the new ones.

##Release 2.20.2 (superseded by 2.20.3)
* Dead on arrival: a false positive test was caused by using the wrong configuration file during tests.

##Release 2.20.1
* Update: Use new IWF categories
* Improvement: Added the notion of Age Divisions.  The defined divisions are DEFAULT, SENIOR, JUNIOR, YOUTH, KIDS, MASTERS, A, B, C, D and TRADITIONAL.  A category is the combination of an Age Division with a weight class (except for Masters, where there are additional age groups).  In this way you can have Juniors and Seniors competing in different categories within the same competition, just mark the categories you need as active.  The category codes use the first letter of the division as a prefix (ex: sm67 and jm67 define senior and junior males under 67kg).  The default category does not use any prefix (m67 is effectively the same as sm67 and jm67 and ym67 and km67 and would be used if there are no age divisions in the competition). The A-D divisions are for your own needs (if you have different categories). The TRADITIONAL ("t") categories are for competitions that would still use the pre-2018 categories.

##Release 2.19.13
* Fix: Used a more robust way to ensure that the results page for the competition secretary shows updates, in particular when switching between groups lifting simultaneously on different platforms
* Improvement: Display the owlcms version number in the log at startup.

##Release 2.19.12
* Fix for Masters: When enforcing the starting total rule, the Masters values of 15kg (male) and 10kg (female) differential from qualifying total are used (instead of IWF value of 20kg for both genders).
* Updated rules: now comply with updated Masters rules for medal rankings -- now same rules as IWF. Bodyweight and age are no longer tie-breakers. Whoever achieved total first wins in case of a tie, an earlier competition group with same total wins.
* Fix: if no birth date was entered for a lifter, and the masters competition setting was true, an error would occur. Default values (1900-01-01) are now provided to encourage data capture.
* Improvement: The results page now updates automatically the display if the selected group is currently lifting.  The same screen allows printing of results.
* Improvement: Clarified documentation for configuring buttons when using keyboard-emulation devices for refereeing.

##Release 2.19.11 (BETA)
* Experimental: FEEDBACK/INDEPENDENT VERIFICATION SOUGHT -- Comply with TCRR2018 tie break rule: if tied, athlete in earlier group wins (same as for establishing a record). Note that the ranking shown on the scoreboard in this version is the ranking *within the current group*, as per current owlcms behavior.  Please look at a result sheet or the Results/Result Sheet page with *no group selected* in order to get the full competition rankings.  I will look at making the current group scoreboard rank match the overall rank in a future release.
* Fix: Handling of missing group competition time was not robust enough, causing intermittent errors depending  
* Fix: Made sure Release Notes and Hardware Configuration guides are included in the various documentation directories.

##Release 2.19.10 (BETA)
* Retired -- use 2.19.11 instead

##Release 2.19.9
* Fix: Selecting the session in the Results editing page would occasionally affect the current session for the announcer. Bug introduced in 2.19.8.
* Improvement: Clarified menu wording for the new Results menu (Follow the current group vs the results sheet and editing screen)

##Release 2.19.8
* New: the Results menu now proposes an auto-refreshing display of lifters, ranked in winning order, to be used by the competition secretary. There is no longer a need to refresh manually. The old page should only be used for editing a mistake in data entry when a group is not lifting.
* Change: a seldom if ever used option was removed on the competition data page. For youth competitions, an age limit could be specified, causing older lifters to lift as invitees and not ranked.
* Improvement: the new auto-refreshing secretary results menu should alleviate and hopefully eliminate database deadlock situations beyond the author's control (over-eager locking due to the user interface library erroneously inferring that rows were being edited).  This situation was occurring unpredictably (rarely) when running competition with simultaneous multiple platforms.

##Release 2.19.7
Fix: tie-break rules for total were incomplete - in some instances a smaller CJ did not break a same-total tie.
Fix: team membership/participation checkbox on the registration screen had been broken inadvertently

##Release 2.19.6
* Improvement: Start List now includes a second sheet that contains the members for each team/club (use the Start List button in Administration/Registration to generate)
* Improvement: There is now a CompetitionBook_SnCJTot template for competitions that award points for each of the lifts and total.  This was already possible with CompetitionBook_All, but there was no template for that specific case.
* Fix: Excel Template CompetitionBook_All had lost some of its sheets.  All predefined sheets are now back in.

##Release 2.19.5
Update: Sinclair coefficients updated to 2017-2020 olympiad values.

##Release 2.19.4
Fix: if using a .war file to deploy the application, the default was still the old-style values for the bodyweight and 20kg rules. Now fixed.

##Release 2.19.3
Fix: bodyweight tiebreak for total and associated test case fixed.
Fix: decision buttons on announcer screen were no longer operational on last attempt (unwanted side-effect of automatic progression clock fix in 2.19.2)
##Release 2.19.2
Fix: Entering a declaration for the current lifter that is the same as the automatic progression no longer stops clock
Fix: User interface now refers to the 20kg rule
Fix: The "clear lifts" button no longer generates errors regarding the 20kg rule being violated
Fix: It is now possible to edit entry totals on the Registration screen and Weigh-In screen (the latter is to allow for fixes should a misunderstanding have occurred)
Improvement: The missing amount is shown in red in a separate column if the 20kg rule is violated. This column is hidden if the rule is not enforced.

##Release 2.19.1
Beta: New display to show athlete bio notes in sync with competition.  See new entry under "Projectors".  The file athleteBio/membership#.html will be looked up for each athlete -- the membership number is taken from the registration excel or the registration page. Sample files and a README.html for instruction are found in the athleteBio directory (look under the Competition Management folder on Windows, or under the Web Application on Mac/Linux)
Fix: the new 90kg and >90kg categories are now created by default in new databases
Fix: the new 2017 rules for bodyweight and 20kg are now on by default on Windows (owlcms.properties had legacy-compatibility setting)

##Release 2.18.3
Improvement: force sound settings for the platform on every announce.
Change: Official 2017 rules are on by default (20kg rule for both men and women, no bodyweight tiebreak)
- to keep the old 15/20kg rule, set the parameter owlcms.useOld20_15Rule=true in your owlcms.properties (windows standalone) or catalina.properties (tomcat), you will get the new 2017 20kg rule for males and females alike.
- to keep the old bodyweight tiebreak rule, set the parameter owlcms.useOldBodyWeightTieBreakRule=true in your owlcms.properties (windows standalone) or catalina.properties (tomcat), you will get the new tiebreaking rule where the bodyweight is no longer considered.


##Release 2.18.2
* Note: by default this release uses the 2016 rules. You need to explicitly override the defaults to get the 2017 rules.
* Beta: Ticket 145:  2017 20kg rule. if you set the parameter owlcms.useOld20_15Rule=false in your owlcms.properties (windows standalone) or catalina.properties (tomcat), you will get the new 2017 20kg rule for males and females alike.
* Beta: Ticket 144:  2017 tiebreak rule. if you set the parameter owlcms.useOldBodyWeightTieBreakRule=false in your owlcms.properties (windows standalone) or catalina.properties (tomcat), you will get the new tiebreaking rule where the bodyweight is no longer considered.

##Release 2.18.1
* Note: this release still follows the 2012-2016 rules for tie-break and 15/20kg.  Next release will implement the 2017 rules.
* Improvement: Ticket 143: Display notification at weigh-in when 15/20kg rule is not respected for a lifter. This complies with IWF guidelines in TO training material regarding minimal declared weight. Display notification at weigh-in when 15/20kg rule is not respected for a lifter. Weigh-in screen also highlights lifters in violation and shows missing amounts for all lifters.
* Fix: Ticket 146: CSV registration files could not handle full birth date.  Fixed, and templates/RegistrationTemplate.csv file adjusted.  Also added templates/RegistrationTemplate_YOB.csv for people that run with owlcms.useBirthYear=true in their configuration file.
* Fix: Ticket 147: the pushFTP program to broadcast the result scoreboard would stop between sessions if the FTP server used had a short timeout.  Version 1.2 has been placed in the files section of the owlcms download site.

##Release 2.17.9
Improvement: Sinclair-Meltzer-Faber (SMF) coefficients have been officialized for masters weightlifting.  The program now uses the published list in the Masters protocol sheets and when the competition is set with "true" for the Masters setting.
Improvement: The US Weightlifting protocol sheet is now in the list of protocol sheets.

##Release 2.17.8
Fix: Issue 141: If a bookmarked link referred to a non-existent platform, inconsistent behaviour would ensue (either picking the default platform, with an exception, or an iobscure internal error).  An error message is now shown on the screen.
Documentation: a new document has been added to describe in more details the hardware required to run a competition under different scenarios.
Improvement: tweaked the colors for the DLP projectors -- will now be black on more subdued black and red

##Release 2.17.7
Fix: ...fr.xls competition book templates use year of birth instead of full birth date; the Excel format was wrong and has been fixed.
Experimental: Added a pushFTP Java program to send the scoreboard to a web site every few seconds (using FTP), and a sample HTML file for the public.
See http://sourceforge.net/projects/owlcms2/files/pushFTP for the files and a README.txt which provides instructions*.

##Release 2.17.6

Fix: Drop down to select protocol sheet and competition book templates would not show when running Tomcat from a directory that had a white space.
Improvement: Cleaned up writing to log files when several groups are going on at once.

## Release 2.17.5

* Improvement: Ticket 139: The attempt board can now be used as a refereeing hub by connecting bluetooth or USB devices (just like the athlete-facing display).  Useful for small club meets where a TV screen can be set to the side of the competition platform.
* Improvement: Better display of combined youth-jr-senior-masters group categories used when selecting the "Masters" protocol sheet and setting "is Masters" to true for a competition.  The display uses 17- 20- and 21 to stick with the Masters style of display (corresponding long forms are 0-17, 18-20 and 21+)
* Fix: When using multiple platforms, weigh-in and results screens no longer require user to select a platform
* Technical: Robot stress testing - Hitting "r" after clicking in a decision or current attempt browser main area will quickly go through all lifters in the current group, randomly deciding on good/bad lifts. Typical use is to keep a database from a prior competition (with weighed-in athletes) and use the Registration screen to clear the lifts for a group.  This is useful when setting up dual-gymnasium competitions and making sure the main laptop is up to par.

## Release 2.17.4

* Fix: Using Masters with lifters under 35 no longer crashes.  M17, M20 and M34 "fake" master age groups are used for youth, juniors and under 35 seniors. This provides a quick fix for multi-age competitions running at once -- athletes over 35 competing as seniors can be handled by temporarily making them 34 years old and fixing the date on the protocol sheet.
* Improvement: Issue 136: added Spanish-language versions of Excel templates for Athlete Card, Weigh-in/Starting weights sheets, and Protocol Sheet. In order to force spanish display, refer to the Frequently Asked Questions of the owlcms site (the code for Spanish is "es").
* Improvement: There is now an "Excel Templates" shortcut in the Competition Management desktop folder.  The templates are now fetched first under OWLCMS_HOME/templates which makes it easier to override them and keep the overridden versions when updating, then inside the web application (WEB-INF/classes/templates)

## Release 2.17.3
* Fix: Issue 132: Registration categories are now shown correctly in the user interface, projector screens and Excel templates. In order to use additional registration categories
  1) add additional categories using the Administration/Categories menu.  Start Male categories with "m" and Female categories with "f".  The suggestion is to use a suffix (such as "y" for youth, "j" for junior) -- so you would have a "m50y" youth category, or a "f59j" junior category.
  2) if running the Windows version, find the owlcms.properties file, edit it with Notepad, and add a line with
  owlcms.useRegistrationCategory=true
  3) if running a standalone Tomcat program, do the same but in the Tomcat catalina.properties file.
* Improvement: Issue 136: Spanish translations are now available for the user interface.  In order to force spanish display, refer to the Frequently Asked Questions of the owlcms site (the code for Spanish is "es").



Release 2.17.1
---------------
* Fix: Issue 133: Ability to have two or more countdown/​down/decision displays for the same group; Useful for euro-style large competitions were there is one announcer and two platforms.  Displays (and buttons) can be associated with the same platform and operate concurrently -- obviously, referees on the idle platform shouldn't use their buttons.
* Improvement: Issue 134: English label for "snatch" is too long on attempt board when the requested weight is high -- overlaps the plate display. Fixed by using "Att" as per IWF.
* Improvement: Issue 135: Display down signal on attempt board. For club meets it is now possible to use the attempt board as a "do-it-all" combo display (countdown, decision, down signal, requested weight, current lifter)
* Technical: General clean-up of application initialization code -- this could lead to "red box" error messages or mix-ups in languages for multi-language setups.

Release 2.16.13
---------------
* Fix: Issue 131: Programming error on the Results screen fixed ("Class Cast Exception")
* Improvement: fixed database backup/restore scripts to support new h2 1.4 database format in addition to 1.3

Release 2.16.12
---------------
*(use 2.16.13 instead which fixes a bug introduced in this release)*

* Fix: Issue 129: Sinclair display no longer shows lifters with no lifts done (also corrects a Java exception)
* Improvement: Issue 130: A confirmation is now required on the registration screen before deleting athletes or clearing lift values.

Release 2.16.11
---------------
* Fix: Issue 126: Clicking "announce" on the announcer screen would sometimes cause the marshall athlete card editor to switch to the current lifter.
* Fix: Issue 127: The warnings to the announcer that the marshall has changed requested weight for the athlete expected to lift (and vice-versa) were no longer operational.
* Fix: Issue 128: Deadlocks between the marshall and announcer screens (each waiting for the other to update information, forever).  There were a few instances where the code was locking the originating window instead of the receiving window.
* Change: the "Referee Testing" menu entry has been removed, as it is in fact obsolete.  Refereeing devices can be tested by selecting the "empty" group on the announcer console.

Release 2.16.10
--------------
Fix: Issue 125: For brand new installs of 2.16.9 only: the database created uses a new extension (.mv.db) and on subsequent restarts would not be recognized as being present. A new database would therefore get created on every run. This does NOT affect people who updated (database name *.h2.db)

Release 2.16.9
--------------
* Improvement: Updated support for Masters competitions:
  1) Added Masters_en.xls and CompetitionBook_Masters_en.xls as templates for protocol file and competition documents
  2) Enabled display of Age group/category to relevant screens and pages (age group is computed from birth date)
  3) Added Masters-specific tie-break rules (if same total at same body weight older lifter wins)
  These updates for Masters have not been tested as extensively as regular competitions.  Kindly do your own additional tests before using.

Release 2.16.8
--------------
* Fix: Issue 124: At the end of a large competition, previewing the Competition Book to predict team rankings while the competition was still running would sometimes create a locking issue in the database. The database engine (H2) has been updated to the current release (1.4) which handles concurrency better, and the code used for spreadsheet reporting has been rewritten to release resources imperatively.

Release 2.16.7 (beta)
--------------
* Improvement: Added a Sinclair Ranking screen for Sinclair-oriented competitions. Shows top 5 lifters, along with nb of kg required to win.
* Improvement: Main board is now a separate setting from other (warmup) result boards.  Public pause messages only show on the main board now.

Release 2.16.6
--------------
* Improvement: For Masters competitions, updated coefficient values to 2015 Sinclair-Meltzer-Faber table
* Improvement: OpenOffice/LibreOffice users should no longer have to define XLS mime type on Windows

Release 2.16.5
--------------
* Fix: Issue 123: Competition books now show team totals without unwarranted decimals
* Fix: Issue 122: Reverted to Java 32-bit mode install for Windows installer.  Installing Java automatically typically ends up installing the 32-bit version.  Installing the 64-bit version is painful, so we'll leave that out for the people who need to run a Tomcat server.
* Fix: Issue 121: Registration file uploads were broken under stand-alone Windows version (missing privilege).

Release 2.16.4
--------------
* Improvement: When running the Windows standalone version, a file called "owlcms.log" is now created that contains detailed logging.  The file is found under the "Configuration" shortcut found in the Competition Management folder on the desktop.
* Improvement: on a 64-bit Windows machine (all modern laptops), a 64-bit version of the Windows launcher will be used, and a file with memory parameters suitable for a modern laptop will be used. In particular, Java will be given 2GB as its preferred maximal memory
* Fix: uninstaller now removes start menu when install was performed in French.

Release 2.16.3
--------------
* Fix: Issue 116: Sound for down signal was no longer being emitted. Also restores the ability to test referee inputs by using the empty group.
* Fix: Issue 117: Some Danish screens were not correctly declaring the page locale as "da-DK"
* Improvement: added the category to the lifting order screen
* Fix: Issues 118, 119: Content of top part of main scoreboard was not refreshing correctly on end of group or end of break

Release 2.16.2
--------------
* Fix: Issue 115: Translations for languages other than English were not being displayed correctly (settings were not being picked up)

Release 2.16.1
--------------
* Improvement: Show the plates and requested weight on the attempt board during an intermission (to hide, announcer selects the blank group from drop down)

Release 2.16.0
--------------
* New: on all systems the location of data files is found in the following order
  1) the environment variable ${OWLCMS_HOME}
  2) The Java system variable owlcms.home
     - set in catalina.properties if running Tomcat as a service
     - set on the command line if running a stand-alone program (-Dowlcms.home=... )
This is makes it easier to back up and customize the data, templates, and options (see next item)
* New: options can be defined in an owlcms.properties file located in the owlcms home.  See the comments in that file for a
  description (or the Installation PDF guide).
* New: the Windows installer has been redone from scratch to create separate Program Files and AppData directories, and to
  better support Windows 8.
* New: the Windows standalone program has been repackaged using WinRun4J in order to support -Dowlcms.home, -Dowlcms.dir, -Dowlcms.locale
  and -Dowlcms.port, in addition to the previously supported OWLCMS_ environment variables.
* New: the Windows standalone program reads the owlcms.properties file. A "Configuration" shortcut is present in the shortcut directory.
* Update: updated the embedded Tomcat for the standalone Windows install to version 7.0.55

