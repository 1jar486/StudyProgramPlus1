<template>
  <div class="app-container">
    <header class="vibe-header">
      <div class="header-left">
        <h1>
          <span class="material-icons title-icon">auto_awesome</span>
          今日清单
        </h1>
        <p class="current-date">{{ currentDate }}</p >
      </div>

      <div class="header-right">
        <button class="btn-pill btn-ai" @click="router.push('/copilot')">
          <span class="material-icons icon-sm">auto_awesome</span>
          进入 AI 备考外脑
        </button>

        <button class="btn-pill btn-outline" @click="handleLogout">
          登出账号
        </button>

        <button class="btn-pill btn-icon" @click="toggleTheme">
          <span class="material-icons">{{ isDark ? 'light_mode' : 'dark_mode' }}</span>
        </button>
      </div>
    </header>

    <div class="vibe-input-cabin">
      <div class="input-group main-input">
        <span class="material-icons group-icon">track_changes</span>
        <input
            type="text"
            v-model="newTask.title"
            placeholder="写下你的下一个目标..."
            autofocus
            @keyup.enter="handleAddTask"
        >
      </div>

      <div class="input-group select-group">
        <span class="material-icons group-icon icon-bolt">bolt</span>
        <select v-model="newTask.priority">
          <option value="高">高优先级</option>
          <option value="中" selected>中优先级</option>
          <option value="低">低优先级</option>
        </select>
        <span class="material-icons dropdown-arrow">expand_more</span>
      </div>

      <div class="input-group select-group">
        <span class="material-icons group-icon icon-bag">business_center</span>
        <select v-model="newTask.tag">
          <option value="学习">学习</option>
          <option value="生活">生活</option>
          <option value="兼职">副业</option>
        </select>
        <span class="material-icons dropdown-arrow">expand_more</span>
      </div>

      <button class="btn-add-task" @click="handleAddTask">
        添加 <span class="material-icons">add</span>
      </button>
    </div>

    <!-- 替换后的统计栏 -->
    <div class="vibe-stats-bar">
      <span class="stat-item">全部 <span class="stat-num">{{ totalCount }}</span></span>
      <span class="stat-item">已完成 <span class="stat-num">{{ completedCount }}</span></span>
    </div>

    <div class="vibe-task-board">
      <div class="vibe-board-column" v-for="col in boardColumns" :key="col.name">
        <div class="column-header-centered">
          <span class="material-icons column-icon">{{ col.icon }}</span>
          <span class="column-name">{{ col.name }}</span>
        </div>

        <div class="task-container">
          <div
              v-for="(task, index) in col.tasks"
              :key="task.id"
              :class="['vibe-task-card', { 'completed': task.completed }, `border-${task.priority}`]"
              style="animation: fadeUp 0.4s ease;"
          >
            <div class="card-top">
              <div class="card-badges">
                <span :class="`badge-vibe badge-priority-${task.priority}`">
                  <span class="material-icons badge-icon">{{ priorityIcons[task.priority] }}</span>
                  {{ task.priority }}
                </span>
                <span class="badge-vibe badge-tag">
                  <span class="material-icons badge-icon">{{ tagIcons[task.tag] }}</span>
                  {{ task.tag }}
                </span>
              </div>
              <input type="checkbox" class="vibe-checkbox" :checked="task.completed" @change="toggleTask(task.id)">
            </div>

            <div class="card-middle">
              <div class="task-index">{{ index + 1 }}</div>
              <div class="task-title">{{ task.title }}</div>
            </div>

            <div class="card-bottom">
              <span class="task-id">#{{ task.id }}</span>
              <div class="bottom-right">
                <span class="task-date">
                  <span class="material-icons icon-date">calendar_today</span> 今天
                </span>
                <button class="btn-more action-detail" @click="openTaskDetail(task)" title="任务详情/备忘">
                  <span class="material-icons" style="font-size: 18px;">edit_document</span>
                </button>
                <button class="btn-more action-delete" @click="deleteTask(task.id)" title="删除任务">
                  <span class="material-icons" style="font-size: 18px;">delete_outline</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <transition name="modal-fade">
      <div class="vibe-modal-overlay" v-if="showDetailModal" @click.self="closeDetailModal">
        <div class="vibe-modal-content glass-effect">
          <div class="modal-header">
            <h2><span class="material-icons title-icon">auto_awesome</span> 任务详情</h2>
            <button class="btn-icon" @click="closeDetailModal"><span class="material-icons">close</span></button>
          </div>

          <div class="modal-body" v-if="currentTask">
            <div class="detail-row">
              <span class="detail-label">任务名称</span>
              <span class="detail-value task-title-large">{{ currentTask.title }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">核心属性</span>
              <div class="card-badges">
                <span :class="`badge-vibe badge-priority-${currentTask.priority}`">{{ currentTask.priority }}优先级</span>
                <span class="badge-vibe badge-tag">{{ currentTask.tag }}</span>
                <span class="badge-vibe" :style="{ background: currentTask.completed ? '#D1FAE5' : '#FEF3C7', color: currentTask.completed ? '#065F46' : '#92400E' }">
                  {{ currentTask.completed ? '已完成' : '进行中' }}
                </span>
              </div>
            </div>

            <div class="memo-section">
              <label class="detail-label">任务备忘 / 笔记区</label>
              <textarea
                  v-model="currentTask.memo"
                  class="memo-input"
                  placeholder="在这里写下你的思路、待办步骤或灵感..."
              ></textarea>
            </div>
          </div>

          <div class="modal-footer">
            <button class="btn-pill btn-outline" @click="closeDetailModal">关闭</button>
            <button class="btn-pill btn-ai" @click="saveDetailModal">保存备忘录</button>
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
// 确保样式文件被引入
import '../assets/style.css';

const router = useRouter();
const tasks = ref([]);
const isDark = ref(false);

const newTask = ref({
  title: '',
  priority: '中',
  tag: '学习'
});

const priorityIcons = { "高": "error", "中": "warning", "低": "check_circle" };
const tagIcons = { "学习": "school", "生活": "home", "兼职": "work" };
const priorityWeight = { "高": 3, "中": 2, "低": 1 };

const currentDate = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' });

const totalCount = computed(() => tasks.value.length);
const completedCount = computed(() => tasks.value.filter(t => t.completed).length);
// --- 新增：任务详情与备忘录逻辑 ---
const showDetailModal = ref(false);
const currentTask = ref(null);

const openTaskDetail = (task) => {
  // 关键：如果后端返回的是 null，我们给它一个空字符串，防止输入框报错
  currentTask.value = {
    ...task,
    memo: task.memo || ''
  };
  showDetailModal.value = true;
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  currentTask.value = null;
};

const saveDetailModal = async () => {
  if (!currentTask.value) return;

  try {
    // 调用你刚刚在后端写好的 RESTful 接口
    // 路径：/api/tasks/{id}/memo
    await request.put(`/api/tasks/${currentTask.value.id}/memo`, {
      memo: currentTask.value.memo
    });

    // 提示成功（可以使用你之前封装的 showToast，如果没有就用普通的）
    alert("备忘录已成功同步至云端数据库！");

    // 关闭弹窗
    showDetailModal.value = false;

    // 刷新列表，确保页面上的数据是最新的
    fetchTasks();
  } catch (error) {
    console.error("保存备忘录失败:", error);
    alert("保存失败，请检查后端服务是否启动");
  }
};

const sortedTasks = computed(() => {
  return [...tasks.value].sort((a, b) => {
    if (priorityWeight[a.priority] !== priorityWeight[b.priority]) {
      return priorityWeight[b.priority] - priorityWeight[a.priority];
    }
    return b.id - a.id;
  });
});

const boardColumns = computed(() => [
  { name: '学习', icon: 'school', tasks: sortedTasks.value.filter(t => t.tag === '学习') },
  { name: '生活', icon: 'home', tasks: sortedTasks.value.filter(t => t.tag === '生活') },
  { name: '兼职', icon: 'work', tasks: sortedTasks.value.filter(t => t.tag === '兼职') }
]);

// 1. 获取任务
const fetchTasks = async () => {
  document.body.style.cursor = "progress";
  try {
    tasks.value = await request.get('/api/tasks');
  } catch (error) {
    console.error("获取任务失败", error);
  } finally {
    document.body.style.cursor = "default";
  }
};

// 2. 添加任务（含你原有的缩放微交互动画）
const handleAddTask = async () => {
  if (!newTask.value.title.trim()) return;
  try {
    await request.post('/api/tasks', newTask.value);

    // 保留你原有的 DOM 缩放动画逻辑
    const input = document.querySelector('input[type="text"]');
    if(input) {
      input.style.transform = "scale(1.05)";
      setTimeout(() => input.style.transform = "scale(1)", 150);
    }

    newTask.value.title = '';
    fetchTasks();
  } catch (error) {
    alert("提交失败");
  }
};

// 3. 切换状态
const toggleTask = async (id) => {
  try {
    await request.put(`/api/tasks/${id}`);
    fetchTasks();
  } catch (error) {
    console.error("更新失败", error);
  }
};

// 4. 删除任务
const deleteTask = async (id) => {
  if (!confirm("确定删除该任务？")) return;
  try {
    await request.delete(`/api/tasks/${id}`);
    fetchTasks();
  } catch (error) {
    console.error("删除失败", error);
  }
};

// 5. 登出逻辑
const handleLogout = () => {
  localStorage.removeItem('token');
  router.push('/login');
};

// 6. 主题切换
const toggleTheme = () => {
  isDark.value = !isDark.value;
  if (isDark.value) {
    document.documentElement.setAttribute('data-theme', 'dark');
    localStorage.setItem('theme', 'dark');
  } else {
    document.documentElement.removeAttribute('data-theme');
    localStorage.setItem('theme', 'light');
  }
};

onMounted(() => {
  // 初始化主题
  if (localStorage.getItem('theme') === 'dark') {
    isDark.value = true;
    document.documentElement.setAttribute('data-theme', 'dark');
  }
  fetchTasks();
});
</script>

<style scoped>
/* ========================
   第一步：全局氛围与 Header 重塑
   ======================== */

/* 覆盖全局容器，注入薄荷绿呼吸感渐变背景 */
.app-container {
  min-height: 100vh;
  max-width: 100% !important; /* 突破原有的宽度限制 */
  margin: 0;
  padding: 40px 60px;
  background: linear-gradient(135deg, #F0FDF4 0%, #F8FAFC 100%);
  /* 添加极其柔和的径向渐变光晕，模拟设计图的通透感 */
  background-image: radial-gradient(at 0% 50%, rgba(209, 250, 229, 0.5) 0, transparent 50%),
  radial-gradient(at 100% 100%, rgba(224, 242, 254, 0.4) 0, transparent 50%);
  font-family: system-ui, -apple-system, sans-serif;
  box-sizing: border-box;
}

/* Header 布局优化 */
.vibe-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto 32px auto; /* 居中对齐 */
}

/* 标题区排版 */
.header-left h1 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
  letter-spacing: 0.5px;
}

.title-icon {
  color: #34D399; /* 设计图中的薄荷绿主色 */
  font-size: 26px;
}

.current-date {
  color: #10B981; /* 稍深的绿色，保证可读性 */
  font-size: 13px;
  font-weight: 500;
  margin: 0;
  padding-left: 34px; /* 故意缩进，与上面的文字左对齐 */
}

/* 按钮区排版 */
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 终极胶囊按钮基础样式 (Pill-shape) */
.btn-pill {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 22px;
  border-radius: 999px; /* 实现完美的半圆胶囊效果 */
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.btn-pill:hover {
  transform: translateY(-2px);
}

/* AI 绿按钮 */
.btn-ai {
  background: #34D399;
  color: white;
}
.btn-ai:hover {
  background: #10B981;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

/* 白色线框按钮 (登出) */
.btn-outline {
  background: #ffffff;
  color: #374151;
}
.btn-outline:hover {
  background: #F9FAFB;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

/* 圆形图标按钮 (暗色模式) */
.btn-icon {
  background: #ffffff;
  color: #374151;
  padding: 0;
  width: 42px;
  height: 42px;
  border-radius: 50%; /* 正圆 */
}
.btn-icon:hover {
  background: #F9FAFB;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.icon-sm {
  font-size: 16px;
}

/* ========================
   第二步：超集成悬浮输入舱
   ======================== */

.vibe-input-cabin {
  display: flex;
  align-items: center;
  max-width: 1100px;
  margin: 0 auto 40px auto;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  padding: 8px 10px 8px 24px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.04);
  gap: 12px;
}

/* 内部组合样式 */
.input-group {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 48px;
}

.main-input {
  flex: 1; /* 让输入框占据主要空间 */
}

.group-icon {
  color: #10B981;
  font-size: 22px;
}

/* 输入框本体 */
.main-input input {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 16px;
  color: #1F2937;
  outline: none;
}

.main-input input::placeholder {
  color: #9CA3AF;
}

/* 下拉选择组合 */
.select-group {
  background: #F8FAFC;
  padding: 0 12px 0 16px;
  border-radius: 12px;
  border: 1px solid #F1F5F9;
  position: relative;
  transition: all 0.3s ease;
}

.select-group:hover {
  background: #F1F5F9;
  border-color: #E2E8F0;
}

.select-group select {
  appearance: none;
  background: transparent;
  border: none;
  outline: none;
  font-size: 14px;
  font-weight: 600;
  color: #4B5563;
  padding-right: 24px;
  cursor: pointer;
  z-index: 1;
}

.dropdown-arrow {
  position: absolute;
  right: 8px;
  color: #94A3B8;
  pointer-events: none;
  font-size: 20px;
}

/* 特殊图标颜色 */
.icon-bolt { color: #64748B; }
.icon-bag { color: #64748B; }

/* 渐变添加按钮 */
.btn-add-task {
  background: linear-gradient(135deg, #34D399, #10B981);
  color: white;
  border: none;
  padding: 0 24px;
  height: 48px;
  border-radius: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.2);
}

.btn-add-task:hover {
  transform: scale(1.02);
  box-shadow: 0 6px 16px rgba(16, 185, 129, 0.3);
}

.btn-add-task:active {
  transform: scale(0.98);
}

/* ========================
   第三步：看板与卡片深度还原
   ======================== */

/* 统计栏样式 */
.vibe-stats-bar {
  max-width: 1100px;
  margin: 0 auto 16px auto;
  display: flex;
  justify-content: space-between;
  padding: 0 8px;
}

.stat-item {
  color: #6B7280;
  font-size: 14px;
  font-weight: 500;
}

.stat-num {
  color: #10B981;
  font-weight: 700;
  font-size: 16px;
  margin-left: 4px;
}

/* 看板整体网格 */
.vibe-task-board {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

/* 列容器：圆角白底 */
.vibe-board-column {
  background: #ffffff;
  border-radius: 24px;
  padding: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
}

/* 列头部 */
.column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.column-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.column-icon {
  color: #34D399;
  font-size: 24px;
}

.column-name {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  letter-spacing: 0.5px;
}

.btn-col-add {
  background: #ECFDF5;
  color: #10B981;
  border: none;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-col-add:hover { background: #D1FAE5; }

/* --- 核心：任务卡片 --- */
.vibe-task-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #F3F4F6;
  border-left-width: 4px;
  border-left-style: solid;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.vibe-task-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.06);
}

.vibe-task-card.completed {
  opacity: 0.6;
  filter: grayscale(0.8);
}

/* 左侧优先级边条颜色 */
.border-高 { border-left-color: #EF4444; }
.border-中 { border-left-color: #F59E0B; }
.border-低 { border-left-color: #10B981; }

/* 卡片内部结构 */
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.card-badges {
  display: flex;
  gap: 8px;
}

.badge-vibe {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.badge-icon { font-size: 14px; }

/* 徽章配色体系 */
.badge-priority-高 { background: #FEF2F2; color: #DC2626; }
.badge-priority-中 { background: #FFFBEB; color: #D97706; }
.badge-priority-低 { background: #ECFDF5; color: #059669; }
.badge-tag { background: #F5F3FF; color: #7C3AED; } /* 原图中的紫色系标签 */

/* 复选框定制 */
.vibe-checkbox {
  width: 20px;
  height: 20px;
  accent-color: #34D399;
  cursor: pointer;
  border-radius: 6px;
}

/* 中部：大序号与标题 */
.card-middle {
  margin-bottom: 24px;
}

.task-index {
  font-size: 20px;
  font-weight: 800;
  color: #111827;
  margin-bottom: 6px;
  line-height: 1;
}

.task-title {
  font-size: 14px;
  color: #4B5563;
  line-height: 1.5;
  font-weight: 500;
}

/* 底部：辅助信息 */
.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px dashed #F3F4F6;
}

.task-id {
  color: #9CA3AF;
  font-size: 13px;
  font-family: ui-monospace, monospace;
}

.bottom-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-date {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #10B981;
  font-size: 13px;
  font-weight: 500;
}

.icon-date { font-size: 14px; }

/* 隐藏在三个点里的删除按钮 */
.btn-more {
  background: transparent;
  border: none;
  color: #9CA3AF;
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.btn-more:hover {
  background: #FEE2E2;
  color: #EF4444; /* 悬浮时变红，暗示删除操作 */
}

/* 列底部的查看全部 */
.column-footer {
  text-align: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #F3F4F6;
  color: #10B981;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.column-footer:hover { opacity: 0.7; }

/* ========================
   最新修正：居中、精简与弹窗
   ======================== */

/* 列标题绝对居中 */
.column-header-centered {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  width: 100%;
}

/* 操作按钮交互优化 */
.action-detail { color: #60A5FA; }
.action-detail:hover { background: #EFF6FF; color: #3B82F6; }

.action-delete { color: #9CA3AF; }
.action-delete:hover { background: #FEF2F2; color: #EF4444; }

/* --- 任务详情弹窗 (Glassmorphism Modal) --- */
.vibe-modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(17, 24, 39, 0.4);
  backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.vibe-modal-content {
  background: rgba(255, 255, 255, 0.95);
  width: 100%;
  max-width: 500px;
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255,255,255,1);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #F3F4F6;
  padding-bottom: 16px;
}

.modal-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 20px;
  color: #111827;
}

.detail-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.detail-label {
  font-size: 13px;
  color: #9CA3AF;
  font-weight: 600;
}

.task-title-large {
  font-size: 18px;
  font-weight: 700;
  color: #1F2937;
  line-height: 1.4;
}

.memo-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
}

.memo-input {
  width: 100%;
  height: 150px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 12px;
  padding: 16px;
  font-size: 14px;
  color: #374151;
  resize: none;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.memo-input:focus {
  background: #FFFFFF;
  border-color: #34D399;
  box-shadow: 0 0 0 4px rgba(52, 211, 153, 0.1);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #F3F4F6;
}

/* 弹窗淡入淡出动画 */
.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(10px);
}
</style>