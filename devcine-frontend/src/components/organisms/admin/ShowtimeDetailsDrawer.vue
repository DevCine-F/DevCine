<script setup>
import { computed } from 'vue'
import SeatMapBuilder from '@/components/admin/SeatMapBuilder.vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  showSeatMapOnly: {
    type: Boolean,
    default: false
  },
  showtime: {
    type: Object,
    default: null
  },
  cinema: {
    type: Object,
    default: null
  },
  getEndTime: {
    type: Function,
    required: true
  }
})

const emit = defineEmits(['close', 'close-seat-map', 'open-seat-map', 'edit', 'delete', 'cancel'])

const getSoldTickets = (movieTitle) => {
  if (!movieTitle) return 0;
  return movieTitle.includes('DORAEMON') ? 45 : 120;
}
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
      
      <!-- Body -->
      <div class="flex-1 overflow-y-auto p-6" v-if="showtime">
        <!-- Movie Info Header -->
        <div class="flex gap-4 mb-6">
          <!-- Portrait Poster -->
          <div class="w-24 h-[140px] shrink-0 rounded-xl bg-gradient-to-br from-primary/20 to-surface-variant flex items-center justify-center border border-white/10 relative overflow-hidden shadow-lg shadow-black/20">
            <span class="material-symbols-outlined text-4xl text-primary/40">movie</span>
          </div>
          
          <!-- Movie Details -->
          <div class="flex flex-col py-1">
            <div class="flex items-center gap-2 mb-1.5">
              <span :class="showtime.movie.includes('DORAEMON') ? 'bg-green-500' : 'bg-red-500'" class="px-1.5 py-0.5 rounded text-[9px] font-bold text-white uppercase tracking-wider">
                {{ showtime.movie.includes('DORAEMON') ? 'P' : 'T18' }}
              </span>
              <span class="text-[10px] font-medium text-white/60">
                {{ showtime.movie.includes('DORAEMON') ? 'Hoạt hình, Phiêu lưu' : 'Tâm lý, Giật gân' }}
              </span>
            </div>
            <div class="flex items-center gap-3 mb-2">
              <h3 class="text-xl font-black font-headline text-white leading-tight">{{ showtime.movie }}</h3>
              <div 
                class="px-2 py-0.5 rounded-full text-[9px] font-bold uppercase tracking-widest border"
                :class="
                  showtime.status === 'ongoing' ? 'bg-green-500/10 text-green-400 border-green-500/20' :
                  showtime.status === 'past' ? 'bg-white/5 text-white/40 border-white/10' :
                  'bg-orange-500/10 text-orange-400 border-orange-500/20'
                "
              >
                {{ showtime.status === 'ongoing' ? 'Đang chiếu' : showtime.status === 'past' ? 'Đã chiếu' : 'Sắp chiếu' }}
              </div>
            </div>
            
            <div class="flex flex-wrap items-center gap-2 mb-3">
              <div class="px-2 h-[18px] bg-white/10 rounded flex items-center justify-center text-[10px] leading-none font-bold font-sans text-white border border-white/20 uppercase tracking-wider">
                {{ showtime.format }}
              </div>
              <div class="px-2 h-[18px] bg-primary/10 text-primary rounded flex items-center justify-center text-[9px] leading-none font-bold font-sans border border-primary/20 uppercase tracking-wider">
                {{ showtime.movie.includes('DORAEMON') ? 'Lồng tiếng' : 'Phụ đề Tiếng Việt' }}
              </div>
              <span class="text-xs font-medium text-white/50">{{ showtime.duration }} phút</span>
            </div>
            
            <div class="text-[11px] text-white/50 font-medium">Đạo diễn: <span class="text-white/80">{{ showtime.movie.includes('DORAEMON') ? 'Kazuaki Imai' : 'Christopher Nolan' }}</span></div>
            <div class="text-[11px] text-white/50 font-medium mt-0.5 line-clamp-1">Diễn viên: <span class="text-white/80">{{ showtime.movie.includes('DORAEMON') ? 'Wasabi Mizuta, Megumi Ohara' : 'Cillian Murphy, Emily Blunt' }}</span></div>
          </div>
        </div>

        <!-- Synopsis -->
        <div class="mb-6">
          <h4 class="text-[11px] font-bold text-white/50 uppercase tracking-widest mb-2">Nội dung phim</h4>
          <p class="text-xs text-white/70 leading-relaxed line-clamp-3">
            {{ showtime.movie.includes('DORAEMON') ? 'Nobita và những người bạn tình cờ phát hiện ra một hòn đảo kỳ lạ, nơi trú ngụ của những loài động vật đã tuyệt chủng. Họ cùng nhau trải qua cuộc phiêu lưu bảo vệ hòn đảo khỏi sự tấn công của những kẻ săn trộm độc ác.' : 'Câu chuyện lịch sử về nhà vật lý J. Robert Oppenheimer và vai trò lãnh đạo của ông trong Dự án Manhattan, dẫn đến việc chế tạo ra bom nguyên tử trong Thế chiến thứ hai, thay đổi cục diện thế giới mãi mãi.' }}
          </p>
        </div>

        <!-- Divider -->
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
                <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-white/10 text-white/90">20/10/2026</span>
                <p class="text-sm font-bold text-white">{{ showtime.startTime }} - {{ getEndTime(showtime.startTime, showtime.duration) }}</p>
              </div>
            </div>
          </div>
          
          <div class="bg-black/20 p-4 rounded-xl border border-white/5 flex items-center gap-4">
            <div class="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
              <span class="material-symbols-outlined text-primary">meeting_room</span>
            </div>
            <div>
              <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Phòng chiếu</p>
              <p class="text-sm font-bold text-white mt-0.5">{{ cinema?.halls.find(h => h.id === showtime.roomId)?.name || showtime.roomId }}</p>
            </div>
          </div>
          
        </div>

        <!-- Thống kê suất chiếu -->
        <h4 class="text-[11px] font-bold text-white/50 uppercase tracking-widest mt-8 mb-4">Thống kê suất chiếu (Tạm tính)</h4>
        <div class="grid grid-cols-2 gap-4">
          <div 
            class="bg-black/20 p-4 rounded-xl border border-white/5 cursor-pointer hover:bg-white/5 transition-all group relative"
            @click="$emit('open-seat-map')"
          >
            <div class="absolute inset-0 bg-white/5 opacity-0 group-hover:opacity-100 transition-opacity rounded-xl pointer-events-none flex items-center justify-center backdrop-blur-[1px]">
              <span class="px-3 py-1.5 bg-black/80 text-white text-[10px] font-bold uppercase tracking-widest rounded-lg border border-white/10 shadow-xl">Xem sơ đồ ghế</span>
            </div>
            
            <div class="flex items-center justify-between mb-1">
              <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest">Ghế đã đặt</p>
              <span class="text-[9px] font-bold text-primary bg-primary/10 px-1.5 py-0.5 rounded border border-primary/20">
                Trống: {{ 144 - getSoldTickets(showtime.movie) }}
              </span>
            </div>
            <div class="flex items-baseline gap-1">
              <span class="text-lg font-black text-white">{{ getSoldTickets(showtime.movie) }}</span>
              <span class="text-xs text-white/40">/ 144</span>
            </div>
            <div class="h-1 w-full bg-white/5 rounded-full mt-2 overflow-hidden">
              <div class="h-full bg-primary rounded-full transition-all duration-1000" :style="{ width: `${(getSoldTickets(showtime.movie) / 144) * 100}%` }"></div>
            </div>
          </div>
          
          <div class="bg-black/20 p-4 rounded-xl border border-white/5 flex flex-col justify-center">
            <p class="text-[10px] font-bold text-white/50 uppercase tracking-widest mb-1">Doanh thu dự kiến</p>
            <span class="text-lg font-black text-green-400">{{ showtime.movie.includes('DORAEMON') ? '4.275.000đ' : '16.800.000đ' }}</span>
          </div>
        </div>
      </div>
      
      <!-- Footer -->
      <div class="p-6 border-t border-outline-variant/10 bg-surface-container flex gap-4">
        <button 
          v-if="getSoldTickets(showtime?.movie) === 0"
          @click="$emit('delete')"
          class="flex-1 py-3 rounded-xl border border-red-500/30 bg-red-500/10 text-red-500 text-[11px] font-black uppercase tracking-widest hover:bg-red-500/20 transition-all flex items-center justify-center gap-2"
        >
          <span class="material-symbols-outlined text-[16px]">delete</span> Xóa
        </button>
        
        <button 
          v-else
          @click="$emit('cancel')"
          class="flex-1 py-3 rounded-xl border border-white/10 bg-black/20 text-white/50 text-[10px] font-bold uppercase tracking-widest hover:bg-white/5 hover:text-red-400 hover:border-red-400/30 transition-all flex flex-col items-center justify-center gap-0.5 group"
        >
          <div class="flex items-center gap-1">
            <span class="material-symbols-outlined text-[14px]">cancel</span> 
            <span class="group-hover:hidden">Hủy & Hoàn tiền</span>
            <span class="hidden group-hover:inline">Xác nhận Hủy</span>
          </div>
          <span class="text-[8px] font-medium text-white/30 lowercase normal-case tracking-normal">(Đã có {{ getSoldTickets(showtime?.movie) }} vé)</span>
        </button>

        <button 
          @click="$emit('edit')"
          class="flex-1 py-3 rounded-xl bg-primary text-on-primary text-[11px] font-black uppercase tracking-widest transition-all flex items-center justify-center gap-2"
          :class="getSoldTickets(showtime?.movie) > 0 ? 'opacity-80 hover:brightness-105' : 'hover:brightness-110 shadow-lg shadow-primary/20'"
        >
          <span class="material-symbols-outlined text-[16px]">edit</span> 
          {{ getSoldTickets(showtime?.movie) > 0 ? 'Sửa (Hạn chế)' : 'Chỉnh sửa' }}
        </button>
      </div>
    </div>
  </Transition>
  
  <!-- Seat Map ReadOnly Modal -->
  <Transition name="fade">
    <div v-if="showSeatMapOnly" class="fixed inset-0 bg-black/80 backdrop-blur-sm z-[200] flex items-center justify-center p-8" @click.self="$emit('close-seat-map')">
      <div class="bg-surface-container rounded-3xl border border-white/10 shadow-2xl w-full max-w-5xl h-[95vh] flex flex-col relative overflow-hidden">
        <!-- Modal Header -->
        <div class="px-8 py-6 border-b border-white/5 flex justify-between items-center bg-black/20">
          <div>
            <h2 class="text-xl font-black text-white uppercase tracking-widest font-headline">Sơ đồ phòng chiếu</h2>
            <p class="text-sm font-medium text-white/50 mt-1">
              {{ cinema?.halls.find(h => h.id === showtime?.roomId)?.name || showtime?.roomId }} • {{ showtime?.movie }}
            </p>
          </div>
          <button @click="$emit('close-seat-map')" class="w-10 h-10 rounded-full bg-white/5 hover:bg-white/10 flex items-center justify-center transition-all border border-white/10">
            <span class="material-symbols-outlined text-white/70">close</span>
          </button>
        </div>
        
        <!-- Modal Body (SeatMapBuilder in ReadOnly Mode) -->
        <div class="flex-1 overflow-hidden relative">
           <SeatMapBuilder 
             :rows="cinema?.halls.find(h => h.id === showtime?.roomId)?.rows || 10" 
             :cols="cinema?.halls.find(h => h.id === showtime?.roomId)?.cols || 16"
             :initialMap="{}"
             :readonly="true"
             :soldTickets="getSoldTickets(showtime?.movie)"
             :canceledTickets="showtime?.movie?.includes('DORAEMON') ? 2 : 5"
             :revenue="showtime?.movie?.includes('DORAEMON') ? '4.275.000đ' : '16.800.000đ'"
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
