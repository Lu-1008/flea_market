<template>
  <div class="order-container">
    <el-card class="box-card">
      <!-- 搜索区域 -->
      <div class="search-area">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="订单ID">
            <el-input v-model="searchForm.orderId" placeholder="请输入订单ID" clearable />
          </el-form-item>
          <el-form-item label="买家">
            <el-input v-model="searchForm.buyerName" placeholder="请输入买家用户名" clearable />
          </el-form-item>
          <el-form-item label="卖家">
            <el-input v-model="searchForm.sellerName" placeholder="请输入卖家用户名" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <select 
              v-model="searchForm.status" 
              class="el-input__inner" 
              style="width: 100%; height: 32px; border-radius: 4px; border: 1px solid #DCDFE6; padding: 0 15px; color: #606266; font-size: 14px; appearance: auto; cursor: pointer; background-color: #FFF; outline: none;"
              @change="handleStatusChange"
            >
              <option value="null">全部</option>
              <option value="PENDING">待处理</option>
              <option value="COMPLETED">已完成</option>
              <option value="CANCELLED">已取消</option>
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
        :resizable="false"
        border-fit
      >
        <el-table-column type="selection" width="45" align="center" :resizable="false" />
        <el-table-column type="index" label="序号" width="50" align="center" :resizable="false" />
        <el-table-column prop="orderId" label="订单ID" align="center" min-width="70" :resizable="false" />
        <el-table-column prop="buyerName" label="买家" align="center" min-width="100" :resizable="false" />
        <el-table-column prop="sellerName" label="卖家" align="center" min-width="100" :resizable="false" />
        <el-table-column prop="totalAmount" label="总金额" align="center" min-width="90" :resizable="false">
          <template #default="scope">
            {{ scope.row.totalAmount.toFixed(2) }} 元
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" align="center" min-width="90" :resizable="false">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" align="center" min-width="150" :resizable="false" />
        <el-table-column label="操作" min-width="220" align="center" :resizable="false">
          <template #default="scope">
            <div class="operation-buttons">
              <el-button type="primary" :icon="View" size="small" @click="handleViewDetails(scope.row)">查看详情</el-button>
              <el-button type="danger" :icon="Delete" size="small" @click="handleDelete(scope.row)">删除</el-button>
              <el-dropdown size="small" @command="(command) => updateOrderStatusAction(scope.row, command)">
                <el-button type="info" size="small">
                  更改状态
                  <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :disabled="scope.row.status === 'PENDING'" command="PENDING">待处理</el-dropdown-item>
                    <el-dropdown-item :disabled="scope.row.status === 'COMPLETED'" command="COMPLETED">已完成</el-dropdown-item>
                    <el-dropdown-item :disabled="scope.row.status === 'CANCELLED'" command="CANCELLED">已取消</el-dropdown-item>
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
    
    <!-- 订单详情对话框 -->
    <el-dialog
      title="订单详情"
      v-model="detailsDialogVisible"
      width="650px"
    >
      <div v-if="currentOrder" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单ID">{{ currentOrder.orderId }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentOrder.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="买家">{{ currentOrder.buyerName }}</el-descriptions-item>
          <el-descriptions-item label="买家地址">{{ currentOrder.buyerAddress || '无地址信息' }}</el-descriptions-item>
          <el-descriptions-item label="卖家">{{ currentOrder.sellerName }}</el-descriptions-item>
          <el-descriptions-item label="卖家地址">{{ currentOrder.sellerAddress || '无地址信息' }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(currentOrder.status)">
              {{ getStatusText(currentOrder.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span class="price">{{ currentOrder.totalAmount.toFixed(2) }} 元</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="order-items">
          <h3>订单商品</h3>
          <el-table :data="currentOrder.orderItems" border stripe style="width: 100%" :resizable="false" border-fit>
            <el-table-column prop="itemId" label="商品ID" min-width="70" align="center" :resizable="false" />
            <el-table-column prop="itemTitle" label="商品名称" min-width="120" show-overflow-tooltip :resizable="false" />
            <el-table-column prop="price" label="单价" min-width="90" align="center" :resizable="false">
              <template #default="scope">
                {{ scope.row.price.toFixed(2) }} 元
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" min-width="70" align="center" :resizable="false" />
            <el-table-column label="小计" min-width="90" align="center" :resizable="false">
              <template #default="scope">
                {{ (scope.row.price * scope.row.quantity).toFixed(2) }} 元
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="currentOrder.review" class="order-review">
          <h3>评价信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="评分">
              <el-rate v-model="currentOrder.review.rating" disabled />
            </el-descriptions-item>
            <el-descriptions-item label="评价时间">
              <el-tag size="small" type="info" v-if="currentOrder.review.createdAt">
                {{ formatDateTime(currentOrder.review.createdAt) }}
              </el-tag>
              <span v-else>暂无数据</span>
            </el-descriptions-item>
            <el-descriptions-item label="评价内容" :span="2">{{ currentOrder.review.comment }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailsDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick, onBeforeUnmount } from 'vue'
import { Search, Delete, RefreshRight, ArrowDown, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, deleteOrder, updateOrderStatus, batchDeleteOrders, getOrderDetails } from '@/api/order'

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
  const otherElementsHeight = 230; // 增加高度预留空间，确保分页组件显示完整
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
  orderId: '',
  buyerName: '',
  sellerName: '',
  status: null
})

// 详情对话框
const detailsDialogVisible = ref(false)
const currentOrder = ref(null)

// 页面初始化
onMounted(() => {
  fetchOrderList()
  
  // 添加窗口大小变化事件监听
  window.addEventListener('resize', handleResize);
})

// 组件卸载前移除事件监听
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
});

// 获取订单列表
const fetchOrderList = async () => {
  loading.value = true
  try {
    const params = {
      orderId: searchForm.orderId || undefined,
      buyerName: searchForm.buyerName || undefined,
      sellerName: searchForm.sellerName || undefined,
      status: searchForm.status === 'null' ? undefined : searchForm.status,
      page: pageData.currentPage,
      size: pageData.pageSize
    }
    
    // 调用后端API获取订单列表
    const response = await getOrderList(params)
    
    if (response.data.code === 200) {
      tableData.value = response.data.data.list
      pageData.total = response.data.data.total
    } else {
      ElMessage.error(response.data.message || '获取订单列表失败')
    }
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 搜索方法
const handleSearch = () => {
  pageData.currentPage = 1
  fetchOrderList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.orderId = ''
  searchForm.buyerName = ''
  searchForm.sellerName = ''
  searchForm.status = null
  
  pageData.currentPage = 1
  fetchOrderList()
}

// 分页变化
const handleSizeChange = (size) => {
  pageData.pageSize = size
  fetchOrderList()
}

const handleCurrentChange = (current) => {
  pageData.currentPage = current
  fetchOrderList()
}

// 刷新列表
const refreshList = () => {
  fetchOrderList()
}

// 状态变化
const handleStatusChange = () => {
  pageData.currentPage = 1
  fetchOrderList()
}

// 多选变化
const handleSelectionChange = (selection) => {
  multipleSelection.value = selection
}

// 查看订单详情
const handleViewDetails = async (row) => {
  try {
    console.log('查看订单详情，订单ID:', row.orderId);
    // 确保orderId是数字类型
    const orderId = parseInt(row.orderId);
    if (isNaN(orderId)) {
      ElMessage.error('无效的订单ID');
      return;
    }
    
    const res = await getOrderDetails(orderId);
    console.log('获取订单详情响应:', res);
    
    if (res.data && res.data.code === 200) {
      currentOrder.value = res.data.data;
      
      // 详细调试日志
      console.log('订单完整数据:', JSON.stringify(currentOrder.value));
      console.log('订单创建时间:', currentOrder.value.createdAt);
      console.log('订单创建时间类型:', typeof currentOrder.value.createdAt);
      
      if (currentOrder.value.review) {
        console.log('评价创建时间:', currentOrder.value.review.createdAt);
        console.log('评价创建时间类型:', typeof currentOrder.value.review.createdAt);
      }
      
      detailsDialogVisible.value = true;
    } else {
      ElMessage.error(res.data?.message || '获取订单详情失败');
    }
  } catch (error) {
    console.error('获取订单详情失败:', error);
    if (error.response) {
      console.error('错误响应:', error.response.data);
      ElMessage.error(error.response.data?.message || '获取订单详情失败，请稍后重试');
    } else {
      ElMessage.error('获取订单详情失败，请检查网络连接');
    }
  }
}

// 删除订单
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteOrder(row.orderId)
      if (res.data.code === 200) {
        ElMessage.success('删除成功')
        fetchOrderList()
      } else {
        ElMessage.error(res.data.message || '删除失败')
      }
    } catch (error) {
      console.error('删除订单失败:', error)
      ElMessage.error('删除订单失败，请稍后重试')
    }
  }).catch(() => {})
}

// 批量删除
const handleBatchDelete = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }
  
  ElMessageBox.confirm(`确定要删除选中的 ${multipleSelection.value.length} 条记录吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const orderIds = multipleSelection.value.map(item => item.orderId)
      const res = await batchDeleteOrders(orderIds)
      if (res.data.code === 200) {
        ElMessage.success('批量删除成功')
        fetchOrderList()
      } else {
        ElMessage.error(res.data.message || '批量删除失败')
      }
    } catch (error) {
      console.error('批量删除订单失败:', error)
      ElMessage.error('批量删除订单失败，请稍后重试')
    }
  }).catch(() => {})
}

// 更新订单状态
const updateOrderStatusAction = async (row, status) => {
  try {
    const res = await updateOrderStatus(row.orderId, status)
    if (res.data.code === 200) {
      ElMessage.success('状态更新成功')
      fetchOrderList()
    } else {
      ElMessage.error(res.data.message || '状态更新失败')
    }
  } catch (error) {
    console.error('更新订单状态失败:', error)
    ElMessage.error('更新订单状态失败，请稍后重试')
  }
}

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 'PENDING':
      return 'warning'
    case 'COMPLETED':
      return 'success'
    case 'CANCELLED':
      return 'danger'
    default:
      return 'info'
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'PENDING':
      return '待处理'
    case 'COMPLETED':
      return '已完成'
    case 'CANCELLED':
      return '已取消'
    default:
      return '未知'
  }
}

// 格式化日期时间
const formatDateTime = (timestamp) => {
  if (!timestamp) return '暂无数据';
  
  try {
    const date = new Date(timestamp);
    
    // 检查日期是否有效
    if (isNaN(date.getTime())) {
      console.error('无效的日期:', timestamp);
      return '日期格式错误';
    }
    
    // 格式化为 YYYY-MM-DD HH:MM:SS
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  } catch (error) {
    console.error('格式化日期时出错:', error, timestamp);
    return '格式化错误';
  }
}

// 组件卸载时移除事件监听
const handleResize = () => {
  // 触发表格高度重新计算
  nextTick(() => {
    // 这里不需要做什么，computed会自动重新计算
  });
};
</script>

<style scoped>
.order-container {
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

.order-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-detail h3 {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: bold;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
  font-family: monospace;
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