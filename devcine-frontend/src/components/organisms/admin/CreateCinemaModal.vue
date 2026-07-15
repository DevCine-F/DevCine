<script setup>
import { reactive, ref, onMounted, watch } from 'vue'
import api from '@/api/axios'

const props = defineProps({
  show: { type: Boolean, default: false },
  newCinema: { type: Object, required: true }
})

const emit = defineEmits(['close', 'create'])

const c = props.newCinema           // object dùng chung với composable (pattern sẵn có)
const errors = reactive({})         // { field: 'thông báo lỗi' }

// ===== Danh mục Tỉnh/Thành & Quận/Huyện (động từ API Backend) =====
const provinces = ref([])
const districts = ref([])
const loadingDistricts = ref(false)

onMounted(async () => {
  try {
    const { data } = await api.get('/locations/provinces')
    provinces.value = data || []
  } catch (e) { console.error('Không tải được danh sách Tỉnh/Thành', e) }
})

const fetchDistricts = async (province) => {
  if (!province) { districts.value = []; return }
  loadingDistricts.value = true
  try {
    const { data } = await api.get('/locations/districts', { params: { province } })
    districts.value = data || []
  } catch (e) {
    console.error('Không tải được danh sách Quận/Huyện', e)
    districts.value = []
  } finally {
    loadingDistricts.value = false
  }
}

// Đổi Tỉnh/Thành → nạp lại Quận/Huyện + reset lựa chọn quận cũ
watch(() => c.city, (val, old) => {
  if (val !== old) {
    c.district = ''
    errors.district = ''
    fetchDistricts(val)
  }
})

// ===== Helpers format =====
const collapseSpaces = (s) => (s || '').trim().replace(/\s+/g, ' ')
// Viết hoa chữ cái đầu mỗi từ, GIỮ nguyên các ký tự còn lại (không hạ thấp "DevCine")
const titleCase = (s) => collapseSpaces(s).replace(/(^|\s)(\p{L})/gu, (m, sp, ch) => sp + ch.toUpperCase())
const rawDigits = (s) => (s || '').replace(/\D/g, '')
// Định dạng hotline cho dễ đọc: tổng đài 1800/1900 → nhóm 4; số khác → 4-3-3...
const formatHotline = (s) => {
  const d = rawDigits(s).slice(0, 11)
  if (!d) return ''
  if (d.startsWith('1800') || d.startsWith('1900')) {
    return (d.slice(0, 4) + ' ' + d.slice(4).replace(/(\d{4})(?=\d)/g, '$1 ')).trim()
  }
  return (d.slice(0, 4) + ' ' + d.slice(4).replace(/(\d{3})(?=\d)/g, '$1 ')).trim()
}

// ===== Validators (set errors[field], trả về true nếu hợp lệ) =====
const NAME_RE = /^[\p{L}\p{N} \-&_]+$/u

const validateName = () => {
  const v = collapseSpaces(c.name)
  if (!v) errors.name = 'Tên cụm rạp không được để trống'
  else if (v.length < 5 || v.length > 100) errors.name = 'Tên cụm rạp phải từ 5 đến 100 ký tự'
  else if (!NAME_RE.test(v)) errors.name = 'Chỉ cho phép chữ, số, khoảng trắng và - & _'
  else errors.name = ''
  return !errors.name
}
const validateHotline = () => {
  const d = rawDigits(c.hotline)
  if (!d) errors.hotline = 'Hotline không được để trống'
  else if (d.length < 8 || d.length > 11) errors.hotline = 'Hotline phải gồm 8 đến 11 chữ số'
  else errors.hotline = ''
  return !errors.hotline
}
const validateAddress = () => {
  const v = collapseSpaces(c.address)
  if (!v) errors.address = 'Địa chỉ không được để trống'
  else if (v.length < 10 || v.length > 255) errors.address = 'Địa chỉ phải từ 10 đến 255 ký tự'
  else errors.address = ''
  return !errors.address
}
const validateCity = () => {
  if (!c.city) errors.city = 'Vui lòng chọn Tỉnh/Thành phố'
  else errors.city = ''
  return !errors.city
}
const validateDistrict = () => {
  if (!c.district) errors.district = 'Vui lòng chọn Quận/Huyện'
  else errors.district = ''
  return !errors.district
}
const validateDescription = () => {
  if ((c.description || '').length > 1000) errors.description = 'Mô tả tối đa 1000 ký tự'
  else errors.description = ''
  return !errors.description
}

// ===== Blur handlers: format trước, validate sau =====
const onNameBlur = () => { c.name = titleCase(c.name); validateName() }
const onAddressBlur = () => { c.address = titleCase(c.address); validateAddress() }
const onHotlineInput = () => { c.hotline = formatHotline(c.hotline); if (errors.hotline) validateHotline() }
const onHotlineBlur = () => { c.hotline = formatHotline(c.hotline); validateHotline() }
const onCityBlur = () => { c.city = collapseSpaces(c.city); validateCity() }
const onDescBlur = () => {
  // Gộp tối đa 2 lần xuống dòng liên tiếp, cắt khoảng trắng đầu/cuối
  c.description = (c.description || '').replace(/\n{3,}/g, '\n\n').trim()
  validateDescription()
}

const validateAll = () =>
  [validateName(), validateHotline(), validateAddress(), validateCity(), validateDistrict(), validateDescription()]
    .every(Boolean)

const handleCreate = () => {
  if (validateAll()) emit('create')
}
</script>

<template>
  <div v-if="show" class="fixed inset-0 z-[110] flex items-center justify-center bg-black/60 backdrop-blur-sm animate-in fade-in duration-300">
    <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col slide-in-from-bottom-8">
      <!-- Header -->
      <div class="px-8 py-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-high/30">
        <h2 class="text-xl font-black uppercase tracking-widest text-primary flex items-center gap-3">
          <span class="material-symbols-outlined">add_business</span>
          Thiết lập Cụm Rạp Mới
        </h2>
        <button @click="$emit('close')" class="text-on-surface-variant hover:text-white transition-colors">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <!-- Body -->
      <div class="p-8 space-y-6 overflow-y-auto">
        <div class="grid grid-cols-2 gap-x-6 gap-y-5">
          <!-- Tên Cụm Rạp -->
          <div class="space-y-1.5 col-span-2 sm:col-span-1">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Tên Cụm Rạp <span class="text-red-500">*</span></label>
            <input v-model="c.name" @blur="onNameBlur" type="text" placeholder="VD: DevCine Landmark 81"
              :class="errors.name ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all placeholder-white/20">
            <p v-if="errors.name" class="text-red-400 text-xs">{{ errors.name }}</p>
          </div>

          <!-- Hotline -->
          <div class="space-y-1.5 col-span-2 sm:col-span-1">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Hotline <span class="text-red-500">*</span></label>
            <input v-model="c.hotline" @input="onHotlineInput" @blur="onHotlineBlur" type="text" inputmode="numeric" placeholder="VD: 1900 1234"
              :class="errors.hotline ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all placeholder-white/20">
            <p v-if="errors.hotline" class="text-red-400 text-xs">{{ errors.hotline }}</p>
          </div>

          <!-- Địa chỉ -->
          <div class="space-y-1.5 col-span-2">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Địa chỉ chi tiết <span class="text-red-500">*</span></label>
            <input v-model="c.address" @blur="onAddressBlur" type="text" placeholder="VD: Tầng B1, Vincom Landmark 81, Bình Thạnh"
              :class="errors.address ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all placeholder-white/20">
            <p v-if="errors.address" class="text-red-400 text-xs">{{ errors.address }}</p>
          </div>

          <!-- Loại rạp -->
          <div class="space-y-1.5 col-span-2 sm:col-span-1">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Loại Cụm Rạp</label>
            <select v-model="c.type" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all appearance-none cursor-pointer">
              <option value="Standard" class="bg-surface-container-high text-white">Standard</option>
              <option value="Premium/IMAX" class="bg-surface-container-high text-white">Premium/IMAX</option>
              <option value="Sweetbox" class="bg-surface-container-high text-white">Sweetbox</option>
              <option value="Gold Class" class="bg-surface-container-high text-white">Gold Class</option>
            </select>
          </div>

          <!-- Tỉnh / Thành phố -->
          <div class="space-y-1.5 col-span-2 sm:col-span-1">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Tỉnh / Thành phố <span class="text-red-500">*</span></label>
            <select v-model="c.city" @change="validateCity"
              :class="errors.city ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all appearance-none cursor-pointer">
              <option value="" disabled class="bg-surface-container-high text-white/40">-- Chọn Tỉnh/Thành --</option>
              <option v-for="p in provinces" :key="p" :value="p" class="bg-surface-container-high text-white">{{ p }}</option>
            </select>
            <p v-if="errors.city" class="text-red-400 text-xs">{{ errors.city }}</p>
          </div>

          <!-- Quận / Huyện (phụ thuộc Tỉnh/Thành) -->
          <div class="space-y-1.5 col-span-2 sm:col-span-1">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Quận / Huyện <span class="text-red-500">*</span></label>
            <select v-model="c.district" @change="validateDistrict" :disabled="!c.city || loadingDistricts"
              :class="errors.district ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all appearance-none cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed">
              <option value="" disabled class="bg-surface-container-high text-white/40">
                {{ !c.city ? '-- Chọn Tỉnh/Thành trước --' : (loadingDistricts ? 'Đang tải...' : '-- Chọn Quận/Huyện --') }}
              </option>
              <option v-for="d in districts" :key="d" :value="d" class="bg-surface-container-high text-white">{{ d }}</option>
            </select>
            <p v-if="errors.district" class="text-red-400 text-xs">{{ errors.district }}</p>
          </div>

          <!-- Trạng thái -->
          <div class="space-y-1.5 col-span-2">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Trạng thái hoạt động</label>
            <select v-model="c.status" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:border-primary/50 focus:ring-1 focus:ring-primary/50 outline-none transition-all appearance-none cursor-pointer">
              <option value="ACTIVE" class="bg-surface-container-high">Đang hoạt động</option>
              <option value="MAINTENANCE" class="bg-surface-container-high">Bảo trì</option>
              <option value="CLOSED" class="bg-surface-container-high">Tạm đóng</option>
            </select>
          </div>

          <!-- Mô tả -->
          <div class="space-y-1.5 col-span-2">
            <label class="text-[10px] font-bold text-white/50 uppercase tracking-widest flex justify-between">
              <span>Mô tả</span>
              <span class="text-white/30 normal-case tracking-normal">{{ (c.description || '').length }}/1000</span>
            </label>
            <textarea v-model="c.description" @blur="onDescBlur" rows="3" maxlength="1000" placeholder="Giới thiệu ngắn về cụm rạp..."
              :class="errors.description ? '!border-red-500 focus:!ring-red-500/40' : 'border-white/10 focus:border-primary/50 focus:ring-primary/50'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-sm text-white focus:ring-1 outline-none transition-all resize-none placeholder-white/20"></textarea>
            <p v-if="errors.description" class="text-red-400 text-xs">{{ errors.description }}</p>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="px-8 py-6 border-t border-outline-variant/10 bg-surface-container-high/10 flex justify-end gap-4">
        <button @click="$emit('close')" class="px-6 py-3 rounded-xl border border-white/10 text-on-surface-variant text-[10px] font-black uppercase tracking-widest hover:bg-white/5 transition-all">
          Hủy bỏ
        </button>
        <button @click="handleCreate" class="px-8 py-3 rounded-xl bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest hover:brightness-110 transition-all shadow-lg shadow-primary/20 flex items-center gap-2">
          Tạo Cụm Rạp
        </button>
      </div>
    </div>
  </div>
</template>
