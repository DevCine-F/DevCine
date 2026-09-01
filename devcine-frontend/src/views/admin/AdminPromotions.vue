<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import axios from 'axios'
import api from '@/api/axios'
import { marketingApi, customerApi, promoArticleApi } from '@/api/admin/index'
import CustomSelect from '@/components/common/CustomSelect.vue'
import TipTapEditor from '@/components/common/TipTapEditor.vue'
import { prepareImageForUpload } from '@/utils/imageUpload'
import { useAdminPerm } from '@/composables/useAdminPerm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'
import { parseMarkdownToHtml } from '@/utils/markdownParser'

const { can } = useAdminPerm()
const toastStore = useToastStore()
// Hôm nay (YYYY-MM-DD, theo giờ local) — chặn chọn ngày hết hạn trong quá khứ và tính trạng thái hiệu lực
const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

const filterStatus = ref('all')
const statusOptions = [
  { value: 'all', label: 'Tất cả trạng thái' },
  { value: 'active', label: 'Đang hiển thị' },
  { value: 'scheduled', label: 'Chưa tới hạn' },
  { value: 'expired', label: 'Đã hết hạn' },
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

const eligibilityOptions = [
  { value: 'ALL', label: 'Mọi khách hàng' },
  { value: 'NEW_CUSTOMER', label: 'Chỉ khách hàng mới' },
  { value: 'TIER_SILVER', label: 'Khách thân thiết (hạng Bạc trở lên)' },
  { value: 'TIER_GOLD', label: 'Khách VIP (hạng Vàng trở lên)' },
  { value: 'TIER_PLATINUM', label: 'Khách VIP (hạng Bạch Kim trở lên)' }
]

const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080') + '/api/marketing'

const isVoucherDrawerOpen = ref(false)
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
const articleBodyRef = ref(null)
const contentTextareaRef = ref(null)

const newArticle = ref({
  title: '',
  description: '',
  image: '',
  startDate: '',
  endDate: '',
  content: '',
  status: 'active'
})

// Lỗi validate từng trường của Tin khuyến mãi (inline error)
const articleErrors = ref({})
const clearAErr = (key) => {
  if (articleErrors.value[key]) {
    const e = { ...articleErrors.value }
    delete e[key]
    articleErrors.value = e
  }
}

// Ngày kết thúc tối thiểu của tin = ngày bắt đầu (hoặc hôm nay)
const articleEndMinStr = computed(() => {
  return newArticle.value.startDate || todayStr.value
})

// Khi đổi ngày bắt đầu: nếu ngày kết thúc < ngày bắt đầu thì tự xóa để chọn lại
const onArticleStartDateChange = () => {
  clearAErr('startDate')
  if (newArticle.value.endDate && newArticle.value.startDate && newArticle.value.endDate < newArticle.value.startDate) {
    newArticle.value.endDate = ''
    clearAErr('endDate')
  }
}

// Trạng thái hiệu lực toàn diện của tin khuyến mãi
const articleStatus = (article) => {
  if (article.status === 'inactive' || article.isActive === false) {
    return { code: 'inactive', label: 'Đang ẩn', tone: 'inactive' }
  }
  const today = todayStr.value
  const start = article.startDate ? String(article.startDate).slice(0, 10) : null
  const end = article.endDate ? String(article.endDate).slice(0, 10) : null
  if (start && start > today) return { code: 'scheduled', label: 'Chưa tới hạn', tone: 'scheduled' }
  if (end && end < today) return { code: 'expired', label: 'Đã hết hạn', tone: 'expired' }
  return { code: 'active', label: 'Đang hiển thị', tone: 'active' }
}

// Lọc theo từ khoá + trạng thái hiệu lực thực tế (client-side)
const filteredArticles = computed(() => {
  const q = articleSearch.value.trim().toLowerCase()
  return articles.value.filter(a => {
    const matchQ = !q || (a.title || '').toLowerCase().includes(q) || (a.description || '').toLowerCase().includes(q)
    const st = articleStatus(a).code
    const matchStatus = filterStatus.value === 'all' || filterStatus.value === st
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

// ===== Quản lý nội dung TipTap & Bài viết =====
const contentLength = computed(() => {
  if (!newArticle.value.content) return 0
  const tmp = document.createElement('div')
  tmp.innerHTML = newArticle.value.content
  return (tmp.innerText || tmp.textContent || '').replace(/\s+/g, ' ').trim().length
})

const openArticleDrawer = () => {
  editingArticleId.value = null
  newArticle.value = { title: '', description: '', image: '', startDate: todayStr.value, endDate: '', content: '', status: 'active' }
  articleErrors.value = {}
  isArticleDrawerOpen.value = true
}

const openEditArticle = (article) => {
  editingArticleId.value = article.id
  const end = article.endDate ? String(article.endDate).slice(0, 10) : ''
  const isExpired = end && end < todayStr.value
  const htmlContent = parseMarkdownToHtml(article.content || '')
  newArticle.value = {
    title: article.title || '',
    description: article.description || '',
    image: article.image || '',
    startDate: article.startDate ? String(article.startDate).slice(0, 10) : '',
    endDate: end,
    content: htmlContent,
    status: isExpired ? 'inactive' : (article.status || 'active')
  }
  articleErrors.value = {}
  isArticleDrawerOpen.value = true
}

// Tự động chuyển trạng thái sang tắt khi ngày kết thúc bị chỉnh về quá khứ
watch(() => newArticle.value.endDate, (newEnd) => {
  if (newEnd && newEnd < todayStr.value && newArticle.value.status === 'active') {
    newArticle.value.status = 'inactive'
  }
})

const toggleNewArticleStatus = () => {
  if (newArticle.value.status === 'inactive') {
    if (newArticle.value.endDate && newArticle.value.endDate < todayStr.value) {
      showToast('Tin đã hết hạn. Vui lòng gia hạn ngày kết thúc để bật hiển thị.', 'warning')
      return
    }
    newArticle.value.status = 'active'
  } else {
    newArticle.value.status = 'inactive'
  }
}

// Format ngày chuẩn Việt Nam DD/MM/YYYY
const formatDisplayDate = (d) => {
  if (!d) return '—'
  const s = String(d).slice(0, 10)
  const parts = s.split('-')
  if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`
  return s
}

// Validate từng trường khi người dùng blur / thay đổi dữ liệu
const validateArticleField = (key) => {
  const f = newArticle.value
  const isCreate = !editingArticleId.value
  const errs = { ...articleErrors.value }

  if (key === 'image') {
    if (!f.image || !f.image.trim()) {
      errs.image = 'Vui lòng tải lên ảnh Thumbnail / Banner cho tin khuyến mãi.'
    } else {
      delete errs.image
    }
  }

  if (key === 'title') {
    const title = (f.title || '').trim().replace(/\s+/g, ' ')
    if (!title) {
      errs.title = 'Vui lòng nhập tiêu đề tin khuyến mãi.'
    } else if (title.length < 5 || title.length > 150) {
      errs.title = `Tiêu đề phải từ 5 đến 150 ký tự (hiện có ${title.length} ký tự).`
    } else if (/<[^>]*>/.test(title)) {
      errs.title = 'Tiêu đề không được chứa thẻ HTML hoặc mã độc.'
    } else {
      delete errs.title
    }
  }

  if (key === 'description') {
    const desc = (f.description || '').trim()
    if (!desc) {
      errs.description = 'Vui lòng nhập mô tả tóm tắt ngắn cho tin khuyến mãi.'
    } else if (desc.length < 5 || desc.length > 255) {
      errs.description = `Mô tả ngắn phải từ 5 đến 255 ký tự (hiện có ${desc.length} ký tự).`
    } else {
      delete errs.description
    }
  }

  if (key === 'startDate') {
    if (!f.startDate) {
      errs.startDate = 'Vui lòng chọn ngày bắt đầu áp dụng.'
    } else if (isCreate && f.startDate < todayStr.value) {
      errs.startDate = 'Ngày bắt đầu không được ở trong quá khứ.'
    } else {
      delete errs.startDate
    }
    if (f.startDate && f.endDate && f.endDate < f.startDate) {
      errs.endDate = 'Ngày kết thúc không được trước ngày bắt đầu.'
    }
  }

  if (key === 'endDate') {
    if (!f.endDate) {
      errs.endDate = 'Vui lòng chọn ngày kết thúc áp dụng.'
    } else if (f.startDate && f.endDate < f.startDate) {
      errs.endDate = 'Ngày kết thúc không được trước ngày bắt đầu.'
    } else if (isCreate && f.endDate < todayStr.value) {
      errs.endDate = 'Ngày kết thúc không được ở trong quá khứ.'
    } else {
      delete errs.endDate
    }
  }

  if (key === 'content') {
    const rawText = (newArticle.value.content || '').replace(/<[^>]*>/g, '').trim()
    if (!rawText) {
      errs.content = 'Vui lòng nhập nội dung chi tiết của chương trình khuyến mãi.'
    } else if (rawText.length < 10) {
      errs.content = `Nội dung chi tiết phải có tối thiểu 10 ký tự (hiện có ${rawText.length} ký tự).`
    } else if (rawText.length > 10000) {
      errs.content = 'Nội dung chi tiết không được vượt quá 10.000 ký tự.'
    } else {
      delete errs.content
    }
  }

  articleErrors.value = errs
}

// Validate toàn diện form Tin Khuyến Mãi trước khi gửi
const articleFieldOrder = ['image', 'title', 'description', 'startDate', 'endDate', 'content']
const validateArticleForm = () => {
  const errs = {}
  const f = newArticle.value
  
  // 1. Ảnh thumbnail/banner
  if (!f.image || !f.image.trim()) {
    errs.image = 'Vui lòng tải lên ảnh Thumbnail / Banner cho tin khuyến mãi.'
  }
  
  // 2. Tiêu đề
  const title = (f.title || '').trim().replace(/\s+/g, ' ')
  if (!title) {
    errs.title = 'Vui lòng nhập tiêu đề tin khuyến mãi.'
  } else if (title.length < 5 || title.length > 150) {
    errs.title = `Tiêu đề phải từ 5 đến 150 ký tự (hiện có ${title.length} ký tự).`
  } else if (/<[^>]*>/.test(title)) {
    errs.title = 'Tiêu đề không được chứa thẻ HTML hoặc mã độc.'
  }
  
  // 3. Mô tả ngắn
  const desc = (f.description || '').trim()
  if (!desc) {
    errs.description = 'Vui lòng nhập mô tả tóm tắt ngắn cho tin khuyến mãi.'
  } else if (desc.length < 5 || desc.length > 255) {
    errs.description = `Mô tả ngắn phải từ 5 đến 255 ký tự (hiện có ${desc.length} ký tự).`
  }
  
  // 4. Ngày áp dụng (Bắt buộc cả 2 ngày)
  const isCreate = !editingArticleId.value
  if (!f.startDate) {
    errs.startDate = 'Vui lòng chọn ngày bắt đầu áp dụng.'
  } else if (isCreate && f.startDate < todayStr.value) {
    errs.startDate = 'Ngày bắt đầu không được ở trong quá khứ.'
  }

  if (!f.endDate) {
    errs.endDate = 'Vui lòng chọn ngày kết thúc áp dụng.'
  } else if (f.startDate && f.endDate < f.startDate) {
    errs.endDate = 'Ngày kết thúc không được trước ngày bắt đầu.'
  } else if (isCreate && f.endDate < todayStr.value) {
    errs.endDate = 'Ngày kết thúc không được ở trong quá khứ.'
  }
  
  // 5. Nội dung chi tiết
  const rawContent = (f.content || '').replace(/<[^>]*>/g, '').trim()
  if (!rawContent) {
    errs.content = 'Vui lòng nhập nội dung chi tiết của chương trình khuyến mãi.'
  } else if (rawContent.length < 10) {
    errs.content = `Nội dung chi tiết phải có tối thiểu 10 ký tự (hiện có ${rawContent.length} ký tự).`
  } else if (rawContent.length > 10000) {
    errs.content = 'Nội dung chi tiết không được vượt quá 10.000 ký tự.'
  }
  
  articleErrors.value = errs
  return Object.keys(errs).length === 0
}

const focusFirstArticleError = () => {
  const firstKey = articleFieldOrder.find(k => articleErrors.value[k])
  if (!firstKey || !articleBodyRef.value) return
  const el = articleBodyRef.value.querySelector(`[data-field="${firstKey}"]`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    if (typeof el.focus === 'function') el.focus()
  }
}

// Upload ảnh tin lên Cloudinary qua /api/upload
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
    const uploadedUrl = data?.data?.url || data?.url || (typeof data?.data === 'string' ? data.data : '')
    if (!uploadedUrl) throw new Error('Không nhận được URL ảnh từ máy chủ')
    newArticle.value.image = uploadedUrl
    clearAErr('image')
    showToast('Tải ảnh lên thành công.')
  } catch (err) {
    showToast('Tải ảnh thất bại.', 'error')
  } finally {
    isUploadingArticleImage.value = false
    e.target.value = '' // cho phép chọn lại cùng file
  }
}

const removeArticleImage = () => {
  newArticle.value.image = ''
}

const handleSaveArticle = async () => {
  if (!validateArticleForm()) {
    focusFirstArticleError()
    showToast('Vui lòng kiểm tra và sửa các trường lỗi màu đỏ.', 'error')
    return
  }
  isSavingArticle.value = true
  try {
    const payload = {
      title: newArticle.value.title.trim().replace(/\s+/g, ' '),
      description: newArticle.value.description?.trim() || null,
      imageUrl: newArticle.value.image || null,
      content: newArticle.value.content?.trim() || null,
      startDate: newArticle.value.startDate || null,
      endDate: newArticle.value.endDate || null,
      isActive: newArticle.value.status === 'active' && !(newArticle.value.endDate && newArticle.value.endDate < todayStr.value)
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
  movieMode: 'all', // 'all' | 'specific'
  selectedMovieIds: [],
  applicableMovieId: '',
  applicableMovieIds: '',
  isHidden: false,
  customerEligibility: 'ALL',
  usageLimit: null,
  maxTicketQuantity: null,
  maxDiscountAmount: null,
  cinemaMode: 'all',
  selectedCinemas: []
})

// Combobox & bộ chọn "Áp dụng theo phim"
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
  if (newVoucher.value.movieMode === 'all' || !newVoucher.value.selectedMovieIds.length) return 'Tất cả phim'
  const count = newVoucher.value.selectedMovieIds.length
  if (count === 1) {
    const m = moviesList.value.find(x => x.id === newVoucher.value.selectedMovieIds[0])
    return m ? m.title : '1 phim đã chọn'
  }
  return `${count} phim đã chọn`
})

// Ngày tối thiểu cho ô Hết hạn = NGÀY SAU ngày bắt đầu (hoặc sau hôm nay nếu chưa chọn) → ép end > start
const endMinStr = computed(() => {
  const base = newVoucher.value.startDate || todayStr.value
  const d = new Date(`${base}T00:00:00`); d.setDate(d.getDate() + 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})

const editingVoucherId = ref(null)
const isSavingVoucher = ref(false)
// Khóa cứng ô Ngày bắt đầu khi sửa voucher ĐANG CHẠY (start <= hôm nay hoặc null) — tránh sai lịch sử đơn cũ
const editStartLocked = ref(false)

const openVoucherDrawer = () => {
  editingVoucherId.value = null
  editStartLocked.value = false
  newVoucher.value = {
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
    movieMode: 'all',
    selectedMovieIds: [],
    applicableMovieId: '',
    applicableMovieIds: '',
    isHidden: false,
    customerEligibility: 'ALL',
    usageLimit: 0,
    maxTicketQuantity: 0,
    maxDiscountAmount: null,
    cinemaMode: 'all',
    selectedCinemas: []
  }
  voucherErrors.value = {}
  pctLimitWarn.value = ''
  movieDropdownOpen.value = false; movieSearch.value = ''; cinemaSearch.value = ''
  isVoucherDrawerOpen.value = true
}

// Mở drawer ở chế độ chỉnh sửa, đổ dữ liệu promotion thật vào form
const openEditVoucher = (promo) => {
  editingVoucherId.value = promo.id
  const movieIdsList = []
  if (promo.applicableMovieIds) {
    String(promo.applicableMovieIds).split(',').map(x => Number(x.trim())).filter(Boolean).forEach(id => {
      if (!movieIdsList.includes(id)) movieIdsList.push(id)
    })
  } else if (promo.applicableMovieId) {
    movieIdsList.push(Number(promo.applicableMovieId))
  }

  newVoucher.value = {
    code: promo.code || '',
    type: promo.discountType || 'PERCENTAGE',
    value: promo.discountValue != null ? Number(promo.discountValue) : null,
    allowPointExchange: !!promo.allowPointRedemption,
    pointsRequired: promo.pointsRequired != null ? Number(promo.pointsRequired) : null,
    title: promo.name || '',
    description: promo.description || '',
    startDate: promo.startDate ? String(promo.startDate).slice(0, 10) : '',
    expiry: promo.endDate ? String(promo.endDate).slice(0, 10) : '',
    minOrderValue: promo.minOrderValue != null ? Number(promo.minOrderValue) : 0,
    movieMode: movieIdsList.length > 0 ? 'specific' : 'all',
    selectedMovieIds: movieIdsList,
    applicableMovieId: promo.applicableMovieId != null ? promo.applicableMovieId : '',
    applicableMovieIds: promo.applicableMovieIds || '',
    isHidden: !!promo.isHidden,
    customerEligibility: promo.customerEligibility || 'ALL',
    usageLimit: promo.usageLimit != null ? Number(promo.usageLimit) : 0,
    maxTicketQuantity: promo.maxTicketQuantity != null ? Number(promo.maxTicketQuantity) : 0,
    maxDiscountAmount: promo.maxDiscountAmount != null ? Number(promo.maxDiscountAmount) : 0,
    cinemaMode: 'all',
    selectedCinemas: []
  }
  // Voucher ĐANG CHẠY (chưa có ngày bắt đầu = áp dụng ngay, hoặc bắt đầu <= hôm nay) → khóa ô ngày bắt đầu
  editStartLocked.value = !promo.startDate || String(promo.startDate).slice(0, 10) <= todayStr.value
  voucherErrors.value = {}
  pctLimitWarn.value = ''
  movieDropdownOpen.value = false; movieSearch.value = ''; cinemaSearch.value = ''
  isVoucherDrawerOpen.value = true
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

// Lỗi "Đơn tối thiểu" theo thời gian thực (khi Tiền cố định: Min = giá trị giảm; Max = 999.999.999)
const minOrderValueError = computed(() => {
  const v = newVoucher.value
  if (v.minOrderValue == null || v.minOrderValue === '') return ''
  const mov = Number(v.minOrderValue)
  if (Number.isNaN(mov)) return 'Đơn tối thiểu không hợp lệ.'
  if (mov < 0) return 'Đơn tối thiểu phải là số nguyên ≥ 0.'
  if (mov > MAX_MONEY) return 'Đơn tối thiểu không được vượt quá 999.999.999đ.'
  if (isFixed.value && v.value != null && v.value !== '') {
    const dv = Number(v.value)
    if (!Number.isNaN(dv) && dv > 0 && mov < dv) {
      return `Đơn tối thiểu phải từ ${fmtThousand(dv)}đ trở lên (≥ giá trị giảm).`
    }
  }
  return ''
})

// ---- (3) RÀNG BUỘC CHÉO (phản ứng ngay khi đổi loại/giá trị) ----
// Loại "Tiền cố định": trần Giảm tối đa & Đơn tối thiểu tự động lấy theo giá trị giảm
const isFixed = computed(() => newVoucher.value.type === 'FIXED_AMOUNT')
watch(
  () => [newVoucher.value.type, newVoucher.value.value],
  () => {
    if (isFixed.value) {
      const valNum = newVoucher.value.value != null && newVoucher.value.value !== '' ? Number(newVoucher.value.value) : null
      newVoucher.value.maxDiscountAmount = valNum
      clearVErr('maxDiscountAmount')
      // Đơn tối thiểu tự động đồng bộ theo Giá trị giảm (VNĐ)
      newVoucher.value.minOrderValue = valNum
      clearVErr('minOrderValue')
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

  // minOrderAmount: BẮT BUỘC, số nguyên >= 0 (nhập 0 nếu không yêu cầu; khi Tiền cố định: Min = giá trị giảm; Max = 999.999.999)
  const minOrder = Number(v.minOrderValue || 0)
  if (v.minOrderValue == null || v.minOrderValue === '') {
    e.minOrderValue = isFixed.value ? 'Vui lòng nhập đơn tối thiểu (≥ giá trị giảm).' : 'Vui lòng nhập đơn tối thiểu (nhập 0 nếu không yêu cầu).'
  } else if (!Number.isInteger(minOrder) || minOrder < 0) {
    e.minOrderValue = 'Đơn tối thiểu phải là số nguyên ≥ 0.'
  } else if (minOrder > MAX_MONEY) {
    e.minOrderValue = 'Đơn tối thiểu không được vượt quá 999.999.999đ.'
  } else if (isFixed.value && !Number.isNaN(dv) && dv > 0 && minOrder < dv) {
    e.minOrderValue = `Đơn tối thiểu phải từ ${fmtThousand(dv)}đ trở lên (≥ giá trị giảm).`
  }

  // usageLimit: số nguyên >= 0 (để trống hoặc 0 = không giới hạn)
  const usage = v.usageLimit != null && v.usageLimit !== '' ? Number(v.usageLimit) : 0
  if (Number.isNaN(usage) || !Number.isInteger(usage) || usage < 0) {
    e.usageLimit = 'Giới hạn lượt dùng phải là số nguyên ≥ 0.'
  } else if (usage > MAX_USAGE) {
    e.usageLimit = 'Giới hạn lượt dùng không được vượt quá 9.999.999.'
  }

  // maxApplicableTickets: số nguyên >= 0 (để trống hoặc 0 = không giới hạn)
  const maxTk = v.maxTicketQuantity != null && v.maxTicketQuantity !== '' ? Number(v.maxTicketQuantity) : 0
  if (Number.isNaN(maxTk) || !Number.isInteger(maxTk) || maxTk < 0) {
    e.maxTicketQuantity = 'Số vé tối đa phải là số nguyên ≥ 0.'
  } else if (maxTk > MAX_TICKETS_PER_HALL) {
    e.maxTicketQuantity = `Số vé tối đa/đơn không được vượt quá ${MAX_TICKETS_PER_HALL} (số ghế một phòng chiếu).`
  }

  // maxDiscountAmount: BẮT BUỘC, số nguyên >= 0 (mã tiền cố định tự điền = giá trị giảm)
  const maxDisc = Number(v.maxDiscountAmount || 0)
  if (!isFixed.value && (v.maxDiscountAmount == null || v.maxDiscountAmount === '')) e.maxDiscountAmount = 'Vui lòng nhập giảm tối đa (nhập 0 nếu không giới hạn).'
  else if (!Number.isInteger(maxDisc) || maxDisc < 0) e.maxDiscountAmount = 'Giảm tối đa phải là số nguyên ≥ 0.'
  else if (maxDisc > MAX_MONEY) e.maxDiscountAmount = 'Giảm tối đa không được vượt quá 999.999.999đ.'

  // ===== RÀNG BUỘC CHÉO =====
  if (v.type === 'PERCENTAGE') {
    // % bắt buộc đặt trần Giảm tối đa để chặn đơn lớn giảm quá tay
    if (!maxDisc || maxDisc <= 0) e.maxDiscountAmount = 'Mã giảm % cần đặt trần Giảm tối đa (> 0).'
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
    const selectedIds = newVoucher.value.movieMode === 'specific' ? newVoucher.value.selectedMovieIds : []
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
      applicableMovieIds: selectedIds.length > 0 ? selectedIds.join(',') : null,
      applicableMovieId: selectedIds.length > 0 ? selectedIds[0] : null,
      isHidden: !!newVoucher.value.isHidden,
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
    } else {
      showToast(friendlyError(err, 'Lưu voucher thất bại.'), 'error')
    }
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

// Xem chi tiết voucher — tự động làm mới dữ liệu từ server khi mở
const detailTarget = ref(null)
const openDetail = async (promo) => {
  if (!promo) return
  detailTarget.value = promo
  try {
    await fetchMarketingData()
    if (detailTarget.value && detailTarget.value.id === promo.id) {
      const fresh = promotions.value.find(p => p.id === promo.id)
      if (fresh) detailTarget.value = fresh
    }
  } catch (err) {
    // Giữ nguyên dữ liệu hiện tại nếu mạng chập chờn
  }
}
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
    showToast(friendlyError(err, 'Xoá voucher thất bại.'), 'error')
  } finally {
    isDeleting.value = false
  }
}

// ===== Gửi email chiến dịch (thông báo mã) tới khách theo đối tượng áp dụng =====
const emailTarget = ref(null)        // promotion đang chờ xác nhận gửi email
const isSendingCampaign = ref(false)
const eligibilityLabel = (val) => eligibilityOptions.find(o => o.value === val)?.label || 'Mọi khách hàng'
const movieTitleById = (id) => id ? (moviesList.value.find(m => m.id === id)?.title || 'Tất cả phim') : 'Tất cả phim'

// Trích xuất danh sách object phim áp dụng cho voucher
const getPromoMoviesList = (p) => {
  if (!p) return []
  const result = []
  const seenIds = new Set()

  if (p.applicableMovieIds) {
    const ids = String(p.applicableMovieIds).split(',').map(x => Number(x.trim())).filter(Boolean)
    ids.forEach(id => {
      if (!seenIds.has(id)) {
        seenIds.add(id)
        const m = moviesList.value.find(item => item.id === id)
        if (m) {
          result.push({
            id: m.id,
            title: m.title || `Phim #${id}`,
            posterUrl: m.posterUrl || '',
            durationMinutes: m.durationMinutes || null,
            genre: m.genre || '',
            releaseDate: m.releaseDate || ''
          })
        } else {
          result.push({
            id,
            title: `Phim #${id}`,
            posterUrl: '',
            durationMinutes: null
          })
        }
      }
    })
  } else if (p.applicableMovieId) {
    const id = Number(p.applicableMovieId)
    const m = moviesList.value.find(item => item.id === id)
    result.push(m ? {
      id: m.id,
      title: m.title || `Phim #${id}`,
      posterUrl: m.posterUrl || '',
      durationMinutes: m.durationMinutes || null,
      genre: m.genre || '',
      releaseDate: m.releaseDate || ''
    } : {
      id,
      title: p.applicableMovieTitle || `Phim #${id}`,
      posterUrl: '',
      durationMinutes: null
    })
  } else if (p.applicableMovieTitle && p.applicableMovieTitle.trim()) {
    const titles = p.applicableMovieTitle.split(',').map(t => t.trim()).filter(Boolean)
    titles.forEach((t, idx) => {
      result.push({
        id: 'title-' + idx,
        title: t,
        posterUrl: '',
        durationMinutes: null
      })
    })
  }

  return result
}

// Modal danh sách phim áp dụng (Phương án 1)
const promoMoviesModalTarget = ref(null)
const promoMoviesSearch = ref('')
const openPromoMoviesModal = (promo) => {
  promoMoviesModalTarget.value = promo
  promoMoviesSearch.value = ''
}
const closePromoMoviesModal = () => {
  promoMoviesModalTarget.value = null
  promoMoviesSearch.value = ''
}
const filteredPromoMovies = computed(() => {
  if (!promoMoviesModalTarget.value) return []
  const list = getPromoMoviesList(promoMoviesModalTarget.value)
  const q = promoMoviesSearch.value.trim().toLowerCase()
  if (!q) return list
  return list.filter(m => (m.title || '').toLowerCase().includes(q))
})

const movieTitleByPromo = (p) => {
  if (!p) return 'Tất cả phim'
  const list = getPromoMoviesList(p)
  if (list.length === 0) return 'Tất cả phim'
  if (list.length === 1) return list[0].title
  return `${list.length} phim áp dụng`
}

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
    await fetchMarketingData()
  } catch (err) {
    showToast(friendlyError(err, 'Phát voucher thất bại.'), 'error')
  } finally {
    isIssuing.value = false
  }
}

// Trạng thái hiệu lực: Tạm dừng (paused) > Hết hạn (expired) > Đang chạy (active)
const promoStatus = (promo) => {
  if (promo?.isActive === false) return 'paused'
  if (!promo?.endDate) return 'active'
  return new Date(promo.endDate) >= new Date() ? 'active' : 'expired'
}
const formatPromoDate = (iso) => {
  if (!iso) return 'Không giới hạn'
  return new Date(iso).toLocaleDateString('vi-VN')
}

// Bật / Tạm dừng voucher
const handleToggleVoucher = async (promo) => {
  const prev = promo.isActive
  promo.isActive = prev === false ? true : false // optimistic update
  try {
    const { data } = await marketingApi.togglePromotion(promo.id)
    showToast(data.message || (promo.isActive ? 'Đã kích hoạt voucher.' : 'Đã tạm dừng voucher.'))
    await fetchMarketingData()
  } catch (err) {
    promo.isActive = prev // revert nếu lỗi
    showToast(friendlyError(err, 'Đổi trạng thái voucher thất bại.'), 'error')
  }
}

const fetchMarketingData = async () => {
  // Tải danh sách voucher (promotions)
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
      isActive: p.isActive,
      isStackable: p.isStackable,
      pointsRequired: p.pointsRequired,
      allowPointRedemption: p.allowPointRedemption,
      minOrderValue: p.minOrderValue,
      maxTicketQuantity: p.maxTicketQuantity,
      maxDiscountAmount: p.maxDiscountAmount,
      applicableMovieId: p.applicableMovieId,
      applicableMovieIds: p.applicableMovieIds,
      isHidden: !!p.isHidden,
      customerEligibility: p.customerEligibility,
      usageLimit: p.usageLimit,
      usedCount: p.usedCount,
      campaignSentAt: p.campaignSentAt,
      campaignSentCount: p.campaignSentCount
    })).sort((a, b) => Number(b.id || 0) - Number(a.id || 0))
  } catch (error) {
    showToast('Không thể tải danh sách voucher.', 'error')
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
        <p class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold">Quản lý chiến dịch tiếp thị, mã giảm giá và tin khuyến mãi</p>
      </div>
      <div class="flex gap-4">
        <button v-if="activeTab === 'vouchers' && can('promotions', 'add')" @click="openVoucherDrawer" class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
          <span class="material-symbols-outlined text-sm">add_card</span>
          Tạo Voucher
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

            <div class="relative flex items-center justify-between mb-2 gap-1.5">
              <span v-if="promo.isHidden" class="inline-flex items-center text-[7.5px] font-bold px-1.5 py-0.5 rounded-sm uppercase tracking-wider bg-purple-500/15 text-purple-300 border border-purple-500/30 leading-none">
                Riêng tư
              </span>
              <span v-else class="inline-flex items-center text-[7.5px] font-bold px-1.5 py-0.5 rounded-sm uppercase tracking-wider bg-sky-500/15 text-sky-300 border border-sky-500/25 leading-none">
                Công khai
              </span>

              <span :class="promoStatus(promo) === 'active' ? 'bg-green-500/15 text-green-400' : promoStatus(promo) === 'paused' ? 'bg-amber-500/15 text-amber-400' : 'bg-red-500/15 text-red-400'" class="shrink-0 inline-flex items-center gap-1 text-[8px] font-black px-2 py-0.5 rounded-full uppercase tracking-widest leading-none">
                <span v-if="promoStatus(promo) === 'active'" class="w-1 h-1 rounded-full bg-green-400 animate-pulse"></span>
                <span v-else-if="promoStatus(promo) === 'paused'" class="w-1 h-1 rounded-full bg-amber-400"></span>
                {{ promoStatus(promo) === 'active' ? 'Đang chạy' : promoStatus(promo) === 'paused' ? 'Tạm dừng' : 'Hết hạn' }}
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
                <span class="text-[11px] font-bold" :class="promoStatus(promo) === 'active' ? 'text-on-surface' : promoStatus(promo) === 'paused' ? 'text-amber-400' : 'text-red-400'">{{ formatPromoDate(promo.endDate) }}</span>
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
              <button v-if="can('promotions', 'edit')" @click.stop="handleToggleVoucher(promo)" :title="promo.isActive === false ? 'Kích hoạt voucher (đang tắt)' : 'Tạm dừng voucher (đang bật)'"
                class="w-8 h-8 rounded-lg flex items-center justify-center transition-colors"
                :class="promo.isActive === false ? 'text-on-surface-variant/40 hover:text-green-400 hover:bg-green-500/10' : 'text-green-400 hover:text-amber-400 hover:bg-amber-500/10'">
                <span class="material-symbols-outlined text-2xl leading-none">{{ promo.isActive === false ? 'toggle_off' : 'toggle_on' }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Articles View -->
    <div v-if="activeTab === 'articles'" class="space-y-6">
      <!-- Search & Filter bar -->
      <div class="flex justify-between items-center bg-surface-container-low p-4 sm:px-6 rounded-xl border border-outline-variant/10 gap-4">
        <div class="relative w-80">
          <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-on-surface-variant text-lg">search</span>
          <input v-model="articleSearch" type="text" placeholder="Tìm kiếm tin khuyến mãi..." class="w-full bg-surface-container-highest border border-outline-variant/15 rounded-sm pl-10 pr-4 py-2 text-xs text-on-surface focus:border-primary outline-none">
        </div>
        <div class="flex gap-4 w-52">
          <CustomSelect 
            v-model="filterStatus" 
            :options="statusOptions" 
            customClass="w-full px-4 py-2 rounded-sm text-xs border border-outline-variant/15 bg-surface-container-highest font-bold text-on-surface-variant" 
          />
        </div>
      </div>
      
      <!-- Table -->
      <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden shadow-xl">
        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse min-w-[900px]">
            <thead>
              <tr class="bg-surface-container-highest/50 border-b border-outline-variant/10 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant select-none">
                <th class="py-4 pl-6 pr-4 w-[12%]">Hình ảnh</th>
                <th class="py-4 px-4 w-[44%]">Thông tin bài viết</th>
                <th class="py-4 px-4 w-[20%]">Thời gian áp dụng</th>
                <th class="py-4 px-4 w-[14%] text-center">Trạng thái</th>
                <th class="py-4 pl-4 pr-6 w-[10%] text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/5">
              <!-- Loading -->
              <tr v-if="isLoadingArticles" v-for="i in 3" :key="'sk' + i">
                <td class="py-4 pl-6 pr-4"><div class="w-20 h-12 bg-surface-container-highest rounded-sm animate-pulse"></div></td>
                <td class="py-4 px-4"><div class="space-y-2"><div class="h-4 w-3/4 bg-surface-container-highest rounded-sm animate-pulse"></div><div class="h-3 w-1/2 bg-surface-container-highest rounded-sm animate-pulse"></div></div></td>
                <td class="py-4 px-4"><div class="h-4 w-28 bg-surface-container-highest rounded-sm animate-pulse"></div></td>
                <td class="py-4 px-4 text-center"><div class="h-5 w-24 bg-surface-container-highest rounded-sm mx-auto animate-pulse"></div></td>
                <td class="py-4 pl-4 pr-6 text-right"><div class="h-8 w-24 bg-surface-container-highest rounded-sm ml-auto animate-pulse"></div></td>
              </tr>
              <!-- Empty -->
              <tr v-else-if="filteredArticles.length === 0">
                <td colspan="5" class="py-16 text-center">
                  <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 mb-2">campaign</span>
                  <p class="text-on-surface-variant text-sm font-semibold">{{ articleSearch || filterStatus !== 'all' ? 'Không tìm thấy tin phù hợp.' : 'Chưa có tin khuyến mãi nào. Bấm "Tạo Tin Khuyến Mãi" để đăng tin.' }}</p>
                </td>
              </tr>
              <!-- Data -->
              <tr v-else v-for="article in filteredArticles" :key="article.id" class="hover:bg-white/[0.03] transition-colors group">
                <!-- Cột 1: Hình ảnh -->
                <td class="py-4 pl-6 pr-4 align-middle">
                  <div class="w-20 h-12 bg-surface-container-highest rounded-sm overflow-hidden flex items-center justify-center border border-outline-variant/15 shadow-sm shrink-0">
                    <img v-if="article.image || article.imageUrl" :src="article.image || article.imageUrl" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                    <span v-else class="text-[10px] text-on-surface-variant/40 uppercase font-bold">Không ảnh</span>
                  </div>
                </td>
                <!-- Cột 2: Thông tin bài viết -->
                <td class="py-4 px-4 align-middle">
                  <h4 class="text-sm font-black text-on-surface uppercase italic group-hover:text-primary transition-colors line-clamp-1" :title="article.title">{{ article.title }}</h4>
                  <p class="text-xs text-on-surface-variant mt-1 line-clamp-1 leading-relaxed" :title="article.description">{{ article.description || 'Chưa có mô tả ngắn' }}</p>
                </td>
                <!-- Cột 3: Thời gian áp dụng -->
                <td class="py-4 px-4 align-middle">
                  <div class="flex flex-col gap-0.5">
                    <span class="text-xs text-on-surface font-mono font-bold">{{ formatDisplayDate(article.startDate) }}</span>
                    <span class="text-[10px] text-on-surface-variant font-mono">đến {{ formatDisplayDate(article.endDate) }}</span>
                  </div>
                </td>
                <!-- Cột 4: Trạng thái (Căn giữa) -->
                <td class="py-4 px-4 align-middle text-center">
                  <span :class="{
                    'bg-green-500/15 text-green-400 border-green-500/30': articleStatus(article).tone === 'active',
                    'bg-red-500/15 text-red-400 border-red-500/30': articleStatus(article).tone === 'expired',
                    'bg-sky-500/15 text-sky-400 border-sky-500/30': articleStatus(article).tone === 'scheduled',
                    'bg-zinc-500/15 text-zinc-400 border-zinc-500/30': articleStatus(article).tone === 'inactive'
                  }" class="inline-block px-3 py-1 rounded-sm text-[9px] font-black uppercase tracking-widest border">
                    {{ articleStatus(article).label }}
                  </span>
                </td>
                <!-- Cột 5: Thao tác (Căn phải, có đệm lề) -->
                <td class="py-4 pl-4 pr-6 align-middle text-right">
                  <div class="flex justify-end items-center gap-1.5">
                    <button v-if="can('promotions', 'edit')" @click.stop="handleToggleArticle(article)"
                      :title="article.status === 'active' ? 'Tắt/Ẩn tin khuyến mãi' : 'Bật hiển thị tin khuyến mãi'"
                      class="w-8 h-8 rounded-sm hover:bg-white/10 flex items-center justify-center transition-colors border border-transparent hover:border-outline-variant/20"
                      :class="article.status === 'active' ? 'text-green-400 hover:text-amber-400' : 'text-on-surface-variant/40 hover:text-green-400'">
                      <span class="material-symbols-outlined text-lg">{{ article.status === 'active' ? 'visibility' : 'visibility_off' }}</span>
                    </button>
                    <button v-if="can('promotions', 'edit')" @click="openEditArticle(article)" title="Chỉnh sửa" class="w-8 h-8 rounded-sm hover:bg-white/10 text-on-surface-variant hover:text-primary flex items-center justify-center transition-colors border border-transparent hover:border-outline-variant/20">
                      <span class="material-symbols-outlined text-sm">edit</span>
                    </button>
                    <button v-if="can('promotions', 'delete')" @click="articleDeleteTarget = article" title="Xoá" class="w-8 h-8 rounded-sm hover:bg-white/10 text-on-surface-variant hover:text-red-400 flex items-center justify-center transition-colors border border-transparent hover:border-outline-variant/20">
                      <span class="material-symbols-outlined text-sm">delete</span>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
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
                <input :value="fmtThousand(newVoucher.minOrderValue)" @input="onMoneyInput($event, 'minOrderValue')" type="text" inputmode="numeric" data-field="minOrderValue" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="(voucherErrors.minOrderValue || minOrderValueError) ? 'border-red-500' : 'border-outline-variant/20'" :placeholder="isFixed ? (newVoucher.value ? `Tối thiểu ${fmtThousand(newVoucher.value)}đ` : 'Tối thiểu = giá trị giảm') : '0 = không yêu cầu'" />
                <p v-if="voucherErrors.minOrderValue || minOrderValueError" class="text-[10px] text-red-400 font-bold">{{ voucherErrors.minOrderValue || minOrderValueError }}</p>
                <p v-else-if="isFixed && newVoucher.value" class="text-[10px] text-on-surface-variant/70">Tự điền = giá trị giảm (tối thiểu {{ fmtThousand(newVoucher.value) }}đ, tối đa 999.999.999đ).</p>
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
            <!-- Áp dụng theo phim: Radio Toàn bộ / Phim được chọn kèm checkbox -->
            <div class="space-y-3">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant block">Áp dụng theo phim</label>
              <div class="flex gap-8">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="newVoucher.movieMode" value="all" class="accent-primary">
                  <span class="text-xs font-bold uppercase">Toàn bộ phim</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="newVoucher.movieMode" value="specific" class="accent-primary">
                  <span class="text-xs font-bold uppercase">Phim được chọn</span>
                </label>
              </div>

              <div v-if="newVoucher.movieMode === 'specific'" class="mt-2 p-4 bg-surface-container-highest rounded-xl border border-outline-variant/10 space-y-3 animate-in fade-in slide-in-from-top-2">
                <div class="flex items-center justify-between gap-2">
                  <span class="text-[10px] font-bold text-primary uppercase tracking-wider">
                    {{ newVoucher.selectedMovieIds.length > 0 ? `Đã chọn ${newVoucher.selectedMovieIds.length} phim` : 'Chưa chọn phim nào (chọn bên dưới)' }}
                  </span>
                  <div class="flex items-center gap-2">
                    <button type="button" @click="newVoucher.selectedMovieIds = moviesList.map(m => m.id)" class="text-[10px] font-bold text-primary hover:underline">Chọn tất cả</button>
                    <span class="text-outline-variant/40">·</span>
                    <button type="button" @click="newVoucher.selectedMovieIds = []" class="text-[10px] font-bold text-on-surface-variant hover:text-red-400">Bỏ chọn</button>
                  </div>
                </div>
                <div class="relative">
                  <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-lg pointer-events-none">search</span>
                  <input v-model="movieSearch" type="text" placeholder="Tìm tên phim áp dụng..."
                    class="w-full bg-surface-container-lowest border border-outline-variant/20 pl-10 pr-3 py-2.5 rounded-lg text-sm text-on-surface focus:border-primary outline-none" />
                </div>
                <div class="flex flex-col gap-1 max-h-52 overflow-y-auto scrollbar-custom">
                  <label v-for="m in filteredMoviesList" :key="m.id" class="flex items-center gap-2.5 cursor-pointer p-2 hover:bg-white/5 rounded-lg transition-colors">
                    <input type="checkbox" :value="m.id" v-model="newVoucher.selectedMovieIds" class="accent-primary rounded-sm shrink-0">
                    <span class="text-xs font-bold text-on-surface truncate">{{ m.title }}</span>
                  </label>
                  <div v-if="filteredMoviesList.length === 0" class="px-2 py-3 text-xs text-on-surface-variant/60 italic text-center">Không tìm thấy phim</div>
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

            <!-- Toggle Voucher Kín -->
            <div class="bg-surface-container-highest rounded-xl border border-outline-variant/10 overflow-hidden transition-all duration-300">
              <div class="flex items-center justify-between p-4">
                <div class="pr-3">
                  <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Voucher riêng tư (Không công khai)</p>
                  <p class="text-[10px] text-on-surface-variant mt-1 font-bold leading-relaxed">
                    Nếu bật, voucher sẽ không hiển thị trên trang Khuyến mãi công khai. Khách có mã vẫn có thể nhập để lưu vào ví hoặc dùng khi đặt vé.
                  </p>
                </div>
                <button type="button" @click="newVoucher.isHidden = !newVoucher.isHidden" :class="newVoucher.isHidden ? 'bg-purple-500' : 'bg-surface-container-high'" class="relative w-10 h-5 rounded-full transition-colors duration-300 focus:outline-none shrink-0">
                  <span :class="newVoucher.isHidden ? 'translate-x-5 bg-white' : 'translate-x-0 bg-on-surface-variant'" class="inline-block w-4 h-4 transform rounded-full transition-transform duration-300 shadow-md absolute top-0.5 left-0.5"></span>
                </button>
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
          <button @click="handleSaveVoucher" :disabled="isSavingVoucher || !!discountValueError || !!maxTicketError || !!minOrderValueError" class="flex-1 px-6 py-4 rounded-xl bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform shadow-xl shadow-primary/20 disabled:opacity-40 disabled:grayscale disabled:cursor-not-allowed disabled:hover:scale-100">{{ isSavingVoucher ? 'Đang lưu...' : 'Lưu Voucher' }}</button>
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
        <div ref="articleBodyRef" class="flex-1 overflow-y-auto p-6 space-y-6 scrollbar-custom">
          <!-- Image Upload (Cloudinary) -->
          <div class="space-y-2">
            <div class="flex justify-between items-center">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                Ảnh Banner / Thumbnail <span class="text-red-400">*</span>
              </label>
              <button v-if="newArticle.image" type="button" @click="removeArticleImage" class="text-[10px] font-bold text-red-400 hover:underline uppercase tracking-wider flex items-center gap-1">
                <span class="material-symbols-outlined text-xs">delete</span> Xóa ảnh
              </button>
            </div>
            <label data-field="image" class="relative block w-full h-44 bg-surface-container-highest border-2 border-dashed rounded-2xl overflow-hidden flex flex-col items-center justify-center text-on-surface-variant hover:border-primary/50 hover:bg-primary/5 transition-colors cursor-pointer" :class="articleErrors.image ? 'border-red-500 bg-red-500/5' : 'border-outline-variant/20'">
              <img v-if="newArticle.image" :src="newArticle.image" class="absolute inset-0 w-full h-full object-cover" />
              <div v-if="newArticle.image" class="absolute inset-0 bg-black/40 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center">
                <span class="text-xs font-bold uppercase tracking-widest text-white flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-sm">photo_camera</span> Đổi ảnh khác
                </span>
              </div>
              <template v-if="!newArticle.image">
                <span v-if="isUploadingArticleImage" class="material-symbols-outlined text-3xl mb-2 animate-spin text-primary">progress_activity</span>
                <span v-else class="material-symbols-outlined text-3xl mb-2 text-on-surface-variant/60">cloud_upload</span>
                <span class="text-xs font-bold uppercase tracking-widest">{{ isUploadingArticleImage ? 'Đang tải lên...' : 'Bấm để tải ảnh lên' }}</span>
                <span class="text-[10px] text-on-surface-variant/60 mt-1">Định dạng JPG, PNG, WEBP (tối đa 5MB)</span>
              </template>
              <input type="file" accept="image/*" class="hidden" @change="handleArticleImageUpload" :disabled="isUploadingArticleImage" />
            </label>
            <p v-if="articleErrors.image" class="text-[10px] text-red-400 font-bold">{{ articleErrors.image }}</p>
          </div>

          <!-- Tiêu đề -->
          <div class="space-y-2">
            <div class="flex justify-between items-center">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                Tiêu đề Tin Khuyến Mãi <span class="text-red-400">*</span>
              </label>
              <span class="text-[10px] text-on-surface-variant/60 font-mono">{{ (newArticle.title || '').length }}/150</span>
            </div>
            <input v-model="newArticle.title" @input="clearAErr('title')" @blur="validateArticleField('title')" maxlength="150" data-field="title" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="articleErrors.title ? 'border-red-500' : 'border-outline-variant/20'" placeholder="VD: Khuyến mãi Hè rực rỡ" />
            <p v-if="articleErrors.title" class="text-[10px] text-red-400 font-bold">{{ articleErrors.title }}</p>
          </div>

          <!-- Mô tả ngắn -->
          <div class="space-y-2">
            <div class="flex justify-between items-center">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                Mô tả ngắn (Tóm tắt) <span class="text-red-400">*</span>
              </label>
              <span class="text-[10px] text-on-surface-variant/60 font-mono">{{ (newArticle.description || '').length }}/255</span>
            </div>
            <textarea v-model="newArticle.description" @input="clearAErr('description')" @blur="validateArticleField('description')" rows="2" maxlength="255" data-field="description" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-medium text-on-surface focus:border-primary outline-none resize-none" :class="articleErrors.description ? 'border-red-500' : 'border-outline-variant/20'" placeholder="Mô tả tóm tắt hiển thị ở danh sách ngoài trang chủ..."></textarea>
            <p v-if="articleErrors.description" class="text-[10px] text-red-400 font-bold">{{ articleErrors.description }}</p>
          </div>

          <!-- Ngày áp dụng -->
          <div class="grid grid-cols-2 gap-6">
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                Ngày bắt đầu <span class="text-red-400">*</span>
              </label>
              <input v-model="newArticle.startDate" @change="onArticleStartDateChange(); validateArticleField('startDate')" type="date" :min="!editingArticleId ? todayStr : undefined" data-field="startDate" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="articleErrors.startDate ? 'border-red-500' : 'border-outline-variant/20'" />
              <p v-if="articleErrors.startDate" class="text-[10px] text-red-400 font-bold">{{ articleErrors.startDate }}</p>
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                Ngày kết thúc <span class="text-red-400">*</span>
              </label>
              <input v-model="newArticle.endDate" @input="clearAErr('endDate')" @change="validateArticleField('endDate')" type="date" :min="articleEndMinStr" data-field="endDate" class="w-full bg-surface-container-highest border p-4 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" :class="articleErrors.endDate ? 'border-red-500' : 'border-outline-variant/20'" />
              <p v-if="articleErrors.endDate" class="text-[10px] text-red-400 font-bold">{{ articleErrors.endDate }}</p>
            </div>
          </div>

          <!-- Trạng thái -->
          <div class="flex items-center justify-between p-4 bg-surface-container-highest rounded-xl border border-outline-variant/10">
             <div>
                <div class="flex items-center gap-2">
                  <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Trạng thái hiển thị</p>
                  <span v-if="newArticle.endDate && newArticle.endDate < todayStr" class="text-[9px] font-black px-2 py-0.5 rounded-full bg-red-500/15 text-red-400 uppercase tracking-widest border border-red-500/30">Đã hết hạn</span>
                </div>
                <p v-if="newArticle.endDate && newArticle.endDate < todayStr" class="text-xs text-red-400 mt-1">Tin đã qua ngày kết thúc. Gia hạn ngày kết thúc để bật hiển thị.</p>
                <p v-else class="text-xs text-on-surface-variant mt-1">Cho phép hiển thị tin tức này trên hệ thống website</p>
             </div>
             <button type="button" @click="toggleNewArticleStatus" :disabled="newArticle.endDate && newArticle.endDate < todayStr" :class="newArticle.status === 'active' ? 'bg-green-500' : 'bg-surface-container-high disabled:opacity-50 disabled:cursor-not-allowed'" class="relative w-10 h-5 rounded-full transition-colors duration-300 focus:outline-none shrink-0">
                <span :class="newArticle.status === 'active' ? 'translate-x-5 bg-white' : 'translate-x-0 bg-on-surface-variant'" class="inline-block w-4 h-4 transform rounded-full transition-transform duration-300 shadow-md absolute top-0.5 left-0.5"></span>
             </button>
          </div>

          <!-- Nội dung chi tiết (TipTap WYSIWYG Editor) -->
          <div class="space-y-2">
            <div class="flex justify-between items-center">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
                Nội dung chi tiết <span class="text-red-400">*</span>
              </label>
              <span class="text-[10px] text-on-surface-variant/60 font-mono">{{ contentLength }}/10000</span>
            </div>

            <div data-field="content" :class="articleErrors.content ? 'ring-1 ring-red-500 rounded-xl' : ''">
              <TipTapEditor
                v-model="newArticle.content"
                @blur="validateArticleField('content')"
                placeholder="Nhập nội dung chi tiết bài viết (chữ in đậm, nghiêng, tiêu đề, danh sách, tải ảnh trực tiếp)..."
              />
            </div>
            <p v-if="articleErrors.content" class="text-[10px] text-red-400 font-bold">{{ articleErrors.content }}</p>
          </div>
        </div>

        <!-- Drawer Footer -->
        <div class="p-6 border-t border-outline-variant/10 bg-surface-container-lowest flex gap-4">
          <button @click="isArticleDrawerOpen = false" class="flex-1 px-6 py-4 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy bỏ</button>
          <button @click="handleSaveArticle" :disabled="isSavingArticle || isUploadingArticleImage" class="flex-1 px-6 py-4 rounded-xl bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform shadow-xl shadow-primary/20 disabled:opacity-60 disabled:hover:scale-100 flex items-center justify-center gap-2">
            <span v-if="isSavingArticle" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
            {{ isSavingArticle ? 'Đang lưu...' : (editingArticleId ? 'Cập nhật tin' : 'Đăng tin') }}
          </button>
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
                <!-- 0 phim: Tất cả phim -->
                <span v-if="getPromoMoviesList(detailTarget).length === 0" class="text-sm font-bold text-on-surface-variant/50">
                  Tất cả phim
                </span>
                <!-- 1 phim: Hiển thị tên phim gọn gàng -->
                <span v-else-if="getPromoMoviesList(detailTarget).length === 1" class="text-sm font-bold text-on-surface truncate text-right max-w-[220px]" :title="getPromoMoviesList(detailTarget)[0].title">
                  {{ getPromoMoviesList(detailTarget)[0].title }}
                </span>
                <!-- Nhiều phim (>= 2): Badge đếm thông minh tương tác (Phương án 1) -->
                <button 
                  v-else 
                  @click="openPromoMoviesModal(detailTarget)" 
                  type="button" 
                  class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-sm bg-primary/10 hover:bg-primary/20 border border-primary/30 text-primary transition-all duration-200 cursor-pointer group shadow-sm shrink-0"
                  title="Bấm để xem danh sách phim áp dụng"
                >
                  <span class="material-symbols-outlined text-[13px] text-primary/80 group-hover:scale-110 transition-transform">movie</span>
                  <span class="text-xs font-black tracking-wide">{{ getPromoMoviesList(detailTarget).length }} phim áp dụng</span>
                  <span class="material-symbols-outlined text-[14px] text-primary/80 group-hover:translate-x-0.5 transition-transform">chevron_right</span>
                </button>
              </div>
              <div class="flex justify-between items-center gap-3">
                <span class="text-[10px] uppercase tracking-wider text-on-surface-variant/60">Hình thức hiển thị</span>
                <span class="text-sm font-bold" :class="detailTarget.isHidden ? 'text-purple-400' : 'text-sky-400'">{{ detailTarget.isHidden ? 'Riêng tư (Không công khai)' : 'Công khai toàn hệ thống' }}</span>
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
          <button v-if="can('promotions', 'edit')" @click="handleToggleVoucher(detailTarget); closeDetail()"
            class="flex-1 px-4 py-3 rounded-xl border text-[10px] font-bold uppercase tracking-widest transition-colors flex items-center justify-center gap-1.5"
            :class="detailTarget.isActive === false ? 'border-green-500/30 text-green-400 hover:bg-green-500/10' : 'border-amber-500/30 text-amber-400 hover:bg-amber-500/10'">
            <span class="material-symbols-outlined text-lg leading-none">{{ detailTarget.isActive === false ? 'toggle_on' : 'toggle_off' }}</span>
            {{ detailTarget.isActive === false ? 'Kích hoạt' : 'Tạm dừng' }}
          </button>
          <button @click="openEditVoucher(detailTarget); closeDetail()" class="flex-1 px-4 py-3 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors flex items-center justify-center gap-1">
            <span class="material-symbols-outlined text-sm">edit</span> Sửa
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

    <!-- Promo Movies List Modal (Xem danh sách phim áp dụng - Phương án 1) -->
    <div v-if="promoMoviesModalTarget" class="fixed inset-0 z-[1050] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/70 backdrop-blur-md" @click="closePromoMoviesModal"></div>
      <div class="relative w-full max-w-lg bg-surface-container-low border border-outline-variant/20 rounded-2xl shadow-2xl flex flex-col max-h-[82vh] overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <!-- Header -->
        <div class="p-5 border-b border-outline-variant/10 flex justify-between items-start gap-3 bg-surface-container/50">
          <div class="min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="material-symbols-outlined text-primary text-base">movie</span>
              <h3 class="font-headline font-black uppercase italic text-primary text-base">Danh sách phim áp dụng</h3>
            </div>
            <p class="text-xs text-on-surface-variant font-bold">
              Mã: <span class="font-mono text-primary">{{ promoMoviesModalTarget.code }}</span> · 
              <span class="text-on-surface">{{ getPromoMoviesList(promoMoviesModalTarget).length }} phim được áp dụng</span>
            </p>
          </div>
          <button @click="closePromoMoviesModal" class="w-8 h-8 shrink-0 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined text-lg">close</span>
          </button>
        </div>

        <!-- Search box if multiple movies -->
        <div v-if="getPromoMoviesList(promoMoviesModalTarget).length > 3" class="px-5 pt-3.5 pb-2">
          <div class="relative">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-base pointer-events-none">search</span>
            <input 
              v-model="promoMoviesSearch" 
              type="text" 
              placeholder="Tìm kiếm theo tên phim..." 
              class="w-full bg-surface-container-highest border border-outline-variant/15 rounded-sm pl-9 pr-3 py-2 text-xs text-on-surface focus:border-primary outline-none" 
            />
          </div>
        </div>

        <!-- Movie List -->
        <div class="flex-1 overflow-y-auto p-5 space-y-2 scrollbar-custom">
          <div v-if="filteredPromoMovies.length === 0" class="py-8 text-center text-on-surface-variant text-xs italic">
            Không tìm thấy phim phù hợp với từ khóa tìm kiếm.
          </div>
          <div 
            v-else 
            v-for="(m, idx) in filteredPromoMovies" 
            :key="m.id || idx"
            class="flex items-center gap-3 bg-surface-container p-2.5 rounded-sm border border-outline-variant/10 hover:border-primary/30 transition-colors"
          >
            <!-- STT Badge -->
            <span class="text-[9.5px] font-mono font-bold text-primary bg-primary/10 px-1.5 py-0.5 rounded-sm shrink-0 border border-primary/20">
              #{{ idx + 1 }}
            </span>

            <!-- Poster Thumbnail -->
            <div class="w-9 h-12 rounded-sm bg-surface-container-highest overflow-hidden shrink-0 border border-white/10 flex items-center justify-center">
              <img v-if="m.posterUrl" :src="m.posterUrl" :alt="m.title" class="w-full h-full object-cover" />
              <span v-else class="material-symbols-outlined text-base text-on-surface-variant/40">theaters</span>
            </div>

            <!-- Title & Metadata -->
            <div class="min-w-0 flex-1">
              <p class="text-xs font-bold text-on-surface truncate" :title="m.title">{{ m.title }}</p>
              <div class="flex items-center gap-2 text-[10px] text-on-surface-variant/70 mt-0.5 font-medium">
                <span v-if="m.durationMinutes">{{ m.durationMinutes }} phút</span>
                <span v-if="m.durationMinutes && m.genre">·</span>
                <span v-if="m.genre" class="truncate">{{ m.genre }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="p-3.5 border-t border-outline-variant/10 bg-surface-container/30 flex justify-end">
          <button 
            @click="closePromoMoviesModal" 
            class="px-5 py-2 bg-surface-container-highest hover:bg-surface-container-high text-on-surface text-xs font-bold uppercase tracking-wider rounded-sm transition-colors cursor-pointer"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wysiwyg-editor:empty:before {
  content: attr(data-placeholder);
  color: rgba(148, 163, 184, 0.4);
  pointer-events: none;
}

.wysiwyg-editor h2 {
  font-size: 1.25rem;
  font-weight: 800;
  text-transform: uppercase;
  color: #fff;
  margin-top: 1rem;
  margin-bottom: 0.5rem;
  padding-bottom: 0.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.wysiwyg-editor h3 {
  font-size: 1.05rem;
  font-weight: 700;
  text-transform: uppercase;
  color: #f5c518;
  margin-top: 0.75rem;
  margin-bottom: 0.35rem;
}

.wysiwyg-editor p {
  margin-bottom: 0.5rem;
}

.wysiwyg-editor ul {
  list-style-type: disc;
  padding-left: 1.5rem;
  margin: 0.5rem 0;
}

.wysiwyg-editor ol {
  list-style-type: decimal;
  padding-left: 1.5rem;
  margin: 0.5rem 0;
}

.wysiwyg-editor li {
  margin-bottom: 0.25rem;
}

.wysiwyg-editor blockquote {
  border-left: 4px solid #f5c518;
  padding: 0.5rem 0.75rem;
  margin: 0.5rem 0;
  background: rgba(245, 197, 24, 0.06);
  font-style: italic;
  color: #cbd5e1;
  border-radius: 0 0.5rem 0.5rem 0;
}

.wysiwyg-editor img {
  border-radius: 0.75rem;
  max-width: 100%;
  display: inline-block;
  box-shadow: 0 10px 20px -5px rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.wysiwyg-editor a {
  color: #f5c518;
  text-decoration: underline;
  font-weight: 600;
}

.wysiwyg-editor hr {
  margin: 1rem 0;
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}

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
