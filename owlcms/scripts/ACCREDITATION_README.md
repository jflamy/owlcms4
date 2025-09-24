# Creating Accreditation Cards

> [!WARNING]
>
> - This is an experimental facility.  The code performing this task can be unpredictable -- a simple change can break things unexpectedly.   If you intend to use this, start long in advance, and have a backup plan.
> - Currently only athletes are covered -- this is triggered the same as Athlete Cards.  Future plans should cover coaches and officials.

## Prepare the Photographs

Refer to CROP_README.md  on how to use `crop.py` to crop all your photographs to square format (`--target 1 1`) using face detection (`--face-detect`).  Crop.py is python script, so you will have to install python and follow the instructions to add the requisite packages.

### Recommendation for formatting images

In fact all images and logos you use should be in the same aspect ratio as the cell where you intend to put them.  The program will in theory resize them to fit in the cell, and try to center them.  But the closer the shape the fewer the surprises.

## Adjust the template

Create a copy of the `local/templates/cards/ZAccreditation.xlsx` file.

Read the documentation at https://jxls.sourceforge.net/each.html to get an idea of how this works.

### Adjusting the LastCell

Whatever you do don't remove Cell A1.  However look at the Note inside.  You will need to adjust the value for LastCell if you add or remove rows.

We actually recommend that you don't remove rows or columns.  Make them very small, but leave them there, it's easier.

### Adjusting images

In the sample template, there are two images included, one for the flag, with a border added, and one for the photo.  The one for the photo has a special `ptHeight` value provided.  

- Due to a limitation in jxls, when photos span several lines, it it necessary to calculate their height.
- In Excel, select each of the rows where the picture occurs. In the example, there are 3 rows.
- Click on each row number (at the very left of the excel sheet).  Then look up the row height.  The number Excel shows is the height in points (1 point = 1/72 inch) -- points are a a traditional typography unit.
- Add the points. In the example, the sum of the 3 rows on which the flag occurs is 305.  We rounded that down a little bit to 295 to be on the safe side.

## Printing

The following instructions work best with international A4 paper.  If you have North American letter-format paper, you will need to do some trimming.

On Windows install PDF24, it is the easiest way.  If you are not on Windows, the pdf24.com site has the same tools.

Print the cards to A4 format on a PDF printer (PDF24 on Windows as printer works fine).  ***Be careful to use "No Scaling" as the option.***  There should be one page per athlete.  Save the PDF.  

Open the PDF using the PDF24 tool (the PDF24 app itself). Select the "Pages per Sheet" tool.  Select "4" and "Draw Borders".  With A4 paper this will give you 4 perfectly aligned cards per A4 sheet with a guiding line for cutting.

