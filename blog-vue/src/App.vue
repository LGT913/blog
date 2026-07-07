<script setup>
import { ref, onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { useUserStore } from './store/user'
import Header from './components/Header.vue'
import LoginModal from './components/LoginModal.vue'
import RegisterModal from './components/RegisterModal.vue'

const userStore = useUserStore()

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
})
</script>

<template>
  <div class="app-container">
    <Header 
      @open-login="handleOpenLogin"
      @open-register="handleOpenRegister"
    />
    
    <RouterView />

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
}
</style>
