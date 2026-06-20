<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '@/api/axios'
import AppButton from '../../components/common/AppButton.vue'
import AppModal from '../../components/common/AppModal.vue'

const faqs = ref([])
const isLoading = ref(false)
const loadError = ref('')
const filterCategory = ref('Tất cả')

const isModalOpen = ref(false)
const isSaving = ref(false)
const editingItem = ref(null)
const form = ref({ category: '', question: '', answer: '', displayOrder: 0, isActive: true })

// Toast
const toast = ref({ show: false, type: 'success', message: '' })
let toastTimer = null
const showToast = (message, type = 'success') => {
  toast.value = { show: true, type, message }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value.show = false }, 3000)
}
const errMsg = (e, fb) => e?.response?.data?.message || e?.response?.data?.error || fb

const categories = computed(() => [...new Set(faqs.value.map(f => f.category).filter(Boolean))])
const filteredFaqs = computed(() =>
  filterCategory.value === 'Tất cả' ? faqs.value : faqs.value.filter(f => f.category === filterCategory.value)
)

const fetchFaqs = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    const { data } = await api.get('/faqs/all')
    faqs.value = data
  } catch (e) {
    console.error(e)
    loadError.value = errMsg(e, 'Không thể tải danh sách FAQ.')
  } finally {
    isLoading.value = false
  }
}

const openModal = (item = null) => {
  editingItem.value = item
  if (item) {
    form.value = { category: item.category, question: item.question, answer: item.answer || '', displayOrder: item.displayOrder ?? 0, isActive: item.isActive ?? true }
  } else {
    form.value = { category: filterCategory.value !== 'Tất cả' ? filterCategory.value : '', question: '', answer: '', displayOrder: 0, isActive: true }
  }
  isModalOpen.value = true
}

const saveItem = async () => {
  if (!form.value.category.trim()) { showToast('Vui lòng nhập danh mục', 'error'); return }
  if (!form.value.question.trim()) { showToast('Vui lòng nhập câu hỏi', 'error'); return }
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
      showToast('Đã cập nhật câu hỏi')
    } else {
      await api.post('/faqs', payload)
      showToast('Đã thêm câu hỏi mới')
    }
    await fetchFaqs()
    isModalOpen.value = false
  } catch (e) {
    console.error(e)
    showToast(errMsg(e, 'Lưu thất bại.'), 'error')
  } finally {
    isSaving.value = false
  }
}

const toggleActive = async (item) => {
  try {
    await api.put(`/faqs/${item.id}`, { ...item, isActive: !item.isActive })
    item.isActive = !item.isActive
    showToast(item.isActive ? 'Đã hiển thị câu hỏi' : 'Đã ẩn câu hỏi')
  } catch (e) {
    showToast(errMsg(e, 'Cập nhật thất bại.'), 'error')
  }
}

const deleteItem = async (item) => {
  if (!confirm(`Xóa câu hỏi "${item.question}"?`)) return
  try {
    await api.delete(`/faqs/${item.id}`)
    showToast('Đã xóa câu hỏi')
    await fetchFaqs()
  } catch (e) {
    showToast(errMsg(e, 'Xóa thất bại.'), 'error')
  }
}

onMounted(fetchFaqs)
onUnmounted(() => { if (toastTimer) clearTimeout(toastTimer) })
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
              <button @click="toggleActive(item)"
                :class="item.isActive ? 'bg-green-500/10 text-green-500 border-green-500/20' : 'bg-surface-container-high text-on-surface-variant border-outline-variant/20'"
                class="px-3 py-1 rounded-full border text-[10px] font-bold uppercase tracking-widest transition-all">
                {{ item.isActive ? 'Hiển thị' : 'Đang ẩn' }}
              </button>
            </td>
            <td class="px-6 py-4 text-right">
              <div class="flex justify-end gap-2">
                <button
                  @click="toggleActive(item)"
                  :title="item.isActive ? 'Ẩn câu hỏi' : 'Hiển thị câu hỏi'"
                  class="p-2 transition-colors"
                  :class="item.isActive ? 'text-green-500 hover:text-on-surface-variant' : 'text-on-surface-variant hover:text-green-500'"
                >
                  <span class="material-symbols-outlined text-lg">{{ item.isActive ? 'visibility' : 'visibility_off' }}</span>
                </button>
                <button @click="openModal(item)" class="p-2 hover:text-primary transition-colors"><span class="material-symbols-outlined text-lg">edit</span></button>
                <button @click="deleteItem(item)" class="p-2 hover:text-red-500 transition-colors"><span class="material-symbols-outlined text-lg">delete</span></button>
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
            <input v-model="form.category" list="faq-cats" placeholder="VD: Đặt vé & Thanh toán"
                   class="w-full bg-surface-container-high border-none rounded-lg py-3 px-4 text-on-surface focus:ring-1 focus:ring-primary outline-none text-sm" />
            <datalist id="faq-cats">
              <option v-for="c in categories" :key="c" :value="c" />
            </datalist>
          </div>
          <div class="space-y-2">
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Thứ tự hiển thị</label>
            <input v-model.number="form.displayOrder" type="number" min="0"
                   class="w-full bg-surface-container-high border-none rounded-lg py-3 px-4 text-on-surface focus:ring-1 focus:ring-primary outline-none text-sm" />
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
    <transition name="fade">
      <div v-if="toast.show" :class="toast.type === 'error' ? 'bg-red-600' : 'bg-green-600'"
           class="fixed bottom-8 right-8 z-[200] px-6 py-4 rounded-xl shadow-2xl text-white flex items-center gap-3 max-w-sm">
        <span class="material-symbols-outlined text-lg">{{ toast.type === 'error' ? 'error' : 'check_circle' }}</span>
        <span class="text-sm font-bold">{{ toast.message }}</span>
      </div>
    </transition>
  </div>
</template>
