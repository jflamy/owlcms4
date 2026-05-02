
In OWLCMS a Competition can have multiple Championships being held at the same time. A championship defines a set of awards for part of a competition.
By default, there is a championship created for created for each age group, with men and women together.
Championships inherit their default values from the Competition settings.

Each Championship is normally for one age group (Masters are the exception where many age groups share awards)
- it determines if there are medals for total only or for each of the lifts
- it determines how medals are awarded: it can be the traditional weight lifted, or a score like QPoints, GAMX, Sinclair etc.
- it determines the best athlete awards and the score used
- it determines the Team Championships and how teams are given
>
Note that currently an age group can only belong to a single championship.

To edit championships, use the Define Championships button on the Prepare Competition page
![image-20260501205014483](img/Championships/image-20260501205014483.png)

### Standard Championships

As a first example, we use a traditional Junior Championship.  

![image-20260501204828475](img/Championships/image-20260501204828475.png)

We intentionally uncheck the "Use default values" checkbox to change the medals that can be won.

![image-20260501205525473](img/Championships/image-20260501205525473.png)

In this example,

- There are medals for snatch, clean and jerk, and total.
- The best Athletes are determined using GAMX as adopted by IWF
- Team points follow the normal IWF rules 28 25 23 then 22, 21, etc.
- Teams win according to the sum of points
- There are 8 athletes per team, maximum 2 per category.   *Team Selection is explained [Below](#team-selection)*

Rules can then be adjusted

- Smaller teams, smaller limits per category
- Use different rules for the team (take top N men and top M women, for example)

### **Score Based Championships**

Many federation use score-based systems to assign a score to a team.,  This is done by changing the radio button and selecting a scoring system.

![image-20260501205759881](img/Championships/image-20260501205759881.png)

### Mixed Championships

Historically, mixed teams championships are defined by adding the points of the men and women teams.  The example below does this

- The format selected is "sum of points"
- Because adding two teams together yields a large number of participants, team members are explicitly selected, or the top N results are used, or the top N men and top N women are used

![image-20260501212233278](img/Championships/image-20260501212233278.png)

### **Score-based Mixed Championships**

There are now scoring formulas such as the GAMX series that are equitable for men and women, such that it is possible to add men and women scores together. "Sum of Scores"  is selected

- In this example, instead of selecting the team members explicitly, we use the top 4 men scores and the top 4 women scores
- to include all the men and all the women, simply set the top N value to the size of the teams, thereby including all athletes.

![image-20260501212359805](img/Championships/image-20260501212359805.png)

- We could have just added the men+women teams together, and we could have decided to just pick the best n men and best n women instead of naming them in advance.  Each championship can have its 

### Team Selection

To create Explicit Teams as in traditional IWF-style competitions, or for explicit mixed teams, use the Team Membership button on the Prepare Competition page

![image-20260406160136522](img/Championships/image-20260406160136522.png)

In this example, the teams were defined to be 5 persons for the gendered teams and 8 for the mixed team

![image-20260406160721400](img/Championships/image-20260406160721400.png)

Opening each section allows selecting who is on the team or not using the checkboxes.
![image-20260406160831713](img/Championships/image-20260406160831713.png)

> Note that the [Registration File Format](2300EditAthleteEntries.md) supports team membership annotations.  By default
> the athletes are assumed to be part of their gendered team.  This can be changed by adding `/-T` after the category.
> When a mixed team championship requires explicit membership, you can add `+MT` to the markers.
> 
> So `JR M 60/+T,-MT` is valid (this would state that the athlete is part of the gendered team, but not of the mixed team).  `+T` is the default, `-MT` as well.

### Team Results

The results page has a dedicated area for team results
![image-20260406161025589](img/Championships/image-20260406161025589.png)

This shows the totals per team
![image-20260406161100911](img/Championships/image-20260406161100911.png)

And the details

![image-20260406161212532](img/Championships/image-20260406161212532.png)

A summary spreadsheet with the Men Women and Mixed results can be downloaded as well
![image-20260406161342160](img/Championships/image-20260406161342160.png)