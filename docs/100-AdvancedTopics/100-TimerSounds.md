# Customizing the Timer Sounds

OWLCMS plays sounds at fixed milestones on two independent timers:

- the **athlete timer** (the 1:00 / 2:00 lifting clock)
- the **break timer** (introductions, breaks between snatch and clean & jerk, medal ceremonies, etc.)

Each timer has three sounds — an **initial warning**, a **final warning**, and a **time over** signal — and each can be pointed at a different audio file. Break timer sounds additionally require a feature switch.

Everything is configured in two places inside the `local` folder of your installed version:

- `local/sounds` — the audio files
- `local/timing/timing.properties` — which sound plays when

The worked example below changes the **break timer delays and sounds**, using three distinct sounds. The same steps apply to the athlete timer; just edit the `athleteTimer*` lines instead.

### Sounds shipped with OWLCMS

`local/sounds` already contains the following stock sounds, each as an `.mp3` and a `.wav`:

| Base name       | Used for                                                                 | Configurable in `timing.properties` |
| --------------- | ------------------------------------------------------------------------ | ----------------------------------- |
| `initialWarning` | Initial warning — default for both the athlete timer and the break timer  | yes                                 |
| `finalWarning`   | Final warning — default for both the athlete timer and the break timer    | yes                                 |
| `timeOver`       | Time over — default for both the athlete timer and the break timer        | yes                                 |
| `down`           | Down signal, when two referees agree on a good lift                       | no — replace the files to change it |
| `beepBeep`       | Notification beep (e.g. jury and other attention signals)                 | no — replace the files to change it |

You can either point the timing settings at new base names (as in the example above), or simply overwrite the stock files to change every place they are used.

## Prepare your sound files

Each sound needs **two files with the same base name**: an `.mp3` and a `.wav`.
The browser plays the `.mp3`; the server plays the `.wav` when sound is emitted on the server. If one is missing, the sound will be silent in that context.
To convert between the `.wav` and `.mp3` formats we suggest you use [VLC](https://www.videolan.org/).

As an example, we want to change the break timer files. We want three different break sounds, so prepare six files:

| Purpose               | Base name         | Files needed                                 |
| --------------------- | ----------------- | -------------------------------------------- |
| Break initial warning | `breakWarning`    | `breakWarning.mp3`, `breakWarning.wav`       |
| Break final warning   | `breakAlmostOver` | `breakAlmostOver.mp3`, `breakAlmostOver.wav` |
| Break time over       | `breakGong`       | `breakGong.mp3`, `breakGong.wav`             |

Base names are arbitrary — pick whatever you like, just keep the `.mp3` and `.wav` names identical.

## Stop OWLCMS first

1. Start **OWLCMS Control Panel** and click the **OWLCMS** tab.
2. **If OWLCMS is currently running**, click the red **Stop OWLCMS &lt;version&gt;** button.
3. A confirmation box appears: *"Stopping OWLCMS will stop the current competition on all platforms. Make sure this is a correct time to stop."* — click **Stop**.

Do this **before** editing anything. Two reasons:

- The version list with the **Files** button only appears when OWLCMS is stopped.
- `timing.properties` is read **once at startup**, so edits made while it runs have no effect anyway.

## Open the `local` folder

4. In the list of installed versions, find your version's row (e.g. `67.1.0`). Each row has: `Launch` · `Files` · `Update` · `Remove`.
5. Click **Files**.
   Explorer / Finder / your file manager opens on the version folder.

You should see `local`, `database`, `logs`, and `owlcms.jar`.

## Add the sounds and edit the timings

The Control Panel extracts the **complete** `local` folder, so `local/sounds` and `local/timing/timing.properties` already exist. You are editing them, not creating them.

6. Open **`local`** ▸ **`sounds`**. Copy in your six files:
   `breakWarning.mp3`, `breakWarning.wav`, `breakAlmostOver.mp3`, `breakAlmostOver.wav`, `breakGong.mp3`, `breakGong.wav`.
7. Back up to `local`, open **`timing`**, and edit the existing **`timing.properties`**.

Note that out of the box both timers point at the *same* three sounds (`initialWarning`, `finalWarning`, `timeOver`). That is why giving the break timer its own base names, as above, is what makes it sound different from the athlete clock. Conversely, if you simply overwrite `initialWarning.*`, `finalWarning.*` and `timeOver.*` in `local/sounds` and leave `timing.properties` alone, you change both timers at once.

In this example we change the break timers:  we also lengthen the break warnings: first warning at 2:00 instead of 1:00, final warning at 0:45 instead of 0:30. Times are in **milliseconds**; sound values are **base names with no extension**.

In the following, we only changed the break section.  The timings for the athlete are according to IWF TCRR rules, you should should not touch them.

```properties
# Athlete timer milestones (milliseconds)
athleteTimerTwoMinutes=120000
athleteTimerInitialWarning=90000
athleteTimerOneMinute=60000
athleteTimerFinalWarning=30000
athleteTimerInitialWarningSound=initialWarning
athleteTimerFinalWarningSound=finalWarning
athleteTimerTimeOverSound=timeOver

# --- Break timer: delays (milliseconds) and sounds (base names, no extension) ---
breakTimerInitialWarning=120000
breakTimerFinalWarning=45000
breakTimerInitialWarningSound=breakWarning
breakTimerFinalWarningSound=breakAlmostOver
breakTimerTimeOverSound=breakGong
```

## Clean up `local`

**Remove anything in `local` that you have not modified.**

If all you did was add your new sounds and edit the timing properties, then when you are done `local` should contain only:

```
local/
  sounds/
    breakWarning.mp3      breakWarning.wav
    breakAlmostOver.mp3   breakAlmostOver.wav
    breakGong.mp3         breakGong.wav
  timing/
    timing.properties     -> the file you edited
```

9. **Only keep in local what you have modified**
 Delete every other subfolder of `local` (`templates`, `styles`, `i18n`, etc.) you have not touched, and delete the untouched default files inside `sounds` that you did not replace.<br>
 Why: `local/` is an **override layer** — OWLCMS looks there first and then falls back to the original inside `owlcms.jar`.  On your next update the Control Panel treats those files as yours and carries them forward. So anything left in `local/` becomes a *frozen* copy that permanently overrides the current version's defaults, and you are responsible for making any required adjustments if OWLCMS evolves. Keep only what you actually changed so everything else tracks the new version automatically.

## Launch and enable the feature switch

Break timer sounds are off by default and need a feature switch. (Athlete timer sounds need no switch — if you only changed `athleteTimer*` settings, skip steps 12–13.)

10. In the Control Panel, click **Launch** on that version row.
11. Click **Open OWLCMS in a browser** once it appears.
12. Go to **Prepare Competition ▸ Language and System Settings**.
13. In **Feature Switches**, add `breakTimerSounds` (comma-separated if other switches are present) and save.

## Check it worked

- On the **Announcer** page, start a break with more than 2:00 remaining. You should hear `breakWarning` at 2:00, `breakAlmostOver` at 0:45, and `breakGong` at 0:00.
- If silent: **Help ▸ Show Control Panel Log** (or the "Tail logs" link) and look for lines like `timing override: breakTimerTimeOverSound = breakGong`. If those lines are missing, `timing.properties` is not where OWLCMS expects it.
- If a sound plays on some screens but not others, you are probably missing either the `.mp3` or the `.wav` for that base name.