<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '@/api/axios'
import AppButton from '../../components/common/AppButton.vue'
import AppModal from '../../components/common/AppModal.vue'
import { useConfirmStore } from '@/stores/confirm'
import { useAdminPerm } from '@/composables/useAdminPerm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const { isAdmin } = useAdminPerm()
const confirm = useConfirmStore()

const faqs = ref([])
const isLoading = ref(false)
const loadError = ref('')
const filterCategory = ref('Tất cả')

const isModalOpen = ref(false)
const isSaving = ref(false)
const editingItem = ref(null)
const form = ref({ category: '', question: '', answer: '', displayOrder: 0, isActive: true })

// Dropdown danh mục
const DEFAULT_CATEGORIES = ['Đặt vé & Thanh toán', 'Thành viên DevCine', 'Quy định rạp', 'Ưu đãi & Khuyến mãi']
const catDropdownOpen = ref(false)
const isCustomCat = ref(false)

// Toast
const toast = useToastStore()

const categories = computed(() => [...new Set(faqs.value.map(f => f.category).filter(Boolean))])
// Danh mục cho dropdown: gộp danh mục mặc định + danh mục đã có trong dữ liệu
const allCategories = computed(() => [...new Set([...DEFAULT_CATEGORIES, ...categories.value])])
const filteredFaqs = computed(() =>
  filterCategory.value === 'Tất cả' ? faqs.value : faqs.value.filter(f => f.category === filterCategory.value)
)

// Thứ tự hiển thị kế tiếp trong một danh mục (max + 1, bắt đầu từ 1)
const nextOrder = (cat) => {
  const inCat = faqs.value.filter(f => f.category === cat)
  return inCat.length ? Math.max(...inCat.map(f => f.displayOrder || 0)) + 1 : 1
}

const selectCategory = (c) => {
  form.value.category = c
  isCustomCat.value = false
  catDropdownOpen.value = false
  // Khi thêm mới, tự gợi ý thứ tự kế tiếp của danh mục vừa chọn
  if (!editingItem.value) form.value.displayOrder = nextOrder(c)
}

const startCustomCat = () => {
  isCustomCat.value = true
  catDropdownOpen.value = false
  form.value.category = ''
}

const fetchFaqs = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const { data } = await api.get('/faqs/all')
    faqs.value = data
  } catch (e) {
    loadError.value = friendlyError(e, 'Không thể tải danh sách FAQ.')
    toast.error(loadError.value)
  } finally {
    isLoading.value = false
  }
}

const openModal = (item = null) => {
  editingItem.value = item
  catDropdownOpen.value = false
  if (item) {
    form.value = { category: item.category, question: item.question, answer: item.answer || '', displayOrder: item.displayOrder ?? 0, isActive: item.isActive ?? true }
    isCustomCat.value = !!item.category && !allCategories.value.includes(item.category)
  } else {
    const initCat = filterCategory.value !== 'Tất cả' ? filterCategory.value : ''
    form.value = { category: initCat, question: '', answer: '', displayOrder: initCat ? nextOrder(initCat) : 1, isActive: true }
    isCustomCat.value = false
  }
  isModalOpen.value = true
}

const saveItem = async () => {
  if (!form.value.category.trim()) { toast.warning('Vui lòng nhập danh mục'); return }
  if (!form.value.question.trim()) { toast.warning('Vui lòng nhập câu hỏi'); return }
  isSaving.value = true
  try {
    const payload = {
      category: form.value.category.trim(),
      question: form.value.question.trim(),
      answer: form.value.answer,
      displayOrder: Number(form.value.displayOrder) || 0,
      isActive: form.value.isActive
    }
    if (editingItem.value) {
      await api.put(`/faqs/${editingItem.value.id}`, payload)
      toast.success('Đã cập nhật câu hỏi')
    } else {
      await api.post('/faqs', payload)
      toast.success('Đã thêm câu hỏi mới')
    }
    await fetchFaqs()
    isModalOpen.value = false
  } catch (e) {
    toast.error(friendlyError(e, 'Lưu thất bại.'))
  } finally {
    isSaving.value = false
  }
}

const toggleActive = async (item) => {
  try {
    await api.put(`/faqs/${item.id}`, { ...item, isActive: !item.isActive })
    item.isActive = !item.isActive
    toast.success(item.isActive ? 'Đã hiển thị câu hỏi' : 'Đã ẩn câu hỏi')
  } catch (e) {
    toast.error(friendlyError(e, 'Cập nhật thất bại.'))
  }
}

const deleteItem = async (item) => {
  const ok = await confirm.show({
    title: 'Xoá câu hỏi',
    message: `Xoá câu hỏi "${item.question}"?`,
    confirmText: 'Xoá',
    tone: 'danger',
  })
  if (!ok) return
  try {
    await api.delete(`/faqs/${item.id}`)
    toast.success('Đã xóa câu hỏi')
    await fetchFaqs()
  } catch (e) {
    toast.error(friendlyError(e, 'Xóa thất bại.'))
  }
}

onMounted(fetchFaqs)

</script>

<template>
  <div class="p-10">
    <header class="flex justify-between items-center mb-10">
      <div>
        <h1 class="text-3xl font-black tracking-tighter text-on-surface uppercase italic">
          Quản lý <span class="text-primary">Câu hỏi (FAQ)</span>
        </h1>
        <p class="text-on-surface-variant text-xs mt-1 font-bold uppercase tracking-widest">
          Nội dung trang Hỗ trợ
        </p>
      </div>
      <button
        v-if="isAdmin()"
        @click="openModal()"
        class="bg-primary text-on-primary font-headline font-bold text-xs uppercase tracking-widest px-8 py-3 rounded-sm hover:brightness-110 active:scale-95 transition-all flex items-center gap-2"
      >
        <span class="material-symbols-outlined text-sm">add</span>
        Thêm câu hỏi
      </button>
    </header>

    <!-- Filter danh mục -->
    <div class="flex flex-wrap gap-2 mb-8">
      <button
        v-for="cat in ['Tất cả', ...categories]" :key="cat"
        @click="filterCategory = cat"
        :class="filterCategory === cat ? 'bg-primary text-on-primary border-primary' : 'bg-surface-container-high/60 text-on-surface-variant border-outline-variant/15 hover:border-primary/40'"
        class="px-4 py-2 rounded-full border text-xs font-bold transition-all"
      >
        {{ cat }}
      </button>
    </div>

    <!-- Table -->
    <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden shadow-2xl">
      <table class="w-full text-left">
        <thead>
          <tr class="bg-surface-container-high/30 border-b border-outline-variant/10">
            <th class="px-6 py-5 text-[10px] font-black uppercase tracking-widest text-on-surface-variant w-16">Thứ tự</th>
            <th class="px-6 py-5 text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Câu hỏi</th>
            <th class="px-6 py-5 text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Danh mục</th>
            <th class="px-6 py-5 text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Trạng thái</th>
            <th class="px-6 py-5 text-[10px] font-black uppercase tracking-widest text-on-surface-variant text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-outline-variant/10">
          <tr v-if="isLoading"><td colspan="5" class="px-6 py-16 text-center"><span class="animate-pulse text-xs font-bold uppercase tracking-widest text-primary">Đang tải...</span></td></tr>
          <tr v-else-if="loadError"><td colspan="5" class="px-6 py-16 text-center">
            <span class="material-symbols-outlined text-3xl text-red-500/70 block mb-2">error</span>
            <p class="text-xs text-on-surface-variant mb-3">{{ loadError }}</p>
            <button @click="fetchFaqs" class="text-[10px] font-black uppercase tracking-widest text-primary hover:underline">Thử lại</button>
          </td></tr>
          <tr v-else-if="filteredFaqs.length === 0"><td colspan="5" class="px-6 py-16 text-center">
            <span class="material-symbols-outlined text-3xl text-on-surface-variant/30 block mb-2">quiz</span>
            <p class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Chưa có câu hỏi nào</p>
          </td></tr>
          <tr v-else v-for="item in filteredFaqs" :key="item.id" class="group hover:bg-white/5 transition-colors">
            <td class="px-6 py-4 text-xs font-mono text-on-surface-variant">{{ item.displayOrder }}</td>
            <td class="px-6 py-4 max-w-md">
              <p class="text-sm font-bold text-on-surface truncate">{{ item.question }}</p>
              <p class="text-xs text-on-surface-variant truncate mt-0.5">{{ item.answer }}</p>
            </td>
            <td class="px-6 py-4">
              <span class="px-2.5 py-1 bg-primary/10 text-primary border border-primary/20 rounded text-[10px] font-bold">{{ item.category }}</span>
            </td>
            <td class="px-6 py-4">
              <button v-if="isAdmin()" @click="toggleActive(item)"
                :class="item.isActive ? 'bg-green-500/10 text-green-500 border-green-500/20' : 'bg-surface-container-high text-on-surface-variant border-outline-variant/20'"
                class="px-3 py-1 rounded-full border text-[10px] font-bold uppercase tracking-widest transition-all">
                {{ item.isActive ? 'Hiển thị' : 'Đang ẩn' }}
              </button>
              <span v-else
                :class="item.isActive ? 'bg-green-500/10 text-green-500 border-green-500/20' : 'bg-surface-container-high text-on-surface-variant border-outline-variant/20'"
                class="px-3 py-1 rounded-full border text-[10px] font-bold uppercase tracking-widest inline-block">
                {{ item.isActive ? 'Hiển thị' : 'Đang ẩn' }}
              </span>
            </td>
            <td class="px-6 py-4 text-right">
              <div class="flex justify-end gap-2">
                <button
                  v-if="isAdmin()"
                  @click="toggleActive(item)"
                  :title="item.isActive ? 'Ẩn câu hỏi' : 'Hiển thị câu hỏi'"
                  class="p-2 transition-colors"
                  :class="item.isActive ? 'text-green-500 hover:text-on-surface-variant' : 'text-on-surface-variant hover:text-green-500'"
                >
                  <span class="material-symbols-outlined text-lg">{{ item.isActive ? 'visibility' : 'visibility_off' }}</span>
                </button>
                <button v-if="isAdmin()" @click="openModal(item)" class="p-2 hover:text-primary transition-colors"><span class="material-symbols-outlined text-lg">edit</span></button>
                <button v-if="isAdmin()" @click="deleteItem(item)" class="p-2 hover:text-red-500 transition-colors"><span class="material-symbols-outlined text-lg">delete</span></button>
                <span v-if="!isAdmin()" class="text-on-surface-variant/40 text-xs">—</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <AppModal :show="isModalOpen" @close="isModalOpen = false" :title="editingItem ? 'Sửa câu hỏi' : 'Thêm câu hỏi mới'">
      <div class="space-y-5 pt-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="space-y-2">
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Danh mục</label>
            <!-- Dropdown tùy biến: luôn hiện đủ danh mục -->
            <div v-if="!isCustomCat" class="relative">
              <button type="button" @click="catDropdownOpen = !catDropdownOpen"
                      class="w-full flex items-center justify-between gap-2 bg-surface-container-high border-none rounded-lg py-3 px-4 text-sm outline-none focus:ring-1 focus:ring-primary"
                      :class="form.category ? 'text-on-surface' : 'text-on-surface-variant'">
                <span class="truncate">{{ form.category || 'Chọn danh mục' }}</span>
                <span class="material-symbols-outlined text-base transition-transform" :class="{ 'rotate-180': catDropdownOpen }">expand_more</span>
              </button>
              <template v-if="catDropdownOpen">
                <!-- Backdrop bắt click ra ngoài -->
                <div class="fixed inset-0 z-10" @click="catDropdownOpen = false"></div>
                <div class="absolute z-20 mt-2 w-full bg-surface-container-high border border-outline-variant/20 rounded-lg shadow-2xl overflow-hidden py-1 max-h-60 overflow-y-auto">
                  <button v-for="c in allCategories" :key="c" type="button" @click="selectCategory(c)"
                          class="w-full text-left px-4 py-2.5 text-sm hover:bg-primary/10 transition-colors flex items-center justify-between"
                          :class="form.category === c ? 'text-primary font-bold' : 'text-on-surface'">
                    <span class="truncate">{{ c }}</span>
                    <span v-if="form.category === c" class="material-symbols-outlined text-sm">check</span>
                  </button>
                  <div class="border-t border-outline-variant/15 my-1"></div>
                  <button type="button" @click="startCustomCat"
                          class="w-full text-left px-4 py-2.5 text-sm text-on-surface-variant hover:bg-primary/10 transition-colors flex items-center gap-2">
                    <span class="material-symbols-outlined text-sm">add</span> Danh mục mới...
                  </button>
                </div>
              </template>
            </div>
            <!-- Nhập danh mục mới -->
            <div v-else class="relative">
              <input v-model="form.category" placeholder="Tên danh mục mới"
                     class="w-full bg-surface-container-high border-none rounded-lg py-3 pl-4 pr-10 text-on-surface focus:ring-1 focus:ring-primary outline-none text-sm" />
              <button type="button" @click="isCustomCat = false; form.category = ''"
                      class="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-on-surface-variant hover:text-primary" title="Chọn từ danh sách">
                <span class="material-symbols-outlined text-base">list</span>
              </button>
            </div>
          </div>
          <div class="space-y-2">
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Thứ tự hiển thị</label>
            <input v-model.number="form.displayOrder" type="number" min="0"
                   class="w-full bg-surface-container-high border-none rounded-lg py-3 px-4 text-on-surface focus:ring-1 focus:ring-primary outline-none text-sm" />
            <p class="text-[10px] text-on-surface-variant/70">Số nhỏ hiển thị trước, theo từng danh mục.</p>
          </div>
        </div>
        <div class="space-y-2">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Câu hỏi</label>
          <input v-model="form.question" placeholder="Nhập câu hỏi..."
                 class="w-full bg-surface-container-high border-none rounded-lg py-3 px-4 text-on-surface focus:ring-1 focus:ring-primary outline-none text-sm" />
        </div>
        <div class="space-y-2">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Câu trả lời (xuống dòng để tách ý)</label>
          <textarea v-model="form.answer" rows="6" placeholder="Nhập nội dung trả lời..."
                    class="w-full bg-surface-container-high border-none rounded-lg py-3 px-4 text-on-surface focus:ring-1 focus:ring-primary outline-none text-sm resize-none"></textarea>
        </div>
        <label class="flex items-center gap-3 cursor-pointer">
          <input type="checkbox" v-model="form.isActive" class="w-4 h-4 accent-primary" />
          <span class="text-sm font-bold text-on-surface">Hiển thị câu hỏi này lên web</span>
        </label>
        <div class="flex gap-4 pt-2">
          <AppButton variant="ghost" class="flex-1" @click="isModalOpen = false" :disabled="isSaving">Hủy</AppButton>
          <AppButton class="flex-1" @click="saveItem" :disabled="isSaving">{{ isSaving ? 'Đang lưu...' : 'Lưu' }}</AppButton>
        </div>
      </div>
    </AppModal>

    <!-- Toast -->
  </div>
</template>
