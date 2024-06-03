**Version 49 beta**

> [!WARNING]
>
> - This is a **beta release**, used for testing and translation. ***Some features could be non-functional***.
> - Beta releases are **not** normally used in actual competitions, except if a new feature is required. Use extreme care in testing if you intend to do so.

- (beta03) Documents
  - An awards schedule document is now available from the Results page.  It shows after what session the medals for a category can be awarded.
- Competition Results
  - The "End of Competition" results can now be produced in the middle of the competition.  By default, categories that are not finished are not included.
  - There is now a separate directory for the competition results templates (previously it was the same as the protocol sheets)
- Announcer+Marshal
  - Notification sent for withdrawal of athletes
  - The previous athlete is now highlighted in blue in the grid.  The current and next athletes are also highlighted (yellow and orange, which is the same color convention as on the default scoreboards).  Blue is shown when the previous athlete is the current or the next.
- Announcer+Timekeeper
  - The clock can be restarted even if a down signal has been given or a decision is shown.  Restarting the clock clears the decisions. This is required if referees mistakenly give reds when the bar has not reached the knees.
- Announcer
  - Refreshing the page correctly keeps the running time and shows the correct colors on the timer buttons.  Previously, refreshing the announcer page would reload the group and reset the timer.  Use the reload button to reload the group.
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
  - Added new accessors for use when creating jxls3 templates. 
    - session.ageGroupInfo.nbAthletes
      session.ageGroupInfo.weightClassRange
      session.ageGroupInfo.ageGroup.gender  (.code, .name etc.)
      session.ageGroupInfo.athletesByStartNumber
      session.ageGroupInfo.athletesByEntryTotal
    - athlete.ageGroupCodesAsString  (age group codes such as U17, ungendered unless Masters)
  - Footers for protocols, start lists and final results are now standardized to show the date of production.  The headers for final results show the championship and age group when selected.
  - The final package document now excludes unfinished categories by default and obeys the override checkbox when unfinished categories are required.
  - Timing statistics: the number of athletes per session is now correct.
- Technical
  - Event Forwarding and MQTT event propagation refactoring. In previous releases, obsolete forwarders could accidentally be kept when reloading sessions.  This would cause the publicresults scoreboard to alternate between out-of-date and current results.
  - Performance: Overall rankings for the selected "best lifter" scoring system (for example, the Sinclair score) are only computed if the option to show them on the scoreboard is selected.

