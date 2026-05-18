<template>
  <div class="stats-page-wrapper custom-scrollbar">
    <div class="summary-container">
      <div class="stat-card glass-panel">
        <div class="card-icon-box"><span class="material-icons">bolt</span></div>
        <div class="card-info">
          <div class="number-row">
            <span class="value">{{ totalNodes }}</span>
            <span class="unit">NODES</span>
          </div>
          <p class="label">累计巩固节点</p>
        </div>
      </div>

      <div class="stat-card glass-panel">
        <div class="card-icon-box streak-icon"><span class="material-icons">local_fire_department</span></div>
        <div class="card-info">
          <div class="number-row">
            <span class="value">{{ maxStreak }}</span>
            <span class="unit">DAYS</span>
          </div>
          <p class="label">最长连续活跃</p>
        </div>
      </div>
    </div>

    <div class="main-chart-section glass-panel">
      <div class="chart-header">
        <h2>年度突触连接热力图</h2>
        <p>记录你每一天的脑力激荡与记忆重塑</p>
      </div>

      <div class="decoration-wave">
        <svg viewBox="0 0 100 40" class="pulse-svg">
          <path d="M0 20 Q 10 5, 20 20 T 40 20 T 60 20 T 80 20 T 100 20" fill="none" stroke="rgba(124, 111, 247, 0.4)" stroke-width="2" />
        </svg>
      </div>

      <div class="chart-scroll-box custom-scrollbar">
        <div ref="heatmapRef" class="heatmap-chart"></div>
      </div>

      <div class="visual-map-legend">
        <span>0</span>
        <div class="legend-bar"></div>
        <span>50+</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import * as echarts from 'echarts';
import request from '../utils/request';

const heatmapRef = ref(null);
let myChart = null;
let resizeObserver = null;
const loading = ref(true);

// 真实业务数据状态
const totalNodes = ref(0);
const maxStreak = ref(0);

// 计算连续天数的算法
const calculateStreak = (data) => {
  if (!data || data.length === 0) return 0;
  const sortedDates = data
      .filter(item => item[1] > 0)
      .map(item => item[0])
      .sort();

  if (sortedDates.length === 0) return 0;

  let currentStreak = 1;
  let max = 1;

  for (let i = 1; i < sortedDates.length; i++) {
    const prev = new Date(sortedDates[i-1]);
    const curr = new Date(sortedDates[i]);
    // 🛡️ 修复：增加 Math.round 抹平时区夏令时偏差
    const diff = Math.round((curr - prev) / (1000 * 60 * 60 * 24));

    if (diff === 1) {
      currentStreak++;
      max = Math.max(max, currentStreak);
    } else if (diff > 1) {
      currentStreak = 1;
    }
  }
  return max;
};

const initChart = async () => {
  try {
    const res = await request.get('/api/flashcards/stats/heatmap');
    const data = res.map(item => [item.date, item.count]);

    // 🌟 真实数据汇总逻辑
    totalNodes.value = data.reduce((sum, item) => sum + item[1], 0);
    maxStreak.value = calculateStreak(data);

    loading.value = false;
    await nextTick();

    if (!myChart && heatmapRef.value) {
      myChart = echarts.init(heatmapRef.value);
    }

    // ================= 👇 刚才的修复代码加在这里 👇 =================
    // 计算滚动的 365 天区间
    const today = new Date();
    const lastYear = new Date();
    lastYear.setFullYear(today.getFullYear() - 1);

    // 转换为 yyyy-MM-dd 格式
    const formatDate = (date) => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
    // ==============================================================

    const option = {
      tooltip: {
        position: 'top',
        backgroundColor: 'rgba(18,18,36,0.9)',
        borderColor: 'rgba(124,111,247,0.5)',
        textStyle: { color: '#e8e8f0' },
        formatter: (p) => `${p.value[0]}<br/>激活突触: ${p.value[1]} 次`
      },
      visualMap: {
        show: false,
        min: 0, max: 20,
        inRange: { color: ['rgba(255,255,255,0.05)', '#4c3fc4', '#7c6ff7', '#a99df9', '#ffffff'] }
      },
      calendar: {
        top: 40, bottom: 20, left: 40, right: 10,
        // 🌟 修复：应用刚刚计算好的滚动日期范围
        range: [formatDate(lastYear), formatDate(today)],
        cellSize: [22, 22],
        splitLine: { show: false },
        itemStyle: { color: 'transparent', borderWidth: 3, borderColor: '#06060f' },
        yearLabel: { show: false },
        dayLabel: { nameMap: 'ZH', color: '#6b6b85', fontSize: 12 },
        monthLabel: { nameMap: 'ZH', color: '#9898b4', fontSize: 12, margin: 10 }
      },
      series: {
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: data,
        itemStyle: { borderRadius: 4 }
      }
    };

    myChart.setOption(option);
  } catch (e) {
    console.error("加载数据失败:", e);
  }
};

onMounted(() => {
  initChart();
  resizeObserver = new ResizeObserver(() => myChart?.resize());
  if (heatmapRef.value) resizeObserver.observe(heatmapRef.value);
});

onUnmounted(() => {
  resizeObserver?.disconnect();
  myChart?.dispose();
});
</script>

<style scoped>
.stats-page-wrapper { padding: 40px 60px; min-height: 100vh; background: transparent; display: flex; flex-direction: column; gap: 30px; box-sizing: border-box; }
.summary-container { display: flex; gap: 24px; }
/* 🌟 已为你合并修复：绝对居中，去除多余重复代码 */
.stat-card { flex: 1; height: 120px; display: flex; align-items: center; justify-content: center; padding: 0 30px; gap: 24px; background: rgba(18, 18, 36, 0.65); backdrop-filter: blur(40px); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 20px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3); }
.card-icon-box { width: 60px; height: 60px; background: rgba(124, 111, 247, 0.15); border-radius: 16px; display: flex; align-items: center; justify-content: center; color: #7c6ff7; }
.card-icon-box .material-icons { font-size: 32px; text-shadow: 0 0 15px #7c6ff7; }
.streak-icon { background: rgba(245, 108, 108, 0.15); color: #f56c6c; }
.streak-icon .material-icons { text-shadow: 0 0 15px #f56c6c; }
.card-info { display: flex; flex-direction: column; justify-content: center; }
.number-row { display: flex; align-items: baseline; gap: 8px; }
.value { font-size: 42px; font-weight: 800; color: #fff; letter-spacing: -1px; }
.unit { font-size: 14px; color: #6b6b85; font-weight: 700; letter-spacing: 1px; }
.label { font-size: 14px; color: #9898b4; margin-top: 4px; }
.main-chart-section { position: relative; padding: 40px; background: rgba(18, 18, 36, 0.65); backdrop-filter: blur(40px); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 24px; }
.chart-header { text-align: center; margin-bottom: 20px; }
.chart-header h2 { font-size: 24px; color: #fff; margin-bottom: 8px; font-weight: 700; }
.chart-header p { color: #6b6b85; font-size: 14px; }
.decoration-wave { position: absolute; right: 40px; top: 40px; width: 100px; opacity: 0.6; }
.chart-scroll-box { width: 100%; overflow-x: auto; padding-bottom: 10px; }
.heatmap-chart { width: 1200px; height: 280px; margin: 0 auto; }
.visual-map-legend { display: flex; align-items: center; justify-content: center; gap: 15px; margin-top: 20px; color: #6b6b85; font-size: 12px; font-weight: 700; }
.legend-bar { width: 180px; height: 10px; border-radius: 10px; background: linear-gradient(90deg, rgba(255, 255, 255, 0.05), #4c3fc4, #7c6ff7, #a99df9, #ffffff); }
.custom-scrollbar::-webkit-scrollbar { height: 6px; width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(124, 111, 247, 0.3); border-radius: 10px; }
</style>