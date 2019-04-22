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
  - You will be prompted to extract the files to a directory.  Select the directory and perform the extraction.  
  - Running owlcms.exe from that location will start the server program in competition mode (see below if you want to run in demo mode with fake data).  You can then connect to the application using [http://localhost:8080](http://localhost:8080)

- On MacOS or Linux, use the following commands (substitute the proper numbers for x.y )

  ```bash
  java -jar owlcms-4.x.y.jar
  ```
- On all platforms, if you want to run in demo mode with fake athletes 
  ```bash
  java -DdemoMode=true -jar owlcms-4.x.y.jar
  ```

## Running on Heroku

### First time setup

- Go to page https://heroku.com
- Create a free account. Remember the login and password information.
- Create an app, pick an available name -- for the rest of this example, the name we use will be "myfederation"
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
  
- Open the application (or go to ``https://myfederation.herokuapp.com``). This will start the application. Heroku provides the database names and database login information automatically.
  
  ```bash
  heroku open
  ```
  
- If you want to run in demo mode with fictitious athletes, run the following commands before doing the deployment
  
  ```bash
  cp Procfile prodProcfile
  cp demoProcfile Procfile
  ```

Reference: https://devcenter.heroku.com/articles/deploying-executable-jar-files
