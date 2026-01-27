import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue';
import { useErrorStore } from '../stores/error';

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

// WebSocket功能开关 - 设置为false可禁用WebSocket功能
const WEBSOCKET_ENABLED = false;

export function useWebSocket() {
  const errorStore = useErrorStore();
  const isConnected = ref(false);
  const isConnecting = ref(false);
  const reconnectAttempts = ref(0);
  const maxReconnectAttempts = 5;
  const reconnectInterval = ref(1000);

  // 通知队列
  const notifications = reactive<WebSocketMessage[]>([]);
  const unreadCount = ref(0);

  let stompClient: any = null;
  let reconnectTimer: number | null = null;

  // 连接WebSocket
  const connect = () => {
    // 如果WebSocket功能被禁用，直接返回
    if (!WEBSOCKET_ENABLED) {
      console.log('WebSocket功能已禁用');
      return;
    }
    
    if (isConnected.value || isConnecting.value) return;

    isConnecting.value = true;

    try {
      // 确保 global 已定义（在导入之前）
      if (typeof global === 'undefined' && typeof window !== 'undefined') {
        (window as any).global = globalThis;
      }
      
      // 动态导入SockJS和Stomp
      import('sockjs-client').then((SockJS) => {
        import('@stomp/stompjs').then((Stomp) => {
          const socket = new SockJS('/api/ws');
          stompClient = Stomp.Stomp.over(socket);

          stompClient.connect(
            {},
            () => {
              // 连接成功
              isConnected.value = true;
              isConnecting.value = false;
              reconnectAttempts.value = 0;
              reconnectInterval.value = 1000;

              console.log('WebSocket连接成功');

              // 订阅通知
              subscribeToNotifications();

              // 发送连接成功通知
              addNotification({
                id: Date.now(),
                title: '连接成功',
                message: '已连接到实时通知服务',
                type: 'connection',
                timestamp: Date.now(),
              });
            },
            (error: any) => {
              // 连接失败
              isConnected.value = false;
              isConnecting.value = false;

              console.error('WebSocket连接失败:', error);

              // 不添加错误通知，避免干扰用户
              // 如果WebSocket功能被禁用，不执行重连
              if (WEBSOCKET_ENABLED) {
                // 尝试重连（但不会执行，因为WEBSOCKET_ENABLED为false）
                scheduleReconnect();
              }
            }
          );
        });
      }).catch((error) => {
        console.error('加载WebSocket库失败:', error);
        isConnecting.value = false;
        // 不抛出错误，避免导致应用崩溃
        // 只在开发环境显示详细错误信息
        if (import.meta.env.DEV) {
          console.warn('WebSocket功能将不可用，但不影响其他功能');
        }
      });
    } catch (error) {
      console.error('WebSocket初始化失败:', error);
      isConnecting.value = false;
    }
  };

  // 断开连接
  const disconnect = () => {
    if (stompClient && isConnected.value) {
      stompClient.disconnect(() => {
        console.log('WebSocket连接已断开');
      });
    }

    isConnected.value = false;
    isConnecting.value = false;

    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
  };

  // 订阅通知
  const subscribeToNotifications = () => {
    if (!stompClient || !isConnected.value) return;

    // 订阅公共通知
    stompClient.subscribe('/topic/notifications', (message: any) => {
      try {
        const notification: WebSocketMessage = JSON.parse(message.body);
        addNotification(notification);
      } catch (error) {
        console.error('解析公共通知失败:', error);
      }
    });

    // 订阅私人通知（需要用户ID）
    const userId = localStorage.getItem('af_user')
      ? JSON.parse(localStorage.getItem('af_user')!).userId
      : null;

    if (userId) {
      stompClient.subscribe(`/user/queue/notifications`, (message: any) => {
        try {
          const notification: WebSocketMessage = JSON.parse(message.body);
          addNotification(notification);
        } catch (error) {
          console.error('解析私人通知失败:', error);
        }
      });
    }
  };

  // 添加通知
  const addNotification = (notification: WebSocketMessage) => {
    notifications.unshift(notification);
    unreadCount.value++;

    // 限制通知数量
    if (notifications.length > 50) {
      notifications.splice(50);
    }

    // 触发浏览器通知（如果用户允许）
    if (Notification.permission === 'granted') {
      new Notification(notification.title, {
        body: notification.message,
        icon: '/favicon.ico',
        tag: `antifraud-${notification.type}`,
      });
    }
  };

  // 标记所有通知为已读
  const markAllAsRead = () => {
    unreadCount.value = 0;
  };

  // 删除通知
  const removeNotification = (id: number) => {
    const index = notifications.findIndex(n => n.id === id);
    if (index > -1) {
      notifications.splice(index, 1);
      if (unreadCount.value > 0) {
        unreadCount.value--;
      }
    }
  };

  // 发送消息
  const sendMessage = (destination: string, message: any) => {
    if (stompClient && isConnected.value) {
      stompClient.send(destination, {}, JSON.stringify(message));
    }
  };

  // 发送心跳
  const sendHeartbeat = () => {
    sendMessage('/app/ping', { timestamp: Date.now() });
  };

  // 发送用户状态
  const sendUserStatus = (status: string) => {
    sendMessage('/app/user-status', { status, timestamp: Date.now() });
  };

  // 发送学习进度
  const sendLearningProgress = (knowledgeId: string, title: string, completed: number) => {
    sendMessage('/app/learning-progress', {
      knowledgeId,
      title,
      completed,
      timestamp: Date.now()
    });
  };

  // 调度重连
  const scheduleReconnect = () => {
    // 如果WebSocket功能被禁用，不执行重连
    if (!WEBSOCKET_ENABLED) {
      console.log('WebSocket功能已禁用，停止重连');
      return;
    }
    
    if (reconnectAttempts.value >= maxReconnectAttempts) {
      console.warn('WebSocket重连次数已达上限，停止重连');
      return;
    }

    reconnectAttempts.value++;
    reconnectInterval.value = Math.min(reconnectInterval.value * 2, 30000); // 指数退避，最多30秒

    console.log(`WebSocket将在 ${reconnectInterval.value}ms 后尝试重连 (${reconnectAttempts.value}/${maxReconnectAttempts})`);

    reconnectTimer = window.setTimeout(() => {
      connect();
    }, reconnectInterval.value);
  };

  // 请求通知权限
  const requestNotificationPermission = async () => {
    if ('Notification' in window && Notification.permission === 'default') {
      const permission = await Notification.requestPermission();
      return permission === 'granted';
    }
    return Notification.permission === 'granted';
  };

  // 组件挂载时连接
  onMounted(async () => {
    // 如果WebSocket功能被禁用，不执行任何连接操作
    if (!WEBSOCKET_ENABLED) {
      console.log('WebSocket功能已禁用，跳过初始化');
      return;
    }
    
    try {
      await nextTick();
      
      // 确保 global 已定义（双重保险）
      if (typeof global === 'undefined' && typeof window !== 'undefined') {
        (window as any).global = globalThis;
      }
      
      await requestNotificationPermission();
      
      // 延迟更长时间，确保所有 polyfill 都已生效
      setTimeout(() => {
        try {
          connect();
        } catch (error) {
          console.error('WebSocket连接尝试失败:', error);
          isConnecting.value = false;
          // 不抛出错误，避免导致应用崩溃
        }
      }, 500); // 增加到 500ms，确保 polyfill 完全生效
    } catch (error) {
      console.error('WebSocket初始化失败:', error);
      isConnecting.value = false;
      // 不抛出错误，避免导致应用崩溃
    }

    // 设置心跳定时器
    const heartbeatTimer = setInterval(() => {
      if (isConnected.value) {
        sendHeartbeat();
      }
    }, 30000); // 每30秒发送一次心跳

    // 组件卸载时清理
    onUnmounted(() => {
      clearInterval(heartbeatTimer);
    });
  });

  // 组件卸载时断开连接
  onUnmounted(() => {
    disconnect();
  });

  return {
    // 状态
    isConnected,
    isConnecting,
    notifications,
    unreadCount,

    // 方法
    connect,
    disconnect,
    markAllAsRead,
    removeNotification,
    sendUserStatus,
    sendLearningProgress,
  };
}