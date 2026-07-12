<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { RouterView } from 'vue-router'
import { useUserStore } from './store/user'
import { useSiteStore } from './store/site'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import LoginModal from './components/LoginModal.vue'
import RegisterModal from './components/RegisterModal.vue'

const userStore = useUserStore()
const siteStore = useSiteStore()

const showLogin = ref(false)
const showRegister = ref(false)

const handleOpenLogin = () => {
  showLogin.value = true
}

const handleOpenRegister = () => {
  showRegister.value = true
}

const handleCloseLogin = () => {
  showLogin.value = false
}

const handleCloseRegister = () => {
  showRegister.value = false
}

const handleSwitchToRegister = () => {
  showLogin.value = false
  showRegister.value = true
}

const handleSwitchToLogin = () => {
  showRegister.value = false
  showLogin.value = true
}

onMounted(() => {
  userStore.init()
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
