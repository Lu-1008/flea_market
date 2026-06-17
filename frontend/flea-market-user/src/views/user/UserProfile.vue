<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElLoading, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/store/user';
import { getFileUrl } from '@/api/config';
import UploadAvatar from '@/components/UploadAvatar.vue';
import { Edit, Lock } from '@element-plus/icons-vue';
import { getUserInfo, updateUserInfo, changePassword } from '@/api/auth';
import { uploadUserAvatar } from '@/api/file';
import { useRouter } from 'vue-router';

const userStore = useUserStore();
const avatarDialogVisible = ref(false);
const formLoading = ref(false);
const formRef = ref(null);
const router = useRouter();

// 用户信息表单
const userForm = reactive({
  userId: '',
  username: '',
  email: '',
  phone: '',
  address: '',
  password: ''
});

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
};

// 修改密码相关
const passwordDialogVisible = ref(false);
const passwordFormRef = ref(null);
const passwordLoading = ref(false);
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 密码表单验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value === passwordForm.oldPassword) {
          callback(new Error('新密码不能与当前密码相同'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: ['blur', 'change'] 
    }
  ]
};

// 获取用户信息
const fetchUserProfile = async () => {
  formLoading.value = true;
  try {
    // 调用真实的API获取用户详细信息
    const res = await getUserInfo();
    
    if (res.code === 200) {
      // 打印用户信息和头像URL
      console.log('获取到的用户信息:', res.data);
      console.log('头像URL:', res.data.userImgUrl);
      console.log('处理后的头像URL:', getAvatarUrl(res.data.userImgUrl));
      
      // 填充表单数据
      const userData = res.data;
      Object.assign(userForm, {
        userId: userData.userId,
        username: userData.username,
        email: userData.email,
        phone: userData.phone || '',
        address: userData.address || ''
      });
    } else {
      ElMessage.error(res.message || '获取用户信息失败');
    }
  } catch (error) {
    console.error('获取用户信息失败:', error);
    ElMessage.error('获取用户信息失败，请稍后重试');
  } finally {
    formLoading.value = false;
  }
};

// 打开头像上传对话框
const openAvatarDialog = () => {
  avatarDialogVisible.value = true;
};

// 处理头像更新
const handleAvatarUpdate = async (newAvatarPath) => {
  console.log('收到头像更新事件，新头像路径:', newAvatarPath);
  
  try {
    // 直接更新用户状态
    userStore.updateUserAvatar(newAvatarPath);
    
    // 关闭对话框
    avatarDialogVisible.value = false;
    
    // 更新用户信息以确保同步
    await fetchUserProfile();
    
    ElMessage.success('头像更新成功');
  } catch (error) {
    console.error('更新头像状态失败:', error);
    ElMessage.error('头像状态更新失败，请刷新页面');
  }
};

// 保存用户信息
const saveUserProfile = async () => {
  if (!formRef.value) return;
  
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请检查表单填写是否正确');
      return;
    }
    
    formLoading.value = true;
    
    try {
      // 保留当前头像URL
      const currentAvatarUrl = userStore.userInfo.userImgUrl;
      console.log('保存信息时保留当前头像URL:', currentAvatarUrl);
      
      // 调用真实的API保存用户信息
      const res = await updateUserInfo({
        userId: userForm.userId,
        username: userForm.username,
        email: userForm.email,
        phone: userForm.phone,
        address: userForm.address,
        userImgUrl: currentAvatarUrl // 保留现有头像URL
      });
      
      if (res.code === 200) {
        ElMessage.success('个人信息已更新');
        // 更新用户存储中的信息，保留头像URL
        userStore.updateUserInfo({ 
          username: userForm.username,
          email: userForm.email,
          phone: userForm.phone,
          address: userForm.address,
          userImgUrl: currentAvatarUrl // 保留现有头像URL
        });
      } else {
        ElMessage.error(res.message || '更新失败');
      }
    } catch (error) {
      console.error('保存用户信息失败:', error);
      ElMessage.error(`保存用户信息失败: ${error.message || '请稍后重试'}`);
    } finally {
      formLoading.value = false;
    }
  });
};

// 获取头像URL
const getAvatarUrl = (url) => {
  if (!url) return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png';
  
  // 如果是默认头像
  if (url === 'user-default.png') {
    return new URL(`../../assets/images/user-default.png`, import.meta.url).href;
  }
  
  // 否则从服务器获取，添加noCache=true防止缓存
  return getFileUrl(url, true, true);
};

// 打开修改密码对话框
const openPasswordDialog = () => {
  // 检查用户信息
  console.log('当前用户信息:', {
    userStoreId: userStore.userId,
    userStoreInfo: userStore.userInfo,
    userFormId: userForm.userId
  });

  // 重置表单
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  
  // 显示对话框
  passwordDialogVisible.value = true;
  
  // 表单验证状态重置
  if (passwordFormRef.value) {
    passwordFormRef.value.resetFields();
  }
};

// 提交修改密码
const handlePasswordChange = async () => {
  if (!passwordFormRef.value) return;
  
  try {
    // 表单验证
    await passwordFormRef.value.validate();
    
    // 显示加载状态
    passwordLoading.value = true;
    
    // 准备请求数据
    const requestData = {
      userId: userForm.userId, // 使用表单中的userId，确保正确
      username: userForm.username, // 添加用户名字段
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    };
    
    console.log('修改密码请求数据:', requestData);
    
    // 调用API修改密码
    const res = await changePassword(requestData);
    
    console.log('修改密码响应:', res);
    
    if (res.code === 200) {
      // 关闭对话框
      passwordDialogVisible.value = false;
      
      // 使用更明确的确认框提示用户
      ElMessageBox.alert(
        '您的密码已修改成功，出于安全原因，您需要使用新密码重新登录。',
        '密码已修改',
        {
          confirmButtonText: '重新登录',
          type: 'success',
          center: true,
          callback: () => {
            // 执行登出操作
            userStore.logout();
            
            // 导航到首页
            router.push('/');
          }
        }
      );
    } else {
      ElMessage.error(res.message || '密码修改失败');
    }
  } catch (error) {
    console.error('修改密码失败:', error);
    console.error('错误详情:', {
      status: error.response?.status,
      headers: error.response?.headers,
      data: error.response?.data
    });
    
    if (error.response && error.response.status === 401) {
      ElMessage.error('原密码错误，请重新输入');
    } else {
      ElMessage.error(error.message || '提交失败，请检查表单填写内容');
    }
  } finally {
    passwordLoading.value = false;
  }
};

onMounted(() => {
  fetchUserProfile();

  // 添加调试信息和重试逻辑
  if (!userStore.userInfo.userImgUrl) {
    console.log('没有找到用户头像，将在2秒后重试获取用户信息');
    setTimeout(() => {
      fetchUserProfile();
    }, 2000);
  } else {
    console.log('用户头像URL:', userStore.userInfo.userImgUrl);
    console.log('处理后的头像URL:', getAvatarUrl(userStore.userInfo.userImgUrl));
  }
});
</script>

<template>
  <div class="user-profile">
    <div class="section-header">
      <h2>个人信息</h2>
    </div>
    
    <el-skeleton :loading="formLoading" animated>
      <template #template>
        <div style="padding: 15px 0; display: flex; flex-direction: column;">
          <div style="display: flex; align-items: center; margin-bottom: 20px;">
            <el-skeleton-item variant="circle" style="width: 80px; height: 80px;" />
            <div style="margin-left: 15px;">
              <el-skeleton-item variant="text" style="width: 150px; margin-bottom: 10px;" />
              <el-skeleton-item variant="text" style="width: 100px;" />
            </div>
          </div>
          <div v-for="i in 5" :key="i" style="margin-bottom: 15px;">
            <el-skeleton-item variant="text" style="width: 30%; margin-bottom: 8px;" />
            <el-skeleton-item variant="text" style="width: 100%;" />
          </div>
        </div>
      </template>
      
      <template #default>
        <div class="profile-content">
          <div class="avatar-section">
            <div class="avatar-container">
              <el-avatar :size="80" :src="getAvatarUrl(userStore.userInfo.userImgUrl)" />
              <div class="avatar-overlay" @click="openAvatarDialog">
                <el-icon><Edit /></el-icon>
                <span>修改头像</span>
              </div>
            </div>
            <div class="user-id">
              <h3>{{ userForm.username }}</h3>
              <p>用户ID: {{ userForm.userId }}</p>
            </div>
          </div>
          
          <el-form 
            ref="formRef"
            :model="userForm" 
            :rules="rules" 
            label-width="80px" 
            label-position="left"
            class="profile-form"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="userForm.username" />
            </el-form-item>
            
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" type="email" />
            </el-form-item>
            
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="userForm.phone" />
            </el-form-item>
            
            <el-form-item label="宿舍地址">
              <el-input v-model="userForm.address" />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="saveUserProfile" :loading="formLoading">保存信息</el-button>
              <el-button type="warning" @click="openPasswordDialog" class="password-btn">
                <el-icon><Lock /></el-icon>
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>
    </el-skeleton>
    
    <!-- 修改头像对话框 -->
    <el-dialog
      v-model="avatarDialogVisible"
      title="修改头像"
      width="400px"
      destroy-on-close
    >
      <upload-avatar
        :user-id="userStore.userInfo.userId"
        :initial-avatar="userStore.userInfo.userImgUrl"
        @update:avatar="handleAvatarUpdate"
      />
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="400px"
      destroy-on-close
      center
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="80px"
        label-position="left"
        class="password-form"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
        <div class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handlePasswordChange" :loading="passwordLoading">确认修改</el-button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-profile {
  width: 100%;
}

.section-header {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.section-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.profile-content {
  width: 100%;
}

.avatar-section {
  display: flex;
  align-items: center;
  margin-bottom: 25px;
  width: 100%;
}

.avatar-container {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid #f2f2f2;
  width: 90px;
  height: 90px;
}

.avatar-container :deep(.el-avatar) {
  width: 100%;
  height: 100%;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-overlay:hover {
  opacity: 1;
}

.avatar-overlay span {
  margin-top: 5px;
  font-size: 12px;
}

.avatar-overlay :deep(.el-icon) {
  font-size: 20px;
}

.user-id {
  margin-left: 20px;
}

.user-id h3 {
  margin: 0 0 5px 0;
  font-size: 20px;
  color: #333;
}

.user-id p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.profile-form {
  max-width: 100%;
  width: 100%;
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.profile-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #555;
}

.profile-form :deep(.el-input) {
  max-width: 550px;
  width: 100%;
}

.password-btn {
  margin-left: 15px;
}

.password-btn .el-icon {
  margin-right: 5px;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.password-form :deep(.el-input) {
  width: 100%;
}

@media (max-width: 768px) {
  .avatar-section {
    flex-direction: column;
    align-items: center;
    margin-bottom: 20px;
  }
  
  .user-id {
    margin-left: 0;
    margin-top: 10px;
    text-align: center;
  }

  .profile-form :deep(.el-input) {
    max-width: 100%;
  }
}
</style> 