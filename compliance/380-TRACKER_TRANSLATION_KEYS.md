# Tracker Translation Keys

This item spans OWLCMS (the forwarder) and the tracker (`owlcms-tracker`, repositories
`tracker` and `tracker-core`). Tracker displays must show text in their own language,
the way OWLCMS pages do, instead of the language the forwarder happened to translate in.

## Context

OWLCMS translation follows one rule (2026-11, see `Translator.getLocale()`): the
language is that of the page showing the text. `FieldOfPlay` never translates; UI
events carry translation keys and values, and each receiving page translates them inside
its own `ui.access`. Work started from a page carries the page's language explicitly
(`Translator.propagate`, `Translator.withLocale`).

The forwarders are the exception. `WebSocketEventForwarder` (V2, current tracker) and
`EventForwarder` (legacy HTTP) build their payloads on the event-forwarding thread, which
has no page: every text is translated in the database default language (English when
there is none). A tracker display in another language shows that text as received.

### What the forwarder translates today

`WebSocketEventForwarder`, `ForwarderPayloadBuilder` and `AthleteExporter` send
already-translated strings, among them:

- break and ceremony titles (`PublicMsg.*`, `BreakMgmt.*`, `BreakType.*`, review labels
  from `ReviewLabels.key`);
- the session line: `Group_number`, `Group_number_results`, `Scoreboard.GroupLiftType`,
  `Scoreboard.DescriptionLiftTypeFormat`, `Scoreboard.AttemptsDone`;
- the attempt: `AttemptBoard_attempt_number`, `Snatch` / `Clean_and_Jerk`, lift type
  keys;
- `recordMessage` (`Scoreboard.NewRecord`, `Scoreboard.RecordAttempt`);
- `Results.Extra/Invited` for athletes outside the competition;
- the scoreboard translation map (`Scoreboard.*` keys and `ScoringTitle`), sent in the
  database language only.

### What the tracker can already do

- OWLCMS sends every locale's translations at startup (`translations_zip`, or on a 428
  "missing translations" request); the hub keeps them all.
- `CompetitionHub.getTranslations({ locale })` and `CompetitionHub.translate(key,
  locale)` resolve a key with regional, base-language and English fallback.
- Displays take a `lang` option (for example the attempt board, `options.lang`), and
  some helpers already rebuild text from keys with the display's translation map
  (`formatAttempt`, `inferGroupName`, `inferBreakMessage` in the attempt board).

What is missing is the input: most payload text arrives translated, without the key and
values the tracker would need to translate it again.

## Decision

- **Additive only.** The V2 format has external readers besides the tracker: existing
  field names, value types and translated strings stay exactly as they are.
- **OWLCMS adds keys and values next to each translated text**, for example a
  `...Key` field holding the translation key and a `...Args` array holding the values to
  fill in, raw (numbers, names, codes), never pre-translated. Nested pieces that are
  themselves translated (a lift type inside `Scoreboard.GroupLiftType`) are sent as
  keys too, so the tracker can translate them in the same language.
- **The tracker translates from the key when it is present**, in the display's
  language (`lang` option), and falls back to the translated string otherwise. Older
  OWLCMS versions keep working unchanged.

## Work

1. OWLCMS: inventory every translated field in `WebSocketEventForwarder`,
   `ForwarderPayloadBuilder` and `AthleteExporter` (the list above is the starting
   point), and add its key and values. The legacy `EventForwarder` keeps sending
   translated text only.
2. OWLCMS: document the new fields in the V2 protocol description used by the tracker.
3. tracker-core: expose a helper that formats `{ key, args }` with
   `CompetitionHub.translate` for a given locale, translating nested keys.
4. tracker: use it wherever a display shows a forwarded text, keeping the received
   string as the fallback; replace the attempt-board `infer*` helpers where the key is
   now available.
5. Check a tracker scoreboard and attempt board in a language other than the database
   language, against an OWLCMS sending keys and against an older OWLCMS.
