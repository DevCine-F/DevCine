<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import AppButton from '../../components/common/AppButton.vue'
import AppModal from '../../components/common/AppModal.vue'

const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080') + '/api/categories'

const genres = ref([])
const formats = ref([])
const ageRatings = ref([])

const activeTab = ref('genres') // 'genres', 'formats', 'age-ratings'

const isLoading = ref(false)
const isModalOpen = ref(false)
const editingItem = ref(null)
const newItem = ref({ name: '', code: '', description: '' })

const fetchData = async () => {
  isLoading.value = true
  try {
    const [genresRes, formatsRes, ageRatingsRes] = await Promise.all([
      axios.get(`${API_BASE_URL}/genres`),
      axios.get(`${API_BASE_URL}/formats`),
      axios.get(`${API_BASE_URL}/age-ratings`)
    ])
    genres.value = genresRes.data
    formats.value = formatsRes.data
    ageRatings.value = ageRatingsRes.data
  } catch (error) {
    console.error('Error fetching categories:', error)
  } finally {
    isLoading.value = false
  }
}

const openModal = (item = null) => {
  editingItem.value = item
  if (item) {
    newItem.value = { ...item }
  } else {
    newItem.value = { name: '', code: '', description: '' }
  }
  isModalOpen.value = true
}

const saveItem = async () => {
  try {
    const endpoint = `${API_BASE_URL}/${activeTab.value}`
    if (editingItem.value) {
      await axios.post(endpoint, newItem.value) // Simplified for mock, assuming save works
    } else {
      await axios.post(endpoint, newItem.value)
    }
    await fetchData()
    isModalOpen.value = false
  } catch (error) {
    console.error('Error saving item:', error)
  }
}

const deleteItem = async (id) => {
  if (!confirm('Bạn có chắc muốn xóa danh mục này?')) return
  try {
    await axios.delete(`${API_BASE_URL}/${activeTab.value}/${id}`)
    await fetchData()
  } catch (error) {
    console.error('Error deleting item:', error)
  }
}


onMounted(fetchData)
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
          <tr v-for="item in (activeTab === 'genres' ? genres : activeTab === 'formats' ? formats : ageRatings)" 
              :key="item.id" class="group hover:bg-white/5 transition-colors">
            <td class="px-8 py-4 text-xs font-mono text-on-surface-variant">#{{ item.id }}</td>
            <td class="px-8 py-4">
              <div v-if="activeTab === 'age-ratings'" class="flex items-center gap-3">
                <span class="px-3 py-1 bg-primary/10 text-primary border border-primary/20 rounded text-[10px] font-black">{{ item.code }}</span>
                <span class="text-sm font-bold">{{ item.name }}</span>
              </div>
              <span v-else class="text-sm font-bold uppercase tracking-tight text-on-surface group-hover:text-primary transition-colors">
                {{ item.name }}
              </span>
            </td>
            <td class="px-8 py-4 text-xs text-on-surface-variant max-w-md truncate">{{ item.description || 'Chưa có mô tả' }}</td>
            <td class="px-8 py-4 text-right">
              <div class="flex justify-end gap-2">
                <button @click="openModal(item)" class="p-2 hover:text-primary transition-colors">
                  <span class="material-symbols-outlined text-lg">edit</span>
                </button>
                <button @click="deleteItem(item.id)" class="p-2 hover:text-red-500 transition-colors">
                  <span class="material-symbols-outlined text-lg">delete</span>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="isLoading">
            <td colspan="4" class="px-8 py-10 text-center">
              <span class="animate-pulse text-xs font-bold uppercase tracking-widest text-primary">Đang tải dữ liệu...</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal -->
    <AppModal v-model="isModalOpen" :title="editingItem ? 'Sửa danh mục' : 'Thêm danh mục mới'">
      <div class="space-y-6 pt-4">
        <div v-if="activeTab === 'age-ratings'" class="space-y-2">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Mã kiểm duyệt (Code)</label>
          <input v-model="newItem.code" placeholder="P, T13, T16..." 
                 class="w-full bg-surface-container-high border-none rounded-lg py-3 px-4 text-on-surface focus:ring-1 focus:ring-primary outline-none text-sm" />
        </div>
        <div class="space-y-2">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tên danh mục</label>
          <input v-model="newItem.name" placeholder="Nhập tên..." 
                 class="w-full bg-surface-container-high border-none rounded-lg py-3 px-4 text-on-surface focus:ring-1 focus:ring-primary outline-none text-sm" />
        </div>
        <div class="space-y-2">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Mô tả</label>
          <textarea v-model="newItem.description" rows="3" placeholder="Ghi chú thêm..." 
                    class="w-full bg-surface-container-high border-none rounded-lg py-3 px-4 text-on-surface focus:ring-1 focus:ring-primary outline-none text-sm resize-none"></textarea>
        </div>
        <div class="flex gap-4 pt-4">
          <AppButton variant="ghost" class="flex-1" @click="isModalOpen = false">Hủy</AppButton>
          <AppButton class="flex-1" @click="saveItem">Lưu thay đổi</AppButton>
        </div>
      </div>
    </AppModal>
  </div>
</template>
