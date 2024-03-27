**Version 48.0 alpha**

> [!CAUTION]
>
> - This is an alpha release, used for validating new features.  *Features may be incomplete or non-functional*.  
> - Alpha releases are **not** normally used in actual competitions.

##### 48.0

- Clean-up of the JSON information sent to the /update endpoint of publicresults, to support extraction of information for video production
  - in the athlete information, the previous field `goodBadClassName` is now `liftStatus`
  - in the group information, there are now two fields, `groupName` and `groupInfo`.  `groupName` now contains only the group name. `groupInfo` contains what is shown on the second line of the scoreboard (group description + attempts done, etc.) 

- Under Feature Toggle AthleteCardEntryTotal, show the Entry Total in the title of the Athlete Card.
- Jury Sheet labels can now all be translated.

