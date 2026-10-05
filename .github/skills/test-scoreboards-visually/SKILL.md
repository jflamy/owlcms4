---
name: test-scoreboards-visually
description: "Use when: visually reviewing OWLCMS scoreboards, validating regular, ranking, or medal boards, scoreboard columns, category boundaries, medal ranks, score-medal layouts, public/no-grid themes, dark/light appearances, or opening the fixture tabs. Keywords: scoreboard visual test, scoreboard fixture, regular scoreboard, ranking scoreboard, Q-Masters, GAMX-U, medal score, medal rank, Playwright scoreboard."
---

# Test Scoreboards Visually

Use the checked-in rendering fixture under `playwright/scoreboards/`. It loads the
real scoreboard web components and real CSS, but supplies deterministic athletes
without starting OWLCMS or creating Vaadin objects.

This suite is intentionally separate from Java `AllTests`.

## Quick recipe (scripted)

Two commands, no browser tools needed. Both exit non-zero on failure.

```bash
# 1. Fixture-data tests (seconds, no browser)
node --test playwright/scoreboards/fixtures.test.mjs

# 2. Full rendering matrix in headless Chrome (about a minute)
node playwright/scoreboards/run.cjs
```

`run.cjs` starts the fixture server on a free port, launches the installed Chrome
headless with a throw-away profile, drives it over the DevTools protocol using
Node's built-in `WebSocket`, runs `runMatrix()` at 1920x1080, prints
`N passed, M failed`, and cleans up. On failure it prints the exact case, e.g.
`{"scenario":"masters","board":"multi",...}: Athlete cells wrapped into different grid rows`.

Narrow the matrix with comma lists:

```bash
node playwright/scoreboards/run.cjs --boards simple,ranking-medals --scenarios total
node playwright/scoreboards/run.cjs --themes public --appearances light --best 00,11
```

Axes: `--scenarios`, `--boards`, `--themes`, `--appearances`, `--best` (pairs of
`0`/`1` for best-score,best-rank). Chrome is found at the usual install paths;
override with `--chrome <path>` or `CHROME_PATH`.

The remaining sections are for interactive human review with the built-in browser.

## Start the fixture server

Run this as an attached asynchronous process:

```bash
node playwright/scoreboards/server.cjs --port 60745
```

Confirm it responds:

```bash
curl --fail --silent --output /dev/null --max-time 5 http://127.0.0.1:60745/
```

Do not install a second Playwright package. Browser automation is supplied by the
VS Code browser tools. The fixture reuses the existing frontend dependencies under
`owlcms/node_modules`.

## Open the human-review tabs

The purpose is to check scoreboard **layouts**: simple, ranking, with medals and
without. The `board` parameter picks the layout; each maps to a production
renderer:

| `board` | Label | Production | Medals highlighted |
| --- | --- | --- | --- |
| `simple` (default) | Simple scoreboard — without medals | `Results` | never |
| `ranking` | Ranking scoreboard — without medals | `ResultsRankingOrder`, `showMedals=false` / auto with category in progress | no |
| `ranking-medals` | Ranking scoreboard — with medals | `ResultsRankingOrder`, `showMedals=true` / auto with category done | yes |
| `multi` | Multi-category scoreboard — without medals | `ResultsMulti` | never |
| `medals` | Medals board — with medals | `ResultsMedals` | always |

Open the four layouts for the Total session side by side:

```text
http://127.0.0.1:60745/?scenario=total&board=simple
http://127.0.0.1:60745/?scenario=total&board=ranking
http://127.0.0.1:60745/?scenario=total&board=ranking-medals
http://127.0.0.1:60745/?scenario=total&board=medals
```

Then repeat for the score-based sessions by changing `scenario` to `masters`,
`scores`, or `mixed`. The `scenario` is the set of championships on display and
therefore the ranking rules; it never decides whether medals are highlighted.

Immediately set each page to a 1920x1080 viewport and reload it, following the
`open-browser-1920x1080` skill. Wait for `window.fixtureReady === true` with
`{ polling: 100 }`: Playwright's default `waitForFunction` polling uses
`requestAnimationFrame`, which never fires on a tab that is not visible, so the
default times out on every background tab even though the page has rendered.

Open tabs one at a time. Parallel `openBrowserPage` calls against the fixture
server intermittently fail with `ERR_FAILED`.

The `theme` parameter selects the production stylesheet family:

- `theme=nogrid` (default) is the **normal scoreboard** (`css/nogrid`).
- `theme=public` is the **public scoreboard** (`css/public`).

The medal-rank column is always present on every layout; the rank *rules* come from the
championships of the categories on display, not from the board.

Every tab title includes both the board and session, for example:

- `Fixture: Simple scoreboard — without medals — F 64 — ranked by Total`
- `Fixture: Ranking scoreboard — with medals — F 64 — ranked by Total`

The controls are hidden by default so the fixture matches a scoreboard display.
Use `Show fixture controls` in the lower-left corner when switching:

- Layout (board) as listed above
- Championships on display (session)
- Normal (nogrid) and public stylesheets
- Dark and light appearances
- Best-athlete score and rank independently
- Body weight column (`bodyWeight=true` in the URL)
- Optional category headers on boards where they are configurable

## Run deterministic fixture tests

The fixture-data tests need no browser:

```bash
node --test playwright/scoreboards/fixtures.test.mjs
```

These validate the modeled competition distinctions and required layout
properties. They do not replace browser rendering checks.

## Run the browser matrix

On one fixture page, use `runPlaywrightCode` with:

```javascript
await page.setViewportSize({ width: 1920, height: 1080 });
await page.reload();
await page.waitForFunction(() => window.fixtureReady === true, null, { polling: 100 });
return await page.evaluate(async () => {
  const { runMatrix } = await import('/fixture/checks.mjs');
  return runMatrix();
});
```

The full matrix covers:

- 4 sessions (championship ranking systems on display)
- 5 layouts: simple, ranking without medals, ranking with medals, multi-category,
  medals board
- 2 stylesheet families (normal `nogrid`, public `public`)
- 2 appearances
- 4 best-athlete score/rank visibility combinations
- body weight column off and on

That is 640 rendered cases. The check restores the tab's original controls after
completion.

## What the browser assertions verify

- Total is always present.
- The simple scoreboard renders without ranking-order mode and never highlights.
- Ranking scoreboards render with ranking-order mode and category headers; only
  the with-medals variant carries `medalN` highlight classes.
- The medals board always highlights; the multi-category board never does.
- Simple Total fixtures preserve start order while ranking fixtures order the
  same athletes by medal rank.
- Snatch and clean-and-jerk rank columns follow the medal policy: shown only when
  a displayed championship awards lift medals (`total`, `mixed`); hidden for
  score-only sessions (`masters`, `scores`), whose policy is forced to Total-only.
- There is one shared medal-rank column.
- There is at most one additional medal-score column.
- Total-medal athletes have a blank medal score in mixed sessions.
- Mixed score systems use the generic `Score` heading.
- Medal rank can differ from total/lift rank.
- Best-athlete score and rank are separate optional columns.
- Mixed medal systems use distinct registration categories and rank each category
  independently.
- Category boundaries are rendered.
- Vertical gutters are blank and borderless.
- No-grid receives the required filler and footer-row layout properties.
- Rows do not collapse, stretch, or wrap into mismatched grid tracks.
- Body weight has its own column, visible only when enabled, and is never written
  into `custom1`; `custom1` stays hidden unless a stylesheet enables it.

## Focused checks

To shorten a diagnosis, pass selected axes:

```javascript
return await page.evaluate(async () => {
  const { runMatrix } = await import('/fixture/checks.mjs');
  return runMatrix({
    scenarios: ['mixed'],
    boards: ['simple', 'ranking-medals'],
    themes: ['nogrid'],
    appearances: ['dark'],
    bestColumns: [[false, false]]
  });
});
```

Do not accept screenshots alone as proof. Run the DOM/layout assertions, then use
the four tabs for human cross-checking.

## Maintaining the suite

- Put domain fixture changes in `fixtures.mjs`.
- Put DOM and computed-layout assertions in `checks.mjs`.
- Keep the local server narrowly allowlisted; do not expose arbitrary repository
  files.
- Use the production components and styles. Never add fixture-only CSS to conceal
  a production rendering defect.
- When adding a new renderer or scenario, add it to the exported fixture constants,
  extend `fixtures.test.mjs`, and include it in the browser matrix.
- Keep fixture athletes distinct when categories or championships differ.
- Preserve score-versus-rank disagreements in fixture data because they detect
  accidental reuse of total or lift ranks as medal ranks.
