**Version 51.0 alpha**

> [!CAUTION]
>
> - This is an **alpha release**, used for validating new features.  *Some features are very likely to be incomplete or non-functional*.  
> - **Alpha releases are not normally used in actual competitions.**

- (alpha03) Expose raw lift type and attempt number in video feed
- (alpha03) Removed translation steps for championship names now that the AgeGroups file allows defining arbitrary ones.
- (alpha03) The results scoreboard now shows the Score and Score Rank column if any category has age group scoring enabled.  The global settings for showing scores and score ranks are no longer needed for this use case.
- (alpha03) Huebner Age-adjusted totals are now supported. This adjusts the total based on age and body weight for athletes aged under 20 and under 115kg.
  - Note: the body weight is interpolated, whereas the on-line calculator from Huebner does rounding.

- (alpha02) Updated to Vaadin 24.4.7.
- Publicresults
  - When a user opens a scoreboard and a timer is running, the timer is now immediately synchronized
  - If the publicresults application is restarted, and sessions are in a break, the remaining time is now immediately synchronized
- Age-based, all body weights Categories (ABAB)
  - Initial support for categories based on age only, medals awarded based on a selected scoring system
    - Typical use: create categories where all Masters in the same age group compete together in a category, from 0 to 999 kg bodyweight, based on their SM(H)F score.
    - Other possible scenario: All youth in a given age group compete against one another based on Sinclair.
  - To create, each age group is given a single category  with 0 999 age limits and 0 999 bodyweight limits
  - The scoring system for each age group is selected in the AgeGroups file 
    - If the column H is named "ageGroupScoring" can be inserted, it is used
    - The available scores are TOTAL, BW_SINCLAIR, CAT_SINCLAIR, SMM, ROBI, AGEFACTORS and GAMX.
    - Different age groups can have different scoring systems
    - **Note**: the Medals sheets and displays do NOT take the scoring into account.  Use the Session or Competition eligibility category reports to award medals, using the "Score" template.
  - There can be both the open bodyweight age groups and the normal age groups in the same meet.  Just assign the desired ranking method to each age group.


For other recent changes, see [version 50 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0)
