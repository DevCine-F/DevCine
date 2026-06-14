<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const API_BASE_URL = (import.meta.env.VITE_API_URL || "http://localhost:8080") + "/api";

const quotas = ref([]);
const movies = ref([]);
const cinemas = ref([]);
const isModalOpen = ref(false);

const newQuota = ref({
  movieId: null,
  cinemaId: null,
  targetShowtimesPerDay: 5,
  startDate: '',
  endDate: '',
  contractTerms: '',
  status: 'active'
});

const fetchData = async () => {
  try {
    const [qRes, mRes, cRes] = await Promise.all([
      axios.get(`${API_BASE_URL}/scheduling/quotas`),
      axios.get(`${API_BASE_URL}/movies`),
      axios.get(`${API_BASE_URL}/cinemas`)
    ]);
    quotas.value = qRes.data;
    movies.value = mRes.data;
    cinemas.value = cRes.data;
  } catch (error) {
    console.error("Error fetching data:", error);
  }
};

const saveQuota = async () => {
  try {
    const payload = {
        movie: { id: newQuota.value.movieId },
        cinema: { id: newQuota.value.cinemaId },
        targetShowtimesPerDay: newQuota.value.targetShowtimesPerDay,
        startDate: newQuota.value.startDate,
        endDate: newQuota.value.endDate,
        contractTerms: newQuota.value.contractTerms,
        status: newQuota.value.status
    };
    await axios.post(`${API_BASE_URL}/scheduling/quotas`, payload);
    await fetchData();
    isModalOpen.value = false;
  } catch (error) {
    console.error("Error saving quota:", error);
  }
};

const deleteQuota = async (id) => {
    if (!confirm("Xác nhận hủy điều phối này?")) return;
    await axios.delete(`${API_BASE_URL}/scheduling/quotas/${id}`);
    await fetchData();
};

onMounted(fetchData);
</script>

<template>
  <div class="p-10 space-y-8">
    <header class="flex justify-between items-end">
      <div>
        <h1 class="text-4xl font-extrabold tracking-tight font-headline uppercase italic text-primary">Master Scheduling</h1>
        <p class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold">Điều phối suất chiếu dựa trên hợp đồng nhà phát hành</p>
      </div>
      <button @click="isModalOpen = true" class="bg-primary text-on-primary px-6 py-3 rounded-sm font-bold uppercase tracking-widest hover:scale-105 transition-transform flex items-center gap-2 text-xs">
        <span class="material-symbols-outlined text-sm">assignment_add</span>
        Thiết lập phân bổ mới
      </button>
    </header>

    <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl overflow-hidden shadow-2xl">
        <table class="w-full text-left border-collapse">
            <thead>
                <tr class="bg-surface-container-high/50 text-[10px] font-black uppercase tracking-[0.2em] text-on-surface-variant">
                    <th class="p-4 border-b border-outline-variant/10">Phim</th>
                    <th class="p-4 border-b border-outline-variant/10">Cụm rạp</th>
                    <th class="p-4 border-b border-outline-variant/10 text-center">Suất/Ngày</th>
                    <th class="p-4 border-b border-outline-variant/10">Thời hạn</th>
                    <th class="p-4 border-b border-outline-variant/10">Trạng thái</th>
                    <th class="p-4 border-b border-outline-variant/10 text-right">Thao tác</th>
                </tr>
            </thead>
            <tbody class="text-sm font-medium">
                <tr v-for="quota in quotas" :key="quota.id" class="border-b border-outline-variant/5 hover:bg-white/5 transition-colors">
                    <td class="p-4">
                        <div class="font-bold text-on-surface uppercase italic tracking-tighter">{{ quota.movie?.title }}</div>
                    </td>
                    <td class="p-4">
                        <div class="text-xs text-on-surface-variant uppercase font-bold">{{ quota.cinema?.name }}</div>
                    </td>
                    <td class="p-4 text-center">
                        <span class="px-3 py-1 bg-primary/10 text-primary rounded-full font-black text-xs">{{ quota.targetShowtimesPerDay }} suất</span>
                    </td>
                    <td class="p-4">
                        <div class="text-[10px] text-on-surface-variant font-bold uppercase tracking-widest">{{ quota.startDate }} → {{ quota.endDate }}</div>
                    </td>
                    <td class="p-4">
                        <span :class="quota.status === 'active' ? 'text-green-500' : 'text-on-surface-variant'" class="text-[10px] font-black uppercase tracking-widest">{{ quota.status }}</span>
                    </td>
                    <td class="p-4 text-right">
                        <button @click="deleteQuota(quota.id)" class="p-2 hover:text-red-500 transition-colors">
                            <span class="material-symbols-outlined text-lg">delete_sweep</span>
                        </button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <!-- Modal -->
    <div v-if="isModalOpen" class="fixed inset-0 z-[1000] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/80 backdrop-blur-sm" @click="isModalOpen = false"></div>
        <div class="relative bg-surface-container-high border border-outline-variant/20 rounded-2xl w-full max-w-xl shadow-2xl overflow-hidden">
            <div class="p-6 border-b border-outline-variant/10 bg-white/5 flex justify-between items-center">
                <h3 class="font-headline font-black uppercase italic text-primary">Phân bổ suất chiếu hợp đồng</h3>
                <button @click="isModalOpen = false" class="material-symbols-outlined hover:text-red-400 transition-colors">close</button>
            </div>
            
            <div class="p-8 space-y-6">
                <div class="grid grid-cols-2 gap-6">
                    <div class="space-y-2">
                        <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Chọn Phim</label>
                        <select v-model="newQuota.movieId" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface outline-none">
                            <option v-for="m in movies" :key="m.id" :value="m.id">{{ m.title }}</option>
                        </select>
                    </div>
                    <div class="space-y-2">
                        <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Chọn Cụm rạp</label>
                        <select v-model="newQuota.cinemaId" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface outline-none">
                            <option v-for="c in cinemas" :key="c.id" :value="c.id">{{ c.name }}</option>
                        </select>
                    </div>
                </div>

                <div class="grid grid-cols-2 gap-6">
                    <div class="space-y-2">
                        <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Ngày bắt đầu</label>
                        <input v-model="newQuota.startDate" type="date" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface outline-none" />
                    </div>
                    <div class="space-y-2">
                        <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Ngày kết thúc</label>
                        <input v-model="newQuota.endDate" type="date" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface outline-none" />
                    </div>
                </div>

                <div class="space-y-2">
                    <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Số suất chiếu mục tiêu / ngày</label>
                    <input v-model="newQuota.targetShowtimesPerDay" type="number" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface outline-none" />
                </div>

                <div class="space-y-2">
                    <label class="text-[10px] font-black uppercase tracking-widest text-on-surface-variant">Ghi chú hợp đồng</label>
                    <textarea v-model="newQuota.contractTerms" class="w-full bg-surface-container-highest border border-outline-variant/20 p-4 rounded-xl text-sm font-bold text-on-surface outline-none h-32" placeholder="VD: Cam kết tối thiểu 5 suất tại phòng IMAX..."></textarea>
                </div>

                <div class="flex gap-4 pt-4">
                    <button @click="isModalOpen = false" class="flex-1 px-6 py-4 rounded-xl border border-outline-variant/20 text-[10px] font-black uppercase tracking-widest hover:bg-white/5 transition-colors">Hủy bỏ</button>
                    <button @click="saveQuota" class="flex-1 px-6 py-4 rounded-xl bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest hover:scale-[1.02] transition-transform">Lưu phân bổ</button>
                </div>
            </div>
        </div>
    </div>
  </div>
</template>

<style scoped>
</style>
