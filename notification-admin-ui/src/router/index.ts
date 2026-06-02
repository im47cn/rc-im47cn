import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/suppliers',
    name: 'SupplierList',
    component: () => import('../views/SupplierListView.vue')
  },
  {
    path: '/suppliers/create',
    name: 'SupplierCreate',
    component: () => import('../views/SupplierFormView.vue')
  },
  {
    path: '/suppliers/:id/edit',
    name: 'SupplierEdit',
    component: () => import('../views/SupplierFormView.vue')
  },
  {
    path: '/suppliers/:id/simulate',
    name: 'SupplierSimulate',
    component: () => import('../views/SimulationView.vue')
  },
  {
    path: '/dlq',
    name: 'DlqList',
    component: () => import('../views/DlqListView.vue')
  },
  {
    path: '/',
    redirect: '/suppliers'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未认证时重定向到登录页
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  if (!authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router
