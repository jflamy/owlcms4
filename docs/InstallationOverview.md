Several installation options are possible, depending on what is available at the competition site and the size of the competition.

- Stand-alone installation, if you prefer to run things locally ([details below](#stand-alone-laptop-installation))
  - [Windows Stand-alone Installation](LocalWindowsSetup)
  - [Linux or Mac Stand-alone Installation](LocalLinuxMacSetup)


- Cloud-based options, where all you need are browsers (and a good internet connection).  Such setups are ideal for virtual competitions.

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

- We provide instructions for the following variations
  - **Fly.io**: Fly.io is essentially free because its cost is about 10 cents per day and there is no billing for less than 5$ usage per month. Both the database and publicresults are covered in their default "free" package. Only the owlcms memory requirements exceed the threshold and incurs the small charge. The application can be turned off when not needed and not billed at all. The only minor drawback is that the initial installation requires typing a few commands. See the [Fly Cloud Installation Instructions](Fly).  
  - **Heroku**: For Heroku, there is an extremely simple one-click install. See [Heroku Cloud Installation Instructions](Heroku).  Current pricing is 5$ per month for all the applications you can use in "Economy mode", but there is an extra 5$ per month for *each* database (each owlcms application needs one).  Hosting one owlcms and one publicresults costs 10$ per month because only owlcms needs a database.
  - **Docker**: if you have your own hosting, you can deploy a docker image using [Docker Instructions](Docker) 
  - **Kubernetes**: see [self-hosted Kubernetes instructions](DigitalOcean)
- All the various display screens and devices connect to the cloud using the competition site's network.
- In the following diagram phones are shown as the referee device.  But you can actually referee using hand signals, flags, phones, or dedicated keypads (USB or Bluetooth). See [this page](Refereeing)

![Slide9](img/PublicResults/CloudExplained/Slide9.SVG)

## Public Scoreboard

A complementary module to owlcms allows anyone with Internet access to see the competition scoreboard.  This means that anyone in the audience with a phone can follow the scoreboard on site.  People watching live streaming or participating in a virtual competition can also see the scoreboard (including the countdown and decisions). And finally, coaches in the warmup room can be given a Wifi connection and watch the scoreboard on a tablet.

See the [Public Scoreboard](PublicResults) page for more information.  Note that this module is normally installed in the cloud and its installation is covered in the [Heroku Cloud Installation Instructions](Heroku) and [Fly Cloud Installation Instructions](Fly).