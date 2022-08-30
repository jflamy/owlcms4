# Installation on fly.io

Fly.io is a cloud service that is very affordable.  Running a small competition should cost less than 10 cents per day, and very large national competition should cost something like 1.50 US$ per day.  You can scale up or down as you wish, and turn off the application to stop paying. See the [Stopping and Resuming Billing](#stopping-and-resuming-billing) section below.

Compared to Heroku, the only drawback is that the configuration is done from an application installed on the laptop, but this actually makes other things like updating easier. 

### Think about application names

All the names in fly.io are global.  You will be asked for two names;

- one for <u>owlcms</u> -- this is the one you will be using to setup the competition, and from the competition site.  If your club is named `myclub`,  you might pick `myclub-competition` as the name, and the URL will be `https://myclub-competition.fly.dev`.  Few people will use the name, so it can be a touch longer
- one for the <u>public scoreboard</u>.  Whether you intend to use it immediately or not, it comes for free, so you might as well configure it.  This allows anyone in the world to watch the scoreboard, including the audience (useful if the scoreboard is small or faint).  Something like `myclub-results` makes sense as a name, and people would then use `https://myclub-results.fly.dev` to reach the scoreboard.

You should check that the names are free: type the URL in your browser, and if the name is still free, you will get an error saying the name could not be found.

Note that if you own your own domain, you can add names under your own domain to reach the fly.io applications. After creating your applications, you will be able to go to the Certificates section on the dashboard and request free automatically renewed certificates for the desired aliases. 

### Installation

1. Install the `flyctl` tool (`fly` for short) as explained on [this page](https://fly.io/docs/hands-on/installing/).  You can install it on a Mac, Linux, or Windows. 

   > If you are running on Windows, you will need to start a PowerShell in administrative mode as explained [here](https://www.howtogeek.com/742916/how-to-open-windows-powershell-as-an-admin-in-windows-10/).  Then paste the 
   > `iwr https://fly.io/install.ps1 -useb | iex` 

2. If you do not have a fly.io account, type the following command.   You need to either associate a credit card with the account, or to allocate a preset amount of money. 

   ```
   fly auth create
   ```

3. If you already have a fly.io account, type this command instead.

   ```powershell
   fly auth login
   ```
   
4. Create two installation directories.  Each application needs one for its configuration file. 

   ```
mkdir owlcms_config
   mkdir results_config
   ```
   
5. install owlcms and give it enough memory.

   > Answer `y` (YES) when asked if you want a Postgres database.  This is required for owlcms to store its data.  Postgres fits in the free tier, so the only charge is for the additional memory on owlcms.

   ```
   fly launch --image owlcms/owlcms:latest --path owlcms_config
   fly scale memory 512 --config owlcms_config/fly.toml
   ```


5. Install public results.
   
   > <u>Answer `n`</u> (NO) when asked if you want a Postgres database.  publicresults does not need a database.  It does not need additional memory, so it runs in the free tier (0$)

   ```
   fly launch --image owlcms/publicresults:latest --path results_config
   ```
   
6. Create a secret that owlcms will use as an update key to send its  updates to the public scoreboard.  See [this page](PublicResults) for an overview of how owlcms and publicresults work together.

   Use your own secret - do not paste this line as is !

   ```
   fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --config results_config/fly.toml
   ```

7. Copy the secret to owlcms so it can update the public scoreboard.  This is done in the web application itself.  Use your browser to go to your owlcms application (if your competition application is called `myclub-competition` then you would use `https://myclub-competition.fly.dev`).  
   Then follow [this procedure](Remote#configure-updates-from-owlcms).   Don't forget to use the Update button at the top of the page.

### Updating

owlcms and publicresults are packaged as Docker images. The `fly deploy` command fetches the newest version available from the public hub.docker.com repository and restarts the application using the current `fly.toml` configuration.   Note that the applications must first be scaled running before they can be updated.

   ```
fly deploy --config owlcms_config/fly.toml
fly deploy --config results_config/fly.toml
   ```

### Using pre-releases

In order to switch to a prerelease, edit the `fly.toml` configuration files.  Change the image name to use `prerelease` instead of `latest`.  Then run the 

### Stopping and Resuming Billing

The nice thing about cloud services is that they are billed according to actual use, by the second.  The not so nice thing is that you have to remember to stop the billing.

You can run the commands from any command shell you have.

1. If you want to stop the applications (and stopped being billed) -- use your own application names.

   ```
   fly scale count 0 --config owlcms_config/fly.toml
   fly scale count 0 --config results_config/fly.toml
   ```


2. If you then want to start using the applications again, scale them back up to 1. <u>Do NOT use any other value than 0 or 1</u>.

   ```
   fly scale count 1 --config owlcms_config/fly.toml
   fly scale count 1 --config results_config/fly.toml
   ```



### Scale-up and Scale-down of owlcms

For a larger competition, you might want to give owlcms a dedicated virtual machine with more memory.  You only need this for owlcms

> NOTE: scaling the memory must be done after scaling the vm because default sizes are re-applied.

1. Make the application bigger.   Use the name of your application instead of myclub-competition.

   ```
   fly scale vm dedicated-cpu-1x --config owlcms_config/fly.toml
   fly scale memory 1024 --config owlcms_config/fly.toml
   
   ```
   
2. Revert to cheaper settings: make the application smaller, use a smaller computer, and either shut it down (count 0) or leave it running (count 1)

   ```
   fly scale vm shared-cpu-1x --config owlcms_config/fly.toml
   fly scale memory 512 --config owlcms_config/fly.toml
   fly scale count 0 --config owlcms_config/fly.toml
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
