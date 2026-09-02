<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import ShowtimeDrawer from "@/components/admin/ShowtimeDrawer.vue";
import BatchShowtimeDrawer from "@/components/admin/BatchShowtimeDrawer.vue";

const route = useRoute();
const router = useRouter();

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
import CinemaAnalyticsTab from "@/components/organisms/admin/CinemaAnalyticsTab.vue";
import CinemaConfigTab from "@/components/organisms/admin/CinemaConfigTab.vue";
import CreateCinemaModal from "@/components/organisms/admin/CreateCinemaModal.vue";
import RoomFormModal from "@/components/organisms/admin/RoomFormModal.vue";
import CinemaSeatMapView from "@/components/organisms/admin/CinemaSeatMapView.vue";
import ShowtimeDetailsDrawer from "@/components/organisms/admin/ShowtimeDetailsDrawer.vue";
import api from "@/api/axios";
import { friendlyError } from "@/utils/friendlyError";
import { useConfirmStore } from "@/stores/confirm";

const confirm = useConfirmStore();

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
const isDeletingRoom = ref(false);
const confirmDeleteRoom = (hall) => { roomToDelete.value = hall; };
const handleConfirmDelete = async () => {
  if (isDeletingRoom.value) return;
  isDeletingRoom.value = true;
  try {
    if (roomToDelete.value) await deleteRoom(roomToDelete.value);
  } finally {
    roomToDelete.value = null;
    isDeletingRoom.value = false;
  }
};

const {
  dates,
  selectedDate,
  selectedDateIso,
  isToday,
  isPastDate,
  gridCols,
  hourMarks,
  showNowIndicator,
  currentTimeLeft,
  getGridStyle,
  getEndTime,
  checkConflict,
  checkFormatMismatch,
  onDragStart,
  onDrop,
  handlePublish,
  prevWeek,
  nextWeek,
  goToday,
  weekOffset
} = useShowtimes(selectedCinema);

const {
  viewingHall,
  currentSeatMap,
  tempRows,
  tempCols,
  isSavingLayout,
  hasChanges,
  hasBookings,
  markDirty,
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
  { id: "analytics", label: "Phân tích", icon: "monitoring" },
  { id: "config", label: "Thông tin cấu hình", icon: "settings" },
];

// ===== Bộ lọc theo địa điểm (Tỉnh/TP + Quận/Huyện) =====
const filterCity = ref("Tất cả");
const filterDistrict = ref("Tất cả");

const cityOptions = computed(() => {
  const set = new Set(cinemas.value.map(c => c.city).filter(Boolean));
  return ["Tất cả", ...Array.from(set)];
});

// Chỉ liệt kê Quận/Huyện THỰC SỰ có cụm rạp trong Tỉnh/TP đang chọn (không show toàn bộ quận của tỉnh).
const districtOptions = computed(() => {
  if (filterCity.value === "Tất cả") return ["Tất cả"]; // chưa chọn tỉnh -> không liệt kê quận
  const pool = cinemas.value.filter(c => c.city === filterCity.value);
  const set = new Set(pool.map(c => c.district).filter(Boolean));
  return ["Tất cả", ...Array.from(set)];
});

// Đổi tỉnh thì reset lựa chọn quận.
watch(filterCity, () => { filterDistrict.value = "Tất cả"; });

const filteredCinemas = computed(() =>
  cinemas.value
    .filter(c => {
      const matchCity = filterCity.value === "Tất cả" || c.city === filterCity.value;
      const matchDistrict = filterDistrict.value === "Tất cả" || c.district === filterDistrict.value;
      return matchCity && matchDistrict;
    })
    .sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0))
);

const openCinemaDetail = (cinema) => {
  loadCinemaDetail(cinema);
  router.replace({
    query: {
      ...route.query,
      cinemaId: cinema.id,
      tab: activeTab.value || "infrastructure"
    }
  });
};

const handleTabChange = (tabId) => {
  activeTab.value = tabId;
  if (selectedCinema.value) {
    router.replace({
      query: {
        ...route.query,
        cinemaId: selectedCinema.value.id,
        tab: tabId
      }
    });
  }
};

const closeDetail = () => {
  selectedCinema.value = null;
  activeTab.value = "infrastructure";
  router.replace({
    query: {
      ...route.query,
      cinemaId: undefined,
      id: undefined,
      tab: undefined
    }
  });
};

const isSyncingRoute = ref(false);

const syncStateFromRoute = async () => {
  if (isSyncingRoute.value) return;
  isSyncingRoute.value = true;
  try {
    const cinemaIdFromQuery = route.query.cinemaId || route.query.id;
    const tabFromQuery = route.query.tab;

    if (tabFromQuery && tabs.some(t => t.id === tabFromQuery)) {
      activeTab.value = tabFromQuery;
    }

    if (cinemaIdFromQuery) {
      if (selectedCinema.value && String(selectedCinema.value.id) === String(cinemaIdFromQuery)) {
        return;
      }
      let target = cinemas.value.find(c => String(c.id) === String(cinemaIdFromQuery));
      if (!target) {
        try {
          const res = await api.get(`/v1/cinemas/${cinemaIdFromQuery}`);
          if (res.data) {
            target = res.data;
          }
        } catch (e) {
          console.warn("Không tìm thấy cụm rạp từ URL query:", cinemaIdFromQuery, e);
          router.replace({
            query: {
              ...route.query,
              cinemaId: undefined,
              id: undefined,
              tab: undefined
            }
          });
          return;
        }
      }
      if (target) {
        await loadCinemaDetail(target);
      }
    } else if (selectedCinema.value) {
      selectedCinema.value = null;
      activeTab.value = "infrastructure";
    }
  } finally {
    isSyncingRoute.value = false;
  }
};

watch(
  () => [route.query.cinemaId, route.query.id, route.query.tab],
  async ([newCinemaId, newId, newTab], [oldCinemaId, oldId, oldTab]) => {
    const currentId = newCinemaId || newId;
    const prevId = oldCinemaId || oldId;
    if (currentId !== prevId || newTab !== oldTab) {
      await syncStateFromRoute();
    }
  }
);

// Trạng thái hoạt động của rạp đang chọn (đồng bộ màu + nhãn với card & tab cấu hình)
const selectedCinemaStatusMeta = computed(() => {
  const s = (selectedCinema.value?.status || 'ACTIVE').toString().toUpperCase();
  if (s === 'MAINTENANCE') {
    return {
      label: 'Bảo trì',
      dot: 'bg-amber-400',
      badgeClass: 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
    };
  }
  if (s === 'CLOSED') {
    return {
      label: 'Đã đóng cửa / Ẩn',
      dot: 'bg-red-400',
      badgeClass: 'bg-red-500/10 text-red-400 border border-red-500/30'
    };
  }
  return {
    label: 'Đang hoạt động',
    dot: 'bg-emerald-400',
    badgeClass: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
  };
});

// Cụm rạp vừa bị xoá cứng từ tab Cấu hình -> gỡ khỏi list & đóng panel chi tiết.
const handleCinemaDeleted = (id) => {
  cinemas.value = cinemas.value.filter((c) => String(c.id) !== String(id));
  closeDetail();
};

// Drawer state
const showDrawer = ref(false);
const selectedShowtime = ref(null);   // object card timeline (id, roomId, movie, startTime...)
const showtimeDetail = ref(null);     // dữ liệu THỰC TẾ từ API /showtimes/{id}/detail
const isLoadingShowtimeDetail = ref(false);
const showAddShowtimeDrawer = ref(false);
const showBatchShowtimeDrawer = ref(false);
const showSeatMapModal = ref(false);
const showtimeSeatData = ref(null);     // ShowtimeSeatResponse THỰC TẾ cho modal sơ đồ ghế
const isLoadingSeatMap = ref(false);

// Mở modal sơ đồ ghế: tải sơ đồ ghế THẬT của suất (từng ghế + trạng thái SOLD/HOLD/MAINTENANCE)
// qua /seats/showtime/{id}?channel=POS — cùng nguồn dữ liệu POS/Booking/Incident (không mock).
const openSeatMap = async () => {
  const st = selectedShowtime.value;
  if (!st) return;
  showSeatMapModal.value = true;
  showtimeSeatData.value = null;
  isLoadingSeatMap.value = true;
  try {
    const { data } = await api.get(`/seats/showtime/${st.id}`, { params: { channel: "POS" } });
    showtimeSeatData.value = data;
  } catch (e) {
    toast.error(friendlyError(e, "Không tải được sơ đồ ghế của suất chiếu."));
  } finally {
    isLoadingSeatMap.value = false;
  }
};

const openShowtimeDetails = async (show) => {
  selectedShowtime.value = show;
  showtimeDetail.value = null;
  showDrawer.value = true;
  isLoadingShowtimeDetail.value = true;
  try {
    const { data } = await api.get(`/showtimes/${show.id}/detail`);
    showtimeDetail.value = data;
  } catch (e) {
    toast.error(friendlyError(e, "Không tải được chi tiết suất chiếu."));
  } finally {
    isLoadingShowtimeDetail.value = false;
  }
};

// Xoá suất chiếu — backend chặn nếu đã có vé bán/giữ (yêu cầu hoàn/huỷ vé trước).
const handleDeleteShowtime = async () => {
  const st = selectedShowtime.value;
  if (!st) return;
  const ok = await confirm.show({
    title: "Xoá suất chiếu",
    message: "Xác nhận xoá suất chiếu này? Thao tác không thể hoàn tác.",
    confirmText: "Xoá suất chiếu",
    cancelText: "Đóng",
    tone: "danger",
  });
  if (!ok) return;
  try {
    await api.delete(`/showtimes/${st.id}`);
    toast.success("Đã xoá suất chiếu.");
    window.dispatchEvent(new Event('showtimes-updated'));
    closeDrawer();
    await loadCinemaDetail(selectedCinema.value);
  } catch (e) {
    toast.error(friendlyError(e, "Không thể xoá suất chiếu."));
  }
};

// Chỉ mở drawer Thêm suất chiếu khi cụm rạp đã có phòng chiếu và ngày không ở quá khứ
const handleAddShowtime = () => {
  if (isPastDate.value) {
    toast.warning("Không thể thêm suất chiếu cho ngày trong quá khứ.");
    return;
  }
  if (!selectedCinema.value?.halls || selectedCinema.value.halls.length === 0) {
    toast.warning("Cụm rạp chưa có phòng chiếu. Vui lòng thêm phòng ở tab \"Phòng chiếu\" trước.");
    return;
  }
  showAddShowtimeDrawer.value = true;
};

const handleOpenBatch = () => {
  if (isPastDate.value) {
    toast.warning("Không thể tạo suất chiếu hàng loạt cho ngày trong quá khứ.");
    return;
  }
  showBatchShowtimeDrawer.value = true;
};

const closeDrawer = () => {
  showDrawer.value = false;
  selectedShowtime.value = null;
  showtimeDetail.value = null;
};

const handleShowtimesUpdated = async () => {
  if (selectedCinema.value) {
    await loadCinemaDetail(selectedCinema.value);
  }
};

onMounted(async () => {
  await fetchCinemas();
  await syncStateFromRoute();
  window.addEventListener('showtimes-updated', handleShowtimesUpdated);
});

onUnmounted(() => {
  window.removeEventListener('showtimes-updated', handleShowtimesUpdated);
});

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
      <div v-if="isAdmin()" class="flex flex-wrap items-center gap-4 mb-8">
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
          <select v-model="filterDistrict" :disabled="filterCity === 'Tất cả'"
            :title="filterCity === 'Tất cả' ? 'Vui lòng chọn Tỉnh/TP trước' : ''"
            class="bg-surface-container-high border border-outline-variant/20 text-sm text-on-surface rounded-xl pl-4 pr-9 py-2.5 outline-none focus:border-primary/50 transition-all appearance-none cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed">
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
              <span class="px-2 py-0.5 bg-primary/20 text-primary text-[10px] font-black rounded uppercase tracking-widest">{{ selectedCinema.type || 'STANDARD' }}</span>
              <span
                class="flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-widest border"
                :class="selectedCinemaStatusMeta.badgeClass"
              >
                <span class="w-1.5 h-1.5 rounded-full" :class="[selectedCinemaStatusMeta.dot, selectedCinema.status !== 'CLOSED' && 'animate-pulse']"></span>
                {{ selectedCinemaStatusMeta.label }}
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

      <!-- Đã tạm ẩn Stat Cards thống kê theo yêu cầu -->
      <!-- <CinemaStatsBar :stats="selectedCinema.stats" /> -->

      <div
        v-if="!viewingHall"
        class="bg-surface-container-low border border-outline-variant/10 rounded-3xl overflow-hidden shadow-2xl"
      >
        <CinemaTabBar 
          :tabs="tabs.filter(t => !['staff', 'analytics'].includes(t.id))" 
          :active-tab="activeTab" 
          @update:activeTab="handleTabChange" 
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
            v-model:selected-date="selectedDate"
            :is-today="isToday"
            :is-past-date="isPastDate"
            :grid-cols="gridCols"
            :hour-marks="hourMarks"
            :show-now="showNowIndicator"
            :current-time-left="currentTimeLeft"
            :get-grid-style="getGridStyle"
            :check-conflict="checkConflict"
            :check-format-mismatch="checkFormatMismatch"
            :get-end-time="getEndTime"
            :can-schedule="can('schedules', 'add')"
            :can-schedule-edit="can('schedules', 'edit')"
            :week-offset="weekOffset"
            @prev-week="prevWeek"
            @next-week="nextWeek"
            @go-today="goToday"
            @update:selectedDate="(d) => selectedDate = d"
            @add-showtime="handleAddShowtime"
            @open-batch="handleOpenBatch"
            @open-showtime="openShowtimeDetails"
          />

          <CinemaStaffTab
            v-else-if="activeTab === 'staff'"
            :staff="selectedCinema.staff"
          />

          <CinemaAnalyticsTab
            v-else-if="activeTab === 'analytics'"
            :cinema="selectedCinema"
          />

          <CinemaConfigTab
            v-else-if="activeTab === 'config'"
            :cinema="selectedCinema"
            @deleted="handleCinemaDeleted"
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
        :has-changes="hasChanges"
        :has-bookings="hasBookings"
        @back="viewingHall = null"
        @reset="resetMap"
        @save="saveSeatLayout"
        @dirty="markDirty"
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
      :cinema="selectedCinema"
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
          <button @click="roomToDelete = null" :disabled="isDeletingRoom" class="flex-1 px-4 py-3 rounded-xl border border-white/10 text-on-surface-variant text-[10px] font-black uppercase tracking-widest hover:bg-white/5 transition-all disabled:opacity-50 disabled:cursor-not-allowed">Huỷ</button>
          <button @click="handleConfirmDelete" :disabled="isDeletingRoom" class="flex-1 px-4 py-3 rounded-xl bg-red-500 text-white text-[10px] font-black uppercase tracking-widest hover:brightness-110 transition-all disabled:opacity-50 disabled:cursor-not-allowed">{{ isDeletingRoom ? 'Đang xoá...' : 'Xoá' }}</button>
        </div>
      </div>
    </div>

    <ShowtimeDetailsDrawer
      :show="showDrawer"
      :show-seat-map-only="showSeatMapModal"
      :detail="showtimeDetail"
      :is-loading="isLoadingShowtimeDetail"
      :cinema="selectedCinema"
      :get-end-time="getEndTime"
      :seat-data="showtimeSeatData"
      :is-loading-seat-map="isLoadingSeatMap"
      @close="closeDrawer"
      @close-seat-map="showSeatMapModal = false"
      @open-seat-map="openSeatMap"
      @delete="handleDeleteShowtime"
    />

    <ShowtimeDrawer
      :is-open="showAddShowtimeDrawer"
      :cinema-id="selectedCinema?.id"
      :cinema="selectedCinema"
      :selected-date="selectedDate"
      :selected-date-iso="selectedDateIso"
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
