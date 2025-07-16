



⚠️⚠️⚠️ 
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)** 
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**



<br>

**New in Release 59.0**

59.0.0: Support for Japanese

59.0.0: When entering a body weight, there could be premature recalculations of categories if the typing was slow.  Now waits until the change is done by moving outside the field.

59.0.0: Experimental: Some federations show the jury decisions to the public.  A new jury decision scoreboard is available for this purpose.

59.0.0: On the scoreboard, show only the leaders from previous groups, in compliance with TCRR.  Previously, the predicted medal-winning athletes (including athletes from the current group) were shown.  Old behaviour can be obtained with toggle switch `medalistsAsLeaders`)

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
