<script setup>
defineProps({
  movie: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <div class="group relative bg-surface-container-low rounded-xl overflow-hidden border border-outline-variant/10 hover:border-primary/50 transition-all duration-500 hover:shadow-[0_20px_50px_-15px_rgba(245,197,24,0.15)]">
    <!-- Poster Image -->
    <div class="aspect-[2/3] overflow-hidden relative hover-shine-effect">
      <img :src="movie.poster || 'https://placehold.co/400x600/1a1a1a/f5c518?text=DevCine'" 
           class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" 
           :alt="movie.title">
      
      <!-- Overlay Gradient -->
      <div class="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent opacity-60 group-hover:opacity-80 transition-opacity"></div>
      
      <!-- Rating Badge -->
      <div class="absolute top-4 left-4 bg-black/60 backdrop-blur-md border border-white/10 px-2 py-1 rounded text-[10px] font-black tracking-widest text-primary">
        {{ movie.rating || 'T18' }}
      </div>
    </div>

    <!-- Movie Info -->
    <div class="p-5">
      <div class="flex items-center gap-2 mb-2">
        <span class="material-symbols-outlined text-primary text-sm">calendar_month</span>
        <span class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">{{ movie.releaseDate || 'Sắp chiếu' }}</span>
      </div>
      <h3 class="text-sm font-black uppercase tracking-tight text-on-surface line-clamp-1 group-hover:text-primary transition-colors">{{ movie.title || 'Untitled Movie' }}</h3>
      <p class="text-[10px] text-on-surface-variant mt-1 line-clamp-1 font-medium">{{ movie.genre || 'Hành động, Viễn tưởng' }}</p>
    </div>

    <!-- Quick Action Hover -->
    <div class="absolute inset-x-0 bottom-0 p-4 translate-y-full group-hover:translate-y-0 transition-transform duration-300 bg-surface/90 backdrop-blur-md">
      <router-link :to="`/movie/${movie.id}`" class="w-full py-3 bg-primary text-black text-[10px] font-black uppercase tracking-[0.2em] flex items-center justify-center gap-2 hover:bg-primary/90 transition-colors">
        Đặt vé ngay
        <span class="material-symbols-outlined text-sm">local_activity</span>
      </router-link>
    </div>
  </div>
</template>
