<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { bannerApi } from '@/api/admin/index'
import api from '@/api/axios'
import { useToastStore } from '@/stores/toast'
import { useConfirmStore } from '@/stores/confirm'
import { friendlyError } from '@/utils/friendlyError'
import { prepareImageForUpload } from '@/utils/imageUpload'
import { useAdminPerm } from '@/composables/useAdminPerm'
import TrailerModal from '@/components/common/TrailerModal.vue'

const { can } = useAdminPerm()
const toast = useToastStore()
const confirm = useConfirmStore()

const banners = ref([])
const isLoading = ref(false)
const isModalOpen = ref(false)
const isSaving = ref(false)
const isUploading = ref(false)
const editingId = ref(null) // null = thêm mới, có id = đang sửa
const movies = ref([])      // danh sách phim cho dropdown chế độ "Theo phim"
const movieImages = ref({}) // movieId -> URL ảnh phim (bannerUrl ưu tiên), khớp với banner trang chủ

// Ngày hôm nay ('YYYY-MM-DD') để khóa min cho ô lịch — chặn chọn ngày quá khứ ngay trên Date Picker.
const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})
// Trần thứ tự ưu tiên: thêm mới -> tổng banner + 1 (vị trí cuối); sửa -> tổng banner hiện có.
const priorityMax = computed(() => banners.value.length + (editingId.value ? 0 : 1))

// ===== Combobox tìm kiếm phim cho dropdown "Chọn phim" =====
const movieSearch = ref('')
const movieDropdownOpen = ref(false)
const movieSelectRef = ref(null)   // wrapper để bắt sự kiện click ra ngoài
const movieSearchInput = ref(null) // ô nhập tìm kiếm (auto focus khi mở)

// Bỏ dấu tiếng Việt để tìm kiếm không phân biệt dấu (vd gõ "nguoi dep" ra "Người đẹp").
const normalizeVi = (s) => (s || '').toString().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/gi, 'd').toLowerCase().trim()

const isArchived = (m) => String(m?.status).toLowerCase() === 'archived'
const movieStatusLabel = (m) => isArchived(m) ? ' (Ngừng chiếu)' : (m.status === 'active' ? '' : ` (${m.status})`)

const selectedMovieLabel = computed(() => {
  const m = movies.value.find(x => x.id === form.value.movieId)
  return m ? `${m.title}${movieStatusLabel(m)}` : '-- Chọn phim hiển thị --'
})

const filteredMovies = computed(() => {
  const q = normalizeVi(movieSearch.value)
  if (!q) return movies.value
  return movies.value.filter(m => normalizeVi(m.title).includes(q))
})

const openMovieDropdown = () => {
  movieDropdownOpen.value = true
  movieSearch.value = ''
  nextTick(() => movieSearchInput.value?.focus())
}
const toggleMovieDropdown = () => { movieDropdownOpen.value ? (movieDropdownOpen.value = false) : openMovieDropdown() }

const selectMovie = (m) => {
  if (isArchived(m)) return // phim ngừng chiếu không cho chọn
  form.value.movieId = m.id
  movieDropdownOpen.value = false
}

const onClickOutsideMovie = (e) => {
  if (movieDropdownOpen.value && movieSelectRef.value && !movieSelectRef.value.contains(e.target)) {
    movieDropdownOpen.value = false
  }
}
onMounted(() => document.addEventListener('mousedown', onClickOutsideMovie))
onBeforeUnmount(() => document.removeEventListener('mousedown', onClickOutsideMovie))

const blankForm = () => ({ title: '', imageUrl: '', link: '', isActive: true, order: 1, startDate: '', endDate: '', mode: 'IMAGE', movieId: null })
const form = ref(blankForm())

const fetchMovies = async () => {
  try {
    const { data } = await api.get('/movies')
    movies.value = (Array.isArray(data) ? data : (data.data ?? [])).map(m => ({
      id: m.id, title: m.title, status: m.status, posterUrl: m.posterUrl || null,
      trailerUrl: m.trailerUrl || null,
    }))
  } catch (e) {
    console.error('Failed to load movies', e)
  }
}

// Backend trả LocalDateTime dạng chuỗi ISO hoặc mảng [y,m,d,...] -> chuẩn hoá về 'YYYY-MM-DD' cho input date
const toDateInput = (dt) => {
  if (!dt) return ''
  if (Array.isArray(dt)) return `${dt[0]}-${String(dt[1]).padStart(2, '0')}-${String(dt[2]).padStart(2, '0')}`
  if (typeof dt === 'string') return dt.split('T')[0]
  return ''
}

const fetchBanners = async () => {
  isLoading.value = true
  now.value = new Date() // làm mới mốc thời gian để tính lại banner còn hạn hay không
  try {
    const { data } = await bannerApi.getAll()
    const list = data.data ?? data
    // Chuẩn hoá: entity serialize field 'displayOrder' -> map sang 'order' để template dùng nhất quán
    // Sắp xếp tăng dần theo thứ tự để vị trí hiển thị (#1, #2...) khớp dữ liệu DB
    banners.value = list
      .map(b => ({ ...b, order: b.displayOrder ?? 0 }))
      .sort((a, b) => (a.order || 0) - (b.order || 0))
    await fetchBannerMovieImages() // nạp ảnh phim (bannerUrl) cho banner chế độ MOVIE
  } catch (e) {
    console.error('Failed to load banners', e)
    toast.error(friendlyError(e, 'Không tải được danh sách banner.'))
  } finally {
    isLoading.value = false
  }
}

// Ảnh phim cho card banner MOVIE: lấy từ /movies/{id} (có bannerUrl) để KHỚP với banner trang chủ,
// vì list /movies (MovieSummaryDTO) chỉ trả posterUrl.
const fetchBannerMovieImages = async () => {
  const ids = [...new Set(banners.value.filter(b => b.mode === 'MOVIE' && b.movieId).map(b => b.movieId))]
  const missing = ids.filter(id => !(id in movieImages.value))
  if (!missing.length) return
  const results = await Promise.allSettled(missing.map(id => api.get(`/movies/${id}`)))
  const map = { ...movieImages.value }
  results.forEach((r, i) => {
    if (r.status === 'fulfilled') {
      const m = r.value.data?.data ?? r.value.data
      map[missing[i]] = m?.bannerUrl || m?.posterUrl || null
    }
  })
  movieImages.value = map
}

const originalStartDate = ref('') // ngày bắt đầu đang lưu (để bỏ qua kiểm tra "quá khứ" khi sửa mà không đổi ngày)

const openAddModal = () => {
  editingId.value = null
  originalStartDate.value = ''
  form.value = { ...blankForm(), order: Math.min(banners.value.length + 1, 99) }
  isModalOpen.value = true
}

const openEditModal = (banner) => {
  editingId.value = banner.id
  originalStartDate.value = toDateInput(banner.startDate)
  form.value = {
    title: banner.title || '',
    imageUrl: banner.imageUrl || '',
    link: banner.link || '',
    isActive: banner.isActive,
    order: banner.order ?? 1,
    startDate: toDateInput(banner.startDate),
    endDate: toDateInput(banner.endDate),
    mode: banner.mode || 'IMAGE',
    movieId: banner.movieId ?? null,
  }
  isModalOpen.value = true
}

const closeModal = () => { isModalOpen.value = false; movieDropdownOpen.value = false }

// Upload ảnh lên Cloudinary qua /api/upload (giống màn Tin khuyến mãi / F&B)
const handleImageUpload = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  let prepared
  try {
    // Banner chỉ nhận JPG/PNG/WEBP (không GIF), tối đa 5MB.
    prepared = await prepareImageForUpload(file, { types: ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'] })
  } catch (err) {
    toast.error(err.message)
    e.target.value = ''
    return
  }
  isUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', prepared)
    const { data } = await api.post('/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    form.value.imageUrl = data.url
    toast.success('Tải ảnh lên thành công.')
  } catch (err) {
    toast.error(friendlyError(err, 'Tải ảnh thất bại.'))
  } finally {
    isUploading.value = false
    e.target.value = '' // cho phép chọn lại cùng file
  }
}

// Gói payload gửi backend. Chuẩn hoá mốc giờ: bắt đầu -> 00:00:00, kết thúc -> 23:59:59.
// Để trống ngày bắt đầu = bắt đầu ngay; để trống ngày kết thúc = treo vô thời hạn.
const buildPayload = () => ({
  title: form.value.title?.trim() || null,
  imageUrl: form.value.mode === 'IMAGE' ? (form.value.imageUrl?.trim() || null) : null,
  link: form.value.link?.trim() || null,
  mode: form.value.mode,
  movieId: form.value.mode === 'MOVIE' ? (form.value.movieId || null) : null,
  placement: 'HOME',
  isActive: form.value.isActive,
  order: Number(form.value.order) || 0,
  startDate: form.value.startDate ? `${form.value.startDate}T00:00:00` : null,
  endDate: form.value.endDate ? `${form.value.endDate}T23:59:59` : null,
})

// Khi đổi ngày bắt đầu: nếu ngày kết thúc đang chọn trước đó -> xóa để buộc chọn lại (đồng bộ với :min).
const onStartDateChange = () => {
  if (form.value.endDate && form.value.startDate && form.value.endDate < form.value.startDate) {
    form.value.endDate = ''
  }
}

// Trả về thông báo lỗi đầu tiên gặp phải; null nếu form hợp lệ.
const validateBannerForm = () => {
  const f = form.value
  // Chế độ hiển thị
  if (f.mode !== 'IMAGE' && f.mode !== 'MOVIE') return 'Vui lòng chọn chế độ hiển thị.'
  // Tiêu đề: 5–100 ký tự, không rỗng/toàn khoảng trắng, không chứa thẻ HTML/mã độc
  const title = (f.title || '').trim()
  if (/<[^>]*>/.test(title) || title.length < 5 || title.length > 100) {
    return 'Tiêu đề banner phải từ 5 - 100 ký tự và không chứa mã độc.'
  }
  if (f.mode === 'IMAGE') {
    // Ảnh: bắt buộc upload file
    if (!f.imageUrl?.trim()) return 'Vui lòng tải lên file ảnh banner hợp lệ (định dạng JPG/PNG/WEBP, tối đa 5MB).'
    // Link điều hướng (tuỳ chọn): nếu nhập phải là URL http(s) hoặc đường dẫn nội bộ bắt đầu bằng /
    const link = (f.link || '').trim()
    if (link && !(link.startsWith('/') || link.startsWith('http://') || link.startsWith('https://'))) {
      return 'Đường dẫn điều hướng không hợp lệ.'
    }
  } else {
    // Theo phim: bắt buộc chọn phim còn khả dụng (không ngừng chiếu)
    if (!f.movieId) return 'Vui lòng chọn phim liên kết với Banner này.'
    const picked = movies.value.find(m => m.id === f.movieId)
    if (picked && String(picked.status).toLowerCase() === 'archived') {
      return 'Phim được chọn hiện không còn khả dụng để tạo banner.'
    }
  }
  // Thứ tự ưu tiên: số nguyên dương, tối đa = tổng số banner (thêm mới cho phép vị trí cuối)
  const order = Number(f.order)
  if (!Number.isInteger(order) || order < 1 || order > priorityMax.value) {
    return `Thứ tự ưu tiên phải là số nguyên dương từ 1 đến ${priorityMax.value}.`
  }
  // Ngày: mốc giờ gán ở buildPayload (bắt đầu 00:00:00, kết thúc 23:59:59)
  const today = todayStr.value
  const isCreate = !editingId.value
  if (f.startDate) {
    // Chặn quá khứ khi tạo mới; khi sửa chỉ chặn nếu ngày bắt đầu thực sự bị đổi
    const startChanged = isCreate || f.startDate !== originalStartDate.value
    if (startChanged && f.startDate < today) return 'Ngày bắt đầu không được ở trong quá khứ.'
  }
  if (f.endDate) {
    if (isCreate && f.endDate < today) return 'Ngày kết thúc phải sau thời điểm hiện tại.'
    if (f.startDate && f.startDate > f.endDate) return 'Ngày kết thúc phải lớn hơn ngày bắt đầu.'
  }
  return null
}

const saveBanner = async () => {
  const err = validateBannerForm()
  if (err) { toast.warning(err); return }
  isSaving.value = true
  try {
    if (editingId.value) {
      await bannerApi.update(editingId.value, buildPayload())
      toast.success('Cập nhật banner thành công.')
    } else {
      await bannerApi.create(buildPayload())
      toast.success('Thêm banner thành công.')
    }
    await fetchBanners()
    closeModal()
  } catch (e) {
    console.error('Failed to save banner', e)
    toast.error(friendlyError(e, 'Lưu banner thất bại.'))
  } finally {
    isSaving.value = false
  }
}

const toggleActive = async (banner) => {
  try {
    await bannerApi.update(banner.id, { isActive: !banner.isActive })
    banner.isActive = !banner.isActive
  } catch (e) {
    console.error('Failed to toggle banner', e)
    toast.error(friendlyError(e, 'Không đổi được trạng thái banner.'))
  }
}

// ===== Kéo-thả sắp xếp thứ tự banner (HTML5 Drag & Drop thuần, không thư viện) =====
const dragIndex = ref(null)     // vị trí card đang nhấc
const dragOverIndex = ref(null) // vị trí card đang được rê tới (để hiện viền gợi ý)
const grabbing = ref(false)     // chỉ cho kéo khi nhấn từ tay cầm (drag handle)

const resetDrag = () => { dragIndex.value = null; dragOverIndex.value = null; grabbing.value = false }

const onCardDragStart = (e, index) => {
  // Chỉ khởi động kéo khi người dùng bấm vào tay cầm; bấm chỗ khác -> huỷ
  if (!grabbing.value) { e.preventDefault(); return }
  dragIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(index)) // Firefox cần có dữ liệu mới cho kéo
}

const onCardDragOver = (e, index) => {
  if (dragIndex.value === null) return
  e.preventDefault() // cho phép thả
  e.dataTransfer.dropEffect = 'move'
  dragOverIndex.value = index
}

const onCardDrop = (e, index) => {
  e.preventDefault()
  moveCard(dragIndex.value, index)
  resetDrag()
}

const moveCard = (from, to) => {
  if (from === null || to === null || from === to) return
  const arr = banners.value
  const [item] = arr.splice(from, 1)
  arr.splice(to, 0, item)
  persistOrder()
}

// Đánh số lại 1..n theo vị trí mới, gửi 1 request bulk chứa các banner thực sự đổi thứ tự
const persistOrder = async () => {
  const changed = []
  banners.value.forEach((b, idx) => {
    const newOrder = idx + 1
    if (b.order !== newOrder) { b.order = newOrder; changed.push({ id: b.id, order: newOrder }) }
  })
  if (!changed.length) return
  try {
    await bannerApi.reorder(changed)
    toast.success('Đã cập nhật thứ tự banner.')
  } catch (e) {
    console.error('Failed to reorder banners', e)
    toast.error(friendlyError(e, 'Không lưu được thứ tự. Đang tải lại danh sách.'))
    fetchBanners() // khôi phục về trạng thái server nếu lưu lỗi
  }
}

const deleteBanner = async (id) => {
  const ok = await confirm.show({
    title: 'Xoá banner',
    message: 'Bạn có chắc chắn muốn xoá banner này? Hành động không thể hoàn tác.',
    confirmText: 'Xoá',
    tone: 'danger',
  })
  if (!ok) return
  try {
    await bannerApi.delete(id)
    banners.value = banners.value.filter(b => b.id !== id)
    toast.success('Đã xoá banner.')
  } catch (e) {
    console.error('Failed to delete banner', e)
    toast.error(friendlyError(e, 'Xoá banner thất bại.'))
  }
}

const movieTitleById = (id) => movies.value.find(m => m.id === id)?.title || `Phim #${id}`
const movieTrailerById = (id) => movies.value.find(m => m.id === id)?.trailerUrl || null

// Xem trước trailer ngay trong màn quản trị (không autoplay trong grid — chỉ mở khi bấm nút Play)
const previewUrl = ref('')
const isPreviewOpen = ref(false)
const openTrailerPreview = (banner) => {
  const url = movieTrailerById(banner.movieId)
  if (!url) return
  previewUrl.value = url
  isPreviewOpen.value = true
}
// Ảnh đại diện phim cho card banner "Theo phim": ưu tiên bannerUrl (từ /movies/{id}, khớp trang chủ),
// tạm thời dùng posterUrl từ list trong lúc chờ chi tiết tải xong.
const movieImageById = (id) => movieImages.value[id] || movies.value.find(m => m.id === id)?.posterUrl || null

// Link điều hướng thực tế của banner khi khách bấm vào:
// - Theo phim  -> trang chi tiết phim /movie/:id (tự gắn, không cần nhập tay)
// - Ảnh        -> link admin tự nhập (có thể để trống)
const bannerLink = (banner) =>
  banner.mode === 'MOVIE'
    ? (banner.movieId ? `/movie/${banner.movieId}` : null)
    : (banner.link || null)

// ===== Trạng thái hiển thị THỰC TẾ trên trang chủ =====
// Số thứ tự (#N) chỉ là vị trí ưu tiên trong toàn bộ danh sách; nó KHÔNG cho biết banner có
// đang xuất hiện với khách hay không. Ở đây tính lại đúng điều kiện findActiveBanners của backend
// (đang bật + trong khoảng ngày + nếu MOVIE thì phim phải đang chiếu) để làm mờ + gắn nhãn lý do,
// giúp admin phân biệt banner nào khách thực sự thấy.
const now = ref(new Date())

// Backend trả LocalDateTime dạng chuỗi ISO hoặc mảng [y,m,d,H,M,S] -> Date (đủ giờ để so mốc)
const toDateTime = (dt) => {
  if (!dt) return null
  if (Array.isArray(dt)) {
    const [y, mo = 1, d = 1, h = 0, mi = 0, s = 0] = dt
    return new Date(y, mo - 1, d, h, mi, s)
  }
  const t = new Date(dt)
  return isNaN(t.getTime()) ? null : t
}

const bannerVisibility = (b) => {
  if (!b.isActive) return { live: false, label: 'Đang tắt', tone: 'off' }
  const start = toDateTime(b.startDate)
  const end = toDateTime(b.endDate)
  const t = now.value
  if (start && start > t) return { live: false, label: 'Chưa tới hạn', tone: 'scheduled' }
  if (end && end < t) return { live: false, label: 'Đã hết hạn', tone: 'expired' }
  if (b.mode === 'MOVIE') {
    // Khớp backend: banner theo phim hiện với phim đang chiếu VÀ sắp chiếu (quảng cáo trước);
    // chỉ ẩn khi phim ngừng chiếu (archived) hoặc đã bị xoá khỏi hệ thống.
    const m = movies.value.find(x => x.id === b.movieId)
    if (!m || String(m.status).toLowerCase() === 'archived') return { live: false, label: 'Phim ngừng chiếu', tone: 'movie' }
  }
  return { live: true, label: 'Đang hiển thị', tone: 'live' }
}

// Tính 1 lần cho cả danh sách (reactive theo banners/movies/now), tránh gọi lặp trong template
const visibilityMap = computed(() => {
  const map = {}
  banners.value.forEach(b => { map[b.id] = bannerVisibility(b) })
  return map
})

const visToneClass = {
  expired: 'bg-red-600 text-white border-red-700',
  scheduled: 'bg-amber-500 text-black border-amber-600',
  off: 'bg-neutral-700 text-white border-neutral-600',
  movie: 'bg-orange-600 text-white border-orange-700',
}
const visIcon = { expired: 'event_busy', scheduled: 'schedule', off: 'visibility_off', movie: 'movie' }

onMounted(() => { fetchBanners(); fetchMovies() })
</script>

<template>
  <div class="p-10 relative h-full flex flex-col">
    <!-- Header -->
    <header class="mb-8 text-on-surface flex justify-between items-end">
      <div>
        <h1 class="text-3xl font-extrabold tracking-tight font-headline uppercase">Quản lý Banner</h1>
        <p class="text-on-surface-variant text-sm mt-1">Tuỳ chỉnh banner trang chủ — kéo biểu tượng <span class="material-symbols-outlined text-sm align-middle">drag_indicator</span> trên mỗi thẻ để sắp xếp thứ tự</p>
      </div>
      <button v-if="can('banners', 'add')" @click="openAddModal" class="px-6 py-3 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded-sm hover:brightness-110 transition-all flex items-center gap-2">
        <span class="material-symbols-outlined text-sm">add_photo_alternate</span>
        Thêm Banner Mới
      </button>
    </header>

    <!-- Banner Grid -->
    <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <div v-for="i in 4" :key="i" class="bg-surface-container-low border border-outline-variant/10 rounded-xl h-64 animate-pulse"></div>
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 items-stretch content-start flex-1 overflow-y-auto pr-2 pb-10">
      <div v-for="(banner, i) in banners" :key="banner.id"
           draggable="true"
           @dragstart="onCardDragStart($event, i)"
           @dragover="onCardDragOver($event, i)"
           @drop="onCardDrop($event, i)"
           @dragend="resetDrag"
           class="bg-surface-container-low border rounded-xl overflow-hidden flex flex-col shadow-sm hover:shadow-md transition-all group"
           :class="[
             dragIndex === i ? 'opacity-40 scale-95' : '',
             dragOverIndex === i && dragIndex !== i ? 'border-primary ring-2 ring-primary/40' : 'border-outline-variant/10'
           ]">

        <!-- Image Preview — giữ tỉ lệ 16:9 khớp khung banner thật (hero full màn hình) ở trang chủ -->
        <div class="relative aspect-video w-full bg-surface-container-highest overflow-hidden">
          <!-- Lớp MEDIA (ảnh/placeholder): CHỈ layer này bị làm xám + mờ khi banner không hiển thị,
               nhờ vậy các tag trạng thái phía trên vẫn giữ màu đặc, rõ ràng (filter/opacity ở cha
               luôn ảnh hưởng con nên phải tách riêng). -->
          <div class="absolute inset-0" :class="visibilityMap[banner.id] && !visibilityMap[banner.id].live ? 'grayscale opacity-60' : ''">
            <img v-if="banner.mode !== 'MOVIE' && banner.imageUrl" :src="banner.imageUrl" draggable="false" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" alt="Banner preview" />
            <template v-else-if="banner.mode === 'MOVIE'">
              <!-- Có ảnh phim: hiện ảnh + phủ tên phim ở đáy -->
              <template v-if="movieImageById(banner.movieId)">
                <img :src="movieImageById(banner.movieId)" draggable="false" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" alt="Ảnh phim" />
                <div class="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/80 to-transparent px-3 py-2">
                  <span class="text-xs font-bold text-white line-clamp-1">{{ movieTitleById(banner.movieId) }}</span>
                </div>
              </template>
              <!-- Không có ảnh phim: giữ placeholder icon + tên -->
              <div v-else class="w-full h-full flex flex-col items-center justify-center text-on-surface-variant gap-2">
                <span class="material-symbols-outlined text-4xl text-primary/40">movie</span>
                <span class="text-xs font-bold text-on-surface px-3 text-center line-clamp-2">{{ movieTitleById(banner.movieId) }}</span>
              </div>
            </template>
            <div v-else class="w-full h-full flex items-center justify-center text-on-surface-variant">
              <span class="material-symbols-outlined text-4xl opacity-20">broken_image</span>
            </div>
          </div>

          <!-- Mode Badge -->
          <div class="absolute top-3 left-3 px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest backdrop-blur-md border bg-black/40 text-on-surface border-white/10 flex items-center gap-1">
            <span class="material-symbols-outlined text-xs">{{ banner.mode === 'MOVIE' ? 'movie' : 'image' }}</span>
            {{ banner.mode === 'MOVIE' ? 'Theo phim' : 'Ảnh' }}
          </div>

          <!-- Status Badge Overlay -->
          <div class="absolute top-3 right-3 px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest backdrop-blur-md border"
               :class="banner.isActive ? 'bg-green-500/20 text-green-400 border-green-500/30' : 'bg-red-500/20 text-red-400 border-red-500/30'">
            {{ banner.isActive ? 'Đang bật' : 'Đang tắt' }}
          </div>

          <!-- Nhãn lý do KHÔNG hiển thị trên trang chủ (dù đang bật) — hết hạn / chưa tới hạn / phim ngừng chiếu.
               Bỏ qua khi lý do là 'off' vì badge "Đang tắt" bên trên đã nói rõ. -->
          <div v-if="visibilityMap[banner.id] && !visibilityMap[banner.id].live && visibilityMap[banner.id].tone !== 'off'"
               class="absolute top-3 left-1/2 -translate-x-1/2 px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-widest backdrop-blur-md border flex items-center gap-1 z-10 whitespace-nowrap"
               :class="visToneClass[visibilityMap[banner.id].tone]">
            <span class="material-symbols-outlined text-xs">{{ visIcon[visibilityMap[banner.id].tone] }}</span>
            {{ visibilityMap[banner.id].label }}
          </div>

          <!-- Nút Play: chỉ hiện cho banner theo phim có trailer; bấm để xem trước (không autoplay trong grid) -->
          <button v-if="banner.mode === 'MOVIE' && movieTrailerById(banner.movieId)"
                  type="button" @click.stop="openTrailerPreview(banner)" draggable="false"
                  class="absolute inset-0 m-auto w-12 h-12 rounded-full bg-black/50 hover:bg-primary text-white hover:text-on-primary backdrop-blur-sm border border-white/20 flex items-center justify-center transition-all opacity-80 hover:opacity-100 hover:scale-110 z-10"
                  title="Xem trước trailer">
            <span class="material-symbols-outlined text-2xl">play_arrow</span>
          </button>
        </div>

        <!-- Banner Info -->
        <div class="p-4 flex flex-col gap-3 flex-1">
          <div class="space-y-1">
            <p class="text-base font-bold text-on-surface truncate leading-relaxed py-0.5">{{ banner.title || 'Banner không tiêu đề' }}</p>
            <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-1.5">Link điều hướng</p>
            <p class="text-xs truncate font-mono px-2 py-1 rounded"
               :class="bannerLink(banner) ? 'text-primary bg-primary/10' : 'text-on-surface-variant/60 bg-surface-container-highest'">
              {{ bannerLink(banner) || 'Không có link' }}
              <span v-if="banner.mode === 'MOVIE' && banner.movieId" class="text-on-surface-variant/50">(tự gắn)</span>
            </p>
          </div>

          <!-- Luôn render (kể cả khi trống) để mọi card cao bằng nhau; truncate chống rớt dòng -->
          <div class="flex items-center gap-1.5 text-[11px] text-on-surface-variant min-w-0">
            <span class="material-symbols-outlined text-sm shrink-0">date_range</span>
            <span class="truncate">{{ toDateInput(banner.startDate) || 'Bắt đầu ngay' }} → {{ toDateInput(banner.endDate) || 'Vô thời hạn' }}</span>
          </div>

          <div class="flex items-center justify-between mt-auto pt-3 border-t border-outline-variant/10">
            <!-- Tay cầm kéo + huy hiệu vị trí: nhấn giữ icon này để nhấc card sắp xếp -->
            <div @mousedown="grabbing = true" @mouseup="grabbing = false"
                 class="flex items-center gap-1.5 cursor-grab active:cursor-grabbing select-none text-on-surface-variant hover:text-on-surface transition-colors"
                 title="Kéo để sắp xếp thứ tự">
              <span class="material-symbols-outlined text-lg">drag_indicator</span>
              <!-- Hiện đúng thứ tự ưu tiên (displayOrder) đang lưu, không phải vị trí trong mảng -->
              <span class="text-xs font-bold tracking-wide tabular-nums">#{{ banner.order }}</span>
            </div>

            <div class="flex items-center gap-2">
              <button v-if="can('banners', 'edit')" @click="openEditModal(banner)" class="w-8 h-8 rounded-full bg-surface-container-highest hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors" title="Sửa">
                <span class="material-symbols-outlined text-sm">edit</span>
              </button>
              <button v-if="can('banners', 'edit')" @click="toggleActive(banner)" class="w-8 h-8 rounded-full bg-surface-container-highest hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors" :title="banner.isActive ? 'Tắt' : 'Bật'">
                <span class="material-symbols-outlined text-sm">{{ banner.isActive ? 'visibility_off' : 'visibility' }}</span>
              </button>
              <button v-if="can('banners', 'delete')" @click="deleteBanner(banner.id)" class="w-8 h-8 rounded-full bg-surface-container-highest hover:bg-red-500/20 hover:text-red-400 flex items-center justify-center text-on-surface-variant transition-colors" title="Xoá">
                <span class="material-symbols-outlined text-sm">delete</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="!isLoading && banners.length === 0" class="flex flex-col items-center justify-center py-24 text-center">
      <span class="material-symbols-outlined text-5xl text-outline-variant mb-4">add_photo_alternate</span>
      <p class="text-on-surface-variant font-semibold">Chưa có banner nào</p>
    </div>

    <!-- Add/Edit Banner Modal Overlay -->
    <div v-if="isModalOpen" class="fixed inset-0 z-[120] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div class="bg-surface-container-low border border-outline-variant/20 rounded-xl w-full max-w-lg shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200 max-h-[90vh] flex flex-col">
        <div class="px-6 py-3 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <h2 class="font-headline font-bold uppercase tracking-tight text-on-surface">{{ editingId ? 'Sửa Banner' : 'Thêm Banner Mới' }}</h2>
          <button @click="closeModal" class="text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="p-5 space-y-4 overflow-y-auto flex-1 min-h-0">
          <!-- Chế độ hiển thị: CHỈ chọn khi thêm mới; khi sửa thì giữ nguyên loại của banner -->
          <div v-if="!editingId" class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Chế độ hiển thị</label>
            <div class="grid grid-cols-2 gap-2">
              <button type="button" @click="form.mode = 'IMAGE'"
                      :class="form.mode === 'IMAGE' ? 'bg-primary/15 border-primary text-primary' : 'bg-surface-container-high border-transparent text-on-surface-variant'"
                      class="flex items-center justify-center gap-2 py-2.5 rounded-lg border text-xs font-bold uppercase tracking-widest transition-all">
                <span class="material-symbols-outlined text-base">image</span> Ảnh
              </button>
              <button type="button" @click="form.mode = 'MOVIE'"
                      :class="form.mode === 'MOVIE' ? 'bg-primary/15 border-primary text-primary' : 'bg-surface-container-high border-transparent text-on-surface-variant'"
                      class="flex items-center justify-center gap-2 py-2.5 rounded-lg border text-xs font-bold uppercase tracking-widest transition-all">
                <span class="material-symbols-outlined text-base">movie</span> Theo phim
              </button>
            </div>
            <p class="text-[11px] text-on-surface-variant/70">{{ form.mode === 'MOVIE' ? 'Tự dựng banner từ thông tin phim được chọn (ảnh nền, tên, mô tả, nút mua vé).' : 'Hiển thị đúng ảnh bạn tải lên, không phủ chữ.' }}</p>
          </div>

          <!-- Khi sửa: hiện nhãn loại banner (chỉ đọc) thay cho bộ chọn chế độ -->
          <div v-else class="flex items-center gap-2 px-3 py-2 rounded-lg bg-surface-container-high w-fit">
            <span class="material-symbols-outlined text-base text-primary">{{ form.mode === 'MOVIE' ? 'movie' : 'image' }}</span>
            <span class="text-xs font-bold uppercase tracking-widest text-on-surface">{{ form.mode === 'MOVIE' ? 'Banner theo phim' : 'Banner ảnh' }}</span>
          </div>

          <!-- Tiêu đề -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tiêu đề <span class="text-error">*</span> <span class="normal-case font-normal text-on-surface-variant/50">(5–100 ký tự, nội bộ để dễ quản lý)</span></label>
            <input v-model="form.title" type="text" maxlength="100" placeholder="VD: Ưu đãi hè rực rỡ" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface">
          </div>

          <!-- CHẾ ĐỘ ẢNH: chỉ upload file từ máy (JPG/PNG/WEBP, tối đa 5MB) -->
          <div v-if="form.mode === 'IMAGE'" class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Hình ảnh banner</label>
            <label class="flex flex-col items-center justify-center gap-2 h-24 rounded-lg border-2 border-dashed border-outline-variant/30 hover:border-primary/50 cursor-pointer transition-colors bg-surface-container-high/40 overflow-hidden relative">
              <img v-if="form.imageUrl" :src="form.imageUrl" class="absolute inset-0 w-full h-full object-cover" />
              <div v-if="form.imageUrl" class="absolute inset-0 bg-black/40 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center text-white text-xs font-bold uppercase tracking-widest">Đổi ảnh khác</div>
              <template v-if="!form.imageUrl">
                <span class="material-symbols-outlined text-3xl text-on-surface-variant">{{ isUploading ? 'hourglass_empty' : 'cloud_upload' }}</span>
                <span class="text-xs text-on-surface-variant">{{ isUploading ? 'Đang tải lên...' : 'Bấm để tải ảnh lên' }}</span>
              </template>
              <input type="file" accept="image/jpeg,image/jpg,image/png,image/webp" class="hidden" @change="handleImageUpload" :disabled="isUploading" />
            </label>
            <p class="text-[10px] text-on-surface-variant/60">Định dạng JPG/PNG/WEBP · tối đa 5MB</p>
          </div>

          <!-- CHẾ ĐỘ THEO PHIM: combobox chọn phim có tìm kiếm -->
          <div v-else class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Chọn phim</label>
            <div ref="movieSelectRef" class="relative">
              <!-- Nút hiển thị phim đang chọn / mở dropdown -->
              <button type="button" @click="toggleMovieDropdown"
                      class="w-full flex items-center justify-between gap-2 bg-surface-container-high text-sm rounded-lg py-2.5 px-4 text-left transition-all"
                      :class="movieDropdownOpen ? 'ring-1 ring-primary' : ''">
                <span class="truncate" :class="form.movieId ? 'text-on-surface' : 'text-on-surface-variant'">{{ selectedMovieLabel }}</span>
                <span class="material-symbols-outlined text-lg text-on-surface-variant transition-transform" :class="movieDropdownOpen ? 'rotate-180' : ''">expand_more</span>
              </button>

              <!-- Panel dropdown: ô tìm kiếm + danh sách phim đã lọc -->
              <div v-if="movieDropdownOpen" class="absolute z-30 mt-1 w-full bg-surface-container-high rounded-lg shadow-2xl border border-outline-variant/20 overflow-hidden">
                <div class="p-2 border-b border-outline-variant/10">
                  <div class="flex items-center gap-2 bg-surface-container-highest rounded-lg px-3">
                    <span class="material-symbols-outlined text-base text-on-surface-variant">search</span>
                    <input ref="movieSearchInput" v-model="movieSearch" type="text" placeholder="Tìm tên phim..."
                           @keydown.esc="movieDropdownOpen = false"
                           class="w-full bg-transparent border-none text-sm py-2 text-on-surface focus:ring-0 focus:outline-none placeholder:text-on-surface-variant/50">
                  </div>
                </div>
                <ul class="max-h-56 overflow-y-auto py-1">
                  <li v-if="!filteredMovies.length" class="px-4 py-3 text-xs text-on-surface-variant text-center">Không tìm thấy phim phù hợp.</li>
                  <li v-for="m in filteredMovies" :key="m.id" @click="selectMovie(m)"
                      class="px-4 py-2.5 text-sm flex items-center justify-between gap-2 transition-colors"
                      :class="[
                        isArchived(m) ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer hover:bg-primary/10',
                        m.id === form.movieId ? 'bg-primary/15 text-primary font-semibold' : 'text-on-surface'
                      ]">
                    <span class="truncate">{{ m.title }}</span>
                    <span v-if="isArchived(m)" class="text-[10px] uppercase tracking-wide text-red-400 shrink-0">Ngừng chiếu</span>
                    <span v-else-if="m.status !== 'active'" class="text-[10px] uppercase tracking-wide text-on-surface-variant/60 shrink-0">{{ m.status }}</span>
                    <span v-else-if="m.id === form.movieId" class="material-symbols-outlined text-base shrink-0">check</span>
                  </li>
                </ul>
              </div>
            </div>
            <p class="text-[11px] text-on-surface-variant/70">Banner sẽ tự lấy ảnh nền, tên, mô tả, đạo diễn, diễn viên từ phim này.</p>
          </div>

          <!-- Link -->
          <div v-if="form.mode === 'IMAGE'" class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Link Điều hướng (Tuỳ chọn)</label>
            <input v-model="form.link" type="text" placeholder="/khuyen-mai/..." class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface">
          </div>

          <!-- Lịch hiển thị -->
          <div class="flex gap-4">
            <div class="space-y-2 flex-1">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngày bắt đầu</label>
              <!-- Chặn cứng: min = hôm nay (khóa ngày quá khứ) · onkeydown preventDefault (chỉ cho chọn qua lịch, không gõ tay) -->
              <input v-model="form.startDate" type="date" :min="todayStr" @change="onStartDateChange" onkeydown="event.preventDefault()" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface cursor-pointer">
              <p class="text-[10px] text-on-surface-variant/60">Để trống = bắt đầu ngay · tự tính từ 00:00:00</p>
            </div>
            <div class="space-y-2 flex-1">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngày kết thúc</label>
              <!-- Chặn cứng: min = ngày bắt đầu (khóa mọi ngày trước đó) · không gõ tay -->
              <input v-model="form.endDate" type="date" :min="form.startDate || todayStr" onkeydown="event.preventDefault()" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface cursor-pointer">
              <p class="text-[10px] text-on-surface-variant/60">Để trống = vô thời hạn · tự tính đến 23:59:59</p>
            </div>
          </div>

          <!-- Thứ tự + trạng thái -->
          <div class="flex gap-4">
             <div class="space-y-2 flex-1">
               <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thứ tự ưu tiên <span class="normal-case font-normal text-on-surface-variant/50">(1–{{ priorityMax }})</span></label>
               <!-- Chặn cứng: type=number, min=1, max=tổng banner; onkeydown chặn dấu - + . , e (chỉ nhập số nguyên dương) -->
               <input v-model="form.order" type="number" min="1" :max="priorityMax" step="1" onkeydown="if(['-','+','e','E','.',','].includes(event.key)) event.preventDefault()" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface">
             </div>

             <div class="space-y-2 flex-1 flex flex-col">
               <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Trạng thái</label>
               <button @click="form.isActive = !form.isActive" :class="form.isActive ? 'bg-primary text-on-primary' : 'bg-surface-container-highest text-on-surface-variant'" class="w-full h-full rounded-lg text-xs font-bold uppercase tracking-widest transition-all py-2.5">
                 {{ form.isActive ? 'BẬT (HIỂN THỊ)' : 'TẮT (ẨN)' }}
               </button>
             </div>
          </div>
        </div>

        <div class="px-6 py-3 bg-surface-container-lowest border-t border-outline-variant/10 flex justify-end gap-3">
          <button @click="closeModal" class="px-5 py-2.5 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded hover:bg-white/10 transition-all">Huỷ</button>
          <button @click="saveBanner" :disabled="isSaving || isUploading" class="px-5 py-2.5 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded hover:brightness-110 transition-all disabled:opacity-60">{{ isSaving ? 'Đang lưu...' : 'Lưu Banner' }}</button>
        </div>
      </div>
    </div>

    <!-- Modal xem trước trailer (tái dùng của trang khách) -->
    <TrailerModal :show="isPreviewOpen" :url="previewUrl" @close="isPreviewOpen = false" />
  </div>
</template>
