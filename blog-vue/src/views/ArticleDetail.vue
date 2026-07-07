<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, categoryApi } from '../api'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const article = ref(null)
const categories = ref([])
const loading = ref(true)
const error = ref('')

const categoryName = computed(() => {
  if (!article.value) return ''
  const cat = categories.value.find(c => String(c.id) === String(article.value.categoryId))
  return cat ? cat.name : '未分类'
})

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}年${month}月${day}日 ${hours}:${minutes}`
}

const wordCount = computed(() => {
  if (!article.value?.content) return 0
  return article.value.content.length
})

const readTime = computed(() => {
  const words = wordCount.value
  const minutes = Math.ceil(words / 300)
  return Math.max(1, minutes)
})

const isOwner = computed(() => {
  return article.value && userStore.state.isLoggedIn && userStore.state.user.id === article.value.userId
})

const loadArticle = async () => {
  loading.value = true
  error.value = ''

  try {
    article.value = await articleApi.get(route.params.id)
  } catch (e) {
    error.value = e.message || '获取文章失败'
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    categories.value = await categoryApi.list()
  } catch (e) {
    console.error('获取分类失败:', e)
  }
}

const handleDelete = async () => {
  if (!confirm('确定要删除这篇文章吗？')) return

  try {
    await articleApi.delete(article.value.id)
    router.push('/')
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

const handleEdit = () => {
  router.push(`/create?id=${article.value.id}`)
}

onMounted(() => {
  loadArticle()
  loadCategories()
})
</script>

<template>
  <div class="page">
    <main class="main">
      <div class="article-container">
        <!-- 返回按钮 -->
        <button class="back-btn" @click="router.push('/')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          返回首页
        </button>

        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>正在加载文章...</p>
        </div>

        <div v-else-if="error" class="error-state">
          <div class="error-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
          </div>
          <p class="error-text">{{ error }}</p>
          <button class="retry-btn" @click="loadArticle">重新加载</button>
        </div>

        <article v-else-if="article" class="article">
          <!-- 文章头部 -->
          <header class="article-header">
            <div class="article-meta-top">
              <span class="category-tag">{{ categoryName }}</span>
            </div>

            <h1 class="article-title">{{ article.title }}</h1>

            <div class="article-info">
              <div class="author">
                <div class="author-avatar">
                  {{ article.userId }}
                </div>
                <div class="author-info">
                  <span class="author-name">作者ID: {{ article.userId }}</span>
                  <span class="publish-time">发布于 {{ formatTime(article.createTime) }}</span>
                </div>
              </div>

              <div class="article-stats">
                <span class="stat-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                  </svg>
                  {{ wordCount }} 字
                </span>
                <span class="stat-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polyline points="12 6 12 12 16 14"></polyline>
                  </svg>
                  {{ readTime }} 分钟阅读
                </span>
                <span v-if="article.updateTime && article.updateTime !== article.createTime" class="stat-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 4v6h6"></path>
                    <path d="M23 20v-6h-6"></path>
                    <path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15"></path>
                  </svg>
                  更新于 {{ formatTime(article.updateTime) }}
                </span>
              </div>
            </div>
          </header>

          <!-- 分割线 -->
          <div class="divider"></div>

          <!-- 操作栏 -->
          <div v-if="isOwner" class="action-bar">
            <button class="action-btn edit" @click="handleEdit">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 20h9"></path>
                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
              </svg>
              编辑文章
            </button>
            <button class="action-btn delete" @click="handleDelete">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
              删除文章
            </button>
          </div>

          <!-- 文章内容 -->
          <div class="article-content">
            <p v-for="(para, idx) in article.content.split('\n').filter(p => p.trim())" :key="idx">
              {{ para }}
            </p>
          </div>
        </article>
      </div>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main {
  flex: 1;
  padding: var(--space-10) 0 var(--space-16);
}

.article-container {
  max-width: 760px;
  margin: 0 auto;
  padding: 0 var(--space-6);
  animation: fadeInUp 0.4s ease-out;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: var(--space-8);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  padding: 8px 12px;
  margin-left: -12px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.back-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

/* 加载/错误状态 */
.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-20) 0;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  margin-bottom: var(--space-4);
  animation: spin 0.8s linear infinite;
}

.loading-state p {
  font-size: var(--font-size-base);
  color: var(--color-text-muted);
}

.error-icon {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-5);
  color: var(--color-error);
  background: var(--color-error-light);
  border-radius: var(--radius-full);
}

.error-text {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-5);
}

.retry-btn {
  padding: 10px 24px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: #ffffff;
  background: var(--color-primary);
  border-radius: var(--radius-md);
}

.retry-btn:hover {
  background: var(--color-primary-hover);
}

/* 文章样式 */
.article {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: var(--space-12) var(--space-10);
  box-shadow: var(--shadow-sm);
}

.article-header {
  margin-bottom: var(--space-8);
}

.article-meta-top {
  margin-bottom: var(--space-5);
}

.category-tag {
  display: inline-block;
  padding: 6px 14px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-radius: var(--radius-full);
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.article-title {
  font-size: 36px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1.3;
  letter-spacing: -0.8px;
  margin-bottom: var(--space-6);
}

.article-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
  flex-wrap: wrap;
}

.author {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), #7c3aed);
  color: #ffffff;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.publish-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.article-stats {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  flex-wrap: wrap;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.divider {
  height: 1px;
  background: var(--color-border);
  margin: var(--space-8) 0;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-8);
  padding: var(--space-4);
  background: var(--color-bg);
  border-radius: var(--radius-md);
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.action-btn.edit {
  color: var(--color-primary);
  background: #ffffff;
  border: 1px solid var(--color-border);
}

.action-btn.edit:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.action-btn.delete {
  color: var(--color-error);
  background: #ffffff;
  border: 1px solid #fecaca;
}

.action-btn.delete:hover {
  background: var(--color-error-light);
  border-color: var(--color-error);
}

.article-content {
  font-size: var(--font-size-lg);
  line-height: 2;
  color: var(--color-text-primary);
  letter-spacing: 0.2px;
}

.article-content p {
  margin-bottom: var(--space-5);
  text-indent: 2em;
}

.article-content p:last-child {
  margin-bottom: 0;
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
