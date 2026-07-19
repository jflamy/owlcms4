## macOS Installation 

macOS is opinionated about software that has not gone through the notarization process and generates needlessly alarming warnings, even when updating (see the [legacy installation](LocalMacSetupLegacy.md) for examples)

> Deepest apologies for forcing you to use the Terminal.  We will actually run two (2) commands.
>
> A yearly subscription to the Apple Developer program would be required to do better. Should someone pledge 99 US$ per year, this could be fixed.

#### 1. (Needed only once) Install the `homebrew` Installation Manager

- Click on the text in the grey box just below. Move your mouse to the top right of the grey box. The text "Copy to Clipboard" will appear.  Move your mouse over it and click.  It should say "Copied"

  ```
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  ```

- Using the Application icon ![image-20260603094422742](img/LocalMacSetup/image-20260603094422742.png)  in the Dock, type `Terminal` to locate the Terminal application. You should see something like this.

  ![image-20260717105936731](img/LocalMacSetup/image-20260717105936731.png)

  - Start Terminal by clicking on the icon.
  - Paste what was copied earlier using `⌘v` (Command-V, or use your mouse, right-click, Paste).  
  - Type `↩` to start the installation (Return key).
  - You will be asked for your password -- type your password and  `↩`  (your password will not be shown while you type)
  - You may have to type `↩` again to start the process, and may have to say "y" or "yes" so the installation is allowed to fetch missing pieces of software.  Basically accept all the suggestions made.
  - The installation will proceed and bring you back to a waiting Terminal.

#### 2. (Needed only once) Install the control panel

- If you have a newer Apple Silicon Mac (M1/M2/M3...) , do the same recipe as before, click on the text, move to the top Right, click to copy.
  ```
  /opt/homebrew/bin/brew install --cask owlcms/brew/controlpanel --force
  ```

  Go back to the terminal window, Paste and use Return `↩` to start the actual installation.

- For an older Intel Mac, the command to copy and paste is

  ```
  /usr/local/bin/brew install --cask owlcms/brew/controlpanel --force
  ```

- After the installation runs, OWLCMS control panel will be visible as owlcms in the Applications folder
  ![image-20260717114320159](img/LocalMacSetup/image-20260717114320159.png)

  Clicking on the icon will now start the control panel.



#### 3. Upgrading the control panel to the current version

If in the future you are asked to update the control panel, this is the recipe.  With luck you will never have to do it (I am hoping to automate this)

- If you have a newer Apple Silicon (M1/M2/M3...) Mac, copy and paste the following to the terminal window 

  ```
  /opt/homebrew/bin/brew update 
  /opt/homebrew/bin/brew upgrade --cask owlcms/brew/controlpanel
  ```

  For an older Intel Mac, the command is

  ```
  /opt/homebrew/bin/brew upgrade 
  /usr/local/bin/brew upgrade --cask owlcms/brew/controlpanel
  ```



