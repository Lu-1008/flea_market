<script setup>
import { ref, onMounted } from 'vue'
import { getStatisticsData } from '@/api/dashboard'
import { ElMessage } from 'element-plus'
import { getFileUrl } from '@/api/file'

// 获取用户头像
const getImageUrl = (imageName) => {
  // 如果是默认头像，从assets获取
  if (imageName === 'user-default.png') {
    return new URL(`../assets/images/${imageName}`, import.meta.url).href
  }
  // 否则从MinIO获取
  return getFileUrl(imageName, true)
}

// 定义数据
const userData = ref({
  username: '',
  role: '',
  lastLoginTime: '',
  userAvatar: 'user-default.png' // 默认头像
})
const statistics = ref({
  userCount: 0,
  itemCount: 0,
  salesAmount: 0
})

// 获取用户信息
const getUserInfo = () => {
  // 优先使用sessionStorage，如果没有再检查localStorage
  const userStr = sessionStorage.getItem('user') || localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      userData.value.username = user.username || ''
      userData.value.role = user.role === 'ADMIN' ? '超级管理员' : 
                           user.role === 'CATEGORY_MANAGER' ? '分类管理员' : '普通用户'
      
      // 使用用户的真实头像
      userData.value.userAvatar = user.userImgUrl || 'user-default.png'
      console.log('获取到用户头像:', userData.value.userAvatar)
    } catch (error) {
      console.error('解析用户信息失败:', error)
    }
  }
  
  // 设置模拟的上次登录时间
  const now = new Date()
  userData.value.lastLoginTime = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

// 获取统计数据
const getStatistics = async () => {
  try {
    // 显示加载状态
    statistics.value = {
      userCount: '加载中...',
      itemCount: '加载中...',
      salesAmount: '加载中...'
    }
    
    // 调用API获取统计数据
    const response = await getStatisticsData()
    if (response.data.code === 200) {
      const data = response.data.data
      statistics.value = {
        userCount: data.userCount.toString(),
        itemCount: data.itemCount.toString(),
        salesAmount: data.salesAmount.toFixed(2) // 保留两位小数
      }
    } else {
      // 如果API调用失败，显示错误消息
      ElMessage.error(response.data.message || '获取统计数据失败')
      statistics.value = {
        userCount: '0',
        itemCount: '0',
        salesAmount: '0'
      }
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    // 发生错误时，显示默认值
    statistics.value = {
      userCount: '0',
      itemCount: '0',
      salesAmount: '0'
    }
  }
}

// 组件挂载时获取数据
onMounted(() => {
  getUserInfo()
  getStatistics()
  
  // 临时测试：打印用户信息
  console.log('从sessionStorage获取的用户信息:', sessionStorage.getItem('user'))
  console.log('从localStorage获取的用户信息:', localStorage.getItem('user'))
  try {
    const sessionUser = sessionStorage.getItem('user') ? JSON.parse(sessionStorage.getItem('user')) : null
    const localUser = localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')) : null
    console.log('解析后的sessionStorage用户信息:', sessionUser)
    console.log('解析后的localStorage用户信息:', localUser)
  } catch (e) {
    console.error('解析用户信息失败:', e)
  }
})
</script>

<template>
  <el-row class="home" :gutter="20">
    <el-col :span="8" style="margin-top: 20px">
      <el-card>
        <div class="user">
          <img :src="getImageUrl(userData.userAvatar)" class="user"/>
          <div class="user-info">
            <p class="user-info-admin">{{ userData.username }}</p>
            <p class="user-info-p">{{ userData.role }}</p>
          </div>
        </div>
        <div class="divider"></div>
        <div class="login-info">
          <p>上次登录时间:<span>{{ userData.lastLoginTime }}</span></p>
        </div>
      </el-card>
    </el-col>

    <el-col :span="5" style="margin-top: 20px">
      <el-card>
        <div class="users-info">
          <svg class="all-users-icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="10em" height="10em">
            <path d="M429.79 875.5c-171.87-0.74-259.77-18-294-57.7-22.78-26.43-18.43-56.51-14.94-80.68l0.66-4.59c3.79-27.12 21.51-49.67 52.68-67.02 23.99-13.35 52.45-22.24 79.97-30.83 24.73-7.72 48.09-15.02 60.71-23.58 17.79-12.07 11.92-41.95 9.02-52.93-27.05-25.65-47.54-60.29-59.43-100.61-0.12-0.39-0.22-0.79-0.31-1.18-4.04-17.06-13.09-61.42-10.01-104.72 3.78-56.28 19.89-103.92 46.58-137.68 30.71-38.85 75.22-59.38 128.73-59.38h0.7c53.51 0 98.02 20.53 128.73 59.38 26.69 33.76 42.8 81.4 46.59 137.76 0.02 0.24 0.03 0.49 0.04 0.73 1.14 33.02 2.44 70.42-10.59 105.84-11.92 40-32.32 74.36-59.21 99.86-2.9 10.98-8.77 40.86 9.01 52.93 12.62 8.56 35.98 15.85 60.71 23.57 27.52 8.59 55.98 17.48 79.97 30.83 31.17 17.35 48.9 39.9 52.69 67.03 0.21 1.51 0.43 3.04 0.66 4.57 3.5 24.17 7.85 54.25-14.93 80.68-34.23 39.71-122.12 56.97-293.9 57.7h-0.1z m-122-430.08c10.08 33.81 28.06 63.41 50.69 83.42 2.53 2.24 4.53 5.02 5.83 8.13 1.94 4.61 7.54 22.36 7.86 42.75 0.47 30.36-10.61 54.08-32.04 68.62-18.12 12.29-44.56 20.55-72.56 29.29-45.56 14.23-97.2 30.35-101.5 61.13-0.23 1.61-0.46 3.2-0.69 4.79-3.14 21.68-4.47 34.46 4.48 44.85 10.01 11.62 32.4 21.03 66.54 27.98 44.82 9.12 108.07 13.74 193.38 14.11 85.31-0.37 148.56-4.98 193.38-14.1 34.13-6.95 56.52-16.36 66.53-27.98 8.96-10.39 7.62-23.17 4.48-44.85-0.23-1.59-0.46-3.19-0.68-4.78-4.3-30.8-55.94-46.92-101.5-61.15-27.99-8.74-54.44-17-72.56-29.29-21.43-14.54-32.5-38.26-32.03-68.62 0.32-20.39 5.92-38.13 7.86-42.74 1.31-3.11 3.3-5.89 5.83-8.13 22.75-20.13 40.82-49.96 50.86-84.01 0.15-0.51 0.32-1.02 0.51-1.52 10.16-27.16 9.12-58.54 8.07-88.92-9.19-134.62-85.57-154.78-130.34-154.8h-0.81c-44.82 0.02-121.28 20.22-130.35 155.16-2.61 36.71 5.14 75.19 8.77 90.65z" fill="#333333"></path>
            <path d="M833.81 757.09c-9.22 0-17.87-5.71-21.17-14.89-4.21-11.69 1.86-24.58 13.55-28.79 17.15-6.17 27.45-13.54 30.63-21.92 3.02-7.94 1.36-19.37-0.39-31.48-0.2-1.41-0.41-2.83-0.61-4.24-3.59-25.68-48.52-39.71-88.16-52.09-24.99-7.8-48.59-15.17-64.96-26.28-19.47-13.21-30.9-34.39-32.18-59.64-0.89-17.59 3.65-39.9 15.79-50.4 19.75-17.52 35.43-43.48 44.17-73.13 0.22-0.75 0.48-1.49 0.78-2.22 5.49-13.31 7.9-50.46 6.72-76.94-2.88-42.26-17.59-115.27-83.64-131.5-12.07-2.97-19.45-15.15-16.48-27.22 2.96-12.07 15.15-19.45 27.22-16.48 36.12 8.88 65.96 30.88 86.29 63.64 18.01 29.01 28.61 65.61 31.52 108.78l0.03 0.49c1.01 22.08 0.26 69.91-9.59 95.2-11.11 37.07-30.38 68.75-55.81 91.81-2.46 6.04-6.13 29.12 10.43 40.35 10.87 7.37 32.34 14.08 53.11 20.56 49.8 15.55 111.78 34.9 119.31 88.8 0.19 1.34 0.38 2.69 0.58 4.04 2.38 16.44 5.07 35.07-2.08 53.89-8.17 21.52-26.97 37.32-57.47 48.29a22.37 22.37 0 0 1-7.62 1.33z" fill="#333333"></path>
          </svg>
          <div class="users-msg">
            <p class="users-info-msg">用户总数:</p>
            <p class="users-info-p">{{ statistics.userCount }}</p>
          </div>
        </div>
      </el-card>
    </el-col>
    <el-col :span="5" style="margin-top: 20px">
      <el-card>
        <div class="goods-info">
          <svg class="all-goods-icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="10em" height="10em">
            <path d="M745.13 808.36H239.76c-34.02 0-65.61-13.62-88.97-38.35s-35.14-57.06-33.2-91.02l23.8-414.92c3.71-64.68 57.37-115.35 122.16-115.35h457.77c64.79 0 118.45 50.67 122.16 115.35l23.8 414.92c1.95 33.96-9.84 66.28-33.19 91.02-23.35 24.73-54.95 38.35-88.97 38.35zM263.56 193.72c-40.96 0-74.89 32.04-77.24 72.93l-23.8 414.92c-1.23 21.47 6.22 41.91 20.99 57.54s34.74 24.25 56.25 24.25h505.37c21.51 0 41.48-8.61 56.25-24.25 14.77-15.64 22.22-36.07 20.99-57.54l-23.8-414.92c-2.35-40.9-36.27-72.93-77.24-72.93H263.56z" fill="#333333"></path>
            <path d="M489.82 541.76c-85.84 0-155.68-69.84-155.68-155.68v-67.37c0-12.43 10.07-22.5 22.5-22.5s22.5 10.07 22.5 22.5v67.37c0 61.03 49.65 110.68 110.68 110.68s115.93-52.01 115.93-115.93v-56.87c0-12.43 10.07-22.5 22.5-22.5s22.5 10.07 22.5 22.5v56.87c0 88.74-72.19 160.93-160.93 160.93z" fill="#333333"></path>
          </svg>
          <div class="goods-msg">
            <p class="goods-info-msg">商品总数:</p>
            <p class="goods-info-p">{{ statistics.itemCount }}</p>
          </div>
        </div>

      </el-card>
    </el-col>
    <el-col :span="5" style="margin-top: 20px">
      <el-card>
        <div class="sales-info">
          <svg class="all-sales-icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" width="9em" height="9em">
            <path d="M824.5 433.5h-37.9c-12.43 0-22.5-10.07-22.5-22.5s10.07-22.5 22.5-22.5h37.9c24.82 0 45-20.19 45-45V227.51c0-24.82-20.19-45-45-45H203.83c-24.82 0-45 20.19-45 45V343.5c0 24.82 20.19 45 45 45h40.14c12.43 0 22.5 10.07 22.5 22.5s-10.07 22.5-22.5 22.5h-40.14c-49.63 0-90-40.38-90-90V227.51c0-49.63 40.38-90 90-90H824.5c49.63 0 90 40.38 90 90V343.5c0 49.63-40.38 90-90 90z" fill="#333333"></path>
            <path d="M514.34 891.42c-12.73 0-25.45-2.61-37.29-7.83l-91.03-40.14c-0.44-0.19-0.88-0.4-1.3-0.62a9.953 9.953 0 0 0-9.15-0.01l-73.39 37.92c-17 8.78-36.9 8.09-53.25-1.86-16.34-9.95-26.1-27.32-26.1-46.45V318.97c0-35.71 29.06-64.77 64.77-64.77h453.14c35.71 0 64.77 29.05 64.77 64.77v513.14c0 19.19-9.8 36.58-26.22 46.52s-36.37 10.56-53.38 1.66l-71.58-37.46a9.924 9.924 0 0 0-9.15-0.03c-0.42 0.22-0.86 0.42-1.29 0.61l-92.64 40.34a92.456 92.456 0 0 1-36.91 7.67z m-109.59-88.9l90.46 39.89a47.564 47.564 0 0 0 38.07 0.09l92.07-40.1c15.71-7.78 34.29-7.58 49.85 0.56l71.58 37.46c4.08 2.13 7.52 0.74 9.21-0.29s4.52-3.43 4.52-8.03V318.97c0-10.9-8.87-19.77-19.77-19.77H287.59c-10.9 0-19.77 8.87-19.77 19.77v513.45c0 4.59 2.82 6.99 4.5 8.01 1.68 1.02 5.11 2.43 9.19 0.32l73.39-37.92a54.997 54.997 0 0 1 49.84-0.3z" fill="#333333"></path>
            <path d="M622.29 556.63H406.04c-12.43 0-22.5-10.07-22.5-22.5s10.07-22.5 22.5-22.5h216.25c12.43 0 22.5 10.07 22.5 22.5s-10.07 22.5-22.5 22.5zM622.29 657.21H406.04c-12.43 0-22.5-10.07-22.5-22.5s10.07-22.5 22.5-22.5h216.25c12.43 0 22.5 10.07 22.5 22.5s-10.07 22.5-22.5 22.5z" fill="#333333"></path>
            <path d="M514.16 734.49c-12.43 0-22.5-10.07-22.5-22.5V561.25c0-12.43 10.07-22.5 22.5-22.5s22.5 10.07 22.5 22.5v150.74c0 12.43-10.07 22.5-22.5 22.5zM514.16 523.42c-4.27 0-8.54-1.21-12.27-3.64l-85.92-55.92c-10.42-6.78-13.36-20.72-6.58-31.13 6.78-10.42 20.72-13.36 31.13-6.58l73.65 47.93 73.65-47.93c10.41-6.78 24.35-3.83 31.13 6.58 6.78 10.42 3.83 24.35-6.58 31.13l-85.92 55.92c-3.73 2.43-8 3.64-12.27 3.64z" fill="#333333"></path></svg>
          <div class="sales-msg">
            <p class="sales-info-msg">总销售额:</p>
            <p class="sales-info-p">{{ statistics.salesAmount }}</p>
          </div>
        </div>
      </el-card>
    </el-col>

  </el-row>
</template>

<style scoped lang="less">
  .home{
    height: 100%;
    overflow: hidden;
    .user{
      display: flex;
      align-items: center;
      margin-bottom: 20px;
      img{
        width: 150px;
        height: 150px;
        border-radius: 50%;
        margin-right: 40px;
        object-fit: cover; /* 确保图片正确裁剪和显示 */
      }
      .user-info{
        p{
          line-height: 40px;
        }
        .user-info-p{
          color: #999;
        }
        .user-info-admin{
          font-size: 35px;
        }
      }
    }
    .login-info{
      p{
        line-height: 30px;
        font-size: 14px;
        color: #999;
        span{
          color: #666;
          margin-left: 60px;
        }
      }
    }

    .divider {
      height: 1px;
      background-color: #f0f0f0; /* 淡灰色 */
      margin: 20px 0; /* 上下各保留20px的间距 */
    }
    .goods-info{
      height: 225px;
      display: flex;
      align-items: center;
      margin-bottom: 20px;
      .goods-msg{
        p{
          line-height: 60px;
        }
        .goods-info-p{
          color: #000;
          font-size: 40px;
          text-align: center;
        }
        .goods-info-msg{
          font-size: 35px;
        }
      }
    }
    .users-info{
      height: 225px;
      display: flex;
      align-items: center;
      margin-bottom: 20px;
      .users-msg{
        p{
          line-height: 60px;
        }
        .users-info-p{
          color: #000;
          font-size: 40px;
          text-align: center;
        }
        .users-info-msg{
          font-size: 35px;
        }
      }
    }
    .sales-info{
      height: 225px;
      display: flex;
      align-items: center;
      margin-bottom: 20px;
      .sales-msg{
        p{
          line-height: 60px;
        }
        .sales-info-p{
          color: #000;
          font-size: 40px;
          text-align: center;
        }
        .sales-info-msg{
          font-size: 35px;
        }
      }
    }

  }

</style>