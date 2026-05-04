from unittest.mock import patch

from app.schemas import JobParseCommand, ParsedJobContent, ScrapeResult
from app.services.job_orchestrator import process_job


def _job(title: str = "Frontend Engineer") -> ParsedJobContent:
    return ParsedJobContent(
        title=title,
        company="Acme",
        description="Build React applications.",
        requirements=["React", "TypeScript"],
    )


@patch("app.services.job_orchestrator.validate_job_with_vision")
@patch("app.services.job_orchestrator.parse_job_text")
@patch("app.services.job_orchestrator.scrape_job_page")
def test_process_job_uses_url_first_and_screenshot_to_validate(
    mock_scrape,
    mock_parse_text,
    mock_validate,
):
    mock_scrape.return_value = ScrapeResult(
        markdown_text="Frontend Engineer at Acme. " * 20,
        screenshot_url=None,
    )
    mock_parse_text.return_value = _job()
    mock_validate.return_value = _job("Senior Frontend Engineer")

    result = process_job(
        JobParseCommand(
            jobId="job-1",
            url="https://example.com/job",
            screenshotUrl="/api/storage/download?key=job.png",
        )
    )

    mock_scrape.assert_called_once()
    mock_parse_text.assert_called_once()
    mock_validate.assert_called_once()
    assert result.status == "COMPLETED"
    assert result.data["title"] == "Senior Frontend Engineer"


@patch("app.services.job_orchestrator.parse_job_from_image")
@patch("app.services.job_orchestrator.scrape_job_page")
def test_process_job_falls_back_to_screenshot_when_url_scrape_fails(
    mock_scrape,
    mock_parse_image,
):
    mock_scrape.side_effect = RuntimeError("blocked")
    mock_parse_image.return_value = _job()

    result = process_job(
        JobParseCommand(
            jobId="job-1",
            url="https://blocked.example/job",
            screenshotUrl="/api/storage/download?key=job.png",
        )
    )

    mock_parse_image.assert_called_once()
    _, kwargs = mock_parse_image.call_args
    assert kwargs["screenshot_url"] == "/api/storage/download?key=job.png"
    assert result.status == "COMPLETED"
    assert result.data["company"] == "Acme"
