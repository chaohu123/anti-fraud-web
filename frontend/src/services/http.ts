import axios from 'axios';

const http = axios.create({
  baseURL: '/api',
  timeout: 8000,
});

http.interceptors.response.use(
  (resp) => {
    const body = resp.data;
    if (body && typeof body === 'object' && 'success' in body) {
      if (body.success) {
        return { ...resp, data: body.data };
      }
      return Promise.reject(new Error(body.message || '接口调用失败'));
    }
    return resp;
  },
  (error) => {
    if (!(error?.code === 'ERR_NETWORK')) {
      console.warn('API error', error);
    }
    return Promise.reject(error);
  },
);

export default http;
