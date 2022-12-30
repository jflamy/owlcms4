# Installation on fly.io

Fly.io is a cloud service that is, in effect, free. The charges incurred for a single owlcms application and the matching remote scoreboard are less than their 5US$ per month threshold, so there is no bill emitted.

Compared to Heroku, the only drawback is that the configuration is done from an application installed on the laptop, but this actually makes other things like updating easier. 

### Choose Application Names

The application names in fly.io are global, so someone may have already used the name you want.  You will be asked for two names;

- one for <u>owlcms</u> -- this is the one you will be using to setup the competition, and from the competition site.  If your club is named `myclub`,  you might pick `myclub` as the name, and the URL would be `https://myclub.fly.dev`
- one for the <u>public scoreboard</u>.  Whether you intend to use it immediately or not, it comes for free, so you might as well configure it.  This allows anyone in the world to watch the scoreboard, including the audience (useful if the scoreboard is small or faint).  Something like `myclub-results` makes sense as a name, and people would then use `myclub-results.fly.dev` to reach the scoreboard from their phone.

You should check that the names are free: type the URL in your browser. If you get a 404 Not Found error, then the name is still free and you can claim it.

Note that if you own your own domain, you can add names under your own domain to reach the fly.io applications. After creating your applications, you will be able to go to the Certificates section on the dashboard and request free automatically renewed certificates for the desired aliases. 

### Install

1. Install the `flyctl` tool (`fly` for short) as explained on [this page](https://fly.io/docs/hands-on/installing/).  You can install it on a Mac, Linux, or Windows. 

   > If you are running on Windows, you will need to start a PowerShell in administrative mode as explained [here](https://www.howtogeek.com/742916/how-to-open-windows-powershell-as-an-admin-in-windows-10/).  Then you can paste the command
   > `iwr https://fly.io/install.ps1 -useb | iex` 

2. Depending on whether or not you already have an account, you will use one of the following options type the following command. 

   1. If you do not have an account, create one and associate a credit card -- it will not be billed, but that is required.
   
      ```bash
      fly auth create
      ```
   
    2. If you already have a fly.io account, type this command instead.

       ```bash
       fly auth login
       ```

4. install owlcms and give it enough memory.

   > **Answer `y` (YES) ** when asked if you want a Postgres database.  This is required for owlcms to store its data.

   Replace `myclub` with the name you want for your application
      ```
   fly launch --image owlcms/owlcms:stable --app myclub
   fly scale memory 512 --app myclub
      ```

### (Optional) Install the public results scoreboard

This is not required, but since there is no extra cost associated, you might as well configure it even if you don't need it immediately.

1. Install public results.

   > **Answer `n` (NO)** when asked if you want a Postgres database.  publicresults does not need a database.

   Replace `myclub-results` with the name you want for your remote scoreboard application

   ```
   fly launch --image owlcms/publicresults:stable --app myclub-results
   ```

2. Create a secret that owlcms will use as an update key to send its updates to the public results scoreboard.  See [this page](PublicResults) for an overview of how owlcms and publicresults work together.

   Use your own secret - do not paste this line as is !
   

   ```
   fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb --app myclub-results
   ```

5. Configure the connection between your owlcms and your Go to the owlcms application you just created  (https://myclub.fly.dev) and  follow [this procedure](Remote#configure-updates-from-owlcms).   Don't forget to use the Update button at the top of the page.

### Updating

owlcms and publicresults are packaged as Docker images. The `fly deploy` command fetches the newest version available from the public hub.docker.com repository and restarts the application.

```
fly deploy --image owlcms/owlcms:stable --app myclub
fly deploy --image owlcms/publicresults:stable --app myclub-results
```

### Using pre-releases

In order to switch to a prerelease, simply use the [Updating](#updating) instructions, but replace the word `stable` with the word `prerelease`

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



### Scale-up and Scale-down of owlcms

For a larger competition, you might want to give owlcms a dedicated virtual machine with more memory.  You only need this for owlcms

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
