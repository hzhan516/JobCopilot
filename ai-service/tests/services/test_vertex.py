import os
import sys
from dotenv import load_dotenv

# 加载上层目录的 .env 文件
env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../../..', '.env')
if os.path.exists(env_path):
    load_dotenv(env_path)
    print(f"成功加载环境变量文件: {env_path}")
else:
    print(f"⚠️ 警告: 未找到 .env 文件: {env_path}")

# 设置测试环境（模拟 docker-compose.yml 里的行为）
os.environ["RABBITMQ_HOST"] = "localhost"

try:
    from app.config import LLM_TEXT_MODEL
    from app.services.llm_client import _generate_text
except ImportError as e:
    print(f"导入失败: {e}\n请确保您在 ai-service 目录下运行此脚本，并且已安装依赖。")
    sys.exit(1)

print("\n--------------------------------------------------")
print("准备测试连通性...")
print(f"目标模型: {LLM_TEXT_MODEL}")
print(f"识别到的项目: {os.getenv('VERTEX_PROJECT_ID')}")
print("--------------------------------------------------\n")

try:
    messages = [{"role": "user", "content": "Please reply with EXACTLY this text: 'Vertex Connection Successful!'"}]
    response = _generate_text(model=LLM_TEXT_MODEL, messages=messages)
    print("连通测试【成功】！大模型返回了以下内容：\n")
    print(f"AI: {response}\n")
except Exception as e:
    print("连通测试【失败】！请检查报错信息：\n")
    print(e)