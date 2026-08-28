<script setup>
import { ref, reactive, computed, watch, nextTick } from 'vue';
import api from '@/api/axios';
import CustomSelect from './CustomSelect.vue';
import { useToastStore } from '@/stores/toast';
import { useConfirmStore } from '@/stores/confirm';
import { friendlyError } from '@/utils/friendlyError';

const confirm = useConfirmStore();

const props = defineProps({
  isOpen: Boolean,
  // Danh sách cụm rạp kèm halls: [{ id, name, city, halls: [{id,name,type}] }]
  cinemas: { type: Array, default: () => [] }
});
const emit = defineEmits(['close', 'saved']);

const toast = useToastStore();

const getLocalTodayStr = () => {
  const today = new Date();
  const year = today.getFullYear();
  const month = (today.getMonth() + 1).toString().padStart(2, '0');
  const day = today.getDate().toString().padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const form = reactive({
  movieId: '',
  formatId: '',
  dateFrom: '',
  dateTo: '',
  daysOfWeek: [],        // ISO 1..7 (rỗng = mọi ngày)
  roomIds: [],           // các phòng đã tick (nhiều cơ sở)
  startTimes: [],        // "HH:mm"
  roomOffsetMins: 0,     // 0 = Đồng thời, 10 = Lệch 10p, 15 = Lệch 15p
  autoFillGaps: true     // Tự động lấp khoảng trống vào mốc giờ tròn gần nhất
});

const staggerOptions = [
  { value: 0, label: 'Chiếu đồng thời' },
  { value: 10, label: '10 phút' },
  { value: 15, label: '15 phút' }
];

const movies = ref([]);
const formats = ref([]);
// Cây cơ sở→phòng cục bộ: tự nạp rooms cho các rạp chưa có halls (danh sách nạp lười).
const localCinemas = ref([]);
const newHour = ref('');
const newMinute = ref('');
const preview = ref(null);   // { toCreate, createdCount, skipped: [] }
const isBusy = ref(false);

// Lỗi theo TỪNG trường (thay banner lỗi tổng ở đầu drawer) — hiện inline + tự cuộn tới lỗi đầu tiên
const fieldErrors = reactive({ movieId: '', formatId: '', roomIds: '', dateFrom: '', dateTo: '', startTimes: '' });
const movieField = ref(null);
const formatField = ref(null);
const roomsField = ref(null);
const dateFromField = ref(null);
const dateToField = ref(null);
const timesField = ref(null);
const clearErrors = () => Object.keys(fieldErrors).forEach(k => { fieldErrors[k] = ''; });

const weekDays = [
  { value: 1, label: 'T2' }, { value: 2, label: 'T3' }, { value: 3, label: 'T4' },
  { value: 4, label: 'T5' }, { value: 5, label: 'T6' }, { value: 6, label: 'T7' },
  { value: 7, label: 'CN' }
];

const movieOptions = computed(() => {
  return [...movies.value]
    .filter(m => (m.status || '').toLowerCase() === 'active' || (m.status || '').toLowerCase() === 'upcoming')
    .sort((a, b) => (b.id || 0) - (a.id || 0))
    .map(m => {
      const isUpcoming = (m.status || '').toLowerCase() === 'upcoming';
      const duration = m.durationMins || m.duration || 120;
      return {
        value: m.id,
        label: `${m.title || m.name} - ${duration}p`,
        badge: {
          text: isUpcoming ? 'Sắp chiếu' : 'Đang chiếu',
          type: isUpcoming ? 'upcoming' : 'active',
          class: isUpcoming
            ? 'bg-blue-500/10 text-blue-400 border-blue-500/30'
            : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
        }
      };
    });
});

// Định dạng lọc theo phim đã chọn (giống ShowtimeDrawer)
const formatOptions = computed(() => {
  if (!form.movieId) return [];
  const movie = movies.value.find(m => m.id === form.movieId);
  if (!movie) return [];
  const supportedStr = movie.supportedFormats || movie.format || '';
  if (!supportedStr) return formats.value.map(f => ({ value: f.id, label: f.name }));
  const supported = supportedStr.split(',').map(s => s.trim().toUpperCase());
  return formats.value
    .filter(f => supported.some(sup => f.name.trim().toUpperCase().includes(sup)))
    .map(f => ({ value: f.id, label: f.name }));
});

const totalRoomsSelected = computed(() => form.roomIds.length);

const fetchOptions = async () => {
  try {
    const [moviesRes, formatsRes] = await Promise.all([api.get('/movies'), api.get('/formats')]);
    movies.value = moviesRes.data;
    formats.value = formatsRes.data;
  } catch (e) {
    toast.error(friendlyError(e, 'Không tải được danh sách phim/định dạng.'));
  }
};

// Dựng cây cơ sở→phòng: dùng halls sẵn có, hoặc nạp 1 request duy nhất cho toàn bộ hệ thống (tránh N+1)
const buildCinemaTree = async () => {
  const base = props.cinemas || [];
  
  // Nếu tất cả cụm rạp đã nạp sẵn halls -> dùng ngay không cần gọi thêm API
  const allHaveHalls = base.length > 0 && base.every(c => c.halls && c.halls.length > 0);
  if (allHaveHalls) {
    localCinemas.value = base.map(c => {
      const sorted = [...c.halls].sort((a, b) => (a.name || '').localeCompare(b.name || '', undefined, { numeric: true, sensitivity: 'base' }));
      return { id: c.id, name: c.name, city: c.city, halls: sorted };
    });
    return;
  }

  // Nạp 1 lần duy nhất toàn bộ phòng (O(1) request thay vì N+1 request riêng lẻ cho từng rạp)
  try {
    const res = await api.get('/rooms');
    const allRooms = Array.isArray(res.data) ? res.data : [];

    // Nhóm danh sách phòng theo cinemaId
    const roomsByCinema = new Map();
    for (const r of allRooms) {
      if (!r.cinemaId) continue;
      if (!roomsByCinema.has(r.cinemaId)) {
        roomsByCinema.set(r.cinemaId, []);
      }
      roomsByCinema.get(r.cinemaId).push({
        id: r.id,
        name: r.name,
        type: r.type,
        status: r.status || 'Active',
        turnaroundTimeMins: r.turnaroundTimeMins ?? 15
      });
    }

    localCinemas.value = base.map(c => {
      const hallsList = (c.halls && c.halls.length)
        ? c.halls
        : (roomsByCinema.get(c.id) || []);
      const sorted = [...hallsList].sort((a, b) => (a.name || '').localeCompare(b.name || '', undefined, { numeric: true, sensitivity: 'base' }));
      return {
        id: c.id,
        name: c.name,
        city: c.city,
        halls: sorted
      };
    });
  } catch (e) {
    toast.error(friendlyError(e, 'Không tải được danh sách phòng chiếu.'));
    localCinemas.value = base.map(c => ({ id: c.id, name: c.name, city: c.city, halls: c.halls || [] }));
  }
};

// Kiểm tra tính tương thích giữa Định dạng chiếu và Loại phòng
const isRoomCompatible = (room, formatId) => {
  if (!formatId) return true;
  const fmt = formats.value.find(f => f.id === parseInt(formatId));
  if (!fmt) return true;
  
  const fmtName = (fmt.name || '').trim().toUpperCase();
  const roomType = (room.type || 'STANDARD').trim().toUpperCase();
  
  // 1. Định dạng SUPERPLEX / IMAX -> Chỉ hỗ trợ phòng SUPERPLEX
  if (fmtName.includes('SUPERPLEX') || fmtName.includes('IMAX')) {
    return roomType.includes('SUPERPLEX') || roomType.includes('IMAX');
  }
  
  // 2. Định dạng CINE COMFORT -> Chỉ hỗ trợ phòng CINE_COMFORT
  if (fmtName.includes('COMFORT') || fmtName.includes('CINE_COMFORT')) {
    return roomType.includes('COMFORT') || roomType.includes('CINE_COMFORT');
  }
  
  // 3. Định dạng phổ thông (2D, 3D...) -> Hỗ trợ mọi loại phòng
  return true;
};

// Tính toán số ngày chiếu và số suất dự kiến (Ngưỡng trần 500 suất)
const activeDaysCount = computed(() => {
  if (!form.dateFrom || !form.dateTo || form.dateFrom > form.dateTo) return 0;
  const start = new Date(form.dateFrom);
  const end = new Date(form.dateTo);
  let count = 0;
  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const dayVal = d.getDay() === 0 ? 7 : d.getDay(); // 1: T2 -> 7: CN
    if (!form.daysOfWeek.length || form.daysOfWeek.includes(dayVal)) {
      count++;
    }
  }
  return count;
});

const estimatedSlots = computed(() => {
  return activeDaysCount.value * form.roomIds.length * form.startTimes.length;
});

watch(() => props.isOpen, (open) => {
  if (open) {
    fetchOptions();
    buildCinemaTree();
    clearErrors();
    preview.value = null;
    const today = getLocalTodayStr();
    form.movieId = ''; form.formatId = '';
    form.dateFrom = today; form.dateTo = today;
    form.daysOfWeek = []; form.roomIds = []; form.startTimes = [];
    form.roomOffsetMins = 0;
    form.autoFillGaps = true;
  }
});

// Đổi bất kỳ tham số nào → preview cũ không còn đúng
watch(() => [form.movieId, form.formatId, form.dateFrom, form.dateTo,
  form.daysOfWeek.length, form.roomIds.length, form.startTimes.length, form.roomOffsetMins, form.autoFillGaps], () => { preview.value = null; });

// Khi đổi định dạng: Tự động lọc bỏ các phòng không còn tương thích
watch(() => form.formatId, (newFmtId) => {
  fieldErrors.formatId = '';
  if (!newFmtId) return;
  const allHalls = localCinemas.value.flatMap(c => c.halls || []);
  form.roomIds = form.roomIds.filter(roomId => {
    const room = allHalls.find(r => r.id === roomId);
    return room ? isRoomCompatible(room, newFmtId) : false;
  });
});

// Lỗi tự xóa khi người dùng bắt đầu sửa đúng trường đó
watch(() => form.movieId, () => { fieldErrors.movieId = ''; });
watch(() => form.formatId, () => { fieldErrors.formatId = ''; });
watch(() => form.roomIds.length, () => { fieldErrors.roomIds = ''; });
watch(() => form.dateFrom, () => { fieldErrors.dateFrom = ''; fieldErrors.dateTo = ''; });
watch(() => form.dateTo, () => { fieldErrors.dateTo = ''; });
watch(() => form.startTimes.length, () => { fieldErrors.startTimes = ''; });

const toggleRoom = (roomId) => {
  const i = form.roomIds.indexOf(roomId);
  if (i === -1) form.roomIds.push(roomId); else form.roomIds.splice(i, 1);
};

const getCinemaCompatibleHalls = (cinema) => {
  return (cinema.halls || []).filter(h => h.status === 'Active' && isRoomCompatible(h, form.formatId));
};

const cinemaAllSelected = (cinema) => {
  const validHalls = getCinemaCompatibleHalls(cinema);
  return validHalls.length > 0 && validHalls.every(h => form.roomIds.includes(h.id));
};

const toggleCinema = (cinema) => {
  const validHalls = getCinemaCompatibleHalls(cinema);
  const ids = validHalls.map(h => h.id);
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
  if (!newHour.value || !newMinute.value) return;
  const t = `${newHour.value}:${newMinute.value}`;
  if (form.startTimes.includes(t)) {
    toast.warning(`Mốc giờ ${t} đã tồn tại trong danh sách.`);
    return;
  }
  form.startTimes.push(t);
  form.startTimes.sort();
  newHour.value = '';
  newMinute.value = '';
  fieldErrors.startTimes = '';
};
const removeTime = (t) => { form.startTimes = form.startTimes.filter(x => x !== t); };

// Trả về phần tử DOM của trường lỗi ĐẦU TIÊN (theo thứ tự hiển thị) hoặc null nếu hợp lệ.
const validate = () => {
  clearErrors();
  if (!form.movieId) fieldErrors.movieId = 'Vui lòng chọn bộ phim cần lên lịch chiếu.';
  if (!form.formatId) fieldErrors.formatId = 'Vui lòng chọn định dạng chiếu cho phim.';
  if (!form.roomIds.length) fieldErrors.roomIds = 'Vui lòng chọn ít nhất một phòng chiếu khả dụng.';

  const todayStr = getLocalTodayStr();
  if (!form.dateFrom) {
    fieldErrors.dateFrom = 'Vui lòng chọn ngày bắt đầu.';
  } else if (form.dateFrom < todayStr) {
    fieldErrors.dateFrom = 'Ngày bắt đầu không được nhỏ hơn ngày hiện tại.';
  }

  if (!form.dateTo) {
    fieldErrors.dateTo = 'Vui lòng chọn ngày kết thúc.';
  } else if (form.dateFrom && form.dateTo < form.dateFrom) {
    fieldErrors.dateTo = 'Ngày kết thúc không được trước ngày bắt đầu.';
  } else if (form.dateFrom && form.dateTo) {
    const diffMs = new Date(form.dateTo) - new Date(form.dateFrom);
    const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
    if (diffDays > 30) {
      fieldErrors.dateTo = 'Khoảng thời gian tạo lịch tối đa không quá 30 ngày.';
    } else {
      const selectedMovie = movies.value.find(m => m.id === form.movieId);
      if (selectedMovie && selectedMovie.endDate) {
        const mEndStr = typeof selectedMovie.endDate === 'string' ? selectedMovie.endDate.slice(0, 10) : '';
        if (mEndStr && form.dateTo > mEndStr) {
          const parts = mEndStr.split('-');
          const formattedEnd = parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : mEndStr;
          fieldErrors.dateTo = `Vượt quá hạn phát hành của phim (kết thúc: ${formattedEnd}).`;
        }
      }
    }
  }

  if (!form.startTimes.length) {
    fieldErrors.startTimes = "Vui lòng thêm ít nhất một mốc giờ chiếu hoặc bấm 'Tự động rải ca'.";
  }

  if (estimatedSlots.value > 500) {
    toast.error(`Số lượng suất chiếu dự kiến (${estimatedSlots.value} suất) vượt quá giới hạn 500 suất/lần.`);
    return timesField.value;
  }

  const order = [
    ['movieId', movieField],
    ['formatId', formatField],
    ['roomIds', roomsField],
    ['dateFrom', dateFromField],
    ['dateTo', dateToField],
    ['startTimes', timesField]
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

// Preset features
const applyGoldenPreset = () => {
  if (!form.movieId) {
    fieldErrors.movieId = 'Vui lòng chọn bộ phim cần lên lịch chiếu.';
    focusFirstError(movieField);
    return;
  }
  if (!form.roomIds.length) {
    fieldErrors.roomIds = 'Vui lòng chọn ít nhất một phòng chiếu khả dụng.';
    focusFirstError(roomsField);
    return;
  }
  form.startTimes = ["10:00", "13:30", "17:00", "20:00", "22:30"];
  fieldErrors.startTimes = '';
  toast.success('Đã áp dụng Gói Khung Giờ Vàng (10:00, 13:30, 17:00, 20:00, 22:30).');
};

const clearTimes = () => {
  form.startTimes = [];
};

// Tự động rải ca: Tính bước nhảy theo thời gian dọn dẹp lớn nhất của các phòng đang chọn
const autoGenerateShifts = () => {
  if (!form.movieId) {
    fieldErrors.movieId = 'Vui lòng chọn bộ phim cần lên lịch chiếu.';
    focusFirstError(movieField);
    return;
  }
  if (!form.roomIds.length) {
    fieldErrors.roomIds = 'Vui lòng chọn ít nhất một phòng chiếu khả dụng.';
    focusFirstError(roomsField);
    return;
  }
  const movie = movies.value.find(m => m.id === form.movieId);
  if (!movie) return;
  const duration = movie.durationMins || movie.duration || 120;

  // Lấy thời gian dọn dẹp lớn nhất từ các phòng đang chọn (fallback 15 phút)
  const allHalls = localCinemas.value.flatMap(c => c.halls || []);
  const selectedHalls = allHalls.filter(h => form.roomIds.includes(h.id));
  const turnaround = selectedHalls.length > 0
    ? Math.max(...selectedHalls.map(h => Number(h.turnaroundTimeMins ?? 15)))
    : 15;
  const totalMins = duration + turnaround;

  let startMin = 8 * 60; // 08:00
  if (newHour.value && newMinute.value) {
    startMin = parseInt(newHour.value) * 60 + parseInt(newMinute.value);
  } else if (form.startTimes.length > 0) {
    const [h, m] = form.startTimes[0].split(':');
    startMin = parseInt(h) * 60 + parseInt(m);
  }

  const endDay = 23 * 60 + 30; // 23:30
  const generated = [];
  let currentMin = startMin;
  
  while (currentMin + totalMins <= endDay) {
    const h = Math.floor(currentMin / 60).toString().padStart(2, '0');
    const m = (currentMin % 60).toString().padStart(2, '0');
    generated.push(`${h}:${m}`);
    currentMin += totalMins;
  }
  
  const allSet = new Set([...form.startTimes, ...generated]);
  form.startTimes = Array.from(allSet).sort();
  newHour.value = '';
  newMinute.value = '';
  fieldErrors.startTimes = '';
  toast.success(`Đã rải ca tự động với bước nhảy ${totalMins} phút (Phim ${duration}p + dọn dẹp tối đa ${turnaround}p).`);
};

const hourOptions = computed(() => {
  let openH = 24, closeH = -1;
  const selectedCins = props.cinemas.filter(c => c.halls?.some(h => form.roomIds.includes(h.id)));
  
  if (selectedCins.length === 0) {
    openH = 8; closeH = 23;
  } else {
    selectedCins.forEach(c => {
      let oh = 8, ch = 23;
      if (c.openingTime) oh = parseInt(c.openingTime.split(':')[0]);
      if (c.closingTime) ch = parseInt(c.closingTime.split(':')[0]);
      if (oh < openH) openH = oh;
      
      // If closingTime is smaller than openingTime, it means it crosses midnight
      if (ch < oh) ch += 24; 
      if (ch > closeH) closeH = ch;
    });
    if (closeH >= 24) closeH -= 24; // map back to 0-23
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
    return { value: val, label: val, disabled: false };
  });
});

const minuteOptions = computed(() => {
  return Array.from({ length: 12 }, (_, i) => {
    const minVal = i * 5;
    const val = minVal.toString().padStart(2, '0');
    return { value: val, label: val, disabled: false };
  });
});

const buildPayload = (dryRun, force = false) => {
  const today = new Date();
  const year = today.getFullYear();
  const month = (today.getMonth() + 1).toString().padStart(2, '0');
  const day = today.getDate().toString().padStart(2, '0');
  const todayStr = `${year}-${month}-${day}`;
  
  const isToday = form.dateFrom === todayStr;
  let validTimes = [...form.startTimes];

  if (isToday) {
    const currentHour = today.getHours();
    const currentMin = today.getMinutes();
    validTimes = validTimes.filter(t => {
      const [th, tm] = t.split(':').map(Number);
      return th > currentHour || (th === currentHour && tm >= currentMin);
    });
  }

  return {
    movieId: parseInt(form.movieId),
    formatId: parseInt(form.formatId),
    roomIds: [...form.roomIds],
    dateFrom: form.dateFrom,
    dateTo: form.dateTo,
    daysOfWeek: [...form.daysOfWeek],
    startTimes: validTimes,
    roomOffsetMins: form.roomOffsetMins || 0,
    autoFillGaps: form.autoFillGaps !== false,
    dryRun,
    force
  };
};

const validateMovieDateRange = () => {
  const selectedMovie = movies.value.find(m => m.id === form.movieId);
  if (!selectedMovie) return false;
  
  if (!form.dateFrom || !form.dateTo) return false;
  const batchStart = new Date(form.dateFrom);
  batchStart.setHours(0,0,0,0);
  
  const batchEnd = new Date(form.dateTo);
  batchEnd.setHours(23,59,59,999);

  let isOutOfRange = false;
  if (selectedMovie.startDate) {
    const startDate = new Date(selectedMovie.startDate);
    startDate.setHours(0,0,0,0);
    if (!isNaN(startDate.getTime()) && batchStart < startDate) {
      isOutOfRange = true;
    }
  }

  if (selectedMovie.endDate) {
    const endDate = new Date(selectedMovie.endDate);
    endDate.setHours(23,59,59,999);
    if (!isNaN(endDate.getTime()) && batchEnd > endDate) {
      isOutOfRange = true;
    }
  }

  if (isOutOfRange) {
    toast.error(`Khoảng ngày tạo lịch nằm ngoài khoảng thời gian khởi chiếu/kết thúc của phim '${selectedMovie.title}'!`);
    return false;
  }
  return true;
};

const runPreview = async () => {
  const badField = validate();
  if (badField) { focusFirstError(badField); return; }
  if (!validateMovieDateRange()) return;
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

// Tạo lịch chiếu hàng loạt (chấp thuận cả các suất đêm hợp lệ kết thúc trước 03:30).
const runCreate = async () => {
  const { data } = await api.post('/showtimes/batch', buildPayload(false, true));
  toast.success(`Đã tạo ${data.createdCount} suất chiếu` +
    (data.skipped?.length ? `, bỏ qua ${data.skipped.length} suất.` : '.'));
  window.dispatchEvent(new Event('showtimes-updated'));
  emit('saved');
  emit('close');
};

const handleCreate = async () => {
  const badField = validate();
  if (badField) { focusFirstError(badField); return; }
  if (!validateMovieDateRange()) return;
  isBusy.value = true;
  try {
    await runCreate(false);
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
            <CustomSelect v-model="form.movieId" :options="movieOptions" :searchable="true" placeholder="-- Chọn phim --"
              @update:modelValue="() => { form.formatId = ''; fieldErrors.movieId = ''; }"
              :class="fieldErrors.movieId ? 'rounded-xl ring-1 ring-red-500/60' : ''" />
            <p v-if="fieldErrors.movieId" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.movieId }}</p>
          </div>
          <div ref="formatField">
            <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Định dạng</label>
            <CustomSelect v-model="form.formatId" :options="formatOptions" :disabled="!form.movieId" placeholder="-- Chọn định dạng --"
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
            <div v-for="c in localCinemas" :key="c.id" class="bg-black/20 border border-white/10 rounded-xl p-3">
              <label class="flex items-center gap-2 cursor-pointer mb-2">
                <input type="checkbox" :checked="cinemaAllSelected(c)" @change="toggleCinema(c)"
                  class="accent-primary w-4 h-4" />
                <span class="text-sm font-bold text-white">{{ c.name }}</span>
                <span class="text-[10px] text-white/40 uppercase tracking-widest">{{ c.city }}</span>
              </label>
              <div v-if="c.halls?.length" class="flex flex-wrap gap-2 pl-6">
                <div v-for="h in c.halls.filter(r => r.status === 'Active')" :key="h.id">
                  <button type="button" @click="isRoomCompatible(h, form.formatId) && toggleRoom(h.id)"
                    :disabled="!isRoomCompatible(h, form.formatId)"
                    :title="!isRoomCompatible(h, form.formatId) ? `Phòng ${h.name} (${h.type || 'STANDARD'}) không hỗ trợ định dạng này` : ''"
                    :class="[
                      !isRoomCompatible(h, form.formatId)
                        ? 'opacity-35 cursor-not-allowed bg-white/5 border-white/5 text-white/30'
                        : form.roomIds.includes(h.id)
                          ? 'bg-primary/20 border-primary text-primary'
                          : 'bg-white/5 border-white/10 text-white/60 hover:border-white/30'
                    ]"
                    class="px-3 py-1.5 rounded-lg border text-[11px] font-bold transition-all flex items-center gap-1.5">
                    <span>{{ h.name }}</span>
                    <span v-if="h.type && h.type !== 'STANDARD'" class="text-[9px] px-1 py-0.5 rounded bg-white/10 text-white/70">
                      {{ h.type.replace('_', ' ') }}
                    </span>
                  </button>
                </div>
              </div>
              <p v-else class="pl-6 text-[11px] text-white/30 italic">Chưa có phòng</p>
            </div>
          </div>
          <p v-if="fieldErrors.roomIds" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.roomIds }}</p>
        </div>

        <!-- Khoảng ngày -->
        <div class="grid grid-cols-2 gap-4">
          <div ref="dateFromField">
            <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Từ ngày</label>
            <input type="date" v-model="form.dateFrom"
              :class="fieldErrors.dateFrom ? 'border-red-500/60 ring-1 ring-red-500/60' : 'border-white/10'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-white outline-none focus:border-primary/50 transition-colors" />
            <p v-if="fieldErrors.dateFrom" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.dateFrom }}</p>
          </div>
          <div ref="dateToField">
            <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Đến ngày</label>
            <input type="date" v-model="form.dateTo"
              :class="fieldErrors.dateTo ? 'border-red-500/60 ring-1 ring-red-500/60' : 'border-white/10'"
              class="w-full bg-black/20 border rounded-xl px-4 py-3 text-white outline-none focus:border-primary/50 transition-colors" />
            <p v-if="fieldErrors.dateTo" aria-live="polite" class="text-[11px] text-red-400 font-bold mt-1.5">{{ fieldErrors.dateTo }}</p>
          </div>
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
          <div class="flex items-center justify-between mb-2">
            <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest">Khung giờ chiếu</label>
            <div class="flex gap-2">
              <button type="button" @click="applyGoldenPreset"
                :disabled="!form.movieId || !form.roomIds.length"
                :title="(!form.movieId || !form.roomIds.length) ? 'Vui lòng chọn Phim và Phòng trước khi áp dụng' : ''"
                class="text-[10px] font-bold text-primary bg-primary/10 px-2 py-1 rounded hover:bg-primary/20 transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
                Gói Khung Giờ Vàng
              </button>
              <button type="button" @click="autoGenerateShifts"
                :disabled="!form.movieId || !form.roomIds.length"
                :title="(!form.movieId || !form.roomIds.length) ? 'Vui lòng chọn Phim và Phòng trước khi rải ca' : ''"
                class="text-[10px] font-bold text-emerald-400 bg-emerald-500/10 px-2 py-1 rounded hover:bg-emerald-500/20 transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
                Tự động rải ca
              </button>
              <button type="button" @click="clearTimes" class="text-[10px] font-bold text-red-400 bg-red-500/10 px-2 py-1 rounded hover:bg-red-500/20 transition-colors">
                Xóa tất cả
              </button>
            </div>
          </div>
          <div class="flex gap-2">
            <CustomSelect v-model="newHour" :options="hourOptions" placeholder="Giờ" class="flex-1 min-w-0" />
            <CustomSelect v-model="newMinute" :options="minuteOptions" placeholder="Phút" class="flex-1 min-w-0" />
            <button type="button" @click="addTime"
              :disabled="!newHour || !newMinute"
              class="px-5 rounded-xl bg-primary/20 border border-primary/40 text-primary font-bold text-xs uppercase tracking-widest hover:bg-primary/30 transition-all shrink-0 disabled:opacity-40 disabled:cursor-not-allowed">
              Thêm
            </button>
          </div>
          <p class="text-[10px] text-white/40 mt-1">Mốc giờ phải nằm trong khung giờ mở cửa rạp.</p>
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

        <!-- Độ lệch so le giữa các phòng (Staggered Scheduling) -->
        <div>
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">
            Độ lệch so le giữa các phòng
          </label>
          <div class="grid grid-cols-3 gap-2">
            <button v-for="opt in staggerOptions" :key="opt.value" type="button"
              @click="form.roomOffsetMins = opt.value"
              :class="form.roomOffsetMins === opt.value
                ? 'bg-primary text-on-primary border-primary font-black shadow-md shadow-primary/20'
                : 'bg-white/5 border-white/10 text-white/70 hover:border-white/20 font-bold'"
              class="py-2.5 px-2 rounded-xl border text-xs text-center transition-all">
              {{ opt.label }}
            </button>
          </div>
          <p v-if="form.roomOffsetMins > 0 && form.roomIds.length > 1" class="text-[11px] text-emerald-400 font-medium mt-2 flex items-center gap-1.5">
            <span class="material-symbols-outlined text-[14px]">schedule</span>
            Phòng 1 theo giờ gốc, phòng 2 +{{ form.roomOffsetMins }}p, phòng 3 +{{ form.roomOffsetMins * 2 }}p...
          </p>
        </div>

        <!-- Tùy chọn Tự động lấp khoảng trống -->
        <label class="flex items-start gap-2.5 cursor-pointer select-none p-3 rounded-xl bg-white/5 border border-white/10 hover:border-white/20 transition-all group">
          <input type="checkbox" v-model="form.autoFillGaps" class="accent-primary w-4 h-4 rounded cursor-pointer mt-0.5" />
          <div class="flex flex-col">
            <span class="text-xs text-white font-bold group-hover:text-primary transition-colors">
              Tự động lấp khoảng trống vào mốc giờ tròn gần nhất
            </span>
            <span class="text-[11px] text-white/40 mt-0.5">
              Khi mốc giờ bị trùng lịch phòng, tự động chèn ca mới ngay khi phòng vừa dọn dẹp xong.
            </span>
          </div>
        </label>

        <!-- Widget ước tính số suất chiếu & Giới hạn trần 500 suất -->
        <div class="rounded-xl border p-3.5 transition-all"
          :class="estimatedSlots > 500
            ? 'bg-red-500/10 border-red-500/40 text-red-300'
            : 'bg-white/5 border-white/10 text-white/80'">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold" :class="estimatedSlots > 500 ? 'text-red-300' : 'text-white/90'">
              Số suất chiếu dự kiến tạo:
            </span>
            <span class="text-xs font-black" :class="estimatedSlots > 500 ? 'text-red-400' : 'text-primary'">
              {{ estimatedSlots }} suất
            </span>
          </div>
          <p class="text-[10px] mt-1 text-white/50">
            {{ activeDaysCount }} ngày chiếu × {{ form.roomIds.length }} phòng × {{ form.startTimes.length }} mốc giờ
          </p>
          <p v-if="estimatedSlots > 500" class="text-[11px] font-bold text-red-400 mt-2 flex items-center gap-1.5">
            <span class="material-symbols-outlined text-sm">error</span>
            Vượt quá giới hạn tối đa (500 suất/lần). Vui lòng thu hẹp khoảng ngày hoặc danh sách phòng.
          </p>
        </div>

        <!-- Thời gian dọn dẹp: lấy tự động theo từng phòng -->
        <p class="text-[11px] text-white/40 italic flex items-center gap-1.5">
          <span class="material-symbols-outlined text-[14px]">info</span>
          Thời gian dọn dẹp được tính tự động theo cấu hình lớn nhất của các phòng đang chọn khi rải ca.
        </p>

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
        <button @click="runPreview" :disabled="isBusy || estimatedSlots > 500 || estimatedSlots === 0"
          class="flex-1 py-3 rounded-xl bg-white/5 hover:bg-white/10 text-white font-bold uppercase tracking-widest text-xs transition-colors disabled:opacity-40 disabled:cursor-not-allowed">
          Xem trước
        </button>
        <button @click="handleCreate" :disabled="isBusy || estimatedSlots > 500 || estimatedSlots === 0"
          class="flex-1 py-3 rounded-xl bg-primary hover:brightness-110 text-on-primary font-bold uppercase tracking-widest text-xs shadow-lg shadow-primary/20 transition-all disabled:opacity-40 disabled:cursor-not-allowed">
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
