<script setup>
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import api from '@/api/axios'

const cinemas = ref([])
const loading = ref(true)
const selectedCity = ref('Tất cả')
const searchQuery = ref('')

const FALLBACK_IMG = 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=1200&q=80'

const fetchCinemas = async () => {
  try {
    const { data } = await api.get('/v1/cinemas')
    cinemas.value = data
  } catch (error) {
    console.error('Error fetching cinemas:', error)
  } finally {
    loading.value = false
  }
}

const cities = computed(() => {
  if (!cinemas.value?.length) return ['Tất cả']
  const all = cinemas.value.map(c => c.city).filter(Boolean)
  return ['Tất cả', ...new Set(all)]
})

const totalRooms = computed(() => cinemas.value.reduce((sum, c) => sum + (c.rooms || 0), 0))

const filteredCinemas = computed(() => {
  let list = cinemas.value
  if (selectedCity.value !== 'Tất cả') list = list.filter(c => c.city === selectedCity.value)
  const q = searchQuery.value.trim().toLowerCase()
  if (q) list = list.filter(c =>
    c.name?.toLowerCase().includes(q) || c.address?.toLowerCase().includes(q)
  )
  return list
})

const statusLabel = (s) => ({ ACTIVE: 'Đang hoạt động', MAINTENANCE: 'Bảo trì', CLOSED: 'Tạm đóng' }[s] || 'Đang hoạt động')

onMounted(fetchCinemas)
</script>

<template>
  <main class="min-h-screen pt-32 pb-24 text-on-surface">
    <!-- Hero -->
    <section class="max-w-[1440px] mx-auto px-6 md:px-10 mb-16 relative overflow-hidden">
      <div class="absolute top-0 right-0 w-[500px] h-[500px] bg-primary/10 blur-[120px] -z-10 rounded-full"></div>
      <div class="border-b border-outline-variant/30 pb-12">
        <div class="flex flex-col lg:flex-row lg:items-end justify-between gap-10">
          <div class="max-w-2xl">
            <span class="text-primary font-bold tracking-[0.3em] uppercase text-xs mb-4 block">Hệ thống rạp DevCine</span>
            <h1 class="text-5xl md:text-7xl font-bold font-headline tracking-tight mb-7 leading-[1.18] md:leading-[1.15]">
              Trải nghiệm<br/><span class="text-primary italic">Điện ảnh</span> Đẳng cấp
            </h1>
            <p class="text-on-surface-variant text-base md:text-lg leading-loose mb-8">
              DevCine hiện vận hành <span class="text-on-surface font-bold">{{ cinemas.length }} cụm rạp</span> tại TP. Hồ Chí Minh, với phòng chiếu <span class="text-on-surface font-bold">IMAX &amp; 3D</span>, ghế <span class="text-on-surface font-bold">VIP</span> và <span class="text-on-surface font-bold">Sweetbox</span> ghế đôi. Đặt vé, chọn ghế và thanh toán trực tuyến chỉ trong vài bước.
            </p>
            <!-- Chip tính năng thực tế của hệ thống -->
            <div class="flex flex-wrap gap-2.5">
              <span v-for="f in ['Phòng IMAX', 'Định dạng 3D', 'Ghế VIP', 'Sweetbox (ghế đôi)', 'Đặt vé online']" :key="f"
                    class="px-3.5 py-1.5 rounded-full bg-surface-container-high/60 border border-outline-variant/20 text-xs font-bold text-on-surface-variant">
                {{ f }}
              </span>
            </div>
          </div>

          <!-- Thống kê (kích thước đồng nhất) -->
          <div class="grid grid-cols-3 gap-3 shrink-0">
            <div class="w-28 p-4 border border-outline-variant/50 rounded-2xl bg-surface-container-low/50 backdrop-blur-md text-center">
              <div class="text-3xl font-bold text-primary">{{ cinemas.length }}</div>
              <div class="text-[0.6rem] uppercase tracking-widest text-on-surface-variant mt-1">Cụm rạp</div>
            </div>
            <div class="w-28 p-4 border border-outline-variant/50 rounded-2xl bg-surface-container-low/50 backdrop-blur-md text-center">
              <div class="text-3xl font-bold text-primary">{{ cities.length - 1 }}</div>
              <div class="text-[0.6rem] uppercase tracking-widest text-on-surface-variant mt-1">Tỉnh / Thành</div>
            </div>
            <div class="w-28 p-4 border border-outline-variant/50 rounded-2xl bg-surface-container-low/50 backdrop-blur-md text-center">
              <div class="text-3xl font-bold text-primary">{{ totalRooms }}</div>
              <div class="text-[0.6rem] uppercase tracking-widest text-on-surface-variant mt-1">Phòng chiếu</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Chọn rạp theo khu vực -->
    <section class="max-w-[1440px] mx-auto px-6 md:px-10">
      <div class="flex flex-col md:flex-row gap-12">
        <!-- Sidebar: khu vực + tìm theo tên -->
        <aside class="md:w-64 shrink-0">
          <div class="sticky top-32">
            <div class="mb-8 p-5 rounded-2xl bg-gradient-to-br from-primary/10 to-transparent border border-primary/20">
              <h4 class="font-bold text-sm mb-3">Tìm theo tên rạp</h4>
              <div class="relative">
                <input
                  v-model="searchQuery"
                  type="text"
                  placeholder="Nhập tên hoặc địa chỉ..."
                  class="w-full bg-surface-container-high/50 border border-outline-variant/30 rounded-lg pl-4 pr-9 py-2.5 text-sm focus:outline-none focus:border-primary/50 transition-colors"
                />
                <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-base opacity-50">search</span>
              </div>
            </div>

            <h2 class="text-xl font-bold font-headline mb-5 uppercase tracking-widest text-on-surface-variant">Khu vực</h2>
            <div class="flex flex-col gap-2">
              <button
                v-for="city in cities" :key="city"
                @click="selectedCity = city"
                class="text-left px-4 py-3 rounded-xl transition-all duration-300 flex items-center justify-between"
                :class="selectedCity === city ? 'bg-primary text-on-primary shadow-lg shadow-primary/20' : 'hover:bg-surface-container-high'"
              >
                <span class="font-bold text-sm">{{ city }}</span>
                <span class="text-xs opacity-60 font-mono">{{ city === 'Tất cả' ? cinemas.length : cinemas.filter(c => c.city === city).length }}</span>
              </button>
            </div>
          </div>
        </aside>

        <!-- Danh sách rạp -->
        <div class="flex-grow">
          <div class="mb-8 flex justify-between items-end">
            <div>
              <h3 class="text-2xl md:text-3xl font-bold font-headline mb-2">{{ selectedCity }}</h3>
              <p class="text-on-surface-variant text-sm">Hiển thị {{ filteredCinemas.length }} rạp chiếu</p>
            </div>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div v-for="i in 4" :key="i" class="h-44 bg-surface-container animate-pulse rounded-3xl"></div>
          </div>

          <!-- Empty -->
          <div v-else-if="filteredCinemas.length === 0" class="py-20 text-center border-2 border-dashed border-outline-variant/30 rounded-3xl">
            <span class="material-symbols-outlined text-5xl opacity-20 mb-4 block">location_off</span>
            <p class="text-on-surface-variant italic">Không tìm thấy rạp nào phù hợp.</p>
          </div>

          <!-- List -->
          <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <RouterLink
              v-for="cinema in filteredCinemas" :key="cinema.id"
              :to="`/he-thong-rap/${cinema.id}`"
              class="flex gap-5 p-5 rounded-3xl bg-surface-container-low border border-outline-variant/10 hover:border-primary/40 transition-all duration-300 group"
            >
              <div class="w-36 h-36 rounded-2xl overflow-hidden shrink-0 border border-outline-variant/20 relative">
                <img :src="cinema.imageUrl || FALLBACK_IMG" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"/>
                <span class="absolute top-2 left-2 px-2 py-0.5 bg-black/70 backdrop-blur text-[0.6rem] font-bold uppercase tracking-widest rounded-full text-on-surface">{{ cinema.type }}</span>
              </div>
              <div class="flex flex-col justify-between py-1 min-w-0 flex-grow">
                <div>
                  <h4 class="font-bold text-lg group-hover:text-primary transition-colors truncate mb-1">{{ cinema.name }}</h4>
                  <p class="text-[0.8rem] text-on-surface-variant line-clamp-2 leading-relaxed opacity-70">{{ cinema.address }}</p>
                </div>
                <div class="flex items-center gap-5 mt-3 flex-wrap">
                  <div class="flex items-center gap-1.5 opacity-70">
                    <span class="material-symbols-outlined text-sm">meeting_room</span>
                    <span class="text-[0.7rem] font-bold uppercase tracking-widest">{{ cinema.rooms }} Phòng</span>
                  </div>
                  <div class="flex items-center gap-1.5 opacity-70">
                    <span class="material-symbols-outlined text-sm">phone</span>
                    <span class="text-[0.7rem] font-mono">{{ cinema.hotline }}</span>
                  </div>
                  <span class="ml-auto text-[0.65rem] font-bold uppercase tracking-widest text-primary flex items-center gap-1 group-hover:gap-2 transition-all">
                    Xem lịch chiếu <span class="material-symbols-outlined text-sm">arrow_forward</span>
                  </span>
                </div>
              </div>
            </RouterLink>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
