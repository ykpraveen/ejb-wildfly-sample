import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { useAuthStore } from './auth'

export const useClinicStore = defineStore('clinic', () => {
  const authStore = useAuthStore()
  const clinicId = ref(authStore.clinicId)

  watch(() => authStore.clinicId, (id) => {
    clinicId.value = id
  })

  return { clinicId }
})
