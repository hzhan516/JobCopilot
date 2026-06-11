"""Pydantic schemas for API commands, events, and service payloads used by the AI service.
AI 服务与后端、MQ 之间的数据契约层，统一序列化/反序列化行为并启用别名映射。
"""

from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator


class AppBaseModel(BaseModel):
    """Base model enabling field population by alias for backward-compatible JSON contracts.
    启用别名映射的基类，保证前端 camelCase 与后端 snake_case 的兼容。"""

    model_config = ConfigDict(populate_by_name=True)


# ── Experience ──────────────────────────────────────────────────────────
class ExperienceItem(AppBaseModel):
    """Single work-experience entry extracted from a resume.
    工作经历条目：从简历解析中提取的单条工作经历。"""

    company: str | None = None
    title: str | None = None
    duration: str | None = None
    summary: str | None = None


# ── Job detail (replaces dict[str, Any] in job_details) ──────────────
class JobDetail(AppBaseModel):
    """Structured job information used in ranking and feature extraction.
    结构化职位信息：用于精排排序与特征提取，替换 loose dict[str, Any]。"""

    title: str | None = None
    company: str | None = None
    description: str | None = None
    requirements: list[str] = Field(default_factory=list)
    salary: str | None = None
    location: str | None = None


class ParsedJobContent(AppBaseModel):
    """Structured output of a parsed job posting.
    结构化职位信息：提取标题、公司、描述及技能要求列表。"""

    title: str
    company: str
    description: str
    requirements: list[str] = Field(default_factory=list)
    salary: str | None = None
    location: str | None = None


class ParsedResumeContent(AppBaseModel):
    """Structured output of a parsed resume.
    结构化简历信息：提取姓名、邮箱、技能列表及工作经历。"""

    name: str | None = None
    email: str | None = None
    skills: list[str] = Field(default_factory=list)
    experience: list[ExperienceItem] = Field(default_factory=list)


# ── AiResultEvent.data union members ────────────────────────────────────
class JobParseData(AppBaseModel):
    """Payload for JOB_PARSE completion events.
    职位解析完成事件的 data 负载。"""

    parsed_content: ParsedJobContent = Field(alias="parsedContent")


class ResumeParseData(AppBaseModel):
    """Payload for RESUME_PARSE completion events.
    简历解析完成事件的 data 负载。"""

    parsed_content: ParsedResumeContent = Field(alias="parsedContent")
    summary: str = ""


class ResumeModification(AppBaseModel):
    """Resume rewrite metadata returned by conversation replies.
    对话回复中返回的简历修改元数据。"""

    modified: bool = False
    markdown: str = ""


class ConversationData(AppBaseModel):
    """Payload for CONVERSATION_REPLY completion events.
    对话回复完成事件的 data 负载。"""

    content: str
    file_url: str | None = Field(default=None, alias="fileUrl")
    request_id: str | None = Field(default=None, alias="requestId")
    locale: str | None = None
    resume_modification: ResumeModification = Field(
        default_factory=ResumeModification, alias="resumeModification"
    )


# ── Feedback ────────────────────────────────────────────────────────────
class FeedbackCommand(AppBaseModel):
    """Command consumed from the feedback MQ queue.
    反馈队列消费命令：替代 loose dict 操作。"""

    match_id: str = Field(alias="matchId")
    user_id: str = Field(alias="userId")
    resume_version_id: str = Field(alias="resumeVersionId")
    job_id: str = Field(alias="jobId")
    feedback_type: str = Field(alias="feedbackType")
    score: float | None = None
    context: str | None = None
    timestamp: str | None = None


# ── Vector upsert DTOs (align with Java backend) ──────────────────────
class JobVectorItem(AppBaseModel):
    """Single job vector entry for batch upsert to the backend.
    单个职位向量条目：对齐 Java BatchJobVectorUpsertRequest.JobVectorItem。"""

    job_id: str = Field(alias="jobId")
    embedding: list[float]
    title: str = ""
    description: str = ""
    requirements: list[str] = Field(default_factory=list)
    raw_content: str = Field(default="", alias="rawContent")
    source_file: str = Field(default="", alias="sourceFile")
    model_version: str = Field(default="", alias="modelVersion")


class ResumeVectorItem(AppBaseModel):
    """Single resume vector entry for batch upsert to the backend.
    单个简历向量条目：对齐 Java BatchResumeVectorUpsertRequest.ResumeVectorItem。"""

    resume_version_id: str = Field(alias="resumeVersionId")
    embedding: list[float]


class BatchVectorUpsertResponse(AppBaseModel):
    """Response from backend batch vector upsert endpoint.
    后端批量向量写入响应：对齐 Java Batch*VectorUpsertResponse。"""

    total: int = 0
    success: int = 0
    failed: int = 0
    skipped: int = 0
    failed_ids: list[str] = Field(default_factory=list, alias="failedIds")


# ── Baseline feature (replaces list[dict[str, Any]]) ────────────────────
class BaselineFeature(AppBaseModel):
    """Baseline feature record fetched from backend for model training.
    基线特征记录：从后端获取用于模型训练，替换 loose dict[str, Any]。"""

    job_id: str = Field(alias="jobId")
    title: str = ""
    description: str = ""
    requirements: list[str] = Field(default_factory=list)
    semantic_match: float = Field(default=0.0, alias="semanticMatch")


# ── MQ commands ───────────────────────────────────────────────────────
class JobParseCommand(AppBaseModel):
    """Command to trigger job-posting extraction from a URL with optional screenshot verification.
    触发职位解析的命令：从招聘页面 URL 提取结构化字段，支持截图交叉验证以提高准确率。"""

    job_id: str = Field(alias="jobId")
    url: str
    image_check_enabled: bool = Field(default=False, alias="imageCheckEnabled")
    screenshot_url: str | None = Field(default=None, alias="screenshotUrl")
    screenshot_base64: str | None = Field(default=None, alias="screenshotBase64")


class ResumeParseCommand(AppBaseModel):
    """Command to trigger resume file parsing into structured profile data.
    触发简历解析的命令：将 PDF/DOCX/TXT 文件提取为结构化简历数据。"""

    resume_id: str = Field(alias="resumeId")
    file_url: str = Field(alias="fileUrl")
    file_type: str = Field(alias="format")


class VectorGenCommand(AppBaseModel):
    """Command to generate an embedding vector for a text entity (job or resume).
    触发向量生成的命令：为职位或简历文本生成语义嵌入，用于后续相似度检索。"""

    reference_id: str = Field(alias="referenceId")
    entity_type: str = Field(alias="entityType")
    text: str


class ConversationMessage(AppBaseModel):
    """Single message within a conversation history, supporting optional file attachments.
    单条对话消息模型，支持附带文件 URL，用于构建多轮上下文。"""

    role: str
    content: str
    file_url: str | None = Field(default=None, alias="fileUrl")


class ConversationRequestCommand(AppBaseModel):
    """Command to orchestrate a conversational AI reply with resume and job context.
    对话请求命令：聚合用户消息、简历文本、职位上下文及附件，驱动 LLM 生成回复。"""

    conversation_id: str = Field(alias="conversationId")
    user_id: str = Field(alias="userId")
    message_history: list[ConversationMessage] = Field(
        default_factory=list, alias="messageHistory"
    )
    current_message: str = Field(alias="currentMessage")
    file_urls: list[str] = Field(default_factory=list, alias="fileUrls")
    resume_version_id: str | None = Field(default=None, alias="resumeVersionId")
    resume_text: str | None = Field(default=None, alias="resumeText")
    primary_job_text: str | None = Field(default=None, alias="primaryJobText")
    related_job_texts: list[str] | None = Field(default=None, alias="relatedJobTexts")
    init: bool | None = None
    locale: str | None = None
    request_id: str | None = Field(default=None, alias="requestId")


class JobRankCommand(AppBaseModel):
    """Command to rank recalled jobs against a resume and query using lexical + semantic signals.
    职位精排命令：基于简历、查询词及后端召回结果，执行 lexical + semantic 混合排序。"""

    match_id: str = Field(alias="matchId")
    user_id: str = Field(alias="userId")
    resume_version_id: str = Field(alias="resumeVersionId")
    resume_text: str = Field(default="", alias="resumeText")
    query: str | None = Field(default=None)
    recalled_job_ids: list[str] = Field(default_factory=list, alias="recalledJobIds")
    job_details: dict[str, JobDetail] = Field(default_factory=dict, alias="jobDetails")


class ScrapeResult(AppBaseModel):
    """Result of a job-page scraping operation.
    职位页面抓取结果：包含去噪后的文本及可选截图路径。"""

    markdown_text: str
    screenshot_url: str | None = None


class JobRankResultData(AppBaseModel):
    """Data payload embedded in AiResultEvent for JOB_RANK results.
    JOB_RANK 结果事件中的 data 负载，对齐后端 AiResultMessageListener 对 event.data() 的读取方式。"""

    rank_time_ms: int = Field(alias="rankTimeMs")
    ranked_results: list[dict[str, Any]] = Field(
        default_factory=list, alias="rankedResults"
    )


class AiResultEvent(AppBaseModel):
    """Standard event envelope for AI processing results consumed by the backend.
    AI 处理结果统一事件信封；必须保留 data 字段以匹配后端 AiResultEvent record。"""

    reference_id: str = Field(alias="referenceId")
    type: str
    status: str
    data: (
        dict[str, Any]
        | ParsedJobContent
        | ResumeParseData
        | ConversationData
        | JobRankResultData
        | None
    ) = None
    error_message: str | None = Field(default=None, alias="errorMessage")
    event_type: str | None = Field(default=None, alias="eventType")


class EmbeddingRequest(AppBaseModel):
    """Batch request for embedding generation.
    批量 embedding 请求：支持一次提交多条文本并指定模型版本。"""

    texts: list[str] = Field(default_factory=list)
    model: str | None = Field(default=None)

    @field_validator("texts")
    @classmethod
    def validate_texts(cls, v: list[str]) -> list[str]:
        """Enforce batch size and per-text length limits to prevent OOM and credit drain.
        强制 batch 大小和单条文本长度限制，防止 OOM 与 LLM 额度耗尽。"""
        from app.config import EMBEDDING_MAX_BATCH_SIZE, EMBEDDING_MAX_TEXT_LENGTH

        if len(v) > EMBEDDING_MAX_BATCH_SIZE:
            raise ValueError(
                f"Batch size exceeds maximum of {EMBEDDING_MAX_BATCH_SIZE}"
            )
        for i, text in enumerate(v):
            if len(text) > EMBEDDING_MAX_TEXT_LENGTH:
                raise ValueError(
                    f"Text at index {i} exceeds maximum length of {EMBEDDING_MAX_TEXT_LENGTH} characters"
                )
        return v


class EmbeddingResponse(AppBaseModel):
    """Batch response containing embedding vectors and model metadata.
    批量 embedding 响应：返回向量列表及实际使用的模型标识。"""

    model_config = ConfigDict(populate_by_name=True, protected_namespaces=())

    embeddings: list[list[float]] = Field(default_factory=list)
    model_used: str = Field(alias="modelUsed")
    count: int
    failed_indices: list[int] = Field(default_factory=list, alias="failedIndices")
    error_count: int = Field(default=0, alias="errorCount")


class SuitabilityRequest(AppBaseModel):
    """Request to evaluate resume-to-job suitability.
    人岗匹配度评估请求：输入简历与职位结构化数据，输出综合评分。"""

    resume: ParsedResumeContent
    job: ParsedJobContent
    semantic_match: float | None = Field(default=None, alias="semanticMatch")


class SuitabilityBreakdown(AppBaseModel):
    """Component-level scores explaining the final suitability result.
    匹配度拆解：从技能、经验等维度解释最终得分的构成。"""

    skill_score: float = Field(alias="skillScore")
    experience_score: float = Field(alias="experienceScore")
    overall_score: float = Field(alias="overallScore")


class SuitabilityResponse(AppBaseModel):
    """Final suitability evaluation result with multi-source scoring.
    人岗匹配度评估结果：融合 LLM 评分、基线规则评分及数据集模型评分，提供可解释摘要。"""

    suitable: bool
    summary: str
    breakdown: SuitabilityBreakdown
    vertex_score: float = Field(alias="vertexScore")
    dataset_score: float | None = Field(default=None, alias="datasetScore")
    final_score: float = Field(alias="finalScore")
    llm_model: str | None = Field(default=None, alias="llmModel")


class JobMatchRequest(AppBaseModel):
    """Request to search and rank jobs by semantic similarity.
    职位搜索请求：通过查询向量与后端向量库检索，返回语义最相似的职位列表。"""

    user_id: str = Field(alias="userId")
    query: str
    top_k: int = Field(default=10, alias="topK")
    filters: dict[str, str] = Field(default_factory=dict)


class MatchFactors(AppBaseModel):
    """Breakdown of the factors contributing to a job match score.
    匹配因子拆解：从技能、经验、地理位置等维度量化匹配程度。"""

    skill_match: float = Field(alias="skillMatch")
    experience_match: float = Field(alias="experienceMatch")
    location_match: float = Field(alias="locationMatch")


class VectorSearchResult(AppBaseModel):
    """Single raw result from backend vector-search endpoint.
    后端向量搜索原始结果条目：反序列化后映射为 MatchItem。"""

    job_id: str = Field(default="", alias="jobId")
    title: str = ""
    company: str = ""
    description: str = ""
    similarity: float = 0.0
    match_factors: MatchFactors = Field(
        default_factory=MatchFactors, alias="matchFactors"
    )


class MatchItem(AppBaseModel):
    """Single job match result returned to the client.
    单条职位匹配结果：包含基础信息、综合匹配得分及匹配因子。"""

    job_id: str = Field(alias="jobId")
    title: str
    company: str
    match_score: float = Field(alias="matchScore")
    match_factors: MatchFactors = Field(alias="matchFactors")
    description: str


class JobMatchResponse(AppBaseModel):
    """Response containing ranked job match results and timing telemetry.
    职位搜索响应：返回排序后的匹配列表及召回/排序耗时，用于前端性能监控。"""

    matches: list[MatchItem]
    total: int
    recall_time: int = Field(alias="recallTime")
    rank_time: int = Field(alias="rankTime")


class JobRankResultItem(AppBaseModel):
    """Single ranked job item with an optional LLM-generated match reason.
    单条精排职位结果：在基础匹配得分上附加 LLM 生成的匹配理由，提升可解释性。"""

    job_id: str = Field(alias="jobId")
    title: str
    company: str
    match_score: float = Field(alias="matchScore")
    match_factors: MatchFactors = Field(alias="matchFactors")
    description: str
    match_reason: str | None = Field(default=None, alias="matchReason")


class JobRankResultPayload(AppBaseModel):
    """Final payload for job ranking results delivered via MQ.
    职位精排最终结果负载：通过 MQ 回传后端，驱动申请追踪状态更新。"""

    match_id: str = Field(alias="matchId")
    status: str
    rank_time_ms: int = Field(alias="rankTimeMs")
    ranked_results: list[JobRankResultItem] = Field(
        default_factory=list, alias="rankedResults"
    )
    error_message: str | None = Field(default=None, alias="errorMessage")
