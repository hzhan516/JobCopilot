import os
from pathlib import Path


RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USERNAME = os.getenv("RABBITMQ_USERNAME", "guest")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "guest")

AI_DIRECT_EXCHANGE = "ai.direct.exchange"
AI_DLX_EXCHANGE = "ai.dlx.exchange"
AI_DLQ_QUEUE = "ai.dlq.queue"
AI_DLQ_ROUTING_KEY = "dlq.routing.key"

JOB_PARSE_REQUEST_QUEUE = "ai.queue.job.parse"
JOB_PARSE_RESULT_QUEUE = "backend.queue.job.parse"
JOB_PARSE_REQUEST_ROUTING_KEY = "ai.req.job.parse"
JOB_PARSE_RESULT_ROUTING_KEY = "backend.res.job.parse"

RESUME_PARSE_REQUEST_QUEUE = "ai.queue.resume.parse"
RESUME_PARSE_RESULT_QUEUE = "backend.queue.resume.parse"
RESUME_PARSE_REQUEST_ROUTING_KEY = "ai.req.resume.parse"
RESUME_PARSE_RESULT_ROUTING_KEY = "backend.res.resume.parse"

VECTOR_GEN_REQUEST_QUEUE = "ai.queue.vector.gen"
VECTOR_GEN_RESULT_QUEUE = "backend.queue.vector.gen"
VECTOR_GEN_REQUEST_ROUTING_KEY = "ai.req.vector.gen"
VECTOR_GEN_RESULT_ROUTING_KEY = "backend.res.vector.gen"

CONVERSATION_REQUEST_QUEUE = "ai.queue.conversation"
CONVERSATION_RESULT_QUEUE = "backend.queue.conversation"
CONVERSATION_REQUEST_ROUTING_KEY = "ai.req.conversation"
CONVERSATION_RESULT_ROUTING_KEY = "backend.res.conversation"

JOB_RANK_REQUEST_QUEUE = "ai.queue.job.rank"
JOB_RANK_RESULT_QUEUE = "backend.queue.job.rank"
JOB_RANK_REQUEST_ROUTING_KEY = "ai.req.job.rank"
JOB_RANK_RESULT_ROUTING_KEY = "backend.res.job.rank"

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

VERTEX_PROJECT_ID = os.getenv("VERTEX_PROJECT_ID", "ser594-ai-service")
VERTEX_LOCATION = os.getenv("VERTEX_LOCATION", "global")
VERTEX_CREDENTIALS = os.getenv("VERTEX_CREDENTIALS")

# Export standard variables required by LiteLLM for Vertex AI routing
os.environ["VERTEXAI_PROJECT"] = VERTEX_PROJECT_ID
os.environ["VERTEX_PROJECT"] = VERTEX_PROJECT_ID
os.environ["VERTEX_LOCATION"] = VERTEX_LOCATION

if VERTEX_CREDENTIALS:
    creds_path = Path(VERTEX_CREDENTIALS)

    # If the path is relative and doesn't exist from CWD, resolve against the
    # project root (the directory containing the ai-service/ folder).
    if not creds_path.is_file() and not creds_path.is_absolute():
        project_root = Path(__file__).resolve().parent.parent.parent
        alt_path = project_root / creds_path
        if alt_path.is_file():
            creds_path = alt_path

    if creds_path.is_file():
        # Read the service account JSON key from the file path
        creds_content = creds_path.read_text()
        os.environ["VERTEX_CREDENTIALS"] = creds_content
        os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = str(creds_path)
    else:
        # Backward compatibility: value may be a raw JSON string
        os.environ["VERTEX_CREDENTIALS"] = VERTEX_CREDENTIALS

LLM_TEXT_MODEL = os.getenv("LLM_TEXT_MODEL", "gemini/gemini-2.5-flash")
LLM_VISION_MODEL = os.getenv("LLM_VISION_MODEL", "gemini/gemini-2.5-flash")
LLM_EMBEDDING_MODEL = os.getenv("LLM_EMBEDDING_MODEL", "gemini/gemini-embedding-001")
LLM_EMBEDDING_MODEL_DIMENSION = int(
    os.getenv("LLM_EMBEDDING_MODEL_DIMENSION")
    or os.getenv("EMBEDDING_OUTPUT_DIMENSION")
    or "1536"
)

LLM_TEMPERATURE = float(os.getenv("LLM_TEMPERATURE", "0.1"))
