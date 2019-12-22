* Release Highlights for release 4.3 (alpha1): 
  - [X] Manage list of age groups with age boundaries ("Edit Age Groups").  Each age group be active or inactive.
  - [x] Recompute athlete categories from the "Edit Athletes" screen after editing (on demand)
  - [X] Each age group has its own bodyweight categories
  - [X] Athlete must select the category they are in (is 36 year old lifting in the senior group or the masters group?)
    - [x] MASTERS, Uxx Age Groups, IWF age groups, All (default) are listed in order of preference (if active).  
    - [x] If all the age groups are active, A 36-year old would be M35 by default, and the other choices, O21, SR, All would be listed in that order.  The default can be overridden to let the 36 year old compete in a senior group.
  - [X] Registration form is backward compatible, but it is now also possible to give birth+gender+bodyweight category in the 3 columns. Finally, If desired the exact name of category can be used.
  - [ ] Allow management of the categories for an age group from the age group itself (not in first release, you can use the Edit Categories button for now -- each category now connects to exactly one age group so there are several categories with the same bodyweight connected to the various age groups)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
