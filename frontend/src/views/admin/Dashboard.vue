<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import http from '../../api/http';
import { ElMessage } from 'element-plus';
import { User, Document, QuestionFilled, Warning, DataBoard, TrendCharts, ArrowUp } from '@element-plus/icons-vue';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();
const router = useRouter();

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
    // 后端返回格式: { data: { total: number } }
    userCount.value = userResp.data?.data?.total || userResp.data?.total || 0;

    // 获取风险等级分布
    const riskResp = await http.get('/admin/statistics/risk-distribution');
    // 后端返回格式: { data: { data: [{ name: string, value: number }] } }
    riskDistribution.value = riskResp.data?.data?.data || riskResp.data?.data || [
      { name: '低风险', value: 0 },
      { name: '中风险', value: 0 },
      { name: '高风险', value: 0 },
    ];

    // 获取活跃趋势
    const trendResp = await http.get('/admin/statistics/active-trend');
    // 后端返回格式: { data: { data: [{ date: string, count: number }] } }
    activeTrend.value = trendResp.data?.data?.data || trendResp.data?.data || [];

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

const goUserManagementWithRisk = (riskLevel: 'LOW' | 'MEDIUM' | 'HIGH') => {
  router.push({ path: '/admin/users', query: { riskLevel } });
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
      right: '10%',
      top: '10%',
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

  const trendDates = activeTrend.value.map((item) => item.date);
  const trendCounts = activeTrend.value.map((item) => item.count);
  const maxCount = trendCounts.length ? Math.max(...trendCounts) : 0;

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
      data: trendDates,
      boundaryGap: false,
    },
    yAxis: {
      type: 'value',
      name: '活跃用户数',
      min: 0,
      // 全 0 时避免图表“看起来没内容”
      max: maxCount <= 0 ? 1 : undefined,
      minInterval: 1,
    },
    series: [
      {
        name: '活跃用户',
        type: 'line',
        smooth: true,
        data: trendCounts,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 6,
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
    <!-- 欢迎横幅 -->
    <el-card class="welcome-banner" shadow="never">
      <div class="banner-content">
        <div class="banner-text">
          <h3>欢迎回来，{{ userStore.name || '管理员' }}！</h3>
          <p>今天是 {{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }}</p>
        </div>
        <div class="banner-icon">
          <el-icon :size="64" color="#667eea"><DataBoard /></el-icon>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card stat-card-primary" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
                <el-icon :size="28"><User /></el-icon>
              </div>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ userCount }}</div>
              <div class="stat-label">用户总数</div>
              <div class="stat-trend">
                <el-icon :size="12" color="#67c23a"><ArrowUp /></el-icon>
                <span>持续增长</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card stat-card-success" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <div class="stat-icon" style="background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%)">
                <el-icon :size="28"><Document /></el-icon>
              </div>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ riskDistribution.reduce((sum, item) => sum + item.value, 0) }}</div>
              <div class="stat-label">已测评用户</div>
              <div class="stat-trend">
                <el-icon :size="12" color="#67c23a"><ArrowUp /></el-icon>
                <span>活跃用户</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card stat-card-warning clickable" shadow="hover" @click="goUserManagementWithRisk('HIGH')">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <div class="stat-icon" style="background: linear-gradient(135deg, #e6a23c 0%, #f5c842 100%)">
                <el-icon :size="28"><Warning /></el-icon>
              </div>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ riskDistribution.find((r) => r.name === '高风险')?.value || 0 }}</div>
              <div class="stat-label">高风险用户</div>
              <div class="stat-trend">
                <el-icon :size="12" color="#e6a23c"><Warning /></el-icon>
                <span>需关注</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="stat-card stat-card-info clickable" shadow="hover" @click="goUserManagementWithRisk('LOW')">
          <div class="stat-content">
            <div class="stat-icon-wrapper">
              <div class="stat-icon" style="background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%)">
                <el-icon :size="28"><QuestionFilled /></el-icon>
              </div>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ riskDistribution.find((r) => r.name === '低风险')?.value || 0 }}</div>
              <div class="stat-label">低风险用户</div>
              <div class="stat-trend">
                <el-icon :size="12" color="#67c23a"><ArrowUp /></el-icon>
                <span>安全状态</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">风险等级分布</span>
              <el-tag type="info" size="small">实时数据</el-tag>
            </div>
          </template>
          <div ref="riskChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">用户活跃趋势</span>
              <el-tag type="success" size="small">最近7天</el-tag>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  .welcome-banner {
    margin-bottom: 24px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-card__body) {
      padding: 24px 32px;
    }

    .banner-content {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .banner-text {
        color: #fff;

        h3 {
          margin: 0 0 8px 0;
          font-size: 24px;
          font-weight: 700;
        }

        p {
          margin: 0;
          font-size: 14px;
          opacity: 0.9;
        }
      }

      .banner-icon {
        opacity: 0.2;
      }
    }
  }

  .stats-row {
    margin-bottom: 24px;
  }

  .stat-card {
    border-radius: 12px;
    border: none;
    transition: all 0.3s;
    overflow: hidden;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 4px;
      background: linear-gradient(90deg, transparent, currentColor, transparent);
      opacity: 0;
      transition: opacity 0.3s;
    }

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);

      &::before {
        opacity: 1;
      }
    }

    :deep(.el-card__body) {
      padding: 24px;
    }

    .stat-content {
      display: flex;
      align-items: center;
      gap: 20px;

      .stat-icon-wrapper {
        flex-shrink: 0;

        .stat-icon {
          width: 72px;
          height: 72px;
          border-radius: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #fff;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
          transition: transform 0.3s;
        }
      }

      .stat-info {
        flex: 1;
        min-width: 0;

        .stat-value {
          font-size: 32px;
          font-weight: 700;
          color: #303133;
          line-height: 1;
          margin-bottom: 8px;
          font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, sans-serif;
        }

        .stat-label {
          font-size: 14px;
          color: #909399;
          margin-bottom: 8px;
          font-weight: 500;
        }

        .stat-trend {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;
          color: #67c23a;

          span {
            color: #909399;
          }
        }
      }
    }

    &.stat-card-primary::before {
      background: linear-gradient(90deg, transparent, #667eea, transparent);
    }

    &.stat-card-success::before {
      background: linear-gradient(90deg, transparent, #67c23a, transparent);
    }

    &.stat-card-warning::before {
      background: linear-gradient(90deg, transparent, #e6a23c, transparent);
    }

    &.stat-card-info::before {
      background: linear-gradient(90deg, transparent, #409eff, transparent);
    }

    &.clickable {
      cursor: pointer;
    }
  }

  .charts-row {
    margin-bottom: 24px;
  }

  .chart-card {
    border-radius: 12px;
    border: none;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
    }

    :deep(.el-card__header) {
      padding: 20px 24px;
      border-bottom: 1px solid #f0f0f0;
      background: #fafafa;
    }

    :deep(.el-card__body) {
      padding: 24px;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }

    .chart-container {
      width: 100%;
      height: 400px;
      min-height: 400px;
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .dashboard {
    .welcome-banner {
      .banner-content {
        flex-direction: column;
        text-align: center;
        gap: 16px;

        .banner-icon {
          display: none;
        }
      }
    }

    .stat-card {
      .stat-content {
        .stat-icon-wrapper .stat-icon {
          width: 56px;
          height: 56px;
        }

        .stat-info .stat-value {
          font-size: 24px;
        }
      }
    }

    .chart-card .chart-container {
      height: 300px;
      min-height: 300px;
    }
  }
}
</style>
