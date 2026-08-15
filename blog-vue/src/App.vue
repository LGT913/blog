<script setup>
import { ref, onMounted, provide } from 'vue'
import { RouterView } from 'vue-router'
import { useUserStore } from './store/user'
import { useSiteStore } from './store/site'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import LoginModal from './components/LoginModal.vue'
import RegisterModal from './components/RegisterModal.vue'

const userStore = useUserStore()
const siteStore = useSiteStore()

// 提供站点配置到全局（子孙组件可通过 inject 获取）
provide('siteConfig', siteStore.state)

const showLogin = ref(false)
const showRegister = ref(false)

const handleOpenLogin = () => { showLogin.value = true }
const handleOpenRegister = () => { showRegister.value = true }
const handleCloseLogin = () => { showLogin.value = false }
const handleCloseRegister = () => { showRegister.value = false }
const handleSwitchToRegister = () => {
  showLogin.value = false
  showRegister.value = true
}
const handleSwitchToLogin = () => {
  showRegister.value = false
  showLogin.value = true
}

onMounted(() => {
  // 1. 初始化用户登录状态（从 localStorage 恢复，idempotent）
  userStore.init()

  // 2. 注册退出登录回调：用户退出时自动重置配置
  userStore.onLogout(() => {
    siteStore.resetConfig()
  })

  // 3. 加载站点配置（idempotent，main.js 已调用过，这里确保回调注册后再次确认）
  siteStore.loadConfig()

  // 4. 监听全局 open:login 事件（由 ArticleDetail 等非 Header 组件触发）
  window.addEventListener('open:login', () => { showLogin.value = true })

  // 5. 监听 auth:expired 事件（token 过期时自动弹出登录窗）
  window.addEventListener('auth:expired', () => { showLogin.value = true })
})
</script>

<template>
  <div class="app-container">
    <Header
      @open-login="handleOpenLogin"
      @open-register="handleOpenRegister"
    />
    <RouterView v-slot="{ Component }">
      <Transition name="page" mode="out-in">
        <component :is="Component" />
      </Transition>
    </RouterView>
    <Footer />

    <Transition name="modal">
      <LoginModal
        v-if="showLogin"
        @close="handleCloseLogin"
        @switch="handleSwitchToRegister"
      />
    </Transition>

    <Transition name="modal">
      <RegisterModal
        v-if="showRegister"
        @close="handleCloseRegister"
        @switch="handleSwitchToLogin"
      />
    </Transition>
  </div>
</template>

<style>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 路由切换动画 */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>