# Installation on fly.io

Fly.io is a cloud service that is very affordable.  Running a small competition would cost less than 10 cents per day, and large national competition would cost something like 1US$ per day.  You can scale up or down as you wish, and turn off the application to stop paying. See the [Stopping and Resuming Billing](#stopping-and-resuming-billing) section below.

Compared to Heroku, the only drawback is that the configuration is done from an application installed on the laptop, but other things like updating are actually easier. 

### First-time-only Preparation

1. Install the `flyctl` tool as explained on [this page](https://fly.io/docs/hands-on/installing/).  You can install it on a Mac, Linux, or Windows. `flyctl` can be invoked as `fly` for short.  See the [Windows Installation Notes](#windows-installation-notes) section at the bottom for additional advice when installing on Windows.
2. Go to https://fly.io and create an account.  Associate a credit card number (or buy a preset amount of credits).  Running owlcms is very inexpensive (it would cost less than 5$ per month to leave it running all the time, but you can turn it off when not needed).


3. Open a terminal/command line interface and login to your account using the following command.  This will open a browser where you will login to your account.

   ```powershell
   fly login
   ```

### Install owlcms


1. Create a directory to contain your configuration files.  Create a separate directory for each of your applications. In this example  we use`fly_owlcms` for the owlcms application  .  The directory name does not matter, the name of the application that will be used in the web URL will be determined in the next step. 
   IMPORTANT: you need to be in the correct configuration directory for the commands to be sent to the correct application

   ```powershell
   mkdir fly_owlcms
   cd fly_owlcms
   ```

2. Create an owlcms application.  The command to use is shown below.

   - Pick carefully a name for your application.  If you choose `myclub` the application will be `myclub.fly.dev`.  The application names cannot be changed easily, you will have to delete and recreate if you change your mind.
    If you have your own DNS domain, there is `Certificates` section for each application which makes it extremely easy to associate an additional name from your own domain.
   - Say `y` when prompted to accept the creation of a Postgres database.  This will  also connect the database to the new application.
- You will be prompted for a region - pick the closest to your competition site.
  
```
   fly launch --image owlcms/owlcms:latest
```

4. At this point, the application attempts to start, but does not have enough memory to actually run.  So we increase the available memory.  The application will reload and will be available under the name you picked.

   ```
   fly scale memory 512 vm shared-cpu-1x
   ```


### Install the Public Scoreboard Module

1. Create a second directory to contain the configuration files.  The actual name of the application will be chosen later.
   IMPORTANT: you need to be in the correct configuration directory for the commands to be sent to the correct application

   ```powershell
   mkdir fly_results
   cd fly_results
   ```

2. Create an owlcms application.  `owlcms/publicresults:latest` is the name in the public repository of Docker images (use `prerelease` instead of `stable` if you want the early adopter versions). 

   - Choose carefully the application name, because it is *not* possible to change it easily
     The application will be known as https://*name*.fly.dev where *name* is the name you picked for publicresults. 
   
   - No database is needed for publicresults. So say `n` when prompted in order to decline the creation of a Postgres database.
     
- You will be prompted for a region, pick one close to where your audience will be.
  
   ```
   fly launch --image owlcms/publicresults:latest
   ```

3. Create a secret that owlcms will use as an update key to send us the updates.  Pick your own secret  !

   ```
   fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb
   ```

4. Go to the Preparation area of owlcms, use the Language and System Settings page to configure the URL and the secret update key.  The URL to use is https://*name*.fly.dev (where you replace *name* with what you picked at step 2).  Don't forget to use the Update button at the top of the page.

### Updating

Go to each of the two folders, and type the following command.  This will automatically re-fetch the latest release and restart the application.

```
fly deploy
```

### Stopping and Resuming Billing

The nice thing about cloud services is that they are billed according to actual use, by the second.  The not so nice thing is that you have to remember to stop the billing.

1. If you want to stop the application (and stopped being billed)

   ```
   fly scale count 0
   ```

2. If you then want to start using the application again, scale it back up to 1. <u>Do NOT use any other value than 0 or 1</u>.

   ```
   fly scale count 1
   ```



### Scale-up and Scale-down of owlcms

For a larger competition, you might want to give owlcms a dedicated virtual machine with more memory.  You only need this for owlcms, not for publicresults.

1. Make the application bigger.  Go to the directory for your owlcms application

   ```
   cd fly_myclub
   fly scale memory 1024 vm dedicated-cpu-1x
   ```

2. Revert to cheaper settings: make the application smaller, use a smaller computer, and either shut it down (count 0) or leave it running (count 1)

   ```
   fly scale memory 512 vm shared-cpu-1x count 0
   ```



### Control access to the application

In a gym setting, people can read the web addresses on the screens, and one day, some "funny" person will log in to the system and be tempted to mess things up.

- We suggest that you set a PIN or Password that officials will be required to type when first logging in.  This is done on via the `Prepare Competition` page, using the `Language and System Settings` button.

![053_editPIN](img/PublicResults/053_editPIN.png)

- You can restrict access to the cloud application to come only from your competition site router. The access list is a comma-separated list of allowed IPv4 addresses.   In order to find the proper value:

  - From your competition site, browse to https://google.com and 

  - Type the string  `my ip`  in the search box.  
    This will display the address of your competition site router as seen from the cloud.  

  - You should see a set of four numbers separated by dots like `24.157.203.247`  . This the address you should use -- owlcms will reject connections coming from other places than your competition router. 

  Note that if you use the OWLCMS_IP environment setting in your fly.toml file, these will take precedence over what is in the database.

- If you have set a password, you may need to set the OWLCMS_BACKDOOR variable to avoid entering passwords on the screens used for video broadcasting.  This would be done by editing the fly.toml file.

### Windows Installation Notes

Windows installation is done with PowerShell, which is installed by default on all Windows 10 and 11 machines.

- Find Powershell by clicking on the start menu and typing `powershell`

- Right-click on the `Windows PowerShell` icon that appears in the search results at the left, and select `Run as administrator`

  ![PowerShell](img/Fly.io/PowerShell.png ':size=500')

- Then paste the application creation command.

  ```
  iwr https://fly.io/install.ps1 -useb | iex 
  ```