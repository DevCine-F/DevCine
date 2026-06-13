<script setup>
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useBookingStore } from '@/stores/booking'
import { onMounted, ref, computed } from 'vue'
import api from '@/api/axios'

const route = useRoute()
const router = useRouter()
const store = useBookingStore()

const movie = ref({})
const loading = ref(true)
const activeDateStr = ref('')

const formatDateForUI = (dateString) => {
  // expects YYYY-MM-DD
  const d = new Date(dateString)
  if (isNaN(d.getTime())) {
    // fallback if dateString is not parseable
    return { weekday: 'Thứ Hai', dateStr: '01/01' }
  }
  const days = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']
  const month = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  return {
    weekday: days[d.getDay()],
    dateStr: `${day}/${month}`
  }
}

onMounted(async () => {
  const movieId = route.params.id || 1 
  
  const fetchMovieData = api.get(`/movies/${movieId}`)
    .then(response => {
      movie.value = response.data
    })
    .catch(e => {
      // mock data if api fails
      movie.value = {
        id: movieId,
        titleVietnamese: 'PHIM SUPER MARIO THIÊN HÀ - P (LỒNG TIẾNG)',
        title: 'SUPER MARIO GALAXY',
        posterUrl: '/images/Hopper.webp',
        format: '2D',
        durationMins: 99,
        director: 'Aaron Horvath, Michael Jelenic',
        castMembers: 'Chris Pratt, Anya Taylor-Joy, Charlie Day, Jack Black, Keegan-Michael Key',
        releaseDate: '2026-04-01',
        description: 'Phim Super Mario Thiên Hà là một bộ phim hoạt hình được lấy bối cảnh trong thế giới của Anh Em Super Mario và là phần tiếp theo của Phim Anh Em Super Mario - tác phẩm ra mắt năm 2023 và đạt doanh thu hơn 1,3 tỷ đô la trên toàn cầu. Cả hai bộ phim Phim Anh Em Super Mario (2023) và Phim Super Mario Thiên Hà đều do Chris...',
        ageRating: 'P'
      }
    })

  await Promise.all([
    fetchMovieData,
    store.fetchCities(),
    store.fetchShowtimes(movieId, store.selectedCity)
  ])
  
  if (uniqueDates.value.length > 0) {
     activeDateStr.value = uniqueDates.value[0]
  }
  loading.value = false
})

const onCityChange = async () => {
  const movieId = route.params.id || 1
  await store.fetchShowtimes(movieId, store.selectedCity)
  if (uniqueDates.value.length > 0 && !uniqueDates.value.includes(activeDateStr.value)) {
     activeDateStr.value = uniqueDates.value[0]
  }
}

const selectShowtime = (showtime, cinema) => {
  store.setMovie(movie.value)
  store.setShowtime(showtime, cinema)
  router.push('/booking')
}

const uniqueDates = computed(() => {
  const dates = new Set()
  store.cinemaShowtimes.forEach(c => {
    Object.keys(c.showtimesByDate).forEach(d => dates.add(d))
  })
  return Array.from(dates).sort()
})

const groupShowtimesByFormat = (showtimes) => {
  if (!showtimes) return {}
  const groups = {}
  showtimes.forEach(st => {
    const format = st.formatName || '2D Phụ Đề'
    if (!groups[format]) {
      groups[format] = []
    }
    groups[format].push(st)
  })
  return groups
}
</script>

<template>
  <main v-if="loading" class="min-h-screen bg-[#111111] text-white">
    <section class="relative pt-32 pb-16 min-h-[600px] flex items-center">
      <div class="relative z-10 max-w-[1200px] mx-auto px-6 w-full flex flex-col md:flex-row gap-12 items-start animate-pulse">
        <!-- Skeleton Poster -->
        <div class="w-full md:w-[320px] flex-shrink-0">
          <div class="w-full aspect-[2/3] bg-white/10 rounded-xl border border-white/5"></div>
        </div>
        
        <!-- Skeleton Info -->
        <div class="flex-1 mt-6 w-full">
          <div class="h-10 bg-white/10 rounded w-3/4 mb-6"></div>
          <div class="space-y-3 mb-8">
            <div class="h-4 bg-white/10 rounded w-1/2"></div>
            <div class="h-4 bg-white/10 rounded w-2/3"></div>
            <div class="h-4 bg-white/10 rounded w-1/3"></div>
          </div>
          <div class="space-y-3 mb-8">
            <div class="h-4 bg-white/10 rounded w-full"></div>
            <div class="h-4 bg-white/10 rounded w-full"></div>
            <div class="h-4 bg-white/10 rounded w-4/5"></div>
          </div>
          <div class="flex gap-6">
            <div class="h-10 bg-white/10 rounded-full w-32"></div>
            <div class="h-10 bg-white/10 rounded-full w-40"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- Skeleton Showtimes -->
    <section class="bg-[#1a1a1a] min-h-[400px]">
      <div class="bg-[#111] border-y border-white/5 py-4">
        <div class="max-w-[1200px] mx-auto px-6 flex gap-4 animate-pulse">
          <div class="w-20 h-16 bg-white/10 rounded"></div>
          <div class="w-20 h-16 bg-white/10 rounded"></div>
          <div class="w-20 h-16 bg-white/10 rounded"></div>
        </div>
      </div>
      <div class="max-w-[1200px] mx-auto px-6 py-10 animate-pulse">
        <div class="flex justify-end mb-8">
          <div class="w-[250px] h-10 bg-white/10 rounded"></div>
        </div>
        <div class="space-y-8">
          <div class="flex flex-col md:flex-row gap-6 border-b border-white/5 pb-8">
            <div class="w-full md:w-[300px] space-y-3">
              <div class="w-3/4 h-5 bg-white/10 rounded"></div>
              <div class="w-full h-4 bg-white/5 rounded"></div>
            </div>
            <div class="flex-1 flex flex-wrap gap-3">
              <div class="w-24 h-10 bg-white/10 rounded"></div>
              <div class="w-24 h-10 bg-white/10 rounded"></div>
              <div class="w-24 h-10 bg-white/10 rounded"></div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>

  <main v-else class="min-h-screen bg-[#111111] text-white">
    <!-- Top Section with Blurred Background -->
    <section class="relative pt-32 pb-16 min-h-[600px] flex items-center">
      <div class="absolute inset-0 z-0 overflow-hidden">
        <img class="w-full h-full object-cover opacity-30 scale-110 blur-xl" :src="movie.posterUrl || '/images/Hopper.webp'"/>
        <div class="absolute inset-0 bg-gradient-to-t from-[#111111] via-[#111111]/80 to-black/30"></div>
        <div class="absolute inset-0 bg-gradient-to-r from-[#111111]/90 via-[#111111]/50 to-transparent"></div>
      </div>
      
      <div class="relative z-10 max-w-[1200px] mx-auto px-6 w-full flex flex-col md:flex-row gap-12 items-start">
        <!-- Poster -->
        <div class="w-full md:w-[320px] flex-shrink-0">
          <div class="rounded-xl overflow-hidden shadow-[0_0_40px_rgba(0,0,0,0.8)] border border-white/10">
            <img class="w-full h-auto object-cover" :src="movie.posterUrl || '/images/Hopper.webp'"/>
          </div>
        </div>
        
        <!-- Info -->
        <div class="flex-1 mt-6">
          <div class="flex flex-wrap items-center gap-4 mb-4">
            <h1 class="text-4xl md:text-[40px] font-bold uppercase tracking-tight text-white leading-tight">
              {{ movie.titleVietnamese || movie.title }}
            </h1>
            <span class="border border-white/50 text-white/90 px-2 py-0.5 rounded text-sm font-bold backdrop-blur-sm">{{ movie.format || '2D' }}</span>
          </div>
          
          <div class="text-[15px] text-gray-300 space-y-1.5 mb-6 leading-relaxed">
            <p><span class="font-bold text-white">{{ movie.durationMins || 120 }} phút</span> &nbsp;|&nbsp; Đạo diễn: <span class="text-gray-400">{{ movie.director || 'Đang cập nhật' }}</span></p>
            <p>Diễn viên: <span class="text-gray-400">{{ movie.castMembers || 'Đang cập nhật' }}</span></p>
            <p>Khởi chiếu: <span class="text-gray-400">{{ movie.startDate ? new Date(movie.startDate).toLocaleDateString('vi-VN') : 'Đang cập nhật' }}</span></p>
          </div>
          
          <p class="text-[15px] text-gray-300 leading-relaxed mb-6 line-clamp-4">
            {{ movie.description || 'Chưa có thông tin nội dung phim.' }}
          </p>
          
          <p class="text-[#ff3b30] text-sm font-medium mb-8">
            Kiểm duyệt: {{ movie.ageRating || 'P' }} - Phim được phép phổ biến đến người xem ở độ tuổi tương ứng.
          </p>
          
          <div class="flex items-center gap-8">
            <button class="text-white hover:text-gray-300 transition-colors text-sm font-semibold flex items-center gap-1">
              Chi tiết nội dung <span class="material-symbols-outlined text-sm ml-1">arrow_forward</span>
            </button>
            <button class="border-2 border-[#f5c518] text-[#f5c518] px-6 py-2.5 rounded-full flex items-center gap-2 hover:bg-[#f5c518] hover:text-black transition-colors font-bold text-sm">
              <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
              Xem trailer
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Date & Showtimes Section -->
    <section class="bg-[#111111] min-h-[500px] text-gray-200 font-sans border-t border-white/5">
      <div class="max-w-[1200px] mx-auto px-6 py-10">
        
        <!-- Top Control Bar: Dates & Filters -->
        <div class="flex flex-col md:flex-row justify-between items-start md:items-end border-b border-white/10 pb-0 mb-8 gap-4">
          <!-- Date Picker -->
          <div class="flex overflow-x-auto no-scrollbar gap-2 pb-0">
            <button 
              v-for="date in uniqueDates" 
              :key="date"
              @click="activeDateStr = date"
              :class="[
                'flex flex-col items-center justify-center min-w-[100px] py-3 px-4 rounded-t-md transition-colors cursor-pointer border-b-2',
                activeDateStr === date ? 'bg-transparent text-[#ff3b30] border-[#ff3b30]' : 'bg-transparent text-gray-400 border-transparent hover:text-white hover:border-white/30'
              ]"
            >
              <span class="text-[14px] font-medium mb-1">{{ formatDateForUI(date).weekday }}</span>
              <span class="text-[14px] font-bold">{{ formatDateForUI(date).dateStr }}</span>
            </button>
          </div>
          
          <!-- Filters -->
          <div class="flex gap-4 w-full md:w-auto pb-3">
             <select v-model="store.selectedCity" @change="onCityChange" class="w-full md:w-[150px] py-2 px-3 bg-[#1a1a1a] border border-white/10 text-gray-300 rounded outline-none focus:border-[#ff3b30] text-[14px] transition-colors">
              <option value="">Toàn quốc</option>
              <option v-for="city in store.cities" :key="city" :value="city">{{ city }}</option>
            </select>
             <select class="w-full md:w-[150px] py-2 px-3 bg-[#1a1a1a] border border-white/10 text-gray-300 rounded outline-none focus:border-[#ff3b30] text-[14px] transition-colors">
              <option value="">Tất cả rạp</option>
            </select>
          </div>
        </div>

        <div v-if="store.cinemaShowtimes.length === 0" class="text-center text-gray-500 py-10">
          Chưa có lịch chiếu cho phim này.
        </div>
        
        <div class="space-y-0" v-else>
          <!-- Using a variable to handle zebra striping only for visible items -->
          <template v-for="(cinema, index) in store.cinemaShowtimes" :key="cinema.cinemaId">
            <div v-if="cinema.showtimesByDate[activeDateStr]" :class="['py-8 px-6 -mx-6 border-b border-white/10 last:border-b-0', index % 2 === 1 ? 'bg-[#1a1a1a]' : 'bg-transparent']">
              <h3 class="font-bold text-[18px] text-white mb-4">{{ cinema.cinemaName }}</h3>
              
              <div v-for="(sts, format) in groupShowtimesByFormat(cinema.showtimesByDate[activeDateStr])" :key="format" class="flex flex-col md:flex-row md:items-center gap-4 mt-6 first:mt-0">
                <!-- Left: Format (15-20% width) -->
                <div class="w-full md:w-[150px] lg:w-[180px] flex-shrink-0">
                  <span class="text-[14px] text-gray-400 font-bold whitespace-pre-line leading-relaxed">{{ format.replace(' Lồng', '\nLồng').replace(' Phụ', '\nPhụ') }}</span>
                </div>
                
                <!-- Right: Times -->
                <div class="flex-1 flex flex-wrap items-center gap-3">
                  <button 
                    v-for="st in sts" 
                    :key="st.id" 
                    @click="selectShowtime(st, cinema)" 
                    class="bg-[#262626] border border-[#444444] text-gray-200 hover:border-[#f5c518] hover:text-[#f5c518] transition-all px-5 py-2 rounded-[6px] text-[15px] font-bold min-w-[80px]"
                  >
                    {{ new Date(st.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}
                  </button>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
