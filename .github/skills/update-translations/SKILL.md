---
name: update-translations
description: "Use when: adding new translation keys, revising existing translations, preparing translation update files for owlcms4, or deciding how to modify translation4.csv without editing it directly. Keywords: translation, translation4.csv, i18n, TSV, localization, add translation key, update translation text."
---

# Update Translations

This skill prepares translation updates for the `owlcms4` repository.

## Scope

Use this skill only for translation work in `owlcms4`.

## Canonical Reference File

The reference file is:

- `shared/src/main/resources/i18n/translation4.csv`

This file is never edited directly.

## Goal

Prepare a `.tsv` file containing only the translation rows to add or revise, using the exact same header order as `translation4.csv`.

This includes UI wording changes such as revising the text of an existing button, label, tooltip, or menu item.

## Workflow

1. If the task is revising existing UI wording, find the translation key in the source code first.
2. Read the first line of `shared/src/main/resources/i18n/translation4.csv`.
3. Copy that header exactly, preserving the language column order.
4. Create a new `.tsv` file in `shared/src/main/resources/i18n/`.
5. Add only the keys that are being added or updated.
6. Put the proposed translations in the `.tsv` rows.
7. Leave `translation4.csv` unchanged.

If the task is to change existing UI wording, update the existing translation key row in the `.tsv` file unless the wording change represents a genuinely different concept that requires a new key.

## Translator Class

The `Translator` class used by `owlcms4` is located here:

- `shared/src/main/java/app/owlcms/i18n/Translator.java`

Do not look for `Translator` under `owlcms/src/main/java`; the implementation lives in the shared module and is imported as `app.owlcms.i18n.Translator`.

Use the Java or frontend source that renders the UI to identify the exact translation key before preparing a TSV row.

Common usage patterns:

- `Translator.translate("Key")` for the normal UI translation lookup
- `Translator.translate("Key", params...)` when the UI string uses parameters
- `Translator.translateExplicitLocale("Key", locale)` when code pins a specific locale
- `Translator.translateNoOverrideOrElseNull("Key", locale)` when code wants a nullable result instead of fallback markup
- `Translator.translateOrElseEn("Key", locale)` when code explicitly falls back to English

When updating wording, derive the key from the actual `Translator.*` call in code rather than guessing from the displayed English text.

## TSV Rules

- The file must be tab-delimited, not comma-delimited.
- The header must exactly match the header from `translation4.csv`.
- Include one row per translation key being added or edited.
- Use the existing key if revising a translation.
- Use a new key only when the source text represents a genuinely new concept.
- Keep unrelated translation keys out of the `.tsv` file.
- Leave regional-variant locale columns blank by default.
- Exception: provide translations for `es_ES`, `es_SV`, `es_EC`, and `zh_HANT` when those columns are present.
- Keep `es_419` blank.
- Leave fake language columns such as `ia` and `io` blank if they are present in the header.
- Leave the last two named columns blank.
- Button text changes such as revising wording to `Keep Latest Official Records` are translation-update tasks and should normally reuse the existing key.
- When revising existing UI wording, derive the key from the UI code that renders the text, label, tooltip, or menu item rather than guessing the key name.

## Translation Writing Rules

- Use Olympic Weightlifting terminology.
- Use competition management software terminology where applicable.
- Follow the capitalization rules of each language.
- Prefer terminology already used elsewhere in the project when the same concept already exists.
- Keep wording consistent across related keys.
- Avoid overly literal translations when domain terminology has a standard accepted form.

## Repository-Specific Notes

- `shared/src/main/resources/i18n/translation4.csv` is managed externally and must not be edited by hand.
- The `.tsv` file is the proposal file for human review and later import into the translation workflow.
- The runtime translation helper class is `shared/src/main/java/app/owlcms/i18n/Translator.java`.
- If the task is only to add or change a few keys, create a focused `.tsv` file rather than a broad export.
- If translation certainty is low for some languages, preserve the row structure and use the best domain-consistent wording available for review.
- Wording refinements to existing UI strings should generally be treated as translation revisions, not as a reason to proliferate near-duplicate keys.
- For UI wording updates, inspect the button, label, tooltip, or menu definition in Java or frontend code to identify the exact translation key before preparing the `.tsv` row, usually by locating the relevant `Translator.*` call.
- In this repository's translation TSVs, leave regional-variant locale columns blank except for `es_ES`, `es_SV`, `es_EC`, and `zh_HANT`.
- In this repository's translation TSVs, keep `es_419` blank.
- In this repository's translation TSVs, leave fake language columns such as `ia` and `io` blank.
- In this repository's translation TSVs, leave the last two named columns blank.

## Expected Output

When using this skill:

- Do not modify `translation4.csv`.
- Create a `.tsv` file in `shared/src/main/resources/i18n/`.
- Use the exact header from `translation4.csv`.
- Include only the proposed new or updated rows.
- Summarize which keys were added or revised.

## Do Not

- Do not edit `shared/src/main/resources/i18n/translation4.csv` directly.
- Do not create CSV output instead of TSV.
- Do not reorder or shorten the language columns.
- Do not invent non-standard sports terminology when established Olympic Weightlifting wording exists.
- Do not stage, commit, or push unless explicitly requested.