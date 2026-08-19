<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import api from '@/api/axios'
import { useBookingStore } from '@/stores/booking'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'
import { formatHotline } from '@/utils/cinemaValidators'

const route = useRoute()
const router = useRouter()
const store = useBookingStore()
const toast = useToastStore()

const cinemaId = Number(route.params.id)
const cinema = ref(null)
const showtimes = ref([])
const loading = ref(true)
const selectedDate = ref('')

// ---- Parse thời gian (backend trả mảng [y,m,d,h,min] hoặc chuỗi ISO) ----
const getDateString = (dt) => {
  if (!dt) return ''
  if (typeof dt === 'string') return dt.split('T')[0]
  if (Array.isArray(dt)) return `${dt[0]}-${String(dt[1]).padStart(2, '0')}-${String(dt[2]).padStart(2, '0')}`
  return new Date(dt).toISOString().split('T')[0]
}
const getTimeString = (dt) => {
  if (!dt) return ''
  if (typeof dt === 'string') {
    const d = new Date(dt)
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  }
  if (Array.isArray(dt)) return `${String(dt[3]).padStart(2, '0')}:${String(dt[4]).padStart(2, '0')}`
  return ''
}

const fetchAll = async () => {
  loading.value = true
  try {
    const [cRes, sRes] = await Promise.all([
      api.get(`/v1/cinemas/${cinemaId}`),
      api.get('/showtimes/upcoming')
    ])
    cinema.value = cRes.data
    showtimes.value = (sRes.data || []).filter(s => s.cinemaId === cinemaId)
    if (availableDates.value.length) selectedDate.value = availableDates.value[0]
  } catch (e) {
    console.error('Lỗi tải chi tiết rạp', e)
    toast.error(friendlyError(e, 'Không tải được thông tin rạp.'))
  } finally {
    loading.value = false
  }
}

const availableDates = computed(() => {
  const set = new Set(showtimes.value.map(s => getDateString(s.startTime)).filter(Boolean))
  return [...set].sort()
})

const dateLabel = (ds) => {
  const d = new Date(ds + 'T00:00:00')
  const wd = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'][d.getDay()]
  return { weekday: wd, day: `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}` }
}

// Nhóm suất chiếu của ngày đang chọn theo phim
const moviesOfDay = computed(() => {
  const map = new Map()
  showtimes.value
    .filter(s => getDateString(s.startTime) === selectedDate.value)
    .forEach(s => {
      if (!map.has(s.movieId)) {
        map.set(s.movieId, {
          movieId: s.movieId,
          title: s.movieTitleVietnamese || s.movieTitle,
          posterUrl: s.moviePosterUrl,
          ageRating: s.movieAgeRating,
          durationMins: s.movieDurationMins,
          genres: s.movieGenres ? [...s.movieGenres] : [],
          shows: []
        })
      }
      map.get(s.movieId).shows.push(s)
    })
  // sắp xếp suất theo giờ
  const arr = [...map.values()]
  arr.forEach(m => m.shows.sort((a, b) => getTimeString(a.startTime).localeCompare(getTimeString(b.startTime))))
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

const mapSrc = computed(() => {
  if (!cinema.value) return ''
  if (cinema.value.latitude && cinema.value.longitude) {
    return `https://maps.google.com/maps?q=${cinema.value.latitude},${cinema.value.longitude}&z=16&output=embed`
  }
  return `https://maps.google.com/maps?q=${encodeURIComponent(cinema.value.address || cinema.value.name)}&z=15&output=embed`
})

const statusLabel = (s) => ({ ACTIVE: 'Đang hoạt động', MAINTENANCE: 'Đang bảo trì', CLOSED: 'Tạm đóng cửa' }[s] || 'Đang hoạt động')

const goToBooking = (s) => {
  store.setMovie({
    id: s.movieId,
    title: s.movieTitle,
    posterUrl: s.moviePosterUrl,
    ageRating: s.movieAgeRating,
    durationMins: s.movieDurationMins
  })
  store.setShowtime(
    {
      id: s.id,
      startTime: s.startTime,
      roomId: s.roomId,
      roomName: s.roomName,
      formatId: s.formatId,
      formatName: s.formatName,
      room: { id: s.roomId, name: s.roomName },
      format: { id: s.formatId, name: s.formatName }
    },
    { id: s.cinemaId, name: s.cinemaName, cinemaName: s.cinemaName, address: s.cinemaAddress }
  )
  router.push('/booking')
}

onMounted(fetchAll)
</script>

<template>
  <main class="min-h-screen pt-28 pb-24 text-on-surface">
    <div class="max-w-[1440px] mx-auto px-6 md:px-10">
      <RouterLink to="/he-thong-rap" class="inline-flex items-center gap-2 text-sm text-on-surface-variant hover:text-primary transition-colors mb-6">
        <span class="material-symbols-outlined text-lg">arrow_back</span> Tất cả cụm rạp
      </RouterLink>

      <!-- Loading -->
      <div v-if="loading" class="space-y-6">
        <div class="h-72 bg-surface-container animate-pulse rounded-3xl"></div>
        <div class="h-96 bg-surface-container animate-pulse rounded-3xl"></div>
      </div>

      <template v-else-if="cinema">
        <!-- Hero banner: ảnh rạp làm nền + lớp phủ tối để chữ trắng đọc rõ -->
        <section class="relative rounded-3xl overflow-hidden mb-10 border border-outline-variant/10 min-h-[20rem] flex items-end">
          <!-- Ảnh nền (fallback nền tối nếu chưa có ảnh) -->
          <div v-if="cinema.imageUrl" class="absolute inset-0 bg-cover bg-center"
               :style="{ backgroundImage: `url(${cinema.imageUrl})` }"></div>
          <div v-else class="absolute inset-0 bg-surface-container-low"></div>

          <!-- Lớp phủ gradient tối: đậm dưới, nhạt trên -->
          <div class="absolute inset-0 bg-gradient-to-t from-black/90 via-black/60 to-black/25"></div>

          <!-- Dải vàng accent bên trái -->
          <div class="absolute left-0 top-0 bottom-0 w-1.5 bg-primary z-10"></div>

          <div class="relative z-10 pl-8 md:pl-12 pr-8 md:pr-12 py-9 md:py-12 w-full">
            <div class="flex items-center gap-3 mb-5 flex-wrap">
              <span class="px-3 py-1 bg-primary/20 text-primary border border-primary/40 text-[0.65rem] font-bold uppercase tracking-widest rounded-full backdrop-blur-sm">{{ cinema.type }}</span>
              <span class="flex items-center gap-1.5 text-[0.7rem] font-bold uppercase tracking-widest"
                    :class="cinema.status === 'ACTIVE' || !cinema.status ? 'text-green-400' : 'text-amber-400'">
                <span class="w-2 h-2 rounded-full" :class="cinema.status === 'ACTIVE' || !cinema.status ? 'bg-green-400 animate-pulse' : 'bg-amber-400'"></span>
                {{ statusLabel(cinema.status) }}
              </span>
            </div>

            <h1 class="text-4xl md:text-6xl font-bold font-headline tracking-tight mb-4 max-w-3xl text-white drop-shadow-lg">{{ cinema.name }}</h1>

            <p class="text-white/85 text-sm md:text-base flex items-center gap-2 max-w-2xl">
              <span class="material-symbols-outlined text-base text-primary">location_on</span> {{ fullAddress }}
            </p>

            <!-- Thông tin nhanh -->
            <div class="flex flex-wrap items-center gap-x-7 gap-y-2 mt-7 pt-6 border-t border-white/15 text-sm text-white/75">
              <span v-if="cinema.rooms" class="flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-lg">meeting_room</span>{{ cinema.rooms }} phòng chiếu
              </span>
              <span v-if="cinema.hotline" class="flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-lg">call</span><span class="font-mono">{{ formatHotline(cinema.hotline) }}</span>
              </span>
              <span class="flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-lg">event_available</span>{{ availableDates.length }} ngày có suất chiếu
              </span>
            </div>
          </div>
        </section>

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-10">
          <!-- Lịch chiếu -->
          <section class="lg:col-span-2 order-2 lg:order-1">
            <h2 class="text-2xl font-bold font-headline mb-6 flex items-center gap-3">
              <span class="material-symbols-outlined text-primary">event</span> Lịch chiếu
            </h2>

            <div v-if="availableDates.length === 0" class="py-16 text-center border-2 border-dashed border-outline-variant/30 rounded-3xl">
              <span class="material-symbols-outlined text-5xl opacity-20 mb-3 block">event_busy</span>
              <p class="text-on-surface-variant italic">Hiện chưa có suất chiếu nào tại rạp này.</p>
            </div>

            <template v-else>
              <!-- Tabs ngày -->
              <div class="flex gap-2 overflow-x-auto pb-2 mb-8">
                <button
                  v-for="ds in availableDates" :key="ds"
                  @click="selectedDate = ds"
                  class="flex flex-col items-center justify-center w-24 h-20 rounded-2xl border shrink-0 transition-all"
                  :class="selectedDate === ds ? 'bg-primary text-on-primary border-primary shadow-lg shadow-primary/20' : 'border-outline-variant/20 hover:border-primary/40'"
                >
                  <span class="text-[0.65rem] font-bold uppercase tracking-widest opacity-80">{{ dateLabel(ds).weekday }}</span>
                  <span class="text-lg font-bold">{{ dateLabel(ds).day }}</span>
                </button>
              </div>

              <!-- Phim trong ngày -->
              <div class="space-y-6">
                <div v-for="m in moviesOfDay" :key="m.movieId" class="flex gap-5 p-5 rounded-3xl bg-surface-container-low border border-outline-variant/10">
                  <RouterLink :to="`/movie/${m.movieId}`" class="w-24 h-36 rounded-xl overflow-hidden shrink-0 bg-surface-container-high border border-white/5">
                    <img v-if="m.posterUrl" :src="m.posterUrl" class="w-full h-full object-cover hover:scale-105 transition-transform" />
                    <div v-else class="w-full h-full flex items-center justify-center"><span class="material-symbols-outlined text-3xl text-outline-variant">movie</span></div>
                  </RouterLink>
                  <div class="flex-grow min-w-0">
                    <div class="flex items-center gap-2 mb-1 flex-wrap">
                      <h3 class="font-bold text-lg">{{ m.title }}</h3>
                      <span v-if="m.ageRating" class="px-1.5 py-0.5 bg-error-container text-on-error-container text-[0.6rem] font-bold rounded">{{ m.ageRating }}</span>
                    </div>
                    <p class="text-xs text-on-surface-variant mb-4">
                      <span v-if="m.durationMins">{{ m.durationMins }} phút</span>
                      <span v-if="m.genres.length"> • {{ m.genres.join(', ') }}</span>
                    </p>
                    <p class="text-[0.7rem] font-bold uppercase tracking-widest text-on-surface-variant/60 mb-2">Suất chiếu</p>
                    <div class="flex flex-wrap gap-2">
                      <button
                        v-for="s in m.shows" :key="s.id"
                        @click="goToBooking(s)"
                        class="px-4 py-2 rounded-lg bg-surface-container-high border border-outline-variant/20 hover:border-primary hover:text-primary transition-all flex items-center gap-2 group"
                      >
                        <span class="font-bold text-sm">{{ getTimeString(s.startTime) }}</span>
                        <span class="text-[0.6rem] uppercase tracking-widest opacity-60 group-hover:opacity-100">{{ s.formatName }}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </section>

          <!-- Thông tin rạp + bản đồ -->
          <aside class="order-1 lg:order-2 space-y-6">
            <div class="p-6 rounded-3xl bg-surface-container-low border border-outline-variant/10">
              <h3 class="font-bold font-headline text-lg mb-4">Thông tin rạp</h3>
              <p v-if="cinema.description" class="text-sm text-on-surface-variant leading-relaxed mb-5">{{ cinema.description }}</p>

              <div class="space-y-3 text-sm">
                <div class="flex items-start gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">location_on</span>
                  <span class="text-on-surface-variant">{{ fullAddress }}</span>
                </div>
                <div class="flex items-center gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">call</span>
                  <span class="text-on-surface-variant font-mono">{{ formatHotline(cinema.hotline) }}</span>
                </div>
                <div class="flex items-center gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">meeting_room</span>
                  <span class="text-on-surface-variant">{{ cinema.rooms }} phòng chiếu</span>
                </div>
                <div v-if="cinema.openingTime && cinema.closingTime" class="flex items-center gap-3">
                  <span class="material-symbols-outlined text-primary text-lg">schedule</span>
                  <span class="text-on-surface-variant">Giờ hoạt động: {{ cinema.openingTime }} – {{ cinema.closingTime }}</span>
                </div>
              </div>

              <div v-if="amenitiesList.length" class="mt-5 pt-5 border-t border-outline-variant/10">
                <p class="text-[0.7rem] font-bold uppercase tracking-widest text-on-surface-variant/60 mb-3">Tiện ích</p>
                <div class="flex flex-wrap gap-2">
                  <span v-for="a in amenitiesList" :key="a" class="px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-primary text-xs font-bold">{{ a }}</span>
                </div>
              </div>
            </div>

            <!-- Bản đồ -->
            <div class="rounded-3xl overflow-hidden border border-outline-variant/10 h-72">
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
