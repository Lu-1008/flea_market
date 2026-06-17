<script setup>
import { ref, onMounted, watch, reactive, onBeforeUnmount } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/store/user';
import { useRouter } from 'vue-router';
import { getFileUrl } from '@/api/config';
import { getUserItems, updateItemStatus, deleteItem, updateProduct, getProductById, updateItemImage } from '@/api/product';
import { getCategoryList } from '@/api/category';
import { uploadProductImage } from '@/api/file';
import { Plus, PictureFilled } from '@element-plus/icons-vue';

const userStore = useUserStore();
const router = useRouter();
const products = ref([]);
const loading = ref(true);
const itemFilter = ref('');
const categories = ref([]);
const detailVisible = ref(false);
const editMode = ref(false);
const detailLoading = ref(false);

// 当前商品详情
const currentItem = reactive({
  itemId: null,
  title: '',
  description: '',
  price: 0,
  itemImageUrl: '',
  status: '',
  categoryId: null,
  createTime: '',
  validUntil: ''
});

// 编辑表单数据
const editForm = reactive({
  itemId: null,
  title: '',
  description: '',
  price: 0,
  categoryId: null,
  itemImageUrl: ''
});

// 表单规则
const rules = {
  title: [
    { required: true, message: '请输入商品标题', trigger: 'blur' },
    { min: 2, max: 50, message: '标题长度在2到50个字符之间', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入商品描述', trigger: 'blur' },
    { min: 5, max: 500, message: '描述长度在5到500个字符之间', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' },
    { type: 'number', message: '价格必须为数字', trigger: 'blur' },
    { validator: (rule, value, callback) => {
        if (value <= 0) {
          callback(new Error('价格必须大于0'));
        } else {
          callback();
        }
      }, 
      trigger: 'blur' 
    }
  ],
  categoryId: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ]
};

// 商品状态映射
const itemStatusMap = {
  'ACTIVE': '在售中',
  'SOLD': '已售出',
  'INACTIVE': '已下架',
  'PENDING': '待审核',
  'REJECTED': '已拒绝',
  'EXPIRED': '已过期',
  'DRAFT': '草稿',
  '1': '在售中',  // 兼容可能的数字状态
  '0': '已下架',
  '2': '已售出'
};

// 获取用户发布的商品列表
const fetchUserItems = async () => {
  if (!userStore.userId) {
    ElMessage.warning('请先登录');
    return;
  }
  
  loading.value = true;
  try {
    // 构建查询参数
    const params = { page: 1, size: 20 };
    if (itemFilter.value) {
      params.status = itemFilter.value;
    }
    
    // 调用接口获取用户发布的商品
    const res = await getUserItems(userStore.userId, params);
    
    if (res.code === 200) {
      console.log('API返回的商品数据:', res.data.list);
      
      products.value = res.data.list.map(item => {
        // 调试输出商品状态
        console.log(`商品 ${item.itemId} 的状态:`, item.status, typeof item.status);
        
        return {
          itemId: item.itemId,
          title: item.title || '未命名商品',
          description: item.description || '',
          price: item.price || 0,
          itemImageUrl: item.itemImageUrl,
          // 确保状态字段有值，如果为null或undefined，则设置为默认值'INACTIVE'
          status: item.status || 'INACTIVE',
          categoryId: item.categoryId,
          categoryName: item.categoryName || '未分类',
          createTime: formatDateTime(item.createdAt),
          validUntil: formatDateTime(item.validUntil)
        };
      });
    } else {
      ElMessage.error(res.message || '获取商品列表失败');
    }
  } catch (error) {
    console.error('获取商品列表失败:', error);
    ElMessage.error('获取商品列表失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 格式化日期时间
const formatDateTime = (timestamp) => {
  if (!timestamp) return '未知时间';
  
  try {
    if (typeof timestamp === 'string') {
      return timestamp;
    }
    
    const date = new Date(timestamp);
    return date.toLocaleDateString();
  } catch (error) {
    console.error('日期格式化错误:', error);
    return '日期错误';
  }
};

// 处理图片URL
const getImageUrl = (url) => {
  if (!url) {
    return new URL('../../assets/images/no-image.png', import.meta.url).href;
  }
  
  // 直接用完整路径访问商品图片
  if (url.startsWith('http')) return url;
  
  // 处理相对路径
  try {
    // 先尝试使用MinIO直接访问，添加noCache=true防止缓存
    return getFileUrl(url, true, true);
  } catch (error) {
    console.error('获取图片URL失败:', error);
    return getFileUrl(url, false, true);
  }
};

// 查看商品详情
const viewProductDetail = async (item) => {
  if (!item || !item.itemId) {
    ElMessage.error('商品信息不完整，无法查看详情');
    return;
  }
  
  editMode.value = false;
  detailLoading.value = true;
  
  try {
    console.log('查看商品详情，商品ID:', item.itemId, '当前状态:', item.status);
    
    // 加载最新的商品信息
    const res = await getProductById(item.itemId);
    
    if (res.code === 200 && res.data) {
      const detail = res.data;
      console.log('获取到的商品详情:', detail);
      
      // 更新当前商品详情，确保所有字段都有默认值
      Object.assign(currentItem, {
        itemId: detail.itemId || item.itemId,
        title: detail.title || '未命名商品',
        description: detail.description || '',
        price: detail.price || 0,
        itemImageUrl: detail.itemImageUrl || null,
        status: detail.status || 'INACTIVE',
        categoryId: detail.categoryId || null,
        categoryName: detail.categoryName || getCategoryNameById(detail.categoryId) || '未分类',
        createTime: formatDateTime(detail.createdAt) || '未知时间',
        validUntil: formatDateTime(detail.validUntil) || '未知时间'
      });
      
      console.log('更新后的currentItem:', currentItem);
      
      // 显示商品详情对话框
      detailVisible.value = true;
    } else {
      ElMessage.error(res.message || '获取商品详情失败');
    }
  } catch (error) {
    console.error('获取商品详情失败:', error);
    ElMessage.error('获取商品详情失败，请稍后重试');
    detailVisible.value = false; // 确保对话框关闭
  } finally {
    detailLoading.value = false;
  }
};

// 处理图片上传
const handleImageUpload = async (file) => {
  if (!file) return;
  
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

  // 确保有商品ID
  if (editForm.itemId) {
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('itemId', editForm.itemId);
      
      const response = await uploadProductImage(formData);
      
      if (response.code === 200) {
        // 保存返回的图片URL
        const imageUrl = response.data;
        editForm.itemImageUrl = imageUrl;
        
        // 同时更新当前商品详情中的图片URL
        currentItem.itemImageUrl = imageUrl;
        
        // 使用专用API更新商品图片
        await updateItemImage(editForm.itemId, imageUrl);
        
        // 更新列表中的商品图片
        const index = products.value.findIndex(p => p.itemId === editForm.itemId);
        if (index !== -1) {
          products.value[index].itemImageUrl = imageUrl;
        }
        
        ElMessage.success('商品图片上传成功');
        return true;
      } else {
        ElMessage.error(response.message || '图片上传失败');
        return false;
      }
    } catch (error) {
      console.error('上传图片失败:', error);
      ElMessage.error('图片上传失败，请稍后重试');
      return false;
    }
  } else {
    ElMessage.warning('请先保存商品基本信息，然后再上传图片');
    return false;
  }
};

// 处理图片上传出错
const handleImageError = (error) => {
  console.error('图片上传失败:', error);
  ElMessage.error('图片上传失败，请稍后重试');
};

// 处理图片加载出错
const handleImageLoadError = (event) => {
  event.target.src = new URL('../../assets/images/no-image.png', import.meta.url).href;
};

// 切换到编辑模式
const switchToEditMode = () => {
  // 添加图片URL到编辑表单
  Object.assign(editForm, {
    itemId: currentItem.itemId,
    title: currentItem.title,
    description: currentItem.description,
    price: currentItem.price,
    categoryId: currentItem.categoryId,
    itemImageUrl: currentItem.itemImageUrl
  });
  
  editMode.value = true;
};

// 根据分类ID获取分类名称
const getCategoryNameById = (id) => {
  const category = categories.value.find(c => c.categoryId === id);
  return category ? category.categoryName : '未知分类';
};

// 获取所有分类
const fetchCategories = async () => {
  try {
    const res = await getCategoryList();
    if (res.code === 200) {
      categories.value = res.data;
    }
  } catch (error) {
    console.error('获取分类列表失败:', error);
  }
};

// 保存编辑
const saveEdit = async (formEl) => {
  if (!formEl) return;
  
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        const updateData = {
          itemId: editForm.itemId,
          title: editForm.title,
          description: editForm.description,
          price: editForm.price,
          categoryId: editForm.categoryId,
          itemImageUrl: editForm.itemImageUrl
        };
        
        const res = await updateProduct(updateData);
        
        if (res.code === 200) {
          ElMessage.success('商品更新成功');
          
          // 更新当前商品详情
          Object.assign(currentItem, {
            ...currentItem,
            title: editForm.title,
            description: editForm.description,
            price: editForm.price,
            categoryId: editForm.categoryId,
            categoryName: getCategoryNameById(editForm.categoryId),
            itemImageUrl: editForm.itemImageUrl
          });
          
          // 更新列表中的商品
          const index = products.value.findIndex(p => p.itemId === editForm.itemId);
          if (index !== -1) {
            products.value[index] = {
              ...products.value[index],
              title: editForm.title,
              description: editForm.description,
              price: editForm.price,
              categoryId: editForm.categoryId,
              categoryName: getCategoryNameById(editForm.categoryId),
              itemImageUrl: editForm.itemImageUrl
            };
          }
          
          // 返回详情模式
          editMode.value = false;
          
          // 延迟关闭对话框，确保状态更新完成
          setTimeout(() => {
            detailVisible.value = false;
            // 重新获取商品列表，确保数据一致性
            fetchUserItems();
          }, 500);
        } else {
          ElMessage.error(res.message || '更新商品失败');
        }
      } catch (error) {
        console.error('更新商品失败:', error);
        ElMessage.error('更新商品失败，请稍后重试');
      }
    } else {
      ElMessage.warning('请正确填写表单信息');
    }
  });
};

// 取消编辑，确保重置状态
const cancelEdit = () => {
  editMode.value = false;
  // 不立即关闭对话框，而是保持在详情模式
};

// 下架商品
const delistProduct = (item) => {
  ElMessageBox.confirm(`确定要下架商品 "${item.title}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await updateItemStatus(item.itemId, 'INACTIVE');
      if (res.code === 200) {
        // 更新列表中的商品状态
        const index = products.value.findIndex(p => p.itemId === item.itemId);
        if (index !== -1) {
          products.value[index].status = 'INACTIVE';
        }
        
        // 更新当前详情中的状态（如果打开）
        if (currentItem.itemId === item.itemId) {
          currentItem.status = 'INACTIVE';
        }
        
        ElMessage.success('商品已成功下架');
      } else {
        ElMessage.error(res.message || '下架商品失败');
      }
    } catch (error) {
      console.error('下架商品失败:', error);
      ElMessage.error('下架商品失败，请稍后重试');
    }
  }).catch(() => {});
};

// 重新上架商品
const relistProduct = (item) => {
  ElMessageBox.confirm(`确定要重新上架商品 "${item.title}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    try {
      const res = await updateItemStatus(item.itemId, 'ACTIVE');
      if (res.code === 200) {
        // 更新列表中的商品状态
        const index = products.value.findIndex(p => p.itemId === item.itemId);
        if (index !== -1) {
          products.value[index].status = 'ACTIVE';
        }
        
        // 更新当前详情中的状态（如果打开）
        if (currentItem.itemId === item.itemId) {
          currentItem.status = 'ACTIVE';
        }
        
        ElMessage.success('商品已成功上架');
      } else {
        ElMessage.error(res.message || '上架商品失败');
      }
    } catch (error) {
      console.error('上架商品失败:', error);
      ElMessage.error('上架商品失败，请稍后重试');
    }
  }).catch(() => {});
};

// 删除商品
const deleteProduct = (item, index) => {
  ElMessageBox.confirm(`确定要删除商品 "${item.title}" 吗？此操作不可恢复！`, '警告', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      const res = await deleteItem(item.itemId);
      if (res.code === 200) {
        products.value.splice(index, 1);
        
        // 如果当前有打开的详情框且正是这个商品，则关闭它
        if (detailVisible.value && currentItem.itemId === item.itemId) {
          detailVisible.value = false;
        }
        
        ElMessage.success('商品已成功删除');
      } else {
        ElMessage.error(res.message || '删除商品失败');
      }
    } catch (error) {
      console.error('删除商品失败:', error);
      ElMessage.error('删除商品失败，请稍后重试');
    }
  }).catch(() => {});
};

// 监听筛选变化
watch(itemFilter, () => {
  fetchUserItems();
});

// 在组件卸载前清理资源
onBeforeUnmount(() => {
  // 确保对话框已关闭
  detailVisible.value = false;
  // 重置状态
  editMode.value = false;
});

onMounted(() => {
  fetchCategories();
  fetchUserItems();
});

// 获取商品状态的CSS类名
const getStatusClass = (status) => {
  if (!status) return 'status-inactive';
  
  try {
    return `status-${status.toLowerCase()}`;
  } catch (e) {
    console.error('获取状态类名错误:', e);
    return 'status-inactive';
  }
};

// 获取商品状态的显示文本
const getStatusText = (status) => {
  // 如果状态为空，优先显示"已下架"
  if (!status) return '已下架';
  
  // 尝试从映射表中获取状态文本
  const statusText = itemStatusMap[status];
  if (statusText) return statusText;
  
  // 如果映射表中没有对应的状态，则直接返回状态值
  console.log('未知的商品状态:', status);
  return status;
};
</script>

<template>
  <div class="user-items">
    <div class="section-header">
      <h2>我的发布</h2>
      <div class="item-filters">
        <el-select v-model="itemFilter" placeholder="全部商品" size="small">
          <el-option label="全部商品" value=""></el-option>
          <el-option label="在售中" value="ACTIVE"></el-option>
          <el-option label="已售出" value="SOLD"></el-option>
          <el-option label="已下架" value="INACTIVE"></el-option>
        </el-select>
      </div>
    </div>
    
    <el-skeleton :loading="loading" animated :rows="3" :throttle="500">
      <template #template>
        <div v-for="i in 3" :key="i" style="padding: 15px 0; display: flex;">
          <el-skeleton-item variant="image" style="width: 80px; height: 80px; margin-right: 15px;" />
          <div style="flex: 1;">
            <el-skeleton-item variant="p" style="width: 50%" />
            <el-skeleton-item variant="text" style="width: 30%; margin-top: 10px" />
            <div style="display: flex; justify-content: space-between; margin-top: 10px">
              <el-skeleton-item variant="text" style="width: 15%" />
              <el-skeleton-item variant="text" style="width: 20%" />
            </div>
          </div>
        </div>
      </template>
      
      <template #default>
        <div v-if="products.length > 0" class="item-list">
          <div v-for="(item, index) in products" :key="item.itemId" class="item-card">
            <div class="item-image" @click="viewProductDetail(item)">
              <img :src="getImageUrl(item.itemImageUrl)" :alt="item.title" @error="item.itemImageUrl = null">
            </div>
            <div class="item-details">
              <div class="item-title" @click="viewProductDetail(item)">{{ item.title }}</div>
              <div class="item-price">¥ {{ item.price.toFixed(2) }}</div>
              <div class="item-status-row">
                <span class="item-date">发布于: {{ item.createTime ? item.createTime.split(' ')[0] : '' }}</span>
                <span class="item-status" :class="getStatusClass(item.status)">
                  {{ getStatusText(item.status) }}
                </span>
              </div>
              
              <div class="item-actions">
                <el-button size="small" @click="viewProductDetail(item)">查看详情</el-button>
                <el-button size="small" type="primary" v-if="item.status === 'INACTIVE'" @click="relistProduct(item)">重新上架</el-button>
                <el-button size="small" type="info" v-if="item.status === 'ACTIVE'" @click="delistProduct(item)">下架</el-button>
                <el-button size="small" type="danger" @click="deleteProduct(item, index)">删除</el-button>
              </div>
            </div>
          </div>
        </div>
        
        <el-empty v-else description="暂无发布的商品"></el-empty>
      </template>
    </el-skeleton>
    
    <!-- 商品详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="editMode ? '编辑商品' : '商品详情'"
      width="650px"
      destroy-on-close
      center
    >
      <el-skeleton :loading="detailLoading" animated>
        <template #template>
          <div style="padding: 20px">
            <el-skeleton-item variant="image" style="width: 100%; height: 240px; margin-bottom: 20px;" />
            <el-skeleton-item variant="p" style="width: 50%" />
            <div style="margin-top: 16px">
              <el-skeleton-item variant="text" style="margin-right: 16px" />
              <el-skeleton-item variant="text" />
            </div>
          </div>
        </template>
        
        <template #default>
          <!-- 商品详情展示模式 -->
          <div v-if="!editMode" class="item-detail">
            <div class="detail-image">
              <img :src="getImageUrl(currentItem.itemImageUrl)" :alt="currentItem.title" @error="currentItem.itemImageUrl = null">
            </div>
            <div class="detail-header">
              <h3>{{ currentItem.title }}</h3>
              <div class="detail-price">¥ {{ currentItem.price ? currentItem.price.toFixed(2) : '0.00' }}</div>
            </div>
            
            <el-divider>基本信息</el-divider>
            
            <div class="detail-info">
              <div class="detail-info-item">
                <span class="detail-label">商品分类:</span>
                <span>{{ currentItem.categoryName || '未分类' }}</span>
              </div>
              <div class="detail-info-item">
                <span class="detail-label">发布时间:</span>
                <span>{{ currentItem.createTime }}</span>
              </div>
              <div class="detail-info-item">
                <span class="detail-label">有效期至:</span>
                <span>{{ currentItem.validUntil }}</span>
              </div>
              <div class="detail-info-item">
                <span class="detail-label">商品状态:</span>
                <span :class="getStatusClass(currentItem.status)">
                  {{ getStatusText(currentItem.status) }}
                </span>
              </div>
            </div>
            
            <el-divider>商品描述</el-divider>
            
            <div class="detail-description">
              {{ currentItem.description }}
            </div>
            
            <div class="detail-actions">
              <el-button type="primary" @click="switchToEditMode" v-if="currentItem.status !== 'SOLD'">编辑商品</el-button>
              <el-button type="info" v-if="currentItem.status === 'ACTIVE'" @click="delistProduct(currentItem)">下架商品</el-button>
              <el-button type="success" v-if="currentItem.status === 'INACTIVE'" @click="relistProduct(currentItem)">重新上架</el-button>
            </div>
          </div>
          
          <!-- 商品编辑模式 -->
          <div v-else class="item-edit">
            <el-form ref="editFormRef" :model="editForm" :rules="rules" label-position="top">
              <!-- 增加商品图片上传功能 -->
              <el-form-item label="商品图片">
                <div class="item-image-uploader">
                  <el-upload
                    class="image-uploader"
                    :auto-upload="false"
                    :show-file-list="false"
                    :on-change="(file) => handleImageUpload(file.raw)"
                    :on-error="handleImageError"
                  >
                    <el-image
                      v-if="editForm.itemImageUrl"
                      :src="getImageUrl(editForm.itemImageUrl)"
                      class="upload-image"
                      fit="cover"
                      @error="handleImageLoadError"
                    />
                    <div v-else class="upload-placeholder">
                      <el-icon><Plus /></el-icon>
                      <div class="upload-text">点击上传图片</div>
                    </div>
                  </el-upload>
                  <div class="upload-tip">建议上传正方形图片，大小不超过2MB</div>
                </div>
              </el-form-item>
            
              <el-form-item label="商品标题" prop="title">
                <el-input v-model="editForm.title" placeholder="请输入商品标题" />
              </el-form-item>
              
              <el-form-item label="商品价格" prop="price">
                <el-input-number 
                  v-model="editForm.price" 
                  :min="0.01" 
                  :precision="2" 
                  :step="0.1" 
                  style="width: 100%"
                  placeholder="请输入价格" />
              </el-form-item>
              
              <el-form-item label="商品分类" prop="categoryId">
                <el-select v-model="editForm.categoryId" placeholder="请选择分类" style="width: 100%">
                  <el-option 
                    v-for="category in categories" 
                    :key="category.categoryId" 
                    :label="category.categoryName" 
                    :value="category.categoryId" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="商品描述" prop="description">
                <el-input 
                  v-model="editForm.description" 
                  type="textarea" 
                  :rows="6" 
                  placeholder="请输入商品描述" />
              </el-form-item>
            </el-form>
          </div>
        </template>
      </el-skeleton>
      
      <template #footer>
        <span class="dialog-footer">
          <template v-if="editMode">
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" @click="saveEdit($refs.editFormRef)">保存</el-button>
          </template>
          <template v-else>
            <el-button @click="detailVisible = false">关闭</el-button>
          </template>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-items {
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

.item-filters {
  min-width: 120px;
}

.item-list {
  width: 100%;
}

.item-card {
  display: flex;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
  width: 100%;
}

.item-card:last-child {
  border-bottom: none;
}

.item-image {
  width: 100px;
  height: 100px;
  border-radius: 4px;
  overflow: hidden;
  margin-right: 15px;
  cursor: pointer;
  transition: transform 0.2s;
  border: 1px solid #eee;
  background-color: #f9f9f9;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-image:hover {
  transform: scale(1.03);
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-details {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.item-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 8px;
  cursor: pointer;
  color: #333;
  transition: color 0.2s;
  line-height: 1.4;
}

.item-title:hover {
  color: #FF5000;
}

.item-price {
  font-size: 16px;
  font-weight: bold;
  color: #FF5000;
  margin-bottom: 8px;
}

.item-status-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 13px;
  color: #909399;
}

.item-status {
  font-weight: 600;
}

.status-active {
  color: #67C23A;
}

.status-sold {
  color: #409EFF;
}

.status-inactive {
  color: #909399;
}

.status-expired {
  color: #F56C6C;
}

.item-actions {
  margin-top: auto;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 商品详情样式 */
.item-detail {
  padding: 0 10px;
}

/* 图片上传相关样式 */
.item-image-uploader {
  width: 100%;
  text-align: center;
  margin-bottom: 20px;
}

.image-uploader {
  display: inline-block;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  width: 178px;
  height: 178px;
  position: relative;
}

.image-uploader:hover {
  border-color: #409EFF;
}

.upload-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background-color: #f5f7fa;
}

.upload-placeholder{
  font-size: 28px;
  color: #8c939d;
  margin-bottom: 8px;
}

.upload-text {
  color: #8c939d;
  font-size: 12px;
  text-align: center;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.detail-image {
  width: 100%;
  height: 300px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid #eee;
  background-color: #f9f9f9;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background-color: #f9f9f9;
}

.detail-header {
  margin-bottom: 15px;
}

.detail-header h3 {
  margin: 0 0 10px 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.detail-price {
  font-size: 24px;
  font-weight: bold;
  color: #FF5000;
}

.detail-info {
  margin-bottom: 15px;
}

.detail-info-item {
  display: flex;
  margin-bottom: 10px;
  font-size: 14px;
}

.detail-label {
  color: #909399;
  width: 80px;
  flex-shrink: 0;
}

.detail-description {
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 4px;
  margin-bottom: 20px;
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
  white-space: pre-wrap;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  gap: 10px;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .item-filters {
    margin-top: 8px;
    width: 100%;
  }
  
  .item-card {
    flex-direction: column;
    padding: 15px 0;
  }
  
  .item-image {
    width: 100%;
    height: 180px;
    margin-right: 0;
    margin-bottom: 10px;
  }
  
  .item-actions {
    flex-wrap: wrap;
    gap: 6px;
  }
  
  .item-actions button {
    margin-bottom: 6px;
  }
  
  .detail-info-item {
    flex-direction: column;
  }
  
  .detail-label {
    width: 100%;
    margin-bottom: 2px;
  }
}
</style> 