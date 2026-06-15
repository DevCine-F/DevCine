<script setup>
defineProps({
  staff: {
    type: Array,
    required: true
  }
})
</script>

<template>
  <div class="flex justify-between items-center mb-10">
    <h3
      class="text-2xl font-bold font-headline uppercase tracking-tight text-on-surface"
    >
      Quản trị Nhân sự
    </h3>
    <div class="flex gap-4">
      <button
        class="bg-surface-container-high px-4 py-2 rounded-xl border border-outline-variant/10 text-[10px] font-black uppercase tracking-widest text-on-surface-variant"
      >
        Xuất Bảng Lương
      </button>
      <button
        class="bg-primary/10 text-primary px-5 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest border border-primary/20 hover:bg-primary/20 transition-all flex items-center gap-2"
      >
        <span class="material-symbols-outlined text-sm font-bold"
          >person_add</span
        >
        Tuyển dụng mới
      </button>
    </div>
  </div>

  <!-- Role-based Groups -->
  <div class="space-y-12">
    <div v-for="role in ['Box Office', 'Usher', 'F&B']" :key="role">
      <div class="flex items-center gap-4 mb-6">
        <span class="w-8 h-[1px] bg-primary/30"></span>
        <h4
          class="text-xs font-black uppercase tracking-[0.3em] text-primary italic"
        >
          {{ role }} Team
        </h4>
        <span
          class="px-2 py-0.5 bg-primary/10 text-primary text-[8px] font-black rounded"
          >{{
            staff.filter((s) => s.role === role).length
          }}
          Thành viên</span
        >
      </div>

      <div
        class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
      >
        <div
          v-for="member in staff.filter(
            (s) => s.role === role,
          )"
          :key="member.id"
          class="bg-surface-container-high border border-outline-variant/10 p-6 rounded-2xl group hover:border-primary/30 transition-all relative overflow-hidden"
        >
          <div class="flex items-center gap-4 mb-6">
            <div
              class="w-14 h-14 rounded-2xl bg-on-surface/5 flex items-center justify-center text-primary font-black text-xl border border-white/5 relative"
            >
              {{ member.name.charAt(0) }}
              <span
                v-if="member.status === 'On Duty'"
                class="absolute -top-1 -right-1 w-3 h-3 bg-green-500 rounded-full border-2 border-surface-container-high"
              ></span>
            </div>
            <div>
              <h4
                class="text-lg font-black text-on-surface group-hover:text-primary transition-colors uppercase tracking-tight"
              >
                {{ member.name }}
              </h4>
              <p
                class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest"
              >
                Ca {{ member.shift }}
              </p>
            </div>
          </div>

          <!-- KPI Stats -->
          <div
            class="grid grid-cols-2 gap-4 mb-6 p-4 bg-on-surface/5 rounded-xl border border-white/5"
          >
            <div v-if="role === 'Box Office'">
              <p
                class="text-[8px] font-black text-on-surface-variant uppercase mb-1"
              >
                Vé đã bán
              </p>
              <p class="text-lg font-black text-on-surface">
                {{ member.sales }}
              </p>
            </div>
            <div v-if="role === 'F&B'">
              <p
                class="text-[8px] font-black text-on-surface-variant uppercase mb-1"
              >
                Đơn hàng
              </p>
              <p class="text-lg font-black text-on-surface">
                {{ member.sales }}
              </p>
            </div>
            <div v-if="role === 'Usher'">
              <p
                class="text-[8px] font-black text-on-surface-variant uppercase mb-1"
              >
                Suất đã soát
              </p>
              <p class="text-lg font-black text-on-surface">--</p>
            </div>
            <div>
              <p
                class="text-[8px] font-black text-on-surface-variant uppercase mb-1"
              >
                Hiệu suất
              </p>
              <p class="text-lg font-black text-green-500">92%</p>
            </div>
          </div>

          <div class="flex gap-2">
            <button
              class="flex-grow py-2.5 bg-on-surface/5 hover:bg-white/5 text-[9px] font-black uppercase tracking-widest rounded-lg border border-white/5 transition-all italic"
            >
              Lịch sử ca trực
            </button>
            <button
              class="w-10 h-10 flex items-center justify-center rounded-lg bg-primary/10 text-primary hover:bg-primary hover:text-on-primary transition-all border border-primary/20"
            >
              <span class="material-symbols-outlined text-sm"
                >trending_up</span
              >
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
