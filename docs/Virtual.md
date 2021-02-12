This document explains the concepts behind running a virtual competition with owlcms. A *virtual competition* is a competition where the athletes and officials are in multiple locations and communicate via a videoconferencing application such as Zoom or Jitsi / 8x8.  We recommend Zoom or Jitsi / 8x8 because the video moderator can select what is seen by all attendees (putting the spotlight on the current athlete, in this case)

## Remote Scoreboards Using the publicresults Cloud-Based Application

In order to hold a virtual competition, even if there are only a few remote lifters, we must allow them to see the scoreboard, timer, down and decisions.  

There is a sister application to owlcms called *publicresults*.  Publicresults is installed in the cloud, and all the remote athletes can access it (or anyone in the public, actually).  Publicresults gets updates from owlcms.  No one other than the officials gets access to owlcms, to reduce the security risks.

![](img/PublicResults/CloudExplained/Slide2.SVG)

The details of using `publicresults` and `owlcms` in this fashion are explained on the [Cloud Installation of the publicresults Application](Remote) page.

## Remote Officials Using Cloud-Based owlcms 

In a true virtual competition, the officials are in multiple locations.  In order to allow access by all officials, `owlcms` is run in the cloud (access is protected using a password).   owlcms already supports remote refereeing -- see the following [page](Refereeing#Mobile-Device-Refereeing) for details 

Remote referees should use a laptop because video rates on most iPads is not very good and a larger screen than a phone is needed.

![Slide5](img/PublicResults/CloudExplained/Slide5.SVG)



## Setting up a Virtual Competition

The following steps are suggested

1. Use one of the following three options to install owlcms and publicresults in the cloud
   1. [Option 1: Heroku Cloud Hosting](InstallationOverview#option-1-heroku-cloud-hosting)
   2. [Option 2: Kubernetes Cloud Hosting](InstallationOverview#option-2-kubernetes-cloud-hosting)
   3. [Option 3: Home Hosting with Cloud Access](InstallationOverview#option-3-home-hosting-with-cloud-access)
2. Test your setup
3. As a backup, you way want to set up your video conference to also [broadast the scoreboard](Video)