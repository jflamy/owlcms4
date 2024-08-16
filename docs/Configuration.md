## Configuration Parameters

Parameters can be set in several ways:

1. As Java System Properties
   1. On Mac OS, Linux and RaspberryOS, on the java command line using the `-Dvariable=value` syntax, immediately after the java word.
      `java -Dport=80 owlcms.jar`
   2. On Windows, in the `owlcms.l4j.ini` file which is read by the `owlcms.exe` file
2. As Environment Variables.  See the table below to see the correspondence with System Properties
   1. On Windows, these are set using the control panel.  Click on the windows icon at the bottom left and type `envi` .  Select `Edit the Environment Variables for your account` and add the variable name and value you need.
      Setting the variable `OWLCMS_PORT` to the value `80` is the same as using `-Dport=80`.
   2. On Mac OS, Linux and RaspberryOS, these are set in the `~/.bash_profile` file.  Google a tutorial if not familiar with this process.
   3. On Fly.io, use the `flyctl secrets set NAM1E=VALUE1 NAME2=VALUE2`... command.
   4. In Docker, use the ENV command.

| System Property Name (-D) | Environment Variable Name | Description                                                  |
| ------------------------- | ------------------------- | ------------------------------------------------------------ |
| port                      | OWLCMS_PORT               | default = 8080<br />HTTP Port used by the various displays   |
| memoryMode                | OWLCMS_MEMORYMODE         | default=false<br />if true, run an in-memory copy of the data.  Data is lost when the server is stopped. Useful for testing, the data in the regular database is not touched. |
| resetMode                 | OWLCMS_RESETMODE          | default=false<br />If true, erase the database completely and recreate the database tables using the current OWLCMS_INITIALDATA (by default, EMPTY_COMPETITION) |
| initialData               | OWLCMS_INITIALDATA        | default=EMPTY_COMPETITION<br />Used together with OWLCMS_RESETMODE=true<br />Possible Values: <br />EMPTY_COMPETITION : empty database, ready for competition.<br />LARGEGROUP_DEMO : Insert a full 20-athlete group and typical-sized groups<br />SINGLE_ATHLETE_GROUPS : Insert groups with a single athlete, useful to rehearse breaks and end-of-group situations.<br />BENCHMARK: insert 80+ sessions of 14 athletes running on 4 concurrent platforms for running large stress tests. |
| masters                   | OWLCMS_MASTERS            | default=false<br />Change the weigh-in order to follow Masters conventions (older groups first) |
| locale                    | OWLCMS_LOCALE             | if locale is not set, the language of a given display will be that of the requesting browser.  If locale is set to an [ISO 639-1 language code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) then that language will be used for all displays.<br />Optionally, there can be an [ISO 3166-2 country code](https://en.wikipedia.org/wiki/List_of_ISO_3166_country_codes) appended after an underscore.<br />Therefore, `fr` designates French, and `fr_CA` designates the Canadian variant for French.<br /><u>Has priority over the locale set in the database.</u> |
| pin                       | OWLCMS_PIN                | If defined, the provided PIN will be required as a password when a user connects to owlcms.<br /><u>Has priority over the PIN/Password set in the application database. Use an empty PIN to get access to a database where you have set a PIN but forgotten it.</u> |
| ip                        | OWLCMS_IP                 | If defined, connections will only be accepted from the address specified (or one of the comma-separated addresses).  Each address can be numerical like `24.157.203.237` or a fully qualified domain name.<br />A PIN is still required. See OWLCMS_BACKDOOR to allow passwordless access.<br /><u>Has priority over the access list set in the application database.</u> |
| backdoor                  | OWLCMS_BACKDOOR           | If defined, no password will be required to access the owlcms application from the listed addresses. <br />-  Can be used for a cloud-based competition to avoid having to enter passwords at the competition site (use the public address of the competition location).<br />-  Can be used during virtual competitions to allow the video editing station to access the owlcms screens without passwords.  This would be the public IP address of the video producer.<br /><u>Has priority over the backdoor setting in the database.</u> |
| mqttServer                | OWLCMS_MQTTSERVER         | If defined, owlcms will connect to the MQTT host and listen for referee decisions sent over MQTT, and will enable sending  reminders  or summoning referees.<br />If NOT defined, owlcms starts an MQTT server itself, and connects to the embedded server.<br />Note that if you set this to 127.0.0.1, owlcms will try to connect to a locally running MQTT Server, and will NOT start its own. |
| mqttPort                  | OWLCMS_MQTTPORT           | default=1883<br />When running the embedded MQTT server, this is the port that clients will use, and only non-TLS connections are accepted.<br />When connecting to an external server, if the port starts with 8 then a TLS connection will be used (mqtts without mutual authentication - no client certificate is used). |
| mqttUserName              | OWLCMS_MQTTUSERNAME       | Login for MQTT server.<br />When running the embedded server, if this is empty any login/password combination will work for the clients. |
| mqttPassword              | OWLCMS_MQTTPASSWORD       | Password for MQTT server<br />If running the embedded server, this is the password that will be expected from the clients.<br />When using an external server, this is the password to use<br />The file from which this value is fetched should be protected from indiscrete eyes. |
| H2ServerPort              | OWLCMS_H2SERVERPORT       | Normally absent.<br />If present, the usual value is 9092, and owlcms will tell its embedded H2 server to listen on this port.  This enables the h2.jar file to be run and start an H2 console.  In H2 console, use an URL of the form `jdbc:h2:tcp://localhost:9092/path`<br />where path is something like `c:/.../owlcms/database/owlcms-h2v2` . There is no .db extension at the end, replace `...` with the the actual path. Forward slashes are used even on Windows. |
| enableEmbeddedMqtt        | OWLCMS_ENABLEEMBEDDEDMQTT | default=true<br />If explicitly set to false, the embedded MQTT server will not be started. |
| publicDemo                | OWLCMS_PUBLICDEMO         | If present, gives the number of seconds before the system exits. A warning is given beforehand.  When running under Kubernetes or under Docker with a restart policy, the process is immediately respawned from scratch. Windows will reload as soon as the site comes back, but with the clean data. |
| cssEdit                   | OWLCMS_CSSEDIT            | default=false<br />If false, a fake timestamp is added to the file names, to force the browser to fetch them again. The fake timestamp changes when the server is restarted.  Restarting the server ensures that the files are fetched again.<br />If true, the css and image files are fetched using the same name they have on the disk.  This makes it easier to use the development mode features of the browser to work. |
| useCompetitionDate        | OWLCMS_USECOMPETITIONDATE | default=false<br />If present and true, the ages will be computed relative to the stored competition date.  Useful when loading a database from a previous year. |
| featureSwitches           | OWLCMS_FEATURESWITCHES    | List of feature switches.  Overrides the ones in the database. |

### JDBC Parameters

The following parameters are used to control where the database is found.  For Postgresql, it is usually more convenient to use Postgres-specific values, see below.  When using H2, these values are normally computed by the program, and don't need to be changed, except perhaps when using Docker and storing the database on a volume.

These values are provided as environment variables or as Java definitions.  When passing them as Java definitions, use the syntax `-DJDBC_DATABASE_USERNAME=owlcms`  (each parameter has a separate `-D`)  When using environment variables, there is no `-D`. 

| Environment Variable Name | Description                                                  |
| ------------------------- | ------------------------------------------------------------ |
| JDBC_DATABASE_URL         | H2 default:  `jdbc:h2:file:./database/owlcms-h2-v2`<br />Postgres format: `jdbc:postgresql://localhost:5432/owlcms_db` |
| JDBC_DATABASE_USERNAME    | username for the database                                    |
| JDBC_DATABASE_PASSWORD    | password for the database.                                   |


### Postgresql Parameters

These parameters can be used instead of the JDBC parameters if the database is Postgres.  For example, these are the parameters used with Docker. 

These values are provided as environment variables or as Java definitions.  When passing them as Java definitions, use the syntax `-DPOSTGRES_PORT=5432` (each parameter has a separate `-D`)  When using environment variables, there is no `-D`. 

| Environment Variable Name | Description                                              |
| ------------------------- | -------------------------------------------------------- |
| POSTGRES_HOST             | address of postgres server                               |
| POSTGRES_PORT             | port number of the postgres server (5432 is the default) |
| POSTGRES_DB               | name of postgres database                                |
| POSTGRES_USER             | name of postgres user                                    |
| POSTGRES_PASSWORD         | password for postgres password                           |