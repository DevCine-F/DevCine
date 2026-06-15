<script setup>
import { ref, onMounted } from "vue";
import axios from "@/api/axios";

const isMonthlyRevenue = ref(false);
const isSeeding = ref(false);
const isLoading = ref(true);

const globalSeed = async () => {
  isSeeding.value = true;
  try {
    const res = await axios.post((import.meta.env.VITE_API_URL || "http://localhost:8080") + "/api/system/seed-all");
    alert(res.data);
    window.location.reload(); // Reload to see new data
  } catch (error) {
    console.error("Seeding error:", error);
    alert("Lỗi khi khởi tạo dữ liệu!");
  } finally {
    isSeeding.value = false;
  }
};

const stats = ref([
  {
    id: "revenue",
    label: "Doanh thu tháng",
    value: "0đ",
    trend: "+0%",
    icon: "payments",
    color: "primary",
    daily: { label: "Doanh thu ngày", value: "0đ", trend: "+0%" },
  },
  {
    id: "tickets",
    label: "Vé đã bán",
    value: "0",
    trend: "+0%",
    icon: "confirmation_number",
    color: "blue",
  },
  {
    id: "users",
    label: "Người dùng mới",
    value: "0",
    trend: "+0%",
    icon: "person_add",
    color: "purple",
  },
  {
    id: "occupancy",
    label: "Tỷ lệ lấp đầy",
    value: "0%",
    trend: "+0%",
    icon: "chair",
    color: "orange",
  },
]);

const topMovies = ref([]);
const businessPerformance = ref([]);
const occupancyRate = ref("0%");

const fetchDashboardStats = async () => {
  try {
    const res = await axios.get("/dashboard/stats");
    const data = res.data;
    
    stats.value[0].value = data.revenueMonthly.value;
    stats.value[0].trend = data.revenueMonthly.trend;
    stats.value[0].daily.value = data.revenueDaily.value;
    stats.value[0].daily.trend = data.revenueDaily.trend;
    
    stats.value[1].value = data.tickets.value;
    stats.value[1].trend = data.tickets.trend;
    
    stats.value[2].value = data.newUsers.value;
    stats.value[2].trend = data.newUsers.trend;
    
    stats.value[3].value = data.occupancy.value;
    stats.value[3].trend = data.occupancy.trend;
    
    occupancyRate.value = data.occupancy.value;
    topMovies.value = data.topMovies;
    businessPerformance.value = data.businessPerformance;
  } catch (error) {
    console.error("Failed to fetch dashboard stats", error);
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchDashboardStats();
});
</script>

<template>
  <div class="p-10">
    <header class="mb-12 text-on-surface">
      <div class="flex justify-between items-end">
        <div>
          <h1
            class="text-4xl font-extrabold tracking-tight font-headline uppercase italic text-primary"
          >
            Bảng Điều khiển
          </h1>
          <p
            class="text-on-surface-variant text-sm mt-1 uppercase tracking-widest font-bold"
          >
            Hệ thống phân tích dữ liệu thời gian thực
          </p>
        </div>
        <div class="flex gap-4">
          <button
            class="bg-surface-container-high px-4 py-2 rounded-sm border border-outline-variant/20 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 transition-all"
          >
            Xuất báo cáo
          </button>
          <div
            class="bg-primary/10 border border-primary/20 px-4 py-2 rounded-sm text-primary text-[10px] font-bold uppercase tracking-widest flex items-center gap-2"
          >
            <span class="relative flex h-2 w-2">
              <span
                class="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"
              ></span>
              <span
                class="relative inline-flex rounded-full h-2 w-2 bg-primary"
              ></span>
            </span>
            Dữ liệu Trực tiếp
          </div>
        </div>
      </div>
    </header>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
      <div
        v-for="stat in stats"
        :key="stat.id"
        style="perspective: 1000px"
        class="h-[184px]"
      >
        <!-- Flip Card for Revenue -->
        <div
          v-if="stat.id === 'revenue'"
          class="relative w-full h-full transition-transform duration-700 cursor-pointer group/flip"
          style="transform-style: preserve-3d"
          :class="{ '[transform:rotateY(180deg)]': !isMonthlyRevenue }"
          @click="isMonthlyRevenue = !isMonthlyRevenue"
        >
          <!-- Front Face: Monthly -->
          <div
            class="absolute inset-0 bg-surface-container-low border border-outline-variant/10 p-6 rounded-lg overflow-hidden transition-all shadow-sm group-hover/flip:border-primary/40"
            :class="{ 'pointer-events-auto z-10': isMonthlyRevenue, 'pointer-events-none z-0': !isMonthlyRevenue }"
            style="backface-visibility: hidden"
          >
            <div
              class="absolute -right-4 -bottom-4 opacity-5 group-hover/flip:opacity-10 transition-opacity"
            >
              <span class="material-symbols-outlined text-8xl">{{
                stat.icon
              }}</span>
            </div>
            <div class="relative z-10">
              <div class="flex justify-between items-start mb-6">
                <div
                  :class="`p-3 bg-${stat.color === 'primary' ? 'primary' : stat.color + '-500'}/10 rounded-xl text-${stat.color === 'primary' ? 'primary' : stat.color + '-500'}`"
                >
                  <span class="material-symbols-outlined">{{ stat.icon }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <div
                    class="flex items-center gap-1 text-[10px] font-bold text-green-500 bg-green-500/10 px-2 py-1 rounded"
                  >
                    <span class="material-symbols-outlined" 
                          style="font-size: 10px !important; transform: scale(1.5); display: inline-block; transform-origin: center;"
                      >trending_up</span>
                    {{ stat.trend }}
                  </div>
                </div>
              </div>
              <div class="flex items-center gap-2 mb-2 h-6 relative z-20">
                <p
                  class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant"
                >
                  {{ stat.label }}
                </p>
                <div
                  class="text-on-surface-variant/50 group-hover/flip:text-primary transition-colors flex items-center p-1"
                >
                  <span
                    class="material-symbols-outlined group-hover/flip:rotate-180 transition-transform duration-500"
                    style="
                      font-size: 12px !important;
                      transform: scale(1.5);
                      display: inline-block;
                      transform-origin: center;
                    "
                    >swap_horiz</span
                  >
                </div>
              </div>
              <h3 class="text-3xl font-black font-headline text-on-surface">
                {{ stat.value }}
              </h3>
            </div>
          </div>
          <!-- Back Face: Daily -->
          <div
            class="absolute inset-0 bg-surface-container-low border border-outline-variant/10 p-6 rounded-lg overflow-hidden transition-all shadow-sm group-hover/flip:border-primary/40"
            :class="{ 'pointer-events-auto z-10': !isMonthlyRevenue, 'pointer-events-none z-0': isMonthlyRevenue }"
            style="backface-visibility: hidden; transform: rotateY(180deg)"
          >
            <div
              class="absolute -right-4 -bottom-4 opacity-5 group-hover/flip:opacity-10 transition-opacity"
            >
              <span class="material-symbols-outlined text-8xl">{{
                stat.icon
              }}</span>
            </div>
            <div class="relative z-10">
              <div class="flex justify-between items-start mb-6">
                <div
                  :class="`p-3 bg-${stat.color === 'primary' ? 'primary' : stat.color + '-500'}/10 rounded-xl text-${stat.color === 'primary' ? 'primary' : stat.color + '-500'}`"
                >
                  <span class="material-symbols-outlined">{{ stat.icon }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <div
                    class="flex items-center gap-1 text-[10px] font-bold text-green-500 bg-green-500/10 px-2 py-1 rounded"
                  >
                    <span class="material-symbols-outlined"
                          style="font-size: 10px !important; transform: scale(1.5); display: inline-block; transform-origin: center;"
                      >trending_up</span>
                    {{ stat.daily.trend }}
                  </div>
                </div>
              </div>
              <div class="flex items-center gap-2 mb-2 h-6 relative z-20">
                <p
                  class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant"
                >
                  {{ stat.daily.label }}
                </p>
                <div
                  class="text-on-surface-variant/50 group-hover/flip:text-primary transition-colors flex items-center p-1"
                >
                  <span
                    class="material-symbols-outlined group-hover/flip:rotate-180 transition-transform duration-500"
                    style="
                      font-size: 12px !important;
                      transform: scale(1.5);
                      display: inline-block;
                      transform-origin: center;
                    "
                    >swap_horiz</span
                  >
                </div>
              </div>
              <h3 class="text-3xl font-black font-headline text-on-surface">
                {{ stat.daily.value }}
              </h3>
            </div>
          </div>
        </div>

        <!-- Normal Card -->
        <div
          v-else
          class="bg-surface-container-low border border-outline-variant/10 p-6 rounded-lg relative overflow-hidden group hover:border-primary/40 transition-all h-full shadow-sm"
        >
          <div
            class="absolute -right-4 -bottom-4 opacity-5 group-hover:opacity-10 transition-opacity"
          >
            <span class="material-symbols-outlined text-8xl">{{
              stat.icon
            }}</span>
          </div>
          <div class="relative z-10">
            <div class="flex justify-between items-start mb-6">
              <div
                :class="`p-3 bg-${stat.color === 'primary' ? 'primary' : stat.color + '-500'}/10 rounded-xl text-${stat.color === 'primary' ? 'primary' : stat.color + '-500'}`"
              >
                <span class="material-symbols-outlined">{{ stat.icon }}</span>
              </div>
              <div
                class="flex items-center gap-1 text-[10px] font-bold text-green-500 bg-green-500/10 px-2 py-1 rounded"
              >
                <span class="material-symbols-outlined"
                      style="font-size: 10px !important; transform: scale(1.5); display: inline-block; transform-origin: center;"
                  >trending_up</span>
                {{ stat.trend }}
              </div>
            </div>
            <div class="flex items-center h-4 mb-2">
              <p
                class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant"
              >
                {{ stat.label }}
              </p>
            </div>
            <h3 class="text-3xl font-black font-headline text-on-surface">
              {{ stat.value }}
            </h3>
          </div>
        </div>
      </div>
    </div>

    <!-- Charts Section -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-12">
      <div
        class="lg:col-span-2 bg-surface-container-low border border-outline-variant/10 rounded-lg p-8 relative overflow-hidden"
      >
        <div class="flex justify-between items-center mb-12">
          <div>
            <h3
              class="font-headline font-bold uppercase tracking-tight text-on-surface text-lg"
            >
              Hiệu quả Kinh doanh
            </h3>
            <p
              class="text-[10px] text-on-surface-variant uppercase tracking-widest mt-1"
            >
              Doanh thu & Lượng vé (7 ngày qua)
            </p>
          </div>
          <div class="flex gap-4">
            <div class="flex items-center gap-2">
              <div class="w-3 h-3 bg-primary rounded-full"></div>
              <span
                class="text-[10px] font-bold uppercase text-on-surface-variant"
                >Doanh thu</span
              >
            </div>
            <div class="flex items-center gap-2">
              <div class="w-3 h-3 bg-blue-500 rounded-full"></div>
              <span
                class="text-[10px] font-bold uppercase text-on-surface-variant"
                >Lượng vé</span
              >
            </div>
          </div>
        </div>

        <!-- Mockup Chart -->
        <div class="h-80 flex items-end justify-between gap-4 px-4 relative">
          <!-- Grid Lines -->
          <div
            class="absolute inset-x-0 top-0 bottom-0 flex flex-col justify-between pointer-events-none opacity-5"
          >
            <div
              v-for="i in 5"
              :key="i"
              class="w-full h-px bg-on-surface"
            ></div>
          </div>

          <div
            v-for="(item, i) in businessPerformance"
            :key="i"
            class="flex-grow flex items-end gap-1.5 h-full relative group"
          >
            <div
              class="w-full bg-primary/20 rounded-t-sm hover:bg-primary transition-all duration-500 cursor-pointer"
              :style="{ height: item.revenuePercentage + '%' }"
            >
              <div
                class="absolute -top-12 left-1/2 -translate-x-1/2 bg-surface-container-highest p-2 rounded text-[10px] font-bold opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-20"
              >
                {{ item.revenueLabel }}
              </div>
            </div>
            <div
              class="w-full bg-blue-500/20 rounded-t-sm hover:bg-blue-500 transition-all duration-500 cursor-pointer"
              :style="{ height: item.ticketPercentage + '%' }"
            >
              <div
                class="absolute -top-12 left-1/2 -translate-x-1/2 bg-surface-container-highest p-2 rounded text-[10px] font-bold opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-20"
              >
                {{ item.ticketLabel }}
              </div>
            </div>
          </div>
        </div>

        <div
          class="flex justify-between mt-8 text-[10px] font-bold uppercase tracking-widest text-on-surface-variant border-t border-outline-variant/10 pt-6"
        >
          <span
            v-for="item in businessPerformance"
            :key="item.day"
            >{{ item.day }}</span
          >
        </div>
      </div>

      <!-- Top Movies Section -->
      <div
        class="bg-surface-container-low border border-outline-variant/10 rounded-lg p-8"
      >
        <div class="flex justify-between items-center mb-10">
          <h3
            class="font-headline font-bold uppercase tracking-tight text-on-surface"
          >
            Phim hàng đầu
          </h3>
          <span
            class="text-[10px] font-bold text-primary uppercase tracking-widest underline cursor-pointer"
            >Xem tất cả</span
          >
        </div>

        <div class="space-y-6">
          <div
            v-for="movie in topMovies"
            :key="movie.title"
            class="flex items-center gap-4 group cursor-pointer p-2 hover:bg-white/5 transition-all rounded-lg"
          >
            <div
              class="w-12 h-16 bg-surface-container-high rounded overflow-hidden flex-shrink-0 border border-outline-variant/10"
            >
              <img
                src="/images/Hopper.webp"
                class="w-full h-full object-cover transition-all"
              />
            </div>
            <div class="flex-grow">
              <h4
                class="text-xs font-bold uppercase tracking-tight text-on-surface group-hover:text-primary transition-colors"
              >
                {{ movie.title }}
              </h4>
              <div class="flex justify-between mt-1">
                <span class="text-[10px] text-on-surface-variant font-bold">{{
                  movie.revenue
                }}</span>
                <span
                  :class="
                    movie.trend === 'up'
                      ? 'text-green-500'
                      : movie.trend === 'down'
                        ? 'text-red-500'
                        : 'text-on-surface-variant'
                  "
                  class="text-[10px] font-black uppercase"
                  >{{ movie.occupancy }}</span
                >
              </div>
            </div>
          </div>
        </div>

        <!-- Capacity Indicator -->
        <div
          class="mt-12 p-6 bg-surface-container-high rounded-lg border border-outline-variant/10"
        >
          <p
            class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-4"
          >
            Trạng thái phòng chiếu
          </p>
          <div class="flex items-center gap-4">
            <div class="relative w-16 h-16">
              <svg class="w-full h-full" viewBox="0 0 36 36">
                <path
                  class="text-surface stroke-current"
                  stroke-width="3"
                  fill="none"
                  d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                />
                <path
                  class="text-primary stroke-current"
                  stroke-width="3"
                  stroke-dasharray="74, 100"
                  stroke-linecap="round"
                  fill="none"
                  d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                />
              </svg>
              <div
                class="absolute inset-0 flex items-center justify-center text-[10px] font-bold"
              >
                {{ occupancyRate }}
              </div>
            </div>
            <div>
              <p
                class="text-xs font-bold text-on-surface uppercase tracking-tight"
              >
                Công suất tối ưu
              </p>
              <p class="text-[10px] text-green-500 font-bold mt-1 uppercase">
                Vượt mục tiêu 4.1%
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
