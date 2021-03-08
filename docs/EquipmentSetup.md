## Physical Setup

OWLCMS is a web-based application.  In the simplest setup, you will run the program on a laptop, and it will open a browser so you can interact with owlcms.

Typically, however, you will want the coaches to see a scoreboard, etc.  You can use *any* device that can run a browser.  The cheapest available device like an old laptop, a FireTV stick with [Firefox installed](https://support.mozilla.org/en-US/kb/firefox-fire-tv),  a [Raspbery Pi](https://www.raspberrypi.org/products/raspberry-pi-400/), or a basic Chromebook will all do.  So a basic setup might look like

<center><img src="img/equipment/ClubCompetitionWIFI.png" alt="ClubCompetitionWIFI" style="zoom:80%;" /></center>

At the opposite end of the spectrum, a full setup for a state competition using refereeing devices might look as follows:

![StateCompetition](img/equipment/FullCompetition.svg)

You can also add a jury laptop with 5 refereeing devices, add additional results displays, as many as you want.  You can even replicate the full setup on multiple platforms, all running at once.  In all cases, there is only one OWLCMS primary server (either a laptop running locally, or a cloud application) 

## Computer Requirements

- The server software will run either 
  - on any reasonably recent laptop (this laptop will act as a primary server in a local networking setup, see [below](#local-access-over-a-local-network) for details.  In our experience, a Core i5 or equivalent is plenty.
  - or on a cloud service. We test and support the [Heroku cloud service](Heroku#Heroku), which has a free tier suitable for owlcms.
- For the user interface and displays,  It is recommended to use a recent version of **Chrome** or **Firefox** on any laptop/miniPC (Windows, Linux, Mac), or on a specialized display device (Amazon FireStick)
  -  The cheapest solutions to drive TV screens are Amazon FireTV stick with Firefox, and Raspberry Pi  computers (the [model 400](https://www.raspberrypi.org/products/raspberry-pi-400/), has everything built-in).
  - For the officials, you can use just about any laptop.  Refurbished Chromebooks or refurbished Windows laptops work well -- all that is needed is the ability to run a recent version of Chrome or Firefox.
  - Apple iPhones and iPads are ok as [mobile refereeing devices](Refereeing#mobile-device-refereeing).   Display features such as the Scoreboard also work.
  - Android phones and tablets work fine for all features (just install Chrome)

## Sound Requirements

By default, the browsers showing scoreboards will also emit sound. This is necessary when the application is run remotely.  You should mute all the laptops except the athlete-facing display or the attempt board, depending on where you connect speakers.

#### Primary Laptop Sound

Some combinations of browser and operating system produce garbled sound. If that is the case, you may want to use the primary laptop to produce the sound (and wire your speakers to the audio output of the primary laptop.)   See [these explanations.](Preparation#associating-an-audio-output-with-a-platform)

## Networking Requirements

When running locally, all that is required is a local Wi-Fi router.

If you are of the nervous kind, we do suggest that the announcer laptop, the attempt board, and the athlete-facing display where the referees are connected be linked using a wired connection.

When running over the cloud, the additional requirement is that the main router be connected to the internet.

When running several platforms, we recommend that you use a well-designed setup with multiple Wi-Fi access points.  Ask a technical person for advice.
