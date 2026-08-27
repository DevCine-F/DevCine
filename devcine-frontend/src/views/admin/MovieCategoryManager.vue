<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import api from '@/api/axios'
import AppButton from '../../components/common/AppButton.vue'
import AppModal from '../../components/common/AppModal.vue'
import { useConfirmStore } from '@/stores/confirm'
import { useAdminPerm } from '@/composables/useAdminPerm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const { can } = useAdminPerm()

const confirm = useConfirmStore()

const genres = ref([])
const formats = ref([])
const ageRatings = ref([])

const activeTab = ref('genres') // 'genres' | 'formats' | 'age-ratings'

const isLoading = ref(false)
const loadError = ref('')
const isSaving = ref(false)
const isModalOpen = ref(false)
const editingItem = ref(null)
const newItem = ref({ name: '', code: '', description: '' })
// Chỉ tô đỏ ô sau khi admin đã gõ / rời ô — form vừa mở không "quát" lỗi ngay.
const touched = ref({ name: false, code: false })

const toast = useToastStore()

// Danh sách đang hiển thị theo tab (luôn sắp xếp ID tăng dần)
const currentList = computed(() => {
  const list = activeTab.value === 'genres' ? genres.value
    : activeTab.value === 'formats' ? formats.value
      : ageRatings.value
  return [...(list || [])].sort((a, b) => Number(a.id || 0) - Number(b.id || 0))
})

const tabLabel = computed(() =>
  activeTab.value === 'genres' ? 'thể loại'
    : activeTab.value === 'formats' ? 'định dạng'
      : 'mục kiểm duyệt'
)

// ===== Validate tên danh mục (scoped theo tab) =====
// Regex ký tự cấm XSS và nguy hiểm chung
const FORBIDDEN_COMMON = /[@#$%^*<>/[\]{}\\]/g
const FORBIDDEN_GENRE_FORMAT = /[@#$%^&*<>/,[\]{}\\]/g

const nameRules = computed(() => {
  if (activeTab.value === 'formats') {
    return { min: 2, max: 50, blockDigits: false, forbidden: FORBIDDEN_GENRE_FORMAT, capitalize: false }
  }
  if (activeTab.value === 'age-ratings') {
    // Tab Kiểm duyệt: cho phép số, dấu ngoặc ( ), dấu gạch nối -, dấu + cho mô tả độ tuổi (VD: Từ 13 tuổi, Dưới 13 tuổi (có người lớn đi kèm))
    return { min: 2, max: 50, blockDigits: false, forbidden: FORBIDDEN_COMMON, capitalize: true }
  }
  // Tab Thể loại: chặn chữ số, chặn ký tự đặc biệt
  return { min: 2, max: 50, blockDigits: true, forbidden: FORBIDDEN_GENRE_FORMAT, capitalize: true }
})

const capitalizeFirst = (s) => { const t = (s || '').trim(); return t ? t.charAt(0).toUpperCase() + t.slice(1) : t }

const formatMovieFormatName = (raw) => {
  if (!raw) return ''
  return raw
    .trim()
    .split(/\s+/)
    .map(w => {
      const lower = w.toLowerCase()
      if (['2d', '3d', '4d', '4dx', 'imax'].includes(lower)) {
        return w.toUpperCase()
      }
      return w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()
    })
    .join(' ')
}

// Chặn cứng khi gõ: bỏ ký tự cấm (+ số cho tab thể loại), giới hạn độ dài tối đa.
const onNameInput = (e) => {
  const r = nameRules.value
  let v = e.target.value.replace(r.forbidden, '')
  if (r.blockDigits) v = v.replace(/[0-9]/g, '')
  v = v.slice(0, r.max)
  newItem.value.name = v
  e.target.value = v
  touched.value.name = true
}

// Rời ô: cắt khoảng trắng thừa 2 đầu & gom nhiều khoảng trắng liên tiếp (\s+) thành 1 dấu cách đơn.
const onNameBlur = () => {
  touched.value.name = true
  const collapsed = (newItem.value.name || '').trim().replace(/\s+/g, ' ')
  if (activeTab.value === 'formats') {
    newItem.value.name = formatMovieFormatName(collapsed)
  } else if (nameRules.value.capitalize) {
    newItem.value.name = capitalizeFirst(collapsed)
  } else {
    newItem.value.name = collapsed
  }
}

const nameError = computed(() => {
  const r = nameRules.value
  const t = (newItem.value.name || '').trim().replace(/\s+/g, ' ')
  if (t.length === 0) return 'Tên danh mục không được để trống'
  if (t.length < r.min || t.length > r.max) return `Tên danh mục phải từ ${r.min} đến ${r.max} ký tự`
  // Trùng lặp (không phân biệt hoa/thường) trong tab hiện tại, loại trừ chính nó khi sửa.
  const dup = currentList.value.some((it) =>
    it.name && it.name.trim().toLowerCase() === t.toLowerCase()
    && (!editingItem.value || it.id !== editingItem.value.id))
  if (dup) return 'Tên danh mục này đã tồn tại'
  return ''
})

// Mã kiểm duyệt: chỉ chữ HOA + số, 1-10 ký tự. Tự động toUpperCase() khi gõ.
const CODE_MAX = 10
const onCodeInput = (e) => {
  const v = e.target.value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, CODE_MAX)
  newItem.value.code = v
  e.target.value = v
  touched.value.code = true
}

const codeError = computed(() => {
  if (activeTab.value !== 'age-ratings') return ''
  const c = (newItem.value.code || '').trim()
  if (!c) return 'Mã kiểm duyệt không được để trống'
  if (c.length > CODE_MAX) return 'Mã kiểm duyệt không vượt quá 10 ký tự'
  if (!/^[A-Z0-9]+$/.test(c)) return 'Mã chỉ được chứa chữ cái và số không dấu (VD: P, T13)'
  const dup = ageRatings.value.some((it) =>
    it.code && it.code.trim().toLowerCase() === c.toLowerCase()
    && (!editingItem.value || it.id !== editingItem.value.id))
  if (dup) return 'Mã kiểm duyệt này đã tồn tại trên hệ thống'
  return ''
})

const descError = computed(() => ((newItem.value.description || '').length > 150 ? 'Mô tả không được vượt quá 150 ký tự' : ''))

// Nút Lưu chỉ sáng khi mọi ràng buộc hợp lệ.
const isSaveDisabled = computed(() => isSaving.value || !!nameError.value || !!codeError.value || !!descError.value)

// ===== Phân trang (client-side, dùng chung 3 tab) =====
const PAGE_SIZE = 8
const currentPage = ref(1)
const totalPages = computed(() => Math.max(1, Math.ceil(currentList.value.length / PAGE_SIZE)))
const pagedList = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return currentList.value.slice(start, start + PAGE_SIZE)
})
// Đổi tab -> về trang 1
watch(activeTab, () => { currentPage.value = 1 })
// Khi danh sách co lại (xoá / lọc) mà trang hiện tại vượt quá -> kẹp lại
watch(totalPages, (tp) => { if (currentPage.value > tp) currentPage.value = tp })
const goToPage = (p) => { if (p >= 1 && p <= totalPages.value) currentPage.value = p }

const fetchData = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const [genresRes, formatsRes, ageRatingsRes] = await Promise.all([
      api.get('/categories/genres'),
      api.get('/categories/formats'),
      api.get('/categories/age-ratings')
    ])
    const sortByAscId = (arr) => [...(arr || [])].sort((a, b) => Number(a.id || 0) - Number(b.id || 0))
    genres.value = sortByAscId(genresRes.data)
    formats.value = sortByAscId(formatsRes.data)
    ageRatings.value = sortByAscId(ageRatingsRes.data)
  } catch (error) {
    loadError.value = friendlyError(error, 'Không thể tải danh mục. Vui lòng thử lại.')
    toast.error(loadError.value)
  } finally {
    isLoading.value = false
  }
}

// Reset form về trạng thái sạch sẽ ban đầu
const resetForm = () => {
  newItem.value = { name: '', code: '', description: '' }
  touched.value = { name: false, code: false }
  editingItem.value = null
}

// Kiểm tra dữ liệu form có thay đổi so với ban đầu (Unsaved changes)
const isDirty = () => {
  const curName = (newItem.value.name || '').trim()
  const curCode = (newItem.value.code || '').trim()
  const curDesc = (newItem.value.description || '').trim()
  if (!editingItem.value) {
    return curName !== '' || curCode !== '' || curDesc !== ''
  }
  const origName = (editingItem.value.name || '').trim()
  const origCode = (editingItem.value.code || '').trim()
  const origDesc = (editingItem.value.description || '').trim()
  return curName !== origName || curCode !== origCode || curDesc !== origDesc
}

// Xử lý đóng modal: Nếu có thay đổi chưa lưu -> hiện popup confirm
const handleCloseModal = async () => {
  if (isDirty()) {
    const ok = await confirm.show({
      title: 'Xác nhận thoát',
      message: 'Dữ liệu chưa được lưu, bạn có chắc chắn muốn thoát?',
      confirmText: 'Thoát',
      cancelText: 'Ở lại',
      tone: 'danger'
    })
    if (!ok) return
  }
  resetForm()
  isModalOpen.value = false
}

// Xử lý phím tắt ESC toàn cục để đóng modal với popup confirm
const onGlobalKeyDown = (e) => {
  if (e.key === 'Escape' && isModalOpen.value && !confirm.open) {
    handleCloseModal()
  }
}

const openModal = (item = null) => {
  editingItem.value = item
  touched.value = { name: false, code: false }
  if (item) {
    newItem.value = { name: item.name || '', code: item.code || '', description: item.description || '' }
  } else {
    newItem.value = { name: '', code: '', description: '' }
  }
  isModalOpen.value = true
}

const saveItem = async () => {
  // Validate phía client trước khi gọi API (chặn cứng theo nameError/codeError/descError).
  touched.value = { name: true, code: true }
  const firstErr = codeError.value || nameError.value || descError.value
  if (firstErr) { toast.warning(firstErr); return }

  const collapsed = (newItem.value.name || '').trim().replace(/\s+/g, ' ')
  const finalName = activeTab.value === 'formats'
    ? formatMovieFormatName(collapsed)
    : (nameRules.value.capitalize ? capitalizeFirst(collapsed) : collapsed)
  const finalDesc = (newItem.value.description || '').trim().replace(/\s+/g, ' ') || null
  const payload = activeTab.value === 'age-ratings'
    ? { code: newItem.value.code.trim().toUpperCase(), name: finalName, description: finalDesc }
    : { name: finalName, description: finalDesc }

  isSaving.value = true
  try {
    if (editingItem.value) {
      await api.put(`/categories/${activeTab.value}/${editingItem.value.id}`, payload)
      toast.success('Đã cập nhật ' + tabLabel.value)
    } else {
      await api.post(`/categories/${activeTab.value}`, payload)
      toast.success('Đã thêm ' + tabLabel.value + ' mới')
    }
    await fetchData()
    resetForm()
    isModalOpen.value = false
  } catch (error) {
    toast.error(friendlyError(error, 'Lưu thất bại. Vui lòng thử lại.'))
  } finally {
    isSaving.value = false
  }
}

const deleteItem = async (item) => {
  const ok = await confirm.show({
    title: 'Xoá mục',
    message: `Bạn có chắc muốn xoá ${tabLabel.value} "${item.code || item.name}"?`,
    confirmText: 'Xoá',
    tone: 'danger',
  })
  if (!ok) return
  try {
    await api.delete(`/categories/${activeTab.value}/${item.id}`)
    toast.success('Đã xóa ' + tabLabel.value)
    await fetchData()
  } catch (error) {
    toast.error(friendlyError(error, 'Xóa thất bại. Vui lòng thử lại.'))
  }
}

onMounted(() => {
  fetchData()
  window.addEventListener('keydown', onGlobalKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onGlobalKeyDown)
})
</script>

<template>
  <div class="p-10">
    <header class="flex justify-between items-center mb-10">
      <div>
        <h1 class="text-3xl font-black tracking-tighter text-on-surface uppercase italic">
          Quản lý <span class="text-primary">Danh mục Phim</span>
        </h1>
        <p class="text-on-surface-variant text-xs mt-1 font-bold uppercase tracking-widest">
          Thể loại, Định dạng & Kiểm duyệt
        </p>
      </div>
      <div class="flex gap-4">
        <button
          v-if="can('movies', 'add')"
          @click="openModal()"
          class="bg-primary text-on-primary font-headline font-bold text-xs uppercase tracking-widest px-8 py-3 rounded-sm hover:brightness-110 active:scale-95 transition-all flex items-center gap-2"
        >
          <span class="material-symbols-outlined text-sm">add</span>
          Thêm mới
        </button>
      </div>
    </header>

    <!-- Tabs -->
    <div class="flex gap-4 mb-8 bg-surface-container-low p-1 rounded-xl w-fit">
      <button
        v-for="tab in [{id: 'genres', label: 'Thể loại'}, {id: 'formats', label: 'Định dạng'}, {id: 'age-ratings', label: 'Kiểm duyệt'}]"
        :key="tab.id"
        @click="activeTab = tab.id"
        :class="activeTab === tab.id ? 'bg-primary text-on-primary shadow-lg' : 'text-on-surface-variant hover:bg-white/5'"
        class="px-8 py-3 rounded-lg text-xs font-black font-headline uppercase tracking-widest transition-all"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Content Table -->
    <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden shadow-2xl">
      <table class="w-full text-left">
        <thead>
          <tr class="bg-surface-container-high/30 border-b border-outline-variant/10">
            <th class="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-on-surface-variant">ID</th>
            <th class="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-on-surface-variant">
              {{ activeTab === 'age-ratings' ? 'Mã / Tên' : 'Tên danh mục' }}
            </th>
            <th class="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Mô tả</th>
            <th class="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-on-surface-variant text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-outline-variant/10">
          <!-- Loading -->
          <tr v-if="isLoading">
            <td colspan="4" class="px-8 py-16 text-center">
              <span class="animate-pulse text-xs font-bold uppercase tracking-widest text-primary">Đang tải dữ liệu...</span>
            </td>
          </tr>
          <!-- Error -->
          <tr v-else-if="loadError">
            <td colspan="4" class="px-8 py-16 text-center">
              <div class="flex flex-col items-center gap-4">
                <span class="material-symbols-outlined text-4xl text-red-500/70">error</span>
                <p class="text-xs font-bold text-on-surface-variant max-w-sm">{{ loadError }}</p>
                <button @click="fetchData" class="text-[10px] font-black uppercase tracking-widest text-primary hover:underline">
                  Thử lại
                </button>
              </div>
            </td>
          </tr>
          <!-- Empty -->
          <tr v-else-if="currentList.length === 0">
            <td colspan="4" class="px-8 py-16 text-center">
              <div class="flex flex-col items-center gap-3">
                <span class="material-symbols-outlined text-4xl text-on-surface-variant/30">category</span>
                <p class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Chưa có {{ tabLabel }} nào</p>
                <button v-if="can('movies', 'add')" @click="openModal()" class="text-[10px] font-black uppercase tracking-widest text-primary hover:underline">
                  Thêm mới ngay
                </button>
              </div>
            </td>
          </tr>
          <!-- Success / data rows -->
          <tr v-else v-for="item in pagedList"
              :key="item.id" class="group hover:bg-white/5 transition-colors">
            <td class="px-8 py-4 text-xs font-mono text-on-surface-variant">#{{ item.id }}</td>
            <td class="px-8 py-4">
              <div v-if="activeTab === 'age-ratings'" class="flex items-center gap-3">
                <span class="px-3 py-1 bg-primary/10 text-primary border border-primary/20 rounded text-[10px] font-black">{{ item.code }}</span>
                <span class="text-sm font-bold">{{ item.name }}</span>
              </div>
              <span v-else class="text-sm font-bold tracking-tight text-on-surface group-hover:text-primary transition-colors">
                {{ item.name }}
              </span>
            </td>
            <td class="px-8 py-4 text-xs text-on-surface-variant max-w-md truncate">{{ item.description || 'Chưa có mô tả' }}</td>
            <td class="px-8 py-4 text-right">
              <div class="flex justify-end gap-2">
                <button v-if="can('movies', 'edit')" @click="openModal(item)" class="p-2 hover:text-primary transition-colors">
                  <span class="material-symbols-outlined text-lg">edit</span>
                </button>
                <button v-if="can('movies', 'delete')" @click="deleteItem(item)" class="p-2 hover:text-red-500 transition-colors">
                  <span class="material-symbols-outlined text-lg">delete</span>
                </button>
                <span v-if="!can('movies','edit') && !can('movies','delete')" class="text-on-surface-variant/40 text-xs">—</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Phân trang -->
      <div v-if="!isLoading && !loadError && currentList.length > 0"
           class="flex items-center justify-between gap-4 px-8 py-4 border-t border-outline-variant/10 bg-surface-container-high/20">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
          {{ (currentPage - 1) * PAGE_SIZE + 1 }}–{{ Math.min(currentPage * PAGE_SIZE, currentList.length) }}
          / {{ currentList.length }} {{ tabLabel }}
        </p>
        <div v-if="totalPages > 1" class="flex items-center gap-1.5">
          <button @click="goToPage(currentPage - 1)" :disabled="currentPage === 1"
                  class="w-8 h-8 flex items-center justify-center rounded-lg text-on-surface-variant hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
            <span class="material-symbols-outlined text-lg">chevron_left</span>
          </button>
          <button v-for="p in totalPages" :key="p" @click="goToPage(p)"
                  :class="p === currentPage ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:bg-white/10'"
                  class="min-w-8 h-8 px-2 flex items-center justify-center rounded-lg text-xs font-bold transition-colors">
            {{ p }}
          </button>
          <button @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages"
                  class="w-8 h-8 flex items-center justify-center rounded-lg text-on-surface-variant hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition-colors">
            <span class="material-symbols-outlined text-lg">chevron_right</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <AppModal :show="isModalOpen" @close="handleCloseModal" :title="editingItem ? 'Sửa danh mục' : 'Thêm danh mục mới'">
      <div class="space-y-6 pt-4">
        <div v-if="activeTab === 'age-ratings'" class="space-y-2">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Mã kiểm duyệt (Code) <span class="text-red-500">*</span></label>
          <input :value="newItem.code" @input="onCodeInput" @blur="touched.code = true" @keyup.enter="saveItem" :maxlength="CODE_MAX"
                 placeholder="VD: P, T13, T16..."
                 :class="touched.code && codeError ? 'ring-1 ring-red-500' : 'focus:ring-1 focus:ring-primary'"
                 class="w-full bg-surface-container-high border-none rounded-lg py-3 px-4 text-on-surface outline-none text-sm font-mono tracking-widest uppercase" />
          <p v-if="touched.code && codeError" class="text-[11px] text-red-400 font-bold">{{ codeError }}</p>
        </div>
        <div class="space-y-2">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tên danh mục <span class="text-red-500">*</span></label>
          <input :value="newItem.name" @input="onNameInput" @blur="onNameBlur" @keyup.enter="saveItem" :maxlength="nameRules.max" placeholder="Nhập tên..."
                 :class="touched.name && nameError ? 'ring-1 ring-red-500' : 'focus:ring-1 focus:ring-primary'"
                 class="w-full bg-surface-container-high border-none rounded-lg py-3 px-4 text-on-surface outline-none text-sm" />
          <p v-if="touched.name && nameError" class="text-[11px] text-red-400 font-bold">{{ nameError }}</p>
        </div>
        <div class="space-y-2">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Mô tả</label>
          <textarea v-model="newItem.description" rows="3" maxlength="150" placeholder="Ghi chú thêm..."
                    class="w-full bg-surface-container-high border-none rounded-lg py-3 px-4 text-on-surface focus:ring-1 focus:ring-primary outline-none text-sm resize-none"></textarea>
          <div class="flex justify-end">
            <span class="text-[10px] text-on-surface-variant/60">{{ (newItem.description || '').length }}/150</span>
          </div>
        </div>
        <div class="flex gap-4 pt-4">
          <AppButton variant="ghost" class="flex-1" @click="handleCloseModal" :disabled="isSaving">Hủy</AppButton>
          <AppButton class="flex-1" @click="saveItem" :disabled="isSaveDisabled">
            {{ isSaving ? (editingItem ? 'Đang lưu...' : 'Đang thêm...') : (editingItem ? 'Lưu thay đổi' : 'Thêm mới') }}
          </AppButton>
        </div>
      </div>
    </AppModal>

  </div>
</template>
