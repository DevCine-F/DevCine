// Tiện ích xử lý ảnh trước khi upload: validate định dạng/dung lượng + nén/thu nhỏ phía client.
// Không dùng thư viện ngoài (Canvas API thuần) — tuân RULES không tự thêm dependency.

const DEFAULT_MAX_MB = 5
const ALLOWED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/gif']

/** Kiểm tra tệp ảnh hợp lệ; ném Error kèm thông điệp tiếng Việt nếu sai. */
export function validateImageFile(file, { maxMB = DEFAULT_MAX_MB, types = ALLOWED_TYPES } = {}) {
  if (!file) throw new Error('Chưa chọn tệp ảnh.')
  if (!file.type || !file.type.startsWith('image/')) throw new Error('Tệp được chọn không phải hình ảnh.')
  if (types && !types.includes(file.type)) throw new Error('Định dạng không hỗ trợ. Chỉ nhận JPG, PNG, WEBP, GIF.')
  if (file.size > maxMB * 1024 * 1024) throw new Error(`Ảnh vượt quá ${maxMB}MB. Vui lòng chọn ảnh nhỏ hơn.`)
  return true
}

/**
 * Nén & thu nhỏ ảnh bằng Canvas. Trả về File mới (nhỏ hơn) hoặc file gốc nếu không nén được/đã nhỏ.
 * - GIF giữ nguyên (canvas làm mất ảnh động).
 * - Ảnh < 200KB bỏ qua nén.
 */
export function compressImage(file, { maxDim = 1600, quality = 0.82 } = {}) {
  return new Promise((resolve) => {
    if (file.type === 'image/gif' || file.size < 200 * 1024) return resolve(file)
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      let { width, height } = img
      if (width > maxDim || height > maxDim) {
        const scale = Math.min(maxDim / width, maxDim / height)
        width = Math.round(width * scale)
        height = Math.round(height * scale)
      }
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0, width, height)
      const outType = file.type === 'image/png' ? 'image/png' : 'image/jpeg'
      canvas.toBlob((blob) => {
        if (!blob || blob.size >= file.size) return resolve(file) // không nhỏ hơn -> giữ gốc
        const ext = outType === 'image/png' ? 'png' : 'jpg'
        const baseName = (file.name || 'image').replace(/\.[^.]+$/, '')
        resolve(new File([blob], `${baseName}.${ext}`, { type: outType, lastModified: Date.now() }))
      }, outType, quality)
    }
    img.onerror = () => { URL.revokeObjectURL(url); resolve(file) }
    img.src = url
  })
}

/** Validate (ném Error nếu sai) rồi nén; dùng ngay trước khi tạo FormData upload. */
export async function prepareImageForUpload(file, opts = {}) {
  validateImageFile(file, opts)
  return compressImage(file, opts)
}
