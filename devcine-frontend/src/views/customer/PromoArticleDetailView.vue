<script setup>
import { ref, onMounted, computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { promoArticleApi } from '@/api/customer/index'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()

const route = useRoute()
const article = ref(null)
const isLoading = ref(true)
const loadError = ref(false)

const fetchArticle = async () => {
  isLoading.value = true
  loadError.value = false
  try {
    const { data } = await promoArticleApi.getDetail(route.params.id)
    article.value = data?.data ?? data
  } catch (e) {
    loadError.value = true
    toast.error(friendlyError(e, 'Không tải được tin khuyến mãi.'))
  } finally {
    isLoading.value = false
  }
}

const formatDate = (iso) => (iso ? new Date(iso).toLocaleDateString('vi-VN') : '')

const periodText = computed(() => {
  if (!article.value) return ''
  const { startDate, endDate } = article.value
  if (startDate && endDate) return `${formatDate(startDate)} — ${formatDate(endDate)}`
  if (endDate) return `Đến ${formatDate(endDate)}`
  if (startDate) return `Từ ${formatDate(startDate)}`
  return 'Đang áp dụng'
})

onMounted(fetchArticle)
</script>

<template>
  <main class="pt-32 pb-24 px-6">
    <div class="max-w-3xl mx-auto">
      <!-- Back -->
      <RouterLink to="/khuyen-mai"
                  class="inline-flex items-center gap-2 text-on-surface-variant hover:text-primary-container transition-colors text-sm font-bold uppercase tracking-widest mb-10">
        <span class="material-symbols-outlined text-lg">arrow_back</span>
        Tất cả ưu đãi
      </RouterLink>

      <!-- Loading -->
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
      <div v-else-if="loadError || !article" class="flex flex-col items-center justify-center text-center py-24 bg-surface-container-low rounded-2xl border border-outline-variant/10">
        <span class="material-symbols-outlined text-5xl text-outline-variant mb-4">sentiment_dissatisfied</span>
        <h2 class="text-xl font-headline font-bold text-on-surface mb-2">Không tìm thấy ưu đãi này</h2>
        <p class="text-on-surface-variant text-sm mb-6">Tin khuyến mãi có thể đã kết thúc hoặc bị gỡ.</p>
        <RouterLink to="/khuyen-mai" class="bg-primary-container text-on-primary font-bold text-xs uppercase tracking-widest px-6 py-3 rounded-lg hover:brightness-110 transition-all">Về trang khuyến mãi</RouterLink>
      </div>

      <!-- Content -->
      <article v-else>
        <header class="border-b border-outline-variant/15 pb-8 mb-8">
          <span class="inline-flex items-center gap-1.5 bg-primary-container/10 border border-primary-container/30 text-primary-container text-[11px] font-bold uppercase tracking-[0.2em] px-3 py-1 rounded-sm mb-6">
            <span class="material-symbols-outlined text-sm">local_activity</span> Ưu đãi đang áp dụng
          </span>
          <h1 class="text-3xl md:text-5xl font-headline font-extrabold tracking-tight text-on-surface italic leading-tight mb-5">
            {{ article.title }}
          </h1>
          <div class="flex items-center gap-2 text-on-surface-variant text-sm font-bold uppercase tracking-widest">
            <span class="material-symbols-outlined text-base text-primary-container">event</span>
            {{ periodText }}
          </div>
        </header>

        <!-- Mô tả ngắn (lead) -->
        <p v-if="article.description" class="text-xl text-on-surface font-semibold leading-relaxed mb-8">
          {{ article.description }}
        </p>

        <!-- Nội dung chi tiết -->
        <div class="prose-content text-on-surface-variant text-lg leading-loose whitespace-pre-line">
          {{ article.content || 'Nội dung chi tiết đang được cập nhật.' }}
        </div>

        <!-- CTA -->
        <div class="mt-14 pt-8 border-t border-outline-variant/15 flex flex-col sm:flex-row gap-4">
          <RouterLink to="/lich-chieu"
                      class="flex-1 flex items-center justify-center gap-2 bg-primary-container text-on-primary font-bold text-sm uppercase tracking-widest px-6 py-4 rounded-lg hover:brightness-110 transition-all">
            <span class="material-symbols-outlined text-lg">confirmation_number</span>
            Đặt vé ngay
          </RouterLink>
          <RouterLink to="/khuyen-mai"
                      class="flex-1 flex items-center justify-center gap-2 border border-outline-variant/30 text-on-surface font-bold text-sm uppercase tracking-widest px-6 py-4 rounded-lg hover:bg-surface-container-high transition-colors">
            <span class="material-symbols-outlined text-lg">redeem</span>
            Xem ưu đãi khác
          </RouterLink>
        </div>
      </article>
    </div>
  </main>
</template>
