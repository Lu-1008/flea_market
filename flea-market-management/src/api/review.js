import request from './request';
import { API_PREFIX } from './config';

/**
 * 获取评价列表

 */
export function getReviewList(params) {
  return request({
    url: `${API_PREFIX}/review/list`,
    method: 'get',
    params
  });
}

/**
 * 获取卖家评价列表

 */
export function getSellerReviews(sellerId, params) {
  return request({
    url: `${API_PREFIX}/review/seller/${sellerId}`,
    method: 'get',
    params
  });
}

/**
 * 获取卖家评分

 */
export function getSellerRating(sellerId) {
  return request({
    url: `${API_PREFIX}/review/seller/${sellerId}/rating`,
    method: 'get'
  });
}

/**
 * 获取评价详情

 */
export function getReviewDetails(reviewId) {
  console.log('API调用: getReviewDetails, reviewId =', reviewId);
  if (!reviewId) {
    console.error('无效的评价ID:', reviewId);
    return Promise.reject(new Error('无效的评价ID'));
  }
  
  return request({
    url: `${API_PREFIX}/review/${reviewId}`,
    method: 'get'
  });
}

/**
 * 删除评价

 */
export function deleteReview(reviewId) {
  return request({
    url: `${API_PREFIX}/review/${reviewId}`,
    method: 'delete'
  });
}

/**
 * 批量删除评价

 */
export function batchDeleteReviews(reviewIds) {
  return request({
    url: `${API_PREFIX}/review/batch`,
    method: 'delete',
    data: { reviewIds: reviewIds }
  });
}

/**
 * 获取评价统计信息

 */
export function getReviewStatistics() {
  return request({
    url: `${API_PREFIX}/review/statistics`,
    method: 'get'
  });
} 