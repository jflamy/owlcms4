# Athlete Card Dialog Closing — Browser Check

Status: one manual browser check remains. The concurrent-editing protections are implemented;
the analysis and implementation record are archived in
[done/ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md](done/ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md).

## What is checked

Athlete cards are opened as strictly modal dialogs (`ModalityMode.STRICT`, in
`OwlcmsGridLayout.showDialog` and `RegistrationContent.openAthleteCard`). Vaadin attaches a
dialog opened from the card to the card, so closing the card should also close it. This was
checked against the Vaadin documentation and implementation, but never in a browser, and the
automated tests do not cover dialogs.

## Steps

1. Start a session and open an athlete card from the announcer page.
2. Change a field on the card without saving.
3. From another station, change the same athlete, then save the card so that the stale-save
   confirmation opens.
4. Leave the confirmation open and give a decision on the platform.

Expected: the card and the confirmation both disappear, and nothing is left that could be
clicked to save.
