OWLCMS supports using refereeing devices, and also supports 3 and 5-person [Jury](#Jury). The decisions are displayed to the public and athletes on the displays started on the [Start Displays](Displays) page.

There are several ways to referee using owlcms.  Each is discussed in details in a section of the document.

1. [Manual refereeing](#manual-refereeing), where the referees use flags, cards, or hand signals.  The announcer enters the decisions.
2. [Mobile devices](#mobile-device-refereeing): Using phones, tablets or laptops to referee.  Any device that has a browser can be used to enter decisions and receive notifications.
3. [Button keypads](#button-keypads): These devices  provide real buttons, which many referees prefer over using a phone.  They can be bought, or built from affordable supplies. However they do not provide the feedback when a referee needs to be reminded to enter a decision.
4. [Full feedback keypads](#full-feedback-keypad).  These devices have a LED and buzzer to remind the referee.  This enables compliance with the TCRR requirements.  The devices use the MQTT protocol to communicate, and also be built to be wireless.

## Manual Refereeing

The referees give their decisions using flags/cards/hand signals. The announcer announces the decision and uses the buttons at the top right of his screen to enter the decision.  A single red or white indicator is shown to the public on the current attempt and scoreboard displays.

![020_Announcer](img/Refereeing/020_Announcer.png)



## Mobile Device Refereeing

 In this setup, each referee uses a phone, tablet or laptop.  

![mobile_ref](img\equipment\mobile_ref.png)



This uses the phone or tablet web browser, which connects to owlcms. It does not matter whether owlcms is running on a laptop or in the cloud.  This setup is therefore useful when running virtual competitions.
![Slide9](img/PublicResults/CloudExplained/Slide9.SVG)

The refereeing screen is started from the "Run a lifting group" page, in the Referees and Jury section at the bottom.  The sequence would look like the following when done on a phone.

![left](img/Refereeing/01_runLiftingGroup.png ':size=350')  ![left](img/Refereeing/02_startReferee.png ':size=350')

The refereeing screen can also be started directly from the phone/tablet by using a browser and adding `/ref` at the end URL for the competition site.   After starting the referee screen, select which referee is which (1, 2 or 3) using the numeric input at the top (use the + and - signs)

Feedback for entering a decision is given by greying out the other key.  The refereeing display is reset when an athlete gets fresh clock.

When the other two referees have entered a decision, a yellow bar with a blinking reminder is shown, as well as an audible beep.  This is only a reminder, and the referee can wait as long as needed if they are not satisfied that the athlete has completed the lift.  The reminder goes away when the decision is entered.

When the jury wants to call the official, a red bar with blinking text is shown.  The call goes away when the competition is resumed.

## Button Keypads

Many referees prefer having their finger rest on a button (which is not possible on a phone).  In this section, we describe how to create a physical refereeing device. 

In this approach, keypads are connected to the laptop or mini PC running a display that shows the countdown timer.  Normally, this is the Athlete-facing display, but you can also use the Attempt Board, or even the Scoreboards.  The sound and down arrow are emitted directly by the browser, without any round-trip to the primary computer.  This reduces delays and increases reliability if the networking is fragile (which is sometimes the case in gyms)

![Slide6](img/PublicResults/CloudExplained/Slide6.SVG)

There are three ways to create such a keypad

1. USB keypads: they emulate a USB keyboard.
2. Joystick keypads: they emulate a joystick, and a small piece of software converts the joystick button press to a keyboard press.
3. Bluetooth keypads: they emulate a Bluetooth keyboard and work with most devices such as iPads and Windows laptops.

#### **Example of USB keypads**

USB Keypads can be bought from industrial device providers, such as [Delcom USB keypads](http://www.delcomproducts.com/productdetails.asp?PartNumber=706502-5M). Specific notes for Delcom keypads can be found [here](Delcom) (older keypads don't work with Linux computers such as Raspberry Pis)

You can also build you own: 

- You can use an Arduino to emulate a USB keyboard:  [Keyboard - Arduino Reference](https://www.arduino.cc/reference/en/language/functions/usb/keyboard/)
- Or you can use devices that emulate a USB keyboard: [Simple USB Buttons Using an Adafruit Trinket M0 - Hackster.io](https://www.hackster.io/laurentslab/simple-usb-buttons-using-an-adafruit-trinket-m0-5ad900#toc-programming-3)

![refereeingSetup](img\equipment\refereeingSetup.jpg)

#### Example of Joystick buttons

In this configuration, a joystick-to-USB converter is used.  This is a do-it-yourself project, but most of the kits require no soldering. Search for `Joystick Zero Delay USB Encoder`, and you will get several [examples](https://www.amazon.ca/EG-Starts-Encoder-Controller-Joystick/dp/B06XVXCJBD)

The refereeing computer sees the device as a Joystick.  To convert the button presses to k[eyboard presses expected by owlcms](#keypad-configuration), you need a small program like [joy2key](https://joytokey.net/en/) or [antimicro](https://sourceforge.net/projects/antimicro.mirror/) on Windows, or `qjoypad` on Linux/Raspberry (use `apt-get install qjoypad`).



 <img src="img/Refereeing/02Joystick.jpg" alt="Wi" style="zoom: 25%;" />



#### Example of Bluetooth buttons

You can use Bluetooth buttons to control an iPad or a Windows laptop.  You can find Bluetooth buttons from Home Automation providers, such as Flic.  Illustrated below, [Flic2](https://flic.io/) buttons can act as stand-alone devices using their [Universal mode](https://flic.io/flic-universal) (you only need the buttons, not the hub)

You can also build your own Bluetooth buttons, 

- You can program an Arduino-like device, the ESP32, to [act as a Bluetooth keyboard](https://gist.github.com/manuelbl/66f059effc8a7be148adb1f104666467).  
- You can also find devices that do the Bluetooth emulation. See  [these instructions](https://learn.adafruit.com/introducing-the-adafruit-bluefruit-spi-breakout/hidkeyboard) as a starting point.

![030_iPad_Flic](img/Refereeing/030_iPad_Flic.jpg)

#### Keypad Configuration

The keys or buttons on the keypads are programmed to send key sequences.  The decision display is waiting for these keypresses.  You must click in the black area of the screen to make sure that the keypresses are seen by the browser.

OWLCMS interprets Even digits as red, and Odd digits as white.  The same devices can be used for referees and for the jury. 

| Referee# | Good | Bad  |
| -------- | ---- | ---- |
| 1        | 1    | 2    |
| 2        | 3    | 4    |
| 3        | 5    | 6    |
|          | 7    | 8    |
|          | 9    | 0    |

Note that the shortcut keys are as defined according to [a standard](https://www.w3.org/TR/uievents-code/#key-alphanumeric-writing-system)

- For most countries, hitting the key "Digit1" sends a 1.  But there are exceptions. For example, in France, hitting Digit1 will actually send a "&" and depending on the software you may actually need to use "&" instead of "1".  Fortunately, most national keyboards send the digits directly.

## Full-feedback Keypad

In order to provide referees with a reminder to enter a decision, or to signal that they need to go to the jury table, it is necessary for owlcms to be able to communicate back with the devices. 

The  most flexible way to accomplish this is to use a technique used for home automation and other internet-connected devices.  The MQTT protocol supports such usage, and there are now inexpensive devices that support it over WiFi.  For example, the ESP32 chip is available for roughly 10US$.

There are two ways this can be done:

- One device per referee.  No network wiring is needed. The only thing needed is electricity (which could be either a battery or a microUSB connection). Each device interacts with owlcms on its own.
- One device for all referees.  Buttons, LED and buzzer are wired to a central box using twisted pair cable (standard Ethernet wires).  The referee devices get power from the central device, and only the central device needs power.  Building this is a little bit more complicated (more connectors, more soldering), but the fact that there is no need to bring power to the referee tables is a strong positive.

Software and schematics for a device that can do both can be found at https://github.com/jflamy/owlcms-esp32.  For the one-device-per-referee variation, only the orange wires are used.  Full information on how to configure owlcms for using such devices can be found on the [MQTT](MQTT) page on this site.

![design](https://camo.githubusercontent.com/c0d799a3bd35c47d4c4aa1d7caa508f32866820ca9ca7e24b20510b0ab27dbd1/68747470733a2f2f776f6b77692e636f6d2f63646e2d6367692f696d6167652f77696474683d313932302f68747470733a2f2f7468756d62732e776f6b77692e636f6d2f70726f6a656374732f3332323533343534333030383436353439312f7468756d626e61696c2e6a7067)