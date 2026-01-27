import { defineStore } from 'pinia';

export interface LoadingState {
  id: string;
  message?: string;
  type: 'spinner' | 'progress';
  progress?: number;
}

export const useLoadingStore = defineStore('loading', {
  state: () => ({
    loadings: new Map<string, LoadingState>(),
    globalLoading: false,
  }),

  getters: {
    isLoading: (state) => state.loadings.size > 0 || state.globalLoading,
    activeLoadings: (state) => Array.from(state.loadings.values()),
    hasGlobalLoading: (state) => state.globalLoading,
  },

  actions: {
    // 开始加载
    startLoading(id: string, message?: string, type: 'spinner' | 'progress' = 'spinner', initialProgress = 0) {
      this.loadings.set(id, {
        id,
        message,
        type,
        progress: type === 'progress' ? initialProgress : undefined,
      });
    },

    // 更新进度
    updateProgress(id: string, progress: number) {
      const loading = this.loadings.get(id);
      if (loading && loading.type === 'progress') {
        loading.progress = Math.min(100, Math.max(0, progress));
        this.loadings.set(id, loading);
      }
    },

    // 结束加载
    finishLoading(id: string) {
      this.loadings.delete(id);
    },

    // 设置全局加载
    setGlobalLoading(loading: boolean) {
      this.globalLoading = loading;
    },

    // 清空所有加载状态
    clearAll() {
      this.loadings.clear();
      this.globalLoading = false;
    },

    // 获取加载消息
    getLoadingMessage(id: string): string | undefined {
      return this.loadings.get(id)?.message;
    },
  },
});