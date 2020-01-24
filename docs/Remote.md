
The remote Public Results application is used to make applications results available to the public at large.

The information is sent from the competition site to an application running on the cloud.  The general public, whether at the competition site or anywhere on the internet, can access the scoreboards via their phone or laptop.  There is no load put on the competition site other than sending an update to the remote application.  The remote application takes all the load for the public queries.

## First-time Install of the Public Results Application

1. Get a free Heroku account -- go to [https://heroku.com](https://heroku.com) and sign up!
2. Go to the github repository for the public results application [https://github.com/jflamy/owlcms-publicresults](https://github.com/jflamy/owlcms-publicresults) and <u>scroll all the way down to the end of the page</u>.
3. Use the `Deploy to Heroku` button.  Enter your Heroku account (or create one if you haven't)
    ![011_deployButton](img/PublicResults/011_deployButton.png)
4. Choose a meaningful application name.  This is what the public will see (unless you use the Heroku options to map your application to a name your club or federation owns, but this is beyond the scope of this tutorial)
    ![020_appName](img/PublicResults/020_appName.png)
5. Deploy the application. This will magically copy the application from the owlcms-publicresults site, install it, and make it available to the public.
    ![030_deploy](img/PublicResults/030_deploy.png)
6. You can now view the public results application
    ![031_viewApp](img/PublicResults/031_viewApp.png)
7. The application is just sitting there, waiting.
    ![032_viewApp1](img/PublicResults/032_viewApp1.png)
8. The public results application expects a secret code to be sent by the competition site in order to do anything.   We now configure the secret code that will be expected (later we will configure owlcms to send that value.)  Go to the `Settings` page for the application.
    ![040_configureServerKey](img/PublicResults/040_configureServerKey.png)
9. We define a configuration variable `OWLCMS_UPDATEKEY` to contain the expected secret.  **Use something easy to type, but quite long**, and not easily guessed -- a sentence from your favorite song, for example.  `abracadabra` is therefore **NOT** a good real-life example.
    ![041_configureServerKey2](img/PublicResults/041_configureServerKey2.png)

## Configure the competition site to send updates

1. Open the file location where OWLCMS4 is configured.  These instructions are for Windows; the equivalent steps for Mac and Linux are performed by adding the options to the `java` command line.
![050_clientLocation](img/PublicResults/050_clientLocation.png)
1. Edit the .ini configuration file using Notepad.
![051_editConfigFile](img/PublicResults/051_editConfigFile.png)
1. At the top of the.ini  file, use Notepad to add the values according to the following format
``` 
-Dremote=https://owlcms-test-publicresults.herokuapp.com/update
-DupdateKey=abracadabra
```
> ##### Notes:
>
> - **use your own site** -- replace `https://owlcms-test-publicresults.herokuapp.com` with your own application.
> - Make sure that the value for `-Dremote=` **ends with `/update`** 
> - **use your own secret** that you defined on the server application earlier (the value of the Heroku variable `OWLCMS_UPDATEKEY` is the secret)

You should therefore have something similar to the following in your file

![052_clientKeyValues](img/PublicResults/052_clientKeyValues.png)
## Running a competition with a remote public scoreboard

1. Start the remote application.  Just accessing the URL is enough -- it will restart the application if it was shut down due to inactivity (on the free Heroku subscription, the remote application will go to sleep if unused for an hour).  During a competition, there will be frequent updates, so there is no chance of this happening. 
2. Start the competition site application as usual, and get the the announcer to select a group,
   ![057_startLifting](img/PublicResults/057_startLifting.png)
1. The public results application will now show that there is a platform active.
![055_updateReceivedHome](img/PublicResults/055_updateReceivedHome.png)
1. Clicking on the `Platform A` link leads to the blank scoreboard, since the announcer has not started a countdown and has not started the lifting. 
![056_updateReceivedFOP](img/PublicResults/056_updateReceivedFOP.png)
1. As soon as a break or lifting starts, the competition site updates the remote application.  From then on the scoreboard updates whenever a pertinent change happens.  Note that in the first release the scoreboard clock only shows the time allocated for the lift, and does not count down, and that decision lights are not shown.
![058_liftingStarted](img/PublicResults/058_liftingStarted.png)
## Updating the application
When testing in the days leading to a competition, it is wise to update both the OWLCMS4 application and the remote public results server.

We suggest the following procedure

1. Go to the installation folder for owlcms and copy the `owlcms.l4j.ini` file to your desktop (click on the file, Copy with Ctrl-C or ⌘C, go to the desktop, Paste with Ctrl-V or ⌘V) .

2. Open a tab on the Heroku site  [https://heroku.com](https://heroku.com) and log in.

3. Open a second tab to the [https://github.com/jflamy/owlcms-publicresults](https://github.com/jflamy/owlcms-publicresults) public results application home page

4. When you are ready, edit the name of your current application and change it, for example by adding _old to the name (in this way, you keep the current settings for reference).

![060_HerokuSettings_Edit](img/PublicResults/060_HerokuSettings_Edit.png)

 

 Ignore the warning given.
![061_rename](img/PublicResults/061_rename.png)

5. Go to the  [https://github.com/jflamy/owlcms-publicresults](https://github.com/jflamy/owlcms-publicresults) public results application home page and use the `Deploy to Heroku` button found at the bottom of the page <u>to deploy again</u>.   Because you renamed your old application, you are now able to reuse the same application name you had before.
6. Follow the same steps as you did before to add the `OWLCMS_UPDATEKEY` shared secret to the new application.  You can refer to the old application, or look inside your .ini file to get the old value.
7. Install the new version of owlcms.  Stop the program. Copy your saved `owlcms.l4j.ini` file back to the installation folder (overwrite the file).
8. Once you have checked that things work with the new versions, you may delete the old Heroku application.  Go to its settings page and scroll all the way down, and use the `Delete` button.