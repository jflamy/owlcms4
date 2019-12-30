
* Release Highlights for release 4.3 (release candidate 1): 
  
  - [X] Manage a list of age groups with age boundaries ("Edit Age Groups").  Each age group be active or inactive.
    - [X] Each age group has its own bodyweight categories
    - [X] Bodyweight Categories are now created/modified by editing the age group. This ensures that each category is connected to a single age group and that there are no gaps in the body weights for each age group.
  - [x] The Edit Athletes and Edit Age Groups pages each have a button at the top of the page to reassign athletes if the age groups or categories are edited.
    - [x] Until actual weigh-in has taken place, the presumed body weight is inferred from the category assigned manually to the athlete using the interface or read through a registration file.  In this way, it is possible to reload the definition files or play with age group categories without losing information about the athlete categories.
  - [X] Age Group definitions are loaded when the program is started based to the locale (language_country). It is also possible to load/reload an age group definition page
  - [X] Athlete must select the category they are in (is 36 year old lifting in the senior group or the masters group?)
    - [x] MASTERS, Uxx Age Groups, IWF age groups, All (default) are listed in order of preference (if active).  
    - [x] If all the age groups are active, A 36-year old would be M35 by default, and the other choices, O21, SR, All would be listed in that order.  The default can be overridden to let the 36 year old compete in a senior group.
  - [X] The registration form is backward compatible, but it is now also possible to give birth+gender+bodyweight category in the 3 columns. Finally, If desired the exact name of category can be used.
  - [X] The 20kg or the Masters 15/10 rules is now applied based on the category of the athlete.  You can therefore mix and match Masters and non-Masters athletes in the same group.

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
