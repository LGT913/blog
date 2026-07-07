import { reactive } from 'vue'

const state = reactive({
  user: null,
  isLoggedIn: false
})

const setUser = (user) => {
  state.user = user
  state.isLoggedIn = !!user
  if (user) {
    localStorage.setItem('blog_user', JSON.stringify(user))
  } else {
    localStorage.removeItem('blog_user')
  }
}

const login = (user) => {
  setUser(user)
}

const logout = () => {
  state.user = null
  state.isLoggedIn = false
  localStorage.removeItem('blog_user')
}

const init = () => {
  const savedUser = localStorage.getItem('blog_user')
  if (savedUser) {
    try {
      setUser(JSON.parse(savedUser))
    } catch (e) {
      localStorage.removeItem('blog_user')
    }
  }
}

export function useUserStore() {
  return {
    state,
    login,
    logout,
    init
  }
}
