# Design Notes

## Browser-side Behaviour
### Referee Decisions
1. Use a "referee-decisions" web-component polymer template that contains both the divs that show the results and listens to inputs.
    + Use a [polymer-keydown component](https://github.com/dmytroyarmak/polymer-keydown) to listen to keys
    + Include the referee divs and the code that changes their color based on key presses in the template
    + Include the code that updates the colors based on remote inputs (e.g. phones or tablets) -- *Synchronize with flow*
    + Include a variation with a single div for situations where announcer enters the decision (e.g. no refs, jury override)
+ Need to figure out how to stop countdown timer on keypress.  Is there a global js scope -- we need to avoid round-trip.
    
### Countdown Timer
1. Use a "slave-countdown-timer" web-component polymer template except when connected to refereeing devices (pressing will stop timer) or acting as time keeper
    + Timer is started from server-side
    + Timer is stopped from server-side
+ Refereeing and marshall use a "stop-countdown-timer"
    + timer is stopped client-side -- referee down signal while timer still running and stop is sent to server
    + server stops all other counters
+ Timekeeper (or announcer acting as timekeeper) uses a "master-countdown-timer"
    + timekeeper (or announcer acting as timekeeper) can stop and start
    + Timer is stopped from server-side by refereeing and marshall
    

    
    