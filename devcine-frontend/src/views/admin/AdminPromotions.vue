<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import axios from 'axios'
import api from '@/api/axios'
import { marketingApi, customerApi, promoArticleApi } from '@/api/admin/index'
import CustomSelect from '@/components/common/CustomSelect.vue'
import { prepareImageForUpload } from '@/utils/imageUpload'
import { useAdminPerm } from '@/composables/useAdminPerm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const { can } = useAdminPerm()
const toastStore = useToastStore()
const filterStatus = ref('all')
const statusOptions = [
  { value: 'all', label: 'Tất cả trạng thái' },
  { value: 'active', label: 'Đang hiển thị' },
  { value: 'inactive', label: 'Đang ẩn' }
]
const discountTypeOptions = [
  { value: 'PERCENTAGE', label: 'Phần trăm (%)' },
  { value: 'FIXED_AMOUNT', label: 'Tiền cố định' }
]

const activeTab = ref('vouchers')
const cinemasList = ref([])
const moviesList = ref([])   // danh sách phim cho voucher "áp dụng theo phim"
const promotions = ref([])
const combos = ref([])

const eligibilityOptions = [
  { value: 'ALL', label: 'Mọi khách hàng' },
  { value: 'NEW_CUSTOMER', label: 'Chỉ khách hàng mới' },
  { value: 'TIER_SILVER', label: 'Khách thân thiết (hạng Bạc trở lên)' },
  { value: 'TIER_GOLD', label: 'Khách VIP (hạng Vàng trở lên)' },
  { value: 'TIER_PLATINUM', label: 'Khách VIP (hạng Bạch Kim trở lên)' }
]

const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080') + '/api/marketing'

const isVoucherDrawerOpen = ref(false)
const isComboDrawerOpen = ref(false)
const isArticleDrawerOpen = ref(false)

// ===== Tin khuyến mãi (Promo Articles) — dữ liệu thật =====
const articles = ref([])
const isLoadingArticles = ref(false)
const articleSearch = ref('')
const editingArticleId = ref(null)
const isSavingArticle = ref(false)
const isUploadingArticleImage = ref(false)
const articleDeleteTarget = ref(null)
const isDeletingArticle = ref(false)

const newArticle = ref({
  title: '',
  description: '',
  image: '',
  startDate: '',
  endDate: '',
  content: '',
  status: 'active'
})

// Lọc theo từ khoá + trạng thái (client-side)
const filteredArticles = computed(() => {
  const q = articleSearch.value.trim().toLowerCase()
  return articles.value.filter(a => {
    const matchQ = !q || (a.title || '').toLowerCase().includes(q) || (a.description || '').toLowerCase().includes(q)
    const matchStatus = filterStatus.value === 'all'
      || (filterStatus.value === 'active' && a.status === 'active')
      || (filterStatus.value === 'inactive' && a.status === 'inactive')
    return matchQ && matchStatus
  })
})

const fetchArticles = async () => {
  isLoadingArticles.value = true
  try {
    const { data } = await promoArticleApi.getAll()
    articles.value = (Array.isArray(data) ? data : []).map(a => ({
      id: a.id,
      title: a.title,
      description: a.description,
      image: a.imageUrl,
      content: a.content,
      startDate: a.startDate || '',
      endDate: a.endDate || '',
      status: a.isActive ? 'active' : 'inactive'
    }))
  } catch (err) {
    showToast('Không thể tải danh sách tin khuyến mãi.', 'error')
  } finally {
    isLoadingArticles.value = false
  }
}

const openArticleDrawer = () => {
  editingArticleId.value = null
  newArticle.value = { title: '', description: '', image: '', startDate: '', endDate: '', content: '', status: 'active' }
  isArticleDrawerOpen.value = true
}

const openEditArticle = (article) => {
  editingArticleId.value = article.id
  newArticle.value = {
    title: article.title || '',
    description: article.description || '',
    image: article.image || '',
    startDate: article.startDate ? String(article.startDate).slice(0, 10) : '',
    endDate: article.endDate ? String(article.endDate).slice(0, 10) : '',
    content: article.content || '',
    status: article.status || 'active'
  }
  isArticleDrawerOpen.value = true
}

// Upload ảnh tin lên Cloudinary qua /api/upload (giống màn Thực đơn F&B)
const handleArticleImageUpload = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  let prepared
  try {
    prepared = await prepareImageForUpload(file)
  } catch (err) {
    showToast(friendlyError(err, 'Ảnh không hợp lệ.'), 'error')
    e.target.value = ''
    return
  }
  isUploadingArticleImage.value = true
  try {
    const fd = new FormData()
    fd.append('file', prepared)
    const { data } = await api.post('/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    newArticle.value.image = data.url
    showToast('Tải ảnh lên thành công.')
  } catch (err) {
    showToast('Tải ảnh thất bại.', 'error')
  } finally {
    isUploadingArticleImage.value = false
    e.target.value = '' // cho phép chọn lại cùng file
  }
}

const handleSaveArticle = async () => {
  if (!newArticle.value.title?.trim()) {
    showToast('Vui lòng nhập tiêu đề tin khuyến mãi.', 'error')
    return
  }
  isSavingArticle.value = true
  try {
    const payload = {
      title: newArticle.value.title.trim(),
      description: newArticle.value.description?.trim() || null,
      imageUrl: newArticle.value.image || null,
      content: newArticle.value.content?.trim() || null,
      startDate: newArticle.value.startDate || null,
      endDate: newArticle.value.endDate || null,
      isActive: newArticle.value.status === 'active'
    }
    if (editingArticleId.value) {
      await promoArticleApi.update(editingArticleId.value, payload)
      showToast('Cập nhật tin khuyến mãi thành công.')
    } else {
      await promoArticleApi.create(payload)
      showToast('Đăng tin khuyến mãi thành công.')
    }
    isArticleDrawerOpen.value = false
    await fetchArticles()
  } catch (err) {
    showToast(friendlyError(err, 'Lưu tin khuyến mãi thất bại.'), 'error')
  } finally {
    isSavingArticle.value = false
  }
}

const handleToggleArticle = async (article) => {
  const prev = article.status
  article.status = prev === 'active' ? 'inactive' : 'active' // optimistic
  try {
    await promoArticleApi.toggle(article.id)
  } catch (err) {
    article.status = prev // revert nếu lỗi
    showToast('Đổi trạng thái thất bại.', 'error')
  }
}

const confirmDeleteArticle = async () => {
  if (!articleDeleteTarget.value) return
  isDeletingArticle.value = true
  try {
    await promoArticleApi.delete(articleDeleteTarget.value.id)
    showToast('Đã xoá tin khuyến mãi.')
    articleDeleteTarget.value = null
    await fetchArticles()
  } catch (err) {
    showToast(friendlyError(err, 'Xoá thất bại.'), 'error')
  } finally {
    isDeletingArticle.value = false
  }
}


const newVoucher = ref({
  code: '',
  type: 'PERCENTAGE',
  value: null,
  allowPointExchange: false,
  pointsRequired: null,
  title: '',
  description: '',
  startDate: '',
  expiry: '',
  minOrderValue: null,
  applicableMovieId: '',
  customerEligibility: 'ALL',
  usageLimit: null,
  maxTicketQuantity: null,
  maxDiscountAmount: null,
  cinemaMode: 'all',
  selectedCinemas: []
})

// Combobox "Áp dụng theo phim": tìm kiếm + chọn (thay <select> native lệch màu nền)
const movieDropdownOpen = ref(false)
const movieSearch = ref('')
const cinemaSearch = ref('')
const filteredMoviesList = computed(() => {
  const q = movieSearch.value.trim().toLowerCase()
  if (!q) return moviesList.value
  return moviesList.value.filter(m => (m.title || '').toLowerCase().includes(q))
})
const filteredCinemasList = computed(() => {
  const q = cinemaSearch.value.trim().toLowerCase()
  if (!q) return cinemasList.value
  return cinemasList.value.filter(c => (c.name || '').toLowerCase().includes(q))
})
const selectedMovieTitle = computed(() => {
  if (!newVoucher.value.applicableMovieId) return 'Tất cả phim'
  const m = moviesList.value.find(x => x.id === newVoucher.value.applicableMovieId)
  return m ? m.title : 'Tất cả phim'
})
const selectMovieForVoucher = (id) => {
  newVoucher.value.applicableMovieId = id
  movieDropdownOpen.value = false
  movieSearch.value = ''
}
// Hôm nay (YYYY-MM-DD, theo giờ local) — chặn chọn ngày hết hạn trong quá khứ
const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})
// Ngày tối thiểu cho ô Hết hạn = NGÀY SAU ngày bắt đầu (hoặc sau hôm nay nếu chưa chọn) → ép end > start
const endMinStr = computed(() => {
  const base = newVoucher.value.startDate || todayStr.value
  const d = new Date(`${base}T00:00:00`); d.setDate(d.getDate() + 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

const newCombo = ref({
  name: '',
  price: null,
  description: '',
  image: '',
  status: 'active',
  items: [{ id: Date.now(), name: '', quantity: 1 }],
  cinemaMode: 'all',
  selectedCinemas: []
})

const addComboItem = () => {
  newCombo.value.items.push({ id: Date.now(), name: '', quantity: 1 })
}

const removeComboItem = (index) => {
  newCombo.value.items.splice(index, 1)
}

const editingVoucherId = ref(null)
const isSavingVoucher = ref(false)
// Khóa cứng ô Ngày bắt đầu khi sửa voucher ĐANG CHẠY (start <= hôm nay hoặc null) — tránh sai lịch sử đơn cũ
const editStartLocked = ref(false)

const openVoucherDrawer = () => {
  editingVoucherId.value = null
  editStartLocked.value = false
  newVoucher.value = { code: '', type: 'PERCENTAGE', value: null, allowPointExchange: false, pointsRequired: null, title: '', description: '', startDate: '', expiry: '', minOrderValue: null, applicableMovieId: '', customerEligibility: 'ALL', usageLimit: null, maxTicketQuantity: null, maxDiscountAmount: null, cinemaMode: 'all', selectedCinemas: [] }
  voucherErrors.value = {}
  pctLimitWarn.value = ''
  movieDropdownOpen.value = false; movieSearch.value = ''; cinemaSearch.value = ''
  isVoucherDrawerOpen.value = true
}

// Mở drawer ở chế độ chỉnh sửa, đổ dữ liệu promotion thật vào form
const openEditVoucher = (promo) => {
  editingVoucherId.value = promo.id
  newVoucher.value = {
    code: promo.code || '',
    type: promo.discountType || 'PERCENTAGE',
    value: promo.discountValue != null ? Number(promo.discountValue) : null,
    allowPointExchange: !!promo.allowPointRedemption,
    pointsRequired: promo.pointsRequired || null,
    title: promo.name || '', description: promo.description || '',
    startDate: promo.startDate ? String(promo.startDate).slice(0, 10) : '',
    expiry: promo.endDate ? String(promo.endDate).slice(0, 10) : '',
    minOrderValue: promo.minOrderValue != null ? Number(promo.minOrderValue) : null,
    applicableMovieId: promo.applicableMovieId != null ? promo.applicableMovieId : '',
    customerEligibility: promo.customerEligibility || 'ALL',
    usageLimit: promo.usageLimit || null,
    maxTicketQuantity: promo.maxTicketQuantity || null,
    maxDiscountAmount: promo.maxDiscountAmount != null ? Number(promo.maxDiscountAmount) : null,
    cinemaMode: 'all', selectedCinemas: []
  }
  // Voucher ĐANG CHẠY (chưa có ngày bắt đầu = áp dụng ngay, hoặc bắt đầu <= hôm nay) → khóa ô ngày bắt đầu
  editStartLocked.value = !promo.startDate || String(promo.startDate).slice(0, 10) <= todayStr.value
  voucherErrors.value = {}
  pctLimitWarn.value = ''
  movieDropdownOpen.value = false; movieSearch.value = ''; cinemaSearch.value = ''
  isVoucherDrawerOpen.value = true
}

const openComboDrawer = () => {
  newCombo.value = { name: '', price: null, description: '', image: '', status: 'active', items: [{ id: Date.now(), name: '', quantity: 1 }], cinemaMode: 'all', selectedCinemas: [] }
  isComboDrawerOpen.value = true
}


// Toast dùng chung toàn web (AppToast trong AdminLayout) — nền đặc, đủ 4 trạng thái
const showToast = (message, type = 'success') => toastStore.push(message, type)

// ============================================================================
//  VOUCHER FORM — 3 tầng kiểm tra: (1) Format tự động khi gõ, (2) Validate từng
//  trường khi Lưu, (3) Ràng buộc chéo giữa các trường.
// ============================================================================

// Lỗi validate theo từng trường (hiển thị inline dưới ô nhập)
const voucherErrors = ref({})
const clearVErr = (key) => {
  if (voucherErrors.value[key]) {
    const e = { ...voucherErrors.value }
    delete e[key]
    voucherErrors.value = e
  }
}

// Cuộn + focus tới ô lỗi ĐẦU TIÊN (theo thứ tự hiển thị) khi validate fail
const voucherBodyRef = ref(null)
const voucherFieldOrder = ['code', 'title', 'description', 'value', 'startDate', 'expiry', 'minOrderValue', 'usageLimit', 'maxTicketQuantity', 'maxDiscountAmount', 'pointsRequired']
const focusFirstVoucherError = () => {
  const firstKey = voucherFieldOrder.find(k => voucherErrors.value[k])
  if (!firstKey) return
  nextTick(() => {
    const el = voucherBodyRef.value?.querySelector(`[data-field="${firstKey}"]`)
    if (!el) return
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    if (typeof el.focus === 'function') el.focus({ preventScroll: true })
  })
}

// ---- (1) FORMAT TỰ ĐỘNG (UX khi gõ) ----
const fmtThousand = (n) => (n === null || n === undefined || n === '' ? '' : Number(n).toLocaleString('vi-VN'))

// Mã code: viết hoa + chỉ giữ chữ và số (bỏ khoảng trắng & ký tự đặc biệt)
// Ô Mã code: bỏ dấu (É→E, Đ→D), viết hoa, chỉ giữ chữ & số. Lọc ở input để không "nhảy" con trỏ.
// Lưu ý: bộ gõ tiếng Việt cấp HĐH (Unikey/EVKey) biến s/f/r/x/j thành dấu THANH trước khi tới ô,
// JS không chặn được — nên khuyến nghị người dùng tắt bộ gõ khi nhập mã (xem gợi ý dưới ô).
const onCodeInput = () => {
  newVoucher.value.code = (newVoucher.value.code || '')
    .normalize('NFD').replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd').replace(/Đ/g, 'D')
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
  clearVErr('code')
}
// ===== Hạn mức trần (Ceiling Bounding) — khống chế SỐ KÝ TỰ để không nhập được số vô lý =====
// 9 số = tối đa 999.999.999đ; 7 số = 9.999.999 lượt; 3 số = 999 vé.
const FIELD_MAX_DIGITS = {
  minOrderValue: 9,
  maxDiscountAmount: 9,
  usageLimit: 7,
  maxTicketQuantity: 3,
  pointsRequired: 9,
}
const MAX_TICKETS_PER_HALL = 200 // trần "số vé/đơn" theo số ghế một phòng chiếu thực tế
const MAX_MONEY = 999999999      // trần tiền (VNĐ) — đơn/giảm không quá ~1 tỷ
const MAX_USAGE = 9999999        // trần lượt dùng — chiến dịch toàn quốc tối đa vài triệu lượt

// Ô tiền tệ: chỉ giữ số, cắt theo trần ký tự, model lưu số nguyên thô. Gán lại e.target.value để
// chặn ký tự lạ ngay cả khi model không đổi (Vue bỏ qua patch :value khi giá trị trùng).
const onMoneyInput = (e, key) => {
  let digits = e.target.value.replace(/\D/g, '')
  if (FIELD_MAX_DIGITS[key]) digits = digits.slice(0, FIELD_MAX_DIGITS[key])
  newVoucher.value[key] = digits ? Number(digits) : null
  e.target.value = digits ? Number(digits).toLocaleString('vi-VN') : ''
  clearVErr(key)
}
// Ô chỉ cho số nguyên (không phân cách) — cũng cắt theo trần ký tự
const onIntInput = (e, key) => {
  let digits = e.target.value.replace(/\D/g, '')
  if (FIELD_MAX_DIGITS[key]) digits = digits.slice(0, FIELD_MAX_DIGITS[key])
  newVoucher.value[key] = digits ? Number(digits) : null
  e.target.value = digits
  clearVErr(key)
}
// Cảnh báo khi CHẶN nhập vượt trần % (minh bạch, không tự đè ngầm)
const pctLimitWarn = ref('')
// Giá trị giảm: % -> chặn keystroke làm vượt 100 (giữ số hợp lệ trước đó + cảnh báo); VNĐ -> tiền tệ.
const onDiscountValueInput = (e) => {
  const digits = e.target.value.replace(/\D/g, '')
  if (newVoucher.value.type === 'PERCENTAGE') {
    const num = digits ? Number(digits) : null
    if (num != null && num > 100) {
      // Vượt 100 → KHÔNG nhận ký tự này: trả ô về giá trị hợp lệ trước đó + báo cho người dùng
      pctLimitWarn.value = 'Chỉ nhập được tối đa 100%. Giá trị giảm (%) từ 1 đến 100.'
      e.target.value = newVoucher.value.value != null ? String(newVoucher.value.value) : ''
      return
    }
    pctLimitWarn.value = ''
    newVoucher.value.value = num
    e.target.value = digits
  } else {
    pctLimitWarn.value = ''
    newVoucher.value.value = digits ? Number(digits) : null
    e.target.value = digits ? Number(digits).toLocaleString('vi-VN') : ''
  }
  clearVErr('value')
}
// Giá trị hiển thị cho ô "Giá trị giảm" tuỳ loại
const discountValueDisplay = computed(() =>
  newVoucher.value.type === 'PERCENTAGE'
    ? (newVoucher.value.value ?? '')
    : fmtThousand(newVoucher.value.value))

// Lỗi giá trị giảm THEO THỜI GIAN THỰC (không tự sửa số) — dùng để viền đỏ + khóa nút Lưu ngay khi vượt ngưỡng
const discountValueError = computed(() => {
  const v = newVoucher.value
  if (v.value == null || v.value === '') return ''         // trống → để validate lúc Lưu xử lý (không chặn nút vì lý do này)
  const dv = Number(v.value)
  if (Number.isNaN(dv)) return 'Giá trị giảm không hợp lệ.'
  if (v.type === 'PERCENTAGE') {
    if (dv < 1 || dv > 100) return 'Giá trị giảm (%) phải từ 1 đến 100.'
  } else if (dv <= 1000) {
    return 'Giá trị giảm (VNĐ) phải lớn hơn 1.000đ.'
  }
  return ''
})

// Lỗi "Số vé tối đa/đơn" theo thời gian thực — không vượt quá số ghế một phòng chiếu
const maxTicketError = computed(() => {
  const v = newVoucher.value.maxTicketQuantity
  if (v == null || v === '') return ''
  if (Number(v) > MAX_TICKETS_PER_HALL) {
    return `Số vé tối đa được ưu đãi trên một đơn không được vượt quá ${MAX_TICKETS_PER_HALL} (số ghế một phòng chiếu).`
  }
  return ''
})

// ---- (3) RÀNG BUỘC CHÉO (phản ứng ngay khi đổi loại/giá trị) ----
// Loại "Tiền cố định": trần Giảm tối đa = chính giá trị giảm và bị khoá.
const isFixed = computed(() => newVoucher.value.type === 'FIXED_AMOUNT')
watch(
  () => [newVoucher.value.type, newVoucher.value.value],
  () => {
    if (isFixed.value) {
      newVoucher.value.maxDiscountAmount = newVoucher.value.value != null ? Number(newVoucher.value.value) : null
      clearVErr('maxDiscountAmount')
    }
  }
)

// ---- (2) VALIDATE TỪNG TRƯỜNG + (3) RÀNG BUỘC CHÉO khi bấm Lưu ----
const validateVoucher = () => {
  const e = {}
  const v = newVoucher.value

  // code: bắt buộc, 3–20 ký tự, chỉ chữ & số
  const code = (v.code || '').trim()
  if (!code) e.code = 'Vui lòng nhập mã code.'
  else if (code.length < 3 || code.length > 20) e.code = 'Mã code dài 3–20 ký tự.'
  else if (!/^[A-Z0-9]+$/.test(code)) e.code = 'Mã code chỉ gồm chữ và số.'

  // title: bắt buộc, 5–100 ký tự
  const title = (v.title || '').trim()
  if (!title) e.title = 'Vui lòng nhập tiêu đề chiến dịch.'
  else if (title.length < 5 || title.length > 100) e.title = 'Tiêu đề dài 5–100 ký tự.'

  // description: bắt buộc, tối đa 255 ký tự
  const desc = (v.description || '').trim()
  if (!desc) e.description = 'Vui lòng nhập mô tả ngắn.'
  else if (desc.length > 255) e.description = 'Mô tả tối đa 255 ký tự.'

  // discountValue: bắt buộc; % 1–100; VNĐ > 1.000
  const dv = Number(v.value)
  if (v.value == null || v.value === '' || Number.isNaN(dv)) {
    e.value = 'Vui lòng nhập giá trị giảm.'
  } else if (v.type === 'PERCENTAGE') {
    if (dv < 1 || dv > 100) e.value = 'Giá trị giảm (%) phải từ 1 đến 100.'
  } else {
    if (dv <= 1000) e.value = 'Giá trị giảm (VNĐ) phải lớn hơn 1.000đ.'
  }

  // ===== NGÀY BẮT ĐẦU & HẾT HẠN =====
  // So sánh theo NGÀY (bỏ giờ). startDate trống = "áp dụng ngay" (hôm nay).
  // Ô ngày bắt đầu bị khóa khi sửa voucher đang chạy → không validate lại (giữ nguyên).
  if (!editStartLocked.value && v.startDate && v.startDate < todayStr.value) {
    e.startDate = 'Ngày bắt đầu không được ở quá khứ.'
  }
  if (!v.expiry) {
    e.expiry = 'Vui lòng chọn ngày hết hạn.'
  } else {
    // Ngày bắt đầu hiệu lực: ô nhập (nếu có) → nếu trống thì coi là hôm nay
    const effStart = v.startDate || todayStr.value
    if (v.expiry <= effStart) {
      e.expiry = 'Ngày hết hạn phải sau ngày bắt đầu.'
    } else if (v.expiry < todayStr.value) {
      e.expiry = 'Ngày hết hạn phải từ hôm nay trở đi.'
    }
  }

  // minOrderAmount: BẮT BUỘC, số nguyên >= 0 (nhập 0 nếu không yêu cầu)
  const minOrder = Number(v.minOrderValue || 0)
  if (v.minOrderValue == null || v.minOrderValue === '') e.minOrderValue = 'Vui lòng nhập đơn tối thiểu (nhập 0 nếu không yêu cầu).'
  else if (!Number.isInteger(minOrder) || minOrder < 0) e.minOrderValue = 'Đơn tối thiểu phải là số nguyên ≥ 0.'
  else if (minOrder > MAX_MONEY) e.minOrderValue = 'Đơn tối thiểu không được vượt quá 999.999.999đ.'

  // usageLimit: BẮT BUỘC, số nguyên >= 0 (0 = không giới hạn)
  const usage = Number(v.usageLimit || 0)
  if (v.usageLimit == null || v.usageLimit === '') e.usageLimit = 'Vui lòng nhập giới hạn lượt dùng (nhập 0 nếu không giới hạn).'
  else if (!Number.isInteger(usage) || usage < 0) e.usageLimit = 'Giới hạn lượt dùng phải là số nguyên ≥ 0.'
  else if (usage > MAX_USAGE) e.usageLimit = 'Giới hạn lượt dùng không được vượt quá 9.999.999.'

  // maxApplicableTickets: BẮT BUỘC, số nguyên >= 0 (0 = không giới hạn)
  const maxTk = Number(v.maxTicketQuantity || 0)
  if (v.maxTicketQuantity == null || v.maxTicketQuantity === '') e.maxTicketQuantity = 'Vui lòng nhập số vé tối đa được giảm (nhập 0 nếu không giới hạn).'
  else if (!Number.isInteger(maxTk) || maxTk < 0) e.maxTicketQuantity = 'Số vé tối đa phải là số nguyên ≥ 0.'
  else if (maxTk > MAX_TICKETS_PER_HALL) e.maxTicketQuantity = `Số vé tối đa/đơn không được vượt quá ${MAX_TICKETS_PER_HALL} (số ghế một phòng chiếu).`

  // maxDiscountAmount: BẮT BUỘC, số nguyên >= 0 (mã tiền cố định tự điền = giá trị giảm)
  const maxDisc = Number(v.maxDiscountAmount || 0)
  if (!isFixed.value && (v.maxDiscountAmount == null || v.maxDiscountAmount === '')) e.maxDiscountAmount = 'Vui lòng nhập giảm tối đa (nhập 0 nếu không giới hạn).'
  else if (!Number.isInteger(maxDisc) || maxDisc < 0) e.maxDiscountAmount = 'Giảm tối đa phải là số nguyên ≥ 0.'
  else if (maxDisc > MAX_MONEY) e.maxDiscountAmount = 'Giảm tối đa không được vượt quá 999.999.999đ.'

  // ===== RÀNG BUỘC CHÉO =====
  if (v.type === 'PERCENTAGE') {
    // % bắt buộc đặt trần Giảm tối đa để chặn đơn lớn giảm quá tay
    if (!maxDisc || maxDisc <= 0) e.maxDiscountAmount = 'Mã giảm % cần đặt trần Giảm tối đa (> 0).'
  } else {
    // Tiền cố định: cần Đơn tối thiểu ≥ giá trị giảm (tránh bán lỗ âm tiền)
    if (!Number.isNaN(dv) && !e.value) {
      if (minOrder <= 0) {
        // Chưa nhập đơn tối thiểu → yêu cầu nhập trước (báo ngay tại ô Đơn tối thiểu)
        e.minOrderValue = 'Mã tiền cố định cần nhập Đơn tối thiểu (≥ giá trị giảm).'
      } else if (dv > minOrder) {
        e.minOrderValue = 'Đơn tối thiểu phải ≥ giá trị giảm (tránh lỗ).'
      }
    }
  }

  // Đổi bằng điểm: bắt buộc nhập số điểm > 0
  if (v.allowPointExchange) {
    const pr = Number(v.pointsRequired)
    if (!v.pointsRequired || Number.isNaN(pr) || pr <= 0) e.pointsRequired = 'Nhập số điểm cần đổi (> 0).'
  }

  voucherErrors.value = e
  return Object.keys(e).length === 0
}

// Lưu (tạo / cập nhật) voucher (promotion) thật
const handleSaveVoucher = async () => {
  if (!validateVoucher()) {
    showToast('Vui lòng kiểm tra lại các trường được đánh dấu.', 'error')
    focusFirstVoucherError()
    return
  }
  isSavingVoucher.value = true
  try {
    const payload = {
      code: newVoucher.value.code.trim().toUpperCase(),
      name: newVoucher.value.title.trim(),
      description: newVoucher.value.description.trim(),
      discountType: newVoucher.value.type,
      discountValue: Number(newVoucher.value.value),
      endDate: newVoucher.value.expiry ? `${newVoucher.value.expiry}T23:59:59` : null,
      allowPointRedemption: !!newVoucher.value.allowPointExchange,
      pointsRequired: newVoucher.value.allowPointExchange ? Number(newVoucher.value.pointsRequired || 0) : 0,
      minOrderValue: Number(newVoucher.value.minOrderValue || 0),
      applicableMovieId: newVoucher.value.applicableMovieId || null,
      customerEligibility: newVoucher.value.customerEligibility || 'ALL',
      usageLimit: Number(newVoucher.value.usageLimit || 0),
      maxTicketQuantity: Number(newVoucher.value.maxTicketQuantity || 0),
      // Tiền cố định: trần giảm = chính giá trị giảm (đã tự đồng bộ)
      maxDiscountAmount: isFixed.value ? Number(newVoucher.value.value || 0) : Number(newVoucher.value.maxDiscountAmount || 0)
    }
    // Ngày bắt đầu: gửi khi TẠO MỚI hoặc SỬA mà ô chưa khóa; sửa-đang-chạy thì bỏ qua để BE giữ nguyên
    if (!editingVoucherId.value || !editStartLocked.value) {
      payload.startDate = newVoucher.value.startDate ? `${newVoucher.value.startDate}T00:00:00` : null
    }
    if (editingVoucherId.value) {
      await marketingApi.updatePromotion(editingVoucherId.value, payload)
      showToast('Cập nhật voucher thành công.')
    } else {
      await marketingApi.createPromotion(payload)
      showToast('Tạo voucher thành công.')
    }
    isVoucherDrawerOpen.value = false
    await fetchMarketingData()
  } catch (err) {
    // Trùng mã (409) -> gắn lỗi vào ô code cho rõ
    if (err.response?.status === 409) {
      voucherErrors.value = { ...voucherErrors.value, code: friendlyError(err, 'Mã code đã tồn tại.') }
      focusFirstVoucherError()
    }
    showToast(friendlyError(err, 'Lưu voucher thất bại.'), 'error')
  } finally {
    isSavingVoucher.value = false
  }
}

// Sao chép mã voucher nhanh
const handleCopyCode = async (code) => {
  if (!code) return
  try {
    await navigator.clipboard.writeText(code)
    showToast(`Đã sao chép mã ${code}.`)
  } catch (err) {
    showToast('Không thể sao chép mã.', 'error')
  }
}

// Xem chi tiết voucher
const detailTarget = ref(null)
const openDetail = (promo) => { detailTarget.value = promo }
const closeDetail = () => { detailTarget.value = null }

// Xoá voucher với xác nhận
const deleteTarget = ref(null)
const isDeleting = ref(false)
const askDeleteVoucher = (promo) => { deleteTarget.value = promo }
const confirmDeleteVoucher = async () => {
  if (!deleteTarget.value) return
  isDeleting.value = true
  try {
    await marketingApi.deletePromotion(deleteTarget.value.id)
    showToast('Đã xoá voucher.')
    deleteTarget.value = null
    await fetchMarketingData()
  } catch (err) {
    showToast(friendlyError(err, 'Xoá thất bại.'), 'error')
  } finally {
    isDeleting.value = false
  }
}

// ===== Gửi email chiến dịch (thông báo mã) tới khách theo đối tượng áp dụng =====
const emailTarget = ref(null)        // promotion đang chờ xác nhận gửi email
const isSendingCampaign = ref(false)
const eligibilityLabel = (val) => eligibilityOptions.find(o => o.value === val)?.label || 'Mọi khách hàng'
const movieTitleById = (id) => id ? (moviesList.value.find(m => m.id === id)?.title || 'Tất cả phim') : 'Tất cả phim'
// Tình trạng sử dụng voucher: %, màu theo mức dùng, số còn lại — cho thanh đo ở view chi tiết
const usageInfo = (promo) => {
  const used = Number(promo?.usedCount || 0)
  const limit = Number(promo?.usageLimit || 0)
  const limited = limit > 0
  const pct = limited ? Math.min(100, Math.round((used / limit) * 100)) : 0
  return {
    used, limit, limited, pct,
    remaining: limited ? Math.max(0, limit - used) : 0,
    exhausted: limited && used >= limit,
    bar: !limited ? 'bg-primary' : pct >= 90 ? 'bg-red-500' : pct >= 70 ? 'bg-amber-400' : 'bg-green-500',
    text: !limited ? 'text-on-surface' : pct >= 90 ? 'text-red-400' : pct >= 70 ? 'text-amber-400' : 'text-green-400',
  }
}
const askSendCampaign = (promo) => { emailTarget.value = promo }
const confirmSendCampaign = async () => {
  if (!emailTarget.value) return
  isSendingCampaign.value = true
  try {
    const { data } = await marketingApi.sendCampaign(emailTarget.value.id)
    showToast(data.message || `Đã gửi email tới ${data.sent ?? 0} khách hàng.`, data.sent > 0 ? 'success' : 'info')
    emailTarget.value = null
    await fetchMarketingData() // cập nhật lịch sử gửi trên card
  } catch (err) {
    showToast(friendlyError(err, 'Gửi email chiến dịch thất bại.'), 'error')
  } finally {
    isSendingCampaign.value = false
  }
}

// Phát voucher cho khách
const issueTarget = ref(null) // promotion đang phát
const customerResults = ref([])
const customerSearch = ref('')
const isSearchingCustomer = ref(false)
const isIssuing = ref(false)
let customerSearchTimer = null

const openIssueModal = (promo) => {
  issueTarget.value = promo
  customerSearch.value = ''
  customerResults.value = []
  fetchCustomers()
}
const closeIssueModal = () => { issueTarget.value = null }

const fetchCustomers = async () => {
  isSearchingCustomer.value = true
  try {
    const { data } = await customerApi.list(customerSearch.value)
    customerResults.value = data.data ?? data
  } catch (err) {
    customerResults.value = []
  } finally {
    isSearchingCustomer.value = false
  }
}
const handleCustomerSearchInput = () => {
  if (customerSearchTimer) clearTimeout(customerSearchTimer)
  customerSearchTimer = setTimeout(fetchCustomers, 400)
}

const handleIssueVoucher = async (customer) => {
  if (!issueTarget.value) return
  isIssuing.value = true
  try {
    await marketingApi.issueVoucher(issueTarget.value.id, customer.userId)
    showToast(`Đã phát voucher ${issueTarget.value.code} cho ${customer.fullName || 'khách'}.`)
    issueTarget.value = null
  } catch (err) {
    showToast(friendlyError(err, 'Phát voucher thất bại.'), 'error')
  } finally {
    isIssuing.value = false
  }
}

// Trạng thái hiệu lực suy ra từ ngày kết thúc
const promoStatus = (promo) => {
  if (!promo.endDate) return 'active'
  return new Date(promo.endDate) >= new Date() ? 'active' : 'expired'
}
const formatPromoDate = (iso) => {
  if (!iso) return 'Không giới hạn'
  return new Date(iso).toLocaleDateString('vi-VN')
}

const fetchMarketingData = async () => {
  // Tải voucher (promotions) độc lập với combo để một bên lỗi không làm hỏng bên kia
  try {
    const { data } = await marketingApi.getPromotions()
    // Promotion entity: code, discountType, discountValue, startDate, endDate, isStackable, pointsRequired, allowPointRedemption
    promotions.value = data.map(p => ({
      id: p.id,
      code: p.code,
      name: p.name,
      description: p.description,
      discountType: p.discountType,
      discountValue: p.discountValue,
      startDate: p.startDate,
      endDate: p.endDate,
      isStackable: p.isStackable,
      pointsRequired: p.pointsRequired,
      allowPointRedemption: p.allowPointRedemption,
      minOrderValue: p.minOrderValue,
      maxTicketQuantity: p.maxTicketQuantity,
      maxDiscountAmount: p.maxDiscountAmount,
      applicableMovieId: p.applicableMovieId,
      customerEligibility: p.customerEligibility,
      usageLimit: p.usageLimit,
      usedCount: p.usedCount,
      campaignSentAt: p.campaignSentAt,
      campaignSentCount: p.campaignSentCount
    })).sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
  } catch (error) {
    showToast('Không thể tải danh sách voucher.', 'error')
  }

  try {
    const { data } = await api.get('/marketing/combos')
    combos.value = data.map(c => ({
      id: c.id,
      name: c.name,
      price: c.price,
      description: c.description,
      items: Array.isArray(c.items) ? c.items : (c.items ? c.items.split(',') : []),
      status: c.active ? 'active' : 'inactive',
      image: '/images/Combo.webp' // Default image
    }))
  } catch (error) {
    // Combo chưa có dữ liệu/endpoint — không chặn tab Voucher
  }
}

onMounted(async () => {
  fetchMarketingData()
  fetchArticles()
  try {
    const { data } = await api.get('/movies')
    moviesList.value = Array.isArray(data) ? data : []
  } catch (error) {
    moviesList.value = []
  }
  try {
    const response = await api.get('/v1/cinemas')
    cinemasList.value = response.data
  } catch (error) {
    cinemasList.value = [
      { id: 1, name: 'DevCine Landmark 81' },
      { id: 2, name: 'DevCine Bitexco' }
    ]
  }
})

onUnmounted(() => {
  if (customerSearchTimer) clearTimeout(customerSearchTimer)
})
</script>

<template>
  <div class="p-10 space-y-8">
    <header class="flex justify-between items-end">
      <div>
        <h1 class="text-4xl font-extrabold tracking-tight font-headline uppercase italic text-primary">Marketing Hub</h1>
        <p class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold">Quản lý chiến dịch, mã giảm giá và combo ưu đãi</p>
      </div>
      <div class="flex gap-4">
        <button v-if="activeTab === 'vouchers' && can('promotions', 'add')" @click="openVoucherDrawer" class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
          <span class="material-symbols-outlined text-sm">add_card</span>
          Tạo Voucher
        </button>
        <button v-if="activeTab === 'combos' && can('promotions', 'add')" @click="openComboDrawer" class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
          <span class="material-symbols-outlined text-sm">lunch_dining</span>
          Tạo Combo
        </button>
        <button v-if="activeTab === 'articles' && can('promotions', 'add')" @click="openArticleDrawer" class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
          <span class="material-symbols-outlined text-sm">post_add</span>
          Tạo Tin Khuyến Mãi
        </button>
      </div>
    </header>

    <!-- Tabs -->
    <div class="flex gap-8 border-b border-outline-variant/10">
      <button @click="activeTab = 'vouchers'" :class="activeTab === 'vouchers' ? 'text-primary border-primary' : 'text-on-surface-variant border-transparent'" class="pb-4 font-black text-xs uppercase tracking-[0.2em] border-b-2 transition-all">
        Mã Giảm Giá (Vouchers)
      </button>
      <button @click="activeTab = 'combos'" :class="activeTab === 'combos' ? 'text-primary border-primary' : 'text-on-surface-variant border-transparent'" class="pb-4 font-black text-xs uppercase tracking-[0.2em] border-b-2 transition-all">
        Chương trình Combo
      </button>
      <button @click="activeTab = 'articles'" :class="activeTab === 'articles' ? 'text-primary border-primary' : 'text-on-surface-variant border-transparent'" class="pb-4 font-black text-xs uppercase tracking-[0.2em] border-b-2 transition-all">
        Tin Khuyến Mãi
      </button>
    </div>

    <!-- Vouchers View -->
    <div v-if="activeTab === 'vouchers'">
      <div v-if="promotions.length === 0" class="py-24 text-center border border-dashed border-outline-variant/20 rounded-2xl">
        <span class="material-symbols-outlined text-5xl text-on-surface-variant/40 mb-4">sell</span>
        <p class="text-on-surface-variant font-semibold">Chưa có voucher nào. Bấm "Tạo Voucher" để thêm mới.</p>
      </div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        <div v-for="promo in promotions" :key="promo.id"
          class="group relative flex flex-col rounded-2xl overflow-hidden border border-outline-variant/10 bg-surface-container-low hover:border-primary/40 hover:-translate-y-1 hover:shadow-xl hover:shadow-black/40 transition-all duration-300">
          <!-- Vầng sáng vàng tô điểm khi hover -->
          <div class="absolute -top-16 -right-16 w-40 h-40 rounded-full bg-primary/10 blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none"></div>

          <!-- ===== Hero: mức giảm ===== -->
          <div class="relative px-5 pt-4 pb-4 bg-gradient-to-br from-primary/10 to-transparent cursor-pointer" @click="openDetail(promo)" title="Bấm để xem chi tiết">
            <span class="material-symbols-outlined absolute -right-2 -bottom-3 text-[80px] leading-none text-primary/[0.07] select-none pointer-events-none">local_activity</span>

            <div class="relative flex items-center justify-between mb-2">
              <span class="inline-flex items-center gap-1 text-[9px] font-black uppercase tracking-[0.2em] text-on-surface-variant">
                <span class="material-symbols-outlined text-xs">sell</span> Voucher
              </span>
              <span :class="promoStatus(promo) === 'active' ? 'bg-green-500/15 text-green-400' : 'bg-red-500/15 text-red-400'" class="shrink-0 inline-flex items-center gap-1 text-[8px] font-black px-2.5 py-1 rounded-full uppercase tracking-widest">
                <span v-if="promoStatus(promo) === 'active'" class="w-1 h-1 rounded-full bg-green-400 animate-pulse"></span>
                {{ promoStatus(promo) === 'active' ? 'Đang chạy' : 'Hết hạn' }}
              </span>
            </div>

            <div class="relative flex items-end gap-1.5">
              <span class="text-3xl font-black text-primary leading-none tracking-tight">{{ promo.discountType === 'PERCENTAGE' ? Number(promo.discountValue) + '%' : Number(promo.discountValue).toLocaleString() + 'đ' }}</span>
              <span class="text-[9px] font-black text-on-surface-variant uppercase tracking-widest mb-1">Giảm</span>
            </div>
          </div>

          <!-- ===== Thân card ===== -->
          <div class="px-5 pt-3 pb-4 flex-1 flex flex-col cursor-pointer" @click="openDetail(promo)">
            <div class="-mx-5 mb-3 border-t border-outline-variant/10"></div>

            <!-- Tên voucher -->
            <h3 class="text-sm font-black text-on-surface uppercase italic leading-tight line-clamp-1 mb-3" :title="promo.name || 'Chưa đặt tên'">
              {{ promo.name || 'Voucher chưa đặt tên' }}
            </h3>

            <!-- Mã code + sao chép -->
            <div class="flex items-center gap-2 mb-3.5">
              <div class="flex-1 flex items-center gap-1.5 px-3 py-2 bg-black/25 rounded-xl border border-primary/20 min-w-0">
                <span class="material-symbols-outlined text-primary text-sm shrink-0">confirmation_number</span>
                <span class="font-black text-xs tracking-[0.15em] text-primary font-mono uppercase truncate">{{ promo.code }}</span>
              </div>
              <button @click.stop="handleCopyCode(promo.code)" title="Sao chép mã" class="w-9 h-9 rounded-xl bg-primary/10 hover:bg-primary text-primary hover:text-black flex items-center justify-center transition-colors shrink-0">
                <span class="material-symbols-outlined text-sm">content_copy</span>
              </button>
            </div>

            <!-- Thông tin phụ -->
            <div class="space-y-2 mt-auto">
              <div class="flex justify-between items-center">
                <span class="flex items-center gap-1.5 text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">
                  <span class="material-symbols-outlined text-xs">schedule</span> Hết hạn
                </span>
                <span class="text-[11px] font-bold" :class="promoStatus(promo) === 'active' ? 'text-on-surface' : 'text-red-400'">{{ formatPromoDate(promo.endDate) }}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="flex items-center gap-1.5 text-[10px] font-bold text-on-surface-variant uppercase tracking-wider">
                  <span class="material-symbols-outlined text-xs">stars</span> Đổi điểm
                </span>
                <span v-if="promo.allowPointRedemption" class="text-[11px] font-black text-amber-400">{{ Number(promo.pointsRequired).toLocaleString() }} điểm</span>
                <span v-else class="text-[11px] font-bold text-on-surface-variant/50">Tắt</span>
              </div>
            </div>
          </div>

          <!-- ===== Hành động ===== -->
          <div class="px-4 py-2.5 border-t border-outline-variant/10 flex justify-between items-center">
            <button v-if="can('promotions', 'add')" @click="openIssueModal(promo)" class="flex items-center gap-1.5 text-[9px] font-black text-primary uppercase tracking-widest hover:bg-primary/10 px-2.5 py-1.5 rounded-lg transition-colors">
              <span class="material-symbols-outlined text-xs">card_giftcard</span> Phát cho khách
            </button>
            <div class="flex items-center gap-1">
              <button v-if="can('promotions', 'edit') && !promo.allowPointRedemption" @click="askSendCampaign(promo)"
                :title="Number(promo.campaignSentCount || 0) > 0 ? `Đã gửi ${promo.campaignSentCount} khách — gửi thêm cho khách mới` : 'Gửi email chiến dịch'"
                class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors"
                :class="Number(promo.campaignSentCount || 0) > 0 ? 'bg-primary/10 text-primary hover:bg-primary/20' : 'hover:bg-primary/15 text-on-surface-variant hover:text-primary'">
                <span class="material-symbols-outlined text-sm">{{ Number(promo.campaignSentCount || 0) > 0 ? 'mark_email_read' : 'mail' }}</span>
              </button>
              <button v-if="can('promotions', 'edit')" @click="openEditVoucher(promo)" title="Chỉnh sửa" class="w-8 h-8 rounded-lg hover:bg-white/10 text-on-surface-variant hover:text-primary flex items-center justify-center transition-colors">
                <span class="material-symbols-outlined text-sm">edit</span>
              </button>
              <button v-if="can('promotions', 'delete')" @click="askDeleteVoucher(promo)" title="Xoá" class="w-8 h-8 rounded-lg hover:bg-red-500/15 text-on-surface-variant hover:text-red-400 flex items-center justify-center transition-colors">
                <span class="material-symbols-outlined text-sm">delete</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Combos View -->
    <div v-if="activeTab === 'combos'" class="grid grid-cols-1 lg:grid-cols-2 gap-8">
       <div v-for="combo in combos" :key="combo.id" class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden flex shadow-xl hover:border-primary/30 transition-all group">
          <div class="w-48 bg-surface-container-high relative overflow-hidden">
             <img :src="combo.image" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" />
             <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
             <div class="absolute bottom-4 left-4">
                <span class="text-lg font-black text-primary">{{ combo.price.toLocaleString() }}đ</span>
             </div>
          </div>
          <div class="flex-1 p-6 flex flex-col justify-between">
             <div>
                <div class="flex justify-between items-start mb-2">
                   <h3 class="text-xl font-black text-on-surface uppercase italic">{{ combo.name }}</h3>
                   <span :class="combo.status === 'active' ? 'bg-green-500/10 text-green-500' : 'bg-on-surface-variant/10 text-on-surface-variant'" class="text-[9px] font-black px-2 py-1 rounded uppercase">{{ combo.status }}</span>
                </div>
                <div class="flex flex-wrap gap-2 mt-4">
                   <span v-for="item in combo.items" :key="item" class="px-3 py-1 bg-surface-container-highest border border-outline-variant/10 rounded-full text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">
                      {{ item }}
                   </span>
                </div>
             </div>
             
             <div class="flex justify-between items-center pt-6 border-t border-outline-variant/5">
                <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">Áp dụng: Toàn hệ thống</span>
                <div class="flex gap-2">
                   <button v-if="can('promotions', 'edit')" class="p-2 hover:text-primary transition-colors"><span class="material-symbols-outlined text-lg">edit</span></button>
                   <button v-if="can('promotions', 'delete')" class="p-2 hover:text-red-500 transition-colors"><span class="material-symbols-outlined text-lg">delete</span></button>
                </div>
             </div>
          </div>
       </div>
    </div>

    <!-- Articles View -->
    <div v-if="activeTab === 'articles'" class="space-y-6">
      <!-- Search & Filter bar -->
      <div class="flex justify-between items-center bg-surface-container-low p-4 rounded-xl border border-outline-variant/10">
        <div class="relative w-80">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-xl">search</span>
          <input v-model="articleSearch" type="text" placeholder="Tìm kiếm tin khuyến mãi..." class="w-full bg-surface-container-highest border-none rounded-lg pl-10 pr-4 py-2 text-sm text-on-surface focus:ring-1 focus:ring-primary outline-none">
        </div>
        <div class="flex gap-4 w-48">
          <CustomSelect 
            v-model="filterStatus" 
            :options="statusOptions" 
            customClass="w-full px-4 py-2 rounded-lg text-sm border-none bg-surface-container-highest font-bold text-on-surface-variant" 
          />
        </div>
      </div>
      
      <!-- Table -->
      <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden shadow-xl">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-surface-container-highest/50 border-b border-outline-variant/10">
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant w-24">Hình ảnh</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thông tin</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thời gian áp dụng</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant text-center">Trạng thái</th>
              <th class="p-4 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <!-- Loading -->
            <tr v-if="isLoadingArticles" v-for="i in 3" :key="'sk' + i" class="border-b border-outline-variant/5">
              <td class="p-4"><div class="w-16 h-12 bg-surface-container-highest rounded animate-pulse"></div></td>
              <td class="p-4"><div class="h-4 w-48 bg-surface-container-highest rounded animate-pulse"></div></td>
              <td class="p-4"><div class="h-4 w-24 bg-surface-container-highest rounded animate-pulse"></div></td>
              <td class="p-4"></td>
              <td class="p-4"></td>
            </tr>
            <!-- Empty -->
            <tr v-else-if="filteredArticles.length === 0">
              <td colspan="5" class="py-16 text-center">
                <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 mb-2">campaign</span>
                <p class="text-on-surface-variant text-sm font-semibold">{{ articleSearch || filterStatus !== 'all' ? 'Không tìm thấy tin phù hợp.' : 'Chưa có tin khuyến mãi nào. Bấm "Tạo Tin Khuyến Mãi" để đăng tin.' }}</p>
              </td>
            </tr>
            <!-- Data -->
            <tr v-else v-for="article in filteredArticles" :key="article.id" class="border-b border-outline-variant/5 hover:bg-white/5 transition-colors group">
              <td class="p-4">
                <div class="w-16 h-12 bg-surface-container-highest rounded overflow-hidden flex items-center justify-center">
                  <img v-if="article.image" :src="article.image" class="w-full h-full object-cover" />
                  <span v-else class="material-symbols-outlined text-on-surface-variant/40 text-xl">image</span>
                </div>
              </td>
              <td class="p-4">
                <h4 class="text-sm font-black text-on-surface uppercase italic">{{ article.title }}</h4>
                <p class="text-xs text-on-surface-variant mt-1 line-clamp-1">{{ article.description }}</p>
              </td>
              <td class="p-4">
                <div class="flex flex-col gap-1">
                  <span class="text-xs text-on-surface font-mono">{{ article.startDate || '—' }}</span>
                  <span class="text-[10px] text-on-surface-variant font-mono">đến {{ article.endDate || '—' }}</span>
                </div>
              </td>
              <td class="p-4 text-center">
                <button v-if="can('promotions', 'edit')" @click="handleToggleArticle(article)" :class="article.status === 'active' ? 'text-green-400' : 'text-on-surface-variant'" class="material-symbols-outlined text-3xl transition-colors" :title="article.status === 'active' ? 'Đang hiển thị' : 'Đang ẩn'">
                  {{ article.status === 'active' ? 'toggle_on' : 'toggle_off' }}
                </button>
                <span v-else class="material-symbols-outlined text-3xl" :class="article.status === 'active' ? 'text-green-400' : 'text-on-surface-variant'">
                  {{ article.status === 'active' ? 'toggle_on' : 'toggle_off' }}
                </span>
              </td>
              <td class="p-4 text-right">
                <div class="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button v-if="can('promotions', 'edit')" @click="openEditArticle(article)" title="Chỉnh sửa" class="w-8 h-8 rounded-full bg-surface-container-highest hover:text-primary flex items-center justify-center transition-colors">
                    <span class="material-symbols-outlined text-sm">edit</span>
                  </button>
                  <button v-if="can('promotions', 'delete')" @click="articleDeleteTarget = article" title="Xoá" class="w-8 h-8 rounded-full bg-surface-container-highest hover:text-red-400 flex items-center justify-center transition-colors">
                    <span class="material-symbols-outlined text-sm">delete</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Voucher Modal Form -->
    <div v-if="isVoucherDrawerOpen" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isVoucherDrawerOpen = false"></div>

      <!-- Modal Panel -->
      <div class="relative w-full max-w-lg max-h-[90vh] bg-surface-container-low rounded-2xl overflow-hidden shadow-2xl flex flex-col border border-outline-variant/20 animate-in fade-in zoom-in-95 duration-200">
        <!-- Modal Header -->
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <div>
            <h3 class="font-headline font-black uppercase italic text-primary text-xl">{{ editingVoucherId ? 'Cập nhật Voucher' : 'Tạo Voucher' }}</h3>
            <p class="text-xs text-on-surface-variant mt-1 uppercase tracking-widest font-bold">{{ editingVoucherId ? 'Chỉnh sửa mã giảm giá' : 'Thêm mã giảm giá mới' }}</p>
          </div>
          <button @click="isVoucherDrawerOpen = false" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        
        <!-- Modal Body -->
        <div ref="voucherBodyRef" class="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-custom" @click="movieDropdownOpen = false">
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mã Code (Tự tạo)</label>
            <input v-model="newVoucher.code" @input="onCodeInput" maxlength="20" data-field="code" autocomplete="off" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none font-mono uppercase tracking-widest" :class="voucherErrors.code ? 'border-red-500' : 'border-outline-variant/20'" placeholder="VD: SUMMER2026" />
            <p v-if="voucherErrors.code" class="text-[10px] text-red-400 font-bold">{{ voucherErrors.code }}</p>
            <p v-else class="text-[10px] text-on-surface-variant/60 flex items-center gap-1">
              <span class="material-symbols-outlined text-[13px]">keyboard</span>
              Chỉ chữ &amp; số, không dấu. Nếu đang bật gõ tiếng Việt (Unikey/EVKey), hãy tắt khi nhập mã.
            </p>
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tiêu đề chiến dịch</label>
            <input v-model="newVoucher.title" @input="clearVErr('title')" maxlength="100" data-field="title" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="voucherErrors.title ? 'border-red-500' : 'border-outline-variant/20'" placeholder="VD: Khuyến mãi hè rực rỡ" />
            <p v-if="voucherErrors.title" class="text-[10px] text-red-400 font-bold">{{ voucherErrors.title }}</p>
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mô tả ngắn</label>
            <textarea v-model="newVoucher.description" @input="clearVErr('description')" rows="2" maxlength="255" data-field="description" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-medium text-on-surface focus:border-primary outline-none resize-none" :class="voucherErrors.description ? 'border-red-500' : 'border-outline-variant/20'" placeholder="Mô tả chi tiết voucher..."></textarea>
            <div class="flex justify-between">
              <p v-if="voucherErrors.description" class="text-[10px] text-red-400 font-bold">{{ voucherErrors.description }}</p>
              <span class="text-[10px] text-on-surface-variant/60 ml-auto">{{ (newVoucher.description || '').length }}/255</span>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Loại giảm giá</label>
              <CustomSelect 
                v-model="newVoucher.type" 
                :options="discountTypeOptions" 
                customClass="w-full p-4 rounded-xl text-sm border-outline-variant/20" 
              />
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Giá trị giảm {{ newVoucher.type === 'PERCENTAGE' ? '(%)' : '(VNĐ)' }}</label>
              <input :value="discountValueDisplay" @input="onDiscountValueInput" type="text" inputmode="numeric" data-field="value" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="(voucherErrors.value || discountValueError || pctLimitWarn) ? 'border-red-500' : 'border-outline-variant/20'" :placeholder="newVoucher.type === 'PERCENTAGE' ? '1 - 100' : 'VD: 20.000'" />
              <p v-if="voucherErrors.value || discountValueError || pctLimitWarn" class="text-[10px] text-red-400 font-bold">{{ voucherErrors.value || discountValueError || pctLimitWarn }}</p>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <!-- Ngày bắt đầu (khóa khi voucher đang chạy) -->
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngày bắt đầu</label>
              <template v-if="editStartLocked">
                <div class="w-full bg-surface-container-highest/50 border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface-variant/70 flex items-center gap-2 cursor-not-allowed">
                  <span class="material-symbols-outlined text-sm">lock</span>
                  {{ newVoucher.startDate ? new Date(newVoucher.startDate).toLocaleDateString('vi-VN') : 'Áp dụng ngay' }}
                </div>
                <p class="text-[10px] text-on-surface-variant/60">Đang chạy — không sửa được.</p>
              </template>
              <template v-else>
                <input v-model="newVoucher.startDate" @input="clearVErr('startDate')" type="date" :min="todayStr" data-field="startDate" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="voucherErrors.startDate ? 'border-red-500' : 'border-outline-variant/20'" />
                <p v-if="voucherErrors.startDate" class="text-[10px] text-red-400 font-bold">{{ voucherErrors.startDate }}</p>
                <p v-else class="text-[10px] text-on-surface-variant/60">Trống = áp dụng ngay.</p>
              </template>
            </div>
            <!-- Ngày hết hạn -->
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngày hết hạn</label>
              <input v-model="newVoucher.expiry" @input="clearVErr('expiry')" type="date" :min="endMinStr" data-field="expiry" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="voucherErrors.expiry ? 'border-red-500' : 'border-outline-variant/20'" />
              <p v-if="voucherErrors.expiry" class="text-[10px] text-red-400 font-bold">{{ voucherErrors.expiry }}</p>
            </div>
          </div>

          <!-- Điều kiện áp dụng nâng cao -->
          <div class="space-y-4 pt-4 border-t border-outline-variant/10">
            <p class="text-[10px] font-black uppercase tracking-widest text-primary flex items-center gap-2">
              <span class="material-symbols-outlined text-sm">rule</span> Điều kiện áp dụng
            </p>
            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Đơn tối thiểu (VNĐ)</label>
                <input :value="fmtThousand(newVoucher.minOrderValue)" @input="onMoneyInput($event, 'minOrderValue')" type="text" inputmode="numeric" data-field="minOrderValue" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="voucherErrors.minOrderValue ? 'border-red-500' : 'border-outline-variant/20'" placeholder="0 = không yêu cầu" />
                <p v-if="voucherErrors.minOrderValue" class="text-[10px] text-red-400 font-bold">{{ voucherErrors.minOrderValue }}</p>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Giới hạn lượt dùng</label>
                <input :value="fmtThousand(newVoucher.usageLimit)" @input="onMoneyInput($event, 'usageLimit')" type="text" inputmode="numeric" data-field="usageLimit" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="voucherErrors.usageLimit ? 'border-red-500' : 'border-outline-variant/20'" placeholder="0 = không giới hạn" />
                <p v-if="voucherErrors.usageLimit" class="text-[10px] text-red-400 font-bold">{{ voucherErrors.usageLimit }}</p>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Số vé tối đa được giảm / đơn</label>
                <input :value="newVoucher.maxTicketQuantity ?? ''" @input="onIntInput($event, 'maxTicketQuantity')" type="text" inputmode="numeric" data-field="maxTicketQuantity" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="(voucherErrors.maxTicketQuantity || maxTicketError) ? 'border-red-500' : 'border-outline-variant/20'" placeholder="0 = không giới hạn" />
                <p v-if="voucherErrors.maxTicketQuantity || maxTicketError" class="text-[10px] text-red-400 font-bold">{{ voucherErrors.maxTicketQuantity || maxTicketError }}</p>
                <p v-else class="text-[10px] text-on-surface-variant/70">Chỉ áp cho tối đa X vé đắt nhất; vé còn lại giá gốc.</p>
              </div>
              <div class="space-y-2">
                <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Giảm tối đa (VNĐ)</label>
                <input :value="fmtThousand(newVoucher.maxDiscountAmount)" @input="onMoneyInput($event, 'maxDiscountAmount')" :disabled="isFixed" type="text" inputmode="numeric" data-field="maxDiscountAmount" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none disabled:opacity-50 disabled:cursor-not-allowed" :class="voucherErrors.maxDiscountAmount ? 'border-red-500' : 'border-outline-variant/20'" placeholder="0 = không giới hạn" />
                <p v-if="voucherErrors.maxDiscountAmount" class="text-[10px] text-red-400 font-bold">{{ voucherErrors.maxDiscountAmount }}</p>
                <p v-else-if="isFixed" class="text-[10px] text-amber-400/80">Tự khoá = giá trị giảm (mã tiền cố định).</p>
              </div>
            </div>
            <div class="space-y-2 relative" @click.stop>
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Áp dụng theo phim</label>
              <button type="button" @click="movieDropdownOpen = !movieDropdownOpen"
                class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold focus:border-primary outline-none flex items-center justify-between text-left"
                :class="movieDropdownOpen ? 'border-primary' : ''">
                <span :class="newVoucher.applicableMovieId ? 'text-on-surface' : 'text-on-surface-variant/70'">{{ selectedMovieTitle }}</span>
                <span class="material-symbols-outlined text-lg text-on-surface-variant transition-transform" :class="{ 'rotate-180': movieDropdownOpen }">expand_more</span>
              </button>
              <div v-if="movieDropdownOpen" class="absolute z-20 left-0 right-0 top-full mt-2 bg-surface-container-high border border-outline-variant/20 rounded-xl shadow-2xl overflow-hidden">
                <div class="p-2 border-b border-outline-variant/10">
                  <input v-model="movieSearch" type="text" placeholder="Tìm tên phim..." autofocus
                    class="w-full bg-surface-container-lowest border border-outline-variant/20 px-3 py-2 rounded-lg text-sm text-on-surface focus:border-primary outline-none" />
                </div>
                <div class="max-h-56 overflow-y-auto py-1 scrollbar-custom">
                  <button type="button" @click="selectMovieForVoucher('')"
                    class="w-full text-left px-4 py-2.5 text-sm hover:bg-white/5 transition-colors"
                    :class="!newVoucher.applicableMovieId ? 'text-primary font-bold' : 'text-on-surface-variant'">Tất cả phim</button>
                  <button v-for="m in filteredMoviesList" :key="m.id" type="button" @click="selectMovieForVoucher(m.id)"
                    class="w-full text-left px-4 py-2.5 text-sm hover:bg-white/5 transition-colors truncate block"
                    :class="newVoucher.applicableMovieId === m.id ? 'text-primary font-bold' : 'text-on-surface-variant'">{{ m.title }}</button>
                  <div v-if="filteredMoviesList.length === 0" class="px-4 py-3 text-sm text-on-surface-variant/60 italic text-center">Không tìm thấy phim</div>
                </div>
              </div>
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Đối tượng áp dụng</label>
              <CustomSelect v-model="newVoucher.customerEligibility" :options="eligibilityOptions" customClass="w-full p-4 rounded-xl text-sm border-outline-variant/20" />
            </div>
          </div>

          <div class="space-y-4 pt-4 border-t border-outline-variant/10">
            <div class="bg-surface-container-highest rounded-xl border border-outline-variant/10 overflow-hidden transition-all duration-300">
              <div class="flex items-center justify-between p-4">
                <div>
                  <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Áp dụng đổi bằng điểm</p>
                  <p class="text-[10px] text-on-surface-variant mt-1 font-bold">Nếu tắt, người dùng nhập trực tiếp mã (Code) để sử dụng</p>
                </div>
                <button @click="newVoucher.allowPointExchange = !newVoucher.allowPointExchange" :class="newVoucher.allowPointExchange ? 'bg-green-500' : 'bg-surface-container-high'" class="relative w-10 h-5 rounded-full transition-colors duration-300 focus:outline-none shrink-0">
                  <span :class="newVoucher.allowPointExchange ? 'translate-x-5 bg-white' : 'translate-x-0 bg-on-surface-variant'" class="inline-block w-4 h-4 transform rounded-full transition-transform duration-300 shadow-md absolute top-0.5 left-0.5"></span>
                </button>
              </div>
              
              <div v-if="newVoucher.allowPointExchange" class="p-4 pt-2 border-t border-outline-variant/5 animate-in fade-in slide-in-from-top-2 space-y-3">
                <div class="flex items-start gap-2 p-3 rounded-lg bg-amber-500/10 border border-amber-500/20">
                  <span class="material-symbols-outlined text-amber-400 text-base shrink-0">warning</span>
                  <p class="text-[10px] text-amber-300/90 font-bold leading-relaxed">Khi BẬT: khách chỉ nhận mã bằng cách <b>đổi điểm</b> và mã <b>KHÔNG</b> nhập trực tiếp ở trang đặt vé được. Muốn khách nhập mã để dùng ngay thì <b>TẮT</b> mục này.</p>
                </div>
                <div>
                  <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-2 block">Số điểm cần đổi</label>
                  <input :value="newVoucher.pointsRequired ?? ''" @input="onIntInput($event, 'pointsRequired')" type="text" inputmode="numeric" data-field="pointsRequired" class="w-full bg-surface-container-lowest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="voucherErrors.pointsRequired ? 'border-red-500' : 'border-outline-variant/20'" placeholder="VD: 50" />
                  <p v-if="voucherErrors.pointsRequired" class="text-[10px] text-red-400 font-bold mt-1">{{ voucherErrors.pointsRequired }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="space-y-4 pt-4 border-t border-outline-variant/10">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant block mb-3">Cụm rạp áp dụng</label>
            <div class="flex gap-8">
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="newVoucher.cinemaMode" value="all" class="accent-primary">
                <span class="text-xs font-bold uppercase">Toàn hệ thống</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="newVoucher.cinemaMode" value="specific" class="accent-primary">
                <span class="text-xs font-bold uppercase">Cụm rạp riêng</span>
              </label>
            </div>
            
            <div v-if="newVoucher.cinemaMode === 'specific'" class="mt-2 p-4 bg-surface-container-highest rounded-xl border border-outline-variant/10 space-y-2">
              <div class="relative">
                <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-lg pointer-events-none">search</span>
                <input v-model="cinemaSearch" type="text" placeholder="Tìm theo tên rạp..."
                  class="w-full bg-surface-container-lowest border border-outline-variant/20 pl-10 pr-3 py-2.5 rounded-lg text-sm text-on-surface focus:border-primary outline-none" />
              </div>
              <div class="flex flex-col gap-1 max-h-52 overflow-y-auto scrollbar-custom">
                <label v-for="cinema in filteredCinemasList" :key="cinema.id" class="flex items-center gap-2 cursor-pointer p-2 hover:bg-white/5 rounded transition-colors">
                  <input type="checkbox" :value="cinema.id" v-model="newVoucher.selectedCinemas" class="accent-primary">
                  <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">{{ cinema.name }}</span>
                </label>
                <div v-if="filteredCinemasList.length === 0" class="px-2 py-3 text-xs text-on-surface-variant/60 italic text-center">Không tìm thấy rạp</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Drawer Footer -->
        <div class="p-6 border-t border-outline-variant/10 bg-surface-container-lowest flex gap-4">
          <button @click="isVoucherDrawerOpen = false" class="flex-1 px-6 py-4 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy bỏ</button>
          <button @click="handleSaveVoucher" :disabled="isSavingVoucher || !!discountValueError || !!maxTicketError" class="flex-1 px-6 py-4 rounded-xl bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform shadow-xl shadow-primary/20 disabled:opacity-40 disabled:grayscale disabled:cursor-not-allowed disabled:hover:scale-100">{{ isSavingVoucher ? 'Đang lưu...' : 'Lưu Voucher' }}</button>
        </div>
      </div>
    </div>
    <!-- Combo Drawer Form -->
    <div v-if="isComboDrawerOpen" class="fixed inset-0 z-[1000] flex justify-end">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isComboDrawerOpen = false"></div>
      
      <!-- Drawer Panel -->
      <div class="relative w-full max-w-md bg-surface-container-low h-full shadow-2xl flex flex-col border-l border-outline-variant/20 animate-in slide-in-from-right duration-300">
        <!-- Drawer Header -->
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <div>
            <h3 class="font-headline font-black uppercase italic text-primary text-xl">Tạo Combo</h3>
            <p class="text-xs text-on-surface-variant mt-1 uppercase tracking-widest font-bold">Thêm Combo Bắp Nước mới</p>
          </div>
          <button @click="isComboDrawerOpen = false" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        
        <!-- Drawer Body -->
        <div class="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-custom">
          <!-- Image Upload Mock -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ảnh Combo</label>
            <div class="w-full h-32 bg-surface-container-highest border-2 border-dashed border-outline-variant/20 rounded-2xl flex flex-col items-center justify-center text-on-surface-variant hover:border-primary/50 hover:bg-primary/5 transition-colors cursor-pointer">
              <span class="material-symbols-outlined text-3xl mb-2">cloud_upload</span>
              <span class="text-[10px] font-bold uppercase tracking-widest">Tải ảnh lên</span>
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tên Combo</label>
            <input v-model="newCombo.name" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none uppercase" placeholder="VD: COMBO COUPLE" />
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Giá bán (VNĐ)</label>
            <input v-model="newCombo.price" type="number" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" placeholder="VD: 159000" />
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mô tả ngắn</label>
            <textarea v-model="newCombo.description" rows="2" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-medium text-on-surface focus:border-primary outline-none resize-none" placeholder="Mô tả thành phần..."></textarea>
          </div>

          <!-- Trạng thái -->
          <div class="flex items-center justify-between p-4 bg-surface-container-highest rounded-xl border border-outline-variant/10">
             <div>
                <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Trạng thái hiển thị</p>
             </div>
             <button @click="newCombo.status = newCombo.status === 'active' ? 'inactive' : 'active'" :class="newCombo.status === 'active' ? 'bg-green-500' : 'bg-surface-container-high'" class="relative w-10 h-5 rounded-full transition-colors duration-300 focus:outline-none shrink-0">
                <span :class="newCombo.status === 'active' ? 'translate-x-5 bg-white' : 'translate-x-0 bg-on-surface-variant'" class="inline-block w-4 h-4 transform rounded-full transition-transform duration-300 shadow-md absolute top-0.5 left-0.5"></span>
             </button>
          </div>

          <!-- Dynamic List: Items -->
          <div class="space-y-4 pt-4 border-t border-outline-variant/10">
            <div class="flex items-center justify-between">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Danh sách Món (Items)</label>
              <button @click="addComboItem" class="text-xs font-bold text-primary hover:text-white transition-colors flex items-center gap-1 uppercase tracking-widest">
                 <span class="material-symbols-outlined text-sm">add</span> Thêm món
              </button>
            </div>
            
            <div class="space-y-3">
              <div v-for="(item, index) in newCombo.items" :key="item.id" class="flex gap-2 items-center">
                <div class="flex-1 bg-surface-container-highest rounded-xl flex overflow-hidden border border-outline-variant/10">
                   <input v-model="item.quantity" type="number" min="1" class="w-16 bg-transparent p-3 text-sm font-bold text-center border-r border-outline-variant/10 outline-none text-primary" placeholder="SL" />
                   <input v-model="item.name" class="flex-1 bg-transparent p-3 text-sm font-bold outline-none text-on-surface" placeholder="Tên món (VD: Bắp lớn)" />
                </div>
                <button @click="removeComboItem(index)" :disabled="newCombo.items.length === 1" class="w-10 h-10 flex-shrink-0 flex items-center justify-center rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500 hover:text-white transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                  <span class="material-symbols-outlined text-sm">delete</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Drawer Footer -->
        <div class="p-6 border-t border-outline-variant/10 bg-surface-container-lowest flex gap-4">
          <button @click="isComboDrawerOpen = false" class="flex-1 px-6 py-4 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy bỏ</button>
          <button class="flex-1 px-6 py-4 rounded-xl bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform shadow-xl shadow-primary/20">Lưu Combo</button>
        </div>
      </div>
    </div>

    <!-- Article Drawer Form -->
    <div v-if="isArticleDrawerOpen" class="fixed inset-0 z-[1000] flex justify-end">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isArticleDrawerOpen = false"></div>
      
      <!-- Drawer Panel -->
      <div class="relative w-full max-w-xl bg-surface-container-low h-full shadow-2xl flex flex-col border-l border-outline-variant/20 animate-in slide-in-from-right duration-300">
        <!-- Drawer Header -->
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <div>
            <h3 class="font-headline font-black uppercase italic text-primary text-xl">{{ editingArticleId ? 'Sửa Tin Khuyến Mãi' : 'Thêm Tin Khuyến Mãi' }}</h3>
            <p class="text-xs text-on-surface-variant mt-1 uppercase tracking-widest font-bold">Nội dung hiển thị ở trang Khuyến mãi</p>
          </div>
          <button @click="isArticleDrawerOpen = false" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        
        <!-- Drawer Body (Scrollable) -->
        <div class="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-custom">
          <!-- Image Upload (Cloudinary) -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ảnh Banner / Thumbnail</label>
            <label class="relative block w-full h-40 bg-surface-container-highest border-2 border-dashed border-outline-variant/20 rounded-2xl overflow-hidden flex flex-col items-center justify-center text-on-surface-variant hover:border-primary/50 hover:bg-primary/5 transition-colors cursor-pointer">
              <img v-if="newArticle.image" :src="newArticle.image" class="absolute inset-0 w-full h-full object-cover" />
              <div v-if="newArticle.image" class="absolute inset-0 bg-black/40 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center">
                <span class="text-xs font-bold uppercase tracking-widest text-white">Đổi ảnh khác</span>
              </div>
              <template v-if="!newArticle.image">
                <span v-if="isUploadingArticleImage" class="material-symbols-outlined text-3xl mb-2 animate-spin">progress_activity</span>
                <span v-else class="material-symbols-outlined text-3xl mb-2">cloud_upload</span>
                <span class="text-xs font-bold uppercase tracking-widest">{{ isUploadingArticleImage ? 'Đang tải lên...' : 'Click để tải ảnh lên' }}</span>
              </template>
              <input type="file" accept="image/*" class="hidden" @change="handleArticleImageUpload" :disabled="isUploadingArticleImage" />
            </label>
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tiêu đề Tin Khuyến Mãi</label>
            <input v-model="newArticle.title" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" placeholder="VD: Khuyến mãi Hè rực rỡ" />
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mô tả ngắn</label>
            <textarea v-model="newArticle.description" rows="2" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-medium text-on-surface focus:border-primary outline-none resize-none" placeholder="Mô tả tóm tắt hiển thị ở danh sách ngoài trang chủ..."></textarea>
          </div>

          <div class="grid grid-cols-2 gap-6">
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngày bắt đầu</label>
              <input v-model="newArticle.startDate" type="date" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" />
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngày kết thúc</label>
              <input v-model="newArticle.endDate" type="date" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" />
            </div>
          </div>

          <!-- Trạng thái -->
          <div class="flex items-center justify-between p-4 bg-surface-container-highest rounded-xl border border-outline-variant/10">
             <div>
                <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Trạng thái hiển thị</p>
                <p class="text-xs text-on-surface-variant mt-1">Cho phép hiển thị tin tức này trên hệ thống website</p>
             </div>
             <button @click="newArticle.status = newArticle.status === 'active' ? 'inactive' : 'active'" :class="newArticle.status === 'active' ? 'bg-green-500' : 'bg-surface-container-high'" class="relative w-10 h-5 rounded-full transition-colors duration-300 focus:outline-none shrink-0">
                <span :class="newArticle.status === 'active' ? 'translate-x-5 bg-white' : 'translate-x-0 bg-on-surface-variant'" class="inline-block w-4 h-4 transform rounded-full transition-transform duration-300 shadow-md absolute top-0.5 left-0.5"></span>
             </button>
          </div>

          <!-- Rich Text Mock -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Nội dung chi tiết</label>
            <div class="bg-surface-container-highest border border-outline-variant/20 rounded-xl overflow-hidden flex flex-col h-64">
              <!-- Toolbar -->
              <div class="bg-surface-container-lowest border-b border-outline-variant/10 p-2 flex gap-1 items-center">
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">format_bold</span></button>
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">format_italic</span></button>
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">format_underlined</span></button>
                 <div class="w-px h-5 bg-outline-variant/20 mx-1"></div>
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">format_list_bulleted</span></button>
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">image</span></button>
                 <button class="w-8 h-8 rounded hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors"><span class="material-symbols-outlined text-sm">link</span></button>
              </div>
              <textarea v-model="newArticle.content" class="flex-1 w-full bg-transparent p-4 text-sm font-medium text-on-surface outline-none resize-none scrollbar-custom" placeholder="Soạn thảo nội dung chi tiết của chương trình khuyến mãi..."></textarea>
            </div>
          </div>
        </div>

        <!-- Drawer Footer -->
        <div class="p-6 border-t border-outline-variant/10 bg-surface-container-lowest flex gap-4">
          <button @click="isArticleDrawerOpen = false" class="flex-1 px-6 py-4 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy bỏ</button>
          <button @click="handleSaveArticle" :disabled="isSavingArticle || isUploadingArticleImage" class="flex-1 px-6 py-4 rounded-xl bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform shadow-xl shadow-primary/20 disabled:opacity-60">{{ isSavingArticle ? 'Đang lưu...' : (editingArticleId ? 'Cập nhật tin' : 'Đăng tin') }}</button>
        </div>
      </div>
    </div>

    <!-- Issue voucher to customer modal -->
    <div v-if="issueTarget" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="closeIssueModal"></div>
      <div class="relative w-full max-w-lg bg-surface-container-low border border-outline-variant/20 rounded-2xl shadow-2xl flex flex-col max-h-[80vh]">
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center">
          <div>
            <h3 class="font-headline font-black uppercase italic text-primary text-lg">Phát voucher cho khách</h3>
            <p class="text-xs text-on-surface-variant mt-1 font-bold">Mã: <span class="font-mono text-primary">{{ issueTarget.code }}</span></p>
          </div>
          <button @click="closeIssueModal" class="w-9 h-9 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="p-6 pb-3">
          <div class="relative">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-xl">search</span>
            <input v-model="customerSearch" @input="handleCustomerSearchInput" type="text" placeholder="Tìm khách theo tên, email, SĐT..." class="w-full bg-surface-container-highest border-none rounded-lg pl-10 pr-4 py-2.5 text-sm text-on-surface focus:ring-1 focus:ring-primary outline-none" />
          </div>
        </div>
        <div class="flex-1 overflow-y-auto px-6 pb-6 space-y-2 scrollbar-custom">
          <div v-if="isSearchingCustomer" class="space-y-2">
            <div v-for="i in 4" :key="i" class="h-14 bg-surface-container-high rounded-lg animate-pulse"></div>
          </div>
          <div v-else-if="customerResults.length === 0" class="py-10 text-center text-on-surface-variant text-sm">Không tìm thấy khách hàng.</div>
          <div v-else v-for="c in customerResults" :key="c.userId" class="flex items-center justify-between gap-3 bg-surface-container-high rounded-lg p-3 border border-white/5">
            <div class="min-w-0">
              <p class="text-sm font-bold text-on-surface truncate">{{ c.fullName || 'Khách hàng' }}</p>
              <p class="text-[11px] text-on-surface-variant truncate">{{ c.email }} · {{ c.phone || 'N/A' }}</p>
            </div>
            <button @click="handleIssueVoucher(c)" :disabled="isIssuing" class="shrink-0 text-[10px] font-bold uppercase tracking-widest px-3 py-2 bg-primary text-on-primary rounded hover:brightness-110 transition-all disabled:opacity-60">Phát</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Voucher detail modal -->
    <div v-if="detailTarget" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="closeDetail"></div>
      <div class="relative w-full max-w-md bg-surface-container-low border border-outline-variant/20 rounded-2xl shadow-2xl flex flex-col max-h-[85vh]">
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-start gap-4">
          <div class="min-w-0">
            <div class="flex items-center gap-3 mb-2">
              <span class="font-black text-lg tracking-widest text-primary font-mono uppercase px-3 py-1 bg-primary/10 rounded-lg border border-primary/20">{{ detailTarget.code }}</span>
              <span :class="promoStatus(detailTarget) === 'active' ? 'bg-green-500/15 text-green-400' : 'bg-red-500/15 text-red-400'" class="text-[9px] font-black px-2.5 py-1 rounded uppercase tracking-widest">
                {{ promoStatus(detailTarget) === 'active' ? 'Đang chạy' : 'Hết hạn' }}
              </span>
            </div>
            <h3 class="font-headline font-black uppercase italic text-on-surface text-lg truncate">{{ detailTarget.name || 'Voucher chưa đặt tên' }}</h3>
          </div>
          <button @click="closeDetail" class="w-9 h-9 shrink-0 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-custom">
          <!-- Mô tả -->
          <div v-if="detailTarget.description" class="space-y-1">
            <p class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Mô tả</p>
            <p class="text-sm text-on-surface whitespace-pre-line leading-relaxed">{{ detailTarget.description }}</p>
          </div>

          <!-- NHÓM 1 · Giá trị giảm -->
          <section class="space-y-2.5">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-base">sell</span>
              <span class="text-[10px] font-black uppercase tracking-widest text-primary">Giá trị giảm</span>
            </div>
            <div class="rounded-xl bg-surface-container p-4">
              <div class="flex items-end justify-between gap-3">
                <div>
                  <p class="text-[10px] uppercase tracking-wider text-on-surface-variant/60 mb-1">Mức giảm</p>
                  <p class="text-3xl font-black text-primary leading-none">{{ detailTarget.discountType === 'PERCENTAGE' ? Number(detailTarget.discountValue) + '%' : Number(detailTarget.discountValue).toLocaleString() + 'đ' }}</p>
                </div>
                <div class="text-right">
                  <p class="text-[10px] uppercase tracking-wider text-on-surface-variant/60 mb-1">Loại giảm</p>
                  <p class="text-sm font-bold text-on-surface">{{ detailTarget.discountType === 'PERCENTAGE' ? 'Phần trăm' : 'Tiền cố định' }}</p>
                </div>
              </div>
              <div class="mt-3 pt-3 border-t border-outline-variant/10 flex justify-between items-center">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Giảm tối đa</span>
                <span class="text-sm font-bold" :class="Number(detailTarget.maxDiscountAmount || 0) > 0 ? 'text-on-surface' : 'text-on-surface-variant/50'">{{ Number(detailTarget.maxDiscountAmount || 0) > 0 ? Number(detailTarget.maxDiscountAmount).toLocaleString() + 'đ' : 'Không giới hạn' }}</span>
              </div>
            </div>
          </section>

          <!-- NHÓM 2 · Điều kiện áp dụng -->
          <section class="space-y-2.5">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-base">rule</span>
              <span class="text-[10px] font-black uppercase tracking-widest text-primary">Điều kiện áp dụng</span>
            </div>
            <div class="rounded-xl bg-surface-container p-4 space-y-2.5">
              <div class="flex justify-between items-center gap-3">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Đơn tối thiểu</span>
                <span class="text-sm font-bold" :class="Number(detailTarget.minOrderValue || 0) > 0 ? 'text-on-surface' : 'text-on-surface-variant/50'">{{ Number(detailTarget.minOrderValue || 0) > 0 ? Number(detailTarget.minOrderValue).toLocaleString() + 'đ' : 'Không yêu cầu' }}</span>
              </div>
              <div class="flex justify-between items-center gap-3">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Số vé tối đa được giảm</span>
                <span class="text-sm font-bold" :class="Number(detailTarget.maxTicketQuantity || 0) > 0 ? 'text-on-surface' : 'text-on-surface-variant/50'">{{ Number(detailTarget.maxTicketQuantity || 0) > 0 ? Number(detailTarget.maxTicketQuantity).toLocaleString() + ' vé' : 'Không giới hạn' }}</span>
              </div>
              <div class="flex justify-between items-center gap-3">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Đối tượng áp dụng</span>
                <span class="text-sm font-bold text-on-surface text-right">{{ eligibilityLabel(detailTarget.customerEligibility) }}</span>
              </div>
              <div class="flex justify-between items-center gap-3">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60 shrink-0">Áp dụng theo phim</span>
                <span class="text-sm font-bold text-on-surface truncate text-right" :title="movieTitleById(detailTarget.applicableMovieId)">{{ movieTitleById(detailTarget.applicableMovieId) }}</span>
              </div>
              <div class="flex justify-between items-center gap-3">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Cộng dồn với mã khác</span>
                <span class="text-sm font-bold text-on-surface">{{ detailTarget.isStackable ? 'Có' : 'Không' }}</span>
              </div>
            </div>
          </section>

          <!-- NHÓM 3 · Thời gian & Đổi điểm -->
          <section class="space-y-2.5">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-base">schedule</span>
              <span class="text-[10px] font-black uppercase tracking-widest text-primary">Thời gian & Đổi điểm</span>
            </div>
            <div class="rounded-xl bg-surface-container p-4 space-y-2.5">
              <div class="flex justify-between items-center gap-3">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Bắt đầu</span>
                <span class="text-sm font-bold text-on-surface">{{ detailTarget.startDate ? formatPromoDate(detailTarget.startDate) : 'Áp dụng ngay' }}</span>
              </div>
              <div class="flex justify-between items-center gap-3">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Hết hạn</span>
                <span class="text-sm font-bold" :class="promoStatus(detailTarget) === 'active' ? 'text-on-surface' : 'text-red-400'">{{ formatPromoDate(detailTarget.endDate) }}</span>
              </div>
              <div class="flex justify-between items-center gap-3 pt-2.5 border-t border-outline-variant/10">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Đổi bằng điểm</span>
                <span v-if="detailTarget.allowPointRedemption" class="text-sm font-black text-amber-400">{{ Number(detailTarget.pointsRequired).toLocaleString() }} điểm</span>
                <span v-else class="text-sm font-bold text-on-surface-variant/50">Tắt</span>
              </div>
            </div>
          </section>

          <!-- NHÓM 4 · Tình trạng sử dụng -->
          <section class="space-y-2.5">
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-primary text-base">insights</span>
              <span class="text-[10px] font-black uppercase tracking-widest text-primary">Tình trạng sử dụng</span>
            </div>
            <div class="rounded-xl bg-surface-container p-4">
              <div class="flex items-baseline justify-between mb-2.5">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Số lượt đã dùng</span>
                <span class="text-sm font-black" :class="usageInfo(detailTarget).text">
                  {{ usageInfo(detailTarget).used.toLocaleString() }}<template v-if="usageInfo(detailTarget).limited">/{{ usageInfo(detailTarget).limit.toLocaleString() }}</template>
                  <span class="text-on-surface-variant/60 font-bold text-xs"> lượt</span>
                </span>
              </div>
              <template v-if="usageInfo(detailTarget).limited">
                <div class="h-2 rounded-full bg-white/[0.08] ring-1 ring-white/5 overflow-hidden">
                  <div class="h-full rounded-full transition-all duration-500" :class="usageInfo(detailTarget).bar" :style="{ width: usageInfo(detailTarget).pct + '%' }"></div>
                </div>
                <div class="flex justify-between items-center mt-1.5">
                  <span class="text-[10px] font-bold" :class="usageInfo(detailTarget).exhausted ? 'text-red-400' : 'text-on-surface-variant/60'">
                    {{ usageInfo(detailTarget).exhausted ? 'Đã dùng hết lượt' : 'Còn ' + usageInfo(detailTarget).remaining.toLocaleString() + ' lượt' }}
                  </span>
                  <span class="text-[10px] font-black" :class="usageInfo(detailTarget).text">{{ usageInfo(detailTarget).pct }}%</span>
                </div>
              </template>
              <p v-else class="text-[11px] font-bold text-on-surface-variant/60">Không giới hạn lượt sử dụng</p>
              <!-- Lịch sử gửi email chiến dịch (mã nhập trực tiếp) -->
              <div v-if="!detailTarget.allowPointRedemption" class="mt-3 pt-3 border-t border-outline-variant/10 flex justify-between items-center gap-3">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Email chiến dịch</span>
                <span class="text-sm font-bold" :class="Number(detailTarget.campaignSentCount || 0) > 0 ? 'text-on-surface' : 'text-on-surface-variant/50'">
                  {{ Number(detailTarget.campaignSentCount || 0) > 0 ? `Đã gửi ${Number(detailTarget.campaignSentCount).toLocaleString()} khách · ${formatPromoDate(detailTarget.campaignSentAt)}` : 'Chưa gửi' }}
                </span>
              </div>
            </div>
          </section>
        </div>
        <div class="p-6 border-t border-outline-variant/10 flex gap-3">
          <button @click="openIssueModal(detailTarget); closeDetail()" class="flex-1 px-4 py-3 rounded-xl bg-primary/10 text-primary text-[10px] font-bold uppercase tracking-widest hover:bg-primary/20 transition-colors flex items-center justify-center gap-1">
            <span class="material-symbols-outlined text-sm">card_giftcard</span> Phát cho khách
          </button>
          <button @click="openEditVoucher(detailTarget); closeDetail()" class="flex-1 px-4 py-3 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors flex items-center justify-center gap-1">
            <span class="material-symbols-outlined text-sm">edit</span> Chỉnh sửa
          </button>
        </div>
      </div>
    </div>

    <!-- Delete confirm modal -->
    <div v-if="deleteTarget" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="deleteTarget = null"></div>
      <div class="relative w-full max-w-sm bg-surface-container-low border border-white/10 rounded-2xl p-6 shadow-2xl text-center">
        <span class="material-symbols-outlined text-4xl text-red-400 mb-3">warning</span>
        <h3 class="text-lg font-bold font-headline text-white mb-2">Xoá voucher?</h3>
        <p class="text-sm text-on-surface-variant">Bạn chắc chắn muốn xoá mã <span class="font-mono font-bold text-primary">{{ deleteTarget.code }}</span>? Hành động này không thể hoàn tác.</p>
        <div class="flex gap-3 mt-6">
          <button @click="deleteTarget = null" class="flex-1 px-4 py-3 rounded-xl border border-white/15 text-xs font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Huỷ</button>
          <button @click="confirmDeleteVoucher" :disabled="isDeleting" class="flex-1 px-4 py-3 rounded-xl bg-red-500 text-white text-xs font-bold uppercase tracking-widest hover:brightness-110 transition-all disabled:opacity-60">{{ isDeleting ? 'Đang xoá...' : 'Xoá' }}</button>
        </div>
      </div>
    </div>

    <!-- Send campaign email confirm modal -->
    <div v-if="emailTarget" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="emailTarget = null"></div>
      <div class="relative w-full max-w-sm bg-surface-container-low border border-white/10 rounded-2xl p-6 shadow-2xl text-center">
        <span class="material-symbols-outlined text-4xl text-primary mb-3">mark_email_read</span>
        <h3 class="text-lg font-bold font-headline text-white mb-2">{{ Number(emailTarget.campaignSentCount || 0) > 0 ? 'Gửi lại chiến dịch?' : 'Gửi email chiến dịch?' }}</h3>
        <p class="text-sm text-on-surface-variant">
          Gửi email kèm mã <span class="font-mono font-bold text-primary">{{ emailTarget.code }}</span>
          tới <span class="font-bold text-on-surface">khách thuộc "{{ eligibilityLabel(emailTarget.customerEligibility) }}"</span> có email.
        </p>
        <!-- Đã gửi trước đó → cảnh báo dedup -->
        <div v-if="Number(emailTarget.campaignSentCount || 0) > 0" class="mt-3 p-3 rounded-lg bg-amber-500/10 border border-amber-500/25 text-left flex items-start gap-2">
          <span class="material-symbols-outlined text-amber-400 text-base shrink-0">history</span>
          <p class="text-[11px] text-amber-300/90 font-bold leading-relaxed">
            Đã gửi cho {{ Number(emailTarget.campaignSentCount).toLocaleString() }} khách ({{ formatPromoDate(emailTarget.campaignSentAt) }}).
            Gửi lại <b>chỉ gửi cho khách CHƯA nhận</b> mã này — không gửi trùng.
          </p>
        </div>
        <p class="text-[11px] text-on-surface-variant/70 mt-2">Hành động này gửi email thật, không thể thu hồi.</p>
        <div class="flex gap-3 mt-6">
          <button @click="emailTarget = null" class="flex-1 px-4 py-3 rounded-xl border border-white/15 text-xs font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Huỷ</button>
          <button @click="confirmSendCampaign" :disabled="isSendingCampaign" class="flex-1 px-4 py-3 rounded-xl bg-primary text-on-primary text-xs font-bold uppercase tracking-widest hover:brightness-110 transition-all disabled:opacity-60">{{ isSendingCampaign ? 'Đang gửi...' : 'Gửi email' }}</button>
        </div>
      </div>
    </div>

    <!-- Delete article confirm modal -->
    <div v-if="articleDeleteTarget" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="articleDeleteTarget = null"></div>
      <div class="relative w-full max-w-sm bg-surface-container-low border border-white/10 rounded-2xl p-6 shadow-2xl text-center">
        <span class="material-symbols-outlined text-4xl text-red-400 mb-3">warning</span>
        <h3 class="text-lg font-bold font-headline text-white mb-2">Xoá tin khuyến mãi?</h3>
        <p class="text-sm text-on-surface-variant">Bạn chắc chắn muốn xoá tin <span class="font-bold text-primary">{{ articleDeleteTarget.title }}</span>? Hành động này không thể hoàn tác.</p>
        <div class="flex gap-3 mt-6">
          <button @click="articleDeleteTarget = null" class="flex-1 px-4 py-3 rounded-xl border border-white/15 text-xs font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Huỷ</button>
          <button @click="confirmDeleteArticle" :disabled="isDeletingArticle" class="flex-1 px-4 py-3 rounded-xl bg-red-500 text-white text-xs font-bold uppercase tracking-widest hover:brightness-110 transition-all disabled:opacity-60">{{ isDeletingArticle ? 'Đang xoá...' : 'Xoá' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(245, 197, 24, 0.2);
  border-radius: 10px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(245, 197, 24, 0.5);
}
</style>
