## macOS Installation 

macOS is opinionated about software that has not gone through the notarization process and generates needlessly alarming warnings, even when updating (see the [legacy installation](LocalMacSetupLegacy.md) for examples)

> Deepest apologies for having to use the Terminal.
> You will be asked to run two (2) commands and type your password once.

#### 1. (Needed only once) Install the `homebrew` Installation Manager

- Move your mouse over the grey box below, then click the **Copy to Clipboard** text that appears in its top-right corner (it will say "Copied").

  ```
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  ```

- Using the Application icon ![image-20260603094422742](img/LocalMacSetup/image-20260603094422742.png)  at the bottom of your screen in  the Dock, 

  1. Type `Terminal` to locate the Terminal application. You should see something like this.

  ![image-20260717105936731](img/LocalMacSetup/image-20260717105936731.png)

  2. Start Terminal by clicking once on the icon
  3. **Paste** the command that we copied earlier using `⌘V` (Command-V) and then type `↩` (Return) to start it.

  4. You will be asked for your password
     -  Type your password
        *Nothing will appear as you type — no letters, no dots, no stars.*
     -  hit  `↩`  (Return) at the end of your password
  5. You will need to hit Return again and accept suggestions by typing `yes` or `y`  as requested
     - Don't worry about lots of text being printed out
  6. The installation will proceed and will bring you back to a waiting Terminal.

#### 2. (Needed only once) Install the control panel

- If you have a newer Apple Silicon Mac (M1/M2/M3...) , move your mouse over the grey box below, then click the **Copy to Clipboard** text that appears in its top-right corner (it will say "Copied").
  ```
  /opt/homebrew/bin/brew install --cask owlcms/brew/controlpanel --force
  ```

  Go back to the terminal window, **Paste** and hit **Return** `↩` to run the command.

- For an older Intel Mac, move your mouse over the grey box below, then click the **Copy to Clipboard** text that appears in its top-right corner (it will say "Copied").

  ```
  /usr/local/bin/brew install --cask owlcms/brew/controlpanel --force
  ```

- After the installation runs, OWLCMS control panel will be visible as owlcms in the Applications folder
  ![image-20260717114320159](img/LocalMacSetup/image-20260717114320159.png)

- Clicking on the icon will now start the control panel.  We are done.



If you later want to update the control panel, see [this page](LocalMacSetupBrewUpdate.md)
