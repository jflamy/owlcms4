# owlcms4 Setup Instructions

## Chrome configuration

On the computer used to connect referee devices and emit the down signal, you need to enable unattended sounds.

- in Chrome, go to page ``chrome://flags``  and search for ``autoplay policy`` 
  set it to ``No user gesture is required``


OR

- Add the following flag  `--autoplay-policy=no-user-gesture-required`
  to a desktop shortcut for chrome.  The path in the shortcut would look like this

  ```bash
  "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" --autoplay-policy=no-user-gesture-required
  ```

## Running stand-alone

- Get the current zip file from <https://github.com/jflamy/owlcms4/releases>

- On Windows, double click on the zip file, and double-click on `owlcms.exe` . You will be prompted to extract the files to a directory.  Select the directory and perform the extraction.  Running owlcms.exe from that location will start the server program in competition mode (see below if you want to run in demo mode with fake data).  You can then connect to the application using [http://localhost:8080](http://localhost:8080)

- On MacOS or Linux, or on Windows if you want to run demo mode

  ```bash
  java -jar owlcms-4.x.y.jar   # to run in production mode
  java -DdemoMode=true -jar owlcms-4.x.y.jar  # to run in demonstration mode
  ```

  

## Running on Heroku

### First time setup

- Create a free Heroku account  by going to https://heroku.com
- Go to page https://devcenter.heroku.com/articles/heroku-cli#download-and-install
  Get the Windows installer.  Ignore the note about installing Git, we don't need it.
  Run the installer to install the heroku CLI (command line interface)


- Go back to heroku.com account
- Create an app, pick an available name -- for the rest of this example, the name we use will be "myfederation"
- *To be completed -- not needed for demo mode*, *not yet supported*  Add a Postgres Database to the app.
- Click on the Windows icon at the bottom left and type cmd
  Start the windows command prompt window which shows up in the results and perform the following commands

|                               |                                                            |
| ----------------------------- | :--------------------------------------------------------- |
| `heroku login`                |                                                            |
| `heroku plugins:install java` | (installing the plugin is only needed the very first time) |

### Installing a version of owlcms

- Unzip the release to a directory

|                                                         |                                          |
| ------------------------------------------------------- | ---------------------------------------- |
| `cd` *the_directory_where_you_unzipped_the_files*       |                                          |
| `cp demoProcfile Procfile`                              | (to get demo mode) OR                    |
| `cp competitionProcfile Procfile`                       | (to run a real competition)              |
| `heroku deploy:jar owlcms-4.X.Y.jar --app myfederation` | (use the real numbers and the real name) |

Contents of demoProcfile

```
web: java -D"server.port"=$PORT -DdemoMode=true -jar owlcms-4.X.Y.jar
```

Contents of Procfile

```
web: java -D"server.port"=$PORT -jar owlcms-4.X.Y.jar
```

Reference: https://devcenter.heroku.com/articles/deploying-executable-jar-files