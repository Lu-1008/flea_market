import axios from 'axios'
import { ElMessage } from 'element-plus'
import { BASE_URL, TIMEOUT, DEFAULT_HEADERS, getErrorMessage } from './config'

// 创建axios实例
const request = axios.create({
  baseURL: BASE_URL,
  timeout: TIMEOUT,
  headers: DEFAULT_HEADERS
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从sessionStorage获取token
    const token = sessionStorage.getItem('token')
    
    // 如果存在token，则添加到请求头
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    
    // 检查接口返回的状态码
    if (res.code !== 200) {
      // 处理401未授权错误
      if (res.code === 401) {
        // 检查是否是敏感API
        if (response.config.url.includes('/api/user/info')) {
          console.log('用户信息API返回401，静默处理');
          // 清除会话存储的认证信息
          sessionStorage.removeItem('token')
          sessionStorage.removeItem('userInfo')
          
          // 不显示全局错误，返回一个被拒绝的Promise
          return Promise.reject(new Error('登录已过期'));
        } else {
          // 显示错误消息
          ElMessage.error(res.message || '请先登录')
          
          // 清除会话存储的认证信息
          sessionStorage.removeItem('token')
          sessionStorage.removeItem('userInfo')
        }
      } else {
        // 检查是否是登出API
        if (response.config.url.includes('/api/auth/logout')) {
          // 登出API失败时不向用户显示错误，允许前端继续进行登出流程
          console.log('登出API返回错误，但允许前端登出:', res.message || '请求失败')
          return Promise.reject(new Error('登出API失败，但前端可以继续登出'));
        } else {
          // 其他错误正常显示
          ElMessage.error(res.message || '请求失败')
        }
      }
      
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    // 返回data字段的数据
    return res
  },
  error => {
    console.error('响应错误:', error)
    
    // 检查是否有响应
    if (error.response) {
      // 获取HTTP状态码
      const status = error.response.status
      
      // 权限相关错误（401、403）
      if (status === 401 || status === 403) {
        // 检查用户是否已登录
        const token = sessionStorage.getItem('token')
        if (!token) {
          // 用户未登录，不显示错误消息，只在控制台记录
          console.log('需要登录的资源，用户未登录')
          return Promise.reject(error)
        } else {
          // 用户已登录但token无效或过期
          ElMessage.error('您的登录已过期，请重新登录')
          // 清除会话存储的认证信息
          sessionStorage.removeItem('token')
          sessionStorage.removeItem('userInfo')
        }
      } 
      // 服务器错误（500）
      else if (status === 500) {
        // 检查是否是登出API
        if (error.config.url.includes('/api/auth/logout')) {
          // 登出API 500错误，不向用户显示错误消息
          console.log('登出API返回500错误，静默处理')
          return Promise.reject(error)
        }
        // 检查是否是需要登录的API
        else if (error.config.url.includes('/api/review') || 
            error.config.url.includes('/api/order') || 
            error.config.url.includes('/api/user/profile')) {
          // 这些API需要登录，不需要向用户显示错误
          console.log('API请求失败但不显示错误:', error.config.url)
          return Promise.reject(error)
        } else {
          // 其他500错误正常显示
          ElMessage.error(getErrorMessage(status))
        }
      }
      // 其他错误
      else {
        ElMessage.error(getErrorMessage(status))
      }
    } else if (error.request) {
      // 请求已发出但没有收到响应
      ElMessage.error('服务器无响应，请检查网络连接')
    } else {
      // 请求配置出错
      ElMessage.error('请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

export default request 