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
const modalMovie = ref(null) // phim đang mở modal chọn suất
const movieCinemasCache = ref({}) // movieId -> [cinema {formats[]}]
const showtimesLoading = ref(false)

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
  modalMovie.value = null
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
    modalMovie.value = null
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

const openMovieModal = async (movie) => {
  modalMovie.value = movie
  if (!movieCinemasCache.value[movie.id]) {
    showtimesLoading.value = true
    try {
      const { data } = await showtimeApi.getByMovie(movie.id, selectedDate.value, selectedCity.value)
      movieCinemasCache.value = { ...movieCinemasCache.value, [movie.id]: groupByCinema(data) }
    } catch (e) { movieCinemasCache.value = { ...movieCinemasCache.value, [movie.id]: [] } } finally { showtimesLoading.value = false }
  }
}
const closeModal = () => { modalMovie.value = null }

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
  <main class="pt-32 pb-20 max-w-7xl mx-auto px-6 md:px-10">
    <header class="mb-10 text-center flex flex-col items-center">
      <h1 class="font-headline text-4xl md:text-5xl font-extrabold tracking-tight text-on-surface mb-5">Lịch chiếu</h1>
      <div class="w-3/4 max-w-md h-[1px] bg-gradient-to-r from-transparent via-[#f5c518]/50 to-transparent"></div>
    </header>

    <div class="flex flex-col lg:flex-row gap-8">
      <!-- ===== SIDEBAR ĐIỀU KHIỂN ===== -->
      <aside class="lg:w-80 shrink-0">
        <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl p-5 space-y-5 lg:sticky lg:top-28">
          <!-- Toggle chế độ -->
          <div class="grid grid-cols-2 gap-1 bg-surface-container-high rounded-xl p-1">
            <button @click="mode = 'movie'" :class="mode === 'movie' ? 'bg-[#f5c518]/15 text-[#f5c518]' : 'text-on-surface-variant hover:text-on-surface'"
              class="py-2 rounded-lg text-sm font-bold flex items-center justify-center gap-1.5 transition-colors">
              <span class="material-symbols-outlined text-base">movie</span> Theo Phim
            </button>
            <button @click="mode = 'cinema'" :class="mode === 'cinema' ? 'bg-[#f5c518]/15 text-[#f5c518]' : 'text-on-surface-variant hover:text-on-surface'"
              class="py-2 rounded-lg text-sm font-bold flex items-center justify-center gap-1.5 transition-colors">
              <span class="material-symbols-outlined text-base">theaters</span> Theo Rạp
            </button>
          </div>

          <!-- Khu vực -->
          <div>
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant block mb-1.5">Khu vực</label>
            <select v-model="selectedCity" class="w-full py-2.5 px-3 bg-surface-container-high border border-outline-variant/30 text-on-surface rounded-lg outline-none focus:border-[#f5c518] text-sm font-bold cursor-pointer">
              <option value="">Tất cả khu vực</option>
              <option v-for="c in cities" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>

          <!-- Theo Phim: tìm phim -->
          <div v-if="mode === 'movie'">
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant block mb-1.5">Tìm phim</label>
            <input v-model="movieQuery" type="text" placeholder="Nhập tên phim..."
              class="w-full py-2.5 px-3 bg-surface-container-high border border-outline-variant/30 text-on-surface rounded-lg outline-none focus:border-[#f5c518] text-sm" />
          </div>

          <!-- Theo Rạp: chọn rạp -->
          <div v-else>
            <label class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant block mb-1.5">Chọn rạp</label>
            <input v-model="cinemaQuery" type="text" placeholder="Tìm rạp..."
              class="w-full mb-3 py-2.5 px-3 bg-surface-container-high border border-outline-variant/30 text-on-surface rounded-lg outline-none focus:border-[#f5c518] text-sm" />
            <div class="space-y-4 max-h-[50vh] overflow-y-auto no-scrollbar pr-1">
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
        </div>
      </aside>

      <!-- ===== NỘI DUNG ===== -->
      <div class="flex-1 min-w-0">
        <!-- Bộ chọn ngày -->
        <div class="flex gap-2 overflow-x-auto no-scrollbar pb-2 mb-6">
          <button v-for="d in availableDates" :key="d.dateStr" @click="selectedDate = d.dateStr"
            :class="selectedDate === d.dateStr ? 'bg-[#f5c518]/15 text-[#f5c518] border-[#f5c518]' : 'bg-surface-container-low text-on-surface/80 border-outline-variant/30 hover:border-[#f5c518]/50 hover:text-[#f5c518]'"
            class="flex flex-col items-center justify-center flex-shrink-0 w-[62px] py-2 rounded-lg border transition-all">
            <span class="text-[10px] font-bold tracking-wider">{{ d.label }}</span>
            <span class="text-sm font-extrabold">{{ d.dayNum }}/{{ d.monthNum }}</span>
          </button>
        </div>

        <!-- ===== THEO PHIM ===== -->
        <template v-if="mode === 'movie'">
          <div v-if="moviesLoading" class="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-5">
            <div v-for="i in 8" :key="i" class="animate-pulse"><div class="aspect-[2/3] bg-white/5 rounded-2xl mb-3"></div><div class="h-4 bg-white/5 rounded w-3/4"></div></div>
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
          <div v-else>
            <div class="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-5">
              <div v-for="movie in movies" :key="movie.id" @click="openMovieModal(movie)" class="group cursor-pointer">
                <div class="relative aspect-[2/3] overflow-hidden rounded-2xl mb-3 border border-white/5 shadow-xl glass-shine-edge">
                  <img :src="movie.posterUrl || '/images/Hopper.webp'" :alt="movie.title" class="w-full h-full object-cover transition-all duration-500 group-hover:scale-110 group-hover:brightness-110" />
                  <span v-if="movie.ageRating" class="absolute top-3 left-3 bg-error-container text-white text-[10px] font-bold px-2 py-1 rounded">{{ movie.ageRating }}</span>
                  <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-end justify-center pb-5">
                    <span class="px-4 py-2 bg-[#f5c518] text-black text-xs font-bold rounded-full flex items-center gap-1.5 shadow-lg"><span class="material-symbols-outlined text-sm">confirmation_number</span> Đặt vé</span>
                  </div>
                </div>
                <h3 class="font-bold text-on-surface text-sm leading-tight line-clamp-1 group-hover:text-[#f5c518] transition-colors">{{ movie.title }}</h3>
                <p class="text-xs text-on-surface-variant mt-1 line-clamp-1">
                  <span v-if="movie.genres">{{ Array.isArray(movie.genres) ? movie.genres.join(', ') : movie.genres }}</span>
                  <span v-if="movie.durationMins"> · {{ movie.durationMins }}'</span>
                </p>
              </div>
            </div>
            <div v-if="movieTotalPages > 1" class="flex items-center justify-center gap-2 pt-10">
              <button @click="moviePage > 0 && moviePage--" :disabled="moviePage === 0" class="px-3 py-2 rounded-lg border border-outline-variant/30 disabled:opacity-30 hover:border-[#f5c518]"><span class="material-symbols-outlined text-base">chevron_left</span></button>
              <span class="text-sm font-bold text-on-surface-variant px-3">Trang {{ moviePage + 1 }}/{{ movieTotalPages }}</span>
              <button @click="moviePage < movieTotalPages - 1 && moviePage++" :disabled="moviePage >= movieTotalPages - 1" class="px-3 py-2 rounded-lg border border-outline-variant/30 disabled:opacity-30 hover:border-[#f5c518]"><span class="material-symbols-outlined text-base">chevron_right</span></button>
            </div>
          </div>
        </template>

        <!-- ===== THEO RẠP ===== -->
        <template v-else>
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
              <div class="w-[110px] lg:w-[130px] shrink-0 aspect-[2/3] rounded-lg overflow-hidden bg-surface-container-high">
                <img :src="movie.posterUrl || '/images/Hopper.webp'" :alt="movie.title" class="w-full h-full object-cover" />
              </div>
              <div class="flex-1 min-w-0 flex flex-col">
                <div class="flex items-center text-xs text-on-surface-variant mb-1.5 gap-2">
                  <span v-if="movie.genres">{{ movie.genres }}</span>
                  <span v-if="movie.genres && movie.duration">|</span>
                  <span v-if="movie.duration">{{ movie.duration }} phút</span>
                </div>
                <h3 class="font-headline text-lg font-bold text-on-surface uppercase leading-tight mb-2">
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
        </template>
      </div>
    </div>

    <!-- Modal chọn suất (theo phim) -->
    <transition name="fade">
      <div v-if="modalMovie" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" @click="closeModal"></div>
        <div class="relative bg-surface-container-high border border-outline-variant/20 rounded-2xl w-full max-w-2xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden">
          <!-- Header phim -->
          <div class="flex gap-4 p-5 border-b border-outline-variant/10">
            <div class="w-[70px] aspect-[2/3] rounded-lg overflow-hidden bg-surface-container-highest shrink-0">
              <img :src="modalMovie.posterUrl || '/images/Hopper.webp'" :alt="modalMovie.title" class="w-full h-full object-cover" />
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="font-headline text-lg sm:text-xl font-bold text-on-surface uppercase leading-tight">
                {{ modalMovie.title }} <span v-if="modalMovie.ageRating" class="text-on-surface-variant">- {{ modalMovie.ageRating }}</span>
              </h3>
              <p class="text-xs text-on-surface-variant mt-1.5">
                <span v-if="modalMovie.genres">{{ Array.isArray(modalMovie.genres) ? modalMovie.genres.join(', ') : modalMovie.genres }}</span>
                <span v-if="modalMovie.durationMins"> · {{ modalMovie.durationMins }} phút</span>
              </p>
              <p class="text-xs text-[#f5c518] mt-1.5 font-bold flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">calendar_month</span>
                {{ availableDates.find(d => d.dateStr === selectedDate)?.dayNum }}/{{ availableDates.find(d => d.dateStr === selectedDate)?.monthNum }}
                · {{ selectedCity || 'Tất cả khu vực' }}
              </p>
            </div>
            <button @click="closeModal" class="material-symbols-outlined text-on-surface-variant hover:text-error h-fit transition-colors">close</button>
          </div>

          <!-- Body: rạp -> giờ -->
          <div class="p-5 overflow-y-auto">
            <div v-if="showtimesLoading && !movieCinemasCache[modalMovie.id]" class="text-center py-10 text-on-surface-variant text-sm">
              <span class="material-symbols-outlined animate-spin block text-3xl mb-2">sync</span> Đang tải suất chiếu...
            </div>
            <div v-else-if="!(movieCinemasCache[modalMovie.id] || []).length" class="text-center py-10 text-on-surface-variant text-sm">
              <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 block mb-2">event_busy</span>
              Không có suất chiếu trong ngày này.
            </div>
            <div v-else class="space-y-5">
              <div v-for="c in movieCinemasCache[modalMovie.id]" :key="c.id" class="border border-outline-variant/10 rounded-xl p-4">
                <div class="flex items-center gap-2 mb-1">
                  <span class="w-2 h-2 rounded-full bg-error"></span>
                  <h4 class="font-bold text-on-surface">{{ c.name }}</h4>
                </div>
                <p v-if="c.address" class="text-xs text-on-surface-variant mb-3 ml-4">{{ c.address }}</p>
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
      </div>
    </transition>
  </main>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
