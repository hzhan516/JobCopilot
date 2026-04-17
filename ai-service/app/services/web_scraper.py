import re
import tempfile
from pathlib import Path

import httpx
from playwright.sync_api import sync_playwright

from app.schemas import ScrapeResult


DEFAULT_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    )
}


def _html_to_text(html: str) -> str:
    text = re.sub(r"<script.*?>.*?</script>", "", html, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<style.*?>.*?</style>", "", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<noscript.*?>.*?</noscript>", "", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"&nbsp;", " ", text, flags=re.IGNORECASE)
    text = re.sub(r"&amp;", "&", text, flags=re.IGNORECASE)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def _capture_screenshot(url: str) -> str | None:
    screenshots_dir = Path(tempfile.gettempdir()) / "resume_assistant_job_screenshots"
    screenshots_dir.mkdir(parents=True, exist_ok=True)

    screenshot_path = screenshots_dir / f"{next(tempfile._get_candidate_names())}.png"

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 2200})
        page.goto(url, wait_until="networkidle", timeout=30000)
        page.screenshot(path=str(screenshot_path), full_page=True)
        browser.close()

    return str(screenshot_path)


def scrape_job_page(url: str, capture_screenshot: bool) -> ScrapeResult:
    response = httpx.get(
        url,
        timeout=20.0,
        follow_redirects=True,
        headers=DEFAULT_HEADERS,
    )
    response.raise_for_status()

    html = response.text
    markdown_text = _html_to_text(html)

    screenshot_url = None
    if capture_screenshot:
        _ = _capture_screenshot(url)


    return ScrapeResult(
        markdown_text=markdown_text,
        screenshot_url=screenshot_url,
    )
