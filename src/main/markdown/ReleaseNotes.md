



⚠️⚠️⚠️ 
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)** 
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**



<br>

**Maintenance Log**

59.1.0-rc02: Made logos available in templates

59.1.0-rc02: Adjustments to unit tests and systematic checks using null pointer analysis.

59.1.0-rc01: Final tweaks to multi-rank scoreboard to ignore age groups that don't award medals

**New in Release 59.1**

59.1.0: The logos (.png, .jpg, .jpeg) in the local/logos directory can now be included in the jxls3 Excel templates using the [jx:image](https://jxls.sourceforge.net/image.html) directive. For example `jx:image(src="logos['right.png']" lastCell="B3")` in cell B2 would copy the image local/logos/right.png in the range B2:B3.  Note that .jpg logos require imageType="JPEG" in the directive (not JPG)

59.1.0:  Added a Clear Body Weights button for simpler testing of weigh-ins

59.1.0: The multiple age groups scoreboard now works for Masters if there are multiple championships (for example, national and regional.) There will be one column per championship, as opposed to one column for each of the Masters multiple age groups.

59.1.0: The down signal/decision lights shown on the scoreboards are now properly styled as `flex`.  If you have customized your scoreboards,  you should check your `results.css` and align your `.athleteInfo .decisionBox`  and `.athleteInfo .timer` rules to match the ones from the standard scoreboards.

59.1.0: When the given names of athletes are provided in All Caps, they are now fixed to follow the usual capitalized first letter convention for each name. This is skipped for unicameral languages that don't make this distinction. A feature toggle `dontFixNames` is available to keep the All Caps if needed.

59.1.0: A Start List scoreboard is available for streaming.  Also available as a public scoreboard, useful if a large LED wall is present.

**New in Release 59.0**

59.0.0: An error message will be emitted if, when loading a session, an athlete has no category.  The athlete will be ignored and not added to the session.  This is a safety check if the weigh-in data entry forced saving without a category.

59.0.0: A new predefined styles directory "public" is available.  Meant to be used if there is a large scoreboard in the main room and a TV-like colourful display is desired.  The top of the scoreboard shows the current attempt information, and there is an opaque background.  Otherwise, It is the same as the "transparent" style meant for streaming.  

59.0.0: Added a Button Testing capability.  See the Referee section on the "running a session" page.  The new page is used to start a special break. It shows when the refereeing, timekeeping and jury buttons are pressed.  This works for all devices phone/tablet, keypad (USB/joystick/Bluetooth) or MQTT devices.

- If the feature toggle `mqttDownSignal` is present, a button to test a stand-alone down signal using MQTT messages is added.

59.0.0: Support for Japanese

50.0.0: When using public scoreboards, the display switches to medals when the announcer starts a medal ceremony from the "Pause" menu.  The font size used for that menu is now larger and more appropriate.

59.9.0: Improved the processing of decisions entered by the speaker (when flags are used, or if a referee button does not work). In such cases, the down signal is no longer shown.

59.0.0: When entering a body weight, if typing was slow, the athlete's category could be recomputed before the weight was typed in completely.  Now waits until the change is done by moving outside the field.

59.0.0: On the scoreboard, show the 3 best athletes from previous groups, in compliance with TCRR.  Previously, the predicted medal-winning athletes (including athletes from the current group) were shown.  Old behaviour can be obtained with toggle switch `medalistsAsLeaders`)

59.0.0: For formatting, the country is now set according to the browser's accepted languages preferences unless the locale has been forced in the database. Chrome, Edge and Safari include that information.  For Firefox, the user has to include the country information in their accepted languages, and if not, the server's country is used.

59.0.0: Experimental: Some federations show the jury decisions to the public.  A new jury decision scoreboard is available for this purpose.

59.0.0: The scoreboard would show unneeded separators when operating in "pure" IWF mode (only bodyweight categories) there were category switches from one athlete to the next.

59.0.0: Changes to enable custom look and feel for broadcasting special events without including them in the free distribution.  

- There are now three styling possibilities: default scoreboards, video styling (colourful with transparent scoreboards), and public scoreboard styling (possibly colourful like video, but with a background).  It is possible to set the video and/or public scoreboards to the same style as the default scoreboards.
- A web designer familiar with CSS styling can start with the provided styles and modify them as required.

59.0.0: New feature toggle `customTeamName` to allow including the Additional Info fields on the attempt board team line.   If the toggle is active, the attempt board will format the team name line using 4 values, using the translation string `AttemptBoard.TeamFormat`  .

- `{0}` is a count that represents the information available for the athlete.
  - 0 means no custom1 and no custom2.  
  - 1 means custom1 is present, but not custom2.  
  - 2 means custom2 is present, but not custom1. 
  - 3 means both custom1 and custom2 are present.  Present means "not null"  and "not blank/empty".
- `{1}` will be the team, `{2}` will be custom1 and `{3}` will be custom2
- A format string of the form `{0, choice, 0#{1}|1#{1}, {2}|2#{1}, {3}|3#{1}, {2}, {3}}` would cover all four cases.  See the Java definition of MessageFormat for details.


For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases) 
