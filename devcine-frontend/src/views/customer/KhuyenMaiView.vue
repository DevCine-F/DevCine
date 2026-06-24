<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { promotionApi, voucherApi } from '@/api/customer/index'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()
const showToast = (message, type = 'success') => toastStore.push(message, type)

const promotions = ref([])
const isLoading = ref(false)
const savingId = ref(null)
const savedIds = ref(new Set())

const errMsg = (e, fb) => friendlyError(e, fb)

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
  if (!confirm(`Dùng ${Number(p.pointsRequired).toLocaleString('vi-VN')} điểm để đổi mã "${p.code}"?`)) return
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
      <div v-for="promo in promotions" :key="promo.id"
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

          <!-- Đã lưu: nút trạng thái (vô hiệu hoá) + link sang Ưu đãi của tôi -->
          <div v-if="savedIds.has(promo.id)" class="flex items-center justify-between gap-3">
            <button type="button" disabled
                    class="self-start flex items-center gap-2 bg-green-500/10 border border-green-500/30 text-green-400 font-bold text-xs uppercase tracking-widest px-5 py-2.5 rounded-lg cursor-default">
              <span class="material-symbols-outlined text-sm">check_circle</span> Đã lưu
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

    <!-- Newsletter / CTA -->
    <section class="mt-24 bg-surface-container rounded-sm p-12 border border-outline-variant/20 relative overflow-hidden">
      <div class="absolute top-0 right-0 w-64 h-64 bg-primary-container/5 rounded-full blur-3xl -mr-32 -mt-32"></div>
      <div class="relative z-10 flex flex-col md:flex-row items-center justify-between gap-8">
        <div class="max-w-xl">
          <h2 class="text-3xl font-headline font-bold text-on-surface mb-2">Đừng bỏ lỡ bất kỳ ưu đãi nào.</h2>
          <p class="text-on-surface-variant">Đăng ký nhận tin để được thông báo về các suất chiếu sớm và mã giảm giá độc quyền dành riêng cho bạn.</p>
        </div>
        <div class="flex w-full md:w-auto gap-2">
          <input class="bg-surface-container-lowest border-outline-variant/30 focus:border-primary-container focus:ring-0 text-on-surface px-6 py-3 rounded-sm w-full md:min-w-[300px]" placeholder="Email của bạn" type="email"/>
          <button class="bg-primary-container text-on-primary px-8 py-3 rounded-sm font-bold uppercase tracking-widest text-xs hover:opacity-90 transition-opacity shrink-0">Đăng ký</button>
        </div>
      </div>
    </section>
  </main>
</template>

<style scoped>
.editorial-gradient {
  background: linear-gradient(to top, rgba(14, 14, 14, 1) 0%, rgba(14, 14, 14, 0.4) 50%, rgba(14, 14, 14, 0) 100%);
}
</style>
