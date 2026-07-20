import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      component: () => import('@/views/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UsersView.vue'),
          meta: { roles: ['ADMIN'] },
        },
        {
          path: 'customers',
          name: 'customers',
          component: () => import('@/views/CustomersView.vue'),
          meta: { roles: ['ADMIN', 'USER'] },
        },
        {
          path: 'doctors',
          name: 'doctors',
          component: () => import('@/views/DoctorsView.vue'),
          meta: { roles: ['ADMIN', 'USER'] },
        },
        {
          path: 'schedules',
          name: 'schedules',
          component: () => import('@/views/SchedulesView.vue'),
          meta: { roles: ['ADMIN', 'USER', 'DOCTOR'] },
        },
        {
          path: 'appointments',
          name: 'appointments',
          component: () => import('@/views/AppointmentsView.vue'),
          meta: { roles: ['ADMIN', 'USER', 'CUSTOMER', 'DOCTOR'] },
        },
        {
          path: 'bookings',
          name: 'bookings',
          component: () => import('@/views/BookingWizardView.vue'),
          meta: { roles: ['ADMIN', 'USER', 'CUSTOMER'] },
        },
        {
          path: 'me',
          name: 'me',
          component: () => import('@/views/ProfileView.vue'),
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && Array.isArray(to.meta.roles)) {
    const allowedRoles = to.meta.roles as string[]
    const hasAccess = allowedRoles.some((role) => authStore.hasRole(role))
    if (!hasAccess) {
      return { name: 'dashboard' }
    }
  }

  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'dashboard' }
  }
})

export default router
