---
name: generate-xlsx-review
description: "Use when: generating OWLCMS XLSX reports from a live Vaadin page for visual review, batch-downloading templates, avoiding native Save dialogs, capturing VAADIN dynamic resources, validating generated workbooks, or preparing protocol/jury/results review folders. Keywords: XLSX review, visual check, report templates, Descargar, Save dialog, dynamic resource, workbook batch."
---

# Generate XLSX Review Workbooks

## Goal

Generate OWLCMS reports from the live authenticated browser session without opening the native Save dialog, save each workbook under an exact filename, and validate the resulting XLSX archives.

## Critical Rules

1. Use the currently active shared browser page. Hidden tabs often reject or defer Vaadin download clicks.
2. Generate one workbook per browser tool call. Do not batch several Vaadin dialogs in one Playwright call.
3. Start a dedicated receiver with an explicit output directory and an unused port.
4. Determine which download implementation the page uses before clicking anything.
5. Select templates by their exact underlying option text. Paper-size suffixes may be omitted in normal labels; inspect each option's `innerText` or `option.item.label`.
6. Never press a Download button or hidden download anchor when its authenticated dynamic-resource URL is already present. Fetch the URL and POST the bytes to the receiver.
7. Validate every output before reporting completion.

## Start The Receiver

Run the bundled receiver asynchronously:

```bash
python3 .github/skills/generate-xlsx-review/scripts/receiver.py \
  --output /absolute/path/to/review-folder \
  --port 8766
```

Keep its terminal ID and stop it after validation.

## JXLSDownloader Procedure

Use this procedure for result pages and other dialogs backed by `app.owlcms.components.JXLSDownloader`. In these dialogs, selecting a template creates an `Anchor` whose `href` is already the authenticated dynamic-resource URL. **Do not click `Descargar`.** Clicking it invokes the native Save dialog.

Perform this sequence separately for each workbook.

1. Confirm no stale dialog is open. Press Escape once if needed.
2. Open the report dialog from the exact button.
3. Open its ComboBox and inspect underlying options when labels are ambiguous:

```javascript
await page.getByRole('option').evaluateAll(options =>
  options.map(option => option.innerText || option.item?.label)
);
```

4. Select the exact filename and wait for the refreshed active overlay.
5. Verify the ComboBox contains the exact expected filename.
6. Read the generated URL without clicking the button or anchor:

```javascript
const href = await page.locator('vaadin-dialog-overlay[opened]').last().evaluate(dialog => {
  const links = [];
  const walk = root => {
    for (const element of root.querySelectorAll('*')) {
      if (element.shadowRoot) walk(element.shadowRoot);
      if (element.tagName === 'A' && (element.href || '').includes('/VAADIN/dynamic/resource/')) {
        links.push(element.href);
      }
    }
  };
  walk(dialog);
  return links.at(-1) || null;
});
```

7. Fetch and relay that URL immediately using the fetch/receiver snippet below.

## Click-Generated Procedure

Use this only for dialogs that do not contain a dynamic-resource anchor after template selection, such as download flows where the anchor is created only after pressing the action button.

Perform this sequence separately for each workbook.

1. Confirm no stale dialog is open. Press Escape once if needed.
2. Install the interceptor before opening the report dialog:

```javascript
await page.evaluate(() => {
  globalThis.__capturedDownloadHref = null;
  if (!globalThis.__owlcmsDownloadInterceptor) {
    const originalClick = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function () {
      if ((this.href || '').includes('/VAADIN/dynamic/resource/')) {
        globalThis.__capturedDownloadHref = this.href;
        return;
      }
      return originalClick.call(this);
    };
    globalThis.__owlcmsDownloadInterceptor = true;
  }
});
```

3. Open the report dialog from the exact button.
4. Open its ComboBox and inspect underlying options when labels are ambiguous:

```javascript
await page.getByRole('option').evaluateAll(options =>
  options.map(option => option.innerText || option.item?.label)
);
```

5. Select the exact filename.
6. Wait for the refreshed active overlay and verify its ComboBox value.
7. Reset `globalThis.__capturedDownloadHref`.
8. Click the enabled `Descargar` button once only after confirming no dynamic-resource anchor already exists.
9. Wait for `__capturedDownloadHref` to become non-null.
10. Fetch and relay the workbook:

```javascript
const result = await page.evaluate(async ({ filename, port }) => {
  const response = await fetch(globalThis.__capturedDownloadHref);
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
}, { filename, port });
```

Success requires resource status `200`, receiver status `204`, and a plausible nonzero byte count.

## Recovery

- Native Save dialog appeared: dismiss it manually, restore/share the live page, and retry after installing the interceptor before opening the report dialog.
- No captured URL: verify the page is the active shared tab, the Download button is enabled, and only one opened dialog exists.
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
