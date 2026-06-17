import request from './request'

/**
 * 获取商品列表
 */
export function getProducts(params) {
  // 后端已默认按created_at DESC排序，无需额外参数
  return request({
    url: '/api/item/list',
    method: 'get',
    params
  })
}

/**
 * 获取商品详情

 */
export function getProductById(id) {
  return request({
    url: `/api/item/${id}`,
    method: 'get'
  })
}

/**
 * 创建新商品

 */
export function createProduct(product) {
  // 确保必要字段存在
  if (!product.title || !product.title.trim()) {
    return Promise.reject(new Error('商品标题不能为空'))
  }
  
  if (!product.description || !product.description.trim()) {
    return Promise.reject(new Error('商品描述不能为空'))
  }
  
  if (!product.price || product.price <= 0) {
    return Promise.reject(new Error('商品价格必须大于0'))
  }
  
  if (!product.categoryId) {
    return Promise.reject(new Error('请选择商品分类'))
  }
  
  if (!product.userId) {
    return Promise.reject(new Error('用户ID不能为空'))
  }

  // 创建请求
  return request({
    url: '/api/item/create',
    method: 'post',
    data: product
  })
}

/**
 * 获取推荐商品

 */
export function getRecommendProducts(limit = 6) {
  return request({
    url: '/api/item/list',
    method: 'get',
    params: { 
      page: 1,
      size: limit,
      status: 'ACTIVE'
    }
  })
}

/**
 * 获取热门商品

 */
export function getHotProducts(limit = 6) {
  return request({
    url: '/api/item/list',
    method: 'get',
    params: { 
      page: 1,
      size: limit,
      status: 'ACTIVE'
    }
  })
}

/**
 * 获取某个分类下的商品

 */
export function getProductsByCategory(categoryId, params) {
  return request({
    url: '/api/item/list',
    method: 'get',
    params: {
      ...params,
      categoryId,
      status: params.status || 'ACTIVE'
    }
  })
}

/**
 * 搜索商品

 */
export function searchProducts(keyword, params) {
  return request({
    url: '/api/item/list',
    method: 'get',
    params: {
      ...params,
      title: keyword,
      status: params.status || 'ACTIVE'
    }
  })
}

/**
 * 更新商品信息

 */
export function updateProduct(product) {
  // 确保商品ID存在
  if (!product.itemId) {
    return Promise.reject(new Error('商品ID不能为空'))
  }
  
  return request({
    url: '/api/item/update',
    method: 'put',
    data: product
  })
}

/**
 * 获取用户发布的商品列表

 */
export function getUserItems(userId, params = {}) {
  return request({
    url: '/api/item/list',
    method: 'get',
    params: {
      ...params,
      userId
    }
  })
}

/**
 * 更新商品状态

 */
export function updateItemStatus(itemId, status) {
  return request({
    url: `/api/item/${itemId}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 删除商品

 */
export function deleteItem(itemId) {
  return request({
    url: `/api/item/${itemId}`,
    method: 'delete'
  })
}

/**
 * 更新商品图片URL（专用API，避免触发其他字段验证）
 */
export function updateItemImage(itemId, imageUrl) {
  return request({
    url: `/api/item/${itemId}/image`,
    method: 'put',
    data: { itemImageUrl: imageUrl }
  })
} 