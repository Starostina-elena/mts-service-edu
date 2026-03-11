import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/auth/useAuth'

import CatalogPage from '@/components/CatalogPage.vue'

const routes = [
  {
    path: '/',
    name: 'catalog',
    component: CatalogPage,
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
  },
  {
    path: '/admin/tariffs/new',
    name: 'tariff-create',
    component: () => import('@/views/TariffCreateView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] },
  },
  {
    path: '/admin/services/new',
    name: 'service-create',
    component: () => import('@/views/ServiceCreateView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const { isLoggedIn, role } = useAuth()

  if (to.meta.requiresAuth && !isLoggedIn.value) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && !to.meta.roles.includes(role.value)) {
    return { name: 'catalog' }
  }
})

export default router
