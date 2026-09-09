import argparse
from pathlib import Path
from zipfile import ZipFile
import xml.etree.ElementTree as ET

MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
NS = {"s": MAIN}


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("path", type=Path)
    return parser.parse_args()


def validate(path):
    with ZipFile(path) as archive:
        broken = archive.testzip()
        if broken is not None:
            raise RuntimeError(f"{path}: corrupt member {broken}")
        required = {"[Content_Types].xml", "xl/workbook.xml"}
        missing = required - set(archive.namelist())
        if missing:
            raise RuntimeError(f"{path}: missing {sorted(missing)}")
        workbook = ET.fromstring(archive.read("xl/workbook.xml"))
        sheets = workbook.findall("s:sheets/s:sheet", NS)
        if not sheets:
            raise RuntimeError(f"{path}: no worksheets")
        return len(sheets)


def main():
    target = parse_args().path.expanduser().resolve()
    files = sorted(target.glob("*.xlsx")) if target.is_dir() else [target]
    if not files:
        raise RuntimeError(f"No XLSX files found at {target}")
    for path in files:
        sheet_count = validate(path)
        print(f"{path.name}: {path.stat().st_size} bytes, {sheet_count} sheet(s)")
    print(f"Validated {len(files)} workbook(s)")


if __name__ == "__main__":
    main()
