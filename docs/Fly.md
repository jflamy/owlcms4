# Installation on fly.io

Fly.io is a cloud service that makes it easy to start from a very affordable service for small competitions, and also support a still affordable tier for larger meets.  Fly.io cheaper than Heroku, but the setup take a few more steps.

Running a small competition would cost less than 10 cents per day, and large national competition would cost something like 1US$ per day (from the time you scale it up to the higher performance mode to the time you scale back down. 

> When the competition is done, you can scale down to 0 and not pay anything while the application is down.  See the Scale Up and Scale Down section below.

### Preparation

1. Install the `flyctl` tool as explained on [this page](https://fly.io/docs/hands-on/installing/).  
   On Windows

   - Start PowerShell as an administrator (click on the start icon, search for powershell, right-click to run as administrator
     ![PowerShell](img/Fly.io/PowerShell.png ':size=500')
   
   - Then paste the creation command.
      ```
      iwr https://fly.io/install.ps1 -useb | iex 
      ```


2. Go to https://fly.io and create an account.  Associate a credit card number (or buy a preset amount of credits)

3. Open a terminal/command line interface and login to your account using the following command.  This will open a browser where you will login to your account.

   ```powershell
   fly login
   ```

### Install owlcms


1. Create a directory to contain your configuration files.  Create a separate directory for each of your applications. In this example  we use`fly_owlcms` for the owlcms application  .  The directory name does not matter, the name of the application that will be used in the web URL will be determined in the next step.

   ```powershell
   mkdir fly_owlcms
   cd fly_owlcms
   ```

2. Create an owlcms application.  The command to use is shown below.

   - Pick carefully a name for your application.  If you choose `myclub` the application will be `myclub.fly.dev`.  The application names cannot be changed easily, you will have to delete and recreate if you change your mind.
- Say `y` when prompted to accept the creation of a Postgres database.  This will  also connect the database to the new application.
   - You will be prompted for a region - pick the closest to your competition site.
- `owlcms/owlcms:latest` is the name in the public repository of Docker images (use `prerelease` instead of `stable` if you want the early adopter versions). 
   - Note that the application will launch, but won't work with the default (free) settings, we will need to give it a bit more memory (and pay about 10 cents per day of actual use for that)

   ```
   fly launch --image owlcms/owlcms:latest
   ```

3. (optional) Start another window to look at the logs

   ```
   fly logs
   ```

4. Make the application big enough to run.  The application will reload.

   ```
   fly scale memory 512 vm shared-cpu-1x
   ```

5. If you want to stop the application (and stopped being billed)

   ```
   fly scale count 0
   ```

6. If you want to restart the application that you stopped. Do NOT use any other value than 0 or 1.

   ```
   fly scale count 1
   ```

### Install the publicresults Module

1. Create a second directory to contain the configuration files.  The actual name of the application will be chosen later.

   ```powershell
   mkdir fly_results
   cd fly_results
   ```

2. Create an owlcms application.  `owlcms/publicresults:latest` is the name in the public repository of Docker images (use `prerelease` instead of `stable` if you want the early adopter versions). 

   - Choose carefully the application name, because it is *not* possible to change it easily
   
   - No database is needed for publicresults. So say `n` when prompted in order to decline the creation of a Postgres database.
     
   - You will be prompted for a region, pick one close to where your audience will be.

   ```
   fly launch --image owlcms/publicresults:latest
   ```

3. Create a secret that will be used by owlcms to connect to publicresults.  

   ```
   fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb
   ```

4. The application will be known as https://*name*.fly.dev where *name* is the name you picked for publicresults. You will need to configure that URL and the update key just defined on the Language and Settings page of your owlcms.

### Scale-up and Scale-down of owlcms

For a larger competition, you might want to give owlcms a dedicated virtual machine with more memory.  You only need this for owlcms, not for publicresults.

1. Make the application bigger.  Go to the directory for your owlcms application

   ```
   cd fly_myclub
   fly scale memory 1024 vm dedicated-cpu-1x
   ```

2. Make the application smaller and shut it down (count 0) or count 1 if you want to keep it running.

   ```
   fly scale memory 512 vm shared-cpu-1x count 0
   ```



#### (For Reference) Alternate configuration with H2

There is no real reason to use this setup if the Postgres setup above works.  owlcms does not require any persistent storage when used with Postgres.  This setup would work with H2.  H2 does not require a database server, but instead you need to allocate persistent storage.

1. We now need to create a file area to store the database.  Make sure to use the region where you created the application (replace ABC with the 3-letter code for your region). This will associate a volume called `myclub_database` with your application

   ```
   fly volumes create myclub_database --region ABC --size 1
   ```

2. Edit the application configuration.  The file is called `fly.toml` .  On Windows, use notepad to edit it (right-click on the file). Add a `[mounts]` section as shown below. The source is the name you used for your volume.  The destination should not be changed.

   ```
   # fly.toml file generated for owlcms on 2022-08-25T22:49:26-04:00
   
   app = "myclub"
   kill_signal = "SIGINT"
   kill_timeout = 5
   processes = []
   
   [mounts]
     source="myclub_database"
     destination="/database"
   
   ```

3. Tell fly to reload

   ```
   fly deploy
   ```