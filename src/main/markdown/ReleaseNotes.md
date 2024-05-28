**Version 49 alpha**

> [!CAUTION]
>
> - This is an **alpha release**, used for validating new features.  *Some features are likely to be incomplete or non-functional*.  
> - **Alpha releases are not normally used in actual competitions.**

- (alpha06) Signal withdrawal of athlete to TOs and Jury.
  
- (alpha06) A done athlete can no longer be highlighted as next on the announcer and marshal screens
  
- (alpha06) Technical: prevent overlapping updates of global rankings triggered from different platforms
  
- (alpha05) added a new data structure for use when creating jxls3 templates.  Given a session object, the following are available
  - session.ageGroupInfo.nbAthletes
    session.ageGroupInfo.weightClassRange
    session.ageGroupInfo.ageGroup.gender  (.code, .name etc.)
    session.ageGroupInfo.athletesByStartNumber
    session.ageGroupInfo.athletesByEntryTotal
  
- Competition Results
  - The "End of Competition" results can now be produced in the middle of the competition.  By default, categories that are not finished are not included.
  - There is now a separate directory for the competition results templates (previously it was the same as the protocol sheets)
- Announcer+Marshal
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
- Team flag preview: 
  - The team membership page now shows the flag for each team, allowing a quick check that all are correctly assigned.
- Documents:
  - The Weigh-in Form now includes the participation categories so the coach can sign them off and they can be cross-checked during data entry.  This is useful when there are multiple championships with the same categories.
  - Additional options to get Session Date/Time for Excel templates: the following values are now available on the session object (for example `${session.localWeighInDay}` would give the short date for weigh-in using the current country settings).
    - Using the local formatting conventions for dates: `localWeighInDay`, `localWeighInHour`, `localStartDay`, `localStartHour`
    - Using the international ISO format: `intlWeighInDay`, `intlWeighInHour`, `intlStartDay`, `intlStartHour`
- Technical
  - Event Forwarding and MQTT event propagation refactoring. In previous releases, obsolete forwarders could accidentally be kept when reloading sessions.  This would cause the publicresults scoreboard to alternate between out-of-date and current results.

