<script setup>
import { ref, onMounted } from 'vue'
import { bannerApi } from '@/api/admin/index'
import api from '@/api/axios'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()

const banners = ref([])
const isLoading = ref(false)
const isModalOpen = ref(false)
const isSaving = ref(false)
const isUploading = ref(false)
const editingId = ref(null) // null = thêm mới, có id = đang sửa
const movies = ref([])      // danh sách phim cho dropdown chế độ "Theo phim"

const blankForm = () => ({ title: '', imageUrl: '', link: '', isActive: true, order: 1, startDate: '', endDate: '', mode: 'IMAGE', movieId: null })
const form = ref(blankForm())

const fetchMovies = async () => {
  try {
    const { data } = await api.get('/movies')
    movies.value = (Array.isArray(data) ? data : (data.data ?? [])).map(m => ({ id: m.id, title: m.titleVietnamese || m.title, status: m.status }))
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
  try {
    const { data } = await bannerApi.getAll()
    const list = data.data ?? data
    // Chuẩn hoá: entity serialize field 'displayOrder' -> map sang 'order' để template dùng nhất quán
    banners.value = list.map(b => ({ ...b, order: b.displayOrder ?? 0 }))
  } catch (e) {
    console.error('Failed to load banners', e)
    toast.error(friendlyError(e, 'Không tải được danh sách banner.'))
  } finally {
    isLoading.value = false
  }
}

const openAddModal = () => {
  editingId.value = null
  form.value = { ...blankForm(), order: banners.value.length + 1 }
  isModalOpen.value = true
}

const openEditModal = (banner) => {
  editingId.value = banner.id
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

const closeModal = () => { isModalOpen.value = false }

// Upload ảnh lên Cloudinary qua /api/upload (giống màn Tin khuyến mãi / F&B)
const handleImageUpload = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  isUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
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

// Gói payload gửi backend (startDate/endDate dạng ISO LocalDateTime)
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

const saveBanner = async () => {
  if (form.value.mode === 'IMAGE' && !form.value.imageUrl?.trim()) {
    toast.warning('Vui lòng chọn hoặc nhập ảnh banner.')
    return
  }
  if (form.value.mode === 'MOVIE' && !form.value.movieId) {
    toast.warning('Vui lòng chọn phim để hiển thị.')
    return
  }
  if (form.value.startDate && form.value.endDate && form.value.startDate > form.value.endDate) {
    toast.warning('Ngày kết thúc phải sau ngày bắt đầu.')
    return
  }
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

// Lưu thứ tự khi sửa ô số (trước đây sửa xong không lưu)
const saveOrder = async (banner) => {
  try {
    await bannerApi.update(banner.id, { order: Number(banner.order) || 0 })
    toast.success('Đã lưu thứ tự.')
  } catch (e) {
    console.error('Failed to save order', e)
    toast.error(friendlyError(e, 'Không lưu được thứ tự.'))
  }
}

const deleteBanner = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xoá banner này?')) return
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

onMounted(() => { fetchBanners(); fetchMovies() })
</script>

<template>
  <div class="p-10 relative h-full flex flex-col">
    <!-- Header -->
    <header class="mb-8 text-on-surface flex justify-between items-end">
      <div>
        <h1 class="text-3xl font-extrabold tracking-tight font-headline uppercase">Quản lý Banner</h1>
        <p class="text-on-surface-variant text-sm mt-1">Tuỳ chỉnh và sắp xếp hình ảnh quảng cáo hiển thị trên trang chủ</p>
      </div>
      <button @click="openAddModal" class="px-6 py-3 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded-sm hover:brightness-110 transition-all flex items-center gap-2">
        <span class="material-symbols-outlined text-sm">add_photo_alternate</span>
        Thêm Banner Mới
      </button>
    </header>

    <!-- Banner Grid -->
    <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <div v-for="i in 4" :key="i" class="bg-surface-container-low border border-outline-variant/10 rounded-xl h-64 animate-pulse"></div>
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 flex-1 overflow-y-auto pr-2 pb-10">
      <div v-for="banner in banners" :key="banner.id" class="bg-surface-container-low border border-outline-variant/10 rounded-xl overflow-hidden flex flex-col shadow-sm hover:shadow-md transition-shadow group">

        <!-- Image Preview -->
        <div class="relative h-40 w-full bg-surface-container-highest overflow-hidden">
          <img v-if="banner.mode !== 'MOVIE' && banner.imageUrl" :src="banner.imageUrl" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" alt="Banner preview" />
          <div v-else-if="banner.mode === 'MOVIE'" class="w-full h-full flex flex-col items-center justify-center text-on-surface-variant gap-2">
            <span class="material-symbols-outlined text-4xl text-primary/40">movie</span>
            <span class="text-xs font-bold text-on-surface px-3 text-center line-clamp-2">{{ movieTitleById(banner.movieId) }}</span>
          </div>
          <div v-else class="w-full h-full flex items-center justify-center text-on-surface-variant">
            <span class="material-symbols-outlined text-4xl opacity-20">broken_image</span>
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
        </div>

        <!-- Banner Info -->
        <div class="p-5 flex flex-col gap-4 flex-1">
          <div class="space-y-1">
            <p class="text-sm font-bold text-on-surface truncate">{{ banner.title || 'Banner không tiêu đề' }}</p>
            <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mt-2">Link điều hướng</p>
            <p class="text-xs text-on-surface truncate font-mono bg-surface-container-highest px-2 py-1 rounded">{{ banner.link || 'Không có link' }}</p>
          </div>

          <div v-if="banner.startDate || banner.endDate" class="flex items-center gap-1.5 text-[11px] text-on-surface-variant">
            <span class="material-symbols-outlined text-sm">date_range</span>
            {{ toDateInput(banner.startDate) || '...' }} → {{ toDateInput(banner.endDate) || '...' }}
          </div>

          <div class="flex items-center justify-between mt-auto pt-4 border-t border-outline-variant/10">
            <div class="flex items-center gap-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thứ tự</label>
              <input v-model="banner.order" @change="saveOrder(banner)" type="number" class="w-16 bg-surface-container-high border-none text-sm rounded focus:ring-1 focus:ring-primary py-1 px-2 text-on-surface text-center">
            </div>

            <div class="flex items-center gap-2">
              <button @click="openEditModal(banner)" class="w-8 h-8 rounded-full bg-surface-container-highest hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors" title="Sửa">
                <span class="material-symbols-outlined text-sm">edit</span>
              </button>
              <button @click="toggleActive(banner)" class="w-8 h-8 rounded-full bg-surface-container-highest hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors" :title="banner.isActive ? 'Tắt' : 'Bật'">
                <span class="material-symbols-outlined text-sm">{{ banner.isActive ? 'visibility_off' : 'visibility' }}</span>
              </button>
              <button @click="deleteBanner(banner.id)" class="w-8 h-8 rounded-full bg-surface-container-highest hover:bg-red-500/20 hover:text-red-400 flex items-center justify-center text-on-surface-variant transition-colors" title="Xoá">
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
    <div v-if="isModalOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div class="bg-surface-container-low border border-outline-variant/20 rounded-xl w-full max-w-lg shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200 max-h-[90vh] flex flex-col">
        <div class="px-6 py-4 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <h2 class="font-headline font-bold uppercase tracking-tight text-on-surface">{{ editingId ? 'Sửa Banner' : 'Thêm Banner Mới' }}</h2>
          <button @click="closeModal" class="text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="p-6 space-y-5 overflow-y-auto">
          <!-- Chế độ hiển thị -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Chế độ hiển thị</label>
            <div class="grid grid-cols-2 gap-2">
              <button type="button" @click="form.mode = 'IMAGE'"
                      :class="form.mode === 'IMAGE' ? 'bg-primary/15 border-primary text-primary' : 'bg-surface-container-high border-transparent text-on-surface-variant'"
                      class="flex items-center justify-center gap-2 py-3 rounded-lg border text-xs font-bold uppercase tracking-widest transition-all">
                <span class="material-symbols-outlined text-base">image</span> Ảnh
              </button>
              <button type="button" @click="form.mode = 'MOVIE'"
                      :class="form.mode === 'MOVIE' ? 'bg-primary/15 border-primary text-primary' : 'bg-surface-container-high border-transparent text-on-surface-variant'"
                      class="flex items-center justify-center gap-2 py-3 rounded-lg border text-xs font-bold uppercase tracking-widest transition-all">
                <span class="material-symbols-outlined text-base">movie</span> Theo phim
              </button>
            </div>
            <p class="text-[11px] text-on-surface-variant/70">{{ form.mode === 'MOVIE' ? 'Tự dựng banner từ thông tin phim được chọn (ảnh nền, tên, mô tả, nút mua vé).' : 'Hiển thị đúng ảnh bạn tải lên, không phủ chữ.' }}</p>
          </div>

          <!-- Tiêu đề -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tiêu đề <span class="normal-case font-normal text-on-surface-variant/50">(nội bộ để dễ quản lý)</span></label>
            <input v-model="form.title" type="text" placeholder="VD: Ưu đãi hè rực rỡ" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
          </div>

          <!-- CHẾ ĐỘ ẢNH: upload + URL -->
          <div v-if="form.mode === 'IMAGE'" class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Hình ảnh banner</label>
            <label class="flex flex-col items-center justify-center gap-2 h-32 rounded-lg border-2 border-dashed border-outline-variant/30 hover:border-primary/50 cursor-pointer transition-colors bg-surface-container-high/40 overflow-hidden relative">
              <img v-if="form.imageUrl" :src="form.imageUrl" class="absolute inset-0 w-full h-full object-cover" />
              <div v-if="form.imageUrl" class="absolute inset-0 bg-black/40 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center text-white text-xs font-bold uppercase tracking-widest">Đổi ảnh khác</div>
              <template v-if="!form.imageUrl">
                <span class="material-symbols-outlined text-3xl text-on-surface-variant">{{ isUploading ? 'hourglass_empty' : 'cloud_upload' }}</span>
                <span class="text-xs text-on-surface-variant">{{ isUploading ? 'Đang tải lên...' : 'Bấm để tải ảnh lên' }}</span>
              </template>
              <input type="file" accept="image/*" class="hidden" @change="handleImageUpload" :disabled="isUploading" />
            </label>
            <input v-model="form.imageUrl" type="text" placeholder="hoặc dán URL ảnh: https://..." class="w-full bg-surface-container-high border-none text-xs rounded-lg focus:ring-1 focus:ring-primary py-2.5 px-4 text-on-surface font-mono">
          </div>

          <!-- CHẾ ĐỘ THEO PHIM: chọn phim -->
          <div v-else class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Chọn phim</label>
            <select v-model="form.movieId" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
              <option :value="null" disabled>-- Chọn phim hiển thị --</option>
              <option v-for="m in movies" :key="m.id" :value="m.id">{{ m.title }}{{ m.status === 'active' ? '' : ` (${m.status})` }}</option>
            </select>
            <p class="text-[11px] text-on-surface-variant/70">Banner sẽ tự lấy ảnh nền, tên, mô tả, đạo diễn, diễn viên từ phim này.</p>
          </div>

          <!-- Link -->
          <div v-if="form.mode === 'IMAGE'" class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Link Điều hướng (Tuỳ chọn)</label>
            <input v-model="form.link" type="text" placeholder="/khuyen-mai/..." class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
          </div>

          <!-- Lịch hiển thị -->
          <div class="flex gap-4">
            <div class="space-y-2 flex-1">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngày bắt đầu</label>
              <input v-model="form.startDate" type="date" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
            </div>
            <div class="space-y-2 flex-1">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ngày kết thúc</label>
              <input v-model="form.endDate" type="date" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
            </div>
          </div>

          <!-- Thứ tự + trạng thái -->
          <div class="flex gap-4">
             <div class="space-y-2 flex-1">
               <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thứ tự ưu tiên</label>
               <input v-model="form.order" type="number" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
             </div>

             <div class="space-y-2 flex-1 flex flex-col">
               <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Trạng thái</label>
               <button @click="form.isActive = !form.isActive" :class="form.isActive ? 'bg-primary text-on-primary' : 'bg-surface-container-highest text-on-surface-variant'" class="w-full h-full rounded-lg text-xs font-bold uppercase tracking-widest transition-all py-3">
                 {{ form.isActive ? 'BẬT (HIỂN THỊ)' : 'TẮT (ẨN)' }}
               </button>
             </div>
          </div>
        </div>

        <div class="px-6 py-4 bg-surface-container-lowest border-t border-outline-variant/10 flex justify-end gap-3">
          <button @click="closeModal" class="px-5 py-2.5 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded hover:bg-white/10 transition-all">Huỷ</button>
          <button @click="saveBanner" :disabled="isSaving || isUploading" class="px-5 py-2.5 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded hover:brightness-110 transition-all disabled:opacity-60">{{ isSaving ? 'Đang lưu...' : 'Lưu Banner' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
