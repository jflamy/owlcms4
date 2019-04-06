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
    
## Initialization of Athlete Screen and Layout

### restart, only URL with a group is announcer's
```
16:22:11.342 DEBUG AthleteGridContent constructor                                              [app.owlcms.ui.group.AthleteGridContent:121 <init>]
16:22:11.372 DEBUG AthleteGridLayout getLayoutConfiguration                                    [app.owlcms.ui.group.AthleteGridLayout:50 getLayoutConfiguration]
16:22:12.114 DEBUG created AthleteGridLayout                                                   [app.owlcms.ui.group.AthleteGridLayout:41 <init>]
16:22:12.118 DEBUG AthleteGridContent parsing URL                                              [app.owlcms.ui.group.AthleteGridContent:130 setParameter]
16:22:12.287 INFO  A loading group M2                                                          [app.owlcms.state.FieldOfPlayState:535 initGroup]
16:22:12.498 DEBUG setting Time -- timeRemaining = 60000 [app.owlcms.state.FieldOfPlayState.recomputeLiftingOrder(FieldOfPlayState.java:585)] [app.owlcms.state.RelayTimer:110 setTimeRemaining]
16:22:12.516 DEBUG AthleteGridLayout setting bi-directional link                               [app.owlcms.ui.group.AthleteGridLayout:67 showRouterLayoutContent]
16:22:12.517 DEBUG showRouterLayoutContent                                                     [app.owlcms.ui.home.OwlcmsRouterLayout:91 showRouterLayoutContent]
16:22:12.852 DEBUG findAll A M2 org.vaadin.crudui.crud.impl.GridCrud.refreshGrid(GridCrud.java:127) [app.owlcms.ui.group.AthleteGridContent:461 findAll]
16:22:12.856 DEBUG AthleteGridContent creating top bar                                         [app.owlcms.ui.group.AthleteGridContent:198 createTopBar]
16:22:12.879 DEBUG select setting group to M2                                                  [app.owlcms.ui.group.AnnouncerContent:73 lambda$0]
16:22:13.203 DEBUG syncWithFOP app.owlcms.ui.group.AthleteGridContent.lambda$1(AthleteGridContent.java:167) [app.owlcms.ui.group.AthleteGridContent:314 syncWithFOP]
16:22:13.206 INFO  A switching to group M2                                                     [app.owlcms.state.FieldOfPlayState:527 switchGroup]
16:22:13.206 INFO  A loading group M2                                                          [app.owlcms.state.FieldOfPlayState:535 initGroup]
16:22:13.314 DEBUG setting Time -- timeRemaining = 60000 [app.owlcms.state.FieldOfPlayState.recomputeLiftingOrder(FieldOfPlayState.java:585)] [app.owlcms.state.RelayTimer:110 setTimeRemaining]
16:22:13.337 DEBUG setting Time -- timeRemaining = 60000 [app.owlcms.state.FieldOfPlayState.recomputeLiftingOrder(FieldOfPlayState.java:585)] [app.owlcms.state.RelayTimer:110 setTimeRemaining]
16:22:13.339 INFO  current athlete = Martinez_Michael_789398712 attempt 1, requested = 61, timeAllowed=60000 [app.owlcms.state.FieldOfPlayState:638 uiDisplayCurrentAthleteAndTime]
16:22:13.340 DEBUG doUpdateTopBar app.owlcms.ui.group.AthleteGridContent.lambda$8(AthleteGridContent.java:329) [app.owlcms.ui.group.AthleteGridContent:289 doUpdateTopBar
```
    
    