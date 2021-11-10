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
   4. **IMPORTANT**: Check the dyno count by using the pencil at the right.  **The value should be 1** (the only other value you can use is 0 to turn off the application).  Values larger than 1 will cause the applications to malfunction.
   ![dynocount](img\Heroku\dynocount.png)
   
2. Update owlcms to the Standard-2X or Performance-M dyno type.  Until you have been billed once by Heroku, you will not be able to get Performance-M.  Standard-2X is sufficient for most meets, but the difference in actual price with Performance-M is small - remember that you only pay for the actual minutes where you are using the high-performance setting.

   1. Select the "Resources" section
   2. If you are on the free tier, change the dyno type to "Professional"
   3. Select "Performance-M" or "Standard-2X" as the performance level by clicking on the hexagon and dropping in the menu.  This will give us the computing we need (and more than enough memory)
   ![perf-m](img\Heroku\perf-m.png)
   4. **IMPORTANT**: Check the dyno count by using the pencil at the right.  **The value should be 1** (the only other value you can use is 0 to turn off the application).  Values larger than 1 will cause the applications to malfunction.

## Scale-down

Deleting the applications will stop billing.  But in most instances you will want to keep the applications for at least a few days to fetch results, print the final competition package, etc.

There are two main ways to scale down

**Method 1: Keep a paying tier, but scaled down**

1. Change the performance level back to 1X (25$) or 2X (50$) for a few days (that's 1.25$ or 2.50$ per day). 
2. If you wish to turn off the application and stop paying, change the dyno count to **0** using the pencil.

**Method 2: Go back to the free tier or hobby tier**.

1. Use the `Change Dyno Type` button to revert back to the hobby tier (7$)
2. Once you are back on the hobby tier, there is a toggle button to switch back to free mode (0$)



## Additional Information

This section is not required in normal situations. It is provided here as a reference.

#### Database Backups

Go to the  Resources page, and click on the Heroku Postgres icon.

Select the Durability section.  You can take and download backups (Postgres .dump format)

#### Copying databases

Should you wish to copy databases around.  You can use the `heroku pg:backups` command to take backups and restore them to another application (or a local postgres database, or just keep them for archival, whatever)

This requires installing the command line interface and typing a few commands.

1. Install the [Heroku command-line interface](https://devcenter.heroku.com/articles/heroku-cli#download-and-install)

2. Assume we have a free application called `ourtest` for doing the setup and the real one is called `ourmeet`

3. Take a backup of the `ourtest` database: 

   ```heroku pg:backups capture --app ourtest```

   This will create a backup "b001" "b002" and so on every time it is run.  Assume our latest backup is "b002"

4. Find the URL of the production database.    This is found on the "Settings" page of the production application `ourmeet`.  Click "Reveal Config Vars" and copy the very long DATABASE_URL string that looks like the following (this is an example, get the real one)
   `postgres://emcpibuwvwetta:7c9b9e6b8401d1983564e1e2fcd21545d9335e7f30895b1597244209426fad27@ec2-52-21-252-142.compute-1.amazonaws.com:5432/d83gikckf3v106`

5. Then we restore our backup b002 from the free ourtest to the paying production as follows (all of this goes to a single line).  The `--app ourtest` at the end of the line is what specifies that it's the b002 backup from `ourtest` that is being used as source.

   ``` bash
   heroku pg:backups restore b002 postgres://emcpibuwvwetta:7c9b9e6b8401d1983564e1e2fcd21545d9335e7f30895b1597244209426fad27@ec2-52-21-252-142.compute-1.amazonaws.com:5432/d83gikckf3v106 --app ourtest
   ```