<template>
  <div class="shopping-cart-page">
    <UserCenterHeader />
    <div class="cart-container" v-if="userInfo">
      <ShoppingCart :user-id="userInfo.userId" />
    </div>
    
    <div v-else class="login-prompt">
      <el-empty description="请登录后查看购物车">
        <el-button type="primary" @click="openLoginDialog">立即登录</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script>
import { ref, inject, onMounted, onBeforeUnmount } from 'vue'
import ShoppingCart from '@/components/ShoppingCart.vue'
import UserCenterHeader from '@/components/UserCenterHeader.vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'ShoppingCartPage',
  components: {
    ShoppingCart,
    UserCenterHeader
  },
  setup() {
    const userInfo = ref(null)
    const emitter = inject('emitter')
    const getUserInfoTimer = ref(null)
    
    // 获取当前登录用户信息
    const getUserInfo = () => {
      try {
        const storedInfo = sessionStorage.getItem('userInfo')
        if (storedInfo) {
          userInfo.value = JSON.parse(storedInfo)
          console.log('获取到用户信息:', userInfo.value)
          
          // 检查用户ID是否有效
          if (!userInfo.value || !userInfo.value.userId) {
            console.error('用户ID无效')
            ElMessage.warning('用户信息不完整，请重新登录')
            userInfo.value = null
          }
        } else {
          console.log('未找到用户信息')
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
        ElMessage.error('获取用户信息失败，请重新登录')
        userInfo.value = null
      }
    }
    
    // 使用防抖处理的getUserInfo
    const debouncedGetUserInfo = () => {
      if (getUserInfoTimer.value) clearTimeout(getUserInfoTimer.value);
      getUserInfoTimer.value = setTimeout(() => {
        getUserInfo();
      }, 300);
    }
    
    // 打开登录对话框
    const openLoginDialog = () => {
      if (emitter) {
        emitter.emit('open-login-dialog')
      }
    }
    
    onMounted(() => {
      debouncedGetUserInfo();
    })
    
    onBeforeUnmount(() => {
      if (getUserInfoTimer.value) {
        clearTimeout(getUserInfoTimer.value);
      }
    })
    
    return {
      userInfo,
      openLoginDialog
    }
  }
}
</script>

<style scoped>
.shopping-cart-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.login-prompt {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}
</style> 