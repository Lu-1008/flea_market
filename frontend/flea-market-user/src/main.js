import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import axios from 'axios'
import router from './router'
import pinia from './store' 
import App from './App.vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import "@/assets/less/index.less"
import emitter from './utils/eventBus'

// 配置axios默认值
axios.defaults.baseURL = import.meta.env.VITE_API_URL || '/api'
axios.defaults.timeout = 15000
axios.defaults.headers.common['Content-Type'] = 'application/json'

// 请求拦截器
axios.interceptors.request.use(
  config => {
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
axios.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response) {
      // 处理401未授权错误
      if (error.response.status === 401) {
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('userInfo')
        // 移除可能不存在的路由跳转
        console.error('未授权访问')
      }
    }
    return Promise.reject(error)
  }
)

const app = createApp(App)

// 全局注册所有Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 注册全局事件总线
app.provide('emitter', emitter)

// 挂载应用
app.mount('#app')

console.log('正在初始化Vue应用...')

console.log('Vue应用挂载完成')
