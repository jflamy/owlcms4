# Simultaneous Ethernet and Wi-Fi

There are two scenarios where you might need Simultaneous Ethernet and Wi-Fi

- There is no Internet access at the facility, so you use a local router. You'd like to make the owlcms and streaming laptops talk to a phone hotspot, in addition to the wired competition router.
- You want to use a local router with Ethernet for reliability, but you cannot connect the router to the Internet using a wired connection.  You'd like to make the owlcms and streaming laptops talk to the facility's Wi-Fi, in addition to the wired competition router.

### macOS and Raspberry Pi

There is nothing special to do. Connect your computer to the Wifi provided by your phone or the facility, and connect the Ethernet cable. The computer should use both

### Windows

The following recipe is for Windows 10 and 11. On Windows, if Ethernet is plugged in, the WiFi connection is ignored. The idea is to disable the automatic priority Windows gives to a wired connection and also to give both network adapters the same importance.

1. Follow the steps documented in this article https://www.makeuseof.com/use-wi-fi-ethernet-simultaneously-windows/
2. Make sure you are connected using a wire to your router, and using Wifi to your phone hotspot.  One way to do that is from the control panel, immediately after steps above.

![NJ3gtayHHH](img/HotSpot/NJ3gtayHHH.png)

#### Using Wi-Fi to reach the Internet

Now we have a connection to router that is NOT connected to the Internet, where owlcms and all the displays are connected.  And a Wi-Fi connection to the phone, that IS connected.  We need to tell Windows to use the Wi-Fi exclusively to get to the Internet, and to forget about the router that it would normally use to get there.

> The following steps are not permanent. *They are reset after a reboot,* and need to be done again when you want to use Wi-Fi hotspot for the Internet and exclude the router.

The next steps requires running a command shell in Administrator mode.  Use the Start menu to locate the CMD command and run it as administrator.

![OPEIQnL0mB](img/HotSpot/OPEIQnL0mB.png)

We then issue a `netstat -rn` command to see the different routes that the machine has to reach the internet.  The two network destinations labelled `0.0.0.0` correspond to the Internet at large. We also see that Windows knows that wired is faster than Wi-Fi, so it will try to go through the router (and that won't work because it is not connected to the Internet.)

![CBm1lyp1Zs](img/HotSpot/CBm1lyp1Zs.png)

The gateway that starts with `192.168` is your local router.    This will be the same starting number that owlcms displays when it starts.  We want to remove that gateway because our router is NOT connected to the Internet, our phone is.

So we delete the route with this command -- use your own router address shown under `Gateway`.

```
route delete 0.0.0.0 mask 0.0.0.0 192.168.1.1
```

We can now see that there is only one `0.0.0.0` route, and that it goes through our phone.

![vu4HgfkXnM](img/HotSpot/vu4HgfkXnM.png)

Everything goes back to normal after a reboot.