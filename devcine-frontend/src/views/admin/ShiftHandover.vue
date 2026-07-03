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
const receiverError = ref('')
const summary = ref(null)
const handovers = ref([])
const receivers = ref([])
const declaredCash = ref('')
const receiverStaffId = ref('')
const note = ref('')
const receiverNote = ref('')

const canManage = computed(() => auth.isAdmin || auth.hasPermission('staff_management', 'edit'))
const canViewList = computed(() => auth.isAdmin || auth.hasPermission('staff_management', 'view'))
const hasSummary = computed(() => !!summary.value)
const selectedScheduleId = computed(() => route.query.scheduleId ? Number(route.query.scheduleId) : null)
const currentUserId = computed(() => auth.user?.id || auth.user?.userId || null)
const canSubmitHandover = computed(() => !receiverError.value && receivers.value.length > 0)

const money = (value) => new Intl.NumberFormat('vi-VN').format(Number(value || 0)) + 'đ'
const datetime = (value) => value ? new Date(value).toLocaleString('vi-VN') : '-'
const dateOnly = (value) => value ? new Date(value).toLocaleDateString('vi-VN') : '-'
const positionLabel = (value) => shiftStore.positionLabel(value)
const statusLabel = (value) => ({
  SUBMITTED: 'Đã gửi',
  RECEIVED: 'Đã nhận',
  CONFIRMED: 'Đã chốt',
  REJECTED: 'Cần kiểm tra',
}[value] || value)
const statusClass = (value) => ({
  SUBMITTED: 'bg-primary/10 text-primary',
  RECEIVED: 'bg-blue-500/10 text-blue-300',
  CONFIRMED: 'bg-green-500/10 text-green-400',
  REJECTED: 'bg-red-500/10 text-red-300',
}[value] || 'bg-surface-container-high text-on-surface-variant')
const canReceive = (handover) => auth.isStaff
  && handover.status === 'SUBMITTED'
  && Number(handover.receivedByStaffId) === Number(currentUserId.value)

const loadData = async () => {
  isLoading.value = true
  errorMessage.value = ''
  receiverError.value = ''
  try {
    await shiftStore.fetchCurrent(true)
    const summaryRequest = selectedScheduleId.value
      ? shiftHandoverApi.summary(selectedScheduleId.value).then((response) => response.data?.data ?? response.data).catch(() => null)
      : shiftStore.fetchCurrentSummary().catch(() => null)
    const listRequest = canViewList.value ? shiftHandoverApi.list() : (auth.isStaff ? shiftHandoverApi.mine() : Promise.resolve(null))
    const [summaryData, listRes] = await Promise.all([summaryRequest, listRequest])

    summary.value = summaryData
    declaredCash.value = summaryData ? String(Number(summaryData.systemCash || 0)) : ''
    receiverStaffId.value = ''
    receivers.value = []
    if (summaryData) {
      try {
        const receiverRes = await shiftHandoverApi.receivers(summaryData.staffScheduleId)
        receivers.value = receiverRes.data?.data ?? receiverRes.data ?? []
      } catch (error) {
        receiverError.value = friendlyError(error, 'Khong tai duoc danh sach nhan vien nhan ban giao.')
      }
    }
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
  if (!receiverStaffId.value) {
    toast.warning(receiverError.value || 'Vui lòng chọn nhân viên nhận bàn giao.')
    return
  }
  const ok = await confirm.show({
    title: 'Gửi bàn giao ca',
    message: 'Sau khi gửi, nhân viên ca sau sẽ xác nhận đã nhận bàn giao.',
    confirmText: 'Gửi bàn giao',
    tone: 'primary',
  })
  if (!ok) return

  isSubmitting.value = true
  try {
    await shiftHandoverApi.submit({
      staffScheduleId: summary.value.staffScheduleId,
      receiverStaffId: Number(receiverStaffId.value),
      declaredCash: amount,
      note: note.value || null,
    })
    toast.success('Đã gửi bàn giao ca.')
    note.value = ''
    receiverStaffId.value = ''
    await loadData()
  } catch (error) {
    toast.error(friendlyError(error, 'Không gửi được bàn giao ca.'))
  } finally {
    isSubmitting.value = false
  }
}

const handleReceive = async (handover) => {
  const ok = await confirm.show({
    title: 'Nhận bàn giao',
    message: `Xác nhận bạn đã nhận bàn giao từ ${handover.staffName}?`,
    confirmText: 'Nhận bàn giao',
    tone: 'primary',
  })
  if (!ok) return

  try {
    await shiftHandoverApi.receive(handover.id, { note: receiverNote.value || null })
    receiverNote.value = ''
    toast.success('Đã nhận bàn giao.')
    await loadData()
  } catch (error) {
    toast.error(friendlyError(error, 'Không nhận được bàn giao ca.'))
  }
}

const handleDecision = async (handover, action) => {
  const ok = await confirm.show({
    title: action === 'confirm' ? 'Chốt đối soát' : 'Yêu cầu kiểm tra lại',
    message: `Biên bản #${handover.id} sẽ được cập nhật trạng thái.`,
    confirmText: action === 'confirm' ? 'Chốt đối soát' : 'Từ chối',
    tone: action === 'confirm' ? 'primary' : 'danger',
  })
  if (!ok) return

  try {
    if (action === 'confirm') await shiftHandoverApi.confirm(handover.id)
    else await shiftHandoverApi.reject(handover.id)
    toast.success(action === 'confirm' ? 'Đã chốt đối soát.' : 'Đã yêu cầu kiểm tra lại.')
    await loadData()
  } catch (error) {
    toast.error(friendlyError(error, 'Không cập nhật được bàn giao ca.'))
  }
}

onMounted(loadData)
</script>

<template>
  <div class="p-8 space-y-6">
    <header class="flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="text-3xl font-black tracking-tight text-on-surface">Bàn giao ca</h1>
        <p class="text-sm text-on-surface-variant">Bàn giao thực tế giữa nhân viên, sau đó quản lý chốt đối soát.</p>
      </div>
      <AppButton variant="outline" :loading="isLoading" @click="loadData">
        <span class="material-symbols-outlined mr-2">refresh</span>Làm mới
      </AppButton>
    </header>

    <div v-if="isLoading" class="grid gap-4 md:grid-cols-4">
      <div v-for="i in 4" :key="i" class="h-28 rounded-lg bg-surface-container-high animate-pulse"></div>
    </div>

    <div v-else-if="errorMessage" class="rounded-lg border border-red-500/20 bg-red-500/10 p-5 text-sm font-semibold text-red-300">
      {{ errorMessage }}
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
          <p class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Vé / đơn F&B</p>
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
            <p><span class="font-bold text-on-surface">Doanh thu F&B:</span> {{ money(summary.concessionRevenue) }}</p>
          </div>
        </div>

        <form class="rounded-lg border border-outline-variant/10 bg-surface p-6 space-y-4" @submit.prevent="handleSubmit">
          <h2 class="text-lg font-black text-on-surface">Gửi bàn giao</h2>
          <label class="block">
            <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Người nhận ca sau</span>
            <select v-model="receiverStaffId" :disabled="receiverError || receivers.length === 0" class="mt-2 w-full rounded-lg border border-outline-variant/20 bg-surface-container-high px-4 py-3 text-on-surface outline-none focus:border-primary disabled:opacity-60">
              <option value="">Chọn nhân viên nhận bàn giao</option>
              <option v-for="staff in receivers" :key="staff.userId" :value="staff.userId">
                {{ staff.fullName }} - {{ staff.staffCode }}
              </option>
            </select>
            <p v-if="receiverError" class="mt-2 text-xs font-semibold text-red-300">{{ receiverError }}</p>
            <p v-else-if="receivers.length === 0" class="mt-2 text-xs font-semibold text-on-surface-variant">Chưa có nhân viên cùng cơ sở để nhận bàn giao.</p>
          </label>
          <label class="block">
            <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Tiền mặt thực tế</span>
            <input v-model="declaredCash" type="number" min="0" class="mt-2 w-full rounded-lg border border-outline-variant/20 bg-surface-container-high px-4 py-3 text-on-surface outline-none focus:border-primary">
          </label>
          <label class="block">
            <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">Ghi chú người gửi</span>
            <textarea v-model="note" rows="3" class="mt-2 w-full rounded-lg border border-outline-variant/20 bg-surface-container-high px-4 py-3 text-on-surface outline-none focus:border-primary"></textarea>
          </label>
          <AppButton class="w-full" :loading="isSubmitting" :disabled="!canSubmitHandover">Gửi bàn giao</AppButton>
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
        <h2 class="text-lg font-black text-on-surface">Biên bản bàn giao</h2>
      </div>
      <div v-if="handovers.length === 0" class="p-8 text-center text-sm text-on-surface-variant">Chưa có biên bản bàn giao.</div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-surface-container-high text-xs uppercase tracking-widest text-on-surface-variant">
            <tr>
              <th class="px-4 py-3 text-left">Ca</th>
              <th class="px-4 py-3 text-left">Người gửi</th>
              <th class="px-4 py-3 text-left">Người nhận</th>
              <th class="px-4 py-3 text-right">Thực tế</th>
              <th class="px-4 py-3 text-right">Hệ thống</th>
              <th class="px-4 py-3 text-right">Chênh lệch</th>
              <th class="px-4 py-3 text-center">Trạng thái</th>
              <th class="px-4 py-3 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-outline-variant/10">
            <tr v-for="handover in handovers" :key="handover.id">
              <td class="px-4 py-3">{{ dateOnly(handover.workDate) }} · {{ positionLabel(handover.workPosition) }}</td>
              <td class="px-4 py-3">{{ handover.staffName }}</td>
              <td class="px-4 py-3">{{ handover.receivedByStaffName || '-' }}</td>
              <td class="px-4 py-3 text-right">{{ money(handover.declaredCash) }}</td>
              <td class="px-4 py-3 text-right">{{ money(handover.systemCash) }}</td>
              <td class="px-4 py-3 text-right" :class="Number(handover.difference || 0) === 0 ? 'text-green-400' : 'text-red-300'">{{ money(handover.difference) }}</td>
              <td class="px-4 py-3 text-center">
                <span class="rounded-full px-3 py-1 text-[10px] font-black uppercase" :class="statusClass(handover.status)">
                  {{ statusLabel(handover.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-right">
                <div class="flex justify-end gap-2">
                  <AppButton v-if="canReceive(handover)" size="sm" @click="handleReceive(handover)">Nhận bàn giao</AppButton>
                  <template v-if="canManage && ['SUBMITTED', 'RECEIVED'].includes(handover.status)">
                    <AppButton size="sm" variant="outline" @click="handleDecision(handover, 'reject')">Từ chối</AppButton>
                    <AppButton v-if="handover.status === 'RECEIVED'" size="sm" @click="handleDecision(handover, 'confirm')">Chốt đối soát</AppButton>
                  </template>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
