## macOS Installation

- Download the `.dmg` installer from the [release repository](https://github.com/owlcms/owlcms-controlpanel/releases) 
  - For current M1/M2/M3/M4/M5... AppleSilicon Macs: use this link [macOS Installer](https://github.com/owlcms/owlcms-controlpanel/releases/latest/download/macOS_OWLMCS.dmg)
  - For older Intel Macs: use this link: [macOS **Intel** Installer](https://github.com/owlcms/owlcms-controlpanel/releases/latest/download/macOS_Intel_OWLMCS.dmg)

- See this link for the [release notes](https://github.com/owlcms/owlcms-controlpanel/releases/latest)
- Open the `.dmg` file.   You should see something like this
  ![image-20260603091558899](img/LocalMacSetup/image-20260603091558899.png)
- Drag the owlcms icon over the Application icon.  This will copy the control panel app in your Application folder and you will find it there along with your other applications

## Starting the OWLCMS Control Panel

Open the Application folder using the ![image-20260603094422742](img/LocalMacSetup/image-20260603094422742.png) Dock icon and double-click on the owlcms icon.

> When you run the application for the first time after the initial installation, or after updating, **macOS will prevent it from running**.  Donations to acquire the necessary certificates to notarize the application and remove the need for the workarounds will be accepted, contact the developer at [owlcms@jflamy.dev]() 
>
> There are two distinct situations.

## 1. Fixing "Start Denied after Initial Installation"

When you first install the application and attempt to launch it, you will see something like 

![image-20260603100336121](img/LocalMacSetup/image-20260603100336121.png)

There are two remediesns for this

- **For Current macOS**
  
  - Go to the  `System Settings` > `Privacy` menu.  Scroll to the bottom.  You should see an option to allow owlcms to run.  See this [illustrated guide](https://wiki.hacks.guide/wiki/Open_unsigned_applications_on_macOS_Sequoia) for the process -- you will of course use `owlcms` as the application name.
  
- **For macOS 14 and earlier**:
  - Instead of double-clicking to start the application, **Right-click** on it. A warning about running an unsigned application will come up. **Select Open** to authorize the application to run.  This is only needed the first time around.

  Once this is done, you can follow the steps shown in the [Local Control Panel Overview](LocalControlPanel)

## 2. Fixing "Start Denied after an Update"

If you later download an updated DMG file and execute it, you will be prompted to replace or keep the existing app. Use "Replace".   When you launch the program, you will likely get a denial like this.

<img src="img/LocalMacSetup/image-20260703080716068.png" alt="image-20260703080716068" style="zoom:50%;" />

The reason is that macOS memorized a fingerprint for the version you initially accepted, but the new version does not match.  The program is then put in "quarantine". The fix is as follows

1. Open the Terminal application

2. Type the following (or move your cursor on the text, copy to clipboard, right-click to paste in Terminal, then ⏎ Return )

   ```
   xattr -rc /Applications/owlcms.app
   ```

3. Start owlcms again

