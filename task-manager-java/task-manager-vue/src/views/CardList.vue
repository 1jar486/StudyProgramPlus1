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
          <span class="material-icons tech-icon">inventory_2</span>卡片管理矩阵
        </h1>
        <p class="nebula-subtitle">构建、编辑与洞察你的知识切片 (支持 Markdown)</p >
      </div>
      <div class="header-right">
        <button class="btn-nebula-outline" @click="router.push('/decks')">
          <span class="material-icons">arrow_back</span>返回牌组大厅
        </button>
        <button class="btn-nebula-primary" @click="goToStudyMode" :disabled="cards.length === 0">
          <span class="material-icons">play_circle_filled</span>进入沉浸学习
        </button>
      </div>
    </header>

    <div class="action-bar">
      <button class="btn-nebula-primary btn-sm" @click="openCreateModal">
        <span class="material-icons">add</span> 新增知识卡片
      </button>
      <button class="btn-nebula-outline btn-sm" @click="exportCards" title="导出为CSV格式">
        <span class="material-icons">download</span> 导出牌组
      </button>
    </div>

    <div class="nebula-table-wrapper">
      <table class="nebula-table" v-if="cards.length > 0">
        <thead>
        <tr>
          <th>正面 (问题)</th>
          <th>背面 (答案)</th>
          <th>记忆状态</th>
          <th>连续答对</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="card in cards" :key="card.id">
          <td class="text-truncate">{{ card.frontContent }}</td>
          <td class="text-truncate">{{ card.backContent }}</td>
          <td>
            <span :class="['status-badge', `status-${(card.status?.toLowerCase()) || 'new'}`]">
              {{ card.status || 'NEW' }}
            </span>
          </td>
          <td>{{ card.consecutiveCorrect }} 次</td>
          <td>
            <div class="table-actions">
              <button class="btn-icon-primary" @click="openEditModal(card)"><span class="material-icons">edit</span></button>
              <button class="btn-icon-danger" @click="deleteCard(card.id)"><span class="material-icons">delete</span></button>
            </div>
          </td>
        </tr>
        </tbody>
      </table>

      <div v-else class="nebula-empty-state">
        <div class="empty-orb"></div>
        <p>当前牌组空空如也，快去提取你的知识切片吧</p >
      </div>
    </div>

    <transition name="modal-fade">
      <div class="nebula-modal-overlay" v-if="cardModal.show" @click.self="cardModal.show = false">
        <div class="nebula-modal-content glass-panel card-modal-lg">
          <div class="modal-header">
            <h2><span class="material-icons">{{ cardModal.isEdit ? 'edit_note' : 'post_add' }}</span> {{ cardModal.isEdit ? '编辑卡片' : '录入新卡片' }}</h2>
            <button class="btn-close-circle" @click="cardModal.show = false"><span class="material-icons">close</span></button>
          </div>
          <div class="modal-body two-col-layout">
            <div class="input-col">
              <label class="nebula-label">正面 (Question / Prompt)</label>
              <div class="nebula-input-wrapper no-margin">
                <textarea v-model="cardModal.data.frontContent" class="nebula-textarea custom-scrollbar" placeholder="支持纯文本或 Markdown 语法..." autofocus></textarea>
                <div class="focus-line"></div>
              </div>
            </div>
            <div class="input-col">
              <label class="nebula-label">背面 (Answer / Detail)</label>
              <div class="nebula-input-wrapper no-margin">
                <textarea v-model="cardModal.data.backContent" class="nebula-textarea custom-scrollbar" placeholder="输入详细解答、代码段或知识点..."></textarea>
                <div class="focus-line"></div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-nebula-outline" @click="cardModal.show = false">取消</button>
            <button class="btn-nebula-primary" @click="saveCard" :disabled="!cardModal.data.frontContent.trim() || !cardModal.data.backContent.trim() || (cardModal.isEdit && cardModal.data.frontContent === cardModal.data.oldFront && cardModal.data.backContent === cardModal.data.oldBack)">
              保存卡片
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';

const route = useRoute();
const router = useRouter();
const deckId = route.params.deckId; // 从路由获取当前牌组ID

// 全局提示
const toast = ref({ show: false, message: '', type: 'success' });
let toastTimer = null;
const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer);
  toast.value = { show: true, message, type };
  toastTimer = setTimeout(() => { toast.value.show = false; }, 3000);
};


// 卡片列表数据
const cards = ref([]);

const fetchCards = async () => {
  try {
    const res = await request.get(`/api/flashcards/deck/${deckId}`);
    cards.value = res;
  } catch (error) {
    showToast('获取卡片失败', 'error');
  }
};

// 卡片编辑/新建 弹窗控制
const cardModal = ref({
  show: false,
  isEdit: false,
  data: { id: null, frontContent: '', backContent: '' }
});

const openCreateModal = () => {
  cardModal.value = { show: true, isEdit: false, data: { id: null, frontContent: '', backContent: '' } };
};

const openEditModal = (card) => {
  cardModal.value = { show: true, isEdit: true, data: { id: card.id, frontContent: card.frontContent, backContent: card.backContent } };
};

const saveCard = async () => {
  const { isEdit, data } = cardModal.value;
  try {
    if (isEdit) {
      await request.put(`/api/flashcards/${data.id}`, { front: data.frontContent, back: data.backContent });
      showToast('卡片已更新', 'success');
    } else {
      await request.post('/api/flashcards', { deckId: deckId, frontContent: data.frontContent, backContent: data.backContent });
      showToast('新卡片录入成功', 'success');
    }
    cardModal.value.show = false;
    fetchCards();
  } catch (error) {
    showToast('保存失败', 'error');
  }
};

const deleteCard = async (id) => {
  if (!confirm('确定要删除这张卡片吗？')) return;
  try {
    await request.delete(`/api/flashcards/${id}`);
    showToast('卡片已删除', 'success');
    fetchCards();
  } catch (error) {
    showToast('删除失败', 'error');
  }
};
// 🛡️ 修复：状态中增加 oldFront 和 oldBack 记录初始值
const cardModal = ref({
  show: false,
  isEdit: false,
  data: { id: null, frontContent: '', backContent: '', oldFront: '', oldBack: '' }
});

const openCreateModal = () => {
  cardModal.value = { show: true, isEdit: false, data: { id: null, frontContent: '', backContent: '', oldFront: '', oldBack: '' } };
};

const openEditModal = (card) => {
  cardModal.value = {
    show: true,
    isEdit: true,
    data: {
      id: card.id,
      frontContent: card.frontContent,
      backContent: card.backContent,
      oldFront: card.frontContent, // 备份原始数据
      oldBack: card.backContent
    }
  };
};
// 导出功能：直接调用我们写好的 CSV 导出接口
const exportCards = () => {
  request.get(`/api/flashcards/deck/${deckId}/export`, { responseType: 'blob' })
      .then(res => {
        const blob = new Blob([res], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `deck_${deckId}_export.csv`;
        link.click();
        URL.revokeObjectURL(link.href);
        showToast('导出成功', 'success');
      }).catch(() => showToast('导出失败，请检查网络', 'error'));
};

const goToStudyMode = () => {
  router.push(`/decks/${deckId}/study`);
};

onMounted(() => {
  fetchCards();
});
</script>

<style scoped>
.glass-app-container { min-height: 100vh; padding: 60px 80px; background: transparent; color: #e8e8f0; font-family: 'Inter', system-ui, sans-serif; box-sizing: border-box; }
.nebula-header { display: flex; justify-content: space-between; align-items: flex-start; max-width: 1200px; margin: 0 auto 40px; }
.header-right { display: flex; align-items: center; gap: 15px;}

.nebula-title-main { font-size: 2rem; font-weight: 800; letter-spacing: -0.03em; margin: 0; display: flex; align-items: center; gap: 12px; color: #fff; text-shadow: 0 0 20px rgba(124, 111, 247, 0.3); }
.tech-icon { color: #7c6ff7; font-size: 32px; }
.nebula-subtitle { color: #9898b4; font-size: 0.95rem; margin: 8px 0 0; }
.action-bar { max-width: 1200px; margin: 0 auto 20px; display: flex; gap: 12px; }
.nebula-table-wrapper { max-width: 1200px; margin: 0 auto; background: rgba(18, 18, 36, 0.65); backdrop-filter: blur(40px) saturate(140%); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 20px; padding: 20px; overflow-x: auto; }
.nebula-table { width: 100%; border-collapse: collapse; text-align: left; }
.nebula-table th { padding: 16px; color: #9898b4; font-weight: 600; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
.nebula-table td { padding: 16px; color: #e8e8f0; border-bottom: 1px solid rgba(255, 255, 255, 0.02); }
.nebula-table tr:hover td { background: rgba(124, 111, 247, 0.05); }
.text-truncate { max-width: 250px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.status-badge { padding: 4px 10px; border-radius: 50px; font-size: 0.75rem; font-weight: 700; letter-spacing: 0.5px; }
.status-new { background: rgba(124, 111, 247, 0.2); color: #a99df9; border: 1px solid rgba(124, 111, 247, 0.4); }
.status-learning { background: rgba(245, 166, 35, 0.2); color: #f5a623; border: 1px solid rgba(245, 166, 35, 0.4); }
.status-review { background: rgba(74, 222, 128, 0.2); color: #4ade80; border: 1px solid rgba(74, 222, 128, 0.4); }
.table-actions { display: flex; gap: 8px; }
.btn-icon-primary { background: transparent; border: none; color: #6b6b85; cursor: pointer; padding: 6px; border-radius: 8px; transition: 0.3s; display: flex; }
.btn-icon-primary:hover { background: rgba(124, 111, 247, 0.1); color: #a99df9; }
.btn-icon-danger { background: transparent; border: none; color: #6b6b85; cursor: pointer; padding: 6px; border-radius: 8px; transition: 0.3s; display: flex; }
.btn-icon-danger:hover { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }
.btn-nebula-primary { background: linear-gradient(135deg, #7c6ff7 0%, #5b4fcf 50%, #4c3fc4 100%); color: white; border: none; border-radius: 12px; padding: 12px 24px; font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: 0.3s; }
.btn-nebula-primary:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 12px 32px rgba(124, 111, 247, 0.5); filter: brightness(1.1); }
.btn-nebula-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-nebula-outline { background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.1); color: #e8e8f0; border-radius: 12px; padding: 10px 20px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: 0.3s; }
.btn-nebula-outline:hover { background: rgba(255, 255, 255, 0.08); border-color: #7c6ff7; }
.btn-sm { padding: 8px 16px; font-size: 0.9rem; border-radius: 10px; }
.nebula-empty-state { text-align: center; padding: 80px 0; color: #6b6b85; }
.empty-orb { width: 100px; height: 100px; background: radial-gradient(circle, rgba(124, 111, 247, 0.1) 0%, transparent 70%); margin: 0 auto 20px; border-radius: 50%; border: 1px dashed rgba(124, 111, 247, 0.3); }
.toast-wrapper { position: fixed; top: 28px; left: 50%; transform: translateX(-50%); padding: 12px 24px; border-radius: 50px; display: flex; align-items: center; gap: 8px; font-size: 0.9rem; font-weight: 600; z-index: 99999; backdrop-filter: blur(20px); border: 1px solid rgba(255,255,255,0.1); box-shadow: 0 12px 40px rgba(0,0,0,0.5); background: rgba(18,18,36,0.85); }
.toast-success { border-color: rgba(74,222,128,0.4); color: #4ade80; }
.toast-error { border-color: rgba(245,108,108,0.4); color: #f56c6c; }
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translate(-50%, -20px) scale(0.95); }
.nebula-modal-overlay { position: fixed; inset: 0; background: rgba(6, 6, 15, 0.7); backdrop-filter: blur(10px); display: flex; justify-content: center; align-items: center; z-index: 10001; }
.card-modal-lg { width: 800px; max-width: 95vw; }
.glass-panel { background: rgba(18, 18, 36, 0.85); backdrop-filter: blur(40px) saturate(140%); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 20px; padding: 32px; box-shadow: 0 25px 80px rgba(0, 0, 0, 0.6); display: flex; flex-direction: column; gap: 20px; text-align: left; }
.modal-header { position: relative; display: flex; justify-content: center; align-items: center; margin-bottom: 16px; }
.modal-header h2 { margin: 0; color: #fff; font-size: 1.3rem; display: flex; align-items: center; justify-content: center; gap: 10px; }
.btn-close-circle { position: absolute; right: 0; top: 50%; transform: translateY(-50%); background: rgba(255, 255, 255, 0.05); border: none; color: #9898b4; border-radius: 50%; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: 0.3s; margin: 0; }
.btn-close-circle:hover { background: rgba(255, 255, 255, 0.1); color: #fff; transform: translateY(-50%) rotate(90deg); }
.modal-body.two-col-layout { display: flex; gap: 24px; }
.input-col { flex: 1; display: flex; flex-direction: column; gap: 10px; }
.nebula-label { color: #a99df9; font-size: 0.9rem; font-weight: 600; }
.nebula-input-wrapper { position: relative; background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 12px; overflow: hidden; }
.no-margin { margin: 0; }
.nebula-textarea { width: 100%; height: 200px; background: transparent; border: none; padding: 16px; color: #fff; font-size: 0.95rem; outline: none; resize: none; font-family: inherit; line-height: 1.6; }
.focus-line { position: absolute; bottom: 0; left: 0; height: 2px; width: 100%; background: linear-gradient(90deg, #7c6ff7, #a99df9, #7c6ff7); transform: scaleX(0); transition: 0.4s; }
.nebula-textarea:focus ~ .focus-line { transform: scaleX(1); }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; margin-top: 12px; }
.modal-fade-enter-active, .modal-fade-leave-active { transition: 0.3s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.9) translateY(20px); }
</style>