# Item 200: acceptance story, "Junior Masters" and "Senior Masters"

Status: done 2026-11-04, archived with item 200.

A realistic end-to-end story for Phase 7 browser acceptance. It exercises, with a believable
sequence of mistakes: moving age groups between championships, renaming a championship, creating
age groups, the age-gap check on delete, the delete dry run, and "Recompute all eligibilities".

Start from the real-data fixture `oldVeryOldMasters_2026.json` (launch "load JSON owlcms reset"; built by
`compliance/phase7/build-fixture.py` as a copy of a real masters database). It has one championship,
**Masters**, with the 2026 age groups M30 to M90 and W30 to W90, all active, and 120 registered athletes:
10 per gender in each 5-year slice from 35 to 64. The 30 and 65-and-over age groups have nobody in them.

Ages are inclusive: M55 covers 55 to 59, M60 covers 60 to 64.

## The story

Marie, the registrar of a regional masters federation, has been told that next season the
competition splits in two: **Junior Masters** for lifters under 55, **Senior Masters** for lifters 55 and over,
each with its own medals and team ranking. She has the Masters database from last year and
decides to reuse it.

### 1. Marie renames Masters to "Senior Masters"

*Preparation > Edit Championships*, open Masters, change the name to `Senior Masters`. The existing
age groups and their athletes follow the rename.

Expected: every age group of the former Masters now shows `Senior Masters` in the Championship column;
no athlete loses a participation.

### 2. Marie creates "Junior Masters" and moves the younger age groups into it

She adds a championship `Junior Masters`, then uses *Select Age Groups* on it. She reads "under 55" a bit
loosely and ticks M30 to M60 and W30 to W60, **including the 55 and 60 age groups**.

Expected: Update applies the move at once, with no confirmation; the moved age groups, with their athletes, show `Junior Masters`; a move is a clean change, so
no reassignment dialog; Senior Masters is left with M65 and up, W65 and up.

Junior Masters was added last, so it is listed after Senior Masters. Marie drags its row (by the
handle at the left) onto the upper edge of the Senior Masters row. Expected: Junior Masters is now
listed first, and the age group list follows that order.

### 3. Marie notices Senior Masters starts at 65, and creates two age groups of her own

"Senior Masters should start at 55, and there is nothing there." She does not notice that M55 and M60
are sitting in Junior Masters. In Senior Masters she adds an age group with the natural code `M55` (55 to 59), men,
with the same weight categories as the 65 group.

Expected: the code field is refused with "The code and gender combination "M55 M" is already
used." (the check looks at every age group of every championship); the Add button does not
create anything.

Puzzled, she does not look for the clash. She settles on a prefix for Senior Masters and adds `VM55`
for the men and `VW55` for the women, with the same weight categories as the 65 group. The form
fills the Championship from the code, so she picks Senior Masters herself; new age groups are
created inactive. For the women she types 55 to 59. For the men she slips: `VM55` is typed 55 to 58.

Expected: creation succeeds without a dialog or warning: a new age group is inactive, and inactive
age groups do not count in the age-gap check. She activates `VM55` at once, and that is where the
gap is signaled: Senior Masters would then have 55 to 58 (women: 55 to 59) and 65 and up, and the
age-gap dialog for Men aged 59-64 (women: 60-64) with Continue / Cancel appears, since there is no
60 group yet. No other page shows which ages a championship covers.

She continues, then adds `VM60` and `VW60`. `VW60` is typed correctly (60 to 64) and its activation
closes the gap: no dialog. For the men she slips again and types `VM60` as 60 to 65. Activating it
shows one dialog with **two** problems: the gap at age 59 (since `VM55` stops at 58), and an
**overlap** with `M65` at age 65. An overlap is always reported on activation, as a warning like a
gap, with Continue / Cancel. She continues, then corrects `VM55` to 55 to 59 and `VM60` to 60 to 64. The
check always lists **all** the gaps and **all** the overlaps of the championship, so she sees what is
left to fix: after the `VM55` correction the dialog still reports the overlap at 65; after the `VM60`
correction nothing is left and the save shows no dialog. Ages 55 and up are covered in Senior
Masters.

### 4. Marie discovers the overlap and starts trimming Junior Masters: the mistake

Junior Masters now contains M55 and M60 (and W55, W60), which Senior Masters already covers. She decides to delete
them, and starts with **M55**, the first one she sees.

*Edit Age Groups*, open M55, Delete, confirm "Delete M55?".

Expected, one dialog titled "Age Gap":

- "Deleting M55 would leave Junior Masters with no active age group for Men aged 55-59."
- below it the explanation and the list of athletes who would lose Junior Masters, for example
  `Name (removed: M55 81; age)`;
- buttons Delete and Cancel.

Marie reads "55 to 59", realises M60 would be left hanging above a hole, and presses **Cancel**.

Expected: nothing is deleted; M55 is still in the grid; the athletes are unchanged.

### 5. Marie deletes from the top: the right order

She opens **M60**, Delete, confirms.

Expected: no age-gap text (M60 is the highest age group of Junior Masters, so no hole is opened); the same
dialog shows only the athletes who would leave Junior Masters (`removed: M60 ...; age`); she presses
**Delete**. M60 disappears, the athletes stay registered, and the audit log has an entry for the
delete.

Then **M55**, which is now the top: again no gap text, the athletes listed, Delete. Same for W60
then W55.

Expected: Junior Masters covers 30 to 54 for both genders, with no holes.

### 6. The athletes of 55 and over are not in Senior Masters yet

Marie looks at the registration list filtered on Senior Masters: the 55 to 64 lifters are not there.
They left Junior Masters but were never in Senior Masters, and deleting an age group only reassigns inside its
own championship.

She presses **Recompute all eligibilities** and reads the dry run: the 55 to 64 lifters are
listed as `added: VM55 ...` or `added: VM60 ...`. She applies.

Expected: these athletes now have participations in `VM55` or `VM60`, and their registration
category is set; athletes aged under 55 are untouched.

### 7. Marie gives the new age groups the proper names

"VM55" and "VM60" were only there to avoid the clash with M55 and M60. Now that the Junior Masters ones are
gone she opens `VM55` in Senior Masters, changes the code to `M55`, and Update. Same for `VM60` to `M60`,
and for the women's groups.

Expected: a plain rename is saved directly, with no age-gap or reassignment dialog; the age
group keeps its athletes and participations; the category codes follow the new code (`V55 81`
becomes `M55 81`) in the grid, the athlete list and the Category column; Senior Masters shows M55, M60,
M65 and up, and Junior Masters shows M30 to M50. The same renames done before step 5 would have been refused,
because Junior Masters still had an M55 and an M60.

## What each step verifies

| Step | Item verified |
| --- | --- |
| 1 | Championship rename propagates to age groups, participations kept |
| 2 | A move needs no reassignment; the move dialog covers both championships; dragging a championship row reorders the list |
| 3 | A duplicate code and gender is refused at creation, across championships; a creation with another code has no dialog |
| 4 | Delete of a middle group: gap warning worded for deletion, athletes listed, Cancel deletes nothing |
| 5 | Delete of the top group: no gap text, athletes listed, Delete removes the group, one audit entry, order top first leaves no hole |
| 6 | Per-group changes never add athletes to a championship; Recompute all does, with a dry run |
| 7 | A code rename is saved directly; participations kept; category codes recomputed; the duplicate code and gender rule applies across championships |

## Handy ids for the browser run

Delete confirmation: the Delete button of the "Delete M55?" dialog. Delete dialog:
`agegroup-delete-apply`, `agegroup-delete-cancel`. Reassign preview from Recompute all:
`agegroup-global-apply`, `agegroup-global-cancel`. Editor: `agegroup-editor-update`.

Use a real mouse click on the confirmation dialog's Delete button (script `click()` on it does
not reach the server reliably), and reload the page on the Edit Age Groups page between steps.

## Not covered here

- Creating an age group that opens a hole: creation runs no gap check (spec 3.6).
