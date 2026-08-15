<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const props = defineProps({
  article: { type: Object, required: true },
  categories: { type: Array, default: () => [] },
  index: { type: Number, default: 0 }
})

const emit = defineEmits(['delete'])

const router = useRouter()
const userStore = useUserStore()

const categoryName = computed(() => {
  const cat = props.categories.find(c => String(c.id) === String(props.article.categoryId))
  return cat ? cat.name : '未分类'
})

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

const isOwner = computed(() =>
  userStore.state.isLoggedIn && String(userStore.state.user.id) === String(props.article.userId)
)

const goToDetail = () => router.push(`/article/${props.article.id}`)

const handleDelete = (e) => {
  e.stopPropagation()
  if (confirm('确定要删除这篇文章吗？')) {
    emit('delete', props.article.id)
  }
}

const handleEdit = (e) => {
  e.stopPropagation()
  router.push(`/create?id=${props.article.id}`)
}
</script>

<template>
  <article
    class="article-card"
    :style="{ animationDelay: `${index * 60}ms` }"
    @click="goToDetail"
  >
    <div class="card-content">
      <!-- 第一行：分类标签 + 标题 + 日期 -->
      <div class="card-title-row">
        <div class="title-group">
          <h3 class="title">{{ article.title }}</h3>
          <span class="category-tag">{{ categoryName }}</span>
        </div>
        <span class="date">{{ formatTime(article.createTime) }}</span>
      </div>

      <!-- 第二块：摘要 -->
      <div class="card-summary">
        <template v-if="article.summary">
          <div class="summary-bar"></div>
          <p class="summary-text">{{ article.summary }}</p>
        </template>
        <p v-else class="summary-placeholder">这篇文章还没有摘要~</p>
      </div>

      <!-- 底部：作者 + 统计 + 操作 -->
      <div class="card-footer">
        <div class="author-info">
          <div class="author-avatar">
            {{ (article.authorName || '用户').charAt(0).toUpperCase() }}
          </div>
          <span class="author-id">{{ article.authorName || '用户' }}</span>
        </div>

        <div class="footer-right">
          <div class="card-stats">
            <span v-if="article.viewCount !== undefined" class="stat">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              {{ article.viewCount }}
            </span>
            <span v-if="article.likeCount !== undefined" class="stat">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z"></path>
              </svg>
              {{ article.likeCount }}
            </span>
          </div>

          <div class="actions" v-if="isOwner">
            <button class="action-btn edit" @click="handleEdit">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 20h9"></path>
                <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
              </svg>
              编辑
            </button>
            <button class="action-btn delete" @click="handleDelete">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
              </svg>
              删除
            </button>
          </div>

          <div class="read-more" v-else @click.stop="goToDetail">
            阅读全文
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="5" y1="12" x2="19" y2="12"></line>
              <polyline points="12 5 19 12 12 19"></polyline>
            </svg>
          </div>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.article-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  animation: fadeInUp 0.35s ease-out;
  overflow: hidden;
}

.article-card:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: 0 4px 24px rgba(126, 200, 160, 0.12);
}

.card-content {
  padding: var(--space-6) var(--space-8);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

/* ========== 第一行：分类标签 + 标题 + 日期 ========== */
.card-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.category-tag {
  display: inline-block;
  flex-shrink: 0;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-radius: var(--radius-full);
  letter-spacing: 0.3px;
  line-height: 1.6;
}

.title-group {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.title {
  min-width: 0;
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  line-height: 1.4;
  letter-spacing: -0.3px;
  transition: color var(--transition-fast);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.article-card:hover .title {
  color: var(--color-primary);
}

.date {
  flex-shrink: 0;
  margin-left: auto;
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

/* ========== 第二块：摘要 ========== */
.card-summary {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.summary-bar {
  flex-shrink: 0;
  width: 3px;
  height: 32px;
  background: var(--color-primary);
  border-radius: 2px;
  opacity: 0.5;
}

.summary-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.summary-placeholder {
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-muted);
  opacity: 0.6;
  font-style: italic;
}

/* ========== 底部：作者 + 统计 + 操作 ========== */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--space-3);
  margin-top: auto;
  border-top: 1px solid var(--color-border-light);
}

.author-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.author-avatar {
  width: 26px;
  height: 26px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, #7ec8a0, #4a9e6e);
  color: #141616;
  font-size: 11px;
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
}

.author-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.footer-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.card-stats {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
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
  background: rgba(126, 200, 160, 0.2);
}

.action-btn.delete {
  color: var(--color-error);
  background: var(--color-error-light);
}

.action-btn.delete:hover {
  background: rgba(224, 108, 117, 0.2);
}

.read-more {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  transition: all var(--transition-fast);
}

.article-card:hover .read-more {
  gap: 8px;
  color: var(--color-primary-hover);
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>