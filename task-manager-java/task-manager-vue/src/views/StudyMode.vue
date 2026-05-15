<template>
  <div class="study-mode-container">
    <h1 style="color: #fff">🚀 沉浸式复习模式开发中...</h1>
    <p style="color: #9898b4">这里将是带有 3D 翻转卡片的核心战场</p >
    <button @click="$router.push('/decks')" class="btn-nebula-outline">返回大厅</button>
  </div>
</template>

<script setup></script>

<style scoped>
.study-mode-container { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #06060f; }
.btn-nebula-outline { margin-top: 20px; padding: 10px 20px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); color: #fff; cursor: pointer; border-radius: 8px; }
</style>