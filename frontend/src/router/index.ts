import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import AppManagePage from '@/pages/admin/AppManagePage.vue'
import AppChatPage from '@/pages/app/AppChatPage.vue'
import AppEditPage from '@/pages/app/AppEditPage.vue'
import ChatManagePage from "@/pages/admin/ChatManagePage.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  // 前端路由切换时回到顶部；浏览器前进/后退保留原位置
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition ?? { top: 0 }
  },
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
    },
    // 身份核验终端：独立亮色布局，不套用深色 HUD 外壳
    {
      path: '/user',
      component: AuthLayout,
      children: [
        {
          path: 'login',
          name: '用户登录',
          component: UserLoginPage,
        },
        {
          path: 'register',
          name: '用户注册',
          component: UserRegisterPage,
        },
      ],
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
    },
    {
      path: '/admin/appManage',
      name: '应用管理',
      component: AppManagePage,
    },
    {
      path: '/admin/chatManage',
      name: '对话管理',
      component: ChatManagePage,
    },
    {
      path: '/app/chat/:id',
      name: '应用对话',
      component: AppChatPage,
    },
    {
      path: '/app/edit/:id',
      name: '编辑应用',
      component: AppEditPage,
    },
  ],
})

export default router
