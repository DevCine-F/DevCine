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

  <!-- Data Table Layout -->
  <div class="bg-surface-container-low border border-outline-variant/10 rounded-2xl overflow-hidden">
    <div class="overflow-x-auto">
      <table class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-surface-container-high border-b border-outline-variant/10 text-[10px] font-black uppercase tracking-widest text-primary">
            <th class="py-4 px-6">Nhân viên</th>
            <th class="py-4 px-6">Vai trò</th>
            <th class="py-4 px-6 text-center">Vé bán hôm nay</th>
            <th class="py-4 px-6">Trạng thái</th>
            <th class="py-4 px-6 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-white/5">
          <tr v-if="staff.length === 0">
            <td colspan="5" class="py-12 text-center text-sm text-on-surface-variant italic">
              Không có dữ liệu nhân sự.
            </td>
          </tr>
          <tr
            v-for="member in staff"
            :key="member.id"
            class="hover:bg-white/5 transition-colors group"
          >
            <!-- Nhân viên -->
            <td class="py-4 px-6">
              <div class="flex items-center gap-3">
                <div
                  class="w-10 h-10 rounded-xl bg-on-surface/5 flex items-center justify-center text-primary font-black text-sm border border-white/5"
                >
                  {{ member.name.charAt(0) }}
                </div>
                <div>
                  <h4 class="text-sm font-black text-on-surface uppercase tracking-tight">
                    {{ member.name }}
                  </h4>
                </div>
              </div>
            </td>
            
            <!-- Vai trò -->
            <td class="py-4 px-6">
              <span class="px-2 py-1 bg-surface-container-highest text-on-surface-variant text-[10px] font-bold rounded uppercase tracking-widest border border-white/5">
                {{ member.role }}
              </span>
            </td>

            <!-- Vé bán hôm nay -->
            <td class="py-4 px-6 text-center">
              <span class="text-lg font-black text-on-surface">
                {{ member.sales }}
              </span>
            </td>

            <!-- Trạng thái -->
            <td class="py-4 px-6">
              <div class="flex items-center gap-2">
                <span
                  class="w-2 h-2 rounded-full"
                  :class="member.status === 'Đang làm việc' ? 'bg-green-500' : 'bg-on-surface-variant/40'"
                ></span>
                <span
                  class="text-[10px] font-black uppercase tracking-widest"
                  :class="member.status === 'Đang làm việc' ? 'text-green-500' : 'text-on-surface-variant'"
                >
                  {{ member.status }}
                </span>
              </div>
            </td>

            <!-- Thao tác -->
            <td class="py-4 px-6 text-right">
              <button
                class="px-3 py-1.5 bg-on-surface/5 hover:bg-primary/20 text-on-surface-variant hover:text-primary text-[9px] font-black uppercase tracking-widest rounded-lg border border-white/5 transition-all"
              >
                Lịch sử
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
