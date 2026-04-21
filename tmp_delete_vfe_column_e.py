import io
import re
import zipfile
from pathlib import Path
import xml.etree.ElementTree as ET


WORKBOOK_PATH = Path(r"C:\Dev\git\owlcms4\owlcms\src\main\resources\templates\teams\VFE_Teams-A4.xlsx")
DELETE_COL = 5  # E

NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
ET.register_namespace("", NS_MAIN)


def col_to_num(col_letters: str) -> int:
    result = 0
    for ch in col_letters:
        result = result * 26 + (ord(ch) - ord("A") + 1)
    return result


def num_to_col(col_num: int) -> str:
    letters = []
    while col_num > 0:
        col_num, rem = divmod(col_num - 1, 26)
        letters.append(chr(ord("A") + rem))
    return "".join(reversed(letters))


def shift_col_num(col_num: int) -> int | None:
    if col_num == DELETE_COL:
        return None
    if col_num > DELETE_COL:
        return col_num - 1
    return col_num


def shift_ref_token(token: str) -> str:
    if ":" in token:
        start, end = token.split(":", 1)
        shifted_start = shift_ref_token(start)
        shifted_end = shift_ref_token(end)
        if shifted_start is None or shifted_end is None:
            raise ValueError(f"Cannot shift range through deleted column: {token}")
        return f"{shifted_start}:{shifted_end}"

    match = re.fullmatch(r"(\$?)([A-Z]+)(\$?)(\d+)", token)
    if not match:
        return token
    leading_dollar, col_letters, row_dollar, row_num = match.groups()
    shifted_col = shift_col_num(col_to_num(col_letters))
    if shifted_col is None:
        return None
    return f"{leading_dollar}{num_to_col(shifted_col)}{row_dollar}{row_num}"


def shift_ref_attr(value: str) -> str:
    parts = []
    for token in value.split():
        shifted = shift_ref_token(token)
        if shifted is not None:
            parts.append(shifted)
    return " ".join(parts)


def shift_comment_text(text: str) -> str:
    return re.sub(r'lastCell="([A-Z]+)(\d+)"', lambda m: f'lastCell="{num_to_col(shift_col_num(col_to_num(m.group(1))))}{m.group(2)}"', text)


def shift_vml_anchor(anchor_text: str) -> str:
    values = [part.strip() for part in anchor_text.split(",")]
    if len(values) != 8:
        return anchor_text
    for index in (0, 4):
        col_index = int(values[index])
        if col_index >= DELETE_COL:
            values[index] = str(col_index - 1)
    return ", ".join(values)


def patch_sheet(xml_bytes: bytes) -> bytes:
    root = ET.fromstring(xml_bytes)

    dimension = root.find(f"{{{NS_MAIN}}}dimension")
    if dimension is not None and dimension.get("ref"):
        dimension.set("ref", shift_ref_attr(dimension.get("ref")))

    cols = root.find(f"{{{NS_MAIN}}}cols")
    if cols is not None:
        for col in list(cols):
            min_col = int(col.get("min"))
            max_col = int(col.get("max"))
            if min_col == DELETE_COL and max_col == DELETE_COL:
                cols.remove(col)
                continue
            if max_col < DELETE_COL:
                continue
            if min_col > DELETE_COL:
                col.set("min", str(min_col - 1))
                col.set("max", str(max_col - 1))
                continue
            new_max = max_col - 1
            if new_max < min_col:
                cols.remove(col)
            else:
                col.set("max", str(new_max))

    for element in root.iter():
        for attr_name in ("ref", "sqref", "activeCell", "topLeftCell"):
            attr_value = element.get(attr_name)
            if attr_value:
                element.set(attr_name, shift_ref_attr(attr_value))

    sheet_data = root.find(f"{{{NS_MAIN}}}sheetData")
    if sheet_data is not None:
        for row in sheet_data.findall(f"{{{NS_MAIN}}}row"):
            spans = row.get("spans")
            if spans:
                start_str, end_str = spans.split(":", 1)
                start_col = int(start_str)
                end_col = int(end_str)
                if start_col > DELETE_COL:
                    start_col -= 1
                if end_col >= DELETE_COL:
                    end_col -= 1
                row.set("spans", f"{start_col}:{end_col}")

            for cell in list(row.findall(f"{{{NS_MAIN}}}c")):
                ref = cell.get("r")
                if not ref:
                    continue
                shifted_ref = shift_ref_token(ref)
                if shifted_ref is None:
                    row.remove(cell)
                elif shifted_ref != ref:
                    cell.set("r", shifted_ref)

    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


def patch_comments(xml_bytes: bytes) -> bytes:
    root = ET.fromstring(xml_bytes)
    for text_elem in root.findall(f".//{{{NS_MAIN}}}t"):
        if text_elem.text:
            text_elem.text = shift_comment_text(text_elem.text)
    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


def patch_vml(xml_bytes: bytes) -> bytes:
    text = xml_bytes.decode("utf-8")
    text = re.sub(r"<x:Anchor>(.*?)</x:Anchor>", lambda m: f"<x:Anchor>{shift_vml_anchor(m.group(1))}</x:Anchor>", text, flags=re.DOTALL)
    return text.encode("utf-8")


def main() -> None:
    with zipfile.ZipFile(WORKBOOK_PATH, "r") as source_zip:
        names = source_zip.namelist()
        files = {name: source_zip.read(name) for name in names}

    files["xl/worksheets/sheet1.xml"] = patch_sheet(files["xl/worksheets/sheet1.xml"])
    files["xl/comments1.xml"] = patch_comments(files["xl/comments1.xml"])
    files["xl/drawings/vmlDrawing1.vml"] = patch_vml(files["xl/drawings/vmlDrawing1.vml"])

    with zipfile.ZipFile(WORKBOOK_PATH, "w", compression=zipfile.ZIP_DEFLATED) as output_zip:
        for name in names:
            output_zip.writestr(name, files[name])

    print(f"Updated {WORKBOOK_PATH}")


if __name__ == "__main__":
    main()