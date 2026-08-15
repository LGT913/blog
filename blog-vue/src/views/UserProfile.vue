<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { articleApi } from '../api'

const router = useRouter()
const userStore = useUserStore()

const articles = ref([])
const loading = ref(true)
const error = ref('')

const fetchUserArticles = async () => {
  const user = userStore.state.user
  if (!user) return
  loading.value = true
  error.value = ''
  try {
    const data = await articleApi.getUserArticles(user.id)
    articles.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message || '获取文章列表失败'
    articles.value = []
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id) => {
  if (!confirm('确定要删除这篇文章吗？')) return
  try {
    await articleApi.delete(id)
    articles.value = articles.value.filter(a => String(a.id) !== String(id))
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

const handleEdit = (article) => {
  router.push(`/create?id=${article.id}`)
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

onMounted(() => {
  if (!userStore.state.isLoggedIn) {
    router.push('/')
    return
  }
  fetchUserArticles()
})
</script>

<template>
  <div class="page">
    <main class="main">
      <div class="container">
        <div class="page-header">
          <button class="back-btn" @click="router.push('/')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="19" y1="12" x2="5" y2="12"></line>
              <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
            返回首页
          </button>
          <h1 class="page-title">个人中心</h1>
        </div>

        <div v-if="userStore.state.user" class="profile-card">
          <div class="avatar">
            {{ userStore.state.user.nickname?.charAt(0).toUpperCase() || 'U' }}
          </div>
          <div class="info">
            <h2 class="nickname">{{ userStore.state.user.nickname }}</h2>
            <p class="username">@{{ userStore.state.user.username }}</p>
            <p class="role">
              <span v-if="userStore.isAdmin.value" class="badge-admin">管理员</span>
              <span v-else class="badge-user">普通用户</span>
            </p>
            <p class="join-time">
              加入于 {{ userStore.state.user.createTime ? formatTime(userStore.state.user.createTime) : '--' }}
            </p>
          </div>
        </div>

        <div class="section">
          <div class="section-header">
            <h3 class="section-title">我的文章</h3>
            <span class="count-badge">{{ articles.length }} 篇</span>
          </div>

          <div v-if="loading" class="loading-state">
            <div v-for="i in 3" :key="i" class="skeleton-item">
              <div class="skeleton-line w-60"></div>
              <div class="skeleton-line w-30"></div>
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
            <button class="retry-btn" @click="fetchUserArticles">重新加载</button>
          </div>

          <div v-else-if="articles.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
              </svg>
            </div>
            <p class="empty-text">还没有发布过文章</p>
            <p class="empty-desc">去写你的第一篇文章吧</p>
            <button class="retry-btn" @click="router.push('/create')">写文章</button>
          </div>

          <div v-else class="article-list">
            <div
              v-for="article in articles"
              :key="article.id"
              class="article-item"
              @click="router.push(`/article/${article.id}`)"
            >
              <div class="article-main">
                <h4 class="article-title">{{ article.title }}</h4>
                <span class="article-date">{{ formatTime(article.createTime) }}</span>
              </div>
              <div class="article-actions">
                <button class="action-btn edit" @click.stop="handleEdit(article)">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 20h9"></path>
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
                  </svg>
                  编辑
                </button>
                <button class="action-btn delete" @click.stop="handleDelete(article.id)">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                  删除
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
.main { flex: 1; padding: 40px 0 64px; }

.container {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 var(--space-6);
  animation: fadeInUp 0.4s ease-out;
}

.page-header {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  margin-bottom: var(--space-8);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.back-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: 700;
  color: var(--color-text-primary);
  font-family: var(--font-serif);
  letter-spacing: -0.5px;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  padding: var(--space-8);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-8);
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), #5a9e7a);
  color: #ffffff;
  font-size: var(--font-size-2xl);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info { flex: 1; }

.nickname {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--color-text-primary);
  font-family: var(--font-serif);
}

.username {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: var(--space-1);
}

.role { margin-top: var(--space-2); }

.badge-admin {
  display: inline-block;
  padding: 2px 10px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
  background: rgba(126, 200, 160, 0.15);
  border-radius: var(--radius-sm);
}

.badge-user {
  display: inline-block;
  padding: 2px 10px;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  background: var(--color-bg-hover, #2a2b30);
  border-radius: var(--radius-sm);
}

.join-time {
  margin-top: var(--space-2);
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border-light);
}

.section-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-family: var(--font-serif);
}

.count-badge {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-muted);
  background: var(--color-bg-hover, #2a2b30);
  padding: 4px 10px;
  border-radius: var(--radius-full);
}

.loading-state { display: flex; flex-direction: column; gap: 12px; }

.skeleton-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
}

.skeleton-line {
  height: 14px;
  background: linear-gradient(90deg, #2a2b30 25%, #34353b 50%, #2a2b30 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

.w-30 { width: 30%; }
.w-60 { width: 60%; }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.empty-icon {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  color: var(--color-text-muted);
  background: #2a2b30;
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
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.retry-btn:hover { background: var(--color-primary-hover); }

.article-list { display: flex; flex-direction: column; gap: 8px; }

.article-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  gap: var(--space-4);
}

.article-item:hover {
  border-color: var(--color-border);
  background: #1e1f22;
}

.article-main {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  min-width: 0;
  flex: 1;
}

.article-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.article-item:hover .article-title {
  color: var(--color-primary);
}

.article-date {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}

.article-actions {
  display: flex;
  align-items: center;
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

.action-btn.delete {
  color: var(--color-error);
  background: var(--color-error-light);
}

.action-btn.delete:hover {
  background: #fee2e2;
}

@keyframes fadeInUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
@keyframes shimmer { 0% { background-position: -200% 0; } 100% { background-position: 200% 0; } }
</style>