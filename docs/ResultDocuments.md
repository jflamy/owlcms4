From the `Results` page, it is possible to get the various competition result documents.

![010_results](img/ResultDocuments/010_results.png)

## Session Results

The Session Results button opens a page where all athletes are listed.  By selecting the group, the list is narrowed to the group.  Clicking on an athlete brings up the athlete card, in case a correction is required.

There are two kinds of session results:

- The "Registration Category" list shows each athlete *once*, under their registration category This it the traditional protocol result sheet.  In a joint Junior and Senior championship, if an athlete is registered as a Junior, but is eligible to lift as a Senior, the athlete will show up as a Junior in this document.
- The "Eligibility Category" list show the athletes multiple times if they are eligible to multiple categories.  Using the same example, the athlete would show up twice, once as a Junior (with the ranking as a Junior) and once as a Senior (with the ranking as a Senior).

In this example, we select the Registration Category, but the process is identical for Eligibility Categories.

![020_selectGroup](img/ResultDocuments/020_selectGroup.png)

Then select "Group Results", the template you want, and click "Download" . The different templates show slightly different information, and use different paper sizes.  You can also add your own.

![021_selectTemplate](img/ResultDocuments/021_selectTemplate.png)

The document is downloaded -- the exact location to retrieve the document depends on your browser.  In the case of Chrome, the download is shown at the bottom. Edge and Firefox show the downloads at the top right.
![022_download](img/ResultDocuments/022_download.png)

Clicking on the file opens Excel (or your spreadsheet editor if you use another one). After clicking on "Enable Editing" when the file opens, you get the protocol sheet:

![023_ExcelProtocol](img/ResultDocuments/023_ExcelProtocol.png)

## Medals

In order to have a smooth medal ceremony, printing out the medals sheet is recommended.  The medals sheet will only list the categories that are completed.  This is very useful when there are multiple A/B/C groups and sessions are mixed.

The resulting Excel shows all medals in the Bronze, Silver, Gold order.  Whether there are medals for all lifts or just the total is controlled on the competition rules page.

> Note:  If [Score-based Medals](ScoreBasedCompetitions) are awarded for some age groups, this is not reflected in the Medals sheet.  You will have to use the `Scores` final package template to get a printable document.

![031_medalsExcel](img/ResultDocuments/031_medalsExcel.png)

## Team Results

On the `Results` Page, the `Team Results` button shows how many points have been scored by each team, using the IWF formula.  When non-standard point ranges are used, or if different championships use different scoring schemes, it is more convenient to produce a Final Package spreadsheet and look things up (see below.)

## Competition Results

The Competition Results page is used to produce the overall results for a competition.  It can be used during a competition to produce interim results, or at the end.

There are three types of competition results

- Registration Category Results: Each athlete appears *once*.  This is normally sent to the Federation after the competition.  All athletes appear under the category under which they were registered and lifted.
- Registration Category Results: This is typically posted as competition results.  Athletes appear in all the categories where they were eligible, with their rank in each category.  If an athlete won several medals, this document will show it.
- Final Package.  This contains the Eligibility Category Results and other Tabs for the best lifter awards.  It also contains the medals won, and the points scored per each teams.

### Championship Selection

Sometimes two or more championships are held together during the same meet.  For example, there can be a Youth Championship the same weekend as a Junior Championship, etc.  Some federations even have all their championships together.

In the following picture, two things are shown

- We want to produce the results only for the Masters Championships

- We want to specify a preference for the SM(H)F scoring system to be used for the Best Lifters sheet in the final package

  > Note: currently, only the `Scores` final package takes this setting into account, see below.  But in upcoming releases the other templates will be modified to do so.

![ChampionshipSelection](img/ResultDocuments/ChampionshipSelection.png)

### Final Package

The "Final Package" package contains several tabs.

- Eligibility Category results
- One or more pages for different "best lifter" scoring schemes (Sinclair, Robi, Q-points, etc.)
- Team Scores (points and medals), see the [section below](#team-points-and-medals)

Four templates are commonly used

- The `Total` template is used for competitions that awards medals and points for Total only.
- The `SnCjTotal` template is used for competitions that award three separate medals, as well as points for all the lifts.
- The `Masters` template is used for Masters events -- it shows the SM(H)F and Q-Masters scores used to determine the Grand Master awards.
- The `Scores` template is used in two circumstances
  - The first one, already mentioned is when a championship uses a specific Best Lifter award.  For example, a Youth championship might use the Q-Youth formula instead of Sinclair, or an IWF championship might use the Q-points or GAMX formula.
  - The second one is when Score-based medals are awarded to the groups.  For these age groups, the computed score will be used to order the results.

### Team Points and Medals

The team ranking tabs shows the team cumulative points, and the details for each athlete at the bottom.

If you wish to sort the points table, please note that due to the way Excel works you will need to copy the table area somewhere else in the page and use the "Paste as values" option before you are able to sort it.

![061_pasteValues](img/ResultDocuments/061_pasteValues.png)

## Time Statistics

The time statistics Excel shows how much time was spent per lift in each group, on average.  It gives the number of athletes that can lift in an hours - if the number is 8 athletes per hour, then a 12 person group will take 90 minutes, on average.  Youth championships often go faster (9 or 10), whereas senior championships are slower (7,5 athletes per hour for example).