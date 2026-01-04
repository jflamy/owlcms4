## Installation on fly.io

Fly.io is a cloud service that is, in effect, free. The charges incurred for a single owlcms application and the matching remote scoreboard are less than the minimal amount for which they emit a bill.

In order to install an application you will need to log in to their site and then copy-paste a few commands given on this page.

### Log in

Go to the site https://fly.io

If you do not have an account, create one.  Running owlcms requires an account, but you will not actually get billed because the fees are lower than their minimum



### Start the Command-Line Interface (CLI)

Installing the application requires copy-pasting a few commands to a command-line interface.  There are two ways to do this.

1. **Web CLI Interface**.  This is the faster, and therefore preferred, approach, but it is a new feature and occasionally there are issues.  So try this first, and if something fails, use the second approach.

   Go the https://fly.io    If you see a "use the Web CLI" button, click it, otherwise type https://fly.io/terminal in your browser address bar yourself.   

2. **Local CLI Install** You may prefer to install the `fly` command on your own machine. See the [flyctl installation instructions](https://fly.io/docs/hands-on/install-flyctl/) for your kind of setup (Windows, Mac or Linux)



### Install owlcms

Even if you intend to run your competition on a laptop, it is convenient to have a cloud-based version. You can then share the work of setting up the competition with others, and when done, simply export the database and import it on your laptop.

1. **Choose an application name.**

   The application names in fly.io are global.  If the name you want is already taken, you will be told.  If your club is named `myclub`,  you might pick `myclub` as the name, and the URL will be `https://myclub.fly.dev`

   - If you are running on the Web CLI, on a Mac, or on Linux, type this command (replace `myclub` with what you want)

      ```
      export FLY_APP=myclub
      ```

   - If you are running on Windows, type this command (replace `myclub` with what you want).  *There must be no spaces around the `=`* *sign*.

      ```
      set FLY_APP=myclub
      ```

3. **Create the owlcms application.**
   *Click on the grey box below to copy the command.  Paste it to the command line interface (if using the Web interface use the browser Edit menu)*

   ```bash
   fly app create --name $FLY_APP
   ```

   - Organization:  use the default `Personal` organization.


3. **Create the database** that will store the competition information (*click on the box to copy*)

   ```bash
   fly postgres create --name $FLY_APP-db
   ```

   - Organization:  use the default `Personal` organization.

   - Configuration of the Postgres database: Choose the default option  `Development`

   - Region: pick the one closest to you if asked

   - Stop after 1 hour:  Answer **n (No)** 

3. **Link the database** to the application

   ```bash
   fly postgres attach $FLY_APP-db --app $FLY_APP
   ```

5. **Start the application** with the proper size

   ```bash
   fly deploy --ha=false --vm-size shared-cpu-2x --image owlcms/owlcms:stable --app $FLY_APP
   ```



**You are now done and can use https://myclub.fly.dev**



### Updating to a new owlcms release

The `fly deploy` command fetches the newest version available and restarts the application (owlcms stores the versions in the public hub.docker.com repository)

To update to the latest stable version, go back to the command line interface and use the following command (redo the `export` command or replace `$FLY_APP` with your site name, as you wish).   

```bash
fly deploy --image owlcms/owlcms:stable --app $FLY_APP
```

If you want to try a prerelease, use "owlcms/owlcms:prerelease" instead -- you can switch between stable and prerelease as you wish.

## Advanced topics

#### (Optional) Install the public results scoreboard

This second site will allow anyone in the world to watch the scoreboard, including the audience (useful if the scoreboard is missing, small or faint).   Something like `myclub-results` makes sense as a name, and people would then use `myclub-results.fly.dev` to reach the scoreboard from their phone.

> In our example we use `$FLY_APP-results`  as the name, so if you defined `FLY_APP` to be `myclub` the results site would be named `myclub-results`. If you want something else, systematically replace `$FLY_APP-results` with what you want in the commands below.

1. Install public results.

   - Choose a meaningful application name.
   - If asked, do NOT copy  existing TOML files
   - Use the default personal environment when asked

   ```bash
   fly app create --name $FLY_APP-results
   ```

2. The two applications (owlcms and publicresults) need to trust one another. So we will create a secret phrase and configure both applications to use it. See [this page](PublicResults) for an overview of how owlcms and publicresults work together.  First we configure publicresults.

   > OWLCMS_UPDATEKEY is the setting name for the secret, and `MaryHadALittleLamb` is the secret phrase.  **Please use your own secret!** Do not put spaces around the `=` sign.
   >
   
    ```
    fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --app $FLY_APP-results
    ```
   
3. owlcms needs to know where public results is located in order to send it updates, and it needs to use the correct secret. 

   - If you are re using a laptop at the competition site, go to the `Prepare Competition` section, on the `Settings and Language page`. Provide the publicresults location (something like `https://myclub-results.fly.dev`) and the secret you configured above.

   - If you are connecting your cloud owlcms to the cloud publicresults, then issue the following commands. 

      ```
      fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --app $FLY_APP
      fly secrets set OWLCMS_REMOTE=https://$FLY_APP-results.fly.dev
      ```

4. Start public results with a correct dimensioning.

   - If asked, do **NOT** copy an existing TOML file
   - Use the personal environment if asked
     ```
     fly deploy --ha=false --vm-size shared-cpu-2x --app $APP-results --image owlcms/publicresults:stable
     ```
     



**You are now done and can use https://myclub-results.fly.dev**



### Updating publicresults for new releases

The `fly deploy` command fetches the newest version available from the public hub.docker.com repository and restarts the application.

To update to the latest stable version

```
fly deploy --image owlcms/publicresults:stable --app $FLY_APP-results
```

### Controlling access to the owlcms application

In a gym setting, people can read the web addresses on the screens.  Because the cloud application is visible to the world, some "funny" person may be tempted to log in to the system and mess things up.  See this [page](AdvancedSystemSettings) for how to control access.

### Using your own site name

Note that if you own your own domain, you can add names under your own domain to reach the fly.io applications.  This is done from the fly.io dashboard, under the `Certificates` section.

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

