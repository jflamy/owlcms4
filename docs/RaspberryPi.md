# Raspberry Pi as Cost-Effective Display Driver

The most cost effective way to run display screens is probably to use Raspbery Pi (RPi) devices.  You can find a full kit for roughly 75$ [(see for example this kit)](https://www.canakit.com/raspberry-pi-3-starter-kit.html).

![rpi](img/equipment/rpi.jpg)

## Hardware Features

Raspberry Pi 3 and 4 have the following features:

1.	Both Ethernet and WiFi
2.	4 USB ports
	-	a USB mouse is used for operation -- normally, clicking on icons and menus is all that is needed.  An on-screen keyboard is available if something needs to be typed (e.g. an address in the browser, or a wifi password)
	-	3 ports are open to connect USB refereeing devices.
3.	HDMI port : you can drive any modern TV or monitor with a HDMI port.  Monitors with DVI-D ports can also be used with the proper cable (HDMI at one end, DVI-D at the other).  Older VGA monitors require an HDMI-to-VGA converter.
4.	Bluetooth: you can use Bluetooth devices built around the [Adafruit EZ-key](https://www.adafruit.com/product/1535) as refereeing devices. 

## Recommended Software

If you use RPi devices to drive displays without keypads then all is good and you can use the current versions of the Raspbian software (named "Stretch" and "Buster").

- The standard version of Chromium that ships with these version works fine.
- Installing the `onboard` virtual keyboard is useful, as it allows operating the raspberry with no physical keyboard attach. `sudo apt-get install onboard`
- You should install   `sudo apt-get install xscreensaver`  After installation. Go to **Menu => Preferences => Screensaver**, to disable screen saving. 
- You can even install TeamViewer to control them remotely.

<u>If you use keypads</u> with your RPi you should be fine *unless* you have older Delcom USB Buttons (see below for discussion and workarounds) . Note that other USB buttons, Bluetooth, NES Classic Keypads, etc) work fine. For example, in the setup above, the keypads are Bluetooth, so there is no issue.

## Workarounds for Raspberry with Legacy Delcom Keypads

If you own older (firmware version 52 and before) [Delcom USB Buttons](Delcom) keypads **and** are connecting them to a Raspberry Pi,  be aware that Delcom has unfortunately [introduced a bug in its device drivers](http://www.delcomproducts.com/webnote.asp?id=3).   See the [Delcom Configuration](Delcom) page for workarounds.