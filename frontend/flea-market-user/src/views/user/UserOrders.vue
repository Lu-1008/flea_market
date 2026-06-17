<script setup>
import { ref, onMounted, watch, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/store/user';
import { getUserOrders, getOrderDetail, getFullUserOrders, getFullOrderDetail, cancelOrder, deleteOrder, getOrderItems } from '@/api/order';
import { useRouter } from 'vue-router';
import { getFileUrl } from '@/api/config';
import { addReview } from '@/api/review';
import request from '@/api/request';
import { updateItemStatus } from '@/api/product';

const userStore = useUserStore();
const router = useRouter();
const orders = ref([]);
const loading = ref(true);
const orderFilter = ref('');
const orderDetailVisible = ref(false);
const currentOrderDetail = reactive({
  orderId: null,
  createTime: '',
  status: '',
  totalPrice: 0,
  sellerName: '',
  sellerAddress: '',
  buyerName: '',
  buyerAddress: '',
  buyerPhone: '',
  items: []
});
const detailLoading = ref(false);

// 评价相关数据
const reviewDialogVisible = ref(false);
const reviewForm = reactive({
  orderId: null,
  itemId: null,
  rating: 5,
  comment: '',
  userId: null
});
const reviewLoading = ref(false);
const currentReviewOrder = ref(null);

// 订单状态映射
const orderStatusMap = {
  'PENDING': '待处理',
  'COMPLETED': '已完成',
  'CANCELLED': '已取消'
};

// 获取订单列表
const fetchOrders = async () => {
  if (!userStore.userId) {
    ElMessage.warning('请先登录');
    return;
  }
  
  loading.value = true;
  try {
    // 使用增强版API获取完整订单列表（包含商品信息）
    const params = {
      buyerRole: true // 添加参数，表明是作为买家查看订单
    };
    
    if (orderFilter.value) {
      params.status = orderFilter.value;
    }
    
    const res = await getFullUserOrders(userStore.userId, params);
    if (res.code === 200) {
      // 过滤掉属于卖家待发货的订单，这些订单应该在待处理页面显示
      orders.value = res.data.list
        .filter(order => !(order.status === 'PENDING' && order.sellerId === userStore.userId))
        .map(order => {
          // 处理订单项，确保数据格式正确
          const orderItems = order.orderItems?.map(item => ({
            itemId: item.itemId,
            itemTitle: item.itemTitle || '未命名商品',
            itemImageUrl: item.itemImageUrl || null,
            price: Number(item.price || 0),
            quantity: Number(item.quantity || 1)
          })) || [];
          
          return {
            orderId: order.orderId,
            createTime: formatDateTime(order.createdAt),
            status: order.status,
            totalPrice: Number(order.totalAmount || 0),
            items: orderItems
          };
        });
      
      console.log('获取到的完整订单数据:', orders.value);
    } else {
      ElMessage.error(res.message || '获取订单列表失败');
    }
  } catch (error) {
    console.error('获取订单列表失败:', error);
    ElMessage.error('获取订单列表失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 查看订单详情
const viewOrderDetail = async (order) => {
  detailLoading.value = true;
  try {
    // 使用增强版API获取完整订单详情
    const res = await getFullOrderDetail(order.orderId);
    if (res.code === 200) {
      const detail = res.data;
      
      // 处理订单项，确保数据格式正确
      const orderItems = detail.orderItems?.map(item => ({
        itemId: item.itemId,
        itemTitle: item.itemTitle || '未命名商品',
        itemImageUrl: item.itemImageUrl || null,
        price: Number(item.price || 0),
        quantity: Number(item.quantity || 1)
      })) || [];
      
      // 更新当前订单详情数据
      Object.assign(currentOrderDetail, {
        orderId: detail.orderId,
        createTime: formatDateTime(detail.createdAt),
        status: detail.status,
        totalPrice: Number(detail.totalAmount || 0),
        sellerName: detail.sellerName || '未知卖家',
        sellerAddress: detail.sellerAddress || '未知地址',
        sellerId: detail.sellerId,
        sellerPhone: '',
        sellerEmail: '',
        buyerName: detail.buyerName || userStore.userInfo.username,
        buyerAddress: detail.buyerAddress || userStore.userInfo.address || '未知地址',
        buyerPhone: detail.buyerPhone || userStore.userInfo.phone || '未知',
        items: orderItems
      });
      
      // 获取卖家详细信息
      if (detail.sellerId) {
        try {
          // 调用API获取卖家信息
          const sellerRes = await request({
            url: `/api/user/${detail.sellerId}`,
            method: 'get'
          });
          
          if (sellerRes.code === 200 && sellerRes.data) {
            // 更新卖家联系信息
            currentOrderDetail.sellerPhone = sellerRes.data.phone || '未提供';
            currentOrderDetail.sellerEmail = sellerRes.data.email || '未提供';
          }
        } catch (sellerError) {
          console.error('获取卖家信息失败:', sellerError);
          // 设置默认值
          currentOrderDetail.sellerPhone = '获取失败';
          currentOrderDetail.sellerEmail = '获取失败';
        }
      }
      
      console.log('获取到的订单详情:', currentOrderDetail);
      
      // 显示订单详情对话框
      orderDetailVisible.value = true;
    } else {
      ElMessage.error(res.message || '获取订单详情失败');
    }
  } catch (error) {
    console.error('获取订单详情失败:', error);
    ElMessage.error('获取订单详情失败，请稍后重试');
  } finally {
    detailLoading.value = false;
  }
};

// 打开评价对话框
const openReviewDialog = (order) => {
  currentReviewOrder.value = order;
  reviewForm.orderId = order.orderId;
  reviewForm.itemId = order.items && order.items.length > 0 ? order.items[0].itemId : null;
  reviewForm.rating = 5;
  reviewForm.comment = '';
  reviewForm.userId = userStore.userId;
  reviewDialogVisible.value = true;
};

// 提交评价
const submitReview = async () => {
  if (!reviewForm.orderId || !reviewForm.itemId) {
    ElMessage.warning('订单或商品信息缺失，无法提交评价');
    return;
  }

  if (!reviewForm.userId) {
    ElMessage.warning('用户未登录，请先登录');
    return;
  }

  if (reviewForm.rating < 1) {
    ElMessage.warning('请至少给出1颗星的评分');
    return;
  }

  reviewLoading.value = true;
  try {
    // 添加更详细的日志输出
    console.log('提交评价数据:', JSON.stringify(reviewForm));
    
    // 判断是否已评价过
    try {
      // 这里可以添加一个请求来检查是否已经评价过该订单
      // 如果后端有提供该API的话
    } catch (checkError) {
      console.warn('检查是否已评价出错:', checkError);
    }
    
    const res = await addReview(reviewForm);
    if (res.code === 200) {
      ElMessage.success('评价提交成功');
      reviewDialogVisible.value = false;
      
      // 刷新订单列表
      fetchOrders();
    } else {
      // 打印更详细的错误信息
      console.error('评价提交返回错误:', res);
      // 根据错误码提供更具体的提示
      if (res.code === 400) {
        ElMessage.warning(res.message || '提交的数据有误，请检查后重试');
      } else if (res.code === 403) {
        ElMessage.warning('您没有权限评价此订单');
      } else if (res.code === 404) {
        ElMessage.warning('订单或商品不存在');
      } else if (res.code === 409) {
        ElMessage.warning('您已经评价过此订单');
      } else {
        ElMessage.error(res.message || '评价提交失败');
      }
    }
  } catch (error) {
    // 打印更详细的错误信息
    console.error('评价提交失败:', error);
    // 添加更多日志，显示错误的详细信息
    if (error.response) {
      console.error('错误状态码:', error.response.status);
      console.error('错误数据:', error.response.data);
      
      // 根据HTTP状态码提供更具体的提示
      if (error.response.status === 400) {
        ElMessage.warning('请求参数有误，请检查后重试');
      } else if (error.response.status === 401) {
        ElMessage.warning('您需要登录后才能评价');
      } else if (error.response.status === 403) {
        ElMessage.warning('您没有权限评价此订单');
      } else if (error.response.status === 404) {
        ElMessage.warning('评价接口不存在，请联系管理员');
      } else if (error.response.status === 500) {
        ElMessage.error('服务器处理评价请求时出错，请稍后重试');
      } else {
        ElMessage.error('评价提交失败，请稍后重试');
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接后重试');
    }
  } finally {
    reviewLoading.value = false;
  }
};

// 获取商品图片URL
const getProductImageUrl = (url) => {
  if (!url) return new URL(`../../assets/images/no-image.png`, import.meta.url).href;
  
  // 如果已经是完整URL，直接返回
  if (url.startsWith('http')) return url;
  
  try {
    // 尝试直接访问MinIO
    const directUrl = getFileUrl(url, true);
    return directUrl;
  } catch (error) {
    console.error('获取图片URL失败:', error);
    // 回退到API代理访问
    return getFileUrl(url, false);
  }
};

// 格式化日期时间
const formatDateTime = (timestamp) => {
  if (!timestamp) return '未知时间';
  
  try {
    // 如果是字符串日期，直接返回
    if (typeof timestamp === 'string') {
      // 可以根据需要进一步处理日期格式
      return timestamp;
    }
    
    // 如果是日期对象或时间戳
    const date = new Date(timestamp);
    return date.toLocaleString();
  } catch (error) {
    console.error('日期格式化错误:', error);
    return '日期错误';
  }
};

// 取消订单
const handleCancelOrder = async (order) => {
  if (!order || !order.orderId) {
    ElMessage.warning('订单信息不完整');
    return;
  }

  try {
    // 获取订单详情，确保有最新的商品信息
    let orderItems = [];
    
    if (order.items && order.items.length > 0) {
      orderItems = order.items;
    } else {
      // 如果订单对象没有商品信息，则获取详情
      const detailRes = await getFullOrderDetail(order.orderId);
      if (detailRes.code === 200 && detailRes.data.orderItems) {
        orderItems = detailRes.data.orderItems;
      }
    }
    
    // 取消订单
    const res = await cancelOrder(order.orderId);
    if (res.code === 200) {
      ElMessage.success('订单已取消');
      
      // 将对应商品状态恢复为在售(ACTIVE)
      if (orderItems && orderItems.length > 0) {
        for (const item of orderItems) {
          if (item.itemId) {
            try {
              await updateItemStatus(item.itemId, 'ACTIVE');
              console.log(`商品 ${item.itemId} 已恢复为在售状态`);
            } catch (error) {
              console.error(`恢复商品状态失败(商品ID: ${item.itemId}):`, error);
            }
          }
        }
      }
      
      // 刷新订单列表
      fetchOrders();
    } else {
      ElMessage.error(res.message || '取消订单失败');
    }
  } catch (error) {
    console.error('取消订单失败:', error);
    ElMessage.error('取消订单失败，请稍后重试');
  }
};

// 删除订单
const handleDeleteOrder = async (order) => {
  if (!order || !order.orderId) {
    ElMessage.warning('订单信息不完整');
    return;
  }

  try {
    const res = await deleteOrder(order.orderId);
    if (res.code === 200) {
      ElMessage.success('订单已删除');
      // 刷新订单列表
      fetchOrders();
    } else {
      ElMessage.error(res.message || '删除订单失败');
    }
  } catch (error) {
    console.error('删除订单失败:', error);
    ElMessage.error('删除订单失败，请稍后重试');
  }
};

// 监听订单筛选变化
watch(orderFilter, () => {
  fetchOrders();
});

onMounted(() => {
  fetchOrders();
});
</script>

<template>
  <div class="user-orders">
    <div class="section-header">
      <h2>我的订单</h2>
      <div class="order-filters">
        <el-select v-model="orderFilter" placeholder="全部订单" size="small">
          <el-option label="全部订单" value=""></el-option>
          <el-option label="待处理" value="PENDING"></el-option>
          <el-option label="已完成" value="COMPLETED"></el-option>
          <el-option label="已取消" value="CANCELLED"></el-option>
        </el-select>
      </div>
    </div>
    
    <el-skeleton :loading="loading" animated :count="3" :throttle="500">
      <template #template>
        <div style="padding: 15px; margin-bottom: 15px; border-radius: 4px; border: 1px solid #ebeef5;">
          <el-skeleton-item variant="p" style="width: 50%" />
          <div style="padding: 10px; background-color: #f9f9f9; margin: 10px 0; border-radius: 4px;">
            <div style="display: flex; align-items: center; margin: 10px 0;">
              <el-skeleton-item variant="image" style="width: 50px; height: 50px; margin-right: 10px;" />
              <div style="flex: 1">
                <el-skeleton-item variant="text" style="width: 60%; margin-bottom: 5px" />
                <el-skeleton-item variant="text" style="width: 30%" />
              </div>
            </div>
          </div>
          <div style="display: flex; justify-content: space-between; margin-top: 10px">
            <el-skeleton-item variant="text" style="width: 20%" />
            <el-skeleton-item variant="button" style="width: 15%" />
          </div>
        </div>
      </template>
    
      <template #default>
        <div v-if="orders.length > 0" class="order-list">
          <el-card v-for="order in orders" :key="order.orderId" class="order-item" shadow="hover">
            <div class="order-header">
              <div class="order-info">
                <span class="order-id">订单号: {{ order.orderId }}</span>
                <span class="order-date">{{ order.createTime }}</span>
              </div>
              <span class="order-status" :class="{'status-pending': order.status === 'PENDING', 'status-completed': order.status === 'COMPLETED', 'status-cancelled': order.status === 'CANCELLED'}">
                {{ orderStatusMap[order.status] || order.status }}
              </span>
            </div>
            
            <div class="order-content">
              <div v-if="order.items && order.items.length > 0">
                <div v-for="item in order.items.slice(0, 2)" :key="item.itemId" class="order-product">
                  <div class="product-image">
                    <img v-if="item.itemImageUrl" :src="getProductImageUrl(item.itemImageUrl)" :alt="item.itemTitle" @error="item.itemImageUrl = null">
                    <img v-else src="../../assets/images/no-image.png" :alt="item.itemTitle">
                  </div>
                  <div class="product-info">
                    <div class="product-title">{{ item.itemTitle || '商品' }}</div>
                    <div class="product-price">¥ {{ item.price ? item.price.toFixed(2) : '0.00' }} × {{ item.quantity || 1 }}</div>
                  </div>
                </div>
                
                <div v-if="order.items.length > 2" class="order-more-items">
                  <el-tag size="small" effect="plain">还有 {{ order.items.length - 2 }} 件商品</el-tag>
                </div>
              </div>
              <div v-else class="no-items">
                <el-empty description="暂无商品详情" :image-size="60"></el-empty>
              </div>
            </div>
            
            <div class="order-footer">
              <div class="order-total">合计: <span class="price">¥ {{ order.totalPrice ? order.totalPrice.toFixed(2) : '0.00' }}</span></div>
              <div class="order-actions">
                <el-button size="small" @click="viewOrderDetail(order)">查看详情</el-button>
                <el-button 
                  v-if="order.status === 'PENDING'" 
                  size="small" 
                  type="warning" 
                  @click="handleCancelOrder(order)"
                >取消订单</el-button>
                <el-button 
                  v-if="order.status === 'CANCELLED'" 
                  size="small" 
                  type="danger" 
                  @click="handleDeleteOrder(order)"
                >删除订单</el-button>
                <el-button 
                  v-if="order.status === 'COMPLETED'" 
                  size="small" 
                  type="primary" 
                  @click="openReviewDialog(order)"
                >评价卖家</el-button>
              </div>
            </div>
          </el-card>
        </div>
        
        <el-empty v-else description="暂无订单记录"></el-empty>
      </template>
    </el-skeleton>
    
    <!-- 订单详情对话框 -->
    <el-dialog
      v-model="orderDetailVisible"
      title="订单详情"
      width="600px"
      center
      destroy-on-close
    >
      <el-skeleton :loading="detailLoading" animated>
        <template #template>
          <div style="padding: 20px">
            <el-skeleton-item variant="p" style="width: 60%" />
            <div style="margin-top: 20px">
              <el-skeleton-item variant="text" style="width: 100%; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 100%; margin-bottom: 10px" />
              <el-skeleton-item variant="text" style="width: 60%; margin-bottom: 10px" />
            </div>
          </div>
        </template>
        
        <template #default>
          <div class="order-detail">
            <div class="detail-header">
              <h3>订单号: {{ currentOrderDetail.orderId }}</h3>
              <div class="detail-status">
                状态: <span :class="`status-${currentOrderDetail.status.toLowerCase()}`">
                  {{ orderStatusMap[currentOrderDetail.status] || currentOrderDetail.status }}
                </span>
              </div>
              <div class="detail-time">下单时间: {{ currentOrderDetail.createTime }}</div>
            </div>
            
            <el-divider>商品信息</el-divider>
            
            <div class="detail-products">
              <div v-if="currentOrderDetail.items && currentOrderDetail.items.length > 0">
                <div v-for="item in currentOrderDetail.items" :key="item.itemId" class="detail-product-item">
                  <div class="detail-product-image">
                    <img v-if="item.itemImageUrl" :src="getProductImageUrl(item.itemImageUrl)" :alt="item.itemTitle" @error="item.itemImageUrl = null">
                    <img v-else src="../../assets/images/no-image.png" :alt="item.itemTitle">
                  </div>
                  <div class="detail-product-info">
                    <div class="detail-product-title">{{ item.itemTitle || '商品' }}</div>
                    <div class="detail-product-price">单价: ¥{{ item.price ? item.price.toFixed(2) : '0.00' }}</div>
                    <div class="detail-product-quantity">数量: {{ item.quantity || 1 }}</div>
                  </div>
                  <div class="detail-product-subtotal">
                    小计: ¥{{ item.quantity && item.price ? (item.quantity * item.price).toFixed(2) : '0.00' }}
                  </div>
                </div>
              </div>
              <div v-else class="empty-detail-products">
                <el-empty description="暂无商品详情" :image-size="60"></el-empty>
              </div>
            </div>
            
            <div class="detail-total">
              订单总价: <span class="price">¥{{ currentOrderDetail.totalPrice ? currentOrderDetail.totalPrice.toFixed(2) : '0.00' }}</span>
            </div>
            
            <el-divider>收货信息</el-divider>
            
            <div class="detail-delivery-info">
              <div class="detail-info-item">收货人: {{ currentOrderDetail.buyerName }}</div>
              <div class="detail-info-item">联系电话: {{ currentOrderDetail.buyerPhone }}</div>
              <div class="detail-info-item">收货地址: {{ currentOrderDetail.buyerAddress }}</div>
            </div>
            
            <el-divider>卖家信息</el-divider>
            
            <div class="detail-seller-info">
              <div class="detail-info-item">卖家: {{ currentOrderDetail.sellerName }}</div>
              <div class="detail-info-item">联系电话: {{ currentOrderDetail.sellerPhone }}</div>
              <div class="detail-info-item">邮箱: {{ currentOrderDetail.sellerEmail }}</div>
              <div class="detail-info-item">地址: {{ currentOrderDetail.sellerAddress }}</div>
            </div>
          </div>
        </template>
      </el-skeleton>
      
      <template #footer>
        <el-button @click="orderDetailVisible = false">关闭</el-button>
        <el-button 
          v-if="currentOrderDetail.status === 'PENDING'"
          type="warning"
          @click="handleCancelOrder({orderId: currentOrderDetail.orderId})"
        >取消订单</el-button>
        <el-button 
          v-if="currentOrderDetail.status === 'CANCELLED'"
          type="danger"
          @click="handleDeleteOrder({orderId: currentOrderDetail.orderId})"
        >删除订单</el-button>
      </template>
    </el-dialog>
    
    <!-- 评价对话框 -->
    <el-dialog
      v-model="reviewDialogVisible"
      title="评价卖家"
      width="500px"
      center
      destroy-on-close
    >
      <div v-if="currentReviewOrder" class="review-form">
        <p class="review-order-info">
          订单号: {{ currentReviewOrder.orderId }}
          <span class="review-order-date">{{ currentReviewOrder.createTime }}</span>
        </p>
        
        <div class="review-products">
          <div v-if="currentReviewOrder.items && currentReviewOrder.items.length > 0" class="review-product-item">
            <div class="review-product-image">
              <img v-if="currentReviewOrder.items[0].itemImageUrl" :src="getProductImageUrl(currentReviewOrder.items[0].itemImageUrl)" :alt="currentReviewOrder.items[0].itemTitle" @error="currentReviewOrder.items[0].itemImageUrl = null">
              <img v-else src="../../assets/images/no-image.png" :alt="currentReviewOrder.items[0].itemTitle">
            </div>
            <div class="review-product-info">
              <div class="review-product-title">{{ currentReviewOrder.items[0].itemTitle || '商品' }}</div>
              <div v-if="currentReviewOrder.items.length > 1" class="review-product-more">
                等 {{ currentReviewOrder.items.length }} 件商品
              </div>
            </div>
          </div>
        </div>
        
        <div class="review-input-form">
          <div class="review-rating">
            <div class="rating-label">评分:</div>
            <el-rate
              v-model="reviewForm.rating"
              allow-half
              :max="5"
              :texts="['很差', '较差', '一般', '较好', '很好']"
              show-text
            />
          </div>
          
          <div class="review-comment">
            <div class="comment-label">评价内容:</div>
            <el-input
              v-model="reviewForm.comment"
              type="textarea"
              :rows="4"
              placeholder="请输入您对卖家的评价..."
              resize="none"
              maxlength="500"
              show-word-limit
            />
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview" :loading="reviewLoading">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-orders {
  width: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.section-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.order-filters {
  min-width: 120px;
}

.order-list {
  width: 100%;
}

.order-item {
  margin-bottom: 15px;
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
  width: 100%;
  padding-bottom: 10px;
  border-bottom: 1px dashed #ebeef5;
}

.order-info {
  display: flex;
  align-items: center;
  flex: 1;
}

.order-id {
  font-weight: bold;
  font-size: 15px;
  margin-right: 10px;
}

.order-date {
  color: #909399;
  font-size: 13px;
}

.order-status {
  font-weight: 600;
}

.status-pending {
  color: #E6A23C;
}

.status-completed {
  color: #67C23A;
}

.status-cancelled {
  color: #F56C6C;
}

.order-content {
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
  width: 100%;
  min-height: 60px;
}

.no-items {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 60px;
}

.order-product {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  width: 100%;
}

.order-product:last-child {
  margin-bottom: 0;
}

.order-more-items {
  padding: 5px 0;
  text-align: center;
}

.product-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  margin-right: 10px;
  border: 1px solid #eee;
  background-color: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  flex: 1;
}

.product-title {
  font-size: 14px;
  margin-bottom: 5px;
  color: #333;
}

.product-price {
  font-size: 13px;
  color: #909399;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 5px;
  width: 100%;
  margin-top: 5px;
}

.order-total {
  font-size: 14px;
}

.price {
  color: #FF5000;
  font-weight: bold;
  font-size: 16px;
  margin-left: 2px;
}

.order-actions {
  display: flex;
  gap: 8px;
}

/* 订单详情样式 */
.order-detail {
  padding: 0 10px;
}

.detail-header {
  margin-bottom: 15px;
}

.detail-header h3 {
  margin: 0 0 10px 0;
  font-size: 18px;
  color: #333;
}

.detail-status {
  margin-bottom: 5px;
  font-size: 14px;
}

.detail-time {
  color: #909399;
  font-size: 14px;
}

.detail-products {
  margin: 15px 0;
  min-height: 120px;
}

.empty-detail-products {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 120px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.detail-product-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #f9f9f9;
  border-radius: 4px;
  margin-bottom: 10px;
}

.detail-product-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  margin-right: 15px;
  border: 1px solid #eee;
  background-color: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-product-info {
  flex: 1;
}

.detail-product-title {
  font-size: 15px;
  margin-bottom: 5px;
  font-weight: 500;
}

.detail-product-price, .detail-product-quantity {
  font-size: 13px;
  color: #606266;
  margin-bottom: 3px;
}

.detail-product-subtotal {
  font-size: 14px;
  font-weight: bold;
  color: #FF5000;
  margin-left: 15px;
}

.detail-total {
  text-align: right;
  font-size: 16px;
  margin: 15px 0;
  font-weight: 500;
}

.detail-delivery-info, .detail-seller-info {
  margin: 15px 0;
}

.detail-info-item {
  margin-bottom: 8px;
  font-size: 14px;
}

@media (max-width: 768px) {
  .detail-product-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .detail-product-image {
    margin-bottom: 10px;
  }
  
  .detail-product-subtotal {
    margin-left: 0;
    margin-top: 5px;
  }
}

/* 评价对话框样式 */
.review-form {
  padding: 10px;
}

.review-order-info {
  font-weight: 600;
  margin-bottom: 15px;
  font-size: 15px;
  display: flex;
  justify-content: space-between;
}

.review-order-date {
  color: #909399;
  font-weight: normal;
  font-size: 13px;
}

.review-products {
  background-color: #f9f9f9;
  border-radius: 4px;
  padding: 10px;
  margin-bottom: 15px;
}

.review-product-item {
  display: flex;
  align-items: center;
}

.review-product-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  margin-right: 10px;
  border: 1px solid #eee;
  background-color: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
}

.review-product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.review-product-title {
  font-size: 14px;
  margin-bottom: 3px;
}

.review-product-more {
  font-size: 13px;
  color: #909399;
}

.review-input-form {
  margin-top: 20px;
}

.review-rating {
  margin-bottom: 15px;
}

.rating-label, .comment-label {
  margin-bottom: 8px;
  font-weight: 500;
  font-size: 14px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .review-order-info {
    flex-direction: column;
  }
  
  .review-order-date {
    margin-top: 5px;
  }
}
</style> 