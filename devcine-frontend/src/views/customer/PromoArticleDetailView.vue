<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { promoArticleApi } from '@/api/customer/index'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'
import { parseMarkdownToHtml } from '@/utils/markdownParser'

const toast = useToastStore()
const route = useRoute()
const router = useRouter()

const article = ref(null)
const isLoading = ref(true)
const loadError = ref(false)

const fetchArticle = async () => {
  const id = route.params.id
  if (!id) {
    router.replace('/khuyen-mai')
    return
  }

  isLoading.value = true
  loadError.value = false
  try {
    const { data } = await promoArticleApi.getDetail(id)
    article.value = data?.data ?? data
  } catch (e) {
    loadError.value = true
    toast.error(friendlyError(e, 'Không tải được tin khuyến mãi.'))
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  fetchArticle()
})

watch(() => route.params.id, (newId) => {
  if (newId) {
    fetchArticle()
  }
})

const formatDate = (iso) => (iso ? new Date(iso).toLocaleDateString('vi-VN') : '')

const periodText = computed(() => {
  if (!article.value) return ''
  const { startDate, endDate } = article.value
  if (startDate && endDate) return `${formatDate(startDate)} — ${formatDate(endDate)}`
  if (endDate) return `Đến ${formatDate(endDate)}`
  if (startDate) return `Từ ${formatDate(startDate)}`
  return 'Đang áp dụng'
})

const renderedContent = computed(() => {
  if (!article.value?.content) return '<p class="text-on-surface-variant italic">Nội dung chi tiết đang được cập nhật.</p>'
  return parseMarkdownToHtml(article.value.content)
})
</script>

<template>
  <main class="pt-28 sm:pt-32 pb-16 sm:pb-24 px-4 sm:px-6">
    <div class="max-w-3xl mx-auto">
      <!-- Back -->
      <RouterLink to="/khuyen-mai"
                  class="inline-flex items-center gap-2 text-on-surface-variant hover:text-primary-container transition-colors text-xs sm:text-sm font-bold uppercase tracking-widest mb-6 sm:mb-10">
        <span class="material-symbols-outlined text-base sm:text-lg">arrow_back</span>
        Tất cả ưu đãi
      </RouterLink>

      <!-- Loading Skeleton -->
      <div v-if="isLoading" class="animate-pulse space-y-6">
        <div class="h-5 w-32 bg-surface-container-high rounded"></div>
        <div class="h-12 w-3/4 bg-surface-container-high rounded"></div>
        <div class="h-4 w-40 bg-surface-container-high rounded"></div>
        <div class="space-y-3 pt-6">
          <div class="h-4 w-full bg-surface-container-high rounded"></div>
          <div class="h-4 w-full bg-surface-container-high rounded"></div>
          <div class="h-4 w-2/3 bg-surface-container-high rounded"></div>
        </div>
      </div>

      <!-- Error / Not found -->
      <div v-else-if="loadError || !article" class="flex flex-col items-center justify-center text-center py-16 sm:py-24 bg-surface-container-low rounded-2xl border border-outline-variant/10 px-4">
        <span class="material-symbols-outlined text-4xl sm:text-5xl text-outline-variant mb-4">sentiment_dissatisfied</span>
        <h2 class="text-lg sm:text-xl font-headline font-bold text-on-surface mb-2">Không tìm thấy ưu đãi này</h2>
        <p class="text-on-surface-variant text-xs sm:text-sm mb-6">Tin khuyến mãi có thể đã kết thúc hoặc bị gỡ.</p>
        <RouterLink to="/khuyen-mai" class="bg-primary-container text-on-primary font-bold text-xs uppercase tracking-widest px-6 py-3 rounded-lg hover:brightness-110 transition-all">Về trang khuyến mãi</RouterLink>
      </div>

      <!-- Content -->
      <article v-else>
        <header class="border-b border-outline-variant/15 pb-6 sm:pb-8 mb-6 sm:mb-8">
          <span class="inline-flex items-center gap-1.5 bg-primary-container/10 border border-primary-container/30 text-primary-container text-[10px] sm:text-[11px] font-bold uppercase tracking-[0.2em] px-2.5 sm:px-3 py-1 rounded-sm mb-4 sm:mb-6">
            <span class="material-symbols-outlined text-sm">local_activity</span> Ưu đãi đang áp dụng
          </span>
          <h1 class="text-2xl sm:text-3xl md:text-5xl font-headline font-extrabold tracking-tight text-on-surface uppercase italic leading-tight mb-4 sm:mb-5">
            {{ article.title }}
          </h1>
          <div class="flex items-center gap-2 text-on-surface-variant text-xs sm:text-sm font-bold uppercase tracking-widest">
            <span class="material-symbols-outlined text-sm sm:text-base text-primary-container">event</span>
            {{ periodText }}
          </div>
        </header>

        <!-- Ảnh Thumbnail / Banner chính nếu có -->
        <div v-if="article.image || article.imageUrl" class="mb-8 rounded-2xl overflow-hidden border border-outline-variant/15 shadow-2xl">
          <img :src="article.image || article.imageUrl" :alt="article.title" class="w-full max-h-[420px] object-cover" />
        </div>

        <!-- Mô tả ngắn (lead) -->
        <p v-if="article.description" class="text-base sm:text-xl text-on-surface font-semibold leading-relaxed mb-8 bg-surface-container-high/40 p-4 sm:p-5 rounded-xl border-l-4 border-primary-container">
          {{ article.description }}
        </p>

        <!-- Nội dung chi tiết Render HTML / TipTap -->
        <div class="prose-content text-on-surface-variant text-sm sm:text-base leading-relaxed sm:leading-loose" v-html="renderedContent"></div>

        <!-- CTA -->
        <div class="mt-10 sm:mt-14 pt-6 sm:pt-8 border-t border-outline-variant/15 flex flex-col sm:flex-row gap-3 sm:gap-4">
          <RouterLink to="/lich-chieu"
                      class="flex-1 flex items-center justify-center gap-2 bg-primary-container text-on-primary font-bold text-xs sm:text-sm uppercase tracking-widest px-5 sm:px-6 py-3.5 sm:py-4 rounded-xl hover:brightness-110 transition-all">
            <span class="material-symbols-outlined text-base sm:text-lg">confirmation_number</span>
            Đặt vé ngay
          </RouterLink>
          <RouterLink to="/khuyen-mai"
                      class="flex-1 flex items-center justify-center gap-2 border border-outline-variant/30 text-on-surface font-bold text-xs sm:text-sm uppercase tracking-widest px-5 sm:px-6 py-3.5 sm:py-4 rounded-xl hover:bg-surface-container-high transition-colors">
            <span class="material-symbols-outlined text-base sm:text-lg">redeem</span>
            Xem ưu đãi khác
          </RouterLink>
        </div>
      </article>
    </div>
  </main>
</template>

<style>
.prose-content h2 {
  font-size: 1.5rem;
  font-weight: 800;
  text-transform: uppercase;
  color: #fff;
  margin-top: 2rem;
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.prose-content h3 {
  font-size: 1.25rem;
  font-weight: 700;
  text-transform: uppercase;
  color: #f5c518;
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
}

.prose-content p {
  margin-bottom: 0.75rem;
  line-height: 1.75;
}

.prose-content strong,
.prose-content b {
  color: #fff;
  font-weight: 700;
}

.prose-content em,
.prose-content i {
  font-style: italic;
  color: rgba(255, 255, 255, 0.9);
}

.prose-content u {
  text-decoration: underline;
}

.prose-content s,
.prose-content strike {
  text-decoration: line-through;
}

.prose-content ul {
  list-style-type: disc;
  padding-left: 1.75rem;
  margin: 0.75rem 0;
}

.prose-content ol {
  list-style-type: decimal;
  padding-left: 1.75rem;
  margin: 0.75rem 0;
}

.prose-content li {
  margin-bottom: 0.35rem;
}

.prose-content blockquote {
  border-left: 4px solid #f5c518;
  padding: 0.75rem 1rem;
  margin: 1rem 0;
  background: rgba(245, 197, 24, 0.06);
  font-style: italic;
  color: #cbd5e1;
  border-radius: 0 0.5rem 0.5rem 0;
}

.prose-content img {
  border-radius: 1rem;
  max-width: 100%;
  height: auto;
  display: block;
  margin: 1.5rem auto;
  box-shadow: 0 15px 30px -10px rgba(0, 0, 0, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.prose-content hr {
  margin: 1.5rem 0;
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}

.prose-content a {
  color: #f5c518;
  text-decoration: underline;
  font-weight: 600;
}
</style>
