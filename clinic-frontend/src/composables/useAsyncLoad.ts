import { ref, onMounted } from 'vue'
import { useNotificationStore } from '@/stores/notification'

/**
 * Wraps the loading-flag / try-catch-finally / onMounted boilerplate that every list view
 * repeats around its data fetch. The fetch logic itself (guards, multiple calls, mapping into
 * refs) stays with the caller since it differs per view.
 */
export function useAsyncLoad(loader: () => Promise<void>, errorMessage?: string, immediate = true) {
  const notify = useNotificationStore()
  const loading = ref(false)

  async function run() {
    loading.value = true
    try {
      await loader()
    } catch {
      if (errorMessage) notify.error(errorMessage)
    } finally {
      loading.value = false
    }
  }

  if (immediate) {
    onMounted(run)
  }

  return { loading, run }
}
