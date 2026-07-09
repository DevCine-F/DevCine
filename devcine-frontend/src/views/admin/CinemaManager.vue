<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import ShowtimeDrawer from "@/components/admin/ShowtimeDrawer.vue";
import BatchShowtimeDrawer from "@/components/admin/BatchShowtimeDrawer.vue";

// Composables
import { useCinemas } from "@/composables/useCinemas";
import { useShowtimes } from "@/composables/useShowtimes";
import { useSeatLayout } from "@/composables/useSeatLayout";
import { useToastStore } from "@/stores/toast";
import { useAdminPerm } from "@/composables/useAdminPerm";

const { can, isAdmin } = useAdminPerm();
const toast = useToastStore();

// Organisms
import CinemaCard from "@/components/organisms/admin/CinemaCard.vue";
import CinemaStatsBar from "@/components/organisms/admin/CinemaStatsBar.vue";
import CinemaTabBar from "@/components/organisms/admin/CinemaTabBar.vue";
import CinemaInfrastructureTab from "@/components/organisms/admin/CinemaInfrastructureTab.vue";
import CinemaShowtimesTab from "@/components/organisms/admin/CinemaShowtimesTab.vue";
import CinemaStaffTab from "@/components/organisms/admin/CinemaStaffTab.vue";
import CinemaFnbTab from "@/components/organisms/admin/CinemaFnbTab.vue";
import CinemaAnalyticsTab from "@/components/organisms/admin/CinemaAnalyticsTab.vue";
import CinemaConfigTab from "@/components/organisms/admin/CinemaConfigTab.vue";
import CreateCinemaModal from "@/components/organisms/admin/CreateCinemaModal.vue";
import RoomFormModal from "@/components/organisms/admin/RoomFormModal.vue";
import CleaningSettingsModal from "@/components/organisms/admin/CleaningSettingsModal.vue";
import CinemaSeatMapView from "@/components/organisms/admin/CinemaSeatMapView.vue";
import ShowtimeDetailsDrawer from "@/components/organisms/admin/ShowtimeDetailsDrawer.vue";

const {
  cinemas,
  selectedCinema,
  isLoadingDetail,
  fetchCinemas,
  loadCinemaDetail,
  showCreateModal,
  newCinema,
  handleCreateCinema,
  showRoomModal,
  roomModalMode,
  editingRoom,
  openAddRoom,
  openEditRoom,
  submitRoom,
  deleteRoom
} = useCinemas();

// Xác nhận xoá phòng
const roomToDelete = ref(null);
const confirmDeleteRoom = (hall) => { roomToDelete.value = hall; };
const handleConfirmDelete = async () => {
  if (roomToDelete.value) await deleteRoom(roomToDelete.value);
  roomToDelete.value = null;
};

const {
  dates,
  selectedDate,
  isToday,
  getGridStyle,
  getEndTime,
  checkConflict,
  checkFormatMismatch,
  onDragStart,
  onDrop,
  handlePublish
} = useShowtimes(selectedCinema);

const {
  viewingHall,
  currentSeatMap,
  tempRows,
  tempCols,
  isSavingLayout,
  openHallDetail,
  resetMap,
  saveSeatLayout
} = useSeatLayout();

// Tab state
const activeTab = ref("infrastructure");
const tabs = [
  { id: "infrastructure", label: "Phòng chiếu", icon: "tv_gen" },
  { id: "showtimes", label: "Lịch chiếu", icon: "calendar_month" },
  { id: "staff", label: "Nhân sự", icon: "badge" },
  { id: "fnb", label: "F&B / Kho", icon: "fastfood" },
  { id: "analytics", label: "Phân tích", icon: "monitoring" },
  { id: "config", label: "Cấu hình", icon: "settings" },
];

// ===== Bộ lọc theo địa điểm (Tỉnh/TP + Quận/Huyện) =====
const filterCity = ref("Tất cả");
const filterDistrict = ref("Tất cả");

const cityOptions = computed(() => {
  const set = new Set(cinemas.value.map(c => c.city).filter(Boolean));
  return ["Tất cả", ...Array.from(set)];
});

const districtOptions = computed(() => {
  const pool = filterCity.value === "Tất cả"
    ? cinemas.value
    : cinemas.value.filter(c => c.city === filterCity.value);
  const set = new Set(pool.map(c => c.district).filter(Boolean));
  return ["Tất cả", ...Array.from(set)];
});

// Đổi tỉnh thì reset quận
watch(filterCity, () => { filterDistrict.value = "Tất cả"; });

const filteredCinemas = computed(() => cinemas.value.filter(c => {
  const matchCity = filterCity.value === "Tất cả" || c.city === filterCity.value;
  const matchDistrict = filterDistrict.value === "Tất cả" || c.district === filterDistrict.value;
  return matchCity && matchDistrict;
}));

const openCinemaDetail = (cinema) => {
  loadCinemaDetail(cinema);
};

const closeDetail = () => {
  selectedCinema.value = null;
  activeTab.value = "infrastructure";
};

// Drawer state
const showDrawer = ref(false);
const selectedShowtime = ref(null);
const showAddShowtimeDrawer = ref(false);
const showBatchShowtimeDrawer = ref(false);
const showSeatMapModal = ref(false);

const openShowtimeDetails = (show) => {
  selectedShowtime.value = show;
  showDrawer.value = true;
};

// Chỉ mở drawer Thêm suất chiếu khi cụm rạp đã có phòng chiếu
const handleAddShowtime = () => {
  if (!selectedCinema.value?.halls || selectedCinema.value.halls.length === 0) {
    toast.warning("Cụm rạp chưa có phòng chiếu. Vui lòng thêm phòng ở tab \"Phòng chiếu\" trước.");
    return;
  }
  showAddShowtimeDrawer.value = true;
};

const closeDrawer = () => {
  showDrawer.value = false;
  selectedShowtime.value = null;
};

// Time tracking
const currentTimeStr = ref("");
const currentMinuteOffset = ref(0);

const updateTime = () => {
  const now = new Date();
  currentTimeStr.value = now.toLocaleTimeString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
  });
  const hours = now.getHours();
  const minutes = now.getMinutes();
  currentMinuteOffset.value = (hours - 8) * 60 + minutes;
};

const currentTimeLeft = computed(() => {
  const totalMinutesInGrid = 16 * 60; // 8h to 24h
  const pct = (currentMinuteOffset.value / totalMinutesInGrid) * 100;
  return `${pct}%`;
});

let timer;
onMounted(() => {
  fetchCinemas();
  updateTime();
  timer = setInterval(updateTime, 60000);
});

onUnmounted(() => {
  clearInterval(timer);
});

// Settings modal
const showCleaningSettingsModal = ref(false);

</script>

<template>
  <div class="min-h-screen p-10 bg-surface font-sans text-on-surface overflow-x-hidden selection:bg-primary/30 selection:text-primary">
    <!-- List of Cinemas -->
    <div v-if="!selectedCinema" class="animate-in fade-in duration-500">
      <header class="flex justify-between items-center mb-12">
        <div>
          <h1 class="text-3xl font-extrabold tracking-tight font-headline uppercase text-on-surface flex items-center gap-3">
            <span class="material-symbols-outlined text-4xl text-primary">domain</span>
            Quản trị Cụm Rạp
          </h1>
          <p class="text-xs text-on-surface-variant mt-2 font-medium tracking-wide uppercase">
            Hệ thống quản lý chuỗi rạp chiếu phim DevCine
          </p>
        </div>
        <button
          v-if="isAdmin()"
          @click="showCreateModal = true"
          class="bg-primary text-on-primary font-headline font-bold text-xs uppercase tracking-widest px-8 py-4 rounded-sm hover:brightness-110 transition-all flex items-center gap-3 shadow-lg shadow-primary/20"
        >
          <span class="material-symbols-outlined text-lg font-bold">add_location</span>
          Thiết lập Cụm Rạp Mới
        </button>
      </header>

      <!-- Bộ lọc theo địa điểm -->
      <div class="flex flex-wrap items-center gap-4 mb-8">
        <span class="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-on-surface-variant">
          <span class="material-symbols-outlined text-base text-primary">filter_alt</span> Lọc theo địa điểm
        </span>
        <div class="relative">
          <select v-model="filterCity" class="bg-surface-container-high border border-outline-variant/20 text-sm text-on-surface rounded-xl pl-4 pr-9 py-2.5 outline-none focus:border-primary/50 transition-all appearance-none cursor-pointer">
            <option v-for="city in cityOptions" :key="city" :value="city" class="bg-surface-container-high">{{ city === 'Tất cả' ? 'Tất cả Tỉnh/TP' : city }}</option>
          </select>
          <span class="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-lg pointer-events-none">expand_more</span>
        </div>
        <div class="relative">
          <select v-model="filterDistrict" class="bg-surface-container-high border border-outline-variant/20 text-sm text-on-surface rounded-xl pl-4 pr-9 py-2.5 outline-none focus:border-primary/50 transition-all appearance-none cursor-pointer">
            <option v-for="d in districtOptions" :key="d" :value="d" class="bg-surface-container-high">{{ d === 'Tất cả' ? 'Tất cả Quận/Huyện' : d }}</option>
          </select>
          <span class="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 text-on-surface-variant/60 text-lg pointer-events-none">expand_more</span>
        </div>
        <button v-if="filterCity !== 'Tất cả' || filterDistrict !== 'Tất cả'" @click="filterCity = 'Tất cả'; filterDistrict = 'Tất cả'"
          class="text-[10px] font-bold uppercase tracking-widest text-primary hover:underline">Xoá lọc</button>
        <span class="ml-auto text-xs text-on-surface-variant">{{ filteredCinemas.length }} cụm rạp</span>
      </div>

      <div v-if="filteredCinemas.length" class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <CinemaCard
          v-for="cinema in filteredCinemas"
          :key="cinema.id"
          :cinema="cinema"
          @click="openCinemaDetail(cinema)"
        />
      </div>

      <!-- Không khớp bộ lọc -->
      <div v-else class="text-center py-20 bg-surface-container-low rounded-2xl border border-outline-variant/10">
        <span class="material-symbols-outlined text-5xl text-on-surface-variant/40 mb-3 block">location_off</span>
        <p class="text-on-surface-variant">Không có cụm rạp nào ở địa điểm này.</p>
      </div>
    </div>

    <!-- Detailed Cinema View -->
    <div v-else class="animate-in fade-in slide-in-from-bottom-8 duration-700">
      <header class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-10">
        <div class="flex items-center gap-6">
          <button
            @click="closeDetail"
            class="w-12 h-12 flex items-center justify-center rounded-2xl bg-surface-container-high border border-outline-variant/10 hover:border-primary/40 hover:text-primary transition-all group"
          >
            <span class="material-symbols-outlined text-xl group-hover:-translate-x-1 transition-transform">arrow_back</span>
          </button>
          <div>
            <div class="flex items-center gap-3 mb-1">
              <span class="px-2 py-0.5 bg-primary/20 text-primary text-[10px] font-black rounded uppercase tracking-widest">{{ selectedCinema.type }}</span>
              <span class="flex items-center gap-1 text-green-500 text-[10px] font-black uppercase tracking-widest">
                <span class="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse"></span> Hoạt động
              </span>
              <span v-if="isLoadingDetail" class="flex items-center gap-1.5 text-on-surface-variant text-[10px] font-black uppercase tracking-widest">
                <span class="w-3 h-3 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></span> Đang tải chi tiết
              </span>
            </div>
            <h1 class="text-4xl font-extrabold tracking-tight font-headline uppercase text-on-surface">
              {{ selectedCinema.name }}
            </h1>
          </div>
        </div>
      </header>

      <CinemaStatsBar :stats="selectedCinema.stats" />

      <div
        v-if="!viewingHall"
        class="bg-surface-container-low border border-outline-variant/10 rounded-3xl overflow-hidden shadow-2xl"
      >
        <CinemaTabBar 
          :tabs="tabs" 
          :active-tab="activeTab" 
          @update:activeTab="(id) => activeTab = id" 
        />

        <div class="p-10 min-h-[500px]">
          <CinemaInfrastructureTab
            v-if="activeTab === 'infrastructure'"
            :halls="selectedCinema.halls"
            :can-manage="isAdmin()"
            @open-hall="openHallDetail"
            @add-room="openAddRoom"
            @edit-room="openEditRoom"
            @delete-room="confirmDeleteRoom"
          />

          <CinemaShowtimesTab
            v-else-if="activeTab === 'showtimes'"
            :cinema="selectedCinema"
            :dates="dates"
            :selected-date="selectedDate"
            :is-today="isToday"
            :current-minute-offset="currentMinuteOffset"
            :current-time-left="currentTimeLeft"
            :get-grid-style="getGridStyle"
            :check-conflict="checkConflict"
            :check-format-mismatch="checkFormatMismatch"
            :get-end-time="getEndTime"
            :can-schedule="can('schedules', 'add')"
            :can-schedule-edit="can('schedules', 'edit')"
            @update:selectedDate="(d) => selectedDate = d"
            @open-settings="showCleaningSettingsModal = true"
            @add-showtime="handleAddShowtime"
            @open-batch="showBatchShowtimeDrawer = true"
            @publish="handlePublish"
            @dragstart="onDragStart"
            @drop="onDrop"
            @open-showtime="openShowtimeDetails"
          />

          <CinemaStaffTab
            v-else-if="activeTab === 'staff'"
            :staff="selectedCinema.staff"
          />

          <CinemaFnbTab
            v-else-if="activeTab === 'fnb'"
            :inventory="selectedCinema.inventory"
          />

          <CinemaAnalyticsTab
            v-else-if="activeTab === 'analytics'"
            :cinema="selectedCinema"
          />

          <CinemaConfigTab
            v-else-if="activeTab === 'config'"
            :cinema="selectedCinema"
          />
        </div>
      </div>

      <CinemaSeatMapView
        v-else
        :viewing-hall="viewingHall"
        :current-seat-map="currentSeatMap"
        :temp-rows="tempRows"
        :temp-cols="tempCols"
        :is-saving-layout="isSavingLayout"
        @back="viewingHall = null"
        @reset="resetMap"
        @save="saveSeatLayout"
        @update:layout="(data) => {
          tempRows = data.rows;
          tempCols = data.cols;
          currentSeatMap = data.seats;
        }"
      />
    </div>

    <CreateCinemaModal
      :show="showCreateModal"
      :new-cinema="newCinema"
      @close="showCreateModal = false"
      @create="handleCreateCinema"
    />

    <!-- Thêm / Sửa phòng chiếu -->
    <RoomFormModal
      :show="showRoomModal"
      :mode="roomModalMode"
      :initial="editingRoom"
      @close="showRoomModal = false"
      @submit="submitRoom"
    />

    <!-- Xác nhận xoá phòng -->
    <div v-if="roomToDelete" class="fixed inset-0 z-[130] flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl shadow-2xl w-full max-w-sm p-8 text-center">
        <span class="material-symbols-outlined text-5xl text-red-400 mb-3 block">warning</span>
        <h3 class="text-lg font-bold text-on-surface mb-2">Xoá phòng chiếu?</h3>
        <p class="text-sm text-on-surface-variant mb-6">
          Phòng <span class="font-bold text-on-surface">{{ roomToDelete.name }}</span> và toàn bộ ghế của phòng sẽ bị xoá. Thao tác không thể hoàn tác.
        </p>
        <div class="flex gap-3">
          <button @click="roomToDelete = null" class="flex-1 px-4 py-3 rounded-xl border border-white/10 text-on-surface-variant text-[10px] font-black uppercase tracking-widest hover:bg-white/5 transition-all">Huỷ</button>
          <button @click="handleConfirmDelete" class="flex-1 px-4 py-3 rounded-xl bg-red-500 text-white text-[10px] font-black uppercase tracking-widest hover:brightness-110 transition-all">Xoá</button>
        </div>
      </div>
    </div>

    <CleaningSettingsModal
      :show="showCleaningSettingsModal"
      :cinema="selectedCinema"
      @close="showCleaningSettingsModal = false"
      @save="(time) => {
        if (selectedCinema) selectedCinema.cleaningTime = time;
        showCleaningSettingsModal = false;
      }"
    />

    <ShowtimeDetailsDrawer
      :show="showDrawer"
      :show-seat-map-only="showSeatMapModal"
      :showtime="selectedShowtime"
      :cinema="selectedCinema"
      :get-end-time="getEndTime"
      @close="closeDrawer"
      @close-seat-map="showSeatMapModal = false"
      @open-seat-map="showSeatMapModal = true"
    />

    <ShowtimeDrawer
      :is-open="showAddShowtimeDrawer"
      :cinema-id="selectedCinema?.id"
      :selected-date="selectedDate"
      @close="showAddShowtimeDrawer = false"
      @saved="() => loadCinemaDetail(selectedCinema)"
    />

    <BatchShowtimeDrawer
      :is-open="showBatchShowtimeDrawer"
      :cinemas="cinemas"
      @close="showBatchShowtimeDrawer = false"
      @saved="() => loadCinemaDetail(selectedCinema)"
    />
  </div>
</template>

<style scoped>
.animate-in {
  animation-duration: 0.5s;
  animation-fill-mode: both;
}
.fade-in {
  animation-name: fade-in;
}
.slide-in-from-bottom-8 {
  animation-name: slide-in-from-bottom;
}
@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes slide-in-from-bottom {
  from { transform: translateY(20px); }
  to { transform: translateY(0); }
}
</style>
