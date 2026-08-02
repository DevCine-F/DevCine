<script setup>
import { ref, reactive, onMounted, watch, computed, nextTick } from 'vue';
import api from '@/api/axios';
import CustomSelect from './CustomSelect.vue';
import { useToastStore } from '@/stores/toast';
import { useConfirmStore } from '@/stores/confirm';
import { friendlyError } from '@/utils/friendlyError';

const toast = useToastStore();
const confirm = useConfirmStore();

const props = defineProps({
  isOpen: Boolean,
  cinemaId: Number,
  selectedDate: String
});

const emit = defineEmits(['close', 'saved']);

const form = reactive({
  movieId: '',
  roomId: '',
  formatId: '',
  startHour: '',
  startMinute: ''
});

const movies = ref([]);
const rooms = ref([]);
const formats = ref([]);

// Lỗi theo TỪNG trường (thay banner lỗi tổng ở đầu drawer) — inline + tự cuộn tới lỗi đầu tiên
const fieldErrors = reactive({ movieId: '', roomId: '', formatId: '', time: '' });
const movieField = ref(null);
const roomField = ref(null);
const formatField = ref(null);
const timeField = ref(null);
const clearErrors = () => Object.keys(fieldErrors).forEach(k => { fieldErrors[k] = ''; });

// Computed properties for CustomSelect options format
const movieOptions = computed(() => {
  return movies.value
    .filter(m => m.status === 'active' || m.status === 'upcoming')
    .map(m => ({ value: m.id, label: m.title || m.name }));
});

const roomOptions = computed(() => {
  return rooms.value.map(r => ({ value: r.id, label: r.name }));
});

const formatOptions = computed(() => {
  if (!form.movieId) return []; // Require movie selection first
  
  const selectedMovie = movies.value.find(m => m.id === form.movieId);
  if (!selectedMovie) return [];

  const supportedFormatsStr = selectedMovie.supportedFormats || selectedMovie.format || "";
  if (!supportedFormatsStr) {
    // Fallback if no format info is provided
    return formats.value.map(f => ({ value: f.id, label: f.name }));
  }

  const supportedList = supportedFormatsStr.split(',').map(s => s.trim().toUpperCase());
  
  const filteredFormats = formats.value.filter(f => {
    const fName = f.name.toUpperCase();
    return supportedList.some(sup => fName.includes(sup));
  });

  return filteredFormats.map(f => ({ value: f.id, label: f.name }));
});

const hourOptions = Array.from({ length: 24 }, (_, i) => {
  const val = i.toString().padStart(2, '0');
  return { value: val, label: val };
});

const minuteOptions = Array.from({ length: 12 }, (_, i) => {
  const val = (i * 5).toString().padStart(2, '0');
  return { value: val, label: val };
});

const fetchOptions = async () => {
  try {
    const [moviesRes, formatsRes] = await Promise.all([
      api.get('/movies'),
      api.get('/formats')
    ]);
    movies.value = moviesRes.data;
    formats.value = formatsRes.data;
  } catch (error) {
    toast.error(friendlyError(error, 'Không tải được danh sách phim/định dạng.'));
  }
};

const fetchRooms = async (cinemaId) => {
  if (!cinemaId) return;
  try {
    const res = await api.get(`/rooms/cinema/${cinemaId}`);
    rooms.value = res.data;
  } catch (error) {
    toast.error(friendlyError(error, 'Không tải được danh sách phòng chiếu.'));
  }
};

watch(() => form.movieId, () => {
  // Reset format selection when movie changes
  form.formatId = '';
  fieldErrors.movieId = '';
});

// Lỗi tự xóa khi người dùng bắt đầu sửa đúng trường đó
watch(() => form.roomId, () => { fieldErrors.roomId = ''; });
watch(() => form.formatId, () => { fieldErrors.formatId = ''; });
watch(() => [form.startHour, form.startMinute], () => { fieldErrors.time = ''; });

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    fetchOptions();
    fetchRooms(props.cinemaId);
    clearErrors();
    // Reset form
    form.movieId = '';
    form.roomId = '';
    form.formatId = '';
    form.startHour = '';
    form.startMinute = '';
  }
});

// Trả về ref của trường lỗi ĐẦU TIÊN (theo thứ tự hiển thị) hoặc null nếu hợp lệ.
const validate = () => {
  clearErrors();
  if (!form.movieId) fieldErrors.movieId = 'Vui lòng chọn phim.';
  if (!form.roomId) fieldErrors.roomId = 'Vui lòng chọn phòng chiếu.';
  if (!form.formatId) fieldErrors.formatId = 'Vui lòng chọn định dạng.';
  if (!form.startHour || !form.startMinute) fieldErrors.time = 'Vui lòng chọn giờ bắt đầu.';

  const order = [
    ['movieId', movieField], ['roomId', roomField],
    ['formatId', formatField], ['time', timeField]
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

// Gửi tạo suất; force=true khi admin đã xác nhận suất khuya (kết thúc quá giờ đóng cửa).
const submit = async (formattedStartTime, force) => {
  const { data } = await api.post('/showtimes', {
    movieId: parseInt(form.movieId),
    roomId: parseInt(form.roomId),
    formatId: parseInt(form.formatId),
    startTime: formattedStartTime,
    force
  });
  // BE trả requiresConfirmation khi suất kết thúc quá giờ đóng cửa → hỏi rồi gửi lại force.
    if (data?.requiresConfirmation) {
      const ok = await confirm.show({
        title: 'Suất chiếu khuya',
        message: 'Suất chiếu kết thúc sau giờ đóng cửa rạp. Bạn có chắc chắn muốn tạo?',
        confirmText: 'Vẫn tạo',
        cancelText: 'Hủy'
      });
      if (ok) return submit(formattedStartTime, true);
      return;
    }
    toast.success('Đã thêm suất chiếu.');
    window.dispatchEvent(new Event('showtimes-updated'));
    emit('saved');
    emit('close');
};

const handleSave = async () => {
  const badField = validate();
  if (badField) { focusFirstError(badField); return; }

  const selectedMovie = movies.value.find(m => m.id === form.movieId);
  if (selectedMovie) {
    const year = new Date().getFullYear();
    const [day, month] = props.selectedDate.split('/');
    const showDate = new Date(`${year}-${month}-${day}T00:00:00`);
    
    const startDate = new Date(selectedMovie.startDate);
    startDate.setHours(0,0,0,0);
    
    let isOutOfRange = showDate < startDate;
    if (selectedMovie.endDate) {
      const endDate = new Date(selectedMovie.endDate);
      endDate.setHours(23,59,59,999);
      if (showDate > endDate) isOutOfRange = true;
    }

    if (isOutOfRange) {
      toast.error(`Ngày chiếu ${props.selectedDate}/${year} nằm ngoài khoảng thời gian khởi chiếu/kết thúc của phim '${selectedMovie.title}'!`);
      return;
    }
  }

  try {
    const year = new Date().getFullYear();
    const [day, month] = props.selectedDate.split('/');
    const formattedStartTime = `${year}-${month}-${day}T${form.startHour}:${form.startMinute}:00`;
    await submit(formattedStartTime, false);
  } catch (error) {
    toast.error(friendlyError(error, 'Có lỗi xảy ra khi lưu suất chiếu.'));
  }
};
</script>

<template>
  <!-- Backdrop: bấm ra ngoài vùng drawer để đóng -->
  <Transition name="fade">
    <div v-if="isOpen" class="fixed inset-0 bg-black/40 z-[140]" @click="emit('close')"></div>
  </Transition>

  <Transition name="drawer">
    <div v-if="isOpen" class="fixed inset-y-0 right-0 w-[450px] bg-surface-container border-l border-white/10 shadow-2xl z-[150] flex flex-col">
      <!-- Header -->
      <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-black/20">
        <div>
          <h2 class="text-xl font-black text-white uppercase tracking-widest font-headline">Thêm suất chiếu</h2>
          <p class="text-xs text-white/50 mt-1 uppercase tracking-widest">Tạo lịch chiếu mới</p>
        </div>
        <button @click="emit('close')" class="w-10 h-10 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center transition-all border border-white/10">
          <span class="material-symbols-outlined text-white/70">close</span>
        </button>
      </div>

      <!-- Body -->
      <div class="flex-1 overflow-y-auto p-8 space-y-6">

        <div ref="movieField">
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Phim</label>
          <CustomSelect
            v-model="form.movieId"
            :options="movieOptions"
            placeholder="-- Chọn Phim --"
            :class="fieldErrors.movieId ? 'rounded-xl ring-1 ring-red-500/60' : ''"
          />
          <p v-if="fieldErrors.movieId" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.movieId }}</p>
        </div>

        <div ref="roomField">
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Phòng chiếu</label>
          <CustomSelect
            v-model="form.roomId"
            :options="roomOptions"
            placeholder="-- Chọn Phòng chiếu --"
            :class="fieldErrors.roomId ? 'rounded-xl ring-1 ring-red-500/60' : ''"
          />
          <p v-if="fieldErrors.roomId" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.roomId }}</p>
        </div>

        <div ref="formatField">
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Định dạng</label>
          <CustomSelect
            v-model="form.formatId"
            :options="formatOptions"
            placeholder="-- Chọn Định dạng --"
            :class="fieldErrors.formatId ? 'rounded-xl ring-1 ring-red-500/60' : ''"
          />
          <p v-if="fieldErrors.formatId" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.formatId }}</p>
        </div>

        <div ref="timeField">
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">
            Thời gian bắt đầu <span class="text-primary font-medium lowercase tracking-normal">(Suất chiếu cho ngày {{ selectedDate }}/{{ new Date().getFullYear() }})</span>
          </label>
          <div class="flex gap-4">
            <div class="flex-1">
              <CustomSelect
                v-model="form.startHour"
                :options="hourOptions"
                placeholder="Giờ"
                :class="fieldErrors.time ? 'rounded-xl ring-1 ring-red-500/60' : ''"
              />
            </div>
            <div class="flex-1">
              <CustomSelect
                v-model="form.startMinute"
                :options="minuteOptions"
                placeholder="Phút"
                :class="fieldErrors.time ? 'rounded-xl ring-1 ring-red-500/60' : ''"
              />
            </div>
          </div>
          <p v-if="fieldErrors.time" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.time }}</p>
        </div>

        <p class="text-[11px] text-white/40 italic flex items-center gap-1.5">
          <span class="material-symbols-outlined text-[14px]">info</span>
          Thời gian dọn dẹp được lấy tự động theo cấu hình của từng phòng chiếu.
        </p>

      </div>

      <!-- Footer -->
      <div class="p-6 border-t border-white/5 bg-black/20 flex gap-4">
        <button @click="emit('close')" class="flex-1 py-3 rounded-xl bg-white/5 hover:bg-white/10 text-white font-bold uppercase tracking-widest text-xs transition-colors">
          Hủy
        </button>
        <button @click="handleSave" class="flex-1 py-3 rounded-xl bg-primary hover:brightness-110 text-on-primary font-bold uppercase tracking-widest text-xs shadow-lg shadow-primary/20 transition-all">
          Lưu Suất Chiếu
        </button>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.drawer-enter-active,
.drawer-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(100%);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
