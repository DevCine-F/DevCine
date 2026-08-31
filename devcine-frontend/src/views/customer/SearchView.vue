<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'
import { movieApi } from '@/api/customer/index'
import api from '@/api/axios'
import MovieCard from '@/components/customer/MovieCard.vue'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()
const query = ref('')
const results = ref([])
const isLoading = ref(false)
const hasSearched = ref(false)

const page = ref(1)
const pageSize = ref(15)

const totalPages = computed(() => Math.max(1, Math.ceil(results.value.length / pageSize.value)))

const pagedResults = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return results.value.slice(start, start + pageSize.value)
})

const pageNumbers = computed(() => {
  const total = totalPages.value;
  const current = page.value;
  const pages = [];
  const add = (n) => pages.push(n);
  if (total <= 7) {
    for (let i = 1; i <= total; i++) add(i);
  } else {
    add(1);
    if (current > 3) add("…");
    for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) add(i);
    if (current < total - 2) add("…");
    add(total);
  }
  return pages;
});

const goToPage = (n) => {
  page.value = Math.min(Math.max(1, n), totalPages.value)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const genres = ref([])
const suggested = ref([])
const isLoadingSuggested = ref(false)

let debounceTimer = null
const searchInput = ref(null)

const runSearch = async (keyword) => {
  if (!keyword || !keyword.trim()) {
    results.value = []
    hasSearched.value = false
    page.value = 1
    return
  }
  isLoading.value = true
  hasSearched.value = true
  page.value = 1
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
  page.value = 1
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
    suggested.value = (data.data ?? data).slice(0, 15)
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
  <main class="min-h-screen pt-28 sm:pt-32 pb-16 sm:pb-24 px-4 sm:px-6 md:px-10 max-w-[1440px] mx-auto">
    <!-- Hero tìm kiếm -->
    <section class="mb-10 sm:mb-14">
      <div class="max-w-3xl mx-auto text-center">
        <p class="font-label text-[10px] sm:text-[11px] uppercase tracking-[0.4em] text-primary-container/80 mb-3 sm:mb-4">DevCine • Thư viện phim</p>
        <h1 class="font-headline font-extrabold tracking-tighter text-3xl sm:text-4xl md:text-6xl text-on-surface mb-3">
          Khám phá <span class="text-primary-container">điện ảnh</span>
        </h1>
        <p class="text-on-surface-variant text-xs sm:text-sm md:text-base mb-6 sm:mb-9 max-w-xl mx-auto px-2">
          Tìm theo tên phim, thể loại hoặc đạo diễn — kết quả cập nhật ngay khi bạn gõ.
        </p>

        <!-- Ô tìm kiếm -->
        <div class="relative group">
          <div class="absolute -inset-0.5 bg-gradient-to-r from-primary/30 via-primary-container/20 to-primary/30 rounded-2xl blur opacity-40 group-focus-within:opacity-80 transition-opacity"></div>
          <div class="relative flex items-center bg-surface-container-low border border-outline-variant/15 rounded-2xl shadow-2xl overflow-hidden focus-within:border-primary-container/50 transition-colors">
            <span class="material-symbols-outlined text-xl sm:text-2xl text-on-surface-variant pl-4 sm:pl-5">search</span>
            <input
              ref="searchInput"
              v-model="query"
              @input="onInput"
              type="text"
              class="flex-grow bg-transparent py-3.5 sm:py-5 px-3 sm:px-4 text-base sm:text-lg md:text-xl text-on-surface outline-none placeholder:text-neutral-600"
              placeholder="Tìm kiếm tiêu đề, thể loại hoặc đạo diễn..."
            />
            <button v-if="query" @click="resetSearch" class="pr-4 sm:pr-5 text-on-surface-variant hover:text-on-surface transition-colors">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>
        </div>

        <!-- Chip thể loại gợi ý -->
        <div v-if="genres.length" class="flex flex-wrap items-center justify-center gap-2 sm:gap-2.5 mt-5 sm:mt-6">
          <span class="text-[10px] sm:text-[11px] font-bold uppercase tracking-widest text-on-surface-variant/60 mr-1">Thể loại:</span>
          <button
            v-for="g in genres"
            :key="g.id"
            @click="searchByGenre(g.name)"
            :class="query === g.name ? 'bg-primary-container text-on-primary border-primary-container' : 'bg-surface-container-high/60 text-on-surface-variant border-outline-variant/15 hover:border-primary-container/50 hover:text-on-surface'"
            class="px-3 sm:px-4 py-1 sm:py-1.5 max-w-full break-words rounded-full border text-[11px] sm:text-xs font-bold transition-all"
          >
            {{ g.name }}
          </button>
        </div>
      </div>
    </section>

    <!-- Loading kết quả -->
    <section v-if="isLoading" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3.5 sm:gap-6">
      <div v-for="i in 15" :key="i" class="animate-pulse">
        <div class="aspect-[2/3] mb-4 rounded-xl bg-surface-container-high"></div>
        <div class="h-4 bg-surface-container-high rounded w-3/4 mb-2"></div>
        <div class="h-3 bg-surface-container-high rounded w-1/2"></div>
      </div>
    </section>

    <!-- Kết quả tìm kiếm -->
    <section v-else-if="results.length > 0">
      <div class="flex justify-between items-end mb-6 sm:mb-10">
        <div>
          <h2 class="text-xl sm:text-2xl md:text-3xl font-headline font-extrabold tracking-tight text-on-surface uppercase mb-2">Kết quả tìm kiếm</h2>
          <div class="h-1 w-16 sm:w-20 bg-primary-container rounded-full"></div>
        </div>
        <span class="text-on-surface-variant text-xs sm:text-sm font-bold">{{ results.length }} phim</span>
      </div>
      <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3.5 sm:gap-6">
        <MovieCard v-for="m in pagedResults" :key="m.id" :movie="m" />
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex flex-col items-center mt-10 sm:mt-12 gap-4">
        <div class="flex items-center gap-1.5 sm:gap-2 flex-wrap justify-center">
          <button
            @click="goToPage(page - 1)"
            :disabled="page === 1"
            class="px-2.5 sm:px-3 h-8 sm:h-9 flex items-center justify-center rounded-sm bg-surface-container-high border border-outline-variant/10 text-on-surface-variant hover:text-primary hover:border-primary/40 transition-all disabled:opacity-30 disabled:cursor-not-allowed text-[10px] sm:text-[11px] font-black uppercase"
          >
            < Trước
          </button>
          <template v-for="(p, i) in pageNumbers" :key="i">
            <span v-if="p === '…'" class="px-1 sm:px-2 text-on-surface-variant/50 text-xs">…</span>
            <button
              v-else
              @click="goToPage(p)"
              :class="p === page ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:text-primary'"
              class="w-8 sm:w-9 h-8 sm:h-9 flex items-center justify-center rounded-sm border border-outline-variant/10 text-[10px] sm:text-[11px] font-black transition-all"
            >{{ p }}</button>
          </template>
          <button
            @click="goToPage(page + 1)"
            :disabled="page === totalPages"
            class="px-2.5 sm:px-3 h-8 sm:h-9 flex items-center justify-center rounded-sm bg-surface-container-high border border-outline-variant/10 text-on-surface-variant hover:text-primary hover:border-primary/40 transition-all disabled:opacity-30 disabled:cursor-not-allowed text-[10px] sm:text-[11px] font-black uppercase"
          >
            Sau >
          </button>
        </div>
      </div>
    </section>

    <!-- Không có kết quả -->
    <section v-else-if="hasSearched" class="flex flex-col items-center justify-center py-14 sm:py-20 px-4 bg-surface-container-low rounded-2xl border border-outline-variant/5 text-center">
      <div class="mb-6 sm:mb-8 inline-flex items-center justify-center w-20 h-20 sm:w-28 sm:h-28 rounded-full bg-surface-container-high border border-outline-variant/10 shadow-2xl">
        <span class="material-symbols-outlined text-4xl sm:text-5xl text-yellow-500/50" style="font-variation-settings: 'FILL' 1;">movie_filter</span>
      </div>
      <h2 class="font-headline text-xl sm:text-2xl md:text-3xl font-bold mb-2 sm:mb-3">Không tìm thấy phim</h2>
      <p class="text-neutral-400 mb-6 sm:mb-9 leading-relaxed text-xs sm:text-sm md:text-base max-w-lg">Không có kết quả nào cho "<span class="text-on-surface font-bold">{{ query }}</span>". Hãy thử một từ khoá khác xem sao.</p>
      <button @click="resetSearch" class="bg-surface-container-highest hover:bg-surface-bright text-primary border border-outline-variant/20 px-6 sm:px-8 py-2.5 sm:py-3 rounded-lg font-bold tracking-widest uppercase text-xs transition-all">Đặt lại tìm kiếm</button>
    </section>

    <!-- Trạng thái ban đầu: gợi ý phim đang chiếu -->
    <section v-else>
      <div class="flex items-center gap-3 mb-6 sm:mb-8">
        <span class="material-symbols-outlined text-primary-container text-xl sm:text-2xl">local_fire_department</span>
        <h2 class="text-lg sm:text-xl md:text-2xl font-headline font-extrabold tracking-tight text-on-surface uppercase">Có thể bạn quan tâm</h2>
      </div>

      <!-- Loading gợi ý -->
      <div v-if="isLoadingSuggested" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3.5 sm:gap-6">
        <div v-for="i in 15" :key="i" class="animate-pulse">
          <div class="aspect-[2/3] mb-4 rounded-xl bg-surface-container-high"></div>
          <div class="h-4 bg-surface-container-high rounded w-3/4"></div>
        </div>
      </div>

      <div v-else-if="suggested.length" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3.5 sm:gap-6">
        <MovieCard v-for="m in suggested" :key="m.id" :movie="m" />
      </div>

      <!-- Không có phim gợi ý -->
      <div v-else class="text-center py-16 text-on-surface-variant">
        <span class="material-symbols-outlined text-5xl text-outline-variant mb-3 block">search</span>
        <p class="text-sm">Nhập từ khoá để tìm kiếm phim trong hệ thống DevCine.</p>
      </div>
    </section>
  </main>
</template>
