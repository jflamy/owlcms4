#!/usr/bin/env python3
"""Move unreachable Markdown and unused media into docs/obsolete/.

Candidates come from the reachability reports. Each candidate is moved to the
same relative path below docs/obsolete/. Use --dry-run to inspect the move plan.
"""

import argparse
from pathlib import Path
import shutil

SCRIPTS = Path(__file__).resolve().parent
DOCS = SCRIPTS.parent
OBSOLETE = DOCS / "obsolete"
REPORTS = SCRIPTS / "archive" / "reports"
UNREACHABLE_REPORT = REPORTS / "unreachable_reanalysis.txt"
IMAGE_REPORT = REPORTS / "image_refs_by_reachability.txt"
LOG = REPORTS / "move_log.txt"
UNUSED_HEADING = "UNUSED_IMAGES:"
MEDIA_ROOTS = {"img", "nimg"}


def log(message: str):
    with LOG.open("a", encoding="utf-8") as output:
        output.write(message + "\n")
    print(message)


def read_unreachable_markdown() -> list[Path]:
    if not UNREACHABLE_REPORT.exists():
        raise FileNotFoundError(f"Reachability report not found: {UNREACHABLE_REPORT}")

    candidates = []
    for line in UNREACHABLE_REPORT.read_text(encoding="utf-8").splitlines():
        candidate = line.strip()
        if not candidate or candidate == "_sidebar.md":
            continue
        relative_path = Path(candidate)
        if relative_path.suffix.lower() != ".md" or ".." in relative_path.parts:
            raise ValueError(f"Invalid unreachable Markdown path in report: {candidate}")
        candidates.append(relative_path)
    return candidates


def read_orphan_media() -> list[Path]:
    if not IMAGE_REPORT.exists():
        raise FileNotFoundError(f"Reachability report not found: {IMAGE_REPORT}")

    candidates = []
    reading_candidates = False
    for line in IMAGE_REPORT.read_text(encoding="utf-8").splitlines():
        candidate = line.strip()
        if candidate == UNUSED_HEADING:
            reading_candidates = True
            continue
        if not reading_candidates or not candidate:
            continue

        relative_path = Path(candidate)
        if relative_path.parts[0] not in MEDIA_ROOTS or ".." in relative_path.parts:
            raise ValueError(f"Invalid orphan-media path in report: {candidate}")
        candidates.append(relative_path)

    if not reading_candidates:
        raise ValueError(f"Missing {UNUSED_HEADING} in {IMAGE_REPORT}")
    return candidates


def move_candidates(candidates: list[Path], label: str, dry_run: bool):
    for relative_path in candidates:
        source = DOCS / relative_path
        destination = OBSOLETE / relative_path

        if not source.is_file():
            log(f"MISSING {label}: {relative_path}")
            continue
        if destination.exists():
            log(f"DESTINATION EXISTS {label}: {relative_path}")
            continue

        if dry_run:
            log(f"WOULD MOVE {label}: {relative_path} -> obsolete/{relative_path}")
            continue

        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(source), str(destination))
        log(f"MOVED {label}: {relative_path} -> obsolete/{relative_path}")


def move_unreachable_markdown(dry_run: bool):
    move_candidates(read_unreachable_markdown(), "MD", dry_run)


def move_orphan_media(dry_run: bool):
    move_candidates(read_orphan_media(), "MEDIA", dry_run)


def main():
    parser = argparse.ArgumentParser(description="Move exact-path orphan media to docs/obsolete.")
    parser.add_argument("--dry-run", action="store_true", help="Report planned moves without changing files.")
    arguments = parser.parse_args()

    REPORTS.mkdir(parents=True, exist_ok=True)
    LOG.write_text("Move log - generated\n", encoding="utf-8")
    log(f"Starting docs obsolete move ({'dry run' if arguments.dry_run else 'move'})")
    move_unreachable_markdown(arguments.dry_run)
    move_orphan_media(arguments.dry_run)
    log("Move operation completed")


if __name__ == "__main__":
    main()
