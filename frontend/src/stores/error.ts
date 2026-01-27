import { defineStore } from 'pinia';

export type ErrorType = 'network' | 'auth' | 'validation' | 'server' | 'client' | 'unknown';

export interface AppError {
  id: string;
  type: ErrorType;
  title: string;
  message: string;
  details?: any;
  timestamp: number;
  retryable: boolean;
  retryFn?: () => Promise<any>;
}

export const useErrorStore = defineStore('error', {
  state: () => ({
    errors: new Map<string, AppError>(),
    globalError: null as AppError | null,
  }),

  getters: {
    hasErrors: (state) => state.errors.size > 0,
    activeErrors: (state) => Array.from(state.errors.values()),
    hasGlobalError: (state) => state.globalError !== null,
  },

  actions: {
    // 添加错误
    addError(error: Omit<AppError, 'id' | 'timestamp'>) {
      const id = `error_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      const fullError: AppError = {
        id,
        timestamp: Date.now(),
        ...error,
      };

      this.errors.set(id, fullError);

      // 如果是严重错误，设置为全局错误
      if (error.type === 'auth' || error.type === 'server') {
        this.globalError = fullError;
      }

      // 自动清理旧错误（保留最近10个）
      if (this.errors.size > 10) {
        const oldestId = Array.from(this.errors.keys())[0];
        this.errors.delete(oldestId);
      }

      return id;
    },

    // 移除错误
    removeError(id: string) {
      this.errors.delete(id);
      if (this.globalError?.id === id) {
        this.globalError = null;
      }
    },

    // 重试错误
    async retryError(id: string) {
      const error = this.errors.get(id);
      if (error?.retryable && error.retryFn) {
        try {
          await error.retryFn();
          this.removeError(id);
        } catch (newError) {
          // 重试失败，更新错误信息
          this.updateError(id, {
            title: '重试失败',
            message: '操作重试后仍然失败，请稍后再试',
          });
        }
      }
    },

    // 更新错误
    updateError(id: string, updates: Partial<Pick<AppError, 'title' | 'message' | 'details'>>) {
      const error = this.errors.get(id);
      if (error) {
        Object.assign(error, updates);
      }
    },

    // 清空所有错误
    clearAllErrors() {
      this.errors.clear();
      this.globalError = null;
    },

    // 从Error对象创建AppError
    createFromError(error: any, context?: string): AppError {
      let type: ErrorType = 'unknown';
      let title = '发生错误';
      let message = '未知错误，请稍后重试';
      let retryable = false;

      if (error?.code === 'ERR_NETWORK') {
        type = 'network';
        title = '网络连接错误';
        message = '无法连接到服务器，请检查网络连接';
        retryable = true;
      } else if (error?.response?.status === 401) {
        type = 'auth';
        title = '认证失败';
        message = '请重新登录';
        retryable = false;
      } else if (error?.response?.status === 403) {
        type = 'auth';
        title = '权限不足';
        message = '您没有执行此操作的权限';
        retryable = false;
      } else if (error?.response?.status >= 400 && error?.response?.status < 500) {
        type = 'validation';
        title = '请求错误';
        message = error?.response?.data?.message || '请求参数有误';
        retryable = false;
      } else if (error?.response?.status >= 500) {
        type = 'server';
        title = '服务器错误';
        message = '服务器暂时不可用，请稍后重试';
        retryable = true;
      } else if (error?.message) {
        type = 'client';
        title = '操作失败';
        message = error.message;
        retryable = true;
      }

      return {
        id: '',
        type,
        title,
        message: context ? `${context}: ${message}` : message,
        details: error,
        timestamp: Date.now(),
        retryable,
      };
    },
  },
});