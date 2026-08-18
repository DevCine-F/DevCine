<script setup>
import { computed } from 'vue'
import { useSeatGridRender } from '@/composables/useSeatGridRender'

const props = defineProps({
  /** Danh sách ghế/ô từ API (ShowtimeSeatResponse.seats) */
  seats: {
    type: Array,
    default: () => []
  },
  /** Số hàng ma trận */
  matrixRow: {
    type: Number,
    default: 10
  },
  /** Số cột ma trận */
  matrixCol: {
    type: Number,
    default: 12
  },
  /** Chế độ hiển thị: 'booking' (Client), 'pos' (Quầy POS), 'preview' (Admin drawer/chi tiết) */
  mode: {
    type: String,
    default: 'booking' // 'booking' | 'pos' | 'preview' | 'incident'
  },
  /** Danh sách ghế đang chọn (mảng seat object hoặc mảng seatId) */
  selectedSeats: {
    type: Array,
    default: () => []
  },
  /** Kích thước ô: 'normal' (40px) | 'compact' (32px) | 'sm' (28px) */
  size: {
    type: String,
    default: '' // nếu rỗng sẽ tự suy theo mode
  },
  /** Readonly: không cho click chọn ghế */
  readonly: {
    type: Boolean,
    default: false
  },
  /** Hiển thị nhãn hàng ở 2 bên */
  showRowLabels: {
    type: Boolean,
    default: true
  },
  /** Hiển thị nhãn số cột ở trên cùng */
  showColLabels: {
    type: Boolean,
    default: false
  },
  /** Hiển thị vạch màn hình phía trên */
  showScreen: {
    type: Boolean,
    default: false
  },
  screenTitle: {
    type: String,
    default: 'MÀN HÌNH CHÍNH'
  },
  /** Custom matcher / checker passed from parent */
  isSeatLockedByOthers: {
    type: Function,
    default: null
  },
  isSeatMaintenance: {
    type: Function,
    default: null
  },
  isOrphanSeat: {
    type: Function,
    default: null
  },
  /**
   * Ghế KHÔNG THỂ CHỌN với cấu hình vé/khối hiện tại (proactive seat locking):
   * ghế còn trống nhưng đặt khối vào đó sẽ không đủ chỗ liền nhau, sai loại ghế,
   * hoặc để lại ghế mồ côi. Không truyền (null) ⇒ tính năng tắt hoàn toàn.
   */
  isSeatUnselectable: {
    type: Function,
    default: null
  },
  seatPreviewClass: {
    type: Function,
    default: null
  },
  /** Custom seat class override function */
  customSeatClass: {
    type: Function,
    default: null
  }
})

const emit = defineEmits(['seat-click', 'seat-enter', 'seat-leave'])

// Sử dụng composable nội suy tọa độ duy nhất
const { cellAt, seatAt, isAisle, isSeat } = useSeatGridRender(() => props.seats)

const effectiveSize = computed(() => {
  if (props.size) return props.size
  if (props.mode === 'booking') return 'normal'
  return 'compact'
})

const cellSizeClass = computed(() => {
  if (effectiveSize.value === 'sm') return 'w-7 h-7 aspect-square'
  if (effectiveSize.value === 'compact') return 'w-8 h-8 aspect-square'
  return 'w-10 h-10 aspect-square'
})

const labelWidthClass = computed(() => {
  if (effectiveSize.value === 'sm') return 'w-4 text-[9px]'
  if (effectiveSize.value === 'compact') return 'w-5 text-[10px]'
  return 'w-6 text-label-sm'
})

// Kiểm tra ô (r, c) có bị che khuất bởi ghế SWEETBOX/Double ở cột trước đó (r, c - 1) hay không
const isOccupiedBySweetbox = (r, c) => {
  if (c <= 0) return false
  const prev = seatAt(r, c - 1)
  if (!prev) return false
  const type = (prev.seatType || '').toUpperCase()
  return type === 'SWEETBOX' || type === 'DOUBLE' || prev.span === 2
}

const isDoubleSeat = (cell) => {
  if (!cell || !isSeat(cell)) return false
  const type = (cell.seatType || '').toUpperCase()
  return type === 'SWEETBOX' || type === 'DOUBLE' || cell.span === 2
}

const isSelected = (seat) => {
  if (!seat) return false
  return props.selectedSeats.some(s => (s?.seatId ?? s?.id ?? s) === seat.seatId)
}

const checkLockedByOthers = (seat) => {
  if (!seat) return false
  if (props.isSeatLockedByOthers) return props.isSeatLockedByOthers(seat)
  return false
}

const checkMaintenance = (seat) => {
  if (!seat) return false
  if (props.isSeatMaintenance) return props.isSeatMaintenance(seat)
  const st = seat.status || seat.seatStatus
  return st === 'MAINTENANCE' || st === 'LOCKED'
}

const checkOrphan = (seat) => {
  if (!seat) return false
  if (props.isOrphanSeat) return props.isOrphanSeat(seat)
  return false
}

const checkUnselectable = (seat) => {
  if (!seat) return false
  if (props.isSeatUnselectable) return props.isSeatUnselectable(seat)
  return false
}

// Chỉ lấy nhãn nếu ghế thực sự tồn tại trong dữ liệu — tuyệt đối không sinh nhãn rác cho ô trống
const getSeatLabel = (seat) => {
  if (!seat) return ''
  if (seat.label && typeof seat.label === 'string' && seat.label.trim()) {
    return seat.label.trim()
  }
  if (seat.rowChar && seat.colNum != null) {
    return `${seat.rowChar}${seat.colNum}`
  }
  return ''
}

// Nhãn hàng bên hông: ưu tiên rowChar thật từ các ghế trên hàng đó, fallback về A..Z theo gridRow
const getRowLabel = (r) => {
  const found = (props.seats || []).find(s => s && s.gridRow === r && s.rowChar)
  if (found && found.rowChar) return found.rowChar
  return String.fromCharCode(65 + r)
}

// Tính class CSS cho từng ô ghế
const computeSeatClass = (seat) => {
  if (!seat) return ''
  if (props.customSeatClass) {
    const custom = props.customSeatClass(seat)
    if (custom) return custom
  }

  const isDouble = isDoubleSeat(seat)
  const sel = isSelected(seat)
  const isMaint = checkMaintenance(seat)
  const lockedOthers = checkLockedByOthers(seat)
  const isOrphan = checkOrphan(seat)
  const type = (seat.seatType || 'STANDARD').toUpperCase()

  const isCompact = effectiveSize.value === 'compact'
  const isSm = effectiveSize.value === 'sm'

  let sizeClass = ''
  if (isSm) {
    sizeClass = isDouble ? 'col-span-2 w-full h-7 rounded-md' : 'w-7 h-7 aspect-square rounded'
  } else if (isCompact) {
    sizeClass = isDouble ? 'col-span-2 w-full h-8 rounded-lg' : 'w-8 h-8 aspect-square rounded-lg'
  } else {
    sizeClass = isDouble ? 'col-span-2 w-full h-10 rounded-t-2xl rounded-b-lg' : 'w-10 h-10 aspect-square rounded-lg'
  }

  const baseClasses = `${sizeClass} flex items-center justify-center font-bold transition-all duration-150 leading-none shrink-0 select-none`
  const textClass = isCompact || isSm ? 'text-[9px]' : 'text-[10px]'

  // 1. Ghế bảo trì / khóa vật lý
  if (isMaint) {
    return `${baseClasses} ${textClass} bg-surface-container-highest border border-white/10 text-red-500 cursor-not-allowed opacity-60 pointer-events-none`
  }

  // 2. Ghế đã bán (SOLD)
  if (seat.status === 'SOLD') {
    return `${baseClasses} ${textClass} bg-surface-container-high border border-white/5 text-white/20 cursor-not-allowed pointer-events-none opacity-40`
  }

  // 3. Ghế đang giữ ở kênh khác (HOLD) hoặc WebSocket locked
  if (seat.status === 'HOLD' || lockedOthers) {
    return `${baseClasses} ${textClass} bg-yellow-500/10 border border-yellow-500/30 text-yellow-500/60 cursor-not-allowed pointer-events-none opacity-60`
  }

  // 4. Ghế đang được chọn bởi người dùng hiện tại
  if (sel) {
    return `${baseClasses} ${textClass} bg-gradient-to-br from-primary to-amber-600 text-on-primary shadow-[0_0_20px_rgba(245,197,24,0.35)] scale-[1.02] border-none cursor-pointer z-10`
  }

  // 5. Ghế còn trống nhưng KHÔNG THỂ CHỌN với cấu hình vé/khối hiện tại (proactive locking).
  //    Đặt SAU nhánh `sel` để ghế đang chọn vẫn bấm được (gỡ khối), TRƯỚC preview/available.
  if (checkUnselectable(seat)) {
    // Không đặt class nền ở đây: lớp phủ .seat-unselectable::after tự sơn nền xám ĐÈ LÊN
    // màu ghế thật (VIP đỏ / Sweetbox tím), khỏi phụ thuộc thứ tự thắng thua của Tailwind.
    return `${baseClasses} ${textClass} relative seat-unselectable border border-white/5 text-white/30 cursor-not-allowed pointer-events-none`
  }

  // 6. Cảnh báo ghế mồ côi (POS mode)
  if (isOrphan) {
    return `${baseClasses} ${textClass} bg-red-500/25 border-2 border-dashed border-white/80 text-white cursor-pointer`
  }

  // 7. Preview class (nếu có từ props)
  if (props.seatPreviewClass) {
    const preview = props.seatPreviewClass(seat)
    if (preview) return `${baseClasses} ${textClass} ${preview}`
  }

  // 8. Readonly preview mode
  if (props.readonly) {
    if (type === 'VIP') {
      return `${baseClasses} ${textClass} bg-gradient-to-b from-red-700/90 to-red-900/90 border border-red-500/50 text-red-100`
    }
    if (type === 'SWEETBOX' || type === 'DOUBLE') {
      return `${baseClasses} ${textClass} bg-gradient-to-b from-purple-600/90 to-purple-900/90 border border-purple-500/50 text-purple-100`
    }
    return `${baseClasses} ${textClass} bg-slate-800/80 border border-slate-600/50 text-slate-300`
  }

  // 9. Ghế khả dụng bình thường (Available)
  const shadowClasses = 'shadow-[0_4px_6px_rgba(0,0,0,0.3),inset_0_1px_1px_rgba(255,255,255,0.1)]'
  switch (type) {
    case 'VIP':
      return `${baseClasses} ${textClass} ${shadowClasses} bg-gradient-to-b from-red-700/90 to-red-900/90 border border-red-500/50 text-red-100 shadow-[0_0_15px_rgba(220,38,38,0.2)] hover:shadow-[0_0_20px_rgba(220,38,38,0.4)] hover:-translate-y-0.5 hover:brightness-110 cursor-pointer`
    case 'SWEETBOX':
    case 'DOUBLE':
      return `${baseClasses} ${textClass} ${shadowClasses} bg-gradient-to-b from-purple-600/90 to-purple-900/90 border border-purple-500/50 text-purple-100 shadow-[0_0_15px_rgba(147,51,234,0.2)] hover:shadow-[0_0_20px_rgba(147,51,234,0.4)] hover:-translate-y-0.5 hover:brightness-110 cursor-pointer`
    default: // STANDARD / NORMAL
      return `${baseClasses} ${textClass} ${shadowClasses} bg-slate-800/80 border border-slate-600/50 text-slate-300 hover:brightness-125 hover:-translate-y-0.5 hover:shadow-lg cursor-pointer`
  }
}

const handleSeatClick = (seat) => {
  if (props.readonly || !seat) return
  if (checkMaintenance(seat) || seat.status === 'SOLD' || seat.status === 'HOLD' || checkLockedByOthers(seat)) {
    return
  }
  // Ghế đã bị khoá chủ động (không đủ chỗ liền nhau / sai loại ghế / để lại ghế mồ côi).
  // Ghế đang chọn KHÔNG bao giờ rơi vào nhóm này nên thao tác gỡ khối vẫn hoạt động.
  if (checkUnselectable(seat)) return
  emit('seat-click', seat)
}

const handleSeatEnter = (seat) => {
  if (props.readonly || !seat) return
  emit('seat-enter', seat)
}

const handleSeatLeave = () => {
  if (props.readonly) return
  emit('seat-leave')
}
</script>

<template>
  <div class="seat-grid-container flex flex-col items-center w-full select-none">
    <!-- Màn hình chiếu (Screen Header) -->
    <div v-if="showScreen" class="w-full flex flex-col items-center flex-shrink-0 relative py-6 mb-8">
      <div class="absolute top-0 w-full h-[80px] bg-gradient-to-b from-primary/5 to-transparent pointer-events-none"></div>
      <div class="w-2/3 h-1.5 bg-primary/70 rounded-full shadow-[0_2px_15px_rgba(245,197,24,0.2)] mb-3 border border-primary/20"></div>
      <p class="text-[9px] font-bold uppercase tracking-[0.5em] text-primary/50 relative z-10">{{ screenTitle }}</p>
    </div>

    <!-- Ma trận các hàng ghế -->
    <div
      :class="[
        effectiveSize === 'compact' ? 'gap-1.5' : effectiveSize === 'sm' ? 'gap-1' : 'gap-2 md:gap-3',
        'flex flex-col mx-auto min-w-max pb-2'
      ]"
    >
      <!-- Nhãn số cột ở trên (tùy chọn) -->
      <div
        v-if="showColLabels"
        :class="[
          effectiveSize === 'compact' ? 'gap-1.5' : effectiveSize === 'sm' ? 'gap-1' : 'gap-2',
          'flex items-center justify-center mb-1'
        ]"
      >
        <div v-if="showRowLabels" :class="labelWidthClass" class="shrink-0"></div>
        <div
          class="grid items-center justify-items-center"
          :class="effectiveSize === 'compact' ? 'gap-1.5' : effectiveSize === 'sm' ? 'gap-1' : 'gap-2'"
          :style="{
            gridTemplateColumns: `repeat(${matrixCol}, minmax(0, 1fr))`
          }"
        >
          <div
            v-for="c in matrixCol"
            :key="`col-label-${c}`"
            :class="[cellSizeClass, 'flex items-center justify-center text-center text-[10px] font-black text-outline-variant/40 shrink-0']"
          >
            {{ c }}
          </div>
        </div>
        <div v-if="showRowLabels" :class="labelWidthClass" class="shrink-0"></div>
      </div>

      <!-- Từng hàng ghế: bọc trong CSS Grid chuẩn -->
      <div
        v-for="r in matrixRow"
        :key="`row-${r}`"
        :class="[
          effectiveSize === 'compact' ? 'gap-1.5' : effectiveSize === 'sm' ? 'gap-1' : 'gap-2',
          'flex items-center justify-center'
        ]"
      >
        <!-- Nhãn hàng bên trái -->
        <div
          v-if="showRowLabels"
          :class="[labelWidthClass, 'font-bold text-outline-variant text-center shrink-0']"
        >
          {{ getRowLabel(r - 1) }}
        </div>

        <!-- CSS Grid cho các ô trong hàng: fix cứng số cột bằng grid-template-columns -->
        <div
          class="grid items-center justify-items-center"
          :class="effectiveSize === 'compact' ? 'gap-1.5' : effectiveSize === 'sm' ? 'gap-1' : 'gap-2'"
          :style="{
            gridTemplateColumns: `repeat(${matrixCol}, minmax(0, 1fr))`
          }"
        >
          <template v-for="c in matrixCol" :key="`cell-${r}-${c}`">
            <!-- 1. Ô bị che khuất bởi ghế SWEETBOX ở cột X-1: BỎ QUA HOÀN TOÀN, không render bất kỳ thẻ DOM nào -->
            <template v-if="isOccupiedBySweetbox(r - 1, c - 1)">
              <!-- Skip cell X+1 completely because Sweetbox at X has col-span-2 -->
            </template>

            <!-- 2. Ghế bán được (Seat Cell) -->
            <template v-else-if="seatAt(r - 1, c - 1)">
              <div
                :class="computeSeatClass(seatAt(r - 1, c - 1))"
                :title="checkMaintenance(seatAt(r - 1, c - 1))
                  ? 'Ghế bảo trì'
                  : checkUnselectable(seatAt(r - 1, c - 1))
                    ? 'Không thể chọn với số vé / khối ghế hiện tại'
                    : getSeatLabel(seatAt(r - 1, c - 1))"
                @click="handleSeatClick(seatAt(r - 1, c - 1))"
                @mouseenter="handleSeatEnter(seatAt(r - 1, c - 1))"
                @mouseleave="handleSeatLeave"
              >
                <slot name="seat" :seat="seatAt(r - 1, c - 1)" :is-maintenance="checkMaintenance(seatAt(r - 1, c - 1))">
                  <span v-if="checkMaintenance(seatAt(r - 1, c - 1))" class="material-symbols-outlined text-[13px]">build</span>
                  <template v-else>{{ getSeatLabel(seatAt(r - 1, c - 1)) }}</template>
                </slot>
              </div>
            </template>

            <!-- 3. Lối đi tường minh (Aisle Cell) -->
            <template v-else-if="isAisle(cellAt(r - 1, c - 1))">
              <div
                :class="[cellSizeClass, 'flex items-center justify-center pointer-events-none shrink-0']"
                title="Lối đi"
              >
                <div
                  :class="effectiveSize === 'compact' || effectiveSize === 'sm' ? 'w-1 h-6' : 'w-1 h-8'"
                  class="rounded-full bg-white/10"
                ></div>
              </div>
            </template>

            <!-- 4. Ô trống / Ô đã xóa: giữ nguyên khoảng trống trong suốt, KHÔNG render nhãn rác -->
            <template v-else>
              <div
                :class="[cellSizeClass, 'opacity-0 pointer-events-none select-none shrink-0']"
              ></div>
            </template>
          </template>
        </div>

        <!-- Nhãn hàng bên phải -->
        <div
          v-if="showRowLabels"
          :class="[labelWidthClass, 'font-bold text-outline-variant text-center shrink-0']"
        >
          {{ getRowLabel(r - 1) }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.seat-grid-container {
  overflow-x: auto;
}

/* Ghế không thể chọn: lớp phủ tự sơn nền XÁM đè lên mọi màu ghế (VIP đỏ / Sweetbox tím),
   rồi vẽ dấu "X" trắng lên trên. Đặt ở pseudo-element nên luôn nằm trên nền của ghế,
   không phải tranh độ ưu tiên với class nền của Tailwind. Nhãn ghế còn hiện mờ bên dưới. */
.seat-unselectable::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  --x-line: rgba(255, 255, 255, 0.9);
  background-color: rgba(28, 31, 38, 0.85);
  background-image:
    linear-gradient(to bottom right, transparent calc(50% - 1px), var(--x-line) calc(50% - 1px), var(--x-line) calc(50% + 1px), transparent calc(50% + 1px)),
    linear-gradient(to bottom left, transparent calc(50% - 1px), var(--x-line) calc(50% - 1px), var(--x-line) calc(50% + 1px), transparent calc(50% + 1px));
}
</style>
