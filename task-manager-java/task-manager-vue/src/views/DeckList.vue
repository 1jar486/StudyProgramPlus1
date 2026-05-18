<template>
  <div class="glass-app-container">
    <transition name="toast-fade">
      <div v-if="toast.show" :class="['toast-wrapper', `toast-${toast.type}`]">
        <span class="material-icons toast-icon">{{ toast.type === 'success' ? 'check_circle' : 'error_outline' }}</span>
        {{ toast.message }}
      </div>
    </transition>

    <header class="nebula-header" style="display: flex; justify-content: space-between; align-items: center; width: 100%;">

      <div class="header-left">
        <h1 class="nebula-title-main">
          <span class="material-icons tech-icon">style</span>
          记忆牌组大厅
        </h1>
        <p class="nebula-subtitle">基于 SM-2 智能算法的专属知识记忆库</p >
      </div>

    </header>

    <div class="notebook-grid">
      <div class="nebula-card" v-for="(deck, index) in decks" :key="deck.id" @click="goToDeckManager(deck.id)" :style="{ '--entry-delay': (index * 0.1) + 's' }">
        <div class="card-glow-effect"></div>
        <div class="card-content">
          <div class="card-visual">
            <div class="icon-orb"><span class="material-icons" style="color: #a99df9;">auto_awesome_motion</span></div>
          </div>
          <div class="nb-info">
            <h3>{{ deck.name }}</h3>
            <p class="deck-desc">{{ deck.description || '暂无描述' }}</p >
          </div>
          <div class="card-actions">
            <button class="btn-icon-primary" @click.stop="openRenameModal(deck)" title="编辑牌组">
              <span class="material-icons">edit</span>
            </button>
            <button class="btn-icon-danger" @click.stop="triggerDelete(deck)" title="删除牌组">
              <span class="material-icons">delete_sweep</span>
            </button>
          </div>
        </div>
      </div>

      <div v-if="decks.length === 0" class="nebula-empty-state">
        <div class="empty-orb"></div>
        <p>尚未构建任何记忆牌组，点击下方按钮开启记忆跃迁</p >
      </div>
    </div>

    <div class="nebula-action-float">
      <button class="btn-nebula-primary btn-huge" @click="openCreateModal">
        <span class="material-icons">add_box</span>构建新牌组
      </button>
    </div>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="showCreateModal" @click.self="showCreateModal = false">
        <div class="nebula-modal-content glass-panel">
          <div class="modal-header">
            <h2><span class="material-icons">library_add</span> 构建新牌组</h2>
            <button class="btn-close-circle" @click="showCreateModal = false"><span class="material-icons">close</span></button>
          </div>
          <div class="modal-body">
            <div class="nebula-input-wrapper">
              <input v-model="newDeckData.name" type="text" class="nebula-input" placeholder="输入牌组名称 (必填)..." autofocus>
              <div class="focus-line"></div>
            </div>
            <div class="nebula-input-wrapper mt-15">
              <input v-model="newDeckData.description" type="text" class="nebula-input" placeholder="输入牌组描述 (选填)..." @keyup.enter="executeCreate">
              <div class="focus-line"></div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-nebula-outline" @click="showCreateModal = false">取消</button>
            <button class="btn-nebula-primary" @click="executeCreate" :disabled="!newDeckData.name.trim()">确认构建</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="showRenameModal" @click.self="showRenameModal = false">
        <div class="nebula-modal-content glass-panel">
          <div class="modal-header">
            <h2><span class="material-icons">edit</span> 编辑牌组信息</h2>
            <button class="btn-close-circle" @click="showRenameModal = false"><span class="material-icons">close</span></button>
          </div>
          <div class="modal-body">
            <div class="nebula-input-wrapper">
              <input v-model="renameData.name" type="text" class="nebula-input" placeholder="牌组名称..." autofocus>
              <div class="focus-line"></div>
            </div>
            <div class="nebula-input-wrapper mt-15">
              <input v-model="renameData.description" type="text" class="nebula-input" placeholder="牌组描述..." @keyup.enter="executeRename">
              <div class="focus-line"></div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-nebula-outline" @click="showRenameModal = false">放弃</button>
            <button class="btn-nebula-primary" @click="executeRename" :disabled="!renameData.name.trim()">保存修改</button>
          </div>
        </div>
      </div>
    </transition>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="deleteConfirm.show" @click.self="deleteConfirm.show = false">
        <div class="nebula-modal-content glass-panel">
          <div class="modal-header">
            <h2 class="text-danger"><span class="material-icons">report_problem</span> 彻底抹除牌组？</h2>
            <button class="btn-close-circle" @click="deleteConfirm.show = false"><span class="material-icons">close</span></button>
          </div>
          <div class="modal-body">
            <p class="modal-desc">此操作将永久销毁 <strong>{{ deleteConfirm.name }}</strong> 及其内部的所有闪卡记忆。无法回溯。</p >
          </div>
          <div class="modal-footer">
            <button class="btn-nebula-outline" @click="deleteConfirm.show = false">中止</button>
            <button class="btn-nebula-danger" @click="executeDelete">确认抹除</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request'; // 假设你的 axios 封装在这里，且会自动携带 Token

const router = useRouter();

// 状态管理
const decks = ref([]);
const toast = ref({ show: false, message: '', type: 'success' });
let toastTimer = null;

const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer);
  toast.value = { show: true, message, type };
  toastTimer = setTimeout(() => { toast.value.show = false; }, 3000);
};

// 获取牌组列表
const fetchDecks = async () => {
  try {
    const res = await request.get('/api/decks');
    decks.value = res;
  } catch (error) {
    showToast('获取牌组失败，请检查网络', 'error');
  }
};

// ================= 构建牌组 =================
const showCreateModal = ref(false);
const newDeckData = ref({ name: '', description: '' });

const executeCreate = async () => {
  if (!newDeckData.value.name.trim()) return;
  try {
    await request.post('/api/decks', newDeckData.value);
    showCreateModal.value = false;
    newDeckData.value = { name: '', description: '' };
    showToast('牌组构建成功！', 'success');
    fetchDecks();
  } catch (error) {
    showToast('构建失败', 'error');
  }
};

// ================= 编辑牌组 =================
const showRenameModal = ref(false);
const renameData = ref({ id: null, name: '', description: '' });

const openRenameModal = (deck) => {
  renameData.value = { id: deck.id, name: deck.name, description: deck.description || '' };
  showRenameModal.value = true;
};

const executeRename = async () => {
  if (!renameData.value.name.trim()) return;
  try {
    await request.put(`/api/decks/${renameData.value.id}`, renameData.value);
    showRenameModal.value = false;
    showToast('牌组信息已更新', 'success');
    fetchDecks();
  } catch (error) {
    showToast('修改失败', 'error');
  }
};

// ================= 删除牌组 =================
const deleteConfirm = ref({ show: false, id: null, name: '' });

const triggerDelete = (deck) => {
  deleteConfirm.value = { show: true, id: deck.id, name: deck.name };
};

const executeDelete = async () => {
  try {
    await request.delete(`/api/decks/${deleteConfirm.value.id}`);
    deleteConfirm.value.show = false;
    showToast('牌组已彻底抹除', 'success');
    fetchDecks();
  } catch (error) {
    showToast('删除失败', 'error');
  }
};

// ================= 路由跳转 =================
const goToDeckManager = (deckId) => {
  // 我们下一步会写这个页面，专门用来管理这个牌组里的卡片
  router.push(`/decks/${deckId}/cards`);
};

// 🛡️ 修复：新增专门的方法，每次打开前强制清空数据
const openCreateModal = () => {
  newDeckData.value = { name: '', description: '' };
  showCreateModal.value = true;
};

onMounted(() => {
  fetchDecks();
});
</script>

<style scoped>
.glass-app-container { min-height: 100vh; padding: 60px 80px; background: transparent; color: #e8e8f0; font-family: 'Inter', system-ui, sans-serif; box-sizing: border-box; }
.nebula-header { display: flex; justify-content: space-between; align-items: flex-start; max-width: 1000px; margin: 0 auto 40px; }
.nebula-title-main { font-size: 2rem; font-weight: 800; letter-spacing: -0.03em; margin: 0; display: flex; align-items: center; gap: 12px; color: #fff; text-shadow: 0 0 20px rgba(124, 111, 247, 0.3); }
.tech-icon { color: #7c6ff7; font-size: 32px; }
.nebula-subtitle { color: #9898b4; font-size: 0.95rem; margin: 8px 0 0; }
.notebook-grid { max-width: 1000px; margin: 0 auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 24px; padding-bottom: 120px; }
.nebula-card { position: relative; background: rgba(18, 18, 36, 0.65); backdrop-filter: blur(40px) saturate(140%); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 20px; padding: 24px; cursor: pointer; transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1); overflow: hidden; animation: cardEntry 0.7s cubic-bezier(0.22, 1, 0.36, 1) both; animation-delay: var(--entry-delay); }
@keyframes cardEntry { from { opacity: 0; transform: translateY(30px) scale(0.96); } to { opacity: 1; transform: translateY(0) scale(1); } }
.nebula-card:hover { transform: translateY(-8px) scale(1.02); border-color: rgba(124, 111, 247, 0.4); background: rgba(25, 25, 50, 0.75); box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4), 0 0 20px rgba(124, 111, 247, 0.15); }
.card-glow-effect { position: absolute; width: 100px; height: 100px; background: radial-gradient(circle, rgba(124, 111, 247, 0.2) 0%, transparent 70%); top: -20px; right: -20px; pointer-events: none; transition: transform 0.5s; }
.nebula-card:hover .card-glow-effect { transform: scale(2); }
.card-content { display: flex; align-items: center; gap: 20px; position: relative; z-index: 1; }
.icon-orb { width: 50px; height: 50px; background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 24px; transition: 0.3s; }
.nebula-card:hover .icon-orb { background: rgba(124, 111, 247, 0.1); border-color: rgba(124, 111, 247, 0.3); transform: rotate(-5deg); }
.nb-info { flex: 1; overflow: hidden; }
.nb-info h3 { margin: 0; font-size: 1.15rem; font-weight: 700; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.deck-desc { font-size: 0.85rem; color: #6b6b85; margin: 6px 0 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card-actions { display: flex; gap: 8px; }
.btn-icon-primary { background: transparent; border: none; color: #6b6b85; cursor: pointer; padding: 8px; border-radius: 10px; transition: 0.3s cubic-bezier(0.4, 0, 0.2, 1); display: flex; align-items: center; justify-content: center; }
.btn-icon-primary:hover { background: rgba(124, 111, 247, 0.1); color: #a99df9; }
.btn-icon-danger { background: transparent; border: none; color: #6b6b85; cursor: pointer; padding: 8px; border-radius: 10px; transition: 0.3s; display: flex; align-items: center; justify-content: center; }
.btn-icon-danger:hover { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }
.btn-nebula-primary { background: linear-gradient(135deg, #7c6ff7 0%, #5b4fcf 50%, #4c3fc4 100%); color: white; border: none; border-radius: 12px; padding: 12px 24px; font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 10px; transition: all 0.3s; box-shadow: 0 8px 24px rgba(124, 111, 247, 0.3); }
.btn-nebula-primary:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 12px 32px rgba(124, 111, 247, 0.5); filter: brightness(1.1); }
.btn-nebula-primary:active { transform: scale(0.98); }
.btn-nebula-primary:disabled { opacity: 0.5; cursor: not-allowed; box-shadow: none; transform: none; }
.btn-nebula-outline { background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.1); color: #e8e8f0; border-radius: 12px; padding: 10px 20px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: 0.3s; }
.btn-nebula-outline:hover { background: rgba(255, 255, 255, 0.08); border-color: #7c6ff7; }
.btn-nebula-danger { background: linear-gradient(135deg, #f56c6c, #c53030); color: #fff; border: none; border-radius: 12px; padding: 12px 24px; font-weight: 700; cursor: pointer; transition: all 0.3s; }
.btn-nebula-danger:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(245, 108, 108, 0.3); }
.nebula-action-float { position: fixed; bottom: 50px; left: 50%; transform: translateX(-50%); z-index: 100; }
.btn-huge { padding: 18px 40px; font-size: 1.1rem; border-radius: 50px; }
.nebula-empty-state { grid-column: 1 / -1; text-align: center; padding: 100px 0; color: #6b6b85; }
.empty-orb { width: 120px; height: 120px; background: radial-gradient(circle, rgba(124, 111, 247, 0.1) 0%, transparent 70%); margin: 0 auto 20px; border-radius: 50%; border: 1px dashed rgba(124, 111, 247, 0.3); }
.toast-wrapper { position: fixed; top: 28px; left: 50%; transform: translateX(-50%); padding: 12px 24px; border-radius: 50px; display: flex; align-items: center; gap: 8px; font-size: 0.9rem; font-weight: 600; z-index: 99999; backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.1); box-shadow: 0 12px 40px rgba(0,0,0,0.5); background: rgba(18,18,36,0.85); }
.toast-success { border-color: rgba(74,222,128,0.4); color: #4ade80; }
.toast-error { border-color: rgba(245,108,108,0.4); color: #f56c6c; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translate(-50%, -20px) scale(0.95); }
.nebula-modal-overlay { position: fixed; inset: 0; background: rgba(6, 6, 15, 0.7); backdrop-filter: blur(10px); display: flex; justify-content: center; align-items: center; z-index: 10001; }
.nebula-modal-content { width: 480px; max-width: 90vw; padding: 32px; background: rgba(18, 18, 36, 0.85); backdrop-filter: blur(40px) saturate(140%); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 20px; box-shadow: 0 25px 80px rgba(0, 0, 0, 0.6); display: flex; flex-direction: column; gap: 20px; text-align: left; }
.modal-header { position: relative; display: flex; justify-content: center; align-items: center; margin-bottom: 16px; }
.modal-header h2 { margin: 0; color: #fff; font-size: 1.3rem; display: flex; align-items: center; justify-content: center; gap: 10px; }
.text-danger { color: #f56c6c; }
.btn-close-circle { position: absolute; right: 0; top: 50%; transform: translateY(-50%); background: rgba(255, 255, 255, 0.05); border: none; color: #9898b4; border-radius: 50%; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: 0.3s; margin: 0; }
.btn-close-circle:hover { background: rgba(255, 255, 255, 0.1); color: #fff; transform: translateY(-50%) rotate(90deg); }
.modal-desc { color: #9898b4; font-size: 0.95rem; line-height: 1.6; margin: 0; }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
.nebula-input-wrapper { margin: 12px 0 0 0; position: relative; background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 12px; overflow: hidden; }
.mt-15 { margin-top: 15px; }
.nebula-input { width: 100%; background: transparent; border: none; padding: 14px 16px; color: #fff; font-size: 1rem; outline: none; }
.focus-line { position: absolute; bottom: 0; left: 0; height: 2px; width: 100%; background: linear-gradient(90deg, #7c6ff7, #a99df9, #7c6ff7); transform: scaleX(0); transition: 0.4s; }
.nebula-input:focus ~ .focus-line { transform: scaleX(1); }
.modal-fade-enter-active, .modal-fade-leave-active { transition: 0.3s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.9) translateY(20px); }
</style>