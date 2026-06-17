import request from './request'

/**
 * 用户端登录
 */
export function userLogin(loginData) {
  return request({
    url: '/api/auth/user-login',
    method: 'post',
    data: loginData
  })
}

/**
 * 用户注册
 */
export function register(registerData) {
  return request({
    url: '/api/user/register',
    method: 'post',
    data: registerData
  })
}

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return request({
    url: '/api/user/info',
    method: 'get'
  })
}

/**
 * 根据用户ID获取用户信息
 */
export function getUserById(userId) {
  return request({
    url: `/api/user/${userId}`,
    method: 'get'
  })
}

/**
 * 用户登出
 * @returns {Promise} - 返回Promise对象
 */
export function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'post'
  })
}

/**
 * 更新用户信息

 */
export function updateUserInfo(userData) {
  return request({
    url: '/api/user/update',
    method: 'put',
    data: userData
  })
}

/**
 * 修改密码

 */
export function changePassword(passwordData) {
  return request({
    url: '/api/auth/change-password',
    method: 'post',
    data: passwordData
  })
} 