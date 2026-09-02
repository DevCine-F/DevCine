// Bộ formatter + validator dùng chung cho form Cụm rạp (CreateCinemaModal & CinemaConfigTab).
// Validator là hàm THUẦN: nhận giá trị -> trả '' nếu hợp lệ, hoặc câu lỗi tiếng Việt.
// Formatter chỉ biến đổi chuỗi hiển thị, không side-effect.

// ===== Formatters =====
export const collapseSpaces = (s) => (s || '').trim().replace(/\s+/g, ' ')

// Viết hoa chữ cái đầu mỗi từ, GIỮ nguyên các ký tự còn lại (không hạ thấp "DevCine").
export const titleCase = (s) =>
  collapseSpaces(s).replace(/(^|\s)(\p{L})/gu, (m, sp, ch) => sp + ch.toUpperCase())

export const rawDigits = (s) => (s || '').replace(/\D/g, '')

// Cắt chuỗi số thành các cụm `size`, nhưng KHÔNG để lại 1 số lẻ mồ côi ở cuối:
// nếu cụm cuối chỉ có 1 chữ số thì gộp ngược vào cụm trước. VD ("12345", 4) -> "12345".
const groupDigits = (str, size) => {
  const parts = []
  for (let i = 0; i < str.length; i += size) parts.push(str.slice(i, i + size))
  if (parts.length > 1 && parts[parts.length - 1].length === 1) {
    parts[parts.length - 2] += parts.pop()
  }
  return parts.join(' ')
}

// Định dạng hotline cho dễ đọc: tổng đài 1800/1900 -> nhóm 4; số khác -> nhóm 3.
// Làm sạch TƯỜNG MINH mọi khoảng trắng trước khi cắt (dù rawDigits cũng loại \D),
// và nhóm số sao cho không bao giờ để lại 1 số lẻ đứng riêng ở cuối.
export const formatHotline = (s) => {
  const d = rawDigits(String(s ?? '').replace(/\s+/g, '')).slice(0, 11)
  if (!d) return ''
  const size = d.startsWith('1800') || d.startsWith('1900') ? 4 : 3
  return (d.slice(0, 4) + ' ' + groupDigits(d.slice(4), size)).trim()
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
  if (!(d.startsWith('0') || d.startsWith('1800') || d.startsWith('1900'))) {
    return 'Đầu số Hotline không hợp lệ (hỗ trợ 0x, 1800, 1900)'
  }
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

// Trích toạ độ từ nhiều định dạng Google Maps mà user dễ lấy:
//  - Mã nhúng iframe / URL embed:  ...!2d<kinh độ>!3d<vĩ độ>...
//  - URL thường trên thanh địa chỉ: .../@<vĩ độ>,<kinh độ>,<zoom>z
//  - Tham số q/query/ll=<vĩ độ>,<kinh độ>
// Trả { lat, lng } (chuỗi) nếu nhận diện được, ngược lại null.
// KHÔNG hỗ trợ link rút gọn maps.app.goo.gl (redirect, không chứa toạ độ) — cần dùng mã nhúng.
export const parseGoogleMapsInput = (input) => {
  const s = String(input ?? '')
  if (!s.trim()) return null
  const NUM = '(-?\\d+(?:\\.\\d+)?)'
  // 1) Mã nhúng / URL embed: !3d = vĩ độ, !2d = kinh độ
  const lat3 = s.match(new RegExp('!3d' + NUM))
  const lng2 = s.match(new RegExp('!2d' + NUM))
  if (lat3 && lng2) return { lat: lat3[1], lng: lng2[1] }
  // 2) URL thường: .../@<vĩ độ>,<kinh độ>,<zoom>
  const at = s.match(new RegExp('@' + NUM + ',' + NUM))
  if (at) return { lat: at[1], lng: at[2] }
  // 3) q/query/ll=<vĩ độ>,<kinh độ>
  const q = s.match(new RegExp('[?&](?:q|query|ll)=' + NUM + ',' + NUM))
  if (q) return { lat: q[1], lng: q[2] }
  return null
}

// Toạ độ bản đồ: tuỳ chọn; nếu có phải là số trong dải hợp lệ.
export const validateLatitude = (v) => {
  if (v === '' || v == null) return ''
  const n = Number(v)
  if (Number.isNaN(n) || n < -90 || n > 90) return 'Vĩ độ phải là số trong khoảng -90 đến 90'
  return ''
}
export const validateLongitude = (v) => {
  if (v === '' || v == null) return ''
  const n = Number(v)
  if (Number.isNaN(n) || n < -180 || n > 180) return 'Kinh độ phải là số trong khoảng -180 đến 180'
  return ''
}
