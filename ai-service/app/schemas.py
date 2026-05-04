from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class AppBaseModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True)


class JobParseCommand(AppBaseModel):
    job_id: str = Field(alias="jobId")
    url: str
    image_check_enabled: bool = Field(default=False, alias="imageCheckEnabled")
    screenshot_url: str | None = Field(default=None, alias="screenshotUrl")
    screenshot_base64: str | None = Field(default=None, alias="screenshotBase64")


class ResumeParseCommand(AppBaseModel):
    resume_id: str = Field(alias="resumeId")
    file_url: str = Field(alias="fileUrl")
    file_type: str = Field(alias="format")


class VectorGenCommand(AppBaseModel):
    reference_id: str = Field(alias="referenceId")
    entity_type: str = Field(alias="entityType")
    text: str


class ConversationMessage(AppBaseModel):
    role: str
    content: str
    file_url: str | None = Field(default=None, alias="fileUrl")


class ConversationRequestCommand(AppBaseModel):
    conversation_id: str = Field(alias="conversationId")
    user_id: str = Field(alias="userId")
    message_history: list[ConversationMessage] = Field(default_factory=list, alias="messageHistory")
    current_message: str = Field(alias="currentMessage")
    file_urls: list[str] = Field(default_factory=list, alias="fileUrls")
    resume_version_id: str | None = Field(default=None, alias="resumeVersionId")
    resume_text: str | None = Field(default=None, alias="resumeText")
    primary_job_text: str | None = Field(default=None, alias="primaryJobText")
    related_job_texts: list[str] | None = Field(default=None, alias="relatedJobTexts")
    init: bool | None = None
    locale: str | None = None


class JobRankCommand(AppBaseModel):
    match_id: str = Field(alias="matchId")
    user_id: str = Field(alias="userId")
    resume_version_id: str = Field(alias="resumeVersionId")
    resume_text: str = Field(default="", alias="resumeText")
    query: str | None = Field(default=None)
    recalled_job_ids: list[str] = Field(default_factory=list, alias="recalledJobIds")
    job_details: dict[str, Any] = Field(default_factory=dict, alias="jobDetails")


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


class JobMatchRequest(AppBaseModel):
    user_id: str = Field(alias="userId")
    query: str
    top_k: int = Field(default=10, alias="topK")
    filters: dict[str, str] = Field(default_factory=dict)


class MatchFactors(AppBaseModel):
    skill_match: float = Field(alias="skillMatch")
    experience_match: float = Field(alias="experienceMatch")
    location_match: float = Field(alias="locationMatch")


class MatchItem(AppBaseModel):
    job_id: str = Field(alias="jobId")
    title: str
    company: str
    match_score: float = Field(alias="matchScore")
    match_factors: MatchFactors = Field(alias="matchFactors")
    description: str


class JobMatchResponse(AppBaseModel):
    matches: list[MatchItem]
    total: int
    recall_time: int = Field(alias="recallTime")
    rank_time: int = Field(alias="rankTime")


class JobRankResultItem(AppBaseModel):
    job_id: str = Field(alias="jobId")
    title: str
    company: str
    match_score: float = Field(alias="matchScore")
    match_factors: MatchFactors = Field(alias="matchFactors")
    description: str
    match_reason: str | None = Field(default=None, alias="matchReason")


class JobRankResultPayload(AppBaseModel):
    match_id: str = Field(alias="matchId")
    status: str
    rank_time_ms: int = Field(alias="rankTimeMs")
    ranked_results: list[JobRankResultItem] = Field(default_factory=list, alias="rankedResults")
    error_message: str | None = Field(default=None, alias="errorMessage")
