For shortcuts, the keys given below are the key locations for a US keyboard. You will use the key at the [location shown on this diagram](https://kbdlayout.info/kbdus), no matter what is actually printed on your keyboard.  Shortcut keys are defined according to [a standard](https://www.w3.org/TR/uievents-code/#key-alphanumeric-writing-system).

We recommend using the numeric pad codes as shortcuts when one is defined.

### Announcer Keypad Shortcuts

- The timekeeping keypad shortcuts are used (see below)
- For a good lift, use the F2 key
- For a bad lift, use the F4 key

### Timekeeping Keypad Shortcuts

- The clock can be started by using the `,` ("Comma"). The `/`("Numpad Divide") key on the numeric keypad also works. On the Flic2 buttons, this is the `/` shortcut.
- The clock can be stopped by using the `.` (period) key on the keyboard. The decimal fraction character on the numeric keypad ("Numpad Decimal") should also work (in many countries, the numeric keypad shows a `,` instead of a `.` to match the local usage for decimal fraction). For the Flic2 buttons, this is the `.` shortcut.
- The clock can be reset to 1:00 by using the + key on the keyboard (more precisely, the key combination that corresponds to + on a US keyboard, that is, Shift=). Using the + key on the numeric keypad will also work. On the Flic2 buttons, this is the `=` shortcut.
- The clock can be reset to 2:00 by using the `=` or `;`key on the keyboard ). Using the = (Numpad Equal) key on the numeric keypad will also work. On the Flic2 buttons, this is the `=` or `;` shortcut.

### Refereeing Keypad Shortcuts

The keys or buttons on the keypads are programmed to send key sequences.  The decision display is waiting for these keypresses.  You must click in the black area of the screen to make sure that the keypresses are seen by the browser.

OWLCMS interprets Even digits as red, and Odd digits as white.  The same devices can be used for referees and for the jury. 

| Referee# | Good | Bad  |
| -------- | ---- | ---- |
| 1        | 1    | 2    |
| 2        | 3    | 4    |
| 3        | 5    | 6    |
|          | 7    | 8    |
|          | 9    | 0    |

- For most countries, hitting the key "Digit1" as defined in this [standard](https://www.w3.org/TR/uievents-code/#key-alphanumeric-writing-system) sends a 1.  But there are exceptions. For example, in France, hitting Digit1 will actually send a "&" .  So you may actually need to use "&" instead of "1".

### Jury Keypads

##### Jury Member Keypad

Each jury member must have a button keypad connected to the laptop (either directly, via a USB hub, or via Bluetooth). 

The same conventions are used as for the refereeing keypads.  You can use the same devices for the first three jury members as for the three referees.

| Jury# | Good | Bad  |
| ----- | ---- | ---- |
| 1     | 1    | 2    |
| 2     | 3    | 4    |
| 3     | 5    | 6    |
| 4     | 7    | 8    |
| 5     | 9    | 0    |

##### Jury Chair Keypad

> If you intend to build a jury chair keypad, you might want to consider the Arduino-based [button-only jury box](../060-Refereeing/020-Jury#button-only-jury-box) since you are likely going to use identical hardware and you can avoid having to do the programming yourself.

An additional keypad can be connected to the Jury Control computer, which would typically be operated by the Jury Chair.  This keypad also works using keyboard shortcuts.

The following shortcuts are bound.  Depending on how your device behaves, you may have to send either the lowercase letter or the uppercase letter, <u>please try both</u> !  The keys are the same as in previous versions; what some of them do depends on whether a review (deliberation or challenge) is in progress.

- "d" starts a jury deliberation (`KeyD` event code)
- "c" starts a challenge; during a review, calls the technical controller (ex: for a loading error where the athlete will need to make a decision) (`KeyC` event code)
- "t" starts a technical break if the Jury spots a technical issue (`KeyT` event code.)
- "h", "i", "j" call referee 1, referee 2, referee 3 to the jury table.
- "k" calls all referees.  During a seven-person Adjudication Panel review (`7personAdjudication` with video playback), it is **Call for Votes**: the votes are cleared and the seven officials vote again.  In the other reviews it calls the referees to the jury table.
- "g" and "b", during a review: the Jury Chair gives a good lift or a bad lift (`KeyG`, `KeyB` event codes).  "b" opens the technical rule selection on Jury Control, and the decision is applied once the rule is chosen.
- "Escape" ends the jury break (`Escape` event code).  It is not needed once a decision has been given: lifting resumes by itself.

Jury Control has no keys for the jury vote confirmation (Confirm Good Lift, the technical rule, Cancel vote result): these are chosen on the screen.

