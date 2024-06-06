**Version 49 release candidate**

> [!WARNING]
>
> - This is a [release candidate](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate), used for final public testing and translation.  *It is still a preliminary release*
> - You should test all releases, with actual data, several days before a competition.  This is especially important for release candidates.

- (rc01) Updated Jury document for jury member signature on the score calculation and inclusion of IWF decision reversal codes.
- (rc01) Results page now includes the selected scoring system in addition to Sinclair, SM(H)F and Robi.
- (rc01) Athlete names were missing on the team membership page.
- Announcer+Marshal
  - The previous athlete is now highlighted in blue in the grid.  The current and next athletes are also highlighted (yellow and orange, which is the same color convention as on the default scoreboards).  Blue is shown when the previous athlete is the current or the next.
  - A Notification is received when athletes withdraw from the snatch or the session.
- Announcer
  - Refreshing the page correctly keeps the running time and shows the correct colors on the timer buttons.  Previously, refreshing the announcer page would reload the group and reset the timer.  
  - Use the reload button at the top left to reload the athletes from the database.
- Single Referee Mode:
  - When selected in the announcer settings (⚙), a decision from any of the 3 refereeing devices or from the announcer is treated as a full decision.
- Announcer+Timekeeper
  - The clock can be restarted even if a down signal has been given or a decision is shown.  Restarting the clock clears the decisions. This is required if referees mistakenly give reds when the bar has not reached the knees.
- Group Selection
  - The announcer sees unfinished sessions first, sorted in ascending time (name if time unavailable)
  - Registration pages have session selection sorted in ascending time (or their name if the time is unavailable)
  - Results pages have session selection sorted with finished sessions first, most recent first (reverse name if unavailable)
- Weigh-in vs Categories
  - When entering the athlete's weight, the program keeps the current registration category and eligibility categories if the new weight is within the eligibility categories, even when it is not the youngest or most specific age group. 
  - To get the old behavior of automatic selection of the youngest age group, add the `bestMatchCategories` feature switch.
- Jury
  - The weight attempted on the previous attempt is now shown, to facilitate filling in a manual protocol sheet.
- Scoreboards:
  - White is now used for good lifts on all scoreboards (previously some used green)
  - The layout now includes vertical spacing between the lifts for better readability.
  - Record ordering at the bottom now goes from lowest-aged age group to highest, based on the maximum age of the age group. U13 before U17 before JR before U23 before SR before Open.
- Team flag preview: 
  - The team membership page now shows the flag for each team, allowing a quick check that all are correctly assigned.
- Documents:
  - The Weigh-in Form now includes the participation categories so the coach can sign them off and they can be cross-checked during data entry.  This is useful when there are multiple championships with the same categories.
  - Additional options to get Session Date/Time for Excel templates: the following values are now available on the session object (for example `${session.localWeighInDay}` would give the short date for weigh-in using the current country settings).
    - Using the local formatting conventions for dates: `localWeighInDay`, `localWeighInHour`, `localStartDay`, `localStartHour`
    - Using the international ISO format: `intlWeighInDay`, `intlWeighInHour`, `intlStartDay`, `intlStartHour`
  - An awards schedule document is now available from the Results page.  It shows after what session the medals for a category can be awarded.
  - Added new accessors for use when creating jxls3 templates. 
    - session.ageGroupInfo.nbAthletes
      session.ageGroupInfo.weightClassRange
      session.ageGroupInfo.ageGroup.gender  (.code, .name etc.)
      session.ageGroupInfo.athletesByStartNumber
      session.ageGroupInfo.athletesByEntryTotal
    - athlete.ageGroupCodesAsString  (age group codes such as U17, ungendered unless Masters)
  - Footers for protocols, start lists and final results are now standardized to show the date of production.  The headers for final results show the championship and age group when selected.
  - Competition Results
    - The final package document now excludes unfinished categories by default and obeys the override checkbox when unfinished categories are required.
    - By default, categories that are not finished are not included, so the results in the "End of Competition" section can now be produced in preliminary versions during the competition.
    - There is now a separate directory for the competition results templates (previously it was the same as the protocol sheets)
  - Timing statistics: the number of athletes per session is now correct.
- Technical
  - Event Forwarding and MQTT event propagation refactoring. In previous releases, obsolete forwarders could accidentally be kept when reloading sessions.  This would cause the publicresults scoreboard to alternate between out-of-date and current results.
  - Performance: Overall rankings for the selected "best lifter" scoring system (for example, the Sinclair score) are only computed if the option to show them on the scoreboard is selected.

