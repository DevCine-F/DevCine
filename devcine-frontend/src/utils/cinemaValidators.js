// Bộ formatter + validator dùng chung cho form Cụm rạp (CreateCinemaModal & CinemaConfigTab).
// Validator là hàm THUẦN: nhận giá trị -> trả '' nếu hợp lệ, hoặc câu lỗi tiếng Việt.
// Formatter chỉ biến đổi chuỗi hiển thị, không side-effect.

// ===== Formatters =====
export const collapseSpaces = (s) => (s || '').trim().replace(/\s+/g, ' ')

// Viết hoa chữ cái đầu mỗi từ, GIỮ nguyên các ký tự còn lại (không hạ thấp "DevCine").
export const titleCase = (s) =>
  collapseSpaces(s).replace(/(^|\s)(\p{L})/gu, (m, sp, ch) => sp + ch.toUpperCase())

export const rawDigits = (s) => (s || '').replace(/\D/g, '')

// Định dạng hotline cho dễ đọc: tổng đài 1800/1900 -> nhóm 4; số khác -> 4-3-3...
export const formatHotline = (s) => {
  const d = rawDigits(s).slice(0, 11)
  if (!d) return ''
  if (d.startsWith('1800') || d.startsWith('1900')) {
    return (d.slice(0, 4) + ' ' + d.slice(4).replace(/(\d{4})(?=\d)/g, '$1 ')).trim()
  }
  return (d.slice(0, 4) + ' ' + d.slice(4).replace(/(\d{3})(?=\d)/g, '$1 ')).trim()
}

// Chuẩn hoá danh sách tiện ích: tách theo dấu phẩy, trim, bỏ rỗng/trùng, nối lại "a, b, c".
export const normalizeAmenities = (s) => {
  const out = []
  for (const raw of (s || '').split(',')) {
    const v = raw.trim()
    if (v && !out.some((x) => x.toLowerCase() === v.toLowerCase())) out.push(v)
  }
  return out.join(', ')
}

// ===== Validators (trả '' nếu hợp lệ, hoặc câu lỗi) =====
const NAME_RE = /^[\p{L}\p{N} \-&_]+$/u

export const validateName = (name) => {
  const v = collapseSpaces(name)
  if (!v) return 'Tên cụm rạp không được để trống'
  if (v.length < 5 || v.length > 100) return 'Tên cụm rạp phải từ 5 đến 100 ký tự'
  if (!NAME_RE.test(v)) return 'Chỉ cho phép chữ, số, khoảng trắng và - & _'
  return ''
}

export const validateHotline = (hotline) => {
  const d = rawDigits(hotline)
  if (!d) return 'Hotline không được để trống'
  if (d.length < 8 || d.length > 11) return 'Hotline phải gồm 8 đến 11 chữ số'
  return ''
}

export const validateAddress = (address) => {
  const v = collapseSpaces(address)
  if (!v) return 'Địa chỉ không được để trống'
  if (v.length < 10 || v.length > 255) return 'Địa chỉ phải từ 10 đến 255 ký tự'
  return ''
}

export const validateCity = (city) => (!city ? 'Vui lòng chọn Tỉnh/Thành phố' : '')

export const validateDistrict = (district) => (!district ? 'Vui lòng chọn Quận/Huyện' : '')

export const validateDescription = (description) =>
  ((description || '').length > 1000 ? 'Mô tả tối đa 1000 ký tự' : '')

// imageUrl không bắt buộc; nếu có phải khớp regex backend:
// URL http(s) kết thúc bằng .jpg/.jpeg/.png/.webp (cho phép query string theo sau).
export const validateImageUrl = (imageUrl) => {
  const v = (imageUrl || '').trim()
  if (!v) return ''
  if (!/^https?:\/\/.+\.(jpg|jpeg|png|webp)(\?.*)?$/i.test(v)) {
    return 'Ảnh phải là URL http(s) kết thúc bằng .jpg/.jpeg/.png/.webp'
  }
  return ''
}
