# Refereeing

OWLCMS supports using refereeing devices, and also supports 3 and 5-person [Jury](#Jury). Refereeing and Jury screens are started from the lifting page as shown below.  However, the decisions are displayed to the public and athletes on the displays started on the [Start Displays](Displays) page.

![010_Ref](img/Refereeing/010_Ref.png)

Refereeing can be performed in 3 different ways

- With no technology.  Decisions are made using flags, colored cards, or hand signals.  
- With mobile devices (typically, a cellular phone or a tablet)
- With buttons connected to the athlete-facing decision display (or an attempt board)

## Manual Refereeing

The announcer announces the decision and uses the buttons at the top right of his screen to register the decision.

## Mobile Device Refereeing

 The referee console is started from the "run a lifting group" page, or by adding /ref to the URL for the competition site.  For example https://owlcms4.herokuapp/ref  leads to the referee application.  After starting the referee screen, it is necessary to select which referee is which (1, 2 or 3) using the numeric input at the top (use the + and - signs)

![mobile_ref](img\equipment\mobile_ref.png)

The selection is confirmed by greying out the other key.  The refereeing display is reset when an athlete gets fresh clock.

## USB or Bluetooth Keypads

In this approach, keypads are connected to the laptop or mini PC running the timer/decision display (or the attempt board, both work the same).   There are two advantages to this approach:

1. Many referees prefer having their finger rest on a button (which is not possible on a phone)
2. The sound and down arrow are emitted directly by the browser, without any round-trip to the master computer.  This reduces delays and increases reliability if the networking is fragile (which is sometimes the case in gyms)

![refereeingSetup](img\equipment\refereeingSetup.jpg)

The keys or buttons on the keypads are programmed to send key sequences.  The decision display is waiting for these keypresses.  You must click in the black area of the screen to make sure that the keypresses are seen by the browser.

OWLCMS4 interprets Even digits as red, and Odd digits as white.  The same devices can be used for referees and for the jury. 

| Referee# | Jury# | Good | Bad  |
| -------- | ----- | ---- | ---- |
| 1        | 1     | 1    | 2    |
| 2        | 2     | 3    | 4    |
| 3        | 3     | 5    | 6    |
|          | 4     | 7    | 8    |
|          | 5     | 9    | 0    |

### Keypad configuration notes

- Specific notes for [Delcom USB keypads](http://www.delcomproducts.com/productdetails.asp?PartNumber=706502-5M) can be found [here](Delcom)

## Jury

In order to use a jury, in the current version, you need 3 or 5 refereeing devices connected to the computer with the Jury console.

The devices are programmed just like for the referees, except that jury 4 uses keys 7 and 8, and jury 5 uses the keys 9 and 0.  As for the referees, even-numbered keys mean "red".

The jury console operates according to IWF rules: the referee decisions are shown as soon as they are made, in the bottom part of the screen.  In the top part of the screen, the decisions circle for a jury member shows that he or she has made a decision, but the individual decisions are only shown in red or white after they have all been given.

![070_Jury](img/Refereeing/070_Jury.png)

