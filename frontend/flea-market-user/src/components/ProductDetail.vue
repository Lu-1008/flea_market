<template>
  <div class="product-detail-overlay" @click="closeDetail">
    <div class="product-detail-container" @click.stop>
      <div class="detail-header">
        <h2 class="detail-title">商品详情</h2>
        <el-button class="close-button" type="text" @click="closeDetail">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
      
      <el-skeleton :loading="loading" animated>
        <template #template>
          <div class="skeleton-content">
            <el-skeleton-item variant="image" style="width: 100%; height: 400px" />
            <div style="padding: 20px">
              <el-skeleton-item variant="h3" style="width: 50%" />
              <div style="margin-top: 16px">
                <el-skeleton-item variant="text" style="margin-bottom: 16px" />
                <el-skeleton-item variant="text" style="width: 60%" />
              </div>
            </div>
          </div>
        </template>
        
        <template #default>
          <div class="detail-content">
            <div class="detail-image-container">
              <img :src="currentMainImage || productImage" :alt="product.title" class="detail-image" />
              
              <div class="thumbnail-list" v-if="additionalImages && additionalImages.length > 0">
                <div 
                  class="thumbnail-item"
                  @click="currentMainImage = productImage"
                >
                  <img :src="productImage" :alt="product.title" class="thumbnail-image" />
                </div>
                <div 
                  v-for="(url, index) in additionalImages" 
                  :key="index"
                  class="thumbnail-item"
                  @click="switchMainImage(url)"
                >
                  <img :src="getProductImageUrl(url)" :alt="product.title" class="thumbnail-image" />
                </div>
              </div>
            </div>
            
            <div class="detail-info">
              <h2 class="info-title">{{ product.title }}</h2>
              
              <div class="info-price">
                <span class="price-label">价格：</span>
                <span class="price-value">¥{{ product.price ? product.price.toFixed(2) : '0.00' }}</span>
              </div>
              
              <div class="info-category" v-if="product.categoryName">
                <span class="category-label">分类：</span>
                <el-tag size="small" type="success">{{ product.categoryName }}</el-tag>
              </div>
              
              <div class="info-seller" v-if="product.username">
                <span class="seller-label">卖家：</span>
                <span class="seller-name">{{ product.username }}</span>
                <span class="seller-rating" v-if="sellerRating > 0">
                  <el-rate
                    v-model="sellerRating"
                    disabled
                    show-score
                    text-color="#ff9900"
                    score-template="{value}"
                    size="small"
                  />
                </span>
                <span v-else class="no-rating">暂无评分</span>
              </div>
              
              <div class="info-date" v-if="product.createdAt">
                <span class="date-label">发布时间：</span>
                <span class="date-value">{{ formatDate(product.createdAt) }}</span>
              </div>
              
              <div class="info-status" v-if="product.status">
                <span class="status-label">状态：</span>
                <el-tag 
                  :type="product.status === 'ACTIVE' ? 'success' : product.status === 'SOLD' ? 'info' : 'danger'"
                >
                  {{ product.status === 'ACTIVE' ? '在售' : product.status === 'SOLD' ? '已售出' : '已下架' }}
                </el-tag>
              </div>
              
              <div class="info-description">
                <h3 class="description-title">商品描述</h3>
                <p class="description-content">{{ product.description || '暂无商品描述' }}</p>
              </div>
              
              <div class="action-buttons">
                <el-button
                  type="warning"
                  icon="ShoppingCart"
                  @click="handleAddToCart"
                  :disabled="product.status !== 'ACTIVE'"
                >
                  加入购物车
                </el-button>
                <el-button
                  type="primary"
                  @click="handleBuy"
                  :disabled="product.status !== 'ACTIVE'"
                >
                  立即购买
                </el-button>
              </div>
            </div>
          </div>
          
          <!-- 卖家评价组件 -->
          <div v-if="product.userId && product.userId > 0" class="seller-rating-container">
            <SellerRating :seller-id="product.userId" :seller="{username: product.username, userImageUrl: product.userImageUrl}" />
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
  
  <!-- 购买对话框 -->
  <PurchaseDialog 
    v-if="showPurchaseDialog"
    v-model:visible="showPurchaseDialog"
    :item-data="product"
    :buyer-id="userStore.userInfo?.userId || 0"
    @purchase-success="handlePurchaseSuccess"
    @update:visible="(val) => showPurchaseDialog = val"
  />
</template>

<script setup>
import { computed, ref, onMounted, inject, watch, nextTick, onBeforeUnmount } from 'vue'
import { Close } from '@element-plus/icons-vue'
import { getFileUrl } from '@/api/config'
import SellerRating from './SellerRating.vue'
import PurchaseDialog from './PurchaseDialog.vue'
import { getSellerRating } from '@/api/review'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useUserStore } from '@/store/user'

const props = defineProps({
  product: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  shouldBuy: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'add-to-cart', 'buy'])
const sellerRating = ref(0)
const showPurchaseDialog = ref(false) // 购买对话框显示状态
const userStore = useUserStore() // 使用userStore
const emitter = inject('emitter', null) // 注入事件总线

// 在组件挂载时获取卖家评分和设置初始主图
onMounted(async () => {
  // 设置初始主图
  if (props.product && props.product.itemImageUrl) {
    currentMainImage.value = getFileUrl(props.product.itemImageUrl, true, true)
  }

  // 获取卖家评分
  // 确保product和userId都有效，且userId不是0
  if (props.product && props.product.userId && props.product.userId > 0) {
    try {
      const response = await getSellerRating(props.product.userId)
      // 修改响应处理逻辑，适应实际返回的响应结构
      if (response && response.code === 200) {
        sellerRating.value = response.data || 0
      } else {
        console.error('获取卖家评分失败:', response?.message || '未知错误')
      }
    } catch (error) {
      console.error('获取卖家评分失败:', error?.message || error)
      // 出错时不设置评分，保持默认值0
    }
  } else {
    console.log('卖家ID无效，不加载评分', props.product?.userId)
  }
})

// 监听shouldBuy属性变化，如果为true则触发购买功能，但要确保用户信息已加载
watch(() => props.shouldBuy, (newValue) => {
  if (newValue && userStore.userInfo && userStore.userInfo.userId && props.product.status === 'ACTIVE') {
    // 确保组件已经完全加载并且用户已登录才尝试显示购买对话框
    nextTick(() => {
      handleBuy(new Event('watch'))
    })
  }
}, { immediate: true })

// 在组件卸载前进行清理工作
onBeforeUnmount(() => {
  // 确保对话框已关闭
  showPurchaseDialog.value = false
})

// 关闭详情对话框
const closeDetail = () => {
  emit('close')
}

// 加入购物车
const handleAddToCart = async () => {
  // 检查用户是否已登录
  if (!userStore.userInfo || !userStore.userInfo.userId) {
    ElMessage.warning('请先登录后再将商品加入购物车')
    // 可以在这里触发登录弹窗
    if (emitter) {
      emitter.emit('open-login-dialog')
    }
    return
  }
  
  // 检查商品状态是否为可购买
  if (props.product.status !== 'ACTIVE') {
    ElMessage.warning(`商品当前状态为"${props.product.status === 'SOLD' ? '已售出' : '已下架'}"，无法加入购物车`)
    return
  }
  
  // 检查不能购买自己的商品
  if (userStore.userInfo.userId === props.product.userId) {
    ElMessage.warning('不能将自己的商品加入购物车')
    return
  }
  
  try {
    // 发起添加到购物车的请求
    emit('add-to-cart', {
      userId: userStore.userInfo.userId,
      itemId: props.product.itemId,
      quantity: 1
    })
  } catch (error) {
    console.error('加入购物车失败:', error)
    ElMessage.error('加入购物车失败，请重试')
  }
}

// 立即购买
const handleBuy = (event) => {
  // 如果是事件对象，阻止默认行为
  if (event && event.preventDefault) {
    event.preventDefault()
  }
  
  // 检查用户是否已登录
  if (!userStore.userInfo || !userStore.userInfo.userId) {
    ElMessage.warning('请先登录后再购买商品')
    // 可以在这里触发登录弹窗
    if (emitter) {
      emitter.emit('open-login-dialog')
    }
    return
  }
  
  // 检查不能购买自己的商品
  if (userStore.userInfo.userId === props.product.userId) {
    ElMessage.warning('不能购买自己发布的商品')
    return
  }
  
  // 检查商品状态是否为可购买
  if (props.product.status !== 'ACTIVE') {
    ElMessage.warning(`商品当前状态为"${props.product.status === 'SOLD' ? '已售出' : '已下架'}"，无法购买`)
    return
  }
  
  // 显示购买对话框
  try {
    console.log('显示购买对话框', userStore.userInfo)
    showPurchaseDialog.value = true
  } catch (error) {
    console.error('打开购买对话框失败:', error)
    ElMessage.error('打开购买对话框失败，请重试')
  }
}

// 处理购买成功
const handlePurchaseSuccess = (orderData) => {
  try {
    ElMessage.success('购买成功！')
    
    // 更新商品状态为已售出
    if (props.product) {
      props.product.status = 'SOLD'
    }
    
    // 使用nextTick确保在DOM更新后再关闭对话框
    nextTick(() => {
      showPurchaseDialog.value = false
      emit('buy-success', orderData) // 传递购买成功事件给父组件
      closeDetail() // 关闭详情对话框
    })
  } catch (error) {
    console.error('处理购买成功时出错:', error)
    ElMessage.error('处理购买操作时出错')
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  
  const date = new Date(dateString)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 获取商品图片
const productImage = computed(() => {
  if (!props.product.itemImageUrl) {
    return new URL(`../assets/images/default-item.jpg`, import.meta.url).href
  }
  return getFileUrl(props.product.itemImageUrl, true, true)
})

// 当前显示的主图
const currentMainImage = ref('')

// 获取产品图片的URL
const getProductImageUrl = (url) => {
  return getFileUrl(url, true, true)
}

// 切换主图片
const switchMainImage = (url) => {
  currentMainImage.value = getProductImageUrl(url)
}

// 获取额外的产品图片
const additionalImages = computed(() => {
  // 如果存在itemImageUrls数组并且有多个图片
  if (props.product.itemImageUrls && Array.isArray(props.product.itemImageUrls) && props.product.itemImageUrls.length > 0) {
    // 过滤掉与主图相同的图片
    return props.product.itemImageUrls.filter(url => url !== props.product.itemImageUrl);
  }
  
  // 如果存在单张额外图片
  if (props.product.extraImageUrl) {
    return [props.product.extraImageUrl];
  }
  
  return [];
})
</script>

<style scoped>
.product-detail-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  padding: 20px;
  overflow-y: auto;
}

.product-detail-container {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 1000px;
  max-height: 90vh;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
}

.detail-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.close-button {
  font-size: 20px;
}

.skeleton-content {
  display: flex;
  flex-direction: column;
}

.detail-content {
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.detail-image-container {
  width: 100%;
  height: auto;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
  padding: 20px;
}

.detail-image {
  max-width: 100%;
  max-height: 500px;
  object-fit: contain;
  margin-bottom: 10px;
  border-radius: 4px;
}

.thumbnail-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 15px;
  justify-content: center;
}

.thumbnail-item {
  width: 80px;
  height: 80px;
  border: 2px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.thumbnail-item:hover {
  border-color: #FF5000;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-info {
  padding: 20px;
  flex: 1;
}

.info-title {
  margin: 0 0 20px 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.info-price {
  font-size: 18px;
  margin-bottom: 16px;
}

.price-value {
  font-size: 24px;
  font-weight: bold;
  color: #FF5000;
}

.info-category,
.info-seller,
.info-date,
.info-status {
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
}

.info-description {
  margin-top: 24px;
  margin-bottom: 24px;
}

.description-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #303133;
}

.description-content {
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
  white-space: pre-wrap;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.info-seller {
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
}

.seller-name {
  margin-right: 10px;
  font-weight: bold;
}

.seller-rating {
  display: inline-flex;
  align-items: center;
  margin-left: 10px;
}

.seller-rating-container {
  margin-top: 0;
  padding: 0;
  border-top: 1px solid #ebeef5;
}

.no-rating {
  color: #909399;
  font-size: 14px;
}

@media (min-width: 768px) {
  .detail-content {
    flex-direction: row;
  }
  
  .detail-image-container {
    width: 60%;
    padding: 20px;
  }
  
  .detail-info {
    width: 40%;
    border-left: 1px solid #ebeef5;
  }
  
  .detail-image {
    max-height: 450px;
  }
}
</style> 