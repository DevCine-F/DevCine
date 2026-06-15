<script setup>
defineProps({
  cinema: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <div class="flex justify-between items-center mb-10">
    <h3
      class="text-2xl font-bold font-headline uppercase tracking-tight text-on-surface"
    >
      Trung tâm Điều hành
    </h3>
    <div class="flex gap-4">
      <div
        class="flex items-center bg-surface-container-high rounded-xl border border-outline-variant/10 p-1"
      >
        <button
          class="px-4 py-2 bg-primary text-on-primary text-[8px] font-black uppercase tracking-widest rounded-lg"
        >
          Thời gian thực
        </button>
        <button
          class="px-4 py-2 text-on-surface-variant text-[8px] font-black uppercase tracking-widest rounded-lg"
        >
          Theo tuần
        </button>
      </div>
      <button
        class="bg-on-surface/5 text-on-surface px-5 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest border border-outline-variant/10 hover:bg-white/5 transition-all flex items-center gap-2 italic"
      >
        <span class="material-symbols-outlined text-sm">print</span>
        Xuất Báo Cáo Cinema
      </button>
    </div>
  </div>

  <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
    <!-- ROW 1: INSIGHT NHANH -->
    <div class="lg:col-span-3 grid grid-cols-1 md:grid-cols-3 gap-8">
      <!-- Cơ cấu Doanh thu -->
      <div class="bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl relative overflow-hidden group hover:border-primary/30 transition-all">
        <div class="flex justify-between items-start mb-6">
          <div>
            <p class="text-[10px] font-black text-primary uppercase tracking-widest mb-1 italic">Tài chính</p>
            <h4 class="text-lg font-bold text-on-surface uppercase tracking-tight">Cơ cấu Doanh thu</h4>
          </div>
          <span class="material-symbols-outlined text-primary/50 text-2xl">pie_chart</span>
        </div>
        <div class="flex items-center justify-center h-32 relative">
          <!-- Placeholder cho Biểu đồ Donut -->
          <div class="w-24 h-24 rounded-full border-8 border-primary border-t-orange-500 border-r-orange-500 relative flex items-center justify-center shadow-[0_0_15px_rgba(245,197,24,0.2)]">
            <span class="text-xs font-black">100%</span>
          </div>
        </div>
        <div class="flex justify-between mt-4 text-[10px] font-bold uppercase tracking-widest">
          <div class="flex items-center gap-2"><div class="w-2 h-2 bg-primary rounded-full"></div>Vé (60%)</div>
          <div class="flex items-center gap-2"><div class="w-2 h-2 bg-orange-500 rounded-full"></div>F&B (40%)</div>
        </div>
      </div>

      <!-- ARPU & Dòng tiền -->
      <div class="bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl relative overflow-hidden group hover:border-primary/30 transition-all">
        <div class="flex justify-between items-start mb-6">
          <div>
            <p class="text-[10px] font-black text-primary uppercase tracking-widest mb-1 italic">Trung bình</p>
            <h4 class="text-lg font-bold text-on-surface uppercase tracking-tight">ARPU & Dòng tiền</h4>
          </div>
          <span class="material-symbols-outlined text-primary/50 text-2xl">account_balance_wallet</span>
        </div>
        <div class="mb-4">
          <p class="text-[9px] font-black text-on-surface-variant uppercase tracking-widest">ARPU F&B / Khách</p>
          <h2 class="text-3xl font-black text-primary mt-1">65.000<span class="text-sm">đ</span></h2>
        </div>
        <div class="space-y-3">
          <div class="flex items-center gap-3">
            <span class="text-[9px] font-bold uppercase w-12 text-on-surface-variant">Thẻ</span>
            <div class="flex-1 h-2 bg-white/5 rounded-full overflow-hidden"><div class="h-full bg-blue-500 w-[50%]"></div></div>
            <span class="text-[9px] font-bold">50%</span>
          </div>
          <div class="flex items-center gap-3">
            <span class="text-[9px] font-bold uppercase w-12 text-on-surface-variant">Ví ĐT</span>
            <div class="flex-1 h-2 bg-white/5 rounded-full overflow-hidden"><div class="h-full bg-purple-500 w-[35%]"></div></div>
            <span class="text-[9px] font-bold">35%</span>
          </div>
          <div class="flex items-center gap-3">
            <span class="text-[9px] font-bold uppercase w-12 text-on-surface-variant">Tiền mặt</span>
            <div class="flex-1 h-2 bg-white/5 rounded-full overflow-hidden"><div class="h-full bg-green-500 w-[15%]"></div></div>
            <span class="text-[9px] font-bold">15%</span>
          </div>
        </div>
      </div>

      <!-- Khách hàng & Suất chiếu -->
      <div class="bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl relative overflow-hidden group hover:border-primary/30 transition-all">
        <div class="flex justify-between items-start mb-6">
          <div>
            <p class="text-[10px] font-black text-primary uppercase tracking-widest mb-1 italic">Insight</p>
            <h4 class="text-lg font-bold text-on-surface uppercase tracking-tight">Khách hàng & Suất</h4>
          </div>
          <span class="material-symbols-outlined text-primary/50 text-2xl">groups</span>
        </div>
        <div class="flex justify-between items-center mb-6">
          <div class="text-center">
            <div class="w-16 h-16 rounded-full border-4 border-primary border-l-white/10 flex items-center justify-center mb-2 mx-auto">
              <span class="text-sm font-black">60%</span>
            </div>
            <p class="text-[9px] font-bold uppercase text-primary">Loyalty</p>
          </div>
          <div class="text-center">
            <div class="w-16 h-16 rounded-full border-4 border-white/20 border-r-white/50 flex items-center justify-center mb-2 mx-auto">
              <span class="text-sm font-black">40%</span>
            </div>
            <p class="text-[9px] font-bold uppercase text-on-surface-variant">Vãng lai</p>
          </div>
        </div>
        <div class="p-3 bg-green-500/10 rounded-xl border border-green-500/20">
          <p class="text-[9px] font-black uppercase text-green-500 tracking-widest mb-1">Thực hiện suất chiếu</p>
          <div class="flex justify-between items-end">
            <span class="text-xl font-black text-white">98%</span>
            <span class="text-[9px] font-bold text-on-surface-variant mb-1">2% Hủy</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ROW 2: BIỂU ĐỒ CỐT LÕI -->
    <!-- Heatmap Doanh thu -->
    <div
      class="lg:col-span-2 bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl relative overflow-hidden"
    >
      <div class="flex justify-between items-center mb-10">
        <div>
          <p
            class="text-[10px] font-black text-primary uppercase tracking-widest mb-1 italic"
          >
            Phân tích giờ cao điểm
          </p>
          <h4
            class="text-xl font-bold text-on-surface uppercase tracking-tight"
          >
            Heatmap Doanh thu trong ngày
          </h4>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-[8px] font-bold opacity-40 uppercase"
            >Low</span
          >
          <div class="flex gap-1">
            <div class="w-3 h-3 rounded-sm bg-primary/20"></div>
            <div class="w-3 h-3 rounded-sm bg-primary/40"></div>
            <div class="w-3 h-3 rounded-sm bg-primary/70"></div>
            <div
              class="w-3 h-3 rounded-sm bg-primary shadow-[0_0_8px_#F5C518]"
            ></div>
          </div>
          <span class="text-[8px] font-bold opacity-40 uppercase"
            >Peak</span
          >
        </div>
      </div>

      <!-- The Heatmap Grid -->
      <div class="grid grid-cols-12 gap-2 h-40">
        <div
          v-for="i in 24"
          :key="i"
          class="rounded-lg transition-all hover:scale-110 hover:z-20 cursor-pointer relative group"
          :class="[
            i > 18 && i < 22
              ? 'bg-primary shadow-[0_0_15px_rgba(245,197,24,0.4)]'
              : i > 16 && i < 24
                ? 'bg-primary/70'
                : i > 10 && i < 15
                  ? 'bg-primary/40'
                  : 'bg-primary/10',
          ]"
        >
          <div
            class="absolute -top-8 left-1/2 -translate-x-1/2 bg-black px-2 py-1 rounded text-[7px] font-black text-white opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-30"
          >
            {{ i }}:00 - Bận rộn
          </div>
        </div>
      </div>
      <div
        class="flex justify-between mt-4 text-[10px] font-bold text-on-surface-variant uppercase tracking-widest opacity-80"
      >
        <span>08:00</span>
        <span>14:00</span>
        <span>20:00</span>
        <span>02:00</span>
      </div>
    </div>

    <!-- Báo cáo phế phẩm -->
    <div
      class="bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl"
    >
      <h4
        class="text-sm font-black uppercase tracking-[0.2em] text-on-surface-variant mb-8 flex items-center gap-2 italic"
      >
        <span
          class="material-symbols-outlined text-orange-500 text-lg"
          >recycling</span
        >
        Báo cáo phế phẩm
      </h4>
      <div class="space-y-6">
        <div
          class="flex justify-between items-end border-b border-white/5 pb-4"
        >
          <p
            class="text-[9px] font-black text-on-surface-variant uppercase italic"
          >
            Tổng Waste (Tháng)
          </p>
          <p class="text-2xl font-black text-red-500">
            1.2tr
            <span
              class="text-[10px] font-normal text-on-surface-variant italic"
              >VND</span
            >
          </p>
        </div>
        <div class="space-y-4">
          <div
            v-for="item in cinema.inventory.filter(
              (i) => i.waste > 5,
            )"
            :key="item.id"
            class="flex items-center justify-between"
          >
            <span
              class="text-[10px] font-bold text-on-surface uppercase"
              >{{ item.name }}</span
            >
            <span class="text-[10px] font-black text-orange-400"
              >{{ item.waste }} {{ item.unit }}</span
            >
          </div>
        </div>
        <button
          class="w-full py-3 bg-on-surface/5 text-[9px] font-black uppercase tracking-widest rounded-xl hover:bg-white/5 transition-all border border-white/5"
        >
          Xem phân tích tối ưu chi phí
        </button>
      </div>
    </div>

    <!-- ROW 3: LẤP ĐẦY & VẬN HÀNH -->
    <!-- Biểu đồ lấp đầy theo khung giờ -->
    <div class="lg:col-span-2 bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl relative overflow-hidden">
      <div class="flex justify-between items-center mb-8">
        <div>
          <p class="text-[10px] font-black text-primary uppercase tracking-widest mb-1 italic">Vận hành</p>
          <h4 class="text-xl font-bold text-on-surface uppercase tracking-tight">Biểu đồ Lấp đầy theo khung giờ</h4>
        </div>
        <button class="text-[10px] font-black text-on-surface-variant hover:text-primary uppercase tracking-widest flex items-center gap-1 transition-colors">
          Hôm nay <span class="material-symbols-outlined text-sm">expand_more</span>
        </button>
      </div>
      <div class="h-48 flex items-end justify-between gap-2 px-2 relative">
        <!-- Trục Y -->
        <div class="absolute left-0 top-0 bottom-6 w-full flex flex-col justify-between opacity-10 pointer-events-none">
          <div class="border-b border-white w-full"></div>
          <div class="border-b border-white w-full"></div>
          <div class="border-b border-white w-full"></div>
          <div class="border-b border-white w-full"></div>
        </div>
        <!-- Cột -->
        <div v-for="h in [8,10,12,14,16,18,20,22,24]" :key="h" class="flex flex-col items-center gap-2 z-10 group flex-1">
          <div class="w-full max-w-[24px] bg-primary/20 hover:bg-primary/50 transition-all rounded-t-sm relative flex items-end justify-center" :style="{height: (Math.random()*60 + 20) + '%'}">
            <div class="absolute -top-6 text-[8px] font-black text-white opacity-0 group-hover:opacity-100">{{ Math.floor(Math.random()*80 + 20) }}%</div>
            <div class="w-1/2 bg-primary rounded-t-sm" :style="{height: (Math.random()*70 + 30) + '%'}"></div>
          </div>
          <span class="text-[9px] font-bold text-on-surface-variant">{{ h }}h</span>
        </div>
      </div>
    </div>

    <!-- Cơ sở vật chất -->
    <div class="bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl relative overflow-hidden">
      <div class="flex justify-between items-center mb-6">
        <div>
          <p class="text-[10px] font-black text-primary uppercase tracking-widest mb-1 italic">Thiết bị</p>
          <h4 class="text-xl font-bold text-on-surface uppercase tracking-tight">Cơ sở vật chất</h4>
        </div>
        <span class="material-symbols-outlined text-red-500 animate-pulse">warning</span>
      </div>
      <div class="space-y-4">
        <div class="p-3 bg-red-500/10 border border-red-500/20 rounded-xl flex gap-3 items-start">
          <span class="material-symbols-outlined text-red-500 text-lg">videocam_off</span>
          <div>
            <p class="text-[10px] font-black text-red-500 uppercase">Phòng 3</p>
            <p class="text-[10px] text-on-surface-variant">Bảo trì máy chiếu</p>
          </div>
        </div>
        <div class="p-3 bg-orange-500/10 border border-orange-500/20 rounded-xl flex gap-3 items-start">
          <span class="material-symbols-outlined text-orange-500 text-lg">chair_alt</span>
          <div>
            <p class="text-[10px] font-black text-orange-500 uppercase">Phòng 1</p>
            <p class="text-[10px] text-on-surface-variant">Hỏng ghế E4, E5</p>
          </div>
        </div>
        <div class="p-3 bg-green-500/10 border border-green-500/20 rounded-xl flex gap-3 items-center">
          <span class="material-symbols-outlined text-green-500 text-lg">check_circle</span>
          <p class="text-[10px] font-black text-green-500 uppercase">Các phòng còn lại tốt</p>
        </div>
      </div>
    </div>

    <!-- ROW 4: PHÂN TÍCH PHIM & PHÒNG -->
    <!-- Top 5 Phim -->
    <div class="lg:col-span-2 bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl relative overflow-hidden">
      <div class="flex justify-between items-center mb-8">
        <div>
          <p class="text-[10px] font-black text-primary uppercase tracking-widest mb-1 italic">BXH Phim</p>
          <h4 class="text-xl font-bold text-on-surface uppercase tracking-tight">Top 5 Phim Ăn Khách Nhất</h4>
        </div>
        <div class="flex gap-2">
          <span class="px-3 py-1.5 bg-primary/20 text-primary text-[9px] font-black uppercase tracking-widest rounded-lg border border-primary/20">Tuần này</span>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="border-b border-white/5 text-[9px] uppercase tracking-widest text-on-surface-variant font-black">
              <th class="pb-4 pl-2">Hạng</th>
              <th class="pb-4">Phim</th>
              <th class="pb-4 text-right">Lượt xem (Admissions)</th>
              <th class="pb-4 text-right">Doanh thu vé</th>
              <th class="pb-4 text-right pr-2">Lấp đầy TB</th>
            </tr>
          </thead>
          <tbody class="text-sm">
            <tr v-for="(movie, index) in [
              {name: 'Lật Mặt 7', views: '12,450', rev: '1.2 Tỷ', occ: '85%'},
              {name: 'Dune: Part Two', views: '8,320', rev: '850 Tr', occ: '72%'},
              {name: 'Kung Fu Panda 4', views: '6,100', rev: '520 Tr', occ: '68%'},
              {name: 'Exhuma', views: '4,500', rev: '380 Tr', occ: '60%'},
              {name: 'Godzilla x Kong', views: '3,200', rev: '290 Tr', occ: '55%'}
            ]" :key="index" class="border-b border-white/5 hover:bg-white/5 transition-colors group">
              <td class="py-4 pl-2 font-black" :class="index < 3 ? 'text-primary' : 'text-on-surface-variant'">#{{ index + 1 }}</td>
              <td class="py-4 font-bold text-on-surface">{{ movie.name }}</td>
              <td class="py-4 text-right text-on-surface-variant">{{ movie.views }}</td>
              <td class="py-4 text-right font-black text-green-400">{{ movie.rev }}</td>
              <td class="py-4 text-right pr-2">
                <div class="flex items-center justify-end gap-2">
                  <div class="w-16 h-1.5 bg-white/5 rounded-full overflow-hidden">
                    <div class="h-full bg-primary" :style="{width: movie.occ}"></div>
                  </div>
                  <span class="text-[10px] font-bold">{{ movie.occ }}</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Room Occupancy Detailed (Legacy) -->
    <div
      class="lg:col-span-1 bg-surface-container-high border border-outline-variant/10 p-8 rounded-3xl overflow-hidden flex flex-col"
    >
      <div class="flex flex-col mb-8">
        <h4
          class="text-xl font-bold text-on-surface uppercase tracking-tight leading-tight mb-2"
        >
          Hiệu suất lấp đầy
        </h4>
        <div
          class="text-[10px] font-black text-primary uppercase tracking-widest italic flex items-center gap-2"
        >
          Target: 75%
          <span
            class="material-symbols-outlined text-green-500 text-xs"
            >trending_up</span
          >
        </div>
      </div>
      <div
        class="flex-1 space-y-4 overflow-y-auto pr-2 custom-scrollbar"
      >
        <div
          v-for="hall in cinema.halls"
          :key="hall.id"
          class="p-4 bg-black/20 rounded-2xl border border-white/5 group hover:border-primary/40 transition-all"
        >
          <div class="flex justify-between items-center mb-2">
            <span
              class="text-[10px] font-black text-white/40 uppercase"
              >{{ hall.name }}</span
            >
            <span
              class="text-lg font-black"
              :class="
                hall.id === 'H2' ? 'text-green-500' : 'text-primary'
              "
              >{{
                hall.id === "H1"
                  ? "88%"
                  : hall.id === "H2"
                    ? "92%"
                    : "45%"
              }}</span
            >
          </div>
          <div
            class="w-full h-1 bg-white/5 rounded-full overflow-hidden mb-2"
          >
            <div
              class="h-full bg-current transition-all duration-1000"
              :style="{
                width:
                  hall.id === 'H1'
                    ? '88%'
                    : hall.id === 'H2'
                      ? '92%'
                      : '45%',
                color: hall.id === 'H2' ? '#22c55e' : '#F5C518',
              }"
            ></div>
          </div>
          <p
            class="text-[8px] font-bold text-on-surface-variant uppercase tracking-widest opacity-40"
          >
            Dựa trên 12 suất chiếu hôm nay
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
