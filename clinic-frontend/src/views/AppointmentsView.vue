<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h2 class="text-h5 font-weight-bold">Appointments</h2>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" to="/bookings">New Booking</v-btn>
    </div>

    <v-card>
      <v-card-text>
        <div class="d-flex align-center ga-4 mb-4">
          <v-text-field
            v-model="search"
            prepend-inner-icon="mdi-magnify"
            label="Search by date, time or notes"
            density="compact"
            hide-details
            clearable
            class="flex-grow-1"
            style="max-width: 360px"
          />
          <v-chip-group v-model="statusFilter" column>
            <v-chip filter variant="outlined" value="ALL">All</v-chip>
            <v-chip filter variant="outlined" value="BOOKED" color="primary">Booked</v-chip>
            <v-chip filter variant="outlined" value="COMPLETED" color="success">Completed</v-chip>
            <v-chip filter variant="outlined" value="CANCELLED" color="error">Cancelled</v-chip>
          </v-chip-group>
        </div>

        <v-data-table
          :headers="headers"
          :items="filteredAppointments"
          :loading="loading"
          density="compact"
          hover
        >
          <template #item.customerId="{ item }">
            {{ customerName(item.customerId) }}
          </template>
          <template #item.doctorId="{ item }">
            {{ doctorName(item.doctorId) }}
          </template>
          <template #item.status="{ item }">
            <v-chip :color="statusColor(item.status)" size="small" variant="flat">
              {{ item.status }}
            </v-chip>
          </template>
          <template #item.rescheduleCount="{ item }">
            {{ item.rescheduleCount }}
          </template>
          <template #item.actions="{ item }">
            <template v-if="item.status === 'BOOKED'">
              <v-btn
                icon="mdi-calendar-clock"
                size="small"
                variant="text"
                color="primary"
                @click="openReschedule(item)"
              />
              <v-btn
                icon="mdi-cancel"
                size="small"
                variant="text"
                color="error"
                :loading="actionLoading === `cancel-${item.id}`"
                @click="confirmCancel(item)"
              />
            </template>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <ConfirmDialog
      ref="cancelDialogRef"
      title="Cancel Appointment"
      :message="`Cancel appointment #${cancelTarget?.id}?`"
      confirm-text="Cancel Appointment"
      color="error"
      @confirm="executeCancel"
    />

    <v-dialog v-model="rescheduleDialog" max-width="420">
      <v-card>
        <v-card-title>Reschedule Appointment</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="executeReschedule">
            <v-text-field
              v-model="rescheduleForm.newTime"
              label="New Time"
              type="time"
              :error-messages="rescheduleErrors.newTime"
              required
            />
            <v-btn type="submit" color="primary" :loading="actionLoading === 'reschedule'" class="mt-2">
              Reschedule
            </v-btn>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { appointmentsApi } from '@/api/appointments'
import { customersApi } from '@/api/customers'
import { doctorsApi } from '@/api/doctors'
import type { Appointment } from '@/api/types'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { rescheduleSchema } from '@/validation'

const authStore = useAuthStore()
const notify = useNotificationStore()

const appointments = ref<Appointment[]>([])
const loading = ref(false)
const actionLoading = ref<string | null>(null)
const search = ref('')
const statusFilter = ref('ALL')

const customerMap = ref<Map<number, string>>(new Map())
const doctorMap = ref<Map<number, string>>(new Map())

const cancelDialogRef = ref<InstanceType<typeof ConfirmDialog> | null>(null)
const cancelTarget = ref<Appointment | null>(null)

const rescheduleDialog = ref(false)
const rescheduleTarget = ref<Appointment | null>(null)
const rescheduleForm = ref({ newTime: '' })
const rescheduleErrors = ref<Record<string, string>>({})

const headers = [
  { title: 'ID', key: 'id', width: 60 },
  { title: 'Customer', key: 'customerId', width: 150 },
  { title: 'Doctor', key: 'doctorId', width: 150 },
  { title: 'Date', key: 'appointmentDate', width: 120 },
  { title: 'Time', key: 'appointmentTime', width: 100 },
  { title: 'Status', key: 'status', width: 110 },
  { title: 'Reschedules', key: 'rescheduleCount', width: 110, align: 'center' as const },
  { title: 'Notes', key: 'notes' },
  { title: 'Actions', key: 'actions', width: 100, sortable: false },
]

function statusColor(status: string) {
  const map: Record<string, string> = { BOOKED: 'primary', COMPLETED: 'success', CANCELLED: 'error' }
  return map[status] ?? 'grey'
}

function customerName(id: number) {
  return customerMap.value.get(id) ?? `#${id}`
}

function doctorName(id: number) {
  return doctorMap.value.get(id) ?? `#${id}`
}

const filteredAppointments = computed(() => {
  let items = appointments.value
  if (statusFilter.value !== 'ALL') {
    items = items.filter((a) => a.status === statusFilter.value)
  }
  if (search.value) {
    const q = search.value.toLowerCase()
    items = items.filter(
      (a) =>
        a.appointmentDate.toLowerCase().includes(q) ||
        a.appointmentTime.toLowerCase().includes(q) ||
        (a.notes && a.notes.toLowerCase().includes(q)),
    )
  }
  return items
})

async function loadData() {
  if (!authStore.clinicId) return
  loading.value = true
  try {
    const [appointmentsRes, customersRes, doctorsRes] = await Promise.all([
      appointmentsApi.list(authStore.clinicId),
      customersApi.list(authStore.clinicId),
      doctorsApi.list(authStore.clinicId),
    ])
    appointments.value = appointmentsRes.data
    customerMap.value = new Map(customersRes.data.map((c) => [c.id, c.fullName]))
    doctorMap.value = new Map(doctorsRes.data.map((d) => [d.id, d.fullName]))
  } catch {
    /* interceptor logged */
  } finally {
    loading.value = false
  }
}

function confirmCancel(item: Appointment) {
  cancelTarget.value = item
  cancelDialogRef.value?.open()
}

async function executeCancel() {
  if (!cancelTarget.value || !authStore.clinicId) return
  const id = cancelTarget.value.id
  actionLoading.value = `cancel-${id}`
  try {
    await appointmentsApi.cancel(id, authStore.clinicId)
    notify.success(`Appointment #${id} cancelled`)
    await loadData()
  } catch {
    notify.error(`Failed to cancel appointment #${id}`)
  } finally {
    actionLoading.value = null
    cancelDialogRef.value?.close()
    cancelTarget.value = null
  }
}

function openReschedule(item: Appointment) {
  rescheduleTarget.value = item
  rescheduleForm.value = { newTime: '' }
  rescheduleErrors.value = {}
  rescheduleDialog.value = true
}

async function executeReschedule() {
  rescheduleErrors.value = {}
  const result = rescheduleSchema.safeParse(rescheduleForm.value)
  if (!result.success) {
    for (const issue of result.error.issues) {
      const field = issue.path[0] as string
      rescheduleErrors.value[field] = issue.message
    }
    return
  }
  if (!rescheduleTarget.value || !authStore.clinicId) return
  const id = rescheduleTarget.value.id
  actionLoading.value = 'reschedule'
  try {
    await appointmentsApi.reschedule(id, authStore.clinicId, { newTime: result.data.newTime })
    notify.success(`Appointment #${id} rescheduled`)
    rescheduleDialog.value = false
    await loadData()
  } catch {
    notify.error(`Failed to reschedule appointment #${id}`)
  } finally {
    actionLoading.value = null
  }
}

onMounted(loadData)
</script>
