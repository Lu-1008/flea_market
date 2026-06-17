<script setup>
import { ref, onMounted, inject, onBeforeUnmount } from 'vue'
import { User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AuthDialog from './AuthDialog.vue'
import UploadAvatar from './UploadAvatar.vue'
import { getUserInfo, logout } from '@/api/auth'
import { getFileUrl } from '@/api/config'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const isLoggedIn = ref(false)
const authDialogRef = ref(null)
const avatarDialogVisible = ref(false)
const emitter = inject('emitter', null)
const router = useRouter()
const userStore = useUserStore()

// 存储定时器ID
const checkIntervalId = ref(null)

// 用户信息
const userInfo = ref({
  userId: null,
  username: '',
  userImgUrl: ''
})

// 打开登录/注册对话框
const openAuthDialog = (tab = 'login') => {
  authDialogRef.value.open(tab)
}

// 打开头像上传对话框
const openAvatarDialog = () => {
  avatarDialogVisible.value = true
}

// 处理头像更新
const handleAvatarUpdate = (newAvatarPath) => {
  // 更新用户信息
  userInfo.value.userImgUrl = newAvatarPath
  
  // 关闭对话框
  avatarDialogVisible.value = false
  
  // 刷新用户信息
  fetchUserInfo()
}

// 处理登录成功
const handleLoginSuccess = (data) => {
  console.log('登录成功:', data)
  
  if (!data) {
    console.error('登录成功处理错误: 未接收到用户数据')
    ElMessage.error('登录成功但获取用户信息失败')
    return
  }
  
  isLoggedIn.value = true
  
  // 更新用户信息
  userInfo.value = {
    userId: data.userId,
    username: data.username || '用户',
    userImgUrl: data.userImgUrl || ''
  }
  
  // 获取用户信息
  fetchUserInfo()
  
  ElMessage.success(`欢迎回来，${userInfo.value.username}！`)
}

// 处理注册成功
const handleRegisterSuccess = (data) => {
  console.log('注册成功:', data)
  ElMessage.success(`注册成功，请登录`)
}

// 更新用户信息方法
const updateFromStore = () => {
  // 从 store 获取最新的用户信息
  const storeUserInfo = userStore.userInfo
  if (storeUserInfo && storeUserInfo.userImgUrl) {
    console.log('从Store同步用户头像:', storeUserInfo.userImgUrl)
    userInfo.value.userImgUrl = storeUserInfo.userImgUrl
  }
}

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res = await getUserInfo()
    if (res.code === 200) {
      userInfo.value = res.data
      isLoggedIn.value = true
      console.log('已从API获取最新用户信息，头像URL:', res.data.userImgUrl)
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 处理头像更新事件
const handleAvatarUpdated = (newAvatarPath) => {
  console.log('收到头像更新事件:', newAvatarPath)
  userInfo.value.userImgUrl = newAvatarPath || userInfo.value.userImgUrl
  // 强制刷新
  updateFromStore()
}

// 处理自定义事件
const handleCustomAvatarEvent = (event) => {
  console.log('收到user-avatar-updated事件:', event.detail)
  if (event.detail) {
    userInfo.value.userImgUrl = event.detail
  }
  updateFromStore()
}

// 处理登出
const handleLogout = async () => {
  try {
    // 先清除会话存储，确保即使API失败也能前端登出
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
    
    // 重置状态
    isLoggedIn.value = false
    userInfo.value = {
      userId: null,
      username: '',
      userImgUrl: ''
    }
    
    // 显示退出消息
    ElMessage.success('已安全退出登录')
    
    // 尝试调用后端API登出
    try {
      await logout()
      
      // 延迟跳转，确保消息能够显示
      setTimeout(() => {
        router.push('/')
      }, 500)
    } catch (apiError) {
      console.error('后端登出API调用失败:', apiError)
      // API失败但前端已登出，仍然延迟跳转到首页
      setTimeout(() => {
        router.push('/')
      }, 500)
    }
  } catch (error) {
    console.error('登出过程中发生错误:', error)
    
    // 即使发生错误，我们也要确保用户能够登出
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
    isLoggedIn.value = false
    
    ElMessage.warning('登出过程中发生错误，但您已退出登录')
    
    // 仍然跳转到首页
    setTimeout(() => {
      router.push('/')
    }, 500)
  }
}

// 监听登录事件
const listenForLoginEvents = () => {
  if (emitter) {
    emitter.on('open-login-dialog', () => {
      openAuthDialog('login')
    })
  }
  
  // 添加头像更新事件监听
  window.addEventListener('storage', handleStorageChange)
  
  // 监听自定义头像更新事件
  window.addEventListener('user-avatar-updated', handleCustomAvatarEvent)
  
  // 尝试监听全局事件
  if (window.$emitter) {
    window.$emitter.on('avatar-updated', handleAvatarUpdated)
  }
}

// 处理storage事件
const handleStorageChange = (event) => {
  if (event.key === 'userInfo' || event.key === 'token') {
    console.log('检测到storage变化，更新头像')
    updateFromStore()
    fetchUserInfo()
  }
}

// 移除事件监听
const removeEventListeners = () => {
  if (emitter) {
    emitter.off('open-login-dialog')
  }
  
  window.removeEventListener('storage', handleStorageChange)
  
  // 移除自定义事件监听
  window.removeEventListener('user-avatar-updated', handleCustomAvatarEvent)
  
  if (window.$emitter) {
    window.$emitter.off('avatar-updated', handleAvatarUpdated)
  }
  
  // 清除定时器
  if (checkIntervalId.value) {
    clearInterval(checkIntervalId.value)
  }
}

// 组件挂载时检查登录状态
onMounted(() => {
  fetchUserInfo()
  listenForLoginEvents()
  
  // 定期从store同步头像
  checkIntervalId.value = setInterval(() => {
    updateFromStore()
  }, 5000)
})

// 组件销毁前移除事件监听
onBeforeUnmount(() => {
  removeEventListeners()
})

// 获取头像URL
const getAvatarUrl = (url) => {
  if (!url) return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
  
  // 如果是默认头像
  if (url === 'user-default.png') {
    return new URL(`../assets/images/user-default.png`, import.meta.url).href
  }
  
  // 否则从MinIO获取，添加noCache参数防止缓存
  return getFileUrl(url, true, true)
}
</script>

<template>
  <div class="common-header">
    <!-- 左侧标题 -->
    <div class="header-left">
      <router-link to="/" class="site-title-link">
        <h1 class="site-title">校园跳蚤市场</h1>
      </router-link>
    </div>
    
    <!-- 右侧用户信息/登录注册 -->
    <div class="header-right">
      <template v-if="isLoggedIn">
        <router-link to="/" class="user-center-link">首页</router-link>
        <el-dropdown trigger="click">
          <div class="avatar-container">
            <el-avatar :src="getAvatarUrl(userInfo.userImgUrl)" :size="40" />
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>
                <div class="user-dropdown-info">
                  <el-avatar :src="getAvatarUrl(userInfo.userImgUrl)" :size="30" />
                  <span>{{ userInfo.username || '用户' }}</span>
                </div>
              </el-dropdown-item>
              <el-dropdown-item divided>
                <div class="dropdown-link" @click="openAvatarDialog">修改头像</div>
              </el-dropdown-item>
              <el-dropdown-item divided>
                <el-button link type="danger" @click="handleLogout" class="logout-button">退出登录</el-button>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <div class="login-container" v-else>
        <el-button class="login-button" @click="openAuthDialog('login')">登录/注册</el-button>
      </div>
    </div>
  </div>
  
  <!-- 登录/注册对话框 -->
  <auth-dialog 
    ref="authDialogRef"
    @login-success="handleLoginSuccess"
    @register-success="handleRegisterSuccess"
  />
  
  <!-- 修改头像对话框 -->
  <el-dialog
    v-model="avatarDialogVisible"
    title="修改头像"
    width="400px"
    destroy-on-close
  >
    <upload-avatar
      :user-id="userInfo.userId"
      :initial-avatar="userInfo.userImgUrl"
      @avatar-updated="handleAvatarUpdate"
    />
  </el-dialog>
</template>

<style scoped>
.common-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 64px;
  background-color: #FF5000;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  flex: 1;
}

.site-title-link {
  text-decoration: none;
  color: inherit;
}

.site-title {
  margin: 0;
  color: #ffffff;
  font-size: 24px;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-center-link {
  text-decoration: none;
  color: #ffffff;
  font-weight: 500;
  transition: color 0.2s;
}

.avatar-container {
  cursor: pointer;
}

.login-button {
  font-weight: 500;
}

.user-dropdown-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
}

.dropdown-link {
  color: #333;
  cursor: pointer;
  width: 100%;
}

.dropdown-link:hover {
  color: #FF5000;
}

.logout-button {
  width: 100%;
  text-align: center;
  font-weight: 500;
}
</style> 