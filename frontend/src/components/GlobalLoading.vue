<template>
  <div v-if="loadingStore.isLoading" class="global-loading-overlay">
    <!-- 全局加载遮罩 -->
    <transition name="fade">
      <div v-if="loadingStore.hasGlobalLoading" class="global-loading-mask">
        <div class="loading-content">
          <el-icon class="loading-spinner" :size="48">
            <Loading />
          </el-icon>
          <div class="loading-text">加载中...</div>
        </div>
      </div>
    </transition>

    <!-- 局部加载指示器 -->
    <div class="loading-indicators">
      <transition-group name="slide-fade">
        <div
          v-for="loading in loadingStore.activeLoadings"
          :key="loading.id"
          class="loading-indicator"
          :class="`loading-${loading.type}`"
        >
          <div class="indicator-content">
            <template v-if="loading.type === 'spinner'">
              <el-icon class="indicator-spinner" :size="20">
                <Loading />
              </el-icon>
            </template>
            <template v-else-if="loading.type === 'progress'">
              <el-progress
                type="circle"
                :percentage="loading.progress || 0"
                :width="40"
                :stroke-width="4"
                :show-text="false"
              />
            </template>
            <span v-if="loading.message" class="indicator-message">{{ loading.message }}</span>
          </div>
        </div>
      </transition-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Loading } from '@element-plus/icons-vue';
import { useLoadingStore } from '../stores/loading';

const loadingStore = useLoadingStore();

// 计算当前显示的消息
const currentMessage = computed(() => {
  const loadings = loadingStore.activeLoadings;
  if (loadings.length === 0) return '';
  // 优先显示有消息的加载状态
  const withMessage = loadings.find(l => l.message);
  return withMessage?.message || '加载中...';
});
</script>

<style scoped lang="scss">
.global-loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 9999;
}

.global-loading-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: auto;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.loading-spinner {
  color: var(--el-color-primary);
  animation: spin 1s linear infinite;
}

.loading-text {
  font-size: 16px;
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.loading-indicators {
  position: fixed;
  top: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 300px;
}

.loading-indicator {
  background: white;
  border-radius: 8px;
  padding: 12px 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 1px solid var(--el-border-color-lighter);
  pointer-events: auto;
  animation: slideInRight 0.3s ease-out;

  &.loading-spinner {
    min-width: 120px;
  }

  &.loading-progress {
    min-width: 140px;
  }
}

.indicator-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.indicator-spinner {
  color: var(--el-color-primary);
  animation: spin 1s linear infinite;
}

.indicator-message {
  font-size: 14px;
  color: var(--el-text-color-primary);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 动画 */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .loading-indicators {
    top: 16px;
    right: 16px;
    left: 16px;
    max-width: none;
  }

  .loading-indicator {
    width: 100%;
  }

  .global-loading-mask {
    padding: 16px;
  }

  .loading-content {
    padding: 24px;
  }
}
</style>