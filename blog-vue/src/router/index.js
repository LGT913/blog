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
    component: CreateArticle
  },
  {
    path: '/categories',
    name: 'CategoryManager',
    component: CategoryManager
  },
  {
    path: '/admin/config',
    name: 'SiteConfigAdmin',
    component: SiteConfigAdmin
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
