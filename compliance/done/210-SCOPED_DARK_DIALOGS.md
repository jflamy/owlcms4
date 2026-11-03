# Plan: Scope DARK theme to display-page dialogs, remove global Lumo theme manipulation

## Background / Problem

Scoreboard and attempt-board pages (litelement display pages) currently force the
Lumo DARK theme **globally**:

- `AbstractAttemptBoard.onAttach` sets `Lumo.DARK` on the **UI element** (the whole browser tab).
- 10 frontend JS components call `document.body.setAttribute("theme", "dark")` and never remove it.

This global state leaks: navigating from a display page back to a normal Vaadin page
(e.g. `/mobile/refjury`) leaves the header/layout in dark mode. A counter-hack
(`RefereeJuryStyles.applyLightTheme`) was added to the mobile launcher pages to mask this.

**Key fact (verified):** the display pages' own content does NOT use Lumo at all.
Scoreboard visuals come from user-overrideable CSS in `shared/src/main/resources/css/`
(zero `--lumo-*` references) driven by the `dark=`/`light` URL parameter and the
`dark`/`light` CSS class (`DisplayParameters.DARK`). The ONLY reason Lumo DARK was set
globally is so the click-to-open **settings dialog overlay** (a Vaadin `Dialog`) renders
dark instead of blinding white in a dark venue.

**Fix strategy:** apply the `dark` theme attribute to the `Dialog` component itself.
Vaadin Flow copies a Dialog's theme attribute to its overlay element, so the dialog
renders dark without touching the UI element or `document.body`. Then delete all
global theme manipulation, including the launcher counter-hack.

## Repository rules (must follow)

- Do NOT run `mvn` or any build. Validate with the editor's error reporting (`get_errors`) only.
- Do NOT use fully-qualified class names in Java code — add imports instead.
- Do NOT hardcode UI strings; do NOT touch `translation4.csv`.
- Do NOT run git commands.

## Changes

### 1. Force dark on the display settings dialog (single central place)

File: `owlcms/src/main/java/app/owlcms/apputils/queryparameters/DisplayParametersReader.java`

In `getDialogCreateIfMissing()` (near line 308), add the dark theme to the dialog:

```java
default Dialog getDialogCreateIfMissing() {
    if (getDialog() == null) {
        setDialog(new Dialog());
    }
    getDialog().setResizable(true);
    getDialog().setDraggable(true);
    // display pages are typically in dark venues; force the settings dialog dark
    // (the theme attribute is propagated by Flow to the dialog overlay)
    getDialog().getElement().getThemeList().add(Lumo.DARK);
    return getDialog();
}
```

Add import: `com.vaadin.flow.theme.lumo.Lumo`.

Why here: `buildDialog(...)` in this same interface calls `getDialogCreateIfMissing()`,
and every display page's settings dialog is created through this path (including
`TopTeamsPage` / `TopTeamsSinclairPage` which call `buildDialog(this)` directly).
One edit covers all scoreboards and attempt boards.

### 2. Force dark on the jury notification dialog on attempt boards

File: `owlcms/src/main/java/app/owlcms/displays/attemptboard/AbstractAttemptBoard.java`

In `ensureJuryNotificationDialog()` (near line 1127), the dialog previously inherited
dark from the global theme. Keep it dark explicitly:

```java
private Dialog ensureJuryNotificationDialog() {
    if (this.juryNotificationDialog == null) {
        this.juryNotificationDialog = new Dialog();
        this.juryNotificationDialog.addThemeName("jury-notification-dialog");
        this.juryNotificationDialog.addThemeName(Lumo.DARK);
        this.juryNotificationDialog.setCloseOnEsc(false);
        this.juryNotificationDialog.setCloseOnOutsideClick(false);
    }
    return this.juryNotificationDialog;
}
```

### 3. Remove the global UI-level DARK in AbstractAttemptBoard

Same file, `onAttach` (lines ~851–853). DELETE these three lines:

```java
ThemeList themeList = UI.getCurrent().getElement().getThemeList();
themeList.remove(Lumo.LIGHT);
themeList.add(Lumo.DARK);
```

Then remove the now-unused import `com.vaadin.flow.dom.ThemeList` (line ~33).
KEEP the `Lumo` import (still used by step 2). Verify with `get_errors` that no
import is left unused and nothing else broke.

### 4. Remove `document.body.setAttribute("theme", "dark")` from JS components

Delete every line containing `document.body.setAttribute("theme", "dark");`
(and any adjacent comment that only explains that line) from these files under
`owlcms/src/main/frontend/components/`:

| File | Occurrences |
|---|---|
| CurrentAthlete.js | 2 (≈ lines 144, 222) |
| Results.js | 1 (≈ line 363) |
| ResultsMedals.js | 1 (≈ line 233) |
| ResultsMulti.js | 1 (≈ line 436) |
| ResultsRankingsByCategory.js | 1 (≈ line 241) |
| ResultsStartList.js | 1 (≈ line 186) |
| TopSinclair.js | 2 (≈ lines 133, 158) |
| TopTeams.js | 1 (≈ line 127) |
| TopTeamsSinclair.js | 1 (≈ line 118) |

Line numbers are approximate — locate by searching for the exact string.
Do not change anything else in these files. Afterward, verify with a grep that
`setAttribute("theme", "dark")` no longer appears anywhere under
`owlcms/src/main/frontend/`. (Per-component theme attributes on buttons etc.,
like `theme="primary error"`, are unrelated — leave them alone.)

### 5. Remove the launcher counter-hack `applyLightTheme`

File: `owlcms/src/main/java/app/owlcms/nui/home/navigation/RefereeJuryStyles.java`

- Delete the `applyLightTheme(UI ui)` method entirely.
- Remove now-unused imports: `com.vaadin.flow.dom.ThemeList`,
  `com.vaadin.flow.theme.lumo.Lumo` (keep everything `ensureLoaded` needs).

Call sites — remove the `RefereeJuryStyles.applyLightTheme(ui);` line from
`onAttach(...)` in each of:

- `owlcms/src/main/java/app/owlcms/nui/home/navigation/RefereeNavigationContent.java`
- `owlcms/src/main/java/app/owlcms/nui/home/navigation/JuryNavigationContent.java`
- `owlcms/src/main/java/app/owlcms/nui/home/navigation/MobileScoreboardsNavigationContent.java`

Keep the `RefereeJuryStyles.ensureLoaded(ui);` calls — those load the CSS and are
unrelated to theming.

## Verification

1. Run `get_errors` on every modified Java file — must be clean (no unused imports,
   no unresolved symbols).
2. Grep checks (all must return nothing):
   - `document.body.setAttribute("theme"` under `owlcms/src/main/frontend/`
   - `applyLightTheme` anywhere under `owlcms/src/main/java/`
   - `getThemeList()` used on a **UI element** (`UI.getCurrent().getElement().getThemeList()`
     or `ui.getElement().getThemeList()`) anywhere under `owlcms/src/main/java/` —
     component-level `getThemeList()` (notifications, checkboxes, grids, dialogs) is fine.
3. Do NOT build. The app runs with DCEVM hot-reload; the human will test manually:
   - Open an attempt board, click it → settings dialog must be dark.
   - Open a scoreboard, click it → settings dialog must be dark.
   - Navigate scoreboard → back to `/mobile/refjury` → header and page must be light.
   - Scoreboard `dark=false` URL param must still switch the board itself to light
     (that mechanism is CSS-class based and untouched).

## Known follow-up to watch for (do not fix unless the human asks)

- Vaadin `Notification`s shown while on a display page were previously dark via the
  global theme; they will now render light. If that turns out to matter, the fix is
  `notification.getElement().getThemeList().add(Lumo.DARK)` at the creation site —
  but do not hunt for these preemptively.
- The `.referee-jury-home-displays` CSS block in
  `owlcms/src/main/resources/META-INF/resources/styles/referee-jury-home.css` is
  unused (buttons moved to the mobile scoreboards page); it may be deleted if asked.
