## Physical Setup

OWLCMS4 is a web-based application. This means that each screen or display is attached to a laptop or miniPC (Windows, Linux, ChromeOS)., and that a browser such as Chrome or Firefox runs on each computer.

The program is extremely scalable.  You can start by using a single laptop to run a club meet (see below for [examples](#local-access-over-a-local-network).) 

At the opposite end, a full setup for a state competition using refereeing devices might look as follows:

![StateCompetition](img/equipment/FullCompetition.svg)

You can also add a jury laptop with 5 refereeing devices, add additional results displays, as many as you want.  You can even replicate the full setup on multiple platforms, all running at once.  In all cases, there is only one OWLCMS master server (either a laptop running locally, or a cloud application) 

## Computer Requirements

- The server software will run either 
  - on any reasonably recent laptop (this laptop will act as a master in a local networking setup, see [below](#local-access-over-a-local-network) for details.
  - or on a cloud service. We test and support the Heroku cloud service, which has a free tier suitable for owlcms. See [below](#cloud-access-over-the-internet) for more info.
- For the user interface and displays,  It is recommended to use a recent version of **Chrome** or **Firefox** on a **laptop** or **miniPC** (Windows, Mac, Linux, ChromeOS). 

  - You can use just about any laptop.  Refurbished Chromebooks or Windows laptops work well.
  - You can even use [Raspberry Pi 3B](https://www.canakit.com/raspberry-pi-3-model-b-plus-starter-kit.html) that cost less than 75$ each fully configured including cables and storage.  Refer to this [section](RaspberryPi) for tips .
  - Apple iPhones and iPads are supported as [mobile refereeing devices](Refereeing#mobile-device-refereeing).  Display features such as the Scoreboard do work.

## Sound Requirements

#### Chrome configuration

Recent versions of Chrome no longer allow web pages to emit sounds by themselves.  In order to hear the signals from the clock and the down signal, we have to override this setting.

- in Chrome, go to page ``chrome://flags``  and search for ``autoplay policy``  in the search bar.
  Set the parameter to ``No user gesture is required``

- OR --  Create a shortcut to chrome and add the following flag `--autoplay-policy=no-user-gesture-required`

#### Master Laptop Sound

Some combinations of browser and operating system produce garbled sound. If that is the case, you may want to use the master laptop to produce the sound (and wire your speakers to the audio output of the master laptop.)   See [these explanations.](Preparation#associating-an-audio-output-with-a-platform)

## Networking Requirements

There are three ways to use OWLCMS4: cloud-based, local, and solo.  Which one you use depends on your circumstances, please read on.

### Cloud access over the Internet

In this setup, OWLCMS4 executes on an external cloud service (we provide [instructions for Heroku](Heroku), which is physically hosted on Amazon AWS).  Nothing is installed locally: all the laptops and miniPCs simply run a browser, which connects to the remote site.  The address that all the browsers need to use is determined when configuring the cloud service -- in the following example, our demo site https://owlcms4.herokuapp.com is used.

![010_Cloud](img/equipment/010_Cloud.PNG)

### Local access over a local network

If there is no Internet access where you hold your meet, or if you prefer not having to rely on it, the second option is to install OWLCMS4 on a good laptop.  OWLCMS4 is started on this laptop, which is designated as the *master*.  

![020_local](img/equipment/020_local.PNG)

It is perhaps surprising to use a laptop as a server, but as the picture shows, it's really the same thing: the display computer runs a browser and asks another computer to provide a web page.  In the local setup, the  Internet is the small router, and the server is the master laptop.  The differences are minor:

- there is no need for the Internet, all the networking takes place in the router.
- in a simple local network, the simplest thing is often to use the numerical addresses that the router assigns to each machine.  This is what is illustrated above.
- when OWLCMS4 starts up, it opens a browser window which tells you [what address the other laptops should use](LocalSetup#initial-startup) to connect to OWLCMS4.  In the illustration, the laptop at the right is the master, and it can be reached from the laptop at the left using https://192.168.1.100 .  The white browser window on the master laptop will show this information at startup.

### Single-computer setup

You can also use the master laptop by itself.  This is useful to prepare for a competition that will run on that laptop.  You can even run a competition with just an announcer, just to announce athletes, keep time and record decisions. 

The situation is the same as before: the display is done via a browser that asks OWLCMS4 for its information.  The only difference is that the two programs are running on the same computer.

-  OWLCMS4 runs on the laptop (pictured in red). It displays its status in a black command-line window.
- A browser (Chrome or Firefox, pictured in blue), which will display its output in a browser window.   There is no need for a router because the two programs are on the same machine.   In such a case, there are magic addresses http://localhost or http://127.0.0.1 that allow the connection to be made locally.

![030_solo](img/equipment/030_solo.PNG)



[^1]: The only caveat is that some refereeing devices require [workarounds](Refereeing#notes-for-raspbery-pi-users-with-delcom-keypads)