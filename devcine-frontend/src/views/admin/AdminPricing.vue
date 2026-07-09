<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { pricingApi } from '@/api/admin'
import { useConfirmStore } from '@/stores/confirm'
import { useAdminPerm } from '@/composables/useAdminPerm'

const { can } = useAdminPerm()
const confirm = useConfirmStore()

const loading = ref(true)
const loadError = ref(false)
const saving = ref(false)
const activeTab = ref('base')

const config = ref(null)
const baseMatrix = reactive({})       // key `${day}|${aud}` -> value (Lotte: không theo giờ)
const seatTypes = ref([])
const formats = ref([])
const specialPrices = reactive({})    // key `${formatId}|${seatTypeId}` -> price
const holidays = ref([])
const newHoliday = reactive({ holidayDate: '', name: '' })

// Simulator (Lotte: không có khung giờ)
const sim = reactive({ dayType: 'WEEKEND', audienceType: 'ADULT', seatTypeId: '', formatId: '' })
const simResult = ref(null)
const simulating = ref(false)

// Toast
const toast = ref({ show: false, type: 'success', message: '' })
let toastTimer = null
const showToast = (message, type = 'success') => {
  toast.value = { show: true, type, message }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value.show = false }, 3500)
}

const fmt = (n) => Number(n || 0).toLocaleString('vi-VN')
const audienceEntries = computed(() => Object.entries(config.value?.audiences || {}))
const fixedFormats = computed(() => formats.value.filter(f => f.isFixedPrice))

const loadConfig = async () => {
  loading.value = true
  loadError.value = false
  try {
    const { data } = await pricingApi.getConfig()
    config.value = data

    Object.keys(baseMatrix).forEach(k => delete baseMatrix[k])
    const existing = {}
    ;(data.baseMatrix || []).forEach(r => { existing[`${r.dayType}|${r.audienceType}`] = r.value })
    data.dayTypes.forEach(d => Object.keys(data.audiences).forEach(a => {
      const key = `${d.code}|${a}`
      baseMatrix[key] = existing[key] ?? 0
    }))

    seatTypes.value = (data.seatTypes || []).map(s => ({ ...s, priceModifier: Number(s.priceModifier || 0) }))
    formats.value = (data.formats || []).map(f => ({
      ...f,
      surcharge: Number(f.surcharge || 0),
      weekendSurcharge: f.weekendSurcharge == null ? null : Number(f.weekendSurcharge),
      isFixedPrice: !!f.isFixedPrice
    }))

    Object.keys(specialPrices).forEach(k => delete specialPrices[k])
    ;(data.specialPrices || []).forEach(sp => { specialPrices[`${sp.formatId}|${sp.seatTypeId}`] = Number(sp.price || 0) })

    holidays.value = data.holidays || []
    if (seatTypes.value.length) sim.seatTypeId = seatTypes.value[0].id
    if (formats.value.length) sim.formatId = formats.value[0].id
  } catch (e) {
    console.error('Lỗi tải cấu hình giá', e)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(loadConfig)
onUnmounted(() => { if (toastTimer) clearTimeout(toastTimer) })

const saveBase = async () => {
  saving.value = true
  try {
    const rules = Object.entries(baseMatrix).map(([k, value]) => {
      const [dayType, audienceType] = k.split('|')
      return { dayType, timeSlot: 'ALL', audienceType, value: Number(value || 0) }
    })
    await pricingApi.saveBaseMatrix(rules)
    showToast('Đã lưu bảng giá nền.')
  } catch (e) {
    showToast(e.response?.data?.message || 'Lưu giá nền thất bại.', 'error')
  } finally { saving.value = false }
}

const saveSeats = async () => {
  saving.value = true
  try {
    await pricingApi.saveSeatTypes(seatTypes.value.map(s => ({ id: s.id, priceModifier: Number(s.priceModifier || 0) })))
    showToast('Đã lưu phụ thu loại ghế.')
  } catch (e) {
    showToast(e.response?.data?.message || 'Lưu loại ghế thất bại.', 'error')
  } finally { saving.value = false }
}

const saveFormats = async () => {
  saving.value = true
  try {
    await pricingApi.saveFormats(formats.value.map(f => ({
      id: f.id,
      surcharge: Number(f.surcharge || 0),
      weekendSurcharge: f.weekendSurcharge == null || f.weekendSurcharge === '' ? null : Number(f.weekendSurcharge),
      isFixedPrice: !!f.isFixedPrice
    })))
    showToast('Đã lưu cấu hình định dạng.')
  } catch (e) {
    showToast(e.response?.data?.message || 'Lưu định dạng thất bại.', 'error')
  } finally { saving.value = false }
}

const saveSpecials = async () => {
  saving.value = true
  try {
    const items = []
    fixedFormats.value.forEach(f => seatTypes.value.forEach(s => {
      const v = specialPrices[`${f.id}|${s.id}`]
      if (v != null && Number(v) > 0) items.push({ formatId: f.id, seatTypeId: s.id, price: Number(v) })
    }))
    await pricingApi.saveSpecialPrices(items)
    showToast('Đã lưu giá phòng đặc biệt.')
  } catch (e) {
    showToast(e.response?.data?.message || 'Lưu giá đặc biệt thất bại.', 'error')
  } finally { saving.value = false }
}

const addHoliday = async () => {
  if (!newHoliday.holidayDate || !newHoliday.name.trim()) {
    showToast('Nhập đủ ngày và tên ngày lễ.', 'error'); return
  }
  try {
    await pricingApi.addHoliday(newHoliday.holidayDate, newHoliday.name.trim())
    newHoliday.holidayDate = ''; newHoliday.name = ''
    await loadConfig()
    showToast('Đã thêm ngày lễ.')
  } catch (e) {
    showToast(e.response?.data?.message || 'Thêm ngày lễ thất bại.', 'error')
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
    showToast('Đã xoá ngày lễ.')
  } catch (e) {
    showToast('Xoá ngày lễ thất bại.', 'error')
  }
}

const runSimulate = async () => {
  simulating.value = true
  simResult.value = null
  try {
    const { data } = await pricingApi.simulate({ ...sim, timeSlot: 'ALL' })
    simResult.value = data
  } catch (e) {
    showToast(e.response?.data?.message || 'Tính thử thất bại.', 'error')
  } finally { simulating.value = false }
}

const TABS = [
  { key: 'base', label: 'Giá nền', icon: 'grid_on' },
  { key: 'seat', label: 'Loại ghế', icon: 'chair' },
  { key: 'format', label: 'Định dạng', icon: 'movie' },
  { key: 'holiday', label: 'Ngày lễ', icon: 'event' },
  { key: 'sim', label: 'Tính thử', icon: 'calculate' },
]
</script>

<template>
  <div class="p-6 md:p-10 space-y-6">
    <header>
      <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight font-headline uppercase text-primary">Cấu hình giá vé</h1>
      <p class="text-on-surface-variant text-sm mt-1">Giá = giá nền (đối tượng × ngày) + phụ thu định dạng (theo ngày). Phòng đặc biệt dùng giá cố định.</p>
    </header>

    <div v-if="loading" class="space-y-3">
      <div v-for="i in 5" :key="i" class="h-12 bg-white/5 rounded-lg animate-pulse"></div>
    </div>

    <div v-else-if="loadError" class="bg-red-500/10 border border-red-500/30 rounded-xl p-8 text-center">
      <span class="material-symbols-outlined text-4xl text-red-400 mb-2">error</span>
      <p class="text-on-surface-variant mb-4">Không tải được cấu hình giá.</p>
      <button @click="loadConfig" class="px-5 py-2 bg-primary text-on-primary rounded-lg font-bold">Thử lại</button>
    </div>

    <template v-else>
      <div class="flex flex-wrap gap-2 border-b border-outline-variant/10">
        <button v-for="t in TABS" :key="t.key" @click="activeTab = t.key"
          :class="activeTab === t.key ? 'border-primary text-primary' : 'border-transparent text-on-surface-variant hover:text-on-surface'"
          class="flex items-center gap-2 px-4 py-3 border-b-2 font-bold text-sm transition-colors">
          <span class="material-symbols-outlined text-lg">{{ t.icon }}</span>{{ t.label }}
        </button>
      </div>

      <!-- TAB: Giá nền (ngày × đối tượng) -->
      <section v-if="activeTab === 'base'" class="space-y-4">
        <div class="overflow-x-auto bg-surface-container-low border border-outline-variant/10 rounded-xl">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant bg-white/5">
                <th class="p-4 text-left">Loại ngày \ Đối tượng</th>
                <th v-for="[code, label] in audienceEntries" :key="code" class="p-4 text-center">{{ label }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in config.dayTypes" :key="d.code" class="border-t border-outline-variant/5">
                <td class="p-4 font-bold text-on-surface">{{ d.label }}</td>
                <td v-for="[code] in audienceEntries" :key="code" class="p-3 text-center">
                  <input type="number" v-model.number="baseMatrix[`${d.code}|${code}`]"
                    class="w-28 bg-surface-container-high border border-outline-variant/20 p-2 rounded text-right font-bold text-on-surface focus:border-primary outline-none" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="text-xs text-on-surface-variant">* Giá ngày lễ nên đã gồm phụ thu lễ. Suất rơi vào ngày trong tab "Ngày lễ" sẽ dùng cột giá Lễ.</p>
        <button v-if="can('pricing', 'edit')" @click="saveBase" :disabled="saving" class="px-6 py-3 bg-primary text-on-primary rounded-lg font-bold disabled:opacity-60">
          {{ saving ? 'Đang lưu...' : 'Lưu giá nền' }}
        </button>
      </section>

      <!-- TAB: Loại ghế -->
      <section v-else-if="activeTab === 'seat'" class="space-y-4 max-w-xl">
        <p class="text-sm text-on-surface-variant">Phụ thu cộng vào giá nền theo loại ghế. Theo biểu giá Lotte: tất cả = 0 (không phân biệt giá ghế).</p>
        <div v-for="s in seatTypes" :key="s.id" class="flex items-center justify-between bg-surface-container-low border border-outline-variant/10 rounded-xl p-4">
          <span class="font-bold text-on-surface">{{ s.name }}</span>
          <div class="flex items-center gap-2">
            <span class="text-on-surface-variant text-sm">+</span>
            <input type="number" v-model.number="s.priceModifier" class="w-36 bg-surface-container-high border border-outline-variant/20 p-2 rounded text-right font-bold text-on-surface focus:border-primary outline-none" />
            <span class="text-on-surface-variant text-sm">đ</span>
          </div>
        </div>
        <button v-if="can('pricing', 'edit')" @click="saveSeats" :disabled="saving" class="px-6 py-3 bg-primary text-on-primary rounded-lg font-bold disabled:opacity-60">
          {{ saving ? 'Đang lưu...' : 'Lưu phụ thu ghế' }}
        </button>
      </section>

      <!-- TAB: Định dạng -->
      <section v-else-if="activeTab === 'format'" class="space-y-4">
        <div class="overflow-x-auto bg-surface-container-low border border-outline-variant/10 rounded-xl">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant bg-white/5">
                <th class="p-4 text-left">Định dạng</th>
                <th class="p-4 text-center">Phụ thu ngày thường (T2–T5)</th>
                <th class="p-4 text-center">Phụ thu cuối tuần & lễ</th>
                <th class="p-4 text-center">Giá cố định?</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="f in formats" :key="f.id" class="border-t border-outline-variant/5">
                <td class="p-4 font-bold text-on-surface">{{ f.name }}</td>
                <td class="p-3 text-center" :class="{ 'opacity-40': f.isFixedPrice }">
                  <input type="number" v-model.number="f.surcharge" :disabled="f.isFixedPrice" class="w-28 bg-surface-container-high border border-outline-variant/20 p-2 rounded text-right font-bold text-on-surface focus:border-primary outline-none" />
                </td>
                <td class="p-3 text-center" :class="{ 'opacity-40': f.isFixedPrice }">
                  <input type="number" v-model.number="f.weekendSurcharge" :disabled="f.isFixedPrice" placeholder="= ngày thường" class="w-28 bg-surface-container-high border border-outline-variant/20 p-2 rounded text-right font-bold text-on-surface focus:border-primary outline-none" />
                </td>
                <td class="p-3 text-center">
                  <input type="checkbox" v-model="f.isFixedPrice" class="accent-primary w-4 h-4" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <button v-if="can('pricing', 'edit')" @click="saveFormats" :disabled="saving" class="px-6 py-3 bg-primary text-on-primary rounded-lg font-bold disabled:opacity-60">
          {{ saving ? 'Đang lưu...' : 'Lưu định dạng' }}
        </button>

        <!-- Giá cố định phòng đặc biệt -->
        <div v-if="fixedFormats.length" class="mt-6 space-y-3">
          <h3 class="font-bold text-on-surface uppercase tracking-widest text-xs">Giá cố định phòng đặc biệt</h3>
          <div class="overflow-x-auto bg-surface-container-low border border-outline-variant/10 rounded-xl">
            <table class="w-full text-sm">
              <thead>
                <tr class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant bg-white/5">
                  <th class="p-4 text-left">Định dạng \ Loại ghế</th>
                  <th v-for="s in seatTypes" :key="s.id" class="p-4 text-center">{{ s.name }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="f in fixedFormats" :key="f.id" class="border-t border-outline-variant/5">
                  <td class="p-4 font-bold text-on-surface">{{ f.name }}</td>
                  <td v-for="s in seatTypes" :key="s.id" class="p-3 text-center">
                    <input type="number" v-model.number="specialPrices[`${f.id}|${s.id}`]"
                      class="w-28 bg-surface-container-high border border-outline-variant/20 p-2 rounded text-right font-bold text-on-surface focus:border-primary outline-none" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <button v-if="can('pricing', 'edit')" @click="saveSpecials" :disabled="saving" class="px-6 py-3 bg-primary text-on-primary rounded-lg font-bold disabled:opacity-60">
            {{ saving ? 'Đang lưu...' : 'Lưu giá đặc biệt' }}
          </button>
        </div>
      </section>

      <!-- TAB: Ngày lễ -->
      <section v-else-if="activeTab === 'holiday'" class="space-y-4 max-w-xl">
        <p class="text-sm text-on-surface-variant">Suất rơi vào ngày lễ áp cột giá "Ngày lễ" + phụ thu định dạng cuối tuần/lễ.</p>
        <div class="flex flex-wrap items-end gap-3 bg-surface-container-low border border-outline-variant/10 rounded-xl p-4">
          <div>
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Ngày</label>
            <input type="date" v-model="newHoliday.holidayDate" class="bg-surface-container-high border border-outline-variant/20 p-2 rounded text-on-surface outline-none" />
          </div>
          <div class="flex-1 min-w-[160px]">
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Tên</label>
            <input type="text" v-model="newHoliday.name" placeholder="Vd: Tết Dương lịch" class="w-full bg-surface-container-high border border-outline-variant/20 p-2 rounded text-on-surface outline-none" />
          </div>
          <button v-if="can('pricing', 'edit')" @click="addHoliday" class="px-5 py-2 bg-primary text-on-primary rounded-lg font-bold">Thêm</button>
        </div>

        <div v-if="!holidays.length" class="text-center text-on-surface-variant py-8">Chưa có ngày lễ nào.</div>
        <div v-for="h in holidays" :key="h.id" class="flex items-center justify-between bg-surface-container-low border border-outline-variant/10 rounded-xl px-4 py-3">
          <div><span class="font-bold text-on-surface">{{ h.holidayDate }}</span> — <span class="text-on-surface-variant">{{ h.name }}</span></div>
          <button v-if="can('pricing', 'edit')" @click="removeHoliday(h)" class="p-2 text-on-surface-variant hover:text-red-500 transition-colors"><span class="material-symbols-outlined text-lg">delete</span></button>
        </div>
      </section>

      <!-- TAB: Tính thử -->
      <section v-else-if="activeTab === 'sim'" class="grid md:grid-cols-2 gap-6 max-w-3xl">
        <div class="space-y-3 bg-surface-container-low border border-outline-variant/10 rounded-xl p-6">
          <div>
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Loại ngày</label>
            <select v-model="sim.dayType" class="w-full bg-surface-container-high border border-outline-variant/20 p-2.5 rounded text-on-surface outline-none">
              <option v-for="d in config.dayTypes" :key="d.code" :value="d.code">{{ d.label }}</option>
            </select>
          </div>
          <div>
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Đối tượng</label>
            <select v-model="sim.audienceType" class="w-full bg-surface-container-high border border-outline-variant/20 p-2.5 rounded text-on-surface outline-none">
              <option v-for="[code, label] in audienceEntries" :key="code" :value="code">{{ label }}</option>
            </select>
          </div>
          <div>
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Loại ghế</label>
            <select v-model="sim.seatTypeId" class="w-full bg-surface-container-high border border-outline-variant/20 p-2.5 rounded text-on-surface outline-none">
              <option v-for="s in seatTypes" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div>
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Định dạng</label>
            <select v-model="sim.formatId" class="w-full bg-surface-container-high border border-outline-variant/20 p-2.5 rounded text-on-surface outline-none">
              <option v-for="f in formats" :key="f.id" :value="f.id">{{ f.name }}</option>
            </select>
          </div>
          <button @click="runSimulate" :disabled="simulating" class="w-full px-6 py-3 bg-primary text-on-primary rounded-lg font-bold disabled:opacity-60">
            {{ simulating ? 'Đang tính...' : 'Tính thử giá' }}
          </button>
        </div>

        <div class="bg-primary/10 border border-primary/20 rounded-xl p-6 flex flex-col justify-center">
          <template v-if="simResult">
            <span class="text-[10px] font-bold uppercase tracking-widest text-primary">Giá vé tính được</span>
            <div class="text-4xl font-black font-headline text-primary mb-4">{{ fmt(simResult.total) }}đ</div>
            <div v-if="simResult.fixedPrice" class="text-sm text-on-surface-variant">Giá cố định (phòng đặc biệt).</div>
            <div v-else class="text-sm text-on-surface-variant space-y-1">
              <div class="flex justify-between"><span>Giá nền</span><span class="font-bold text-on-surface">{{ fmt(simResult.basePrice) }}đ</span></div>
              <div class="flex justify-between"><span>Phụ thu ghế</span><span class="font-bold text-on-surface">+{{ fmt(simResult.seatSurcharge) }}đ</span></div>
              <div class="flex justify-between"><span>Phụ thu định dạng</span><span class="font-bold text-on-surface">+{{ fmt(simResult.formatSurcharge) }}đ</span></div>
            </div>
          </template>
          <div v-else class="text-center text-on-surface-variant">
            <span class="material-symbols-outlined text-4xl mb-2">calculate</span>
            <p class="text-sm">Chọn thông số và bấm "Tính thử giá".</p>
          </div>
        </div>
      </section>
    </template>

    <transition name="fade">
      <div v-if="toast.show" :class="[
        'fixed bottom-6 right-6 z-[1100] px-5 py-3 rounded-xl shadow-2xl text-sm font-semibold flex items-center gap-2 border',
        toast.type === 'success' ? 'bg-green-500/15 border-green-500/30 text-green-300' : 'bg-red-500/15 border-red-500/30 text-red-300'
      ]">
        <span class="material-symbols-outlined text-base">{{ toast.type === 'success' ? 'check_circle' : 'error' }}</span>
        {{ toast.message }}
      </div>
    </transition>
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
