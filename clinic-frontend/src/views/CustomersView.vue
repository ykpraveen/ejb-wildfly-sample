<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h2 class="text-h5 font-weight-bold">Customer Management</h2>
      <v-spacer />
      <v-btn color="primary" prepend-icon="mdi-plus" @click="openCreate">New Customer</v-btn>
    </div>

    <v-card>
      <v-card-text>
        <v-text-field
          v-model="search"
          prepend-inner-icon="mdi-magnify"
          label="Search customers..."
          density="compact"
          variant="outlined"
          hide-details
          single-line
          class="mb-4"
        />
        <v-data-table
          :headers="headers"
          :items="filteredCustomers"
          :loading="loading"
          density="compact"
          hover
        >
          <template #item.actions="{ item }">
            <v-icon size="small" class="mr-2" @click="openEdit(item)">mdi-pencil</v-icon>
            <v-icon size="small" color="error" @click="openDelete(item)">mdi-delete</v-icon>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <!-- Create Dialog -->
    <v-dialog v-model="showCreate" max-width="500" persistent>
      <v-card>
        <v-card-title>Create Customer</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="handleCreate">
            <Field v-slot="{ field, errorMessage }" name="fullName">
              <v-text-field v-bind="field" :error-messages="errorMessage" label="Full Name" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="username">
              <v-text-field v-bind="field" :error-messages="errorMessage" label="Username" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="email">
              <v-text-field v-bind="field" :error-messages="errorMessage" label="Email" type="email" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="phone">
              <v-text-field v-bind="field" :error-messages="errorMessage" label="Phone" />
            </Field>
            <v-card-actions class="pa-0 pt-4">
              <v-spacer />
              <v-btn variant="text" @click="closeCreate">Cancel</v-btn>
              <v-btn type="submit" color="primary" :loading="submitting">Create</v-btn>
            </v-card-actions>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Edit Dialog -->
    <v-dialog v-model="showEdit" max-width="500" persistent>
      <v-card>
        <v-card-title>Edit Customer</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="handleUpdate">
            <Field v-slot="{ field, errorMessage }" name="fullName">
              <v-text-field v-bind="field" :error-messages="errorMessage" label="Full Name" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="email">
              <v-text-field v-bind="field" :error-messages="errorMessage" label="Email" type="email" />
            </Field>
            <Field v-slot="{ field, errorMessage }" name="phone">
              <v-text-field v-bind="field" :error-messages="errorMessage" label="Phone" />
            </Field>
            <v-card-actions class="pa-0 pt-4">
              <v-spacer />
              <v-btn variant="text" @click="closeEdit">Cancel</v-btn>
              <v-btn type="submit" color="primary" :loading="submitting">Save</v-btn>
            </v-card-actions>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Delete Dialog -->
    <ConfirmDialog
      ref="deleteDialogRef"
      title="Delete Customer"
      :message="`Are you sure you want to delete '${customerToDelete?.fullName}'?`"
      confirm-text="Delete"
      icon="mdi-delete"
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
import { customersApi } from '@/api/customers'
import type { Customer } from '@/api/types'
import { createCustomerSchema, updateCustomerSchema } from '@/validation'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const authStore = useAuthStore()
const notify = useNotificationStore()

const customers = ref<Customer[]>([])
const loading = ref(false)
const submitting = ref(false)
const search = ref('')

const showCreate = ref(false)
const showEdit = ref(false)
const editingCustomer = ref<Customer | null>(null)
const customerToDelete = ref<Customer | null>(null)
const deleteDialogRef = ref<InstanceType<typeof ConfirmDialog> | null>(null)

const createForm = useForm({
  validationSchema: toTypedSchema(createCustomerSchema),
})

const editForm = useForm({
  validationSchema: toTypedSchema(updateCustomerSchema),
})

const headers = [
  { title: 'ID', key: 'id', width: 60 },
  { title: 'Full Name', key: 'fullName' },
  { title: 'Username', key: 'username' },
  { title: 'Email', key: 'email' },
  { title: 'Phone', key: 'phone' },
  { title: 'Actions', key: 'actions', sortable: false, width: 100 },
]

const filteredCustomers = computed(() => {
  if (!search.value) return customers.value
  const q = search.value.toLowerCase()
  return customers.value.filter(
    (c) =>
      c.fullName.toLowerCase().includes(q) ||
      c.username.toLowerCase().includes(q) ||
      c.email.toLowerCase().includes(q) ||
      c.phone.toLowerCase().includes(q),
  )
})

async function loadCustomers() {
  if (!authStore.clinicId) return
  loading.value = true
  try {
    const { data } = await customersApi.list(authStore.clinicId)
    customers.value = data
  } catch {
    notify.error('Failed to load customers.')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.resetForm()
  showCreate.value = true
}

function closeCreate() {
  showCreate.value = false
  createForm.resetForm()
}

const handleCreate = createForm.handleSubmit(async (values) => {
  submitting.value = true
  try {
    await customersApi.create({ clinicId: authStore.clinicId, ...values })
    notify.success('Customer created successfully.')
    closeCreate()
    await loadCustomers()
  } catch {
    notify.error('Failed to create customer.')
  } finally {
    submitting.value = false
  }
})

function openEdit(customer: Customer) {
  editingCustomer.value = customer
  editForm.resetForm({
    values: { fullName: customer.fullName, email: customer.email, phone: customer.phone },
  })
  showEdit.value = true
}

function closeEdit() {
  showEdit.value = false
  editingCustomer.value = null
  editForm.resetForm()
}

const handleUpdate = editForm.handleSubmit(async (values) => {
  if (!editingCustomer.value) return
  submitting.value = true
  try {
    await customersApi.update(editingCustomer.value.id, authStore.clinicId, values)
    notify.success('Customer updated successfully.')
    closeEdit()
    await loadCustomers()
  } catch {
    notify.error('Failed to update customer.')
  } finally {
    submitting.value = false
  }
})

function openDelete(customer: Customer) {
  customerToDelete.value = customer
  deleteDialogRef.value?.open()
}

async function handleDelete() {
  if (!customerToDelete.value) return
  try {
    await customersApi.remove(customerToDelete.value.id, authStore.clinicId)
    notify.success('Customer deleted successfully.')
    await loadCustomers()
  } catch {
    notify.error('Failed to delete customer.')
  } finally {
    deleteDialogRef.value?.close()
    customerToDelete.value = null
  }
}

onMounted(loadCustomers)
</script>
