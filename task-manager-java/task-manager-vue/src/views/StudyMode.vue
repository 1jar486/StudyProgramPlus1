<template>
  <div class="glass-app-container study-container">
    <transition name="toast-fade">
      <div v-if="toast.show" :class="['toast-wrapper', `toast-${toast.type}`]">
        <span class="material-icons toast-icon">{{ toast.type === 'success' ? 'check_circle' : 'error_outline' }}</span>
        {{ toast.message }}
      </div>
    </transition>

    <header class="study-header">
      <div class="header-left-actions">
        <button class="btn-icon-blur" @click="exitStudy" title="中断并返回">
          <span class="material-icons">close</span>
        </button>
        <div class="stats-badge" title="今日已巩固节点数">
          <span class="material-icons" style="font-size: 16px;">local_fire_department</span>
          今日攻克: {{ todayReviewedCount }}
        </div>
      </div>

      <div class="progress-indicator">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPercentage + '%' }"></div>
        </div>
        <span class="progress-text">{{ currentIndex + 1 }} / {{ totalCards === 0 ? 1 : totalCards }}</span>
      </div>

      <div class="header-right-actions">
        <div class="nebula-dropdown" v-click-outside="closeDropdown">
          <div class="dropdown-trigger" @click="toggleDropdown" :class="{ 'active': isDropdownOpen }">
            <span class="material-icons trigger-icon">layers</span>
            <span class="trigger-text">{{ currentModeLabel }}</span>
            <span class="material-icons arrow-icon" :class="{ 'rotated': isDropdownOpen }">expand_more</span>
          </div>
          <transition name="dropdown-fade">
            <ul class="dropdown-menu glass-panel" v-if="isDropdownOpen">
              <li
                  v-for="option in modeOptions"
                  :key="option.value"
                  :class="{ 'selected': selectedMode === option.value }"
                  @click="selectMode(option.value)"
              >
                <span class="material-icons opt-icon">{{ option.icon }}</span>
                {{ option.label }}
              </li>
            </ul>
          </transition>
        </div>

        <button class="btn-icon-blur" :class="{'disabled': undoStack.length === 0}" @click="handleUndo" title="撤销上一步 (Ctrl+Z)">
          <span class="material-icons">undo</span>
        </button>
        <button class="btn-icon-blur" @click="showSettings = true" title="配置学习配额">
          <span class="material-icons">tune</span>
        </button>
        <div class="deck-title-badge">
          <span class="material-icons" style="font-size: 16px; margin-right: 4px;">style</span>
          沉浸记忆链路
        </div>
      </div>
    </header>

    <main class="study-main">
      <template v-if="!isFinished && currentCard">
        <div class="anki-flashcard glass-panel">

          <div class="card-content question-area">
            <span class="card-label">Question</span>
            <div :class="['card-text markdown-body custom-scrollbar', isSingleWord(currentCard.frontContent) ? 'hero-word' : 'regular-sentence']"
                 v-html="renderMd(currentCard.frontContent)">
            </div>
          </div>

          <div class="divider" v-if="isFlipped"></div>

          <transition name="fade-slide">
            <div class="card-content answer-area" v-if="isFlipped">
              <span class="card-label success-text">Answer</span>
              <div :class="['card-text markdown-body custom-scrollbar', isSingleWord(currentCard.backContent) ? 'hero-word' : 'regular-sentence']"
                   v-html="renderMd(currentCard.backContent)">
              </div>
            </div>
          </transition>

          <div class="card-actions">
            <button v-if="!isFlipped" class="btn-reveal" @click="flipCard">
              点击显示答案 (Space)
            </button>

            <div v-else class="rating-buttons">
              <button class="btn-rate hard" @click="submitGrade('HARD')">生疏 (1)</button>
              <button class="btn-rate good" @click="submitGrade('GOOD')">掌握 (2)</button>
              <button class="btn-rate easy" @click="submitGrade('EASY')">简单 (3)</button>
            </div>
          </div>

        </div>
      </template>

      <div v-else class="completion-state glass-panel">
        <div class="completion-orb">
          <span class="material-icons" style="font-size: 48px; color: #4ade80;">workspace_premium</span>
        </div>
        <h2>阶段目标达成</h2>
        <p>今日分配的知识锚点已全部遍历完毕，大脑神经元已成功加固。</p >
        <button class="btn-nebula-primary" @click="exitStudy" style="margin-top: 24px;">
          <span class="material-icons">flight_takeoff</span> 返回控制大厅
        </button>
      </div>
    </main>

    <transition name="modal-fade">
      <div class="modal-overlay" v-if="showSettings" @click.self="showSettings = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3><span class="material-icons">tune</span> 调整记忆引擎负荷</h3>
            <button class="btn-icon-blur" @click="showSettings = false" style="width:32px;height:32px;">
              <span class="material-icons" style="font-size: 18px;">close</span>
            </button>
          </div>
          <div class="modal-body">
            <div class="nebula-input-group">
              <label>每日新知识配额 (默认20)</label>
              <input type="number" class="nebula-input" v-model.number="tempNewLimit" min="0" />
            </div>
            <div class="nebula-input-group">
              <label>每日复习极限负荷 (默认100)</label>
              <input type="number" class="nebula-input" v-model.number="tempReviewLimit" min="0" />
            </div>
          </div>
          <div class="modal-footer" style="display:flex; gap:12px; margin-top:24px;">
            <button class="btn-nebula-primary" style="flex:1; justify-content:center;" @click="saveSettings">
              重载记忆序列
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { marked } from 'marked';
import hljs from 'highlight.js';
import DOMPurify from 'dompurify';
import 'highlight.js/styles/atom-one-dark.css';
import { watch } from 'vue'; // 确保顶部 import 了 watch
// 负荷设置状态
const showSettings = ref(false);
const newLimit = ref(parseInt(localStorage.getItem('anki_new_limit') || '20'));
const reviewLimit = ref(parseInt(localStorage.getItem('anki_review_limit') || '100'));
const tempNewLimit = ref(newLimit.value);
const tempReviewLimit = ref(reviewLimit.value);

const isSubmitting = ref(false);

const toast = ref({ show: false, message: '', type: 'success' });
let toastTimer = null;

// 🛡️ 修复：每次打开面板时，强制读取真实配置，清空上一次的残留输入
watch(showSettings, (newVal) => {
  if (newVal) {
    tempNewLimit.value = newLimit.value;
    tempReviewLimit.value = reviewLimit.value;
  }
});

marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext';
    return hljs.highlight(code, { language }).value;
  },
  breaks: true
});

const renderMd = (text) => DOMPurify.sanitize(marked(text || ''));

const route = useRoute();
const router = useRouter();
const deckId = route.params.deckId;

// 数据与状态
const cards = ref([]);
const currentIndex = ref(0);
const isFlipped = ref(false);
const isFinished = ref(false);
const todayReviewedCount = ref(0);
const undoStack = ref([]);

// 🌟 核心新增：下拉框专用的状态和选项配置（支持绑定你原有的变量逻辑）
const isDropdownOpen = ref(false);
const selectedMode = ref('standard');
const modeOptions = ref([
  { value: 'standard', label: '标准记忆模式', icon: 'psychology' },
  { value: 'rapid', label: '快速突破模式', icon: 'bolt' },
  { value: 'difficult', label: '重难点轰炸', icon: 'gavel' }
]);

const currentModeLabel = computed(() => {
  const matched = modeOptions.value.find(opt => opt.value === selectedMode.value);
  return matched ? matched.label : '记忆模式';
});

const toggleDropdown = () => { isDropdownOpen.value = !isDropdownOpen.value; };
const closeDropdown = () => { isDropdownOpen.value = false; };
const selectMode = (val) => {
  selectedMode.value = val;
  isDropdownOpen.value = false;
  showToast(`记忆序列已切换至：${currentModeLabel.value}`, 'success');

  // 🌟 核心改动：切换模式后，立刻命令引擎重新向后端拉取特定模式的卡片！
  initData();
};


const showToast = (message, type = 'success') => {
  if (toastTimer) clearTimeout(toastTimer);
  toast.value = { show: true, message, type };
  toastTimer = setTimeout(() => { toast.value.show = false; }, 2200);
};

const totalCards = computed(() => cards.value.length);
const currentCard = computed(() => cards.value[currentIndex.value] || null);
const progressPercentage = computed(() => {
  if (totalCards.value === 0) return 100;
  return (currentIndex.value / totalCards.value) * 100;
});

const initData = async () => {
  try {
    // 🌟 核心改动：在 URL 后面拼接上 &mode=${selectedMode.value}
    const [dueRes, statsRes] = await Promise.all([
      request.get(`/api/flashcards/deck/${deckId}/due?newLimit=${newLimit.value}&reviewLimit=${reviewLimit.value}&mode=${selectedMode.value}`),
      request.get(`/api/flashcards/stats/today`)
    ]);
    cards.value = dueRes;
    todayReviewedCount.value = statsRes;
    currentIndex.value = 0;
    undoStack.value = [];
    isFinished.value = cards.value.length === 0;
  } catch (error) {
    showToast('获取记忆节点失败，连接断开', 'error');
  }
};

const saveSettings = () => {
  // 🛡️ 修复：增加防御性容错，防止用户清空输入框导致存入 NaN 从而搞崩后端
  newLimit.value = parseInt(tempNewLimit.value) || 20;
  reviewLimit.value = parseInt(tempReviewLimit.value) || 100;

  // 强行纠正用户的错误输入，反馈到 UI 上
  tempNewLimit.value = newLimit.value;
  tempReviewLimit.value = reviewLimit.value;

  localStorage.setItem('anki_new_limit', newLimit.value);
  localStorage.setItem('anki_review_limit', reviewLimit.value);
  showSettings.value = false;
  initData();
  showToast('记忆引擎负荷已重载', 'success');
};

const flipCard = () => { isFlipped.value = !isFlipped.value; };

const isSingleWord = (text) => {
  if (!text) return false;
  const trimmed = text.trim();
  return !/\s/.test(trimmed) || trimmed.length <= 20;
};

const submitGrade = async (grade) => {
  if (!currentCard.value || isSubmitting.value) return;
  isSubmitting.value = true;
  const cardId = currentCard.value.id;
  const snapshot = JSON.parse(JSON.stringify(currentCard.value));

  try {
    await request.post(`/api/flashcards/${cardId}/review?grade=${grade}`);
    undoStack.value.push(snapshot);
    todayReviewedCount.value++;
    currentIndex.value++;
    isFlipped.value = false;
    if (currentIndex.value >= totalCards.value) isFinished.value = true;
  } catch (error) {
    showToast('数据同步失败，请重试', 'error');
  } finally {
    isSubmitting.value = false;
  }
};

const handleUndo = async () => {
  if (undoStack.value.length === 0) return;
  const lastState = undoStack.value.pop();
  try {
    await request.post(`/api/flashcards/${lastState.id}/rollback`, lastState);
    currentIndex.value--;
    todayReviewedCount.value--;
    isFlipped.value = true;
    isFinished.value = false;
    showToast('已回溯至上一个锚点', 'success');
  } catch (error) {
    showToast('时空回溯失败', 'error');
    undoStack.value.push(lastState);
  }
};

const exitStudy = () => router.push('/decks');

const handleKeyDown = (e) => {
  if (showSettings.value || isDropdownOpen.value) return;

  if (e.ctrlKey && e.key === 'z') {
    e.preventDefault();
    handleUndo();
    return;
  }
  if (isFinished.value) return;

  if (e.code === 'Space') {
    e.preventDefault();
    flipCard();
  } else if (isFlipped.value) {
    if (e.key === '1') submitGrade('HARD');
    if (e.key === '2') submitGrade('GOOD');
    if (e.key === '3') submitGrade('EASY');
  }
};

// 自定义指令：用于点击空白处自动收起下拉框
const vClickOutside = {
  mounted(el, binding) {
    el.clickOutsideEvent = (event) => {
      if (!(el === event.target || el.contains(event.target))) {
        binding.value(event);
      }
    };
    document.addEventListener("click", el.clickOutsideEvent);
  },
  unmounted(el) {
    document.removeEventListener("click", el.clickOutsideEvent);
  },
};

onMounted(() => {
  initData();
  window.addEventListener('keydown', handleKeyDown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown);
});
</script>

<style scoped>
.glass-app-container{min-height:100vh;background:transparent;color:#e8e8f0;font-family:'Inter',system-ui,sans-serif;box-sizing:border-box;display:flex;flex-direction:column;}
.study-container{padding:0;overflow:hidden;}
.study-header{height:70px;display:flex;justify-content:space-between;align-items:center;padding:0 40px;background:rgba(18,18,36,0.4);border-bottom:1px solid rgba(255,255,255,0.05);backdrop-filter:blur(20px);z-index:100;}
.header-left-actions{display:flex;align-items:center;gap:16px;}
.header-right-actions{display:flex;align-items:center;gap:16px;}
.btn-icon-blur{background:rgba(255,255,255,0.05);border:1px solid rgba(255,255,255,0.1);color:#9898b4;width:40px;height:40px;border-radius:50%;display:flex;align-items:center;justify-content:center;cursor:pointer;transition:all 0.3s;}
.btn-icon-blur:hover{background:rgba(245,108,108,0.15);color:#f56c6c;border-color:rgba(245,108,108,0.3);transform:rotate(90deg);}
.btn-icon-blur.disabled{opacity:0.3;cursor:not-allowed;pointer-events:none;}
.stats-badge{background:rgba(245,108,108,0.1);color:#f56c6c;border:1px solid rgba(245,108,108,0.2);padding:6px 14px;border-radius:50px;font-size:0.85rem;font-weight:600;display:flex;align-items:center;gap:4px;}
.progress-indicator{display:flex;align-items:center;gap:16px;flex:1;max-width:400px;}
.progress-bar{flex:1;height:6px;background:rgba(255,255,255,0.1);border-radius:10px;overflow:hidden;}
.progress-fill{height:100%;background:linear-gradient(90deg,#7c6ff7,#a99df9);transition:width 0.4s cubic-bezier(0.34,1.56,0.64,1);}
.progress-text{font-size:0.85rem;font-weight:600;color:#a99df9;letter-spacing:1px;}
.deck-title-badge{background:rgba(124,111,247,0.15);color:#a99df9;border:1px solid rgba(124,111,247,0.3);padding:6px 14px;border-radius:50px;font-size:0.85rem;font-weight:600;display:flex;align-items:center;}
.study-main{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:40px;position:relative;}
.glass-panel{background:rgba(18,18,36,0.8);backdrop-filter:blur(40px) saturate(140%);border:1px solid rgba(255,255,255,0.1);border-radius:24px;box-shadow:0 25px 80px rgba(0,0,0,0.6);}
.glass-panel:hover{border-color:rgba(255,255,255,0.2);}
.completion-state{text-align:center;padding:60px 80px;max-width:500px;}
.completion-orb{width:100px;height:100px;background:radial-gradient(circle,rgba(74,222,128,0.15) 0%,transparent 70%);margin:0 auto 20px;border-radius:50%;display:flex;align-items:center;justify-content:center;border:1px solid rgba(74,222,128,0.3);}
.completion-state h2{font-size:1.5rem;margin:0 0 12px;color:#fff;}
.completion-state p{color:#9898b4;line-height:1.6;}
.btn-nebula-primary{background:linear-gradient(135deg,#7c6ff7 0%,#5b4fcf 50%,#4c3fc4 100%);color:white;border:none;border-radius:12px;padding:12px 24px;font-weight:700;cursor:pointer;display:inline-flex;align-items:center;gap:10px;transition:all 0.3s;box-shadow:0 8px 24px rgba(124,111,247,0.3);}
.btn-nebula-primary:hover{transform:translateY(-2px);box-shadow:0 12px 32px rgba(124,111,247,0.5);}
.toast-wrapper{position:fixed;top:28px;left:50%;transform:translateX(-50%);padding:12px 24px;border-radius:50px;display:flex;align-items:center;gap:8px;font-size:0.9rem;font-weight:600;z-index:10000;backdrop-filter:blur(20px);border:1px solid rgba(255,255,255,0.1);box-shadow:0 12px 40px rgba(0,0,0,0.5);background:rgba(18,18,36,0.85);}
.toast-success{border-color:rgba(74,222,128,0.4);color:#4ade80;}
.toast-error{border-color:rgba(245,108,108,0.4);color:#f56c6c;}
.toast-fade-enter-active,.toast-fade-leave-active{transition:all 0.4s cubic-bezier(0.34,1.56,0.64,1);}
.toast-fade-enter-from,.toast-fade-leave-to{opacity:0;transform:translate(-50%,-20px) scale(0.95);}
.custom-scrollbar::-webkit-scrollbar{width:4px;}
.custom-scrollbar::-webkit-scrollbar-thumb{background:rgba(255,255,255,0.1);border-radius:4px;}
.markdown-body:deep(p){margin:0 0 12px;}
.markdown-body:deep(pre){background:rgba(0,0,0,0.3);padding:12px;border-radius:8px;border:1px solid rgba(255,255,255,0.05);overflow-x:auto;margin-bottom:12px;}
.markdown-body:deep(code){font-family:'SF Mono',Consolas,monospace;font-size:0.9em;background:rgba(0,0,0,0.2);padding:2px 4px;border-radius:4px;}
.markdown-body:deep(pre code){background:transparent;padding:0;}
.markdown-body:deep(ul),.markdown-body:deep(ol){margin-top:0;padding-left:20px;}
.markdown-body:deep(h1),.markdown-body:deep(h2),.markdown-body:deep(h3){margin-top:0;margin-bottom:12px;color:#a99df9;font-weight:700;}
.modal-overlay{position:fixed;top:0;left:0;width:100vw;height:100vh;background:rgba(6,6,15,0.6);backdrop-filter:blur(10px);z-index:9999;display:flex;justify-content:center;align-items:center;}
.modal-content{background:rgba(18,18,36,0.85);border:1px solid rgba(255,255,255,0.1);border-radius:20px;padding:32px;width:360px;box-shadow:0 25px 80px rgba(0,0,0,0.6);}
.modal-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:24px;}
.modal-header h3{margin:0;font-size:1.2rem;color:#e8e8f0;display:flex;align-items:center;gap:8px;}
.nebula-input-group{margin-bottom:20px;}
.nebula-input-group label{display:block;font-size:0.85rem;color:#9898b4;margin-bottom:8px;}
.nebula-input{width:100%;background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.1);color:#fff;border-radius:12px;padding:12px 16px;outline:none;transition:all 0.3s;}
.nebula-input:focus{border-color:#7c6ff7;background:rgba(124,111,247,0.05);box-shadow:0 0 0 4px rgba(124,111,247,0.1);}

/* 闪卡本体：大小适中，高级玻璃态，水平居中 */
.anki-flashcard {
  width: 90%;
  max-width: 580px;
  min-height: 400px;
  margin: 40px auto;
  background: rgba(25, 25, 45, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  padding: 40px;
  position: relative;
  transition: all 0.3s ease;
}

/* 舞台调度器净化 */
.card-content {
  font-size: 1.15rem;
  line-height: 1.8;
  color: #fff;
  text-align: left;
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.card-text {
  width: 100%;
  max-height: 250px;
  overflow-y: auto;
  padding-right: 8px;
}

.card-label { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 2px; color: #6b6b85; margin-bottom: 16px; text-align: center; }
.success-text { color: #4ade80; }

.hero-word { text-align: center; }
.hero-word :deep(p) {
  font-size: 3.5rem !important;
  font-weight: 800 !important;
  line-height: 1.2;
  color: #ffffff;
  text-shadow: 0 4px 20px rgba(255, 255, 255, 0.1);
  margin: 0;
}

.regular-sentence { text-align: left; }
.regular-sentence :deep(p) { font-size: 1.15rem; line-height: 1.7; color: #e8e8f0; }

.divider { height: 1px; background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent); margin: 30px 0; }
.card-actions { margin-top: auto; padding-top: 30px; display: flex; justify-content: center; }

.btn-reveal { background: transparent; color: #a99df9; border: 1px solid #7c6ff7; padding: 12px 32px; border-radius: 50px; font-size: 1rem; cursor: pointer; transition: all 0.3s ease; }
.btn-reveal:hover { background: #7c6ff7; color: #fff; box-shadow: 0 0 20px rgba(124, 111, 247, 0.4); }

.rating-buttons { display: flex; gap: 16px; }
.btn-rate { padding: 12px 28px; border-radius: 12px; font-weight: 600; cursor: pointer; transition: 0.3s; border: none; font-size: 0.95rem; }
.hard { background: rgba(245,108,108,0.15); color: #f56c6c; border: 1px solid rgba(245,108,108,0.3); }
.hard:hover { background: #f56c6c; color: #fff; box-shadow: 0 0 15px rgba(245,108,108,0.4); }
.good { background: rgba(124,111,247,0.15); color: #a99df9; border: 1px solid rgba(124,111,247,0.3); }
.good:hover { background: #7c6ff7; color: #fff; box-shadow: 0 0 15px rgba(124,111,247,0.4); }
.easy { background: rgba(74,222,128,0.15); color: #4ade80; border: 1px solid rgba(74,222,128,0.3); }
.easy:hover { background: #4ade80; color: #111827; box-shadow: 0 0 15px rgba(74,222,128,0.4); }

.fade-slide-enter-active { transition: all 0.5s cubic-bezier(0.2, 0.8, 0.2, 1); }
.fade-slide-enter-from { opacity: 0; transform: translateY(15px); }

/* 🌟 ==================== 【全新重构：Nebula 极客流线下拉框样式】 ==================== */
.nebula-dropdown {
  position: relative;
  display: inline-block;
  user-select: none;
}

/* 触发按钮 */
.dropdown-trigger {
  height: 40px;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50px; /* 胶囊态，完美融入顶部控制区 */
  color: #e8e8f0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-trigger:hover, .dropdown-trigger.active {
  background: rgba(124, 111, 247, 0.08);
  border-color: #a99df9;
  box-shadow: 0 0 15px rgba(124, 111, 247, 0.2);
  color: #fff;
}

.trigger-icon {
  font-size: 16px;
  color: #7c6ff7;
}

.arrow-icon {
  font-size: 18px;
  color: #9898b4;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.arrow-icon.rotated {
  transform: rotate(180deg);
  color: #a99df9;
}

/* 下拉菜单面板 */
.dropdown-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 180px;
  padding: 8px;
  background: rgba(18, 18, 36, 0.9); /* 加深底色防背景穿透 */
  backdrop-filter: blur(25px) saturate(140%);
  border-radius: 16px;
  list-style: none;
  margin: 0;
  z-index: 1000;
  transform-origin: top right;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.7), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

/* 菜单列表项 */
.dropdown-menu li {
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 0.88rem;
  color: #9898b4;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dropdown-menu li:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #e8e8f0;
}

.dropdown-menu li.selected {
  background: rgba(124, 111, 247, 0.15);
  color: #a99df9;
  font-weight: 600;
}

.opt-icon {
  font-size: 16px;
}

/* 下拉渐变动画 */
.dropdown-fade-enter-active, .dropdown-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.dropdown-fade-enter-from, .dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}
</style>