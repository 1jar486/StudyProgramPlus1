<template>
  <div class="glass-app-container">
    <header class="nebula-header">
      <transition name="toast-fade">
        <div v-if="toast.show" :class="['toast-wrapper', `toast-${toast.type}`]">
          <span class="material-icons toast-icon">{{ toast.type === 'success' ? 'check_circle' : 'error_outline' }}</span>
          {{ toast.message }}
        </div>
      </transition>

      <div class="header-left">
        <h1 class="nebula-title-main">
          <span class="material-icons tech-icon">psychology</span>
          专属备考外脑
        </h1>
        <p class="nebula-subtitle">已挂载节点 ID: #{{ notebookId }} · 当前环境独立</p >
      </div>
      <div class="header-right">
        <button @click="goBack" class="btn-nebula-outline">
          <span class="material-icons">arrow_back</span>
          断开连接并返回
        </button>
      </div>
    </header>

    <div class="copilot-layout">
      <div class="library-section glass-panel">
        <div class="section-header">
          <h2><span class="material-icons">storage</span> 知识库区块</h2>
          <p>上传本地复习资料，构建当前节点的独立记忆</p >
        </div>

        <div class="upload-area">
          <input type="file" ref="fileInput" @change="handleFileUpload" accept=".pdf,.txt" multiple style="display: none;" />
          <button class="btn-upload" @click="triggerUpload" :disabled="isUploading">
            <span class="material-icons">{{ isUploading ? 'cloud_sync' : 'cloud_upload' }}</span>
            {{ isUploading ? '数据切片传输中...' : '注入新资料 (PDF/TXT)' }}
          </button>
        </div>

        <ul class="doc-list custom-scrollbar">
          <li v-for="doc in documents" :key="doc.id" class="doc-item" @click="openPreview(doc)">
            <div class="doc-icon"><span class="material-icons">description</span></div>
            <div class="doc-info">
              <span class="doc-name" :title="doc.fileName">{{ cleanFileName(doc.fileName) }}</span>
              <span class="doc-status" :class="{ 'processing': doc.status === 'PROCESSING' }">
                <span class="material-icons status-icon">{{ doc.status === 'PROCESSING' ? 'memory' : 'verified' }}</span>
                {{ doc.status === 'PROCESSING' ? 'AI 神经元解析中...' : '解析完成，可检索' }}
              </span>
            </div>
            <button class="btn-icon-danger" @click.stop="triggerDeleteDoc(doc.id)" title="销毁资料">
              <span class="material-icons">delete</span>
            </button>
          </li>
          <div v-if="documents.length === 0" class="empty-state">
            <span class="material-icons" style="font-size: 32px; margin-bottom: 10px; opacity: 0.5;">folder_open</span>
            <p>该区块暂时为空，亟待数据注入</p >
          </div>
        </ul>
      </div>

      <div class="chat-section glass-panel">
        <div class="section-header">
          <h2><span class="material-icons" style="color: #60A5FA;">auto_awesome</span> AI 导师链路</h2>
          <p>基于当前知识库区块进行对话检索</p >
        </div>

        <div class="chat-window custom-scrollbar" ref="chatWindow">
          <div v-for="(msg, index) in chatHistory" :key="index" :class="['chat-bubble-wrapper', msg.role]">
            <div class="chat-bubble-inner">
              <div v-if="msg.role === 'ai'" class="ai-avatar">✨</div>
              <div class="chat-bubble">
                <template v-for="(part, idx) in parseMessage(msg.text)" :key="idx">
                  <span v-if="part.type === 'text'" style="white-space: pre-wrap;">{{ part.content }}</span>
                  <span v-else-if="part.type === 'citation'" class="citation-badge" @click="showSource(msg.sources, part.id)">
                    {{ part.id }}
                  </span>
                </template>
              </div>
              <div v-if="msg.role === 'user'" class="user-avatar">👤</div>
            </div>
          </div>
          <div v-if="isChatting" class="chat-bubble-wrapper ai">
            <div class="chat-bubble-inner">
              <div class="ai-avatar">✨</div>
              <div class="chat-bubble typing-glow">正在高速遍历本地知识图谱...</div>
            </div>
          </div>
        </div>

        <div class="chat-input-wrapper">
          <textarea
              class="chat-input custom-scrollbar"
              v-model="userQuery"
              placeholder="向 AI 导师提问 (按 Enter 飞速发送)..."
              @keydown="handleKeydown"
              @input="autoResize"
              :disabled="isChatting"
              rows="1"
              ref="chatInputRef"
          ></textarea>
          <button class="btn-send" @click="handleChat" :disabled="!userQuery.trim() || isChatting">
            <span class="material-icons">send</span>
          </button>
        </div>
      </div>
    </div>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="deleteDocConfirm.show" @click.self="deleteDocConfirm.show = false">
        <div class="nebula-modal-content glass-panel danger-modal">
          <div class="danger-icon-wrapper"><span class="material-icons">warning_amber</span></div>
          <h2>确认销毁该资料节点？</h2>
          <p class="warning-text">切断链接后，该文件的所有专属向量记忆将被从空间中彻底清除。</p >
          <div class="modal-footer">
            <button class="btn-nebula-outline" @click="deleteDocConfirm.show = false">中止</button>
            <button class="btn-nebula-danger" @click="executeDeleteDoc">确认销毁</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="sourceModal.show" @click.self="sourceModal.show = false">
        <div class="nebula-modal-content glass-panel source-modal">
          <div class="source-header">
            <span class="material-icons tech-icon" style="font-size: 24px;">find_in_page</span>
            <h3>溯源定位: {{ sourceModal.fileName }}</h3>
          </div>
          <div class="source-body custom-scrollbar">
            {{ sourceModal.content }}
          </div>
          <div class="modal-footer" style="margin-top: 24px;">
            <button class="btn-nebula-outline" @click="sourceModal.show = false">关闭窗口</button>
          </div>
        </div>
      </div>
    </transition>
    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="previewModal.show" @click.self="closePreview">
        <div class="nebula-modal-content glass-panel preview-modal-lg">

          <div class="modal-header">
            <h2 class="preview-title">
              <span class="material-icons" :style="{ color: previewModal.isPdf ? '#f56c6c' : '#7c6ff7' }">
                {{ previewModal.isPdf ? 'picture_as_pdf' : 'text_snippet' }}
              </span>
              {{ previewModal.fileName }}
            </h2>
            <button class="btn-close-circle" @click="closePreview">
              <span class="material-icons">close</span>
            </button>
          </div>

          <div class="preview-body custom-scrollbar">
            <div v-if="previewModal.loading" class="preview-loading">
              <div class="btn-spinner" style="border-top-color: #7c6ff7; width: 40px; height: 40px; opacity: 1; position: relative;"></div>
              <p>正在解密并传输数据流...</p >
            </div>

            <iframe
                v-else-if="previewModal.isPdf && previewModal.url"
                :src="previewModal.url"
                class="pdf-viewer"
                frameborder="0">
            </iframe>

            <pre v-else-if="!previewModal.isPdf" class="txt-viewer custom-scrollbar">{{ previewModal.textContent }}</pre>
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
const route = useRoute();
const notebookId = route.params.notebookId;

const fileInput = ref(null);
const documents = ref([]);
const isUploading = ref(false);
const chatWindow = ref(null);
const toast = ref({ show: false, message: '', type: 'success' });
let toastTimer = null;
const userQuery = ref('');
const isChatting = ref(false);
const chatInputRef = ref(null);
const sourceModal = ref({ show: false, fileName: '', content: '' });

const deleteDocConfirm = ref({ show: false, id: null });
const triggerDeleteDoc = (id) => { deleteDocConfirm.value = { show: true, id }; };

const chatHistory = ref([]);
const goBack = () => router.push('/notebooks');
const triggerUpload = () => fileInput.value.click();

// ==================== 【新增：文件预览核心状态与逻辑】 ====================
const previewModal = ref({
  show: false,
  fileName: '',
  url: '',
  isPdf: false,
  textContent: '',
  loading: false
});

const openPreview = async (doc) => {
  const isPdf = doc.fileName.toLowerCase().endsWith('.pdf');
  previewModal.value = {
    show: true,
    fileName: doc.fileName,
    url: '',
    isPdf: isPdf,
    textContent: '',
    loading: true
  };

  try {
    // 关键：针对不同文件类型，要求 Axios 返回不同的数据格式
    const responseType = isPdf ? 'blob' : 'text';

    // 发起请求获取文件流
    const res = await request.get(`/api/documents/${doc.id}/preview`, {
      responseType: responseType
    });

    if (isPdf) {
      // 将 Blob 转换为浏览器本地的临时 URL，供 iframe 渲染
      const blob = new Blob([res], { type: 'application/pdf' });
      previewModal.value.url = URL.createObjectURL(blob);
    } else {
      // TXT 文件直接读取文本
      previewModal.value.textContent = res;
    }
  } catch (error) {
    console.error("预览文件失败", error);
    showToast('文件读取失败或已被物理损坏', 'error');
    previewModal.value.show = false;
  } finally {
    previewModal.value.loading = false;
  }
};

const closePreview = () => {
  // 释放内存中的 Blob URL
  if (previewModal.value.url) {
    URL.revokeObjectURL(previewModal.value.url);
  }
  previewModal.value.show = false;
};
// =========================================================================

const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer);
  toast.value = { show: true, message, type };
  toastTimer = setTimeout(() => { toast.value.show = false; }, 3000);
};

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

const showSource = (sources, id) => {
  if (!sources || sources.length === 0) {
    showToast('当前回答没有关联的本地知识库来源', 'error');
    return;
  }
  let source = sources.find(s => s.id === id);
  if (!source && sources.length > 0) source = sources[0];
  if (source) {
    sourceModal.value = { show: true, fileName: source.fileName, content: source.content };
  } else {
    showToast('溯源信息丢失', 'error');
  }
};

const cleanFileName = (fileName) => {
  if (!fileName) return '未知文件';
  return fileName.replace(/^\d+_/, '');
};

const executeDeleteDoc = async () => {
  try {
    await request.delete(`/api/documents/${deleteDocConfirm.value.id}`);
    deleteDocConfirm.value.show = false;
    showToast('节点资料已成功销毁', 'success');
    fetchDocuments();
  } catch (error) {
    showToast('销毁失败，请稍后重试', 'error');
  }
};

const scrollToBottom = async () => {
  await nextTick();
  if (chatWindow.value) {
    chatWindow.value.scrollTop = chatWindow.value.scrollHeight;
  }
};

let pollingTimer = null;

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

const fetchChatHistory = async () => {
  try {
    const history = await request.get(`/api/chat/${notebookId}`);
    if (history && history.length > 0) {
      chatHistory.value = history.map(msg => ({
        role: msg.role,
        text: msg.content,
        sources: msg.sources ? JSON.parse(msg.sources) : [],
        isExpanded: false
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
  if (successCount > 0) {
    showToast(`${successCount} 个文件已加入神经元解析队列！`, 'success');
    await fetchDocuments();
  }
  isUploading.value = false;
  event.target.value = '';
};

const handleChat = async () => {
  if (!userQuery.value.trim() || isChatting.value) return;
  const currentQuestion = userQuery.value;
  chatHistory.value.push({ role: 'user', text: currentQuestion, sources: [] });
  userQuery.value = '';
  isChatting.value = true;
  await nextTick(() => { if (chatInputRef.value) chatInputRef.value.style.height = '24px'; });
  await scrollToBottom();
  try {
    const res = await request.post('/api/chat', { notebookId: parseInt(notebookId), query: currentQuestion });
    chatHistory.value.push({ role: 'ai', text: res.answer, sources: res.sources || [], isExpanded: false });
  } catch (error) {
    chatHistory.value.push({ role: 'ai', text: '❌ 抱歉，大脑暂时断连，请检查底层服务器。', sources: [], isExpanded: false });
  } finally {
    isChatting.value = false;
    await scrollToBottom();
    await nextTick(() => { if (chatInputRef.value) chatInputRef.value.focus(); });
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
  fetchChatHistory();
});
</script>

<style scoped>
/* 一行一类名：超高密度压缩 CSS */
.glass-app-container { min-height: 100vh; padding: 40px 60px; background: transparent; color: #e8e8f0; font-family: 'Inter', system-ui, sans-serif; box-sizing: border-box; }
.nebula-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.nebula-title-main { font-size: 1.8rem; font-weight: 800; margin: 0; display: flex; align-items: center; gap: 10px; color: #fff; text-shadow: 0 0 20px rgba(124, 111, 247, 0.3); }
.tech-icon { color: #7c6ff7; font-size: 28px; }
.nebula-subtitle { color: #6b6b85; font-size: 0.9rem; margin: 6px 0 0; }
.btn-nebula-outline { background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.1); color: #e8e8f0; border-radius: 12px; padding: 10px 18px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: 0.3s; }
.btn-nebula-outline:hover { background: rgba(255,255,255,0.08); border-color: #7c6ff7; }
.copilot-layout { display: grid; grid-template-columns: 340px 1fr; gap: 24px; height: calc(100vh - 140px); }
.glass-panel { background: rgba(18,18,36,0.65); backdrop-filter: blur(40px) saturate(140%); border: 1px solid rgba(255,255,255,0.08); border-radius: 20px; padding: 24px; box-shadow: 0 16px 40px rgba(0,0,0,0.4); display: flex; flex-direction: column; overflow: hidden; }
.section-header { margin-bottom: 20px; border-bottom: 1px solid rgba(255,255,255,0.05); padding-bottom: 14px; }
.section-header h2 { font-size: 1.15rem; color: #fff; margin: 0 0 6px; display: flex; align-items: center; gap: 8px; }
.section-header p { font-size: 0.85rem; color: #6b6b85; margin: 0; }
.upload-area { margin-bottom: 16px; }
.btn-upload { width: 100%; background: rgba(124, 111, 247, 0.08); border: 1.5px dashed rgba(124, 111, 247, 0.4); color: #a99df9; padding: 14px; border-radius: 14px; font-weight: 600; cursor: pointer; transition: 0.3s; display: flex; justify-content: center; align-items: center; gap: 8px; }
.btn-upload:hover:not(:disabled) { background: rgba(124, 111, 247, 0.2); border-color: #7c6ff7; color: #fff; box-shadow: 0 0 16px rgba(124, 111, 247, 0.2); }
.btn-upload:disabled { opacity: 0.5; cursor: not-allowed; }
.doc-list { list-style: none; padding: 0; margin: 0; overflow-y: auto; flex: 1; padding-right: 6px; display: flex; flex-direction: column; gap: 12px; }
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 4px; }
.doc-item { background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.05); border-radius: 14px; padding: 14px; display: flex; align-items: center; gap: 12px; transition: 0.3s; position: relative; overflow: hidden; }
.doc-item:hover { background: rgba(255,255,255,0.06); border-color: rgba(124,111,247,0.3); }
.doc-item::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; background: #7c6ff7; opacity: 0; transition: 0.3s; }
.doc-item:hover::before { opacity: 1; }
.doc-icon { color: #a99df9; display: flex; align-items: center; }
.doc-info { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.doc-name { font-size: 0.9rem; font-weight: 600; color: #e8e8f0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.doc-status { font-size: 0.75rem; color: #4ade80; display: flex; align-items: center; gap: 4px; margin-top: 4px; }
.doc-status.processing { color: #f59e0b; }
.status-icon { font-size: 14px; }
.btn-icon-danger { background: transparent; border: none; color: #6b6b85; padding: 6px; cursor: pointer; border-radius: 8px; transition: 0.3s; display: flex; align-items: center; justify-content: center; }
.btn-icon-danger:hover { background: rgba(245,108,108,0.1); color: #f56c6c; }
.empty-state { text-align: center; color: #6b6b85; font-size: 0.85rem; margin-top: 40px; display: flex; flex-direction: column; align-items: center; }
.chat-window { flex: 1; overflow-y: auto; padding-right: 12px; display: flex; flex-direction: column; gap: 24px; margin-bottom: 20px; }
.chat-bubble-wrapper { display: flex; width: 100%; }
.chat-bubble-wrapper.ai { justify-content: flex-start; }
.chat-bubble-wrapper.user { justify-content: flex-end; }
.chat-bubble-inner { display: flex; align-items: flex-end; gap: 12px; max-width: 85%; }
.ai-avatar, .user-avatar { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; background: rgba(124,111,247,0.2); border: 1px solid rgba(124,111,247,0.4); border-radius: 50%; flex-shrink: 0; font-size: 16px; }
.user-avatar { background: rgba(74,222,128,0.2); border-color: rgba(74,222,128,0.4); }
.chat-bubble { padding: 14px 18px; font-size: 0.95rem; line-height: 1.6; border-radius: 16px; position: relative; word-break: break-word; }
.ai .chat-bubble { background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.08); color: #e8e8f0; border-bottom-left-radius: 4px; }
.user .chat-bubble { background: linear-gradient(135deg, #7c6ff7, #5b4fcf); color: #fff; border-bottom-right-radius: 4px; box-shadow: 0 4px 15px rgba(124, 111, 247, 0.3); }
.typing-glow { animation: pulseGlow 1.5s infinite; color: #a99df9 !important; }
@keyframes pulseGlow { 0%, 100% { opacity: 0.6; } 50% { opacity: 1; } }
.chat-input-wrapper { display: flex; align-items: flex-end; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.1); border-radius: 16px; padding: 10px 10px 10px 16px; gap: 12px; transition: 0.3s; }
.chat-input-wrapper:focus-within { border-color: #7c6ff7; background: rgba(18,18,36,0.8); box-shadow: 0 0 0 3px rgba(124,111,247,0.15); }
.chat-input { flex: 1; background: transparent; border: none; color: #e8e8f0; font-size: 0.95rem; resize: none; max-height: 120px; outline: none; padding: 8px 0; line-height: 1.5; font-family: inherit; }
.chat-input::placeholder { color: #6b6b85; }
.btn-send { background: #7c6ff7; color: #fff; border: none; border-radius: 12px; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: 0.3s; flex-shrink: 0; }
.btn-send:hover:not(:disabled) { background: #6b5fdf; box-shadow: 0 4px 15px rgba(124,111,247,0.4); transform: translateY(-2px); }
.btn-send:disabled { background: rgba(255,255,255,0.08); color: #6b6b85; cursor: not-allowed; }
.citation-badge { display: inline-flex; justify-content: center; align-items: center; background: rgba(124,111,247,0.2); color: #a99df9; border: 1px solid rgba(124,111,247,0.4); font-size: 0.75rem; font-weight: 700; width: 22px; height: 22px; border-radius: 50%; margin: 0 4px; cursor: pointer; transition: 0.3s; transform: translateY(-2px); }
.citation-badge:hover { background: #7c6ff7; color: #fff; box-shadow: 0 0 10px rgba(124,111,247,0.5); transform: translateY(-4px) scale(1.1); }
.toast-wrapper { position: fixed; top: 28px; left: 50%; transform: translateX(-50%); padding: 12px 24px; border-radius: 50px; display: flex; align-items: center; gap: 8px; font-size: 0.9rem; font-weight: 600; z-index: 9999; backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.1); box-shadow: 0 12px 40px rgba(0,0,0,0.5); background: rgba(18,18,36,0.85); }
.toast-success { border-color: rgba(74,222,128,0.4); color: #4ade80; }
.toast-error { border-color: rgba(245,108,108,0.4); color: #f56c6c; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translate(-50%, -20px) scale(0.95); }
.nebula-modal-overlay { position: fixed; inset: 0; background: rgba(6,6,15,0.7); backdrop-filter: blur(10px); display: flex; justify-content: center; align-items: center; z-index: 9999; }
.nebula-modal-content { background: rgba(18,18,36,0.85); backdrop-filter: blur(40px) saturate(140%); border: 1px solid rgba(255,255,255,0.1); border-radius: 24px; padding: 32px; box-shadow: 0 25px 80px rgba(0,0,0,0.6); max-width: 450px; width: 100%; }
.danger-modal { text-align: center; }
.danger-icon-wrapper { width: 64px; height: 64px; background: rgba(245,108,108,0.1); color: #f56c6c; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; font-size: 32px; border: 1px solid rgba(245,108,108,0.2); }
.warning-text { color: #9898b4; font-size: 0.9rem; line-height: 1.6; margin-bottom: 24px; }
.modal-footer { display: flex; justify-content: center; gap: 12px; }
.btn-nebula-danger { background: linear-gradient(135deg, #f56c6c, #c53030); color: #fff; border: none; border-radius: 12px; padding: 10px 24px; font-weight: 600; cursor: pointer; transition: 0.3s; }
.btn-nebula-danger:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(245,108,108,0.3); }
.source-modal { max-width: 600px; width: 100%; }
.source-header { display: flex; align-items: center; gap: 10px; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 16px; margin-bottom: 20px; }
.source-header h3 { margin: 0; font-size: 1.1rem; color: #fff; }
.source-body { background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.05); border-radius: 12px; padding: 20px; font-size: 0.95rem; color: #e8e8f0; line-height: 1.8; max-height: 40vh; overflow-y: auto; white-space: pre-wrap; }
.modal-fade-enter-active, .modal-fade-leave-active { transition: 0.3s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: translateY(20px) scale(0.95); }

/* ================== 新增：文件预览相关的 UI 样式 ================== */

/* 让文档列表项看起来可点击 */
.doc-item {
  cursor: pointer; /* 增加鼠标手型 */
}
.doc-item:hover {
  transform: translateY(-2px); /* 点击前轻微上浮提示交互 */
}

/* 巨型预览窗 */
.preview-modal-lg {
  max-width: 900px !important; /* 突破原有 modal 的限制 */
  width: 90vw;
  height: 85vh;
  display: flex;
  flex-direction: column;
  padding: 24px 32px 32px 32px;
}

.preview-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 1.25rem !important;
  color: #e8e8f0;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-body {
  flex: 1;
  background: rgba(6, 6, 15, 0.4); /* 深底色以突显文件内容 */
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  margin-top: 16px;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
}

.pdf-viewer {
  width: 100%;
  height: 100%;
  background: #fff; /* PDF 背景通常为白，避免黑色底导致自带的阅读器突兀 */
}

.txt-viewer {
  margin: 0;
  padding: 24px;
  width: 100%;
  height: 100%;
  overflow-y: auto;
  color: #e8e8f0;
  font-family: 'SF Mono', ui-monospace, Consolas, monospace;
  font-size: 0.95rem;
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #a99df9;
  gap: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}

/* 覆盖之前可能存在的 .btn-close-circle 冲突，让其在预览窗完美居右 */
.preview-modal-lg .btn-close-circle {
  background: rgba(255, 255, 255, 0.05);
  border: none;
  color: #9898b4;
  border-radius: 50%;
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}
.preview-modal-lg .btn-close-circle:hover {
  background: rgba(245, 108, 108, 0.15);
  color: #f56c6c;
  transform: rotate(90deg) scale(1.1);
}
</style>