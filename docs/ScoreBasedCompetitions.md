It is sometimes desirable to award medals for all athletes in an age group on a score.  Examples include:

- Masters of all bodyweights in a given age group could be ranked according to their SM(H)F score. You could them have 3 medals for the full age group, instead of giving gold to each weight class.  In fact, you could have both the normal medals and the medals per age group.  You could even create a special age group for all the athletes between 30 and 50 and give medals for young Masters. Whatever option you like.
- Kids of given age groups compete all together based on a formula such as the Q-youth Age-Adjusted Totals.  This applies a factor based on a statistical analysis of young athletes where age and bodyweight are considered *together* for young athletes.   You can award the medals based on that formula.

#### Exporting the Current Definitions

Currently, creating such special medal awards requires editing the AgeGroups definition file using Excel.  The easiest way to get started is to export the current ones, as follows.

![10menu](img/ScoreBased/10menu.png)

![11export](img/ScoreBased/11export.png)

#### Example: Masters combined body weight classes

To create score-based age groups, there needs to be a column H named `agegroupscoring`
If you exported the definitions in the previous step, this column will be present. 

![20Masters](img/ScoreBased/20Masters.png)

- Column H includes the desired scoring system.  For Masters, this is `SMHF` .  If the cell is empty, the score is the `TOTAL` according to IWF rules.
- Column A gives this special category a new code.  In this example, the `A` prefix indicates "all bodyweights".
- Column B indicates that you can produce the Final Package for all the `MAgeGroups` together.  This would allow you to easily grant a `Best Lifter` award for all the groups in a championship.
- Column I indicates that there is a single bodyweight category from 0 to 999 kg.  All athletes fall in that single category.
- You can keep the normal Masters age groups as well if you wish.   If you pick the AF categories as the registration category the SMHF scores will be shown on the scoreboard.
- You can have Masters athletes mixed with non-masters in the same session. If that is the case, it is recommended that you use the `Display Athletes by Age Group` competition option.  By default, this will show the Total for non-masters athletes as their score unless you have selected something else for your non-masters age groups.

#### Example: Youth combined body weight classes

To create score-based age groups, there needs to be a column H named `agegroupscoring`
If you exported the definitions as explained above, this column will be present. 

![30Youth](img/ScoreBased/30Youth.png)

- Column H includes the desired scoring system.  For kid/youth/junior ages, this would be `AGEFACTORS` to use the Huebner Q-youth quantitative statistical factors. Other choices can be `BW_SINCLAIR` (body weight sinclair), `CAT_SINCLAIR` (Sinclair computed at the category weight), `ROBI` or `GAMX`   If the cell is empty, the score is the `TOTAL` according to IWF rules.
- Column A gives this special category a new code.  In this example, the `A` prefix indicates "all bodyweights".
- Column B indicates that you can produce the Final Package for all the `AgeFactors` together.  This would allow you to easily grant a `Best Lifter` award for all the groups in a championship.
- Column I indicates that there is a single bodyweight category from 0 to 999 kg.  All athletes fall in that single category.
- You can keep the normal age groups as well if you wish.   If you pick the AF categories as the registration category the selected scores will be shown on the scoreboard.
- You can have Score-based athletes mixed with normal totals in the same session. If that is the case, it is recommended that you use the `Display Athletes by Age Group` competition option.  This will show the Total for traditional categories as the score.

#### Example: True Open

You can also create a true open category using the same approach (a single bodyweight class 0-0-999kg) and a single age group (15-999 age).  You could make that 0-999 but be aware that Sinclair formulas were based on Senior athletes, so you probably want kids to be separate.

