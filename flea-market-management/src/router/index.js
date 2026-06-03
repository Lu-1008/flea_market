import {createRouter,createWebHashHistory} from 'vue-router'
import { ElMessage } from 'element-plus'

//路由规则
const routes = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        name: 'login',
        component: () => import('@/views/Login.vue')
    },
    {
        path: '/main',
        name: 'main',
        component: () => import('@/views/Main.vue'),
        redirect: '/home',
        meta: {
            requiresAuth: true
        },
        children: [
            {
                path: '/home',
                name: 'home',
                component: () => import('@/views/Home.vue'),
                meta: {
                    requiresAuth: true,
                    roles: ['ADMIN', 'CATEGORY_MANAGER'] // 允许访问的角色
                }
            },
            {
                path: '/user',
                name: 'user',
                component: () => import('@/views/User.vue'),
                meta: {
                    requiresAuth: true,
                    roles: ['ADMIN'] // 只有管理员可以访问
                }
            },
            {
                path: '/mall',
                name: 'mall',
                component: () => import('@/views/Mall.vue'),
                meta: {
                    requiresAuth: true,
                    roles: ['ADMIN', 'CATEGORY_MANAGER'] // 允许管理员和分类管理员访问
                }
            },
            {
                path: '/category',
                name: 'category',
                component: () => import('@/views/Category.vue'),
                meta: {
                    requiresAuth: true,
                    roles: ['ADMIN', 'CATEGORY_MANAGER'] // 管理员和分类管理员可以访问
                }
            },
            {
                path: '/review',
                name: 'review',
                component: () => import('@/views/Review.vue'),
                meta: {
                    requiresAuth: true,
                    roles: ['ADMIN'] // 只有管理员可以访问
                }
            },
            {
                path: '/order',
                name: 'order',
                component: () => import('@/views/Order.vue'),
                meta: {
                    requiresAuth: true,
                    roles: ['ADMIN'] // 只有管理员可以访问
                }
            }
        ],
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/login'
    }
];

const router = createRouter(
    {
        history: createWebHashHistory(),
        routes,
    }
);

// 全局前置守卫，判断是否登录和权限控制
router.beforeEach((to, from, next) => {
    // 清除可能存在的过期登录状态
    const checkAndClearExpiredSession = () => {
        // 只使用sessionStorage，确保关闭浏览器后登录状态失效
        const loginTime = sessionStorage.getItem('loginTime');
        
        if (loginTime) {
            const expirationTime = parseInt(loginTime) + (2 * 60 * 60 * 1000); // 2小时过期
            if (Date.now() > expirationTime) {
                // 登录已过期，清除所有登录信息
                sessionStorage.removeItem('token');
                sessionStorage.removeItem('user');
                sessionStorage.removeItem('isLogin');
                sessionStorage.removeItem('loginTime');
                return false;
            }
            return sessionStorage.getItem('isLogin') === 'true' && sessionStorage.getItem('token');
        }
        return false;
    };
    
    // 获取登录状态
    const isLogin = checkAndClearExpiredSession();
    
    // 如果路由需要认证
    if (to.matched.some(record => record.meta.requiresAuth)) {
        // 如果未登录，重定向到登录页
        if (!isLogin) {
            ElMessage.warning('请先登录');
            next({
                path: '/login',
                query: { redirect: to.fullPath } // 保存原本要访问的路径
            });
            return;
        }
        
        // 如果已登录，检查权限
        if (to.meta && to.meta.roles) {
            try {
                // 从sessionStorage获取用户信息
                const userStr = sessionStorage.getItem('user');
                
                if (!userStr) {
                    // 用户信息不存在，重定向到登录页
                    sessionStorage.removeItem('isLogin'); // 清除可能不一致的登录状态
                    next('/login');
                    return;
                }
                
                const user = JSON.parse(userStr);
                const userRole = user.role;
                
                // 检查用户是否有权限访问该页面
                if (to.meta.roles.includes(userRole)) {
                    next(); // 有权限，放行
                } else {
                    // 无权限，提示并重定向到首页或有权限的页面
                    ElMessage.error('您没有权限访问该页面');
                    
                    // 根据用户角色和尝试访问的页面，选择合适的重定向目标
                    if (userRole === 'CATEGORY_MANAGER') {
                        // 如果分类管理员尝试访问订单管理或评价管理，则重定向到商品管理
                        if (to.path === '/order' || to.path === '/review') {
                            next('/mall');
                        } else {
                            // 其他情况重定向到首页
                            next('/home');
                        }
                    } else {
                        next('/home'); // 默认重定向到首页
                    }
                }
            } catch (error) {
                console.error('解析用户信息失败:', error);
                // 清除可能损坏的数据
                sessionStorage.removeItem('user');
                sessionStorage.removeItem('isLogin');
                next('/login');
            }
        } else {
            // 页面需要认证但没有特定角色要求，已登录用户可以访问
            next();
        }
    } else if (to.path === '/login') {
        // 如果已登录且访问登录页，重定向到首页
        if (isLogin) {
            next('/home');
        } else {
            // 未登录访问登录页，直接放行
            next();
        }
    } else {
        // 不需要认证的页面，直接放行
        next();
    }
});

export default router;