# Physical Setup

owlcms is a web-based application.  All the displays connect to owlcms using a browser.  In most scenarios, owlcms runs on a good laptop at the competition site, and all the other displays connect to that laptop.  For virtual meets, the computer running owlcms is actually running somewhere in the internet -- everything else on this page still applies.

## Simple Setup

In a small club meet, the announcer will often do everything - announce, enter the weight changes, and operate the clock. This can be done on a single laptop, as demonstrated in this [video](Demo1).  

More frequently, you have a meeting with multiple sessions, The following setup will allow you to comply with most requirements.  This adds a computer in the warmup area, and there is an attempt board on the platform.

<table>
<tr><td><img src='img/Gallery/ElSalvador.jpg'></img></td><td><img src='img/Gallery/ElSalvador_marshall.jpg'></img></td></tr></table>
The simplest setup does not use a marshal, and uses flags for decisions.  The announcer enters the changes and the decisions. 

![setup-local-club-minimal.drawio](img/EquipmentSetup/setup-local-club-minimal.drawio-4740776.svg)

**IMPORTANT**: a small router is needed so the computers ot tablets can talk to OWLCMS (even the most basic model will work).  If you have good internet access and don't want to deal with that, you can [run in the cloud](Fly) instead.

The next level up is to have a Marshal. The warmup scoreboard is an "extended" display on the Marshal laptop (all laptops can have a second display).  You can also have the referees use their phone or a tablet.

![](img/EquipmentSetup/setup-local-club.drawio.svg)


- The clock and decisions are visible on the attempt board.  The attempt board should visible from the chalk box and from the center of the platform.
- This uses the "extend desktop" capability of the laptop to have a different output on the monitor. 
- If you have an extra laptop and a projector at your disposal, you can add a scoreboard for the public. 
- The next step up is to have the secretary on a separate laptop so that weigh-in data can be entered while the competition is going on.

## Large Competition Setup

At the opposite end of the spectrum, a setup for a state competition using refereeing devices would provide all the requisite displays and technical official stations.  To keep costs down, TVs and projectors can be driven using less expensive devices such as Raspberry Pi, and you can also use HDMI splitters.

![StateCompetition](img/equipment/StateCompetition.png)

There are several building blocks to such a setup

1. Some devices require frequent user input (Marshal, Secretary, Announcer). Laptops or computers with a keyboard are needed for that.
2. It is common to use a tablet for the timekeeper
3. The various displays and TVs need a signal.  With owlcms, the signal comes from a web browser.  You can use any old laptop to provide the signal, or smaller devices like Raspberry Pi.
4. You can extend the display from laptops. For example, have the marshall work on the laptop screen and provide the warmup room scoreboard on a connected monitor using the "exxtended desktop" feature that all laptops provide.
5. Video splitters.  Sometimes it is possible to share the output from a PC and send it to several TVs.  Scoreboards are often replicated this way.  

The setup then looks like this.

![setup-local](img/EquipmentSetup/setup-local.svg)


## Computer Requirements

- The server software will run either 
  - on any reasonably recent laptop (this laptop will act as a primary server in a local networking setup).  In our experience, an Intel i5 or equivalent is plenty.
  - or on a cloud service.  The the minimum image size required is 512MB, and 1024 is preferred for large competitions.
- As stated above, for the user interface and displays,  It is recommended to use a recent version of **Chrome**, **Edge** or **Firefox** on any laptop/miniPC (Windows, Raspberry, Mac), or on a specialized display device (Amazon FireStick).  **Safari** also works on iPads, but the smaller screen resolution needs to be taken into account.
- Apple iPhones and iPads are ok as [mobile refereeing devices](Refereeing#mobile-device-refereeing).   Display features such as the Scoreboard and the refereeing displays (attempt board, athlete-facing decisions) also work.

## Sound Requirements

By default, only the athlete-facing decision display emits sound.  See this [page](Displays#display-settings) for controlling the sound parameters for the various displays if you need to enable it on another display.  You should normally enable sound only on one display per room, multiple sources are confusing.

If the equipment used for display has no speakers, you can get the main computer to generate the sounds.   See [these explanations.](Preparation#associating-an-audio-output-with-a-platform)

## Internet Access

If available, Internet access is used for streaming and to publish results to the cloud.   There are 3 scenarios

- You are using a local router and the router is connected to the Internet using Ethernet.  There is nothing to do, all should work.
- You are using the facility Wi-Fi for all the computers, so they all have access to the Internet.  Likewise, nothing to do.
- You are using a local router, but there is no Ethernet access to the Internet.  There are 3 options:
  - If there is Wi-Fi at the facility, you can wire your owlcms and OBS computers to the local router and also connect them to the facility's Wi-Fi. There is nothing required for macOS or Raspberry Pi.  For Windows, see [Using Both Ethernet and Wi-Fi](WiFiPlusEthernet)
  - There is no Wi-Fi, but you can use a phone as a hotspot. You would use the same approach as above, see [Using Both Ethernet and Wi-Fi](WiFiPlusEthernet)
  - You can get a device called a Cellular Router.  The competition router connects to the Cellular Router using Ethernet, and the Cellular Router connects to your LTE or 5G network.