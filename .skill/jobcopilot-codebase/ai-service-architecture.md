# AI Service Architecture Details

## Two Runtime Modes

The AI service Docker image runs in two modes:

| Mode | Command | Container | Responsibilities |
|------|---------|-----------|-----------------|
| **API Server** | `uvicorn app.main:app` | `jobcopilot-ai` | REST API + MQ consumers (in thread) |
| **Worker** | `python -m app.worker_main` | `jobcopilot-ai-worker` | MQ consumers + scheduled retraining |

## Service Layer Details

### `app/services/llm_client.py`
- Central LLM abstraction using LiteLLM
- Handles all model calls: text generation, vision, embeddings
- Configurable timeout, temperature, max tokens
- Provider-agnostic: switch models via `LLM_TEXT_MODEL` env var

### `app/services/resume_parser.py`
- Parses resume content via LLM into structured JSON
- Handles PDF/DOCX text extraction first, then LLM structuring

### `app/services/job_parser.py`
- Parses job descriptions into structured fields (title, company, skills, requirements, etc.)

### `app/services/job_matching_service.py`
- `find_job_matches()` — matches resumes against jobs using embedding similarity
- REST endpoint: `POST /api/v1/match`

### `app/services/suitability_service.py`
- `evaluate_suitability_with_vertex()` — detailed suitability scoring
- REST endpoint: `POST /api/v1/suitability`

### `app/services/vector_service.py`
- `generate_embedding()` — single text → embedding vector
- REST endpoint: `POST /api/v1/ai/embeddings` (batch)

### `app/services/job_rank_service.py`
- Ranks jobs using ML model (LightGBM when available, fallback to heuristic)
- Consumed via MQ: `ai.queue.job.rank`

### `app/services/backend_client.py`
- HTTP client for calling backend internal APIs
- Vector upsert, baseline feature retrieval
- Configurable batch size and timeouts

### `app/services/conversation_service.py`
- LLM-powered chat for resume advice and job Q&A
- Consumed via MQ: `ai.queue.conversation`

### `app/services/file_parser.py`
- Extracts text from PDF, DOCX files using pypdf, python-docx
- Reads from shared volume (`FILE_STORAGE_PATH`)

### `app/services/web_scraper.py`
- Playwright-based web scraping for job listings

### `app/services/job_orchestrator.py`
- Orchestrates multi-step job processing pipeline

### `app/services/resume_orchestrator.py`
- Orchestrates multi-step resume processing pipeline

## MQ Consumer Architecture

### Queue Definitions (in `config.py`)

```
ai.queue.job.parse        ← Job parsing requests from backend
ai.queue.resume.parse     ← Resume parsing requests from backend
ai.queue.conversation     ← Chat requests from backend
ai.queue.job.rank         ← Job ranking requests from backend
ai.queue.feedback         ← User feedback events for training
ai.queue.model.incremental ← Model retraining triggers
```

### Consumer Files

| Consumer | File | Handles |
|----------|------|---------|
| Job Parse | `worker/consumers/parse_consumer.py` | Parses job descriptions via LLM |
| Resume Parse | `worker/consumers/parse_consumer.py` | Parses resumes via LLM |
| Rank | `worker/consumers/rank_consumer.py` | Job ranking with ML model |

## Model Training Pipeline

```
User feedback → Redis buffer (ai:feedback:*)
  → Scheduler checks sample count (≥ MIN_SAMPLES_FOR_RETRAIN)
  → Fetches baseline features from backend (via backend_client)
  → LightGBM training
  → Upload model artifact to MinIO (bucket: MINIO_MODEL_BUCKET)
  → Publish reload event to Redis Pub/Sub
  → AI Service model_manager watches Pub/Sub, loads new model
```

### Model Manager (`api/model_manager.py`)
- Loads LightGBM model from MinIO on startup
- Watches Redis Pub/Sub for reload events
- Exposes `/admin/recompute-model` (deprecated, use ai-worker)

## Environment Configuration

All config is in `config.py`, loaded from env vars. Key env vars:

| Variable | Default | Purpose |
|----------|---------|---------|
| `LLM_TEXT_MODEL` | `gemini/gemini-2.5-flash` | Text generation model |
| `LLM_EMBEDDING_MODEL` | `gemini/gemini-embedding-001` | Embedding model |
| `LLM_EMBEDDING_MODEL_DIMENSION` | `1536` | Must match pgvector dimension |
| `BACKEND_SERVICE_URL` | `http://backend:8080` | Backend internal API |
| `INTERNAL_API_KEY` | (required in prod) | Shared secret with backend |
