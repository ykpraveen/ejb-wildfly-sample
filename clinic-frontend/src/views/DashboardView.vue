<template>
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">Dashboard</h2>

    <v-row>
      <v-col cols="12" sm="6" md="3" v-for="card in statCards" :key="card.title">
        <v-card variant="tonal" :color="card.color" class="pa-4">
          <div class="d-flex align-center">
            <v-icon :icon="card.icon" size="40" :color="card.color" class="mr-4" />
            <div>
              <div class="text-h4 font-weight-bold">{{ card.value }}</div>
              <div class="text-body-2 text-medium-emphasis">{{ card.title }}</div>
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>

    <v-row class="mt-4">
      <v-col cols="12" md="8">
        <v-card>
          <v-card-title>Recent Appointments</v-card-title>
          <v-card-text>
            <v-data-table
              :headers="appointmentHeaders"
              :items="appointments"
              :loading="loadingAppointments"
              density="compact"
              hover
            >
              <template #item.status="{ item }">
                <v-chip :color="statusColor(item.status)" size="small" variant="flat">
                  {{ item.status }}
                </v-chip>
              </template>
            </v-data-table>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card>
          <v-card-title>Quick Actions</v-card-title>
          <v-card-text>
            <v-list density="compact">
              <v-list-item
                v-for="action in quickActions"
                :key="action.to"
                :prepend-icon="action.icon"
                :title="action.title"
                :to="action.to"
              />
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { appointmentsApi } from '@/api/appointments'
import type { Appointment } from '@/api/types'

const authStore = useAuthStore()
const appointments = ref<Appointment[]>([])
const loadingAppointments = ref(false)

const QUICK_ACTIONS = [
  { title: 'Book Appointment', icon: 'mdi-plus-circle', to: '/bookings', roles: ['ADMIN', 'USER', 'CUSTOMER'] },
  { title: 'Manage Customers', icon: 'mdi-account-group', to: '/customers', roles: ['ADMIN', 'USER'] },
  { title: 'Manage Doctors', icon: 'mdi-doctor', to: '/doctors', roles: ['ADMIN', 'USER'] },
  { title: 'Manage Schedules', icon: 'mdi-calendar-clock', to: '/schedules', roles: ['ADMIN', 'USER', 'DOCTOR'] },
]

const quickActions = computed(() =>
  QUICK_ACTIONS.filter((action) => action.roles.some((role) => authStore.hasRole(role))),
)

const statCards = computed(() => [
  { title: 'Appointments', value: appointments.value.length, icon: 'mdi-calendar-check', color: 'primary' },
  { title: 'Upcoming', value: appointments.value.filter(a => a.status === 'BOOKED').length, icon: 'mdi-clock-outline', color: 'info' },
  { title: 'Completed', value: appointments.value.filter(a => a.status === 'COMPLETED').length, icon: 'mdi-check-circle', color: 'success' },
  { title: 'Cancelled', value: appointments.value.filter(a => a.status === 'CANCELLED').length, icon: 'mdi-cancel', color: 'error' },
])

const appointmentHeaders = [
  { title: 'ID', key: 'id', width: 80 },
  { title: 'Date', key: 'appointmentDate' },
  { title: 'Time', key: 'appointmentTime' },
  { title: 'Status', key: 'status', width: 120 },
  { title: 'Notes', key: 'notes' },
]

function statusColor(status: string) {
  const map: Record<string, string> = { BOOKED: 'primary', COMPLETED: 'success', CANCELLED: 'error' }
  return map[status] ?? 'grey'
}

onMounted(async () => {
  if (!authStore.clinicId) return
  if (!authStore.hasRole('ADMIN') && !authStore.hasRole('USER')) return
  loadingAppointments.value = true
  try {
    const { data } = await appointmentsApi.list(authStore.clinicId)
    appointments.value = data.slice(0, 10)
  } catch {
    // silent — interceptor already logged
  } finally {
    loadingAppointments.value = false
  }
})
</script>
