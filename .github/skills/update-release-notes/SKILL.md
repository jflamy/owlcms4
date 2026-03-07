---
name: update-release-notes
description: "Use when: updating OWLCMS 4 release notes, adding a release note entry, documenting a bug fix or feature in release notes, or deciding which ReleaseNotes.md file is canonical in the owlcms4 repository. Keywords: release notes, ReleaseNotes.md, markdown release notes, changelog, document fix, add note for release 64."
---

# Update Release Notes

This skill updates release notes for the `owlcms4` repository.

## Scope

Use this skill only for `owlcms4` release note work.

## Canonical File

The canonical release notes file is:

- `src/main/markdown/ReleaseNotes.md`

Do not update the repository-root `ReleaseNotes.md` unless the user explicitly asks for that file too.

## Goal

Add or revise release note entries in the existing house style, with short user-facing wording.

## Workflow

1. Read `src/main/markdown/ReleaseNotes.md`.
2. Find the current release section the change belongs to.
3. Add a concise entry in the same formatting style already used in the file.
4. Prefer user-visible behavior over implementation details.
5. If useful, add one or two flat sub-bullets for clarification.
6. Avoid duplicate entries.
7. Leave unrelated release notes unchanged.

## Writing Rules

- Use the existing release number section already present in the file.
- Do not create a new release heading unless the user explicitly asks.
- Keep wording brief and concrete.
- Mention the visible outcome, not internal class names, unless the user asks for technical detail.
- If a change affects exports, templates, or generated documents, describe the exported result from the user perspective.
- Match the punctuation and indentation style already in the file.

## Repository-Specific Notes

- In `owlcms4`, the markdown-folder file is authoritative for release notes.
- The repository-root `ReleaseNotes.md` may exist for packaging or publishing flow, but it is not the default edit target.
- If you accidentally update the root file, move the entry to `src/main/markdown/ReleaseNotes.md` and remove the stray root change unless the user requested both.

## Expected Output

When using this skill:

- Edit only the intended release notes file.
- Add the new entry under the appropriate release section.
- Summarize what was added.
- If relevant, remind the user which file should be staged.

## Do Not

- Do not touch translation CSV files.
- Do not stage, commit, or push unless explicitly requested.
- Do not rewrite large sections of release notes just for wording preference.
