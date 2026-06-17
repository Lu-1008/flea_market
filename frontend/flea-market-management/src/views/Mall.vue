<template>
  <div class="mall-container">
    <el-card class="box-card">
      <!-- 搜索区域 -->
      <div class="search-area">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="商品标题">
            <el-input v-model="searchForm.title" placeholder="请输入商品标题" clearable />
          </el-form-item>
          <el-form-item label="分类">
            <!-- 使用原生select但增强样式 -->
            <select 
              v-model="searchForm.categoryId" 
              class="el-input__inner" 
              style="width: 100%; height: 32px; border-radius: 4px; border: 1px solid #DCDFE6; padding: 0 15px; color: #606266; font-size: 14px; appearance: auto; cursor: pointer; background-color: #FFF; outline: none;"
              @change="handleCategoryChange"
            >
              <option value="null">全部</option>
              <option 
                v-for="category in categoryOptions" 
                :key="category.categoryId" 
                :value="category.categoryId"
              >
                {{ category.name }}
              </option>
            </select>
          </el-form-item>
          <el-form-item label="状态">
            <!-- 使用原生select但增强样式 -->
            <select 
              v-model="searchForm.status" 
              class="el-input__inner" 
              style="width: 100%; height: 32px; border-radius: 4px; border: 1px solid #DCDFE6; padding: 0 15px; color: #606266; font-size: 14px; appearance: auto; cursor: pointer; background-color: #FFF; outline: none;"
              @change="handleStatusChange"
            >
              <option value="null">全部</option>
              <option value="ACTIVE">在售</option>
              <option value="SOLD">已售</option>
              <option value="INACTIVE">下架</option>
            </select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="RefreshRight" @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 操作按钮区域 -->
      <div class="operation-area">
        <!-- 管理员可以添加商品 -->
        <el-button v-if="userRole === 'ADMIN'" type="primary" :icon="Plus" @click="handleAdd">添加商品</el-button>
        <!-- 管理员可以批量删除商品 -->
        <el-button v-if="userRole === 'ADMIN'" type="danger" :icon="Delete" @click="handleBatchDelete" :disabled="multipleSelection.length === 0">批量删除</el-button>
        <!-- 分类管理员可以批量强制下架商品 -->
        <el-button 
          v-if="userRole === 'CATEGORY_MANAGER'" 
          type="warning" 
          @click="handleBatchOffShelf" 
          :disabled="multipleSelection.length === 0 || !hasActiveItems">
          批量强制下架
        </el-button>
        <!-- 分类管理员可以批量上架商品 -->
        <el-button 
          v-if="userRole === 'CATEGORY_MANAGER'" 
          type="success" 
          @click="handleBatchActivate" 
          :disabled="multipleSelection.length === 0 || !hasInactiveItems">
          批量上架
        </el-button>
        <!-- 分类管理员可以批量删除商品 -->
        <el-button 
          v-if="userRole === 'CATEGORY_MANAGER'" 
          type="danger" 
          :icon="Delete" 
          @click="handleBatchDelete" 
          :disabled="multipleSelection.length === 0">
          批量删除
        </el-button>
        <el-button :icon="RefreshRight" @click="refreshList" :loading="loading">刷新</el-button>
      </div>

      <!-- 表格区域 -->
      <el-table 
        :data="tableData" 
        style="width: 100%" 
        border 
        stripe
        v-loading="loading"
        :scrollbar-always-on="false"
        :height="tableHeight"
        @selection-change="handleSelectionChange"
        :resizable="false"
        border-fit
      >
        <el-table-column type="selection" width="45" align="center" :resizable="false" />
        <el-table-column type="index" label="序号" width="50" align="center" :resizable="false" />
        <el-table-column label="图片" width="80" align="center" :resizable="false">
          <template #default="scope">
            <el-image 
              v-if="scope.row.itemImageUrl" 
              :src="getImageUrl(scope.row.itemImageUrl)" 
              style="width: 50px; height: 50px; object-fit: cover; cursor: pointer;" 
              fit="cover"
              @error="handleImageLoadError"
              @click="previewImage(scope.row.itemImageUrl)"
            />
            <el-icon v-else style="font-size: 20px;"><picture-filled /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="商品标题" align="center" min-width="180" show-overflow-tooltip :resizable="false" />
        <el-table-column prop="price" label="价格" align="center" min-width="80" :resizable="false">
          <template #default="scope">
            {{ scope.row.price.toFixed(2) }} 元
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" align="center" min-width="80" :resizable="false" />
        <el-table-column prop="username" label="卖家" align="center" min-width="100" :resizable="false" />
        <el-table-column prop="status" label="状态" align="center" min-width="70" :resizable="false">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" align="center" min-width="140" :resizable="false" />
        <el-table-column label="操作" width="240">
          <template #default="scope">
            <div class="operation-buttons">
              <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
              <el-dropdown @command="(status) => updateItemStatusAction(scope.row, status)">
                <el-button size="small" type="info">
                  更改状态
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :disabled="scope.row.status === 'ACTIVE'" command="ACTIVE">在售</el-dropdown-item>
                    <el-dropdown-item :disabled="scope.row.status === 'INACTIVE'" command="INACTIVE">下架</el-dropdown-item>
                    <el-dropdown-item :disabled="scope.row.status === 'SOLD'" command="SOLD">已售</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页区域 -->
      <div class="pagination-area">
        <el-pagination
          v-model:current-page="pageData.currentPage"
          v-model:page-size="pageData.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pageData.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 商品表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="650px"
      :close-on-click-modal="false"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="itemForm"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="商品标题" prop="title">
          <el-input v-model="itemForm.title" placeholder="请输入商品标题" />
        </el-form-item>
        
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="itemForm.description" type="textarea" :rows="4" placeholder="请输入商品描述" />
        </el-form-item>
        
        <el-form-item label="商品价格" prop="price">
          <el-input-number v-model="itemForm.price" :min="0" :precision="2" :step="0.01" style="width: 100%;" />
        </el-form-item>
        
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="itemForm.categoryId" placeholder="请选择分类" style="width: 100%;">
            <el-option 
              v-for="category in categoryOptions" 
              :key="category.categoryId" 
              :label="category.name" 
              :value="category.categoryId" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="商品状态" prop="status">
          <el-select v-model="itemForm.status" placeholder="请选择状态" style="width: 100%;">
            <el-option label="在售" value="ACTIVE" />
            <el-option label="已售" value="SOLD" />
            <el-option label="下架" value="INACTIVE" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="商品图片">
          <div class="item-image-uploader">
            <el-upload
              class="item-image-uploader"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="(file) => beforeImageUpload(file.raw)"
              :on-error="handleImageError"
            >
              <el-image
                v-if="itemForm.itemImageUrl"
                :src="getImageUrl(itemForm.itemImageUrl)"
                class="item-image"
                fit="cover"
                @error="handleImageLoadError"
              />
              <div v-else class="item-image-placeholder">
                <el-icon><plus /></el-icon>
                <div class="el-upload__text">点击上传</div>
              </div>
            </el-upload>
            <div class="image-tip">建议上传正方形图片，大小不超过2MB</div>
          </div>
        </el-form-item>
        
        <el-form-item label="有效期至" prop="validUntil">
          <el-date-picker 
            v-model="itemForm.validUntil" 
            type="datetime" 
            placeholder="选择有效期" 
            style="width: 100%;" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false" :disabled="submitLoading">取消</el-button>
          <el-button type="primary" @click="submitItemForm" :loading="submitLoading">
            {{ isEdit ? '更新' : '添加' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed, nextTick, onBeforeUnmount } from 'vue'
import { Search, Edit, Delete, RefreshRight, Plus, ArrowDown, PictureFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getItemList, createItem, updateItem, deleteItem, updateItemStatus, batchDeleteItems, updateItemImage } from '@/api/item'
import { getAllCategories } from '@/api/category'
import { getFileUrl, uploadItemImage, clearImageCache } from '@/api/file'
import { API_PREFIX } from '@/api/config'

// 表格数据
const tableData = ref([])
const loading = ref(false)
const multipleSelection = ref([])

// 分页数据
const pageData = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 表格高度计算
const tableHeight = computed(() => {
  // 获取视窗高度
  const windowHeight = window.innerHeight;
  // 预留头部导航栏高度
  const headerHeight = 60;
  // 预留容器内其他元素的高度（搜索区域、操作按钮区域、分页区域等）
  const otherElementsHeight = 210; // 增加高度预留空间，确保分页组件显示完整
  // 预留边距
  const padding = 40;
  
  // 计算表格可用高度
  const availableHeight = windowHeight - headerHeight - otherElementsHeight - padding;
  
  // 设置最小高度
  const minHeight = 300;
  // 表格高度为可用高度，但不小于最小高度
  return Math.max(minHeight, availableHeight);
});

// 搜索表单
const searchForm = reactive({
  title: '',
  categoryId: null,
  status: null
})

// 分类选项
const categoryOptions = ref([])

// 商品表单对话框
const dialogVisible = ref(false)
const dialogTitle = ref('添加商品')
const formRef = ref(null)
const itemForm = reactive({
  itemId: null,
  title: '',
  description: '',
  price: 0,
  userId: null,
  categoryId: null,
  status: 'ACTIVE',
  validUntil: null,
  itemImageUrl: null
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入商品标题', trigger: 'blur' },
    { min: 2, max: 50, message: '商品标题长度为2-50个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入商品描述', trigger: 'blur' },
    { min: 10, max: 1000, message: '商品描述长度为10-1000个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' },
    { type: 'number', min: 0, message: '商品价格必须大于等于0', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择商品状态', trigger: 'change' }
  ]
}

// 是否是编辑模式
const isEdit = ref(false)

// 提交状态
const submitLoading = ref(false)

// 用户角色
const userRole = ref(null)

// 上传相关
const uploadHeaders = computed(() => {
  const token = sessionStorage.getItem('token');
  return { Authorization: token ? `Bearer ${token}` : '' };
});

// 计算选中项中是否有活跃商品
const hasActiveItems = computed(() => {
  return multipleSelection.value.some(item => item.status === 'ACTIVE')
})

// 计算选中项中是否有下架商品
const hasInactiveItems = computed(() => {
  return multipleSelection.value.some(item => item.status === 'INACTIVE')
})

// 监听分类ID变化，用于显示分类名称
watch(() => searchForm.categoryId, (newVal) => {
  console.log('分类ID变化:', newVal);
}, { immediate: true });

// 监听状态变化
watch(() => searchForm.status, (newVal) => {
  console.log('状态变化:', newVal);
}, { immediate: true });

// 页面初始化
onMounted(() => {
  fetchItemList()
  fetchCategories()
  
  // 获取当前登录用户信息
  const userStr = sessionStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      itemForm.userId = user.userId
      // 获取用户角色
      userRole.value = user.role
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  }
  
  // 添加窗口大小调整的监听器
  window.addEventListener('resize', handleResize)
})

// 组件卸载前清理
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  cleanupTempUrl()
})

// 获取分类列表
const fetchCategories = async () => {
  try {
    const response = await getAllCategories()
    if (response.data.code === 200) {
      categoryOptions.value = response.data.data
    } else {
      ElMessage.error(response.data.message || '获取分类列表失败')
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
    ElMessage.error('获取分类列表失败，请稍后重试')
  }
}

// 获取商品列表
const fetchItemList = async () => {
  loading.value = true
  try {
    const params = {
      title: searchForm.title,
      categoryId: searchForm.categoryId,
      status: searchForm.status,
      page: pageData.currentPage,
      size: pageData.pageSize
    }
    
    // 调用后端API获取商品列表
    const response = await getItemList(params)
    
    if (response.data.code === 200) {
      tableData.value = response.data.data.list
      pageData.total = response.data.data.total
    } else {
      ElMessage.error(response.data.message || '获取商品列表失败')
    }
  } catch (error) {
    console.error('获取商品列表失败:', error)
    ElMessage.error('获取商品列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 搜索方法
const handleSearch = () => {
  pageData.currentPage = 1
  fetchItemList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.title = ''
  searchForm.categoryId = null
  searchForm.status = null
  pageData.currentPage = 1
  fetchItemList()
}

// 分页变化
const handleSizeChange = (size) => {
  pageData.pageSize = size
  fetchItemList()
}

const handleCurrentChange = (current) => {
  pageData.currentPage = current
  fetchItemList()
}

// 处理分类选择变化
const handleCategoryChange = (event) => {
  console.log('分类选择变化, 新值:', event.target.value)
  // 将字符串"null"转换为null值
  searchForm.categoryId = event.target.value === "null" ? null : Number(event.target.value)
  
  // 分类变化后立即搜索
  fetchItemList()
}

// 处理状态选择变化
const handleStatusChange = (event) => {
  console.log('状态选择变化, 新值:', event.target.value)
  // 将字符串"null"转换为null值
  searchForm.status = event.target.value === "null" ? null : event.target.value
  
  // 状态变化后立即搜索
  fetchItemList()
}

// 打开添加商品对话框
const handleAdd = () => {
  isEdit.value = false
  dialogVisible.value = true
  resetItemForm()
  
  // 使用当前登录管理员的ID作为商品所有者
  const userStr = sessionStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      itemForm.userId = user.userId
      console.log('设置商品所有者ID:', user.userId)
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  } else {
    console.warn('未找到登录用户信息，商品可能无法创建')
  }
}

// 打开编辑商品对话框
const handleEdit = (row) => {
  isEdit.value = true
  dialogVisible.value = true
  resetItemForm()
  
  // 复制行数据到表单
  Object.keys(itemForm).forEach(key => {
    if (key in row) {
      itemForm[key] = row[key]
    }
  })
}

// 重置商品表单
const resetItemForm = () => {
  itemForm.itemId = null
  itemForm.title = ''
  itemForm.description = ''
  itemForm.price = 0
  itemForm.userId = null
  itemForm.categoryId = null
  itemForm.status = 'ACTIVE'
  itemForm.validUntil = null
  itemForm.itemImageUrl = null
  selectedFile.value = null
  
  // 清理临时URL
  cleanupTempUrl()
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 清理临时URL
const cleanupTempUrl = () => {
  if (itemForm.itemImageUrl && itemForm.itemImageUrl.startsWith('blob:')) {
    URL.revokeObjectURL(itemForm.itemImageUrl)
    console.log('已清理临时图片URL')
  }
}

// 提交商品表单
const submitItemForm = () => {
  if (!formRef.value) return
  
  // 确保userId存在
  if (!itemForm.userId) {
    // 如果userId为空，尝试从会话中获取
    const userStr = sessionStorage.getItem('user')
    if (userStr) {
      try {
        const user = JSON.parse(userStr)
        itemForm.userId = user.userId
        console.log('提交前设置商品所有者ID:', user.userId)
      } catch (e) {
        console.error('解析用户信息失败:', e)
        ElMessage.error('无法获取用户信息，请重新登录')
        return
      }
    } else {
      ElMessage.error('未找到用户信息，请重新登录')
      return
    }
  }
  
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      const actionType = isEdit.value ? '更新' : '添加'
      
      try {
        // 检查是否有blob URL (本地预览图片)
        const hasBlobImage = itemForm.itemImageUrl && itemForm.itemImageUrl.startsWith('blob:')
        
        // 创建表单数据的副本，避免直接修改原始表单
        const formData = { ...itemForm }
        
        // 如果是blob URL，临时清除图片URL，避免将blob URL发送到后端
        if (hasBlobImage) {
          console.log('检测到Blob URL，在提交前临时移除')
          formData.itemImageUrl = null
        }
        
        if (isEdit.value) {
          // 编辑商品
          console.log('提交更新商品表单:', formData)
          const response = await updateItem(formData)
          if (response.data.code === 200) {
            const updatedItemId = response.data.data.itemId
            // 如果有blob图片，则上传图片
            if (hasBlobImage && selectedFile.value) {
              console.log('更新商品成功，开始上传图片')
              await handleImageUpload(selectedFile.value, updatedItemId)
            }
            ElMessage.success('商品更新成功')
            dialogVisible.value = false
            fetchItemList()
          } else {
            ElMessage.error(response.data.message || '商品更新失败')
          }
        } else {
          // 添加商品
          console.log('提交创建商品表单:', formData)
          const response = await createItem(formData)
          if (response.data.code === 200) {
            const itemId = response.data.data.itemId
            // 如果有blob图片，则上传图片
            if (hasBlobImage && selectedFile.value && itemId) {
              console.log('创建商品成功，开始上传图片')
              await handleImageUpload(selectedFile.value, itemId)
            }
            ElMessage.success('商品添加成功')
            dialogVisible.value = false
            fetchItemList()
          } else {
            ElMessage.error(response.data.message || '商品添加失败')
          }
        }
      } catch (error) {
        console.error(`${actionType}商品失败:`, error)
        if (error.response && error.response.data) {
          ElMessage.error(error.response.data.message || `${actionType}失败，请稍后重试`)
        } else {
          ElMessage.error(`${actionType}失败，请稍后重试`)
        }
      } finally {
        submitLoading.value = false
      }
    } else {
      ElMessage.warning('请正确填写表单信息')
      return false
    }
  })
}

// 关闭对话框
const handleDialogClose = () => {
  resetItemForm()
  dialogVisible.value = false
}

// 删除商品
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除商品 "${row.title}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await deleteItem(row.itemId)
      if (response.data.code === 200) {
        ElMessage.success('商品删除成功')
        fetchItemList()
      } else {
        ElMessage.error(response.data.message || '商品删除失败')
      }
    } catch (error) {
      console.error('删除商品失败:', error)
      if (error.response && error.response.data) {
        ElMessage.error(error.response.data.message || '删除失败，请稍后重试')
      } else {
        ElMessage.error('删除商品失败，请稍后重试')
      }
    }
  }).catch(() => {
    // 取消删除，不做任何操作
  })
}

// 刷新列表
const refreshList = () => {
  loading.value = true
  fetchItemList()
}

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  multipleSelection.value = selection
}

// 批量删除
const handleBatchDelete = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }
  
  ElMessageBox.confirm(
    `确定要删除选中的 ${multipleSelection.value.length} 条记录吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    loading.value = true
    try {
      // 获取选中的商品ID列表
      const itemIds = multipleSelection.value.map(item => item.itemId)
      
      // 调用批量删除API
      const response = await batchDeleteItems(itemIds)
      
      if (response.data.code === 200) {
        const result = response.data.data
        const successCount = result.success.length
        const errorCount = result.error.length
        
        if (successCount > 0) {
          ElMessage.success(`成功删除 ${successCount} 个商品`)
        }
        
        if (errorCount > 0) {
          ElMessage.warning(`${errorCount} 个商品删除失败`)
          console.error('删除失败的商品:', result.error)
        }
        
        // 刷新列表
        fetchItemList()
      } else {
        ElMessage.error(response.data.message || '批量删除失败')
      }
    } catch (error) {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败，请稍后重试')
    } finally {
      loading.value = false
    }
  }).catch(() => {
    // 用户取消删除，不做任何操作
  })
}

// 更改商品状态（表格操作）
const updateItemStatusAction = async (row, status) => {
  try {
    const response = await updateItemStatus(row.itemId, status)
    if (response.data.code === 200) {
      ElMessage.success(`商品状态已更改为${getStatusText(status)}`)
      fetchItemList()
    } else {
      ElMessage.error(response.data.message || '更改商品状态失败')
    }
  } catch (error) {
    console.error('更改商品状态失败:', error)
    ElMessage.error('更改商品状态失败，请稍后重试')
  }
}

// 分类管理员强制下架商品
const forceOffShelf = (row) => {
  ElMessageBox.confirm(
    `确定要强制下架商品 "${row.title}" 吗？`,
    '强制下架确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await updateItemStatus(row.itemId, 'INACTIVE')
      if (response.data.code === 200) {
        ElMessage.success('商品已强制下架')
        fetchItemList()
      } else {
        ElMessage.error(response.data.message || '强制下架商品失败')
      }
    } catch (error) {
      console.error('强制下架商品失败:', error)
      if (error.response && error.response.data) {
        ElMessage.error(error.response.data.message || '强制下架失败，请稍后重试')
      } else {
        ElMessage.error('强制下架商品失败，请稍后重试')
      }
    }
  }).catch(() => {
    // 取消操作，不做任何处理
  })
}

// 批量强制下架
const handleBatchOffShelf = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }
  
  // 检查选中的商品中是否有在售商品
  const activeItems = multipleSelection.value.filter(item => item.status === 'ACTIVE')
  if (activeItems.length === 0) {
    ElMessage.warning('选中的商品中没有在售状态的商品')
    return
  }
  
  ElMessageBox.confirm(
    `确定要下架选中的 ${activeItems.length} 个在售商品吗？`,
    '批量下架确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    loading.value = true
    try {
      // 并行处理所有下架请求
      const promises = activeItems.map(item => updateItemStatus(item.itemId, 'INACTIVE'))
      const results = await Promise.allSettled(promises)
      
      // 统计成功和失败的数量
      const successCount = results.filter(result => result.status === 'fulfilled' && 
                                         result.value.data && 
                                         result.value.data.code === 200).length
      const errorCount = results.length - successCount
      
      if (successCount > 0) {
        ElMessage.success(`成功下架 ${successCount} 个商品`)
      }
      
      if (errorCount > 0) {
        ElMessage.warning(`${errorCount} 个商品下架失败`)
      }
      
      // 刷新列表
      fetchItemList()
    } catch (error) {
      console.error('批量强制下架失败:', error)
      ElMessage.error('批量强制下架失败，请稍后重试')
    } finally {
      loading.value = false
    }
  }).catch(() => {
    // 用户取消操作，不做任何处理
  })
}

// 分类管理员批量上架商品
const handleBatchActivate = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }
  
  // 检查选中的商品中是否有下架商品
  const inactiveItems = multipleSelection.value.filter(item => item.status === 'INACTIVE')
  if (inactiveItems.length === 0) {
    ElMessage.warning('选中的商品中没有下架状态的商品')
    return
  }
  
  ElMessageBox.confirm(
    `确定要上架选中的 ${inactiveItems.length} 个下架商品吗？`,
    '批量上架确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    loading.value = true
    try {
      // 并行处理所有上架请求
      const promises = inactiveItems.map(item => updateItemStatus(item.itemId, 'ACTIVE'))
      const results = await Promise.allSettled(promises)
      
      // 统计成功和失败的数量
      const successCount = results.filter(result => result.status === 'fulfilled' && 
                                         result.value.data && 
                                         result.value.data.code === 200).length
      const errorCount = results.length - successCount
      
      if (successCount > 0) {
        ElMessage.success(`成功上架 ${successCount} 个商品`)
      }
      
      if (errorCount > 0) {
        ElMessage.warning(`${errorCount} 个商品上架失败`)
      }
      
      // 刷新列表
      fetchItemList()
    } catch (error) {
      console.error('批量上架失败:', error)
      ElMessage.error('批量上架失败，请稍后重试')
    } finally {
      loading.value = false
    }
  }).catch(() => {
    // 用户取消操作，不做任何处理
  })
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'ACTIVE':
      return '在售'
    case 'SOLD':
      return '已售'
    case 'INACTIVE':
      return '下架'
    default:
      return '未知'
  }
}

// 获取状态类型（用于标签颜色）
const getStatusType = (status) => {
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'SOLD':
      return 'info'
    case 'INACTIVE':
      return 'danger'
    default:
      return 'info'
  }
}

// 分类管理员重新上架商品
const reActivateItem = (row) => {
  ElMessageBox.confirm(
    `确定要重新上架商品 "${row.title}" 吗？`,
    '重新上架确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await updateItemStatus(row.itemId, 'ACTIVE')
      if (response.data.code === 200) {
        ElMessage.success('商品已重新上架')
        fetchItemList()
      } else {
        ElMessage.error(response.data.message || '重新上架商品失败')
      }
    } catch (error) {
      console.error('重新上架商品失败:', error)
      if (error.response && error.response.data) {
        ElMessage.error(error.response.data.message || '重新上架失败，请稍后重试')
      } else {
        ElMessage.error('重新上架商品失败，请稍后重试')
      }
    }
  }).catch(() => {
    // 取消操作，不做任何处理
  })
}

// 获取图片URL
const getImageUrl = (url) => {
  // 如果是blob URL，直接返回，这是本地预览
  if (url && url.startsWith('blob:')) {
    return url;
  }
  // 否则通过getFileUrl获取服务器路径
  return getFileUrl(url, true, true); // true表示直接访问MinIO，第三个参数true表示禁用缓存
}

// 处理图片加载错误
const handleImageLoadError = (event) => {
  // 移除默认图片替换，仅输出错误日志
  console.error('商品图片加载失败:', event.target.src);
}

// 图片上传相关函数
const handleImageSuccess = (response, uploadFile) => {
  if (response.code === 200) {
    itemForm.itemImageUrl = response.data;
    ElMessage.success('图片上传成功');
  } else {
    ElMessage.error(response.message || '图片上传失败');
  }
};

const handleImageError = (error) => {
  console.error('图片上传失败:', error);
  ElMessage.error('图片上传失败，请稍后重试');
};

const selectedFile = ref(null)

// 处理图片上传
const handleImageUpload = async (file, itemId) => {
  if (!file) return null
  
  try {
    console.log(`开始上传商品图片: itemId=${itemId}, 文件名=${file.name}`)
    const response = await uploadItemImage(file, itemId)
    
    if (response.code === 200 || (response.data && response.data.code === 200)) {
      // 提取并设置返回的文件路径
      const filePath = response.code === 200 ? response.data : response.data.data
      console.log(`图片上传成功，获取到filePath: ${filePath}`)
      
      // 将图片URL保存到数据库
      console.log(`调用updateItemImage API保存图片URL到数据库: itemId=${itemId}, imageUrl=${filePath}`)
      try {
        const updateResponse = await updateItemImage(itemId, filePath)
        if (updateResponse.data && updateResponse.data.code === 200) {
          console.log('图片URL成功保存到数据库')
        } else {
          console.error('保存图片URL到数据库失败:', updateResponse)
          ElMessage.warning('更新商品图片链接失败，请刷新页面')
        }
      } catch (updateError) {
        console.error('调用更新图片URL API失败:', updateError)
        ElMessage.warning('更新商品图片链接失败，请刷新页面')
      }
      
      // 更新表单中的图片URL
      itemForm.itemImageUrl = filePath
      ElMessage.success('图片上传成功')
      
      // 强制更新表格中对应商品的图片URL
      updateItemImageInTable(itemId, filePath)
      
      return filePath
    } else {
      const errorMsg = (response.data && response.data.message) || response.message || '图片上传失败'
      console.error('上传图片失败:', errorMsg)
      ElMessage.error(errorMsg)
      return null
    }
  } catch (error) {
    console.error('上传过程出现异常:', error)
    ElMessage.error('图片上传失败，请稍后重试')
    return null
  }
}

// 处理图片选择
const beforeImageUpload = (file) => {
  console.log('开始处理文件上传:', file.name, file.type, file.size)
  
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  
  // 保存选中的文件
  selectedFile.value = file
  
  // 创建预览
  itemForm.itemImageUrl = URL.createObjectURL(file)
  
  return false // 阻止自动上传行为
}

// 在表格数据中直接更新商品图片
const updateItemImageInTable = (itemId, newImageUrl) => {
  // 查找表格数据中的对应商品
  const itemIndex = tableData.value.findIndex(item => item.itemId === itemId);
  if (itemIndex !== -1) {
    // 强制更新表格中商品的图片URL并触发视图更新
    const updatedItem = { ...tableData.value[itemIndex], itemImageUrl: newImageUrl };
    tableData.value.splice(itemIndex, 1, updatedItem);
    console.log(`已在表格中更新商品 ${itemId} 的图片: ${newImageUrl}`);
  }
};

// 组件卸载时移除事件监听
const handleResize = () => {
  // 触发表格高度重新计算
  nextTick(() => {
    // 这里不需要做什么，computed会自动重新计算
  });
};

// 自定义图片预览方法
const previewImage = (imageUrl) => {
  if (!imageUrl) return;
  
  // 如果是blob URL，直接使用，否则通过getImageUrl获取
  const displayUrl = imageUrl.startsWith('blob:') ? imageUrl : getImageUrl(imageUrl);
  
  // 创建一个新的Dialog来显示图片
  ElMessageBox.alert(
    `<div style="text-align: center;">
      <img src="${displayUrl}" style="max-width: 100%; max-height: 70vh;" />
    </div>`,
    '图片预览',
    {
      dangerouslyUseHTMLString: true,
      showConfirmButton: true,
      confirmButtonText: '关闭',
      callback: () => {}
    }
  );
};

// 刷新图片缓存
const refreshImage = async (itemId) => {
  if (!itemId) {
    ElMessage.warning('商品ID不能为空');
    return;
  }
  
  const fileKey = `item-${itemId}`;
  
  try {
    ElMessage.info('正在清除图片缓存...');
    const res = await clearImageCache(fileKey);
    if (res.data && res.data.code === 200) {
      ElMessage.success('图片缓存已清除，正在刷新...');
      // 找到对应的商品并更新图片URL，添加时间戳强制刷新
      const itemIndex = tableData.value.findIndex(item => item.itemId === itemId);
      if (itemIndex !== -1) {
        const currentItem = tableData.value[itemIndex];
        // 更新图片URL，添加时间戳
        const newUrl = currentItem.itemImageUrl + '?t=' + Date.now();
        tableData.value[itemIndex] = {
          ...currentItem,
          itemImageUrl: newUrl
        };
      }
    } else {
      ElMessage.error(res.data?.message || '清除缓存失败');
    }
  } catch (error) {
    console.error('清除图片缓存失败:', error);
    ElMessage.error('清除图片缓存失败，请稍后重试');
  }
};
</script>

<style scoped>
.mall-container {
  padding: 20px;
  height: calc(100vh - 60px); /* 减去header高度 */
  overflow: auto;
  display: flex;
  flex-direction: column;
}

.box-card {
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.search-area {
  margin-bottom: 20px;
}

.operation-area {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-start;
}

.pagination-area {
  margin-top: 20px;
  text-align: right;
  padding-bottom: 15px; /* 增加底部内边距，确保分页组件有足够空间 */
  min-height: 50px; /* 确保分页区域有最小高度 */
  display: flex;
  justify-content: flex-end; /* 右对齐 */
  align-items: center; /* 垂直居中 */
}

.operation-buttons {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 5px;
  min-width: 220px;
}

/* 图片上传相关样式 */
.item-image-uploader {
  width: 100%;
  text-align: center;
}

.item-image {
  width: 150px;
  height: 150px;
  display: block;
  object-fit: cover;
  border-radius: 4px;
}

.item-image-placeholder, .no-image-placeholder {
  width: 150px;
  height: 150px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  background-color: #fafafa;
}

.item-image-placeholder:hover {
  border-color: #409EFF;
}

.no-image-placeholder {
  cursor: default;
  color: #909399;
}

.no-image-placeholder .el-icon {
  font-size: 28px;
  color: #8c939d;
  margin-bottom: 8px;
}

.no-image-placeholder p {
  font-size: 12px;
  margin: 5px 0;
  line-height: 1.4;
}

.item-image-placeholder .el-icon {
  font-size: 28px;
  color: #8c939d;
  margin-bottom: 8px;
}

.image-tip {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

/* 确保表格占满整个卡片 */
.el-table {
  width: 100% !important;
  table-layout: fixed;
  flex: 1;
}

/* 优化表格列宽自动分配 */
:deep(.el-table__header),
:deep(.el-table__body) {
  width: 100% !important;
}

:deep(.el-table__header-wrapper),
:deep(.el-table__body-wrapper) {
  width: 100% !important;
}

/* 确保操作列按钮正确显示 */
:deep(.el-table .cell) {
  padding: 0 8px;
  white-space: normal;
  word-break: break-word;
  line-height: 23px;
}

/* 禁用表格列宽调整 */
:deep(.el-table th.is-leaf) {
  cursor: default !important;
}

:deep(.el-table th.is-leaf .cell) {
  overflow: visible !important;
}

:deep(.el-table th.is-leaf .cell > .caret-wrapper) {
  display: none !important;
}

:deep(.el-table th.is-leaf .cell > .el-resizable) {
  display: none !important;
}
</style> 