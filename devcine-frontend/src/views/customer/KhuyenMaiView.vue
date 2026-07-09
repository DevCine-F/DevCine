<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { promotionApi, voucherApi, promoArticleApi } from '@/api/customer/index'
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

// Tin khuyến mãi (nội dung biên tập)
const articles = ref([])
const isLoadingArticles = ref(false)

const errMsg = (e, fb) => friendlyError(e, fb)

const fetchArticles = async () => {
  isLoadingArticles.value = true
  try {
    const { data } = await promoArticleApi.getActive()
    articles.value = Array.isArray(data) ? data : (data.data ?? [])
  } catch (e) {
    console.error('Không tải được tin khuyến mãi', e)
  } finally {
    isLoadingArticles.value = false
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

const fetchPromotions = async () => {
  isLoading.value = true
  try {
    const { data } = await promotionApi.getActive()
    promotions.value = data.data ?? data
  } catch (e) {
    console.error('Không tải được khuyến mãi', e)
  } finally {
    isLoading.value = false
  }
}

// Đánh dấu sẵn các mã user đã lưu từ trước (match theo code) để hiện "Đã lưu"
const markAlreadySaved = async () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) return
  try {
    const { data } = await voucherApi.getAllVouchers(authStore.user.id)
    const list = data.data ?? data
    const ownedCodes = new Set(list.map(v => (v.code || '').toUpperCase()))
    promotions.value.forEach(p => {
      if (p.code && ownedCodes.has(p.code.toUpperCase())) savedIds.value.add(p.id)
    })
  } catch (e) {
    console.error('Không tải được voucher đã lưu', e)
  }
}

const isPointPromo = (p) => p.allowPointRedemption && Number(p.pointsRequired) > 0

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
    showToast(errMsg(e, 'Đổi điểm thất bại (có thể không đủ điểm).'), 'error')
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
    showToast(errMsg(e, 'Lưu mã thất bại.'), 'error')
  } finally {
    savingId.value = null
  }
}

onMounted(async () => {
  fetchArticles()
  await fetchPromotions()
  await markAlreadySaved()
})
</script>

<template>
  <main class="pt-32 pb-20 max-w-[1440px] mx-auto px-6 md:px-10">
    <!-- Hero Section / Header -->
    <header class="mb-16">
      <div class="inline-block bg-primary-container/10 px-4 py-1 border-l-2 border-primary-container mb-4">
        <span class="text-primary-container text-xs font-bold tracking-widest uppercase font-label">Đặc quyền hội viên</span>
      </div>
      <h1 class="text-5xl md:text-7xl font-headline font-extrabold tracking-tighter mb-6 text-on-surface leading-none">
        KHUYẾN MÃI <br/><span class="text-primary-container">& ƯU ĐÃI.</span>
      </h1>
      <p class="max-w-2xl text-on-surface-variant text-lg leading-relaxed">
        Nâng tầm trải nghiệm điện ảnh của bạn với những gói ưu đãi độc quyền. Từ những combo bắp nước chủ đề đến các chương trình dành riêng cho thành viên DevCine.
      </p>
    </header>

    <!-- ===== Tin khuyến mãi (nội dung biên tập) ===== -->
    <section v-if="isLoadingArticles" class="mb-20 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
      <div v-for="i in 3" :key="'a' + i" class="h-80 bg-surface-container-low rounded-2xl animate-pulse border border-outline-variant/10"></div>
    </section>

    <section v-else-if="articles.length > 0" class="mb-20">
      <div class="flex items-end justify-between mb-8">
        <h2 class="text-3xl md:text-4xl font-headline font-extrabold tracking-tight text-on-surface">Tin khuyến mãi</h2>
        <span class="text-on-surface-variant text-sm font-bold uppercase tracking-widest">{{ articles.length }} chương trình</span>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <article v-for="article in pagedArticles" :key="article.id"
                 class="group bg-surface-container-low rounded-2xl overflow-hidden flex flex-col border border-outline-variant/10 hover:border-primary-container/40 transition-all duration-300">
          <!-- Ảnh -->
          <div class="aspect-[16/10] relative overflow-hidden bg-surface-container-high">
            <img v-if="article.imageUrl" :src="article.imageUrl" :alt="article.title"
                 class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700" />
            <div v-else class="w-full h-full flex items-center justify-center">
              <span class="material-symbols-outlined text-5xl text-outline-variant">image</span>
            </div>
            <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-transparent to-transparent"></div>
            <div class="absolute top-3 left-3 bg-primary-container text-on-primary px-3 py-1 font-bold text-[10px] uppercase tracking-widest rounded-sm">Ưu đãi</div>
          </div>

          <!-- Nội dung -->
          <div class="p-6 flex flex-col flex-grow">
            <h3 class="text-lg font-headline font-bold text-on-surface uppercase italic leading-snug mb-2 line-clamp-2">{{ article.title }}</h3>
            <p class="text-on-surface-variant text-sm leading-relaxed mb-4 flex-grow line-clamp-3">{{ article.description }}</p>
            <div class="flex items-center justify-between gap-3">
              <span v-if="article.endDate" class="text-[11px] font-bold uppercase tracking-widest text-on-surface-variant">
                Đến {{ formatArticleDate(article.endDate) }}
              </span>
              <span v-else></span>
              <RouterLink :to="`/khuyen-mai/${article.id}`"
                      class="shrink-0 flex items-center gap-1.5 bg-primary-container/10 border border-primary-container/30 text-primary-container font-bold text-xs uppercase tracking-widest px-4 py-2 rounded-lg hover:bg-primary-container/20 transition-all">
                Xem chi tiết
                <span class="material-symbols-outlined text-sm">arrow_forward</span>
              </RouterLink>
            </div>
          </div>
        </article>
      </div>

      <!-- Phân trang khu Tin khuyến mãi -->
      <nav v-if="articleTotalPages > 1" class="flex justify-center items-center gap-2 mt-10">
        <button @click="goArticlePage(articlePage - 1, $event)" :disabled="articlePage === 1"
                class="w-10 h-10 flex items-center justify-center rounded-lg border border-outline-variant/20 text-on-surface-variant hover:border-primary-container hover:text-primary-container transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
          <span class="material-symbols-outlined text-lg">chevron_left</span>
        </button>
        <button v-for="n in articleTotalPages" :key="'ap' + n" @click="goArticlePage(n, $event)"
                :class="n === articlePage ? 'bg-primary-container text-on-primary border-primary-container' : 'border-outline-variant/20 text-on-surface-variant hover:border-primary-container'"
                class="min-w-10 h-10 px-3 flex items-center justify-center rounded-lg border font-bold text-sm transition-colors">
          {{ n }}
        </button>
        <button @click="goArticlePage(articlePage + 1, $event)" :disabled="articlePage === articleTotalPages"
                class="w-10 h-10 flex items-center justify-center rounded-lg border border-outline-variant/20 text-on-surface-variant hover:border-primary-container hover:text-primary-container transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
          <span class="material-symbols-outlined text-lg">chevron_right</span>
        </button>
      </nav>
    </section>

    <!-- Tiêu đề khu voucher (chỉ hiện khi đã có mục Tin ở trên để phân tách) -->
    <h2 v-if="articles.length > 0" class="text-3xl md:text-4xl font-headline font-extrabold tracking-tight text-on-surface mb-8">Mã ưu đãi & Voucher</h2>

    <!-- Promotions Grid -->
    <!-- Loading -->
    <section v-if="isLoading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
      <div v-for="i in 3" :key="i" class="h-52 bg-surface-container-low rounded-xl animate-pulse border border-outline-variant/10"></div>
    </section>

    <!-- Empty -->
    <section v-else-if="promotions.length === 0" class="flex flex-col items-center justify-center py-24 text-center bg-surface-container-low rounded-xl border border-outline-variant/10">
      <span class="material-symbols-outlined text-5xl text-outline-variant mb-4">local_activity</span>
      <p class="text-on-surface-variant font-semibold">Hiện chưa có chương trình khuyến mãi nào đang diễn ra</p>
      <p class="text-sm text-outline-variant mt-1">Vui lòng quay lại sau để không bỏ lỡ ưu đãi mới.</p>
    </section>

    <!-- Danh sách khuyến mãi đang chạy -->
    <section v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
      <div v-for="promo in pagedPromotions" :key="promo.id"
           class="group bg-surface-container-low rounded-xl overflow-hidden flex flex-col border border-outline-variant/10 hover:border-primary-container/30 transition-all duration-300">
        <div class="h-40 relative bg-gradient-to-br from-primary-container/20 to-surface-container-high flex items-center justify-center">
          <span class="text-4xl font-headline font-extrabold text-primary-container">{{ formatValue(promo) }}</span>
          <div class="absolute top-0 left-0 bg-error-container text-on-error-container px-3 py-1 font-bold text-[10px] uppercase tracking-widest">Đang áp dụng</div>
        </div>
        <div class="p-8 flex flex-col flex-grow">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-lg font-headline font-bold text-on-surface">{{ promo.name || 'Ưu đãi đặc biệt' }}</h3>
            <span v-if="promo.pointsRequired > 0" class="text-[10px] font-bold uppercase tracking-widest text-amber-400 bg-amber-500/10 px-2 py-1 rounded">{{ Number(promo.pointsRequired).toLocaleString('vi-VN') }} điểm</span>
          </div>
          <p class="text-on-surface-variant text-sm leading-relaxed mb-6 flex-grow">
            {{ formatValue(promo) }} khi đặt vé tại DevCine.
            {{ isPointPromo(promo) ? 'Đổi bằng điểm tích luỹ.' : 'Lưu mã để dùng khi thanh toán.' }}
            {{ formatEnd(promo.endDate) }}.
          </p>

          <!-- Đã sở hữu: nút trạng thái (vô hiệu hoá) + link sang Ưu đãi của tôi.
               Mã đổi-điểm hiện "Đã đổi" (mỗi mã chỉ đổi 1 lần/khách), mã thường hiện "Đã lưu". -->
          <div v-if="savedIds.has(promo.id)" class="flex items-center justify-between gap-3">
            <button type="button" disabled
                    class="self-start flex items-center gap-2 bg-green-500/10 border border-green-500/30 text-green-400 font-bold text-xs uppercase tracking-widest px-5 py-2.5 rounded-lg cursor-default">
              <span class="material-symbols-outlined text-sm">{{ isPointPromo(promo) ? 'redeem' : 'check_circle' }}</span>
              {{ isPointPromo(promo) ? 'Đã đổi' : 'Đã lưu' }}
            </button>
            <RouterLink to="/profile/vouchers" class="text-primary-container font-bold text-xs uppercase tracking-widest hover:opacity-80 shrink-0">Ưu đãi của tôi →</RouterLink>
          </div>

          <!-- Nút đổi điểm (mã point) -->
          <button v-else-if="isPointPromo(promo)" @click="redeemPoints(promo)" :disabled="savingId === promo.id"
                  class="self-start flex items-center gap-2 bg-amber-500/10 border border-amber-500/30 text-amber-400 font-bold text-xs uppercase tracking-widest px-5 py-2.5 rounded-lg hover:bg-amber-500/20 transition-all disabled:opacity-60">
            <span class="material-symbols-outlined text-sm">redeem</span>
            {{ savingId === promo.id ? 'Đang đổi...' : `Đổi ${Number(promo.pointsRequired).toLocaleString('vi-VN')} điểm` }}
          </button>

          <!-- Nút lưu mã (mã free) -->
          <button v-else @click="claimCode(promo)" :disabled="savingId === promo.id"
                  class="self-start flex items-center gap-2 bg-primary-container text-on-primary font-bold text-xs uppercase tracking-widest px-5 py-2.5 rounded-lg hover:brightness-110 transition-all disabled:opacity-60">
            <span class="material-symbols-outlined text-sm">bookmark_add</span>
            {{ savingId === promo.id ? 'Đang lưu...' : 'Lưu mã' }}
          </button>
        </div>
      </div>
    </section>

    <!-- Phân trang khu Voucher -->
    <nav v-if="!isLoading && promoTotalPages > 1" class="flex justify-center items-center gap-2 mt-10">
      <button @click="goPromoPage(promoPage - 1, $event)" :disabled="promoPage === 1"
              class="w-10 h-10 flex items-center justify-center rounded-lg border border-outline-variant/20 text-on-surface-variant hover:border-primary-container hover:text-primary-container transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
        <span class="material-symbols-outlined text-lg">chevron_left</span>
      </button>
      <button v-for="n in promoTotalPages" :key="'pp' + n" @click="goPromoPage(n, $event)"
              :class="n === promoPage ? 'bg-primary-container text-on-primary border-primary-container' : 'border-outline-variant/20 text-on-surface-variant hover:border-primary-container'"
              class="min-w-10 h-10 px-3 flex items-center justify-center rounded-lg border font-bold text-sm transition-colors">
        {{ n }}
      </button>
      <button @click="goPromoPage(promoPage + 1, $event)" :disabled="promoPage === promoTotalPages"
              class="w-10 h-10 flex items-center justify-center rounded-lg border border-outline-variant/20 text-on-surface-variant hover:border-primary-container hover:text-primary-container transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
        <span class="material-symbols-outlined text-lg">chevron_right</span>
      </button>
    </nav>

  </main>
</template>

<style scoped>
.editorial-gradient {
  background: linear-gradient(to top, rgba(14, 14, 14, 1) 0%, rgba(14, 14, 14, 0.4) 50%, rgba(14, 14, 14, 0) 100%);
}
</style>
