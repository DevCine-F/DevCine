<script setup>
import { ref, onMounted, computed } from 'vue'
import { customerApi } from '@/api/customer'
import { useAuthStore } from '@/stores/auth'
import BookingHistoryView from './BookingHistoryView.vue'

const authStore = useAuthStore()
const customer = ref(null)
const isLoading = ref(true)
const isSaving = ref(false)
const saveSuccess = ref(false)
const saveError = ref('')
const isEditing = ref(false)
const editForm = ref({ fullName: '', phone: '', dob: '' })

const fetchProfile = async () => {
  if (!authStore.isAuthenticated || !authStore.user?.id) return
  isLoading.value = true
  try {
    const { data } = await customerApi.getProfile(authStore.user.id)
    customer.value = data
    editForm.value = { fullName: data.fullName || '', phone: data.phone || '', dob: data.dob || '' }
  } catch (err) {
    console.error('Failed to fetch profile', err)
  } finally {
    isLoading.value = false
  }
}

const handleSaveProfile = async () => {
  isSaving.value = true
  saveSuccess.value = false
  saveError.value = ''
  try {
    const res = await customerApi.updateProfile(authStore.user.id, editForm.value)
    customer.value = res.data.data
    isEditing.value = false
    saveSuccess.value = true
    setTimeout(() => { saveSuccess.value = false }, 3000)
  } catch (err) {
    saveError.value = err.response?.data?.message || 'Cập nhật thất bại.'
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  fetchProfile()
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
  const points = customer.value?.loyaltyPoints || 0
  
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
    <div v-if="isLoading" class="grid grid-cols-1 lg:grid-cols-12 gap-8 mb-6 animate-pulse">
      <div class="lg:col-span-7 h-[320px] bg-surface-container-low rounded-3xl border border-white/5"></div>
      <div class="lg:col-span-5 h-[320px] bg-surface-container-low rounded-3xl border border-white/5"></div>
    </div>

    <!-- Loaded State -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
      <!-- Profile Details Card -->
      <div class="lg:col-span-7 bg-surface-container-low border border-outline-variant/10 rounded-3xl p-8 relative overflow-hidden flex flex-col justify-between">
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

        <!-- Success / Error banners -->
        <div v-if="saveSuccess" class="mt-4 px-4 py-2 bg-green-500/10 border border-green-500/20 rounded text-green-400 text-xs font-semibold flex items-center gap-2">
          <span class="material-symbols-outlined text-sm">check_circle</span> Cập nhật hồ sơ thành công.
        </div>
        <div v-if="saveError" class="mt-4 px-4 py-2 bg-red-500/10 border border-red-500/20 rounded text-red-400 text-xs font-semibold flex items-center gap-2">
          <span class="material-symbols-outlined text-sm">error</span> {{ saveError }}
        </div>

        <!-- Edit Form -->
        <div v-if="isEditing" class="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="block text-[9px] uppercase font-bold tracking-widest text-on-surface-variant mb-1">Họ và tên</label>
            <input v-model="editForm.fullName" type="text" class="w-full bg-surface-container-highest border-none text-sm text-white px-3 py-2.5 rounded focus:ring-1 focus:ring-primary-container" placeholder="Họ và tên" />
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

        <div class="mt-6 pt-4 border-t border-outline-variant/10 flex justify-between items-center">
          <div class="flex items-center gap-2 text-xs text-on-surface-variant">
            <span class="material-symbols-outlined text-green-400 text-lg">verified_user</span>
            <p>Tài khoản cá nhân được liên kết an toàn</p>
          </div>
          <div class="flex gap-2">
            <button v-if="!isEditing" @click="isEditing = true" class="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider px-4 py-2 border border-outline-variant/20 rounded hover:bg-surface-container-highest transition-colors text-on-surface-variant hover:text-white">
              <span class="material-symbols-outlined text-sm">edit</span> Chỉnh sửa
            </button>
            <template v-else>
              <button @click="isEditing = false; saveError = ''" class="text-xs font-bold uppercase tracking-wider px-4 py-2 border border-outline-variant/20 rounded hover:bg-surface-container-highest transition-colors text-on-surface-variant">Huỷ</button>
              <button @click="handleSaveProfile" :disabled="isSaving" class="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider px-4 py-2 bg-primary-container text-on-primary rounded hover:brightness-110 transition-all disabled:opacity-60">
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
              <p class="text-[8px] uppercase tracking-widest text-white/50 mb-0.5">Điểm tích lũy</p>
              <p class="text-sm font-mono font-bold text-white">{{ customer?.loyaltyPoints?.toLocaleString() || 0 }} pts</p>
            </div>
          </div>
        </div>

        <!-- Progress Card -->
        <div class="bg-surface-container-low border border-outline-variant/10 rounded-3xl p-6 flex flex-col justify-between flex-grow">
          <div class="space-y-4">
            <div class="flex justify-between items-center text-xs">
              <span class="font-bold text-on-surface-variant uppercase tracking-wider">Tình trạng tích lũy</span>
              <span class="font-mono font-bold text-white">{{ customer?.loyaltyPoints?.toLocaleString() || 0 }} điểm</span>
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

    <!-- Booking History -->
    <BookingHistoryView />
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
</style>
