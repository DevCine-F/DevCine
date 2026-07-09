<script setup>
import { ref, computed, onMounted } from "vue";
import axios from "@/api/axios";
import { useAdminPerm } from "@/composables/useAdminPerm";

const { can } = useAdminPerm();
const range = ref("today");
const isLoading = ref(true);
const loadError = ref(false);

const ranges = [
  { key: "today", label: "Hôm nay" },
  { key: "week", label: "Tuần" },
  { key: "month", label: "Tháng" },
];

const data = ref({
  rangeLabel: "Hôm nay",
  revenue: { value: "0đ", trend: "0%" },
  tickets: { value: "0", trend: "0%" },
  newUsers: { value: "0", trend: "0%" },
  occupancy: { value: "0%", trend: "0%" },
  businessPerformance: [],
  topMovies: [],
  recentBookings: [],
  todayShowtimes: [],
  lowStock: [],
});

const colorMap = {
  primary: { bg: "bg-primary/10", text: "text-primary" },
  blue: { bg: "bg-blue-500/10", text: "text-blue-500" },
  purple: { bg: "bg-purple-500/10", text: "text-purple-500" },
  orange: { bg: "bg-orange-500/10", text: "text-orange-500" },
};

const revenueLabel = computed(() =>
  range.value === "month" ? "Doanh thu tháng" : range.value === "week" ? "Doanh thu 7 ngày" : "Doanh thu hôm nay"
);

const kpiCards = computed(() => [
  { id: "revenue", label: revenueLabel.value, item: data.value.revenue, icon: "payments", color: "primary" },
  { id: "tickets", label: "Vé đã bán", item: data.value.tickets, icon: "confirmation_number", color: "blue" },
  { id: "users", label: "Người dùng mới", item: data.value.newUsers, icon: "person_add", color: "purple" },
  { id: "occupancy", label: "Tỷ lệ lấp đầy", item: data.value.occupancy, icon: "chair", color: "orange" },
]);

const isTrendUp = (trend) => !String(trend || "").startsWith("-");

const occupancyValue = computed(() => {
  const n = parseFloat(String(data.value.occupancy.value).replace("%", ""));
  return isNaN(n) ? 0 : Math.min(100, n);
});

// ===== Biểu đồ SVG doanh thu + vé =====
const W = 700, H = 240, padL = 8, padR = 8, padT = 16, padB = 24;
const chartGeo = computed(() => {
  const pts = data.value.businessPerformance || [];
  const n = pts.length;
  const innerW = W - padL - padR;
  const innerH = H - padT - padB;
  if (!n) return { empty: true, revArea: "", revLine: "", tkLine: "", dots: [], labels: [], gridY: [] };

  const maxRev = Math.max(1, ...pts.map((p) => p.revenue || 0));
  const maxTk = Math.max(1, ...pts.map((p) => p.tickets || 0));
  const x = (i) => (n === 1 ? padL + innerW / 2 : padL + (i * innerW) / (n - 1));
  const yRev = (v) => padT + innerH * (1 - (v || 0) / maxRev);
  const yTk = (v) => padT + innerH * (1 - (v || 0) / maxTk);

  const revLine = pts.map((p, i) => `${x(i)},${yRev(p.revenue)}`).join(" ");
  const tkLine = pts.map((p, i) => `${x(i)},${yTk(p.tickets)}`).join(" ");
  const revArea = `${padL},${padT + innerH} ${revLine} ${x(n - 1)},${padT + innerH}`;

  const dots = pts.map((p, i) => ({
    x: x(i), yRev: yRev(p.revenue), yTk: yTk(p.tickets),
    revenueLabel: p.revenueLabel, ticketLabel: p.ticketLabel, label: p.label,
  }));

  const step = Math.ceil(n / 7);
  const labels = pts.map((p, i) => ({ x: x(i), text: p.label, show: i % step === 0 || i === n - 1 }));
  const gridY = [0, 0.25, 0.5, 0.75, 1].map((f) => padT + innerH * f);
  return { empty: false, revArea, revLine, tkLine, dots, labels, gridY };
});

const fetchStats = async () => {
  isLoading.value = true;
  loadError.value = false;
  try {
    const res = await axios.get("/dashboard/stats", { params: { range: range.value } });
    data.value = res.data;
  } catch (error) {
    console.error("Failed to fetch dashboard stats", error);
    loadError.value = true;
  } finally {
    isLoading.value = false;
  }
};

const changeRange = (key) => {
  if (range.value === key) return;
  range.value = key;
  fetchStats();
};

onMounted(fetchStats);
</script>

<template>
  <div class="p-6 md:p-10">
    <!-- Header -->
    <header class="mb-10 flex flex-col lg:flex-row lg:justify-between lg:items-end gap-6">
      <div>
        <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight font-headline uppercase italic text-primary">
          Bảng Điều khiển
        </h1>
        <p class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold">
          Hệ thống phân tích dữ liệu thời gian thực
        </p>
      </div>
      <div class="flex items-center gap-3 flex-wrap">
        <!-- Bộ lọc thời gian -->
        <div class="flex bg-surface-container-high rounded-lg p-1 border border-outline-variant/20">
          <button v-for="r in ranges" :key="r.key" @click="changeRange(r.key)"
            :class="range === r.key ? 'bg-primary text-black shadow' : 'text-on-surface-variant hover:text-on-surface'"
            class="px-4 py-1.5 rounded-md text-xs font-bold uppercase tracking-wider transition-all">
            {{ r.label }}
          </button>
        </div>
        <button
          v-if="can('dashboard_stats', 'export')"
          class="bg-surface-container-high px-4 py-2.5 rounded-lg border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-all text-on-surface-variant">
          Xuất báo cáo
        </button>
        <div
          class="bg-primary/10 border border-primary/20 px-4 py-2.5 rounded-lg text-primary text-[10px] font-bold uppercase tracking-widest flex items-center gap-2">
          <span class="relative flex h-2 w-2">
            <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
            <span class="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
          </span>
          Trực tiếp
        </div>
      </div>
    </header>

    <!-- Error -->
    <div v-if="loadError" class="text-center py-16 bg-surface-container-low rounded-xl border border-outline-variant/10">
      <span class="material-symbols-outlined text-5xl text-error mb-3 block">error</span>
      <p class="text-on-surface-variant mb-4">Không tải được dữ liệu thống kê.</p>
      <button @click="fetchStats" class="px-5 py-2 bg-primary text-black rounded-lg font-bold">Thử lại</button>
    </div>

    <template v-else>
      <!-- KPI -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div v-for="card in kpiCards" :key="card.id"
          class="bg-surface-container-low border border-outline-variant/10 p-6 rounded-xl relative overflow-hidden group hover:border-primary/40 transition-all shadow-sm">
          <div class="absolute -right-4 -bottom-4 opacity-5 group-hover:opacity-10 transition-opacity">
            <span class="material-symbols-outlined text-8xl">{{ card.icon }}</span>
          </div>
          <div class="relative z-10">
            <div class="flex justify-between items-start mb-6">
              <div :class="[colorMap[card.color].bg, colorMap[card.color].text]" class="p-3 rounded-xl">
                <span class="material-symbols-outlined">{{ card.icon }}</span>
              </div>
              <div v-if="!isLoading"
                :class="isTrendUp(card.item.trend) ? 'text-green-500 bg-green-500/10' : 'text-red-500 bg-red-500/10'"
                class="flex items-center gap-1 text-[10px] font-bold px-2 py-1 rounded">
                <span class="material-symbols-outlined" style="font-size: 14px">
                  {{ isTrendUp(card.item.trend) ? "trending_up" : "trending_down" }}
                </span>
                {{ card.item.trend }}
              </div>
            </div>
            <p class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant mb-2">{{ card.label }}</p>
            <h3 v-if="!isLoading" class="text-3xl font-black font-headline text-on-surface">{{ card.item.value }}</h3>
            <div v-else class="h-8 w-24 bg-white/5 rounded animate-pulse"></div>
          </div>
        </div>
      </div>

      <!-- Chart + Top movies -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        <!-- Chart -->
        <div class="lg:col-span-2 bg-surface-container-low border border-outline-variant/10 rounded-xl p-6 md:p-8">
          <div class="flex justify-between items-start mb-6">
            <div>
              <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface text-lg">Hiệu quả Kinh doanh</h3>
              <p class="text-[10px] text-on-surface-variant uppercase tracking-widest mt-1">
                Doanh thu & Lượng vé ({{ range === "month" ? "30 ngày qua" : "7 ngày qua" }})
              </p>
            </div>
            <div class="flex gap-4">
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 bg-primary rounded-full"></div>
                <span class="text-[10px] font-bold uppercase text-on-surface-variant">Doanh thu</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="w-3 h-3 bg-blue-500 rounded-full"></div>
                <span class="text-[10px] font-bold uppercase text-on-surface-variant">Lượng vé</span>
              </div>
            </div>
          </div>

          <div v-if="isLoading" class="h-64 bg-white/5 rounded-lg animate-pulse"></div>
          <svg v-else :viewBox="`0 0 ${W} ${H}`" class="w-full" style="height: 260px" preserveAspectRatio="none">
            <!-- grid -->
            <line v-for="(gy, i) in chartGeo.gridY" :key="i" :x1="padL" :x2="W - padR" :y1="gy" :y2="gy"
              class="stroke-on-surface" stroke-width="1" opacity="0.06" />
            <!-- revenue area + line -->
            <polygon :points="chartGeo.revArea" fill="url(#revGrad)" opacity="0.5" />
            <polyline :points="chartGeo.revLine" fill="none" class="stroke-primary" stroke-width="2.5"
              stroke-linejoin="round" stroke-linecap="round" />
            <!-- tickets line -->
            <polyline :points="chartGeo.tkLine" fill="none" class="stroke-blue-500" stroke-width="2" stroke-dasharray="4 4"
              stroke-linejoin="round" stroke-linecap="round" opacity="0.9" />
            <!-- dots + native tooltip -->
            <g v-for="(d, i) in chartGeo.dots" :key="'d' + i">
              <circle :cx="d.x" :cy="d.yRev" r="3" class="fill-primary" />
              <circle :cx="d.x" :cy="d.yTk" r="2.5" class="fill-blue-500" />
              <rect :x="d.x - 18" y="0" width="36" :height="H" fill="transparent">
                <title>{{ d.label }} · {{ d.revenueLabel }} · {{ d.ticketLabel }}</title>
              </rect>
            </g>
            <defs>
              <linearGradient id="revGrad" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" class="text-primary" stop-color="currentColor" stop-opacity="0.35" />
                <stop offset="100%" class="text-primary" stop-color="currentColor" stop-opacity="0" />
              </linearGradient>
            </defs>
          </svg>
          <div v-if="!isLoading" class="flex justify-between mt-2 text-[9px] font-bold uppercase tracking-widest text-on-surface-variant">
            <span v-for="(l, i) in chartGeo.labels" :key="i" :class="{ 'opacity-0': !l.show }">{{ l.text }}</span>
          </div>
        </div>

        <!-- Top movies + occupancy -->
        <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-6 md:p-8 flex flex-col">
          <div class="flex justify-between items-center mb-6">
            <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface">Phim hàng đầu</h3>
            <router-link to="/admin/movies" class="text-[10px] font-bold text-primary uppercase tracking-widest hover:underline">Xem tất cả</router-link>
          </div>

          <div v-if="isLoading" class="space-y-4 flex-grow">
            <div v-for="i in 4" :key="i" class="flex gap-3 items-center animate-pulse">
              <div class="w-10 h-14 bg-white/5 rounded"></div>
              <div class="flex-grow space-y-2"><div class="h-3 bg-white/5 rounded w-2/3"></div><div class="h-2 bg-white/5 rounded w-1/3"></div></div>
            </div>
          </div>
          <div v-else-if="!data.topMovies.length" class="flex-grow flex flex-col items-center justify-center text-center py-8">
            <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 mb-2">movie</span>
            <p class="text-xs text-on-surface-variant">Chưa có dữ liệu doanh thu phim.</p>
          </div>
          <div v-else class="space-y-4 flex-grow">
            <div v-for="(movie, i) in data.topMovies" :key="movie.title"
              class="flex items-center gap-3 group cursor-default p-2 hover:bg-white/5 transition-all rounded-lg">
              <span class="text-xs font-black text-on-surface-variant/40 w-4 text-center">{{ i + 1 }}</span>
              <div class="w-10 h-14 bg-surface-container-high rounded overflow-hidden flex-shrink-0 border border-outline-variant/10">
                <img :src="movie.posterUrl || '/images/Hopper.webp'" class="w-full h-full object-cover" />
              </div>
              <div class="flex-grow min-w-0">
                <h4 class="text-xs font-bold uppercase tracking-tight text-on-surface truncate">{{ movie.title }}</h4>
                <div class="flex justify-between mt-1 gap-2">
                  <span class="text-[10px] text-primary font-bold">{{ movie.revenue }}</span>
                  <span class="text-[10px] text-on-surface-variant font-bold">{{ movie.tickets }} vé</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Occupancy -->
          <div class="mt-6 p-5 bg-surface-container-high rounded-lg border border-outline-variant/10">
            <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-4">Tỷ lệ lấp đầy ({{ data.rangeLabel }})</p>
            <div class="flex items-center gap-4">
              <div class="relative w-16 h-16">
                <svg class="w-full h-full -rotate-90" viewBox="0 0 36 36">
                  <path class="text-surface stroke-current" stroke-width="3.5" fill="none"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                  <path class="text-primary stroke-current" stroke-width="3.5" :stroke-dasharray="`${occupancyValue}, 100`"
                    stroke-linecap="round" fill="none"
                    d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                </svg>
                <div class="absolute inset-0 flex items-center justify-center text-[11px] font-bold">{{ data.occupancy.value }}</div>
              </div>
              <div>
                <p class="text-xs font-bold text-on-surface uppercase tracking-tight">Công suất ghế</p>
                <p class="text-[10px] text-on-surface-variant font-bold mt-1">Vé bán / tổng ghế các suất</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 3 khu vực vận hành -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Giao dịch gần đây -->
        <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-6">
          <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-5 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary text-xl">receipt_long</span> Giao dịch gần đây
          </h3>
          <div v-if="isLoading" class="space-y-3">
            <div v-for="i in 5" :key="i" class="h-12 bg-white/5 rounded animate-pulse"></div>
          </div>
          <div v-else-if="!data.recentBookings.length" class="text-center py-8">
            <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 mb-2">inbox</span>
            <p class="text-xs text-on-surface-variant">Chưa có giao dịch nào.</p>
          </div>
          <div v-else class="space-y-1">
            <div v-for="b in data.recentBookings" :key="b.code"
              class="flex items-center gap-3 p-2.5 rounded-lg hover:bg-white/5 transition-all">
              <div class="w-9 h-9 rounded-lg bg-primary/10 text-primary flex items-center justify-center shrink-0">
                <span class="material-symbols-outlined text-base">confirmation_number</span>
              </div>
              <div class="flex-grow min-w-0">
                <p class="text-xs font-bold text-on-surface truncate">{{ b.movieTitle }}</p>
                <p class="text-[10px] text-on-surface-variant truncate">{{ b.customerName }} · {{ b.channel }} · {{ b.time }}</p>
              </div>
              <span class="text-xs font-bold text-primary shrink-0">{{ b.amount }}</span>
            </div>
          </div>
        </div>

        <!-- Suất chiếu hôm nay -->
        <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-6">
          <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-5 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary text-xl">event_seat</span> Suất chiếu hôm nay
          </h3>
          <div v-if="isLoading" class="space-y-3">
            <div v-for="i in 5" :key="i" class="h-12 bg-white/5 rounded animate-pulse"></div>
          </div>
          <div v-else-if="!data.todayShowtimes.length" class="text-center py-8">
            <span class="material-symbols-outlined text-4xl text-on-surface-variant/40 mb-2">event_busy</span>
            <p class="text-xs text-on-surface-variant">Hôm nay chưa có suất chiếu.</p>
          </div>
          <div v-else class="space-y-3 max-h-[360px] overflow-y-auto pr-1 no-scrollbar">
            <div v-for="(s, i) in data.todayShowtimes" :key="i" class="p-2.5 rounded-lg hover:bg-white/5 transition-all">
              <div class="flex items-center justify-between gap-2 mb-1.5">
                <div class="flex items-center gap-2 min-w-0">
                  <span class="text-xs font-black text-primary shrink-0">{{ s.time }}</span>
                  <span class="text-xs font-bold text-on-surface truncate">{{ s.movieTitle }}</span>
                </div>
                <span class="text-[10px] font-bold text-on-surface-variant shrink-0">{{ s.sold }}/{{ s.total }}</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="flex-grow h-1.5 bg-surface-container-high rounded-full overflow-hidden">
                  <div class="h-full bg-primary rounded-full" :style="{ width: Math.min(100, s.occupancy) + '%' }"></div>
                </div>
                <span class="text-[9px] font-bold text-on-surface-variant w-9 text-right">{{ s.occupancy.toFixed(0) }}%</span>
              </div>
              <p class="text-[10px] text-on-surface-variant mt-1">{{ s.cinemaName }} · {{ s.roomName }}</p>
            </div>
          </div>
        </div>

        <!-- Cảnh báo tồn kho F&B -->
        <div class="bg-surface-container-low border border-outline-variant/10 rounded-xl p-6">
          <h3 class="font-headline font-bold uppercase tracking-tight text-on-surface mb-5 flex items-center gap-2">
            <span class="material-symbols-outlined text-orange-500 text-xl">warning</span> Cảnh báo tồn kho
          </h3>
          <div v-if="isLoading" class="space-y-3">
            <div v-for="i in 5" :key="i" class="h-12 bg-white/5 rounded animate-pulse"></div>
          </div>
          <div v-else-if="!data.lowStock.length" class="text-center py-8">
            <span class="material-symbols-outlined text-4xl text-green-500/60 mb-2">check_circle</span>
            <p class="text-xs text-on-surface-variant">Kho ổn định, không có cảnh báo.</p>
          </div>
          <div v-else class="space-y-1">
            <div v-for="(item, i) in data.lowStock" :key="i"
              class="flex items-center gap-3 p-2.5 rounded-lg hover:bg-white/5 transition-all">
              <div class="w-9 h-9 rounded-lg bg-orange-500/10 text-orange-500 flex items-center justify-center shrink-0">
                <span class="material-symbols-outlined text-base">inventory_2</span>
              </div>
              <div class="flex-grow min-w-0">
                <p class="text-xs font-bold text-on-surface truncate">{{ item.name }}</p>
                <p class="text-[10px] text-on-surface-variant truncate">{{ item.cinemaName }}</p>
              </div>
              <span :class="item.inStock <= 5 ? 'text-red-500 bg-red-500/10' : 'text-orange-500 bg-orange-500/10'"
                class="text-[10px] font-bold px-2 py-1 rounded shrink-0">Còn {{ item.inStock }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
</style>
