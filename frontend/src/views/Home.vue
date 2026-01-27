<template>
  <div class="home-page">
    <!-- Hero -->
    <el-card class="hero" shadow="never">
      <div class="hero-container">
        <div class="hero-left">
          <p class="hero-eyebrow">反诈训练 + 风险评估 · 毕业设计展示</p>
          <h1 class="hero-title">反诈训练营 · Anti-Fraud Lab</h1>
          <p class="hero-sub">模拟真实诈骗场景，通过互动训练与风险评估，提升防骗能力。</p>
          <div class="hero-actions">
            <el-button type="primary" size="large" @click="$router.push('/train')">开始识别训练</el-button>
            <el-button size="large" plain @click="$router.push('/assessment')">立即风险自测</el-button>
          </div>
          <div class="hero-badges">
            <el-tag type="success" effect="dark" round>专业可信</el-tag>
            <el-tag type="info" effect="dark" round>毕业设计演示</el-tag>
            <el-tag type="warning" effect="dark" round>互动训练</el-tag>
          </div>
        </div>
        <div class="hero-right">
          <div class="hero-metrics">
            <div class="metric">
              <div class="metric-label">已训练次数</div>
              <div class="metric-value">{{ user.trainingCount }}</div>
            </div>
            <div class="metric">
              <div class="metric-label">风险等级</div>
              <el-tag :type="riskTagType" effect="light" size="large" round>{{ riskLabel }}</el-tag>
            </div>
            <div class="metric metric-click" @click="goLearnedKnowledge">
              <div class="metric-label">已学习知识</div>
              <div class="metric-value">{{ knowledge.readCount }}</div>
              <div class="metric-hint">点击查看已学习</div>
            </div>
          </div>
          <div class="hero-illustration">
            <el-icon class="illus-icon"><Lock /></el-icon>
            <div class="illus-text">
              <div class="illus-title">安全防护</div>
              <div class="illus-desc">多模态识别 · 风险评估 · 知识库学习</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 主要内容区域：左右分栏布局 -->
    <div class="main-content">
      <!-- 左侧：数据概览和核心功能 -->
      <div class="main-left">
        <!-- 数据概览 -->
        <div class="section-header">
          <div class="section-title">数据概览</div>
        </div>
        <div class="overview">
          <el-card
            v-for="card in overviewCards"
            :key="card.title"
            shadow="hover"
            class="overview-card"
            :class="{ 'overview-card-click': !!(card as any).to }"
            @click="(card as any).to && $router.push((card as any).to)"
          >
            <div class="overview-icon" :class="card.type">
              <component :is="card.icon" />
            </div>
            <div class="overview-meta">
              <div class="overview-label">{{ card.title }}</div>
              <div class="overview-value">{{ card.value }}</div>
              <div class="overview-sub">{{ card.sub }}</div>
            </div>
          </el-card>
        </div>

        <!-- 核心功能入口 -->
        <div class="section-header">
          <div class="section-title">核心功能</div>
        </div>
        <div class="entry-grid">
          <el-card
            v-for="entry in entries"
            :key="entry.title"
            class="entry-card"
            shadow="hover"
            @click="$router.push(entry.to)"
          >
            <div class="entry-head">
              <component :is="entry.icon" class="entry-icon" />
              <div class="entry-title">{{ entry.title }}</div>
            </div>
            <div class="entry-desc">{{ entry.desc }}</div>
            <div class="entry-footer">
              <span>查看详情</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 右侧：成长与成就 -->
      <div class="main-right">
        <div class="section-header">
          <div class="section-title">成长与成就</div>
        </div>
        <el-card shadow="hover" class="growth">
          <div class="growth-content">
            <div class="growth-item">
              <div class="growth-label">当前等级</div>
              <el-tag type="primary" size="large" round>{{ riskLabel }}</el-tag>
            </div>
            <div class="growth-item">
              <div class="growth-label">已获勋章</div>
              <div class="badges">
                <el-tag v-for="badge in user.badges" :key="badge" type="success" effect="light" round>
                  {{ badge }}
                </el-tag>
                <span v-if="!user.badges.length" class="text-muted">暂无勋章</span>
              </div>
            </div>
          </div>
        </el-card>
        <!-- 推荐学习内容（与左侧核心功能视觉对齐） -->
        <div class="section-header recommend-header">
          <div class="section-title">推荐学习内容</div>
        </div>
        <el-card shadow="hover" class="recommend">
          <div class="recommend-list">
            <div
              v-for="item in recommendedLearning"
              :key="item.title"
              class="recommend-item"
            >
              <div class="recommend-main">
                <div class="recommend-title">{{ item.title }}</div>
                <div class="recommend-desc">{{ item.desc }}</div>
              </div>
              <el-button
                type="primary"
                text
                size="small"
                @click="$router.push(item.to)"
              >
                {{ item.actionText }}
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowRight, Lock, DataAnalysis, Files, MagicStick } from '@element-plus/icons-vue';
import { useUserStore } from '../stores/user';
import { useKnowledgeStore } from '../store/knowledge';

const router = useRouter();
const user = useUserStore();
const knowledge = useKnowledgeStore();

const riskLabel = computed(() => {
  const raw: any = (user as any).riskLevel;
  if (raw == null || raw === '') return '低';
  // 兼容历史数据/后端枚举：low/medium/high、LOW/MEDIUM/HIGH、0/1/2、1/2/3
  const normalized = String(raw).toLowerCase();
  const map: Record<string, string> = {
    low: '低',
    medium: '中',
    high: '高',
    '0': '低',
    '1': '中',
    '2': '高',
    '3': '高',
  };
  return map[normalized] ?? '低';
});

const riskTagType = computed(() => {
  const map: Record<string, 'success' | 'warning' | 'danger'> = {
    low: 'success',
    medium: 'warning',
    high: 'danger',
  };
  return map[user.riskLevel] || 'success';
});

const overviewCards = computed(() => [
  {
    title: '已训练次数',
    value: user.trainingCount,
    sub: '完成的识别练习',
    icon: DataAnalysis,
    type: 'primary',
  },
  {
    title: '风险等级',
    value: riskLabel.value,
    sub: '根据测评结果动态更新',
    icon: Lock,
    type: 'warning',
  },
  {
    title: '已学习知识',
    value: knowledge.readCount,
    sub: '累计标记已学的知识点',
    icon: Files,
    type: 'info',
    to: '/knowledge/learned',
  },
]);

function goLearnedKnowledge() {
  router.push('/knowledge/learned');
}

const entries = [
  {
    title: '识别训练',
    desc: '短信 / 邮件 / 网站多场景互动识别，强化防骗直觉。',
    icon: Lock,
    to: '/train',
  },
  {
    title: '风险测评',
    desc: '多维度风险指数评估，生成个性化改进建议。',
    icon: DataAnalysis,
    to: '/assessment',
  },
  {
    title: '防诈知识库',
    desc: '高频诈骗套路与防范要点，系统化学习与标记。',
    icon: MagicStick,
    to: '/knowledge',
  },
];

const recommendedLearning = computed(() => {
  const risk = riskLabel.value;
  const items: { title: string; desc: string; to: string; actionText: string }[] = [];

  if (risk === '高') {
    items.push(
      {
        title: '优先完成一次风险测评',
        desc: '当前风险较高，建议先进行完整测评，获取个性化防骗建议。',
        to: '/assessment',
        actionText: '去测评',
      },
      {
        title: '系统学习常见高危诈骗套路',
        desc: '从“资金转账类”“冒充公检法”等专题开始，巩固基础防骗意识。',
        to: '/knowledge',
        actionText: '去学习',
      },
    );
  } else if (risk === '中') {
    items.push(
      {
        title: '针对性补齐薄弱场景',
        desc: '从最近做错较多的训练题型入手，查漏补缺，降低真实受骗概率。',
        to: '/train',
        actionText: '继续训练',
      },
      {
        title: '结合知识库做巩固复习',
        desc: '选取近30天更新的知识内容，保持对新型诈骗的敏感度。',
        to: '/knowledge',
        actionText: '去复习',
      },
    );
  } else {
    items.push(
      {
        title: '保持训练频率，巩固成果',
        desc: '每周完成 2–3 组识别训练，持续强化识别直觉。',
        to: '/train',
        actionText: '开始训练',
      },
      {
        title: '挑战进阶场景与专题',
        desc: '尝试更复杂、多步骤的诈骗剧本，进一步提高综合防护能力。',
        to: '/knowledge',
        actionText: '查看专题',
      },
    );
  }

  if (knowledge.readCount < 5) {
    items.push({
      title: '从基础知识开始入门',
      desc: '先完成 5 篇基础防骗知识学习，打好认知地基。',
      to: '/knowledge',
      actionText: '从基础开始',
    });
  }

  return items;
});
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 0 4px;
  animation: fadeIn 0.6s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Hero 区域美化 */
.hero {
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 25%, #f093fb 50%, #4facfe 100%);
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
  color: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(102, 126, 234, 0.3);
  position: relative;
  overflow: hidden;
  padding: 0;
}

.hero-container {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 48px;
  padding: 48px 48px;
  position: relative;
  z-index: 1;
}

.hero::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  animation: rotate 20s linear infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.hero-eyebrow {
  margin: 0;
  opacity: 0.9;
  letter-spacing: 0.8px;
  font-size: 13px;
  text-transform: uppercase;
  font-weight: 600;
}

.hero-title {
  margin: 0;
  font-size: 36px;
  font-weight: 900;
  line-height: 1.2;
  letter-spacing: -0.5px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.hero-sub {
  margin: 0;
  opacity: 0.95;
  font-size: 16px;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.hero-actions :deep(.el-button) {
  padding: 12px 24px;
  font-weight: 600;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.hero-actions :deep(.el-button--primary) {
  background: rgba(255, 255, 255, 0.25);
  border: 2px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
}

.hero-actions :deep(.el-button--primary:hover) {
  background: rgba(255, 255, 255, 0.35);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.hero-actions :deep(.el-button.is-plain) {
  background: rgba(255, 255, 255, 0.15);
  border: 2px solid rgba(255, 255, 255, 0.3);
  color: #fff;
}

.hero-actions :deep(.el-button.is-plain:hover) {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

.hero-badges {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.hero-badges :deep(.el-tag) {
  padding: 6px 14px;
  font-weight: 500;
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.hero-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
  justify-content: space-between;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  width: 100%;
}

.hero-illustration {
  width: 100%;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 18px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  margin-top: auto;
}

.hero-illustration:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.illus-icon {
  font-size: 42px;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.1));
  flex-shrink: 0;
}

.illus-text {
  flex: 1;
}

.illus-title {
  font-weight: 700;
  font-size: 18px;
  margin-bottom: 6px;
}

.illus-desc {
  opacity: 0.9;
  font-size: 14px;
  line-height: 1.5;
}

.metric {
  background: rgba(255, 255, 255, 0.18);
  border-radius: 14px;
  padding: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.metric-click {
  cursor: pointer;
}

.metric-hint {
  margin-top: 6px;
  font-size: 12px;
  opacity: 0.85;
}

.metric:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

.metric-label {
  opacity: 0.9;
  font-size: 12px;
  margin-bottom: 6px;
  font-weight: 500;
}

.metric-value {
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
}

/* 主要内容区域：左右分栏 */
.main-content {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  align-items: start;
}

.main-left {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.main-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 章节标题 */
.section-header {
  margin-bottom: 16px;
}

.section-title {
  font-weight: 800;
  font-size: 22px;
  color: var(--af-text);
  position: relative;
  padding-left: 24px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 22px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 2px;
}

/* 数据概览卡片 */
.overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.overview-card {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.overview-card-click {
  cursor: pointer;
}

.overview-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
}

.overview-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 24px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.overview-icon.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.overview-icon.warning {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.overview-icon.info {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.overview-meta {
  flex: 1;
  min-width: 0;
}

.overview-label {
  font-weight: 600;
  font-size: 14px;
  color: var(--af-muted);
  margin-bottom: 6px;
}

.overview-value {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.2;
  color: var(--af-text);
  margin-bottom: 4px;
}

.overview-sub {
  color: var(--af-muted);
  font-size: 12px;
}

/* 核心功能入口 */
.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.entry-card {
  cursor: pointer;
  padding: 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
}

.entry-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.entry-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(102, 126, 234, 0.2);
}

.entry-card:hover::before {
  transform: scaleX(1);
}

.entry-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.entry-icon {
  font-size: 28px;
  color: var(--af-primary);
  transition: transform 0.3s ease;
}

.entry-card:hover .entry-icon {
  transform: scale(1.1) rotate(5deg);
}

.entry-title {
  font-weight: 700;
  font-size: 18px;
  color: var(--af-text);
}

.entry-desc {
  margin: 12px 0 16px;
  color: var(--af-muted);
  line-height: 1.6;
  font-size: 14px;
}

.entry-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--af-primary);
  font-weight: 600;
  font-size: 14px;
  transition: gap 0.3s ease;
}

.entry-card:hover .entry-footer {
  gap: 12px;
}

/* 成长与成就 */
.growth {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  padding: 0;
}

.growth :deep(.el-card__body) {
  padding: 24px;
}

.growth:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
}

.growth-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.growth-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.growth-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.growth-label {
  font-weight: 600;
  font-size: 14px;
  color: var(--af-muted);
}

.growth-next {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.growth-next :deep(.el-button) {
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.growth-next :deep(.el-button:hover) {
  transform: translateY(-2px);
}

.badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.badges :deep(.el-tag) {
  padding: 6px 12px;
  border-radius: 12px;
  font-weight: 500;
}

.text-muted {
  color: var(--af-muted);
  font-size: 14px;
  font-style: italic;
}

/* 推荐学习内容 */
.recommend-header {
  margin-top: 40px;
}

.recommend {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  padding: 0;
}

.recommend :deep(.el-card__body) {
  padding: 20px 22px;
}

.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.recommend-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.recommend-main {
  flex: 1;
  min-width: 0;
}

.recommend-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--af-text);
  margin-bottom: 4px;
}

.recommend-desc {
  font-size: 13px;
  color: var(--af-muted);
  line-height: 1.6;
}

.recommend :deep(.el-button) {
  white-space: nowrap;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .main-right {
    position: static;
  }

  .overview {
    grid-template-columns: repeat(3, 1fr);
  }

  .entry-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 960px) {
  .hero-container {
    grid-template-columns: 1fr;
    gap: 32px;
    padding: 36px 28px;
  }

  .hero-title {
    font-size: 28px;
  }

  .hero-metrics {
    grid-template-columns: repeat(3, 1fr);
  }

  .overview {
    grid-template-columns: 1fr;
  }

  .entry-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .home-page {
    gap: 20px;
  }

  .hero {
    padding: 24px 20px;
  }

  .hero-title {
    font-size: 24px;
  }

  .hero-actions {
    flex-direction: column;
  }

  .hero-actions :deep(.el-button) {
    width: 100%;
  }

  .section-title {
    font-size: 20px;
  }
}
</style>
