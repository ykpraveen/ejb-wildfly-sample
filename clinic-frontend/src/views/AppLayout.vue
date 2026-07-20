<template>
  <v-app>
    <v-app-bar color="primary" density="comfortable" flat>
      <v-app-bar-nav-icon @click="drawer = !drawer" />
      <v-app-bar-title>
        <v-icon icon="mdi-clinic-medical" class="mr-1" />
        Clinic Portal
      </v-app-bar-title>
      <v-spacer />
      <v-chip size="small" variant="outlined" class="mr-2" color="white">
        {{ authStore.username }}
      </v-chip>
      <v-menu>
        <template #activator="{ props }">
          <v-btn icon v-bind="props">
            <v-avatar size="32" color="white">
              <span class="text-primary text-body-2">{{ initials }}</span>
            </v-avatar>
          </v-btn>
        </template>
        <v-list density="compact">
          <v-list-item prepend-icon="mdi-account" title="Profile" @click="$router.push('/me')" />
          <v-divider />
          <v-list-item prepend-icon="mdi-logout" title="Sign Out" @click="handleLogout" />
        </v-list>
      </v-menu>
    </v-app-bar>

    <v-navigation-drawer v-model="drawer" :permanent="$vuetify.display.mdAndUp" elevation="4">
      <v-list nav density="compact">
        <v-list-item
          v-for="item in filteredNavItems"
          :key="item.to"
          :to="item.to"
          :prepend-icon="item.icon"
          :title="item.title"
          rounded="lg"
        />
      </v-list>
      <template #append>
        <v-list-item prepend-icon="mdi-logout" title="Sign Out" @click="handleLogout" rounded="lg" class="mb-2" />
      </template>
    </v-navigation-drawer>

    <v-main>
      <v-container fluid class="pa-6">
        <router-view />
      </v-container>
    </v-main>

    <v-snackbar
      v-model="notifStore.visible"
      :color="notifColor"
      :timeout="notifStore.timeout"
      location="bottom end"
    >
      <div class="d-flex align-center">
        <v-icon :icon="notifIcon" class="mr-2" />
        {{ notifStore.message }}
      </div>
      <template #actions>
        <v-btn icon="mdi-close" variant="text" size="small" @click="notifStore.dismiss()" />
      </template>
    </v-snackbar>
  </v-app>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { setNavigator } from '@/api/client'

const router = useRouter()
const authStore = useAuthStore()
const notifStore = useNotificationStore()
const drawer = ref(true)

setNavigator(router.push)

const notifColor = computed(() => {
  const map: Record<string, string> = { success: 'success', error: 'error', warning: 'warning', info: 'info' }
  return map[notifStore.type] ?? 'info'
})

const notifIcon = computed(() => {
  const map: Record<string, string> = { success: 'mdi-check-circle', error: 'mdi-alert-circle', warning: 'mdi-alert', info: 'mdi-information' }
  return map[notifStore.type] ?? 'mdi-information'
})

const initials = computed(() => {
  const name = authStore.username
  return name.slice(0, 2).toUpperCase()
})

interface NavItem {
  to: string
  icon: string
  title: string
  roles?: string[]
}

const navItems: NavItem[] = [
  { to: '/', icon: 'mdi-view-dashboard', title: 'Dashboard' },
  { to: '/customers', icon: 'mdi-account-group', title: 'Customers', roles: ['ADMIN', 'USER'] },
  { to: '/doctors', icon: 'mdi-doctor', title: 'Doctors', roles: ['ADMIN', 'USER'] },
  { to: '/schedules', icon: 'mdi-calendar-clock', title: 'Schedules', roles: ['ADMIN', 'USER', 'DOCTOR'] },
  { to: '/appointments', icon: 'mdi-calendar-check', title: 'Appointments', roles: ['ADMIN', 'USER', 'CUSTOMER', 'DOCTOR'] },
  { to: '/bookings', icon: 'mdi-plus-circle', title: 'Book Appointment', roles: ['ADMIN', 'USER', 'CUSTOMER'] },
  { to: '/users', icon: 'mdi-account-cog', title: 'Users', roles: ['ADMIN'] },
]

const filteredNavItems = computed(() => {
  return navItems.filter((item) => {
    if (!item.roles) return true
    return item.roles.some((role) => authStore.hasRole(role))
  })
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
