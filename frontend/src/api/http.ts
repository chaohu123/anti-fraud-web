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
      // 返回后端的具体错误信息
      const errorMessage = body.message || '接口调用失败';
      return Promise.reject(new Error(errorMessage));
    }
    return resp;
  },
  (error) => {
    // 处理 HTTP 错误响应（如 400, 500 等）
    if (error.response) {
      const responseData = error.response.data;
      // 如果后端返回了统一的 Result 格式
      if (responseData && typeof responseData === 'object' && 'message' in responseData) {
        const errorMessage = responseData.message || '系统内部错误';
        return Promise.reject(new Error(errorMessage));
      }
      // 否则使用 HTTP 状态码信息
      const statusText = error.response.statusText || '请求失败';
      return Promise.reject(new Error(`${error.response.status}: ${statusText}`));
    }
    // 网络错误等其他错误
    if (error.code === 'ERR_NETWORK') {
      return Promise.reject(new Error('网络连接失败，请检查网络设置'));
    }
    // 其他错误，显示错误消息或默认消息
    const errorMessage = error.message || '系统内部错误';
    return Promise.reject(new Error(errorMessage));
  },
);

export default http;

