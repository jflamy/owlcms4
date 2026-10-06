"""Check bundled XLSX XML without rewriting the workbooks or building Java.

Run: python3 owlcms/scripts/test_template_structure.py

Excel discards styles with out-of-order border/xf children, even when the XML
parses successfully. Namespace prefixes referenced by mc:Ignorable must also
remain declared when a template is edited.
"""

from io import BytesIO
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
from zipfile import ZipFile


TEMPLATES = Path(__file__).resolve().parents[1] / "src/main/resources/templates"
NAMES = tuple(f"{kind}-{paper}.xlsx" for kind in ("Total", "Score", "SnCJTot") for paper in ("A4", "LETTER"))
MAIN = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"
MC = "{http://schemas.openxmlformats.org/markup-compatibility/2006}"
CHILD_ORDER = {
    "border": ("start", "end", "left", "right", "top", "bottom", "diagonal", "vertical", "horizontal"),
    "xf": ("alignment", "protection", "extLst"),
}


def check_style_order(styles):
    for name, order in CHILD_ORDER.items():
        for index, element in enumerate(styles.iter(MAIN + name)):
            positions = [order.index(child.tag.removeprefix(MAIN)) for child in element]
            if positions != sorted(set(positions)):
                raise ValueError(f"{name}[{index}]: invalid child order or duplicate children")


def check_namespaces(data):
    namespaces = {}
    stack = []
    pending = {}
    for event, element in ET.iterparse(BytesIO(data), events=("start-ns", "start", "end")):
        if event == "start-ns":
            prefix, uri = element
            pending[prefix] = uri
        elif event == "start":
            stack.append(namespaces)
            namespaces = {**namespaces, **pending}
            pending.clear()
            for prefix in element.get(MC + "Ignorable", "").split():
                if prefix not in namespaces:
                    raise ValueError(f"Undeclared mc:Ignorable prefix: {prefix}")
        else:
            namespaces = stack.pop()


class TemplateStructureTest(unittest.TestCase):
    def test_bundled_templates(self):
        files = sorted(TEMPLATES.rglob("*.xlsx"))
        self.assertTrue(files)
        for path in files:
            with self.subTest(template=str(path.relative_to(TEMPLATES))), ZipFile(path) as archive:
                self.assertIsNone(archive.testzip())
                for member in archive.namelist():
                    if member.endswith((".xml", ".rels")):
                        check_namespaces(archive.read(member))
                styles = ET.fromstring(archive.read("xl/styles.xml"))
                check_style_order(styles)
                for group in styles:
                    if "count" in group.attrib:
                        self.assertEqual(int(group.get("count")), len(group))

                limits = {
                    "fontId": len(styles.find(MAIN + "fonts")),
                    "fillId": len(styles.find(MAIN + "fills")),
                    "borderId": len(styles.find(MAIN + "borders")),
                    "xfId": len(styles.find(MAIN + "cellStyleXfs")),
                }
                for xf in styles.iter(MAIN + "xf"):
                    for attribute, limit in limits.items():
                        self.assertTrue(0 <= int(xf.get(attribute, "0")) < limit)
                style_count = len(styles.find(MAIN + "cellXfs"))
                for member in archive.namelist():
                    if member.startswith("xl/worksheets/") and member.endswith(".xml"):
                        sheet = ET.fromstring(archive.read(member))
                        for cell in sheet.iter(MAIN + "c"):
                            self.assertTrue(0 <= int(cell.get("s", "0")) < style_count)

    def test_competition_results_commands(self):
        for name in NAMES:
            with self.subTest(template=name), ZipFile(TEMPLATES / "competitionResults" / name) as archive:
                sheet = ET.fromstring(archive.read("xl/worksheets/sheet1.xml"))
                self.assertIsNone(sheet.find(MAIN + "conditionalFormatting"))
                comments = ET.fromstring(archive.read("xl/comments1.xml"))
                commands = {
                    c.get("ref"): "".join(c.itertext())
                    for c in comments.iter(MAIN + "comment")
                }
                self.assertEqual(set(commands), {"A1", "A8", "A10", "A12", "A15"})
                self.assertIn("jx:area", commands["A1"])

    def test_rejects_out_of_order_styles(self):
        for name, children in (
            ("border", "<left/><diagonal/><bottom/>"),
            ("xf", "<protection/><alignment/>"),
        ):
            with self.subTest(element=name), self.assertRaises(ValueError):
                check_style_order(ET.fromstring(f'<{name} xmlns="{MAIN[1:-1]}">{children}</{name}>'))

    def test_accepts_valid_style_order(self):
        check_style_order(ET.fromstring(
            f'<styleSheet xmlns="{MAIN[1:-1]}">'
            '<borders><border><left/><bottom/><diagonal/></border></borders>'
            '<cellXfs><xf><alignment/><protection/></xf></cellXfs>'
            '</styleSheet>'
        ))

    def test_rejects_undeclared_ignorable_prefix(self):
        with self.assertRaises(ValueError):
            check_namespaces(
                f'<styleSheet xmlns:mc="{MC[1:-1]}" mc:Ignorable="xr"/>'.encode()
            )

    def test_accepts_scoped_ignorable_prefix(self):
        check_namespaces(
            f'<root xmlns:mc="{MC[1:-1]}"><child xmlns:xr="urn:test" mc:Ignorable="xr"/></root>'.encode()
        )


if __name__ == "__main__":
    unittest.main()
