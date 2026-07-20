import axios from 'axios'
import type { ApiError } from './types'
import { useAuthStore } from '@/stores/auth'

let globalNavigate: ((path: string) => void) | null = null

export function setNavigator(nav: (path: string) => void) {
  globalNavigate = nav
}

const client = axios.create({
  baseURL: '/clinic-api/api',
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  config.headers['X-Correlation-Id'] = crypto.randomUUID()
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const status = error.response.status as number
      const data = error.response.data as ApiError | undefined

      if (status === 401) {
        const auth = useAuthStore()
        auth.logout()
        globalNavigate?.('/login')
      }

      const message = data?.message ?? `HTTP ${status}`
      const correlationId = data?.correlationId ?? ''
      console.error(`[API ${status}] ${message} (${correlationId})`)
    } else {
      console.error('[API] Network error:', error.message)
    }
    return Promise.reject(error)
  },
)

export default client
