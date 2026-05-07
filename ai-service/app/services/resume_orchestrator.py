from app.schemas import AiResultEvent, ResumeParseCommand
from app.services.file_parser import download_file_bytes, extract_resume_text
from app.services.resume_parser import parse_resume_text


def process_resume(command: ResumeParseCommand) -> AiResultEvent:
    """Orchestrate resume file download, text extraction, and structured parsing.
    简历解析编排器：串联文件下载、格式识别、文本提取与 LLM 结构化解析，
    将原始文件转化为后端可直接消费的标准化简历数据。"""
    file_bytes = download_file_bytes(command.file_url)
    resume_text = extract_resume_text(file_bytes, command.file_type)
    parsed_content = parse_resume_text(resume_text)

    return AiResultEvent(
        referenceId=command.resume_id,
        type="RESUME_PARSE",
        status="COMPLETED",
        data={
            "parsedContent": parsed_content.model_dump(),
            "summary": "",
        },
        errorMessage=None,
        eventType="RESUME",
    )
