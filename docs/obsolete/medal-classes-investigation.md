# Medal Classes Not Applied on `resultsRankingOrder` Page

## Problem

URL: `http://localhost:8080/displays/resultsRankingOrder?fop=A&currentAttempt=false&video=true&group=F3&showMedals=true`

When inspecting the DOM, no `.medal1` / `.medal2` / `.medal3` CSS classes appear on rank `<td>` elements.

## Root Cause

`resultsRankingOrder` uses `Results.js` which has **no medal logic at all**.

Medal-aware rendering only exists in `ResultsRankingsByCategory.js`, used by a different route.

## Two Parallel Template Hierarchies

### 1. `Results.js` (no medals)

- **Java component:** `ResultsRankingOrder extends Results` → `@Tag("results-template")` → `@JsModule("./components/Results.js")`
- **Page:** `WarmupRankingOrderPage` at route `displays/resultsRankingOrder`
- **Rank cells are static classes:**
  ```js
  <td class="rank">           // snatch rank
  <td class="rank">           // C&J rank
  <td class="totalRank">      // total rank
  <td class="sinclairRank">   // sinclair rank
  ```
- No `showMedals` property declared in `static get properties()`
- No `item?.snatchMedal`, `item?.totalMedal`, etc. referenced anywhere

### 2. `ResultsRankingsByCategory.js` (has medals)

- **Java component:** `ResultsRankingsByCategory` → `@Tag("results-by-category-template")` → `@JsModule("./components/ResultsRankingsByCategory.js")`
- **Page:** `PublicRankingOrderPage` at route `displays/publicRankingOrder`
- **Rank cells use conditional medal classes:**
  ```js
  <td class="${"rank " + ((this.showMedals === "true" || (this.showMedals === "auto" && mc.categoryDone))
      ? (item?.snatchMedal ?? "") : "")}">
  ```
- `showMedals` property declared: `showMedals: {type: String}`
- Medal fields used: `item?.snatchMedal`, `item?.cleanJerkMedal`, `item?.totalMedal`, `item?.sinclairMedal`

## Parameter Flow (working path, `ResultsRankingsByCategory` only)

```
URL ?showMedals=true
  → DisplayParametersReader.readParams()        parses param
    → switchShowMedals()                         updates URL, calls setShowMedals()
      → AbstractDisplayPage.setShowMedals()      stores field, delegates to board
        → BaseResults.setShowMedals()            getElement().setProperty("showMedals", value)
          → Lit property showMedals              received by ResultsRankingsByCategory.js
            → render()                           conditionally adds medal1/medal2/medal3 classes
```

## `WarmupRankingOrderPage` Also Missing `SHOW_MEDALS` Default

`WarmupRankingOrderPage.init()` does not include `DisplayParameters.SHOW_MEDALS` in its parameter map at all (compare with `PublicRankingOrderPage` which sets it to `"auto"`).

## What Would Need to Change

To add medal support to `Results.js` / `resultsRankingOrder`:

1. **Add `showMedals` property** to `Results.js` `static get properties()`:
   ```js
   showMedals: { type: String },
   ```

2. **Add conditional medal classes** to the four rank `<td>` elements in `Results.js` (lines ~132, 146, 153, 159), mirroring the pattern in `ResultsRankingsByCategory.js`. The challenge is that `Results.js` iterates a flat `athletes` array, not grouped by category with `mc.categoryDone` — so the `"auto"` mode logic would need adaptation.

3. **Add `SHOW_MEDALS` to `WarmupRankingOrderPage.init()`** default parameters.

4. **Verify Java side** sends medal fields (`snatchMedal`, `cleanJerkMedal`, `totalMedal`, `sinclairMedal`) in the athlete JSON for the `Results` component (needs checking in `BaseResults` athlete serialization).

## Key Files

| File | Role |
|------|------|
| `owlcms/src/main/frontend/components/Results.js` | Lit template — **no medal logic** |
| `owlcms/src/main/frontend/components/ResultsRankingsByCategory.js` | Lit template — **has medal logic** |
| `owlcms/src/main/java/app/owlcms/displays/scoreboard/ResultsRankingOrder.java` | Java component, uses Results.js |
| `owlcms/src/main/java/app/owlcms/nui/displays/scoreboards/WarmupRankingOrderPage.java` | Route `displays/resultsRankingOrder`, missing `SHOW_MEDALS` |
| `owlcms/src/main/java/app/owlcms/nui/displays/scoreboards/PublicRankingOrderPage.java` | Route `displays/publicRankingOrder`, has `SHOW_MEDALS` |
| `owlcms/src/main/java/app/owlcms/displays/scoreboard/BaseResults.java` | Pushes `showMedals` property to element |
| `owlcms/src/main/java/app/owlcms/apputils/queryparameters/DisplayParametersReader.java` | Reads `showMedals` from URL |
