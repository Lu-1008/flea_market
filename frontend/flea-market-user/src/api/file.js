import request from './request'

/**
 * 上传商品图片
 */
export function uploadProductImage(formData) {
  return request({
    url: '/api/file/upload/item',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 上传用户头像
 **/
export function uploadUserAvatar(formData) {
  // 添加调试信息
  console.log('准备发送头像上传请求:', {
    url: '/api/file/upload/user',
    formDataEntries: Array.from(formData.entries()).map(([key, value]) => {
      if (value instanceof File) {
        return [key, `File(name=${value.name}, type=${value.type}, size=${value.size})`];
      }
      return [key, value];
    })
  });
  
  return request({
    url: '/api/file/upload/user',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    // 添加超时设置
    timeout: 30000, // 30秒超时
    // 错误处理
    validateStatus: function(status) {
      return status >= 200 && status < 300; // 默认值
    }
  }).catch(error => {
    console.error('头像上传请求失败:', error);
    if (error.response) {
      console.error('错误响应:', {
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data
      });
    }
    throw error;
  });
} 