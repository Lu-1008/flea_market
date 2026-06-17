<script setup>
// App组件
console.log('App组件已加载')

// 添加对头像更新的事件监听
// 使用 provide 提供全局事件总线
import { ref, provide, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getUserInfo } from '@/api/auth'
import emitter from './utils/eventBus'

const userStore = useUserStore()

// 提供事件总线
provide('emitter', emitter)

// 全局挂载方便组件间通信
window.$emitter = emitter

// 监听头像更新事件
onMounted(() => {
  // 添加头像更新事件监听
  emitter.on('avatar-updated', async (newAvatarPath) => {
    console.log('接收到头像更新事件:', newAvatarPath)
    
    // 重新获取用户信息以确保头像更新
    try {
      const res = await getUserInfo()
      if (res.code === 200) {
        userStore.setUserInfo(res.data)
        console.log('头像更新后重新获取用户信息成功')
      }
    } catch (error) {
      console.error('重新获取用户信息失败:', error)
    }
  })
})
</script>

<template>
  <div class="app-container">
    <!-- 路由视图 -->
    <router-view />
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  user-select: none; /* 防止文字被选中 */
  -webkit-user-select: none; /* Safari 浏览器 */
  -moz-user-select: none; /* Firefox 浏览器 */
  -ms-user-select: none; /* IE/Edge 浏览器 */
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: #f5f5f5;
}

.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
</style>
