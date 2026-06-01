from app.schemas import AiResultEvent, JobParseCommand
from app.services.job_parser import (
    is_job_content_incomplete,
    parse_job_from_image,
    parse_job_text,
    validate_job_with_vision,
)
from app.services.web_scraper import scrape_job_page


# Minimum text length to trust scraped content before falling back to vision.
# 抓取文本可信度阈值：低于此长度时优先使用截图解析，避免脏数据进入下游。
MIN_SCRAPED_TEXT_LENGTH = 200


def process_job(command: JobParseCommand) -> AiResultEvent:
    """Orchestrate job parsing with a scrape-first, vision-fallback strategy.
    职位解析编排器：优先从页面文本提取结构化信息；当文本不足或字段缺失时，
    降级到截图 OCR/vision 解析，最大化解析成功率并减少 LLM vision 调用成本。"""
    scrape_result = None
    scrape_error: Exception | None = None
    screenshot_url = command.screenshot_url
    if command.screenshot_base64:
        screenshot_url = command.screenshot_base64

    try:
        scrape_result = scrape_job_page(
            url=command.url,
            capture_screenshot=command.image_check_enabled and not screenshot_url,
        )
        if not screenshot_url:
            screenshot_url = scrape_result.screenshot_url
    except Exception as exc:
        scrape_error = exc

    parsed_content = None
    scraped_text = scrape_result.markdown_text if scrape_result else ""

    if scraped_text.strip() and len(scraped_text.strip()) >= MIN_SCRAPED_TEXT_LENGTH:
        try:
            parsed_content = parse_job_text(scraped_text)
        except Exception as exc:
            scrape_error = exc

    if parsed_content is not None and not is_job_content_incomplete(parsed_content):
        if screenshot_url:
            parsed_content = validate_job_with_vision(
                parsed_content=parsed_content,
                page_text=scraped_text,
                screenshot_url=screenshot_url,
            )
    elif screenshot_url:
        parsed_content = parse_job_from_image(
            screenshot_url=screenshot_url,
            context_text=scraped_text,
        )
    else:
        if scrape_error:
            raise scrape_error
        raise ValueError("Unable to parse job posting from URL and no screenshotUrl was provided.")

    return AiResultEvent(
        referenceId=command.job_id,
        type="JOB_PARSE",
        status="COMPLETED",
        data=parsed_content.model_dump(by_alias=True),
        errorMessage=None,
        eventType="JOB",
    )
