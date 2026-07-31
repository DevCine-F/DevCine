<script setup>
import { computed } from 'vue'
import SeatMapBuilder from '@/components/admin/SeatMapBuilder.vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  showSeatMapOnly: { type: Boolean, default: false },
  // Dữ liệu THỰC TẾ từ API /showtimes/{id}/detail (không còn hardcode).
  detail: { type: Object, default: null },
  isLoading: { type: Boolean, default: false },
  cinema: { type: Object, default: null },
  getEndTime: { type: Function, required: true }
})

defineEmits(['close', 'close-seat-map', 'open-seat-map', 'delete'])

// LocalDateTime từ backend có thể là chuỗi "yyyy-MM-ddTHH:mm:ss" hoặc mảng [y,m,d,h,mi].
const toDate = (val) => {
  if (Array.isArray(val)) return new Date(val[0], (val[1] || 1) - 1, val[2] || 1, val[3] || 0, val[4] || 0)
  if (typeof val === 'string') return new Date(val)
  return null
}
const pad = (n) => n.toString().padStart(2, '0')
const fmtDate = (val) => { const d = toDate(val); return d ? `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}` : '—' }
const fmtTime = (val) => { const d = toDate(val); return d ? `${pad(d.getHours())}:${pad(d.getMinutes())}` : '—' }
const fmtMoney = (v) => `${Number(v || 0).toLocaleString('vi-VN')}đ`

// Trạng thái LIVE tính theo thời gian thực (chuẩn hơn chuỗi status lưu sẵn ở DB).
const liveStatus = computed(() => {
  const d = props.detail
  if (!d) return 'upcoming'
  const now = new Date(), s = toDate(d.startTime), e = toDate(d.endTime)
  if (s && now < s) return 'upcoming'
  if (e && now > e) return 'past'
  return 'ongoing'
})

const reserved = computed(() => (props.detail ? (props.detail.soldSeats || 0) + (props.detail.heldSeats || 0) : 0))
const total = computed(() => props.detail?.totalSeats || 0)
const soldPct = computed(() => (total.value > 0 ? Math.round(((props.detail?.soldSeats || 0) / total.value) * 100) : 0))

const roomGeom = computed(() =>
  props.cinema?.halls?.find(h => h.id === props.detail?.roomId) || null)
</script>

<template>
  <Transition name="fade">
    <div v-if="show" class="fixed inset-0 bg-black/60 backdrop-blur-sm z-[100]" @click="$emit('close')"></div>
  </Transition>

  <Transition name="drawer">
    <div v-if="show" class="fixed top-0 right-0 h-full w-[500px] bg-surface-container-high border-l border-outline-variant/10 shadow-2xl z-[101] flex flex-col">
      <!-- Header -->
      <div class="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container">
        <h2 class="text-lg font-black font-headline uppercase tracking-widest text-on-surface">Chi tiết lịch chiếu</h2>
        <button @click="$emit('close')" class="w-8 h-8 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center transition-all">
          <span class="material-symbols-outlined text-white/70 text-sm">close</span>
        </button>
      </div>

      <!-- Loading -->
      <div v-if="isLoading && !detail" class="flex-1 flex flex-col items-center justify-center gap-3 text-white/40">
        <span class="material-symbols-outlined text-4xl animate-spin">progress_activity</span>
        <p class="text-xs uppercase tracking-widest font-bold">Đang tải chi tiết...</p>
      </div>

      <!-- Body -->
      <div v-else-if="detail" class="flex-1 overflow-y-auto p-6">
        <!-- Movie Info Header -->
        <div class="flex gap-4 mb-6">
          <div class="w-24 h-[140px] shrink-0 rounded-xl bg-gradient-to-br from-primary/20 to-surface-variant flex items-center justify-center border border-white/10 relative overflow-hidden shadow-lg shadow-black/20">
            <img v-if="detail.posterUrl" :src="detail.posterUrl" :alt="detail.movieTitle" class="absolute inset-0 w-full h-full object-cover" />
            <span v-else class="material-symbols-outlined text-4xl text-primary/40">movie</span>
          </div>

          <div class="flex flex-col py-1 min-w-0">
            <div class="flex items-center gap-2 mb-1.5">
              <span v-if="detail.ageRating" class="px-1.5 py-0.5 rounded text-[9px] font-bold text-white uppercase tracking-wider bg-red-500">{{ detail.ageRating }}</span>
              <span v-if="detail.genres?.length" class="text-[10px] font-medium text-white/60 truncate">{{ detail.genres.join(', ') }}</span>
            </div>
            <div class="flex items-center gap-3 mb-2">
              <h3 class="text-xl font-black font-headline text-white leading-tight">{{ detail.movieTitle }}</h3>
              <div
                class="px-2 py-0.5 rounded-full text-[9px] font-bold uppercase tracking-widest border shrink-0"
                :class="
                  liveStatus === 'ongoing' ? 'bg-green-500/10 text-green-400 border-green-500/20' :
                  liveStatus === 'past' ? 'bg-white/5 text-white/40 border-white/10' :
                  'bg-orange-500/10 text-orange-400 border-orange-500/20'
                "
              >
                {{ liveStatus === 'ongoing' ? 'Đang chiếu' : liveStatus === 'past' ? 'Đã chiếu' : 'Sắp chiếu' }}
              </div>
            </div>

            <div class="flex flex-wrap items-center gap-2 mb-3">
              <div class="px-2 h-[18px] bg-white/10 rounded flex items-center justify-center text-[10px] leading-none font-bold font-sans text-white border border-white/20 uppercase tracking-wider">
                {{ detail.formatName }}
              </div>
              <div v-if="detail.versionType" class="px-2 h-[18px] bg-primary/10 text-primary rounded flex items-center justify-center text-[9px] leading-none font-bold font-sans border border-primary/20 uppercase tracking-wider">
                {{ detail.versionType }}
              </div>
              <span class="text-xs font-medium text-white/50">{{ detail.durationMins }} phút</span>
            </div>

            <div v-if="detail.director" class="text-[11px] text-white/50 font-medium">Đạo diễn: <span class="text-white/80">{{ detail.director }}</span></div>
            <div v-if="detail.castMembers" class="text-[11px] text-white/50 font-medium mt-0.5 line-clamp-1">Diễn viên: <span class="text-white/80">{{ detail.castMembers }}</span></div>
          </div>
        </div>

        <!-- Synopsis -->
        <div v-if="detail.description" class="mb-6">
          <h4 class="text-[11px] font-bold text-white/50 uppercase tracking-widest mb-2">Nội dung phim</h4>
          <p class="text-xs text-white/70 leading-relaxed line-clamp-3">{{ detail.description }}</p>
        </div>

        <div class="h-px w-full bg-outline-variant/10 mb-6"></div>

        <h4 class="text-[11px] font-bold text-white/50 uppercase tracking-widest mb-4">Thông tin lịch chiếu</h4>
        <div class="space-y-4">
          <div class="bg-black/20 p-4 rounded-xl border border-white/5 flex items-center gap-4">
            <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined text-primary">schedule</span>
            </div>
            <div>
              <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Ngày & Thời gian</p>
              <div class="flex items-center gap-2 mt-0.5">
                <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-white/10 text-white/90">{{ fmtDate(detail.startTime) }}</span>
                <p class="text-sm font-bold text-white">{{ fmtTime(detail.startTime) }} - {{ fmtTime(detail.endTime) }}</p>
              </div>
            </div>
          </div>

          <div class="bg-black/20 p-4 rounded-xl border border-white/5 flex items-center gap-4">
            <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined text-primary">meeting_room</span>
            </div>
            <div>
              <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Phòng chiếu</p>
              <p class="text-sm font-bold text-white mt-0.5">{{ detail.roomName }} <span class="text-white/40 font-medium">· {{ detail.cinemaName }}</span></p>
            </div>
          </div>
        </div>

        <!-- Thống kê suất chiếu (THỰC TẾ) -->
        <h4 class="text-[11px] font-bold text-white/50 uppercase tracking-widest mt-8 mb-4">Thống kê suất chiếu</h4>
        <div class="grid grid-cols-2 gap-4">
          <div
            class="bg-black/20 p-4 rounded-xl border border-white/5 cursor-pointer hover:bg-white/5 transition-all group relative"
            @click="$emit('open-seat-map')"
          >
            <div class="absolute inset-0 bg-white/5 opacity-0 group-hover:opacity-100 transition-opacity rounded-xl pointer-events-none flex items-center justify-center backdrop-blur-[1px]">
              <span class="px-3 py-1.5 bg-black/80 text-white text-[10px] font-bold uppercase tracking-widest rounded-lg border border-white/10 shadow-xl">Xem sơ đồ ghế</span>
            </div>

            <div class="flex items-center justify-between mb-1">
              <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Ghế đã bán</p>
              <span class="text-[9px] font-bold text-primary bg-primary/10 px-1.5 py-0.5 rounded border border-primary/20">
                Trống: {{ detail.availableSeats }}
              </span>
            </div>
            <div class="flex items-baseline gap-1">
              <span class="text-lg font-black text-white">{{ detail.soldSeats }}</span>
              <span class="text-xs text-white/40">/ {{ total }}</span>
              <span v-if="detail.heldSeats > 0" class="text-[9px] text-orange-400 ml-1">(+{{ detail.heldSeats }} giữ)</span>
            </div>
            <div class="h-1 w-full bg-white/5 rounded-full mt-2 overflow-hidden">
              <div class="h-full bg-primary rounded-full transition-all duration-1000" :style="{ width: `${soldPct}%` }"></div>
            </div>
          </div>

          <div class="bg-black/20 p-4 rounded-xl border border-white/5 flex flex-col justify-center">
            <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest mb-1">Doanh thu vé</p>
            <span class="text-lg font-black text-green-400">{{ fmtMoney(detail.revenue) }}</span>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div v-if="detail" class="p-6 border-t border-outline-variant/10 bg-surface-container">
        <button
          v-if="reserved === 0"
          @click="$emit('delete')"
          class="w-full py-3 rounded-xl border border-red-500/30 bg-red-500/10 text-red-500 text-[11px] font-black uppercase tracking-widest hover:bg-red-500/20 transition-all flex items-center justify-center gap-2"
        >
          <span class="material-symbols-outlined text-[16px]">delete</span> Xoá suất chiếu
        </button>
        <div
          v-else
          class="w-full py-3 rounded-xl border border-white/10 bg-black/20 text-white/50 text-[10px] font-bold uppercase tracking-widest text-center flex flex-col items-center justify-center gap-0.5"
        >
          <div class="flex items-center gap-1.5 text-amber-400">
            <span class="material-symbols-outlined text-[16px]">lock</span> Không thể xoá
          </div>
          <span class="text-[9px] font-medium text-white/30 normal-case tracking-normal">Đã có {{ reserved }} vé bán/giữ — cần hoàn/huỷ vé trước</span>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Seat Map ReadOnly Modal -->
  <Transition name="fade">
    <div v-if="showSeatMapOnly" class="fixed inset-0 bg-black/80 backdrop-blur-sm z-[200] flex items-center justify-center p-8" @click.self="$emit('close-seat-map')">
      <div class="bg-surface-container rounded-3xl border border-white/10 shadow-2xl w-full max-w-5xl h-[95vh] flex flex-col relative overflow-hidden">
        <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-black/20">
          <div>
            <h2 class="text-xl font-black text-white uppercase tracking-widest font-headline">Sơ đồ phòng chiếu</h2>
            <p class="text-sm font-medium text-white/50 mt-1">
              {{ detail?.roomName || detail?.roomId }} • {{ detail?.movieTitle }}
            </p>
          </div>
          <button @click="$emit('close-seat-map')" class="w-10 h-10 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center transition-all border border-white/10">
            <span class="material-symbols-outlined text-white/70">close</span>
          </button>
        </div>

        <div class="flex-1 overflow-hidden relative">
          <SeatMapBuilder
            :rows="roomGeom?.rows || 10"
            :cols="roomGeom?.cols || 16"
            :initialMap="{}"
            :readonly="true"
            :soldTickets="detail?.soldSeats || 0"
            :canceledTickets="0"
            :revenue="fmtMoney(detail?.revenue)"
          />
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.drawer-enter-active,
.drawer-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
