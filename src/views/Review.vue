<template>
  <div class="review-container">
    <el-card class="box-card">
      <!-- 搜索区域 -->
      <div class="search-area">
        <el-form :inline="true" :model="searchForm" class="search-form">
          <el-form-item label="商品标题">
            <el-input v-model="searchForm.itemTitle" placeholder="请输入商品标题" clearable />
          </el-form-item>
          <el-form-item label="评价者">
            <el-input v-model="searchForm.username" placeholder="请输入评价用户名" clearable />
          </el-form-item>
          <el-form-item label="卖家">
            <el-input v-model="searchForm.sellerName" placeholder="请输入卖家用户名" clearable />
          </el-form-item>
          <el-form-item label="评分范围">
            <div class="rating-range">
              <el-rate v-model="searchForm.minRating" :max="5" :allow-half="false" />
              <span class="rating-separator">至</span>
              <el-rate v-model="searchForm.maxRating" :max="5" :allow-half="false" />
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="RefreshRight" @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 统计信息卡片 -->
      <div class="statistics-cards">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-card shadow="hover">
              <div class="statistic-item">
                <div class="statistic-icon">
                  <el-icon><comment /></el-icon>
                </div>
                <div class="statistic-info">
                  <div class="statistic-title">评价总数</div>
                  <div class="statistic-value">{{ statistics.totalReviews || 0 }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
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
        <el-table-column prop="item_title" label="商品标题" min-width="180" show-overflow-tooltip align="center" :resizable="false" />
        <el-table-column prop="username" label="评价者" min-width="100" align="center" :resizable="false" />
        <el-table-column prop="seller_name" label="卖家" min-width="100" align="center" :resizable="false" />
        <el-table-column label="评分" width="150" align="center" :resizable="false">
          <template #default="scope">
            <el-rate v-model="scope.row.rating" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="评价内容" min-width="200" align="center" :resizable="false">
          <template #default="scope">
            <div class="comment-cell">{{ scope.row.comment }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="评价时间" min-width="140" align="center" :resizable="false" />
        <el-table-column label="操作" min-width="150" align="center" :resizable="false">
          <template #default="scope">
            <div class="operation-buttons">
              <el-button type="primary" :icon="View" size="small" @click="handleViewDetails(scope.row)">查看详情</el-button>
              <el-button type="danger" :icon="Delete" size="small" @click="handleDelete(scope.row)">删除</el-button>
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
    
    <!-- 评价详情对话框 -->
    <el-dialog
      title="用户评价详情"
      v-model="detailsDialogVisible"
      width="750px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <div v-if="currentReview" class="review-details">
        <!-- 商品图片和基本信息并排布局 -->
        <div class="review-header">
          <div class="review-basic-info">
            <h3>{{ currentReview.item_title || currentReview.itemTitle }}</h3>
            <div class="review-meta">
              <div class="review-meta-item">
                <span class="label">商品ID:</span>
                <span class="value">{{ currentReview.item_id || currentReview.itemId || '未关联商品' }}</span>
              </div>
              <div class="review-meta-item">
                <span class="label">评价用户:</span>
                <span class="value">{{ currentReview.username || '未知用户' }}</span>
              </div>
              <div class="review-meta-item">
                <span class="label">评价用户ID:</span>
                <span class="value">{{ currentReview.user_id || currentReview.userId || '未知' }}</span>
              </div>
              <div class="review-meta-item">
                <span class="label">卖家用户名:</span>
                <span class="value">{{ currentReview.seller_name || currentReview.sellerName || '未知卖家' }}</span>
              </div>
              <div class="review-meta-item">
                <span class="label">卖家ID:</span>
                <span class="value">{{ currentReview.seller_id || currentReview.sellerId || '未知' }}</span>
              </div>
              <div class="review-meta-item">
                <span class="label">订单ID:</span>
                <span class="value">{{ currentReview.order_id || currentReview.orderId || '未关联订单' }}</span>
              </div>
              <div class="review-meta-item">
                <span class="label">评价时间:</span>
                <span class="value">
                  <el-tag size="small" type="info" v-if="currentReview.created_at || currentReview.createdAt">
                    {{ formatDateTime(currentReview.created_at || currentReview.createdAt) }}
                  </el-tag>
                  <span v-else>暂无数据</span>
                </span>
              </div>
              <div class="review-meta-item">
                <span class="label">评价ID:</span>
                <span class="value">{{ currentReview.review_id || currentReview.reviewId || '未知' }}</span>
              </div>
              <div class="review-meta-item">
                <span class="label">评分:</span>
                <span class="value">
                  <el-rate v-model="currentReview.rating" disabled />
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 评价内容区域 -->
        <div class="review-content-section">
          <h3>对卖家的评价内容</h3>
          <div class="review-comment">{{ currentReview.comment || '暂无评价内容' }}</div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailsDialogVisible = false">关闭</el-button>
          <el-button type="danger" @click="handleDeleteInDialog">删除评价</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, onBeforeUnmount, nextTick } from 'vue'
import { Search, Delete, RefreshRight, View, Comment } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReviewList, getReviewDetails, deleteReview, batchDeleteReviews, getReviewStatistics } from '@/api/review'

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

// 搜索表单
const searchForm = reactive({
  itemTitle: '',
  username: '',
  sellerName: '',
  minRating: null,
  maxRating: 5
})

// 统计数据
const statistics = reactive({
  totalReviews: 0
})

// 详情对话框
const detailsDialogVisible = ref(false)
const currentReview = ref(null)

// 表格高度计算
const tableHeight = computed(() => {
  // 获取视窗高度
  const windowHeight = window.innerHeight;
  // 预留头部导航栏高度
  const headerHeight = 60;
  // 预留容器内其他元素的高度（搜索区域、统计卡片、操作按钮区域、分页区域等）
  const otherElementsHeight = 380; // 增加高度预留空间，确保分页组件显示完整
  // 预留边距
  const padding = 40;
  
  // 计算表格可用高度
  const availableHeight = windowHeight - headerHeight - otherElementsHeight - padding;
  
  // 设置最小高度
  const minHeight = 300;
  // 表格高度为可用高度，但不小于最小高度
  return Math.max(minHeight, availableHeight);
});

// 页面初始化
onMounted(() => {
  fetchReviewList()
  fetchStatistics()
  
  // 添加窗口大小变化事件监听
  window.addEventListener('resize', handleResize);
})

// 组件卸载前移除事件监听
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
});

// 窗口大小变化处理
const handleResize = () => {
  // 触发表格高度重新计算
  nextTick(() => {
    // 这里不需要做什么，computed会自动重新计算
  });
};

// 获取评价列表
const fetchReviewList = async () => {
  loading.value = true
  try {
    const params = {
      page: pageData.currentPage,
      size: pageData.pageSize
    };
    
    // 添加搜索条件
    if (searchForm.itemTitle) {
      params.itemTitle = searchForm.itemTitle;
    }
    
    if (searchForm.username) {
      params.username = searchForm.username;
    }
    
    if (searchForm.sellerName) {
      params.sellerName = searchForm.sellerName;
    }
    
    if (searchForm.minRating !== null) {
      params.minRating = searchForm.minRating;
    }
    
    if (searchForm.maxRating !== null) {
      params.maxRating = searchForm.maxRating;
    }
    
    const response = await getReviewList(params);
    
    // 处理后端返回的Result对象
    if (response.data && response.data.code === 200) {
      // 后端Result.success返回的数据在data字段中
      const responseData = response.data.data;
      tableData.value = responseData.list || [];
      pageData.total = responseData.total || 0;
    } else {
      ElMessage.error(response.data?.message || '获取评价列表失败');
    }
  } catch (error) {
    console.error('获取评价列表出错:', error);
    ElMessage.error('获取评价列表失败: ' + (error.message || '未知错误'));
  } finally {
    loading.value = false;
  }
}

// 获取统计信息
const fetchStatistics = async () => {
  try {
    console.log('获取评价统计信息');
    const response = await getReviewStatistics();
    console.log('统计信息响应:', response);
    
    if (response.data && response.data.code === 200) {
      const data = response.data.data || {};
      statistics.totalReviews = data.totalReviews || 0;
    } else {
      console.error('获取评价统计信息失败:', response.data?.message);
    }
  } catch (error) {
    console.error('获取评价统计信息失败:', error);
    if (error.response) {
      console.error('错误响应:', error.response.data);
    }
    // 设置默认值
    statistics.totalReviews = 0;
  }
}

// 搜索方法
const handleSearch = () => {
  pageData.currentPage = 1
  fetchReviewList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.itemTitle = ''
  searchForm.username = ''
  searchForm.sellerName = ''
  searchForm.minRating = null
  searchForm.maxRating = 5
  
  pageData.currentPage = 1
  fetchReviewList()
}

// 分页变化
const handleSizeChange = (size) => {
  pageData.pageSize = size
  fetchReviewList()
}

const handleCurrentChange = (current) => {
  pageData.currentPage = current
  fetchReviewList()
}

// 刷新列表
const refreshList = () => {
  fetchReviewList()
  fetchStatistics()
}

// 多选变化
const handleSelectionChange = (selection) => {
  multipleSelection.value = selection
}

// 查看评价详情
const handleViewDetails = async (row) => {
  try {
    console.log('查看评价详情，评价ID:', row.review_id);
    if (!row.review_id) {
      console.error('无效的评价ID:', row);
      ElMessage.error('无法查看详情：评价ID无效');
      return;
    }
    
    const response = await getReviewDetails(row.review_id);
    console.log('获取评价详情响应:', response);
    
    if (response.data && response.data.code === 200) {
      currentReview.value = response.data.data || {};
      console.log('设置当前评价详情:', currentReview.value);
      
      // 确保评价对象中的关键字段有合理默认值，避免前端渲染错误
      if (!currentReview.value.reviewId && row.review_id) {
        currentReview.value.reviewId = row.review_id;
        console.log('从行数据补充评价ID:', currentReview.value.reviewId);
      }
      
      if (!currentReview.value.itemId && row.item_id) {
        currentReview.value.itemId = row.item_id;
        console.log('从行数据补充商品ID:', currentReview.value.itemId);
      }
      
      if (!currentReview.value.userId && row.user_id) {
        currentReview.value.userId = row.user_id;
        console.log('从行数据补充用户ID:', currentReview.value.userId);
      }
      
      if (!currentReview.value.sellerId && row.seller_id) {
        currentReview.value.sellerId = row.seller_id;
        console.log('从行数据补充卖家ID:', currentReview.value.sellerId);
      }
      
      // 确保表格行数据和详情数据的字段名称一致
      if (row.review_id && !currentReview.value.review_id) {
        currentReview.value.review_id = row.review_id;
      }
      
      if (row.item_id && !currentReview.value.item_id) {
        currentReview.value.item_id = row.item_id;
      }
      
      detailsDialogVisible.value = true;
    } else {
      ElMessage.error(response.data?.message || '获取评价详情失败');
    }
  } catch (error) {
    console.error('获取评价详情出错:', error);
    ElMessage.error('获取评价详情失败: ' + (error.message || '未知错误'));
  }
}

// 删除评价
const handleDelete = (row) => {
  const reviewId = row.review_id;
  if (!reviewId) {
    ElMessage.error('无效的评价ID');
    return;
  }
  
  ElMessageBox.confirm('确定要删除该评价吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      console.log('删除评价，评价ID:', reviewId);
      const res = await deleteReview(reviewId);
      console.log('删除评价响应:', res);
      
      if (res.data && res.data.code === 200) {
        ElMessage.success('删除成功');
        fetchReviewList();
        fetchStatistics();
      } else {
        ElMessage.error(res.data?.message || '删除失败');
      }
    } catch (error) {
      console.error('删除评价失败:', error);
      if (error.response) {
        console.error('错误响应:', error.response.data);
      }
      ElMessage.error('删除评价失败: ' + (error.message || '未知错误'));
    }
  }).catch(() => {});
}

// 在对话框中删除评价
const handleDeleteInDialog = () => {
  if (!currentReview.value) return
  
  ElMessageBox.confirm('确定要删除该评价吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteReview(currentReview.value.review_id)
      if (res.data && res.data.code === 200) {
        ElMessage.success('删除成功')
        detailsDialogVisible.value = false
        fetchReviewList()
        fetchStatistics()
      } else {
        ElMessage.error(res.data?.message || '删除失败')
      }
    } catch (error) {
      console.error('删除评价失败:', error)
      ElMessage.error('删除评价失败: ' + (error.message || '未知错误'))
    }
  }).catch(() => {})
}

// 批量删除
const handleBatchDelete = () => {
  if (multipleSelection.value.length === 0) {
    ElMessage.warning('请至少选择一条记录');
    return;
  }
  
  ElMessageBox.confirm(`确定要删除选中的 ${multipleSelection.value.length} 条记录吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 获取选中的评价ID列表
      const reviewIds = multipleSelection.value.map(item => {
        const id = item.review_id;
        if (!id) {
          console.warn('发现无效的评价ID:', item);
        }
        return id;
      }).filter(id => id); // 过滤掉无效的ID
      
      if (reviewIds.length === 0) {
        ElMessage.error('未找到有效的评价ID');
        return;
      }
      
      console.log('批量删除评价，ID列表:', reviewIds);
      const res = await batchDeleteReviews(reviewIds);
      console.log('批量删除响应:', res);
      
      if (res.data && res.data.code === 200) {
        const result = res.data.data || {};
        const successCount = result.success?.length || 0;
        const errorCount = result.error?.length || 0;
        
        if (successCount > 0) {
          ElMessage.success(`成功删除 ${successCount} 个评价`);
        }
        
        if (errorCount > 0) {
          ElMessage.warning(`${errorCount} 个评价删除失败`);
          console.error('删除失败的评价ID:', result.error);
        }
        
        // 刷新列表
        fetchReviewList();
        fetchStatistics();
      } else {
        ElMessage.error(res.data?.message || '批量删除失败');
      }
    } catch (error) {
      console.error('批量删除评价失败:', error);
      if (error.response) {
        console.error('错误响应:', error.response.data);
      }
      ElMessage.error('批量删除评价失败: ' + (error.message || '未知错误'));
    }
  }).catch(() => {
    // 用户取消删除，不做任何操作
  });
}

// 格式化时间
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
</script>

<style scoped>
.review-container {
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

.statistics-cards {
  margin-bottom: 20px;
}

.statistics-cards .el-card {
  height: 120px; /* 设置固定高度 */
  display: flex;
}

.statistics-cards .el-card__body {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

.statistic-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  text-align: center;
}

.statistic-icon {
  font-size: 2.5rem;
  margin-right: 15px;
  color: #409EFF;
  display: flex;
  justify-content: center;
}

.statistic-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
}

.statistic-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 5px;
  text-align: center;
}

.statistic-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
  text-align: center;
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
  min-width: 150px;
}

.review-details {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.review-details h3 {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.review-images {
  margin-bottom: 15px;
}

.image-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-container .el-image {
  width: 200px;
  height: 200px;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.review-content-section {
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  padding: 15px;
  background-color: #F8F8F9;
}

.review-comment {
  white-space: pre-wrap;
  word-break: break-word;
  padding: 15px;
  background-color: #ffffff;
  border-radius: 4px;
  min-height: 100px;
  max-height: 300px;
  overflow-y: auto;
  text-align: left;
  border: 1px solid #EBEEF5;
  line-height: 1.5;
  font-size: 14px;
  color: #606266;
}

.rating-range {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating-separator {
  margin: 0 5px;
  color: #909399;
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

.box-card {
  width: 100%;
  overflow: hidden;
}

/* 固定评价内容行高并添加省略号 */
.comment-cell {
  max-height: 40px;
  line-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2; /* 最多显示2行 */
  -webkit-box-orient: vertical;
  word-break: break-all;
  text-align: left;
  padding: 0 5px;
}

/* 评价详情样式 */
.review-details {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 评价头部区域 - 基本信息 */
.review-header {
  display: flex;
  margin-bottom: 10px;
}

.review-basic-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.review-basic-info h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  line-height: 1.4;
}

.review-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.review-meta-item {
  display: flex;
  align-items: center;
}

.review-meta-item .label {
  width: 80px;
  font-size: 14px;
  color: #909399;
}

.review-meta-item .value {
  font-size: 14px;
  color: #606266;
}

/* 评价内容区域样式 */
.review-content-section {
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  padding: 20px;
  background-color: #F8F8F9;
}

.review-content-section h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

/* 评价内容样式 */
.review-comment {
  white-space: pre-wrap;
  word-break: break-word;
  padding: 20px;
  background-color: #ffffff;
  border-radius: 8px;
  min-height: 150px;
  max-height: 350px;
  overflow-y: auto;
  text-align: left;
  border: 1px solid #EBEEF5;
  line-height: 1.6;
  font-size: 14px;
  color: #606266;
  box-shadow: inset 0 0 5px rgba(0, 0, 0, 0.05);
}
</style>