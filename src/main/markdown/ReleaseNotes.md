**Version 51.0 alpha**

> [!CAUTION]
>
> - This is an **alpha release**, used for validating new features.  *Some features are likely to be incomplete or non-functional*.  
> - **Alpha releases are not normally used in actual competitions.**

- (alpha01) Publicresults
  - When a user opens a scoreboard and a timer is running, the timer is now immediately synchronized
  - If the publicresults application is restarted, and sessions are in a break, the remaining time is now immediately synchronized
- Age-based, all body weights Categories (ABAB)
  - Initial support for categories based on age only, medals awarded based on a selected scoring system
    - Typical use: create categories where all Masters in the same age group compete together in a category, from 0 to 999 kg bodyweight, based on their SM(H)F score.
    - Other possible scenario: All youth in a given age group compete against one another based on Sinclair.
  - To create, each age group is given a single category  with 0 999 age limits and 0 999 bodyweight limits
  - The scoring system for each age group is selected in the AgeGroups file 
    - If the column H is named "ageGroupScoring" can be inserted, it is used
    - The available scores are TOTAL, BW_SINCLAIR, CAT_SINCLAIR, SMM, ROBI and GAMX.  Others will added in the future (such as youth age-graded totals)
    - Different age groups can have different scoring systems
    - **Note**: the Medals sheets and displays do NOT take the scoring into account.  Use the Session or Competition eligibility category reports to award medals, using the "Score" template.
  - There can be both the open bodyweight age groups and the normal age groups in the same meet.  Just assign the desired ranking method to each age group.


For other recent changes, see [version 50 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0)
