<script setup>
import { computed, onMounted, ref } from 'vue'
import AppButton from '@/components/common/AppButton.vue'
import AppModal from '@/components/common/AppModal.vue'
import { approvalApi } from '@/api/admin/index'
import { useAuthStore } from '@/stores/auth'
import { useConfirmStore } from '@/stores/confirm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const auth = useAuthStore()
const confirm = useConfirmStore()
const toast = useToastStore()

const activeTab = ref('pending') // 'pending' | 'mine'
const isLoading = ref(false)
const loadError = ref('')
const pending = ref([])
const mine = ref([])
const busyId = ref(null)

// Modal từ chối (kèm lý do)
const rejectTarget = ref(null)
const rejectNote = ref('')

// Chỉ Quản lý (MANAGER) hoặc Quản trị viên (ADMIN) mới được phê duyệt.
const canApprove = computed(() => auth.isAdmin || auth.isManager)

const typeLabel = (t) => ({ FNB_VOID: 'Hủy hóa đơn F&B' }[t] || t)
const typeClass = (t) => ({
  FNB_VOID: 'bg-red-500/10 text-red-300',
}[t] || 'bg-surface-container-high text-on-surface-variant')
const statusLabel = (s) => ({ PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối' }[s] || s)
const statusClass = (s) => ({
  PENDING: 'bg-primary/10 text-primary',
  APPROVED: 'bg-green-500/10 text-green-400',
  REJECTED: 'bg-red-500/10 text-red-300',
}[s] || 'bg-surface-container-high text-on-surface-variant')
const datetime = (v) => v ? new Date(v).toLocaleString('vi-VN') : '-'

const currentList = computed(() => activeTab.value === 'pending' ? pending.value : mine.value)

const loadData = async ({ silent = false } = {}) => {
  if (!silent) isLoading.value = true
  loadError.value = ''
  try {
    if (activeTab.value === 'pending') {
      const { data } = await approvalApi.pending()
      pending.value = data?.data ?? data ?? []
    } else {
      const { data } = await approvalApi.mine()
      mine.value = data?.data ?? data ?? []
    }
  } catch (err) {
    loadError.value = friendlyError(err, 'Không tải được danh sách yêu cầu.')
  } finally {
    isLoading.value = false
  }
}

const switchTab = (tab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  loadData()
}

const handleApprove = async (item) => {
  const ok = await confirm.show({
    title: 'Duyệt yêu cầu',
    message: `Xác nhận ${typeLabel(item.type).toLowerCase()}: ${item.summary}?`,
    confirmText: 'Duyệt',
  })
  if (!ok) return
  busyId.value = item.id
  try {
    await approvalApi.approve(item.id)
    // Cập nhật tại chỗ: bản ghi biến mất khỏi hàng chờ ngay, rồi đồng bộ lại im lặng.
    pending.value = pending.value.filter((x) => x.id !== item.id)
    toast.success('Đã duyệt yêu cầu.')
    await loadData({ silent: true })
  } catch (err) {
    toast.error(friendlyError(err, 'Duyệt thất bại.'))
  } finally {
    busyId.value = null
  }
}

const openReject = (item) => {
  rejectTarget.value = item
  rejectNote.value = ''
}

const handleReject = async () => {
  if (!rejectTarget.value) return
  const rejectedId = rejectTarget.value.id
  busyId.value = rejectedId
  try {
    await approvalApi.reject(rejectedId, rejectNote.value?.trim() || null)
    // Cập nhật tại chỗ: bản ghi biến mất khỏi hàng chờ ngay, rồi đồng bộ lại im lặng.
    pending.value = pending.value.filter((x) => x.id !== rejectedId)
    toast.success('Đã từ chối yêu cầu.')
    rejectTarget.value = null
    await loadData({ silent: true })
  } catch (err) {
    toast.error(friendlyError(err, 'Từ chối thất bại.'))
  } finally {
    busyId.value = null
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="space-y-6">
    <header class="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
      <div>
        <h1 class="text-2xl font-black text-on-surface">Phê duyệt sửa sai</h1>
        <p class="text-sm text-on-surface-variant mt-1">
          Quản lý duyệt yêu cầu hủy hóa đơn F&B bấm nhầm (hoàn điểm cho thành viên).
        </p>
      </div>
      <div class="flex gap-2 bg-surface-container rounded-xl p-1">
        <button
          class="px-4 py-2 rounded-lg text-sm font-bold transition-colors"
          :class="activeTab === 'pending' ? 'bg-primary text-on-primary' : 'text-on-surface-variant'"
          @click="switchTab('pending')"
        >Chờ duyệt</button>
        <button
          class="px-4 py-2 rounded-lg text-sm font-bold transition-colors"
          :class="activeTab === 'mine' ? 'bg-primary text-on-primary' : 'text-on-surface-variant'"
          @click="switchTab('mine')"
        >Yêu cầu của tôi</button>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="isLoading" class="space-y-3">
      <div v-for="n in 3" :key="n" class="h-20 rounded-xl bg-surface-container animate-pulse"></div>
    </div>

    <!-- Error -->
    <div v-else-if="loadError" class="rounded-xl bg-red-500/10 text-red-300 p-6 text-center">
      <p class="font-bold">{{ loadError }}</p>
      <AppButton class="mt-3" variant="outline" @click="loadData">Thử lại</AppButton>
    </div>

    <!-- Empty -->
    <div v-else-if="!currentList.length" class="rounded-xl bg-surface-container p-10 text-center">
      <span class="material-symbols-outlined text-5xl text-on-surface-variant/50">task_alt</span>
      <p class="mt-2 text-on-surface-variant font-medium">
        {{ activeTab === 'pending' ? 'Không có yêu cầu nào đang chờ duyệt.' : 'Bạn chưa tạo yêu cầu nào.' }}
      </p>
    </div>

    <!-- Success -->
    <div v-else class="space-y-3">
      <article
        v-for="item in currentList"
        :key="item.id"
        class="rounded-xl bg-surface-container p-4 md:p-5 flex flex-col md:flex-row md:items-center gap-4"
      >
        <div class="flex-1 min-w-0">
          <div class="flex flex-wrap items-center gap-2">
            <span class="px-2.5 py-1 rounded-full text-[11px] font-black" :class="typeClass(item.type)">{{ typeLabel(item.type) }}</span>
            <span class="px-2.5 py-1 rounded-full text-[11px] font-black" :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
            <span class="text-xs text-on-surface-variant font-mono">{{ item.refCode }}</span>
          </div>
          <p class="mt-2 font-bold text-on-surface">{{ item.summary }}</p>
          <p v-if="item.reason" class="text-sm text-on-surface-variant mt-0.5">Lý do: {{ item.reason }}</p>
          <p class="text-[11px] text-on-surface-variant/70 mt-1">
            {{ item.requestedByName || 'N/A' }} · {{ datetime(item.createdAt) }}
            <span v-if="item.approvedByName"> · Xử lý bởi {{ item.approvedByName }}</span>
          </p>
          <p v-if="item.decisionNote" class="text-xs text-red-300 mt-0.5">Ghi chú: {{ item.decisionNote }}</p>
        </div>

        <div v-if="activeTab === 'pending' && item.status === 'PENDING' && canApprove" class="flex gap-2 shrink-0">
          <AppButton :loading="busyId === item.id" @click="handleApprove(item)">Duyệt</AppButton>
          <AppButton variant="outline" :disabled="busyId === item.id" @click="openReject(item)">Từ chối</AppButton>
        </div>
        <div v-else-if="activeTab === 'pending' && item.status === 'PENDING' && !canApprove" class="text-xs text-on-surface-variant italic shrink-0">
          Cần Quản lý duyệt
        </div>
      </article>
    </div>

    <!-- Modal từ chối -->
    <AppModal :show="!!rejectTarget" title="Từ chối yêu cầu" @close="rejectTarget = null">
      <div class="space-y-3">
        <p class="text-sm text-on-surface-variant">{{ rejectTarget?.summary }}</p>
        <label class="block space-y-1.5">
          <span class="text-[11px] uppercase tracking-widest font-black text-on-surface-variant">Lý do từ chối (tùy chọn)</span>
          <textarea v-model="rejectNote" rows="3" class="w-full bg-surface-container-high rounded-xl px-4 py-3 text-sm outline-none" placeholder="VD: Hóa đơn đã giao hàng cho khách..."></textarea>
        </label>
        <div class="flex justify-end gap-2 pt-2">
          <AppButton variant="outline" @click="rejectTarget = null">Hủy</AppButton>
          <AppButton :loading="busyId === rejectTarget?.id" @click="handleReject">Xác nhận từ chối</AppButton>
        </div>
      </div>
    </AppModal>
  </div>
</template>
