import { createRouter, createWebHistory } from 'vue-router'
import adminRoutes from './admin'
import customerRoutes from './customer'
import { useAuthStore } from '@/stores/auth'
import { canUseAdminRoute, resolveFirstAccessibleAdminPath } from '@/utils/adminAccess'

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

// === Quản lý "phiên" điều hướng ===
// Mục tiêu: khi mới truy cập lại hoặc lâu rồi mới tải lại (coi như hết phiên) thì đưa về
// trang chủ (khách) / Tổng quan (admin); KHÔNG áp dụng khi reload liên tục trong thời gian ngắn.
const ACTIVITY_KEY = 'lastActiveAt'
const SESSION_STALE_MS = 30 * 60 * 1000 // quá 30 phút không hoạt động ⇒ coi như phiên mới/hết phiên
let initialNavigationHandled = false
let lastActivityWrite = 0

function touchActivity() {
  const now = Date.now()
  if (now - lastActivityWrite < 15000) return // throttle ghi localStorage tối đa 1 lần/15s
  lastActivityWrite = now
  localStorage.setItem(ACTIVITY_KEY, String(now))
}

function isStaleSession() {
  const last = parseInt(localStorage.getItem(ACTIVITY_KEY) || '0', 10)
  if (!last) return true // chưa có dấu vết hoạt động ⇒ truy cập mới hoàn toàn
  return Date.now() - last > SESSION_STALE_MS
}

// Các trang KHÔNG ép về landing dù phiên đã cũ (luồng đặc biệt)
function shouldSkipStaleRedirect(to) {
  // Trang đăng nhập là đích đến có chủ đích — luôn giữ
  if (to.path === '/login' || to.path === '/admin/login') return true
  // /success chỉ giữ khi đúng là VNPay trả về (có chữ ký); reload "treo" để lâu thì cho về trang chủ
  if (to.path === '/success') return !!to.query.vnp_SecureHash
  return false
}

// Giữ phiên "sống" theo tương tác thực của người dùng (không chỉ dựa vào điều hướng)
if (typeof window !== 'undefined') {
  ;['mousedown', 'keydown', 'scroll', 'touchstart'].forEach((evt) => {
    window.addEventListener(evt, touchActivity, { passive: true })
  })
}

// Navigation Guard (Middleware)
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token');
  const userRole = localStorage.getItem('role'); // admin, customer
  const isAdminRole = ['admin', 'manager', 'staff'].includes(userRole);

  // Lần điều hướng ĐẦU TIÊN sau khi tải trang: nếu phiên đã cũ/hết hạn thì về landing theo vai trò.
  // Reload liên tục → mốc hoạt động còn mới → không vào nhánh này.
  if (!initialNavigationHandled) {
    initialNavigationHandled = true
    if (!shouldSkipStaleRedirect(to) && isStaleSession()) {
      const landing = isAdminRole ? '/admin/dashboard' : '/'
      if (to.path !== landing) {
        touchActivity()
        return next(landing)
      }
    }
  }

  // Logic chặn quyền Admin
  if (to.path.startsWith('/admin')) {
    if (to.path === '/admin/login') {
      return next();
    }
    if (!token || !isAdminRole) {
      return next({ name: 'admin-login' });
    }

    const authStore = useAuthStore()
    try {
      await authStore.fetchPermissions(!authStore.isAdmin)
    } catch (error) {
      return next({ name: 'admin-login' })
    }

    if (to.path === '/admin') {
      return next(resolveFirstAccessibleAdminPath(adminRoutes, authStore))
    }

    if (!canUseAdminRoute(authStore, to)) {
      return next('/admin/403')
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

// Mỗi lần điều hướng thành công ⇒ đánh dấu phiên còn hoạt động
router.afterEach(() => {
  touchActivity()
})

export default router
