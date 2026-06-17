/**
 * API基础URL配置
 * 使用环境变量或默认值
 */
export const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

/**
 * API请求超时时间（毫秒）
 */
export const TIMEOUT = 15000

/**
 * 默认请求头
 */
export const DEFAULT_HEADERS = {
  'Content-Type': 'application/json'
}

/**
 * 错误消息配置
 */
export const ERROR_MESSAGES = {
  400: '请求参数错误',
  401: '未授权，请重新登录',
  403: '拒绝访问',
  404: '请求的资源不存在',
  500: '服务器内部错误',
  502: '网关错误',
  503: '服务不可用',
  504: '网关超时',
  default: '系统繁忙，请稍后再试'
}

/**
 * 获取错误消息
 * @param {number} status - HTTP状态码
 * @returns {string} - 对应的错误消息
 */
export function getErrorMessage(status) {
  return ERROR_MESSAGES[status] || ERROR_MESSAGES.default
}

/**
 * MinIO配置（如果需要直接访问文件）
 */
export const MINIO_CONFIG = {
  url: 'http://127.0.0.1:9000',
  bucket: 'flea-market'
}

/**
 * 获取文件URL

 */
export function getFileUrl(key, direct = false, noCache = false) {
  if (!key) return '';
  
  // 如果已经是完整URL，直接返回
  if (key.startsWith('http')) {
    return noCache ? `${key}?t=${Date.now()}` : key;
  }
  
  let url;
  if (direct) {
    // 直接从MinIO访问
    url = `${MINIO_CONFIG.url}/${MINIO_CONFIG.bucket}/${key}`;
  } else {
    // 通过API代理访问
    url = `${BASE_URL}/file/view/${key}`;
  }
  
  // 添加时间戳以防止缓存问题
  return noCache ? `${url}?t=${Date.now()}` : url;
}