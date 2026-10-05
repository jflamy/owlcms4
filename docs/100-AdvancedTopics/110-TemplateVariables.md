## Excel Templates

The documents produced by the system are in Excel format.  They are produced by reading an Excel file (a *template*) and reading special instructions inside the template.  When producing a document, owlcms extracts information and feeds it into the template.

The templates are located in the `local/templates` directory that you can reach using the `Files` button on the control panel (next to the version number you are using) . There is a subdirectory for each type of report that contains the various templates available.  You can remove files you don't use or rename them.  The update program will only overwrite the remaining files that have not been touched, so you won't lose your changes.

The software library used to process the templates is called `jxls`.

- Older templates were created using version 1 of jxls.  They can be recognized by the presence of instructions with angle brackets, like `<jx:foreach>`
- The newer templates use version 3 of jxls, which is many times faster.  That version puts the instructions inside Excel notes.  See the documentation at [https://jxls.sourceforge.net/](https://jxls.sourceforge.net/)

### Template Variables

When writing a template, you can get information about athletes, sessions, etc.  The best way to find about them is to look at existing templates.

Substitutions follow the JEXL conventions, so you can actually chain accesses.

#### General Variables

The following variables are set for every template

| Variable    | Notes                                                        |
| ----------- | ------------------------------------------------------------ |
| t           | Transation map.  All the entries in the translation map are available by using `${t.get('key')}` where `key` is an entry in the translation file. |
| competition | The current Competition object.  All the methods are available, for example ${competition.name} |
| session     | The current session name, when pertinent.  ${session} gives the session name, ${session.description} the details if any were provided |
| platforms   | An array of platforms.                                       |
| local       | local is an object that allows retrieving images located in the local directory.   Any image or picture file in PNG or JPEG format found in the local directory can now be included in the jxls3 Excel templates using the [jx:image](https://jxls.sourceforge.net/image.html) directive.<br /><ul><li>For example `jx:image(src="local.getBytes('logos/right.png')" lastCell="B3")` in cell B2 would copy the image local/logos/right.png in the range B2:B3. </li><li>Note that .jpg pictures require `imageType="JPEG"` in the directive (careful to use JPEG as the image type, not JPG).  `jx:image(src="local.getBytes('pictures/4123.jpg')" imageType="JPEG" lastCell="B3")`</li></ul> |

#### Best-athlete lists and accessors

Use these variables in final-package and category-results templates:

| Variable | Notes |
| -------- | ----- |
| `mBest` | Male athletes in best-athlete order, ranked with the selected best-athlete scoring system. |
| `wBest` | Female athletes in best-athlete order, ranked with the selected best-athlete scoring system. |
| `bestRankingTitle` | Name of that scoring system, suitable for the best-athlete heading. |
| `l.bestAthleteRank` | Rank of an athlete in the current best-athlete list: positive ranks start at 1; 0 means no rank; -1 means Extra/Invited. This is the preferred rank accessor for new templates. |
| `l.bestLifterRank` | The same best-athlete rank, with presentation rules for Extra/Invited athletes and hiding unfinished results applied. Supported, not deprecated. |
| `l.bestLifterScore` | Score computed using the selected best-athlete scoring system. Supported, not deprecated. |

`l` is the athlete variable of a loop over `mBest` or `wBest`. For example,
a JXLS3 note can contain `jx:each(items="mBest" var="l" lastCell="D12")`, with
`${l.fullName}`, `${l.bestLifterScore}` and `${l.bestAthleteRank}` in the
repeated cells. Choose `lastCell` for the actual layout of your template.

Each list contains one reporting `PAthlete` per athlete, even when the athlete
has several participations in the selected championship. A reporting `PAthlete`
has a private copy of its participation. The report always recalculates ranks
on these copies after applying its filters; it does not overwrite the stored
championship ranks.

The championship selector filters the population. Selecting a championship
initializes the scoring selector to its best-athlete system, but an explicit
choice in the scoring selector controls the report. Clearing the championship
keeps the selected scoring system and allows a report over all displayed
athletes. Ranks are assigned independently by gender.

The former per-system best-athlete lists, such as `mSmm`, `wSmm`,
`mCatSinclair`, `wCatSinclair`, `mRobi` and `wRobi`, are no longer produced.
Replace those loops with `mBest` / `wBest`, and use the generic accessors above.
Category and team lists are unchanged.

Scores for a specific formula, such as `${l.sinclair}`, `${l.smm}`,
`${l.qAge}` or `${l.gamx}`, can still be displayed: they are computed on demand.
They are not necessarily the score used to rank the current list. For example,
when GAMX-M is selected, `bestLifterScore` is GAMX-M and `bestAthleteRank` is
the rank by GAMX-M; `sinclair` remains a Sinclair score.

#### Deprecated best-athlete rank aliases

The following accessors are deprecated but currently retained for custom
templates. **Every one returns `bestAthleteRank`, regardless of its historical
formula name.** None calculates a separate rank for that formula.

The rank is held by a `Participation`, not by an `Athlete`:

- A reporting `PAthlete` in `mBest` / `wBest` reads its private participation
  copy. The report assigns that copy a rank using the selected scoring system
  and the report's filtered population.
- A plain `Athlete` delegates to its main (registration-category)
  participation. That participation holds the official rank calculated among
  the participants of its championship, using the championship's best-athlete
  system. Report filters and the report's scoring selector do not change it.

The deprecated meet-wide rank fields on `Athlete` are no longer read or
computed, and are neither exported nor imported.

| Template variable | Deprecated Java accessor |
| ----------------- | ------------------------ |
| `sinclairRank` | `getSinclairRank()` |
| `catSinclairRank` | `getCatSinclairRank()` |
| `catQPointsRank` | `getCatQPointsRank()` |
| `catGAMXRank` | `getCatGAMXRank()` |
| `robiRank` | `getRobiRank()` |
| `smhfRank` | `getSmhfRank()` |
| `smmRank` | `getSmmRank()` |
| `qPointsRank` | `getqPointsRank()` |
| `qMastersRank` | `getQMastersRank()` |
| `qAgeRank` | `getqAgeRank()` |
| `qYouthRank` | `getQYouthRank()` |
| `ageAdjustedTotalRank` | `getAgeAdjustedTotalRank()` |
| `gamxRank` | `getGamxRank()` |
| `gamxMRank` | `getGamxMRank()` |
| `gamxMSRank` | `getGamxMSRank()` |
| `gamxMCRank` | `getGamxMCRank()` |
| `gamxURank` | `getGamxURank()` |
| `gamxARank` | `getGamxARank()` |
| `gamxSRank` | `getGamxSRank()` |
| `gamxCRank` | `getGamxCRank()` |

For example, replace `${l.robiRank}` with `${l.bestAthleteRank}`, and use
`${l.bestLifterScore}` for the matching score. The aliases remain available
pending a decision on removing them; the specific score accessors are not
deprecated by this rank refactor.

#### Athlete information

| Variable                       | Notes                                                        |
| ------------------------------ | ------------------------------------------------------------ |
| 20kgRuleValue                  | 20 for IWF competition, 15 or 10 for Masters, if the Initial Total rule is in effect. |
| age                            | Age as of current date (beware, not the date of competition) |
| ageGroup                       |                                                              |
| attemptedLifts                 | same as attemptsDone                                         |
| attemptNumber                  | Attempt number for current lift (1..3)                       |
| attemptsDone                   | Lifts attempted (0..6)                                       |
| bestAthleteRank                 | Best-athlete rank in the current ranking context; preferred rank accessor. See the best-athlete section above. |
| bestCleanJerk                  |                                                              |
| bestCleanJerkAttemptNumber     | (1..3)                                                       |
| bestLifterRank                  | Best-athlete rank with results-presentation rules applied; supported, not deprecated. |
| bestLifterScore                 | Score using the selected best-athlete scoring system.        |
| bestResultAttemptNumber        | (1..6) used when reaching back to snatch is necessary to tie break |
| bestSnatch                     |                                                              |
| bestSnatchAttemptNumber        | (1..3)                                                       |
| bodyWeight                     |                                                              |
| bWCategory                     | Category limit without gender (example: 67  or >109)         |
| category                       | Full category name including age group and gender            |
| categoryScore                  | Value used by this category's medal scoring system.          |
| categoryScoreRank              | Rank within the category by its medal scoring system; equals totalRank for Total medals. |
| categorySinclair               | Sinclair calculated at bodyweight category instead of bodyweight |
| cleanJerk1ActualLift           | positive if lift was good, negative if lift was failed, 0 if declined by athlete, empty if not yet taken. |
| cleanJerk1AsInteger            | same as actual lift except null if not taken                 |
| cleanJerk1AutomaticProgression |                                                              |
| cleanJerk1Change1              |                                                              |
| cleanJerk1Change2              |                                                              |
| cleanJerk1Declaration          |                                                              |
| cleanJerk1LiftTime             |                                                              |
| cleanJerk2ActualLift           | positive if lift was good, negative if lift was failed, 0 if declined by athlete, empty if not yet taken. |
| cleanJerk2AsInteger            |                                                              |
| cleanJerk2AutomaticProgression |                                                              |
| cleanJerk2Change1              |                                                              |
| cleanJerk2Change2              |                                                              |
| cleanJerk2Declaration          |                                                              |
| cleanJerk2LiftTime             |                                                              |
| cleanJerk3ActualLift           | positive if lift was good, negative if lift was failed, 0 if declined by athlete, empty if not yet taken. |
| cleanJerk3AsInteger            |                                                              |
| cleanJerk3AutomaticProgression |                                                              |
| cleanJerk3Change1              |                                                              |
| cleanJerk3Change2              |                                                              |
| cleanJerk3Declaration          |                                                              |
| cleanJerk3LiftTime             |                                                              |
| cleanJerkAttemptsDone          |                                                              |
| cleanJerkPoints                |                                                              |
| cleanJerkRank                  |                                                              |
| cleanJerkTotal                 |                                                              |
| club                           | same as team (team should be preferred)                      |
| combinedPoints                 |                                                              |
| currentAutomatic               |                                                              |
| currentChange1                 |                                                              |
| currentDeclaration             |                                                              |
| customPoints                   | Not yet in owlcms4                                           |
| customRank                     | Not yet in owlcms4                                           |
| customScore                    | Not yet in owlcms4 -- used to override the total by using a formula (e.g. technical points for certain kid competitions, bonus points based on local traditions, etc.) |
| displayCategory                | same as category, but safe to use if athletes have not all registered yet. |
| firstAttemptedLiftTime         |                                                              |
| firstName                      |                                                              |
| forcedAsCurrent                | not relevant for reports                                     |
| formattedBirth                 | should be used for Excel if you need the full birth date (as on a starting/registration list) -- use yearOfBirth if dealing with a narrow column and where the full birth date is not essential. |
| fullBirthDate                  | see formattedBirth                                           |
| fullId                         | full identification of the athlete with start number and category, as a single string |
| fullName                       | lastname, firstname as a single string                       |
| gender                         | M(ale) or F(emale)                                           |
| group                          | the athlete's group                                          |
| lastAttemptedLiftTime          |                                                              |
| lastName                       |                                                              |
| lastSuccessfulLiftTime         |                                                              |
| liftOrderRank                  |                                                              |
| longCategory                   | same as displayCategory                                      |
| lotNumber                      |                                                              |
| mastersAgeGroup                | legacy.  can be used to display the ageGroup                 |
| mastersAgeGroupInterval        | age boundaries for the age group.  works for all ageGroups   |
| mastersGenderAgeGroupInterval  | same as mastersAgeGroupInterval but with the Gender          |
| mastersLongCategory            | same as displayCategory                                      |
| medalRank                      | 1..3 or empty, based on Total                                |
| membership                     |                                                              |
| nextAttemptRequestedWeight     |                                                              |
| presumedBodyWeight             | body weight as inferred from the category (if not weighed-in) or the actual bodyweight (if weighed-in) |
| previousLiftTime               |                                                              |
| qualifyingTotal                | currently ambiguous.  the same field is also used as the entry total for the 20kg rule. |
| rank                           | rank according to total.  for the protocol sheet, rank within the current group (will be fixed) |
| registrationCategory           | obsolete -- same as category                                 |
| robi                           |                                                              |
| robiRank                       | Deprecated alias of bestAthleteRank, not a separately computed ROBI rank. |
| shortCategory                  | obsolete - use bWCategory                                    |
| sinclair                       |                                                              |
| sinclairFactor                 |                                                              |
| sinclairForDelta               | kg needed to get to best sinclair ranking                    |
| sinclairPoints                 |                                                              |
| sinclairRank                   | Deprecated alias of bestAthleteRank, not a separately computed Sinclair rank. |
| smm                            |                                                              |
| snatch1ActualLift              | positive if lift was good, negative if lift was failed, 0 if declined by athlete, empty if not yet taken. |
| snatch1AsInteger               |                                                              |
| snatch1AutomaticProgression    |                                                              |
| snatch1Change1                 |                                                              |
| snatch1Change2                 |                                                              |
| snatch1Declaration             |                                                              |
| snatch1LiftTime                |                                                              |
| snatch2ActualLift              | positive if lift was good, negative if lift was failed, 0 if declined by athlete, empty if not yet taken. |
| snatch2AsInteger               |                                                              |
| snatch2AutomaticProgression    |                                                              |
| snatch2Change1                 |                                                              |
| snatch2Change2                 |                                                              |
| snatch2Declaration             |                                                              |
| snatch2LiftTime                |                                                              |
| snatch3ActualLift              | positive if lift was good, negative if lift was failed, 0 if declined by athlete, empty if not yet taken. |
| snatch3AsInteger               |                                                              |
| snatch3AutomaticProgression    |                                                              |
| snatch3Change1                 |                                                              |
| snatch3Change2                 |                                                              |
| snatch3Declaration             |                                                              |
| snatch3LiftTime                |                                                              |
| snatchAttemptsDone             | 0..3                                                         |
| snatchPoints                   |                                                              |
| snatchRank                     |                                                              |
| snatchTotal                    |                                                              |
| startNumber                    |                                                              |
| team                           |                                                              |
| teamCleanJerkRank              | unused so far - computed by Excel                            |
| teamCombinedRank               | unused so far - computed by Excel                            |
| teamMember                     |                                                              |
| teamRobiRank                   |                                                              |
| teamSinclairRank               | unused so far - computed by Excel                            |
| teamSnatchRank                 | unused so far - computed by Excel                            |
| teamTotalRank                  | unused so far - computed by Excel                            |
| total                          |                                                              |
| totalPoints                    |                                                              |
| totalRank                      | Rank within the category by kg total; distinct from categoryScoreRank when medals are awarded by score. |
| yearOfBirth                    | 4-digit year of birth                                        |
| isEligibleForIndividualRanking | false if athlete is competing out-of-competition (as invited lifter from another state in a state championship, for example) |
| isEligibleForTeamRanking       | unused so far                                                |