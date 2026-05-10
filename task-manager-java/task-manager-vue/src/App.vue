<template>
  <div class="nebula-app-container">

    <main class="nebula-main-viewport custom-scrollbar">
      <router-view />
    </main>

    <transition name="sidebar-slide">
      <aside class="nebula-notion-sidebar" v-show="showChat">
        <div class="chat-header">
          <span class="title">
            <span class="material-icons" style="font-size: 18px; color: #7c6ff7;">
              {{ chatTargetId === 999 ? 'public' : 'library_books' }}
            </span>
            {{ chatTargetId === 999 ? '🤖 智能机器黑' : `🧠 专属外脑 (${chatTargetName})` }}
          </span>
          <button class="btn-close" @click="showChat = false" title="收起面板"><span class="material-icons">close</span></button>
        </div>

        <div class="chat-body custom-scrollbar" ref="chatBodyRef">
          <div v-for="(msg, index) in chatHistory" :key="index" :class="['chat-bubble-wrapper', msg.role]">
            <div class="chat-bubble">
              <img v-if="msg.role === 'ai'" src="/pet/小黑.jpg" class="ai-avatar-img" alt="小黑" />
              {{ msg.text }}
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <div class="nebula-input-wrapper">
            <input type="text" class="nebula-input" v-model="userMessage" placeholder="呼叫小黑..." @keyup.enter="sendMessage" />
            <button class="btn-send" @click="sendMessage">发送</button>
            <div class="focus-line"></div>
          </div>
        </div>
      </aside>
    </transition>

  </div>

  <div v-if="menuState > 0" class="nebula-overlay" @click="menuState = 0"></div>

  <div class="global-pet-wrapper" ref="petRef" :style="{ left: petX + 'px', top: petY + 'px' }" @mousedown="startDrag" @touchstart="startDragTouch">

    <img :src="currentDisplaySrc" alt="罗小黑" draggable="false" crossorigin="anonymous" @load="extractStaticFrame"/>

    <transition name="menu-fade">
      <div v-if="menuState > 0" class="nebula-list-menu glass-panel" @click.stop @mousedown.stop @touchstart.stop>

        <div v-if="menuState === 1" class="menu-container">
          <div class="menu-header">中枢控制面板</div>
          <div class="menu-body custom-scrollbar">

            <div class="menu-item primary-item" @click="openChat(999)">
              <span class="material-icons">public</span> 全局学习助手
            </div>

            <div v-if="isNotebookMode" class="menu-item current-item" @click="openChat(currentNotebookId)">
              <span class="material-icons">library_books</span> 当前: {{ currentNotebookName }}
            </div>

            <div class="menu-divider"></div>
            <div class="menu-subtitle">最近思绪空间</div>

            <div class="menu-item" v-for="nb in recentNotebooks" :key="nb.id" @click="navigateToNotebook(nb.id)">
              <span class="material-icons" style="font-size: 16px;">book</span> {{ nb.name }}
            </div>
            <div v-if="recentNotebooks.length === 0" class="empty-text">暂无记录</div>

            <div class="menu-divider"></div>

            <div class="menu-item action-item" @click="openCreateModal">
              <span class="material-icons">add_circle_outline</span> 新建思绪空间
            </div>

            <div class="menu-item" @click="menuState = 2">
              <span class="material-icons">pets</span> 宠物动作指令
            </div>
          </div>
        </div>

        <div v-if="menuState === 2" class="menu-container">
          <div class="menu-header with-back">
            <span class="material-icons btn-back" @click="menuState = 1">chevron_left</span>
            动作库
          </div>
          <div class="menu-body custom-scrollbar">
            <div class="menu-item" v-for="action in actionData" :key="action.id" @click.stop="triggerAction(action.id)">
              <span class="emoji-icon">{{ action.icon }}</span> {{ action.name }}
            </div>
          </div>
        </div>

      </div>
    </transition>
  </div>

  <transition name="modal-fade">
    <div class="nebula-modal-overlay" v-if="showCreateModal" @click.self="showCreateModal = false">
      <div class="nebula-modal-content glass-panel">
        <div class="modal-header">
          <h2>新建思绪空间</h2>
          <button class="btn-close" @click="showCreateModal = false"><span class="material-icons">close</span></button>
        </div>
        <div class="modal-body">
          <div class="nebula-input-wrapper" style="margin-bottom: 0;">
            <input v-model="newNotebookName" type="text" class="nebula-input" placeholder="输入课题或科目名称..." @keyup.enter="handleCreateNotebook" autofocus />
            <div class="focus-line"></div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-nebula-outline" @click="showCreateModal = false">取消</button>
          <button class="btn-nebula-primary" @click="handleCreateNotebook" :disabled="!newNotebookName.trim()">跃迁构建</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from './utils/request';

const route = useRoute();
const router = useRouter();

// =======================
// 1. 状态与配置管理
// =======================
const menuState = ref(0);
const recentNotebooks = ref([]);
const showCreateModal = ref(false);
const newNotebookName = ref('');

const actionData = [
  { id: 1, name: '趴', icon: '' },
  { id: 2, name: '听歌', icon: '' },
  { id: 3, name: '吃西瓜', icon: '' },
  { id: 4, name: '翻滚', icon: '' },
  { id: 5, name: '奔跑', icon: '' },
  { id: 6, name: '洗脸', icon: '' }
];

const currentNotebookId = computed(() => route.params.notebookId ? parseInt(route.params.notebookId) : null);
const isNotebookMode = computed(() => !!currentNotebookId.value);
const currentNotebookName = ref('');

watch(() => route.params.notebookId, async (newId) => {
  if (newId) {
    try {
      const nbs = await request.get('/api/notebooks');
      const nb = nbs.find(n => n.id == newId);
      currentNotebookName.value = nb ? nb.name : `笔记本 #${newId}`;
    } catch (e) { currentNotebookName.value = `获取中...`; }
  }
}, { immediate: true });

// =======================
// 2. 菜单与弹窗业务逻辑
// =======================
const fetchRecentNotebooks = async () => {
  try {
    const res = await request.get('/api/notebooks');
    recentNotebooks.value = res.slice(0, 3);
  } catch (error) {
    console.error("无法获取最近笔记本");
  }
};

const openMenu = async () => {
  if (showChat.value) {
    // 再次点击宠物，如果聊天框开着，就关掉它
    showChat.value = false;
  } else {
    menuState.value = 1;
    await fetchRecentNotebooks();
  }
};

const navigateToNotebook = (id) => {
  menuState.value = 0;
  router.push(`/copilot/${id}`);
};

const openCreateModal = () => {
  menuState.value = 0;
  showCreateModal.value = true;
};

const handleCreateNotebook = async () => {
  if (!newNotebookName.value.trim()) return;
  try {
    const res = await request.post('/api/notebooks', { name: newNotebookName.value });
    newNotebookName.value = '';
    showCreateModal.value = false;
    if(res && res.id) router.push(`/copilot/${res.id}`);
  } catch (error) {
    alert("创建失败，请检查网络或登录状态");
  }
};

// =======================
// 3. 动画控制：绝对手动挡 (零冲突版)
// =======================
const currentAction = ref(4);
const currentDisplaySrc = ref('/pet/4.gif');
const staticImages = ref({});
let animTimer = null;

const extractStaticFrame = (e) => {
  const img = e.target;
  if (img.src.includes('.gif') && !staticImages.value[currentAction.value]) {
    try {
      const canvas = document.createElement('canvas');
      canvas.width = img.naturalWidth || 100; canvas.height = img.naturalHeight || 100;
      canvas.getContext('2d').drawImage(img, 0, 0);
      staticImages.value[currentAction.value] = canvas.toDataURL('image/png');
    } catch (err) {}
  }
};

const triggerAction = (id) => {
  menuState.value = 0;
  currentAction.value = id;

  if (animTimer) {
    clearTimeout(animTimer);
    animTimer = null;
  }

  // 强制附带时间戳打断浏览器底层缓存，实现点击必动
  currentDisplaySrc.value = `/pet/${id}.gif?t=${Date.now()}`;

  animTimer = setTimeout(() => {
    currentDisplaySrc.value = staticImages.value[id] || `/pet/${id}.gif`;
    animTimer = null;
  }, 3500);
};

const playAnimation = () => {
  triggerAction(currentAction.value);
};

// =======================
// 4. 拖拽交互 (原生逻辑不变)
// =======================
const petRef = ref(null);
const petX = ref(window.innerWidth - 180);
const petY = ref(window.innerHeight - 180);
let isDragging = false, startMouseX = 0, startMouseY = 0, startPetX = 0, startPetY = 0;

const startDrag = (e) => {
  isDragging = false; startMouseX = e.clientX; startMouseY = e.clientY;
  startPetX = petX.value; startPetY = petY.value;
  document.addEventListener('mousemove', doDrag); document.addEventListener('mouseup', stopDrag);
};
const doDrag = (e) => {
  const dx = e.clientX - startMouseX, dy = e.clientY - startMouseY;
  if (Math.abs(dx) > 5 || Math.abs(dy) > 5) isDragging = true;
  if (isDragging) {
    const maxX = window.innerWidth - (petRef.value?.clientWidth || 100);
    const maxY = window.innerHeight - (petRef.value?.clientHeight || 100);
    petX.value = Math.max(0, Math.min(startPetX + dx, maxX));
    petY.value = Math.max(0, Math.min(startPetY + dy, maxY));
  }
};
const stopDrag = () => {
  document.removeEventListener('mousemove', doDrag); document.removeEventListener('mouseup', stopDrag);
  if (!isDragging) openMenu();
};

const startDragTouch = (e) => {
  isDragging = false; startMouseX = e.touches[0].clientX; startMouseY = e.touches[0].clientY;
  startPetX = petX.value; startPetY = petY.value;
  document.addEventListener('touchmove', doDragTouch, { passive: false }); document.addEventListener('touchend', stopDragTouch);
};
const doDragTouch = (e) => {
  e.preventDefault();
  const dx = e.touches[0].clientX - startMouseX, dy = e.touches[0].clientY - startMouseY;
  if (Math.abs(dx) > 5 || Math.abs(dy) > 5) isDragging = true;
  if (isDragging) { petX.value = startPetX + dx; petY.value = startPetY + dy; }
};
const stopDragTouch = () => {
  document.removeEventListener('touchmove', doDragTouch); document.removeEventListener('touchend', stopDragTouch);
  if (!isDragging) openMenu();
};

// =======================
// 5. 聊天对讲核心
// =======================
const showChat = ref(false);
const chatTargetId = ref(999);
const chatTargetName = ref('');
const userMessage = ref('');
const chatHistory = ref([]);
const chatBodyRef = ref(null);

const openChat = (targetId) => {
  chatTargetId.value = targetId;
  chatTargetName.value = targetId === 999 ? '全局' : currentNotebookName.value;
  menuState.value = 0;
  showChat.value = true;
  playAnimation();
  if(chatHistory.value.length === 0){
    chatHistory.value = [{ role: 'ai', text: targetId === 999 ? '喵~ 全局节点已连接。有什么我可以帮你的吗？' : '专属知识库已挂载，随时提问喵！' }];
  }
};

const scrollToBottom = async () => { await nextTick(); if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight; };

const sendMessage = async () => {
  if (!userMessage.value.trim()) return;
  const msg = userMessage.value;
  chatHistory.value.push({ role: 'user', text: msg });
  userMessage.value = '';
  await scrollToBottom(); playAnimation();

  try {
    const res = await request.post('/api/chat', { notebookId: chatTargetId.value, query: msg });
    chatHistory.value.push({ role: 'ai', text: res.answer || '喵呜...' });
  } catch (error) { chatHistory.value.push({ role: 'ai', text: '连接断开喵...' }); }
  await scrollToBottom();
};

let autoPlayTimer;
onMounted(() => {
  autoPlayTimer = setInterval(() => { playAnimation(); }, 60000);
  window.addEventListener('resize', () => {
    const maxX = window.innerWidth - (petRef.value?.clientWidth || 100);
    const maxY = window.innerHeight - (petRef.value?.clientHeight || 100);
    petX.value = Math.min(petX.value, maxX); petY.value = Math.min(petY.value, maxY);
  });
});
onUnmounted(() => { clearInterval(autoPlayTimer); });
</script>

<style scoped>
/* ========================================= 核心布局：Flex 自适应（绝不遮挡主内容） ========================================= */
.nebula-app-container { display: flex; width: 100vw; height: 100vh; overflow: hidden; background-color: #06060f; }
.nebula-main-viewport { flex: 1; height: 100%; overflow-y: auto; position: relative; transition: flex 0.4s cubic-bezier(0.4, 0, 0.2, 1); }

/* ========================================= Notion 风格侧边栏聊天框 ========================================= */
.nebula-notion-sidebar { width: 380px; height: 100%; flex-shrink: 0; display: flex; flex-direction: column; background: rgba(18, 18, 36, 0.85); backdrop-filter: blur(40px) saturate(140%); border-left: 1px solid rgba(255, 255, 255, 0.08); box-shadow: -8px 0 32px rgba(0, 0, 0, 0.4); z-index: 9000; }
.sidebar-slide-enter-active, .sidebar-slide-leave-active { transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1); }
.sidebar-slide-enter-from, .sidebar-slide-leave-to { transform: translateX(100%); }
.chat-header { padding: 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
.chat-header .title { color: #e8e8f0; font-weight: 700; font-size: 15px; display: flex; align-items: center; gap: 8px; }
.btn-close { background: transparent; border: none; color: #6b6b85; cursor: pointer; transition: all 0.3s; display: flex; padding: 4px; border-radius: 6px; }
.btn-close:hover { color: #e8e8f0; background: rgba(255,255,255,0.1); transform: rotate(90deg); }
.chat-body { flex: 1; padding: 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; }
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.1); border-radius: 4px; }
.chat-bubble-wrapper { display: flex; width: 100%; }
.chat-bubble-wrapper.ai { justify-content: flex-start; }
.chat-bubble-wrapper.user { justify-content: flex-end; }
.chat-bubble { max-width: 85%; padding: 12px 16px; font-size: 14px; line-height: 1.6; border-radius: 12px; display: flex; align-items: flex-start; }
.ai-avatar-img { width: 28px; height: 28px; border-radius: 50%; border: 2px solid rgba(124, 111, 247, 0.8); box-shadow: 0 0 10px rgba(124, 111, 247, 0.4); margin-right: 12px; object-fit: cover; flex-shrink: 0; background: #06060f; margin-top: 2px; }
.ai .chat-bubble { background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.08); color: #e8e8f0; border-top-left-radius: 4px; }
.user .chat-bubble { background: linear-gradient(135deg, #7c6ff7, #4c3fc4); color: #fff; border-top-right-radius: 4px; }
.chat-input-area { padding: 20px; border-top: 1px solid rgba(255, 255, 255, 0.05); background: rgba(18, 18, 36, 0.5); }

/* ========================================= 全局桌宠与 Nebula 列表菜单 ========================================= */
.global-pet-wrapper { position: fixed; z-index: 10000; cursor: grab; user-select: none; filter: drop-shadow(0 10px 15px rgba(0, 0, 0, 0.5)); transition: filter 0.3s ease; }
.global-pet-wrapper:active { cursor: grabbing; filter: drop-shadow(0 5px 8px rgba(0, 0, 0, 0.6)); }
.global-pet-wrapper img { width: 100px; height: auto; pointer-events: none; }
.nebula-overlay { position: fixed; inset: 0; z-index: 9998; }
.nebula-list-menu { position: absolute; top: -10px; right: 110px; width: 220px; z-index: 9999; padding: 12px; background: rgba(18, 18, 36, 0.85); display: flex; flex-direction: column; }
.menu-header { font-size: 13px; color: #9898b4; font-weight: 600; padding: 0 8px 12px; border-bottom: 1px solid rgba(255,255,255,0.08); margin-bottom: 8px; display: flex; align-items: center; }
.menu-header.with-back { color: #e8e8f0; }
.btn-back { font-size: 18px; margin-right: 4px; cursor: pointer; transition: color 0.2s; }
.btn-back:hover { color: #a99df9; }
.menu-subtitle { font-size: 11px; color: #6b6b85; padding: 4px 8px; text-transform: uppercase; letter-spacing: 1px; }
.menu-divider { height: 1px; background: rgba(255,255,255,0.05); margin: 8px 0; }
.menu-item { display: flex; align-items: center; gap: 10px; padding: 10px; border-radius: 10px; color: #e8e8f0; font-size: 14px; cursor: pointer; transition: all 0.3s; position: relative; overflow: hidden; }
.menu-item .material-icons { font-size: 18px; color: #9898b4; transition: color 0.3s; }
.menu-item:hover { background: rgba(255,255,255,0.06); transform: translateX(4px); }
.menu-item:hover .material-icons { color: #a99df9; }
.primary-item { color: #a99df9; font-weight: 500; }
.primary-item .material-icons { color: #7c6ff7; }
.current-item { background: rgba(124, 111, 247, 0.15); border: 1px solid rgba(124, 111, 247, 0.3); }
.current-item:hover { background: rgba(124, 111, 247, 0.25); }
.action-item:hover { background: rgba(74, 222, 128, 0.1); color: #4ade80; }
.action-item:hover .material-icons { color: #4ade80; }
.empty-text { font-size: 12px; color: #6b6b85; text-align: center; padding: 10px 0; }
.emoji-icon { font-size: 16px; margin-right: 4px; }
.menu-fade-enter-active, .menu-fade-leave-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.menu-fade-enter-from, .menu-fade-leave-to { opacity: 0; transform: scale(0.9) translateX(20px); }

/* ========================================= 全局基础玻璃态与输入/弹窗组件 ========================================= */
.glass-panel { background: rgba(18, 18, 36, 0.65); backdrop-filter: blur(40px) saturate(140%); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 20px; box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1), 0 8px 32px rgba(0, 0, 0, 0.4); }
.nebula-input-wrapper { position: relative; background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 12px; display: flex; overflow: hidden; transition: all 0.3s; margin-bottom: 0px; }
.nebula-input-wrapper:focus-within { border-color: transparent; background: rgba(18, 18, 36, 0.8); }
.nebula-input { flex: 1; background: transparent; border: none; padding: 12px 16px; color: #e8e8f0; outline: none; font-size: 14px; }
.focus-line { position: absolute; bottom: 0; left: 0; height: 2px; width: 100%; background: linear-gradient(90deg, #7c6ff7, #a99df9, #7c6ff7); transform: scaleX(0); transform-origin: left; transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.nebula-input-wrapper:focus-within .focus-line { transform: scaleX(1); }
.btn-send { background: transparent; color: #7c6ff7; border: none; padding: 0 16px; font-weight: 600; cursor: pointer; transition: color 0.3s; }
.btn-send:hover { color: #a99df9; }
.nebula-modal-overlay { position: fixed; inset: 0; background: rgba(6, 6, 15, 0.6); backdrop-filter: blur(8px); display: flex; justify-content: center; align-items: center; z-index: 10001; }
.nebula-modal-content { width: 100%; max-width: 400px; padding: 32px; }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.modal-header h2 { margin: 0; font-size: 1.2rem; color: #e8e8f0; }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }
.btn-nebula-outline { background: transparent; border: 1px solid rgba(255, 255, 255, 0.15); color: #e8e8f0; border-radius: 10px; padding: 10px 20px; font-weight: 600; cursor: pointer; transition: 0.3s; }
.btn-nebula-outline:hover { background: rgba(255, 255, 255, 0.05); border-color: #a99df9; }
.btn-nebula-primary { background: linear-gradient(135deg, #7c6ff7, #5b4fcf); color: #fff; border: none; border-radius: 10px; padding: 10px 20px; font-weight: 600; cursor: pointer; transition: all 0.3s; }
.btn-nebula-primary:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 15px rgba(124, 111, 247, 0.4); }
.btn-nebula-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.95) translateY(20px); }
</style>