import { createRouter, createWebHistory } from 'vue-router'

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
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 路由守卫 — 未登录跳转登录页
router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '智观·古建'
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
