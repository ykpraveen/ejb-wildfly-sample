import type { Pinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

export default function setupPersist(pinia: Pinia) {
  pinia.use(piniaPluginPersistedstate)
}
