---
name: generate-xlsx-review
description: "Use when: generating OWLCMS XLSX reports from a live Vaadin page for visual review, batch-downloading templates, avoiding native Save dialogs, capturing Vaadin DownloadHandler resources, validating generated workbooks, or preparing protocol/jury/results review folders. Keywords: XLSX review, visual check, report templates, Descargar, Save dialog, dynamic resource, workbook batch."
---

# Generate XLSX Review Workbooks

## Goal

Generate OWLCMS reports from the live authenticated browser session without opening the native Save dialog, save each workbook under an exact filename, and validate the resulting XLSX archives.

## Critical Rules

1. Use the currently active shared browser page. Hidden tabs often reject or defer Vaadin download clicks.
2. Generate one workbook per browser tool call. Do not batch several Vaadin dialogs in one Playwright call.
3. Start a dedicated receiver with an explicit output directory and an unused port.
4. Never click `Descargar` or an anchor with a `download` attribute. A trusted click can open the native Save dialog and cannot be safely intercepted by overriding `HTMLAnchorElement.prototype.click`.
5. Select templates by their exact underlying option text. Paper-size suffixes may be omitted in normal labels; inspect each option's trimmed `textContent` or `option.item.label`.
6. Vaadin's hidden `role=dialog` controller is not the visible dialog. Wait for `vaadin-dialog-overlay[opened]`, and do not wait for `getByRole('dialog')` to become visible.
7. Find the authenticated download anchor in the whole page document, not inside the overlay. Fetch its URL and POST the bytes to the receiver.
8. Validate every output before reporting completion.

## Start The Receiver

Run the bundled receiver asynchronously:

```bash
python3 .github/skills/generate-xlsx-review/scripts/receiver.py \
  --output /absolute/path/to/review-folder \
  --port 8766
```

Keep its terminal ID and stop it after validation.

## JXLSDownloader Procedure

Use this procedure for result pages and other dialogs backed by `app.owlcms.components.JXLSDownloader`. Vaadin 25 `DownloadHandler` creates an `Anchor` whose relative `href` resembles `VAADIN/dynamic/resource/...`. The anchor is attached to the page's light DOM and is not a descendant of `vaadin-dialog-overlay`. Selecting a template replaces the anchor with a fresh one-time resource URL.

**Do not click `Descargar`.** A Playwright click is a trusted user click; overriding `HTMLAnchorElement.prototype.click` does not intercept it and the native Save dialog can open.

Perform this sequence separately for each workbook.

1. Confirm no stale dialog is open. Press Escape once if needed.
2. Open the report dialog from the exact button.
3. Wait for the visible overlay, not the hidden dialog controller:

```javascript
await page.locator('vaadin-dialog-overlay[opened]').last().waitFor({ state: 'visible' });
```

4. Use the last visible ComboBox after the overlay opens. Open it and inspect exact underlying options:

```javascript
const combo = page.locator('vaadin-combo-box:visible').last();
await combo.click();
const options = await page.getByRole('option').evaluateAll(items =>
  items.map(item => item.textContent?.trim() || item.item?.label)
);
```

5. Select the exact filename with `getByRole('option', { name: filename, exact: true })`.
6. Wait briefly for JXLSDownloader to replace the anchor, then verify the selected option through its `aria-selected` state or the ComboBox item key.
7. Read the generated URL from the whole document without clicking the button or anchor:

```javascript
const anchor = page.locator(
  'a[download][href*="VAADIN/dynamic/resource/"]:visible'
).last();
await anchor.waitFor({ state: 'visible' });
const href = await anchor.evaluate(link => link.href);
```

8. Fetch and relay the workbook immediately. Dynamic-resource URLs are one-time resources:

```javascript
const result = await page.evaluate(async ({ href, filename, port }) => {
  const response = await fetch(href);
  const body = await response.arrayBuffer();
  const receiver = await fetch(`http://127.0.0.1:${port}/`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'X-Filename': filename,
    },
    body,
  });
  return {
    resourceStatus: response.status,
    bytes: body.byteLength,
    receiverStatus: receiver.status,
  };
}, { href, filename, port });
```

Success requires resource status `200`, receiver status `204`, and a plausible nonzero byte count.

## Recovery

- Native Save dialog appeared: dismiss it manually and stop. Do not retry with a click interceptor; restart from a clean dialog and read the anchor URL directly.
- No anchor URL: do not click `Descargar`. Verify the page is the active shared tab, exactly one overlay is open, the template selection completed, and search the whole document for `a[download]`. If no URL exists, inspect the owning Java download implementation before proceeding.
- Multiple overlays: press Escape, wait for closing overlays, then reopen one clean dialog.
- HTTP 404: the one-time resource was already consumed. Regenerate it; do not reuse the URL.
- Paper suffix missing: inspect the option's underlying `innerText`/`item.label` and validate workbook page metadata rather than trusting the visible ComboBox label.

## Validate Outputs

Run:

```bash
python3 .github/skills/generate-xlsx-review/scripts/validate_xlsx.py \
  /absolute/path/to/review-folder
```

Confirm the expected file count and filenames separately. For task-specific semantics, add focused assertions for headers, records, blank fields, officials, or paper sizes.

## Finish

Stop the dedicated receiver, report the exact review directory and filenames, and mention any templates intentionally excluded from visual review.
