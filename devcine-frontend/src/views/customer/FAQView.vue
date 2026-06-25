<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import api from '@/api/axios'

const faqs = ref([])
const loading = ref(true)
const loadError = ref('')
const selectedCategory = ref('')
const searchQuery = ref('')

const ICONS = {
  'Đặt vé & Thanh toán': 'payments',
  'Thành viên DevCine': 'stars',
  'Quy định rạp': 'gavel',
  'Ưu đãi & Khuyến mãi': 'local_offer'
}
const iconFor = (cat) => ICONS[cat] || 'help'

const fetchFaqs = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const { data } = await api.get('/faqs')
    faqs.value = data
    if (categories.value.length) selectedCategory.value = categories.value[0]
  } catch (e) {
    console.error('Lỗi tải FAQ', e)
    loadError.value = 'Không thể tải nội dung. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

// Giữ thứ tự danh mục theo lần xuất hiện đầu tiên (backend đã sort)
const categories = computed(() => {
  const seen = []
  faqs.value.forEach(f => { if (!seen.includes(f.category)) seen.push(f.category) })
  return seen
})

const displayedFaqs = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (q) {
    return faqs.value.filter(f =>
      f.question?.toLowerCase().includes(q) || f.answer?.toLowerCase().includes(q)
    )
  }
  return faqs.value.filter(f => f.category === selectedCategory.value)
})

const countOf = (cat) => faqs.value.filter(f => f.category === cat).length

onMounted(fetchFaqs)
</script>

<template>
  <main class="min-h-screen">
    <!-- Hero -->
    <section class="relative min-h-[460px] pt-32 pb-16 flex flex-col items-center justify-center text-center overflow-hidden">
      <div class="absolute inset-0 z-0">
        <img class="w-full h-full object-cover opacity-30" src="/images/Hopper.webp"/>
        <div class="absolute inset-0 bg-gradient-to-t from-surface via-surface/60 to-transparent"></div>
      </div>
      <div class="relative z-10 px-4 max-w-4xl w-full">
        <h1 class="font-headline font-extrabold text-5xl md:text-7xl text-on-surface tracking-tighter mb-8">
          CÂU HỎI <span class="text-primary-container italic">THƯỜNG GẶP</span>
        </h1>
        <div class="relative group max-w-2xl mx-auto">
          <div class="absolute inset-y-0 left-6 flex items-center pointer-events-none">
            <span class="material-symbols-outlined text-primary-container">search</span>
          </div>
          <input v-model="searchQuery" type="text"
                 class="w-full bg-black/30 border border-white/10 focus:border-primary-container focus:ring-1 focus:ring-primary-container/30 text-white py-5 pl-16 pr-8 rounded-2xl font-body text-lg shadow-2xl placeholder:text-neutral-500 transition-all"
                 placeholder="Tìm kiếm câu trả lời của bạn..."/>
        </div>
      </div>
    </section>

    <!-- Content -->
    <section class="max-w-[1440px] mx-auto px-6 md:px-10 py-12 flex flex-col md:flex-row gap-12">
      <!-- Sidebar danh mục -->
      <aside class="md:w-1/4 space-y-2">
        <div class="sticky top-32">
          <h3 class="font-headline font-bold text-neutral-500 text-xs uppercase tracking-[0.2em] mb-6 px-4">Danh mục</h3>
          <nav class="flex flex-col gap-1">
            <button
              v-for="cat in categories" :key="cat"
              @click="selectedCategory = cat; searchQuery = ''"
              class="flex items-center gap-4 px-4 py-4 rounded-xl text-left transition-all group"
              :class="selectedCategory === cat && !searchQuery ? 'bg-primary-container text-on-primary font-semibold' : 'text-neutral-400 hover:bg-white/10 hover:text-white'"
            >
              <span class="material-symbols-outlined" :class="selectedCategory === cat && !searchQuery ? '' : 'group-hover:text-primary-container transition-colors'">{{ iconFor(cat) }}</span>
              <span class="flex-grow">{{ cat }}</span>
              <span class="text-xs opacity-60 font-mono">{{ countOf(cat) }}</span>
            </button>
          </nav>
          <div class="mt-12 p-6 rounded-2xl glass-card glass-shine-edge">
            <p class="text-sm text-neutral-400 mb-4 font-body leading-relaxed">Không tìm thấy điều bạn cần?</p>
            <RouterLink to="/contact" class="text-primary-container font-headline font-bold text-sm flex items-center gap-2 group">
              LIÊN HỆ CHÚNG TÔI
              <span class="material-symbols-outlined text-sm group-hover:translate-x-1 transition-transform">arrow_forward</span>
            </RouterLink>
          </div>
        </div>
      </aside>

      <!-- Danh sách câu hỏi -->
      <div class="md:w-3/4 space-y-8">
        <div class="mb-10">
          <h2 class="font-headline font-bold text-3xl text-on-surface mb-2">
            {{ searchQuery ? `Kết quả cho "${searchQuery}"` : selectedCategory }}
          </h2>
          <div class="h-1 w-20 bg-primary-container"></div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="space-y-4">
          <div v-for="i in 3" :key="i" class="h-20 glass-card rounded-2xl animate-pulse"></div>
        </div>

        <!-- Error -->
        <div v-else-if="loadError" class="py-16 text-center glass-card rounded-2xl">
          <span class="material-symbols-outlined text-4xl text-red-500/70 mb-3 block">error</span>
          <p class="text-on-surface-variant mb-4">{{ loadError }}</p>
          <button @click="fetchFaqs" class="text-primary-container font-bold text-sm">Thử lại</button>
        </div>

        <!-- Empty -->
        <div v-else-if="displayedFaqs.length === 0" class="py-16 text-center glass-card rounded-2xl">
          <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 mb-3 block">quiz</span>
          <p class="text-on-surface-variant">{{ searchQuery ? 'Không tìm thấy câu hỏi phù hợp.' : 'Chưa có câu hỏi trong mục này.' }}</p>
        </div>

        <!-- Accordions -->
        <div v-else class="space-y-4">
          <details v-for="(f, idx) in displayedFaqs" :key="f.id" class="group glass-card rounded-2xl overflow-hidden" :open="idx === 0">
            <summary class="flex items-center justify-between p-6 cursor-pointer list-none">
              <p class="font-body font-semibold text-lg md:text-xl text-on-surface group-hover:text-primary-container transition-colors pr-4">{{ f.question }}</p>
              <span class="material-symbols-outlined text-neutral-500 group-open:rotate-180 transition-transform shrink-0">expand_more</span>
            </summary>
            <div class="px-6 pb-7 text-on-surface-variant font-body leading-relaxed whitespace-pre-line">{{ f.answer }}</div>
          </details>
        </div>

        <!-- CTA -->
        <div class="mt-20 p-12 glass-card glass-shine-edge rounded-3xl relative overflow-hidden text-center">
          <div class="absolute top-0 right-0 w-64 h-64 bg-primary-container/5 rounded-full -translate-y-1/2 translate-x-1/2 blur-3xl"></div>
          <div class="relative z-10">
            <h3 class="font-headline font-bold text-3xl mb-4">Vẫn còn thắc mắc?</h3>
            <p class="text-on-surface-variant mb-8 max-w-lg mx-auto">Đội ngũ hỗ trợ của chúng tôi luôn sẵn sàng giải đáp mọi câu hỏi của bạn qua hotline hoặc liên hệ trực tuyến.</p>
            <RouterLink to="/contact" class="inline-block bg-primary-container text-on-primary font-headline font-bold uppercase py-4 px-10 rounded-sm hover:shadow-[0_0_30px_rgba(245,197,24,0.3)] transition-all">
              Liên hệ hỗ trợ
            </RouterLink>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
details > summary::-webkit-details-marker { display: none; }
</style>
