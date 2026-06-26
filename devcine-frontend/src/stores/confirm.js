import { defineStore } from 'pinia'

// Hộp thoại xác nhận dùng chung (thay cho window.confirm). Dùng kiểu promise:
//   const ok = await confirm.show({ title, message, confirmText, tone: 'danger' })
//   if (!ok) return
export const useConfirmStore = defineStore('confirm', {
  state: () => ({
    open: false,
    title: 'Xác nhận',
    message: '',
    confirmText: 'Xác nhận',
    cancelText: 'Huỷ',
    tone: 'danger', // 'danger' (đỏ) | 'primary'
    _resolve: null,
  }),
  actions: {
    show(opts = {}) {
      this.title = opts.title || 'Xác nhận'
      this.message = opts.message || ''
      this.confirmText = opts.confirmText || 'Xác nhận'
      this.cancelText = opts.cancelText || 'Huỷ'
      this.tone = opts.tone || 'danger'
      this.open = true
      return new Promise((resolve) => { this._resolve = resolve })
    },
    _settle(result) {
      this.open = false
      const r = this._resolve
      this._resolve = null
      if (r) r(result)
    },
    confirm() { this._settle(true) },
    cancel() { this._settle(false) },
  },
})
