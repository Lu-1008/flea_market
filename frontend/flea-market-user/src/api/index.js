// API 模块入口文件
import * as authApi from './auth'
import { getFileUrl } from './config'

// 导出所有API模块
export {
  authApi,
  getFileUrl
}

// 导出默认对象
export default {
  auth: authApi,
  getFileUrl
} 