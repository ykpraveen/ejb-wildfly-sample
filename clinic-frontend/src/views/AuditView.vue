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
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAsyncLoad } from '@/composables/useAsyncLoad'
import { auditApi } from '@/api/audit'
import type { AuditLogEntry } from '@/api/types'

const authStore = useAuthStore()

const entries = ref<AuditLogEntry[]>([])
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

const { loading, run: loadEntries } = useAsyncLoad(async () => {
  if (!authStore.clinicId) return
  const { data } = await auditApi.list(authStore.clinicId, {
    entityType: entityTypeFilter.value || undefined,
    actor: actorFilter.value || undefined,
  })
  entries.value = data
}, 'Failed to load audit log')
</script>
