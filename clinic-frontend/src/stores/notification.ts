import { ref } from 'vue'
import { defineStore } from 'pinia'

export type NotificationType = 'success' | 'error' | 'warning' | 'info'

interface Notification {
  message: string
  type: NotificationType
  timeout: number
}

export const useNotificationStore = defineStore('notification', () => {
  const visible = ref(false)
  const message = ref('')
  const type = ref<NotificationType>('info')
  const timeout = ref(3000)

  function show(msg: string, notificationType: NotificationType = 'info', ms = 3000) {
    message.value = msg
    type.value = notificationType
    timeout.value = ms
    visible.value = true
  }

  function success(msg: string) { show(msg, 'success') }
  function error(msg: string) { show(msg, 'error', 5000) }
  function warning(msg: string) { show(msg, 'warning', 4000) }
  function info(msg: string) { show(msg, 'info') }

  function dismiss() { visible.value = false }

  return { visible, message, type, timeout, show, success, error, warning, info, dismiss }
})
