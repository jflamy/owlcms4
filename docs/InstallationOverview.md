Several installation options are possible, depending on what is available at the competition site and the size of the competition.  The four scenarios below fall in two categories:

- Cloud-based options, where all you need are browsers (and a good internet connection)
- Stand-alone options, where there is no reliable internet and you need to run things locally.

## Easiest: Cloud-Based Installation (Internet required)

If there is good internet communication at the competition site, there is no need to install anything locally. 

- There is a one-click install procedure to a *free* (0$) cloud service called Heroku (a division of Salesforce.com).  For large competitions, you can change the configuration on the competition days to use the paying tiers, at a cost of about 12$ per day, and then go back to the free tiers.
- The install will create your own private copy of the application, with your own database.
- The owlcms software runs as a web site. All the various display screens and devices connect to the cloud using the competition site's wifi or ethernet network.

![Slide9](img/PublicResults/CloudExplained/Slide9.SVG)

See the following for instructions

  * [Heroku Cloud Installation Instructions](Heroku)
  * [Large Competition Heroku Setup](LargeHeroku)

## Cloud-Based Virtual Competitions

In a virtual competition, the officials are in multiple locations.  In order to allow access by all officials, `owlcms` is run in the cloud and supports remote refereeing -- see the following [page](Refereeing#Mobile-Device-Refereeing) for details. Remote referees need to use a laptop because a proper screen is needed for refereeing but unfortunately iPads do not support simultaneous use of video and of the refereeing application.

![Slide5](img/PublicResults/CloudExplained/Slide5.SVG)

The following pages will guide you through setting up a virtual competition

1. [Setup Heroku for a Virtual Competition](LargeHeroku)
2. [Preparing the Zoom Setup](PrepareZoomBroacasting)
3. [Preparing the Video Broadcasting Setup](OBS)
4. [Live Streaming Events](Streaming)
5. [Optional Modified Competition Rules](ModifiedRules)

If you wish to control the full setup and are technology-savvy, alternatives to using Heroku are possible, for example, [Kubernetes on Digital Ocean Cloud](DigitalOcean) or [Home Kubernetes Hosting with Secure Internet Access](k3d)

## Stand-alone: Laptop installation

If there is no Internet connectivity at your competition site, or if you can't trust it, you can use a stand-alone setup and run the software on a laptop.

- The software runs on a laptop (labeled owlcms in the diagram). 

- All the other screens and officials connect to that laptop using only a web browser, so you can use whatever you want (old laptops, chromebooks, tablets, firetv sticks, etc.).  All the communications take place over a local network (wifi or ethernet).

- You can run owlcms on the same machine as one of the officials.  It is often the case that owlcms runs on the same machine as the announcer or the competition secretary.  In the simplest setups, there is just the announcer, and maybe one scoreboard.

- You can referee using hand signals, flags, phones, or dedicated devices. See [this page](Refereeing)

  ![Slide1](img/PublicResults/CloudExplained/Slide7.SVG)

See the following instructions

  * [Windows Stand-alone Installation](LocalWindowsSetup)
  * [Linux or Mac Stand-alone Installation](LocalLinuxMacSetup)

## Stand-Alone: Local Individual Scoreboards

It may be desirable to provide coaches with individual access to the scoreboard in order to respect distancing guidelines, or to give access to the members of the audience. This can be done by connecting to a [publicresults installed in the cloud](), or by creating a second local network to isolate the main competition network.  The fully local setup is explained in [this page](PublicResults_Local).

![Slide1](img/PublicResults/LocalPublicResults/Slide1.SVG)

