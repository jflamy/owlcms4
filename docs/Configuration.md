## Configuration Parameters

Parameters can be set in several ways:

1. As Java System Properties
   1. On Mac OS and Linux, on the java command line using the `-Dvariable=value` syntax, immediately after the java word.
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
| initialData               | OWLCMS_INITIALDATA        | EMPTY_COMPETITION | Possible Values: <br />EMPTY_COMPETITION : empty database, ready for competition.<br />LARGEGROUP_DEMO : Insert a full 20-athlete group and typical-sized groups<br />SINGLE_ATHLETE_GROUPS : Insert groups with a single athlete, useful to rehearse breaks and end of group situations. |
| masters                   | OWLCMS_MASTERS            | false             | Change the weigh-in order to follow Masters conventions (older groups first) |
| locale                    | OWLCMS_LOCALE             |                   | if locale is not set, the language of a given display will be that of the requesting browser.  If locale is set to an [ISO 639-1 language code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) then that language will be used for all displays.<br />Optionally, there can be an [ISO 3166-2 country code](https://en.wikipedia.org/wiki/List_of_ISO_3166_country_codes) appended after an underscore.<br />Therefore, `fr` designates French, and `fr_CA` designates the Canadian variant for French.<br /><u>Has priority over the locale set in the database.</u> |
| pin                       | OWLCMS_PIN                |                   | If defined, the provided PIN will be required as a password when a user connects to owlcms.<br /><u>Has priority over the PIN/Password set in the application database. Use an empty PIN to get access to a database where you have set a PIN but forgotten it.</u> |
| ip                        | OWLCMS_IP                 |                   | If defined, connections will only be accepted from the address specified (or one of the comma-separated addresses).  Each address can be numerical like `24.157.203.237` or a fully qualified domain name.<br />A PIN is still required. See OWLCMS_BACKDOOR to allow passwordless access.<br /><u>Has priority over the access list set in the application database.</u> |
| backdoor                  | OWLCMS_BACKDOOR           |                   | If defined, no password will be required to access the owlcms application from the listed addresses. <br />-  Can be used for a cloud-based competition to avoid having to enter passwords at the competition site (use the public address of the competition location).<br />-  Can be used during virtual competitions to allow the video editing station to access the owlcms screens without passwords.  This would be the public IP address of the video producer.<br /><u>Has priority over the backdoor setting in the database.</u> |
| mqttServer                | OWLCMS_MQTTSERVER         |                   | If defined, owlcms will connect to the MQTT host and listen for referee decisions sent over MQTT, and will enable sending  reminders  or summoning referees. |
| mqttPort                  | OWLCMS_MQTTPORT           | 1883              | Default is the non-TLS connection.                           |
| mqttUserName              | OWLCMS_MQTTUSERNAME       |                   | Login for MQTT server                                        |
| mqttPassword              | OWLCMS_MQTTPASSWORD       |                   | Password for MQTT server.  The file from which this value is fetched should be protected.  Do not add the actual cleartext value to a command-line parameter in a script. |
| H2ServerPort              | OWLCMS_H2SERVERPORT       |                   | Normally given as 9092.  owlcms will tell its embedded H2 server to listen on this port.  This enables the h2.jar file to be run and start an H2 console.  In H2 console, use an URL of the form `jdbc:h2:tcp://localhost:9092/path`<br />where path is something like `c:/.../owlcms/database/owlcms-h2v2` . There is no .db extension at the end, replace `...` with the the actual path. Forward slashes are used even on Windows. |

### JDBC Parameters

The following parameters are used to control where the database is found.  For Postgresql, it is usually more convenient to use Postgres-specific values, see below.  When using H2, these values are normally computed by the program, and don't need to be changed, except perhaps when using Docker and storing the database on a volume.

These values are provided as environment variables or as Java definitions.  When passing them as Java definitions, use the syntax `-DJDBC_DATABASE_USERNAME=owlcms`  (each parameter has a separate `-D`)  When using environment variables, there is no `-D`. 

| Environment Variable Name | Description                                                  |
| ------------------------- | ------------------------------------------------------------ |
| JDBC_DATABASE_URL         | H2 default:  `jdbc:h2:file:./database/owlcms-h2-v2`<br />Postgres format: `jdbc:postgresql://localhost:5432/owlcms_db` |
| JDBC_DATABASE_USERNAME    | username for the database                                    |
| JDBC_DATABASE_PASSWORD    | password for the database.                                   |


### Postgresql Parameters

These parameters can be used instead of the JDBC parameters if the database is Postgres.  For example, these are the parameters used with Heroku cloud or with Docker. 

These values are provided as environment variables or as Java definitions.  When passing them as Java definitions, use the syntax `-DPOSTGRES_PORT=5432` (each parameter has a separate `-D`)  When using environment variables, there is no `-D`. 

| Environment Variable Name | Description                                              |
| ------------------------- | -------------------------------------------------------- |
| POSTGRES_HOST             | address of postgres server                               |
| POSTGRES_PORT             | port number of the postgres server (5432 is the default) |
| POSTGRES_DB               | name of postgres database                                |
| POSTGRES_USER             | name of postgres user                                    |
| POSTGRES_PASSWORD         | password for postgres password                           |