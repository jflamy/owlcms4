---
name: scoreboard-design
description: "Use when: designing, styling, or refining OWLCMS scoreboards, attempt boards, medal/ranking boards, or transparent video overlays — color selection, contrast, typography, tabular-nums, anti-clutter layout, and motion hygiene. Keywords: scoreboard design, color palette, contrast, typography, tabular-nums, CSS theme, attempt board, medal board, transparent overlay, broadcast graphics."
license: Apache-2.0
metadata:
  adapted_from: "google-gemini/demolab skills/gemini-frontend-design (Apache-2.0)"
---

# Scoreboard Design (OWLCMS)

Adapted from `google-gemini/demolab`'s `gemini-frontend-design` skill (Apache-2.0),
keeping only the visual-design, color, typography, layout, and motion rules that apply to
competition scoreboards and broadcast displays.

## 1. Color Selection & Contrast

- **Honor explicit federation/event colors first**: When a competition, federation, or
  broadcast brief specifies brand colors, build the palette around them cleanly.
- **Avoid generic AI defaults**: Do not default automatically to warm beige/terracotta or
  dark-slate + neon-purple glow. When choosing a fresh palette, derive it from the venue
  and broadcast environment: deep ink, marine, forest, or charcoal darks for arena LED
  walls and dim halls; crisp high-contrast light surfaces for bright venues; or bold
  chromatic color-blocking anchored to the event identity.
- **Tokenized palette + one accent**: Respect existing OWLCMS theme variables in
  `shared/src/main/resources/css/public/colors.css` (`.wrapper`, `.dark`, `.light`). When
  defining a new theme or board surface, define explicit CSS custom properties for:
  - primary surface and subtle secondary surface / row banding
  - primary text and muted metadata text (meeting WCAG AA `>= 4.5:1` contrast)
  - one deliberate brand/header accent
- **Reserve semantic colors for competition state**: Keep high-chroma status colors
  strictly for weightlifting state — good lift, fail/no lift, current lifter, next lifter,
  requested/pending weight, referee/jury decision lights, review warning, record attempt,
  and medal ranks. Do not reuse those status colors for decorative accents.
- **Transparent / OBS video overlays**: Files under `shared/src/main/resources/css/transparent/`
  must `@import` their `css/public/` counterpart and add only minimal transparent-background
  overrides. Ensure strong figure-ground separation over live camera feeds without muddy
  full-screen dark scrims.

## 2. Typography & Numeric Stability

- **At most 2 font families**: Use either 1 versatile sans family across weights, or a
  deliberate 2-font pairing (e.g., a condensed/display face for headers and weights plus a
  clean sans for athlete names and tables).
- **Use `tabular-nums`, never a 3rd monospace font**: Set
  `font-variant-numeric: tabular-nums` on timers, weights, start numbers, birth years,
  ranks, totals, and points so columns align vertically and countdown clocks never jitter.
- **Strict size-step discipline**: Keep the board to at most 3–4 consistent font-size
  steps (for example: hero clock/attempt weight, athlete row text, column headers/secondary
  fields, compact auxiliary metadata). Differentiate hierarchy via **weight, family, and
  color** rather than inventing arbitrary intermediate pixel steps.
- **Distance legibility**: Tighten tracking slightly (`-0.02em` to `-0.05em`) only on large
  display numbers/titles where it improves compactness; avoid tiny micro-text that fails at
  venue or 1080p stream distance.

## 3. Layout & Anti-Clutter Discipline

- **Live data immediately**: Scoreboards open directly into the competition state on a
  fixed `1920x1080` canvas (or configured `--forcedScreenAspectRatio`) with zero wasted
  header chrome and zero unintended scrollbars.
- **No decorative UI chrome**:
  - Do not add eyebrow kickers above board titles, decorative non-interactive chips/pills,
    decorative numbering (`01`, `02`), or fake `"LIVE"` telemetry dots.
  - Reserve bordered boxes, pills, or high-contrast fills strictly for genuine competition
    state (start number box, good/fail attempt cells, current/next lifter highlight, record
    notifications, medal highlights, decision lights).
- **No nested card-in-card cages**: Separate major board bands (video header, current
  attempt bar, results/lifting grid, records footer) with whitespace, `1px` dividers, or
  subtle surface tint rather than stacked bordered boxes inside boxes.
- **Translations**: Never hard-code visible labels; use OWLCMS's translation system
  (`Translator.translate(...)`).

## 4. Motion & Transition Hygiene

- **Brisk, explicit transitions**: Keep state transitions quick (`140ms–240ms`, smooth
  ease-out such as `cubic-bezier(0.22, 1, 0.36, 1)`; never `ease-in` and never
  `transition: all`). Name the exact properties being animated (`transform`, `opacity`,
  `clip-path`, or specific color/background tokens).
- **No distracting idle motion**: Avoid continuous bobbing, pulsing, or ambient glow loops
  that compete with the platform or countdown clock.
- **Safe rest state & reduced motion**: Animated elements must always settle cleanly at
  full visibility (never stuck at `opacity: 0`) and honor
  `@media (prefers-reduced-motion: reduce)`.

## 5. Pre-Delivery Visual Check

1. Audit CSS and markup:
   - [ ] Palette uses tokens (`colors.css` / CSS custom properties), avoids generic beige or
         dark-neon defaults unless requested, and keeps WCAG AA `>= 4.5:1` contrast.
   - [ ] Semantic competition colors are not diluted by decorative accents.
   - [ ] At most 2 font families; `font-variant-numeric: tabular-nums` on all numbers/timers;
         at most 3–4 consistent font-size steps.
   - [ ] Zero decorative chips, eyebrow kickers, fake `"LIVE"` indicators, or nested
         card-in-card borders.
   - [ ] Explicit CSS transition properties only (no `transition: all`) and
         `prefers-reduced-motion` support.
2. Verify visually at `1920x1080`:
   - Run the scoreboard rendering checks (`test-scoreboards-visually`:
     `node playwright/scoreboards/run.cjs`) and/or inspect in the built-in browser using
     `open-browser-1920x1080`.
   - Check light, dark, and transparent appearances for row/cell wrapping, clipped names or
     weights, contrast, and zero horizontal or vertical overflow.
