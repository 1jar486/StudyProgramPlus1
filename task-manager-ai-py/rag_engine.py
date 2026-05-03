import os
from langchain_openai import ChatOpenAI
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_community.document_loaders import PyPDFLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate
# 找到原来的导入部分，加上这一行
from langchain_community.document_loaders import PyPDFLoader, TextLoader

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
embeddings = HuggingFaceEmbeddings(model_name="all-MiniLM-L6-v2")

# 3. 初始化本地向量数据库 (持久化保存在 chroma_db 文件夹中)
DB_DIR = "./chroma_db"
vector_store = Chroma(persist_directory=DB_DIR, embedding_function=embeddings)

def process_document(file_path: str):
    """读取文件、切片并存入向量数据库"""
    print(f"开始解析文件: {file_path}")

    # --- [逻辑更新：支持 TXT 和 PDF] ---
    if file_path.endswith('.pdf'):
        loader = PyPDFLoader(file_path)
        docs = loader.load()
    elif file_path.endswith('.txt'):
        # 指定 encoding='utf-8' 防止中文乱码
        loader = TextLoader(file_path, encoding='utf-8')
        docs = loader.load()
    else:
        raise ValueError("目前仅支持 PDF 和 TXT 格式")
    # --- [更新结束] ---

    # 文本切片
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
    splits = text_splitter.split_documents(docs)

    # 存入 Chroma 向量数据库
    vector_store.add_documents(documents=splits)
    print(f"✅ 文件 {os.path.basename(file_path)} 向量化成功！")
    return True

def chat_with_knowledge(query: str):
    """基于本地知识库进行问答"""
    # 构造 Prompt 模板
    system_prompt = (
        "你是一个专业的备考外脑助手。请严格根据以下检索到的【参考资料】来回答用户的问题。\n"
        "如果参考资料中没有相关信息，请诚实地回答“我不知道”，绝不允许胡编乱造。\n\n"
        "【参考资料】:\n{context}"
    )
    prompt = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        ("human", "{input}"),
    ])

    # 组装 RAG 检索链
    retriever = vector_store.as_retriever(search_kwargs={"k": 3}) # 每次找出最相关的 3 段话
    question_answer_chain = create_stuff_documents_chain(llm, prompt)
    rag_chain = create_retrieval_chain(retriever, question_answer_chain)

    # 发起提问
    response = rag_chain.invoke({"input": query})
    return response["answer"]