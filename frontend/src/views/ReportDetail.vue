<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import { useAssessmentStore } from '../store/assessment';
import RiskRadar from '../components/RiskRadar.vue';
import { ElMessage } from 'element-plus';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const assessmentStore = useAssessmentStore();

const reportId = computed(() => route.params.id as string);
const report = ref(assessmentStore.getReportById(reportId.value));

// 风险等级标签
const riskLevelLabel = computed(() => {
  if (!report.value) return '';
  const map: Record<string, string> = {
    low: '低风险',
    medium: '中等风险',
    high: '高风险',
  };
  return map[report.value.level] ?? report.value.level;
});

// 题目表现等级标签
const questionLevelLabelMap: Record<string, string> = {
  low: '表现较弱',
  medium: '中等水平',
  high: '表现较好',
};

// 灰色说明块（explanation）的本地化文案
const localizedExplanation = computed(() => {
  if (!report.value?.explanation) return '';
  let text = report.value.explanation as string;
  // 将英文等级替换为中文说明
  text = text.replace(/HIGH/g, '高').replace(/MEDIUM/g, '中等').replace(/LOW/g, '较低');
  return text;
});

const riskTagType = computed<'success' | 'warning' | 'danger'>(() => {
  if (!report.value) return 'success';
  const map: Record<string, 'success' | 'warning' | 'danger'> = {
    low: 'success',
    medium: 'warning',
    high: 'danger',
  };
  return map[report.value.level] ?? 'success';
});

const dimMap = computed(() => {
  const m: Record<string, number> = { info: 0, finance: 0, psych: 0 };
  report.value?.dimensions.forEach((d) => {
    m[d.dimension] = d.score;
  });
  return m;
});

// 维度中文标签
const dimLabelMap: Record<string, string> = {
  info: '信息识别能力',
  finance: '资金安全意识',
  psych: '心理防范能力',
};

// 顶部展示用的重点建议（取前 3 条）
const topSuggestions = computed(() => {
  if (!report.value?.suggestions) return [];
  return report.value.suggestions.slice(0, 3);
});

const mainRisks = computed(() => {
  if (!report.value) return [];
  return [...report.value.dimensions]
    .sort((a, b) => b.score - a.score)
    .slice(0, 3);
});

const weakestDimension = computed(() => {
  if (!report.value) return null;
  const dims = [...report.value.dimensions].sort((a, b) => b.score - a.score);
  return dims[0]?.dimension ?? null;
});

// 顶部“按建议开始学习”滚动定位
const adviceSectionRef = ref<HTMLElement | null>(null);
const scrollToAdvice = () => {
  if (adviceSectionRef.value) {
    adviceSectionRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
};

// 根据维度跳转到知识库，并带上关键词用于筛选
const goKnowledgeByDimension = (dimension?: string) => {
  const dim = dimension || weakestDimension.value;
  if (!dim) {
    router.push('/knowledge');
    return;
  }
  const category = dim === 'finance' ? '社交' : dim === 'info' ? '网站' : '电话';
  const keyword = dimLabelMap[dim] || '';
  router.push({ path: '/knowledge', query: { category, keyword, dimension: dim } });
};

const goRecommendedKnowledge = () => {
  goKnowledgeByDimension();
};

// 根据维度跳转到训练（可携带维度标记，方便后续扩展）
const goTrainingByDimension = (dimension?: string) => {
  if (dimension) {
    router.push({ path: '/train', query: { dimension } });
  } else {
    router.push('/train');
  }
};

const goTraining = () => {
  goTrainingByDimension();
};

const goBack = () => {
  router.push('/assessment');
};

const goRetest = () => {
  router.push('/assessment');
};

onMounted(() => {
  if (!report.value) {
    ElMessage.warning('报告不存在或已删除');
    router.push('/assessment');
  }
});
</script>

<template>
  <div class="report-detail-page">
    <el-card v-if="!report" class="empty-card">
      <el-empty description="报告不存在">
        <el-button type="primary" @click="goBack">返回测评页</el-button>
      </el-empty>
    </el-card>

    <template v-else>
      <el-card class="report-detail-card">
        <template #header>
          <div class="report-header">
            <div>
              <div class="title">风险评估报告详情</div>
              <div class="subtitle">评估时间：{{ new Date(report.createdAt).toLocaleString('zh-CN') }}</div>
            </div>
            <el-button @click="goBack">返回</el-button>
          </div>
        </template>

        <section class="overview-section">
          <div class="overview-grid">
            <div class="overview-main af-soft-panel">
              <div class="overview-main-header">
                <el-result
                  :title="`综合风险指数：${report.score}`"
                  icon="warning"
                >
                  <template #extra>
                    <div class="summary">
                      <div class="summary-main">
                        <span>评估对象：{{ userStore.name || '未命名用户' }}</span>
                        <el-tag :type="riskTagType" effect="dark" size="small">
                          {{ riskLevelLabel }}
                        </el-tag>
                      </div>
                      <div class="summary-sub">
                        <span class="summary-highlight">
                          当前防骗短板主要集中在：
                        </span>
                        <span v-if="weakestDimension">
                          {{ dimLabelMap[weakestDimension as string] || '综合风险维度' }}，
                          建议优先针对该维度进行学习与训练。
                        </span>
                        <span v-else>建议结合识别训练与防骗知识库持续提升防骗能力。</span>
                      </div>
                    </div>
                  </template>
                </el-result>
              </div>
            </div>

            <div class="overview-side af-soft-panel">
              <div class="overview-side-title">本次答题个性化建议</div>
              <p class="overview-side-tip">
                已根据你本次的答题表现自动生成，可作为接下来学习和训练的重点方向。
              </p>
              <el-empty
                v-if="!topSuggestions.length"
                description="暂无个性化建议"
                :image-size="60"
              />
              <div v-else class="overview-suggestion-list">
                <div
                  v-for="(s, i) in topSuggestions"
                  :key="i"
                  class="overview-suggestion-item"
                >
                  <div class="overview-suggestion-header">
                    <el-tag
                      size="small"
                      :type="i === 0 ? 'danger' : i === 1 ? 'warning' : 'info'"
                    >
                      {{ i === 0 ? '优先' : i === 1 ? '短期' : '长期' }}
                    </el-tag>
                    <span class="overview-suggestion-title">建议 {{ i + 1 }}</span>
                  </div>
                  <div class="overview-suggestion-content">
                    {{ s }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="content-section">
          <div class="content-left">
            <div class="af-soft-panel">
              <div class="card-section-title">问题分布与风险画像</div>
              <p class="section-tip">从维度角度整体查看你的风险暴露情况。</p>
              <risk-radar
                :info="dimMap.info"
                :finance="dimMap.finance"
                :psych="dimMap.psych"
              />
            </div>
          </div>

          <div class="content-right af-soft-panel">
            <div class="card-section-title">重点问题维度</div>
            <p class="section-tip">根据答题情况，以下维度存在明显薄弱点：</p>
            <el-empty v-if="!mainRisks.length" description="暂无风险点数据" :image-size="70" />
            <div v-else class="risk-list">
              <div
                v-for="(d, i) in mainRisks"
                :key="d.dimension"
                class="risk-item"
              >
                <div class="risk-item-header">
                  <span class="risk-rank">NO.{{ i + 1 }}</span>
                  <span class="risk-dim-label">
                    {{ dimLabelMap[d.dimension] || d.name }}
                  </span>
                  <el-tag size="small" type="danger">问题集中</el-tag>
                </div>
                <div class="risk-item-body">
                  <div class="risk-score">风险指数：{{ d.score }}</div>
                  <div class="risk-level text-muted">
                    题目表现：{{ questionLevelLabelMap[d.level] || d.level }}
                  </div>
                  <div class="risk-suggestion">
                    学习建议：重点查看与
                    <strong>{{ dimLabelMap[d.dimension] || d.name }}</strong>
                    相关的防骗案例与实战技巧。
                  </div>
                  <div class="risk-actions">
                    <el-button
                      link
                      type="primary"
                      size="small"
                      @click="goKnowledgeByDimension(d.dimension)"
                    >
                      查看相关知识
                    </el-button>
                    <el-button
                      link
                      type="success"
                      size="small"
                      @click="goTrainingByDimension(d.dimension)"
                    >
                      做针对性训练
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <el-card class="advice" shadow="never" ref="adviceSectionRef">
          <div class="card-section-title">学习路径与防诈建议</div>
          <p class="section-tip">
            以下建议基于你的整体风险水平与各维度表现自动生成，建议按从上到下的顺序逐步完成。
          </p>

          <el-alert
            v-if="report.explanation"
            :title="localizedExplanation"
            type="info"
            show-icon
            class="advice-explanation"
          />

          <div class="advice-layout">
            <div class="advice-main">
              <div class="advice-block">
                <div class="advice-block-title">学习建议清单</div>
                <el-timeline>
                  <el-timeline-item
                    v-for="(s, i) in report.suggestions"
                    :key="i"
                    :timestamp="`步骤 ${i + 1}`"
                    placement="top"
                  >
                    <p>{{ s }}</p>
                  </el-timeline-item>
                </el-timeline>
              </div>
            </div>

            <div class="advice-side af-soft-panel">
              <div class="advice-block-title">推荐学习与训练动作</div>
              <ul class="advice-side-list">
                <li>
                  <span class="dot"></span>
                  优先练习与你主要问题维度相关的情景题，提升识别反应速度。
                </li>
                <li>
                  <span class="dot"></span>
                  结合真实诈骗案例，强化“关键危险信号”的敏感度。
                </li>
                <li>
                  <span class="dot"></span>
                  将学到的防骗要点整理成个人“反诈清单”，遇到可疑情形时对照检查。
                </li>
              </ul>

              <div class="af-actions advice-actions">
                <el-button type="primary" @click="goTraining">前往识别训练</el-button>
                <el-button type="success" @click="goRecommendedKnowledge">学习推荐知识</el-button>
                <el-button text @click="goBack">返回测评记录</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-card>
    </template>
  </div>
</template>

<style scoped lang="scss">
.report-detail-page {
  min-height: 100vh;
}

.empty-card {
  max-width: 800px;
  margin: 100px auto;
}

.report-detail-card {
  overflow: hidden;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 20px;
  font-weight: 700;
}

.subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: var(--af-muted);
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-sub {
  font-size: 13px;
  color: var(--af-muted);
}

.summary-highlight {
  font-weight: 600;
  color: var(--af-primary);
  margin-right: 4px;
}

.overview-section {
  margin-top: 4px;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
}

.overview-main {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
}

.overview-main-header :deep(.el-result) {
  margin: 0;
  padding: 4px 0 0;
  text-align: left;
}

.overview-main-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.overview-side {
  font-size: 13px;
}

.overview-side-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.overview-side-tip {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--af-muted);
}

.overview-suggestion-list {
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-suggestion-item {
  padding: 6px 8px;
  border-radius: 6px;
  background: var(--af-soft-bg);
}

.overview-suggestion-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.overview-suggestion-title {
  font-size: 13px;
  font-weight: 500;
}

.overview-suggestion-content {
  font-size: 12px;
  color: var(--af-muted);
}

.content-section {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.content-left,
.content-right {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-tip {
  font-size: 12px;
  color: var(--af-muted);
  margin: 4px 0 8px;
}

.risk-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 360px;
  overflow: auto;
}

.risk-item {
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--af-soft-bg);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.risk-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.risk-rank {
  font-weight: 600;
  color: var(--af-primary);
}

.risk-dim-label {
  font-weight: 600;
}

.risk-item-body {
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.risk-suggestion {
  margin-top: 4px;
}

.risk-actions {
  margin-top: 4px;
  display: flex;
  gap: 8px;
}

.advice {
  margin-top: 12px;
}

.advice-explanation {
  margin-bottom: 12px;
}

.advice-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
  gap: 20px;
  margin-top: 8px;
  align-items: stretch;
}

.advice-block-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.advice-main {
  display: flex;
  flex-direction: column;
}

.advice-block {
  flex: 1;
}

.advice-side {
  font-size: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  line-height: 1.8;
  letter-spacing: 0.02em;
}

.advice-side-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
  flex: 1;
  justify-content: space-between;
}

.advice-side-list li {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  width: 100%;
}

.advice-side-list .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--af-primary);
  margin-top: 6px;
}

.advice-actions {
  margin-top: 16px;
}

@media (max-width: 960px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .content-section {
    grid-template-columns: 1fr;
  }

  .advice-layout {
    grid-template-columns: 1fr;
  }
}
</style>
