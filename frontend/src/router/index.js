import { createRouter, createWebHistory } from 'vue-router'
import { decodeTokenPayload } from '@/stores/user'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/CommunityFeedView.vue'),
    meta: { title: '文化社区 - 智观·古建' }
  },
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: () => import('@/views/PostDetailView.vue'),
    meta: { title: '沉浸展示' }
  },
  {
    path: '/huanzhu',
    name: 'HuanZhu',
    component: () => import('@/views/HuanZhuView.vue'),
    meta: { title: '一键幻筑', requiresAuth: true }
  },
  {
    path: '/zhixi',
    name: 'ZhiXi',
    component: () => import('@/views/ZhiXiView.vue'),
    meta: { title: '古建智析' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/NotificationListView.vue'),
    meta: { title: '通知中心', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: () => import('@/views/UserProfileView.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { title: '设置', requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/AdminDashboardView.vue'),
    meta: { title: '审核工作台', requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '智观·古建'
  const token = localStorage.getItem('token')

  // 需要登录的页面
  if (to.meta.requiresAuth && !token) {
    return next('/login')
  }

  // 需要管理员权限的页面 — 前端路由守卫
  if (to.meta.requiresAdmin) {
    const payload = token ? decodeTokenPayload(token) : null
    if (!payload) return next('/login')
    if (payload.role !== 'ADMIN') return next('/home')
  }

  next()
})

export default router
