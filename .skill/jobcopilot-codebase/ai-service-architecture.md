# AI Service Architecture Details

## Two Runtime Modes

The AI service Docker image runs in two modes:

| Mode | Command | Container | Responsibilities |
|------|---------|-----------|-----------------|
| **API Server** | `uvicorn app.main:app` | `jobcopilot-ai` | REST API + MQ consumers (in thread) |
| **Worker** | `python -m app.worker_main` | `jobcopilot-ai-worker` | MQ feedback consumer + scheduled retraining |

## Service Layer Details

### `app/services/llm_client.py`
- Central LLM abstraction using LiteLLM (`litellm.completion` / `litellm.embedding`)
- Text generation via `_generate_text()` with a bounded semaphore (`MAX_CONCURRENT_LLM_REQUESTS=5`)
- JSON extraction with markdown-fence stripping, brace balancing, and control-character repair
- `generate_json_from_text_prompt()` — strict text → JSON
- `generate_json_from_text_prompt_with_repair()` — one model-assisted repair attempt before raising `LlmJsonParseError`
- `generate_json_from_image_prompt()` — vision model calls for screenshots
- Retry strategy: 3 attempts with exponential backoff for rate limits, connection errors, and timeouts

### `app/services/resume_parser.py`
- `parse_resume_text(resume_text)` — parses resume content via LLM into `ParsedResumeContent`
- Normalizes `skills` and `experience` to tolerate list/string/dict model outputs
- Truncates input to 12000 characters

### `app/services/job_parser.py`
- `parse_job_text(markdown_text)` — parses job descriptions into `ParsedJobContent`
- `parse_job_from_image(screenshot_url, context_text)` — vision-model fallback for screenshots
- `validate_job_with_vision(parsed_content, page_text, screenshot_url)` — cross-checks parsed fields against a screenshot
- `_load_image_bytes(image_url)` — loads images from data URIs or HTTP(S) only; blocks local file paths

### `app/services/job_matching_service.py`
- `find_job_matches(request)` — embeds the user query and delegates vector search to the backend
- REST endpoint: `POST /api/v1/match`
- Calls backend `POST /api/v1/jobs/vector-search` with `queryEmbedding`, `queryText`, `limit`, and `filters`

### `app/services/suitability_service.py`
- `evaluate_suitability_with_vertex(request)` — detailed suitability scoring
- REST endpoint: `POST /api/v1/suitability`
- Computes a heuristic baseline first, then calls the LLM; falls back to baseline on any failure
- Returns `vertexScore`, `finalScore`, `breakdown`, and `llmModel`

### `app/services/vector_service.py`
- `generate_embedding(text)` — single text → embedding vector
- REST endpoint: `POST /api/v1/ai/embeddings` (batch)
- Validates output dimension against `LLM_EMBEDDING_MODEL_DIMENSION`
- Only passes `dimensions=` for models that advertise dimension support

### `app/services/job_rank_service.py`
- `rank_jobs(command)` — ranks recalled jobs using the loaded LightGBM model or a heuristic fallback
- Consumed via MQ: `ai.queue.job.rank`
- Extracts features via `app/domain/ml/features.py` (`FEATURE_COLUMNS`)
- Generates LLM match reasons for the top 3 results via `_generate_match_reason()`

### `app/services/conversation_service.py`
- `process_conversation(command)` — LLM-powered chat for resume advice and job Q&A
- Consumed via MQ: `ai.queue.conversation`
- Downloads up to 3 attachments via `file_parser.download_file_bytes()` and `extract_resume_text()`
- Uses `generate_json_from_text_prompt_with_repair()` with a JSON contract fallback
- Returns `ConversationData` with `content`, `fileUrl`, `requestId`, `locale`, and `resumeModification`

### `app/services/file_parser.py`
- `download_file_bytes(file_url)` — resolves HTTP(S), `/api/storage/download?key=...`, or local object keys
- `extract_resume_text(file_bytes, content_format)` — extracts text from PDF, DOCX, TXT, and Markdown
- Blocks internal/private IP HTTP requests and path-traversal local lookups

### `app/services/web_scraper.py`
- `scrape_job_page(url, capture_screenshot)` — Playwright-based HTTP fetch + optional full-page screenshot
- Returns `ScrapeResult` with `markdown_text` and `screenshot_url`

### `app/services/job_orchestrator.py`
- `process_job(command)` — orchestrates multi-step job processing
- Scrapes the page, parses text, and falls back to screenshot vision parsing if text is missing or incomplete
- Uses `MIN_SCRAPED_TEXT_LENGTH=200` to decide when to trust scraped content

### `app/services/resume_orchestrator.py`
- `process_resume(command)` — orchestrates resume file download, text extraction, and structured parsing
- Calls `file_parser.download_file_bytes()` and `file_parser.extract_resume_text()` before `resume_parser.parse_resume_text()`

## MQ Consumer Architecture

### Queue Definitions (in `app/config.py`)

| Queue | Purpose |
|-------|---------|
| `ai.queue.job.parse` | Job parsing requests from backend |
| `ai.queue.resume.parse` | Resume parsing requests from backend |
| `ai.queue.conversation` | Chat requests from backend |
| `ai.queue.job.rank` | Job ranking requests from backend |
| `ai.queue.feedback` | User feedback events for training (worker only) |

Result queues (`backend.queue.job.parse`, `backend.queue.resume.parse`, `backend.queue.conversation`, `backend.queue.job.rank`) and a shared dead-letter queue (`ai.dlq.queue`) are also declared.

### Consumer Files

| Consumer | File | Handler | Produces |
|----------|------|---------|----------|
| Job Parse | `app/mq/consumer.py` | `handle_job_message()` → `process_job()` | `backend.queue.job.parse` |
| Resume Parse | `app/mq/consumer.py` | `handle_resume_message()` → `process_resume()` | `backend.queue.resume.parse` |
| Conversation | `app/mq/consumer.py` | `handle_conversation_message()` → `process_conversation()` | `backend.queue.conversation` |
| Rank | `app/mq/consumer.py` | `handle_job_rank_message()` → `rank_jobs()` | `backend.queue.job.rank` |
| Feedback | `app/worker/consumers/feedback.py` | `handle_feedback_message()` | Buffers to Redis `ai:feedback:buffer` |

The API server starts all workflow consumers in a daemon thread (`initialize_mq()` in `app/main.py`). The worker only consumes `ai.queue.feedback` via `app/worker/consumers/rabbitmq_setup.py`.

### Error Classification

`app/mq/consumer.py` classifies failures into:
- `RATE_LIMITED`
- `UPSTREAM_TIMEOUT`
- `UPSTREAM_UNAVAILABLE`
- `INVALID_MODEL_RESPONSE`
- `UNKNOWN`

Failed messages are NACKed with `requeue=False` and routed to the DLX/`ai.dlq.queue`.

## Model Training Pipeline

```
User feedback → Redis buffer (ai:feedback:buffer)
  → Worker scheduler runs `IncrementalTrainer.try_retrain()` daily at 02:00 UTC
  → Acquires distributed lock `ai:model:retrain:lock`
  → Drains feedback buffer; checks sample count (≥ MIN_SAMPLES_FOR_RETRAIN)
  → Fetches baseline features from backend via `InternalApiClient.get_baseline_features_async()`
  → Trains LightGBM binary classifier (app/domain/ml/features.py)
  → Uploads model artifact to MinIO (bucket: MINIO_MODEL_BUCKET)
  → Updates `latest_meta.json` in MinIO
  → Publishes reload event to Redis Pub/Sub channel `ai.model.reload`
  → AI Service `ModelManager` watches Pub/Sub and loads the new model
```

### Model Manager (`app/api/model_manager.py`)
- `load_latest()` — downloads the latest model from MinIO on startup
- `predict(feature_matrix)` — runs LightGBM inference with an asyncio lock
- `watch_for_reloads()` — subscribes to `ai.model.reload` and reloads the model
- Singleton `model_manager` imported by `app/main.py` and `app/services/job_rank_service.py`

## Infrastructure Clients

### `app/infrastructure/api_client/client.py`
- `InternalApiClient` — async HTTP client for backend internal APIs
- `get_baseline_features_async()` fetches `GET /api/internal/ai/baseline-features` for retraining

### `app/infrastructure/minio/client.py`
- `MinioModelRegistry` — S3-compatible client using `boto3`
- Uploads models, updates `latest_meta.json`, and downloads artifacts

### `app/infrastructure/redis/client.py`
- `get_redis_client()` — async Redis client
- `RedisBuffer` — feedback buffer (`ai:feedback:buffer`), retrain lock (`ai:model:retrain:lock`), and reload Pub/Sub

## Environment Configuration

All config is in `app/config.py`, loaded from env vars. Key env vars:

| Variable | Default | Purpose |
|----------|---------|---------|
| `LLM_TEXT_MODEL` | `gemini/gemini-2.5-flash` | Text generation model |
| `LLM_VISION_MODEL` | `gemini/gemini-2.5-flash` | Vision model for screenshot parsing |
| `LLM_EMBEDDING_MODEL` | `gemini/gemini-embedding-001` | Embedding model |
| `LLM_EMBEDDING_MODEL_DIMENSION` | `1536` | Must match pgvector dimension |
| `LLM_TEMPERATURE` | `0.1` | LLM sampling temperature |
| `LLM_REQUEST_TIMEOUT_SECONDS` | `60` | LLM call timeout |
| `LLM_MAX_TOKENS` | `8192` | Max tokens per generation |
| `BACKEND_SERVICE_URL` | `http://backend:8080` | Backend internal API |
| `BACKEND_QUERY_TIMEOUT` | `5` | Backend read timeout |
| `BACKEND_BATCH_UPSERT_TIMEOUT` | `30` | Backend batch write timeout |
| `INTERNAL_API_KEY` | (required in prod) | Shared secret with backend |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `REDIS_HOST` | `redis` | Redis host |
| `MINIO_ENDPOINT` | — | MinIO endpoint |
| `MINIO_MODEL_BUCKET` | `ai-models` | Model artifact bucket |
| `RETRAIN_INTERVAL_HOURS` | `24` | Scheduled retrain interval |
| `MIN_SAMPLES_FOR_RETRAIN` | `10` | Minimum feedback samples before retraining |
| `EMBEDDING_MAX_BATCH_SIZE` | `32` | Max embedding batch size |
| `EMBEDDING_MAX_TEXT_LENGTH` | `100000` | Max text length per embedding input |
| `FILE_STORAGE_PATH` | `/app/uploads` | Shared upload volume path |
