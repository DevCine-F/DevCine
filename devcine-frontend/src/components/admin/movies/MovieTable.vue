<script setup>
import { ref, onMounted, onUnmounted } from "vue";

const props = defineProps({
  movies: { type: Array, default: () => [] },
  selectedIds: { type: Object, required: true }, // Set
  sortKey: { type: String, default: "id" },
  sortDir: { type: String, default: "desc" },
  isPageAllSelected: { type: Boolean, default: false },
  optimizeCloudinaryUrl: { type: Function, required: true },
  formatPrice: { type: Function, required: true },
  formatDate: { type: Function, required: true },
  releaseInfo: { type: Function, required: true },
  canEdit: { type: Boolean, default: true },
  canDelete: { type: Boolean, default: true },
});

const emit = defineEmits([
  "sort",
  "toggle-all",
  "toggle-select",
  "row-click",
  "edit",
  "delete",
  "quick-status",
]);

const STATUS_OPTIONS = [
  { value: "active", label: "Đang chiếu" },
  { value: "upcoming", label: "Sắp chiếu" },
  { value: "archived", label: "Lưu trữ" },
];

const SORTABLE = {
  id: "ID",
  title: "Bộ phim",
  releaseDate: "Khởi chiếu",
  rating: "Đánh giá",
};

// Menu đổi trạng thái nhanh — lưu id dòng đang mở
const openStatusMenuId = ref(null);
const toggleStatusMenu = (id) => {
  openStatusMenuId.value = openStatusMenuId.value === id ? null : id;
};
const pickStatus = (movie, status) => {
  openStatusMenuId.value = null;
  if (status !== movie.status) emit("quick-status", { movie, status });
};

// Đóng menu trạng thái khi click ra ngoài (click trong ô trạng thái đã .stop nên không tới document)
const closeStatusMenu = () => (openStatusMenuId.value = null);
onMounted(() => document.addEventListener("click", closeStatusMenu));
onUnmounted(() => document.removeEventListener("click", closeStatusMenu));

const ageToneClass = (code) => {
  const c = (code || "").toUpperCase();
  if (c === "P") return "bg-green-500/10 text-green-400 border-green-500/20";
  if (c.includes("13")) return "bg-blue-500/10 text-blue-400 border-blue-500/20";
  if (c.includes("16")) return "bg-orange-500/10 text-orange-400 border-orange-500/20";
  if (c.includes("18")) return "bg-red-500/10 text-red-400 border-red-500/20";
  return "bg-surface-container-highest text-on-surface-variant border-outline-variant/10";
};

const releaseToneClass = (tone) =>
  ({
    future: "text-blue-400",
    live: "text-green-400/80",
    ending: "text-orange-400",
    ended: "text-on-surface-variant/40",
  })[tone] || "text-on-surface-variant/40";

const statusBadgeClass = (status) =>
  ({
    active: "bg-green-500/10 text-green-500 border-green-500/20",
    upcoming: "bg-blue-500/10 text-blue-400 border-blue-500/20",
    archived: "bg-surface-container-highest text-on-surface-variant border-outline-variant/10",
  })[status] || "bg-surface-container-highest text-on-surface-variant border-outline-variant/10";
</script>

<template>
  <section
    class="bg-surface-container-low border border-outline-variant/10 rounded-lg overflow-x-auto"
  >
    <table class="w-full text-left border-collapse min-w-[1100px]">
      <thead>
        <tr
          class="text-[10px] font-bold uppercase tracking-[0.15em] text-on-surface-variant border-b border-outline-variant/10 bg-surface-container-high/30"
        >
          <th class="px-5 py-5 w-10">
            <input
              type="checkbox"
              :checked="isPageAllSelected"
              @change="emit('toggle-all')"
              class="w-4 h-4 accent-primary cursor-pointer align-middle"
            />
          </th>
          <th
            v-for="(label, key) in SORTABLE"
            :key="key"
            @click="emit('sort', key)"
            :class="key === 'rating' ? 'text-right' : ''"
            class="px-5 py-5 cursor-pointer select-none hover:text-primary transition-colors whitespace-nowrap"
          >
            <span class="inline-flex items-center gap-1">
              {{ label }}
              <span
                class="material-symbols-outlined text-[14px] transition-opacity"
                :class="sortKey === key ? 'text-primary opacity-100' : 'opacity-30'"
              >{{ sortKey === key && sortDir === "asc" ? "arrow_upward" : "arrow_downward" }}</span>
            </span>
          </th>
          <th class="px-5 py-5">Phân loại</th>
          <th class="px-5 py-5">Độ tuổi</th>
          <th class="px-5 py-5">Trạng thái</th>
          <th class="px-5 py-5 text-right">Thao tác</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-outline-variant/10">
        <tr
          v-for="movie in movies"
          :key="movie.id"
          @click="emit('row-click', movie)"
          class="group hover:bg-white/5 transition-all text-on-surface cursor-pointer"
          :class="selectedIds.has(movie.id) ? 'bg-primary/5' : ''"
        >
          <!-- Checkbox -->
          <td class="px-5 py-4" @click.stop>
            <input
              type="checkbox"
              :checked="selectedIds.has(movie.id)"
              @change="emit('toggle-select', movie.id)"
              class="w-4 h-4 accent-primary cursor-pointer align-middle"
            />
          </td>

          <!-- ID -->
          <td class="px-5 py-4 text-[11px] font-black text-white/40 tracking-widest">
            #{{ movie.id }}
          </td>

          <!-- Bộ phim -->
          <td class="px-5 py-4">
            <div class="flex items-center gap-4">
              <div
                class="w-11 h-16 bg-surface-container-highest overflow-hidden rounded-sm relative border border-outline-variant/10 shrink-0"
              >
                <img
                  v-if="movie.posterUrl"
                  :src="optimizeCloudinaryUrl(movie.posterUrl)"
                  class="w-full h-full object-cover"
                />
                <div
                  v-else
                  class="w-full h-full flex items-center justify-center text-on-surface-variant/30"
                >
                  <span class="material-symbols-outlined text-xl">image</span>
                </div>
              </div>
              <div class="min-w-0">
                <p
                  class="font-bold text-sm uppercase tracking-tight group-hover:text-primary transition-colors truncate max-w-[260px]"
                >
                  {{ movie.title }}
                </p>
                <p
                  class="text-[10px] text-on-surface-variant mt-1 uppercase tracking-widest font-bold flex items-center gap-3"
                >
                  <span class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[11px]">schedule</span>
                    {{ movie.duration }}
                  </span>
                  <span v-if="movie.format" class="flex items-center gap-1">
                    <span class="material-symbols-outlined text-[11px]">movie_edit</span>
                    {{ movie.format }}
                  </span>
                  <span class="text-on-surface-variant/50">{{ movie.country }}</span>
                </p>
              </div>
            </div>
          </td>

          <!-- Khởi chiếu + badge vòng đời -->
          <td class="px-5 py-4">
            <p class="text-xs font-bold text-on-surface">{{ formatDate(movie.releaseDate) }}</p>
            <p
              v-if="releaseInfo(movie).label"
              class="text-[10px] font-black uppercase tracking-widest mt-0.5"
              :class="releaseToneClass(releaseInfo(movie).tone)"
            >
              {{ releaseInfo(movie).label }}
            </p>
          </td>

          <!-- Đánh giá -->
          <td class="px-5 py-4 text-right">
            <div class="flex items-center justify-end gap-1">
              <span
                class="material-symbols-outlined text-sm text-primary"
                style="font-variation-settings: 'FILL' 1"
              >star</span>
              <span class="text-xs font-black">{{ movie.rating || "—" }}</span>
              <span
                v-if="movie.ratingCount"
                class="text-[10px] text-on-surface-variant/60 font-bold"
              >({{ movie.ratingCount.toLocaleString("vi-VN") }})</span>
            </div>
          </td>

          <!-- Phân loại -->
          <td class="px-5 py-4">
            <div v-if="movie.genres?.length" class="flex flex-wrap gap-1 max-w-[180px]">
              <span
                v-for="g in movie.genres.slice(0, 3)"
                :key="g.id"
                class="px-2 py-0.5 bg-surface-container-highest/60 border border-outline-variant/10 text-[9px] font-bold uppercase tracking-wider rounded text-on-surface-variant"
              >{{ g.name }}</span>
              <span
                v-if="movie.genres.length > 3"
                class="px-2 py-0.5 text-[9px] font-bold text-on-surface-variant/50"
              >+{{ movie.genres.length - 3 }}</span>
            </div>
            <span v-else class="text-xs text-on-surface-variant/40">—</span>
          </td>

          <!-- Độ tuổi -->
          <td class="px-5 py-4">
            <span
              v-if="movie.ageRating"
              class="px-2.5 py-1 text-[10px] font-black uppercase tracking-widest rounded border"
              :class="ageToneClass(movie.ageRating)"
            >{{ movie.ageRating }}</span>
            <span v-else class="text-xs text-on-surface-variant/40">—</span>
          </td>

          <!-- Trạng thái (đổi nhanh) -->
          <td class="px-5 py-4 relative" @click.stop>
            <button
              v-if="canEdit"
              @click="toggleStatusMenu(movie.id)"
              class="px-3 py-1.5 text-[9px] font-black uppercase tracking-widest rounded-full border inline-flex items-center gap-1.5 hover:brightness-110 transition-all"
              :class="statusBadgeClass(movie.status)"
            >
              {{ movie.statusText }}
              <span class="material-symbols-outlined text-[13px]">expand_more</span>
            </button>
            <span
              v-else
              class="px-3 py-1.5 text-[9px] font-black uppercase tracking-widest rounded-full border inline-flex items-center"
              :class="statusBadgeClass(movie.status)"
            >
              {{ movie.statusText }}
            </span>
            <div
              v-if="openStatusMenuId === movie.id"
              class="absolute z-20 mt-1 left-5 bg-surface-container-high border border-outline-variant/20 rounded-lg shadow-2xl overflow-hidden min-w-[130px]"
            >
              <button
                v-for="opt in STATUS_OPTIONS"
                :key="opt.value"
                @click="pickStatus(movie, opt.value)"
                class="w-full text-left px-4 py-2.5 text-[10px] font-bold uppercase tracking-widest hover:bg-primary/10 transition-colors"
                :class="opt.value === movie.status ? 'text-primary' : 'text-on-surface-variant'"
              >
                {{ opt.label }}
              </button>
            </div>
          </td>

          <!-- Thao tác -->
          <td class="px-5 py-4 text-right" @click.stop>
            <div class="flex justify-end gap-2">
              <button
                v-if="canEdit"
                @click="emit('edit', movie)"
                title="Chỉnh sửa"
                class="w-9 h-9 flex items-center justify-center rounded-sm bg-surface-container-high border border-outline-variant/10 hover:border-primary/50 hover:text-primary transition-all"
              >
                <span class="material-symbols-outlined text-base">edit_note</span>
              </button>
              <button
                v-if="canDelete"
                @click="emit('delete', movie.id)"
                title="Xóa"
                class="w-9 h-9 flex items-center justify-center rounded-sm bg-surface-container-high border border-outline-variant/10 hover:border-red-500/50 hover:text-red-500 transition-all"
              >
                <span class="material-symbols-outlined text-base">delete_sweep</span>
              </button>
              <span v-if="!canEdit && !canDelete" class="text-on-surface-variant/40 text-xs">—</span>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
