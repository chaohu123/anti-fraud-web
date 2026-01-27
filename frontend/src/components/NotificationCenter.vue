<template>
  <div class="notification-center">
    <!-- 通知按钮 -->
    <el-button
      class="notification-btn"
      link
      @click.stop="togglePanel"
      :class="{ 'has-unread': unreadCount > 0 }"
    >
      <el-icon :size="20">
        <Bell />
      </el-icon>
      <el-badge
        v-if="unreadCount > 0"
        :value="unreadCount"
        :max="99"
        class="notification-badge"
      />
    </el-button>

    <!-- 通知面板 -->
    <div v-if="showPanel" class="notification-panel" v-click-outside="closePanel">
      <div class="panel-header">
        <h3 class="panel-title">通知中心</h3>
        <div class="panel-actions">
          <el-button
            v-if="unreadCount > 0"
            link
            size="small"
            @click="handleMarkAllAsRead"
          >
            全部已读
          </el-button>
          <el-button link size="small" @click="closePanel">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>

      <div class="panel-body">
        <div v-if="!safeNotifications || safeNotifications.length === 0" class="empty-state">
          <el-icon :size="48" class="empty-icon">
            <Bell />
          </el-icon>
          <p class="empty-text">暂无通知</p>
        </div>

        <div v-else class="notification-list">
          <div
            v-for="notification in safeNotifications"
            :key="notification.id"
            class="notification-item"
            :class="[
              `notification-${notification.type}`,
              { unread: isUnread(notification) }
            ]"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-icon">
              <el-icon :size="16">
                <component :is="getNotificationIcon(notification.type)" />
              </el-icon>
            </div>

            <div class="notification-content">
              <div class="notification-header">
                <span class="notification-title">{{ notification.title }}</span>
                <span class="notification-time">{{ formatTime(notification.timestamp) }}</span>
              </div>
              <p class="notification-message">{{ notification.message }}</p>

              <!-- 特殊类型的额外信息 -->
              <div v-if="notification.type === 'achievement'" class="notification-extra">
                <span class="achievement-desc">{{ notification.description }}</span>
              </div>

              <div v-if="notification.type === 'risk-update'" class="notification-extra">
                <div class="risk-info">
                  <span>风险指数: {{ notification.score?.toFixed(2) }}</span>
                  <el-tag :type="getRiskTagType(notification.newLevel)" size="small">
                    {{ notification.newLevel }}
                  </el-tag>
                </div>
              </div>

              <div v-if="notification.type === 'training'" class="notification-extra">
                <div class="training-info">
                  <el-tag :type="notification.isCorrect ? 'success' : 'danger'" size="small">
                    {{ notification.isCorrect ? '识别正确' : '识别错误' }}
                  </el-tag>
                  <span>{{ notification.scamType }}</span>
                </div>
              </div>
            </div>

            <el-button
              link
              size="small"
              @click.stop="removeNotification(notification.id)"
              class="notification-close"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 连接状态指示器 -->
      <div class="panel-footer">
        <div class="connection-status">
          <el-icon :size="14" :class="connectionStatusClass">
            <component :is="connectionStatusIcon" />
          </el-icon>
          <span class="connection-text">{{ connectionStatusText }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue';
import {
  Bell,
  Close,
  InfoFilled,
  SuccessFilled,
  WarningFilled,
  CircleCheck,
  CircleClose,
  Trophy,
  DataAnalysis,
  Reading,
  Link,
  User,
} from '@element-plus/icons-vue';
// 暂时禁用 WebSocket 导入，避免加载相关库
// import { useWebSocket, type WebSocketMessage } from '../composables/useWebSocket';

// 定义 WebSocketMessage 类型，避免依赖 useWebSocket
export interface WebSocketMessage {
  id: number;
  title: string;
  message: string;
  type: string;
  timestamp: number;
  description?: string;
  score?: number;
  oldLevel?: string;
  newLevel?: string;
  progress?: number;
  isCorrect?: boolean;
  scamType?: string;
}

// 自定义指令：点击外部关闭
const vClickOutside = {
  mounted(el: HTMLElement, binding: any) {
    el.clickOutsideEvent = (event: Event) => {
      const target = event.target as Node;
      const notificationBtn = document.querySelector('.notification-btn');
      
      // 如果点击的是通知按钮或其子元素，不关闭面板
      if (notificationBtn && (notificationBtn === target || notificationBtn.contains(target))) {
        console.log('[NotificationCenter] 点击了通知按钮，不关闭面板');
        return;
      }
      
      // 如果点击的是面板内部，不关闭
      if (el === target || el.contains(target)) {
        console.log('[NotificationCenter] 点击了面板内部，不关闭');
        return;
      }
      
      console.log('[NotificationCenter] 点击了外部区域，关闭面板');
      binding.value();
    };
    // 使用捕获阶段，确保在其他事件之前处理
    document.addEventListener('click', el.clickOutsideEvent, true);
  },
  unmounted(el: HTMLElement) {
    document.removeEventListener('click', el.clickOutsideEvent, true);
  },
};

// 使用 useWebSocket composable（添加错误处理）
// 由于 WebSocket 功能已禁用，直接使用默认值，避免加载相关库
// let websocketComposable: ReturnType<typeof useWebSocket> | null = null;
let isConnected = ref(false);
let isConnecting = ref(false);
let notifications = reactive<WebSocketMessage[]>([]);
let unreadCount = ref(0);
let markAllAsRead = () => {};
let removeNotification = (id: number) => {};

// 完全禁用 WebSocket 功能，避免加载 sockjs-client 库导致错误
// try {
//   websocketComposable = useWebSocket();
//   if (websocketComposable) {
//     isConnected = websocketComposable.isConnected;
//     isConnecting = websocketComposable.isConnecting;
//     notifications = websocketComposable.notifications;
//     unreadCount = websocketComposable.unreadCount;
//     markAllAsRead = websocketComposable.markAllAsRead;
//     removeNotification = websocketComposable.removeNotification;
//   }
// } catch (error) {
//   console.error('初始化 WebSocket 失败，通知功能将不可用:', error);
//   // 使用默认值，确保组件仍能正常渲染
// }

// 确保 notifications 始终是响应式数组
const safeNotifications = computed(() => {
  try {
    // 确保 notifications 存在且是数组
    if (!notifications) {
      return [];
    }
    // 如果 notifications 是响应式对象，访问其值
    const notificationsValue = Array.isArray(notifications) ? notifications : [];
    return notificationsValue || [];
  } catch (error) {
    console.warn('[NotificationCenter] safeNotifications 计算错误:', error);
    return [];
  }
});

const showPanel = ref(false);

// 未读通知ID集合（简化实现，实际应该从后端获取）
const readNotificationIds = ref<Set<number>>(new Set());

const togglePanel = (event?: Event) => {
  const safeNotificationsValue = safeNotifications.value || [];
  console.log('[NotificationCenter] togglePanel 被调用', {
    event,
    currentShowPanel: showPanel.value,
    unreadCount: unreadCount?.value || 0,
    notificationsCount: Array.isArray(safeNotificationsValue) ? safeNotificationsValue.length : 0,
    timestamp: Date.now(),
  });
  
  // 阻止事件冒泡和默认行为
  if (event) {
    event.stopPropagation();
    event.preventDefault();
  }
  
  const newValue = !showPanel.value;
  showPanel.value = newValue;
  
  console.log('[NotificationCenter] showPanel 更新为:', newValue);
  
  // 使用 nextTick 确保 DOM 已更新
  setTimeout(() => {
    const panel = document.querySelector('.notification-panel');
    console.log('[NotificationCenter] 面板 DOM 状态:', {
      exists: !!panel,
      visible: panel ? window.getComputedStyle(panel).display !== 'none' : false,
      showPanelValue: showPanel.value,
    });
  }, 0);
};

const closePanel = () => {
  console.log('[NotificationCenter] closePanel 被调用');
  showPanel.value = false;
};

const isUnread = (notification: WebSocketMessage) => {
  return !readNotificationIds.value.has(notification.id);
};

// 处理通知项点击
const handleNotificationClick = (notification: WebSocketMessage) => {
  // 标记为已读
  if (isUnread(notification)) {
    readNotificationIds.value.add(notification.id);
    // 如果未读计数大于0，则减少
    if (unreadCount.value > 0) {
      unreadCount.value--;
    }
  }
  
  // 根据通知类型执行相应操作
  // 这里可以根据需要添加路由跳转等逻辑
  console.log('点击通知:', notification);
};

const getNotificationIcon = (type: string) => {
  const iconMap: Record<string, any> = {
    welcome: InfoFilled,
    achievement: Trophy,
    'risk-update': DataAnalysis,
    learning: Reading,
    training: type === 'training' ? CircleCheck : CircleClose,
    login: User,
    connection: Link,
    notification: Bell,
  };
  return iconMap[type] || InfoFilled;
};

const getRiskTagType = (level?: string) => {
  const typeMap: Record<string, 'success' | 'warning' | 'danger'> = {
    '低风险': 'success',
    '中等风险': 'warning',
    '高风险': 'danger',
    low: 'success',
    medium: 'warning',
    high: 'danger',
  };
  return typeMap[level || ''] || 'info';
};

const formatTime = (timestamp: number) => {
  const now = Date.now();
  const diff = now - timestamp;
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return new Date(timestamp).toLocaleDateString();
};

const connectionStatusText = computed(() => {
  if (isConnecting.value) return '连接中...';
  if (isConnected.value) return '已连接';
  return '未连接';
});

const connectionStatusIcon = computed(() => {
  if (isConnecting.value) return WarningFilled;
  if (isConnected.value) return SuccessFilled;
  return CircleClose;
});

const connectionStatusClass = computed(() => {
  if (isConnecting.value) return 'status-connecting';
  if (isConnected.value) return 'status-connected';
  return 'status-disconnected';
});

// 监听通知变化，自动标记为已读
const handleMarkAllAsRead = () => {
  markAllAsRead();
  const notificationsList = safeNotifications.value;
  if (Array.isArray(notificationsList) && notificationsList.length > 0) {
    notificationsList.forEach(notification => {
      if (notification && notification.id) {
        readNotificationIds.value.add(notification.id);
      }
    });
  }
};

onMounted(() => {
  try {
    // 安全访问notifications，确保不会读取undefined的length属性
    const notificationsList = safeNotifications.value || [];
    const notificationsCount = Array.isArray(notificationsList) ? notificationsList.length : 0;
    
    console.log('[NotificationCenter] 组件已挂载', {
      notificationsCount,
      unreadCount: (unreadCount && typeof unreadCount.value !== 'undefined') ? unreadCount.value : 0,
      isConnected: (isConnected && typeof isConnected.value !== 'undefined') ? isConnected.value : false,
    });
    
    // 从localStorage恢复已读通知ID
    try {
      const saved = localStorage.getItem('af_read_notifications');
      if (saved) {
        readNotificationIds.value = new Set(JSON.parse(saved));
        console.log('[NotificationCenter] 恢复已读通知ID:', readNotificationIds.value.size);
      }
    } catch (error) {
      console.warn('恢复已读通知失败:', error);
    }

    // 监听通知变化，保存到localStorage
    const saveReadNotifications = () => {
      try {
        localStorage.setItem('af_read_notifications', JSON.stringify([...readNotificationIds.value]));
      } catch (error) {
        console.warn('保存已读通知失败:', error);
      }
    };

    // 每分钟保存一次
    setInterval(saveReadNotifications, 60000);
    
    // 添加按钮点击事件监听器（备用方案）
    setTimeout(() => {
      try {
        const btn = document.querySelector('.notification-btn');
        if (btn) {
          // 添加多个事件监听器用于调试
          ['click', 'mousedown', 'mouseup'].forEach(eventType => {
            btn.addEventListener(eventType, (e) => {
              console.log(`[NotificationCenter] 按钮 ${eventType} 事件触发`, {
                event: e,
                target: e.target,
                currentTarget: e.currentTarget,
                timestamp: Date.now(),
              });
            }, true); // 使用捕获阶段
          });
        } else {
          console.warn('[NotificationCenter] 未找到通知按钮元素！');
        }
      } catch (error) {
        console.warn('[NotificationCenter] 添加按钮事件监听器失败:', error);
      }
    }, 100);
  } catch (error) {
    console.error('[NotificationCenter] 组件挂载时发生错误:', error);
    // 不抛出错误，确保组件仍能渲染
  }
});
</script>

<style scoped lang="scss">
.notification-center {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 1000;
  pointer-events: auto;
}

.notification-btn {
  position: relative;
  padding: 8px;
  border-radius: 50%;
  cursor: pointer;
  pointer-events: auto;
  z-index: 1001;

  &:hover {
    background-color: var(--el-color-primary-light-9);
  }

  &.has-unread {
    color: var(--el-color-primary);
  }
  
  // 确保按钮可点击
  :deep(.el-button__inner) {
    pointer-events: none; // 让点击事件穿透到按钮本身
  }
}

.notification-badge {
  position: absolute;
  top: 4px;
  right: 4px;
}

.notification-panel {
  position: absolute;
  top: 50px;
  right: 0;
  width: 400px;
  max-height: 600px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: 1px solid var(--el-border-color-lighter);
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0;
}

.panel-actions {
  display: flex;
  gap: 8px;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  max-height: 500px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--el-text-color-secondary);
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 14px;
  margin: 0;
}

.notification-list {
  padding: 8px 0;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  transition: background-color 0.2s ease;
  position: relative;

  &:hover {
    background-color: var(--el-bg-color-page);
  }

  &.unread {
    background-color: var(--el-color-primary-light-9);

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 20px;
      background-color: var(--el-color-primary);
      border-radius: 0 2px 2px 0;
    }
  }

  &.notification-achievement {
    border-left: 4px solid #E6A23C;
  }

  &.notification-risk-update {
    border-left: 4px solid #F56C6C;
  }

  &.notification-training {
    border-left: 4px solid #67C23A;
  }
}

.notification-icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--el-text-color-secondary);
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.notification-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
  margin-left: 8px;
}

.notification-message {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.4;
  margin: 0 0 8px 0;
}

.notification-extra {
  margin-top: 8px;
}

.achievement-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-style: italic;
}

.risk-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.training-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notification-close {
  flex-shrink: 0;
  opacity: 0.6;
  margin-top: 2px;

  &:hover {
    opacity: 1;
  }
}

.panel-footer {
  padding: 12px 20px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color-page);
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.status-connected {
  color: #67C23A;
}

.status-connecting {
  color: #E6A23C;
}

.status-disconnected {
  color: #F56C6C;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .notification-panel {
    width: calc(100vw - 32px);
    max-width: none;
    left: 16px;
    right: 16px;
  }

  .notification-item {
    padding: 12px 16px;
  }
}
</style>