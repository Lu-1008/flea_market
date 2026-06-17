<script setup>
import { ref, onMounted, defineEmits, inject, onBeforeUnmount, computed, watchEffect } from 'vue'
import { Search, User, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AuthDialog from './AuthDialog.vue'
import UploadAvatar from './UploadAvatar.vue'
import { getUserInfo, logout } from '@/api/auth'
import { getFileUrl } from '@/api/config'
import { getCartItemCount } from '@/api/cart'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'

const searchKeyword = ref('')
const authDialogRef = ref(null)
const avatarDialogVisible = ref(false)
const emitter = inject('emitter', null)
const userStore = useUserStore()
const router = useRouter()
const cartItemCount = ref(0)

// 定义组件事件
const emit = defineEmits(['search'])

// 搜索方法
const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  console.log('搜索关键词:', searchKeyword.value)
  // 触发搜索事件，传递给父组件
  emit('search', searchKeyword.value.trim())
}

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
  userStore.updateUserAvatar(newAvatarPath)
  
  // 关闭对话框
  avatarDialogVisible.value = false
  
  // 刷新用户信息
  fetchUserInfo()
}

// 处理登录成功
const handleLoginSuccess = (data) => {
  console.log('登录成功返回的数据:', data)
  
  if (!data) {
    console.error('登录成功处理错误: 未接收到用户数据')
    ElMessage.error('登录成功但获取用户信息失败')
    return
  }
  
  // 使用userStore更新登录状态和用户信息
  userStore.setUserInfo(data)
  if (data.token) {
    userStore.setToken(data.token)
  }
  
  // 获取用户信息 - 登录后立即获取完整信息
  fetchUserInfo()
  
  ElMessage.success(`欢迎回来，${data.username || '用户'}！`)
}

// 处理注册成功
const handleRegisterSuccess = (data) => {
  console.log('注册成功:', data)
  ElMessage.success(`注册成功，请登录`)
}

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res = await getUserInfo()
    if (res.code === 200) {
      console.log('获取用户信息成功:', res.data) 
      
      // 确保userImgUrl字段存在
      if (res.data && res.data.userImgUrl) {
        console.log('用户头像URL:', res.data.userImgUrl)
      } else {
        console.warn('用户信息中无头像URL')
      }
      
      // 更新user store中的用户信息
      userStore.setUserInfo(res.data)
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 处理登出
const handleLogout = async () => {
  try {
    // 先清除会话存储，确保即使API失败也能前端登出
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
    
    // 重置状态
    userStore.logout(false) // 不显示消息
    
    // 显示退出登录消息
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
    userStore.logout(false) // 不显示消息
    
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
}

// 获取购物车商品数量
const fetchCartItemCount = async () => {
  if (!userStore.isLoggedIn || !userStore.userId) return
  
  try {
    const count = await getCartItemCount(userStore.userId)
    cartItemCount.value = count
  } catch (error) {
    console.error('获取购物车数量失败:', error)
  }
}

// 更新购物车商品数量（当购物车变化时）
const updateCartCount = () => {
  fetchCartItemCount()
}

// 监听购物车更新事件
const listenForCartEvents = () => {
  if (window.$emitter) {
    window.$emitter.on('update-cart-count', updateCartCount)
  }
}

// 移除事件监听
const removeEventListeners = () => {
  if (emitter) {
    emitter.off('open-login-dialog')
  }
  
  if (window.$emitter) {
    window.$emitter.off('update-cart-count')
  }
}

// 获取头像URL
const getAvatarUrl = (url) => {
  // 添加调试日志
  console.log('获取头像URL:', url)
  
  if (!url || url === '') {
    console.log('头像URL为空，使用默认头像')
    // 返回本地默认头像
    return new URL(`../assets/images/user-default.png`, import.meta.url).href
  }
  
  // 如果是默认头像名
  if (url === 'user-default.png') {
    console.log('使用本地默认头像')
    return new URL(`../assets/images/user-default.png`, import.meta.url).href
  }
  
  // 尝试从MinIO获取，添加noCache=true参数防止缓存
  try {
    console.log('从MinIO获取头像:', url)
    return getFileUrl(url, true, true)
  } catch (error) {
    console.error('获取头像失败，使用默认头像:', error)
    return new URL(`../assets/images/user-default.png`, import.meta.url).href
  }
}

// 组件挂载时检查登录状态
onMounted(() => {
  fetchUserInfo()
  fetchCartItemCount()
  listenForLoginEvents()
  listenForCartEvents()
})

// 组件销毁前移除事件监听
onBeforeUnmount(() => {
  removeEventListeners()
})

// 监听用户头像变化，确保实时更新
watchEffect(() => {
  if (userStore.avatar) {
    console.log('监测到头像变化:', userStore.avatar)
    const avatarUrl = getAvatarUrl(userStore.avatar)
    console.log('处理后的头像URL:', avatarUrl)
  }
})

// 点击个人中心菜单时
const handleUserAction = async () => {
  if (!userStore.isLoggedIn) {
    // 如果没登录，打开登录对话框
    openAuthDialog('login');
    return;
  }
  
  try {
    // 预加载用户信息
    const res = await getUserInfo();
    if (res.code === 200) {
      // 更新用户存储中的信息
      userStore.setUserInfo(res.data);
      // 允许导航继续
      router.push('/user/profile');
    } else {
      throw new Error(res.message || '获取用户信息失败');
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
    if (error.response && error.response.status === 401) {
      ElMessage.error('登录已过期，请重新登录');
      // 清除会话存储的认证信息
      userStore.logout();
      // 打开登录对话框
      openAuthDialog('login');
    }
  }
};
</script>

<template>
  <div class="common-header">
    <!-- 左侧标题 -->
    <div class="header-left">
      <router-link to="/" class="site-title-link">
        <h1 class="site-title">校园跳蚤市场</h1>
      </router-link>
    </div>
    
    <!-- 中间搜索框 -->
    <div class="header-center">
      <div class="search-container">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索商品"
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button class="search-button" @click="handleSearch">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
    
    <!-- 右侧用户信息/登录注册 -->
    <div class="header-right">
      <template v-if="userStore.isLoggedIn">
        <div class="user-center-link" @click="handleUserAction">个人中心</div>
        <el-dropdown trigger="click">
          <div class="avatar-container">
            <el-avatar :src="getAvatarUrl(userStore.avatar)" :size="40" />
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>
                <div class="user-dropdown-info">
                  <el-avatar :src="getAvatarUrl(userStore.avatar)" :size="30" />
                  <span>{{ userStore.username || '用户' }}</span>
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
      :user-id="userStore.userId"
      :initial-avatar="userStore.avatar"
      @update:avatar="handleAvatarUpdate"
    />
  </el-dialog>
</template>

<style scoped>
.common-header {
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 20px;
  background-color: #FF5000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-left {
  flex: 1;
  display: flex;
  align-items: center;
}

.site-title-link {
  text-decoration: none;
  cursor: pointer;
}

.site-title {
  margin: 0;
  font-size: 22px;
  color: white;
  font-weight: bold;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.header-center {
  flex: 2;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 20px;
}

.search-container {
  width: 100%;
  max-width: 500px;
}

.search-input {
  border-radius: 4px;
  overflow: hidden;
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border-radius: 4px 0 0 4px !important;
}

.search-button {
  background-color: #fff;
  border-color: #dcdfe6;
  color: #FF5000;
  font-weight: bold;
  border-radius: 0 4px 4px 0 !important;
}

.search-button:hover {
  background-color: #f2f2f2;
}

.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 15px;
}

.user-center-link {
  color: white;
  font-size: 16px;
  font-weight: bold;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.3s;
}

.user-center-link:hover {
  opacity: 0.8;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.5);
}

.avatar-container {
  cursor: pointer;
  transition: transform 0.2s;
  border: 2px solid #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-container:hover {
  transform: scale(1.05);
}

.login-container {
  display: flex;
  align-items: center;
}

.login-button {
  background-color: transparent;
  border: 2px solid white;
  color: white;
  font-weight: bold;
  font-size: 16px;
  height: 40px;
  padding: 0 20px;
  border-radius: 20px;
  transition: all 0.3s;
}

.login-button:hover {
  background-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.user-dropdown-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 0;
}

.dropdown-link {
  display: block;
  width: 100%;
  color: #606266;
  text-decoration: none;
}

.logout-button {
  width: 100%;
  text-align: left;
  height: auto;
  padding: 0;
}

@media (max-width: 768px) {
  .site-title {
    font-size: 18px;
  }
  
  .header-center {
    padding: 0 10px;
  }
  
  .login-button {
    font-size: 14px;
    padding: 0 15px;
  }
  
  .user-center-link {
    font-size: 14px;
  }
}

@media (max-width: 576px) {
  .site-title {
    font-size: 16px;
  }
  
  .header-left {
    flex: 0 0 auto;
  }
  
  .header-center {
    flex: 1;
  }
  
  .header-right {
    flex: 0 0 auto;
  }
  
  .login-button {
    padding: 0 10px;
  }
  
  .user-center-link {
    font-size: 12px;
  }
}
</style>