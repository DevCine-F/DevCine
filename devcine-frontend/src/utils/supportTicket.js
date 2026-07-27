// Tiện ích hiển thị ticket hỗ trợ (CSKH) — dùng chung cho danh sách & modal chi tiết.

// Nhãn chủ đề — đồng bộ với SUBJECTS ở ContactView.vue và ISSUE_TYPE_LABELS ở backend.
export const ISSUE_TYPE_LABELS = {
  TICKET: 'Vấn đề về vé',
  MEMBERSHIP: 'Thành viên',
  SERVICE: 'Góp ý dịch vụ',
  PARTNERSHIP: 'Hợp tác quảng cáo'
}

export const issueTypeLabel = (issueType) =>
  ISSUE_TYPE_LABELS[issueType] || issueType || 'Yêu cầu hỗ trợ'

// Nhãn & class trạng thái.
export const STATUS_META = {
  OPEN: { label: 'Chờ xử lý', class: 'bg-red-500/10 text-red-400 border-red-500/20' },
  IN_PROGRESS: { label: 'Đang xử lý', class: 'bg-blue-500/10 text-blue-400 border-blue-500/20' },
  CLOSED: { label: 'Đã đóng', class: 'bg-green-500/10 text-green-400 border-green-500/20' }
}

export const statusLabel = (status) => STATUS_META[status]?.label || status
export const statusClass = (status) =>
  STATUS_META[status]?.class || 'bg-white/10 text-on-surface-variant border-white/10'

// SĐT có thể nằm ở cột phone (ticket mới) hoặc bị nhét vào description dạng
// "[SĐT: ...] nội dung" (ticket cũ). Trả về { phone, message } đã tách sạch.
const PHONE_PREFIX = /^\s*\[SĐT:\s*([^\]]*)\]\s*/

export const parseSupportContent = (ticket) => {
  const raw = ticket?.description || ''
  const match = raw.match(PHONE_PREFIX)
  const phoneFromDesc = match ? match[1].trim() : ''
  const message = match ? raw.replace(PHONE_PREFIX, '') : raw
  return {
    phone: (ticket?.phone || phoneFromDesc || '').trim(),
    message: message.trim()
  }
}

export const formatTime = (iso) => {
  if (!iso) return ''
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
  })
}
