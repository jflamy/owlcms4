# owlcms4 Setup Instructions

## Chrome configuration

Recent versions of Chrome no longer allow web pages to emit sounds by themselves.  In order to hear the signals from the clock and the down signal, we have to override this setting.

- in Chrome, go to page ``chrome://flags``  and search for ``autoplay policy``  in the search bar.
  Set the parameter to ``No user gesture is required``


OR

- Create a shortcut to chrome and add the following flag
  ```bash
   --autoplay-policy=no-user-gesture-required
  ```
  The path in the shortcut would look like this
  ```bash
  "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" --autoplay-policy=no-user-gesture-required
  
  ```

## Running stand-alone

- Get the current zip file from <https://github.com/jflamy/owlcms4/releases>

- On Windows,

  - double click on the zip file, and double-click on `owlcms.exe` .
  - You will be prompted to extract the files to a directory.  Select a directory and perform the extraction.
  - Running owlcms.exe from that location will start the server program in competition mode (see below if you want to run in demo mode with fake data).  You can then connect to the application using [http://localhost:8080](http://localhost:8080)
  - The database will be created in a directory named `database` next to owlcms.exe

- On MacOS or Linux, use the following commands (substitute the proper numbers for x.y ) to run the program

  ```bash
  java -jar owlcms-4.x.y.jar
  ```
- On all platforms, if you want to run in demo mode with fake athletes, reset every time the program is launched
  ```bash
  java -DdemoMode=true -jar owlcms-4.x.y.jar
  ```

## Running on Heroku

### First time setup

- Go to page https://heroku.com
- Create a free account. Remember the login and password information.
- Create an app -- this is the name under which your cloud copy of owlcms will be known. Pick an available name, typically the name of your club or federation. For the rest of this example, the name we use will be "myfederation"
- On the application configuration page, add the Postgres plugin.  This will automatically associate a free database with your application.
- Go to page https://devcenter.heroku.com/articles/heroku-cli#download-and-install
- Get the Windows installer.  Ignore the note about installing Git, we don't need actually need it.
  
- Run the installer to install the heroku CLI (command line interface)
- Click on the Windows icon at the bottom left and type cmd
  Start the windows command prompt window which shows up in the results and perform the following commands

  ```bash
  heroku login
  heroku plugins:install java 
  ```
  These commands are only needed once.

### Deploying a version of owlcms to Heroku

- Download a release and unzip it release to a directory by double-clicking on it. 
  
- Start a command shell and go to the directory where you unzipped the files
  ```bash
   cd *the_directory_where_you_unzipped_the_files*
  ```

- Run the deploy command (replace X.Y and myfederation with the proper values)

  ```bash
   heroku deploy:jar owlcms-4.X.Y.jar --app myfederation 
  ```
  
- Open the application using the following command (or go to ``https://myfederation.herokuapp.com``). This will start the application. Heroku provides the database names and database login information automatically.  See below for securing the application for actual competition usage.
  
  ```bash
  heroku open --app myfederation
  ```
  
- If you want to run in demo mode with fictitious athletes, run the following commands before doing the deployment. This adds the `-DdemoMode=true` flag to tell owlcms to reset on every start and recreate the fake data.
  
  ```bash
  cp Procfile prodProcfile
  cp demoProcfile Procfile
  ```

Reference: https://devcenter.heroku.com/articles/deploying-executable-jar-files

## Controlling access to the application

In actual competition settings, malicious users may know the password to your WiFi.  In cloud settings, the application address will be visible to attendees on some screens.  It is therefore recommended to limit access to the application.  This is done by setting two environment variables:

- `IP` is a comma-separated list of allowed addresses.  It is used when connecting to the cloud from a router on the competition site. In order to find the proper value, go to https://google.com and type 
  `my ip`
  in the search box.  This will display the address of your competition site router as seen from the cloud.  You should see a set of four numbers separated by dots like `24.157.203.247`                                        

- `PIN` is an arbitrary strings of characters that will be requested when starting the first screen whenever you start a new session (typically, once per browser, or when the system is restarted).  You can use it whether or not you are connected to the cloud.

### Securing a local installation

In order to require a PIN on a local installation (running the program on a computer at the local competition site), start the program as follows

- Under Windows 10, right click on the Windows icon at the bottom left and select the option to start a command prompt.  Change directory to where the program was extracted.

```bash
set PIN=1234
java -jar owlcms-4.X.Y.jar
```

- Under Mac or Linux, start a terminal shell and change directory to where the program was extracted.

```bash
PIN=1234 java -jar owlcms-4.X.Y.jar
```

### Securing a Heroku cloud installation

The simplest way is to use the Heroku dashboard for the application and add the IP and PIN variables under the Settings tab, in the Config Vars section.

![pin](img\Heroku\pin.png)