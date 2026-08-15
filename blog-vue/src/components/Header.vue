<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useSiteStore } from '../store/site'
import { userApi } from '../api'

const emit = defineEmits(['open-login', 'open-register'])

const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()

const showDropdown = ref(false)
const showMobileMenu = ref(false)
const closeTimer = ref(null)

const navItems = computed(() => {
  const items = [
    { path: '/', label: '首页' },
    { path: '/ranking', label: '排行榜' }
  ]
  return items
})

const currentPath = computed(() => router.currentRoute.value.path)

const isActive = (path) => {
  return currentPath.value === path || (path !== '/' && currentPath.value.startsWith(path))
}

const toggleDropdown = () => { showDropdown.value = !showDropdown.value }
const closeDropdown = () => {
  closeTimer.value = setTimeout(() => {
    showDropdown.value = false
  }, 200)
}
const cancelCloseDropdown = () => {
  if (closeTimer.value) {
    clearTimeout(closeTimer.value)
    closeTimer.value = null
  }
}
const toggleMobileMenu = () => { showMobileMenu.value = !showMobileMenu.value }

const handleLogout = async () => {
  cancelCloseDropdown()
  try {
    await userApi.logout()
  } catch (e) {
    // ignore
  }
  userStore.logout()
  siteStore.resetConfig()
  closeDropdown()
  router.push('/')
}

const handleGoToProfile = () => {
  closeDropdown()
  router.push('/profile')
}

const handleGoToAdmin = () => {
  closeDropdown()
  router.push('/admin')
}

const handleOpenLogin = () => {
  closeDropdown()
  emit('open-login')
}

const handleOpenRegister = () => {
  closeDropdown()
  emit('open-register')
}

const navigateTo = (path) => {
  showMobileMenu.value = false
  router.push(path)
}
</script>

<template>
  <header class="header">
    <div class="header-inner">
      <div class="logo" @click="router.push('/')">
        <span class="logo-mark">✦</span>
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
          <button class="btn btn-primary" @click="router.push('/create')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 20h9"></path>
              <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
            </svg>
            写文章
          </button>
          <div class="user-dropdown" @mouseleave="closeDropdown">
            <button class="avatar-btn" @click="toggleDropdown" @mouseenter="cancelCloseDropdown">
              <div class="avatar">
                {{ userStore.state.user.nickname?.charAt(0).toUpperCase() || 'U' }}
              </div>
            </button>
            <Transition name="dropdown">
              <div v-if="showDropdown" class="dropdown-menu" @mouseenter="cancelCloseDropdown" @mouseleave="closeDropdown">
                <div class="dropdown-header">
                  <span class="dropdown-name">{{ userStore.state.user.nickname }}</span>
                  <span v-if="userStore.isAdmin.value" class="role-badge">管理员</span>
                </div>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item" @click="handleGoToProfile">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                  </svg>
                  个人中心
                </button>
                <button v-if="userStore.isAdmin.value" class="dropdown-item" @click="handleGoToAdmin">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="3"></circle>
                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                  </svg>
                  管理面板
                </button>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item logout" @click="handleLogout">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                    <polyline points="16 17 21 12 16 7"></polyline>
                    <line x1="21" y1="12" x2="9" y2="12"></line>
                  </svg>
                  退出登录
                </button>
              </div>
            </Transition>
          </div>
        </template>
        <template v-else>
          <button class="btn btn-ghost" @click="handleOpenLogin">登录</button>
          <button class="btn btn-primary" @click="handleOpenRegister">注册</button>
        </template>
      </div>

      <!-- 移动端汉堡菜单 -->
      <button class="hamburger" @click="toggleMobileMenu">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <template v-if="!showMobileMenu">
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <line x1="3" y1="12" x2="21" y2="12"></line>
            <line x1="3" y1="18" x2="21" y2="18"></line>
          </template>
          <template v-else>
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </template>
        </svg>
      </button>
    </div>

    <!-- 移动端菜单 -->
    <Transition name="mobile-menu">
      <div v-if="showMobileMenu" class="mobile-menu">
        <button
          v-for="item in navItems"
          :key="item.path"
          class="mobile-nav-item"
          :class="{ active: isActive(item.path) }"
          @click="navigateTo(item.path)"
        >
          {{ item.label }}
        </button>
        <div class="mobile-divider"></div>
        <template v-if="userStore.state.isLoggedIn">
          <button class="mobile-nav-item" @click="navigateTo('/create')">写文章</button>
          <button class="mobile-nav-item" @click="navigateTo('/profile')">个人中心</button>
          <button v-if="userStore.isAdmin.value" class="mobile-nav-item" @click="navigateTo('/admin')">管理面板</button>
          <button class="mobile-nav-item logout" @click="handleLogout">退出登录</button>
        </template>
        <template v-else>
          <button class="mobile-nav-item" @click="handleOpenLogin">登录</button>
          <button class="mobile-nav-item primary" @click="handleOpenRegister">注册</button>
        </template>
      </div>
    </Transition>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(20, 22, 22, 0.78);
  backdrop-filter: saturate(180%) blur(24px);
  -webkit-backdrop-filter: saturate(180%) blur(24px);
  border-bottom: 1px solid rgba(46, 50, 50, 0.5);
  transition: background var(--transition-normal);
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
  gap: 12px;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}

.logo-mark {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-primary);
  box-shadow: 0 0 10px rgba(126, 200, 160, 0.5);
  animation: cursor-blink 1.2s step-end infinite;
  flex-shrink: 0;
}

@keyframes cursor-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.logo-text {
  font-family: 'JetBrains Mono', var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: 1px;
}

.nav {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  flex: 1;
  justify-content: center;
}

.nav-item {
  padding: 8px 18px;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  font-family: var(--font-serif);
  color: var(--color-text-muted);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  letter-spacing: 0.5px;
}

.nav-item:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}

.nav-item-active {
  color: var(--color-primary);
  background: rgba(126, 200, 160, 0.08);
}

.nav-item-active:hover {
  color: var(--color-primary);
  background: rgba(126, 200, 160, 0.12);
}

.actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 38px;
  padding: 0 20px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  letter-spacing: 0.3px;
}

.btn-primary {
  background: var(--color-primary);
  color: #141616;
  font-weight: var(--font-weight-semibold);
}

.btn-primary:hover {
  background: var(--color-primary-hover);
  box-shadow: 0 0 20px rgba(126, 200, 160, 0.25);
}

.btn-ghost {
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
}

.btn-ghost:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: rgba(126, 200, 160, 0.06);
}

/* 用户下拉菜单 */
.user-dropdown {
  position: relative;
}

.avatar-btn {
  padding: 0;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-primary), #4a9e6e);
  color: #141616;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'JetBrains Mono', var(--font-mono);
  transition: all var(--transition-fast);
  border: 2px solid transparent;
}

.avatar-btn:hover .avatar {
  transform: scale(1.05);
  border-color: var(--color-primary);
  box-shadow: 0 0 16px rgba(126, 200, 160, 0.3);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  min-width: 190px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: var(--space-2);
  z-index: 60;
}

.dropdown-header {
  padding: var(--space-2) var(--space-3);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.dropdown-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.role-badge {
  font-size: 10px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
  background: rgba(126, 200, 160, 0.12);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  letter-spacing: 0.3px;
}

.dropdown-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: var(--space-1) 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-2) var(--space-3);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.dropdown-item:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}

.dropdown-item.logout:hover {
  color: var(--color-error);
  background: var(--color-error-light);
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.96);
}

/* 移动端汉堡菜单 */
.hamburger {
  display: none;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.hamburger:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}

.mobile-menu {
  display: none;
  flex-direction: column;
  padding: var(--space-4) var(--space-6);
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border-light);
}

.mobile-nav-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.mobile-nav-item:hover,
.mobile-nav-item.active {
  color: var(--color-primary);
}

.mobile-nav-item.logout {
  color: var(--color-error);
}

.mobile-nav-item.primary {
  color: var(--color-primary);
}

.mobile-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: var(--space-2) 0;
}

.mobile-menu-enter-active,
.mobile-menu-leave-active {
  transition: all var(--transition-normal);
}

.mobile-menu-enter-from,
.mobile-menu-leave-to {
  opacity: 0;
  max-height: 0;
}

@media (max-width: 768px) {
  .nav { display: none; }
  .actions { display: none; }
  .hamburger { display: flex; }
  .mobile-menu { display: flex; }
}
</style>