import os
import sys
from dotenv import load_dotenv

# Load the .env file from the upper directory
# 从上级目录加载 .env 文件，确保本地测试时能读取 Vertex AI 配置。
env_path = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "../../../..", ".env"
)
if os.path.exists(env_path):
    load_dotenv(env_path)
    print(f"Successfully loaded environment variable file: {env_path}")
else:
    print(f"Warning: .env file not found: {env_path}")

# Set up test environment (simulating the behavior in docker-compose.yml)
os.environ["RABBITMQ_HOST"] = "localhost"

try:
    from app.config import LLM_TEXT_MODEL
    from app.services.llm_client import _generate_text
except ImportError as e:
    print(
        f"Import failed: {e}\nPlease ensure you run this script in the ai-service directory and dependencies are installed."
    )
    sys.exit(1)

print("\n--------------------------------------------------")
print("Preparing to test connectivity...")
print(f"Target model: {LLM_TEXT_MODEL}")
print(f"Identified project: {os.getenv('VERTEX_PROJECT_ID')}")
print("--------------------------------------------------\n")

try:
    messages = [
        {
            "role": "user",
            "content": "Please reply with EXACTLY this text: 'Vertex Connection Successful!'",
        }
    ]
    response = _generate_text(model=LLM_TEXT_MODEL, messages=messages)
    print(
        "Connectivity test [SUCCESS]! The large model returned the following content:\n"
    )
    print(f"AI: {response}\n")
except Exception as e:
    print("Connectivity test [FAILED]! Please check the error message:\n")
    print(e)
