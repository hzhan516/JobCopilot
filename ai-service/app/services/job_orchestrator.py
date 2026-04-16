from app.schemas import AiResultEvent, JobParseCommand
from app.services.job_parser import parse_job_text, validate_job_with_vision
from app.services.web_scraper import scrape_job_page


def process_job(command: JobParseCommand) -> AiResultEvent:
    scrape_result = scrape_job_page(
        url=command.url,
        capture_screenshot=command.image_check_enabled,
    )

    parsed_content = parse_job_text(scrape_result.markdown_text)

    if command.image_check_enabled:
        parsed_content = validate_job_with_vision(
            parsed_content=parsed_content,
            page_text=scrape_result.markdown_text,
            screenshot_url=scrape_result.screenshot_url,
        )

    return AiResultEvent(
        referenceId=command.job_id,
        type="JOB_PARSE",
        status="COMPLETED",
        data=parsed_content.model_dump(),
        errorMessage=None,
        eventType="JOB",
    )
