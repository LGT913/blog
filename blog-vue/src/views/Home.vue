<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSiteStore } from '../store/site'
import { articleApi, categoryApi, noticeApi } from '../api'

const router = useRouter()
const siteStore = useSiteStore()

const articles = ref([])
const categories = ref([])
const notices = ref([])
const rankingViews = ref([])
const rankingLatest = ref([])
const loading = ref(true)
const error = ref('')
const activeCategory = ref('all')
const searchQuery = ref('')

const loadingNotices = ref(true)
const loadingRankings = ref(true)

// 分页相关变量
const currentPage = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

const filteredArticles = computed(() => {
  let result = articles.value || []
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

const loadArticles = async (page = 0, size = 10) => {
  console.log('[分页] loadArticles 被调用，page:', page, '类型:', typeof page, 'size:', size)

  // 防御性编程：确保参数是数字
  const pageNum = Number(page)
  const sizeNum = Number(size)
  if (isNaN(pageNum) || isNaN(sizeNum)) {
    console.error('[分页] 非法参数，page:', page, 'size:', size)
    error.value = '分页参数错误'
    loading.value = false
    return
  }

  loading.value = true
  error.value = ''
  try {
    console.log('[分页] 请求接口，page:', pageNum, 'size:', sizeNum)
    const result = await articleApi.list(pageNum, sizeNum)
    console.log('[分页] 接口返回:', result)
    if (result) {
      articles.value = result.content || []
      totalElements.value = result.totalElements || 0
      totalPages.value = result.totalPages || 0
      currentPage.value = result.number ?? 0
      pageSize.value = result.size || 10
    } else {
      articles.value = []
      totalElements.value = 0
      totalPages.value = 0
    }
  } catch (e) {
    console.error('[分页] 接口错误:', e)
    error.value = e.message || '获取文章失败'
  } finally {
    loading.value = false
  }
}

// 切换分类时重置分页为第1页
watch(activeCategory, () => {
  currentPage.value = 0
  loadArticles(0, pageSize.value)
})

const handlePageChange = (page) => {
  console.log('[分页] handlePageChange 被调用，原始参数:', page, '类型:', typeof page)

  // 防御性编程：确保 page 是数字
  const pageNum = Number(page)
  if (isNaN(pageNum) || pageNum < 0) {
    console.error('[分页] 非法页码:', page, '使用默认值 0')
    loadArticles(0, pageSize.value)
    return
  }

  console.log('[分页] 转换后的页码:', pageNum)
  loadArticles(pageNum, pageSize.value)
}

const handlePageSizeChange = (size) => {
  console.log('[分页] 切换每页条数:', size)
  pageSize.value = Number(size) || 10
  loadArticles(0, pageSize.value)
}

const getVisiblePages = () => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value

  if (total <= 7) {
    for (let i = 0; i < total; i++) {
      pages.push(i)
    }
  } else {
    if (current <= 2) {
      for (let i = 0; i <= 4; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total - 1)
    } else if (current >= total - 3) {
      pages.push(0)
      pages.push('...')
      for (let i = total - 5; i < total; i++) {
        pages.push(i)
      }
    } else {
      pages.push(0)
      pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total - 1)
    }
  }
  return pages
}

const loadCategories = async () => {
  try {
    categories.value = await categoryApi.list()
  } catch (e) {
    console.error('获取分类失败:', e)
  }
}

const loadNotices = async () => {
  loadingNotices.value = true
  try {
    notices.value = await noticeApi.list()
  } catch (e) {
    console.error('获取公告失败:', e)
  } finally {
    loadingNotices.value = false
  }
}

const loadRankings = async () => {
  loadingRankings.value = true
  try {
    const [views, latest] = await Promise.all([
      articleApi.rankingByViews(),
      articleApi.rankingByLatest()
    ])
    rankingViews.value = views || []
    rankingLatest.value = latest || []
  } catch (e) {
    console.error('获取排行失败:', e)
  } finally {
    loadingRankings.value = false
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

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return time
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}年${month}月${day}日`
}

const goToArticle = (id) => {
  router.push(`/article/${id}`)
}

onMounted(() => {
  loadArticles()
  loadCategories()
  loadNotices()
  loadRankings()
})
</script>

<template>
  <div class="page">
    <!-- 站点横幅 -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-content">
          <div v-if="siteStore.state.loading" class="hero-skeleton">
            <div class="skeleton-line w-48"></div>
            <div class="skeleton-line w-96"></div>
            <div class="skeleton-line w-80"></div>
          </div>
          <template v-else>
            <div class="hero-badge">
              <span class="dot"></span>
              {{ siteStore.state.config.siteDesc }}
            </div>
            <h1 class="hero-title">{{ siteStore.state.config.slogan }}</h1>
            <p class="hero-desc">{{ siteStore.state.config.siteDesc }}</p>
          </template>
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

    <!-- 公告栏 -->
    <div v-if="!loadingNotices && notices.length > 0" class="notice-bar">
      <div class="notice-inner">
        <span class="notice-tag">📢 公告</span>
        <div class="notice-carousel">
          <div
            v-for="(notice, idx) in notices"
            :key="notice.id"
            class="notice-item"
            :style="{ animationDelay: `${idx * 4}s` }"
          >
            <span class="notice-title">{{ notice.title }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <main class="main">
      <div class="main-inner">
        <div class="content">
          <div class="section-header">
            <div class="section-title">
              <h2>{{ activeCategory === 'all' ? '全部文章' : categories.find(c => String(c.id) === String(activeCategory))?.name || '全部文章' }}</h2>
              <span class="count">{{ filteredArticles.length }} 篇</span>
            </div>
            <div class="search-box">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input v-model="searchQuery" type="text" placeholder="搜索文章..." />
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
            <p class="empty-desc">换个关键词试试</p>
          </div>

          <div v-else class="article-list">
            <article
              v-for="article in filteredArticles"
              :key="article.id"
              class="article-card"
              @click="goToArticle(article.id)"
            >
              <div class="card-header">
                <span class="category-tag">{{ categories.find(c => String(c.id) === String(article.categoryId))?.name || '未分类' }}</span>
                <span class="date">{{ formatTime(article.createTime) }}</span>
              </div>
              <h3 class="article-title">{{ article.title }}</h3>
              <div v-if="article.summary" class="ai-summary">
                <div class="ai-summary-bar"></div>
                <span class="ai-summary-text">{{ article.summary }}</span>
              </div>
              <div class="card-footer">
                <span class="read-more">阅读全文 →</span>
              </div>
            </article>

            <!-- 分页组件 -->
            <div v-if="totalPages > 1" class="pagination">
              <div class="pagination-info">
                共 {{ totalElements }} 篇文章
              </div>
              <div class="pagination-controls">
                <button
                  class="pagination-btn"
                  :class="{ disabled: currentPage === 0 }"
                  @click="() => handlePageChange(currentPage - 1)"
                  :disabled="currentPage === 0"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="15 18 9 12 15 6"></polyline>
                  </svg>
                </button>
                <div class="pagination-pages">
                  <template v-for="(page, idx) in getVisiblePages()" :key="`${page}-${idx}`">
                    <span
                      v-if="page === '...'"
                      class="pagination-ellipsis"
                    >...</span>
                    <button
                      v-else
                      class="pagination-page"
                      :class="{ active: page === currentPage }"
                      @click="() => handlePageChange(Number(page))"
                    >
                      {{ page + 1 }}
                    </button>
                  </template>
                </div>
                <button
                  class="pagination-btn"
                  :class="{ disabled: currentPage >= totalPages - 1 }"
                  @click="() => handlePageChange(currentPage + 1)"
                  :disabled="currentPage >= totalPages - 1"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="9 18 15 12 9 6"></polyline>
                  </svg>
                </button>
                <select
                  class="pagination-size"
                  :value="pageSize"
                  @change="handlePageSizeChange(Number($event.target.value))"
                >
                  <option :value="5">5条/页</option>
                  <option :value="10">10条/页</option>
                  <option :value="20">20条/页</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <aside class="sidebar">
          <!-- 排行 -->
          <div class="sidebar-card">
            <h3 class="sidebar-title">📊 阅读排行</h3>
            <div v-if="loadingRankings" class="sidebar-loading">
              <div class="skeleton-line"></div>
              <div class="skeleton-line"></div>
              <div class="skeleton-line"></div>
            </div>
            <div v-else-if="rankingViews.length === 0" class="sidebar-empty">暂无数据</div>
            <div v-else class="ranking-list">
              <div
                v-for="(item, idx) in rankingViews.slice(0, 5)"
                :key="item.id"
                class="ranking-item"
                @click="goToArticle(item.id)"
              >
                <span class="ranking-index" :class="{ top: idx < 3 }">{{ idx + 1 }}</span>
                <div class="ranking-content">
                  <span class="ranking-title">{{ item.title }}</span>
                  <span class="ranking-meta">{{ item.viewCount || 0 }} 阅读</span>
                </div>
              </div>
            </div>
          </div>

          <div class="sidebar-card">
            <h3 class="sidebar-title">🕐 最新发布</h3>
            <div v-if="loadingRankings" class="sidebar-loading">
              <div class="skeleton-line"></div>
              <div class="skeleton-line"></div>
              <div class="skeleton-line"></div>
            </div>
            <div v-else-if="rankingLatest.length === 0" class="sidebar-empty">暂无数据</div>
            <div v-else class="ranking-list">
              <div
                v-for="item in rankingLatest.slice(0, 5)"
                :key="item.id"
                class="ranking-item"
                @click="goToArticle(item.id)"
              >
                <div class="ranking-content">
                  <span class="ranking-title">{{ item.title }}</span>
                  <span class="ranking-meta">{{ formatTime(item.createTime) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 分类 -->
          <div class="sidebar-card">
            <h3 class="sidebar-title">📂 文章分类</h3>
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

.hero {
  padding: 64px 0 48px;
  background: linear-gradient(180deg, #ffffff 0%, var(--color-bg) 100%);
}

.hero-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 var(--space-6);
}

.hero-content {
  max-width: 640px;
  animation: fadeInUp 0.6s ease-out;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  margin-bottom: 24px;
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
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.25;
  letter-spacing: -1px;
  margin-bottom: 16px;
}

.hero-desc {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
  margin-bottom: 32px;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 32px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-number {
  font-size: 30px;
  font-weight: 700;
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

.hero-skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.skeleton-line {
  height: 16px;
  background: linear-gradient(90deg, var(--color-border-light) 25%, var(--color-bg-hover) 50%, var(--color-border-light) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

.skeleton-line.w-48 { width: 48%; }
.skeleton-line.w-96 { width: 96%; }
.skeleton-line.w-80 { width: 80%; }

/* 公告栏 */
.notice-bar {
  background: var(--color-primary-light);
  border-bottom: 1px solid var(--color-primary-lighter);
}

.notice-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 10px var(--space-6);
  display: flex;
  align-items: center;
  gap: 12px;
}

.notice-tag {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
  white-space: nowrap;
  flex-shrink: 0;
}

.notice-carousel {
  overflow: hidden;
  flex: 1;
  position: relative;
  height: 22px;
}

.notice-item {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  animation: noticeScroll 12s infinite;
  opacity: 0;
  transform: translateY(8px);
}

@keyframes noticeScroll {
  0%, 20% { opacity: 1; transform: translateY(0); }
  25%, 100% { opacity: 0; transform: translateY(-8px); }
}

.notice-title {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.main {
  flex: 1;
  padding: 40px 0 64px;
}

.main-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 var(--space-6);
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 40px;
  align-items: start;
}

.content {
  min-width: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
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
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  outline: none;
  transition: border-color var(--transition-fast);
}

.search-box input:focus {
  border-color: var(--color-primary);
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.article-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 24px;
  cursor: pointer;
  transition: all var(--transition-normal);
  animation: fadeInUp 0.3s ease-out;
}

.article-card:hover {
  border-color: var(--color-border);
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.category-tag {
  display: inline-block;
  padding: 4px 12px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-radius: var(--radius-full);
}

.date {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.article-title {
  font-size: 22px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  line-height: 1.4;
  margin-bottom: 12px;
  transition: color var(--transition-fast);
}

.article-card:hover .article-title {
  color: var(--color-primary);
}

.ai-summary {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: var(--color-bg);
  border-radius: 6px;
  margin-bottom: 12px;
}

.ai-summary-bar {
  width: 3px;
  flex-shrink: 0;
  background: #3b82f6;
  border-radius: 2px;
}

.ai-summary-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-secondary);
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.read-more {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  transition: gap var(--transition-fast);
}

.article-card:hover .read-more {
  gap: 8px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  margin-bottom: 16px;
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
  margin-bottom: 20px;
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  border-radius: 50%;
}

.empty-icon.error {
  color: var(--color-error);
  background: var(--color-error-light);
}

.empty-text {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  margin-bottom: 20px;
}

.retry-btn {
  padding: 10px 24px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: #ffffff;
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.retry-btn:hover {
  background: var(--color-primary-hover);
}

.sidebar {
  position: sticky;
  top: calc(var(--header-height) + 24px);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.sidebar-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: 16px;
}

.sidebar-loading {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar-loading .skeleton-line {
  height: 32px;
  border-radius: 6px;
}

.sidebar-empty {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  text-align: center;
  padding: 12px 0;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.ranking-item:hover {
  background: var(--color-bg-hover);
}

.ranking-index {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  border-radius: 4px;
  flex-shrink: 0;
}

.ranking-index.top {
  color: #ffffff;
  background: var(--color-primary);
}

.ranking-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ranking-title {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.ranking-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.ranking-item:hover .ranking-title {
  color: var(--color-primary);
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
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
  width: 100%;
  border: none;
  background: none;
  cursor: pointer;
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

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

/* 分页样式 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32px 0;
  border-top: 1px solid var(--color-border-light);
  margin-top: 16px;
}

.pagination-info {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.pagination-btn:hover:not(.disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.pagination-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination-pages {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-page {
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  padding: 0 8px;
}

.pagination-page:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.pagination-page.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #ffffff;
}

.pagination-ellipsis {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  padding: 0 4px;
}

.pagination-size {
  padding: 8px 12px;
  font-size: var(--font-size-sm);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  cursor: pointer;
  outline: none;
  transition: border-color var(--transition-fast);
}

.pagination-size:focus {
  border-color: var(--color-primary);
}
</style>
