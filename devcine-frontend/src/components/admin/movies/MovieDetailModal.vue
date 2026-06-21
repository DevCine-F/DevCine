<script setup>
import { ref, watch } from "vue";

const props = defineProps({
  open: { type: Boolean, default: false },
  movie: { type: Object, default: null }, // chi tiết phim đầy đủ
  optimizeCloudinaryUrl: { type: Function, required: true },
  formatPrice: { type: Function, required: true },
  formatDate: { type: Function, required: true },
  fetchStats: { type: Function, required: true }, // async (id) => stats
});

const emit = defineEmits(["close", "edit"]);

const isDescriptionExpanded = ref(false);
const stats = ref(null);
const statsLoading = ref(false);
const statsError = ref(false);

watch(
  [() => props.open, () => props.movie?.id],
  async ([isOpen, id]) => {
    if (!isOpen || !id) return;
    isDescriptionExpanded.value = false;
    stats.value = null;
    statsError.value = false;
    statsLoading.value = true;
    try {
      stats.value = await props.fetchStats(id);
    } catch (error) {
      console.error("Lỗi khi tải thống kê phim:", error);
      statsError.value = true;
    } finally {
      statsLoading.value = false;
    }
  },
  { immediate: true },
);

const durationText = (m) => (m?.durationMins ? m.durationMins + " Phút" : "N/A");
</script>

<template>
  <div v-if="open && movie" class="fixed inset-0 z-[100] flex items-center justify-center p-4 md:p-10">
    <div class="absolute inset-0 bg-[#0a0a0a]/90 backdrop-blur-[100px]" @click="emit('close')"></div>

    <div class="bg-white/[0.02] w-full max-w-7xl h-full max-h-[900px] rounded-[48px] border border-white/10 shadow-[0_40px_100px_rgba(0,0,0,0.8)] relative z-10 overflow-hidden flex flex-col md:flex-row">
      <!-- Sidebar poster -->
      <div class="w-full md:w-[480px] h-full relative shrink-0 overflow-hidden">
        <div class="absolute inset-0 z-0 bg-surface-container-highest">
          <img v-if="movie.posterUrl" :src="optimizeCloudinaryUrl(movie.posterUrl)" class="w-full h-full object-cover scale-110 blur-2xl opacity-30" />
        </div>
        <div class="relative z-10 h-full p-12 flex flex-col justify-between">
          <div class="aspect-[2/3] w-full rounded-[32px] overflow-hidden shadow-[0_30px_60px_rgba(0,0,0,0.6)] border border-white/10 group relative">
            <img v-if="movie.posterUrl" :src="optimizeCloudinaryUrl(movie.posterUrl)" class="w-full h-full object-cover transition-transform duration-1000 group-hover:scale-110" />
            <div v-else class="w-full h-full flex flex-col items-center justify-center bg-surface-container-highest text-on-surface-variant/40">
              <span class="material-symbols-outlined text-5xl">movie</span>
              <span class="text-[10px] font-bold tracking-widest mt-4 uppercase">Chưa có ảnh bìa</span>
            </div>
          </div>
          <div class="space-y-6 pt-10">
            <div class="flex items-center gap-3">
              <span class="px-4 py-1.5 bg-red-600/20 text-red-500 border border-red-500/20 text-[10px] font-black rounded-lg uppercase tracking-widest">{{ movie.ageRating || "P" }}</span>
              <span class="px-4 py-1.5 bg-primary/10 text-primary border border-primary/20 text-[10px] font-black rounded-lg uppercase tracking-widest">{{ movie.status === "active" ? "Đang chiếu" : movie.status === "upcoming" ? "Sắp chiếu" : "Lưu trữ" }}</span>
            </div>
            <div>
              <h2 class="text-4xl font-black text-white uppercase leading-tight mb-4">{{ movie.title }}</h2>
              <div class="flex items-center gap-4">
                <div class="flex items-center gap-2 py-1 px-3 bg-white/5 rounded-full border border-white/5">
                  <span class="material-symbols-outlined text-primary text-sm" style="font-variation-settings: 'FILL' 1">star</span>
                  <span class="text-sm font-black text-white">{{ movie.rating }}</span>
                  <span v-if="movie.ratingCount" class="text-[10px] text-white/50 font-bold ml-1">({{ movie.ratingCount.toLocaleString("vi-VN") }} lượt)</span>
                </div>
                <span class="text-[10px] font-bold text-white/40 uppercase tracking-widest">{{ durationText(movie) }}</span>
                <span class="text-[10px] font-bold text-white/40 uppercase tracking-widest">{{ movie.country }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Body -->
      <div class="flex-grow flex flex-col h-full bg-black/20 backdrop-blur-md border-l border-white/10">
        <div class="px-16 py-10 flex justify-between items-center shrink-0 border-b border-white/5">
          <div class="flex items-center gap-16">
            <div v-for="(info, i) in [
              { label: 'Ngôn ngữ gốc', val: movie.originalLanguage || 'Tiếng Anh' },
              { label: 'Phiên bản', val: movie.versionType || 'Phụ đề' },
              { label: 'Năm', val: movie.productionYear || '—' },
              { label: 'Hệ thống ID', val: '#' + movie.id },
            ]" :key="i" class="space-y-1">
              <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em]">{{ info.label }}</p>
              <p class="text-xs font-bold text-white uppercase">{{ info.val }}</p>
            </div>
          </div>
          <button @click="emit('close')" class="w-12 h-12 flex items-center justify-center rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-all">
            <span class="material-symbols-outlined text-white">close</span>
          </button>
        </div>

        <div class="flex-grow overflow-y-auto custom-scrollbar p-10">
          <div class="grid grid-cols-12 gap-10">
            <!-- Trái: thống kê THẬT -->
            <div class="col-span-7 space-y-10">
              <!-- Loading -->
              <div v-if="statsLoading" class="grid grid-cols-2 gap-6">
                <div v-for="n in 4" :key="n" class="p-8 bg-white/[0.02] border border-white/10 rounded-[24px] animate-pulse h-[180px]"></div>
              </div>

              <!-- Error -->
              <div v-else-if="statsError" class="p-8 bg-red-500/5 border border-red-500/20 rounded-[24px] text-center">
                <span class="material-symbols-outlined text-red-400/60 text-4xl mb-2">error</span>
                <p class="text-sm text-red-300/80 font-bold">Không tải được thống kê phim</p>
              </div>

              <template v-else-if="stats">
                <!-- Quick insights THẬT -->
                <div class="grid grid-cols-2 gap-6">
                  <div class="p-8 bg-white/[0.02] border border-white/10 rounded-[24px] group hover:bg-white/[0.04] transition-all">
                    <div class="flex justify-between items-start mb-6">
                      <span class="material-symbols-outlined text-white/30 text-3xl">payments</span>
                    </div>
                    <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em] mb-2">Doanh thu vé</p>
                    <p class="text-3xl font-bold text-white">{{ formatPrice(stats.ticketRevenue) }}</p>
                  </div>
                  <div class="p-8 bg-white/[0.02] border border-white/10 rounded-[24px] group hover:bg-white/[0.04] transition-all">
                    <div class="flex justify-between items-start mb-6">
                      <span class="material-symbols-outlined text-white/30 text-3xl">confirmation_number</span>
                    </div>
                    <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em] mb-2">Tổng vé đã bán</p>
                    <p class="text-3xl font-bold text-white">{{ stats.ticketsSold.toLocaleString("vi-VN") }} <span class="text-lg text-white/40 font-bold">vé</span></p>
                  </div>
                  <div class="p-8 bg-white/[0.02] border border-white/10 rounded-[24px] group hover:bg-white/[0.04] transition-all">
                    <div class="flex justify-between items-start mb-6">
                      <span class="material-symbols-outlined text-white/30 text-3xl">event_seat</span>
                    </div>
                    <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em] mb-2">Suất chiếu</p>
                    <p class="text-3xl font-bold text-white">{{ stats.showtimeCount }} <span class="text-lg text-white/40 font-bold">suất</span></p>
                  </div>
                  <div class="p-8 bg-white/[0.02] border border-white/10 rounded-[24px] flex flex-col justify-between">
                    <div>
                      <div class="flex justify-between items-start mb-6">
                        <span class="material-symbols-outlined text-white/30 text-3xl">chair</span>
                      </div>
                      <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em] mb-2">Tỷ lệ lấp đầy (suất đã chiếu)</p>
                    </div>
                    <div>
                      <p class="text-3xl font-bold text-white mb-3">{{ stats.occupancyRate }}%</p>
                      <div class="w-full h-2 bg-white/5 rounded-full overflow-hidden">
                        <div class="h-full bg-white rounded-full" :style="{ width: Math.min(stats.occupancyRate, 100) + '%' }"></div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Phân bổ doanh thu theo hạng vé THẬT -->
                <div class="p-8 bg-white/[0.02] border border-white/10 rounded-[24px]">
                  <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em] mb-6">Phân bổ doanh thu theo hạng vé</p>
                  <div v-if="stats.classDistribution?.length" class="space-y-4">
                    <div v-for="cls in stats.classDistribution" :key="cls.name" class="flex items-center gap-4">
                      <span class="text-xs font-bold text-white/60 w-20 truncate">{{ cls.name }}</span>
                      <div class="flex-grow h-2 bg-white/5 rounded-full overflow-hidden">
                        <div class="h-full bg-white/60 rounded-full" :style="{ width: cls.percentage + '%' }"></div>
                      </div>
                      <span class="text-xs font-bold text-white w-12 text-right">{{ cls.percentage }}%</span>
                    </div>
                  </div>
                  <p v-else class="text-xs text-white/40">Chưa có dữ liệu bán vé cho phim này.</p>
                </div>
              </template>
            </div>

            <!-- Phải: metadata THẬT -->
            <div class="col-span-5 pl-10 border-l border-white/10 space-y-12">
              <section class="space-y-4">
                <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em]">Nội dung phim</p>
                <div>
                  <p class="text-sm text-white/60 leading-relaxed font-medium transition-all duration-300" :class="{ 'line-clamp-4': !isDescriptionExpanded }">
                    {{ movie.description || "Chưa có tóm tắt nội dung chính thức cho bộ phim này." }}
                  </p>
                  <button @click="isDescriptionExpanded = !isDescriptionExpanded" class="text-[10px] text-white font-bold uppercase tracking-widest mt-2 hover:underline">
                    {{ isDescriptionExpanded ? "Thu gọn" : "Xem thêm" }}
                  </button>
                </div>
              </section>

              <section class="space-y-6">
                <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em]">Đội ngũ sản xuất</p>
                <div class="space-y-4">
                  <div>
                    <p class="text-[10px] font-bold text-white/40 uppercase tracking-widest mb-1">Đạo diễn</p>
                    <p class="text-sm font-medium text-white">{{ movie.director || "Đang cập nhật" }}</p>
                  </div>
                  <div>
                    <p class="text-[10px] font-bold text-white/40 uppercase tracking-widest mb-1">Diễn viên chính</p>
                    <p class="text-sm font-medium text-white">{{ movie.castMembers || "Đang cập nhật" }}</p>
                  </div>
                  <div>
                    <p class="text-[10px] font-bold text-white/40 uppercase tracking-widest mb-1">Nhà phát hành</p>
                    <p class="text-sm font-medium text-white">{{ movie.distributor || "Đang cập nhật" }}</p>
                  </div>
                </div>
              </section>

              <section class="space-y-6">
                <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em]">Lịch trình phát hành</p>
                <div class="space-y-6">
                  <div class="flex items-center gap-4">
                    <div class="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center border border-white/10">
                      <span class="material-symbols-outlined text-sm text-white/70">rocket_launch</span>
                    </div>
                    <div>
                      <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Khởi chiếu</p>
                      <p class="text-xs font-bold text-white">{{ formatDate(movie.startDate || movie.releaseDate) }}</p>
                    </div>
                  </div>
                  <div class="flex items-center gap-4">
                    <div class="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center border border-white/10">
                      <span class="material-symbols-outlined text-sm text-white/70">timer_off</span>
                    </div>
                    <div>
                      <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Kết thúc</p>
                      <p class="text-xs font-bold text-white">{{ formatDate(movie.endDate) }}</p>
                    </div>
                  </div>
                </div>
              </section>

              <section class="space-y-8">
                <div>
                  <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em] mb-4">Thể loại & Phân loại</p>
                  <div class="flex flex-wrap gap-2">
                    <span v-for="g in movie.genres" :key="g.id" class="px-3 py-1.5 bg-primary/10 border border-primary/20 text-[9px] font-bold text-primary uppercase tracking-widest rounded-lg">{{ g.name }}</span>
                  </div>
                </div>
                <div v-if="movie.format">
                  <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em] mb-4">Định dạng chiếu chính</p>
                  <div class="flex flex-wrap gap-2">
                    <span v-for="f in movie.format?.split(', ')" :key="f" class="px-3 py-1.5 bg-primary/10 border border-primary text-[9px] font-black text-primary uppercase tracking-widest rounded-lg">{{ f }}</span>
                  </div>
                </div>
                <div v-if="movie.supportedFormats">
                  <p class="text-[10px] font-bold text-white/50 uppercase tracking-[0.2em] mb-4">Định dạng hỗ trợ</p>
                  <div class="flex flex-wrap gap-2">
                    <span v-for="f in movie.supportedFormats?.split(', ')" :key="f" class="px-3 py-1.5 bg-white/10 border border-white/20 text-[9px] font-black text-white uppercase tracking-widest rounded-lg">{{ f }}</span>
                  </div>
                </div>
              </section>
            </div>
          </div>
        </div>

        <div class="px-16 py-8 shrink-0 bg-white/[0.02] border-t border-white/5 flex items-center justify-end">
          <button @click="emit('edit', movie)" class="px-10 py-4 bg-primary text-on-primary font-black text-[10px] uppercase tracking-[0.2em] rounded-2xl hover:brightness-110 hover:scale-[1.02] transition-all shadow-xl shadow-primary/20 flex items-center gap-2">
            <span class="material-symbols-outlined text-sm">edit_square</span>
            Cập nhật nội dung
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-track { background: rgba(0, 0, 0, 0.05); }
.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(var(--primary-rgb, 245, 197, 24), 0.3); border-radius: 10px; }
</style>
