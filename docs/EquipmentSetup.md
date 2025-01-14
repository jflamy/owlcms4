# Physical Setup

owlcms is a web-based application.  All the displays connect to owlcms using a browser.  In most scenarios, owlcms runs on a good laptop at the competition site, and all the other displays connect to that laptop.  For virtual meets, the computer running owlcms is actually running somewhere in the internet -- everything else on this page still applies.

## Minimalistic Setup

In a small club meet, the announcer will often do everything - announce, enter the weight changes, and operate the clock. This can be done on a single laptop, as demonstrated in this [video](Demo1).  You can sometimes even do away with the scoreboard.

<center><img src="EquipmentSetup/images/no_wifi.png" alt="ClubCompetitionWIFI" /></center>

## Suggested Small Competition Setup

If you have a meeting with multiple groups, the following setup will allow you to comply with most requirements.  This adds a computer in the warmup area, and there is an attempt board on the platform.

<table>
<tr><td><img src='img/Gallery/ElSalvador.jpg'></img></td><td><img src='img/Gallery/ElSalvador_marshall.jpg'></img></td></tr></table>
Many gyms have a WiFi router, in which case you can simply connect to it (diagram on the left) If there is no Wifi at the location, you will need to bring your own router. Note that we recommend that you use an ethernet wired connection whenever possible, at least for the owlcms computer (diagram on the right)

<table>
<tr><td align=center ><img src='EquipmentSetup/images/wifi_minimal.png'></img></td><td align=center><img src='EquipmentSetup/images/minimal.png'></img></td></tr></table>

- The clock and decisions are visible on the attempt board.  The attempt board should visible from the chalk box and from the center of the platform.
- This uses the "extend desktop" capability of the laptop to have a different output on the monitor. 
- If you have an extra laptop and a projector at your disposal, you can add a scoreboard for the public. 
- The next step up is to have the secretary on a separate laptop so that weigh-in data can be entered while the competition is going on.

## Large Competition Setup

At the opposite end of the spectrum, a setup for a state competition using refereeing devices would provide all the requisite displays and technical official stations.  To keep costs down, TVs and projectors can be driven using less expensive devices such as Raspberry Pi, and you can also use HDMI splitters.



![StateCompetition](img/equipment/StateCompetition.png)



There are three building blocks to such a setup

1. Some devices require frequent user input (Marshal, Secretary, Announcer). These are handled by laptops.  owlcms is often run on the secretary computer, only that laptop needs to be recent and performant.  All the other laptops can be basic, or refurbished.
2. The various displays and TVs need a signal.  With owlcms, the signal comes from a web browser.  The most flexible way to do this is to use any of the following
   - Old laptops or Chromebooks that can run Chrome or Firefox
   - Raspberry Pi (the [model 500](https://www.raspberrypi.org/products/raspberry-pi-500/), has everything built-in and is an excellent choice.  They have two HDMI ports and so fewer devices are needed (you can buy long optical fiber HDMI cables of more than 30m if you need to)
   - Mini PCs (preferably with an Ethernet port). Mini PCs also have multiple HDMI ports, so you need fewer devices.  The basic models with 4GB or 8GB of memory are often quite cheap.
   - Chromecasts (this requires internet access for setup, and Wi-Fi has to be of excellent quality). A computer must provide the display being replicated -- a single laptop can drive multiple displays)
3. Video splitters.  Sometimes it is possible to share the output from a PC and send it to a TV.  For example, the scoreboard in the warmup room can be obtained from the marshal computer and shown on a marshal monitor and a warmup room TV.

### Large Competition: Networking for Maximum Reliability

In this setup, all the devices are wired using Ethernet, and the network is private.  The competition can go on if the facility's network is down or if there is no Internet.  

In this approach, a networking switch is used for each platform.  A networking switch is like an extension for the router and allows more wired ports. The internet access aspect is discussed [further down on this page](#internet-access) .



![Slide1](EquipmentSetup/Networking/Equipment/Slide1.SVG)



### Large Competition: Hybrid Approach with Wi-Fi

Using Wi-Fi simplifies the setup, but in large venues there are sometimes intermittent (or persistent issues) that don't affect casual browsing, but would interfere with time-sensitive displays.  So for a large competition, we **strongly recommend** to wire owlcms itself and the computer that shows the countdown clock and emits the down signal. 

This yields an alternate setup where a portion of the competition network is WiFi and a portion is wired. The diagram also shows a different way to reach the Internet, discussed [further down on this page](#internet-access) 



![Slide2](EquipmentSetup/Networking/Equipment/Slide2.SVG)

## Computer Requirements

- The server software will run either 
  - on any reasonably recent laptop (this laptop will act as a primary server in a local networking setup, see [below](#local-access-over-a-local-network) for details.  In our experience, a Core i5 or equivalent is plenty.
  - or on a cloud service.  The the minimum image size required is 512MB, and 1024 is preferred for large competitions.
- As stated above, for the user interface and displays,  It is recommended to use a recent version of **Chrome**, **Edge** or **Firefox** on any laptop/miniPC (Windows, Raspberry, Mac), or on a specialized display device (Amazon FireStick).  **Safari** also works on iPads, but the smaller screen resolution needs to be taken into account.
- Apple iPhones and iPads are ok as [mobile refereeing devices](Refereeing#mobile-device-refereeing).   Display features such as the Scoreboard and the refereeing displays (attempt board, athlete-facing decisions) also work.

## Sound Requirements

By default, only the athlete-facing decision display emits sound.  See this [page](Displays#display-settings) for controlling the sound parameters for the various displays if you need to enable it on another display.  You should normally enable sound only on one display per room, multiple sources are confusing.

If the equipment used for display has no speakers, you can get the main computer to generate the sounds.   See [these explanations.](Preparation#associating-an-audio-output-with-a-platform)

## Internet Access

When available, Internet access is used for two reasons

1. On the video streaming computers, to send video to YouTube or Facebook or another streaming service
2. On the owlcms computer send the competition results to the publicresults module of owlcms running in the cloud.  This is increasingly desirable due to the cost and difficulty of setting up a large scoreboard in the main venue.

What complicates matters is that these computers also need to talk to the rest of the competition network, in addition to the Internet.

There are four ways to solve the problem.

1. If the facility can offer Ethernet access to their network, that is the preferred option.  Simply connect the competition router to the facility's network.

2. If the facility has excellent WiFi, you can take the risk of running everything on the facility WiFi.  Large facilities often have several WiFi networks. You should not use the WiFi used by the crowd, use a separate one if available.

3. You can connect the competition router to a cellular network hotspot

   - Some routers (for example the ASUS RT-AX58U or RT-AX68U) have a USB port and you use a cellular phone to get Internet Access (just like sharing a connection)

   - You can buy or rent a cellular router that has a SIM card.  You connect the competition router to that router, and get Internet access that way.  In the picture, the competition router is in the center.  It is plugged into the cellular router at the left to get Internet access.  The box on the right is a switch that adds additional ports to the router.

![hotspot](EquipmentSetup/Networking/hotspot.png)

4. You can connect OBS and owlcms to the router with a wire, and use the facility WiFi or a phone hotspot to get to the Internet.  This is the approach illustrated in the second diagram -- OBS and owlcms communicate to the Internet on their own, independently.   The configuration required is explained on [this page](PhoneHotSpot.md)