# Installation on fly.io

Fly.io is a cloud service that is, in effect, free. The charges incurred for a single owlcms application and the matching remote scoreboard are less than their 5US$ per month threshold, so there is no bill emitted.

In order to install an application you will need to log in to their site and then type 3 commands.

### Log in

Go to the site https://fly.io

If you do not have an account, create one.  Running owlcms requires an account, but you will not actually get billed because the fees are lower than their minimum

If there is a "use the Web CLI" button, click it, otherwise type the https://fly.io/terminal yourself.

### Install owlcms

1. Choose an application name The application names in fly.io are global.  If the name you want is already taken, you will be told.  If your club is named `myclub`,  you might pick `myclub` as the name, and the URL will be `https://myclub.fly.dev` . See "Advanced topics" below if your club already has a domain name - you will be also be able to use it.

You will now have to type three commands, and answer a few questions.  The fly user interface will highlight the default values in pale blue, you can accept them by just using the "enter" key.

1. Install owlcms.  Type the following command, and answer the questions as explained below

   ```
   fly launch -i owlcms/owlcms:stable
   ```

      - You will be asked for the name of the application: in our example use `myclub`

   - You should use your `personal` organization
   - **Answer `y` (YES) ** when asked if you want a Postgres database.  This is required for owlcms to store its data.

      - **Answer n (No)** when asked if you want a Redis database

      - **Answer n (No)** when asked to deploy immediately.


3. Add memory for the application and launch it. 
   ```
   fly scale memory 512
   fly deploy
   ```

> You are now done and can use https://myclub.fly.dev



#### Updating for new releases

The `fly deploy` command fetches the newest version available and restarts the application (owlcms stores the versions in the public hub.docker.com repository)

To update to the latest stable version, log in again to https://fly.io/terminal and type

```
fly deploy -i owlcms/owlcms:stable -a myclub
```



### Advanced topics

#### (Optional) Install the public results scoreboard

This second site will allow anyone in the world to watch the scoreboard, including the audience (useful if the scoreboard is missing, small or faint).   Something like `myclub-results` makes sense as a name, and people would then use `myclub-results.fly.dev` to reach the scoreboard from their phone.

This is not required, but since there is no extra cost associated, you might as well configure it even if you don't need it immediately.

1. Install public results.  This is the same as before, except we don't want the databases

   > Replace `myclub-results` with the name you want for your remote scoreboard application
   >
   > **Answer `n` (NO)** when asked if you want a Postgres database.  publicresults does not need a database.

   ```
   fly launch -image owlcms/publicresults:stable --app myclub-results
   ```

2. The two applications (owlcms and publicresults) need to trust one another. So we create a shared secret between the two applications. See [this page](PublicResults) for an overview of how owlcms and publicresults work together.

   > OWLCMS_UPDATEKEY is the name of the secret, and `MaryHadALittleLamb` is the secret.  Please use your own secret! 
   >
   > Replace `myclub` and `myclub-results` with your own names
   >

    ```
    fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --app myclub-results
    fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --app myclub
    ```

3. Tell your owlcms application where your public results application is so it can connect.

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

### Using you own site name

Note that if you own your own domain, you can add names under your own domain to reach the fly.io applications.  This is done from the fly.io dashboard, under the `Certificates`

### Scale-up and Scale-down of owlcms

For a larger competition, you might want to give owlcms a dedicated virtual machine with more memory.  **This is only needed for the duration of the competition.**

> NOTE: scaling the memory must be done after scaling the vm because default sizes are re-applied.

1. Make the application bigger. 

   ```
   fly scale vm dedicated-cpu-1x --app myclub
   fly scale memory 1024 --app myclub
   ```

2. Revert to cheaper settings: make the application smaller, use a smaller computer, and either shut it down (count 0) or leave it running (count 1)

   ```
   fly scale vm shared-cpu-1x --app myclub
   fly scale memory 512 --app myclub
   fly scale count 0 --app myclub
   ```

### Stopping and Resuming Billing

The nice thing about cloud services is that they are billed according to actual use, by the second.  Since the service is essentially free because it falls under the billing threshold, this is "just in case"

You can run the commands from any command shell you have.

1. If you want to stop the applications (and stopped being billed) 

   ```
   fly scale count 0 --app myclub
   fly scale count 0 --app myclub-results
   ```


2. If you then want to start using the applications again, scale them back up to 1. <u>Do NOT use any other value than 0 or 1</u>.

   ```
   fly scale count 1 --app myclub
   fly scale count 1 --app myclub-results
   ```

### 
