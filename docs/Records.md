Record definition files can be loaded in owlcms so that when an athlete is about to lift, the records for his/her categories can be shown.  As many record files as needed can be loaded, so that state, national, continental, world, or event-specific records can all be shown.

In the following example

- the athlete can potentially break records in categories from two age groups (JR 45 and SR 45)
- the records from two federations have been loaded to illustrate. Normally one might expect related state, national and continental federations to be used in a given meet.
- the next lift would break the records highlighted in purple (assuming of course that the athlete meets citizenship and that other record requirements such as proper referee levels are met).
- the athlete had, in fact, just set the records on the previous lift-- the system updates the display when a record is provisionally set during a meet.

![records](img/Records/records.png)

### Loading Records

The program reads all the tabs of all the files found in the `local/records` directory.  For legibility, we suggest using one Excel per federation/jurisdiction, and one tab per age group.  This does *not* actually matter, since the program reads all the files and all the tabs in each file.

> Note that `local/records` is case-sensitive (lowercase `r`)

Records are shown according the the sorting order of the files.  To control the sorting order, start the file names with a numerical prefix, e.g. 10_Canada.xlsx and 20_World.xlsx and 30_Commonwealth.xlsx would display the records in that order.

The following figure shows the content of the 10_Canada file, organized with one age group per tab.  In order to support non-standard age groups, and non-standard categories, the lower and upper bounds for ages and for body weights are given. 

![](img/Records/excel.png)

Notes:

-  The lower bound for bodyweights is the top of the previous category, and the heavyweight category is given with a > (`>109` for example).
- You can translate the RecordName and AgeGroup columns.  They are used for display only.
- The `Lift` column relies on the first letter being S C or T to distinguish the lift types
- Columns `K` and after are not currently displayed, but are very useful when updating the files, and for human readers.

### Updating Records

In some countries, regional championships are held in different time zones on the same days, and national records could therefore be broken in several meets.  This is why the Excel file is expected to be consolidated manually by the association or federation.

Records set during a lift are considered to be provisional.  The updated information is displayed as long as the program is not restarted. So if there is high confidence that the record will indeed become official, you may elect to update the Excel file.







 

