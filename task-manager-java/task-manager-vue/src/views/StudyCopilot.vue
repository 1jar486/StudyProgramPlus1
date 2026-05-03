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
        <h1>🧠 专属备考外脑 (StudyProgramPlus)</h1>
        <p class="subtitle">让每一份资料都能开口说话</p >
      </div>
      <div class="header-right">
        <button @click="goBack" class="btn-back">
          <span class="material-icons" style="font-size: 16px; margin-right: 4px;">arrow_back</span>
          返回任务看板
        </button>
      </div>
    </header>

    <div class="copilot-layout">
      <!-- 左侧：知识库管理 -->
      <div class="library-section glass-card">
        <div class="section-title">
          <span class="material-icons" style="color: #34D399; margin-right: 8px;">library_books</span>
          <h2>我的知识库</h2>
        </div>
        <p class="section-desc">上传资料，构建你的专属备考大脑</p >

        <div class="upload-area">
          <input type="file" ref="fileInput" @change="handleFileUpload" accept=".pdf,.doc,.docx,.txt,.xls,.xlsx,.csv" style="display: none;" />
          <button class="btn-upload" @click="triggerUpload" :disabled="isUploading">
            <span class="material-icons" style="margin-right: 6px;">cloud_upload</span>
            {{ isUploading ? '资料传送中...' : '上传新复习资料 (PDF/Word)' }}
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
              <span class="material-icons action-icon">more_horiz</span>
              <span class="material-icons delete-icon" @click.stop="deleteDoc(doc.id)">delete_outline</span>
            </div>
          </li>
          <div v-if="documents.length === 0" class="empty-state">
            还没有上传任何复习资料哦~
          </div>
        </ul>
      </div>

      <!-- 右侧：AI 问答区 -->
      <div class="chat-section glass-card">
        <div class="section-title">
          <span class="material-icons" style="color: #60A5FA; margin-right: 8px;">auto_awesome</span>
          <h2>AI 导师对话</h2>
        </div>
        <p class="section-desc">你的专属学习助理，随时为你答疑解惑</p >

        <div class="chat-window" ref="chatWindow">
          <!-- 历史对话渲染 -->
          <div v-for="(msg, index) in chatHistory" :key="index" :class="['chat-bubble-wrapper', msg.role]">
            <div class="chat-bubble">
              <span v-if="msg.role === 'ai'" class="ai-avatar">👋</span>
              <div class="bubble-content" style="white-space: pre-wrap;">{{ msg.text }}</div>
            </div>
          </div>
          <div v-if="isChatting" class="chat-bubble-wrapper ai">
            <div class="chat-bubble"><span class="ai-avatar">✨</span>正在飞速思考中...</div>
          </div>
        </div>

        <!-- 底部输入框 -->
        <div class="chat-input-wrapper">
          <textarea
              class="chat-input custom-scrollbar"
              v-model="userQuery"
              placeholder="提问：帮我总结资料，或粘贴长段英语阅读让我分析..."
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
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';

const router = useRouter();
const fileInput = ref(null);
const documents = ref([]);
const isUploading = ref(false);
const chatWindow = ref(null);
const toast = ref({ show: false, message: '', type: 'success' }); // 定义toast的状态
let toastTimer = null;
const userQuery = ref('');
const isChatting = ref(false);

const chatInputRef = ref(null)

const chatHistory = ref([
  { role: 'ai', text: '你好！我是你的 StudyProgramPlus 专属学习助理。\n\n请在左侧上传你的复习资料，然后随时向我提问。' }
]);

const goBack = () => router.push('/tasks');
const triggerUpload = () => fileInput.value.click();

const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer); // 如果连按，重置定时器
  toast.value = { show: true, message, type };
  // 3 秒后自动消失
  toastTimer = setTimeout(() => {
    toast.value.show = false;
  }, 3000);
};

// 自动滚动到聊天底部
const scrollToBottom = async () => {
  await nextTick();
  if (chatWindow.value) {
    chatWindow.value.scrollTop = chatWindow.value.scrollHeight;
  }
};

// 声明一个定时器
let pollingTimer = null;

const fetchDocuments = async () => {
  try {
    documents.value = await request.get('/api/documents');

    // 检查列表中是否还有处于 "PROCESSING" (待解析) 状态的文件
    const hasProcessing = documents.value.some(doc => doc.status === 'PROCESSING');

    // 如果有没解析完的，且定时器还没开，就启动一个每 2 秒刷新的定时器
    if (hasProcessing && !pollingTimer) {
      pollingTimer = setInterval(() => {
        fetchDocuments(); // 偷偷刷新
      }, 2000);
    }
    // 如果全都解析完了（或者全是成功/失败），就关掉定时器，节省性能
    else if (!hasProcessing && pollingTimer) {
      clearInterval(pollingTimer);
      pollingTimer = null;
    }
  } catch (error) {
    console.error("获取知识库失败", error);
  }
};

// 当用户离开这个页面时，一定要记得销毁定时器，防止内存泄漏
onUnmounted(() => {
  if (pollingTimer) clearInterval(pollingTimer);
});

const handleFileUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  // --- 【新增校验逻辑 1：空文件拦截】 ---
  if (file.size === 0) {
    showToast('抱歉，不能上传空文件哦！', 'error');
    event.target.value = ''; // 清空选择器缓存
    return;
  }

  // --- 【新增校验逻辑 2：重复文件拦截】 ---
  // 判断当前页面的 documents 列表里，有没有和准备上传的文件同名的
  const isDuplicate = documents.value.some(doc => doc.fileName === file.name);
  if (isDuplicate) {
    showToast('该文件已在知识库中，请勿重复上传！', 'error');
    event.target.value = ''; // 清空选择器缓存
    return;
  }

  const formData = new FormData();
  formData.append('file', file);

  isUploading.value = true;
  try {
    await request.post('/api/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    showToast('资料上传成功，正在后台解析...', 'success');
    fetchDocuments();
  } catch (error) {
    showToast('文件上传失败，请检查后端服务', 'error');
  } finally {
    isUploading.value = false;
    event.target.value = '';
  }
};

const deleteDoc = async (id) => {
  if (!confirm("确定要删除这条资料吗？")) return; // 这里保留 confirm，因为删除是危险操作需要二次确认
  try {
    await request.delete(`/api/documents/${id}`);
    showToast('资料已成功删除', 'success');
    fetchDocuments();
  } catch (error) {
    showToast('删除失败，请稍后重试', 'error');
  }
};

const handleChat = async () => {
  if (!userQuery.value.trim() || isChatting.value) return;

  const currentQuestion = userQuery.value;
    chatHistory.value.push({ role: 'user', text: currentQuestion });
    userQuery.value = '';

    // 【新增】消息发出去后，把输入框的高度强行缩回默认的一行高度 (约24px)
    nextTick(() => {
      if (chatInputRef.value) {
        chatInputRef.value.style.height = '24px';
      }
    });

  try {
    const res = await request.post('http://localhost:8000/api/ai/chat', {
      query: currentQuestion
    });
    chatHistory.value.push({ role: 'ai', text: res.answer });
  } catch (error) {
    chatHistory.value.push({ role: 'ai', text: '❌ 抱歉，大脑暂时断连，请检查 Python 引擎是否启动。' });
  } finally {
    isChatting.value = false;
    scrollToBottom();
    // 延迟一帧，等待输入框解除 disabled 状态后重新聚焦
    nextTick(() => {
      if (chatInputRef.value) chatInputRef.value.focus();
    });
  }
};

// 监听键盘按键：如果是单独按 Enter 且没有按 Shift，则发送消息
const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault(); // 阻止默认的换行行为
    handleChat();
  }
};

// 动态调整输入框高度
const autoResize = () => {
  const textarea = chatInputRef.value;
  if (!textarea) return;

  // 第一步：先将高度重置为 auto，这样在删除文字时输入框才能缩回去
  textarea.style.height = 'auto';
  // 第二步：获取内部文字的真实高度，并赋值给输入框
  textarea.style.height = textarea.scrollHeight + 'px';
};


onMounted(() => {
  fetchDocuments();
});
</script>

<style scoped>
/* 全局背景图：模拟设计图中的渐变与植物剪影氛围 */
.glass-app-container {
  min-height: 100vh;
  padding: 30px 40px;
  background: linear-gradient(135deg, #F0FDF4 0%, #E0F2FE 100%);
  background-image: radial-gradient(at 80% 80%, rgba(209, 250, 229, 0.7) 0, transparent 50%),
  radial-gradient(at 10% 20%, rgba(224, 242, 254, 0.7) 0, transparent 50%);
  font-family: system-ui, -apple-system, sans-serif;
  box-sizing: border-box;
}

/* 顶部 Header */
.glass-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.glass-header h1 {
  font-size: 24px;
  color: #1F2937;
  margin: 0 0 4px 0;
  font-weight: 700;
}
.subtitle {
  color: #6B7280;
  font-size: 14px;
  margin: 0;
}
.btn-back {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.9);
  padding: 8px 16px;
  border-radius: 99px;
  color: #4B5563;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
  transition: all 0.3s ease;
}
.btn-back:hover {
  background: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

/* 主体布局 */
.copilot-layout {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 24px;
  height: calc(100vh - 120px);
}

/* 毛玻璃卡片公用样式 */
.glass-card {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.03);
  padding: 24px;
  display: flex;
  flex-direction: column;
}

/* 卡片标题区 */
.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}
.section-title h2 {
  font-size: 18px;
  color: #111827;
  margin: 0;
  font-weight: 600;
}
.section-desc {
  color: #9CA3AF;
  font-size: 13px;
  margin: 0 0 20px 0;
}

/* 左侧：上传按钮 */
.btn-upload {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #D1FAE5;
  color: #059669;
  border: none;
  padding: 14px;
  border-radius: 16px;
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}
.btn-upload:hover {
  background: #A7F3D0;
  transform: translateY(-1px);
}

/* 左侧：文档列表 */
.doc-list {
  list-style: none;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  flex: 1;
  padding-right: 4px;
}
.doc-list::-webkit-scrollbar { width: 4px; }
.doc-list::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 4px; }

.doc-card {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.9);
  padding: 16px;
  border-radius: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(255, 255, 255, 1);
  transition: all 0.2s;
  position: relative;
}
.doc-card:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.04);
  transform: translateY(-2px);
}
.doc-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 15%;
  height: 70%;
  width: 3px;
  background: #34D399;
  border-radius: 0 4px 4px 0;
}
.doc-info {
  flex: 1;
  margin-left: 12px;
  display: flex;
  flex-direction: column;
}
.doc-name {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
  word-break: break-all;
}
.doc-status {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #10B981;
}
.doc-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #9CA3AF;
}
.action-icon { font-size: 20px; cursor: pointer; }
.delete-icon {
  font-size: 20px;
  cursor: pointer;
  color: #FCA5A5;
  transition: 0.2s;
}
.delete-icon:hover { color: #EF4444; }
.empty-state {
  text-align: center;
  color: #9CA3AF;
  font-size: 14px;
  margin-top: 40px;
}

/* 右侧：聊天区域 */
.chat-window {
  flex: 1;
  overflow-y: auto;
  padding: 10px 20px 20px 0;
  display: flex;
  flex-direction: column;
}
.chat-window::-webkit-scrollbar { width: 4px; }
.chat-window::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 4px; }

.chat-bubble-wrapper {
  display: flex;
  margin-bottom: 24px;
  width: 100%;
}
.chat-bubble-wrapper.ai { justify-content: flex-start; }
.chat-bubble-wrapper.user { justify-content: flex-end; }

.chat-bubble {
  display: flex;
  align-items: flex-start;
  max-width: 80%;
  padding: 16px 20px;
  font-size: 15px;
  line-height: 1.6;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.ai .chat-bubble {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255,255,255,1);
  color: #374151;
  border-radius: 4px 20px 20px 20px;
}
.user .chat-bubble {
  background: #34D399;
  color: #ffffff;
  border-radius: 20px 4px 20px 20px;
}
.ai-avatar {
  margin-right: 8px;
  font-size: 18px;
}

/* 右侧：胶囊输入框 */
.chat-input-wrapper {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 1);
  border-radius: 99px;
  padding: 6px 6px 6px 20px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
  margin-top: 10px;
}
.chat-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 15px;
  color: #374151;
  resize: none; /* 禁用手动拖拽大小 */
  padding-top: 2px;
  line-height: 1.5;
  max-height: 120px; /* 限制最高高度，防止文字太多撑爆屏幕 */
  font-family: inherit;
}
.chat-input::placeholder { color: #9CA3AF; }

.btn-send {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #34D399;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 99px;
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-send:hover:not(:disabled) {
  background: #10B981;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}
.btn-send:disabled {
  background: #D1D5DB;
  cursor: not-allowed;
}

/* =========================
   Toast 轻提示样式 & 动画
   ========================= */
.toast-wrapper {
  position: fixed;
  top: 32px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: 99px; /* 胶囊形状 */
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
  z-index: 9999;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

/* 成功状态：毛玻璃薄荷绿 */
.toast-success {
  background: rgba(16, 185, 129, 0.85);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* 失败状态：毛玻璃珊瑚红 */
.toast-error {
  background: rgba(239, 68, 68, 0.85);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* Vue 的 Transition 过渡动画：实现丝滑的下滑淡入、上滑淡出 */
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); /* 带有轻微弹性的动画曲线 */
}
.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -20px) scale(0.95);
}
</style>