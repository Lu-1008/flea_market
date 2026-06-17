<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllCategories } from '@/api/category'

// 分类数据
const allCategories = ref([])
const loading = ref(false)

// 当前选中的分类ID
const activeCategory = ref(null)

// 计算主要分类（前7个）
const categories = computed(() => {
  return allCategories.value.slice(0, 7)
})

// 计算其他分类（第7个之后的）
const otherCategories = computed(() => {
  return allCategories.value.slice(7)
})

// 获取所有分类数据
const fetchCategories = async () => {
  loading.value = true
  try {
    const res = await getAllCategories()
    if (res.code === 200 && res.data) {
      allCategories.value = res.data
      // 默认选中"全部"
      handleShowAll()
    } else {
      throw new Error(res.message || '获取分类失败')
    }
  } catch (error) {
    console.error('获取分类失败:', error)
    ElMessage.error('获取分类数据失败，请刷新重试')
    // 使用静态数据作为备用
    allCategories.value = [
      { categoryId: 1, name: '电子产品' },
      { categoryId: 2, name: '图书教材' },
      { categoryId: 3, name: '生活用品' },
      { categoryId: 4, name: '服饰鞋包' },
      { categoryId: 5, name: '运动健身' },
      { categoryId: 6, name: '美妆护肤' },
      { categoryId: 7, name: '闲置数码' },
      { categoryId: 8, name: '票券小物' }
    ]
    // 默认选中"全部"
    handleShowAll()
  } finally {
    loading.value = false
  }
}

// 处理分类点击
const handleCategoryClick = (categoryId) => {
  activeCategory.value = categoryId
  console.log('点击分类:', categoryId)
  // 触发自定义事件，通知父组件分类变化
  emit('category-change', categoryId)
}

// 显示全部商品
const handleShowAll = () => {
  activeCategory.value = null
  console.log('显示全部商品')
  // 触发自定义事件，通知父组件显示全部商品
  emit('category-change', null)
}

// 定义组件事件
const emit = defineEmits(['category-change'])

// 组件挂载时获取分类数据
onMounted(() => {
  fetchCategories()
})
</script>

<template>
  <div class="common-category">
    <div class="category-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="1" animated />
      </div>
      
      <template v-else>
        <!-- 全部按钮 -->
        <el-button 
          class="category-button"
          :type="activeCategory === null ? 'primary' : 'default'"
          @click="handleShowAll"
        >
          全部
        </el-button>
        
        <!-- 常规分类项 -->
        <el-button 
          v-for="category in categories" 
          :key="category.categoryId" 
          class="category-button"
          :type="activeCategory === category.categoryId ? 'primary' : 'default'"
          @click="handleCategoryClick(category.categoryId)"
        >
          {{ category.name }}
        </el-button>
        
        <!-- 其他分类按钮组 -->
        <el-dropdown v-if="otherCategories.length > 0" trigger="click">
          <el-button class="category-button">
            更多分类
            <el-icon class="el-icon--right">
              <i class="el-icon-arrow-down"></i>
            </el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item 
                v-for="item in otherCategories" 
                :key="item.categoryId"
                @click="handleCategoryClick(item.categoryId)"
              >
                {{ item.name }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </div>
  </div>
</template>

<style scoped>
.common-category {
  background-color: #fff;
  padding: 10px 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
}

.category-container {
  display: flex;
  align-items: center;
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 20px;
  overflow-x: auto;
  width: 100%;
  gap: 10px;
}

.category-container::-webkit-scrollbar {
  display: none;
}

.loading-container {
  width: 100%;
  padding: 0 20px;
}

.category-button {
  flex-shrink: 0;
  border-radius: 20px;
  font-size: 14px;
  padding: 8px 16px;
  height: auto;
}

.category-button.el-button--primary {
  background-color: #FF5000;
  border-color: #FF5000;
}

.category-button.el-button--primary:hover,
.category-button.el-button--primary:focus {
  background-color: #ff6a1f;
  border-color: #ff6a1f;
}

.category-button.el-button--default:hover {
  color: #FF5000;
  border-color: #FF5000;
}

@media (max-width: 768px) {
  .category-container {
    justify-content: flex-start;
    padding: 0 10px;
  }
  
  .category-button {
    padding: 6px 12px;
    font-size: 13px;
  }
}
</style> 