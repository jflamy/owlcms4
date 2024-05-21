**Version 49 alpha**

> [!CAUTION]
>
> - This is an **alpha release**, used for validating new features.  *Some features are likely to be incomplete or non-functional*.  
> - Alpha releases are **not** normally used in actual competitions.

- (alpha02) The timekeeper can restart the clock even if a down signal has been given or a decision is shown.  This is required if referees mistakenly give reds when the bar has not reached the knees.
  
- (alpha01) Additional options to get Session Date/Time for Excel templates: the following functions are now available for use on the session object (for example: `${session.localWeighInDay}` would give the short date for weigh-in using the current country settings).
  - Using the local formatting conventions for dates: localWeighInDay, localWeighInHour, localStartDay, localStartHour
  - Using the international ISO format: intlWeighInDay, intlWeighInHour, intlStartDay, intlStartHour

- (alpha01) Scoreboards:
  - White is now used for good lifts on all scoreboards (previously some used green)
  - The layout now includes vertical spacing between the lifts for better readability.
- (alpha01)Team flag preview: 
  - The team membership page now shows the flag for each team, allowing a quick check that are all correctly assigned.
- (alpha01) Documents:
  - The Weigh-in Form now includes the participation categories so the coach can sign them off and they can be cross-checked during data entry.  This is useful when there are multiple championships with the same categories.
- (alpha01) Event Forwarding and MQTT event propagation:
  - In previous releases, obsolete forwarders could accidentally be kept when reloading sessions.  This would cause the publicresults scoreboard to alternate between out-of-date and current results.

