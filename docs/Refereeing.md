# Refereeing

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

The keys or buttons on the keypads are programmed to send key sequences.  The decision display is waiting for these keypresses.  

- 1 will be interpreted as a red from referee 1, and 2 will be interpreted as a white. 
- 3 will be interpreted as a red from referee 2, and 4 will be interpreted as a white.  
- Finally, 5 will be interpreted as a red from referee 3, and 6 will be interpreted as a white.