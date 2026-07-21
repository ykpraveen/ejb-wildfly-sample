import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { bookingsApi } from '@/api/bookings'
import { doctorsApi } from '@/api/doctors'
import { schedulesApi } from '@/api/schedules'
import type { Doctor, DoctorSchedule, BookingSummary } from '@/api/types'

export type WizardStep = 'customer' | 'doctor' | 'schedule' | 'time' | 'confirm'

const STEPS: WizardStep[] = ['customer', 'doctor', 'schedule', 'time', 'confirm']

export const useBookingStore = defineStore('booking', () => {
  const currentStep = ref<WizardStep>('customer')
  const sessionId = ref('')
  const loading = ref(false)
  const confirming = ref(false)

  const customers = ref<{ id: number; fullName: string }[]>([])
  const doctors = ref<Doctor[]>([])
  const schedules = ref<DoctorSchedule[]>([])
  const summary = ref<BookingSummary | null>(null)

  const selectedCustomerId = ref<number | null>(null)
  const selectedDoctorId = ref<number | null>(null)
  const selectedScheduleId = ref<number | null>(null)
  const selectedTime = ref('')
  const notes = ref('')

  const stepIndex = computed(() => STEPS.indexOf(currentStep.value))
  const canGoBack = computed(() => stepIndex.value > 0)
  const progress = computed(() => ((stepIndex.value + 1) / STEPS.length) * 100)

  const selectedSchedule = computed(() =>
    schedules.value.find((s) => s.id === selectedScheduleId.value) ?? null,
  )

  const timeMin = computed(() => selectedSchedule.value?.startTime ?? '00:00')
  const timeMax = computed(() => selectedSchedule.value?.endTime ?? '23:59')

  function goBack() {
    if (!canGoBack.value) return
    const idx = stepIndex.value - 1
    currentStep.value = STEPS[idx]
  }

  function goToStep(step: WizardStep) {
    const targetIdx = STEPS.indexOf(step)
    if (targetIdx < stepIndex.value) {
      currentStep.value = step
    }
  }

  async function loadCustomers(list: { id: number; fullName: string }[]) {
    customers.value = list
  }

  async function selectCustomer(customerId: number, clinicId: number): Promise<boolean> {
    loading.value = true
    try {
      selectedCustomerId.value = customerId
      const { data } = await bookingsApi.start({ clinicId, customerId })
      sessionId.value = data.sessionId
      const { data: docs } = await doctorsApi.list(clinicId)
      doctors.value = docs.filter((d) => d.active)
      currentStep.value = 'doctor'
      return true
    } catch {
      return false
    } finally {
      loading.value = false
    }
  }

  async function selectDoctor(doctorId: number, clinicId: number): Promise<boolean> {
    loading.value = true
    try {
      selectedDoctorId.value = doctorId
      await bookingsApi.selectDoctor(sessionId.value, doctorId)
      const { data: scheds } = await schedulesApi.list(doctorId, clinicId)
      schedules.value = scheds
      selectedScheduleId.value = null
      selectedTime.value = ''
      currentStep.value = 'schedule'
      return true
    } catch {
      return false
    } finally {
      loading.value = false
    }
  }

  async function selectSchedule(scheduleId: number): Promise<boolean> {
    loading.value = true
    try {
      selectedScheduleId.value = scheduleId
      await bookingsApi.selectSchedule(sessionId.value, scheduleId)
      selectedTime.value = ''
      currentStep.value = 'time'
      return true
    } catch {
      return false
    } finally {
      loading.value = false
    }
  }

  async function selectTime(time: string): Promise<boolean> {
    loading.value = true
    try {
      selectedTime.value = time
      await bookingsApi.selectTime(sessionId.value, time)
      await bookingsApi.addNotes(sessionId.value, notes.value)
      const { data } = await bookingsApi.getSummary(sessionId.value)
      summary.value = data
      currentStep.value = 'confirm'
      return true
    } catch {
      return false
    } finally {
      loading.value = false
    }
  }

  function updateNotes(notesText: string): void {
    notes.value = notesText
  }

  async function confirmBooking(): Promise<boolean> {
    confirming.value = true
    try {
      await bookingsApi.confirm(sessionId.value)
      return true
    } catch {
      return false
    } finally {
      confirming.value = false
    }
  }

  async function cancelSession(): Promise<void> {
    if (sessionId.value) {
      try { await bookingsApi.cancel(sessionId.value) } catch { /* silent */ }
    }
    reset()
  }

  function reset() {
    currentStep.value = 'customer'
    sessionId.value = ''
    loading.value = false
    confirming.value = false
    doctors.value = []
    schedules.value = []
    summary.value = null
    selectedCustomerId.value = null
    selectedDoctorId.value = null
    selectedScheduleId.value = null
    selectedTime.value = ''
    notes.value = ''
  }

  return {
    currentStep, sessionId, loading, confirming,
    customers, doctors, schedules, summary,
    selectedCustomerId, selectedDoctorId, selectedScheduleId, selectedTime, notes,
    stepIndex, canGoBack, progress, selectedSchedule, timeMin, timeMax,
    goBack, goToStep, loadCustomers,
    selectCustomer, selectDoctor, selectSchedule, selectTime,
    updateNotes, confirmBooking, cancelSession, reset,
  }
})
