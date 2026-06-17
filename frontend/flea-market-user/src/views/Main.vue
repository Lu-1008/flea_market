<script setup>
import { ref, onMounted, watch, onUnmounted, nextTick } from 'vue'
import CommonHeader from '../components/CommonHeader.vue'
import CommonCategory from '../components/CommonCategory.vue'
import CartIconIcon from '../components/icons/CartIcon.vue'
import ProductDetail from '../components/ProductDetail.vue'
import AddProductDialog from '../components/AddProductDialog.vue'
import { Plus, ArrowUp, ShoppingCart } from '@element-plus/icons-vue'
import { getProducts, getProductsByCategory, getProductById } from '@/api/product'
import { getFileUrl } from '@/api/config'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import { addToCart as addItemToCart } from '@/api/cart'

// 用户状态
const userStore = useUserStore()
const router = useRouter()

// 商品数据
const products = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const currentCategoryId = ref(null)
const searchKeyword = ref('')

// 控制骨架屏加载状态
const loading = ref(false)

// 控制回顶部按钮显示
const showBackTop = ref(false)

// 控制无限滚动的标记
const allLoaded = ref(false)
const scrollThrottleTimer = ref(null)

// 商品详情相关
const detailVisible = ref(false)
const currentProduct = ref(null)
const detailLoading = ref(false)

// 添加商品对话框
const addProductVisible = ref(false)

// 添加一个新的标志变量用于直接购买
const shouldShowBuyDialog = ref(false)

// 获取商品列表
const fetchProducts = async (reset = true) => {
  if (reset) {
    currentPage.value = 1
    products.value = []
    allLoaded.value = false
  }
  
  loading.value = true
  
  try {
    let res
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      status: 'ACTIVE' // 只获取上架的商品
    }
    
    // 根据分类ID获取商品或获取全部商品
    if (currentCategoryId.value) {
      res = await getProductsByCategory(currentCategoryId.value, params)
    } else {
      // 如果有搜索关键词，则添加到参数中
      if (searchKeyword.value) {
        params.title = searchKeyword.value
      }
      res = await getProducts(params)
    }
    
    if (res.code === 200) {
      // 检查返回数据结构
      if (res.data && res.data.list && Array.isArray(res.data.list)) {
        const newItems = res.data.list || []
        
        if (reset) {
          products.value = newItems
        } else {
          products.value = [...products.value, ...newItems]
        }
        
        total.value = res.data.total || 0
        
        // 检查是否已加载全部数据
        if (products.value.length >= total.value) {
          allLoaded.value = true
        }
      } else {
        throw new Error('返回数据格式不正确')
      }
    } else {
      throw new Error(res.message || '获取商品失败')
    }
  } catch (error) {
    console.error('获取商品列表失败:', error)
    ElMessage.error('获取商品列表失败，请刷新重试')
    
    // 如果是首次加载，使用静态数据作为备用
  } finally {
    loading.value = false
  }
}

// 加载更多商品
const loadMore = async () => {
  if (loading.value || allLoaded.value) return
  
  if (products.value.length >= total.value) {
    // 只在第一次达到全部商品时提示
    if (!allLoaded.value) {
      ElMessage.info('已加载全部商品')
      allLoaded.value = true
    }
    return
  }
  
  currentPage.value++
  await fetchProducts(false)
}

// 处理分类变化
const handleCategoryChange = (categoryId) => {
  currentCategoryId.value = categoryId
  // 重置搜索关键词
  if (searchKeyword.value) {
    searchKeyword.value = ''
  }
  fetchProducts()
}

// 处理搜索
const handleSearch = (keyword) => {
  searchKeyword.value = keyword
  fetchProducts()
}

// 打开商品详情
const openProductDetail = async (product) => {
  try {
    // 先设置基本信息，以便快速显示
    currentProduct.value = product
    detailVisible.value = true
    
    // 总是加载详细信息以确保数据完整性
    detailLoading.value = true
    
    // 发起API请求获取完整商品信息
    const res = await getProductById(product.itemId)
    
    if (res.code === 200 && res.data) {
      // 合并详细信息
      currentProduct.value = { ...product, ...res.data }
      console.log('获取到商品详情成功:', currentProduct.value)
    } else {
      throw new Error(res.message || '获取商品详情失败')
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
    ElMessage.error('获取商品详情失败，请稍后重试')
  } finally {
    detailLoading.value = false
  }
}

// 关闭商品详情
const closeProductDetail = () => {
  try {
    // 先重置状态
    shouldShowBuyDialog.value = false
    
    // 使用nextTick确保状态更新后再关闭对话框
    nextTick(() => {
      detailVisible.value = false
      currentProduct.value = null
    })
  } catch (error) {
    console.error('关闭商品详情出错:', error)
    // 如果出错，强制重置所有相关状态
    shouldShowBuyDialog.value = false
    detailVisible.value = false
    currentProduct.value = null
  }
}

// 加入购物车方法
const addToCart = async (product, event) => {
  // 阻止事件冒泡，避免同时触发卡片点击事件
  event.stopPropagation()
  
  // 检查用户是否已登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再将商品加入购物车')
    return
  }
  
  // 不能添加自己的商品到购物车
  if (userStore.userId === product.userId) {
    ElMessage.warning('不能将自己的商品加入购物车')
    return
  }
  
  try {
    loading.value = true
    const res = await addItemToCart(userStore.userId, product.itemId, 1)
    
    if (res.code === 200) {
      ElMessage.success('商品已成功加入购物车')
      
      // 触发事件，通知其他组件更新购物车数量
      if (window.$emitter) {
        window.$emitter.emit('update-cart-count')
      }
    } else {
      ElMessage.error(res.message || '加入购物车失败')
    }
  } catch (error) {
    console.error('加入购物车失败:', error)
    ElMessage.error('加入购物车失败，请重试')
  } finally {
    loading.value = false
  }
}

// 购买商品
const buyProduct = (product, event) => {
  // 阻止事件冒泡，避免同时触发卡片点击事件
  event.stopPropagation()
  
  console.log('购买商品:', product)

  // 检查用户是否已登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再购买商品')
    return
  }
  
  // 检查不能购买自己的商品
  if (userStore.userId === product.userId) {
    ElMessage.warning('不能购买自己发布的商品')
    return
  }
  
  // 检查商品状态是否为可购买
  if (product.status && product.status !== 'ACTIVE') {
    ElMessage.warning(`商品当前状态不可购买`)
    return
  }
  
  // 打开商品详情，然后调用购买功能
  currentProduct.value = product
  detailVisible.value = true
  shouldShowBuyDialog.value = true
  
  // 获取完整商品信息
  getProductById(product.itemId).then(res => {
    if (res.code === 200 && res.data) {
      currentProduct.value = { ...product, ...res.data }
    }
  }).catch(error => {
    console.error('获取商品详情失败:', error)
    ElMessage.error('获取商品详情失败')
    // 失败时不影响显示购买对话框
  })
}

// 打开购物车方法
const viewCart = () => {
  console.log('查看购物车')
  // 检查用户是否已登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再查看购物车')
    return
  }
  
  // 跳转到购物车页面
  router.push('/cart')
}

// 回到顶部方法
const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

// 处理"我要卖"点击事件
const handlePublish = () => {
  // 检查用户是否已登录
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再发布商品')
    return
  }
  
  // 打开添加商品对话框
  addProductVisible.value = true
}

// 商品添加成功回调
const handleProductAdded = (product) => {
  ElMessage.success('商品发布成功，已添加到商品库')
  // 刷新商品列表
  fetchProducts()
}

// 监听页面滚动，控制回顶部按钮显示和实现无限滚动
const handleScroll = () => {
  // 更新回顶部按钮显示状态
  showBackTop.value = window.scrollY > 300
  
  // 使用节流控制滚动事件处理频率
  if (scrollThrottleTimer.value) return
  
  scrollThrottleTimer.value = setTimeout(() => {
    // 无限滚动逻辑
    const scrollHeight = document.documentElement.scrollHeight
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
    const clientHeight = document.documentElement.clientHeight
    
    // 当滚动到距离底部100px时，加载更多数据
    if (scrollHeight - scrollTop - clientHeight < 100 && !loading.value && !allLoaded.value) {
      loadMore()
    }
    
    scrollThrottleTimer.value = null
  }, 200)
}

// 监听分类变化
watch(currentCategoryId, () => {
  // 回到顶部
  window.scrollTo({ top: 0 })
})

// 组件挂载时获取商品数据并添加滚动监听
onMounted(() => {
  fetchProducts()
  
  window.addEventListener('scroll', handleScroll)
})

// 组件卸载时清除滚动监听和定时器
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  
  if (scrollThrottleTimer.value) {
    clearTimeout(scrollThrottleTimer.value)
  }
})

// 获取商品图片
const getProductImage = (product) => {
  if (!product.itemImageUrl) {
    return new URL(`../assets/images/default-item.jpg`, import.meta.url).href
  }
  return getFileUrl(product.itemImageUrl, true, true)
}

// 购买成功回调
const handleBuySuccess = (orderData) => {
  ElMessage.success('商品购买成功，即将刷新商品列表')
  // 刷新商品列表
  fetchProducts()
}

// 处理产品详情页添加到购物车
const handleDetailAddToCart = async (data) => {
  try {
    loading.value = true
    const res = await addItemToCart(data.userId, data.itemId, data.quantity)
    
    if (res.code === 200) {
      ElMessage.success('商品已成功加入购物车')
      
      // 触发事件，通知其他组件更新购物车数量
      if (window.$emitter) {
        window.$emitter.emit('update-cart-count')
      }
    } else {
      ElMessage.error(res.message || '加入购物车失败')
    }
  } catch (error) {
    console.error('加入购物车失败:', error)
    ElMessage.error('加入购物车失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="main-container">
    <CommonHeader @search="handleSearch" />
    <CommonCategory @category-change="handleCategoryChange" />
    <div class="main-content">
      <!-- 商品展示区域 -->
      <div class="products-section">
        <h3 class="section-title">全部商品</h3>
        <div class="waterfall-container">
          <div v-for="product in products" :key="product.itemId" class="product-item">
            <el-skeleton style="width: 100%;" :loading="loading && products.length === 0" animated>
              <template #template>
                <el-skeleton-item variant="image" style="width: 100%; height: 240px" />
                <div style="padding: 14px">
                  <el-skeleton-item variant="h3" style="width: 50%" />
                  <div
                    style="
                      display: flex;
                      align-items: center;
                      margin-top: 16px;
                      height: 16px;
                    "
                  >
                    <el-skeleton-item variant="text" style="margin-right: 16px" />
                    <el-skeleton-item variant="text" style="width: 30%" />
                  </div>
                </div>
              </template>
              <template #default>
                <el-card 
                  :body-style="{ padding: '0px', marginBottom: '1px' }" 
                  class="product-card"
                  @click="openProductDetail(product)"
                >
                  <div class="card-content">
                    <div class="image-container">
                      <img
                        :src="getProductImage(product)"
                        :alt="product.title"
                        class="image"
                      />
                    </div>
                    <div class="product-info">
                      <span class="product-title">{{ product.title }}</span>
                      <div class="price-section">
                        <div class="product-price">¥{{ product.price }}</div>
                      </div>
                      <div class="button-group">
                        <el-button 
                          type="warning" 
                          size="small" 
                          circle 
                          class="cart-button"
                          @click="addToCart(product, $event)"
                        >
                          <CartIconIcon />
                        </el-button>
                        <el-button 
                          type="primary" 
                          size="small" 
                          class="buy-button"
                          @click="buyProduct(product, $event)"
                        >
                          购买
                        </el-button>
                      </div>
                    </div>
                  </div>
                </el-card>
              </template>
            </el-skeleton>
          </div>
        </div>
        
        <!-- 加载状态 -->
        <div v-if="loading && products.length > 0" class="loading-more">
          <el-skeleton :rows="1" animated />
        </div>
        
        <!-- 空状态 -->
        <el-empty v-if="products.length === 0 && !loading" description="暂无商品" />
      </div>
    </div>
    
    <!-- 右侧工具栏 -->
    <div class="side-toolbar">
      <div class="toolbar-item" @click="handlePublish">
        <el-icon><Plus /></el-icon>
        <span>我要卖</span>
      </div>
      <div class="toolbar-item" @click="viewCart">
        <el-icon><ShoppingCart /></el-icon>
        <span>购物车</span>
      </div>
      <div class="toolbar-item" @click="scrollToTop">
        <el-icon><ArrowUp /></el-icon>
        <span>回顶部</span>
      </div>
    </div>
    
    <!-- 商品详情对话框 -->
    <ProductDetail
      v-if="detailVisible && currentProduct"
      :product="currentProduct"
      :loading="detailLoading"
      :should-buy="shouldShowBuyDialog"
      @close="closeProductDetail"
      @add-to-cart="handleDetailAddToCart"
      @buy-success="handleBuySuccess"
    />

    <!-- 添加商品对话框 -->
    <AddProductDialog
      v-model:visible="addProductVisible"
      @success="handleProductAdded"
    />
  </div>
</template>

<style scoped>
.main-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f5f5f5;
  position: relative;
}

.main-content {
  flex: 1;
  padding: 20px;
  max-width: 1600px; /* 增加1/3宽度，从1200px扩展到1600px */
  margin: 0 auto;
  width: 100%;
}

.section-title {
  font-size: 22px;
  color: #333;
  margin-bottom: 20px;
  font-weight: bold;
}

.products-section {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-top: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  min-height: 400px;
}

/* 瀑布流布局 */
.waterfall-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.product-item {
  width: 100%;
  margin-bottom: 20px;
}

.product-card {
  width: 100%;
  transition: transform 0.3s, box-shadow 0.3s;
  border-radius: 8px;
  overflow: hidden;
  height: auto; /* 改为自适应高度 */
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.card-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.image-container {
  height: 240px;
  overflow: hidden;
  width: 100%; /* 确保图片容器宽度占满 */
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
}

.image {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 改为contain，保持图片比例并完整显示 */
  display: block;
}

.product-info {
  padding: 14px;
  display: flex;
  flex-direction: column;
  flex: 1;
  position: relative;
}

.product-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  display: block;
  line-height: 1.4;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.price-section {
  margin-top: 8px;
}

.product-price {
  font-size: 16px;
  color: #FF5000;
  font-weight: bold;
}

.button-group {
  display: flex;
  align-items: center;
  gap: 8px;
  position: absolute;
  bottom: 14px;
  right: 14px;
}

.buy-button {
  background-color: #FF5000;
  border-color: #FF5000;
}

.cart-button {
  background-color: #FFEEE5;
  border-color: #FF5000;
  color: #FF5000;
}

.loading-more {
  padding: 20px 0;
  text-align: center;
}

/* 右侧工具栏样式 - 新设计 */
.side-toolbar {
  position: fixed;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  background-color: transparent;
  padding: 15px 0;
  z-index: 99;
  gap: 15px;
}

.toolbar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fff;
  border-radius: 50%;
  width: 60px;
  height: 60px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.toolbar-item:hover {
  background-color: #FF5000;
}

.toolbar-item:hover,
.toolbar-item:hover span {
  color: #fff;
}

.toolbar-item{
  font-size: 20px;
  margin-bottom: 5px;
  color: #333;
  transition: color 0.3s;
}

.toolbar-item span {
  font-size: 12px;
  color: #333;
  line-height: 1;
  transition: color 0.3s;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .waterfall-container {
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  }
}

@media (max-width: 768px) {
  .waterfall-container {
    grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  }
  
  .toolbar-item {
    width: 50px;
    height: 50px;
    padding: 8px;
  }
}

@media (max-width: 480px) {
  .waterfall-container {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}
</style>