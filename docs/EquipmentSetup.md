## Physical Setup

Each screen or display is attached to a laptop or miniPC (Windows, Linux, ChromeOS).  A setup for a regional competition without a jury might look as follows.  If [running on the cloud](Heroku.md), the router would be connected to the Internet, and the owlcms software is accessed remotely.  If [running locally](LocalSetup.md), the software runs on the master laptop, which is typically that used by the competition secretary.

![StateCompetition](img/equipment/StateCompetition.svg)

## Hardware Requirements

- The server software will run on any recent laptop acting as a server (or on a cloud) with Java8 installed, or on a cloud service. We test and support the Heroku cloud service, which has a free tier suitable for owlcms.

- For the user interface and displays, 

  - It is recommended to use a recent version of Chrome or Firefox on a laptop (Windows, Mac, Linux, ChromeOS)

  - Apple iPads are only supported as [refereeing devices](Refereeing#mobile-refereeing-devices). Apple Safari does not comply with standards and unless someone donates a Mac development laptop, I have no intention of investigating further why what works everywhere else doesn't on the everlagging Safari.
  - You can use any laptop or miniPC (such as a [Raspberry Pi](https://www.raspberrypi.org/products/raspberry-pi-3-model-b/)) running Windows, Linux or ChromeOS. Be aware however that Raspberry Pi require a specific version of Linux to work with certain refereeing devices (Delcom USB keypads).

## Pre-requisite for sound : Chrome configuration

Recent versions of Chrome no longer allow web pages to emit sounds by themselves.  In order to hear the signals from the clock and the down signal, we have to override this setting.

- in Chrome, go to page ``chrome://flags``  and search for ``autoplay policy``  in the search bar.
  Set the parameter to ``No user gesture is required``


OR

- Create a shortcut to chrome and add the following flag
  ```bash
   --autoplay-policy=no-user-gesture-required
  ```
  The path in the shortcut would look like this
  ```bash
  "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" --autoplay-policy=no-user-gesture-required
  
  ```

