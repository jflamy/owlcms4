# Running stand-alone

In a stand-alone setting, you do not need an Internet connection.  Everything runs locally at the competition site.  One laptop is used to run the software and acts as a *master*.  All the other screens connect to the master using a Web browser.

See the [Equipment Setup](EquipmentSetup.md) page for details on how the various computers are setup, from the simplest case of a  club meet to the full setup for national championships.

## Pre-requisites for the master laptop

The master laptop requires Java 8 to be installed. We recommend using the installer from https://adoptopenjdk.net/ 

- For Windows, click on the link above, and the correct installer for your version will be proposed. When installing, you will see a screen with options, we recommend you enable the options for JavaSoft registry variables

  ![](img\AdoptOpenJDK_Javasoft.png)

- On MacOS or Linux, get the installer from the link above, and use the default options.

## Installing the owlcms server software on the master laptop

Get the current zip file from <https://github.com/jflamy/owlcms4/releases>

- On Windows,
  - Double-click on the zip file, and double-click on `owlcms.exe` .
  - You will be prompted to extract the files to a directory.  Select a directory and perform the extraction.
- On MacOS or Linux
  - Double-click on the zip file, and extract the files to a directory
- Notes:
  - The database will be created in a directory named `database` 
  - Log files will be created in a directory called `logs` . If you report bugs, you will be asked to send a copy of the files found in that directory (and possibly a copy of the files in the database folder as well).

## Starting the owlcms4 server software

### Windows - Simple Version

Running owlcms.exe from the extracted location will start the server program in competition mode (see [Testing](#Testing) below if you want to run in demo mode with fake data).  

### Windows - Advanced Version

The second method is to right-click on the Windows icon at the bottom left and select the option to start a command prompt.  Change directory to where the program was extracted.  Use the following commands (substitute the proper numbers for x.y ) to run the program

```bash
cd ***to the directory where you extracted owlcms***
java -jar owlcms-4.X.Y.jar
```

### MacOS or Linux

On MacOS or Linux, use the following commands (substitute the proper numbers for x.y ) to run the program

```bash
java -jar owlcms-4.x.y.jar
```

### Testing

On all platforms, if you just want to run tests with fake athletes, without messing with your database, you can at any time use the command

```bash
java -DdemoMode=true -jar owlcms-4.x.y.jar
```

## Securing a local installation

Mischievous users may know or discover your Wifi router password, and could then get in th application.  In order to require a PIN when running the program on a computer at the local competition site), start the program as follows

- Under Windows 10, right click on the Windows icon at the bottom left and select the option to start a command prompt.  Change directory to where the program was extracted. Run the following commands

```bash
set PIN=1234
java -jar owlcms-4.X.Y.jar
```

- Under Mac or Linux, start a terminal shell and change directory to where the program was extracted.

```bash
PIN=1234 java -jar owlcms-4.X.Y.jar
```

## Starting screens or displays from the main laptop

On the laptop where you started owlcms, you can type http://localhost:8080 to get at the program.  Typically, this is where you will run the competition secretary tasks.

## Starting screens or displays from other laptops

Any additional screen you want to use (for TVs, projectors, or additional screens for officials) requires a laptop or a small PC.  A Web browser is run on each PC, and the browser drives the screen.

If you start a browser on the main laptop, the home screen shows a list of Web addresses that you can use to connect additional browsers to owlcms.  These addresses will typically look something similar to

```
http://192.168.0.100:8080
```

but they change depending on the network you are connecting to and how it was configured.  There will normally be several such addresses if WIFI is enabled on the router.

1. If the additional laptop is connected using ethernet wiring to a router, use the address labeled `(wired)` in the list. 
2. If your laptop is not connected using an ethernet cable, use the address labeled ```(wireless)```
3. There may be several addresses listed for each kind; try them in turn, or else ask the person who configured the router
4. If none of the addresses work, or if only the localhost (127.0.0.1) address shows up, ask the person who configured the router or the server laptop.  

