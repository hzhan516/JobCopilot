import re
import tempfile
import uuid
from pathlib import Path

import httpx
from playwright.sync_api import sync_playwright

from app.schemas import ScrapeResult


# Standard browser headers to reduce the chance of bot detection on job boards.
# 使用标准桌面浏览器 UA，降低被招聘网站反爬拦截的概率。
DEFAULT_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    )
}


def _html_to_text(html: str) -> str:
    """Strip scripts, styles, and tags to produce readable plain text.
    HTML 转文本：移除 script/style/noscript 及标签，将 HTML 实体还原，
    保留页面正文供 LLM 提取职位信息。"""
    text = re.sub(r"<script.*?>.*?</script>", "", html, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<style.*?>.*?</style>", "", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<noscript.*?>.*?</noscript>", "", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"&nbsp;", " ", text, flags=re.IGNORECASE)
    text = re.sub(r"&amp;", "&", text, flags=re.IGNORECASE)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def _capture_screenshot(url: str) -> str | None:
    """Capture a full-page screenshot using Playwright for vision-based fallback parsing.
    使用 Playwright 捕获整页截图：在页面文本不足时作为 vision 模型的输入源，提高解析成功率。"""
    screenshots_dir = Path(tempfile.gettempdir()) / "resume_assistant_job_screenshots"
    screenshots_dir.mkdir(parents=True, exist_ok=True)

    screenshot_path = screenshots_dir / f"{uuid.uuid4().hex}.png"

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        try:
            page = browser.new_page(viewport={"width": 1440, "height": 2200})
            page.goto(url, wait_until="networkidle", timeout=30000)
            page.screenshot(path=str(screenshot_path), full_page=True)
        finally:
            browser.close()

    return str(screenshot_path)


def scrape_job_page(url: str, capture_screenshot: bool) -> ScrapeResult:
    """Fetch a job page, extract plain text, and optionally capture a screenshot for vision fallback.
    抓取职位页面：获取 HTML 后去噪提取文本；当启用图片校验且未提供外部截图时，自动截图备用。"""
    if not url.startswith(("http://", "https://")):
        raise ValueError("Only HTTP(S) URLs are supported for job scraping.")
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
        screenshot_url = _capture_screenshot(url)


    return ScrapeResult(
        markdown_text=markdown_text,
        screenshot_url=screenshot_url,
    )
