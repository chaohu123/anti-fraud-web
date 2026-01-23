<script setup lang="ts">
import { ref, onMounted } from 'vue';
import * as echarts from 'echarts';
import http from '../../api/http';
import { ElMessage } from 'element-plus';
import { User, Document, QuestionFilled, Warning } from '@element-plus/icons-vue';

const userCount = ref(0);
const riskDistribution = ref<{ name: string; value: number }[]>([]);
const activeTrend = ref<{ date: string; count: number }[]>([]);

const riskChartRef = ref<HTMLDivElement>();
const trendChartRef = ref<HTMLDivElement>();

let riskChart: echarts.ECharts | null = null;
let trendChart: echarts.ECharts | null = null;

const fetchStatistics = async () => {
  try {
    // 获取用户数量
    const userResp = await http.get('/admin/statistics/users');
    userCount.value = userResp.data?.total || 0;

    // 获取风险等级分布
    const riskResp = await http.get('/admin/statistics/risk-distribution');
    // 后端返回格式: { data: [{ name: string, value: number }] }
    riskDistribution.value = riskResp.data?.data || [
      { name: '低风险', value: 0 },
      { name: '中风险', value: 0 },
      { name: '高风险', value: 0 },
    ];

    // 获取活跃趋势
    const trendResp = await http.get('/admin/statistics/active-trend');
    // 后端返回格式: { data: [{ date: string, count: number }] }
    activeTrend.value = trendResp.data?.data || [];

    // 渲染图表
    renderRiskChart();
    renderTrendChart();
  } catch (error) {
    console.error('获取统计数据失败:', error);
    ElMessage.error('无法加载统计数据，请检查后端服务');
    // 失败时使用空数据而不是模拟数据
    userCount.value = 0;
    riskDistribution.value = [
      { name: '低风险', value: 0 },
      { name: '中风险', value: 0 },
      { name: '高风险', value: 0 },
    ];
    activeTrend.value = [];
    renderRiskChart();
    renderTrendChart();
  }
};

const renderRiskChart = () => {
  if (!riskChartRef.value) return;

  if (riskChart) {
    riskChart.dispose();
  }

  riskChart = echarts.init(riskChartRef.value);

  const option = {
    title: {
      text: '风险等级分布',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal',
      },
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle',
    },
    series: [
      {
        name: '风险等级',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        data: riskDistribution.value.map((item) => ({
          value: item.value,
          name: item.name,
        })),
      },
    ],
  };

  riskChart.setOption(option);
};

const renderTrendChart = () => {
  if (!trendChartRef.value) return;

  if (trendChart) {
    trendChart.dispose();
  }

  trendChart = echarts.init(trendChartRef.value);

  const option = {
    title: {
      text: '用户活跃趋势',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal',
      },
    },
    tooltip: {
      trigger: 'axis',
    },
    xAxis: {
      type: 'category',
      data: activeTrend.value.map((item) => item.date),
      boundaryGap: false,
    },
    yAxis: {
      type: 'value',
      name: '活跃用户数',
    },
    series: [
      {
        name: '活跃用户',
        type: 'line',
        smooth: true,
        data: activeTrend.value.map((item) => item.count),
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: 'rgba(64, 158, 255, 0.3)',
              },
              {
                offset: 1,
                color: 'rgba(64, 158, 255, 0.1)',
              },
            ],
          },
        },
        lineStyle: {
          color: '#409eff',
          width: 2,
        },
      },
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
  };

  trendChart.setOption(option);
};

onMounted(() => {
  fetchStatistics();
  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    riskChart?.resize();
    trendChart?.resize();
  });
});
</script>

<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff">
              <el-icon :size="32"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ userCount }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67c23a">
              <el-icon :size="32"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ riskDistribution.reduce((sum, item) => sum + item.value, 0) }}</div>
              <div class="stat-label">已测评用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6a23c">
              <el-icon :size="32"><QuestionFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ riskDistribution.find((r) => r.name === '高风险')?.value || 0 }}</div>
              <div class="stat-label">高风险用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f56c6c">
              <el-icon :size="32"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ riskDistribution.find((r) => r.name === '低风险')?.value || 0 }}</div>
              <div class="stat-label">低风险用户</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <div ref="riskChartRef" style="width: 100%; height: 400px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div ref="trendChartRef" style="width: 100%; height: 400px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;

      .stat-icon {
        width: 64px;
        height: 64px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
      }

      .stat-info {
        flex: 1;

        .stat-value {
          font-size: 28px;
          font-weight: 600;
          color: #303133;
          line-height: 1;
          margin-bottom: 8px;
        }

        .stat-label {
          font-size: 14px;
          color: #909399;
        }
      }
    }
  }
}
</style>
