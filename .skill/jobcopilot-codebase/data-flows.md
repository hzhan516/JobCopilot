# Key Data Flows

## 1. Resume Upload & Parse (Full Flow)

```
1. User uploads PDF/DOCX via frontend
2. Frontend POST /v1/resumes (multipart/form-data) → ResumeController
3. ResumeController → ResumeFacadeImpl → ResumeApplicationService.handleUpload
   → ResumeUploadHandler.upload:
   a. Sanitizes filename and uploads the file via FileStorageService
      (MinIO/local/S3/OSS, selected by STORAGE_TYPE)
   b. Creates the resume group and original version (resumes/resume_groups/resume_versions tables)
   c. Converts the file to markdown via ResumeConverterService and saves a CONVERTED version
   d. Triggers vector generation for the converted markdown via VectorGenerationService
      → VectorGenerationPort → VectorFacadeImpl → VectorApplicationService
      → VectorEmbeddingRestAdapter POST /api/v1/ai/embeddings
      → stores the embedding in resume_vectors (pgvector)
   e. Marks the ORIGINAL version as PARSING
   f. Generates a presigned URL and builds ResumeParseCommand
   g. AiMessagePublisherPort.sendResumeForParsing writes a PENDING OutboxMessage
      to outbox_messages (exchange ai.direct.exchange, routing key ai.req.resume.parse)
4. OutboxRelayScheduler polls every 2 seconds:
   a. Reads PENDING rows from outbox_messages
   b. OutboxRelayTransactionService publishes each message to ai.direct.exchange
   c. Routing key ai.req.resume.parse → queue ai.queue.resume.parse
   d. Marks the row as SENT (or FAILED)
5. AI Service app/mq/consumer.py handle_resume_message:
   a. Reads from ai.queue.resume.parse
   b. process_resume downloads the file bytes from the presigned URL
   c. extract_resume_text (app/services/file_parser.py) extracts raw text from PDF/DOCX
   d. parse_resume_text (app/services/resume_parser.py) calls the LLM to structure the text
   e. Publishes an AiResultEvent to ai.direct.exchange
      routing key backend.res.resume.parse → queue backend.queue.resume.parse
6. Backend AiResultMessageListener.onResumeParseResult:
   a. ResumeFacadeImpl.handleParseResult → ResumeApplicationService.handleParseResult
      → ResumeParseResultHandler.handle
   b. On COMPLETED: extracts parsedContent JSON, marks the original version PARSED,
      propagates the parsed content to derived versions, and triggers vector generation
      for the parsed JSON
   c. On FAILED: marks the version PARSE_FAILED with the error message
7. Frontend polls GET /v1/resumes/groups to observe parseStatus on each version
```

All request queues are configured with `x-dead-letter-exchange=ai.dlx.exchange`, so
repeatedly failing messages land in `ai.dlq.queue` for inspection instead of looping.

## 2. Job Matching Flow

```
1. User requests matches from the frontend
2. Frontend POST /v1/jobs/match {resumeVersionId, query?, topK?}
   → JobController → MatchingFacadeImpl → MatchingApplicationService.startJobMatch
3. MatchingApplicationService:
   a. Selects the active recall model version from matching_models
   b. If the resume vector is missing, regenerates it synchronously via
      VectorGenerationPort → POST /api/v1/ai/embeddings
   c. MatchTransactionService.execute:
      - Creates a JobMatchResult in PROCESSING status
      - Loads the resume embedding from resume_vectors
      - Calls VectorSearchPort.findSimilarJobs for pgvector Euclidean-distance
        recall (`embedding <=> query`) against job_vectors (top-K, default 10)
      - Persists recall results and timing
      - Builds JobRankCommand and writes it to the outbox for
        routing key ai.req.job.rank → queue ai.queue.job.rank
4. AI Service app/mq/consumer.py handle_job_rank_message:
   a. Reads from ai.queue.job.rank
   b. rank_jobs (app/services/job_rank_service.py):
      - Extracts FEATURE_COLUMNS-based features for each recalled job
      - Scores jobs via ModelManager.predict (LightGBM) or a heuristic fallback
      - Generates LLM match reasons for the top 3 results
   c. Publishes the ranked list to backend.queue.job.rank
      (routing key backend.res.job.rank)
5. Backend AiResultMessageListener.onJobRankResult:
   a. MatchingFacadeImpl.saveJobRankResult → MatchingApplicationService.saveMatchResult
   b. Transitions the JobMatchResult from PROCESSING to COMPLETED
6. Frontend polls GET /v1/jobs/match/{matchId}; the completed response contains
   rankedResults with matchScore, matchFactors, and matchReason, plus recallTimeMs
   and rankTimeMs
```

Jobs are assumed to be already parsed and vectorized before matching. They are
submitted via `POST /v1/jobs` and processed through the separate `JOB_PARSE` flow
(`ai.queue.job.parse` → `backend.queue.job.parse`), which also generates job vectors.

## 3. Conversation / Chat Flow

```
1. User sends a message in the Chat page
2. Frontend POST /v1/conversations/{conversationId}/messages
   → ConversationController → ConversationFacadeImpl
   → ConversationApplicationService.sendMessage
   → ConversationMessageService.sendMessage
3. ConversationMessageService:
   a. Validates ownership, saves the USER message, auto-generates a title
   b. Determines whether this is the first turn (no ASSISTANT message yet)
   c. ConversationContextService.queueConversationRequest:
      - Loads resume text (AI-optimized version if one exists, else the linked resume version)
      - Loads primary job text and up to 5 related completed jobs
      - Builds message history and attachment list
      - Creates ConversationRequestCommand with locale and requestId
      - AiMessagePublisherPort.sendConversationRequest writes it to the outbox
        for routing key ai.req.conversation → queue ai.queue.conversation
4. OutboxRelayScheduler delivers the command to ai.queue.conversation
5. AI Service app/mq/consumer.py handle_conversation_message:
   a. Reads from ai.queue.conversation
   b. process_conversation (app/services/conversation_service.py):
      - Downloads and extracts text from up to 3 attachments
      - Builds a JSON-structured prompt grounded in resume, job, attachments,
        history, and the current message
      - Calls the LLM via generate_json_from_text_prompt_with_repair
      - Normalizes content, fileUrl, and resumeModification
   c. Publishes the reply to backend.queue.conversation
      (routing key backend.res.conversation)
6. Backend AiResultMessageListener.onConversationReply:
   a. On failure: resolves a localized error message, saves an ASSISTANT error
      message, and fails the stream
   b. On success: extracts content, fileUrl, and aiOptimizedMarkdown;
      ConversationFacadeImpl.saveAiReply persists the ASSISTANT message;
      if a resume optimization was produced, an AI-optimized version is saved
      and its vector is regenerated
   c. ConversationFacadeImpl.completeAiReply → ConversationStreamPort.completeReply
7. ConversationStreamService bridges the asynchronous MQ reply with the HTTP stream:
   a. Uses an in-memory CompletableFuture plus Redis Pub/Sub channel ra:conv:reply
      for cross-instance coordination
   b. Unblocks GET /v1/conversations/{conversationId}/stream and writes the reply
```

File attachments are uploaded separately via `POST /v1/conversations/{id}/files`.

## 4. Feedback & Incremental Training Flow

```
1. User interacts with a job (CLICK, APPLY, REJECT) from the frontend
2. Frontend POST /v1/jobs/{jobId}/track?action=CLICK|APPLY|REJECT&resumeVersionId={resumeVersionId}
   → JobController → JobFacadeImpl → JobApplicationService.trackUserAction
3. JobApplicationService.trackUserAction:
   a. Validates job ownership
   b. Builds UserFeedbackCommand
   c. AiMessagePublisherPort.sendUserFeedback writes it to the outbox
      for routing key ai.req.feedback → queue ai.queue.feedback
4. OutboxRelayScheduler delivers feedback messages to ai.queue.feedback
5. AI Worker app/worker_main.py feedback consumer:
   a. handle_feedback_message (app/worker/consumers/feedback.py) validates the
      FeedbackCommand and assigns label=1 for APPLY/CLICK/EXPLICIT_THUMBS_UP,
      otherwise label=0
   b. Appends the sample to Redis list ai:feedback:buffer via RedisBuffer
6. IncrementalTrainer (app/worker/scheduler/trainer.py), scheduled daily at 02:00 UTC
   and also run once at worker startup:
   a. Acquires distributed lock ai:model:retrain:lock via RedisBuffer
   b. Drains ai:feedback:buffer
   c. Skips retraining if the count is below MIN_SAMPLES_FOR_RETRAIN (default 10)
   d. Fetches baseline features from backend GET /api/internal/ai/baseline-features
      (InternalApiClient)
   e. Trains a LightGBM binary classifier on baseline (label=1) + feedback samples
   f. Uploads the model artifact to MinIO bucket ai-models (MinioModelRegistry)
   g. Updates latest metadata and publishes ai.model.reload over Redis Pub/Sub
7. AI Service ModelManager (app/api/model_manager.py):
   a. Subscribes to ai.model.reload
   b. On reload notification, downloads the new artifact from MinIO
   c. Hot-swaps the LightGBM model in memory
   d. The model is used by rank_jobs for job ranking
```

## 5. Authentication Flow

```
1. User logs in via email/password or Google OAuth
2. Backend AuthController:
   a. Validates credentials
   b. Issues JWT access token (short-lived) + refresh token (HttpOnly cookie)
   c. Returns { accessToken, expiresIn, userId, email }
3. Frontend api.ts interceptor:
   a. Stores access token in memory (tokenStorage)
   b. Attaches Authorization: Bearer header to all requests
4. On 401 response:
   a. Interceptor calls /api/v1/auth/refresh (with HttpOnly cookie)
   b. Gets new access token
   c. Retries original request
   d. Queues concurrent requests during refresh
5. On refresh failure → redirect to /login
```

## 6. CAPTCHA Flow

```
1. Frontend requests CAPTCHA challenge: GET /api/v1/auth/captcha
2. Backend generates slider puzzle:
   a. Creates random x-position
   b. Generates CAPTCHA token → stores in Redis (5 min TTL)
   c. Returns background image, slider image, track width
3. User slides to correct position
4. Frontend POST /api/v1/auth/captcha/verify { x, captchaToken }
5. Backend validates:
   a. Checks Redis for token existence
   b. Compares x position within tolerance (±CAPTCHA_TOLERANCE px)
   c. Rate limited: max CAPTCHA_MAX_ATTEMPTS attempts
   d. Returns captchaToken for use in login/register
```
