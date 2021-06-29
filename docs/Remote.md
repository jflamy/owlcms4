# Public Scoreboard

The `publicresults` application is used to make applications results available to the public at large, or to participants in virtual competition. For this reason, it is usually run in the cloud.

The following example shows owlcms running standalone in a gym, sending information to publicresults running in the cloud.
![Slide2](img/PublicResults/CloudExplained/Slide2.SVG)

- updates are sent from the competition site to  the publicresults application running on the cloud whenever there is a significant event (clock start/stop, weight changes, lift decisions, etc.)  
- The public can connect to publicresults and  see the scoreboards via their phone or laptop no matter where they are
- There is no load put on the competition site other than sending an update to the publicresults application.  The publicresults application takes all the load for the public queries.

The other common configuration is when both owlcms and publicresults are in the cloud,  That scenario is discussed [here](VirtualOverview).

## Install the public results scoreboard on Heroku

1. Get a free Heroku account -- 

    - Go to [https://heroku.com](https://heroku.com) and create a free account
    - Remember the login and password information.

2. Click on the purple button below to start the installation on Heroku Cloud.

    [![Deploy to Heroku](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy?template=https://github.com/jflamy-dev/owlcms-publicresults)

3. Enter your Heroku account (or create one if you haven't)

4. Enter a meaningful application name.  In the following example, we use `owlcms-test-publicresults` We will later use `owlcms-test-publicresults.herokuapp.com`  to see the results.  You should use a name that makes sense to your club or federation.

    *(Reminder: You can click on the images to make them bigger)*

    ![020_appName](img/PublicResults/020_appName.png)

5. Deploy the application. This will fetch the application, install it, and make it available to the public.
    ![030_deploy](img/PublicResults/030_deploy.png)

6. You can check that the application is running by starting a new browser tab. In our example, we connect to `https://owlcms-test-publicresults.herokuapp.com`.  Since we have not yet connected owlcms to feed publicresults, you will see this screen.
    ![032_viewApp1](img/PublicResults/032_viewApp1.png)
    

## Configure a Shared Secret

7. We now need to configure a secret code to keep communications secure between the competition site and the publicresults repeater.  Go to the `Settings` page for the application.
    ![040_configureServerKey](img/PublicResults/040_configureServerKey.png)

8. Create configuration variable `OWLCMS_UPDATEKEY` and set it to the secret key that will be shared with owlcms.  **Use something easy to type, but quite long**, and not easily guessed.  The  password to your Heroku account and the update key is what prevents vandals from messing up your scoreboards.  Do not share them, and make sure they cannot be guessed easily.
    ![ljyvckBm6F](img/PublicResults/Example/ljyvckBm6F.png)

9. Copy the value for the update key (Ctrl-C)

## Reset `publicresults`

10. Restart the publicresults application
    ![6Ihs0ei0Ad](img/PublicResults/Example/6Ihs0ei0Ad.png)

2. Start a new browser tab and go to the address for the application (or you can use the `Open app` button at the top right) and check that the application is again waiting.
     ![AAAxZYQKZK](img/PublicResults/Example/AAAxZYQKZK.png)

## Configure Updates from `owlcms`

1. Open the owlcms application on Heroku and go to the `Prepare Competition` - `Technical Configuration ` page.

     - paste the secret key on the right-hand side -- use the "eye" icon to see what you pasted.
     - copy and paste the correct URL for the publicresults application we opened in step 5.
     - Click on update.

     ![GkwHZ4ZHeW](img/PublicResults/Example/GkwHZ4ZHeW.png)

 ## Test the setup

1. Create the athletes for a group

     - load a registration file or create an athlete
     - go to the weigh-in screen and add body weight and starting weights.

2. Go to the announcer page and select a group.
     ![layHD1stff](img/PublicResults/Example/layHD1stff.png)

3. As soon as a group is selected, publicresults is updated.  Switch to the publicresults tab.   Because we have not started lifting, you should see the following.   If your site has more than one platform, you will see a page that allows you to select which platform you want to watch.

     ![RIxGO9RShj](img/PublicResults/Example/RIxGO9RShj.png)

5. The announcer selects a group, and clicks on "Countdown to Introduction", and starts the countdown.
     ![vC53fjpSuq](img/PublicResults/Example/vC53fjpSuq.png)

6. The publicresults screen immediately switches to the countdown.![X0qHw40LKh](img/PublicResults/Example/X0qHw40LKh.png)

7. From then on, the publicresults screen will track the competition. It just repeats the events taking place on the local scoreboard.

## Check for updates

Once you have created the application once, you can download a program that will check for updates and remote control Heroku to grab them.   See [Instructions](https://github.com/jflamy-dev/owlcms-heroku-updater) and [Releases · owlcms/owlcms4-heroku-updater (github.com)](https://github.com/owlcms/owlcms4-heroku-updater/releases)

