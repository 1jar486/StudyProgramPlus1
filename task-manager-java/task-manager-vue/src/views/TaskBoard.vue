<template>
  <div class="nebula-app">
    <div class="nebula-halos">
      <div class="halo halo-1"></div>
      <div class="halo halo-2"></div>
      <div class="halo halo-3"></div>
    </div>
    <canvas id="nebula-canvas" class="nebula-particles"></canvas>


    <transition name="toast-fade">
      <div v-if="toast.show" :class="['nebula-toast', `toast-${toast.type}`]">
        <span class="material-icons toast-icon">{{ toast.type === 'success' ? 'check_circle' : 'error_outline' }}</span>
        {{ toast.message }}
      </div>
    </transition>


    <div class="nebula-nav-bar glass-panel">
      <button class="nav-btn text-accent" @click="router.push('/notebooks')">
        <span class="material-icons">auto_awesome</span> 进入AI大脑
      </button>
      <div class="nav-divider"></div>
      <button class="nav-icon-btn" @click="toggleTheme" title="切换模式">
        <span class="material-icons">{{ isDark ? 'light_mode' : 'dark_mode' }}</span>
      </button>
      <button class="nav-btn text-weak" @click="handleLogout">登出</button>
    </div>


    <div class="nebula-main-card glass-panel">
      <h1 class="nebula-title">任务控制台</h1>


      <div class="nebula-input-wrapper">
        <span class="material-icons input-icon">add_task</span>
        <input type="text" class="nebula-input" v-model="newTask.title" placeholder="有什么新计划？" @keyup.enter="handleAddTask" autofocus />
        <button class="btn-nebula-primary btn-add" @click="handleAddTask" :disabled="!newTask.title.trim()">添加</button>
        <div class="focus-line"></div>
      </div>


      <div class="nebula-controls">
        <div class="nebula-tabs">
          <button class="nebula-tab" :class="{ active: currentFilter === 'all' }" @click="currentFilter = 'all'">全部 <span>{{ totalCount }}</span></button>
          <button class="nebula-tab" :class="{ active: currentFilter === 'incomplete' }" @click="currentFilter = 'incomplete'">未完成 <span>{{ incompleteCount }}</span></button>
          <button class="nebula-tab" :class="{ active: currentFilter === 'complete' }" @click="currentFilter = 'complete'">已完成 <span>{{ completedCount }}</span></button>
        </div>
        <button class="btn-nebula-sort" :class="{ 'is-active': isSortByPriority }" @click="isSortByPriority = !isSortByPriority">
          <span class="material-icons sort-icon">{{ isSortByPriority ? 'done_all' : 'sort' }}</span> 优先级排序
        </button>
      </div>


      <div class="nebula-task-list">
        <transition-group name="list" tag="div">
          <div v-for="task in filteredTasks" :key="task.id" class="nebula-task-row glass-panel-inner" :class="{ 'is-completed': task.completed }">
            <div class="row-left">
              <label class="nebula-checkbox-wrapper">
                <input type="checkbox" class="hide-native-checkbox" :checked="task.completed" @change="toggleTask(task.id)">
                <div class="custom-checkbox"><span class="material-icons check-icon">done</span></div>
              </label>
              <span class="row-title">{{ task.title }}</span>
            </div>
            <div class="row-right">
              <span class="row-time"><span class="material-icons time-icon">{{ task.completed ? 'task_alt' : 'schedule' }}</span> {{ task.completed ? ('完成于 ' + formatExactTime(task.completedTime)) : ('创建于 ' + formatExactTime(task.createdTime)) }}</span>
              <div class="nebula-select-wrapper" :class="`priority-${task.priority}`">
                <select v-model="task.priority" @change="handleInlineEdit(task)">
                  <option value="高">高优先级</option><option value="中">中优先级</option><option value="低">低优先级</option>
                </select>
                <span class="material-icons select-arrow">expand_more</span>
              </div>
              <div class="nebula-select-wrapper tag-wrapper">
                <select v-model="task.tag" @change="handleInlineEdit(task)">
                  <option value="学习">学习</option><option value="生活">生活</option><option value="兼职">兼职</option>
                </select>
                <span class="material-icons select-arrow">expand_more</span>
              </div>
              <div class="row-actions">
                <button class="nebula-icon-btn action-info" @click="openTaskDetail(task)" title="详情"><span class="material-icons">info</span></button>
                <button class="nebula-icon-btn action-danger" @click.stop="triggerDeleteTask(task.id)" title="删除"><span class="material-icons">delete</span></button>
              </div>
            </div>
          </div>
        </transition-group>
      </div>
    </div>


    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="showDetailModal" @click.self="closeDetailModal">
        <div class="nebula-modal-content glass-panel">
          <div class="modal-header">
            <h2>任务中枢</h2>
            <button class="btn-close" @click="closeDetailModal"><span class="material-icons">close</span></button>
          </div>
          <div class="modal-body" v-if="currentTask">
            <div class="detail-grid">
              <div class="detail-item"><span class="label">全息标识</span><span class="value text-glow">{{ currentTask.title }}</span></div>
              <div class="detail-item"><span class="label">节点状态</span><span class="value" :class="currentTask.completed ? 'text-success' : 'text-accent'">{{ currentTask.completed ? '✅ 已归档' : '⏳ 激活中' }}</span></div>
              <div class="detail-item"><span class="label">优先级量级</span><span class="value">{{ currentTask.priority }}</span></div>
              <div class="detail-item"><span class="label">象限归属</span><span class="value">{{ currentTask.tag }}</span></div>
              <div class="detail-item full-width"><span class="label">时间戳记</span><span class="value">{{ formatExactTime(currentTask.createdTime) }}</span></div>
            </div>
            <div class="memo-area">
              <label class="label">描述备忘录</label>
              <div class="nebula-textarea-wrapper">
                <textarea v-model="currentTask.memo" class="nebula-textarea" placeholder="输入节点详细特征..."></textarea>
                <div class="focus-line"></div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-nebula-outline" @click="closeDetailModal">取消同步</button>
            <button class="btn-nebula-primary" @click="saveDetailModal">保存数据</button>
          </div>
        </div>
      </div>
    </transition>


    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="deleteTaskConfirm.show" @click.self="deleteTaskConfirm.show = false">
        <div class="nebula-modal-content glass-panel modal-mini">
          <div class="warning-icon-wrapper"><span class="material-icons text-danger">warning_amber</span></div>
          <h2 class="text-center">抹除该节点？</h2>
          <p class="text-center text-sub">此操作将永久抹除宇宙网络中的该任务数据，无法回溯。</p>
          <div class="modal-footer justify-center">
            <button class="btn-nebula-outline" @click="deleteTaskConfirm.show = false">中止</button>
            <button class="btn-nebula-danger" @click="executeDeleteTask">确认抹除</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>


<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import '../assets/style.css';


const router = useRouter();
const tasks = ref([]);
const isDark = ref(false);


const toast = ref({ show: false, message: '', type: 'success' });
let toastTimer = null;
const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer);
  toast.value = { show: true, message, type };
  toastTimer = setTimeout(() => { toast.value.show = false; }, 2200);
};


const formatExactTime = (dateStr) => {
  if (!dateStr) return '暂无记录';
  const safeDateStr = typeof dateStr === 'string' ? dateStr.replace(' ', 'T') : dateStr;
  const d = new Date(safeDateStr);
  if (isNaN(d.getTime())) return '解析异常';
  const year = d.getFullYear(), month = d.getMonth() + 1, day = d.getDate(), hour = String(d.getHours()).padStart(2, '0'), min = String(d.getMinutes()).padStart(2, '0');
  return `${year}年${month}月${day}日 ${hour}:${min}`;
};


const isSortByPriority = ref(false);
const currentFilter = ref('all');
const priorityWeight = { "高": 3, "中": 2, "低": 1 };
const newTask = ref({ title: '', priority: '中', tag: '学习' });


const totalCount = computed(() => tasks.value.length);
const completedCount = computed(() => tasks.value.filter(t => t.completed).length);
const incompleteCount = computed(() => tasks.value.filter(t => !t.completed).length);


const showDetailModal = ref(false);
const currentTask = ref(null);
const deleteTaskConfirm = ref({ show: false, id: null });


const filteredTasks = computed(() => {
  let result = [...tasks.value];
  if (currentFilter.value === 'incomplete') result = result.filter(t => !t.completed);
  else if (currentFilter.value === 'complete') result = result.filter(t => t.completed);
  return result.sort((a, b) => {
    if (isSortByPriority.value && priorityWeight[a.priority] !== priorityWeight[b.priority]) return priorityWeight[b.priority] - priorityWeight[a.priority];
    const safeTimeA = typeof a.createdTime === 'string' ? a.createdTime.replace(' ', 'T') : a.createdTime;
    const safeTimeB = typeof b.createdTime === 'string' ? b.createdTime.replace(' ', 'T') : b.createdTime;
    const timeA = new Date(safeTimeA || 0).getTime(), timeB = new Date(safeTimeB || 0).getTime();
    return (isNaN(timeB) ? 0 : timeB) - (isNaN(timeA) ? 0 : timeA);
  });
});


const fetchTasks = async () => {
  document.body.style.cursor = "progress";
  try { tasks.value = await request.get('/api/tasks'); }
  catch (error) { console.error("获取任务失败", error); }
  finally { document.body.style.cursor = "default"; }
};


const handleAddTask = async () => {
  if (!newTask.value.title.trim()) return;
  try {
    await request.post('/api/tasks', newTask.value);
    newTask.value.title = ''; fetchTasks();
  } catch (error) { showToast('添加失败', 'error'); }
};


const toggleTask = async (id) => {
  try { await request.put(`/api/tasks/${id}`); fetchTasks(); }
  catch (error) { showToast("状态更新失败", "error"); }
};


const handleInlineEdit = async (task) => {
  try {
    await request.put(`/api/tasks/${task.id}/core`, { title: task.title, priority: task.priority, tag: task.tag });
    showToast('属性已更新', 'success');
  } catch (error) {
    showToast('更新失败，正在回滚', 'error'); fetchTasks();
  }
};


const triggerDeleteTask = (id) => { deleteTaskConfirm.value = { show: true, id }; };
const executeDeleteTask = async () => {
  try {
    await request.delete(`/api/tasks/${deleteTaskConfirm.value.id}`);
    deleteTaskConfirm.value.show = false; fetchTasks(); showToast('任务已成功抹除', 'success');
  } catch (error) { showToast('抹除失败', 'error'); }
};


const openTaskDetail = (task) => { currentTask.value = { ...task, memo: task.memo || '' }; showDetailModal.value = true; };
const closeDetailModal = () => { showDetailModal.value = false; currentTask.value = null; };
const saveDetailModal = async () => {
  if (!currentTask.value) return;
  try {
    await request.put(`/api/tasks/${currentTask.value.id}/memo`, { memo: currentTask.value.memo });
    showToast('备忘录已同步', 'success'); closeDetailModal(); fetchTasks();
  } catch (error) { showToast('同步失败', 'error'); }
};


const handleLogout = () => { localStorage.removeItem('token'); router.push('/login'); };
const toggleTheme = () => {
  isDark.value = !isDark.value;
  if (isDark.value) { document.documentElement.setAttribute('data-theme', 'dark'); localStorage.setItem('theme', 'dark'); }
  else { document.documentElement.removeAttribute('data-theme'); localStorage.setItem('theme', 'light'); }
};


// Canvas 粒子系统逻辑 (Nebula 规范核心)
let animationFrameId;
const initParticles = () => {
  const canvas = document.getElementById('nebula-canvas');
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  const particles = [];
  let w = canvas.width = window.innerWidth;
  let h = canvas.height = window.innerHeight;
  let mouse = { x: null, y: null };
  window.addEventListener('resize', () => { w = canvas.width = window.innerWidth; h = canvas.height = window.innerHeight; });
  window.addEventListener('mousemove', (e) => { mouse.x = e.x; mouse.y = e.y; });
  window.addEventListener('mouseout', () => { mouse.x = null; mouse.y = null; });


  for (let i = 0; i < 75; i++) {
    particles.push({
      x: Math.random() * w, y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.8, vy: (Math.random() - 0.5) * 0.8,
      r: Math.random() * 1.5 + 0.5,
      baseAlpha: Math.random() * 0.5 + 0.2, phase: Math.random() * Math.PI * 2
    });
  }


  const draw = () => {
    ctx.clearRect(0, 0, w, h);
    for (let i = 0; i < particles.length; i++) {
      let p = particles[i];
      p.x += p.vx; p.y += p.vy;
      if (p.x < 0 || p.x > w) p.vx *= -1;
      if (p.y < 0 || p.y > h) p.vy *= -1;
      if (mouse.x) {
        let dx = mouse.x - p.x, dy = mouse.y - p.y, dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 150) { p.x += dx * 0.005; p.y += dy * 0.005; }
      }
      p.phase += 0.02;
      let alpha = p.baseAlpha + Math.sin(p.phase) * 0.2;
      ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(180, 170, 220, ${alpha})`; ctx.fill();
      for (let j = i + 1; j < particles.length; j++) {
        let p2 = particles[j], dx = p.x - p2.x, dy = p.y - p2.y, dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 130) {
          ctx.beginPath(); ctx.moveTo(p.x, p.y); ctx.lineTo(p2.x, p2.y);
          ctx.strokeStyle = `rgba(180, 170, 220, ${0.15 - dist / 130 * 0.15})`; ctx.stroke();
        }
      }
    }
    animationFrameId = requestAnimationFrame(draw);
  };
  draw();
};


onMounted(() => {
  if (localStorage.getItem('theme') === 'dark') { isDark.value = true; document.documentElement.setAttribute('data-theme', 'dark'); }
  fetchTasks(); initParticles();
});
onUnmounted(() => { if (animationFrameId) cancelAnimationFrame(animationFrameId); });
</script>


<style scoped>
/* 深度压缩的 Nebula CSS */
.nebula-app{min-height:100vh;background:#06060f;font-family:'Inter','SF Pro Display',system-ui,sans-serif;padding:60px 20px;box-sizing:border-box;display:flex;flex-direction:column;align-items:center;position:relative;overflow-x:hidden;color:#e8e8f0;}
.nebula-particles{position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:0;pointer-events:none;}
.nebula-halos{position:fixed;inset:0;z-index:0;pointer-events:none;overflow:hidden;}
.halo{position:absolute;border-radius:50%;filter:blur(120px);opacity:0.5;animation:float 15s ease-in-out infinite;}
.halo-1{width:400px;height:400px;background:radial-gradient(circle,#7c6ff7,transparent);top:-10%;left:10%;}
.halo-2{width:500px;height:500px;background:radial-gradient(circle,#4c3fc4,transparent);bottom:-20%;right:-5%;animation-delay:-5s;}
.halo-3{width:350px;height:350px;background:radial-gradient(circle,#a99df9,transparent);top:40%;left:50%;animation-delay:-10s;}
@keyframes float{0%,100%{transform:translate(0,0) scale(1);}50%{transform:translate(30px,-50px) scale(1.1);}}
.glass-panel{background:rgba(18,18,36,0.65);backdrop-filter:blur(40px) saturate(140%);border:1px solid rgba(255,255,255,0.08);box-shadow:inset 0 1px 0 rgba(255,255,255,0.1),0 8px 32px rgba(0,0,0,0.4);border-radius:20px;position:relative;z-index:1;transition:all 0.3s cubic-bezier(0.4,0,0.2,1);}
.glass-panel:hover{border-color:rgba(255,255,255,0.12);box-shadow:inset 0 1px 0 rgba(255,255,255,0.15),0 12px 40px rgba(124,111,247,0.15);}
.glass-panel-inner{background:rgba(255,255,255,0.02);border:1px solid rgba(255,255,255,0.04);border-radius:16px;}
.nebula-nav-bar{position:absolute;top:24px;right:40px;display:flex;align-items:center;gap:16px;padding:8px 20px;border-radius:50px;}
.nav-btn,.nav-icon-btn{background:transparent;border:none;font-size:0.9rem;font-weight:600;cursor:pointer;display:flex;align-items:center;gap:6px;transition:color 0.3s;}
.text-accent{color:#a99df9;}.text-weak{color:#6b6b85;}
.nav-btn:hover,.nav-icon-btn:hover{color:#e8e8f0;}.nav-divider{width:1px;height:14px;background:rgba(255,255,255,0.1);}
.nebula-main-card{width:100%;max-width:850px;padding:40px;animation:cardIn 0.7s cubic-bezier(0.4,0,0.2,1) forwards;}
@keyframes cardIn{from{opacity:0;transform:translateY(30px) scale(0.96);}to{opacity:1;transform:translateY(0) scale(1);}}
.nebula-title{text-align:center;font-size:1.65rem;font-weight:700;letter-spacing:-0.02em;margin:0 0 32px 0;color:#e8e8f0;}
.nebula-input-wrapper, .nebula-textarea-wrapper{position:relative;background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.1);border-radius:12px;display:flex;align-items:center;padding:8px;transition:all 0.3s;overflow:hidden;margin-bottom:24px;}
.nebula-input-wrapper:focus-within,.nebula-textarea-wrapper:focus-within{background:rgba(18,18,36,0.8);border-color:transparent;box-shadow:none;}
.input-icon{position:absolute;left:16px;color:#6b6b85;font-size:20px;transition:color 0.3s;}
.nebula-input-wrapper:focus-within .input-icon{color:#a99df9;}.nebula-input{flex:1;background:transparent;border:none;color:#e8e8f0;font-size:0.95rem;padding:10px 10px 10px 44px;outline:none;}
.nebula-input::placeholder,.nebula-textarea::placeholder{color:#6b6b85;transition:all 0.3s;}
.nebula-input:focus::placeholder{opacity:0.5;transform:translateX(4px);}
.focus-line{position:absolute;bottom:0;left:0;height:2px;width:100%;background:linear-gradient(90deg,#7c6ff7,#a99df9,#7c6ff7);transform:scaleX(0);transform-origin:left;transition:transform 0.4s cubic-bezier(0.34,1.56,0.64,1);}
.nebula-input-wrapper:focus-within .focus-line,.nebula-textarea-wrapper:focus-within .focus-line{transform:scaleX(1);}
.btn-nebula-primary{background:linear-gradient(135deg,#7c6ff7,#5b4fcf,#4c3fc4);color:#fff;border:none;border-radius:10px;padding:10px 24px;font-weight:600;cursor:pointer;transition:all 0.3s cubic-bezier(0.4,0,0.2,1);position:relative;overflow:hidden;}
.btn-add{padding:10px 20px;border-radius:8px;}
.btn-nebula-primary::before{content:'';position:absolute;top:0;left:-100%;width:50%;height:100%;background:linear-gradient(90deg,transparent,rgba(255,255,255,0.2),transparent);transition:left 0.5s;}
.btn-nebula-primary:hover{transform:translateY(-2px);box-shadow:0 6px 20px rgba(124,111,247,0.4);}
.btn-nebula-primary:hover::before{left:150%;}.btn-nebula-primary:active{transform:scale(0.98);}
.nebula-controls{display:flex;justify-content:space-between;align-items:center;margin-bottom:24px;gap:16px;}
.nebula-tabs{display:flex;background:rgba(255,255,255,0.04);padding:4px;border-radius:14px;border:1px solid rgba(255,255,255,0.05);}
.nebula-tab{background:transparent;border:none;color:#9898b4;padding:8px 20px;border-radius:10px;font-size:0.9rem;font-weight:600;cursor:pointer;transition:all 0.3s;display:flex;align-items:center;gap:6px;}
.nebula-tab span{background:rgba(255,255,255,0.1);padding:2px 8px;border-radius:20px;font-size:0.8rem;}
.nebula-tab.active{background:rgba(255,255,255,0.1);color:#e8e8f0;box-shadow:0 2px 8px rgba(0,0,0,0.2);}
.nebula-tab.active span{background:#7c6ff7;color:#fff;}
.btn-nebula-sort{background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.08);color:#9898b4;padding:8px 16px;border-radius:12px;font-size:0.9rem;font-weight:600;cursor:pointer;display:flex;align-items:center;gap:6px;transition:all 0.3s;}
.btn-nebula-sort:hover{background:rgba(255,255,255,0.08);color:#e8e8f0;}
.btn-nebula-sort.is-active{background:rgba(124,111,247,0.15);border-color:#7c6ff7;color:#a99df9;box-shadow:0 4px 12px rgba(124,111,247,0.2);}
.nebula-task-row{display:flex;justify-content:space-between;align-items:center;padding:16px 20px;margin-bottom:12px;transition:all 0.3s;}
.nebula-task-row:hover{background:rgba(255,255,255,0.06);transform:translateX(4px);}
.row-left{display:flex;align-items:center;gap:16px;flex:1;min-width:0;}
.nebula-checkbox-wrapper{position:relative;width:20px;height:20px;display:inline-block;cursor:pointer;}
.hide-native-checkbox{opacity:0;width:0;height:0;position:absolute;}
.custom-checkbox{width:20px;height:20px;border:1.5px solid rgba(255,255,255,0.2);border-radius:6px;background:rgba(0,0,0,0.2);transition:all 0.4s cubic-bezier(0.34,1.56,0.64,1);display:flex;justify-content:center;align-items:center;}
.hide-native-checkbox:focus+.custom-checkbox{outline:2px solid rgba(169,157,249,0.5);outline-offset:2px;}
.hide-native-checkbox:checked+.custom-checkbox{background:#7c6ff7;border-color:#7c6ff7;}
.check-icon{font-size:14px;color:#fff;opacity:0;transform:scale(0.5) rotate(-20deg);transition:all 0.4s cubic-bezier(0.34,1.56,0.64,1);}
.hide-native-checkbox:checked+.custom-checkbox .check-icon{opacity:1;transform:scale(1) rotate(0deg);}
.row-title{font-size:0.95rem;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;transition:color 0.3s;}
.is-completed .row-title{color:#6b6b85;text-decoration:line-through;text-decoration-color:rgba(255,255,255,0.2);}.row-right{display:flex;align-items:center;gap:16px;flex-shrink:0;}
.row-time{font-size:0.8rem;color:#6b6b85;display:flex;align-items:center;gap:4px;font-family:monospace;}
.time-icon{font-size:14px;}
.nebula-select-wrapper{position:relative;background:rgba(255,255,255,0.06);border:1px solid rgba(255,255,255,0.04);border-radius:8px;padding:2px 4px;transition:all 0.3s;}
.nebula-select-wrapper:hover{background:rgba(255,255,255,0.1);box-shadow:0 0 12px rgba(255,255,255,0.05);}
.nebula-select-wrapper select{appearance:none;background:transparent;border:none;color:#e8e8f0;font-size:0.8rem;font-weight:600;padding:4px 24px 4px 8px;cursor:pointer;outline:none;}
.nebula-select-wrapper select option{background:#121224;color:#e8e8f0;}
.select-arrow{position:absolute;right:4px;top:50%;transform:translateY(-50%);font-size:14px;color:#9898b4;pointer-events:none;}.priority-高{background:rgba(245,108,108,0.15);border-color:rgba(245,108,108,0.3);}
.priority-高 select{color:#f56c6c;}.priority-中{background:rgba(245,158,11,0.15);border-color:rgba(245,158,11,0.3);}
.priority-中 select{color:#f59e0b;}
.row-actions{display:flex;gap:4px;}
.nebula-icon-btn{background:transparent;border:none;color:#6b6b85;padding:6px;border-radius:8px;cursor:pointer;transition:all 0.2s;}
.nebula-icon-btn:hover{background:rgba(255,255,255,0.1);color:#e8e8f0;}
.action-danger:hover{background:rgba(245,108,108,0.15);color:#f56c6c;}
.nebula-toast{position:fixed;top:28px;left:50%;transform:translateX(-50%);padding:12px 24px;border-radius:50px;background:rgba(18,18,36,0.85);backdrop-filter:blur(20px);border:1px solid rgba(255,255,255,0.1);display:flex;align-items:center;gap:8px;font-size:0.9rem;font-weight:600;z-index:9999;box-shadow:0 10px 30px rgba(0,0,0,0.5);}.toast-success{border-color:rgba(74,222,128,0.5);box-shadow:0 0 20px rgba(74,222,128,0.1);}
.toast-success .toast-icon{color:#4ade80;}.toast-error{border-color:rgba(245,108,108,0.5);box-shadow:0 0 20px rgba(245,108,108,0.1);}
.toast-error .toast-icon{color:#f56c6c;}.toast-fade-enter-active,.toast-fade-leave-active{transition:all 0.4s cubic-bezier(0.34,1.56,0.64,1);}.toast-fade-enter-from,.toast-fade-leave-to{opacity:0;transform:translate(-50%,-120px);}
.nebula-modal-overlay{position:fixed;inset:0;background:rgba(6,6,15,0.6);backdrop-filter:blur(8px);display:flex;justify-content:center;align-items:center;z-index:9999;}
.nebula-modal-content{width:100%;max-width:520px;padding:32px;}
.modal-mini{max-width:400px;text-align:center;}.modal-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:28px;}
.modal-header h2{margin:0;font-size:1.4rem;font-weight:700;}.btn-close{background:transparent;border:none;color:#6b6b85;cursor:pointer;transition:0.3s;}
.btn-close:hover{color:#e8e8f0;transform:rotate(90deg);}
.detail-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:28px;}
.detail-item{display:flex;flex-direction:column;gap:6px;background:rgba(255,255,255,0.02);padding:12px;border-radius:12px;border:1px solid rgba(255,255,255,0.03);}
.full-width{grid-column:1/-1;}.label{font-size:0.8rem;color:#9898b4;text-transform:uppercase;letter-spacing:1px;}.value{font-size:0.95rem;font-weight:600;}
.text-glow{color:#a99df9;text-shadow:0 0 8px rgba(169,157,249,0.4);}
.text-success{color:#4ade80;}
.memo-area{display:flex;flex-direction:column;gap:12px;}
.nebula-textarea{width:100%;height:120px;background:transparent;border:none;color:#e8e8f0;font-size:0.95rem;resize:none;outline:none;line-height:1.6;}
.modal-footer{display:flex;justify-content:flex-end;gap:12px;margin-top:32px;}
.justify-center{justify-content:center;}
.btn-nebula-outline{background:transparent;border:1px solid rgba(255,255,255,0.15);color:#e8e8f0;border-radius:10px;padding:10px 24px;font-weight:600;cursor:pointer;transition:0.3s;}
.btn-nebula-outline:hover{background:rgba(255,255,255,0.05);border-color:#a99df9;}
.btn-nebula-danger{background:linear-gradient(135deg,#f56c6c,#c53030);color:#fff;border:none;border-radius:10px;padding:10px 24px;font-weight:600;cursor:pointer;transition:0.3s;}
.btn-nebula-danger:hover{transform:translateY(-2px);box-shadow:0 6px 20px rgba(245,108,108,0.3);}
.warning-icon-wrapper{width:64px;height:64px;background:rgba(245,108,108,0.1);border-radius:50%;display:flex;justify-content:center;align-items:center;margin:0 auto 20px auto;border:1px solid rgba(245,108,108,0.2);}
.text-center{text-align:center;}
.text-sub{color:#9898b4;font-size:0.9rem;margin-bottom:24px;}
.list-enter-active,.list-leave-active{transition:all 0.4s cubic-bezier(0.34,1.56,0.64,1);}.list-enter-from,.list-leave-to{opacity:0;transform:translateX(-30px);}
</style>