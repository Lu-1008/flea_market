<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import axios from 'axios'
import { BASE_URL } from '@/api/config'

const router = useRouter()
const route = useRoute()

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ]
}

const formRef = ref(null)
const loading = ref(false)

// 组件挂载时清除可能存在的登录状态
onMounted(() => {
  // 检查是否有明确的退出登录请求
  if (route.query.logout === 'true') {
    // 清除所有登录信息
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('user')
    sessionStorage.removeItem('isLogin')
    sessionStorage.removeItem('loginTime')
    ElMessage.success('已退出登录')
  }
  
  // 检查是否是修改密码后跳转
  if (route.query.passwordChanged === 'true') {
    ElMessage.success('密码修改成功，请重新登录')
  }
  
  // 检查后端服务是否可用
  checkServerStatus()
})

// 检查后端服务状态
const checkServerStatus = async () => {
  try {
    // 尝试请求后端健康检查接口，检查服务是否可用
    await axios.get(`${BASE_URL}/api/health`, { 
      timeout: 5000,
      withCredentials: true 
    })
    console.log('后端服务连接正常')
  } catch (error) {
    console.error('后端服务连接检查失败:', error)
    ElMessage.warning({
      message: '无法连接到后端服务，请确保服务已启动',
      duration: 5000
    })
  }
}

// 登录方法
const handleLogin = () => {
  if (!formRef.value) return
  
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      
      try {
        // 调用后端登录API
        const response = await login({
          username: loginForm.username,
          password: loginForm.password
        })
        
        // 处理响应
        if (response.data.code === 200) {
          const userData = response.data.data
          
          // 检查用户权限
          if (userData.role === 'USER') {
            ElMessage.error('普通用户无权访问管理后台')
            loading.value = false
            return
          }
          
          // 调试信息
          console.log('登录响应数据:', userData)
          
          // 确保userId存在，如果不存在则设置默认值
          let userId = userData.userId
          if (userId === null || userId === undefined) {
            console.warn('用户ID不存在，使用默认ID')
            // 为不同用户设置不同的默认ID
            if (userData.username === 'admin') {
              userId = 1
              console.log('为admin用户设置默认ID: 1')
            } else {
              userId = 100
              console.log('为用户' + userData.username + '设置默认ID: 100')
            }
          }
          
          // 确保userImgUrl字段存在
          const userImgUrl = userData.userImgUrl || 'user-default.png'
          console.log('用户头像URL:', userImgUrl)
          
          // 登录成功，存储token和用户信息到sessionStorage（关闭浏览器后自动清除）
          const userToStore = {
            userId: userId, // 使用处理后的userId
            username: userData.username,
            role: userData.role,
            email: userData.email,
            userImgUrl: userImgUrl // 使用处理后的userImgUrl
          }
          
          console.log('存储到sessionStorage的用户信息:', userToStore)
          
          sessionStorage.setItem('token', userData.token)
          sessionStorage.setItem('user', JSON.stringify(userToStore))
          sessionStorage.setItem('isLogin', 'true')
          // 记录登录时间
          sessionStorage.setItem('loginTime', Date.now().toString())
          
          // 显示成功消息
          ElMessage({
            message: '登录成功',
            type: 'success'
          })
          
          // 如果有重定向地址，则跳转到该地址，否则跳转到首页
          const redirectPath = route.query.redirect || '/home'
          router.push(redirectPath)
        } else {
          ElMessage.error(response.data.message || '登录失败')
        }
      } catch (error) {
        console.error('登录错误:', error)
        // 提供更详细的错误信息显示
        if (error.response && error.response.data) {
          // 服务器返回了错误响应
          ElMessage.error(error.response.data.message || `服务器错误 (${error.response.status})`)
        } else if (error.message) {
          // 网络错误或其他客户端错误
          if (error.message === 'Network Error') {
            ElMessage.error('网络连接失败，请检查服务器是否运行')
          } else if (error.message.includes('timeout')) {
            ElMessage.error('请求超时，请检查网络连接')
          } else {
            ElMessage.error(`请求错误: ${error.message}`)
          }
        } else {
          // 未知错误
          ElMessage.error('登录失败，未知错误')
        }
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<template>
  <div class="login-container" >
    <div class="login-box">
      <div class="login-title">
        <h2>市场后台管理系统</h2>
      </div>
      
      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        label-width="0"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped lang="less">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #2b4b6b;
  
  .login-box {
    width: 400px;
    padding: 40px;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    
    .login-title {
      text-align: center;
      margin-bottom: 30px;
      
      h2 {
        font-size: 24px;
        color: #333;
      }
    }
    
    .login-form {
      .login-button {
        width: 100%;
      }
    }
  }
}
</style> 