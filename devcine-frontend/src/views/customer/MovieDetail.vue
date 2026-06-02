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
    return { month: 'Th. 01', day: '01', weekday: 'Thứ hai' }
  }
  const days = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy']
  const month = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  return {
    month: `Th. ${month}`,
    day: day,
    weekday: days[d.getDay()]
  }
}

onMounted(async () => {
  const movieId = route.params.id || 1 
  
  try {
    const response = await api.get(`/movies/${movieId}`)
    movie.value = response.data
  } catch(e) {
    // mock data if api fails
    movie.value = {
      id: movieId,
      titleVietnamese: 'PHIM SUPER MARIO THIÊN HÀ - P (LỒNG TIẾNG)',
      title: 'SUPER MARIO GALAXY',
      posterUrl: '/images/Hopper.webp',
      format: '2D',
      durationMins: 99,
      director: 'Aaron Horvath, Michael Jelenic',
      cast: 'Chris Pratt, Anya Taylor-Joy, Charlie Day, Jack Black, Keegan-Michael Key',
      releaseDate: '2026-04-01',
      description: 'Phim Super Mario Thiên Hà là một bộ phim hoạt hình được lấy bối cảnh trong thế giới của Anh Em Super Mario và là phần tiếp theo của Phim Anh Em Super Mario - tác phẩm ra mắt năm 2023 và đạt doanh thu hơn 1,3 tỷ đô la trên toàn cầu. Cả hai bộ phim Phim Anh Em Super Mario (2023) và Phim Super Mario Thiên Hà đều do Chris...',
      ageRating: 'P'
    }
  }

  await store.fetchCities()
  await store.fetchShowtimes(movieId, store.selectedCity)
  
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
</script>

<template>
  <main v-if="!loading" class="min-h-screen bg-[#111111] text-white">
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
            <p>Diễn viên: <span class="text-gray-400">{{ movie.cast || 'Đang cập nhật' }}</span></p>
            <p>Khởi chiếu: <span class="text-gray-400">{{ movie.releaseDate ? new Date(movie.releaseDate).toLocaleDateString('vi-VN') : 'Đang cập nhật' }}</span></p>
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
    <section class="bg-[#1a1a1a] min-h-[400px]">
      <!-- Date Picker Bar -->
      <div class="bg-[#111] border-y border-white/5">
        <div class="max-w-[1200px] mx-auto px-6 flex overflow-x-auto no-scrollbar items-center justify-center md:justify-start gap-2">
          <button 
            v-for="date in uniqueDates" 
            :key="date"
            @click="activeDateStr = date"
            :class="[
              'flex flex-col items-center justify-center min-w-[100px] py-4 px-4 transition-colors cursor-pointer border-t-[3px]',
              activeDateStr === date ? 'bg-[#ff3b30] text-white border-transparent' : 'text-gray-400 hover:text-white border-transparent hover:bg-white/5'
            ]"
          >
            <span class="text-[11px] font-medium">{{ formatDateForUI(date).month }}</span>
            <span class="text-2xl font-bold my-0.5">{{ formatDateForUI(date).day }}</span>
            <span class="text-[11px] font-medium">{{ formatDateForUI(date).weekday }}</span>
          </button>
        </div>
      </div>
      
      <!-- Cinemas & Showtimes -->
      <div class="max-w-[1200px] mx-auto px-6 py-10">
        <div class="mb-8 flex justify-end">
           <select v-model="store.selectedCity" @change="onCityChange" class="w-full md:w-[250px] py-2.5 px-4 bg-[#222] border border-white/10 text-white rounded-md outline-none focus:border-[#ff3b30] text-sm">
            <option value="">Toàn quốc</option>
            <option v-for="city in store.cities" :key="city" :value="city">{{ city }}</option>
          </select>
        </div>

        <div v-if="store.cinemaShowtimes.length === 0" class="text-center text-gray-500 py-10">
          Chưa có lịch chiếu cho phim này.
        </div>
        
        <div class="space-y-8" v-else>
          <div v-for="cinema in store.cinemaShowtimes" :key="cinema.cinemaId">
            <!-- Only show cinema if it has showtimes for active date -->
            <div v-if="cinema.showtimesByDate[activeDateStr]" class="flex flex-col md:flex-row gap-6 items-start border-b border-white/5 pb-8 last:border-0">
              <div class="w-full md:w-[300px]">
                <h3 class="font-bold text-lg text-white mb-1.5">{{ cinema.cinemaName }}</h3>
                <p class="text-[13px] text-gray-500 leading-relaxed">{{ cinema.address }}</p>
              </div>
              <div class="flex-1">
                <div class="flex flex-wrap gap-3">
                  <button 
                    v-for="st in cinema.showtimesByDate[activeDateStr]" 
                    :key="st.id" 
                    @click="selectShowtime(st, cinema)" 
                    class="bg-[#2a2a2a] border border-white/10 text-gray-300 hover:bg-[#ff3b30] hover:text-white hover:border-transparent transition-colors px-6 py-2.5 rounded font-semibold text-[15px]"
                  >
                    {{ new Date(st.startTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
