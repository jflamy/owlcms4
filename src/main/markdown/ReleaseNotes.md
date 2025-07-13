



⚠️⚠️⚠️ 
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)** 
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**



<br>

**New in Release 59.0**

59.0.0: Support for Japanese

59.0.0: New feature toggle `customTeamName` to allow including the Additional Info fields on the attempt board team line.   If the toggle is active, the attempt board will format the team name line using 4 values, using the translation string `AttemptBoard.TeamFormat`  .

- `{0}` is a count that represents the information available for the athlete.
  - 0 means no custom1 and no custom2.  
  - 1 means custom1 is present, but not custom2.  
  - 2 means custom2 is present, but not custom1. 
  - 3 means both custom1 and custom2 are present.  Present means "not null"  and "not blank/empty".
- `{1}` will be the team, `{2}` will be custom1 and `{3}` will be custom2
- A format string of the form `{0, choice, 0#{1}|1#{1}, {2}|2#{1}, {3}|3#{1}, {2}, {3}}` would cover all four cases.  See the Java definition of MessageFormat for details.

59.0.0: Changes to enable custom look and feel for broadcasting special events without including them in the free distribution.  

- There are now three styling possibilities: default scoreboards, video styling (colourful and with transparent scoreboards), and public scoreboard styling.  
- The main room public scoreboard styling can now differ from the default, and be more like the TV design (with backgrounds instead of transparency)
- A web designer familiar with CSS styling can start with the provided styles and modify them as required.


For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases) 
