# Installation on fly.io

Fly.io is a cloud service that is very affordable.  Running a small competition should cost less than 10 cents per day, and very large national competition should cost something like 1.50 US$ per day.  You can scale up or down as you wish, and turn off the application to stop paying. See the [Stopping and Resuming Billing](#stopping-and-resuming-billing) section below.

Compared to Heroku, the only drawback is that the configuration is done from an application installed on the laptop, but this actually makes other things like updating easier. 

### Think about application names

All the names in fly.io are global.  You will be asked for two names;

- one for owlcms -- this is the one you will be using to setup the competition, and from the competition site.  If your club is named `myclub`,  you might pick `myclub-competition` as the name, and the URL will be `https://myclub-competition.fly.dev`.  Few people will use the name, so it can be a touch longer
- one for the public scoreboard.  Whether you intend to use it immediately or not, it comes for free, so you might as well configure it.  This allows anyone in the world to watch the scoreboard. You can turn it off at any time. So you might pick `myclub-results` as the name, and people would use `https://myclub-results.fly.dev` to reach the scoreboard.

Think about alternatives if your first choice is already taken.

Note that if you own your own domain, you can add names under your own domain to reach the fly.io applications. After creating your applications, you will be able to go to the Certificates section on the dashboard and request free automatically renewed certificates for the desired aliases. 

### Preparation

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
   
4. install owlcms and give it enough memory.

   > Answer `y` when asked if you want a Postgres database.  This comes free with the application, and this is required for owlcms to store its data

   ```
   mkdir owlcms_config
   cd owlcms_config
   fly launch -i owlcms/owlcms:latest
   fly scale memory 512 vm shared-cpu-1x
   cd ..
   ```


5. Install public results.
   
   > Answer `n` when asked if you want a Postgres database.  publicresults does not need a database

   ```
   mkdir results_config
   cd results_config
   fly launch --image owlcms/publicresults:latest
   ```

6. Create a secret that owlcms will use as an update key to send its  updates to the public scoreboard  Pick your own secret !    See [this page](Remote) for an overview of how this works.

   ```
   fly secrets set OWLCMS_UPDATEKEY=MaryHadALittleLamb
   ```

7. Configure the secret in owlcms so it can update the public scoreboard.
   Use your browser to go to your owlcms application (open `https://myclub-competition.fly.dev` where you replace myclub-competition with your own owlcms application name).  The procedure to use is for the shared key is described on [at this location](Remote#configure-updates-from-owlcms).   Don't forget to use the Update button at the top of the page.

### Updating

On every release (stable or prerelease), owlcms and publicresults versions are created as Docker images and stored in the public hub.docker.com repository.  The `fly deploy` command fetches the newest version available and restarts the application.

<u>Go to each of the two folders</u>, start a command shell, and run the `fly deploy` command.

> On Windows, *Hold the Shift Key Down* *and Right-Click* on the folder with the mouse.  In the menu that comes up, pick the item that opens a command interface.  Depending on your version, it will be cmd.exe, PowerShell, or Terminal.  Which one is used does not matter.

   ```
fly deploy
   ```

### Stopping and Resuming Billing

The nice thing about cloud services is that they are billed according to actual use, by the second.  The not so nice thing is that you have to remember to stop the billing.

You can run the commands from any command shell you have.

1. If you want to stop the applications (and stopped being billed) -- use your own application names.

   ```
   fly scale count 0 -a myclub-competition
   fly scale count 0 -a myclub-results
   ```


2. If you then want to start using the application again, scale it back up to 1. <u>Do NOT use any other value than 0 or 1</u>.

   ```
   fly scale count 1 -a myclub-competition
   fly scale count 1 -a myclub-results
   ```



### Scale-up and Scale-down of owlcms

For a larger competition, you might want to give owlcms a dedicated virtual machine with more memory.  You only need this for owlcms

1. Make the application bigger.   Use the name of your application instead of myclub-competition.

   ```
   fly scale memory 1024 vm dedicated-cpu-1x -a myclub-competition
   ```
   
2. Revert to cheaper settings: make the application smaller, use a smaller computer, and either shut it down (count 0) or leave it running (count 1)

   ```
   fly scale memory 512 vm shared-cpu-1x count 0 -a myclub-competition
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
