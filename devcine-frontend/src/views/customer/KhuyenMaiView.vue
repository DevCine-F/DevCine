<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { promotionApi, voucherApi, promoArticleApi, bookingApi } from '@/api/customer/index'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { useConfirmStore } from '@/stores/confirm'
import { friendlyError } from '@/utils/friendlyError'

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()
const confirm = useConfirmStore()
const showToast = (message, type = 'success') => toastStore.push(message, type)

const promotions = ref([])
const isLoading = ref(false)
const savingId = ref(null)
const savedIds = ref(new Set())
const hasConfirmedBookings = ref(false)

// Tin khuyến mãi (nội dung biên tập)
const articles = ref([])
const isLoadingArticles = ref(false)


const fetchArticles = async () => {
  isLoadingArticles.value = true
  try {
    const { data } = await promoArticleApi.getActive()
    articles.value = Array.isArray(data) ? data : (data.data ?? [])
  } catch (e) {
    console.error('Lỗi bài viết', e)
  } finally {
    isLoadingArticles.value = false
  }
}

const fetchPromotions = async () => {
  isLoading.value = true
  try {
    const { data } = await promotionApi.getActive(authStore.user?.id)
    const list = data?.data ?? data ?? []
    promotions.value = list
    const newSaved = new Set()
    list.forEach(p => {
      if (p.saved) newSaved.add(p.id)
    })
    savedIds.value = newSaved
  } catch (e) {
    showToast(friendlyError(e, 'Không tải được danh sách khuyến mãi.'), 'error')
  } finally {
    isLoading.value = false
  }
}

const isPointPromo = (p) => p && p.allowPointRedemption && Number(p.pointsRequired) > 0

const getIneligibilityReason = (promo) => {
  if (promo.ineligibilityReason) return promo.ineligibilityReason
  return null
}

const getPromoStatusBadge = (promo) => {
  if (promo.exhausted) {
    return { label: 'Hết lượt lưu', class: 'bg-neutral-800/90 text-neutral-400 border border-neutral-700' }
  }
  if (isPointPromo(promo)) {
    return { label: 'Đổi bằng điểm', class: 'bg-amber-500/20 text-amber-300 border border-amber-500/30' }
  }
  const isNewCust = promo.customerEligibility === 'NEW_CUSTOMER' || 
    (promo.name && promo.name.toLowerCase().includes('khách hàng mới'))
  if (isNewCust) {
    return { label: 'Khách hàng mới', class: 'bg-primary-container/20 text-primary-container border border-primary-container/30' }
  }
  if (promo.customerEligibility && promo.customerEligibility.startsWith('TIER_')) {
    return { label: 'Hạng ' + promo.customerEligibility.substring(5), class: 'bg-purple-500/20 text-purple-300 border border-purple-500/30' }
  }
  return { label: 'Đang diễn ra', class: 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' }
}

const formatValue = (p) => {
  if (p.discountType === 'PERCENTAGE') return `Giảm ${Number(p.discountValue)}%`
  return `Giảm ${Number(p.discountValue).toLocaleString('vi-VN')}đ`
}

const formatEnd = (iso) => {
  if (!iso) return 'Không giới hạn'
  return 'HSD ' + new Date(iso).toLocaleDateString('vi-VN')
}

const requireLogin = () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) {
    showToast('Vui lòng đăng nhập để lưu ưu đãi', 'error')
    router.push('/login')
    return false
  }
  return true
}

// Mã đổi-điểm: trừ điểm rồi lưu voucher
const redeemPoints = async (p) => {
  if (!requireLogin()) return
  const ok = await confirm.show({
    title: 'Đổi điểm lấy ưu đãi',
    message: `Dùng ${Number(p.pointsRequired).toLocaleString('vi-VN')} điểm để đổi mã "${p.code}"?`,
    confirmText: 'Đổi điểm',
    tone: 'primary',
  })
  if (!ok) return
  savingId.value = p.id
  try {
    await voucherApi.redeem(authStore.user.id, p.id)
    savedIds.value.add(p.id)
    showToast('Đã đổi & lưu vào "Ưu đãi của tôi"')
  } catch (e) {
    showToast(friendlyError(e, 'Đổi điểm thất bại (có thể không đủ điểm).'), 'error')
  } finally {
    savingId.value = null
  }
}

// Mã miễn phí: lưu thẳng vào tài khoản
const claimCode = async (p) => {
  if (!requireLogin()) return
  savingId.value = p.id
  try {
    await voucherApi.claim(authStore.user.id, p.code)
    savedIds.value.add(p.id)
    showToast('Đã lưu mã vào "Ưu đãi của tôi"')
  } catch (e) {
    showToast(friendlyError(e, 'Lưu mã thất bại.'), 'error')
  } finally {
    savingId.value = null
  }
}

const formatArticleDate = (iso) => {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('vi-VN')
}

// ===== Phân trang riêng cho từng khu — mỗi trang 2 dòng (3 cột × 2 = 6 mục) =====
const PER_PAGE = 6
const articlePage = ref(1)
const promoPage = ref(1)

const articleTotalPages = computed(() => Math.max(1, Math.ceil(articles.value.length / PER_PAGE)))
const promoTotalPages = computed(() => Math.max(1, Math.ceil(promotions.value.length / PER_PAGE)))

const pagedArticles = computed(() => {
  const start = (articlePage.value - 1) * PER_PAGE
  return articles.value.slice(start, start + PER_PAGE)
})
const pagedPromotions = computed(() => {
  const start = (promoPage.value - 1) * PER_PAGE
  return promotions.value.slice(start, start + PER_PAGE)
})

// Giữ nút vừa bấm ở nguyên vị trí trên màn hình sau khi lưới đổi chiều cao → không bị nhảy/cuộn trang
const keepAnchored = (evt, apply) => {
  const el = evt?.currentTarget
  const before = el ? el.getBoundingClientRect().top : null
  apply()
  nextTick(() => {
    if (el && before != null) {
      const delta = el.getBoundingClientRect().top - before
      if (delta) window.scrollBy(0, delta)
    }
  })
}

const goArticlePage = (n, evt) => {
  if (n < 1 || n > articleTotalPages.value || n === articlePage.value) return
  keepAnchored(evt, () => { articlePage.value = n })
}
const goPromoPage = (n, evt) => {
  if (n < 1 || n > promoTotalPages.value || n === promoPage.value) return
  keepAnchored(evt, () => { promoPage.value = n })
}

onMounted(() => {
  fetchArticles()
  fetchPromotions()
})
</script>

<template>
  <main class="pt-28 sm:pt-32 pb-16 sm:pb-20 max-w-[1440px] mx-auto px-4 sm:px-6 md:px-10">
    
    <!-- Hero Header -->
    <header class="mb-12 sm:mb-16">
      <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-surface-container-high border border-outline-variant/30 text-primary-container text-xs font-bold uppercase tracking-widest mb-4 sm:mb-6">
        <span class="material-symbols-outlined text-sm">local_activity</span> Special Offers
      </div>
      <h1 class="text-3xl sm:text-5xl md:text-7xl font-headline font-extrabold tracking-tighter mb-4 sm:mb-6 text-on-surface leading-none">
        KHUYẾN MÃI <br/><span class="text-primary-container">& ƯU ĐÃI.</span>
      </h1>
      <p class="max-w-2xl text-on-surface-variant text-sm sm:text-lg leading-relaxed">
        Nâng tầm trải nghiệm điện ảnh của bạn với những gói ưu đãi độc quyền. Từ những combo bắp nước chủ đề đến các chương trình dành riêng cho thành viên DevCine.
      </p>
    </header>

    <!-- ===== Tin khuyến mãi (nội dung biên tập) ===== -->
    <section v-if="isLoadingArticles" class="mb-12 sm:mb-20 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 md:gap-8">
      <div v-for="i in 3" :key="'a' + i" class="h-80 bg-surface-container-low rounded-2xl animate-pulse border border-outline-variant/10"></div>
    </section>

    <section v-else-if="articles.length > 0" class="mb-12 sm:mb-20">
      <div class="flex items-end justify-between mb-6 sm:mb-8">
        <h2 class="text-2xl sm:text-3xl md:text-4xl font-headline font-extrabold tracking-tight text-on-surface">Tin khuyến mãi</h2>
        <span class="text-on-surface-variant text-xs sm:text-sm font-bold uppercase tracking-widest">{{ articles.length }} chương trình</span>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 md:gap-8">
        <article v-for="item in pagedArticles" :key="item.id"
                 class="group bg-surface-container-low rounded-2xl overflow-hidden flex flex-col border border-outline-variant/10 hover:border-primary-container/30 transition-all duration-300 hover:-translate-y-1">
          <div class="aspect-video relative overflow-hidden bg-surface-container-high">
            <img v-if="item.bannerUrl" :src="item.bannerUrl" :alt="item.title"
                 class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
            <div v-else class="w-full h-full flex items-center justify-center text-outline-variant">
              <span class="material-symbols-outlined text-4xl sm:text-5xl">image</span>
            </div>
            <div class="absolute inset-0 editorial-gradient opacity-60"></div>
            <span class="absolute top-3 left-3 sm:top-4 sm:left-4 bg-surface-container-highest/80 backdrop-blur-md text-[10px] sm:text-xs font-bold px-2.5 sm:px-3 py-1 rounded-full uppercase tracking-wider text-primary-container">
              {{ item.category || 'Tin tức' }}
            </span>
          </div>

          <div class="p-4 sm:p-6 md:p-8 flex flex-col flex-grow">
            <time class="text-xs font-bold text-on-surface-variant/60 uppercase tracking-widest mb-2 sm:mb-3 block">
              {{ formatArticleDate(item.publishedAt) }}
            </time>
            <h3 class="text-base sm:text-xl font-headline font-bold text-on-surface group-hover:text-primary-container transition-colors line-clamp-2 mb-2 sm:mb-3">
              {{ item.title }}
            </h3>
            <p class="text-on-surface-variant text-xs sm:text-sm line-clamp-2 leading-relaxed mb-4 sm:mb-6 flex-grow">
              {{ item.summary }}
            </p>
            <RouterLink :to="`/khuyen-mai/${item.slug || item.id}`"
                        class="inline-flex items-center gap-2 text-primary-container font-bold text-xs sm:text-sm uppercase tracking-wider group/link mt-auto">
              Xem chi tiết
              <span class="material-symbols-outlined text-base sm:text-lg group-hover/link:translate-x-1 transition-transform">arrow_forward</span>
            </RouterLink>
          </div>
        </article>
      </div>

      <!-- Phân trang khu Tin tức -->
      <nav v-if="!isLoadingArticles && articleTotalPages > 1" class="flex justify-center items-center gap-1.5 sm:gap-2 mt-8 sm:mt-10">
        <button @click="goArticlePage(articlePage - 1, $event)" :disabled="articlePage === 1"
                class="w-9 h-9 sm:w-10 sm:h-10 flex items-center justify-center rounded-lg border border-outline-variant/20 text-on-surface-variant hover:border-primary-container hover:text-primary-container transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
          <span class="material-symbols-outlined text-base sm:text-lg">chevron_left</span>
        </button>
        <button v-for="n in articleTotalPages" :key="'ap' + n" @click="goArticlePage(n, $event)"
                :class="n === articlePage ? 'bg-primary-container text-on-primary border-primary-container' : 'border-outline-variant/20 text-on-surface-variant hover:border-primary-container'"
                class="min-w-9 h-9 sm:min-w-10 sm:h-10 px-2.5 sm:px-3 flex items-center justify-center rounded-lg border font-bold text-xs sm:text-sm transition-colors">
          {{ n }}
        </button>
        <button @click="goArticlePage(articlePage + 1, $event)" :disabled="articlePage === articleTotalPages"
                class="w-9 h-9 sm:w-10 sm:h-10 flex items-center justify-center rounded-lg border border-outline-variant/20 text-on-surface-variant hover:border-primary-container hover:text-primary-container transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
          <span class="material-symbols-outlined text-base sm:text-lg">chevron_right</span>
        </button>
      </nav>
    </section>

    <!-- Tiêu đề khu voucher -->
    <h2 class="text-2xl sm:text-3xl md:text-4xl font-headline font-extrabold tracking-tight text-on-surface mb-6 sm:mb-8">Mã ưu đãi & Voucher</h2>

    <!-- Promotions Grid -->
    <!-- Loading -->
    <section v-if="isLoading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 md:gap-8">
      <div v-for="i in 3" :key="i" class="h-52 bg-surface-container-low rounded-xl animate-pulse border border-outline-variant/10"></div>
    </section>

    <!-- Empty -->
    <section v-else-if="promotions.length === 0" class="flex flex-col items-center justify-center py-16 sm:py-24 text-center bg-surface-container-low rounded-xl border border-outline-variant/10 px-4">
      <span class="material-symbols-outlined text-4xl sm:text-5xl text-outline-variant mb-4">local_activity</span>
      <p class="text-on-surface-variant font-semibold text-sm sm:text-base">Hiện chưa có chương trình khuyến mãi nào đang diễn ra</p>
      <p class="text-xs sm:text-sm text-outline-variant mt-1">Vui lòng quay lại sau để không bỏ lỡ ưu đãi mới.</p>
    </section>

    <!-- Danh sách khuyến mãi đang chạy -->
    <section v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 md:gap-8">
      <div v-for="promo in pagedPromotions" :key="promo.id"
           :class="[
             'group bg-surface-container-low rounded-xl overflow-hidden flex flex-col border transition-all duration-300 shadow-lg',
             promo.exhausted ? 'opacity-60 border-outline-variant/10' : 'border-outline-variant/10 hover:border-primary-container/30'
           ]">
        <div class="h-32 sm:h-40 relative bg-gradient-to-br from-primary-container/20 to-surface-container-high flex items-center justify-center">
          <span class="text-2xl sm:text-4xl font-headline font-extrabold text-primary-container">{{ formatValue(promo) }}</span>
          <div :class="['absolute top-3 left-3 px-2.5 py-0.5 rounded-sm font-bold text-[9px] sm:text-[10px] uppercase tracking-widest backdrop-blur-md', getPromoStatusBadge(promo).class]">
            {{ getPromoStatusBadge(promo).label }}
          </div>
        </div>
        <div class="p-4 sm:p-6 md:p-8 flex flex-col flex-grow">
          <div class="flex items-center justify-between mb-2 gap-2">
            <h3 class="text-base sm:text-lg font-headline font-bold text-on-surface truncate">{{ promo.name || 'Ưu đãi đặc biệt' }}</h3>
            <span v-if="promo.pointsRequired > 0" class="text-[9px] sm:text-[10px] font-bold uppercase tracking-widest text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded shrink-0">{{ Number(promo.pointsRequired).toLocaleString('vi-VN') }} điểm</span>
          </div>
          <p class="text-on-surface-variant text-xs sm:text-sm leading-relaxed mb-3 flex-grow">
            {{ promo.description || `${formatValue(promo)} khi đặt vé tại DevCine. ${isPointPromo(promo) ? 'Đổi bằng điểm tích luỹ.' : 'Lưu mã để dùng khi thanh toán.'} ${formatEnd(promo.endDate)}.` }}
          </p>

          <!-- Badges điều kiện áp dụng (Snapshot chuẩn) -->
          <div v-if="promo.applicableMovieTitle || Number(promo.minOrderValue) > 0 || Number(promo.maxDiscountAmount) > 0 || Number(promo.maxTicketQuantity) > 0" class="flex flex-wrap gap-2 mb-4">
            <span v-if="promo.applicableMovieTitle" class="inline-flex items-center bg-white/5 text-neutral-300 border border-white/10 text-[10px] font-medium px-2.5 py-1 rounded">
              Phim: {{ promo.applicableMovieTitle }}
            </span>
            <span v-if="Number(promo.minOrderValue) > 0" class="inline-flex items-center bg-white/5 text-neutral-400 border border-white/10 text-[10px] font-medium px-2.5 py-1 rounded">
              Đơn từ {{ Number(promo.minOrderValue).toLocaleString('vi-VN') }}đ
            </span>
            <span v-if="Number(promo.maxDiscountAmount) > 0" class="inline-flex items-center bg-white/5 text-neutral-400 border border-white/10 text-[10px] font-medium px-2.5 py-1 rounded">
              Tối đa {{ Number(promo.maxDiscountAmount).toLocaleString('vi-VN') }}đ
            </span>
            <span v-if="Number(promo.maxTicketQuantity) > 0" class="inline-flex items-center bg-white/5 text-neutral-400 border border-white/10 text-[10px] font-medium px-2.5 py-1 rounded">
              Tối đa {{ promo.maxTicketQuantity }} vé
            </span>
          </div>

          <!-- Khu vực trạng thái / hành động -->
          <div v-if="savedIds.has(promo.id)" class="flex items-center justify-between gap-3 mt-auto pt-3 border-t border-white/5">
            <span class="inline-flex items-center gap-1.5 bg-green-500/10 border border-green-500/30 text-green-400 font-bold text-[10px] sm:text-xs uppercase tracking-widest px-3 py-1.5 rounded-sm select-none">
              <span class="material-symbols-outlined text-sm">check_circle</span>
              {{ isPointPromo(promo) ? 'Đã đổi' : 'Đã lưu' }}
            </span>
            <RouterLink to="/profile/vouchers" class="text-primary-container font-bold text-[10px] sm:text-xs uppercase tracking-widest hover:underline shrink-0">
              Ưu đãi của tôi →
            </RouterLink>
          </div>

          <div v-else-if="promo.exhausted" class="flex items-center justify-between gap-3 mt-auto pt-3 border-t border-white/5">
            <span class="inline-flex items-center bg-neutral-800 text-neutral-400 border border-neutral-700 font-bold text-[10px] sm:text-xs uppercase tracking-widest px-3 py-1.5 rounded-sm select-none">
              Hết lượt lưu
            </span>
            <span class="text-[11px] text-neutral-500">Đã hết suất hệ thống</span>
          </div>

          <div v-else-if="getIneligibilityReason(promo)" class="flex items-center justify-between gap-3 mt-auto pt-3 border-t border-white/5">
            <span class="inline-flex items-center bg-amber-500/10 border border-amber-500/20 text-amber-400 font-medium text-[11px] px-3 py-1.5 rounded-sm select-none">
              {{ getIneligibilityReason(promo) }}
            </span>
          </div>

          <!-- Nút đổi điểm -->
          <div v-else-if="isPointPromo(promo)" class="flex items-center justify-between gap-3 mt-auto pt-3 border-t border-white/5">
            <button @click="redeemPoints(promo)" :disabled="savingId === promo.id"
                    class="flex items-center gap-1.5 bg-amber-500/10 border border-amber-500/30 text-amber-400 font-bold text-[10px] sm:text-xs uppercase tracking-widest px-4 py-2 rounded-sm hover:bg-amber-500/20 transition-all disabled:opacity-60">
              <span class="material-symbols-outlined text-sm">redeem</span>
              {{ savingId === promo.id ? 'Đang đổi...' : `Đổi ${Number(promo.pointsRequired).toLocaleString('vi-VN')} điểm` }}
            </button>
          </div>

          <!-- Nút lưu mã -->
          <div v-else class="flex items-center justify-between gap-3 mt-auto pt-3 border-t border-white/5">
            <button @click="claimCode(promo)" :disabled="savingId === promo.id"
                    class="flex items-center gap-1.5 bg-primary-container text-on-primary font-bold text-[10px] sm:text-xs uppercase tracking-widest px-4 py-2 rounded-sm hover:bg-primary-fixed-dim transition-all disabled:opacity-60 shadow-sm">
              <span class="material-symbols-outlined text-sm">bookmark_add</span>
              {{ savingId === promo.id ? 'Đang lưu...' : 'Lưu mã' }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- Phân trang khu Voucher -->
    <nav v-if="!isLoading && promoTotalPages > 1" class="flex justify-center items-center gap-1.5 sm:gap-2 mt-8 sm:mt-10">
      <button @click="goPromoPage(promoPage - 1, $event)" :disabled="promoPage === 1"
              class="w-9 h-9 sm:w-10 sm:h-10 flex items-center justify-center rounded-lg border border-outline-variant/20 text-on-surface-variant hover:border-primary-container hover:text-primary-container transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
        <span class="material-symbols-outlined text-base sm:text-lg">chevron_left</span>
      </button>
      <button v-for="n in promoTotalPages" :key="'pp' + n" @click="goPromoPage(n, $event)"
              :class="n === promoPage ? 'bg-primary-container text-on-primary border-primary-container' : 'border-outline-variant/20 text-on-surface-variant hover:border-primary-container'"
              class="min-w-9 h-9 sm:min-w-10 sm:h-10 px-2.5 sm:px-3 flex items-center justify-center rounded-lg border font-bold text-xs sm:text-sm transition-colors">
        {{ n }}
      </button>
      <button @click="goPromoPage(promoPage + 1, $event)" :disabled="promoPage === promoTotalPages"
              class="w-9 h-9 sm:w-10 sm:h-10 flex items-center justify-center rounded-lg border border-outline-variant/20 text-on-surface-variant hover:border-primary-container hover:text-primary-container transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
        <span class="material-symbols-outlined text-base sm:text-lg">chevron_right</span>
      </button>
    </nav>

  </main>
</template>

<style scoped>
.editorial-gradient {
  background: linear-gradient(to top, rgba(14, 14, 14, 1) 0%, rgba(14, 14, 14, 0.4) 50%, rgba(14, 14, 14, 0) 100%);
}
</style>
