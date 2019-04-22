# Frequently Asked Questions (FAQ)

###### On a phone or tablet, how do I get to the referee screens shown in the screenshots?
* If the main laptop is running on (say) machine 192.168.0.100, then the URL to access the mobile device screens is http://192.168.0.100/owlcms/m/#mobileHome
You will then be able to select which screen you want by pressing on a button.

###### How do I know what address to use to connect to the main laptop?
* The main laptop address is normally shown when selecting the "About" item in the menu bar.
* If not, there is an application shipped with the Windows version of owlcms called HoverIP. We suggest that the main laptop be wired to its router, and if this is the case you should be looking for the ethernet (wired) connection address as opposed to the wireless address.
* You can always resort to the command line ipconfig command for Windows, or "ip a" for Linux to get the address.

###### How can I get more than one working owlcms screen on a laptop?
* When you create a new tab or open a new window, you will get an "Out of Sync" error.  This is because by default the browser reuses the same HTTP session.  The sophisticated updates owlcms performs on the screens require advanced synchronization features that require separate sessions.
* The simplest workaround is to run one screen in (say) Chrome and the second one in Firefox.  Different browsers do not share sessions.
* The second simplest workaround is to use the "New Session" menu item from the Internet Explorer "File" Menu Bar.  By default the Menu Bar is hidden, and you need to right-click in the top area of the browser (next to the tabs) to enable it.
* The cleanest workaround is to use Chrome, and have it create several "Users", one for each role for which you need a screen.  In Chrome, these fake users have independent configurations, and can each have their own home page and bookmarks. So you can create a "Announcer" user with the announcer page as home page, and so on.  In order to create users, go to the "Settings" menu at the right hand side of the menu bar, and use the "Users" section -- 5th block down the page.


###### How do I select the language for the application?
* By default, owlcms uses the language preferences from the internet browser. In this way, you can have different screens display in different languages by setting the browser preferences.
* If you want all browsers to display the same language, the recipe varies depending on how you are running owlcms
    * If you are running the Windows version under Windows 7, set the OWLCMS_LOCALE environment variable to the value __en__ as explained in this [example](http://viralpatel.net/blogs/windows-7-set-environment-variable-without-admin-access/).  If you are not running an English version of Windows you can find the user account settings via the Control Panel. You only need to do this once.
    * If you are running the Windows version under Windows 8, set the OWLCMS_LOCALE environment variable to the value __en__ as explained in this [link](http://www.itechtics.com/customize-windows-environment-variables/). You only need to do this once.
    * If you are running the War file under Tomcat, find the catalina.properties file in the conf directory and set add a line with
<code>owlcms.locale=en</code>
at the end of the file. You only need to do this once.

###### Does the application run on a Mac?
* It will run in a Windows environment under Parallels Desktop, VMWare or VirtualBox (because you are actually running Windows)
* If running Windows is out of the question, then the __.war__ Web Archive should run provided you have installed Java and Tomcat as explained in the [Mac Installation](Setup.md#Mac_Installation) section of the [Setting](Setup.md) page. 

###### What is the recommended way to run the application for a large competition?
* Running the application under a standalone Tomcat web application server is the recommended setup.  This provides the ability to configure the amount of memory used by the server and also keeps a copy of the various log files for analysis (all user interface event and any technical error that might happen are written to the log files).  See this [link](http://tomcat.apache.org/tomcat-7.0-doc/deployer-howto.html) for details.

###### What are the limitations or differences between OWLCMS and the official rules
* If you are using USB or Bluetooth devices, there is no reminder to the referee to enter a decision. Editorial: if there is noise, or the referee is distracted, the reminder is likely to get ignored anyway. If there has been no down signal, the referee actually learns that the other two referees are split in their opinion, which gives him unwarranted information.
* If you are using phones or tablets to referee, you get a visual (not audible) cue to enter decision
* owlcms is currently limited to 3 person juries
* owlcms scoreboard shows the ranking within the current group, not overall (lifters in earlier group are not taken into account).  The protocol sheet for the full competition, and the competition book show the actual overall ranking.
* owlcms only shows the current lifter on the scoreboard, not the next lifter as recommended.  This is available on another screen which is typically shown along the scoreboad in the warm-up room (the lifting order screen).