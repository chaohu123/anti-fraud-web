<template>
  <!-- 全局错误遮罩 -->
  <div v-if="errorStore.hasGlobalError && globalError" class="global-error-overlay">
    <div class="error-modal">
      <div class="error-content">
        <div class="error-icon">
          <el-icon :size="48" color="#F56C6C">
            <Warning />
          </el-icon>
        </div>
        <h3 class="error-title">{{ globalError.title }}</h3>
        <p class="error-message">{{ globalError.message }}</p>
        <div class="error-actions">
          <el-button @click="dismissGlobalError">关闭</el-button>
          <el-button
            v-if="globalError.retryable"
            type="primary"
            @click="retryGlobalError"
            :loading="retrying"
          >
            重试
          </el-button>
        </div>
      </div>
    </div>
  </div>

  <!-- 错误通知列表 -->
  <div v-if="activeErrors.length > 0" class="error-notifications">
    <transition-group name="slide-down">
      <div
        v-for="error in activeErrors"
        :key="error.id"
        class="error-notification"
        :class="`error-${error.type}`"
      >
        <div class="error-header">
          <div class="error-icon-small">
            <el-icon :size="16">
              <component :is="getErrorIcon(error.type)" />
            </el-icon>
          </div>
          <span class="error-title-small">{{ error.title }}</span>
          <el-button
            text
            size="small"
            @click="removeError(error.id)"
            class="error-close"
          >
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="error-body">
          <p class="error-message-small">{{ error.message }}</p>
          <div v-if="error.retryable" class="error-actions-small">
            <el-button
              text
              size="small"
              @click="retryError(error.id)"
              :loading="retrying === error.id"
            >
              重试
            </el-button>
          </div>
        </div>
        <div class="error-time">
          {{ formatTime(error.timestamp) }}
        </div>
      </div>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  Warning,
  Close,
  Connection,
  User,
  Check,
  InfoFilled,
} from '@element-plus/icons-vue';
import { useErrorStore, type ErrorType } from '../stores/error';

const errorStore = useErrorStore();
const retrying = ref<string | null>(null);

const globalError = computed(() => errorStore.globalError);
const activeErrors = computed(() => errorStore.activeErrors);

const getErrorIcon = (type: ErrorType) => {
  const iconMap = {
    network: Connection,
    auth: User,
    validation: Warning,
    server: Warning,
    client: InfoFilled,
    unknown: Warning,
  };
  return iconMap[type] || Warning;
};

const dismissGlobalError = () => {
  if (globalError.value) {
    errorStore.removeError(globalError.value.id);
  }
};

const retryGlobalError = async () => {
  if (!globalError.value) return;

  retrying.value = globalError.value.id;
  try {
    await errorStore.retryError(globalError.value.id);
  } finally {
    retrying.value = null;
  }
};

const removeError = (id: string) => {
  errorStore.removeError(id);
};

const retryError = async (id: string) => {
  retrying.value = id;
  try {
    await errorStore.retryError(id);
  } finally {
    retrying.value = null;
  }
};

const formatTime = (timestamp: number) => {
  const now = Date.now();
  const diff = now - timestamp;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  return new Date(timestamp).toLocaleDateString();
};
</script>

<style scoped lang="scss">
.global-error-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.error-modal {
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  max-width: 400px;
  width: 90%;
  margin: 20px;
}

.error-content {
  padding: 32px;
  text-align: center;
}

.error-icon {
  margin-bottom: 20px;
}

.error-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 12px 0;
}

.error-message {
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
  margin: 0 0 24px 0;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.error-notifications {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9998;
  max-width: 400px;
  width: 100%;
}

.error-notification {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 8px;
  border-left: 4px solid;
  overflow: hidden;
  animation: slideInRight 0.3s ease-out;

  &.error-network {
    border-left-color: #E6A23C;
  }

  &.error-auth {
    border-left-color: #F56C6C;
  }

  &.error-validation {
    border-left-color: #E6A23C;
  }

  &.error-server {
    border-left-color: #F56C6C;
  }

  &.error-client {
    border-left-color: #909399;
  }

  &.error-unknown {
    border-left-color: #909399;
  }
}

.error-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--el-bg-color-page);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.error-icon-small {
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
}

.error-title-small {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.error-close {
  flex-shrink: 0;
  color: var(--el-text-color-secondary);

  &:hover {
    color: var(--el-text-color-primary);
  }
}

.error-body {
  padding: 12px 16px;
}

.error-message-small {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
  margin: 0 0 8px 0;
}

.error-actions-small {
  text-align: right;
}

.error-time {
  padding: 8px 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-bg-color-page);
  text-align: right;
}

/* 动画 */
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

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from {
  transform: translateY(-100%);
  opacity: 0;
}

.slide-down-leave-to {
  transform: translateX(100%);
  opacity: 0;
  height: 0;
  margin-bottom: 0;
  padding: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .error-notifications {
    top: 16px;
    right: 16px;
    left: 16px;
    max-width: none;
  }

  .error-modal {
    margin: 16px;
  }

  .error-content {
    padding: 24px;
  }
}
</style>