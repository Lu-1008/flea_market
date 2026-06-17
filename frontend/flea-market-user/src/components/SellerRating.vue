<template>
  <div class="seller-rating">
    <div v-if="loading" class="loading-indicator">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 未登录时的提示 -->
    <div v-if="!loading && !isLoggedIn" class="login-required">
      <el-empty description="登录后查看卖家评价">
        <template #extra>
          <el-button type="primary" size="large" @click="openLoginDialog" class="login-button">立即登录</el-button>
        </template>
      </el-empty>
    </div>

    <div v-else-if="!loading && reviews.length > 0" class="review-list">
      <div v-for="review in reviews" :key="review.review_id" class="review-item">
        <div class="review-user">
          <el-avatar :size="40" :src="getUserImageUrl(review.user_image_url) || defaultAvatar"></el-avatar>
          <span class="review-username">{{ review.username }}</span>
        </div>
        <div class="review-content">
          <div class="review-rating">
            <el-rate v-model="review.rating" disabled></el-rate>
            <span class="review-date">{{ formatDate(review.created_at) }}</span>
          </div>
          <div class="review-comment">{{ review.comment }}</div>
          <div class="review-item-info">
            <el-tag size="small" type="info">购买商品: {{ review.item_title }}</el-tag>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else-if="!loading && reviews.length === 0 && isLoggedIn" class="no-reviews">
      <el-empty description="暂无评价" />
    </div>
    
    <div v-if="!loading && hasMore && isLoggedIn" class="load-more">
      <el-button type="primary" plain @click="loadMore" :loading="loadingMore">
        加载更多
      </el-button>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, inject } from 'vue';
import { getSellerReviews, getSellerRating } from '@/api/review';
import defaultAvatar from '@/assets/images/user-default.png';
import { getFileUrl } from '@/api/config';
import { ElMessage } from 'element-plus';

export default {
  name: 'SellerRating',
  props: {
    sellerId: {
      type: Number,
      required: true
    },
    seller: {
      type: Object,
      default: () => ({})
    }
  },
  setup(props) {
    const reviews = ref([]);
    const loading = ref(true);
    const loadingMore = ref(false);
    const currentPage = ref(1);
    const pageSize = ref(5);
    const totalReviews = ref(0);
    const hasMore = ref(false);
    const averageRating = ref(0);
    const isLoggedIn = ref(false);
    
    // 注入全局事件总线，用于触发登录对话框
    const emitter = inject('emitter', null);
    
    // 检查用户是否已登录
    const checkLoginStatus = () => {
      try {
        const token = sessionStorage.getItem('token');
        const userInfo = sessionStorage.getItem('userInfo');
        isLoggedIn.value = !!(token && userInfo);
      } catch (error) {
        console.error('检查登录状态失败:', error);
        isLoggedIn.value = false;
      }
    };
    
    // 打开登录对话框
    const openLoginDialog = () => {
      if (emitter) {
        emitter.emit('open-login-dialog');
      } else {
        ElMessage.info('请点击右上角的登录按钮进行登录');
      }
    };
    
    const sellerImageUrl = computed(() => {
      return props.seller.userImageUrl || null;
    });
    
    // 格式化日期
    const formatDate = (dateString) => {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleString('zh-CN', { 
        year: 'numeric', 
        month: '2-digit', 
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    };
    
    // 处理用户头像URL
    const getUserImageUrl = (url) => {
      console.log('原始头像URL:', url);
      
      // 如果url为空，直接返回null
      if (!url) {
        console.log('头像URL为空，使用默认头像');
        return null;
      }
      
      // 如果已经是完整URL，直接返回
      if (url.startsWith('http')) {
        console.log('完整URL，直接使用:', url);
        return url;
      }
      
      // 否则先尝试直接从MinIO获取
      const processedUrl = getFileUrl(url, true); // 改为true，直接从MinIO获取
      console.log('处理后头像URL(直接访问MinIO):', processedUrl);
      return processedUrl;
    };
    
    // 加载卖家评价
    const loadSellerReviews = async (page = 1, replace = true) => {
      // 检查登录状态
      checkLoginStatus();
      
      // 如果未登录，不加载数据
      if (!isLoggedIn.value) {
        loading.value = false;
        return;
      }
      
      // 验证sellerId是否有效
      if (!props.sellerId || props.sellerId <= 0) {
        console.error('无效的卖家ID:', props.sellerId);
        loading.value = false;
        return;
      }
      
      if (page === 1) {
        loading.value = true;
      } else {
        loadingMore.value = true;
      }
      
      try {
        const params = {
          page,
          size: pageSize.value
        };
        
        const response = await getSellerReviews(props.sellerId, params);
        
        // 检查响应是否符合预期结构
        if (response && response.code === 200 && response.data) {
          const responseData = response.data;
          if (replace) {
            reviews.value = responseData.list || [];
          } else {
            reviews.value = [...reviews.value, ...(responseData.list || [])];
          }
          
          totalReviews.value = responseData.total || 0;
          hasMore.value = reviews.value.length < totalReviews.value;
        } else {
          console.error('获取卖家评价失败:', response?.message || '未知错误');
          // 确保在错误时也能显示空状态
          if (replace) {
            reviews.value = [];
          }
        }
      } catch (error) {
        console.error('获取卖家评价失败:', error?.message || error);
        if (replace) {
          reviews.value = [];
        }
      } finally {
        loading.value = false;
        loadingMore.value = false;
      }
    };
    
    // 加载卖家评分
    const loadSellerRating = async () => {
      // 检查登录状态
      checkLoginStatus();
      
      // 如果未登录，不加载数据
      if (!isLoggedIn.value) {
        return;
      }
      
      // 验证sellerId是否有效
      if (!props.sellerId || props.sellerId <= 0) {
        console.error('无效的卖家ID:', props.sellerId);
        return;
      }
    
      try {
        const response = await getSellerRating(props.sellerId);
        if (response && response.code === 200) {
          averageRating.value = response.data || 0;
        } else {
          console.warn('获取卖家评分返回非200状态:', response?.message || '未知错误');
          // 设置默认评分为0
          averageRating.value = 0;
        }
      } catch (error) {
        console.warn('获取卖家评分出现异常，可能是该卖家暂无评价:', error?.message || error);
        // 设置默认评分为0
        averageRating.value = 0;
      }
    };
    
    // 加载更多评价
    const loadMore = () => {
      currentPage.value++;
      loadSellerReviews(currentPage.value, false);
    };
    
    onMounted(() => {
      // 检查登录状态
      checkLoginStatus();
      
      // 无论是否登录都要将加载状态设为false，以便显示相应提示
      if (props.sellerId && props.sellerId > 0) {
        if (isLoggedIn.value) {
          loadSellerReviews();
          loadSellerRating();
        } else {
          loading.value = false;
        }
      } else {
        loading.value = false;
        console.log('无效的卖家ID，不加载评价', props.sellerId);
      }
    });
    
    return {
      reviews,
      loading,
      loadingMore,
      hasMore,
      totalReviews,
      averageRating,
      defaultAvatar,
      sellerImageUrl,
      isLoggedIn,
      formatDate,
      loadMore,
      getUserImageUrl,
      openLoginDialog
    };
  }
};
</script>

<style scoped>
.seller-rating {
  margin: 0;
  padding: 0;
  border-radius: 0;
  background-color: #fff;
  box-shadow: none;
  width: 100%;
}

.loading-indicator {
  width: 100%;
}

.review-list {
  margin-top: 5px;
  max-height: 350px;
  overflow-y: auto;
  padding-right: 10px;
  width: 100%;
}

.review-item {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
  width: 100%;
}

.review-user {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 15px;
  min-width: 55px;
  max-width: 55px;
}

.review-username {
  margin-top: 3px;
  font-size: 12px;
  color: #606266;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 55px;
}

.review-content {
  flex: 1;
  width: calc(100% - 70px);
}

.review-rating {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
}

.review-date {
  color: #909399;
  font-size: 12px;
}

.review-comment {
  line-height: 1.4;
  color: #303133;
  margin-bottom: 5px;
  font-size: 14px;
}

.review-item-info {
  margin-top: 3px;
}

.no-reviews {
  padding: 20px 0;
  text-align: center;
}

.login-required {
  padding: 50px 0;
  text-align: center;
  background-color: #f9f9f9;
  border-radius: 4px;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.login-required :deep(.el-empty) {
  padding: 40px 0;
}

.login-required :deep(.el-empty__image) {
  width: 120px;
  height: 120px;
}

.login-required :deep(.el-empty__description) {
  color: #606266;
  font-size: 16px;
  margin-bottom: 15px;
}

.login-button {
  padding: 12px 30px;
  font-weight: bold;
  font-size: 16px;
}

.load-more {
  text-align: center;
  margin-top: 10px;
}

/* 滚动条样式美化 */
.review-list::-webkit-scrollbar {
  width: 6px;
}

.review-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.review-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.review-list::-webkit-scrollbar-thumb:hover {
  background: #ccc;
}
</style> 