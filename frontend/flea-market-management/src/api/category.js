import request from './request';
import { API_PREFIX } from './config';
/**
 * 获取分类列表

 */
export function getCategoryList(params) {
  return request({
    url: `${API_PREFIX}/category/list`,
    method: 'get',
    params
  });
}

/**
 * 创建分类

 */
export function createCategory(data) {
  return request({
    url: `${API_PREFIX}/category/create`,
    method: 'post',
    data
  });
}

/**
 * 更新分类

 */
export function updateCategory(data) {
  return request({
    url: `${API_PREFIX}/category/update`,
    method: 'put',
    data
  });
}

/**
 * 删除分类

 */
export function deleteCategory(categoryId) {
  return request({
    url: `${API_PREFIX}/category/${categoryId}`,
    method: 'delete'
  });
}

/**
 * 获取所有分类

 */
export function getAllCategories() {
  return request({
    url: `${API_PREFIX}/category/categories`,
    method: 'get'
  });
}

/**
 * 批量删除分类
 * @param {Array} categoryIds 分类ID数组
 */
export function batchDeleteCategories(categoryIds) {
  return request({
    url: `${API_PREFIX}/category/batch`,
    method: 'delete',
    data: categoryIds
  });
} 