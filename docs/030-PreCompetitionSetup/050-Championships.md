In OWLCMS a Competition can have multiple Championships being held at the same time. A championship defines a set of awards for part of a competition. Each Championship

- Is linked to a set of age groups.  Normally a men age group and a women age group.  Sometimes several -- Masters age groups are the typical exception.
- Determines if there are medals for total only or for each of the lifts
- Determines if medals are awarded on the weight lifted or on some other score (for example, the 3 best Sinclair scores)
- Determines the scores used for best athlete awards
- Determines if team awards are given and how they are computed.

To edit championships, use the Define Championships button on the Prepare Competition page (or the Competitions, Medals and Teams tab in the Competition Rules)

![image-20260718093019042](img/050-Championships/image-20260718093019042.png)



This brings up a list of championships

![image-20261014082938332](img/050-Championships/image-20261014082938332.png)

You will observe that championships have a "Reset to Template" button.  If you are creating several championships and want them to have similar rules, you can use the "Define Championship Template" button and selectively reset a championship to be like your template before doing other specific changes.

### Championships vs Age Groups vs Categories

Age groups and the bodyweight categories are the building blocks for championships.  

- Normally, you define championships first, so you can connect age groups to the championship they belong to.
  - You can also create an age group without a championship -- A championship with the same name will then be created.  This is common for Youth age groups (Say a U15 championship)
- You can see the age groups by using the button at the top of the page
- An age group can belong to a single championship
- This is why there is a JR M age group with the same categories as a SR M age group. You assign the JR M and JR F categories to the Junior championship.
- Medals are awarded by categories, and categories belong to age groups
  - If you have two Senior Championships going on at once (say a National and a Continental), you need to create separate age groups (say CSR M and CSR F for the continental and NSR M and NSR F for a National Senior). 
- For Masters, it is the same. If you have two Masters championships going on at once (say a National and a Continental), you need to duplicate the age groups and give them different codes. 

### IWF-Style Championships

As a first example, we use a traditional Junior Championship. 

![image-20261014083633886](img/050-Championships/image-20261014083633886.png)

In this example, we have the traditional IWF championship format

![image-20261014083732995](img/050-Championships/image-20261014083732995.png)

In this example,

- There are medals for snatch, clean and jerk, and total.
- The best Athletes are determined using GAMX as adopted by IWF
- Team points follow the normal IWF rules 28 25 23 then 22, 21, etc.
- Teams win according to the sum of points
- There are 8 athletes per team, maximum 2 per category.   
  - Team Selection is explicit and  is explained [Below](#team-selection)


Rules can then be adjusted as we want, and applied to the championship

- Smaller teams, smaller limits per category
- Use different rules for the team (take top N men and top M women, for example)

### **Score Based Awards**

Many federations use score-based systems to assign a team score. This is done by changing the radio button and selecting a scoring system.  For example, we can change the team awards so the team with the best sum of GAMX wins, counting the top 4 men and top 4 women respectively.

![image-20261014083951742](img/050-Championships/image-20261014083951742.png)

### Mixed Championships

Historically, mixed team championships are defined by adding the points of the men's and women's teams.  The example below does this

- The format selected is "sum of points" -- this is a large number as it is the sum of 16 athletes typically
- There are alternatives like using the top N athletes


![image-20261014084124297](img/050-Championships/image-20261014084124297.png)

### **Score-based Mixed Championships**

There are now scoring formulas such as the GAMX series that are equitable for men and women, such that it is possible to add men and women scores together. "Sum of Scores"  is selected

- In this example, we take the best 5 GAMX scores from men or women

![image-20261014084453784](img/050-Championships/image-20261014084453784.png)

- In this example, we use the top 4 men scores and the top 4 women scores.

![image-20261014084528072](img/050-Championships/image-20261014084528072.png)

### Team Selection

IWF competitions normally require the teams to be named explicitly.  Or in other competitions, the number of participating athletes is larger than the allowed team, and the team must be selected.  This is done on the Team Membership page.

![image-20260406160136522](img/050-Championships/image-20260406160136522.png)

In this example, the teams were defined to be 5 persons for the gendered teams and 8 for the mixed team

![image-20260406160721400](img/050-Championships/image-20260406160721400.png)

Opening each section allows selecting who is on the team or not using the checkboxes.
![image-20260406160831713](img/050-Championships/image-20260406160831713.png)

> Note that the [Registration File Format](../040-CompetitionSetup/030-EditAthleteEntries.md) supports team membership annotations.  By default
> the athletes are assumed to be part of their gendered team.  This can be changed by adding `/-T` after the category.
> When a mixed team championship requires explicit membership, you can add `+MT` to the markers.
> 
> So `JR M 60/+T,-MT` is valid (this would state that the athlete is part of the gendered team, but not of the mixed team).  `+T` is the default, `-MT` as well.

### Team Results

The results page has a dedicated area for team results
![image-20260406161025589](img/050-Championships/image-20260406161025589.png)

This shows the totals per team
![image-20260406161100911](img/050-Championships/image-20260406161100911.png)

And the details

![image-20260406161212532](img/050-Championships/image-20260406161212532.png)

A summary spreadsheet with the Men Women and Mixed results can be downloaded as well
![image-20260406161342160](img/050-Championships/image-20260406161342160.png)