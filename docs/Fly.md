## Installation on fly.io

Fly.io is a cloud service that is, in effect, free. The charges incurred for a single owlcms application and the matching remote scoreboard are less than the minimal amount for which they emit a bill.

In order to install an application you will need to log in to their site and then copy-paste a few commands given on this page.

#### Log in

- Go to the site https://fly.io and create an account if you don't have one, or login if you already do


- Go to https://fly.io/terminal page in your browser address bar.  This will allow you to copy-paste the commands given on this page.

  Note: You can also install the `fly` command on your own machine. See the [flyctl installation instructions](https://fly.io/docs/hands-on/install-flyctl/) if so.

#### Install owlcms

- The application names in fly.io are global.  If the name you want is already taken, you will be told.

- In this example, we will use `myclub` as the site name. The URL would then be `https://myclub.fly.dev`.  *You will of course replace `myclub` with your own site name everywhere in the commands given.*

- Click on the grey box below to copy the command.  Paste it to the command line interface (*when using the Web interface use the browser Edit menu* -- control-C and control-V don't work).   *Replace `myclub` with your own value.*

   ```bash
   fly launch --ha=false --vm-size shared-cpu-2x --image owlcms/owlcms:stable --name myclub
   ```

   You will be asked a few questions:

   - Organization:  use the default `Personal` organization.

   - Pick the region closest to you if asked

   - Important: say **y** (yes) when asked if you want a Postgres database

   - Hit enter to pick the default (**Development**) for the Postgres database

   - **n** (no) when asked if you want Postgres to stop after 1h of inactivity

   - **n** (no) when asked if you want an Upstash Redit

   - **y** (yes) when asked if you want to deploy

​	**You are now done and can use https://myclub.fly.dev**

- If you make a mistake and want to start over again, just issue the following commands `fly destroy --app myclub` and `fly destroy --app myclub-db`



### Updating to a new owlcms release

The `fly deploy` command fetches the newest version available and restarts the application (owlcms stores the versions in the public hub.docker.com repository)

To update to the latest stable version, go back to the command line interface and use the following command (replace `myclub` with your application name).   

```bash
fly deploy --image owlcms/owlcms:stable --app myclub
```

If you want to try a prerelease, use "owlcms/owlcms:prerelease" instead -- you can switch between stable and prerelease as you wish.



## Advanced topics

#### (Optional) Install the public results scoreboard

This second site will allow anyone in the world to watch the scoreboard, including the audience (useful if the scoreboard is missing, small or faint).   Something like `myclub-results` makes sense as a name, and people would then use `myclub-results.fly.dev` to reach the scoreboard from their phone.

1. Install public results.  This is the same as for owlcms except we do *NOT* need a database.

   - Choose a meaningful application name.
   - If asked, do NOT copy  existing TOML files
   - Select a region close to you if you are asked
   - Use the default personal environment when asked
   - **Important**: Do **NOT** ask for a postgres database: answer **n**
   - Do NOT ask for an Upstash Redis database: answer **n**
   - Answer **y** (yes)

   *Replace `myclub-results` with what you want as your public results name*

   ```bash
   fly launch --ha=false --vm-size shared-cpu-2x --image owlcms/publicresults:stable --name myclub-results
   ```

2. The two applications (owlcms and publicresults) need to trust one another. So we will create a secret phrase and configure both applications to use it. See [this page](PublicResults) for an overview of how owlcms and publicresults work together.  First we configure publicresults.  

   *Replace `myclub-results` with what you used as your public results name*

   > OWLCMS_UPDATEKEY is the setting name for the secret, and `MaryHadALittleLamb` is the secret phrase.  **Please use your own secret!** Do not put spaces around the `=` sign. 

    ```
    fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --app myclub-results
    ```

3. owlcms needs to know where public results is located in order to send it updates, and it needs to use the correct secret. 

   - Go to to the `Prepare Competition` section, on the `Settings and Language page`. Provide the public results location (in our example `https://myclub-results.fly.dev`) and the secret you configured above.

   **You are now done and can use https://myclub-results.fly.dev**



### Updating publicresults for new releases

The `fly deploy` command fetches the newest version available from the public hub.docker.com repository and restarts the application.

To update to the latest stable version

```
fly deploy --image owlcms/publicresults:stable --app myclub-results
```

### Controlling access to the owlcms application

In a gym setting, people can read the web addresses on the screens.  Because the cloud application is visible to the world, some "funny" person may be tempted to log in to the system and mess things up.  See this [page](AdvancedSystemSettings) for how to control access.

### Using your own site name

If you own your own domain, you can add names under your own domain to reach the fly.io applications.  This is done from [http://fly.io/dashboard](http://fly.io/dashboard).  Select your application, then go under the `Certificates` section to provide the required information.

### Scale-up and Scale-down of owlcms

If you run a very large competition, you may wish to increase the memory or use a dedicated CPU.   You can actually pause the competition and issue these commands if needed.  The program will restart, but since all the information is in the database, you will not lose anything, and all the screens should reconnect once the program is back.

The following commands, are examples: the first one sets the memory to 1GB. The second command would move to a dedicated CPU with 2GB. The third one would  use 2 dedicated CPUs with 4GB. Type the `fly platform show` command to see the options.

```
fly scale memory 1024 --app $FLY_APP
fly scale vm performance-1x --app $FLY_APP
fly scale vm performance-2x --app $FLY_APP
```

Running a performance-2x setup would cost 2US$ for one day -- 61$ per month.  So you would want to revert to the "free" setup after a competition

```
fly scale vm shared-cpu-2x --app $FLY_APP
```

