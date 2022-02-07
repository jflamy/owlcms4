In order to use a jury, in the current version, you need 3 or 5 refereeing devices connected to the computer running the Jury console.  You may optionally have a separate keypad for the jury president (see below) in order to initiate deliberation and transmit decisions.

The jury console is started from the "run a lifting group" page.

![010_Ref](img/Refereeing/010_Ref.png) 

The juror devices are programmed just like for the referees, except that jury 4 uses keys 7 and 8, and jury 5 uses the keys 9 and 0.  As for the referees, even-numbered keys mean "red".

The jury console operates according to IWF rules:

- In the bottom part of the screen the referee decisions are shown <u>as soon as they are made</u>  
- In the top part of the screen, the decisions circle for a jury member shows that he or she has made a decision, but the individual decisions are only shown in red or white <u>after they have all been given</u>.

![070_Jury](img/Refereeing/070_Jury.png)

The jury console now allows direct reversal/confirmation of lifts 
  - The Jury Deliberation button opens a dialog whereby the lift can be confirmed or reversed,
  - The Jury Head can ask the announcer to call the technical controller. 

![080_JuryDeliberation](img/Refereeing/080_JuryDeliberation.png)

Jury decisions are shown to the other technical officials consoles to keep them informed.  The announcer can then inform the public, coaches and athletes of the outcome.
![090_JuryReversal](img/Refereeing/090_JuryReversal.png)

## Jury Member Keypad

The same conventions are used as for the refereeing keypads.  You can use the same devices for the first three jury members as for the three referees.

| Jury# | Good | Bad  |
| ----- | ---- | ---- |
| 1     | 1    | 2    |
| 2     | 3    | 4    |
| 3     | 5    | 6    |
| 4     | 7    | 8    |
| 5     | 9    | 0    |

## Jury President Keypad

An additional keypad can be connected to the Jury console, which would typically be operated by the Jury president.  This keypad also works using keyboard shortcuts.

In order to support a jury console keypad, the following shortcuts are bound.  Depending on how your device behaves, you may have to send either the lowercase letter or the uppercase letter, <u>please try both</u> !

- "d" opens the deliberation dialog and starts a jury break (`KeyD` event code)
- "g" to indicate a good lift  (`KeyG` event code)
- "b" to indicate a bad lift  (`KeyB` event code)
- "c" to call the technical controller (ex: for a loading error where the athlete will need to make a decision) (`KeyC` event code)
- "t" to start a technical break if the Jury spots a technical issue (`KeyT` event code.)  The process is the same as for a deliberation. "c" can be used to call the controller, and "Escape" ends the technical pause.
- "h", "i", "j" and "k".  Call referee 1, referee 2, referee 3 and all referees.
- "Escape" to close the dialog and ends the jury break (`Escape` event code)

Please refer to [this document](https://www.w3.org/TR/uievents-code/#key-alphanumeric-writing-system) for the exact definition of the event codes.