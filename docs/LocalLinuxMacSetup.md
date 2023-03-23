## macOS or Linux Installation

- **Get the installation zip archive**: Get the current  [`owlcms_38.2.0.zip`](https://github.com/owlcms/owlcms4/releases/latest/download/owlcms_38.2.0.zip) file  (located in the `assets` section at the bottom of each release in the [release repository](https://github.com/owlcms/owlcms4/releases/latest) .

- Double-click on the downloaded zip file, and extract the files to a directory.  We suggest you use `~/owlcms` as the unzipped location.

- Make sure you have a Java 11 or 17 installed (JRE or JDK)

  - For Linux, refer to [Latest Releases | Adoptium](https://adoptium.net/temurin/releases/) depending on the Linux type you run

    - For Ubuntu and other Debian variants, the following should work 

       ```bash
       sudo apt install default-jre
       ```

  - For macOS, you can use Homebrew to install (or see [Latest Releases | Adoptium](https://adoptium.net/temurin/releases/) if you prefer)

    - Start a terminal and run the following commands - each command is on a single line -- move your mouse over the grey box and use "Copy to Clipboard".

      ```bash
      /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
      ```

      ```bash
      brew install openjdk@17
      ```

      

- To start the program, open a Terminal window,  directory to the location where you unzipped the files and launch Java as follows.  Assuming you extracted to a directory called `owlcms` in your home, the following would work

  ```bash
  cd ~/owlcms
  java -jar owlcms.jar
  ```
  This will actually start the program. See [Initial Startup](#initial-startup) for how to proceed.

  If you just want to use dummy data to practice (which will not touch the actual database), use instead:

  ```
  java -DdemoMode=true -jar owlcms.jar
  ```


## Initial Startup

When owlcms is started on a laptop, two windows are visible:  a command-line window, and an internet browser

- The command-line window (typically with a black background) is where the OWLCMS primary web server shows its execution log.  

  All the other displays and screens connect to the primary server.  <u>You can stop the program by clicking on the x</u> or clicking in the window and typing `Control-C`.  The various screens and displays will spin in wait mode until you restart the primary program -- there is normally no need to restart or refresh them.

- Depending on the platform, a browser will be opened automatically (or not).  If the browser does not open automatically, navigate to http://localhost:8080

- After the browser opens, if you look at the top, you will see two or more lines that tell you how to open more browsers and connect them to the primary server.

  ![060_urls](img\LocalInstall\060_urls.png)

  In this example the other laptops on the network would use the address `http://192.168.4.1:8080/` to communicate with the primary server.  "(wired)" refers to the fact that the primary laptop is connected via an Ethernet wire to its router -- see [Local Access](EquipmentSetup#local-access-over-a-local-network) for discussion.  When available, a wired connection is preferred.

  The address <u>depends on your own specific networking setup</u> and you must use one of the addresses displayed **on your setup.**  If none of the addresses listed work, you will need to refer to the persons that set up the networking at your site and on your laptop.  A "proxy" or a "firewall", or some other technical configuration may be blocking access, or requiring a different address that the server can't discover.

## Accessing the Program Files and Configuration

In order to uninstall owlcms4, to report problems, or to change some program configurations, you may need to access the program directory where you unzipped the files (the same where you start java from).

If you do so, you will see the installation directory content that is relevant to Linux and MacOS:

- `database` contains a file ending in `.db` which contains competition data and is managed using the [H2 database engine](https://www.h2database.com/html/main.html). 

- `logs` contains the execution journal of the program where the full details of what happened are written. If you report bugs, you will be asked to send a copy of the files found in that directory (and possibly a copy of the files in the database folder as well).

- `local` is a directory that is used for translating the screens and documents to other languages, or to add alternate formats for results documents.

- `jre`  contains the Java Runtime Environment

- the file ending in `.jar` is the OWLCMS application in executable format


## Configuration Parameters

See the [Configuration Parameters](Configuration.md  ' :include') page to see additional configuration options in addition to the ones presented on this page.