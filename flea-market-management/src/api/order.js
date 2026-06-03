import request from './request';
import { API_PREFIX } from './config';
/**
 * 获取订单列表

 */
export function getOrderList(params) {
  return request({
    url: `${API_PREFIX}/order/list`,
    method: 'get',
    params
  });
}

/**
 * 获取订单详情

 */
export function getOrderDetails(orderId) {
  // 确保orderId是数字类型
  const id = parseInt(orderId);
  if (isNaN(id)) {
    return Promise.reject({
      response: {
        data: {
          code: 400,
          message: '无效的订单ID'
        }
      }
    });
  }
  
  console.log('请求订单详情，ID:', id);
  return request({
    url: `${API_PREFIX}/order/${id}`,
    method: 'get'
  });
}

/**
 * 删除订单

 */
export function deleteOrder(orderId) {
  return request({
    url: `${API_PREFIX}/order/${orderId}`,
    method: 'delete'
  });
}

/**
 * 更新订单状态

 */
export function updateOrderStatus(orderId, status) {
  return request({
    url: `${API_PREFIX}/order/${orderId}/status/${status}`,
    method: 'put'
  });
}

/**
 * 批量删除订单
 */
export function batchDeleteOrders(orderIds) {
  return request({
    url: `${API_PREFIX}/order/batch`,
    method: 'delete',
    data: orderIds
  });
} 