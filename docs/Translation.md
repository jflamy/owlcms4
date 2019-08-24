# Translation Instructions

## Translating the screens and displays

1. For translation, you need to install OWLCMS4 locally.  Refer to the [Local Setup instructions](https://jflamy.github.io/owlcms4/#/LocalSetup).  Once installed,

    - Under Windows, [open your installation directory](https://jflamy.github.io/owlcms4/#/LocalSetup?id=accessing-the-program-files-and-configuration) and find the `local\i18n`  folder (which stands for "internationalization" -- the word has 18 letters between i and n)
    - Under Mac OS and Linux, open where you unzipped the file, and find the `local/i18n` directory

2. Open the file `translation4.csv` using Excel or OpenOffice/LibreOffice by double-clicking on the file.

    - The file uses the `UTF-8` international format for characters, the fields are delimited with a comma `,` and strings are quoted with straight double-quotes `"`.

    - Depending on you configurations, either

      1. the file opens correctly, one language per column, and you see French, Danish, Russian characters on the second line. If so, <u>proceed to step 3</u>  OR
      2. Excel does not open the file -- see the instructions at the bottom of the page for possible workarounds.
3. Each column of `translation4.csv`represents a language.  
    - Add your own language by adding a column at the end; use the ISO 639-1 two-letter code for your language -- see the list in https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes  (for example, da is Danish, fr is French, ru is Russian, etc.) 
    - You may  have variations per country. For example `fr_CA` is the code for French in Canada, where different words are used and some of the displays are bilingual.
4. For an initial test, translate the first line in the file, which is the title on the home page.

5. **Critical**: When SAVING you will need to make sure to use the "UTF-8 CSV" option. UTF-8 is a way to encode characters so that all world languages can be used together in the same file.  CSV is a simple text format that makes it easier to compare different versions of the file.

- For Excel, if you do a Save As you should see the CSV UTF-8 option
![60_CSV_Save As](img/Translation/60_CSV_SaveAs.png)

- For OpenOffice/LibreOffice, you will need to set some parameters when saving.  See the following [instructions for saving in UTF-8 CSV](https://rolandd.com/documentation/ro-csvi/save-a-csv-file-as-utf-8)


## Testing your screen and display translations

   3. Start the program as usual for your local setup

         - > The files that you have in your `local` directory will have precedence over the files shipped with OWLCMS4, so the translation file in `local -> i18n -> translation4.csv` will be used instead of the official one. 
         
         - Any string you have not translated will come out as `!xx: SomeCode`. This means that there is no value in the row `SomeCode` for the language `xx`.
         
4. If you make changes to the file while the program is running, you can reload the file to see your changes by going to the bottom of the `About` screen and clicking on the reload button.  After reloading the translations you need to tell your browser to reload the pages from OWLCMS4 to see the new text (F5 or Ctrl-R or Right-Click depending on your browser)

      ![15_reload](img/Translation/15_reload.png)
- By default, OWLCMS4 obeys your browser settings.  So if your browser is set to have xx as the preferred language, and there are translations available for language `xx`, you will see the `xx` text you provided.
  - If you don't get the right language (for example, my browser is in English, but I need to see French when translating to French), see the [instructions for forcing the language](https://jflamy.github.io/owlcms4/#/LocalSetup?id=defining-the-language)

## Translating the Excel files
- Under the `local\templates` directory you will find the various Excel templates, in separate subdirectories
- For example, for athlete cards, you will need to copy `templates\Cards\CardTemplate_en.xls` to `templates\Cards\CardTemplate_xx.xls`  to translate it for language xx, where xx is the [ISO 639-1 code]( https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes) for your language.
- Note: if the templates are not found in your language, the English version will be used.  So you don't have to translate all the templates, or even any template at all, if you are OK with the English version initially.

## Testing your Excel File Translations

- For the protocol and final package files where several formats are possible, the files are loaded when they are selected on the results and package pages.  To reload your file if you make changes, select another language, and then switch back to your own.
- For the other files (athlete cards, start list, etc.) there is only one choice per language, and there is no selection menu.  These files are read every time the document is produced.

## Reminder

If you are using the Windows installer, uninstalling will also delete the local directory.  **Make a copy before updating **

## Sending your translations back

Once you are happy with your `local` directory, either

- Open an issue on github and attach your translation as an enhancement request.

- or send an e-mail to the author to [jf@jflamy.dev](mailto:jf@jflamy.dev)





------



## *Workaround to force Excel to Read a UTF-8 CSV*

<u>This is only needed if Excel cannot read your file initially</u>

1. Open Excel from the start menu -- do NOT double-click on the file

2. Use the Data / From Text/CSV option to open the file

   ![10_FromTextCSV](img/Translation/10_FromTextCSV.png)

3. A wizard will run and detect the settings. Unfortunately, it cannot guess that the top line is used as a header line, so we click `Transform Data` at the bottom.![20_Wizard_transform](../../../../../docs/img/Translation/20_Wizard_transform.png)

3. Select the `Use first row as headers` option
   ![30_HeaderRows](img/Translation/30_HeaderRows.png)
4. Select `Close and Load` to reload with the header line
   ![40_CloseAndSave](img/Translation/40_CloseAndSave.png)
5. Go to the Design menu and select `Convert to Range` to go back to normal Excel style.  You may close the Queries and Connections panel at the right by clicking X
   ![50_Range](img/Translation/50_Range.png)
6. Critical: When SAVING make sure to use "Save As" and use the "UTF-8 CSV" option
   ![60_CSV_Save As](img/Translation/60_CSV_SaveAs.png)