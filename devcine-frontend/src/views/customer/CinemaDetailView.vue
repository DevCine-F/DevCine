<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import api from '@/api/axios'
import { useBookingStore } from '@/stores/booking'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'
import { formatHotline } from '@/utils/cinemaValidators'
import { useSeatRealtime } from '@/composables/useSeatRealtime'

const route = useRoute()
const router = useRouter()
const store = useBookingStore()
const toast = useToastStore()

const cinemaId = Number(route.params.id)
const cinema = ref(null)
const showtimes = ref([])
const loading = ref(true)
const selectedDate = ref('')

const realtime = useSeatRealtime({
  onScheduleUpdate: () => {
    fetchAll()
  }
})

// ---- Parse thời gian (backend trả mảng [y,m,d,h,min] hoặc chuỗi ISO) ----
const toDate = (dt) => {
  if (!dt) return null
  if (dt instanceof Date) return dt
  if (typeof dt === 'string') return new Date(dt)
  if (Array.isArray(dt)) return new Date(dt[0], (dt[1] || 1) - 1, dt[2] || 1, dt[3] || 0, dt[4] || 0)
  return null
}

const getDateString = (dt) => {
  if (!dt) return ''
  if (typeof dt === 'string') return dt.split('T')[0]
  if (Array.isArray(dt)) return `${dt[0]}-${String(dt[1]).padStart(2, '0')}-${String(dt[2]).padStart(2, '0')}`
  const d = toDate(dt)
  return d ? `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` : ''
}

const fmtTime = (t) => {
  const d = toDate(t)
  if (!d || isNaN(d.getTime())) return ''
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const fmtEndTime = (st) => {
  if (st.endTime) {
    const end = toDate(st.endTime)
    if (end && !isNaN(end.getTime())) {
      return `${String(end.getHours()).padStart(2, '0')}:${String(end.getMinutes()).padStart(2, '0')}`
    }
  }
  const dur = st.movieDurationMins || st.duration
  if (dur) {
    const start = toDate(st.startTime)
    if (start && !isNaN(start.getTime())) {
      const end = new Date(start.getTime() + dur * 60 * 1000)
      return `${String(end.getHours()).padStart(2, '0')}:${String(end.getMinutes()).padStart(2, '0')}`
    }
  }
  return ''
}

const isSoldOut = (st) => (st.totalSeats > 0) && (st.availableSeats <= 0)
const isLowSeats = (st) => (st.totalSeats > 0) && st.availableSeats > 0 && st.availableSeats < 10

// Mốc thời gian thực để tự động ẩn suất chiếu đã bắt đầu
const currentTime = ref(Date.now())
let timeInterval = null

const fetchAll = async () => {
  loading.value = true
  try {
    const [cRes, sRes] = await Promise.all([
      api.get(`/v1/cinemas/${cinemaId}`),
      api.get('/showtimes/upcoming')
    ])
    cinema.value = cRes.data
    showtimes.value = (sRes.data || []).filter(s => s.cinemaId === cinemaId)
    if (availableDates.value.length) {
      const stillValid = availableDates.value.some(d => d.dateStr === selectedDate.value)
      if (!stillValid) {
        const firstWithShow = availableDates.value.find(d => d.hasShowtimes)
        selectedDate.value = firstWithShow ? firstWithShow.dateStr : availableDates.value[0].dateStr
      }
    }
  } catch (e) {
    console.error('Lỗi tải chi tiết rạp', e)
    toast.error(friendlyError(e, 'Không tải được thông tin rạp.'))
  } finally {
    loading.value = false
  }
}

// Tập hợp các ngày thực tế có suất chiếu tương lai tại rạp này
const datesWithShowtimes = computed(() => {
  const set = new Set()
  showtimes.value.forEach(s => {
    const d = toDate(s.startTime)
    if (d && d.getTime() > currentTime.value) {
      const ds = getDateString(s.startTime)
      if (ds) set.add(ds)
    }
  })
  return set
})

// Chuẩn Lotte Cinema: Cố định 7 ngày liên tiếp từ Hôm nay (Hôm nay -> Hôm nay + 6 ngày)
const availableDates = computed(() => {
  const dates = []
  const today = new Date()
  const dayNames = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7']
  const pad = (n) => String(n).padStart(2, '0')

  for (let i = 0; i < 7; i++) {
    const d = new Date(today)
    d.setDate(today.getDate() + i)
    const dateStr = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

    let weekdayLabel = ''
    if (i === 0) {
      weekdayLabel = 'Hôm nay'
    } else {
      weekdayLabel = dayNames[d.getDay()]
    }

    dates.push({
      dateStr,
      weekday: weekdayLabel,
      day: `${pad(d.getDate())}/${pad(d.getMonth() + 1)}`,
      hasShowtimes: datesWithShowtimes.value.has(dateStr)
    })
  }
  return dates
})

// Nhóm suất chiếu của ngày đang chọn theo phim và theo định dạng (LotteCinema style)
const moviesOfDay = computed(() => {
  const map = new Map()
  showtimes.value
    .filter(s => {
      const matchDate = getDateString(s.startTime) === selectedDate.value
      const d = toDate(s.startTime)
      const isFuture = d && d.getTime() > currentTime.value
      return matchDate && isFuture
    })
    .forEach(s => {
      if (!map.has(s.movieId)) {
        map.set(s.movieId, {
          movieId: s.movieId,
          title: s.movieTitleVietnamese || s.movieTitle,
          posterUrl: s.moviePosterUrl,
          ageRating: s.movieAgeRating,
          durationMins: s.movieDurationMins,
          genres: s.movieGenres ? [...s.movieGenres] : [],
          hasEarlyScreening: s.status === 'Xuất chiếu sớm',
          shows: []
        })
      }
      if (s.status === 'Xuất chiếu sớm') {
        map.get(s.movieId).hasEarlyScreening = true
      }
      map.get(s.movieId).shows.push(s)
    })

  const arr = []
  for (const m of map.values()) {
    // Sắp xếp các suất chiếu theo thời gian bắt đầu
    m.shows.sort((a, b) => {
      const ta = toDate(a.startTime)?.getTime() || 0
      const tb = toDate(b.startTime)?.getTime() || 0
      return ta - tb
    })

    // Nhóm theo Định dạng (Format)
    const formatMap = new Map()
    m.shows.forEach(s => {
      const format = s.formatName || '2D Phụ Đề'
      if (!formatMap.has(format)) {
        formatMap.set(format, {
          formatName: format,
          shows: []
        })
      }
      formatMap.get(format).shows.push(s)
    })
    m.formatGroups = [...formatMap.values()].filter(g => g.shows.length > 0)
    if (m.formatGroups.length > 0) {
      arr.push(m)
    }
  }
  return arr
})

const amenitiesList = computed(() =>
  cinema.value?.amenities ? cinema.value.amenities.split(',').map(a => a.trim()).filter(Boolean) : []
)

// Ghép địa chỉ đầy đủ: số nhà, quận/huyện, tỉnh/thành (lọc phần rỗng để không dính dấu phẩy thừa).
const fullAddress = computed(() =>
  cinema.value
    ? [cinema.value.address, cinema.value.district, cinema.value.city].filter(Boolean).join(', ')
    : ''
)

// Rạp chỉ cho xem/đặt lịch khi ĐANG hoạt động. Fallback an toàn: status null/undefined coi như mở.
const isOperating = computed(() => !cinema.value?.status || cinema.value.status === 'ACTIVE')

const mapSrc = computed(() => {
  if (!cinema.value) return ''
  if (cinema.value.latitude && cinema.value.longitude) {
    return `https://maps.google.com/maps?q=${cinema.value.latitude},${cinema.value.longitude}&z=16&output=embed`
  }
  return `https://maps.google.com/maps?q=${encodeURIComponent(cinema.value.address || cinema.value.name)}&z=15&output=embed`
})

const statusLabel = (s) => ({ ACTIVE: 'Đang hoạt động', MAINTENANCE: 'Đang bảo trì', CLOSED: 'Tạm đóng cửa' }[s] || 'Đang hoạt động')

const goToBooking = (s) => {
  const d = toDate(s.startTime)
  if (d && d.getTime() <= currentTime.value) {
    toast.warning('Suất chiếu đã bắt đầu, không thể đặt vé.')
    return
  }
  if (isSoldOut(s)) return
  store.setMovie({
    id: s.movieId,
    title: s.movieTitleVietnamese || s.movieTitle,
    posterUrl: s.moviePosterUrl,
    ageRating: s.movieAgeRating,
    durationMins: s.movieDurationMins
  })
  store.setShowtime(
    {
      id: s.id,
      startTime: s.startTime,
      endTime: s.endTime,
      roomId: s.roomId,
      roomName: s.roomName,
      formatId: s.formatId,
      formatName: s.formatName,
      room: { id: s.roomId, name: s.roomName },
      format: { id: s.formatId, name: s.formatName }
    },
    { id: s.cinemaId || cinemaId, name: s.cinemaName || cinema.value?.name, cinemaName: s.cinemaName || cinema.value?.name, address: s.cinemaAddress || fullAddress.value }
  )
  router.push('/booking')
}

onMounted(() => {
  fetchAll()
  realtime.connect(null)
  timeInterval = setInterval(() => {
    currentTime.value = Date.now()
  }, 15000)
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
  realtime.disconnect()
})
</script>

<template>
  <main class="min-h-screen pt-24 sm:pt-28 pb-16 sm:pb-24 text-on-surface">
    <div class="max-w-[1440px] mx-auto px-4 sm:px-6 md:px-10">
      <RouterLink to="/he-thong-rap" class="inline-flex items-center gap-2 text-xs sm:text-sm text-on-surface-variant hover:text-primary transition-colors mb-4 sm:mb-6">
        <span class="material-symbols-outlined text-base sm:text-lg">arrow_back</span> Tất cả cụm rạp
      </RouterLink>

      <!-- Loading -->
      <div v-if="loading" class="space-y-6">
        <div class="h-72 bg-surface-container animate-pulse rounded-2xl sm:rounded-3xl"></div>
        <div class="h-96 bg-surface-container animate-pulse rounded-2xl sm:rounded-3xl"></div>
      </div>

      <template v-else-if="cinema">
        <!-- Hero banner -->
        <section class="relative rounded-2xl sm:rounded-3xl overflow-hidden mb-8 sm:mb-10 border border-outline-variant/10 min-h-[16rem] sm:min-h-[20rem] flex items-end">
          <div class="absolute inset-0 bg-surface-container-low"></div>
          <div class="absolute inset-0 bg-gradient-to-t from-black/90 via-black/60 to-black/25"></div>
          <div class="absolute left-0 top-0 bottom-0 w-1.5 bg-primary z-10"></div>

          <div class="relative z-10 pl-5 sm:pl-8 md:pl-12 pr-5 sm:pr-8 md:pr-12 py-6 sm:py-9 md:py-12 w-full">
            <div class="flex items-center gap-2.5 sm:gap-3 mb-3 sm:mb-5 flex-wrap">
              <span class="px-2.5 sm:px-3 py-0.5 sm:py-1 bg-primary/20 text-primary border border-primary/40 text-[9px] sm:text-[0.65rem] font-bold uppercase tracking-widest rounded-full backdrop-blur-sm">{{ cinema.type }}</span>
              <span class="flex items-center gap-1.5 text-[10px] sm:text-[0.7rem] font-bold uppercase tracking-widest"
                    :class="cinema.status === 'ACTIVE' || !cinema.status ? 'text-green-400' : 'text-amber-400'">
                <span class="w-2 h-2 rounded-full" :class="cinema.status === 'ACTIVE' || !cinema.status ? 'bg-green-400 animate-pulse' : 'bg-amber-400'"></span>
                {{ statusLabel(cinema.status) }}
              </span>
            </div>

            <h1 class="text-2xl sm:text-4xl md:text-6xl font-bold font-headline tracking-tight mb-2 sm:mb-4 max-w-3xl text-white drop-shadow-lg">{{ cinema.name }}</h1>

            <p class="text-white/85 text-xs sm:text-sm md:text-base flex items-center gap-1.5 sm:gap-2 max-w-2xl">
              <span class="material-symbols-outlined text-sm sm:text-base text-primary shrink-0">location_on</span> {{ fullAddress }}
            </p>

            <p v-if="cinema.description" class="text-white/60 text-xs sm:text-sm leading-relaxed mt-2 sm:mt-3 max-w-2xl line-clamp-3">{{ cinema.description }}</p>

            <!-- Thông tin nhanh -->
            <div class="flex flex-wrap items-center gap-x-4 sm:gap-x-7 gap-y-2 mt-5 sm:mt-7 pt-4 sm:pt-6 border-t border-white/15 text-xs sm:text-sm text-white/75">
              <span v-if="cinema.rooms" class="flex items-center gap-1.5 sm:gap-2">
                <span class="material-symbols-outlined text-primary text-base sm:text-lg">meeting_room</span>{{ cinema.rooms }} phòng chiếu
              </span>
              <span v-if="cinema.hotline" class="flex items-center gap-1.5 sm:gap-2">
                <span class="material-symbols-outlined text-primary text-base sm:text-lg">call</span><span class="font-mono">{{ formatHotline(cinema.hotline) }}</span>
              </span>
              <span v-if="cinema.openingTime && cinema.closingTime" class="flex items-center gap-1.5 sm:gap-2">
                <span class="material-symbols-outlined text-primary text-base sm:text-lg">schedule</span>Mở cửa: {{ cinema.openingTime }} – Suất cuối: {{ cinema.closingTime }}
              </span>
              <span class="flex items-center gap-1.5 sm:gap-2">
                <span class="material-symbols-outlined text-primary text-base sm:text-lg">event_available</span>{{ datesWithShowtimes.size }} ngày có suất chiếu
              </span>
            </div>

            <!-- Tiện ích -->
            <div v-if="amenitiesList.length" class="flex flex-wrap gap-1.5 sm:gap-2 mt-4 sm:mt-6">
              <span v-for="a in amenitiesList" :key="a" class="px-2.5 sm:px-3 py-0.5 sm:py-1 rounded-full bg-white/10 border border-white/20 text-white/90 text-[10px] sm:text-xs font-bold backdrop-blur-sm">{{ a }}</span>
            </div>
          </div>
        </section>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:gap-10">
          <!-- Lịch chiếu -->
          <section class="lg:col-span-2 order-1">
            <!-- Rạp đóng cửa / bảo trì -->
            <div v-if="!isOperating" class="flex flex-col items-center justify-center text-center py-14 sm:py-20 px-6 sm:px-8 min-h-[20rem] sm:min-h-[24rem] rounded-2xl sm:rounded-3xl border border-amber-500/20 bg-amber-500/5">
              <div class="w-16 h-16 sm:w-20 sm:h-20 rounded-full bg-amber-500/10 flex items-center justify-center mb-4 sm:mb-6">
                <span class="material-symbols-outlined text-4xl sm:text-5xl text-amber-400">sentiment_dissatisfied</span>
              </div>
              <h3 class="text-lg sm:text-xl font-bold font-headline text-on-surface mb-2 sm:mb-3">Rạp tạm ngừng phục vụ</h3>
              <p class="text-on-surface-variant/80 text-xs sm:text-sm max-w-md leading-relaxed mb-6 sm:mb-8">
                Rạp hiện đang tạm đóng cửa hoặc đang trong quá trình bảo trì. Thành thật xin lỗi quý khách vì sự bất tiện này. Vui lòng chọn một cụm rạp khác để tiếp tục trải nghiệm!
              </p>
              <RouterLink to="/he-thong-rap" class="inline-flex items-center gap-2 px-5 sm:px-6 py-2.5 sm:py-3 rounded-xl bg-primary text-on-primary font-bold text-xs sm:text-sm hover:brightness-110 transition-all">
                <span class="material-symbols-outlined text-base sm:text-lg">theaters</span> Chọn cụm rạp khác
              </RouterLink>
            </div>

            <!-- Rạp đang hoạt động -->
            <template v-else>
            <h2 class="text-xl sm:text-2xl font-bold font-headline mb-4 sm:mb-6 flex items-center gap-2.5 sm:gap-3">
              <span class="material-symbols-outlined text-primary text-xl sm:text-2xl">event</span> Lịch chiếu
            </h2>

            <div v-if="datesWithShowtimes.size === 0" class="py-16 text-center border-2 border-dashed border-outline-variant/30 rounded-2xl sm:rounded-3xl">
              <span class="material-symbols-outlined text-4xl sm:text-5xl opacity-20 mb-3 block">event_busy</span>
              <p class="text-on-surface-variant text-sm italic">Hiện chưa có suất chiếu nào tại rạp này.</p>
            </div>

            <template v-else>
              <!-- Tabs ngày (Chuẩn 7 ngày liên tiếp từ Hôm nay) -->
              <div class="flex gap-2 overflow-x-auto no-scrollbar pb-2 mb-6 sm:mb-8 touch-pan-x">
                <button
                  v-for="d in availableDates" :key="d.dateStr"
                  @click="selectedDate = d.dateStr"
                  class="relative flex flex-col items-center justify-center w-20 sm:w-24 h-16 sm:h-20 rounded-xl sm:rounded-2xl border shrink-0 transition-all"
                  :class="selectedDate === d.dateStr ? 'bg-primary text-on-primary border-primary shadow-lg shadow-primary/20' : 'border-outline-variant/20 hover:border-primary/40'"
                >
                  <span class="text-[9px] sm:text-[0.65rem] font-bold uppercase tracking-widest opacity-80">{{ d.weekday }}</span>
                  <span class="text-base sm:text-lg font-bold">{{ d.day }}</span>
                  <span v-if="selectedDate !== d.dateStr && d.hasShowtimes" class="absolute bottom-1.5 w-1 h-1 rounded-full bg-primary/70"></span>
                </button>
              </div>

              <!-- Trạng thái không có suất chiếu cho ngày được chọn -->
              <div v-if="moviesOfDay.length === 0" class="py-16 text-center border-2 border-dashed border-outline-variant/30 rounded-2xl sm:rounded-3xl">
                <span class="material-symbols-outlined text-4xl sm:text-5xl opacity-20 mb-3 block">event_busy</span>
                <p class="text-on-surface-variant text-sm italic">Không có suất chiếu nào vào ngày này. Quý khách vui lòng chọn ngày khác.</p>
              </div>

              <!-- Phim trong ngày -->
              <div v-else class="space-y-4 sm:space-y-6">
                <div v-for="m in moviesOfDay" :key="m.movieId" class="flex flex-col sm:flex-row gap-4 sm:gap-5 p-4 sm:p-5 rounded-2xl sm:rounded-3xl bg-surface-container-low border border-outline-variant/10">
                  <RouterLink :to="`/movie/${m.movieId}`" class="w-20 sm:w-24 h-28 sm:h-36 rounded-xl overflow-hidden shrink-0 bg-surface-container-high border border-white/5 self-start sm:self-auto">
                    <img v-if="m.posterUrl" :src="m.posterUrl" class="w-full h-full object-cover hover:scale-105 transition-transform" />
                    <div v-else class="w-full h-full flex items-center justify-center"><span class="material-symbols-outlined text-3xl text-outline-variant">movie</span></div>
                  </RouterLink>
                  <div class="flex-grow min-w-0">
                    <div class="flex items-center gap-2 mb-1 flex-wrap">
                      <h3 class="font-bold text-base sm:text-lg">{{ m.title }}</h3>
                      <span v-if="m.ageRating" class="px-1.5 py-0.5 bg-error-container text-on-error-container text-[9px] sm:text-[0.6rem] font-bold rounded">{{ m.ageRating }}</span>
                    </div>
                    <p class="text-xs text-on-surface-variant mb-3 sm:mb-4">
                      <span v-if="m.durationMins">{{ m.durationMins }} phút</span>
                      <span v-if="m.genres.length"> • {{ m.genres.join(', ') }}</span>
                    </p>
                    
                    <div class="space-y-4 sm:space-y-6 mt-3 sm:mt-4">
                      <div
                        v-for="group in m.formatGroups"
                        :key="group.formatName"
                        class="mb-3 sm:mb-4 last:mb-0"
                      >
                        <!-- Tiêu đề định dạng -->
                        <div class="flex items-center gap-3 mb-2.5 sm:mb-3">
                          <span class="text-xs sm:text-[13px] font-bold text-gray-300 uppercase tracking-wider">{{ group.formatName }}</span>
                          <div class="flex-1 h-px bg-white/[0.08]"></div>
                        </div>

                        <!-- Danh sách suất chiếu -->
                        <div class="flex flex-wrap items-center gap-2.5 sm:gap-3">
                          <button
                            v-for="st in group.shows"
                            :key="st.id"
                            @click="goToBooking(st)"
                            :disabled="isSoldOut(st)"
                            :title="isSoldOut(st) ? 'Suất chiếu đã hết ghế' : st.roomName"
                            :class="[
                              'group flex flex-col items-center justify-center gap-1 border rounded-lg min-w-[130px] sm:min-w-[160px] flex-1 sm:flex-none min-h-[72px] sm:min-h-[82px] px-2.5 sm:px-3.5 py-2 sm:py-2.5 flex-shrink-0 transition-all duration-200',
                              isSoldOut(st)
                                ? 'border-[#2a2a2a] bg-[#161616] opacity-40 cursor-not-allowed'
                                : isLowSeats(st)
                                  ? 'border-[#8b6914]/40 bg-[#1f1b12] hover:border-[#f5c518] hover:bg-[#282214] cursor-pointer'
                                  : 'border-[#333333] bg-[#1c1c1c] hover:border-[#f5c518] hover:bg-[#201d0a] cursor-pointer'
                            ]"
                          >
                            <!-- Dòng 1: Tên phòng -->
                            <span class="block w-full text-center text-[10.5px] sm:text-[11.5px] text-gray-400 font-medium leading-tight whitespace-nowrap" :title="st.roomName">
                              {{ st.roomName }}
                            </span>

                            <!-- Dòng 2: Giờ chiếu -->
                            <div class="relative flex items-center justify-center w-full h-[22px] sm:h-[26px]">
                              <span
                                class="text-lg sm:text-xl font-bold leading-none tracking-tight tabular-nums transition-all duration-200 group-hover:opacity-0 group-hover:scale-90 absolute"
                                :class="isSoldOut(st) ? 'text-gray-500' : 'text-[#f5c518]'"
                              >
                                {{ fmtTime(st.startTime) }}
                              </span>
                              <span
                                class="text-xs sm:text-sm font-bold leading-none tracking-tight tabular-nums transition-all duration-200 opacity-0 scale-90 group-hover:opacity-100 group-hover:scale-100 text-[#f5c518]"
                                :class="isSoldOut(st) ? 'text-gray-500' : ''"
                              >
                                {{ fmtTime(st.startTime) }}<span v-if="fmtEndTime(st)" class="text-gray-400 font-normal mx-0.5">~</span>{{ fmtEndTime(st) }}
                              </span>
                            </div>

                            <!-- Dòng 3: Tình trạng ghế -->
                            <span
                              v-if="st.totalSeats > 0"
                              class="text-[10px] sm:text-[11px] font-medium leading-tight whitespace-nowrap"
                              :class="isSoldOut(st) ? 'text-gray-500' : (isLowSeats(st) ? 'text-[#f5c518]/80' : 'text-gray-400')"
                            >
                              {{ isSoldOut(st) ? 'Hết ghế' : `${st.availableSeats} / ${st.totalSeats} Ghế` }}
                            </span>
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            </template>
          </section>

          <!-- Bản đồ -->
          <aside class="order-2 lg:sticky lg:top-28 self-start">
            <div class="rounded-2xl sm:rounded-3xl overflow-hidden border border-outline-variant/10 h-72 sm:h-96 lg:h-[34rem]">
              <iframe :src="mapSrc" class="w-full h-full" style="border:0" loading="lazy" referrerpolicy="no-referrer-when-downgrade"></iframe>
            </div>
          </aside>
        </div>
      </template>

      <!-- Không tìm thấy rạp -->
      <div v-else class="py-20 text-center">
        <span class="material-symbols-outlined text-5xl opacity-20 mb-4 block">error</span>
        <p class="text-on-surface-variant mb-6">Không tìm thấy thông tin cụm rạp.</p>
        <RouterLink to="/he-thong-rap" class="text-primary font-bold">← Về danh sách rạp</RouterLink>
      </div>
    </div>
  </main>
</template>
