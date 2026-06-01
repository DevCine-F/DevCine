import { defineStore } from 'pinia'

export const useNotificationStore = defineStore('notifications', {
  state: () => ({
    notifications: [
      {
        id: 1,
        title: 'Đặt vé thành công',
        message: 'Bạn đã đặt vé thành công phim Oppenheimer: Kẻ Hủy Diệt.',
        time: '5 phút trước',
        isRead: false,
        type: 'booking'
      },
      {
        id: 2,
        title: 'Khuyến mãi mới',
        message: 'Mã SUMMER26 hiện đã sẵn sàng. Giảm ngay 20% cho mọi đơn hàng!',
        time: '2 giờ trước',
        isRead: false,
        type: 'promotion'
      },
      {
        id: 3,
        title: 'Cập nhật lịch chiếu',
        message: 'Phim Dune: Part Two vừa bổ sung thêm các suất chiếu IMAX.',
        time: '1 ngày trước',
        isRead: true,
        type: 'info'
      }
    ]
  }),
  getters: {
    unreadCount: (state) => state.notifications.filter(n => !n.isRead).length,
    sortedNotifications: (state) => [...state.notifications].sort((a, b) => b.id - a.id)
  },
  actions: {
    markAsRead(id) {
      const notification = this.notifications.find(n => n.id === id);
      if (notification) {
        notification.isRead = true;
      }
    },
    markAllAsRead() {
      this.notifications.forEach(n => n.isRead = true);
    }
  }
})
