<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h2 class="text-h5 font-weight-bold">Schedule Management</h2>
      <v-spacer />
      <v-btn
        color="primary"
        prepend-icon="mdi-plus"
        :disabled="!selectedDoctorId"
        @click="openCreateDialog"
      >
        New Schedule
      </v-btn>
    </div>

    <v-card class="mb-4">
      <v-card-text>
        <v-row align="center">
          <v-col cols="12" md="4">
            <v-select
              v-model="selectedDoctorId"
              :items="doctorsList"
              item-title="fullName"
              item-value="id"
              label="Select Doctor"
              :loading="loadingDoctors"
              prepend-inner-icon="mdi-doctor"
              clearable
              @update:model-value="onDoctorChange"
            />
          </v-col>
          <v-col cols="12" md="8">
            <v-text-field
              v-model="search"
              label="Search schedules"
              prepend-inner-icon="mdi-magnify"
              density="compact"
              hide-details
              clearable
              :disabled="!selectedDoctorId"
            />
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <v-card>
      <v-progress-linear v-if="loadingSchedules" indeterminate color="primary" />
      <v-card-text>
        <v-data-table
          :headers="headers"
          :items="filteredSchedules"
          :loading="loadingSchedules"
          density="compact"
          hover
          no-data-text="Select a doctor to view schedules"
        >
          <template #item.actions="{ item }">
            <v-icon size="small" class="mr-2" @click="openEditDialog(item)">mdi-pencil</v-icon>
            <v-icon size="small" color="error" @click="openDeleteDialog(item)">mdi-delete</v-icon>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <!-- Create Dialog -->
    <v-dialog v-model="showCreate" max-width="500" persistent>
      <v-card>
        <v-card-title>Create Schedule</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="handleCreate">
            <Field v-slot="{ field, errorMessage }" name="availableDate">
              <v-text-field v-bind="field" label="Date" type="date" :error-messages="errorMessage" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="startTime">
              <v-text-field v-bind="field" label="Start Time" type="time" :error-messages="errorMessage" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="endTime">
              <v-text-field v-bind="field" label="End Time" type="time" :error-messages="errorMessage" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="capacity">
              <v-text-field
                :model-value="field.value"
                @update:model-value="field.onChange(Number($event))"
                label="Capacity"
                type="number"
                :error-messages="errorMessage"
              />
            </Field>
            <v-card-actions>
              <v-spacer />
              <v-btn variant="text" @click="showCreate = false">Cancel</v-btn>
              <v-btn type="submit" color="primary" :loading="submittingCreate">Create</v-btn>
            </v-card-actions>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Edit Dialog -->
    <v-dialog v-model="showEdit" max-width="500" persistent>
      <v-card>
        <v-card-title>Edit Schedule</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="handleUpdate">
            <v-text-field
              :model-value="editingSchedule?.availableDate"
              label="Date"
              type="date"
              disabled
              readonly
            />
            <Field v-slot="{ field, errorMessage }" name="startTime">
              <v-text-field v-bind="field" label="Start Time" type="time" :error-messages="errorMessage" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="endTime">
              <v-text-field v-bind="field" label="End Time" type="time" :error-messages="errorMessage" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="capacity">
              <v-text-field
                :model-value="field.value"
                @update:model-value="field.onChange(Number($event))"
                label="Capacity"
                type="number"
                :error-messages="errorMessage"
              />
            </Field>
            <v-card-actions>
              <v-spacer />
              <v-btn variant="text" @click="showEdit = false">Cancel</v-btn>
              <v-btn type="submit" color="primary" :loading="submittingEdit">Save</v-btn>
            </v-card-actions>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Delete Confirm Dialog -->
    <ConfirmDialog
      ref="confirmDialogRef"
      title="Delete Schedule"
      :message="`Are you sure you want to delete schedule #${deletingSchedule?.id ?? ''}?`"
      confirm-text="Delete"
      icon="mdi-delete"
      color="error"
      @confirm="handleDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useForm, Field } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { useAsyncLoad } from '@/composables/useAsyncLoad'
import { schedulesApi } from '@/api/schedules'
import { doctorsApi } from '@/api/doctors'
import type { DoctorSchedule, Doctor } from '@/api/types'
import { scheduleFormSchema, updateScheduleSchema } from '@/validation'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const authStore = useAuthStore()
const notify = useNotificationStore()

const doctorsList = ref<Doctor[]>([])
const schedules = ref<DoctorSchedule[]>([])
const selectedDoctorId = ref<number | null>(null)
const search = ref('')
const submittingCreate = ref(false)
const submittingEdit = ref(false)
const showCreate = ref(false)
const showEdit = ref(false)
const editingSchedule = ref<DoctorSchedule | null>(null)
const deletingSchedule = ref<DoctorSchedule | null>(null)
const confirmDialogRef = ref<InstanceType<typeof ConfirmDialog> | null>(null)

const headers = [
  { title: 'ID', key: 'id', width: 60 },
  { title: 'Doctor', key: 'doctorId', width: 90 },
  { title: 'Date', key: 'availableDate' },
  { title: 'Start Time', key: 'startTime' },
  { title: 'End Time', key: 'endTime' },
  { title: 'Capacity', key: 'capacity', width: 90 },
  { title: 'Actions', key: 'actions', sortable: false, width: 100 },
]

const filteredSchedules = computed(() => {
  if (!search.value) return schedules.value
  const q = search.value.toLowerCase()
  return schedules.value.filter(
    (s) =>
      s.availableDate.toLowerCase().includes(q) ||
      s.startTime.toLowerCase().includes(q) ||
      s.endTime.toLowerCase().includes(q),
  )
})

const createForm = useForm({
  validationSchema: toTypedSchema(scheduleFormSchema),
  initialValues: { availableDate: '', startTime: '', endTime: '', capacity: 1 },
})

const editForm = useForm({
  validationSchema: toTypedSchema(updateScheduleSchema),
  initialValues: { startTime: '', endTime: '', capacity: 1 },
})

const { loading: loadingDoctors, run: loadDoctors } = useAsyncLoad(async () => {
  if (!authStore.clinicId) return
  const { data } = await doctorsApi.list(authStore.clinicId)
  doctorsList.value = data
}, 'Failed to load doctors')

const { loading: loadingSchedules, run: loadSchedules } = useAsyncLoad(async () => {
  if (!authStore.clinicId || !selectedDoctorId.value) return
  const { data } = await schedulesApi.list(selectedDoctorId.value, authStore.clinicId)
  schedules.value = data
}, 'Failed to load schedules', false)

function onDoctorChange() {
  search.value = ''
  schedules.value = []
  if (selectedDoctorId.value) {
    loadSchedules()
  }
}

function openCreateDialog() {
  createForm.resetForm()
  showCreate.value = true
}

function openEditDialog(schedule: DoctorSchedule) {
  editingSchedule.value = schedule
  editForm.resetForm({
    values: {
      startTime: schedule.startTime,
      endTime: schedule.endTime,
      capacity: schedule.capacity,
    },
  })
  showEdit.value = true
}

function openDeleteDialog(schedule: DoctorSchedule) {
  deletingSchedule.value = schedule
  confirmDialogRef.value?.open()
}

const handleCreate = createForm.handleSubmit(async (values) => {
  if (!selectedDoctorId.value || !authStore.clinicId) return
  submittingCreate.value = true
  try {
    await schedulesApi.create(selectedDoctorId.value, {
      clinicId: authStore.clinicId,
      doctorId: selectedDoctorId.value,
      ...values,
    })
    showCreate.value = false
    notify.success('Schedule created')
    await loadSchedules()
  } catch {
    notify.error('Failed to create schedule')
  } finally {
    submittingCreate.value = false
  }
})

const handleUpdate = editForm.handleSubmit(async (values) => {
  if (!selectedDoctorId.value || !authStore.clinicId || !editingSchedule.value) return
  submittingEdit.value = true
  try {
    await schedulesApi.update(
      selectedDoctorId.value,
      editingSchedule.value.id,
      authStore.clinicId,
      values,
    )
    showEdit.value = false
    notify.success('Schedule updated')
    await loadSchedules()
  } catch {
    notify.error('Failed to update schedule')
  } finally {
    submittingEdit.value = false
  }
})

async function handleDelete() {
  if (!selectedDoctorId.value || !authStore.clinicId || !deletingSchedule.value) return
  try {
    await schedulesApi.remove(selectedDoctorId.value, deletingSchedule.value.id, authStore.clinicId)
    confirmDialogRef.value?.close()
    notify.success('Schedule deleted')
    await loadSchedules()
  } catch {
    notify.error('Failed to delete schedule')
    confirmDialogRef.value?.close()
  }
}

</script>
