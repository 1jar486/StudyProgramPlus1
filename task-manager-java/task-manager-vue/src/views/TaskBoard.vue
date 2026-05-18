<template>
  <div class="nebula-app">
    <transition name="toast-fade">
      <div v-if="toast.show" :class="['nebula-toast', `toast-${toast.type}`]">
        <span class="material-icons toast-icon">{{ toast.type === 'success' ? 'check_circle' : 'error_outline' }}</span>
        {{ toast.message }}
      </div>
    </transition>

    <div class="nebula-main-card glass-panel">
      <div class="header-flex">
        <h1 class="nebula-title">
          <span class="material-icons tech-icon">task_alt</span>
          任务控制台
        </h1>
      </div>

      <div class="nebula-input-wrapper">
        <span class="material-icons input-icon">add_task</span>
        <input type="text" class="nebula-input" v-model="newTask.title" placeholder="有什么新计划？..." @keyup.enter="handleAddTask" autofocus />
        <button class="btn-nebula-primary btn-add" @click="handleAddTask" :disabled="!newTask.title.trim()">注入序列</button>
        <div class="focus-line"></div>
      </div>

      <div class="nebula-controls">
        <div class="nebula-tabs">
          <button class="nebula-tab" :class="{ active: currentFilter === 'all' }" @click="currentFilter = 'all'">全部 <span>{{ totalCount }}</span></button>
          <button class="nebula-tab" :class="{ active: currentFilter === 'incomplete' }" @click="currentFilter = 'incomplete'">未完成 <span>{{ incompleteCount }}</span></button>
          <button class="nebula-tab" :class="{ active: currentFilter === 'complete' }" @click="currentFilter = 'complete'">已归档 <span>{{ completedCount }}</span></button>
        </div>
        <button class="btn-nebula-sort" :class="{ 'is-active': isSortByPriority }" @click="isSortByPriority = !isSortByPriority">
          <span class="material-icons sort-icon">{{ isSortByPriority ? 'done_all' : 'sort' }}</span> 优先级排序
        </button>
      </div>

      <div class="nebula-task-list custom-scrollbar">
        <transition-group name="list" tag="div">
          <div v-for="task in filteredTasks" :key="task.id" class="nebula-task-row glass-panel-inner" :class="{ 'is-completed': task.completed }">

            <div class="row-left">
              <label class="nebula-checkbox-wrapper">
                <input type="checkbox" class="hide-native-checkbox" :checked="task.completed" @change="toggleTask(task.id)">
                <div class="custom-checkbox"><span class="material-icons check-icon">done</span></div>
              </label>
              <div class="title-container" @dblclick="editTaskTitle(task)">
                <input v-if="task.isEditingTitle" v-model="task.editTitleTemp" @blur="saveTaskTitle(task)" @keyup.enter="saveTaskTitle(task)" class="inline-edit-input" autofocus />
                <span v-else class="row-title" :class="{ 'is-completed-text': task.completed }">{{ task.title }}</span>
              </div>
            </div>

            <div class="row-right">
              <span class="row-time">
                <span class="material-icons time-icon">{{ task.completed ? 'task_alt' : 'schedule' }}</span>
                {{ task.completed ? ('完成于 ' + formatExactTime(task.completedTime)) : ('创建于 ' + formatExactTime(task.createdTime)) }}
              </span>
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
        <div v-if="filteredTasks.length === 0" class="empty-state">未观测到符合条件的节点</div>
      </div>
    </div>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="showDetailModal" @click.self="closeDetailModal">
        <div class="nebula-modal-content glass-panel">
          <div class="modal-header">
            <h2>任务中枢参数</h2>
            <button class="btn-close" @click="closeDetailModal"><span class="material-icons">close</span></button>
          </div>
          <div class="modal-body" v-if="currentTask">
            <div class="detail-grid">
              <div class="detail-item"><span class="label">全息标识</span><span class="value text-glow">{{ currentTask.title }}</span></div>
              <div class="detail-item"><span class="label">节点状态</span><span class="value" :class="currentTask.completed ? 'text-success' : 'text-accent'">{{ currentTask.completed ? '✅ 已归档' : '⏳ 激活中' }}</span></div>
              <div class="detail-item"><span class="label">优先级</span><span class="value">{{ currentTask.priority }}</span></div>
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
          <div class="warning-icon-wrapper"><span class="material-icons">warning_amber</span></div>
          <h2>抹除该节点？</h2>
          <p class="text-sub">此操作将永久抹除数据，无法回溯。</p >
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
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';

const router = useRouter();
const tasks = ref([]);

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

// ================= 原生功能完美还原 =================
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

// 完美的筛选与优先级排序逻辑还原
const filteredTasks = computed(() => {
  let result = [...tasks.value];
  if (currentFilter.value === 'incomplete') result = result.filter(t => !t.completed);
  else if (currentFilter.value === 'complete') result = result.filter(t => t.completed);
  return result.sort((a, b) => {
    if (isSortByPriority.value && priorityWeight[a.priority] !== priorityWeight[b.priority]) {
      return priorityWeight[b.priority] - priorityWeight[a.priority];
    }
    const safeTimeA = typeof a.createdTime === 'string' ? a.createdTime.replace(' ', 'T') : a.createdTime;
    const safeTimeB = typeof b.createdTime === 'string' ? b.createdTime.replace(' ', 'T') : b.createdTime;
    const timeA = new Date(safeTimeA || 0).getTime(), timeB = new Date(safeTimeB || 0).getTime();
    return (isNaN(timeB) ? 0 : timeB) - (isNaN(timeA) ? 0 : timeA);
  });
});

// ================= API 对接完美还原 =================
const fetchTasks = async () => {
  try { tasks.value = await request.get('/api/tasks'); }
  catch (error) { console.error("获取任务失败", error); }
};

const handleAddTask = async () => {
  if (!newTask.value.title.trim()) return;
  try {
    await request.post('/api/tasks', newTask.value);
    newTask.value.title = '';
    await fetchTasks();
    showToast('录入成功', 'success');
  } catch (error) { showToast('添加失败', 'error'); }
};

const toggleTask = async (id) => {
  try {
    await request.put(`/api/tasks/${id}`);
    await fetchTasks();
  }
  catch (error) { showToast("状态更新失败", "error"); }
};

// 行内修改标签、优先级的接口还原
const handleInlineEdit = async (task) => {
  try {
    await request.put(`/api/tasks/${task.id}/core`, { title: task.title, priority: task.priority, tag: task.tag });
    showToast('属性已更新', 'success');
  } catch (error) {
    showToast('更新失败，正在回滚', 'error'); await fetchTasks();
  }
};

// ================= 双击修改标题功能 =================
const editTaskTitle = (task) => {
  task.isEditingTitle = true;
  task.editTitleTemp = task.title;
};

// 修复后：增加防空串拦截
const saveTaskTitle = async (task) => {
  task.isEditingTitle = false;
  // 如果修改为空，自动恢复原标题，阻止发送请求
  if (!task.editTitleTemp || !task.editTitleTemp.trim()) {
    task.editTitleTemp = task.title;
    return;
  }
  if (task.editTitleTemp.trim() !== task.title) {
    task.title = task.editTitleTemp.trim();
    await handleInlineEdit(task);
  }
};

// ================= 删除与详情流 =================
const triggerDeleteTask = (id) => { deleteTaskConfirm.value = { show: true, id }; };
const executeDeleteTask = async () => {
  try {
    await request.delete(`/api/tasks/${deleteTaskConfirm.value.id}`);
    deleteTaskConfirm.value.show = false; await fetchTasks();
    showToast('任务已成功抹除', 'success');
  } catch (error) { showToast('抹除失败', 'error'); }
};

const openTaskDetail = (task) => { currentTask.value = { ...task, memo: task.memo || '' }; showDetailModal.value = true; };
const closeDetailModal = () => { showDetailModal.value = false; currentTask.value = null; };

const saveDetailModal = async () => {
  if (!currentTask.value) return;
  try {
    await request.put(`/api/tasks/${currentTask.value.id}/memo`, { memo: currentTask.value.memo });
    showToast('备忘录已同步', 'success'); closeDetailModal(); await fetchTasks();
  } catch (error) { showToast('同步失败', 'error'); }
};

onMounted(() => { fetchTasks(); });
</script>

<style scoped>
/* 核心代码极其干净：删除了原本顶部栏和冗余按钮关联的所有无用 CSS，大幅减轻渲染压力 */
.nebula-app{min-height:100vh;background:transparent;font-family:'Inter','SF Pro Display',system-ui,sans-serif;padding:80px 20px 40px;box-sizing:border-box;display:flex;flex-direction:column;align-items:center;position:relative;overflow-x:hidden;color:#e8e8f0;}
.glass-panel{background:rgba(18,18,36,0.65);backdrop-filter:blur(40px) saturate(140%);border:1px solid rgba(255,255,255,0.08);box-shadow:inset 0 1px 0 rgba(255,255,255,0.1),0 8px 32px rgba(0,0,0,0.4);border-radius:20px;position:relative;z-index:1;transition:all 0.3s cubic-bezier(0.4,0,0.2,1);}
.glass-panel-inner{background:rgba(255,255,255,0.02);border:1px solid rgba(255,255,255,0.04);border-radius:16px;}
.nebula-main-card{width:100%;max-width:1100px;padding:40px;animation:cardIn 0.7s cubic-bezier(0.4,0,0.2,1) forwards;margin-top:20px;}
@keyframes cardIn{from{opacity:0;transform:translateY(30px) scale(0.96);}to{opacity:1;transform:translateY(0) scale(1);}}
.header-flex{display:flex;justify-content:center;align-items:center;margin-bottom:32px;width:100%;}
.nebula-title{font-size:1.8rem;font-weight:800;letter-spacing:-0.02em;margin:0;color:#e8e8f0;display:flex;align-items:center;gap:12px;text-align:center;text-shadow:0 0 20px rgba(124,111,247,0.35);}
.nebula-title .tech-icon{color:#7c6ff7;font-size:32px;filter:drop-shadow(0 0 8px rgba(124,111,247,0.6));}
.nebula-input-wrapper,.nebula-textarea-wrapper{position:relative;background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.1);border-radius:12px;display:flex;align-items:center;padding:8px;transition:all 0.3s;overflow:hidden;margin-bottom:24px;}
.nebula-input-wrapper:focus-within,.nebula-textarea-wrapper:focus-within{background:rgba(18,18,36,0.8);border-color:transparent;box-shadow:none;}
.input-icon{position:absolute;left:16px;color:#6b6b85;font-size:20px;transition:color 0.3s;}
.nebula-input-wrapper:focus-within .input-icon{color:#a99df9;}
.nebula-input{flex:1;background:transparent;border:none;color:#e8e8f0;font-size:1rem;padding:12px 10px 12px 44px;outline:none;}
.nebula-input::placeholder,.nebula-textarea::placeholder{color:#6b6b85;transition:all 0.3s;}
.nebula-input:focus::placeholder{opacity:0.5;transform:translateX(4px);}
.focus-line{position:absolute;bottom:0;left:0;height:2px;width:100%;background:linear-gradient(90deg,#7c6ff7,#a99df9,#7c6ff7);transform:scaleX(0);transform-origin:left;transition:transform 0.4s cubic-bezier(0.34,1.56,0.64,1);}
.nebula-input-wrapper:focus-within .focus-line,.nebula-textarea-wrapper:focus-within .focus-line{transform:scaleX(1);}
.btn-nebula-primary{background:linear-gradient(135deg,#7c6ff7,#5b4fcf,#4c3fc4);color:#fff;border:none;border-radius:10px;padding:12px 24px;font-weight:600;cursor:pointer;transition:all 0.3s;position:relative;overflow:hidden;}
.btn-add{padding:12px 28px;border-radius:10px;font-size:1rem;}
.btn-nebula-primary:hover:not(:disabled){transform:translateY(-2px);box-shadow:0 6px 20px rgba(124,111,247,0.4);}
.btn-nebula-primary:disabled{opacity:0.5;cursor:not-allowed;}
.nebula-controls{display:flex;justify-content:space-between;align-items:center;margin-bottom:24px;gap:16px;}
.nebula-tabs{display:flex;background:rgba(255,255,255,0.04);padding:4px;border-radius:14px;border:1px solid rgba(255,255,255,0.05);}
.nebula-tab{background:transparent;border:none;color:#9898b4;padding:8px 20px;border-radius:10px;font-size:0.9rem;font-weight:600;cursor:pointer;transition:all 0.3s;display:flex;align-items:center;gap:6px;}
.nebula-tab span{background:rgba(255,255,255,0.1);padding:2px 8px;border-radius:20px;font-size:0.8rem;}
.nebula-tab.active{background:rgba(255,255,255,0.1);color:#e8e8f0;box-shadow:0 2px 8px rgba(0,0,0,0.2);}
.nebula-tab.active span{background:#7c6ff7;color:#fff;}
.btn-nebula-sort{background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.08);color:#9898b4;padding:8px 16px;border-radius:12px;font-size:0.9rem;font-weight:600;cursor:pointer;display:flex;align-items:center;gap:6px;transition:all 0.3s;}
.btn-nebula-sort:hover{background:rgba(255,255,255,0.08);color:#e8e8f0;}
.btn-nebula-sort.is-active{background:rgba(124,111,247,0.15);border-color:#7c6ff7;color:#a99df9;box-shadow:0 4px 12px rgba(124,111,247,0.2);}
.nebula-task-list{max-height:500px;overflow-y:auto;padding-right:8px;}
.custom-scrollbar::-webkit-scrollbar{width:6px;}.custom-scrollbar::-webkit-scrollbar-thumb{background:rgba(255,255,255,0.1);border-radius:6px;}
.nebula-task-row{display:flex;justify-content:space-between;align-items:center;padding:16px 20px;margin-bottom:12px;transition:all 0.3s;}
.nebula-task-row:hover{background:rgba(255,255,255,0.06);transform:translateX(4px);border-color:rgba(124,111,247,0.3);}
.row-left{display:flex;align-items:center;gap:16px;flex:1;min-width:0;}
.nebula-checkbox-wrapper{position:relative;width:20px;height:20px;display:inline-block;cursor:pointer;}
.hide-native-checkbox{opacity:0;width:0;height:0;position:absolute;}
.custom-checkbox{width:20px;height:20px;border:1.5px solid rgba(255,255,255,0.2);border-radius:6px;background:rgba(0,0,0,0.2);transition:all 0.3s;display:flex;justify-content:center;align-items:center;}
.hide-native-checkbox:focus+.custom-checkbox{outline:2px solid rgba(169,157,249,0.5);outline-offset:2px;}
.hide-native-checkbox:checked+.custom-checkbox{background:#7c6ff7;border-color:#7c6ff7;}
.check-icon{font-size:14px;color:#fff;opacity:0;transform:scale(0.5) rotate(-20deg);transition:all 0.3s;}
.hide-native-checkbox:checked+.custom-checkbox .check-icon{opacity:1;transform:scale(1) rotate(0deg);}
.title-container{flex:1;min-width:0;cursor:text;}
.inline-edit-input{width:100%;background:rgba(18,18,36,0.8);border:1px solid #7c6ff7;color:#fff;padding:6px 10px;border-radius:8px;font-size:1rem;outline:none;}
.row-title{font-size:1.05rem;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;transition:color 0.3s;}
.is-completed-text{color:#6b6b85;text-decoration:line-through;text-decoration-color:rgba(255,255,255,0.3);}
.row-right{display:flex;align-items:center;gap:16px;flex-shrink:0;}
.row-time{font-size:0.85rem;color:#6b6b85;display:flex;align-items:center;gap:4px;font-family:monospace;}
.time-icon{font-size:14px;}
.nebula-select-wrapper{position:relative;background:rgba(255,255,255,0.06);border:1px solid rgba(255,255,255,0.04);border-radius:8px;padding:4px 6px;transition:all 0.3s;}
.nebula-select-wrapper:hover{background:rgba(255,255,255,0.1);box-shadow:0 0 12px rgba(255,255,255,0.05);}
.nebula-select-wrapper select{appearance:none;background:transparent;border:none;color:#e8e8f0;font-size:0.85rem;font-weight:600;padding:4px 28px 4px 10px;cursor:pointer;outline:none;}
.nebula-select-wrapper select option{background:#121224;color:#e8e8f0;}
.select-arrow{position:absolute;right:8px;top:50%;transform:translateY(-50%);font-size:16px;color:#9898b4;pointer-events:none;}
.priority-高{background:rgba(245,108,108,0.15);border-color:rgba(245,108,108,0.3);}
.priority-高 select{color:#f56c6c;}
.priority-中{background:rgba(245,158,11,0.15);border-color:rgba(245,158,11,0.3);}
.priority-中 select{color:#f59e0b;}
.priority-低{background:rgba(16,185,129,0.15);border-color:rgba(16,185,129,0.3);}
.priority-低 select{color:#10b981;}
.tag-wrapper{background:rgba(124,111,247,0.1);border-color:rgba(124,111,247,0.2);}
.tag-wrapper select{color:#a99df9;}
.row-actions{display:flex;gap:6px;}
.nebula-icon-btn{background:rgba(255,255,255,0.04);border:none;color:#9898b4;padding:8px;border-radius:8px;cursor:pointer;transition:all 0.2s;display:flex;align-items:center;}
.nebula-icon-btn:hover{background:rgba(255,255,255,0.1);color:#fff;transform:translateY(-2px);}
.action-danger:hover{background:rgba(245,108,108,0.15);color:#f56c6c;}
.empty-state{text-align:center;padding:60px 0;color:#6b6b85;font-size:1rem;}
.nebula-toast{position:fixed;top:28px;left:50%;transform:translateX(-50%);padding:12px 24px;border-radius:50px;background:rgba(18,18,36,0.85);backdrop-filter:blur(20px);border:1px solid rgba(255,255,255,0.1);display:flex;align-items:center;gap:8px;font-size:0.9rem;font-weight:600;z-index:9999;box-shadow:0 10px 30px rgba(0,0,0,0.5);}
.toast-success{border-color:rgba(74,222,128,0.5);box-shadow:0 0 20px rgba(74,222,128,0.1);}
.toast-success .toast-icon{color:#4ade80;}
.toast-error{border-color:rgba(245,108,108,0.5);box-shadow:0 0 20px rgba(245,108,108,0.1);}
.toast-error .toast-icon{color:#f56c6c;}
.toast-fade-enter-active,.toast-fade-leave-active{transition:all 0.4s cubic-bezier(0.34,1.56,0.64,1);}
.toast-fade-enter-from,.toast-fade-leave-to{opacity:0;transform:translate(-50%,-120px);}
.nebula-modal-overlay{position:fixed;inset:0;background:rgba(6,6,15,0.6);backdrop-filter:blur(8px);display:flex;justify-content:center;align-items:center;z-index:9999;}
.nebula-modal-content{width:100%;max-width:520px;padding:32px;}
.modal-mini{max-width:400px;text-align:center;}
.modal-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:28px;}
.modal-header h2{margin:0;font-size:1.4rem;font-weight:700;}
.btn-close{background:transparent;border:none;color:#6b6b85;cursor:pointer;transition:0.3s;padding:4px;}
.btn-close:hover{color:#fff;transform:rotate(90deg);}
.detail-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:28px;}
.detail-item{display:flex;flex-direction:column;gap:6px;background:rgba(255,255,255,0.02);padding:12px;border-radius:12px;border:1px solid rgba(255,255,255,0.03);}
.full-width{grid-column:1/-1;}
.label{font-size:0.8rem;color:#9898b4;text-transform:uppercase;letter-spacing:1px;}
.value{font-size:1rem;font-weight:600;}
.text-glow{color:#a99df9;text-shadow:0 0 8px rgba(169,157,249,0.4);}
.text-success{color:#4ade80;}
.memo-area{display:flex;flex-direction:column;gap:12px;}
.nebula-textarea{width:100%;height:120px;background:transparent;border:none;color:#e8e8f0;font-size:0.95rem;resize:none;outline:none;line-height:1.6;padding:12px;}
.modal-footer{display:flex;justify-content:flex-end;gap:12px;margin-top:32px;}
.justify-center{justify-content:center;}
.btn-nebula-outline{background:transparent;border:1px solid rgba(255,255,255,0.15);color:#e8e8f0;border-radius:10px;padding:10px 24px;font-weight:600;cursor:pointer;transition:0.3s;}
.btn-nebula-outline:hover{background:rgba(255,255,255,0.05);border-color:#a99df9;}
.btn-nebula-danger{background:linear-gradient(135deg,#f56c6c,#c53030);color:#fff;border:none;border-radius:10px;padding:10px 24px;font-weight:600;cursor:pointer;transition:0.3s;}
.btn-nebula-danger:hover{transform:translateY(-2px);box-shadow:0 6px 20px rgba(245,108,108,0.3);}
.warning-icon-wrapper{width:64px;height:64px;background:rgba(245,108,108,0.1);border-radius:50%;display:flex;justify-content:center;align-items:center;margin:0 auto 20px auto;border:1px solid rgba(245,108,108,0.2);}
.text-sub{color:#9898b4;font-size:0.9rem;margin-bottom:24px;}
.list-enter-active,.list-leave-active{transition:all 0.4s cubic-bezier(0.34,1.56,0.64,1);}
.list-enter-from,.list-leave-to{opacity:0;transform:translateX(-30px);}
.text-accent { color: #f59e0b; } /* 补充遗漏的激活状态颜色（琥珀色） */

</style>