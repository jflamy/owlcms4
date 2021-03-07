Several installation options are possible, depending on what is available at the competition site

## Easiest: Cloud-Based Installation (Internet required)

If there is good internet communication at the competition site, there is no need to install anything locally. 

- There is a one-click install procedure to a free (0$) cloud service called Heroku (a division of Salesforce.com). 
- The install will create your own private copy of the application, with your own database.
- The owlcms software runs as a web site. All the various display screens and devices connect to the cloud using the competition site's wifi or ethernet network.

![Slide9](img/PublicResults/CloudExplained/Slide9.SVG)

See the following for instructions

  * [Heroku Cloud Installation Instructions](Heroku)

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

## Advanced: Remote Scoreboards for Virtual or Distanced Competitions

There are additional modules available to support more involved scenarios.  For example, it is possible to have individual scoreboards available to every coach -- supporting physical distancing.  This can also be used to provide individual scoreboards to every member of the public watching the competition remotely.

A competition with a main site and remote gyms connected by videoconferencing could look as follows

![Slide3](img/PublicResults/CloudExplained/Slide3.SVG)

The following pages describe these options and others.

*	[Distancing using Individual Scoreboards](Distancing)
*	[Virtual Competitions and Remote Referees](Virtual)
*	[Video-Conference Setup](Video)

## Option 1: Heroku Cloud Hosting

In order to host a virtual competition effectively, you can install owlcms and publicresults both as separate free applications on Heroku, and make them talk to each other.  This is described on the following page

- [Free Heroku hosting of a virtual competition](Heroku)

## Option 2: Cloud Kubernetes Hosting

For larger competitions, or for a more polished experience, you can use a hosted setup that can scale up.

Even though the following uses a paid setup, you can still run a competition for less than 10$. You also get the added benefit that if your run a larger competition, you can increase the memory available.  If you want to run a large national competition with remote gyms, this is probably the better option.

- [Kubernetes on Digital Ocean Cloud](DigitalOcean)

## Option 3: Home Kubernetes Hosting with Cloud Access

If you have good internet connectivity at home, and a good performance Windows, Linux or Mac server, you can use the following approach

- [Home Hosting with Secure Internet Access](k3d)

If you have a Linux Host at home, you can use the same approach. Read above for the Cloud Hosting instructions, which you can apply at home, and use KubeSail.com to grant access.