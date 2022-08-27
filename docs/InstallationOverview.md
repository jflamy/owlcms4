Several installation options are possible, depending on what is available at the competition site and the size of the competition.  The scenarios below fall in two categories:

- Cloud-based options, where all you need are browsers (and a good internet connection).  Such setups are ideal for virtual competitions.
- Stand-alone installation, if you prefer to run things locally

## Stand-alone: Laptop installation

In a local stand-alone setup, there is no reliance on a trustworthy internet connection, but owlcms needs to be installed on a reasonably recent laptop with adequate performance.

- The OWLCMS software runs on a laptop (labeled owlcms in the diagram) which acts as a web server to the other screens and displays.

- The primary laptop and all the other screens and official stations are connected to a wi-fi network or physically cabled to a router.

- All the displays are driven by a web browser - there are web browsers on all the computers and phones and tablets.

- You can run owlcms on the same machine as one of the officials.  It is often the case that owlcms runs on the same machine as the announcer or the competition secretary.

- In the following drawing phones are used as the referee device.  But you can actually referee using hand signals, flags, phones, or dedicated keypads (USB or Bluetooth). See [this page](Refereeing)

  ![Slide1](img/PublicResults/CloudExplained/Slide7.SVG)

See the following instructions

  * [Windows Stand-alone Installation](LocalWindowsSetup)
  * [Linux or Mac Stand-alone Installation](LocalLinuxMacSetup)



## Cloud-Based Installation

In this scenario, the owlcms software runs as a web site on the Internet, but it is your own private copy, with your own database.  All that is needed is a good internet connection from the competition site.

- We provide instructions for two inexpensive cloud services
  - For Heroku, there is an extremely simple one-click install. See [Heroku Cloud Installation Instructions](Heroku).  Current pricing is about 0.50 US$ per day when the application is active (when preparing and running a competition, the billing can be turned off the rest of the time.)
  - A second cloud offering, fly.io, is also supported.  It is actually cheaper at 0.10 US$ per day when the application is active (the application can be turned off when not needed). The only minor drawback is that the initial installation requires typing a few commands. See the [Fly Cloud Installation Instructions](Fly).
- All the various display screens and devices connect to the cloud using the competition site's network.
- In the following diagram phones are shown as the referee device.  But you can actually referee using hand signals, flags, phones, or dedicated keypads (USB or Bluetooth). See [this page](Refereeing)

![Slide9](img/PublicResults/CloudExplained/Slide9.SVG)

