<script setup>
import { ref, onMounted } from 'vue'
import { categoryApi, articleApi } from '../api'
import { useRouter } from 'vue-router'

const router = useRouter()

const categories = ref([])
const articles = ref([])
const loading = ref(true)
const error = ref('')
const newName = ref('')
const newDescription = ref('')
const adding = ref(false)
const editingId = ref(null)
const editingName = ref('')
const editingDescription = ref('')

// 文章列表视图相关
const currentView = ref('categories') // 'categories' | 'articles'
const selectedCategory = ref(null)
const categoryArticles = ref([])
const articlesLoading = ref(false)
const articlesError = ref('')
const currentPage = ref(0)
const pageSize = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

const loadData = async () => {
  loading.value = true
  error.value = ''

  try {
    const [cats, artsResult] = await Promise.all([
      categoryApi.list(),
      articleApi.list(0, 100)  // 加载所有文章用于统计分类数量
    ])
    categories.value = cats || []
    // 分页接口返回 { content: [...] }，提取文章数组
    articles.value = artsResult?.content || []
  } catch (e) {
    console.error('[分类管理] 加载数据失败:', e)
    error.value = e.message || '加载失败'
    categories.value = []
    articles.value = []
  } finally {
    loading.value = false
  }
}

const getArticleCount = (categoryId) => {
  return articles.value.filter(a => String(a.categoryId) === String(categoryId)).length
}

const handleAdd = async () => {
  if (!newName.value.trim()) {
    error.value = '请输入分类名称'
    return
  }

  adding.value = true
  error.value = ''

  try {
    await categoryApi.create(newName.value.trim(), newDescription.value.trim())
    newName.value = ''
    newDescription.value = ''
    await loadData()
  } catch (e) {
    error.value = e.message || '添加失败'
  } finally {
    adding.value = false
  }
}

const startEdit = (cat) => {
  editingId.value = cat.id
  editingName.value = cat.name
  editingDescription.value = cat.description || ''
}

const cancelEdit = () => {
  editingId.value = null
  editingName.value = ''
  editingDescription.value = ''
}

const handleUpdate = async (id) => {
  if (!editingName.value.trim()) {
    return
  }

  try {
    await categoryApi.update(id, editingName.value.trim(), editingDescription.value.trim())
    cancelEdit()
    await loadData()
  } catch (e) {
    error.value = e.message || '更新失败'
  }
}

const handleDelete = async (id) => {
  const count = getArticleCount(id)
  if (count > 0) {
    if (!confirm(`该分类下有 ${count} 篇文章，确定要删除吗？`)) return
  } else {
    if (!confirm('确定要删除该分类吗？')) return
  }

  try {
    await categoryApi.delete(id)
    await loadData()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

// ===== 文章列表视图方法 =====

const viewArticles = (cat) => {
  selectedCategory.value = cat
  currentView.value = 'articles'
  currentPage.value = 0
  loadCategoryArticles(0, pageSize.value)
}

const backToCategories = () => {
  currentView.value = 'categories'
  selectedCategory.value = null
  categoryArticles.value = []
  articlesError.value = ''
}

const loadCategoryArticles = async (page = 0, size = 10) => {
  articlesLoading.value = true
  articlesError.value = ''
  try {
    const result = await articleApi.list(page, size, String(selectedCategory.value.id))
    if (result) {
      categoryArticles.value = result.content || []
      totalElements.value = result.totalElements || 0
      totalPages.value = result.totalPages || 0
      currentPage.value = result.number ?? 0
      pageSize.value = result.size || 10
    }
  } catch (e) {
    articlesError.value = e.message || '获取文章失败'
    categoryArticles.value = []
  } finally {
    articlesLoading.value = false
  }
}

const handlePageChange = (page) => {
  const pageNum = Number(page)
  if (isNaN(pageNum) || pageNum < 0) return
  loadCategoryArticles(pageNum, pageSize.value)
}

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
  loadData()
})
</script>

<template>
  <div class="page">
    <main class="main">
      <div class="container">
        <!-- 分类管理视图 -->
        <template v-if="currentView === 'categories'">
        <!-- 页面头部 -->
        <div class="page-header">
          <div>
            <button class="back-btn" @click="router.push('/')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="19" y1="12" x2="5" y2="12"></line>
                <polyline points="12 19 5 12 12 5"></polyline>
              </svg>
              返回首页
            </button>
            <h1 class="page-title">分类管理</h1>
            <p class="page-desc">管理你的文章分类，让内容更有条理</p>
          </div>

          <div class="stats-card">
            <div class="stat">
              <div class="stat-number">{{ categories.length }}</div>
              <div class="stat-label">个分类</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat">
              <div class="stat-number">{{ articles.length }}</div>
              <div class="stat-label">篇文章</div>
            </div>
          </div>
        </div>

        <div v-if="error" class="error-banner">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <span>{{ error }}</span>
        </div>

        <!-- 添加分类 -->
        <div class="add-section">
          <h3 class="section-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="16"></line>
              <line x1="8" y1="12" x2="16" y2="12"></line>
            </svg>
            新建分类
          </h3>
          <div class="add-form">
            <input
              v-model="newName"
              type="text"
              class="add-input"
              placeholder="分类名称"
              @keyup.enter="handleAdd"
            />
            <input
              v-model="newDescription"
              type="text"
              class="add-input add-desc"
              placeholder="分类描述（可选）"
              @keyup.enter="handleAdd"
            />
            <button
              class="btn btn-primary"
              @click="handleAdd"
              :disabled="adding || !newName.trim()"
            >
              <span v-if="adding" class="spinner-sm"></span>
              <span>{{ adding ? '添加中...' : '添加' }}</span>
            </button>
          </div>
        </div>

        <!-- 分类列表 -->
        <div class="list-section">
          <h3 class="section-title">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
            </svg>
            所有分类
            <span class="badge">{{ categories.length }}</span>
          </h3>

          <div v-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>正在加载分类...</p>
          </div>

          <div v-else-if="categories.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
              </svg>
            </div>
            <p class="empty-text">还没有分类</p>
            <p class="empty-desc">创建你的第一个分类来管理文章吧</p>
          </div>

          <div v-else class="category-grid">
            <div
              v-for="cat in categories"
              :key="cat.id"
              class="category-card"
              :class="{ editing: editingId === cat.id }"
            >
              <div v-if="editingId === cat.id" class="edit-form">
                <input
                  v-model="editingName"
                  type="text"
                  class="edit-input"
                  placeholder="分类名称"
                  @keyup.enter="handleUpdate(cat.id)"
                  @keyup.esc="cancelEdit"
                  autofocus
                />
                <input
                  v-model="editingDescription"
                  type="text"
                  class="edit-input edit-desc"
                  placeholder="分类描述"
                  @keyup.enter="handleUpdate(cat.id)"
                  @keyup.esc="cancelEdit"
                />
                <div class="edit-actions">
                  <button class="icon-btn confirm" @click="handleUpdate(cat.id)" :disabled="!editingName.trim()">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="20 6 9 17 4 12"></polyline>
                    </svg>
                  </button>
                  <button class="icon-btn cancel" @click="cancelEdit">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <line x1="18" y1="6" x2="6" y2="18"></line>
                      <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                  </button>
                </div>
              </div>

              <template v-else>
                <div class="cat-info">
                  <div class="cat-icon">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                    </svg>
                  </div>
                  <div class="cat-meta">
                    <h4 class="cat-name">{{ cat.name }}</h4>
                    <p v-if="cat.description" class="cat-desc">{{ cat.description }}</p>
                    <span class="cat-count">{{ getArticleCount(cat.id) }} 篇文章</span>
                  </div>
                </div>

                <div class="cat-actions">
                  <button class="action-btn view" @click="viewArticles(cat)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                      <circle cx="12" cy="12" r="3"></circle>
                    </svg>
                    查看
                  </button>
                  <button class="action-btn edit" @click="startEdit(cat)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M12 20h9"></path>
                      <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
                    </svg>
                    编辑
                  </button>
                  <button class="action-btn delete" @click="handleDelete(cat.id)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="3 6 5 6 21 6"></polyline>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                    删除
                  </button>
                </div>
              </template>
            </div>
          </div>
        </div>
        </template>

        <!-- 文章列表视图 -->
        <template v-else>
          <div class="page-header">
            <div>
              <button class="back-btn" @click="backToCategories">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="19" y1="12" x2="5" y2="12"></line>
                  <polyline points="12 19 5 12 12 5"></polyline>
                </svg>
                返回分类列表
              </button>
              <h1 class="page-title">{{ selectedCategory?.name || '分类文章' }}</h1>
              <p v-if="selectedCategory?.description" class="page-desc">{{ selectedCategory.description }}</p>
            </div>
            <div class="stats-card">
              <div class="stat">
                <div class="stat-number">{{ totalElements }}</div>
                <div class="stat-label">篇文章</div>
              </div>
            </div>
          </div>

          <div v-if="articlesError" class="error-banner">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <span>{{ articlesError }}</span>
          </div>

          <div class="list-section">
            <div v-if="articlesLoading" class="loading-state">
              <div class="spinner"></div>
              <p>正在加载文章...</p>
            </div>

            <div v-else-if="categoryArticles.length === 0" class="empty-state">
              <div class="empty-icon">
                <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                </svg>
              </div>
              <p class="empty-text">该分类下暂无文章</p>
            </div>

            <div v-else class="article-list">
              <article
                v-for="article in categoryArticles"
                :key="article.id"
                class="article-card"
                @click="goToArticle(article.id)"
              >
                <div class="card-header">
                  <span class="date">{{ formatTime(article.createTime) }}</span>
                </div>
                <h3 class="article-title">{{ article.title }}</h3>
                <div v-if="article.summary" class="ai-summary">
                  <div class="ai-summary-bar"></div>
                  <span class="ai-summary-text">{{ article.summary }}</span>
                </div>
              </article>

              <!-- 分页组件 -->
              <div v-if="totalPages > 1" class="pagination">
                <div class="pagination-info">共 {{ totalElements }} 篇文章</div>
                <div class="pagination-controls">
                  <button
                    class="pagination-btn"
                    :class="{ disabled: currentPage === 0 }"
                    @click="handlePageChange(currentPage - 1)"
                    :disabled="currentPage === 0"
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
                    @click="handlePageChange(currentPage + 1)"
                    :disabled="currentPage >= totalPages - 1"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="9 18 15 12 9 6"></polyline>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </template>
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

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 var(--space-6);
  animation: fadeInUp 0.4s ease-out;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-8);
  margin-bottom: var(--space-10);
  padding-bottom: var(--space-6);
  border-bottom: 1px solid var(--color-border-light);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: var(--space-3);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  padding: 6px 12px;
  margin-left: -12px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.back-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.page-title {
  font-size: var(--font-size-4xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  letter-spacing: -0.8px;
  margin-bottom: var(--space-2);
}

.page-desc {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

.stats-card {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  padding: var(--space-4) var(--space-6);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  flex-shrink: 0;
}

.stat {
  text-align: center;
}

.stat-number {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
  letter-spacing: -0.3px;
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-top: 2px;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--color-border);
}

.error-banner {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4);
  margin-bottom: var(--space-6);
  background: var(--color-error-light);
  color: var(--color-error);
  font-size: var(--font-size-sm);
  border-radius: var(--radius-md);
}

/* 添加分类 */
.add-section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  margin-bottom: var(--space-6);
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-4);
}

.section-title svg {
  color: var(--color-primary);
}

.badge {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  padding: 2px 8px;
  border-radius: var(--radius-full);
  margin-left: var(--space-1);
}

.add-form {
  display: flex;
  gap: var(--space-3);
}

.add-input {
  flex: 1;
  padding: 12px 16px;
  font-size: var(--font-size-base);
}

.add-desc {
  flex: 1.5;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 44px;
  padding: 0 24px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.btn-primary {
  color: #ffffff;
  background: var(--color-primary);
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.spinner-sm {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 分类列表 */
.list-section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
}

/* 加载/空状态 */
.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) 0;
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
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.empty-icon {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-4);
  color: var(--color-text-muted);
  background: var(--color-bg-hover);
  border-radius: var(--radius-full);
}

.empty-text {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-1);
}

.empty-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

/* 分类网格 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-4);
}

.category-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
  gap: var(--space-3);
}

.category-card:hover {
  border-color: var(--color-border);
  box-shadow: var(--shadow-md);
}

.category-card.editing {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.cat-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
  flex: 1;
}

.cat-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.cat-meta {
  min-width: 0;
  flex: 1;
}

.cat-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cat-desc {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cat-count {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.cat-actions {
  display: flex;
  gap: var(--space-2);
  flex-shrink: 0;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.action-btn.edit {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.action-btn.edit:hover {
  background: var(--color-primary-lighter);
}

.action-btn.view {
  color: var(--color-text-secondary);
  background: var(--color-bg-hover);
}

.action-btn.view:hover {
  background: var(--color-border);
  color: var(--color-text-primary);
}

.action-btn.delete {
  color: var(--color-error);
  background: var(--color-error-light);
}

.action-btn.delete:hover {
  background: #fee2e2;
}

/* 编辑模式 */
.edit-form {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
}

.edit-input {
  flex: 1;
  padding: 8px 12px;
  font-size: var(--font-size-sm);
}

.edit-desc {
  flex: 1.2;
}

.edit-actions {
  display: flex;
  gap: var(--space-1);
}

.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.icon-btn.confirm {
  color: var(--color-success);
  background: #ecfdf5;
}

.icon-btn.confirm:hover:not(:disabled) {
  background: #d1fae5;
}

.icon-btn.confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.icon-btn.cancel {
  color: var(--color-text-secondary);
  background: var(--color-bg-hover);
}

.icon-btn.cancel:hover {
  background: var(--color-border);
  color: var(--color-text-primary);
}

/* 文章列表 */
.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-card {
  padding: 20px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.article-card:hover {
  border-color: var(--color-border);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.date {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.article-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  line-height: 1.4;
  margin-bottom: 10px;
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

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 0 8px;
  border-top: 1px solid var(--color-border-light);
  margin-top: 12px;
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
