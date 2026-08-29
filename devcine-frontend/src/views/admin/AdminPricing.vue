<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { pricingApi } from '@/api/admin'
import { useConfirmStore } from '@/stores/confirm'
import { useAdminPerm } from '@/composables/useAdminPerm'
import { useToastStore } from '@/stores/toast'
import { friendlyError } from '@/utils/friendlyError'

const { can } = useAdminPerm()
const confirm = useConfirmStore()

const loading = ref(true)
const loadError = ref(false)
const saving = ref(false)
const activeTab = ref('base')

const config = ref(null)
const baseMatrix = reactive({})       // key `${roomType}|${dayType}|${audience}` -> value (flat: không theo giờ)
const formats = ref([])
const holidays = ref([])
const newHoliday = reactive({ holidayDate: '', name: '' })

// Simulator (flat pricing: ngày × đối tượng × loại phòng × định dạng)
const sim = reactive({ dayType: 'WEEKEND', audienceType: 'ADULT', roomType: 'STANDARD', formatId: '' })
const simResult = ref(null)
const simulating = ref(false)

const toast = useToastStore()

const fmt = (n) => Number(n || 0).toLocaleString('vi-VN')
const fmtThousand = (n) => (n === null || n === undefined || n === '' ? '' : Number(n).toLocaleString('vi-VN'))

// Giới hạn giá trị tối đa cho 1 ô tiền vé
const MIN_BASE_PRICE = 20000
const MAX_BASE_PRICE = 1000000
const MAX_PRICE = 999999999

const baseErrors = reactive({})
const baseTouched = ref(false)

const getBasePriceError = (val) => {
  if (val === null || val === undefined || val === '') {
    return 'Không được để trống'
  }
  const num = Number(val)
  if (isNaN(num) || !Number.isInteger(num) || num < 0) {
    return 'Phải là số nguyên không âm'
  }
  if (num < MIN_BASE_PRICE) {
    return `Tối thiểu ${fmtThousand(MIN_BASE_PRICE)} đ`
  }
  if (num > MAX_BASE_PRICE) {
    return `Tối đa ${fmtThousand(MAX_BASE_PRICE)} đ`
  }
  if (num % 1000 !== 0) {
    return 'Phải là bội số 1.000 đ'
  }
  return null
}

const onBaseMatrixInput = (e, key) => {
  const input = e.target
  const rawOldVal = input.value || ''
  const caretPos = input.selectionStart || 0
  const digitsBefore = rawOldVal.slice(0, caretPos).replace(/\D/g, '').length

  let cleanDigits = rawOldVal.replace(/\D/g, '').replace(/^0+(?=\d)/, '')
  if (cleanDigits.length > 9) cleanDigits = cleanDigits.slice(0, 9)

  if (!cleanDigits) {
    baseMatrix[key] = ''
    input.value = ''
    baseErrors[key] = getBasePriceError('')
    return
  }

  const numVal = Number(cleanDigits)
  baseMatrix[key] = numVal
  baseErrors[key] = getBasePriceError(numVal)

  const formattedVal = numVal.toLocaleString('vi-VN')
  input.value = formattedVal

  let newCaretPos = 0
  let digitsCount = 0
  for (let i = 0; i < formattedVal.length; i++) {
    if (/\d/.test(formattedVal[i])) digitsCount++
    if (digitsCount === digitsBefore) {
      newCaretPos = i + 1
      break
    }
  }
  if (digitsBefore === 0) {
    newCaretPos = 0
  }
  input.setSelectionRange(newCaretPos, newCaretPos)
}

const onFormatSurchargeInput = (e, f, field) => {
  const input = e.target
  const rawOldVal = input.value || ''
  const caretPos = input.selectionStart || 0
  const digitsBefore = rawOldVal.slice(0, caretPos).replace(/\D/g, '').length

  let cleanDigits = rawOldVal.replace(/\D/g, '').replace(/^0+(?=\d)/, '')
  if (cleanDigits.length > 9) cleanDigits = cleanDigits.slice(0, 9)

  if (field === 'weekendSurcharge' && !cleanDigits && rawOldVal.trim() === '') {
    f[field] = null
    input.value = ''
    validateFormatItem(f)
    return
  }

  if (field === 'surcharge' && !cleanDigits) {
    f[field] = ''
    input.value = ''
    validateFormatItem(f)
    return
  }

  const numVal = cleanDigits ? Math.min(Number(cleanDigits), MAX_PRICE) : 0
  f[field] = numVal

  const formattedVal = cleanDigits ? numVal.toLocaleString('vi-VN') : (field === 'weekendSurcharge' ? '' : '0')
  input.value = formattedVal

  let newCaretPos = 0
  let digitsCount = 0
  for (let i = 0; i < formattedVal.length; i++) {
    if (/\d/.test(formattedVal[i])) digitsCount++
    if (digitsCount === digitsBefore) {
      newCaretPos = i + 1
      break
    }
  }
  if (digitsBefore === 0) {
    newCaretPos = 0
  }
  input.setSelectionRange(newCaretPos, newCaretPos)

  validateFormatItem(f)
}

const clearWeekendSurcharge = (f) => {
  f.weekendSurcharge = null
  validateFormatItem(f)
}

const formatMovieFormatName = (raw) => {
  if (!raw) return ''
  return raw
    .trim()
    .split(/\s+/)
    .map(w => {
      const lower = w.toLowerCase()
      if (['2d', '3d', '4d', '4dx', 'imax'].includes(lower)) {
        return w.toUpperCase()
      }
      return w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()
    })
    .join(' ')
}
const audienceEntries = computed(() => Object.entries(config.value?.audiences || {}))
const roomTypes = computed(() => config.value?.roomTypes || [])

const MAX_SURCHARGE = 1000000
const formatErrors = reactive({})

const getFormatSurchargeError = (val) => {
  if (val === null || val === undefined || val === '') {
    return 'Không được để trống'
  }
  const num = Number(val)
  if (isNaN(num) || !Number.isInteger(num) || num < 0) {
    return 'Phải là số nguyên không âm'
  }
  if (num > MAX_SURCHARGE) {
    return `Tối đa ${fmtThousand(MAX_SURCHARGE)} đ`
  }
  if (num % 1000 !== 0) {
    return 'Phải là bội số 1.000 đ'
  }
  return null
}

const getFormatWeekendSurchargeError = (weekendVal, regularVal) => {
  if (weekendVal === null || weekendVal === undefined || weekendVal === '') {
    return null // Cho phép để trống = ngày thường
  }
  const num = Number(weekendVal)
  if (isNaN(num) || !Number.isInteger(num) || num < 0) {
    return 'Phải là số nguyên không âm'
  }
  if (num > MAX_SURCHARGE) {
    return `Tối đa ${fmtThousand(MAX_SURCHARGE)} đ`
  }
  if (num % 1000 !== 0) {
    return 'Phải là bội số 1.000 đ'
  }
  const regNum = Number(regularVal || 0)
  if (num < regNum) {
    return `Phải ≥ ngày thường (${fmtThousand(regNum)} đ)`
  }
  return null
}

const validateFormatItem = (f) => {
  const errSurcharge = getFormatSurchargeError(f.surcharge)
  if (errSurcharge) {
    formatErrors[`${f.id}_surcharge`] = errSurcharge
  } else {
    delete formatErrors[`${f.id}_surcharge`]
  }

  const errWeekend = getFormatWeekendSurchargeError(f.weekendSurcharge, f.surcharge)
  if (errWeekend) {
    formatErrors[`${f.id}_weekendSurcharge`] = errWeekend
  } else {
    delete formatErrors[`${f.id}_weekendSurcharge`]
  }
}

const loadConfig = async () => {
  loading.value = true
  loadError.value = false
  try {
    const { data } = await pricingApi.getConfig()
    config.value = data

    Object.keys(baseMatrix).forEach(k => delete baseMatrix[k])
    Object.keys(baseErrors).forEach(k => delete baseErrors[k])
    Object.keys(formatErrors).forEach(k => delete formatErrors[k])
    const existing = {}
    ;(data.baseMatrix || []).forEach(r => { existing[`${r.roomType}|${r.dayType}|${r.audienceType}`] = r.value })
    ;(data.roomTypes || []).forEach(rt => data.dayTypes.forEach(d => Object.keys(data.audiences).forEach(a => {
      const key = `${rt.code}|${d.code}|${a}`
      baseMatrix[key] = existing[key] ?? 0
    })))

    formats.value = (data.formats || [])
      .map(f => ({
        ...f,
        name: formatMovieFormatName(f.name),
        surcharge: Number(f.surcharge || 0),
        weekendSurcharge: f.weekendSurcharge == null ? null : Number(f.weekendSurcharge),
      }))
      .sort((a, b) => Number(a.id || 0) - Number(b.id || 0))

    holidays.value = data.holidays || []
    if (roomTypes.value.length) sim.roomType = roomTypes.value[0].code
    if (formats.value.length) sim.formatId = formats.value[0].id
  } catch (e) {
    loadError.value = true
    toast.error(friendlyError(e, 'Không tải được cấu hình giá vé.'))
  } finally {
    loading.value = false
  }
}

onMounted(loadConfig)

const validateAllBase = () => {
  baseTouched.value = true
  let hasError = false
  let firstErrorMsg = ''

  ;(roomTypes.value || []).forEach(rt => {
    ;(config.value?.dayTypes || []).forEach(d => {
      Object.keys(config.value?.audiences || {}).forEach(a => {
        const key = `${rt.code}|${d.code}|${a}`
        const err = getBasePriceError(baseMatrix[key])
        if (err) {
          baseErrors[key] = err
          hasError = true
          if (!firstErrorMsg) {
            const audLabel = config.value?.audiences?.[a] || a
            firstErrorMsg = `${rt.label} (${d.label} - ${audLabel}): ${err}`
          }
        } else {
          delete baseErrors[key]
        }
      })
    })
  })

  if (hasError) {
    toast.error(firstErrorMsg || 'Vui lòng sửa các ô giá vé chưa hợp lệ.')
    return false
  }
  return true
}

const saveBase = async () => {
  if (!validateAllBase()) return

  saving.value = true
  try {
    const rules = Object.entries(baseMatrix).map(([k, value]) => {
      const [roomType, dayType, audienceType] = k.split('|')
      return { dayType, roomType, audienceType, value: Number(value || 0) }
    })
    await pricingApi.saveBaseMatrix(rules)
    toast.success('Đã lưu bảng giá nền.')
  } catch (e) {
    toast.error(friendlyError(e, 'Lưu giá nền thất bại.'))
  } finally { saving.value = false }
}

const validateAllFormats = () => {
  let hasError = false
  let firstErrorMsg = ''

  ;(formats.value || []).forEach(f => {
    validateFormatItem(f)
    const errSur = formatErrors[`${f.id}_surcharge`]
    const errWk = formatErrors[`${f.id}_weekendSurcharge`]
    if (errSur && !firstErrorMsg) {
      firstErrorMsg = `${f.name} (Phụ thu ngày thường): ${errSur}`
    }
    if (errWk && !firstErrorMsg) {
      firstErrorMsg = `${f.name} (Phụ thu cuối tuần & lễ): ${errWk}`
    }
    if (errSur || errWk) {
      hasError = true
    }
  })

  if (hasError) {
    toast.error(firstErrorMsg || 'Vui lòng sửa các ô phụ thu định dạng chưa hợp lệ.')
    return false
  }
  return true
}

const saveFormats = async () => {
  if (!validateAllFormats()) return

  saving.value = true
  try {
    await pricingApi.saveFormats(formats.value.map(f => ({
      id: f.id,
      surcharge: Number(f.surcharge || 0),
      weekendSurcharge: f.weekendSurcharge == null || f.weekendSurcharge === '' ? null : Number(f.weekendSurcharge),
    })))
    toast.success('Đã lưu cấu hình định dạng.')
  } catch (e) {
    toast.error(friendlyError(e, 'Lưu định dạng thất bại.'))
  } finally { saving.value = false }
}

const addHoliday = async () => {
  if (!newHoliday.holidayDate || !newHoliday.name.trim()) {
    toast.warning('Nhập đủ ngày và tên ngày lễ.'); return
  }
  try {
    await pricingApi.addHoliday(newHoliday.holidayDate, newHoliday.name.trim())
    newHoliday.holidayDate = ''; newHoliday.name = ''
    await loadConfig()
    toast.success('Đã thêm ngày lễ.')
  } catch (e) {
    toast.error(friendlyError(e, 'Thêm ngày lễ thất bại.'))
  }
}

const removeHoliday = async (h) => {
  const ok = await confirm.show({
    title: 'Xoá ngày lễ',
    message: `Xoá ngày lễ "${h.name}" (${h.holidayDate})?`,
    confirmText: 'Xoá',
    tone: 'danger',
  })
  if (!ok) return
  try {
    await pricingApi.deleteHoliday(h.id)
    holidays.value = holidays.value.filter(x => x.id !== h.id)
    toast.success('Đã xoá ngày lễ.')
  } catch (e) {
    toast.error(friendlyError(e, 'Xoá ngày lễ thất bại.'))
  }
}

const runSimulate = async () => {
  simulating.value = true
  simResult.value = null
  try {
    const { data } = await pricingApi.simulate({
      dayType: sim.dayType,
      audienceType: sim.audienceType,
      roomType: sim.roomType,
      formatId: sim.formatId,
    })
    simResult.value = data
  } catch (e) {
    toast.error(friendlyError(e, 'Tính thử thất bại.'))
  } finally { simulating.value = false }
}

const TABS = [
  { key: 'base', label: 'Giá nền', icon: 'grid_on' },
  { key: 'format', label: 'Định dạng', icon: 'movie' },
  { key: 'holiday', label: 'Ngày lễ', icon: 'event' },
  { key: 'sim', label: 'Tính thử', icon: 'calculate' },
]
</script>

<template>
  <div class="p-6 md:p-10 space-y-6">
    <header>
      <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight font-headline uppercase text-primary">Cấu hình giá vé</h1>
      <p class="text-on-surface-variant text-sm mt-1">Flat pricing: giá = giá nền (loại phòng × loại ngày × đối tượng) + phụ thu định dạng (2D/3D). Mọi ghế trong cùng phòng + định dạng đồng giá.</p>
    </header>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 5" :key="i" class="h-12 bg-white/5 animate-pulse"></div>
    </div>

    <div v-else-if="loadError" class="bg-red-500/10 border border-red-500/30 p-8 text-center">
      <span class="material-symbols-outlined text-4xl text-red-400 mb-2">error</span>
      <p class="text-on-surface-variant mb-4">Không tải được cấu hình giá.</p>
      <button @click="loadConfig" class="px-5 py-2 bg-primary text-on-primary font-bold">Thử lại</button>
    </div>

    <template v-else>
      <div class="flex flex-wrap gap-2 border-b border-outline-variant/10">
        <button v-for="t in TABS" :key="t.key" @click="activeTab = t.key"
          :class="activeTab === t.key ? 'border-primary text-primary' : 'border-transparent text-on-surface-variant hover:text-on-surface'"
          class="flex items-center gap-2 px-4 py-3 border-b-2 font-bold text-sm transition-colors">
          <span class="material-symbols-outlined text-lg">{{ t.icon }}</span>{{ t.label }}
        </button>
      </div>

      <!-- TAB: Giá nền (loại phòng × loại ngày × đối tượng) -->
      <section v-if="activeTab === 'base'" class="space-y-6">
        <div v-for="rt in roomTypes" :key="rt.code" class="space-y-3 bg-surface-container-low border border-outline-variant/10 p-5 md:p-6 shadow-sm">
          <div class="flex items-center justify-between">
            <h3 class="font-bold text-on-surface uppercase tracking-wider text-sm flex items-center gap-2.5">
              <span class="p-1.5 bg-primary/10 text-primary flex items-center justify-center">
                <span class="material-symbols-outlined text-lg">meeting_room</span>
              </span>
              <span>{{ rt.label }}</span>
            </h3>
            <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant bg-white/5 px-2.5 py-1 border border-outline-variant/10">Mã: {{ rt.code }}</span>
          </div>

          <div class="overflow-x-auto border border-outline-variant/10">
            <table class="w-full text-sm">
              <thead>
                <tr class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant bg-surface-container-high/60 border-b border-outline-variant/10">
                  <th class="py-3.5 px-4 text-left">Loại ngày \ Đối tượng</th>
                  <th v-for="[code, label] in audienceEntries" :key="code" class="py-3.5 px-4 text-center">{{ label }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-outline-variant/10">
                <tr v-for="d in config.dayTypes" :key="d.code" class="hover:bg-white/[0.02] transition-colors">
                  <td class="py-3 px-4 font-bold text-on-surface">
                    <div class="flex items-center gap-2">
                      <span v-if="d.code === 'WEEKEND'" class="w-2 h-2 bg-amber-400"></span>
                      <span v-else class="w-2 h-2 bg-primary"></span>
                      <span>{{ d.label }}</span>
                    </div>
                  </td>
                  <td v-for="[code] in audienceEntries" :key="code" class="py-3 px-4 text-center align-top">
                    <div class="inline-flex flex-col items-center">
                      <div class="inline-flex items-center relative group">
                        <input
                          type="text"
                          inputmode="numeric"
                          :value="fmtThousand(baseMatrix[`${rt.code}|${d.code}|${code}`])"
                          @input="onBaseMatrixInput($event, `${rt.code}|${d.code}|${code}`)"
                          class="w-32 bg-surface-container-high border py-2 pl-3 pr-7 text-right font-bold outline-none transition-all tabular-nums text-sm shadow-sm"
                          :class="baseErrors[`${rt.code}|${d.code}|${code}`] ? 'border-red-500 text-red-400 bg-red-500/10 focus:border-red-500 focus:ring-1 focus:ring-red-500' : 'border-outline-variant/20 text-on-surface group-hover:border-outline-variant/40 focus:border-primary focus:ring-1 focus:ring-primary'"
                        />
                        <span class="absolute right-2.5 text-xs font-bold pointer-events-none select-none" :class="baseErrors[`${rt.code}|${d.code}|${code}`] ? 'text-red-400' : 'text-on-surface-variant/60 group-focus-within:text-primary'">đ</span>
                      </div>
                      <span v-if="baseErrors[`${rt.code}|${d.code}|${code}`]" class="text-[10px] font-bold text-red-400 mt-1 max-w-[130px] leading-tight text-center" :title="baseErrors[`${rt.code}|${d.code}|${code}`]">
                        {{ baseErrors[`${rt.code}|${d.code}|${code}`] }}
                      </span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs text-on-surface-variant">
          <p class="flex items-center gap-1.5">
            <span class="material-symbols-outlined text-sm text-primary">info</span>
            * Bậc "Cao điểm" gộp T6, T7, CN và mọi ngày trong tab "Ngày lễ".
          </p>
          <p class="text-on-surface-variant/80">
            Quy định: Số nguyên từ 20.000 đ – 1.000.000 đ, bội số của 1.000 đ.
          </p>
        </div>
        <div>
          <button v-if="can('pricing', 'edit')" @click="saveBase" :disabled="saving" class="px-6 py-3 bg-primary hover:bg-primary/90 text-on-primary font-bold uppercase tracking-wider text-xs shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center gap-2 disabled:opacity-60 disabled:pointer-events-none">
            <span class="material-symbols-outlined text-base">{{ saving ? 'sync' : 'save' }}</span>
            {{ saving ? 'Đang lưu...' : 'Lưu giá nền' }}
          </button>
        </div>
      </section>

      <!-- TAB: Định dạng (phụ thu công nghệ 2D/3D) -->
      <section v-else-if="activeTab === 'format'" class="space-y-4">
        <p class="text-sm text-on-surface-variant">Phụ thu công nghệ định dạng, cộng vào giá nền. Giá theo hạng phòng đã cấu hình ở tab "Giá nền".</p>
        <div class="overflow-x-auto bg-surface-container-low border border-outline-variant/10 shadow-sm">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant bg-surface-container-high/60 border-b border-outline-variant/10">
                <th class="py-4 px-5 text-left">Định dạng</th>
                <th class="py-4 px-5 text-center">Phụ thu ngày thường (T2–T5)</th>
                <th class="py-4 px-5 text-center">Phụ thu cuối tuần & lễ</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
              <tr v-for="f in formats" :key="f.id" class="hover:bg-white/[0.02] transition-colors">
                <td class="py-3.5 px-5 font-bold text-on-surface">
                  <div class="flex items-center gap-2.5">
                    <span class="material-symbols-outlined text-base text-primary">movie</span>
                    <span>{{ f.name }}</span>
                  </div>
                </td>
                <td class="py-3.5 px-5 text-center align-top">
                  <div class="inline-flex flex-col items-center">
                    <div class="inline-flex items-center relative group">
                      <input
                        type="text"
                        inputmode="numeric"
                        :value="fmtThousand(f.surcharge)"
                        @input="onFormatSurchargeInput($event, f, 'surcharge')"
                        class="w-36 bg-surface-container-high border py-2 pl-3 pr-7 text-right font-bold outline-none transition-all tabular-nums text-sm shadow-sm"
                        :class="formatErrors[`${f.id}_surcharge`] ? 'border-red-500 text-red-400 bg-red-500/10 focus:border-red-500 focus:ring-1 focus:ring-red-500' : 'border-outline-variant/20 text-on-surface group-hover:border-outline-variant/40 focus:border-primary focus:ring-1 focus:ring-primary'"
                      />
                      <span class="absolute right-2.5 text-xs font-bold pointer-events-none select-none" :class="formatErrors[`${f.id}_surcharge`] ? 'text-red-400' : 'text-on-surface-variant/60 group-focus-within:text-primary'">đ</span>
                    </div>
                    <span v-if="formatErrors[`${f.id}_surcharge`]" class="text-[10px] font-bold text-red-400 mt-1 max-w-[140px] leading-tight text-center" :title="formatErrors[`${f.id}_surcharge`]">
                      {{ formatErrors[`${f.id}_surcharge`] }}
                    </span>
                  </div>
                </td>
                <td class="py-3.5 px-5 text-center align-top">
                  <div class="inline-flex flex-col items-center">
                    <div class="inline-flex items-center relative group">
                      <input
                        type="text"
                        inputmode="numeric"
                        :value="f.weekendSurcharge != null ? fmtThousand(f.weekendSurcharge) : ''"
                        @input="onFormatSurchargeInput($event, f, 'weekendSurcharge')"
                        placeholder="= ngày thường"
                        class="w-40 bg-surface-container-high border py-2 pl-3 pr-7 text-right font-bold outline-none transition-all tabular-nums text-sm placeholder:text-on-surface-variant/40 placeholder:text-xs placeholder:font-normal shadow-sm"
                        :class="formatErrors[`${f.id}_weekendSurcharge`] ? 'border-red-500 text-red-400 bg-red-500/10 focus:border-red-500 focus:ring-1 focus:ring-red-500' : 'border-outline-variant/20 text-on-surface group-hover:border-outline-variant/40 focus:border-primary focus:ring-1 focus:ring-primary'"
                      />
                      <span v-if="f.weekendSurcharge != null && f.weekendSurcharge !== ''" class="absolute right-2.5 text-xs font-bold pointer-events-none select-none" :class="formatErrors[`${f.id}_weekendSurcharge`] ? 'text-red-400' : 'text-on-surface-variant/60 group-focus-within:text-primary'">đ</span>
                    </div>
                    <span v-if="formatErrors[`${f.id}_weekendSurcharge`]" class="text-[10px] font-bold text-red-400 mt-1 max-w-[160px] leading-tight text-center" :title="formatErrors[`${f.id}_weekendSurcharge`]">
                      {{ formatErrors[`${f.id}_weekendSurcharge`] }}
                    </span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs text-on-surface-variant">
          <p class="flex items-center gap-1.5">
            <span class="material-symbols-outlined text-sm text-primary">info</span>
            * Phụ thu cuối tuần & lễ để trống sẽ tự động áp dụng bằng mức phụ thu ngày thường.
          </p>
          <p class="text-on-surface-variant/80">
            Quy định: Số nguyên từ 0 đ – 1.000.000 đ, bội số của 1.000 đ. Cuối tuần ≥ ngày thường.
          </p>
        </div>
        <div>
          <button v-if="can('pricing', 'edit')" @click="saveFormats" :disabled="saving" class="px-6 py-3 bg-primary hover:bg-primary/90 text-on-primary font-bold uppercase tracking-wider text-xs shadow-lg shadow-primary/20 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center gap-2 disabled:opacity-60 disabled:pointer-events-none">
            <span class="material-symbols-outlined text-base">{{ saving ? 'sync' : 'save' }}</span>
            {{ saving ? 'Đang lưu...' : 'Lưu định dạng' }}
          </button>
        </div>
      </section>

      <!-- TAB: Ngày lễ -->
      <section v-else-if="activeTab === 'holiday'" class="space-y-4 max-w-xl">
        <p class="text-sm text-on-surface-variant">Suất rơi vào ngày lễ áp bậc giá "Cao điểm" + phụ thu định dạng cuối tuần/lễ.</p>
        <div class="flex flex-wrap items-end gap-3 bg-surface-container-low border border-outline-variant/10 p-4">
          <div>
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Ngày</label>
            <input type="date" v-model="newHoliday.holidayDate" class="bg-surface-container-high border border-outline-variant/20 p-2 text-on-surface outline-none" />
          </div>
          <div class="flex-1 min-w-[160px]">
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Tên</label>
            <input type="text" v-model="newHoliday.name" placeholder="Vd: Tết Dương lịch" class="w-full bg-surface-container-high border border-outline-variant/20 p-2 text-on-surface outline-none" />
          </div>
          <button v-if="can('pricing', 'edit')" @click="addHoliday" class="px-5 py-2 bg-primary text-on-primary font-bold">Thêm</button>
        </div>

        <div v-if="!holidays.length" class="text-center text-on-surface-variant py-8">Chưa có ngày lễ nào.</div>
        <div v-for="h in holidays" :key="h.id" class="flex items-center justify-between bg-surface-container-low border border-outline-variant/10 px-4 py-3">
          <div><span class="font-bold text-on-surface">{{ h.holidayDate }}</span> — <span class="text-on-surface-variant">{{ h.name }}</span></div>
          <button v-if="can('pricing', 'edit')" @click="removeHoliday(h)" class="p-2 text-on-surface-variant hover:text-red-500 transition-colors"><span class="material-symbols-outlined text-lg">delete</span></button>
        </div>
      </section>

      <!-- TAB: Tính thử -->
      <section v-else-if="activeTab === 'sim'" class="grid md:grid-cols-2 gap-6 max-w-3xl">
        <div class="space-y-3 bg-surface-container-low border border-outline-variant/10 p-6">
          <div>
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Loại ngày</label>
            <select v-model="sim.dayType" class="w-full bg-surface-container-high border border-outline-variant/20 p-2.5 text-on-surface outline-none">
              <option v-for="d in config.dayTypes" :key="d.code" :value="d.code">{{ d.label }}</option>
            </select>
          </div>
          <div>
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Đối tượng</label>
            <select v-model="sim.audienceType" class="w-full bg-surface-container-high border border-outline-variant/20 p-2.5 text-on-surface outline-none">
              <option v-for="[code, label] in audienceEntries" :key="code" :value="code">{{ label }}</option>
            </select>
          </div>
          <div>
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Loại phòng</label>
            <select v-model="sim.roomType" class="w-full bg-surface-container-high border border-outline-variant/20 p-2.5 text-on-surface outline-none">
              <option v-for="rt in roomTypes" :key="rt.code" :value="rt.code">{{ rt.label }}</option>
            </select>
          </div>
          <div>
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Định dạng</label>
            <select v-model="sim.formatId" class="w-full bg-surface-container-high border border-outline-variant/20 p-2.5 text-on-surface outline-none">
              <option v-for="f in formats" :key="f.id" :value="f.id">{{ f.name }}</option>
            </select>
          </div>
          <button @click="runSimulate" :disabled="simulating" class="w-full px-6 py-3 bg-primary text-on-primary font-bold disabled:opacity-60">
            {{ simulating ? 'Đang tính...' : 'Tính thử giá' }}
          </button>
        </div>

        <div class="bg-primary/10 border border-primary/20 p-6 flex flex-col justify-center relative overflow-hidden">
          <template v-if="simResult">
            <span class="text-[10px] font-bold uppercase tracking-widest text-primary">Giá vé tính được</span>
            <div class="text-4xl font-black font-headline text-primary my-3">{{ fmt(simResult.total) }} <span class="text-2xl font-bold">đ</span></div>
            <div class="text-sm text-on-surface-variant space-y-2 border-t border-primary/20 pt-3">
              <div class="flex justify-between items-center">
                <span>Giá nền:</span>
                <span class="font-bold text-on-surface">{{ fmt(simResult.basePrice) }} đ</span>
              </div>
              <div class="flex justify-between items-center">
                <span>Phụ thu định dạng:</span>
                <span class="font-bold text-on-surface">+{{ fmt(simResult.formatSurcharge) }} đ</span>
              </div>
            </div>
          </template>
          <div v-else class="text-center text-on-surface-variant py-6">
            <span class="material-symbols-outlined text-4xl mb-2 text-primary/60">calculate</span>
            <p class="text-sm">Chọn thông số và bấm "Tính thử giá".</p>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
