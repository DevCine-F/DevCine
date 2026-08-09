<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '@/api/axios'
import { fnbApi, fnbGroupApi } from '@/api/admin/index'
import { prepareImageForUpload } from '@/utils/imageUpload'
import { useAdminPerm } from '@/composables/useAdminPerm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const { can } = useAdminPerm()
const items = ref([])
const isLoading = ref(false)
const error = ref('')

const activeTab = ref('items')

const isDrawerOpen = ref(false)
const editingId = ref(null)
const isSaving = ref(false)
const isUploading = ref(false)
const form = ref({ name: '', type: 'COMBO', price: null, imageUrl: '', description: '', isActive: true, slots: [] })
const optionGroups = ref([])

const isGroupDrawerOpen = ref(false)
const editingGroupId = ref(null)
// Kho Tùy Chọn (Pool) nay thuần túy: chỉ tên + danh sách vị. Ràng buộc min/max/required
// đã chuyển xuống Slot của từng combo.
const groupForm = ref({ name: '', items: [] })

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

const fetchOptionGroups = async () => {
  try {
    const { data } = await api.get('/fnbs/groups')
    optionGroups.value = data.data ?? data
  } catch (err) {
    console.error('Failed to fetch option groups', err)
  }
}

const openCreate = () => {
  editingId.value = null
  form.value = { name: '', type: 'COMBO', price: null, imageUrl: '', description: '', isActive: true, slots: [] }
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
    isActive: item.isActive !== false,
    slots: (item.slots || []).map(s => ({
      slotLabel: s.slotLabel || '',
      optionGroupId: s.optionGroup?.id ?? null,
      defaultOptionItemId: s.defaultOptionItem?.id ?? null,
      minChoices: s.minChoices ?? 1,
      maxChoices: s.maxChoices ?? 1,
      isRequired: s.isRequired ?? true
    }))
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
      isActive: form.value.isActive,
      slots: form.value.slots
        .map((s, i) => ({
          slotLabel: (s.slotLabel || '').trim(),
          optionGroupId: s.optionGroupId,
          defaultOptionItemId: s.defaultOptionItemId || null,
          displayOrder: i,
          minChoices: Number(s.minChoices) || 0,
          maxChoices: Number(s.maxChoices) || 1,
          isRequired: !!s.isRequired
        }))
        .filter(s => s.optionGroupId)
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

const openGroupCreate = () => {
  editingGroupId.value = null
  groupForm.value = { name: '', items: [] }
  isGroupDrawerOpen.value = true
}

const openGroupEdit = (group) => {
  editingGroupId.value = group.id
  groupForm.value = {
    name: group.name || '',
    items: (group.items || []).map(i => ({ ...i }))
  }
  isGroupDrawerOpen.value = true
}

const handleGroupSave = async () => {
  if (!groupForm.value.name?.trim()) {
    toast.warning('Vui lòng nhập tên nhóm.')
    return
  }
  isSaving.value = true
  try {
    const payload = {
      name: groupForm.value.name.trim(),
      items: groupForm.value.items.map(i => ({
        id: i.id,
        name: i.name.trim(),
        surchargePrice: Number(i.surchargePrice) || 0
      })).filter(i => i.name)
    }
    if (editingGroupId.value) {
      await fnbGroupApi.update(editingGroupId.value, payload)
      toast.success('Cập nhật nhóm thành công.')
    } else {
      await fnbGroupApi.create(payload)
      toast.success('Thêm nhóm thành công.')
    }
    isGroupDrawerOpen.value = false
    await fetchOptionGroups()
  } catch (err) {
    toast.error(friendlyError(err, 'Lưu thất bại.'))
  } finally {
    isSaving.value = false
  }
}

const deleteGroupTarget = ref(null)
const confirmDeleteGroup = async () => {
  if (!deleteGroupTarget.value) return
  isDeleting.value = true
  try {
    await fnbGroupApi.delete(deleteGroupTarget.value.id)
    toast.success('Đã xoá nhóm.')
    deleteGroupTarget.value = null
    await fetchOptionGroups()
  } catch (err) {
    toast.error(friendlyError(err, 'Không thể xoá nhóm.'))
    deleteGroupTarget.value = null
  } finally {
    isDeleting.value = false
  }
}

const addGroupItem = () => {
  groupForm.value.items.push({ id: null, name: '', surchargePrice: 0 })
}
const removeGroupItem = (index) => {
  groupForm.value.items.splice(index, 1)
}

// ── Cấu hình Ô chọn (Slot) khi tạo/sửa Combo ──
const addSlot = () => {
  form.value.slots.push({
    slotLabel: '',
    optionGroupId: optionGroups.value[0]?.id ?? null,
    minChoices: 1,
    maxChoices: 1,
    isRequired: true
  })
}
const removeSlot = (index) => {
  form.value.slots.splice(index, 1)
}

const formatPrice = (n) => (n != null ? Number(n).toLocaleString('vi-VN') + 'đ' : '')

onMounted(() => {
  fetchItems()
  fetchOptionGroups()
})

</script>

<template>
  <div class="p-10 space-y-8">
    <header class="flex justify-between items-end flex-wrap gap-4">
      <div>
        <h1 class="text-4xl font-extrabold tracking-tight font-headline uppercase italic text-primary">Thực đơn F&B / Combo</h1>
        <p class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold">Combo bắp nước & đồ ăn khách chọn khi đặt vé</p>
      </div>
      <button v-if="can('fnb_menu', 'add')" @click="activeTab === 'items' ? openCreate() : openGroupCreate()" class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
        <span class="material-symbols-outlined text-sm">add</span> {{ activeTab === 'items' ? 'Thêm món / combo' : 'Thêm kho tùy chọn' }}
      </button>
    </header>

    <!-- Sub-tabs -->
    <div class="flex bg-surface-container-high p-1 rounded-xl w-fit">
      <button 
        @click="activeTab = 'items'" 
        :class="activeTab === 'items' ? 'bg-primary text-on-primary shadow-md' : 'text-on-surface-variant hover:text-on-surface hover:bg-white/5'"
        class="px-6 py-2 rounded-lg font-bold text-sm uppercase tracking-widest transition-all"
      >
        Thực đơn & Combo
      </button>
      <button 
        @click="activeTab = 'groups'" 
        :class="activeTab === 'groups' ? 'bg-primary text-on-primary shadow-md' : 'text-on-surface-variant hover:text-on-surface hover:bg-white/5'"
        class="px-6 py-2 rounded-lg font-bold text-sm uppercase tracking-widest transition-all"
      >
        Kho Tùy Chọn (Pools)
      </button>
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="i in 6" :key="i" class="h-40 bg-surface-container-low rounded-2xl animate-pulse border border-white/5"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="p-6 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm flex items-center gap-2">
      <span class="material-symbols-outlined">error</span> {{ error }}
    </div>

    <template v-else-if="activeTab === 'items'">
      <!-- Empty -->
      <div v-if="items.length === 0" class="py-24 text-center border border-dashed border-white/10 rounded-2xl">
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
    </template>
    
    <template v-else-if="activeTab === 'groups'">
      <div v-if="optionGroups.length === 0" class="py-24 text-center border border-dashed border-white/10 rounded-2xl">
        <span class="material-symbols-outlined text-5xl text-neutral-600 mb-4">list_alt</span>
        <p class="text-neutral-400 font-semibold">Chưa có kho tùy chọn nào.</p>
      </div>
      
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div v-for="group in optionGroups" :key="group.id" class="bg-surface-container-low border border-outline-variant/10 rounded-2xl p-5 shadow-xl flex flex-col space-y-4">
          <div class="flex justify-between items-start">
            <div>
              <h3 class="text-lg font-black text-primary uppercase">{{ group.name }}</h3>
              <p class="text-xs text-on-surface-variant mt-1 font-bold">
                {{ (group.items || []).length }} lựa chọn · Kho dùng chung
              </p>
            </div>
            <div class="flex gap-2">
              <button v-if="can('fnb_menu', 'edit')" @click="openGroupEdit(group)" class="w-8 h-8 rounded-full hover:bg-white/10 text-on-surface-variant hover:text-primary flex items-center justify-center transition-colors">
                <span class="material-symbols-outlined text-sm">edit</span>
              </button>
              <button v-if="can('fnb_menu', 'delete')" @click="deleteGroupTarget = group" class="w-8 h-8 rounded-full hover:bg-white/10 text-on-surface-variant hover:text-red-400 flex items-center justify-center transition-colors">
                <span class="material-symbols-outlined text-sm">delete</span>
              </button>
            </div>
          </div>
          
          <div class="flex-grow space-y-2">
            <div v-for="item in group.items" :key="item.id" class="flex justify-between items-center text-sm p-2 bg-surface-container-highest rounded-lg border border-outline-variant/5">
              <span class="text-on-surface font-semibold">{{ item.name }}</span>
              <span v-if="item.surchargePrice > 0" class="text-primary font-bold">+{{ formatPrice(item.surchargePrice) }}</span>
              <span v-else class="text-on-surface-variant text-xs">Miễn phí</span>
            </div>
          </div>
        </div>
      </div>
    </template>

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

          <!-- Cấu hình Ô chọn món (Slot) — mô hình Combo Slot -->
          <div class="space-y-3 pt-5 border-t border-outline-variant/10">
            <div class="flex justify-between items-center">
              <label class="text-[10px] font-bold uppercase tracking-widest text-primary">Ô chọn món (Slots)</label>
              <button @click="addSlot" :disabled="optionGroups.length === 0" class="text-xs font-bold text-on-surface-variant hover:text-primary transition-colors flex items-center gap-1 disabled:opacity-40 disabled:cursor-not-allowed">
                <span class="material-symbols-outlined text-sm">add_circle</span> Thêm ô chọn
              </button>
            </div>

            <p v-if="optionGroups.length === 0" class="text-xs text-on-surface-variant italic">
              Chưa có Kho tùy chọn. Hãy tạo Kho ở tab "Kho Tùy Chọn (Pools)" trước.
            </p>
            <p v-else-if="form.slots.length === 0" class="text-xs text-on-surface-variant italic">
              Món không có ô chọn nào (bán trực tiếp, không cần chọn vị).
            </p>

            <div v-for="(slot, idx) in form.slots" :key="idx" class="bg-surface-container-highest p-3 rounded-xl border border-outline-variant/20 space-y-3">
              <div class="flex justify-between items-center">
                <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Ô chọn #{{ idx + 1 }}</span>
                <button @click="removeSlot(idx)" class="w-7 h-7 rounded-full hover:bg-red-500/20 text-on-surface-variant hover:text-red-400 flex items-center justify-center transition-colors">
                  <span class="material-symbols-outlined text-sm">close</span>
                </button>
              </div>

              <div class="space-y-2">
                <label class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant">Nhãn hiển thị</label>
                <input v-model="slot.slotLabel" class="w-full bg-surface-container border border-outline-variant/20 p-2 rounded-lg text-sm text-on-surface focus:border-primary outline-none" placeholder="VD: Chọn vị bắp / Nước 1" />
              </div>

              <div class="space-y-2">
                <label class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant">Kho tùy chọn (Pool)</label>
                <select v-model="slot.optionGroupId" class="w-full bg-surface-container border border-outline-variant/20 p-2 rounded-lg text-sm font-bold text-on-surface focus:border-primary outline-none" @change="slot.defaultOptionItemId = null">
                  <option v-for="g in optionGroups" :key="g.id" :value="g.id">{{ g.name }}</option>
                </select>
              </div>

              <div class="space-y-2" v-if="slot.optionGroupId">
                <label class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant">Vị mặc định</label>
                <select v-model="slot.defaultOptionItemId" class="w-full bg-surface-container border border-outline-variant/20 p-2 rounded-lg text-sm text-on-surface focus:border-primary outline-none">
                  <option :value="null">-- Không chọn sẵn --</option>
                  <option v-for="i in optionGroups.find(g => g.id === slot.optionGroupId)?.items || []" :key="i.id" :value="i.id">{{ i.name }} (+{{ formatPrice(i.surchargePrice) }})</option>
                </select>
              </div>

              <div class="grid grid-cols-2 gap-2">
                <div class="space-y-1">
                  <label class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant">Chọn tối thiểu</label>
                  <input v-model.number="slot.minChoices" type="number" min="0" class="w-full bg-surface-container border border-outline-variant/20 p-2 rounded-lg text-sm text-on-surface focus:border-primary outline-none" />
                </div>
                <div class="space-y-1">
                  <label class="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant">Chọn tối đa</label>
                  <input v-model.number="slot.maxChoices" type="number" min="1" class="w-full bg-surface-container border border-outline-variant/20 p-2 rounded-lg text-sm text-on-surface focus:border-primary outline-none" />
                </div>
              </div>

              <label class="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" v-model="slot.isRequired" class="w-4 h-4 text-primary bg-surface-container border-outline-variant rounded" />
                <span class="text-xs font-semibold text-on-surface">Bắt buộc chọn</span>
              </label>
            </div>
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

    <!-- Delete Group confirm -->
    <div v-if="deleteGroupTarget" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="deleteGroupTarget = null"></div>
      <div class="relative w-full max-w-sm bg-surface-container-low border border-white/10 rounded-2xl p-6 shadow-2xl text-center">
        <span class="material-symbols-outlined text-4xl text-red-400 mb-3">warning</span>
        <h3 class="text-lg font-bold font-headline text-white mb-2">Xoá Kho Tùy Chọn?</h3>
        <p class="text-sm text-on-surface-variant">Xoá "<span class="font-bold text-primary">{{ deleteGroupTarget.name }}</span>"? Nếu kho đang được dùng trong Ô chọn của combo, hệ thống sẽ chặn xoá — hãy gỡ khỏi combo trước.</p>
        <div class="flex gap-3 mt-6">
          <button @click="deleteGroupTarget = null" class="flex-1 px-4 py-3 rounded-xl border border-white/15 text-xs font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Huỷ</button>
          <button @click="confirmDeleteGroup" :disabled="isDeleting" class="flex-1 px-4 py-3 rounded-xl bg-red-500 text-white text-xs font-bold uppercase tracking-widest hover:brightness-110 transition-all disabled:opacity-60">{{ isDeleting ? 'Đang xoá...' : 'Xoá' }}</button>
        </div>
      </div>
    </div>

    <!-- Group Drawer Form -->
    <div v-if="isGroupDrawerOpen" class="fixed inset-0 z-[1000] flex justify-end">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="isGroupDrawerOpen = false"></div>
      <div class="relative w-full max-w-md bg-surface-container-low h-full shadow-2xl flex flex-col border-l border-outline-variant/20">
        <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <h3 class="font-headline font-black uppercase italic text-primary text-xl">{{ editingGroupId ? 'Sửa Kho Tùy Chọn' : 'Thêm Kho Tùy Chọn' }}</h3>
          <button @click="isGroupDrawerOpen = false" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-white/10 text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-6 space-y-5">
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Tên kho tùy chọn</label>
            <input v-model="groupForm.name" class="w-full bg-surface-container-highest border border-outline-variant/20 p-3 rounded-xl text-sm font-bold text-on-surface focus:border-primary outline-none" placeholder="VD: Tùy Chọn Bắp" />
            <p class="text-[10px] text-on-surface-variant italic">Kho dùng chung — số lượng chọn (min/max) đặt riêng ở từng Ô chọn của combo.</p>
          </div>

          <div class="space-y-4 mt-2 pt-6 border-t border-outline-variant/10">
            <div class="flex justify-between items-center">
              <label class="text-[10px] font-bold uppercase tracking-widest text-primary">Danh sách Vị con</label>
              <button @click="addGroupItem" class="text-xs font-bold text-on-surface-variant hover:text-primary transition-colors flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">add_circle</span> Thêm vị
              </button>
            </div>
            
            <div v-for="(item, idx) in groupForm.items" :key="idx" class="flex gap-2 items-center bg-surface-container-highest p-3 rounded-xl border border-outline-variant/20">
              <div class="flex-grow space-y-2">
                <input v-model="item.name" class="w-full bg-surface-container border border-outline-variant/20 p-2 rounded-lg text-sm text-on-surface focus:border-primary outline-none" placeholder="Tên vị (VD: Caramel)" />
                <input v-model="item.surchargePrice" type="number" min="0" class="w-full bg-surface-container border border-outline-variant/20 p-2 rounded-lg text-sm text-on-surface focus:border-primary outline-none" placeholder="Phụ thu (VD: 10000)" />
              </div>
              <button @click="removeGroupItem(idx)" class="w-8 h-8 rounded-full hover:bg-red-500/20 text-on-surface-variant hover:text-red-400 flex flex-shrink-0 items-center justify-center transition-colors">
                <span class="material-symbols-outlined text-sm">close</span>
              </button>
            </div>
          </div>
        </div>

        <div class="p-6 border-t border-outline-variant/10 bg-surface-container-lowest flex gap-4">
          <button @click="isGroupDrawerOpen = false" class="flex-1 px-6 py-3 rounded-xl border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy</button>
          <button @click="handleGroupSave" :disabled="isSaving" class="flex-1 px-6 py-3 rounded-xl bg-primary text-on-primary text-[10px] font-bold uppercase tracking-widest hover:scale-[1.02] transition-transform disabled:opacity-60">{{ isSaving ? 'Đang lưu...' : 'Lưu' }}</button>
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
