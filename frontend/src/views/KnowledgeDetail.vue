<template>
  <div class="knowledge-detail-page">
    <el-page-header @back="goBack" class="page-header">
      <template #content>
        <span class="header-title">防骗知识详情</span>
      </template>
    </el-page-header>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="8" animated />
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon style="margin: 20px 0" />

    <div v-if="!loading && !error && detail" class="detail-content">
      <!-- 详情头部 -->
      <el-card class="detail-header">
        <div class="header-info">
          <div class="title-section">
            <el-tag :type="getRiskTagType(detail.riskLevel)" size="large" class="risk-tag">
              {{ detail.riskLevel }}风险
            </el-tag>
            <el-tag :type="getCategoryTagType(detail.category)" size="small" class="category-tag">
              {{ detail.category }}
            </el-tag>
            <h1 class="detail-title">{{ detail.title }}</h1>
          </div>
          <div class="action-section">
            <el-button 
              :type="knowledgeStore.isRead(detail.id) ? 'success' : 'primary'"
              @click="toggleLearn(detail.id)">
              <el-icon v-if="knowledgeStore.isRead(detail.id)"><Check /></el-icon>
              <el-icon v-else><Document /></el-icon>
              {{ knowledgeStore.isRead(detail.id) ? '已学习' : '标记已学' }}
            </el-button>
          </div>
        </div>
        <p class="detail-summary">{{ detail.summary }}</p>
      </el-card>

      <!-- 完整说明 -->
      <el-card class="detail-section" v-if="detail.content">
        <template #header>
          <div class="section-header">
            <el-icon><Document /></el-icon>
            <span>完整说明</span>
          </div>
        </template>
        <div class="content-text" v-html="formatContent(detail.content)"></div>
      </el-card>

      <!-- 常见诈骗话术 -->
      <el-card class="detail-section" v-if="detail.commonTactics && detail.commonTactics.length">
        <template #header>
          <div class="section-header">
            <el-icon><ChatLineRound /></el-icon>
            <span>常见诈骗话术</span>
          </div>
        </template>
        <div class="tactics-list">
          <el-alert
            v-for="(tactic, index) in detail.commonTactics"
            :key="index"
            :title="tactic"
            type="warning"
            :closable="false"
            class="tactic-item"
          />
        </div>
      </el-card>

      <!-- 真实案例说明 -->
      <el-card class="detail-section" v-if="detail.cases && detail.cases.length">
        <template #header>
          <div class="section-header">
            <el-icon><Warning /></el-icon>
            <span>真实案例说明</span>
          </div>
        </template>
        <div class="cases-list">
          <div v-for="(caseItem, index) in detail.cases" :key="index" class="case-item">
            <div class="case-number">案例 {{ index + 1 }}</div>
            <p class="case-content">{{ caseItem }}</p>
          </div>
        </div>
      </el-card>

      <!-- 防范技巧列表 -->
      <el-card class="detail-section" v-if="detail.preventionTips && detail.preventionTips.length">
        <template #header>
          <div class="section-header">
            <el-icon><Lock /></el-icon>
            <span>防范技巧</span>
          </div>
        </template>
        <div class="tips-list">
          <div v-for="(tip, index) in detail.preventionTips" :key="index" class="tip-item">
            <el-icon class="tip-icon"><CircleCheck /></el-icon>
            <span class="tip-text">{{ tip }}</span>
          </div>
        </div>
      </el-card>

      <!-- 推荐关联识别训练模块 -->
      <el-card class="detail-section" v-if="detail.relatedTraining">
        <template #header>
          <div class="section-header">
            <el-icon><Connection /></el-icon>
            <span>推荐训练</span>
          </div>
        </template>
        <div class="training-recommend">
          <p class="recommend-text">建议您通过以下训练模块巩固学习：</p>
          <el-button type="primary" @click="goToTraining">
            {{ detail.relatedTraining }}
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Document, ChatLineRound, Warning, Lock, Connection, CircleCheck, Check } from '@element-plus/icons-vue';
import http from '../api/http';
import { useKnowledgeStore } from '../store/knowledge';
import { useUserStore } from '../stores/user';
import { useAchievementStore } from '../store/achievement';
import { ElMessage } from 'element-plus';

type KnowledgeDetail = {
  id: number;
  category: string;
  title: string;
  summary: string;
  riskLevel?: string;
  preventionTips?: string[];
  commonTactics?: string[];
  cases?: string[];
  content?: string;
  relatedTraining?: string;
};

const route = useRoute();
const router = useRouter();
const detail = ref<KnowledgeDetail | null>(null);
const loading = ref(false);
const error = ref('');
const knowledgeStore = useKnowledgeStore();
const userStore = useUserStore();
const achievementStore = useAchievementStore();

// 浏览时间跟踪
let viewStartTime: number | null = null;
let viewTimer: ReturnType<typeof setTimeout> | null = null;
const VIEW_DURATION = 30000; // 30秒（毫秒）
let isMarkedAsRead = false; // 防止重复标记

onMounted(() => {
  knowledgeStore.hydrate();
  achievementStore.hydrate();
  loadDetail();
});

onUnmounted(() => {
  stopViewTracking();
});

function loadDetail() {
  const id = Number(route.params.id);
  if (!id) {
    error.value = '无效的知识ID';
    return;
  }

  loading.value = true;
  error.value = '';
  isMarkedAsRead = false; // 重置标记状态

  http
    .get(`/knowledge/${id}`)
    .then((resp) => {
      detail.value = resp.data;
    })
    .catch((err) => {
      error.value = '加载知识详情失败，请稍后重试';
      console.error(err);
      // 使用 mock 数据作为降级方案
      detail.value = {
        id,
        category: '冒充客服',
        title: '假冒客服退款诈骗',
        summary: '诈骗分子冒充电商、银行等客服，以退款、账户异常等为由，诱导用户提供验证码或点击钓鱼链接。',
        riskLevel: '高',
        preventionTips: [
          '官方客服不会主动索要验证码',
          '退款通常原路返回，无需额外操作',
          '通过官方渠道核实客服身份',
          '警惕要求转账或提供密码的客服',
        ],
        commonTactics: [
          '您的订单出现异常，需要退款，请点击链接操作',
          '您的账户存在风险，需要验证身份，请提供验证码',
        ],
        cases: [
          '张女士接到自称某电商平台客服的电话，称其购买的商品有质量问题需要退款，要求张女士点击链接填写银行卡信息。张女士按照要求操作后，银行卡被盗刷5000元。',
        ],
        content: '冒充客服诈骗是当前最常见的诈骗手段之一。\n\n识别要点：\n1. 官方客服不会主动索要验证码、密码等敏感信息\n2. 退款通常原路返回，无需额外操作',
        relatedTraining: '短信识别训练、电话识别训练',
      };
    })
    .finally(() => {
      loading.value = false;
      // 详情加载完成后，如果未标记为已学，开始计时
      if (detail.value && !knowledgeStore.isRead(detail.value.id)) {
        startViewTracking();
      }
    });
}

// 开始浏览时间跟踪
function startViewTracking() {
  // 如果已经标记为已学，不启动计时
  if (detail.value && knowledgeStore.isRead(detail.value.id)) {
    return;
  }

  stopViewTracking(); // 先清除之前的计时器
  viewStartTime = Date.now();
  isMarkedAsRead = false;

  // 设置30秒后自动标记
  viewTimer = setTimeout(() => {
    if (detail.value && !isMarkedAsRead && !knowledgeStore.isRead(detail.value.id)) {
      autoMarkAsRead();
    }
  }, VIEW_DURATION);
}

// 停止浏览时间跟踪
function stopViewTracking() {
  if (viewTimer) {
    clearTimeout(viewTimer);
    viewTimer = null;
  }
  viewStartTime = null;
}

// 自动标记为已学
async function autoMarkAsRead() {
  if (!detail.value || isMarkedAsRead) {
    return;
  }

  const id = detail.value.id;
  
  // 检查是否已经学习过（防止重复添加经验）
  const wasAlreadyRead = knowledgeStore.isRead(id);
  
  isMarkedAsRead = true;
  knowledgeStore.markRead(id);

  // 如果是首次学习，添加经验值
  if (!wasAlreadyRead) {
    const expResult = achievementStore.addExp(50);
    if (expResult.leveledUp) {
      ElMessage.success({
        message: `恭喜升级！获得50经验值，当前等级：${expResult.newLevel}级`,
        duration: 3000,
      });
    } else {
      ElMessage.success(`已自动标记为已学习，获得50经验值`);
    }
    // 检查成就
    achievementStore.checkAchievements();
  }

  // 同步到后端
  if (userStore.userId) {
    try {
      await http.post(`/knowledge/${id}/learn`, null, {
        params: { userId: userStore.userId, progress: 100 },
      });
      if (wasAlreadyRead) {
        ElMessage.success('已自动标记为已学习');
      }
    } catch {
      // 静默失败，不影响用户体验
      console.warn('同步学习记录失败');
    }
  }

  stopViewTracking();
}

function formatContent(content: string): string {
  if (!content) return '';
  const numberPattern = /^\d+\./;
  return content.split('\n').map((line) => {
    if (numberPattern.test(line.trim())) {
      return `<p style="margin: 8px 0; padding-left: 20px;">${line}</p>`;
    }
    return `<p style="margin: 12px 0;">${line}</p>`;
  }).join('');
}

function getRiskTagType(level?: string): string {
  const types: Record<string, string> = {
    '高': 'danger',
    '中': 'warning',
    '低': 'success',
  };
  return types[level || ''] || 'info';
}

function getCategoryTagType(category: string): string {
  const types: Record<string, string> = {
    '冒充客服': 'danger',
    '投资理财': 'warning',
    '冒充熟人': 'warning',
    '网络钓鱼': 'danger',
    '情感诈骗': 'danger',
    '冒充公检法': 'danger',
  };
  return types[category] || 'info';
}

function goBack() {
  router.push('/knowledge');
}

function goToTraining() {
  router.push('/train');
}

async function toggleLearn(id: number) {
  const willMark = !knowledgeStore.isRead(id);
  const wasAlreadyRead = knowledgeStore.isRead(id);
  
  knowledgeStore.toggleRead(id);

  // 如果标记为已学，停止计时器
  if (willMark) {
    stopViewTracking();
    isMarkedAsRead = true;
    
    // 如果是首次学习，添加经验值
    if (!wasAlreadyRead) {
      const expResult = achievementStore.addExp(50);
      if (expResult.leveledUp) {
        ElMessage.success({
          message: `恭喜升级！获得50经验值，当前等级：${expResult.newLevel}级`,
          duration: 3000,
        });
      } else {
        ElMessage.success(`已标记为已学习，获得50经验值`);
      }
      // 检查成就
      achievementStore.checkAchievements();
    }
  } else {
    // 如果取消标记，重新开始计时
    startViewTracking();
  }

  if (willMark && userStore.userId) {
    try {
      await http.post(`/knowledge/${id}/learn`, null, {
        params: { userId: userStore.userId, progress: 100 },
      });
      if (wasAlreadyRead) {
        ElMessage.success('已同步学习记录');
      }
    } catch {
      ElMessage.warning('同步学习记录失败（不影响本地标记）');
    }
  }
}
</script>

<style scoped lang="scss">
.knowledge-detail-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;

  .page-header {
    margin-bottom: 20px;
    background: white;
    padding: 16px 20px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

    .header-title {
      font-size: 18px;
      font-weight: 600;
    }
  }

  .loading-container {
    background: white;
    padding: 30px;
    border-radius: 8px;
  }

  .detail-content {
    .detail-header {
      margin-bottom: 20px;

      .header-info {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 16px;
        flex-wrap: wrap;
        gap: 16px;

        .title-section {
          flex: 1;
          display: flex;
          align-items: center;
          gap: 12px;
          flex-wrap: wrap;

          .risk-tag {
            font-size: 16px;
            padding: 8px 16px;
          }

          .detail-title {
            font-size: 28px;
            font-weight: 700;
            color: #303133;
            margin: 0;
            flex: 1;
            min-width: 200px;
          }
        }
      }

      .detail-summary {
        font-size: 16px;
        color: #606266;
        line-height: 1.8;
        margin: 0;
        padding: 16px;
        background: #f8f9fa;
        border-radius: 8px;
      }
    }

    .detail-section {
      margin-bottom: 20px;

      .section-header {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 18px;
        font-weight: 600;
        color: #303133;

        .el-icon {
          font-size: 20px;
          color: #667eea;
        }
      }

      .content-text {
        font-size: 15px;
        line-height: 1.8;
        color: #606266;

        :deep(p) {
          margin: 12px 0;
        }
      }

      .tactics-list {
        .tactic-item {
          margin-bottom: 12px;

          &:last-child {
            margin-bottom: 0;
          }

          :deep(.el-alert__title) {
            font-size: 14px;
            line-height: 1.6;
          }
        }
      }

      .cases-list {
        .case-item {
          margin-bottom: 24px;
          padding: 16px;
          background: #f8f9fa;
          border-radius: 8px;
          border-left: 4px solid #667eea;

          &:last-child {
            margin-bottom: 0;
          }

          .case-number {
            font-size: 14px;
            font-weight: 600;
            color: #667eea;
            margin-bottom: 8px;
          }

          .case-content {
            font-size: 14px;
            line-height: 1.8;
            color: #606266;
            margin: 0;
          }
        }
      }

      .tips-list {
        .tip-item {
          display: flex;
          align-items: flex-start;
          gap: 12px;
          padding: 12px 0;
          border-bottom: 1px solid #f0f0f0;

          &:last-child {
            border-bottom: none;
          }

          .tip-icon {
            color: #67c23a;
            font-size: 18px;
            margin-top: 2px;
            flex-shrink: 0;
          }

          .tip-text {
            font-size: 15px;
            line-height: 1.8;
            color: #606266;
            flex: 1;
          }
        }
      }

      .training-recommend {
        text-align: center;
        padding: 20px;

        .recommend-text {
          font-size: 15px;
          color: #606266;
          margin-bottom: 16px;
        }
      }
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .knowledge-detail-page {
    padding: 12px;

    .detail-content {
      .detail-header {
        .header-info {
          .title-section {
            .detail-title {
              font-size: 22px;
            }
          }
        }
      }
    }
  }
}
</style>
