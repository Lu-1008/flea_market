<script setup>
  import {ref, computed, onMounted} from "vue";
  import {useAllDataStore} from "@/stores";
  import {useRouter} from "vue-router";

  const router = useRouter();

  // 所有可能的菜单项
  const allMenuItems = [
    {
      path: '/home',
      name: 'home',
      label: '首页',
      icon: 'home-filled',
      url: 'Home/Home',
      roles: ['ADMIN', 'CATEGORY_MANAGER'] // 允许访问的角色
    },
    {
      path: '/mall',
      name: 'mall',
      label: '商品管理',
      icon: 'shopping-cart',
      url: 'Mall',
      roles: ['ADMIN', 'CATEGORY_MANAGER'] // 修改为管理员和分类管理员都可以访问
    },
    {
      path: '/category',
      name: 'category',
      label: '分类管理',
      icon: 'menu',
      url: 'Category/Category',
      roles: ['ADMIN', 'CATEGORY_MANAGER'] // 管理员和分类管理员可以访问
    },
    {
      path: '/order',
      name: 'order',
      label: '订单管理',
      icon: 'document',
      url: 'Order',
      roles: ['ADMIN'] // 只有管理员可以访问
    },
    {
      path: '/review',
      name: 'review',
      label: '评价管理',
      icon: 'star',
      url: 'Review',
      roles: ['ADMIN'] // 只有管理员可以访问
    },
    {
      path: '/user',
      name: 'user',
      label: '用户管理',
      icon: 'user',
      url: 'User/User',
      roles: ['ADMIN'] // 只有管理员可以访问
    }
  ];

  // 当前用户可以访问的菜单项
  const list = ref([]);

  // 获取用户角色并过滤菜单
  const getUserRoleAndFilterMenu = () => {
    try {
      // 只使用sessionStorage
      const userStr = sessionStorage.getItem('user');
      if (userStr) {
        const user = JSON.parse(userStr);
        const userRole = user.role;
        
        // 根据用户角色过滤菜单
        list.value = allMenuItems.filter(item => item.roles.includes(userRole));
      } else {
        // 如果用户信息不存在，重定向到登录页
        router.push('/login');
      }
    } catch (error) {
      console.error('获取用户角色失败:', error);
      // 如果出错，默认只显示首页
      list.value = [allMenuItems[0]];
    }
  };

  // 组件挂载时获取用户角色并过滤菜单
  onMounted(() => {
    getUserRoleAndFilterMenu();
  });

  const noChildren = computed(() => list.value.filter(item => !item.children));
  const hasChildren = computed(() => list.value.filter(item => item.children));

  const store = useAllDataStore();
  const isCollapse = computed(()=>store.state.isCollapse);
  //width
  const width = computed(()=>store.state.isCollapse ? '64px' : '180px');

  // 菜单点击事件
  const handleMenuClick = (item) => {
    router.push(item.path);
  }

  // 子菜单点击事件
  const handleSubMenuClick = (subItem) => {
    router.push(subItem.path);
  }
</script>

<template>
  <el-aside :width="width">
    <el-menu
        :default-active="$route.path"
        class="el-menu"
        background-color="#545c64"
        text-color="#fff"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
    >
      <h3 v-show="!isCollapse">市场后台管理系统</h3>
      <h3 v-show="isCollapse">后台</h3>
      <el-menu-item
          v-for="item in noChildren"
          :index="item.path"
          :key="item.path"
          @click="handleMenuClick(item)"
      >
        <component class="icons" :is="item.icon"></component>
        <span>{{item.label}}</span>
      </el-menu-item>
      <el-sub-menu
          v-for="item in hasChildren"
          :index="item.path"
          :key="item.path"
      >
        <template #title>
          <component class="icons" :is="item.icon"></component>
          <span>{{item.label}}</span>
        </template>
        <el-menu-item-group>
          <el-menu-item
          v-for="(subItem,subIndex) in item.children"
          :index="subItem.path"
          :key="subItem.path"
          @click="handleSubMenuClick(subItem)"
          >
          <component class="icons" :is="subItem.icon"></component>
          <span>{{subItem.label}}</span></el-menu-item>
        </el-menu-item-group>
      </el-sub-menu>
    </el-menu>
  </el-aside>
</template>

<style lang="less" scoped>
  .icons{
    width: 18px;
    height: 18px;
    margin-right: 5px;
  }
  .el-menu{
    border-right: none;
    h3{
      line-height: 48px;
      color: #fff;
      text-align: center;
    }
  }
  .el-aside{
    height: 100%;
    background-color: #545c64;
  }
</style>