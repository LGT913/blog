<script setup>
import { ref, onMounted, watch } from 'vue'
import { articleApi, categoryApi } from '../api'

const articles = ref([])
const categories = ref([])
const loading = ref(true)
const error = ref('')
const activeTab = ref('views')

const getCategoryName = (categoryId) => {
  const cat = categories.value.find(c => String(c.id) === String(categoryId))
  return cat ? cat.name : '未分类'
}

const formatTime = (time) => {
  if (!time) return ''
  try {
    const date = new Date(time)
    if (isNaN(date.getTime())) {
      const match = time.match(/(\d{4})[-/](\d{1,2})[-/](\d{1,2})/)
      if (match) return `${match[1]}年${String(match[2]).padStart(2, '0')}月${String(match[3]).padStart(2, '0')}日`
      return time
    }
    const y = date.getFullYear()
    const M = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}年${M}月${d}日`
  } catch (e) {
    return time
  }
}

const getExcerpt = (article) => {
  if (article.summary) return article.summary
  if (!article.content) return ''
  const text = article.content.replace(/\s+/g, ' ').trim()
  return text.length > 80 ? text.slice(0, 80) + '...' : text
}

const fetchRanking = async () => {
  loading.value = true
  error.value = ''
  try {
    const data = activeTab.value === 'views'
      ? await articleApi.rankingByViews()
      : await articleApi.rankingByLatest()
    articles.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message || '获取排行榜失败'
    articles.value = []
  } finally {
    loading.value = false
  }
}

const switchTab = (tab) => {
  if (tab === activeTab.value) return
  activeTab.value = tab
}

const loadCategories = async () => {
  try {
    categories.value = await categoryApi.list()
  } catch (e) {
    console.error('获取分类失败:', e)
  }
}

onMounted(() => {
  loadCategories()
  fetchRanking()
})

watch(activeTab, () => fetchRanking())
</script>

<template>
  <div class="page">
    <main class="main">
      <div class="container">
        <div class="page-header">
          <h1 class="page-title">排行榜</h1>
          <p class="page-desc">🔥 根据文章阅读量自动排序，展示热门好文</p>
        </div>

        <div class="tabs">
          <button
            class="tab"
            :class="{ 'tab-active': activeTab === 'views' }"
            @click="switchTab('views')"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
            </svg>
            阅读排行
          </button>
          <button
            class="tab"
            :class="{ 'tab-active': activeTab === 'latest' }"
            @click="switchTab('latest')"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
            最新发布
          </button>
        </div>

        <div v-if="loading" class="loading-state">
          <div v-for="i in 3" :key="i" class="skeleton-card">
            <div class="skeleton-line w-30"></div>
            <div class="skeleton-line w-80"></div>
            <div class="skeleton-line w-60"></div>
            <div class="skeleton-line w-100"></div>
          </div>
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
          <button class="retry-btn" @click="fetchRanking">重新加载</button>
        </div>

        <div v-else-if="articles.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
            </svg>
          </div>
          <p class="empty-text">暂无数据</p>
          <p class="empty-desc">还没有文章被发布</p>
        </div>

        <div v-else class="article-list">
          <div
            v-for="(article, idx) in articles"
            :key="article.id"
            class="rank-card"
            :class="{ 'top-rank': idx < 3 }"
            :style="{ animationDelay: `${idx * 60}ms` }"
            @click="$router.push(`/article/${article.id}`)"
          >
            <div class="rank-card-inner">
              <!-- 排名角标 -->
              <div class="rank-badge" :class="{ top: idx < 3 }">
                <span v-if="idx === 0" class="rank-icon">🥇</span>
                <span v-else-if="idx === 1" class="rank-icon">🥈</span>
                <span v-else-if="idx === 2" class="rank-icon">🥉</span>
                <span class="rank-number">{{ idx + 1 }}</span>
              </div>

              <!-- 内容 -->
              <div class="rank-content">
                <div class="rank-header">
                  <span class="rank-category">{{ getCategoryName(article.categoryId) }}</span>
                  <span class="rank-date">{{ formatTime(article.createTime) }}</span>
                </div>
                <h3 class="rank-title">{{ article.title }}</h3>
                <p v-if="article.summary || article.content" class="rank-excerpt">
                  {{ getExcerpt(article) }}
                </p>
                <div class="rank-footer">
                  <div class="rank-author">
                    <div class="rank-author-avatar">{{ (article.authorName || '?').charAt(0).toUpperCase() }}</div>
                    <span class="rank-author-name">{{ article.authorName || '用户' }}</span>
                  </div>
                  <div class="rank-stats">
                    <!-- 阅读排行模式：高亮阅读量 -->
                    <span v-if="activeTab === 'views'" class="rank-stat highlight">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                      </svg>
                      {{ article.viewCount || 0 }} 阅读
                    </span>
                    <!-- 最新发布模式：高亮时间 -->
                    <span v-if="activeTab === 'latest'" class="rank-stat highlight">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <polyline points="12 6 12 12 16 14"></polyline>
                      </svg>
                      {{ formatTime(article.createTime) }}
                    </span>
                    <span class="rank-stat">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z"></path>
                      </svg>
                      {{ article.likeCount || 0 }}
                    </span>
                  </div>
                </div>
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
.main { flex: 1; padding: 24px 0 64px; }

.container {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 var(--space-6);
  animation: fadeInUp 0.4s ease-out;
}

.page-header {
  text-align: center;
  margin-bottom: 8px;
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-text-primary);
  font-family: var(--font-serif);
  letter-spacing: -0.5px;
}

.page-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.tabs {
  display: flex;
  justify-content: center;
  gap: var(--space-3);
  margin-bottom: var(--space-8);
  margin-top: var(--space-6);
}

.tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-muted);
  background: transparent;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);
}

.tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(126, 200, 160, 0.06);
}

.tab-active {
  color: #141616;
  background: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}

.tab-active:hover {
  color: #141616;
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
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

/* 排名卡片容器 */
.rank-card {
  position: relative;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  overflow: hidden;
  animation: fadeInUp 0.35s ease-out;
}

.rank-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-primary);
  box-shadow: 0 4px 24px rgba(126, 200, 160, 0.12);
}

.rank-card.top-rank {
  border-color: rgba(126, 200, 160, 0.25);
  background: rgba(126, 200, 160, 0.03);
}

.rank-card-inner {
  display: flex;
  align-items: stretch;
  padding: var(--space-6);
  gap: var(--space-5);
}

/* 排名角标 */
.rank-badge {
  flex-shrink: 0;
  width: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-number {
  font-size: 22px;
  font-weight: 700;
  font-family: var(--font-mono);
  color: var(--color-text-muted);
  letter-spacing: -0.5px;
}

.rank-badge.top .rank-number {
  color: var(--color-primary);
  font-size: 26px;
  text-shadow: 0 0 12px rgba(126, 200, 160, 0.4);
}

.rank-icon {
  margin-right: 2px;
  font-size: 18px;
}

/* 卡片内容 */
.rank-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.rank-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.rank-category {
  display: inline-block;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-radius: var(--radius-full);
  flex-shrink: 0;
}

.rank-date {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

.rank-title {
  font-family: var(--font-serif);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color var(--transition-fast);
}

.rank-card:hover .rank-title {
  color: var(--color-primary);
}

.rank-excerpt {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rank-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--space-2);
  margin-top: auto;
}

.rank-author {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.rank-author-avatar {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, #7ec8a0, #4a9e6e);
  color: #141616;
  font-size: 10px;
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-author-name {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.rank-stats {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.rank-stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.rank-stat.highlight {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
}

.rank-stat.highlight svg {
  color: var(--color-primary);
}

@keyframes fadeInUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }
</style>