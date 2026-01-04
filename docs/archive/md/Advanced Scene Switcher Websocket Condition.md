### Advanced Scene Switcher "Websocket" Condition

This condition allows you to trigger an ASS macro by sending a specific **custom message** via the OBS WebSocket server. It's a powerful way to integrate external applications or scripts directly with your ASS logic.

**How it works (The Communication Flow):**

1. **Your External Application/Script:** Connects to OBS Studio via OBS WebSocket.

2. Your External Application Sends:

    A specific type of OBS WebSocket request called 

   ```
   CallVendorRequest
   ```

   .

   - `vendorName`: This **must** be `"AdvancedSceneSwitcher"`.
   - `requestType`: This **must** be `"AdvancedSceneSwitcherMessage"`.
   - `requestData`: This is a JSON object where you define your custom message. The ASS plugin will look for a key named `"message"` within this object, but it can contain other data as well.

3. **OBS Studio:** Receives the `CallVendorRequest` and forwards it to the "Advanced Scene Switcher" plugin because of the `vendorName`.

4. Advanced Scene Switcher Plugin:

   - Parses the `requestType` and `requestData`.
   - If the `requestType` is `"AdvancedSceneSwitcherMessage"`, it then checks its configured macros for any that have a "Websocket" condition.
   - It compares the `message` content (or other parts of `requestData` if configured) from the incoming request with the criteria defined in the macro's "Websocket" condition.

5. **Macro Triggered:** If the incoming WebSocket message satisfies the "Websocket" condition(s) of a macro, that macro's actions are executed.

**Setting up the "Websocket" Condition in Advanced Scene Switcher:**

1. Open OBS Studio and navigate to `Tools > Advanced Scene Switcher`.

2. Go to the "Macros" tab.

3. Add a new macro (or edit an existing one).

4. In the "If" section (Conditions), click the `+` button.

5. Select `Websocket` from the dropdown list.

6. You'll then have options to configure how it listens for the message:

   - Message:

      This is the most common. You provide a specific string.

     - `is equal to`: The incoming message must be exactly this string.
     - `contains`: The incoming message must contain this string.
     - `starts with`: The incoming message must start with this string.
     - `ends with`: The incoming message must end with this string.
     - `is not equal to`: The incoming message must *not* be this string.
     - `does not contain`: The incoming message must *not* contain this string.
     - `regex matches`: Use a regular expression to match the message.

   - **Data (Advanced):** You can optionally check other fields within the `requestData` JSON object, not just the "message" field. This is powerful for sending more complex trigger data. You specify a "JSON Path" (e.g., `$.myCustomKey` to check the value of `myCustomKey`) and then a comparison type (e.g., `is equal to`, `greater than`, `is true`, `is not null`).

**Example: Triggering with a "Websocket" Condition (Python using `obs-websocket-py`):**

First, ensure you have the `obs-websocket-py` library installed (`pip install obs-websocket-py`).

**1. Advanced Scene Switcher Macro Setup:**

- **Condition:** `Websocket` -> `Message` -> `is equal to` -> `trigger_my_overlay_macro`
- **Action:** `Set Source Visibility` -> `Overlay Group` -> `visible`

**2. Python Script to Send the WebSocket Message:**

Python

```
import asyncio
import obsws_light as obs

# OBS WebSocket Server details
OBS_HOST = 'localhost'
OBS_PORT = 4455
OBS_PASSWORD = 'your_obs_websocket_password' # Set this in OBS Tools > WebSocket Server Settings

async def send_ass_message():
    client = obs.Client(host=OBS_HOST, port=OBS_PORT, password=OBS_PASSWORD)

    try:
        await client.connect()
        print("Connected to OBS WebSocket.")

        # Construct the vendor request for Advanced Scene Switcher
        response = await client.call(
            "CallVendorRequest",
            vendorName="AdvancedSceneSwitcher",
            requestType="AdvancedSceneSwitcherMessage",
            requestData={"message": "trigger_my_overlay_macro", "data_value": 123} # You can send more data too
        )

        print(f"Vendor request sent. Response: {response}")

        if response.status:
            print("Successfully sent message to Advanced Scene Switcher.")
        else:
            print(f"Failed to send message: {response.error}")

    except obs.exceptions.ConnectionFailed as e:
        print(f"Connection failed: {e}")
    except Exception as e:
        print(f"An error occurred: {e}")
    finally:
        await client.disconnect()
        print("Disconnected from OBS WebSocket.")

if __name__ == "__main__":
    asyncio.run(send_ass_message())
```

When you run this Python script, it will send the specified `CallVendorRequest`. If your Advanced Scene Switcher macro is active and listening for the message `trigger_my_overlay_macro`, it will execute its actions.

**When to use the "Websocket" Condition:**

- **Direct External Control:** When you want your own scripts or applications to directly initiate a specific ASS macro without relying on hotkeys, file changes, or OBS state changes.
- **Complex Data:** When you need to send more than just a simple trigger (e.g., a specific value, a username, an item ID) along with the trigger command, and your ASS macro needs to make decisions based on that data (using the "Data (Advanced)" condition type).
- **Decoupling:** When you want to decouple the trigger logic from specific OBS scene names or source states, allowing your external application to remain somewhat agnostic to your OBS setup while still controlling it.

This "Websocket" condition provides a very robust and direct way to integrate external control into your Advanced Scene Switcher workflows.