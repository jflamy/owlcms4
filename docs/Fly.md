## Installation on fly.io

Fly.io is a cloud service that is, in effect, free. Indeed, the charges for an owlcms application, its database the matching cloud scoreboard are less than the minimal billing amount and are therefore free.

To install in the cloud you will need to log in to their site and then copy-paste a few commands given on this page.

#### One-time tool installation and Log in

1. Go to the site https://fly.io and create an account if you don't have one

2. Install the `fly` command on your machine. This sends your instructions to fly.io to configure your applications.

      - For Windows users: 
      
         1. Click on the gray area just below and copy the installation command.
      
            ```
          powershell -Command "iwr https://fly.io/install.ps1 -useb | iex"
           ```
      
      2. Then click the `⊞` icon in your Windows taskbar at the bottom and type `cmd` to open a command window.
        3. Then paste the command and hit Enter.
      
      - For other users: See the [flyctl installation instructions](https://fly.io/docs/hands-on/install-flyctl/).  


   - Type the `fly auth login` command to login to your account.


#### Install owlcms

- The application names in fly.io are global.  If the name you want is already taken, you will be told.

   - In this example, we will use `myclub` as the site name. The URL would then be `https://myclub.fly.dev`.  *You will of course replace `myclub` with your own site name everywhere in the commands given.*

- Click on the grey box below to copy the command.  Paste it to the command line interface.  *Replace `myclub` with your own value.*

   ```bash
   fly launch --ha=false --vm-size shared-cpu-2x --image owlcms/owlcms:stable --name myclub
   ```


- If asked, do NOT copy  existing TOML files
- IMPORTANT: Answer **y** (Yes) when asked if you want to adjust the settings.  A web page will open.
  - We need a database
    - Find the setting for Postgres
    - Instead of `none` select `Fly Postgres`
    - Type a name for the database. We suggest that you add `-db` to your application name. 
      So in our example, the database name would be `myclub-db` .
    - Select the "Development" option instead of the "Production" (we don't need that much, and don't want to pay for it for nothing)
  - Go to the bottom and click on `Confirm Settings`
  - The creation process will take place and the application will be started.


​	**You are now done and can use https://myclub.fly.dev** (replacing *myclub* with your chosen name, of course)

- If you make a mistake and want to start over again, just issue the following commands 
  `fly destroy --app myclub` and `fly destroy --app myclub-db`



### Updating to a new owlcms release

The `fly deploy` command fetches the newest version available and restarts the application (owlcms stores the versions in the public hub.docker.com repository)

To update to the latest stable version, go back to the command line interface and use the following command (replace `myclub` with your application name).   

```bash
fly deploy --image owlcms/owlcms:stable --app myclub
```

If you want to try a prerelease, use `owlcms/owlcms:prerelease` instead -- you can switch between stable and prerelease as you wish.
You can also deploy a specific version, using for example ``owlcms/owlcms:44.6.0`



## Public Results Scoreboard (optional)

#### Install the public results scoreboard

This second site will allow anyone in the world to watch the scoreboard, including the audience (useful if the scoreboard is missing, small or faint).   Something like `myclub-results` makes sense as a name, and people would then use `myclub-results.fly.dev` to reach the scoreboard from their phone.

1. Install public results.  This is the same process as for owlcms except we do *NOT* need a database and no adjustments are needed.
   When asked if you want to adjust the settings, answer **n** (No)

   *Replace `myclub-results` with what you want as your public results name*

   ```bash
   fly launch --ha=false --vm-size shared-cpu-2x --image owlcms/publicresults:stable --name myclub-results
   ```

2. The two applications (owlcms and publicresults) need to trust one another. So we will create a secret phrase and configure both applications to use it. See [this page](PublicResults) for an overview of how owlcms and publicresults work together.  
   First we configure publicresults: *in the command below replace `myclub-results` with what you used as your public results name*

   > OWLCMS_UPDATEKEY is the setting name for the secret, and `MaryHadALittleLamb` is the secret phrase.  **Please use your own secret!** Do not put spaces around the `=` sign. 

    ```
    fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --app myclub-results
    ```

3. owlcms needs to know where public results is located in order to send it updates, and it needs to use the correct secret. 

   - In the web application, go to to the `Prepare Competition` section, on the `Settings and Language page`. Provide the public results location (in our example `https://myclub-results.fly.dev`) and the secret you configured above.

   **You are now done and can use https://myclub-results.fly.dev**

### Updating publicresults

The `fly deploy` command fetches the newest version available from the public hub.docker.com repository and restarts the application.  The same conventions as owlcms regarding the version numbers are used.

To update to the latest stable version

```
fly deploy --image owlcms/publicresults:stable --app myclub-results
```



## Advanced Topics

### Controlling access to the owlcms application

In a gym setting, people can read the web addresses on the screens.  Because the cloud application is visible to the world, some "funny" person may be tempted to log in to the system and mess things up.  See this [page](AdvancedSystemSettings) for how to control access.

### Using your own site name

If you own a domain, you can add names under the domain to reach the fly.io applications.  This is done from [http://fly.io/dashboard](http://fly.io/dashboard).  Select your application, then go under the `Certificates` section to provide the required information.

### Scale-up and Scale-down of owlcms

If you run a very large competition, you may wish to increase the memory or use a dedicated CPU.   You can actually pause the competition and issue these commands if needed.  The program will restart, but since all the information is in the database, you will not lose anything, and all the screens should reconnect once the program is back.

The following commands are examples: the first one sets the memory to 1GB. The second command would move to a dedicated CPU with 2GB. The third one would  use 2 dedicated CPUs with 4GB. Type the `fly platform show` command to see the options.

```
fly scale memory 1024 --app $FLY_APP
fly scale vm performance-1x --app $FLY_APP
fly scale vm performance-2x --app $FLY_APP
```

Running a performance-2x setup would cost 2US$ for one day -- 61$ per month.  So you will want to revert to the "free" setup after a competition

```
fly scale vm shared-cpu-2x --app $FLY_APP
```

