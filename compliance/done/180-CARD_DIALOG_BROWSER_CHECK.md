# Athlete Card Dialog Closing — Browser Check

Status: done. The browser check passed (see [Result](#result)). The concurrent-editing protections are implemented;
the analysis and implementation record are archived in
[ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md](ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md).

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

## Result

Run with the `owlcms/src/test/resources/startup/competition-director-acceptance.json` fixture,
session M1, two 1366×768 browser windows: the marshal page (admin), which holds the card, and
the announcer page (competition supervisor), which saves the same athlete. Clock and decisions
were sent over MQTT (`owlcms/clock/A`, `owlcms/refbox/decision/A`); `0 good` is an announcer
decision, `1 good` to `3 good` are referee decisions.

- First run: failed. The card and the confirmation both stayed open after an announcer
  decision. The card compared its athlete with the FOP's current athlete when the queued UI
  update ran, but the lifting order had already moved to the next athlete (25 ms after the
  decision).
- Fix: on a decision, the card uses the athlete carried by the `UIEvent.Decision` event (the
  athlete the decision is about); on a down signal, it still uses the current athlete.
  `DecisionEventAthleteTest` checks that the Decision event names the decided athlete while the
  current athlete has moved on (announcer decision, delayed and immediate referee decisions).
- Announcer decision (MQTT `0 good`): passed. The marshal had Simpson's card open with an unsaved
  C&J 1 declaration of 83; the announcer saved 84; the marshal clicked Update and the stale-save
  confirmation opened. After the decision, the log shows `Athlete card closed on event Decision`
  and the marshal page had no open dialog.
- Referee decision with the marshal's card open (MQTT clock start/stop, then three good
  decisions): passed. This is the case where the marshal expects the athlete to come back right
  away and keeps the card open while the athlete is lifting; saving that card after the decision
  would wipe out the lift result. The marshal had Simpson's card open with an unsaved C&J 1
  declaration of 81. The card closed on the down signal (`Athlete card closed on event
  DownSignal`), before the decision was recorded, and the marshal page had no open dialog.
- Both checks were repeated on a freshly restarted server with the same results. At the time,
  the fixture competition had a 3-member jury, so after a referee decision the clock was refused
  until all jury votes were in (`owlcms/jurybox/juryMember/decision/A` `1 good`..`3 good`). The
  fixture has since been changed to jury size 0 so that these checks need no jury votes. The referee
  check was then repeated with the card visibly open on the marshal page (Schneider, snatch 2 at
  61, C&J 1 changed from 82 to 83 and not saved): the card closed on the down signal, the lift
  was recorded as good about 3 seconds later, and the unsaved value was not saved.
- Testing note: a card closed in a background browser tab stays attached on the server (and can
  log a second `Athlete card closed` line), because Vaadin removes an auto-added dialog only
  when the browser reports the end of its close animation, which a hidden tab does not run.
