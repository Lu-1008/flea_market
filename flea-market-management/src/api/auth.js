import request from './request';
import { API_PREFIX } from './config';

/**
 * 用户登录
 */
export function login(data) {
  return request({
    url: `${API_PREFIX}/auth/login`,
    method: 'post',
    data
  });
}

/**
 * 获取用户信息
 */
export function getUserInfo(userId) {
  return request({
    url: `${API_PREFIX}/user/${userId}`,
    method: 'get'
  });
}

/**
 * 退出登录
 */
export function logout() {
  return request({
    url: `${API_PREFIX}/auth/logout`,
    method: 'post'
  });
}

/**
 * 修改密码

 */
export function changePassword(data) {
  return request({
    url: `${API_PREFIX}/auth/change-password`,
    method: 'post',
    data
  });
} 