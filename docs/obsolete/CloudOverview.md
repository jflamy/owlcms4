

In a Cloud-based installation, all that is needed is browsers (and a good internet connection). This is a good option for club competitions since many clubs have wifi.

## Cloud-Based Setup

In this scenario, the owlcms software runs as a web site on the Internet, but it is your own private copy of the application, with your own private database.

When using a cloud-based setup, especially for club meets, it is common to use phones as the refereeing device, since they can connect to the cloud application just like any laptop.  But you can actually referee using hand signals, flags, phones, or dedicated keypads (USB or Bluetooth). See [this page](Refereeing) for the refereeing options.![Slide1](EquipmentSetup/OwlcmsCloud/CloudExplained/Slide1.SVG)All the various display screens and devices connect to the cloud using the facilities network at the competition site.

The preferred cloud deployment solution is hosting on **fly.io**.  Fly.io is available world-wide.  Their plans are structured such owlcms can typically be hosted free of charge (the cost of running a meet is well below the minimum for which they issue a bill).

owlcms has a [web application to automate the installation](https://owlcms-cloud.fly.dev) on the fly.io cloud.  For details, see the [Fly Cloud Installation Instructions](Fly).  Contrary to some other cloud-based options, your data and application are not hosted on the same machine as anyone else, and you can choose what type of server you want for the performance you need.

If you have your own hosting, you can also deploy a docker image using [Docker Instructions](Docker) 

## Public Scoreboard

A complementary module to owlcms allows anyone with Internet access to see the competition scoreboard, live and without delay, including the down signal and decisions. For example, you can connect an on-site major competition with the public scoreboard running in the cloud.
![Slide3](EquipmentSetup/OwlcmsCloud/CloudExplained/Slide3.SVG)See the [Public Scoreboard](PublicResults) page for more information.  Note that this module is normally installed in the cloud and its installation is described in the [Fly Cloud Installation Instructions](Fly).