## Cloud-based Public Scoreboard Configuration

In order to make the results available to remote gyms or to the public at large, you need to have performed the following steps beforehand.

1. **install owlcms**.  For a virtual competition where officials are not all at the same competition site, you should [install owlcms in the Cloud](Heroku).
2. **install publicresults**, 
   - Normally, publicresults is installed in the cloud as explained in [this page](Remote).
   - Note that It is also possible to install publicresults locally if you do not have internet access and wish a strictly local solution. See [this page](PublicResults_local) instead of this one.
3. if you have already installed these applications, update them to the latest release by using the [automatic update procedure](UpdatingCloudApplications).

### Configuration and Testing Steps

1. Go to the publicresults application configuration and open the configuration variables section.
   ![5tFs827XLo](img/PublicResults/Example/5tFs827XLo.png)

2. Check for a OWLCMS_UPDATEKEY variable.  If absent, use the add button to create it.  The  password to your Heroku account and the update key is what prevents vandals from messing up your scoreboards.  Do not share them, and make sure they cannot be guessed easily.
   ![ljyvckBm6F](img/PublicResults/Example/ljyvckBm6F.png)

3. Copy the value for the update key (Ctrl-C)

4. Restart the publicresults application
   ![6Ihs0ei0Ad](img/PublicResults/Example/6Ihs0ei0Ad.png)

5. Start a new browser tab and go to the address for the application (or you can use the `Open app` button at the top right) and check that the application is waiting.
   ![AAAxZYQKZK](img/PublicResults/Example/AAAxZYQKZK.png)

6. Open the owlcms application on Heroku and go to the `Prepare Competition` - `Language and System Settings ` page.

   - paste the secret key on the right-hand side -- use the "eye" icon to see what you pasted.
   - copy and paste the correct URL for the publicresults application we opened in step 5.
   - Click on update.

   ![GkwHZ4ZHeW](img/PublicResults/Example/GkwHZ4ZHeW.png)

7. Create the athletes for a group

   - load a registration file or create an athlete
   - go to the weigh-in screen and add body weight and starting weights.

8. Go to the announcer page and select a group.
   ![layHD1stff](img/PublicResults/Example/layHD1stff.png)

9. As soon as a group is selected, publicresults is updated.  Switch to publicresults, and you should see
   ![V1YaYXsAWr](img/PublicResults/Example/V1YaYXsAWr.png)

10. Click on "Platform A" and because no group is currently lifting, you see

    ![RIxGO9RShj](img/PublicResults/Example/RIxGO9RShj.png)

11. The announcer selects a group, and clicks on "Countdown to Introduction", and starts the countdown.
    ![vC53fjpSuq](img/PublicResults/Example/vC53fjpSuq.png)

12. The publicresults screen immediately switches to the countdown.![X0qHw40LKh](img/PublicResults/Example/X0qHw40LKh.png)

13. From then on, the publicresults screen will track the competition. It just repeats the events taking place on the local scoreboard.