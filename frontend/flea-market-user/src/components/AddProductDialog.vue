<template>
  <el-dialog
    v-model="dialogVisible"
    title="发布新商品"
    width="700px"
    :close-on-click-modal="false"
    @closed="handleDialogClosed"
  >
    <add-product-form 
      @success="handleSuccess"
      @cancel="closeDialog"
    />
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import AddProductForm from './AddProductForm.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible', 'success'])

const dialogVisible = ref(props.visible)

// 监听visible属性变化
watch(() => props.visible, (val) => {
  dialogVisible.value = val
})

// 监听dialogVisible变化，同步回父组件
watch(dialogVisible, (val) => {
  emit('update:visible', val)
})

// 处理表单提交成功
const handleSuccess = (data) => {
  emit('success', data)
  closeDialog()
}

// 关闭对话框
const closeDialog = () => {
  dialogVisible.value = false
}

// 对话框完全关闭后的处理
const handleDialogClosed = () => {
  // 可以在这里进行一些清理工作
}
</script>

<style scoped>
:deep(.el-dialog__body) {
  padding: 0;
}
</style> 