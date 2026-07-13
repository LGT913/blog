<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSiteStore } from '../store/site'
import { siteApi } from '../api'
import { useUserStore } from '../store/user'

const router = useRouter()
const siteStore = useSiteStore()
const userStore = useUserStore()

const form = ref({
  siteName: '',
  siteLogo: '',
  siteDesc: '',
  slogan: '',
  copyright: '',
  recordNo: '',
  email: ''
})

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const success = ref('')

onMounted(() => {
  if (!userStore.state.isLoggedIn) {
    router.push('/')
    return
  }
  loadForm()
})

const loadForm = async () => {
  loading.value = true
  error.value = ''
  try {
    const data = await siteApi.getConfig('blog_info')
    if (data && data.configValue) {
      const parsed = JSON.parse(data.configValue)
      form.value = { ...form.value, ...parsed }
    }
  } catch (e) {
    error.value = '加载配置失败：' + (e.message || '未知错误')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  saving.value = true
  error.value = ''
  success.value = ''
  try {
    const configValue = JSON.stringify(form.value)
    await siteApi.updateConfig('blog_info', configValue)
    success.value = '配置保存成功！'
    siteStore.loadConfig()
  } catch (e) {
    if (e.message && e.message.includes('401')) {
      error.value = '登录已过期，请重新登录后再保存配置'
      userStore.logout()
      setTimeout(() => router.push('/'), 2000)
    } else {
      error.value = '保存失败：' + (e.message || '未知错误')
    }
  } finally {
    saving.value = false
  }
}
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
          <div>
            <h1 class="page-title">网站配置</h1>
            <p class="page-desc">管理网站名称、LOGO、版权信息等基础配置</p>
          </div>
        </div>

        <div v-if="error" class="message message-error">{{ error }}</div>
        <div v-if="success" class="message message-success">{{ success }}</div>

        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>加载配置中...</p>
        </div>

        <form v-else class="config-form" @submit.prevent="handleSubmit">
          <div class="form-section">
            <h2 class="section-title">基本信息</h2>
            <div class="form-group">
              <label class="form-label">网站名称</label>
              <input v-model="form.siteName" type="text" class="form-input" placeholder="输入网站名称" maxlength="50" />
            </div>
            <div class="form-group">
              <label class="form-label">LOGO 路径</label>
              <input v-model="form.siteLogo" type="text" class="form-input" placeholder="如 /logo.png，留空则显示文字 LOGO" />
              <p class="form-hint">相对于 public 目录的路径，例如 /logo.png</p>
            </div>
            <div class="form-group">
              <label class="form-label">网站简介</label>
              <input v-model="form.siteDesc" type="text" class="form-input" placeholder="简短描述站点" maxlength="200" />
            </div>
          </div>

          <div class="form-section">
            <h2 class="section-title">首页内容</h2>
            <div class="form-group">
              <label class="form-label">首页标语</label>
              <input v-model="form.slogan" type="text" class="form-input" placeholder="首页主标题" maxlength="100" />
            </div>
          </div>

          <div class="form-section">
            <h2 class="section-title">底部信息</h2>
            <div class="form-group">
              <label class="form-label">版权声明</label>
              <input v-model="form.copyright" type="text" class="form-input" placeholder="如 All Rights Reserved" maxlength="100" />
            </div>
            <div class="form-group">
              <label class="form-label">备案号</label>
              <input v-model="form.recordNo" type="text" class="form-input" placeholder="如 粤ICP备xxxxxxxx号" maxlength="50" />
            </div>
            <div class="form-group">
              <label class="form-label">联系邮箱</label>
              <input v-model="form.email" type="email" class="form-input" placeholder="联系邮箱地址" maxlength="100" />
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-ghost" @click="router.push('/')">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="saving">
              <span v-if="saving" class="spinner-sm"></span>
              <span>{{ saving ? '保存中...' : '保存配置' }}</span>
            </button>
          </div>
        </form>
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
  max-width: 680px;
  margin: 0 auto;
  padding: 0 var(--space-6);
}

.page-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-5);
  margin-bottom: var(--space-8);
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: var(--color-text-secondary);
  border: none;
  background: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
  margin-top: 4px;
}

.back-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.page-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.message {
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-6);
}

.message-error {
  background: var(--color-error-light);
  color: var(--color-error);
}

.message-success {
  background: #ecfdf5;
  color: #059669;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
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

.config-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

.form-section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.section-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  padding-bottom: var(--space-4);
  margin-bottom: var(--space-5);
  border-bottom: 1px solid var(--color-border-light);
}

.form-group {
  margin-bottom: var(--space-5);
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2);
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  font-size: var(--font-size-base);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  outline: none;
  transition: border-color var(--transition-fast);
}

.form-input:focus {
  border-color: var(--color-primary);
}

.form-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-top: 4px;
}

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
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-ghost {
  color: var(--color-text-secondary);
  background: none;
}

.btn-ghost:hover {
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

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
