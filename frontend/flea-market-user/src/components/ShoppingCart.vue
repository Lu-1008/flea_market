<template>
  <div class="shopping-cart">
    <div class="cart-header">
      <h2>我的购物车</h2>
      <div class="cart-summary" v-if="!loading && cartItems.length > 0">
        <span>共 {{ totalItems }} 件商品</span>
        <span class="price-total">合计: ¥{{ totalPrice.toFixed(2) }}</span>
        <el-button type="primary" @click="handleCheckout" :loading="checkoutLoading">结算</el-button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>
    
    <div v-else-if="cartItems.length === 0" class="empty-cart">
      <el-empty description="购物车是空的" :image-size="200">
        <template #description>
          <p>您的购物车还没有商品，快去挑选心仪的商品吧！</p>
        </template>
        <el-button type="primary" @click="$router.push('/')">去购物</el-button>
      </el-empty>
    </div>
    
    <div v-else class="cart-items">
      <!-- 按卖家分组的购物车商品 -->
      <div v-for="(group, sellerId) in groupedItems" :key="sellerId" class="seller-group">
        <div class="seller-info">
          <el-checkbox 
            v-model="sellerSelection[sellerId]" 
            @change="(val) => handleSellerCheckboxChange(sellerId, val)"
          >
            <span class="seller-name">卖家：{{ group.sellerName }}</span>
          </el-checkbox>
        </div>
        
        <el-table
          :data="group.items"
          style="width: 100%"
          @selection-change="(selection) => handleSelectionChange(selection, sellerId)"
        >
          <el-table-column type="selection" width="55" />
          
          <el-table-column label="商品" min-width="300">
            <template #default="scope">
              <div class="item-info">
                <el-image 
                  :src="getItemImage(scope.row)" 
                  fit="cover" 
                  class="item-image"
                  @error="handleImageError"
                />
                <div class="item-details">
                  <div class="item-title">{{ scope.row.itemTitle }}</div>
                  <div class="item-price">¥{{ scope.row.price.toFixed(2) }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column label="数量" width="100" align="center">
            <template #default>
              <span class="quantity">1</span>
            </template>
          </el-table-column>
          
          <el-table-column label="小计" width="120">
            <template #default="scope">
              <span class="subtotal">
                ¥{{ scope.row.price.toFixed(2) }}
              </span>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button 
                size="small" 
                type="danger" 
                text
                @click="handleRemoveItem(scope.row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <!-- 结算区域 -->
      <div class="checkout-area">
        <div class="checkout-info">
          <el-checkbox 
            v-model="selectAll" 
            @change="handleSelectAllChange"
          >
            全选
          </el-checkbox>
          <span class="selected-summary" v-if="selectedItems.length > 0">
            已选择 {{ selectedItems.length }} 件商品
          </span>
          <span class="selected-price">
            合计: <span class="price">¥{{ selectedTotalPrice.toFixed(2) }}</span>
          </span>
          <el-button 
            type="primary" 
            size="large" 
            @click="handleCheckoutSelected" 
            :disabled="selectedItems.length === 0"
            :loading="checkoutLoading"
          >
            结算
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFileUrl } from '@/api/config'
import { getCart, removeCartItem, checkout } from '@/api/cart'
import { useRouter } from 'vue-router'

export default {
  name: 'ShoppingCart',
  props: {
    userId: {
      type: Number,
      required: true
    }
  },
  setup(props) {
    const router = useRouter()
    const cartItems = ref([])
    const loading = ref(false)
    const checkoutLoading = ref(false)
    const selectAll = ref(false)
    const sellerSelection = ref({})
    const selectedItems = ref([])
    
    // 加载购物车数据
    const loadCartData = async () => {
      loading.value = true
      try {
        const res = await getCart(props.userId)
        if (res.code === 200 && res.data) {
          cartItems.value = res.data.cartItems || []
          // 初始化卖家选择状态
          initSellerSelection()
        }
      } catch (error) {
        console.error('获取购物车数据失败:', error)
        ElMessage.error('获取购物车数据失败')
      } finally {
        loading.value = false
      }
    }
    
    // 初始化卖家选择状态
    const initSellerSelection = () => {
      const sellers = {}
      cartItems.value.forEach(item => {
        sellers[item.sellerId] = false
      })
      sellerSelection.value = sellers
    }
    
    // 按卖家分组商品
    const groupedItems = computed(() => {
      const groups = {}
      
      cartItems.value.forEach(item => {
        if (!groups[item.sellerId]) {
          groups[item.sellerId] = {
            sellerName: item.sellerName,
            items: []
          }
        }
        groups[item.sellerId].items.push(item)
      })
      
      return groups
    })
    
    // 计算总价
    const totalPrice = computed(() => {
      return cartItems.value.reduce((sum, item) => sum + item.price, 0)
    })

    // 计算选中商品的总价
    const selectedTotalPrice = computed(() => {
      return selectedItems.value.reduce((sum, item) => sum + item.price, 0)
    })

    // 计算商品总数
    const totalItems = computed(() => {
      return cartItems.value.length
    })
    
    // 处理图片加载错误
    const handleImageError = (e) => {
      e.target.src = new URL('../assets/images/no-image.png', import.meta.url).href
    }

    // 获取商品图片
    const getItemImage = (item) => {
      if (!item.itemImage) {
        return new URL('../assets/images/no-image.png', import.meta.url).href
      }
      
      // 优先尝试使用直接访问方式，添加noCache=true参数防止缓存
      try {
        return getFileUrl(item.itemImage, true, true)
      } catch (e) {
        console.error('加载图片失败:', e)
        return new URL('../assets/images/no-image.png', import.meta.url).href
      }
    }
    
    // 处理移除商品
    const handleRemoveItem = (item) => {
      ElMessageBox.confirm('确认从购物车移除该商品?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await removeCartItem(item.cartItemId)
          ElMessage.success('商品已移除')
          loadCartData() // 重新加载购物车数据
        } catch (error) {
          console.error('移除商品失败:', error)
          ElMessage.error('移除商品失败')
        }
      }).catch(() => {})
    }
    
    // 处理选择变更
    const handleSelectionChange = (selection, sellerId) => {
      // 更新所选商品列表
      const sellerItems = groupedItems.value[sellerId].items
      const allSelected = sellerItems.length === selection.length
      
      // 更新卖家选择状态
      sellerSelection.value[sellerId] = allSelected
      
      // 更新已选商品列表
      updateSelectedItems()
    }
    
    // 处理卖家复选框变化
    const handleSellerCheckboxChange = (sellerId, checked) => {
    }
    
    // 更新已选商品列表
    const updateSelectedItems = () => {
      selectedItems.value = cartItems.value.filter(item => 
        sellerSelection.value[item.sellerId]
      )
      
      // 更新全选状态
      selectAll.value = Object.values(sellerSelection.value).every(v => v) &&
                    Object.keys(sellerSelection.value).length > 0
    }
    
    // 处理全选
    const handleSelectAllChange = (checked) => {
      // 更新所有卖家的选择状态
      for (const sellerId in sellerSelection.value) {
        sellerSelection.value[sellerId] = checked
      }
      
      // 更新已选商品列表
      updateSelectedItems()
    }
    
    // 处理结算所有商品
    const handleCheckout = () => {
      handleCheckoutItems(null) // null表示结算所有商品
    }
    
    // 处理结算选中商品
    const handleCheckoutSelected = () => {
      if (selectedItems.value.length === 0) {
        ElMessage.warning('请选择要结算的商品')
        return
      }
      
      const cartItemIds = selectedItems.value.map(item => item.cartItemId)
      handleCheckoutItems(cartItemIds)
    }
    
    // 处理结算
    const handleCheckoutItems = async (cartItemIds) => {
      checkoutLoading.value = true
      try {
        const res = await checkout(props.userId, cartItemIds)
        if (res.code === 200) {
          ElMessage.success('结算成功')
          // 跳转到订单列表页面
          router.push('/user/orders')
        }
      } catch (error) {
        console.error('结算失败:', error)
        ElMessage.error('结算失败，请重试')
      } finally {
        checkoutLoading.value = false
      }
    }
    
    // 监听userId变化，重新加载数据
    watch(() => props.userId, (newVal) => {
      if (newVal) {
        loadCartData()
      }
    })
    
    // 加载数据
    onMounted(() => {
      if (props.userId) {
        loadCartData()
      }
    })

    return {
      cartItems,
      loading,
      checkoutLoading,
      totalPrice,
      totalItems,
      groupedItems,
      selectAll,
      sellerSelection,
      selectedItems,
      selectedTotalPrice,
      handleImageError,
      getItemImage,
      handleRemoveItem,
      handleSelectionChange,
      handleSellerCheckboxChange,
      handleSelectAllChange,
      handleCheckout,
      handleCheckoutSelected
    }
  }
}
</script>

<style scoped>
.shopping-cart {
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.cart-summary {
  display: flex;
  align-items: center;
  gap: 15px;
}

.price-total {
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
}

.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
}

.loading-state {
  padding: 20px;
}

.seller-group {
  margin-bottom: 30px;
}

.seller-info {
  padding: 10px;
  margin-bottom: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.seller-name {
  font-weight: bold;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.item-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.item-details {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.item-title {
  font-size: 14px;
  line-height: 1.4;
  max-height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-price {
  color: #f56c6c;
  font-weight: bold;
}

.quantity {
  font-size: 16px;
  font-weight: bold;
  color: #606266;
}

.subtotal {
  color: #f56c6c;
  font-weight: bold;
}

.checkout-area {
  margin-top: 20px;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.checkout-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.selected-summary {
  margin-left: 10px;
  color: #606266;
}

.selected-price {
  margin-left: auto;
  font-size: 14px;
}

.price {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
  margin-left: 5px;
}
</style>