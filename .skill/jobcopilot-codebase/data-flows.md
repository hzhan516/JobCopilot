# Key Data Flows

## 1. Resume Upload & Parse (Full Flow)

```
1. User uploads PDF/DOCX via frontend
2. Frontend POST /api/v1/resumes → Backend ResumeController
3. ResumeApplicationService:
   a. Saves file to local/MinIO storage (FileStorageService)
   b. Saves resume metadata to PostgreSQL (resumes table)
   c. Creates OutboxMessage with parse request → outbox_messages table
   d. All within single @Transactional
4. OutboxRelayScheduler (polling every 5s):
   a. Reads PENDING outbox_messages
   b. Publishes to RabbitMQ exchange ai.direct.exchange
   c. Routing key: ai.req.resume.parse → queue: ai.queue.resume.parse
   d. Marks message as SENT
5. AI Service MQ consumer (parse_consumer.py):
   a. Reads message from ai.queue.resume.parse
   b. Extracts file path, downloads from shared volume
   c. Calls file_parser.py → extract text from PDF/DOCX
   d. Calls resume_parser.py → LLM structures the text
   e. Calls vector_service.py → generate embedding for resume text
   f. Calls backend_client.py → POST to backend internal API
6. Backend receives parsed data:
   a. Saves structured resume data (skills, experience, education, etc.)
   b. Upserts embedding to pgvector (resume_embeddings table)
   c. Updates resume status to PARSED
7. Frontend polls GET /api/v1/resumes/:id/status until PARSED
```

## 2. Job Matching Flow

```
1. User views job matches from frontend (Dashboard or Jobs page)
2. Frontend GET /api/v1/jobs/matches?resumeId=X
3. Backend JobApplicationService.matchJobs():
   a. Retrieves resume embedding from pgvector
   b. Performs cosine similarity search against job_embeddings
   c. Returns top-N candidate jobs
4. For detailed scoring (on demand):
   a. Backend publishes to ai.queue.job.rank
   b. AI worker rank_consumer.py:
      - Loads LightGBM model (from MinIO via model_manager)
      - Fetches baseline features from backend
      - Scores each (resume, job) pair
   c. Results published to backend.queue.job.rank
   d. Backend updates match_scores table
5. Frontend displays ranked jobs with match scores
```

## 3. Conversation / Chat Flow

```
1. User sends message in Chat page
2. Frontend POST /api/v1/conversations/:id/messages
3. Backend ConversationApplicationService:
   a. Saves user message to messages table
   b. Creates OutboxMessage → ai.queue.conversation
4. AI Service conversation_service.py:
   a. Retrieves conversation history
   b. Calls LLM with context (resume data, job data, chat history)
   c. Returns AI response to backend.queue.conversation
5. Backend AiResultMessageListener:
   a. Saves AI response to messages table
   b. Sends WebSocket notification to frontend (if connected)
6. Frontend renders AI response in chat UI
```

## 4. Feedback & Incremental Training Flow

```
1. User interacts with job matches (view, apply, dismiss)
2. Frontend sends scoring events → Backend
3. Backend publishes to ai.queue.feedback
4. AI Worker feedback consumer:
   a. Buffers labeled samples in Redis (ai:feedback:samples)
   b. Counts samples for each (resume_id, job_id) pair
5. AI Worker scheduler (periodic, configurable RETRAIN_INTERVAL_HOURS):
   a. Checks sample count ≥ MIN_SAMPLES_FOR_RETRAIN
   b. Acquires distributed lock via Redis (prevents duplicate training)
   c. Fetches baseline features from backend (POST internal API)
   d. Trains LightGBM ranking model
   e. Uploads model artifact to MinIO
   f. Publishes model_reload event to Redis Pub/Sub
6. AI Service model_manager:
   a. Subscribes to Redis Pub/Sub channel
   b. On reload event, downloads new model from MinIO
   c. Hot-swaps model in memory
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
