<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { movieApi } from '@/api/customer/index'
import api from '@/api/axios'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()
const query = ref('')
const results = ref([])
const isLoading = ref(false)
const hasSearched = ref(false)

const genres = ref([])
const suggested = ref([])
const isLoadingSuggested = ref(false)

let debounceTimer = null
const searchInput = ref(null)

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
    toast.error(friendlyError(e, 'Tìm kiếm thất bại, vui lòng thử lại.'))
  } finally {
    isLoading.value = false
  }
}

// Debounce 400ms theo quy ước dự án
const onInput = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => runSearch(query.value), 400)
}

const searchByGenre = (name) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  // Bấm lại chip đang chọn → bỏ lọc
  if (query.value === name) {
    resetSearch()
    return
  }
  query.value = name
  runSearch(name)
}

const resetSearch = () => {
  query.value = ''
  results.value = []
  hasSearched.value = false
  searchInput.value?.focus()
}

onMounted(async () => {
  searchInput.value?.focus()
  // Chip thể loại
  try {
    const { data } = await api.get('/categories/genres')
    genres.value = data
  } catch (e) {
    console.error('Không tải được thể loại', e)
  }
  // Phim gợi ý (đang chiếu) khi chưa nhập gì
  isLoadingSuggested.value = true
  try {
    const { data } = await movieApi.getNowShowing()
    suggested.value = (data.data ?? data).slice(0, 10)
  } catch (e) {
    console.error('Không tải được phim gợi ý', e)
  } finally {
    isLoadingSuggested.value = false
  }
})

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
  <main class="min-h-screen pt-32 pb-24 px-4 md:px-10 max-w-[1440px] mx-auto">
    <!-- Hero tìm kiếm -->
    <section class="mb-14">
      <div class="max-w-3xl mx-auto text-center">
        <p class="font-label text-[11px] uppercase tracking-[0.4em] text-primary-container/80 mb-4">DevCine • Thư viện phim</p>
        <h1 class="font-headline font-extrabold tracking-tighter text-4xl md:text-6xl text-on-surface mb-3">
          Khám phá <span class="text-primary-container">điện ảnh</span>
        </h1>
        <p class="text-on-surface-variant text-sm md:text-base mb-9 max-w-xl mx-auto">
          Tìm theo tên phim, thể loại hoặc đạo diễn — kết quả cập nhật ngay khi bạn gõ.
        </p>

        <!-- Ô tìm kiếm -->
        <div class="relative group">
          <div class="absolute -inset-0.5 bg-gradient-to-r from-primary/30 via-primary-container/20 to-primary/30 rounded-2xl blur opacity-40 group-focus-within:opacity-80 transition-opacity"></div>
          <div class="relative flex items-center bg-surface-container-low border border-outline-variant/15 rounded-2xl shadow-2xl overflow-hidden focus-within:border-primary-container/50 transition-colors">
            <span class="material-symbols-outlined text-2xl text-on-surface-variant pl-5">search</span>
            <input
              ref="searchInput"
              v-model="query"
              @input="onInput"
              type="text"
              class="flex-grow bg-transparent py-5 px-4 text-lg md:text-xl text-on-surface outline-none placeholder:text-neutral-600"
              placeholder="Tìm kiếm tiêu đề, thể loại hoặc đạo diễn..."
            />
            <button v-if="query" @click="resetSearch" class="pr-5 text-on-surface-variant hover:text-on-surface transition-colors">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
        </div>

        <!-- Chip thể loại gợi ý -->
        <div v-if="genres.length" class="flex flex-wrap items-center justify-center gap-2.5 mt-6">
          <span class="text-[11px] font-bold uppercase tracking-widest text-on-surface-variant/60 mr-1">Thể loại:</span>
          <button
            v-for="g in genres"
            :key="g.id"
            @click="searchByGenre(g.name)"
            :class="query === g.name ? 'bg-primary-container text-on-primary border-primary-container' : 'bg-surface-container-high/60 text-on-surface-variant border-outline-variant/15 hover:border-primary-container/50 hover:text-on-surface'"
            class="px-4 py-1.5 rounded-full border text-xs font-bold transition-all"
          >
            {{ g.name }}
          </button>
        </div>
      </div>
    </section>

    <!-- Loading kết quả -->
    <section v-if="isLoading" class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
      <div v-for="i in 10" :key="i" class="animate-pulse">
        <div class="aspect-[2/3] mb-4 rounded-xl bg-surface-container-high"></div>
        <div class="h-4 bg-surface-container-high rounded w-3/4 mb-2"></div>
        <div class="h-3 bg-surface-container-high rounded w-1/2"></div>
      </div>
    </section>

    <!-- Kết quả tìm kiếm -->
    <section v-else-if="results.length > 0">
      <div class="flex justify-between items-end mb-10">
        <div>
          <h2 class="text-2xl md:text-3xl font-headline font-extrabold tracking-tight text-on-surface uppercase mb-2">Kết quả tìm kiếm</h2>
          <div class="h-1 w-20 bg-primary-container rounded-full"></div>
        </div>
        <span class="text-on-surface-variant text-sm font-bold">{{ results.length }} phim</span>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
        <RouterLink v-for="m in results" :key="m.id" :to="`/movie/${m.id}`" class="group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-xl bg-surface-container-high border border-white/5">
            <img v-if="m.posterUrl" :src="m.posterUrl" class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" />
            <div v-else class="w-full h-full flex items-center justify-center">
              <span class="material-symbols-outlined text-4xl text-outline-variant">movie</span>
            </div>
            <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
            <div v-if="m.ageRating" class="absolute top-2 left-2 bg-error-container text-on-error-container px-2 py-0.5 rounded font-label text-[10px] font-bold">{{ m.ageRating }}</div>
            <div v-if="m.rating" class="absolute top-2 right-2 flex items-center gap-1 bg-black/70 backdrop-blur px-2 py-0.5 rounded text-[10px] font-bold text-primary-container">
              <span class="material-symbols-outlined text-[12px]" style="font-variation-settings:'FILL' 1">star</span>{{ m.rating }}
            </div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">{{ m.title }}</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">{{ formatMeta(m) }}</p>
        </RouterLink>
      </div>
    </section>

    <!-- Không có kết quả -->
    <section v-else-if="hasSearched" class="flex flex-col items-center justify-center py-20 bg-surface-container-low rounded-2xl border border-outline-variant/5">
      <div class="mb-8 inline-flex items-center justify-center w-28 h-28 rounded-full bg-surface-container-high border border-outline-variant/10 shadow-2xl">
        <span class="material-symbols-outlined text-5xl text-yellow-500/50" style="font-variation-settings: 'FILL' 1;">movie_filter</span>
      </div>
      <h2 class="font-headline text-2xl md:text-3xl font-bold mb-3">Không tìm thấy phim</h2>
      <p class="text-neutral-400 mb-9 leading-relaxed text-center max-w-lg">Không có kết quả nào cho "<span class="text-on-surface font-bold">{{ query }}</span>". Hãy thử một từ khoá khác xem sao.</p>
      <button @click="resetSearch" class="bg-surface-container-highest hover:bg-surface-bright text-primary border border-outline-variant/20 px-8 py-3 rounded-lg font-bold tracking-widest uppercase text-xs transition-all">Đặt lại tìm kiếm</button>
    </section>

    <!-- Trạng thái ban đầu: gợi ý phim đang chiếu -->
    <section v-else>
      <div class="flex items-center gap-3 mb-8">
        <span class="material-symbols-outlined text-primary-container">local_fire_department</span>
        <h2 class="text-xl md:text-2xl font-headline font-extrabold tracking-tight text-on-surface uppercase">Có thể bạn quan tâm</h2>
      </div>

      <!-- Loading gợi ý -->
      <div v-if="isLoadingSuggested" class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
        <div v-for="i in 5" :key="i" class="animate-pulse">
          <div class="aspect-[2/3] mb-4 rounded-xl bg-surface-container-high"></div>
          <div class="h-4 bg-surface-container-high rounded w-3/4"></div>
        </div>
      </div>

      <div v-else-if="suggested.length" class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
        <RouterLink v-for="m in suggested" :key="m.id" :to="`/movie/${m.id}`" class="group cursor-pointer">
          <div class="relative aspect-[2/3] mb-4 overflow-hidden rounded-xl bg-surface-container-high border border-white/5">
            <img v-if="m.posterUrl" :src="m.posterUrl" class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" />
            <div v-else class="w-full h-full flex items-center justify-center">
              <span class="material-symbols-outlined text-4xl text-outline-variant">movie</span>
            </div>
            <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
            <div v-if="m.ageRating" class="absolute top-2 left-2 bg-error-container text-on-error-container px-2 py-0.5 rounded font-label text-[10px] font-bold">{{ m.ageRating }}</div>
          </div>
          <h3 class="font-headline font-bold text-sm text-on-surface group-hover:text-primary-container transition-colors uppercase truncate">{{ m.title }}</h3>
          <p class="font-label text-[10px] text-on-surface-variant tracking-wider">{{ formatMeta(m) }}</p>
        </RouterLink>
      </div>

      <!-- Không có phim gợi ý -->
      <div v-else class="text-center py-16 text-on-surface-variant">
        <span class="material-symbols-outlined text-5xl text-outline-variant mb-3 block">search</span>
        <p>Nhập từ khoá để tìm kiếm phim trong hệ thống DevCine.</p>
      </div>
    </section>
  </main>
</template>
