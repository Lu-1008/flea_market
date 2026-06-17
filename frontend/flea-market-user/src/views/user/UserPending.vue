<script setup>
import { ref, onMounted, computed, reactive } from 'vue';
import { useUserStore } from '@/store/user';
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus';
import { getFullPendingOrders, shipOrder, getFullOrderDetail } from '@/api/order';
import { formatTime } from '@/utils/format';
import { Picture } from '@element-plus/icons-vue';
import { getFileUrl } from '@/api/config';
import { useRouter, useRoute } from 'vue-router';
import { getUserById } from '@/api/auth';

const router = useRouter();
const userStore = useUserStore();
const pendingOrders = ref([]);
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

// 订单详情相关
const orderDetailVisible = ref(false);
const detailLoading = ref(false);
const currentOrderDetail = reactive({
  orderId: '',
  createTime: '',
  status: '',
  totalPrice: 0,
  buyerName: '',
  buyerAddress: '',
  buyerPhone: '',
  buyerEmail: '',
  buyerId: '',
  items: []
});

// 查询地址栏参数，检查是否有指定卖家ID
const route = useRoute();

// 获取待处理订单列表
const fetchPendingOrders = async () => {
  try {
    loading.value = true;
    
    // 使用新的API获取完整的待处理订单信息
    const sellerId = userStore.userId;
    if (!sellerId) {
      ElMessage.warning('用户信息无效，请重新登录');
      loading.value = false;
      return;
    }
    
    console.log('获取待处理订单，卖家ID:', sellerId);
    
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    };
    
    const res = await getFullPendingOrders(sellerId, params);
    console.log('待处理订单API响应:', res);
    
    if (res && res.code === 200) {
      // 直接使用处理好的数据
      const orders = res.data.list || [];
      total.value = res.data.total || 0;
      
      // 再次确保仅显示当前用户作为卖家的订单
      const filteredOrders = orders.filter(order => {
        return order.sellerId === sellerId || order.sellerId === Number(sellerId);
      });
      
      if (filteredOrders.length !== orders.length) {
        console.warn(`过滤掉了 ${orders.length - filteredOrders.length} 个不属于当前用户的订单`);
        total.value = filteredOrders.length;
      }
      
      // 格式化和处理订单数据
      pendingOrders.value = filteredOrders.map(order => {
        // 确保有标准格式的商品信息
        let mainItem = { title: '未知商品', price: 0, imageUrl: '' };
        
        // 从orderItems中获取第一个商品信息
        if (order.orderItems && order.orderItems.length > 0) {
          const item = order.orderItems[0];
          mainItem = {
            title: item.itemTitle || '未命名商品',
            price: item.price || 0,
            imageUrl: item.itemImageUrl || ''
          };
        }
        
        // 处理日期格式
        let createTime;
        try {
          createTime = order.createdAt ? new Date(order.createdAt) : new Date();
        } catch (e) {
          createTime = new Date();
        }
        
        // 返回标准格式订单
        return {
          orderId: order.orderId,
          itemImgUrl: mainItem.imageUrl,
          itemTitle: mainItem.title,
          price: mainItem.price,
          buyerName: order.buyerName || '买家',
          createTime: createTime,
          amount: order.totalAmount || mainItem.price || 0
        };
      });
      
      console.log('处理后的订单列表:', pendingOrders.value);
    } else {
      console.error('获取订单列表API返回错误:', res);
      ElMessage.error(res?.message || '获取订单列表失败');
    }
  } catch (error) {
    console.error('获取待处理订单失败:', error);
    ElMessage.error('获取待处理订单失败');
  } finally {
    loading.value = false;
  }
};

// 处理订单发货
const handleShipOrder = async (order, fromDialog = false) => {
  try {
    await ElMessageBox.confirm(
      '确认已经发货该商品吗？确认后订单将被标记为"已完成"状态。',
      '发货确认',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    console.log('开始发货，订单ID:', order.orderId);
    
    // 发货前检查订单状态
    try {
      const res = await shipOrder(order.orderId);
      console.log('发货API响应:', res);
      
      if (res && res.code === 200) {
        ElNotification({
          title: '发货成功',
          message: '您已成功确认发货，订单已完成',
          type: 'success'
        });
        
        // 如果是从对话框调用的，关闭对话框
        if (fromDialog) {
          orderDetailVisible.value = false;
        }
        
        // 重新加载订单
        fetchPendingOrders();
      } else {
        // API返回了错误信息
        ElMessage.error(res?.message || '发货失败，请稍后重试');
      }
    } catch (apiError) {
      console.error('API调用出错:', apiError);
      
      // 处理服务器错误
      if (apiError.response && apiError.response.status === 500) {
        ElMessage.error('服务器处理请求时出错，请稍后重试');
      } else if (apiError.message) {
        ElMessage.error(apiError.message);
      } else {
        ElMessage.error('发货失败，请稍后重试');
      }
    }
  } catch (error) {
    // 忽略用户取消操作
    if (error !== 'cancel') {
      console.error('发货操作失败:', error);
      
      // 提供更明确的错误信息
      if (error.message && error.message.includes('无效的订单状态')) {
        ElMessage.error('无法更新订单状态，订单可能已被处理');
      } else {
        ElMessage.error(typeof error === 'string' ? error : '发货操作失败，请稍后重试');
      }
    }
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
        createTime: formatTime(new Date(detail.createdAt)),
        status: detail.status,
        totalPrice: Number(detail.totalAmount || 0),
        buyerName: detail.buyerName || '未知买家',
        buyerAddress: detail.buyerAddress || '未知地址',
        buyerPhone: detail.buyerPhone || '未知',
        buyerEmail: detail.buyerEmail || '未知',
        buyerId: detail.buyerId,
        items: orderItems
      });
      
      // 获取买家详细信息
      if (detail.buyerId) {
        try {
          // 调用API获取买家信息
          const buyerRes = await getUserById(detail.buyerId);
          
          if (buyerRes.code === 200 && buyerRes.data) {
            // 更新买家联系信息
            currentOrderDetail.buyerName = buyerRes.data.username || currentOrderDetail.buyerName;
            currentOrderDetail.buyerPhone = buyerRes.data.phone || currentOrderDetail.buyerPhone;
            currentOrderDetail.buyerEmail = buyerRes.data.email || currentOrderDetail.buyerEmail;
            currentOrderDetail.buyerAddress = buyerRes.data.address || currentOrderDetail.buyerAddress;
          }
        } catch (buyerError) {
          console.error('获取买家信息失败:', buyerError);
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

// 页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page;
  fetchPendingOrders();
};

// 计算订单是否为空
const isEmpty = computed(() => {
  return !loading.value && pendingOrders.value.length === 0;
});

// 检查用户是否有卖家权限 - 简单判断，如果用户有商品则认为是卖家
const isSellerRole = ref(true);

// 检查用户是否是卖家
const checkSellerRole = async () => {
  try {
    // 此处可以调用API检查用户是否有发布商品，作为简单的卖家判断
    // 或者检查用户角色信息
    // 暂时默认为true，实际项目中应该根据后端API判断
    isSellerRole.value = true;
  } catch (error) {
    console.error('检查用户卖家角色失败:', error);
    isSellerRole.value = false;
  }
};

// 页面加载时获取订单
onMounted(() => {
  checkSellerRole();
  fetchPendingOrders();
});

// 获取订单商品图片URL
const getOrderItemImageUrl = (order) => {
  if (!order.itemImgUrl) return '';
  
  try {
    // 优先使用MinIO直接访问，添加noCache=true防止缓存
    return getFileUrl(order.itemImgUrl, true, true);
  } catch (e) {
    console.error('加载商品图片失败:', e);
    return '';
  }
};

// 检查日期是否有效
const isValidDate = (date) => {
  return date instanceof Date && !isNaN(date.getTime());
};

// 获取商品图片URL
const getProductImageUrl = (url) => {
  if (!url) return new URL(`../../assets/images/no-image.png`, import.meta.url).href;
  
  // 如果已经是完整URL，直接返回
  if (url.startsWith('http')) return url;
  
  try {
    // 尝试直接访问MinIO，添加noCache=true防止缓存
    const directUrl = getFileUrl(url, true, true);
    return directUrl;
  } catch (error) {
    console.error('获取图片URL失败:', error);
    // 回退到API代理访问，同样添加noCache=true
    return getFileUrl(url, false, true);
  }
};
</script>

<template>
  <div class="user-pending-container">
    <h2 class="page-title">待处理订单</h2>
    
    <!-- 页面说明信息 -->
    <el-alert
      title="这里显示的是您作为卖家需要发货的订单"
      type="info"
      description="当买家购买您的商品后，订单会显示在此页面，请及时处理发货。确认发货后，订单将被标记为已完成状态。"
      show-icon
      :closable="true"
      style="margin-bottom: 20px"
    />
    
    <div class="pending-content">
      <el-table
        v-loading="loading"
        :data="pendingOrders"
        style="width: 100%"
        :header-cell-style="{backgroundColor: '#f5f7fa', color: '#666'}"
      >
        <el-table-column label="订单号" min-width="180">
          <template #default="scope">
            <span class="order-id">{{ scope.row.orderId }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="商品信息" min-width="280">
          <template #default="scope">
            <div class="item-info">
              <el-image 
                :src="getOrderItemImageUrl(scope.row)"
                fit="cover" 
                class="item-image"
                :preview-src-list="scope.row.itemImgUrl ? [scope.row.itemImgUrl] : []"
              >
                <template #error>
                  <div class="item-no-image">
                    <el-icon><Picture /></el-icon>
                    <div>暂无图片</div>
                  </div>
                </template>
              </el-image>
              <div class="item-details">
                <div class="item-title">{{ scope.row.itemTitle }}</div>
                <div class="item-price">￥{{ scope.row.price }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="买家" min-width="120">
          <template #default="scope">
            {{ scope.row.buyerName }}
          </template>
        </el-table-column>
        
        <el-table-column label="下单时间" min-width="180">
          <template #default="scope">
            <span v-if="isValidDate(scope.row.createTime)">{{ formatTime(scope.row.createTime) }}</span>
            <span v-else>{{ formatTime(new Date()) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="金额" min-width="120">
          <template #default="scope">
            <span class="amount">￥{{ scope.row.amount }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" fixed="right" width="210">
          <template #default="scope">
            <div class="operation-buttons">
              <el-button 
                type="info" 
                size="small" 
                @click="viewOrderDetail(scope.row)"
              >
                查看详情
              </el-button>
              <el-button 
                type="primary" 
                size="small" 
                @click="handleShipOrder(scope.row)"
              >
                确认发货
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 空状态 -->
      <el-empty 
        v-if="isEmpty" 
        description="暂无待处理订单" 
        :image-size="200"
      >
        <template #description>
          <p>您当前没有需要发货的订单</p>
          <p class="empty-sub-desc">当买家购买您的商品后，相关订单将显示在此页面等待您发货</p>
        </template>
      </el-empty>
      
      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
    
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
            
            <el-divider>买家信息（发货参考）</el-divider>
            
            <div class="detail-buyer-info">
              <div class="detail-info-item">收货人: {{ currentOrderDetail.buyerName }}</div>
              <div class="detail-info-item">联系电话: {{ currentOrderDetail.buyerPhone }}</div>
              <div class="detail-info-item">电子邮箱: {{ currentOrderDetail.buyerEmail }}</div>
              <div class="detail-info-item">收货地址: {{ currentOrderDetail.buyerAddress }}</div>
            </div>
          </div>
        </template>
      </el-skeleton>
      
      <template #footer>
        <el-button @click="orderDetailVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleShipOrder(pendingOrders.find(o => o.orderId === currentOrderDetail.orderId), true)">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-pending-container {
  width: 100%;
}

.page-title {
  font-size: 20px;
  color: #333;
  margin-bottom: 20px;
  font-weight: 600;
}

.empty-sub-desc {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.item-info {
  display: flex;
  align-items: center;
}

.item-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  object-fit: cover;
}

.item-no-image {
  width: 60px;
  height: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  border-radius: 4px;
  color: #909399;
  font-size: 12px;
}

.item-no-image{
  font-size: 20px;
  margin-bottom: 4px;
}

.item-details {
  margin-left: 10px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.item-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.item-price {
  font-size: 14px;
  color: #ff6a2c;
  font-weight: 600;
}

.amount {
  font-size: 14px;
  color: #ff6a2c;
  font-weight: 600;
}

.order-id {
  font-family: monospace;
  color: #666;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

/* 新增的样式 */
.operation-buttons {
  display: flex;
  gap: 8px;
}

/* 订单详情样式 */
.order-detail {
  padding: 10px;
}

.detail-header {
  margin-bottom: 15px;
}

.detail-header h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  font-weight: 600;
}

.detail-time {
  font-size: 14px;
  color: #909399;
}

.detail-products {
  margin-bottom: 15px;
}

.detail-product-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.detail-product-item:last-child {
  border-bottom: none;
}

.detail-product-image {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
}

.detail-product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-product-info {
  flex: 1;
  margin-left: 10px;
}

.detail-product-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
}

.detail-product-price, .detail-product-quantity {
  font-size: 13px;
  color: #606266;
}

.detail-product-subtotal {
  font-size: 14px;
  color: #ff6a2c;
  font-weight: 600;
}

.detail-total {
  text-align: right;
  font-size: 16px;
  margin: 10px 0;
}

.detail-total .price {
  color: #ff6a2c;
  font-weight: 600;
}

.detail-buyer-info {
  padding: 10px 0;
}

.detail-info-item {
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.empty-detail-products {
  padding: 20px 0;
  text-align: center;
}
</style> 