<template>
  <div class="add-product-container">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="product-form">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入商品标题"></el-input>
      </el-form-item>
      
      <el-form-item label="价格" prop="price">
        <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="0.01" :controls="true" style="width: 100%;"></el-input-number>
      </el-form-item>
      
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择商品分类" style="width: 100%;">
          <el-option v-for="item in categories" :key="item.categoryId" :label="item.name" :value="item.categoryId"></el-option>
        </el-select>
      </el-form-item>
      
      <el-form-item label="有效期" prop="expiryDate">
        <el-date-picker
          v-model="form.expiryDate"
          type="date"
          placeholder="选择有效期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :disabled-date="disabledDate"
          style="width: 100%;"
        ></el-date-picker>
      </el-form-item>
      
      <el-form-item label="图片" prop="image">
        <el-upload
          class="product-image-uploader"
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileChange"
          accept="image/*"
        >
          <img v-if="imageDisplay" :src="imageDisplay" class="product-image" />
          <el-icon v-else class="product-image-uploader-icon"><Plus /></el-icon>
        </el-upload>
        <div class="upload-tip">请上传清晰的商品图片，大小不超过5MB</div>
      </el-form-item>
      
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入商品详细描述"></el-input>
      </el-form-item>
      
      <el-form-item>
        <el-button type="primary" @click="submitForm" :loading="submitting">发布商品</el-button>
        <el-button @click="cancel">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getCategories } from '@/api/category'
import { createProduct, updateProduct } from '@/api/product'
import { getFileUrl } from '@/api/config'
import { uploadProductImage } from '@/api/file'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

// 获取当前日期函数
const getCurrentDate = () => {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 获取7天后的日期作为默认有效期
const getDefaultExpiryDate = () => {
  const date = new Date()
  date.setDate(date.getDate() + 7)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 表单数据
const form = reactive({
  title: '',
  price: 0,
  categoryId: '',
  description: '',
  expiryDate: getDefaultExpiryDate(),
  status: 'ACTIVE'
})

// 禁用今天之前的日期
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7; // 禁用今天之前的日期
}

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入商品标题', trigger: 'blur' },
    { min: 3, max: 50, message: '标题长度应为3至50个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '价格必须大于0', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ],
  expiryDate: [
    { required: true, message: '请选择商品有效期', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入商品描述', trigger: 'blur' },
    { min: 10, max: 1000, message: '描述长度应为10至1000个字符', trigger: 'blur' }
  ]
}

// 文件相关
const imageDisplay = ref('') // 用于显示的临时图片URL
const selectedFile = ref(null) // 选中的文件

// 状态控制
const submitting = ref(false)
const formRef = ref(null)
const categories = ref([])
const emit = defineEmits(['success', 'cancel'])

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res = await getCategories()
    if (res.code === 200 && res.data) {
      categories.value = res.data
    } else {
      throw new Error(res.message || '获取分类列表失败')
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
    ElMessage.error('获取分类列表失败，请稍后重试')
  }
}

// 处理文件选择
const handleFileChange = (file) => {
  if (!file || !file.raw) {
    ElMessage.error('请选择有效的图片文件')
    return
  }
  
  const isImage = /^image\/(jpeg|png|jpg|gif)$/.test(file.raw.type)
  const isLt5M = file.raw.size / 1024 / 1024 < 5
  
  if (!isImage) {
    ElMessage.error('请上传JPG/PNG格式的图片!')
    return
  }
  
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB!')
    return
  }
  
  selectedFile.value = file.raw
  
  // 创建临时URL用于预览
  imageDisplay.value = URL.createObjectURL(file.raw)
}

// 上传图片
const uploadImage = async (itemId) => {
  if (!selectedFile.value || !itemId) return null
  
  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('itemId', itemId.toString()) // 确保itemId是字符串
  
  try {
    const res = await uploadProductImage(formData)
    
    if (res.code === 200 && res.data) {
      return res.data // 返回图片路径
    } else {
      throw new Error(res.message || '图片上传失败')
    }
  } catch (error) {
    console.error('图片上传失败:', error)
    throw error
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    // 先验证表单
    const valid = await formRef.value.validate()
    
    if (!valid) {
      ElMessage.warning('请正确填写所有必填项')
      return
    }
    
    // 检查图片是否已选择
    if (!selectedFile.value) {
      ElMessage.warning('请上传商品图片')
      return
    }
    
    // 检查用户是否已登录
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录后再发布商品')
      return
    }
    
    submitting.value = true
    
    // 显示加载指示器
    const loadingInstance = ElLoading.service({
      lock: true,
      text: '正在创建商品...',
      background: 'rgba(0, 0, 0, 0.7)'
    })
    
    try {
      // 准备商品数据 - 确保字段名与后端Item实体类一致
      const userId = userStore.userInfo.userId
      if (!userId) {
        throw new Error('无法获取用户ID，请重新登录')
      }
      
      const productData = { 
        title: form.title.trim(),
        price: form.price,
        categoryId: form.categoryId,
        description: form.description.trim(),
        validUntil: form.expiryDate,
        status: form.status,
        userId: userId
      }
      
      console.log('提交商品数据:', productData)
      
      // 1. 创建商品
      const createRes = await createProduct(productData)
      
      // 检查返回结果
      if (createRes.code !== 200 || !createRes.data) {
        throw new Error(createRes.message || '创建商品失败')
      }
      
      const createdItem = createRes.data
      const itemId = createdItem.itemId
      
      if (!itemId) {
        throw new Error('创建商品成功但未返回商品ID')
      }
      
      // 2. 上传商品图片
      loadingInstance.setText('正在上传商品图片...')
      const imagePath = await uploadImage(itemId)
      
      // 3. 更新商品信息，添加图片URL
      if (imagePath) {
        loadingInstance.setText('正在更新商品信息...')
        
        // 更新数据需要包含所有必要字段，避免数据库约束错误
        const updateData = {
          itemId: itemId,
          title: form.title.trim(),
          description: form.description.trim(), // 确保包含描述
          price: form.price,
          categoryId: form.categoryId,
          status: form.status,
          validUntil: form.expiryDate,
          itemImageUrl: imagePath
        }
        
        const updateRes = await updateProduct(updateData)
        
        if (updateRes.code !== 200) {
          console.warn('商品图片路径更新失败:', updateRes.message)
          // 继续执行不中断流程，因为商品已创建
        }
      }
      
      ElMessage.success('商品发布成功')
      emit('success', createdItem)
    } catch (error) {
      console.error('发布商品失败:', error)
      ElMessage.error(`发布商品失败: ${error.message || '请稍后重试'}`)
    } finally {
      // 关闭加载指示器
      loadingInstance.close()
      submitting.value = false
      
      // 清理临时URL
      cleanupImageURL()
    }
  } catch (error) {
    console.error('表单验证失败:', error)
    ElMessage.warning('请正确填写所有必填项')
  }
}

// 清理图片临时URL
const cleanupImageURL = () => {
  if (imageDisplay.value && imageDisplay.value.startsWith('blob:')) {
    URL.revokeObjectURL(imageDisplay.value)
    imageDisplay.value = ''
  }
}

// 取消
const cancel = () => {
  cleanupImageURL()
  emit('cancel')
}

// 组件挂载和卸载
onMounted(() => {
  fetchCategories()
})

onUnmounted(() => {
  cleanupImageURL()
})
</script>

<style scoped>
.add-product-container {
  padding: 20px;
}

.product-form {
  max-width: 600px;
  margin: 0 auto;
}

.product-image-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 178px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.product-image-uploader:hover {
  border-color: #409EFF;
}

.product-image-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.product-image {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style> 