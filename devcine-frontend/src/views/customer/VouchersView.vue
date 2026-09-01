<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { voucherApi, customerApi } from '@/api/customer/index'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'
import { useSeatRealtime } from '@/composables/useSeatRealtime'

const authStore = useAuthStore()
const toastStore = useToastStore()
const showToast = (message, type = 'success') => toastStore.push(message, type)
const activeTab = ref('active') // 'active' | 'redeem' | 'history'
const vouchers = ref([])
const redeemable = ref([])
const loyaltyPoints = ref(0)
const isLoading = ref(false)
const isLoadingRedeem = ref(false)
const error = ref('')

const realtime = useSeatRealtime({
  onVoucherUpdate: () => {
    fetchVouchers()
    fetchRedeemable()
    fetchPoints()
  }
})

// Trạng thái đổi điểm
const redeemTarget = ref(null) // promotion đang chờ xác nhận đổi
const isRedeeming = ref(false)

const formatDiscount = (v) => {
  if (v.discountType === 'PERCENTAGE') return `Giảm ${Number(v.discountValue)}%`
  return `Giảm ${Number(v.discountValue).toLocaleString('vi-VN')}đ`
}

const formatDate = (iso) => {
  if (!iso) return '—'
  const d = new Date(iso)
  return d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

// Voucher còn hạn & chưa dùng -> tab "Voucher của tôi" (bao gồm cả còn lượt và hết lượt)
const activeVouchers = computed(() => {
  const list = vouchers.value.filter(v => v.status === 'ACTIVE' || v.status === 'EXHAUSTED')
  // Sắp xếp: ACTIVE lên trước, EXHAUSTED xuống cuối
  list.sort((a, b) => {
    if (a.status === b.status) return 0
    return a.status === 'ACTIVE' ? -1 : 1
  })
  return list.map(v => {
    let desc = v.description
    if (!desc || desc.trim() === '') {
      if (v.applicableMovieTitle) {
        desc = `Áp dụng riêng khi mua vé phim "${v.applicableMovieTitle}".`
      } else {
        desc = `Mã ưu đãi ${v.code}. Áp dụng khi đặt vé tại DevCine.`
      }
    }
    return {
      id: v.code,
      rawId: v.id,
      title: v.title && v.title.trim() ? v.title : formatDiscount(v),
      discountBadge: formatDiscount(v),
      description: desc,
      type: v.discountType === 'PERCENTAGE' ? 'Giảm %' : 'Giảm tiền',
      expiry: formatDate(v.validUntil),
      applicableMovieId: v.applicableMovieId,
      applicableMovieTitle: v.applicableMovieTitle,
      minOrderValue: Number(v.minOrderValue || 0),
      maxDiscountAmount: Number(v.maxDiscountAmount || 0),
      maxTicketQuantity: Number(v.maxTicketQuantity || 0),
      status: v.status,
      isExhausted: v.status === 'EXHAUSTED'
    }
  })
})

// Voucher đã dùng / hết hạn -> tab "Lịch sử"
const historyVouchers = computed(() => vouchers.value
  .filter(v => v.status === 'USED' || v.status === 'EXPIRED')
  .map(v => ({
    usedAt: v.usedAt ? formatDate(v.usedAt) : '—', // '—' cho voucher hết hạn hoặc dữ liệu cũ chưa lưu mốc dùng
    date: formatDate(v.validUntil),
    code: v.code,
    description: v.title && v.title.trim() ? v.title : formatDiscount(v),
    applicableMovieTitle: v.applicableMovieTitle,
    status: v.status === 'USED' ? 'Đã sử dụng'
           : v.status === 'EXHAUSTED' ? 'Hết lượt dùng'
           : 'Đã hết hạn'
  })))

// Phân trang lịch sử voucher
const HISTORY_PAGE_SIZE = 8
const historyPage = ref(1)
const historyTotalPages = computed(() => Math.max(1, Math.ceil(historyVouchers.value.length / HISTORY_PAGE_SIZE)))
const pagedHistory = computed(() => {
  const start = (historyPage.value - 1) * HISTORY_PAGE_SIZE
  return historyVouchers.value.slice(start, start + HISTORY_PAGE_SIZE)
})
const historyPageNumbers = computed(() => {
  const total = historyTotalPages.value, cur = historyPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = [1]
  if (cur > 3) pages.push('...')
  for (let i = Math.max(2, cur - 1); i <= Math.min(total - 1, cur + 1); i++) pages.push(i)
  if (cur < total - 2) pages.push('...')
  pages.push(total)
  return pages
})
const goToHistoryPage = (p) => { if (typeof p === 'number' && p >= 1 && p <= historyTotalPages.value) historyPage.value = p }
watch(() => activeTab.value, () => { historyPage.value = 1 }) // đổi tab -> về trang 1
watch(historyTotalPages, (n) => { if (historyPage.value > n) historyPage.value = n }) // dữ liệu giảm -> kẹp lại

const fetchVouchers = async () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) return
  isLoading.value = true
  error.value = ''
  try {
    const { data } = await voucherApi.getAllVouchers(authStore.user.id)
    vouchers.value = data.data ?? data
  } catch (err) {
    error.value = 'Không thể tải danh sách voucher.'
  } finally {
    isLoading.value = false
  }
}

const fetchPoints = async () => {
  if (!authStore.user?.id) return
  try {
    const { data } = await customerApi.getProfile(authStore.user.id)
    loyaltyPoints.value = data.loyaltyPoints ?? 0
  } catch (err) {
    // điểm không tải được không nên chặn cả màn
  }
}

const fetchRedeemable = async () => {
  isLoadingRedeem.value = true
  try {
    const { data } = await voucherApi.getRedeemable()
    redeemable.value = data.data ?? data
  } catch (err) {
    error.value = 'Không thể tải danh sách ưu đãi đổi điểm.'
  } finally {
    isLoadingRedeem.value = false
  }
}

// Tìm kiếm voucher đang có + tra cứu / lưu mã mới
const voucherSearch = ref('')
const lookupResult = ref(null)
const isLookingUp = ref(false)
const isClaiming = ref(false)
let lookupTimer = null

const filteredActiveVouchers = computed(() => {
  const q = voucherSearch.value.trim().toLowerCase()
  if (!q) return activeVouchers.value
  return activeVouchers.value.filter(v =>
    v.id.toLowerCase().includes(q) || v.title.toLowerCase().includes(q))
})

const handleVoucherSearchInput = () => {
  if (lookupTimer) clearTimeout(lookupTimer)
  lookupResult.value = null
  const code = voucherSearch.value.trim()
  if (!code || !authStore.user?.id) return
  lookupTimer = setTimeout(async () => {
    isLookingUp.value = true
    try {
      const { data } = await voucherApi.lookup(authStore.user.id, code)
      const d = data.data ?? data
      // Gợi ý lưu mã khi tìm thấy (cả trường hợp còn lưu được hoặc hết lượt để hiển thị badge thông báo)
      lookupResult.value = (d.found && (d.claimable || d.reason === 'EXHAUSTED' || d.reason === 'PAUSED')) ? d : null
    } catch {
      lookupResult.value = null
    } finally {
      isLookingUp.value = false
    }
  }, 400)
}

const handleClaim = async () => {
  if (!lookupResult.value || !authStore.user?.id) return
  isClaiming.value = true
  try {
    await voucherApi.claim(authStore.user.id, lookupResult.value.code)
    showToast(`Đã lưu mã ${lookupResult.value.code} vào ví!`, 'success')
    lookupResult.value = null
    voucherSearch.value = ''
    await fetchVouchers()
  } catch (err) {
    showToast(friendlyError(err, 'Lưu mã thất bại.'), 'error')
  } finally {
    isClaiming.value = false
  }
}

const handleUseVoucher = async (code) => {
  try {
    await navigator.clipboard.writeText(code)
    showToast(`Đã sao chép mã ${code}. Dán vào ô voucher khi đặt vé!`, 'success')
  } catch {
    showToast(`Mã của bạn: ${code}`, 'success')
  }
}

const askRedeem = (promo) => { redeemTarget.value = promo }
const cancelRedeem = () => { redeemTarget.value = null }

const confirmRedeem = async () => {
  if (!redeemTarget.value || !authStore.user?.id) return
  isRedeeming.value = true
  try {
    const { data } = await voucherApi.redeem(authStore.user.id, redeemTarget.value.id)
    loyaltyPoints.value = data.remainingPoints ?? loyaltyPoints.value
    showToast('Đổi ưu đãi thành công! Voucher đã vào ví của bạn.', 'success')
    redeemTarget.value = null
    await Promise.all([fetchVouchers(), fetchRedeemable()])
  } catch (err) {
    showToast(friendlyError(err, 'Đổi ưu đãi thất bại.'), 'error')
  } finally {
    isRedeeming.value = false
  }
}

onMounted(() => {
  fetchVouchers()
  fetchPoints()
  fetchRedeemable()
  realtime.connect(null)
})

onUnmounted(() => {
  if (lookupTimer) clearTimeout(lookupTimer)
  realtime.disconnect()
})
</script>

<template>
  <section class="w-full">
    <div class="flex flex-col md:flex-row justify-between items-start md:items-baseline mb-6 sm:mb-8 gap-4">
      <div class="flex items-center gap-3 sm:gap-4 flex-wrap">
        <h2 class="text-xl sm:text-2xl font-bold tracking-tight font-headline">Ưu đãi của tôi</h2>
        <span class="inline-flex items-center gap-1.5 text-xs font-bold px-2.5 sm:px-3 py-1 sm:py-1.5 rounded-full bg-primary-container/15 text-primary-container border border-primary-container/30">
          <span class="material-symbols-outlined text-sm" style="font-variation-settings: 'FILL' 1;">stars</span>
          {{ loyaltyPoints.toLocaleString('vi-VN') }} điểm
        </span>
      </div>
      <div class="flex gap-2 sm:gap-4 overflow-x-auto no-scrollbar w-full md:w-auto pb-1 md:pb-0 touch-pan-x">
        <button
          @click="activeTab = 'active'"
          :class="['text-xs font-bold uppercase tracking-widest pb-1 border-b-2 transition-colors shrink-0', activeTab === 'active' ? 'border-primary-container text-primary-container' : 'border-transparent text-neutral-500 hover:text-on-surface']"
        >
          Voucher của tôi
        </button>
        <button
          @click="activeTab = 'redeem'"
          :class="['text-xs font-bold uppercase tracking-widest pb-1 border-b-2 transition-colors shrink-0', activeTab === 'redeem' ? 'border-primary-container text-primary-container' : 'border-transparent text-neutral-500 hover:text-on-surface']"
        >
          Đổi điểm lấy ưu đãi
        </button>
        <button
          @click="activeTab = 'history'"
          :class="['text-xs font-bold uppercase tracking-widest pb-1 border-b-2 transition-colors shrink-0', activeTab === 'history' ? 'border-primary-container text-primary-container' : 'border-transparent text-neutral-500 hover:text-on-surface']"
        >
          Lịch sử voucher
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="isLoading && activeTab !== 'redeem'" class="grid grid-cols-1 gap-4 sm:gap-5">
      <div v-for="i in 2" :key="i" class="h-36 bg-surface-container-low rounded-xl animate-pulse border border-white/5"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error && activeTab !== 'redeem'" class="p-4 sm:p-6 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-xs sm:text-sm">{{ error }}</div>

    <!-- Tab 1: Active Vouchers -->
    <div v-else-if="activeTab === 'active'" class="space-y-4 sm:space-y-6">
      <!-- Tìm / nhập mã -->
      <div>
        <div class="relative">
          <span class="material-symbols-outlined absolute left-3.5 sm:left-4 top-1/2 -translate-y-1/2 text-on-surface-variant/70 text-lg sm:text-xl pointer-events-none" :class="{ 'animate-spin': isLookingUp }">{{ isLookingUp ? 'progress_activity' : 'search' }}</span>
          <input
            v-model="voucherSearch"
            @input="handleVoucherSearchInput"
            type="text"
            placeholder="Tìm voucher đang có hoặc nhập mã mới để lưu..."
            class="w-full bg-black/30 border border-outline-variant/25 text-xs sm:text-sm text-white pl-10 sm:pl-12 pr-4 py-3 sm:py-4 rounded-xl outline-none transition-all hover:border-outline-variant/40 focus:border-primary-container focus:ring-2 focus:ring-primary-container/30 placeholder:text-on-surface-variant/40 shadow-lg"
          />
        </div>

        <!-- Voucher mới tìm thấy theo mã -> cho phép lưu hoặc hiển thị trạng thái hết lượt -->
        <div v-if="lookupResult" class="mt-4 relative bg-primary-container/5 rounded-xl flex overflow-hidden border border-primary-container/30 shadow-xl">
          <div class="w-20 sm:w-24 md:w-32 bg-primary-container/15 flex flex-col items-center justify-center border-r border-dashed border-white/20 p-3 sm:p-4 shrink-0">
            <span class="material-symbols-outlined text-3xl sm:text-4xl text-primary-container mb-1 sm:mb-2" style="font-variation-settings: 'FILL' 1;">redeem</span>
            <span class="text-[8px] sm:text-[9px] font-bold uppercase tracking-widest text-primary-container text-center">Mã mới</span>
          </div>
          <div class="p-4 sm:p-6 flex-grow flex flex-col min-w-0">
            <div class="flex flex-col sm:flex-row sm:justify-between sm:items-start mb-2 gap-2">
              <h3 class="text-base sm:text-xl font-bold font-headline text-white truncate">{{ formatDiscount(lookupResult) }}</h3>
              <span class="bg-surface-container-high text-[10px] font-bold px-2 py-0.5 sm:py-1 rounded border border-white/10 tracking-widest w-fit">{{ lookupResult.code }}</span>
            </div>
            <p class="text-xs sm:text-sm text-on-surface-variant mb-4 sm:mb-6 flex-grow">
              {{ lookupResult.reason === 'EXHAUSTED' 
                ? 'Mã ưu đãi này đã hết lượt sử dụng, hiện không thể lưu thêm vào ví.' 
                : lookupResult.reason === 'PAUSED'
                ? 'Mã ưu đãi đang tạm dừng áp dụng.'
                : `Mã hợp lệ! Lưu vào ví để dùng cho đơn đặt vé. Hạn dùng đến ${formatDate(lookupResult.endDate)}.` }}
            </p>
            <div class="flex justify-end items-center mt-auto pt-3 sm:pt-4 border-t border-white/5">
              <button 
                v-if="lookupResult.claimable" 
                @click="handleClaim" 
                :disabled="isClaiming" 
                class="bg-primary-container text-on-primary text-[10px] font-bold uppercase tracking-widest px-4 sm:px-5 py-2 sm:py-2.5 hover:bg-primary-fixed-dim transition-colors rounded-sm disabled:opacity-60 flex items-center gap-1.5"
              >
                <span class="material-symbols-outlined text-sm">bookmark_add</span>
                {{ isClaiming ? 'Đang lưu...' : 'Lưu mã này' }}
              </button>
              <span 
                v-else-if="lookupResult.reason === 'EXHAUSTED'" 
                class="inline-flex items-center px-3 py-1.5 rounded-sm bg-amber-500/15 text-amber-400 border border-amber-500/30 text-xs font-bold tracking-wide"
              >
                Mã ưu đãi đã hết lượt sử dụng
              </span>
              <span 
                v-else-if="lookupResult.reason === 'PAUSED'" 
                class="inline-flex items-center px-3 py-1.5 rounded-sm bg-amber-500/15 text-amber-400 border border-amber-500/30 text-xs font-bold tracking-wide"
              >
                Mã ưu đãi đang tạm dừng áp dụng
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Danh sách voucher (1 hàng 1 thẻ full-width) -->
      <div class="grid grid-cols-1 gap-4 sm:gap-5">
      <div 
        v-for="voucher in filteredActiveVouchers" 
        :key="voucher.id" 
        :class="[
          'relative rounded-xl flex overflow-hidden border transition-all shadow-xl',
          voucher.isExhausted 
            ? 'bg-surface-container-low/60 border-white/5 opacity-60 hover:border-white/10' 
            : 'bg-surface-container-low border-white/5 hover:border-primary-container/30'
        ]"
      >
        <div 
          :class="[
            'w-20 sm:w-24 md:w-32 flex flex-col items-center justify-center border-r border-dashed border-white/20 p-3 sm:p-4 shrink-0',
            voucher.isExhausted ? 'bg-neutral-800/40 text-neutral-400' : 'bg-primary-container/10 text-primary-container'
          ]"
        >
          <span class="material-symbols-outlined text-3xl sm:text-4xl mb-1 sm:mb-2" :class="voucher.isExhausted ? 'text-neutral-400' : 'text-primary-container'">loyalty</span>
          <span class="text-[8px] sm:text-[10px] font-bold uppercase tracking-widest text-center" :class="voucher.isExhausted ? 'text-neutral-400' : 'text-primary-container'">{{ voucher.type }}</span>
        </div>
        <div class="p-4 sm:p-6 flex-grow flex flex-col min-w-0">
          <div class="flex flex-col sm:flex-row sm:justify-between sm:items-start mb-2 gap-2">
            <div class="flex items-center gap-2 flex-wrap min-w-0">
              <h3 class="text-base sm:text-xl font-bold font-headline truncate" :class="voucher.isExhausted ? 'text-neutral-300' : 'text-white'">{{ voucher.title }}</h3>
              <span v-if="voucher.title !== voucher.discountBadge" :class="[
                'text-[10px] font-bold px-2 py-0.5 rounded shrink-0',
                voucher.isExhausted ? 'bg-neutral-700/50 text-neutral-300' : 'bg-primary/20 text-primary'
              ]">
                {{ voucher.discountBadge }}
              </span>
            </div>
            <span class="bg-surface-container-high text-[10px] font-bold px-2 py-0.5 sm:py-1 rounded border border-white/10 tracking-widest w-fit">{{ voucher.id }}</span>
          </div>
          <p class="text-xs sm:text-sm text-on-surface-variant mb-2.5 flex-grow">{{ voucher.description }}</p>

          <!-- Thông tin điều kiện áp dụng (Tối giản, cùng hệ màu tinh tế) -->
          <div v-if="voucher.applicableMovieTitle || voucher.minOrderValue > 0 || voucher.maxDiscountAmount > 0 || voucher.maxTicketQuantity > 0" class="flex flex-wrap gap-2 mb-3">
            <span v-if="voucher.applicableMovieTitle" class="inline-flex items-center bg-white/5 text-neutral-300 border border-white/10 text-[10px] font-medium px-2.5 py-1 rounded">
              Phim: {{ voucher.applicableMovieTitle }}
            </span>
            <span v-if="voucher.minOrderValue > 0" class="inline-flex items-center bg-white/5 text-neutral-400 border border-white/10 text-[10px] font-medium px-2.5 py-1 rounded">
              Đơn từ {{ voucher.minOrderValue.toLocaleString('vi-VN') }}đ
            </span>
            <span v-if="voucher.maxDiscountAmount > 0" class="inline-flex items-center bg-white/5 text-neutral-400 border border-white/10 text-[10px] font-medium px-2.5 py-1 rounded">
              Tối đa {{ voucher.maxDiscountAmount.toLocaleString('vi-VN') }}đ
            </span>
            <span v-if="voucher.maxTicketQuantity > 0" class="inline-flex items-center bg-white/5 text-neutral-400 border border-white/10 text-[10px] font-medium px-2.5 py-1 rounded">
              Tối đa {{ voucher.maxTicketQuantity }} vé
            </span>
          </div>

          <div class="flex justify-between items-center mt-auto pt-3 sm:pt-4 border-t border-white/5 gap-2">
            <div>
              <p class="text-[8px] sm:text-[9px] uppercase tracking-widest text-neutral-500 mb-0.5">Ngày hết hạn</p>
              <p class="text-xs font-bold" :class="voucher.isExhausted ? 'text-neutral-400' : 'text-error'">{{ voucher.expiry }}</p>
            </div>
            <div class="flex items-center gap-2.5">
              <span v-if="voucher.isExhausted" class="text-[11px] text-neutral-400 hidden sm:inline">
                Đã hết lượt sử dụng toàn hệ thống
              </span>
              <button v-if="!voucher.isExhausted" @click="handleUseVoucher(voucher.id)" class="bg-primary-container text-on-primary text-[10px] font-bold uppercase tracking-widest px-4 py-2 hover:bg-primary-fixed-dim transition-colors rounded-sm shrink-0 shadow-sm">
                Dùng ngay
              </button>
              <span v-else class="inline-flex items-center bg-neutral-800 text-neutral-400 border border-neutral-700/80 text-[10px] font-bold uppercase tracking-widest px-3.5 sm:px-4 py-1.5 sm:py-2 rounded-sm shrink-0 select-none">
                Hết lượt dùng
              </span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="filteredActiveVouchers.length === 0 && voucherSearch.trim()" class="col-span-full py-12 sm:py-16 text-center border border-dashed border-white/10 rounded-xl px-4">
        <span class="material-symbols-outlined text-3xl sm:text-4xl text-neutral-600 mb-3">search_off</span>
        <p class="text-xs sm:text-sm text-neutral-400">Không có voucher nào trong ví khớp "{{ voucherSearch }}".</p>
        <p class="text-[11px] sm:text-xs text-neutral-500 mt-1">Nếu đây là mã mới, hệ thống sẽ gợi ý nút "Lưu mã" ở trên.</p>
      </div>
      <div v-else-if="activeVouchers.length === 0" class="col-span-full py-16 sm:py-20 text-center border border-dashed border-white/10 rounded-xl px-4">
        <span class="material-symbols-outlined text-3xl sm:text-4xl text-neutral-600 mb-4">sentiment_dissatisfied</span>
        <p class="text-xs sm:text-sm text-neutral-400">Bạn chưa có voucher nào trong ví.</p>
        <button @click="activeTab = 'redeem'" class="mt-4 text-xs font-bold uppercase tracking-widest text-primary-container hover:underline">Đổi điểm lấy ưu đãi ngay →</button>
      </div>
      </div>
    </div>

    <!-- Tab 2: Redeem with points -->
    <div v-else-if="activeTab === 'redeem'">
      <!-- Loading -->
      <div v-if="isLoadingRedeem" class="grid grid-cols-1 gap-4 sm:gap-5">
        <div v-for="i in 2" :key="i" class="h-36 bg-surface-container-low rounded-xl animate-pulse border border-white/5"></div>
      </div>

      <!-- Empty -->
      <div v-else-if="redeemable.length === 0" class="py-16 sm:py-20 text-center border border-dashed border-white/10 rounded-xl px-4">
        <span class="material-symbols-outlined text-3xl sm:text-4xl text-neutral-600 mb-4">redeem</span>
        <p class="text-xs sm:text-sm text-neutral-400">Hiện chưa có ưu đãi nào để đổi bằng điểm.</p>
      </div>

      <!-- List -->
      <div v-else class="grid grid-cols-1 gap-4 sm:gap-5">
        <div v-for="promo in redeemable" :key="promo.id" class="relative bg-surface-container-low rounded-xl flex overflow-hidden border border-white/5 hover:border-primary-container/30 transition-all shadow-xl">
          <div class="w-20 sm:w-24 md:w-32 bg-amber-500/10 flex flex-col items-center justify-center border-r border-dashed border-white/20 p-3 sm:p-4 shrink-0">
            <span class="material-symbols-outlined text-3xl sm:text-4xl text-amber-400 mb-1 sm:mb-2" style="font-variation-settings: 'FILL' 1;">stars</span>
            <span class="text-[9px] sm:text-[10px] font-bold uppercase tracking-widest text-amber-400 text-center">{{ promo.pointsRequired.toLocaleString('vi-VN') }} điểm</span>
          </div>
          <div class="p-4 sm:p-6 flex-grow flex flex-col min-w-0">
            <div class="flex flex-col sm:flex-row sm:justify-between sm:items-start mb-2 gap-2">
              <h3 class="text-base sm:text-xl font-bold font-headline text-white truncate">{{ formatDiscount(promo) }}</h3>
              <span class="bg-surface-container-high text-[10px] font-bold px-2 py-0.5 sm:py-1 rounded border border-white/10 tracking-widest w-fit">{{ promo.code }}</span>
            </div>
            <p class="text-xs sm:text-sm text-on-surface-variant mb-4 sm:mb-6 flex-grow">Đổi <span class="text-amber-400 font-bold">{{ promo.pointsRequired.toLocaleString('vi-VN') }} điểm</span> lấy mã ưu đãi này. Hạn dùng đến {{ formatDate(promo.endDate) }}.</p>
            <div class="flex justify-end items-center mt-auto pt-3 sm:pt-4 border-t border-white/5">
              <button
                @click="askRedeem(promo)"
                :disabled="promo.redeemed || loyaltyPoints < promo.pointsRequired"
                class="bg-primary-container text-on-primary text-[10px] font-bold uppercase tracking-widest px-3.5 sm:px-4 py-1.5 sm:py-2 hover:bg-primary-fixed-dim transition-colors rounded-sm disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {{ promo.redeemed ? 'Đã đổi' : loyaltyPoints < promo.pointsRequired ? 'Chưa đủ điểm' : 'Đổi ngay' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab 3: Voucher History -->
    <div v-else-if="activeTab === 'history'">
      <div class="bg-surface-container-low rounded-xl border border-white/5 overflow-hidden shadow-xl">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-surface-container-high border-b border-white/5">
              <th class="py-4 px-6 text-[10px] uppercase font-bold tracking-widest text-on-surface-variant whitespace-nowrap">Thời điểm sử dụng</th>
              <th class="py-4 px-6 text-[10px] uppercase font-bold tracking-widest text-on-surface-variant whitespace-nowrap">Hạn sử dụng</th>
              <th class="py-4 px-6 text-[10px] uppercase font-bold tracking-widest text-on-surface-variant whitespace-nowrap">Mã voucher</th>
              <th class="py-4 px-6 text-[10px] uppercase font-bold tracking-widest text-on-surface-variant">Nội dung voucher</th>
              <th class="py-4 px-6 text-[10px] uppercase font-bold tracking-widest text-on-surface-variant whitespace-nowrap text-right">Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in pagedHistory" :key="index" class="border-b border-white/5 last:border-0 hover:bg-white/5 transition-colors">
              <td class="py-4 px-6 text-sm whitespace-nowrap">{{ item.usedAt }}</td>
              <td class="py-4 px-6 text-sm whitespace-nowrap">{{ item.date }}</td>
              <td class="py-4 px-6">
                <span class="bg-surface-container-highest px-2 py-1 rounded text-[10px] font-bold tracking-widest border border-white/5 inline-block">{{ item.code }}</span>
              </td>
              <td class="py-4 px-6 text-sm text-neutral-300">{{ item.description }}</td>
              <td class="py-4 px-6 whitespace-nowrap text-right">
                <span :class="[
                  'text-[9px] font-bold uppercase tracking-widest px-2 py-1 rounded-sm inline-block',
                  item.status === 'Đã sử dụng'  ? 'bg-primary-container/20 text-primary-container'
                  : item.status === 'Hết lượt dùng' ? 'bg-amber-500/20 text-amber-400'
                  : 'bg-error/20 text-error'
                ]">
                  {{ item.status }}
                </span>
              </td>
            </tr>
            <tr v-if="historyVouchers.length === 0">
              <td colspan="5" class="py-12 text-center text-neutral-500">Chưa có lịch sử sử dụng voucher.</td>
            </tr>
          </tbody>
        </table>
      </div>
      </div>

      <!-- Phân trang -->
      <div v-if="historyTotalPages > 1" class="mt-8 flex items-center justify-center gap-2">
        <button @click="goToHistoryPage(historyPage - 1)" :disabled="historyPage === 1"
                class="w-9 h-9 flex items-center justify-center rounded border border-white/10 text-on-surface-variant hover:bg-surface-container-highest hover:text-white transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent">
          <span class="material-symbols-outlined text-lg">chevron_left</span>
        </button>
        <template v-for="(p, i) in historyPageNumbers" :key="i">
          <span v-if="p === '...'" class="w-9 h-9 flex items-center justify-center text-on-surface-variant">…</span>
          <button v-else @click="goToHistoryPage(p)"
                  :class="['w-9 h-9 flex items-center justify-center rounded text-sm font-bold transition-colors border', p === historyPage ? 'bg-primary-container text-on-primary border-primary-container' : 'border-white/10 text-on-surface-variant hover:bg-surface-container-highest hover:text-white']">
            {{ p }}
          </button>
        </template>
        <button @click="goToHistoryPage(historyPage + 1)" :disabled="historyPage === historyTotalPages"
                class="w-9 h-9 flex items-center justify-center rounded border border-white/10 text-on-surface-variant hover:bg-surface-container-highest hover:text-white transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent">
          <span class="material-symbols-outlined text-lg">chevron_right</span>
        </button>
      </div>
    </div>

    <!-- Confirm redeem modal -->
    <div v-if="redeemTarget" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" @click="cancelRedeem"></div>
      <div class="relative w-full max-w-sm bg-surface-container-low border border-white/10 rounded-2xl p-6 shadow-2xl">
        <div class="flex flex-col items-center text-center">
          <span class="material-symbols-outlined text-4xl text-amber-400 mb-3" style="font-variation-settings: 'FILL' 1;">stars</span>
          <h3 class="text-lg font-bold font-headline text-white mb-2">Xác nhận đổi ưu đãi</h3>
          <p class="text-sm text-on-surface-variant">
            Bạn sẽ dùng <span class="text-amber-400 font-bold">{{ redeemTarget.pointsRequired.toLocaleString('vi-VN') }} điểm</span>
            để đổi lấy <span class="text-white font-bold">{{ formatDiscount(redeemTarget) }}</span>.
          </p>
          <p class="text-xs text-neutral-500 mt-2">Điểm hiện tại: {{ loyaltyPoints.toLocaleString('vi-VN') }}</p>
          <p class="text-xs text-neutral-500 mt-1">Điểm còn lại sau khi đổi: {{ (loyaltyPoints - redeemTarget.pointsRequired).toLocaleString('vi-VN') }}</p>
        </div>
        <div class="flex gap-3 mt-6">
          <button @click="cancelRedeem" class="flex-1 px-4 py-3 rounded-xl border border-white/15 text-xs font-bold uppercase tracking-widest hover:bg-white/5 transition-colors">Huỷ</button>
          <button @click="confirmRedeem" :disabled="isRedeeming" class="flex-1 px-4 py-3 rounded-xl bg-primary-container text-on-primary text-xs font-bold uppercase tracking-widest hover:brightness-110 transition-all disabled:opacity-60">
            {{ isRedeeming ? 'Đang đổi...' : 'Xác nhận' }}
          </button>
        </div>
      </div>
    </div>

  </section>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s, transform 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>
