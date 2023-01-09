## Installing the Blue Owl Refereeing Package

### Pre-Requisites

Blue Owl requires a Node.js server to run.  We will install two pieces of software, nvm and npm

- nvm  which is a tool for installing Node.js.  The process for Windows and for macOS/Linux is different.
- npm (node package manager) which is what we will be using mostly.

#### Installing NVM

- On Windows, Download the [latest release](https://github.com/coreybutler/nvm-windows/releases/latest/download/nvm-setup.exe) and execute it.
- On macOS, see [this article](https://collabnix.com/how-to-install-and-configure-nvm-on-mac-os/)

- On Linux, there are different methods based on the distribution. For Ubuntu, see [this article](https://tecadmin.net/how-to-install-nvm-on-ubuntu-20-04/)

#### Installing Node.js and NPM

You now need to start a command shell.  On Windows aaaa10/11 left-click *once* on `⊞` and type `cmd` (or use the  `⊞` `R` shortcut to run a command. and then type `cmd`). On a Mac or Linux, start a Terminal session.

Installing the Node.js and npm programs is now done using

```
nvm install 16
nvm use 16
nvm alias default 16
```

 Currently the packages require version 16, so we make it the default.

#### Installing the Refereeing Package

- on Windows, obtain the blue-owl.exe self-extracting zip file from ...  Execute the file and unzip it to your desktop.
- on macOS or Linux, obtain the Zip and unzip it to a directory of your choice.

We then use our command line window to go to the directory and make it available.

```
cd Desktop/blue-owl
npm install
```

#### Installing an MQTT Server

The refereeing  device scripts and owlcms communicate with one another using the MQTT protocol.  A very simple server can be installed as follows:

```
npm install -g aedes-cli
```

