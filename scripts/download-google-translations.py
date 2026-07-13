#!/usr/bin/env python3
"""Download the public translation sheet through the Google Sheets API."""

import csv
import json
import os
import shlex
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import urlopen


SPREADSHEET_ID = "1ZRfYHCARnPCnUEVZYo3Y_7qJGS9z7NRVg-Se7z3lHtE"
PREFERRED_TAB = "translation4"
ENV_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".vscode", ".env.mac")


def load_dotenv_file(path):
    """Load unset variables from a simple shell-compatible dotenv file."""
    if not os.path.isfile(path):
        return

    with open(path, encoding="utf-8") as environment_file:
        for line in environment_file:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            name, value = line.split("=", 1)
            name = name.strip()
            if not name or name in os.environ:
                continue
            try:
                parsed = shlex.split(value, comments=True, posix=True)
            except ValueError:
                continue
            os.environ[name] = parsed[0] if parsed else ""


def fetch_json(url):
    with urlopen(url, timeout=120) as response:
        return json.load(response)


def a1_quote_title(title):
    """Quote a sheet title for A1 notation."""
    return "'" + title.replace("'", "''") + "'"


def choose_tab(sheets):
    titles = [
        sheet.get("properties", {}).get("title")
        for sheet in sheets
        if sheet.get("properties", {}).get("title")
    ]

    requested = os.environ.get("GOOGLE_SHEETS_TAB")
    candidates = [requested, PREFERRED_TAB] if requested else [PREFERRED_TAB]
    for candidate in candidates:
        for title in titles:
            if title == candidate:
                return title
        for title in titles:
            if title.strip().casefold() == candidate.strip().casefold():
                return title

    return titles[0] if titles else None


def main() -> int:
    load_dotenv_file(ENV_FILE)

    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <destination.csv>", file=sys.stderr)
        return 2

    api_key = os.environ.get("GOOGLE_SHEETS_API_KEY")
    if not api_key:
        print("GOOGLE_SHEETS_API_KEY is not set", file=sys.stderr)
        return 2

    encoded_key = quote(api_key, safe="")

    metadata_url = (
        "https://sheets.googleapis.com/v4/spreadsheets/"
        f"{SPREADSHEET_ID}?fields=sheets.properties.title&key={encoded_key}"
    )
    try:
        metadata = fetch_json(metadata_url)
    except HTTPError as error:
        details = error.read().decode("utf-8", errors="replace").strip()
        print(f"Google Sheets API metadata request failed: {error}", file=sys.stderr)
        if details:
            print(details, file=sys.stderr)
        return 1
    except (URLError, TimeoutError, json.JSONDecodeError) as error:
        print(f"Google Sheets API metadata request failed: {error}", file=sys.stderr)
        return 1

    tab = choose_tab(metadata.get("sheets", []))
    if not tab:
        print("Google Sheets API returned no worksheet titles", file=sys.stderr)
        return 1

    encoded_range = quote(a1_quote_title(tab), safe="")
    url = (
        "https://sheets.googleapis.com/v4/spreadsheets/"
        f"{SPREADSHEET_ID}/values/{encoded_range}"
        f"?majorDimension=ROWS&key={encoded_key}"
    )

    try:
        payload = fetch_json(url)
    except HTTPError as error:
        details = error.read().decode("utf-8", errors="replace").strip()
        print(f"Google Sheets API download failed: {error}", file=sys.stderr)
        if details:
            print(details, file=sys.stderr)
        return 1
    except (URLError, TimeoutError, json.JSONDecodeError) as error:
        print(f"Google Sheets API download failed: {error}", file=sys.stderr)
        return 1

    values = payload.get("values")
    if not isinstance(values, list) or not values:
        print("Google Sheets API returned no values", file=sys.stderr)
        return 1

    with open(sys.argv[1], "w", encoding="utf-8", newline="") as destination:
        csv.writer(destination, lineterminator="\n").writerows(values)

    return 0


if __name__ == "__main__":
    sys.exit(main())