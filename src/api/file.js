import request from './request';
import { API_PREFIX } from './config';

// MinIO服务器URL，添加此配置以便直接访问MinIO
const MINIO_URL = 'http://127.0.0.1:9000';
const BUCKET_NAME = 'flea-market';

/**
 * 上传用户头像
 */
export function uploadUserAvatar(file, userId) {
  if (!file) {
    console.error('上传头像文件不能为空');
    return Promise.reject(new Error('上传头像文件不能为空'));
  }
  
  if (!userId) {
    console.error('上传头像必须指定用户ID');
    return Promise.reject(new Error('上传头像必须指定用户ID'));
  }
  
  console.log(`开始上传用户头像: userId=${userId}, 文件名=${file.name}, 大小=${file.size}`);
  
  // 创建一个新FormData对象
  const formData = new FormData();
  
  // 确保文件对象被正确添加
  formData.append('file', file);
  
  // 确保userId为数字类型
  const numericUserId = Number(userId);
  if (isNaN(numericUserId) || numericUserId <= 0) {
    console.error('用户ID必须是有效的正数:', userId);
    return Promise.reject(new Error('用户ID必须是有效的正数'));
  }
  
  // 添加用户ID到表单
  formData.append('userId', numericUserId);
  
  return request({
    url: `${API_PREFIX}/file/upload/user`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

/**
 * 上传商品图片
 */
export function uploadItemImage(file, itemId) {
  if (!itemId) {
    console.error('上传商品图片必须指定商品ID');
    return Promise.reject(new Error('上传商品图片必须指定商品ID'));
  }
  
  // 创建一个新FormData对象
  const formData = new FormData();
  
  // 确保文件对象被正确添加
  formData.append('file', file);
  
  // 确保itemId为数字类型
  const numericItemId = Number(itemId);
  if (isNaN(numericItemId) || numericItemId <= 0) {
    console.error('商品ID必须是有效的正数:', itemId);
    return Promise.reject(new Error('商品ID必须是有效的正数'));
  }
  
  // 添加商品ID到表单
  formData.append('itemId', numericItemId);
  
  // 请求时打印日志，检查请求参数
  console.log(`准备上传商品图片: itemId=${numericItemId}, 文件名=${file.name}, 大小=${file.size}`);
  
  return request({
    url: `${API_PREFIX}/file/upload/item`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

/**
 * 获取文件访问URL
 */
export function getFileUrl(fileKey, directAccess = true, noCache = false) {
  if (!fileKey) return '';
  
  // 如果已经是完整URL，直接返回
  if (fileKey.startsWith('http')) {
    return noCache ? `${fileKey}?t=${Date.now()}` : fileKey;
  }
  
  // 处理文件路径 - 移除可能存在的flea-market前缀
  let normalizedFileKey = fileKey;
  
  // 处理可能存在的flea-market前缀（向后兼容旧格式）
  if (normalizedFileKey.startsWith('flea-market/')) {
    normalizedFileKey = normalizedFileKey.substring('flea-market/'.length);
    console.debug(`移除flea-market前缀，处理后路径: ${normalizedFileKey}`);
  }
  
  let url;
  if (directAccess) {
    // 直接访问MinIO，不需要通过后端API
    // 确保文件名不再包含路径前缀
    if (normalizedFileKey.includes('/')) {
      console.warn(`文件路径包含斜杠，可能导致路径错误: ${normalizedFileKey}`);
    }
    url = `${MINIO_URL}/${BUCKET_NAME}/${normalizedFileKey}`;
  } else {
    // 通过后端API获取
    // API需要的是连字符格式
    if (normalizedFileKey.includes('/')) {
      normalizedFileKey = normalizedFileKey.replace(/\//g, '-');
    }
    url = `${API_PREFIX}/file/view/${normalizedFileKey}`;
  }
  
  // 添加时间戳以防止缓存问题
  return noCache ? `${url}?t=${Date.now()}` : url;
}

/**
 * 删除文件
 */
export function deleteFile(fileKey) {
  return request({
    url: `${API_PREFIX}/file/delete`,
    method: 'post',
    data: { fileKey }
  });
}

/**
 * 清除图片缓存
 * @param {string} fileKey - 文件路径/键
 */
export function clearImageCache(fileKey) {
  if (!fileKey) return Promise.reject('文件路径不能为空');
  
  return request({
    url: `${API_PREFIX}/file/clear-cache/${fileKey}`,
    method: 'get'
  });
} 