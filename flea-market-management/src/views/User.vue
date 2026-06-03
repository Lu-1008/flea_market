<script setup>
import { ref, reactive, onMounted, watch, computed, nextTick, onBeforeUnmount } from 'vue'
import { Search, Edit, Delete, RefreshRight, Plus, UserFilled, PictureFilled, Key } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser, getAllRoles, batchDeleteUsers, resetPassword } from '@/api/user'
import { getFileUrl, uploadUserAvatar } from '@/api/file'

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
  username: '',
  role: ''
})

// 角色选项
const roleOptions = ref([
  { value: '', label: '全部' },
  { value: 'ADMIN', label: '管理员' },
  { value: 'CATEGORY_MANAGER', label: '分类管理员' },
  { value: 'USER', label: '普通用户' }
])

// 用户表单对话框
const dialogVisible = ref(false)
const dialogTitle = ref('添加用户')
const formRef = ref(null)
const userForm = reactive({
  userId: null,
  username: '',
  password: '',
  email: '',
  phone: '',
  role: 'USER',
  userImgUrl: '',
  averageRating: null
})

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 是否是编辑模式
const isEdit = ref(false)

// 提交状态
const submitLoading = ref(false)

// 上传相关
const uploadHeaders = computed(() => {
  const token = sessionStorage.getItem('token');
  return { Authorization: token ? `Bearer ${token}` : '' };
});

// 获取图片URL
const getImageUrl = (url) => {
  return getFileUrl(url, true, true); // true表示直接访问MinIO，第三个参数true表示禁用缓存
}

// 处理图片加载错误
const handleImageLoadError = (event) => {
  // 移除默认图片替换，仅输出错误日志
  console.error('用户头像加载失败:', event.target.src);
}

// 根据角色值获取角色标签
const getRoleLabelByValue = (value) => {
  if (!value) return '';
  const option = roleOptions.value.find(item => item.value === value);
  return option ? option.label : formatRoleName(value);
}

// 页面初始化
onMounted(() => {
  fetchUserList()
  fetchRoles()
  
  // 添加窗口大小变化事件监听
  window.addEventListener('resize', handleResize);
})

// 组件卸载前移除事件监听
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
});

// 获取角色列表
const fetchRoles = async () => {
  try {
    const res = await getAllRoles()
    if (res.data.code === 200) {
      const roles = res.data.data
      // 保存当前选择的值
      const currentValue = searchForm.role
      
      // 更新角色选项
      roleOptions.value = [
        { value: '', label: '全部' },
        ...roles.map(role => ({ value: role, label: formatRoleName(role) }))
      ]
      
      // 如果当前值不为空但在新选项中不存在，则重置为空
      if (currentValue && currentValue !== '' && roles.indexOf(currentValue) === -1) {
        console.log(`当前选择的角色 ${currentValue} 在后端返回的角色列表中不存在，重置为空`)
        searchForm.role = ''
      }
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
}

// 格式化角色名称
const formatRoleName = (role) => {
  if (!role) return '';
  
  switch (role) {
    case 'ADMIN':
      return '管理员'
    case 'CATEGORY_MANAGER':
      return '分类管理员'
    case 'USER':
      return '普通用户'
    default:
      return role
  }
}

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const params = {
      username: searchForm.username,
      role: searchForm.role,
      page: pageData.currentPage,
      size: pageData.pageSize
    }
    
    console.log('开始获取用户列表，参数:', params);
    
    // 调用后端API获取用户列表
    const response = await getUserList(params);
    console.log('获取用户列表响应:', response);
    
    if (response.data.code === 200) {
      tableData.value = response.data.data.list;
      pageData.total = response.data.data.total;
      loading.value = false;
      console.log('用户列表数据:', tableData.value);
    } else {
      console.error('获取用户列表响应错误:', response.data);
      ElMessage.error(response.data.message || '获取用户列表失败');
      loading.value = false;
    }
  } catch (error) {
    console.error('获取用户列表异常详情:', error);
    if (error.response) {
      console.error('错误响应状态:', error.response.status);
      console.error('错误响应数据:', error.response.data);
    } else if (error.request) {
      console.error('未收到响应，请求详情:', error.request);
    } else {
      console.error('请求配置错误:', error.message);
    }
    console.error('错误配置:', error.config);
    
    ElMessage.error('获取用户列表失败，请稍后重试');
    loading.value = false;
  }
}

// 搜索方法
const handleSearch = () => {
  pageData.currentPage = 1
  fetchUserList()
}

// 重置搜索
const resetSearch = () => {
  // 先清空表单
  searchForm.username = ''
  searchForm.role = ''
  
  // 重置分页和获取数据
  pageData.currentPage = 1
  fetchUserList()
}

// 分页变化
const handleSizeChange = (size) => {
  pageData.pageSize = size
  fetchUserList()
}

const handleCurrentChange = (current) => {
  pageData.currentPage = current
  fetchUserList()
}

// 打开添加用户对话框
const handleAdd = () => {
  resetUserForm()
  dialogTitle.value = '添加用户'
  isEdit.value = false
  dialogVisible.value = true
}

// 打开编辑用户对话框
const handleEdit = (row) => {
  resetUserForm()
  dialogTitle.value = '编辑用户'
  isEdit.value = true
  
  // 填充表单数据
  userForm.userId = row.userId
  userForm.username = row.username
  userForm.email = row.email
  userForm.phone = row.phone
  userForm.role = row.role
  userForm.userImgUrl = row.userImgUrl || 'user-default.png'
  userForm.averageRating = row.averageRating
  
  dialogVisible.value = true
}

// 重置用户表单
const resetUserForm = () => {
  userForm.userId = null
  userForm.username = ''
  userForm.password = ''
  userForm.email = ''
  userForm.phone = ''
  userForm.role = 'USER'
  userForm.userImgUrl = ''
  userForm.averageRating = null
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交用户表单
const submitUserForm = () => {
  if (!formRef.value) return
  
  formRef.value.validate(async (valid) => {
    if (valid) {
      // 添加提交按钮加载状态
      submitLoading.value = true
      const actionType = isEdit.value ? '更新' : '添加'
      
      try {
        if (isEdit.value) {
          // 编辑用户
          const response = await updateUser(userForm)
          if (response.data.code === 200) {
            ElMessage.success('用户更新成功')
            dialogVisible.value = false
            fetchUserList()
          } else {
            ElMessage.error(response.data.message || '用户更新失败')
          }
        } else {
          // 添加用户
          const response = await createUser(userForm)
          if (response.data.code === 200) {
            ElMessage.success('用户添加成功')
            dialogVisible.value = false
            fetchUserList()
          } else {
            ElMessage.error(response.data.message || '用户添加失败')
          }
        }
      } catch (error) {
        console.error(`${actionType}用户失败:`, error)
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
  resetUserForm()
  dialogVisible.value = false
}

// 删除用户
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除用户 "${row.username}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await deleteUser(row.userId)
      if (response.data.code === 200) {
        ElMessage.success('用户删除成功')
        fetchUserList()
      } else {
        ElMessage.error(response.data.message || '用户删除失败')
      }
    } catch (error) {
      console.error('删除用户失败:', error)
      ElMessage.error('删除用户失败，请稍后重试')
    }
  }).catch(() => {
    // 取消删除，不做任何操作
  })
}

// 批量删除用户
const handleBatchDelete = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }
  
  ElMessageBox.confirm(
    `确定要删除选中的 ${multipleSelection.value.length} 个用户吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    loading.value = true
    try {
      // 获取选中的用户ID列表
      const userIds = multipleSelection.value.map(item => item.userId)
      
      // 调用批量删除API
      const response = await batchDeleteUsers(userIds)
      
      if (response.data.code === 200) {
        const result = response.data.data
        const successCount = result.success.length
        const errorCount = result.error.length
        
        if (successCount > 0) {
          ElMessage.success(`成功删除 ${successCount} 个用户`)
        }
        
        if (errorCount > 0) {
          ElMessage.warning(`${errorCount} 个用户删除失败`)
          console.error('删除失败的用户:', result.error)
        }
        
        // 刷新列表
        fetchUserList()
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

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  multipleSelection.value = selection
}

// 刷新列表
const refreshList = () => {
  loading.value = true
  fetchUserList()
}

// 监听角色选择的变化
watch(() => searchForm.role, (newVal, oldVal) => {
  console.log('角色选择变化:', {
    新值: newVal,
    旧值: oldVal,
    显示文本: getRoleLabelByValue(newVal)
  })
}, { immediate: true })

// 头像上传相关函数
const handleAvatarSuccess = (response, uploadFile) => {
  if (response.code === 200) {
    userForm.userImgUrl = response.data;
    ElMessage.success('头像上传成功');
  } else {
    ElMessage.error(response.message || '头像上传失败');
  }
};

const handleAvatarError = (error) => {
  console.error('头像上传失败:', error);
  ElMessage.error('头像上传失败，请稍后重试');
};

const beforeAvatarUpload = (file) => {
  console.log('开始处理用户头像上传:', file.name, file.type, file.size);
  
  const isImage = file.type.startsWith('image/');
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isImage) {
    ElMessage.error('只能上传图片文件!');
    return false;
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!');
    return false;
  }
  
  // 只在编辑模式且有用户ID时才允许上传
  if (isEdit.value && userForm.userId) {
    // 编辑模式，有用户ID
    console.log(`编辑模式上传头像，用户ID=${userForm.userId}`);
    uploadUserAvatar(file, userForm.userId).then(handleUploadResponse).catch(handleUploadError);
  } else {
    // 新增模式，提示用户先保存信息
    console.warn("需要先保存用户信息才能上传头像");
    ElMessage.warning('请先保存用户基本信息，然后再上传头像');
  }
  
  return false; // 总是阻止默认上传
};

// 处理上传响应
const handleUploadResponse = (response) => {
  console.log('头像上传响应:', response);
  if (response.data && response.data.code === 200) {
    const newAvatarUrl = response.data.data;
    userForm.userImgUrl = newAvatarUrl;
    ElMessage.success('头像上传成功');
    
    // 如果是当前登录用户，更新sessionStorage中的头像信息
    updateCurrentUserAvatar(userForm.userId, newAvatarUrl);
    
    // 强制更新表格中对应用户的头像
    updateUserAvatarInTable(userForm.userId, newAvatarUrl);
    
    // 上传头像成功后立即刷新用户列表
    fetchUserList();
  } else if (response.code === 200) {
    const newAvatarUrl = response.data;
    userForm.userImgUrl = newAvatarUrl;
    ElMessage.success('头像上传成功');
    
    // 如果是当前登录用户，更新sessionStorage中的头像信息
    updateCurrentUserAvatar(userForm.userId, newAvatarUrl);
    
    // 强制更新表格中对应用户的头像
    updateUserAvatarInTable(userForm.userId, newAvatarUrl);
    
    // 上传头像成功后立即刷新用户列表
    fetchUserList();
  } else {
    const errorMsg = (response.data && response.data.message) || response.message || '头像上传失败';
    ElMessage.error(errorMsg);
    console.error('头像上传失败:', errorMsg);
  }
};

// 在表格数据中直接更新用户头像
const updateUserAvatarInTable = (userId, newAvatarUrl) => {
  // 查找表格数据中的对应用户
  const userIndex = tableData.value.findIndex(user => user.userId === userId);
  if (userIndex !== -1) {
    // 强制更新表格中用户的头像URL并触发视图更新
    const updatedUser = { ...tableData.value[userIndex], userImgUrl: newAvatarUrl };
    tableData.value.splice(userIndex, 1, updatedUser);
    console.log(`已在表格中更新用户 ${userId} 的头像: ${newAvatarUrl}`);
  }
};

// 更新当前登录用户的头像信息
const updateCurrentUserAvatar = (userId, avatarUrl) => {
  // 从sessionStorage获取用户信息
  const userStr = sessionStorage.getItem('user');
  if (userStr) {
    try {
      const user = JSON.parse(userStr);
      // 如果是当前登录用户，更新头像
      if (user.userId === userId) {
        console.log('更新当前登录用户的头像:', avatarUrl);
        user.userImgUrl = avatarUrl;
        // 保存回sessionStorage
        sessionStorage.setItem('user', JSON.stringify(user));
      }
    } catch (e) {
      console.error('解析或更新用户信息失败:', e);
    }
  }
};

// 处理上传错误
const handleUploadError = (error) => {
  console.error('头像上传失败:', error);
  ElMessage.error('头像上传失败，请稍后重试');
};

// 处理角色选择变化
const handleRoleChange = (value) => {
  console.log('角色选择变化, 新值:', value, '类型:', typeof value)
  // 确保searchForm.role被正确设置
  if (typeof value === 'object' && value.target) {
    // 原生select事件传递的是事件对象
    searchForm.role = value.target.value
    console.log('从事件对象中获取的值:', value.target.value)
  } else {
    // 直接传递的值
    searchForm.role = value
  }
  
  // 角色变化后立即搜索
  fetchUserList()
}

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
  
  // 创建一个新的Dialog来显示图片
  ElMessageBox.alert(
    `<div style="text-align: center;">
      <img src="${getImageUrl(imageUrl)}" style="max-width: 100%; max-height: 70vh;" />
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

// 添加重置密码函数
const handleResetPassword = (row) => {
  ElMessageBox.confirm(
    `确定要将用户"${row.username}"的密码重置为"123456"吗？`,
    '重置密码',
    {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      loading.value = true;
      const response = await resetPassword(row.userId);
      
      if (response.data.code === 200) {
        ElMessage.success(`用户"${row.username}"的密码已成功重置为"123456"`);
      } else {
        ElMessage.error(response.data.message || '密码重置失败');
      }
    } catch (error) {
      console.error('密码重置失败:', error);
      ElMessage.error('密码重置失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  }).catch(() => {
    // 用户取消操作，不做任何处理
  });
};
</script>

<template>
  <div class="user-container">
    <el-card class="box-card">
      <!-- 搜索区域 -->
      <div class="search-area">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="用户名">
            <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="角色">
            <!-- 使用原生select但增强样式 -->
            <select 
              v-model="searchForm.role" 
              class="el-input__inner" 
              style="width: 100%; height: 32px; border-radius: 4px; border: 1px solid #DCDFE6; padding: 0 15px; color: #606266; font-size: 14px; appearance: auto; cursor: pointer; background-color: #FFF; outline: none;"
              @change="handleRoleChange"
            >
              <option value="">全部</option>
              <option value="ADMIN">管理员</option>
              <option value="CATEGORY_MANAGER">分类管理员</option>
              <option value="USER">普通用户</option>
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
        <el-button type="primary" :icon="Plus" @click="handleAdd">添加用户</el-button>
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
        <el-table-column label="头像" width="80" align="center" :resizable="false">
          <template #default="scope">
            <el-image 
              v-if="scope.row.userImgUrl" 
              :src="getImageUrl(scope.row.userImgUrl)" 
              style="width: 50px; height: 50px; object-fit: cover; border-radius: 50%; cursor: pointer;" 
              fit="cover"
              @error="handleImageLoadError"
              @click="previewImage(scope.row.userImgUrl)"
            />
            <el-icon v-else style="font-size: 20px;"><user-filled /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" align="center" :resizable="false" />
        <el-table-column prop="email" label="邮箱" align="center" :resizable="false" />
        <el-table-column prop="phone" label="电话" align="center" :resizable="false" />
        <el-table-column prop="role" label="角色" align="center" :resizable="false">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : scope.row.role === 'CATEGORY_MANAGER' ? 'warning' : 'info'">
              {{ getRoleLabelByValue(scope.row.role) || '未知角色' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="平均评分" align="center" :resizable="false">
          <template #default="scope">
            <div v-if="scope.row.averageRating !== null && scope.row.averageRating !== undefined">
              <el-rate
                v-model="scope.row.averageRating"
                disabled
                show-score
                text-color="#ff9900"
                score-template="{value}"
              />
            </div>
            <span v-else>暂无评分</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" align="center" :resizable="false" />
        <el-table-column label="操作" width="280" align="center" :resizable="false">
          <template #default="scope">
            <div class="button-group">
              <el-button type="primary" :icon="Edit" size="small" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button type="danger" :icon="Delete" size="small" @click="handleDelete(scope.row)">删除</el-button>
              <el-button type="warning" :icon="Key" size="small" @click="handleResetPassword(scope.row)">重置密码</el-button>
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
    
    <!-- 用户表单对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="userForm"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        
        <el-form-item label="电话" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入电话" />
        </el-form-item>
        
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色">
            <el-option
              v-for="item in roleOptions.filter(item => item.value !== '')"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="平均评分" v-if="isEdit">
          <div v-if="userForm.averageRating !== null && userForm.averageRating !== undefined">
            <el-rate
              v-model="userForm.averageRating"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}"
            />
          </div>
          <span v-else>暂无评分</span>
        </el-form-item>
        
        <el-form-item label="头像">
          <div v-if="isEdit && userForm.userId" class="user-avatar-uploader">
            <el-upload
              class="user-avatar-uploader"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="(file) => beforeAvatarUpload(file.raw)"
              :on-error="handleAvatarError"
            >
              <el-image
                v-if="userForm.userImgUrl"
                class="user-avatar"
                :src="getImageUrl(userForm.userImgUrl)"
                fit="cover"
                @error="handleImageLoadError"
              />
              <div v-else class="user-avatar-placeholder">
                <el-icon><plus /></el-icon>
                <div class="el-upload__text">点击上传</div>
              </div>
            </el-upload>
            <div class="avatar-tip">建议上传正方形头像，大小不超过2MB</div>
          </div>
          <div v-else>
            <div v-if="userForm.userImgUrl">
              <el-image
                :src="getImageUrl(userForm.userImgUrl)"
                class="user-avatar"
                fit="cover"
                @error="handleImageLoadError"
              />
            </div>
            <div v-else class="no-avatar-placeholder">
              <el-icon><picture-filled /></el-icon>
              <p>请先保存用户信息，然后再上传头像</p>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false" :disabled="submitLoading">取消</el-button>
          <el-button type="primary" @click="submitUserForm" :loading="submitLoading">
            {{ isEdit ? '更新' : '添加' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-container {
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

/* 头像上传相关样式 */
.user-avatar-uploader {
  width: 100%;
  text-align: center;
}

.user-avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
  border-radius: 50%;
}

.user-avatar-placeholder {
  width: 100px;
  height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  color: #8c939d;
  cursor: pointer;
}

.no-avatar-placeholder {
  width: 100px;
  height: 100px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  color: #8c939d;
}

.no-avatar-placeholder p {
  font-size: 12px;
  margin-top: 8px;
  text-align: center;
  padding: 0 10px;
}

.avatar-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

/* 按钮组样式 */
.button-group {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 5px;
}

.button-group .el-button {
  padding: 6px 8px;
  flex-shrink: 0;
  font-size: 12px;
}

/* 确保表格占满整个卡片 */
.el-table {
  width: 100% !important;
  table-layout: fixed;
  flex: 1;
}
</style> 