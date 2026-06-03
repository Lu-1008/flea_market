<script setup>
  import {ref, computed, onMounted} from 'vue';
  import {useAllDataStore} from "@/stores/index.js";
  import {useRoute, useRouter} from 'vue-router';
  import {ElMessageBox, ElMessage} from 'element-plus';
  import ChangePasswordDialog from './ChangePasswordDialog.vue';
  import { getFileUrl } from '@/api/file';

  const router = useRouter();

  // 修改密码对话框显示状态
  const changePasswordVisible = ref(false);
  
  // 当前用户信息
  const currentUser = ref(null);
  
  // 当前用户头像
  const userAvatar = ref('user-default.png');

  // 获取图片URL
  const getImageUrl = (imageName) => {
    // 如果是默认头像或者为空，从assets获取
    if (!imageName || imageName === 'user-default.png') {
      return new URL(`../assets/images/user-default.png`, import.meta.url).href;
    }
    // 否则从MinIO获取
    return getFileUrl(imageName, true);
  }
  
  // 初始化用户信息和头像
  onMounted(() => {
    // 从sessionStorage获取用户信息
    const userStr = sessionStorage.getItem('user');
    if (userStr) {
      try {
        currentUser.value = JSON.parse(userStr);
        // 使用用户的真实头像
        userAvatar.value = currentUser.value.userImgUrl || 'user-default.png';
        console.log('头部组件获取到用户头像:', userAvatar.value);
      } catch (e) {
        console.error('解析用户信息失败', e);
      }
    }
  });

  const store = useAllDataStore();
  const handleCollapse = ()=>{
    store.state.isCollapse = !store.state.isCollapse;
  }

  const route = useRoute();
  
  // 菜单映射表，用于面包屑显示
  const menuMap = {
    '/home': '首页',
    '/user': '用户管理',
    '/mall': '商品管理',
    '/category': '分类管理',
    '/order': '订单管理',
    '/review': '评价管理'
  };
  
  // 当前路由对应的菜单名称
  const currentMenu = computed(() => {
    return menuMap[route.path] || '首页';
  });

  // 处理下拉菜单命令
  const handleCommand = (command) => {
    if (command === 'logout') {
      handleLogout();
    } else if (command === 'changePassword') {
      // 显示修改密码对话框
      changePasswordVisible.value = true;
    }
  }

  // 修改密码成功回调
  const handlePasswordChangeSuccess = () => {
    ElMessage.success('密码修改成功，请重新登录');
    // 清除登录信息
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    sessionStorage.removeItem('isLogin');
    // 跳转到登录页
    router.push('/login?passwordChanged=true');
  }

  // 退出登录
  const handleLogout = () => {
    ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出登录',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    .then(() => {
      // 清除登录信息
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('user');
      sessionStorage.removeItem('isLogin');
      
      // 提示用户
      ElMessage.success('已退出登录');
      
      // 跳转到登录页
      router.push('/login?logout=true');
    })
    .catch(() => {
      // 用户取消操作，不做任何处理
    });
  }
</script>

<template>
  <div class="header">
    <div class="l-content">
      <el-button size="small" class="el-button" @click="handleCollapse">
        <svg class="icons" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="1em" height="1em">
          <path d="M418.4 497.7H139.7c-42.1 0-76.3-34.2-76.3-76.3V142.6c0-42.1 34.2-76.3 76.3-76.3h278.8c42.1 0 76.3 34.2 76.3 76.3v278.8c-0.1 42.1-34.3 76.3-76.4 76.3zM139.7 112.8c-16.5 0-29.9 13.4-29.9 29.9v278.8c0 16.5 13.4 29.9 29.9 29.9h278.8c16.5 0 29.9-13.4 29.9-29.9V142.6c0-16.5-13.4-29.9-29.9-29.9H139.7zM883 497.7H604.3c-42.1 0-76.3-34.2-76.3-76.3V142.6c0-42.1 34.2-76.3 76.3-76.3H883c42.1 0 76.3 34.2 76.3 76.3v278.8c0 42.1-34.2 76.3-76.3 76.3zM604.3 112.8c-16.5 0-29.9 13.4-29.9 29.9v278.8c0 16.5 13.4 29.9 29.9 29.9H883c16.5 0 29.9-13.4 29.9-29.9V142.6c0-16.5-13.4-29.9-29.9-29.9H604.3zM418.4 962.3H139.7c-42.1 0-76.3-34.2-76.3-76.3V607.2c0-42.1 34.2-76.3 76.3-76.3h278.8c42.1 0 76.3 34.2 76.3 76.3V886c-0.1 42.1-34.3 76.3-76.4 76.3zM139.7 577.4c-16.5 0-29.9 13.4-29.9 29.9V886c0 16.5 13.4 29.9 29.9 29.9h278.8c16.5 0 29.9-13.4 29.9-29.9V607.2c0-16.5-13.4-29.9-29.9-29.9H139.7z" fill="#7D7D7D"></path>
          <path d="M928.1 939.1H559.2c-4.4 0-8-3.6-8-8v-369c0-4.4 3.6-8 8-8h368.9c4.4 0 8 3.6 8 8V931c0 4.5-3.6 8.1-8 8.1z" fill="#CBCBCB"></path>
          <path d="M883 962.3H604.3c-42.1 0-76.3-34.2-76.3-76.3V607.2c0-42.1 34.2-76.3 76.3-76.3H883c42.1 0 76.3 34.2 76.3 76.3V886c0 42.1-34.2 76.3-76.3 76.3zM604.3 577.4c-16.5 0-29.9 13.4-29.9 29.9V886c0 16.5 13.4 29.9 29.9 29.9H883c16.5 0 29.9-13.4 29.9-29.9V607.2c0-16.5-13.4-29.9-29.9-29.9H604.3z" fill="#7D7D7D"></path>
        </svg>
      </el-button>

      <el-breadcrumb separator="/" class="bread">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.path !== '/home'">{{ currentMenu }}</el-breadcrumb-item>
      </el-breadcrumb>

    </div>
    <div class="r-content">
      <el-dropdown @command="handleCommand">
        <span class="el-dropdown-link">
          <img :src="getImageUrl(userAvatar)" class="user"/>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout">退出</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    
    <!-- 修改密码对话框 -->
    <change-password-dialog
      v-model:visible="changePasswordVisible"
      @success="handlePasswordChangeSuccess"
    />
  </div>
</template>

<style scoped lang="less">
  .header{
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    height: 100%;
    background-color: #333;
  }

  .l-content{
    display: flex;
    align-items: center;
    .el-button{
      background-color: #333;
      border-color: #333;
    }
    .icons{
      width: 1.5em;
      height: 1.5em;
    }
  }

  .r-content{
    margin-right: 60px; /* 增加右侧边距，使头像向左移动 */
    .user{
      width: 40px;
      height: 40px;
      border-radius: 50%;
      object-fit: cover;
    }
  }

  :deep(.bread span){
    color:#fff !important;
    cursor: pointer !important;
  }
</style>