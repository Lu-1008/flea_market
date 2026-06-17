import { createRouter, createWebHistory } from 'vue-router'
import Main from '../views/Main.vue'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    name: 'Main',
    component: Main
  },
  // 购物车路由
  {
    path: '/cart',
    name: 'ShoppingCart',
    component: () => import('../views/ShoppingCart.vue'),
    meta: { requiresAuth: true } // 需要登录
  },
  // 个人中心路由
  {
    path: '/user',
    name: 'UserCenter',
    component: () => import('../views/user/UserCenter.vue'),
    meta: { requiresAuth: true }, // 需要登录
    children: [
      {
        path: '',
        redirect: '/user/profile'
      },
      {
        path: 'orders',
        name: 'UserOrders',
        component: () => import('../views/user/UserOrders.vue'),
        meta: { requiresAuth: true } // 需要登录
      },
      {
        path: 'items',
        name: 'UserItems',
        component: () => import('../views/user/UserItems.vue'),
        meta: { requiresAuth: true } // 需要登录
      },
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('../views/user/UserProfile.vue'),
        meta: { requiresAuth: true } // 需要登录
      },
      {
        path: 'pending',
        name: 'UserPending',
        component: () => import('../views/user/UserPending.vue'),
        meta: { requiresAuth: true } // 需要登录
      }
    ]
  },
  // 捕获所有未匹配路由，重定向到首页
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 导航守卫，检查登录状态
router.beforeEach((to, from, next) => {
  console.log('导航到:', to.path)
  
  // 检查是否需要登录
  if (to.matched.some(record => record.meta.requiresAuth)) {
    // 从sessionStorage获取token
    const token = sessionStorage.getItem('token')
    // 获取用户信息
    const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}')
    
    if (!token || !userInfo.userId) {
      ElMessage({ message: '请先登录', type: 'warning' })
      next({ path: '/' })
      return
    }
    
    // 特殊处理待处理订单页面
    if (to.name === 'UserPending') {
      // 获取用户角色或权限信息
      const userRole = userInfo.role || ''
      
      // 检查用户是否为卖家 - 这里只要用户登录就可以访问，因为用户同时可以是买家和卖家
      // 会在页面内部控制只显示当前用户作为卖家的待处理订单
      console.log('用户访问待处理订单页面，用户ID:', userInfo.userId)
    }
  }
  
  // 正常导航
  next()
})

export default router
