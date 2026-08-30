<script setup>
import { computed } from 'vue'
import { useToastStore } from "@/stores/toast";

const props = defineProps({
  cinema: {
    type: Object,
    required: true
  },
  dates: {
    type: Array,
    required: true
  },
  selectedDate: {
    type: String,
    required: true
  },
  isToday: {
    type: Boolean,
    default: false
  },
  // Số cột 15' của lưới (co giãn theo giờ hoạt động rạp).
  gridCols: {
    type: Number,
    default: 72
  },
  // Nhãn giờ trên thước: [{ label, leftPct }].
  hourMarks: {
    type: Array,
    default: () => []
  },
  // Có hiển thị vạch thời gian hiện tại không.
  showNow: {
    type: Boolean,
    default: false
  },
  currentTimeLeft: {
    type: String,
    default: "0%"
  },
  getGridStyle: {
    type: Function,
    required: true
  },
  checkConflict: {
    type: Function,
    required: true
  },
  checkFormatMismatch: {
    type: Function,
    required: true
  },
  getEndTime: {
    type: Function,
    required: true
  },
  canSchedule: {
    type: Boolean,
    default: true
  },
  canScheduleEdit: {
    type: Boolean,
    default: true
  }
})

defineEmits([
  'update:selectedDate',
  'update:selected-date',
  'add-showtime',
  'open-batch',
  'publish',
  'dragstart',
  'drop',
  'open-showtime'
])

const PX_PER_COL = 34 // bề rộng mỗi ô 15'
const LABEL_COL_PX = 192 // cột "Room \ Time" (w-48)
const gridTemplate = computed(() => `repeat(${props.gridCols}, minmax(0, 1fr))`)
const gridMinWidth = computed(() => `${props.gridCols * PX_PER_COL}px`)
const wrapperMinWidth = computed(() => `${LABEL_COL_PX + props.gridCols * PX_PER_COL}px`)

const toast = useToastStore()

const sortedHalls = computed(() => {
  if (!props.cinema?.halls) return []
  return [...props.cinema.halls].sort((a, b) =>
    (a.name || '').localeCompare(b.name || '', undefined, { numeric: true, sensitivity: 'base' })
  )
})

const isShowtimeLocked = (st) => {
  return (st.soldSeats || 0) + (st.heldSeats || 0) > 0 || st.reserved > 0;
}

const handleDragStart = (event, show) => {
  if (isShowtimeLocked(show)) {
    event.preventDefault();
    toast.warning("Suất chiếu đã có khách đặt/bán vé, không thể di chuyển giờ!");
    return;
  }
  emit('dragstart', event, show);
}
</script>

<template>
  <!-- Header: Date Picker -->
  <header
    class="flex justify-between items-center p-8 border-b border-outline-variant/10 bg-on-surface/[0.02]"
  >
    <div class="flex items-center gap-3">
      <button
        v-for="d in dates"
        :key="d.date"
        @click="$emit('update:selectedDate', d.date); $emit('update:selected-date', d.date)"
        :class="
          selectedDate === d.date
            ? 'bg-primary text-on-primary border-primary shadow-lg shadow-primary/20'
            : 'bg-surface-container-high text-on-surface-variant border-outline-variant/10'
        "
        class="flex flex-col items-center min-w-[65px] py-2 rounded-xl border transition-all hover:bg-white/5"
      >
        <span class="text-[8px] font-black uppercase opacity-40">{{
          d.day
        }}</span>
        <span class="text-xs font-black">{{ d.date }}</span>
      </button>
    </div>

    <div class="flex gap-4">
      <button
        v-if="canSchedule"
        @click="$emit('open-batch')"
        class="bg-surface-container-highest text-on-surface px-5 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest border border-outline-variant/10 hover:bg-white/10 transition-all flex items-center gap-2"
      >
        <span class="material-symbols-outlined text-sm">bolt</span> Tạo hàng loạt
      </button>
      <button
        v-if="canSchedule"
        @click="$emit('add-showtime')"
        class="bg-primary text-on-primary px-6 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest shadow-lg shadow-primary/20 hover:brightness-110 transition-all flex items-center gap-2"
      >
        <span class="material-symbols-outlined text-sm">add</span>
        Thêm suất chiếu
      </button>
      <button
        v-if="canScheduleEdit"
        @click="$emit('publish')"
        class="bg-green-500 text-white px-6 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest shadow-lg shadow-green-500/20 hover:brightness-110 transition-all flex items-center gap-2"
      >
        <span class="material-symbols-outlined text-sm">publish</span>
        Xuất bản
      </button>
    </div>
  </header>

  <!-- Main Timeline Matrix -->
  <div class="overflow-hidden relative h-[500px] flex flex-col">
    <!-- Scrollable Area -->
    <div
      class="flex-grow overflow-x-auto overflow-y-auto scrollbar-hide relative bg-[#0b111e]"
    >
      <!-- Main Wrapper -->
      <div class="flex flex-col min-h-full relative" :style="{ minWidth: wrapperMinWidth }">
        <!-- Time Ruler -->
        <div
          class="flex border-b border-outline-variant/10 bg-[#0b111e] flex-shrink-0 sticky top-0 z-40"
        >
          <div
            class="w-48 h-full flex-shrink-0 p-4 border-r border-[#1e293b]/60 flex items-center justify-center font-black text-primary uppercase tracking-[0.2em] text-[8px] italic bg-[#0b111e] sticky left-0 z-40"
          >
            Room \ Time
          </div>
          <div class="flex-grow relative h-10" :style="{ minWidth: gridMinWidth }">
            <div
              v-for="mark in hourMarks"
              :key="mark.label + mark.leftPct"
              class="absolute top-0 bottom-0 flex items-center pl-1 text-[9px] font-black text-on-surface-variant/30 border-l border-outline-variant/10"
              :style="{ left: mark.leftPct }"
            >
              {{ mark.label }}
            </div>
          </div>
        </div>

        <!-- Vertical Grid Lines -->
        <div
          class="absolute inset-0 top-10 grid pointer-events-none pl-48 z-0"
          :style="{ gridTemplateColumns: gridTemplate }"
        >
          <div
            v-for="i in gridCols"
            :key="i"
            :class="
              i % 4 === 0
                ? 'border-r border-outline-variant/20'
                : 'border-r border-outline-variant/5'
            "
            class="h-full"
          ></div>
        </div>

        <!-- Current Time Indicator -->
        <div
          v-if="showNow"
          class="absolute top-10 bottom-0 left-48 right-0 pointer-events-none z-10"
        >
          <div
            class="absolute top-0 bottom-0 w-[1px] bg-primary/80"
            :style="{ left: currentTimeLeft }"
          ></div>
        </div>

        <!-- Rows Container -->
        <div class="flex-grow relative z-10 flex flex-col">
          <div
            v-for="hall in sortedHalls"
            :key="hall.id"
            class="flex items-center border-b border-outline-variant/10 group hover:bg-white/[0.02] transition-all min-h-[100px] relative"
          >
            <!-- Hall Labels (Sticky Column) -->
            <div
              class="w-48 flex-shrink-0 p-5 border-r border-[#1e293b]/60 flex items-center gap-3 bg-[#0b111e] sticky left-0 z-30 self-stretch"
            >
              <div
                class="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0 border border-primary/20"
              >
                <span
                  class="material-symbols-outlined text-primary text-lg"
                  >tv_gen</span
                >
              </div>
              <div class="flex flex-col text-left overflow-hidden">
                <h3
                  class="text-[11px] font-black uppercase tracking-tight text-on-surface truncate"
                >
                  {{ hall.name }}
                </h3>
                <p
                  class="text-[8px] font-bold text-on-surface-variant/50 uppercase tracking-widest mt-0.5"
                >
                  {{ hall.type }}
                </p>
              </div>
            </div>

            <!-- Showtime Container -->
            <div
              @dragover.prevent
              @drop="$emit('drop', $event, hall.id)"
              class="flex-grow grid grid-rows-1 gap-x-0 relative p-0 items-center"
              :style="{ gridTemplateColumns: gridTemplate, minWidth: gridMinWidth }"
            >
              <div
                v-for="show in cinema.shows.filter(
                  (s) => s.roomId === hall.id && s.date === selectedDate
                )"
                :key="show.id"
                :draggable="!isShowtimeLocked(show)"
                @dragstart="handleDragStart($event, show)"
                :title="isShowtimeLocked(show) ? 'Suất chiếu đã có khách đặt/bán vé, không thể di chuyển giờ!' : 'Kéo thả để đổi giờ chiếu'"
                :style="{
                  ...getGridStyle(show.startTime, show.duration),
                  backgroundColor: show.color + '33',
                  borderColor:
                    checkConflict(hall.id, show) ||
                    checkFormatMismatch(hall, show.format)
                      ? '#ef4444'
                      : show.color + '66',
                }"
                @click="$emit('open-showtime', show)"
                class="relative h-[76px] mx-0.5 border rounded-xl p-2.5 group/card transition-all duration-300 hover:z-30 hover:scale-[1.02] hover:brightness-125 shadow-xl flex flex-col justify-between"
                :class="{
                  'ring-2 ring-red-500 ring-inset animate-pulse': checkConflict(hall.id, show) || checkFormatMismatch(hall, show.format),
                  'cursor-not-allowed opacity-80': isShowtimeLocked(show),
                  'cursor-pointer': !isShowtimeLocked(show)
                }"
              >
                <div class="flex justify-between items-start">
                  <p class="text-[12px] font-bold font-sans text-white leading-tight truncate tracking-wide flex-1 pr-1 flex items-center gap-1">
                    <span v-if="isShowtimeLocked(show)" class="text-[10px]">🔒</span>
                    {{ show.movie }}
                  </p>
                  <div class="flex items-center gap-1 shrink-0">
                    <span v-if="checkConflict(hall.id, show)" class="material-symbols-outlined text-red-500 text-[12px]">warning</span>
                    <span v-if="checkFormatMismatch(hall, show.format)" class="material-symbols-outlined text-red-500 text-[12px]">error</span>
                    <div class="px-2 h-[18px] bg-white/10 rounded flex items-center justify-center text-[8px] leading-none font-bold font-sans text-white border border-white/20 uppercase tracking-wider">
                      {{ show.format }}
                    </div>
                  </div>
                </div>
                
                <div class="text-[10px] font-bold font-sans text-white/90 tracking-wide mt-0.5">
                  {{ show.startTime }} - {{ getEndTime(show.startTime, show.duration) }}
                </div>
                
                <div class="flex justify-between items-center mt-auto">
                  <span class="text-[9px] font-medium font-sans text-[#B3B3B3]">
                    {{ show.duration }}m
                  </span>
                  <span class="text-[9px] font-bold font-sans text-primary">
                    {{ show.price.toLocaleString() }}đ
                  </span>
                </div>
                <!-- Dynamic Glow -->
                <div
                  class="absolute inset-0 bg-gradient-to-br from-white/5 to-transparent pointer-events-none rounded-2xl"
                ></div>
                
                <!-- Cleaning Time Tail -->
                <div
                  class="absolute top-0 bottom-0 left-[100%] bg-[repeating-linear-gradient(45deg,transparent,transparent_4px,rgba(255,255,255,0.05)_4px,rgba(255,255,255,0.05)_8px)] border-y border-r border-white/5 rounded-r-lg pointer-events-none z-[-1]"
                  :style="{ width: `${(hall.turnaroundTimeMins || 15) / show.duration * 100}%` }"
                  title="Thời gian dọn dẹp"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer Legend -->
    <footer
      class="p-4 bg-on-surface/[0.02] border-t border-outline-variant/10 flex items-center gap-8"
    >
      <div class="flex items-center gap-2">
        <div
          class="w-1.5 h-1.5 rounded-full bg-primary shadow-[0_0_8px_#F5C518]"
        ></div>
        <span
          class="text-[8px] font-black uppercase tracking-widest text-on-surface-variant"
          >Đang chiếu</span
        >
      </div>
      <div class="flex items-center gap-2">
        <div class="w-1.5 h-1.5 rounded-full bg-white/20"></div>
        <span
          class="text-[8px] font-black uppercase tracking-widest text-on-surface-variant"
          >Sắp chiếu</span
        >
      </div>
      <div
        class="ml-auto text-[7px] font-black uppercase tracking-[0.3em] text-on-surface-variant opacity-20 italic"
      >
        Constraint Engine v4.0
      </div>
    </footer>
  </div>
</template>
