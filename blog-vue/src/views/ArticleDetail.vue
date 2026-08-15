<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, categoryApi, commentApi, likeApi } from '../api'
import { useUserStore } from '../store/user'
import { useCommentWebSocket } from '../composables/useCommentWebSocket'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const article = ref(null)
const categories = ref([])
const comments = ref([])
const loading = ref(true)
const error = ref('')

const commentsLoading = ref(false)
const commentsError = ref('')

const commentContent = ref('')
const commentSubmitting = ref(false)

// 回复状态
const replyingTo = ref(null)       // 正在回复的评论 ID（null 表示顶层评论）
const replyContent = ref('')
const replySubmitting = ref(false)
const expandedReplies = ref(new Set())  // 展开子回复的评论 ID 集合

const liked = ref(false)
const likeLoading = ref(false)

const newCommentNotification = ref(null)
const showCommentToast = ref(false)

const { newComment, connect: connectWS, disconnect: disconnectWS } = useCommentWebSocket(route.params.id)

watch(newComment, (comment) => {
  if (!comment) return
  // Toast 提示（兼容 CommentNotifyMessage.commentContent 与 Comment.content）
  newCommentNotification.value = comment
  showCommentToast.value = true
  setTimeout(() => { showCommentToast.value = false }, 5000)
  // 只有真正的 Comment 结构（含 id + content）才加入评论列表，并按 id 去重。
  // 后端 WS 广播的是 CommentNotifyMessage（轻量通知，无 id/content），只提示不加入列表，
  // 避免渲染出空的假评论（真实评论由 handleSubmitComment unshift 的返回值保证）。
  if (comment.id != null && comment.content) {
    const exists = comments.value.some(c => String(c.id) === String(comment.id))
    if (!exists) {
      comments.value.unshift(comment)
    }
  }
})

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return time
  const y = date.getFullYear()
  const M = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  return `${y}年${M}月${d}日 ${h}:${m}`
}

const categoryName = computed(() => {
  if (!article.value) return ''
  const cat = categories.value.find(c => String(c.id) === String(article.value.categoryId))
  return cat ? cat.name : '未分类'
})

const wordCount = computed(() => article.value?.content?.length || 0)

const readTime = computed(() => Math.max(1, Math.ceil(wordCount.value / 300)))

const isOwner = computed(() =>
  article.value && userStore.state.isLoggedIn && String(userStore.state.user.id) === String(article.value.userId)
)

const isAdmin = computed(() => userStore.isAdmin.value)

const canEdit = computed(() => isOwner.value || isAdmin.value)

// 顶层评论（parentId 为 null）
const topLevelComments = computed(() =>
  comments.value.filter(c => c.parentId == null)
)

// 获取某条评论的所有子回复
const getReplies = (parentId) =>
  comments.value.filter(c => c.parentId === parentId)

const loadArticle = async () => {
  loading.value = true
  error.value = ''
  try {
    article.value = await articleApi.get(route.params.id)
    // 文章加载完成后，登录用户加载点赞状态
    if (userStore.state.isLoggedIn) {
      loadLikeStatus()
    }
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

const loadComments = async () => {
  commentsLoading.value = true
  commentsError.value = ''
  try {
    comments.value = await commentApi.getByArticle(route.params.id)
  } catch (e) {
    commentsError.value = e.message || '获取评论失败'
  } finally {
    commentsLoading.value = false
  }
}

// 打开登录窗口（模板中不能直接访问 window，统一封装为组件方法）
const openLogin = () => {
  window.dispatchEvent(new CustomEvent('open:login'))
}

const handleLike = async () => {
  if (!userStore.state.isLoggedIn) {
    openLogin()
    return
  }
  likeLoading.value = true
  try {
    const action = liked.value ? 'unlike' : 'like'
    const result = await likeApi.toggle(article.value.id, action)
    liked.value = result.liked
    article.value.likeCount = result.likeCount
  } catch (e) {
    // 401 → token 已失效或被禁用，清除登录态并提示重新登录
    if (e.message && (e.message.includes('登录') || e.message.includes('401'))) {
      userStore.clearAuth()
      window.dispatchEvent(new CustomEvent('auth:expired'))
      return
    }
    alert(e.message || '操作失败')
  } finally {
    likeLoading.value = false
  }
}

const handleSubmitComment = async () => {
  if (!userStore.state.isLoggedIn) {
    openLogin()
    return
  }
  if (!commentContent.value.trim()) {
    alert('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    const newC = await commentApi.create(article.value.id, commentContent.value.trim())
    comments.value.unshift(newC)
    commentContent.value = ''
  } catch (e) {
    alert(e.message || '评论失败')
  } finally {
    commentSubmitting.value = false
  }
}

const handleReply = (commentId) => {
  replyingTo.value = commentId
  replyContent.value = ''
}

const toggleReplies = (commentId) => {
  const set = expandedReplies.value
  if (set.has(commentId)) {
    set.delete(commentId)
  } else {
    set.add(commentId)
  }
  // 触发响应式更新
  expandedReplies.value = new Set(set)
}

const handleCancelReply = () => {
  replyingTo.value = null
  replyContent.value = ''
}

const handleSubmitReply = async () => {
  if (!replyContent.value.trim()) {
    alert('请输入回复内容')
    return
  }
  replySubmitting.value = true
  try {
    const newC = await commentApi.create(article.value.id, replyContent.value.trim(), replyingTo.value)
    comments.value.unshift(newC)
    replyContent.value = ''
    replyingTo.value = null
  } catch (e) {
    alert(e.message || '回复失败')
  } finally {
    replySubmitting.value = false
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

const canDeleteComment = (comment) => {
  return isAdmin.value || isOwner.value
}

const handleDeleteComment = async (commentId) => {
  if (!confirm('确定要删除这条评论吗？')) return
  try {
    await commentApi.delete(commentId)
    comments.value = comments.value.filter(c => c.id !== commentId)
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

const handleEdit = () => {
  router.push(`/create?id=${article.value.id}`)
}

const loadLikeStatus = async () => {
  // 未登录或无文章时不请求
  if (!userStore.state.isLoggedIn || !article.value) return
  try {
    const result = await likeApi.status(article.value.id)
    liked.value = result.liked
    article.value.likeCount = result.likeCount
  } catch (e) {
    // 401 → token 失效，静默清除登录态
    if (e.message && (e.message.includes('登录') || e.message.includes('401'))) {
      userStore.clearAuth()
      return
    }
    console.warn('获取点赞状态失败:', e.message)
  }
}

onMounted(() => {
  loadArticle()
  loadCategories()
  loadComments()
  // 仅登录用户建立 WebSocket 连接（避免 403 控制台报错）
  if (userStore.state.isLoggedIn) {
    connectWS()
  }
})

// 监听登录状态变化：登录后自动加载点赞状态 + 建立 WebSocket
watch(
  () => userStore.state.isLoggedIn,
  (isLoggedIn) => {
    if (isLoggedIn && article.value) {
      loadLikeStatus()
      connectWS()
    }
  }
)
</script>

<template>
  <div class="page">
    <main class="main">
      <div class="article-container">
        <button class="back-btn" @click="router.push('/')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          返回首页
        </button>

        <!-- Loading -->
        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>正在加载文章...</p>
        </div>

        <!-- Error -->
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

        <!-- Article -->
        <article v-else-if="article" class="article">
          <!-- Comment Toast -->
          <Transition name="toast">
            <div v-if="showCommentToast && newCommentNotification" class="comment-toast">
              <div class="toast-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                </svg>
              </div>
              <div class="toast-body">
                <span class="toast-title">新评论</span>
                <span class="toast-text">{{ newCommentNotification.content || newCommentNotification.commentContent }}</span>
              </div>
            </div>
          </Transition>

          <header class="article-header">
            <div class="article-meta-top">
              <span class="category-tag">{{ categoryName }}</span>
            </div>
            <h1 class="article-title">{{ article.title }}</h1>
            <div class="article-info">
              <div class="author">
                <div class="author-avatar">{{ (article.authorName || '用户').charAt(0).toUpperCase() }}</div>
                <div class="author-info">
                  <span class="author-name">{{ article.authorName || '用户' }}</span>
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
                <span v-if="article.viewCount !== undefined" class="stat-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                  {{ article.viewCount }} 阅读
                </span>
                <span class="stat-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z"></path>
                  </svg>
                  {{ article.likeCount || 0 }} 点赞
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

          <div class="divider"></div>

          <!-- 编辑/删除按钮 -->
          <div v-if="canEdit" class="action-bar">
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

          <!-- AI 摘要 -->
          <div v-if="article.summary" class="ai-summary-block">
            <div class="ai-summary-label">AI 摘要</div>
            <div class="ai-summary-content">{{ article.summary }}</div>
          </div>

          <!-- 文章内容 -->
          <div class="article-content">
            <p v-for="(para, idx) in (article.content || '').split('\n').filter(p => p.trim())" :key="idx">
              {{ para }}
            </p>
          </div>

          <!-- 点赞按钮 -->
          <div class="like-section">
            <button
              class="like-btn"
              :class="{ liked }"
              :disabled="likeLoading"
              @click="handleLike"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" :fill="liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z"></path>
              </svg>
              <span>{{ liked ? '已点赞' : '点赞' }} ({{ article.likeCount || 0 }})</span>
            </button>
          </div>

          <div class="divider"></div>

          <!-- 评论区 -->
          <div class="comment-section">
            <h3 class="comment-title">评论 ({{ comments.length }})</h3>

            <!-- 评论表单 -->
            <div v-if="userStore.state.isLoggedIn" class="comment-form">
              <textarea
                v-model="commentContent"
                class="comment-input"
                placeholder="写下你的评论..."
                rows="3"
              ></textarea>
              <div class="comment-form-footer">
                <span class="comment-hint">{{ commentContent.length }} 字</span>
                <button
                  class="comment-submit"
                  :disabled="commentSubmitting || !commentContent.trim()"
                  @click="handleSubmitComment"
                >
                  <span v-if="commentSubmitting" class="spinner-sm"></span>
                  <span>{{ commentSubmitting ? '提交中...' : '发表评论' }}</span>
                </button>
              </div>
            </div>
            <div v-else class="comment-login-hint">
              请<button class="link-btn" @click="openLogin">登录</button>后发表评论
            </div>

            <!-- 评论列表 -->
            <div v-if="commentsLoading" class="loading-state small">
              <div class="spinner"></div>
              <p>加载评论中...</p>
            </div>
            <div v-else-if="commentsError" class="error-state small">
              <p>{{ commentsError }}</p>
            </div>
            <div v-else-if="comments.length === 0" class="empty-comments">
              <p>暂无评论，快来抢沙发</p>
            </div>
            <div v-else class="comments-list">
              <div v-for="comment in topLevelComments" :key="comment.id" class="comment-thread">
                <!-- 父评论 -->
                <div class="comment-item">
                  <div class="comment-avatar">{{ (comment.username || '用户').charAt(0).toUpperCase() }}</div>
                  <div class="comment-body">
                    <div class="comment-header">
                      <span class="comment-username">{{ comment.username || '用户' }}</span>
                      <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                    </div>
                    <p class="comment-content">{{ comment.content }}</p>
                    <div class="comment-actions">
                      <button
                        v-if="canDeleteComment(comment)"
                        class="delete-comment-btn"
                        @click="handleDeleteComment(comment.id)"
                      >删除</button>
                      <button
                        v-if="userStore.state.isLoggedIn"
                        class="reply-btn"
                        @click="handleReply(comment.id)"
                      >回复</button>
                      <button
                        v-if="getReplies(comment.id).length > 0"
                        class="toggle-replies-btn"
                        @click="toggleReplies(comment.id)"
                      >
                        {{ expandedReplies.has(comment.id) ? '收起' : `展开 ${getReplies(comment.id).length} 条回复` }}
                      </button>
                    </div>
                  </div>
                </div>

                <!-- 子回复列表 + 回复输入框（展开时显示） -->
                <template v-if="expandedReplies.has(comment.id)">
                  <div class="replies-list">
                    <div v-for="reply in getReplies(comment.id)" :key="reply.id" class="comment-item reply-item">
                      <div class="comment-avatar reply-avatar">{{ (reply.username || '用户').charAt(0).toUpperCase() }}</div>
                      <div class="comment-body">
                        <div class="comment-header">
                          <span class="comment-username">{{ reply.username || '用户' }}</span>
                          <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                        </div>
                        <p class="comment-content">{{ reply.content }}</p>
                        <div class="comment-actions">
                          <button
                            v-if="canDeleteComment(reply)"
                            class="delete-comment-btn"
                            @click="handleDeleteComment(reply.id)"
                          >删除</button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 回复输入框 -->
                  <div v-if="replyingTo === comment.id" class="reply-form">
                    <textarea
                      v-model="replyContent"
                      class="reply-input"
                      placeholder="写下你的回复..."
                      rows="2"
                    ></textarea>
                    <div class="reply-form-footer">
                      <button class="cancel-reply-btn" @click="handleCancelReply">取消</button>
                      <button
                        class="reply-submit-btn"
                        :disabled="replySubmitting || !replyContent.trim()"
                        @click="handleSubmitReply"
                      >
                        <span v-if="replySubmitting" class="spinner-sm"></span>
                        <span>{{ replySubmitting ? '提交中...' : '回复' }}</span>
                      </button>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </article>

        <!-- Not Found -->
        <div v-else class="error-state">
          <div class="error-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
            </svg>
          </div>
          <p class="error-text">文章不存在或已被删除</p>
          <button class="retry-btn" @click="router.push('/')">返回首页</button>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.page { min-height: 100vh; display: flex; flex-direction: column; }
.main { flex: 1; padding: var(--space-10) 0 var(--space-16); }
.article-container { max-width: 760px; margin: 0 auto; padding: 0 var(--space-6); animation: fadeInUp 0.4s ease-out; }

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
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-20) 0;
}

.loading-state.small { padding: var(--space-8) 0; }
.error-state.small { padding: var(--space-8) 0; }

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  margin-bottom: var(--space-4);
  animation: spin 0.8s linear infinite;
}

.loading-state p { font-size: var(--font-size-base); color: var(--color-text-muted); }

.error-icon {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-5);
  color: var(--color-error);
  background: var(--color-error-light);
  border-radius: 50%;
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
  transition: background var(--transition-fast);
}

.retry-btn:hover { background: var(--color-primary-hover); }

.article {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: var(--space-12) var(--space-10);
  box-shadow: 0 4px 24px rgba(0,0,0,0.3);
}

.article-header { margin-bottom: var(--space-8); }
.article-meta-top { margin-bottom: var(--space-5); }

.category-tag {
  display: inline-block;
  padding: 6px 14px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-radius: var(--radius-full);
}

.article-title {
  font-size: 36px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1.3;
  letter-spacing: -0.8px;
  margin-bottom: var(--space-6);
  font-family: var(--font-serif);
}

.article-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
  flex-wrap: wrap;
}

.author, .author-info { display: flex; align-items: center; gap: var(--space-3); }
.author-info { flex-direction: column; align-items: flex-start; gap: 2px; }

.author-avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  color: #141616;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
}

.author-name { font-size: var(--font-size-sm); font-weight: var(--font-weight-medium); color: var(--color-text-primary); }
.publish-time { font-size: var(--font-size-xs); color: var(--color-text-muted); }

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

.divider { height: 1px; background: var(--color-border-light); margin: var(--space-8) 0; }

.action-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-8);
  padding: var(--space-4);
  background: var(--color-bg-code);
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
  color: #ffffff;
  background: var(--color-primary);
}

.action-btn.edit:hover { background: var(--color-primary-hover); }

.action-btn.delete {
  color: #ffffff;
  background: var(--color-error);
}

.action-btn.delete:hover { background: #dc2626; }

.ai-summary-block {
  background: var(--color-bg-code);
  border-left: 3px solid var(--color-primary);
  border-radius: 8px;
  padding: 16px 18px;
  margin-bottom: var(--space-8);
  word-break: break-word;
  white-space: pre-wrap;
}

.ai-summary-label {
  display: inline-block;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #ffffff;
  background: var(--color-primary);
  border-radius: 4px;
  margin-bottom: 10px;
}

.ai-summary-content {
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text-secondary);
}

.article-content {
  font-size: var(--font-size-base);
  line-height: 2;
  color: var(--color-text-primary);
  letter-spacing: 0.2px;
}

.article-content p { margin-bottom: var(--space-5); text-indent: 2em; }
.article-content p:last-child { margin-bottom: 0; }

.like-section {
  display: flex;
  justify-content: center;
  padding: var(--space-8) 0;
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: 12px 28px;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  background: var(--color-bg-hover);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);
}

.like-btn:hover:not(:disabled) { border-color: var(--color-primary); color: var(--color-primary); }
.like-btn.liked { color: var(--color-primary); border-color: var(--color-primary); background: var(--color-primary-light); }
.like-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.comment-section { margin-top: var(--space-4); }

.comment-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-6);
  font-family: var(--font-serif);
}

.comment-form {
  background: var(--color-bg-code);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  margin-bottom: var(--space-6);
}

.comment-input {
  width: 100%;
  padding: var(--space-3);
  font-size: var(--font-size-base);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-input);
  color: var(--color-text-primary);
  resize: vertical;
  outline: none;
  transition: border-color var(--transition-fast);
}

.comment-input:focus { border-color: var(--color-primary); }

.comment-form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-3);
}

.comment-hint { font-size: var(--font-size-xs); color: var(--color-text-muted); }

.comment-submit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: #ffffff;
  background: var(--color-primary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.comment-submit:hover:not(:disabled) { background: var(--color-primary-hover); }
.comment-submit:disabled { opacity: 0.5; cursor: not-allowed; }

.spinner-sm {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.comment-login-hint {
  text-align: center;
  padding: var(--space-6);
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  background: var(--color-bg-code);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-6);
}

.link-btn { color: var(--color-primary); font-weight: var(--font-weight-medium); }
.link-btn:hover { color: var(--color-primary-hover); }

.empty-comments { text-align: center; padding: var(--space-8); font-size: var(--font-size-sm); color: var(--color-text-muted); }

.comments-list { display: flex; flex-direction: column; gap: var(--space-4); }

.comment-item {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--color-bg-code);
  border-radius: var(--radius-lg);
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  color: #141616;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.comment-body { flex: 1; min-width: 0; }

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-1);
}

.comment-username {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.comment-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.comment-content {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
  word-break: break-word;
}

/* 评论回复 */
.comment-thread {
  display: flex;
  flex-direction: column;
}

.comment-actions {
  margin-top: var(--space-2);
}

.reply-btn {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.reply-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.delete-comment-btn {
  font-size: var(--font-size-xs);
  color: var(--color-error);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.delete-comment-btn:hover {
  background: var(--color-error-light);
}

.toggle-replies-btn {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.toggle-replies-btn:hover {
  background: var(--color-primary-light);
}

.replies-list {
  margin-left: 48px;
  margin-top: var(--space-2);
  padding-left: var(--space-4);
  border-left: 2px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.reply-item {
  padding: var(--space-3);
  background: transparent;
  border: 1px solid var(--color-border-light);
}

.reply-avatar {
  width: 28px;
  height: 28px;
  font-size: 11px;
}

.reply-form {
  margin-left: 48px;
  margin-top: var(--space-2);
  padding: var(--space-3);
  background: var(--color-bg-code);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
}

.reply-input {
  width: 100%;
  padding: var(--space-2) var(--space-3);
  font-size: var(--font-size-sm);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-input);
  color: var(--color-text-primary);
  resize: vertical;
  outline: none;
  transition: border-color var(--transition-fast);
}

.reply-input:focus {
  border-color: var(--color-primary);
}

.reply-form-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.cancel-reply-btn {
  padding: 6px 14px;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.cancel-reply-btn:hover {
  background: var(--color-bg-hover);
}

.reply-submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: #ffffff;
  background: var(--color-primary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.reply-submit-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.reply-submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Toast */
.comment-toast {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 18px;
  margin-bottom: var(--space-6);
  background: linear-gradient(135deg, #1a2a1f, #1e3a24);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-lg);
  animation: slideIn 0.3s ease-out;
}

.toast-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary-light);
  border-radius: var(--radius-full);
  color: var(--color-primary);
}

.toast-body { display: flex; flex-direction: column; gap: 2px; overflow: hidden; }

.toast-title {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.toast-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 500px;
}

.toast-enter-active { animation: slideIn 0.3s ease-out; }
.toast-leave-active { animation: slideOut 0.3s ease-in; }

@keyframes fadeInUp { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes slideIn { from { opacity: 0; transform: translateY(-12px); } to { opacity: 1; transform: translateY(0); } }
@keyframes slideOut { from { opacity: 1; transform: translateY(0); } to { opacity: 0; transform: translateY(-12px); } }
</style>