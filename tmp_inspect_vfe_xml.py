import pathlib
import re
import zipfile

path = pathlib.Path(r"C:\Dev\git\owlcms4\owlcms\src\main\resources\templates\teams\VFE_Teams-A4.xlsx")
with zipfile.ZipFile(path) as zf:
    sheet = zf.read("xl/worksheets/sheet1.xml").decode("utf-8")
    comments = zf.read("xl/comments1.xml").decode("utf-8")

print("dimension", re.search(r'<dimension ref="([^"]+)"', sheet).group(1))
cols_match = re.search(r'<cols>(.*?)</cols>', sheet)
print("cols", cols_match.group(1) if cols_match else None)
print("merges", re.findall(r'<mergeCell ref="([^"]+)"', sheet))
print("comments", comments)
