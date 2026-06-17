<script setup>
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { changePassword } from '@/api/auth';
import { useRouter } from 'vue-router';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:visible', 'success']);

const router = useRouter();

// 表单数据
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 表单引用
const formRef = ref(null);

// 加载状态
const loading = ref(false);

// 表单验证规则
const rules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

// 关闭对话框
const handleClose = () => {
  // 重置表单
  if (formRef.value) {
    formRef.value.resetFields();
  }
  emit('update:visible', false);
};

// 提交表单
const handleSubmit = () => {
  if (!formRef.value) return;

  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        // 从sessionStorage获取用户信息
        const userStr = sessionStorage.getItem('user');
        console.log('从sessionStorage获取的用户字符串:', userStr);
        
        if (!userStr) {
          ElMessage.error('用户信息不存在，请重新登录');
          // 跳转到登录页
          router.push('/login');
          return;
        }
        
        // 使用sessionStorage中的用户信息
        const user = JSON.parse(userStr);
        console.log('从sessionStorage解析的用户信息:', user);
        
        // 确保userId存在，如果不存在则使用默认值
        let userId = user.userId;
        if (userId === null || userId === undefined) {
          if (user.username === 'admin') {
            userId = 1;
            console.warn('会话存储中用户ID不存在，为admin用户使用默认ID: 1');
          } else {
            userId = 100;
            console.warn('会话存储中用户ID不存在，为用户' + user.username + '使用默认ID: 100');
          }
        }
        
        // 构建请求参数
        const requestData = {
          userId: Number(userId), // 确保userId是数字类型
          oldPassword: form.oldPassword,
          newPassword: form.newPassword
        };
        
        console.log('提交的数据:', requestData);
        
        // 调用修改密码API
        const response = await changePassword(requestData);
        console.log('API响应:', response);
        
        if (response.data.code === 200) {
          handlePasswordSuccess();
        } else {
          ElMessage.error(response.data.message || '密码修改失败');
        }
      } catch (error) {
        console.error('修改密码失败:', error);
        if (error.response) {
          console.error('错误响应数据:', error.response.data);
          console.error('错误状态码:', error.response.status);
        }
        ElMessage.error(error.response?.data?.message || '网络错误，请稍后重试');
      } finally {
        loading.value = false;
      }
    }
  });
};

// 密码修改成功后的处理
const handlePasswordSuccess = () => {
  // 通知父组件密码修改成功
  emit('success');
  // 关闭对话框
  handleClose();
  
  // 清除登录信息，相当于注销
  setTimeout(() => {
    // 清除登录信息（只使用sessionStorage）
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    sessionStorage.removeItem('isLogin');
    sessionStorage.removeItem('loginTime');
    
    // 跳转到登录页，添加提示参数
    router.push('/login?passwordChanged=true');
  }, 500); // 延迟500毫秒，确保对话框已关闭
}
</script>

<template>
  <el-dialog
    title="修改密码"
    v-model="props.visible"
    width="400px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="当前密码" prop="oldPassword">
        <el-input
          v-model="form.oldPassword"
          type="password"
          placeholder="请输入当前密码"
          show-password
        />
      </el-form-item>
      
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          placeholder="请输入新密码"
          show-password
        />
      </el-form-item>
      
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          placeholder="请再次输入新密码"
          show-password
        />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取 消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="loading">确 定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
/* 没有特殊样式需求 */
</style> 