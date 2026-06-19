<script setup>
import { ref, onMounted } from 'vue'
import { bannerApi } from '@/api/admin/index'

const banners = ref([])
const isLoading = ref(false)
const isAddModalOpen = ref(false)
const isSaving = ref(false)
const newBanner = ref({ imageUrl: '', link: '', isActive: true, order: 1 })

const fetchBanners = async () => {
  isLoading.value = true
  try {
    const { data } = await bannerApi.getAll()
    banners.value = data.data ?? data
  } catch (e) {
    console.error('Failed to load banners', e)
  } finally {
    isLoading.value = false
  }
}

const openAddModal = () => {
  newBanner.value = { imageUrl: '', link: '', isActive: true, order: banners.value.length + 1 }
  isAddModalOpen.value = true
}

const closeAddModal = () => {
  isAddModalOpen.value = false
}

const saveBanner = async () => {
  isSaving.value = true
  try {
    await bannerApi.create(newBanner.value)
    await fetchBanners()
    closeAddModal()
  } catch (e) {
    console.error('Failed to save banner', e)
  } finally {
    isSaving.value = false
  }
}

const toggleActive = async (banner) => {
  try {
    await bannerApi.update(banner.id, { ...banner, isActive: !banner.isActive })
    await fetchBanners()
  } catch (e) {
    console.error('Failed to toggle banner', e)
  }
}

const deleteBanner = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xoá banner này?')) return
  try {
    await bannerApi.delete(id)
    banners.value = banners.value.filter(b => b.id !== id)
  } catch (e) {
    console.error('Failed to delete banner', e)
  }
}

onMounted(fetchBanners)
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
          <img v-if="banner.imageUrl" :src="banner.imageUrl" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" alt="Banner preview" />
          <div v-else class="w-full h-full flex items-center justify-center text-on-surface-variant">
            <span class="material-symbols-outlined text-4xl opacity-20">broken_image</span>
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
            <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Link điều hướng</p>
            <p class="text-sm text-on-surface truncate font-mono bg-surface-container-highest px-2 py-1 rounded">{{ banner.link || 'Không có link' }}</p>
          </div>

          <div class="flex items-center justify-between mt-auto pt-4 border-t border-outline-variant/10">
            <div class="flex items-center gap-2">
              <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thứ tự</label>
              <input v-model="banner.order" type="number" class="w-16 bg-surface-container-high border-none text-sm rounded focus:ring-1 focus:ring-primary py-1 px-2 text-on-surface text-center">
            </div>

            <div class="flex items-center gap-2">
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

    <!-- Add Banner Modal Overlay -->
    <div v-if="isAddModalOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
      <div class="bg-surface-container-low border border-outline-variant/20 rounded-xl w-full max-w-lg shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        <div class="px-6 py-4 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest">
          <h2 class="font-headline font-bold uppercase tracking-tight text-on-surface">Thêm Banner Mới</h2>
          <button @click="closeAddModal" class="text-on-surface-variant hover:text-white transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        
        <div class="p-6 space-y-5">
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">URL Hình ảnh</label>
            <input v-model="newBanner.imageUrl" type="text" placeholder="https://..." class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
            <!-- Preview tiny -->
            <div v-if="newBanner.imageUrl" class="mt-2 h-20 w-full rounded overflow-hidden bg-surface-container-highest">
               <img :src="newBanner.imageUrl" class="w-full h-full object-cover" />
            </div>
          </div>
          
          <div class="space-y-2">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Link Điều hướng (Tuỳ chọn)</label>
            <input v-model="newBanner.link" type="text" placeholder="/movies/..." class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
          </div>

          <div class="flex gap-4">
             <div class="space-y-2 flex-1">
               <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Thứ tự ưu tiên</label>
               <input v-model="newBanner.order" type="number" class="w-full bg-surface-container-high border-none text-sm rounded-lg focus:ring-1 focus:ring-primary py-3 px-4 text-on-surface">
             </div>
             
             <div class="space-y-2 flex-1 flex flex-col">
               <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Trạng thái</label>
               <button @click="newBanner.isActive = !newBanner.isActive" :class="newBanner.isActive ? 'bg-primary text-on-primary' : 'bg-surface-container-highest text-on-surface-variant'" class="w-full h-full rounded-lg text-xs font-bold uppercase tracking-widest transition-all">
                 {{ newBanner.isActive ? 'BẬT (HIỂN THỊ)' : 'TẮT (ẨN)' }}
               </button>
             </div>
          </div>
        </div>

        <div class="px-6 py-4 bg-surface-container-lowest border-t border-outline-variant/10 flex justify-end gap-3">
          <button @click="closeAddModal" class="px-5 py-2.5 bg-surface-container-highest text-on-surface font-bold text-xs uppercase tracking-widest rounded hover:bg-white/10 transition-all">Huỷ</button>
          <button @click="saveBanner" :disabled="isSaving" class="px-5 py-2.5 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded hover:brightness-110 transition-all disabled:opacity-60">{{ isSaving ? 'Đang lưu...' : 'Lưu Banner' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
