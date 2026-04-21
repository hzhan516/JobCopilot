from app.schemas import AiResultEvent, ResumeParseCommand
from app.services.file_parser import download_file_bytes, extract_resume_text
from app.services.resume_parser import parse_resume_text


def process_resume(command: ResumeParseCommand) -> AiResultEvent:
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
