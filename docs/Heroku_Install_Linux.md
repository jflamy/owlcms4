# Install the Heroku deployment tools (Mac OS / Linux)

The following tools are required to install or update OWLCMS4 on the cloud.  <u>This process is only required once</u>.

### Install the Heroku Command-Line Interface

-  Go to page https://devcenter.heroku.com/articles/heroku-cli#download-and-install

- For MacOS and Linux, refer to the instructions given on  https://devcenter.heroku.com/articles/heroku-cli#download-and-install for your installation type. 
  
- Once done, open a terminal shell (on MacOS, Applications → Utilities → Terminal) 
  
- Run the following commands
  
  ```bash
  heroku login
  heroku plugins:install java 
  ```
These commands are only needed once.  The `login` command will open a browser window and ask you to login on your Heroku account.

### Install Java

The plugin we just downloaded above requires Java to operate.

- Go to https://adoptopenjdk.net/ .  
- Download the JDK8 version for your environment and install it -- the default options are fine. 

### Define your application name

- Time-saving tip: if you want to avoid typing `--app myHerokuAppName` on all the commands, you can define an environment variable called `HEROKU_APP` with your application name.  You only need to do this <u>once</u>.

  - Start a terminal shell **Applications → Utilities → Terminal** on Mac OS

  - Type
  ```
    touch ~/.bash_profile
    open ~/.bash_profile
  ```
  
  - Add the following line:  
    ```export HEROKU_APP=owlcms4```
  
    If your .bash_profile is not empty, there are probably other lines that start with "export", add the line with the other export statements.
  
  - Update your settings by typing
  
    ```
    . ~/.bash_profile
    ```

