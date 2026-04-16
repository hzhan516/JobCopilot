import os


RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USERNAME = os.getenv("RABBITMQ_USERNAME", "guest")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "guest")

AI_DIRECT_EXCHANGE = "ai.direct.exchange"

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

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

GOOGLE_CLOUD_PROJECT = os.getenv("GOOGLE_CLOUD_PROJECT", "ser594-ai-service")
VERTEX_AI_LOCATION = os.getenv("VERTEX_AI_LOCATION", "us-central1")

GEMINI_TEXT_MODEL = os.getenv("GEMINI_TEXT_MODEL", "gemini-2.5-flash")
GEMINI_VISION_MODEL = os.getenv("GEMINI_VISION_MODEL", "gemini-2.5-flash")
