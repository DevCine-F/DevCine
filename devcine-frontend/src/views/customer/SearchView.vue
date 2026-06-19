<script setup>
import { ref, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { movieApi } from '@/api/customer/index'

const query = ref('')
const results = ref([])
const isLoading = ref(false)
const hasSearched = ref(false)
let debounceTimer = null

const runSearch = async (keyword) => {
  if (!keyword || !keyword.trim()) {
    results.value = []
    hasSearched.value = false
    return
  }
  isLoading.value = true
  hasSearched.value = true
  try {
    const { data } = await movieApi.search(keyword.trim())
    results.value = data.data ?? data
  } catch (e) {
    console.error('Tìm kiếm thất bại', e)
    results.value = []
  } finally {
    isLoading.value = false
  }
}

// Debounce 400ms theo quy ước dự án (tránh gọi API mỗi phím gõ)
const onInput = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => runSearch(query.value), 400)
}

const resetSearch = () => {
  query.value = ''
  results.value = []
  hasSearched.value = false
}

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
})

const formatMeta = (m) => {
  const parts = []
  if (m.genres && m.genres.length) parts.push([...m.genres][0].name)
  if (m.durationMins) parts.push(`${m.durationMins}m`)
  return parts.join(' • ')
}
</script>

<template>
  <main class="min-h-screen pt-32 pb-20 px-4 md:px-10 max-w-[1440px] mx-auto">
    <!-- Search Interface -->
    <section class="mb-16">
      <div class="max-w-3xl mx-auto text-center space-y-8">
        <h1 class="font-headline font-extrabold tracking-tighter text-5xl md:text-6xl text-on-surface">Khám phá điện ảnh</h1>
        <div class="relative group">
          <div class="absolute inset-y-0 left-5 flex items-center pointer-events-none text-outline">
            <span class="material-symbols-outlined">search</span>
          </div>
          <input v-model="query" @input="onInput" type="text" autofocus
                 class="w-full bg-surface-container-lowest border-0 focus:ring-1 focus:ring-primary-container py-6 pl-14 pr-6 text-xl font-body rounded-lg shadow-inner outline-none transition-all placeholder:text-neutral-600"
                 placeholder="Tìm kiếm tiêu đề, thể loại hoặc đạo diễn..." />
          <button v-if="query" @click="resetSearch" class="absolute inset-y-0 right-5 flex items-center text-outline hover:text-on-surface transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </div>
    </section>

    <!-- Loading -->
    <section v-if="isLoading" class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
      <div v-for="i in 5" :key="i" class="animate-pulse">
        <div class="aspect-[2/3] mb-4 rounded-sm bg-surface-container-high"></div>
        <div class="h-4 bg-surface-container-high rounded w-3/4 mb-2"></div>
        <div class="h-3 bg-surface-container-high rounded w-1/2"></div>
      </div>
    </section>

    <!-- Results -->
    <section v-else-if="results.length > 0">
      <div class="flex justify-between items-end mb-12">
        <div>
          <h2 class="text-3xl font-headline font-extrabold tracking-tight text-on-surface uppercase mb-2">Kết quả tìm kiếm</h2>
          <div class="h-1 w-20 bg-primary-container"></div>
        </div>
        <span class="text-on-surface-variant text-sm">{{ results.length }} phim</span>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
        <router-link v-for="m in results" :key="m.id" :to="`/movie/${m.id}`" class="group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-sm bg-surface-container-high">
            <img v-if="m.posterUrl" :src="m.posterUrl" class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" />
            <div v-else class="w-full h-full flex items-center justify-center">
              <span class="material-symbols-outlined text-4xl text-outline-variant">movie</span>
            </div>
            <div v-if="m.ageRating" class="absolute top-0 left-0 bg-error-container text-on-error-container px-2 py-0.5 font-label text-[10px] font-bold">{{ m.ageRating }}</div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">{{ m.titleVietnamese || m.title }}</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">{{ formatMeta(m) }}</p>
        </router-link>
      </div>
    </section>

    <!-- Empty (đã tìm nhưng không có kết quả) -->
    <section v-else-if="hasSearched" class="flex flex-col items-center justify-center py-20 bg-surface-container-low rounded-xl border border-outline-variant/5">
      <div class="mb-8 inline-flex items-center justify-center w-32 h-32 rounded-full bg-surface-container-high border border-outline-variant/10 shadow-2xl">
        <span class="material-symbols-outlined text-6xl text-yellow-500/50" style="font-variation-settings: 'FILL' 1;">movie_filter</span>
      </div>
      <h2 class="font-headline text-3xl font-bold mb-4">Không tìm thấy phim</h2>
      <p class="text-neutral-400 mb-10 leading-relaxed text-center max-w-lg">Không có kết quả nào cho "{{ query }}". Hãy thử một từ khoá khác xem sao.</p>
      <button @click="resetSearch" class="bg-surface-container-highest hover:bg-surface-bright text-primary border border-outline-variant/20 px-8 py-3 font-bold tracking-widest uppercase transition-all">Đặt lại tìm kiếm</button>
    </section>

    <!-- Trạng thái ban đầu (chưa nhập) -->
    <section v-else class="text-center py-20 text-on-surface-variant">
      <span class="material-symbols-outlined text-5xl text-outline-variant mb-4">search</span>
      <p>Nhập từ khoá để tìm kiếm phim trong hệ thống DevCine.</p>
    </section>
  </main>
</template>
