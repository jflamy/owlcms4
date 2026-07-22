This page explains how to broadcast a Meet using OBS (Open Broadcast Software).  The process is the same for a live meet or for a virtual meet.  For a live meet, you will use a "video capture source".  For a Zoom meet, you will use a window running zoom, or a monitor showing zoom as a source.

Requirements:

1. A laptop and at least one external monitor.
3. The [OBS Studio](https://obsproject.com/) free software that will combine what we want to show and stream it out.
4. A Facebook or YouTube account -- we will use Facebook live for this demo, but you can use whatever streaming service OBS Studio supports (including YouTube, Twitch, etc.)

## 1. Install OBS

Download [OBS Studio](https://obsproject.com/) and install it.   

- Plug in your camera

In OBS Studio, you combine individual elements called ***sources*** to create ***scenes***.  In our example, we will create two scenes

- The first scene will just show the scoreboard.  OBS has a built-in browser, and it can use any URL as a *browser source*.
- The second scene will take the camera input and the current athlete information from owlcms as a browser source. 

You can have as many scenes as you want, you could have others using slide shows or videos from your sponsors as sources, etc

## 2. Setup owlcms

You should load owlcms with some competition data.  Importing a previous competition is useful. 

In order to show scoreboards. there are two options

1. Use a transparent mode, where the scoreboard "floats" over the camera.  This gives a professional look.
2. Use the normal scoreboards from the application

To select which mode you want, go to the system settings page, and then select "Customization"

![010](nimg/4000OBS/010.png)

On the customization page, select `transparent` to get the "floating scoreboard" look, or "nogrid" to have the same black background as with the scoreboards used on-site.

![020](nimg/4000OBS/020.png)

## 3. Prepare the scoreboard view

1. Start owlcms and enter enough information to get a session going.  Start the session as the announcer.

2. Go to the Video Streaming section and start a simple scoreboard.

   ![030](nimg/4000OBS/030.png)

3. When the scoreboard starts, open it full screen, or make the window as close as possible to 1920x1080 as possible (the easiest way is to temporarily set your screen resolution to 1920x1080 if you can).  Click on the window to bring up the menu and adjust the font size and team size to what you prefer

   In the following example, the team width is set to 14 and the font size to 1.3![035](nimg/4000OBS/035a.png)

4. Copy the URL
   ![036](nimg/4000OBS/036.png)

## 3. Define a Scene to Show the Scoreboard

1. Let's create a scene that we will use when no athlete is lifting.  In the "Scenes" panel, use the +

   ![040](nimg/4000OBS/040.png)

2. Add a browser source named "Scoreboard" -- OBS has a built-in version of the Chrome engine, so it just talks to owlcms like any other display device.
   ![042](nimg/4000OBS/042.png)

   ![044](nimg/4000OBS/044.png)

3. Paste the URL, and set the resolution to 1920x1080.  You should then see the scoreboard.  The information displayed comes from the session description - if there is none, the session code is displayed.

   ![046](nimg/4000OBS/046.png)

   ![048](nimg/4000OBS/048.png)

4. Now we go back to the Scene panel. We use the + button to define a new "Video Capture Device" for our camera.![50](nimg/4000OBS/50.png)

   We name our source "Camera"![52](nimg/4000OBS/52.png)

   Normally the default parameters are ok

   ![54](nimg/4000OBS/54.png)

5. We now extend the Camera source to the full size of the screen.  Use the corner handles, or use the Control-F shortcut on Windows.
   ![56](nimg/4000OBS/56.png)

6. We reorder our sources by using the arrows at the bottom so the Camera is behind our floating scoreboard
   ![56](nimg/4000OBS/58.png)

## 4. Prepare a Current Athlete view

The process used to create a scene for the athlete lifting with information at the bottom of the screen is similar.

1. Back to the Video Streaming page, open the Current Athlete view
   ![60](nimg/4000OBS/60.png)

2. Copy the URL.  The background of the display is transparent so we will be able to put the view anywhere we want and resize it![62](nimg/4000OBS/62.png)

3. Then we add, like earlier, our browser source with the URL we copied.  We define the resolution as 1920x1080

   ![66](nimg/4000OBS/66.png)

4. The view appears and we drag it to the bottom.
   ![68](nimg/4000OBS/68.png)

5. The we add the Camera source -- we reuse the same one that we already defined.
   ![72](nimg/4000OBS/72.png)

6. And we move the camera behind, as before.
   ![74](nimg/4000OBS/74.png)

7. Finally, we resize the bottom view to our liking
   ![76](nimg/4000OBS/76.png)

   

## 5. Advanced Options

### 5.1 Medals

If you also add a scene with the medals as a browser source, then if the announcer switches the medals display to a specific category, the video source will follow.  You can also switch the medals category from the video streaming.

### 5.2 Style Editing, Colors, Logos

In order to create you own video styles, copy the `nogrid` or `transparent` folder to your own name. 
The general process for style changes is described on the [Style Customization](Styles) page.  All these steps can be performed by a Web Designer or by a sufficiently motivated person -- the configuration uses standard CSS files for which there are many tutorials on the web.

- You can change the color of the video header to create a banner
- You can edit the style of the video header to include logos

Select your new folder on the `Prepare Competition > Language and System Settings > Customization`

> Note that if you change the styles, the OBS cache may need to be emptied.  On Windows,the cache is located in `%appdata%\obs-studio\plugin_config\obs-browser\Cache` -- exit OBS and delete everything inside.
>

![09_transparent](img/OBSVideo/09_transparent.jpg)



