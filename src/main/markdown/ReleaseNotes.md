> **Version 46.0 alpha**	
>
> - **Alpha versions are NOT ready for use in actual competitions and can be non-functional**.  They are used as milestones when adding new features or making important changes.
> - Version 46 updates the Excel production
> - Reminder: you should always test any new release several days in advance, with your own data.

##### 46.0
- (alpha00) Templates and documents
  - All templates are now .xlsx by default.  Older .xls templates still work.



**Version 45 CUSTOM CSS ADJUSTMENTS**

This only concerns advanced users who have edited the css files

- *Important changes were made to the css files* for the scoreboards.  
  - In particular, the differences between dark and light styles are defined by variables in the  `colors.css` . 
  - The other files, such as `results.css` use these variables. Only a very small number of CSS rules still need to be different between dark and light.

- The major change is using an absolute `rem` font size unit in the `wrapper`.  All the font sizes elsewhere should be `em` units.   However, for now, the dimensions of the timers and decision lights remain vh/vw units.
- Another noticeable change is that the record grids at the bottom of scoreboards have been redone using grids and as a result, the border colors and width must now be defined using the `outline` directive.

