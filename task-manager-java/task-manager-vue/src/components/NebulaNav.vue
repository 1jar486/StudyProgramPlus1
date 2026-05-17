<template>
  <nav class="nebula-sidebar glass-panel">
    <div class="nav-logo">
      <span class="material-icons tech-icon">psychology</span>
      <span class="nav-title">星云终端</span>
    </div>

    <div class="nav-links">
      <div class="nav-item" :class="{ active: route.path === '/tasks' }" @click="navigate('/tasks')">
        <span class="material-icons">task_alt</span>
        <span class="nav-text">任务大厅</span>
      </div>

      <div class="nav-item" :class="{ active: route.path === '/notebooks' || route.path.startsWith('/copilot') }" @click="navigate('/notebooks')">
        <span class="material-icons">auto_awesome</span>
        <span class="nav-text">复习舱 (LM)</span>
      </div>

      <div class="nav-item" :class="{ active: route.path.startsWith('/decks') }" @click="navigate('/decks')">
        <span class="material-icons">style</span>
        <span class="nav-text">Anki 记忆</span>
      </div>

      <div class="nav-item heatmap-item" :class="{ active: route.path === '/stats' }" @click="navigate('/stats')">
        <span class="material-icons">local_fire_department</span>
        <span class="nav-text">活跃热力图</span>
      </div>
    </div>

    <div class="nav-footer">
      <div class="nav-item danger-item" @click="handleLogout">
        <span class="material-icons">logout</span>
        <span class="nav-text">断开连接</span>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

// 基础页面跳转
const navigate = (path) => {
  if (route.path !== path) {
    router.push(path);
  }
};

// 退出登录逻辑
const handleLogout = () => {
  localStorage.removeItem('token');
  router.push('/login');
};
</script>

<style scoped>
.nebula-sidebar {
  position: fixed; left: 0; top: 0; bottom: 0; width: 80px;
  display: flex; flex-direction: column;
  background: rgba(18, 18, 36, 0.65); backdrop-filter: blur(40px) saturate(140%);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  padding: 30px 0; z-index: 9999;
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden; white-space: nowrap;
}
.nebula-sidebar:hover { width: 240px; box-shadow: 20px 0 60px rgba(0, 0, 0, 0.5); }
.nav-logo { display: flex; align-items: center; padding: 0 24px; margin-bottom: 40px; color: #fff; }
.tech-icon { font-size: 32px; color: #7c6ff7; text-shadow: 0 0 15px rgba(124, 111, 247, 0.5); }
.nav-title { font-size: 1.2rem; font-weight: 800; margin-left: 16px; opacity: 0; transition: opacity 0.3s; }
.nebula-sidebar:hover .nav-title { opacity: 1; }
.nav-links { flex: 1; display: flex; flex-direction: column; gap: 8px; width: 100%; }
.nav-footer { width: 100%; }
.nav-item { display: flex; align-items: center; padding: 16px 28px; color: #9898b4; cursor: pointer; transition: all 0.3s ease; position: relative; }
.nav-item::before { content: ''; position: absolute; left: 0; top: 10%; bottom: 10%; width: 3px; background: #7c6ff7; border-radius: 0 4px 4px 0; opacity: 0; transition: opacity 0.3s; }
.nav-item .material-icons { font-size: 24px; min-width: 24px; }
.nav-text { margin-left: 20px; font-size: 0.95rem; font-weight: 600; opacity: 0; transition: opacity 0.3s; }
.nebula-sidebar:hover .nav-text { opacity: 1; }
.nav-item:hover { background: rgba(255, 255, 255, 0.05); color: #e8e8f0; }
.nav-item.active { color: #a99df9; background: rgba(124, 111, 247, 0.08); }
.nav-item.active::before { opacity: 1; box-shadow: 0 0 10px rgba(124, 111, 247, 0.5); }
.danger-item:hover { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }
.heatmap-item:hover { color: #f59e0b; }
</style>