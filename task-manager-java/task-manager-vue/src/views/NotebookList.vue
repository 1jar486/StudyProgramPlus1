<template>
  <div class="glass-app-container">
    <header class="vibe-header">
      <div class="header-left">
        <h1><span class="material-icons title-icon">auto_awesome</span> 我的 AI 笔记本</h1>
      </div>
      <div class="header-right">
        <button class="btn-pill btn-outline" @click="router.push('/tasks')">返回看板</button>
      </div>
    </header>

    <!-- 顶部分类 Tabs (营造 NotebookLM 的 Vibe) -->
    <div class="vibe-tabs">
      <div class="tab active">最近</div>
      <div class="tab">已分享</div>
      <div class="tab">标题</div>
    </div>

    <!-- 笔记本卡片列表 -->
    <div class="notebook-grid">
      <div
          class="notebook-card"
          v-for="(nb, index) in notebooks"
          :key="nb.id"
          :style="{ background: cardColors[index % cardColors.length] }"
          @click="goToNotebook(nb.id)"
      >
        <div class="card-left">
          <span class="emoji-icon">{{ emojis[index % emojis.length] }}</span>
          <div class="nb-info">
            <h3>{{ nb.name }}</h3>
            <p>创建于: {{ formatDate(nb.createdTime) }}</p >
          </div>
        </div>
        <div class="card-right">
          <!-- 阻止冒泡，避免点击删除时触发进入笔记本 -->
          <button class="btn-action" @click.stop="triggerDeleteNotebook(nb.id, nb.name)" title="删除笔记本">
            <span class="material-icons">delete_outline</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 底部固定的新建按钮 -->
    <div class="bottom-action-area">
      <button class="btn-huge-create" @click="showCreateModal = true">
        <span class="material-icons">add</span> 新建笔记本
      </button>
    </div>

    <!-- 新建笔记本弹窗 -->
    <transition name="modal-fade">
      <div class="vibe-modal-overlay" v-if="showCreateModal" @click.self="showCreateModal = false">
        <div class="vibe-modal-content">
          <h2>创建新笔记本</h2>
          <input
              v-model="newNotebookName"
              type="text"
              class="vibe-input"
              placeholder="例如：高等数学期末复习、AI 论文阅读..."
              @keyup.enter="createNotebook"
              autofocus
          >
          <div class="modal-footer">
            <button class="btn-pill btn-outline" @click="showCreateModal = false">取消</button>
            <button class="btn-pill btn-ai" @click="createNotebook" :disabled="!newNotebookName.trim()">创建</button>
          </div>
        </div>
      </div>
    </transition>
    <!-- 自定义删除确认弹窗 -->
    <transition name="modal-fade">
      <div class="vibe-modal-overlay" v-if="deleteConfirm.show" @click.self="deleteConfirm.show = false">
        <div class="vibe-modal-content confirm-modal">
          <div class="modal-icon-warning">
            <span class="material-icons">warning_amber</span>
          </div>
          <h2>确定要删除该笔记本吗？</h2>
          <p class="warning-text">你即将删除 <strong>{{ deleteConfirm.name }}</strong>。<br>这会彻底清空里面所有的文档和对话记录，此操作无法恢复！</p >
          <div class="modal-footer">
            <button class="btn-pill btn-outline" @click="deleteConfirm.show = false">取消</button>
            <button class="btn-pill btn-danger" @click="executeDeleteNotebook">确定删除</button>
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
// 新增确认弹窗的状态管理
const deleteConfirm = ref({ show: false, id: null, name: '' });
// 营造 NotebookLM 的缤纷马卡龙色系
const cardColors = ['#E0F2FE', '#F5F3FF', '#F0FDF4', '#FEFCE8', '#FFE4E6'];
const emojis = ['🚀', '☀️', '📖', '🧠', '💡'];

const fetchNotebooks = async () => {
  try {
    notebooks.value = await request.get('/api/notebooks');
  } catch (error) {
    console.error("获取笔记本失败", error);
  }
};

// 1. 点击垃圾桶时，不再调用浏览器原生 confirm，而是呼出我们自己画的弹窗
const triggerDeleteNotebook = (id, name) => {
  deleteConfirm.value = { show: true, id, name };
};

// 2. 用户在自定义弹窗里点击“确定删除”后，才真正执行删除
const executeDeleteNotebook = async () => {
  try {
    await request.delete(`/api/notebooks/${deleteConfirm.value.id}`);
    deleteConfirm.value.show = false; // 关闭弹窗
    await fetchNotebooks(); // 刷新列表
    // 如果你在这个文件里也配了 Toast，可以加上这句：
    // showToast(`笔记本【${deleteConfirm.value.name}】已删除`, 'success');
  } catch (error) {
    alert("删除失败"); // 建议后续也替换为 showToast
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
    alert("创建失败，请检查后端");
  }
};



const goToNotebook = (id) => {
  // 携带动态路由参数跳转到特定的工作区
  router.push(`/copilot/${id}`);
};

const formatDate = (dateString) => {
  if (!dateString) return '刚刚';
  const date = new Date(dateString);
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
};

onMounted(() => {
  fetchNotebooks();
});
</script>

<style scoped>
/* 延续薄荷绿背景 */
.glass-app-container {
  min-height: 100vh;
  padding: 40px 60px;
  background: linear-gradient(135deg, #F0FDF4 0%, #F8FAFC 100%);
  font-family: system-ui, -apple-system, sans-serif;
  box-sizing: border-box;
}

.vibe-header {
  display: flex; justify-content: space-between; align-items: center; max-width: 800px; margin: 0 auto 32px auto;
}
.header-left h1 { display: flex; align-items: center; gap: 8px; font-size: 28px; color: #111827; margin: 0; }
.title-icon { color: #34D399; font-size: 28px; }

/* 按钮通用样式复用 */
.btn-pill {
  display: flex; align-items: center; gap: 6px; padding: 10px 22px; border-radius: 999px; font-weight: 600; cursor: pointer; border: none; transition: 0.3s;
}
.btn-outline { background: #ffffff; color: #374151; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.btn-outline:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.btn-ai { background: #34D399; color: white; }
.btn-ai:hover { transform: translateY(-2px); background: #10B981; box-shadow: 0 4px 12px rgba(16,185,129,0.3); }

/* Tabs 样式 */
.vibe-tabs {
  display: flex; gap: 16px; max-width: 800px; margin: 0 auto 24px auto;
}
.tab {
  padding: 8px 16px; border-radius: 999px; font-size: 15px; font-weight: 600; color: #6B7280; cursor: pointer;
}
.tab.active { background: #E5E7EB; color: #111827; }

/* 笔记本卡片网格 */
.notebook-grid {
  max-width: 800px; margin: 0 auto; display: flex; flex-direction: column; gap: 16px; padding-bottom: 100px;
}

.notebook-card {
  display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-radius: 20px; cursor: pointer; transition: all 0.3s;
}
.notebook-card:hover { transform: translateY(-2px); filter: brightness(0.95); }

.card-left { display: flex; align-items: center; gap: 16px; }
.emoji-icon { font-size: 32px; }
.nb-info h3 { margin: 0 0 4px 0; font-size: 20px; color: #111827; }
.nb-info p { margin: 0; font-size: 14px; color: #6B7280; }

.card-right { display: flex; gap: 8px; }
.btn-action {
  background: rgba(255,255,255,0.7); border: none; width: 40px; height: 40px; border-radius: 50%; display: flex; justify-content: center; align-items: center; cursor: pointer; transition: 0.2s; color: #4B5563;
}
.btn-action:hover { background: #ffffff; color: #EF4444; }

/* 底部新建按钮区域 */
.bottom-action-area {
  position: fixed; bottom: 40px; left: 0; right: 0; display: flex; justify-content: center; pointer-events: none;
}
.btn-huge-create {
  pointer-events: auto; background: #111827; color: white; border: none; padding: 16px 32px; border-radius: 999px; font-size: 18px; font-weight: 600; display: flex; align-items: center; gap: 8px; cursor: pointer; box-shadow: 0 10px 25px rgba(0,0,0,0.2); transition: 0.3s;
}
.btn-huge-create:hover { transform: translateY(-4px) scale(1.02); box-shadow: 0 15px 35px rgba(0,0,0,0.3); }

/* 弹窗样式 */
.vibe-modal-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(17,24,39,0.4); backdrop-filter: blur(8px); display: flex; justify-content: center; align-items: center; z-index: 999;
}
.vibe-modal-content {
  background: white; width: 400px; padding: 32px; border-radius: 24px; box-shadow: 0 25px 50px rgba(0,0,0,0.2);
}
.vibe-modal-content h2 { margin: 0 0 20px 0; font-size: 20px; }
.vibe-input {
  width: 100%; box-sizing: border-box; padding: 12px 16px; font-size: 16px; border: 1px solid #E5E7EB; border-radius: 12px; margin-bottom: 24px; outline: none;
}
.vibe-input:focus {border-color: #7c6ff7;box-shadow: 0 0 0 3px rgba(124, 111, 247, 0.2);}
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; }

.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.3s; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: translateY(10px); }

/* 自定义确认弹窗专属样式 */
.confirm-modal { text-align: center; }
.modal-icon-warning { background: #FEF2F2; color: #EF4444; width: 64px; height: 64px; border-radius: 50%; display: flex; justify-content: center; align-items: center; margin: 0 auto 16px auto; font-size: 32px; }
.modal-icon-warning .material-icons { font-size: 32px; }
.warning-text { color: #6B7280; font-size: 14px; line-height: 1.6; margin-bottom: 24px; }
.warning-text strong { color: #111827; }
.btn-danger { background: #EF4444; color: white; border: none; }
.btn-danger:hover { background: #DC2626; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3); }

</style>