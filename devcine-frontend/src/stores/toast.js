import { defineStore } from 'pinia'

// Toast dùng chung cho toàn bộ trang khách: góc trên-phải, xếp chồng, tự ẩn ~3.5s.
let _id = 0

export const useToastStore = defineStore('toast', {
  state: () => ({
    toasts: [] // [{ id, type: 'success'|'error'|'info'|'warning', message }]
  }),
  actions: {
    push(message, type = 'info', duration = 3500) {
      if (!message) return
      // Chống trùng: một sự kiện lỗi (vd 401/403) làm nhiều request fail cùng lúc → tránh xếp
      // chồng nhiều toast y hệt. Nếu đã có toast cùng nội dung + loại đang hiển thị thì bỏ qua.
      if (this.toasts.some(t => t.message === message && t.type === type)) return
      const id = ++_id
      this.toasts.push({ id, type, message })
      if (duration > 0) {
        setTimeout(() => this.remove(id), duration)
      }
      return id
    },
    remove(id) {
      this.toasts = this.toasts.filter(t => t.id !== id)
    },
    success(message, duration) { return this.push(message, 'success', duration) },
    error(message, duration) { return this.push(message, 'error', duration) },
    info(message, duration) { return this.push(message, 'info', duration) },
    warning(message, duration) { return this.push(message, 'warning', duration) }
  }
})
