<script setup>
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import api from '@/api/axios'


const movies = ref([])
const loading = ref(true)

const fetchMovies = async () => {
  try {
    const response = await api.get('/movies')
    movies.value = response.data
  } catch (error) {
    console.error('Error fetching movies:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchMovies()
})

const nowShowingMovies = computed(() => movies.value.filter(m => m.status === 'active'))
const upcomingMovies = computed(() => movies.value.filter(m => m.status === 'upcoming'))
const featuredMovie = computed(() => nowShowingMovies.value[0] || movies.value[0])

const getGenreNames = (movie) => {
  if (!movie.genres) return 'ĐANG CẬP NHẬT'
  return movie.genres.map(g => g.name).join(', ').toUpperCase()
}
</script>

<template>
  <div>
    <!-- Hero Banner -->
    <section v-if="featuredMovie" class="relative h-screen w-full overflow-hidden">
      <div class="absolute inset-0 bg-cover bg-center transition-transform duration-700 scale-105" 
           :style="{ backgroundImage: `url(${featuredMovie.posterUrl || '/images/Hopper.webp'})` }">
        <div class="absolute inset-0 bg-gradient-to-t from-surface via-surface/40 to-transparent"></div>
        <div class="absolute inset-0 bg-gradient-to-r from-surface via-transparent to-transparent"></div>
      </div>
      <div class="relative z-10 h-full flex flex-col justify-center px-10 max-w-[1440px] mx-auto">
        <span class="text-primary-container font-label text-xs font-bold tracking-[0.2em] mb-4 uppercase">ĐANG CHIẾU TẠI RẠP</span>
        <h1 class="font-headline text-7xl md:text-8xl font-extrabold text-white tracking-tighter mb-6 leading-none max-w-3xl uppercase">
          {{ featuredMovie.titleVietnamese || featuredMovie.title }}
        </h1>
        <p class="text-on-surface-variant text-lg max-w-xl mb-10 leading-relaxed font-body line-clamp-3">
          {{ featuredMovie.description || 'Trải nghiệm điện ảnh đỉnh cao cùng những siêu phẩm bom tấn tại DevCine.' }}
        </p>
        <div class="flex items-center space-x-4">
          <button class="bg-primary-container text-on-primary px-10 py-4 rounded-lg font-headline font-bold flex items-center space-x-3 hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-primary-container/10">
            <span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
            <span>XEM TRAILER</span>
          </button>
          <router-link :to="`/movie/${featuredMovie.id}`" class="border border-outline-variant text-white px-10 py-4 rounded-lg font-headline font-bold hover:bg-white/10 active:scale-95 transition-all inline-block">
            ĐẶT VÉ NGAY
          </router-link>
        </div>
      </div>
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
              <router-link :to="`/movie/${movie.id}`" v-for="movie in nowShowingMovies" :key="movie.id" class="group cursor-pointer block">
                <div class="relative aspect-[2/3] overflow-hidden rounded-2xl mb-4 border border-white/5 shadow-xl glass-shine-edge">
                  <img :alt="movie.title" crossorigin="anonymous" class="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110" 
                       :src="movie.posterUrl || '/images/Hopper.webp'"/>
                  <span class="absolute top-3 left-3 bg-error-container text-white text-[10px] font-bold px-2 py-1 rounded">{{ movie.ageRating }}</span>
                </div>
                <span class="text-[#f5c518] text-[11px] font-bold uppercase tracking-wider block mb-1">{{ getGenreNames(movie) }}</span>
                <div class="font-headline text-lg font-bold text-white mb-2 uppercase tracking-tight line-clamp-1 group-hover:text-primary-container transition-colors">
                  {{ movie.titleVietnamese || movie.title }}
                </div>
                <div class="flex justify-between items-center text-sm text-on-surface-variant/80 font-normal">
                  <span>{{ movie.versionType || movie.format || 'Phụ đề' }}</span>
                  <span>{{ movie.durationMins ? movie.durationMins + ' phút' : '' }}</span>
                </div>
              </router-link>
            </div>
          </section>
        </div>

        <!-- Right Column: Sidebar -->
        <aside class="lg:w-[18%] space-y-12">
          <div>
            <h2 class="font-headline text-lg font-bold tracking-tight mb-8 border-l-4 border-primary-container pl-4 uppercase">KHUYẾN MẠI</h2>
            <div class="space-y-6">
              <div class="group cursor-pointer overflow-hidden rounded-xl glass-card glass-shine-edge">
                <img alt="Promo Banner" class="w-full aspect-[16/10] object-cover transition-transform duration-500 group-hover:scale-105" src="https://files.betacinemas.vn/files/media/images/2024/05/10/banner-web-le-hoi-102555-100524.jpg"/>
                <div class="p-2.5">
                  <h4 class="font-headline font-bold text-white uppercase mb-1 text-[9px] leading-tight">COMBO HÈ RỰC RỠ</h4>
                  <p class="text-on-surface-variant text-[8px] leading-snug">Giảm ngay 20% khi mua kèm 2 vé xem phim.</p>
                </div>
              </div>
              <div class="group cursor-pointer overflow-hidden rounded-xl glass-card glass-shine-edge" style="animation-delay: 4s">
                <img alt="Promo Banner" class="w-full aspect-[16/10] object-cover transition-transform duration-500 group-hover:scale-105" src="https://files.betacinemas.vn/files/media/images/2024/04/24/banner-web-hoc-sinh-113522-240424.jpg"/>
                <div class="p-2.5">
                  <h4 class="font-headline font-bold text-white uppercase mb-1 text-[9px] leading-tight">ƯU ĐÃI HỌC SINH</h4>
                  <p class="text-on-surface-variant text-[8px] leading-snug">Đồng giá vé chỉ 45k cho HSSV vào ngày thường.</p>
                </div>
              </div>
            </div>
          </div>
          <div class="glass-card rounded-xl p-4">
            <h3 class="font-headline font-bold text-[#f5c518] mb-3 uppercase text-[10px]">GIỜ VÀNG GIÁ VÉ</h3>
            <p class="text-on-surface-variant leading-relaxed mb-4 text-[8px]">Mọi suất chiếu trước 12:00 sáng Thứ Hai đến Thứ Năm chỉ với 50.000 VNĐ.</p>
            <router-link to="/contact" class="w-full border border-primary-container text-primary-container font-headline text-[8px] font-bold rounded-md hover:bg-primary-container hover:text-on-primary transition-colors uppercase py-1.5 inline-block text-center">XEM CHI TIẾT</router-link>
          </div>
        </aside>
      </div>
      <!-- Upcoming Movies Section -->
      <section class="py-10">
        <div class="flex items-center space-x-4 mb-12">
          <h2 class="font-headline text-4xl font-extrabold tracking-tighter uppercase whitespace-nowrap">SẮP RA MẮT</h2>
          <div class="h-[1px] w-full bg-outline-variant/30"></div>
        </div>
        <div class="grid lg:grid-cols-2 gap-12">
          <div class="space-y-12">
            <div v-if="upcomingMovies.length">
              <span class="text-primary-container font-headline text-sm font-bold tracking-widest uppercase mb-4 block">SẮP KHỞI CHIẾU</span>
              <p class="text-on-surface-variant text-lg leading-relaxed max-w-md italic">
                "Những kiệt tác điện ảnh chuẩn bị được vén màn, hứa hẹn một mùa lễ hội bùng nổ cảm xúc tại DevCine."
              </p>
            </div>
            <div class="space-y-8">
              <div v-for="(movie, index) in upcomingMovies.slice(0, 3)" :key="movie.id" 
                   class="group cursor-pointer border-b border-outline-variant/20 pb-8 transition-colors hover:border-primary-container">
                <div class="flex items-baseline space-x-6">
                  <span class="font-headline text-2xl font-bold text-primary-container/40 group-hover:text-primary-container transition-colors">0{{ index + 1 }}</span>
                  <div>
                    <h3 class="font-headline text-xl font-bold text-white uppercase group-hover:text-primary-container transition-colors">
                      {{ movie.titleVietnamese || movie.title }}
                    </h3>
                    <p class="text-xs text-on-surface-variant mt-1 uppercase tracking-wider font-semibold">Khởi chiếu: Đang cập nhật</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="flex gap-4 h-[500px]" v-if="upcomingMovies.length >= 2">
            <div class="flex-1 rounded-2xl overflow-hidden shadow-2xl border border-white/5">
              <img :alt="upcomingMovies[0].title" class="w-full h-full object-cover" :src="upcomingMovies[0].posterUrl || '/images/Hopper.webp'"/>
            </div>
            <div class="flex-1 rounded-2xl overflow-hidden shadow-2xl mt-12 border border-white/5">
              <img :alt="upcomingMovies[1].title" class="w-full h-full object-cover" :src="upcomingMovies[1].posterUrl || '/images/Hopper.webp'"/>
            </div>
          </div>
        </div>
      </section>

      <!-- Special Screenings Section -->
      <section class="mt-20">
        <div class="relative w-full rounded-2xl overflow-hidden bg-black/40 backdrop-blur-md shadow-2xl flex flex-col md:flex-row border border-white/10">
          <div class="md:w-1/2 relative min-h-[400px]">
            <img alt="Special Screening Interior" class="absolute inset-0 w-full h-full object-cover" src="/images/Hopper.webp"/>
            <div class="absolute top-6 left-6 bg-primary-container text-on-primary px-4 py-1.5 font-headline font-black text-[10px] tracking-widest uppercase rounded">
              SNEAK PREVIEW
            </div>
          </div>
          <div class="md:w-1/2 p-12 md:p-16 flex flex-col justify-center space-y-8">
            <div>
              <span class="text-primary-container font-headline text-sm font-bold tracking-[0.2em] uppercase mb-2 block">TRẢI NGHIỆM SỚM</span>
              <h2 class="font-headline text-5xl font-black text-white uppercase tracking-tighter leading-none">CHUYẾN TÀU ĐỊNH MỆNH</h2>
            </div>
            <p class="text-on-surface-variant text-lg leading-relaxed max-w-lg">
              Đừng bỏ lỡ cơ hội trở thành những khán giả đầu tiên tại Việt Nam được trải nghiệm siêu phẩm hành động kịch tính này trước ngày khởi chiếu chính thức.
            </p>
            <div class="flex flex-wrap gap-6 border-t border-b border-outline-variant/20 py-6">
              <div class="flex items-center space-x-2">
                <span class="material-symbols-outlined text-primary-container">calendar_today</span>
                <span class="text-sm font-bold text-white">20.12.2024</span>
              </div>
              <div class="flex items-center space-x-2">
                <span class="material-symbols-outlined text-primary-container">schedule</span>
                <span class="text-sm font-bold text-white">19:00 & 21:30</span>
              </div>
              <div class="flex items-center space-x-2">
                <span class="material-symbols-outlined text-primary-container">location_on</span>
                <span class="text-sm font-bold text-white">Toàn hệ thống</span>
              </div>
            </div>
            <div>
              <button class="bg-primary-container text-on-primary px-10 py-4 rounded-lg font-headline font-extrabold text-sm hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-primary-container/20">
                ĐẶT CHỖ TRƯỚC
              </button>
            </div>
          </div>
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
                <p class="text-on-surface-variant text-sm leading-relaxed mb-8 flex-grow">Trải nghiệm ẩm thực 5 sao được phục vụ trực tiếp tại phòng chiếu hạng Gold Class.</p>
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
      <section class="relative py-32 px-10 overflow-hidden">
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
            Tại DevCine, chúng tôi tin rằng mỗi bộ phim không chỉ là sự giải trí, mà là một kiệt tác nghệ thuật cần được thưởng thức trong không gian hoàn hảo nhất. Từ công nghệ trình chiếu IMAX tân tiến đến dịch vụ Gold Class chuẩn mực, chúng tôi định nghĩa lại khái niệm đi xem rạp.
          </p>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12 mb-20">
            <div class="space-y-4">
              <span class="material-symbols-outlined text-4xl text-primary-container">center_focus_strong</span>
              <h4 class="font-headline text-lg font-bold text-white uppercase tracking-tight">CÔNG NGHỆ IMAX</h4>
              <p class="text-sm text-on-surface-variant leading-relaxed">Màn hình cực đại với độ phân giải siêu sắc nét và âm thanh choáng ngợp.</p>
            </div>
            <div class="space-y-4">
              <span class="material-symbols-outlined text-4xl text-primary-container">airline_seat_recline_extra</span>
              <h4 class="font-headline text-lg font-bold text-white uppercase tracking-tight">GOLD CLASS</h4>
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
  </div>
</template>
