import { useAuthStore } from '@/stores/auth'

/**
 * Helper phân quyền action-based dùng chung cho các view khu quản trị.
 * `can(feature, action)` khớp CHÍNH XÁC với ma trận backend enforce (@perm.can).
 * ADMIN luôn true (đã xử lý trong authStore.hasPermission). Dùng ẩn nút Thêm/Sửa/Xoá
 * để MANAGER/STAFF thấy trang read-only đúng nghĩa, tránh bấm rồi mới 403.
 */
export function useAdminPerm() {
  const authStore = useAuthStore()
  const can = (feature, action = 'view') => authStore.hasPermission(feature, action)
  const isAdmin = () => authStore.isAdmin
  return { can, isAdmin }
}
