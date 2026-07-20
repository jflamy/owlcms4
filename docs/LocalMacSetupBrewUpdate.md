

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

