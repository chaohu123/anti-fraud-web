import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
import { useLoadingStore } from '../stores/loading';

// 创建基础 Axios 实例
const http = axios.create({
  baseURL: '/api',
  timeout: 8000,
});

const loadingStore = useLoadingStore();

// 请求拦截器：自动开始加载
http.interceptors.request.use(
  (config: AxiosRequestConfig & { loadingId?: string; loadingMessage?: string }) => {
    // 如果配置了loadingId，则启动加载状态
    if (config.loadingId) {
      loadingStore.startLoading(
        config.loadingId,
        config.loadingMessage || '加载中...',
        'spinner'
      );
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：自动结束加载
http.interceptors.response.use(
  (resp: AxiosResponse & { config: { loadingId?: string } }) => {
    // 如果请求配置了loadingId，则结束加载状态
    if (resp.config.loadingId) {
      loadingStore.finishLoading(resp.config.loadingId);
    }

    // 后端统一返回 Result：{ success, code, message, data }
    const body = resp.data;
    if (body && typeof body === 'object' && 'success' in body) {
      if (body.success) {
        // 兼容现有页面写法：仍然通过 resp.data 读取业务数据
        return { ...resp, data: body.data };
      }
      return Promise.reject(new Error(body.message || '接口调用失败'));
    }
    return resp;
  },
  (error) => {
    // 如果请求配置了loadingId，则结束加载状态
    if (error.config?.loadingId) {
      loadingStore.finishLoading(error.config.loadingId);
    }

    // 网络不可达时不在控制台刷屏，交由页面的 catch 处理（已给出提示/回退）
    if (!(error?.code === 'ERR_NETWORK')) {
      console.warn('API error', error);
    }
    return Promise.reject(error);
  }
);

// 扩展的HTTP方法，支持自动加载状态管理
export const httpClient = {
  get: <T = any>(url: string, config?: AxiosRequestConfig & { loadingId?: string; loadingMessage?: string }) =>
    http.get<T>(url, config),

  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig & { loadingId?: string; loadingMessage?: string }) =>
    http.post<T>(url, data, config),

  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig & { loadingId?: string; loadingMessage?: string }) =>
    http.put<T>(url, data, config),

  delete: <T = any>(url: string, config?: AxiosRequestConfig & { loadingId?: string; loadingMessage?: string }) =>
    http.delete<T>(url, config),

  patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig & { loadingId?: string; loadingMessage?: string }) =>
    http.patch<T>(url, data, config),
};

export default http;