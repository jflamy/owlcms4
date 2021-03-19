# Broadcasting a Virtual Competition

This page explains how to broacast a Virtual Meet using Zoom and OBS (Open Broadcast Software).

Requirements:

1. A laptop and at least one external monitor.
2. A license for Zoom, since meets last longer than the 40 minutes free version.  You may use another meeting software, but Zoom is likely the one most people know.
3. The [OBS Studio](https://obsproject.com/) free software that will combine what we want to show and stream it out.
4. A Facebook account -- we will use Facebook live for this demo, but you can use whatever streaming service OBS Studio supports (including YouTube, Twitch, etc.)

## Install OBS

Download  [OBS Studio](https://obsproject.com/) and install it.

In OBS Studio, you combine individual elements called *sources* to create *scenes*.  In our example, we will create two scenes

- One scene will take our laptop screen (completely) as one source and overlay the owlcms attempt board as a second surce (OBS has a built-in browser that we can use as a source)
- A second scene will take the full scoreboard.
- You can have as many scenes as you want, you could have others using slide shows or videos from your sponsors as sources, etc.

## Start owlcms

1. Start owlcms and enter enough information to get a group going
2. Go to "Start Displays" and start an Attempt Board
3. Start a Scoreboard

## Define a Scoreboard Scene

As a first possible output to our live stream, let's create a Scoreboard view to be used when the platform is empty.

**Create a Scene.** Click on the + below the Scenes pane and give the Scene a name.  You can rename it afterwards.

![CreateScene](img/CreateScene.png)

**Create a Source:** Next we can add as many video and sound sources as we want.  For our first scene, we only need one, a web browser view for the owlcms scoreboard.  OBS Studio has a built-in browser for that purpose.

Click on the + below the Sources pane and select Browser.  

![browser1](img/browser1.png)

Select "Create New" and give your source a unique name (you can reuse the same source in several scenes).

![SourceNaming1](img/SourceNaming1.png)

Hit OK, Type the URL to the owlcms display.  If you will be using a different site on competition day, you can change it later. Use 1920  and 1080 as the dimensions, we will shrink and crop later if needed.

![browserprops1](img/browserprops1.png)

Hit OK. OBS then displays the current scene, with the source highlighted in red.  We have defined our first scene.  Close the definition pane.  Whenever you click on the scene in the list at the left, the video output switches to that scene.

## ![scoreboard](img/scoreboard.png)

## Define the Live Video with Attempt Board Scene

The second scene will be built with two elements. 

- We will use the laptop screen as our live source to show the current athlete -- Zoom will be displaying fullscreen on the laptop.
- We will open the owlcms attempt board and shrink it so it occupies a corner of the screen.  This will show the athlete's info, the requested weight, the timer, and the decisions.

**Create the scene**: Same as before, create a new scene with a meaningful name -- say "Athlete+Board"

**Create the Live Video source:** Create a Display Capture source using the + sign at the bottom of the source pane.

![screenSource](img/screenSource.png)

**Name the source:** Give the source a meaningful name, like "Live" or "Screen".

**Select the display:** Pick your laptop display from the list.  You should see whatever is currently on your laptop display in the preview.

![Screen](img/Screen.png)

If you see a black area instead, you most likely have a laptop with multiple graphic cards.  You will need to change a Windows setting so OBS can use the one associated with your laptop.  Go to the bottom of this document and perform the steps under "Select Graphics Card", then come back here.

Now we overlay the attempt board.

**Create the Attempt Board Source:** We now redo the same process as before for the scoreboard, but this time we use the Attempt Board as our URL.  So we create a source using the + at the bottom, select "Browser" as our type, give it a meaningful name like "Attempt Board".

**Define the properties:** We use the URL for the display (ends with "/display/attemptBoard"). We make it full-size (1920 x 1080).

![AttemptBoardProperties](img/AttemptBoardProperties.png)

**Shrink the view:**  Using the handles, you can shrink the view and also drag it to where you want.

![Shrink](img/Shrink.png)

**Check the magic**:  If you open an image on the laptop screen, you will immediately see it in OBS Studio as the background. 

![IMG_0978](img/IMG_0978.jpg)



## Sound

Normally there is nothing to do.  OBS will grab sound from the default output device.  Since Zoom does not send back your own sound to your own computer, the video operator will be able to participate in the Zoom meeting but the operator's voice will not be captured. 

## Setup Zoom

Start Zoom and go to Home, and then click on the settings Icon at the top right.  Make sure that "Use Dual Monitors" is selected.

![zoomsettings](img/zoomsettings.png)

**Setup your screens**

- Rearrange the Windows so you have the Gallery View and OBS on the same screen.  
- You can drag the panes in OBS to make room. 
-  On the laptop screen, double click on the window title bar to make it go full screen.

**Select the Zoom view**

Right-click on the current athlete in the gallery view and use the "Spotlight" and "Replace Spotlight" menu entry to to control which of the athletes is shown on the laptop screen and on all the Zoom participants. If someone else controls the Zoom for the competition, you can use "Pin" and "Replace Pin" to ignore the spotlight.

![zoom](img/zoom.png)

**Switch Scenes**

During lulls, you can switch to the scoreboard view.  Just click on one of the entries in the Scenes list.



## Setup Streaming

In order to stream to Facebook Live, go to the File menu and select Settings.   Select Facebook Live as your streaming service.  Click on the "Get Streaming Key" entry

![StreamSettings](img/StreamSettings.png)

This will bring you to the Facebook configuration page.  

1. Click on the copy button next to the streaming key.  Note: you may need to log out of your own account and log in to your Federation or Club account to get the proper key and be able to post to the right page.
2. Go back to OBS, clear the streaming key area and paste the new key, and click OK.  Go to the main OBS Screen and click "Start Streaming" using the buttons at the bottom left.  After maybe 10-15 seconds, you should see your stream appear in the preview window.
3. Prepare a nice message
4. Go Live.

![FBLive](img/FBLive.png)



## Troubleshooting: Select the Graphics Card for OBS

This step is sometimes needed for laptops that have two video cards (such as "gaming" laptops).  OBS Studio can only capture full-screen video from one card at a time.  The setting is done using the Windows Graphics Settings. 

- Click on the Windows icon at the bottom left and locate the"Graphics Settings" menu
- Locate the OBS Studio app and set the PowerSaving

![graphicsSettings](img/graphicsSettings.png)

