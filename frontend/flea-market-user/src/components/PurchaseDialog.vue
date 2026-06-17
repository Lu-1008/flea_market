<template>
  <div>
    <el-dialog
      v-model="dialogVisible"
      :title="title"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
      :append-to-body="true"
      :z-index="10001"
      @open="handleDialogOpen"
      @close="handleDialogClose"
    >
      <el-form
        ref="purchaseFormRef"
        :model="purchaseForm"
        :rules="rules"
        label-width="100px"
        @submit.prevent
      >
        <div class="item-info">
          <div class="item-image-container">
            <el-image
              v-if="itemData.itemImageUrl"
              :src="getItemImageUrl(itemData.itemImageUrl)"
              fit="cover"
              class="item-image"
              @error="handleImageError"
            >
              <template #error>
                <div class="item-fallback-image">
                  <el-icon><Picture /></el-icon>
                  <div>商品图片</div>
                </div>
              </template>
            </el-image>
            <div v-else class="item-no-image">
              <el-icon><Picture /></el-icon>
              <div>暂无图片</div>
            </div>
          </div>
          
          <div class="item-details">
            <h3 class="item-title">{{ itemData.title }}</h3>
            <div class="item-price">¥{{ itemData.price }}</div>
            <div class="item-seller">
              卖家: {{ itemData.username }}
            </div>
          </div>
        </div>
        
        <!-- 联系信息表单 -->
        <div class="contact-form">
          <h4 class="section-title">联系信息</h4>
          
          <el-form-item label="联系电话" prop="phone">
            <el-input 
              v-model="purchaseForm.phone" 
              placeholder="请输入您的联系电话"
              clearable
            />
          </el-form-item>
          
          <el-form-item label="收货地址" prop="address">
            <el-input 
              v-model="purchaseForm.address" 
              type="textarea"
              :rows="2"
              placeholder="请输入您的收货地址"
              clearable
            />
          </el-form-item>
        </div>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handlePurchase" :loading="loading">
            确认购买
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, toRefs, watch, onMounted } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { purchaseItem } from '@/api/order'
import { getFileUrl } from '@/api/config'
import { getUserInfo } from '@/api/auth'
import { useUserStore } from '@/store/user'

export default {
  name: 'PurchaseDialog',
  components: {
    Picture
  },
  props: {
    itemData: {
      type: Object,
      required: true
    },
    visible: {
      type: Boolean,
      default: false
    },
    buyerId: {
      type: Number,
      required: true
    }
  },
  emits: ['update:visible', 'purchase-success'],
  setup(props, { emit }) {
    const { visible, itemData, buyerId } = toRefs(props)
    const userStore = useUserStore()
    
    // 对话框可见状态
    const dialogVisible = computed({
      get: () => visible.value,
      set: (val) => emit('update:visible', val)
    })
    
    // 对话框标题
    const title = computed(() => `购买商品: ${itemData.value?.title || ''}`)
    
    // 表单引用
    const purchaseFormRef = ref(null)
    
    // 表单数据 - 使用reactive而不是ref
    const purchaseForm = reactive({
      phone: '',
      address: ''
    })
    
    // 表单校验规则
    const rules = reactive({
      phone: [
        { required: true, message: '请输入联系电话', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
      ],
      address: [
        { required: true, message: '请输入收货地址', trigger: 'blur' }
      ]
    })
    
    // 加载状态
    const loading = ref(false)
    
    // 自动填充用户联系信息的标志
    const autoFillInfo = reactive({
      loading: false,
      filled: false
    })
    
    // 获取商品图片URL
    const getItemImageUrl = (url) => {
      if (!url) return ''
      
      try {
        // 首先尝试使用直接URL (从静态资源)
        if (url.startsWith('default-') || url.includes('/assets/')) {
          try {
            return new URL(`../assets/images/${url}`, import.meta.url).href
          } catch (e) {
            console.error('加载本地图片失败:', e)
          }
        }
        
        // 否则使用已有的getFileUrl方法
        return getFileUrl(url, true)
      } catch (error) {
        console.error('获取图片URL出错:', error)
        return ''
      }
    }
    
    // 处理图片加载错误
    const handleImageError = () => {
      console.log('商品图片加载失败:', itemData.value?.itemImageUrl)
      // 加载失败时不需要额外处理，因为我们已经提供了#error模板
    }
    
    // 防止可能的响应性问题
    const resetAndInitForm = () => {
      // 清空表单
      purchaseForm.phone = '';
      purchaseForm.address = '';
      
      // 重置填充状态
      autoFillInfo.filled = false;
      autoFillInfo.loading = false;
      
      // 延迟一点再加载用户信息
      setTimeout(() => {
        loadUserContactInfo();
      }, 100);
    }
    
    // 监听对话框显示，重置表单并加载用户信息
    watch(() => dialogVisible.value, (newVal) => {
      if (newVal) {
        resetAndInitForm()
      }
    }, { immediate: true })
    
    // 加载用户联系信息
    const loadUserContactInfo = async () => {
      if (autoFillInfo.filled || autoFillInfo.loading || !buyerId.value) return
      
      autoFillInfo.loading = true
      
      try {
        // 尝试从userStore获取联系信息
        if (userStore.userInfo) {
          purchaseForm.phone = userStore.userInfo.phone || ''
          purchaseForm.address = userStore.userInfo.address || ''
          
          if (purchaseForm.phone && purchaseForm.address) {
            autoFillInfo.filled = true
            return
          }
        }
        
        // 如果userStore中没有联系信息，尝试从API获取
        const response = await getUserInfo(buyerId.value)
        
        if (response.code === 200 && response.data) {
          purchaseForm.phone = response.data.phone || ''
          purchaseForm.address = response.data.address || ''
          
          if (purchaseForm.phone || purchaseForm.address) {
            autoFillInfo.filled = true
            
            // 更新userStore中的联系信息
            if (userStore.userInfo) {
              userStore.updateUserInfo({
                phone: purchaseForm.phone,
                address: purchaseForm.address
              })
            }
          }
        }
      } catch (error) {
        console.error('获取用户联系信息失败:', error)
        // 获取失败不显示错误提示，静默处理
      } finally {
        autoFillInfo.loading = false
      }
    }
    
    // 组件可能已被卸载，需要判断
    const safeSetLoading = (value) => {
      if (loading && loading.value !== undefined) {
        loading.value = value
      }
    }

    const safeSetDialog = (value) => {
      try {
        emit('update:visible', value)
      } catch (e) {
        console.warn('设置对话框状态失败', e)
      }
    }

    // 处理购买对话框关闭，确保清理资源
    const handleDialogClose = () => {
      // 重置表单和状态
      resetAndInitForm()
      loading.value = false
    }

    // 处理购买
    const handlePurchase = async () => {
      // 表单验证
      if (!purchaseFormRef.value) {
        console.error('表单引用不存在')
        
        // 手动验证必填字段
        if (!purchaseForm.phone) {
          ElMessage.error('请输入联系电话')
          return
        }
        
        if (!purchaseForm.address) {
          ElMessage.error('请输入收货地址')
          return
        }
      } else {
        // 使用表单验证
        const valid = await purchaseFormRef.value.validate().catch(() => false)
        if (!valid) {
          return
        }
      }
      
      if (!itemData.value || !itemData.value.itemId) {
        ElMessage.error('商品信息不完整')
        return
      }
      
      if (!buyerId.value) {
        ElMessage.error('请先登录')
        return
      }
      
      loading.value = true
      
      // 构造联系信息
      const contactInfo = {
        phone: purchaseForm.phone,
        address: purchaseForm.address
      }
      
      console.log('提交购买请求，联系信息:', contactInfo)
      
      // 先保存需要的变量，避免组件卸载后无法访问
      const itemId = itemData.value.itemId
      const buyerIdVal = buyerId.value
      
      // 调用购买API
      try {
        // 保存响应对象
        let successData = null
        
        const response = await purchaseItem(buyerIdVal, itemId, contactInfo)
        
        // 安全设置loading状态
        safeSetLoading(false)
        
        if (response.code === 200) {
          successData = response.data
          
          ElNotification({
            title: '购买成功',
            message: '您已成功提交订单，可在"我的订单"中查看详情',
            type: 'success'
          })
          
          // 更新userStore中的联系信息
          userStore.updateUserInfo({
            phone: contactInfo.phone,
            address: contactInfo.address
          })
          
          // 安全关闭对话框
          safeSetDialog(false)
          
          // 安全地触发事件
          if (successData) {
            try {
              emit('purchase-success', successData)
            } catch (e) {
              console.warn('发送购买成功事件失败', e)
            }
          }
        } else {
          ElMessage.error(response.message || '购买失败')
        }
      } catch (error) {
        // 安全设置loading状态
        safeSetLoading(false)
        ElMessage.error(error.message || '购买过程中出现错误')
      }
    }
    
    // 在组件挂载时尝试加载用户信息
    onMounted(() => {
      if (buyerId.value) {
        loadUserContactInfo()
      }
    })
    
    // 处理对话框打开
    const handleDialogOpen = () => {
      console.log('对话框打开')
      // 确保表单重置并重新加载用户信息
      resetAndInitForm()
    }
    
    return {
      dialogVisible,
      title,
      purchaseForm,
      rules,
      purchaseFormRef,
      loading,
      getItemImageUrl,
      handleImageError,
      handlePurchase,
      handleDialogOpen,
      handleDialogClose
    }
  }
}
</script>

<style scoped>
.item-info {
  display: flex;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e6e6e6;
}

.item-image-container {
  width: 100px;
  height: 100px;
  margin-right: 15px;
  overflow: hidden;
  border-radius: 4px;
}

.item-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-no-image, .item-fallback-image {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #f5f7fa;
  color: #909399;
  font-size: 14px;
}

.item-no-image .el-icon, .item-fallback-image .el-icon {
  font-size: 24px;
  margin-bottom: 5px;
}

.item-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.item-title {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 500;
}

.item-price {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
  margin-bottom: 10px;
}

.item-seller {
  font-size: 14px;
  color: #606266;
}

.section-title {
  margin: 10px 0 15px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.contact-form {
  margin-top: 10px;
}
</style> 