<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '../stores/user';
import { useAssessmentStore, type AssessmentReport, type DimensionKey } from '../store/assessment';
import RiskRadar from '../components/RiskRadar.vue';
import http from '../api/http';

const router = useRouter();
const userStore = useUserStore();
const assessmentStore = useAssessmentStore();

// 0: 引导页  1: 答题页  2: 结果页
const step = ref<0 | 1 | 2>(0);
const answered = ref<Record<number, number | number[]>>({});
const idx = ref(0);
const report = ref<AssessmentReport | null>(null);

type QuestionType = 'single' | 'multi';
type QuestionView = {
  id: number;
  text: string;
  dimension: DimensionKey;
  weight: number;
  qType: QuestionType;
  optionLabels: { id: number; label: string; value: number }[];
};

const questions = ref<QuestionView[]>([]);
const loading = ref(false);
const loadingQuestions = ref(false);

const total = computed(() => questions.value.length);
const current = computed(() => questions.value[idx.value]);
const progress = computed(() => (total.value ? Math.round(((idx.value + 1) / total.value) * 100) : 0));
const estimatedMinutes = computed(() => (total.value ? Math.max(1, Math.round(total.value / 4)) : 3));

const isCurrentAnswered = computed(() => {
  const q = current.value;
  if (!q) return false;
  const value = answered.value[q.id];
  if (q.qType === 'single') {
    return value != null;
  }
  return Array.isArray(value) && value.length > 0;
});

const riskLevelLabel = computed(() => {
  if (!report.value) return '';
  const map: Record<string, string> = {
    low: '低风险',
    medium: '中等风险',
    high: '高风险',
  };
  return map[report.value.level] ?? report.value.level;
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

const dimMap = computed<Record<DimensionKey, number>>(() => {
  const m: Record<DimensionKey, number> = { info: 0, finance: 0, psych: 0 };
  report.value?.dimensions.forEach((d) => {
    m[d.dimension] = d.score;
  });
  return m;
});

const mainRisks = computed(() => {
  if (!report.value) return [];
  return [...report.value.dimensions]
    .sort((a, b) => b.score - a.score)
    .slice(0, 3);
});

const weakestDimension = computed<DimensionKey | null>(() => {
  if (!report.value) return null;
  const dims = [...report.value.dimensions].sort((a, b) => b.score - a.score);
  return dims[0]?.dimension ?? null;
});

const startAssessment = () => {
  if (!total.value) {
    ElMessage.warning('题目数据尚未加载完成，请稍后重试。');
    return;
  }
  step.value = 1;
};

const next = () => {
  if (!isCurrentAnswered.value) {
    ElMessage.warning('请先作答当前题目，再进入下一题。');
    return;
  }
  if (idx.value < total.value - 1) {
    idx.value += 1;
  }
};

const prev = () => {
  if (idx.value === 0) {
    step.value = 0;
    return;
  }
  if (idx.value > 0) idx.value -= 1;
};

const goReportPage = () => {
  router.push('/report');
};

const goRecommendedKnowledge = () => {
  const dim = weakestDimension.value;
  if (!dim) {
    router.push('/knowledge');
    return;
  }
  const category = dim === 'finance' ? '社交' : dim === 'info' ? '网站' : '电话';
  router.push({ path: '/knowledge', query: { category } });
};

const goTraining = () => {
  router.push('/train');
};

async function calcRisk() {
  if (!isCurrentAnswered.value) {
    ElMessage.warning('请先完成当前题目再提交。');
    return;
  }
  // 简单校验：是否有未作答题目
  const missing = questions.value.filter((q) => {
    const v = answered.value[q.id];
    return v == null || (Array.isArray(v) && !v.length);
  });
  if (missing.length) {
    ElMessageBox.alert('还有未作答的题目，请完成全部题目后再提交。', '提示', {
      type: 'warning',
    });
    return;
  }
  if (!userStore.userId) {
    ElMessageBox.confirm(
      '为了为你生成个性化报告并保存历史记录，建议先在个人中心登录/注册。现在前往个人中心吗？',
      '未登录提示',
      {
        confirmButtonText: '去个人中心',
        cancelButtonText: '暂不，继续留在本页',
        type: 'info',
      },
    )
      .then(() => {
        router.push('/profile');
      })
      .catch(() => {
        // 用户选择留在本页，仅提示但不强制跳转
      });
    return;
  }

  loading.value = true;
  const payload = {
    userId: userStore.userId,
    answers: questions.value.map((q) => {
      const raw = answered.value[q.id];
      const optionIds = Array.isArray(raw) ? raw : [raw as number];
      return { questionId: q.id, optionIds };
    }),
  };
  try {
    const resp = await http.post('/assessment', payload);
    const data = resp.data as AssessmentReport;
    assessmentStore.setReport(data);
    userStore.setRisk(data.score, data.level);
    report.value = data;
    step.value = 2;
  } catch (e: any) {
    ElMessage.error(e?.message || '提交测评失败，请稍后重试。');
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  loadingQuestions.value = true;
  try {
    const resp = await http.get('/questions');
    const list = resp.data as {
      id: number;
      text: string;
      dimension: DimensionKey;
      weight: number;
      type: QuestionType;
      options: { id: number; label: string; value: number }[];
    }[];
    questions.value = list.map((q) => ({
      id: q.id,
      text: q.text,
      dimension: q.dimension,
      weight: q.weight,
      qType: q.type,
      optionLabels: q.options,
    }));
  } catch {
    // 后端不可用时保留空态提示，避免控制台刷 warning
  } finally {
    loadingQuestions.value = false;
  }
});
</script>

<template>
  <el-card class="assessment-page">
    <template #header>
      <div class="af-title-row">
        <div>
          <div class="title">防诈骗风险测评</div>
          <div class="subtitle">通过多维度问卷，评估你在信息防护、金融安全、心理防线等方面的防骗能力。</div>
        </div>
        <el-tag v-if="report" :type="riskTagType" size="large" round>
          当前等级：{{ riskLevelLabel }}
        </el-tag>
      </div>
    </template>

    <!-- 引导页 -->
    <template v-if="step === 0">
      <el-skeleton v-if="loadingQuestions" :rows="3" animated />
      <template v-else-if="total">
        <div class="guide">
          <div class="guide-main af-soft-panel">
            <div class="guide-title">测评说明</div>
            <ul class="guide-list">
              <li>本测评共 {{ total }} 道题，预计用时 {{ estimatedMinutes }} 分钟。</li>
              <li>涵盖 <strong>信息防护、金融安全、心理防线</strong> 三个风险维度。</li>
              <li>请根据真实习惯与直觉作答，以便得到更准确的评估结果。</li>
            </ul>
            <div class="guide-meta">
              <el-tag type="info" round>支持中途保存结果（登录状态下）</el-tag>
              <el-tag type="warning" round>答案无对错，仅反映风险暴露程度</el-tag>
            </div>
            <div class="af-actions">
              <el-button type="primary" size="large" @click="startAssessment">
                开始测评
              </el-button>
              <el-button size="large" plain @click="$router.push('/train')">先去做识别训练</el-button>
            </div>
          </div>
        </div>
      </template>
      <el-alert
        v-else
        title="暂无测评分题数据，请先在后端配置 questions.json。"
        type="warning"
        show-icon
      />
    </template>

    <!-- 答题页 -->
    <template v-else-if="step === 1">
      <el-skeleton v-if="loadingQuestions" :rows="3" animated />
      <template v-else-if="total">
        <div class="progress-row">
          <div>题目 {{ idx + 1 }} / {{ total }}</div>
          <div class="text-muted">未作答题目将无法进入下一题</div>
        </div>
        <el-progress :percentage="progress" :text-inside="true" status="success" />

        <transition name="fade-slide" mode="out-in">
          <div :key="current?.id" class="question">
            <div class="q-title">{{ current?.text }}</div>
            <div class="q-body af-soft-panel">
              <el-radio-group
                v-if="current?.qType === 'single'"
                v-model="answered[current.id]"
              >
                <el-radio-button
                  v-for="opt in current.optionLabels"
                  :key="opt.id"
                  :value="opt.id"
                >
                  {{ opt.label }}
                </el-radio-button>
              </el-radio-group>
              <el-checkbox-group v-else v-model="answered[current!.id]">
                <el-checkbox v-for="opt in current!.optionLabels" :key="opt.id" :value="opt.id">
                  {{ opt.label }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </transition>

        <div class="actions af-actions">
          <el-button @click="prev">返回</el-button>
          <div class="actions-right">
            <el-button
              v-if="idx < total - 1"
              type="primary"
              @click="next"
              :disabled="!isCurrentAnswered"
            >
              下一题
            </el-button>
            <el-button
              v-else
              type="primary"
              :loading="loading"
              :disabled="!isCurrentAnswered"
              @click="calcRisk"
            >
              提交并生成评估结果
            </el-button>
          </div>
        </div>
      </template>
      <el-alert
        v-else
        title="暂无测评分题数据，请先在后端配置 questions.json。"
        type="warning"
        show-icon
      />
    </template>

    <!-- 结果页 -->
    <template v-else>
      <el-alert
        v-if="!report"
        title="暂无评估结果，请返回重新完成测评。"
        type="info"
        show-icon
      />
      <template v-else>
        <el-result
          :title="`综合风险指数：${report.score}`"
          :sub-title="`风险等级：${riskLevelLabel}`"
          icon="warning"
        >
          <template #extra>
            <div class="summary">
              <span>评估对象：{{ userStore.name || '未命名用户' }}</span>
              <span class="text-muted">建议结合识别训练与防骗知识库持续提升防骗能力。</span>
            </div>
          </template>
        </el-result>

        <div class="charts">
          <risk-radar
            :info="dimMap.info"
            :finance="dimMap.finance"
            :psych="dimMap.psych"
          />
          <div class="risk-points af-soft-panel">
            <div class="card-section-title">主要风险点</div>
            <el-empty v-if="!mainRisks.length" description="暂无风险点数据" :image-size="70" />
            <el-timeline v-else>
              <el-timeline-item
                v-for="(d, i) in mainRisks"
                :key="d.dimension"
                :timestamp="`维度 ${i + 1}`"
                placement="top"
              >
                <div class="risk-item">
                  <div class="risk-name">{{ d.name }}</div>
                  <div class="risk-score">风险指数：{{ d.score }}</div>
                  <div class="risk-level text-muted">等级：{{ d.level }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>

        <el-card class="advice" shadow="never">
          <div class="card-section-title">个性化防诈建议</div>
          <el-alert
            v-if="report.explanation"
            :title="report.explanation"
            type="info"
            show-icon
            style="margin-bottom: 12px"
          />
          <el-timeline>
            <el-timeline-item
              v-for="(s, i) in report.suggestions"
              :key="i"
              :timestamp="`建议 ${i + 1}`"
              placement="top"
            >
              <p>{{ s }}</p>
            </el-timeline-item>
          </el-timeline>

          <div class="card-section-title" style="margin-top: 16px">推荐训练与学习模块</div>
          <div class="af-actions">
            <el-button type="primary" @click="goTraining">前往识别训练</el-button>
            <el-button type="success" @click="goRecommendedKnowledge">学习推荐知识</el-button>
            <el-button text type="primary" @click="goReportPage">查看完整报告页</el-button>
          </div>
        </el-card>
      </template>
    </template>
  </el-card>
</template>

<style scoped lang="scss">
.assessment-page {
  overflow: hidden;
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

.guide {
  margin-top: 4px;
}

.guide-main {
  animation: fade-in 0.4s ease-out;
}

.guide-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.guide-list {
  padding-left: 20px;
  margin: 0 0 8px;
}

.guide-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.progress-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.question {
  margin-top: 12px;
}

.q-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.q-body {
  /* 视觉样式由全局 .af-soft-panel 统一 */
}

.actions {
  justify-content: space-between;
}

.actions-right {
  display: flex;
  gap: 8px;
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.charts {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr);
  gap: 12px;
  margin-top: 12px;
}

.risk-points {
  max-height: 340px;
  overflow: auto;
}

.risk-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.advice {
  margin-top: 12px;
}

/* 卡片切换动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.25s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 960px) {
  .charts {
    grid-template-columns: 1fr;
  }
}
</style>
