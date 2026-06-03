import axios from 'axios';
import { BASE_URL, TIMEOUT } from './config';

// 创建axios实例
const request = axios.create({
  baseURL: BASE_URL,
  timeout: TIMEOUT, // 请求超时时间
  withCredentials: true // 确保跨域请求携带凭证（cookies等）
});

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 添加请求日志
    console.log(`[请求] ${config.method.toUpperCase()} ${config.url}`, config.params || config.data || '');
    
    // 从sessionStorage获取token
    const token = sessionStorage.getItem('token');
    // 如果token存在，则添加到请求头
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    console.error('[请求错误]', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 添加响应日志
    console.log(`[响应] ${response.config.method.toUpperCase()} ${response.config.url}`, response.data);
    
    // 如果返回的状态码为200，说明接口请求成功，可以正常拿到数据
    if (response.status === 200) {
      // 检查响应数据格式是否符合预期
      if (!response.data) {
        console.warn('[响应警告] 响应数据为空');
      } else if (!response.data.hasOwnProperty('code')) {
        console.warn('[响应警告] 响应数据缺少code字段');
      }
      
      return response; // 返回整个response对象，以保持与现有代码的兼容性
    }
    return Promise.reject(response);
  },
  error => {
    // 添加错误日志
    console.error('[响应错误]', error.config ? `${error.config.method.toUpperCase()} ${error.config.url}` : '', error.response || error.message || error);
    
    // 处理网络错误
    let message = '';
    if (error.response && error.response.status) {
      switch (error.response.status) {
        case 401:
          message = '未授权，请重新登录';
          // 可以在这里执行退出登录操作
          sessionStorage.removeItem('token');
          sessionStorage.removeItem('user');
          sessionStorage.removeItem('isLogin');
          window.location.href = '/login';
          break;
        case 403:
          message = '拒绝访问';
          break;
        case 404:
          message = '请求地址出错';
          break;
        case 500:
          message = '服务器内部错误';
          break;
        default:
          message = `连接出错(${error.response.status})!`;
      }
    } else {
      // 提供更详细的网络错误信息
      if (error.message === 'Network Error') {
        message = '网络连接异常，请检查后端服务是否正常运行';
      } else if (error.message && error.message.includes('timeout')) {
        message = '请求超时，请检查网络连接';
      } else {
        message = '网络连接异常，请稍后再试: ' + (error.message || '未知错误');
      }
      console.error('详细错误信息:', error);
    }
    console.error(message);
    return Promise.reject(error);
  }
);

export default request; 