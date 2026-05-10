<template>
  <div class="glass-app-container">
    <header class="nebula-header">
      <div class="header-left">
        <h1 class="nebula-title-main">
          <span class="material-icons tech-icon">biotech</span>
          思绪空间中枢
        </h1>
        <p class="nebula-subtitle">探索与构建你的数字第二大脑</p >
      </div>
      <div class="header-right">
        <button class="btn-nebula-outline" @click="router.push('/tasks')">
          <span class="material-icons">dashboard</span>
          返回任务控制台
        </button>
      </div>
    </header>

    <div class="nebula-tabs-container">
      <div class="nebula-tab active">
        <span class="tab-dot"></span>
        最近跃迁
      </div>
      <div class="nebula-tab">全部档案</div>
      <div class="nebula-tab">星标标记</div>
      <div class="tabs-line"></div>
    </div>

    <div class="notebook-grid">
      <div
          class="nebula-card"
          v-for="(nb, index) in notebooks"
          :key="nb.id"
          @click="goToNotebook(nb.id)"
          :style="{ '--entry-delay': (index * 0.1) + 's' }"
      >
        <div class="card-glow-effect"></div>
        <div class="card-content">
          <div class="card-visual">
            <div class="icon-orb">
              <span class="emoji-icon">{{ emojis[index % emojis.length] }}</span>
            </div>
          </div>
          <div class="nb-info">
            <h3>{{ nb.name }}</h3>
            <div class="nb-meta">
              <span class="material-icons">schedule</span>
              {{ formatDate(nb.createdTime) }}
            </div>
          </div>
          <div class="card-actions">
            <button class="btn-icon-danger" @click.stop="triggerDeleteNotebook(nb.id, nb.name)" title="抹除空间">
              <span class="material-icons">auto_delete</span>
            </button>
          </div>
        </div>
      </div>

      <div v-if="notebooks.length === 0" class="nebula-empty-state">
        <div class="empty-orb"></div>
        <p>尚未观测到思绪空间，点击下方按钮开启初次跃迁</p >
      </div>
    </div>

    <div class="nebula-action-float">
      <button class="btn-nebula-primary btn-huge" @click="showCreateModal = true">
        <span class="material-icons">add_circle</span>
        构建新思绪空间
      </button>
    </div>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="showCreateModal" @click.self="showCreateModal = false">
        <div class="nebula-modal-content glass-panel">
          <div class="modal-header">
            <h2><span class="material-icons">create_new_folder</span> 开启新课题</h2>
            <button class="btn-close-circle" @click="showCreateModal = false">
              <span class="material-icons">close</span>
            </button>
          </div>
          <div class="modal-body">
            <div class="nebula-input-wrapper">
              <input
                  v-model="newNotebookName"
                  type="text"
                  class="nebula-input"
                  placeholder="输入空间全称 (如: 量子力学复习)..."
                  @keyup.enter="createNotebook"
                  autofocus
              >
              <div class="focus-line"></div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-nebula-outline" @click="showCreateModal = false">放弃</button>
            <button class="btn-nebula-primary" @click="createNotebook" :disabled="!newNotebookName.trim()">
              确认跃迁
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="deleteConfirm.show" @click.self="deleteConfirm.show = false">
        <div class="nebula-modal-content glass-panel danger-modal">
          <div class="danger-icon-wrapper">
            <span class="material-icons">report_problem</span>
          </div>
          <h2>彻底抹除空间？</h2>
          <p class="danger-desc">
            此操作将永久销毁 <strong>{{ deleteConfirm.name }}</strong> 及其内部所有 AI 记忆。
          </p >
          <div class="modal-footer">
            <button class="btn-nebula-outline" @click="deleteConfirm.show = false">中止</button>
            <button class="btn-nebula-danger" @click="executeDeleteNotebook">确认抹除</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';

const router = useRouter();
const notebooks = ref([]);
const showCreateModal = ref(false);
const newNotebookName = ref('');
const deleteConfirm = ref({ show: false, id: null, name: '' });

const emojis = ['💠', '🪐', '🧬', '⚛️', '🛰️'];

const fetchNotebooks = async () => {
  try {
    notebooks.value = await request.get('/api/notebooks');
  } catch (error) {
    console.error("获取失败", error);
  }
};

const triggerDeleteNotebook = (id, name) => {
  deleteConfirm.value = { show: true, id, name };
};

const executeDeleteNotebook = async () => {
  try {
    await request.delete(`/api/notebooks/${deleteConfirm.value.id}`);
    deleteConfirm.value.show = false;
    await fetchNotebooks();
  } catch (error) {
    console.error("删除失败");
  }
};

const createNotebook = async () => {
  if (!newNotebookName.value.trim()) return;
  try {
    await request.post('/api/notebooks', { name: newNotebookName.value });
    newNotebookName.value = '';
    showCreateModal.value = false;
    await fetchNotebooks();
  } catch (error) {
    console.error("创建失败");
  }
};

const goToNotebook = (id) => {
  router.push(`/copilot/${id}`);
};

const formatDate = (dateString) => {
  if (!dateString) return '刚刚激活';
  const date = new Date(dateString);
  return `${date.getFullYear()}.${date.getMonth() + 1}.${date.getDate()}`;
};

onMounted(() => {
  fetchNotebooks();
});
</script>

<style scoped>
/* 1. 基础布局 - 全透明承接底层星空 */
.glass-app-container {
  min-height: 100vh;
  padding: 60px 80px;
  background: transparent;
  color: #e8e8f0;
  font-family: 'Inter', system-ui, sans-serif;
  box-sizing: border-box;
}

/* 2. Header 样式 */
.nebula-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  max-width: 1000px;
  margin: 0 auto 40px;
}
.nebula-title-main {
  font-size: 2rem;
  font-weight: 800;
  letter-spacing: -0.03em;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #fff;
  text-shadow: 0 0 20px rgba(124, 111, 247, 0.3);
}
.tech-icon { color: #7c6ff7; font-size: 32px; }
.nebula-subtitle { color: #9898b4; font-size: 0.95rem; margin: 8px 0 0; }

/* 3. Nebula Tabs 样式 */
.nebula-tabs-container {
  display: flex;
  gap: 30px;
  max-width: 1000px;
  margin: 0 auto 32px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding-bottom: 12px;
  position: relative;
}
.nebula-tab {
  font-size: 0.9rem;
  font-weight: 600;
  color: #6b6b85;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}
.nebula-tab.active { color: #a99df9; }
.tab-dot { width: 6px; height: 6px; background: #7c6ff7; border-radius: 50%; box-shadow: 0 0 8px #7c6ff7; }

/* 4. 核心组件：Nebula 玻璃态卡片 */
.notebook-grid {
  max-width: 1000px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
  padding-bottom: 120px;
}

.nebula-card {
  position: relative;
  background: rgba(18, 18, 36, 0.65);
  backdrop-filter: blur(40px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  animation: cardEntry 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: var(--entry-delay);
}

@keyframes cardEntry {
  from { opacity: 0; transform: translateY(30px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.nebula-card:hover {
  transform: translateY(-8px) scale(1.02);
  border-color: rgba(124, 111, 247, 0.4);
  background: rgba(25, 25, 50, 0.75);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4), 0 0 20px rgba(124, 111, 247, 0.15);
}

/* 卡片内部光晕 */
.card-glow-effect {
  position: absolute;
  width: 100px; height: 100px;
  background: radial-gradient(circle, rgba(124, 111, 247, 0.2) 0%, transparent 70%);
  top: -20px; right: -20px;
  pointer-events: none;
  transition: transform 0.5s;
}
.nebula-card:hover .card-glow-effect { transform: scale(2); }

.card-content { display: flex; align-items: center; gap: 20px; position: relative; z-index: 1; }
.icon-orb {
  width: 56px; height: 56px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px;
  transition: 0.3s;
}
.nebula-card:hover .icon-orb { background: rgba(124, 111, 247, 0.1); border-color: rgba(124, 111, 247, 0.3); transform: rotate(-5deg); }

.nb-info h3 { margin: 0; font-size: 1.15rem; font-weight: 700; color: #fff; }
.nb-meta { font-size: 0.8rem; color: #6b6b85; display: flex; align-items: center; gap: 4px; margin-top: 6px; }
.nb-meta .material-icons { font-size: 14px; }

/* 5. 按钮系统 */
.btn-nebula-primary {
  background: linear-gradient(135deg, #7c6ff7 0%, #5b4fcf 50%, #4c3fc4 100%);
  color: white; border: none; border-radius: 12px;
  padding: 12px 24px; font-weight: 700; cursor: pointer;
  display: flex; align-items: center; gap: 10px;
  transition: all 0.3s;
  box-shadow: 0 8px 24px rgba(124, 111, 247, 0.3);
}
.btn-nebula-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(124, 111, 247, 0.5);
  filter: brightness(1.1);
}
.btn-nebula-primary:active { transform: scale(0.98); }

.btn-nebula-outline {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #e8e8f0; border-radius: 12px;
  padding: 10px 20px; font-weight: 600; cursor: pointer;
  display: flex; align-items: center; gap: 8px;
  transition: 0.3s;
}
.btn-nebula-outline:hover { background: rgba(255, 255, 255, 0.08); border-color: #7c6ff7; }

.btn-icon-danger {
  background: transparent; border: none; color: #6b6b85; cursor: pointer;
  padding: 8px; border-radius: 10px; transition: 0.3s;
}
.btn-icon-danger:hover { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }

/* 6. 底部浮动区 */
.nebula-action-float {
  position: fixed; bottom: 50px; left: 50%; transform: translateX(-50%);
  z-index: 100;
}
.btn-huge { padding: 18px 40px; font-size: 1.1rem; border-radius: 50px; }

/* 7. 玻璃态弹窗样式 */
.glass-panel {
  background: rgba(18, 18, 36, 0.85);
  backdrop-filter: blur(40px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.6);
}
.modal-header h2 { margin: 0; color: #fff; font-size: 1.5rem; display: flex; align-items: center; gap: 12px; }
.btn-close-circle {
  background: rgba(255, 255, 255, 0.05); border: none; color: #9898b4; border-radius: 50%;
  width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: 0.3s;
}
.btn-close-circle:hover { background: rgba(255, 255, 255, 0.1); color: #fff; transform: rotate(90deg); }

.nebula-input-wrapper {
  position: relative; background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 14px;
  margin: 30px 0; overflow: hidden;
}
.nebula-input {
  width: 100%; background: transparent; border: none; padding: 18px 20px;
  color: #fff; font-size: 1.05rem; outline: none;
}
.focus-line {
  position: absolute; bottom: 0; left: 0; height: 2px; width: 100%;
  background: linear-gradient(90deg, #7c6ff7, #a99df9, #7c6ff7);
  transform: scaleX(0); transition: 0.4s;
}
.nebula-input:focus ~ .focus-line { transform: scaleX(1); }

.modal-footer { display: flex; justify-content: flex-end; gap: 16px; margin-top: 10px; }

/* 危险状态弹窗 */
.danger-modal { text-align: center; }
.danger-icon-wrapper {
  width: 72px; height: 72px; background: rgba(245, 108, 108, 0.1);
  color: #f56c6c; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  margin: 0 auto 20px; font-size: 40px; border: 1px solid rgba(245, 108, 108, 0.2);
}
.btn-nebula-danger {
  background: linear-gradient(135deg, #f56c6c, #c53030);
  color: #fff; border: none; border-radius: 12px; padding: 12px 24px; font-weight: 700; cursor: pointer;
  transition: all 0.3s;
}
.btn-nebula-danger:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(245, 108, 108, 0.3); }

/* 动效 */
.modal-fade-enter-active, .modal-fade-leave-active { transition: 0.3s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.9) translateY(20px); }

/* 空状态 */
.nebula-empty-state {
  grid-column: 1 / -1; text-align: center; padding: 100px 0; color: #6b6b85;
}
.empty-orb {
  width: 120px; height: 120px; background: radial-gradient(circle, rgba(124, 111, 247, 0.1) 0%, transparent 70%);
  margin: 0 auto 20px; border-radius: 50%; border: 1px dashed rgba(124, 111, 247, 0.3);
}
</style>