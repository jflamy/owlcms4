"""Word outline numbering helpers for the OTIS proposal headings.

Headings use a multilevel list attached to the Heading styles: level 1 is a
capital letter ("A."), deeper levels are decimal ("A.1", "A.1.1", ...).
Unnumbered chapters (Cover Letter, Appendices, ...) suppress the list with a
paragraph-level numId of 0 so they do not consume a letter.
"""

import copy
import re

from docx.enum.style import WD_STYLE_TYPE
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn
from docx.text.paragraph import Paragraph


MAX_LEVELS = 9
TYPED_PREFIX = re.compile(r"^([A-Z])\.(\d+(?:\.\d+)*)?\s+")
REF_INSTRUCTION = re.compile(r"^\s*REF\s+(\S+)(.*)$")


def heading_level(paragraph):
    match = re.fullmatch(r"Heading (\d)", paragraph.style.name)
    return int(match.group(1)) if match else None


def format_number(counters):
    letter = chr(ord("A") + counters[0] - 1)
    return ".".join([letter] + [str(value) for value in counters[1:]])


def typed_number(text):
    match = TYPED_PREFIX.match(text)
    if not match:
        return None
    parts = [ord(match.group(1)) - ord("A") + 1]
    if match.group(2):
        parts.extend(int(part) for part in match.group(2).split("."))
    return parts


def _style_num_id(style):
    while style is not None:
        pPr = style.element.pPr
        if pPr is not None and pPr.numPr is not None and pPr.numPr.numId is not None:
            return pPr.numPr.numId.val
        style = style.base_style
    return None


def _paragraph_num_id(paragraph):
    pPr = paragraph._element.pPr
    if pPr is None or pPr.numPr is None or pPr.numPr.numId is None:
        return None
    return pPr.numPr.numId.val


def is_numbered_heading(paragraph):
    """Whether Word shows an outline number for this heading.

    Documents that still carry typed prefixes (no list on the style) are
    treated as numbered when the text starts with such a prefix.
    """
    if heading_level(paragraph) is None:
        return False
    paragraph_num_id = _paragraph_num_id(paragraph)
    if paragraph_num_id is not None:
        return paragraph_num_id != 0
    if _style_num_id(paragraph.style) is not None:
        return True
    return typed_number(paragraph.text) is not None


def heading_numbers(document):
    """Map each numbered heading paragraph element to its displayed number."""
    numbers = {}
    counters = [0] * MAX_LEVELS
    for paragraph in document.paragraphs:
        level = heading_level(paragraph)
        if level is None or not is_numbered_heading(paragraph):
            continue
        counters[level - 1] += 1
        for index in range(level, MAX_LEVELS):
            counters[index] = 0
        numbers[paragraph._element] = format_number(counters[:level])
    return numbers


def check_typed_sequence(document):
    """Fail unless typed prefixes match what automatic numbering will produce."""
    expected = heading_numbers(document)
    for paragraph in document.paragraphs:
        if paragraph._element not in expected:
            continue
        typed = typed_number(paragraph.text)
        if typed is None or format_number(typed) != expected[paragraph._element]:
            raise RuntimeError(
                f"Heading {paragraph.text!r} would be renumbered "
                f"{expected[paragraph._element]}"
            )


def _level_xml(level, style_id):
    if level == 0:
        number_format, text = "upperLetter", "%1."
    else:
        number_format = "decimal"
        text = ".".join(f"%{index + 1}" for index in range(level + 1))
    style = f'<w:pStyle w:val="{style_id}"/>' if style_id else ""
    return (
        f'<w:lvl w:ilvl="{level}"><w:start w:val="1"/>'
        f'<w:numFmt w:val="{number_format}"/>{style}<w:suff w:val="space"/>'
        f'<w:lvlText w:val="{text}"/><w:lvlJc w:val="left"/>'
        '<w:pPr><w:ind w:left="0" w:firstLine="0"/></w:pPr></w:lvl>'
    )


def ensure_heading_list(document):
    """Attach a lettered outline list to Heading 1..9; return its numId."""
    heading_styles = {}
    for level in range(1, MAX_LEVELS + 1):
        name = f"Heading {level}"
        if name in document.styles:
            heading_styles[level] = document.styles[name]
    existing = _style_num_id(heading_styles[1])
    if existing is not None:
        return existing

    numbering = document.part.numbering_part.element
    abstract_ids = [int(item.get(qn("w:abstractNumId"))) for item in numbering.findall(qn("w:abstractNum"))]
    num_ids = [int(item.get(qn("w:numId"))) for item in numbering.findall(qn("w:num"))]
    abstract_id = max(abstract_ids, default=-1) + 1
    num_id = max(num_ids, default=0) + 1

    levels = "".join(
        _level_xml(
            level,
            heading_styles[level + 1].style_id if level + 1 in heading_styles else None,
        )
        for level in range(MAX_LEVELS)
    )
    abstract = parse_xml(
        f'<w:abstractNum {nsdecls("w")} w:abstractNumId="{abstract_id}">'
        f'<w:multiLevelType w:val="multilevel"/>{levels}</w:abstractNum>'
    )
    first_num = numbering.find(qn("w:num"))
    if first_num is not None:
        first_num.addprevious(abstract)
    else:
        numbering.append(abstract)
    numbering.append(
        parse_xml(
            f'<w:num {nsdecls("w")} w:numId="{num_id}">'
            f'<w:abstractNumId w:val="{abstract_id}"/></w:num>'
        )
    )

    for level, style in heading_styles.items():
        numPr = style.element.get_or_add_pPr().get_or_add_numPr()
        if level > 1:
            numPr.get_or_add_ilvl().val = level - 1
        numPr.get_or_add_numId().val = num_id
    suppress_inherited_numbering(document)
    return num_id


def suppress_inherited_numbering(document):
    """Styles derived from a Heading style (e.g. TOC Heading) must not be numbered."""
    suppressed = []
    for style in document.styles:
        if style.type != WD_STYLE_TYPE.PARAGRAPH or re.fullmatch(r"Heading \d", style.name):
            continue
        base = style.base_style
        while base is not None and not re.fullmatch(r"Heading \d", base.name):
            base = base.base_style
        if base is None:
            continue
        pPr = style.element.get_or_add_pPr()
        if pPr.numPr is not None and pPr.numPr.numId is not None and pPr.numPr.numId.val == 0:
            continue
        numPr = pPr.get_or_add_numPr()
        if numPr.ilvl is not None:
            numPr.remove(numPr.ilvl)
        numPr.get_or_add_numId().val = 0
        suppressed.append(style.name)
    return suppressed


def suppress_numbering(paragraph):
    numPr = paragraph._element.get_or_add_pPr().get_or_add_numPr()
    if numPr.ilvl is not None:
        numPr.remove(numPr.ilvl)
    numPr.get_or_add_numId().val = 0


def strip_typed_prefix(paragraph):
    match = TYPED_PREFIX.match(paragraph.text)
    if not match:
        return
    remaining = len(match.group(0))
    for element in list(paragraph._element.iter(qn("w:t"), qn("w:tab"))):
        if remaining == 0:
            break
        if element.tag == qn("w:tab"):
            element.getparent().remove(element)
            remaining -= 1
            continue
        text = element.text or ""
        removed = min(remaining, len(text))
        element.text = text[removed:]
        remaining -= removed
        if not element.text:
            element.getparent().remove(element)


def _enclosing_paragraph(element):
    while element is not None and element.tag != qn("w:p"):
        element = element.getparent()
    return element


def insert_after_pPr(paragraph_element, child):
    pPr = paragraph_element.find(qn("w:pPr"))
    if pPr is None:
        paragraph_element.insert(0, child)
    else:
        pPr.addnext(child)


def repair_heading_bookmarks(document):
    """Move drifted bookmark starts back into the heading holding their end."""
    body = document.element.body
    ends = {end.get(qn("w:id")): end for end in body.iter(qn("w:bookmarkEnd"))}
    repaired = []
    for start in list(body.iter(qn("w:bookmarkStart"))):
        end = ends.get(start.get(qn("w:id")))
        if end is None:
            continue
        end_paragraph = _enclosing_paragraph(end)
        if end_paragraph is None or _enclosing_paragraph(start) is end_paragraph:
            continue
        if heading_level(Paragraph(end_paragraph, document)) is None:
            continue
        start.getparent().remove(start)
        insert_after_pPr(end_paragraph, start)
        repaired.append(start.get(qn("w:name")))
    return repaired


def heading_bookmarks(document):
    """Map bookmark names to the heading paragraph element they sit in."""
    result = {}
    for start in document.element.body.iter(qn("w:bookmarkStart")):
        paragraph = _enclosing_paragraph(start)
        if paragraph is not None and heading_level(Paragraph(paragraph, document)) is not None:
            result[start.get(qn("w:name"))] = paragraph
    return result


def unique_bookmark_name(document, base):
    names = {
        bookmark.get(qn("w:name"))
        for bookmark in document.element.body.iter(qn("w:bookmarkStart"))
    }
    name = base
    suffix = 2
    while name in names:
        name = f"{base}_{suffix}"
        suffix += 1
    return name


def _field_run(run_properties, child):
    run = OxmlElement("w:r")
    if run_properties is not None:
        run.append(copy.deepcopy(run_properties))
    run.append(child)
    return run


def _field_char(kind):
    element = OxmlElement("w:fldChar")
    element.set(qn("w:fldCharType"), kind)
    return element


def ref_field_runs(bookmark_name, switches, result_text, run_properties=None):
    """Runs for a complex REF field with a cached result."""
    instruction = OxmlElement("w:instrText")
    instruction.set(qn("xml:space"), "preserve")
    instruction.text = f" REF {bookmark_name} {switches} "
    result = OxmlElement("w:t")
    result.set(qn("xml:space"), "preserve")
    result.text = result_text
    return [
        _field_run(run_properties, _field_char("begin")),
        _field_run(run_properties, instruction),
        _field_run(run_properties, _field_char("separate")),
        _field_run(run_properties, result),
        _field_run(run_properties, _field_char("end")),
    ]


def reference_run_properties():
    properties = OxmlElement("w:rPr")
    color = OxmlElement("w:color")
    color.set(qn("w:val"), "0563C1")
    underline = OxmlElement("w:u")
    underline.set(qn("w:val"), "single")
    properties.extend((color, underline))
    return properties


def append_heading_number_ref(paragraph_element, bookmark_name, number):
    for run in ref_field_runs(bookmark_name, r"\w \h", number, reference_run_properties()):
        paragraph_element.append(run)


def field_bookmark(cell_element):
    """Bookmark targeted by the REF field or internal hyperlink in a cell."""
    for instruction in cell_element.iter(qn("w:instrText")):
        match = REF_INSTRUCTION.match(instruction.text or "")
        if match:
            return match.group(1)
    for hyperlink in cell_element.iter(qn("w:hyperlink")):
        anchor = hyperlink.get(qn("w:anchor"))
        if anchor:
            return anchor
    return None


def convert_number_hyperlinks(document, bookmarks, numbers):
    """Turn internal hyperlinks showing a heading number into REF \\w fields."""
    converted = 0
    for hyperlink in list(document.element.body.iter(qn("w:hyperlink"))):
        anchor = hyperlink.get(qn("w:anchor"))
        heading = bookmarks.get(anchor)
        if heading is None or heading not in numbers:
            continue
        text = "".join(t.text or "" for t in hyperlink.iter(qn("w:t"))).strip()
        if not re.fullmatch(r"[A-Z](?:\.\d+)*\.?", text):
            continue
        first_run = hyperlink.find(qn("w:r"))
        properties = first_run.find(qn("w:rPr")) if first_run is not None else None
        for run in ref_field_runs(anchor, r"\w \h", numbers[heading], properties):
            hyperlink.addprevious(run)
        hyperlink.getparent().remove(hyperlink)
        converted += 1
    return converted


def _complex_fields(paragraph_element):
    """Yield (begin_run, instruction_elements, result_runs, end_run) tuples."""
    runs = [child for child in paragraph_element if child.tag == qn("w:r")]
    index = 0
    while index < len(runs):
        char = runs[index].find(qn("w:fldChar"))
        if char is None or char.get(qn("w:fldCharType")) != "begin":
            index += 1
            continue
        begin = runs[index]
        instructions, results = [], []
        in_result = False
        depth = 0
        index += 1
        end = None
        while index < len(runs):
            run = runs[index]
            char = run.find(qn("w:fldChar"))
            kind = char.get(qn("w:fldCharType")) if char is not None else None
            if kind == "begin":
                depth += 1
            elif kind == "end" and depth:
                depth -= 1
            elif kind == "end":
                end = run
                break
            elif kind == "separate" and not depth:
                in_result = True
            elif in_result:
                results.append(run)
            else:
                instructions.extend(run.findall(qn("w:instrText")))
            index += 1
        if end is not None:
            yield begin, instructions, results, end
        index += 1


def split_heading_text_refs(document, bookmarks, numbers):
    """Rewrite REF fields that showed a typed "number text" heading.

    Without typed prefixes, a plain REF yields only the heading text, so each
    such field becomes a REF \\w (number) followed by a plain REF (text).
    """
    rewritten = 0
    for paragraph_element in list(document.element.body.iter(qn("w:p"))):
        for begin, instructions, results, end in list(_complex_fields(paragraph_element)):
            instruction = "".join(item.text or "" for item in instructions)
            match = REF_INSTRUCTION.match(instruction)
            if not match:
                continue
            name, switches = match.group(1), match.group(2)
            heading = bookmarks.get(name)
            if heading is None or heading not in numbers:
                continue
            if re.search(r"\\[nrwp]", switches):
                continue
            cached = "".join(t.text or "" for run in results for t in run.iter(qn("w:t")))
            if not TYPED_PREFIX.match(cached):
                continue
            instructions[0].text = f" REF {name} \\w{switches.rstrip()} "
            for item in instructions[1:]:
                item.text = ""
            text = TYPED_PREFIX.sub("", cached)
            first_t = next((t for run in results for t in run.iter(qn("w:t"))), None)
            for run in results:
                for t in run.iter(qn("w:t")):
                    t.text = ""
            if first_t is not None:
                first_t.text = numbers[heading]
            properties = begin.find(qn("w:rPr"))
            space = OxmlElement("w:t")
            space.set(qn("xml:space"), "preserve")
            space.text = " "
            new_runs = [_field_run(properties, space)]
            new_runs += ref_field_runs(name, switches.strip(), text, properties)
            anchor = end
            for run in new_runs:
                anchor.addnext(run)
                anchor = run
            rewritten += 1
    return rewritten
