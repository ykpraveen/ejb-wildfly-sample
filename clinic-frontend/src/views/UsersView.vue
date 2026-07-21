<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h2 class="text-h5 font-weight-bold">User Management</h2>
      <v-spacer />
      <v-text-field
        v-model="search"
        prepend-inner-icon="mdi-magnify"
        label="Search users..."
        single-line
        hide-details
        density="compact"
        class="mr-4"
        style="max-width: 320px"
        variant="outlined"
      />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreateDialog">New User</v-btn>
    </div>

    <v-card>
      <v-card-text>
        <v-data-table
          :headers="headers"
          :items="filteredUsers"
          :loading="loading"
          density="compact"
          hover
        >
          <template #item.roles="{ item }">
            <v-chip
              v-for="role in item.roles"
              :key="role"
              size="small"
              variant="flat"
              :color="roleColor(role)"
              class="mr-1"
            >
              {{ role }}
            </v-chip>
          </template>
          <template #item.active="{ item }">
            <v-chip :color="item.active ? 'success' : 'warning'" size="small" variant="flat">
              {{ item.active ? 'Active' : 'Inactive' }}
            </v-chip>
          </template>
          <template #item.actions="{ item }">
            <v-btn
              v-if="!item.active"
              size="small"
              variant="flat"
              color="success"
              prepend-icon="mdi-check"
              @click="openConfirmDialog(item, 'activate')"
            >
              Activate
            </v-btn>
            <v-btn
              v-else
              size="small"
              variant="flat"
              color="warning"
              prepend-icon="mdi-cancel"
              @click="openConfirmDialog(item, 'deactivate')"
            >
              Deactivate
            </v-btn>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <!-- Create User Dialog -->
    <v-dialog v-model="dialogOpen" max-width="500" persistent>
      <v-card>
        <v-card-title class="text-h6">Create User</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="onSubmit">
            <Field v-slot="{ field, errorMessage }" name="username">
              <v-text-field v-bind="field" label="Username" :error-messages="errorMessage" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="password">
              <v-text-field v-bind="field" label="Password" type="password" :error-messages="errorMessage" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="roles">
              <v-select
                v-bind="field"
                :items="roleOptions"
                label="Roles"
                multiple
                chips
                :error-messages="errorMessage"
              />
            </Field>
            <div class="d-flex justify-end gap-2 mt-2">
              <v-btn variant="text" @click="closeDialog">Cancel</v-btn>
              <v-btn type="submit" color="primary" :loading="submitting">Create</v-btn>
            </div>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Activate / Deactivate Confirm Dialog -->
    <ConfirmDialog
      ref="confirmDialogRef"
      :title="confirmAction === 'activate' ? 'Activate User' : 'Deactivate User'"
      :message="confirmAction === 'activate'
        ? `Are you sure you want to activate user '${userToConfirm?.username}'?`
        : `Are you sure you want to deactivate user '${userToConfirm?.username}'? They will not be able to log in.`"
      :confirm-text="confirmAction === 'activate' ? 'Activate' : 'Deactivate'"
      :icon="confirmAction === 'activate' ? 'mdi-check-circle' : 'mdi-cancel'"
      :color="confirmAction === 'activate' ? 'success' : 'warning'"
      @confirm="handleConfirmAction"
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
import { usersApi } from '@/api/users'
import type { User } from '@/api/types'
import { createUserSchema } from '@/validation'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const authStore = useAuthStore()
const notify = useNotificationStore()

const users = ref<User[]>([])
const submitting = ref(false)
const search = ref('')
const dialogOpen = ref(false)

const confirmDialogRef = ref<InstanceType<typeof ConfirmDialog> | null>(null)
const userToConfirm = ref<User | null>(null)
const confirmAction = ref<'activate' | 'deactivate'>('activate')

const roleOptions = ['ADMIN', 'USER', 'DOCTOR', 'CUSTOMER']

const headers = [
  { title: 'ID', key: 'id', width: 60 },
  { title: 'Username', key: 'username' },
  { title: 'Roles', key: 'roles' },
  { title: 'Active', key: 'active', width: 100 },
  { title: 'Actions', key: 'actions', sortable: false, width: 120 },
]

const filteredUsers = computed(() => {
  if (!search.value) return users.value
  const q = search.value.toLowerCase()
  return users.value.filter(
    (u) =>
      u.username.toLowerCase().includes(q) ||
      u.roles.some((r) => r.toLowerCase().includes(q)),
  )
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

const { loading, run: loadUsers } = useAsyncLoad(async () => {
  if (!authStore.clinicId) return
  const { data } = await usersApi.list(authStore.clinicId)
  users.value = data
}, 'Failed to load users')

// --- VeeValidate form ---
const { handleSubmit, resetForm, errors } = useForm({
  validationSchema: toTypedSchema(createUserSchema),
  initialValues: { username: '', password: '', roles: [] as string[] },
})

function openCreateDialog() {
  resetForm({ values: { username: '', password: '', roles: [] } })
  dialogOpen.value = true
}

function closeDialog() {
  dialogOpen.value = false
}

const onSubmit = handleSubmit(async (values) => {
  submitting.value = true
  try {
    await usersApi.create({ clinicId: authStore.clinicId, ...values })
    notify.success('User created successfully')
    closeDialog()
    await loadUsers()
  } catch {
    notify.error('Failed to create user')
  } finally {
    submitting.value = false
  }
})

// --- Activate / Deactivate ---
function openConfirmDialog(user: User, action: 'activate' | 'deactivate') {
  userToConfirm.value = user
  confirmAction.value = action
  confirmDialogRef.value?.open()
}

async function handleConfirmAction() {
  if (!userToConfirm.value) return
  try {
    if (confirmAction.value === 'activate') {
      await usersApi.activate(userToConfirm.value.id, { clinicId: authStore.clinicId })
      notify.success(`User '${userToConfirm.value.username}' activated`)
    } else {
      await usersApi.deactivate(userToConfirm.value.id, { clinicId: authStore.clinicId })
      notify.success(`User '${userToConfirm.value.username}' deactivated`)
    }
    await loadUsers()
  } catch {
    notify.error(confirmAction.value === 'activate' ? 'Failed to activate user' : 'Failed to deactivate user')
  } finally {
    confirmDialogRef.value?.close()
    userToConfirm.value = null
  }
}

</script>
