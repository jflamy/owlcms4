> [!IMPORTANT]
>
> - You should test all releases, with actual data, several days before a competition. 

- Publicresults
  - When a user opens a scoreboard and a timer is running, the timer is now immediately synchronized
  - If the publicresults application is restarted, and sessions are in a break, the remaining time is now immediately synchronized
- Score-based Rankings
  - Initial support for Age-Based, All-Bodyweights Categories (ABAB), with ranking based on a scoring system.  See [Score-based Rankings](https://jflamy.github.io/owlcms4/#/ScoreBasedCompetitions) in the documentation.
    - Example: create categories where all Masters in the same age group compete together in a category, from 0 to 999 kg bodyweight, based on their SM(H)F score.
    - Example: All youth in a given age group compete against one another based on Sinclair.
    - **Note**: Use the `Score` templates the Session or Competition eligibility category reports to get the rankings and award medals. The Medals Excel sheets and and displays do not support scoring systems at present.
    - `Scores-*.xlsx` templates can be used as a simple Final package.  Category rankings by the age-group scoring system -- defaults to TOTAL. The Best Lifter page uses the global scoring system selected for the competition.  Team scores are based on the age-group scoring system.
  - Huebner "Age Factors"  are now supported. These multiplicative factors adjust the total based on age and body weight for athletes aged under 20 and under 115kg.
    - Note: the body weight is interpolated, whereas the current on-line calculator from Huebner does rounding.  The online calculator is meant to be updated to also use interpolation.
  - Huebner GAMX scoring system now supported
    - This scoring system aims to provide compatibility between men and women scores
  - There can be both the open bodyweight age groups and the normal age groups in the same meet.  Just assign the desired ranking method to each age group.
    - The results scoreboard shows a Score and Rank column when a scoring system is selected for any of the age groups.
    - The TOTAL is used as score for groups that do not have a Scoring System.
- Fix: Start Book Data Entry Spreadsheet now works when there are no athletes entered.


For other recent changes, see [version 50 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0)
