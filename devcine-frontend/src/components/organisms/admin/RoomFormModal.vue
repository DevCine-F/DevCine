<script setup>
import { reactive, ref, computed, watch } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  mode: { type: String, default: 'create' },   // 'create' | 'edit'
  initial: { type: Object, default: null },      // dữ liệu phòng khi sửa
  cinema: { type: Object, default: null }        // cụm rạp để check danh sách phòng
})

const emit = defineEmits(['close', 'submit'])

// Bắt mã hạng phòng chuẩn Lotte (đồng bộ ALLOWED_TYPES ở RoomService backend)
const ROOM_TYPES = [
  { value: 'STANDARD', label: 'Standard' },
  { value: 'SUPERPLEX', label: 'Superplex' },
  { value: 'CINE_COMFORT', label: 'Cine Comfort' }
]

const form = reactive({ name: '', type: 'STANDARD', status: 'Active', turnaroundTimeMins: 15, matrixRow: 10, matrixCol: 16 })
const errors = reactive({})

const hasShowtimes = computed(() => props.mode === 'edit' && props.initial?.hasShowtimes)

// Dynamic Naming Logic
const padZero = (n) => (n < 10 ? '0' + n : n)

const isDuplicateName = computed(() => {
  if (!props.cinema || !props.cinema.halls) return false
  const v = (form.name || '').trim().toLowerCase()
  if (!v) return false
  return props.cinema.halls.some(room => {
    if (props.mode === 'edit' && props.initial && room.id === props.initial.id) return false
    return room.name.toLowerCase() === v
  })
})

const nextRoomNum = computed(() => {
  if (!props.cinema || !props.cinema.halls) return 1
  let maxFound = 0
  props.cinema.halls.forEach(room => {
    const match = room.name.match(/Phòng\s*(\d+)/i) || room.name.match(/\d+/)
    if (match) {
      const num = parseInt(match[1] || match[0], 10)
      if (num > maxFound) maxFound = num
    }
  })
  let nextNum = maxFound + 1
  while (true) {
    const candidateName = `Phòng ${padZero(nextNum)}`.toLowerCase()
    const isDup = props.cinema.halls.some(room => room.name.toLowerCase().includes(candidateName))
    if (!isDup) break
    nextNum++
  }
  return nextNum
})

const isSubmitting = ref(false)

// Nạp dữ liệu mỗi khi mở modal
watch(() => props.show, (open) => {
  if (!open) return
  isSubmitting.value = false
  Object.keys(errors).forEach(k => delete errors[k])
  if (props.mode === 'edit' && props.initial) {
    form.name = props.initial.name || ''
    form.type = props.initial.type || 'STANDARD'
    form.status = props.initial.status === 'Maintenance' || props.initial.status === 'Inactive' ? 'Maintenance' : 'Active'
    form.turnaroundTimeMins = props.initial.turnaroundTimeMins ?? 15
    form.matrixRow = props.initial.rows ?? props.initial.matrixRow ?? 10
    form.matrixCol = props.initial.cols ?? props.initial.matrixCol ?? 16
  } else {
    form.type = 'STANDARD'; form.status = 'Active'
    form.name = `Phòng ${padZero(nextRoomNum.value)} - Standard`
    form.turnaroundTimeMins = 15; form.matrixRow = 10; form.matrixCol = 16
  }
})

// Smart Naming Feature
watch(() => form.type, (newType) => {
  const t = ROOM_TYPES.find(x => x.value === newType)
  if (!t) return
  const match = form.name.match(/^Phòng (\d+)/i)
  const stt = match ? match[1] : padZero(nextRoomNum.value)
  
  if (!form.name || form.name.match(/^Phòng \d+( - .*)?$/i)) {
    form.name = `Phòng ${stt} - ${t.label}`
    validateName()
  }
})

const setRoomName = (base) => {
  const t = ROOM_TYPES.find(x => x.value === form.type)?.label || ''
  form.name = `${base} - ${t}`
  validateName()
}

const appendRoomType = () => {
  const t = ROOM_TYPES.find(x => x.value === form.type)?.label || ''
  const base = form.name.split(' - ')[0] || `Phòng ${padZero(nextRoomNum.value)}`
  form.name = `${base} - ${t}`
  validateName()
}

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
  if (!Number.isInteger(n) || n < 5 || n > 20) errors.matrixRow = 'Số hàng phải từ 5 đến 20'
  else errors.matrixRow = ''
  return !errors.matrixRow
}
const validateCol = () => {
  const n = Number(form.matrixCol)
  if (!Number.isInteger(n) || n < 5 || n > 30) errors.matrixCol = 'Số cột phải từ 5 đến 30'
  else errors.matrixCol = ''
  return !errors.matrixCol
}
const validateTurnaround = () => {
  const n = Number(form.turnaroundTimeMins)
  if (!Number.isInteger(n) || n < 10 || n > 60) errors.turnaroundTimeMins = 'Thời gian dọn phải từ 10 đến 60 phút'
  else errors.turnaroundTimeMins = ''
  return !errors.turnaroundTimeMins
}

const validateAll = () => [validateName(), validateRow(), validateCol(), validateTurnaround()].every(Boolean)

const handleSubmit = () => {
  if (!validateAll() || isDuplicateName.value) return
  isSubmitting.value = true
  
  emit('submit', {
    name: form.name,
    type: form.type,
    status: form.status,
    turnaroundTimeMins: Number(form.turnaroundTimeMins),
    matrixRow: Number(form.matrixRow),
    matrixCol: Number(form.matrixCol),
    onSuccess: () => {
      isSubmitting.value = false
    },
    onError: () => {
      isSubmitting.value = false
    }
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
            :class="(errors.name || isDuplicateName) ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
            class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all placeholder-white/20">
          <div class="flex flex-wrap gap-2 mt-2">
            <button type="button" @click="setRoomName('Phòng ' + padZero(nextRoomNum))" class="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-white/70 hover:bg-white/10 text-[10px] font-medium transition-colors">[ Phòng {{ padZero(nextRoomNum) }} ]</button>
            <button type="button" @click="setRoomName('Phòng ' + padZero(nextRoomNum + 1))" class="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-white/70 hover:bg-white/10 text-[10px] font-medium transition-colors">[ Phòng {{ padZero(nextRoomNum + 1) }} ]</button>
            <button type="button" @click="setRoomName('Phòng ' + padZero(nextRoomNum + 2))" class="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-white/70 hover:bg-white/10 text-[10px] font-medium transition-colors">[ Phòng {{ padZero(nextRoomNum + 2) }} ]</button>
            <button type="button" @click="appendRoomType" class="px-3 py-1 rounded-full bg-primary/20 border border-primary/30 text-primary hover:bg-primary/30 text-[10px] font-medium transition-colors">[ + Tên loại phòng ]</button>
          </div>
          <p v-if="errors.name" class="text-red-400 text-xs mt-1">{{ errors.name }}</p>
          <p v-else-if="isDuplicateName" class="text-red-400 text-xs mt-1">* Tên phòng này đã tồn tại trong cụm rạp!</p>
        </div>

        <div class="grid grid-cols-2 gap-5">
          <!-- Loại phòng -->
            <div class="space-y-1.5">
              <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Loại phòng</label>
              <select v-model="form.type" :disabled="hasShowtimes" 
                :class="hasShowtimes ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'"
                class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all appearance-none">
                <option v-for="t in ROOM_TYPES" :key="t.value" :value="t.value" class="bg-surface-container-high text-white">{{ t.label }}</option>
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
            <input v-model.number="form.matrixRow" @blur="validateRow" type="number" min="5" max="20" :disabled="hasShowtimes"
              :class="[errors.matrixRow ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50', hasShowtimes ? 'opacity-50 cursor-not-allowed' : '']"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all">
            <p v-if="errors.matrixRow" class="text-red-400 text-xs">{{ errors.matrixRow }}</p>
          </div>
          <!-- Số cột -->
          <div class="space-y-1.5">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Số cột ghế <span class="text-red-500">*</span></label>
            <input v-model.number="form.matrixCol" @blur="validateCol" type="number" min="5" max="30" :disabled="hasShowtimes"
              :class="[errors.matrixCol ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50', hasShowtimes ? 'opacity-50 cursor-not-allowed' : '']"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all">
            <p v-if="errors.matrixCol" class="text-red-400 text-xs">{{ errors.matrixCol }}</p>
          </div>

          <!-- Thời gian dọn -->
          <div class="space-y-1.5 col-span-2">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Thời gian dọn (phút)</label>
            <input v-model.number="form.turnaroundTimeMins" @blur="validateTurnaround" type="number" min="10" max="60"
              :class="errors.turnaroundTimeMins ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all">
            <p v-if="errors.turnaroundTimeMins" class="text-red-400 text-xs">{{ errors.turnaroundTimeMins }}</p>
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
        <button @click="$emit('close')" :disabled="isSubmitting" class="px-6 py-3 rounded-xl border border-white/10 text-on-surface-variant text-[10px] font-black uppercase tracking-widest hover:bg-white/5 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
          Hủy bỏ
        </button>
        <button @click="handleSubmit" :disabled="isDuplicateName || isSubmitting" class="px-8 py-3 rounded-xl bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest hover:brightness-110 transition-all shadow-lg shadow-primary/20 disabled:opacity-50 disabled:cursor-not-allowed">
          {{ isSubmitting ? 'Đang lưu...' : (mode === 'edit' ? 'Lưu thay đổi' : 'Tạo phòng') }}
        </button>
      </div>
    </div>
  </div>
</template>
