import { createRouter, createWebHistory } from 'vue-router'
import { StorageKey } from '../utils/constants'

/**
 * 路由表
 * meta.requiresAuth: 需要登录
 * meta.requiresAdmin: 需要 ADMIN 角色（双重检查：token + role）
 */
const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('../views/ArticleDetail.vue')
  },
  {
    path: '/create',
    name: 'CreateArticle',
    component: () => import('../views/CreateArticle.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ranking',
    name: 'Ranking',
    component: () => import('../views/Ranking.vue')
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: () => import('../views/UserProfile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('../views/AdminDashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/categories',
    name: 'CategoryManager',
    component: () => import('../views/CategoryManager.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/config',
    name: 'SiteConfigAdmin',
    component: () => import('../views/SiteConfigAdmin.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 安全地从 localStorage 读取并解析 JSON
 * 处理数据损坏的情况
 */
const safeGetJSON = (key) => {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return null
    return JSON.parse(raw)
  } catch (e) {
    // 数据损坏，清除脏数据
    localStorage.removeItem(key)
    return null
  }
}

/**
 * 全局路由守卫
 * 1. 通过 localStorage 中的 blog_token 判断登录状态
 * 2. meta.requiresAuth 的路由：未登录则跳转首页
 * 3. meta.requiresAdmin 的路由：双重检查 token + role === 'ADMIN'
 */
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem(StorageKey.TOKEN)

  // 需要登录的路由守卫
  if (to.meta.requiresAuth) {
    if (!token) {
      next('/')
      return
    }
  }

  // 需要管理员权限的路由守卫（双重检查）
  if (to.meta.requiresAdmin) {
    // 第一重：检查 token
    if (!token) {
      next('/')
      return
    }
    // 第二重：检查 role
    const user = safeGetJSON(StorageKey.USER)
    if (!user || user.role !== 'ADMIN') {
      next('/')
      return
    }
  }

  next()
})

export default router