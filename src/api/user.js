import request from './request';
import { API_PREFIX } from './config';
/**
 * 获取用户列表

 */
export function getUserList(params) {
  return request({
    url: `${API_PREFIX}/user/list`,
    method: 'get',
    params
  });
}

/**
 * 获取用户详情

 */
export function getUserById(userId) {
  return request({
    url: `${API_PREFIX}/user/${userId}`,
    method: 'get'
  });
}

/**
 * 创建用户

 */
export function createUser(data) {
  return request({
    url: `${API_PREFIX}/user/create`,
    method: 'post',
    data
  });
}

/**
 * 更新用户

 */
export function updateUser(data) {
  return request({
    url: `${API_PREFIX}/user/update`,
    method: 'put',
    data
  });
}

/**
 * 删除用户

 */
export function deleteUser(userId) {
  return request({
    url: `${API_PREFIX}/user/${userId}`,
    method: 'delete'
  });
}

/**
 * 获取所有角色

 */
export function getAllRoles() {
  return request({
    url: `${API_PREFIX}/user/roles`,
    method: 'get'
  });
}

/**
 * 批量删除用户

 */
export function batchDeleteUsers(userIds) {
  return request({
    url: `${API_PREFIX}/user/batch`,
    method: 'delete',
    data: userIds
  });
}

/**
 * 重置用户密码为123456

 */
export function resetPassword(userId) {
  // 构造重置密码的数据
  const resetData = {
    userId: userId,
    newPassword: '123456'
  };
  
  return request({
    url: `${API_PREFIX}/user/reset-password`,
    method: 'post',
    data: resetData
  });
} 