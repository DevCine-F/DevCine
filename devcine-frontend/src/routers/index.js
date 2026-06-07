import { createRouter, createWebHistory } from 'vue-router'
import adminRoutes from './admin'
import customerRoutes from './customer'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    ...adminRoutes,
    ...customerRoutes
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

import { useThemeStore } from '@/stores/theme'

// Navigation Guard (Middleware)
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token');
  const userRole = localStorage.getItem('role'); // admin, customer

  // Logic chặn quyền Admin
  if (to.path.startsWith('/admin')) {
    if (to.path === '/admin/login') {
      return next();
    }
    if (!token || userRole !== 'admin') {
      return next({ name: 'admin-login' });
    }
  }

  // Star Wars Warp Transition
  if (to.path !== from.path && !to.path.startsWith('/admin') && from.name) {
    try {
      const themeStore = useThemeStore()
      themeStore.triggerWarp()
      // Đợi hiệu ứng Warp chạy trước khi load trang mới
      await new Promise(resolve => setTimeout(resolve, 500))
    } catch (error) {
      // Pinia might not be ready on very first load, ignore
    }
  }

  next();
})

export default router
