from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class AppBaseModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True)


class JobParseCommand(AppBaseModel):
    job_id: str = Field(alias="jobId")
    url: str
    image_check_enabled: bool = Field(alias="imageCheckEnabled")


class ResumeParseCommand(AppBaseModel):
    resume_id: str = Field(alias="resumeId")
    file_url: str = Field(alias="fileUrl")
    format: str


class VectorGenCommand(AppBaseModel):
    reference_id: str = Field(alias="referenceId")
    entity_type: str = Field(alias="entityType")
    text: str


class ScrapeResult(AppBaseModel):
    markdown_text: str
    screenshot_url: str | None = None


class ParsedJobContent(AppBaseModel):
    title: str
    company: str
    description: str
    requirements: list[str] = Field(default_factory=list)


class ParsedResumeContent(AppBaseModel):
    name: str | None = None
    email: str | None = None
    skills: list[str] = Field(default_factory=list)
    experience: list[dict[str, Any]] = Field(default_factory=list)


class AiResultEvent(AppBaseModel):
    reference_id: str = Field(alias="referenceId")
    type: str
    status: str
    data: dict[str, Any] | None = None
    error_message: str | None = Field(default=None, alias="errorMessage")
    event_type: str | None = Field(default=None, alias="eventType")


class SuitabilityRequest(AppBaseModel):
    resume: ParsedResumeContent
    job: ParsedJobContent


class SuitabilityBreakdown(AppBaseModel):
    skill_score: float = Field(alias="skillScore")
    experience_score: float = Field(alias="experienceScore")
    overall_score: float = Field(alias="overallScore")


class SuitabilityResponse(AppBaseModel):
    suitable: bool
    summary: str
    breakdown: SuitabilityBreakdown
    vertex_score: float = Field(alias="vertexScore")
    dataset_score: float | None = Field(default=None, alias="datasetScore")
    final_score: float = Field(alias="finalScore")