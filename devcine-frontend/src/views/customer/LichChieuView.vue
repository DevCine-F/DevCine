<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import api from '@/api/axios'

const showtimes = ref([])
const loading = ref(true)

const fetchShowtimes = async () => {
  try {
    const response = await api.get('/showtimes/upcoming')
    showtimes.value = response.data
  } catch (error) {
    console.error('Error fetching showtimes:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchShowtimes()
})

// Generate next 7 days for the date selector
const availableDates = computed(() => {
  const dates = []
  const today = new Date()
  const daysOfWeek = ['CN', 'THỨ 2', 'THỨ 3', 'THỨ 4', 'THỨ 5', 'THỨ 6', 'THỨ 7']
  
  for (let i = 0; i < 7; i++) {
    const d = new Date(today)
    d.setDate(today.getDate() + i)
    // Avoid toISOString() timezone offset issue
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const dateStr = `${year}-${month}-${day}`
    
    dates.push({
      dateStr,
      dayOfWeek: i === 0 ? 'HÔM NAY' : daysOfWeek[d.getDay()],
      dateNum: d.getDate(),
      monthStr: `TH ${String(d.getMonth() + 1).padStart(2, '0')}`
    })
  }
  return dates
})

const selectedDate = ref(availableDates.value[0].dateStr)

// Helper to safely get Date string (YYYY-MM-DD) from mixed backend format
const getDateString = (dateTime) => {
  if (!dateTime) return '';
  if (typeof dateTime === 'string') return dateTime.split('T')[0];
  if (Array.isArray(dateTime)) return `${dateTime[0]}-${String(dateTime[1]).padStart(2, '0')}-${String(dateTime[2]).padStart(2, '0')}`;
  return new Date(dateTime).toISOString().split('T')[0];
}

// Helper to safely get Time string (HH:mm) from mixed backend format
const getTimeString = (dateTime) => {
  if (!dateTime) return '';
  if (typeof dateTime === 'string') {
    const d = new Date(dateTime);
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }
  if (Array.isArray(dateTime)) return `${String(dateTime[3]).padStart(2, '0')}:${String(dateTime[4]).padStart(2, '0')}`;
  return '';
}

const selectedCinemaId = ref('')

// Extract unique cinemas for filter
const availableCinemas = computed(() => {
  const cinemas = new Map()
  showtimes.value.forEach(s => {
    if (!cinemas.has(s.cinemaId)) {
      cinemas.set(s.cinemaId, { id: s.cinemaId, name: s.cinemaName })
    }
  })
  return Array.from(cinemas.values()).sort((a, b) => a.name.localeCompare(b.name))
})

// Group showtimes for the selected date
const groupedData = computed(() => {
  if (!selectedDate.value || !showtimes.value.length) return []

  // Filter by selected date and cinema
  let filtered = showtimes.value.filter(s => getDateString(s.startTime) === selectedDate.value)
  if (selectedCinemaId.value) {
    filtered = filtered.filter(s => s.cinemaId === selectedCinemaId.value)
  }

  // Group by Cinema -> Movie -> Format
  const cinemaMap = new Map()
  
  filtered.forEach(s => {
    // Cinema Level
    if (!cinemaMap.has(s.cinemaId)) {
      cinemaMap.set(s.cinemaId, {
        id: s.cinemaId,
        name: s.cinemaName,
        address: s.cinemaAddress,
        movies: new Map()
      })
    }
    const cinemaGroup = cinemaMap.get(s.cinemaId)
    
    // Movie Level
    if (!cinemaGroup.movies.has(s.movieId)) {
      cinemaGroup.movies.set(s.movieId, {
        id: s.movieId,
        title: s.movieTitle,
        genres: s.movieGenres && s.movieGenres.length ? Array.from(s.movieGenres).join(', ') : '',
        duration: s.movieDurationMins,
        posterUrl: s.moviePosterUrl,
        ageRating: s.movieAgeRating,
        country: s.movieCountry,
        releaseDate: s.movieReleaseDate,
        description: s.movieDescription,
        formats: new Map()
      })
    }
    const movieGroup = cinemaGroup.movies.get(s.movieId)
    
    // Format Level
    if (!movieGroup.formats.has(s.formatId)) {
      movieGroup.formats.set(s.formatId, {
        id: s.formatId,
        name: s.formatName,
        showtimes: []
      })
    }
    const formatGroup = movieGroup.formats.get(s.formatId)
    
    // Add showtime
    const timeString = getTimeString(s.startTime)
    formatGroup.showtimes.push({
      id: s.id,
      time: timeString,
      status: s.status
    })
  })

  // Convert maps to arrays for Vue iteration
  return Array.from(cinemaMap.values()).map(cinema => ({
    ...cinema,
    movies: Array.from(cinema.movies.values()).map(movie => ({
      ...movie,
      formats: Array.from(movie.formats.values()).map(format => {
         // Sort showtimes by time
         format.showtimes.sort((a, b) => a.time.localeCompare(b.time))
         return format
      })
    }))
  }))
})
</script>

<template>
  <main class="pt-32 pb-20 max-w-[1440px] mx-auto px-10">
    <!-- Page Title -->
    <header class="mb-12">
      <h1 class="font-headline text-5xl font-extrabold tracking-tight text-on-surface mb-2">Lịch Chiếu</h1>
      <p class="text-on-surface-variant font-medium">Khám phá các suất chiếu đặc sắc tại hệ thống DevCine hôm nay.</p>
    </header>
    
    <!-- Date Selector -->
    <section class="mb-12 flex justify-center">
      <div class="flex space-x-3 overflow-x-auto no-scrollbar pb-2">
        <button 
          v-for="d in availableDates" 
          :key="d.dateStr"
          @click="selectedDate = d.dateStr"
          :class="[
            'flex-shrink-0 px-6 py-2 rounded-lg transition-all duration-200 border text-sm font-bold tracking-wider',
            selectedDate === d.dateStr 
              ? 'bg-error text-white border-error shadow-lg shadow-error/20' 
              : 'bg-surface-container-low text-white border-outline-variant/30 hover:border-error/50 hover:bg-surface-container-high'
          ]"
        >
          {{ String(d.dateNum).padStart(2, '0') }}-{{ d.dateStr.split('-')[1] }}-{{ d.dateStr.split('-')[0] }}
        </button>
      </div>
    </section>

    <!-- Filters (Temporarily hidden) -->
    <!-- <section class="mb-16 flex justify-end">
      <select v-model="selectedCinemaId" class="w-full md:w-[250px] py-3 px-4 bg-surface-container-high border border-outline-variant/30 text-on-surface rounded-sm outline-none focus:border-primary-container text-sm font-bold transition-colors cursor-pointer appearance-none">
        <option value="">Tất cả rạp</option>
        <option v-for="cinema in availableCinemas" :key="cinema.id" :value="cinema.id">{{ cinema.name }}</option>
      </select>
    </section> -->

    <!-- Main Showtimes Section -->
    <div class="space-y-16">
      
      <div v-if="loading" class="text-center py-10 text-on-surface-variant font-headline">
        <span class="material-symbols-outlined animate-spin text-4xl mb-4 block">sync</span>
        Đang tải lịch chiếu...
      </div>
      
      <div v-else-if="!groupedData.length" class="text-center py-20 bg-surface-container-low rounded-sm">
        <span class="material-symbols-outlined text-on-surface-variant/50 text-6xl mb-4 block">event_busy</span>
        <h3 class="font-headline text-2xl font-bold text-on-surface">Không có suất chiếu nào</h3>
        <p class="font-body text-on-surface-variant mt-2">Vui lòng chọn ngày khác để xem lịch chiếu.</p>
      </div>

      <!-- Cinema Branches -->
      <section v-for="cinema in groupedData" :key="cinema.id" class="mb-12">
        <div class="flex items-center justify-center mb-8">
          <div class="w-3 h-3 rounded-full bg-error mr-3 shadow-[0_0_10px_rgba(255,0,0,0.5)]"></div>
          <h2 class="font-headline text-xl md:text-2xl font-bold text-on-surface uppercase">{{ cinema.name }}</h2>
        </div>
        
        <div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
          <!-- Movies -->
          <div v-for="movie in cinema.movies" :key="movie.id" class="flex flex-row gap-6 p-6 border border-outline-variant/10 bg-surface-container-low rounded-xl transition-all hover:border-primary-container/30">
            <!-- Poster -->
            <div class="w-[140px] sm:w-[180px] lg:w-[200px] shrink-0">
              <div class="relative group overflow-hidden rounded-lg aspect-[2/3] shadow-md bg-surface-container-high">
                <img v-if="movie.posterUrl" :src="movie.posterUrl" class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"/>
                <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
              </div>
            </div>
            
            <!-- Details -->
            <div class="flex-1 flex flex-col justify-start relative">
              <!-- Format Badge Top Right (First format only, for style) -->
              <div class="absolute top-0 right-0 hidden sm:block">
                <span class="border border-outline-variant/30 text-on-surface-variant text-[11px] font-bold px-3 py-1.5 rounded">{{ movie.formats && movie.formats[0] ? movie.formats[0].name.replace(' PHỤ ĐỀ', '').replace(' LỒNG TIẾNG', '') : '2D' }}</span>
              </div>
              
              <!-- Meta (Genres, Duration) -->
              <div class="flex items-center text-xs text-on-surface-variant mb-2 pr-12">
                <span v-if="movie.genres">{{ movie.genres }}</span>
                <span v-if="movie.genres && movie.duration" class="mx-2">|</span>
                <span v-if="movie.duration">{{ movie.duration }} phút</span>
              </div>
              
              <!-- Title -->
              <h3 class="font-headline text-xl sm:text-2xl font-bold text-on-surface uppercase mb-3 leading-tight pr-12">
                {{ movie.title }} <span v-if="movie.ageRating" class="text-on-surface-variant">- {{ movie.ageRating }}</span>
              </h3>
              
              <!-- Additional Info -->
              <div class="text-xs text-on-surface-variant space-y-1.5 mb-3">
                <p>Xuất xứ: <span class="text-on-surface">{{ movie.country || 'Đang cập nhật' }}</span></p>
                <p>Khởi chiếu: <span class="text-on-surface">{{ movie.releaseDate ? new Date(movie.releaseDate).toLocaleDateString('vi-VN') : 'Đang cập nhật' }}</span></p>
              </div>

              <!-- Age Rating Warning -->
              <p v-if="movie.ageRating" class="text-xs text-error mb-5 italic">
                * Phim được phổ biến đến người xem từ độ tuổi {{ movie.ageRating.replace('T', '') }} trở lên
              </p>

              
              <!-- Showtimes -->
              <div class="mt-auto border-t border-outline-variant/10 pt-4">
                <p class="text-sm font-bold text-on-surface mb-3 tracking-wide">Lịch chiếu</p>
                <div class="space-y-4">
                  <div v-for="format in movie.formats" :key="format.id">
                    <span v-if="movie.formats.length > 1" class="text-[10px] font-bold text-on-surface-variant uppercase mb-2 block border-l-2 border-primary-container pl-2">{{ format.name }}</span>
                    <div class="flex flex-wrap gap-2.5">
                      <router-link 
                        v-for="st in format.showtimes" 
                        :key="st.id" 
                        :to="'/booking?showtimeId=' + st.id" 
                        class="px-5 py-2 border border-outline-variant/30 text-on-surface hover:border-primary-container hover:bg-primary-container/10 hover:text-primary-container text-sm font-bold transition-all rounded-md"
                      >
                        {{ st.time }}
                      </router-link>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Legend / Info -->
    <section class="mt-16 grid grid-cols-1 md:grid-cols-3 gap-8 py-10 border-t border-outline-variant/20">
      <div class="flex items-start space-x-4">
        <span class="material-symbols-outlined text-primary-container text-3xl">confirmation_number</span>
        <div>
          <h4 class="font-bold text-on-surface mb-1">Đặt vé trực tuyến</h4>
          <p class="text-xs text-on-surface-variant leading-relaxed">Tiết kiệm thời gian, chọn chỗ ngồi đẹp nhất và nhận vé ngay qua ứng dụng.</p>
        </div>
      </div>
      <div class="flex items-start space-x-4">
        <span class="material-symbols-outlined text-primary-container text-3xl">info</span>
        <div>
          <h4 class="font-bold text-on-surface mb-1">Quy định phân loại</h4>
          <p class="text-xs text-on-surface-variant leading-relaxed">Vui lòng kiểm tra độ tuổi yêu cầu của phim trước khi đặt vé để đảm bảo trải nghiệm tốt nhất.</p>
        </div>
      </div>
      <div class="flex items-start space-x-4">
        <span class="material-symbols-outlined text-primary-container text-3xl">loyalty</span>
        <div>
          <h4 class="font-bold text-on-surface mb-1">Ưu đãi thành viên</h4>
          <p class="text-xs text-on-surface-variant leading-relaxed">Tích lũy điểm DevPoints cho mỗi giao dịch và đổi nhiều phần quà hấp dẫn.</p>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
