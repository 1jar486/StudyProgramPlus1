import os
import requests
# --- [新增：解决 HuggingFace 连接与 SSL 报错] ---
# 设置国内镜像源，解决 SSL 证书验证失败和连接超时的典型问题
os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"
# --- [新增结束] ---

from fastapi import FastAPI, HTTPException
# ... 后面保持不变
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import rag_engine
import uvicorn

app = FastAPI()

# 允许跨域请求 (为了让 Vue 能直接调)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 定义接收数据的格式
class ParseRequest(BaseModel):
    file_path: str

class ChatRequest(BaseModel):
    query: str

@app.post("/api/ai/parse")
async def parse_file(request: ParseRequest):
    try:
        # 1. 执行解析
        success = rag_engine.process_document(request.file_path)

        # 2. 解析成功后，通知 Java (回执)
        if success:
            java_url = "http://localhost:9090/start/api/documents/update-status"
            requests.post(java_url, json={"file_path": request.file_path})

        return {"status": "success", "message": "文件已成功加入知识库"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ai/chat")
async def chat(request: ChatRequest):
    """接口2：接收前端的提问，让 AI 回答"""
    try:
        answer = rag_engine.chat_with_knowledge(request.query)
        return {"answer": answer}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    # 启动服务，运行在 8000 端口
    uvicorn.run(app, host="0.0.0.0", port=8000)