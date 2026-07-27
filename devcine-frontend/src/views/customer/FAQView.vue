<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import api from '@/api/axios'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'
import SupportRequestForm from '@/components/customer/SupportRequestForm.vue'

const toast = useToastStore()

const faqs = ref([])
const loading = ref(true)
const loadError = ref('')
const selectedCategory = ref('')
const searchQuery = ref('')

// Form hỗ trợ mở rộng tại chỗ (inline expand) ở cuối trang.
const showSupportForm = ref(false)
const supportBox = ref(null)
const supportInner = ref(null)
// Chiều cao đích để animate max-height (đo scrollHeight khi mở → mượt, không overshoot).
const supportHeight = ref('0px')

const openSupportForm = () => {
  showSupportForm.value = true
  nextTick(() => { supportHeight.value = `${supportInner.value?.scrollHeight ?? 0}px` })
}

const toggleSupport = () => {
  if (showSupportForm.value) {
    showSupportForm.value = false
    supportHeight.value = '0px'
  } else {
    openSupportForm()
  }
}

const openSupport = () => {
  openSupportForm()
  nextTick(() => {
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    supportBox.value?.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'center' })
  })
}

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
    loadError.value = friendlyError(e, 'Không thể tải nội dung. Vui lòng thử lại.')
    toast.error(loadError.value)
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
            <button type="button" @click="openSupport" class="text-primary-container font-headline font-bold text-sm flex items-center gap-2 group">
              GỬI YÊU CẦU HỖ TRỢ
              <span class="material-symbols-outlined text-sm group-hover:translate-x-1 transition-transform">arrow_forward</span>
            </button>
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
        <div ref="supportBox" class="mt-20 p-12 glass-card glass-shine-edge rounded-3xl relative overflow-hidden text-center scroll-mt-32">
          <div class="absolute top-0 right-0 w-64 h-64 bg-primary-container/5 rounded-full -translate-y-1/2 translate-x-1/2 blur-3xl"></div>
          <div class="relative z-10">
            <h3 class="font-headline font-bold text-3xl mb-4">Vẫn còn thắc mắc?</h3>
            <p class="text-on-surface-variant mb-8 max-w-lg mx-auto">Nếu chưa tìm được câu trả lời phù hợp, hãy gửi yêu cầu để bộ phận CSKH hỗ trợ bạn trực tiếp.</p>
            <button type="button" @click="toggleSupport"
                    class="inline-flex items-center gap-2 bg-primary-container text-on-primary font-headline font-bold uppercase py-4 px-10 rounded-sm hover:shadow-[0_0_30px_rgba(245,197,24,0.3)] transition-all">
              {{ showSupportForm ? 'Thu gọn' : 'Gửi yêu cầu hỗ trợ' }}
              <span class="material-symbols-outlined transition-transform duration-300" :class="showSupportForm ? 'rotate-180' : ''">expand_more</span>
            </button>

            <!-- Inline expand: đẩy nội dung xuống, không overlay -->
            <div class="support-expand overflow-hidden" :class="showSupportForm ? 'opacity-100' : 'opacity-0'"
                 :style="{ maxHeight: supportHeight }" :inert="!showSupportForm">
              <div ref="supportInner" class="pt-10 text-left">
                <div class="p-[1.5px] rounded-2xl bg-gradient-to-br from-primary-container/60 via-primary-container/10 to-transparent shadow-[0_0_40px_rgba(245,197,24,0.12)]">
                  <div class="bg-surface rounded-2xl p-6 md:p-8">
                    <SupportRequestForm @submitted="toggleSupport" />
                  </div>
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
details > summary::-webkit-details-marker { display: none; }

/* Inline expand: animate max-height (đo scrollHeight) + fade — mượt, không overlay */
.support-expand {
  transition: max-height 0.5s ease, opacity 0.5s ease;
}

@media (prefers-reduced-motion: reduce) {
  .support-expand {
    transition: none;
  }
}
</style>
