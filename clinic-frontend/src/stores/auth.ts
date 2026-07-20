import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import type { LoginResponse } from '@/api/types'

interface StoredUser {
  clinicId: number
  username: string
  roles: string[]
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref('')
  const user = ref<StoredUser | null>(null)

  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const roles = computed(() => user.value?.roles ?? [])
  const username = computed(() => user.value?.username ?? '')
  const clinicId = computed(() => user.value?.clinicId ?? 0)

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  async function login(clinicId: number, username: string, password: string): Promise<void> {
    const { data } = await authApi.login({ clinicId, username, password })
    token.value = data.accessToken
    user.value = { clinicId: data.clinicId, username: data.username, roles: data.roles }
  }

  function logout(): void {
    token.value = ''
    user.value = null
  }

  return { token, user, isAuthenticated, roles, username, clinicId, hasRole, login, logout }
}, {
  persist: {
    storage: sessionStorage,
    pick: ['token', 'user'],
  },
})
