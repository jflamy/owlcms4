#!/usr/bin/env python3
"""Convert IWF records scraped from VS Code's approved browser tab to Excel.

Usage:
    python3 scrape_iwf_records_playwright.py records.json [--output <output.xlsx>]
"""
from __future__ import annotations

import argparse
import json
from datetime import datetime
from pathlib import Path

from scrape_iwf_records import (
    _default_output_dir,
    _maybe_open_output,
    _normalize_date,
    copy_to_destination,
    write_to_excel,
)


def main() -> int:
    parser = argparse.ArgumentParser(description="Convert VS Code browser-scraped IWF records to Excel.")
    parser.add_argument("input", type=Path, help="JSON exported from the approved VS Code browser tab")
    parser.add_argument("--output", type=Path, default=None, help="Final owlCMS xlsx file")
    parser.add_argument("--output-dir", type=Path, default=None, help="Directory for timestamped output")
    args = parser.parse_args()

    timestamp = datetime.now().strftime("%Y-%m-%d_%H%M%S")
    destination = args.output.expanduser() if args.output else None
    output_dir = args.output_dir.expanduser() if args.output_dir else _default_output_dir("IWF")
    output_dir.mkdir(parents=True, exist_ok=True)
    output_name = f"{args.output.stem}_{timestamp}{args.output.suffix or '.xlsx'}" if args.output else f"IWF_scraped_{timestamp}.xlsx"
    output_path = output_dir / output_name

    with args.input.expanduser().open(encoding="utf-8") as input_file:
        records = json.load(input_file)
    for record in records:
        record["Born"] = _normalize_date(record.get("Born", ""))
        record["Date"] = _normalize_date(record.get("Date", ""))

    if not records:
        print("No records found!")
        return 1

    print(f"\nTotal records scraped: {len(records)}")
    write_to_excel(records, output_path)
    final_path = copy_to_destination(output_path, destination) if destination else output_path
    _maybe_open_output(final_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
