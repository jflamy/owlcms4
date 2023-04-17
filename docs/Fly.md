# Installation on fly.io

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

2. Click on the grey box below to copy the command.  Paste it to the command line interface (use right-click on Windows, or ctrl-click on macOS, or the browser Edit menu)

   ```
   fly launch --name $APP -i owlcms/owlcms:stable --force-machines
   ```

   - Organization:  use the default `Personal` organization.

   - Postgres Database: **IMPORTANT**, answer **y (YES)**  when asked if you want a Postgres database.  This is required for owlcms to store its data.

   - Configuration of the Postgres database : Choose the default option  `Development`

   - Upstash Redis : Answer **n (No)**

   - Deploy immediately: Answer **y (Yes)** 


3. We now request more memory for the machine. Click the grey box below to copy the command and paste it to the command line (use right-click on Windows, or ctrl-click on macOS, or the browser Edit menu)

   ```
   fly machine update $(fly m list -a $APP -q) --memory 512 --app $APP --yes
   ```

   You are now done and can use https://myclub.fly.dev

### Updating for new releases

The `fly deploy` command fetches the newest version available and restarts the application (owlcms stores the versions in the public hub.docker.com repository)

To update to the latest stable version, log in again to https://fly.io/terminal and type the following command (replacing `myclub` with your own site name)

```
fly deploy -i owlcms/owlcms:stable -a myclub
```

### Advanced topics

#### (Optional) Install the public results scoreboard

This second site will allow anyone in the world to watch the scoreboard, including the audience (useful if the scoreboard is missing, small or faint).   Something like `myclub-results` makes sense as a name, and people would then use `myclub-results.fly.dev` to reach the scoreboard from their phone.

This is not required, but since there is no extra cost associated, you might as well configure it even if you don't need it immediately.

1. Install public results.

   - Choose a meaningful application name.  In our example we use `myclub-results` with the name you want for your remote scoreboard application
   - **Answer `n` (NO)** when asked if you want a Postgres database.  publicresults does not need a database.

   ```
   fly launch -i owlcms/publicresults:stable --name myclub-results
   ```

2. The two applications (owlcms and publicresults) need to trust one another. So we create a secret phrase and configure both applications to use it. See [this page](PublicResults) for an overview of how owlcms and publicresults work together.

   > OWLCMS_UPDATEKEY is the setting name for the secret, and `MaryHadALittleLamb` is the secret phrase.  **Please use your own secret!** 
   >
   > Replace `myclub` and `myclub-results` with your own names
   >

    ```
    fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --app myclub-results
    fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --app myclub
    ```

3. The last step is to tell owlcms where the results publishing application resides so it can feed it with results.

      > Replace `myclub` and `myclub-results` with your own names

    ```
    fly secrets set OWLCMS_REMOTE=https://myclub-results.fly.dev --app myclub-results
    ```

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

If you run a very large competition, you may wish to increase the memory for the duration of the competition. You would use the same commands as above (`fly machine update`) to set the memory to `1024` and after the competition you would set it back to `512`.

### Attach to Postgres DB manually

If for some reason if your application is not attached to Postgres DB, the internal H2 database will be used and data will be lost when server is restarted. 

You can follow the below steps to attach your application to the postgres DB:

1. If you have data in the H2 database and need to keep it, export the database from the "Prepare Competition" page
2. Create a postgres DB if you didn't create it before

```
fly postgres create myclub-db
```

3. Attach the application to the database

```
fly postgres attach myclub-db --app myclub --database-name myclub-db --database-user owlcms
```

4. Restart the application

```
fly deploy --app myclub
```

5. Reload the database export, if you created one at step 1.
