"""
智能求职助手 - Python AI服务
FastAPI入口文件
"""

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
import os

app = FastAPI(
    title="Resume Assistant AI Service",
    description="智能求职助手AI服务 - 简历解析、匹配计算、对话处理",
    version="1.0.0"
)


@app.get("/")
async def root():
    """根路径 - 服务状态"""
    return {
        "service": "Resume Assistant AI",
        "version": "1.0.0",
        "status": "running"
    }


@app.get("/health")
async def health_check():
    """健康检查端点"""
    return {
        "status": "healthy",
        "openai_api_key_configured": bool(os.getenv("OPENAI_API_KEY"))
    }


@app.get("/api/status")
async def status():
    """详细状态信息"""
    return {
        "service": "AI Service",
        "features": [
            "resume_parse",
            "embedding_generation",
            "job_matching",
            "chat_processing"
        ],
        "ready": True
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
