<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '@/api/axios'

const cinemas = ref([])
const loading = ref(true)
const userLocation = ref(null)
const selectedCity = ref('Tất cả')

const fetchCinemas = async () => {
  try {
    const response = await api.get('/cinemas')
    cinemas.value = response.data
  } catch (error) {
    console.error('Error fetching cinemas:', error)
  } finally {
    loading.value = false
  }
}

const getUserLocation = () => {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        userLocation.value = {
          lat: position.coords.latitude,
          lng: position.coords.longitude
        }
      },
      (error) => {
        console.warn('Geolocation error:', error)
      }
    )
  }
}

const calculateDistance = (lat1, lon1, lat2, lon2) => {
  if (!lat1 || !lon1 || !lat2 || !lon2) return Infinity
  const R = 6371 // Radius of the earth in km
  const dLat = (lat2 - lat1) * (Math.PI / 180)
  const dLon = (lon2 - lon1) * (Math.PI / 180)
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c // Distance in km
}

const nearCinemas = computed(() => {
  if (!cinemas.value?.length) return []
  if (!userLocation.value) return cinemas.value.filter(c => c?.type?.includes('Premium') || c?.type?.includes('IMAX')).slice(0, 3)
  return [...cinemas.value]
    .map(c => ({
      ...c,
      distance: calculateDistance(userLocation.value.lat, userLocation.value.lng, c.latitude, c.longitude)
    }))
    .sort((a, b) => (a.distance || Infinity) - (b.distance || Infinity))
    .slice(0, 3)
})

const cities = computed(() => {
  if (!cinemas.value?.length) return ['Tất cả']
  const allCities = cinemas.value.map(c => c.city).filter(Boolean)
  return ['Tất cả', ...new Set(allCities)]
})

const filteredCinemas = computed(() => {
  if (selectedCity.value === 'Tất cả') return cinemas.value
  return cinemas.value.filter(c => c.city === selectedCity.value)
})

onMounted(() => {
  fetchCinemas()
  getUserLocation()
})
</script>

<template>
  <main class="min-h-screen pt-32 pb-20 text-on-surface">
    <!-- Hero Section with Background Glow -->
    <section class="max-w-[1440px] mx-auto px-6 md:px-10 mb-20 relative overflow-hidden">
      <div class="absolute top-0 right-0 w-[500px] h-[500px] bg-primary/10 blur-[120px] -z-10 rounded-full"></div>
      <div class="absolute bottom-0 left-0 w-[300px] h-[300px] bg-secondary/5 blur-[80px] -z-10 rounded-full"></div>
      
      <div class="flex flex-col md:flex-row md:items-end justify-between gap-8 border-b border-outline-variant/30 pb-12">
        <div class="max-w-3xl">
          <span class="text-primary font-bold tracking-[0.3em] uppercase text-xs mb-4 block">Hệ thống rạp chiếu</span>
          <h1 class="text-5xl md:text-7xl font-bold font-headline tracking-tighter mb-6 leading-[0.9]">
            Trải nghiệm <br/><span class="text-primary italic">Điện ảnh</span> Đẳng cấp
          </h1>
          <p class="text-on-surface-variant text-lg md:text-xl leading-relaxed max-w-xl">
            Sở hữu công nghệ IMAX® và âm thanh Dolby Atmos® hiện đại nhất, DevCine mang đến không gian giải trí sang trọng và tinh tế cho mọi tín đồ điện ảnh.
          </p>
        </div>
        <div class="flex gap-4">
          <div class="p-4 border border-outline-variant/50 rounded-2xl bg-surface-container-low/50 backdrop-blur-md">
            <div class="text-3xl font-bold text-primary">{{ cinemas.length }}</div>
            <div class="text-[0.625rem] uppercase tracking-widest text-on-surface-variant">Cụm rạp toàn quốc</div>
          </div>
          <div class="p-4 border border-outline-variant/50 rounded-2xl bg-surface-container-low/50 backdrop-blur-md">
            <div class="text-3xl font-bold text-primary">{{ cities.length - 1 }}</div>
            <div class="text-[0.625rem] uppercase tracking-widest text-on-surface-variant">Tỉnh thành phố</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Rạp gần bạn Section -->
    <section class="max-w-[1440px] mx-auto px-6 md:px-10 mb-24">
      <div class="flex items-center gap-4 mb-10">
        <h2 class="text-2xl md:text-3xl font-bold font-headline">Rạp gần bạn nhất</h2>
        <div class="h-px flex-grow bg-gradient-to-r from-outline-variant/50 to-transparent"></div>
        <div v-if="userLocation" class="flex items-center gap-2 text-xs text-primary bg-primary/10 px-3 py-1 rounded-full">
          <span class="material-symbols-outlined text-sm">my_location</span>
          Vị trí đã xác định
        </div>
      </div>

      <div v-if="loading" class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div v-for="i in 3" :key="i" class="h-[400px] bg-surface-container animate-pulse rounded-3xl"></div>
      </div>
      
      <div v-else class="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div v-for="cinema in nearCinemas" :key="cinema.id" 
          class="group relative overflow-hidden rounded-3xl bg-surface-container-low border border-outline-variant/20 hover:border-primary/50 transition-all duration-500 hover:-translate-y-2">
          <div class="aspect-[4/5] relative overflow-hidden">
            <div class="absolute inset-0 bg-gradient-to-t from-surface to-transparent opacity-80 z-10"></div>
            <img class="absolute inset-0 w-full h-full object-cover grayscale-[0.3] group-hover:grayscale-0 transition-all duration-700 group-hover:scale-110" 
              :src="`/images/Hopper.webp`" alt="Cinema image"/>
            
            <div class="absolute top-4 right-4 z-20">
              <span class="px-3 py-1 bg-surface-container-highest/80 backdrop-blur-md text-on-surface text-[0.625rem] font-bold uppercase tracking-widest rounded-full border border-outline-variant/30">
                {{ cinema.type }}
              </span>
            </div>

            <div class="absolute bottom-0 left-0 p-8 z-20 w-full">
              <div v-if="cinema.distance !== Infinity" class="text-primary font-bold text-sm mb-2">
                Cách bạn {{ cinema.distance.toFixed(1) }} km
              </div>
              <h3 class="text-2xl font-bold font-headline mb-3 text-on-surface group-hover:text-primary transition-colors">{{ cinema.name }}</h3>
              <p class="text-sm text-on-surface-variant line-clamp-2 mb-6 opacity-80">{{ cinema.address }}</p>
              
              <div class="flex items-center justify-between">
                <button class="text-xs font-bold uppercase tracking-widest border-b-2 border-primary pb-1 group-hover:tracking-[0.2em] transition-all duration-500">
                  Xem lịch chiếu
                </button>
                <div class="flex items-center gap-1 text-on-surface-variant">
                  <span class="material-symbols-outlined text-sm">phone</span>
                  <span class="text-[0.625rem] font-mono">{{ cinema.contactNumber }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Chọn rạp theo khu vực Section -->
    <section class="max-w-[1440px] mx-auto px-6 md:px-10">
      <div class="flex flex-col md:flex-row gap-12">
        <!-- Sticky Sidebar for Cities -->
        <aside class="md:w-64 shrink-0">
          <div class="sticky top-40">
            <h2 class="text-xl font-bold font-headline mb-8 uppercase tracking-widest text-on-surface-variant">Khu vực</h2>
            <div class="flex flex-col gap-2">
              <button 
                v-for="city in cities" :key="city"
                @click="selectedCity = city"
                class="text-left px-4 py-3 rounded-xl transition-all duration-300 flex items-center justify-between group"
                :class="selectedCity === city ? 'bg-primary text-on-primary shadow-lg shadow-primary/20' : 'hover:bg-surface-container-high'"
              >
                <span class="font-bold text-sm">{{ city }}</span>
                <span class="text-xs opacity-50 font-mono">{{ city === 'Tất cả' ? cinemas.length : cinemas.filter(c => c.city === city).length }}</span>
              </button>
            </div>
            
            <div class="mt-12 p-6 rounded-2xl bg-gradient-to-br from-primary/10 to-transparent border border-primary/20">
              <h4 class="font-bold text-sm mb-3">Tìm kiếm theo tên</h4>
              <div class="relative">
                <input type="text" placeholder="Tên rạp..." 
                  class="w-full bg-surface-container-high/50 border border-outline-variant/30 rounded-lg px-4 py-2 text-sm focus:outline-none focus:border-primary/50 transition-colors"/>
                <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-sm opacity-50">search</span>
              </div>
            </div>
          </div>
        </aside>

        <!-- Cinema List for selected region -->
        <div class="flex-grow">
          <div class="mb-10 flex justify-between items-end">
            <div>
              <h3 class="text-3xl font-bold font-headline mb-2">{{ selectedCity }}</h3>
              <p class="text-on-surface-variant text-sm">Hiển thị {{ filteredCinemas.length }} rạp chiếu tại khu vực này</p>
            </div>
            <div class="flex gap-2 p-1 bg-surface-container-low rounded-lg border border-outline-variant/20">
              <button class="p-2 rounded-md bg-surface-container-high text-primary shadow-sm"><span class="material-symbols-outlined text-sm">grid_view</span></button>
              <button class="p-2 rounded-md hover:bg-surface-container-high transition-colors"><span class="material-symbols-outlined text-sm">list</span></button>
            </div>
          </div>

          <div v-if="filteredCinemas.length === 0" class="py-20 text-center border-2 border-dashed border-outline-variant/30 rounded-3xl">
            <span class="material-symbols-outlined text-5xl opacity-20 mb-4 block">location_off</span>
            <p class="text-on-surface-variant italic">Không tìm thấy rạp nào tại khu vực này.</p>
          </div>

          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div v-for="cinema in filteredCinemas" :key="cinema.id" 
              class="flex gap-6 p-6 rounded-3xl bg-surface-container-low border border-outline-variant/10 hover:border-primary/30 transition-all duration-300 group">
              <div class="w-32 h-32 rounded-2xl overflow-hidden shrink-0 border border-outline-variant/20">
                <img :src="`/images/Hopper.webp`" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"/>
              </div>
              <div class="flex flex-col justify-between py-1">
                <div>
                  <div class="flex items-center gap-2 mb-1">
                    <h4 class="font-bold text-lg group-hover:text-primary transition-colors">{{ cinema.name }}</h4>
                    <span v-if="cinema.type?.includes('Premium')" class="material-symbols-outlined text-sm text-primary">verified</span>
                  </div>
                  <p class="text-[0.8rem] text-on-surface-variant line-clamp-2 leading-relaxed opacity-70">{{ cinema.address }}</p>
                </div>
                <div class="flex items-center gap-6 mt-4">
                  <div class="flex items-center gap-1.5 opacity-60">
                    <span class="material-symbols-outlined text-sm">meeting_room</span>
                    <span class="text-[0.7rem] font-bold uppercase tracking-widest">{{ cinema.totalRooms }} Phòng</span>
                  </div>
                  <button class="text-[0.65rem] font-bold uppercase tracking-widest text-primary hover:opacity-80 transition-opacity">
                    Thông tin rạp
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Interractive Map CTA -->
    <section class="max-w-[1440px] mx-auto px-6 md:px-10 mt-32">
      <div class="relative rounded-[40px] overflow-hidden bg-surface-container-highest p-12 md:p-20 flex flex-col md:flex-row items-center gap-16 border border-outline-variant/20">
        <div class="absolute inset-0 noir-gradient pointer-events-none opacity-60"></div>
        <div class="relative z-10 md:w-1/2">
          <span class="text-primary font-bold tracking-[0.2em] uppercase text-[0.65rem] mb-6 block">Bản đồ tương tác</span>
          <h2 class="text-4xl md:text-5xl font-bold font-headline mb-8 leading-[1.1]">Dễ dàng tìm rạp <br/>quanh bạn nhất</h2>
          <p class="text-on-surface-variant mb-10 text-lg leading-relaxed">
            Hệ thống bản đồ tích hợp GPS giúp bạn xác định lộ trình đến cụm rạp DevCine thuận tiện nhất, kèm theo thông tin chi tiết về các suất chiếu đang diễn ra.
          </p>
          <button class="px-12 py-5 bg-primary text-on-primary font-bold rounded-full shadow-2xl shadow-primary/30 hover:scale-105 transition-transform duration-300 flex items-center gap-3 group">
            Khám phá bản đồ
            <span class="material-symbols-outlined group-hover:translate-x-1 transition-transform">arrow_forward</span>
          </button>
        </div>
        <div class="md:w-1/2 aspect-video w-full relative group">
          <div class="absolute -inset-4 bg-primary/20 blur-3xl rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-1000"></div>
          <div class="relative h-full w-full rounded-3xl overflow-hidden border-4 border-surface-container-low shadow-2xl">
            <img src="/images/Hopper.webp" class="w-full h-full object-cover opacity-60 grayscale hover:grayscale-0 transition-all duration-1000"/>
            <div class="absolute inset-0 flex items-center justify-center">
              <div class="w-16 h-16 bg-primary text-on-primary rounded-full flex items-center justify-center animate-bounce shadow-2xl shadow-primary/50 border-4 border-surface">
                <span class="material-symbols-outlined">location_on</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>

.noir-gradient {
  background: radial-gradient(circle at center, transparent 0%, rgba(0, 0, 0, 0.8) 100%);
}

/* Custom scrollbar for better look */
::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(var(--primary-rgb), 0.2);
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(var(--primary-rgb), 0.5);
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
