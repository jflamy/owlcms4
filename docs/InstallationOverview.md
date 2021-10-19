Several installation options are possible, depending on what is available at the competition site and the size of the competition.  The scenarios below fall in two categories:

- Cloud-based options, where all you need are browsers (and a good internet connection)
- Stand-alone installation, if you need to run things locally (typically because there is no good internet access)

## Easiest: Cloud-Based Installation

If there is good internet communication at the competition site, the process is extremely simple. 

- There is a one-click install procedure to a *free* (0$) cloud service called Heroku (a division of Salesforce.com). 
- The install will create your *own private copy* of the application, with your *own database*.
- The owlcms software runs as a web site. All the various display screens and devices connect to the cloud using the competition site's network.
- In the following diagram phones are shown as the referee device.  But you can actually referee using hand signals, flags, phones, or dedicated keypads (USB or Bluetooth). See [this page](Refereeing)

![Slide9](img/PublicResults/CloudExplained/Slide9.SVG)

See the following for instructions

  * [Heroku Cloud Installation Instructions](Heroku)

For large competitions, you can change the configuration on the competition days to use a paying tier, at a cost of about 5$ per day, and once done go back to the 0$ free tier.

  * [Large Competition Heroku Setup](HerokuLarge)
  * [Virtual Competition Setup](VirtualOverview)

## Stand-alone: Laptop installation

If there is no good Internet connectivity at your competition site you can use a stand-alone setup and run the software on a laptop.  In that setup: 

- The OWLCMS software runs on a laptop (labeled owlcms in the diagram) which acts as a web server to the other screens and displays.

- The primary laptop and all the other screens and official stations are connected to a wi-fi network.  If there is none in the building, you will need to configure a local router and connect all machines to that router (exactly like a home network).

- All machines need a web browser to drive their display or screen.

- You can run owlcms on the same machine as one of the officials.  It is often the case that owlcms runs on the same machine as the announcer or the competition secretary.

- In the following drawing phones are shown as the referee device.  But you can actually referee using hand signals, flags, phones, or dedicated keypads (USB or Bluetooth). See [this page](Refereeing)

  ![Slide1](img/PublicResults/CloudExplained/Slide7.SVG)

See the following instructions

  * [Windows Stand-alone Installation](LocalWindowsSetup)
  * [Linux or Mac Stand-alone Installation](LocalLinuxMacSetup)

If you run a local setup, you may still want to have individual scoreboards so distancing guidelines are followed, or provide to provide access to people in the attendance.

- [Local networking setup for individual scoreboards](PublicResults_Local).

