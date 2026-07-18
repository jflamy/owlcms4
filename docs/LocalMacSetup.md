## macOS Installation 

macOS is opinionated about software that has not gone through the notarization process and generates needlessly alarming warnings, even when updating (see the [legacy installation](LocalMacSetupLegacy.md) for examples)

This process uses an installation manager to get around those issues.

#### 1. (Needed only once) Install the `brew` Installation Manager

Click on the link to download [**this file**](https://github.com/Homebrew/brew/releases/latest/download/Homebrew.pkg).   Allow downloading if asked.

- If using Safari, you will see something like this if you click on the down arrow icon at the top right of the browser. (Similar icons are present on Chrome and Firefox)

  ![image-20260717104511676](img/LocalMacSetup/image-20260717104511676.png)

- Double click on the `Homebrew.pkg` icon. This will open an installer.
  
-  Click on **Continue** and accept all the proposed defaults.

  - You will be asked for your passoword or TouchId.
  - When the **Close** button is shown, just Close and ignore the "Next Steps" that are proposed.
  - When asked to move the .pkg installer to trash, accept.

#### 2. Install the control panel

- Using the Application icon ![image-20260603094422742](img/LocalMacSetup/image-20260603094422742.png)  in the Dock, type `Terminal` to locate the Terminal application. Start it by clicking on the icon.
  ![image-20260717105936731](img/LocalMacSetup/image-20260717105936731.png)

- If you have a newer Apple Silicon Mac (M1/M2/M3...) , copy and paste the following to the terminal window (move your mouse over the command text and click on the box at the top right to copy)
  ```
  /opt/homebrew/bin/brew install --cask owlcms/brew/controlpanel
  ```

  For an older Intel Mac, the command is
  ```
  /usr/local/bin/brew install --cask owlcms/brew/controlpanel
  ```

- After the installation runs, OWLCMS control panel will be visible as owlcms in the Applications folder
  ![image-20260717114320159](img/LocalMacSetup/image-20260717114320159.png)
  
  

#### 3. Upgrading the control panel to the current version

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



