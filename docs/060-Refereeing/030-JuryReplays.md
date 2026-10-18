# Self-Service Jury Replays

The `video` module is a complement to owlcms that, among other features, allows capturing jury replays.  The lifts are automatically trimmed to start a little before the lift and stop after the decisions have been shown.  The jury can watch them using a regular browser, and the replays are organized by session and time (most recent first)

### Starting the Video Module

The video module is started from the control panel, but is not on by default.  You need to enable it.  You will then be offered to install it.

![image-20261018161810768](img/030-JuryReplays/image-20261018161810768.png)

Once installed, you get the usual control panel look, and you start the program using the highlighted button.  Updates and multiple versions work exactly the same as the other modules.

![image-20261018162034826](img/030-JuryReplays/image-20261018162034826.png)

Starting the module brings us to a monitoring page where we start cameras.  For the purpose of this tutorial, we will start a Logitech webcam attached to our laptop.  

By default, the program intentionally hides the built-in cameras since we don't use those for replays.  It only shows cameras that were previously configured.  In this example, we have previously configured 4 cameras, so these are visible.

![image-20261018163329678](img/030-JuryReplays/image-20261018163329678.png)

In order to perform tests at home before a meet, it is convenient to use the built-in cameras, so we enable them from the Cameras menu.

![image-20261018163728767](img/030-JuryReplays/image-20261018163728767.png)

and the Configuration menu shows all that have been seen. On a Mac, this will include your phone as a linked camera, we disable it.

![image-20261018163930033](img/030-JuryReplays/image-20261018163930033.png)

Going back to the monitoring page, we start the built-in camera. The preview button allows you to see what is being captured.

![image-20261018164223290](img/030-JuryReplays/image-20261018164223290.png)

## Setting up replay cameras

From the Replays tab, we use the Select Cameras button.  In our initial test, we don't have our actual replay cameras at home, so we unselect them.  We only keep our built-in camera.  At the competition site, we will select the real cameras, of course.

![image-20261018164556172](img/030-JuryReplays/image-20261018164556172.png)

## Testing Replays

1. Start OWLCMS with some data: a session with some weighed-in athletes

2. Check that the Replays module found OWLCMS (1). If not, give the address using the menu (2) and restart the Replays module (3).

   ![image-20261018170911429](img/030-JuryReplays/image-20261018170911429.png)

3. Click on the link at the top of the replays window

   ![image-20261018165952366](img/030-JuryReplays/image-20261018165952366.png)

   You should see something like this

   ![image-20261018170051944](img/030-JuryReplays/image-20261018170051944.png)

4. In OWLCMS, run start the session and start the clock.  

   1. Let the clock run for 10 seconds, then show a "thumbs up" to the camera, stop the clock.
   2. Wait 10 seconds, then enter your bad lift decision (you can use the controls on the announcer screen) - show a "thumbs down" towards your laptop camera.

   What should happen: during the time from the start of the clock to about 5 seconds after decision, the replays screen should show that it is recording

   ![image-20261018171117212](img/030-JuryReplays/image-20261018171117212.png)

   After the lift, you should see "Ready" and the video added to the list at the top.
   ![image-20261018171340719](img/030-JuryReplays/image-20261018171340719.png)

### Watching Replays

The jury can simply get this screen and click on the links.  This will open the standard video browsing experience from the browser.

For an easier experience, there is a tracker module that can fetch the videos and make a nicer interface.

![image-20261018172006810](img/030-JuryReplays/image-20261018172006810.png)
