# Public Scoreboard

The `publicresults` application is used to make applications results available to the public at large, or to participants in virtual competition. For this reason, it is usually run in the cloud.

The following example shows owlcms running standalone in a gym, sending information to publicresults running in the cloud.
![Slide2](img/PublicResults/CloudExplained/Slide2.SVG)

- updates are sent from the competition site to  the publicresults application running on the cloud whenever there is a significant event (clock start/stop, weight changes, lift decisions, etc.)  
- The public can connect to publicresults and  see the scoreboards via their phone or laptop no matter where they are
- There is no load put on the competition site other than sending an update to the publicresults application.  The publicresults application takes all the load for the public queries.

The other common configuration is when both owlcms and publicresults are in the cloud,  That scenario is discussed [here](VirtualOverview).

## Install publicresults

There are three ways to install and run publicresults.  The first two options are usual, since the purpose is normally go give access to all persons in attendance and all persons live streaming.

1. Install it on the Heroku cloud.  See [this page](Heroku) for details
2. Install it on the Fly.io cloud.  See [this page](Fly) for details.
3. Install it locally.  See [this page](PublicResults_Local) for details.



## Configure `owlcms` to send updates

1. Open the owlcms application on Heroku and go to the `Prepare Competition` - `Language and System Settings ` page.

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



## Update the program when you update owlcms

- For fly.io, if your application is called `myclub-scoreboard` you would use

  ```
  fly deploy --app myclub-scoreboard -i owlcms/publicresults-stable --detach
  ```

  

- For Heroku Once you have created the application once, you can download a program that will check for updates and remote control Heroku to grab them.   See [Instructions](https://github.com/owlcms/owlcms-heroku-updater) and [Releases · owlcms/owlcms4-heroku-updater (github.com)](https://github.com/owlcms/owlcms4-heroku-updater/releases)

