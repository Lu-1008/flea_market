<template>
  <div class="upload-avatar-container">
    <el-upload
      class="avatar-uploader"
      action="#"
      :auto-upload="false"
      :show-file-list="false"
      :on-change="handleFileChange"
      accept="image/*"
    >
      <div class="avatar-wrapper">
        <img 
          v-if="avatarUrl" 
          :src="avatarUrl" 
          class="avatar-image" 
          @error="handleAvatarError"
        />
        <div v-else class="avatar-placeholder">
          <el-icon><Plus /></el-icon>
        </div>
      </div>
    </el-upload>
    <div class="tip-text">点击上传头像</div>
    
    <!-- 上传确认对话框 -->
    <el-dialog
      v-model="confirmDialogVisible"
      title="确认上传头像"
      width="400px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="preview-container">
        <img :src="previewUrl" class="preview-image" alt="预览头像" />
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="confirmDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="uploadAvatar" :loading="uploading">确认上传</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, defineEmits, defineProps } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { uploadUserAvatar } from '@/api/file'
import { getFileUrl } from '@/api/config'
import { useUserStore } from '@/store/user'
import { updateUserInfo } from '@/api/auth'

const userStore = useUserStore()
const props = defineProps({
  userId: {
    type: [Number, String],
    required: true
  },
  initialAvatar: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:avatar'])

// 头像URL
const avatarUrl = ref('')
// 预览相关
const confirmDialogVisible = ref(false)
const previewUrl = ref('')
// 上传状态
const uploading = ref(false)
// 选择的文件
const selectedFile = ref(null)

// 初始化头像
if (props.initialAvatar) {
  if (props.initialAvatar === 'user-default.png') {
    // 使用静态资源中的默认头像
    avatarUrl.value = new URL(`../assets/images/user-default.png`, import.meta.url).href
  } else {
    // 通过API获取MinIO中的头像，添加noCache参数防止缓存
    avatarUrl.value = getFileUrl(props.initialAvatar, true, true)
  }
}

// 处理文件选择
const handleFileChange = (file) => {
  if (!file || !file.raw) {
    ElMessage.error('请选择有效的图片文件')
    return
  }
  
  const isImage = file.raw.type.startsWith('image/')
  const isLt2M = file.raw.size / 1024 / 1024 < 2
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return
  }
  
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过2MB')
    return
  }
  
  selectedFile.value = file.raw
  
  // 读取图片并打开预览对话框
  const reader = new FileReader()
  reader.onload = (e) => {
    previewUrl.value = e.target.result
    confirmDialogVisible.value = true
  }
  reader.readAsDataURL(file.raw)
}

// 上传头像
const uploadAvatar = async () => {
  if (!selectedFile.value) {
    ElMessage.error('请先选择图片')
    return
  }
  
  uploading.value = true
  
  // 显示全屏加载
  const loadingInstance = ElLoading.service({
    lock: true,
    text: '上传中...',
    background: 'rgba(0, 0, 0, 0.7)'
  })
  
  try {
    // 创建表单数据
    const formData = new FormData()
    
    // 确保文件对象正确添加
    formData.append('file', selectedFile.value)
    
    // 确保userId是数字格式
    const numericUserId = Number(props.userId)
    if (isNaN(numericUserId) || numericUserId <= 0) {
      console.error('无效的用户ID:', props.userId)
      throw new Error('无效的用户ID')
    }
    
    // 添加用户ID到表单
    formData.append('userId', numericUserId)
    
    // 打印调试信息
    console.log(`准备上传头像: userId=${numericUserId}, 文件名=${selectedFile.value.name}, 大小=${selectedFile.value.size}`)
    
    // 调用上传接口
    const response = await uploadUserAvatar(formData)
    
    // 处理上传成功
    if (response && response.data) {
      // 打印返回数据
      console.log('头像上传成功，服务器返回:', response)
      
      // 更新本地头像URL，添加noCache参数防止缓存
      const newAvatarPath = response.data
      avatarUrl.value = getFileUrl(newAvatarPath, true, true)
      
      // 发送更新事件
      emit('update:avatar', newAvatarPath)
      emit('avatar-updated', newAvatarPath)
      
      // 关闭预览对话框
      confirmDialogVisible.value = false
      
      // 显示成功消息
      ElMessage.success('头像上传成功')
      
      // 直接使用 userStore 更新头像
      userStore.updateUserAvatar(newAvatarPath)
      console.log('已直接更新 Pinia store 中的用户头像:', newAvatarPath)
      
      // 发送全局事件通知其他组件
      if (window.$emitter) {
        window.$emitter.emit('avatar-updated', newAvatarPath)
        console.log('已发送头像更新全局事件')
      }
      
      // 触发自定义事件，通知所有组件更新头像
      window.dispatchEvent(new CustomEvent('user-avatar-updated', { detail: newAvatarPath }))
      console.log('已触发user-avatar-updated自定义事件')
      
      // 也更新本地存储
      updateLocalUserAvatar(newAvatarPath)
      
      // 调用更新用户信息接口，将新头像URL保存到数据库
      try {
        // 获取当前用户信息
        const userInfo = userStore.userInfo
        
        // 调用更新用户API，将新头像URL保存到数据库
        const updateData = {
          userId: numericUserId,
          username: userInfo.username,
          email: userInfo.email,
          phone: userInfo.phone || '',
          address: userInfo.address || '',
          userImgUrl: newAvatarPath
        }
        
        console.log('准备更新用户信息:', updateData)
        
        const updateResult = await updateUserInfo(updateData)
        
        if (updateResult.code === 200) {
          console.log('成功更新用户头像URL到数据库')
        } else {
          console.error('更新用户头像URL到数据库失败:', updateResult.message)
        }
      } catch (updateError) {
        console.error('调用更新用户API失败:', updateError)
      }
    } else {
      console.error('上传响应无效:', response)
      throw new Error('上传失败，服务器响应无效')
    }
  } catch (error) {
    console.error('上传头像失败:', error)
    if (error.response) {
      console.error('错误响应:', {
        status: error.response.status,
        data: error.response.data,
        headers: error.response.headers
      })
    }
    ElMessage.error(`上传头像失败: ${error.message || '请稍后重试'}`)
  } finally {
    uploading.value = false
    loadingInstance.close()
  }
}

// 处理头像加载错误
const handleAvatarError = () => {
  console.warn('头像加载失败，使用默认头像')
  avatarUrl.value = new URL(`../assets/images/user-default.png`, import.meta.url).href
}

// 更新本地存储的用户头像
const updateLocalUserAvatar = (newAvatarPath) => {
  try {
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      const userInfo = JSON.parse(userInfoStr)
      userInfo.userImgUrl = newAvatarPath
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      console.log('已更新本地存储的用户头像:', newAvatarPath)
    }
  } catch (error) {
    console.error('更新本地用户头像失败:', error)
  }
}
</script>

<style scoped>
.upload-avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 10px 0;
}

.avatar-uploader {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.avatar-uploader:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.avatar-wrapper {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24px;
  color: #909399;
  background-color: #f5f7fa;
}

.tip-text {
  margin-top: 10px;
  font-size: 14px;
  color: #909399;
}

.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.preview-image {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}
</style> 