<template>
  <div class="glass-app-container">
    <header class="app-header glass-header">
      <!-- 悬浮 Toast 轻提示 -->
      <transition name="toast-fade">
        <div v-if="toast.show" :class="['toast-wrapper', `toast-${toast.type}`]">
        <span class="material-icons" style="margin-right: 8px; font-size: 20px;">
          {{ toast.type === 'success' ? 'check_circle' : 'error_outline' }}
        </span>
          {{ toast.message }}
        </div>
      </transition>
      <div class="header-left">
        <h1>🧠 专属备考外脑 (工作区)</h1>
        <p class="subtitle">当前笔记本 ID: #{{ notebookId }}</p >
      </div>
      <div class="header-right">
        <!-- 【修改】：返回大厅而不是直接回任务看板 -->
        <button @click="goBack" class="btn-back">
          <span class="material-icons" style="font-size: 16px; margin-right: 4px;">arrow_back</span>
          返回笔记本大厅
        </button>
      </div>
    </header>

    <div class="copilot-layout">
      <!-- 左侧：知识库管理 -->
      <div class="library-section glass-card">
        <div class="section-title">
          <span class="material-icons" style="color: #34D399; margin-right: 8px;">library_books</span>
          <h2>专属知识库</h2>
        </div>
        <p class="section-desc">上传资料，构建当前笔记本的专属大脑</p >

        <div class="upload-area">
          <input type="file" ref="fileInput" @change="handleFileUpload" accept=".pdf,.txt" multiple style="display: none;" />
          <button class="btn-upload" @click="triggerUpload" :disabled="isUploading">
            <span class="material-icons" style="margin-right: 6px;">cloud_upload</span>
            {{ isUploading ? '资料传送中...' : '上传新复习资料 (PDF/TXT)' }}
          </button>
        </div>

        <ul class="doc-list">
          <li v-for="doc in documents" :key="doc.id" class="doc-card">
            <div class="doc-icon">
              <span class="material-icons" style="color: #34D399; font-size: 28px;">description</span>
            </div>
            <div class="doc-info">
              <span class="doc-name">{{ doc.fileName }}</span>
              <span class="doc-status">
                <span class="material-icons" style="font-size: 12px; margin-right: 2px;">{{ doc.status === 'PROCESSING' ? 'hourglass_empty' : 'check_circle' }}</span>
                {{ doc.status === 'PROCESSING' ? '待 AI 解析' : 'AI 已解析' }}
              </span>
            </div>
            <div class="doc-actions">
              <span class="material-icons delete-icon" @click.stop="triggerDeleteDoc(doc.id)">delete_outline</span>
            </div>
          </li>
          <span v-if="documents.length === 0" class="empty-state">
            当前笔记本还没有上传复习资料哦~
          </span>
        </ul>
      </div>

      <!-- 右侧：AI 问答区 -->
      <div class="chat-section glass-card">
        <div class="section-title">
          <span class="material-icons" style="color: #60A5FA; margin-right: 8px;">auto_awesome</span>
          <h2>AI 导师对话</h2>
        </div>
        <p class="section-desc">上下文独立，对话记录永久保存</p >

        <div class="chat-window" ref="chatWindow">
          <!-- 历史对话渲染 -->
          <div v-for="(msg, index) in chatHistory" :key="index" :class="['chat-bubble-wrapper', msg.role]">
            <div class="chat-bubble">
              <span v-if="msg.role === 'ai'" class="ai-avatar">👋</span>
              <div class="bubble-content">
                <template v-for="(part, idx) in parseMessage(msg.text)" :key="idx">
                  <!-- 普通文本 -->
                  <span v-if="part.type === 'text'" style="white-space: pre-wrap;">{{ part.content }}</span>
                  <!-- 引用按钮 -->
                  <span v-else-if="part.type === 'citation'" class="citation-badge" @click="showSource(msg.sources, part.id)">{{ part.id }}</span>
                </template>
              </div>
            </div>
          </div>
          <div v-if="isChatting" class="chat-bubble-wrapper ai">
            <div class="chat-bubble"><span class="ai-avatar">✨</span>正在飞速检索当前知识库...</div>
          </div>
        </div>

        <!-- 底部输入框 -->
        <div class="chat-input-wrapper">
          <textarea
              class="chat-input custom-scrollbar"
              v-model="userQuery"
              placeholder="向当前笔记本提问..."
              @keydown="handleKeydown"
              @input="autoResize"
              :disabled="isChatting"
              rows="1"
              ref="chatInputRef"
          ></textarea>
          <button
              class="btn-send"
              @click="handleChat"
              :disabled="!userQuery.trim() || isChatting"
          >
            <span class="material-icons" style="font-size: 18px; margin-right: 4px;">send</span>
            发送
          </button>
        </div>
      </div>
    </div>
    <!-- 自定义删除资料确认弹窗 -->
    <transition name="modal-fade">
      <div class="vibe-modal-overlay" v-if="deleteDocConfirm.show" @click.self="deleteDocConfirm.show = false">
        <div class="vibe-modal-content confirm-modal">
          <div class="modal-icon-warning">
            <span class="material-icons">warning_amber</span>
          </div>
          <h2>确定要删除这条资料吗？</h2>
          <p class="warning-text">删除后，该文件及其专属向量记忆将被彻底清除。</p >
          <div class="modal-footer">
            <button class="btn-pill btn-outline" @click="deleteDocConfirm.show = false">取消</button>
            <button class="btn-pill btn-danger" @click="executeDeleteDoc">确定删除</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- 来源溯源查看弹窗 -->
    <transition name="modal-fade">
      <div class="vibe-modal-overlay" v-if="sourceModal.show" @click.self="sourceModal.show = false">
        <div class="vibe-modal-content source-modal">
          <div class="source-header">
            <span class="material-icons" style="color: #60A5FA; margin-right: 8px;">find_in_page</span>
            <h3>溯源信息 (来源自：{{ sourceModal.fileName }})</h3>
          </div>
          <div class="source-body custom-scrollbar">
            {{ sourceModal.content }}
          </div>
          <div class="modal-footer" style="margin-top: 20px;">
            <button class="btn-pill btn-outline" @click="sourceModal.show = false">关闭</button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import request from '../utils/request';

const router = useRouter();
const route = useRoute(); // 引入路由获取参数
const notebookId = route.params.notebookId; // 核心：当前页面的隔离 ID

const fileInput = ref(null);
const documents = ref([]);
const isUploading = ref(false);
const chatWindow = ref(null);
const toast = ref({ show: false, message: '', type: 'success' });
let toastTimer = null;
const userQuery = ref('');
const isChatting = ref(false);
const chatInputRef = ref(null);
const sourceModal = ref({ show: false, fileName: '', content: '' }); // 在 script 顶部新增溯源弹窗状态

// 1. 新增：资料删除确认弹窗的状态
const deleteDocConfirm = ref({ show: false, id: null });

// 2. 新增：点击垃圾桶时，呼出弹窗并记录要删除的资料 ID
const triggerDeleteDoc = (id) => {deleteDocConfirm.value = { show: true, id };};

// 初始聊天记录为空，等待后端拉取
const chatHistory = ref([]);

const goBack = () => router.push('/notebooks');
const triggerUpload = () => fileInput.value.click();

const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer);
  toast.value = { show: true, message, type };
  toastTimer = setTimeout(() => {
    toast.value.show = false;
  }, 3000);
};

// 帮前端解析 "[1]" 为按钮的魔法函数
const parseMessage = (text) => {
  if (!text) return [];
  const regex = /\[(\d+)\]/g;
  const parts = [];
  let lastIndex = 0;
  let match;
  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push({ type: 'text', content: text.substring(lastIndex, match.index) });
    }
    parts.push({ type: 'citation', id: parseInt(match[1]) });
    lastIndex = regex.lastIndex;
  }
  if (lastIndex < text.length) {
    parts.push({ type: 'text', content: text.substring(lastIndex) });
  }
  return parts;
};

// 点击引用角标触发溯源弹窗
const showSource = (sources, id) => {
  if (!sources || sources.length === 0) {
    showToast('当前回答没有关联的本地知识库来源', 'error');
    return;
  }

  // 1. 正常去找 AI 标出的序号
  let source = sources.find(s => s.id === id);

  // 2. 【前端容错机制】：如果 AI 产生了幻觉乱标了序号（例如只有1个来源它却标了2）
  // 我们直接拿检索出来的第 1 个最高相关性文件兜底显示，避免弹窗报错失效
  if (!source && sources.length > 0) {
    source = sources[0];
  }

  // 3. 弹出窗口
  if (source) {
    sourceModal.value = { show: true, fileName: source.fileName, content: source.content };
  } else {
    showToast('溯源信息丢失', 'error');
  }
};

// 清洗文件名：去除前面的时间戳和下划线 (例如 1777948535919_文档.txt -> 文档.txt)
const cleanFileName = (fileName) => {
  if (!fileName) return '未知文件';
  return fileName.replace(/^\d+_/, '');
};

// 修改：点击自定义弹窗的“确定”后，真正执行删除
const executeDeleteDoc = async () => {
  try {
    await request.delete(`/api/documents/${deleteDocConfirm.value.id}`);
    deleteDocConfirm.value.show = false; // 隐藏弹窗
    showToast('资料已成功删除', 'success');
    fetchDocuments(); // 刷新资料列表
  } catch (error) {
    showToast('删除失败，请稍后重试', 'error');
  }
};

const scrollToBottom = async () => {
  await nextTick();
  if (chatWindow.value) {
    chatWindow.value.scrollTop = chatWindow.value.scrollHeight;
  }
};

let pollingTimer = null;

// 【修改】按笔记本 ID 获取资料
const fetchDocuments = async () => {
  try {
    documents.value = await request.get(`/api/documents?notebookId=${notebookId}`);
    const hasProcessing = documents.value.some(doc => doc.status === 'PROCESSING');
    if (hasProcessing && !pollingTimer) {
      pollingTimer = setInterval(() => { fetchDocuments(); }, 2000);
    } else if (!hasProcessing && pollingTimer) {
      clearInterval(pollingTimer);
      pollingTimer = null;
    }
  } catch (error) {
    console.error("获取专属知识库失败", error);
  }
};

// 【修改】fetchChatHistory：把数据库里的 JSON 字符串转回数组
const fetchChatHistory = async () => {
  try {
    const history = await request.get(`/api/chat/${notebookId}`);
    if (history && history.length > 0) {
      chatHistory.value = history.map(msg => ({
        role: msg.role,
        text: msg.content,
        sources: msg.sources ? JSON.parse(msg.sources) : [],
        isExpanded: false // <--- 新增：让历史记录默认折叠起来
      }));
    } else {
      chatHistory.value = [{ role: 'ai', text: '你好！我已经准备好了，上传资料后随时向我提问吧！', sources: [] }];
    }
    await scrollToBottom();
  } catch (error) {
    console.error("获取对话历史失败", error);
  }
};

onUnmounted(() => {
  if (pollingTimer) clearInterval(pollingTimer);
});

const handleFileUpload = async (event) => {
  const files = event.target.files;
  if (!files || files.length === 0) return;

  isUploading.value = true;
  let successCount = 0;
  let failCount = 0;

  // 使用 for...of 循环，让文件一个接一个地上传（保护后端 Python 引擎不被瞬间冲垮）
  for (const file of files) {
    if (file.size === 0) {
      showToast(`【${file.name}】是空文件，已跳过`, 'error');
      continue;
    }

    const isDuplicate = documents.value.some(doc => doc.fileName === file.name);
    if (isDuplicate) {
      showToast(`【${file.name}】已存在，已跳过`, 'error');
      continue;
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('notebookId', notebookId);

    try {
      await request.post('/api/documents/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      successCount++;
    } catch (error) {
      const errorMsg = error.response?.data || `【${file.name}】上传失败`;
      showToast(errorMsg, 'error');
      failCount++;
    }
  }

  // 全部循环结束后，统一给个提示并刷新列表
  if (successCount > 0) {
    showToast(`${successCount} 个文件已加入解析队列！`, 'success');
    await fetchDocuments();
  }

  isUploading.value = false;
  event.target.value = ''; // 清空 input，方便下次选择同样的文件
};

const deleteDoc = async (id) => {
  if (!confirm("确定要删除这条资料吗？")) return;
  try {
    await request.delete(`/api/documents/${id}`);
    showToast('资料已成功删除', 'success');
    await fetchDocuments();
  } catch (error) {
    showToast('删除失败，请稍后重试', 'error');
  }
};

const handleChat = async () => {
  if (!userQuery.value.trim() || isChatting.value) return;

  const currentQuestion = userQuery.value;
  chatHistory.value.push({ role: 'user', text: currentQuestion, sources: [] });
  userQuery.value = '';
  isChatting.value = true;

  await nextTick(() => {
    if (chatInputRef.value) {
      chatInputRef.value.style.height = '24px';
    }
  });
  await scrollToBottom();

  try {
    const res = await request.post('/api/chat', {
      notebookId: parseInt(notebookId),
      query: currentQuestion
    });

    chatHistory.value.push({
      role: 'ai',
      text: res.answer,
      sources: res.sources || [],
      isExpanded: false
    });

  } catch (error) {
    chatHistory.value.push({
      role: 'ai',
      text: '❌ 抱歉，大脑暂时断连，请检查 Java 后端或 Python 引擎是否启动。',
      sources: [],
      isExpanded: false
    });
  } finally {
    isChatting.value = false;
    await scrollToBottom();
    await nextTick(() => {
      if (chatInputRef.value) chatInputRef.value.focus();
    });
  }
};

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    handleChat();
  }
};

const autoResize = () => {
  const textarea = chatInputRef.value;
  if (!textarea) return;
  textarea.style.height = 'auto';
  textarea.style.height = textarea.scrollHeight + 'px';
};

onMounted(() => {
  fetchDocuments();
  fetchChatHistory(); // 加载页面时拉取漫游历史记录
});
</script>

<style scoped>
/* 此处的 CSS 和之前完全一致，保留了你喜欢的渐变与毛玻璃风格 */
.glass-app-container {min-height: 100vh;padding: 30px 40px;background: linear-gradient(135deg, #F0FDF4 0%, #E0F2FE 100%);background-image: radial-gradient(at 80% 80%, rgba(209, 250, 229, 0.7) 0, transparent 50%), radial-gradient(at 10% 20%, rgba(224, 242, 254, 0.7) 0, transparent 50%);font-family: system-ui, -apple-system, sans-serif;box-sizing: border-box;}

.glass-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.glass-header h1 { font-size: 24px; color: #1F2937; margin: 0 0 4px 0; font-weight: 700; }
.subtitle { color: #6B7280; font-size: 14px; margin: 0; }
.btn-back { display: flex; align-items: center; background: rgba(255, 255, 255, 0.8); border: 1px solid rgba(255, 255, 255, 0.9); padding: 8px 16px; border-radius: 999px; color: #4B5563; font-weight: 500; cursor: pointer; box-shadow: 0 2px 10px rgba(0,0,0,0.02); transition: all 0.3s ease; }
.btn-back:hover { background: #ffffff; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.05); }

.copilot-layout { display: grid; grid-template-columns: 350px 1fr; gap: 24px; height: calc(100vh - 120px); }

.glass-card { background: rgba(255, 255, 255, 0.65); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); border: 1px solid rgba(255, 255, 255, 0.8); border-radius: 24px; box-shadow: 0 10px 40px rgba(0, 0, 0, 0.03); padding: 24px; display: flex; flex-direction: column; }

.section-title { display: flex; align-items: center; margin-bottom: 4px; }
.section-title h2 { font-size: 18px; color: #111827; margin: 0; font-weight: 600; }
.section-desc { color: #9CA3AF; font-size: 13px; margin: 0 0 20px 0; }

.btn-upload { width: 100%; display: flex; align-items: center; justify-content: center; background: #D1FAE5; color: #059669; border: none; padding: 14px; border-radius: 16px; font-weight: 600; font-size: 15px; cursor: pointer; transition: all 0.3s ease; margin-bottom: 20px; }
.btn-upload:hover { background: #A7F3D0; transform: translateY(-1px); }

.doc-list { list-style: none; padding: 0; margin: 0; overflow-y: auto; flex: 1; padding-right: 4px; }
.doc-list::-webkit-scrollbar { width: 4px; }
.doc-list::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 4px; }

.doc-card { display: flex; align-items: center; background: rgba(255, 255, 255, 0.9); padding: 16px; border-radius: 16px; margin-bottom: 12px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02); border: 1px solid rgba(255, 255, 255, 1); transition: all 0.2s; position: relative; }
.doc-card:hover { box-shadow: 0 6px 16px rgba(0, 0, 0, 0.04); transform: translateY(-2px); }
.doc-card::before { content: ''; position: absolute; left: 0; top: 15%; height: 70%; width: 3px; background: #34D399; border-radius: 0 4px 4px 0; }
.doc-info { flex: 1; margin-left: 12px; display: flex; flex-direction: column; }
.doc-name { font-size: 14px; font-weight: 600; color: #374151; margin-bottom: 4px; word-break: break-all; }
.doc-status { display: flex; align-items: center; font-size: 12px; color: #10B981; }
.doc-actions { display: flex; align-items: center; gap: 8px; color: #9CA3AF; }
.delete-icon { font-size: 20px; cursor: pointer; color: #FCA5A5; transition: 0.2s; }
.delete-icon:hover { color: #EF4444; }
.empty-state { text-align: center; color: #9CA3AF; font-size: 14px; margin-top: 40px; }

.chat-window { flex: 1; overflow-y: auto; padding: 10px 20px 20px 0; display: flex; flex-direction: column; }
.chat-window::-webkit-scrollbar { width: 4px; }
.chat-window::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 4px; }

.chat-bubble-wrapper { display: flex; margin-bottom: 24px; width: 100%; }
.chat-bubble-wrapper.ai { justify-content: flex-start; }
.chat-bubble-wrapper.user { justify-content: flex-end; }

.chat-bubble { display: flex; align-items: flex-start; max-width: 80%; padding: 16px 20px; font-size: 15px; line-height: 1.6; box-shadow: 0 4px 12px rgba(0,0,0,0.03); }
.ai .chat-bubble { background: rgba(255, 255, 255, 0.9); border: 1px solid rgba(255,255,255,1); color: #374151; border-radius: 4px 20px 20px 20px; }
.user .chat-bubble { background: #34D399; color: #ffffff; border-radius: 20px 4px 20px 20px; }
.ai-avatar { margin-right: 8px; font-size: 18px; }

.chat-input-wrapper { display: flex; align-items: center; background: rgba(255, 255, 255, 0.8); border: 1px solid rgba(255, 255, 255, 1); border-radius: 99px; padding: 6px 6px 6px 20px; box-shadow: 0 4px 20px rgba(0,0,0,0.04); margin-top: 10px; }
.chat-input { flex: 1; border: none; background: transparent; outline: none; font-size: 15px; color: #374151; resize: none; padding-top: 2px; line-height: 1.5; max-height: 120px; font-family: inherit; }
.chat-input::placeholder { color: #9CA3AF; }

.btn-send { display: flex; align-items: center; justify-content: center; background: #34D399; color: white; border: none; padding: 10px 24px; border-radius: 999px; font-weight: 600; font-size: 15px; cursor: pointer; transition: all 0.3s ease; }
.btn-send:hover:not(:disabled) { background: #10B981; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3); }
.btn-send:disabled { background: #D1D5DB; cursor: not-allowed; }

.toast-wrapper { position: fixed; top: 32px; left: 50%; transform: translateX(-50%); padding: 12px 24px; border-radius: 99px; display: flex; align-items: center; font-size: 14px; font-weight: 500; z-index: 9999; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px); }
.toast-success { background: rgba(16, 185, 129, 0.85); color: white; border: 1px solid rgba(255, 255, 255, 0.3); }
.toast-error { background: rgba(239, 68, 68, 0.85); color: white; border: 1px solid rgba(255, 255, 255, 0.3); }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translate(-50%, -20px) scale(0.95); }

/* --- 确认弹窗专属警告样式 --- */
.vibe-modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(17,24,39,0.4); backdrop-filter: blur(8px);
  display: flex; justify-content: center; align-items: center; z-index: 999;
}
.vibe-modal-content {
  background: white; width: 400px; padding: 32px; border-radius: 24px; box-shadow: 0 25px 50px rgba(0,0,0,0.2);
}
.confirm-modal { text-align: center; }
.modal-icon-warning {
  background: #FEF2F2; color: #EF4444; width: 64px; height: 64px;
  border-radius: 50%; display: flex; justify-content: center; align-items: center;
  margin: 0 auto 16px auto; font-size: 32px;
}
.warning-text { color: #6B7280; font-size: 14px; line-height: 1.6; margin-bottom: 24px; }
.modal-footer { display: flex; justify-content: center; gap: 12px; }
.btn-danger { background: #EF4444; color: white; border: none; }
.btn-danger:hover { background: #DC2626; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3); }

/* 复用一些弹窗通用样式，如果你之前有了就可以忽略 */
.btn-pill { display: flex; align-items: center; gap: 6px; padding: 10px 22px; border-radius: 999px; font-weight: 600; cursor: pointer; transition: 0.3s; }
.btn-outline { background: #ffffff; color: #374151; border: 1px solid #E5E7EB; }
.btn-outline:hover { background: #F3F4F6; }
.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.3s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: translateY(10px); }

/* NotebookLM 风格的引用角标 */
.citation-badge {display: inline-flex; justify-content: center; align-items: center;background: #E0E7FF; color: #4338CA; font-size: 12px; font-weight: 700;width: 20px; height: 20px; border-radius: 50%; margin: 0 4px;cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 4px rgba(67, 56, 202, 0.15);transform: translateY(-2px); /* 稍微上浮，类似上标 */}
.citation-badge:hover { background: #4338CA; color: white; transform: translateY(-3px) scale(1.1); box-shadow: 0 4px 8px rgba(67, 56, 202, 0.3); }

/* 溯源查看弹窗专属样式 */
.source-modal { width: 500px; max-width: 90vw; }
.source-header { display: flex; align-items: center; border-bottom: 1px solid #E5E7EB; padding-bottom: 12px; margin-bottom: 16px; }
.source-header h3 { margin: 0; font-size: 16px; color: #1F2937; }
.source-body { background: #F9FAFB; padding: 16px; border-radius: 12px; font-size: 14px; color: #4B5563; line-height: 1.8; max-height: 300px; overflow-y: auto; white-space: pre-wrap; border: 1px solid #E5E7EB; }

/* --- 新增：溯源兜底区域的优雅样式 --- */
.source-fallback-area {margin-top: 12px;width: 100%;}
.source-divider {height: 1px;background: rgba(0, 0, 0, 0.06);margin-bottom: 8px;}
.source-title {font-size: 12px;color: #6B7280;margin-bottom: 6px;font-weight: 500;}
.source-chips {display: flex;flex-wrap: wrap;gap: 8px;}
.fallback-chip {background: #F3F4F6;color: #374151;font-size: 12px;padding: 4px 10px;border-radius: 6px;cursor: pointer;transition: all 0.2s ease;border: 1px solid #E5E7EB;display: inline-flex;align-items: center;}
.fallback-chip:hover {background: #E0E7FF;color: #4338CA;border-color: #C7D2FE;transform: translateY(-1px);}

/* ==========================================
   NotebookLM 风格高级溯源面板
========================================== */
.notebook-source-widget {
  /* 核心魔法：宽度由内容撑开，不再占满整行 */
  width: fit-content;
  min-width: 180px;
  max-width: 100%;

  /* 柔和的 Material 3 表面色，无边框 */
  background-color: #F3F4F6;
  border-radius: 16px;
  margin-top: 12px;

  /* 确保内部圆角不溢出 */
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* --- 头部触发区 --- */
.widget-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s;
}

.widget-header:hover {
  background-color: #E5E7EB;
}

.widget-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.chevron {
  font-size: 18px;
  color: #6B7280;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.chevron.is-open {
  /* 展开时箭头平滑翻转 180 度 */
  transform: rotate(180deg);
}

/* --- 展开的内容区 --- */
.widget-body {
  padding: 0 8px 8px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.source-row {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #FFFFFF;
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  /* 非常克制的交互边框，替代粗糙的阴影 */
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.source-row:hover {
  border-color: #D1D5DB;
  background-color: #F9FAFB;
}

/* --- 左侧极致还原的数字徽章 --- */
.source-index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  /* 提取自截图的淡紫色底与深紫色字 */
  background-color: #E0E7FF;
  color: #4338CA;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.source-filename {
  font-size: 13px;
  color: #4B5563;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>