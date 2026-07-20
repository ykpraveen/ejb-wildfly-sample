<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h2 class="text-h5 font-weight-bold">Book an Appointment</h2>
      <v-spacer />
      <v-btn v-if="store.sessionId" variant="text" color="error" prepend-icon="mdi-close" @click="handleCancel">
        Cancel Session
      </v-btn>
    </div>

    <v-card elevation="2">
      <v-progress-linear :model-value="store.progress" color="primary" height="4" />

      <v-stepper
        :model-value="store.stepIndex + 1"
        :items="stepperItems"
        alt-labels
        flat
        hide-actions
      >
        <template #item.1>
          <v-card variant="flat" class="pa-4">
            <h3 class="text-h6 mb-3">
              <v-icon icon="mdi-account-search" class="mr-1" />
              Select Customer
            </h3>
            <v-autocomplete
              v-model="customerId"
              :items="store.customers"
              item-title="fullName"
              item-value="id"
              label="Search customer by name..."
              prepend-inner-icon="mdi-magnify"
              variant="outlined"
              :loading="store.loading"
            />
          </v-card>
        </template>

        <template #item.2>
          <v-card variant="flat" class="pa-4">
            <h3 class="text-h6 mb-3">
              <v-icon icon="mdi-doctor" class="mr-1" />
              Select Doctor
            </h3>
            <v-row v-if="store.doctors.length">
              <v-col v-for="doc in store.doctors" :key="doc.id" cols="12" sm="6" md="4">
                <v-card
                  :color="selectedDoctorId === doc.id ? 'primary' : undefined"
                  :variant="selectedDoctorId === doc.id ? 'flat' : 'outlined'"
                  class="cursor-pointer h-100"
                  @click="handleDoctorSelect(doc.id)"
                >
                  <v-card-title class="text-body-1">
                    <v-icon icon="mdi-account" class="mr-1" />
                    {{ doc.fullName }}
                  </v-card-title>
                  <v-card-subtitle>{{ doc.specialty }}</v-card-subtitle>
                  <v-card-text class="text-caption text-medium-emphasis">
                    <v-icon icon="mdi-clock-outline" size="small" class="mr-1" />
                    {{ doc.slotMinutes }}-min slots
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
            <v-alert v-else type="info" variant="tonal">No active doctors available.</v-alert>
          </v-card>
        </template>

        <template #item.3>
          <v-card variant="flat" class="pa-4">
            <h3 class="text-h6 mb-3">
              <v-icon icon="mdi-calendar" class="mr-1" />
              Select Date
            </h3>
            <v-row v-if="store.schedules.length">
              <v-col v-for="sched in store.schedules" :key="sched.id" cols="12" sm="6" md="4">
                <v-card
                  :color="selectedScheduleId === sched.id ? 'primary' : undefined"
                  :variant="selectedScheduleId === sched.id ? 'flat' : 'outlined'"
                  class="cursor-pointer h-100"
                  @click="handleScheduleSelect(sched.id)"
                >
                  <v-card-title class="text-body-1">
                    <v-icon icon="mdi-calendar-account" class="mr-1" />
                    {{ formatDate(sched.availableDate) }}
                  </v-card-title>
                  <v-card-text>
                    <div class="text-caption">
                      <v-icon icon="mdi-clock" size="small" class="mr-1" />
                      {{ sched.startTime }} – {{ sched.endTime }}
                    </div>
                    <div class="text-caption text-medium-emphasis mt-1">
                      <v-icon icon="mdi-account-group" size="small" class="mr-1" />
                      {{ sched.capacity }} slots available
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
            <v-alert v-else type="info" variant="tonal">No schedules available for this doctor.</v-alert>
          </v-card>
        </template>

        <template #item.4>
          <v-card variant="flat" class="pa-4">
            <h3 class="text-h6 mb-3">
              <v-icon icon="mdi-clock-edit" class="mr-1" />
              Select Time
            </h3>
            <v-alert v-if="store.selectedSchedule" type="info" variant="tonal" class="mb-4">
              Available window: <strong>{{ store.selectedSchedule.startTime }}</strong> to
              <strong>{{ store.selectedSchedule.endTime }}</strong>
              ({{ store.selectedSchedule.capacity }} slots)
            </v-alert>
            <v-text-field
              v-model="appointmentTime"
              label="Appointment time"
              type="time"
              variant="outlined"
              prepend-inner-icon="mdi-clock"
              :min="store.timeMin"
              :max="store.timeMax"
              :rules="timeRules"
              class="max-w-300"
            />
            <v-textarea
              :model-value="notesText"
              @update:model-value="handleNotesInput"
              label="Notes (optional)"
              variant="outlined"
              rows="2"
              auto-grow
              class="mt-4"
            />
          </v-card>
        </template>

        <template #item.5>
          <v-card variant="flat" class="pa-4">
            <h3 class="text-h6 mb-3">
              <v-icon icon="mdi-check-circle" class="mr-1" />
              Confirm Booking
            </h3>
            <v-card variant="tonal" class="mb-4">
              <v-card-text>
                <v-row density="comfortable">
                  <v-col cols="12" sm="6">
                    <div class="text-caption text-medium-emphasis">Customer</div>
                    <div class="text-body-1">{{ customerName }}</div>
                  </v-col>
                  <v-col cols="12" sm="6">
                    <div class="text-caption text-medium-emphasis">Doctor</div>
                    <div class="text-body-1">{{ store.summary?.doctorName }}</div>
                  </v-col>
                  <v-col cols="12" sm="6">
                    <div class="text-caption text-medium-emphasis">Date</div>
                    <div class="text-body-1">{{ formatDate(store.summary?.scheduleDate ?? '') }}</div>
                  </v-col>
                  <v-col cols="12" sm="6">
                    <div class="text-caption text-medium-emphasis">Time</div>
                    <div class="text-body-1">{{ store.selectedTime }}</div>
                  </v-col>
                  <v-col v-if="store.notes" cols="12">
                    <div class="text-caption text-medium-emphasis">Notes</div>
                    <div class="text-body-1">{{ store.notes }}</div>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-card>
        </template>
      </v-stepper>

      <v-divider />

      <v-card-actions class="pa-4">
        <v-btn v-if="store.canGoBack" variant="text" prepend-icon="mdi-arrow-left" @click="store.goBack()">
          Back
        </v-btn>
        <v-spacer />
        <v-btn v-if="store.currentStep === 'time'" color="primary" :disabled="!canConfirmTime" :loading="store.loading" @click="handleTimeConfirm">
          Continue
        </v-btn>
        <v-btn v-if="store.currentStep === 'confirm'" color="success" prepend-icon="mdi-check" :loading="store.confirming" @click="handleFinalConfirm">
          Confirm Booking
        </v-btn>
      </v-card-actions>
    </v-card>

    <v-snackbar v-model="snackbar.show" :color="snackbar.color" timeout="3000" location="bottom end">
      {{ snackbar.message }}
      <template #actions>
        <v-btn icon="mdi-close" variant="text" size="small" @click="snackbar.show = false" />
      </template>
    </v-snackbar>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useBookingStore } from '@/stores/booking'
import { useNotificationStore } from '@/stores/notification'
import { customersApi } from '@/api/customers'

const router = useRouter()
const authStore = useAuthStore()
const store = useBookingStore()
const notif = useNotificationStore()

const customerId = ref<number | null>(null)
const selectedDoctorId = ref<number | null>(null)
const selectedScheduleId = ref<number | null>(null)
const appointmentTime = ref('')
const notesText = ref('')

watch(customerId, async (newVal) => {
  if (newVal && authStore.clinicId) {
    const ok = await store.selectCustomer(newVal, authStore.clinicId)
    if (!ok) showMsg('Failed to start booking session', 'error')
  }
})

const snackbar = ref({ show: false, message: '', color: 'success' })

const stepperItems = [
  { title: 'Customer', subtitle: 'Who is booking', value: 1 },
  { title: 'Doctor', subtitle: 'Choose a doctor', value: 2 },
  { title: 'Date', subtitle: 'Pick a date', value: 3 },
  { title: 'Time', subtitle: 'Select time', value: 4 },
  { title: 'Confirm', subtitle: 'Review & book', value: 5 },
]

const customerName = computed(() =>
  store.customers.find((c) => c.id === store.selectedCustomerId)?.fullName ?? '',
)

const timeRules = [
  (v: string) => !!v || 'Time is required',
  (v: string) => v >= store.timeMin || `Must be at or after ${store.timeMin}`,
  (v: string) => v <= store.timeMax || `Must be at or before ${store.timeMax}`,
]

const canConfirmTime = computed(() =>
  !!appointmentTime.value &&
  appointmentTime.value >= store.timeMin &&
  appointmentTime.value <= store.timeMax,
)

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  try {
    return new Date(dateStr + 'T00:00:00').toLocaleDateString('en-US', {
      weekday: 'short', year: 'numeric', month: 'short', day: 'numeric',
    })
  } catch {
    return dateStr
  }
}

function showMsg(msg: string, color = 'success') {
  snackbar.value = { show: true, message: msg, color }
}

function showNotif(msg: string, type: 'success' | 'error' = 'success') {
  if (type === 'error') notif.error(msg)
  else notif.success(msg)
}

onMounted(async () => {
  if (!authStore.clinicId) return
  const { data } = await customersApi.list(authStore.clinicId)
  store.loadCustomers(
    data.map((c) => ({ id: c.id, fullName: `${c.fullName} (${c.username})` })),
  )
})

async function handleDoctorSelect(doctorId: number) {
  selectedDoctorId.value = doctorId
  const ok = await store.selectDoctor(doctorId, authStore.clinicId)
  if (!ok) showMsg('Failed to load schedules', 'error')
}

async function handleScheduleSelect(scheduleId: number) {
  selectedScheduleId.value = scheduleId
  const ok = await store.selectSchedule(scheduleId)
  if (!ok) showMsg('Failed to select date', 'error')
}

function handleNotesInput(val: string) {
  notesText.value = val
  store.updateNotes(val)
}

async function handleTimeConfirm() {
  if (!canConfirmTime.value) return
  const ok = await store.selectTime(appointmentTime.value)
  if (!ok) {
    showMsg('Failed to select time', 'error')
    return
  }
  showNotif('Time selected — review your booking below')
}

async function handleFinalConfirm() {
  const ok = await store.confirmBooking()
  if (!ok) {
    showMsg('Booking failed — please try again', 'error')
    return
  }
  showNotif('Appointment booked successfully!')
  store.reset()
  customerId.value = null
  selectedDoctorId.value = null
  selectedScheduleId.value = null
  appointmentTime.value = ''
  notesText.value = ''
  router.push('/appointments')
}

async function handleCancel() {
  await store.cancelSession()
  customerId.value = null
  selectedDoctorId.value = null
  selectedScheduleId.value = null
  appointmentTime.value = ''
  notesText.value = ''
  showMsg('Booking session cancelled', 'warning')
}
</script>

<style scoped>
.cursor-pointer { cursor: pointer; }
.max-w-300 { max-width: 300px; }
</style>
