<script setup>
import { ref, computed, onMounted } from 'vue'
import ArticleCard from '../components/ArticleCard.vue'
import { articleApi, categoryApi } from '../api'

const articles = ref([])
const categories = ref([])
const loading = ref(true)
const error = ref('')
const activeCategory = ref('all')
const searchQuery = ref('')

const filteredArticles = computed(() => {
  let result = articles.value || []

  console.log('[Home] filteredArticles 计算:', articles.value?.length, activeCategory.value)

  if (activeCategory.value !== 'all') {
    result = result.filter(a => String(a.categoryId) === String(activeCategory.value))
  }

  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(a =>
      a.title.toLowerCase().includes(q) ||
      (a.content && a.content.toLowerCase().includes(q))
    )
  }

  return result
})

const categoryCount = computed(() => {
  const counts = { all: articles.value.length }
  categories.value.forEach(cat => {
    counts[cat.id] = articles.value.filter(a => String(a.categoryId) === String(cat.id)).length
  })
  return counts
})

const loadArticles = async () => {
  loading.value = true
  error.value = ''

  try {
    const result = await articleApi.list()
    console.log('[Home] 文章列表数据:', result)
    articles.value = result || []
    console.log('[Home] articles.value:', articles.value)
    console.log('[Home] filteredArticles:', filteredArticles.value)
  } catch (e) {
    error.value = e.message || '获取文章失败'
    console.error('[Home] 获取文章失败:', e)
  } finally {
    loading.value = false
    console.log('[Home] loading:', loading.value, 'error:', error.value)
  }
}

const loadCategories = async () => {
  try {
    categories.value = await categoryApi.list()
  } catch (e) {
    console.error('获取分类失败:', e)
  }
}

const handleDelete = async (id) => {
  try {
    await articleApi.delete(id)
    articles.value = articles.value.filter(a => a.id !== id)
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

onMounted(() => {
  loadArticles()
  loadCategories()
})
</script>

<template>
  <div class="page">
    <!-- Hero 区域 -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-content">
          <div class="hero-badge">
            <span class="dot"></span>
            分享 · 记录 · 成长
          </div>
          <h1 class="hero-title">记录思考与灵感的<br/>个人空间</h1>
          <p class="hero-desc">
            在这里写下你的故事，分享你的见解，与志同道合的人一起成长。
            每一篇文字都是时光的印记。
          </p>
          <div class="hero-stats">
            <div class="stat">
              <div class="stat-number">{{ articles.length }}</div>
              <div class="stat-label">篇文章</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat">
              <div class="stat-number">{{ categories.length }}</div>
              <div class="stat-label">个分类</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 主内容区 -->
    <main class="main">
      <div class="main-inner">
        <!-- 左侧 - 文章列表 -->
        <div class="content">
          <div class="section-header">
            <div class="section-title">
              <h2>{{ activeCategory === 'all' ? '全部文章' : categories.find(c => String(c.id) === String(activeCategory))?.name }}</h2>
              <span class="count">{{ filteredArticles.length }} 篇</span>
            </div>
            <div class="search-box">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索文章..."
              />
            </div>
          </div>

          <div v-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>正在加载文章...</p>
          </div>

          <div v-else-if="error" class="empty-state">
            <div class="empty-icon error">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            </div>
            <p class="empty-text">{{ error }}</p>
            <button class="retry-btn" @click="loadArticles">重新加载</button>
          </div>

          <div v-else-if="filteredArticles.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="9" y1="13" x2="15" y2="13"></line>
                <line x1="9" y1="17" x2="15" y2="17"></line>
              </svg>
            </div>
            <p class="empty-text">暂无相关文章</p>
            <p class="empty-desc">换个关键词试试，或者去写第一篇文章吧</p>
          </div>

          <div v-else class="article-list">
            <ArticleCard
              v-for="(article, index) in filteredArticles"
              :key="article.id"
              :article="article"
              :categories="categories"
              :index="index"
              @delete="handleDelete"
            />
          </div>
        </div>

        <!-- 右侧 - 侧边栏 -->
        <aside class="sidebar">
          <div class="sidebar-card">
            <h3 class="sidebar-title">文章分类</h3>
            <div class="category-list">
              <button
                class="category-item"
                :class="{ active: activeCategory === 'all' }"
                @click="activeCategory = 'all'"
              >
                <span class="cat-name">全部</span>
                <span class="cat-count">{{ categoryCount.all }}</span>
              </button>
              <button
                v-for="cat in categories"
                :key="cat.id"
                class="category-item"
                :class="{ active: activeCategory === cat.id }"
                @click="activeCategory = cat.id"
              >
                <span class="cat-name">{{ cat.name }}</span>
                <span class="cat-count">{{ categoryCount[cat.id] || 0 }}</span>
              </button>
            </div>
          </div>

          <div class="sidebar-card">
            <h3 class="sidebar-title">关于博客</h3>
            <p class="about-text">
              这是一个专注于分享技术见解与生活感悟的个人博客。
              在这里，我记录学习过程中的点滴收获，也分享生活中的思考与感悟。
            </p>
          </div>
        </aside>
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

/* Hero 区域 */
.hero {
  padding: var(--space-16) 0 var(--space-12);
  background: linear-gradient(180deg, #ffffff 0%, var(--color-bg) 100%);
}

.hero-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 var(--space-6);
}

.hero-content {
  max-width: 600px;
  animation: fadeInUp 0.6s ease-out;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: 6px 14px;
  margin-bottom: var(--space-6);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-radius: var(--radius-full);
}

.dot {
  width: 6px;
  height: 6px;
  background: var(--color-primary);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.hero-title {
  font-size: 42px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1.25;
  letter-spacing: -1px;
  margin-bottom: var(--space-5);
}

.hero-desc {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
  margin-bottom: var(--space-8);
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: var(--space-8);
}

.stat {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.stat-number {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--color-border);
}

/* 主内容区 */
.main {
  flex: 1;
  padding: var(--space-10) 0 var(--space-16);
}

.main-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 var(--space-6);
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: var(--space-10);
  align-items: start;
}

/* 左侧内容 */
.content {
  min-width: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
  margin-bottom: var(--space-6);
}

.section-title {
  display: flex;
  align-items: baseline;
  gap: var(--space-3);
}

.section-title h2 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  letter-spacing: -0.3px;
}

.count {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.search-box {
  position: relative;
  width: 280px;
}

.search-box svg {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
}

.search-box input {
  width: 100%;
  padding: 10px 14px 10px 40px;
  font-size: var(--font-size-sm);
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

/* 加载状态 */
.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16) 0;
}

.spinner {
  width: 32px;
  height: 32px;
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

.empty-icon {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-5);
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  border-radius: var(--radius-full);
}

.empty-icon.error {
  color: var(--color-error);
  background: var(--color-error-light);
}

.empty-text {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2);
}

.empty-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
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

/* 侧边栏 */
.sidebar {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.sidebar-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

.sidebar-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-4);
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  text-align: left;
}

.category-item:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}

.category-item.active {
  color: var(--color-primary);
  background: var(--color-primary-light);
  font-weight: var(--font-weight-medium);
}

.cat-count {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  min-width: 24px;
  text-align: center;
}

.category-item.active .cat-count {
  color: var(--color-primary);
  background: #ffffff;
}

.about-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
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

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
