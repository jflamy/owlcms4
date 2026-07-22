#!/usr/bin/env python3
"""Find local 404s reachable from docs/_sidebar.md.

The checker does not make network requests. Absolute URLs, mailto links, and
other URI schemes are external. It follows local Docsify and Markdown links
from the sidebar and checks that each referenced local page, image, or download
exists. It also reports stale heading anchors, but never unreferenced files.
"""

from __future__ import annotations

import re
import sys
import unicodedata
from collections import deque
from dataclasses import dataclass
from difflib import get_close_matches
from pathlib import Path
from urllib.parse import unquote, urlsplit


DOCS = Path(__file__).resolve().parent.parent
SIDEBAR = DOCS / "_sidebar.md"
MARKDOWN_SUFFIXES = {".md", ".markdown", ".mdx"}
EXCLUDED_DIRECTORIES = {"obsolete", "scripts"}
INLINE_LINK = re.compile(r"(?<!!)\[[^\]]*]\(([^)]+)\)")
IMAGE_LINK = re.compile(r"!\[[^\]]*]\(([^)]+)\)")
HTML_LINK = re.compile(r"<(?:a|img)\b[^>]+?(?:href|src)\s*=\s*['\"]([^'\"]+)['\"]", re.IGNORECASE)
HEADING = re.compile(r"^#{1,6}\s+(.+?)\s*#*\s*$")


@dataclass(frozen=True)
class Link:
    target: str
    line: int
    image: bool = False


@dataclass(frozen=True)
class Problem:
    source: Path
    line: int
    target: str
    message: str
    detail: str


def relative(path: Path) -> str:
    return path.relative_to(DOCS).as_posix()


def included_markdown(path: Path) -> bool:
    return path.suffix.lower() in MARKDOWN_SUFFIXES and not any(part in EXCLUDED_DIRECTORIES for part in path.relative_to(DOCS).parts)


def markdown_files() -> set[Path]:
    return {path.resolve() for path in DOCS.rglob("*") if path.is_file() and included_markdown(path)}


def similar_markdown_paths(candidate: Path, known_markdown: set[Path]) -> list[str]:
    """Return likely Docsify page paths for a missing local Markdown route."""
    route = relative(candidate.with_suffix(""))
    routes = {relative(path.with_suffix("")): path for path in known_markdown}
    matches = get_close_matches(route, routes, n=3, cutoff=0.35)
    return [f"docs/{routes[match].relative_to(DOCS).as_posix()}" for match in matches]


def markdown_slug(text: str) -> str:
    text = unicodedata.normalize("NFKD", text).lower()
    text = "".join(char for char in text if not unicodedata.combining(char))
    text = re.sub(r"[`*_~]", "", text)
    text = re.sub(r"[^\w\s-]", "", text, flags=re.UNICODE)
    return re.sub(r"[\s-]+", "-", text).strip("-")


def heading_slugs(path: Path) -> set[str]:
    counts: dict[str, int] = {}
    slugs = set()
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        match = HEADING.match(line)
        if not match:
            continue
        slug = markdown_slug(match.group(1))
        if not slug:
            continue
        count = counts.get(slug, 0)
        counts[slug] = count + 1
        slugs.add(slug if count == 0 else f"{slug}-{count}")
    return slugs


def links_in(path: Path) -> list[Link]:
    links: list[Link] = []
    for line_number, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), start=1):
        for pattern, image in ((IMAGE_LINK, True), (INLINE_LINK, False), (HTML_LINK, False)):
            for match in pattern.finditer(line):
                target = match.group(1).strip()
                if target.startswith("<") and target.endswith(">"):
                    target = target[1:-1].strip()
                links.append(Link(target, line_number, image))
    return links


def is_external(target: str) -> bool:
    return bool(urlsplit(target).scheme) or target.startswith("//")


def split_target(target: str) -> tuple[str, str | None]:
    target = re.split(r"\s+['\"]:", target, maxsplit=1)[0]
    target = target.replace("\\", "/")
    parsed = urlsplit(target)
    path = unquote(parsed.path)
    fragment = unquote(parsed.fragment) or None
    if "#/" in target:
        route = target.split("#/", 1)[1]
        route_path, _, route_fragment = route.partition("?")
        return unquote(route_path), unquote(route_fragment.split("id=", 1)[1]) if "id=" in route_fragment else None
    return path, fragment


def resolve(source: Path, target: str, known_markdown: set[Path]) -> tuple[Path | None, str | None, str | None, str]:
    """Return destination, anchor, error message, and an actionable detail."""
    if not target or target.startswith("#") and not target.startswith("#/"):
        return source, target[1:] or None, None, ""
    if is_external(target):
        return None, None, None, ""

    path_text, anchor = split_target(target)
    if not path_text:
        return source, anchor, None, ""

    candidate = Path(path_text)
    docsify_route = candidate.suffix == "" and not path_text.startswith(("./", "../", "/"))
    if docsify_route:
        candidate = DOCS / f"{path_text}.md"
    elif path_text.startswith("/"):
        candidate = DOCS / path_text.lstrip("/")
    else:
        candidate = source.parent / candidate

    if candidate.suffix == "" and not candidate.exists():
        candidate = candidate.with_suffix(".md")
    candidate = candidate.resolve()
    try:
        candidate.relative_to(DOCS)
    except ValueError:
        return None, anchor, "points outside docs/", "Move the file under docs/ or replace this with a published URL."
    if not candidate.is_file():
        path = f"docs/{relative(candidate)}"
        if candidate.suffix.lower() in MARKDOWN_SUFFIXES:
            suggestions = similar_markdown_paths(candidate, known_markdown)
            detail = f"Link to a real Docsify page or create {path}."
            if suggestions:
                detail += " Similar pages: " + ", ".join(suggestions)
            return None, anchor, "target file does not exist", detail
        return None, anchor, "target file does not exist", f"Add the asset at {path} or change this to a published URL."
    if candidate.suffix.lower() in MARKDOWN_SUFFIXES and candidate not in known_markdown:
        return None, anchor, "target Markdown file is excluded from this documentation set", "Move it outside an excluded directory or update the link."
    return candidate, anchor, None, ""


def check() -> tuple[list[Problem], set[Path], set[Path]]:
    known_markdown = markdown_files()
    if not SIDEBAR.is_file():
        return [Problem(SIDEBAR, 1, "", "_sidebar.md does not exist", "Create docs/_sidebar.md.")], set(), known_markdown

    reachable = {SIDEBAR.resolve()}
    pending = deque([SIDEBAR.resolve()])
    asset_sources = [DOCS / "index.html"] if (DOCS / "index.html").is_file() else []
    problems: list[Problem] = []
    checked = set()

    while pending or asset_sources:
        source = pending.popleft() if pending else asset_sources.pop()
        if source in checked:
            continue
        checked.add(source)
        for link in links_in(source):
            destination, anchor, error, detail = resolve(source, link.target, known_markdown)
            if error:
                problems.append(Problem(source, link.line, link.target, error, detail))
                continue
            if destination is None:
                continue
            if anchor and destination.suffix.lower() in MARKDOWN_SUFFIXES:
                normalized_anchor = markdown_slug(anchor)
                headings = heading_slugs(destination)
                if normalized_anchor not in headings:
                    suggestions = get_close_matches(normalized_anchor, sorted(headings), n=3, cutoff=0.4)
                    detail = "Change the fragment to an existing heading"
                    if suggestions:
                        detail += ": " + ", ".join(f"#{suggestion}" for suggestion in suggestions)
                    else:
                        detail += " or add a matching heading."
                    problems.append(Problem(source, link.line, link.target, f"stale heading anchor '#{anchor}'", detail))
            if destination.suffix.lower() in MARKDOWN_SUFFIXES and destination not in reachable:
                reachable.add(destination)
                pending.append(destination)

    return problems, reachable, known_markdown


def main() -> int:
    problems, reachable, _ = check()

    for problem in sorted(problems, key=lambda item: (relative(item.source), item.line, item.target)):
        print(f"{relative(problem.source)}:{problem.line}: {problem.message}")
        print(f"  link: {problem.target}")
        print(f"  fix:  {problem.detail}")

    stale_anchors = sum(problem.message.startswith("stale heading anchor") for problem in problems)
    missing_targets = len(problems) - stale_anchors
    print(
        f"Checked {len(reachable)} Markdown files reachable from _sidebar.md; found "
        f"{missing_targets} local targets that would 404 and {stale_anchors} stale heading anchors."
    )
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())