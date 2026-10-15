#!/usr/bin/env python3
"""Extract compliance-table text to Markdown for manual/AI summarization, and read it back.

Workflow:
  1. python3 compliance_summaries.py extract OTIS_Proposal.docx compliance_summaries.md
  2. Fill in the "R:" (requirement) and "J:" (justification) summary lines in the .md file.
  3. python3 rebuild_compliance.py OTIS_Proposal.docx OTIS_Proposal_01.docx \
         --summaries compliance_summaries.md
     This also writes OTIS_Proposal_01.md, read back from the new table.
  4. After editing OTIS_Proposal_01.docx in Word (text or table cells), refresh its .md:
       python3 compliance_summaries.py extract OTIS_Proposal_01.docx OTIS_Proposal_01.md --from-table
     then rebuild 01 -> 02 with --summaries OTIS_Proposal_01.md, and so on.

This script never writes summaries itself; it only moves text in and out.
R:/J: lines are authoritative; the quoted full text in the .md is for reading only.
"""

import argparse
from pathlib import Path
import re
import sys

from docx import Document


BLOCK_PATTERN = re.compile(r"^## (\S+) \| (\S+)\s*$")
SUMMARY_PATTERN = re.compile(r"^([RJ]):[ \t]*(.*)$")
FIX_PATTERN = re.compile(r"^FIX:[ \t]*(.*)$")


def read_summaries(path, strict=True):
    """Return {requirement_id: {"reference", "requirement", "justification", "fix"}}."""
    summaries = {}
    current = None
    for line_number, line in enumerate(Path(path).read_text(encoding="utf-8").splitlines(), 1):
        block = BLOCK_PATTERN.match(line)
        if block:
            requirement_id = block.group(1)
            if requirement_id in summaries:
                message = f"{path}:{line_number}: duplicate block {requirement_id}"
                if strict:
                    raise RuntimeError(message)
                print(f"Warning: {message} ignored")
                current = None
                continue
            current = {
                "reference": block.group(2),
                "requirement": "",
                "justification": "",
                "fix": "",
            }
            summaries[requirement_id] = current
            continue
        if current is None:
            continue
        fix = FIX_PATTERN.match(line)
        if fix:
            current["fix"] = " ".join(fix.group(1).split())
            continue
        summary = SUMMARY_PATTERN.match(line)
        if summary:
            key = "requirement" if summary.group(1) == "R" else "justification"
            current[key] = " ".join(summary.group(2).split())
    return summaries


def apply_summaries(requirements, summaries):
    """Set *_summary on records from the R:/J: lines; return warnings."""
    warnings = []
    known_ids = set()
    for record in requirements:
        requirement_id = record["requirement_id"]
        known_ids.add(requirement_id)
        entry = summaries.get(requirement_id)
        if entry is None:
            warnings.append(f"{requirement_id}: no summary block; original text kept")
            continue
        if entry["fix"]:
            warnings.append(f"{requirement_id}: FIX in proposal: {entry['fix']}")
        for key in ("requirement", "justification"):
            if entry[key]:
                record[f"{key}_summary"] = entry[key]
            else:
                warnings.append(f"{requirement_id}: empty {key} summary; original text kept")
    for requirement_id in sorted(set(summaries) - known_ids):
        warnings.append(f"{requirement_id}: summary block has no matching requirement")
    return warnings


def quote(text):
    return "\n".join(f"> {line}" if line else ">" for line in text.splitlines())


def table_summaries(requirements, tables):
    """Return {requirement_id: (R, J)} from the compliance table cells."""
    records = {record["requirement_id"]: record for record in requirements}
    summaries = {}
    for table in tables:
        for row in table.rows[1:]:
            words = row.cells[0].text.split(None, 1)
            record = records.get(words[0]) if words else None
            if record is None:
                continue
            requirement = " ".join(words[1].split()) if len(words) > 1 else ""
            justification = " ".join(row.cells[3].text.split())
            summaries[words[0]] = (requirement, justification)
    return summaries


def write_markdown(requirements, previous, path, source_name, from_table=None):
    lines = [
        "# Compliance table summaries",
        "",
        f"Extracted from `{source_name}`. Fill in the `R:` (requirement) and `J:`",
        "(justification) lines; keep each on one line. Do not edit the ## headings.",
        "Quoted text is for reading only; fix the full text in Word.",
        "Empty summaries keep the original text when reinjected.",
        "`FIX:` lines flag problems to correct in the proposal itself; delete a `FIX:` line",
        "once the proposal is corrected.",
        "",
    ]
    fixes = [
        (record["requirement_id"], previous[record["requirement_id"]]["fix"])
        for record in requirements
        if previous.get(record["requirement_id"], {}).get("fix")
    ]
    if fixes:
        lines += ["## Open problems in the proposal", ""]
        lines += [f"- ⚠️ **{requirement_id}**: {note}" for requirement_id, note in fixes]
        lines += [""]
    current_section = None
    for record in requirements:
        section = re.match(r"^(\d+)", record["origin"]).group(1)
        if section != current_section:
            current_section = section
            lines += [f"# RFP Section {section}", ""]
        kept = previous.get(record["requirement_id"])
        fix_line = [f"FIX: {kept['fix']}"] if kept and kept["fix"] else []
        if from_table is not None:
            requirement, justification = from_table.get(record["requirement_id"], ("", ""))
            # A cell holding the full original text is not a summary,
            # unless the previous .md already recorded that text as one.
            if requirement == " ".join(record["requirement"].split()) and not (
                kept and kept["requirement"] == requirement
            ):
                requirement = ""
            kept = {"requirement": requirement, "justification": justification}
        lines += [
            f"## {record['requirement_id']} | {record['reference']}",
            *fix_line,
            "",
            f"**Requirement** (RFP §{record['origin']})",
            "",
            quote(record["requirement"]),
            "",
            f"R: {kept['requirement'] if kept else ''}".rstrip(),
            "",
            "**Justification**",
            "",
            quote(record["justification"]),
            "",
            f"J: {kept['justification'] if kept else ''}".rstrip(),
            "",
        ]
    Path(path).write_text("\n".join(lines), encoding="utf-8")


def extract(source, output, previous_path=None, from_table=False):
    import rebuild_compliance

    previous_path = previous_path or output
    previous = read_summaries(previous_path, strict=False) if previous_path.is_file() else {}
    requirements, tables = rebuild_compliance.collect_requirements(Document(source))
    in_table = table_summaries(requirements, tables) if from_table else None
    write_markdown(requirements, previous, output, source.name, in_table)
    print(f"Wrote {output}: {len(requirements)} requirements")


def fill(markdown, entries_text):
    """Paste hand-written "ID | R | J [| FIX]" lines into the .md slots.

    "=" keeps a slot unchanged; an optional fourth field sets the FIX flag ("-" removes it).
    """
    entries = {}
    for line in entries_text.splitlines():
        if not line.strip():
            continue
        parts = re.split(r"\s*\|\s*", line.strip())
        if len(parts) not in (3, 4):
            raise RuntimeError(f"Expected 'ID | R | J [| FIX]': {line!r}")
        entries[parts[0]] = parts[1:]

    output = []
    current = None
    filled = set()
    for line in Path(markdown).read_text(encoding="utf-8").splitlines():
        block = BLOCK_PATTERN.match(line)
        if block:
            current = block.group(1)
            output.append(line)
            entry = entries.get(current)
            if entry and len(entry) == 3 and entry[2] != "-":
                output.append(f"FIX: {entry[2]}")
            continue
        entry = entries.get(current)
        if entry is None:
            output.append(line)
            continue
        if FIX_PATTERN.match(line) and len(entry) == 3:
            continue
        summary = SUMMARY_PATTERN.match(line)
        if summary:
            text = entry[0 if summary.group(1) == "R" else 1]
            line = line if text == "=" else f"{summary.group(1)}: {text}"
            filled.add(current)
        output.append(line)
    missing = sorted(set(entries) - filled)
    if missing:
        raise RuntimeError(f"Unknown requirement IDs: {', '.join(missing)}")
    Path(markdown).write_text("\n".join(output) + "\n", encoding="utf-8")
    print(f"Filled {len(filled)} blocks in {markdown}")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    subcommands = parser.add_subparsers(dest="command", required=True)
    extract_parser = subcommands.add_parser("extract", help="Write/refresh the summaries .md")
    extract_parser.add_argument("source", type=Path)
    extract_parser.add_argument("output", type=Path)
    extract_parser.add_argument(
        "--from-table",
        action="store_true",
        help="Take R/J summaries from the document's compliance table (e.g. after Word edits)",
    )
    extract_parser.add_argument(
        "--previous",
        type=Path,
        help="Earlier .md providing summaries and FIX flags (default: the output file)",
    )
    fill_parser = subcommands.add_parser(
        "fill", help="Paste 'ID | R | J [| FIX]' lines from stdin into the .md slots"
    )
    fill_parser.add_argument("markdown", type=Path)
    arguments = parser.parse_args()
    if arguments.command == "extract":
        extract(arguments.source, arguments.output, arguments.previous, arguments.from_table)
    elif arguments.command == "fill":
        fill(arguments.markdown, sys.stdin.read())


if __name__ == "__main__":
    main()
