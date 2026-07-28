export const ADMIN_ACCOUNT_PATH = '/admin/account'

export function canUseAdminPermission(authStore, permission) {
  if (!permission) return true
  return authStore.hasPermission(permission.feature, permission.action || 'view')
}

export function canUseAdminRoute(authStore, route) {
  const meta = route?.meta || {}
  if (meta.adminOnly) return authStore.isAdmin
  // Khai đồng thời staffOnly + permission ⇒ hiểu là HOẶC (khớp điều kiện v-if ở sidebar,
  // vd Bàn giao ca / Phê duyệt: STAFF thấy, hoặc MANAGER có quyền staff_management cũng thấy).
  const checks = []
  if (meta.staffOnly) checks.push(authStore.isStaff)
  if (meta.permission) checks.push(canUseAdminPermission(authStore, meta.permission))
  if (checks.length) return checks.some(Boolean)
  return true
}

export function resolveFirstAccessibleAdminPath(adminRoutes, authStore) {
  const adminRoot = adminRoutes.find((route) => route.path === '/admin')
  const children = adminRoot?.children || []
  const first = children.find((route) => {
    if (route.redirect || !route.path || route.path === 'account') return false
    return canUseAdminRoute(authStore, route)
  })

  if (!first) return ADMIN_ACCOUNT_PATH
  return first.path.startsWith('/') ? first.path : `/admin/${first.path}`
}
