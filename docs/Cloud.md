

## Heroku

Heroku is a cloud service provider that provides an attractive free (0$) pricing plan for running programs like OWLCMS.  When running on Heroku you only need a good Internet connection, and you do not need to configure a primary laptop.

The installation process for Heroku has been completely redone, and is now extremely simple -- there is nothing whatsoever that needs to be installed on any of the laptops other than a browser.

**1. Create a free Heroku Account**

- Go to page https://heroku.com
- Create a free account.  Yes, it is free.  Remember the login and password information.

**2. Go to the releases repository and start the process**

The Heroku releases directory is located at [this location](https://github.com/${env.REPO_OWNER}/${env.H_REPO_NAME}/releases/latest). Click the big `Deploy to Heroku` button.

![010_deployButton](img/Heroku/010_deployButton.png)

**3. Name your application and deploy**

Enter the name that will be used on all your competition site laptops and displays.  Once you are done start the deployment (this will prepare the application and make it available)

![020_selectName](img/Heroku/020_selectName.png)

**4. Check correct deployment**

![030_deployApp](img/Heroku/030_deployApp.png)

**5. Go to the application**

![040_success](img/Heroku/040_success.png)

**6. Time zone configuration**
By default, Heroku uses the universal UTC time (colloquially known as Greenwich).  You should set the time zone to match the published schedule, so that break timers can be set accordingly.
Go to the `Prepare Competition`- `Language and System Settings` page. 
  - If you are in the same time zone as the competition, you can use the button indicated by the arrow to quicky set the time zone.
  - If not, you need to use the drop down list to select a city in the same time zone as the schedule.

![030_timezone](img/SystemSettings/030_timezone.png)

## Control access to the application

In a gym setting, people can read the web addresses on the screens, and one day, some "funny" person will log in to the system and be tempted to mess things up.
- You should therefore set a PIN or Password that officials will be required to type when first logging in.  This is done on via the `Prepare Competition` page, using the `Language and System Settings` button.

![053_editPIN](img/PublicResults/053_editPIN.png)

- If running from a competition site, you can restrict access to the cloud application to come only from your competition site router. The access list is a comma-separated list of allowed IPv4 addresses.   In order to find the proper value:

  - From your competition site, browse to https://google.com and 
  
  - Type the string  `my ip`  in the search box.  
    This will display the address of your competition site router as seen from the cloud.  
    
  - You should see a set of four numbers separated by dots like `24.157.203.247`  . This the address you should use -- owlcms will reject connections coming from other places than your competition router. 
  

Note that if you use the OWLCMS_IP or -Dip settings, these will take precedence over what is in the database.

## Configuration Parameters

See the [Configuration Parameters](./Configuration.md ':include') page to see additional configuration options in addition to the ones presented on this page.
