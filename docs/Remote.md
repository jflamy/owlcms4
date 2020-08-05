The `publicresults` application is used to make applications results available to the public at large, or to run virtual competitions. As shown in the following diagram (and further discussed in on [this page](Virtual):![Slide2](img/PublicResults/CloudExplained/Slide2.SVG)

- updates are sent from the competition site to  the publicresults application running on the cloud.  
- The general public, whether at the competition site or anywhere on the internet, can connect to publicresults and  see the scoreboards via their phone or laptop.  
- There is no load put on the competition site other than sending an update to the publicresults application.  The publicresults application takes all the load for the public queries.

## Install on Heroku

1. Get a free Heroku account -- go to [https://heroku.com](https://heroku.com) and sign up!

2. Click on this Button [![Deploy to Heroku](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy?template=https://github.com/jflamy/owlcms-publicresults)

3. Enter your Heroku account (or create one if you haven't)

4. Enter a meaningful application name.  In the following example, we use `owlcms-test-publicresults` We will later use `owlcms-test-publicresults.herokuapp.com`  to see the results.  You should use a name that makes sense to your club or federation.

    *(Reminder: You can click on the images to make them bigger)*

    ![020_appName](img/PublicResults/020_appName.png)

5. Deploy the application. This will magically fetch the application from where you clicked on the `Deploy to Heroku` button, install it, and make it available to the public.
    ![030_deploy](img/PublicResults/030_deploy.png)

6. You can check that the application is running by starting a new browser tab. In our example, we connect to `https://owlcms-test-publicresults.herokuapp.com`
   

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

11. Start a new browser tab and go to the address for the application (or you can use the `Open app` button at the top right) and check that the application is waiting.
     ![AAAxZYQKZK](img/PublicResults/Example/AAAxZYQKZK.png)

     ## Configure Updates from `owlcms`

12. Open the owlcms application on Heroku and go to the `Prepare Competition` - `Technical Configuration ` page.

     - paste the secret key on the right-hand side -- use the "eye" icon to see what you pasted.
     - copy and paste the correct URL for the publicresults application we opened in step 5.
     - Click on update.

     ![GkwHZ4ZHeW](img/PublicResults/Example/GkwHZ4ZHeW.png)

     ## Test the setup

13. Create the athletes for a group

     - load a registration file or create an athlete
     - go to the weigh-in screen and add body weight and starting weights.

14. Go to the announcer page and select a group.
     ![layHD1stff](img/PublicResults/Example/layHD1stff.png)

15. As soon as a group is selected, publicresults is updated.  Switch to publicresults, and you should see
     ![V1YaYXsAWr](img/PublicResults/Example/V1YaYXsAWr.png)

16. Click on "Platform A" and because no group is currently lifting, you see

     ![RIxGO9RShj](img/PublicResults/Example/RIxGO9RShj.png)

17. The announcer selects a group, and clicks on "Countdown to Introduction", and starts the countdown.
     ![vC53fjpSuq](img/PublicResults/Example/vC53fjpSuq.png)

18. The publicresults screen immediately switches to the countdown.![X0qHw40LKh](img/PublicResults/Example/X0qHw40LKh.png)

19. From then on, the publicresults screen will track the competition. It just repeats the events taking place on the local scoreboard.
