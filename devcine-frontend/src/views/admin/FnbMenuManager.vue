<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '@/api/axios'
import { fnbApi } from '@/api/admin/index'
import { prepareImageForUpload } from '@/utils/imageUpload'
import { useAdminPerm } from '@/composables/useAdminPerm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const { can } = useAdminPerm()
const items = ref([])
const isLoading = ref(false)
const error = ref('')

const isDrawerOpen = ref(false)
const editingId = ref(null)
const isSaving = ref(false)
const isUploading = ref(false)
const form = ref({ name: '', type: 'COMBO', price: null, imageUrl: '', description: '', isActive: true })

const typeOptions = [
  { value: 'COMBO', label: 'Combo' },
  { value: 'POPCORN', label: 'Bắp rang' },
  { value: 'DRINK', label: 'Nước uống' },
  { value: 'SNACK', label: 'Đồ ăn vặt' }
]
const typeLabel = (t) => (typeOptions.find(o => o.value === t)?.label) || t || '—'

// Toast
const toast = useToastStore()

const fetchItems = async () => {
  isLoading.value = true
  error.value = ''
  try {
    const { data } = await fnbApi.getAll()
    items.value = data.data ?? data
  } catch (err) {
    error.value = friendlyError(err, 'Không thể tải thực đơn F&B.')
    toast.error(error.value)
  } finally {
    isLoading.value = false
  }
}

const openCreate = () => {
  editingId.value = null
  form.value = { name: '', type: 'COMBO', price: null, imageUrl: '', description: '', isActive: true }
  isDrawerOpen.value = true
}

const openEdit = (item) => {
  editingId.value = item.id
  form.value = {
    name: item.name || '',
    type: item.type || 'COMBO',
    price: item.price != null ? Number(item.price) : null,
    imageUrl: item.imageUrl || '',
    description: item.description || '',
    isActive: item.isActive !== false
  }
  isDrawerOpen.value = true
}

const handleUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  let prepared
  try {
    prepared = await prepareImageForUpload(file)
  } catch (err) {
    toast.error(friendlyError(err, 'Ảnh không hợp lệ.'))
    event.target.value = ''
    return
  }
  isUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', prepared)
    const { data } = await api.post('/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    form.value.imageUrl = data.url
  } catch (err) {
    toast.error(friendlyError(err, 'Tải ảnh thất bại.'))
  } finally {
    isUploading.value = false
  }
}

const handleSave = async () => {
  if (!form.value.name?.trim() || form.value.price == null) {
    toast.warning('Vui lòng nhập tên và giá.')
    return
  }
  isSaving.value = true
  try {
    const payload = {
      name: form.value.name.trim(),
      type: form.value.type,
      price: Number(form.value.price),
      imageUrl: form.value.imageUrl || null,
      description: form.value.description || null,
      isActive: form.value.isActive
    }
    if (editingId.value) {
      await fnbApi.update(editingId.value, payload)
      toast.success('Cập nhật món thành công.')
    } else {
      await fnbApi.create(payload)
      toast.success('Thêm món thành công.')
    }
    isDrawerOpen.value = false
    await fetchItems()
  } catch (err) {
    toast.error(friendlyError(err, 'Lưu thất bại.'))
  } finally {
    isSaving.value = false
  }
}

const toggleActive = async (item) => {
  try {
    await fnbApi.update(item.id, { isActive: !(item.isActive !== false) })
    await fetchItems()
  } catch (err) {
    toast.error(friendlyError(err, 'Đổi trạng thái thất bại.'))
  }
}

const deleteTarget = ref(null)
const isDeleting = ref(false)
const confirmDelete = async () => {
  if (!deleteTarget.value) return
  isDeleting.value = true
  try {
    await fnbApi.delete(deleteTarget.value.id)
    toast.success('Đã xoá món.')
    deleteTarget.value = null
    await fetchItems()
  } catch (err) {
    toast.error(friendlyError(err, 'Không thể xoá (món có thể đang nằm trong đơn). Hãy ẩn món thay vì xoá.'))
    deleteTarget.value = null
  } finally {
    isDeleting.value = false
  }
}

const formatPrice = (n) => (n != null ? Number(n).toLocaleString('vi-VN') + 'đ' : '')

onMounted(fetchItems)

</script>

<template>
  <div class="p-10 space-y-8">
    <header class="flex justify-between items-end flex-wrap gap-4">
      <div>
        <h1 class="text-4xl font-extrabold tracking-tight font-headline uppercase italic text-primary">Thực đơn F&B / Combo</h1>
        <p class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold">Combo bắp nước & đồ ăn khách chọn khi đặt vé</p>
      </div>
      <button v-if="can('fnb_menu', 'add')" @click="openCreate" class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
        <span class="material-symbols-outlined text-sm">add</span> Thêm món / combo
      </button>
    </header>

    <!-- Loading -->
    <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="i in 6" :key="i" class="h-40 bg-surface-container-low rounded-2xl animate-pulse border border-white/5"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="p-6 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm flex items-center gap-2">
      <span class="material-symbols-outlined">error</span> {{ error }}
    </div>

    <!-- Empty -->
    <div v-else-if="items.length === 0" class="py-24 text-center border border-dashed border-white/10 rounded-2xl">
      <span class="material-symbols-outlined text-5xl text-neutral-600 mb-4">fastfood</span>
      <p class="text-neutral-400 font-semibold">Chưa có món nào. Bấm "Thêm món / combo".</p>
    </div>

    <!-- Grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="item in items" :key="item.id" class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden shadow-xl flex flex-col">
        <div class="h-36 bg-surface-container-high relative overflow-hidden">
          <img v-if="item.imageUrl" :src="item.imageUrl" class="w-full h-full object-cover" />
          <div v-else class="w-full h-full flex items-center justify-center text-on-surface-variant/40">
            <span class="material-symbols-outlined text-5xl">fastfood</span>
          </div>
          <span :class="item.isActive !== false ? 'bg-green-500/80 text-white' : 'bg-neutral-600/80 text-white'" class="absolute top-3 left-3 text-[9px] font-bold uppercase tracking-widest px-2 py-1 rounded">
            {{ item.isActive !== false ? 'Đang bán' : 'Đang ẩn' }}
          </span>
          <span class="absolute top-3 right-3 text-[9px] font-bold uppercase tracking-widest px-2 py-1 rounded bg-black/50 text-primary">{{ typeLabel(item.type) }}</span>
        </div>
        <div class="p-5 flex flex-col flex-grow">
          <div class="flex justify-between items-start gap-2 mb-1">
            <h3 class="text-base font-black text-on-surface uppercase italic">{{ item.name }}</h3>
            <span class="text-sm font-black text-primary whitespace-nowrap">{{ formatPrice(item.price) }}</span>
          </div>
          <p class="text-xs text-on-surface-variant line-clamp-2 flex-grow">{{ item.description || 'Chưa có mô tả' }}</p>
          <div class="flex justify-end items-center gap-2 pt-4 mt-3 border-t border-outline-variant/5">
            <button v-if="can('fnb_menu', 'edit')" @click="toggleActive(item)" class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant hover:text-primary px-2 py-1 transition-colors">
              {{ item.isActive !== false ? 'Ẩn' : 'Hiện' }}
            </button>
            <button v-if="can('fnb_menu', 'edit')" @click="openEdit(item)" class="w-8 h-8 rounded-full hover:bg-white/10 text-on-surface-variant hover:text-primary flex items-center justify-center transition-colors">
              <span class="material-symbols-outlined text-sm">edit</span>
            </button>
            <button v-if="can('fnb_menu', 'delete')" @click="deleteTarget = item" class="w-8 h-8 rounded-full hover:bg-white/10 text-on-surface-variant hover:text-red-400 flex items-center justify-center transition-colors">
              <span class="material-symbols-outlined text-sm">delete</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Drawer Form -->
    <div v-if="isDrawerOpen" class="fixed inset-0 z-[1000] flex justify-end">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isDrawerOpen = false"></div>
      <div class="relative w-full max-w-md bg-surface-container-low h-full shadow-2xl flex flex-col border-l border-outline-variant/20">
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <h3 class="font-headline font-black uppercase italic text-primary text-xl">{{ editingId ? 'Sửa món / combo' : 'Thêm món / combo' }}</h3>
          <button @click="isDrawerOpen = false" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-6 space-y-5">
          <!-- Ảnh -->
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Hình ảnh</label>
            <div class="w-full h-36 bg-surface-container-highest border-2 border-dashed border-outline-variant/20 rounded-2xl flex items-center justify-center overflow-hidden relative">
              <img v-if="form.imageUrl" :src="form.imageUrl" class="w-full h-full object-cover" />
              <div v-else class="text-on-surface-variant/50 flex flex-col items-center">
                <span class="material-symbols-outlined text-3xl mb-1">cloud_upload</span>
                <span class="text-[10px] font-bold uppercase tracking-widest">{{ isUploading ? 'Đang tải...' : 'Tải ảnh lên' }}</span>
              </div>
              <input type="file" accept="image/*" @change="handleUpload" class="absolute inset-0 opacity-0 cursor-pointer" :disabled="isUploading" />
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tên món / combo</label>
            <input v-model="form.name" class="w-full bg-surface-container-highest border border-outline-variant/20 p-3 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" placeholder="VD: Combo Couple" />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Loại</label>
              <select v-model="form.type" class="w-full bg-surface-container-highest border border-outline-variant/20 p-3 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none">
                <option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
            <div class="space-y-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Giá (VNĐ)</label>
              <input v-model="form.price" type="number" min="0" class="w-full bg-surface-container-highest border border-outline-variant/20 p-3 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" placeholder="VD: 89000" />
            </div>
          </div>

          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Mô tả</label>
            <textarea v-model="form.description" rows="3" class="w-full bg-surface-container-highest border border-outline-variant/20 p-3 rounded-xl text-sm text-on-surface focus:border-primary outline-none resize-none" placeholder="VD: 1 bắp lớn + 2 nước ngọt"></textarea>
          </div>

          <div class="flex items-center justify-between p-4 bg-surface-container-highest rounded-xl border border-outline-variant/10">
            <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface">Đang bán (hiện cho khách)</p>
            <button @click="form.isActive = !form.isActive" :class="form.isActive ? 'bg-green-500' : 'bg-surface-container-high'" class="relative w-10 h-5 rounded-full transition-colors shrink-0">
              <span :class="form.isActive ? 'translate-x-5 bg-white' : 'translate-x-0 bg-on-surface-variant'" class="inline-block w-4 h-4 transform rounded-full transition-transform shadow-md absolute top-0.5 left-0.5"></span>
            </button>
          </div>
        </div>

        <div class="p-6 border-t border-outline-variant/10 bg-surface-container-lowest flex gap-4">
          <button @click="isDrawerOpen = false" class="flex-1 px-6 py-3 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy</button>
          <button @click="handleSave" :disabled="isSaving" class="flex-1 px-6 py-3 rounded-xl bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform disabled:opacity-60">{{ isSaving ? 'Đang lưu...' : 'Lưu' }}</button>
        </div>
      </div>
    </div>

    <!-- Delete confirm -->
    <div v-if="deleteTarget" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="deleteTarget = null"></div>
      <div class="relative w-full max-w-sm bg-surface-container-low border border-white/10 rounded-2xl p-6 shadow-2xl text-center">
        <span class="material-symbols-outlined text-4xl text-red-400 mb-3">warning</span>
        <h3 class="text-lg font-bold font-headline text-white mb-2">Xoá món?</h3>
        <p class="text-sm text-on-surface-variant">Xoá "<span class="font-bold text-primary">{{ deleteTarget.name }}</span>"? Nếu món đã có trong đơn cũ, hãy dùng "Ẩn" thay vì xoá.</p>
        <div class="flex gap-3 mt-6">
          <button @click="deleteTarget = null" class="flex-1 px-4 py-3 rounded-xl border border-white/15 text-xs font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Huỷ</button>
          <button @click="confirmDelete" :disabled="isDeleting" class="flex-1 px-4 py-3 rounded-xl bg-red-500 text-white text-xs font-bold uppercase tracking-widest hover:brightness-110 transition-all disabled:opacity-60">{{ isDeleting ? 'Đang xoá...' : 'Xoá' }}</button>
        </div>
      </div>
    </div>

    <!-- Toast -->
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>
