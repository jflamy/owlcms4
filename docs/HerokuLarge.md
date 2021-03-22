# Large Competition Setup on Heroku

Heroku is a cloud service that makes it easy to start from a free service for small competitions, but also support an affordable paid tier for larger meets.  In order to host a large competition, we need to update so the application gets dedicated and faster computing resources as well as more memory. 

As an order of magnitude, setting up (5 days) and running a large 2-day national competition would cost something like 30 US$  You can even save a few more dollars by doing the setup on the free tier and either copying the database from one app to the other or sharing between apps.

> The key thing when using paid tiers is to *scale down* when done, otherwise billing continues! See bottom of this page.

## Initial Setup

1. Install owlcms, as described on [this page](Heroku).
2. Install publicresults, and connect the two applications together, as explained on [this page](Remote)
3. Update the account to the professional plan.  This requires that you give a credit card for payment.

## Scale-up

These steps are needed a few hours before the competition.  They will cause the applications to be redeployed on a different (larger) container, which takes less than a minute.

1. Update publicresults to the 2x dyno type. A "dyno" is the Heroku word for a Linux container.

   1. Select the "Resources" section
   2. If you are on the free tier, change the dyno type to "Professional"
   3. Select "Standard-2X" as the performance level by clicking on the hexagon and dropping in the menu.  This will give us the memory we need.

    ![2x](C:\Dev\git\owlcms4\docs\img\Heroku\2x.png)

   4. **IMPORTANT**: If this is not your first meet, and you have previously scaled down the application, you need to reset the number of containers to 1.   The only meaningful numbers for us are 0 (off and not billed) and 1 (on and billed).   Use the pencil icon to edit the dyno formation and set the value

      ![dynocount](C:\Dev\git\owlcms4\docs\img\Heroku\dynocount.png)

2. Update owlcms to the Performance-M dyno type.  Same steps as above.

   1. Select the "Resources" section
   2. If you are on the free tier, change the dyno type to "Professional"
   3. Select "Performance-M as the performance level by clicking on the hexagon and dropping in the menu.  This will give us the computing we need (and more than enough memory)

    ![perf-m](C:\Dev\git\owlcms4\docs\img\Heroku\perf-m.png)

## IMPORTANT: Scale-down

Normally this step would not be required, unless for some reason you want to keep the applications.

It is usually simpler to juste delete the apps as soon as done.  

Should you need to keep them:

1. Use the same screens as above to shut down publicresults 
   1. Move it back to the 1X configuration.
   2. Turn off publicresults if you don't need it by moving the number of dynos down to **zero** (this will save you 25$ per month - 300$ per year.)
2. Use the same screens as above to shut down owlcms
   1. <u>Move it back to the 1X configuration.</u>  This will bring pricing back to 25$ per month
   2. Turn off owlcms if you don't need it by moving the number of dynos down to zero.  Remember that you can check the Excel registration sheets for athletes, groups, categories and so on using the free version.  So you have 3 options
      - Turn off owlcms, restart it on the 1x tier a week before a meet, and do all the work there.
      - Use a free owlcms, and reload the Excel on the paid tier.
      - Use a free owlcms and copy the database to the paid tier.  See below.

### Copying databases

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