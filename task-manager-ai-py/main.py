import os
import requests
from typing import List, Dict, Optional
from pydantic import BaseModel
# --- [新增：解决 HuggingFace 连接与 SSL 报错] ---
# 设置国内镜像源，解决 SSL 证书验证失败和连接超时的典型问题
os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"
os.environ["CHROMA_TELEMETRY"] = "false"
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
    notebook_id: int

class ChatRequest(BaseModel):
    query: str
    notebook_id: int
    # 【解耦核心】：新增 history 字段，接收包含角色的字典列表，默认设为空列表
    history: Optional[List[Dict[str, str]]] = []

class DeleteRequest(BaseModel):
    file_path: str
    notebook_id: int

@app.post("/api/ai/delete")
async def delete_file_vectors(request: DeleteRequest):
    """接口3：接收 Java 的指令，全链路物理销毁数据"""
    try:
        # 调用我们刚刚强化过的双重物理抹除机制
        success = rag_engine.delete_document_vectors(request.file_path, request.notebook_id)
        if success:
            return {"status": "success", "message": "全链路数据（向量+磁盘物理文件）已彻底深度销毁"}
        else:
            raise HTTPException(status_code=500, detail="由于文件状态异常，数据未完全销毁")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ai/parse")
async def parse_file(request: ParseRequest):
    try:
        # 1. 执行解析（向量化入库）
        success = rag_engine.process_document(request.file_path, request.notebook_id)

        # 2. 【核心回执逻辑】解析成功后，立刻通知 Java 更新状态
        if success:
            java_url = "http://localhost:9090/start/api/documents/update-status"
            # 发送文件路径，让 Java 知道是哪份文件搞定了
            try:
                requests.post(java_url, json={
                    "file_path": request.file_path,
                    "status": "COMPLETED"
                })
            except Exception as e:
                print(f"通知 Java 失败，但向量化已完成: {e}")

            return {"status": "success", "message": "文件已成功加入专属知识库"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ai/chat")
async def chat(request: ChatRequest):
    try:
        # ✅ 正确：直接将 rag_engine 组装好的完整数据返回给 Java
        result = rag_engine.chat_with_knowledge(request.query, request.notebook_id, request.history)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    # 启动服务，运行在 8000 端口
    uvicorn.run(app, host="0.0.0.0", port=8000)