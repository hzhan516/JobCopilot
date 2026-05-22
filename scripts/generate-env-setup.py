#!/usr/bin/env python3
"""
Generate / update docs/deployment/env-setup.html from .env.example.

This script safely injects the content of .env.example into the static HTML
configurator as a JSON string, avoiding JavaScript syntax errors caused by
backticks, ${}, quotes, or other special characters.

Usage:
    python scripts/generate-env-setup.py
"""

import json
import sys
from pathlib import Path


def main() -> int:
    project_root = Path(__file__).resolve().parent.parent
    env_path = project_root / ".env.example"
    html_path = project_root / "docs" / "deployment" / "env-setup.html"

    if not env_path.exists():
        print(f"Error: .env.example not found at {env_path}", file=sys.stderr)
        return 1

    if not html_path.exists():
        print(f"Error: env-setup.html not found at {html_path}", file=sys.stderr)
        return 1

    env_content = env_path.read_text(encoding="utf-8")
    # json.dumps safely escapes quotes, backslashes, newlines, backticks, $, etc.
    safe_json = json.dumps(env_content)

    html_content = html_path.read_text(encoding="utf-8")

    # Find the placeholder block and replace it
    start_marker = "// ENV_EXAMPLE_TEMPLATE_PLACEHOLDER_START"
    end_marker = "// ENV_EXAMPLE_TEMPLATE_PLACEHOLDER_END"

    start_idx = html_content.find(start_marker)
    end_idx = html_content.find(end_marker)

    if start_idx == -1 or end_idx == -1 or end_idx <= start_idx:
        print(
            "Error: Could not find the ENV_EXAMPLE_TEMPLATE placeholder markers in env-setup.html. "
            "Ensure the file contains the start/end marker comments.",
            file=sys.stderr,
        )
        return 1

    # Include the end marker in the slice
    end_idx += len(end_marker)

    # Build replacement: keep markers, replace the line between them
    new_block = (
        start_marker + "\n"
        "    window.ENV_EXAMPLE_TEMPLATE = " + safe_json + ";\n"
        "    " + end_marker
    )

    new_html = html_content[:start_idx] + new_block + html_content[end_idx:]

    # Sanity check: verify the injected string can be parsed back
    # Use string length to extract exactly the JSON we injected
    # (avoiding issues where .env.example itself contains semicolons)
    js_assign = "window.ENV_EXAMPLE_TEMPLATE = "
    assign_idx = new_html.find(js_assign)
    if assign_idx == -1:
        print("Error: Sanity check failed — could not find assignment.", file=sys.stderr)
        return 1

    json_start = assign_idx + len(js_assign)
    json_str = new_html[json_start:json_start + len(safe_json)]
    try:
        parsed = json.loads(json_str)
        assert isinstance(parsed, str)
        assert len(parsed) == len(env_content)
    except (json.JSONDecodeError, AssertionError) as exc:
        print(f"Error: Sanity check failed — injected JSON is invalid: {exc}", file=sys.stderr)
        return 1

    html_path.write_text(new_html, encoding="utf-8")
    print(f"Updated {html_path} with latest .env.example ({len(env_content)} chars, {env_content.count(chr(10))} lines)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
