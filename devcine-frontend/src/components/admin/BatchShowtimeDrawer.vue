<script setup>
import { ref, reactive, computed, watch, nextTick } from 'vue';
import api from '@/api/axios';
import CustomSelect from './CustomSelect.vue';
import { useToastStore } from '@/stores/toast';
import { friendlyError } from '@/utils/friendlyError';

const props = defineProps({
  isOpen: Boolean,
  // Danh sách cụm rạp kèm halls: [{ id, name, city, halls: [{id,name,type}] }]
  cinemas: { type: Array, default: () => [] }
});
const emit = defineEmits(['close', 'saved']);

const toast = useToastStore();

const form = reactive({
  movieId: '',
  formatId: '',
  cleaningTime: 15,
  dateFrom: '',
  dateTo: '',
  daysOfWeek: [],        // ISO 1..7 (rỗng = mọi ngày)
  roomIds: [],           // các phòng đã tick (nhiều cơ sở)
  startTimes: []         // "HH:mm"
});

const movies = ref([]);
const formats = ref([]);
const newTime = ref('');
const preview = ref(null);   // { toCreate, createdCount, skipped: [] }
const isBusy = ref(false);

// Lỗi theo TỪNG trường (thay banner lỗi tổng ở đầu drawer) — hiện inline + tự cuộn tới lỗi đầu tiên
const fieldErrors = reactive({ movieId: '', formatId: '', roomIds: '', dateRange: '', startTimes: '' });
const movieField = ref(null);
const formatField = ref(null);
const roomsField = ref(null);
const dateField = ref(null);
const timesField = ref(null);
const clearErrors = () => Object.keys(fieldErrors).forEach(k => { fieldErrors[k] = ''; });

const weekDays = [
  { value: 1, label: 'T2' }, { value: 2, label: 'T3' }, { value: 3, label: 'T4' },
  { value: 4, label: 'T5' }, { value: 5, label: 'T6' }, { value: 6, label: 'T7' },
  { value: 7, label: 'CN' }
];

const movieOptions = computed(() => movies.value.map(m => ({ value: m.id, label: m.title || m.name })));

// Định dạng lọc theo phim đã chọn (giống ShowtimeDrawer)
const formatOptions = computed(() => {
  if (!form.movieId) return [];
  const movie = movies.value.find(m => m.id === form.movieId);
  if (!movie) return [];
  const supportedStr = movie.supportedFormats || movie.format || '';
  if (!supportedStr) return formats.value.map(f => ({ value: f.id, label: f.name }));
  const supported = supportedStr.split(',').map(s => s.trim().toUpperCase());
  return formats.value
    .filter(f => supported.some(sup => f.name.toUpperCase().includes(sup)))
    .map(f => ({ value: f.id, label: f.name }));
});

const totalRoomsSelected = computed(() => form.roomIds.length);

const fetchOptions = async () => {
  try {
    const [moviesRes, formatsRes] = await Promise.all([api.get('/movies'), api.get('/formats')]);
    movies.value = moviesRes.data;
    formats.value = formatsRes.data;
  } catch (e) {
    console.error('Error fetching options', e);
  }
};

watch(() => form.movieId, () => { form.formatId = ''; });

watch(() => props.isOpen, (open) => {
  if (open) {
    fetchOptions();
    clearErrors();
    preview.value = null;
    const today = new Date().toISOString().slice(0, 10);
    form.movieId = ''; form.formatId = ''; form.cleaningTime = 15;
    form.dateFrom = today; form.dateTo = today;
    form.daysOfWeek = []; form.roomIds = []; form.startTimes = [];
  }
});

// Đổi bất kỳ tham số nào → preview cũ không còn đúng
watch(() => [form.movieId, form.formatId, form.cleaningTime, form.dateFrom, form.dateTo,
  form.daysOfWeek.length, form.roomIds.length, form.startTimes.length], () => { preview.value = null; });

// Lỗi tự xóa khi người dùng bắt đầu sửa đúng trường đó (đỡ cảm giác lỗi dai)
watch(() => form.movieId, () => { fieldErrors.movieId = ''; });
watch(() => form.formatId, () => { fieldErrors.formatId = ''; });
watch(() => form.roomIds.length, () => { fieldErrors.roomIds = ''; });
watch(() => [form.dateFrom, form.dateTo], () => { fieldErrors.dateRange = ''; });
watch(() => form.startTimes.length, () => { fieldErrors.startTimes = ''; });

const toggleRoom = (roomId) => {
  const i = form.roomIds.indexOf(roomId);
  if (i === -1) form.roomIds.push(roomId); else form.roomIds.splice(i, 1);
};
const cinemaAllSelected = (cinema) => (cinema.halls || []).length > 0
  && cinema.halls.every(h => form.roomIds.includes(h.id));
const toggleCinema = (cinema) => {
  const ids = (cinema.halls || []).map(h => h.id);
  if (cinemaAllSelected(cinema)) {
    form.roomIds = form.roomIds.filter(id => !ids.includes(id));
  } else {
    ids.forEach(id => { if (!form.roomIds.includes(id)) form.roomIds.push(id); });
  }
};

const toggleDay = (d) => {
  const i = form.daysOfWeek.indexOf(d);
  if (i === -1) form.daysOfWeek.push(d); else form.daysOfWeek.splice(i, 1);
};

const addTime = () => {
  const t = (newTime.value || '').trim();
  if (!t) return;
  if (!form.startTimes.includes(t)) form.startTimes.push(t);
  form.startTimes.sort();
  newTime.value = '';
};
const removeTime = (t) => { form.startTimes = form.startTimes.filter(x => x !== t); };

// Trả về key của trường lỗi ĐẦU TIÊN (theo thứ tự hiển thị) hoặc null nếu hợp lệ.
const validate = () => {
  clearErrors();
  if (!form.movieId) fieldErrors.movieId = 'Vui lòng chọn phim.';
  if (!form.formatId) fieldErrors.formatId = 'Vui lòng chọn định dạng.';
  if (!form.roomIds.length) fieldErrors.roomIds = 'Vui lòng chọn ít nhất một phòng chiếu.';
  if (!form.dateFrom || !form.dateTo) fieldErrors.dateRange = 'Vui lòng chọn khoảng ngày.';
  else if (form.dateFrom > form.dateTo) fieldErrors.dateRange = 'Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.';
  if (!form.startTimes.length) fieldErrors.startTimes = 'Vui lòng thêm ít nhất một khung giờ.';

  const order = [
    ['movieId', movieField], ['formatId', formatField], ['roomIds', roomsField],
    ['dateRange', dateField], ['startTimes', timesField]
  ];
  const first = order.find(([k]) => fieldErrors[k]);
  return first ? first[1] : null;
};

// Cuộn trường lỗi vào giữa màn hình + focus phần tử nhập đầu tiên bên trong.
const focusFirstError = async (fieldRef) => {
  await nextTick();
  const el = fieldRef?.value;
  if (!el) return;
  el.scrollIntoView({ behavior: 'smooth', block: 'center' });
  el.querySelector('input, button, select, [tabindex]')?.focus?.();
};

const buildPayload = (dryRun) => ({
  movieId: parseInt(form.movieId),
  formatId: parseInt(form.formatId),
  cleaningTime: parseInt(form.cleaningTime) || 15,
  roomIds: [...form.roomIds],
  dateFrom: form.dateFrom,
  dateTo: form.dateTo,
  daysOfWeek: [...form.daysOfWeek],
  startTimes: [...form.startTimes],
  dryRun
});

const runPreview = async () => {
  const badField = validate();
  if (badField) { focusFirstError(badField); return; }
  isBusy.value = true;
  try {
    const { data } = await api.post('/showtimes/batch', buildPayload(true));
    preview.value = data;
  } catch (e) {
    toast.error(friendlyError(e, 'Không thể xem trước lịch chiếu.'));
  } finally {
    isBusy.value = false;
  }
};

const handleCreate = async () => {
  const badField = validate();
  if (badField) { focusFirstError(badField); return; }
  isBusy.value = true;
  try {
    const { data } = await api.post('/showtimes/batch', buildPayload(false));
    toast.success(`Đã tạo ${data.createdCount} suất chiếu` +
      (data.skipped?.length ? `, bỏ qua ${data.skipped.length} suất.` : '.'));
    emit('saved');
    emit('close');
  } catch (e) {
    toast.error(friendlyError(e, 'Không thể tạo lịch chiếu.'));
  } finally {
    isBusy.value = false;
  }
};
</script>

<template>
  <Transition name="fade">
    <div v-if="isOpen" class="fixed inset-0 bg-black/40 z-[140]" @click="emit('close')"></div>
  </Transition>

  <Transition name="drawer">
    <div v-if="isOpen" class="fixed inset-y-0 right-0 w-[560px] max-w-full bg-surface-container border-l border-white/10 shadow-2xl z-[150] flex flex-col">
      <!-- Header -->
      <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-black/20">
        <div>
          <h2 class="text-xl font-black text-white uppercase tracking-widest font-headline">Tạo lịch hàng loạt</h2>
          <p class="text-xs text-white/50 mt-1 uppercase tracking-widest">Nhiều cơ sở · phòng · ngày · giờ</p>
        </div>
        <button @click="emit('close')" class="w-10 h-10 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center transition-all border border-white/10">
          <span class="material-symbols-outlined text-white/70">close</span>
        </button>
      </div>

      <!-- Body -->
      <div class="flex-1 overflow-y-auto p-8 space-y-6">
        <!-- Phim + định dạng -->
        <div class="grid grid-cols-2 gap-4">
          <div ref="movieField">
            <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Phim</label>
            <CustomSelect v-model="form.movieId" :options="movieOptions" placeholder="-- Chọn phim --"
              :class="fieldErrors.movieId ? 'rounded-xl ring-1 ring-red-500/60' : ''" />
            <p v-if="fieldErrors.movieId" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.movieId }}</p>
          </div>
          <div ref="formatField">
            <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Định dạng</label>
            <CustomSelect v-model="form.formatId" :options="formatOptions" placeholder="-- Chọn định dạng --"
              :class="fieldErrors.formatId ? 'rounded-xl ring-1 ring-red-500/60' : ''" />
            <p v-if="fieldErrors.formatId" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.formatId }}</p>
          </div>
        </div>

        <!-- Cơ sở → phòng -->
        <div ref="roomsField">
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">
            Cơ sở & phòng chiếu <span class="text-primary">({{ totalRoomsSelected }} phòng)</span>
          </label>
          <div class="space-y-3 max-h-64 overflow-y-auto pr-1"
            :class="fieldErrors.roomIds ? 'ring-1 ring-red-500/60 rounded-xl p-1' : ''">
            <div v-for="c in cinemas" :key="c.id" class="bg-black/20 border border-white/10 rounded-xl p-3">
              <label class="flex items-center gap-2 cursor-pointer mb-2">
                <input type="checkbox" :checked="cinemaAllSelected(c)" @change="toggleCinema(c)"
                  class="accent-primary w-4 h-4" />
                <span class="text-sm font-bold text-white">{{ c.name }}</span>
                <span class="text-[10px] text-white/40 uppercase tracking-widest">{{ c.city }}</span>
              </label>
              <div v-if="c.halls?.length" class="flex flex-wrap gap-2 pl-6">
                <button v-for="h in c.halls" :key="h.id" type="button" @click="toggleRoom(h.id)"
                  :class="form.roomIds.includes(h.id)
                    ? 'bg-primary/20 border-primary text-primary'
                    : 'bg-white/5 border-white/10 text-white/60'"
                  class="px-3 py-1.5 rounded-lg border text-[11px] font-bold transition-all">
                  {{ h.name }}
                </button>
              </div>
              <p v-else class="pl-6 text-[11px] text-white/30 italic">Chưa có phòng</p>
            </div>
          </div>
          <p v-if="fieldErrors.roomIds" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.roomIds }}</p>
        </div>

        <!-- Khoảng ngày -->
        <div ref="dateField">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Từ ngày</label>
              <input type="date" v-model="form.dateFrom"
                :class="fieldErrors.dateRange ? 'border-red-500/60' : 'border-white/10'"
                class="w-full bg-black/20 border rounded-xl px-4 py-3 text-white outline-none focus:border-primary/50 transition-colors" />
            </div>
            <div>
              <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Đến ngày</label>
              <input type="date" v-model="form.dateTo"
                :class="fieldErrors.dateRange ? 'border-red-500/60' : 'border-white/10'"
                class="w-full bg-black/20 border rounded-xl px-4 py-3 text-white outline-none focus:border-primary/50 transition-colors" />
            </div>
          </div>
          <p v-if="fieldErrors.dateRange" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.dateRange }}</p>
        </div>

        <!-- Thứ trong tuần -->
        <div>
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">
            Thứ trong tuần <span class="text-white/30 lowercase tracking-normal">(bỏ trống = mọi ngày)</span>
          </label>
          <div class="flex gap-2">
            <button v-for="d in weekDays" :key="d.value" type="button" @click="toggleDay(d.value)"
              :class="form.daysOfWeek.includes(d.value)
                ? 'bg-primary text-on-primary border-primary'
                : 'bg-white/5 border-white/10 text-white/60'"
              class="flex-1 py-2 rounded-lg border text-[11px] font-black transition-all">
              {{ d.label }}
            </button>
          </div>
        </div>

        <!-- Khung giờ -->
        <div ref="timesField">
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Khung giờ chiếu</label>
          <div class="flex gap-2">
            <input type="time" v-model="newTime" @keyup.enter="addTime"
              :class="fieldErrors.startTimes ? 'border-red-500/60' : 'border-white/10'"
              class="flex-1 bg-black/20 border rounded-xl px-4 py-3 text-white outline-none focus:border-primary/50 transition-colors" />
            <button type="button" @click="addTime" class="px-5 rounded-xl bg-primary/20 border border-primary/40 text-primary font-bold text-xs uppercase tracking-widest hover:bg-primary/30 transition-all">Thêm</button>
          </div>
          <p v-if="fieldErrors.startTimes" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.startTimes }}</p>
          <div v-if="form.startTimes.length" class="flex flex-wrap gap-2 mt-3">
            <span v-for="t in form.startTimes" :key="t"
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10 text-white text-xs font-bold">
              {{ t }}
              <button type="button" @click="removeTime(t)" class="text-white/40 hover:text-red-400">
                <span class="material-symbols-outlined text-[14px] leading-none">close</span>
              </button>
            </span>
          </div>
        </div>

        <!-- Thời gian dọn dẹp -->
        <div>
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Thời gian dọn dẹp (phút)</label>
          <input type="number" min="0" v-model="form.cleaningTime" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-white outline-none focus:border-primary/50 transition-colors" />
        </div>

        <!-- Kết quả xem trước -->
        <div v-if="preview" class="rounded-xl border border-white/10 bg-black/20 overflow-hidden">
          <div class="p-4 border-b border-white/5 flex items-center gap-4">
            <div class="flex items-baseline gap-1.5">
              <span class="text-2xl font-black text-primary">{{ preview.toCreate }}</span>
              <span class="text-[11px] font-bold text-white/50 uppercase tracking-widest">suất sẽ tạo</span>
            </div>
            <div v-if="preview.skipped?.length" class="flex items-baseline gap-1.5">
              <span class="text-2xl font-black text-red-400">{{ preview.skipped.length }}</span>
              <span class="text-[11px] font-bold text-white/50 uppercase tracking-widest">bỏ qua</span>
            </div>
          </div>
          <div v-if="preview.skipped?.length" class="max-h-40 overflow-y-auto divide-y divide-white/5">
            <div v-for="(s, i) in preview.skipped" :key="i" class="px-4 py-2 flex items-center justify-between text-[11px]">
              <span class="text-white/70 font-bold">{{ s.roomName }} · {{ s.startTime.slice(0, 16).replace('T', ' ') }}</span>
              <span class="text-red-400/80">{{ s.reason }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="p-6 border-t border-white/5 bg-black/20 flex gap-4">
        <button @click="runPreview" :disabled="isBusy"
          class="flex-1 py-3 rounded-xl bg-white/5 hover:bg-white/10 text-white font-bold uppercase tracking-widest text-xs transition-colors disabled:opacity-50">
          Xem trước
        </button>
        <button @click="handleCreate" :disabled="isBusy"
          class="flex-1 py-3 rounded-xl bg-primary hover:brightness-110 text-on-primary font-bold uppercase tracking-widest text-xs shadow-lg shadow-primary/20 transition-all disabled:opacity-50">
          {{ isBusy ? 'Đang xử lý...' : 'Tạo lịch' }}
        </button>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.drawer-enter-active, .drawer-leave-active { transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.drawer-enter-from, .drawer-leave-to { transform: translateX(100%); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
