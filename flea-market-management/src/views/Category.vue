<script setup>
import { ref, reactive, onMounted, computed, nextTick, onBeforeUnmount } from 'vue'
import { Search, Edit, Delete, RefreshRight, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryList, createCategory, updateCategory, deleteCategory, getAllCategories, batchDeleteCategories } from '@/api/category'

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
  const otherElementsHeight = 240; // 增加高度预留空间，确保分页组件显示完整
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
  categoryName: '',
  creator: ''
})

// 分类表单对话框
const dialogVisible = ref(false)
const dialogTitle = ref('添加分类')
const formRef = ref(null)
const categoryForm = reactive({
  categoryId: null,
  name: '',
  creator: ''
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 20, message: '分类名称长度为2-20个字符', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value && value.trim() === '') {
          callback(new Error('分类名称不能为空格'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ],
  creator: [
    { required: true, message: '请输入创建人', trigger: 'blur' }
  ]
}

// 是否是编辑模式
const isEdit = ref(false)

// 提交状态
const submitLoading = ref(false)

// 页面初始化
onMounted(() => {
  fetchCategoryList()
  // 获取当前登录用户信息，设置为默认创建人
  const userStr = sessionStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      categoryForm.creator = user.username
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  }
  
  // 添加窗口大小变化事件监听
  window.addEventListener('resize', handleResize);
})

// 组件卸载前移除事件监听
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
});

// 获取分类列表
const fetchCategoryList = async () => {
  loading.value = true
  try {
    const params = {
      categoryName: searchForm.categoryName,
      creator: searchForm.creator,
      page: pageData.currentPage,
      size: pageData.pageSize
    }
    
    // 调用后端API获取分类列表
    const response = await getCategoryList(params)
    
    if (response.data.code === 200) {
      tableData.value = response.data.data.list
      pageData.total = response.data.data.total
    } else {
      ElMessage.error(response.data.message || '获取分类列表失败')
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
    ElMessage.error('获取分类列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 搜索方法
const handleSearch = () => {
  pageData.currentPage = 1
  fetchCategoryList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.categoryName = ''
  searchForm.creator = ''
  pageData.currentPage = 1
  fetchCategoryList()
}

// 分页变化
const handleSizeChange = (size) => {
  pageData.pageSize = size
  fetchCategoryList()
}

const handleCurrentChange = (current) => {
  pageData.currentPage = current
  fetchCategoryList()
}

// 打开添加分类对话框
const handleAdd = () => {
  resetCategoryForm()
  dialogTitle.value = '添加分类'
  isEdit.value = false
  
  // 获取当前登录用户信息，设置为创建人
  const userStr = sessionStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      categoryForm.creator = user.username
    } catch (e) {
      console.error('解析用户信息失败:', e)
    }
  }
  
  dialogVisible.value = true
}

// 打开编辑分类对话框
const handleEdit = (row) => {
  resetCategoryForm()
  dialogTitle.value = '编辑分类'
  isEdit.value = true
  
  // 填充表单数据
  categoryForm.categoryId = row.categoryId
  categoryForm.name = row.name
  categoryForm.creator = row.creator || ''
  
  dialogVisible.value = true
}

// 重置分类表单
const resetCategoryForm = () => {
  categoryForm.categoryId = null
  categoryForm.name = ''
  categoryForm.creator = ''
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交分类表单
const submitCategoryForm = () => {
  if (!formRef.value) return
  
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      const actionType = isEdit.value ? '更新' : '添加'
      
      try {
        if (isEdit.value) {
          // 编辑分类
          const response = await updateCategory(categoryForm)
          if (response.data.code === 200) {
            ElMessage.success('分类更新成功')
            dialogVisible.value = false
            fetchCategoryList()
          } else {
            ElMessage.error(response.data.message || '分类更新失败')
          }
        } else {
          // 添加分类
          const response = await createCategory(categoryForm)
          if (response.data.code === 200) {
            ElMessage.success('分类添加成功')
            dialogVisible.value = false
            fetchCategoryList()
          } else {
            ElMessage.error(response.data.message || '分类添加失败')
          }
        }
      } catch (error) {
        console.error(`${actionType}分类失败:`, error)
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
  resetCategoryForm()
  dialogVisible.value = false
}

// 删除分类
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除分类 "${row.name}" 吗？删除后，该分类下的所有商品将被转移到默认分类"服饰鞋帽"。`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await deleteCategory(row.categoryId)
      if (response.data.code === 200) {
        ElMessage.success('分类删除成功')
        fetchCategoryList()
      } else {
        ElMessage.error(response.data.message || '分类删除失败')
      }
    } catch (error) {
      console.error('删除分类失败:', error)
      if (error.response && error.response.data) {
        ElMessage.error(error.response.data.message || '删除失败，请稍后重试')
      } else {
        ElMessage.error('删除分类失败，请稍后重试')
      }
    }
  }).catch(() => {
    // 取消删除，不做任何操作
  })
}

// 刷新列表
const refreshList = () => {
  loading.value = true
  fetchCategoryList()
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
    `确定要删除选中的 ${multipleSelection.value.length} 条记录吗？删除后，这些分类下的所有商品将被转移到默认分类"服饰鞋帽"。`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    loading.value = true
    try {
      // 获取选中的分类ID列表
      const categoryIds = multipleSelection.value.map(item => item.categoryId)
      
      // 调用批量删除API
      const response = await batchDeleteCategories(categoryIds)
      
      if (response.data.code === 200) {
        const result = response.data.data
        const successCount = result.success.length
        const errorCount = result.error.length
        
        if (successCount > 0) {
          ElMessage.success(`成功删除 ${successCount} 个分类`)
        }
        
        if (errorCount > 0) {
          ElMessage.warning(`${errorCount} 个分类删除失败`)
          console.error('删除失败的分类:', result.error)
        }
        
        // 刷新列表
        fetchCategoryList()
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

// 组件卸载时移除事件监听
const handleResize = () => {
  // 触发表格高度重新计算
  nextTick(() => {
    // 这里不需要做什么，computed会自动重新计算
  });
};
</script>

<template>
  <div class="category-container">
    <el-card class="box-card">
      <!-- 搜索区域 -->
      <div class="search-area">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="分类名称">
            <el-input v-model="searchForm.categoryName" placeholder="请输入分类名称" clearable />
          </el-form-item>
          <el-form-item label="创建人">
            <el-input v-model="searchForm.creator" placeholder="请输入创建人" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="RefreshRight" @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 操作按钮区域 -->
      <div class="operation-area">
        <el-button type="primary" :icon="Plus" @click="handleAdd">添加分类</el-button>
        <el-button type="danger" :icon="Delete" @click="handleBatchDelete" :disabled="multipleSelection.length === 0">批量删除</el-button>
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
      >
        <el-table-column type="selection" width="55" align="center" :resizable="false" />
        <el-table-column type="index" label="序号" width="60" align="center" :resizable="false" />
        <el-table-column prop="name" label="分类名称" align="center" :resizable="false" />
        <el-table-column prop="creator" label="创建人" align="center" :resizable="false" />
        <el-table-column prop="createdAt" label="创建时间" align="center" :resizable="false" />
        <el-table-column label="操作" width="180" align="center" :resizable="false">
          <template #default="scope">
            <el-button type="primary" :icon="Edit" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="danger" :icon="Delete" size="small" @click="handleDelete(scope.row)">删除</el-button>
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
    
    <!-- 分类表单对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="categoryForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        
        <el-form-item label="创建人" prop="creator">
          <el-input v-model="categoryForm.creator" placeholder="请输入创建人" :disabled="isEdit" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false" :disabled="submitLoading">取消</el-button>
          <el-button type="primary" @click="submitCategoryForm" :loading="submitLoading">
            {{ isEdit ? '更新' : '添加' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.category-container {
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
  padding-bottom: 20px; /* 增加底部内边距，确保分页组件有足够空间 */
  min-height: 60px; /* 增加分页区域最小高度 */
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

/* 确保表格占满整个卡片 */
.el-table {
  width: 100% !important;
  table-layout: fixed;
  flex: 1;
}
</style> 