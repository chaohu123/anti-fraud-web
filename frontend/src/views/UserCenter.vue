<template>
  <div class="user-center-page">
    <!-- 顶部用户信息区 -->
    <el-card class="user-header-card" shadow="never">
      <div class="user-header">
        <div class="user-avatar-section">
          <el-avatar 
            :size="100" 
            class="user-avatar"
            :style="{ background: avatarBg }"
          >
            <el-icon :size="50"><UserFilled /></el-icon>
          </el-avatar>
          <el-button 
            v-if="isAuthed" 
            class="avatar-edit-btn" 
            circle 
            size="small"
            @click="handleAvatarEdit"
          >
            <el-icon><Camera /></el-icon>
          </el-button>
        </div>
        <div class="user-info">
          <div class="user-name-row">
            <h2 class="user-name">{{ userStore.name || '访客' }}</h2>
            <el-tag v-if="isAuthed" type="primary" effect="dark" size="large" round>
              Lv.{{ achievementStore.level }}
            </el-tag>
          </div>
          <div class="user-title-row">
            <el-tag type="info" effect="light" size="default">
              {{ achievementStore.levelTitle }}
            </el-tag>
            <el-tag 
              :type="riskTagType" 
              effect="light" 
              size="default"
              style="margin-left: 8px"
            >
              风险等级：{{ riskLabel }}
            </el-tag>
          </div>
          <div class="user-exp-section">
            <div class="exp-info">
              <span class="exp-label">经验值</span>
              <span class="exp-value">
                {{ achievementStore.exp }} / {{ achievementStore.expForNextLevel }}
              </span>
            </div>
            <el-progress 
              :percentage="expProgress" 
              :stroke-width="12"
              :color="expProgressColor"
              :show-text="false"
              class="exp-progress"
            />
          </div>
        </div>
      </div>
    </el-card>

    <!-- 快捷功能区 -->
    <el-card class="quick-actions-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">快捷功能</span>
        </div>
      </template>
      <div class="quick-actions-grid">
        <QuickActionCard
          title="识别训练"
          description="多场景诈骗识别训练，提升防骗能力"
          :icon="Lock"
          icon-bg="linear-gradient(135deg, #667eea 0%, #764ba2 100%)"
          path="/train"
        />
        <QuickActionCard
          title="风险测评"
          description="评估您的防骗风险指数"
          :icon="DataAnalysis"
          icon-bg="linear-gradient(135deg, #f093fb 0%, #f5576c 100%)"
          path="/assessment"
        />
        <QuickActionCard
          title="防骗知识"
          description="学习防骗知识，提升安全意识"
          :icon="Document"
          icon-bg="linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)"
          path="/knowledge"
        />
        <QuickActionCard
          title="成就系统"
          description="查看您的成就与勋章"
          :icon="Star"
          icon-bg="linear-gradient(135deg, #fa709a 0%, #fee140 100%)"
          path="/achievement"
        />
        <QuickActionCard
          title="评估报告"
          description="查看详细的风险评估报告"
          :icon="Files"
          icon-bg="linear-gradient(135deg, #30cfd0 0%, #330867 100%)"
          path="/report"
        />
      </div>
    </el-card>

    <!-- 成长数据概览区 -->
    <div class="stats-section">
      <el-card class="stats-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="card-title">成长数据</span>
          </div>
        </template>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon :size="28"><Lock /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ userStore.trainingCount }}</div>
              <div class="stat-label">训练次数</div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon :size="28"><DataAnalysis /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ assessmentCount }}</div>
              <div class="stat-label">测评次数</div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
              <el-icon :size="28"><Star /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ unlockedAchievementsCount }}</div>
              <div class="stat-label">成就数量</div>
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
              <el-icon :size="28"><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ knowledgeStore.readCount }}</div>
              <div class="stat-label">已学知识</div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 我的操作区 -->
    <el-card class="actions-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">我的操作</span>
        </div>
      </template>
      <div v-if="!isAuthed" class="guest-tip-section">
        <el-alert
          title="您当前以游客身份访问"
          description="登录后可以保存训练记录、查看详细报告、解锁成就等功能"
          type="info"
          :closable="false"
          show-icon
        />
        <div class="guest-actions">
          <el-button type="primary" @click="goLogin">立即登录</el-button>
          <el-button @click="goRegister">注册账号</el-button>
        </div>
      </div>
      <div v-else class="user-actions">
        <el-button class="action-btn" @click="handleAccountSettings">
          <el-icon><Setting /></el-icon>
          <span>账号设置</span>
        </el-button>
        <el-button class="action-btn" @click="handleChangePassword">
          <el-icon><Lock /></el-icon>
          <span>修改密码</span>
        </el-button>
        <el-button class="action-btn" type="danger" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出登录</span>
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  UserFilled,
  Camera,
  Lock,
  DataAnalysis,
  Document,
  Star,
  Files,
  Setting,
  SwitchButton,
} from '@element-plus/icons-vue';
import { useUserStore } from '../stores/user';
import { useAchievementStore } from '../store/achievement';
import { useKnowledgeStore } from '../store/knowledge';
import QuickActionCard from '../components/QuickActionCard.vue';

const router = useRouter();
const userStore = useUserStore();
const achievementStore = useAchievementStore();
const knowledgeStore = useKnowledgeStore();

const isAuthed = computed(() => !!userStore.userId);

// 风险等级标签
const riskLabel = computed(() => {
  const map: Record<string, string> = {
    low: '低',
    medium: '中',
    high: '高',
  };
  return map[userStore.riskLevel] || '低';
});

const riskTagType = computed(() => {
  const map: Record<string, 'success' | 'warning' | 'danger'> = {
    low: 'success',
    medium: 'warning',
    high: 'danger',
  };
  return map[userStore.riskLevel] || 'success';
});

// 经验值进度
const expProgress = computed(() => {
  return Math.round(achievementStore.expProgress);
});

const expProgressColor = computed(() => {
  if (expProgress.value < 30) return '#f5576c';
  if (expProgress.value < 70) return '#f093fb';
  return '#667eea';
});

// 头像背景色（根据等级变化）
const avatarBg = computed(() => {
  const level = achievementStore.level;
  if (level >= 5) return 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)';
  if (level >= 3) return 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
  return 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)';
});

// 测评次数
const assessmentCount = computed(() => {
  return userStore.riskHistory.length;
});

// 已解锁成就数量
const unlockedAchievementsCount = computed(() => {
  return achievementStore.unlockedAchievements.size;
});

// 页面加载时刷新成就
onMounted(() => {
  if (isAuthed.value) {
    achievementStore.refresh();
    userStore.fetchUserInfo().catch(() => {
      // 忽略错误
    });
  }
});

// 处理头像编辑
const handleAvatarEdit = () => {
  ElMessage.info('头像更换功能开发中');
};

// 处理账号设置
const handleAccountSettings = () => {
  ElMessage.info('账号设置功能开发中');
};

// 处理修改密码
const handleChangePassword = () => {
  ElMessage.info('修改密码功能开发中');
};

// 处理退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    userStore.logout();
    ElMessage.success('已退出登录');
    router.push('/');
  } catch {
    // 用户取消
  }
};

// 跳转登录
const goLogin = () => {
  router.push('/login');
};

// 跳转注册
const goRegister = () => {
  router.push('/login?tab=register');
};
</script>

<style scoped lang="scss">
.user-center-page {
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

// 顶部用户信息区
.user-header-card {
  border-radius: 16px;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  overflow: hidden;

  :deep(.el-card__body) {
    padding: 32px;
  }
}

.user-header {
  display: flex;
  align-items: flex-start;
  gap: 32px;
}

.user-avatar-section {
  position: relative;
  flex-shrink: 0;
}

.user-avatar {
  border: 4px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.avatar-edit-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  background: #fff;
  border: 2px solid #667eea;
  color: #667eea;

  &:hover {
    background: #f5f7fa;
  }
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.user-name {
  margin: 0;
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.user-title-row {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.user-exp-section {
  margin-top: 20px;
}

.exp-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.exp-label {
  opacity: 0.9;
  font-weight: 500;
}

.exp-value {
  opacity: 0.9;
  font-weight: 600;
}

.exp-progress {
  :deep(.el-progress-bar__outer) {
    background-color: rgba(255, 255, 255, 0.2);
    border-radius: 6px;
  }

  :deep(.el-progress-bar__inner) {
    border-radius: 6px;
  }
}

// 卡片通用样式
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

// 快捷功能区
.quick-actions-card {
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

// 成长数据概览区
.stats-section {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

.stats-card {
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--el-bg-color-page);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

// 我的操作区
.actions-card {
  border-radius: 16px;
  border: 1px solid var(--el-border-color-lighter);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.guest-tip-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.guest-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.user-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn {
  width: 100%;
  justify-content: flex-start;
  padding: 16px 20px;
  font-size: 15px;
  border-radius: 10px;
  transition: all 0.3s ease;

  &:hover {
    transform: translateX(4px);
  }

  .el-icon {
    margin-right: 8px;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .user-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .user-name {
    font-size: 24px;
  }

  .user-title-row {
    justify-content: center;
  }
}
</style>
