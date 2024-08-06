> [!WARNING]
>
> - This is a release candidate [(see definition)](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate), used for final public testing and translation. *It is still a preliminary release*
> - You should test all releases, with actual data, *several days* before a competition. This is especially important when considering the use of a release candidate.

- Preliminary releases change log
  - (rc01) When defining categories with all bodyweights, a spurious blank was present in the display name, interfering with the ability to use the registration and SBDE files for these categories.
  - (rc01) Out-of-competition athletes now have their ranks on the scoreboards marked with a string ("ext." by default, defined as a translation string).  Translation strings also define a marker for the attempt board, and for the scoreboard name.  The scoreboard marker is empty by default since the ranks show the status.
  - (rc01) Fix: documentation for video styles now correctly references `local/css`
  - (beta04) Fix: SBDE export had stopped working if there were no athletes.
  - (beta03) Technical: when running on the cloud it is best to open the http port as early as possible.  Enabled a latch to wait until the database initialization and all other initializations are done before actually responding to outside requests.
  - (beta02) `Scores-*.xlsx` templates can be used as a simple Final package.  Category rankings by the age-group scoring system -- defaults to TOTAL. The Best Lifter page uses the global scoring system selected for the competition.  Team scores are based on the age-group scoring system.
  - (beta02) Added Qage score for Masters: Qpoints * Masters age factor.  Works with the `Scores-*.xlsx` templates.
  - (beta01) Documentation: added page for Score-based rankings using Age-Based All Bodyweight categories.
  - (beta01) Fixed AgeGroups export to include scoring system
  - (alpha03) Expose raw lift type and attempt number in video feed
  - (alpha03) Removed translation steps for championship names now that the AgeGroups file allows defining arbitrary ones.
  - (alpha03) The results scoreboard now shows the Score and Score Rank column if any category has age group scoring enabled.  The global settings for showing scores and score ranks are no longer needed for this use case.
  - (alpha03) Huebner Age-adjusted totals are now supported. This adjusts the total based on age and body weight for athletes aged under 20 and under 115kg.
    - Note: the body weight is interpolated, whereas the on-line calculator from Huebner does rounding.
  - (alpha02) Updated to Vaadin 24.4.7.
- Publicresults
  - When a user opens a scoreboard and a timer is running, the timer is now immediately synchronized
  - If the publicresults application is restarted, and sessions are in a break, the remaining time is now immediately synchronized
- Score-based Rankings
  - Initial support for Age-Based, All Body weights Categories (ABAB), with ranking based on a scoring system.  See [Score-based Rankings](https://jflamy.github.io/owlcms4/#/ScoreBasedCompetitions) in the documentation.
    - Typical use: create categories where all Masters in the same age group compete together in a category, from 0 to 999 kg bodyweight, based on their SM(H)F score.
    - Other possible scenario: All youth in a given age group compete against one another based on Sinclair.
    - **Note**: the Medals sheets and displays do NOT take the scoring into account.  Use the `Score` templates the Session or Competition eligibility category reports to get the rankings and award medals.
  - Huebner "Age Factors"  are now supported. These multiplicative factors adjust the total based on age and body weight for athletes aged under 20 and under 115kg.
    - Note: the body weight is interpolated, whereas the current on-line calculator from Huebner does rounding.  The online calculator is meant to be updated to also use interpolation.
  - Huebner GAMX scoring system now supported
    - This scoring system aims to provide compatibility between men and women scores
  - There can be both the open bodyweight age groups and the normal age groups in the same meet.  Just assign the desired ranking method to each age group.
    - The results scoreboard shows a Score and Rank column when a scoring system is selected for any of the age groups.
    - The TOTAL is used as score for groups that do not have a Scoring System.
  - `Scores-*.xlsx` templates were added for final package/team rankings for an age group or championship using a scoring system form medals.


For other recent changes, see [version 50 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0)
