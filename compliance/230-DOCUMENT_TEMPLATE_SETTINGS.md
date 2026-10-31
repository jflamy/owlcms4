# Document Template Settings

Status: parked (2026-10-31).
Related: [136-CONFIG_COARSE_CONFLICT.md](done/136-CONFIG_COARSE_CONFLICT.md).

## Problem

The last template chosen for each document (start list, cards, protocol,
medal schedule, results, ...) is stored on the `Competition` singleton, in
about 25 `...TemplateFileName` columns
([Competition.java](../owlcms/src/main/java/app/owlcms/data/competition/Competition.java)).

So choosing a different template when printing is a save of the competition
settings:

- it is recorded with `lastSavedAt`/`lastSavedBy` like any other save;
- a Competition page opened before that print refuses its next Update
  ("saved by RESULTS after you opened this page").

Someone printing documents should not interact with the competition
settings. 136 already limits the damage: opening a download dialog that
preselects the stored template no longer saves. Only a different choice
does.

### Where templates are chosen today

- [JXLSDownloader.processTemplateSelection](../owlcms/src/main/java/app/owlcms/components/JXLSDownloader.java):
  every download dialog with a template picker (results, medals, protocol,
  ...).
- [DocumentDownloadDialog](../owlcms/src/main/java/app/owlcms/nui/preparation/DocumentDownloadDialog.java)
  and [TemplateSelectionFormFactory](../owlcms/src/main/java/app/owlcms/nui/preparation/TemplateSelectionFormFactory.java):
  the template choices of the document kits on the Documents page.
- [PreCompetitionTemplate](../owlcms/src/main/java/app/owlcms/nui/preparation/PreCompetitionTemplate.java):
  getter and setter pairs on `Competition` for each kit element.

All of them write through `CompetitionRepository.apply`.

## Proposal

Store the template choices apart from the competition settings, one entry
per document, for example a `DocumentTemplate` table (document key, template
file name).

- Choosing a template saves only that document's entry, last choice wins.
  No stale-save check: two people printing do not conflict in any way that
  matters.
- The Competition page and its stale-save check no longer see template
  choices.
- The kit and downloader code reads and writes the entry instead of the
  `Competition` getter and setter; `PreCompetitionTemplate` carries a
  document key instead of a getter and setter.

## Decided: no migration

- The existing template choices are not migrated. After the upgrade, every
  document starts from its default template; the `Competition` columns are
  no longer read (and can be dropped).
- Importing an older export (V1, or V2 without the new section) also loses
  the template choices: they are ignored, not copied into the new table.
- Only exports made by the new version carry the template choices in their
  own section, and only those imports restore them into the new table.

## Open points

- Whether the choice should stay global, or be remembered per user or per
  platform.
- Other print-time settings on `Competition`, if any, should move with the
  templates.
