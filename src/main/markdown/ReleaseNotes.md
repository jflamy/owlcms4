**Version 51.0 alpha**

> [!WARNING]
>
> - This is a **beta release**, used for testing and translation. ***Some features could be non-functional***.
> - Beta releases are **not** normally used in actual competitions, except if a new feature is required. Use extreme care in testing if you intend to do so.

- (alpha01) Publicresults
  - When a user opens a scoreboard and a timer is running, the timer is now immediately synchronized
  - If the publicresults application is restarted, and sessions are in a break, the remaining time is now immediately synchronized
- All bodyweights, per Age Group Categories
  - Initial support for categories based on age group only, medals awarded based on a selected scoring system
    - Typical use: create categories where all Masters in the same age group compete together in a category, from 0 to 999 kg bodyweight, based on their SM(H)F score.
    - Typical use: All youth in a given age group compete against one another based on Sinclair.

  - To create, each age group is given a single bodyweight category  with the 999 upper limit.
  - The scoring system is selected in the AgeGroups file (a new column H named "ageGroupScoring" can be inserted, and is used if present)
  - Different age groups (e.g. youth, masters) can have their own scoring system
  - The available scores are TOTAL, BW_SINCLAIR, CAT_SINCLAIR, SMM, ROBI and GAMX.  Others will added in the future (such as youth age-graded totals)
  - Note: the Medals sheets and displays do NOT take this into account.  Use the Session or Competition eligibility category reports to award medals, using the "Score" template.
  - Note2: There can be both the open bodyweight age groups and the normal age groups in the same meet.  Just assign the desired ranking method to each age group.


See also [version 50 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0)
