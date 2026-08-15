<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { userApi } from '../api'

const emit = defineEmits(['close', 'switch'])

const router = useRouter()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const nickname = ref('')
const error = ref('')
const loading = ref(false)

const handleSubmit = async () => {
  if (!username.value.trim()) {
    error.value = '请输入用户名'
    return
  }
  if (!password.value) {
    error.value = '请输入密码'
    return
  }
  if (!nickname.value.trim()) {
    error.value = '请输入昵称'
    return
  }

  loading.value = true
  error.value = ''

  try {
    await userApi.register({
      username: username.value.trim(),
      password: password.value,
      nickname: nickname.value.trim()
    })
    const loginResult = await userApi.login({ username: username.value.trim(), password: password.value })
    userStore.login(loginResult.user, loginResult.token)
    emit('close')
    router.push('/')
  } catch (e) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  error.value = ''
  emit('close')
}
const handleSwitch = () => {
  error.value = ''
  emit('switch')
}
</script>

<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal">
      <button class="close-btn" @click="handleClose">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>

      <div class="modal-header">
        <div class="logo-mark">✦</div>
        <h2>创建账户</h2>
        <p>加入我们，开启你的博客之旅</p>
      </div>

      <div v-if="error" class="error-banner">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        <span>{{ error }}</span>
      </div>

      <form class="form" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <div class="input-wrapper">
            <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <input v-model="username" type="text" class="form-input" placeholder="请设置用户名" autocomplete="username" />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">昵称</label>
          <div class="input-wrapper">
            <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <input v-model="nickname" type="text" class="form-input" placeholder="请输入昵称" autocomplete="nickname" />
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <div class="input-wrapper">
            <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
            <input v-model="password" type="password" class="form-input" placeholder="请设置密码" autocomplete="new-password" />
          </div>
        </div>

        <button type="submit" :disabled="loading" class="submit-btn">
          <span v-if="loading" class="spinner"></span>
          <span>{{ loading ? '注册中...' : '注册' }}</span>
        </button>
      </form>

      <div class="modal-footer">
        <span>已有账户？</span>
        <button class="link-btn" @click="handleSwitch">去登录</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  animation: fadeIn var(--transition-normal) ease-out;
}

.modal {
  position: relative;
  width: 100%;
  max-width: 420px;
  margin: var(--space-6);
  padding: var(--space-10) var(--space-8);
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  animation: scaleIn var(--transition-normal) ease-out;
}

.close-btn {
  position: absolute;
  top: var(--space-4);
  right: var(--space-4);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.close-btn:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}

.modal-header {
  text-align: center;
  margin-bottom: var(--space-8);
}

.logo-mark {
  width: 56px;
  height: 56px;
  margin: 0 auto var(--space-4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-radius: var(--radius-lg);
}

.modal-header h2 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-2);
  letter-spacing: -0.5px;
}

.modal-header p {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

.error-banner {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-6);
  background: var(--color-error-light);
  color: var(--color-error);
  font-size: var(--font-size-sm);
  border-radius: var(--radius-md);
}

.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  margin-left: var(--space-1);
}

.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
  pointer-events: none;
}

.form-input {
  width: 100%;
  padding: 12px 14px 12px 42px;
  font-size: var(--font-size-base);
}

.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  width: 100%;
  height: 48px;
  margin-top: var(--space-2);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: #141616;
  background: var(--color-primary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.submit-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(26, 27, 30, 0.3);
  border-top-color: #141616;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--color-border-light);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.link-btn {
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  padding: 0;
}

.link-btn:hover {
  color: var(--color-primary-hover);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.96) translateY(8px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>