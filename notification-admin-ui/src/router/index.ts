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
  // 发布方管理
  {
    path: '/publishers',
    name: 'PublisherList',
    component: () => import('../views/PublisherListView.vue')
  },
  // 事件类型管理
  {
    path: '/event-types',
    name: 'EventTypeList',
    component: () => import('../views/EventTypeListView.vue')
  },
  {
    path: '/event-types/:id',
    name: 'EventTypeDetail',
    component: () => import('../views/EventTypeDetailView.vue')
  },
  // 订阅关系管理
  {
    path: '/subscriptions',
    name: 'SubscriptionList',
    component: () => import('../views/SubscriptionListView.vue')
  },
  // 订阅方（原供应商）管理
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
  // 死信队列
  {
    path: '/dlq',
    name: 'DlqList',
    component: () => import('../views/DlqListView.vue')
  },
  {
    path: '/',
    redirect: '/publishers'
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
