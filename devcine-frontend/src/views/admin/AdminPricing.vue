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
const audienceEntries = computed(() => Object.entries(config.value?.audiences || {}))
const roomTypes = computed(() => config.value?.roomTypes || [])

const loadConfig = async () => {
  loading.value = true
  loadError.value = false
  try {
    const { data } = await pricingApi.getConfig()
    config.value = data

    Object.keys(baseMatrix).forEach(k => delete baseMatrix[k])
    const existing = {}
    ;(data.baseMatrix || []).forEach(r => { existing[`${r.roomType}|${r.dayType}|${r.audienceType}`] = r.value })
    ;(data.roomTypes || []).forEach(rt => data.dayTypes.forEach(d => Object.keys(data.audiences).forEach(a => {
      const key = `${rt.code}|${d.code}|${a}`
      baseMatrix[key] = existing[key] ?? 0
    })))

    formats.value = (data.formats || []).map(f => ({
      ...f,
      surcharge: Number(f.surcharge || 0),
      weekendSurcharge: f.weekendSurcharge == null ? null : Number(f.weekendSurcharge),
    }))

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

const saveBase = async () => {
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

const saveFormats = async () => {
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

      <!-- TAB: Giá nền (loại phòng × loại ngày × đối tượng) -->
      <section v-if="activeTab === 'base'" class="space-y-6">
        <div v-for="rt in roomTypes" :key="rt.code" class="space-y-2">
          <h3 class="font-bold text-on-surface uppercase tracking-widest text-xs flex items-center gap-2">
            <span class="material-symbols-outlined text-base text-primary">meeting_room</span>{{ rt.label }}
          </h3>
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
                    <input type="number" v-model.number="baseMatrix[`${rt.code}|${d.code}|${code}`]"
                      class="w-28 bg-surface-container-high border border-outline-variant/20 p-2 rounded text-right font-bold text-on-surface focus:border-primary outline-none" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <p class="text-xs text-on-surface-variant">* Bậc "Cao điểm" gộp T6, T7, CN và mọi ngày trong tab "Ngày lễ".</p>
        <button v-if="can('pricing', 'edit')" @click="saveBase" :disabled="saving" class="px-6 py-3 bg-primary text-on-primary rounded-lg font-bold disabled:opacity-60">
          {{ saving ? 'Đang lưu...' : 'Lưu giá nền' }}
        </button>
      </section>

      <!-- TAB: Định dạng (phụ thu công nghệ 2D/3D) -->
      <section v-else-if="activeTab === 'format'" class="space-y-4">
        <p class="text-sm text-on-surface-variant">Phụ thu công nghệ định dạng, cộng vào giá nền. Giá theo hạng phòng đã cấu hình ở tab "Giá nền".</p>
        <div class="overflow-x-auto bg-surface-container-low border border-outline-variant/10 rounded-xl">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant bg-white/5">
                <th class="p-4 text-left">Định dạng</th>
                <th class="p-4 text-center">Phụ thu ngày thường (T2–T5)</th>
                <th class="p-4 text-center">Phụ thu cuối tuần & lễ</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="f in formats" :key="f.id" class="border-t border-outline-variant/5">
                <td class="p-4 font-bold text-on-surface">{{ f.name }}</td>
                <td class="p-3 text-center">
                  <input type="number" v-model.number="f.surcharge" class="w-28 bg-surface-container-high border border-outline-variant/20 p-2 rounded text-right font-bold text-on-surface focus:border-primary outline-none" />
                </td>
                <td class="p-3 text-center">
                  <input type="number" v-model.number="f.weekendSurcharge" placeholder="= ngày thường" class="w-28 bg-surface-container-high border border-outline-variant/20 p-2 rounded text-right font-bold text-on-surface focus:border-primary outline-none" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <button v-if="can('pricing', 'edit')" @click="saveFormats" :disabled="saving" class="px-6 py-3 bg-primary text-on-primary rounded-lg font-bold disabled:opacity-60">
          {{ saving ? 'Đang lưu...' : 'Lưu định dạng' }}
        </button>
      </section>

      <!-- TAB: Ngày lễ -->
      <section v-else-if="activeTab === 'holiday'" class="space-y-4 max-w-xl">
        <p class="text-sm text-on-surface-variant">Suất rơi vào ngày lễ áp bậc giá "Cao điểm" + phụ thu định dạng cuối tuần/lễ.</p>
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
            <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant block mb-1">Loại phòng</label>
            <select v-model="sim.roomType" class="w-full bg-surface-container-high border border-outline-variant/20 p-2.5 rounded text-on-surface outline-none">
              <option v-for="rt in roomTypes" :key="rt.code" :value="rt.code">{{ rt.label }}</option>
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
            <div class="text-sm text-on-surface-variant space-y-1">
              <div class="flex justify-between"><span>Giá nền</span><span class="font-bold text-on-surface">{{ fmt(simResult.basePrice) }}đ</span></div>
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
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
