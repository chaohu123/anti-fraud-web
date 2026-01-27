import { ref } from 'vue';
import { useErrorStore } from '../stores/error';

export function useError() {
  const errorStore = useErrorStore();

  const handleError = (
    error: any,
    options: {
      context?: string;
      showToast?: boolean;
      retryable?: boolean;
      retryFn?: () => Promise<any>;
    } = {}
  ) => {
    const { context, showToast = true, retryable = false, retryFn } = options;

    const appError = errorStore.createFromError(error, context);
    appError.retryable = retryable;
    appError.retryFn = retryFn;

    const errorId = errorStore.addError(appError);

    // 如果需要显示toast，使用Element Plus的消息提示
    if (showToast) {
      import('element-plus').then(({ ElMessage }) => {
        const messageType = getMessageType(appError.type);
        ElMessage({
          type: messageType,
          message: appError.message,
          duration: 5000,
          showClose: true,
        });
      });
    }

    return errorId;
  };

  const clearError = (id: string) => {
    errorStore.removeError(id);
  };

  const clearAllErrors = () => {
    errorStore.clearAllErrors();
  };

  return {
    handleError,
    clearError,
    clearAllErrors,
  };
}

// 获取Element Plus消息类型
function getMessageType(errorType: string) {
  const typeMap: Record<string, 'error' | 'warning' | 'info'> = {
    network: 'warning',
    auth: 'error',
    validation: 'warning',
    server: 'error',
    client: 'warning',
    unknown: 'error',
  };
  return typeMap[errorType] || 'error';
}

// 包装异步操作的错误处理
export function useAsyncError() {
  const { handleError } = useError();
  const loading = ref(false);

  const wrapAsync = async <T>(
    asyncFn: () => Promise<T>,
    options: {
      context?: string;
      showToast?: boolean;
      retryable?: boolean;
      onSuccess?: (result: T) => void;
      onError?: (error: any) => void;
    } = {}
  ): Promise<T | null> => {
    const { onSuccess, onError, ...errorOptions } = options;

    loading.value = true;
    try {
      const result = await asyncFn();
      onSuccess?.(result);
      return result;
    } catch (error) {
      handleError(error, errorOptions);
      onError?.(error);
      return null;
    } finally {
      loading.value = false;
    }
  };

  return {
    loading: loading.value,
    wrapAsync,
  };
}

// 网络请求的错误处理
export function useNetworkError() {
  const { handleError } = useError();

  const wrapRequest = async <T>(
    requestFn: () => Promise<T>,
    options: {
      context?: string;
      retryCount?: number;
      retryDelay?: number;
    } = {}
  ): Promise<T> => {
    const { context, retryCount = 1, retryDelay = 1000 } = options;

    let lastError: any;

    for (let i = 0; i <= retryCount; i++) {
      try {
        return await requestFn();
      } catch (error) {
        lastError = error;

        // 如果不是网络错误或最后一次重试，不再重试
        if (error?.code !== 'ERR_NETWORK' || i === retryCount) {
          break;
        }

        // 等待重试
        if (i < retryCount) {
          await new Promise(resolve => setTimeout(resolve, retryDelay));
        }
      }
    }

    // 抛出错误，让调用方处理
    throw lastError;
  };

  return {
    wrapRequest,
  };
}