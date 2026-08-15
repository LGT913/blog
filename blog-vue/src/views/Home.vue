<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useSiteStore } from '../store/site'
import { articleApi, categoryApi } from '../api'
import ArticleCard from '../components/ArticleCard.vue'

const router = useRouter()
const siteStore = useSiteStore()

const articles = ref([])
const categories = ref([])
const loading = ref(true)
const error = ref('')
const activeCategory = ref('all')
const searchQuery = ref('')

const currentPage = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

let searchTimer = null

// ========== 分类标签滚动 ==========
const categoryNavRef = ref(null)
const showLeftArrow = ref(false)
const showRightArrow = ref(false)
const leftDisabled = ref(true)
const rightDisabled = ref(false)

const checkOverflow = () => {
  const el = categoryNavRef.value
  if (!el) return
  const overflow = el.scrollWidth > el.clientWidth
  showLeftArrow.value = overflow
  showRightArrow.value = overflow
  if (overflow) {
    updateArrowState(el)
  }
}

const updateArrowState = (el) => {
  leftDisabled.value = el.scrollLeft <= 0
  rightDisabled.value = el.scrollLeft + el.clientWidth >= el.scrollWidth - 1
}

const scrollBy = (direction) => {
  const el = categoryNavRef.value
  if (!el) return
  const scrollAmount = el.clientWidth * 0.6
  const target = direction === 'left'
    ? el.scrollLeft - scrollAmount
    : el.scrollLeft + scrollAmount
  el.scrollTo({ left: target, behavior: 'smooth' })
}

const onCategoryScroll = () => {
  const el = categoryNavRef.value
  if (!el) return
  updateArrowState(el)
}

// 鼠标拖拽滚动
let isDragging = false
let startX = 0
let scrollStart = 0

const onDragStart = (e) => {
  isDragging = true
  startX = e.clientX
  scrollStart = categoryNavRef.value.scrollLeft
  categoryNavRef.value.style.cursor = 'grabbing'
  categoryNavRef.value.style.userSelect = 'none'
}

const onDragMove = (e) => {
  if (!isDragging) return
  const dx = startX - e.clientX
  categoryNavRef.value.scrollLeft = scrollStart + dx
}

const onDragEnd = () => {
  isDragging = false
  if (categoryNavRef.value) {
    categoryNavRef.value.style.cursor = ''
    categoryNavRef.value.style.userSelect = ''
  }
}

// 全局监听 mouseup（防止拖出容器后松手状态残留）
const onGlobalMouseUp = () => {
  if (isDragging) onDragEnd()
}

// ========== 文章加载 ==========

const loadArticles = async (page = 0, size = 10) => {
  loading.value = true
  error.value = ''
  try {
    const catId = activeCategory.value === 'all' ? null : activeCategory.value
    const kw = searchQuery.value && searchQuery.value.trim() ? searchQuery.value.trim() : null
    const result = await articleApi.list(page, size, catId, kw)
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
    error.value = e.message || '获取文章失败'
  } finally {
    loading.value = false
  }
}

watch(activeCategory, () => {
  currentPage.value = 0
  loadArticles(0, pageSize.value)
})

watch(searchQuery, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 0
    loadArticles(0, pageSize.value)
  }, 300)
})

const getVisiblePages = () => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  if (total <= 7) {
    for (let i = 0; i < total; i++) pages.push(i)
  } else {
    if (current <= 2) {
      for (let i = 0; i <= 4; i++) pages.push(i)
      pages.push('...')
      pages.push(total - 1)
    } else if (current >= total - 3) {
      pages.push(0)
      pages.push('...')
      for (let i = total - 5; i < total; i++) pages.push(i)
    } else {
      pages.push(0)
      pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) pages.push(i)
      pages.push('...')
      pages.push(total - 1)
    }
  }
  return pages
}

const handlePageChange = (page) => {
  const pageNum = Number(page)
  if (isNaN(pageNum) || pageNum < 0) return
  loadArticles(pageNum, pageSize.value)
}

const handleDelete = async (id) => {
  try {
    await articleApi.delete(id)
    if (articles.value.length === 1 && currentPage.value > 0) {
      loadArticles(currentPage.value - 1, pageSize.value)
    } else {
      loadArticles(currentPage.value, pageSize.value)
    }
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

const loadCategories = async () => {
  try {
    categories.value = await categoryApi.list()
    await nextTick()
    checkOverflow()
  } catch (e) {
    console.error('获取分类失败:', e)
  }
}

onMounted(() => {
  loadArticles()
  loadCategories()
  window.addEventListener('mouseup', onGlobalMouseUp)
  window.addEventListener('resize', checkOverflow)
})

onUnmounted(() => {
  window.removeEventListener('mouseup', onGlobalMouseUp)
  window.removeEventListener('resize', checkOverflow)
})
</script>

<template>
  <div class="page">
    <section class="hero">
      <div class="hero-bg"></div>
      <div class="hero-inner">
        <div class="hero-content">
          <div v-if="siteStore.state.loading" class="hero-skeleton">
            <div class="skeleton-line" style="width:40%"></div>
            <div class="skeleton-line" style="width:90%"></div>
            <div class="skeleton-line" style="width:70%"></div>
          </div>
          <template v-else>
            <div class="hero-badge">
              <span class="hero-badge-dot"></span>
              {{ siteStore.state.config.siteDesc }}
            </div>
            <h1 class="hero-title">{{ siteStore.state.config.slogan || '记录思考与灵感的个人空间' }}</h1>
            <p class="hero-desc">整理学习路径、代码示例、项目复盘与面试重点，从基础到工程实践</p>
          </template>
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-number">{{ totalElements }}</span>
              <span class="stat-label">篇文章</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-number">{{ categories.length }}</span>
              <span class="stat-label">个分类</span>
            </div>
          </div>
        </div>
        <div class="hero-visual">
          <div class="hero-illustration">
            <div class="ilogo-card"><span class="ilogo-icon">✦</span></div>
            <div class="ilogo-ring ring-1"></div>
            <div class="ilogo-ring ring-2"></div>
            <div class="ilogo-ring ring-3"></div>
          </div>
        </div>
      </div>
    </section>

    <main class="main">
      <div class="main-inner">
        <div class="content">
          <!-- 分类导航 -->
          <div class="category-scroll-wrapper">
            <button
              v-show="showLeftArrow"
              class="category-arrow category-arrow-left"
              :class="{ disabled: leftDisabled }"
              :disabled="leftDisabled"
              @click="scrollBy('left')"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="15 18 9 12 15 6"></polyline>
              </svg>
            </button>
            <div
              ref="categoryNavRef"
              class="category-nav"
              @scroll="onCategoryScroll"
              @mousedown="onDragStart"
              @mousemove="onDragMove"
              @mouseup="onDragEnd"
              @mouseleave="onDragEnd"
            >
              <button
                class="cat-nav-btn"
                :class="{ active: activeCategory === 'all' }"
                @click="activeCategory = 'all'"
              >全部</button>
              <button
                v-for="cat in categories"
                :key="cat.id"
                class="cat-nav-btn"
                :class="{ active: activeCategory === String(cat.id) }"
                @click="activeCategory = String(cat.id)"
              >{{ cat.name }}</button>
            </div>
            <button
              v-show="showRightArrow"
              class="category-arrow category-arrow-right"
              :class="{ disabled: rightDisabled }"
              :disabled="rightDisabled"
              @click="scrollBy('right')"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </button>
          </div>

          <!-- 搜索 + 标题 -->
          <div class="section-header">
            <h2 class="section-title">
              {{ activeCategory === 'all' ? '全部文章' : categories.find(c => String(c.id) === activeCategory)?.name || '全部文章' }}
            </h2>
            <div class="search-box">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input v-model="searchQuery" type="text" placeholder="搜索文章..." />
            </div>
          </div>

          <!-- Loading 骨架屏 -->
          <div v-if="loading" class="loading-state">
            <div v-for="i in 3" :key="i" class="skeleton-card">
              <div class="skeleton-line w-30"></div>
              <div class="skeleton-line w-80"></div>
              <div class="skeleton-line w-60"></div>
              <div class="skeleton-line w-100"></div>
            </div>
          </div>

          <!-- Error -->
          <div v-else-if="error" class="empty-state">
            <div class="empty-icon error">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
            </div>
            <p class="empty-text">{{ error }}</p>
            <button class="retry-btn" @click="loadArticles()">重新加载</button>
          </div>

          <!-- Empty -->
          <div v-else-if="articles.length === 0" class="empty-state">
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

          <!-- 文章列表 -->
          <div v-else class="article-list">
            <ArticleCard
              v-for="(article, idx) in articles"
              :key="article.id"
              :article="article"
              :categories="categories"
              :index="idx"
              @delete="handleDelete"
            />

            <!-- 分页 -->
            <div v-if="totalPages > 1" class="pagination">
              <div class="pagination-info">共 {{ totalElements }} 篇文章</div>
              <div class="pagination-controls">
                <button
                  class="pagination-btn"
                  :class="{ disabled: currentPage === 0 }"
                  :disabled="currentPage === 0"
                  @click="handlePageChange(currentPage - 1)"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="15 18 9 12 15 6"></polyline>
                  </svg>
                </button>
                <div class="pagination-pages">
                  <template v-for="(page, idx) in getVisiblePages()" :key="`${page}-${idx}`">
                    <span v-if="page === '...'" class="pagination-ellipsis">...</span>
                    <button
                      v-else
                      class="pagination-page"
                      :class="{ active: page === currentPage }"
                      @click="handlePageChange(Number(page))"
                    >{{ page + 1 }}</button>
                  </template>
                </div>
                <button
                  class="pagination-btn"
                  :class="{ disabled: currentPage >= totalPages - 1 }"
                  :disabled="currentPage >= totalPages - 1"
                  @click="handlePageChange(currentPage + 1)"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="9 18 15 12 9 6"></polyline>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.page { min-height: 100vh; display: flex; flex-direction: column; }

.hero {
  position: relative;
  padding: 100px 0 72px;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(170deg, #1a2120 0%, #141616 30%, #181e1c 60%, #141616 100%);
  z-index: 0;
}

.hero-bg::before {
  content: '';
  position: absolute;
  top: -40%;
  right: -15%;
  width: 700px;
  height: 700px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(126,200,160,0.08) 0%, rgba(126,200,160,0.03) 40%, transparent 70%);
}

.hero-bg::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(126,200,160,0.04) 0%, transparent 60%);
}

.hero-inner {
  position: relative;
  z-index: 1;
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 var(--space-6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 60px;
}

.hero-content {
  flex: 1;
  max-width: 600px;
  animation: fadeInUp 0.6s ease-out;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  margin-bottom: 28px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  background: rgba(126,200,160,0.1);
  border-radius: var(--radius-full);
  letter-spacing: 0.5px;
  border: 1px solid rgba(126,200,160,0.18);
}

.hero-badge-dot {
  width: 6px;
  height: 6px;
  background: var(--color-primary);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

.hero-title {
  font-size: 42px;
  font-weight: 800;
  color: var(--color-text-primary);
  line-height: 1.25;
  letter-spacing: -0.8px;
  margin-bottom: 16px;
  font-family: var(--font-serif);
}

.hero-desc {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
  margin-bottom: 36px;
  max-width: 460px;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: -0.5px;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--color-border-light);
}

.hero-skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.skeleton-line {
  height: 16px;
  background: linear-gradient(90deg, #1e2222 25%, #2a3030 50%, #1e2222 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

.w-30 { width: 30%; }
.w-60 { width: 60%; }
.w-80 { width: 80%; }
.w-100 { width: 100%; }

.hero-visual {
  flex-shrink: 0;
  width: 280px;
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.8s ease-out 0.2s both;
}

.hero-illustration {
  position: relative;
  width: 220px;
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: heroFloat 4s ease-in-out infinite;
}

.ilogo-card {
  width: 100px;
  height: 120px;
  background: linear-gradient(135deg, #7ec8a0, #4a9e6e);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
  box-shadow: 0 12px 40px rgba(126,200,160,0.2);
}

.ilogo-icon {
  font-size: 42px;
  color: #141616;
}

.ilogo-ring {
  position: absolute;
  border: 2px solid rgba(126,200,160,0.2);
  border-radius: 50%;
  animation: ringFloat 3s ease-in-out infinite;
}

.ring-1 { width: 180px; height: 180px; animation-delay: 0s; }
.ring-2 { width: 220px; height: 220px; border-color: rgba(126,200,160,0.12); animation-delay: 0.5s; }
.ring-3 { width: 260px; height: 260px; border-color: rgba(126,200,160,0.06); animation-delay: 1s; }

.main { flex: 1; padding: 32px 0 64px; }

.main-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 var(--space-6);
}

.content { min-width: 0; }

/* 分类导航 — 滚动容器 */
.category-scroll-wrapper {
  position: relative;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--color-border-light);
}

.category-nav {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  scroll-behavior: smooth;
}

.category-nav::-webkit-scrollbar { display: none; }

/* 左右箭头 */
.category-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(20, 22, 22, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid var(--color-border-light);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-top: -10px; /* 偏移以补偿 padding-bottom */
}

.category-arrow:hover:not(.disabled) {
  background: rgba(30, 33, 33, 0.95);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.category-arrow.disabled {
  opacity: 0.25;
  cursor: not-allowed;
}

.category-arrow-left {
  left: 0;
}

.category-arrow-right {
  right: 0;
}

.cat-nav-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-muted);
  background: transparent;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);
  white-space: nowrap;
  flex-shrink: 0;
}

.cat-nav-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(126,200,160,0.06);
}

.cat-nav-btn.active {
  color: #141616;
  background: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
  padding: 0 var(--space-1);
}

.section-title {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.3px;
  font-family: var(--font-serif);
  flex-shrink: 0;
}

.search-box {
  position: relative;
  width: 260px;
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
  border-radius: var(--radius-full);
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  outline: none;
  transition: all var(--transition-fast);
}

.search-box input::placeholder { color: var(--color-text-muted); }

.search-box input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(126,200,160,0.1);
}

.loading-state { display: flex; flex-direction: column; gap: 16px; }

.skeleton-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-8);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.empty-icon {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  color: var(--color-text-muted);
  background: var(--color-bg-card);
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
  color: #141616;
  background: var(--color-primary);
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.retry-btn:hover { background: var(--color-primary-hover); }

.article-list { display: flex; flex-direction: column; gap: 16px; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 0 8px;
  border-top: 1px solid var(--color-border-light);
  margin-top: 12px;
}

.pagination-info { font-size: var(--font-size-sm); color: var(--color-text-muted); }

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
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.pagination-btn:hover:not(.disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(126,200,160,0.1);
}

.pagination-btn.disabled { opacity: 0.3; cursor: not-allowed; }

.pagination-pages { display: flex; align-items: center; gap: 4px; }

.pagination-page {
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  padding: 0 8px;
}

.pagination-page:hover { border-color: var(--color-primary); color: var(--color-primary); }

.pagination-page.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #141616;
}

.pagination-ellipsis { font-size: var(--font-size-sm); color: var(--color-text-muted); padding: 0 4px; }

@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes pulse { 0%,100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.5; transform: scale(0.85); } }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }
@keyframes ringFloat { 0%,100% { transform: scale(1); opacity: 0.5; } 50% { transform: scale(1.06); opacity: 0.9; } }
@keyframes heroFloat { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
</style>