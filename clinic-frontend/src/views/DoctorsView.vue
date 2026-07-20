<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h2 class="text-h5 font-weight-bold">Doctor Management</h2>
      <v-spacer />
      <v-text-field
        v-model="search"
        prepend-inner-icon="mdi-magnify"
        label="Search doctors..."
        single-line
        hide-details
        density="compact"
        class="mr-4"
        style="max-width: 320px"
        variant="outlined"
      />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreateDialog">New Doctor</v-btn>
    </div>

    <v-card>
      <v-card-text>
        <v-data-table
          :headers="headers"
          :items="filteredDoctors"
          :loading="loading"
          density="compact"
          hover
        >
          <template #item.active="{ item }">
            <v-chip :color="item.active ? 'success' : 'warning'" size="small" variant="flat">
              {{ item.active ? 'Active' : 'Inactive' }}
            </v-chip>
          </template>
          <template #item.actions="{ item }">
            <v-icon size="small" class="mr-2" color="primary" @click="openEditDialog(item)">
              mdi-pencil
            </v-icon>
            <v-icon size="small" color="error" @click="openDeleteDialog(item)">
              mdi-delete
            </v-icon>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <!-- Create / Edit Dialog -->
    <v-dialog v-model="dialogOpen" max-width="500" persistent>
      <v-card>
        <v-card-title class="text-h6">{{ isEditing ? 'Edit Doctor' : 'Create Doctor' }}</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="onSubmit">
            <Field v-slot="{ field, errorMessage }" name="fullName">
              <v-text-field v-bind="field" label="Full Name" :error-messages="errorMessage" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="username">
              <v-text-field
                v-bind="field"
                label="Username"
                :disabled="isEditing"
                :error-messages="errorMessage"
              />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="specialty">
              <v-text-field v-bind="field" label="Specialty" :error-messages="errorMessage" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="slotMinutes">
              <v-text-field
                v-bind="field"
                v-model.number="field.value"
                label="Slot Minutes"
                type="number"
                :error-messages="errorMessage"
              />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="active" type="checkbox">
              <v-switch
                :model-value="field.value"
                @update:model-value="field.onChange"
                label="Active"
                color="primary"
                :error-messages="errorMessage"
                hide-details
              />
            </Field>
            <div class="d-flex justify-end gap-2 mt-2">
              <v-btn variant="text" @click="closeDialog">Cancel</v-btn>
              <v-btn type="submit" color="primary" :loading="submitting">
                {{ isEditing ? 'Save' : 'Create' }}
              </v-btn>
            </div>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Delete Confirm Dialog -->
    <ConfirmDialog
      ref="confirmDialogRef"
      title="Delete Doctor"
      :message="`Are you sure you want to delete '${doctorToDelete?.fullName ?? ''}'? This action cannot be undone.`"
      confirm-text="Delete"
      color="error"
      @confirm="handleDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useForm, Field } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { doctorsApi } from '@/api/doctors'
import type { Doctor } from '@/api/types'
import { createDoctorSchema } from '@/validation'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const authStore = useAuthStore()
const notify = useNotificationStore()

const doctors = ref<Doctor[]>([])
const loading = ref(false)
const search = ref('')

// --- Table ---
const headers = [
  { title: 'ID', key: 'id', width: 60 },
  { title: 'Full Name', key: 'fullName' },
  { title: 'Username', key: 'username' },
  { title: 'Specialty', key: 'specialty' },
  { title: 'Slot (min)', key: 'slotMinutes', width: 100 },
  { title: 'Active', key: 'active', width: 100 },
  { title: 'Actions', key: 'actions', sortable: false, width: 100 },
]

const filteredDoctors = computed(() => {
  if (!search.value) return doctors.value
  const q = search.value.toLowerCase()
  return doctors.value.filter(
    (d) =>
      d.fullName.toLowerCase().includes(q) ||
      d.username.toLowerCase().includes(q) ||
      d.specialty.toLowerCase().includes(q),
  )
})

async function loadDoctors() {
  if (!authStore.clinicId) return
  loading.value = true
  try {
    const { data } = await doctorsApi.list(authStore.clinicId)
    doctors.value = data
  } catch {
    notify.error('Failed to load doctors')
  } finally {
    loading.value = false
  }
}

// --- VeeValidate form (shared for create & edit) ---
const createForm = useForm({
  validationSchema: toTypedSchema(createDoctorSchema),
})

const dialogOpen = ref(false)
const isEditing = ref(false)
const submitting = ref(false)
const editingDoctor = ref<Doctor | null>(null)

function openCreateDialog() {
  isEditing.value = false
  editingDoctor.value = null
  createForm.resetForm({
    values: { fullName: '', username: '', specialty: '', slotMinutes: 30, active: true },
  })
  dialogOpen.value = true
}

function openEditDialog(doctor: Doctor) {
  isEditing.value = true
  editingDoctor.value = doctor
  createForm.resetForm({
    values: {
      fullName: doctor.fullName,
      username: doctor.username,
      specialty: doctor.specialty,
      slotMinutes: doctor.slotMinutes,
      active: doctor.active,
    },
  })
  dialogOpen.value = true
}

function closeDialog() {
  dialogOpen.value = false
  editingDoctor.value = null
}

const onSubmit = createForm.handleSubmit(async (values) => {
  submitting.value = true
  try {
    if (isEditing.value && editingDoctor.value) {
      const { username: _, ...payload } = values
      await doctorsApi.update(editingDoctor.value.id, authStore.clinicId, payload)
      notify.success('Doctor updated successfully')
    } else {
      await doctorsApi.create({ clinicId: authStore.clinicId, ...values })
      notify.success('Doctor created successfully')
    }
    closeDialog()
    await loadDoctors()
  } catch {
    notify.error(isEditing.value ? 'Failed to update doctor' : 'Failed to create doctor')
  } finally {
    submitting.value = false
  }
})

// --- Delete ---
const confirmDialogRef = ref<InstanceType<typeof ConfirmDialog> | null>(null)
const doctorToDelete = ref<Doctor | null>(null)

function openDeleteDialog(doctor: Doctor) {
  doctorToDelete.value = doctor
  confirmDialogRef.value?.open()
}

async function handleDelete() {
  if (!doctorToDelete.value) return
  try {
    await doctorsApi.remove(doctorToDelete.value.id, authStore.clinicId)
    notify.success('Doctor deleted successfully')
    await loadDoctors()
  } catch {
    notify.error('Failed to delete doctor')
  } finally {
    confirmDialogRef.value?.close()
    doctorToDelete.value = null
  }
}

onMounted(loadDoctors)
</script>
