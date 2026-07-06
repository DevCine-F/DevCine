<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppButton from '../../components/common/AppButton.vue'
import { staffShiftApi } from '@/api/admin/index'
import { useShiftStore } from '@/stores/shift'
import { useToastStore } from '@/stores/toast'
import { useConfirmStore } from '@/stores/confirm'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()
const shiftStore = useShiftStore()
const confirm = useConfirmStore()

const today = new Date().toISOString().slice(0, 10)
const fromDate = ref(today)
const toDate = ref(new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10))
const shifts = ref([])
const isLoading = ref(false)
const errorMessage = ref('')

const positions = {
  POS_TICKETING: 'Bán vé POS',
  CHECK_IN: 'Kiểm soát vé',
  FNB: 'Quầy F&B',
  PROJECTION: 'Phòng chiếu',
  SHIFT_LEAD: 'Trưởng ca',
}

const statusLabels = {
  SCHEDULED: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  IN_PROGRESS: 'Đang trong ca',
  COMPLETED: 'Đã kết ca',
  REJECTED: 'Từ chối',
}

const actionLoadingId = ref(null)

const actionByPosition = {
  POS_TICKETING: { label: 'Vào POS', path: '/admin/ticketing', icon: 'confirmation_number' },
  CHECK_IN: { label: 'Kiểm soát vé', path: '/admin/check-in', icon: 'qr_code_scanner' },
  FNB: { label: 'Quầy F&B', path: '/admin/ticketing', icon: 'fastfood' },
  SHIFT_LEAD: { label: 'Vận hành ca', path: '/admin/ticketing', icon: 'dashboard_customize' },
}

const positionLabel = (value) => positions[value] || value || 'Chưa phân công'
const statusLabel = (value) => statusLabels[value] || value || 'Chưa rõ'

const toDateTime = (value) => value ? new Date(value) : null
const formatDate = (value) => {
  if (!value) return ''
  return new Date(`${value}T00:00:00`).toLocaleDateString('vi-VN', {
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}
const formatTime = (value) => value ? value.slice(0, 5) : '--:--'

// Trạng thái vận hành theo status ca (mô hình Check-in):
//  PENDING (chờ duyệt) · WAITING (đã duyệt, chờ vào ca) · ACTIVE (đã check-in) · ENDED · REJECTED
const runtimeState = (shift) => {
  const s = shift.status
  if (s === 'REJECTED') return 'REJECTED'
  if (s === 'IN_PROGRESS') return 'ACTIVE'
  if (s === 'COMPLETED') return 'ENDED'
  if (s !== 'APPROVED') return 'PENDING'
  const end = toDateTime(shift.endAt)
  if (end && new Date() > end) return 'ENDED'
  return 'WAITING'
}

const stateLabel = (shift) => ({
  ACTIVE: 'Đang trong ca',
  WAITING: 'Chờ vào ca',
  ENDED: 'Đã kết thúc',
  PENDING: 'Chờ duyệt',
  REJECTED: 'Từ chối',
}[runtimeState(shift)])

const stateClass = (shift) => ({
  ACTIVE: 'bg-green-500/10 text-green-400 border-green-500/20',
  WAITING: 'bg-blue-500/10 text-blue-300 border-blue-500/20',
  ENDED: 'bg-surface-container-high text-on-surface-variant border-outline-variant/20',
  PENDING: 'bg-primary/10 text-primary border-primary/20',
  REJECTED: 'bg-red-500/10 text-red-300 border-red-500/20',
}[runtimeState(shift)])

const sortedShifts = computed(() => [...shifts.value].sort((a, b) => {
  const first = toDateTime(a.startAt)?.getTime() || 0
  const second = toDateTime(b.startAt)?.getTime() || 0
  return first - second
}))

const currentShift = computed(() => sortedShifts.value.find((shift) => runtimeState(shift) === 'ACTIVE'))
const upcomingShifts = computed(() => sortedShifts.value.filter((shift) => ['WAITING', 'PENDING'].includes(runtimeState(shift))))
const endedShifts = computed(() => sortedShifts.value.filter((shift) => ['ENDED', 'REJECTED'].includes(runtimeState(shift))))
const approvedCount = computed(() => shifts.value.filter((shift) => shift.status === 'APPROVED').length)
const pendingCount = computed(() => shifts.value.filter((shift) => shift.status === 'SCHEDULED').length)

const primaryAction = (shift) => actionByPosition[shift.workPosition] || null
const canCheckIn = (shift) => runtimeState(shift) === 'WAITING'
const canCheckOut = (shift) => shift.status === 'IN_PROGRESS'
const canHandover = (shift) => {
  if (shift.status !== 'IN_PROGRESS' || !shift.endAt) return false
  return new Date() >= new Date(new Date(shift.endAt).getTime() - 30 * 60 * 1000)
}
const handoverRoute = (shift) => ({
  path: '/admin/shift-handover',
  query: { scheduleId: shift.id },
})

const handleCheckIn = async (shift) => {
  const ok = await confirm.show({
    title: 'Vào ca làm việc',
    message: `Bắt đầu ca ${positionLabel(shift.workPosition)} (${formatTime(shift.startTime)} - ${formatTime(shift.endTime)})? Quyền thao tác quầy sẽ được kích hoạt cho ca này.`,
    confirmText: 'Vào ca',
    tone: 'primary',
  })
  if (!ok) return
  actionLoadingId.value = shift.id
  try {
    await staffShiftApi.checkIn(shift.id)
    toast.success('Đã vào ca — quyền thao tác đã được kích hoạt.')
    await fetchShifts()
  } catch (error) {
    toast.error(friendlyError(error, 'Không vào ca được.'))
  } finally {
    actionLoadingId.value = null
  }
}

const handleCheckOut = async (shift) => {
  const ok = await confirm.show({
    title: 'Kết ca làm việc',
    message: `Kết thúc ca ${positionLabel(shift.workPosition)}? Sau khi kết ca, quyền thao tác quầy sẽ bị thu hồi.`,
    confirmText: 'Kết ca',
    tone: 'danger',
  })
  if (!ok) return
  actionLoadingId.value = shift.id
  try {
    await staffShiftApi.checkOut(shift.id)
    toast.success('Đã kết ca.')
    await fetchShifts()
  } catch (error) {
    toast.error(friendlyError(error, 'Không kết ca được.'))
  } finally {
    actionLoadingId.value = null
  }
}

const fetchShifts = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const [myRes] = await Promise.all([
      staffShiftApi.my({ from: fromDate.value, to: toDate.value }),
      shiftStore.fetchCurrent(true),
    ])
    shifts.value = Array.isArray(myRes.data) ? myRes.data : (myRes.data?.data ?? [])
  } catch (error) {
    errorMessage.value = friendlyError(error, 'Không tải được lịch ca của bạn.')
    toast.error(errorMessage.value)
  } finally {
    isLoading.value = false
  }
}

onMounted(fetchShifts)
</script>

<template>
  <div class="p-6 lg:p-10 space-y-6">
    <header class="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
      <div>
        <h1 class="text-2xl lg:text-3xl font-black tracking-tight text-on-surface uppercase">Ca của tôi</h1>
        <p class="text-xs text-on-surface-variant font-bold uppercase tracking-widest mt-1">
          Lịch làm việc cá nhân, vị trí được phân công và trạng thái ca
        </p>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <input v-model="fromDate" type="date" class="bg-surface-container-high rounded-xl px-4 py-3 text-sm font-bold outline-none text-on-surface" />
        <input v-model="toDate" type="date" class="bg-surface-container-high rounded-xl px-4 py-3 text-sm font-bold outline-none text-on-surface" />
        <AppButton :loading="isLoading" @click="fetchShifts">
          <span class="material-symbols-outlined mr-2">refresh</span>
          Tải lại
        </AppButton>
      </div>
    </header>

    <section class="grid grid-cols-1 md:grid-cols-3 gap-3">
      <div class="rounded-xl border border-outline-variant/10 bg-surface-container-low p-4">
        <p class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Tổng ca</p>
        <p class="text-2xl font-black mt-1">{{ shifts.length }}</p>
      </div>
      <div class="rounded-xl border border-outline-variant/10 bg-surface-container-low p-4">
        <p class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Đã duyệt</p>
        <p class="text-2xl font-black mt-1 text-green-400">{{ approvedCount }}</p>
      </div>
      <div class="rounded-xl border border-outline-variant/10 bg-surface-container-low p-4">
        <p class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Chờ duyệt</p>
        <p class="text-2xl font-black mt-1 text-primary">{{ pendingCount }}</p>
      </div>
    </section>

    <section v-if="currentShift" class="rounded-xl border border-green-500/20 bg-green-500/10 p-5 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
      <div class="flex items-start gap-4">
        <div class="w-12 h-12 rounded-xl bg-green-500/15 text-green-300 flex items-center justify-center">
          <span class="material-symbols-outlined">play_circle</span>
        </div>
        <div>
          <p class="text-[10px] font-black uppercase tracking-widest text-green-300">Đang trong ca</p>
          <h2 class="text-xl font-black mt-1">{{ positionLabel(currentShift.workPosition) }}</h2>
          <p class="text-sm text-on-surface-variant font-semibold mt-1">
            {{ formatDate(currentShift.workDate) }} · {{ formatTime(currentShift.startTime) }} - {{ formatTime(currentShift.endTime) }} · {{ currentShift.location || currentShift.cinemaName }}
          </p>
        </div>
      </div>
      <div class="flex flex-wrap gap-3">
        <RouterLink v-if="primaryAction(currentShift)" :to="primaryAction(currentShift).path">
          <AppButton>
            <span class="material-symbols-outlined mr-2">{{ primaryAction(currentShift).icon }}</span>
            {{ primaryAction(currentShift).label }}
          </AppButton>
        </RouterLink>
        <RouterLink v-if="canHandover(currentShift)" :to="handoverRoute(currentShift)">
          <AppButton variant="outline">
            <span class="material-symbols-outlined mr-2">point_of_sale</span>
            Bàn giao ca
          </AppButton>
        </RouterLink>
        <AppButton variant="outline" :loading="actionLoadingId === currentShift.id" @click="handleCheckOut(currentShift)">
          <span class="material-symbols-outlined mr-2">logout</span>
          Kết ca
        </AppButton>
      </div>
    </section>

    <div v-if="errorMessage" class="rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-300 font-semibold flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
      <span>{{ errorMessage }}</span>
      <AppButton size="sm" variant="outline" @click="fetchShifts">Thử lại</AppButton>
    </div>

    <section class="rounded-xl border border-outline-variant/10 bg-surface-container-low overflow-hidden">
      <div class="p-5 border-b border-outline-variant/10">
        <h2 class="text-sm font-black uppercase tracking-widest text-on-surface">Lịch ca được phân</h2>
      </div>

      <div v-if="isLoading" class="p-5 space-y-3">
        <div v-for="n in 5" :key="n" class="h-20 rounded-xl bg-surface-container-high animate-pulse"></div>
      </div>

      <div v-else-if="shifts.length === 0" class="p-10 text-center">
        <div class="w-14 h-14 mx-auto rounded-2xl bg-surface-container-high flex items-center justify-center text-on-surface-variant">
          <span class="material-symbols-outlined">event_busy</span>
        </div>
        <p class="mt-4 text-sm font-bold text-on-surface">Chưa có ca trong khoảng ngày này.</p>
        <p class="mt-1 text-xs text-on-surface-variant">Khi quản lý phân và duyệt ca, lịch của bạn sẽ hiển thị tại đây.</p>
      </div>

      <div v-else class="divide-y divide-outline-variant/10">
        <article v-for="shift in sortedShifts" :key="shift.id" class="p-5 flex flex-col xl:flex-row xl:items-center xl:justify-between gap-4 hover:bg-white/5 transition-colors">
          <div class="flex items-start gap-4 min-w-0">
            <div class="w-11 h-11 rounded-xl bg-surface-container-high text-primary flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined">event_available</span>
            </div>
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <h3 class="text-base font-black text-on-surface">{{ positionLabel(shift.workPosition) }}</h3>
                <span class="rounded-full border px-3 py-1 text-[10px] font-black uppercase" :class="stateClass(shift)">
                  {{ stateLabel(shift) }}
                </span>
              </div>
              <p class="text-xs font-bold text-on-surface-variant mt-2">
                {{ formatDate(shift.workDate) }} · {{ formatTime(shift.startTime) }} - {{ formatTime(shift.endTime) }}
              </p>
              <p class="text-xs text-on-surface-variant mt-1">
                {{ shift.cinemaName || 'Chưa gán cơ sở' }} · {{ shift.location || 'Theo điều phối trưởng ca' }}
              </p>
              <p v-if="shift.note" class="mt-2 text-xs text-on-surface-variant italic">{{ shift.note }}</p>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-3 xl:justify-end">
            <span class="rounded-full bg-surface-container-high px-3 py-2 text-[10px] font-black uppercase text-on-surface-variant">
              {{ statusLabel(shift.status) }}
            </span>
            <AppButton v-if="canCheckIn(shift)" size="sm" :loading="actionLoadingId === shift.id" @click="handleCheckIn(shift)">
              <span class="material-symbols-outlined mr-2">login</span>
              Vào ca
            </AppButton>
            <RouterLink v-if="runtimeState(shift) === 'ACTIVE' && primaryAction(shift)" :to="primaryAction(shift).path">
              <AppButton size="sm">
                <span class="material-symbols-outlined mr-2">{{ primaryAction(shift).icon }}</span>
                {{ primaryAction(shift).label }}
              </AppButton>
            </RouterLink>
            <RouterLink v-if="canHandover(shift)" :to="handoverRoute(shift)">
              <AppButton size="sm" variant="outline">
                <span class="material-symbols-outlined mr-2">point_of_sale</span>
                Bàn giao
              </AppButton>
            </RouterLink>
            <AppButton v-if="canCheckOut(shift)" size="sm" variant="outline" :loading="actionLoadingId === shift.id" @click="handleCheckOut(shift)">
              <span class="material-symbols-outlined mr-2">logout</span>
              Kết ca
            </AppButton>
          </div>
        </article>
      </div>
    </section>

    <section v-if="upcomingShifts.length || endedShifts.length" class="grid grid-cols-1 xl:grid-cols-2 gap-4">
      <div class="rounded-xl border border-outline-variant/10 bg-surface-container-low p-5">
        <h2 class="text-sm font-black uppercase tracking-widest text-on-surface mb-4">Sắp tới</h2>
        <div class="space-y-3">
          <p v-if="upcomingShifts.length === 0" class="text-sm text-on-surface-variant font-semibold">Không có ca sắp tới.</p>
          <div v-for="shift in upcomingShifts.slice(0, 3)" :key="shift.id" class="rounded-xl bg-surface-container-high p-4">
            <p class="text-sm font-black">{{ positionLabel(shift.workPosition) }}</p>
            <p class="text-xs text-on-surface-variant mt-1">{{ formatDate(shift.workDate) }} · {{ formatTime(shift.startTime) }} - {{ formatTime(shift.endTime) }}</p>
          </div>
        </div>
      </div>
      <div class="rounded-xl border border-outline-variant/10 bg-surface-container-low p-5">
        <h2 class="text-sm font-black uppercase tracking-widest text-on-surface mb-4">Đã qua</h2>
        <div class="space-y-3">
          <p v-if="endedShifts.length === 0" class="text-sm text-on-surface-variant font-semibold">Chưa có ca đã kết thúc trong khoảng ngày này.</p>
          <div v-for="shift in endedShifts.slice(-3).reverse()" :key="shift.id" class="rounded-xl bg-surface-container-high p-4">
            <p class="text-sm font-black">{{ positionLabel(shift.workPosition) }}</p>
            <p class="text-xs text-on-surface-variant mt-1">{{ formatDate(shift.workDate) }} · {{ formatTime(shift.startTime) }} - {{ formatTime(shift.endTime) }}</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
