<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppButton from '../../components/common/AppButton.vue'
import AppModal from '../../components/common/AppModal.vue'
import { cinemaListApi, staffApi, staffShiftApi } from '@/api/admin/index'
import { useConfirmStore } from '@/stores/confirm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const toast = useToastStore()
const confirm = useConfirmStore()

const today = new Date().toISOString().slice(0, 10)
const selectedDate = ref(today)
const selectedCinemaId = ref('')
const selectedStatus = ref('ALL')
const staffList = ref([])
const cinemas = ref([])
const shifts = ref([])
const isLoading = ref(false)
const isSubmitting = ref(false)
const errorMessage = ref('')
const isAddModalOpen = ref(false)
const updatingIds = ref(new Set())

const form = reactive({
  staffId: '',
  cinemaId: '',
  workDate: today,
  startTime: '08:00',
  endTime: '16:00',
  workPosition: 'POS_TICKETING',
  location: '',
  note: ''
})

const statuses = [{ value: 'ALL', label: 'Tất cả' }, { value: 'SCHEDULED', label: 'Chờ duyệt' }, { value: 'APPROVED', label: 'Đã duyệt' }, { value: 'IN_PROGRESS', label: 'Đang trong ca' }, { value: 'COMPLETED', label: 'Đã kết ca' }, { value: 'REJECTED', label: 'Từ chối' }]
const workPositions = [{ value: 'POS_TICKETING', label: 'Bán vé POS' }, { value: 'CHECK_IN', label: 'Kiểm soát vé' }, { value: 'FNB', label: 'Quầy F&B' }, { value: 'PROJECTION', label: 'Phòng chiếu' }, { value: 'SHIFT_LEAD', label: 'Trưởng ca' }]

const normalizeList = (payload) => Array.isArray(payload) ? payload : (payload?.data ?? [])
const positionLabel = (value) => workPositions.find(p => p.value === value)?.label || value || 'Chưa gán'
const statusLabel = (value) => statuses.find(s => s.value === value)?.label || value

const activeStaff = computed(() => staffList.value.filter(s => s.isActive))
const availableStaff = computed(() => {
  const cinemaId = Number(form.cinemaId)
  return activeStaff.value.filter(s => !cinemaId || s.cinemaId === cinemaId)
})
const scheduledShifts = computed(() => shifts.value.filter(s => s.status === 'SCHEDULED'))
const approvedCount = computed(() => shifts.value.filter(s => s.status === 'APPROVED').length)
const totalHours = computed(() => shifts.value.reduce((sum, shift) => {
  if (!shift.startAt || !shift.endAt) return sum
  const diff = (new Date(shift.endAt) - new Date(shift.startAt)) / 36e5
  return sum + Math.max(diff, 0)
}, 0))

const setUpdating = (id, value) => {
  const next = new Set(updatingIds.value)
  value ? next.add(id) : next.delete(id)
  updatingIds.value = next
}

const fetchData = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const params = {
      date: selectedDate.value,
      cinemaId: selectedCinemaId.value || undefined,
      status: selectedStatus.value === 'ALL' ? undefined : selectedStatus.value
    }
    const [staffRes, shiftsRes, cinemaRes] = await Promise.all([
      staffApi.list(selectedCinemaId.value ? { cinemaId: selectedCinemaId.value } : undefined),
      staffShiftApi.list(params),
      cinemaListApi.getAll()
    ])
    staffList.value = normalizeList(staffRes.data)
    shifts.value = normalizeList(shiftsRes.data)
    cinemas.value = normalizeList(cinemaRes.data)
  } catch (error) {
    errorMessage.value = friendlyError(error, 'Không tải được dữ liệu phân ca.')
    toast.error(errorMessage.value)
  } finally {
    isLoading.value = false
  }
}

const openAddModal = () => {
  form.workDate = selectedDate.value
  form.cinemaId = selectedCinemaId.value || activeStaff.value[0]?.cinemaId || cinemas.value[0]?.id || ''
  form.staffId = availableStaff.value[0]?.userId || ''
  form.startTime = '08:00'
  form.endTime = '16:00'
  form.workPosition = 'POS_TICKETING'
  form.location = ''
  form.note = ''
  isAddModalOpen.value = true
}

const handleStaffChange = () => {
  const staff = staffList.value.find(s => s.userId === Number(form.staffId))
  if (staff?.cinemaId) form.cinemaId = staff.cinemaId
  // Tự điền vị trí theo vị trí mặc định của nhân viên (vẫn có thể đổi tay)
  if (staff?.defaultPosition) form.workPosition = staff.defaultPosition
}

const handleCinemaChange = () => {
  if (!availableStaff.value.some(staff => staff.userId === Number(form.staffId))) {
    form.staffId = availableStaff.value[0]?.userId || ''
  }
}

const validateForm = () => {
  if (!form.staffId) return 'Vui lòng chọn nhân viên.'
  if (!form.cinemaId) return 'Vui lòng chọn cơ sở.'
  if (!form.workDate || !form.startTime || !form.endTime) return 'Vui lòng nhập đủ ngày và giờ làm việc.'
  if (!form.workPosition) return 'Vui lòng chọn vị trí làm việc.'
  return ''
}

const handleAddShift = async () => {
  const message = validateForm()
  if (message) { toast.warning(message); return }
  isSubmitting.value = true
  try {
    await staffShiftApi.create({
      staffId: Number(form.staffId),
      cinemaId: Number(form.cinemaId),
      workDate: form.workDate,
      startTime: form.startTime,
      endTime: form.endTime,
      workPosition: form.workPosition,
      location: form.location || null,
      note: form.note || null
    })
    toast.success('Đã tạo ca làm việc.')
    isAddModalOpen.value = false
    await fetchData()
  } catch (error) {
    toast.error(friendlyError(error, 'Không tạo được ca làm việc.'))
  } finally {
    isSubmitting.value = false
  }
}

const handleApprove = async (id) => {
  setUpdating(id, true)
  try {
    await staffShiftApi.approve(id)
    toast.success('Đã duyệt ca làm việc.')
    await fetchData()
  } catch (error) {
    toast.error(friendlyError(error, 'Không duyệt được ca làm việc.'))
  } finally {
    setUpdating(id, false)
  }
}

const handleReject = async (shift) => {
  const ok = await confirm.show({
    title: 'Từ chối ca làm việc',
    message: `Từ chối ca của ${shift.staffName} vào ngày ${shift.workDate}?`,
    confirmText: 'Từ chối',
    tone: 'danger'
  })
  if (!ok) return
  setUpdating(shift.id, true)
  try {
    await staffShiftApi.reject(shift.id)
    toast.success('Đã từ chối ca làm việc.')
    await fetchData()
  } catch (error) {
    toast.error(friendlyError(error, 'Không từ chối được ca làm việc.'))
  } finally {
    setUpdating(shift.id, false)
  }
}

const handleApproveAll = async () => {
  const ok = await confirm.show({
    title: 'Duyệt tất cả ca chờ',
    message: `Duyệt ${scheduledShifts.value.length} ca đang chờ trong ngày ${selectedDate.value}?`,
    confirmText: 'Duyệt tất cả'
  })
  if (!ok) return
  try {
    await Promise.all(scheduledShifts.value.map(shift => staffShiftApi.approve(shift.id)))
    toast.success('Đã duyệt tất cả ca đang chờ.')
    await fetchData()
  } catch (error) {
    toast.error(friendlyError(error, 'Không duyệt được tất cả ca.'))
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="p-6 lg:p-10 space-y-6">
    <header class="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
      <div>
        <h1 class="text-2xl lg:text-3xl font-black tracking-tight text-on-surface uppercase">Phân ca làm việc</h1>
        <p class="text-xs text-on-surface-variant font-bold uppercase tracking-widest mt-1">
          Xếp ca theo ngày, giờ, cơ sở và vị trí vận hành
        </p>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-3 xl:flex gap-3">
        <input v-model="selectedDate" @change="fetchData" type="date" class="bg-surface-container-high rounded-xl px-4 py-3 text-sm font-bold outline-none text-on-surface" />
        <select v-model="selectedCinemaId" @change="fetchData" class="bg-surface-container-high rounded-xl px-4 py-3 text-sm font-bold outline-none text-on-surface">
          <option value="">Tất cả cơ sở</option>
          <option v-for="cinema in cinemas" :key="cinema.id" :value="cinema.id">{{ cinema.name }}</option>
        </select>
        <select v-model="selectedStatus" @change="fetchData" class="bg-surface-container-high rounded-xl px-4 py-3 text-sm font-bold outline-none text-on-surface">
          <option v-for="status in statuses" :key="status.value" :value="status.value">{{ status.label }}</option>
        </select>
        <AppButton @click="openAddModal"><span class="material-symbols-outlined mr-2">add_task</span>Tạo ca</AppButton>
      </div>
    </header>

    <section class="grid grid-cols-2 xl:grid-cols-4 gap-3">
      <div class="rounded-xl border border-outline-variant/10 bg-surface-container-low p-4"><p class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Tổng ca</p><p class="text-2xl font-black mt-1">{{ shifts.length }}</p></div>
      <div class="rounded-xl border border-outline-variant/10 bg-surface-container-low p-4"><p class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Chờ duyệt</p><p class="text-2xl font-black mt-1 text-primary">{{ scheduledShifts.length }}</p></div>
      <div class="rounded-xl border border-outline-variant/10 bg-surface-container-low p-4"><p class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Đã duyệt</p><p class="text-2xl font-black mt-1 text-green-500">{{ approvedCount }}</p></div>
      <div class="rounded-xl border border-outline-variant/10 bg-surface-container-low p-4"><p class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Giờ phân bổ</p><p class="text-2xl font-black mt-1">{{ totalHours.toFixed(1) }}h</p></div>
    </section>

    <section class="grid grid-cols-1 xl:grid-cols-12 gap-6">
      <div class="xl:col-span-8 rounded-xl border border-outline-variant/10 bg-surface-container-low overflow-hidden">
        <div class="p-5 border-b border-outline-variant/10 flex items-center justify-between gap-3">
          <h2 class="text-sm font-black uppercase tracking-widest text-on-surface">Lịch ca ngày {{ selectedDate }}</h2>
          <AppButton v-if="scheduledShifts.length" variant="secondary" size="sm" @click="handleApproveAll">Duyệt tất cả</AppButton>
        </div>
        <div v-if="errorMessage" class="m-5 rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-400 font-semibold">{{ errorMessage }}</div>
        <div class="overflow-x-auto">
          <table class="w-full text-left min-w-[760px]">
            <thead class="bg-surface-container-high/40 text-[10px] uppercase tracking-widest text-on-surface-variant">
              <tr><th class="px-5 py-4">Nhân viên</th><th class="px-5 py-4">Cơ sở</th><th class="px-5 py-4">Vị trí</th><th class="px-5 py-4">Thời gian</th><th class="px-5 py-4 text-center">Trạng thái</th><th class="px-5 py-4 text-right">Thao tác</th></tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
              <template v-if="isLoading">
                <tr v-for="n in 5" :key="n"><td colspan="6" class="px-5 py-4"><div class="h-9 rounded-lg bg-surface-container-high animate-pulse"></div></td></tr>
              </template>
              <template v-else>
                <tr v-for="shift in shifts" :key="shift.id" class="hover:bg-white/5 transition-colors">
                  <td class="px-5 py-4"><p class="text-sm font-black">{{ shift.staffName }}</p><p class="text-[10px] text-on-surface-variant font-bold">{{ shift.staffCode || 'Chưa có mã' }}</p></td>
                  <td class="px-5 py-4 text-xs font-bold text-on-surface-variant">{{ shift.cinemaName || 'Chưa gán' }}</td>
                  <td class="px-5 py-4"><p class="text-xs font-black">{{ positionLabel(shift.workPosition) }}</p><p class="text-[10px] text-on-surface-variant">{{ shift.location || 'Theo điều phối trưởng ca' }}</p></td>
                  <td class="px-5 py-4 text-xs font-black">{{ shift.startTime }} - {{ shift.endTime }}</td>
                  <td class="px-5 py-4 text-center"><span class="px-3 py-1 rounded-full text-[10px] font-black uppercase" :class="{ 'bg-primary/15 text-primary': shift.status === 'SCHEDULED', 'bg-green-500/10 text-green-500': shift.status === 'APPROVED', 'bg-emerald-500/15 text-emerald-400': shift.status === 'IN_PROGRESS', 'bg-surface-container-high text-on-surface-variant': shift.status === 'COMPLETED', 'bg-red-500/10 text-red-500': shift.status === 'REJECTED' }">{{ statusLabel(shift.status) }}</span></td>
                  <td class="px-5 py-4 text-right">
                    <div v-if="shift.status === 'SCHEDULED'" class="flex justify-end gap-2">
                      <button :disabled="updatingIds.has(shift.id)" @click="handleApprove(shift.id)" class="p-2 rounded-lg hover:bg-green-500/10 hover:text-green-500 disabled:opacity-50" title="Duyệt"><span class="material-symbols-outlined text-lg">check_circle</span></button>
                      <button :disabled="updatingIds.has(shift.id)" @click="handleReject(shift)" class="p-2 rounded-lg hover:bg-red-500/10 hover:text-red-500 disabled:opacity-50" title="Từ chối"><span class="material-symbols-outlined text-lg">cancel</span></button>
                    </div>
                    <span v-else class="text-[10px] text-on-surface-variant font-bold uppercase">Đã xử lý</span>
                  </td>
                </tr>
              </template>
              <tr v-if="!isLoading && shifts.length === 0"><td colspan="6" class="px-5 py-16 text-center text-sm text-on-surface-variant font-semibold">Chưa có ca làm việc nào trong bộ lọc này.</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      <aside class="xl:col-span-4 rounded-xl border border-outline-variant/10 bg-surface-container-low p-5">
        <h2 class="text-sm font-black uppercase tracking-widest text-on-surface mb-4">Nhân sự khả dụng</h2>
        <div class="space-y-3 max-h-[520px] overflow-y-auto pr-1">
          <div v-for="staff in activeStaff" :key="staff.userId" class="flex items-center justify-between rounded-xl bg-surface-container-high p-3">
            <div><p class="text-xs font-black">{{ staff.fullName }}</p><p class="text-[10px] text-on-surface-variant font-bold">{{ staff.cinemaName || 'Chưa gán cơ sở' }}</p></div>
            <span class="text-[10px] font-mono text-primary">{{ staff.staffCode }}</span>
          </div>
          <p v-if="!activeStaff.length && !isLoading" class="py-8 text-center text-xs text-on-surface-variant font-bold">Chưa có nhân viên đang hoạt động.</p>
        </div>
      </aside>
    </section>

    <AppModal :show="isAddModalOpen" title="Tạo ca làm việc" @close="isAddModalOpen = false">
      <div class="space-y-5">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <label class="space-y-2"><span class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Cơ sở</span><select v-model="form.cinemaId" @change="handleCinemaChange" class="w-full bg-surface-container-high rounded-xl px-4 py-3 text-sm outline-none"><option value="">Chọn cơ sở</option><option v-for="cinema in cinemas" :key="cinema.id" :value="cinema.id">{{ cinema.name }}</option></select></label>
          <label class="space-y-2"><span class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Nhân viên</span><select v-model="form.staffId" @change="handleStaffChange" class="w-full bg-surface-container-high rounded-xl px-4 py-3 text-sm outline-none"><option value="">Chọn nhân viên</option><option v-for="staff in availableStaff" :key="staff.userId" :value="staff.userId">{{ staff.fullName }} - {{ staff.staffCode }}</option></select></label>
          <label class="space-y-2"><span class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Ngày</span><input v-model="form.workDate" type="date" class="w-full bg-surface-container-high rounded-xl px-4 py-3 text-sm outline-none" /></label>
          <label class="space-y-2"><span class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Vị trí</span><select v-model="form.workPosition" class="w-full bg-surface-container-high rounded-xl px-4 py-3 text-sm outline-none"><option v-for="position in workPositions" :key="position.value" :value="position.value">{{ position.label }}</option></select></label>
          <label class="space-y-2"><span class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Bắt đầu</span><input v-model="form.startTime" type="time" class="w-full bg-surface-container-high rounded-xl px-4 py-3 text-sm outline-none" /></label>
          <label class="space-y-2"><span class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Kết thúc</span><input v-model="form.endTime" type="time" class="w-full bg-surface-container-high rounded-xl px-4 py-3 text-sm outline-none" /></label>
        </div>
        <label class="space-y-2 block"><span class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Điểm làm việc</span><input v-model.trim="form.location" placeholder="Quầy vé 01, cổng soát vé, quầy bắp nước..." class="w-full bg-surface-container-high rounded-xl px-4 py-3 text-sm outline-none" /></label>
        <label class="space-y-2 block"><span class="text-[10px] uppercase tracking-widest text-on-surface-variant font-black">Ghi chú</span><textarea v-model.trim="form.note" rows="2" class="w-full bg-surface-container-high rounded-xl px-4 py-3 text-sm outline-none resize-none"></textarea></label>
        <div class="flex flex-col sm:flex-row gap-3 pt-2"><AppButton variant="ghost" class="flex-1" @click="isAddModalOpen = false">Hủy</AppButton><AppButton class="flex-1" :loading="isSubmitting" @click="handleAddShift">Lưu ca</AppButton></div>
      </div>
    </AppModal>
  </div>
</template>
