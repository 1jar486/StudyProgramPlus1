from langchain_openai import ChatOpenAI
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_community.document_loaders import PyPDFLoader, TextLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
# 【新增导入】：历史感知检索器、消息占位符和消息对象
from langchain.chains import create_history_aware_retriever, create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.messages import HumanMessage, AIMessage
import os

# 1. 配置 DeepSeek 大模型 (兼容 OpenAI 格式)
DEEPSEEK_API_KEY = "sk-87e4b5bb71154fd6ae9cc7e6252b448e" # ！！！填入你申请的 API Key！！！
llm = ChatOpenAI(
    api_key=DEEPSEEK_API_KEY,
    base_url="https://api.deepseek.com",
    model="deepseek-v4-pro",
    max_tokens=2000
)

# 2. 配置本地向量模型 (免费的轻量级 Embedding 模型)
# 首次运行会自动从 HuggingFace 下载模型权重，约几十MB
# 换成对中文极其友好的 BGE 模型
embeddings = HuggingFaceEmbeddings(model_name="BAAI/bge-small-zh-v1.5")

# 3. 本地向量数据库基础存储目录
DB_DIR = "./chroma_db"

def get_vector_store(notebook_id: int):
    """
    核心隔离逻辑：根据 notebook_id 动态获取或创建独立的向量集合 (Collection)
    这样可以确保不同笔记本的数据在底层完全物理隔离。
    """
    collection_name = f"notebook_{notebook_id}"
    return Chroma(
        persist_directory=DB_DIR,
        embedding_function=embeddings,
        collection_name=collection_name
    )

def process_document(file_path: str, notebook_id: int):
    """读取文件、切片并存入专属笔记本的向量数据库"""
    print(f"开始解析文件: {file_path} 到笔记本: {notebook_id}")

    # 读取文件逻辑支持 TXT 和 PDF
    if file_path.endswith('.pdf'):
        loader = PyPDFLoader(file_path)
        docs = loader.load()
    elif file_path.endswith('.txt'):
        # 指定 encoding='utf-8' 防止中文乱码
        loader = TextLoader(file_path, encoding='utf-8')
        docs = loader.load()
    else:
        raise ValueError("目前仅支持 PDF 和 TXT 格式")

    # 文本切片
    # 智能中文分块策略：优先按段落切，再按句号切，保证一句话绝对不会被切断两半。
    text_splitter = RecursiveCharacterTextSplitter(
        separators=["\n\n", "\n", "。", "！", "？", "；", "，"],
        chunk_size=800,  # 稍微调大，给 AI 更多上下文
        chunk_overlap=150
    )
    splits = text_splitter.split_documents(docs)

    # 动态获取当前笔记本的专属向量库，并存入数据
    vector_store = get_vector_store(notebook_id)
    vector_store.add_documents(documents=splits)

    print(f"✅ 文件 {os.path.basename(file_path)} 向量化并隔离存储成功！")
    return True

def chat_with_knowledge(query: str, notebook_id: int, history: list = None):
    if history is None:
        history = []

    # 1. 组装 LangChain 格式的历史消息
    chat_history = []
    for msg in history:
        if msg.get("role") == "user":
            chat_history.append(HumanMessage(content=msg.get("content", "")))
        elif msg.get("role") == "ai":
            chat_history.append(AIMessage(content=msg.get("content", "")))

    # ==========================================
    # 【核心修改】：全局 AI 模式 (短路逻辑)
    # ==========================================
    if notebook_id == 999:
        print("🧠 进入全局 LLM 模式 (跳过向量检索)")

        # 组装全局对话 Prompt
        global_system_prompt = (
            "你是一个全能的智能学习助手。请根据用户的提问和上下文历史进行解答。"
            "你的回答应该专业、友善，并且富有启发性。"
        )
        global_prompt = ChatPromptTemplate.from_messages([
            ("system", global_system_prompt),
            MessagesPlaceholder("chat_history"),
            ("human", "{input}"),
        ])

        # 直接使用 LLM 进行回答
        chain = global_prompt | llm
        response = chain.invoke({"input": query, "chat_history": chat_history})

        return {"answer": response.content, "sources": []}

    # ==========================================
    # 专属外脑 RAG 模式
    # ==========================================
    print(f"📚 进入专属外脑模式，检索笔记本: {notebook_id}")
    vector_store = get_vector_store(notebook_id)
    retriever = vector_store.as_retriever(
        search_type="similarity_score_threshold",
        search_kwargs={"score_threshold": 0.4, "k": 5}
    )

    contextualize_q_system_prompt = (
        "给定聊天历史记录和最新的用户问题，该问题可能会引用历史记录中的上下文，"
        "请构建一个独立的问题。不要回答问题，只需重新表述它。"
    )
    contextualize_q_prompt = ChatPromptTemplate.from_messages([
        ("system", contextualize_q_system_prompt),
        MessagesPlaceholder("chat_history"),
        ("human", "{input}"),
    ])
    history_aware_retriever = create_history_aware_retriever(llm, retriever, contextualize_q_prompt)

    qa_system_prompt = (
        "你是一个极其严谨的学术级知识库助手。你的唯一任务是基于提供的【参考资料】来解答用户问题。\n"
        "执行以下步骤：\n"
        "1. 在脑海中默默评估每一条【参考资料】是否真的与问题相关。\n"
        "2. 剔除完全无关的资料。\n"
        "3. 仅使用剩余的高度相关资料组织答案。\n\n"
        "【极其重要的强制规则】：\n"
        "- 任何未经参考资料证实的话，你一个字都不准写。\n"
        "- 在你回答的每一个关键陈述后，必须紧跟来源编号，格式严格为 [1], [2] 等。如果没有对应的依据，不要标编号。\n"
        "- 如果你发现所有资料都与问题无关，请直接回答：“当前笔记本中的资料未提及相关内容。”\n\n"
        "【参考资料】:\n{context}\n\n"
        "(注：上述资料按顺序对应编号 [1], [2], [3]...)"
    )
    qa_prompt = ChatPromptTemplate.from_messages([
        ("system", qa_system_prompt),
        MessagesPlaceholder("chat_history"),
        ("human", "{input}"),
    ])

    question_answer_chain = create_stuff_documents_chain(llm, qa_prompt)
    rag_chain = create_retrieval_chain(history_aware_retriever, question_answer_chain)

    response = rag_chain.invoke({"input": query, "chat_history": chat_history})
    answer = response["answer"]
    source_docs = response["context"]

    sources = []
    for i, doc in enumerate(source_docs):
        file_path = doc.metadata.get("source", "未知文件")
        file_name = os.path.basename(file_path)
        sources.append({
            "id": i + 1,
            "fileName": file_name,
            "content": doc.page_content
        })

    return {"answer": answer, "sources": sources}

def delete_document_vectors(file_path: str, notebook_id: int):
    """
    🌌 星云安全防御：双重物理垃圾回收机制
    不仅无缝抹除 Chroma 向量库里的数据切片，同时将本地磁盘/服务器上的物理文件一并抹杀！
    """
    print(f"🧹 启动全链路大扫除：解绑笔记本[{notebook_id}] -> 目标文件[{file_path}]")

    # 🌟 修复隐患1：标准化路径格式，自动消灭 Windows \\ 和 Linux / 的跨平台不匹配悲剧
    normalized_path = os.path.normpath(file_path)

    vector_success = False
    file_success = False

    # 1️⃣ 物理清空 ChromaDB 中的幽灵向量
    try:
        vector_store = get_vector_store(notebook_id)

        # 双重保险匹配删除
        vector_store._collection.delete(where={"source": file_path})
        if normalized_path != file_path:
            vector_store._collection.delete(where={"source": normalized_path})

        print(f"✅ [1/2] Chroma 向量库中关于该文件的所有 Chunk 切片已彻底融毁")
        vector_success = True
    except Exception as e:
        print(f"❌ Chroma 向量清除失败，错误日志: {e}")

    # 2️⃣ 🌟 核心补丁：物理清空本地磁盘/服务器上的幽灵物理文件
    try:
        # 尝试寻找并移除标准化路径的文件
        if os.path.exists(normalized_path):
            os.remove(normalized_path)
            print(f"✅ [2/2] 服务器磁盘物理文件已彻底抹除: {normalized_path}")
            file_success = True
        elif os.path.exists(file_path):
            os.remove(file_path)
            print(f"✅ [2/2] 服务器磁盘物理文件已彻底抹除: {file_path}")
            file_success = True
        else:
            # 安全放行：如果文件本身已经不存在了，说明已经干净，不卡死流程
            print(f"⚠️ 提示: 本地磁盘未发现该物理文件（可能在此之前已被手动移除）: {normalized_path}")
            file_success = True
    except Exception as e:
        print(f"❌ 服务器磁盘物理文件删除失败，错误日志: {e}")

    # 只有向量和物理文件都搞定了，才向 Java 后端返回真正的 True
    return vector_success and file_success