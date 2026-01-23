<script setup lang="ts">
import { computed } from 'vue';
import type { Achievement } from '../store/achievement';
import { Lock, Check, Loading, Star } from '@element-plus/icons-vue';

const props = defineProps<{
  achievement: Achievement;
}>();

const emit = defineEmits<{
  click: [];
}>();

// 状态配置
const statusConfig = computed(() => {
  const configs = {
    locked: {
      icon: Lock,
      color: '#c0c4cc',
      bgColor: '#f5f7fa',
      borderColor: '#e4e7ed',
      text: '未解锁',
    },
    progress: {
      icon: Loading,
      color: '#409eff',
      bgColor: '#ecf5ff',
      borderColor: '#b3d8ff',
      text: '进行中',
    },
    unlocked: {
      icon: Check,
      color: '#67c23a',
      bgColor: '#f0f9ff',
      borderColor: '#b3e19d',
      text: '已解锁',
    },
  };
  return configs[props.achievement.status];
});

const handleClick = () => {
  emit('click');
};
</script>

<template>
  <div
    class="achievement-card"
    :class="[`status-${achievement.status}`]"
    @click="handleClick"
  >
    <div class="card-icon" :style="{ backgroundColor: statusConfig.bgColor }">
      <span class="icon-emoji">{{ achievement.icon }}</span>
      <div class="status-badge" :style="{ backgroundColor: statusConfig.color }">
        <el-icon><component :is="statusConfig.icon" /></el-icon>
      </div>
    </div>
    <div class="card-content">
      <h3 class="card-title">{{ achievement.name }}</h3>
      <p class="card-description">{{ achievement.description }}</p>
      <div class="card-progress">
        <el-progress
          :percentage="achievement.progress"
          :stroke-width="6"
          :show-text="false"
          :color="statusConfig.color"
        />
        <span class="progress-text">{{ Math.round(achievement.progress) }}%</span>
      </div>
      <div class="card-reward">
        <el-tag size="small" type="warning">
          <el-icon><Star /></el-icon>
          +{{ achievement.expReward }} EXP
        </el-tag>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.achievement-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid v-bind('statusConfig.borderColor');
  display: flex;
  gap: 16px;
  position: relative;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    border-color: v-bind('statusConfig.color');
  }

  &.status-unlocked {
    background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%);
  }

  &.status-progress {
    background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
  }

  &.status-locked {
    opacity: 0.7;
    filter: grayscale(0.3);
  }

  .card-icon {
    flex-shrink: 0;
    width: 64px;
    height: 64px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;

    .icon-emoji {
      font-size: 36px;
      line-height: 1;
    }

    .status-badge {
      position: absolute;
      top: -4px;
      right: -4px;
      width: 24px;
      height: 24px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 12px;
      border: 2px solid white;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    }
  }

  .card-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .card-title {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }

  .card-description {
    margin: 0;
    font-size: 14px;
    color: #606266;
    line-height: 1.5;
    flex: 1;
  }

  .card-progress {
    display: flex;
    align-items: center;
    gap: 8px;

    :deep(.el-progress) {
      flex: 1;
    }

    .progress-text {
      font-size: 12px;
      color: #909399;
      font-weight: 600;
      min-width: 40px;
      text-align: right;
    }
  }

  .card-reward {
    margin-top: 4px;
  }
}
</style>
