import { ref, computed, onUnmounted } from 'vue';
import { useLoadingStore } from '../stores/loading';

export function useLoading() {
  const loadingStore = useLoadingStore();

  // 生成唯一ID
  const generateId = () => `loading_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

  return {
    // 响应式状态
    isLoading: computed(() => loadingStore.isLoading),
    activeLoadings: computed(() => loadingStore.activeLoadings),

    // 控制方法
    startLoading: (message?: string, type: 'spinner' | 'progress' = 'spinner') => {
      const id = generateId();
      loadingStore.startLoading(id, message, type);
      return id;
    },

    updateProgress: (id: string, progress: number) => {
      loadingStore.updateProgress(id, progress);
    },

    finishLoading: (id: string) => {
      loadingStore.finishLoading(id);
    },

    setGlobalLoading: (loading: boolean) => {
      loadingStore.setGlobalLoading(loading);
    },

    clearAll: () => {
      loadingStore.clearAll();
    },
  };
}

// 自动管理的加载Hook
export function useAutoLoading(options: {
  message?: string;
  type?: 'spinner' | 'progress';
  autoStart?: boolean;
} = {}) {
  const { message = '加载中...', type = 'spinner', autoStart = false } = options;
  const { startLoading, finishLoading, updateProgress } = useLoading();

  const loadingId = ref<string | null>(null);
  const isActive = ref(false);

  const start = () => {
    if (loadingId.value) return;
    loadingId.value = startLoading(message, type);
    isActive.value = true;
  };

  const finish = () => {
    if (loadingId.value) {
      finishLoading(loadingId.value);
      loadingId.value = null;
      isActive.value = false;
    }
  };

  const update = (progress: number) => {
    if (loadingId.value) {
      updateProgress(loadingId.value, progress);
    }
  };

  // 组件卸载时自动清理
  onUnmounted(() => {
    finish();
  });

  // 自动开始
  if (autoStart) {
    start();
  }

  return {
    isActive: computed(() => isActive.value),
    start,
    finish,
    update,
  };
}

// HTTP请求的加载Hook
export function useHttpLoading() {
  const { startLoading, finishLoading } = useLoading();

  const wrapRequest = async <T>(
    requestFn: () => Promise<T>,
    options: {
      message?: string;
      onSuccess?: (result: T) => void;
      onError?: (error: any) => void;
    } = {}
  ): Promise<T> => {
    const { message = '请求中...', onSuccess, onError } = options;
    const loadingId = startLoading(message);

    try {
      const result = await requestFn();
      onSuccess?.(result);
      return result;
    } catch (error) {
      onError?.(error);
      throw error;
    } finally {
      finishLoading(loadingId);
    }
  };

  return {
    wrapRequest,
  };
}