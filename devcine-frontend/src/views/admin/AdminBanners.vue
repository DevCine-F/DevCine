<script setup>
import { ref } from 'vue'

const banners = ref([
  {
    id: 1,
    imageUrl: 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?q=80&w=2000&auto=format&fit=crop',
    link: '/movies/oppenheimer',
    isActive: true,
    order: 1
  },
  {
    id: 2,
    imageUrl: 'https://images.unsplash.com/photo-1626814026160-2237a95fc5a0?q=80&w=2000&auto=format&fit=crop',
    link: '/movies/avatar-2',
    isActive: true,
    order: 2
  },
  {
    id: 3,
    imageUrl: 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?q=80&w=2000&auto=format&fit=crop',
    link: '/promotions/summer',
    isActive: false,
    order: 3
  }
])

const isAddModalOpen = ref(false)
const newBanner = ref({
  imageUrl: '',
  link: '',
  isActive: true,
  order: 1
})

const openAddModal = () => {
  newBanner.value = { imageUrl: '', link: '', isActive: true, order: banners.value.length + 1 }
  isAddModalOpen.value = true
}

const closeAddModal = () => {
  isAddModalOpen.value = false
}

const saveBanner = () => {
  banners.value.push({
    id: Date.now(),
    ...newBanner.value
  })
  closeAddModal()
}

const deleteBanner = (id) => {
  if (confirm('Bạn có chắc chắn muốn xoá banner này?')) {
    banners.value = banners.value.filter(b => b.id !== id)
  }
}
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
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 flex-1 overflow-y-auto pr-2 pb-10">
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
              <button class="w-8 h-8 rounded-full bg-surface-container-highest hover:bg-white/10 flex items-center justify-center text-on-surface-variant transition-colors" title="Chỉnh sửa">
                <span class="material-symbols-outlined text-sm">edit</span>
              </button>
              <button @click="deleteBanner(banner.id)" class="w-8 h-8 rounded-full bg-surface-container-highest hover:bg-red-500/20 hover:text-red-400 flex items-center justify-center text-on-surface-variant transition-colors" title="Xoá">
                <span class="material-symbols-outlined text-sm">delete</span>
              </button>
            </div>
          </div>
        </div>
      </div>
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
          <button @click="saveBanner" class="px-5 py-2.5 bg-primary text-on-primary font-bold text-xs uppercase tracking-widest rounded hover:brightness-110 transition-all">Lưu Banner</button>
        </div>
      </div>
    </div>
  </div>
</template>
