# Age Group Definitions and Athlete Reassignment — Target Behavior

Status: target behavior agreed 2026-10-27, to be reimplemented. This replaces the earlier bug-fix
plan (git history). "Today" notes describe dev69 where it differs from the target. Section 9
lists what is still open.

Priority: can wait. The current behavior has been the same since version 65 and has been lived
with; it is not on the blocking path for a beta.

## 1. Concepts

- **Championship**: the age groups competing for the same medals and team results (Youth,
  Junior, Masters…). Most championships have one age group; Masters is the usual multi-age-group
  case. Age groups of one championship may overlap (USAW Juniors: U25 15–25 and JR 15–20).
- **Age group**: code, gender, age range, active flag, championship, and its body-weight
  categories. The categories are contiguous from 0 up to the unlimited category (maximum 999).
- **Category**: one body-weight class of an age group, with an optional qualifying total (QT).
- **Participation**: an athlete competing in a category. It holds the team and mixed-team flags
  and the ranks.
- **Registration category**: the athlete's main participation.
- **Entry total**: the total the athlete declares at registration (`Athlete.qualifyingTotal`,
  shown as "Entry Total").

An athlete's intent is the **championship**. Being entered in a championship is a deliberate
choice made at registration (form or import). Within that championship, age groups and
categories follow from the athlete's data.

## 2. Fitting a category

An athlete fits a category when all of these hold:

- its age group is active;
- the athlete's age (existing age computation, including the birth-year rule) is within the age
  group's range;
- the gender matches (existing gender-inclusive rules);
- the body weight is above the category minimum and at most its maximum, using the actual body
  weight, or else the existing presumed weight;
- the category QT is 0, or the entry total is at least the QT.

**Missing data.** An athlete with no birth date, or with no body weight and no presumed weight,
could only fit a completely open category (ages 0–999, body weight 0–999, medals on a score),
which OWLCMS does not provide. In practice such an athlete fits nothing: reassignment takes them
out of the championship, and the dry run lists them with the reason "missing birth date" or
"missing body weight".

**Qualifying total.** The QT is part of the fit rule everywhere: athlete form, spreadsheet
import, reassignment and the global actions. An athlete whose entry total is below the QT is out
of that category. If that leaves no category of the championship, the athlete is out of the
championship. The only way back in is an exception made for that athlete in the athlete form,
during data entry before the competition starts (the Verification of Final Entries, VFE, TCRR
6.2). The exception is not recorded: the check runs again whenever the athlete is edited, so it
shows up again at weigh-in, and a later reassignment ejects the athlete again. The officials
handle such an abnormality with their own procedure, outside OWLCMS. At the definition level
there are no per-athlete exceptions: changing a QT in an age group applies to every athlete's
entry total, exactly like changing the age range.

- **Athlete form.** Categories that fail only on the QT are still listed, marked as needing an
  exception. Selecting one requires "Ignore errors". The check covers every selected category,
  not only the registration category.
- **Spreadsheet import.** The import goes athlete by athlete. A category that the athlete does not
  fit because of the QT is skipped, with a warning naming the athlete and the category, and the
  rest of the line is imported. If the skipped category was the registration category (the first
  one on the line), the preferred remaining category becomes the registration category, or there
  is none; the warning says which.
- **Reassignment and global actions.** Athletes who leave the championship because of the QT are
  listed in the dry run with the reason "QT" (sections 3.4 and 5).
- **Results.** Unchanged, already implemented (`AthleteSorter`): an athlete whose final total is
  below the QT is out of competition for the Total, with no Total rank or medal. Under IMWA rules
  the rank and team points count, but the Total medal is withheld. Snatch and Clean & Jerk medals
  are not affected. The exception is not recorded, so it plays no part at results. The IMWA choice
  is a global competition setting: IMWA competitions are IMWA-only, so a per-championship setting
  for mixed competitions (300) is not a priority.

## 3. Editing an age group

### 3.1 What can be edited

- Age group: code/name, gender, age range, active, championship, medals, scoring, already-gendered.
- Categories, with exactly three operations:
  - **add an upper limit** (a new category, starting with QT 0);
  - **delete a category** (never the unlimited one);
  - **change a category's QT**.

A category's lower limit is not edited: it is always the upper limit of the category below it, or
0 for the first one. The unlimited category (upper limit 999) always exists and keeps its
identity; deleting every finite limit leaves it covering 0 to 999. Because the categories are
contiguous by construction, there is never a body-weight gap. Adding an upper limit that already
exists is refused.

Today: a duplicate upper limit is not refused.

### 3.2 Categories: identity, creation, deletion and remapping

Editing an age group never destroys data it does not have to. Within an age group, a category is
identified by its **upper limit**. Saved categories keep their database id.

| Edit | Category rows when saved |
| --- | --- |
| Change a QT | Same category, same id; the QT is updated in place. |
| Add upper limit L | A new category (new id) with upper limit L. The category just above keeps its id; its lower limit becomes L. |
| Delete a category | Its row is deleted, after its participations are remapped and registrations repaired. The category just above keeps its id; its lower limit becomes the deleted category's lower limit. |
| Move a limit (e.g. 81 → 83) | Done as delete 81 and add 83: M81 is deleted, M83 is created, M89 keeps its id with lower limit 83. |

A category whose upper limit is unchanged is the same category, even if its lower limit or QT
changed. A different upper limit is a different category, with a different code and its own
results and medals; nothing is carried from the deleted category to the new one. The unlimited
category's code ends in 999 and does not change; only its displayed name follows the highest
finite limit (+89 becomes +81 when 89 is deleted).

Example, men's categories 73, 81, 89, +89:
- Change the M81 QT from 200 to 220: M81 is updated in place.
- Add 85: M85 is created, covering above 81 up to 85; M89 keeps its id, now above 85.
- Delete 81: M81 is deleted; M89 keeps its id, now above 73.

**Remapping** happens when the change is applied (Apply, or a later "Reassign within
championships"); it is part of the championship reassignment of section 4. For the edited age
group:
- Each athlete who was in the age group before the change is placed in the category of the new
  list that contains their body weight (above the lower limit, at most the upper limit), if
  their entry total meets that category's QT. Body weight is the actual body weight, or else the
  presumed weight, computed before the change (it can depend on a category being deleted).
- Same category as before (same id): the participation is unchanged, with its team flags and
  ranks.
- Different category: the old participation is removed and a new one is created in the target,
  with the default team flags (section 4).
- The registration category is then recomputed (section 4).
- QT not met: the athlete has no participation in this age group. They stay in the championship
  only if another of its age groups fits them.
- Since the weight range always reaches 999, body weight alone always finds a category: only the
  QT, age, gender or the active flag can leave an athlete without one.
- With Later, new categories are created and limits and QTs are updated, but participations stay
  where they are, possibly no longer fitting, until "Reassign within championships". Later is
  not offered when a category is deleted, because its participations must be remapped before
  its row disappears.

Today: the editor copies the categories with new ids. Confirming any change that needs
reassignment (a single QT change is enough):
- deletes and recreates every category of the age group;
- clears the registration category of its athletes;
- deletes all their participations in the age group, losing team flags and manual choices;
- then runs a cleanup over the whole database.

Reproduced on dev69 2026-10-27: one QT edit took the age group from 40 participations to 0, and
kept 0 of 14 category ids. "Reassign athletes" can rebuild participations afterwards, but not
manual choices or team flags. The comment in `CategoryGridField` saying the ids are preserved is
false.

### 3.3 Which changes reassign athletes

| Change | Effect |
| --- | --- |
| Age range, gender, active flag, a category QT, an upper limit added or a category deleted | The age group's championship is reassigned (section 4) |
| Code/name, already-gendered, medals, scoring | Saved directly; participations untouched; category codes recomputed |
| Championship association | Signaled and confirmed (section 3.4); the age group and its participants then move to the other championship as they are, with no reassignment |

Today: the decision compares a hash of category codes, weights and QT, and a plain rename may
also trigger it if the category codes change. Changes to the age range, gender or active flag
only trigger it when the athletes already in the age group are affected.

### 3.4 Save flow

**Pending changes.** The editor works on a scratchpad: a detached copy of the age group and its
categories, never stored. Age range, gender, active flag, limits and QTs changed in the editor
are pending changes, not applied to the stored age group. When the user confirms, the pending
changes are applied to the stored age group (section 3.2) and, with Apply, the championship is
reassigned, in the same transaction. On Cancel, at any step, the scratchpad is thrown away and
nothing is stored.

A change that doesn't reassign is saved and the editor closes. A change of championship
association is signaled first: the confirmation names the age group, the two championships and
the number of athletes moving, and runs the age-gap check on both championships (the one losing
the age group and the one gaining it). Confirm / Cancel.

A change that reassigns goes through these steps:

1. **Age-gap check**, always first, even when nobody is registered. For each gender, the active
   age groups of the championship must cover a continuous range of ages, from the lowest minimum
   to the highest maximum. If there is a hole, warn first, for example "Masters no longer covers
   men aged 39". Continue / Cancel. Overlaps are allowed, and inactive age groups do not count.
2. **Reassignment dialog**, when the championship has participants. A dry run computes the
   result, and the dialog names the championship and lists the athletes who would leave it
   because they fit nothing in it. For each, it gives the reason: age, gender, QT, inactive,
   missing birth date or missing body weight. When nobody leaves, it says so.
   - **Apply**: apply the pending changes and reassign the championship now.
   - **Later**: apply the pending changes to the definitions only. Participations stay as they
     are, and the "Reassign within championships" button (section 5) turns red. Disabled when
     categories are removed, because their participations would point to deleted rows.
   - **Cancel**: the scratchpad is thrown away; nothing is saved.
   When the championship has no participants, the change is saved directly.
3. Apply recomputes everything from current data when saving; the dry run is only a preview.

The editor closes only after a successful save.

Today: there is no gap check and no list. A single dialog says that athletes will need
reassignment (Confirm / Cancel), and Confirm triggers the data loss of section 3.2. The
championship row is created when the form opens and again before the dialog, so Cancel can leave
it behind. `AgeGroup.setForceSave(false)` ignores its argument. A change of championship is
saved without any confirmation.

Examples:
- **Gap from a wrong editing order.** M35 35–39 and M40 40–44 must become M35 35–38 and M40
  39–44. Narrow M35: gap warning (men aged 39), Continue, Later. Widen M40: no gap, Later. Then
  "Reassign within championships" lists nobody leaving Masters: Apply.
- **Overlap correction.** M30 is erroneously 30–35, overlapping M35 35–39. Correcting M30 to
  30–34 drops nobody from Masters: the 35-year-olds lose their M30 participation and keep M35.

### 3.5 Active checkbox in the grid

Toggling Active is an edit of the active flag and follows the same flow. The checkbox shows the
stored value until the change is confirmed, and goes back to it on Cancel.
- Deactivating moves the age group's athletes to other fitting age groups of the championship,
  or out of the championship.
- Activating places the championship's athletes who fit the age group. Activating the only age
  group of a championship adds nobody, because nobody is in that championship (section 4).

Today: the checkbox saves directly with no confirmation, and participations stay as they were.

### 3.6 Deleting an age group

Deletion uses the existing CRUD delete confirmation, with text specific to age groups. In a
single transaction, its participations are removed, and athletes whose registration category was
in it get the preferred one among their remaining participations, or none.

Today: the registration category is moved to a remaining participation, and then a cleanup runs
over the whole database.

### 3.7 Changes during the competition

Editing definitions and reassigning after the competition has started is allowed. The lifts,
body weight, group and start number are on the athlete; a participation holds only ranks and
team flags. An athlete moved to another category keeps all their lifts, and the ranks are
recomputed after the change (section 6), so no result is lost.

Such a change is refused while a session is under way on any platform: a change during a session
would reorder ranks and scoreboards while lifts are being entered. Worse, the platform keeps its
own copies of the session's athletes, participations included, and saves them after each lift;
since participations are saved with the athlete, such a save can recreate removed participations
and silently undo part of the change. A session is under way when
`FieldOfPlay.isSessionInProgress()` is true: the athlete clock has been started for the selected
session, or it already had attempts when selected; stops and breaks do not clear it, unselecting
the session does. Every platform is checked, whatever session it runs; the refusal names each
session under way and its platform. It applies to every action on the age-group page: saving an
age group, toggling Active, deleting an age group, changing its championship, and both global
actions. It is checked when the action is requested and
again when the transaction starts (section 11, step 1). Medals already presented and printed
results are outside OWLCMS.

Today: nothing prevents these changes during a session. The athlete forms already refuse edits
of an athlete whose session is under way (`LiftingSessionGuard`).

### 3.8 One user at a time on the age-group page

The age-group page has a single global lock: only one user at a time can have it open.

- **Taking the lock.** Opening the page takes the lock when nobody holds it.
- **Lock already held.** The page does not open. A dialog names the holder and since when, with
  **Break the glass** and **Back**. The holder is shown as the account user name in accounts
  mode; in PIN mode, where there are no names, as the browser's address and session tag (as in
  the audit log, `AuditActor`).
- **Breaking the glass.** After a confirmation, the lock passes to the new user and the page
  opens. The previous holder's editor or dialog closes, their scratchpad is thrown away, and
  their page shows the lock dialog naming the new holder, so they can break the glass in turn
  or go back. A save already running finishes first. The takeover is written to the audit log
  with both users.
- **Releasing the lock.** Leaving the page, closing the browser tab or the end of the browser
  session releases it. A lock left by a crashed browser is released when the server notices the
  browser is gone, or can be broken.
- **Coverage.** The lock covers everything on the page: the editor, the Active checkbox,
  deleting an age group and the global actions.
- The lock is held in transient fields of the `Competition` singleton; it is never stored, and a
  restart clears it (section 11).

Every save or action first checks that the user still holds the lock (section 11, step 1).

Today: several users can edit age groups at the same time, and the last save wins.

## 4. Reassigning a championship

Reassignment covers every athlete who participated in the championship before the change,
including athletes linked only to a category being removed. The athlete's age, gender, body
weight (including the presumed weight) and entry total are taken from before the change. For
each athlete:

- Their participations in the championship become every category of its active age groups that
  they fit (section 2). An athlete who fits several overlapping age groups gets all of them.
- A participation in the same category before and after is kept unchanged: same row, flags and
  ranks.
- Their other participations in the championship are removed.
- An athlete who fits nothing in the championship leaves it.
- Athletes who were not in the championship are never added.
- Other championships are never touched.

**Team flags.**
- An unchanged participation keeps its team and mixed flags. The mixed flag is cleared only if
  the age group no longer has mixed teams.
- A moved or new participation gets the import default: team member when the "Explicit Teams"
  feature switch is off, no team when it is on, never mixed. This includes a move to another
  body-weight category of the same age group, because the opponents and the chances of a medal
  are different.
- A removed participation takes its flags with it; they are never transferred elsewhere.

**Registration category.**
- For every athlete in the reassigned championship, it is recomputed as the most preferred of
  all the athlete's participations, in every championship, using the existing preference
  (specificity, then medal order, as in `doFindEligibleCategories` and `Athlete.bestMatch`).
- So reassigning one championship can change the registration category, even to a category of
  another championship. The typical case is an athlete who becomes eligible for a younger age
  group, which is more specific.
- A manual choice of registration category is replaced by the most preferred one when the
  athlete's championship is reassigned.
- When nothing is left, it is cleared.

Today: reassignment considers only the athletes already in the edited age group and only its
categories. It ignores the QT, and replaces participations instead of keeping the ones that
still fit. The athlete form already applies the target rule to one athlete when their age or
body weight changes: `CategoryRepository.migrateEligibleCategories` keeps the athlete within
their championships and checks the QT.

## 5. Global actions (age-group page)

Two actions replace today's single "Reassign athletes" button. Both start with a dry run, show
the list, and end with Apply / Cancel.

- **Reassign within championships**: for use after Later. It runs the section 4 rule over every
  championship and adds nobody to a championship. The list shows, per championship, the athletes
  who would leave it.
- **Recompute all eligibilities**: for setting up a competition, for example after loading
  definitions or activating championships. Every athlete is placed in every category they fit in
  all active age groups, so this adds athletes to championships by design. The team-flag and
  registration rules of section 4 apply. The list shows the athletes who would gain or lose each
  championship, so an unwanted addition is visible before Apply: for example, a junior entered
  only in JR on purpose who would be pulled into a games qualifier.

"Keep Main Registration Category" (`ResetCategories.ClearEligibles`) is removed. It was meant to
undo the extra categories added by "Reassign athletes", for competitions where athletes compete
only in what they registered for. "Reassign within championships" covers that need, since it
never adds anyone to a championship.

Today: "Keep Main Registration Category" is broken. It removes every participation, including
the registration category's own: verified on dev69 2026-10-27, 10 athletes went from 10
participations to 0, each still with a registration category but absent from every category's
rankings until edited or reassigned. It was added on 2025-04-08 and never documented.

Today: "Reassign athletes" recomputes all eligibilities, ignores the QT, adds athletes to every
category they fit in all active age groups, then sets "team member" on every participation and
clears mixed-team membership.

## 6. Guarantees

- Each save or action is all-or-nothing. Championship creation, definitions, participations and
  registration categories commit together or not at all.
- No other championship's data changes, and there is no database-wide cleanup.
- Opening the editor or computing a preview writes nothing. Championship rows are created only by
  an accepted save.
- Only the user holding the page lock can save (section 3.8). Athlete data is read again when
  saving.
- After a change, participant counts are updated and rankings are recomputed.
- The Vaadin UI thread never waits on database work.

Today: there are several transactions and a global cleanup; the championship row is created
before confirmation; there is no protection against concurrent edits.

## 7. Differences from current behavior

| Area | Today | Target |
| --- | --- | --- |
| Category identity | Every category recreated with a new id on reassignment | Identified by its upper limit: kept and updated in place; a new limit creates a category; a deleted category is removed after its athletes are remapped |
| QT-only edit | Wipes the age group's registrations and participations | Reassigns the championship under the QT rule |
| QT in reassignment | Ignored | Entry total checked against the QT |
| QT at registration | Form checks the registration category only; import refuses the whole line | Form checks every selected category (exception with "Ignore errors"); import skips the category with a warning |
| Scope | Athletes in the edited age group, its categories only | Athletes in the championship, all its age groups |
| Athletes outside the championship | Added by "Reassign athletes" | Never added, except by "Recompute all eligibilities" |
| Team flags | Lost (edit), or "team member" everywhere with mixed cleared (reset) | Kept on unchanged participations; default on moved or new ones |
| Registration category | Cleared (edit) or recomputed (reset) | Recomputed for the championship's athletes: most preferred of all their participations, in every championship |
| Confirmation | Confirm / Cancel, generic text | Gap check, then a list of athletes leaving, Apply / Later / Cancel |
| Editor changes | Championship row created when the form opens; Cancel can leave it behind | Kept in a scratchpad; applied only when confirmed, thrown away on Cancel |
| Championship association | Saved silently | Signaled, gap check on both championships, Confirm / Cancel |
| During a session | Allowed | Refused while a session is under way on any platform |
| Concurrent users | Several users edit at once; the last save wins | One user at a time on the page; others can break the glass |
| Active checkbox | Saved silently | Same flow as an edit |
| Global actions | "Reassign athletes" and "Keep Main Registration Category" (broken) | "Reassign within championships" and "Recompute all eligibilities" |
| Side effects | Database-wide cleanup; championship row on Cancel | None outside the championship; nothing written before acceptance |

## 8. Messages

Add the keys through a translation TSV; never edit `translation4.csv` directly. English text:

| Key | English |
| --- | --- |
| `AgeGroup.AgeGap.Title` | Age gap |
| `AgeGroup.AgeGap.Warning` | {0} no longer covers {1} aged {2}. |
| `AgeGroup.Reassign.Title` | Reassign the athletes of {0} |
| `AgeGroup.Reassign.Warning` | The athletes of this championship will be placed in the categories they now fit. The athletes listed below no longer fit any category of the championship and will leave it. |
| `AgeGroup.Reassign.NobodyLeaves` | No athlete leaves the championship. |
| `AgeGroup.Reassign.DeletionNote` | Categories are being removed; athletes must be reassigned now. |
| `AgeGroup.Reassign.Apply` | Apply |
| `AgeGroup.Reassign.Later` | Later |
| `AgeGroup.Delete.Warning` | Athletes will be removed from this age group. If their registration category belongs to this age group, a remaining participation becomes their registration category, if there is one. |

Also needed:
- the reasons for leaving: age, gender, QT, inactive, missing birth date, missing body weight;
- the confirmation of a championship change (age group, both championships, athletes moving);
- the labels and warnings of the two global actions;
- the dry-run texts for "Recompute all eligibilities" (athletes gaining or losing a championship);
- the page-lock dialog (holder and since when, Break the glass, Back), its confirmation, and the
  notice to a user whose lock was taken;
- a message refusing a change while sessions are under way, naming each session and its platform
  (as `SessionInProgress.CannotEditRegistration` does for athletes);
- a message when an upper limit that already exists is added.

`ResetCategories.ResetAthletes`, `ResetCategories.Warning_ResetCategories`,
`ResetCategories.ClearEligibles`, `ResetCategories.Warning_ClearEligibles`,
`ResetCategories.ClearEligiblesMouseOver`, `CategoryAssignment.Title` and
`CategoryAssignment.Warning` become unused; the external translation tool removes them.

## 9. Open decisions

- **Save in progress and failures.** Proposed:
  - While a save runs, the dialog buttons are disabled and a progress indicator is shown.
  - A failure closes the dialog and shows the error; Apply is never offered again on a stale
    dry run. The editor stays open with the scratchpad values.
  - Saving again starts over: lock check, session check, gap check and a fresh dry run.
  - A user who lost the lock (section 3.8) cannot save again: the editor closes.

## 10. Acceptance tests

Tests use domain code and fixtures, never Vaadin objects.

- **Data preservation.** A QT edit, a rename, a change of medals or scoring, and a change of
  championship keep every category id, participation and team flag except those the rules
  change. Registration categories change only as section 4 says. This is the regression test for the 40 → 0 case.
- **Classification.** Every change in section 3.3 is classified correctly.
- **Category identity and remapping** (men's 73, 81, 89, +89):
  - A QT change on M81 keeps its id.
  - Adding 85 creates M85 and keeps the M89 id; athletes above 81 and up to 85 move from M89 to
    M85, the others keep their M89 participation unchanged.
  - Deleting 81 removes M81 after moving its athletes to M89; M89 keeps its id.
  - Moving 81 to 83 deletes M81 and creates M83; athletes up to 83 go to M83, the others above 81
    go to M89.
  - The unlimited category keeps its id; deleting every finite limit leaves it covering 0 to 999.
  - A duplicate upper limit is refused.
  - Presumed body weight is taken before the change, including for athletes registered in a
    deleted category.
- **Scratchpad and copy-back** (section 11, steps 1–8). The tests build the scratchpad as a
  detached copy, the way the editor does, and call the domain save directly.
  - Editing the copy (QT, added limit, deleted category, age range, Active) and then cancelling
    or never confirming leaves every stored row unchanged: ids, limits, QTs, participations,
    team flags, registration categories, and no new championship row.
  - Each age-group field (code/name, gender, age range, active, championship, medals, scoring,
    already-gendered) is copied to the stored age group, and only that age group changes.
  - One save combining a QT change on M81, adding 85 and deleting 73 gives exactly: M81 same id
    with the new QT, M85 new, M73 gone, M89 and +89 same ids. The lower limits of M81, M85, M89
    and +89 are 0, 81, 85 and 89; codes are recomputed, the unlimited code still ending in 999.
    With Apply, the M73 athletes move to M81, the M89 athletes up to 85 move to M85, and
    athletes below the new M81 QT have no participation in the age group; everyone else keeps
    their participation.
  - The copy's categories have new ids and empty participation lists, yet no participation of
    a kept category is deleted: the participation count of each kept category is unchanged
    except for the athletes the rules move. This is the regression test for the 40 → 0 case.
  - The snapshot is taken at save time, not when the editor opened: a body weight entered
    meanwhile is used. Presumed weights of athletes in a deleted category come from the
    snapshot.
  - Before a category is removed (step 7), no participation and no registration category
    points to it; after commit, no reference to a deleted category remains.
  - With Later, steps 1–5 run, step 6 does not: participations are identical before and after.
  - A change of championship goes through steps 1–3 only; participations are identical.
  - A failure injected after each of steps 3 to 7 rolls everything back to the state before the
    save.
  - Participant counts and rankings are updated after commit, and not on failure.
- **Championship scope.**
  - A Masters age-range edit moves athletes between Masters age groups.
  - An athlete who fits no Masters age group leaves Masters only.
  - Open and the other championships are unchanged.
  - The M30 overlap correction drops nobody.
  - An athlete who fits U25 and JR gets both.
- **No recruiting.**
  - The JR-only junior stays out of the games qualifier after an edit and after "Reassign within
    championships".
  - Activating the only age group of a championship adds nobody.
  - "Recompute all eligibilities" adds the junior, and its dry run lists the addition.
- **QT.** A raised QT removes athletes whose entry total is below it, and the dry run lists them
  with the reason "QT". A lowered QT keeps everyone who still fits. A missing or zero entry total
  fails a QT above 0.
- **Missing data.** An athlete with no birth date, or with no body weight and no presumed weight,
  leaves the championship on reassignment and is listed with the matching reason.
- **QT at registration.**
  - The form lists QT-failing categories as needing an exception, and requires "Ignore errors"
    for each selected one, not only the registration category.
  - The import skips a QT-failing category with a warning naming the athlete and the category,
    imports the rest of the line, and picks another registration category when the first one
    was skipped.
- **Team flags.**
  - An unchanged participation keeps all four flag combinations.
  - The mixed flag is cleared when the age group loses mixed teams.
  - A move to another weight category, a move to another age group and a new participation each
    get the Explicit Teams default.
  - A removed participation never transfers its flags.
- **Registration category.**
  - It is the most preferred of all the athlete's participations, across championships, after
    reassignment (specificity ties included).
  - An athlete who becomes eligible for a younger, more specific age group gets it as
    registration category, even when the old registration participation is unchanged.
  - Athletes outside the reassigned championship keep their registration category.
  - It is cleared when nothing is left.
  - It is repaired before a category or an age group is deleted.
- **Age gap.** A hole is reported per gender with the missing ages; overlaps are not gaps;
  inactive age groups are ignored.
- **During the competition.**
  - An athlete with recorded lifts who is moved to another category keeps every lift, and gets
    the same ranks in the new category as a full recomputation from scratch would give. The old
    category's ranks are recomputed without them.
  - With a session under way on any platform, each action of section 3.7 is refused, and nothing
    changes. With two platforms and a session under way only on the second, the action is still
    refused, and the message names that session and platform. A stop or a break does not lift
    the refusal; unselecting the session does. A session that starts between the request and the
    transaction is caught at step 1.
  - Regression for the reason of the refusal: after a reassignment made with no session under
    way, starting a session and recording lifts does not recreate any removed participation.
- **Dry run.** It writes nothing, and the athletes it lists are exactly the ones Apply removes.
- **Later.** Later keeps participations and is refused when categories are removed. The M35/M40
  example works with Later, then "Reassign within championships".
- **Atomicity.** A failure injected after championship creation, after the definition changes or
  after the participation changes rolls everything back. This includes deleting an age group,
  toggling Active and both global actions.
- **Page lock.**
  - A second user opening the page gets the dialog naming the holder (user name in accounts
    mode, browser address in PIN mode) and cannot edit.
  - Breaking the glass gives the lock to the second user and posts
    `AppEvent.LockHasBeenLost`. Only the first user's page reacts: its editor closes and it
    shows the lock dialog naming the second user. A save the first user attempts afterwards is
    refused, even if the event was missed. The audit log records the takeover.
  - Leaving the page or ending the browser session releases the lock.
  - A save already running when the glass is broken completes, and the new holder sees its
    result.
  - Saving the competition settings while the page is open keeps the lock.
- **No writes before acceptance.** Opening the editor or computing a preview creates no
  championship row and changes nothing. Cancel at any step leaves the stored age group,
  categories and participations exactly as before.
- **Championship change.** Moving an age group to another championship asks for confirmation,
  reports an age gap in either championship, and keeps its participations as they are.
- **Browser checks.**
  - The editor closes only after a successful save.
  - Cancel saves nothing, and there is no double delete confirmation.
  - The Active checkbox shows the stored value until confirmed, and again after Cancel.
  - The list scrolls and the layout holds with long translations on narrow screens.

## 11. Implementation notes

- **Category identity.** Match the editor's categories to the stored ones by upper limit
  (section 3.2). Stored categories keep their database id; added ones have none until saved.
  Never infer identity from generated ids, codes or positions.
- **Scratchpad.** The editor binds to a detached copy of the age group and its categories. It is
  never persisted or merged. On confirmation, one transaction walks the copy explicitly and
  updates the stored rows:
  1. Read the stored age group and its categories, and check that the user still holds the page
     lock (section 3.8) and that no session is under way on any platform (section 3.7).
  2. Take the snapshot of the championship's athletes (age, gender, body weight including
     presumed weight, entry total, participations, team flags, registration category).
  3. Copy the age-group fields from the copy to the stored age group: code/name, gender, age
     range, active, championship, medals, scoring, already-gendered. Create the championship row
     here if it is new.
  4. Match the categories by upper limit:
     - in both: copy the QT onto the stored category, which keeps its id;
     - only in the copy: create a new category in the stored age group;
     - only in the stored age group: mark it for deletion; do not delete it yet.
  5. Recompute the lower limits from the sorted upper limits, and recompute the category codes.
  6. With Apply, reassign the championship from the snapshot (section 4); with Later, skip.
  7. Remove the marked categories from the stored age group, which deletes them through
     `orphanRemoval`. They have no participations or registrations left at this point.
  8. Commit, then update participant counts and rankings.

  The traversal is domain code that takes the scratchpad and the user's decision (Apply or
  Later) as arguments, with no Vaadin dependency, so that section 10 can test it directly.

  Never merge the copy instead: it would bring new ids, and its empty participation lists
  combined with `orphanRemoval` would delete the real participations.
- **Page lock.** Additional `@Transient` and `@JsonIgnore` fields on the `Competition`
  singleton: the holder (an `AuditActor`, for the name shown and the audit log), the holder's
  browser session (to release the lock when it ends), and since when.
  - `CompetitionRepository.save` replaces the singleton through `Competition.setCurrent` with the
    merged copy, whose transient fields are empty. `setCurrent` must carry the lock fields over,
    otherwise saving the competition settings silently drops the lock.
  - Take, break and release the lock under one class-level monitor, not the instance, since the
    instance can be replaced.
  - Release the lock from the page's detach and the browser session's destroy listeners.
- **Lock-lost event.** Breaking the glass posts a new `AppEvent.LockHasBeenLost` on the
  application bus `OwlcmsFactory.getAppUIBus()`, which is not tied to a platform. This is the bus
  and the event family (`AppEvent`) once used for the demo-mode shutdown notices; nothing posts
  to it today. It is created lazily without synchronization, so create it eagerly.
  - The event carries the lock (the age-group page), the new holder (`AuditActor`) and the
    previous holder's browser session.
  - The age-group page registers on attach and unregisters on detach. It ignores events for
    another browser session.
  - The bus calls subscribers on the thread that breaks the glass. The handler must use the UI
    captured when the page attached, never `UI.getCurrent()` (which would be the new holder's
    UI). The existing `AppEvent` classes make that mistake and must not be copied.
  - The previous holder's page closes its editor or dialog, discards the scratchpad, and shows
    the lock dialog naming the new holder (section 3.8), all in a short `ui.access` block.
  - The event is only a notice: a save is protected by the lock check of step 1, not by the
    event arriving in time.
- **Session check.** `OwlcmsFactory.getFOPSessionInProgress(Group)` only looks at the platform
  running a given session. Add a check that goes through all of `OwlcmsFactory.getFOPs()` and
  returns every platform where `isSessionInProgress()` is true, and use it for every action of
  section 3.7.
- **One transaction per save or action.** This also applies to deleting an age group, toggling
  Active and both global actions. Helpers called from the save must not start their own
  transactions or reset caches before commit.
- **Shared fit rule.** Use a single fit computation (section 2) for edits, both global actions
  and the dry run. Reuse the existing age, gender-inclusive, presumed-weight and preference code
  rather than writing new versions. Start from `CategoryRepository.migrateEligibleCategories`,
  which the athlete form already uses for one athlete.
- **Legacy code.** Retire `AgeGroup.forceSave`, `AssignedAthletesException`,
  `AgeGroupRepository.cleanUp`, the global `removeBrokenParticipationsAndCategories()` call on
  these paths, `resetParticipations(false, true)` behind the UI button, and the "Keep Main
  Registration Category" button with its `resetParticipations(true, false)` call. First audit their
  other callers (spreadsheet import, definition loader).
- **UI.**
  - Report success to the CRUD framework only after commit, through a narrow hook; other CRUD
    forms keep their behavior.
  - The dialog offers explicit Apply, Later and Cancel decisions.
  - Run database work outside the UI thread, and keep `ui.access` blocks short.
  - Load the Vaadin skills before writing UI code.
- **Interim delivery.** If the reimplementation takes time, the data-loss part can ship first:
  keep the ids of existing categories in the editor copies and copy their values onto the stored
  categories. That stops the 40 → 0 loss without the new flow.
