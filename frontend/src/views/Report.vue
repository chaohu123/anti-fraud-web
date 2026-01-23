<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import { useUserStore } from '../stores/user';
import { useAssessmentStore, type AssessmentReport } from '../store/assessment';
import { ElMessage } from 'element-plus';
import {
  Document,
  InfoFilled,
  WarningFilled,
  MagicStick,
  Reading,
  Trophy,
  Refresh,
} from '@element-plus/icons-vue';

const router = useRouter();
const userStore = useUserStore();
const assessmentStore = useAssessmentStore();

// 获取报告数据
const report = computed(() => assessmentStore.lastReport);

// 雷达图容器
const radarChartEl = ref<HTMLDivElement | null>(null);
let radarChart: echarts.ECharts | null = null;

// 维度映射：将3个维度扩展为5个维度用于雷达图展示
const radarDimensions = computed(() => {
  if (!report.value) return null;
  
  const dimMap: Record<string, number> = {
    info: 0,
    finance: 0,
    psych: 0,
  };
  
  // 从报告数据中提取各维度得分
  report.value.dimensions.forEach((d) => {
    dimMap[d.dimension] = d.score;
  });
  
  // 扩展为5个维度
  return {
    '信息保护意识': dimMap.info,
    '金融安全意识': dimMap.finance,
    '情感判断能力': dimMap.psych,
    '网络安全意识': Math.round((dimMap.info + dimMap.finance) / 2), // 综合信息防护和金融安全
    '决策冷静程度': Math.max(0, 100 - dimMap.psych), // 心理防线越高，决策越冷静（反向）
  };
});

// 风险等级标签和颜色
const riskLevelConfig = computed(() => {
  if (!report.value) return { label: '', type: 'info', color: '#909399' };
  
  const configs: Record<string, { label: string; type: string; color: string }> = {
    low: { label: '低风险', type: 'success', color: '#67c23a' },
    medium: { label: '中等风险', type: 'warning', color: '#e6a23c' },
    high: { label: '高风险', type: 'danger', color: '#f56c6c' },
  };
  
  return configs[report.value.level] || configs.low;
});

// 综合得分评价
const scoreEvaluation = computed(() => {
  if (!report.value) return '';
  
  const score = report.value.score;
  if (score < 30) {
    return '您的防骗能力较强，继续保持警惕，定期更新防骗知识。';
  } else if (score < 60) {
    return '您的防骗能力处于中等水平，建议加强薄弱环节的学习和训练。';
  } else {
    return '您的防骗能力需要提升，建议系统学习防骗知识，提高风险识别能力。';
  }
});

// 薄弱环节（得分最高的维度）
const weakPoints = computed(() => {
  if (!report.value || !report.value.dimensions.length) return [];
  
  const sorted = [...report.value.dimensions].sort((a, b) => b.score - a.score);
  return sorted.slice(0, 2); // 取前2个最薄弱的维度
});

// 初始化雷达图
const initRadarChart = () => {
  if (!radarChartEl.value || !radarDimensions.value) return;
  
  if (!radarChart) {
    radarChart = echarts.init(radarChartEl.value);
  }
  
  const dimensions = Object.keys(radarDimensions.value);
  const values = Object.values(radarDimensions.value);
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `${params.name}: ${params.value}分`;
      },
    },
    radar: {
      indicator: dimensions.map((name) => ({
        name,
        max: 100,
      })),
      center: ['50%', '55%'],
      radius: '70%',
      splitNumber: 4,
      axisName: {
        color: '#666',
        fontSize: 14,
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(250, 250, 250, 0.3)', 'rgba(200, 200, 200, 0.2)'],
        },
      },
      splitLine: {
        lineStyle: {
          color: '#e0e0e0',
        },
      },
      axisLine: {
        lineStyle: {
          color: '#d0d0d0',
        },
      },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: values,
            name: '防骗能力评估',
            areaStyle: {
              color: 'rgba(64, 158, 255, 0.3)',
            },
            lineStyle: {
              color: '#409eff',
              width: 2,
            },
            itemStyle: {
              color: '#409eff',
            },
          },
        ],
      },
    ],
  };
  
  radarChart.setOption(option);
  
  // 响应式调整
  window.addEventListener('resize', () => {
    radarChart?.resize();
  });
};

// 跳转到知识库（根据薄弱环节推荐）
const goToKnowledge = (dimension?: string) => {
  let category = '';
  if (dimension) {
    // 根据维度推荐知识分类
    const categoryMap: Record<string, string> = {
      info: '网站',
      finance: '社交',
      psych: '电话',
    };
    category = categoryMap[dimension] || '';
  }
  router.push({ path: '/knowledge', query: category ? { category } : {} });
};

// 跳转到训练页
const goToTraining = () => {
  router.push('/train');
};

// 重新测评
const reAssessment = () => {
  router.push('/assessment');
};

// 获取维度描述
const getDimensionDescription = (dimension: string, score: number): string => {
  const descriptions: Record<string, Record<string, string>> = {
    info: {
      low: '您在信息保护方面表现良好，能够有效识别和防范信息泄露风险。',
      medium: '您在信息保护方面有一定意识，但仍有提升空间，建议加强相关学习。',
      high: '您在信息保护方面存在较大风险，需要重点关注个人信息安全。',
    },
    finance: {
      low: '您在金融安全方面表现优秀，能够理性处理金融相关事务。',
      medium: '您在金融安全方面需要提高警惕，避免冲动决策。',
      high: '您在金融安全方面风险较高，建议谨慎处理涉及资金的操作。',
    },
    psych: {
      low: '您具备良好的心理防线，不容易被情感操控。',
      medium: '您的心理防线需要加强，建议提高对情感诈骗的识别能力。',
      high: '您的心理防线较为薄弱，容易被情感操控，需要重点提升。',
    },
  };
  
  const level = score < 40 ? 'low' : score < 70 ? 'medium' : 'high';
  return descriptions[dimension]?.[level] || '该维度需要持续关注和提升。';
};

// 获取薄弱环节描述
const getWeakPointDescription = (dimension: string, score: number): string => {
  const descriptions: Record<string, string> = {
    info: '信息保护意识不足，容易泄露个人信息或点击可疑链接。建议学习如何识别钓鱼网站和防范信息泄露。',
    finance: '金融安全意识薄弱，可能在面对高收益诱惑时做出冲动决策。建议学习金融诈骗常见手法，提高警惕。',
    psych: '情感判断能力需要提升，容易被情感操控或恐吓手段影响。建议学习情感诈骗识别技巧，保持理性思考。',
  };
  
  return descriptions[dimension] || '该维度存在风险，建议加强相关学习和训练。';
};

onMounted(() => {
  if (report.value) {
    setTimeout(() => {
      initRadarChart();
    }, 100);
  }
});

onBeforeUnmount(() => {
  radarChart?.dispose();
  window.removeEventListener('resize', () => {
    radarChart?.resize();
  });
});
</script>

<template>
  <div class="report-page">
    <!-- 无报告时的提示 -->
    <el-card v-if="!report" class="empty-card">
      <el-empty description="暂无评估报告">
        <template #image>
          <el-icon :size="100" color="#c0c4cc"><Document /></el-icon>
        </template>
        <el-button type="primary" @click="reAssessment">立即开始风险测评</el-button>
      </el-empty>
    </el-card>

    <!-- 报告内容 -->
    <div v-else class="report-content">
      <!-- 1. 顶部报告概览区 -->
      <el-card class="overview-card" shadow="hover">
        <div class="overview-header">
          <div class="user-info">
            <el-avatar :size="60" class="user-avatar">
              {{ userStore.name.charAt(0) }}
            </el-avatar>
            <div class="user-details">
              <h2 class="user-name">{{ userStore.name }}</h2>
              <p class="report-time">评估时间：{{ new Date(report.createdAt).toLocaleString('zh-CN') }}</p>
            </div>
          </div>
          <div class="score-section">
            <div class="score-value">{{ report.score }}</div>
            <div class="score-label">综合防骗得分</div>
            <el-tag :type="riskLevelConfig.type" size="large" class="risk-tag">
              {{ riskLevelConfig.label }}
            </el-tag>
          </div>
        </div>
        <div class="evaluation-text">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ scoreEvaluation }}</span>
        </div>
      </el-card>

      <!-- 2. 风险指数可视化区 - 雷达图 -->
      <el-card class="chart-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">防骗能力雷达图</span>
            <span class="card-subtitle">多维度能力评估可视化</span>
          </div>
        </template>
        <div ref="radarChartEl" class="radar-chart"></div>
      </el-card>

      <!-- 3. 风险维度分析区 -->
      <el-card class="dimensions-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">风险维度分析</span>
            <span class="card-subtitle">各维度详细得分与说明</span>
          </div>
        </template>
        <div class="dimensions-grid">
          <div
            v-for="dim in report.dimensions"
            :key="dim.dimension"
            class="dimension-item"
          >
            <div class="dimension-header">
              <h3 class="dimension-name">{{ dim.name }}</h3>
              <el-tag
                :type="dim.level === 'low' ? 'success' : dim.level === 'medium' ? 'warning' : 'danger'"
                size="small"
              >
                {{ dim.score }}分
              </el-tag>
            </div>
            <el-progress
              :percentage="dim.score"
              :color="dim.score < 40 ? '#67c23a' : dim.score < 70 ? '#e6a23c' : '#f56c6c'"
              :stroke-width="12"
              :show-text="true"
            />
            <p class="dimension-desc">
              {{ getDimensionDescription(dim.dimension, dim.score) }}
            </p>
          </div>
        </div>
      </el-card>

      <!-- 4. 风险点总结区 -->
      <el-card v-if="weakPoints.length > 0" class="weak-points-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon class="warning-icon"><WarningFilled /></el-icon>
            <span class="card-title">主要薄弱环节</span>
          </div>
        </template>
        <div class="weak-points-list">
          <el-alert
            v-for="(point, index) in weakPoints"
            :key="point.dimension"
            :title="`${index + 1}. ${point.name}`"
            :description="getWeakPointDescription(point.dimension, point.score)"
            :type="point.score >= 70 ? 'error' : 'warning'"
            show-icon
            :closable="false"
            class="weak-point-alert"
          />
        </div>
      </el-card>

      <!-- 5. 个性化防骗建议区 -->
      <el-card class="suggestions-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <el-icon class="suggestion-icon"><MagicStick /></el-icon>
            <span class="card-title">个性化防骗建议</span>
          </div>
        </template>
        <div class="suggestions-content">
          <el-timeline>
            <el-timeline-item
              v-for="(suggestion, index) in report.suggestions"
              :key="index"
              :timestamp="`建议 ${index + 1}`"
              placement="top"
            >
              <p class="suggestion-text">{{ suggestion }}</p>
            </el-timeline-item>
          </el-timeline>

          <!-- 推荐内容 -->
          <div class="recommendations">
            <div class="recommendation-section">
              <h4 class="recommendation-title">
                <el-icon><Reading /></el-icon>
                推荐学习内容
              </h4>
              <div class="recommendation-buttons">
                <el-button
                  v-for="point in weakPoints.slice(0, 2)"
                  :key="point.dimension"
                  type="primary"
                  plain
                  @click="goToKnowledge(point.dimension)"
                >
                  学习{{ point.name }}相关知识
                </el-button>
                <el-button type="primary" plain @click="goToKnowledge()">
                  浏览全部防骗知识
                </el-button>
              </div>
            </div>
            <div class="recommendation-section">
              <h4 class="recommendation-title">
                <el-icon><Trophy /></el-icon>
                推荐训练模块
              </h4>
              <el-button type="success" plain @click="goToTraining">
                开始识别训练
              </el-button>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 6. 底部操作区 -->
      <el-card class="actions-card" shadow="never">
        <div class="actions-content">
          <el-button type="primary" size="large" @click="reAssessment">
            <el-icon><Refresh /></el-icon>
            重新测评
          </el-button>
          <el-button type="success" size="large" @click="goToKnowledge()">
            <el-icon><Reading /></el-icon>
            去学习推荐内容
          </el-button>
          <el-button size="large" @click="goToTraining">
            <el-icon><Trophy /></el-icon>
            去做识别训练
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>


<style scoped lang="scss">
.report-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 20px;
}

.empty-card {
  max-width: 800px;
  margin: 100px auto;
}

.report-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

// 1. 顶部概览区
.overview-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;

  :deep(.el-card__body) {
    padding: 30px;
  }

  .overview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 20px;

    .user-avatar {
      background: rgba(255, 255, 255, 0.2);
      color: white;
      font-weight: bold;
    }

    .user-details {
      .user-name {
        margin: 0 0 8px 0;
        font-size: 28px;
        font-weight: 600;
      }

      .report-time {
        margin: 0;
        font-size: 14px;
        opacity: 0.9;
      }
    }
  }

  .score-section {
    text-align: center;

    .score-value {
      font-size: 56px;
      font-weight: 700;
      line-height: 1;
      margin-bottom: 8px;
    }

    .score-label {
      font-size: 16px;
      opacity: 0.9;
      margin-bottom: 12px;
    }

    .risk-tag {
      font-size: 16px;
      padding: 8px 20px;
    }
  }

  .evaluation-text {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 16px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 8px;
    font-size: 16px;
    backdrop-filter: blur(10px);
  }
}

// 2. 图表卡片
.chart-card {
  :deep(.el-card__header) {
    padding: 20px;
    border-bottom: 1px solid #ebeef5;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .card-title {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }

    .card-subtitle {
      font-size: 14px;
      color: #909399;
    }
  }

  .radar-chart {
    width: 100%;
    height: 450px;
    min-height: 400px;
  }
}

// 3. 维度分析区
.dimensions-card {
  :deep(.el-card__header) {
    padding: 20px;
    border-bottom: 1px solid #ebeef5;
  }

  .dimensions-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 24px;
  }

  .dimension-item {
    padding: 20px;
    background: #f8f9fa;
    border-radius: 8px;
    transition: all 0.3s;

    &:hover {
      background: #f0f2f5;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .dimension-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .dimension-name {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }
    }

    .dimension-desc {
      margin: 12px 0 0 0;
      font-size: 14px;
      color: #606266;
      line-height: 1.6;
    }
  }
}

// 4. 薄弱环节区
.weak-points-card {
  :deep(.el-card__header) {
    padding: 20px;
    border-bottom: 1px solid #ebeef5;
    display: flex;
    align-items: center;
    gap: 8px;

    .warning-icon {
      color: #e6a23c;
      font-size: 20px;
    }
  }

  .weak-points-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .weak-point-alert {
    :deep(.el-alert__title) {
      font-size: 16px;
      font-weight: 600;
    }
  }
}

// 5. 建议区
.suggestions-card {
  :deep(.el-card__header) {
    padding: 20px;
    border-bottom: 1px solid #ebeef5;
    display: flex;
    align-items: center;
    gap: 8px;

    .suggestion-icon {
      color: #409eff;
      font-size: 20px;
    }
  }

  .suggestions-content {
    .suggestion-text {
      margin: 0;
      font-size: 15px;
      line-height: 1.8;
      color: #606266;
    }

    .recommendations {
      margin-top: 30px;
      padding-top: 30px;
      border-top: 1px solid #ebeef5;
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 24px;
    }

    .recommendation-section {
      .recommendation-title {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 16px 0;
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }

      .recommendation-buttons {
        display: flex;
        flex-direction: column;
        gap: 12px;
      }
    }
  }
}

// 6. 底部操作区
.actions-card {
  background: transparent;
  border: none;

  :deep(.el-card__body) {
    padding: 30px;
  }

  .actions-content {
    display: flex;
    justify-content: center;
    gap: 16px;
    flex-wrap: wrap;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .report-page {
    padding: 10px;
  }

  .overview-header {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }

  .dimensions-grid {
    grid-template-columns: 1fr !important;
  }

  .recommendations {
    grid-template-columns: 1fr !important;
  }

  .actions-content {
    flex-direction: column;

    .el-button {
      width: 100%;
    }
  }
}
</style>
