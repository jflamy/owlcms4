**Version 48.0 beta**

> [!WARNING]
>
> - This is a beta release, used for testing and translation.  *Some features could be non-functional*.  
> - Beta releases are **not** normally used in actual competitions, except if a new feature is required.  Use extreme care in testing if you intend to do so.

##### 48.0

- (beta09) Event Forwarding: publicresults and video listeners now get all information aboutceremonies and jury decisions, in addition to breaks and interruptions.  Jury decisions are sent on the `/decision` endpoint.
  
- (beta08) Break Management:
  - It is now possible to go from an interruption (for example a Technical Pause because platform is broken) to a timed countdown (after repair, timed warmup before resuming), and vice-versa
  - Spurious interruptions of the countdown are now prevented and signalled to the announcer (e.g. normally a technical pause during a countdown should not interrupt the count, and if stopping the countdown is indeed required a decision on when to restart will be required).

- (beta08) Fix: exporting all records from an empty database (to obtain a template) yielded a file with an incorrect validation criterion.
- (beta07) Publicresults/video updates: fixed issues with athlete timers and break timers updates. The header text shown during breaks matches the owlcms scoreboard.
- (beta06) publicresults now behaves the same as the announcer scoreboard wrt the showing/hiding of the ranks of individual lifts.  Individual lift ranks are shown if the option for separate medals is selected in the competition options.
- (beta05) Fixed pre-existing issue (v47 or before) with wrong values for the positions shown on scoreboard when using global rankings like Sinclair.
- (beta04) Fixed intermittent issues with lists using the registration sort order and re-enabled unit tests that had been wrongly thought to be false positives.
- (beta03) Fixed sequencing of actions during JSON database restore to ensure Championships, Platforms and MQTT monitors are reset correctly.
- (beta02) Fixed the AgeGroups definition file export to include both the Championship name and type.
- (beta02) Accept a registration file where there are explicit categories but no athlete birth date.
- (beta01) Now exporting the SBDE dates in ISO yyyy-MM-dd format to avoid issues when reading in a different locale.
- (beta01) An English-language initial registration file will always be understood, in addition to the current language.
- (alpha06) Fix: The SBDE reader was inverting the declarations and personal best values.
- (alpha05) Event forwarding now includes all UI events, so publicresults now correctly reflects the start and end of ceremonies.  Event type names in the published JSON are now separate (timer, break, and decision subtypes have been added)
- (alpha04) The second column, previously empty, of the AgeGroups file is now used for a Championship Name. The third column is the championship type.  A Championship Name typically corresponds to one final package with its own teams and its own medals.
  The built-in AgeGroups files have been updated, you can use them as example to update your own.
  - There is a single tab in the file.  The tab with Robi records is removed.

  - For Youth, Jr and Sr championships, or U13 U15 U17 age groups championships that take place simultaneously, put a championship name on each row, because each normally has its own teams and medals.

  - For Masters championships, you can put "Masters" in your local language as the championship name on all the Masters lines
    - If you have multiple Masters championships taking place simultaneously, you would have two sets of age groups. One would have (for example) "PanAm Masters" as the championship name (with all the age groups named normally "M35").  You would also add all the age groups a second time, with a name like "SouthAmerican Masters", and name all the age groups as "SA M35".  The M35 would be the registration category, and the "SA M35" would be an eligibility category for South American athletes.

  - You can have "combined age group championships" if you wish.  This is the same as for Masters -- you put the same championship name on the age groups that belong together.  You might have a combined U15 and U17 championship in high schools where there is a single combined team score, you would have the same championship name for both age groups.
- (alpha04) The weigh-in sheet has been fixed for formatting issues and is now in Portrait mode, for both US letter and international A4 formats.
- (alpha03) Weigh-in: a Quick Mode is added when a weigh-in sheet is available.  When selected, the entry form has an "Update and Next" button that switches to the next athlete in the weigh-in order, so it is not necessary to select the athlete.
- (alpha02) Additional output URL for structured JSON data, to be used for creating video overlays. The information emitted is identical to what is sent to publicresults.  Video production companies can build a custom web server to receive this information and transform the data as required by their video software.
- (alpha02) /update endpoint is updated every 15 seconds.  publicresults computes a hashcode before putting the event on its event bus. Individual publicresults user sessions compare the hashcode and ignore duplicate events.  This deals with publicresults being restarted randomly and users joining in at random times.
- Clean-up of the JSON information sent to the /update endpoint of publicresults, to support extraction of information for video production
  - in the athlete information, the previous field `goodBadClassName` is now `liftStatus`
  - in the group information, there are now two fields, `groupName` and `groupInfo`.  `groupName` now contains only the group name. `groupInfo` contains what is shown on the second line of the scoreboard (group description + attempts done, etc.) 
- If the Feature Toggle `AthleteCardEntryTotal` is enabled, show the Entry Total in the title of the Athlete Card.
- Jury Sheet labels can now all be translated.

