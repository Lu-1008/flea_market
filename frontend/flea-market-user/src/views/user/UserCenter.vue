<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/store/user';
import { ElMessage } from 'element-plus';
import { getFileUrl } from '@/api/config';
import { Document, Goods, User, Box } from '@element-plus/icons-vue';
import UserCenterHeader from '@/components/UserCenterHeader.vue';
import { getUserInfo } from '@/api/auth';

const userStore = useUserStore();
const route = useRoute();
const router = useRouter();
const loadingUserInfo = ref(false);
const checkIntervalId = ref(null);
const userAvatar = ref('');

// 激活的标签页
const activeTab = ref('profile');

// 触发强制更新头像
const forceUpdateAvatar = () => {
  userAvatar.value = userStore.userInfo.userImgUrl || '';
};

// 切换标签页
const handleTabChange = (tab) => {
  activeTab.value = tab;
  router.push(`/user/${tab}`);
};

// 处理搜索
const handleSearch = (keyword) => {
  // 跳转到首页并传递搜索参数
  router.push({
    path: '/',
    query: { keyword }
  });
};

// 处理头像更新事件
const handleAvatarUpdated = (event) => {
  console.log('UserCenter: 收到头像更新事件:', event.detail);
  forceUpdateAvatar();
  setTimeout(() => {
    forceUpdateAvatar(); // 再次更新，确保渲染
  }, 100);
};

// 处理storage事件
const handleStorageChange = (event) => {
  if (event.key === 'userInfo') {
    console.log('UserCenter: 检测到storage变化，更新头像');
    forceUpdateAvatar();
  }
};

// 获取头像URL
const getAvatarUrl = (url) => {
  if (!url) return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png';
  
  // 如果是默认头像
  if (url === 'user-default.png') {
    return new URL(`../../assets/images/user-default.png`, import.meta.url).href;
  }
  
  // 否则从MinIO获取，添加noCache参数防止缓存
  return getFileUrl(url, true, true);
};

// 页面加载时验证登录状态
onMounted(async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录');
    router.push('/');
    return;
  }
  
  // 设置激活的标签页
  const path = route.path;
  if (path.includes('/user/orders')) {
    activeTab.value = 'orders';
  } else if (path.includes('/user/items')) {
    activeTab.value = 'items';
  } else if (path.includes('/user/profile')) {
    activeTab.value = 'profile';
  } else if (path.includes('/user/pending')) {
    activeTab.value = 'pending';
  }
  
  // 加载最新用户信息
  await refreshUserInfo();
  
  // 添加事件监听
  window.addEventListener('user-avatar-updated', handleAvatarUpdated);
  window.addEventListener('storage', handleStorageChange);
  
  // 设置定时更新
  checkIntervalId.value = setInterval(() => {
    forceUpdateAvatar();
  }, 5000);
});

// 组件卸载前清除
onBeforeUnmount(() => {
  if (checkIntervalId.value) {
    clearInterval(checkIntervalId.value);
  }
  
  window.removeEventListener('user-avatar-updated', handleAvatarUpdated);
  window.removeEventListener('storage', handleStorageChange);
});

// 刷新用户信息
const refreshUserInfo = async () => {
  try {
    loadingUserInfo.value = true;
    const res = await getUserInfo();
    if (res && res.code === 200) {
      // 更新用户信息到store
      userStore.setUserInfo(res.data);
      forceUpdateAvatar();
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
  } finally {
    loadingUserInfo.value = false;
  }
};
</script>

<template>
  <div class="user-center-container">
    <UserCenterHeader @search="handleSearch" />
    
    <div class="user-center-header">
      <div class="user-info">
        <el-avatar 
          :size="60" 
          :src="getAvatarUrl(userStore.userInfo.userImgUrl)" 
          :key="userStore.userInfo.userImgUrl" 
        />
        <div class="user-details">
          <h2>{{ userStore.userInfo.username || '用户' }}</h2>
          <p>用户ID: {{ userStore.userInfo.userId }}</p>
        </div>
      </div>
    </div>
    
    <div class="user-center-content">
      <el-menu 
        :default-active="activeTab" 
        class="user-menu" 
        mode="horizontal" 
        @select="handleTabChange"
        background-color="#fff"
        text-color="#333"
        active-text-color="#fff"
      >
        <el-menu-item index="orders">
          <el-icon><document /></el-icon>
          <span>我的订单</span>
        </el-menu-item>
        <el-menu-item index="items">
          <el-icon><goods /></el-icon>
          <span>我的发布</span>
        </el-menu-item>
        <el-menu-item index="profile">
          <el-icon><user /></el-icon>
          <span>个人信息</span>
        </el-menu-item>
        <el-menu-item index="pending">
          <el-icon><box /></el-icon>
          <span>待处理</span>
        </el-menu-item>
      </el-menu>
      
      <div class="tab-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-center-container {
  width: 100%;
  min-height: calc(100vh - 60px);
  background-color: #f5f5f5;
  padding: 0;
  margin: 0;
}

.user-center-header {
  background-color: #fff;
  padding: 15px 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  margin: 0 0 1px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 0;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-details {
  margin-left: 15px;
}

.user-details h2 {
  margin: 0 0 5px 0;
  font-size: 20px;
  color: #333;
  font-weight: 600;
}

.user-details p {
  margin: 0;
  font-size: 14px;
  color: #999;
}

.user-center-content {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  margin: 0;
  border-radius: 0;
  overflow: hidden;
}

.user-menu {
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: flex-start;
  padding-left: 20px;
}

.user-menu :deep(.el-menu-item) {
  font-size: 16px;
  height: 50px;
  line-height: 50px;
  padding: 0 25px;
}

.user-menu :deep(.el-menu-item.is-active) {
  background-color: #ff6a2c !important;
  color: #fff;
  box-shadow: 0 2px 4px rgba(255, 80, 0, 0.2);
}

.tab-content {
  padding: 20px;
}

@media (max-width: 992px) {
  .tab-content {
    padding: 15px;
  }
}

@media (max-width: 768px) {
  .user-center-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 15px 10px;
  }
  
  .user-info {
    flex-direction: column;
    text-align: center;
  }
  
  .user-details {
    margin-left: 0;
    margin-top: 10px;
  }
  
  .user-menu {
    justify-content: space-around;
    padding-left: 0;
  }
  
  .user-menu :deep(.el-menu-item) {
    padding: 0 15px;
    font-size: 14px;
  }
  
  .tab-content {
    padding: 10px;
  }
}
</style> 