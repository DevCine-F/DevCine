<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue';
import api from '@/api/axios';
import CustomSelect from './CustomSelect.vue';

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
  startMinute: '',
  cleaningTime: 15
});

const movies = ref([]);
const rooms = ref([]);
const formats = ref([]);
const errorMsg = ref('');

// Computed properties for CustomSelect options format
const movieOptions = computed(() => {
  return movies.value.map(m => ({ value: m.id, label: m.title || m.name }));
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
    console.error("Error fetching options:", error);
  }
};

const fetchRooms = async (cinemaId) => {
  if (!cinemaId) return;
  try {
    const res = await api.get(`/rooms/cinema/${cinemaId}`);
    rooms.value = res.data;
  } catch (error) {
    console.error("Error fetching rooms:", error);
  }
};

watch(() => form.movieId, () => {
  // Reset format selection when movie changes
  form.formatId = '';
});

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    fetchOptions();
    fetchRooms(props.cinemaId);
    errorMsg.value = '';
    // Reset form
    form.movieId = '';
    form.roomId = '';
    form.formatId = '';
    form.startHour = '';
    form.startMinute = '';
  }
});

const handleSave = async () => {
  if (!form.movieId || !form.roomId || !form.formatId || !form.startHour || !form.startMinute) {
    errorMsg.value = "Vui lòng nhập đầy đủ thông tin.";
    return;
  }
  
  try {
    // Combine selectedDate (e.g. "12/06") and time (e.g. "14" + "30")
    // Assuming current year for mock API
    const year = new Date().getFullYear();
    const [day, month] = props.selectedDate.split('/');
    const formattedStartTime = `${year}-${month}-${day}T${form.startHour}:${form.startMinute}:00`;

    await api.post('/showtimes', {
      movieId: parseInt(form.movieId),
      roomId: parseInt(form.roomId),
      formatId: parseInt(form.formatId),
      startTime: formattedStartTime,
      cleaningTime: parseInt(form.cleaningTime)
    });
    
    emit('saved');
    emit('close');
  } catch (error) {
    if (error.response && error.response.data) {
      errorMsg.value = error.response.data;
    } else {
      errorMsg.value = "Có lỗi xảy ra khi lưu suất chiếu.";
    }
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
        
        <div v-if="errorMsg" class="p-4 bg-red-500/10 border border-red-500/20 rounded-xl">
          <p class="text-red-400 text-sm font-medium flex items-center gap-2">
            <span class="material-symbols-outlined text-base">error</span>
            {{ errorMsg }}
          </p>
        </div>

        <div>
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Phim</label>
          <CustomSelect 
            v-model="form.movieId" 
            :options="movieOptions" 
            placeholder="-- Chọn Phim --" 
          />
        </div>

        <div>
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Phòng chiếu</label>
          <CustomSelect 
            v-model="form.roomId" 
            :options="roomOptions" 
            placeholder="-- Chọn Phòng chiếu --" 
          />
        </div>

        <div>
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Định dạng</label>
          <CustomSelect 
            v-model="form.formatId" 
            :options="formatOptions" 
            placeholder="-- Chọn Định dạng --" 
          />
        </div>

        <div>
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">
            Thời gian bắt đầu <span class="text-primary font-medium lowercase tracking-normal">(Suất chiếu cho ngày {{ selectedDate }}/{{ new Date().getFullYear() }})</span>
          </label>
          <div class="flex gap-4">
            <div class="flex-1">
              <CustomSelect 
                v-model="form.startHour" 
                :options="hourOptions" 
                placeholder="Giờ" 
              />
            </div>
            <div class="flex-1">
              <CustomSelect 
                v-model="form.startMinute" 
                :options="minuteOptions" 
                placeholder="Phút" 
              />
            </div>
          </div>
        </div>

        <div>
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Thời gian dọn dẹp (Phút)</label>
          <input type="number" v-model="form.cleaningTime" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-white outline-none focus:border-primary/50 transition-colors" />
        </div>

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
