<template>
  <div class="glass-app-container stats-container">
    <header class="study-header">
      <div class="header-left-actions">
        <button class="btn-icon-blur" @click="goBack" title="返回控制大厅">
          <span class="material-icons">arrow_back</span>
        </button>
        <div class="deck-title-badge">
          <span class="material-icons" style="font-size: 16px; margin-right: 4px;">analytics</span>
          记忆神经元活跃雷达
        </div>
      </div>
    </header>

    <main class="stats-main">
      <div class="glass-panel chart-panel">
        <div class="panel-header">
          <h2>年度突触连接热力图</h2>
          <p class="subtitle">记录你每一天的脑力激荡与记忆重塑</p >
        </div>

        <div ref="heatmapRef" class="echarts-container"></div>

        <div class="stats-summary">
          <div class="summary-item">
            <span class="summary-value">{{ totalReviews }}</span>
            <span class="summary-label">累计巩固节点</span>
          </div>
          <div class="summary-item">
            <span class="summary-value">{{ maxStreak }}</span>
            <span class="summary-label">最长连续活跃(天)</span>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, markRaw } from 'vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import * as echarts from 'echarts';

const router = useRouter();
const heatmapRef = ref(null);
let myChart = null;

const totalReviews = ref(0);
const maxStreak = ref(0);

const goBack = () => {
  router.push('/decks'); // 返回你的主面板
};

// 计算图表需要的日期范围 (今年)
const getYearRange = () => {
  const currentYear = new Date().getFullYear();
  return [`${currentYear}-01-01`, `${currentYear}-12-31`];
};

// 计算最长连续打卡天数
const calculateStreak = (data) => {
  if (!data || data.length === 0) return 0;
  let currentStreak = 1;
  let longestStreak = 1;

  for (let i = 1; i < data.length; i++) {
    const prevDate = new Date(data[i-1].date);
    const currDate = new Date(data[i].date);
    const diffTime = Math.abs(currDate - prevDate);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 1) {
      currentStreak++;
      longestStreak = Math.max(longestStreak, currentStreak);
    } else {
      currentStreak = 1;
    }
  }
  return longestStreak;
};

const initChart = async () => {
  try {
    const res = await request.get('/api/flashcards/stats/heatmap');

    // 整理 ECharts 所需的数据格式: [[date, count], [date, count]]
    let sum = 0;
    const chartData = res.map(item => {
      sum += item.count;
      return [item.date, item.count];
    });

    totalReviews.value = sum;
    maxStreak.value = calculateStreak(res);

    // 渲染 ECharts
    if (heatmapRef.value) {
      myChart = markRaw(echarts.init(heatmapRef.value));
      const option = {
        tooltip: {
          position: 'top',
          backgroundColor: 'rgba(18,18,36,0.9)',
          borderColor: 'rgba(124,111,247,0.5)',
          textStyle: { color: '#e8e8f0' },
          formatter: function (p) {
            const format = echarts.time.format(p.data[0], '{yyyy}-{MM}-{dd}', false);
            return format + ' : ' + p.data[1] + ' 个节点';
          }
        },
        visualMap: {
          min: 0,
          max: 150, // 颜色最深对应的复习量阈值，可调
          calculable: true,
          orient: 'horizontal',
          left: 'center',
          bottom: 0,
          inRange: {
            // Nebula 风格渐变：极暗紫 -> 明亮紫罗兰 -> 耀眼浅紫
            color: ['rgba(255,255,255,0.05)', '#5b4fcf', '#7c6ff7', '#a99df9']
          },
          textStyle: { color: '#9898b4' }
        },
        calendar: [{
          range: getYearRange(),
          cellSize: ['auto', 20],
          right: 20,
          left: 40,
          top: 40,
          itemStyle: {
            color: 'rgba(255,255,255,0.02)',
            borderWidth: 3,
            borderColor: 'transparent' // 通过透明边框制造间距感
          },
          splitLine: { show: false },
          yearLabel: { show: false },
          dayLabel: { nameMap: 'ZH', color: '#9898b4' },
          monthLabel: { nameMap: 'ZH', color: '#9898b4' }
        }],
        series: [{
          type: 'heatmap',
          coordinateSystem: 'calendar',
          data: chartData,
          itemStyle: { borderRadius: 4 } // 高椭圆角
        }]
      };
      myChart.setOption(option);
    }
  } catch (error) {
    console.error("加载热力引擎失败", error);
  }
};

// 监听窗口大小变化以自适应
const handleResize = () => {
  if (myChart) myChart.resize();
};

onMounted(() => {
  initChart();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (myChart) myChart.dispose();
});
</script>

<style scoped>
.glass-app-container{min-height:100vh;background:transparent;color:#e8e8f0;font-family:'Inter',system-ui,sans-serif;box-sizing:border-box;display:flex;flex-direction:column;}
.stats-container{padding:0;}
.study-header{height:70px;display:flex;justify-content:space-between;align-items:center;padding:0 40px;background:rgba(18,18,36,0.4);border-bottom:1px solid rgba(255,255,255,0.05);backdrop-filter:blur(20px);z-index:10;}
.header-left-actions{display:flex;align-items:center;gap:16px;}
.btn-icon-blur{background:rgba(255,255,255,0.05);border:1px solid rgba(255,255,255,0.1);color:#9898b4;width:40px;height:40px;border-radius:50%;display:flex;align-items:center;justify-content:center;cursor:pointer;transition:all 0.3s;}
.btn-icon-blur:hover{background:rgba(124,111,247,0.15);color:#7c6ff7;border-color:rgba(124,111,247,0.3);transform:translateX(-2px);}
.deck-title-badge{background:rgba(124,111,247,0.15);color:#a99df9;border:1px solid rgba(124,111,247,0.3);padding:6px 14px;border-radius:50px;font-size:0.85rem;font-weight:600;display:flex;align-items:center;}
.stats-main{flex:1;display:flex;justify-content:center;align-items:center;padding:40px;}
.glass-panel{background:rgba(18,18,36,0.8);backdrop-filter:blur(40px) saturate(140%);border:1px solid rgba(255,255,255,0.1);border-radius:24px;box-shadow:0 25px 80px rgba(0,0,0,0.6);}
.chart-panel{width:100%;max-width:1000px;padding:40px;display:flex;flex-direction:column;}
.panel-header{margin-bottom:30px;text-align:center;}
.panel-header h2{margin:0 0 8px;font-size:1.8rem;color:#fff;}
.subtitle{margin:0;color:#9898b4;font-size:0.95rem;}
.echarts-container{width:100%;height:300px;}
.stats-summary{display:flex;justify-content:center;gap:60px;margin-top:20px;padding-top:30px;border-top:1px solid rgba(255,255,255,0.05);}
.summary-item{display:flex;flex-direction:column;align-items:center;}
.summary-value{font-size:2.5rem;font-weight:800;color:#7c6ff7;text-shadow:0 0 20px rgba(124,111,247,0.4);line-height:1;}
.summary-label{margin-top:8px;font-size:0.85rem;color:#6b6b85;text-transform:uppercase;letter-spacing:1px;}
</style>