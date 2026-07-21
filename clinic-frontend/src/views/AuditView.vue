<template>
  <div>
    <div class="d-flex align-center mb-4">
      <h2 class="text-h5 font-weight-bold">Audit Log</h2>
      <v-spacer />
      <v-text-field
        v-model="entityTypeFilter"
        label="Entity type"
        placeholder="e.g. Doctor"
        single-line
        hide-details
        density="compact"
        class="mr-2"
        style="max-width: 200px"
        variant="outlined"
        clearable
        @keyup.enter="loadEntries"
      />
      <v-text-field
        v-model="actorFilter"
        label="Actor"
        single-line
        hide-details
        density="compact"
        class="mr-4"
        style="max-width: 200px"
        variant="outlined"
        clearable
        @keyup.enter="loadEntries"
      />
      <v-btn color="primary" prepend-icon="mdi-refresh" :loading="loading" @click="loadEntries">Refresh</v-btn>
    </div>

    <v-card>
      <v-card-text>
        <v-data-table
          :headers="headers"
          :items="entries"
          :loading="loading"
          density="compact"
          hover
        >
          <template #item.createdAt="{ item }">
            {{ new Date(item.createdAt).toLocaleString() }}
          </template>
          <template #item.action="{ item }">
            <v-chip size="small" variant="flat" color="primary">{{ item.action }}</v-chip>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { auditApi } from '@/api/audit'
import type { AuditLogEntry } from '@/api/types'

const authStore = useAuthStore()
const notify = useNotificationStore()

const entries = ref<AuditLogEntry[]>([])
const loading = ref(false)
const entityTypeFilter = ref('')
const actorFilter = ref('')

const headers = [
  { title: 'Timestamp', key: 'createdAt', width: 200 },
  { title: 'Actor', key: 'actor', width: 140 },
  { title: 'Action', key: 'action', width: 220 },
  { title: 'Entity Type', key: 'entityType', width: 140 },
  { title: 'Entity ID', key: 'entityId', width: 100 },
  { title: 'Details', key: 'details' },
]

async function loadEntries() {
  if (!authStore.clinicId) return
  loading.value = true
  try {
    const { data } = await auditApi.list(authStore.clinicId, {
      entityType: entityTypeFilter.value || undefined,
      actor: actorFilter.value || undefined,
    })
    entries.value = data
  } catch {
    notify.error('Failed to load audit log')
  } finally {
    loading.value = false
  }
}

onMounted(loadEntries)
</script>
