import request from './request'

/**
 * 获取所有分类

 */
export function getAllCategories() {
  return request({
    url: '/api/category/all',
    method: 'get'
  })
}

/**
 * 获取分类列表

 */
export function getCategories() {
  return getAllCategories()
}

/**
 * 获取分类列表（用于表单选择）

 */
export function getCategoryList() {
  return getAllCategories()
}

/**
 * 获取主要分类（用于导航栏显示）

 */
export function getMainCategories() {
  // 由于后端没有专门的main接口，使用all接口获取所有分类
  return request({
    url: '/api/category/all',
    method: 'get'
  })
}

/**
 * 获取其他分类（用于"其他分类"下拉菜单）

 */
export function getOtherCategories() {
  // 由于后端没有专门的other接口，使用all接口获取所有分类
  return request({
    url: '/api/category/all',
    method: 'get'
  })
}

/**
 * 获取分类详情

 */
export function getCategoryById(id) {
  return request({
    url: `/api/category/${id}`,
    method: 'get'
  })
} 