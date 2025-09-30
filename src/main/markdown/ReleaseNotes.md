



⚠️⚠️⚠️ 
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)** 
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**Maintenance Log**

60.0.1: Adding the snatch and c&j declaration columns to the registration file did not work - the column name lookup was broken.

60.0.1: Work around an issue that would prevent listing athletes when some had no eligibility categories set

60.0.1: Fix for the list of athlete results not being available when multiple open categories overlapped exactly


**New in Release 61.0**

61.0.0: Cleaner processing of errors on the Documents preparation page.  Errors that take place while processing
the document are now shown in the dialog box. 

61.0.0: Fix: the athlete registration/weigh-in editing form recomputed the eligibility categories when opened on an athlete with no body weight.  This was wrongly ignoring the categories previously given in the registration Excel.

61.0.0: Fix: Excessive validation in the record Excel files meant that the usual convention of using 999 for the upper limit of superheavyweights did not work (record files expected the >110 or +110 format instead). For backward compatibility, any number over 199 will now be accepted and treated as superheavy; the output format will compensate as well.

61.0.0: Improvements to Registration and SBDE files

- The Gender can either be "M" or "F", or the translated value in the current language (e.g. "W")
- The page with session information can now have the columns reordered or removed
- The column headers can be in English or in the current language, but cannot be renamed.

61.0.0: When using a jury keypad with decision lights, and there is no deliberation or break going on, then the decision lights are reset by using the resume button.

61.0.0: Updates for introductions and technical officials

- Competition Doctor added as technical official role and to the introduction sheets
- Added a button to print the Introduction sheet from the weigh-in entry page (the introduction sheet is much easier to read by the speaker than the protocol)
- Added the reserve referee and reserve jury to the protocol sheets to the official document

61.0.0: Fixes for glitches in clock restart (reset of decisions) and forced time (missing/delayed MQTT events)

61.0.0: Technical Update to [Vaadin](https://vaadin.com/) version 24.8.7.   

- The main changes are to the Upload and Download mechanisms.
- Other dependencies were updated to match the expectations of Vaadin (netty, various commons packages)


For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases) 
