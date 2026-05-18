<template>
  <div class="glass-app-container">
    <transition name="toast-fade">
      <div v-if="toast.show" :class="['toast-wrapper', `toast-${toast.type}`]">
        <span class="material-icons toast-icon">{{ toast.type === 'success' ? 'check_circle' : 'error_outline' }}</span>
        {{ toast.message }}
      </div>
    </transition>
    <header class="nebula-header">
      <div class="header-left">
        <h1 class="nebula-title-main">
          <span class="material-icons tech-icon">biotech</span>
          思绪空间中枢
        </h1>
        <p class="nebula-subtitle">探索与构建你的数字第二大脑</p >
      </div>
    </header>

    <div class="nebula-tabs-container">
      <div class="nebula-tab active">
        <span class="tab-dot"></span>
        最近跃迁
      </div>
      <div class="nebula-tab">全部档案</div>
      <div class="nebula-tab">星标标记</div>
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
            <button class="btn-icon-primary" @click.stop="openRenameModal(nb.id, nb.name)" title="重命名空间">
              <span class="material-icons">edit_note</span>
            </button>
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
      <button class="btn-nebula-primary btn-huge" @click="openCreateModal">
        <span class="material-icons">add_circle</span>
        构建新思绪空间
      </button>
    </div>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="deleteConfirm.show" @click.self="deleteConfirm.show = false">
        <div class="nebula-modal-content glass-panel">

          <div class="modal-header">
            <h2 class="text-danger"><span class="material-icons">report_problem</span> 彻底抹除空间？</h2>
            <button class="btn-close-circle" @click="deleteConfirm.show = false">
              <span class="material-icons">close</span>
            </button>
          </div>

          <div class="modal-body">
            <p class="modal-desc">
              此操作将永久销毁 <strong>{{ deleteConfirm.name }}</strong> 及其内部所有 AI 记忆。无法回溯。
            </p >
          </div>
          <div class="modal-footer">
            <button class="btn-nebula-outline" @click="deleteConfirm.show = false">中止</button>
            <button class="btn-nebula-danger" @click="executeDeleteNotebook">确认抹除</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="showRenameModal" @click.self="showRenameModal = false">
        <div class="nebula-modal-content glass-panel">
          <div class="modal-header">
            <h2><span class="material-icons">edit</span> 重命名空间</h2>
            <button class="btn-close-circle" @click="showRenameModal = false">
              <span class="material-icons">close</span>
            </button>
          </div>
          <div class="modal-body">
            <div class="nebula-input-wrapper">
              <input
                  v-model="renameData.newName"
                  type="text"
                  class="nebula-input"
                  placeholder="输入新的空间名称..."
                  @keyup.enter="executeRename"
                  autofocus
              >
              <div class="focus-line"></div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-nebula-outline" @click="showRenameModal = false">放弃</button>
            <button class="btn-nebula-primary" @click="executeRename" :disabled="!renameData.newName.trim() || renameData.newName === renameData.oldName">
              确认修改
            </button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="showCreateModal" @click.self="showCreateModal = false">
        <div class="nebula-modal-content glass-panel">
          <div class="modal-header">
            <h2><span class="material-icons">add_circle_outline</span> 构建新思绪空间</h2>
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
                  placeholder="输入课题或科目名称..."
                  @keyup.enter="createNotebook"
                  autofocus
              />
              <div class="focus-line"></div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-nebula-outline" @click="showCreateModal = false">取消</button>
            <button class="btn-nebula-primary" @click="createNotebook" :disabled="!newNotebookName.trim()">
              跃迁构建
            </button>
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

// ==================== 【新增：全局 Toast 提示逻辑】 ====================
const toast = ref({ show: false, message: '', type: 'success' });
let toastTimer = null;
const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer);
  toast.value = { show: true, message, type };
  toastTimer = setTimeout(() => { toast.value.show = false; }, 3000);
};

// ==================== 【新增：重命名核心逻辑】 ====================
const showRenameModal = ref(false);
const renameData = ref({ id: null, newName: '', oldName: '' });

// 打开弹窗并初始化数据
const openRenameModal = (id, currentName) => {
  renameData.value = { id, newName: currentName, oldName: currentName };
  showRenameModal.value = true;
};

// 执行重命名提交
const executeRename = async () => {
  // 防御性校验
  if (!renameData.value.newName.trim() || renameData.value.newName === renameData.value.oldName) return;

  try {
    await request.put(`/api/notebooks/${renameData.value.id}`, { name: renameData.value.newName });
    showRenameModal.value = false;
    showToast('空间已成功重命名！', 'success');
    await fetchNotebooks(); // 刷新列表渲染新名字
  } catch (error) {
    console.error("重命名失败", error);
    showToast('重命名失败，请检查网络状态', 'error');
  }
};

// 🛡️ 修复：新增专门的方法来打开弹窗，清空上一次的残影
const openCreateModal = () => {
  newNotebookName.value = '';
  showCreateModal.value = true;
};

onMounted(() => {
  fetchNotebooks();
});
</script>

<style scoped>
/* 1. 基础布局与头部 */
.glass-app-container { min-height: 100vh; padding: 60px 80px; background: transparent; color: #e8e8f0; font-family: 'Inter', system-ui, sans-serif; box-sizing: border-box; }
.nebula-header { display: flex; justify-content: space-between; align-items: flex-start; max-width: 1000px; margin: 0 auto 40px; }
.nebula-title-main { font-size: 2rem; font-weight: 800; letter-spacing: -0.03em; margin: 0; display: flex; align-items: center; gap: 12px; color: #fff; text-shadow: 0 0 20px rgba(124, 111, 247, 0.3); }
.tech-icon { color: #7c6ff7; font-size: 32px; }
.nebula-subtitle { color: #9898b4; font-size: 0.95rem; margin: 8px 0 0; }

/* 2. 标签页 */
.nebula-tabs-container { display: flex; gap: 30px; max-width: 1000px; margin: 0 auto 32px; border-bottom: 1px solid rgba(255, 255, 255, 0.05); padding-bottom: 12px; position: relative; }
.nebula-tab { font-size: 0.9rem; font-weight: 600; color: #6b6b85; cursor: pointer; transition: all 0.3s; display: flex; align-items: center; gap: 8px; }
.nebula-tab.active { color: #a99df9; }
.tab-dot { width: 6px; height: 6px; background: #7c6ff7; border-radius: 50%; box-shadow: 0 0 8px #7c6ff7; }

/* 3. 卡片网格与动效 */
.notebook-grid { max-width: 1000px; margin: 0 auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 24px; padding-bottom: 120px; }
.nebula-card { position: relative; background: rgba(18, 18, 36, 0.65); backdrop-filter: blur(40px) saturate(140%); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 20px; padding: 24px; cursor: pointer; transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1); overflow: hidden; animation: cardEntry 0.7s cubic-bezier(0.22, 1, 0.36, 1) both; animation-delay: var(--entry-delay); }
@keyframes cardEntry { from { opacity: 0; transform: translateY(30px) scale(0.96); } to { opacity: 1; transform: translateY(0) scale(1); } }
.nebula-card:hover { transform: translateY(-8px) scale(1.02); border-color: rgba(124, 111, 247, 0.4); background: rgba(25, 25, 50, 0.75); box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4), 0 0 20px rgba(124, 111, 247, 0.15); }
.card-glow-effect { position: absolute; width: 100px; height: 100px; background: radial-gradient(circle, rgba(124, 111, 247, 0.2) 0%, transparent 70%); top: -20px; right: -20px; pointer-events: none; transition: transform 0.5s; }
.nebula-card:hover .card-glow-effect { transform: scale(2); }
.card-content { display: flex; align-items: center; gap: 20px; position: relative; z-index: 1; }
.icon-orb { width: 56px; height: 56px; background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 16px; display: flex; align-items: center; justify-content: center; font-size: 28px; transition: 0.3s; }
.nebula-card:hover .icon-orb { background: rgba(124, 111, 247, 0.1); border-color: rgba(124, 111, 247, 0.3); transform: rotate(-5deg); }
.nb-info h3 { margin: 0; font-size: 1.15rem; font-weight: 700; color: #fff; }
.nb-meta { font-size: 0.8rem; color: #6b6b85; display: flex; align-items: center; gap: 4px; margin-top: 6px; }
.nb-meta .material-icons { font-size: 14px; }

/* 4. 卡片操作按钮 */
.card-actions { display: flex; gap: 8px; }
.btn-icon-primary { background: transparent; border: none; color: #6b6b85; cursor: pointer; padding: 8px; border-radius: 10px; transition: 0.3s cubic-bezier(0.4, 0, 0.2, 1); display: flex; align-items: center; justify-content: center; }
.btn-icon-primary:hover { background: rgba(124, 111, 247, 0.1); color: #a99df9; }
.btn-icon-danger { background: transparent; border: none; color: #6b6b85; cursor: pointer; padding: 8px; border-radius: 10px; transition: 0.3s; }
.btn-icon-danger:hover { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }

/* 5. 全局通用按钮与空状态 */
.btn-nebula-primary { background: linear-gradient(135deg, #7c6ff7 0%, #5b4fcf 50%, #4c3fc4 100%); color: white; border: none; border-radius: 12px; padding: 12px 24px; font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 10px; transition: all 0.3s; box-shadow: 0 8px 24px rgba(124, 111, 247, 0.3); }
.btn-nebula-primary:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 12px 32px rgba(124, 111, 247, 0.5); filter: brightness(1.1); }
.btn-nebula-primary:active { transform: scale(0.98); }
.btn-nebula-outline { background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.1); color: #e8e8f0; border-radius: 12px; padding: 10px 20px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: 0.3s; }
.btn-nebula-outline:hover { background: rgba(255, 255, 255, 0.08); border-color: #7c6ff7; }
.btn-nebula-danger { background: linear-gradient(135deg, #f56c6c, #c53030); color: #fff; border: none; border-radius: 12px; padding: 12px 24px; font-weight: 700; cursor: pointer; transition: all 0.3s; }
.btn-nebula-danger:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(245, 108, 108, 0.3); }
.nebula-action-float { position: fixed; bottom: 50px; left: 50%; transform: translateX(-50%); z-index: 100; }
.btn-huge { padding: 18px 40px; font-size: 1.1rem; border-radius: 50px; }
.nebula-empty-state { grid-column: 1 / -1; text-align: center; padding: 100px 0; color: #6b6b85; }
.empty-orb { width: 120px; height: 120px; background: radial-gradient(circle, rgba(124, 111, 247, 0.1) 0%, transparent 70%); margin: 0 auto 20px; border-radius: 50%; border: 1px dashed rgba(124, 111, 247, 0.3); }


/* 6. Toast 全局提示框 */
.toast-wrapper { position: fixed; top: 28px; left: 50%; transform: translateX(-50%); padding: 12px 24px; border-radius: 50px; display: flex; align-items: center; gap: 8px; font-size: 0.9rem; font-weight: 600; z-index: 99999; backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.1); box-shadow: 0 12px 40px rgba(0,0,0,0.5); background: rgba(18,18,36,0.85); }
.toast-success { border-color: rgba(74,222,128,0.4); color: #4ade80; }
.toast-error { border-color: rgba(245,108,108,0.4); color: #f56c6c; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translate(-50%, -20px) scale(0.95); }

/* 7. 标准长方形弹窗 (整合所有修改) */
.nebula-modal-overlay { position: fixed; inset: 0; background: rgba(6, 6, 15, 0.7); backdrop-filter: blur(10px); display: flex; justify-content: center; align-items: center; z-index: 10001; }
.nebula-modal-content { width: 480px; max-width: 90vw; padding: 32px; background: rgba(18, 18, 36, 0.85); backdrop-filter: blur(40px) saturate(140%); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 20px; box-shadow: 0 25px 80px rgba(0, 0, 0, 0.6); display: flex; flex-direction: column; gap: 20px; text-align: left; }
.modal-header { position: relative; display: flex; justify-content: center; align-items: center; margin-bottom: 16px; }
.modal-header h2 { margin: 0; color: #fff; font-size: 1.3rem; display: flex; align-items: center; justify-content: center; gap: 10px; }
.modal-header .text-danger { color: #f56c6c; }
.modal-header .btn-close-circle { position: absolute; right: 0; top: 50%; transform: translateY(-50%); background: rgba(255, 255, 255, 0.05); border: none; color: #9898b4; border-radius: 50%; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: 0.3s; margin: 0; }
.modal-header .btn-close-circle:hover { background: rgba(255, 255, 255, 0.1); color: #fff; transform: translateY(-50%) rotate(90deg); }
.modal-desc { color: #9898b4; font-size: 0.95rem; line-height: 1.6; margin: 0; }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
.modal-footer button { padding: 10px 24px; font-size: 0.9rem; font-weight: 600; border-radius: 10px; cursor: pointer; transition: 0.3s; }
.nebula-input-wrapper { margin: 12px 0 0 0; position: relative; background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 12px; overflow: hidden; }
.nebula-input { width: 100%; background: transparent; border: none; padding: 14px 16px; color: #fff; font-size: 1rem; outline: none; }
.focus-line { position: absolute; bottom: 0; left: 0; height: 2px; width: 100%; background: linear-gradient(90deg, #7c6ff7, #a99df9, #7c6ff7); transform: scaleX(0); transition: 0.4s; }
.nebula-input:focus ~ .focus-line { transform: scaleX(1); }
.modal-fade-enter-active, .modal-fade-leave-active { transition: 0.3s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.9) translateY(20px); }
</style>