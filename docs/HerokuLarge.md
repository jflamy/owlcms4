# Large Competition Setup on Heroku

Heroku is a cloud service that makes it easy to start from a free service for small competitions, but also support an affordable paid tier for larger meets.  In order to host a large competition, we need to update so the application gets dedicated and faster computing resources as well as more memory. 

As an order of magnitude, running a large 2-day national competition would cost something like 20 US$  You can create your application as a free setup, and scale it up to high-capacity, then scale it down back to the free tier when you are done (or delete it).  Pricing is by second, based on the actual elapsed time the application is running.

> The key thing when using paid tiers is to *scale down* when done, otherwise billing continues! See the [Scale Down](#scale-down)

## Initial Setup

1. Install owlcms, as described on [this page](Heroku).
2. Install publicresults, and connect the two applications together, as explained on [this page](Remote)
3. Associate a credit card with your account.  This is done by clicking on your profile icon at the top right and changing the account settings.

## Scale-up

These steps are needed a few hours before the competition.  They will cause the applications to be redeployed on a different (larger) container, which takes less than a minute.

1. Update publicresults to the 2x dyno type. A "dyno" is the Heroku word for a Linux container.

   1. Select the "Resources" section
   2. If you are on the free tier, change the dyno type to "Professional"
   3. Select "Standard-2X" as the performance level by clicking on the hexagon and dropping in the menu.  This will give us the memory we need.

    ![2x](img\Heroku\2x.png)

   4. **IMPORTANT**: Use the pencil icon to edit the dyno formation and make sure that the dyno count is set to 1.  <u>Values other than 1 will NOT work</u>.   (0 is used to turn off the application and its billing)

      ![dynocount](img\Heroku\dynocount.png)

2. Update owlcms to the Performance-M dyno type.  Same steps as above.

   1. Select the "Resources" section
   2. If you are on the free tier, change the dyno type to "Professional"
   3. Select "Performance-M as the performance level by clicking on the hexagon and dropping in the menu.  This will give us the computing we need (and more than enough memory)
4. Make sure that the dyno count is set to 1 (larger values will NOT work.)
   
    ![perf-m](img\Heroku\perf-m.png)

## Scale-down

You can also just delete the applications, which will stop billing.

If you want to keep your application setup, and just rename the applications, you should scale them down

1. Use the same screens as above to shut down publicresults as follows
   1. Setting the dyno count to 0 shuts down the application (and stops billing)
   2. You should change the dyno type back to 1X if you want.  You can even use the change dyno type button to revert back to the hobby tier. 
   3. If you go back the hobby tier, there is a button to switch between hobby (7$ per month), or the free (0$ per month) setup.
2. Use the same procedure to scale down owlcms. Due to the high price of the Performance Tier, i<u>t is very important to set the dyno type to 1X, or to go back to the hobby/free tier</u> (using the change dyno type button).    <u>Set the dyno count to 0 if you want to stop billing</u>.







The following sections are not needed for normal setups, but are here for reference.

## Database Backups

Go to the  Resources page, and click on the Heroku Postgres icon.

Select the Durability section.  You can take and download backups (Postgres .dump format)

## Copying databases

This section should not be needed, but here is the procedure should you wish to copy databases around.  You can use the `heroku pg:backups` command to take backups and restore them to another application (or a local postgres database, or just keep them for archival, whatever)

This requires installing the command line interface and typing a few commands.

1. Install the [Heroku command-line interface](https://devcenter.heroku.com/articles/heroku-cli#download-and-install)

2. Assume we have a free application called `ourtest` for doing the setup and the real one is called `ourmeet`

3. Take a backup of the ourtest database: 

   ```heroku pg:backups capture --app ourtest```

   This will create a backup "b001" "b002" and so on every time it is run.  Assume our latest backup is "b002"

4. Find the URL of the production database.    This is found on the "Settings" page of the production application `ourmeet`.  Click "Reveal Config Vars" and copy the very long DATABASE_URL string that looks like the following (this is an example, get the real one)
   `postgres://emcpibuwvwetta:7c9b9e6b8401d1983564e1e2fcd21545d9335e7f30895b1597244209426fad27@ec2-52-21-252-142.compute-1.amazonaws.com:5432/d83gikckf3v106`

5. Then we restore our backup b002 from the free ourtest to the paying production as follows (all of this goes to a single line).  The `--app ourtest` at the end of the line is what specifies that it's the b002 backup from `ourtest` that is being used as source.

   ``` bash
   heroku pg:backups restore b002 postgres://emcpibuwvwetta:7c9b9e6b8401d1983564e1e2fcd21545d9335e7f30895b1597244209426fad27@ec2-52-21-252-142.compute-1.amazonaws.com:5432/d83gikckf3v106 --app ourtest
   ```