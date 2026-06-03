import request from './request';
import { API_PREFIX } from './config';

/**
 * 获取首页统计数据
 */
export function getStatisticsData() {
  return request({
    url: `${API_PREFIX}/dashboard/statistics`,
    method: 'get'
  });
}

/**
 * 获取用户总数

 */
export function getUserCount() {
  return request({
    url: `${API_PREFIX}/dashboard/user-count`,
    method: 'get'
  });
}

/**
 * 获取商品总数

 */
export function getItemCount() {
  return request({
    url: `${API_PREFIX}/dashboard/item-count`,
    method: 'get'
  });
}

/**
 * 获取总销售额

 */
export function getSalesAmount() {
  return request({
    url: `${API_PREFIX}/dashboard/sales-amount`,
    method: 'get'
  });
} 