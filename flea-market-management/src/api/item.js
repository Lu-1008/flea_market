import request from './request';
import { API_PREFIX } from './config';
/**
 * 获取商品列表

 */
export function getItemList(params) {
  return request({
    url: `${API_PREFIX}/item/list`,
    method: 'get',
    params
  });
}

/**
 * 获取商品详情

 */
export function getItemById(itemId) {
  return request({
    url: `${API_PREFIX}/item/${itemId}`,
    method: 'get'
  });
}

/**
 * 创建商品

 */
export function createItem(data) {
  return request({
    url: `${API_PREFIX}/item/create`,
    method: 'post',
    data
  });
}

/**
 * 更新商品

 */
export function updateItem(data) {
  return request({
    url: `${API_PREFIX}/item/update`,
    method: 'put',
    data
  });
}

/**
 * 删除商品

 */
export function deleteItem(itemId) {
  return request({
    url: `${API_PREFIX}/item/${itemId}`,
    method: 'delete'
  });
}

/**
 * 更新商品状态

 */
export function updateItemStatus(itemId, status) {
  return request({
    url: `${API_PREFIX}/item/${itemId}/status`,
    method: 'put',
    params: { status }
  });
}

/**
 * 批量删除商品

 */
export function batchDeleteItems(itemIds) {
  return request({
    url: `${API_PREFIX}/item/batch`,
    method: 'delete',
    data: itemIds
  });
}

/**
 * 更新商品图片URL
 * @param {number} itemId - 商品ID
 * @param {string} imageUrl - 图片URL
 */
export function updateItemImage(itemId, imageUrl) {
  return request({
    url: `${API_PREFIX}/item/${itemId}/image`,
    method: 'put',
    data: { 
      itemImageUrl: imageUrl 
    }
  });
} 