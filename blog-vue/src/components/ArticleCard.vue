<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const props = defineProps({
  article: {
    type: Object,
    required: true
  },
  categories: {
    type: Array,
    default: () => []
  },
  index: {
    type: Number,
    default: 0
  }
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
      if (match) {
        return `${match[1]}年${String(match[2]).padStart(2, '0')}月${String(match[3]).padStart(2, '0')}日`
      }
      return time
    }
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}年${month}月${day}日`
  } catch (e) {
    console.error('[ArticleCard] 日期格式化失败:', time, e)
    return time
  }
}

const isOwner = computed(() => {
  return userStore.state.isLoggedIn && String(userStore.state.user.id) === String(props.article.userId)
})

const excerpt = computed(() => {
  if (!props.article.content) return ''
  const text = props.article.content.replace(/\s+/g, ' ').trim()
  return text.length > 120 ? text.slice(0, 120) + '...' : text
})

const goToDetail = () => {
  router.push(`/article/${props.article.id}`)
}

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
      <div class="card-header">
        <span class="category-tag">{{ categoryName }}</span>
        <span class="date">{{ formatTime(article.createTime) }}</span>
      </div>

      <h3 class="title">{{ article.title }}</h3>

      <p class="excerpt">{{ excerpt }}</p>

      <div class="card-footer">
        <div class="author-info">
          <div class="author-avatar">
            {{ article.userId }}
          </div>
          <span class="author-id">作者ID: {{ article.userId }}</span>
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

        <div class="read-more" v-else>
          阅读全文
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="5" y1="12" x2="19" y2="12"></line>
            <polyline points="12 5 19 12 12 19"></polyline>
          </svg>
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
  opacity: 1;
  animation: fadeInUp 0.3s ease-out;
  overflow: hidden;
}

.article-card:hover {
  border-color: var(--color-border);
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}

.card-content {
  padding: var(--space-8);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.category-tag {
  display: inline-block;
  padding: 4px 12px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-radius: var(--radius-full);
  letter-spacing: 0.3px;
}

.date {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

.title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  line-height: 1.4;
  letter-spacing: -0.3px;
  transition: color var(--transition-fast);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-card:hover .title {
  color: var(--color-primary);
}

.excerpt {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--space-4);
  margin-top: auto;
  border-top: 1px solid var(--color-border-light);
}

.author-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.author-avatar {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, #64748b, #94a3b8);
  color: #ffffff;
  font-size: 11px;
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
}

.author-id {
  font-size: var(--font-size-sm);
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
  padding: 6px 12px;
  font-size: var(--font-size-sm);
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

.action-btn.delete {
  color: var(--color-error);
  background: var(--color-error-light);
}

.action-btn.delete:hover {
  background: #fee2e2;
}

.read-more {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  transition: gap var(--transition-fast);
}

.article-card:hover .read-more {
  gap: 8px;
}

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
</style>
