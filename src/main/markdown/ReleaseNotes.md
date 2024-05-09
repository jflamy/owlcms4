**Version 49 alpha**

> [!CAUTION]
>
> - This is an **alpha release**, used for validating new features.  *Some features are likely to be incomplete or non-functional*.  
> - Alpha releases are **not** normally used in actual competitions.

- (alpha01) Scoreboards:
  - White is now used for good lifts on all scoreboards (previously some used green)
  - The layout now includes vertical spacing between the lifts for better readability.

- (alpha01)Team flag preview: 
  - The team membership page now shows the flag for each team, allowing a quick check that are all correctly assigned.

- (alpha01) Documents:
  - The Weigh-in Form now includes the participation categories so they can be signed off by the coach and confirmed d during data entry.  This is useful when there are multiple championships with the same categories and the program signals a possible error in category selection.

- (alpha01) Event Forwarding and MQTT event propagation:
  - In previous releases, it could happen that more than one of each could exist for a platform.  This would cause the publicresults scoreboard to alternate between out-of-date and current results.

