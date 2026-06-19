import { defineStore } from 'pinia'
import { notificationApi } from '@/api/customer/index'
import { useAuthStore } from './auth'

// Định dạng thời gian tương đối ngắn gọn cho UI
const formatRelative = (iso) => {
  if (!iso) return ''
  const then = new Date(iso).getTime()
  const diffMin = Math.floor((Date.now() - then) / 60000)
  if (diffMin < 1) return 'Vừa xong'
  if (diffMin < 60) return `${diffMin} phút trước`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour} giờ trước`
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return `${diffDay} ngày trước`
  return new Date(iso).toLocaleDateString('vi-VN')
}

export const useNotificationStore = defineStore('notifications', {
  state: () => ({
    notifications: [],
    loading: false
  }),
  getters: {
    unreadCount: (state) => state.notifications.filter(n => !n.isRead).length,
    // API đã trả về sắp xếp mới nhất trước
    sortedNotifications: (state) => state.notifications
  },
  actions: {
    async fetch() {
      const auth = useAuthStore()
      if (!auth.user?.id) return
      this.loading = true
      try {
        const { data } = await notificationApi.getForCustomer(auth.user.id)
        const list = data.data ?? data
        this.notifications = list.map(n => ({
          id: n.id,
          title: n.title,
          message: n.message,
          type: (n.type || 'system').toLowerCase(),
          isRead: n.isRead,
          time: formatRelative(n.createdAt)
        }))
      } catch (e) {
        console.error('Không tải được thông báo', e)
      } finally {
        this.loading = false
      }
    },
    async markAsRead(id) {
      const notification = this.notifications.find(n => n.id === id)
      if (notification && !notification.isRead) {
        notification.isRead = true
        try {
          await notificationApi.markAsRead(id)
        } catch (e) {
          console.error('Đánh dấu đã đọc thất bại', e)
        }
      }
    },
    async markAllAsRead() {
      const auth = useAuthStore()
      this.notifications.forEach(n => { n.isRead = true })
      if (!auth.user?.id) return
      try {
        await notificationApi.markAllAsRead(auth.user.id)
      } catch (e) {
        console.error('Đánh dấu tất cả đã đọc thất bại', e)
      }
    }
  }
})
