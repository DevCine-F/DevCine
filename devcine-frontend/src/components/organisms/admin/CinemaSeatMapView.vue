<script setup>
import { useRouter } from 'vue-router'
import SeatMapBuilder from '@/components/admin/SeatMapBuilder.vue'

const router = useRouter()

defineProps({
  viewingHall: {
    type: Object,
    required: true
  },
  currentSeatMap: {
    type: Object,
    required: true
  },
  tempRows: {
    type: Number,
    required: true
  },
  tempCols: {
    type: Number,
    required: true
  },
  isSavingLayout: {
    type: Boolean,
    default: false
  },
  hasChanges: {
    type: Boolean,
    default: false
  },
  /**
   * Phòng đã phát sinh vé đặt (SOLD/HOLD) ở ít nhất một suất.
   * Khi true → Trình thiết kế chuyển sang chế độ Chỉ đọc: ẩn toolbar,
   * khóa nút Lưu/Đặt lại, hiển thị banner cảnh báo.
   */
  hasBookings: {
    type: Boolean,
    default: false
  }
})

defineEmits(['back', 'reset', 'save', 'update:layout', 'dirty'])

const goToIncidents = () => {
  router.push('/admin/incidents')
}
</script>

<template>
  <div
    class="animate-in fade-in slide-in-from-right-8 duration-700 flex flex-col h-[calc(100vh-120px)]"
  >
    <header
      class="flex justify-between items-center mb-4 px-4 flex-shrink-0"
    >
      <div class="flex items-center gap-6">
        <button
          @click="$emit('back')"
          class="w-10 h-10 flex items-center justify-center rounded-full bg-on-surface/5 border border-white/10 text-on-surface hover:text-primary transition-all"
        >
          <span class="material-symbols-outlined text-lg">arrow_back</span>
        </button>
        <div>
          <h1
            class="text-2xl font-black tracking-tight font-headline uppercase text-on-surface flex items-center gap-3"
          >
            {{ viewingHall.name }}
            <span class="text-primary/30 text-lg">/</span>
            <span class="text-primary text-lg">{{ viewingHall.type }}</span>
          </h1>
        </div>
      </div>
      <div class="flex gap-3">
        <button
          @click="$emit('reset')"
          :disabled="hasBookings"
          :title="hasBookings ? 'Phòng đã có vé đặt – không thể đặt lại sơ đồ' : ''"
          class="px-6 py-2.5 rounded-lg border border-white/10 text-on-surface-variant text-[9px] font-black uppercase tracking-widest hover:bg-white/5 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
        >
          Đặt lại
        </button>
        <button
          @click="$emit('save')"
          :disabled="isSavingLayout || !hasChanges || hasBookings"
          :title="hasBookings ? 'Phòng đã có vé đặt – sơ đồ ghế bị khóa' : (!hasChanges && !isSavingLayout ? 'Chưa có thay đổi nào để lưu' : '')"
          :class="(isSavingLayout || !hasChanges || hasBookings)
            ? 'opacity-30 cursor-not-allowed shadow-none'
            : 'hover:brightness-110 shadow-xl shadow-primary/20'"
          class="px-8 py-2.5 rounded-lg bg-primary text-on-primary text-[9px] font-black uppercase tracking-widest transition-all flex items-center gap-2"
        >
          <span class="material-symbols-outlined text-sm">
            {{ isSavingLayout ? 'hourglass_empty' : 'save' }}
          </span>
          {{ isSavingLayout ? 'Đang lưu...' : 'Lưu Cấu Trúc' }}
        </button>
      </div>
    </header>

    <!-- Banner cảnh báo khi phòng đã có vé đặt -->
    <div
      v-if="hasBookings"
      class="mx-4 mb-4 flex-shrink-0 rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 flex items-start gap-4"
    >
      <span class="material-symbols-outlined text-amber-400 text-2xl mt-0.5 flex-shrink-0">lock</span>
      <div class="flex-1 min-w-0">
        <p class="text-amber-300 font-bold text-sm mb-1">Sơ đồ phòng chiếu đã bị khóa</p>
        <p class="text-amber-200/70 text-xs leading-relaxed">
          Phòng chiếu này đã phát sinh vé đặt. Để bảo vệ dữ liệu vé của khách hàng,
          sơ đồ ghế không thể chỉnh sửa.
          <br />
          Nếu có ghế bị hỏng hoặc sự cố, hãy chuyển sang <strong class="text-amber-300">Xử lý sự cố ghế</strong>.
        </p>
      </div>
      <button
        @click="goToIncidents"
        class="flex-shrink-0 flex items-center gap-1.5 px-4 py-2 rounded-lg bg-amber-500/20 border border-amber-500/40 text-amber-300 text-[9px] font-black uppercase tracking-widest hover:bg-amber-500/30 transition-all whitespace-nowrap"
      >
        <span class="material-symbols-outlined text-sm">build</span>
        Xử lý sự cố ghế
      </button>
    </div>

    <div class="flex-grow overflow-hidden min-h-0">
      <SeatMapBuilder
        :initial-rows="tempRows"
        :initial-cols="tempCols"
        :initial-seat-map="currentSeatMap"
        :read-only="hasBookings"
        @update:layout="(data) => $emit('update:layout', data)"
        @dirty="$emit('dirty')"
      />
    </div>
  </div>
</template>
