#!/usr/bin/env python3
"""Validate repository-local Markdown links without making network requests."""

from __future__ import annotations

import re
import sys
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
LINK_RE = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
FENCE_RE = re.compile(r"^\s*(```|~~~)")
EXTERNAL_SCHEMES = {"http", "https", "mailto", "tel", "data"}


def markdown_files() -> list[Path]:
    files = list(ROOT.glob("README*.md"))
    for folder in (ROOT / "docs", ROOT / ".skill"):
        if folder.exists():
            files.extend(folder.rglob("*.md"))
    return sorted(set(files))


def local_target(raw: str) -> str | None:
    value = raw.strip()
    if value.startswith("<") and ">" in value:
        value = value[1 : value.index(">")]
    elif " " in value:
        value = value.split(" ", 1)[0]
    if not value or value.startswith("#"):
        return None
    parsed = urlsplit(value)
    if parsed.scheme.lower() in EXTERNAL_SCHEMES or value.startswith("//"):
        return None
    return unquote(parsed.path)


def validate(path: Path) -> list[str]:
    failures: list[str] = []
    in_fence = False
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if FENCE_RE.match(line):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        for match in LINK_RE.finditer(line):
            target_text = local_target(match.group(1))
            if target_text is None:
                continue
            target = (ROOT / target_text.lstrip("/")) if target_text.startswith("/") else (path.parent / target_text)
            target = target.resolve()
            if not target.exists():
                failures.append(f"{path.relative_to(ROOT)}:{number}: missing {match.group(1)}")
    return failures


def main() -> int:
    failures = [failure for path in markdown_files() for failure in validate(path)]
    if failures:
        print("Local Markdown link validation failed:", file=sys.stderr)
        print("\n".join(failures), file=sys.stderr)
        return 1
    print(f"Validated local links in {len(markdown_files())} Markdown files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
