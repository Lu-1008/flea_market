import request from './request'

/**
 * 获取卖家评价列表
 */
export function getSellerReviews(sellerId, params = {}) {
  return request({
    url: `/api/review/seller/${sellerId}`,
    method: 'get',
    params
  })
}

/**
 * 获取卖家评分
 */
export function getSellerRating(sellerId) {
  return request({
    url: `/api/review/seller/${sellerId}/rating`,
    method: 'get'
  })
}

/**
 * 添加评价
 */
export function addReview(data) {
  // 确保请求数据符合后端期望的格式
  const reviewData = {
    orderId: data.orderId,
    itemId: data.itemId,
    userId: data.userId,
    rating: data.rating || 5,
    comment: data.comment || ''
  };
  
  return request({
    url: '/api/review',
    method: 'post',
    data: reviewData
  })
}

/**
 * 获取用户评价列表
 */
export function getUserReviews(params = {}) {
  return request({
    url: '/api/review/user',
    method: 'get',
    params
  })
}

/**
 * 获取待评价订单列表
 */
export function getPendingReviewOrders(params = {}) {
  return request({
    url: '/api/review/pending',
    method: 'get',
    params
  })
} 