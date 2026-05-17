<template>
  <div class="nebula-app-container">

    <div class="ambient-glow glow-1"></div>
    <div class="ambient-glow glow-2"></div>
    <div class="ambient-glow glow-3"></div>
    <canvas id="globalParticleCanvas" ref="globalCanvasRef" class="global-particles"></canvas>

    <NebulaNav v-if="showNav" />

    <main class="nebula-main-viewport custom-scrollbar" :class="{ 'main-content-shifted': showNav }">
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
          <button class="btn-close" @click="showChat = false" title="收起面板">
            <span class="material-icons">close</span>
          </button>
        </div>

        <div class="chat-body custom-scrollbar" ref="chatBodyRef">
          <div v-for="(msg, index) in chatHistory" :key="index" :class="['chat-bubble-wrapper', msg.role]">
            <div class="chat-bubble">
              <img v-if="msg.role === 'ai'" src="/pet/小黑.jpg" class="ai-avatar-img" alt="小黑" />
              <div v-if="msg.role === 'ai'" class="markdown-body" v-html="renderAiMessage(msg.text)" @click="handleBubbleClick($event, msg.sources)"></div>
              <div v-else>{{ msg.text }}</div>
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

    <div v-if="menuState > 0" class="nebula-overlay" @click="menuState = 0"></div>

    <div class="global-pet-wrapper" ref="petRef" :style="{ left: petX + 'px', top: petY + 'px' }" @mousedown="startDrag" @touchstart="startDragTouch">
      <img :src="currentDisplaySrc" alt="罗小黑" draggable="false" crossorigin="anonymous" @load="extractStaticFrame"/>
    </div>

    <transition name="menu-fade">
      <div v-if="menuState > 0" class="nebula-list-menu glass-panel" :style="{ position: 'fixed', left: Math.max(90, petX - 230) + 'px', top: Math.max(10, petY - 10) + 'px', zIndex: 9999 }" @click.stop>
        <div v-if="menuState === 1" class="menu-container">
          <div class="menu-header">中枢控制面板</div>
          <div class="menu-body custom-scrollbar">
            <div class="menu-item primary-item" @click="openChat(999)"><span class="material-icons">public</span> 全局学习助手</div>
            <div class="menu-item primary-item" @click="router.push('/decks'); menuState = 0"><span class="material-icons" style="color: #a99df9;">style</span> 记忆牌组大厅</div>
            <div v-if="isNotebookMode" class="menu-item current-item" @click="openChat(currentNotebookId)"><span class="material-icons">library_books</span> 当前: {{ currentNotebookName }}</div>
            <div class="menu-divider"></div>
            <div class="menu-subtitle">最近思绪空间</div>
            <div class="menu-item" v-for="nb in recentNotebooks" :key="nb.id" @click="navigateToNotebook(nb.id)"><span class="material-icons" style="font-size: 16px;">book</span> {{ nb.name }}</div>
            <div v-if="recentNotebooks.length === 0" class="empty-text">暂无记录</div>
            <div class="menu-divider"></div>
            <div class="menu-item action-item" @click="openCreateModal"><span class="material-icons">add_circle_outline</span> 新建思绪空间</div>
            <div class="menu-item" @click="menuState = 2"><span class="material-icons">pets</span> 宠物动作指令</div>
          </div>
        </div>

        <div v-if="menuState === 2" class="menu-container">
          <div class="menu-header with-back"><span class="material-icons btn-back" @click="menuState = 1">chevron_left</span>动作库</div>
          <div class="menu-body custom-scrollbar">
            <div class="menu-item" v-for="action in actionData" :key="action.id" @click="triggerAction(action.id)"><span class="emoji-icon">{{ action.icon }}</span> {{ action.name }}</div>
          </div>
        </div>
      </div>
    </transition>

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

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from './utils/request';
import { renderAiMessage } from '@/utils/markdown';
import NebulaNav from '@/components/NebulaNav.vue'; // 引入全局导航栏

const route = useRoute();
const router = useRouter();

// 计算属性：控制导航栏是否显示 (避免在登录页出现)
const showNav = computed(() => {
  return route.path !== '/login' && route.path !== '/';
});

// 全局粒子系统核心逻辑
const globalCanvasRef = ref(null);
let particles = [];
const PARTICLE_COUNT = 75;
const CONNECTION_DIST = 130;
let animationId;
let mouseX = -1000, mouseY = -1000;
let isMouseOnCanvas = false;
let canvas, ctx;

class Particle {
  constructor() { this.reset(); this.x = Math.random() * (canvas?.width || window.innerWidth); this.y = Math.random() * (canvas?.height || window.innerHeight); }
  reset() { this.x = Math.random() * (canvas?.width || window.innerWidth); this.y = Math.random() * (canvas?.height || window.innerHeight); this.vx = (Math.random() - 0.5) * 0.5; this.vy = (Math.random() - 0.5) * 0.5; this.radius = Math.random() * 2 + 1; this.opacity = Math.random() * 0.5 + 0.25; this.pulseSpeed = Math.random() * 0.02 + 0.008; this.pulseOffset = Math.random() * Math.PI * 2; }
  update(time) {
    this.x += this.vx; this.y += this.vy;
    if (this.x < -20) this.x = (canvas?.width || window.innerWidth) + 20;
    if (this.x > (canvas?.width || window.innerWidth) + 20) this.x = -20;
    if (this.y < -20) this.y = (canvas?.height || window.innerHeight) + 20;
    if (this.y > (canvas?.height || window.innerHeight) + 20) this.y = -20;
    if (isMouseOnCanvas) {
      const dx = mouseX - this.x, dy = mouseY - this.y; const dist = Math.sqrt(dx * dx + dy * dy);
      if (dist < 200 && dist > 5) { const force = 0.015 / (dist * 0.05); this.vx += dx * force * 0.3; this.vy += dy * force * 0.3; }
    }
    const speed = Math.sqrt(this.vx * this.vx + this.vy * this.vy); const maxSpeed = 1.2;
    if (speed > maxSpeed) { this.vx = (this.vx / speed) * maxSpeed; this.vy = (this.vy / speed) * maxSpeed; }
    this.vx += (Math.random() - 0.5) * 0.015; this.vy += (Math.random() - 0.5) * 0.015; this.vx *= 0.9995; this.vy *= 0.9995;
    const pulse = Math.sin(time * this.pulseSpeed + this.pulseOffset) * 0.15; this.currentOpacity = Math.max(0.1, Math.min(0.8, this.opacity + pulse));
  }
  draw(ctx) {
    ctx.beginPath(); ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2); ctx.fillStyle = `rgba(180,170,220,${this.currentOpacity})`; ctx.fill();
    if (this.radius > 1.5) { ctx.beginPath(); ctx.arc(this.x, this.y, this.radius * 2.5, 0, Math.PI * 2); ctx.fillStyle = `rgba(160,150,210,${this.currentOpacity * 0.25})`; ctx.fill(); }
  }
}
function initParticles() { particles = []; for (let i = 0; i < PARTICLE_COUNT; i++) particles.push(new Particle()); }
function drawConnections() {
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x, dy = particles[i].y - particles[j].y, dist = Math.sqrt(dx * dx + dy * dy);
      if (dist < CONNECTION_DIST) {
        const alpha = (1 - dist / CONNECTION_DIST) * 0.35, gradient = ctx.createLinearGradient(particles[i].x, particles[i].y, particles[j].x, particles[j].y);
        gradient.addColorStop(0, `rgba(150,140,200,${alpha * particles[i].currentOpacity * 2})`); gradient.addColorStop(1, `rgba(150,140,200,${alpha * particles[j].currentOpacity * 2})`);
        ctx.beginPath(); ctx.moveTo(particles[i].x, particles[i].y); ctx.lineTo(particles[j].x, particles[j].y); ctx.strokeStyle = gradient; ctx.lineWidth = 0.6; ctx.stroke();
      }
    }
  }
}
function animate(timestamp) { if (!ctx || !canvas) return; ctx.clearRect(0, 0, canvas.width, canvas.height); for (const p of particles) { p.update(timestamp); p.draw(ctx); } drawConnections(); animationId = requestAnimationFrame(animate); }
function resizeCanvas() { if (!canvas) return; canvas.width = window.innerWidth; canvas.height = window.innerHeight; }
const onResize = () => { resizeCanvas(); initParticles(); };
const onMouseMove = (e) => { mouseX = e.clientX; mouseY = e.clientY; isMouseOnCanvas = true; };
const onMouseLeave = () => { isMouseOnCanvas = false; };
const onTouchMove = (e) => { mouseX = e.touches[0].clientX; mouseY = e.touches[0].clientY; isMouseOnCanvas = true; };
const onTouchEnd = () => { isMouseOnCanvas = false; };

// 状态与配置管理
const menuState = ref(0);
const recentNotebooks = ref([]);
const showCreateModal = ref(false);
const newNotebookName = ref('');
const actionData = [ { id: 1, name: '趴', icon: '' }, { id: 2, name: '听歌', icon: '' }, { id: 3, name: '吃西瓜', icon: '' }, { id: 4, name: '翻滚', icon: '' }, { id: 5, name: '奔跑', icon: '' }, { id: 6, name: '洗脸', icon: '' } ];
const currentNotebookId = computed(() => route.params.notebookId ? parseInt(route.params.notebookId) : null);
const isNotebookMode = computed(() => !!currentNotebookId.value);
const currentNotebookName = ref('');

watch(() => route.params.notebookId, async (newId) => {
  if (newId) {
    try { const nbs = await request.get('/api/notebooks'); const nb = nbs.find(n => n.id == newId); currentNotebookName.value = nb ? nb.name : `笔记本 #${newId}`; }
    catch (e) { currentNotebookName.value = `获取中...`; }
  }
}, { immediate: true });

// 菜单与弹窗业务逻辑
const fetchRecentNotebooks = async () => { try { const res = await request.get('/api/notebooks'); recentNotebooks.value = res.slice(0, 3); } catch (error) { console.error("无法获取最近笔记本"); } };
const openMenu = async () => { if (showChat.value) { showChat.value = false; } else { menuState.value = 1; await fetchRecentNotebooks(); } };
const navigateToNotebook = (id) => { menuState.value = 0; router.push(`/copilot/${id}`); };
const openCreateModal = () => { showCreateModal.value = true; menuState.value = 0; };
const handleCreateNotebook = async () => {
  if (!newNotebookName.value.trim()) return;
  try { const res = await request.post('/api/notebooks', { name: newNotebookName.value }); newNotebookName.value = ''; showCreateModal.value = false; if(res && res.id) router.push(`/copilot/${res.id}`); }
  catch (error) { alert("创建失败，请检查网络或登录状态"); }
};

// 动画控制
const currentAction = ref(4);
const currentDisplaySrc = ref('/pet/4.gif');
const staticImages = ref({});
let animTimer = null;
const extractStaticFrame = (e) => {
  const img = e.target;
  if (img.src.includes('.gif') && !staticImages.value[currentAction.value]) {
    try { const canvas = document.createElement('canvas'); canvas.width = img.naturalWidth || 100; canvas.height = img.naturalHeight || 100; canvas.getContext('2d').drawImage(img, 0, 0); staticImages.value[currentAction.value] = canvas.toDataURL('image/png'); }
    catch (err) {}
  }
};
const triggerAction = (id) => {
  menuState.value = 0; currentAction.value = id;
  if (animTimer) { clearTimeout(animTimer); animTimer = null; }
  currentDisplaySrc.value = `/pet/${id}.gif?t=${Date.now()}`;
  animTimer = setTimeout(() => { currentDisplaySrc.value = staticImages.value[id] || `/pet/${id}.gif`; animTimer = null; }, 3500);
};
const playAnimation = () => { triggerAction(currentAction.value); };

// 拖拽交互
const petRef = ref(null);
const petX = ref(window.innerWidth - 180), petY = ref(window.innerHeight - 180);
let isDragging = false, startMouseX = 0, startMouseY = 0, startPetX = 0, startPetY = 0;
const startDrag = (e) => { isDragging = false; startMouseX = e.clientX; startMouseY = e.clientY; startPetX = petX.value; startPetY = petY.value; document.addEventListener('mousemove', doDrag); document.addEventListener('mouseup', stopDrag); };
const doDrag = (e) => {
  const dx = e.clientX - startMouseX, dy = e.clientY - startMouseY; if (Math.abs(dx) > 5 || Math.abs(dy) > 5) isDragging = true;
  if (isDragging) { const maxX = window.innerWidth - (petRef.value?.clientWidth || 100), maxY = window.innerHeight - (petRef.value?.clientHeight || 100); petX.value = Math.max(0, Math.min(startPetX + dx, maxX)); petY.value = Math.max(0, Math.min(startPetY + dy, maxY)); }
};
const stopDrag = () => { document.removeEventListener('mousemove', doDrag); document.removeEventListener('mouseup', stopDrag); if (!isDragging) openMenu(); };
const startDragTouch = (e) => { isDragging = false; startMouseX = e.touches[0].clientX; startMouseY = e.touches[0].clientY; startPetX = petX.value; startPetY = petY.value; document.addEventListener('touchmove', doDragTouch, { passive: false }); document.addEventListener('touchend', stopDragTouch); };
const doDragTouch = (e) => {
  e.preventDefault(); const dx = e.touches[0].clientX - startMouseX, dy = e.touches[0].clientY - startMouseY; if (Math.abs(dx) > 5 || Math.abs(dy) > 5) isDragging = true;
  if (isDragging) { petX.value = startPetX + dx; petY.value = startPetY + dy; }
};
const stopDragTouch = () => { document.removeEventListener('touchmove', doDragTouch); document.removeEventListener('touchend', stopDragTouch); if (!isDragging) openMenu(); };

// 聊天对讲核心
const showChat = ref(false), chatTargetId = ref(999), chatTargetName = ref(''), userMessage = ref(''), chatHistory = ref([]), chatBodyRef = ref(null);
const openChat = (targetId) => {
  chatTargetId.value = targetId; chatTargetName.value = targetId === 999 ? '全局' : currentNotebookName.value; menuState.value = 0; showChat.value = true; playAnimation();
  if(chatHistory.value.length === 0){ chatHistory.value = [{ role: 'ai', text: targetId === 999 ? '喵~ 全局节点已连接。有什么我可以帮你的吗？' : '专属知识库已挂载，随时提问喵！' }]; }
};
const scrollToBottom = async () => { await nextTick(); if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight; };
const sendMessage = async () => {
  if (!userMessage.value.trim()) return; const msg = userMessage.value; chatHistory.value.push({ role: 'user', text: msg }); userMessage.value = ''; await scrollToBottom(); playAnimation();
  try { const res = await request.post('/api/chat', { notebookId: chatTargetId.value, query: msg }); chatHistory.value.push({ role: 'ai', text: res.answer || '喵呜...' }); }
  catch (error) { chatHistory.value.push({ role: 'ai', text: '连接断开喵...' }); } await scrollToBottom();
};
// 处理气泡内部引用角标的点击代理
const handleBubbleClick = (event, sources) => {
  const target = event.target;
  if (target.classList.contains('citation-badge')) {
    const id = parseInt(target.getAttribute('data-id'), 10);
    // 全局聊天如果没有做来源溯源弹窗，可以打印出来或者做个简单的 Toast
    // 如果你有对应的 showSource 方法，可以调用它
    console.log('用户点击了引用来源 ID:', id, '数据源:', sources);
    // alert(`引用文献 [${id}]`); // 临时替代方案
  }
};

let autoPlayTimer;
onMounted(() => {
  autoPlayTimer = setInterval(() => { playAnimation(); }, 60000);
  window.addEventListener('resize', () => { const maxX = window.innerWidth - (petRef.value?.clientWidth || 100), maxY = window.innerHeight - (petRef.value?.clientHeight || 100); petX.value = Math.min(petX.value, maxX); petY.value = Math.min(petY.value, maxY); });
  canvas = globalCanvasRef.value; ctx = canvas?.getContext('2d');
  if (canvas && ctx) { resizeCanvas(); initParticles(); animationId = requestAnimationFrame(animate); window.addEventListener('resize', onResize); document.addEventListener('mousemove', onMouseMove); document.addEventListener('mouseleave', onMouseLeave); document.addEventListener('touchmove', onTouchMove, { passive: true }); document.addEventListener('touchend', onTouchEnd); }
});
onUnmounted(() => {
  clearInterval(autoPlayTimer);
  if (animationId) cancelAnimationFrame(animationId);
  window.removeEventListener('resize', onResize); document.removeEventListener('mousemove', onMouseMove); document.removeEventListener('mouseleave', onMouseLeave); document.removeEventListener('touchmove', onTouchMove); document.removeEventListener('touchend', onTouchEnd);
});
</script>

<style scoped>
.nebula-app-container{display:flex;width:100vw;height:100vh;overflow:hidden;background-color:#06060f;}
.nebula-main-viewport{flex:1;height:100%;overflow-y:auto;position:relative;transition:all 0.4s cubic-bezier(0.4,0,0.2,1);z-index:1;}
.main-content-shifted{padding-left:80px;box-sizing:border-box;}
.nebula-notion-sidebar{width:380px;height:100%;flex-shrink:0;display:flex;flex-direction:column;background:rgba(18,18,36,0.85);backdrop-filter:blur(40px) saturate(140%);border-left:1px solid rgba(255,255,255,0.08);box-shadow:-8px 0 32px rgba(0,0,0,0.4);z-index:9000;}
.sidebar-slide-enter-active,.sidebar-slide-leave-active{transition:transform 0.4s cubic-bezier(0.4,0,0.2,1);}
.sidebar-slide-enter-from,.sidebar-slide-leave-to{transform:translateX(100%);}
.chat-header{padding:20px;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid rgba(255,255,255,0.05);}
.chat-header .title{color:#e8e8f0;font-weight:700;font-size:15px;display:flex;align-items:center;gap:8px;}
.btn-close{background:transparent;border:none;color:#6b6b85;cursor:pointer;transition:all 0.3s;display:flex;padding:4px;border-radius:6px;}
.btn-close:hover{color:#e8e8f0;background:rgba(255,255,255,0.1);transform:rotate(90deg);}
.chat-body{flex:1;padding:20px;overflow-y:auto;display:flex;flex-direction:column;gap:16px;}
.custom-scrollbar::-webkit-scrollbar{width:4px;}
.custom-scrollbar::-webkit-scrollbar-thumb{background:rgba(255,255,255,0.1);border-radius:4px;}
.chat-bubble-wrapper{display:flex;width:100%;}
.chat-bubble-wrapper.ai{justify-content:flex-start;}
.chat-bubble-wrapper.user{justify-content:flex-end;}
.chat-bubble{max-width:85%;padding:12px 16px;font-size:14px;line-height:1.6;border-radius:12px;display:flex;align-items:flex-start;}
.ai-avatar-img{width:28px;height:28px;border-radius:50%;border:2px solid rgba(124,111,247,0.8);box-shadow:0 0 10px rgba(124,111,247,0.4);margin-right:12px;object-fit:cover;flex-shrink:0;background:#06060f;margin-top:2px;}
.ai .chat-bubble{background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.08);color:#e8e8f0;border-top-left-radius:4px;}
.user .chat-bubble{background:linear-gradient(135deg,#7c6ff7,#4c3fc4);color:#fff;border-top-right-radius:4px;}
.chat-input-area{padding:20px;border-top:1px solid rgba(255,255,255,0.05);background:rgba(18,18,36,0.5);}
.global-pet-wrapper{position:fixed;z-index:10000;cursor:grab;user-select:none;filter:drop-shadow(0 10px 15px rgba(0,0,0,0.5));transition:filter 0.3s ease;}
.global-pet-wrapper:active{cursor:grabbing;filter:drop-shadow(0 5px 8px rgba(0,0,0,0.6));}
.global-pet-wrapper img{width:100px;height:auto;pointer-events:none;}
.nebula-overlay{position:fixed;inset:0;z-index:9998;}
.nebula-list-menu{position:absolute;top:-10px;right:110px;width:220px;z-index:9999;padding:12px;background:rgba(18,18,36,0.85);display:flex;flex-direction:column;}
.menu-header{font-size:13px;color:#9898b4;font-weight:600;padding:0 8px 12px;border-bottom:1px solid rgba(255,255,255,0.08);margin-bottom:8px;display:flex;align-items:center;}
.menu-header.with-back{color:#e8e8f0;}
.btn-back{font-size:18px;margin-right:4px;cursor:pointer;transition:color 0.2s;}
.btn-back:hover{color:#a99df9;}
.menu-subtitle{font-size:11px;color:#6b6b85;padding:4px 8px;text-transform:uppercase;letter-spacing:1px;}
.menu-divider{height:1px;background:rgba(255,255,255,0.05);margin:8px 0;}
.menu-item{display:flex;align-items:center;gap:10px;padding:10px;border-radius:10px;color:#e8e8f0;font-size:14px;cursor:pointer;transition:all 0.3s;position:relative;overflow:hidden;}
.menu-item .material-icons{font-size:18px;color:#9898b4;transition:color 0.3s;}
.menu-item:hover{background:rgba(255,255,255,0.06);transform:translateX(4px);}
.menu-item:hover .material-icons{color:#a99df9;}
.primary-item{color:#a99df9;font-weight:500;}
.primary-item .material-icons{color:#7c6ff7;}
.current-item{background:rgba(124,111,247,0.15);border:1px solid rgba(124,111,247,0.3);}
.current-item:hover{background:rgba(124,111,247,0.25);}
.action-item:hover{background:rgba(74,222,128,0.1);color:#4ade80;}
.action-item:hover .material-icons{color:#4ade80;}
.empty-text{font-size:12px;color:#6b6b85;text-align:center;padding:10px 0;}
.emoji-icon{font-size:16px;margin-right:4px;}
.menu-fade-enter-active,.menu-fade-leave-active{transition:all 0.4s cubic-bezier(0.34,1.56,0.64,1);}
.menu-fade-enter-from,.menu-fade-leave-to{opacity:0;transform:scale(0.9) translateX(20px);}
.glass-panel{background:rgba(18,18,36,0.65);backdrop-filter:blur(40px) saturate(140%);border:1px solid rgba(255,255,255,0.08);border-radius:20px;box-shadow:inset 0 1px 0 rgba(255,255,255,0.1),0 8px 32px rgba(0,0,0,0.4);}
.nebula-input-wrapper{position:relative;background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.1);border-radius:12px;display:flex;overflow:hidden;transition:all 0.3s;margin-bottom:0px;}
.nebula-input-wrapper:focus-within{border-color:transparent;background:rgba(18,18,36,0.8);}
.nebula-input{flex:1;background:transparent;border:none;padding:12px 16px;color:#e8e8f0;outline:none;font-size:14px;}
.focus-line{position:absolute;bottom:0;left:0;height:2px;width:100%;background:linear-gradient(90deg,#7c6ff7,#a99df9,#7c6ff7);transform:scaleX(0);transform-origin:left;transition:transform 0.4s cubic-bezier(0.34,1.56,0.64,1);}
.nebula-input-wrapper:focus-within .focus-line{transform:scaleX(1);}
.btn-send{background:transparent;color:#7c6ff7;border:none;padding:0 16px;font-weight:600;cursor:pointer;transition:color 0.3s;}
.btn-send:hover{color:#a99df9;}
.nebula-modal-overlay{position:fixed;inset:0;background:rgba(6,6,15,0.6);backdrop-filter:blur(8px);display:flex;justify-content:center;align-items:center;z-index:10001;}
.nebula-modal-content{width:100%;max-width:400px;padding:32px;}
.modal-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:24px;}
.modal-header h2{margin:0;font-size:1.2rem;color:#e8e8f0;}
.modal-footer{display:flex;justify-content:flex-end;gap:12px;margin-top:24px;}
.btn-nebula-outline{background:transparent;border:1px solid rgba(255,255,255,0.15);color:#e8e8f0;border-radius:10px;padding:10px 20px;font-weight:600;cursor:pointer;transition:0.3s;}
.btn-nebula-outline:hover{background:rgba(255,255,255,0.05);border-color:#a99df9;}
.btn-nebula-primary{background:linear-gradient(135deg,#7c6ff7,#5b4fcf);color:#fff;border:none;border-radius:10px;padding:10px 20px;font-weight:600;cursor:pointer;transition:all 0.3s;}
.btn-nebula-primary:hover:not(:disabled){transform:translateY(-2px);box-shadow:0 4px 15px rgba(124,111,247,0.4);}
.btn-nebula-primary:disabled{opacity:0.5;cursor:not-allowed;}
.modal-fade-enter-active,.modal-fade-leave-active{transition:all 0.3s cubic-bezier(0.4,0,0.2,1);}
.modal-fade-enter-from,.modal-fade-leave-to{opacity:0;transform:scale(0.95) translateY(20px);}
#globalParticleCanvas{position:absolute;top:0;left:0;width:100%;height:100%;z-index:0;pointer-events:none;}
.ambient-glow{position:absolute;border-radius:50%;filter:blur(120px);pointer-events:none;z-index:0;opacity:0.5;animation:ambientFloat 12s ease-in-out infinite;}
.ambient-glow.glow-1{width:500px;height:500px;background:radial-gradient(circle,rgba(124,111,247,0.35) 0%,transparent 70%);top:-15%;left:-10%;animation-duration:14s;}
.ambient-glow.glow-2{width:400px;height:400px;background:radial-gradient(circle,rgba(56,189,248,0.25) 0%,transparent 70%);bottom:-12%;right:-8%;animation-delay:-5s;animation-duration:16s;}
.ambient-glow.glow-3{width:350px;height:350px;background:radial-gradient(circle,rgba(168,85,247,0.3) 0%,transparent 70%);top:50%;left:55%;animation-delay:-9s;animation-duration:18s;}
@keyframes ambientFloat{0%,100%{transform:translate(0,0) scale(1);}20%{transform:translate(60px,-40px) scale(1.15);}40%{transform:translate(-30px,30px) scale(0.9);}60%{transform:translate(-50px,-25px) scale(1.1);}80%{transform:translate(35px,45px) scale(0.85);}}
.markdown-body{font-family:'Inter','SF Pro Display',system-ui,-apple-system,sans-serif;font-size:0.95rem;line-height:1.75;color:#e8e8f0;word-break:break-word;}
.markdown-body p{margin:0 0 14px 0;}
.markdown-body p:last-child{margin-bottom:0;}
.markdown-body h1,.markdown-body h2,.markdown-body h3,.markdown-body h4{color:#ffffff;font-weight:700;letter-spacing:-0.02em;margin:24px 0 12px 0;}
.markdown-body h1{font-size:1.5rem;border-bottom:1px solid rgba(255,255,255,0.1);padding-bottom:6px;}
.markdown-body h2{font-size:1.3rem;}
.markdown-body h3{font-size:1.15rem;}
.markdown-body strong{color:#a99df9;font-weight:600;}
.markdown-body ul,.markdown-body ol{margin:0 0 14px 0;padding-left:22px;}
.markdown-body li{margin-bottom:6px;position:relative;}
.markdown-body blockquote{margin:0 0 16px 0;padding:10px 16px;background:rgba(124,111,247,0.08);border-left:4px solid #7c6ff7;border-radius:0 12px 12px 0;color:#9898b4;}
.markdown-body blockquote p{margin:0;}
.markdown-body :not(pre) > code{font-family:'SF Mono',Consolas,Monaco,monospace;font-size:0.85em;background:rgba(124,111,247,0.15);color:#a99df9;padding:3px 6px;border-radius:6px;border:1px solid rgba(124,111,247,0.2);}
.markdown-body pre{margin:16px 0;padding:16px;background:rgba(6,6,15,0.8) !important;border:1px solid rgba(255,255,255,0.08);border-radius:12px;overflow-x:auto;box-shadow:inset 0 1px 2px rgba(0,0,0,0.5);}
.markdown-body pre code{font-family:'SF Mono',Consolas,Monaco,monospace;font-size:0.9em;line-height:1.5;background:transparent !important;padding:0;border:none;color:#e8e8f0;}
.markdown-body .citation-badge{display:inline-flex;justify-content:center;align-items:center;background:rgba(124,111,247,0.2);color:#a99df9;border:1px solid rgba(124,111,247,0.4);font-size:0.72rem;font-weight:700;width:18px;height:18px;border-radius:50%;margin:0 3px;cursor:pointer;transition:all 0.3s cubic-bezier(0.4,0,0.2,1);vertical-align:super;}
.markdown-body .citation-badge:hover{background:#7c6ff7;color:#ffffff;box-shadow:0 0 8px rgba(124,111,247,0.6);transform:scale(1.15) translateY(-2px);}
.chat-bubble :deep(.markdown-body){font-size:14px;line-height:1.6;color:#e8e8f0;}
.chat-bubble :deep(.markdown-body strong){color:#a99df9;font-weight:600;}
.chat-bubble :deep(.markdown-body h1),.chat-bubble :deep(.markdown-body h2){font-size:16px;margin:10px 0;color:#fff;}
</style>