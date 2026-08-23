<script setup>
import { computed } from 'vue'
import { formatDateDot } from '@/utils/format'

const props = defineProps({
  movie: {
    type: Object,
    required: true
  },
  // Trang chủ chỉ hiển thị phim đang chiếu/sắp chiếu (archived đã bị auto-sync loại bỏ),
  // nên tắt hẳn hiệu ứng "ĐÃ HẾT HẠN" ở đó cho gọn. Mặc định vẫn bật (dùng cho tìm kiếm...).
  showExpired: {
    type: Boolean,
    default: true
  }
})

const getGenreNames = (movie) => {
  if (!movie.genres || !movie.genres.length) return 'ĐANG CẬP NHẬT'
  return [...movie.genres].slice(0, 2).map(g => g.name).join(', ').toUpperCase()
}

const isExpired = computed(() => {
  if (!props.showExpired) return false
  if (!props.movie.endDate) return false
  return new Date(props.movie.endDate) < new Date()
})

const displayPoster = computed(() => props.movie.posterUrl || props.movie.poster || props.movie.poster_url || props.movie.posterBase64)

const formatMeta = computed(() => {
  const parts = []
  if (props.movie.versionType || props.movie.format) parts.push(props.movie.versionType || props.movie.format || 'Phụ đề')
  if (props.movie.durationMins) parts.push(`${props.movie.durationMins} phút`)
  return parts.join(' • ')
})
</script>

<template>
  <component 
    :is="isExpired ? 'div' : 'router-link'" 
    :to="isExpired ? undefined : `/movie/${movie.id}`" 
    class="group block" 
    :class="isExpired ? 'cursor-not-allowed opacity-70' : 'cursor-pointer'"
  >
    <div class="relative aspect-[2/3] overflow-hidden rounded-2xl mb-4 border border-white/5 shadow-xl glass-shine-edge"
         :class="!isExpired ? 'hover-shine-effect' : ''">
      
      <!-- Poster -->
      <img v-if="displayPoster" :alt="movie.title" crossorigin="anonymous" 
           class="w-full h-full object-cover transition-all duration-500" 
           :class="!isExpired ? 'group-hover:scale-110 group-hover:brightness-110' : 'grayscale'"
           :src="displayPoster"/>
      <div v-else class="w-full h-full flex items-center justify-center bg-surface-container-high">
        <span class="material-symbols-outlined text-4xl text-outline-variant">movie</span>
      </div>
      
      <!-- Overlay Gradient -->
      <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-transparent to-transparent opacity-0 transition-opacity"
           :class="!isExpired ? 'group-hover:opacity-100' : ''"></div>
           
      <!-- Badges -->
      <span v-if="movie.ageRating" class="absolute top-3 left-3 bg-error-container text-white text-[10px] font-bold px-2 py-1 rounded">{{ movie.ageRating }}</span>
      <div v-if="movie.rating" class="absolute top-3 right-3 flex items-center gap-1 bg-black/70 backdrop-blur px-2 py-0.5 rounded text-[10px] font-bold text-primary-container">
        <span class="material-symbols-outlined text-[12px]" style="font-variation-settings:'FILL' 1">star</span>{{ movie.rating }}
      </div>

      <!-- Expired Overlay -->
      <div v-if="isExpired" class="absolute inset-0 bg-black/60 flex items-center justify-center backdrop-blur-[2px]">
        <div class="bg-error px-3 py-1.5 rounded text-white font-bold text-[10px] tracking-widest uppercase border border-error-container">
          ĐÃ HẾT HẠN
        </div>
      </div>
    </div>
    
    <!-- Info -->
    <div class="flex justify-between items-center mb-1">
      <span class="text-[#f5c518] text-[11px] font-bold uppercase tracking-wider truncate mr-2">{{ getGenreNames(movie) }}</span>
      <span class="text-[#f5c518] text-[11px] font-bold tracking-widest whitespace-nowrap">{{ movie.releaseDate ? formatDateDot(movie.releaseDate) : 'Sắp chiếu' }}</span>
    </div>
    <div class="font-headline text-lg font-bold text-white mb-2 uppercase tracking-tight line-clamp-1 transition-colors"
         :class="!isExpired ? 'group-hover:text-primary-container' : ''">
      {{ movie.title }}
    </div>
    <div class="flex justify-between items-center text-[11px] text-on-surface-variant/80 font-normal">
      <span>{{ formatMeta }}</span>
    </div>
  </component>
</template>
