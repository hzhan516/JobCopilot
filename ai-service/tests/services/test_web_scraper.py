"""Test web scraper module.
网页抓取模块测试：覆盖正常抓取、文本提取、截图生成、异常降级等路径。
"""
from unittest.mock import MagicMock, patch

import httpx
import pytest

from app.services.web_scraper import (
    _html_to_text,
    _capture_screenshot,
    scrape_job_page,
)
from app.schemas import ScrapeResult


# ── _html_to_text ──────────────────────────────────────────

def test_html_to_text_basic():
    """Should strip tags and extract readable text.
    应移除标签并提取可读文本。"""
    html = "<html><body><h1>Title</h1><p>Paragraph 1.</p><p>Paragraph 2.</p></body></html>"
    result = _html_to_text(html)
    assert "Title" in result
    assert "Paragraph 1." in result
    assert "Paragraph 2." in result


def test_html_to_text_removes_scripts():
    """Should remove script and style tags completely.
    应彻底移除 script 和 style 标签。"""
    html = "<html><script>alert('xss')</script><body><p>Safe text.</p></body></html>"
    result = _html_to_text(html)
    assert "alert" not in result
    assert "xss" not in result
    assert "Safe text." in result


def test_html_to_text_empty():
    """Empty HTML should return empty string.
    空 HTML 应返回空字符串。"""
    assert _html_to_text("") == ""


def test_html_to_text_entities():
    """Should decode HTML entities.
    应解码 HTML 实体。"""
    html = "<p>AT&amp;T&nbsp;Corp</p>"
    result = _html_to_text(html)
    assert "AT&T" in result
    assert "Corp" in result


# ── scrape_job_page ────────────────────────────────────────

@patch("app.services.web_scraper.httpx.get")
def test_scrape_success_no_screenshot(mock_get):
    """Successful scrape without screenshot should return ScrapeResult.
    成功抓取且不截图时应返回 ScrapeResult。"""
    mock_response = MagicMock()
    mock_response.text = "<html><body><h1>Job Title</h1><p>Description here.</p></body></html>"
    mock_response.raise_for_status.return_value = None
    mock_get.return_value = mock_response

    result = scrape_job_page("https://example.com/job", capture_screenshot=False)

    assert isinstance(result, ScrapeResult)
    assert "Job Title" in result.markdown_text
    assert "Description here." in result.markdown_text
    assert result.screenshot_url is None
    mock_get.assert_called_once()


@patch("app.services.web_scraper._capture_screenshot")
@patch("app.services.web_scraper.httpx.get")
def test_scrape_with_screenshot(mock_get, mock_screenshot):
    """When capture_screenshot=True, screenshot_url should be populated.
    启用截图时，screenshot_url 应被填充。"""
    mock_response = MagicMock()
    mock_response.text = "<html><body><p>Content.</p></body></html>"
    mock_response.raise_for_status.return_value = None
    mock_get.return_value = mock_response
    mock_screenshot.return_value = "/tmp/screenshot.png"

    result = scrape_job_page("https://example.com/job", capture_screenshot=True)

    assert result.screenshot_url == "/tmp/screenshot.png"
    mock_screenshot.assert_called_once_with("https://example.com/job")


@patch("app.services.web_scraper.httpx.get")
def test_scrape_empty_content(mock_get):
    """Empty page content should still return a ScrapeResult.
    空页面内容应返回 ScrapeResult。"""
    mock_response = MagicMock()
    mock_response.text = ""
    mock_response.raise_for_status.return_value = None
    mock_get.return_value = mock_response

    result = scrape_job_page("https://example.com/job", capture_screenshot=False)

    assert result.markdown_text == ""
    assert result.screenshot_url is None


@patch("app.services.web_scraper.httpx.get")
def test_scrape_http_error(mock_get):
    """HTTP error should be propagated.
    HTTP 错误应被传播。"""
    mock_get.side_effect = httpx.HTTPStatusError(
        "403 Forbidden",
        request=httpx.Request("GET", "https://example.com/job"),
        response=httpx.Response(403),
    )

    with pytest.raises(httpx.HTTPStatusError):
        scrape_job_page("https://example.com/job", capture_screenshot=False)


@patch("app.services.web_scraper.httpx.get")
def test_scrape_network_timeout(mock_get):
    """Network timeout should be propagated.
    网络超时应被传播。"""
    mock_get.side_effect = httpx.TimeoutException("Connection timed out")

    with pytest.raises(httpx.TimeoutException):
        scrape_job_page("https://example.com/job", capture_screenshot=False)


def test_scrape_invalid_url():
    """Non-HTTP URL should raise ValueError.
    非 HTTP URL 应抛出 ValueError。"""
    with pytest.raises(ValueError, match="Only HTTP"):
        scrape_job_page("ftp://example.com/job", capture_screenshot=False)
