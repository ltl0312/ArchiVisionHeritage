import { createRouter, createWebHistory } from 'vue-router'
import { decodeTokenPayload } from '@/stores/user'

/**
 * 路由表（V4 信息架构 · 计划书 §5-P2 路由与导航映射）
 *
 * 一级入口 5 项由 SideNav 持有：营造志 / 古建智析 / 一键幻筑 / 我的档案 / 匠人社区。
 * 通知中心 / 个人设置 / 审核工作台是二级页面：入口在顶栏铃铛与账户菜单，
 * 不作为侧栏导航项（与设计稿一致）。
 *
 * meta 字段：
 *   title    浏览器标题
 *   heading  顶栏主标题（TopBar 单一来源）
 *   sub      顶栏副标题
 */
const routes = [
  { path: '/', redirect: '/home' },

  /* ── 营造志（首页 · 决策 1）：能力宣言 + 数据可信条 + 双能力卡 + 精选档案 ── */
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/HeritageFeedView.vue'),
    meta: {
      title: '营造志 - 智观·古建',
      heading: '营造志',
      sub: '古建数字孪生 · 文化传承平台'
    }
  },

  /* ── 匠人社区（决策 4）：原文化社区信息流迁入此入口 ── */
  {
    path: '/community',
    name: 'CommunitySquare',
    component: () => import('@/views/CommunitySquareView.vue'),
    meta: {
      title: '匠人社区 - 智观·古建',
      heading: '匠人社区',
      sub: '大家正在营造与解析的数字档案'
    }
  },
  {
    path: '/community/post/:id',
    name: 'PostDetail',
    component: () => import('@/views/PostDetailView.vue'),
    meta: {
      title: '档案详情 - 智观·古建',
      heading: '档案详情',
      sub: '三维查看 · 评论与互动'
    }
  },
  /* 旧路径兜底：/post/:id → /community/post/:id（避免外链与书签失效） */
  { path: '/post/:id', redirect: (to) => `/community/post/${to.params.id}` },

  /* ── 一键幻筑（设计稿 F4）：创作区 + 工序时间轴 ── */
  {
    path: '/huanzhu',
    name: 'HuanZhu',
    component: () => import('@/views/HuanZhuView.vue'),
    meta: {
      title: '一键幻筑 - 智观·古建',
      heading: '一键幻筑',
      sub: 'AI 三维生成 · 异步任务与幂等防重',
      requiresAuth: true
    }
  },

  /* ── 古建智析（设计稿 F3）：三区工作台 ── */
  {
    path: '/zhixi',
    name: 'ZhiXi',
    component: () => import('@/views/ZhiXiView.vue'),
    meta: {
      title: '古建智析 - 智观·古建',
      heading: '古建智析',
      sub: 'VGGT 高精几何解析 · 每日节制'
    }
  },

  /* ── 我的档案（决策 6）：改造现有 UserProfileView，不另建列表页 ── */
  {
    path: '/archive',
    name: 'MyArchive',
    component: () => import('@/views/UserProfileView.vue'),
    meta: {
      title: '我的档案 - 智观·古建',
      heading: '我的档案',
      // 计划书原为「我的解析 / 我的幻筑 / 我的收藏」三 Tab；因 PostBriefResponse
      // 不含 modelAssetId、后端也无按用户持久化的解析记录，无法区分实景解析与
      // AI 幻筑，故落地为「我的档案 / 我的收藏」两 Tab + 真实 status 筛选。
      sub: '我的档案 · 我的收藏',
      requiresAuth: true
    }
  },
  /* 数字档案沉浸详情（设计稿 F5） */
  {
    path: '/archive/:id',
    name: 'ArchiveDetail',
    component: () => import('@/views/ArchiveDetailView.vue'),
    meta: {
      title: '数字档案 - 智观·古建',
      heading: '数字档案',
      sub: '形制档案 · 数据溯源'
    }
  },
  /* 旧路径兜底：/profile → /archive */
  { path: '/profile', redirect: '/archive' },

  /* ── 二级页面：入口在顶栏 / 账户菜单 ── */
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/NotificationListView.vue'),
    meta: {
      title: '通知中心 - 智观·古建',
      heading: '通知中心',
      sub: '数字锦盒与系统消息',
      requiresAuth: true
    }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: {
      title: '个人设置 - 智观·古建',
      heading: '个人设置',
      sub: '视觉偏好 · 账号资料 · 通知策略 · 关于平台',
      requiresAuth: true
    }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/AdminDashboardView.vue'),
    meta: {
      title: '审核工作台 - 智观·古建',
      heading: '审核工作台',
      sub: '逐条决策 · 驳回须填理由',
      requiresAuth: true,
      requiresAdmin: true
    }
  },

  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录 - 智观·古建', heading: '登录', sub: '' }
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
