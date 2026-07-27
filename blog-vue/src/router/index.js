import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import ArticleDetail from '../views/ArticleDetail.vue'
import CreateArticle from '../views/CreateArticle.vue'
import CategoryManager from '../views/CategoryManager.vue'
import SiteConfigAdmin from '../views/SiteConfigAdmin.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: ArticleDetail
  },
  {
    path: '/create',
    name: 'CreateArticle',
    component: CreateArticle,
    meta: { requiresAuth: true }
  },
  {
    path: '/categories',
    name: 'CategoryManager',
    component: CategoryManager
  },
  {
    path: '/admin/config',
    name: 'SiteConfigAdmin',
    component: SiteConfigAdmin,
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('blog_token')

  if (to.meta.requiresAuth) {
    if (!token) {
      next('/')
      return
    }
  }

  if (to.meta.requiresAdmin) {
    const userStr = localStorage.getItem('blog_user')
    if (!userStr) {
      next('/')
      return
    }
    try {
      const user = JSON.parse(userStr)
      if (user.role !== 'ADMIN') {
        next('/')
        return
      }
    } catch (e) {
      next('/')
      return
    }
  }

  next()
})

export default router
