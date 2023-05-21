## Installation on fly.io

Fly.io is a cloud service that is, in effect, free. The charges incurred for a single owlcms application and the matching remote scoreboard are about 1US$ per month, cheaper than the minimal amount for which they emit a bill.

In order to install an application you will need to log in to their site and then issue 3 commands.

### Log in

Go to the site https://fly.io

If you do not have an account, create one.  Running owlcms requires an account, but you will not actually get billed because the fees are lower than their minimum

If there is a "use the Web CLI" button, click it, otherwise type https://fly.io/terminal in your browser address bar yourself.

### Install owlcms

1. Choose an application name.

   The application names in fly.io are global.  If the name you want is already taken, you will be told.  If your club is named `myclub`,  you might pick `myclub` as the name, and the URL will be `https://myclub.fly.dev`

   Type the following command in the black command line area, replacing `myclub` with the name you want.

   ```
   export APP=myclub
   ```

2. Create the application.
   *Click on the grey box below to copy the command.  Paste it to the command line interface (use right-click on Windows, or ctrl-click on macOS, or the browser Edit menu)*
   
   ```
   fly app create --name $APP 
   ```
   
   - Organization:  use the default `Personal` organization.


3. Create the database that will store the competition information (*click on the box to copy, right-click to paste*)

   ```bash
   fly postgres create --name $APP-db
   ```

   - Organization:  use the default `Personal` organization.

   - Configuration of the Postgres database : Choose the default option  `Development`

   - Stop after 1 hour:  Answer **n (No)** 

3. Link the database to the application

   ```bash
   fly postgres attach $APP-db --app $APP
   ```

5. Start the application with the proper size

   ```bash
   fly deploy --ha=false --vm-size shared-cpu-2x --app $APP
   ```

**You are now done and can use https://myclub.fly.dev**

### Updating for new releases

The `fly deploy` command fetches the newest version available and restarts the application (owlcms stores the versions in the public hub.docker.com repository)

To update to the latest stable version, log in again to https://fly.io/terminal and type the following command (replacing `myclub` with your own site name)

```
fly deploy -i owlcms/owlcms:stable -a myclub
```

## Advanced topics

#### (Optional) Install the public results scoreboard

This second site will allow anyone in the world to watch the scoreboard, including the audience (useful if the scoreboard is missing, small or faint).   Something like `myclub-results` makes sense as a name, and people would then use `myclub-results.fly.dev` to reach the scoreboard from their phone.

This is not required, but since there is no extra cost associated, you might as well configure it even if you don't need it immediately.

1. Install public results.

   - Choose a meaningful application name.  In our example we use `myclub-results` with the name you want for your remote scoreboard application
   - If asked, do NOT copy  existing TOML files
   - Use the default personal environment when asked

   ```
   fly app create --name $APP-results
   ```
   
2. The two applications (owlcms and publicresults) need to trust one another. So we create a secret phrase and configure both applications to use it. See [this page](PublicResults) for an overview of how owlcms and publicresults work together.

   > OWLCMS_UPDATEKEY is the setting name for the secret, and `MaryHadALittleLamb` is the secret phrase.  **Please use your own secret!** 
   >
   > Replace `myclub` and `myclub-results` with your own names
   >

    ```
    fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --app $APP-results
    fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --app $APP
    ```

3. owlcms also needs to know where public results is in order to send it updates.  Use the correct name for your publicresults app.

   ```
   fly secrets set OWLCMS_REMOTE=https://$APP-results.fly.dev --app $APP
   ```
      
4. Start public results with a correct dimensioning.

      - If asked, do NOT copy an existing TOML file
      - Use the personal environment when asked
      
      ```
      fly deploy --ha=false --vm-size shared-cpu-2x --app $APP-results
      ```

**You are now done and can use https://myclub-results.fly.dev**



### Updating publicresults for new releases

The `fly deploy` command fetches the newest version available from the public hub.docker.com repository and restarts the application.

To update to the latest stable version

```
fly deploy --image owlcms/publicresults:stable --app myclub-results
```

### Control access to the owlcms application

In a gym setting, people can read the web addresses on the screens.  Because the cloud application is visible to the world, some "funny" person may be tempted to log in to the system and mess things up.  See this [page](AdvancedSystemSettings) for how to control access.

### Using your own site name

Note that if you own your own domain, you can add names under your own domain to reach the fly.io applications.  This is done from the fly.io dashboard, under the `Certificates`

### Scale-up and Scale-down of owlcms

If you run a very large competition, you may wish to increase the memory to 1024 or to use a dedicated CPU.
