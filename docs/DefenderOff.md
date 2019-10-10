## Why is the anti-virus scan taking forever ?

The Windows Defender scan of the installer is extremely slow.  There are a few thousand files zipped inside the installer, and apparently Defender is looking at each and everyone of them, even though roughly 90% of them are from well-known open-source libraries. I have no control over what Microsoft does.

The easiest work-around is to disable the real-time scan feature of Windows 10.  After installing, you can re-enable it.  The installer will be scanned anyway the next time the nightly scan is run (which will reassure you that there was nothing evil inside). 

## Telling Defender to skip the scan

In order to allow the install to go ahead, proceed as follows

1. Click on the start menu at the bottom left and then on the cogwheel
   ![](img\DefenderOff\0cogwheel.png)

2. Type "defender" in the search box and select the Windows Defender settings
   ![2019-10-09 19_21_42-Settings](img\DefenderOff\1defenderSettings.png)

3. Select the Virus & threat protection settings
   ![2019-10-09 19_22_24-Settings](img\DefenderOff\2virusProtection.png)

4. Select the "Virus Protection" Settings
   ![3virusSettings](img\DefenderOff\3virusSettings.png)

5. Select the "Real-time Protection" Manage Settings option and turn it off
   ![2019-10-09 19_22_44-Windows Security](img\DefenderOff\4RealTime.png)

6. Proceed with the installation by double-clicking on the `owlcms_setup.exe` file you downloaded

    - **If the install does not start because of a blue message "Windows protected your PC", see below**.

8. In order to re-enable real-time protection after installing, just flip the switch back on.

## Allowing the installation to take place

If you see a blue box like this
![5protected](img\DefenderOff\5protected.png)

Click on the `More info` link, which will make a button appear at the bottom.
![6allow](img\DefenderOff\6allow.png)

## Why is Windows doing this?

The `unknown publisher` message means that the author did not buy a code signing certificate which costs roughly 400$ per year and requires sending personal information such as passport information to establish legitimate identity.  And in any case, author did not feel like adding the technically tricky steps to perform a cryptographic signature of the installer.

Should anyone donate the money to acquire the certificate, I *might* reconsider.