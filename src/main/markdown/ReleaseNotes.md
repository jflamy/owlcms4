**Version 49.0.4**

> [!IMPORTANT]
>
> - You should test all releases, with actual data, several days before a competition. 

- (49.0.4) Fix: The ordering used for allocating start numbers should not consider age groups, only bodyweight category
  - Now fixed to apply only for Masters competition, or when the option to order weigh-in by age group is selected.
- (49.0.4) For readability, the athlete registration page, competition document page, and weigh-in page now list athletes first by session then by start ordering.  The sort order can changed by clicking the "Session" column header.
- (49.0.3) Fix: creating an Age Group interactively by using the Add button on the Edit Age Groups and Categories page had stopped working since version 48.
- (49.0.2) Enhancement: if a session is configured to start in the future, when opening the countdown screen, the default will be to start the introduction at the planned time. It will not be necessary to switch away from the "Duration" setting and not necessary to select the time.
- (49.0.2) Fix: In some rare edge cases, declaring the same value as the automatic progression would produce a denial warning on the marshal card.  This would happen when declaring on a 1:00 clock when the declaring athlete had lifted the same as the current athlete on the previous attempt.
- (49.0.2) Fix: Importing a database JSON export that includes a zip for local overrides now applies correctly the zipped overrides.
- (49.0.2) Fix: the `agegroupinfo.formattedRange` now correctly handles situations where athletes have not been assigned an A/B/C annotation.
- (49.0.1) Fix: Loading a registration file with English headers now works when the database has another language selected by default.
- (49.0.1) Fix: The current athlete display used for videos would occasionally display the scoreboard status from the previous athlete. 
- (49.0.1) Technical:  An improved implementation of the athlete and break timers is now enabled by default in this release.  In the very unlikely event you want the old behavior back use the `oldTimers` [Feature toggle](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/FeatureToggles).  
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
  - New Jury scoreboard highlights the previous athlete with a marker and the same color code as the marshal page
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
    - `athlete.ageGroupCodesAsString`  (age group codes such as U17, ungendered unless Masters)
    - `athlete.score` property for templating -- gets the current score according to the selected global scoring scheme
    - `formattedRange` summarizes the body weight categories and subcategories for an age group.  Output can be, for example, `55 B` if all the athletes are in that case, or `55-64 A` if all athletes are `A`.  If the athletes are not all in the same A/B/C subcategory, they are enumerated: `64A, 71B`.
    - lowestEntryTotal and highestEntryTotal for writing templates. 
    - `competition.translatedScoringSystemName` will return the header name for the selected scoring (Sinclair, Robi, ...)
  - Footers for protocols, start lists and final results are now standardized to show the date of production.  The headers for final results show the championship and age group when selected.
  - Competition Results
    - The final package document now excludes unfinished categories by default and obeys the override checkbox when unfinished categories are required.
    - By default, categories that are not finished are not included, so the results in the "End of Competition" section can now be produced in preliminary versions during the competition.
    - There is now a separate directory for the competition results templates (previously it was the same as the protocol sheets)
  - Timing statistics: the number of athletes per session is now correct.
  - IWF Start cards show the date at which the session is taking place
  - start-of-session documents check for missing start numbers and trigger generation if missing
  - computation of medals to be awarded correctly considers all unfinished categories
- Technical
  - Event Forwarding and MQTT event propagation refactoring. In previous releases, obsolete forwarders could accidentally be kept when reloading sessions.  This would cause the publicresults scoreboard to alternate between out-of-date and current results.
  - Performance: Overall rankings for the selected "best lifter" scoring system (for example, the Sinclair score) are only computed if the option to show them on the scoreboard is selected.
