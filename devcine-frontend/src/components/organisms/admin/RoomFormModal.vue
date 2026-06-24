<script setup>
import { reactive, ref, computed, watch } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },   // 'create' | 'edit'
  initial: { type: Object, default: null }      // dữ liệu phòng khi sửa
})

const emit = defineEmits(['close', 'submit'])

const ROOM_TYPES = ['2D', '3D', 'IMAX', '4DX', 'Sweetbox']

const form = reactive({ name: '', type: '2D', status: 'Active', turnaroundTimeMins: 15, matrixRow: 8, matrixCol: 10 })
const errors = reactive({})

// Nạp dữ liệu mỗi khi mở modal
watch(() => props.show, (open) => {
  if (!open) return
  Object.keys(errors).forEach(k => delete errors[k])
  if (props.mode === 'edit' && props.initial) {
    form.name = props.initial.name || ''
    form.type = props.initial.type || '2D'
    form.status = props.initial.status === 'Maintenance' ? 'Maintenance' : 'Active'
    form.turnaroundTimeMins = props.initial.turnaroundTimeMins ?? props.initial.cleaningTime ?? 15
    form.matrixRow = props.initial.rows ?? props.initial.matrixRow ?? 8
    form.matrixCol = props.initial.cols ?? props.initial.matrixCol ?? 10
  } else {
    form.name = ''; form.type = '2D'; form.status = 'Active'
    form.turnaroundTimeMins = 15; form.matrixRow = 8; form.matrixCol = 10
  }
})

const totalSeats = computed(() => (Number(form.matrixRow) || 0) * (Number(form.matrixCol) || 0))

const validateName = () => {
  const v = (form.name || '').trim().replace(/\s+/g, ' ')
  form.name = v
  if (!v) errors.name = 'Tên phòng không được để trống'
  else if (v.length < 2 || v.length > 50) errors.name = 'Tên phòng phải từ 2 đến 50 ký tự'
  else errors.name = ''
  return !errors.name
}
const validateRow = () => {
  const n = Number(form.matrixRow)
  if (!Number.isInteger(n) || n < 1 || n > 26) errors.matrixRow = 'Số hàng từ 1 đến 26 (A–Z)'
  else errors.matrixRow = ''
  return !errors.matrixRow
}
const validateCol = () => {
  const n = Number(form.matrixCol)
  if (!Number.isInteger(n) || n < 1 || n > 50) errors.matrixCol = 'Số cột từ 1 đến 50'
  else errors.matrixCol = ''
  return !errors.matrixCol
}
const validateTurnaround = () => {
  const n = Number(form.turnaroundTimeMins)
  if (!Number.isInteger(n) || n < 0 || n > 120) errors.turnaroundTimeMins = 'Thời gian dọn từ 0 đến 120 phút'
  else errors.turnaroundTimeMins = ''
  return !errors.turnaroundTimeMins
}

const validateAll = () => [validateName(), validateRow(), validateCol(), validateTurnaround()].every(Boolean)

const handleSubmit = () => {
  if (!validateAll()) return
  emit('submit', {
    name: form.name,
    type: form.type,
    status: form.status,
    turnaroundTimeMins: Number(form.turnaroundTimeMins),
    matrixRow: Number(form.matrixRow),
    matrixCol: Number(form.matrixCol)
  })
}
</script>

<template>
  <div v-if="show" class="fixed inset-0 z-[120] flex items-center justify-center bg-black/60 backdrop-blur-sm animate-in fade-in duration-300">
    <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col">
      <!-- Header -->
      <div class="px-8 py-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-high/30">
        <h2 class="text-lg font-black uppercase tracking-widest text-primary flex items-center gap-3">
          <span class="material-symbols-outlined">tv_gen</span>
          {{ mode === 'edit' ? 'Sửa Phòng Chiếu' : 'Thêm Phòng Chiếu' }}
        </h2>
        <button @click="$emit('close')" class="text-on-surface-variant hover:text-white transition-colors">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <!-- Body -->
      <div class="p-8 space-y-5">
        <!-- Tên phòng -->
        <div class="space-y-1.5">
          <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Tên phòng <span class="text-red-500">*</span></label>
          <input v-model="form.name" @blur="validateName" type="text" placeholder="VD: Phòng 01"
            :class="errors.name ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
            class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all placeholder-white/20">
          <p v-if="errors.name" class="text-red-400 text-xs">{{ errors.name }}</p>
        </div>

        <div class="grid grid-cols-2 gap-5">
          <!-- Loại phòng -->
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Loại phòng</label>
            <select v-model="form.type" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all appearance-none cursor-pointer">
              <option v-for="t in ROOM_TYPES" :key="t" :value="t" class="bg-surface-container-high text-white">{{ t }}</option>
            </select>
          </div>
          <!-- Trạng thái -->
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Trạng thái</label>
            <select v-model="form.status" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all appearance-none cursor-pointer">
              <option value="Active" class="bg-surface-container-high">Hoạt động</option>
              <option value="Maintenance" class="bg-surface-container-high">Bảo trì</option>
            </select>
          </div>

          <!-- Số hàng -->
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Số hàng ghế (A–Z) <span class="text-red-500">*</span></label>
            <input v-model.number="form.matrixRow" @blur="validateRow" type="number" min="1" max="26"
              :class="errors.matrixRow ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all">
            <p v-if="errors.matrixRow" class="text-red-400 text-xs">{{ errors.matrixRow }}</p>
          </div>
          <!-- Số cột -->
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Số cột ghế <span class="text-red-500">*</span></label>
            <input v-model.number="form.matrixCol" @blur="validateCol" type="number" min="1" max="50"
              :class="errors.matrixCol ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all">
            <p v-if="errors.matrixCol" class="text-red-400 text-xs">{{ errors.matrixCol }}</p>
          </div>

          <!-- Thời gian dọn -->
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Thời gian dọn (phút)</label>
            <input v-model.number="form.turnaroundTimeMins" @blur="validateTurnaround" type="number" min="0" max="120"
              :class="errors.turnaroundTimeMins ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all">
            <p v-if="errors.turnaroundTimeMins" class="text-red-400 text-xs">{{ errors.turnaroundTimeMins }}</p>
          </div>
          <!-- Tổng ghế -->
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Tổng số ghế</label>
            <div class="w-full bg-black/10 border border-white/5 rounded-xl px-4 py-3 text-sm font-black text-primary">{{ totalSeats }} ghế</div>
          </div>
        </div>

        <p class="text-[11px] text-on-surface-variant/70 leading-relaxed flex items-start gap-2">
          <span class="material-symbols-outlined text-sm text-primary mt-0.5">info</span>
          Hệ thống tự sinh lưới ghế thường theo kích thước trên. Bạn có thể tuỳ chỉnh loại ghế chi tiết sau ở mục "Sơ đồ ghế".
          <template v-if="mode === 'edit'"> Đổi kích thước sẽ tạo lại ghế (chỉ khi phòng chưa có suất chiếu).</template>
        </p>
      </div>

      <!-- Footer -->
      <div class="px-8 py-6 border-t border-outline-variant/10 bg-surface-container-high/10 flex justify-end gap-4">
        <button @click="$emit('close')" class="px-6 py-3 rounded-xl border border-white/10 text-on-surface-variant text-[10px] font-black uppercase tracking-widest hover:bg-white/5 transition-all">
          Hủy bỏ
        </button>
        <button @click="handleSubmit" class="px-8 py-3 rounded-xl bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest hover:brightness-110 transition-all shadow-lg shadow-primary/20">
          {{ mode === 'edit' ? 'Lưu thay đổi' : 'Tạo phòng' }}
        </button>
      </div>
    </div>
  </div>
</template>
