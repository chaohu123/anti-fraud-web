<script setup lang="ts">
import * as echarts from 'echarts';
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useUserStore } from '../stores/user';
import { useKnowledgeStore } from '../store/knowledge';
import http from '../api/http';

const store = useUserStore();
const knowledgeStore = useKnowledgeStore();

const trainingStats = ref<{ totalTimes: number; correctTimes: number; accuracy: number; typeErrorRateMap: Record<string, number> } | null>(null);

const trendEl = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;

const trend = computed(() => store.riskHistory.slice(-12));

const render = () => {
  if (!trendEl.value) return;
  if (!chart) chart = echarts.init(trendEl.value);
  chart.setOption({
    tooltip: {},
    xAxis: {
      type: 'category',
      data: trend.value.map((x) => new Date(x.at).toLocaleDateString()),
    },
    yAxis: { type: 'value', max: 100 },
    series: [
      {
        type: 'line',
        data: trend.value.map((x) => x.score),
        smooth: true,
        areaStyle: { opacity: 0.12 },
        itemStyle: { color: '#2f71ff' },
      },
    ],
  });
};

onMounted(async () => {
  render();
  // 已登录用户进入页面时自动拉取一次训练统计
  await refreshTrainingStats();
});
watch(trend, render);
onBeforeUnmount(() => chart?.dispose());

async function refreshTrainingStats() {
  if (!store.userId) return;
  try {
    const resp = await http.get(`/train/stats/${store.userId}`);
    trainingStats.value = resp.data as any;
  } catch {
    trainingStats.value = null;
  }
}

</script>

<template>
  <el-card>
    <template #header>
      <span>个人中心</span>
    </template>


    <el-descriptions :column="2" border>
      <el-descriptions-item label="userId">{{ store.userId ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ store.name }}</el-descriptions-item>
      <el-descriptions-item label="风险等级">{{ store.riskLevel }}</el-descriptions-item>
      <el-descriptions-item label="风险指数">{{ store.riskIndex }}</el-descriptions-item>
      <el-descriptions-item label="训练次数">{{ store.trainingCount }}</el-descriptions-item>
      <el-descriptions-item label="已学习知识">{{ knowledgeStore.readCount }}</el-descriptions-item>
      <el-descriptions-item label="成就">
        <el-tag v-for="badge in store.badges" :key="badge" type="success" style="margin-right: 8px">
          {{ badge }}
        </el-tag>
        <span v-if="!store.badges.length">暂无</span>
      </el-descriptions-item>
    </el-descriptions>

    <el-card v-if="store.userId && trainingStats" shadow="never" style="margin-top: 12px">
      <div class="card-section-title">后端训练统计</div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="总训练次数">{{ trainingStats.totalTimes }}</el-descriptions-item>
        <el-descriptions-item label="正确率">{{ Math.round(trainingStats.accuracy * 100) }}%</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <div class="section">
      <div class="card-section-title">风险指数变化趋势</div>
      <el-alert v-if="!trend.length" title="暂无历史记录，请先完成一次风险测评。" type="info" show-icon />
      <div v-else ref="trendEl" class="trend"></div>
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.section {
  margin-top: 12px;
}
.trend {
  width: 100%;
  height: 320px;
}
</style>
