<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import api from '@/api/axios'
import { promoArticleApi } from '@/api/customer/index'
import { formatDate, formatDateDot } from '@/utils/format'
import TrailerModal from '@/components/common/TrailerModal.vue'
import MovieCard from '@/components/customer/MovieCard.vue'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()
const router = useRouter()
const movies = ref([])
const loading = ref(true)
const showTrailer = ref(false)
const promoArticles = ref([])

// ===== Hero carousel: banner admin (2 chế độ) + fallback phim đang chiếu =====
const currentSlide = ref(0)
const activeTrailer = ref(null)
const banners = ref([])
const bannerMovies = ref({}) // movieId -> chi tiết phim (cho banner chế độ MOVIE)
let slideTimer = null

const goToSlide = (i) => {
  const n = slides.value.length
  if (!n) return
  currentSlide.value = (i + n) % n
  restartAutoSlide()
}
const nextSlide = () => goToSlide(currentSlide.value + 1)
const prevSlide = () => goToSlide(currentSlide.value - 1)

// Thời gian dừng ở mỗi slide: slide phát trailer nền -> 15s; còn lại -> 8s
const slideDelay = () => {
  const s = slides.value[currentSlide.value]
  return (canPlayBg && s && s.kind === 'movie' && ytId(s.movie?.trailerUrl)) ? 15000 : 8000
}
const restartAutoSlide = () => {
  clearTimeout(slideTimer)
  if (slides.value.length > 1) {
    slideTimer = setTimeout(() => {
      currentSlide.value = (currentSlide.value + 1) % slides.value.length
      restartAutoSlide()
    }, slideDelay())
  }
}

const openTrailer = (movie) => {
  if (!movie?.trailerUrl) return
  activeTrailer.value = movie
  showTrailer.value = true
}

// ===== Trailer nền (background video) cho slide phim =====
const showVideo = ref(false)   // true sau khi ảnh hiện ~1.2s -> trailer mờ vào
const muted = ref(true)
const videoEl = ref(null)
const setVideoRef = (el) => { videoEl.value = el }
const canPlayBg = typeof window !== 'undefined' && !window.matchMedia('(max-width: 768px)').matches
let videoTimer = null

const ytId = (url) => {
  if (!url) return ''
  let m = url.match(/youtu\.be\/([\w-]+)/); if (m) return m[1]
  m = url.match(/[?&]v=([\w-]+)/); if (m) return m[1]
  m = url.match(/embed\/([\w-]+)/); if (m) return m[1]
  return ''
}
const bgTrailerUrl = (movie) => {
  const id = ytId(movie?.trailerUrl)
  if (!id) return ''
  return `https://www.youtube.com/embed/${id}?autoplay=1&mute=1&loop=1&playlist=${id}&controls=0&modestbranding=1&rel=0&playsinline=1&disablekb=1&enablejsapi=1&iv_load_policy=3&fs=0`
}

// Mỗi khi đổi slide: ẩn video, reset tiếng, hẹn hiện lại nếu slide là phim có trailer (desktop)
const maybeStartVideo = () => {
  showVideo.value = false
  muted.value = true
  clearTimeout(videoTimer)
  const s = slides.value[currentSlide.value]
  if (canPlayBg && s && s.kind === 'movie' && ytId(s.movie?.trailerUrl)) {
    videoTimer = setTimeout(() => { showVideo.value = true }, 1200)
  }
}
watch(currentSlide, maybeStartVideo)

const toggleSound = () => {
  muted.value = !muted.value
  const win = videoEl.value?.contentWindow
  if (!win) return
  win.postMessage(JSON.stringify({ event: 'command', func: muted.value ? 'mute' : 'unMute', args: [] }), '*')
  if (!muted.value) win.postMessage(JSON.stringify({ event: 'command', func: 'setVolume', args: [70] }), '*')
}

// ===== Sneak Previews (Suất chiếu sớm) carousel =====
const sneakPreviews = ref([])
const currentSneakSlide = ref(0)
let sneakSlideTimer = null
let sneakTouchStartX = 0

const goToSneakSlide = (i) => {
  const n = sneakPreviews.value.length
  if (!n) return
  currentSneakSlide.value = (i + n) % n
  restartSneakAutoSlide()
}
const nextSneakSlide = () => goToSneakSlide(currentSneakSlide.value + 1)
const prevSneakSlide = () => goToSneakSlide(currentSneakSlide.value - 1)

const restartSneakAutoSlide = () => {
  clearTimeout(sneakSlideTimer)
  if (sneakPreviews.value.length > 1) {
    sneakSlideTimer = setTimeout(() => {
      currentSneakSlide.value = (currentSneakSlide.value + 1) % sneakPreviews.value.length
      restartSneakAutoSlide()
    }, 7000)
  }
}

const pauseSneakAutoSlide = () => {
  clearTimeout(sneakSlideTimer)
}

const onSneakTouchStart = (e) => {
  if (e.touches && e.touches[0]) {
    sneakTouchStartX = e.touches[0].clientX
  }
}

const onSneakTouchEnd = (e) => {
  if (e.changedTouches && e.changedTouches[0]) {
    const diff = sneakTouchStartX - e.changedTouches[0].clientX
    if (Math.abs(diff) > 40) {
      if (diff > 0) nextSneakSlide()
      else prevSneakSlide()
    }
  }
}

const navigateToSneakMovie = (item) => {
  if (!item?.movieId) return
  router.push({
    path: `/movie/${item.movieId}`,
    hash: '#showtimes-section',
    query: item.defaultDate ? { date: item.defaultDate } : {}
  })
}

const fetchSneakPreviews = async () => {
  try {
    const { data } = await api.get('/showtimes/sneak-previews')
    sneakPreviews.value = Array.isArray(data) ? data : (data.data ?? [])
  } catch (error) {
    console.error('Không tải được danh sách suất chiếu sớm', error)
  }
}

onUnmounted(() => {
  clearTimeout(slideTimer)
  clearTimeout(videoTimer)
  clearTimeout(sneakSlideTimer)
})

const fetchPromoArticles = async () => {
  try {
    const { data } = await promoArticleApi.getActive()
    promoArticles.value = Array.isArray(data) ? data : (data.data ?? [])
  } catch (error) {
    console.error('Không tải được tin khuyến mãi', error)
  }
}

const fetchMovies = async () => {
  try {
    const response = await api.get('/movies')
    movies.value = response.data
  } catch (error) {
    console.error('Error fetching movies:', error)
    toast.error(friendlyError(error, 'Không tải được danh sách phim.'))
  } finally {
    loading.value = false
  }
}

const fetchBanners = async () => {
  try {
    const { data } = await api.get('/banners/active')
    banners.value = Array.isArray(data) ? data : (data.data ?? [])
    // Tải chi tiết phim cho các banner chế độ MOVIE
    const movieIds = [...new Set(banners.value.filter(b => b.mode === 'MOVIE' && b.movieId).map(b => b.movieId))]
    const results = await Promise.allSettled(movieIds.map(id => api.get(`/movies/${id}`)))
    const map = {}
    results.forEach((r, idx) => { if (r.status === 'fulfilled') map[movieIds[idx]] = r.value.data })
    bannerMovies.value = map
  } catch (error) {
    console.error('Không tải được banner', error)
  }
}

onMounted(async () => {
  fetchPromoArticles()
  fetchSneakPreviews().then(() => restartSneakAutoSlide())
  await Promise.all([fetchMovies(), fetchBanners()])
  restartAutoSlide()
  maybeStartVideo()
})

const nowShowingMovies = computed(() => movies.value.filter(m => m.status === 'active'))
const upcomingMovies = computed(() => movies.value.filter(m => m.status === 'upcoming'))

// Slide hợp nhất: ưu tiên banner admin; nếu chưa có banner nào -> fallback phim đang chiếu
const slides = computed(() => {
  if (banners.value.length) {
    return banners.value
      .map(b => b.mode === 'MOVIE'
        ? (bannerMovies.value[b.movieId] ? { kind: 'movie', key: `b${b.id}`, movie: bannerMovies.value[b.movieId] } : null)
        : { kind: 'image', key: `b${b.id}`, imageUrl: b.imageUrl, link: b.link, title: b.title })
      .filter(Boolean)
  }
  return nowShowingMovies.value.slice(0, 6).map(m => ({ kind: 'movie', key: `m${m.id}`, movie: m }))
})

const onImageBannerClick = (link) => {
  if (!link) return
  if (/^https?:\/\//i.test(link)) window.open(link, '_blank', 'noopener')
  else router.push(link)
}

const getGenreNames = (movie) => {
  if (!movie.genres || !movie.genres.length) return 'ĐANG CẬP NHẬT'
  // Chỉ hiển thị tối đa 2 thể loại chính cho card gọn gàng, đều khung;
  // danh sách đầy đủ xem ở trang Chi tiết phim.
  return movie.genres.slice(0, 2).map(g => g.name).join(', ').toUpperCase()
}

// Phim chưa tới ngày chiếu -> chưa mở bán vé. Đồng bộ với admin (movie.status = 'upcoming').
const isUpcoming = (movie) => String(movie?.status).toLowerCase() === 'upcoming'
// Nhãn trạng thái hero lấy động theo trạng thái thực của phim (KHÔNG hardcode).
const heroStatusLabel = (movie) => isUpcoming(movie) ? 'Sắp khởi chiếu' : 'Đang chiếu tại rạp'
</script>

<template>
  <div>
    <!-- Hero carousel full màn hình: banner ảnh (nguyên bản) hoặc banner theo phim -->
    <section v-if="slides.length" class="relative h-screen w-full overflow-hidden group/hero">
      <div class="flex h-full transition-transform duration-500 ease-out" :style="{ transform: `translateX(-${currentSlide * 100}%)` }">
        <div v-for="(slide, i) in slides" :key="slide.key" class="relative w-full h-full shrink-0 overflow-hidden">

          <!-- CHẾ ĐỘ ẢNH: hiển thị nguyên ảnh, không phủ chữ -->
          <template v-if="slide.kind === 'image'">
            <img :src="slide.imageUrl" :alt="slide.title || 'Banner'"
                 class="w-full h-full object-cover" :class="slide.link ? 'cursor-pointer' : ''"
                 @click="onImageBannerClick(slide.link)" />
          </template>

          <!-- CHẾ ĐỘ THEO PHIM: backdrop phủ kín màn, chữ dồn bên trái (kiểu Chiếu Phim Quốc Gia) -->
          <template v-else>
            <!-- Nền: ưu tiên ảnh ngang (bannerUrl); nếu chỉ có poster dọc thì làm mờ cho đỡ vỡ -->
            <div class="absolute inset-0 bg-cover bg-center"
                 :class="slide.movie.bannerUrl ? 'scale-105' : 'blur-xl scale-110 opacity-70'"
                 :style="{ backgroundImage: `url(${slide.movie.bannerUrl || slide.movie.posterUrl || '/images/Hopper.webp'})` }"></div>

            <!-- Trailer nền: chỉ slide active + có trailer + desktop; ảnh hiện trước rồi video mờ vào -->
            <div v-if="i === currentSlide && showVideo && bgTrailerUrl(slide.movie)"
                 class="absolute inset-0 overflow-hidden transition-opacity duration-700"
                 :class="showVideo ? 'opacity-100' : 'opacity-0'">
              <iframe :ref="setVideoRef" :src="bgTrailerUrl(slide.movie)"
                      class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-screen h-[56.25vw] min-h-screen min-w-[177.78vh] pointer-events-none"
                      frameborder="0" allow="autoplay; encrypted-media" referrerpolicy="strict-origin-when-cross-origin"></iframe>
            </div>

            <div class="absolute inset-0 bg-gradient-to-r from-surface via-surface/85 to-surface/20"></div>
            <div class="absolute inset-0 bg-gradient-to-t from-surface via-surface/30 to-transparent"></div>

            <div class="relative z-10 h-full flex items-center px-10 md:px-16 max-w-[1440px] mx-auto">
              <div class="max-w-xl">
                <span class="text-primary-container font-label text-xs font-bold tracking-[0.2em] mb-4 uppercase block">{{ heroStatusLabel(slide.movie) }}</span>
                <router-link :to="`/movie/${slide.movie.id}`" class="block">
                  <!-- leading-[1.35]: chữ HOA tiếng Việt có dấu chồng (Ữ/Ệ/Ọ) cao 1.26em (Montserrat) -> leading
                       nhỏ hơn 1.3 sẽ bị overflow của line-clamp cắt mất dấu và để lọt mực dòng thứ 3 vào khung 2 dòng. -->
                  <h1 class="font-headline text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-extrabold text-white tracking-tight mb-5 leading-[1.35] uppercase line-clamp-2 hover:text-primary-container transition-colors">
                    {{ slide.movie.title }}
                  </h1>
                </router-link>
                <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-on-surface-variant text-sm mb-4">
                  <span v-if="slide.movie.genres && slide.movie.genres.length" class="text-primary-container font-bold uppercase tracking-wide text-xs">{{ getGenreNames(slide.movie) }}</span>
                  <span v-if="slide.movie.country">{{ slide.movie.country }}</span>
                  <span v-if="slide.movie.durationMins" class="flex items-center gap-1"><span class="material-symbols-outlined text-base">schedule</span>{{ slide.movie.durationMins }} phút</span>
                  <span v-if="slide.movie.ageRating" class="bg-error-container text-white text-[10px] font-bold px-2 py-0.5 rounded">{{ slide.movie.ageRating }}</span>
                </div>
                <p v-if="slide.movie.director" class="text-sm text-on-surface-variant mb-1"><span class="text-on-surface-variant/60">Đạo diễn:</span> {{ slide.movie.director }}</p>
                <p v-if="slide.movie.castMembers" class="text-sm text-on-surface-variant mb-4 line-clamp-1"><span class="text-on-surface-variant/60">Diễn viên:</span> {{ slide.movie.castMembers }}</p>
                <p v-if="slide.movie.description" class="text-base text-on-surface-variant/90 leading-relaxed line-clamp-3 mb-4">{{ slide.movie.description }}</p>
                <p v-if="slide.movie.releaseDate" class="text-sm text-on-surface-variant mb-8"><span class="text-on-surface-variant/60">Khởi chiếu:</span> {{ formatDateDot(slide.movie.releaseDate) }}</p>
                <div class="flex items-center gap-4 flex-wrap">
                  <!-- Đang chiếu hoặc có suất chiếu sớm: mua vé ngay.
                       Sắp chiếu chưa có suất: ưu tiên xem trailer, không có trailer thì xem chi tiết. -->
                  <router-link v-if="!isUpcoming(slide.movie) || slide.movie.hasEarlyScreening" :to="`/movie/${slide.movie.id}`" class="bg-primary-container text-on-primary px-10 py-4 rounded-lg font-headline font-bold flex items-center gap-2 hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-primary-container/10">
                    <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">confirmation_number</span> MUA VÉ NGAY
                  </router-link>
                  <button v-else-if="slide.movie.trailerUrl" @click="openTrailer(slide.movie)" class="bg-primary-container text-on-primary px-10 py-4 rounded-lg font-headline font-bold flex items-center gap-2 hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-primary-container/10">
                    <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">play_arrow</span> XEM TRAILER
                  </button>
                  <router-link v-else :to="`/movie/${slide.movie.id}`" class="bg-primary-container text-on-primary px-10 py-4 rounded-lg font-headline font-bold flex items-center gap-2 hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-primary-container/10">
                    <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">info</span> XEM CHI TIẾT
                  </router-link>
                  <!-- Nút trailer phụ: chỉ khi ĐANG CHIẾU (sắp chiếu đã đưa trailer lên nút chính).
                       Cũng hiện khi sắp chiếu có chiếu sớm + có trailer. -->
                  <button v-if="(!isUpcoming(slide.movie) || slide.movie.hasEarlyScreening) && slide.movie.trailerUrl" @click="openTrailer(slide.movie)" class="border border-outline-variant text-white px-10 py-4 rounded-lg font-headline font-bold hover:bg-white/10 active:scale-95 transition-all flex items-center gap-2">
                    <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">play_arrow</span> TRAILER
                  </button>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- Mũi tên 2 lề -->
      <template v-if="slides.length > 1">
        <button @click="prevSlide" aria-label="Slide trước"
                class="absolute left-6 top-1/2 -translate-y-1/2 z-20 w-12 h-12 rounded-full bg-black/30 backdrop-blur border border-white/10 text-white flex items-center justify-center opacity-60 hover:opacity-100 hover:bg-primary-container hover:text-on-primary transition-all">
          <span class="material-symbols-outlined">chevron_left</span>
        </button>
        <button @click="nextSlide" aria-label="Slide sau"
                class="absolute right-6 top-1/2 -translate-y-1/2 z-20 w-12 h-12 rounded-full bg-black/30 backdrop-blur border border-white/10 text-white flex items-center justify-center opacity-60 hover:opacity-100 hover:bg-primary-container hover:text-on-primary transition-all">
          <span class="material-symbols-outlined">chevron_right</span>
        </button>
      </template>

      <!-- Chấm chỉ slide -->
      <div v-if="slides.length > 1" class="absolute bottom-10 left-1/2 -translate-x-1/2 z-20 flex gap-2.5">
        <button v-for="(slide, i) in slides" :key="slide.key" @click="goToSlide(i)" :aria-label="`Slide ${i + 1}`"
                class="h-2 rounded-full transition-all"
                :class="i === currentSlide ? 'w-8 bg-primary-container' : 'w-2 bg-white/40 hover:bg-white/70'"></button>
      </div>

      <!-- Nút bật/tắt tiếng trailer nền -->
      <button v-if="showVideo" @click="toggleSound" :aria-label="muted ? 'Bật tiếng' : 'Tắt tiếng'"
              class="absolute bottom-9 right-8 z-20 w-11 h-11 rounded-full bg-black/40 backdrop-blur border border-white/10 text-white flex items-center justify-center hover:bg-primary-container hover:text-on-primary transition-all">
        <span class="material-symbols-outlined">{{ muted ? 'volume_off' : 'volume_up' }}</span>
      </button>
    </section>

    <!-- Main Content Area -->
    <main class="max-w-[1440px] mx-auto px-10 py-20">
      <div class="flex flex-col lg:flex-row gap-12">
        <!-- Left Column: Movies -->
        <div class="lg:w-[82%] space-y-20">
          <!-- PHIM ĐANG CHIẾU Section -->
          <section class="mt-[5px]">
            <div class="flex justify-between items-end mb-10">
              <h2 class="font-headline text-3xl font-bold tracking-tight uppercase">PHIM ĐANG CHIẾU</h2>
              <router-link class="text-primary-container font-label text-xs font-bold tracking-widest hover:underline uppercase" to="/search">Xem tất cả</router-link>
            </div>
            
            <div v-if="loading" class="grid grid-cols-2 md:grid-cols-4 gap-6">
              <div v-for="i in 4" :key="i" class="animate-pulse">
                <div class="aspect-[2/3] bg-surface-container-high rounded-2xl mb-4"></div>
                <div class="h-3 bg-surface-container-high rounded w-2/3 mb-2"></div>
                <div class="h-4 bg-surface-container-high rounded w-full"></div>
              </div>
            </div>

            <div v-else class="grid grid-cols-2 md:grid-cols-4 gap-6">
              <MovieCard v-for="movie in nowShowingMovies.slice(0, 12)" :key="movie.id" :movie="movie" :show-expired="false" />
            </div>
          </section>
        </div>

        <!-- Right Column: Sidebar -->
        <aside class="lg:w-[18%] space-y-12">
          <div v-if="promoArticles.length">
            <h2 class="font-headline text-lg font-bold tracking-tight mb-8 border-l-4 border-primary-container pl-4 uppercase">KHUYẾN MẠI</h2>
            <div class="space-y-6">
              <RouterLink v-for="promo in promoArticles.slice(0, 2)" :key="promo.id" to="/khuyen-mai" class="block group cursor-pointer overflow-hidden rounded-xl glass-card glass-shine-edge">
                <img alt="Promo Banner" class="w-full aspect-[16/10] object-cover transition-transform duration-500 group-hover:scale-105" :src="promo.imageUrl || '/images/Hopper.webp'"/>
                <div class="p-2.5">
                  <h4 class="font-headline font-bold text-white uppercase mb-1 text-[9px] leading-tight line-clamp-1">{{ promo.title }}</h4>
                  <p class="text-on-surface-variant text-[8px] leading-snug line-clamp-2">{{ promo.description }}</p>
                </div>
              </RouterLink>
            </div>
          </div>
          <div v-if="promoArticles[2]" class="glass-card rounded-xl p-4">
            <h3 class="font-headline font-bold text-[#f5c518] mb-3 uppercase text-[10px] line-clamp-1">{{ promoArticles[2].title }}</h3>
            <p class="text-on-surface-variant leading-relaxed mb-4 text-[8px] line-clamp-3">{{ promoArticles[2].description }}</p>
            <router-link :to="`/khuyen-mai/${promoArticles[2].id}`" class="w-full border border-primary-container text-primary-container font-headline text-[8px] font-bold rounded-md hover:bg-primary-container hover:text-on-primary transition-colors uppercase py-1.5 inline-block text-center">XEM CHI TIẾT</router-link>
          </div>
        </aside>
      </div>
      <!-- Upcoming Movies Section -->
      <section class="py-10">
        <div class="flex items-center space-x-4 mb-12">
          <h2 class="font-headline text-4xl font-extrabold tracking-tighter uppercase whitespace-nowrap">SẮP RA MẮT</h2>
          <div class="h-[1px] w-full bg-outline-variant/30"></div>
        </div>
        <div v-if="loading" class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
          <div v-for="i in 5" :key="i" class="animate-pulse">
            <div class="aspect-[2/3] bg-surface-container-high rounded-2xl mb-4"></div>
            <div class="h-3 bg-surface-container-high rounded w-2/3 mb-2"></div>
            <div class="h-4 bg-surface-container-high rounded w-full"></div>
          </div>
        </div>

        <div v-else class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-6">
          <MovieCard v-for="movie in upcomingMovies.slice(0, 12)" :key="movie.id" :movie="movie" :show-expired="false" />
        </div>
      </section>

      <!-- Special Screenings Section (Sneak Previews) -->
      <section v-if="sneakPreviews.length" class="mt-20 relative group/sneak"
               @mouseenter="pauseSneakAutoSlide"
               @mouseleave="restartSneakAutoSlide"
               @touchstart="onSneakTouchStart"
               @touchend="onSneakTouchEnd">
        <div class="relative w-full rounded-2xl overflow-hidden bg-black/50 backdrop-blur-xl shadow-2xl border border-white/10 glass-card">
          <!-- Slider Track -->
          <div class="flex transition-transform duration-700 ease-out"
               :style="{ transform: `translateX(-${currentSneakSlide * 100}%)` }">
            <div v-for="item in sneakPreviews" :key="item.movieId"
                 class="w-full flex-shrink-0 flex flex-col md:flex-row cursor-pointer select-none"
                 @click="navigateToSneakMovie(item)">
              
              <!-- Cột trái: Poster / Backdrop -->
              <div class="md:w-1/2 relative min-h-[380px] md:min-h-[460px] overflow-hidden group/img">
                <img :alt="item.title"
                     class="absolute inset-0 w-full h-full object-cover transition-transform duration-700 group-hover/img:scale-105"
                     :src="item.bannerUrl || item.posterUrl || '/images/Hopper.webp'" />
                <div class="absolute inset-0 bg-gradient-to-t md:bg-gradient-to-r from-black/80 via-black/20 to-transparent"></div>
                
                <!-- Badge Sneak Preview -->
                <div class="absolute top-6 left-6 bg-primary-container text-on-primary px-4 py-1.5 font-headline font-black text-[11px] tracking-widest uppercase rounded shadow-lg">
                  SNEAK PREVIEW
                </div>

                <div v-if="item.ageRating" class="absolute bottom-6 left-6 bg-error-container text-white px-3 py-1 font-bold text-xs rounded uppercase">
                  {{ item.ageRating }}
                </div>
              </div>

              <!-- Cột phải: Thông tin chi tiết -->
              <div class="md:w-1/2 p-8 md:p-14 flex flex-col justify-center space-y-6">
                <div>
                  <span class="text-primary-container font-headline text-sm font-bold tracking-[0.2em] uppercase mb-2 block">
                    TRẢI NGHIỆM SỚM
                  </span>
                  <h2 class="font-headline text-3xl md:text-5xl font-black text-white uppercase tracking-tighter leading-tight line-clamp-2">
                    {{ item.title }}
                  </h2>
                  <p v-if="item.titleVietnamese && item.titleVietnamese !== item.title" class="text-[#f5c518] text-sm font-bold mt-1 uppercase tracking-wide">
                    {{ item.titleVietnamese }}
                  </p>
                </div>

                <p class="text-on-surface-variant text-sm md:text-base leading-relaxed line-clamp-3">
                  {{ item.description || 'Đừng bỏ lỡ cơ hội trở thành những khán giả đầu tiên tại Việt Nam được trải nghiệm siêu phẩm đặc sắc này trước ngày khởi chiếu chính thức.' }}
                </p>

                <!-- 3 thông số có icon Material Symbols -->
                <div class="flex flex-wrap gap-4 md:gap-6 border-t border-b border-outline-variant/20 py-5">
                  <div v-if="item.formattedDates" class="flex items-center space-x-2">
                    <span class="material-symbols-outlined text-primary-container text-xl">calendar_today</span>
                    <span class="text-sm font-bold text-white">{{ item.formattedDates }}</span>
                  </div>
                  <div v-if="item.formattedTimes" class="flex items-center space-x-2">
                    <span class="material-symbols-outlined text-primary-container text-xl">schedule</span>
                    <span class="text-sm font-bold text-white">{{ item.formattedTimes }}</span>
                  </div>
                  <div class="flex items-center space-x-2">
                    <span class="material-symbols-outlined text-primary-container text-xl">location_on</span>
                    <span class="text-sm font-bold text-white">{{ item.locationSummary || 'Toàn hệ thống' }}</span>
                  </div>
                </div>

                <!-- Nút Đặt chỗ trước -->
                <div class="pt-2">
                  <button @click.stop="navigateToSneakMovie(item)"
                          class="bg-primary-container text-on-primary px-8 md:px-10 py-3.5 md:py-4 rounded-lg font-headline font-extrabold text-sm hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-primary-container/20 flex items-center gap-2 group/btn">
                    <span>ĐẶT CHỖ TRƯỚC</span>
                    <span class="material-symbols-outlined text-base transition-transform group-hover/btn:translate-x-1">arrow_forward</span>
                  </button>
                </div>
              </div>

            </div>
          </div>

          <!-- Nút điều hướng chuyển slide (Khi có >= 2 phim) -->
          <template v-if="sneakPreviews.length > 1">
            <button @click.stop="prevSneakSlide"
                    class="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 md:w-12 md:h-12 rounded-full bg-black/60 hover:bg-primary-container text-white hover:text-black border border-white/20 flex items-center justify-center transition-all duration-300 backdrop-blur-md opacity-80 hover:opacity-100 z-10"
                    aria-label="Previous Slide">
              <span class="material-symbols-outlined text-2xl">chevron_left</span>
            </button>
            <button @click.stop="nextSneakSlide"
                    class="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 md:w-12 md:h-12 rounded-full bg-black/60 hover:bg-primary-container text-white hover:text-black border border-white/20 flex items-center justify-center transition-all duration-300 backdrop-blur-md opacity-80 hover:opacity-100 z-10"
                    aria-label="Next Slide">
              <span class="material-symbols-outlined text-2xl">chevron_right</span>
            </button>

            <!-- Pagination Dots -->
            <div class="absolute bottom-4 right-6 md:right-10 flex items-center gap-2 z-10">
              <button v-for="(_, idx) in sneakPreviews" :key="idx"
                      @click.stop="goToSneakSlide(idx)"
                      class="h-2 rounded-full transition-all duration-300"
                      :class="currentSneakSlide === idx ? 'w-6 bg-primary-container' : 'w-2 bg-white/40 hover:bg-white/70'"
                      :aria-label="`Slide ${idx + 1}`" />
            </div>
          </template>
        </div>
      </section>

      <!-- Promo Tiles Section -->
      <section class="py-20 px-10 relative">
        <div class="absolute inset-0 bg-gradient-to-b from-transparent via-black/20 to-transparent pointer-events-none"></div>
        <div class="max-w-[1440px] mx-auto text-center relative z-10">
          <span class="text-primary-container font-headline text-sm font-bold tracking-[0.3em] uppercase mb-2 block">ĐẶC QUYỀN DEVCINE</span>
          <h2 class="font-headline text-4xl md:text-5xl font-black tracking-tighter mb-16 uppercase text-white">ƯU ĐÃI & KHUYẾN MẠI</h2>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-8 text-left">
            <div class="glass-card rounded-2xl overflow-hidden flex flex-col group transition-transform duration-300 hover:-translate-y-2">
              <div class="aspect-video relative overflow-hidden">
                <img alt="DevCine Elite Member" class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" src="/images/Hopper.webp"/>
              </div>
              <div class="p-8 flex flex-col flex-grow">
                <h3 class="font-headline text-xl font-bold text-white mb-4 uppercase tracking-tight">THẺ THÀNH VIÊN DEVCINE ELITE</h3>
                <p class="text-on-surface-variant text-sm leading-relaxed mb-8 flex-grow">Tích điểm nhận quà và hưởng các đặc quyền ưu tiên đặt chỗ, phòng chờ VIP cao cấp.</p>
                <a class="inline-flex items-center text-[#f5c518] font-headline font-bold text-xs uppercase tracking-widest group/link hover:opacity-80 transition-opacity" href="#">
                  KHÁM PHÁ NGAY
                  <span class="material-symbols-outlined ml-2 text-sm transition-transform duration-300 group-hover/link:translate-x-1">arrow_forward</span>
                </a>
              </div>
            </div>
            <div class="glass-card rounded-2xl overflow-hidden flex flex-col group transition-transform duration-300 hover:-translate-y-2">
              <div class="aspect-video relative overflow-hidden">
                <img alt="Finger Food Experience" class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" src="/images/Hopper.webp"/>
              </div>
              <div class="p-8 flex flex-col flex-grow">
                <h3 class="font-headline text-xl font-bold text-white mb-4 uppercase tracking-tight">TIFC FINGER FOOD TẠI CHỖ</h3>
                <p class="text-on-surface-variant text-sm leading-relaxed mb-8 flex-grow">Trải nghiệm ẩm thực 5 sao được phục vụ trực tiếp tại phòng chiếu hạng Cine Comfort.</p>
                <a class="inline-flex items-center text-[#f5c518] font-headline font-bold text-xs uppercase tracking-widest group/link hover:opacity-80 transition-opacity" href="#">
                  XEM THỰC ĐƠN
                  <span class="material-symbols-outlined ml-2 text-sm transition-transform duration-300 group-hover/link:translate-x-1">arrow_forward</span>
                </a>
              </div>
            </div>
            <div class="glass-card rounded-2xl overflow-hidden flex flex-col group transition-transform duration-300 hover:-translate-y-2">
              <div class="aspect-video relative overflow-hidden">
                <img alt="Family Day" class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" src="/images/Hopper.webp"/>
              </div>
              <div class="p-8 flex flex-col flex-grow">
                <h3 class="font-headline text-xl font-bold text-white mb-4 uppercase tracking-tight">NGÀY HỘI GIA ĐÌNH</h3>
                <p class="text-on-surface-variant text-sm leading-relaxed mb-8 flex-grow">Ưu đãi giảm 30% giá vé và miễn phí bắp nước cho các suất chiếu gia đình mỗi Chủ Nhật.</p>
                <a class="inline-flex items-center text-[#f5c518] font-headline font-bold text-xs uppercase tracking-widest group/link hover:opacity-80 transition-opacity" href="#">
                  ĐẶT VÉ GIA ĐÌNH
                  <span class="material-symbols-outlined ml-2 text-sm transition-transform duration-300 group-hover/link:translate-x-1">arrow_forward</span>
                </a>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Introduction/About Section -->
      <section id="about" class="relative py-32 px-10 overflow-hidden scroll-mt-32">
        <div class="absolute inset-0 z-0">
          <img alt="Cinema Background" class="w-full h-full object-cover opacity-20" src="/images/Hopper.webp"/>
          <div class="absolute inset-0 bg-gradient-to-b from-transparent via-black/60 to-black/80"></div>
        </div>
        <div class="relative z-10 max-w-[1440px] mx-auto text-center md:text-left">
          <div class="inline-flex items-center space-x-3 bg-white/5 backdrop-blur-md px-4 py-2 rounded-full border border-white/10 mb-8">
            <span class="material-symbols-outlined text-primary-container text-sm" style="font-variation-settings: 'FILL' 1;">stars</span>
            <span class="text-[10px] font-headline font-bold text-white/70 uppercase tracking-widest">CÂU CHUYỆN DEVCINE</span>
          </div>
          <h2 class="font-headline text-5xl md:text-7xl font-black text-white uppercase tracking-tighter leading-tight mb-8 max-w-4xl">
            TRẢI NGHIỆM ĐIỆN ẢNH<br/>THƯỢNG LƯU & ĐỘC BẢN
          </h2>
          <p class="text-on-surface-variant text-xl leading-relaxed max-w-2xl mb-16">
            Tại DevCine, chúng tôi tin rằng mỗi bộ phim không chỉ là sự giải trí, mà là một kiệt tác nghệ thuật cần được thưởng thức trong không gian hoàn hảo nhất. Từ phòng chiếu Superplex màn ảnh siêu lớn đến dịch vụ Cine Comfort chuẩn mực, chúng tôi định nghĩa lại khái niệm đi xem rạp.
          </p>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12 mb-20">
            <div class="space-y-4">
              <span class="material-symbols-outlined text-4xl text-primary-container">center_focus_strong</span>
              <h4 class="font-headline text-lg font-bold text-white uppercase tracking-tight">PHÒNG SUPERPLEX</h4>
              <p class="text-sm text-on-surface-variant leading-relaxed">Màn hình siêu lớn với độ phân giải siêu sắc nét và âm thanh choáng ngợp.</p>
            </div>
            <div class="space-y-4">
              <span class="material-symbols-outlined text-4xl text-primary-container">airline_seat_recline_extra</span>
              <h4 class="font-headline text-lg font-bold text-white uppercase tracking-tight">CINE COMFORT</h4>
              <p class="text-sm text-on-surface-variant leading-relaxed">Ghế sofa da cao cấp điều chỉnh điện, mang lại sự thoải mái tuyệt đối.</p>
            </div>
            <div class="space-y-4">
              <span class="material-symbols-outlined text-4xl text-primary-container">restaurant</span>
              <h4 class="font-headline text-lg font-bold text-white uppercase tracking-tight">ẨM THỰC CAO CẤP</h4>
              <p class="text-sm text-on-surface-variant leading-relaxed">Thực đơn đa dạng được chuẩn bị bởi các đầu bếp danh tiếng.</p>
            </div>
            <div class="space-y-4">
              <span class="material-symbols-outlined text-4xl text-primary-container">concierge</span>
              <h4 class="font-headline text-lg font-bold text-white uppercase tracking-tight">DỊCH VỤ TẬN TÂM</h4>
              <p class="text-sm text-on-surface-variant leading-relaxed">Đội ngũ quản gia chuyên nghiệp sẵn sàng phục vụ mọi nhu cầu của bạn.</p>
            </div>
          </div>
          <button class="bg-primary-container text-on-primary px-12 py-5 rounded-lg font-headline font-extrabold text-sm hover:opacity-90 active:scale-95 transition-all shadow-xl shadow-primary-container/10">
            TÌM HIỂU THÊM VỀ CHÚNG TÔI
          </button>
        </div>
      </section>
    </main>

    <!-- Modal Trailer phim nổi bật -->
    <TrailerModal :show="showTrailer" :url="activeTrailer?.trailerUrl" @close="showTrailer = false" />
  </div>
</template>
