<script setup>
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useBookingStore } from '@/stores/booking'
import { useAuthStore } from '@/stores/auth'
import { reviewApi } from '@/api/customer/index'
import { onMounted, ref, computed, nextTick, watch } from 'vue'
import api from '@/api/axios'
import TrailerModal from '@/components/common/TrailerModal.vue'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const route = useRoute()
const router = useRouter()
const store = useBookingStore()
const authStore = useAuthStore()
const toast = useToastStore()

const movie = ref({})
const loading = ref(true)
const loadError = ref(false)
const activeDateStr = ref('')

// --- Chi tiết nội dung / Trailer / Lọc rạp ---
const descExpanded = ref(false)
const showTrailer = ref(false)
const selectedCinemaId = ref('')
const allCinemas = ref([]) // toàn bộ rạp (để liệt kê theo thành phố, kể cả rạp chưa có suất)
const ageRatings = ref([])

const ageRatingDesc = computed(() => {
  const code = (movie.value?.ageRating || 'P').trim().toUpperCase()
  const found = ageRatings.value.find(a => (a.code || '').trim().toUpperCase() === code)
  if (found && found.description) return found.description
  if (found && found.name) return found.name
  const defaultMap = {
    'P': 'Phim được phép phổ biến rộng rãi đến mọi lứa tuổi người xem',
    'K': 'Phim dành cho khán giả dưới 13 tuổi với điều kiện có cha mẹ hoặc người giám hộ đi cùng',
    'T13': 'Phim chỉ dành cho khán giả từ đủ 13 tuổi trở lên (13+)',
    'T16': 'Phim chỉ dành cho khán giả từ đủ 16 tuổi trở lên (16+)',
    'T18': 'Phim chỉ dành cho khán giả từ đủ 18 tuổi trở lên (18+)',
    'C': 'Phim không được phép phổ biến'
  }
  return defaultMap[code] || 'Phim được phép phổ biến rộng rãi đến mọi lứa tuổi người xem'
})

// --- Đánh giá phim ---
const reviewsData = ref({ averageRating: 0, totalReviews: 0, reviews: [], distribution: {} })
const myRating = ref(0)
const hoverRating = ref(0)
const myComment = ref('')
const submittingReview = ref(false)
const reviewFilter = ref(0) // 0 = tất cả; 1..5 = lọc theo số sao
const showLoginModal = ref(false)
const ratingError = ref(false)
const commentError = ref(false)

// Trạng thái quyền đánh giá (form động): null = chưa xác định, còn lại theo eligibility từ BE
const eligibilityLoaded = ref(false)
const canReview = ref(false)
const hasReviewed = ref(false)

// Đã đăng nhập hay chưa (dùng cho form 3 trạng thái)
const isLoggedIn = computed(() => authStore.isAuthenticated && !!authStore.user?.id)

const fetchEligibility = async (movieId) => {
  if (!isLoggedIn.value) { eligibilityLoaded.value = true; return }
  try {
    const { data } = await reviewApi.eligibility(movieId, authStore.user.id)
    const e = data.data ?? data
    canReview.value = !!e.canReview
    hasReviewed.value = !!e.hasReviewed
    // Đã đánh giá trước đó → đổ lại nội dung cũ để khách sửa
    if (e.hasReviewed) {
      myRating.value = e.myRating || 0
      myComment.value = e.myComment || ''
    }
  } catch (e) {
    console.error('Không kiểm tra được quyền đánh giá', e)
    canReview.value = false
  } finally {
    eligibilityLoaded.value = true
  }
}

// Đưa khách tới khu vực chọn suất chiếu để mua vé
const goToShowtimes = () => {
  document.getElementById('showtimes-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// Chọn sao / gõ nội dung → xóa trạng thái lỗi tương ứng
const selectRating = (value) => {
  myRating.value = value
  ratingError.value = false
}
const onCommentInput = () => {
  if (myComment.value.trim()) commentError.value = false
}

// Lưu tạm bản nháp đánh giá theo phim để không mất khi khách đi đăng nhập rồi quay lại
const reviewDraftKey = () => `reviewDraft:${route.params.id}`

const saveReviewDraft = () => {
  try {
    sessionStorage.setItem(reviewDraftKey(), JSON.stringify({
      rating: myRating.value,
      comment: myComment.value
    }))
  } catch (e) { /* sessionStorage không khả dụng → bỏ qua */ }
}

const restoreReviewDraft = () => {
  try {
    const raw = sessionStorage.getItem(reviewDraftKey())
    if (!raw) return
    const draft = JSON.parse(raw)
    if (draft.rating) myRating.value = draft.rating
    if (draft.comment) myComment.value = draft.comment
    sessionStorage.removeItem(reviewDraftKey())
    // Cuộn về đúng khu vực đánh giá khách đang thao tác trước khi đăng nhập
    nextTick(() => {
      document.getElementById('review-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
  } catch (e) { /* dữ liệu hỏng → bỏ qua */ }
}

const goToLogin = () => {
  saveReviewDraft()
  showLoginModal.value = false
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

const dismissLoginModal = () => {
  showLoginModal.value = false
  toast.warning('Vui lòng đăng nhập để chia sẻ cảm nhận của bạn về bộ phim.')
}

// Phân phối sao 5→1 kèm % để vẽ thanh
const ratingDistribution = computed(() => {
  const dist = reviewsData.value.distribution || {}
  const total = reviewsData.value.totalReviews || 0
  return [5, 4, 3, 2, 1].map(star => {
    const count = dist[String(star)] || 0
    return { star, count, percent: total ? Math.round((count / total) * 100) : 0 }
  })
})

// Danh sách đánh giá sau khi lọc theo số sao
const filteredReviews = computed(() => {
  const list = reviewsData.value.reviews || []
  return reviewFilter.value ? list.filter(r => r.rating === reviewFilter.value) : list
})

const fetchReviews = async (movieId) => {
  try {
    const { data } = await reviewApi.getForMovie(movieId)
    reviewsData.value = data.data ?? data
  } catch (e) {
    toast.error(friendlyError(e, 'Không tải được đánh giá phim.'))
  }
}

const submitReview = async () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) {
    showLoginModal.value = true
    return
  }
  // Lỗi nhập liệu → chỉ hiển thị inline (viền đỏ + dòng lỗi), không dùng toast
  ratingError.value = myRating.value < 1
  commentError.value = !myComment.value.trim()
  if (ratingError.value || commentError.value) return

  submittingReview.value = true
  try {
    await reviewApi.submit({
      movieId: route.params.id,
      customerId: authStore.user.id,
      rating: myRating.value,
      comment: myComment.value
    })
    toast.success('Gửi đánh giá thành công! Cảm ơn nhận xét của bạn.')
    hasReviewed.value = true // giữ nội dung để khách có thể sửa lại
    await fetchReviews(route.params.id)
  } catch (err) {
    toast.error(friendlyError(err, 'Không thể gửi đánh giá, vui lòng thử lại sau!'))
  } finally {
    submittingReview.value = false
  }
}

const formatReviewDate = (iso) => {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

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
      console.error('Không tải được thông tin phim', e)
      loadError.value = true
    })

  // Endpoint công khai (khác /cinemas cần đăng nhập): trả rạp kèm city để lọc theo thành phố
  const fetchCinemas = api.get('/showtimes/cinemas')
    .then(r => { const l = r.data?.data ?? r.data; allCinemas.value = Array.isArray(l) ? l : [] })
    .catch(() => { allCinemas.value = [] })

  const fetchAgeRatings = api.get('/categories/age-ratings')
    .then(r => { const l = r.data?.data ?? r.data; ageRatings.value = Array.isArray(l) ? l : [] })
    .catch(() => { ageRatings.value = [] })

  await Promise.all([
    fetchMovieData,
    store.fetchCities(),
    fetchReviews(movieId),
    fetchCinemas,
    fetchAgeRatings
  ])

  // Mặc định tự động: Toàn quốc (city = '') và Tất cả rạp (cinemaId = '')
  store.selectedCity = ''
  selectedCinemaId.value = ''

  await store.fetchShowtimes(movieId, store.selectedCity)
  
  if (uniqueDates.value.length > 0) {
     activeDateStr.value = uniqueDates.value[0]
  }
  loading.value = false

  // Kiểm tra quyền đánh giá để render form động (đủ điều kiện / chưa mua vé)
  await fetchEligibility(movieId)

  // Khôi phục bản nháp đánh giá sau khi khách đăng nhập quay lại (chỉ khi đủ quyền)
  if (canReview.value) restoreReviewDraft()
})

const onCityChange = async () => {
  const movieId = route.params.id || 1
  // Khi đổi tỉnh/thành, tự động đặt lại là Tất cả rạp của tỉnh/thành đó
  selectedCinemaId.value = ''

  await store.fetchShowtimes(movieId, store.selectedCity)
  if (uniqueDates.value.length > 0 && !uniqueDates.value.includes(activeDateStr.value)) {
     activeDateStr.value = uniqueDates.value[0]
  }
}

const openTrailer = () => { showTrailer.value = true }

// Thể loại phim (gộp tên các Category) cho khối "Chi tiết nội dung"
const genreText = computed(() => {
  const g = movie.value?.genres
  if (!g || g.length === 0) return ''
  return g.map(x => x.name).filter(Boolean).join(', ')
})

// Danh sách rạp để đổ vào bộ lọc "cụm rạp": TẤT CẢ rạp của thành phố đang chọn
// (kể cả rạp chưa có suất chiếu phim này). Toàn quốc (city rỗng) -> mọi rạp.
const cinemaOptions = computed(() => {
  const city = store.selectedCity
  return allCinemas.value
    .filter(c => !city || c.city === city)
    .map(c => ({ id: c.id, name: c.name }))
})

// Rạp hiển thị: có suất ở ngày đang chọn + khớp bộ lọc rạp
const visibleCinemas = computed(() =>
  store.cinemaShowtimes.filter(c => {
    const sts = c.showtimesByDate?.[activeDateStr.value]
    const hasDate = Array.isArray(sts) && sts.length > 0
    const matchCinema = !selectedCinemaId.value || c.cinemaId === selectedCinemaId.value
    return hasDate && matchCinema
  })
)

const selectShowtime = (showtime, cinema) => {
  if (isSoldOut(showtime)) return // hết ghế → chặn chọn
  store.setMovie(movie.value)
  store.setShowtime(showtime, cinema)
  router.push('/booking')
}

// ===== Card suất chiếu: giờ + phòng + tình trạng ghế =====
const fmtTime = (t) => new Date(t).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false })
// Chỉ coi là "hết ghế" khi phòng có sơ đồ ghế (totalSeats > 0) và không còn ghế trống
const isSoldOut = (st) => (st.totalSeats > 0) && (st.availableSeats <= 0)
// Sắp hết: còn dưới 10 ghế
const isLowSeats = (st) => (st.totalSeats > 0) && st.availableSeats > 0 && st.availableSeats < 10

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

  <main v-else-if="loadError" class="min-h-screen bg-[#111111] text-white flex items-center justify-center px-6">
    <div class="text-center max-w-md">
      <span class="material-symbols-outlined text-6xl text-error mb-4">error</span>
      <h2 class="text-2xl font-headline font-bold text-white mb-2 uppercase tracking-tight">Không tải được thông tin phim</h2>
      <p class="text-on-surface-variant mb-8">Phim không tồn tại hoặc đã xảy ra lỗi khi kết nối máy chủ. Vui lòng thử lại.</p>
      <div class="flex items-center justify-center gap-4">
        <RouterLink to="/lich-chieu" class="px-6 py-3 bg-primary-container text-on-primary font-bold rounded-full hover:brightness-110 transition-all">Xem lịch chiếu</RouterLink>
        <RouterLink to="/" class="px-6 py-3 border border-white/15 text-white font-bold rounded-full hover:bg-white/5 transition-all">Về trang chủ</RouterLink>
      </div>
    </div>
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
              {{ movie.title }}
            </h1>
            <span class="border border-white/50 text-white/90 px-2 py-0.5 rounded text-sm font-bold backdrop-blur-sm">{{ movie.format || '2D' }}</span>
          </div>
          
          <div class="text-[15px] text-gray-300 space-y-1.5 mb-6 leading-relaxed">
            <p><span class="font-bold text-white">{{ movie.durationMins || 120 }} phút</span> &nbsp;|&nbsp; Đạo diễn: <span class="text-gray-400">{{ movie.director || 'Đang cập nhật' }}</span></p>
            <p>Diễn viên: <span class="text-gray-400">{{ movie.castMembers || 'Đang cập nhật' }}</span></p>
            <p>Khởi chiếu: <span class="text-gray-400">{{ movie.startDate ? new Date(movie.startDate).toLocaleDateString('vi-VN') : 'Đang cập nhật' }}</span></p>
          </div>
          
          <p class="text-[15px] text-gray-300 leading-relaxed mb-4" :class="{ 'line-clamp-4': !descExpanded }">
            {{ movie.description || 'Chưa có thông tin nội dung phim.' }}
          </p>

          <!-- Khối thông tin mở rộng khi bấm "Chi tiết nội dung" -->
          <div v-if="descExpanded" class="text-[15px] text-gray-300 space-y-1.5 mb-6 leading-relaxed border-l-2 border-[#f5c518]/40 pl-4">
            <p v-if="genreText">Thể loại: <span class="text-gray-400">{{ genreText }}</span></p>
            <p>Quốc gia: <span class="text-gray-400">{{ movie.country || 'Đang cập nhật' }}</span></p>
            <p>Ngôn ngữ: <span class="text-gray-400">{{ movie.language || movie.originalLanguage || 'Đang cập nhật' }}</span></p>
            <p v-if="movie.productionYear">Năm sản xuất: <span class="text-gray-400">{{ movie.productionYear }}</span></p>
          </div>

          <p class="text-[#ff3b30] text-sm font-medium mb-8">
            Kiểm duyệt: {{ movie.ageRating || 'P' }} - {{ ageRatingDesc }}
          </p>

          <div class="flex items-center gap-8">
            <button @click="descExpanded = !descExpanded" class="text-white hover:text-gray-300 transition-colors text-sm font-semibold flex items-center gap-1">
              {{ descExpanded ? 'Thu gọn' : 'Chi tiết nội dung' }}
              <span class="material-symbols-outlined text-sm ml-1 transition-transform" :class="{ 'rotate-90': descExpanded }">arrow_forward</span>
            </button>
            <button v-if="movie.trailerUrl" @click="openTrailer" class="border-2 border-[#f5c518] text-[#f5c518] px-6 py-2.5 rounded-full flex items-center gap-2 hover:bg-[#f5c518] hover:text-black transition-colors font-bold text-sm">
              <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
              Xem trailer
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Date & Showtimes Section -->
    <section id="showtimes-section" class="bg-[#111111] min-h-[500px] text-gray-200 font-sans border-t border-white/5">
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
             <select v-model="selectedCinemaId" class="w-full md:w-[150px] py-2 px-3 bg-[#1a1a1a] border border-white/10 text-gray-300 rounded outline-none focus:border-[#ff3b30] text-[14px] transition-colors">
              <option value="">Tất cả rạp</option>
              <option v-for="c in cinemaOptions" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
        </div>

        <div v-if="visibleCinemas.length === 0" class="text-center text-gray-500 py-10">
          <template v-if="selectedCinemaId">Chưa có lịch chiếu của phim tại rạp này.</template>
          <template v-else-if="store.cinemaShowtimes.length === 0">Chưa có lịch chiếu cho phim này.</template>
          <template v-else>Không có suất chiếu phù hợp với lựa chọn của bạn.</template>
        </div>

        <div class="space-y-0" v-else>
          <div
            v-for="(cinema, index) in visibleCinemas"
            :key="cinema.cinemaId"
            :class="['py-8 px-6 -mx-6 border-b border-white/10 last:border-b-0', index % 2 === 1 ? 'bg-[#1a1a1a]' : 'bg-transparent']"
          >
            <h3 class="font-bold text-[18px] text-white mb-4">{{ cinema.cinemaName }}</h3>

            <div v-for="(sts, format) in groupShowtimesByFormat(cinema.showtimesByDate[activeDateStr])" :key="format" class="flex flex-col md:flex-row md:items-center gap-4 mt-6 first:mt-0">
              <!-- Left: Format (15-20% width) -->
              <div class="w-full md:w-[150px] lg:w-[180px] flex-shrink-0">
                <span class="text-[14px] text-gray-400 font-bold whitespace-pre-line leading-relaxed">{{ format.replace(' Lồng', '\nLồng').replace(' Phụ', '\nPhụ') }}</span>
              </div>

              <!-- Right: Showtime cards (Phòng / Giờ / Ghế) -->
              <div class="flex-1 flex flex-wrap items-center gap-3">
                <button
                  v-for="st in sts"
                  :key="st.id"
                  @click="selectShowtime(st, cinema)"
                  :disabled="isSoldOut(st)"
                  :title="isSoldOut(st) ? 'Suất chiếu đã hết ghế' : ''"
                  :class="isSoldOut(st)
                    ? 'border-[#333] bg-[#1a1a1a] opacity-40 cursor-not-allowed'
                    : 'border-[#444444] bg-[#1f1f1f] hover:border-[#f5c518] hover:bg-[#262626] cursor-pointer'"
                  class="group flex flex-col items-center justify-center gap-1 border rounded-lg w-[140px] min-h-[80px] px-4 py-3 flex-shrink-0 transition-all"
                >
                  <!-- Dòng 1: Tên phòng — cố định 1 dòng, tràn thì ellipsis gọn gàng -->
                  <span class="block w-full text-center text-xs text-gray-400 font-medium leading-tight overflow-hidden text-ellipsis whitespace-nowrap">{{ st.roomName }}</span>
                  <!-- Dòng 2: Giờ chiếu (tâm điểm) -->
                  <span
                    class="text-xl font-bold leading-none tracking-tight"
                    :class="isSoldOut(st) ? 'text-gray-500' : 'text-[#f5c518]'"
                  >{{ fmtTime(st.startTime) }}</span>
                  <!-- Dòng 3: Tình trạng ghế — luôn 1 dòng, không wrap -->
                  <span
                    v-if="st.totalSeats > 0"
                    class="text-xs font-medium leading-tight whitespace-nowrap"
                    :class="isSoldOut(st) ? 'text-gray-500' : (isLowSeats(st) ? 'text-orange-400' : 'text-gray-400')"
                  >{{ isSoldOut(st) ? 'Hết ghế' : `${st.availableSeats} / ${st.totalSeats} Ghế` }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ĐÁNH GIÁ & BÌNH LUẬN -->
    <section id="review-section" class="bg-[#111111] border-t border-white/5">
      <div class="max-w-[1200px] mx-auto px-6 py-14">
        <div class="flex items-center justify-between mb-8">
          <h2 class="text-2xl font-bold text-white uppercase tracking-tight">Đánh giá phim</h2>
          <div v-if="reviewsData.totalReviews > 0" class="flex items-center gap-3">
            <span class="text-4xl font-extrabold text-[#f5c518]">{{ reviewsData.averageRating }}</span>
            <div class="flex flex-col">
              <div class="flex">
                <span v-for="i in 5" :key="i" class="material-symbols-outlined text-[18px]"
                      :class="i <= Math.round(reviewsData.averageRating) ? 'text-[#f5c518]' : 'text-white/20'"
                      style="font-variation-settings: 'FILL' 1;">star</span>
              </div>
              <span class="text-[11px] text-gray-400">{{ reviewsData.totalReviews }} lượt đánh giá</span>
            </div>
          </div>
        </div>

        <!-- Phân phối sao -->
        <div v-if="reviewsData.totalReviews > 0" class="bg-[#1a1a1a] border border-white/5 rounded-xl p-6 mb-6">
          <p class="text-sm font-bold text-white mb-4 uppercase tracking-wider">Phân phối đánh giá</p>
          <div class="space-y-2">
            <button v-for="d in ratingDistribution" :key="d.star"
                    @click="reviewFilter = reviewFilter === d.star ? 0 : d.star"
                    class="w-full flex items-center gap-3 group"
                    :class="reviewFilter === d.star ? 'opacity-100' : 'opacity-90 hover:opacity-100'">
              <span class="flex items-center gap-1 w-12 shrink-0 text-xs font-bold"
                    :class="reviewFilter === d.star ? 'text-[#f5c518]' : 'text-gray-400'">
                {{ d.star }} <span class="material-symbols-outlined text-[14px]" style="font-variation-settings: 'FILL' 1;">star</span>
              </span>
              <div class="flex-1 h-2.5 rounded-full bg-white/10 overflow-hidden">
                <div class="h-full rounded-full bg-[#f5c518] transition-all duration-300" :style="{ width: d.percent + '%' }"></div>
              </div>
              <span class="w-10 shrink-0 text-right text-xs text-gray-400">{{ d.count }}</span>
            </button>
          </div>
          <p v-if="reviewFilter" class="text-[11px] text-gray-400 mt-3">
            Đang lọc theo {{ reviewFilter }} sao —
            <button @click="reviewFilter = 0" class="text-[#f5c518] hover:underline font-semibold">bỏ lọc</button>
          </p>
        </div>

        <!-- Trạng thái 1: Chưa đăng nhập → khung mời đăng nhập -->
        <button v-if="!isLoggedIn"
                @click="showLoginModal = true"
                class="w-full text-left bg-[#1a1a1a] border border-dashed border-white/15 rounded-xl p-6 mb-10 hover:border-[#f5c518]/50 transition-colors group">
          <p class="text-sm font-bold text-white mb-2 uppercase tracking-wider">Chia sẻ cảm nhận của bạn</p>
          <p class="text-sm text-gray-400 group-hover:text-gray-300 transition-colors">
            Vui lòng <span class="text-[#f5c518] font-semibold">đăng nhập</span> để chia sẻ cảm nhận của bạn về bộ phim.
          </p>
        </button>

        <!-- Trạng thái 2: Đã đăng nhập nhưng chưa mua vé → banner + CTA mua vé -->
        <div v-else-if="eligibilityLoaded && !canReview"
             class="bg-[#f5c518]/10 border border-[#f5c518]/30 rounded-xl p-6 mb-10">
          <div class="flex items-start gap-4">
            <span class="material-symbols-outlined text-[#f5c518] text-2xl shrink-0">lock</span>
            <div class="flex-1">
              <p class="text-sm font-bold text-white mb-1">Chỉ dành cho khán giả đã xem phim</p>
              <p class="text-sm text-gray-300">
                Tính năng đánh giá chỉ dành cho khán giả đã mua vé xem phim này tại DevCine.
              </p>
              <button @click="goToShowtimes"
                      class="mt-4 inline-flex items-center gap-2 bg-[#f5c518] text-black font-bold text-xs uppercase tracking-widest px-6 py-2.5 rounded-lg hover:brightness-110 transition-all">
                <span class="material-symbols-outlined text-base">local_activity</span>
                Mua vé ngay
              </button>
            </div>
          </div>
        </div>

        <!-- Trạng thái 3: Đủ điều kiện → form đánh giá đầy đủ -->
        <div v-else-if="canReview" class="bg-[#1a1a1a] border border-white/5 rounded-xl p-6 mb-10">
          <p class="text-sm font-bold text-white mb-3 uppercase tracking-wider">
            {{ hasReviewed ? 'Cập nhật đánh giá của bạn' : 'Chia sẻ cảm nhận của bạn' }}
          </p>
          <div class="flex items-center gap-2 mb-1 w-fit rounded-lg transition-all"
               :class="ratingError ? 'ring-1 ring-red-500 px-2 py-1 -mx-2' : ''">
            <span v-for="i in 5" :key="i"
                  @click="selectRating(i)"
                  @mouseenter="hoverRating = i"
                  @mouseleave="hoverRating = 0"
                  class="material-symbols-outlined text-3xl cursor-pointer transition-transform hover:scale-110"
                  :class="i <= (hoverRating || myRating) ? 'text-[#f5c518]' : (ratingError ? 'text-red-500/60' : 'text-white/20')"
                  style="font-variation-settings: 'FILL' 1;">star</span>
            <span class="text-xs ml-2" :class="ratingError ? 'text-red-400' : 'text-gray-400'">{{ myRating > 0 ? myRating + '/5 sao' : 'Chọn số sao' }}</span>
          </div>
          <p v-if="ratingError" class="text-xs text-red-400 mb-3">Vui lòng chọn số sao đánh giá.</p>
          <div :class="ratingError ? '' : 'mt-4'">
            <textarea v-model="myComment" @input="onCommentInput" rows="3" placeholder="Viết nhận xét của bạn về bộ phim..."
                      class="w-full bg-[#262626] border rounded-lg p-4 text-sm text-white placeholder:text-gray-600 focus:outline-none transition-colors"
                      :class="commentError ? 'border-red-500 focus:border-red-500' : 'border-white/10 focus:border-[#f5c518]/50'"></textarea>
            <p v-if="commentError" class="text-xs text-red-400 mt-1">Vui lòng nhập nội dung đánh giá.</p>
          </div>

          <div class="flex justify-end mt-4">
            <button @click="submitReview" :disabled="submittingReview"
                    class="bg-[#f5c518] text-black font-bold text-xs uppercase tracking-widest px-8 py-3 rounded-lg hover:brightness-110 transition-all disabled:opacity-60">
              {{ submittingReview ? 'Đang gửi...' : (hasReviewed ? 'Cập nhật đánh giá' : 'Gửi đánh giá') }}
            </button>
          </div>
        </div>
        <!-- /Form đánh giá động -->

        <!-- Danh sách đánh giá -->
        <div v-if="filteredReviews.length > 0" class="space-y-5">
          <div v-for="rv in filteredReviews" :key="rv.id" class="border-b border-white/5 pb-5 last:border-0">
            <div class="flex items-center justify-between mb-2">
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-full bg-[#f5c518]/15 flex items-center justify-center text-[#f5c518] font-bold text-xs">
                  {{ (rv.customerName || 'K').charAt(0).toUpperCase() }}
                </div>
                <div>
                  <p class="text-sm font-bold text-white">{{ rv.customerName }}</p>
                  <div class="flex">
                    <span v-for="i in 5" :key="i" class="material-symbols-outlined text-[13px]"
                          :class="i <= rv.rating ? 'text-[#f5c518]' : 'text-white/20'"
                          style="font-variation-settings: 'FILL' 1;">star</span>
                  </div>
                </div>
              </div>
              <span class="text-[11px] text-gray-500">{{ formatReviewDate(rv.createdAt) }}</span>
            </div>
            <p v-if="rv.comment" class="text-sm text-gray-300 leading-relaxed pl-12">{{ rv.comment }}</p>
          </div>
        </div>
        <div v-else-if="reviewFilter" class="text-center py-10 text-gray-500 text-sm">
          Không có đánh giá {{ reviewFilter }} sao nào.
          <button @click="reviewFilter = 0" class="text-[#f5c518] hover:underline font-semibold ml-1">Xem tất cả</button>
        </div>
        <div v-else class="text-center py-10 text-gray-500 text-sm">
          Chưa có đánh giá nào. Hãy là người đầu tiên đánh giá bộ phim này!
        </div>
      </div>
    </section>

    <!-- Modal Trailer -->
    <TrailerModal :show="showTrailer" :url="movie.trailerUrl" @close="showTrailer = false" />

    <!-- Modal yêu cầu đăng nhập để đánh giá -->
    <Teleport to="body">
      <Transition name="login-modal">
        <div
          v-if="showLoginModal"
          @click.self="dismissLoginModal"
          class="fixed inset-0 z-[999] bg-black/85 backdrop-blur-sm flex items-center justify-center p-4"
        >
          <div class="relative w-full max-w-[420px] bg-[#1a1a1a] border border-white/10 rounded-2xl shadow-2xl p-6 text-center">
            <div class="mx-auto mb-4 w-14 h-14 flex items-center justify-center rounded-full bg-[#f5c518]/15 text-[#f5c518]">
              <span class="material-symbols-outlined text-3xl">lock</span>
            </div>
            <h3 class="text-lg font-bold text-white">Cần đăng nhập</h3>
            <p class="mt-2 text-sm text-gray-400">
              Vui lòng đăng nhập để gửi đánh giá cho phim này.
            </p>
            <div class="mt-6 flex flex-col-reverse sm:flex-row gap-3">
              <button
                @click="dismissLoginModal"
                class="flex-1 px-4 py-2.5 rounded-lg border border-white/15 text-gray-300 font-semibold hover:bg-white/5 transition-colors"
              >
                Để sau
              </button>
              <button
                @click="goToLogin"
                class="flex-1 px-4 py-2.5 rounded-lg bg-[#f5c518] text-black font-semibold hover:bg-[#e0b200] transition-colors"
              >
                Đăng nhập
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </main>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
.login-modal-enter-active, .login-modal-leave-active { transition: opacity 0.2s ease; }
.login-modal-enter-from, .login-modal-leave-to { opacity: 0; }
</style>
