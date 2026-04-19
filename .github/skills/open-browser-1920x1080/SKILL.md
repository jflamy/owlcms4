---
name: open-browser-1920x1080
description: "Use when: opening the built-in browser for Playwright inspection, screenshot review, layout debugging, responsive verification, or DOM introspection where the page must reflect a 1920x1080 desktop viewport. Keywords: built-in browser, Playwright, viewport, 1920x1080, 1080p, screenshot, inspect layout, open browser page."
---

# Open Browser 1920x1080

This skill standardizes how to open and validate the built-in browser for Playwright-based inspection in this workspace.

## Scope

Use this skill whenever browser inspection, screenshots, layout checks, or DOM analysis depend on a stable desktop viewport.

## Goal

Ensure the page is inspected in a verified 1920x1080 viewport instead of relying on the built-in browser's previous window state or cached layout assumptions.

## Core Rule

Do not trust the built-in browser's default size.

Open a fresh page when practical, set the viewport to 1920x1080 with Playwright immediately, verify the resulting metrics, and only then inspect or screenshot the page.

## Default Workflow

1. Prefer a fresh page rather than reusing an existing browser tab with unknown viewport state.
2. Open a new browser page.
3. Immediately set the viewport with Playwright to width `1920` and height `1080`.
4. Verify the effective metrics before making visual or DOM conclusions.
5. If the target page was opened before the viewport was set, reload it after resizing.
6. Only then continue with `read_page`, screenshots, clicks, or deeper Playwright inspection.

## Preferred Sequence

If possible:

1. Open a fresh page to the target URL.
2. Run Playwright code to set the viewport.
3. Reload the page once.
4. Verify viewport and window metrics.

If the page is already open and must be reused:

1. Set the viewport to `1920x1080`.
2. Reload the page.
3. Verify metrics.
4. Continue inspection only after verification.

## Standard Playwright Snippet

Use a snippet equivalent to this with `run_playwright_code`:

```javascript
await page.setViewportSize({ width: 1920, height: 1080 });
return {
  viewport: page.viewportSize(),
  metrics: await page.evaluate(() => ({
    innerWidth: window.innerWidth,
    innerHeight: window.innerHeight,
    outerWidth: window.outerWidth,
    outerHeight: window.outerHeight,
    devicePixelRatio: window.devicePixelRatio
  }))
};
```

## Verification Rule

Treat `window.innerWidth` and `window.innerHeight` as the important layout confirmation.

What matters for most UI debugging is:

- viewport width is `1920`
- viewport height is `1080`
- page layout is evaluated after the resize and reload

Do not assume the browser window chrome size or monitor pixel density matters for ordinary scoreboard layout checks.

## Screenshots

- Capture screenshots only after viewport verification.
- Prefer a fresh screenshot after resize and reload.
- If a screenshot looks inconsistent with expected desktop layout, re-check `window.innerWidth` and `window.innerHeight` before drawing conclusions.

## Practical Notes

- The built-in browser may preserve prior state between tabs or sessions.
- A page can remain visually stale if opened before the viewport is corrected.
- Reload after `setViewportSize` when first paint or responsive layout matters.
- For CSS debugging, viewport correctness and cache correctness are separate concerns.

## Expected Output

When using this skill:

- open the built-in browser in a controlled way
- force a `1920x1080` viewport explicitly
- verify the effective layout metrics
- inspect or screenshot only after the page reflects that viewport

## Do Not

- Do not assume the built-in browser opened at desktop size already.
- Do not inspect responsive behavior before verifying viewport metrics.
- Do not rely on screenshot appearance alone when layout is in question.
- Do not reuse an old page with unknown viewport state if a fresh page is practical.
- Do not conflate cache invalidation with viewport configuration.