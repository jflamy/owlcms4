## MQTT Messages

### Conventions

- All topics are under `owlcms`.  The full topic would be `owlcms/fop/config`
- _In all example messages, `A` is the name of the current platform._
- The topics end with the platform name.  In the examples below, the message content is what follows the space
- Parameters to the message are shown in the form `:parameter` ; the actual messages do not include the `:` this is just a convention to identify what information will be substituted.

### Messages Published by owlcms

#### Subscribed by all devices

- `fop/startup/A :status`: owlcms has completed the startup process.
    - `status` is `on` or `off`
    - Turns the LEDs on (or off) on external devices to confirm connectivity.
    - DEPRECATED : the following message legacy message is equivalent : `led/A :status`
- `fop/config`  this message published in response to a `config` request from the device.  Note that there can be multiple answers - one per platform.  The answers are all identical, all but the first one can be ignored.
    - returns a JSON map of the form `{"platforms":["A","B"],"version":"38.0.0-alpha00"}` 
    - the `platforms` entry contains the list of configured platforms.


#### Subscribed by jury and referee devices

- `fop/resetDecisions/A :option`: The clock has started for a new attempt. All LEDs for referee and jury member decisions are turned off. 
    - `option` is currently `reset` but is ignored.
    - This is not the timekeeper clock start.  It is the clock start only when a new attempt clock is starting, or athlete clock continues after changes. This event does not occur if the athlete lifts the bar and puts it down.
- `fop/decision/A :ref :decision`: 
   A referee has made a decision.  The jury devices shows the decision if it has the red/white LEDs.
   - `ref` is 1 2 or 3
    - `decision` is `good` or `bad`

#### Subscribed by jury devices

- `fop/juryMemberDecision/A :juryMember :decision`: 
   A jury member has made a decision.  Decisions are not shown until all jury members have voted The jury devices shows `hidden` as a green LED, and good/bad as white/red.  This message is emitted as hidden when the jury member votes, and on the last vote the message is emitted again with the actual decision. The last jury member to vote is not given `hidden`.
   A jury device may perform this processing autonomously and ignore these messages.
   - `juryMember ` is 1 2 3 4 or 5
    - `decision` is `hidden` `good` or `bad`

- `fop/juryDeliberation/A`: 
  Jury deliberation has been started (the application is in BreakType.JURY mode.)  There is no argument. The device should clear the jury member decision lights in order to allow a secret vote to be taken on a video replay.
  
- `fop/challenge/A`: 
  A challenge has been issued (the application is in BreakType.CHALLENGE mode.)  There is no argument. The device should clear the jury member decision lights in order to allow a secret vote to be taken on a video replay.
  

#### Subscribed by the referee device:

- `fop/decisionRequest/A :ref`: The third ref must make a decision.
    - LEGACY: `decisionRequest/A/:ref :status` (status is `on` or `off`)
    - `ref` is `1`, `2`, or `3`
    - The delay before triggering is configurable in owlcms.  If the device decides to remind the referee on its own, it should be longer than the owlcms default.
- `fop/summon/A :official`: The jury summons an official to the jury table.
    - LEGACY: `summon/A/:official :status` (status is `on` or `off`, off is unused, device turns it off on its own)
    - `official` is `1`, `2`, `3`
    - Summon `all` results in 3 individual messages for referees 1 2 and 3;
    - Official 4 is used for the technical controller.  Currently results in a notification on the announcer/marshal/timekeeper screens 

#### Subscribed by the down signal device:

These messages are used by a signal tower with a white light and a buzzer.

- `fop/down/A`: The down signal has been given
    - device must turn itself off.
- `fop/timeRemaining/A :seconds`
    - `seconds` are remaining for the athlete.  Values are 90 (timeout for declarations on a 2:00 clock), 30 (final warning), 0 seconds (time is over).

#### Subscribed by external observers

These messages are used by observers such as video production software (in conjunction with others such as the `decision` and `resetDecisions` messages).

In general, an external observer should listen to all `fop` events to decide what is going on.

- `fop/liftingOrderUpdated/A` Data entry has taken place (possibly changing requested weight or lifting order) or decision has been given.

- `fop/ceremony/A :name :status`  A ceremony has started or stopped

  - `name` is one of

    ```
    INTRODUCTION
    OFFICIALS_INTRODUCTION
    MEDALS
    ```

  - `status` is `start` or `stop`

- `fop/break/A :breakType` A break has started.  There are no end break event.  THe `fop/startlifting/A` message will indicate that competition has resumed. or a `ceremony` message will take place.  Not that JURY and CHALLENGE breaks are not be issued, they are issued as separate `juryDeliberation` events.

  - breakType is one of 

    	BEFORE_INTRODUCTION
    	FIRST_SNATCH
    	FIRST_CJ
    	GROUP_DONE
    	TECHNICAL
    	MARSHAL


- `fop/startLifting/A`   Lifting has started on the platform.  This is the normal way to end a break.
- `fop/refereesDecision/A` `:decision` after a lift, when `good` or `bad` lift majority decisions are shown.  Used by observers to control lighting or other ambiance effects without having to parse the individual decisions to determine majority.  Issued at the same time as the individual decisions.

### Messages published by all devices

Only owlcms listens; all devices can query owlcms for its configuration

- `config` request the owlcms configuration
  - no parameter
  - The response will come in an a `fop/config` message.

### Messages published by the referee devices

Only owlcms listens; devices do not listen to one another.

- `refbox/decision/A :ref :decision`: Ref decides the attempt is a good lift or no lift. 
    - `ref` is `1`, `2`, or `3`
    - `decision` is `good` or `bad`
    - DEPRECATED: This message is equivalent to `decision/A :ref :decision`
- `refbox/downEmitted/A`: The referee device has emitted a down signal.

### Messages published by the timekeeper device

Only owlcms listens; devices do not listen to one another.

- `clock/A :action`: The timekeeper has started, stopped, or reset the clock.
  - `action` is `start`, `stop`, `60`, or `120`

### Messages published by the jury device

Only owlcms listens; devices do not listen to one another.

- `jurybox/summon/A :official`: The jury has summoned an official.
    - `official` is `1`, `2`, `3`, `all` or `controller`
- `jurybox/juryMember/decision/A :member :decision`: Jury member decides the attempt is a good lift or no lift.
    - `member` is `1`, `2`, `3`, `4`, or `5`
    - `decision` is `good` or `bad`
- `jurybox/decision/A :decision`: The jury has made a decision.
    - `decision` is `good` or `bad`
- `jurybox/break/A :breakEvent`: The jury has called for a break.
   - `breakEvent` is `technical` `deliberation` (start a jury-initiated break), `challenge` (start a challenge-initiated break) or `stop` (resume competition).

## External controller logic:

- General
    - When performing an action that may change the state of the field of play, publish the relevant message, then wait for the corresponding message to be published by owlcms before updating other devices. For example, when the jury summons a referee, publish `jurybox/summon/A 1`, but do not play a sound from the referee control box until owlcms publishes `fop/summon/A 1`.
        - The down signal is an exception to this rule since athlete safety is a concern.  The referee device may play the sound on its own initiative, and immediately broadcast that it has done so.
- Referees
    - When a decision is made, publish `refbox/decision/A 1 good`.
    - When a majority decision has been made, activate down signal.
        - Publish `refbox/downEmitted/A`.
    - Reset decision tracking 3 seconds after all referees have made a decision.
        - Subscribe to `fop/clockStart/A` for additional reset scenarios.
    - When summoned by the jury, play a sound.
        - Subscribe to `fop/summon/A 1`.
- Down Signal
    - When a majority decision has been made, activate light and sound for 2 seconds.
        - Subscribe to `fop/down/A` as a fallback; ignore if published within 5 seconds of on board signal.
- Jury
    - When the jury summons a referee, publish `jurybox/summon/A 1`.
    - When a jury member has made a decision, publish `jurybox/juryMember/decision/A 1 good`.
        - Display a green light on the control panel for the relevant jury member.
    - When all jury members have made a decision, change the green lights to red and white lights.
    - When the jury has reached an unanimous decision, publish `jurybox/decision/A good`.
    - When the jury calls the controller, publish `jurybox/summon/A controller`.
    - When the jury calls for a deliberation, publish `jurybox/break/A deliberation`.
    - When the jury calls for a technical break, publish `jurybox/break/A technical`.
    - When the jury ends a break, publish `jurybox/break/A stop`.
    - When a referee decision has been made, immediately display the decision on the jury control panel.
        - Subscribe to `fop/decision/A`.
        - Reset the decision on `fop/resetDecisions/A` .
- Timekeeper
    - When the timekeeper starts the clock, publish `clock/A start`.
    - When the timekeeper stop the clock, publish `clock/A stop`.
    - When the timekeeper resets the clock to one minute, publish `clock/A 60`.
    - When the timekeeper resets the clock to two minutes, publish `clock/A 120`.
