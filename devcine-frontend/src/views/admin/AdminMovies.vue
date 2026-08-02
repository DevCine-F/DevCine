<script setup>
import { ref, computed, onMounted } from "vue";
import { useMovieManagement } from "@/composables/useMovieManagement";
import MovieToolbar from "@/components/admin/movies/MovieToolbar.vue";
import MovieTable from "@/components/admin/movies/MovieTable.vue";
import MovieFormModal from "@/components/admin/movies/MovieFormModal.vue";
import MovieDetailModal from "@/components/admin/movies/MovieDetailModal.vue";
import { useToastStore } from "@/stores/toast";
import { useConfirmStore } from "@/stores/confirm";
import { friendlyError } from "@/utils/friendlyError";
import { useAdminPerm } from "@/composables/useAdminPerm";

const { can } = useAdminPerm();
const mm = useMovieManagement();
const toast = useToastStore();
const confirm = useConfirmStore();

// ===== Modal state =====
const showFormModal = ref(false);
const isEditing = ref(false);
const formMovieData = ref(null);

const showDetailModal = ref(false);
const detailMovie = ref(null);

// ===== Handlers =====
const handleAdd = async () => {
  await mm.fetchCategories();
  isEditing.value = false;
  formMovieData.value = null;
  showFormModal.value = true;
};

const handleEdit = async (movieSummary) => {
  try {
    await mm.fetchCategories();
    formMovieData.value = await mm.getMovieDetail(movieSummary.id);
    isEditing.value = true;
    showDetailModal.value = false;
    showFormModal.value = true;
  } catch (error) {
    console.error("Lỗi khi lấy chi tiết phim:", error);
    toast.error(friendlyError(error, "Không thể tải thông tin chi tiết phim!"));
  }
};

const handleSave = async (payload, id) => {
  showFormModal.value = false; // optimistic: đóng ngay
  try {
    await mm.saveMovie(payload, id);
  } catch (error) {
    console.error("Lỗi khi lưu phim:", error);
    toast.error(friendlyError(error, "Lưu dữ liệu thất bại. Vui lòng tải lại trang!"));
  }
};

const handleRowClick = async (movieSummary) => {
  try {
    detailMovie.value = await mm.getMovieDetail(movieSummary.id);
    showDetailModal.value = true;
  } catch (error) {
    console.error("Lỗi khi lấy chi tiết phim:", error);
    toast.error(friendlyError(error, "Không thể tải thông tin chi tiết phim!"));
  }
};

const handleDelete = async (id) => {
  const ok = await confirm.show({
    title: "Xoá phim",
    message: "Bạn có chắc chắn muốn xoá bộ phim này?",
    confirmText: "Xoá",
    tone: "danger",
  });
  if (!ok) return;
  try {
    await mm.deleteMovie(id);
  } catch (error) {
    console.error("Lỗi khi xóa phim:", error);
    toast.error(friendlyError(error, "Xóa phim thất bại. Phim có thể đang được dùng trong suất chiếu."));
  }
};

const handleBulkStatus = async (status) => {
  const labels = { active: "Đang chiếu", upcoming: "Sắp chiếu", archived: "Lưu trữ" };
  const ok = await confirm.show({
    title: "Đổi trạng thái phim",
    message: `Đổi trạng thái ${mm.selectionCount.value} phim sang "${labels[status]}"?`,
    confirmText: "Đồng ý",
    tone: "primary",
  });
  if (!ok) return;
  try {
    await mm.bulkUpdateStatus(status);
  } catch (error) {
    console.error("Lỗi đổi trạng thái hàng loạt:", error);
    toast.error(friendlyError(error, "Thao tác thất bại. Vui lòng thử lại!"));
  }
};

const handleBulkDelete = async () => {
  const ok = await confirm.show({
    title: "Xoá phim đã chọn",
    message: `Xoá ${mm.selectionCount.value} phim đã chọn? Hành động không thể hoàn tác.`,
    confirmText: "Xoá",
    tone: "danger",
  });
  if (!ok) return;
  try {
    await mm.bulkDelete();
  } catch (error) {
    console.error("Lỗi xóa hàng loạt:", error);
    toast.error(friendlyError(error, "Xóa thất bại. Một số phim có thể đang được dùng trong suất chiếu."));
  }
};

const isSubmittingStatus = ref(false);

const handleQuickStatus = async ({ movie, status }) => {
  if (isSubmittingStatus.value) return;
  const newStatus = status;
  const oldStatus = movie.status;
  
  const revertUI = () => {
    movie.status = oldStatus;
    const realMovie = mm.pagedMovies.value.find(m => m.id === movie.id);
    if (realMovie) {
      realMovie.status = oldStatus;
      if (mm.statusToText) {
        realMovie.statusText = mm.statusToText(oldStatus);
        movie.statusText = mm.statusToText(oldStatus);
      }
    }
  };

  // LỚP 1: CHẶN "SẮP CHIẾU" NẾU NGÀY KHỞI CHIẾU <= HÔM NAY
  if (newStatus === 'upcoming') {
     const startStr = movie.startDate || movie.releaseDate;
     if (startStr) {
       const start = new Date(startStr);
       start.setHours(0, 0, 0, 0);
       const today = new Date();
       today.setHours(0, 0, 0, 0);
       if (start <= today) {
          toast.warning("Không thể chuyển thành Sắp chiếu vì ngày khởi chiếu đã/đang diễn ra!");
          revertUI();
          return; // BẮT BUỘC RETURN NGẮT LUỒNG NGAY TẠI ĐÂY!
       }
     }
  }

  // LỚP 2: CHẶN LỊCH CHIẾU CÒN ACTIVE (CHO CẢ 'upcoming' VÀ 'archived')
  if ((newStatus === 'archived' || newStatus === 'upcoming') && movie.hasActiveShowtimes) {
    toast.error(`Không thể ngừng chiếu '${movie.title}'. Hiện vẫn còn suất chiếu chưa hoàn tất!`);
    revertUI();
    return; // BẮT BUỘC RETURN NGẮT LUỒNG NGAY TẠI ĐÂY! TUYỆT ĐỐI KHÔNG BẬT MODAL HOẶC GỌI API!
  }

  // LỚP 3: NẾU HỢP LỆ VÀ LÀ "LƯU TRỮ" -> BẬT POPUP CONFIRM
  if (newStatus === 'archived') {
    const ok = await confirm.show({
      title: "XÁC NHẬN NGỪNG CHIẾU PHIM",
      message: `Bạn có chắc chắn muốn chuyển trạng thái phim '${movie.title}' sang LƯU TRỮ không? Phim sẽ bị ẩn khỏi danh sách bán vé trên Website và App.`,
      confirmText: "XÁC NHẬN",
      cancelText: "HỦY BỎ",
      tone: "danger",
    });
    if (!ok) {
      revertUI();
      return; // DỪNG LẠI CHỜ ADMIN BẤM POPUP, KHÔNG GỌI API TRỰC TIẾP
    }
  }
  
  // LỚP 4: THỰC THI CẬP NHẬT TRỰC TIẾP CHO CÁC TRƯỜNG HỢP HỢP LỆ KHÁC
  try {
    isSubmittingStatus.value = true;
    await mm.quickUpdateStatus(movie, newStatus);
  } catch (error) {
    console.error("Lỗi cập nhật trạng thái:", error);
    revertUI();
  } finally {
    isSubmittingStatus.value = false;
  }
};

// ===== Phân trang: dãy số trang gọn =====
const pageNumbers = computed(() => {
  const total = mm.totalPages.value;
  const current = mm.page.value;
  const pages = [];
  const add = (n) => pages.push(n);
  if (total <= 7) {
    for (let i = 1; i <= total; i++) add(i);
  } else {
    add(1);
    if (current > 3) add("…");
    for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) add(i);
    if (current < total - 2) add("…");
    add(total);
  }
  return pages;
});

// ===== Fix Logic Tự Động =====
const computeMovieStatus = (startDateStr, endDateStr) => {
  if (!startDateStr) return "upcoming";
  
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  
  const start = new Date(startDateStr);
  start.setHours(0, 0, 0, 0);
  
  let end = null;
  if (endDateStr) {
    end = new Date(endDateStr);
    end.setHours(23, 59, 59, 999);
  }
  
  if (start > today) return "upcoming";
  if (end && today > end) return "archived";
  return "active";
};

const enhancedPagedMovies = computed(() => {
  if (!mm.pagedMovies.value) return [];
  return mm.pagedMovies.value.map(movie => {
    return {
      ...movie,
      status: computeMovieStatus(movie.startDate || movie.releaseDate, movie.endDate)
    };
  });
});

const customReleaseInfo = (movie) => {
  const startStr = movie.startDate || movie.releaseDate;
  if (!startStr) return { label: "", tone: "none" };

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const start = new Date(startStr);
  start.setHours(0, 0, 0, 0);

  const diffMs = start.getTime() - today.getTime();
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays > 0) return { label: `CÒN ${diffDays} NGÀY`, tone: "future" };
  if (diffDays === 0) return { label: "KHỞI CHIẾU HÔM NAY", tone: "live" };
  return { label: `ĐÃ KHỞI CHIẾU ${Math.abs(diffDays)} NGÀY`, tone: "ended" };
};

onMounted(() => {
  mm.fetchMovies();
  mm.fetchCategories();
});
</script>

<template>
  <div class="p-10">
    <!-- Header -->
    <header class="flex justify-between items-center mb-12 gap-4 flex-wrap">
      <div>
        <h1 class="text-3xl font-bold font-headline uppercase text-on-surface">
          Quản lý <span class="text-primary">Phim</span>
        </h1>
        <p class="text-on-surface-variant text-sm mt-1 font-bold uppercase tracking-widest">
          Danh sách các phim trên hệ thống
        </p>
      </div>

      <div class="flex items-center gap-4">
        <div class="relative">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-sm">search</span>
          <input
            v-model="mm.searchQuery.value"
            type="text"
            placeholder="Tìm kiếm phim, đạo diễn..."
            class="bg-surface-container-high border-none text-xs pl-10 pr-4 py-3 rounded-sm w-72 focus:ring-1 focus:ring-primary transition-all text-on-surface"
          />
        </div>
        <button
          v-if="can('movies', 'add')"
          @click="handleAdd"
          class="bg-primary text-on-primary font-headline font-bold text-xs uppercase tracking-widest px-8 py-3 rounded-sm hover:brightness-110 active:scale-95 transition-all flex items-center gap-2"
        >
          <span class="material-symbols-outlined text-sm">add</span>
          Thêm Phim Mới
        </button>
      </div>
    </header>

    <!-- Toolbar: filters + bulk bar -->
    <MovieToolbar
      v-model:searchQuery="mm.searchQuery.value"
      v-model:filterStatus="mm.filterStatus.value"
      v-model:filterCountry="mm.filterCountry.value"
      v-model:filterRating="mm.filterRating.value"
      v-model:filterAgeRating="mm.filterAgeRating.value"
      v-model:filterGenre="mm.filterGenre.value"
      v-model:filterFormat="mm.filterFormat.value"
      :genres="mm.genres.value"
      :available-formats="mm.availableFormats.value"
      :available-age-ratings="mm.availableAgeRatings.value"
      :has-active-filters="mm.hasActiveFilters.value"
      :total-filtered="mm.totalFiltered.value"
      :selection-count="mm.selectionCount.value"
      :can-edit="can('movies', 'edit')"
      :can-delete="can('movies', 'delete')"
      @clear="mm.clearFilters"
      @clear-selection="mm.clearSelection"
      @bulk-status="handleBulkStatus"
      @bulk-delete="handleBulkDelete"
    />

    <!-- Loading -->
    <div v-if="mm.isLoading.value" class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-6 space-y-4">
      <div v-for="n in 6" :key="n" class="h-16 bg-surface-container-high/40 rounded animate-pulse"></div>
    </div>

    <!-- Error -->
    <div v-else-if="mm.loadError.value" class="bg-red-500/5 border border-red-500/20 rounded-lg p-16 text-center">
      <span class="material-symbols-outlined text-red-400/60 text-5xl mb-4">cloud_off</span>
      <p class="text-sm text-red-300/80 font-bold mb-4">Không tải được danh sách phim</p>
      <button @click="mm.fetchMovies" class="px-6 py-2.5 bg-red-500/10 text-red-300 border border-red-500/20 rounded-sm text-[10px] font-black uppercase tracking-widest hover:bg-red-500/20 transition-all">Thử lại</button>
    </div>

    <!-- Empty -->
    <div v-else-if="mm.totalFiltered.value === 0" class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-16 text-center">
      <span class="material-symbols-outlined text-on-surface-variant/30 text-5xl mb-4">movie_filter</span>
      <p class="text-sm text-on-surface-variant font-bold mb-1">Không tìm thấy phim nào</p>
      <p class="text-xs text-on-surface-variant/50">Thử thay đổi bộ lọc hoặc thêm phim mới.</p>
    </div>

    <!-- Table + pagination -->
    <template v-else>
      <MovieTable
        :movies="enhancedPagedMovies"
        :selected-ids="mm.selectedIds.value"
        :sort-key="mm.sortKey.value"
        :sort-dir="mm.sortDir.value"
        :is-page-all-selected="mm.isPageAllSelected.value"
        :optimize-cloudinary-url="mm.optimizeCloudinaryUrl"
        :format-price="mm.formatPrice"
        :format-date="mm.formatDate"
        :release-info="customReleaseInfo"
        :can-edit="can('movies', 'edit')"
        :can-delete="can('movies', 'delete')"
        @sort="mm.setSort"
        @toggle-all="mm.toggleSelectAllOnPage"
        @toggle-select="mm.toggleSelect"
        @row-click="handleRowClick"
        @edit="handleEdit"
        @delete="handleDelete"
        @quick-status="handleQuickStatus"
      />

      <!-- Pagination -->
      <div class="flex items-center justify-between mt-6 flex-wrap gap-4">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">
          Trang {{ mm.page.value }} / {{ mm.totalPages.value }} · {{ mm.totalFiltered.value }} phim
        </p>
        <div class="flex items-center gap-2">
          <button
            @click="mm.goToPage(mm.page.value - 1)"
            :disabled="mm.page.value === 1"
            class="w-9 h-9 flex items-center justify-center rounded-sm bg-surface-container-high border border-outline-variant/10 text-on-surface-variant hover:text-primary hover:border-primary/40 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <span class="material-symbols-outlined text-base">chevron_left</span>
          </button>
          <template v-for="(p, i) in pageNumbers" :key="i">
            <span v-if="p === '…'" class="px-2 text-on-surface-variant/50 text-xs">…</span>
            <button
              v-else
              @click="mm.goToPage(p)"
              :class="p === mm.page.value ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:text-primary'"
              class="w-9 h-9 flex items-center justify-center rounded-sm border border-outline-variant/10 text-[11px] font-black transition-all"
            >{{ p }}</button>
          </template>
          <button
            @click="mm.goToPage(mm.page.value + 1)"
            :disabled="mm.page.value === mm.totalPages.value"
            class="w-9 h-9 flex items-center justify-center rounded-sm bg-surface-container-high border border-outline-variant/10 text-on-surface-variant hover:text-primary hover:border-primary/40 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <span class="material-symbols-outlined text-base">chevron_right</span>
          </button>
        </div>
      </div>
    </template>

    <!-- Modals -->
    <MovieFormModal
      :open="showFormModal"
      :is-editing="isEditing"
      :movie-data="formMovieData"
      :genres="mm.genres.value"
      :available-formats="mm.availableFormats.value"
      :available-age-ratings="mm.availableAgeRatings.value"
      @close="showFormModal = false"
      @save="handleSave"
    />

    <MovieDetailModal
      :open="showDetailModal"
      :movie="detailMovie"
      :optimize-cloudinary-url="mm.optimizeCloudinaryUrl"
      :format-price="mm.formatPrice"
      :format-date="mm.formatDate"
      :fetch-stats="mm.getMovieStats"
      @close="showDetailModal = false"
      @edit="handleEdit"
    />
  </div>
</template>
