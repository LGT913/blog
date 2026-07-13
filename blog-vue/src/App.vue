<script setup>
import { ref, onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { useUserStore } from './store/user'
import { useSiteStore } from './store/site'
import { setClearAuthCallback } from './api'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import LoginModal from './components/LoginModal.vue'
import RegisterModal from './components/RegisterModal.vue'

const userStore = useUserStore()
const siteStore = useSiteStore()

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
  // 1. 初始化用户登录状态（从 localStorage 恢复）
  userStore.init()

  // 2. 注册退出登录回调：用户退出时自动重置配置
  userStore.onLogout(() => {
    siteStore.resetConfig()
  })

  // 3. 注册 token 过期回调（api/index.js 中 401 时触发）
  setClearAuthCallback(() => {
    userStore.clearAuth()
    siteStore.resetConfig()
  })

  // 4. 加载站点配置
  // 如果有本地缓存，会先展示缓存内容，后台静默更新
  // 如果没有缓存，直接请求后端
  siteStore.loadConfig()
})
</script>

<template>
  <div class="app-container">
    <Header
      @open-login="handleOpenLogin"
      @open-register="handleOpenRegister"
    />
    <RouterView />
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
.app-container > main {
  flex: 1;
}
</style>