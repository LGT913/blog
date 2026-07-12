import { reactive } from 'vue'

const state = reactive({
  user: null,
  token: null,
  isLoggedIn: false
})

const setUser = (user, token) => {
  state.user = user
  state.token = token
  state.isLoggedIn = !!user
  if (user) {
    localStorage.setItem('blog_user', JSON.stringify(user))
    if (token) localStorage.setItem('blog_token', token)
  } else {
    localStorage.removeItem('blog_user')
    localStorage.removeItem('blog_token')
  }
}

const login = (user, token) => {
  setUser(user, token)
}

const logout = () => {
  state.user = null
  state.token = null
  state.isLoggedIn = false
  localStorage.removeItem('blog_user')
  localStorage.removeItem('blog_token')
}

const init = () => {
  const savedUser = localStorage.getItem('blog_user')
  const savedToken = localStorage.getItem('blog_token')
  if (savedUser && savedToken) {
    try {
      setUser(JSON.parse(savedUser), savedToken)
    } catch (e) {
      localStorage.removeItem('blog_user')
      localStorage.removeItem('blog_token')
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