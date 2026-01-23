import axios from 'axios';

// 基础 Axios 实例封装（默认走相对路径，由 Vite 代理到后端，避免 CORS）
const http = axios.create({
  baseURL: '/api',
  timeout: 8000,
});

http.interceptors.response.use(
  (resp) => {
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
    // 网络不可达时不在控制台刷屏，交由页面的 catch 处理（已给出提示/回退）
    if (!(error?.code === 'ERR_NETWORK')) {
      console.warn('API error', error);
    }
    return Promise.reject(error);
  },
);

export default http;

