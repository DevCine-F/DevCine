<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppButton from '@/components/common/AppButton.vue'
import { shiftHandoverApi } from '@/api/admin/index'
import { useAuthStore } from '@/stores/auth'
import { useConfirmStore } from '@/stores/confirm'
import { useShiftStore } from '@/stores/shift'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const auth = useAuthStore()
const confirm = useConfirmStore()
const route = useRoute()
const shiftStore = useShiftStore()
const toast = useToastStore()

const isLoading = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')
const summary = ref(null)
const handovers = ref([])
const declaredCash = ref('')
const note = ref('')

const canViewList = computed(() => auth.isAdmin || auth.hasPermission('staff_management', 'view'))
const hasSummary = computed(() => !!summary.value)
const selectedScheduleId = computed(() => route.query.scheduleId ? Number(route.query.scheduleId) : null)
// Biên bản của đúng ca đang xem — đã bàn giao thì ẩn form và báo POS đã khóa.
const activeHandoverForSummary = computed(() => {
  if (!summary.value) return null
  return handovers.value.find((h) => Number(h.staffScheduleId) === Number(summary.value.staffScheduleId)) || null
})
const canSubmitHandover = computed(() => !activeHandoverForSummary.value)

const money = (value) => new Intl.NumberFormat('vi-VN').format(Number(value || 0)) + 'đ'
const datetime = (value) => value ? new Date(value).toLocaleString('vi-VN') : '-'
const dateOnly = (value) => value ? new Date(value).toLocaleDateString('vi-VN') : '-'
const timeShort = (value) => value ? new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '—'
const positionLabel = (value) => shiftStore.positionLabel(value)
const isMismatch = (handover) => Number(handover.difference || 0) !== 0
// Kỳ vọng = quỹ đầu ca + doanh thu tiền mặt hệ thống.
const expectedCash = (handover) => Number(handover.openingFloat || 0) + Number(handover.systemCash || 0)
const statusLabel = (value) => ({
  COMPLETED: 'Đã hoàn tất',
  SUBMITTED: 'Đã gửi',
  RECEIVED: 'Đã nhận',
  CONFIRMED: 'Đã chốt',
  REJECTED: 'Cần kiểm tra',
}[value] || value)
const statusClass = (value) => ({
  COMPLETED: 'bg-green-500/10 text-green-400',
  SUBMITTED: 'bg-primary/10 text-primary',
  RECEIVED: 'bg-blue-500/10 text-blue-300',
  CONFIRMED: 'bg-green-500/10 text-green-400',
  REJECTED: 'bg-red-500/10 text-red-300',
}[value] || 'bg-surface-container-high text-on-surface-variant')

const loadData = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    await shiftStore.fetchCurrent(true)
    const summaryRequest = selectedScheduleId.value
      ? shiftHandoverApi.summary(selectedScheduleId.value).then((response) => response.data?.data ?? response.data).catch(() => null)
      : shiftStore.fetchCurrentSummary().catch(() => null)
    const listRequest = canViewList.value ? shiftHandoverApi.list() : (auth.isStaff ? shiftHandoverApi.mine() : Promise.resolve(null))
    const [summaryData, listRes] = await Promise.all([summaryRequest, listRequest])

    summary.value = summaryData
    declaredCash.value = summaryData ? String(Number(summaryData.systemCash || 0)) : ''
    if (listRes) handovers.value = listRes.data?.data ?? listRes.data ?? []
  } catch (error) {
    errorMessage.value = friendlyError(error, 'Không tải được dữ liệu bàn giao ca.')
    toast.error(errorMessage.value)
  } finally {
    isLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!summary.value) return
  const amount = Number(declaredCash.value || 0)
  if (amount < 0) {
    toast.warning('Tiền mặt thực tế không được âm.')
    return
  }
  const ok = await confirm.show({
    title: 'Gửi bàn giao ca',
    message: 'Sau khi gửi, ca sẽ kết thúc và biên bản đối soát được lưu lại. Bạn không thể bàn giao lại ca này.',
    confirmText: 'Gửi bàn giao',
    tone: 'primary',
  })
  if (!ok) return

  isSubmitting.value = true
  try {
    await shiftHandoverApi.submit({
      staffScheduleId: summary.value.staffScheduleId,
      declaredCash: amount,
      note: note.value || null,
    })
    toast.success('Đã bàn giao ca và chốt đối soát.')
    note.value = ''
    await loadData()
  } catch (error) {
    toast.error(friendlyError(error, 'Không gửi được bàn giao ca.'))
  } finally {
    isSubmitting.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="p-8 space-y-6">
    <header class="flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="text-3xl font-black tracking-tight text-on-surface">Bàn giao ca</h1>
        <p class="text-sm text-on-surface-variant">Nhân viên nhập tiền mặt cuối ca, hệ thống tự chốt đối soát. Quản lý xem lại &amp; theo dõi chênh lệch.</p>
      </div>
      <AppButton variant="outline" :loading="isLoading" @click="loadData">
        <span class="material-symbols-outlined mr-2">refresh</span>Làm mới
      </AppButton>
    </header>

    <div v-if="isLoading" class="grid gap-4 md:grid-cols-4">
      <div v-for="i in 4" :key="i" class="h-28 rounded-lg bg-surface-container-high animate-pulse"></div>
    </div>

    <div v-else-if="errorMessage" class="rounded-lg border border-red-500/20 bg-red-500/10 p-5 text-sm font-semibold text-red-300 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
      <span>{{ errorMessage }}</span>
      <AppButton size="sm" variant="outline" @click="loadData">Thử lại</AppButton>
    </div>

    <section v-else-if="hasSummary" class="space-y-5">
      <div class="grid gap-4 md:grid-cols-4">
        <div class="rounded-lg border border-outline-variant/10 bg-surface p-5">
          <p class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tiền mặt hệ thống</p>
          <p class="mt-2 text-2xl font-black text-on-surface">{{ money(summary.systemCash) }}</p>
        </div>
        <div class="rounded-lg border border-outline-variant/10 bg-surface p-5">
          <p class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Thẻ</p>
          <p class="mt-2 text-2xl font-black text-on-surface">{{ money(summary.cardSales) }}</p>
        </div>
        <div class="rounded-lg border border-outline-variant/10 bg-surface p-5">
          <p class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Chuyển khoản</p>
          <p class="mt-2 text-2xl font-black text-on-surface">{{ money(summary.transferSales) }}</p>
        </div>
        <div class="rounded-lg border border-outline-variant/10 bg-surface p-5">
          <p class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Vé / đơn F&amp;B</p>
          <p class="mt-2 text-2xl font-black text-on-surface">{{ summary.ticketCount }} / {{ summary.concessionOrderCount }}</p>
        </div>
      </div>

      <div class="grid gap-5 lg:grid-cols-[1.2fr_0.8fr]">
        <div class="rounded-lg border border-outline-variant/10 bg-surface p-6">
          <h2 class="text-lg font-black text-on-surface">Ca bàn giao</h2>
          <div class="mt-4 grid gap-3 text-sm text-on-surface-variant md:grid-cols-2">
            <p><span class="font-bold text-on-surface">Nhân viên:</span> {{ summary.staffName }}</p>
            <p><span class="font-bold text-on-surface">Vị trí:</span> {{ positionLabel(summary.workPosition) }}</p>
            <p><span class="font-bold text-on-surface">Ngày:</span> {{ dateOnly(summary.workDate) }}</p>
            <p><span class="font-bold text-on-surface">Giờ:</span> {{ datetime(summary.startAt) }} - {{ datetime(summary.endAt) }}</p>
            <p><span class="font-bold text-on-surface">Rạp:</span> {{ summary.cinemaName || '-' }}</p>
            <p><span class="font-bold text-on-surface">Doanh thu vé:</span> {{ money(summary.ticketRevenue) }}</p>
            <p><span class="font-bold text-on-surface">Doanh thu F&amp;B:</span> {{ money(summary.concessionRevenue) }}</p>
          </div>
        </div>

        <div v-if="activeHandoverForSummary" class="rounded-lg border border-green-500/20 bg-green-500/10 p-6 flex flex-col items-center justify-center text-center">
          <span class="material-symbols-outlined text-4xl text-green-400">lock</span>
          <p class="mt-3 text-base font-black text-on-surface">Đã bàn giao ca</p>
          <p class="mt-1 text-sm text-on-surface-variant">POS bán hàng đã khóa, biên bản đối soát đã được lưu.</p>
        </div>

        <form v-else class="rounded-lg border border-outline-variant/10 bg-surface p-6 space-y-4" @submit.prevent="handleSubmit">
          <h2 class="text-lg font-black text-on-surface">Bàn giao cuối ca</h2>
          <label class="block">
            <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Tiền mặt thực tế trong két</span>
            <input v-model="declaredCash" type="number" min="0" class="mt-2 w-full rounded-lg border border-outline-variant/20 bg-surface-container-high px-4 py-3 text-on-surface outline-none focus:border-primary">
          </label>
          <label class="block">
            <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Ghi chú (tuỳ chọn)</span>
            <textarea v-model="note" rows="3" class="mt-2 w-full rounded-lg border border-outline-variant/20 bg-surface-container-high px-4 py-3 text-on-surface outline-none focus:border-primary"></textarea>
          </label>
          <AppButton class="w-full" :loading="isSubmitting" :disabled="!canSubmitHandover">Gửi bàn giao &amp; chốt ca</AppButton>
        </form>
      </div>
    </section>

    <section v-else class="rounded-lg border border-dashed border-outline-variant/20 bg-surface p-10 text-center">
      <span class="material-symbols-outlined text-5xl text-on-surface-variant/50">lock_clock</span>
      <p class="mt-3 text-lg font-black text-on-surface">Chưa chọn được ca bàn giao</p>
      <p class="mt-1 text-sm text-on-surface-variant">Hãy mở từ Ca của tôi hoặc refresh khi đang ở 30 phút cuối ca.</p>
    </section>

    <section class="rounded-lg border border-outline-variant/10 bg-surface overflow-hidden">
      <div class="border-b border-outline-variant/10 p-5">
        <h2 class="text-lg font-black text-on-surface">Lịch sử bàn giao ca</h2>
        <p class="text-xs text-on-surface-variant mt-1">Dòng có chênh lệch tiền được tô đỏ để kiểm tra lại.</p>
      </div>
      <div v-if="handovers.length === 0" class="p-8 text-center text-sm text-on-surface-variant">Chưa có biên bản bàn giao.</div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-surface-container-high text-xs uppercase tracking-widest text-on-surface-variant">
            <tr>
              <th class="px-4 py-3 text-left">Ca</th>
              <th class="px-4 py-3 text-left">Người gửi</th>
              <th class="px-4 py-3 text-left">Vào–Ra</th>
              <th class="px-4 py-3 text-right">Quỹ đầu ca</th>
              <th class="px-4 py-3 text-right">DT tiền mặt</th>
              <th class="px-4 py-3 text-right">Thực đếm</th>
              <th class="px-4 py-3 text-right">Chênh lệch</th>
              <th class="px-4 py-3 text-center">Trạng thái</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/10">
            <tr v-for="handover in handovers" :key="handover.id" :class="isMismatch(handover) ? 'bg-red-500/5' : ''">
              <td class="px-4 py-3">{{ dateOnly(handover.workDate) }} · {{ positionLabel(handover.workPosition) }}</td>
              <td class="px-4 py-3">{{ handover.staffName }}</td>
              <td class="px-4 py-3 whitespace-nowrap text-on-surface-variant">{{ timeShort(handover.actualCheckInAt) }} – {{ timeShort(handover.actualCheckOutAt) }}</td>
              <td class="px-4 py-3 text-right">{{ money(handover.openingFloat) }}</td>
              <td class="px-4 py-3 text-right">{{ money(handover.systemCash) }}</td>
              <td class="px-4 py-3 text-right">{{ money(handover.declaredCash) }}</td>
              <td class="px-4 py-3 text-right font-bold" :class="isMismatch(handover) ? 'text-red-300' : 'text-green-400'">
                <span v-if="isMismatch(handover)" class="material-symbols-outlined align-middle text-sm mr-0.5">warning</span>{{ money(handover.difference) }}
              </td>
              <td class="px-4 py-3 text-center">
                <span class="rounded-full px-3 py-1 text-[10px] font-black uppercase" :class="statusClass(handover.status)">
                  {{ statusLabel(handover.status) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
