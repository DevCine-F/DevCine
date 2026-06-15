<script setup>
defineProps({
  stats: {
    type: Object,
    required: true
  }
})
</script>

<template>
  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
    <div
      v-for="(val, key) in stats"
      :key="key"
      class="bg-surface-container-low border border-outline-variant/10 p-6 rounded-2xl flex justify-between items-center group hover:border-primary/30 transition-all"
    >
      <div>
        <p
          class="text-[10px] font-black text-on-surface-variant uppercase tracking-widest mb-1"
        >
          {{
            key === "revenue"
              ? "Doanh thu tháng"
              : key === "occupancy"
                ? "Tỷ lệ lấp đầy"
                : key === "admissions"
                  ? "Tổng lượt khách"
                  : "Cơ sở vật chất"
          }}
        </p>
        <h4 class="text-2xl font-black text-on-surface">{{ val }}</h4>
      </div>
      <div
        class="w-12 h-12 rounded-xl bg-on-surface/5 flex items-center justify-center text-on-surface-variant group-hover:text-primary transition-colors"
      >
        <span class="material-symbols-outlined text-2xl">{{
          key === "revenue"
            ? "payments"
            : key === "occupancy"
              ? "chair"
              : key === "admissions"
                ? "local_activity"
                : "build"
        }}</span>
      </div>
    </div>
  </div>
</template>
