<script setup lang="ts">
import { computed, onMounted, ref, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Lock } from '@element-plus/icons-vue';
import { useUserStore } from '../stores/user';
import { useAssessmentStore, type AssessmentReport, type DimensionKey } from '../store/assessment';
import http from '../api/http';

const router = useRouter();
const userStore = useUserStore();
const assessmentStore = useAssessmentStore();

// 0: 引导页  1: 答题页  2: 结果页（只显示分数）
const step = ref<0 | 1 | 2>(0);
const answered = ref<Record<number, number | number[]>>({});
const idx = ref(0);
const currentReport = ref<AssessmentReport | null>(null);

type QuestionType = 'single' | 'multi';
type QuestionView = {
  id: number;
  text: string;
  dimension: DimensionKey;
  weight: number;
  qType: QuestionType;
  optionLabels: { id: number; label: string; value: number }[];
};

const allQuestions = ref<QuestionView[]>([]);
const questions = ref<QuestionView[]>([]); // 随机选出的10题
const loading = ref(false);
const loadingQuestions = ref(false);

const total = computed(() => questions.value.length);
const current = computed(() => questions.value[idx.value]);
const progress = computed(() => (total.value ? Math.round(((idx.value + 1) / total.value) * 100) : 0));

const isCurrentAnswered = computed(() => {
  const q = current.value;
  if (!q) return false;
  const value = answered.value[q.id];
  if (q.qType === 'single') {
    return value != null;
  }
  return Array.isArray(value) && value.length > 0;
});

// 报告列表相关
const reportList = computed(() => assessmentStore.reportHistory);
const reportPage = ref(1);
const reportPageSize = ref(12); // 一行四列，每页12个（3行）
const reportScoreFilter = ref<string>(''); // 分数筛选：'all' | 'low' | 'medium' | 'high'

// 筛选后的报告列表
const filteredReports = computed(() => {
  let list = [...reportList.value];
  if (reportScoreFilter.value && reportScoreFilter.value !== 'all') {
    if (reportScoreFilter.value === 'low') {
      list = list.filter(r => r.score < 40);
    } else if (reportScoreFilter.value === 'medium') {
      list = list.filter(r => r.score >= 40 && r.score < 70);
    } else if (reportScoreFilter.value === 'high') {
      list = list.filter(r => r.score >= 70);
    }
  }
  return list;
});

// 分页后的报告列表
const paginatedReports = computed(() => {
  const start = (reportPage.value - 1) * reportPageSize.value;
  const end = start + reportPageSize.value;
  return filteredReports.value.slice(start, end);
});

const reportTotal = computed(() => filteredReports.value.length);

// 随机选择10题
const selectRandomQuestions = () => {
  if (allQuestions.value.length === 0) return;
  const shuffled = [...allQuestions.value].sort(() => Math.random() - 0.5);
  questions.value = shuffled.slice(0, Math.min(10, shuffled.length));
  // 重置答题状态
  answered.value = {};
  idx.value = 0;
};

const startAssessment = () => {
  if (allQuestions.value.length === 0) {
    ElMessage.warning('题目数据尚未加载完成，请稍后重试。');
    return;
  }
  selectRandomQuestions();
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

// 自动进入下一题的标志，避免重复触发
const isAutoNexting = ref(false);
const autoNextInitialized = ref(false);

// 监听当前题目的答案变化，自动进入下一题（非最后一题）
watch(
  () => {
    const q = current.value;
    if (!q) return null;
    const answer = answered.value[q.id];
    // 返回一个包含题目ID和答案的对象，确保能正确监听变化
    return { questionId: q.id, answer };
  },
  async (newValue, oldValue) => {
    // 如果正在自动跳转中，跳过
    if (isAutoNexting.value) {
      return;
    }
    
    // 如果没有当前题目，跳过
    const q = current.value;
    if (!q) return;
    
    // 首次进入 watch 视为初始化，避免初始化时触发；之后开始允许自动跳转
    if (!autoNextInitialized.value) {
      autoNextInitialized.value = true;
      return;
    }
    
    // 如果新答案为空或未定义，跳过（用户还没有选择）
    if (newValue?.answer == null) {
      return;
    }
    
    // 如果是多选题，检查是否至少选择了一个选项
    if (q.qType === 'multi') {
      if (!Array.isArray(newValue.answer) || newValue.answer.length === 0) {
        return;
      }
      // 如果只是取消选择（从有选项变成空数组），不触发自动跳转
      if (Array.isArray(oldValue?.answer) && oldValue.answer.length > 0 && newValue.answer.length === 0) {
        return;
      }
    }
    
    // 如果是最后一题，不自动跳转
    if (idx.value >= total.value - 1) {
      return;
    }
    
    // 确保当前题目已作答
    if (!isCurrentAnswered.value) {
      return;
    }
    
    // 延迟一小段时间，让用户看到选项被选中的反馈
    isAutoNexting.value = true;
    await nextTick();
    
    setTimeout(() => {
      // 再次检查是否还是当前题目（防止用户快速切换）
      if (idx.value < total.value - 1 && current.value?.id === q.id) {
        idx.value += 1;
      }
      // 重置标志，等待下一题
      setTimeout(() => {
        isAutoNexting.value = false;
      }, 300);
    }, 600); // 600ms延迟，让用户看到选择反馈
  },
  { deep: true }
);

const prev = () => {
  if (idx.value === 0) {
    step.value = 0;
    return;
  }
  if (idx.value > 0) idx.value -= 1;
};

const goReportDetail = (reportId?: string) => {
  if (reportId) {
    router.push(`/report/${reportId}`);
  } else if (currentReport.value?.id) {
    router.push(`/report/${currentReport.value.id}`);
  }
};

const goBack = () => {
  step.value = 0;
  currentReport.value = null;
  answered.value = {};
  idx.value = 0;
};

// 处理多选题的选项切换
const toggleMultiOption = (optionId: number) => {
  const q = current.value;
  if (!q) return;
  
  const currentAnswer = answered.value[q.id];
  if (!Array.isArray(currentAnswer)) {
    answered.value[q.id] = [optionId];
  } else {
    const index = currentAnswer.indexOf(optionId);
    if (index > -1) {
      currentAnswer.splice(index, 1);
    } else {
      currentAnswer.push(optionId);
    }
  }
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
    try {
      await ElMessageBox.confirm(
        '为了为你生成个性化报告并保存历史记录，建议先在个人中心登录/注册。现在前往个人中心吗？',
        '未登录提示',
        {
          confirmButtonText: '去个人中心',
          cancelButtonText: '暂不，继续留在本页',
          type: 'info',
        },
      );
      router.push('/user-center');
    } catch {
      // 用户选择留在本页，仅提示但不强制跳转
    }
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
    currentReport.value = data;
    step.value = 2; // 只显示分数
  } catch (e: any) {
    ElMessage.error(e?.message || '提交测评失败，请稍后重试。');
  } finally {
    loading.value = false;
  }
}

// 获取风险等级标签
const getRiskLevelLabel = (level: string) => {
  const map: Record<string, string> = {
    low: '低风险',
    medium: '中等风险',
    high: '高风险',
  };
  return map[level] ?? level;
};

// 获取风险等级标签类型
const getRiskTagType = (level: string): 'success' | 'warning' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'danger'> = {
    low: 'success',
    medium: 'warning',
    high: 'danger',
  };
  return map[level] ?? 'success';
};

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
    allQuestions.value = list.map((q) => ({
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
  <div class="assessment-container">
    <!-- 上半部分：风险测评 -->
    <el-card class="assessment-section">
      <template #header>
        <div class="af-title-row">
          <div>
            <div class="title">防诈骗风险测评</div>
            <div class="subtitle">通过多维度问卷，评估你在信息防护、金融安全、心理防线等方面的防骗能力。</div>
          </div>
        </div>
      </template>

      <!-- 引导页 -->
      <template v-if="step === 0">
        <el-skeleton v-if="loadingQuestions" :rows="3" animated />
        <template v-else-if="allQuestions.length">
          <div class="guide">
            <div class="guide-main af-soft-panel">
              <div class="guide-title">测评说明</div>
              <ul class="guide-list">
                <li>本测评将随机抽取 <strong>10 道题</strong>，预计用时 3-5 分钟。</li>
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
          title="暂无测评分题数据，请先在后端配置题目。"
          type="warning"
          show-icon
        />
      </template>

      <!-- 答题页 -->
      <template v-else-if="step === 1">
        <!-- 顶部区域 -->
        <div class="assessment-header">
          <div class="header-icon">
            <el-icon :size="32"><Lock /></el-icon>
          </div>
          <div class="header-content">
            <h1 class="header-title">防诈骗风险测评</h1>
            <p class="header-subtitle">10道情景题 · 约2分钟 · 生成专属风险评估</p>
          </div>
        </div>

        <!-- 进度区域 -->
        <div class="progress-section">
          <div class="progress-info">
            <span class="progress-text">第 {{ idx + 1 }} 题 / 共 {{ total }} 题</span>
          </div>
          <el-progress 
            :percentage="progress" 
            :stroke-width="12"
            :show-text="false"
            class="progress-bar"
            color="#409eff"
          />
        </div>

        <!-- 题目区域 -->
        <div class="question-wrapper">
          <transition name="fade-slide" mode="out-in">
            <div :key="current?.id" class="question-section">
            <!-- 场景提示卡片 -->
            <div class="scene-hint-card">
              <el-icon class="hint-icon"><Lock /></el-icon>
              <span class="hint-text">请根据您的真实情况选择最符合的答案</span>
            </div>
            
            <!-- 问题文本 -->
            <div class="question-text">{{ current?.text }}</div>
            
            <!-- 选项区域 -->
            <div class="options-container">
              <div
                v-if="current?.qType === 'single'"
                class="options-list"
              >
                <div
                  v-for="opt in current.optionLabels"
                  :key="opt.id"
                  class="option-card"
                  :class="{ 'option-selected': answered[current.id] === opt.id }"
                  @click="answered[current.id] = opt.id"
                >
                  <div class="option-indicator"></div>
                  <div class="option-content">
                    <span class="option-label">{{ opt.label }}</span>
                  </div>
                </div>
              </div>
              <div
                v-else
                class="options-list"
              >
                <div
                  v-for="opt in current!.optionLabels"
                  :key="opt.id"
                  class="option-card"
                  :class="{ 'option-selected': Array.isArray(answered[current!.id]) && answered[current!.id].includes(opt.id) }"
                  @click="toggleMultiOption(opt.id)"
                >
                  <div class="option-indicator"></div>
                  <div class="option-content">
                    <span class="option-label">{{ opt.label }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          </transition>
        </div>

        <!-- 底部操作区 -->
        <div class="actions-section">
          <el-button 
            size="large" 
            @click="prev"
            class="action-btn-secondary"
          >
            返回
          </el-button>
          <div class="action-hint" v-if="!isCurrentAnswered">
            <span class="hint-text-small">请先选择答案</span>
          </div>
          <el-button
            v-if="idx < total - 1"
            type="primary"
            size="large"
            @click="next"
            :disabled="!isCurrentAnswered"
            class="action-btn-primary"
          >
            下一题
          </el-button>
          <el-button
            v-else
            type="primary"
            size="large"
            :loading="loading"
            :disabled="!isCurrentAnswered"
            @click="calcRisk"
            class="action-btn-primary"
          >
            提交并生成评估结果
          </el-button>
        </div>
        
        <div v-if="idx < total - 1 && isCurrentAnswered && isAutoNexting" class="auto-next-hint">
          <span>正在进入下一题...</span>
        </div>
      </template>

      <!-- 结果页（只显示分数） -->
      <template v-else>
        <div class="result-container">
          <el-alert
            v-if="!currentReport"
            title="暂无评估结果，请返回重新完成测评。"
            type="info"
            show-icon
            class="result-alert"
          />
          <template v-else>
            <el-result
              :title="`综合风险指数：${currentReport.score}`"
              :sub-title="`风险等级：${getRiskLevelLabel(currentReport.level)}`"
              icon="warning"
              class="result-content"
            >
              <template #extra>
                <div class="result-actions">
                  <el-button type="primary" size="large" @click="goReportDetail()">
                    查看报告
                  </el-button>
                  <el-button size="large" @click="goBack">返回</el-button>
                </div>
              </template>
            </el-result>
          </template>
        </div>
      </template>
    </el-card>

    <!-- 下半部分：报告列表 -->
    <el-card class="reports-section" v-if="step === 0">
      <template #header>
        <div class="reports-header">
          <div>
            <div class="title">历史报告</div>
            <div class="subtitle">查看您的历史测评报告</div>
          </div>
          <div class="reports-filters">
            <el-select
              v-model="reportScoreFilter"
              placeholder="按风险等级筛选"
              clearable
              style="width: 180px"
              @change="reportPage = 1"
            >
              <el-option label="全部" value="all" />
              <el-option label="低风险 (< 40分)" value="low" />
              <el-option label="中等风险 (40-70分)" value="medium" />
              <el-option label="高风险 (≥ 70分)" value="high" />
            </el-select>
          </div>
        </div>
      </template>

      <div v-if="reportList.length === 0" class="empty-reports">
        <el-empty description="暂无历史报告，完成测评后会自动保存" :image-size="100" />
      </div>
      <div v-else>
        <div class="reports-grid">
          <div
            v-for="report in paginatedReports"
            :key="report.id"
            class="report-card"
            @click="goReportDetail(report.id)"
          >
            <div class="report-card-header">
              <el-tag :type="getRiskTagType(report.level)" size="small">
                {{ getRiskLevelLabel(report.level) }}
              </el-tag>
              <span class="report-date">{{ new Date(report.createdAt).toLocaleDateString('zh-CN') }}</span>
            </div>
            <div class="report-score">{{ report.score }}</div>
            <div class="report-score-label">综合风险指数</div>
            <div class="report-dimensions">
              <div
                v-for="dim in report.dimensions"
                :key="dim.dimension"
                class="dimension-item"
              >
                <span class="dim-name">{{ dim.name }}</span>
                <span class="dim-score">{{ dim.score }}分</span>
              </div>
            </div>
          </div>
        </div>
        <div class="reports-pagination">
          <el-pagination
            v-model:current-page="reportPage"
            :page-size="reportPageSize"
            :total="reportTotal"
            layout="prev, pager, next, total"
            @current-change="() => {}"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.assessment-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow-x: hidden; // 防止水平滚动条
  overflow-y: auto; // 允许垂直滚动
  
  @media (max-width: 768px) {
    padding: 20px 12px;
  }
}

.assessment-section {
  width: 100%;
  max-width: 800px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  position: relative;
  
  :deep(.el-card__header) {
    display: none; // 隐藏默认header，使用自定义header
  }
  
  :deep(.el-card__body) {
    padding: 0;
    overflow: hidden; // 防止内容溢出导致滚动条
  }
}

// 保留原有的title和subtitle样式用于其他部分
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
  padding: 40px;
  
  @media (max-width: 768px) {
    padding: 24px 20px;
  }
}

.guide-main {
  animation: fade-in 0.4s ease-out;
  padding: 32px;
  background: #fafbfc;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  
  @media (max-width: 768px) {
    padding: 24px;
  }
}

.guide-title {
  font-weight: 700;
  font-size: 20px;
  margin-bottom: 16px;
  color: #303133;
}

.guide-list {
  padding-left: 20px;
  margin: 0 0 20px;
  line-height: 1.8;
  color: #606266;
  
  li {
    margin-bottom: 8px;
  }
}

.guide-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

// 顶部区域
.assessment-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 32px 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  
  .header-icon {
    flex-shrink: 0;
    width: 56px;
    height: 56px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(10px);
  }
  
  .header-content {
    flex: 1;
    
    .header-title {
      margin: 0 0 8px 0;
      font-size: 24px;
      font-weight: 700;
      color: #ffffff;
    }
    
    .header-subtitle {
      margin: 0;
      font-size: 14px;
      color: rgba(255, 255, 255, 0.9);
      line-height: 1.5;
    }
  }
  
  @media (max-width: 768px) {
    padding: 24px 20px;
    
    .header-icon {
      width: 48px;
      height: 48px;
    }
    
    .header-content {
      .header-title {
        font-size: 20px;
      }
      
      .header-subtitle {
        font-size: 13px;
      }
    }
  }
}

// 进度区域
.progress-section {
  padding: 24px 40px;
  background: #fafbfc;
  border-bottom: 1px solid #e4e7ed;
  
  .progress-info {
    margin-bottom: 12px;
    
    .progress-text {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
    }
  }
  
  .progress-bar {
    :deep(.el-progress-bar__outer) {
      background-color: #e4e7ed;
      border-radius: 6px;
      overflow: hidden;
    }
    
    :deep(.el-progress-bar__inner) {
      border-radius: 6px;
      transition: width 0.4s ease;
    }
  }
  
  @media (max-width: 768px) {
    padding: 20px;
  }
}

// 结果页容器
.result-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 500px;
  padding: 40px;
  
  .result-alert {
    max-width: 600px;
    width: 100%;
  }
  
  .result-content {
    width: 100%;
    
    :deep(.el-result) {
      padding: 40px 20px;
    }
  }
  
  @media (max-width: 768px) {
    min-height: 400px;
    padding: 24px 20px;
  }
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
  
  @media (max-width: 768px) {
    flex-direction: column;
    width: 100%;
    
    .el-button {
      width: 100%;
    }
  }
}

// 题目包装器 - 用于动画容器
.question-wrapper {
  position: relative;
  overflow: hidden; // 防止动画时出现滚动条
  min-height: 400px; // 确保有足够高度，避免布局跳动
  width: 100%;
  box-sizing: border-box;
  
  // 确保内部内容不会溢出
  * {
    box-sizing: border-box;
  }
}

// 题目区域
.question-section {
  padding: 32px 40px;
  position: relative;
  width: 100%;
  box-sizing: border-box;
  
  // 场景提示卡片
  .scene-hint-card {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    background: #f0f9ff;
    border-left: 4px solid #409eff;
    border-radius: 8px;
    margin-bottom: 24px;
    
    .hint-icon {
      color: #409eff;
      font-size: 18px;
    }
    
    .hint-text {
      font-size: 14px;
      color: #606266;
      line-height: 1.5;
    }
  }
  
  // 问题文本
  .question-text {
    font-size: 20px;
    font-weight: 700;
    line-height: 1.6;
    color: #303133;
    margin-bottom: 32px;
    padding: 0;
  }
  
  // 选项容器
  .options-container {
    margin-bottom: 8px;
  }
  
  .options-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  
  // 选项卡片
  .option-card {
    position: relative;
    display: flex;
    align-items: center;
    padding: 18px 20px;
    background: #ffffff;
    border: 2px solid #e4e7ed;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s ease;
    min-height: 56px;
    
    .option-indicator {
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 0;
      background: #409eff;
      border-radius: 12px 0 0 12px;
      transition: width 0.3s ease;
    }
    
    .option-content {
      flex: 1;
      display: flex;
      align-items: center;
      
      .option-label {
        font-size: 16px;
        color: #303133;
        line-height: 1.5;
      }
    }
    
    // Hover状态
    &:hover {
      border-color: #409eff;
      background: #f0f9ff;
      transform: translateX(4px);
      box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
      
      .option-indicator {
        width: 4px;
      }
    }
    
    // Selected状态
    &.option-selected {
      border-color: #409eff;
      background: #ecf5ff;
      box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
      
      .option-indicator {
        width: 4px;
      }
      
      .option-content .option-label {
        color: #409eff;
        font-weight: 600;
      }
    }
  }
  
  @media (max-width: 768px) {
    padding: 24px 20px;
    
    .question-text {
      font-size: 18px;
      margin-bottom: 24px;
    }
    
    .option-card {
      padding: 16px 18px;
      min-height: 52px;
      
      .option-content .option-label {
        font-size: 15px;
      }
    }
  }
}

// 底部操作区
.actions-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 40px;
  background: #fafbfc;
  border-top: 1px solid #e4e7ed;
  gap: 16px;
  
  .action-btn-secondary {
    flex-shrink: 0;
    padding: 12px 24px;
    font-size: 15px;
    border-color: #dcdfe6;
    color: #606266;
    
    &:hover {
      border-color: #c0c4cc;
      color: #303133;
    }
  }
  
  .action-hint {
    flex: 1;
    text-align: center;
    
    .hint-text-small {
      font-size: 13px;
      color: #909399;
    }
  }
  
  .action-btn-primary {
    flex-shrink: 0;
    padding: 12px 32px;
    font-size: 15px;
    font-weight: 600;
    
    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
  
  @media (max-width: 768px) {
    padding: 20px;
    flex-direction: column;
    
    .action-btn-secondary,
    .action-btn-primary {
      width: 100%;
    }
    
    .action-hint {
      order: -1;
      width: 100%;
      margin-bottom: 8px;
    }
  }
}

.auto-next-hint {
  text-align: center;
  padding: 12px 40px;
  color: #409eff;
  font-size: 14px;
  animation: fade-in 0.3s ease-out;
  background: #f0f9ff;
  
  span {
    display: inline-block;
    animation: pulse 1.5s ease-in-out infinite;
  }
  
  @media (max-width: 768px) {
    padding: 12px 20px;
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

.actions-right {
  display: flex;
  gap: 8px;
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 卡片切换动画 - 优化为更丝滑的动画 */
.fade-slide-enter-active {
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0.06, 0.68, 0.19);
  position: absolute;
  width: 100%;
  left: 0;
  right: 0;
  top: 0;
  z-index: 1;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px) scale(0.98);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px) scale(0.98);
}

.fade-slide-enter-to,
.fade-slide-leave-from {
  opacity: 1;
  transform: translateX(0) scale(1);
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

// 报告列表样式
.reports-section {
  width: 100%;
  max-width: 800px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  
  :deep(.el-card__header) {
    padding: 24px 40px;
    border-bottom: 1px solid #e4e7ed;
    background: #fafbfc;
    
    @media (max-width: 768px) {
      padding: 20px;
    }
  }
  
  :deep(.el-card__body) {
    padding: 24px 40px;
    
    @media (max-width: 768px) {
      padding: 20px;
    }
  }
  
  .reports-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    @media (max-width: 768px) {
      flex-direction: column;
      align-items: flex-start;
      gap: 16px;
    }
  }

  .reports-filters {
    display: flex;
    gap: 12px;
  }

  .empty-reports {
    padding: 60px 0;
    text-align: center;
  }

  .reports-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 20px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(3, 1fr);
    }

    @media (max-width: 768px) {
      grid-template-columns: repeat(2, 1fr);
    }

    @media (max-width: 480px) {
      grid-template-columns: 1fr;
    }
  }

  .report-card {
    padding: 16px;
    border: 1px solid var(--el-border-color);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
    background: var(--el-bg-color);

    &:hover {
      border-color: var(--el-color-primary);
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
      transform: translateY(-2px);
    }

    .report-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .report-date {
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }

    .report-score {
      font-size: 32px;
      font-weight: 700;
      color: var(--el-color-primary);
      text-align: center;
      margin-bottom: 4px;
    }

    .report-score-label {
      font-size: 12px;
      color: var(--el-text-color-secondary);
      text-align: center;
      margin-bottom: 12px;
    }

    .report-dimensions {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .dimension-item {
        display: flex;
        justify-content: space-between;
        font-size: 12px;

        .dim-name {
          color: var(--el-text-color-regular);
        }

        .dim-score {
          color: var(--el-text-color-secondary);
          font-weight: 500;
        }
      }
    }
  }

  .reports-pagination {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }
}
</style>
