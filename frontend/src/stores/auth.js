import { defineStore } from 'pinia'
import request from '../utils/request'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('civil-token') || '',
    user: null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    roles: (state) => state.user?.roles || []
  },
  actions: {
    async login(form) {
      const data = await request.post('/api/auth/login', form)
      this.token = data.token
      this.user = data.user
      localStorage.setItem('civil-token', data.token)
    },
    async fetchMe() {
      if (!this.token) {
        return null
      }
      try {
        this.user = await request.get('/api/auth/me')
        return this.user
      } catch (error) {
        this.logout()
        return null
      }
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('civil-token')
    }
  }
})
