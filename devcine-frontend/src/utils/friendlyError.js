// Chuyển lỗi (axios error / chuỗi) thành thông báo tiếng Việt thân thiện cho khách.
// Nguyên tắc: KHÔNG bao giờ hiển thị chuỗi kỹ thuật / mã lỗi / tiếng Anh thô.
// - Khớp bảng ánh xạ các lỗi đã biết -> câu tiếng Việt.
// - Nếu là câu tiếng Việt "sạch" -> hiển thị nguyên văn.
// - Còn lại (kỹ thuật / lạ) -> câu chung.

// Bảng ánh xạ các thông điệp backend đã biết (thường là tiếng Anh/kỹ thuật) sang tiếng Việt.
const MAP = [
  [/already taken|on hold/i, 'Một số ghế vừa được người khác đặt. Vui lòng tải lại sơ đồ ghế và chọn ghế khác.'],
  [/voucher has expired/i, 'Mã giảm giá đã hết hạn.'],
  [/already been used/i, 'Mã giảm giá đã được sử dụng.'],
  [/voucher does not belong/i, 'Mã giảm giá không thuộc tài khoản của bạn.'],
  [/voucher not found/i, 'Không tìm thấy mã giảm giá.'],
  [/showtime not found/i, 'Không tìm thấy suất chiếu. Vui lòng chọn lại.'],
  [/seat.*(not found|taken)|seat not found/i, 'Ghế không hợp lệ, vui lòng chọn lại.'],
  [/(f&b|fnb).*not found/i, 'Món ăn/combo không hợp lệ, vui lòng chọn lại.'],
  [/booking not found/i, 'Không tìm thấy đơn đặt vé.'],
  [/customer not found/i, 'Không tìm thấy thông tin khách hàng.'],
  [/network error|timeout|econnaborted/i, 'Mất kết nối máy chủ, vui lòng kiểm tra mạng và thử lại.']
]

// Có chứa dấu tiếng Việt -> nhiều khả năng là câu cho người đọc.
const VN_CHARS = /[ăâđêôơưàáảãạằắẳẵặầấẩẫậèéẻẽẹềếểễệìíỉĩịòóỏõọồốổỗộờớởỡợùúủũụừứửữựỳýỷỹỵ]/i
// Dấu hiệu chuỗi kỹ thuật cần ẩn.
const TECH = /(exception|error\b|null|undefined|\bnan\b|stack|\bsql\b|https?:|\/api\/|status code|cannot read|bytea|constraint|\bat [a-z]+\.)/i

function looksSafeVietnamese(msg) {
  if (!msg || msg.length > 160) return false
  if (TECH.test(msg)) return false
  return VN_CHARS.test(msg)
}

/**
 * @param {*} err - axios error, Error, hoặc chuỗi.
 * @param {string} fallback - câu chung khi không xác định được lỗi an toàn.
 * @returns {string} thông báo tiếng Việt an toàn để hiển thị.
 */
export function friendlyError(err, fallback = 'Đã có lỗi xảy ra, vui lòng thử lại.') {
  if (err && err.code === 'ERR_NETWORK') {
    return 'Mất kết nối máy chủ, vui lòng kiểm tra mạng và thử lại.'
  }
  const status = err?.response?.status
  let msg = ''
  if (typeof err === 'string') msg = err
  else msg = err?.response?.data?.message || err?.response?.data?.error || ''

  for (const [pattern, friendly] of MAP) {
    if (pattern.test(msg)) return friendly
  }

  if (looksSafeVietnamese(msg)) return msg

  // Không có message an toàn -> dựa vào mã trạng thái HTTP cho câu phù hợp
  // Tách 401 (hết phiên) và 403 (thiếu quyền) — trước đây gộp chung gây hiểu nhầm "phải đăng nhập"
  if (status === 401) return 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.'
  if (status === 403) return 'Bạn không có quyền thực hiện thao tác này.'
  if (status >= 500) return 'Máy chủ đang gặp sự cố, vui lòng thử lại sau.'
  return fallback
}
