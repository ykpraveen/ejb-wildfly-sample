<template>
  <v-container fluid class="fill-height">
    <v-row justify="center">
      <v-col cols="12" sm="8" md="5" lg="4">
        <v-card class="elevation-8">
          <v-card-title class="text-h5 text-center pt-6 pb-2">
            <v-icon icon="mdi-clinic-medical" size="large" color="primary" class="mr-2" />
            Clinic Portal
          </v-card-title>
          <v-card-subtitle class="text-center mb-4">Sign in to continue</v-card-subtitle>

          <v-card-text>
            <v-alert v-if="errorMessage" type="error" variant="tonal" class="mb-4" closable @click:close="errorMessage = ''">
              {{ errorMessage }}
            </v-alert>

            <v-form ref="formRef" v-model="formValid" @submit.prevent="handleLogin" lazy-validation>
              <v-text-field
                v-model="form.clinicId"
                label="Clinic ID"
                type="number"
                prepend-inner-icon="mdi-hospital-building"
                :rules="clinicIdRules"
                required
                class="mb-2"
              />
              <v-text-field
                v-model="form.username"
                label="Username"
                prepend-inner-icon="mdi-account"
                :rules="usernameRules"
                required
                class="mb-2"
              />
              <v-text-field
                v-model="form.password"
                label="Password"
                type="password"
                prepend-inner-icon="mdi-lock"
                :rules="passwordRules"
                required
                class="mb-4"
              />
              <v-btn
                type="submit"
                color="primary"
                block
                size="large"
                :loading="loading"
                :disabled="!formValid"
              >
                Sign In
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref()
const formValid = ref(false)
const loading = ref(false)
const errorMessage = ref('')

const form = reactive({
  clinicId: 1,
  username: '',
  password: '',
})

const clinicIdRules = [
  (v: number) => !!v || 'Clinic ID is required',
  (v: number) => v > 0 || 'Clinic ID must be positive',
]

const usernameRules = [
  (v: string) => !!v || 'Username is required',
  (v: string) => v.length >= 3 || 'Username must be at least 3 characters',
]

const passwordRules = [
  (v: string) => !!v || 'Password is required',
  (v: string) => v.length >= 6 || 'Password must be at least 6 characters',
]

async function handleLogin() {
  const { valid } = await formRef.value.validate()
  if (!valid) return

  loading.value = true
  errorMessage.value = ''

  try {
    await authStore.login(form.clinicId, form.username, form.password)
    const redirect = (route.query.redirect as string) ?? '/'
    router.push(redirect)
  } catch (err: any) {
    const message = err?.response?.data?.message
    errorMessage.value = message ?? 'Login failed. Please check your credentials.'
  } finally {
    loading.value = false
  }
}
</script>
