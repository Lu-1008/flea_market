import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // 状态 - 从sessionStorage获取
  const token = ref(sessionStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(sessionStorage.getItem('userInfo') || '{}'))
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value.username || '')
  const userId = computed(() => userInfo.value.userId)
  const avatar = computed(() => userInfo.value.userImgUrl || '')
  
  // 方法 - 使用sessionStorage存储
  function setToken(newToken) {
    token.value = newToken
    sessionStorage.setItem('token', newToken)
  }
  
  function setUserInfo(info) {
    userInfo.value = info
    sessionStorage.setItem('userInfo', JSON.stringify(info))
  }
  
  function login(loginData) {
    setToken(loginData.token)
    setUserInfo(loginData.userInfo)
  }
  
  function logout(showMessage = false) {
    token.value = ''
    userInfo.value = {}
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
    if (showMessage) {
      ElMessage.success('已退出登录')
    }
  }
  
  // 更新用户头像
  function updateUserAvatar(avatarPath) {
    userInfo.value = {
      ...userInfo.value,
      userImgUrl: avatarPath
    }
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }
  
  // 更新用户信息
  function updateUserInfo(info) {
    userInfo.value = {
      ...userInfo.value,
      ...info
    }
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }
  
  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    userId,
    avatar,
    setToken,
    setUserInfo,
    login,
    logout,
    updateUserAvatar,
    updateUserInfo
  }
}) 