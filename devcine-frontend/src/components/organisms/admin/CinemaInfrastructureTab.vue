<script setup>
defineProps({
  halls: {
    type: Array,
    required: true
  }
})

defineEmits(['open-hall'])
</script>

<template>
  <div class="flex justify-between items-center mb-10">
    <h3
      class="text-2xl font-bold font-headline uppercase tracking-tight text-on-surface"
    >
      Cấu hình Phòng chiếu
    </h3>
    <button
      class="bg-primary/10 text-primary px-5 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest border border-primary/20 hover:bg-primary/20 transition-all flex items-center gap-2"
    >
      <span class="material-symbols-outlined text-sm font-bold"
        >add</span
      >
      Thêm Phòng
    </button>
  </div>

  <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
    <div
      v-for="hall in halls"
      :key="hall.id"
      class="bg-surface-container-high border border-outline-variant/10 p-8 rounded-2xl hover:border-primary/30 transition-all group"
    >
      <div class="flex justify-between items-start mb-8">
        <div class="flex items-center gap-5">
          <div
            class="w-16 h-16 rounded-2xl bg-on-surface/5 flex items-center justify-center text-on-surface-variant group-hover:text-primary transition-all group-hover:scale-105"
          >
            <span class="material-symbols-outlined text-3xl"
              >tv_gen</span
            >
          </div>
          <div>
            <h4 class="text-xl font-bold text-on-surface mb-1">
              {{ hall.name }}
            </h4>
            <span
              class="text-[9px] font-black text-primary uppercase tracking-[0.2em] px-2 py-1 bg-primary/10 rounded-md"
              >{{ hall.type }}</span
            >
          </div>
        </div>
        <div
          :class="
            hall.status === 'Active'
              ? 'text-green-500'
              : 'text-orange-500'
          "
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-black/20 text-[9px] font-black uppercase tracking-widest border border-white/5"
        >
          <span
            class="w-1.5 h-1.5 rounded-full bg-current animate-pulse"
          ></span>
          {{ hall.status === "Active" ? "Hoạt động" : "Bảo trì" }}
        </div>
      </div>

      <div class="grid grid-cols-2 gap-4 mb-8">
        <div
          class="p-4 bg-on-surface/[0.03] rounded-xl border border-white/5"
        >
          <p
            class="text-[9px] font-bold text-on-surface-variant uppercase tracking-widest mb-1 opacity-50"
          >
            Kích thước
          </p>
          <p class="text-lg font-black text-on-surface">
            {{ hall.rows }}x{{ hall.cols }} Matrix
          </p>
        </div>
        <div
          class="p-4 bg-on-surface/[0.03] rounded-xl border border-white/5"
        >
          <p
            class="text-[9px] font-bold text-on-surface-variant uppercase tracking-widest mb-1 opacity-50"
          >
            Tổng số ghế
          </p>
          <p class="text-lg font-black text-on-surface">
            {{ hall.rows * hall.cols }} Ghế
          </p>
        </div>
      </div>

      <div class="flex gap-3">
        <button
          @click="$emit('open-hall', hall)"
          class="flex-grow py-3 bg-primary text-on-primary text-[10px] font-black uppercase tracking-widest rounded-xl transition-all shadow-lg shadow-primary/10 hover:brightness-110"
        >
          Xem Chi tiết & Sơ đồ ghế
        </button>
        <button
          class="w-12 h-12 flex items-center justify-center rounded-xl bg-on-surface/5 hover:bg-on-surface/10 text-on-surface-variant transition-all border border-white/5"
        >
          <span class="material-symbols-outlined text-sm"
            >settings</span
          >
        </button>
      </div>
    </div>
  </div>
</template>
