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

> Example of record files (and up-to-date IWF and EWF records) can be found at the following [location](https://www.dropbox.com/sh/sbr804kqfwkgs6g/AAAEcT2sih9MmnrpYzkh6Erma?dl=0).  These are the files you would copy to local/records, rename and edit according to your need.

Records rows are shown according the the sorting order of the files.  The name of the rows. To control the sorting order, start the file names with a numerical prefix, e.g. 10_Canada.xlsx and 20_World.xlsx and 30_Commonwealth.xlsx would display the records in that order.

### Record File Content

The following fields are expected in the file, in that specific order.  The first line contains the names of the field.  The program stops reading at the first line where the Federation field is blank.

| Field      | Content                                                      |
| ---------- | ------------------------------------------------------------ |
| Federation | The acronym of the federation with authority to certify the record.  In competitions that involve athletes from multiple federations, this can be used to check whether an athlete belongs to the correct federation to break a record (see [Eligibility Criteria](#eligibility-criteria) below).<br />Using the official federation acronym is recommended (e.g. IWF) |
| RecordName | The name of the record, used for naming the rows in the display.  This field can be translated to the local language.<br />For an IWF record, the name will likely be "World".<br />**Note:**  Because the name of the files controls the ordering of the rows, records that bear the same name MUST all be in the same file.  If you have "National" Masters records and "National" SR records, and you want them to be on the same row, then combine the two in the same file. |
| AgeGroup   | The age group to which the record applies.  The codes should match those that have been specified when loading the Age Groups (see the [Age Groups and Categories](Categories) page).  In competitions that involve multiple age groups, this can be used to determine which records can be broken by an athlete (see [Eligibility Criteria](#eligibility-criteria) below).<br />Note that there can also be records whose age group does not match a competition age group -- for example, a record that can be broken by anyone.  If the name does not match an age group active in the competition, the eligibility checks will be skipped. |
| Gender     | M or F depending on the gender of the athlete.               |
| ageLow     | Lowest inclusive age for breaking the record.  For IWF JR, you would use 15. |
| ageCat     | Highest inclusive age for breaking the record. For IWF JR you would use 20. Use 999 when there is no upper limit. |
| bwLow      | Lowest *exclusive* body weight for breaking the record.  For the women under 55kg category, this would be 49 with the understanding that the body weight must be strictly above 49. |
| bwcat      | Highest *inclusive* body weight for breaking the record. For the women under 55kg category, the field would be 55. |
| Lift       | The kind of record: `SNATCH`, `CLEANJERK`, `TOTAL`.  Note that only the first letter (`S` `C` `T`) is actually checked. |
| Record     | The weight lifted for the record                             |
| Name       | The name of the athlete holding the record (optional).  Not currently displayed by the program, but available in some federations' databases; could be used in the future. |
| Born       | The date of birth of the athlete holding the record (optional). |
| Nation     | The nationality of the athlete holding the record (optional).  Not currently displayed by the program, but available in some federations' databases; could be used in the future. |
| Date       | The date at which the record was established (optional).  Not currently displayed by the program, but available in some federations' databases; could be used in the future. |
| Place      | The location where the record was established. Typically City, Country. |

The following figure shows the content of the 10_Canada file, organized with one age group per tab.

![](img/Records/excel.png)



### Eligibility Criteria

For a record to be broken, in addition to meeting the age and bodyweight requirements, the athlete must be eligible

- **Federation Eligibility** In the database, the athlete's registration record can optionnally have a list of federations under which they can break records.  If provided, there must be a match with the first column in the Excel file that states the federation with authority over the record.
  -  By default, the list is empty and athletes are eligible for the records from all the listed federations if they meet the age group, age and weight requirements.
  - If a list of federations (comma-separated) is given, the athletes will be restricted to these federation records. For example, if in a national championship for Canada (Federation WHC) you have an athlete invited from another country USA (Federation USAW).  If all Canadian athletes have no federation codes, and the USA athletes have USAW, then the USAW athletes cannot break Canadian records.
  - If, in a joint South American and PanAm championship, `SudAm` and `PanAm` records have been loaded, then South American athletes would have `SudAm,Panam` and all others (such as North American Athletes) would have only `PanAm` to determine who can break what record.

- **Age Group Eligibility** 

  - As a good practice, the AgeGroup column should match the age groups in your competition.  When there is a match, this means that the athlete must be eligible in that age group to break the record.  For example, if a record is marked M35, the athlete must eligible as M35 -- a 35 year-old senior could for example be ineligible because he is not a member of his Masters federation, and he would not be shown as attempting the record.

  - **Exception: ** If you have "Absolute" records that are accessible to all categories, or other records for which the eligibility requirements don't matter, just use another name for the age group.  For example, "Absolute", "Juvenile", etc.  Then the age group eligibility will not be checked, only the actual and bodyweight criteria listed on the record.

### Other Notes

- Ages are inclusive.  Masters 35 is athletes between 35 and 39
- The lower bound of the bodyweight category is excluded.  45 to 49 means greater than 45 and lower or equal to 49.
- The upperbound of the heavyweight categories is written with a `>`.  So the SR heavyweights are written as bwlow 109 bwcat >109
- You can translate the RecordName column, it is used only for display
- The `Lift` column relies on the first letter being S C or T to distinguish the lift types
- Columns `K` and after are not currently displayed, but are very useful when updating the files, and for human readers.

### Updating Records

In some countries, regional championships are held in different time zones on the same days, and national records could therefore be broken in several meets.  This is why the Excel file is expected to be consolidated manually by the association or federation.

Records set during a lift are considered to be provisional.  The updated information is displayed as long as the program is not restarted. So if there is high confidence that the record will indeed become official, you may elect to update the Excel file.







 

