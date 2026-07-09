<script setup>
import { ref } from "vue";

const props = defineProps({
  genres: { type: Array, default: () => [] },
  availableFormats: { type: Array, default: () => [] },
  availableAgeRatings: { type: Array, default: () => [] },
  hasActiveFilters: { type: Boolean, default: false },
  totalFiltered: { type: Number, default: 0 },
  selectionCount: { type: Number, default: 0 },
  canEdit: { type: Boolean, default: true },
  canDelete: { type: Boolean, default: true },
});

const emit = defineEmits(["clear", "bulk-status", "bulk-delete", "clear-selection"]);

// Hai chiều cho các bộ lọc
const searchQuery = defineModel("searchQuery", { default: "" });
const filterStatus = defineModel("filterStatus", { default: "Tất cả trạng thái" });
const filterCountry = defineModel("filterCountry", { default: "Tất cả quốc gia" });
const filterRating = defineModel("filterRating", { default: "Tất cả đánh giá" });
const filterAgeRating = defineModel("filterAgeRating", { default: "Tất cả độ tuổi" });
const filterGenre = defineModel("filterGenre", { default: "Tất cả phân loại" });
const filterFormat = defineModel("filterFormat", { default: "Tất cả định dạng" });

const isFilterOpen = ref(false);

const chipBase =
  "px-3 py-1.5 bg-surface-container-highest/50 border border-outline-variant/20 text-[10px] font-black uppercase tracking-widest text-on-surface flex items-center gap-3 rounded-sm";
const selectBase =
  "w-full bg-surface-container-high border border-outline-variant/10 text-[11px] font-bold uppercase tracking-wider rounded-lg py-3 px-4 text-on-surface focus:border-primary transition-all outline-none appearance-none";
</script>

<template>
  <section class="mb-8">
    <!-- Thanh thao tác hàng loạt (hiện khi có chọn) -->
    <transition name="fade">
      <div
        v-if="selectionCount > 0"
        class="bg-primary/10 border border-primary/30 p-4 rounded-lg flex items-center justify-between mb-4"
      >
        <div class="flex items-center gap-4">
          <span class="text-[11px] font-black uppercase tracking-widest text-primary">
            Đã chọn {{ selectionCount }} phim
          </span>
          <button
            @click="emit('clear-selection')"
            class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant hover:text-white transition-colors"
          >
            Bỏ chọn
          </button>
        </div>
        <div class="flex items-center gap-2">
          <button
            v-if="canEdit"
            @click="emit('bulk-status', 'active')"
            class="px-4 py-2 rounded-sm text-[10px] font-black uppercase tracking-widest bg-green-500/10 text-green-400 border border-green-500/20 hover:bg-green-500/20 transition-all"
          >Đang chiếu</button>
          <button
            v-if="canEdit"
            @click="emit('bulk-status', 'upcoming')"
            class="px-4 py-2 rounded-sm text-[10px] font-black uppercase tracking-widest bg-blue-500/10 text-blue-400 border border-blue-500/20 hover:bg-blue-500/20 transition-all"
          >Sắp chiếu</button>
          <button
            v-if="canEdit"
            @click="emit('bulk-status', 'archived')"
            class="px-4 py-2 rounded-sm text-[10px] font-black uppercase tracking-widest bg-surface-container-highest text-on-surface-variant border border-outline-variant/20 hover:brightness-125 transition-all"
          >Lưu trữ</button>
          <button
            v-if="canDelete"
            @click="emit('bulk-delete')"
            class="px-4 py-2 rounded-sm text-[10px] font-black uppercase tracking-widest bg-red-500/10 text-red-400 border border-red-500/20 hover:bg-red-500/20 transition-all flex items-center gap-1.5"
          >
            <span class="material-symbols-outlined text-sm">delete</span>
            Xóa
          </button>
        </div>
      </div>
    </transition>

    <!-- Hàng nút lọc + chips + đếm -->
    <div
      class="bg-surface-container-low border border-outline-variant/10 p-4 rounded-lg flex items-center justify-between gap-4 flex-wrap"
    >
      <div class="flex items-center gap-4 flex-wrap">
        <button
          @click="isFilterOpen = !isFilterOpen"
          :class="
            isFilterOpen
              ? 'bg-primary text-on-primary shadow-lg shadow-primary/20'
              : 'bg-surface-container-high text-on-surface-variant'
          "
          class="px-6 py-2.5 rounded-sm text-[10px] font-black uppercase tracking-widest flex items-center gap-3 transition-all hover:scale-[1.02] active:scale-95"
        >
          <span class="material-symbols-outlined text-sm">filter_list</span>
          Bộ lọc nâng cao
        </button>

        <div v-if="hasActiveFilters" class="h-8 w-px bg-outline-variant/20"></div>

        <div class="flex flex-wrap gap-2">
          <div v-if="filterStatus !== 'Tất cả trạng thái'" :class="chipBase">
            <span class="text-primary/60">Trạng thái:</span>{{ filterStatus }}
            <span @click="filterStatus = 'Tất cả trạng thái'" class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500">close</span>
          </div>
          <div v-if="filterCountry !== 'Tất cả quốc gia'" :class="chipBase">
            <span class="text-primary/60">Quốc gia:</span>{{ filterCountry }}
            <span @click="filterCountry = 'Tất cả quốc gia'" class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500">close</span>
          </div>
          <div v-if="filterAgeRating !== 'Tất cả độ tuổi'" :class="chipBase">
            <span class="text-primary/60">Độ tuổi:</span>{{ filterAgeRating }}
            <span @click="filterAgeRating = 'Tất cả độ tuổi'" class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500">close</span>
          </div>
          <div v-if="filterGenre !== 'Tất cả phân loại'" :class="chipBase">
            <span class="text-primary/60">Phân loại:</span>{{ filterGenre }}
            <span @click="filterGenre = 'Tất cả phân loại'" class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500">close</span>
          </div>
          <div v-if="filterFormat !== 'Tất cả định dạng'" :class="chipBase">
            <span class="text-primary/60">Định dạng:</span>{{ filterFormat }}
            <span @click="filterFormat = 'Tất cả định dạng'" class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500">close</span>
          </div>
          <div v-if="filterRating !== 'Tất cả đánh giá'" :class="chipBase">
            <span class="text-primary/60">Đánh giá:</span>{{ filterRating }}
            <span @click="filterRating = 'Tất cả đánh giá'" class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500">close</span>
          </div>
          <div v-if="searchQuery" :class="chipBase">
            <span class="text-primary/60">Từ khóa:</span>{{ searchQuery }}
            <span @click="searchQuery = ''" class="material-symbols-outlined text-[14px] cursor-pointer hover:text-red-500">close</span>
          </div>
        </div>
      </div>

      <p class="text-[10px] font-black uppercase tracking-[0.2em] text-primary">
        Hiển thị {{ totalFiltered }} bộ phim
      </p>
    </div>

    <!-- Lưới bộ lọc mở rộng -->
    <transition name="fade">
      <div
        v-if="isFilterOpen"
        class="bg-surface-container-low border-x border-b border-outline-variant/10 p-8 grid grid-cols-1 md:grid-cols-3 xl:grid-cols-7 gap-6 rounded-b-lg shadow-2xl relative z-20"
      >
        <div class="space-y-3">
          <label class="text-[9px] font-black uppercase tracking-[0.2em] text-white/30">Trạng thái vận hành</label>
          <select v-model="filterStatus" :class="selectBase">
            <option>Tất cả trạng thái</option>
            <option>Đang chiếu</option>
            <option>Sắp chiếu</option>
            <option>Lưu trữ</option>
          </select>
        </div>
        <div class="space-y-3">
          <label class="text-[9px] font-black uppercase tracking-[0.2em] text-white/30">Quốc gia sản xuất</label>
          <select v-model="filterCountry" :class="selectBase">
            <option>Tất cả quốc gia</option>
            <option>Mỹ</option>
            <option>Nhật Bản</option>
            <option>Anh</option>
            <option>Hàn Quốc</option>
            <option>Việt Nam</option>
            <option>Pháp</option>
          </select>
        </div>
        <div class="space-y-3">
          <label class="text-[9px] font-black uppercase tracking-[0.2em] text-white/30">Đánh giá cộng đồng</label>
          <select v-model="filterRating" :class="selectBase">
            <option>Tất cả đánh giá</option>
            <option>4.5 - 5.0 sao</option>
            <option>4.0 - 4.5 sao</option>
            <option>Dưới 4.0 sao</option>
          </select>
        </div>
        <div class="space-y-3">
          <label class="text-[9px] font-black uppercase tracking-[0.2em] text-white/30">Phân loại độ tuổi</label>
          <select v-model="filterAgeRating" :class="selectBase">
            <option>Tất cả độ tuổi</option>
            <option v-for="rating in availableAgeRatings" :key="rating.code" :value="rating.code">{{ rating.code }}</option>
          </select>
        </div>
        <div class="space-y-3">
          <label class="text-[9px] font-black uppercase tracking-[0.2em] text-white/30">Thể loại phim</label>
          <select v-model="filterGenre" :class="selectBase">
            <option>Tất cả phân loại</option>
            <option v-for="g in genres" :key="g.id" :value="g.name">{{ g.name }}</option>
          </select>
        </div>
        <div class="space-y-3">
          <label class="text-[9px] font-black uppercase tracking-[0.2em] text-white/30">Định dạng chiếu</label>
          <select v-model="filterFormat" :class="selectBase">
            <option>Tất cả định dạng</option>
            <option v-for="f in availableFormats" :key="f.name || f" :value="f.name || f">{{ f.name || f }}</option>
          </select>
        </div>
        <div class="flex items-end">
          <button
            @click="emit('clear')"
            class="w-full bg-surface-container-highest text-on-surface py-3.5 text-[10px] font-black uppercase tracking-widest hover:bg-red-500 hover:text-white transition-all border border-outline-variant/10 rounded-lg shadow-lg flex items-center justify-center gap-2"
          >
            <span class="material-symbols-outlined text-sm">restart_alt</span>
            Xóa tất cả bộ lọc
          </button>
        </div>
      </div>
    </transition>
  </section>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
