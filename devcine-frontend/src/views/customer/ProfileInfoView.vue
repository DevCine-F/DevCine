<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { customerApi } from '@/api/customer'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'
import BookingHistoryView from './BookingHistoryView.vue'

const authStore = useAuthStore()
const toast = useToastStore()
const customer = ref(null)
const isLoading = ref(true)
const isSaving = ref(false)
const isEditing = ref(false)
const editForm = ref({ fullName: '', email: '', phone: '', dob: '' })
const pointHistory = ref([])
const isLoadingHistory = ref(false)

// Phân trang lịch sử điểm
const POINT_PAGE_SIZE = 8
const pointPage = ref(1)
const pointTotalPages = computed(() => Math.max(1, Math.ceil(pointHistory.value.length / POINT_PAGE_SIZE)))
const pagedPointHistory = computed(() => {
  const start = (pointPage.value - 1) * POINT_PAGE_SIZE
  return pointHistory.value.slice(start, start + POINT_PAGE_SIZE)
})
const pointPageNumbers = computed(() => {
  const total = pointTotalPages.value, cur = pointPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = [1]
  if (cur > 3) pages.push('...')
  for (let i = Math.max(2, cur - 1); i <= Math.min(total - 1, cur + 1); i++) pages.push(i)
  if (cur < total - 2) pages.push('...')
  pages.push(total)
  return pages
})
const goToPointPage = (p) => { if (typeof p === 'number' && p >= 1 && p <= pointTotalPages.value) pointPage.value = p }
watch(pointTotalPages, (n) => { if (pointPage.value > n) pointPage.value = n }) // dữ liệu giảm -> kẹp lại

const fetchProfile = async () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) return
  isLoading.value = true
  try {
    const { data } = await customerApi.getProfile(authStore.user.id)
    customer.value = data
    editForm.value = { fullName: data.fullName || '', email: data.email || '', phone: data.phone || '', dob: data.dob || '' }
  } catch (err) {
    console.error('Failed to fetch profile', err)
    toast.error(friendlyError(err, 'Không tải được thông tin hồ sơ.'))
  } finally {
    isLoading.value = false
  }
}

const fetchPointHistory = async () => {
  if (!authStore.user?.id) return
  isLoadingHistory.value = true
  try {
    const { data } = await customerApi.pointHistory(authStore.user.id)
    pointHistory.value = Array.isArray(data) ? data : (data?.data ?? [])
  } catch (err) {
    toast.error(friendlyError(err, 'Không tải được lịch sử điểm thưởng.'))
  } finally {
    isLoadingHistory.value = false
  }
}

const pointSourceLabel = (t) => {
  const map = {
    BOOKING: 'Mua vé', FNB: 'Mua bắp nước', PROMO_REDEEM: 'Đổi ưu đãi',
    VOID_FNB: 'Hủy hóa đơn F&B', ADMIN: 'Điều chỉnh',
  }
  return map[t?.source] || map[t?.type] || t?.source || t?.type || '—'
}
const pointDateLabel = (v) => v ? new Date(v).toLocaleString('vi-VN') : '—'

const handleSaveProfile = async () => {
  const email = editForm.value.email?.trim() || ''
  if (!email || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
    toast.error('Email không hợp lệ, vui lòng kiểm tra lại.')
    return
  }
  isSaving.value = true
  try {
    const res = await customerApi.updateProfile(authStore.user.id, editForm.value)
    customer.value = res.data?.data ?? res.data
    isEditing.value = false
    toast.success('Cập nhật hồ sơ thành công.')
  } catch (err) {
    toast.error(friendlyError(err, 'Cập nhật thất bại.'))
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  fetchProfile()
  fetchPointHistory()
})

const formattedMemberSince = computed(() => {
  if (!customer.value?.createdAt) return '12/2025'
  const d = new Date(customer.value.createdAt)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const yyyy = d.getFullYear()
  return `${mm}/${yyyy}`
})

const formattedDob = computed(() => {
  if (!customer.value?.dob) return 'Chưa cập nhật'
  const d = new Date(customer.value.dob)
  const dd = String(d.getDate()).padStart(2, '0')
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const yyyy = d.getFullYear()
  return `${dd}/${mm}/${yyyy}`
})

const tierInfo = computed(() => {
  const tier = (customer.value?.membershipTier || 'BRONZE').toUpperCase()
  // Hạng & tiến độ tính theo ĐIỂM TÍCH LŨY TRỌN ĐỜI (không tụt khi khách đổi điểm); fallback số dư nếu thiếu.
  const points = customer.value?.lifetimePoints ?? customer.value?.loyaltyPoints ?? 0

  if (tier === 'PLATINUM' || points >= 10000) {
    return {
      name: 'Platinum',
      nextTier: 'Max Tier',
      pointsNeeded: 0,
      percent: 100,
      colorClass: 'text-sky-200',
      bgGradient: 'from-[#0c1420] via-[#16243a] to-[#0a0e16] border-sky-400/30',
      badgeBg: 'bg-sky-400/15 text-sky-200 border border-sky-400/40',
      glow: 'shadow-[0_0_38px_rgba(56,189,248,0.28)]',
      cardPattern: 'radial-gradient(circle at 80% 20%, rgba(56,189,248,0.18) 0%, transparent 60%)',
      cardIcon: 'workspace_premium'
    }
  } else if (tier === 'GOLD' || points >= 5000) {
    const needed = 10000 - points
    const base = 5000
    const percent = Math.min(100, Math.max(0, ((points - base) / 5000) * 100))
    return {
      name: 'Gold',
      nextTier: 'Platinum',
      pointsNeeded: needed,
      percent,
      colorClass: 'text-primary',
      bgGradient: 'from-[#241c06] via-[#3a2e08] to-[#16110a] border-primary/40',
      badgeBg: 'bg-primary/15 text-primary border border-primary/40',
      glow: 'shadow-[0_0_38px_rgba(245,197,24,0.3)]',
      cardPattern: 'radial-gradient(circle at 80% 20%, rgba(245,197,24,0.2) 0%, transparent 60%)',
      cardIcon: 'military_tech'
    }
  } else if (tier === 'SILVER' || points >= 2000) {
    const needed = 5000 - points
    const base = 2000
    const percent = Math.min(100, Math.max(0, ((points - base) / 3000) * 100))
    return {
      name: 'Silver',
      nextTier: 'Gold',
      pointsNeeded: needed,
      percent,
      colorClass: 'text-slate-100',
      bgGradient: 'from-[#1b1e24] via-[#2b313b] to-[#101216] border-slate-300/30',
      badgeBg: 'bg-slate-300/15 text-slate-100 border border-slate-300/30',
      glow: 'shadow-[0_0_32px_rgba(203,213,225,0.18)]',
      cardPattern: 'radial-gradient(circle at 80% 20%, rgba(203,213,225,0.14) 0%, transparent 60%)',
      cardIcon: 'stars'
    }
  } else {
    const needed = 2000 - points
    const percent = Math.min(100, Math.max(0, (points / 2000) * 100))
    return {
      name: 'Bronze',
      nextTier: 'Silver',
      pointsNeeded: needed,
      percent,
      colorClass: 'text-amber-500',
      bgGradient: 'from-[#201712] via-[#3b2a1b] to-[#130d09] border-amber-600/30',
      badgeBg: 'bg-amber-600/15 text-amber-500 border border-amber-600/40',
      glow: 'shadow-[0_0_30px_rgba(217,119,6,0.2)]',
      cardPattern: 'radial-gradient(circle at 80% 20%, rgba(217,119,6,0.18) 0%, transparent 60%)',
      cardIcon: 'hotel_class'
    }
  }
})
</script>

<template>
  <div class="space-y-10">
    <!-- Loading State -->
    <div v-if="isLoading" class="grid grid-cols-1 lg:grid-cols-12 gap-8 mb-6 animate-pulse items-start">
      <div class="lg:col-span-7 h-[240px] bg-surface-container-low rounded-3xl border border-white/5"></div>
      <div class="lg:col-span-5 h-[460px] bg-surface-container-low rounded-3xl border border-white/5"></div>
    </div>

    <!-- Loaded State -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
      <!-- Profile Details Card -->
      <div class="lg:col-span-7 bg-surface-container-low border border-outline-variant/10 rounded-3xl p-8 relative overflow-hidden flex flex-col transition-all duration-300">
        <!-- Decoration background glow -->
        <div class="absolute -top-32 -left-32 w-64 h-64 bg-primary/5 rounded-full blur-3xl pointer-events-none"></div>
        
        <div class="relative z-10 flex flex-col md:flex-row gap-8 items-start md:items-center">
          <!-- User Avatar -->
          <div class="relative w-28 h-28 md:w-32 md:h-32 shrink-0 rounded-2xl overflow-hidden border border-white/10 shadow-2xl">
            <img class="w-full h-full object-cover" src="/images/Hopper.webp" alt="Avatar"/>
            <div class="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
          </div>
          
          <!-- Basic Info -->
          <div class="flex-grow space-y-4">
            <div class="flex items-center gap-3">
              <span :class="tierInfo.badgeBg" class="text-[9px] font-bold tracking-[0.15em] uppercase px-2.5 py-1 rounded-md">
                THÀNH VIÊN {{ tierInfo.name }}
              </span>
              <span class="text-on-surface-variant/70 text-xs font-mono">ID: #DC-{{ customer?.userId }}</span>
            </div>
            
            <h2 class="text-3xl font-extrabold tracking-tight font-headline text-white">
              {{ customer?.fullName || 'Khách hàng' }}
            </h2>
            
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-3 pt-2">
              <div>
                <p class="text-[9px] uppercase font-bold tracking-widest text-on-surface-variant mb-0.5">Email</p>
                <p class="text-sm font-semibold text-white/90 truncate">{{ customer?.email }}</p>
              </div>
              <div>
                <p class="text-[9px] uppercase font-bold tracking-widest text-on-surface-variant mb-0.5">Số điện thoại</p>
                <p class="text-sm font-semibold text-white/90">{{ customer?.phone || 'Chưa cập nhật' }}</p>
              </div>
              <div>
                <p class="text-[9px] uppercase font-bold tracking-widest text-on-surface-variant mb-0.5">Ngày sinh</p>
                <p class="text-sm font-semibold text-white/90">{{ formattedDob }}</p>
              </div>
              <div>
                <p class="text-[9px] uppercase font-bold tracking-widest text-on-surface-variant mb-0.5">Thành viên từ</p>
                <p class="text-sm font-semibold text-white/90">{{ formattedMemberSince }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Edit Form with Expansion Animation -->
        <Transition name="expand">
          <div v-if="isEditing" class="mt-6 pt-6 border-t border-outline-variant/10">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label class="block text-[9px] uppercase font-bold tracking-widest text-on-surface-variant mb-1">Họ và tên</label>
                <input v-model="editForm.fullName" type="text" class="w-full bg-surface-container-highest border-none text-sm text-white px-3 py-2.5 rounded focus:ring-1 focus:ring-primary-container" placeholder="Họ và tên" />
              </div>
              <div>
                <label class="block text-[9px] uppercase font-bold tracking-widest text-on-surface-variant mb-1">Email</label>
                <input v-model="editForm.email" type="email" class="w-full bg-surface-container-highest border-none text-sm text-white px-3 py-2.5 rounded focus:ring-1 focus:ring-primary-container" placeholder="email@devcine.com" />
              </div>
              <div>
                <label class="block text-[9px] uppercase font-bold tracking-widest text-on-surface-variant mb-1">Số điện thoại</label>
                <input v-model="editForm.phone" type="tel" class="w-full bg-surface-container-highest border-none text-sm text-white px-3 py-2.5 rounded focus:ring-1 focus:ring-primary-container" placeholder="0912345678" />
              </div>
              <div>
                <label class="block text-[9px] uppercase font-bold tracking-widest text-on-surface-variant mb-1">Ngày sinh</label>
                <input v-model="editForm.dob" type="date" class="w-full bg-surface-container-highest border-none text-sm text-white px-3 py-2.5 rounded focus:ring-1 focus:ring-primary-container" />
              </div>
            </div>
          </div>
        </Transition>

        <div class="mt-6 pt-4 border-t border-outline-variant/10 flex justify-between items-center">
          <div class="flex items-center gap-2 text-xs text-on-surface-variant">
            <span class="material-symbols-outlined text-green-400 text-lg">verified_user</span>
            <p>Tài khoản cá nhân được liên kết an toàn</p>
          </div>
          <div class="flex gap-2">
            <button v-if="!isEditing" @click="isEditing = true" class="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider px-4 py-2 border border-outline-variant/20 rounded hover:bg-surface-container-highest transition-colors text-on-surface-variant hover:text-white cursor-pointer">
              <span class="material-symbols-outlined text-sm">edit</span> Chỉnh sửa
            </button>
            <template v-else>
              <button @click="isEditing = false" class="text-xs font-bold uppercase tracking-wider px-4 py-2 border border-outline-variant/20 rounded hover:bg-surface-container-highest transition-colors text-on-surface-variant cursor-pointer">Huỷ</button>
              <button @click="handleSaveProfile" :disabled="isSaving" class="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider px-4 py-2 bg-primary-container text-on-primary rounded hover:brightness-110 transition-all disabled:opacity-60 cursor-pointer">
                <span v-if="isSaving" class="material-symbols-outlined text-sm animate-spin">autorenew</span>
                <span v-else class="material-symbols-outlined text-sm">save</span>
                {{ isSaving ? 'Đang lưu...' : 'Lưu' }}
              </button>
            </template>
          </div>
        </div>
      </div>

      <!-- Virtual Membership Card & Progress -->
      <div class="lg:col-span-5 flex flex-col gap-6">
        <!-- Virtual Membership Card -->
        <div :class="['relative aspect-[1.586/1] w-full rounded-3xl p-6 overflow-hidden flex flex-col justify-between border bg-gradient-to-br transition-all duration-700 shadow-2xl', tierInfo.bgGradient, tierInfo.glow]">
          <!-- Decorative card patterns -->
          <div class="absolute inset-0 opacity-100 pointer-events-none" :style="{ backgroundImage: tierInfo.cardPattern }"></div>
          <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_top_left,rgba(255,255,255,0.05)_0%,transparent_50%)]"></div>
          
          <!-- Card Header -->
          <div class="flex items-start justify-between relative z-10">
            <div>
              <p class="text-[9px] font-black tracking-[0.25em] text-white/60 uppercase">DevCine Cinema</p>
              <h4 class="text-sm font-bold tracking-[0.1em] text-white">MEMBER CARD</h4>
            </div>
            <div class="flex items-center gap-1.5 opacity-95">
              <span class="material-symbols-outlined text-3xl text-white/90" style="font-variation-settings: 'FILL' 1;">
                {{ tierInfo.cardIcon }}
              </span>
            </div>
          </div>

          <!-- Card Body: Large Tier Name -->
          <div class="relative z-10 my-1">
            <h3 :class="['text-4xl font-extrabold italic tracking-tight font-headline uppercase filter drop-shadow-[0_2px_10px_rgba(0,0,0,0.5)]', tierInfo.colorClass]">
              {{ tierInfo.name }}
            </h3>
          </div>

          <!-- Card Footer -->
          <div class="flex items-end justify-between relative z-10 pt-2 border-t border-white/10">
            <div>
              <p class="text-[8px] uppercase tracking-widest text-white/50 mb-0.5">Tên chủ thẻ</p>
              <p class="text-xs font-bold tracking-wide uppercase text-white truncate max-w-[180px]">{{ customer?.fullName }}</p>
            </div>
            <div class="text-right">
              <p class="text-[8px] uppercase tracking-widest text-white/50 mb-0.5">Điểm khả dụng</p>
              <p class="text-sm font-mono font-bold text-white">{{ customer?.loyaltyPoints?.toLocaleString() || 0 }} pts</p>
            </div>
          </div>
        </div>

        <!-- Progress Card -->
        <div class="bg-surface-container-low border border-outline-variant/10 rounded-3xl p-6 flex flex-col justify-between flex-grow">
          <div class="space-y-4">
            <div class="flex justify-between items-center text-xs">
              <span class="font-bold text-on-surface-variant uppercase tracking-wider">Tích lũy trọn đời</span>
              <span class="font-mono font-bold text-white">{{ (customer?.lifetimePoints ?? customer?.loyaltyPoints ?? 0).toLocaleString() }} điểm</span>
            </div>

            <!-- Progress Bar -->
            <div class="relative h-3 w-full bg-surface-container-highest rounded-full overflow-hidden border border-white/5 shadow-inner">
              <div :class="['h-full bg-gradient-to-r from-primary to-amber-500 rounded-full transition-all duration-1000 ease-out shadow-[0_0_10px_rgba(245,197,24,0.3)]']" :style="{ width: `${tierInfo.percent}%` }"></div>
            </div>

            <!-- Progress status text -->
            <div class="flex justify-between text-[10px] uppercase font-bold tracking-wider text-on-surface-variant">
              <span>Hạng hiện tại: {{ tierInfo.name }}</span>
              <span v-if="tierInfo.pointsNeeded > 0">Mục tiêu: {{ tierInfo.nextTier }}</span>
              <span v-else>Hạng cao nhất</span>
            </div>
          </div>

          <!-- Alert / Info Banner -->
          <div class="mt-4 p-4 rounded-xl bg-surface-container-high/60 border border-white/5 flex gap-3 items-center">
            <span class="material-symbols-outlined text-primary-container text-xl" style="font-variation-settings: 'FILL' 1;">stars</span>
            <p v-if="tierInfo.pointsNeeded > 0" class="text-xs text-on-surface-variant leading-relaxed">
              Bạn cần tích lũy thêm <span class="text-white font-bold">{{ tierInfo.pointsNeeded?.toLocaleString() }} điểm</span> để đạt được thứ hạng kế tiếp <span class="text-primary-container font-bold">{{ tierInfo.nextTier }}</span>.
            </p>
            <p v-else class="text-xs text-on-surface-variant leading-relaxed">
              Tuyệt vời! Bạn đang ở hạng thành viên cao nhất <span class="text-primary-container font-bold">Platinum</span> với những ưu đãi đặc quyền tối đa.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Lịch sử điểm thưởng -->
    <div class="bg-surface-container-low border border-outline-variant/10 rounded-3xl p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-sm font-black uppercase tracking-wider text-on-surface flex items-center gap-2">
          <span class="material-symbols-outlined text-primary text-lg">history</span> Lịch sử điểm
        </h3>
        <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Khả dụng: {{ (customer?.loyaltyPoints || 0).toLocaleString() }} điểm</span>
      </div>

      <div v-if="isLoadingHistory" class="space-y-2">
        <div v-for="n in 3" :key="n" class="h-12 rounded-xl bg-surface-container-high animate-pulse"></div>
      </div>
      <div v-else-if="pointHistory.length === 0" class="py-8 text-center text-xs text-on-surface-variant font-semibold">
        Chưa có biến động điểm nào. Điểm sẽ được cộng khi bạn mua vé hoặc bắp nước.
      </div>
      <div v-else class="divide-y divide-outline-variant/10">
        <div v-for="tx in pagedPointHistory" :key="tx.id" class="flex items-center justify-between py-3">
          <div class="min-w-0">
            <p class="text-sm font-bold text-on-surface">{{ pointSourceLabel(tx) }}</p>
            <p class="text-[11px] text-on-surface-variant">{{ pointDateLabel(tx.createdAt) }}<span v-if="tx.refCode"> · {{ tx.refCode }}</span></p>
          </div>
          <div class="text-right shrink-0">
            <p class="text-sm font-black" :class="tx.points >= 0 ? 'text-green-400' : 'text-red-300'">
              {{ tx.points >= 0 ? '+' : '' }}{{ tx.points?.toLocaleString() }} điểm
            </p>
            <p class="text-[10px] text-on-surface-variant">Số dư: {{ tx.balanceAfter?.toLocaleString() }}</p>
          </div>
        </div>
      </div>

      <!-- Phân trang -->
      <div v-if="!isLoadingHistory && pointTotalPages > 1" class="mt-6 flex items-center justify-center gap-2">
        <button @click="goToPointPage(pointPage - 1)" :disabled="pointPage === 1"
                class="w-9 h-9 flex items-center justify-center rounded border border-white/10 text-on-surface-variant hover:bg-surface-container-highest hover:text-white transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent">
          <span class="material-symbols-outlined text-lg">chevron_left</span>
        </button>
        <template v-for="(p, i) in pointPageNumbers" :key="i">
          <span v-if="p === '...'" class="w-9 h-9 flex items-center justify-center text-on-surface-variant">…</span>
          <button v-else @click="goToPointPage(p)"
                  :class="['w-9 h-9 flex items-center justify-center rounded text-sm font-bold transition-colors border', p === pointPage ? 'bg-primary-container text-on-primary border-primary-container' : 'border-white/10 text-on-surface-variant hover:bg-surface-container-highest hover:text-white']">
            {{ p }}
          </button>
        </template>
        <button @click="goToPointPage(pointPage + 1)" :disabled="pointPage === pointTotalPages"
                class="w-9 h-9 flex items-center justify-center rounded border border-white/10 text-on-surface-variant hover:bg-surface-container-highest hover:text-white transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent">
          <span class="material-symbols-outlined text-lg">chevron_right</span>
        </button>
      </div>
    </div>

    <!-- Booking History (preview: 3 lượt gần nhất + CTA xem tất cả) -->
    <BookingHistoryView preview />
  </div>
</template>

<style scoped>
@keyframes slide-in {
  from {
    transform: translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
.animate-slide-in {
  animation: slide-in 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  max-height: 300px;
  overflow: hidden;
  opacity: 1;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
  margin-top: 0;
  padding-top: 0;
  border-top-color: transparent;
  overflow: hidden;
}
</style>
