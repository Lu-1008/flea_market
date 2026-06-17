<template>
  <el-dialog
    v-model="dialogVisible"
    :title="activeTab === 'login' ? '用户登录' : '用户注册'"
    width="400px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-tabs v-model="activeTab" class="auth-tabs">
      <el-tab-pane label="登录" name="login">
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-width="80px"
          status-icon
        >
          <el-form-item label="用户名" prop="username">
            <el-input 
              v-model="loginForm.username" 
              placeholder="请输入用户名"
              prefix-icon="User"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
        </el-form>
        <div class="form-actions">
          <el-button 
            type="primary" 
            :loading="loginLoading" 
            @click="handleLogin" 
            round 
            style="width: 100%"
          >
            登录
          </el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="注册" name="register">
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          label-width="80px"
          status-icon
        >
          <el-form-item label="用户名" prop="username">
            <el-input 
              v-model="registerForm.username" 
              placeholder="请输入3-20位用户名"
              prefix-icon="User"
            />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input 
              v-model="registerForm.email" 
              placeholder="请输入邮箱"
              prefix-icon="Message"
            />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input 
              v-model="registerForm.phone" 
              placeholder="请输入手机号"
              prefix-icon="Phone"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input 
              v-model="registerForm.password" 
              type="password" 
              placeholder="请输入6-20位密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input 
              v-model="registerForm.confirmPassword" 
              type="password" 
              placeholder="请再次输入密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
        </el-form>
        <div class="form-actions">
          <el-button 
            type="primary" 
            :loading="registerLoading" 
            @click="handleRegister" 
            round 
            style="width: 100%"
          >
            注册
          </el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, defineExpose, defineEmits } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { User, Lock, Message, Phone } from '@element-plus/icons-vue'
import { userLogin, register } from '@/api/auth'

const emit = defineEmits(['login-success', 'register-success', 'update:visible'])

// 对话框可见性
const dialogVisible = ref(false)

// 当前激活的标签
const activeTab = ref('login')

// 登录表单
const loginFormRef = ref(null)
const loginForm = reactive({
  username: '',
  password: ''
})

// 注册表单
const registerFormRef = ref(null)
const registerForm = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: ''
})

// 加载状态
const loginLoading = ref(false)
const registerLoading = ref(false)

// 登录表单验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// 注册表单验证规则
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 处理登录请求失败
const handleLoginError = (error) => {
  console.error('登录失败:', error)
  
  if (error.response) {
    const { status, data } = error.response
    
    if (status === 401) {
      ElMessage.error('用户名或密码错误')
    } else if (status === 403) {
      ElMessage.error('您的账号无权访问')
    } else if (data && data.message) {
      ElMessage.error(data.message)
    } else {
      ElMessage.error('登录失败，请稍后再试')
    }
  } else if (error.message && error.message.includes('timeout')) {
    ElMessage.error('登录请求超时，请检查网络连接')
  } else {
    ElMessage.error('登录失败，请检查用户名和密码')
  }
}

// 处理注册请求失败
const handleRegisterError = (error) => {
  console.error('注册失败:', error)
  
  if (error.response) {
    const { status, data } = error.response
    
    if (status === 400) {
      if (data.message && data.message.includes('用户名已存在')) {
        ElMessage.error('用户名已被注册，请更换用户名')
      } else if (data.message && data.message.includes('邮箱已被使用')) {
        ElMessage.error('邮箱已被注册，请更换邮箱')
      } else if (data.message) {
        ElMessage.error(data.message)
      } else {
        ElMessage.error('注册信息有误，请检查后重试')
      }
    } else {
      ElMessage.error(data?.message || '注册失败，请稍后再试')
    }
  } else if (error.message && error.message.includes('timeout')) {
    ElMessage.error('注册请求超时，请检查网络连接')
  } else {
    ElMessage.error('注册失败，请稍后再试')
  }
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loginLoading.value = true
      
      // 显示全屏加载
      const loadingInstance = ElLoading.service({
        lock: true,
        text: '登录中...',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      
      try {
        // 调用用户端登录接口
        const response = await userLogin(loginForm)
        
        // 处理登录成功
        // 检查响应结构，确保有数据
        console.log('登录响应:', response)
        
        if (!response || !response.data) {
          throw new Error('登录响应数据为空')
        }
        
        const userData = response.data
        const token = userData.token
        
        // 保存token和用户信息到会话存储
        sessionStorage.setItem('token', token)
        sessionStorage.setItem('userInfo', JSON.stringify(userData))
        
        // 登录成功，发出事件
        emit('login-success', userData)
        
        // 关闭对话框
        dialogVisible.value = false
        
        // 显示成功消息
        ElMessage.success('登录成功')
      } catch (error) {
        handleLoginError(error)
      } finally {
        loginLoading.value = false
        loadingInstance.close() // 关闭加载动画
      }
    }
  })
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      registerLoading.value = true
      
      // 显示全屏加载
      const loadingInstance = ElLoading.service({
        lock: true,
        text: '注册中...',
        background: 'rgba(0, 0, 0, 0.7)'
      })
      
      try {
        // 准备注册数据，去除确认密码
        const registerData = {
          username: registerForm.username,
          password: registerForm.password,
          email: registerForm.email,
          phone: registerForm.phone || ''  // 确保空字符串而非undefined
        }
        
        // 调用注册接口
        const response = await register(registerData)
        
        // 注册成功，发出事件
        emit('register-success', response.data)
        
        // 切换到登录标签
        activeTab.value = 'login'
        
        // 填充登录表单
        loginForm.username = registerForm.username
        loginForm.password = ''
        
        // 显示成功消息
        ElMessage.success('注册成功，请登录')
        
        // 重置注册表单
        registerFormRef.value.resetFields()
      } catch (error) {
        handleRegisterError(error)
      } finally {
        registerLoading.value = false
        loadingInstance.close() // 关闭加载动画
      }
    }
  })
}

// 打开对话框
const open = (tab = 'login') => {
  activeTab.value = tab
  dialogVisible.value = true
}

// 关闭对话框
const close = () => {
  dialogVisible.value = false
}

// 暴露方法给父组件
defineExpose({
  open,
  close
})

// 监听对话框可见性变化
const updateVisible = (value) => {
  emit('update:visible', value)
}
</script>

<style scoped>
.auth-tabs {
  width: 100%;
}

.form-actions {
  margin-top: 20px;
}

:deep(.el-tabs__item) {
  font-size: 16px;
  padding: 0 15px;
}

:deep(.el-tabs__active-bar) {
  background-color: #FF5000;
}

:deep(.el-tabs__item.is-active) {
  color: #FF5000;
}

:deep(.el-form-item__content) {
  display: flex;
  align-items: center;
}

:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #FF5000 inset !important;
}

:deep(.el-button--primary) {
  background-color: #FF5000;
  border-color: #FF5000;
}

:deep(.el-button--primary:hover),
:deep(.el-button--primary:focus) {
  background-color: #FF6A2C;
  border-color: #FF6A2C;
}

:deep(.el-button--primary:active) {
  background-color: #E04600;
  border-color: #E04600;
}

:deep(.el-button.is-link) {
  color: #FF5000;
}

:deep(.el-button.is-link:hover) {
  color: #FF6A2C;
}
</style> 