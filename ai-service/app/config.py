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

CONVERSATION_REQUEST_QUEUE = "ai.queue.conversation"
CONVERSATION_RESULT_QUEUE = "backend.queue.conversation"
CONVERSATION_REQUEST_ROUTING_KEY = "ai.req.conversation"
CONVERSATION_RESULT_ROUTING_KEY = "backend.res.conversation"

JOB_RANK_REQUEST_QUEUE = "ai.queue.job.rank"
JOB_RANK_RESULT_QUEUE = "backend.queue.job.rank"
JOB_RANK_REQUEST_ROUTING_KEY = "ai.req.job.rank"
JOB_RANK_RESULT_ROUTING_KEY = "backend.res.job.rank"

MODEL_INCREMENTAL_QUEUE = "ai.queue.model.incremental"
MODEL_INCREMENTAL_ROUTING_KEY = "ai.req.model.incremental"

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

VERTEX_PROJECT_ID = os.getenv("VERTEX_PROJECT_ID", "ser594-ai-service")
VERTEX_LOCATION = os.getenv("VERTEX_LOCATION", "global")
VERTEX_CREDENTIALS = os.getenv("VERTEX_CREDENTIALS")

# LiteLLM requires these exact environment variables to route requests to Vertex AI.
# LiteLLM 依赖这些特定的环境变量完成 Vertex AI 请求路由。
os.environ["VERTEXAI_PROJECT"] = VERTEX_PROJECT_ID
os.environ["VERTEX_PROJECT"] = VERTEX_PROJECT_ID
os.environ["VERTEX_LOCATION"] = VERTEX_LOCATION

if VERTEX_CREDENTIALS:
    creds_path = Path(VERTEX_CREDENTIALS)

    # When running inside Docker, relative paths may not resolve from CWD.
    # Fall back to the project root (parent of ai-service/) to keep credentials portable.
    # Docker 内运行时相对路径可能无法从当前工作目录解析，因此以项目根目录为基准进行二次查找，保证凭据路径可移植。
    if not creds_path.is_file() and not creds_path.is_absolute():
        project_root = Path(__file__).resolve().parent.parent.parent
        alt_path = project_root / creds_path
        if alt_path.is_file():
            creds_path = alt_path

    if creds_path.is_file():
        creds_content = creds_path.read_text()
        os.environ["VERTEX_CREDENTIALS"] = creds_content
        os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = str(creds_path)
    else:
        # Support raw JSON string for environments without a mounted file.
        # 无文件挂载的环境支持直接传入 JSON 字符串作为凭据。
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
LLM_REQUEST_TIMEOUT_SECONDS = float(os.getenv("LLM_REQUEST_TIMEOUT_SECONDS", "60"))
LLM_MAX_TOKENS = int(os.getenv("LLM_MAX_TOKENS", "8192"))

BACKEND_SERVICE_URL = os.getenv("BACKEND_SERVICE_URL", "http://backend:8080")
BACKEND_QUERY_TIMEOUT = float(os.getenv("BACKEND_QUERY_TIMEOUT", "5"))
BACKEND_BATCH_UPSERT_TIMEOUT = float(os.getenv("BACKEND_BATCH_UPSERT_TIMEOUT", "30"))
BACKEND_BATCH_SIZE = int(os.getenv("BACKEND_BATCH_SIZE", "100"))
