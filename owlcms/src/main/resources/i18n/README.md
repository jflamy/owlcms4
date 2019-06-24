# Translation Instructions

1. Open https://raw.githubusercontent.com/jflamy/owlcms4/develop/owlcms/src/main/resources/i18n/translation4.csv
2. Right-click "Save As" and save the file somewhere (see step 5.3 for a useful location)
3. Open using Excel by double-clicking on the file. Depending on you configurations, either
   1. the file opens correctly, in several columns, and you see funny French or Danish characters. If so, proceed to step 4  OR
   2. the CSV file does not open with one column per language (if you see semicolons and everything is in column A).  If so
      1. Open Excel -- do not double-click on the file
      2. Select the .csv file.  This  should start the conversion wizard
      3. Select the "delimited" option on the first screen
      4. add "semicolon" as delimiter on the second.
4. Each column represents a language.  You may add your own language by adding a column at the end; use the ISO 639-1 two-letter code for your language -- see the list in https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes  (for example, da is Danish, fr is French, etc.)
5. In order to test your translations:
   1. Add a column for your language in the .csv 
   2. Translate a few items by filling in the cells in the csv abd save it.
   3. Create a folder local\i18n next to the owlcms jar and copy the .csv in there
   4. Start the program as follows (replace X.Y.Z with your version)
      ```java -cp local;owlcms-X.Y.Z.jar app.owlcms.Main```
      This tells the program to look for files inside the "local" folder prior to looking inside the jar (which is just a big .zip)
   5. Any string you have not translated will come out as `!ru: SomeCode`
      This means that there is no value in the row `SomeCode` for the language `ru`
