<script setup lang="ts">
import { computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAchievementStore, type AchievementStatus } from '../store/achievement';
import { useUserStore } from '../stores/user';
import { useKnowledgeStore } from '../store/knowledge';
import { ArrowRight, Close, Star } from '@element-plus/icons-vue';

const props = defineProps<{
  modelValue: boolean;
  achievementId: string | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  close: [];
}>();

const router = useRouter();
const achievementStore = useAchievementStore();
const userStore = useUserStore();
const knowledgeStore = useKnowledgeStore();

// 当前成就
const achievement = computed(() => {
  if (!props.achievementId) return null;
  return achievementStore.achievements.find((a) => a.id === props.achievementId);
});

// 当前进度值
const currentProgress = computed(() => {
  if (!achievement.value) return 0;

  const condition = achievement.value.condition;
  let current = 0;

  switch (condition.type) {
    case 'training':
      current = userStore.trainingCount;
      break;
    case 'learning':
      current = knowledgeStore.readCount;
      break;
    case 'assessment':
      if (condition.key === 'low_risk') {
        current = userStore.riskLevel === 'low' ? 1 : 0;
      } else {
        current = userStore.riskHistory.length;
      }
      break;
    case 'special':
      if (condition.key === 'level') {
        current = achievementStore.level;
      } else if (condition.key === 'all_categories') {
        const hasTraining = achievementStore.achievements
          .filter((a) => a.category === 'training')
          .some((a) => achievementStore.unlockedAchievements.has(a.id));
        const hasLearning = achievementStore.achievements
          .filter((a) => a.category === 'learning')
          .some((a) => achievementStore.unlockedAchievements.has(a.id));
        const hasAssessment = achievementStore.achievements
          .filter((a) => a.category === 'assessment')
          .some((a) => achievementStore.unlockedAchievements.has(a.id));
        current = hasTraining && hasLearning && hasAssessment ? 1 : 0;
      }
      break;
  }

  return current;
});

// 目标值
const targetValue = computed(() => {
  return achievement.value?.condition.target || 0;
});

// 进度百分比
const progressPercent = computed(() => {
  if (!achievement.value) return 0;
  return achievement.value.progress;
});

// 关闭弹窗
const close = () => {
  emit('update:modelValue', false);
  emit('close');
};

// 跳转到相关页面
const goToComplete = () => {
  if (!achievement.value) return;

  const routeMap: Record<string, string> = {
    training: '/train',
    learning: '/knowledge',
    assessment: '/assessment',
    special: '/',
  };

  const route = routeMap[achievement.value.category] || '/';
  router.push(route);
  close();
};

// 监听弹窗打开，刷新数据
watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) {
      achievementStore.refresh();
    }
  }
);

// 弹窗显示状态
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
});

// 获取条件文本
const getConditionText = (achievement: any) => {
  const condition = achievement.condition;
  const texts: Record<string, string> = {
    training: `完成 ${condition.target} 次识别训练`,
    learning: `学习 ${condition.target} 条防骗知识`,
    assessment: condition.key === 'low_risk' 
      ? '获得低风险评级'
      : `完成 ${condition.target} 次风险测评`,
    special: condition.key === 'level'
      ? `达到 ${condition.target} 级`
      : '完成所有类型的成就',
  };
  return texts[condition.type] || '未知条件';
};

// 获取进度条颜色
const getProgressColor = (status: AchievementStatus) => {
  const colors: Record<AchievementStatus, string> = {
    locked: '#c0c4cc',
    progress: '#409eff',
    unlocked: '#67c23a',
  };
  return colors[status] || '#409eff';
};
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="achievement?.name || '成就详情'"
    width="500px"
    :close-on-click-modal="false"
    class="achievement-dialog"
  >
    <template #header>
      <div class="dialog-header">
        <div class="header-left">
          <span class="header-icon">{{ achievement?.icon }}</span>
          <span class="header-title">{{ achievement?.name }}</span>
        </div>
        <el-button text @click="close">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </template>

    <div v-if="achievement" class="dialog-content">
      <!-- 成就描述 -->
      <div class="content-section">
        <h4 class="section-title">成就描述</h4>
        <p class="section-text">{{ achievement.description }}</p>
      </div>

      <!-- 解锁条件 -->
      <div class="content-section">
        <h4 class="section-title">解锁条件</h4>
        <div class="condition-box">
          <div class="condition-item">
            <span class="condition-label">目标：</span>
            <span class="condition-value">{{ getConditionText(achievement) }}</span>
          </div>
        </div>
      </div>

      <!-- 当前进度 -->
      <div class="content-section">
        <h4 class="section-title">当前进度</h4>
        <div class="progress-box">
          <div class="progress-info">
            <span class="progress-current">{{ currentProgress }}</span>
            <span class="progress-separator">/</span>
            <span class="progress-target">{{ targetValue }}</span>
          </div>
          <el-progress
            :percentage="progressPercent"
            :stroke-width="12"
            :color="achievement ? getProgressColor(achievement.status) : '#409eff'"
          />
        </div>
      </div>

      <!-- 奖励信息 -->
      <div class="content-section">
        <h4 class="section-title">奖励</h4>
        <div class="reward-box">
          <el-tag type="warning" size="large">
            <el-icon><Star /></el-icon>
            经验值 +{{ achievement.expReward }}
          </el-tag>
        </div>
      </div>

      <!-- 解锁时间 -->
      <div v-if="achievement.status === 'unlocked' && achievement.unlockedAt" class="content-section">
        <h4 class="section-title">解锁时间</h4>
        <p class="section-text">
          {{ new Date(achievement.unlockedAt).toLocaleString('zh-CN') }}
        </p>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="close">关闭</el-button>
        <el-button
          v-if="achievement?.status !== 'unlocked'"
          type="primary"
          @click="goToComplete"
        >
          去完成
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.achievement-dialog {
  :deep(.el-dialog__header) {
    padding: 0;
  }

  :deep(.el-dialog__body) {
    padding: 20px;
  }
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #ebeef5;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .header-icon {
      font-size: 32px;
      line-height: 1;
    }

    .header-title {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }
  }
}

.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.content-section {
  .section-title {
    margin: 0 0 12px 0;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .section-text {
    margin: 0;
    font-size: 14px;
    color: #606266;
    line-height: 1.6;
  }
}

.condition-box {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;

  .condition-item {
    display: flex;
    align-items: center;
    gap: 8px;

    .condition-label {
      font-size: 14px;
      color: #909399;
    }

    .condition-value {
      font-size: 14px;
      color: #303133;
      font-weight: 600;
    }
  }
}

.progress-box {
  .progress-info {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-bottom: 12px;
    font-size: 18px;

    .progress-current {
      color: #409eff;
      font-weight: 700;
    }

    .progress-separator {
      color: #909399;
    }

    .progress-target {
      color: #606266;
      font-weight: 600;
    }
  }
}

.reward-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
