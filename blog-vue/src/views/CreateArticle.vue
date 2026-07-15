<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, categoryApi } from '../api'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isEdit = computed(() => !!route.query.id)
const title = ref('')
const content = ref('')
const categoryId = ref('')
const categories = ref([])
const loading = ref(false)
const initialLoading = ref(true)
const error = ref('')

const wordCount = computed(() => content.value.length)

const loadCategories = async () => {
  try {
    categories.value = await categoryApi.list()
  } catch (e) {
    console.error('获取分类失败:', e)
  }
}

const loadArticle = async () => {
  initialLoading.value = true
  try {
    const article = await articleApi.get(route.query.id)
    title.value = article.title
    content.value = article.content
    categoryId.value = String(article.categoryId)
  } catch (e) {
    error.value = e.message || '获取文章失败'
  } finally {
    initialLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!title.value.trim()) {
    error.value = '请输入文章标题'
    return
  }
  if (!content.value.trim()) {
    error.value = '请输入文章内容'
    return
  }
  if (!categoryId.value) {
    error.value = '请选择文章分类'
    return
  }

  loading.value = true
  error.value = ''

  try {
    if (isEdit.value) {
      await articleApi.update(route.query.id, title.value, content.value, categoryId.value)
    } else {
      await articleApi.create({
        title: title.value,
        content: content.value,
        userId: userStore.state.user.id,
        categoryId: categoryId.value
      })
    }
    router.push('/')
  } catch (e) {
    error.value = e.message || (isEdit.value ? '更新失败' : '发布失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  if (isEdit.value) {
    loadArticle()
  } else {
    initialLoading.value = false
    if (categories.value.length > 0) {
      categoryId.value = String(categories.value[0].id)
    }
  }
})
</script>

<template>
  <div class="page">
    <main class="main">
      <div class="editor-container">
        <!-- 页面头部 -->
        <div class="page-header">
          <div class="header-left">
            <button class="back-btn" @click="router.push('/')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="19" y1="12" x2="5" y2="12"></line>
                <polyline points="12 19 5 12 12 5"></polyline>
              </svg>
              返回
            </button>
            <div class="page-title-group">
              <h1 class="page-title">{{ isEdit ? '编辑文章' : '写新文章' }}</h1>
              <p class="page-desc">
                {{ isEdit ? '修改你的文章内容，记录更新的思考' : '分享你的想法，记录这段时光' }}
              </p>
            </div>
          </div>

          <div class="word-count">
            <span class="count-number">{{ wordCount }}</span>
            <span class="count-label">字</span>
          </div>
        </div>

        <div v-if="initialLoading" class="loading-state">
          <div class="spinner"></div>
          <p>正在加载...</p>
        </div>

        <template v-else>
          <div v-if="error" class="error-banner">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
            <span>{{ error }}</span>
          </div>

          <form class="editor-form" @submit.prevent="handleSubmit">
            <!-- 标题输入 -->
            <div class="form-section">
              <label class="form-label">
                文章标题
                <span class="required">*</span>
              </label>
              <input
                v-model="title"
                type="text"
                class="title-input"
                placeholder="请输入文章标题..."
                maxlength="100"
              />
              <div class="input-hint">{{ title.length }}/100</div>
            </div>

            <!-- 分类选择 -->
            <div class="form-section">
              <label class="form-label">
                文章分类
                <span class="required">*</span>
              </label>
              <div class="category-select-wrapper">
                <svg class="select-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                </svg>
                <select v-model="categoryId" class="category-select">
                  <option value="" disabled>请选择分类</option>
                  <option
                    v-for="cat in categories"
                    :key="cat.id"
                    :value="String(cat.id)"
                  >
                    {{ cat.name }}
                  </option>
                </select>
              </div>
            </div>

            <!-- 内容输入 -->
            <div class="form-section content-section">
              <label class="form-label">
                文章内容
                <span class="required">*</span>
              </label>
              <textarea
                v-model="content"
                class="content-input"
                placeholder="在这里写下你的文章内容..."
                rows="16"
              ></textarea>
            </div>

            <!-- 操作按钮 -->
            <div class="form-actions">
              <button
                type="button"
                class="btn btn-ghost"
                @click="router.push('/')"
                :disabled="loading"
              >
                取消
              </button>
              <button
                type="submit"
                class="btn btn-primary"
                :disabled="loading"
              >
                <span v-if="loading" class="spinner-sm"></span>
                <span>{{ loading ? (isEdit ? '保存中...' : '发布中...') : (isEdit ? '保存修改' : '发布文章') }}</span>
              </button>
            </div>
          </form>
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

.editor-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 var(--space-6);
  animation: fadeInUp 0.4s ease-out;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
  margin-bottom: var(--space-8);
  padding-bottom: var(--space-6);
  border-bottom: 1px solid var(--color-border-light);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-5);
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.back-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.page-title-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  letter-spacing: -0.5px;
}

.page-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.word-count {
  display: flex;
  align-items: baseline;
  gap: var(--space-1);
  padding: var(--space-3) var(--space-5);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  flex-shrink: 0;
}

.count-number {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}

.count-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

/* 加载状态 */
.loading-state {
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

/* 表单样式 */
.editor-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.form-label {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  margin-left: var(--space-1);
}

.required {
  color: var(--color-error);
}

.title-input {
  width: 100%;
  padding: 16px 20px;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  line-height: 1.4;
  border-radius: var(--radius-lg);
}

.title-input::placeholder {
  font-weight: var(--font-weight-normal);
}

.input-hint {
  align-self: flex-end;
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

/* 分类选择 */
.category-select-wrapper {
  position: relative;
}

.select-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
  pointer-events: none;
}

.category-select {
  width: 100%;
  padding: 12px 16px 12px 44px;
  font-size: var(--font-size-base);
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 16px center;
  padding-right: 44px;
}

.category-select:hover {
  border-color: var(--color-text-muted);
}

/* 内容输入 */
.content-section {
  flex: 1;
}

.content-input {
  width: 100%;
  min-height: 400px;
  padding: 20px;
  font-size: var(--font-size-base);
  line-height: var(--line-height-relaxed);
  border-radius: var(--radius-lg);
  font-family: inherit;
}

/* 操作按钮 */
.form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border-light);
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 44px;
  padding: 0 28px;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.btn-ghost {
  color: var(--color-text-secondary);
}

.btn-ghost:hover:not(:disabled) {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
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
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
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
