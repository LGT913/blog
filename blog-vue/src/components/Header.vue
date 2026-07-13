<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useSiteStore } from '../store/site'

const emit = defineEmits(['open-login', 'open-register'])

const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()

const navItems = computed(() => {
  const items = [
    { path: '/', label: '首页' },
    { path: '/categories', label: '分类管理' }
  ]
  if (userStore.state.isLoggedIn) {
    items.push({ path: '/admin/config', label: '网站配置' })
  }
  return items
})

const currentPath = computed(() => router.currentRoute.value.path)

const isActive = (path) => {
  return currentPath.value === path || (path !== '/' && currentPath.value.startsWith(path))
}

const handleLogout = () => {
  userStore.logout()
  siteStore.resetConfig()
  router.push('/')
}

const handleOpenLogin = () => {
  emit('open-login')
}

const handleOpenRegister = () => {
  emit('open-register')
}
</script>

<template>
  <header class="header">
    <div class="header-inner">
      <div class="logo" @click="router.push('/')">
        <span class="logo-icon">✦</span>
        <span class="logo-text">{{ siteStore.state.config.siteName }}</span>
      </div>

      <nav class="nav">
        <button
          v-for="item in navItems"
          :key="item.path"
          class="nav-item"
          :class="{ 'nav-item-active': isActive(item.path) }"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="actions">
        <template v-if="userStore.state.isLoggedIn">
          <button
            class="btn btn-primary btn-sm"
            @click="router.push('/create')"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 20h9"></path>
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
            </svg>
            写文章
          </button>
          <div class="user-info">
            <div class="avatar">
              {{ userStore.state.user.nickname.charAt(0).toUpperCase() }}
            </div>
            <div class="user-detail">
              <div class="user-name">{{ userStore.state.user.nickname }}</div>
              <button class="logout-btn" @click="handleLogout">退出登录</button>
            </div>
          </div>
        </template>
        <template v-else>
          <button class="btn btn-ghost btn-sm" @click="handleOpenLogin">
            登录
          </button>
          <button class="btn btn-primary btn-sm" @click="handleOpenRegister">
            注册
          </button>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid var(--color-border-light);
}

.header-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 var(--space-6);
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-8);
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  user-select: none;
}

.logo-icon {
  font-size: 22px;
  color: var(--color-primary);
  line-height: 1;
}

.logo-text {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  letter-spacing: -0.5px;
}

.nav {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  flex: 1;
  justify-content: center;
}

.nav-item {
  padding: 8px 16px;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  position: relative;
}

.nav-item:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}

.nav-item-active {
  color: var(--color-primary);
  background: var(--color-primary-light);
}

.nav-item-active:hover {
  color: var(--color-primary);
  background: var(--color-primary-lighter);
}

.actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-weight: var(--font-weight-medium);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.btn-sm {
  padding: 8px 16px;
  font-size: var(--font-size-sm);
  height: 36px;
}

.btn-primary {
  background: var(--color-primary);
  color: #ffffff;
}

.btn-primary:hover {
  background: var(--color-primary-hover);
}

.btn-ghost {
  color: var(--color-text-secondary);
}

.btn-ghost:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-left: var(--space-2);
  padding-left: var(--space-4);
  border-left: 1px solid var(--color-border-light);
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), #7c3aed);
  color: #ffffff;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.logout-btn {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  padding: 0;
  text-align: left;
}

.logout-btn:hover {
  color: var(--color-error);
}
</style>
