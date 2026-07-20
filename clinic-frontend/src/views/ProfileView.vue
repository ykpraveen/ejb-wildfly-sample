<template>
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">My Profile</h2>

    <v-card max-width="600" elevation="2">
      <v-card-text class="pa-6">
        <div class="d-flex align-center mb-6">
          <v-avatar color="primary" size="72" class="mr-4">
            <span class="text-h4 text-white">{{ initials }}</span>
          </v-avatar>
          <div>
            <div class="text-h5 font-weight-bold">{{ authStore.username }}</div>
            <div class="text-body-2 text-medium-emphasis">
              Clinic #{{ authStore.clinicId }}
            </div>
          </div>
        </div>

        <v-divider class="mb-4" />

        <v-list lines="one" density="compact" class="pa-0">
          <v-list-item prepend-icon="mdi-identifier">
            <v-list-item-title>Username</v-list-item-title>
            <v-list-item-subtitle>{{ authStore.username }}</v-list-item-subtitle>
          </v-list-item>
          <v-list-item prepend-icon="mdi-hospital-building">
            <v-list-item-title>Clinic</v-list-item-title>
            <v-list-item-subtitle>#{{ authStore.clinicId }}</v-list-item-subtitle>
          </v-list-item>
          <v-list-item prepend-icon="mdi-shield-account">
            <v-list-item-title>Roles</v-list-item-title>
            <template #subtitle>
              <div class="d-flex flex-wrap ga-1 mt-1">
                <v-chip
                  v-for="role in authStore.roles"
                  :key="role"
                  :color="roleColor(role)"
                  size="small"
                  variant="flat"
                >
                  {{ role }}
                </v-chip>
              </div>
            </template>
          </v-list-item>
        </v-list>

        <v-divider class="my-4" />

        <div class="d-flex justify-space-between align-center">
          <div class="text-caption text-medium-emphasis">
            Session active
          </div>
          <v-btn
            color="error"
            variant="outlined"
            prepend-icon="mdi-logout"
            @click="handleLogout"
          >
            Sign Out
          </v-btn>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const initials = computed(() => {
  const name = authStore.username
  if (!name) return '?'
  const parts = name.split(/[.\s_-]+/)
  return parts.length >= 2
    ? (parts[0][0] + parts[1][0]).toUpperCase()
    : name.slice(0, 2).toUpperCase()
})

function roleColor(role: string): string {
  const map: Record<string, string> = {
    ADMIN: 'red',
    DOCTOR: 'blue',
    USER: 'grey',
    CUSTOMER: 'green',
  }
  return map[role] ?? 'grey'
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
