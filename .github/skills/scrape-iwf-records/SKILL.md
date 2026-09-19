---
name: scrape-iwf-records
description: "Use when: scraping or updating IWF world records, running the IWF record scraper, obtaining current IWF records through Cloudflare, or generating an owlCMS IWF records XLSX. Keywords: IWF record scraper, world records, approved browser tab, Cloudflare, scrape IWF, IWF XLSX."
---

# Scrape IWF World Records

## Goal

Scrape current IWF records from the user-approved VS Code integrated browser tab and generate an owlCMS-format XLSX workbook.

## Critical Boundary

Only the VS Code browser tool can control the approved tab and reuse its Cloudflare session. Do not launch Selenium, standalone Playwright, Chrome, or a persistent browser profile. Those are separate sessions and will be blocked.

The browser JavaScript performs the scrape. Python only receives its JSON and converts it to XLSX.

## Procedure

1. Open `https://iwf.sport/results/world-records/` with `open_browser_page`, reusing a shared page when available.
2. Inspect the page. If `#ranking_curprog` is absent, ask the user to complete Cloudflare verification in the visible VS Code tab. Continue only after that control exists.
3. Start the one-shot receiver asynchronously from the repository root:

```bash
python3 tools/records/receive_iwf_browser_records.py \
  tools/records/iwf_records_browser.json \
  --port 8765
```

4. Read [scrape-approved-page.js](./scripts/scrape-approved-page.js) in full and pass its contents as the `code` argument to `run_playwright_code` for the approved page ID. Do not translate, reconstruct, or run it with Node.js. It relies on the browser page's authenticated `fetch`.
5. Require the browser result to report a positive record count and counts for every age-group/gender combination shown by the page.
6. Confirm the receiver reports that `tools/records/iwf_records_browser.json` was written. A browser `net::ERR_ABORTED` event can accompany the opaque `no-cors` response; receiver completion is authoritative.
7. Convert the JSON:

```bash
python3 tools/records/scrape_iwf_records_playwright.py \
  tools/records/iwf_records_browser.json \
  --output-dir tools/records
```

8. Validate the generated workbook with OpenPyXL. Require one sheet per scraped age-group/gender combination, three lifts per bodyweight category, and a total workbook row count equal to the browser result.
9. Report the exact XLSX path and record counts.

## Recovery

- `HTTP 403` or Cloudflare page: the shared tab is not approved. Stop and let the user approve it.
- Receiver port already in use: stop the stale receiver or choose another port, then change the port in a temporary copy of the browser code passed to the tool.
- Receiver never completes: keep the approved tab open, restart the receiver, and rerun the saved browser JavaScript.
- Do not fall back to Selenium or a separate browser process.
