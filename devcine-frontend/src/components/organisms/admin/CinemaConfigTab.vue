<script setup>
import { toRef, ref, computed, watch, nextTick } from 'vue'
import { useCinemaConfig } from '@/composables/useCinemaConfig'
import { useConfirmStore } from '@/stores/confirm'
import { titleCase, formatHotline, parseGoogleMapsInput } from '@/utils/cinemaValidators'

const props = defineProps({
  cinema: { type: Object, required: true }
})
const emit = defineEmits(['deleted'])

const confirm = useConfirmStore()
const cinemaRef = toRef(props, 'cinema')

const {
  form,
  errors,
  provinces,
  districts,
  loadingDistricts,
  saving,
  closingCinema,
  reopeningCinema,
  isDirty,
  loadConfig,
  onCityChange,
  resetForm,
  validateField,
  saveConfig,
  closeCinema,
  reopenCinema,
} = useCinemaConfig(cinemaRef)

// ===== Textarea Mô tả auto-grow =====
const descRef = ref(null)
const sizeDescription = () => {
  const el = descRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}
const onDescInput = () => { validateField('description'); sizeDescription() }

// Dropdown liên hoàn: Tỉnh/Thành đổi -> nạp lại Quận/Huyện
watch(() => form.city, (val, old) => onCityChange(val, old))

// ===== Blur/format handlers =====
const onNameBlur = () => { form.name = titleCase(form.name); validateField('name') }
const onAddressBlur = () => { form.address = titleCase(form.address); validateField('address') }
const onHotlineInput = () => { form.hotline = formatHotline(form.hotline); if (errors.hotline) validateField('hotline') }
const onHotlineBlur = () => { form.hotline = formatHotline(form.hotline); validateField('hotline') }

// ===== Toạ độ bản đồ: GỘP về 1 ô "vĩ độ, kinh độ" (nhận cả link/mã Google Maps) =====
// form.latitude/longitude vẫn là nguồn dữ liệu thật; coordsInput chỉ là view 1 chiều
// (field -> form khi gõ; form -> field chỉ tại điểm load/reset để tránh vòng lặp reactive).
const coordsInput = ref('')
const coordsError = ref('')
// Bóc toạ độ: ưu tiên link/mã Maps, sau đó tới dạng "lat, lng" gõ tay.
const parseCoords = (raw) => {
  const s = (raw || '').trim()
  if (!s) return null
  const link = parseGoogleMapsInput(s)
  if (link) return link
  const m = s.match(/^(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)$/)
  return m ? { lat: m[1], lng: m[2] } : null
}
const syncCoordsFromForm = () => {
  const { latitude: lat, longitude: lng } = form
  coordsInput.value = (lat !== '' && lat != null && lng !== '' && lng != null) ? `${lat}, ${lng}` : ''
  coordsError.value = ''
}
const onCoordsInput = () => {
  const raw = coordsInput.value
  if (!raw.trim()) {
    form.latitude = ''; form.longitude = ''
    validateField('latitude'); validateField('longitude')
    coordsError.value = ''
    return
  }
  const parsed = parseCoords(raw)
  if (parsed) {
    form.latitude = parsed.lat
    form.longitude = parsed.lng
    validateField('latitude'); validateField('longitude')
    coordsError.value = ''
    // Dán link/mã -> chuẩn hoá hiển thị về "lat, lng" (chỉ khi có ký tự chữ/@/http)
    if (/[a-z@/]/i.test(raw)) coordsInput.value = `${parsed.lat}, ${parsed.lng}`
  }
  // gõ dở dang -> chưa báo lỗi, đợi blur
}
const onCoordsBlur = () => {
  const v = coordsInput.value.trim()
  coordsError.value = (v && !parseCoords(v)) ? 'invalid' : ''
}
const clearCoords = () => {
  coordsInput.value = ''
  form.latitude = ''; form.longitude = ''
  validateField('latitude'); validateField('longitude')
  coordsError.value = ''
}
// Hoàn tác: reset form rồi đồng bộ lại ô toạ độ từ giá trị đã nạp.
const handleReset = () => { resetForm(); syncCoordsFromForm() }

// Nạp cấu hình khi đổi cụm rạp (đặt sau các helper toạ độ để tránh TDZ với immediate:true)
watch(() => props.cinema, (c) => { if (c) { loadConfig(c); syncCoordsFromForm(); nextTick(sizeDescription) } }, { immediate: true })

// ===== Tiện ích: chip input (lưu canonical vào form.amenities dạng "a, b, c") =====
const amenityDraft = ref('')
const amenityChips = computed(() =>
  (form.amenities || '').split(',').map((s) => s.trim()).filter(Boolean)
)
const addAmenity = () => {
  const v = amenityDraft.value.trim()
  amenityDraft.value = ''
  if (!v) return
  if (!amenityChips.value.some((x) => x.toLowerCase() === v.toLowerCase())) {
    form.amenities = [...amenityChips.value, v].join(', ')
  }
}
const removeAmenity = (idx) => {
  const list = amenityChips.value.slice()
  list.splice(idx, 1)
  form.amenities = list.join(', ')
}
const onAmenityKey = (e) => {
  if (e.key === 'Enter' || e.key === ',') { e.preventDefault(); addAmenity() }
  else if (e.key === 'Backspace' && !amenityDraft.value && amenityChips.value.length) {
    removeAmenity(amenityChips.value.length - 1)
  }
}

// Nhãn/màu badge trạng thái
const STATUS_META = {
  ACTIVE: { label: 'Đang hoạt động', dot: 'bg-emerald-400', badgeClass: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30' },
  MAINTENANCE: { label: 'Bảo trì', dot: 'bg-amber-400', badgeClass: 'bg-amber-500/10 text-amber-400 border border-amber-500/30' },
  CLOSED: { label: 'Đã đóng cửa / Ẩn', dot: 'bg-red-400', badgeClass: 'bg-red-500/10 text-red-400 border border-red-500/30' },
}
</script>

<template>
  <div class="space-y-6 pb-28">
    <!-- ============ CARD: THÔNG TIN CƠ BẢN ============ -->
    <section class="bg-surface-container-high border border-outline-variant/10 rounded-2xl overflow-hidden">
      <header class="flex items-center gap-4 px-6 py-5 border-b border-outline-variant/10">
        <div class="w-10 h-10 rounded-2xl bg-primary/10 flex items-center justify-center flex-shrink-0">
          <span class="material-symbols-outlined text-primary text-lg">store</span>
        </div>
        <div class="flex-1">
          <h4 class="text-sm font-black uppercase tracking-widest text-on-surface">Thông tin cơ bản</h4>
          <p class="text-[10px] text-on-surface-variant mt-0.5">Tên, địa chỉ &amp; phân loại của cụm rạp</p>
        </div>
        <span class="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[10px] font-black uppercase tracking-widest"
          :class="(STATUS_META[form.status] || STATUS_META.ACTIVE).badgeClass">
          <span class="w-2 h-2 rounded-full" :class="(STATUS_META[form.status] || STATUS_META.ACTIVE).dot"></span>
          {{ (STATUS_META[form.status] || STATUS_META.ACTIVE).label }}
        </span>
      </header>

      <div class="p-6 grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-5">
        <!-- Tên rạp -->
        <div class="space-y-1.5">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tên rạp <span class="text-red-500">*</span></label>
          <input v-model="form.name" @blur="onNameBlur" type="text" placeholder="VD: DevCine Hà Nội"
            :class="errors.name ? '!border-red-500' : 'border-outline-variant/20 focus:border-primary/50'"
            class="w-full bg-surface-container border rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none transition-all" />
          <p v-if="errors.name" class="text-red-400 text-xs">{{ errors.name }}</p>
        </div>

        <!-- Hotline -->
        <div class="space-y-1.5">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Hotline <span class="normal-case text-on-surface-variant/50">(8–11 số)</span> <span class="text-red-500">*</span></label>
          <input v-model="form.hotline" @input="onHotlineInput" @blur="onHotlineBlur" type="text" inputmode="numeric" placeholder="VD: 1900 1234"
            :class="errors.hotline ? '!border-red-500' : 'border-outline-variant/20 focus:border-primary/50'"
            class="w-full bg-surface-container border rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none transition-all" />
          <p v-if="errors.hotline" class="text-red-400 text-xs">{{ errors.hotline }}</p>
        </div>

        <!-- Tỉnh / Thành -->
        <div class="space-y-1.5">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tỉnh / Thành phố <span class="text-red-500">*</span></label>
          <select v-model="form.city" @change="validateField('city')"
            :class="errors.city ? '!border-red-500' : 'border-outline-variant/20 focus:border-primary/50'"
            class="w-full bg-surface-container border rounded-xl px-4 py-3 text-sm text-on-surface focus:outline-none transition-all appearance-none cursor-pointer">
            <option value="" disabled>-- Chọn Tỉnh/Thành --</option>
            <option v-for="p in provinces" :key="p" :value="p" class="bg-surface-container-high text-on-surface">{{ p }}</option>
          </select>
          <p v-if="errors.city" class="text-red-400 text-xs">{{ errors.city }}</p>
        </div>

        <!-- Quận / Huyện -->
        <div class="space-y-1.5">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Quận / Huyện <span class="text-red-500">*</span></label>
          <select v-model="form.district" @change="validateField('district')" :disabled="!form.city || loadingDistricts"
            :class="errors.district ? '!border-red-500' : 'border-outline-variant/20 focus:border-primary/50'"
            class="w-full bg-surface-container border rounded-xl px-4 py-3 text-sm text-on-surface focus:outline-none transition-all appearance-none cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed">
            <option value="" disabled>
              {{ !form.city ? '-- Chọn Tỉnh/Thành trước --' : (loadingDistricts ? 'Đang tải...' : '-- Chọn Quận/Huyện --') }}
            </option>
            <option v-for="d in districts" :key="d" :value="d" class="bg-surface-container-high text-on-surface">{{ d }}</option>
          </select>
          <p v-if="errors.district" class="text-red-400 text-xs">{{ errors.district }}</p>
        </div>

        <!-- Địa chỉ -->
        <div class="md:col-span-2 space-y-1.5">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Địa chỉ chi tiết <span class="text-red-500">*</span></label>
          <input v-model="form.address" @blur="onAddressBlur" type="text" placeholder="VD: 123 Phố Huế, Hai Bà Trưng"
            :class="errors.address ? '!border-red-500' : 'border-outline-variant/20 focus:border-primary/50'"
            class="w-full bg-surface-container border rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none transition-all" />
          <p v-if="errors.address" class="text-red-400 text-xs">{{ errors.address }}</p>
        </div>

        <!-- Toạ độ bản đồ: 1 ô — dán link/mã Google Maps hoặc gõ "vĩ độ, kinh độ" -->
        <div class="md:col-span-2 space-y-1.5">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant flex items-center gap-1.5">
            <span class="material-symbols-outlined text-primary text-sm">map</span>Toạ độ bản đồ <span class="normal-case text-on-surface-variant/50">(tuỳ chọn)</span>
          </label>
          <div class="relative">
            <input v-model="coordsInput" @input="onCoordsInput" @blur="onCoordsBlur" type="text"
              placeholder="Dán link Google Maps, hoặc nhập: vĩ độ, kinh độ"
              :class="(coordsError || errors.latitude || errors.longitude) ? '!border-red-500' : 'border-outline-variant/20 focus:border-primary/50'"
              class="w-full bg-surface-container border rounded-xl pl-4 pr-10 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none transition-all" />
            <button v-if="coordsInput" type="button" @click="clearCoords" tabindex="-1"
              class="absolute right-2 top-1/2 -translate-y-1/2 w-6 h-6 rounded-lg flex items-center justify-center text-on-surface-variant/60 hover:text-on-surface hover:bg-white/5 transition-colors">
              <span class="material-symbols-outlined text-base">close</span>
            </button>
          </div>
        </div>

        <!-- Loại rạp -->
        <div class="md:col-span-2 space-y-1.5">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Loại cụm rạp</label>
          <select v-model="form.type"
            class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface focus:outline-none focus:border-primary/50 transition-all appearance-none cursor-pointer">
            <option value="STANDARD" class="bg-surface-container-high">Standard</option>
            <option value="SUPERPLEX" class="bg-surface-container-high">Superplex</option>
            <option value="CINE_COMFORT" class="bg-surface-container-high">Cine Comfort</option>
          </select>
        </div>

        <!-- Tiện ích (chip) -->
        <div class="md:col-span-2 space-y-1.5">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Tiện ích <span class="normal-case text-on-surface-variant/50">(Enter hoặc dấu phẩy để thêm)</span></label>
          <div class="flex flex-wrap items-center gap-2 bg-surface-container border border-outline-variant/20 rounded-xl px-3 py-2.5 focus-within:border-primary/50 transition-all">
            <span v-for="(chip, idx) in amenityChips" :key="chip + idx"
              class="inline-flex items-center gap-1.5 pl-3 pr-1.5 py-1 rounded-lg bg-primary/10 text-primary text-xs font-bold">
              {{ chip }}
              <button type="button" @click="removeAmenity(idx)" class="w-4 h-4 rounded flex items-center justify-center hover:bg-primary/20 transition-colors">
                <span class="material-symbols-outlined text-xs leading-none">close</span>
              </button>
            </span>
            <input v-model="amenityDraft" @keydown="onAmenityKey" @blur="addAmenity" type="text"
              :placeholder="amenityChips.length ? 'Thêm tiện ích...' : 'VD: Bãi đỗ xe, Nhà hàng, Wifi'"
              class="flex-1 min-w-[8rem] bg-transparent text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none py-1" />
          </div>
        </div>

        <!-- Mô tả -->
        <div class="md:col-span-2 space-y-1.5">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant flex justify-between">
            <span>Mô tả <span class="normal-case text-on-surface-variant/50">(tối đa 1000 ký tự)</span></span>
            <span class="text-on-surface-variant/40 normal-case tracking-normal">{{ (form.description || '').length }}/1000</span>
          </label>
          <textarea ref="descRef" v-model="form.description" @input="onDescInput" rows="3" maxlength="1000" placeholder="Giới thiệu ngắn về cụm rạp..."
            :class="errors.description ? '!border-red-500' : 'border-outline-variant/20 focus:border-primary/50'"
            class="w-full bg-surface-container border rounded-xl px-4 py-3 text-sm text-on-surface placeholder-on-surface-variant/40 focus:outline-none transition-all resize-none overflow-hidden" />
          <p v-if="errors.description" class="text-red-400 text-xs">{{ errors.description }}</p>
        </div>
      </div>
    </section>

    <!-- ============ CARD: GIỜ HOẠT ĐỘNG ============ -->
    <section class="bg-surface-container-high border border-outline-variant/10 rounded-2xl overflow-hidden">
      <header class="flex items-center gap-4 px-6 py-5 border-b border-outline-variant/10">
        <div class="w-10 h-10 rounded-2xl bg-blue-500/10 flex items-center justify-center flex-shrink-0">
          <span class="material-symbols-outlined text-blue-400 text-lg">schedule</span>
        </div>
        <div class="flex-1">
          <h4 class="text-sm font-black uppercase tracking-widest text-on-surface">Giờ hoạt động</h4>
          <p class="text-[10px] text-on-surface-variant mt-0.5">Khung giờ mở/đóng cửa hằng ngày</p>
        </div>
        <span class="text-blue-400 text-xs font-black">{{ form.openingTime || '--:--' }} – {{ form.closingTime || '--:--' }}</span>
      </header>
      <div class="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="space-y-1.5">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Giờ mở cửa</label>
          <input v-model="form.openingTime" type="time"
            class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface focus:outline-none focus:border-primary/50 transition-all" />
        </div>
        <div class="space-y-1.5">
          <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Giờ đóng cửa</label>
          <input v-model="form.closingTime" type="time"
            class="w-full bg-surface-container border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface focus:outline-none focus:border-primary/50 transition-all" />
        </div>
        <p class="md:col-span-2 text-[10px] text-on-surface-variant/60 flex items-center gap-1.5">
          <span class="material-symbols-outlined text-sm">info</span>Nếu giờ đóng ≤ giờ mở, hệ thống hiểu là rạp đóng cửa rạng sáng hôm sau (suất khuya).
        </p>
      </div>
    </section>

    <!-- ============ VÙNG NGUY HIỂM ============ -->
    <section class="bg-red-500/5 border border-red-500/30 rounded-2xl overflow-hidden">
      <header class="flex items-center gap-4 px-6 py-5 border-b border-red-500/20">
        <div class="w-10 h-10 rounded-2xl bg-red-500/15 flex items-center justify-center flex-shrink-0">
          <span class="material-symbols-outlined text-red-400 text-lg">warning</span>
        </div>
        <div class="flex-1">
          <h4 class="text-sm font-black uppercase tracking-widest text-red-400">Vùng nguy hiểm</h4>
          <p class="text-[10px] text-on-surface-variant mt-0.5">Thao tác ảnh hưởng trực tiếp đến trạng thái hiển thị và vận hành rạp</p>
        </div>
      </header>

      <div class="p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div v-if="form.status !== 'CLOSED'" class="flex-1">
          <p class="text-sm font-bold text-on-surface">Đóng cụm rạp này</p>
          <p class="text-xs text-on-surface-variant mt-1 leading-relaxed">
            Ẩn hoàn toàn cụm rạp khỏi hệ thống người dùng (Client). Chỉ thực hiện được khi cụm rạp <b class="text-on-surface">KHÔNG còn bất kỳ suất chiếu nào</b> trong tương lai.
          </p>
        </div>
        <div v-else class="flex-1">
          <p class="text-sm font-bold text-on-surface">Mở lại cụm rạp này</p>
          <p class="text-xs text-on-surface-variant mt-1 leading-relaxed">
            Cụm rạp đang bị đóng và ẩn khỏi toàn bộ trang đặt vé phía khách hàng. Bấm vào đây để mở lại hoạt động cho cụm rạp.
          </p>
        </div>

        <button 
          v-if="form.status !== 'CLOSED'"
          @click="closeCinema" 
          :disabled="closingCinema"
          class="flex-shrink-0 bg-red-500 text-white font-black text-[10px] uppercase tracking-widest px-6 py-3 rounded-xl hover:bg-red-600 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-red-500/20"
        >
          <span v-if="closingCinema" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
          <span v-else class="material-symbols-outlined text-sm">block</span>
          {{ closingCinema ? 'Đang kiểm tra...' : 'Đóng cụm rạp này' }}
        </button>

        <button 
          v-else
          @click="reopenCinema" 
          :disabled="reopeningCinema"
          class="flex-shrink-0 bg-emerald-600 text-white font-black text-[10px] uppercase tracking-widest px-6 py-3 rounded-xl hover:bg-emerald-500 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-emerald-600/20"
        >
          <span v-if="reopeningCinema" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
          <span v-else class="material-symbols-outlined text-sm">lock_open</span>
          {{ reopeningCinema ? 'Đang mở lại...' : 'Mở lại cụm rạp' }}
        </button>
      </div>
    </section>

    <!-- ============ FLOATING ACTION BAR ============ -->
    <!-- Teleport ra body: panel chi tiết có transform (animate-in) tạo containing block,
         khiến position:fixed bị "giam" trong panel thay vì neo theo viewport. -->
    <Teleport to="body">
      <transition name="fab">
        <div v-if="isDirty" class="fixed bottom-6 left-1/2 -translate-x-1/2 z-[200] flex items-center gap-3 bg-surface-container-highest border border-outline-variant/20 shadow-2xl shadow-black/40 rounded-2xl pl-5 pr-3 py-3">
          <span class="material-symbols-outlined text-amber-400 text-lg">edit_note</span>
          <span class="text-xs font-bold text-on-surface hidden sm:inline">Có thay đổi chưa lưu</span>
          <button @click="handleReset" :disabled="saving"
            class="px-4 py-2.5 rounded-xl border border-outline-variant/20 text-on-surface-variant text-[10px] font-black uppercase tracking-widest hover:bg-white/5 transition-all disabled:opacity-50">
            Hoàn tác
          </button>
          <button @click="saveConfig" :disabled="saving"
            class="bg-primary text-on-primary font-black text-[10px] uppercase tracking-widest px-6 py-2.5 rounded-xl hover:brightness-110 transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
            <span v-if="saving" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
            <span v-else class="material-symbols-outlined text-sm">save</span>
            {{ saving ? 'Đang lưu...' : 'Lưu cấu hình' }}
          </button>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<style scoped>
.fab-enter-active, .fab-leave-active { transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1); }
.fab-enter-from, .fab-leave-to { opacity: 0; transform: translate(-50%, 1.5rem); }
</style>
