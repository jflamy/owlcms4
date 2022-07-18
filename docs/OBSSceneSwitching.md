## Automatic Video Scene Switching

Here are example scenarios where the ability to control video output is useful

- On the attempt board, or on the main scoreboard, you want to show a video or a slide show during breaks
- You are live streaming and you would like to switch to the athlete when the timer starts, and to show the scoreboard after a lift, automatically.
- You would like something special to be shown when there is a jury decision given
- You would like to switch to a different scene during medals presentation, etc.

The suggested way to do all of these is to use [OBS Studio](https://obsproject.com/) with an additional plug-in module called the [Advanced Scene Switcher](https://obsproject.com/forum/resources/advanced-scene-switcher.395/).  This module can track what is going on on the computer, and in particular, can trigger actions when certain window titles are present.

## Field of Play Status Monitor

owlcms includes a special-purpose output that displays the competition status in the browser title bar.  It also shows it in the main window, but that is for human readability, and is not actually used for video switching.

For example, instead of starting a browser as your attempt board display, you will start OBS.  OBS will open the browser and your presentation, and decide which to show by looking at what the status monitor window title is.

1. On the machine where you want to run OBS, start the status monitor![statusMonitorStart](img/OBS/statusMonitorStart.png)

2. The status monitor starts in its own tab.  Close the other tabs so it is alone in a window.  The window content is the same as what is actually in the title (even though the title is visually cut off.)  In the following example, the title indicates that the athlete information is visible on the various displays and scoreboards (CURRENT_ATHLETE_DISPLAYED). 
   

![statusMonitorExample](img/OBS/statusMonitorExample.png)

   > The full set of possible state transitions is documented further down on this page.

3. We can also see that the *previous* state is shown in the monitor.  This allows special cases with different scene transitions

   - For example, when the jury decides that a lift is denied, the status will start with `state=DECISION_VISIBLE.BAD_LIFT;previous=BREAK.JURY` because the decision took place while the system was in a jury break  
   - For a normal decision, the previous state would not be a jury break (likely the down signal or perhaps time stopped if down was given verbally) `state=DECISION_VISIBLE.GOOD_LIFT;previous=DOWN_SIGNAL_VISIBLE`

General rules:

- When the competition is in a break, the line starts with `break=`
- When the competition is not in a break, the line starts with `state=`



## Installing the Scene Switching Plugin

1. Stop OBS if running
2. Go to [[Advanced Scene Switcher | OBS Forums (obsproject.com)](https://obsproject.com/forum/resources/advanced-scene-switcher.395/)](https://obsproject.com/forum/resources/advanced-scene-switcher.395/) and scroll down to the "Installing the Plugin" section for your operating system.
3. Restart OBS.
4. Go to the Tools / Advanced Scene Switcher menu, and start the plugin if needed. ![AdvSceneSwitcher](img/OBS/AdvSceneSwitcher.png)

## Example: Switching at the end of a group

The following example is also available as a video demo: [Example video](https://user-images.githubusercontent.com/678663/147373848-89b91086-b16d-48c0-8f48-445f6c1ca828.mp4)

In this example, OBS is running on the machine that controls the attempt board.  We have two scenes defined.  One uses the browser source for the attempt board, the other shows a video. 

We define a "Group is done" macro as follows:

1. The top part is for the conditions that will trigger our scene change. We the status monitor window. 
   - The scene switcher checks every few hundred milliseconds for changes in the window titles.  
   - We want a match for `break=BREAK.GROUP_DONE.*$` 
     Note that `.*$` reads "anything up to the end".  We add this because otherwise the match would be on the exact title, and our title is longer (it includes the previous state and what platform we are on).
2. The bottom part is the sequence of actions we want
   - We wait a little while (we remain on our current scene "Attempt Board") 
   - and then switch to the video.

![example1](img/OBS/example1.png)

## Example: Switching on multiple conditions

We would like the display to switch back to the attempt board as soon as the announcer changes the state to "introduction" or when the timer for the start of competition is started.

We use an expression like `(state=.*$)|(break=INTRODUCTION.*$)|(break=BEFORE_.*$)` to detect the three interesting states, and switch back to the attempt board scene.

- Each of the three sets of parentheses describes a possible condition. 
- The `|` separates the possible alternatives
- As before, each condition ends with `.*$` to match anything up to the end of the line.

![example2](img/OBS/example2.png)

In actual life, you would probably consider BREAK_BEFORE as a separate conditions, and wait 8 minutes before switching, or do whatever you want.

## State Transition Sequences

Here is the full set of state transitions that can be expected over the course of a competition. The `fop=A` in the examples indicates what field of play (platform) on which the event took place.  If you have multiple platforms, you should start different status monitors,  one per platform.

1. Announcer selects "No Group"

```
break=INACTIVE;previous=INACTIVE;fop=A
```

2. Announcer goes through the Before intro, during intro, before first snatch sequence.
   Note that it is possible that the state=CURRENT_ATHLETE_DISPLAYED will be shown if the announcer exits the break management dialog

```
break=BREAK.BEFORE_INTRODUCTION;previous=INACTIVE;fop=A
break=BREAK.DURING_INTRODUCTION;previous=BREAK.BEFORE_INTRODUCTION;fop=A
break=BREAK.FIRST_SNATCH;previous=BREAK.DURING_INTRODUCTION;fop=A
state=CURRENT_ATHLETE_DISPLAYED;previous=BREAK.FIRST_SNATCH;fop=A
```

3. After the breaks, the status is CURRENT_ATHLETE_DISPLAYED (athlete information is on attempt board)

```
state=TIME_RUNNING;previous=CURRENT_ATHLETE_DISPLAYED;fop=A
state=TIME_STOPPED;previous=TIME_RUNNING;fop=A
state=DOWN_SIGNAL_VISIBLE;previous=TIME_STOPPED;fop=A
state=DECISION_VISIBLE.GOOD_LIFT;previous=DOWN_SIGNAL_VISIBLE;fop=A
state=CURRENT_ATHLETE_DISPLAYED;previous=DECISION_VISIBLE.GOOD_LIFT;fop=A
```

4. If jury deliberates and does nothing (after Jury, the state is back to CURRENT_ATHLETE_DISPLAYED)

```
break=BREAK.JURY;previous=CURRENT_ATHLETE_DISPLAYED;fop=A
state=CURRENT_ATHLETE_DISPLAYED;previous=BREAK.JURY;fop=A
```

5. New lift from athlete 

```
state=TIME_RUNNING;previous=CURRENT_ATHLETE_DISPLAYED;fop=A
state=TIME_STOPPED;previous=TIME_RUNNING;fop=A
state=DOWN_SIGNAL_VISIBLE;previous=TIME_STOPPED;fop=A
state=DECISION_VISIBLE.GOOD_LIFT;previous=DOWN_SIGNAL_VISIBLE;fop=A
state=CURRENT_ATHLETE_DISPLAYED;previous=DECISION_VISIBLE.GOOD_LIFT;fop=A
```

​	Jury deliberates and decides good or bad (DECISION_VISIBLE.BAD_LIFT or DECISION_VISIBLE.GOOD_LIFT)

```
break=BREAK.JURY;previous=CURRENT_ATHLETE_DISPLAYED;fop=A
state=DECISION_VISIBLE.BAD_LIFT;previous=BREAK.JURY;fop=A
state=CURRENT_ATHLETE_DISPLAYED;previous=BREAK.JURY;fop=A
```

6. Break after last lift in the snatch (is started by the announcer).

```
state=CURRENT_ATHLETE_DISPLAYED;previous=DECISION_VISIBLE.GOOD_LIFT;fop=A
break=BREAK.FIRST_CJ;previous=CURRENT_ATHLETE_DISPLAYED;fop=A
```

7. End of the clean & jerk break, followed by normal lifting sequence same as above

```
state=CURRENT_ATHLETE_DISPLAYED;previous=BREAK.FIRST_CJ;fop=A
```

8. End of group <u>variation A</u>: last lift of group with a normal referee decision. status goes to GROUP_DONE without CURRENT_ATHLETE_DISPLAYED.

```
state=CURRENT_ATHLETE_DISPLAYED;previous=BREAK.GROUP_DONE;fop=A
state=TIME_RUNNING;previous=CURRENT_ATHLETE_DISPLAYED;fop=A
state=TIME_STOPPED;previous=TIME_RUNNING;fop=A
state=DOWN_SIGNAL_VISIBLE;previous=TIME_STOPPED;fop=A
state=DECISION_VISIBLE.GOOD_LIFT;previous=DOWN_SIGNAL_VISIBLE;fop=A
break=BREAK.GROUP_DONE;previous=DECISION_VISIBLE.GOOD_LIFT;fop=A
```

​	Jury break works after last lift of group: it goes back to GROUP_DONE same as the last lift.

```
break=BREAK.JURY;previous=BREAK.GROUP_DONE;fop=A
state=DECISION_VISIBLE.BAD_LIFT;previous=BREAK.JURY;fop=A
break=BREAK.GROUP_DONE;previous=DECISION_VISIBLE.BAD_LIFT;fop=A
```

9. Variation B: Athlete withdraws and marshal/announcer enters 0 on athlete card.  There is no decision, the status goes directly to BREAK.GROUP_DONE (previous state does not matter)

```
break=BREAK.GROUP_DONE;previous=CURRENT_ATHLETE_DISPLAYED;fop=A 
```

10. Announcer goes to medals break. GROUP_DONE if the break is ended.

```
break=BREAK.MEDALS;previous=BREAK.GROUP_DONE;fop=A
break=BREAK.GROUP_DONE;previous=BREAK.MEDALS;fop=A 
```

11. Additional cases for records attempted or broken

```
state=CURRENT_ATHLETE_DISPLAYED.RECORD_ATTEMPT
state=DECISION_VISIBLE.GOOD_LIFT.NEW_RECORD
```

 