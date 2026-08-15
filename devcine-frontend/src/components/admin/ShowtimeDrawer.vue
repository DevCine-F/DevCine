<script setup>
import { ref, reactive, onMounted, watch, computed, nextTick } from 'vue';
import api from '@/api/axios';
import { pricingApi } from '@/api/admin/index';
import CustomSelect from './CustomSelect.vue';
import { useToastStore } from '@/stores/toast';
import { useConfirmStore } from '@/stores/confirm';
import { friendlyError } from '@/utils/friendlyError';

const toast = useToastStore();
const confirm = useConfirmStore();

const props = defineProps({
  isOpen: Boolean,
  cinemaId: Number,
  cinema: Object,
  selectedDate: String,
  editData: {
    type: Object,
    default: null
  }
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
const pricingConfig = ref(null);
const existingShowtimes = ref([]);

const fieldErrors = reactive({ movieId: '', roomId: '', formatId: '', time: '' });
const movieField = ref(null);
const roomField = ref(null);
const formatField = ref(null);
const timeField = ref(null);
const clearErrors = () => Object.keys(fieldErrors).forEach(k => { fieldErrors[k] = ''; });

const movieOptions = computed(() => {
  return [...movies.value]
    .filter(m => m.status === 'active' || m.status === 'upcoming')
    .sort((a, b) => (b.id || 0) - (a.id || 0))
    .map(m => ({ value: m.id, label: m.title || m.name }));
});

const roomOptions = computed(() => {
  return rooms.value
    .filter(r => r.status === 'Active' || r.id === form.roomId)
    .map(r => ({ value: r.id, label: r.name }));
});

const formatOptions = computed(() => {
  if (!form.movieId || !form.roomId) return []; 
  
  const selectedMovie = movies.value.find(m => m.id === form.movieId);
  const selectedRoom = rooms.value.find(r => r.id === form.roomId);
  if (!selectedMovie || !selectedRoom) return [];

  const movieSupportedStr = selectedMovie.supportedFormats || selectedMovie.format || "";
  const supportedList = movieSupportedStr.split(',').map(s => s.trim().toUpperCase());

  let supportedFormats = formats.value;

  if (supportedList.length > 0 && supportedList[0] !== "") {
    supportedFormats = supportedFormats.filter(f => supportedList.some(sup => f.name.trim().toUpperCase().includes(sup)));
  }

  const roomType = selectedRoom.type?.trim().toUpperCase() || 'STANDARD';
  
  supportedFormats = supportedFormats.filter(f => {
    const fn = f.name.trim().toUpperCase();
    if (roomType === 'SUPERPLEX') return true;
    if (roomType === 'STANDARD' || roomType === 'CINE_COMFORT') {
      return fn === '2D PHỤ ĐỀ' || fn === '2D LỒNG TIẾNG' || fn === '3D PHỤ ĐỀ' || fn === '3D LỒNG TIẾNG';
    }
    return false;
  });

  return supportedFormats.map(f => ({ value: f.id, label: f.name }));
});

const todayCheck = computed(() => {
  if (!props.selectedDate) return false;
  const year = new Date().getFullYear();
  const [day, month] = props.selectedDate.split('/');
  const showDate = new Date(`${year}-${month}-${day}T00:00:00`);
  const today = new Date();
  today.setHours(0,0,0,0);
  return showDate.getTime() === today.getTime();
});

const currentHour = computed(() => todayCheck.value ? new Date().getHours() : -1);
const currentMinute = computed(() => todayCheck.value ? new Date().getMinutes() : -1);

const hourOptions = computed(() => {
  let openH = 8, closeH = 23;
  if (props.cinema) {
    if (props.cinema.openingTime) openH = parseInt(props.cinema.openingTime.split(':')[0]);
    if (props.cinema.closingTime) closeH = parseInt(props.cinema.closingTime.split(':')[0]);
  }

  const hours = [];
  let h = openH;
  while (true) {
    hours.push(h);
    if (h === closeH) break;
    h = (h + 1) % 24;
    if (hours.length >= 24) break;
  }

  return hours.map(h => {
    const val = h.toString().padStart(2, '0');
    let disabled = false;
    if (todayCheck.value) {
      if (h >= openH) {
         disabled = h < currentHour.value;
      } else {
         disabled = false; // Next day hours are never past
      }
    }
    return { value: val, label: val, disabled };
  }).filter(opt => !opt.disabled);
});

const minuteOptions = computed(() => {
  return Array.from({ length: 12 }, (_, i) => {
    const minVal = i * 5;
    const val = minVal.toString().padStart(2, '0');
    let disabled = false;
    if (currentHour.value !== -1 && form.startHour === currentHour.value.toString().padStart(2, '0')) {
      disabled = minVal < currentMinute.value;
    }
    return { value: val, label: val, disabled };
  }).filter(opt => !opt.disabled);
});

const fetchOptions = async () => {
  try {
    const [moviesRes, formatsRes, configRes] = await Promise.all([
      api.get('/movies'),
      api.get('/formats'),
      pricingApi.getConfig().catch(() => ({ data: null }))
    ]);
    movies.value = moviesRes.data;
    formats.value = formatsRes.data;
    if (configRes?.data) pricingConfig.value = configRes.data;
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

const fetchShowtimes = async () => {
  if (!props.cinemaId || !props.selectedDate) return;
  const year = new Date().getFullYear();
  const [day, month] = props.selectedDate.split('/');
  const dateStr = `${year}-${month}-${day}`;
  
  try {
    const { data } = await api.get('/showtimes/by-cinema', { params: { cinemaId: props.cinemaId, date: dateStr } });
    existingShowtimes.value = data || [];
  } catch (e) {
    console.error('Failed to fetch showtimes for slots', e);
  }
};



watch(() => form.formatId, () => { fieldErrors.formatId = ''; });
watch(() => [form.startHour, form.startMinute], () => { fieldErrors.time = ''; });

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    fetchOptions();
    fetchRooms(props.cinemaId);
    fetchShowtimes();
    clearErrors();
    if (props.editData) {
      form.movieId = props.editData.movieId || props.editData.movie?.id || '';
      form.roomId = props.editData.roomId || props.editData.room?.id || '';
      form.formatId = props.editData.formatId || props.editData.format?.id || '';
      if (props.editData.startTime) {
        const st = new Date(props.editData.startTime);
        form.startHour = st.getHours().toString().padStart(2, '0');
        form.startMinute = st.getMinutes().toString().padStart(2, '0');
      }
    } else {
      form.movieId = '';
      form.roomId = '';
      form.formatId = '';
      form.startHour = '';
      form.startMinute = '';
    }
  }
});

const isLocked = computed(() => {
  if (!props.editData) return false;
  return (props.editData.soldSeats || 0) > 0 || (props.editData.reserved || 0) > 0 || (props.editData.heldSeats || 0) > 0;
});

const pricePreview = computed(() => {
  if (!form.roomId || !form.formatId || !pricingConfig.value || !props.selectedDate) return null;
  const config = pricingConfig.value;
  const room = rooms.value.find(r => r.id === form.roomId);
  const format = formats.value.find(f => f.id === form.formatId);
  if (!room || !format) return null;

  const year = new Date().getFullYear();
  const [day, month] = props.selectedDate.split('/');
  const showDate = new Date(`${year}-${month}-${day}T00:00:00`);
  
  const dateString = `${year}-${month}-${day}`;
  const isHoliday = config.holidays?.some(h => h.holidayDate === dateString);
  const dayOfWeek = showDate.getDay();
  const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
  
  const dayType = isHoliday ? 'HOLIDAY' : (isWeekend ? 'WEEKEND' : 'WEEKDAY');
  const roomType = room.type || 'STANDARD';
  const audienceType = 'ADULT';

  const baseRule = config.baseMatrix?.find(r => 
    r.roomType === roomType && r.dayType === dayType && r.audienceType === audienceType
  );
  
  if (!baseRule) return null;
  
  const baseValue = Number(baseRule.value || 0);
  const surcharge = isWeekend && format.weekendSurcharge != null ? Number(format.weekendSurcharge) : Number(format.surcharge || 0);
  
  const totalPrice = baseValue + surcharge;
  const dayLabel = isHoliday ? 'Ngày lễ' : (isWeekend ? 'Cuối tuần' : 'Ngày thường');
  return { label: `${dayLabel} - ${totalPrice.toLocaleString('vi-VN')}đ`, value: totalPrice };
});

const endTimePreview = computed(() => {
  if (!form.movieId || !form.roomId || !form.startHour || !form.startMinute) return null;
  const movie = movies.value.find(m => m.id === form.movieId);
  const room = rooms.value.find(r => r.id === form.roomId);
  if (!movie || !room) return null;

  const duration = movie.durationMins || 120;
  const turnaround = room.turnaroundTimeMins || 15;
  
  const start = new Date(2000, 0, 1, parseInt(form.startHour), parseInt(form.startMinute));
  const end = new Date(start.getTime() + (duration + turnaround) * 60000);
  
  return {
    startStr: `${form.startHour}:${form.startMinute}`,
    endStr: `${end.getHours().toString().padStart(2, '0')}:${end.getMinutes().toString().padStart(2, '0')}`,
    turnaround
  };
});

/* ===== Kiểm tra khung giờ REAL-TIME (inline error, không toast) =====
   Soi ngay khi đổi giờ/phút bằng dữ liệu đã nạp sẵn -> không gọi API, không debounce.
   Mirror đúng thứ tự luật của ShowtimeService.createShowtime: giờ hoạt động -> trùng lịch -> giờ đóng cửa.
   Đây chỉ là lớp cảnh báo sớm; backend vẫn là chốt chặn cuối cùng. */
const cinemaWindow = computed(() => {
  const toMin = (t, dh, dm) => {
    if (!t) return dh * 60 + dm;
    const [h, m] = String(t).split(':');
    return (parseInt(h) || 0) * 60 + (parseInt(m) || 0);
  };
  const openMin = toMin(props.cinema?.openingTime, 8, 0);
  let closeMin = toMin(props.cinema?.closingTime, 23, 30);
  if (closeMin <= openMin) closeMin += 1440;
  return { openMin, closeMin };
});

const fmtMin = (min) => {
  const m = ((min % 1440) + 1440) % 1440;
  return `${Math.floor(m / 60).toString().padStart(2, '0')}:${(m % 60).toString().padStart(2, '0')}`;
};

const fmtClock = (date) => `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;

const buildStartDate = () => {
  const year = new Date().getFullYear();
  const [day, month] = props.selectedDate.split('/');
  return new Date(`${year}-${month}-${day}T${form.startHour}:${form.startMinute}:00`);
};

const timeConflictError = computed(() => {
  if (isLocked.value) return '';
  if (!form.movieId || !form.roomId || !form.startHour || !form.startMinute || !props.selectedDate) return '';

  const movie = movies.value.find(m => m.id === form.movieId);
  const room = rooms.value.find(r => r.id === form.roomId);
  if (!movie || !room) return '';

  const duration = movie.durationMins || 120;
  const turnaround = room.turnaroundTimeMins || 15;
  const start = buildStartDate();
  if (isNaN(start.getTime())) return '';
  const end = new Date(start.getTime() + (duration + turnaround) * 60000);

  if (start < new Date()) {
    return 'Không thể tạo suất chiếu ở thời gian đã trôi qua. Vui lòng chọn giờ khác.';
  }

  const { openMin, closeMin } = cinemaWindow.value;
  let startPos = parseInt(form.startHour) * 60 + parseInt(form.startMinute);
  if (startPos < openMin) startPos += 1440;
  const endPos = startPos + duration + turnaround;

  if (startPos >= closeMin) {
    return `Suất chiếu bắt đầu ngoài giờ hoạt động của rạp (${fmtMin(openMin)} – ${fmtMin(closeMin)}). Vui lòng chọn giờ khác.`;
  }

  const clash = existingShowtimes.value.find(s => {
    if (props.editData && String(s.id) === String(props.editData.id)) return false;
    if (String(s.roomId ?? s.room?.id) !== String(form.roomId)) return false;
    const existStart = new Date(s.startTime);
    const existEnd = new Date(s.endTime);
    if (isNaN(existStart.getTime()) || isNaN(existEnd.getTime())) return false;
    return start < existEnd && end > existStart;
  });
  if (clash) {
    return `Phòng chiếu đã có lịch ${fmtClock(new Date(clash.startTime))} – ${fmtClock(new Date(clash.endTime))} (đã gồm thời gian dọn phòng). Vui lòng chọn giờ khác.`;
  }

  if (endPos > closeMin) {
    return `Suất chiếu kết thúc lúc ${fmtMin(endPos)}, vượt quá giờ đóng cửa (${fmtMin(closeMin)}). Vui lòng chọn giờ khác.`;
  }

  return '';
});

// Dòng đỏ dưới trường thời gian: lỗi bỏ trống (khi bấm Lưu) hoặc lỗi khung giờ (real-time).
const timeError = computed(() => fieldErrors.time || timeConflictError.value);

const suggestedSlots = computed(() => {
  if (!form.movieId || !form.roomId || !props.selectedDate) return [];
  const movie = movies.value.find(m => m.id === form.movieId);
  const room = rooms.value.find(r => r.id === form.roomId);
  if (!movie || !room) return [];

  const duration = movie.durationMins || 120;
  const turnaround = room.turnaroundTimeMins || 15;
  const totalMins = duration + turnaround;

  const roomShows = existingShowtimes.value
    .filter(s => String(s.roomId || s.room?.id) === String(form.roomId))
    .map(s => {
      const st = new Date(s.startTime);
      const et = new Date(s.endTime);
      return { 
        existStart: st.getHours() * 60 + st.getMinutes(), 
        existEnd: et.getHours() * 60 + et.getMinutes() + turnaround 
      };
    });

  const slots = [];
  const startDay = 8 * 60; 
  const endDay = 23 * 60 + 30;

  let currentMin = startDay;
  if (currentHour.value > 0) {
    const now = new Date();
    const nowMin = now.getHours() * 60 + now.getMinutes() + 30;
    if (nowMin > currentMin) currentMin = nowMin;
  }
  
  currentMin = Math.ceil(currentMin / 5) * 5;

  const potentialStarts = [...roomShows.map(s => Math.ceil(s.existEnd / 5) * 5)];
  for (let m = currentMin; m <= endDay - totalMins; m += 30) {
    potentialStarts.push(m);
  }
  potentialStarts.sort((a, b) => a - b);

  for (const propStart of potentialStarts) {
    if (propStart < currentMin) continue;
    const propEnd = propStart + totalMins;
    if (propEnd > endDay) continue;

    const hasConflict = roomShows.some(s => (propStart < s.existEnd && propEnd > s.existStart));
    if (!hasConflict) {
      slots.push(propStart);
    }
  }

  const formattedSlots = slots.map(mins => {
    const h = Math.floor(mins / 60).toString().padStart(2, '0');
    const m = (mins % 60).toString().padStart(2, '0');
    return `${h}:${m}`;
  });

  const uniqueSlots = [...new Set(formattedSlots)];
  return uniqueSlots.slice(0, 4);
});

const selectSlot = (timeStr) => {
  const [h, m] = timeStr.split(':');
  form.startHour = h;
  form.startMinute = m;
};

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

const focusFirstError = async (fieldRef) => {
  await nextTick();
  const el = fieldRef?.value;
  if (!el) return;
  el.scrollIntoView({ behavior: 'smooth', block: 'center' });
  el.querySelector('input, button, select, [tabindex]')?.focus?.();
};

const submit = async (formattedStartTime, force) => {
  const payload = {
    movieId: parseInt(form.movieId),
    roomId: parseInt(form.roomId),
    formatId: parseInt(form.formatId),
    startTime: formattedStartTime,
    force
  };

  let data;
  if (props.editData) {
    const res = await api.patch(`/showtimes/${props.editData.id}`, payload);
    data = res.data;
  } else {
    const res = await api.post('/showtimes', payload);
    data = res.data;
  }

  if (data?.requiresConfirmation) {
    const ok = await confirm.show({
      title: 'Suất chiếu khuya',
      message: 'Suất chiếu kết thúc sau giờ đóng cửa rạp. Bạn có chắc chắn muốn tạo/cập nhật?',
      confirmText: 'Vẫn lưu',
      cancelText: 'Hủy'
    });
    if (ok) return submit(formattedStartTime, true);
    return;
  }
  
  toast.success(props.editData ? 'Đã cập nhật suất chiếu.' : 'Đã thêm suất chiếu.');
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

    const selectedDateTime = new Date(`${year}-${month}-${day}T${form.startHour}:${form.startMinute}:00`);
    const now = new Date();
    if (selectedDateTime < now) {
      toast.error('Không thể tạo hoặc sửa suất chiếu ở thời gian đã trôi qua!');
      return;
    }
    
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
  <Transition name="fade">
    <div v-if="isOpen" class="fixed inset-0 bg-black/40 z-[140]" @click="emit('close')"></div>
  </Transition>

  <Transition name="drawer">
    <div v-if="isOpen" class="fixed inset-y-0 right-0 w-[450px] bg-surface-container border-l border-white/10 shadow-2xl z-[150] flex flex-col">
      <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-black/20">
        <div>
          <h2 class="text-xl font-black text-white uppercase tracking-widest font-headline">{{ editData ? 'Sửa suất chiếu' : 'Thêm suất chiếu' }}</h2>
          <p class="text-xs text-white/50 mt-1 uppercase tracking-widest">{{ editData ? 'Cập nhật thông tin suất chiếu' : 'Tạo lịch chiếu mới' }}</p>
        </div>
        <button @click="emit('close')" class="w-10 h-10 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center transition-all border border-white/10">
          <span class="material-symbols-outlined text-white/70">close</span>
        </button>
      </div>

      <div class="flex-1 overflow-y-auto p-8 space-y-6">

        <div v-if="isLocked" class="bg-amber-500/10 border border-amber-500/20 rounded-xl p-3 flex items-center gap-3">
          <span class="material-symbols-outlined text-amber-400">lock</span>
          <p class="text-[11px] text-amber-400 font-bold">Suất chiếu đã có vé đặt, không thể thay đổi thời gian/phòng chiếu.</p>
        </div>

        <div ref="movieField">
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Phim</label>
          <CustomSelect
            v-model="form.movieId"
            :options="movieOptions"
            :searchable="true"
            @update:modelValue="() => { form.formatId = ''; fieldErrors.movieId = ''; }"
            :disabled="isLocked"
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
            @update:modelValue="() => { form.formatId = ''; fieldErrors.roomId = ''; }"
            :disabled="isLocked"
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
            :disabled="isLocked"
            placeholder="-- Chọn Định dạng --"
            :class="fieldErrors.formatId ? 'rounded-xl ring-1 ring-red-500/60' : ''"
          />
          <p v-if="fieldErrors.formatId" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.formatId }}</p>
          <div v-if="pricePreview" class="mt-2 inline-flex items-center gap-1.5 px-2 py-1 rounded-md bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-[11px] font-bold">
            <span class="material-symbols-outlined text-[14px]">sell</span>
            Bảng giá áp dụng: [{{ pricePreview.label }}]
          </div>
        </div>

        <div v-if="suggestedSlots.length > 0" class="space-y-2">
          <label class="block text-[11px] font-bold text-white/50 uppercase tracking-widest">Gợi ý giờ trống:</label>
          <div class="flex flex-wrap gap-2">
            <button 
              v-for="slot in suggestedSlots" 
              :key="slot" 
              @click="selectSlot(slot)"
              class="px-3 py-1.5 rounded-lg bg-primary/10 border border-primary/20 text-primary hover:bg-primary/20 text-[11px] font-bold transition-colors"
            >
              {{ slot }}
            </button>
          </div>
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
                :disabled="isLocked"
                placeholder="Giờ"
                :class="timeError ? 'rounded-xl ring-1 ring-red-500/60' : ''"
              />
            </div>
            <div class="flex-1">
              <CustomSelect
                v-model="form.startMinute"
                :options="minuteOptions"
                :disabled="isLocked"
                placeholder="Phút"
                :class="timeError ? 'rounded-xl ring-1 ring-red-500/60' : ''"
              />
            </div>
          </div>

          <div v-if="endTimePreview" class="mt-3 p-3 rounded-xl bg-white/5 border border-white/10 space-y-1">
            <p class="text-[11px] text-white/70 font-medium">
              Khung giờ chiếm phòng dự kiến: <span class="text-white font-bold">{{ endTimePreview.startStr }} - {{ endTimePreview.endStr }}</span>
            </p>
            <p class="text-[10px] text-white/40 italic">
              (Bao gồm {{ endTimePreview.turnaround }} phút dọn phòng)
            </p>
          </div>
          <p v-else class="text-[11px] text-white/40 italic flex items-center gap-1.5 mt-3">
            <span class="material-symbols-outlined text-[14px]">info</span>
            Thời gian dọn dẹp được lấy tự động theo cấu hình của từng phòng chiếu.
          </p>

          <div class="min-h-[18px] mt-2">
            <Transition name="fade">
              <p v-if="timeError" aria-live="polite" class="text-[11px] text-red-400 font-bold leading-snug flex items-start gap-1.5">
                <span class="material-symbols-outlined text-[14px] leading-none shrink-0 mt-px">error</span>
                <span>{{ timeError }}</span>
              </p>
            </Transition>
          </div>
        </div>

      </div>

      <div class="p-6 border-t border-white/5 bg-black/20 flex gap-4">
        <button @click="emit('close')" class="flex-1 py-3 rounded-xl bg-white/5 hover:bg-white/10 text-white font-bold uppercase tracking-widest text-xs transition-colors">
          Hủy
        </button>
        <button
          @click="handleSave"
          :disabled="!!timeConflictError"
          class="flex-1 py-3 rounded-xl bg-primary enabled:hover:brightness-110 text-on-primary font-bold uppercase tracking-widest text-xs shadow-lg shadow-primary/20 transition-all disabled:opacity-40 disabled:shadow-none disabled:cursor-not-allowed"
        >
          {{ editData ? 'Lưu Thay Đổi' : 'Lưu Suất Chiếu' }}
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
