<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useBookingStore } from '@/stores/booking'
import { showtimeApi } from '@/api/customer'
import api from '@/api/axios'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()

const router = useRouter()
const store = useBookingStore()

// ===== Dải ngày (7 ngày tới) =====
const availableDates = computed(() => {
  const dates = []
  const today = new Date()
  const dow = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
  for (let i = 0; i < 7; i++) {
    const d = new Date(today); d.setDate(today.getDate() + i)
    const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    dates.push({
      dateStr,
      label: i === 0 ? 'HÔM NAY' : dow[d.getDay()],
      dayNum: String(d.getDate()).padStart(2, '0'),
      monthInt: d.getMonth() + 1
    })
  }
  return dates
})
const todayStr = availableDates.value[0].dateStr
const selectedDate = ref(todayStr)

// ===== Dữ liệu =====
const loading = ref(false)
const loadError = ref(false)
const allShowtimes = ref([])
const cinemas = ref([])               // danh sách rạp (lấy city/ảnh từ /v1/cinemas)
const selectedCinemaId = ref(null)    // rạp đang chọn — null = chưa chọn (hiện màn chọn rạp)
const cityFilter = ref('Tất cả')      // lọc theo tỉnh/thành ở màn chọn rạp
const cinemaSearch = ref('')          // ô tìm rạp theo tên/địa chỉ

// ===== Helpers =====
const toDate = (dt) => {
  if (!dt) return null
  if (typeof dt === 'string') return new Date(dt)
  if (Array.isArray(dt)) return new Date(dt[0], (dt[1] || 1) - 1, dt[2] || 1, dt[3] || 0, dt[4] || 0)
  return null
}
const dateKey = (dt) => {
  const d = toDate(dt)
  return d ? `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` : ''
}
const getTimeString = (dt) => {
  const d = toDate(dt)
  return d ? `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}` : ''
}
const formatReleaseDate = (dt) => {
  const d = toDate(dt)
  return d ? `${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}` : ''
}
const ageDescription = (rating) => {
  if (!rating) return ''
  const r = String(rating).toUpperCase()
  const m = r.match(/(\d+)/)
  if (m) return `* Phim được phổ biến đến người xem từ độ tuổi ${m[1]} trở lên`
  if (r === 'P') return '* Phim được phép phổ biến đến mọi độ tuổi'
  if (r === 'K') return '* Phim phù hợp khán giả dưới 13 tuổi khi có người lớn đi cùng'
  return ''
}

// Gom suất chiếu theo phim cho ngày đã chọn (gộp mọi rạp/định dạng vào 1 hàng giờ)
const moviesForDate = computed(() => {
  const map = new Map()
  for (const s of allShowtimes.value) {
    if (selectedCinemaId.value && s.cinemaId !== selectedCinemaId.value) continue
    if (dateKey(s.startTime) !== selectedDate.value) continue
    if (!map.has(s.movieId)) {
      map.set(s.movieId, {
        id: s.movieId, title: s.movieTitle, posterUrl: s.moviePosterUrl, ageRating: s.movieAgeRating,
        durationMins: s.movieDurationMins, country: s.movieCountry, releaseDate: s.movieReleaseDate,
        genres: s.movieGenres && s.movieGenres.length ? Array.from(s.movieGenres).join(', ') : '',
        formatSet: new Set(), showtimes: []
      })
    }
    const m = map.get(s.movieId)
    if (s.formatName) m.formatSet.add(s.formatName)
    const d = toDate(s.startTime)
    m.showtimes.push({ id: s.id, time: getTimeString(s.startTime), roomName: s.roomName, roomTypeName: s.roomTypeName, sort: d ? d.getTime() : 0, past: selectedDate.value === todayStr && d && d.getTime() < Date.now(), raw: s })
  }
  return Array.from(map.values()).map(m => ({
    ...m,
    formats: Array.from(m.formatSet),
    showtimes: m.showtimes.sort((a, b) => a.sort - b.sort)
  }))
})

// ===== Bước chọn rạp =====
// Chi tiết rạp (city/ảnh) theo id từ /v1/cinemas
const cinemaDetailById = computed(() => {
  const m = new Map()
  for (const c of cinemas.value) m.set(c.id, c)
  return m
})

// Chỉ những rạp ĐANG CÓ suất chiếu (distinct theo cinemaId trong allShowtimes),
// bổ sung city/ảnh từ /v1/cinemas (fallback nếu thiếu).
const availableCinemas = computed(() => {
  const seen = new Map()
  for (const s of allShowtimes.value) {
    if (seen.has(s.cinemaId)) continue
    const detail = cinemaDetailById.value.get(s.cinemaId)
    seen.set(s.cinemaId, {
      id: s.cinemaId,
      name: detail?.name || s.cinemaName,
      address: detail?.address || s.cinemaAddress,
      city: detail?.city || 'Khác',
      imageUrl: detail?.imageUrl || ''
    })
  }
  return Array.from(seen.values())
})

// Danh sách tỉnh/thành (kèm 'Tất cả') cho hàng chip lọc
const cityOptions = computed(() => {
  const set = new Set(availableCinemas.value.map(c => c.city))
  return ['Tất cả', ...Array.from(set)]
})

// Chuẩn hoá để tìm không dấu (vd "ho chi minh" khớp "Hồ Chí Minh")
const norm = (s) => (s || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd')

// Rạp sau khi lọc theo tỉnh/thành + ô tìm kiếm (tên/địa chỉ/tỉnh-thành; không dấu; client-side)
const filteredCinemas = computed(() => {
  const q = norm(cinemaSearch.value.trim())
  return availableCinemas.value.filter(c => {
    const matchCity = cityFilter.value === 'Tất cả' || c.city === cityFilter.value
    const matchSearch = !q || norm(c.name).includes(q) || norm(c.address).includes(q) || norm(c.city).includes(q)
    return matchCity && matchSearch
  })
})

const selectedCinema = computed(() => availableCinemas.value.find(c => c.id === selectedCinemaId.value) || null)

const selectCinema = (id) => {
  selectedCinemaId.value = id
  selectedDate.value = todayStr
}
const changeCinema = () => { selectedCinemaId.value = null }

const goToBooking = (st) => {
  if (st.past) return
  const s = st.raw
  store.setMovie({ id: s.movieId, title: s.movieTitle, posterUrl: s.moviePosterUrl, ageRating: s.movieAgeRating, durationMins: s.movieDurationMins })
  store.setShowtime(
    { id: s.id, startTime: s.startTime, room: { id: s.roomId, name: s.roomName }, format: { id: s.formatId, name: s.formatName } },
    { id: s.cinemaId, name: s.cinemaName, cinemaName: s.cinemaName, address: s.cinemaAddress }
  )
  router.push('/booking')
}

const loadShowtimes = async () => {
  loading.value = true; loadError.value = false
  try {
    const [stRes, cinemaRes] = await Promise.all([
      showtimeApi.getUpcoming(),
      api.get('/v1/cinemas').catch(() => ({ data: [] }))  // city/ảnh — lỗi cũng không chặn
    ])
    allShowtimes.value = stRes.data || []
    cinemas.value = cinemaRes.data || []
  } catch (e) {
    loadError.value = true
    toast.error(friendlyError(e, 'Không tải được lịch chiếu.'))
  } finally { loading.value = false }
}

onMounted(loadShowtimes)
</script>

<template>
  <main class="pt-32 pb-20 max-w-7xl mx-auto px-6 md:px-10">
    <header class="mb-8 text-center flex flex-col items-center">
      <h1 class="font-headline text-4xl md:text-5xl font-extrabold tracking-tight text-on-surface mb-5">Lịch chiếu</h1>
      <div class="w-3/4 max-w-md h-[1px] bg-gradient-to-r from-transparent via-[#f5c518]/50 to-transparent"></div>
    </header>

    <!-- Loading ban đầu -->
    <div v-if="loading" class="grid grid-cols-1 2xl:grid-cols-2 gap-6">
      <div v-for="i in 4" :key="i" class="flex gap-5 p-5 bg-surface-container-low rounded-2xl animate-pulse">
        <div class="w-[120px] aspect-[2/3] bg-white/5 rounded-lg shrink-0"></div>
        <div class="flex-1 space-y-3 py-2">
          <div class="h-5 bg-white/5 rounded w-2/3"></div>
          <div class="h-3 bg-white/5 rounded w-1/3"></div>
          <div class="h-3 bg-white/5 rounded w-1/2"></div>
          <div class="h-9 bg-white/5 rounded w-3/4 mt-8"></div>
        </div>
      </div>
    </div>

    <!-- Error -->
    <div v-else-if="loadError" class="text-center py-16 bg-surface-container-low rounded-2xl">
      <span class="material-symbols-outlined text-5xl text-error mb-3 block">error</span>
      <p class="text-on-surface-variant mb-4">Không tải được lịch chiếu.</p>
      <button @click="loadShowtimes" class="px-5 py-2 bg-[#f5c518] text-black rounded-lg font-bold">Thử lại</button>
    </div>

    <!-- ===== BƯỚC 1: Chọn rạp (chưa chọn rạp) ===== -->
    <section v-else-if="!selectedCinemaId">
      <!-- Không có rạp nào đang có suất chiếu -->
      <div v-if="!availableCinemas.length" class="text-center py-20 bg-surface-container-low rounded-2xl">
        <span class="material-symbols-outlined text-on-surface-variant/50 text-6xl mb-4 block">theaters</span>
        <h3 class="font-headline text-2xl font-bold text-on-surface">Chưa có rạp nào mở suất chiếu</h3>
        <p class="text-on-surface-variant mt-2">Vui lòng quay lại sau nhé.</p>
      </div>

      <div v-else>
        <p class="text-center text-on-surface-variant mb-8 flex items-center justify-center gap-2">
          <span class="material-symbols-outlined text-[#f5c518]">location_on</span>
          Vui lòng chọn rạp bạn muốn xem lịch chiếu
        </p>

        <!-- Bộ lọc: chip Tỉnh/TP + ô tìm rạp -->
        <div class="flex flex-col lg:flex-row lg:items-center gap-4 mb-8">
          <div class="flex flex-wrap gap-2 flex-1">
            <button v-for="city in cityOptions" :key="city" @click="cityFilter = city"
              :class="cityFilter === city
                ? 'bg-[#f5c518] text-black border-transparent'
                : 'bg-surface-container-high/50 text-on-surface-variant border-outline-variant/20 hover:border-[#f5c518]/50 hover:text-on-surface'"
              class="px-4 py-2 rounded-full border text-xs font-bold uppercase tracking-widest transition-all">
              {{ city }}
            </button>
          </div>
          <div class="relative lg:w-72 shrink-0">
            <span class="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-lg pointer-events-none">search</span>
            <input v-model="cinemaSearch" type="text" placeholder="Tìm theo tên rạp hoặc tỉnh/thành..."
              class="w-full bg-surface-container-high/50 border border-outline-variant/20 text-sm text-on-surface pl-11 pr-9 py-2.5 rounded-full outline-none focus:border-[#f5c518]/50 transition-all placeholder:text-on-surface-variant/40" />
            <button v-if="cinemaSearch" @click="cinemaSearch = ''"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant/60 hover:text-on-surface">
              <span class="material-symbols-outlined text-lg">close</span>
            </button>
          </div>
        </div>

        <!-- Lưới rạp (đã lọc) -->
        <div v-if="filteredCinemas.length" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          <button v-for="c in filteredCinemas" :key="c.id" @click="selectCinema(c.id)"
            class="group relative text-left flex flex-col p-6 pl-7 overflow-hidden rounded-2xl border border-outline-variant/15 bg-surface-container-low hover:border-[#f5c518]/50 hover:-translate-y-1 hover:shadow-xl hover:shadow-black/30 transition-all duration-300">
            <!-- Thanh nhấn dọc bên trái -->
            <div class="absolute left-0 top-0 bottom-0 w-1 bg-gradient-to-b from-[#f5c518] to-[#e0a000] opacity-40 group-hover:opacity-100 transition-opacity duration-300"></div>
            <!-- Vầng sáng tô điểm ở góc khi hover -->
            <div class="absolute -top-16 -right-16 w-40 h-40 rounded-full bg-[#f5c518]/5 blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none"></div>

            <!-- Chip tỉnh/thành -->
            <span class="relative self-start inline-flex items-center gap-1.5 bg-[#f5c518]/10 text-[#f5c518] text-[10px] font-bold px-3 py-1 rounded-full uppercase tracking-wider border border-[#f5c518]/20 mb-4">
              <span class="material-symbols-outlined text-[13px]">location_city</span>{{ c.city }}
            </span>

            <!-- Tên rạp -->
            <h3 class="relative font-headline text-lg font-bold text-on-surface mb-2.5 leading-snug group-hover:text-[#f5c518] transition-colors">{{ c.name }}</h3>

            <!-- Địa chỉ -->
            <div class="relative flex items-start gap-2 flex-1 mb-5">
              <span class="material-symbols-outlined text-base text-on-surface-variant/50 mt-0.5 shrink-0">location_on</span>
              <p class="text-xs text-on-surface-variant leading-relaxed line-clamp-2">{{ c.address }}</p>
            </div>

            <!-- CTA -->
            <div class="relative flex items-center justify-between pt-4 border-t border-outline-variant/10">
              <span class="text-[11px] font-bold uppercase tracking-widest text-[#f5c518]">Xem lịch chiếu</span>
              <span class="w-8 h-8 flex items-center justify-center rounded-full bg-[#f5c518]/10 text-[#f5c518] group-hover:bg-[#f5c518] group-hover:text-black transition-all duration-300">
                <span class="material-symbols-outlined text-lg group-hover:translate-x-0.5 transition-transform">arrow_forward</span>
              </span>
            </div>
          </button>
        </div>

        <!-- Không khớp bộ lọc -->
        <div v-else class="text-center py-16 bg-surface-container-low rounded-2xl">
          <span class="material-symbols-outlined text-on-surface-variant/50 text-5xl mb-3 block">search_off</span>
          <p class="text-on-surface-variant">Không tìm thấy rạp phù hợp.</p>
          <button @click="cityFilter = 'Tất cả'; cinemaSearch = ''" class="mt-3 text-[#f5c518] font-bold text-sm hover:underline">Xoá bộ lọc</button>
        </div>
      </div>
    </section>

    <!-- ===== BƯỚC 2: Đã chọn rạp — giao diện lịch chiếu ===== -->
    <template v-else>
      <!-- Thanh rạp đã chọn + đổi rạp -->
      <div class="flex items-center justify-between gap-4 mb-8 p-4 rounded-xl border border-[#f5c518]/20 bg-[#f5c518]/5">
        <div class="flex items-center gap-3 min-w-0">
          <span class="material-symbols-outlined text-[#f5c518] shrink-0">location_on</span>
          <div class="min-w-0">
            <p class="font-headline font-bold text-on-surface truncate">{{ selectedCinema?.name }}</p>
            <p v-if="selectedCinema?.address" class="text-xs text-on-surface-variant truncate">{{ selectedCinema.address }}</p>
          </div>
        </div>
        <button @click="changeCinema"
          class="shrink-0 flex items-center gap-1.5 px-4 py-2 rounded-lg border border-outline-variant/30 text-on-surface-variant text-xs font-bold uppercase tracking-widest hover:border-[#f5c518] hover:text-[#f5c518] transition-all">
          <span class="material-symbols-outlined text-sm">sync_alt</span> Đổi rạp
        </button>
      </div>

      <!-- Dải ngày -->
      <div class="flex justify-center gap-2.5 overflow-x-auto no-scrollbar mb-10 px-1 py-2">
        <button v-for="d in availableDates" :key="d.dateStr" @click="selectedDate = d.dateStr"
          :class="selectedDate === d.dateStr
            ? 'bg-gradient-to-b from-[#f5c518] to-[#e0a000] text-black border-transparent shadow-lg shadow-[#f5c518]/25 scale-105'
            : 'bg-surface-container-high/50 text-on-surface-variant border-outline-variant/20 hover:border-[#f5c518]/50 hover:text-on-surface hover:-translate-y-0.5'"
          class="flex flex-col items-center justify-center flex-shrink-0 w-[68px] py-3 rounded-xl border transition-all duration-200">
          <span class="text-[10px] font-bold uppercase tracking-wider"
            :class="selectedDate === d.dateStr ? 'text-black/65' : 'text-on-surface-variant/80'">{{ d.label }}</span>
          <span class="text-2xl font-extrabold leading-none my-1">{{ d.dayNum }}</span>
          <span class="text-[10px] font-semibold"
            :class="selectedDate === d.dateStr ? 'text-black/65' : 'text-on-surface-variant/60'">Th{{ d.monthInt }}</span>
        </button>
      </div>

      <!-- Empty -->
      <div v-if="!moviesForDate.length" class="text-center py-20 bg-surface-container-low rounded-2xl">
        <span class="material-symbols-outlined text-on-surface-variant/50 text-6xl mb-4 block">event_busy</span>
        <h3 class="font-headline text-2xl font-bold text-on-surface">Không có suất chiếu</h3>
        <p class="text-on-surface-variant mt-2">Rạp này chưa có phim nào chiếu trong ngày này. Thử chọn ngày khác.</p>
      </div>

      <!-- Danh sách phim -->
      <div v-else class="grid grid-cols-1 2xl:grid-cols-2 gap-6">
        <div v-for="movie in moviesForDate" :key="movie.id"
          class="flex gap-5 p-5 border border-outline-variant/10 bg-surface-container-low rounded-2xl hover:border-[#f5c518]/30 transition-all">
          <div class="w-32 sm:w-36 md:w-40 flex-shrink-0 aspect-[2/3] bg-surface-container-high rounded-xl overflow-hidden">
            <img :src="movie.posterUrl || '/images/Hopper.webp'" :alt="movie.title" class="w-full h-full object-cover rounded-xl" />
          </div>
          <div class="flex-1 min-w-0 flex flex-col">
            <div class="flex items-start justify-between gap-2 mb-1.5">
              <span v-if="movie.durationMins" class="text-xs text-on-surface-variant">{{ movie.durationMins }} phút</span>
              <div class="flex flex-wrap gap-1 justify-end">
                <span v-for="f in movie.formats" :key="f" class="bg-surface-container-high border border-outline-variant/30 text-on-surface-variant text-[10px] font-bold px-2 py-0.5 rounded">{{ f }}</span>
              </div>
            </div>
            <h3 class="font-headline text-lg font-bold text-on-surface uppercase leading-tight mb-2">
              {{ movie.title }} <span v-if="movie.ageRating" class="text-on-surface-variant">- {{ movie.ageRating }}</span>
            </h3>
            <p v-if="movie.country" class="text-xs text-on-surface-variant">Xuất xứ: <span class="text-on-surface">{{ movie.country }}</span></p>
            <p v-if="movie.releaseDate" class="text-xs text-on-surface-variant mt-0.5">Khởi chiếu: <span class="text-on-surface">{{ formatReleaseDate(movie.releaseDate) }}</span></p>
            <p v-if="ageDescription(movie.ageRating)" class="text-[11px] italic text-on-surface-variant/80 mt-2">{{ ageDescription(movie.ageRating) }}</p>

            <div class="mt-auto border-t border-outline-variant/10 pt-3">
              <p class="text-xs font-bold text-on-surface mb-2">Lịch chiếu</p>
              <div class="flex flex-wrap gap-2">
                <button v-for="st in movie.showtimes" :key="st.id" @click="goToBooking(st)" :disabled="st.past"
                  :class="st.past ? 'border-outline-variant/20 grayscale cursor-not-allowed' : 'border-outline-variant/30 hover:border-[#f5c518] hover:bg-[#f5c518]/10 group'"
                  class="relative flex flex-col items-center justify-center min-w-[95px] px-3 py-2 border bg-surface-container-high/50 rounded-xl transition-all duration-300">
                  <span class="text-[9px] sm:text-[10px] font-bold uppercase tracking-wider mb-0.5 text-on-surface-variant group-hover:text-[#f5c518]/80 transition-colors line-clamp-1">
                    {{ st.roomName }}<span v-if="st.roomTypeName"> - {{ st.roomTypeName }}</span>
                  </span>
                  <span class="text-base font-extrabold text-on-surface group-hover:text-[#f5c518] transition-colors" :class="{'line-through text-on-surface-variant/40': st.past}">
                    {{ st.time }}
                  </span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </main>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
