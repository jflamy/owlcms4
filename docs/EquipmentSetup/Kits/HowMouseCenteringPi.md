### How to Create Keyboard Shortcuts to Center the Mouse on a Specific Screen of a Raspberry Pi

This guide shows you how to set up shortcuts to instantly move your mouse to the center of any screen in a multi-monitor setup on your Raspberry Pi.

---

### Step 1: Identify Your Screen Names and Positions

First, you need to find the name, position, and dimensions of each of your monitors. We'll use the `wlr-randr` command for this.

1.  Open a terminal and run:
    ```bash
    wlr-randr
    ```

2.  Examine the output. For each screen, note its **Name** (e.g., `HDMI-A-1`, `DP-1`), **Position** (e.g., `0,0`), and **Resolution** (e.g., `1920x1080`).

    **Example Output:**
    ```
    HDMI-A-1
      ...
      Modes:
        1920x1080@60.000Hz
      Position: 0,0
    DP-1
      ...
      Modes:
        2560x1440@144.000Hz
      Position: 1920,0
    ```

---

### Step 2: Create a Universal Mouse-Centering Script

This script will take a screen's name as an argument and calculate its center before moving the mouse.

1.  Open a terminal and create the script file:
    ```bash
    mkdir -p ~/scripts
    nano ~/scripts/center-mouse-on-screen.sh
    ```

2.  Copy and paste the following code into the file:
    ```bash
    #!/bin/bash
    
    # Get the screen name from the script's first argument
    SCREEN_NAME=$1
    
    # Use wlr-randr to get the dimensions and position of the screen
    SCREEN_INFO=$(wlr-randr | grep -A 2 "$SCREEN_NAME")
    
    # Extract resolution and position
    RESOLUTION=$(echo "$SCREEN_INFO" | grep "Modes:" | head -n 1 | awk '{print $2}')
    POSITION=$(echo "$SCREEN_INFO" | grep "Position:" | awk '{print $2}')
    
    # Parse dimensions and position
    WIDTH=$(echo "$RESOLUTION" | cut -d'x' -f1)
    HEIGHT=$(echo "$RESOLUTION" | cut -d'x' -f2)
    OFFSET_X=$(echo "$POSITION" | cut -d',' -f1)
    OFFSET_Y=$(echo "$POSITION" | cut -d',' -f2)
    
    # Calculate the center coordinates
    CENTER_X=$((OFFSET_X + WIDTH / 2))
    CENTER_Y=$((OFFSET_Y + HEIGHT / 2))
    
    # Use ydotool to move the mouse to the calculated center
    ydotool mousemove --absolute $CENTER_X $CENTER_Y
    ```

3.  Save the file by pressing `Ctrl + X`, then `Y`, then `Enter`.

4.  Make the script executable:
    ```bash
    chmod +x ~/scripts/center-mouse-on-screen.sh
    ```

---

### Step 3: Configure Keyboard Shortcuts in LabWC

Now, you'll edit LabWC's configuration file to create a shortcut for each screen you want to control.

1.  Open the `rc.xml` file:
    ```bash
    nano ~/.config/labwc/rc.xml
    ```

2.  Find the `<keyboard>` section.

3.  Add a separate `<keybind>` entry for each screen. This example uses the **Raspberry Pi logo key** plus **1** for the first screen (`HDMI-A-1`) and the **Raspberry Pi logo key** plus **2** for the second screen (`DP-1`). **Remember to replace the screen names with your own.**

    ```xml
    <keybind key="W-1">
      <action name="Execute">
        <command>~/scripts/center-mouse-on-screen.sh HDMI-A-1</command>
      </action>
    </keybind>
    
    <keybind key="W-2">
      <action name="Execute">
        <command>~/scripts/center-mouse-on-screen.sh DP-1</command>
      </action>
    </keybind>
    ```

4.  Save the `rc.xml` file.

---

### Step 4: Apply the Changes

Finally, reload LabWC to apply the new shortcuts.

* You can log out and log back in, or
* Use a reload keybind if one is configured in your `rc.xml` file (often the **Raspberry Pi logo key** plus **R**).

Your shortcuts are now active! Use the **Raspberry