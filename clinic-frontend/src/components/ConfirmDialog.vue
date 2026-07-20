<template>
  <v-dialog v-model="dialog" max-width="420" persistent>
    <v-card>
      <v-card-title class="text-h6">
        <v-icon :icon="icon" :color="color" class="mr-2" />
        {{ title }}
      </v-card-title>
      <v-card-text class="text-body-1">{{ message }}</v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn variant="text" @click="handleCancel">Cancel</v-btn>
        <v-btn :color="color" :loading="loading" @click="handleConfirm">{{ confirmText }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = withDefaults(defineProps<{
  title: string
  message: string
  confirmText?: string
  icon?: string
  color?: string
}>(), {
  confirmText: 'Confirm',
  icon: 'mdi-alert',
  color: 'error',
})

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

const dialog = ref(false)
const loading = ref(false)

function open() { dialog.value = true }
function close() { dialog.value = false; loading.value = false }

async function handleConfirm() {
  loading.value = true
  emit('confirm')
}

function handleCancel() {
  close()
  emit('cancel')
}

defineExpose({ open, close })
</script>
