<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useBookingStore } from '@/stores/booking'
import { showtimeApi } from '@/api/customer'

const router = useRouter()
const store = useBookingStore()

const mode = ref('movie') // 'movie' = Theo Phim · 'cinema' = Theo Rạp

// ===== Bộ lọc chung =====
const cities = ref([])
const selectedCity = ref('') // '' = tất cả khu vực

const availableDates = computed(() => {
  const dates = []
  const today = new Date()
  const dow = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
  for (let i = 0; i < 7; i++) {
    const d = new Date(today); d.setDate(today.getDate() + i)
    const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    dates.push({ dateStr, label: i === 0 ? 'HÔM NAY' : dow[d.getDay()], dayNum: String(d.getDate()).padStart(2, '0'), monthNum: String(d.getMonth() + 1).padStart(2, '0') })
  }
  return dates
})
const selectedDate = ref(availableDates.value[0].dateStr)

const loadError = ref(false)

// ===== Theo Phim =====
const movieQuery = ref('')
const moviePage = ref(0)
const movieTotalPages = ref(1)
const movies = ref([])
const moviesLoading = ref(false)
const expandedMovieId = ref(null)
const movieCinemasCache = ref({}) // movieId -> [cinema {formats[]}]
const expandLoading = ref(false)

// ===== Theo Rạp =====
const cinemas = ref([])
const selectedCinemaId = ref('')
const cinemaQuery = ref('')
const cinemaMovies = ref([]) // [movie {formats[]}]
const cinemaLoading = ref(false)

// Danh sách rạp cho sidebar: lọc theo ô tìm + nhóm theo thành phố
const cinemasGrouped = computed(() => {
  const q = cinemaQuery.value.trim().toLowerCase()
  const list = q ? cinemas.value.filter(c => (c.name || '').toLowerCase().includes(q)) : cinemas.value
  const map = new Map()
  list.forEach(c => {
    const city = c.city || 'Khác'
    if (!map.has(city)) map.set(city, [])
    map.get(city).push(c)
  })
  return Array.from(map.entries()).map(([city, items]) => ({ city, items }))
})
const selectedCinema = computed(() => cinemas.value.find(c => c.id === selectedCinemaId.value) || null)

// ===== Helpers =====
const getTimeString = (dt) => {
  if (!dt) return ''
  if (typeof dt === 'string') { const d = new Date(dt); return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}` }
  if (Array.isArray(dt)) return `${String(dt[3]).padStart(2, '0')}:${String(dt[4]).padStart(2, '0')}`
  return ''
}
const fmtDate = (d) => d ? new Date(d).toLocaleDateString('vi-VN') : 'Đang cập nhật'

// Nhóm list PublicShowtimeDTO theo Rạp -> Định dạng -> giờ (cho chế độ Theo Phim)
const groupByCinema = (list) => {
  const map = new Map()
  list.forEach(s => {
    if (!map.has(s.cinemaId)) map.set(s.cinemaId, { id: s.cinemaId, name: s.cinemaName, address: s.cinemaAddress, formats: new Map() })
    const c = map.get(s.cinemaId)
    if (!c.formats.has(s.formatId)) c.formats.set(s.formatId, { id: s.formatId, name: s.formatName, showtimes: [] })
    c.formats.get(s.formatId).showtimes.push({ id: s.id, time: getTimeString(s.startTime), raw: s })
  })
  return Array.from(map.values()).map(c => ({ ...c, formats: Array.from(c.formats.values()).map(f => (f.showtimes.sort((a, b) => a.time.localeCompare(b.time)), f)) }))
}

// Nhóm list theo Phim -> Định dạng -> giờ (cho chế độ Theo Rạp)
const groupByMovie = (list) => {
  const map = new Map()
  list.forEach(s => {
    if (!map.has(s.movieId)) map.set(s.movieId, {
      id: s.movieId, title: s.movieTitle, posterUrl: s.moviePosterUrl, ageRating: s.movieAgeRating,
      duration: s.movieDurationMins, country: s.movieCountry, releaseDate: s.movieReleaseDate,
      genres: s.movieGenres && s.movieGenres.length ? Array.from(s.movieGenres).join(', ') : '', formats: new Map()
    })
    const m = map.get(s.movieId)
    if (!m.formats.has(s.formatId)) m.formats.set(s.formatId, { id: s.formatId, name: s.formatName, showtimes: [] })
    m.formats.get(s.formatId).showtimes.push({ id: s.id, time: getTimeString(s.startTime), raw: s })
  })
  return Array.from(map.values()).map(m => ({ ...m, formats: Array.from(m.formats.values()).map(f => (f.showtimes.sort((a, b) => a.time.localeCompare(b.time)), f)) }))
}

const goToBooking = (st) => {
  const s = st.raw
  store.setMovie({ id: s.movieId, title: s.movieTitle, posterUrl: s.moviePosterUrl, ageRating: s.movieAgeRating, durationMins: s.movieDurationMins })
  store.setShowtime(
    { id: s.id, startTime: s.startTime, room: { id: s.roomId, name: s.roomName }, format: { id: s.formatId, name: s.formatName } },
    { id: s.cinemaId, name: s.cinemaName, cinemaName: s.cinemaName, address: s.cinemaAddress }
  )
  router.push('/booking')
}

// ===== Loaders =====
const loadMovies = async () => {
  moviesLoading.value = true; loadError.value = false
  try {
    const { data } = await showtimeApi.getMovies({ city: selectedCity.value, date: selectedDate.value, q: movieQuery.value, page: moviePage.value, size: 12 })
    movies.value = data.content || []
    movieTotalPages.value = data.totalPages || 1
    expandedMovieId.value = null
  } catch (e) { console.error(e); loadError.value = true } finally { moviesLoading.value = false }
}

const loadCinemas = async () => {
  try {
    const { data } = await showtimeApi.getCinemas(selectedCity.value)
    cinemas.value = data || []
    if (!cinemas.value.some(c => c.id === selectedCinemaId.value)) selectedCinemaId.value = cinemas.value[0]?.id || ''
  } catch (e) { cinemas.value = []; selectedCinemaId.value = '' }
}

const loadCinemaShowtimes = async () => {
  if (!selectedCinemaId.value) { cinemaMovies.value = []; return }
  cinemaLoading.value = true; loadError.value = false
  try {
    const { data } = await showtimeApi.getByCinema(selectedCinemaId.value, selectedDate.value)
    cinemaMovies.value = groupByMovie(data)
  } catch (e) { console.error(e); loadError.value = true } finally { cinemaLoading.value = false }
}

const toggleMovie = async (movie) => {
  if (expandedMovieId.value === movie.id) { expandedMovieId.value = null; return }
  expandedMovieId.value = movie.id
  if (!movieCinemasCache.value[movie.id]) {
    expandLoading.value = true
    try {
      const { data } = await showtimeApi.getByMovie(movie.id, selectedDate.value, selectedCity.value)
      movieCinemasCache.value = { ...movieCinemasCache.value, [movie.id]: groupByCinema(data) }
    } catch (e) { movieCinemasCache.value = { ...movieCinemasCache.value, [movie.id]: [] } } finally { expandLoading.value = false }
  }
}

const reloadCurrent = () => {
  movieCinemasCache.value = {}
  if (mode.value === 'movie') loadMovies()
  else loadCinemaShowtimes()
}

// ===== Watchers =====
let searchTimer = null
watch(movieQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { moviePage.value = 0; loadMovies() }, 400)
})
watch(moviePage, () => loadMovies())
watch(mode, () => { moviePage.value = 0; reloadCurrent() })
watch(selectedDate, () => { moviePage.value = 0; reloadCurrent() })
watch(selectedCity, async () => {
  moviePage.value = 0
  await loadCinemas()
  reloadCurrent()
})
watch(selectedCinemaId, () => { if (mode.value === 'cinema') loadCinemaShowtimes() })

onMounted(async () => {
  try { const { data } = await showtimeApi.getCities(); cities.value = data || [] } catch (e) { cities.value = [] }
  await loadCinemas()
  loadMovies()
})
onUnmounted(() => { if (searchTimer) clearTimeout(searchTimer) })
</script>

<template>
  <main class="pt-32 pb-20 max-w-[1440px] mx-auto px-6 md:px-10">
    <header class="mb-10 text-center flex flex-col items-center">
      <h1 class="font-headline text-4xl md:text-5xl font-extrabold tracking-tight text-on-surface mb-5">Lịch chiếu</h1>
      <div class="w-3/4 max-w-md h-[1px] bg-gradient-to-r from-transparent via-[#f5c518]/50 to-transparent"></div>
    </header>

    <!-- Toggle chế độ -->
    <div class="flex justify-center mb-8">
      <div class="inline-flex bg-surface-container-low border border-outline-variant/20 rounded-full p-1">
        <button @click="mode = 'movie'" :class="mode === 'movie' ? 'bg-[#f5c518]/15 text-[#f5c518]' : 'text-on-surface-variant hover:text-on-surface'"
          class="px-6 py-2 rounded-full text-sm font-bold transition-colors flex items-center gap-2">
          <span class="material-symbols-outlined text-lg">movie</span> Theo Phim
        </button>
        <button @click="mode = 'cinema'" :class="mode === 'cinema' ? 'bg-[#f5c518]/15 text-[#f5c518]' : 'text-on-surface-variant hover:text-on-surface'"
          class="px-6 py-2 rounded-full text-sm font-bold transition-colors flex items-center gap-2">
          <span class="material-symbols-outlined text-lg">theaters</span> Theo Rạp
        </button>
      </div>
    </div>

    <!-- Thanh lọc -->
    <section class="mb-8 flex flex-wrap items-center justify-center gap-3">
      <select v-model="selectedCity" class="py-2.5 px-4 bg-surface-container-high border border-outline-variant/30 text-on-surface rounded-lg outline-none focus:border-[#f5c518] text-sm font-bold cursor-pointer">
        <option value="">Tất cả khu vực</option>
        <option v-for="c in cities" :key="c" :value="c">{{ c }}</option>
      </select>

      <input v-if="mode === 'movie'" v-model="movieQuery" type="text" placeholder="Tìm phim..."
        class="py-2.5 px-4 bg-surface-container-high border border-outline-variant/30 text-on-surface rounded-lg outline-none focus:border-[#f5c518] text-sm w-full sm:w-[240px]" />
    </section>

    <!-- Bộ chọn ngày -->
    <section class="mb-10 flex justify-center">
      <div class="flex gap-2 overflow-x-auto no-scrollbar py-2 px-1">
        <button v-for="d in availableDates" :key="d.dateStr" @click="selectedDate = d.dateStr"
          :class="selectedDate === d.dateStr ? 'bg-[#f5c518]/15 text-[#f5c518] border-[#f5c518]' : 'bg-surface-container-low text-on-surface/80 border-outline-variant/30 hover:border-[#f5c518]/50 hover:text-[#f5c518]'"
          class="flex flex-col items-center justify-center flex-shrink-0 w-[64px] py-2 rounded-lg border transition-all">
          <span class="text-[10px] font-bold tracking-wider">{{ d.label }}</span>
          <span class="text-sm font-extrabold">{{ d.dayNum }}/{{ d.monthNum }}</span>
        </button>
      </div>
    </section>

    <!-- ===== THEO PHIM ===== -->
    <div v-if="mode === 'movie'">
      <div v-if="moviesLoading" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-6">
        <div v-for="i in 8" :key="i" class="animate-pulse"><div class="aspect-[2/3] bg-white/5 rounded-xl mb-3"></div><div class="h-4 bg-white/5 rounded w-3/4"></div></div>
      </div>
      <div v-else-if="loadError" class="text-center py-16 bg-surface-container-low rounded-xl">
        <span class="material-symbols-outlined text-5xl text-error mb-3 block">error</span>
        <p class="text-on-surface-variant mb-4">Không tải được lịch chiếu.</p>
        <button @click="loadMovies" class="px-5 py-2 bg-[#f5c518] text-black rounded-lg font-bold">Thử lại</button>
      </div>
      <div v-else-if="!movies.length" class="text-center py-20 bg-surface-container-low rounded-xl">
        <span class="material-symbols-outlined text-on-surface-variant/50 text-6xl mb-4 block">event_busy</span>
        <h3 class="font-headline text-2xl font-bold text-on-surface">Không có phim nào</h3>
        <p class="text-on-surface-variant mt-2">Thử đổi khu vực hoặc ngày khác.</p>
      </div>

      <div v-else class="space-y-5">
        <div v-for="movie in movies" :key="movie.id" class="border border-outline-variant/10 bg-surface-container-low rounded-2xl overflow-hidden transition-all" :class="{ 'border-[#f5c518]/30': expandedMovieId === movie.id }">
          <!-- Hàng phim -->
          <div class="flex gap-5 p-5 cursor-pointer hover:bg-white/[0.02]" @click="toggleMovie(movie)">
            <div class="w-[90px] sm:w-[110px] shrink-0 aspect-[2/3] rounded-lg overflow-hidden bg-surface-container-high">
              <img v-if="movie.posterUrl" :src="movie.posterUrl" class="w-full h-full object-cover" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-start justify-between gap-3">
                <h3 class="font-headline text-lg sm:text-xl font-bold text-on-surface uppercase leading-tight">
                  {{ movie.title }} <span v-if="movie.ageRating" class="text-on-surface-variant">- {{ movie.ageRating }}</span>
                </h3>
                <span class="material-symbols-outlined text-on-surface-variant transition-transform shrink-0" :class="{ 'rotate-180 text-[#f5c518]': expandedMovieId === movie.id }">expand_more</span>
              </div>
              <div class="flex items-center text-xs text-on-surface-variant mt-1.5 gap-2">
                <span v-if="movie.genres">{{ Array.isArray(movie.genres) ? movie.genres.join(', ') : movie.genres }}</span>
                <span v-if="movie.genres && movie.durationMins">|</span>
                <span v-if="movie.durationMins">{{ movie.durationMins }} phút</span>
              </div>
              <p class="text-xs text-on-surface-variant mt-2">Xuất xứ: <span class="text-on-surface">{{ movie.country || 'Đang cập nhật' }}</span> · Khởi chiếu: <span class="text-on-surface">{{ fmtDate(movie.releaseDate) }}</span></p>
            </div>
          </div>

          <!-- Panel suất theo rạp -->
          <div v-if="expandedMovieId === movie.id" class="border-t border-outline-variant/10 p-5 bg-black/20">
            <div v-if="expandLoading && !movieCinemasCache[movie.id]" class="text-center py-6 text-on-surface-variant text-sm">
              <span class="material-symbols-outlined animate-spin">sync</span> Đang tải suất chiếu...
            </div>
            <div v-else-if="!(movieCinemasCache[movie.id] || []).length" class="text-center py-6 text-on-surface-variant text-sm">Không có suất chiếu trong ngày này.</div>
            <div v-else class="space-y-5">
              <div v-for="c in movieCinemasCache[movie.id]" :key="c.id">
                <div class="flex items-center gap-2 mb-2">
                  <span class="w-2 h-2 rounded-full bg-error"></span>
                  <h4 class="font-bold text-on-surface">{{ c.name }}</h4>
                </div>
                <p v-if="c.address" class="text-xs text-on-surface-variant mb-2 ml-4">{{ c.address }}</p>
                <div class="ml-4 space-y-3">
                  <div v-for="f in c.formats" :key="f.id">
                    <span v-if="c.formats.length > 1" class="text-[10px] font-bold text-on-surface-variant uppercase mb-1.5 block border-l-2 border-[#f5c518] pl-2">{{ f.name }}</span>
                    <div class="flex flex-wrap gap-2">
                      <button v-for="st in f.showtimes" :key="st.id" @click="goToBooking(st)"
                        class="px-4 py-1.5 border border-outline-variant/30 text-on-surface hover:border-[#f5c518] hover:bg-[#f5c518]/10 hover:text-[#f5c518] text-sm font-bold rounded-md transition-all">{{ st.time }}</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Phân trang -->
        <div v-if="movieTotalPages > 1" class="flex items-center justify-center gap-2 pt-4">
          <button @click="moviePage > 0 && moviePage--" :disabled="moviePage === 0" class="px-3 py-2 rounded-lg border border-outline-variant/30 disabled:opacity-30 hover:border-[#f5c518]"><span class="material-symbols-outlined text-base">chevron_left</span></button>
          <span class="text-sm font-bold text-on-surface-variant px-3">Trang {{ moviePage + 1 }}/{{ movieTotalPages }}</span>
          <button @click="moviePage < movieTotalPages - 1 && moviePage++" :disabled="moviePage >= movieTotalPages - 1" class="px-3 py-2 rounded-lg border border-outline-variant/30 disabled:opacity-30 hover:border-[#f5c518]"><span class="material-symbols-outlined text-base">chevron_right</span></button>
        </div>
      </div>
    </div>

    <!-- ===== THEO RẠP (sidebar) ===== -->
    <div v-else class="flex flex-col lg:flex-row gap-6">
      <!-- Sidebar: danh sách rạp (nhóm theo thành phố + ô tìm) -->
      <aside class="lg:w-72 shrink-0">
        <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl p-4 lg:sticky lg:top-28">
          <input v-model="cinemaQuery" type="text" placeholder="Tìm rạp..."
            class="w-full mb-3 py-2 px-3 bg-surface-container-high border border-outline-variant/30 text-on-surface rounded-lg outline-none focus:border-[#f5c518] text-sm" />
          <div class="space-y-4 max-h-[40vh] lg:max-h-[60vh] overflow-y-auto no-scrollbar pr-1">
            <div v-for="grp in cinemasGrouped" :key="grp.city">
              <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-2 px-1">{{ grp.city }}</p>
              <button v-for="c in grp.items" :key="c.id" @click="selectedCinemaId = c.id"
                :class="selectedCinemaId === c.id ? 'bg-[#f5c518]/15 border-[#f5c518]' : 'bg-surface-container-high/40 border-transparent hover:border-[#f5c518]/40'"
                class="w-full text-left px-3 py-2.5 rounded-lg border mb-2 transition-colors">
                <span class="font-bold text-sm block" :class="selectedCinemaId === c.id ? 'text-[#f5c518]' : 'text-on-surface'">{{ c.name }}</span>
                <span v-if="c.address" class="text-[11px] text-on-surface-variant line-clamp-1">{{ c.address }}</span>
              </button>
            </div>
            <p v-if="!cinemasGrouped.length" class="text-sm text-on-surface-variant text-center py-4">Không tìm thấy rạp.</p>
          </div>
        </div>
      </aside>

      <!-- Kết quả: lịch chiếu của rạp đã chọn -->
      <div class="flex-1 min-w-0">
        <div v-if="selectedCinema" class="mb-5">
          <h2 class="font-headline text-2xl font-bold text-on-surface uppercase">{{ selectedCinema.name }}</h2>
          <p v-if="selectedCinema.address" class="text-sm text-on-surface-variant mt-1">{{ selectedCinema.address }}</p>
        </div>

        <div v-if="cinemaLoading" class="space-y-4">
          <div v-for="i in 3" :key="i" class="h-40 bg-white/5 rounded-xl animate-pulse"></div>
        </div>
        <div v-else-if="loadError" class="text-center py-16 bg-surface-container-low rounded-xl">
          <span class="material-symbols-outlined text-5xl text-error mb-3 block">error</span>
          <p class="text-on-surface-variant mb-4">Không tải được lịch chiếu.</p>
          <button @click="loadCinemaShowtimes" class="px-5 py-2 bg-[#f5c518] text-black rounded-lg font-bold">Thử lại</button>
        </div>
        <div v-else-if="!selectedCinemaId" class="text-center py-20 bg-surface-container-low rounded-xl">
          <span class="material-symbols-outlined text-on-surface-variant/50 text-6xl mb-4 block">theaters</span>
          <h3 class="font-headline text-2xl font-bold text-on-surface">Chưa có rạp</h3>
          <p class="text-on-surface-variant mt-2">Khu vực này chưa có rạp.</p>
        </div>
        <div v-else-if="!cinemaMovies.length" class="text-center py-20 bg-surface-container-low rounded-xl">
          <span class="material-symbols-outlined text-on-surface-variant/50 text-6xl mb-4 block">event_busy</span>
          <h3 class="font-headline text-2xl font-bold text-on-surface">Không có suất chiếu</h3>
          <p class="text-on-surface-variant mt-2">Rạp này chưa có suất trong ngày đã chọn.</p>
        </div>

        <div v-else class="grid grid-cols-1 2xl:grid-cols-2 gap-6">
          <div v-for="movie in cinemaMovies" :key="movie.id" class="flex gap-5 p-5 border border-outline-variant/10 bg-surface-container-low rounded-xl hover:border-[#f5c518]/30 transition-all">
          <div class="w-[120px] lg:w-[150px] shrink-0 aspect-[2/3] rounded-lg overflow-hidden bg-surface-container-high">
            <img v-if="movie.posterUrl" :src="movie.posterUrl" class="w-full h-full object-cover" />
          </div>
          <div class="flex-1 min-w-0 flex flex-col">
            <div class="flex items-center text-xs text-on-surface-variant mb-1.5 gap-2">
              <span v-if="movie.genres">{{ movie.genres }}</span>
              <span v-if="movie.genres && movie.duration">|</span>
              <span v-if="movie.duration">{{ movie.duration }} phút</span>
            </div>
            <h3 class="font-headline text-lg sm:text-xl font-bold text-on-surface uppercase leading-tight mb-2">
              {{ movie.title }} <span v-if="movie.ageRating" class="text-on-surface-variant">- {{ movie.ageRating }}</span>
            </h3>
            <p class="text-xs text-on-surface-variant mb-3">Xuất xứ: <span class="text-on-surface">{{ movie.country || 'Đang cập nhật' }}</span></p>
            <div class="mt-auto border-t border-outline-variant/10 pt-3 space-y-3">
              <div v-for="f in movie.formats" :key="f.id">
                <span v-if="movie.formats.length > 1" class="text-[10px] font-bold text-on-surface-variant uppercase mb-1.5 block border-l-2 border-[#f5c518] pl-2">{{ f.name }}</span>
                <div class="flex flex-wrap gap-2">
                  <button v-for="st in f.showtimes" :key="st.id" @click="goToBooking(st)"
                    class="px-4 py-1.5 border border-outline-variant/30 text-on-surface hover:border-[#f5c518] hover:bg-[#f5c518]/10 hover:text-[#f5c518] text-sm font-bold rounded-md transition-all">{{ st.time }}</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
