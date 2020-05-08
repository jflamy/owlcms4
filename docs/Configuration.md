## Configuration Parameters

Parameters can be set in several ways:

1. As Java System Properties
   1. On Mac OS and Linux, on the java command line using the `-Dvariable=value`syntax, immediately after the java word.
      `java -Dport=80 owlcms.jar`
   2. On Windows, in the `owlcms.l4j.ini` file which is read by the `owlcms.exe` file
   3. On Heroku, it is easier to use Environment Variables (see below)
2. As Environment Variables.  See the table below to see the correspondence with System Properties
   1. On Windows, these are set using the control panel.  Click on the windows icon at the bottom left and type `envi` .  Select `Edit the Environment Variables for your account` and add the variable name and value you need.
      Setting the variable `OWLCMS_PORT` to the value `80` is the same as using `-Dport=80`. 
   2. On Linux and Mac OS, these are set in the `~/.bash_profile` file.  Google a tutorial if not familiar with this process.
   3. On Heroku, on the `Settings` page for your application, under `Config Vars`.
   4. On Kubernetes, as part of a secrets or configmap section in a manifest

| System Property Name (-D) | Environment Variable Name | Default Value     | Description                                                  |
| ------------------------- | ------------------------- | ----------------- | ------------------------------------------------------------ |
| port                      | OWLCMS_PORT               | 8080              | HTTP Port used by the various displays                       |
| memoryMode                | OWLCMS_MEMORYMODE         | false             | if true, run an in-memory copy of the data.  Data is lost when the server is stopped. Useful for testing, the data in the regular database is not touched. |
| resetMode                 | OWLCMS_RESETMODE          | false             | If true, erase the database completely and recreate the database tables. |
| initialData               | OWLCMS_INITIALDATA        | EMPTY_COMPETITION | Possible Values: <br />EMPTY_COMPETITION : empty database, ready for competition.<br />LARGEGROUP_DEMO : Insert two full 20-athlete groups<br />SINGLE_ATHLETE_GROUPS : Insert groups with a single athlete, useful to rehearse breaks and end of group situations. |
| demoMode                  | OWLCMS_DEMOMODE           | false             | if true, same as resetMode=true and memoryMode=true and devMode=true |
| masters                   | OWLCMS_MASTERS            | false             | Run the competition according to Masters rules.  Masters mode can be set on the Competition Information page. |
| locale                    | OWLCMS_LOCALE             |                   | if locale is not set, the language of a given display will be that of the requesting browser.  If locale is set to an [ISO 639-1 language code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) then that language will be used for all displays.<br />Optionally, there can be an [ISO 3166-2 country code](https://en.wikipedia.org/wiki/List_of_ISO_3166_country_codes) appended after an underscore.<br />Therefore, `fr` designates French, and `fr_CA` designates the Canadian variant for French.<br />Currently available locales are<br />`en`(English), `fr`(French), `fr_CA` (Canadian French), `da` (Danish), `sp` (Spanish) and `ru`(Russian). |
| pin                       | OWLCMS_PIN                |                   | If defined, the provided PIN will be required as a password when a user connects to owlcms. |
| ip                        | OWLCMS_IP                 |                   | If defined, connections will only be accepted from the address specified (or one of the comma-separated addresses).  Each address can be numerical like `24.157.203.237` or a fully qualified domain name. |

### Legacy Options

The following options are deprecated, and will progressively be removed from the configuration scripts.

| System Property Name (-D) | Environment Variable Name | Default Value | Description                                                  |
| ------------------------- | ------------------------- | ------------- | ------------------------------------------------------------ |
| devMode                   | OWLCMS_DEVMODE            | false         | Insert two full 20-athlete groups. <br />Replaced by -DinitialData=LARGEGROUP_DEMO or OWLCMS_INITIALDATA=LARGEGROUP_DEMO |
| testMode                  | OWLCMS_TESTMODE           | false         | Insert two one-athlete test groups.<br />Replaced by<br />-DinitialData=SINGLE_ATHLETE_GROUPS or OWLCMS_INITIALDATA=SINGLE_ATHLETE_GROUPS |
| demoMode                  | OWLCMS_DEMOMODE           | false         | if true, same as memoryMode=true and<br />-DinitialData=LARGEGROUP_DEMO |
| locale                    | LOCALE                    |               | use OWLCMS_LOCALE                                            |
| PIN                       | PIN                       |               | Use OWLCMS_PIN                                               |
| IP                        | IP                        |               | Use OWLCMS_IP                                                |