<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue';
import axios from 'axios';
import CustomSelect from './CustomSelect.vue';

const props = defineProps({
  isOpen: Boolean,
  cinemaId: Number,
});

const emit = defineEmits(['close', 'saved']);

const API_BASE_URL = "http://localhost:8080/api";

const form = reactive({
  movieId: '',
  roomId: '',
  formatId: '',
  startTime: '',
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
  return formats.value.map(f => ({ value: f.id, label: f.name }));
});

const fetchOptions = async () => {
  try {
    const [moviesRes, formatsRes] = await Promise.all([
      axios.get(`${API_BASE_URL}/movies`), // Assuming this exists
      axios.get(`${API_BASE_URL}/formats`)
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
    const res = await axios.get(`${API_BASE_URL}/rooms/cinema/${cinemaId}`);
    rooms.value = res.data;
  } catch (error) {
    console.error("Error fetching rooms:", error);
  }
};

watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    fetchOptions();
    fetchRooms(props.cinemaId);
    errorMsg.value = '';
    // Reset form
    form.movieId = '';
    form.roomId = '';
    form.formatId = '';
    form.startTime = '';
  }
});

const handleSave = async () => {
  if (!form.movieId || !form.roomId || !form.formatId || !form.startTime) {
    errorMsg.value = "Vui lòng nhập đầy đủ thông tin.";
    return;
  }
  
  try {
    errorMsg.value = '';
    await axios.post(`${API_BASE_URL}/showtimes`, {
      movieId: parseInt(form.movieId),
      roomId: parseInt(form.roomId),
      formatId: parseInt(form.formatId),
      startTime: form.startTime,
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
          <label class="block text-xs font-bold text-on-surface-variant uppercase tracking-widest mb-2">Thời gian bắt đầu</label>
          <input type="datetime-local" v-model="form.startTime" class="w-full bg-black/20 border border-white/10 rounded-xl px-4 py-3 text-white outline-none focus:border-primary/50 transition-colors" />
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
</style>
